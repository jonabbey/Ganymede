/*

   PermMatrix.java

   This class stores a matrix of PermEntry bits, organized by
   object type and field id's.
   
   Created: 3 October 1997
   Version: $Revision: 1.6 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import java.util.*;
import java.rmi.*;
import java.io.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                      PermMatrix

------------------------------------------------------------------------------*/

public class PermMatrix implements java.io.Serializable {

  public static final boolean debug = false;

  // -- 

  private Hashtable matrix;

  /* -- */

  public PermMatrix()
  {
    matrix = new Hashtable();
  }

  public PermMatrix(PermissionMatrixDBField field)
  {
    if (field.matrix != null)
      {
	this.matrix = (Hashtable) field.matrix.clone();
      }
    else
      {
	this.matrix = new Hashtable();
      }
  }

  public PermMatrix(PermMatrix orig)
  {
    this.matrix = (Hashtable) orig.matrix.clone();
  }

  /**
   *
   * This method combines this PermMatrix with that
   * of orig.  The returned PermMatrix will allow
   * any access either of the source PermMatrices
   * would.
   *
   */

  public PermMatrix union(PermissionMatrixDBField orig)
  {
    return union(new PermMatrix(orig));	// this will cause a redundant copy, but who cares?
  }

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
   *
   * Returns a PermEntry object representing this PermMatrix's 
   * permissions on the field <fieldID> in base <baseID>
   *
   * @see arlut.csd.ganymede.PermMatrix
   */

  public PermEntry getPerm(short baseID, short fieldID)
  {
    return (PermEntry) matrix.get(matrixEntry(baseID, fieldID));
  }

  /**
   *
   * Returns a PermEntry object representing this PermMatrix's 
   * permissions on the base <baseID>
   *
   * @see arlut.csd.ganymede.PermMatrix
   */

  public PermEntry getPerm(short baseID)
  {
    PermEntry result;

    /* -- */

    result = (PermEntry) matrix.get(matrixEntry(baseID));

    return result;
  }

  /**
   *
   * Returns a PermEntry object representing this PermMatrix's 
   * permissions on the field <field> in base <base>
   *
   * @see arlut.csd.ganymede.PermMatrix
   */

  public PermEntry getPerm(Base base, BaseField field)
  {
    PermEntry result;

    /* -- */

    try
      {
	result = (PermEntry) matrix.get(matrixEntry(base.getTypeID(), field.getID()));
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("caught remote: " + ex);
      }

    return result;
  }

  /**
   *
   * Returns a PermEntry object representing this PermMatrix's 
   * permissions on the base <base>
   *
   * @see arlut.csd.ganymede.PermMatrix
   */

  public PermEntry getPerm(Base base)
  {
    PermEntry result;

    /* -- */

    try
      {
	result = (PermEntry) matrix.get(matrixEntry(base.getTypeID()));
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("caught remote: " + ex);
      }

    return result;
  }

  /**
   *
   * Sets the permission entry for this matrix for base <baseID>,
   * field <fieldID> to PermEntry <entry>.
   *
   * This operation will fail if this permission matrix is not
   * associated with a currently checked-out-for-editing
   * PermissionMatrixDBField.
   *
   * @see arlut.csd.ganymede.PermMatrix 
   */

  public synchronized void setPerm(short baseID, short fieldID, PermEntry entry)
  {
    matrix.put(matrixEntry(baseID, fieldID), entry);

    if (debug)
      {
	System.err.println("PermMatrix: base " + baseID + ", field " + fieldID + " set to " + entry);
      }
  }

  /**
   *
   * Sets the permission entry for this matrix for base <baseID>
   * to PermEntry <entry>
   *
   * This operation will fail if this permission matrix is not
   * associated with a currently checked-out-for-editing
   * PermissionMatrixDBField.
   *
   * @see arlut.csd.ganymede.PermMatrix
   */

  public synchronized void setPerm(short baseID, PermEntry entry)
  {
    matrix.put(matrixEntry(baseID), entry);

    if (debug)
      {
	System.err.println("PermMatrix: base " + baseID + " set to " + entry);
      }
  }

  /**
   *
   * Sets the permission entry for this matrix for base <base>,
   * field <field> to PermEntry <entry>
   *
   * This operation will fail if this permission matrix is not
   * associated with a currently checked-out-for-editing
   * PermissionMatrixDBField.
   *
   * @see arlut.csd.ganymede.PermMatrix
   */

  public synchronized void setPerm(Base base, BaseField field, PermEntry entry)
  {
    try
      {
	matrix.put(matrixEntry(base.getTypeID(), field.getID()), entry);
	
	if (debug)
	  {
	    System.err.println("PermMatrix: base " + base.getName() + 
			       ", field " + field.getName() + " set to " + entry);
	  }
      }
    catch (RemoteException ex)
      {
      }
  }

  /**
   *
   * Sets the permission entry for this matrix for base <baseID>
   * to PermEntry <entry>.
   *
   * This operation will fail if this permission matrix is not
   * associated with a currently checked-out-for-editing
   * PermissionMatrixDBField.
   *
   * @see arlut.csd.ganymede.PermMatrix
   */

  public synchronized void setPerm(Base base, PermEntry entry)
  {
    try
      {
	matrix.put(matrixEntry(base.getTypeID()), entry);

	if (debug)
	  {
	    System.err.println("PermMatrix: base " + base.getName() + 
			       " set to " + entry);
	  }
      }
    catch (RemoteException ex)
      {
      }
  }

  private String matrixEntry(short baseID, short fieldID)
  {
    return (baseID + ":" + fieldID);
  }
  
  private String matrixEntry(short baseID)
  {
    return (baseID + "::");
  }

  private boolean isBasePerm(String matrixEntry)
  {
    return (matrixEntry.indexOf(':') != matrixEntry.lastIndexOf(':'));
  }

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
}
