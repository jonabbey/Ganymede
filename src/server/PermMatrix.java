/*

   PermMatrix.java

   This class stores a matrix of PermEntry bits, organized by
   object type and field id's.
   
   Created: 3 October 1997
   Version: $Revision: 1.2 $ %D%
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

public class PermMatrix {

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

  public PermMatrix union(PermissionMatrixDBField field)
  {
    PermMatrix result = new PermMatrix(field);
    Enumeration enum;
    PermEntry entry1, entry2;
    Object key;

    /* -- */

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
	    result.matrix.put(key, entry1);
	  }
      }

    return result;
  }

  public synchronized PermMatrix union(PermMatrix orig)
  {
    PermMatrix result = new PermMatrix(orig);
    Enumeration enum;
    PermEntry entry1, entry2;
    Object key;

    /* -- */

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
	    result.matrix.put(key, entry1);
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
    return (PermEntry) matrix.get(matrixEntry(baseID));
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
    try
      {
	return (PermEntry) matrix.get(matrixEntry(base.getTypeID(), field.getID()));
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("caught remote: " + ex);
      }
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
}
