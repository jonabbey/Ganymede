/*
   GASH 2

   DBObject.java

   The GANYMEDE object storage system.

   Created: 2 July 1996
   Version: $Revision: 1.59 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import java.io.*;
import java.util.*;
import java.rmi.*;
import java.rmi.server.*;
import java.lang.reflect.*;
import arlut.csd.Util.*;

import arlut.csd.JDialog.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                        DBObject

------------------------------------------------------------------------------*/

/**
 * <p>Class to hold a database object as represented in the DBStore.</p>
 *
 * <p>Note that we don't include the object type in the object explicitly.. this
 * is encoded by the objectBase reference.</p>
 *
 * <p>A DBObject does not allow its fields to be directly modified by anyone once the
 * object is instantiated.  When a user wants to edit an object, he gets a handle
 * to it from the DBStore, then calls createShadow to get an editable version
 * of the object.  If the user is satisfied with the changes made to the object,
 * the user will cause the editSet to commit, which will get a write lock on
 * the database, then replace the old version of this object with the modified
 * shadow object.  The shadow object field of the replaced object is cleared,
 * and then someone else can pull the object out for editing.</p>
 * 
 * <p>Changes made to the shadow object do not affect the original object until
 * the transaction is committed;  other processes can examine the current
 * contents of the object until commit time.</p>
 *
 * <p>This is kind of like modern dating.</p>
 *
 * <p>The constructors of this object can throw RemoteException because of the
 * UnicastRemoteObject superclass' constructor.</p>
 *
 * @version $Revision: 1.59 $ %D% (Created 2 July 1996)
 * @author Jonathan Abbey, jonabbey@arlut.utexas.edu, ARL:UT
 *
 */

public class DBObject implements db_object, FieldType, Remote {

  static boolean debug = false;

  public static void setDebug(boolean val)
  {
    debug = val;
  }

  /* - */

  /**
   *
   * The type definition for this object.
   *
   */

  protected DBObjectBase objectBase;

  /**
   *
   * 32 bit id - the object's invariant id
   *
   */

  protected int id;

  /**
   *
   * Our field table, essentially a custom hash of DBField objects
   * keyed by their numeric field id's.
   *
   * @see arlut.csd.ganymede.DBField
   *
   */

  protected DBFieldTable fields;

  /**
   *
   * if this object is being edited or removed, this points
   * to the DBEditObject copy that is being edited.  If
   * this object is not being edited, this field will be null,
   * and we are available for someone to edit.
   *
   */

  DBEditObject shadowObject;	

  /**
   * transaction that this object has been checked out in
   * care of, if any.
   */

  protected DBEditSet editset;	

  /**
   * if this object is being viewed by a particular
   * Ganymede Session, we record that here.
   */

  protected GanymedeSession gSession;

  /** 
   * A fixed copy of our Invid, so that we don't have to create
   * new ones all the time when people call getInvid() on us.
   */

  Invid myInvid = null;

  /** 
   * used by the DBObjectTable logic
   */

  DBObject next = null;

  /* -- */

  /**
   *
   * No param constructor, here to allow DBEditObject to have
   * a no-param constructor for a static method handle
   *
   */

  public DBObject()
  {
    gSession = null;
  }

  /**
   *
   * Base constructor, used to create a new object of
   * type objectBase.  Note that DBObject itself is
   * a mere carrier of data and there is nothing application
   * type specific in a base DBObject.  The only type
   * information is represented by the DBObjectBase passed
   * in to this constructor.
   *
   */

  DBObject(DBObjectBase objectBase)
  {
    this.objectBase = objectBase;
    id = 0;
    fields = null;

    shadowObject = null;
    editset = null;

    myInvid = new Invid(objectBase.type_code, id);
    gSession = null;
  }

  /**
   *
   * Constructor to create an object of type objectBase
   * with the specified object number.
   *
   */

  DBObject(DBObjectBase objectBase, int id)
  {
    this(objectBase);
    this.id = id;
    myInvid = new Invid(objectBase.type_code, id);
    gSession = null;
  }

  /**
   *
   * Read constructor.  Constructs an objectBase from a
   * DataInput stream.
   *
   */

  DBObject(DBObjectBase objectBase, DataInput in, boolean journalProcessing) throws IOException
  {
    this.objectBase = objectBase;
    shadowObject = null;
    editset = null;
    receive(in, journalProcessing);
    gSession = null;
  }

  /**
   *
   * <p>This constructor is used to create a non-editable DBObject from a
   * DBEditObject that we have finished editing.  Whenever a
   * transaction checks a created or edited shadow back into the
   * DBStore, it actually does so by creating a new DBObject to
   * replace any previous version of the object in the DBStore.</p>
   *
   * @param eObj The shadow object to copy into the new DBObject
   *
   * @see arlut.csd.ganymede.DBEditSet#commit()
   * @see arlut.csd.ganymede.DBEditSet#release()
   * 
   */
  
  DBObject(DBEditObject eObj)
  {
    Enumeration enum;
    DBField field;

    /* -- */

    objectBase = eObj.objectBase;
    id = eObj.id;
    myInvid = eObj.myInvid;

    shadowObject = null;
    editset = null;

    fields = new DBFieldTable(objectBase.fieldTable.size(), (float) 1.0);

    // put any defined fields into the object we're going
    // to commit back into our DBStore

    enum = eObj.fields.elements();

    while (enum.hasMoreElements())
      {
	field = (DBField) enum.nextElement();

	if (field.isDefined())
	  {
	    field.setOwner(this); // this will make the field non-editable
	    fields.putNoSyncNoRemove(field);
	  }
	else
	  {
	    Ganymede.debug("DBObject check-in: rejecting undefined field " + field.getName());
	  }
      }

    gSession = null;
  }

  /**
   *
   * This is a view-copy constructor, designed to make a view-only
   * duplicate of an object from the database.  This view-only object
   * knows who is looking at it through its GanymedeSession reference,
   * and so can properly enforce field access permissions.<br><br>
   *
   * &lt;gSession&gt; may be null, in which case the returned DBObject
   * will be simply an un-linked fresh copy of &lt;original&gt;.
   *
   */

  public DBObject(DBObject original, GanymedeSession gSession)
  {
    Enumeration enum;
    DBField field, copy;

    /* -- */

    objectBase = original.objectBase;
    id = original.id;
    myInvid = original.myInvid;

    shadowObject = null;
    editset = null;

    fields = new DBFieldTable(original.fields.size(), (float) 1.0);

    // put any defined fields into the object we're going
    // to commit back into our DBStore

    enum = original.fields.elements();

    while (enum.hasMoreElements())
      {
	field = (DBField) enum.nextElement();

	switch (field.getType())
	  {
	  case BOOLEAN:
	    copy = new BooleanDBField(this, (BooleanDBField) field);

	    copy.setOwner(this);
	    fields.putNoSyncNoRemove(copy);

	    break;

	  case NUMERIC:
	    copy = new NumericDBField(this, (NumericDBField) field);

	    copy.setOwner(this);
	    fields.putNoSyncNoRemove(copy);

	    break;

	  case DATE:
	    copy = new DateDBField(this, (DateDBField) field);

	    copy.setOwner(this);
	    fields.putNoSyncNoRemove(copy);

	    break;

	  case STRING:
	    copy = new StringDBField(this, (StringDBField) field);

	    copy.setOwner(this);
	    fields.putNoSyncNoRemove(copy);

	    break;

	  case INVID:
	    copy = new InvidDBField(this, (InvidDBField) field);

	    copy.setOwner(this);
	    fields.putNoSyncNoRemove(copy);

	    break;

	  case PERMISSIONMATRIX:
	    copy = new PermissionMatrixDBField(this, (PermissionMatrixDBField) field);

	    copy.setOwner(this);
	    fields.putNoSyncNoRemove(copy);

	    break;
	    
	  case PASSWORD:
	    copy = new PasswordDBField(this, (PasswordDBField) field);

	    copy.setOwner(this);
	    fields.putNoSyncNoRemove(copy);

	    break;

	  case IP:
	    copy = new IPDBField(this, (IPDBField) field);

	    copy.setOwner(this);
	    fields.putNoSyncNoRemove(copy);

	    break;
	  }
      }

    this.gSession = gSession;
  }

  /**
   *
   * This method makes the fields in this object remotely accessible.
   * Used by GanymedeSession when it provides a DBObject to the
   * client.
   *  
   */

  public final void exportFields()
  {
    Enumeration enum = fields.elements();
    DBField field;

    while (enum.hasMoreElements())
      {
	field = (DBField) enum.nextElement();

	// export can fail if the object has already
	// been exported.. don't worry about it if
	// it happens.. the client will know about it
	// if we try to pass a non-exported object
	// back to it, anyway.

	try
	  {
	    UnicastRemoteObject.exportObject(field);
	  }
	catch (RemoteException ex)
	  {
	  }
      }
  }

  public final int hashCode()
  {
    return id;
  }

  /**
   *
   * Returns the numeric id of the object in the objectBase
   *
   * @see arlut.csd.ganymede.db_object
   */

  public final int getID()
  {
    return id;
  }

  /**
   *
   * Returns the invid of this object
   * for the db_object remote interface
   *
   * @see arlut.csd.ganymede.db_object
   */

  public final Invid getInvid()
  {
    return myInvid;
  }

  /**
   *
   * Returns the numeric id of the object's objectBase
   *
   * @see arlut.csd.ganymede.db_object
   */

  public final short getTypeID()
  {
    return objectBase.type_code;
  }

  /**
   *
   * Returns the name of the object's objectBase
   *
   * @see arlut.csd.ganymede.db_object
   */

  public final String getTypeName()
  {
    return objectBase.getName();
  }

  /**
   *
   * Returns the data dictionary for this object
   *
   */

  public final DBObjectBase getBase()
  {
    return objectBase;
  }

  /**
   *
   * Returns the primary label of this object.. 
   * calls DBEditObject.getLabelHook() to get the
   * label for this object.
   *
   * This base implementation just gives a generic
   * label for the object.
   *
   * @see arlut.csd.ganymede.db_object
   *
   */

  public String getLabel()
  {
    String result;

    result = objectBase.objectHook.getLabelHook(this);

    if (result == null)
      {
	// no class for this object.. just go
	// ahead and use the default label
	// obtaining bit

	short val = objectBase.getLabelField();

	if (val == -1)
	  {
	    //	    Ganymede.debug("val == -1");
	    return "<" + getTypeDesc() + ":" + getID() + ">";
	  }
	else
	  {
	    // Ganymede.debug("Getting field " + val + " for label");

	    DBField f = (DBField) getField(val);

	    if (f != null)
	      {
		// Ganymede.debug("Got field " + f);

		// string fields are most common for 
		// label fields.. return as quickly as possible,
		// without bothering with permission checking
		// for this common case.

		if (f.value instanceof String)
		  {
		    return (String) f.value;
		  }
		else
		  {
		    return f.getValueString();
		  }
	      }
	    else
	      {
		// Ganymede.debug("Couldn't find field " + val);
		return "<" + getTypeDesc() + ":" + getID() + ">";
	      }
	  }
      }
    else
      {
	return result;
      }
  }

  /**
   *
   * <p>Get access to the field that serves as this object's label</p>
   *
   * <p>Not all objects use simple field values as their labels.  If an
   * object has a calculated label, this method will return null.</p>
   *
   */

  public db_field getLabelField()
  {
    String result = objectBase.objectHook.getLabelHook(this);

    if (result == null)
      {
	// no class for this object.. just go
	// ahead and use the default label
	// obtaining bit

	short val = objectBase.getLabelField();

	if (val == -1)
	  {
	    return null;
	  }
	else
	  {
	    // Ganymede.debug("Getting field " + val + " for label");

	    DBField f = (DBField) getField(val);

	    if (f != null)
	      {
		return f;
	      }
	  }
      }

    return null;
  }

  /**
   *
   * Returns true if this object is an embedded type.
   *
   * @see arlut.csd.ganymede.db_object
   *
   */

  public boolean isEmbedded()
  {
    return objectBase.isEmbedded();
  }

  /**
   *
   * Returns the string of the object's type
   *
   */

  public String getTypeDesc()
  {
    return objectBase.object_name;
  }

  /**
   *
   * The partialEmit() method is used when the server is doing
   * a limited schema dump.  partialEmit() will look at the
   * type of the object represented by this DBObject object and
   * will choose how to restrict the fields emitted in order
   * to not leave spare invid links in place that are typically
   * not to be emitted in a schema dump.<br><br>
   *
   * This method is really of a piece with DBObjectBase.partialEmit().<br><br>
   *
   * And, this method is really a hack.  I intend to ditch this as
   * soon as possible and replace it with a separate cleaner executable
   * which will load a schema file and delete any invid's that refer
   * to objects not present in the schema file.  Even that would be
   * something of a hack, given that we could have some other object
   * deletion tasks that would logically need to be carried out, but
   * I'm not yet ready to commit to having the schema dump routine
   * actually delete all objects not desired for the schema dump.
   * 
   * @param out A DBStore writing stream.
   * 
   */

  synchronized void partialEmit(DataOutput out) throws IOException
  {
    Enumeration enum;
    Short key;
    short keynum;
    DBField field;

    int counter = 0;
    Vector fieldsToEmit = new Vector();

    /* -- */

    //    System.err.println("Partial Emitting " + objectBase.getName() + " <" + id + ">");

    out.writeInt(id);

    if (objectBase.getTypeID() == SchemaConstants.OwnerBase)
      {
	fieldsToEmit.addElement(new Short(SchemaConstants.OwnerNameField));
	fieldsToEmit.addElement(new Short(SchemaConstants.OwnerMembersField));

	// omit OwnerObjectsOwned
      }

    enum = fields.elements();

    while (enum.hasMoreElements())
      {
	field = (DBField) enum.nextElement();
	key = new Short(field.getID());

	if (fieldsToEmit.contains(key))
	  {
	    counter++;
	  }
      }
    
    out.writeShort(counter);    

    //    System.err.println("emitting fields");
   
    enum = fields.elements();

    while (enum.hasMoreElements())
      {
	field = (DBField) enum.nextElement();
	keynum = field.getID();
	key = new Short(keynum);

	if (fieldsToEmit.contains(key))
	  {
	    out.writeShort(key.shortValue());

	    if (key.shortValue() == SchemaConstants.OwnerMembersField)
	      {
		// only want to emit the persona objects we're keeping,
		// which would be supergash and monitor

		DBField oldF = (DBField) fields.get(keynum);

		if (!(oldF instanceof InvidDBField))
		  {
		    Ganymede.debug("Error in DBObject.partialEmit(): expected SchemaConstants.OwnerMembersField to be invidfield");
		    ((DBField) fields.get(keynum)).emit(out);
		  }
		else
		  {
		    InvidDBField invF = new InvidDBField(this, (InvidDBField) oldF);

		    if (!invF.isVector())
		      {
			Ganymede.debug("Error in DBObject.partialEmit(): expected SchemaConstants.OwnerMembersField to be vector");
			((DBField) fields.get(keynum)).emit(out);
		      }
		    else
		      {
			invF.values = new Vector();

			invF.values.addElement(new Invid(SchemaConstants.PersonaBase, 0)); // 0 is supergash
			invF.values.addElement(new Invid(SchemaConstants.PersonaBase, 1)); // 1 monitor

			invF.emit(out);
		      }
		  }
	      }
	    else
	      {
		((DBField) fields.get(keynum)).emit(out);
	      }
	  }
      }
  }
  
  /**
   *
   * The emit() method is part of the process of dumping the DBStore
   * to disk.  emit() dumps an object in its entirety to the
   * given out stream.
   *
   * @param out A journal or DBStore writing stream.
   *
   */

  synchronized void emit(DataOutput out) throws IOException
  {
    Enumeration enum;
    DBField field;

    /* -- */

    //    System.err.println("Emitting " + objectBase.getName() + " <" + id + ">");

    out.writeInt(id);

    if (fields.size() == 0)
      {
	System.err.println("**** Error: writing object with no fields: " + objectBase.getName() + " <" + id + ">");
      }

    out.writeShort(fields.size());

    //    System.err.println("emitting fields");
   
    enum = fields.elements();

    while (enum.hasMoreElements())
      {
	field = (DBField) enum.nextElement();
	out.writeShort(field.getID());
	field.emit(out);
      }
  }

  /**
   *
   * The receive() method is part of the process of loading the DBStore
   * from disk.  receive() reads an object from the given in stream and
   * instantiates it into the DBStore.
   *
   */

  synchronized void receive(DataInput in, boolean journalProcessing) throws IOException
  {
    DBField 
      tmp = null;

    DBObjectBaseField 
      definition;

    short 
      fieldcode,
      type;

    int 
      tmp_count;

    /* -- */

    // get our unique id

    id = in.readInt();
    myInvid = new Invid(objectBase.type_code, id);

    // get number of fields

    tmp_count = in.readShort();

    if (debug && tmp_count == 0)
      {
	System.err.println("DBObject.receive(): No fields reading object " + id);
      }

    if (tmp_count > 0)
      {
	fields = new DBFieldTable(tmp_count, (float) 1.0);
      }
    else
      {
	fields = new DBFieldTable(objectBase.fieldTable.size(), (float) 1.0);
      }

    for (int i = 0; i < tmp_count; i++)
      {
	// read our field code, look it up in our
	// DBObjectBase

	fieldcode = in.readShort();
	definition = objectBase.fieldTable.get(fieldcode);

	if (definition == null)
	  {
	    System.err.println("What the heck?  Null definition for " + 
			       objectBase.getName() + ", fieldcode = " + fieldcode +
			       ", " + i + "th field in object");
	  }
	else
	  {
	    //	    System.err.println("Reading " + definition.getName() + " " + definition.getTypeDesc());
	  }

	type = definition.getType();

	switch (type)
	  {
	  case BOOLEAN:
	    tmp = new BooleanDBField(this, in, definition);
	    break;

	  case NUMERIC:
	    tmp = new NumericDBField(this, in, definition);
	    break;

	  case DATE:
	    tmp = new DateDBField(this, in, definition);
	    break;

	  case STRING:
	    tmp = new StringDBField(this, in, definition);
	    break;

	  case INVID:
	    tmp = new InvidDBField(this, in, definition);
	    break;

	  case PERMISSIONMATRIX:
	    tmp = new PermissionMatrixDBField(this, in, definition);
	    break;

	  case PASSWORD:
	    tmp = new PasswordDBField(this, in, definition);
	    break;

	  case IP:
	    tmp = new IPDBField(this, in, definition);
	    break;
	  }

	if (tmp == null)
	  {
	    throw new Error("Don't recognize field type in datastore");
	  }

	if (!journalProcessing && (definition.namespace != null))
	  {
	    if (tmp.isVector())
	      {
		// mark the elements in the vector in the namespace
		// note that we don't use the namespace mark method here, 
		// because we are just setting up the namespace, not
		// manipulating it in the context of an editset

		for (int j = 0; j < tmp.size(); j++)
		  {
		    if (definition.namespace.uniqueHash.containsKey(tmp.key(j)))
		      {
			throw new RuntimeException("Duplicate unique value detected in vector field: " + tmp.key(j));
		      } 

		    definition.namespace.uniqueHash.put(tmp.key(j), new DBNameSpaceHandle(null, true, tmp));
		  }
	      }
	    else
	      {
		// mark the scalar value in the namespace
		
		if (definition.namespace.uniqueHash.containsKey(tmp.key()))
		  {
		    throw new RuntimeException("Duplicate unique value detected in scalar field: " + tmp.key());
		  }

		definition.namespace.uniqueHash.put(tmp.key(), new DBNameSpaceHandle(null, true, tmp));
	      }
	  }
	
	// now add the field to our fields table

	if (tmp.isDefined())
	  {
	    fields.putNoSyncNoRemove(tmp);
	  }
	else
	  {
	    System.err.println("%%% Loader skipping " + definition.getName());
	  }
      }
  }

  /**
   *
   * <p>Check this object out from the datastore for editing.  This
   * method is intended to be called by the editDBObject method in
   * DBSession.. createShadow should not be called on an arbitrary
   * viewed object in other contexts.. probably should do something to
   * guarantee this?</p>
   *
   * <p>If this object is being edited, we say that it has a shadow
   * object; a session gets a copy of this object.. the copy is
   * actually a DBEditObject, which has the intelligence to allow the
   * client to modify the (copies of the) data fields.</p>
   *
   * <p>note: this is only used for editing pre-existing objects..
   * the code for creating new objects is in DBSession..  this method
   * might be better incorporated into DBSession as well.</p>
   * 
   * @param editset The transaction to own this shadow.
   *
   */

  synchronized DBEditObject createShadow(DBEditSet editset)
  {
    if (shadowObject != null)
      {
	// this object has already been checked out
	// for editing / deleting

	return null;
      }

    // if we are a customized object type, dynamically invoke
    // the proper check-out constructor for the DBEditObject
    // subtype.

    if (objectBase.classdef != null)
      {
	Constructor c;
	Class classArray[];
	Object parameterArray[];

	classArray = new Class[2];

	classArray[0] = this.getClass();
	classArray[1] = editset.getClass();

	parameterArray = new Object[2];

	parameterArray[0] = this;
	parameterArray[1] = editset;

	String error_code = null;

	try
	  {
	    c = objectBase.classdef.getDeclaredConstructor(classArray);
	    shadowObject = (DBEditObject) c.newInstance(parameterArray);
	  }
	catch (NoSuchMethodException ex)
	  {
	    error_code = "NoSuchMethod Exception";
	  }
	catch (SecurityException ex)
	  {
	    error_code = "Security Exception";
	  }
	catch (IllegalAccessException ex)
	  {
	    error_code = "Illegal Access Exception";
	  }
	catch (IllegalArgumentException ex)
	  {
	    error_code = "Illegal Argument Exception";
	  }
	catch (InstantiationException ex)
	  {
	    error_code = "Instantiation Exception";
	  }
	catch (InvocationTargetException ex)
	  {
	    InvocationTargetException tex = (InvocationTargetException) ex;

	    tex.getTargetException().printStackTrace();

	    error_code = "Invocation Target Exception " + tex.getTargetException();
	  }

	if (error_code != null)
	  {
	    // note that we know editset is set here, so we find our GanymedeSession
	    // instance through the editset since we may not be explicitly checked out
	    // for viewing

	    editset.getSession().setLastError("createNewObject failure: " + error_code +
					      " in trying to check out custom object");
	    return null;
	  }
      }
    else
      {
	shadowObject = new DBEditObject(this, editset);
      }

    editset.addObject(shadowObject);

    objectBase.store.checkOut(); // update checked out count

    return shadowObject;
  }

  /**
   * <p>This method is the complement to createShadow, and
   * is used during editset release.</p>
   *
   * @param editset The transaction owning this object's shadow.
   *
   * @see arlut.csd.ganymede.DBEditSet#release()
   *
   */

  synchronized boolean clearShadow(DBEditSet editset)
  {
    if (editset != shadowObject.editset)
      {
	// couldn't clear the shadow..  this editSet
	// wasn't the one to create the shadow

	Ganymede.debug("DBObject.clearShadow(): couldn't clear, editset mismatch");

	return false;
      }

    shadowObject = null;

    objectBase.store.checkIn(); // update checked out count

    return true;
  }

  /**
   *
   * Returns the transaction object owning this object, or
   * null if an unowned data object.
   *
   * Note that this is public, but not made available
   * to the client via a remote interface.
   *
   */

  public DBEditSet getEditSet()
  {
    return editset;
  }

  /**
   *
   * <p>Get read-only list of DBFields contained in this object.</p>
   *
   * @see arlut.csd.ganymede.db_object
   */

  synchronized public Vector getFieldInfoVector(boolean customOnly)
  {
    Vector results = new Vector();
    Enumeration enum;
    DBField field;

    /* -- */

    enum = fields.elements();
    
    while (enum.hasMoreElements())
      {
	field = (DBField) enum.nextElement();

	if (!(field.isBuiltIn() && customOnly))
	  {
	    results.addElement(new FieldInfo(field));
	  }
      }

    // sort by display order

    (new VecQuickSort(results,
		      new arlut.csd.Util.Compare()
		      {
			public int compare(Object a, Object b)
			  {
			    FieldInfo aF, bF;

			    aF = (FieldInfo) a;
			    bF = (FieldInfo) b;

			    if (aF.displayOrder < bF.displayOrder)
			      {
				return -1;
			      }
			    else if (aF.displayOrder > bF.displayOrder)
			      {
				return 1;
			      }
			    else
			      {
				return 0;
			      }
		       }
		   }
		   )).sort();

    return results;
  }

  /**
   *
   * <p>Get read-only access to a field from this object.</p>
   *
   * @param id The field code for the desired field of this object.
   *
   *
   * @see arlut.csd.ganymede.db_object
   */

  public final db_field getField(short id)
  {
    return fields.get(id);
  }

  /**
   *
   * <p>Get read-only access to a field from this object, by name.</p>
   *
   * This method needs to be synchronized to avoid conflict with 
   * DBEditObject.clearTransientFields().
   *
   * @param fieldname The fieldname for the desired field of this object
   *
   * @see arlut.csd.ganymede.db_object
   */

  synchronized public db_field getField(String fieldname)
  {
    Enumeration enum;
    DBField field;

    /* -- */

    enum = fields.elements();

    while (enum.hasMoreElements())
      {
	field = (DBField) enum.nextElement();

	if (field.getName().equalsIgnoreCase(fieldname))
	  {
	    return field;
	  }
      }

    return null;
  }

  /**
   *
   * <p>Get read-only list of DBFields contained in this object.</p>
   *
   * This method needs to be synchronized to avoid conflict with 
   * DBEditObject.clearTransientFields().
   *
   * @see arlut.csd.ganymede.db_object
   */

  public synchronized db_field[] listFields(boolean customOnly)
  {
    db_field[] results;
    Enumeration enum;
    int count = 0;
    DBField localField;

    /* -- */

    if (customOnly)
      {
	enum = fields.elements();

	while (enum.hasMoreElements())
	  {
	    if (!((DBField) enum.nextElement()).isBuiltIn())
	      {
		count++;
	      }
	  }
      }
    else
      {
	count = fields.size();
      }
    
    results = new db_field[count];

    // note that a hash doesn't keep the fields in any particular
    // order..

    enum = fields.elements();

    while (enum.hasMoreElements())
      {
	if (customOnly)
	  {
	    localField = (DBField) enum.nextElement();

	    if (!localField.isBuiltIn())
	      {
		results[--count] = localField;
	      }
	  }
	else
	  {
	    results[--count] = (db_field) enum.nextElement();
	  }
      }

    if (count != 0)
      {
	throw new RuntimeException("synchronization error, fields hash modified");
      }

    // sort by display order

    (new QuickSort(results,  
		   new arlut.csd.Util.Compare()
		   {
		     public int compare(Object a, Object b) 
		       {
			 db_field aF, bF;
			 
			 aF = (db_field) a;
			 bF = (db_field) b;
			 
			 try
			   {
			     if (aF.getDisplayOrder() < bF.getDisplayOrder())
			       {
				 return -1;
			       }
			     else if (aF.getDisplayOrder() > bF.getDisplayOrder())
			       {
				 return 1;
			       }
			     else
			       {
				 return 0;
			       }
			   }
			 catch (RemoteException ex)
			   {
			     throw new RuntimeException("couldn't get field ID in sort" + ex);
			   }
		       }
		   }
		   )).sort();

    return results;
  }

  /**
   *
   * <p>Returns true if inactivate() is a valid operation on
   * checked-out objects of this type.</p>
   *
   * @see arlut.csd.ganymede.db_object
   */

  public boolean canInactivate()
  {
    return objectBase.canInactivate();
  }
  
  /**
   *
   * <p>Returns true if this object has been inactivated and is
   * pending deletion.</p>
   *
   * @see arlut.csd.ganymede.db_object
   */

  public boolean isInactivated()
  {
    return (objectBase.canInactivate() && 
	    (getFieldValueLocal(SchemaConstants.RemovalField) != null));
  }

  /**
   *
   * <p>Returns the date that this object is to go through final removal
   * if it has been inactivated.</p>
   *
   * @see arlut.csd.ganymede.db_object
   */

  public Date getRemovalDate()
  {
    DateDBField dbf = (DateDBField) getField(SchemaConstants.RemovalField);
    
    if (dbf == null)
      {
	return null;
      }

    return dbf.value();
  }

  /**
   *
   * <p>Returns true if this object has an expiration date set.</p>
   *
   * @see arlut.csd.ganymede.db_object
   */

  public boolean willExpire()
  {
    return (getFieldValueLocal(SchemaConstants.ExpirationField) != null);
  }

  /**
   *
   * <p>Returns true if this object has a removal date set.</p>
   *
   * @see arlut.csd.ganymede.db_object
   */

  public boolean willBeRemoved()
  {
    return (getFieldValueLocal(SchemaConstants.RemovalField) != null);
  }

  /**
   *
   * <p>Returns the date that this object is to be automatically
   * inactivated if it has an expiration date set.</p>
   *
   * @see arlut.csd.ganymede.db_object
   */

  public Date getExpirationDate()
  {
    DateDBField dbf = (DateDBField) getField(SchemaConstants.ExpirationField);
    
    if (dbf == null)
      {
	return null;
      }

    return dbf.value();
  }

  /**
   *
   * Shortcut method to set a field's value.  Using this
   * method saves a roundtrip to the server, which is
   * particularly useful in database loading.
   *
   * @see arlut.csd.ganymede.db_object
   */

  public ReturnVal setFieldValue(short fieldID, Object value)
  {
    // we override in DBEditObject

    return Ganymede.createErrorDialog("Server: Error in DBObject.setFieldValue()",
				      "setFieldValue called on a non-editable object");
  }

  /**
   *
   * Shortcut method to get a field's value.  Using this
   * method saves a roundtrip to the server, which is
   * particularly useful in database loading.
   *
   * @see arlut.csd.ganymede.db_object
   */

  public Object getFieldValue(short fieldID)
  {
    DBField f = (DBField) getField(fieldID);

    /* -- */
    
    if (f == null)
      {
	return null;
      }

    if (f.isVector())
      {
	if (gSession != null)
	  {
	    gSession.setLastError("couldn't get scalar value on vector field " + fieldID);
	  }

	return null;
      }

    return f.getValue();
  }

  /**
   *
   * Shortcut method to get a field's value.  Used only
   * on the server, as permissions are not checked.
   *
   */

  public Object getFieldValueLocal(short fieldID)
  {
    DBField f = (DBField) getField(fieldID);

    /* -- */
    
    if (f == null)
      {
	return null;
      }

    if (f.isVector())
      {
	if (gSession != null)
	  {
	    gSession.setLastError("couldn't get scalar value on vector field " + fieldID);
	  }

	return null;
      }

    return f.getValueLocal();
  }

  /**
   *
   * Shortcut method to set a field's value.  Using this
   * method saves a roundtrip to the server, which is
   * particularly useful in database loading.
   *
   * @see arlut.csd.ganymede.db_object
   */

  public Vector getFieldValues(short fieldID)
  {
    DBField f = (DBField) getField(fieldID);

    /* -- */
    
    if (f == null)
      {
	return null;
      }

    if (!f.isVector())
      {
	if (gSession != null)
	  {
	    gSession.setLastError("couldn't get vector value on scalar field " + fieldID);
	  }

	return null;
      }

    return f.getValues();
  }

  /**
   *
   * Shortcut method to set a field's value.  Using this
   * method saves a roundtrip to the server, which is
   * particularly useful in database loading.
   *
   * @see arlut.csd.ganymede.db_object
   */

  public Vector getFieldValuesLocal(short fieldID)
  {
    DBField f = (DBField) getField(fieldID);

    /* -- */
    
    if (f == null)
      {
	return null;
      }

    if (!f.isVector())
      {
	if (gSession != null)
	  {
	    gSession.setLastError("couldn't get vector value on scalar field " + fieldID);
	  }

	return null;
      }

    return f.getValuesLocal();
  }

  /**
   *
   * Generate a complete printed representation of the object,
   * suitable for printing to a debug or log stream.
   *
   */

  public synchronized void print(PrintStream out)
  {
    Enumeration enum;
    DBField field;

    /* -- */

    out.println("Invid: <" + objectBase.object_name + ":" + id + ">");
   
    enum = fields.elements();

    while (enum.hasMoreElements())
      {
	field = (DBField) enum.nextElement();

	out.print(field.getName());
	out.print(" : ");

	if (field.isVector())
	  {
	    for (int i = 0; i < field.size(); i++)
	      {
		out.print("\t" + field.key(i));

		if (i + 1 < field.size())
		  {
		    out.println(",");
		  }
		else
		  {
		    out.println();
		  }
	      }
	  }
	else
	  {
	    out.println(field.getID());
	  }
      }    
  }

  /**
   *
   * This method is used to provide a hook to allow different
   * objects to generate different labels for a given object
   * based on their perspective.  This is used to sort
   * of hackishly simulate a relational-type capability for
   * the purposes of viewing backlinks.<br><br>
   *
   * See the automounter map and NFS volume DBEditObject
   * subclasses for how this is to be used, if you have
   * them.
   *
   */
  
  public String lookupLabel(DBObject object)
  {
    return object.getLabel();	// default
  }

  /**
   *
   * This method is used to correct this object's base and basefield
   * pointers when the base changes.  This happens when the schema is
   * edited.. this method is called on all objects under a
   * DBObjectBase to make the object point and its fields point to the
   * new version of the DBObjectBase and DBObjectBaseFields.
   *
   */

  synchronized void updateBaseRefs(DBObjectBase newBase)
  {
    this.objectBase = newBase;

    Enumeration enum = fields.elements();

    while (enum.hasMoreElements())
      {
	DBField field = (DBField) enum.nextElement();
	field.definition = (DBObjectBaseField) newBase.getField(field.getID());
      }
  }

  /**
   *
   * This method is a convenience for server-side code.  If
   * this object is an embedded object, this method will
   * return the label of the containing object.  If this
   * object is not embedded, or the containing object's
   * label cannot be determined, null will be returned.
   *
   */
  
  public String getContainingLabel()
  {
    if (!isEmbedded())
      {
	return null;
      }

    InvidDBField field = (InvidDBField) getField(SchemaConstants.ContainerField);

    if (field == null)
      {
	return null;
      }

    return field.getValueString();
  }
}
