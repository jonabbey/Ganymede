/*
   GASH 2

   DBObjectBase.java

   The GANYMEDE object storage system.

   Created: 2 July 1996
   Release: $Name:  $
   Version: $Revision: 1.134 $
   Last Mod Date: $Date: 2002/08/07 18:39:20 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999, 2000, 2001, 2002
   The University of Texas at Austin.

   Contact information

   Web site: http://www.arlut.utexas.edu/gash2
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
   Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
   02111-1307, USA

*/

package arlut.csd.ganymede;

import java.lang.reflect.*;
import java.io.*;
import java.util.*;
import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;

import com.jclark.xml.output.*;
import arlut.csd.Util.*;
import arlut.csd.JDialog.JDialogBuff;

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

  /**
   * <P>More debugging.</P>
   */

  final static boolean debug2 = false;
  final static boolean xmldebug = false;

  public static void setDebug(boolean val)
  {
    System.err.println("DBObjectBase.setDebug(): " + val);
    debug = val;
  }

  private static arlut.csd.Util.Compare comparator =
    new arlut.csd.Util.Compare() {
    public int compare(Object a, Object b) 
      {
	DBObjectBaseField aF, bF;

	aF = (DBObjectBaseField) a;
	bF = (DBObjectBaseField) b;

	if (aF.tmp_displayOrder < bF.tmp_displayOrder)
	  {
	    return -1;
	  }
	else if (bF.tmp_displayOrder > aF.tmp_displayOrder)
	  { 
	    return 1;
	  } 
	else
	  { 
	    return 0;
	  }
      }
  };

  /* - */

  /**
   * <P>The central Ganymede database object that this object base is contained
   * within.</P>
   */

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
   * what category is this object in?
   */

  Category category;

  /**
   * <P>If true, this type of object is used as a target for an
   * edit-in-place {@link arlut.csd.ganymede.InvidDBField
   * InvidDBField}.</P>
   */

  private boolean embedded;

  // runtime data

  /**
   * Custom field dictionary sorted in display order.  This Vector
   * does *not* include any built-in fields.  Elements of this Vector
   * are {@link arlut.csd.ganymede.DBObjectBaseField
   * DBObjectBaseFields}.
   */

  Vector customFields;

  /**
   * <P>Cached template vector</P>
   */

  Vector templateVector;
  
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
   * used only during loading of pre-2.0 format ganymede.db files
   */

  int tmp_displayOrder = -1;

  /**
   * <P>Timestamp for the last time this DBObjectBase was
   * changed, used by 
   * {@link arlut.csd.ganymede.GanymedeBuilderTask GanymedeBuilderTasks} 
   * to determine whether a particular build sequence is necessary.</P>
   */

  Date lastChange;

  /**
   * <P>If this DBObjectBase is locked with an exclusive lock
   * (a {@link arlut.csd.ganymede.DBWriteLock DBWriteLock}),
   * this field will point to it.</P>
   *
   * <P>This field is not currently used for anything in particular
   * in the lock logic, it is here strictly for informational/debugging
   * purposes.</P>
   */

  DBLock currentLock;

  /**
   * <P>Set of {@link arlut.csd.ganymede.DBWriteLock DBWriteLock}s
   * pending on this DBObjectBase.  DBWriteLocks will add themselves
   * to the writerList upon entering establish().  If writerList is
   * not empty, no new {@link arlut.csd.ganymede.DBReadLock
   * DBReadLock}s will be allowed to add to add themselve to the readerList
   * in this DBObjectBase.  {@link arlut.csd.ganymede.DBDumpLock DBDumpLock}s
   * don't check the writerList, and will add themselves to the dumperList
   * as needed, which will block any further writers from queuing up
   * in the list.</P>
   *
   * <P>When a DBWriteLock is locked onto this base, it is taken out
   * of writerList, writeInProgress is set to true, and currentLock 
   * is set to point to the DBWriteLock that has exclusive access.</P>
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
   * <P>Set of {@link arlut.csd.ganymede.DBDumpLock DBDumpLock}s
   * pending on this DBObjectBase.  DBDumpLocks will add themselves to
   * the dumperList upon entering establish().  If dumperList is not
   * empty, no new {@link arlut.csd.ganymede.DBWriteLock DBWriteLock}s
   * will be allowed to add themselves to the {@link
   * arlut.csd.ganymede.DBObjectBase#writerList writerList} in this
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
   * <P>Collection of {@link arlut.csd.ganymede.DBDumpLock DBDumpLock}s
   * that are locked on this DBObjectBase.</P>
   */

  private Vector dumpLockList;

  /**
   * <P>Boolean flag monitoring whether or not this DBObjectBase is
   * currently locked for writing.</P>
   */

  boolean writeInProgress;

  /**
   * Used to keep track of schema editing
   */

  DBSchemaEdit editor;

  /**
   * <P>This Vector holds the current collection of {@link
   * arlut.csd.ganymede.DBObject DBObject} objects in this
   * DBObjectBase, for enumeration access.  The GanymedeSession query
   * logic iterates over this Vector so that querying on single bases
   * can proceed while commits are under way.</P>
   *
   * <P>This is practicable because assignment to this variable is
   * an inherently atomic event in the Java spec, so we just wait
   * to assign a new Vector here until we have a new one composed.  We
   * just have to depend on all code that accesses this vector to grab
   * its own reference to this vector and then not modify it, and to
   * drop reference to it when the iteration is complete.</P>
   */

  private Vector iterationSet;

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

  /**
   * <P>Generic constructor.</P>
   *
   * @param store The DBStore database this DBObjectBase is being created for.
   * @param embedded If true, objects of this DBObjectBase type will not
   * be top-level objects, but rather will be embedded using edit-in-place
   * {@link arlut.csd.ganymede.InvidDBField InvidDBFields}.
   */

  public DBObjectBase(DBStore store, boolean embedded) throws RemoteException
  {
    this(store, embedded, true);
  }

  /**
   * <P>This constructor actually does all the work of initializing a new
   * DBObjectBase.  All other constructors for DBObjectBase will eventually
   * call this constructor.</P>
   *
   * @param store The DBStore database this DBObjectBase is being created for.
   * @param embedded If true, objects of this DBObjectBase type will not
   * be top-level objects, but rather will be embedded using edit-in-place
   * {@link arlut.csd.ganymede.InvidDBField InvidDBFields}.
   * @param createFields If true, the standard fields required by the server
   * for its own operations will be created as part of DBObjectBase creation.  This
   * should be false if this DBObjectBase is being created in the process of loading
   * data from a pre-existing database which will presumably already have all
   * essential fields defined.
   */

  public DBObjectBase(DBStore store, boolean embedded, boolean createFields) throws RemoteException
  {
    super();			// initialize UnicastRemoteObject

    debug = Ganymede.debug;

    this.store = store;

    writerList = new Vector();
    readerList = new Vector();
    dumperList = new Vector();
    dumpLockList = new Vector();
    iterationSet = new Vector();

    object_name = "";
    classname = "";
    classdef = null;
    type_code = -1;
    label_id = -1;
    category = null;
    customFields = new Vector();
    fieldTable = new DBBaseFieldTable(20, (float) 1.0);
    objectTable = new DBObjectTable(4000, (float) 1.0);
    maxid = 0;
    lastChange = new Date();

    editor = null;

    this.embedded = embedded;

    if (createFields)
      {
	createBuiltIns(embedded);
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
    this.editor = editor;
    setTypeID(id);
  }

  /**
   * <p>receive constructor.  Used to initialize this DBObjectBase from disk
   * and load the objects of this type in from the standing store.</p>
   *
   * @param in Input stream to read this object base from.
   * @param store The Ganymede database object we are loading into.
   */

  public DBObjectBase(DataInput in, DBStore store) throws IOException, RemoteException
  {
    // create an empty object base without creating the built in
    // fields.. we'll load fields and create the system standard
    // fields once we know whether the newly loaded object definition
    // is for an embedded object or not.

    this(store, false, false);

    receive(in);

    // need to recreate objectHook now that we have loaded our classdef info
    // from disk.

    objectHook = this.createHook();
  }

  /**
   * <p>copy constructor.  Used to create a copy that we can play with for
   * schema editing.</p>
   */

  public DBObjectBase(DBObjectBase original, DBSchemaEdit editor) throws RemoteException
  {
    this(original.store, original.embedded, true);
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
	embedded = original.embedded;
    
	// make copies of all the custom field definitions for this
	// object type, and save them into our own field hash.
    
	Enumeration enum;
	DBObjectBaseField field;
    
	enum = original.customFields.elements();

	while (enum.hasMoreElements())
	  {
	    field = (DBObjectBaseField) enum.nextElement();
	    bf = new DBObjectBaseField(field, editor); // copy this base field
	    bf.base = this;

	    addFieldToEnd(bf);
	  }

	// remember the objects.. note that we don't at this point notify
	// the objects that this new DBObjectBase is their owner.. we'll
	// take care of that when and if the DBSchemaEdit base editing session
	// commits this copy

	objectTable = original.objectTable;
	iterationSet = original.iterationSet; // this is safe to do only in the schema editing context

	maxid = original.maxid;
    
	objectHook = original.objectHook;

	lastChange = new Date();
      }
  }

  synchronized void emit(DataOutput out, boolean dumpObjects) throws IOException
  {
    Enumeration baseEnum;

    /* -- */

    out.writeUTF(object_name);

    if (classname == null)
      {
	out.writeUTF("");
      }
    else
      {
	out.writeUTF(classname);
      }

    out.writeShort(type_code);

    out.writeShort((short) customFields.size()); // should have no more than 32k fields

    // and write out the field definitions, in order

    for (int i = 0; i < customFields.size(); i++)
      {
	DBObjectBaseField fieldDef = (DBObjectBaseField) customFields.elementAt(i);

	fieldDef.emit(out);
      }
    
    out.writeShort(label_id);	// added at file version 1.1

    out.writeBoolean(embedded);	// added at file version 1.5

    if (dumpObjects)
      {
	out.writeInt(maxid);	// added at file version 1.12

	out.writeInt(objectTable.size());
   
	baseEnum = objectTable.elements();

	while (baseEnum.hasMoreElements())
	  {
	    ((DBObject) baseEnum.nextElement()).emit(out);
	  }
      }
    else
      {
	out.writeInt(0);	// maxid added at file version 1.12

	out.writeInt(0);	// table size
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

    setName(in.readUTF());	// we use setName to filter out any bad chars in transition to 1.0

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

    // how many field definitions?

    size = in.readShort();

    if (debug)
      {
	System.err.println("DBObjectBase.receive(): " + size + " fields in dictionary");
      }

    // read in the custom field dictionary for this object

    for (int i = 0; i < size; i++)
      {
	field = new DBObjectBaseField(in, this);

	// skip any system standard field definitions, which will
	// be created in the field table separately

	if (field.getID() <= SchemaConstants.FinalSystemField)
	  {
	    continue;		// don't save the db's version of a system standard field
	  }

	if (debug2)
	  {
	    System.err.println("DBObjectBaseField.receive(): " + field);
	  }

	addFieldToEnd(field);
      }

    // if we're reading an old ganymede.db file, sort the customFields
    // for 2.0 the customFields will have been read in sort order

    if (store.isLessThan(2,0))
      {
	new VecQuickSort(customFields, comparator).sort();

	if (false)
	  {
	    System.err.println("** Sorted DBObjectBase " + getName());

	    for (int i = 0; i < customFields.size(); i++)
	      {
		Object x = customFields.elementAt(i);

		if (x instanceof DBObjectBaseField)
		  {
		    System.err.print("Field [" + ((DBObjectBaseField) x).tmp_displayOrder);
		    System.err.println("] = " + ((DBObjectBaseField) x).getName());
		  }
		else
		  {
		    System.err.println("**XXX***");
		  }
	      }
	  }
      }

    // at file version 1.1, we introduced label_id's.

    if (store.isAtLeast(1,1))
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
    // at file version 2.0, we took the category specification out of
    // the DBObjectBase block in favor of having it defined by context
    // of the DBBaseCategory this DBObjectBase was read in.

    if (store.isBetweenRevs(1,3,2,0))
      {
	String categoryName = in.readUTF();

	category = (DBBaseCategory) store.getCategoryNode(categoryName);

	if (category != null)
	  {
	    category.addNodeAfter(this, null); // add to end of category
	  }
      }

    // if we're reading an old ganymede.db file, read in the display
    // order for this base.  if we're at 2.0 or later, the
    // DBObjectBase will be read in order within its category from the
    // file.

    if (store.isBetweenRevs(1,4,2,0))
      {
	tmp_displayOrder = in.readInt();
      }
    else
      {
	tmp_displayOrder = -1;
      }

    if (store.isAtLeast(1,5))
      {
	embedded = in.readBoolean(); // added at file version 1.5
      }

    // create the system standard fields for this object definition
    // now that we know whether the object is embedded or not

    createBuiltIns(embedded);

    if (store.isAtLeast(1,12))
      {
	maxid = in.readInt(); // added at file version 1.12
      }

    // read in the objects belonging to this ObjectBase

    object_count = in.readInt();

    if (debug)
      {
    	System.err.println("DBObjectBase.receive(): reading " + object_count + " objects");
      }

    Vector tmpIterationSet = new Vector(object_count);

    temp_val = (object_count > 0) ? (object_count * 2 + 1) : 4000;

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

	tmpIterationSet.addElement(tempObject);
	objectTable.putNoSyncNoRemove(tempObject);
	tempObject.setBackPointers(); // register anonymous invid fields
      }
    
    // and switch it in

    this.iterationSet = tmpIterationSet;

    if (debug)
      {
	System.err.println("DBObjectBase.receive(): maxid for " + object_name + " is " + maxid);
      }
  }

  /**
   * <P>This method is used to instantiate the system default fields in a newly
   * created or loaded DBObjectBase.</P>
   */

  private synchronized void createBuiltIns(boolean embedded)
  {
    DBObjectBaseField bf;

    /* -- */

    if (embedded)
      {
	/* Set up our 0 field, the containing object owning us */

	bf = addSystemField("Containing Object",
			    SchemaConstants.ContainerField,
			    FieldType.INVID);

	bf.allowedTarget = -1;	// we can point at anything, but there'll be a special
	bf.targetField = -1;	// procedure for handling deletion and what not..
	bf.visibility = false;	// we don't want the client to show the owner link

	// note that we won't have an expiration date or removal date
	// for an embedded object
      }
    else
      {
	/* Set up our 0 field, the owner list. */

	bf = addSystemField("Owner list",
			    SchemaConstants.OwnerListField,
			    FieldType.INVID);

	bf.allowedTarget = SchemaConstants.OwnerBase;
	bf.targetField = SchemaConstants.OwnerObjectsOwned;
	bf.array = true;

	addSystemField("Expiration Date",
		       SchemaConstants.ExpirationField,
		       FieldType.DATE);

	addSystemField("Removal Date",
		       SchemaConstants.RemovalField,
		       FieldType.DATE);

	addSystemField("Notes",
		       SchemaConstants.NotesField,
		       FieldType.STRING);

	addSystemField("Creation Date",
		       SchemaConstants.CreationDateField,
		       FieldType.DATE);

	addSystemField("Creator Info",
		       SchemaConstants.CreatorField,
		       FieldType.STRING);

	addSystemField("Modification Date",
		       SchemaConstants.ModificationDateField,
		       FieldType.DATE);

	addSystemField("Modifier Info",
		       SchemaConstants.ModifierField,
		       FieldType.STRING);
      }
  }

  /**
   * <P>This method dumps schema information to an XML stream.</P>
   */

  synchronized void emitXML(XMLDumpContext xmlOut) throws IOException
  {
    xmlOut.startElementIndent("objectdef");
    xmlOut.attribute("name", XMLUtils.XMLEncode(object_name));
    xmlOut.attribute("id", java.lang.Short.toString(type_code));
    xmlOut.indentOut();

    if (classname != null && !classname.equals(""))
      {
	xmlOut.startElementIndent("classdef");
	xmlOut.attribute("name", classname);
	xmlOut.endElement("classdef");
      }

    if (embedded)
      {
	xmlOut.startElementIndent("embedded");
	xmlOut.endElement("embedded");
      }

    if (label_id != -1)
      {
	xmlOut.startElementIndent("label");
	xmlOut.attribute("fieldid", java.lang.Integer.toString(label_id));
	xmlOut.endElement("label");
      }

    synchronized (customFields)
      {
	for (int i = 0; i < customFields.size(); i++)
	  {
	    DBObjectBaseField fieldDef = (DBObjectBaseField) customFields.elementAt(i);

	    fieldDef.emitXML(xmlOut);
	  }
      }

    xmlOut.indentIn();
    xmlOut.endElementIndent("objectdef");
  }

  /**
   * <P>This method is used to read the definition for this
   * DBObjectBase from an XMLItem &lt;objectdef&gt; tree.</P>
   */

  synchronized ReturnVal setXML(XMLItem root, boolean resolveInvidLinks, PrintWriter err)
  {
    XMLItem item;
    String _objectName = null;
    Integer _idInt;
    DBObjectBaseField newField;
    Hashtable nameTable = new Hashtable();
    Hashtable idTable = new Hashtable();
    String _classStr = null;
    Integer _labelInt = null;
    boolean _embedded = false;
    boolean classSet = false;
    boolean labelSet = false;
    Vector fieldsInXML = new Vector(); // order vector of Integer field id's
    Vector fieldsInBase = new Vector(); // vector of Integer field id's previously in schema
    Vector _fieldsToDelete; // vector of Integer field id's to be deleted from schema
    ReturnVal retVal;

    /* -- */

    if (root == null || !root.matches("objectdef"))
      {
	throw new IllegalArgumentException("DBObjectBase.setXML(): root element != open objectdef: " + 
					   root);
      }

    if (xmldebug)
      {
	err.println("Setting XML for object Base.." + root);
      }

    // GanymedeXMLSession.processSchema does a handleBaseRenaming up
    // front, but if we are a newly created base we might not have had
    // our name set yet.. go ahead and try to do it here
    
    _objectName = XMLUtils.XMLDecode(root.getAttrStr("name"));

    if (_objectName == null || _objectName.equals(""))
      {
	return Ganymede.createErrorDialog("xml",
					  "DBObjectBase.setXML(): objectdef missing name attribute:\n " + 
					  root.getTreeString());
      }

    // we call setName() at the bottom, after we know for sure what our
    // embedded status is going to be

    _idInt = root.getAttrInt("id");

    if (_idInt == null)
      {
	return Ganymede.createErrorDialog("xml",
					  "DBObjectBase.setXML(): objectdef missing id attribute:\n " + 
					  root.getTreeString());
      }

    if (xmldebug)
      {
	err.println("Setting id");
      }

    retVal = setTypeID(_idInt.shortValue());

    if (retVal != null && !retVal.didSucceed())
      {
	return retVal;
      }

    // first scan the children nodes, make sure all fields have unique
    // names and id's, build up a list of Integer field id's for us to
    // compare against the list of field id's currently defined

    if (xmldebug)
      {
	err.println("Scanning fields");
      }

    XMLItem children[] = root.getChildren();

    for (int i = 0; i < children.length; i++)
      {
	item = children[i];

	if (item.matches("fielddef"))
	  {
	    String _fieldNameStr = XMLUtils.XMLDecode(item.getAttrStr("name"));
	    Integer _fieldIDInt = item.getAttrInt("id");


	    if (_fieldNameStr == null || _fieldIDInt == null)
	      {
		return Ganymede.createErrorDialog("xml",
						  "Field definition missing name and/or id: " + 
						  item.getTreeString());
	      }

	    if (nameTable.containsKey(_fieldNameStr))
	      {
		return Ganymede.createErrorDialog("xml",
						  "More than one field in objectdef: " + 
						  root.getTreeString() + 
						  "\ncontains field name " + _fieldNameStr);
	      }

	    DBObjectBaseField _field = (DBObjectBaseField) getField(_fieldNameStr);

	    if (_field != null && _field.isBuiltIn())
	      {
		return Ganymede.createErrorDialog("xml",
						  "Can't set a field:\n " + item.getTreeString() + 
						  "\nwith the same name as a pre-existing built-in field in objectdef:\n" +
						  root.getTreeString());
	      }

	    nameTable.put(_fieldNameStr, item);

	    if (_fieldIDInt.shortValue() < 100)
	      {
		return Ganymede.createErrorDialog("xml",
						  "Can't modify or set a field:\n " + item.getTreeString() + 
						  "\nwith a field id in the global field range:\n" +
						  root.getTreeString());
	      }

	    if (idTable.containsKey(_fieldIDInt))
	      {
		return Ganymede.createErrorDialog("xml",
						  "More than one field in objectdef: " + root.getTreeString() + 
						  "\ncontains field id " + _fieldIDInt);
	      }

	    idTable.put(_fieldIDInt, item);

	    fieldsInXML.addElement(_fieldIDInt);
	  }
      }

    if (xmldebug)
      {
	err.println("Calculating fields to delete");
      }

    // we've got a vector of Integers for the field id's we are going to be
    // setting.. we now need to find out field id's that are missing
    // in the xml, as we'll need to delete these

    for (int i = 0; i < customFields.size(); i++)
      {
	DBObjectBaseField _field = (DBObjectBaseField) customFields.elementAt(i);

	fieldsInBase.addElement(new Integer(_field.getID()));
      }

    _fieldsToDelete = VectorUtils.difference(fieldsInBase, fieldsInXML);

    for (int i = 0; i < _fieldsToDelete.size(); i++)
      {
	Integer _fieldID = (Integer) _fieldsToDelete.elementAt(i);

	DBObjectBaseField _field = (DBObjectBaseField) getField(_fieldID.shortValue());

	err.println("\t\tDeleting field " + _field.getName());

	retVal = deleteField(_field.getName());

	if (retVal != null && !retVal.didSucceed())
	  {
	    return retVal;
	  }
      }

    // we've done our first data integrity test and we've figured out
    // what fields we're going to need to delete.. go ahead and scan
    // ahead and actually create and/or edit fields as needed

    for (int i = 0; i < children.length; i++)
      {
	item = children[i];

	if (item.matches("classdef"))
	  {
	    if (classSet)
	      {
		return Ganymede.createErrorDialog("xml",
						  "Objectdef contains more than one classdef element:\n" +
						  root.getTreeString());
	      }

	    _classStr = item.getAttrStr("name");

	    classSet = true;
	  }
	else if (item.matches("embedded"))
	  {
	    _embedded = true;
	  }
	else if (item.matches("label"))
	  {
	    if (labelSet)
	      {
		return Ganymede.createErrorDialog("xml",
						  "Objectdef contains more than one label element:\n" +
						  root.getTreeString());
	      }

	    _labelInt = item.getAttrInt("fieldid");

	    labelSet = true;
	  }
	else if (item.matches("fielddef"))
	  {
	    newField = (DBObjectBaseField) getField(item.getAttrInt("id").shortValue());

	    if (newField == null)
	      {
		err.println("\t\tCreating field " + item.getAttrStr("name"));

		try
		  {
		    newField = new DBObjectBaseField(this, editor);
		  }
		catch (RemoteException ex)
		  {
		    ex.printStackTrace();
		    throw new RuntimeException("UnicastRemoteObject initialization error " + ex.getMessage());
		  }

		if (xmldebug)
		  {
		    err.println("Setting XML on new field " + item);
		  }

		retVal = newField.setXML(item, resolveInvidLinks, err);
		
		if (retVal != null && !retVal.didSucceed())
		  {
		    return retVal;
		  }

		addFieldToEnd(newField);
	      }
	    else
	      {
		err.println("\t\tEditing field " + item.getAttrStr("name"));
		
		retVal = newField.setXML(item, resolveInvidLinks, err);

		if (retVal != null && !retVal.didSucceed())
		  {
		    return retVal;
		  }
	      }
	  }
	else
	  {
	    return Ganymede.createErrorDialog("xml",
					      "Unrecognized XML item: " + item + " in objectdef:\n" +
					      root.getTreeString());
	  }
      }

    // and set or clear the label and class options

    if (xmldebug)
      {
	err.println("Setting label field");
      }

    if (_labelInt == null)
      {
	retVal = setLabelField(null);
      }
    else
      {
	retVal = setLabelField(_labelInt.shortValue());
      }
    
    if (retVal != null && !retVal.didSucceed())
      {
	return retVal;
      }

    if (xmldebug)
      {
	err.println("Setting class name");
      }
    
    retVal = setClassName(_classStr);
    
    if (retVal != null && !retVal.didSucceed())
      {
	return retVal;
      }

    // we have to set embedded before calling setName() so that
    // setName() can enforce the embedded object naming convention

    embedded = _embedded;	// XXX need to work on this

    if (xmldebug)
      {
	err.println("Setting object name");
      }
    
    retVal = setName(_objectName);
    
    if (retVal != null && !retVal.didSucceed())
      {
	return retVal;
      }

    // and we need to order the fields in customFields in the same
    // order they appeared in the XML

    if (xmldebug)
      {
	err.println("Sorting fields");
      }

    Vector _newCustom = new Vector();

    for (int i = 0; i < fieldsInXML.size(); i++)
      {
	Integer _fieldID = (Integer) fieldsInXML.elementAt(i);

	DBObjectBaseField _field = (DBObjectBaseField) getField(_fieldID.shortValue());

	if (_field == null)
	  {
	    return Ganymede.createErrorDialog("xml",
					      "Couldn't find field " + _fieldID +
					      "while resorting customFields.");
	  }

	_newCustom.addElement(_field);
      }

    // make sure we are consistent like we think we should be

    Vector _intersection = VectorUtils.intersection(customFields, _newCustom);

    if ((_intersection.size() != customFields.size()) ||
	(_intersection.size() != _newCustom.size()))
      {
	err.println("Consistency error while resorting customFields in base " + getName());

	err.println("customFields.size() = " + customFields.size());
	err.println("_newCustom.size() = " + _newCustom.size());
	err.println("_intersection.size() = " + _intersection.size());

	return Ganymede.createErrorDialog("xml",
					  "Consistency error while resorting customFields.");
      }
    
    customFields = _newCustom;

    // and we're done

    if (xmldebug)
      {
	err.println("Done processing object base " + root);
      }

    return null;
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
		// it might seem like we would emit this message a lot
		// of a class file wasn't in our custom.jar file, but
		// in fact createHook only gets called once
		// usually.. even if we can't find the class, we'll
		// pass back a default DBEditObject

		System.err.println("DBObjectBase.createHook(): class definition could not be found: " + ex);
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
	invid = Invid.createInvid(getTypeID(), getNextID());

	if (debug2)
	  {
	    if (objectTable.containsKey(chosenSlot.getNum()))
	      {
		throw new IllegalArgumentException("bad invid chosen in createNewObject: num already taken");
	      }
	  }
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

	if (maxid < invid.getNum())
	  {
	    maxid = invid.getNum();
	  }
      }

    // it is crucial that we will have called createHook() before
    // getting here, or else we might return a non-differentiated
    // DBEditObject instead of a proper object of custom class type

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
   * <p>Print a debugging summary of the custom type information encoded
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

    out.println("<table border>");
    out.println("<tr>");
    out.println("<th>Field Name</th> <th>Field ID</th> <th>Field Type</th>");
    out.println("<th>Array?</th> <th>NameSpace</th> <th>Notes</th>");
    out.println("</tr>");

    enum = customFields.elements();

    while (enum.hasMoreElements())
      {
	bf = (DBObjectBaseField) enum.nextElement();

	out.println("<tr>");
	bf.printHTML(out);
	out.println("</tr>");
      }

    out.println("</table>");
    out.println("<br>");
  }

  /**
   * <p>Print a debugging summary of the custom type information encoded
   * in this objectbase to a PrintWriter.</p>
   *
   * @param out PrintWriter to print to.
   */

  public synchronized void print(PrintWriter out, String indent)
  {
    Enumeration enum;
    DBObjectBaseField fieldDef;

    /* -- */

    out.println(indent + object_name + "(" + type_code + ")");
    
    enum = customFields.elements();

    while (enum.hasMoreElements())
      {
	fieldDef = (DBObjectBaseField) enum.nextElement();

	fieldDef.print(out, indent + "\t");
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
   * <p>Returns the name of this object type. Guaranteed
   * to be unique in the Ganymede server. </p>
   *
   * @see arlut.csd.ganymede.Base
   */

  public String getName()
  {
    return object_name;
  }

  /**
   * Returns the name and category path of this object type.
   * Guaranteed to be unique in the Ganymede server.
   */

  public String getPath()
  {
    if (category == null)
      {
	return "./" + getName();
      }

    try
      {
	return category.getPath() + "/" + object_name;
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException(ex.getMessage());
      }
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

  public synchronized ReturnVal setName(String newName)
  {
    if (!store.loading && editor == null)
      {
	throw new IllegalArgumentException("not in a schema editing context");
      }

    // make sure we strip any chars that would cause this object name
    // to not be a valid XML entity name character.  We make an
    // exception for spaces, which we will replace with underscores as
    // an XML char.

    newName = StringUtils.strip(newName,
				"abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 .-").trim();

    // no change, no harm

    if (newName.equals(object_name))
      {
	return null;
      }

    // check to make sure another object type isn't using the proposed
    // new name

    if (this.editor != null)
      {
	if (this.editor.getBase(newName) != null)
	  {
	    return Ganymede.createErrorDialog("Schema Editing Error",
					      "Can't rename base " + object_name + 
					      " to " + newName + ", that name is already taken.");
	  }
      }
    else
      {
	if (this.store.getObjectBase(newName) != null)
	  {
	    return Ganymede.createErrorDialog("Schema Editing Error",
					      "Can't rename base " + object_name + 
					      " to " + newName + ", that name is already taken.");
	  }
      }

    // ok, go for it

    object_name = newName;

    return null;
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

  public synchronized ReturnVal setClassName(String newName)
  {
    if (!store.loading && editor == null)
      {
	throw new IllegalArgumentException("not in a schema editing context");
      }

    // return if no changes

    if (newName == classname || (newName != null && newName.equals(classname)))
      {
	return null;
      }

    if (newName == null || newName.equals(""))
      {
	classname = "";
	classdef = null;
	objectHook = null;
	return null;
      }

    classname = newName;

    // try to load the proposed class.. if we can't, no big deal,
    // it'll just have to be done after the server is restarted.

    try
      {
	classdef = Class.forName(classname);
	objectHook = this.createHook();
      }
    catch (ClassNotFoundException ex)
      {
	classdef = null;

	ReturnVal retVal = new ReturnVal(true);	// success, but...
	retVal.setDialog(new JDialogBuff("Schema Editor Warning",
					 "Couldn't find class " + classname +
					 " in the server's CLASSPATH.  This probably means that " +
					 "you have not yet rebuilt the custom.jar file with this class " +
					 "added.\n\nThis classname has been set for object type " + getName() +
					 " in " +
					 "the server, but will not take effect until the server is restarted " +
					 "with this class available to the server.",
					 "Ok",
					 null,
					 "error.gif"));

	return retVal;
      }
    catch (RemoteException ex)
      {
	Ganymede.debug("DBObjectBase.setClassName(): local rmi error constructing object hook");
	objectHook = null;
      }

    return null;
  }

  /**
   * <p>Returns the class definition for this object type</p>
   */

  public Class getClassDef()
  {
    return classdef;
  }

  /**
   * <p>This method is used to adjust the ordering of a custom field
   * in this Base.</p>
   *
   * @param fieldName The name of the field to move
   * @param previousFieldName The name of the field that fieldName is going to
   * be put after, or null if fieldName is to be the first field displayed
   * in this object type.
   */

  public synchronized ReturnVal moveFieldAfter(String fieldName, String previousFieldName)
  {
    DBObjectBaseField oldField, prevField;

    /* -- */

    oldField = (DBObjectBaseField) getField(fieldName);

    if (oldField == null)
      {
	return Ganymede.createErrorDialog("Schema Editing Error",
					  "Error, can't move field " + fieldName +
					  ", no such field in object type.");
      }

    if (previousFieldName == null || previousFieldName.equals(""))
      {
	customFields.removeElement(oldField);
	customFields.insertElementAt(oldField, 0);
	return null;
      }

    prevField = (DBObjectBaseField) getField(previousFieldName);

    if (prevField == null || !customFields.contains(prevField))
      {
	return Ganymede.createErrorDialog("Schema Editing Error",
					  "Error, can't move field " + fieldName +
					  " after " + previousFieldName +
					  ", no such field in object type.");
      }

    customFields.removeElement(oldField);
    customFields.insertElementAt(oldField, customFields.indexOf(prevField) + 1);

    return null;
  }

  /**
   * <p>This method is used to adjust the ordering of a custom field
   * in this Base.</p>
   *
   * @param fieldName The name of the field to move
   * @param nextFieldName The name of the field that fieldName is going to
   * be put before, or null if fieldName is to be the last field displayed
   * in this object type.
   */

  public synchronized ReturnVal moveFieldBefore(String fieldName, String nextFieldName)
  {
    DBObjectBaseField oldField, nextField;

    /* -- */

    oldField = (DBObjectBaseField) getField(fieldName);

    if (oldField == null)
      {
	return Ganymede.createErrorDialog("Schema Editing Error",
					  "Error, can't move field " + fieldName +
					  ", no such field in object type.");
      }

    if (nextFieldName == null || nextFieldName.equals(""))
      {
	customFields.removeElement(oldField);
	customFields.addElement(oldField);
	return null;
      }

    nextField = (DBObjectBaseField) getField(nextFieldName);

    if (nextField == null || !customFields.contains(nextField))
      {
	return Ganymede.createErrorDialog("Schema Editing Error",
					  "Error, can't move field " + fieldName +
					  " before " + nextFieldName +
					  ", no such field in object type.");
      }

    customFields.removeElement(oldField);
    customFields.insertElementAt(oldField, customFields.indexOf(nextField));

    return null;
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
   * <p>Sets the object ID code for this object type</p>
   */

  public synchronized ReturnVal setTypeID(short objectId)
  {
    if (objectId == type_code)
      {
	return null;
      }

    if (!store.loading && editor == null)
      {
	throw new IllegalArgumentException("not in a schema editing context");
      }

    if ((objectId != type_code) && (type_code != -1))
      {
	return Ganymede.createErrorDialog("xml",
					  "Can't change the type_code for an existing object base");
      }

    if (store.getObjectBase(objectId) != null)
      {
	return Ganymede.createErrorDialog("xml",
					  "Can't set the type_code for object base " + toString() +
					  " to that of an existing object base");
      }

    type_code = objectId;

    return null;
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
   * <p>Returns all {@link arlut.csd.ganymede.DBObjectBaseField DBObjectBaseField}
   * base field definitions for objects of this type, in random order.</p>
   *
   * @see arlut.csd.ganymede.Base
   */

  public Vector getFields()
  {
    return getFields(true);
  }

  /**
   * <p>Returns {@link arlut.csd.ganymede.DBObjectBaseField DBObjectBaseField}
   * base field definitions for objects of this type.
   *
   * <P>If includeBuiltIns is false, the fields returned will be the
   * custom fields defined for this object type, and they will be
   * returned in display order.  If includeBuiltIns is true, the
   * built-in fields will be appended to the Vector after the custom
   * types, in random order.</P>
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

    // first we return the custom fields, in display order

    enum = customFields.elements();
    
    while (enum.hasMoreElements())
      {
	field = (DBObjectBaseField) enum.nextElement();
	
	result.addElement(field);
      }

    // now if we are to return the built-in fields, go ahead and add
    // them in whatever hashing order we find them

    if (includeBuiltIns)
      {
	enum = fieldTable.elements();

	while (enum.hasMoreElements())
	  {
	    field = (DBObjectBaseField) enum.nextElement();

	    if (field.isBuiltIn())
	      {	    
		result.addElement(field);
	      }
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

  public ReturnVal setLabelField(String fieldName)
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
	return null;
      }

    bF = getField(fieldName);

    if (bF == null)
      {
	return Ganymede.createErrorDialog("Schema Editing Error",
					  "setLabelField() called with an unrecognized field name.");
      }

    try
      {
	label_id = bF.getID();
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("runtime except: " + ex);
      }

    return null;
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

  public ReturnVal setLabelField(short fieldID)
  {
    if (!store.loading && editor == null)
      {
	throw new IllegalArgumentException("can't call in a non-edit context");
      }

    if ((fieldID != -1) && (null == getField(fieldID)))
      {
	return Ganymede.createErrorDialog("Schema Editing Error",
					  "setLabelField() called with an unrecognized field id.");
      }

    label_id = fieldID;

    return null;
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
   * <p>This method is only valid when the Base reference is obtained
   * from a {@link arlut.csd.ganymede.SchemaEdit SchemaEdit} reference
   * by the Ganymede schema editor.</p>
   *
   * @see arlut.csd.ganymede.Base
   */
  
  public synchronized BaseField createNewField()
  {
    short id;
    DBObjectBaseField field;

    /* -- */

    if (!store.loading && editor == null)
      {
	throw new IllegalArgumentException("can't call in a non-edit context");
      }

    id = getNextFieldID();

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

    // give it an initial, unique name

    String newName = "New Field";

    int i = 2;

    while (getField(newName) != null)
      {
	newName = "New Field " + i++;
      }

    field.setName(newName);

    // default it to boolean, until such time as a schema editor
    // changes it

    field.setType(FieldType.BOOLEAN);

    // and set it up in our field hash and add this to the sorted
    // fields vector

    addFieldToEnd(field);

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

  public synchronized ReturnVal deleteField(String fieldName)
  {
    DBObjectBaseField field = null;
    short id = -1;

    /* -- */

    if (!store.loading && editor == null)
      {
	throw new IllegalArgumentException("can't call in a non-edit context");
      }

    if (fieldInUse(fieldName))
      {
	return Ganymede.createErrorDialog("Schema Editing Error",
					  "deleteField() called on object type " + getName() + 
					  " with a field name ( " + fieldName + "). that is in use in the database");
      }

    field = (DBObjectBaseField) getField(fieldName);

    if (field == null)
      {
	return Ganymede.createErrorDialog("Schema Editing Error",
					  "deleteField() called on object type " + getName() + 
					  " with an unrecognized field name ( " + fieldName + ").");
      }

    if (!field.isRemovable())
      {
	return Ganymede.createErrorDialog("Schema Editing Error",
					  "deleteField() called on object type " + getName() + 
					  " with a system field name ( " + fieldName + 
					  "). that may not be deleted");
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

    return null;
  }

  /**
   * <p>This method is used by the SchemaEditor to detect whether any
   * objects are using a field definition.</p>
   *
   * <p>Server-side only.</p>
   */

  public boolean fieldInUse(DBObjectBaseField bF)
  {
    Enumeration enum;

    /* -- */

    synchronized (objectTable)
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

    return false;
  }

  /**
   * <p>This method is used by the SchemaEditor to detect whether any
   * objects are using a field definition.</p>
   *
   * @see arlut.csd.ganymede.Base
   */

  public boolean fieldInUse(String fieldName)
  {
    Enumeration enum;
    short id;

    DBObjectBaseField fieldDef = (DBObjectBaseField) getField(fieldName);

    if (fieldDef == null)
      {
	throw new RuntimeException("can't check for non-existent field: " + fieldName);
      }

    id = fieldDef.getID();

    synchronized (objectTable)
      {
	enum = objectTable.elements();
	    
	while (enum.hasMoreElements())
	  {
	    DBObject obj = (DBObject) enum.nextElement();
	    
	    if (obj.getField(id) != null)
	      {
		return true;
	      }
	  }
      }

    return false;
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
   * <p>Get the next available field id for a new custom field.</p>
   */

  synchronized short getNextFieldID()
  {
    short id = 256;		// below 256 reserved for future server-mandatory fields
    Enumeration enum;
    DBObjectBaseField fieldDef;

    /* -- */

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
   * <p>This server-side routine provides a convenient accessor to retrieve
   * a specific object stored in this DBObjectBase.</p>
   */

  public final DBObject getObject(Invid invid)
  {
    if (invid.getType() != this.getTypeID())
      {
	throw new IllegalArgumentException("wrong invid type");
      }

    return objectTable.get(invid.getNum());
  }

  /**
   * <p>This server-side routine provides a convenient accessor to retrieve
   * a specific object stored in this DBObjectBase.</p>
   */

  public final DBObject getObject(int objectID)
  {
    return objectTable.get(objectID);
  }

  /**
   * <p>Clear the editing flag.  This disables the DBObjectBase set
   * methods on this ObjectBase and all dependent field definitions.
   * This method also updates the FieldTemplate for each field in this
   * object base.</p>
   */
  
  synchronized void clearEditor()
  {
    Enumeration enum;
    DBObjectBaseField fieldDef;

    /* -- */

    if (DBSchemaEdit.debug)
      {
	Ganymede.debug("DBObjectBase.clearEditor(): clearing editor for " + getName());
      }

    if (this.editor == null)
      {
	throw new IllegalArgumentException("not editing");
      }
    
    this.editor = null;
    this.templateVector = null;

    // we need to make sure any objectHook for this class knows that
    // we are now its objectBase and not the pre-edit DBObjectBase.

    this.reloadCustomClass();

    // all objects stored in this object base need to be updated
    // to point to the edited object base

    this.updateBaseRefs();

    synchronized (fieldTable)
      {
	enum = fieldTable.elements();
	
	while (enum.hasMoreElements())
	  {
	    fieldDef = (DBObjectBaseField) enum.nextElement();
	    fieldDef.editor = null;
	    fieldDef.template = new FieldTemplate(fieldDef);
	  }
      }

    if (debug2)
      {
	if (customFields == null)
	  {
	    System.err.println("DBObjectBase.clearEditor(): customFields (" + this.toString() + "== null!!!");
	  }
	else
	  {
	    for (int i = 0; i <customFields.size(); i++)
	      {
		System.err.println("DBObjectBase.clearEditor(): customFields[" + i + "(" + 
				   this.toString() + ")] = " + customFields.elementAt(i));
	      }
	  }
      }
  }

  /**
   * <p>This method is used by the DBEditSet commit logic to
   * replace this DBObjectBase's iterationSet with a new
   * Vector with the current objectTable's values.  This
   * method should only be called within the context
   * of a DBWriteLock being established on this DBObjectBase,
   * in the DBEditSet.commitTransaction() logic.</p>
   */

  void updateIterationSet()
  {
    Enumeration enum;
    Vector newIterationSet;

    /* -- */

    synchronized (objectTable)
      {
	newIterationSet = new Vector(objectTable.size());

	enum = objectTable.elements();

	while (enum.hasMoreElements())
	  {
	    newIterationSet.addElement(enum.nextElement());
	  }
      }

    // and we do the big swap

    this.iterationSet = newIterationSet;
  }

  /**
   * <P>This method returns a vector containing references to all
   * objects in this DBObjectBase at the time the vector reference
   * is accessed.  The vector returned *must not* be modified
   * by the caller, or else other threads iterating on that copy
   * of the vector will be disrupted.</P>
   */

  Vector getIterationSet()
  {
    return iterationSet;
  }

  /**
   * <p>This method is used to update base references in objects
   * after this base has replaced an old version via the
   * SchemaEditor.</p>
   */

  private void updateBaseRefs()
  {
    Enumeration enum;
    DBObject obj;

    /* -- */

    synchronized (objectTable)
      {
	enum = objectTable.elements();
	
	while (enum.hasMoreElements())
	  {
	    obj = (DBObject) enum.nextElement();
	   
	    if (DBSchemaEdit.debug)
	      {
		System.err.println("Updating base reference on " + obj);
	      }

	    obj.updateBaseRefs(this);
	  }
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
    lastChange = new Date();
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
    synchronized (store.lockSync)
      {
	return (!isReaderEmpty() || writeInProgress || !isDumpLockListEmpty());
      }
  }

  /**
   * <p>Add a DBWriteLock to this base's writer wait set.</p>
   */

  boolean addWriter(DBWriteLock writer)
  {
    synchronized (store.lockSync)
      {
	writerList.addElement(writer);
      }

    return true;
  }

  /**
   * <p>Remove a DBWriteLock from this base's writer wait set.</p>
   */

  boolean removeWriter(DBWriteLock writer)
  {
    boolean result;

    synchronized (store.lockSync)
      {
	result = writerList.removeElement(writer);
	store.lockSync.notifyAll();

	return result;
      }
  }

  /**
   * <p>Returns true if this base's writer wait set is empty.</p>
   */

  boolean isWriterEmpty()
  {
    return writerList.isEmpty();
  }

  /**
   * <p>Returns the size of the writer wait set</p>
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
    synchronized (store.lockSync)
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

    synchronized (store.lockSync)
      {
	result = readerList.removeElement(reader);

	store.lockSync.notifyAll();
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
   * <p>Add a DBDumpLock to this base's dumper waiting set.</p>
   */

  boolean addDumper(DBDumpLock dumper)
  {
    synchronized (store.lockSync)
      {
	dumperList.addElement(dumper);
      }

    return true;
  }

  /**
   * <p>Remove a DBDumpLock from this base's dumper waiting set.</p>
   */

  boolean removeDumper(DBDumpLock dumper)
  {
    boolean result;

    /* -- */

    synchronized (store.lockSync)
      {
	result = dumperList.removeElement(dumper);
	
	store.lockSync.notifyAll();
	return result;
      }
  }

  /**
   * <p>Returns true if this base's dumper wait set is empty.</p>
   */

  boolean isDumperEmpty()
  {
    return dumperList.isEmpty();
  }

  /**
   * <p>Returns the size of the dumper wait set</p>
   */

  int getDumperSize()
  {
    return dumperList.size();
  }

  /**
   * <p>Add a DBDumpLock to this base's dumper lock list.</p>
   */

  boolean addDumpLock(DBDumpLock dumper)
  {
    synchronized (store.lockSync)
      {
	dumpLockList.addElement(dumper);
      }

    return true;
  }

  /**
   * <p>Remove a DBDumpLock from this base's dumper lock list.</p>
   */

  boolean removeDumpLock(DBDumpLock dumper)
  {
    boolean result;

    synchronized (store.lockSync)
      {
	result = dumpLockList.removeElement(dumper);

	store.lockSync.notifyAll();
	return result;
      }
  }

  /**
   * <p>Returns true if this base's dumper lock list is empty.</p>
   */

  boolean isDumpLockListEmpty()
  {
    return dumpLockList.isEmpty();
  }

  /**
   * <p>Returns the size of the dumper lock list</p>
   */

  int getDumpLockListSize()
  {
    return dumpLockList.size();
  }

  /**
   * <p>Let's get our name here.</p>
   */

  public String toString()
  {
    return object_name;
  }

  /**
   * <p>Returns a vector of field definition templates, in display order.</p>
   *
   * @see arlut.csd.ganymede.FieldTemplate
   * @see arlut.csd.ganymede.Session
   */

  public synchronized Vector getFieldTemplateVector()
  {
    if (templateVector == null)
      {
	templateVector = new Vector();
	Enumeration enum;
	DBObjectBaseField fieldDef;

	// first load our system fields

	enum = fieldTable.elements();

	while (enum.hasMoreElements())
	  {
	    fieldDef = (DBObjectBaseField) enum.nextElement();

	    if (!fieldDef.isBuiltIn())
	      {
		continue;
	      }
	
	    templateVector.addElement(fieldDef.getTemplate());
	  }

	// then load our custom fields
    
	enum = customFields.elements();
	
	while (enum.hasMoreElements())
	  {
	    fieldDef = (DBObjectBaseField) enum.nextElement();
	
	    templateVector.addElement(fieldDef.getTemplate());
	  }
      }

    return templateVector;
  }

  /**
   * <p>This method is used to put a new user field into both the hashed field
   * table and the customFields vector.</p>
   */

  synchronized void addFieldToStart(DBObjectBaseField field)
  {
    if (field.getID() <= SchemaConstants.FinalSystemField)
      {
	throw new IllegalArgumentException("Error, attempted to add a system field using addFieldToStart().");
      }

    fieldTable.put(field);

    customFields.insertElementAt(field,0);
  }

  /**
   * <p>This method is used to put a new user field into both the hashed field
   * table and the customFields vector.</p>
   */

  synchronized void addFieldToEnd(DBObjectBaseField field)
  {
    if (field.getID() <= SchemaConstants.FinalSystemField)
      {
	throw new IllegalArgumentException("Error, attempted to add a system field using addFieldToEnd().");
      }

    fieldTable.put(field);

    customFields.addElement(field);
  }

  /**
   * <p>This method is used to instantiate a mandatory system field in this object.</p>
   */

  synchronized DBObjectBaseField addSystemField(String name, short id, short type)
  {
    DBObjectBaseField bf;

    try
      {
	bf = new DBObjectBaseField(this);
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException(ex.getMessage());
      }

    // we use direct assignment for these fields to avoid schema
    // editing checks

    bf.field_name = name;
    bf.field_code = id;
    bf.field_type = type;

    addSystemField(bf);

    return bf;
  }

  /**
   * <p>This method is used to store a system field.</p>
   */

  synchronized void addSystemField(DBObjectBaseField field)
  {
    if (field.getID() > SchemaConstants.FinalSystemField)
      {
	throw new IllegalArgumentException("Error, attempted to add a non-system field using addSystemField().");
      }

    fieldTable.put(field);
  }

  /**
   * <p>This method is used to remove a field from this base's
   * field database.</p>
   *
   * <p>This method is not intended for access from outside this
   * class.. use deleteField() for that.</p>
   */

  private synchronized void removeField(DBObjectBaseField field)
  {
    fieldTable.remove(field.getID());

    if (field.getID() > SchemaConstants.FinalSystemField)
      {
	customFields.removeElement(field);
      }
  }
}
