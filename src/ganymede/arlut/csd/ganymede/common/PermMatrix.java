/*

   PermMatrix.java

   This class stores a read-only matrix of PermEntry bits, organized by
   object type and field id's.

   Created: 3 October 1997

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2010
   The University of Texas at Austin

   Contact information

   Author Email: ganymede_author@arlut.utexas.edu
   Email mailing list: ganymede@arlut.utexas.edu

   US Mail:

   Computer Science Division
   Applied Research Laboratories
   The University of Texas at Austin
   PO Box 8029, Austin TX 78713-8029

   Telephone: (512) 835-3200

   This program is free software; you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation; either version 2 of the License, or
   (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/

package arlut.csd.ganymede.common;

import java.rmi.RemoteException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;

import arlut.csd.ganymede.rmi.Base;
import arlut.csd.ganymede.rmi.BaseField;
import arlut.csd.ganymede.server.PermissionMatrixDBField;

/*------------------------------------------------------------------------------
                                                                           class
                                                                      PermMatrix

------------------------------------------------------------------------------*/

/**
 * <P>Serializable permissions matrix object, used to handle
 * permissions for a given user, admin, or role.</P>
 *
 * <P>This class stores a read-only Hashtable of
 * {@link arlut.csd.ganymede.common.PermEntry PermEntry} objects, organized by
 * object type and field id's.</P>
 *
 * <P>The keys to the Hashtable are Strings that are encoded by the
 * static {@link
 * arlut.csd.ganymede.server.PermissionMatrixDBField#matrixEntry(short,
 * short) matrixEntry()} methods defined in this class.  I probably
 * could have used some sort of class object for the key, but then I
 * would have had to define a key() of similar complexity to the
 * matrixEntry() and decode methods anyway, as well as some sort of
 * on-disk representation for the Ganymede.db file.</P>
 *
 * <P>Here's some examples of the key encoding algorithm:</P>
 *
 * <UL>
 * <LI><CODE>3::/CODE> - Object type 3, permission for object itself</LI>
 * <LI><CODE>3:10</CODE> - Object type 3, permission for field 10</LI>
 * </UL>
 *
 * <P>PermMatrix is used on the client in the Permissions Editor dialog,
 * and on the server in both
 * {@link arlut.csd.ganymede.server.PermissionMatrixDBField PermissionMatrixDBField}
 * and {@link arlut.csd.ganymede.server.GanymedeSession GanymedeSession}.</P> */

public class PermMatrix implements java.io.Serializable {

  static final boolean debug = false;

  static final long serialVersionUID = 7354985227082627640L;

  // ---

  private Hashtable<String, PermEntry> matrix;

  /* -- */

  public PermMatrix()
  {
    matrix = new Hashtable<String, PermEntry>();
  }

  public PermMatrix(Hashtable<String, PermEntry> orig)
  {
    if (orig == null)
      {
	this.matrix = new Hashtable<String, PermEntry>();
      }
    else
      {
	this.matrix = (Hashtable<String, PermEntry>) orig.clone();
      }
  }

  public PermMatrix(PermMatrix orig)
  {
    this.matrix = (Hashtable<String, PermEntry>) orig.matrix.clone();
  }

  /**
   * <p>Returns a read-only view of the underlying Hashtable we're
   * using to keep our permissions data.</p>
   *
   * <p>Used by {@link
   * arlut.csd.ganymede.server.PermissionMatrixDBField} to provide
   * debug dumping of our contents in a server context.</p>
   */

  public Map<String, PermEntry> getMatrix()
  {
    return Collections.unmodifiableMap(matrix);
  }

  /**
   * <p>Returns a copy of the internal Hashtable containing
   * our permission matrix map.</p>
   *
   * <p>Used by {@link arlut.csd.ganymede.server.PermMatrixCkPoint}
   * class.</p>
   */

  public Hashtable<String, PermEntry> getMatrixClone()
  {
    return (Hashtable<String, PermEntry>) matrix.clone();
  }

  /**
   * <P>This method combines this PermMatrix with that
   * of orig.  The returned PermMatrix will allow
   * any access either of the source PermMatrices
   * would.</P>
   */

  public PermMatrix union(Hashtable<String, PermEntry> orig)
  {
    return union(new PermMatrix(orig));	// this will cause a redundant copy, but who cares?
  }

  /**
   * <P>This method combines this PermMatrix with that of orig.  The
   * returned PermMatrix will allow any access either of the source
   * PermMatrices would.</P>
   *
   * <P>Note that unlike all the other methods in PermMatrix, we are
   * handling inheritance of default permissions for an object base
   * into fields which do not have permissions specified.  We have
   * to take this into account here in order to do a proper union.</P>
   *
   * <P>As a consequence, this union() method is more complex than
   * you might expect.  That complexity really is needed.. don't
   * mess with this unless you really, really know what you're doing.</P>
   */

  public synchronized PermMatrix union(PermMatrix orig)
  {
    PermMatrix result;
    PermEntry entry1, entry2, entry3;

    /* -- */

    if (orig == null)
      {
	return new PermMatrix(this);
      }

    // duplicate orig as our starting point.  we'll then union
    // anything in this PermMatrix on top of orig in result

    //    result = new PermMatrix(orig);
    result = new PermMatrix();

    // now go through our matrix and for any entries in this.matrix,
    // put the union of that and the matching entry already in result
    // into result.

    for (String key: this.matrix.keySet())
      {
	entry1 = this.matrix.get(key);
	entry2 = orig.matrix.get(key);

	if (entry2 != null)
	  {
	    result.matrix.put(key, entry1.union(entry2));
	  }
	else
	  {
	    // okay, orig didn't have any entry for key

	    if (!isBasePerm(key))
	      {
		// We are union'ing a field entry.. since orig doesn't
		// contain an explicit record for this field while our
		// matrix does, see if we can find a record for the
		// containing base in field and union that with this entry..
		// this will serve to maintain the permission inheritance
		// issues

		entry3 = orig.matrix.get(baseEntry(key));

		// union will handle a null entry3

		result.matrix.put(key, entry1.union(entry3));
	      }
	    else
	      {
		// we've got a base entry from this.matrix that wasn't in orig,
		// so we need to just copy it into result

		result.matrix.put(key, entry1);
	      }
	  }
      }

    // result now contains all of the records from this.matrix,
    // union'ed with the orig versions.  The only problem now is that
    // we haven't covered entries in orig that weren't in this.matrix.

    // loop over the orig values for completeness

    for (String key: orig.matrix.keySet())
      {
	entry1 = orig.matrix.get(key);
	entry2 = this.matrix.get(key);

	if (entry2 != null)
	  {
	    result.matrix.put(key, entry1.union(entry2));
	  }
	else
	  {
	    if (!isBasePerm(key))
	      {
		// the orig matrix has a field entry that we didn't
		// have a match for in this.matrix.. we need to check
		// to see if we have a base entry for the corresponding
		// base that we need to union in to reflect the default
		// base -> field inheritance

		entry3 = this.matrix.get(baseEntry(key));

		// union will handle a null entry3

		result.matrix.put(key, entry1.union(entry3));
	      }
	    else
	      {
		result.matrix.put(key, entry1);
	      }
	  }
      }

    return result;
  }

  /**
   * <P>Returns a PermEntry object representing this PermMatrix's
   * permissions on the field &lt;fieldID&gt; in base &lt;baseID&gt;</P>
   *
   * <P>If there is no entry in this PermMatrix for the given field,
   * getPerm() will return null.</P>
   *
   * @see arlut.csd.ganymede.common.PermMatrix
   */

  public PermEntry getPerm(short baseID, short fieldID)
  {
    return matrix.get(matrixEntry(baseID, fieldID));
  }

  /**
   * <P>Returns a PermEntry object representing this PermMatrix's
   * permissions on the base &lt;baseID&gt;</P>
   *
   * @see arlut.csd.ganymede.common.PermMatrix
   */

  public PermEntry getPerm(short baseID)
  {
    return matrix.get(matrixEntry(baseID));
  }

  /**
   * <P>Returns a PermEntry object representing this PermMatrix's
   * permissions on the field &lt;field&gt; in base &lt;base&gt;</P>
   *
   * @see arlut.csd.ganymede.common.PermMatrix
   */

  public PermEntry getPerm(Base base, BaseField field)
  {
    try
      {
	return getPerm(base.getTypeID(), field.getID());
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("caught remote: " + ex);
      }
  }

  /**
   * <P>Returns a PermEntry object representing this PermMatrix's
   * permissions on the base &lt;base&gt;</P>
   *
   * @see arlut.csd.ganymede.common.PermMatrix
   */

  public PermEntry getPerm(Base base)
  {
    try
      {
	return matrix.get(matrixEntry(base.getTypeID()));
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("caught remote: " + ex);
      }
  }

  /**
   * <P>Private method to generate a key for use in
   * our internal Hashtable, used to encode the
   * permission for a given {@link arlut.csd.ganymede.server.DBObjectBase
   * DBObjectBase} and {@link arlut.csd.ganymede.server.DBObjectBaseField
   * DBObjectBaseField}.</P>
   */

  private String matrixEntry(short baseID, short fieldID)
  {
    return (baseID + ":" + fieldID);
  }

  /**
   * <P>Private method to generate a key for use in
   * our internal Hashtable, used to encode the
   * permission for a given {@link arlut.csd.ganymede.server.DBObjectBase
   * DBObjectBase}.</P>
   */

  private String matrixEntry(short baseID)
  {
    return (baseID + "::");
  }

  /**
   * <P>Returns true if the given String encodes the identity of
   * a {@link arlut.csd.ganymede.server.DBObjectBase DBObjectBase} and
   * not a field within a DBObjectBase.</P>
   */

  private boolean isBasePerm(String matrixEntry)
  {
    return (matrixEntry.indexOf("::") != -1);
  }

  /**
   * <P>Private helper method used to decode a hash key generated
   * by the matrixEntry() methods.</P>
   *
   * @return Returns the {@link arlut.csd.ganymede.server.DBObjectBase DBObjectBase}
   * object id encoded by the given String.
   */

  private short entryBase(String matrixEntry)
  {
    if (matrixEntry.indexOf(':') == -1)
      {
	throw new IllegalArgumentException("not a valid matrixEntry");
      }

    String baseStr = matrixEntry.substring(0, matrixEntry.indexOf(':'));

    try
      {
	return Short.parseShort(baseStr);
      }
    catch (NumberFormatException ex)
      {
	throw new RuntimeException("bad string format:" + ex);
      }
  }

  /**
   * <P>Private helper method used to decode a hash key generated
   * by the matrixEntry() methods.</P>
   *
   * @return Returns the
   * {@link arlut.csd.ganymede.server.DBObjectBaseField DBObjectBaseField}
   * object id encoded by the given String.
   */

  private short entryField(String matrixEntry)
  {
    if (matrixEntry.indexOf(':') == -1)
      {
	throw new IllegalArgumentException("not a valid matrixEntry");
      }

    if (isBasePerm(matrixEntry))
      {
	throw new IllegalArgumentException("not a field matrixEntry");
      }

    String fieldStr = matrixEntry.substring(matrixEntry.lastIndexOf(':')+1);

    try
      {
	return Short.parseShort(fieldStr);
      }
    catch (NumberFormatException ex)
      {
	throw new RuntimeException("bad string format:" + ex);
      }
  }

  /**
   * <P>Private helper method used to generate a matrixEntry() encoded String
   * for a single  {@link arlut.csd.ganymede.server.DBObjectBase DBObjectBase} from
   * a matrixEntry() encoded String that also includes a field specification.</P>
   */

  private String baseEntry(String matrixEntry)
  {
    if (isBasePerm(matrixEntry))
      {
	return matrixEntry;
      }
    else
      {
	return matrixEntry(entryBase(matrixEntry));
      }
  }

  /**
   * <P>Debugging output</P>
   */

  public String toString()
  {
    try
      {
	return PermissionMatrixDBField.debugdecode(matrix);
      }
    catch (Throwable ex)
      {
	// the client will get a ClassNotFoundException on PermissionMatrixDBField.

	return super.toString();
      }
  }

  public static void main(String argv[])
  {
    PermMatrix x = new PermMatrix();

    x.matrix.put(x.matrixEntry((short) 267), PermEntry.getPermEntry(true, false, false, false));
    x.matrix.put(x.matrixEntry((short) 267, (short) 1), PermEntry.getPermEntry(true, false, false, true));
    x.matrix.put(x.matrixEntry((short) 267, (short) 2), PermEntry.getPermEntry(true, false, false, true));
    x.matrix.put(x.matrixEntry((short) 267, (short) 3), PermEntry.getPermEntry(true, false, false, true));
    x.matrix.put(x.matrixEntry((short) 267, (short) 4), PermEntry.getPermEntry(true, true, true, false));

    System.err.println("Matrix 1: " + x.toString());

    PermMatrix y = new PermMatrix();
    y.matrix.put(y.matrixEntry((short) 267), PermEntry.getPermEntry(false, true, false, false));
    y.matrix.put(y.matrixEntry((short) 267, (short) 1), PermEntry.getPermEntry(true, false, false, false));
    y.matrix.put(y.matrixEntry((short) 267, (short) 2), PermEntry.getPermEntry(true, false, false, false));
    y.matrix.put(y.matrixEntry((short) 267, (short) 5), PermEntry.getPermEntry(true, false, false, false));
    y.matrix.put(y.matrixEntry((short) 267, (short) 6), PermEntry.getPermEntry(true, false, false, false));


    System.err.println("Matrix 2: " + y.toString());

    PermMatrix z = x.union(y);

    System.err.println("Matrix 1 union 2: " + z.toString());

    PermMatrix a = y.union(x);

    System.err.println("Matrix 2 union 1: " + a.toString());
  }
}
