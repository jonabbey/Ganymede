/*
   GASH 2

   DBObjectBase.java

   The GANYMEDE object storage system.

   Created: 2 July 1996
   Release: $Name:  $
   Version: $Revision: 1.98 $
   Last Mod Date: $Date: 1999/11/19 20:37:01 $
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

import java.lang.reflect.*;
import java.io.*;
import java.util.*;
import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;

import arlut.csd.Util.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                    DBObjectBase

------------------------------------------------------------------------------*/

/**
 * <p>The data dictionary and object store for a particular kind of
 * object in the {@link arlut.csd.ganymede.DBStore DBStore} on the
 * Ganymede server.</p>
 *
 * <p>Each DBObjectBase object includes a set of
 * {@link arlut.csd.ganymede.DBObjectBaseField DBObjectBaseField} objects, which
 * define the types and constraints on fields that may be present in objects
 * of this type.  These field definitions are held in an
 * {@link arlut.csd.ganymede.DBBaseFieldTable DBBaseFieldTable}.</p>
 *
 * <p>The actual {@link arlut.csd.ganymede.DBObject DBObject}'s themselves are
 * contained in an optimized {@link arlut.csd.ganymede.DBObjectTable DBObjectTable}
 * contained within this DBObjectBase.</p>
 *
 * <p>In addition to holding name, type id, and category information for a
 * given object type, the DBObjectBase class may also contain a string classname
 * for a Java class to be dynamically loaded to manage the server's interactions
 * with objects of this type.  Such a class name must refer to a subclass of the
 * {@link arlut.csd.ganymede.DBEditObject DBEditObject} class.  If such a custom
 * class is defined for this object type, DBObjectBase will contain an
 * {@link arlut.csd.ganymede.DBObjectBase#objectHook objectHook} DBEditObject
 * instance whose methods will be consulted to customize a lot of the server's
 * functioning.</p>
 *
 * <p>DBObjectBase also keeps track of {@link arlut.csd.ganymede.DBReadLock DBReadLocks},
 * {@link arlut.csd.ganymede.DBWriteLock DBWriteLocks}, and 
 * {@link arlut.csd.ganymede.DBDumpLock DBDumpLocks}, to manage 
 * changes to be made to objects contained in this DBObjectBase.</p>
 *
 * <p>DBObjectBase implements the {@link arlut.csd.ganymede.Base Base} RMI remote 
 * interface, which is used by the client to determine type information for objects
 * of this type, as well as by the schema editor when the schema is being edited.</p>
 */

public class DBObjectBase extends UnicastRemoteObject implements Base, CategoryNode {

  static boolean debug = true;
  final static boolean debug2 = false;

  public static void setDebug(boolean val)
  {
    System.err.println("DBObjectBase.setDebug(): " + val);
    debug = val;
  }

  /* - */

  DBStore store;

  /**
   * <P>Name of this object type</P>
   */

  String object_name;

  /**
   * <P>short type id code for this object type.  This number is
   * used as the {@link arlut.csd.ganymede.Invid#type type} code
   * in {@link arlut.csd.ganymede.Invid Invid}s pointing to objects
   * of this type.</P>
   */

  short type_code;

  /**
   * <P>Fully qualified package and class name for a custom 
   * {@link arlut.csd.ganymede.DBEditObject DBEditObject} subclass
   * to be dynamically loaded to manage operations on this DBObjectBase.</P>
   */

  String classname;

  /**
   * <P>Class definition for a
   * {@link arlut.csd.ganymede.DBEditObject DBEditObject} subclass
   * dynamically loaded to manage operations on this DBObjectBase.</P>
   */

  Class classdef;

  /**
   * which field represents our label?
   */

  short label_id;

  /**
   * what type of object is this?
   */

  Category category;

  /**
   * <P>What order will this object type appear in the client's
   * object tree?  This ordering number applies within the
   * {@link arlut.csd.ganymede.Category Category} containing
   * this DBObjectBase.</P>
   */

  int displayOrder = 0;

  private boolean embedded;

  // runtime data

  /**
   * field dictionary, sorted in displayOrder
   */

  Vector sortedFields;

  /**
   * The hash listing us by object type
   * id.. we can iterate over the elements of
   * this hash to determine whether a proposed name
   * is acceptable.
   */

  Hashtable containingHash;	
  
  /**
   * field dictionary
   */

  DBBaseFieldTable fieldTable;

  /**
   * objects in our objectBase
   */

  DBObjectTable objectTable;

  /**
   * highest invid to date
   */

  int maxid;

  /**
   * Timestamp for the last time this DBObjectBase was
   * changed, used by GanymedeBuilderTasks to determine
   * whether a particular build sequence is necessary.
   */

  Date lastChange;

  boolean reallyLoading;

  /**
   * <P>If this DBObjectBase is locked with an exclusive lock
   * (either a {@link arlut.csd.ganymede.DBWriteLock DBWriteLock} or
   * a {@link arlut.csd.ganymede.DBDumpLock DBDumpLock}), this field
   * will point to it.</P>
   *
   * <P>This field is not currently used for anything in particular
   * in the lock logic, it is here strictly for informational/debugging
   * purposes.</P>
   */

  DBLock currentLock;

  /**
   * <P>"Queue" of {@link arlut.csd.ganymede.DBWriteLock DBWriteLock}s pending
   * on this DBObjectBase.  DBWriteLocks will add themselves to the writerList
   * upon entering establish().  If writerList is not empty, no new
   * {@link arlut.csd.ganymede.DBReadLock DBReadLock}s will be allowed to
   * add themselves to the
   * {@link arlut.csd.ganymede.DBObjectBase#readerList readerList} in this
   * DBObjectBase.</P>
   *
   * <P>Note that there is no guarantee that DBWriteLocks will be granted
   * access to any given DBObjectBase in the order that their threads
   * entered the establish() method, as different DBWriteLocks may be
   * attempting to establish() on differing sets of DBObjectBases.  There
   * is not in fact any attempt in the DBWriteLock establish() method to
   * ensure that writers are given the lock on a DBObjectBase in their
   * writerList ordering.  The establish() methods may establish() any
   * writer in any order, depending on the server's threading behavior.</P>
   */

  private Vector writerList;

  /**
   * <P>Collection of {@link arlut.csd.ganymede.DBReadLock DBReadLock}s
   * that are locked on this DBObjectBase.</P>
   */

  private Vector readerList;

  /**
   * <P>"Queue" of {@link arlut.csd.ganymede.DBDumpLock DBDumpLock}s pending
   * on this DBObjectBase.  DBDumpLocks will add themselves to the dumperList
   * upon entering establish().  If dumperList is not empty, no new
   * {@link arlut.csd.ganymede.DBWriteLock DBWriteLock}s will be allowed to
   * add themselves to the
   * {@link arlut.csd.ganymede.DBObjectBase#writerList writerList} in this
   * DBObjectBase.</P>
   *
   * <P>Note that there is no guarantee that DBDumpLocks will be granted
   * access to any given DBObjectBase in the order that their threads
   * entered the establish() method, as different DBDumpLocks may be
   * attempting to establish() on differing sets of DBObjectBases.  There
   * is not in fact any attempt in the DBDumpLock establish() method to
   * ensure that writers are given the lock on a DBObjectBase in their
   * dumperList ordering.  The establish() methods may establish() any
   * dumper in any order, depending on the server's threading behavior.</P>
   */

  private Vector dumperList;

  /**
   * <P>Boolean flag monitoring whether or not this DBObjectBase is
   * currently locked for writing.</P>
   */

  boolean writeInProgress;

  /**
   * <P>Boolean flag monitoring whether or not this DBObjectBase is
   * currently locked for dumping.</P>
   */

  boolean dumpInProgress;

  /**
   * Used to keep track of schema editing
   */

  DBSchemaEdit editor;

  /**
   * Used to keep track of schema editing
   */

  DBObjectBase original;

  /**
   * Used to keep track of schema editing
   */

  boolean save;

  /**
   * Used to keep track of schema editing
   */

  boolean changed;

  // Customization Management Object

  /**
   * <P>Each DBObjectBase can have an instantiation of a custom
   * {@link arlut.csd.ganymede.DBEditObject DBEditObject} subclass
   * to respond to a number of 'pseudostatic' method calls which customize
   * the Ganymede server's behavior when dealing with objects of this DBObjectBase's
   * type.  The DBObjectBase 
   * {@link arlut.csd.ganymede.DBObjectBase#createHook() createHook()} method
   * is responsible for loading the custom DBEditObject subclass
   * ({@link arlut.csd.ganymede.DBObjectBase#classdef classdef}) from
   * the {@link arlut.csd.ganymede.DBObjectBase#classname classname}
   * specified in the ganymede.db schema section.</P>
   *
   * <P>objectHook should never be null while the server is in operation. If the
   * Ganymede schema definition data in the ganymede.db file does not specify
   * a special class for this object type's objectHook, DBObjectBase should have
   * an instance of the base DBEditObject class here.</P>
   *
   * <P>See the Ganymede DBEditObject subclassing/customization guide for a lot
   * more details on the use of DBEditObjects as objectHooks.</P>
   */

  DBEditObject objectHook;	

  /* -- */


  public DBObjectBase(DBStore store, boolean embedded) throws RemoteException
  {
    this(store, embedded, true);
  }

  public DBObjectBase(DBStore store, boolean embedded, boolean createFields) throws RemoteException
  {
    super();			// initialize UnicastRemoteObject

    DBObjectBaseField bf;

    /* -- */

    debug = Ganymede.debug;

    this.store = store;
    this.containingHash = store.objectBases;

    writerList = new Vector();
    readerList = new Vector();
    dumperList = new Vector();

    object_name = "";
    classname = "";
    classdef = null;
    type_code = 0;
    label_id = -1;
    category = null;
    sortedFields = new Vector();
    fieldTable = new DBBaseFieldTable(20, (float) 1.0);
    objectTable = new DBObjectTable(4000, (float) 1.0);
    maxid = 0;
    lastChange = new Date();

    editor = null;
    original = null;
    save = true;		// by default, we'll want to keep this
    changed = false;

    this.embedded = embedded;

    if (createFields)
      {
	if (embedded)
	  {
	    /* Set up our 0 field, the containing object owning us */

	    bf = new DBObjectBaseField(this);

	    // notice that we don't mark this field as builtin, because
	    // that only applies to fields present in all non-embedded
	    // objects.

	    bf.field_name = "Containing Object";
	    bf.field_code = SchemaConstants.ContainerField;
	    bf.field_type = FieldType.INVID;
	    bf.field_order = bf.field_code;
	    bf.allowedTarget = -1;	// we can point at anything, but there'll be a special
	    bf.targetField = -1;	// procedure for handling deletion and what not..
	    bf.editable = false;
	    bf.removable = false;
	    bf.array = false;
	    bf.visibility = false;	// we don't want the client to show the owner link

	    addField(bf);

	    // note that we won't have an expiration date or removal date
	    // for an embedded object
	  }
	else
	  {
	    /* Set up our 0 field, the owner list. */

	    bf = new DBObjectBaseField(this);

	    bf.field_name = "Owner list";
	    bf.field_code = SchemaConstants.OwnerListField;
	    bf.field_type = FieldType.INVID;
	    bf.field_order = 0;
	    bf.allowedTarget = SchemaConstants.OwnerBase;
	    bf.targetField = SchemaConstants.OwnerObjectsOwned;
	    bf.editable = false;
	    bf.removable = false;
	    bf.builtIn = true;	// this isn't optional
	    bf.array = true;

	    addField(bf);

	    /* And our 1 field, the expiration date. */

	    bf = new DBObjectBaseField(this);
    
	    bf.field_name = "Expiration Date";
	    bf.field_code = SchemaConstants.ExpirationField;
	    bf.field_type = FieldType.DATE;
	    bf.field_order = 1;
	    bf.editable = false;
	    bf.removable = false;
	    bf.builtIn = true;	// this isn't optional

	    addField(bf);

	    /* And our 2 field, the expiration date. */

	    bf = new DBObjectBaseField(this);
    
	    bf.field_name = "Removal Date";
	    bf.field_code = SchemaConstants.RemovalField;
	    bf.field_type = FieldType.DATE;
	    bf.field_order = 2;
	    bf.editable = false;
	    bf.removable = false;
	    bf.builtIn = true;	// this isn't optional

	    addField(bf);

	    /* And our 3 field, the notes field */

	    bf = new DBObjectBaseField(this);
    
	    bf.field_name = "Notes";
	    bf.field_code = SchemaConstants.NotesField;
	    bf.field_type = FieldType.STRING;
	    bf.field_order = 3;
	    bf.editable = false;
	    bf.removable = false;
	    bf.builtIn = true;	// this isn't optional

	    addField(bf);

	    // And our 4 field, the creation date field */

	    bf = new DBObjectBaseField(this);
    
	    bf.field_name = "Creation Date";
	    bf.field_code = SchemaConstants.CreationDateField;
	    bf.field_type = FieldType.DATE;
	    bf.field_order = 4;
	    bf.editable = false;
	    bf.removable = false;
	    bf.builtIn = true;	// this isn't optional

	    addField(bf);

	    /* And our 5 field, the Creator field */

	    bf = new DBObjectBaseField(this);
    
	    bf.field_name = "Creator Info";
	    bf.field_code = SchemaConstants.CreatorField;
	    bf.field_type = FieldType.STRING;
	    bf.field_order = 5;
	    bf.editable = false;
	    bf.removable = false;
	    bf.builtIn = true;	// this isn't optional

	    addField(bf);

	    // And our 6 field, the modification date field */

	    bf = new DBObjectBaseField(this);
    
	    bf.field_name = "Modification Date";
	    bf.field_code = SchemaConstants.ModificationDateField;
	    bf.field_type = FieldType.DATE;
	    bf.field_order = 6;
	    bf.editable = false;
	    bf.removable = false;
	    bf.builtIn = true;	// this isn't optional

	    addField(bf);

	    /* And our 7 field, the Modifier field */

	    bf = new DBObjectBaseField(this);
    
	    bf.field_name = "Modifier Info";
	    bf.field_code = SchemaConstants.ModifierField;
	    bf.field_type = FieldType.STRING;
	    bf.field_order = 7;
	    bf.editable = false;
	    bf.removable = false;	
	    bf.builtIn = true;	// this isn't optional

	    addField(bf);
	  }
      }

    objectHook = this.createHook();
  }

  /**
   * <p>Creation constructor.  Used when the schema editor interface is
   * used to create a new DBObjectBase.</p>
   */

  public DBObjectBase(DBStore store, short id, boolean embedded,
		      DBSchemaEdit editor) throws RemoteException
  {
    this(store, embedded);
    type_code = id;
    this.editor = editor;
  }

  /**
   * <p>receive constructor.  Used to initialize this DBObjectBase from disk
   * and load the objects of this type in from the standing store.</p>
   *
   * @param in Input stream to read this object base from.
   * @param store The Ganymede database object we are loading into.
   * @param reallyLoad If false, we won't remember objects loaded.  This
   * is used when we want to just examine a db file's schema information.
   */

  public DBObjectBase(DataInput in, DBStore store, boolean reallyLoad) throws IOException, RemoteException
  {
    /* assume not embedded, we'll correct that in receive()
       if we have to

       we don't want to add in the built-in fields.. we assume that they
       are properly registered on disk. */

    this(store, false, false);

    this.reallyLoading = reallyLoad;

    receive(in);

    // need to recreate objectHook now that we have loaded our classdef info
    // from disk.

    if (reallyLoad)
      {
	objectHook = this.createHook();
      }
  }

  /**
   * <p>receive constructor.  Used to initialize this DBObjectBase from disk
   * and load the objects of this type in from the standing store.</p>
   */

  public DBObjectBase(DataInput in, DBStore store) throws IOException, RemoteException
  {
    this (in, store, true);
  }

  /**
   * <p>copy constructor.  Used to create a copy that we can play with for
   * schema editing.</p>
   */

  public DBObjectBase(DBObjectBase original, DBSchemaEdit editor) throws RemoteException
  {
    this(original.store, original.embedded, false);
    this.editor = editor;

    DBObjectBaseField bf;

    synchronized (original)
      {
	object_name = original.object_name;
	classname = original.classname;
	classdef = original.classdef;
	type_code = original.type_code;
	label_id = original.label_id;
	category = original.category;
	displayOrder = original.displayOrder;
	embedded = original.embedded;
    
	// make copies of all the old field definitions
	// for this object type, and save them into our
	// own field hash.
    
	Enumeration enum;
	DBObjectBaseField field;
    
	enum = original.fieldTable.elements();

	while (enum.hasMoreElements())
	  {
	    field = (DBObjectBaseField) enum.nextElement();
	    bf = new DBObjectBaseField(field, editor); // copy this base field
	    bf.base = this;

	    addField(bf);
	  }

	sortFields();

	// remember the objects.. note that we don't at this point notify
	// the objects that this new DBObjectBase is their owner.. we'll
	// take care of that when and if the DBSchemaEdit base editing session
	// commits this copy

	objectTable = original.objectTable;

	maxid = original.maxid;
    
	changed = false;
	this.original = original; // remember that we are a copy

	// in case our classdef was set during the copy.

	objectHook = this.createHook();

	lastChange = new Date();
      }
  }

  void setContainingHash(Hashtable ht)
  {
    this.containingHash = ht;
  }

  /**
   * <p>This method writes out a schema-only definition of this base
   * to disk, for use by the DBStore dumpSchema() method.</p>
   *
   * <p>Note that some objects are emitted by this method, specifically
   * things like the supergash owner group, and the like.</p>
   */

  synchronized void partialEmit(DataOutput out) throws IOException
  {
    int size;
    Enumeration enum;
    Enumeration baseEnum;

    /* -- */

    out.writeUTF(object_name);
    out.writeUTF(classname);
    out.writeShort(type_code);

    // calculate what fields we want to emit.  We're going to
    // skip any backlinks field we see, since we stopped using
    // those fields as of file version 1.17.

    Vector fieldsToEmit = new Vector();

    enum = fieldTable.elements();

    while (enum.hasMoreElements())
      {
	DBObjectBaseField fieldDef = (DBObjectBaseField) enum.nextElement();

	if (fieldDef.getID() != SchemaConstants.BackLinksField)
	  {
	    fieldsToEmit.addElement(fieldDef);
	  }
      }
    
    // okay, we have the field definitions we're actually going to
    // write.  put out the field count

    size = fieldsToEmit.size();

    out.writeShort((short) size); // should have no more than 32k fields

    // and write out the field definitions

    for (int i = 0; i < fieldsToEmit.size(); i++)
      {
	DBObjectBaseField fieldDef = (DBObjectBaseField) fieldsToEmit.elementAt(i);

	fieldDef.emit(out);
      }

    if ((store.major_version >= 1) && (store.minor_version >= 1))
      {
	out.writeShort(label_id);	// added at file version 1.1
      }

    if ((store.major_version >= 1) && (store.minor_version >= 3))
      {
	if (category.getPath() == null)
	  {
	    out.writeUTF(store.rootCategory.getPath());
	  }
	else
	  {
	    out.writeUTF(category.getPath()); // added at file version 1.3
	  }
      }

    if ((store.major_version >= 1) && (store.minor_version >= 4))
      {
	out.writeInt(displayOrder);	// added at file version 1.4
      }

    if ((store.major_version >= 1) && (store.minor_version >= 5))
      {
	out.writeBoolean(embedded);	// added at file version 1.5
      }

    // since we're doing a partial emit for a schema dump, we won't
    // record an explicit maxid.. our receive method will still keep
    // the maxid for this base at a safe high-water mark.. we don't
    // want to uniquely preserve Invid's across a schema dump

    if ((store.major_version >= 1) && (store.minor_version >= 12))
      {
	out.writeInt(0);	// added at file version 1.12
      }

    // now, we're doing a partial emit.. if we're SchemaConstants.PersonaBase,
    // we only want to emit the 'constant' personae.. those that aren't associated
    // with regular user accounts.

    // if we're SchemaConstants.OwnerBase, we only want to emit the 'supergash'
    // owner group.

    if (type_code == SchemaConstants.PersonaBase)
      {
	// first, figure out how many we're going to save to emit

	int counter = 0;
	DBObject personaObj;

	baseEnum = objectTable.elements();

	while (baseEnum.hasMoreElements())
	  {
	    personaObj = (DBObject) baseEnum.nextElement();

	    Invid invid = personaObj.getInvid();

	    // Persona 1 is supergash/root, Persona 2 is monitor

	    if (invid.getNum() <= 2)
	      {
		counter++;
	      }
	  }

	//	System.err.println("Writing out " + counter + " objects");

	out.writeInt(counter);

	baseEnum = objectTable.elements();

	while (baseEnum.hasMoreElements())
	  {
	    personaObj = (DBObject) baseEnum.nextElement();

	    Invid invid = personaObj.getInvid();

	    // Persona 1 is supergash/root, Persona 2 is monitor

	    if (invid.getNum() <= 2)
	      {
		personaObj.partialEmit(out);
	      }
	  }
      }
    else if (type_code == SchemaConstants.OwnerBase)
      {
	// first, figure out how many we're going to save to emit

	int counter = 0;
	DBObject ownerObj;

	baseEnum = objectTable.elements();

	while (baseEnum.hasMoreElements())
	  {
	    ownerObj = (DBObject) baseEnum.nextElement();

	    if (ownerObj.getLabel().equals(Ganymede.rootname))
	      {
		counter++;
	      }
	  }

	//	System.err.println("Writing out " + counter + " objects");

	out.writeInt(counter);

	baseEnum = objectTable.elements();

	while (baseEnum.hasMoreElements())
	  {
	    ownerObj = (DBObject) baseEnum.nextElement();

	    if (ownerObj.getLabel().equals(Ganymede.rootname))
	      {
		ownerObj.partialEmit(out);
	      }
	  }
      }
    else  // just write everything in this base out, using standard emit()
      {
	out.writeInt(objectTable.size());
	
	baseEnum = objectTable.elements();
	
	while (baseEnum.hasMoreElements())
	  {
	    ((DBObject) baseEnum.nextElement()).emit(out);
	  }
      }
  }

  synchronized void emit(DataOutput out, boolean dumpObjects) throws IOException
  {
    int size;
    Enumeration enum;
    Enumeration baseEnum;

    /* -- */

    out.writeUTF(object_name);
    out.writeUTF(classname);
    out.writeShort(type_code);

    // calculate what fields we want to emit.  We're going to
    // skip any backlinks field we see, since we stopped using
    // those fields as of file version 1.17.

    Vector fieldsToEmit = new Vector();

    enum = fieldTable.elements();

    while (enum.hasMoreElements())
      {
	DBObjectBaseField fieldDef = (DBObjectBaseField) enum.nextElement();

	if (fieldDef.getID() != SchemaConstants.BackLinksField)
	  {
	    fieldsToEmit.addElement(fieldDef);
	  }
      }
    
    // okay, we have the field definitions we're actually going to
    // write.  put out the field count

    size = fieldsToEmit.size();

    out.writeShort((short) size); // should have no more than 32k fields

    // and write out the field definitions

    for (int i = 0; i < fieldsToEmit.size(); i++)
      {
	DBObjectBaseField fieldDef = (DBObjectBaseField) fieldsToEmit.elementAt(i);

	fieldDef.emit(out);
      }
    
    if ((store.major_version >= 1) && (store.minor_version >= 1))
      {
	out.writeShort(label_id);	// added at file version 1.1
      }

    if ((store.major_version >= 1) && (store.minor_version >= 3))
      {
	if (category.getPath() == null)
	  {
	    out.writeUTF(store.rootCategory.getPath());
	  }
	else
	  {
	    out.writeUTF(category.getPath()); // added at file version 1.3
	  }
      }

    if ((store.major_version >= 1) && (store.minor_version >= 4))
      {
	out.writeInt(displayOrder);	// added at file version 1.4
      }

    if ((store.major_version >= 1) && (store.minor_version >= 5))
      {
	out.writeBoolean(embedded);	// added at file version 1.5
      }

    if (dumpObjects)
      {
	if ((store.major_version >= 1) && (store.minor_version >= 12))
	  {
	    out.writeInt(maxid);	// added at file version 1.12
	  }

	out.writeInt(objectTable.size());
   
	baseEnum = objectTable.elements();

	while (baseEnum.hasMoreElements())
	  {
	    ((DBObject) baseEnum.nextElement()).emit(out);
	  }
      }
    else
      {
	if ((store.major_version >= 1) && (store.minor_version >= 12))
	  {
	    out.writeInt(0);	// added at file version 1.12
	  }

	out.writeInt(0);
      }
  }

  synchronized void receive(DataInput in) throws IOException
  {
    int size;
    DBObject tempObject;
    int temp_val;
    int object_count;
    DBObjectBaseField field;

    /* -- */

    if (debug)
      {
	System.err.println("DBObjectBase.receive(): enter");
      }

    object_name = in.readUTF();

    if (debug)
      {
	System.err.println("DBObjectBase.receive(): object base name: " + object_name);
      }

    classname = in.readUTF();

    if (debug)
      {
	System.err.println("DBObjectBase.receive(): class name: " + classname);
      }

    type_code = in.readShort();	// read our index for the DBStore's objectbase hash

    size = in.readShort();

    if (debug)
      {
	System.err.println("DBObjectBase.receive(): " + size + " fields in dictionary");
      }

    if (size > 0)
      {
	fieldTable = new DBBaseFieldTable(size, (float) 1.0);
      }
    else
      {
	fieldTable = new DBBaseFieldTable();
      }

    // read in the field dictionary for this object

    for (int i = 0; i < size; i++)
      {
	field = new DBObjectBaseField(in, this);

	if (debug2)
	  {
	    System.err.println("DBObjectBaseField.receive(): " + field);
	  }

	// at file version 1.17, we stopped using the backlinks
	// field.. don't register it in this object type.

	if (field.getID() != SchemaConstants.BackLinksField)
	  {
	    addField(field);
	  }
      }

    sortFields();

    // at file version 1.1, we introduced label_id's.

    if ((store.file_major >= 1) && (store.file_minor >= 1))
      {
	label_id = in.readShort();
      }
    else
      {
	label_id = -1;
      }

    if (debug)
      {
	System.err.println("DBObjectBase.receive(): " + label_id + " is object label");
      }

    // at file version 1.3, we introduced object base categories's.

    if ((store.file_major >= 1) && (store.file_minor >= 3))
      {
	String pathName = in.readUTF();

	if (debug)
	  {
	    System.err.println("DBObjectBase.receive(): category is " + pathName);
	  }

	// and get our parent
	
	category = store.getCategory(pathName);

	if (debug)
	  {
	    if (category == null)
	      {
		System.err.println("DBObjectBase.receive(): category is null");
	      }
	  }
      }
    else
      {
	category = null;
      }

    if ((store.file_major >= 1) && (store.file_minor >= 4))
      {
	displayOrder = in.readInt();	// added at file version 1.4

	if (category != null)
	  {
	    category.addNode(this, true, false);
	  }
      }
    else
      {
	displayOrder = 0;
      }

    if ((store.file_major >= 1) && (store.file_minor >= 5))
      {
	embedded = in.readBoolean(); // added at file version 1.5
      }

    if ((store.file_major >= 1) && (store.file_minor >= 12))
      {
	maxid = in.readInt(); // added at file version 1.12
      }

    // read in the objects belonging to this ObjectBase

    object_count = in.readInt();

    if (debug)
      {
    	System.err.println("DBObjectBase.receive(): reading " + object_count + " objects");
      }

    temp_val = (object_count > 0) ? object_count : 4000;

    objectTable = new DBObjectTable(temp_val, (float) 1.0);

    for (int i = 0; i < object_count; i++)
      {
	//	if (debug)
	//	  {
	//	    System.err.println("DBObjectBase.receive(): reading object " + i);
	//	  }

	tempObject = new DBObject(this, in, false);

	if (tempObject.getID() > maxid)
	  {
	    maxid = tempObject.getID();
	  }

	if (reallyLoading)
	  {
	    objectTable.putNoSyncNoRemove(tempObject);
	    tempObject.setBackPointers();
	  }
      }

    if (debug)
      {
	System.err.println("DBObjectBase.receive(): maxid for " + object_name + " is " + maxid);
      }
  }

  /**
   * <p>This method returns true if this object base is for
   * an embedded object.  Embedded objects do not have
   * their own expiration and removal dates, do not have
   * history trails, and can be only owned by a single
   * object, not by a list of administrators.</p>
   */

  public boolean isEmbedded()
  {
    return embedded;
  }

  /**
   * <p>This method indicates whether this base may be removed in
   * the Schema Editor.</p>
   *
   * <p>We don't allow removal of built-in Bases that the server
   * depends on for its operation, such as permissions, notification,
   * and logging object types.</p>
   *
   * @see arlut.csd.ganymede.Base
   */

  public boolean isRemovable()
  {
    return (getTypeID() > SchemaConstants.FinalBase);
  }

  /**
   * <p>This method is used to force a reload of the custom object code
   * for this object type.</p>
   */

  public synchronized void reloadCustomClass()
  {
    this.classdef = null;

    try
      {
	this.objectHook = this.createHook();
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("Unexpected remote exception.. RMI init prob? " + ex);
      }
  }

  /** 
   * <p>This method is used to create a DBEditObject subclass handle
   * ({@link arlut.csd.ganymede.DBObjectBase#objectHook objectHook}),
   * to allow various classes to make calls to overridden static
   * methods for DBEditObject subclasses.</p> 
   */

  DBEditObject createHook() throws RemoteException
  {
    if (classdef == null)
      {
	if (classname != null && !classname.equals(""))
	  {
	    try
	      {
		classdef = Class.forName(classname);
	      }
	    catch (ClassNotFoundException ex)
	      {
		System.err.println("DBObjectBase.receive(): class definition could not be found: " + ex);
		classdef = null;
	      }
	  }

	// if we don't have a custom object hook, use the default
	
	if (classdef == null)
	  {
	    return new DBEditObject(this);
	  }
      }

    Constructor c;
    DBEditObject e_object = null;
    Class[] cParams = new Class[1];

    cParams[0] = this.getClass();

    Object[] params = new Object[1];
    params[0] = this;

    try
      {
	c = classdef.getDeclaredConstructor(cParams); // no param constructor
	e_object = (DBEditObject) c.newInstance(params);
      }
    catch (NoSuchMethodException ex)
      {
	System.err.println("NoSuchMethodException " + ex);
      }
    catch (SecurityException ex)
      {
	System.err.println("SecurityException " + ex);
      }
    catch (IllegalAccessException ex)
      {
	System.err.println("IllegalAccessException " + ex);
      }
    catch (IllegalArgumentException ex)
      {
	System.err.println("IllegalArgumentException " + ex);
      }
    catch (InstantiationException ex)
      {
	System.err.println("InstantiationException " + ex);
      }
    catch (InvocationTargetException ex)
      {
	System.err.println("InvocationTargetException " + ex);
      }

    if (debug2)
      {
	System.err.println("Created objectHook: object of type " + e_object.getClass());
      }

    return e_object;
  }

  /**
   * <p>Factory method to create a new DBEditObject of this
   * type.  The created DBEditObject will be connected
   * to the editset, and will not be integrated into the
   * DBStore until the editset is committed.</p>
   *
   * <p><b>IMPORTANT NOTE</b>: This method *must not* be public!  All
   * DBEditObject customization classes should go through 
   * DBSession.createDBObject() to create new objects.</p>
   *
   * @param editset The transaction this object is to be created in
   */

  DBEditObject createNewObject(DBEditSet editset)
  {
    return createNewObject(editset, null);
  }

  /**
   * <p>Factory method to create a new DBEditObject of this
   * type.  The created DBEditObject will be connected
   * to the editset, and will not be integrated into the
   * DBStore until the editset is committed.</p>
   *
   * <p><b>IMPORTANT NOTE</b>: This method *must not* be public!  All
   * DBEditObject customization classes should go through 
   * DBSession.createDBObject() to create new objects.</p>
   *
   * @param editset The transaction this object is to be created in
   * @param chosenSlot If this is non-null, the object will be assigned 
   * the given invid, if available
   *
   */

  synchronized DBEditObject createNewObject(DBEditSet editset, Invid chosenSlot)
  {
    DBEditObject 
      e_object = null;

    Invid invid;

    /* -- */

    if (editset == null)
      {
	throw new NullPointerException("null editset in createNewObject");
      }

    if (chosenSlot == null)
      {
	invid = new Invid(getTypeID(), getNextID());
      }
    else
      {
	if (chosenSlot.getType() != type_code)
	  {
	    throw new IllegalArgumentException("bad chosen_slot passed into createNewObject: bad type");
	  }

	if (objectTable.containsKey(chosenSlot.getNum()))
	  {
	    throw new IllegalArgumentException("bad chosen_slot passed into createNewObject: num already taken");
	  }

	invid = chosenSlot;
      }

    if (classdef == null)
      {
	e_object = new DBEditObject(this, invid, editset);
      }
    else
      {
	Constructor c;
	Class classArray[];
	Object parameterArray[];

	classArray = new Class[3];

	classArray[0] = this.getClass();
	classArray[1] = invid.getClass();
	classArray[2] = editset.getClass();

	parameterArray = new Object[3];

	parameterArray[0] = this;
	parameterArray[1] = invid;
	parameterArray[2] = editset;

	String error_code = null;

	try
	  {
	    c = classdef.getDeclaredConstructor(classArray);
	    e_object = (DBEditObject) c.newInstance(parameterArray);
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
	    error_code = "Invocation Target Exception: " + 
	      ex.getTargetException() + "\n" + 
	      ex.getMessage() + "\n\n" +
	      Ganymede.stackTrace(ex) + "\n";
	  }

	if (error_code != null)
	  {
	    Ganymede.debug("createNewObject failure: " + 
			   error_code + " in trying to construct custom object");
	  }
      }

    return e_object;
  }

  /**
   *
   * allocate a new object id 
   *
   */

  synchronized int getNextID()
  {
    //    if (debug)
    //      {
    //	System.err.println("DBObjectBase.getNextID(): " + object_name + "'s maxid is " + maxid);
    //      }

    return ++maxid;
  }

  /**
   * <p>releases an id if an object initially
   * created by createDBObject is rejected
   * due to its transaction being aborted</p>
   *
   * <p>note that we aren't being real fancy
   * here.. if this doesn't work, it doesn't
   * work.. we have 2 billion slots in this
   * object base after all..</p>
   */

  synchronized void releaseId(int id)
  {
    if (id==maxid)
      {
	maxid--;
      }
  }

  /**
   * <p>Print a debugging summary of the type information encoded
   * in this objectbase to a PrintWriter.</p>
   *
   * @param out PrintWriter to print to.
   */

  public synchronized void printHTML(PrintWriter out)
  {
    Enumeration enum;
    DBObjectBaseField bf;

    /* -- */

    out.println("<H3>");
    out.print(object_name + " (" + type_code + ") <font color=\"#0000ff\">label:</font> " + getLabelFieldName());

    if (classname != null && !classname.equals(""))
      {
	out.print(" <font color=\"#0000ff\">managing class:</font> " + classname);
      }

    out.println("</H3><p>");

    // we don't want to include the built-ins normally, as they should
    // be the same in all non-embedded bases.. but we may want to
    // check it out for debugging reasons sometime.

    if (false)
      {
	out.println("<h4>Built-in fields</h4>");
	out.println("<table border>");
	out.println("<tr>");
	out.println("<th>Field Name</th> <th>Field ID</th> <th>Field Type</th>");
	out.println("<th>Array?</th> <th>NameSpace</th> <th>Notes</th>");
	out.println("</tr>");
	
	enum = sortedFields.elements();
	
	while (enum.hasMoreElements())
	  {
	    bf = (DBObjectBaseField) enum.nextElement();

	    if (bf.isBuiltIn())
	      {
		out.println("<tr>");
		bf.printHTML(out);
		out.println("</tr>");
	      }
	  }

	out.println("</table>");
	out.println("<br>");

	out.println("<h4>Custom fields</h4>");
      }

    out.println("<table border>");
    out.println("<tr>");
    out.println("<th>Field Name</th> <th>Field ID</th> <th>Field Type</th>");
    out.println("<th>Array?</th> <th>NameSpace</th> <th>Notes</th>");
    out.println("</tr>");

    enum = sortedFields.elements();

    while (enum.hasMoreElements())
      {
	bf = (DBObjectBaseField) enum.nextElement();

	if (!bf.isBuiltIn())
	  {
	    out.println("<tr>");
	    bf.printHTML(out);
	    out.println("</tr>");
	  }
      }

    out.println("</table>");
    out.println("<br>");
  }

  /**
   * <p>Print a debugging summary of the type information encoded
   * in this objectbase to a PrintWriter.</p>
   *
   * @param out PrintWriter to print to.
   */

  public synchronized void print(PrintWriter out, String indent, boolean showAll)
  {
    Enumeration enum;
    DBObjectBaseField fieldDef;

    /* -- */

    out.println(indent + object_name + "(" + type_code + ")");
    
    enum = sortedFields.elements();

    while (enum.hasMoreElements())
      {
	fieldDef = (DBObjectBaseField) enum.nextElement();

	if (showAll || !fieldDef.isBuiltIn())
	  {
	    fieldDef.print(out, indent + "\t");
	  }
      }
  }

  /**
   * <p>Returns the DBStore containing this DBObjectBase.</p>
   */

  public DBStore getStore()
  {
    return store;
  }

  /**
   * <p>Returns the name of this object type</p>
   *
   * @see arlut.csd.ganymede.Base
   */

  public String getName()
  {
    return object_name;
  }


  /**
   * <p>Sets the name for this object type</p>
   *
   * <p>This method is only valid when the Base reference is obtained
   * from a {@link arlut.csd.ganymede.SchemaEdit SchemaEdit} reference
   * by the Ganymede schema editor.</p>
   *
   * @see arlut.csd.ganymede.Base
   */

  public synchronized boolean setName(String newName)
  {
    String myNewName;

    /* -- */

    if (isEmbedded() && !newName.startsWith("Embedded: "))
      {
	myNewName = "Embedded: " + newName;
      }
    else
      {
	myNewName = newName;
      }

    if (!store.loading && editor == null)
      {
	throw new IllegalArgumentException("not in an schema editing context");
      }

    // check to make sure another object type isn't using the proposed
    // new name

    synchronized (containingHash)
      {
	Enumeration enum = containingHash.elements();

	while (enum.hasMoreElements())
	  {
	    DBObjectBase base = (DBObjectBase) enum.nextElement();

	    if (!base.equals(this) && base.object_name.equals(myNewName))
	      {
		return false;
	      }
	  }
      }

    // ok, go for it

    object_name = myNewName;
    changed = true;

    return true;
  }

  /**
   * <p>Returns the name of the class managing this object type</p>
   *
   * @see arlut.csd.ganymede.Base
   */
  
  public String getClassName()
  {
    return classname;
  }

  /**
   * <p>Sets the fully qualified classname of the class 
   * managing this object type</p>
   *
   * <p>This method is only valid when the Base reference is obtained
   * from a {@link arlut.csd.ganymede.SchemaEdit SchemaEdit} reference
   * by the Ganymede schema editor.</p>
   *
   * @see arlut.csd.ganymede.Base
   */

  public synchronized void setClassName(String newName)
  {
    if (!store.loading && editor == null)
      {
	throw new IllegalArgumentException("not in an schema editing context");
      }

    // return if no changes

    if (newName.equals(classname))
      {
	return;
      }

    classname = newName;

    if (newName.equals(""))
      {
	return;
      }

    // try to load the proposed class.. if we can't, no big deal,
    // it'll just have to be done after the server is restarted.

    try
      {
	classdef = Class.forName(classname);
	objectHook = this.createHook();
      }
    catch (ClassNotFoundException ex)
      {
	Ganymede.debug("class definition " + classname + " could not be found: " + ex);
	classdef = null;
      }
    catch (RemoteException ex)
      {
	Ganymede.debug("DBObjectBase.setClassName(): local rmi error constructing object hook");
	objectHook = null;
      }

    changed = true;
  }

  /**
   * <p>Returns the class definition for this object type</p>
   */

  public Class getClassDef()
  {
    return classdef;
  }

  /**
   * <p>Returns true if the current session is permitted to
   * create an object of this type.</p>
   *
   * @see arlut.csd.ganymede.Base
   */

  public boolean canCreate(Session session)
  {
    return objectHook.canCreate(session);
  }

  /**
   * <p>Returns true if this object type can be inactivated</p>
   *
   * @see arlut.csd.ganymede.Base
   */

  public synchronized boolean canInactivate()
  {
    return objectHook.canBeInactivated();
  }

  /**
   * <p>Returns the invid type id for this object definition</p>
   *
   * @see arlut.csd.ganymede.Base
   */

  public short getTypeID()
  {
    return type_code;
  }

  /**
   * <p>Returns the short type id for the field designated as this object's
   * primary label field, if any.  Objects do not need to have a primary
   * label field designated if labels for this object type are dynamically
   * generated.</p>
   *
   * @see arlut.csd.ganymede.Base
   */

  public short getLabelField()
  {
    return label_id;
  }

  /**
   * <p>Returns the field name for the field designated as this object's
   * primary label field.  null is returned if no label has been
   * designated.</p>
   *
   * @see arlut.csd.ganymede.Base
   */

  public String getLabelFieldName()
  {
    BaseField bf;

    /* -- */

    if (label_id == -1)
      {
	return null;
      }
    
    bf = getField(label_id);

    if (bf == null)
      {
	return null;
      }

    try
      {
	return bf.getName();
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("caught remote: " + ex);
      }
  }

  /**
   * <p>Returns the invid type id for this object definition as
   * a Short, suitable for use in a hash.</p>
   */

  public Short getKey()
  {
    return new Short(type_code);
  }

  /**
   * <p>Returns {@link arlut.csd.ganymede.DBObjectBaseField DBObjectBaseField}
   * base field definitions for objects of this type.</p>
   *
   * @see arlut.csd.ganymede.Base
   */

  public Vector getFields()
  {
    return getFields(true);
  }

  /**
   * <p>Returns {@link arlut.csd.ganymede.DBObjectBaseField DBObjectBaseField}
   * base field definitions for objects of this type, sorted by field
   * display order..</p>
   *
   * @see arlut.csd.ganymede.Base
   */

  public synchronized Vector getFields(boolean includeBuiltIns)
  {
    Vector result;
    Enumeration enum;
    DBObjectBaseField field;

    /* -- */

    result = new Vector();

    enum = sortedFields.elements();
    
    while (enum.hasMoreElements())
      {
	field = (DBObjectBaseField) enum.nextElement();

	if (includeBuiltIns || !field.isBuiltIn())
	  {
	    result.addElement(field);
	  }
      }

    return result;
  }

  /**
   * <p>Returns the field definition for the field matching id,
   * or null if no match found.</p>
   *
   * @see arlut.csd.ganymede.BaseField
   * @see arlut.csd.ganymede.Base
   */

  public BaseField getField(short id)
  {
    return fieldTable.get(id);
  }

  /**
   * <p>Returns the field definition for the field matching name,
   * or null if no match found.</p>
   *
   * @see arlut.csd.ganymede.BaseField
   * @see arlut.csd.ganymede.Base
   */

  public synchronized BaseField getField(String name)
  {
    BaseField bf;
    Enumeration enum;

    /* -- */

    enum = fieldTable.elements();
    
    while (enum.hasMoreElements())
      {
	bf = (BaseField) enum.nextElement();

	try
	  {
	    if (bf.getName().equals(name))
	      {
		return bf;
	      }
	  }
	catch (RemoteException ex)
	  {
	    // pass through to return null below
	  }
      }

    return null;
  }

  /**
   * <p>Choose what field will serve as this objectBase's label.  A fieldName
   * parameter of null will cause the object's label field to be undefined,
   * in which case the object will have to generate its own label using the
   * {@link arlut.csd.ganymede.DBEditObject#getLabelHook(arlut.csd.ganymede.DBObject) getLabelHook()}
   * method.</p>
   *
   * <p>This method is only valid when the Base reference is obtained
   * from a {@link arlut.csd.ganymede.SchemaEdit SchemaEdit} reference
   * by the Ganymede schema editor.</p>
   *
   * @see arlut.csd.ganymede.Base
   */

  public void setLabelField(String fieldName)
  {
    BaseField bF;

    /* -- */

    if (!store.loading && editor == null)
      {
	throw new IllegalArgumentException("can't call in a non-edit context");
      }

    if (fieldName == null)
      {
	label_id = -1;
	return;
      }

    bF = getField(fieldName);

    if (bF == null)
      {
	throw new IllegalArgumentException("unrecognized field name");
      }

    try
      {
	label_id = bF.getID();
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("runtime except: " + ex);
      }
  }

  /**
   * <p>Choose what field will serve as this objectBase's label.  A fieldID
   * parameter of -1 will cause the object's label field to be undefined,
   * in which case the object will have to generate its own label using the
   * {@link arlut.csd.ganymede.DBEditObject#getLabelHook(arlut.csd.ganymede.DBObject) getLabelHook()}
   * method.</p>
   *
   * <p>This method is only valid when the Base reference is obtained
   * from a {@link arlut.csd.ganymede.SchemaEdit SchemaEdit} reference
   * by the Ganymede schema editor.</p>
   *
   * @see arlut.csd.ganymede.Base
   */

  public void setLabelField(short fieldID)
  {
    if (!store.loading && editor == null)
      {
	throw new IllegalArgumentException("can't call in a non-edit context");
      }

    if ((fieldID != -1) && (null == getField(fieldID)))
      {
	throw new IllegalArgumentException("invalid fieldID");
      }

    label_id = fieldID;
  }

  /**
   * <p>Get the parent Category for this object type.  This is used by the
   * Ganymede client and schema editor to present object types in
   * a hierarchical tree.</p>
   *
   * @see arlut.csd.ganymede.Base
   * @see arlut.csd.ganymede.CategoryNode
   */

  public Category getCategory()
  {
    return category;
  }

  /**
   * <p>Set the objectbase category.  This operation only registers
   * the category in this base, it doesn't register the base in the
   * category.  The proper way to add this base to a Category is to
   * call addNode(Base, nodeBefore) on the appropriate Category
   * object.  That addNode() operation will call setCategory() here.</p>
   *
   * <p>This method is only valid when the Base reference is obtained
   * from a {@link arlut.csd.ganymede.SchemaEdit SchemaEdit} reference
   * by the Ganymede schema editor.</p>
   *
   * @see arlut.csd.ganymede.CategoryNode
   */

  public void setCategory(Category category)
  {
    if (!store.loading && editor == null)
      {
	throw new IllegalArgumentException("can't set category in non-edit context");
      }

    this.category = category;
  }

  /**
   * <p>Creates a new base field, inserts it into the DBObjectBase
   * field definitions hash, and returns a reference to it. </p>
   *
   * <p>If lowRange is true, the field's id will start at 100 and go up,
   * other wise it will start at 256 and go up.</p>
   *
   * <p>This method is only valid when the Base reference is obtained
   * from a {@link arlut.csd.ganymede.SchemaEdit SchemaEdit} reference
   * by the Ganymede schema editor.</p>
   *
   * @see arlut.csd.ganymede.Base
   */
  
  public synchronized BaseField createNewField(boolean lowRange)
  {
    short id;
    DBObjectBaseField field;

    /* -- */

    if (!store.loading && editor == null)
      {
	throw new IllegalArgumentException("can't call in a non-edit context");
      }

    id = getNextFieldID(lowRange);

    try
      {
	field = new DBObjectBaseField(this, editor);
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("couldn't create field due to initialization error: " + ex);
      }

    // set its id

    field.setID(id);

    // and set it up in our field hash and add this to the sorted
    // fields vector, but we won't resort.  We'll leave that to
    // DBObjectBaseField.setDisplayOrder().

    addField(field);

    return field;
  }

  /**
   * <p>This method is used to create a new builtIn field.  This
   * method should *only* be called from DBSchemaEdit, which
   * will insure that the new field will be created with a
   * proper field id, and that before the schema edit transaction
   * is finalized, all non-embedded object types will have an
   * identical built-in field registered.</p>
   *
   * <p>Typically, the DBSchemaEdit code will create a new built-in on
   * the SchemaConstants.UserBase objectbase, and then depend on
   * DBSchemaEdit.synchronizeBuiltInFields() to replicate the
   * new field into all the non-embedded bases.</p>
   *
   * <p>Once again, DBSchemaEdit developMode is *only* to be used when
   * the built-in field types are being modified by a Ganymede developer.</p>
   *
   * <p>Such an action should never be taken lightly, as it *is* necessary
   * to edit the DBObjectBase() constructor afterwards to keep the
   * system consistent for newly created object types.</p>
   */
  
  synchronized DBObjectBaseField createNewBuiltIn(short id)
  {
    DBObjectBaseField field;

    /* -- */

    if (!store.loading && editor == null)
      {
	throw new IllegalArgumentException("can't call in a non-edit context");
      }

    if (!editor.isDevelopMode())
      {
	throw new IllegalArgumentException("error.. createBuiltIn can only be called when the editor is in developMode");
      }

    if (isEmbedded())
      {
	throw new IllegalArgumentException("error.. schema editing built-ins is only supported for non-embedded objects");
      }

    try
      {
	field = new DBObjectBaseField(this, editor);
	field.builtIn = true;
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("couldn't create field due to initialization error: " + ex);
      }

    // set its id

    field.setID(id);

    // and set it up in our field hash

    addField(field);

    return field;
  }

  /**
   * <p>This method is used to remove a field definition from 
   * the current schema.</p>
   *
   * <p>Of course, this removal will only take effect if
   * the schema editor commits.</p>
   *
   * @see arlut.csd.ganymede.Base
   */

  public synchronized boolean deleteField(BaseField bF)
  {
    DBObjectBaseField field = null;
    short id = -1;

    /* -- */

    if (!store.loading && editor == null)
      {
	throw new IllegalArgumentException("can't call in a non-edit context");
      }

    // since we can be called remotely, we need to convert to a local id

    try
      {
	id = bF.getID();
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("couldn't remove field due to remote error: " + ex);
      }
    
    field = (DBObjectBaseField) fieldTable.get(id);

    if (field == null)
      {
	// no such field.

	try
	  {
	    Ganymede.debug("DBObjectBase.deleteField(): couldn't really delete " + getName() + ":" + bF.getName());
	  }
	catch (RemoteException ex)
	  {
	    throw new RuntimeException("couldn't remove field due to remote error: " + ex);
	  }

	return false;
      }

    removeField(field);

    if (debug2)
      {
	Ganymede.debug("field definition " + getName() + ":" + field.getName() + " removed");
      }

    if (id == label_id)
      {
	label_id = -1;
      }

    return true;
  }

  /**
   * <p>This method is used by the SchemaEditor to detect whether any
   * objects are using a field definition.</p>
   *
   * @see arlut.csd.ganymede.Base
   */

  public synchronized boolean fieldInUse(BaseField bF)
  {
    Enumeration enum;

    /* -- */

    try
      {
	enum = objectTable.elements();
	
	while (enum.hasMoreElements())
	  {
	    DBObject obj = (DBObject) enum.nextElement();

	    if (obj.getField(bF.getID()) != null)
	      {
		return true;
	      }
	  }
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("shouldn't happen: " + ex);
      }

    return false;
  }

  /**
   * <p>Returns the display order of this Base within the containing
   * category.</p>
   *
   * @see arlut.csd.ganymede.CategoryNode
   */

  public int getDisplayOrder()
  {
    return displayOrder;
  }

  /**
   * <p>Sets the display order of this Base within the containing
   * category.</p>
   *
   * @see arlut.csd.ganymede.CategoryNode
   */

  public void setDisplayOrder(int displayOrder)
  {
    this.displayOrder = displayOrder;
  }

  /**
   * <p>Helper method for DBEditObject subclasses</p>
   */

  public DBEditObject getObjectHook()
  {
    if (objectHook == null)
      {
	try
	  {
	    objectHook = createHook();
	  }
	catch (RemoteException ex)
	  {
	    throw new RuntimeException("Error, couldn't create hook in getObjectHook(). " + 
				       ex.getMessage());
	  }
      }

    return objectHook;
  }

  /**
   * <p>Get the next available field id for a new field.</p>
   */

  synchronized short getNextFieldID(boolean lowRange)
  {
    short id;
    Enumeration enum;
    DBObjectBaseField fieldDef;

    /* -- */

    if (lowRange)
      {
	id = 100;
      }
    else
      {
	id = 256;  // reserve 256 field slots for built-in types
      }

    enum = fieldTable.elements();

    while (enum.hasMoreElements())
      {
	fieldDef = (DBObjectBaseField) enum.nextElement();

	if (fieldDef.getID() >= id)
	  {
	    id = (short) (fieldDef.getID() + 1);
	  }
      }

    return id;
  }

  /**
   * <p>Clear the editing flag.  This disables the DBObjectBase set
   * methods on this ObjectBase and all dependent field definitions.
   * This method also updates the FieldTemplate for each field and 
   * resorts the field index.</p>
   */
  
  synchronized void clearEditor(DBSchemaEdit editor)
  {
    Enumeration enum;
    DBObjectBaseField fieldDef;

    /* -- */

    if (debug2)
      {
	Ganymede.debug("DBObjectBase.clearEditor(): clearing editor for " + getName());
      }

    if (this.editor != editor)
      {
	throw new IllegalArgumentException("not editing");
      }
    
    this.editor = null;

    if (debug2)
      {
	System.err.println("DBObjectBase.clearEditor(): before sort:");

	displayFieldOrder();
      }

    enum = fieldTable.elements();

    while (enum.hasMoreElements())
      {
	fieldDef = (DBObjectBaseField) enum.nextElement();
	fieldDef.editor = null;
	fieldDef.template = new FieldTemplate(fieldDef);
      }

    // the fields may have been re-ordered.. make sure our
    // sortedFields field is still in order.

    sortFields();

    if (debug2)
      {
	System.err.println("DBObjectBase.clearEditor(): after sort:");

	displayFieldOrder();
      }

    original = null;
  }

  /**
   * <p>This method is used to update base references in objects
   * after this base has replaced an old version via the
   * SchemaEditor.</p>
   */

  synchronized void updateBaseRefs()
  {
    Enumeration enum;
    DBObject obj;

    /* -- */

    enum = objectTable.elements();

    while (enum.hasMoreElements())
      {
	obj = (DBObject) enum.nextElement();
	
	obj.updateBaseRefs(this);
      }
  }

  /**
   * <P>This method is used to allow objects in this base to notify us when
   * their state changes.  It is called from the
   * {@link arlut.csd.ganymede.DBEditSet DBEditSet} commit() method.</P>
   *
   * <P>We use this method to be able to determine the last time anything in
   * this DBObjectBase changed when making decisions as to what needs to
   * be done in BuilderTasks.</P>
   */

  void updateTimeStamp()
  {
    lastChange.setTime(System.currentTimeMillis());
  }

  /**
   * <P>Returns a Date object containing the time that any changes were
   * committed to this DBObjectBase.</P> 
   */

  public Date getTimeStamp()
  {
    return lastChange;
  }

  //
  // the following methods are used to manage locks on this base
  // All methods that modify writerList, readerList, or dumperList
  // must be synchronized on store.
  //

  /**
   * <P>Returns true if this DBObjectBase is currently locked for reading,
   * writing, or dumping.</P>
   */

  boolean isLocked()
  {
    synchronized (store)
      {
	return (!isReaderEmpty() || writeInProgress || dumpInProgress);
      }
  }

  /**
   * <p>Add a DBWriteLock to this base's writer queue.</p>
   */

  boolean addWriter(DBWriteLock writer)
  {
    synchronized (store)
      {
	writerList.addElement(writer);
      }

    return true;
  }

  /**
   * <p>Remove a DBWriteLock from this base's writer queue.</p>
   */

  boolean removeWriter(DBWriteLock writer)
  {
    boolean result;

    synchronized (store)
      {
	result = writerList.removeElement(writer);
	store.notifyAll();

	return result;
      }
  }

  /**
   * <p>Returns true if this base's writer queue is empty.</p>
   */

  boolean isWriterEmpty()
  {
    return writerList.isEmpty();
  }

  /**
   * <p>Returns the size of the writer queue</p>
   */

  int getWriterSize()
  {
    return writerList.size();
  }

  /**
   * <p>Add a DBReadLock to this base's reader list.</p>
   */

  boolean addReader(DBReadLock reader)
  {
    synchronized (store)
      {
	readerList.addElement(reader);
      }

    return true;
  }

  /**
   * <p>Remove a DBReadLock from this base's reader list.</p>
   */

  boolean removeReader(DBReadLock reader)
  {
    boolean result;

    synchronized (store)
      {
	result = readerList.removeElement(reader);

	store.notifyAll();
	return result;
      }
  }

  /**
   * <p>Returns true if this base's reader list is empty.</p>
   */

  boolean isReaderEmpty()
  {
    return readerList.isEmpty();
  }

  /**
   * <p>Returns the size of the reader list</p>
   */

  int getReaderSize()
  {
    return readerList.size();
  }

  /**
   * <p>Add a DBDumpLock to this base's dumper queue.</p>
   */

  boolean addDumper(DBDumpLock dumper)
  {
    synchronized (store)
      {
	dumperList.addElement(dumper);
      }

    return true;
  }

  /**
   * <p>Remove a DBDumpLock from this base's dumper queue.</p>
   */

  boolean removeDumper(DBDumpLock dumper)
  {
    boolean result;

    /* -- */

    synchronized (store)
      {
	result = dumperList.removeElement(dumper);
	
	store.notifyAll();
	return result;
      }
  }

  /**
   * <p>Returns true if this base's dumper list is empty.</p>
   */

  boolean isDumperEmpty()
  {
    return dumperList.isEmpty();
  }

  /**
   * <p>Returns the size of the dumper list</p>
   */

  int getDumperSize()
  {
    return dumperList.size();
  }

  /**
   * <p>Let's get our name here.</p>
   */

  public String toString()
  {
    return "objectBase:" + object_name;
  }

  /**
   * <p>A debug tool, to show how field definitions are currently ordered
   * in this base.</p>
   */

  synchronized void displayFieldOrder()
  {
    for (int i = 0; i < sortedFields.size(); i++)
      {
	DBObjectBaseField bf = (DBObjectBaseField) sortedFields.elementAt(i);

	System.err.println("displayFieldOrder(): field " + bf.getName() + " has order " + bf.getDisplayOrder());
      }
  }

  /**
   * <p>This method is used to put a new field into both the hashed field
   * table and the sortedFields vector.</p>
   */

  synchronized void addField(DBObjectBaseField field)
  {
    fieldTable.put(field);
    sortedFields.addElement(field);
  }

  /**
   * <p>This method is used to remove a field from this base's
   * field database.</p>
   */

  synchronized void removeField(DBObjectBaseField field)
  {
    fieldTable.remove(field.getID());

    if (!sortedFields.removeElement(field))
      {
	Ganymede.debug("DBObject.removeField(): Couldn't find field " + field.getName());
      }
  }

  /**
   * <p>This method reshuffles the fields in keeping with the fields'
   * display order.  Note that there's no guarantee that the field
   * orders will be unique.. the schema editor will keep the
   * non-builtin fields' displayOrder unique, which is what we really
   * care about.</p>
   */

  synchronized void sortFields()
  {
    new VecQuickSort(sortedFields, 
		     new arlut.csd.Util.Compare()
		     {
		       public int compare(Object a, Object b)
			 {
			   DBObjectBaseField aN, bN;

			   aN = (DBObjectBaseField) a;
			   bN = (DBObjectBaseField) b;

			   if (aN.getDisplayOrder() < bN.getDisplayOrder())
			     {
			       return -1;
			     }
			   else if (aN.getDisplayOrder() > bN.getDisplayOrder())
			     {
			       return 1;
			     }
			   else
			     {
			       return 0;
			     }
			 }
		     }
		     ).sort();
  }
}
