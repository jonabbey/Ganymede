/*

   PermMatrix.java

   This class stores a read-only matrix of PermEntry bits, organized by
   object type and field id's.
   
   Created: 3 October 1997
   Release: $Name:  $
   Version: $Revision: 1.15 $
   Last Mod Date: $Date: 1999/06/24 00:56:26 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999  The University of Texas at Austin.

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

  public PermMatrix(PermissionMatrixDBField field)
  {
    if ((field == null) || (field.matrix == null))
      {
	this.matrix = new Hashtable();
      }	
    else
      {
	this.matrix = (Hashtable) field.matrix.clone();
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

  public PermMatrix union(PermissionMatrixDBField orig)
  {
    return union(new PermMatrix(orig));	// this will cause a redundant copy, but who cares?
  }

  /**
   * <P>This method combines this PermMatrix with that
   * of orig.  The returned PermMatrix will allow
   * any access either of the source PermMatrices
   * would.</P>
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

    result = new PermMatrix(orig);

    // now go through our matrix and for any entries in this.matrix,
    // put the union of that and the matching entry already in result
    // into result.

    enum = this.matrix.keys();

    while (enum.hasMoreElements())
      {
	key = enum.nextElement();

	entry1 = (PermEntry) this.matrix.get(key);
	entry2 = (PermEntry) result.matrix.get(key);

	if (entry2 != null)
	  {
	    result.matrix.put(key, entry1.union(entry2));
	  }
	else
	  {
	    if (!isBasePerm((String)key))
	      {
		// We are union'ing a field entry.. since orig doesn't
		// contain an explicit record for this field while our
		// matrix does, see if we can find a record for the
		// containing base in field and union that with this entry..
		// this will serve to maintain the permission inheritance
		// issues

		entry3 = (PermEntry) result.matrix.get(baseEntry((String)key));

		if (entry3 == null)
		  {
		    // there is no entry for this field's base.. put in
		    // the original from our matrix

		    result.matrix.put(key, entry1);
		  }
		else
		  {
		    // there is an entry for this field's base.. put it in
		    // so that we properly handle permission inheritance

		    result.matrix.put(key, entry1.union(entry3));
		  }
	      }
	    else
	      {
		result.matrix.put(key, entry1);
	      }
	  }
      }

    // result now contains all of the records from orig, with all of
    // the records from this.matrix union'ed in.  The only problem now
    // is that it's possible that orig contained field records that
    // this.matrix didn't have a match for, which means that we should
    // have unioned in the corresponding base record from this.matrix
    // at that point, but we didn't since we were looping over the
    // entries in this.matrix rather than in orig.

    enum = result.matrix.keys();

    while (enum.hasMoreElements())
      {
	key = enum.nextElement();

	entry1 = (PermEntry) result.matrix.get(key);
	entry2 = (PermEntry) this.matrix.get(key);

	if (entry2 != null)
	  {
	    continue; // we already took care of this above
	  }
	else
	  {
	    if (!isBasePerm((String)key))
	      {
		// This is the case we are concerned with.. the result
		// matrix has a field entry that we don't have a match
		// for in this.matrix.. we need to check to see
		// if we have a base entry for the corresponding base
		// that we need to union in to reflect the default base
		// -> field inheritance

		entry3 = (PermEntry) this.matrix.get(baseEntry((String)key));

		if (entry3 == null)
		  {
		    continue;	// no inheritance to be had here
		  }
		else
		  {
		    // there is an entry for this field's base.. put it in
		    // so that we properly handle permission inheritance

		    result.matrix.put(key, entry1.union(entry3));
		  }
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
}
