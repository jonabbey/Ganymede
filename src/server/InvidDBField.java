/*
   GASH 2

   InvidDBField.java

   The GANYMEDE object storage system.

   Created: 2 July 1996
   Version: $Revision: 1.50 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import java.io.*;
import java.util.*;
import java.rmi.*;

import arlut.csd.JDialog.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                    InvidDBField

------------------------------------------------------------------------------*/

public final class InvidDBField extends DBField implements invid_field {

  static final boolean debug = false;

  // --

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

    if (isVector())
      {
	values = new Vector();
      }
    else
      {
	values = null;
      }
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
	throw new IllegalArgumentException("scalar value constructor called on vector field " + getName() +
					   " in object " + owner.getLabel());
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
	throw new IllegalArgumentException("vector value constructor called on scalar field " + getName() +
					   " in object " + owner.getLabel());
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
	defined = (values.size() > 0);
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

	try
	  {
	    out.writeShort(temp.getType());
	    out.writeInt(temp.getNum());
	  }
	catch (NullPointerException ex)
	  {
	    System.err.println(owner.getLabel() + ":" + getName() + ": void value in emit");

	    if (temp == null)
	      {
		System.err.println(owner.getLabel() + ":" + getName() + ": field value itself is null");
	      }

	    throw ex;
	  }
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
	throw new IllegalArgumentException("scalar accessor called on vector " + getName() +
					   " in object " + owner.getLabel());
      }

    return (Invid) value;
  }

  public Invid value(int index)
  {
    if (!isVector())
      {
	throw new IllegalArgumentException("vector accessor called on scalar " + getName() +
					   " in object " + owner.getLabel());
      }

    return (Invid) values.elementAt(index);
  }

  public synchronized String getValueString()
  {
    GanymedeSession gsession = null;

    /* -- */

    if (!verifyReadPermission())
      {
	throw new IllegalArgumentException("permission denied to read this field " + getName() +
					   " in object " + owner.getLabel());
      }

    // where will we go to look up the label for our target(s)?

    try
      {
	if (owner.editset != null)
	  {
	    gsession = owner.editset.getSession().getGSession();
	  }
      }
    catch (NullPointerException ex)
      {
      }

    if (gsession == null)
      {
	gsession = Ganymede.internalSession;
      }

    // now do the work

    if (!isVector())
      {
	if (value == null)
	  {
	    return "null";
	  }

	Invid localInvid = (Invid) this.value();

	if (gsession != null)
	  {
	    return gsession.viewObjectLabel(localInvid);
	  }
	else
	  {
	    return this.value().toString();
	  }
      }
    else
      {
	String result = "";
	int size = size();
	Invid tmp;

	for (int i = 0; i < size; i++)
	  {
	    if (i != 0)
	      {
		result = result + ", ";
	      }

	    tmp = this.value(i);

	    if (gsession != null)
	      {
		result = result + gsession.viewObjectLabel(tmp);
	      }
	    else
	      {
		result = result + this.value(i).toString();
	      }
	  }

	return result;
      }
  }

  /**
   *
   * OK, this is a bit vague.. getEncodingString() is used by the new
   * dump system to allow all fields to be properly sorted in the table..
   * a real reversible encoding of an invid field would *not* be the
   * getValueString() results, but getValueString() is what we want in
   * the dump result table, so we'll do that here for now.
   *
   */

  public String getEncodingString()
  {
    return getValueString();
  }

  /**
   *
   * Returns a String representing the change in value between this
   * field and orig.  This String is intended for logging and email,
   * not for any sort of programmatic activity.  The format of the
   * generated string is not defined, but is intended to be suitable
   * for inclusion in a log entry and in an email message.
   *
   * If there is no change in the field, null will be returned.
   * 
   */

  public synchronized String getDiffString(DBField orig)
  {
    StringBuffer result = new StringBuffer();
    InvidDBField origI;
    DBObject object;
    GanymedeSession gsession = null;

    /* -- */

    if (!(orig instanceof InvidDBField))
      {
	throw new IllegalArgumentException("bad field comparison " + getName());
      }

    if (debug)
      {
	System.err.println("Entering InvidDBField getDiffString()");
      }

    if (orig == this)
      {
	return "";
      }

    origI = (InvidDBField) orig;

    try
      {
	if (owner.editset != null)
	  {
	    gsession = owner.editset.getSession().getGSession();
	  }
      }
    catch (NullPointerException ex)
      {
      }
    
    if (gsession == null)
      {
	gsession = Ganymede.internalSession;
      }

    if (isVector())
      {
	Vector 
	  added = new Vector(),
	  deleted = new Vector();

	Enumeration enum;

	Object 
	  element = null;

	Invid
	  elementA = null,
	  elementB = null;

	boolean found = false;

	/* -- */

	if (debug)
	  {
	    System.err.println("vector diff.. searching for deleted items");
	  }

	// find elements in the orig field that aren't in our present field

	Hashtable currentElements = new Hashtable();

	for (int i = 0; !found && i < values.size(); i++)
	  {
	    if (debug)
	      {
		System.err.print(",");
	      }

	    element = values.elementAt(i);

	    currentElements.put(element, element);
	  }

	enum = origI.values.elements();

	while (enum.hasMoreElements())
	  {
	    if (debug)
	      {
		System.err.print("x");
	      }

	    element = enum.nextElement();

	    if (currentElements.get(element) == null)
	      {
		deleted.addElement(element);
	      }
	  }

	// find elements in our present field that aren't in the orig field

	if (debug)
	  {
	    System.err.println("vector diff.. searching for added items");
	  }

	Hashtable origElements = new Hashtable();

	for (int i = 0; !found && i < origI.values.size(); i++)
	  {
	    if (debug)
	      {
		System.err.print(",");
	      }

	    element = origI.values.elementAt(i);
	    
	    origElements.put(element, element);
	  }

	enum = values.elements();

	while (enum.hasMoreElements())
	  {
	    if (debug)
	      {
		System.err.print("x");
	      }

	    element = enum.nextElement();

	    if (origElements.get(element) == null)
	      {
		added.addElement(element);
	      }
	  }

	// were there any changes at all?

	if (deleted.size() == 0 && added.size() == 0)
	  {
	    return null;
	  }
	else
	  {
	    if (deleted.size() != 0)
	      {
		if (debug)
		  {
		    System.err.print("Working out deleted items");
		  }

		result.append("\tDeleted: ");
	    
		for (int i = 0; i < deleted.size(); i++)
		  {
		    elementA = (Invid) deleted.elementAt(i);

		    if (i > 0)
		      {
			result.append(", ");
		      }

		    if (gsession != null)
		      {
			result.append(gsession.viewObjectLabel(elementA));
		      }
		    else
		      {
			result.append(elementA.toString());
		      }
		  }

		result.append("\n");
	      }

	    if (added.size() != 0)
	      {
		if (debug)
		  {
		    System.err.print("Working out added items");
		  }

		result.append("\tAdded: ");
	    
		for (int i = 0; i < added.size(); i++)
		  {
		    elementA = (Invid) added.elementAt(i);

		    if (i > 0)
		      {
			result.append(", ");
		      }

		    if (gsession != null)
		      {
			result.append(gsession.viewObjectLabel(elementA));
		      }
		    else
		      {
			result.append(elementA.toString());
		      }
		  }

		result.append("\n");
	      }

	    return result.toString();
	  }
      }
    else
      {
	if (debug)
	  {
	    System.err.println("InvidDBField: scalar getDiffString() comparison");
	  }

	if (origI.value().equals(this.value()))
	  {
	    return null;
	  }
	else
	  {
	    result.append("\tOld: ");

	    if (gsession != null)
	      {
		result.append(gsession.viewObjectLabel(origI.value()));
	      }
	    else
	      {
		result.append(origI.value().toString());
	      }

	    result.append("\n\tNew: ");

	    if (gsession != null)
	      {
		result.append(gsession.viewObjectLabel(this.value()));
	      }
	    else
	      {
		result.append(this.value().toString());
	      }

	    result.append("\n");
	
	    return result.toString();
	  }
      }
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

  private boolean bind(Invid oldRemote, Invid newRemote, boolean local)
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

    boolean 
      anonymous = false,
      anonymous2 = false;

    /* -- */

    if (newRemote == null)
      {
	setLastError("InvidDBField.bind: null newRemote");
	throw new IllegalArgumentException("null newRemote " + getName() + " in object " + owner.getLabel());
      }

    if (!isEditable(local))
      {
	throw new IllegalArgumentException("not an editable invid field: " + getName() + 
					   " in object " + owner.getLabel());
      }

    eObj = (DBEditObject) this.owner;
    session = eObj.getSession();

    // find out whether there is an explicit back-link field

    if (getFieldDef().isSymmetric())
      {
	// find out what field in remote we might need to update

	targetField = getFieldDef().getTargetField();
      }
    else
      {
	targetField = SchemaConstants.BackLinksField;
      }

    if ((oldRemote != null) && oldRemote.equals(newRemote))
      {
	return true;
      }

    // check out the old object and the new object
    // remove the reference from the old object
    // add the reference to the new object

    if (oldRemote != null)
      {
	// check to see if we have permission to anonymously unlink
	// this field from the target field, else go through the
	// GanymedeSession layer to have our permissions checked.

	// note that if the GanymedeSession layer has already checked out the
	// object, session.editDBObject() will return a reference to that
	// version, and we'll lose our security bypass.. for that reason,
	// we also use the anonymous variable to instruct dissolve to disregard
	// write permissions if we have gotten the anonymous OK

	if (session.getObjectHook(oldRemote).anonymousUnlinkOK(targetField))
	  {
	    oldRef = (DBEditObject) session.editDBObject(oldRemote);
	    anonymous = true;
	  }
	else
	  {
	    oldRef = (DBEditObject) session.getGSession().edit_db_object(oldRemote);
	  }

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
	    setLastError("InvidDBField.bind: invid target field designated in schema is not an invid field");
	    throw new IllegalArgumentException("invid target field designated in schema is not an invid field: " +
					       getName() + " in object " + owner.getLabel());
	  }
	
	if (oldRefField == null)
	  {
	    // editDBObject() will create undefined fields for all fields defined
	    // in the DBObjectBase, so if we got a null result we have a schema
	    // corruption problem.
	    
	    setLastError("InvidDBField.bind: target field not defined in schema:" + targetField);
	    
	    throw new RuntimeException("target field not defined in schema:" + targetField);
	  }
      }

    // check to see if we have permission to anonymously link
    // this field to the target field, else go through the
    // GanymedeSession layer to have our permissions checked.

    // note that if the GanymedeSession layer has already checked out the
    // object, session.editDBObject() will return a reference to that
    // version, and we'll lose our security bypass.. for that reason,
    // we also use the anonymous2 variable to instruct establish to disregard
    // write permissions if we have gotten the anonymous OK
    
    if (session.getObjectHook(newRemote).anonymousLinkOK(targetField))
      {
	newRef = session.editDBObject(newRemote);
	anonymous2 = true;
      }
    else
      {
	newRef = (DBEditObject) session.getGSession().edit_db_object(newRemote);
      }
    
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
	setLastError("invid target field designated in schema is not an invid field");
	throw new IllegalArgumentException("invid target field designated in schema is not an invid field: " +
					   getName() + " in object " + owner.getLabel());
      }
    
    if (newRefField == null)
      {
	// editDBObject() will create undefined fields for all fields defined
	// in the DBObjectBase, so if we got a null result we have a schema
	// corruption problem.
	
	setLastError("target field not defined in schema:" + targetField);
	
	throw new RuntimeException("target field not defined in schema:" + targetField);
      }

    if (oldRefField != null)
      {
 	if (!oldRefField.dissolve(owner.getInvid(), (anonymous||local)))
	  {
	    setLastError("couldn't dissolve old field symmetry with " + oldRef);
	    return false;
	  }
      }
	    
    if (!newRefField.establish(owner.getInvid(), (anonymous2||local)))
      {
	// oops!  try to undo what we did.. this probably isn't critical
	// because something above us will do a rollback, but it's polite.

	if (oldRefField != null)
	  {
	    oldRefField.establish(owner.getInvid(), (anonymous||local)); // hope this works
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

  private boolean unbind(Invid remote, boolean local)
  {
    short targetField;

    DBEditObject 
      eObj = null,
      oldRef = null;

    InvidDBField 
      oldRefField = null;

    DBSession
      session = null;

    // debug vars

    boolean anon = false;

    /* -- */

    if (remote == null)
      {
	throw new IllegalArgumentException("null remote: " + getName() + " in object " + owner.getLabel());
      }

    if (!isEditable(local))
      {
	throw new IllegalArgumentException("not an editable invid field: " + getName() +
					   " in object " + owner.getLabel());
      }

    // find out whether there is an explicit back-link field

    if (getFieldDef().isSymmetric())
      {
	// find out what field in remote we might need to update

	targetField = getFieldDef().getTargetField();
      }
    else
      {
	targetField = SchemaConstants.BackLinksField;
      }

    eObj = (DBEditObject) this.owner;
    session = eObj.getSession();

    // check to see if we have permission to anonymously unlink
    // this field from the target field, else go through the
    // GanymedeSession layer to have our permissions checked.

    // note that if the GanymedeSession layer has already checked out the
    // object, session.editDBObject() will return a reference to that
    // version, and we'll lose our security bypass.. for that reason,
    // we also use the anon variable to instruct dissolve to disregard
    // write permissions if we have gotten the anonymous OK

    if (session.getObjectHook(remote).anonymousUnlinkOK(targetField))
      {
	anon= true;		// debug

	oldRef = session.editDBObject(remote);

	if (oldRef == null)
	  {
	    return true;		// it's not there, so we are certainly unbound, no?
	  }
      }
    else
      {
	oldRef = (DBEditObject) session.getGSession().edit_db_object(remote);

	if (oldRef == null)
	  {
	    if (session.viewDBObject(remote) == null)
	      {
		return true;	// it's not there, so we are certainly unbound, no?
	      }
	    else
	      {
		return false;	// it's there, but we can't unlink it
	      }
	  }
      }

    try
      {
	oldRefField = (InvidDBField) oldRef.getField(targetField);
      }
    catch (ClassCastException ex)
      {
	throw new IllegalArgumentException("invid target field designated in schema is not an invid field " +
					   getName() + " in object " + owner.getLabel());
      }

    if (oldRefField == null)
      {
	// editDBObject() will create undefined fields for all fields defined
	// in the DBObjectBase, so if we got a null result we have a schema
	// corruption problem.
	
	throw new RuntimeException("target field not defined in schema");
      }

    try
      {
	if (!oldRefField.dissolve(owner.getInvid(), anon||local))
	  {
	    setLastError("couldn't dissolve old field symmetry with " + oldRef);
	    return false;
	  }
      }
    catch (IllegalArgumentException ex)
      {
	System.err.println("hm, couldn't dissolve a reference in " + getName());

	if (anon)
	  {
	    System.err.println("Did do an anonymous edit on target");
	  }
	else
	  {
	    System.err.println("Didn't do an anonymous edit on target");
	  }

	throw (IllegalArgumentException) ex;
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

  synchronized boolean dissolve(Invid oldInvid, boolean local)
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

    if (!isEditable(local))
      {
	throw new IllegalArgumentException("dissolve called on non-editable field: " + getName() +
					   " in object " + owner.getLabel());
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
	    Ganymede.debug("warning: dissolve for " + 
			   owner.getLabel() + ":" + getName() + 
			   " called with an unbound invid (vector): " + 
			   oldInvid.toString());

	    return true;	// we're already dissolved, effectively
	  }

	if (eObj.finalizeDeleteElement(this, index))
	  {
	    values.removeElementAt(index);

	    defined = (values.size() > 0 ? true : false);

	    return true;
	  }
	else
	  {
	    setLastError("InvidDBField remote dissolve: couldn't finalizeDeleteElement");
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
	    setLastError("InvidDBField remote dissolve: couldn't finalizeSetValue");
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

  synchronized boolean establish(Invid newInvid, boolean local)
  {
    Invid 
      tmp = null;

    DBEditObject eObj;

    /* -- */

    if (!isEditable(local))
      {
	throw new IllegalArgumentException("establish called on non-editable field: " + getName() +
					   " in object " + owner.getLabel());
      }

    eObj = (DBEditObject) owner;

    if (isVector())
      {
	if (size() >= getMaxArraySize())
	  {
	    setLastError("InvidDBField remote establish: vector overrun");
	    return false;
	  }

	if (eObj.finalizeAddElement(this, newInvid))
	  {
	    values.addElement(newInvid);
	    defined = true;
	    return true;
	  }
	else
	  {
	    setLastError("InvidDBField remote establish: finalize returned false");
	    return false;
	  }
      }
    else
      {
	// ok, since we're scalar, *we* need to be unbound from *our* existing target
	// to be free to point back to our friend who is trying to establish a link
	// to us

	if (value != null)
	  {
	    tmp = (Invid) value;
	    
	    if (tmp.equals(newInvid))
	      {
		return true;	// already linked
	      }

	    if (!unbind(tmp, local))
	      {
		setLastError("InvidDBField remote establish: couldn't unbind old value");
		return false;
	      }
	  }

	if (eObj.finalizeSetValue(this, newInvid))
	  {
	    value = newInvid;
	    defined = true;
	    return true;
	  }
	else
	  {
	    if (!bind(null, tmp, local))	// this should always work
	      {
		throw new RuntimeException("couldn't rebind a value " + tmp + " we just unbound.. sync error");
	      }
	    return false;
	  }
      }
  }

  /**
   *
   * This method tests to see if the invid's held in this InvidDBField
   * are properly back-referenced.
   *
   */

  synchronized boolean test(DBSession session, String objectName)
  {
    Invid temp = null;
    Invid myInvid = getOwner().getInvid();
    short targetField;
    DBObject target;
    InvidDBField backField;
    boolean result = true;

    /* -- */

    // find out what the back-pointer field in the target object is

    if (getFieldDef().isSymmetric())
      {
	targetField = getFieldDef().getTargetField();
      }
    else
      {
	targetField = SchemaConstants.BackLinksField;
      }

    if (isVector())
      {
	// test for all values in our vector

	for (int i = 0; i < values.size(); i++)
	  {
	    temp = (Invid) values.elementAt(i);

	    if (temp == null)
	      {
		Ganymede.debug("HEEEEEYYYYY!!!!!");
	      }

	    // find the object that this invid points to

	    target = session.viewDBObject(temp);

	    if (target == null)
	      {
		Ganymede.debug("*** InvidDBField.test(): Invid pointer to null object located: " + 
			       objectName + " in field " + getName());
		result = false;

		continue;
	      }

	    // find the field that should contain the back-pointer
	    
	    try
	      {
		backField = (InvidDBField) target.getField(targetField);
	      }
	    catch (ClassCastException ex)
	      {
		String fieldName = ((DBField) target.getField(targetField)).getName();

		Ganymede.debug("**** InvidDBField.test(): schema error!  back-reference field not an invid field!!\n\t>" +
			       owner.lookupLabel(target) + ":" + fieldName + ", referenced from " + objectName +
			       ":" + getName());
		result = false;

		continue;
	      }

	    if (backField == null || !backField.defined)
	      {
		Ganymede.debug("InvidDBField.test(): Null backField field in targeted field: " + 
			       objectName + " in field " + getName());
		result = false;

		continue;
	      }

	    if (backField.isVector())
	      {
		if (backField.values == null)
		  {
		    Ganymede.debug("*** InvidDBField.test(): Null back-link invid found for invid " + 
				   temp + " in object " + objectName + " in field " + getName());
		    result = false;
		    
		    continue;
		  }
		else
		  {
		    boolean found = false;
		    Invid testInv;

		    /* -- */

		    for (int j = 0; !found && (j < backField.values.size()); j++)
		      {
			testInv = (Invid) backField.values.elementAt(j);

			if (myInvid.equals(testInv))
			  {
			    found = true;
			  }
		      }

		    if (!found)
		      {
			Ganymede.debug("*** InvidDBField.test(): No back-link invid found for invid " + 
				       temp + " in object " + objectName + ":" + getName() + " in " + 
				       backField.getName());
			result = false;
			
			continue;
		      }
		  }
	      }
	    else
	      {
		if ((backField.value == null) || !(backField.value.equals(myInvid)))
		  {
		    Ganymede.debug("*** InvidDBField.test(): <scalar> No back-link invid found for invid " + 
				   temp + " in object " + objectName + " in field " + getName());
		    result = false;
		    
		    continue;
		  }
	      }
	  }
      }
    else
      {
	temp = (Invid) value;

	if (temp != null)
	  {
	    target = session.viewDBObject(temp);

	    if (target == null)
	      {
		Ganymede.debug("*** InvidDBField.test(): Invid pointer to null object located: " + objectName);
	    
		return false;
	      }

	    try
	      {
		backField = (InvidDBField) target.getField(targetField);
	      }
	    catch (ClassCastException ex)
	      {
		Ganymede.debug("**** InvidDBField.test(): schema error!  back-reference field not an invid field!! " +
			       "field: " + getName() + " in object " + objectName);

		return false;
	      }

	    if (backField == null || !backField.defined)
	      {
		Ganymede.debug("*** InvidDBField.test(): No proper back-reference field in targeted field: " + 
			       objectName + ":" + getName());
	    
		return false;
	      }
	
	    if (backField.isVector())
	      {
		if (backField.values == null)
		  {
		    Ganymede.debug("*** InvidDBField.test(): Null back-link invid found for invid " + 
				   temp + " in object " + objectName + " in field " + getName());
		    
		    return false;
		  }
		else
		  {
		    boolean found = false;
		    Invid testInv;

		    for (int j = 0; !found && (j < backField.values.size()); j++)
		      {
			testInv = (Invid) backField.values.elementAt(j);

			if (myInvid.equals(testInv))
			  {
			    found = true;
			  }
		      }

		    if (!found)
		      {
			Ganymede.debug(">>> InvidDBField.test(): No back-link invid found for invid " + 
				       temp + " in object " + objectName + ":" + getName() + " in " + 
				       backField.getName());

			return false;
		      }
		  }
	      }
	    else
	      {
		if ((backField.value == null) || !(backField.value.equals(myInvid)))
		  {
		    Ganymede.debug("*** InvidDBField.test(): <scalar> No back-link invid found for invid " + 
				   temp + " in object " + objectName + ":" + getName());
		    
		    return false;
		  }
	      }
	  }
      }

    return result;
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
   *
   * The ReturnVal object returned encodes
   * success or failure, and may optionally
   * pass back a dialog.
   *
   * @see arlut.csd.ganymede.DBSession
   *
   */

  public ReturnVal setValue(Object value, boolean local)
  {
    DBEditObject eObj;
    Invid oldRemote, newRemote;

    /* -- */

    if (!isEditable(local))
      {
	throw new IllegalArgumentException("don't have permission to change field /  non-editable object: " +
					   getName() + " in object " + owner.getLabel());
      }

    if (isVector())
      {
	throw new IllegalArgumentException("scalar method called on a vector field: " + getName() +
					   " in object " + owner.getLabel());
      }

    if (!verifyNewValue(value))
      {
	return Ganymede.createErrorDialog("Server: Error in InvidDBField.setValue()",
					  getLastError());
      }

    // we now know that value is an invid
    
    oldRemote = (Invid) this.value;
    newRemote = (Invid) value;

    eObj = (DBEditObject) owner;

    // try to do the binding

    if (newRemote != null)
      {
	if (!bind(oldRemote, newRemote, local))
	  {
	    setLastError("InvidDBField setValue: couldn't bind");

	    return Ganymede.createErrorDialog("Server: Error in InvidDBField.setValue()",
					      "InvidDBField setValue: couldn't bind");
	  }
      }
    else
      {
	if (oldRemote != null)
	  {
	    unbind(oldRemote, local);
	  }
      }

    // check our owner, do it.  Checking our owner should
    // be the last thing we do.. if it returns true, nothing
    // should stop us from running the change to completion

    this.newValue = value;

    if (eObj.finalizeSetValue(this, value))
      {
	this.value = value;

	if (value == null)
	  {
	    defined = false;
	  }
	else
	  {
	    defined = true;
	  }

	this.newValue = null;

	return null;
      }
    else
      {
	this.newValue = null;

	setLastError("InvidDBField setValue: couldn't finalize");

	unbind(newRemote, local);
	bind(null, oldRemote, local);

	return Ganymede.createErrorDialog("Server: Error in InvidDBField.setValue()",
					  "InvidDBField setValue: couldn't finalize");

      }
  }

  /**
   *
   * Sets the value of an element of this field, if a vector.
   *
   * The ReturnVal object returned encodes
   * success or failure, and may optionally
   * pass back a dialog.
   *
   * @see arlut.csd.ganymede.DBSession
   *
   */
  
  public ReturnVal setElement(int index, Object value, boolean local)
  {
    DBEditObject eObj;
    Invid oldRemote, newRemote;

    /* -- */

    if (isEditInPlace())
      {
	throw new IllegalArgumentException("can't manually set element in edit-in-place vector: " +
					   getName() + " in object " + owner.getLabel());
      }

    if (!isEditable(local))
      {
	throw new IllegalArgumentException("don't have permission to change field /  non-editable object: " +
					   getName() + " in object " + owner.getLabel());
      }

    if (!isVector())
      {
	throw new IllegalArgumentException("vector accessor called on scalar field: " +
					   getName() + " in object " + owner.getLabel());
      }

    if ((index < 0) || (index > values.size()))
      {
	throw new IllegalArgumentException("invalid index " + (index) + 
					   getName() + " in object " + owner.getLabel());
      }

    if (!verifyNewValue(value))
      {
	return Ganymede.createErrorDialog("Server: Error in InvidDBField.setElement()",
					  getLastError());
      }

    eObj = (DBEditObject) owner;

    oldRemote = (Invid) values.elementAt(index);
    newRemote = (Invid) value;
    
    // try to do the binding

    if (!bind(oldRemote, newRemote, local))
      {
	return Ganymede.createErrorDialog("Server: Error in InvidDBField.setElement()",
					  getLastError());
      }

    // check our owner, do it.  Checking our owner should
    // be the last thing we do.. if it returns true, nothing
    // should stop us from running the change to completion

    if (eObj.finalizeSetElement(this, index, value))
      {
	values.setElementAt(value, index);
	return null;
      }
    else
      {
	unbind(newRemote, local);
	bind(null, oldRemote, local);

	return Ganymede.createErrorDialog("Server: Error in InvidDBField.setElement()",
					  "InvidDBField setElement: couldn't finalize\n" +
					  getLastError());
      }
  }

  /**
   *
   * Adds an element to the end of this field, if a vector.
   *
   * The ReturnVal object returned encodes
   * success or failure, and may optionally
   * pass back a dialog.
   *
   */

  public ReturnVal addElement(Object value, boolean local)
  {
    DBEditObject eObj;
    Invid remote;

    /* -- */

    if (isEditInPlace())
      {
	throw new IllegalArgumentException("can't manually add element to edit-in-place vector" +
					   getName() + " in object " + owner.getLabel());
      }

    if (!isEditable(local))
      {
	setLastError("don't have permission to change field /  non-editable object");
	throw new IllegalArgumentException("don't have permission to change field /  non-editable object " +
					   getName() + " in object " + owner.getLabel());
      }

    if (!isVector())
      {
	setLastError("vector accessor called on scalar field");
	throw new IllegalArgumentException("vector accessor called on scalar field " +
					   getName() + " in object " + owner.getLabel());
      }

    if (!verifyNewValue(value))
      {
	return Ganymede.createErrorDialog("Server: Error in InvidDBField.addElement()",
					  getLastError());
      }

    if (size() >= getMaxArraySize())
      {
	setLastError("Field " + getName() + " already at or beyond array size limit");

	return Ganymede.createErrorDialog("Server: Error in InvidDBField.addElement()",
					  "Field " + getName() + " already at or beyond array size limit");
      }

    remote = (Invid) value;

    eObj = (DBEditObject) owner;

    if (!bind(null, remote, local))
      {
	setLastError("Couldn't bind reverse pointer");

	return Ganymede.createErrorDialog("Server: Error in InvidDBField.addElement()",
					  "Couldn't bind reverse pointer\n" + getLastError());
      }

    if (eObj.finalizeAddElement(this, value)) 
      {
	values.addElement(value);

	//	if (debug)
	//	  {
	//	    setLastError("InvidDBField debug: successfully added " + value);
	//	  }

	defined = true;		// very important!

	return null;
      } 
    else
      {
	unbind(remote, local);

	return Ganymede.createErrorDialog("Server: Error in InvidDBField.addElement()",
					  "Couldn't finalize\n" + getLastError());
      }
  }

  /**
   *
   * Creates and adds a new embedded object in this
   * field, if it is an edit-in-place vector.
   *
   * Returns an Invid pointing to the newly created
   * and appended embedded object, or null if
   * creation / addition was not possible.
   *
   * @see arlut.csd.ganymede.invid_field
   *
   */

  public Invid createNewEmbedded()
  {
    if (!isEditable(true))
      {
	throw new IllegalArgumentException("don't have permission to change field /  non-editable object: " +
					   getName() + " in object " + owner.getLabel());
      }

    if (!isVector())
      {
	throw new IllegalArgumentException("vector method called on a scalar field " +
					   getName() + " in object " + owner.getLabel());
      }

    if (!isEditInPlace())
      {
	throw new IllegalArgumentException("edit-in-place method called on a referential invid field " +
					   getName() + " in object " + owner.getLabel());
      }

    if (size() >= getMaxArraySize())
      {
	setLastError("Field " + getName() + " already at or beyond array size limit");
	return null;
      }

    DBEditObject eObj = (DBEditObject) owner;

    // have our owner create a new embedded object
    // for us 

    Invid newObj = eObj.createNewEmbeddedObject(this);

    if (newObj == null)
      {
	return null;
      }

    // now we need to do the binding as appropriate
    // note that we assume that we don't need to verify the
    // new value

    DBEditObject embeddedObj = (DBEditObject) owner.editset.getSession().editDBObject(newObj);

    if (embeddedObj.setFieldValue((short) 0, owner.getInvid()) != null)
      {
	setLastError("Couldn't bind reverse pointer");
	return null;
      }

    if (eObj.finalizeAddElement(this, newObj)) 
      {
	values.addElement(newObj);

	if (debug)
	  {
	    setLastError("InvidDBField debug: successfully added " + newObj);
	  }

	defined = true;		// very important!

	return newObj;
      } 
    else
      {
	embeddedObj.setFieldValue((short) 0, null);
	return null;
      }
  }

  /**
   *
   * <p>Return the object type that this invid field is constrained to point to, if set</p>
   *
   * <p>-1 means there is no restriction on target type.</p>
   *
   * <p>-2 means there is no restriction on target type, but there is a specified symmetric field.</p>
   *
   * @see arlut.csd.ganymede.invid_field
   */

  public short getTargetBase()
  {
    return definition.getTargetBase();
  }

  /**
   *
   * Deletes an element of this field, if a vector.
   *
   * Returns null on success, non-null on failure.
   *
   * If non-null is returned, the ReturnVal object
   * will include a dialog specification that the
   * client can use to display the error condition.
   *
   */

  public ReturnVal deleteElement(int index, boolean local)
  {
    ReturnVal retVal = null;
    DBEditObject eObj;
    Invid remote;

    /* -- */

    if (!isEditable(local))
      {
	throw new IllegalArgumentException("don't have permission to change field /  non-editable object " +
					   getName() + " in object " + owner.getLabel());
      }

    if (!isVector())
      {
	throw new IllegalArgumentException("vector accessor called on scalar field " +
					   getName() + " in object " + owner.getLabel());
      }

    if ((index < 0) || (index >= values.size()))
      {
	throw new IllegalArgumentException("invalid index " + index + 
					   getName() + " in object " + owner.getLabel());
      }

    remote = (Invid) values.elementAt(index);

    eObj = (DBEditObject) owner;

    if (!unbind(remote, local))
      {
	return Ganymede.createErrorDialog("Server: Error in InvidDBField.deleteElement()",
					  "Couldn't unbind old value\n" + getLastError());
      }

    if (eObj.finalizeDeleteElement(this, index))
      {
	values.removeElementAt(index);

	// if we are an editInPlace field, unlinking
	// this object means that we should go ahead
	// and delete the object.

	if (getFieldDef().isEditInPlace())
	  {
	    retVal = eObj.getSession().deleteDBObject(remote);
	  }

	if (retVal != null && !retVal.didSucceed())
	  {
	    return retVal;	// go ahead and return our error code
	  }

	if (values.size() > 0)
	  {
	    return null;
	  }
	else
	  {
	    defined = false;

	    return null;
	  }
      }
    else
      {
	bind(null, remote, local);

	return Ganymede.createErrorDialog("Server: Error in InvidDBField.deleteElement()",
					  "Couldn't finalize\n" + getLastError());
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
    return definition.getTargetBase();
  }

  /**
   *
   * Returns a StringBuffer encoded list of the current values
   * stored in this field.
   *
   * @see arlut.csd.ganymede.invid_field
   *
   */

  public QueryResult encodedValues()
  {
    QueryResult results = new QueryResult();
    Invid invid;
    String label;
    DBObject object;
    GanymedeSession gsession = null;

    /* -- */

    if (!isVector())
      {
	throw new IllegalArgumentException("can't call encodedValues on scalar field: " +
					   getName() + " in object " + owner.getLabel());
      }

    try
      {
	if (owner.editset != null)
	  {
	    gsession = owner.editset.getSession().getGSession();
	  }
      }
    catch (NullPointerException ex)
      {
      }

    if (gsession == null)
      {
	gsession = Ganymede.internalSession;
      }

    for (int i = 0; i < values.size(); i++)
      {
	invid = (Invid) values.elementAt(i);
	
	if (gsession != null)
	  {
	    label = gsession.viewObjectLabel(invid);
	  }
	else
	  {
	    label = invid.toString();
	  }
	
	if (label != null)
	  {
	    results.addRow(invid, label, false); // we're not going to report the values as editable here
	  }
      }

    return results;
  }

  /**
   *
   * This method returns true if this invid field should not
   * show any choices that are currently selected in field
   * x, where x is another field in this db_object.
   *
   */

  public boolean excludeSelected(db_field x)
  {
    return ((DBEditObject) owner).excludeSelected(x, this);    
  }

  /**
   *
   * Returns a StringBuffer encoded list of acceptable invid values
   * for this field.
   *
   * @see arlut.csd.ganymede.invid_field
   * 
   */

  public QueryResult choices()
  {
    DBEditObject eObj;

    /* -- */

    if (!isEditable(true))
      {
	throw new IllegalArgumentException("not an editable field: " + 
					   getName() + " in object " + owner.getLabel());
      }

    eObj = (DBEditObject) owner;

    return eObj.obtainChoiceList(this);
  }

  /**
   *
   * This method returns a key that can be used by the client
   * to cache the value returned by choices().  If the client
   * already has the key cached on the client side, it
   * can provide the choice list from its cache rather than
   * calling choices() on this object again.
   *
   * If there is no caching key, this method will return null.
   *
   */

  public Object choicesKey()
  {
    if (owner instanceof DBEditObject)
      {
	return ((DBEditObject) owner).obtainChoicesKey(this);
      }
    else
      {
	return null;
      }
  }

  // ****
  //
  // Overridable methods for implementing intelligent behavior
  //
  // ****

  public boolean verifyTypeMatch(Object o)
  {
    return ((o == null) || (o instanceof Invid));
  }

  public boolean verifyNewValue(Object o)
  {
    DBEditObject eObj;
    Invid i;

    /* -- */

    if (!isEditable(true))
      {
	setLastError("object/field not editable");
	return false;
      }

    eObj = (DBEditObject) owner;

    if (!verifyTypeMatch(o))
      {
	setLastError("type mismatch");
	return false;
      }

    i = (Invid) o;

    if (i == null)
      {
	return eObj.verifyNewValue(this, o);
      }

    if (limited() && (getAllowedTarget() != -2) &&
	(i.getType() != getAllowedTarget()))
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
