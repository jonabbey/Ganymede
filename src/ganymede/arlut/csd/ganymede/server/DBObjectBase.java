/*
   GASH 2

   DBObjectBase.java

   The GANYMEDE object storage system.

   Created: 2 July 1996
   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996-2004
   The University of Texas at Austin

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

package arlut.csd.ganymede.server;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.python.core.PyInteger;

import arlut.csd.JDialog.JDialogBuff;
import arlut.csd.Util.JythonMap;
import arlut.csd.Util.StringUtils;
import arlut.csd.Util.TranslationService;
import arlut.csd.Util.VecQuickSort;
import arlut.csd.Util.VectorUtils;
import arlut.csd.Util.XMLItem;
import arlut.csd.Util.XMLUtils;
import arlut.csd.Util.booleanSemaphore;
import arlut.csd.ganymede.common.BaseListTransport;
import arlut.csd.ganymede.common.CategoryTransport;
import arlut.csd.ganymede.common.FieldTemplate;
import arlut.csd.ganymede.common.FieldType;
import arlut.csd.ganymede.common.Invid;
import arlut.csd.ganymede.common.ReturnVal;
import arlut.csd.ganymede.common.SchemaConstants;
import arlut.csd.ganymede.rmi.Base;
import arlut.csd.ganymede.rmi.BaseField;
import arlut.csd.ganymede.rmi.Category;
import arlut.csd.ganymede.rmi.CategoryNode;
import arlut.csd.ganymede.rmi.Session;

/*------------------------------------------------------------------------------
                                                                           class
                                                                    DBObjectBase

------------------------------------------------------------------------------*/

/**
 * <p>The data dictionary and object store for a particular kind of
 * object in the {@link arlut.csd.ganymede.server.DBStore DBStore} on the
 * Ganymede server.</p>
 *
 * <p>Each DBObjectBase object includes a set of
 * {@link arlut.csd.ganymede.server.DBObjectBaseField DBObjectBaseField} objects, which
 * define the types and constraints on fields that may be present in objects
 * of this type.  These field definitions are held in an
 * {@link arlut.csd.ganymede.server.DBBaseFieldTable DBBaseFieldTable}.</p>
 *
 * <p>The actual {@link arlut.csd.ganymede.server.DBObject DBObject}'s themselves are
 * contained in an optimized {@link arlut.csd.ganymede.server.DBObjectTable DBObjectTable}
 * contained within this DBObjectBase.</p>
 *
 * <p>In addition to holding name, type id, and category information for a
 * given object type, the DBObjectBase class may also contain a string classname
 * for a Java class to be dynamically loaded to manage the server's interactions
 * with objects of this type.  Such a class name must refer to a subclass of the
 * {@link arlut.csd.ganymede.server.DBEditObject DBEditObject} class.  If such a custom
 * class is defined for this object type, DBObjectBase will contain an
 * {@link arlut.csd.ganymede.server.DBObjectBase#objectHook objectHook} DBEditObject
 * instance whose methods will be consulted to customize a lot of the server's
 * functioning.</p>
 *
 * <p>DBObjectBase also keeps track of {@link arlut.csd.ganymede.server.DBReadLock DBReadLocks},
 * {@link arlut.csd.ganymede.server.DBWriteLock DBWriteLocks}, and 
 * {@link arlut.csd.ganymede.server.DBDumpLock DBDumpLocks}, to manage 
 * changes to be made to objects contained in this DBObjectBase.</p>
 *
 * <p>DBObjectBase implements the {@link arlut.csd.ganymede.rmi.Base Base} RMI remote 
 * interface, which is used by the client to determine type information for objects
 * of this type, as well as by the schema editor when the schema is being edited.</p>
 */

public class DBObjectBase implements Base, CategoryNode, JythonMap {

  static boolean debug = true;

  /**
   * <p>TranslationService object for handling string localization in
   * the Ganymede server.</p>
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.server.DBObjectBase");

  /**
   * <P>More debugging.</P>
   */

  final static boolean debug2 = false;
  final static boolean xmldebug = false;

  static Hashtable upgradeClassMap = null;

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

  /**
   * <p>If we are processing an older ganymede.db file, our built-in
   * classes may be specified using the older packages.  This method
   * initializes a static map upgradeClassMap from the old class names
   * to the new ones, for use by the receive() method in this
   * class.</p>
   */

  private synchronized static void prepClassMap()
  {
    if (upgradeClassMap == null)
      {
	upgradeClassMap = new Hashtable();
	upgradeClassMap.put("arlut.csd.ganymede.eventCustom", "arlut.csd.ganymede.server.eventCustom");
	upgradeClassMap.put("arlut.csd.ganymede.objectEventCustom", "arlut.csd.ganymede.server.objectEventCustom");
	upgradeClassMap.put("arlut.csd.ganymede.objectEventCustom", "arlut.csd.ganymede.server.objectEventCustom");
	upgradeClassMap.put("arlut.csd.ganymede.ownerCustom", "arlut.csd.ganymede.server.ownerCustom");
	upgradeClassMap.put("arlut.csd.ganymede.adminPersonaCustom", "arlut.csd.ganymede.server.adminPersonaCustom");
	upgradeClassMap.put("arlut.csd.ganymede.permCustom", "arlut.csd.ganymede.server.permCustom");
	upgradeClassMap.put("arlut.csd.ganymede.taskCustom", "arlut.csd.ganymede.server.taskCustom");
      }
  }

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
   * used as the {@link arlut.csd.ganymede.common.Invid#type type} code
   * in {@link arlut.csd.ganymede.common.Invid Invid}s pointing to objects
   * of this type.</P>
   */

  short type_code;

  /**
   * <P>Fully qualified package and class name for a custom 
   * {@link arlut.csd.ganymede.server.DBEditObject DBEditObject} subclass
   * to be dynamically loaded to manage operations on this DBObjectBase.</P>
   *
   * <p>Note that this needs not to be private because {@link
   * arlut.csd.ganymede.server.DBStore#initializeSchema()} uses it to
   * initialize class definitions when bootstrapping.  If we ever
   * revise the initializeSchema code (tricky, given that
   * setClassInfo() automatically sets the classdef and creates the
   * objectHook, which we aren't necessarily ready to do during
   * initializeSchema until the very end) , we can think about making
   * this private.</p>
   */

  String classname;

  /**
   * <P>Class definition for a
   * {@link arlut.csd.ganymede.server.DBEditObject DBEditObject} subclass
   * dynamically loaded to manage operations on this DBObjectBase.</P>
   */

  private Class classdef;

  /**
   * <P>Option string to be available to custom classes.  The purpose of
   * this field is to allow the use of Jython custom classes, and to be
   * able to define a single Jython class which can consult this string
   * to find the Jython program text for this object base.</p>
   */

  String classOptionString;

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
   * edit-in-place {@link arlut.csd.ganymede.server.InvidDBField
   * InvidDBField}.</P>
   */

  private boolean embedded;

  // runtime data

  /**
   * Custom field dictionary sorted in display order.  This Vector
   * does *not* include any built-in fields.  Elements of this Vector
   * are {@link arlut.csd.ganymede.server.DBObjectBaseField
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
   * {@link arlut.csd.ganymede.server.GanymedeBuilderTask GanymedeBuilderTasks} 
   * to determine whether a particular build sequence is necessary.</P>
   */

  Date lastChange;

  /**
   * <P>If this DBObjectBase is locked with an exclusive lock
   * (a {@link arlut.csd.ganymede.server.DBWriteLock DBWriteLock}),
   * this field will point to it.</P>
   *
   * <P>This field is not currently used for anything in particular
   * in the lock logic, it is here strictly for informational/debugging
   * purposes.</P>
   */

  private DBLock currentLock;

  /**
   * <P>Set of {@link arlut.csd.ganymede.server.DBWriteLock DBWriteLock}s
   * pending on this DBObjectBase.  DBWriteLocks will add themselves
   * to the writerList upon entering establish().  If writerList is
   * not empty, no new {@link arlut.csd.ganymede.server.DBReadLock
   * DBReadLock}s will be allowed to add to add themselve to the readerList
   * in this DBObjectBase.  {@link arlut.csd.ganymede.server.DBDumpLock DBDumpLock}s
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
   * <P>Collection of {@link arlut.csd.ganymede.server.DBReadLock DBReadLock}s
   * that are locked on this DBObjectBase.</P>
   */

  private Vector readerList;

  /**
   * <P>Set of {@link arlut.csd.ganymede.server.DBDumpLock DBDumpLock}s
   * pending on this DBObjectBase.  DBDumpLocks will add themselves to
   * the dumperList upon entering establish().  If dumperList is not
   * empty, no new {@link arlut.csd.ganymede.server.DBWriteLock DBWriteLock}s
   * will be allowed to add themselves to the {@link
   * arlut.csd.ganymede.server.DBObjectBase#writerList writerList} in this
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
   * <P>Collection of {@link arlut.csd.ganymede.server.DBDumpLock DBDumpLock}s
   * that are locked on this DBObjectBase.</P>
   */

  private Vector dumpLockList;

  /**
   * <P>Boolean semaphore monitoring whether or not this DBObjectBase
   * is currently locked for writing.  We use a booleanSemaphore here
   * rather than a simple boolean so that we can force a memory barrier
   * for access on multiple CPU systems.</P>
   */

  private booleanSemaphore writeInProgress = new booleanSemaphore(false);

  /**
   * Used to keep track of schema editing
   */

  DBSchemaEdit editor;

  /**
   * <P>This Vector holds the current collection of {@link
   * arlut.csd.ganymede.server.DBObject DBObject} objects in this
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
   * {@link arlut.csd.ganymede.server.DBEditObject DBEditObject} subclass
   * to respond to a number of 'pseudostatic' method calls which customize
   * the Ganymede server's behavior when dealing with objects of this DBObjectBase's
   * type.  The DBObjectBase 
   * {@link arlut.csd.ganymede.server.DBObjectBase#createHook() createHook()} method
   * is responsible for loading the custom DBEditObject subclass
   * ({@link arlut.csd.ganymede.server.DBObjectBase#classdef classdef}) from
   * the {@link arlut.csd.ganymede.server.DBObjectBase#classname classname}
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

  private DBEditObject objectHook;	

  /* -- */

  /**
   * <P>Generic constructor.</P>
   *
   * @param store The DBStore database this DBObjectBase is being created for.
   * @param embedded If true, objects of this DBObjectBase type will not
   * be top-level objects, but rather will be embedded using edit-in-place
   * {@link arlut.csd.ganymede.server.InvidDBField InvidDBFields}.
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
   * {@link arlut.csd.ganymede.server.InvidDBField InvidDBFields}.
   * @param createFields If true, the standard fields required by the server
   * for its own operations will be created as part of DBObjectBase creation.  This
   * should be false if this DBObjectBase is being created in the process of loading
   * data from a pre-existing database which will presumably already have all
   * essential fields defined.
   */

  public DBObjectBase(DBStore store, boolean embedded, boolean createFields) throws RemoteException
  {
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

    Ganymede.rmi.publishObject(this);
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
	classOptionString = original.classOptionString;
	classdef = original.classdef;
	type_code = original.type_code;
	label_id = original.label_id;
	category = original.category;
	embedded = original.embedded;
    
	// make copies of all the custom field definitions for this
	// object type, and save them into our own field hash.
    
	Enumeration en;
	DBObjectBaseField field;
    
	en = original.customFields.elements();

	while (en.hasMoreElements())
	  {
	    field = (DBObjectBaseField) en.nextElement();
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

    if (classOptionString == null) // added at file version 2.6
      {
	out.writeUTF("");
      }
    else
      {
	out.writeUTF(classOptionString);
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
	System.err.println(ts.l("receive.enter"));
      }

    setName(in.readUTF());	// we use setName to filter out any bad chars in transition to 1.0

    if (debug)
      {
	System.err.println(ts.l("receive.basename", object_name));
      }

    classname = in.readUTF();

    // if we're reading a ganymede.db file from before the DBStore 2.7
    // rework, let's fix up our class names

    if (store.isLessThan(2,7))
      {
	prepClassMap();
	
	if (upgradeClassMap.containsKey(classname))
	  {
	    String newclassname = (String) upgradeClassMap.get(classname);

	    if (debug)
	      {
		// "DBObjectBase.receive(): Rewriting old system class name: {0} as {1}"
		System.err.println(ts.l("receive.rewritingClassname", classname, newclassname));
	      }

	    classname = newclassname;
	  }
      }

    if (debug)
      {
	System.err.println(ts.l("receive.classname", classname));
      }

    if (store.isAtLeast(2,6))
      {
	classOptionString = in.readUTF();
      }
    else
      {
	classOptionString = null;
      }

    type_code = in.readShort();	// read our index for the DBStore's objectbase hash

    // how many field definitions?

    size = in.readShort();

    if (debug)
      {
	System.err.println(ts.l("receive.fieldcount", new Integer(size)));
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

	if (type_code == SchemaConstants.OwnerBase && field.getID() == SchemaConstants.OwnerObjectsOwned)
	  {
	    continue;		// as of DBStore 2.7, we no longer symmetrically link owner groups to owned objects
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
	System.err.println(ts.l("receive.label", new Integer(label_id)));
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
    	System.err.println(ts.l("receive.reading", new Integer(object_count)));
      }

    Vector tmpIterationSet = new Vector(object_count);

    temp_val = (object_count > 0) ? (object_count * 2 + 1) : 4000;

    objectTable = new DBObjectTable(temp_val, (float) 1.0);

    for (int i = 0; i < object_count; i++)
      {
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
	System.err.println(ts.l("receive.maxid", object_name, new Integer(maxid)));
      }
  }

  /**
   * This method is used to add a DBObjectBase's information to this
   * {@link arlut.csd.ganymede.common.CategoryTransport
   * CategoryTransport} object for serialization to the client.
   */

  public void addBaseToTransport(CategoryTransport transport, GanymedeSession session)
  {
    transport.addChunk("base");
    transport.addChunk(this.getName());
    transport.addChunk(this.getPath());
    transport.addChunk(String.valueOf(this.getTypeID()));
    transport.addChunk(String.valueOf(this.getLabelField()));
    transport.addChunk(this.getLabelFieldName());
    transport.addChunk(String.valueOf(this.canInactivate()));

    if (session != null)
      {
	transport.addChunk(String.valueOf(this.canCreate(session)));
      }
    else
      {
	transport.addChunk(String.valueOf(true));
      }

    transport.addChunk(String.valueOf(this.isEmbedded()));
  }

  /**
   * This method is used to add a DBObjectBase's information to this
   * {@link arlut.csd.ganymede.common.BaseListTransport
   * BaseListTransport} object for serialization to the client.
   */

  public void addBaseToTransport(BaseListTransport transport, GanymedeSession session)
  {
    transport.addChunk("base");
    transport.addChunk(this.getName());
    transport.addChunk(this.getPath());
    transport.addChunk(String.valueOf(this.getTypeID()));
    transport.addChunk(String.valueOf(this.getLabelField()));
    transport.addChunk(this.getLabelFieldName());
    transport.addChunk(String.valueOf(this.canInactivate()));

    if (session != null)
      {
	transport.addChunk(String.valueOf(this.canCreate(session)));
      }
    else
      {
	transport.addChunk(String.valueOf(true));
      }

    transport.addChunk(String.valueOf(this.isEmbedded()));
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
	bf.targetField = -1;
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

	if (classOptionString != null)
	  {
	    xmlOut.attribute("optionString", classOptionString);
	  }

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
    String _classOptionStr = null;
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
	// "DBObjectBase.setXML(): root element != open objectdef: {0}"
	throw new IllegalArgumentException(ts.l("setXML.baddoc", root));
      }

    if (xmldebug)
      {
	// "Setting XML for object Base..{0}"
	err.println(ts.l("setXML.debugroot", root));
      }

    // GanymedeXMLSession.processSchema does a handleBaseRenaming up
    // front, but if we are a newly created base we might not have had
    // our name set yet.. go ahead and try to do it here
    
    _objectName = XMLUtils.XMLDecode(root.getAttrStr("name"));

    if (_objectName == null || _objectName.equals(""))
      {
	return Ganymede.createErrorDialog("xml",
					  // "DBObjectBase.setXML(): objectdef missing name attribute:\n {0}"
					  ts.l("setXML.missingname", root.getTreeString()));
      }

    // we call setName() at the bottom, after we know for sure what our
    // embedded status is going to be

    _idInt = root.getAttrInt("id");

    if (_idInt == null)
      {
	return Ganymede.createErrorDialog("xml",
					  // "DBObjectBase.setXML(): objectdef missing id attribute:\n {0}"
					  ts.l("setXML.missingid", root.getTreeString()));
      }

    if (xmldebug)
      {
	// "Setting id"
	err.println(ts.l("setXML.debugid"));
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
	// "Scanning fields"
	err.println(ts.l("setXML.debugscanning"));
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
						  // "Field definition missing name and/or id: {0}"
						  ts.l("setXML.noid", item.getTreeString()));
	      }

	    if (nameTable.containsKey(_fieldNameStr))
	      {
		return Ganymede.createErrorDialog("xml",
						  // "More than one field in objectdef: {0}\ncontains field name {1}"
						  ts.l("setXML.dupfieldname", root.getTreeString(), _fieldNameStr));
	      }

	    DBObjectBaseField _field = (DBObjectBaseField) getField(_fieldNameStr);

	    if (_field != null && _field.isBuiltIn())
	      {
		return Ganymede.createErrorDialog("xml",
						  // "Can't set a field:\n{0}\nwith the same name as a pre-existing built-in field in objectdef:\n{1}"
						  ts.l("setXML.sysfield", item.getTreeString(), root.getTreeString()));
	      }

	    nameTable.put(_fieldNameStr, item);

	    if (_fieldIDInt.shortValue() < 100)
	      {
		return Ganymede.createErrorDialog("xml",
						  // "Can't modify or set a field:\n{0}\nwith a field id in the global field range:\n{1}"
						  ts.l("setXML.noglobals", item.getTreeString(), root.getTreeString()));
	      }

	    if (idTable.containsKey(_fieldIDInt))
	      {
		return Ganymede.createErrorDialog("xml",
						  // "More than one field in objectdef: {0}\ncontains field id {1}"
						  ts.l("setXML.dupfieldid", root.getTreeString(), _fieldIDInt));
	      }

	    idTable.put(_fieldIDInt, item);

	    fieldsInXML.addElement(_fieldIDInt);
	  }
      }

    if (xmldebug)
      {
	// "Calculating fields to delete"
	err.println(ts.l("setXML.debugdels"));
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

	// "\t\tDeleting field {0}"
	err.println(ts.l("setXML.deleting", _field.getName()));

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
						  // "Objectdef contains more than one classdef element:\n{0}"
						  ts.l("setXML.dupclassdef", root.getTreeString()));
	      }

	    _classStr = item.getAttrStr("name");

	    _classOptionStr = item.getAttrStr("optionString");

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
						  // "Objectdef contains more than one label element:\n{0}"
						  ts.l("setXML.duplabel", root.getTreeString()));
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
		    throw new RuntimeException("Publishing error " + ex.getMessage());
		  }

		if (xmldebug)
		  {
		    // "Setting XML on new field {0}"
		    err.println(ts.l("setXML.debugnew", item));
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
		// "\t\tEditing field {0}"
		err.println(ts.l("setXML.editing", item.getAttrStr("name")));
		
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
					      // "Unrecognized XML item: {0} in objectdef:\n{1}"
					      ts.l("setXML.unrecognized", item, root.getTreeString()));
	  }
      }

    // and set or clear the label and class options

    if (xmldebug)
      {
	// "Setting label field"
	err.println(ts.l("setXML.debuglabel"));
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
	// "Setting class name"
	err.println(ts.l("setXML.debugclass"));
      }
    
    retVal = setClassInfo(_classStr, _classOptionStr);
    
    if (retVal != null && !retVal.didSucceed())
      {
	return retVal;
      }

    // we have to set embedded before calling setName() so that
    // setName() can enforce the embedded object naming convention

    embedded = _embedded;	// XXX need to work on this

    if (xmldebug)
      {
	// "Setting object name"
	err.println(ts.l("setXML.debugname"));
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
	// "Sorting fields"
	err.println(ts.l("setXML.debugsorting"));
      }

    Vector _newCustom = new Vector();

    for (int i = 0; i < fieldsInXML.size(); i++)
      {
	Integer _fieldID = (Integer) fieldsInXML.elementAt(i);

	DBObjectBaseField _field = (DBObjectBaseField) getField(_fieldID.shortValue());

	if (_field == null)
	  {
	    return Ganymede.createErrorDialog("xml",
					      // "Couldn't find field {0} while resorting customFields."
					      ts.l("setXML.mysteryfield", _fieldID));
	  }

	_newCustom.addElement(_field);
      }

    // make sure we are consistent like we think we should be

    Vector _intersection = VectorUtils.intersection(customFields, _newCustom);

    if ((_intersection.size() != customFields.size()) ||
	(_intersection.size() != _newCustom.size()))
      {
	// "Consistency error while resorting customFields in base {0}"
	err.println(ts.l("setXML.inconsistent", getName()));

	err.println("customFields.size() = " + customFields.size());
	err.println("_newCustom.size() = " + _newCustom.size());
	err.println("_intersection.size() = " + _intersection.size());

	return Ganymede.createErrorDialog("xml",
					  // "Consistency error while resorting customFields."
					  ts.l("setXML.consistencyerror"));
      }
    
    customFields = _newCustom;

    // and we're done

    if (xmldebug)
      {
	// "Done processing object base {0}"
	err.println(ts.l("setXML.debugdone", root));
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
   * @see arlut.csd.ganymede.rmi.Base
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
   * <p>
   * This method will attempt to invoke an apppropriate <i>factory</i> method
   * on this object base's specified custom class. This adds a layer of 
   * indirection to Ganymede's custom class loading behaviour. Instead of trying
   * to invoke a constructor directly on the custom class, we instead look for
   * a method called "factory" in the custom class that takes the same
   * parameters.
   * </p>
   * 
   * <p>
   * This allows programmers to define a custom class that can do fancy tricks
   * before constructing a new {@link arlut.csd.ganymede.server DBEditObject
   * DBEditObject} like, say, loading the class from a Jython interpreter, or
   * doing code-generation from an external class descriptor file.
   * </p>
   * 
   * @param classParams the list of parameter types
   * @param methodParams the parameters to pass to the factory method
   * @return a new {@link arlut.csd.ganymede.server.DBEditObject DBEditObject}
   * @throws SecurityException
   * @throws InvocationTargetException
   * @throws IllegalAccessException
   */
  
  protected DBEditObject invokeFactory(Class[] classParams,
      Object[] methodParams) throws SecurityException,
      InvocationTargetException, IllegalAccessException
  {
    /*
     * Deepak sez: This is sort of a hack. Sun, in its wisdom, decided to make
     * Class.searchMethods() private. So now if I want to see if a class has a
     * particular method, I have to do a getMethods() and iterate through the
     * results. Instead, I've decided to cheat and use the fact that when you
     * call Class.getMethod(), a NoSuchMethodException is thrown if that method
     * isn't defined.
     */
    try
      {
        Method factory = classdef.getMethod("factory", classParams);
        return (DBEditObject) factory.invoke(null, methodParams);
      }
    catch (NoSuchMethodException ex)
      {
        /*
         * Don't panic...if there's no factory method defined, we'll just return
         * null in the end.
         */
      }
    return null;
  }

  /** 
   * <p>This method is used to create a DBEditObject subclass handle
   * ({@link arlut.csd.ganymede.server.DBObjectBase#objectHook objectHook}),
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

		// "DBObjectBase.createHook(): class definition could not be found: {0}"
		System.err.println(ts.l("createHook.noclass", ex));
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
        /* First, try to use classdef's factory method to create the object.
         * If that doesn't work (if the classdef has no factory methods, for
         * example), then use a default constructor. */

	e_object = invokeFactory(cParams, params);

	if (e_object == null)
	  {
	    c = classdef.getDeclaredConstructor(cParams); // DBObjectBase constructor
	    e_object = (DBEditObject) c.newInstance(params);
	  }
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
    finally
      {
	if (e_object == null)
	  {
	    e_object = new DBEditObject(this);
	  }
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
	// "null editset in createNewObject"
	throw new NullPointerException(ts.l("createNewObject.noeditset"));
      }

    if (chosenSlot == null)
      {
	invid = Invid.createInvid(getTypeID(), getNextID());

	if (debug2)
	  {
	    if (objectTable.containsKey(chosenSlot.getNum()))
	      {
		// "bad invid chosen in createNewObject: num already taken"
		throw new IllegalArgumentException(ts.l("createNewObject.badinvid"));
	      }
	  }
      }
    else
      {
	if (chosenSlot.getType() != type_code)
	  {
	    // "bad chosen_slot passed into createNewObject: bad type"
	    throw new IllegalArgumentException(ts.l("createNewObject.badslottype"));
	  }

	if (objectTable.containsKey(chosenSlot.getNum()))
	  {
	    // "bad chosen_slot passed into createNewObject: num already taken"
	    throw new IllegalArgumentException(ts.l("createNewObject.badslotnum"));
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
	    /* First, try to use classdef's factory method to create the object. If
	     * that doesn't work (if the classdef has no factory methods, for
	     * example), then use a default constructor. */

            e_object = invokeFactory(classArray, parameterArray);
            if (e_object == null)
              {
                c = classdef.getDeclaredConstructor(classArray); 
                e_object = (DBEditObject) c.newInstance(parameterArray);
              } 
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
	finally
	  {
	    if (e_object == null)
	      {
		if (error_code != null)
		  {
		    // "createNewObject failure: {0} in trying to construct custom object"
		    String errormsg = ts.l("createNewObject.failure1", error_code);
		    Ganymede.debug(errormsg);
		    throw new GanymedeManagementException(errormsg);
		  }
		else
		  {
		    throw new GanymedeManagementException();
		  }
	      }
	  }
      }

    return e_object;
  }
  
  /**
  *
  * Check-out constructor, used by DBObject.createShadow()
  * to pull out an object for editing.
  *
  * @param original the object to create the shadow of
  * @param editset the transaction this object is to be created in
  */
  
  public DBEditObject createNewObject(DBObject original, DBEditSet editset)
  {
    // if we are a customized object type, dynamically invoke
    // the proper check-out constructor for the DBEditObject
    // subtype.

    if (classdef != null)
      {
        Constructor c;
        Class classArray[];
        Object parameterArray[];

        classArray = new Class[2];

        classArray[0] = original.getClass();
        classArray[1] = editset.getClass();

        parameterArray = new Object[2];

        parameterArray[0] = original;
        parameterArray[1] = editset;

        String error_code = null;
        DBEditObject shadowObject = null;

        try
          {
            /*
             * First, try to use classdef's factory method to create the object.
             * If that doesn't work (if the classdef has no factory methods, for
             * example), then use a default constructor.
             */

            shadowObject = invokeFactory(classArray, parameterArray);
            if (shadowObject == null)
              {
                c = classdef.getDeclaredConstructor(classArray);
                shadowObject = (DBEditObject) c.newInstance(parameterArray);
              }
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

            error_code = "Invocation Target Exception "
                + tex.getTargetException();
          }
	finally
	  {
	    if (shadowObject == null)
	      {
		if (error_code != null)
		  {
		    // "createNewObject failure: {0} in trying to check out custom object"
		    String errormsg = ts.l("createNewObject.failure2", error_code);
		    Ganymede.debug(errormsg);
		    throw new GanymedeManagementException(errormsg);
		  }
		else
		  {
		    throw new GanymedeManagementException();
		  }
	      }
	  }

	return shadowObject;
      }
    else
      {
        return new DBEditObject(original, editset);
      }
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
    Enumeration en;
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

    en = customFields.elements();

    while (en.hasMoreElements())
      {
	bf = (DBObjectBaseField) en.nextElement();

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
    Enumeration en;
    DBObjectBaseField fieldDef;

    /* -- */

    out.println(indent + object_name + "(" + type_code + ")");
    
    en = customFields.elements();

    while (en.hasMoreElements())
      {
	fieldDef = (DBObjectBaseField) en.nextElement();

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
   * @see arlut.csd.ganymede.rmi.Base
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
   * from a {@link arlut.csd.ganymede.rmi.SchemaEdit SchemaEdit} reference
   * by the Ganymede schema editor.</p>
   *
   * @see arlut.csd.ganymede.rmi.Base
   */

  public synchronized ReturnVal setName(String newName)
  {
    if (!store.loading && editor == null)
      {
	throw new IllegalArgumentException(ts.l("global.notediting"));
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
					      // "Can't rename base {0} to {1}, that name is already taken."
					      ts.l("setName.norename", object_name, newName));
	  }
      }
    else
      {
	if (this.store.getObjectBase(newName) != null)
	  {
	    return Ganymede.createErrorDialog("Schema Editing Error",
					      // "Can't rename base {0} to {1}, that name is already taken."
					      ts.l("setName.norename", object_name, newName));
	  }
      }

    // ok, go for it

    object_name = newName;

    return null;
  }

  /**
   * <p>Returns the name of the class managing this object type</p>
   *
   * @see arlut.csd.ganymede.rmi.Base
   */
  
  public String getClassName()
  {
    return classname;
  }

  /**
   * <p>Returns the option string for the class definition.. see {@link
   * arlut.csd.ganymede.DBObjectBase#classOptionString} for more
   * details.</p>
   *
   * @see arlut.csd.ganymede.Base
   */
  
  public String getClassOptionString()
  {
    return classOptionString;
  }

  /**
   * <p>This method is used to associate a management class with this
   * object base.</p>
   *
   * <p>The newClassName argument must be fully qualified, and must
   * refer to one of two kinds of classes.  The first is a {@link
   * arlut.csd.ganymede.server.DBEditObject DBEditObject} subclass that
   * implements the requisite three constructors, a la the traditional
   * Ganymede customization hook.  The second is any class
   * implementing the {@link arlut.csd.ganymede.common.DDPluginFactory
   * DDPluginFactory} interface, which provides a set of factory
   * methods which return DBEditObject instances.</p>
   *
   * <p>If newClassName implements DDPluginFactory, the
   * newOptionString argument will be available to the factory methods
   * so that the constructed objects can be dynamically customized.
   * This is intended to support the use of DBEditObject subclasses
   * written in Jython, with support for dynamic reloading during
   * server execution.</p>
   *
   * <p>This method is only valid when the Base reference is obtained
   * from a {@link arlut.csd.ganymede.rmi.SchemaEdit SchemaEdit} reference
   * by the Ganymede schema editor.</p>
   *
   * @see arlut.csd.ganymede.rmi.Base
   */

  public synchronized ReturnVal setClassInfo(String newClassName, String newOptionString)
  {
    /*
     * Remember the original values we've got, just in case the new values don't
     * take for whatever reason.
     */
    String originalClassName = classname;
    Class originalClassDef = classdef;
    DBEditObject originalObjectHook = objectHook;
    
    if (!store.loading && editor == null)
      {
	throw new IllegalArgumentException(ts.l("global.notediting"));
      }

    if ((newClassName == classname ||
	 (newClassName != null && newClassName.equals(classname))) &&
	(newOptionString == classOptionString ||
	 (newOptionString != null && newOptionString.equals(classOptionString))))
      {
	return null;		// no change
      }

    if (newClassName == null || newClassName.equals(""))
      {
	classname = "";
	classdef = null;
	objectHook = null;
	return null;
      }

    classname = newClassName;

    if (newOptionString != null && newOptionString.equals(""))
      {
	newOptionString = null;
      }

    classOptionString = newOptionString;
      	
    // Reset the classdef so that createHook can load the newly specified
    // class via class.forName()

    classdef = null;

    // try to create an object using the proposed class
    // information.. if we can't, no big deal, it'll just have to be
    // done after the server is restarted.

    try
      {
	objectHook = this.createHook();
      }
    catch (RemoteException ex)
      {
	// Restore our state back to the way it was originally
	
	classname = originalClassName;
	classdef = originalClassDef;
	objectHook = originalObjectHook;
		
	return Ganymede.createErrorDialog("setClassInfo Failure",
					  // "Internal RemoteException in setClassInfo: {0}"
					  ts.l("setClassInfo.internalError", Ganymede.stackTrace(ex)));
      }

    if (objectHook.getClass().getName().equals("arlut.csd.ganymede.server.DBEditObject") &&
	!classname.equals("arlut.csd.ganymede.server.DBEditObject"))
      {
	ReturnVal retVal = new ReturnVal(false);

	if (classOptionString == null)
	  {
	    retVal.setDialog(new JDialogBuff("Schema Editor Warning",
					     // "Couldn't find class {0} in the server's CLASSPATH.  This probably means
					     // that you have not yet rebuilt the custom.jar file with this class added."
					     ts.l("setClassInfo.noclass", classname),
					     "Ok",
					     null,
					     "error.gif"));
	  }
	else
	  {
	    retVal.setDialog(new JDialogBuff("Schema Editor Warning",
					     // "Couldn't load custom management logic from class {0} using class option string '\
					     // {1}'.\n\nThis may mean that you have not yet rebuilt the custom jar file with the {0} class added, or that \
					     // the resource specified in the option string can not be found by {0}'s factory methods."
					     ts.l("setClassInfo.noclassoption", classname, classOptionString),
					     "Ok",
					     null,
					     "error.gif"));
	  }

	// Restore our state back to the way it was originally
	
	classname = originalClassName;
	classdef = originalClassDef;
	objectHook = originalObjectHook;
	
	return retVal;
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
					  // "Error, can't move field {0}, no such field in object type."
					  ts.l("moveFieldAfter.nomove", fieldName));
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
					  // "Error, can't move field {0} after {1}, no such field in object type."
					  ts.l("moveFieldAfter.nofield", fieldName, previousFieldName));
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
					  // "Error, can't move field {0}, no such field in object type."
					  ts.l("moveFieldBefore.nomove", fieldName));
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
					  // "Error, can't move field {0} before {1}, no such field in object type."
					  ts.l("moveFieldBefore.nofield", fieldName, nextFieldName));
      }

    customFields.removeElement(oldField);
    customFields.insertElementAt(oldField, customFields.indexOf(nextField));

    return null;
  }

  /**
   * <p>Returns true if the current session is permitted to
   * create an object of this type.</p>
   *
   * @see arlut.csd.ganymede.rmi.Base
   */

  public boolean canCreate(Session session)
  {
    return getObjectHook().canCreate(session);
  }

  /**
   * <p>Returns true if this object type can be inactivated</p>
   *
   * @see arlut.csd.ganymede.rmi.Base
   */

  public synchronized boolean canInactivate()
  {
    return getObjectHook().canBeInactivated();
  }

  /**
   * <p>Returns the invid type id for this object definition</p>
   *
   * @see arlut.csd.ganymede.rmi.Base
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
	throw new IllegalArgumentException(ts.l("global.notediting"));
      }

    if ((objectId != type_code) && (type_code != -1))
      {
	return Ganymede.createErrorDialog("xml",
					  // "Can't change the type_code for an existing object base"
					  ts.l("setTypeID.notypemutation"));
      }

    if (store.getObjectBase(objectId) != null)
      {
	return Ganymede.createErrorDialog("xml",
					  // "Can't set the type_code for object base {0} to that of an existing object base"
					  ts.l("setTypeID.typeconflict", this.toString()));
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
   * @see arlut.csd.ganymede.rmi.Base
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
   * @see arlut.csd.ganymede.rmi.Base
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
   * <p>Returns all {@link arlut.csd.ganymede.server.DBObjectBaseField DBObjectBaseField}
   * base field definitions for objects of this type, in random order.</p>
   *
   * @see arlut.csd.ganymede.rmi.Base
   */

  public Vector getFields()
  {
    return getFields(true);
  }

  /**
   * <p>Returns {@link arlut.csd.ganymede.server.DBObjectBaseField DBObjectBaseField}
   * base field definitions for objects of this type.
   *
   * <P>If includeBuiltIns is false, the fields returned will be the
   * custom fields defined for this object type, and they will be
   * returned in display order.  If includeBuiltIns is true, the
   * built-in fields will be appended to the Vector after the custom
   * types, in random order.</P>
   *
   * @see arlut.csd.ganymede.rmi.Base 
   */

  public synchronized Vector getFields(boolean includeBuiltIns)
  {
    Vector result;
    Enumeration en;
    DBObjectBaseField field;

    /* -- */

    result = new Vector();

    // first we return the custom fields, in display order

    en = customFields.elements();
    
    while (en.hasMoreElements())
      {
	field = (DBObjectBaseField) en.nextElement();
	
	result.addElement(field);
      }

    // now if we are to return the built-in fields, go ahead and add
    // them in whatever hashing order we find them

    if (includeBuiltIns)
      {
	en = fieldTable.elements();

	while (en.hasMoreElements())
	  {
	    field = (DBObjectBaseField) en.nextElement();

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
   * @see arlut.csd.ganymede.rmi.BaseField
   * @see arlut.csd.ganymede.rmi.Base
   */

  public BaseField getField(short id)
  {
    return fieldTable.get(id);
  }

  /**
   * <p>Returns the field definition for the field matching name,
   * or null if no match found.</p>
   *
   * @see arlut.csd.ganymede.rmi.BaseField
   * @see arlut.csd.ganymede.rmi.Base
   */

  public synchronized BaseField getField(String name)
  {
    BaseField bf;
    Enumeration en;

    /* -- */

    en = fieldTable.elements();
    
    while (en.hasMoreElements())
      {
	bf = (BaseField) en.nextElement();

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
   * {@link arlut.csd.ganymede.server.DBEditObject#getLabelHook(arlut.csd.ganymede.server.DBObject) getLabelHook()}
   * method.</p>
   *
   * <p>This method is only valid when the Base reference is obtained
   * from a {@link arlut.csd.ganymede.rmi.SchemaEdit SchemaEdit} reference
   * by the Ganymede schema editor.</p>
   *
   * @see arlut.csd.ganymede.rmi.Base
   */

  public ReturnVal setLabelField(String fieldName)
  {
    BaseField bF;

    /* -- */

    if (!store.loading && editor == null)
      {
	throw new IllegalArgumentException(ts.l("global.notediting"));
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
					  // "setLabelField() called with an unrecognized field name."
					  ts.l("setLabelField.badfieldname"));
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
   * {@link arlut.csd.ganymede.server.DBEditObject#getLabelHook(arlut.csd.ganymede.server.DBObject) getLabelHook()}
   * method.</p>
   *
   * <p>This method is only valid when the Base reference is obtained
   * from a {@link arlut.csd.ganymede.rmi.SchemaEdit SchemaEdit} reference
   * by the Ganymede schema editor.</p>
   *
   * @see arlut.csd.ganymede.rmi.Base
   */

  public ReturnVal setLabelField(short fieldID)
  {
    if (!store.loading && editor == null)
      {
	throw new IllegalArgumentException(ts.l("global.notediting"));
      }

    if ((fieldID != -1) && (null == getField(fieldID)))
      {
	return Ganymede.createErrorDialog("Schema Editing Error",
					  // "setLabelField() called with an unrecognized field id."
					  ts.l("setLabelField.badfieldid"));
      }

    label_id = fieldID;

    return null;
  }

  /**
   * <p>Get the parent Category for this object type.  This is used by the
   * Ganymede client and schema editor to present object types in
   * a hierarchical tree.</p>
   *
   * @see arlut.csd.ganymede.rmi.Base
   * @see arlut.csd.ganymede.rmi.CategoryNode
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
   * from a {@link arlut.csd.ganymede.rmi.SchemaEdit SchemaEdit} reference
   * by the Ganymede schema editor.</p>
   *
   * @see arlut.csd.ganymede.rmi.CategoryNode
   */

  public void setCategory(Category category)
  {
    if (!store.loading && editor == null)
      {
	throw new IllegalArgumentException(ts.l("global.notediting"));
      }

    this.category = category;
  }

  /**
   * <p>Creates a new base field, inserts it into the DBObjectBase
   * field definitions hash, and returns a reference to it. </p>
   *
   * <p>This method is only valid when the Base reference is obtained
   * from a {@link arlut.csd.ganymede.rmi.SchemaEdit SchemaEdit} reference
   * by the Ganymede schema editor.</p>
   *
   * @see arlut.csd.ganymede.rmi.Base
   */
  
  public synchronized BaseField createNewField()
  {
    short id;
    DBObjectBaseField field;

    /* -- */

    if (!store.loading && editor == null)
      {
	throw new IllegalArgumentException(ts.l("global.notediting"));
      }

    id = getNextFieldID();

    try
      {
	field = new DBObjectBaseField(this, editor);
      }
    catch (RemoteException ex)
      {
	// "Couldn't create field due to initialization error: {0}"
	throw new RuntimeException(ts.l("createNewField.noluck", ex));
      }

    // set its id

    field.setID(id);

    // give it an initial, unique name

    String newName = ts.l("createNewField.defaultname");

    int i = 2;

    while (getField(newName) != null)
      {
	newName = ts.l("createNewField.defaultname") + i++;
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
   * @see arlut.csd.ganymede.rmi.Base
   */

  public synchronized ReturnVal deleteField(String fieldName)
  {
    DBObjectBaseField field = null;
    short id = -1;

    /* -- */

    if (!store.loading && editor == null)
      {
	throw new IllegalArgumentException(ts.l("global.notediting"));
      }

    if (fieldInUse(fieldName))
      {
	return Ganymede.createErrorDialog("Schema Editing Error",
					  // "deleteField() called on object type {0} with a field name ({1}) that is in use in the database."
					  ts.l("deleteField.fieldused", getName(), fieldName));
      }

    field = (DBObjectBaseField) getField(fieldName);

    if (field == null)
      {
	return Ganymede.createErrorDialog("Schema Editing Error",
					  // "deleteField() called on object type {0} with an unrecognized field name ({1})."
					  ts.l("deleteField.fieldunknown", getName(), fieldName));
      }

    if (!field.isRemovable())
      {
	return Ganymede.createErrorDialog("Schema Editing Error",
					  // "deleteField() called on object type {0} with a system field name ({1}) that may not be deleted."
					  ts.l("deleteField.sysfield", getName(), fieldName));
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
    Enumeration en;

    /* -- */

    synchronized (objectTable)
      {
	en = objectTable.elements();
	    
	while (en.hasMoreElements())
	  {
	    DBObject obj = (DBObject) en.nextElement();
	    
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
   * @see arlut.csd.ganymede.rmi.Base
   */

  public boolean fieldInUse(String fieldName)
  {
    Enumeration en;
    short id;

    DBObjectBaseField fieldDef = (DBObjectBaseField) getField(fieldName);

    if (fieldDef == null)
      {
	// "Can't check for non-existent field: {0}"
	throw new RuntimeException(ts.l("fieldInUse.nofield", fieldName));
      }

    id = fieldDef.getID();

    synchronized (objectTable)
      {
	en = objectTable.elements();
	    
	while (en.hasMoreElements())
	  {
	    DBObject obj = (DBObject) en.nextElement();
	    
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
	    // "Error, couldn't create hook in getObjectHook().\n{0}"
	    throw new RuntimeException(ts.l("getObjectHook.error", Ganymede.stackTrace(ex)));
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
    Enumeration en;
    DBObjectBaseField fieldDef;

    /* -- */

    en = fieldTable.elements();

    while (en.hasMoreElements())
      {
	fieldDef = (DBObjectBaseField) en.nextElement();

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
	// "Wrong Invid type"
	throw new IllegalArgumentException(ts.l("getObject.badtype"));
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
   * <p>This server-side routing provides a convenient accessor to retrieve
   * the entire member-object hashtable for this DBObjectBase.</p>
   * 
   * @return The internal object table
   */
   
  public final DBObjectTable getObjectTable()
  {
    return objectTable;
  }

  /**
   * <p>Clear the editing flag.  This disables the DBObjectBase set
   * methods on this ObjectBase and all dependent field definitions.
   * This method also updates the FieldTemplate for each field in this
   * object base.</p>
   */
  
  synchronized void clearEditor()
  {
    Enumeration en;
    DBObjectBaseField fieldDef;

    /* -- */

    if (DBSchemaEdit.debug)
      {
	// "DBObjectBase.clearEditor(): clearing editor for {0}"
	Ganymede.debug(ts.l("clearEditor.clearing", getName()));
      }

    if (this.editor == null)
      {
	throw new IllegalArgumentException(ts.l("global.notediting"));
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
	en = fieldTable.elements();
	
	while (en.hasMoreElements())
	  {
	    fieldDef = (DBObjectBaseField) en.nextElement();
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
    Enumeration en;
    Vector newIterationSet;

    /* -- */

    synchronized (objectTable)
      {
	newIterationSet = new Vector(objectTable.size());

	en = objectTable.elements();

	while (en.hasMoreElements())
	  {
	    newIterationSet.addElement(en.nextElement());
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
    Enumeration en;
    DBObject obj;

    /* -- */

    synchronized (objectTable)
      {
	en = objectTable.elements();
	
	while (en.hasMoreElements())
	  {
	    obj = (DBObject) en.nextElement();
	   
	    if (DBSchemaEdit.debug)
	      {
		// "Updating base reference on {0}"
		System.err.println(ts.l("updateBaseRefs.updating", obj));
	      }

	    obj.updateBaseRefs(this);
	  }
      }
  }

  /**
   * <P>This method is used to allow objects in this base to notify us when
   * their state changes.  It is called from the
   * {@link arlut.csd.ganymede.server.DBEditSet DBEditSet} commit() method.</P>
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
	return (!isReaderEmpty() || writeInProgress.isSet() || !isDumpLockListEmpty());
      }
  }

   /**
   * <p>Returns true if we have a writer lock locking us.</p>
   */

  boolean isWriteInProgress()
  {
    return writeInProgress.isSet();
  }

  /**
   * <p>Used by {@link arlut.csd.ganymede.server.DBWriteLock DBWriteLock} to establish or clear a lock.</p>
   */

  void setWriteInProgress(boolean state)
  {
    if (writeInProgress.set(state) == state)
      {
	// assert

	if (state)
	  {
	    // "double write lock in DBObjectBase"
	    Ganymede.logAssert(ts.l("setWriteInProgress.doublelock"));
	  }
	else
	  {
	    // "double write unlock in DBObjectBase"
	    Ganymede.logAssert(ts.l("setWriteInProgress.doubleunlock"));
	  }
      }
  }

  /**
   * <p>Used by {@link arlut.csd.ganymede.server.DBWriteLock DBWriteLock} to
   * set a possibly informative lock reference, so that a debugger can
   * show a reference to the DBWriteLock locking us down.</p>
   */

  void setCurrentLock(DBWriteLock lock)
  {
    this.currentLock = lock;
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
   * <p>Returns a vector of field definition templates, in display order.</p>
   *
   * @see arlut.csd.ganymede.common.FieldTemplate
   * @see arlut.csd.ganymede.rmi.Session
   */

  public synchronized Vector getFieldTemplateVector()
  {
    if (templateVector == null)
      {
	templateVector = new Vector();
	Enumeration en;
	DBObjectBaseField fieldDef;

	// first load our system fields

	en = fieldTable.elements();

	while (en.hasMoreElements())
	  {
	    fieldDef = (DBObjectBaseField) en.nextElement();

	    if (!fieldDef.isBuiltIn())
	      {
		continue;
	      }
	
	    templateVector.addElement(fieldDef.getTemplate());
	  }

	// then load our custom fields
    
	en = customFields.elements();
	
	while (en.hasMoreElements())
	  {
	    fieldDef = (DBObjectBaseField) en.nextElement();
	
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
	// "Error, attempted to add a system field using addFieldToStart()."
	throw new IllegalArgumentException(ts.l("addFieldToStart.sysfield"));
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
	// "Error, attempted to add a system field using addFieldToEnd()."
	throw new IllegalArgumentException(ts.l("addFieldToEnd.sysfield"));
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
	// "Error, attempted to add a non-system field using addSystemField()."
	throw new IllegalArgumentException(ts.l("addSystemField.nonsysfield"));
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

  /* *************************************************************************
   *
   * The following methods are for Jython/Map support
   *    
   * For this object, the Map interface allows for indexing based on either
   * the name or the numeric ID of a DBObject. Indexing by numeric id, however,
   * is only supported for "direct" access to the Map; the numeric id numbers
   * won't appear in the list of keys for the Map.
   *
   * EXAMPLE:
   * MyDBObjectBaseVariable.get("foo") will return the DBObject with the label
   * of "foo".
   */

  
  public boolean has_key(Object key)
  {
    return keys().contains(key);
  }

  public List items()
  {
    List list = new ArrayList();
    DBObject obj;
    Object[] tuple;
    
    for (Iterator iter = getIterationSet().iterator(); iter.hasNext();)
      {
        obj = (DBObject) iter.next();
        tuple = new Object[2];
        tuple[0] = obj.getLabel();
        tuple[1] = obj;
        list.add(tuple);
      }
    
    return list;    
  }

  public Set keys()
  {
    DBObject obj;
    Set keys = new HashSet(objectTable.size());
    
    for(Iterator iter = getIterationSet().iterator(); iter.hasNext();)
      {
        obj = (DBObject) iter.next();
        keys.add(obj.getLabel());
      }
    
    return keys;
  }

  public boolean containsKey(Object key)
  {
    return has_key(key);
  }

  public boolean containsValue(Object value)
  {
    for(Iterator iter = getIterationSet().iterator(); iter.hasNext();)
      {
        if (iter.next().equals(value))
          {
            return true;
          }
      }
    return false;
  }

  public Set entrySet()
  {
    Set entrySet = new HashSet(objectTable.size());
    DBObject obj;
    
    for (Iterator iter = getIterationSet().iterator(); iter.hasNext();)
      {
        obj = (DBObject) iter.next();
        entrySet.add(new Entry(obj));
      }
    
    return entrySet;
  }

  public Object get(Object key)
  {
    if (key instanceof PyInteger)
      {
        PyInteger pi = (PyInteger) key;
        return objectTable.get(pi.getValue());
      }
    else if (key instanceof Integer)
      {
        return objectTable.get(((Integer) key).intValue());
      }
    else if (key instanceof String)
      {
        /* Snag this object's label field */
        String labelFieldName = getLabelFieldName();
        if (labelFieldName == null)
          {
            return null;
          }
        
        /* Now we'll check to see if there's a namespace on this field */
        DBNameSpace namespace = ((DBObjectBaseField) getField(labelFieldName)).getNameSpace();
        if (namespace == null)
          {
            return null;
          }
        
        DBField field = namespace.lookup(key);
        if (field.getObjTypeID() == getTypeID())
          {
            return field.getOwner();
          }
        else
          {
            return null;
          }
      }
    return null;
  }

  public boolean isEmpty()
  {
    return objectTable.isEmpty();
  }

  public Set keySet()
  {
    return keys();
  }
  
  public int size()
  {
    return objectTable.size();
  }

  public Collection values()
  {
    return getIterationSet();
  }
  
  public String toString()
  {
    return keys().toString();
  }
  
  static class Entry implements Map.Entry
  {
    Object key, value;
    
    public Entry( DBObject obj )
    {
      key = obj.getLabel();
      value = obj;
    }
    
    public Object getKey()
    {
      return key;
    }

    public Object getValue()
    {
      return value;
    }

    public Object setValue(Object value)
    {
      return null;
    }
  }  
  
  /* 
   * These methods are are no-ops since we don't want this object
   * messed with via the Map interface.
   */
      
  public void clear()
  {
    return;
  }

  public Object put(Object key, Object value)
  {
    return null;
  }

  public void putAll(Map t)
  {
    return;
  }

  public Object remove(Object key)
  {
    return null;
  }

}
