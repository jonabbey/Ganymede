/*
   GASH 2

   InvidDBField.java

   The GANYMEDE object storage system.

   Created: 2 July 1996
   Version: $Revision: 1.7 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import java.io.*;
import java.util.*;
import java.rmi.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                    InvidDBField

------------------------------------------------------------------------------*/

public class InvidDBField extends DBField implements invid_field {

  /**
   *
   * Receive constructor.  Used to create a InvidDBField from a DBStore/DBJournal
   * DataInput stream.
   *
   */

  InvidDBField(DBObject owner, DataInput in, DBObjectBaseField definition) throws IOException, RemoteException
  {
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

  InvidDBField(DBObject owner, DBObjectBaseField definition) throws RemoteException
  {
    this.owner = owner;
    this.definition = definition;
    
    defined = false;
    value = null;
    values = null;
  }

  /**
   *
   * Copy constructor.
   *
   */

  public InvidDBField(DBObject owner, InvidDBField field) throws RemoteException
  {
    this.owner = owner;
    definition = field.definition;
    
    if (isVector())
      {
	values = (Vector) field.values.clone();
	value = null;
      }
    else
      {
	value = field.value;
	values = null;
      }

    defined = true;
  }

  /**
   *
   * Scalar value constructor.
   *
   */

  public InvidDBField(DBObject owner, Invid value, DBObjectBaseField definition) throws RemoteException
  {
    if (definition.isArray())
      {
	throw new IllegalArgumentException("scalar value constructor called on vector field");
      }

    this.owner = owner;
    this.definition = null;
    this.value = value;

    if (value != null)
      {
	defined = true;
      }

    values = null;
  }

  /**
   *
   * Vector value constructor.
   *
   */

  public InvidDBField(DBObject owner, Vector values, DBObjectBaseField definition) throws RemoteException
  {
    if (!definition.isArray())
      {
	throw new IllegalArgumentException("vector value constructor called on scalar field");
      }

    this.owner = owner;
    this.definition = definition;
    
    if (values == null)
      {
	this.values = new Vector();
	defined = false;
      }
    else
      {
	this.values = (Vector) values.clone();
	defined = true;
      }

    value = null;
  }
  
  public Object clone()
  {
    try
      {
	return new InvidDBField(owner, this);
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("Couldn't create InvidDBField: " + ex);
      }
  }

  void emit(DataOutput out) throws IOException
  {
    Invid temp;

    /* -- */

    if (isVector())
      {
	out.writeShort(values.size());
	for (int i = 0; i < values.size(); i++)
	  {
	    temp = (Invid) values.elementAt(i);
	    out.writeShort(temp.getType());
	    out.writeInt(temp.getNum());
	  }
      }
    else
      {
	temp = (Invid) value;
	out.writeShort(temp.getType());
	out.writeInt(temp.getNum());
      }
  }

  void receive(DataInput in) throws IOException
  {
    Invid temp;
    int count;

    /* -- */

    if (isVector())
      {
	count = in.readShort();
	values = new Vector(count);
	for (int i = 0; i < count; i++)
	  {
	    temp = new Invid(in.readShort(), in.readInt());
	    values.addElement(temp);
	  }
      }
    else
      {
	value = new Invid(in.readShort(), in.readInt());
      }

    defined = true;
  }

  // ****
  //
  // type-specific accessor methods
  //
  // ****

  public Invid value()
  {
    if (isVector())
      {
	throw new IllegalArgumentException("scalar accessor called on vector");
      }

    return (Invid) value;
  }

  public Invid value(int index)
  {
    if (!isVector())
      {
	throw new IllegalArgumentException("vector accessor called on scalar");
      }

    return (Invid) values.elementAt(index);
  }

  // ****
  //
  // methods for maintaining invid symmetry
  //
  // ****

  /**
   *
   * This method is used to link the remote invid to this checked-out invid
   * in accordance with this field's defined symmetry constraints.
   *
   * This method will extract the objects referenced by the old and new
   * remote parameters, and will cause the appropriate invid dbfields in
   * them to be updated to reflect the change in link status.  If either
   * operation can not be completed, bind will return the system to its
   * pre-bind status and return false.  One or both of the specified
   * remote objects may remain checked out in the current editset until
   * the transaction is committed or released.
   *
   * It is an error for newRemote to be null;  if you wish to undo an
   * existing binding, use the unbind() method call.  oldRemote may
   * be null if this currently holds no value, or if this is a vector
   * field and newRemote is being added.
   *
   * @param oldRemote the old invid to be replaced
   * @param newRemote the new invid to be linked
   *
   * @see unbind
   *
   */

  private boolean bind(Invid oldRemote, Invid newRemote)
  {
    short targetField;

    DBEditObject 
      eObj = null,
      oldRef = null,
      newRef = null;

    InvidDBField 
      oldRefField = null,
      newRefField = null;

    DBSession
      session = null;

    /* -- */

    if (newRemote == null)
      {
	throw new IllegalArgumentException("null newRemote");
      }

    if (!isEditable())
      {
	throw new IllegalArgumentException("not an editable invid field");
      }

    eObj = (DBEditObject) this.owner;
    session = eObj.getSession();

    // find out whether we need to do anything to maintain symmetry

    if (!getFieldDef().isSymmetric())
      {
	return true;
      }

    // find out what field in remote we might need to update

    targetField = getFieldDef().getTargetField();

    if ((oldRemote != null) && oldRemote.equals(newRemote))
      {
	return true;
      }

    // check out the old object and the new object
    // remove the reference from the old object
    // add the reference to the new object

    if (oldRemote != null)
      {
	oldRef = session.editDBObject(oldRemote);

	if (oldRef == null)
	  {
	    setLastError("couldn't check out old invid " + oldRemote + " for symmetry maintenance");
	    return false;
	  }
    
	try
	  {
	    oldRefField = (InvidDBField) oldRef.getField(targetField);
	  }
	catch (ClassCastException ex)
	  {
	    throw new IllegalArgumentException("invid target field designated in schema is not an invid field");
	  }

	if (oldRefField == null)
	  {
	    // editDBObject() will create undefined fields for all fields defined
	    // in the DBObjectBase, so if we got a null result we have a schema
	    // corruption problem.

	    throw new RuntimeException("target field not defined in schema");
	  }
      }

    newRef = session.editDBObject(newRemote);
    
    if (newRef == null)
      {
	setLastError("couldn't check out new invid " + newRemote + " for symmetry maintenance");
	return false;
      }

    try
      {
	newRefField = (InvidDBField) newRef.getField(targetField);
      }
    catch (ClassCastException ex)
      {
	throw new IllegalArgumentException("invid target field designated in schema is not an invid field");
      }

    if (newRefField == null)
      {
	// editDBObject() will create undefined fields for all fields defined
	// in the DBObjectBase, so if we got a null result we have a schema
	// corruption problem.
	
	throw new RuntimeException("target field not defined in schema");
      }

    if (oldRefField != null)
      {
	if (!oldRefField.dissolve(owner.getInvid()))
	  {
	    setLastError("couldn't dissolve old field symmetry with " + oldRef);
	    return false;
	  }
      }
	    
    if (!newRefField.establish(owner.getInvid()))
      {
	if (oldRefField != null)
	  {
	    oldRefField.establish(owner.getInvid()); // hope this works
	  }

	setLastError("couldn't establish field symmetry with " + newRef);
	return false;
      }

    return true;
  }

  /**
   *
   * This method is used to unlink this field from the specified remote
   * invid in accordance with this field's defined symmetry constraints.
   *
   * @param remote An invid for an object to be checked out and unlinked
   *
   */

  private boolean unbind(Invid remote)
  {
    short targetField;

    DBEditObject 
      eObj = null,
      oldRef = null;

    InvidDBField 
      oldRefField = null;

    DBSession
      session = null;

    /* -- */

    if (remote == null)
      {
	throw new IllegalArgumentException("null remote");
      }

    if (!isEditable())
      {
	throw new IllegalArgumentException("not an editable invid field");
      }

    eObj = (DBEditObject) this.owner;
    session = eObj.getSession();

    // find out whether we need to do anything to maintain symmetry

    if (!getFieldDef().isSymmetric())
      {
	return true;
      }

    // find out what field in remote we might need to update

    targetField = getFieldDef().getTargetField();
    oldRef = session.editDBObject(remote);
	
    if (oldRef == null)
      {
	setLastError("couldn't check out old invid " + remote + " for symmetry maintenance");
	return false;
      }
    
    try
      {
	oldRefField = (InvidDBField) oldRef.getField(targetField);
      }
    catch (ClassCastException ex)
      {
	throw new IllegalArgumentException("invid target field designated in schema is not an invid field");
      }

    if (oldRefField == null)
      {
	// editDBObject() will create undefined fields for all fields defined
	// in the DBObjectBase, so if we got a null result we have a schema
	// corruption problem.
	
	throw new RuntimeException("target field not defined in schema");
      }

    if (!oldRefField.dissolve(owner.getInvid()))
      {
	setLastError("couldn't dissolve old field symmetry with " + oldRef);
	return false;
      }
	
    return true;
  }

  /**
   *
   * This method is used to effect the remote side of an unbind operation.
   *
   * An InvidDBField being manipulated with the standard editing accessors
   * (setValue, addElement, deleteElement, setElement) will call this method
   * on another InvidDBField in order to unlink a pair of symmetrically bound
   * InvidDBFields.
   *
   * This method will return false if the unbinding could not be performed for
   * some reason.
   *
   */

  synchronized boolean dissolve(Invid oldInvid)
  {
    int 
      index = -1;

    Invid tmp;

    DBEditObject eObj;

    /* -- */

    // NOTE: WE PROBABLY DON'T WANT TO CALL ISEDITABLE HERE, AS WE PROBABLY WANT
    // TO ALLOW DISSOLVE TO GO FORWARD EVEN IN CASES WHERE THE CURRENT USER WOULDN'T
    // BE ABLE TO EDIT THIS FIELD.. IF A USER'S BEING DELETED, THAT USER SHOULD BE
    // REMOVABLE FROM GROUPS AND WHATNOT REGARDLESS OF WHETHER THE SESSION WOULD HAVE
    // EDIT PERMISSION FOR THE GROUP.

    if (!isEditable())
      {
	throw new IllegalArgumentException("dissolve called on non-editable field");
      }

    eObj = (DBEditObject) owner;

    if (isVector())
      {
	for (int i = 0; i < values.size(); i++)
	  {
	    tmp = (Invid) values.elementAt(i);

	    if (tmp.equals(oldInvid))
	      {
		index = i;
		break;
	      }
	  }

	if (index == -1)
	  {
	    throw new RuntimeException("dissolve called with an unbound invid (vector)");
	  }

	if (eObj.finalizeDeleteElement(this, index))
	  {
	    values.removeElementAt(index);
	    return true;
	  }
	else
	  {
	    return false;
	  }
      }
    else
      {
	tmp = (Invid) value;

	if (!tmp.equals(oldInvid))
	  {
	    throw new RuntimeException("dissolve called with an unbound invid (scalar)");
	  }

	if (eObj.finalizeSetValue(this, null))
	  {
	    value = null;
	    return true;
	  }
	else
	  {
	    return false;
	  }
      }
  }

  /**
   *
   * This method is used to effect the remote side of an bind operation.
   *
   * An InvidDBField being manipulated with the standard editing accessors
   * (setValue, addElement, deleteElement, setElement) will call this method
   * on another InvidDBField in order to link a pair of symmetrically bound
   * InvidDBFields.
   *
   * This method will return false if the binding could not be performed for
   * some reason.
   *
   */

  synchronized boolean establish(Invid newInvid)
  {
    int 
      index = -1;

    Invid 
      tmp = null;

    DBEditObject eObj;

    /* -- */

    if (!isEditable())
      {
	throw new IllegalArgumentException("dissolve called on non-editable field");
      }

    eObj = (DBEditObject) owner;

    if (isVector())
      {
	if (size() >= getMaxArraySize())
	  {
	    return false;
	  }

	if (eObj.finalizeAddElement(this, newInvid))
	  {
	    values.addElement(newInvid);
	    return true;
	  }
	else
	  {
	    return false;
	  }
      }
    else
      {
	if (value != null)
	  {
	    tmp = (Invid) value;
	    
	    if (tmp.equals(newInvid))
	      {
		return true;	// already linked
	      }

	    if (!unbind(tmp))
	      {
		return false;
	      }
	  }

	if (eObj.finalizeSetValue(this, newInvid))
	  {
	    value = newInvid;
	    return true;
	  }
	else
	  {
	    if (!bind(null, tmp))	// this should always work
	      {
		throw new RuntimeException("couldn't rebind a value " + tmp + " we just unbound.. sync error");
	      }
	    return false;
	  }
      }
  }

  // ****
  //
  // InvidDBField is a special kind of DBField in that we have symmetry
  // maintenance issues to handle.  We're overriding all DBField field-changing
  // methods to include symmetry maintenance code.
  //
  // ****

  /**
   *
   * Sets the value of this field, if a scalar.
   * Returns true on success, false on failure.
   * If false is returned, the DBSession's
   * last error value will have been set to
   * indicate the reason for failure.
   *
   * @see arlut.csd.ganymede.DBSession
   * @see arlut.csd.ganymede.db_field
   *
   */

  public boolean setValue(Object value)
  {
    DBEditObject eObj;
    Invid oldRemote, newRemote;

    /* -- */

    if (!isEditable())
      {
	throw new IllegalArgumentException("don't have permission to change field /  non-editable object");
      }

    if (isVector())
      {
	throw new IllegalArgumentException("scalar method called on a vector field");
      }

    if (!verifyNewValue(value))
      {
	return false;
      }

    // we now know that value is an invid
    
    oldRemote = (Invid) this.value;
    newRemote = (Invid) value;

    eObj = (DBEditObject) owner;

    // try to do the binding

    if (!bind(oldRemote, newRemote))
      {
	return false;
      }

    // check our owner, do it.  Checking our owner should
    // be the last thing we do.. if it returns true, nothing
    // should stop us from running the change to completion

    if (eObj.finalizeSetValue(this, value))
      {
	this.value = value;
	return true;
      }
    else
      {
	unbind(newRemote);
	bind(null, oldRemote);
	return false;
      }
  }

  /**
   *
   * Sets the value of an element of this field, if a vector.
   * Returns true on success, false on failure.
   * If false is returned, the DBSession's
   * last error value will have been set to
   * indicate the reason for failure.
   *
   * @see arlut.csd.ganymede.DBSession
   * @see arlut.csd.ganymede.db_field
   *
   */
  
  public boolean setElement(int index, Object value)
  {
    DBEditObject eObj;
    Invid oldRemote, newRemote;

    /* -- */

    if (!isEditable())
      {
	throw new IllegalArgumentException("don't have permission to change field /  non-editable object");
      }

    if (!isVector())
      {
	throw new IllegalArgumentException("vector accessor called on scalar field");
      }

    if ((index < 0) || (index > values.size()))
      {
	throw new IllegalArgumentException("invalid index " + index);
      }

    if (!verifyNewValue(value))
      {
	return false;
      }

    eObj = (DBEditObject) owner;

    oldRemote = (Invid) values.elementAt(index);
    newRemote = (Invid) value;
    
    // try to do the binding

    if (!bind(oldRemote, newRemote))
      {
	return false;
      }

    // check our owner, do it.  Checking our owner should
    // be the last thing we do.. if it returns true, nothing
    // should stop us from running the change to completion

    if (eObj.finalizeSetElement(this, index, value))
      {
	values.setElementAt(value, index);
	return true;
      }
    else
      {
	unbind(newRemote);
	bind(null, oldRemote);
	return false;
      }
  }

  /**
   *
   * Adds an element to the end of this field, if a vector.
   * Returns true on success, false on failure.
   * If false is returned, the DBSession's
   * last error value will have been set to
   * indicate the reason for failure.
   *
   * @see arlut.csd.ganymede.DBSession
   * @see arlut.csd.ganymede.db_field
   *
   */

  public boolean addElement(Object value)
  {
    DBEditObject eObj;
    Invid remote;

    /* -- */

    if (!isEditable())
      {
	throw new IllegalArgumentException("don't have permission to change field /  non-editable object");
      }

    if (!isVector())
      {
	throw new IllegalArgumentException("vector accessor called on scalar field");
      }

    if (!verifyNewValue(value))
      {
	return false;
      }

    if (size() >= getMaxArraySize())
      {
	setLastError("Field " + getName() + " already at or beyond array size limit");
	return false;
      }

    remote = (Invid) value;

    eObj = (DBEditObject) owner;

    if (!bind(null, remote))
      {
	return false;
      }

    if (eObj.finalizeAddElement(this, value)) 
      {
	values.addElement(value);
	return true;
      } 
    else
      {
	unbind(remote);
	return false;
      }
  }

  /**
   *
   * Deletes an element of this field, if a vector.
   * Returns true on success, false on failure.
   * If false is returned, the DBSession's
   * last error value will have been set to
   * indicate the reason for failure.
   *
   * @see arlut.csd.ganymede.DBSession
   * @see arlut.csd.ganymede.db_field
   *
   */

  public boolean deleteElement(int index)
  {
    DBEditObject eObj;
    Invid remote;

    /* -- */

    if (!isEditable())
      {
	throw new IllegalArgumentException("don't have permission to change field /  non-editable object");
      }

    if (!isVector())
      {
	throw new IllegalArgumentException("vector accessor called on scalar field");
      }

    if ((index < 0) || (index >= values.size()))
      {
	throw new IllegalArgumentException("invalid index " + index);
      }

    remote = (Invid) values.elementAt(index);

    eObj = (DBEditObject) owner;

    if (!unbind(remote))
      {
	return false;
      }

    if (eObj.finalizeDeleteElement(this, index))
      {
	values.removeElementAt(index);
	return true;
      }
    else
      {
	bind(null, remote);
	return false;
      }
  }

  // ****
  //
  // invid_field methods
  //
  // ****

  /**
   *
   * Returns true if this invid field may only point to objects
   * of a particular type.
   * 
   * @see arlut.csd.ganymede.invid_field 
   *
   */

  public boolean limited()
  {
    return definition.isTargetRestricted();
  }

  /**
   *
   * Returns the object type permitted for this field if this invid
   * field may only point to objects of a particular type.
   * 
   * @see arlut.csd.ganymede.invid_field 
   * 
   */

  public int getAllowedTarget()
  {
    return definition.getAllowedTarget();
  }

  /**
   *
   * Returns a vector of acceptable invid values for
   * this field. 
   *
   * @see arlut.csd.ganymede.invid_field
   *
   */

  public Vector choices()
  {
    DBEditObject eObj;

    /* -- */

    if (!isEditable())
      {
	throw new IllegalArgumentException("not an editable field");
      }

    eObj = (DBEditObject) owner;

    return eObj.obtainChoiceList(this);
  }

  // ****
  //
  // Overridable methods for implementing intelligent behavior
  //
  // ****

  public boolean verifyTypeMatch(Object o)
  {
    return (o instanceof Invid);
  }

  public boolean verifyNewValue(Object o)
  {
    DBEditObject eObj;
    Invid i;

    /* -- */

    if (!isEditable())
      {
	return false;
      }

    eObj = (DBEditObject) owner;

    if (!verifyTypeMatch(o))
      {
	setLastError("type mismatch");
	return false;
      }

    i = (Invid) o;

    if (limited() && i.getType() != getAllowedTarget())
      {
	// the invid points to an object of the wrong type

	setLastError("invid value " + i + 
		     " points to the wrong kind of" +
		     " object for field " +
		     getName() +
		     " which should point to an" +
		     " object of type " + 
		     getAllowedTarget());
	return false;
      }

    // have our parent make the final ok on the value

    return eObj.verifyNewValue(this, o);
  }
}
