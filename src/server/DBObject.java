/*
   GASH 2

   DBObject.java

   The GANYMEDE object storage system.

   Created: 2 July 1996
   Version: $Revision: 1.34 $ %D%
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
 * @version $Revision: 1.34 $ %D% (Created 2 July 1996)
 * @author Jonathan Abbey, jonabbey@arlut.utexas.edu, ARL:UT
 *
 */

public class DBObject extends UnicastRemoteObject implements db_object, FieldType {

  static boolean debug = true;

  public static void setDebug(boolean val)
  {
    debug = val;
  }

  /* - */

  boolean customizer = false;
  protected DBObjectBase objectBase;
  protected int id;			// 32 bit id - the object's invariant id
  protected Hashtable fields;

  DBEditObject shadowObject;	// if this object is being edited or removed, this points
				// to the shadow that manages the changes
  
  protected DBEditSet editset;	// transaction that this object has been checked out in
				// care of.

  Invid myInvid = null;

  /* -- */

  /**
   *
   * No param constructor, here to allow DBEditObject to have
   * a no-param constructor for a static method handle
   *
   */
  public DBObject() throws RemoteException
  {
    customizer = true;
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

  DBObject(DBObjectBase objectBase) throws RemoteException
  {
    this.objectBase = objectBase;
    id = 0;
    fields = null;

    shadowObject = null;
    editset = null;

    myInvid = new Invid(objectBase.type_code, id);
  }

  /**
   *
   * Constructor to create an object of type objectBase
   * with the specified object number.
   *
   */

  DBObject(DBObjectBase objectBase, int id) throws RemoteException
  {
    this(objectBase);
    this.id = id;
    myInvid = new Invid(objectBase.type_code, id);
  }

  /**
   *
   * Read constructor.  Constructs an objectBase from a
   * DataInput stream.
   *
   */

  DBObject(DBObjectBase objectBase, DataInput in, boolean journalProcessing) throws IOException, RemoteException
  {
    this.objectBase = objectBase;
    shadowObject = null;
    editset = null;
    receive(in, journalProcessing);
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
  
  DBObject(DBEditObject eObj) throws RemoteException
  {
    Enumeration enum;
    DBField field;
    Object key;

    /* -- */

    objectBase = eObj.objectBase;
    id = eObj.id;
    myInvid = eObj.myInvid;

    shadowObject = null;
    editset = null;

    fields = new Hashtable();

    // put any defined fields into the object we're going
    // to commit back into our DBStore

    enum = eObj.fields.keys();

    while (enum.hasMoreElements())
      {
	key = enum.nextElement();
	field = (DBField) eObj.fields.get(key);

	if (field.isDefined())
	  {
	    field.setOwner(this); // this will make the field non-editable
	    fields.put(key, field);
	  }
	else
	  {
	    Ganymede.debug("DBObject check-in: rejecting undefined field " + field.getName());
	  }
      }
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
   * Returns the numeric id of the object in the objectBase
   *
   * @see arlut.csd.ganymede.db_object
   */

  public final short getTypeID()
  {
    return objectBase.type_code;
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
		return f.getValueString();
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
    Short key;

    /* -- */

    //    System.err.println("Emitting " + objectBase.getName() + " <" + id + ">");

    out.writeInt(id);

    if (fields.size() == 0)
      {
	System.err.println("**** Error: writing object with no fields: " + objectBase.getName() + " <" + id + ">");
      }

    out.writeShort(fields.size());

    //    System.err.println("emitting fields");
   
    enum = fields.keys();

    while (enum.hasMoreElements())
      {
	key = (Short) enum.nextElement();

	//	System.err.println("field:" + key);

	out.writeShort(key.shortValue());
	((DBField) fields.get(key)).emit(out);
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

    Short
      key;

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
	fields = new Hashtable(tmp_count);
      }
    else
      {
	fields = new Hashtable(); 
      }

    for (int i = 0; i < tmp_count; i++)
      {
	// read our field code, look it up in our
	// DBObjectBase

	fieldcode = in.readShort();
	key = new Short(fieldcode);

	definition = (DBObjectBaseField) objectBase.fieldHash.get(key);

	if (definition == null)
	  {
	    System.err.println("What the heck?  Null definition for " + objectBase.getName() + ", key = " + key);
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
	
	// now add the field to our fields hash
	fields.put(key, tmp);
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
	    error_code = "Invocation Target Exception";
	  }

	if (error_code != null)
	  {
	    editset.getSession().setLastError("createNewObject failure: " + error_code +
					      " in trying to check out custom object");
	    return null;
	  }
      }
    else
      {
	try
	  {
	    shadowObject = new DBEditObject(this, editset);
	  }
	catch (RemoteException ex)
	  {
	    editset.getSession().setLastError("remote exception creating shadow for " + 
					      getBase().getName() + ": " + getID());
	    return null;
	  }
      }

    editset.addObject(shadowObject);
    this.editset = editset;

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
    if (editset != this.editset)
      {
	// couldn't clear the shadow..  this editSet
	// wasn't the one to create the shadow

	return false;
      }

    this.editset = null;
    shadowObject = null;

    objectBase.store.checkIn(); // update checked out count

    return true;
  }

  /**
   *
   * Returns the transaction object owning this object, or
   * null if an unowned data object.
   *
   */

  DBEditSet getEditSet()
  {
    return editset;
  }

  /**
   *
   * <p>Get read-only list of DBFields contained in this object.</p>
   *
   * @see arlut.csd.ganymede.db_object
   */

  synchronized public Vector getFieldTemplateVector()
  {
    Vector results = new Vector();
    Enumeration enum;
    DBObjectBaseField fieldDef;

    /* -- */

    synchronized (objectBase)
      {
	enum = objectBase.sortedFields.elements();;
	
	while (enum.hasMoreElements())
	  {
	    fieldDef = (DBObjectBaseField) enum.nextElement();

	    results.addElement(fieldDef.template);
	  }
      }

    return results;
  }


  /**
   *
   * <p>Get read-only list of DBFields contained in this object.</p>
   *
   * @see arlut.csd.ganymede.db_object
   */

  synchronized public Vector getFieldInfoVector()
  {
    Vector results = new Vector();
    Enumeration enum;
    DBField field;

    /* -- */

    enum = fields.elements();
    
    while (enum.hasMoreElements())
      {
	field = (DBField) enum.nextElement();

	results.addElement(new FieldInfo(field));
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

  public db_field getField(short id)
  {
    db_field f;

    f = (DBField) fields.get(new Short(id));

    //    if (f == null)
    //      {
    //	Ganymede.debug("Couldn't find field " + id);
    //      }

    return f;
  }

  /**
   *
   * <p>Get read-only access to a field from this object, by name.</p>
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
   * @see arlut.csd.ganymede.db_object
   */

  synchronized public db_field[] listFields(boolean customOnly)
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
   * <p>Returns true if the last field change peformed on this
   * object necessitates the client rescanning this object to
   * reveal previously invisible fields or to hide previously
   * visible fields.</p>
   *
   * <p>Note that a non-editable DBObject never needs to be
   * rescanned, this method only has an impact on DBEditObject
   * and subclasses thereof.</p>
   *
   * <p>shouldRescan() should reset itself after returning
   * true</p>
   *
   * @see arlut.csd.ganymede.db_object
   */

  public boolean shouldRescan()
  {
    return false;
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
    return (getField(SchemaConstants.RemovalField) != null);
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
    return (getField(SchemaConstants.ExpirationField) != null);
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

  public boolean setFieldValue(short fieldID, Object value)
  {
    return false;		// we override in DBEditObject
  }

  /**
   *
   * Generate a complete printed representation of the object,
   * suitable for printing to a debug or log stream.
   *
   */

  synchronized public void print(PrintStream out)
  {
    Enumeration enum;
    Object key;
    DBField field;

    /* -- */

    out.println("Invid: <" + objectBase.object_name + ":" + id + ">");
   
    enum = fields.keys();

    while (enum.hasMoreElements())
      {
	key = enum.nextElement();
	out.print(((DBObjectBaseField) objectBase.fieldHash.get(key)).field_name);
	out.print(" : ");

	field = (DBField) fields.get(key);

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
	    out.println(field.key());
	  }
      }    
  }

  /**
   *
   * Generate a dump row for the query processor, which
   * includes a string representation of the Invid and
   * the label of this object.
   *
   */

  synchronized public String resultDump()
  {
    StringBuffer buffer = new StringBuffer();

    /* -- */

    // first thing you do is insert a representation for the Invid

    buffer.append(getInvid().toString());
    buffer.append("|");

    char[] chars = getLabel().toCharArray();
    
    for (int j = 0; j < chars.length; j++)
      {
	if (chars[j] == '|')
	  {
	    buffer.append("\\|");
	  }
	else if (chars[j] == '\n')
	  {
	    buffer.append("\\\n");
	  }
	else if (chars[j] == '\\')
	  {
	    buffer.append("\\\\");
	  }
	else
	  {
	    buffer.append(chars[j]);
	  }
      }

    buffer.append("\n");

    return buffer.toString();
  }

  /**
   *
   * This method is used to correct this object's base and basefield
   * pointers when the base changes.
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
}
