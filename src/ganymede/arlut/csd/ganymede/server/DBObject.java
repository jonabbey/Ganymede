/*
   GASH 2

   DBObject.java

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
import java.io.PrintStream;
import java.io.PrintWriter;
import java.rmi.NoSuchObjectException;
import java.rmi.Remote;
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

import arlut.csd.Util.JythonMap;
import arlut.csd.Util.TranslationService;
import arlut.csd.Util.VectorUtils;
import arlut.csd.Util.XMLUtils;
import arlut.csd.ganymede.common.GanyPermissionsException;
import arlut.csd.ganymede.common.FieldInfo;
import arlut.csd.ganymede.common.FieldTemplate;
import arlut.csd.ganymede.common.FieldType;
import arlut.csd.ganymede.common.Invid;
import arlut.csd.ganymede.common.NotLoggedInException;
import arlut.csd.ganymede.common.ObjectStatus;
import arlut.csd.ganymede.common.PermEntry;
import arlut.csd.ganymede.common.ReturnVal;
import arlut.csd.ganymede.common.SchemaConstants;
import arlut.csd.ganymede.rmi.db_field;
import arlut.csd.ganymede.rmi.db_object;

/*------------------------------------------------------------------------------
                                                                           class
                                                                        DBObject

------------------------------------------------------------------------------*/

/**
 * <p>Class to hold a typed, read-only database object as represented
 * in the Ganymede {@link arlut.csd.ganymede.server.DBStore DBStore}
 * database.  DBObjects can be exported via RMI for remote access by
 * remote clients. Clients directly access instances of DBObject for
 * viewing or editing in the form of a {@link
 * arlut.csd.ganymede.rmi.db_object db_object} RMI interface type passed
 * as return value in calls made on the {@link
 * arlut.csd.ganymede.rmi.Session Session} remote interface.</p>
 *
 * <p>A DBObject is identified by a unique identifier called an {@link
 * arlut.csd.ganymede.common.Invid Invid} and contains a set of {@link
 * arlut.csd.ganymede.server.DBField DBField} objects which hold the actual
 * data values held in the object.  The client typically interacts
 * with the fields held in this object directly using the {@link
 * arlut.csd.ganymede.rmi.db_field db_field} remote interface which is
 * returned by the DBObject getField methods.  DBObject is not
 * directly involved in the client's interaction with the DBFields,
 * although the DBFields will call methods on the owning DBObject to
 * consult about permissions and the like.  Clients that call the
 * GanymedeSession's {@link
 * arlut.csd.ganymede.server.GanymedeSession#view_db_object(arlut.csd.ganymede.common.Invid)
 * view_db_object()} method to view a DBObject actually interact with
 * a copy of the DBObject created by the view_db_object() method to
 * enforce appropriate read permissions.</p>
 *
 * <p>A plain DBObject is not editable;  all value-changing calls to DBFields contained
 * in a plain DBObject will reject any change requests.  In order to edit a DBObject,
 * a client must get access to a {@link arlut.csd.ganymede.server.DBEditObject DBEditObject}
 * object derived from the DBObject.  This is typically done by calling
 * {@link arlut.csd.ganymede.rmi.Session#edit_db_object(arlut.csd.ganymede.common.Invid) edit_db_object}
 * on the server's {@link arlut.csd.ganymede.rmi.Session Session} remote interface.</p>
 *
 * <p>The DBStore contains a single read-only DBObject in its database for each Invid.
 * In order to change a DBObject, that DBObject must have its 
 * {@link arlut.csd.ganymede.server.DBObject#createShadow(arlut.csd.ganymede.server.DBEditSet) createShadow}
 * method called.  This is a synchronized method which attaches a new DBEditObject
 * to the DBObject.  Only one DBEditObject can be created from a single DBObject at
 * a time, and it must be created in the context of a
 * {@link arlut.csd.ganymede.server.DBEditSet DBEditSet} transaction object.  Once the DBEditObject
 * is created, that transaction has exclusive right to make changes to the DBEditObject.  When
 * the transaction is committed, a new DBObject is created from the values held in the
 * DBEditObject.  That DBObject is then placed back into the DBStore, replacing the
 * original DBObject.  If instead the transaction is aborted, the DBObject forgets about
 * the DBEditObject that had been attached to it and the DBObject is once again available
 * for other transactions to edit.</p>
 *
 * <p>Actually, the above picture is a bit too simple.  The server's DBStore object does
 * not directly contain DBObjects, but instead contains
 * {@link arlut.csd.ganymede.server.DBObjectBase DBObjectBase} objects, which define a type
 * of DBObject, and contain all DBObjects of that type in turn.  The DBObjectBase
 * is responsible for making sure that each DBObject has its own unique Invid based
 * on the DBObjectBase's type id and a unique number for the individual DBObject.</p>
 * 
 * <p>In terms of type definition, the DBObjectBase object acts as a template for
 * objects of the type.  Each DBObjectBase contains a set of
 * {@link arlut.csd.ganymede.server.DBObjectBaseField DBObjectBaseField} objects which
 * define the names and types of DBFields that a DBObject of that type is
 * meant to store.</p>
 *
 * <p>In addition, each DBObjectBase can be linked to a custom DBEditObject subclass
 * that oversees all kinds of operations on DBObjects of this kind.  Custom
 * DBEditObject subclasses can define special logic for object creation, viewing,
 * and editing, including custom object linking logic, acceptable value constraints, 
 * and even step-by-step wizard dialog sequences to oversee certain kinds of
 * operations.</p>
 *
 * <p>All DBObjects have a certain number of DBFields pre-defined, including an
 * {@link arlut.csd.ganymede.server.InvidDBField InvidDBField} listing the owner groups
 * that this DBObject belongs to, a number of {@link arlut.csd.ganymede.server.StringDBField StringDBField}s
 * that contain information about the last admin to modify this DBObject,
 * {@link arlut.csd.ganymede.server.DateDBField DateDBField}s recording the creation and
 * last modification dates of this object, and so on.  See 
 * {@link arlut.csd.ganymede.common.SchemaConstants SchemaConstants} for details on the
 * built-in field types.</p>
 *
 * <p>DBObject has had its synchronization revised so that only the createShadow,
 * clearShadow, getFieldPerm, receive, and emitXML methods are sync'ed on
 * the DBObject itself.  Everything else syncs on the field table held within
 * the DBObject.  createShadow() and clearShadow() in particular must remain
 * sync'ed on the same monitor, but for most things we want to sync on the
 * interior fieldAry.</p>
 *
 * <p>Is all this clear?  Good!</p>
 *
 * @version $Id$
 * @author Jonathan Abbey, jonabbey@arlut.utexas.edu, ARL:UT
 */

public class DBObject implements db_object, FieldType, Remote, JythonMap {

  static boolean debug = false;
  final static boolean debugEmit = false;

  /**
   * <p>TranslationService object for handling string localization in
   * the Ganymede server.</p>
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.server.DBObject");

  // ---

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
   * Our fields, hashed into an array
   *
   * @see arlut.csd.ganymede.server.DBField
   *
   */

  protected DBField[] fieldAry;

  /**
   * Permission cache for our fields, hashed into an array
   * using the same indexing as fieldAry.
   */

  protected PermEntry[] permCacheAry;

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
    fieldAry = null;
    permCacheAry = null;

    shadowObject = null;

    myInvid = Invid.createInvid(objectBase.type_code, 0);
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
    myInvid = Invid.createInvid(objectBase.type_code, id);
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
    if (objectBase == null)
      {
	throw new RuntimeException("Error, null object base");
      }

    this.objectBase = objectBase;
    shadowObject = null;
    receive(in, journalProcessing);
    gSession = null;
  }

  /**
   * <p>This constructor is used to create a non-editable DBObject from a
   * DBEditObject that we have finished editing.  Whenever a
   * transaction checks a created or edited shadow back into the
   * DBStore, it actually does so by creating a new DBObject to
   * replace any previous version of the object in the DBStore.</p>
   *
   * @param eObj The shadow object to copy into the new DBObject
   *
   * @see arlut.csd.ganymede.server.DBEditSet#commit()
   * @see arlut.csd.ganymede.server.DBEditSet#release()
   */
  
  DBObject(DBEditObject eObj)
  {
    DBField field;

    /* -- */

    objectBase = eObj.objectBase;
    myInvid = eObj.myInvid;

    shadowObject = null;

    short count = 0;

    synchronized (eObj.fieldAry)
      {
	for (short i = 0; i < eObj.fieldAry.length; i++)
	  {
	    field = eObj.fieldAry[i];
	    
	    if (field != null && field.isDefined())
	      {
		count++;
	      }
	  }

	// put any defined fields into the object we're going
	// to commit back into our DBStore
	
	fieldAry = new DBField[count];
	
	for (short i = 0; i < eObj.fieldAry.length; i++)
	  {
	    field = eObj.fieldAry[i];
	    
	    if (field != null && field.isDefined())
	      {
		// clean up any cached data the field was holding during
		// editing
		
		field.cleanup();
		
		// Create a new copy and save it in the new DBObject.  We
		// *must not* save the field from the DBEditObject,
		// because that field has likely been RMI exported to a
		// remote client, and if we keep the exported field in
		// local use, all of the extra bulk of the RMI mechanism
		// will also be retained, as the DBField's Stub and Skel
		// are associated with the field through a weak hash ref.  By
		// letting the old field from the DBEditObject get locally
		// garbage collected, we make it possible for all the RMI
		// stuff to get garbage collected as well.
		
		// Making a copy here rather than saving a ref to the
		// exported field makes a *huge* difference in overall
		// memory usage on the Ganymede server.
		
		saveField(field.getCopy(this));	// safe since we started with an empty fieldAry
	      }
	  }
      }

    gSession = null;
  }

  /**
   * <p>This is a view-copy constructor, designed to make a view-only
   * duplicate of an object from the database.  This view-only object
   * knows who is looking at it through its GanymedeSession reference,
   * and so can properly enforce field access permissions.</p>
   *
   * <p>&lt;gSession&gt; may be null, in which case the returned DBObject
   * will be simply an un-linked fresh copy of &lt;original&gt;.</p>
   */

  public DBObject(DBObject original, GanymedeSession gSession)
  {
    DBField field;

    /* -- */

    objectBase = original.objectBase;
    myInvid = original.myInvid;

    shadowObject = null;

    synchronized (original.fieldAry)
      {
	// make fieldAry big enough to hold all fields defined, because
	// DBObjectDeltaRec uses this constructor when doing
	// journal edits to an object

	fieldAry = new DBField[objectBase.fieldTable.size()];

	// put any defined fields into the object we're going
	// to commit back into our DBStore
	
	for (int i = 0; i < original.fieldAry.length; i++)
	  {
	    field = original.fieldAry[i];
	    
	    if (field == null)
	      {
		continue;
	      }
	    
	    switch (field.getType())
	      {
	      case BOOLEAN:
		fieldAry[i] = new BooleanDBField(this, (BooleanDBField) field);
		
		break;
		
	      case NUMERIC:
		fieldAry[i] = new NumericDBField(this, (NumericDBField) field);
		
		break;
		
	      case FLOAT:
		fieldAry[i] = new FloatDBField(this, (FloatDBField) field);
		
		break;
		
	      case FIELDOPTIONS:
		fieldAry[i] = new FieldOptionDBField(this, (FieldOptionDBField) field);
		
		break;

	      case DATE:
		fieldAry[i] = new DateDBField(this, (DateDBField) field);
		
		break;
		
	      case STRING:
		fieldAry[i] = new StringDBField(this, (StringDBField) field);
		
		break;
		
	      case INVID:
		fieldAry[i] = new InvidDBField(this, (InvidDBField) field);
		
		break;
		
	      case PERMISSIONMATRIX:
		fieldAry[i] = new PermissionMatrixDBField(this, (PermissionMatrixDBField) field);
		
		break;
		
	      case PASSWORD:
		fieldAry[i] = new PasswordDBField(this, (PasswordDBField) field);
		
		break;
		
	      case IP:
		fieldAry[i] = new IPDBField(this, (IPDBField) field);
		
		break;
	      }
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
    DBField field;

    /* -- */

    synchronized (fieldAry)
      {
	for (int i = 0; i < fieldAry.length; i++)
	  {
	    field = fieldAry[i];

	    if (field == null)
	      {
		continue;
	      }

	    // export can fail if the object has already
	    // been exported.. don't worry about it if
	    // it happens.. the client will know about it
	    // if we try to pass a non-exported object
	    // back to it, anyway.
	
	    try
	      {
		Ganymede.rmi.publishObject(field);
	      }
	    catch (RemoteException ex)
	      {
		return;
	      }
	  }
      }
  }


  /**
   *
   * This method pulls the fields in this object from remote accessibility through
   * RMI, possibly improving our security posture and reducing the memory loading
   * on the RMI system.
   *  
   */

  public final void unexportFields()
  {
    DBField field;

    /* -- */

    synchronized (fieldAry)
      {
	for (int i = 0; i < fieldAry.length; i++)
	  {
	    field = fieldAry[i];

	    if (field == null)
	      {
		continue;
	      }

	    // unexport can fail if the object has already been
	    // unexported, or if it was never exported.  This should
	    // never happen, but if it does, we'll log it.
	
	    try
	      {
		Ganymede.rmi.unpublishObject(field, true);
	      }
	    catch (NoSuchObjectException ex)
	      {
		Ganymede.debug(Ganymede.stackTrace(ex)); // continue
	      }
	  }
      }
  }

  public final int hashCode()
  {
    return myInvid.getNum();
  }

  /**
   *
   * Returns the numeric id of the object in the objectBase
   *
   * @see arlut.csd.ganymede.rmi.db_object
   */

  public final int getID()
  {
    return myInvid.getNum();
  }

  /**
   *
   * Returns the invid of this object
   * for the db_object remote interface
   *
   * @see arlut.csd.ganymede.rmi.db_object
   */

  public final Invid getInvid()
  {
    return myInvid;
  }

  /**
   *
   * Returns the numeric id of the object's objectBase
   *
   * @see arlut.csd.ganymede.rmi.db_object
   */

  public final short getTypeID()
  {
    return objectBase.type_code;
  }

  /**
   *
   * Returns the name of the object's objectBase
   *
   * @see arlut.csd.ganymede.rmi.db_object
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
   * Returns the field definition for the given field code, or
   * null if that field code is not registered with this object
   * type.
   *
   */

  public final DBObjectBaseField getFieldDef(short fieldcode)
  {
    return (DBObjectBaseField) objectBase.getField(fieldcode);
  }

  public final synchronized PermEntry getFieldPerm(short fieldcode)
  {
    PermEntry result = null;

    /* -- */

    if (gSession == null)
      {
	return PermEntry.fullPerms; // assume supergash if we have no session
      }

    short index = findField(fieldcode);

    if (index == -1)
      {
	// "Unrecognized fieldcode: {0}"
	throw new IllegalArgumentException(ts.l("getFieldPerm.nofield", new Integer(fieldcode)));
      }

    if (permCacheAry == null)
      {
	permCacheAry = new PermEntry[fieldAry.length];
      }
    else
      {
	result = permCacheAry[index];
      }

    if (result == null)
      {
	result = gSession.getPerm(this, fieldcode);

	if (result == null)
	  {
	    result = gSession.getPerm(this);
	  }
      }

    permCacheAry[index] = result;

    return result;
  }

  /**
   * <p>Returns the GanymedeSession that this object is checked out in
   * care of.</p>
   */

  public final GanymedeSession getGSession()
  {
    return gSession;
  }

  /**
   *
   * Provide easy server-side access to this object's name in a String
   * context.
   *
   */

  public String toString()
  {
    return getLabel() + " (" + getInvid() + ")";
  }

  /**
   *
   * Simple equals test.. doesn't really test to see if things are
   * value-equals, but rather identity equals.
   *
   */

  public boolean equals(Object param)
  {
    if (!(param instanceof DBObject))
      {
	return false;
      }

    try
      {
	return (getInvid().equals(((DBObject) param).getInvid()));
      }
    catch (NullPointerException ex)
      {
	return false;
      }
  }

  /**
   * <P>Returns the primary label of this object.. calls
   * {@link arlut.csd.ganymede.server.DBEditObject#getLabelHook(arlut.csd.ganymede.server.DBObject) getLabelHook()}
   * on the {@link arlut.csd.ganymede.server.DBEditObject DBEditObject} serving
   * as the {@link arlut.csd.ganymede.server.DBObjectBase#objectHook objectHook} for
   * this object's {@link arlut.csd.ganymede.server.DBObjectBase DBObjectBase}
   * to get the label for this object.</P>
   *
   * <P>If the objectHook customization object doesn't define a getLabelHook()
   * method, this base implementation will return a string based on the
   * designated label field for this object, or a generic
   * label constructed based on the object type and invid if no label
   * field is designated.</P>
   *
   * <P>We don't synchronize getLabel(), as it is very, very frequently
   * called from all over, and we don't want to chance deadlock.  getField()
   * and getValueString() are both synchronized on subcomponents of DBObject,
   * so this method should be adequately safe as written.</P>
   *
   * <P>Note that while you probably should seek to do so, it is not
   * necessarily guaranteed that the label returned by getLabel() will
   * be unique amongst all objects of the same type as this object.
   * The labels will be unique if a namespace-constrained label field
   * is assigned in the schema editor, or if the DBEditObject plug-in
   * class for this object type has a getLabelHook() method defined
   * and has overridden labelHookGuaranteedUnique() to return true,
   * thus affirmatively declaring that the labels returned by
   * getLabelHook() will always be unique.</P>
   *
   * @see arlut.csd.ganymede.rmi.db_object
   */

  public String getLabel()
  {
    String result = null;

    if (objectBase.getObjectHook().useLabelHook())
      {
	result = objectBase.getObjectHook().getLabelHook(this);
      }

    if (result == null)
      {
	// no class for this object.. just go
	// ahead and use the default label
	// obtaining bit

	short val = objectBase.getLabelField();

	if (val == -1)
	  {
	    return getDefaultUniqueLabel();
	  }
	else
	  {
	    // Ganymede.debug("Getting field " + val + " for label");

	    DBField f = (DBField) getField(val);

	    if (f != null && f.isDefined())
	      {
		// Ganymede.debug("Got field " + f);

		// string fields are most common for 
		// label fields.. return as quickly as possible,
		// without bothering with permission checking
		// for this common case.

		return f.getValueString();
	      }
	    else
	      {
		return getDefaultUniqueLabel();
	      }
	  }
      }
    else
      {
	return result;
      }
  }

  /**
   * <p>This method returns a guaranteed unique label for this object,
   * for use in xml dumps.  The returned string will be one of three
   * things.. it will either be 1) the contents of the label field, if
   * this object type has a defined label field and that label field
   * is namespace constrained, or 2) the custom label synthesized by this
   * object type's DBObjectBase's getObjectHook() plug-in's getLabelHook(),
   * if and only if the plug-in overrides labelHookGuaranteedUnique() to
   * return true, or 3) the output of getDefaultUniqueLabel(), which is a simple
   * fixed format that provides a combination of the type name and the invid
   * object number for this object.</p>
   *
   * <p>In any of these cases, the label will be guaranteed to be
   * persistent and unique for the purposes of the xml dumping, and
   * findable using the GanymedeSession class' internalQuery
   * mechanism.</p>
   */

  public final String getXMLLabel()
  {
    DBField labelField = (DBField) getLabelField();
    String result = null;

    if ((labelField != null && labelField.getNameSpace() != null) ||
	(objectBase.getObjectHook().useLabelHook() && objectBase.getObjectHook().labelHookGuaranteedUnique()))
      {
	result = getLabel();
      }

    // we shouldn't get a null or empty string from the above, but
    // just in case, fall back to our known good (if invid-dependent)
    // label

    if (result == null || result.equals(""))
      {
	result = getDefaultUniqueLabel();
      }

    return result;
  }

  /**
   * <p>This method returns a guaranteed unique label for this object.
   * The format of this string is not arbitrary.. the GanymedeSession
   * internalQuery() mechanism has special logic for doing optimized
   * lookups with this format.
   */

  public final String getDefaultUniqueLabel()
  {
    return getTypeName() + "[" + getID() + "]";
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
    // check to see if getLabelHook() is used to generate a string
    // label..  if so, there is no label field per se, and we'll
    // return null.

    if (objectBase.getObjectHook().useLabelHook())
      {
	return null;
      }

    // no calculated label for this object.. just go ahead and use the
    // default label obtaining bit
    
    short val = objectBase.getLabelField();

    if (val != -1)
      {
	// Ganymede.debug("Getting field " + val + " for label");

	DBField f = (DBField) getField(val);

	return f;
      }

    return null;
  }

  /**
   * <p>Get access to the field id for the field that serves as this
   * object's label, if any.</p>
   *
   * <p>Not all objects use simple field values as their labels.  If an
   * object has a calculated label, this method will return -1.</p> 
   */

  public short getLabelFieldID()
  {
    // check to see if getLabelHook() is used to generate a string
    // label..  if so, there is no label field per se, and we'll
    // return null.

    if (objectBase.getObjectHook().useLabelHook())
      {
	return -1;
      }

    // no calculated label for this object.. just go ahead and use the
    // default label obtaining bit
    
    return objectBase.getLabelField();
  }

  /**
   *
   * Returns true if this object is an embedded type.
   *
   * @see arlut.csd.ganymede.rmi.db_object
   *
   */

  public boolean isEmbedded()
  {
    return objectBase.isEmbedded();
  }

  /**
   * <p>The emit() method is part of the process of dumping the DBStore
   * to disk.  emit() dumps an object in its entirety to the
   * given out stream.</p>
   *
   * @param out A {@link arlut.csd.ganymede.server.DBJournal DBJournal} or
   * {@link arlut.csd.ganymede.server.DBStore DBStore} writing stream.
   */

  void emit(DataOutput out) throws IOException
  {
    DBField field;

    /* -- */

    //    System.err.println("Emitting " + objectBase.getName() + " <" + id + ">");

    out.writeInt(getID());	// write out our object id

    synchronized (fieldAry)
      {
	short count = 0;

	for (int i = 0; i < fieldAry.length; i++)
	  {
	    field = fieldAry[i];

	    if (field != null && field.isDefined())
	      {
		count++;
	      }
	  }

	if (count == 0)
	  {
	    // "**** Error: writing object with no fields: {0}"
	    Ganymede.debug(ts.l("emit.nofields", 
				objectBase.getName() + " <" + getID() + ">"));
	  }

	out.writeShort(count);

	for (int i = 0; i < fieldAry.length; i++)
	  {
	    field = fieldAry[i];

	    if (field != null && field.isDefined())
	      {
		out.writeShort(field.getID());
		field.emit(out);
	      }
	  }
      }
  }

  /**
   * <p>The receive() method is part of the process of loading the
   * {@link arlut.csd.ganymede.server.DBStore DBStore}
   * from disk.  receive() reads an object from the given in stream and
   * instantiates it into the DBStore.</p>
   *
   * <p>This method is synchronized, but there are a lot of other methods
   * in DBObject which are not synchronized and which could cause problems
   * if they are run concurrently with receive.  All the ones that
   * play in the fieldAry array.  This is only workable because receive
   * is not called on an object after it has been loaded into the
   * database.</p>
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
      tmp_count,
      upgradeSkipCount = 0;

    /* -- */

    // get our unique id

    myInvid = Invid.createInvid(objectBase.type_code, in.readInt());

    // get number of fields

    tmp_count = in.readShort();

    if (debug && tmp_count == 0)
      {
	// "DBObject.receive(): No fields reading object {0}"
	System.err.println(ts.l("receive.nofields", new Integer(getID())));
      }

    fieldAry = new DBField[tmp_count];
    permCacheAry = null;	// okay in synchronized block

    for (int i = 0; i < tmp_count; i++)
      {
	// read our field code, look it up in our
	// DBObjectBase

	fieldcode = in.readShort();

	definition = objectBase.fieldTable.get(fieldcode);

	// we used to have a couple of Invid vector fields that we
	// have gotten rid of, for the sake of improving Ganymede's
	// concurrency and reducing inter-object lock contention.  The
	// BackLinksField we got rid of a long time ago, the
	// OwnerBase's OwnerObjectsOwned field we got rid of at
	// DBStore 2.7.

	if ((fieldcode == SchemaConstants.BackLinksField) ||
	    (getTypeID() == SchemaConstants.OwnerBase && fieldcode == SchemaConstants.OwnerObjectsOwned))
	  {
	    // the backlinks field was always a vector of invids, so
	    // now that we are no longer explicitly recording
	    // asymmetric relationships with the backlinks field, we
	    // can just skip forward in the database file and skip the
	    // backlinks info

	    // Ditto, starting at DBStore version 2.7, with the
	    // OwnerBase's OwnerObjectsOwned

	    if (Ganymede.db.isLessThan(2,3))
	      {
		upgradeSkipCount = in.readShort();
	      }
	    else
	      {
		upgradeSkipCount = in.readInt();
	      }

	    int count = upgradeSkipCount;

	    while (count-- > 0)
	      {
		in.readShort();
		in.readInt();
	      }

	    continue;
	  }
	else if (definition == null)
	  {
	    // "What the heck?  Null definition for {0}, fieldcode = {1}, {2}th field in object"
	    System.err.println(ts.l("receive.nulldef", this.getTypeName(), new Integer(fieldcode), new Integer(i)));
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

	  case FLOAT:
	    tmp = new FloatDBField(this, in, definition);
	    break;

	  case FIELDOPTIONS:
	    tmp = new FieldOptionDBField(this, in, definition);
	    break;

	  case DATE:
	    tmp = new DateDBField(this, in, definition);
	    break;

	  case STRING:
	    tmp = new StringDBField(this, in, definition);
	    break;

	  case INVID:
	    tmp = new InvidDBField(this, in, definition);

	    // at 1.17 we started ignoring back links field, so we
	    // don't want to actually retain such in memory if we find
	    // one.

	    if (fieldcode == SchemaConstants.BackLinksField)
	      {
		continue;	// don't actually put this field in the object
	      }

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
	    // "Don't recognize field type in datastore."
	    throw new Error(ts.l("receive.badfieldtype"));
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
		    if (definition.namespace.containsKey(tmp.key(j)))
		      {
			// "Non-unique value {0} detected in vector field {1} which is constrained by namespace {2}"
			throw new RuntimeException(ts.l("receive.vectornamespace", tmp.key(j), definition, definition.namespace));
		      } 

		    definition.namespace.putHandle(tmp.key(j), 
						   new DBNameSpaceHandle(null, true, tmp));
		  }
	      }
	    else
	      {
		// mark the scalar value in the namespace
		
		if (definition.namespace.containsKey(tmp.key()))
		  {
		    // "Non-unique value {0} detected in scalar field {1} which is constrained by namespace {2}"
		    throw new RuntimeException(ts.l("receive.scalarnamespace", tmp.key(), definition, definition.namespace));
		  }

		definition.namespace.putHandle(tmp.key(), 
					       new DBNameSpaceHandle(null, true, tmp));
	      }
	  }
	
	// now add the field to our fields table

	if (tmp.isDefined())
	  {
	    saveField(tmp);	// safe since we started with an empty fieldAry
	  }
	else
	  {
	    // "%%% Loader skipping empty field {0}"
	    System.err.println(ts.l("receive.skipping", definition.getName()));
	  }
      }

    if (getTypeID() == SchemaConstants.OwnerBase && upgradeSkipCount != 0)
      {
	// "Skipped over {0} objects in deprecated OwnerObjectsOwned field while reading owner group {1}"
	System.err.println(ts.l("receive.upgradeSkippingOwned", new Integer(upgradeSkipCount), this.getLabel()));
      }
  }

  /**
   * <p>This method is used when this object is being dumped.  It is
   * mated with receiveXML().</p> 
   */

  synchronized public void emitXML(XMLDumpContext xmlOut) throws IOException
  {
    xmlOut.startElementIndent("object");
    xmlOut.attribute("type", XMLUtils.XMLEncode(getTypeName()));
    xmlOut.attribute("id", getXMLLabel());
    xmlOut.indentOut();

    // by using getFieldVector(), we get the fields in display
    // order

    Vector fieldVec = getFieldVector(false);

    for (int i = 0; i < fieldVec.size(); i++)
      {
	DBField field = (DBField) fieldVec.elementAt(i);
	    
	if (field.isDefined() && xmlOut.mayInclude(field))
	  {
	    field.emitXML(xmlOut);
	  }
      }

    xmlOut.indentIn();
    xmlOut.endElementIndent("object");
  }

  /**
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
   */

  synchronized DBEditObject createShadow(DBEditSet editset)
  {
    if (shadowObject != null)
      {
	// this object has already been checked out
	// for editing / deleting

	return null;
      }

    shadowObject = objectBase.createNewObject(this, editset);

    // if this object currently points to an object that
    // is being deleted by way of an asymmetric InvidDBField,
    // addObject() may fail.  In this case, we have to deny
    // the edit

    if (!editset.addObject(shadowObject))
      {
	shadowObject = null;
	return null;
      }

    // update the session's checkout count first, then
    // update the database's overall checkout, which
    // will trigger a console update

    if (editset.session.GSession != null)
      {
	editset.session.GSession.checkOut(); // update session checked out count
      }

    objectBase.store.checkOut(); // update checked out count

    return shadowObject;
  }

  /**
   * <p>This method is the complement to createShadow, and
   * is used during editset release.</p>
   *
   * @param editset The transaction owning this object's shadow.
   *
   * @see arlut.csd.ganymede.server.DBEditSet#release()
   *
   */

  synchronized boolean clearShadow(DBEditSet editset)
  {
    if (editset != shadowObject.editset)
      {
	// couldn't clear the shadow..  this editSet
	// wasn't the one to create the shadow

	// "DBObject.clearShadow(): couldn't clear, editset mismatch"
	Ganymede.debug(ts.l("clearShadow.mismatch"));

	return false;
      }

    shadowObject = null;

    if (editset.session.GSession != null)
      {
	editset.session.GSession.checkIn();
      }

    objectBase.store.checkIn(); // update checked out count

    return true;
  }

  /**
   * <p>Get read-only Vector of DBFieldInfo objects for the custom
   * DBFields contained in this object, in display order.</p>
   *
   * @see arlut.csd.ganymede.rmi.db_object
   */

  public Vector getFieldInfoVector()
  {
    Vector results = new Vector();
    DBField field;

    /* -- */

    synchronized (fieldAry)
      {
	for (int i = 0; i < objectBase.customFields.size(); i++)
	  {
	    DBObjectBaseField fieldDef = (DBObjectBaseField) objectBase.customFields.elementAt(i);
	    
	    field = retrieveField(fieldDef.getID());
	    
	    if (field != null)
	      {
		try
		  {
		    results.addElement(new FieldInfo(field));
		  }
		catch (IllegalArgumentException ex)
		  {
		    // we had a permissions failure reading this
		    // field.. skip it.
		  }
	      }
	  }
      }

    return results;
  }

  /**
   * <p>This method provides a Vector of DBFields contained in this
   * object in a fashion that does not contribute to fieldAry threadlock.</p>
   */

  public final Vector getFieldVect()
  {
    DBField field;
    Vector fieldVect = new Vector(fieldAry.length);

    synchronized (fieldAry)
      {
	for (int i = 0; i < fieldAry.length; i++)
	  {
	    field = fieldAry[i];

	    if (field == null)
	      {
		continue;
	      }
	    
	    fieldVect.addElement(field);
	  }
      }

    return fieldVect;
  }

  /**
   * <p>This method places a DBField into a slot in this object's
   * fieldAry DBField array.  As a (probably reckless) speed
   * optimization, this method makes no checks to ensure that another
   * DBField with the same field id has not previously been stored, so
   * it should only be used when the DBObject's fieldAry is in a known
   * state.  Otherwise, {@link
   * arlut.csd.ganymede.server.DBObject#clearField(short) clearField()}
   * should be called before calling saveField(), so that duplicate
   * field id's are not accidentally introduced into the DBObject's
   * fieldAry.</p>
   *
   * <p>saveField() uses a hashing algorithm to try and speed up field
   * save and retrieving, but we are optimizing for low memory usage
   * rather than O(1) saving and retrieving.  Hash collisions are
   * saved directly in the fieldAry, meaning that any hash collisions
   * increase the likelihood of further hash collisions, but we don't
   * need an extra 'next' pointer in the DBField class, saving us 4
   * bytes of memory for every field of every object in the
   * database.</p>.
   */

  public final void saveField(DBField field)
  {
    synchronized (fieldAry)
      {
	if (field == null)
	  {
	    // "null value passed to saveField"
	    throw new IllegalArgumentException(ts.l("saveField.null"));
	  }
	
	short hashindex = (short) ((field.getID() & 0x7FFF) % fieldAry.length);

	short index = hashindex;

	while (fieldAry[index] != null)
	  {
	    // we don't guarantee that the fieldAry has a prime
	    // length, so we have to use a linear hashing probe step
	    // of 1

	    if (++index >= fieldAry.length)
	      {
		index = 0;
	      }

	    if (index == hashindex)
	      {
		// couldn't find it
		
		// "full fieldAry hash"
		throw new ArrayIndexOutOfBoundsException(ts.l("saveField.full"));
	      }
	  }
	
	fieldAry[index] = field;
      }
  }

  /**
   * <p>This method replaces a DBField with a given field id in this
   * object's fieldAry DBField array with a new DBField sharing the
   * same id.  If this DBObject does not contain a field with the same
   * id as the field argument for this method, no action will be taken
   * and an IllegalArgumentException will be thrown.</p>
   *
   * <p>replaceField() uses a hashing algorithm to try and speed up
   * field save and retrieving, but we are optimizing for low memory
   * usage rather than O(1) saving and retrieving.  Hash collisions
   * are saved directly in the fieldAry, meaning that any hash
   * collisions increase the likelihood of further hash collisions,
   * but we don't need an extra 'next' pointer in the DBField class,
   * saving us 4 bytes of memory for every field of every object in
   * the database.</p>.
   */

  public final void replaceField(DBField field)
  {
    synchronized (fieldAry)
      {
	if (field == null)
	  {
	    // "null value passed to replaceField"
	    throw new IllegalArgumentException(ts.l("replaceField.null"));
	  }

	short id = field.getID();

	short hashindex = (short) ((id & 0x7FFF) % fieldAry.length);

	short index = hashindex;

	while ((fieldAry[index] == null) || (fieldAry[index].getID() != id))
	  {
	    // we don't guarantee that the fieldAry has a prime
	    // length, so we have to use a linear hashing probe step
	    // of 1

	    if (++index >= fieldAry.length)
	      {
		index = 0;
	      }

	    if (index == hashindex)
	      {
		// couldn't find it

		// "Error, DBObject.replaceField could not find matching field"
		throw new IllegalArgumentException(ts.l("replaceField.none"));
	      }
	  }

	fieldAry[index] = field;

	if (permCacheAry != null)
	  {
	    permCacheAry[index] = null;
	  }
      }
  }

  /**
   * <p>This method removes a DBField that has the a field id matching
   * the argument from this object's fieldAry.  This method will never
   * fail..  if there is no field matching the given field id, the
   * method will return without changing the fieldAry.</p>
   *
   * <p>clearField() uses a hashing algorithm to
   * try and speed up field save and retrieving, but we are optimizing
   * for low memory usage rather than O(1) saving and retrieving.
   * Hash collisions are saved directly in the fieldAry, meaning that
   * any hash collisions increase the likelihood of further hash
   * collisions, but we don't need an extra 'next' pointer in the
   * DBField class, saving us 4 bytes of memory for every field of
   * every object in the database.</p>.
   */

  public final void clearField(short id)
  {
    synchronized (fieldAry)
      {
	short hashindex = (short) ((id & 0x7FFF) % fieldAry.length);

	short index = hashindex;

	while ((fieldAry[index] == null) || (fieldAry[index].getID() != id))
	  {
	    // we don't guarantee that the fieldAry has a prime
	    // length, so we have to use a linear hashing probe step
	    // of 1

	    if (++index >= fieldAry.length)
	      {
		index = 0;
	      }

	    if (index == hashindex)
	      {
		// couldn't find it

		return;
	      }
	  }

	fieldAry[index] = null;

	if (permCacheAry != null)
	  {
	    permCacheAry[index] = null;
	  }
      }
  }

  /**
   * <p>This method retrieves a DBField from this object's
   * fieldAry DBField array.  retrieveField() uses a hashing algorithm to
   * try and speed up field retrieving, but we are optimizing
   * for low memory usage rather than O(1) operations.</p>
   */

  public final DBField retrieveField(short id)
  {
    synchronized (fieldAry)
      {
	short hashindex = (short) ((id & 0x7FFF) % fieldAry.length);

	short index = hashindex;

	while ((fieldAry[index] == null) || (fieldAry[index].getID() != id))
	  {
	    // we don't guarantee that the fieldAry has a prime
	    // length, so we have to use a linear hashing probe step
	    // of 1

	    if (++index >= fieldAry.length)
	      {
		index = 0;
	      }

	    if (index == hashindex)
	      {
		// couldn't find it

		return null;
	      }
	  }

	return fieldAry[index];
      }
  }

  /**
   * <p>This method finds the index for the given field id in this object's
   * fieldAry and permCacheAry tables.</p>
   *
   * @return -1 if we couldn't find a field with the given id
   */

  public final short findField(short id)
  {
    synchronized (fieldAry)
      {
	short hashindex = (short) ((id & 0x7FFF) % fieldAry.length);

	short index = hashindex;

	while ((fieldAry[index] == null) || (fieldAry[index].getID() != id))
	  {
	    // we don't guarantee that the fieldAry has a prime
	    // length, so we have to use a linear hashing probe step
	    // of 1

	    if (++index >= fieldAry.length)
	      {
		index = 0;
	      }

	    if (index == hashindex)
	      {
		// couldn't find it

		return -1;
	      }
	  }

	return index;
      }
  }

  /**
   * <p>This method clears any cached PermEntry value for the
   * given field id.</p>.
   */

  public final void clearFieldPerm(short id)
  {
    synchronized (fieldAry)
      {
	short hashindex = (short) ((id & 0x7FFF) % fieldAry.length);

	short index = hashindex;

	while ((fieldAry[index] == null) || (fieldAry[index].getID() != id))
	  {
	    // we don't guarantee that the fieldAry has a prime
	    // length, so we have to use a linear hashing probe step
	    // of 1

	    if (++index >= fieldAry.length)
	      {
		index = 0;
	      }

	    if (index == hashindex)
	      {
		// couldn't find it

		return;
	      }
	  }

	if (permCacheAry != null)
	  {
	    permCacheAry[index] = null;
	  }
      }
  }

  /**
   * <p>Get access to a field from this object.  This method
   * is exported to clients over RMI.</p>
   *
   * @param id The field code for the desired field of this object.
   *
   * @see arlut.csd.ganymede.rmi.db_object
   *
   */

  public final db_field getField(short id)
  {
    return retrieveField(id);
  }

  /**
   * <p>Get read-only access to a field from this object, by name.</p>
   *
   * @param fieldname The fieldname for the desired field of this object
   *
   * @see arlut.csd.ganymede.rmi.db_object
   */

  public final db_field getField(String fieldname)
  {
    DBField field;

    /* -- */

    synchronized (fieldAry)
      {
	for (int i = 0; i < fieldAry.length; i++)
	  {
	    field = fieldAry[i];
	    
	    if (field != null && field.getName().equalsIgnoreCase(fieldname))
	      {
		return field;
	      }
	  }
      }

    return null;
  }

  /**
   * <P>Returns the name of a field from this object.</P>
   *
   * @param id The field code for the desired field of this object.
   */

  public final String getFieldName(short id)
  {
    DBField field = retrieveField(id);

    if (field != null)
      {
	return field.toString();
      }

    return "<<" + id + ">>";
  }

  /**
   * <p>This method returns the short field id code for the named
   * field, if the field is present in this object, or -1 if the
   * field could not be found.</p>
   */

  public final short getFieldId(String fieldname)
  {
    DBField field;

    /* -- */

    synchronized (fieldAry)
      {
	for (int i = 0; i < fieldAry.length; i++)
	  {
	    field = fieldAry[i];

	    if (field != null && field.getName().equalsIgnoreCase(fieldname))
	      {
		return field.getID();
	      }
	  }
      }

    return -1;
  }

  /**
   * <p>Get complete list of DBFields contained in this object.
   * The list returned will appear in unsorted order.</p>
   *
   * @see arlut.csd.ganymede.rmi.db_object
   */

  public db_field[] listFields()
  {
    db_field result[];
    short count = 0;

    synchronized (fieldAry)
      {
	for (int i = 0; i < fieldAry.length; i++)
	  {
	    if (fieldAry[i] != null)
	      {
		count++;
	      }
	  }

	result = new db_field[count];

	count = 0;

	for (int i = 0; i < fieldAry.length; i++)
	  {
	    if (fieldAry[i] != null)
	      {
		result[count++] = fieldAry[i];
	      }
	  }
      }

    return result;
  }

  /**
   * <p>Returns true if inactivate() is a valid operation on
   * checked-out objects of this type.</p>
   *
   * @see arlut.csd.ganymede.rmi.db_object
   */

  public boolean canInactivate()
  {
    return objectBase.canInactivate();
  }
  
  /**
   * <p>Returns true if this object has been inactivated and is
   * pending deletion.</p>
   *
   * @see arlut.csd.ganymede.rmi.db_object
   */

  public boolean isInactivated()
  {
    return (objectBase.canInactivate() && 
	    (getFieldValueLocal(SchemaConstants.RemovalField) != null));
  }

  /**
   * <p>Returns true if this object has all its required fields defined</p>
   *
   * <p>This method can be overridden in DBEditObject subclasses to do a
   * more refined validity check if desired.</p>
   *
   * @see arlut.csd.ganymede.rmi.db_object
   */

  public boolean isValid()
  {
    return (checkRequiredFields() == null);
  }

  /**
   * <p>This method scans through all fields defined in the 
   * {@link arlut.csd.ganymede.server.DBObjectBase DBObjectBase}
   * for this object type and determines if all required fields have
   * been filled in.  If everything is ok, this method will return
   * null.  If any required fields are found not to have been filled
   * out, this method returns a vector of field names that need to
   * be filled out.</p>
   *
   * <p>This method is used by the transaction commit logic to ensure a
   * consistent transaction. If server-local code has called
   * {@link arlut.csd.ganymede.server.GanymedeSession#enableOversight(boolean) GanymedeSession.enableOversight(false)}
   * this method will not be called at transaction commit time.</p>
   */

  public final Vector checkRequiredFields()
  {
    Vector localFields = new Vector();
    DBField field = null;

    /* -- */

    // sync on fieldAry since we are looping over our fields and since retrieveField itself
    // sync's on fieldAry

    synchronized (fieldAry)
      {
	// assume that the object type's fields will not be changed at a
	// time when this method is called.  A reasonable assumption,
	// as the objectbase field table is only altered when the
	// schema is being edited.
	
	Vector fieldTemplates = objectBase.getFieldTemplateVector();

	for (int i = 0; i < fieldTemplates.size(); i++)
	  {
	    FieldTemplate template = (FieldTemplate) fieldTemplates.elementAt(i);
	    
	    try
	      {
		// nota bene: calling fieldRequired here could
		// potentially leave us open for threadlock, depending
		// on how the fieldRequired method is written.  I
		// think this is a low-level risk, but not zero.

		if (objectBase.getObjectHook().fieldRequired(this, template.getID()))
		  {
		    field = retrieveField(template.getID());
		    
		    if (field == null || !field.isDefined())
		      {
			localFields.addElement(template.getName());
		      }
		  }
	      }
	    catch (NullPointerException ex)
	      {
		System.err.println("Null pointer exception in checkRequiredFields().");
		ex.printStackTrace();
		System.err.println("\n");
		
		System.err.println("My type is " + getTypeName() + "\nMy invid is " + getInvid());
	      }
	  }
      }

    // if all required fields checked out, return null to signify success

    if (localFields.size() == 0)
      {
	return null;
      }
    else
      {
	return localFields;
      }
  }

  /**
   * <p>Returns the date that this object is to go through final removal
   * if it has been inactivated.</p>
   *
   * @see arlut.csd.ganymede.rmi.db_object
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
   * <p>Returns true if this object has an expiration date set.</p>
   *
   * @see arlut.csd.ganymede.rmi.db_object
   */

  public boolean willExpire()
  {
    return (getFieldValueLocal(SchemaConstants.ExpirationField) != null);
  }

  /**
   * <p>Returns true if this object has a removal date set.</p>
   *
   * @see arlut.csd.ganymede.rmi.db_object
   */

  public boolean willBeRemoved()
  {
    return (getFieldValueLocal(SchemaConstants.RemovalField) != null);
  }

  /**
   * <p>Returns the date that this object is to be automatically
   * inactivated if it has an expiration date set.</p>
   *
   * @see arlut.csd.ganymede.rmi.db_object
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
   * <p>Returns true if this object has an 'in-care-of' email address
   * that should be notified when this object is changed.</p>
   */

  public final boolean hasEmailTarget()
  {
    return objectBase.getObjectHook().hasEmailTarget(this);
  }

  /**
   * <p>Returns a vector of email addresses that can be used to send
   * 'in-care-of' email for this object.</p>
   */

  public final Vector getEmailTargets()
  {
    return objectBase.getObjectHook().getEmailTargets(this);
  }

  /**
   * <p>Shortcut method to set a field's value.  Using this
   * method saves a roundtrip to the server, which is
   * particularly useful in database loading.</p>
   *
   * @see arlut.csd.ganymede.rmi.db_object
   */

  public ReturnVal setFieldValue(short fieldID, Object value) throws GanyPermissionsException
  {
    // we override in DBEditObject

    // "Server: Error in DBObject.setFieldValue()"
    // "setFieldValue called on a non-editable object"

    return Ganymede.createErrorDialog(ts.l("setFieldValue.noneditable"),
				      ts.l("setFieldValue.noneditabletext"));
  }

  /**
   * <p>Shortcut method to get a field's value.  Using this
   * method saves a roundtrip to the server, which is
   * particularly useful in database loading.</p>
   *
   * @see arlut.csd.ganymede.rmi.db_object
   */

  public Object getFieldValue(short fieldID) throws GanyPermissionsException
  {
    DBField f = (DBField) getField(fieldID);

    /* -- */
    
    if (f == null)
      {
	return null;
      }

    if (f.isVector())
      {
	// "Couldn't get scalar value on vector field {0}"
	throw new IllegalArgumentException(ts.l("getFieldValue.badtype", new Integer(fieldID)));
      }

    return f.getValue();
  }

  /**
   * <p>Shortcut method to get a field's value.  Used only
   * on the server, as permissions are not checked.</p>
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
	// "Couldn't get scalar value on vector field {0}"
	throw new IllegalArgumentException(ts.l("getFieldValue.badtype", new Integer(fieldID)));
      }

    return f.getValueLocal();
  }

  /**
   * <P>This method is for use on the server, so that custom code can call a simple
   * method to test to see if a boolean field is defined and has a true value.</P>
   *
   * <P>An exception will be thrown if the field is not a boolean.</P>
   */

  public boolean isSet(short fieldID)
  {
    DBField f = (DBField) getField(fieldID);

    /* -- */
    
    if (f == null)
      {
	return false;
      }

    if (f.isVector())
      {
	// "Can't call isSet on a vector field."
	throw new RuntimeException(ts.l("isSet.vector"));
      }

    Boolean bool = (Boolean) f.getValueLocal();

    return (bool != null && bool.booleanValue());
  }

  /**
   * <p>Shortcut method to set a field's value.  Using this
   * method saves a roundtrip to the server, which is
   * particularly useful in database loading.</p>
   *
   * @see arlut.csd.ganymede.rmi.db_object
   */

  public Vector getFieldValues(short fieldID) throws GanyPermissionsException
  {
    DBField f = (DBField) getField(fieldID);

    /* -- */
    
    if (f == null)
      {
	return null;
      }

    if (!f.isVector())
      {
	// "Couldn't get vector values on scalar field {0}"
	throw new IllegalArgumentException(ts.l("getFieldValues.badtype", new Integer(fieldID)));
      }

    return f.getValues();
  }

  /**
   * <p>Shortcut method to set a field's value.  This
   * is a server-side method, but it can be a quick
   * way to get a vector of elements.</p>
   *
   * <p><b>Warning!</b>  The Vector returned by getFieldValuesLocal()
   * is not a clone, but is direct access to the vector
   * held in the DBField.  Clone the vector you get back
   * if you need to do anything with it other than read
   * it.</p>
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
	// "Couldn't get vector values on scalar field {0}"
	throw new IllegalArgumentException(ts.l("getFieldValues.badtype", new Integer(fieldID)));
      }

    return f.getValuesLocal();
  }

  /**
   * <p>Get a display-order sorted list of DBFields contained in this
   * object.</p>
   *
   * <p>This is a server-side only operation.. permissions are not
   * checked.</p>
   */

  public Vector getFieldVector(boolean customOnly)
  {
    Vector results = new Vector();
    DBField field;

    /* -- */

    // sync on fieldAry so we can loop over fields

    synchronized (fieldAry)
      {
	if (customOnly)
	  {
	    // return the custom fields only, in display order
	    
	    for (int i = 0; i < objectBase.customFields.size(); i++)
	      {
		DBObjectBaseField fieldDef = (DBObjectBaseField) objectBase.customFields.elementAt(i);
		
		field = retrieveField(fieldDef.getID());
		
		if (field != null)
		  {
		    results.addElement(field);
		  }
	      }
	  }
	else			// all fields in this object
	  {
	    // first the display fields
	    
	    for (int i = 0; i < objectBase.customFields.size(); i++)
	      {
		DBObjectBaseField fieldDef = (DBObjectBaseField) objectBase.customFields.elementAt(i);
		
		field = retrieveField(fieldDef.getID());
		
		if (field != null)
		  {
		    results.addElement(field);
		  }
	      }
	    
	    // then tack on the built-in fields
	    
	    for (int i = 0; i < fieldAry.length; i++)
	      {
		field = fieldAry[i];
		
		if (field != null && field.isBuiltIn())
		  {
		    results.addElement(field);
		  }
	      }
	  }
      }
    
    return results;
  }

  /**
   * <p>This method is used to provide a hook to allow different
   * objects to generate different labels for a given object
   * based on their perspective.  This is used to sort
   * of hackishly simulate a relational-type capability for
   * the purposes of viewing backlinks.</p>
   *
   * <p>See the automounter map and NFS volume DBEditObject
   * subclasses for how this is to be used, if you have
   * them.</p>
   */
  
  public String lookupLabel(DBObject object)
  {
    return object.getLabel();	// default
  }

  /**
   * <p>This method is used to correct this object's base pointers
   * when the base changes.  This happens when the schema is
   * edited.. this method is called on all objects under a {@link
   * arlut.csd.ganymede.server.DBObjectBase DBObjectBase} to make the object
   * point to the new version of the DBObjectBase.  This method also
   * takes care of cleaning out any fields that have become undefined
   * due to a change in the schema for the field, as in a change from
   * a vector to a scalar field, or vice-versa.</p>
   */

  void updateBaseRefs(DBObjectBase newBase)
  {
    this.objectBase = newBase;

    DBField field;
    short count = 0;

    // we need to be double-synchronized because we are looping
    // over fieldAry and because we are replacing fieldAry in midstream

    DBField oldAry[] = fieldAry;

    synchronized (oldAry)
      {
	for (int i = 0; i < fieldAry.length; i++)
	  {
	    field = fieldAry[i];

	    if (field == null)
	      {
		continue;
	      }

	    if (newBase.getField(field.getID()) != null && field.isDefined())
	      {
		count++;
	      }
	  }

	DBField tmpFieldAry[] = new DBField[count];

	// we sync on the new field ary before we update the fieldAry ref so
	// that we can preemptively block other threads from messing
	// with fieldAry until we get it set the way we want

	synchronized (tmpFieldAry)
	  {
	    fieldAry = tmpFieldAry;

	    permCacheAry = null;	// okay in synchronized block

	    for (int i = 0; i < oldAry.length; i++)
	      {
		field = oldAry[i];
		
		if (field == null)
		  {
		    continue;
		  }
		
		if (newBase.getField(field.getID()) != null && field.isDefined())
		  {
		    saveField(field); // safe since we start with an empty fieldAry
		  }
	      }
	  }
      }
  }

  /**
   * <p>This method is a convenience for server-side code.  If
   * this object is an embedded object, this method will
   * return the label of the containing object.  If this
   * object is not embedded, or the containing object's
   * label cannot be determined, null will be returned.</p>
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

  /** 
   * <p>This method returns a Vector of Invids for objects that are
   * pointed to from this object by way of non-symmetric links.  These
   * are Invids that may need to be marked as non-deletable if this
   * object is checked out by a DBEditSet.</p> 
   */

  public Vector getASymmetricTargets()
  {
    Vector results = new Vector();
    DBField field;
    InvidDBField invField;

    synchronized (fieldAry)
      {
	for (int i = 0; i < fieldAry.length; i++)
	  {
	    field = fieldAry[i];

	    if (field == null)
	      {
		continue;
	      }

	    if (field instanceof InvidDBField)
	      {
		invField = (InvidDBField) field;

		if (invField.isDefined() && !invField.getFieldDef().isSymmetric())
		  {
		    if (!invField.isVector())
		      {
			VectorUtils.unionAdd(results, invField.value);
		      }
		    else
		      {
			results = VectorUtils.union(results, invField.getValuesLocal());
		      }
		  }
	      }
	  }
      }

    return results;
  }

  /**
   * <p>This method returns a Vector of Invids that point to this object via
   * asymmetric link fields.</p>
   */

  public Vector getBackLinks()
  {
    Vector results = new Vector();

    synchronized (Ganymede.db.backPointers)
      {
	Hashtable table = (Hashtable) Ganymede.db.backPointers.get(this.getInvid());

	if (table != null)
	  {
	    int size = table.size();
	    Enumeration en = table.elements();

	    for (int i = 0; i < size; i++)
	      {
		results.addElement(en.nextElement());
	      }
	  }
      }

    return results;
  }

  /**
   * <p>This method is called to register all asymmetric pointers in
   * this object with the DBStore's backPointers hash structure.</p>
   *
   * <p>Typically this will be done when an object is first loaded from
   * the database, at a time when the DBStore backPointers hash structure
   * has no entries for this object at all.</p>
   *
   * <p>During the commit process of a normal transaction, the
   * {@link arlut.csd.ganymede.server.DBEditSet#syncObjBackPointers(arlut.csd.ganymede.server.DBEditObject) syncObjBackPointers()}
   * method in the {@link arlut.csd.ganymede.server.DBEditSet DBEditSet} class handles these
   * updates.</p>
   */

  void setBackPointers()
  {
    Vector backPointers;
    Invid target;
    Hashtable reverseLinks;

    /* -- */

    synchronized (Ganymede.db.backPointers)
      {
	backPointers = getASymmetricTargets();

	for (int i = 0; i < backPointers.size(); i++)
	  {
	    target = (Invid) backPointers.elementAt(i);

	    reverseLinks = (Hashtable) Ganymede.db.backPointers.get(target);

	    if (reverseLinks == null)
	      {
		reverseLinks = new Hashtable();
		Ganymede.db.backPointers.put(target, reverseLinks);
	      }

	    reverseLinks.put(getInvid(), getInvid());
	  }
      }
  }

  /**
   * <p>This method is called to unregister all asymmetric pointers in
   * this object from the DBStore's backPointers hash structure.</p>
   *
   * <p>Typically this will be done when an object is being deleted from
   * the database in response to a journal entry, or if the object is
   * being replaced with an updated version from the journal.</p>
   *
   * <p>During the commit process of a normal transaction, the
   * {@link arlut.csd.ganymede.server.DBEditSet#syncObjBackPointers(arlut.csd.ganymede.server.DBEditObject) syncObjBackPointers()}
   * method in the {@link arlut.csd.ganymede.server.DBEditSet DBEditSet} class handles these
   * updates.</p>
   */

  void unsetBackPointers()
  {
    Vector backPointers;
    Invid target;
    Hashtable reverseLinks;

    /* -- */

    synchronized (Ganymede.db.backPointers)
      {
	backPointers = getASymmetricTargets();

	for (int i = 0; i < backPointers.size(); i++)
	  {
	    target = (Invid) backPointers.elementAt(i);

	    reverseLinks = (Hashtable) Ganymede.db.backPointers.get(target);

	    if (reverseLinks == null)
	      {
		continue;
	      }

	    reverseLinks.remove(getInvid());

	    if (reverseLinks.size() == 0)
	      {
		Ganymede.db.backPointers.remove(target);
	      }
	  }
      }
  }

  /**
   * <p>Generate a complete printed representation of the object,
   * suitable for printing to a debug or log stream.</p>
   */

  public void print(PrintStream out)
  {
    out.print(getPrintString());
  }

  /**
   * <p>Generate a complete printed representation of the object,
   * suitable for printing to a debug or log stream.</p>
   */

  public void print(PrintWriter out)
  {
    out.print(getPrintString());
  }

  /**
   * <p>This server-side method returns a summary description of
   * this object, including a listing of all non-null fields and
   * their contents.</p>
   * 
   * <p>This method calls
   * {@link arlut.csd.ganymede.server.DBObject#appendObjectInfo(java.lang.StringBuffer,
   * java.lang.String, boolean) appendObjectInfo} to do most of its work.</p>
   */

  public String getPrintString()
  {
    StringBuffer result = new StringBuffer();

    this.appendObjectInfo(result, null, true);

    return result.toString();
  }

  /**
   * <p>This method is used to provide a summary description of
   * this object, including a listing of all non-null fields and
   * their contents.  This method is remotely callable by the client,
   * and so will only reveal fields that the user has permission
   * to view.  This method returns a StringBuffer to work around
   * problems with serializing large strings in early versions of the
   * JDK.</p>
   * 
   * <p>This method calls
   * {@link arlut.csd.ganymede.server.DBObject#appendObjectInfo(java.lang.StringBuffer,
   * java.lang.String, boolean) appendObjectInfo} to do most of its work.</p>
   */

  public StringBuffer getSummaryDescription()
  {
    StringBuffer result = new StringBuffer();

    if (gSession != null && !gSession.getPerm(this).isVisible())
      {
	return result;
      }

    this.appendObjectInfo(result, null, false);

    return result;
  }

  /**
   * <p>This method is used to concatenate a textual description of this
   * object to the passed-in StringBuffer.  This description is relatively
   * free-form, and is intended to be used for human consumption and not for
   * programmatic operations.</p>
   *
   * <p>This method is called by
   * {@link arlut.csd.ganymede.server.DBObject#getSummaryDescription() getSummaryDescription}.</p>
   *
   * @param buffer The StringBuffer to append this object's description to
   * @param prefix Used for recursive calls on embedded objects, this prefix will
   * be inserted at the beginning of each line of text concatenated to buffer
   * by this method.
   * @param local If false, read permissions will be checked for each field before
   * adding it to the buffer.
   */

  private void appendObjectInfo(StringBuffer buffer, String prefix, boolean local)
  {
    DBObjectBaseField fieldDef;
    DBField field;

    /* -- */

    // customFields shouldn't be changing unless we are in the middle of
    // schema editing

    Vector customFields = objectBase.customFields;

    synchronized (customFields)
      {
	for (int i = 0; i < customFields.size(); i++)
	  {
	    fieldDef = (DBObjectBaseField) customFields.elementAt(i);

	    field = retrieveField(fieldDef.getID());

	    if (field != null && field.isDefined() && (local || field.isVisible()))
	      {
		if (!field.isEditInPlace())
		  {
		    if (prefix != null)
		      {
			buffer.append(prefix);
		      }

		    buffer.append(field.getName());
		    buffer.append(" : ");
		    buffer.append(field.getValueString());
		    buffer.append("\n");
		  }
		else
		  {
		    InvidDBField invField = (InvidDBField) field;

		    for (int j = 0; j < invField.size(); j++)
		      {
			if (prefix != null)
			  {
			    buffer.append(prefix);
			  }

			buffer.append(field.getName());
			buffer.append("[");
			buffer.append(j);
			buffer.append("]");
			buffer.append("\n");
			
			Invid x = invField.value(j);

			DBObject remObj = null;

			if (gSession != null)
			  {
			    // if this object has been checked out for
			    // viewing by a session, we'll use
			    // view_db_object() so that we don't
			    // reveal fields that should not be seen.
			   
			    try
			      {
				ReturnVal retVal = gSession.view_db_object(x);
				remObj = (DBObject) retVal.getObject();
			      }
			    catch (NotLoggedInException ex)
			      {
			      }
			  }

			if (remObj == null)
			  {
			    // we use DBStore's static viewDBObject
			    // method so that we can call this even
			    // before the GanymedeServer object is
			    // initialized

			    remObj = DBStore.viewDBObject(x);
			  }

			if (remObj instanceof DBEditObject)
			  {
			    DBEditObject eO = (DBEditObject) remObj;
			    
			    if (eO.getStatus() == ObjectStatus.DELETING)
			      {
				remObj = eO.getOriginal();
			      }
			  }

			if (prefix != null)
			  {
			    remObj.appendObjectInfo(buffer, prefix + "\t", local);
			  }
			else
			  {
			    remObj.appendObjectInfo(buffer, "\t", local);
			  }
		      }
		  }
	      }
	  }
      }

    synchronized (fieldAry)
      {
	// okay, got all the custom fields.. now we need to summarize all the
	// built-in fields that were not listed in customFields.
	
	for (int i = 0; i < fieldAry.length; i++)
	  {
	    field = fieldAry[i];
	    
	    if (field == null || !field.isBuiltIn() || !field.isDefined())
	      {
		continue;
	      }

	    if (local || field.isVisible())
	      {
		if (prefix != null)
		  {
		    buffer.append(prefix);
		  }
		
		buffer.append(field.getName());
		buffer.append(" : ");
		buffer.append(field.getValueString());
		buffer.append("\n");
	      }
	  }
      }
  }

  /* *************************************************************************
  *
  * The following methods are for Jython/Map support
  *    
  * For this object, the Map interface allows for indexing based on either
  * the name or the numeric ID of a DBField. Indexing by numeric id, however,
  * is only supported for "direct" access to the Map; the numeric id numbers
  * won't appear in the list of keys for the Map.
  *
  * EXAMPLE:
  * MyDBObject.get("field_x") will return the DBField with the label
  * of "field_x".
  */
  
  public boolean has_key(Object key)
  {
    return keys().contains(key);
  }

  public List items()
  {
    List list = new ArrayList();
    DBField field;
    Object[] tuple;
    
    for (Iterator iter = getFieldVect().iterator(); iter.hasNext();)
      {
        field = (DBField) iter.next();
        tuple = new Object[2];
        tuple[0] = field.getName();
        tuple[1] = field;
        list.add(tuple);
      }
    
    return list;    
  }

  public Set keys()
  {
    Set keys = new HashSet();
    DBField field;
    for (Iterator iter = getFieldVect().iterator(); iter.hasNext();)
      {
        field = (DBField) iter.next();
        keys.add(field.getName());
      }
    return keys;
  }
  
  public boolean containsKey(Object key)
  {
    return has_key(key);
  }

  public boolean containsValue(Object value)
  {
    return getFieldVect().contains(value);
  }

  public Set entrySet()
  {
    Set entrySet = new HashSet();
    DBField field;
    for (Iterator iter = getFieldVect().iterator(); iter.hasNext();)
      {
        field = (DBField) iter.next();
        entrySet.add(new Entry(field));
      }
    return entrySet;
  }

  public Object get(Object key)
  {
    if (key instanceof PyInteger)
      {
        PyInteger pi = (PyInteger) key;
        return (DBField) getField(new Integer(pi.getValue()).shortValue());
      }
    else if (key instanceof Integer)
      {
        return (DBField) getField(((Integer) key).shortValue());
      }
    else if (key instanceof Short)
      {
        return (DBField) getField(((Short) key).shortValue());
      }
    else if (key instanceof String)
      {
        return (DBField) getField((String) key);
      }
    return null;
  }

  public boolean isEmpty()
  {
    return getFieldVect().isEmpty();
  }

  public Set keySet()
  {
    return keys();
  }

  public int size()
  {
    return getFieldVect().size();
  }

  public Collection values()
  {
    return getFieldVect();
  }

  static class Entry implements Map.Entry
  {
    Object key, value;
    
    public Entry( DBField obj )
    {
      key = obj.getName();
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
