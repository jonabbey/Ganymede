/*

   PermissionMatrixDBField.java

   This class defines the permission matrix field used in the
   'Admin' DBObjectBase class.
   
   Created: 27 June 1997
   Version: $Revision: 1.5 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import java.io.*;
import java.util.*;
import java.rmi.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                         PermissionMatrixDBField

------------------------------------------------------------------------------*/

public class PermissionMatrixDBField extends DBField implements perm_field {

  private Hashtable matrix;

  /* -- */

  /**
   *
   * Receive constructor.  Used to create a PermissionMatrixDBField
   * from a DBStore/DBJournal DataInput stream.
   * 
   */

  PermissionMatrixDBField(DBObject owner, DataInput in, DBObjectBaseField definition) throws IOException, RemoteException
  {
    super();			// initialize UnicastRemoteObject

    value = values = null;
    this.owner = owner;
    this.definition = definition;
    receive(in);
  }

  /**
   *
   * No-value constructor.  Allows the construction of a
   * 'non-initialized' field, for use where the DBObjectBase
   * definition indicates that a given field may be present,
   * but for which no value has been stored in the DBStore.
   *
   * Used to provide the client a template for 'creating' this
   * field if so desired.
   *
   */

  PermissionMatrixDBField(DBObject owner, DBObjectBaseField definition) throws RemoteException
  {
    super();			// initialize UnicastRemoteObject

    this.owner = owner;
    this.definition = definition;
    
    matrix = new Hashtable();
    defined = false;
    value = null;
    values = null;
  }

  /**
   *
   * Copy constructor.
   *
   */

  public PermissionMatrixDBField(DBObject owner, PermissionMatrixDBField field) throws RemoteException
  {
    super();			// initialize UnicastRemoteObject

    value = values = null;

    this.definition = field.definition;
    this.owner = owner;
    this.matrix = (Hashtable) field.matrix.clone();

    defined = true;
  }

  // we never allow setValue

  public boolean verifyTypeMatch(Object v)
  {
    return false;
  }

  // we never allow setValue

  public boolean verifyNewValue(Object v)
  {
    return false;
  }

  // fancy equals method really does check for value equality

  public synchronized boolean equals(Object obj)
  {
    PermissionMatrixDBField pmdb;
    Enumeration keys;
    String key;
    
    /* -- */

    if (!(obj.getClass().equals(this.getClass())))
      {
	return false;
      }

    pmdb = (PermissionMatrixDBField) obj;

    if (matrix.size() != pmdb.matrix.size())
      {
	return false;
      }

    keys = matrix.keys();

    while (keys.hasMoreElements())
      {
	key = (String) keys.nextElement();

	try
	  {
	    if (!(matrix.get(key).equals(pmdb.matrix.get(key))))
	      {
		return false;
	      }
	  }
	catch (NullPointerException ex)
	  {
	    return false;
	  }
      }
    
    return true;
  }

  // we don't really want to hash according to our permission
  // contents, so just hash according to our containing object's
  // i.d.

  public Object key()
  {
    return new Integer(owner.getID());
  }

  // we don't allow setValue.. PermissionMatrixDBField doesn't allow
  // direct setting of the entire matrix.. just use the get() and set()
  // methods below.

  public boolean setValue(Object value)
  {
    return false;
  }

  public Object clone()
  {
    try
      {
	return new PermissionMatrixDBField(owner, this);
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("Couldn't create PermissionMatrixDBField: " + ex);
      }
  }

  synchronized void emit(DataOutput out) throws IOException
  {
    Enumeration keys;
    PermEntry pe;
    String key;

    /* -- */

    out.writeInt(matrix.size());

    keys = matrix.keys();

    while (keys.hasMoreElements())
      {
	key = (String) keys.nextElement();
	pe = (PermEntry) matrix.get(key);

	out.writeUTF(key);
	pe.emit(out);
      }
  }

  synchronized void receive(DataInput in) throws IOException
  {
    int tableSize;
    PermEntry pe;
    String key;

    /* -- */

    tableSize = in.readInt();
    matrix = new Hashtable(tableSize);
    
    for (int i = 0; i < tableSize; i++)
      {
	key = in.readUTF();
	pe = new PermEntry(in);
	matrix.put(key, pe);
      }
  }

  public synchronized String getValueString()
  {
    if (!verifyReadPermission())
      {
	throw new IllegalArgumentException("permission denied to read this field");
      }

    if (!isVector())
      {
	if (value == null)
	  {
	    return "null";
	  }

	return "PermissionMatrix";
      }

    String result = "";
    int size = size();

    for (int i = 0; i < size; i++)
      {
	if (!result.equals(""))
	  {
	    result = result + ", ";
	  }

	result = result + "PermissionMatrix";
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
    if (isEditable())
      {
	matrix.put(matrixEntry(baseID, fieldID), entry);
      }
    else
      {
	throw new IllegalArgumentException("not an editable field");
      }

    defined = true;
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
    if (isEditable())
      {
	matrix.put(matrixEntry(baseID), entry);
      }
    else
      {
	throw new IllegalArgumentException("not an editable field");
      }

    defined = true;
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
    if (isEditable())
      {
	try
	  {
	    matrix.put(matrixEntry(base.getTypeID(), field.getID()), entry);
	  }
	catch (RemoteException ex)
	  {
	  }
      }
    else
      {
	throw new IllegalArgumentException("not an editable field");
      }

    defined = true;
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
    if (isEditable())
      {
	try
	  {
	    matrix.put(matrixEntry(base.getTypeID()), entry);
	  }
	catch (RemoteException ex)
	  {
	  }
      }
    else
      {
	throw new IllegalArgumentException("not an editable field");
      }

    defined = true;
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
