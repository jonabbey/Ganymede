/*

   PermMatrix.java

   This class stores a read-only matrix of PermEntry bits, organized by
   object type and field id's.
   
   Created: 3 October 1997
   Release: $Name:  $
   Version: $Revision: 1.19 $
   Last Mod Date: $Date: 2001/07/27 01:02:19 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999, 2000, 2001
   The University of Texas at Austin.

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
   along with this program; if not, write to the Free Software
   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.

*/

package arlut.csd.ganymede;

import java.util.*;
import java.rmi.*;
import java.io.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                      PermMatrix

------------------------------------------------------------------------------*/

/**
 * <P>Serializable permissions matrix object, used to handle
 * permissions for a given user, admin, or role.</P>
 *
 * <P>This class stores a read-only Hashtable of
 * {@link arlut.csd.ganymede.PermEntry PermEntry} objects, organized by
 * object type and field id's.</P>
 *
 * <P>The keys to the Hashtable are Strings that are encoded by the
 * static {@link
 * arlut.csd.ganymede.PermissionMatrixDBField#matrixEntry(short,
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
 * {@link arlut.csd.ganymede.PermissionMatrixDBField PermissionMatrixDBField}
 * and {@link arlut.csd.ganymede.GanymedeSession GanymedeSession}.</P> */

public class PermMatrix implements java.io.Serializable {

  static final boolean debug = false;

  static final long serialVersionUID = 7354985227082627640L;

  // ---

  public Hashtable matrix;

  /* -- */

  public PermMatrix()
  {
    matrix = new Hashtable();
  }

  public PermMatrix(Hashtable orig)
  {
    if (orig == null)
      {
	this.matrix = new Hashtable();
      }
    else
      {
	this.matrix = (Hashtable) orig.clone();
      }
  }

  public PermMatrix(PermMatrix orig)
  {
    this.matrix = (Hashtable) orig.matrix.clone();
  }

  /**
   * <P>This method combines this PermMatrix with that
   * of orig.  The returned PermMatrix will allow
   * any access either of the source PermMatrices
   * would.</P>
   */

  public PermMatrix union(Hashtable orig)
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
    Enumeration enum;
    PermEntry entry1, entry2, entry3;
    Object key;

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

    enum = this.matrix.keys();

    while (enum.hasMoreElements())
      {
	key = enum.nextElement();

	entry1 = (PermEntry) this.matrix.get(key);
	entry2 = (PermEntry) orig.matrix.get(key);

	if (entry2 != null)
	  {
	    result.matrix.put(key, entry1.union(entry2));
	  }
	else
	  {
	    // okay, orig didn't have any entry for key

	    if (!isBasePerm((String)key))
	      {
		// We are union'ing a field entry.. since orig doesn't
		// contain an explicit record for this field while our
		// matrix does, see if we can find a record for the
		// containing base in field and union that with this entry..
		// this will serve to maintain the permission inheritance
		// issues

		entry3 = (PermEntry) orig.matrix.get(baseEntry((String)key));

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

    enum = orig.matrix.keys();

    while (enum.hasMoreElements())
      {
	key = enum.nextElement();

	entry1 = (PermEntry) orig.matrix.get(key);
	entry2 = (PermEntry) this.matrix.get(key);

	if (entry2 != null)
	  {
	    result.matrix.put(key, entry1.union(entry2));
	  }
	else
	  {
	    if (!isBasePerm((String)key))
	      {
		// the orig matrix has a field entry that we didn't
		// have a match for in this.matrix.. we need to check
		// to see if we have a base entry for the corresponding
		// base that we need to union in to reflect the default
		// base -> field inheritance

		entry3 = (PermEntry) this.matrix.get(baseEntry((String)key));

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
   * @see arlut.csd.ganymede.PermMatrix 
   */

  public PermEntry getPerm(short baseID, short fieldID)
  {
    return (PermEntry) matrix.get(matrixEntry(baseID, fieldID));
  }

  /**
   * <P>Returns a PermEntry object representing this PermMatrix's 
   * permissions on the base &lt;baseID&gt;</P>
   *
   * @see arlut.csd.ganymede.PermMatrix
   */

  public PermEntry getPerm(short baseID)
  {
    return (PermEntry) matrix.get(matrixEntry(baseID));
  }

  /**
   * <P>Returns a PermEntry object representing this PermMatrix's 
   * permissions on the field &lt;field&gt; in base &lt;base&gt;</P>
   *
   * @see arlut.csd.ganymede.PermMatrix
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
   * @see arlut.csd.ganymede.PermMatrix
   */

  public PermEntry getPerm(Base base)
  {
    try
      {
	return (PermEntry) matrix.get(matrixEntry(base.getTypeID()));
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("caught remote: " + ex);
      }
  }

  /**
   * <P>Private method to generate a key for use in
   * our internal Hashtable, used to encode the 
   * permission for a given {@link arlut.csd.ganymede.DBObjectBase
   * DBObjectBase} and {@link arlut.csd.ganymede.DBObjectBaseField
   * DBObjectBaseField}.</P>
   */

  private String matrixEntry(short baseID, short fieldID)
  {
    return (baseID + ":" + fieldID);
  }

  /**
   * <P>Private method to generate a key for use in
   * our internal Hashtable, used to encode the
   * permission for a given {@link arlut.csd.ganymede.DBObjectBase
   * DBObjectBase}.</P>
   */
  
  private String matrixEntry(short baseID)
  {
    return (baseID + "::");
  }

  /**
   * <P>Returns true if the given String encodes the identity of
   * a {@link arlut.csd.ganymede.DBObjectBase DBObjectBase} and
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
   * @return Returns the {@link arlut.csd.ganymede.DBObjectBase DBObjectBase}
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
   * {@link arlut.csd.ganymede.DBObjectBaseField DBObjectBaseField}
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
   * for a single  {@link arlut.csd.ganymede.DBObjectBase DBObjectBase} from
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
	return super.toString();
      }
  }

  public String toString(boolean t)
  {
    StringBuffer result = new StringBuffer();
    Enumeration enum;
    String key;
    PermEntry entry;
    String basename;
    Hashtable baseHash = new Hashtable();
    Vector vec;

    /* -- */

    result.append("PermMatrix DebugDump\n");

    enum = matrix.keys();

    while (enum.hasMoreElements())
      {
	key = (String) enum.nextElement();

	entry = (PermEntry) matrix.get(key);

	basename = key;

	if (baseHash.containsKey(basename))
	  {
	    vec = (Vector) baseHash.get(basename);
	  }
	else
	  {
	    vec = new Vector();
	    baseHash.put(basename, vec);
	  }

	vec.addElement(key + " -- " + entry.toString());
      }

    enum = baseHash.keys();

    while (enum.hasMoreElements())
      {
	key = (String) enum.nextElement();

	//	result.append("\nBase - " + key + "\n");

	vec = (Vector) baseHash.get(key);

	for (int i = 0; i < vec.size(); i++)
	  {
	    result.append(vec.elementAt(i) + "\n");
	  }

	result.append("\n");
      }

    return result.toString();
  }

  public static void main(String argv[])
  {
    PermMatrix x = new PermMatrix();

    x.matrix.put(x.matrixEntry((short) 267), PermEntry.getPermEntry(true, false, false, false));
    x.matrix.put(x.matrixEntry((short) 267, (short) 1), PermEntry.getPermEntry(true, false, false, true));
    x.matrix.put(x.matrixEntry((short) 267, (short) 2), PermEntry.getPermEntry(true, false, false, true));
    x.matrix.put(x.matrixEntry((short) 267, (short) 3), PermEntry.getPermEntry(true, false, false, true));
    x.matrix.put(x.matrixEntry((short) 267, (short) 4), PermEntry.getPermEntry(true, true, true, false));

    System.err.println("Matrix 1: " + x.toString(true));

    PermMatrix y = new PermMatrix();
    y.matrix.put(y.matrixEntry((short) 267), PermEntry.getPermEntry(false, true, false, false));
    y.matrix.put(y.matrixEntry((short) 267, (short) 1), PermEntry.getPermEntry(true, false, false, false));
    y.matrix.put(y.matrixEntry((short) 267, (short) 2), PermEntry.getPermEntry(true, false, false, false));
    y.matrix.put(y.matrixEntry((short) 267, (short) 5), PermEntry.getPermEntry(true, false, false, false));
    y.matrix.put(y.matrixEntry((short) 267, (short) 6), PermEntry.getPermEntry(true, false, false, false));


    System.err.println("Matrix 2: " + y.toString(true));

    PermMatrix z = x.union(y);

    System.err.println("Matrix 1 union 2: " + z.toString(true));

    PermMatrix a = y.union(x);

    System.err.println("Matrix 2 union 1: " + a.toString(true));
  }
}
