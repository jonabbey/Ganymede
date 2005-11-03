/*
   GASH 2

   InvidDBField.java

   The GANYMEDE object storage system.

   Created: 2 July 1996
   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996-2005
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
import java.rmi.RemoteException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

import arlut.csd.JDialog.JDialogBuff;
import arlut.csd.JDialog.StringDialog; // for localized "Ok" and "Cancel"
import arlut.csd.Util.TranslationService;
import arlut.csd.Util.VectorUtils;
import arlut.csd.Util.XMLUtils;
import arlut.csd.ganymede.common.GanyPermissionsException;
import arlut.csd.ganymede.common.Invid;
import arlut.csd.ganymede.common.NotLoggedInException;
import arlut.csd.ganymede.common.ObjectStatus;
import arlut.csd.ganymede.common.QueryResult;
import arlut.csd.ganymede.common.ReturnVal;
import arlut.csd.ganymede.common.SchemaConstants;
import arlut.csd.ganymede.rmi.db_field;
import arlut.csd.ganymede.rmi.invid_field;

/*------------------------------------------------------------------------------
                                                                           class
                                                                    InvidDBField

------------------------------------------------------------------------------*/

/**
 * InvidDBField is a subclass of {@link arlut.csd.ganymede.server.DBField DBField}
 * for the storage and handling of {@link arlut.csd.ganymede.common.Invid Invid}
 * fields in the {@link arlut.csd.ganymede.server.DBStore DBStore} on the Ganymede
 * server.
 *
 * The Ganymede client talks to InvidDBFields through the
 * {@link arlut.csd.ganymede.rmi.invid_field invid_field} RMI interface. 
 *
 * This class implements one of the most fundamental pieces of logic in the
 * Ganymede server, the object pointer/object binding logic.  Whenever the
 * client calls setValue(), setElement(), addElement(), or deleteElement()
 * on an InvidDBField, the object being pointed to by the Invid being set
 * or cleared will be checked out for editing and the corresponding back
 * pointer will be set or cleared as appropriate.
 *
 * In other words, the InvidDBField logic guarantees that all objects
 * references in the server are symmetric.  If one object points to
 * another via an InvidDBField, the target of that pointer will point
 * back, either through a field explicitly specified in the schema, or
 * through the server's in-memory {@link arlut.csd.ganymede.server.DBStore#backPointers backPointers}
 * hash structure.
 *
 * @version $Id$
 * @author Jonathan Abbey, jonabbey@arlut.utexas.edu, ARL:UT
 */

public final class InvidDBField extends DBField implements invid_field {

  static final boolean debug = false;

  /**
   * TranslationService object for handling string localization in
   * the Ganymede server.
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.server.InvidDBField");

  // ---


  /**
   * We'll cache the choiceList from our parent in case we're doing
   * a large vector add/delete.  Any time we change our value/values
   * actually contained in this field, we'll null this out.
   *
   * Note that having this here costs us 4 bytes RAM for every InvidDBField
   * held in the Ganymede server's database, but without it we'll have
   * an extraordinarily painful time doing mass adds/deletes.
   */

  private QueryResult qr = null;

  /**
   * Receive constructor.  Used to create a InvidDBField from a
   * {@link arlut.csd.ganymede.server.DBStore DBStore}/{@link arlut.csd.ganymede.server.DBJournal DBJournal}
   * DataInput stream.
   */

  InvidDBField(DBObject owner, DataInput in, DBObjectBaseField definition) throws IOException
  {
    super();			// may throw RemoteException

    value = null;
    this.owner = owner;
    this.fieldcode = definition.getID();
    receive(in, definition);
  }

  /**
   * No-value constructor.  Allows the construction of a
   * 'non-initialized' field, for use where the 
   * {@link arlut.csd.ganymede.server.DBObjectBase DBObjectBase}
   * definition indicates that a given field may be present,
   * but for which no value has been stored in the 
   * {@link arlut.csd.ganymede.server.DBStore DBStore}.
   *
   * Used to provide the client a template for 'creating' this
   * field if so desired.
   */

  InvidDBField(DBObject owner, DBObjectBaseField definition)
  {
    this.owner = owner;
    this.fieldcode = definition.getID();
    
    if (isVector())
      {
	value = new Vector();
      }
    else
      {
	value = null;
      }
  }

  /**
   *
   * Copy constructor.
   *
   */

  public InvidDBField(DBObject owner, InvidDBField field)
  {
    this.owner = owner;
    this.fieldcode = field.getID();
    
    if (isVector())
      {
	value = (Vector) field.getVectVal().clone();
      }
    else
      {
	value = field.value;
      }
  }

  /**
   *
   * Scalar value constructor.
   *
   */

  public InvidDBField(DBObject owner, Invid value, DBObjectBaseField definition)
  {
    this.owner = owner;
    this.fieldcode = definition.getID();

    if (definition.isArray())
      {
	// "scalar value constructor called on vector field {0} in object {1}"
	throw new IllegalArgumentException(ts.l("init.type_mismatch", getName(), owner.getLabel()));
      }

    this.value = value;
  }

  /**
   *
   * Vector value constructor.
   *
   */

  public InvidDBField(DBObject owner, Vector values, DBObjectBaseField definition)
  {
    this.owner = owner;
    this.fieldcode = definition.getID();

    if (!definition.isArray())
      {
	// "vector value constructor called on scalar field {0} in object {1}"
	throw new IllegalArgumentException(ts.l("init.type_mismatch2", getName(), owner.getLabel()));
      }
    
    if (values == null)
      {
	value = new Vector();
      }
    else
      {
	value = values.clone();
      }
  }

  /**
   * This method is used to return a copy of this field, with the field's owner
   * set to newOwner.
   */

  public DBField getCopy(DBObject newOwner)
  {
    return new InvidDBField(newOwner, this);
  }
  
  public Object clone()
  {
    return new InvidDBField(owner, this);
  }

  /**
   *
   * This method is used to write the contents of this field to the
   * Ganymede.db file and/or to the Journal file.
   *
   */

  void emit(DataOutput out) throws IOException
  {
    Invid temp;

    /* -- */

    if (isVector())
      {
	Vector values = getVectVal();

	out.writeInt(values.size());

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

  /**
   *
   * This method is used to read the contents of this field from the
   * Ganymede.db file and/or from the Journal file.
   *
   */

  void receive(DataInput in, DBObjectBaseField definition) throws IOException
  {
    Invid temp;
    int count;

    /* -- */

    if (definition.isArray())
      {
	//	System.err.println("Reading InvidDBField: " + getName());

	if (Ganymede.db.isLessThan(2,3))
	  {
	    count = in.readShort();
	  }
	else
	  {
	    count = in.readInt();
	  }

	//	System.err.println(count + " values");

	if (count > 0)
	  {
	    value = new Vector(count);

	    // a cast of convenience..

	    Vector v = (Vector) value;

	    for (int i = 0; i < count; i++)
	      {
		temp = Invid.createInvid(in.readShort(), in.readInt());
		v.addElement(temp);
	      }
	  }
	else
	  {
	    value = new Vector();
	  }
      }
    else
      {
	value = Invid.createInvid(in.readShort(), in.readInt());
      }
  }

  /**
   * This method is used when the database is being dumped, to write
   * out this field to disk.  It is mated with receiveXML().
   */

  synchronized void emitXML(XMLDumpContext xmlOut) throws IOException
  {
    // if we're the containing object field in an embedded object, we
    // don't need to describe ourselves to the XML file.. our
    // ownership will be implicitly recorded in the structure of the
    // XML file

    if (getOwner().isEmbedded() && getID() == 0 && !xmlOut.isDeltaSyncing())
      {
	return;
      }

    // if we are an edit-in-place field, we won't write out an invid
    // reference for our target, but instead we will actually write
    // out the object in-place.

    boolean asEmbedded = isEditInPlace() && !xmlOut.isDeltaSyncing();

    xmlOut.startElementIndent(this.getXMLName());

    if (!isVector())
      {
	emitInvidXML(xmlOut, value(), asEmbedded);
      }
    else
      {
	Vector values = getVectVal();

	for (int i = 0; i < values.size(); i++)
	  {
	    if (!asEmbedded)
	      {
		xmlOut.indentOut();
		xmlOut.indent();
		xmlOut.indentIn();
	      }

	    emitInvidXML(xmlOut, (Invid) values.elementAt(i), asEmbedded);
	  }

	xmlOut.indent();
      }

    xmlOut.endElement(this.getXMLName());
  }

  /**
   * This method writes out an Invid in XML form to a Ganymede
   * XML data dump stream.
   *
   * Whenever Ganymede writes out an Invid to an XML data dump, it
   * uses an &lt;invid&gt; element with two attributes, type and
   * id.  type is the name of the object type that the invid points
   * to, and id is an identifying label for the target object.
   *
   * When it can, emitInvidXML() will use a human-readable label
   * for the id attribute.  This can only be done, however, in those
   * cases where the object in question has a designated label field
   * and in which that label field is guaranteed to have a unique
   * value through the use of a DBNameSpace.  If emitInvidXML() cannot
   * guarantee that the label will be unique, it will write out the
   * target object's type-specific object number
   *
   * If the target invid has a unique label, the label of the
   * object will be written out in the 'id' attribute of the
   * invid element.  If not, the 'id' attribute will be omitted and
   * the target element will be identified by its numeric object id,
   * using the 'num' attribute.
   *
   * All this is a bit different if this InvidDBField is an
   * edit-in-place field.  In that case, emitInvidXML will simply
   * write out the embedded object, in place of an invid element. 
   */

  public void emitInvidXML(XMLDumpContext xmlOut, Invid invid, 
			   boolean asEmbedded) throws IOException
  {
    DBObject target = null;
    DBField targetField;

    /* -- */

    target = getOwner().lookupInvid(invid, xmlOut.isBeforeStateDumping());

    if (target == null)
      {
	throw new IllegalArgumentException(ts.l("emitInvidXML.bad_invid", this.toString(), invid));
      }

    if (asEmbedded)
      {
	target.emitXML(xmlOut);
      }
    else
      {
	xmlOut.startElement("invid");
	xmlOut.attribute("type", XMLUtils.XMLEncode(target.getTypeName()));
	xmlOut.attribute("id", target.getLabel()); // getLabel() gives us the XML-friendly label

	if (xmlOut.isSyncing())
	  {
	    xmlOut.attribute("oid", invid.toString());

	    //
	    // first get any extra Invid element attributes from the
	    // object that we are writing out
	    //

	    DBEditObject hook = getOwner().getBase().getObjectHook();
	    String extras[] = hook.getForeignSyncKeys(invid, getOwner(),
						      target, xmlOut.getSyncChannelName(),
						      xmlOut.isBeforeStateDumping());

	    if (extras != null && extras.length > 0)
	      {
		if (extras.length % 2 != 0)
		  {
		    // "InvidDBField.emitInvidXML(): mismatched attribute/value pairs returned from getForeignSyncKeys() call on {0}"
		    throw new RuntimeException(ts.l("emitInvidXML.bad_foreign_keys", getOwner().toString()));
		  }

		for (int i = 0; i < extras.length; i = i + 2)
		  {
		    String name = extras[i];
		    String value = extras[i+1];

		    if (name.equals("id") || name.equals("num") || name.equals("type") || name.equals("oid"))
		      {
			// "InvidDBField.emitInvidXML(): improper use of a reserved attribute name in attribute/value pairs returned from getForeignSyncKeys call on {0}"
			throw new RuntimeException(ts.l("emitInvidXML.bad_attribute_name", getOwner().toString()));
		      }

		    xmlOut.attribute(name, value);
		  }
	      }

	    //
	    // now get any extra Invid element attributes from the
	    // object that we are targeting
	    //

	    hook = target.getBase().getObjectHook();
	    extras = hook.getMyExtraInvidAttributes(target,
						    xmlOut.getSyncChannelName(),
						    xmlOut.isBeforeStateDumping());

	    if (extras != null && extras.length > 0)
	      {
		if (extras.length % 2 != 0)
		  {
		    // "InvidDBField.emitInvidXML(): mismatched attribute/value pairs returned from getForeignSyncKeys() call on {0}"
		    throw new RuntimeException(ts.l("emitInvidXML.bad_foreign_keys", getOwner().toString()));
		  }

		for (int i = 0; i < extras.length; i = i + 2)
		  {
		    String name = extras[i];
		    String value = extras[i+1];

		    if (name.equals("id") || name.equals("num") || name.equals("type") || name.equals("oid"))
		      {
			// "InvidDBField.emitInvidXML(): improper use of a reserved attribute name in attribute/value pairs returned from getForeignSyncKeys call on {0}"
			throw new RuntimeException(ts.l("emitInvidXML.bad_attribute_name", getOwner().toString()));
		      }

		    xmlOut.attribute(name, value);
		  }
	      }
	  }

	xmlOut.endElement("invid");
      }
  }

  /**
   * This method is intended to be called when this field is being checked into
   * the database.  Subclasses of DBField will override this method to clean up
   * data that is cached for speed during editing.
   */

  public void cleanup()
  {
    qr = null;

    super.cleanup();
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
	// "scalar accessor called on vector field {0} in object {1}"
	throw new IllegalArgumentException(ts.l("value.type_mismatch", getName(), owner.getLabel()));
      }

    return (Invid) value;
  }

  public Invid value(int index)
  {
    if (!isVector())
      {
	// "vector accessor called on scalar field {0} in object {1}"
	throw new IllegalArgumentException(ts.l("value.type_mismatch2", getName(), owner.getLabel()));
      }

    return (Invid) getVectVal().elementAt(index);
  }

  /**
   * This method returns a text encoded value for this InvidDBField
   * without checking permissions.
   *
   * This method avoids checking permissions because it is used on
   * the server side only and because it is involved in the 
   * {@link arlut.csd.ganymede.server.DBObject#getLabel() getLabel()}
   * logic for {@link arlut.csd.ganymede.server.DBObject DBObject}, 
   * which is invoked from {@link arlut.csd.ganymede.server.GanymedeSession GanymedeSession}'s
   * {@link arlut.csd.ganymede.server.GanymedeSession#getPerm(arlut.csd.ganymede.server.DBObject) getPerm()} 
   * method.
   *
   * If this method checked permissions and the getPerm() method
   * failed for some reason and tried to report the failure using
   * object.getLabel(), as it does at present, the server could get
   * into an infinite loop.
   */

  public synchronized String getValueString()
  {
    GanymedeSession gsession = null;

    /* -- */

    // where will we go to look up the label for our target(s)?

    gsession = owner.getGSession();

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

	// XXX note: we don't use our owner's lookupLabel() method
	// for scalar invid values.. 

	return getRemoteLabel(gsession, localInvid, false);
      }
    else
      {
	int size = size();

	if (size == 0)
	  {
	    return "";
	  }

	String entries[] = new String[size];
	Invid tmp;

	for (int i = 0; i < size; i++)
	  {
	    tmp = this.value(i);

	    entries[i] = getRemoteLabel(gsession, tmp, false);
	  }

	java.util.Arrays.sort(entries, null);

	StringBuffer result = new StringBuffer();

	for (int i = 0; i < entries.length; i++)
	  {
	    if (i > 0)
	      {
		result.append(",");
	      }

	    result.append(entries[i]);
	  }

	return result.toString();
      }
  }

  /**
   * This method returns the label of an object referenced by an
   * invid held in this field.  If the remote object referenced by the
   * invid argument is currently being deleted, we'll try to get the
   * label from the state of that object as it existed at the start of
   * the current transaction.  This is to allow us to do proper
   * logging of the values deleted from this field in the case of the
   * string generated by {@link arlut.csd.ganymede.server.DBEditObject#diff()
   * DBEditObject.diff()} during transaction logging.
   *
   * If forceOriginal is set to true, getRemoteLabel will always
   * try to retrieve the remote object's original label, even if the
   * remote object has not been deleted by the active transaction.
   */

  private String getRemoteLabel(GanymedeSession gsession, Invid invid, boolean forceOriginal)
  {
    if (gsession != null)
      {
	/*
	 * okay.. if we are finding the name of the referenced field in
	 * the DBEditSet logging context, our reference might have already
	 * had its fields cleared out.. we want to be able to get access
	 * to the label it had for the purpose of logging this transaction..
	 *
	 * the DBEditSet commit() logic uses DBEditObject.diff(), which
	 * will call us to get the original value of a invid field (perhaps
	 * before the current version of this field was cleared.. we need
	 * to also be able to present the name of the remote object before
	 * it was cleared..
	 */

	DBObject objectRef = gsession.session.viewDBObject(invid);

	if (objectRef != null && (objectRef instanceof DBEditObject))
	  {
	    DBEditObject eObjectRef = (DBEditObject) objectRef;

	    if (forceOriginal || eObjectRef.getStatus() == ObjectStatus.DELETING)
	      {
		objectRef = eObjectRef.getOriginal();
	      }
	  }

	if (objectRef == null)
	  {
	    // "*** no target for invid {0} ***"
	    return ts.l("getRemoteLabel.nonesuch", invid.toString());
	  }

	if (objectRef.isEmbedded())
	  {
	    return objectRef.getEmbeddedObjectDisplayLabel();
	  }
	else
	  {
	    return objectRef.getLabel();
	  }
      }
    else
      {
	if (!isVector())
	  {
	    return this.value().toString();
	  }
	else
	  {
	    return VectorUtils.vectorString(this.getVectVal());
	  }
      }
  }

  /** 
   * OK, this is a bit vague.. getEncodingString() is used by the
   * new dump system to allow all fields to be properly sorted in the
   * client's query result table.. a real reversible encoding of an
   * invid field would *not* be the getValueString() results, but
   * getValueString() is what we want in the dump result table, so
   * we'll do that here for now. 
   */

  public String getEncodingString()
  {
    return getValueString();
  }

  /**
   * Returns a String representing the change in value between this
   * field and orig.  This String is intended for logging and email,
   * not for any sort of programmatic activity.  The format of the
   * generated string is not defined, but is intended to be suitable
   * for inclusion in a log entry and in an email message.
   *
   * If there is no change in the field, null will be returned.
   */

  public synchronized String getDiffString(DBField orig)
  {
    StringBuffer result = new StringBuffer();
    InvidDBField origI;
    GanymedeSession gsession = null;

    /* -- */

    if (!(orig instanceof InvidDBField))
      {
	// "Bad field comparison {0}"
	throw new IllegalArgumentException(ts.l("getDiffString.badtype", getName()));
      }

    if (orig == this)
      {
	return "";
      }

    origI = (InvidDBField) orig;

    gsession = owner.getGSession();
    
    if (gsession == null)
      {
	gsession = Ganymede.internalSession;
      }

    if (isVector())
      {
	Vector 
	  added = new Vector(),
	  deleted = new Vector();

	Enumeration en;

	Object 
	  element = null;

	Invid
	  elementA = null;

	boolean found = false;

	Vector values = getVectVal();
	Vector origValues = origI.getVectVal();

	/* -- */

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

	en = origValues.elements();

	while (en.hasMoreElements())
	  {
	    element = en.nextElement();

	    if (currentElements.get(element) == null)
	      {
		deleted.addElement(element);
	      }
	  }

	// find elements in our present field that aren't in the orig field

	Hashtable origElements = new Hashtable();

	for (int i = 0; !found && i < origValues.size(); i++)
	  {
	    element = origValues.elementAt(i);
	    
	    origElements.put(element, element);
	  }

	en = values.elements();

	while (en.hasMoreElements())
	  {
	    element = en.nextElement();

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
		StringBuffer deleteString = new StringBuffer();
	    
		for (int i = 0; i < deleted.size(); i++)
		  {
		    elementA = (Invid) deleted.elementAt(i);

		    if (i > 0)
		      {
			deleteString.append(", ");
		      }

		    deleteString.append(getRemoteLabel(gsession, elementA, true)); // get original if edited
		  }

		// "\tDeleted: {0}\n"
		result.append(ts.l("getDiffString.deleted_items", deleteString.toString()));
	      }

	    if (added.size() != 0)
	      {
		StringBuffer addString = new StringBuffer();

		for (int i = 0; i < added.size(); i++)
		  {
		    elementA = (Invid) added.elementAt(i);

		    if (i > 0)
		      {
			addString.append(", ");
		      }

		    addString.append(getRemoteLabel(gsession, elementA, false));
		  }

		// "\tAdded: {0}\n"
		result.append(ts.l("getDiffString.added_items", addString.toString()));
	      }

	    return result.toString();
	  }
      }
    else
      {
	if (origI.value().equals(this.value()))
	  {
	    return null;
	  }
	else
	  {
	    // "\tOld: {0}\n\tNew:{1}\n"
	    result.append(ts.l("getDiffString.scalar", 
			       getRemoteLabel(gsession, origI.value(), true),
			       getRemoteLabel(gsession, this.value(), false)));

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
   * This private helper method attempts to verify that a
   * prospective bind operation in an vector add context can succeed
   * without forcing an unbinding on a scalar remote field.
   *
   * This method <b>only</b> checks to see if we're trying to bind
   * to an already bound scalar InvidDBField.  If there are any other
   * schema problems that would cause a bind to fail, this method will
   * return a null (success) ReturnVal, trusting the later bind attempt
   * to fail and produce an informative message.
   *
   * @return null on 'no problems' or 'a problem that bind will
   * detect', and a non-null ReturnVal with a dialog encoded if there
   * is a scalar conflict in place.
   */

  private final ReturnVal checkBindConflict(Invid newRemote)
  {
    short targetField;
    DBObject remobj;
    InvidDBField remoteField;
    DBEditObject myParent;
    DBSession mySession;

    /* -- */

    if (!getFieldDef().isSymmetric())
      {
	return null;
      }

    myParent = (DBEditObject) this.owner;
    mySession = myParent.getSession();

    targetField = getFieldDef().getTargetField();

    remobj = mySession.viewDBObject(newRemote);

    if (remobj == null)
      {
	return null;		// failure that bind will catch
      }

    try
      {
	remoteField = (InvidDBField) remobj.getField(targetField);
      }
    catch (ClassCastException ex)
      {
	return null;
      }

    if (remoteField == null)
      {
	// the target is either non-edited and doesn't contain the
	// field in question, or it's been checked out for editing and
	// the field in question just isn't defined.  it's either not
	// a problem because there's no conflicting bind, or the
	// schema is screwed up.  Not for us to worry about either way.

	return null;
      }

    if (!remoteField.isVector() && remoteField.isDefined())
      {
	Invid myInvid = (Invid) remoteField.getValueLocal();

	if (myInvid.equals(myParent.getInvid()))
	  {
	    return null;	// rebinding self.. bind() will catch this
	  }

	// this is the case we care about

	// "Link Error"
	// "Your operation could not be performed.  The target object {0} can only be linked to one {1} at a time."

	return Ganymede.createErrorDialog(ts.l("checkBindConflict.subj"),
					  ts.l("checkBindConflict.overlink", 
					       remobj.getLabel(),
					       this.getOwner().getTypeName()));
      }

    return null;
  }

  /**
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
   * This method should only be called from synchronized methods within
   * InvidDBField.
   *
   * <b>This method is private, and is not to be called by any code outside
   * of this class.</b>
   *
   * @param oldRemote the old invid to be replaced
   * @param newRemote the new invid to be linked
   * @param local if true, this operation will be performed without regard
   * to permissions limitations.
   *
   * @return null on success, or a ReturnVal with an error dialog encoded on failure
   *
   * @see arlut.csd.ganymede.server.InvidDBField#unbind(arlut.csd.ganymede.common.Invid, boolean)
   */

  private final ReturnVal bind(Invid oldRemote, Invid newRemote, boolean local)
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

    ReturnVal 
      retVal = null,
      newRetVal;

    DBObject remobj = null;

    /* -- */

    if (newRemote == null)
      {
	// "Null newRemote {0} in object {1}"
	throw new IllegalArgumentException(ts.l("bind.noremote", getName(), owner.getLabel()));
      }

    if (!isEditable(local))
      {
	// "Not an editable invid field: {0} in object {1}"
	throw new IllegalArgumentException(ts.l("bind.noteditable", getName(), owner.getLabel()));
      }

    eObj = (DBEditObject) this.owner;
    session = eObj.getSession();

    if ((oldRemote != null) && oldRemote.equals(newRemote))
      {
	return null;		// success
      }

    // find out whether there is a symmetric field pointing back to
    // us, or whether we will need to use the backPointers hashing
    // structure.

    if (getFieldDef().isSymmetric())
      {
	// find out what field in remote we might need to update

	targetField = getFieldDef().getTargetField();
      }
    else
      {
	// the containerField in an embedded object is a special case.
	// the containerField is generic across all embedded objects
	// and doesn't contain the data we need to connect this field
	// with the embedded object field in our container object.

	// because of this, we won't attempt to try to do a bind here.
	// instead, this binding will get done by the InvidDBField
	// createNewEmbedded() method, which handles all the details

	if (owner.objectBase.isEmbedded() && getID() == SchemaConstants.ContainerField)
	  {
	    return null;	// done!
	  }

	// With an asymmetric field, we don't actually ever touch the
	// target object(s).  Instead, we depend on the DBEditSet
	// commit logic doing an update of the DBStore backPointers
	// hash structure at the right time.

	// We do need to make sure that we're not trying to link to an
	// object that is in the middle of being deleted, so we check
	// that here.  If deleteLockObject() returns true, the system
	// will prevent the target object from being deleted until our
	// transaction has cleared.

	// We don't have to worry about oldRemote (if it is indeed not
	// null), as the DBEditSet marked it as non-deletable when we
	// added the owner of this field to the transaction.  This is
	// done in DBEditSet.addObject().  We don't want to clear the
	// delete lock our transaction has on it, because we have to
	// be able to revert the link to oldRemote if the transaction
	// is cancelled.

	if (DBDeletionManager.deleteLockObject(session.viewDBObject(newRemote), session))
	  {
	    return null;
	  }
	else
	  {
	    // "Bind link error"
	    // "Can't forge an asymmetric link between {0} and invid {1}, the target object is being deleted."
	    return Ganymede.createErrorDialog(ts.l("bind.deletedremote_sub"),
					      ts.l("bind.deletedremote_text", this.getName(), newRemote.toString()));
	  }
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

	remobj = session.viewDBObject(oldRemote);

	if (remobj == null)
	  {
	    // "InvidDBField.bind(): Couldn''t find old reference"
	    // "Your operation could not succeed because field {0} was linked to a remote reference {1} that could not be found \
	    //  for unlinking.\n\nThis is a serious logic error in the server."
	    return Ganymede.createErrorDialog(ts.l("bind.no_oldref"),
					      ts.l("bind.no_oldref_text", getName(), oldRef.toString()));
	  }

	// see if we are allowed to unlink the remote object without
	// having permission to edit it generally.

	anonymous = session.getObjectHook(oldRemote).anonymousUnlinkOK(remobj,
								       targetField,
								       this.getOwner(),
								       this.getID(),
								       session.getGSession());

	// if we're already editing it, just go with that.

	if (remobj instanceof DBEditObject)
	  {
	    oldRef = (DBEditObject) remobj;
	  }
	else
	  {
	    if (anonymous || session.getGSession().getPerm(remobj).isEditable())
	      {
		oldRef = (DBEditObject) session.editDBObject(oldRemote);
	      }
	    else
	      {
		// "InvidDBField.bind(): Couldn''t unlink from old reference"
		// "Your operation could not succeed because you don''t have permission to dissolve the link to the old object \
		// held in field {0} in object {1}"
		return Ganymede.createErrorDialog(ts.l("bind.no_unlink_sub"),
						  ts.l("bind.no_perms_old", getName(), owner.getLabel()));
	      }
	  }

	// if we couldn't check edit the old object, we need to see why.

	if (oldRef == null)
	  {
	    // this check is not truly thread safe, as the
	    // shadowObject might be cleared by another thread while
	    // we're working..  this isn't a grave risk, but we'll
	    // wrap all of this in a NullPointerException catch just
	    // in case.  this check is just for informative purposes,
	    // so we don't mind throwing a null pointer exception in
	    // here, it's not worth doing all of the careful sync work
	    // to lock down this stuff without risk of deadlock

	    try
	      {
		String edit_username, edit_hostname;
		DBEditObject editing = remobj.shadowObject;

		if (editing != null)
		  {
		    if (editing.gSession != null)
		      {
			edit_username = editing.gSession.getMyUserName();
			edit_hostname = editing.gSession.clienthost;

			// "InvidDBField.bind(): Couldn''t unlink from old reference"
			// "Field {0} could not be unlinked from the {1} {2} object, which is busy being edited by {3} on system {4}"
			return Ganymede.createErrorDialog(ts.l("bind.no_unlink_sub"),
							  ts.l("bind.busy_old",
							       this.getName(), remobj.getLabel(), remobj.getTypeName(),
							       edit_username, edit_hostname));
		      }
		
		    // "InvidDBField.bind(): Couldn''t unlink from old reference"
		    // "Field {0} could not be unlinked from the {1} {2} object, which is busy being edited by another user."
		    return Ganymede.createErrorDialog(ts.l("bind.no_unlink_sub"),
						      ts.l("bind.busy_old2",
							   this.getName(), remobj.getLabel(), remobj.getTypeName()));
		  }
		else
		  {
		    // "InvidDBField.bind(): Couldn''t unlink from old reference"
		    // "Field {0} could not be unlinked from the {1} {2} object. 
		    // This is probably a temporary condition due to other user activity on the Ganymede server."
		    return Ganymede.createErrorDialog(ts.l("bind.no_unlink_sub"),
						      ts.l("bind.busy_old_temp",
							   this.getName(), remobj.getLabel(), remobj.getTypeName()));
		  }
	      }
	    catch (NullPointerException ex)
	      {
		// "InvidDBField.bind(): Couldn''t unlink from old reference"
		// "Field {0} could not be unlinked from the {1} {2} object. 
		// This is probably a temporary condition due to other user activity on the Ganymede server."
		return Ganymede.createErrorDialog(ts.l("bind.no_unlink_sub"),
						  ts.l("bind.busy_old_temp",
						       this.getName(), remobj.getLabel(), remobj.getTypeName()));
	      }
	  }

	try
	  {
	    oldRefField = (InvidDBField) oldRef.getField(targetField);
	  }
	catch (ClassCastException ex)
	  {
	    try
	      {
		// "InvidDBField.bind(): Couldn''t unlink from old reference"
		// "Your operation could not succeed due to an error in the server''s schema.  Target field {0} in object {1} is not an invid field."
		return Ganymede.createErrorDialog(ts.l("bind.no_unlink_sub"),
						  ts.l("bind.schema_error",
						       oldRef.getField(targetField).getName(),
						       oldRef.getLabel()));
	      }
	    catch (RemoteException rx)
	      {
		// "InvidDBField.bind(): Couldn''t unlink from old reference"
		// "Your operation could not succeed due to an error in the server''s schema.  Target field {0} in object {1} is not an invid field."
		return Ganymede.createErrorDialog(ts.l("bind.no_unlink_sub"),
						  ts.l("bind.schema_error",
						       Integer.toString(targetField), oldRef.getLabel()));
	      }
	  }
	
	if (oldRefField == null)
	  {
	    // editDBObject() will create undefined fields for all
	    // fields defined in the DBObjectBase as long as the user
	    // had permission to create those fields, so if we got a
	    // null result we either have a schema corruption problem
	    // or a permission to create field problem

	    DBObjectBaseField fieldDef = oldRef.getFieldDef(targetField);

	    if (fieldDef == null)
	      {
		// "InvidDBField.bind(): Couldn''t unlink from old reference"
		// "Your operation could not succeed due to a possible inconsistency in the server database.  Target field number {0} in object {1} does not exist."
		return Ganymede.createErrorDialog(ts.l("bind.no_unlink_sub"),
						  ts.l("bind.inconsistency", Integer.toString(targetField), oldRef.getLabel()));
	      }
	    else
	      {
		// "InvidDBField.bind(): Couldn''t unlink from old reference"
		// "Your operation could not succeed due to a possible inconsistency in the server database.  Target field {0} is undefined in object {1}."
		return Ganymede.createErrorDialog(ts.l("bind.no_unlink_sub"),
						  ts.l("bind.inconsistency2", fieldDef.getName(), oldRef.getLabel()));
	      }
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

    remobj = session.viewDBObject(newRemote);
    
    if (remobj == null)
      {
	// "InvidDBField.bind(): Couldn''t find new reference"
	// "Your operation could not succeed because field {0} cannot link to non-existent invid {1}.\n\nThis is a serious logic error in the server."	
	return Ganymede.createErrorDialog(ts.l("bind.no_newref_sub"),
					  ts.l("bind.no_newref", getName(), newRemote.toString()));
      }

    // see if we are allowed to link the remote object without having
    // permission to edit it generally.
    
    anonymous = session.getObjectHook(newRemote).anonymousLinkOK(remobj,
								 targetField, 
								 this.getOwner(),
								 this.getID(),
								 session.getGSession());
    // if we're already editing it, just go with that.

    if (remobj instanceof DBEditObject)
      {
	newRef = (DBEditObject) remobj;

	// if the object is being deleted or dropped, don't allow the link

	if (newRef.getStatus() == ObjectStatus.DELETING || newRef.getStatus() == ObjectStatus.DROPPING)
	  {
	    // "InvidDBField.bind(): Couldn''t link to remote object"
	    // "Field {0} cannot be linked to remote object {1}.\n\nThe remote object has been deleted."
	    return Ganymede.createErrorDialog(ts.l("bind.no_new_link_sub"),
					      ts.l("bind.deleted_new", this.getName(), newRemote.toString()));
	  }
      }
    else
      {
	if (anonymous || session.getGSession().getPerm(remobj).isEditable())
	  {
	    newRef = (DBEditObject) session.editDBObject(newRemote);
	  }
	else
	  {
	    // "InvidDBField.bind(): Couldn''t link to remote object"
	    // "Field {0} could not be linked to the {1} {2} object.  You do not have permission to edit the {1} {2} object."
	    return Ganymede.createErrorDialog(ts.l("bind.no_new_link_sub"),
					      ts.l("bind.no_newref_perm",
						   this.getName(), remobj.getLabel(), remobj.getTypeName()));
	  }
      }
    
    // if we couldn't check out the object, we need to see why.

    if (newRef == null)
      {
	// this section is not truly thread safe, as the shadowObject
	// might be cleared by another thread while we're working..
	// this isn't a grave risk, but we'll wrap all of this in a
	// NullPointerException catch just in case.  this clause is
	// just for informative purposes, so we don't mind throwing a
	// null pointer exception in here. It's not worth doing all of
	// the careful sync work to lock down this stuff without risk
	// of deadlock
	
	try
	  {
	    String edit_username, edit_hostname;
	    DBEditObject editing = remobj.shadowObject;

	    if (editing != null)
	      {
		if (editing.gSession != null)
		  {
		    edit_username = editing.gSession.getMyUserName();
		    edit_hostname = editing.gSession.clienthost;
		    
		    // "InvidDBField.bind(): Couldn''t link to new reference"
		    // "Field {0} could not be linked to the {1} {2} object, which is busy being edited by {3} on system {4}."
		    return Ganymede.createErrorDialog(ts.l("bind.no_new_link_sub"),
						      ts.l("bind.busy_new", this.getName(), remobj.getLabel(), remobj.getTypeName(),
							   edit_username, edit_hostname));
		  }
		
		// "InvidDBField.bind(): Couldn''t link to new reference"
		// "Field {0} could not be linked to the {1} {2} object, which is busy being edited by another user."
		return Ganymede.createErrorDialog(ts.l("bind.no_new_link_sub"),
						  ts.l("bind.busy_new2", this.getName(), remobj.getLabel(), remobj.getTypeName()));
	      }
	    else
	      {
		// "InvidDBField.bind(): Couldn''t link to new reference"
		// "Field {0} could not be linked to the {1} {2} object.  
		// This is probably a temporary condition due to other user activity on the Ganymede server."
		return Ganymede.createErrorDialog(ts.l("bind.no_new_link_sub"),
						  ts.l("bind.busy_new_temp", this.getName(), remobj.getLabel(), remobj.getTypeName()));
	      }
	  }
	catch (NullPointerException ex)
	  {
	    // "InvidDBField.bind(): Couldn''t link to new reference"
	    // "Field {0} could not be linked to the {1} {2} object.  
	    // This is probably a temporary condition due to other user activity on the Ganymede server."
	    return Ganymede.createErrorDialog(ts.l("bind.no_new_link_sub"),
					      ts.l("bind.busy_new_temp", this.getName(), remobj.getLabel(), remobj.getTypeName()));
	  }
      }

    try
      {
	newRefField = (InvidDBField) newRef.getField(targetField);
      }
    catch (ClassCastException ex)
      {
	try
	  {
	    // "InvidDBField.bind(): Couldn''t link to new reference"
	    // "Your operation could not succeed due to an error in the server''s schema.  Target field {0} in object {1} is not an invid field."
	    return Ganymede.createErrorDialog(ts.l("bind.no_new_link_sub"),
					      ts.l("bind.schema_error", newRef.getField(targetField).getName(), newRef.getLabel()));
	  }
	catch (RemoteException rx)
	  {
	    // "InvidDBField.bind(): Couldn''t link to new reference"
	    // "Your operation could not succeed due to an error in the server''s schema.  Target field {0} in object {1} is not an invid field."
	    return Ganymede.createErrorDialog(ts.l("bind.no_new_link_sub"),
					      ts.l("bind.schema_error", Integer.toString(targetField), newRef.getLabel()));
	  }
      }
    
    if (newRefField == null)
      {
	// editDBObject() will create undefined fields for all
	// fields defined in the DBObjectBase as long as the user
	// had permission to create those fields, so if we got a
	// null result we either have a schema corruption problem
	// or a permission to create field problem

	DBObjectBaseField fieldDef = newRef.getFieldDef(targetField);

	if (fieldDef == null)
	  {
	    // "InvidDBField.bind(): Couldn''t link to new reference"
	    // "Your operation could not succeed due to a possible inconsistency in the server database.  Target field number {0} in object {1} does not exist."
	    return Ganymede.createErrorDialog(ts.l("bind.no_new_link_sub"),
					      ts.l("bind.inconsistency", Integer.toString(targetField), newRef.getLabel()));
	  }
	else
	  {
	    // "InvidDBField.bind(): Couldn''t link to new reference"
	    // "Your operation could not succeed because you do not have permission to create the (previously undefined) {0} field in the {1} {2} object."
	    return Ganymede.createErrorDialog(ts.l("bind.no_new_link_sub"),
					      ts.l("bind.no_create_perm", fieldDef.getName(), newRef.getLabel(), newRef.getTypeName()));
	  }
      }

    // okay, at this point we should have oldRefField pointing to the
    // old target field, and newRefField pointing to the new target field.

    // Do our job.

    if (oldRefField != null)
      {
        retVal = oldRefField.dissolve(owner.getInvid(), (anonymous||local));

 	if (retVal != null && !retVal.didSucceed())
	  {
	    return retVal;
	  }
      }
    
    newRetVal = newRefField.establish(owner, (anonymous2||local));

    if (newRetVal != null && !newRetVal.didSucceed())
      {
	// oops!  try to undo what we did.. this probably isn't critical
	// because something above us will do a rollback, but it's polite.

	if (oldRefField != null)
	  {
	    oldRefField.establish(owner, (anonymous||local)); // hope this works
	  }
	
	return newRetVal;
      }

    if (retVal != null)
      {
	retVal.unionRescan(newRetVal);
      }
    else
      {
	retVal = newRetVal;
      }

    // tell the client that it needs to rescan both the old and new
    // remote ends of this binding

    newRetVal = new ReturnVal(true, true);

    if (oldRemote != null)
      {
	newRetVal.addRescanField(oldRemote, targetField);
      }

    newRetVal.addRescanField(newRemote, targetField);
    newRetVal.unionRescan(retVal);

    return newRetVal;		// success
  }

  /**
   * This method is used to unlink this field from the specified remote
   * invid in accordance with this field's defined symmetry constraints.
   *
   * <b>This method is private, and is not to be called by any code outside
   * of this class.</b>
   *
   * @param remote An invid for an object to be checked out and unlinked
   * @param local if true, this operation will be performed without regard
   * to permissions limitations.
   *
   * @return null on success, or a ReturnVal with an error dialog encoded on failure
   */

  private final ReturnVal unbind(Invid remote, boolean local)
  {
    short targetField;

    DBEditObject 
      eObj = null,
      oldRef = null;

    DBObject
      remobj;

    InvidDBField 
      oldRefField = null;

    DBSession
      session = null;

    ReturnVal
      retVal = null,
      newRetVal;

    boolean anonymous;

    /* -- */

    if (remote == null)
      {
	return null;

	// throw new IllegalArgumentException("null remote: " + getName() + " in object " + owner.getLabel());
      }

    if (!isEditable(local))
      {
	// "Not an editable invid field: {0} in object {1}"
	throw new IllegalArgumentException(ts.l("unbind.noteditable", getName(), owner.getLabel()));
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
	// if we are unbinding an asymmetric field, we do nothing.
	// the fact that we were asymmetrically linked to remote at
	// some point during this transaction is enough to insure that
	// the object pointed to by remote is delete locked for the
	// duration of this transaction.  If and when the transaction
	// commits, the remote object will have its delete lock
	// cleared, and later transactions will be able to delete that
	// object.  See DBEditSet.addObject() to see how this implicit
	// locking is done.

	return null;
      }

    // check to see if we have permission to anonymously unlink
    // this field from the target field, else go through the
    // GanymedeSession layer to have our permissions checked.

    // note that if the GanymedeSession layer has already checked out the
    // object, session.editDBObject() will return a reference to that
    // version, and we'll lose our security bypass.. for that reason,
    // we also use the anonymous variable to instruct dissolve to disregard
    // write permissions if we have gotten the anonymous OK

    remobj = session.viewDBObject(remote);
	
    if (remobj == null)
      {
	// "InvidDBField.unbind(): Couldn't find old reference"
	// "Your operation could not succeed because field {0} was linked to a remote reference {1} that could not be found 
	// for unlinking.\n\nThis is a serious logic error in the server."
	return Ganymede.createErrorDialog(ts.l("unbind.no_oldref"),
					  ts.l("bind.no_oldref_text",  getName(), remote.toString()));
      }

    // see if we are allowed to unlink the remote object without
    // having permission to edit it generally.

    anonymous = session.getObjectHook(remote).anonymousUnlinkOK(remobj,
								targetField, 
								this.getOwner(),
								this.getID(),
								session.getGSession());

    // if we're already editing it, just go with that.

    if (remobj instanceof DBEditObject)
      {
	oldRef = (DBEditObject) remobj;
      }
    else
      {
	if (anonymous || session.getGSession().getPerm(remobj).isEditable())
	  {
	    oldRef = (DBEditObject) session.editDBObject(remote);

	    // if we couldn't checkout the old object for editing, despite
	    // having permissions, we need to see why.

	    if (oldRef == null)
	      {
		// this check is not truly thread safe, as the
		// shadowObject might be cleared by another thread while
		// we're working..  this isn't a grave risk, but we'll
		// wrap all of this in a NullPointerException catch just
		// in case.  this check is just for informative purposes,
		// so we don't mind throwing a null pointer exception in
		// here, it's not worth doing all of the careful sync work
		// to lock down this stuff without risk of deadlock

		try
		  {
		    String edit_username, edit_hostname;
		    DBEditObject editing = remobj.shadowObject;

		    edit_username = editing.gSession.getMyUserName();
		    edit_hostname = editing.gSession.getClientHostName();
		    
		    // "InvidDBField.unbind(): Couldn''t unlink from old reference"
		    // "Field {0} could not be unlinked from the {1} {2} object, which is busy being edited by {3} on system {4}."
		    return Ganymede.createErrorDialog(ts.l("unbind.no_unlink_sub"),
						      ts.l("bind.busy_old", this.getName(), remobj.getLabel(), remobj.getTypeName(), edit_username, edit_hostname));
		  }
		catch (NullPointerException ex)
		  {
		    // "InvidDBField.unbind(): Couldn''t unlink from old reference"
		    // "Field {0} could not be unlinked from the {1} {2} object.  
		    // This is probably a temporary condition due to other user activity on the Ganymede server."
		    return Ganymede.createErrorDialog(ts.l("unbind.no_unlink_sub"),
						      ts.l("bind.busy_old_temp", this.getName(), remobj.getLabel(), remobj.getTypeName()));
		  }
	      }
	  }
	else
	  {
	    // it's there, but we don't have permission to unlink it
	    
	    // "InvidDBField.unbind(): Couldn''t unlink from old reference"
	    // "We couldn''t unlink field {0} in object {1} from field {2} in object {3} due to a permissions problem."
	    return Ganymede.createErrorDialog(ts.l("unbind.no_unlink_sub"),
					      ts.l("unbind.perm_fail",
						   getName(),
						   getOwner().getLabel(),
						   remobj.getFieldName(targetField), 
						   getRemoteLabel(session.getGSession(), remote, false)));
	  }
      }

    try
      {
	oldRefField = (InvidDBField) oldRef.getField(targetField);
      }
    catch (ClassCastException ex)
      {
	// "InvidDBField.unbind(): Couldn''t unlink from old reference"
	// "Your operation could not succeed due to an error in the server''s schema.  Target field {0} in object {1} is not an invid field."
	return Ganymede.createErrorDialog(ts.l("unbind.no_unlink_sub"),
					  ts.l("bind.schema_error", oldRef.getFieldName(targetField), oldRef.getLabel()));
      }

    if (oldRefField == null)
      {
	// editDBObject() will create undefined fields for all
	// fields defined in the DBObjectBase as long as the user
	// had permission to create those fields, so if we got a
	// null result we either have a schema corruption problem
	// or a permission to create field problem

	DBObjectBaseField fieldDef = oldRef.getFieldDef(targetField);
	
	if (fieldDef == null)
	  {
	    // "InvidDBField.unbind(): Couldn''t unlink from old reference"
	    // "Your operation could not succeed due to a possible inconsistency in the server database.  Target field number {0} in object {1} does not exist."
	    return Ganymede.createErrorDialog(ts.l("unbind.no_unlink_sub"),
					      ts.l("bind.inconsistency", Integer.toString(targetField), oldRef.getLabel()));	  }
	else
	  {
	    // "InvidDBField.unbind(): Couldn''t unlink from old reference"
	    // "Your operation could not succeed due to a possible inconsistency in the server database.  Target field {0} is undefined in object {1}."
	    return Ganymede.createErrorDialog(ts.l("unbind.no_unlink_sub"),
					      ts.l("bind.inconsistency", fieldDef.getName(), oldRef.getLabel()));
	  }
      }

    try
      {
	// note that we only want to remove one instance of the invid
	// pointing back to us.. we may have multiple fields on the
	// this object pointing to the remote, and we want to only
	// clear one back pointer at a time.

	retVal = oldRefField.dissolve(owner.getInvid(), anonymous||local);

	if (retVal != null && !retVal.didSucceed())
	  {
	    return retVal;
	  }
      }
    catch (IllegalArgumentException ex)
      {
	System.err.println("hm, couldn't dissolve a reference in " + getName());

	if (anonymous)
	  {
	    System.err.println("Did do an anonymous edit on target");
	  }
	else
	  {
	    System.err.println("Didn't do an anonymous edit on target");
	  }

	throw (IllegalArgumentException) ex;
      }
	

    // tell the client that it needs to rescan the old remote end of this binding

    newRetVal = new ReturnVal(true, true);
    newRetVal.addRescanField(remote, targetField);

    newRetVal.unionRescan(retVal);

    return newRetVal;		// success
  }

  /**
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
   * This method is private, and is not to be called by any code outside
   * of this class.
   *
   * @param oldInvid The invid to be unlinked from this field.  If this
   * field is not linked to the invid specified, nothing will happen.
   * @param local if true, this operation will be performed without regard
   * to permissions limitations.
   */

  synchronized final ReturnVal dissolve(Invid oldInvid, boolean local)
  {
    Invid tmp;

    DBEditObject eObj;

    /* -- */

    // We wouldn't be called here unless this Object and InvidDBField
    // were editable.. bind/unbind check things out for us.

    eObj = (DBEditObject) owner;

    if (isVector())
      {
	Vector values = getVectVal();

	for (int i = 0; i < values.size(); i++)
	  {
	    tmp = (Invid) values.elementAt(i);

	    if (!tmp.equals(oldInvid))
	      {
		continue;
	      }

	    ReturnVal retVal = eObj.finalizeDeleteElement(this, i);

	    if (retVal == null || retVal.didSucceed())
	      {
		// we got the okay, so we are going to take out this
		// element and return.  note that if we didn't return
		// here, we might get confused on our values loop.

		values.removeElementAt(i);
		qr = null;

		return retVal;
	      }
	    else
	      {
		if (retVal.getDialog() != null)
		  {
		    // "InvidDBField.dissolve(): couldn''t finalizeDeleteElement"
		    // "The custom plug-in class for object {0} refused to allow us to clear out all the references in field {1}:\n\n{2}"
		    return Ganymede.createErrorDialog(ts.l("dissolve.no_finalize_vect"),
						      ts.l("dissolve.refused_vect", eObj.getLabel(), getName(), retVal.getDialog().getText()));
		  }
		else
		  {
		    // "InvidDBField.dissolve(): couldn''t finalizeDeleteElement"
		    // "The custom plug-in class for object {0} refused to allow us to clear out all the references in field {1}"
		    return Ganymede.createErrorDialog(ts.l("dissolve.no_finalize_vect"),
						      ts.l("dissolve.refused_vect_notext", eObj.getLabel(), getName()));
		  }
	      }
	  }

	// "Warning: dissolve for {0}:{1} called with an unbound invid {2}"
	Ganymede.debug(ts.l("dissolve.unbound_vector", owner.getLabel(), getName(), oldInvid.toString()));
	
	return null;	// we're already dissolved, effectively
      }
    else
      {
	tmp = (Invid) value;

	if (!tmp.equals(oldInvid))
	  {
	    // Warning: dissolve for {0}:{1} called with an unbound invid {2}, current value = {3}
	    throw new RuntimeException(ts.l("dissolve.unbound_scalar",
					    owner.getLabel(), getName(), oldInvid, tmp));
	  }

	ReturnVal retVal = eObj.finalizeSetValue(this, null);

	if (retVal == null || retVal.didSucceed())
	  {
	    value = null;
	    qr = null;

	    return retVal;
	  }
	else
	  {
	    if (retVal.getDialog() != null)
	      {
		// "InvidDBField.dissolve(): couldn''t finalizeSetValue"
		// "The custom plug-in class for object {0} refused to allow us to clear out the reference in field {1}:\n\n{2}"
		return Ganymede.createErrorDialog(ts.l("dissolve.no_finalize_scalar"),
						  ts.l("dissolve.refused_scalar", eObj.getLabel(), getName(), retVal.getDialog().getText()));
	      }
	    else
	      {
		// "InvidDBField.dissolve(): couldn''t finalizeSetValue"
		// "The custom plug-in class for object {0} refused to allow us to clear out the reference in field {1}"
		return Ganymede.createErrorDialog(ts.l("dissolve.no_finalize_scalar"),
						  ts.l("dissolve.refused_scalar_notext", eObj.getLabel(), getName()));
	      }
	  }
      }
  }

  /**
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
   * <b>This method is private, and is not to be called by any code outside
   * of this class.</b>
   *
   * @param newObject The DBObject whose Invid is to be linked to this field.
   * @param local if true, this operation will be performed without regard
   * to permissions limitations.
   *
   */

  private synchronized final ReturnVal establish(DBObject newObject, boolean local)
  {
    Invid 
      tmp = null;

    DBEditObject eObj;
    
    ReturnVal retVal = null;

    Invid newInvid = newObject.getInvid();

    /* -- */

    // We wouldn't be called here unless this Object and InvidDBField
    // were editable.. bind checks things out for us.

    eObj = (DBEditObject) owner;

    if (eObj.getStatus() == ObjectStatus.DELETING || eObj.getStatus() == ObjectStatus.DROPPING)
      {
	// "InvidDBField.establish(): can''t link to deleted object"
	// "Couldn''t establish a new linkage in field {0} because object {1} has been deleted."
	return Ganymede.createErrorDialog(ts.l("establish.deletion_sub"),
					  ts.l("establish.deletion_text", this.getName(), getOwner().getLabel()));
      }

    if (isVector())
      {
	if (size() >= getMaxArraySize())
	  {
	    // "InvidDBField.establish(): Can''t link to full field"
	    // "Couldn''t establish a new linkage in vector field {0} in object {1} because the vector field is already at maximum capacity."
	    return Ganymede.createErrorDialog(ts.l("establish.overrun_sub"),
					      ts.l("establish.overrun_text", getName(), getOwner().getLabel()));
	  }

	Vector values = getVectVal();

	// For everybody else, though, this is a no-no.

	if (values.contains(newInvid))
	  {
	    // "InvidDBField.establish(): Schema logic error"
	    // "The reverse link field field {0} in object {1} refused the pointer binding 
	    // because it already points back to the object requesting binding.  This sugests that multiple fields in the originating 
	    // object {2} {3} are trying to link to one vector field in we, the target, which can''t work.  If one of the fields in {3} 
	    // were ever cleared or changed, we''d be cleared and the symmetric relationship would be broken.\n\n
	    // Have your adopter check the schema."
	    return Ganymede.createErrorDialog(ts.l("establish.schema_sub"),
					      ts.l("establish.schema_text",
						   getName(),
						   getOwner().getLabel(),
						   newObject.getTypeName(),
						   newObject.getLabel()));
	  }

	retVal = eObj.finalizeAddElement(this, newInvid);

	if (retVal == null || retVal.didSucceed())
	  {
	    values.addElement(newInvid);
	    qr = null;
	    return retVal;
	  }
	else
	  {
	    if (retVal.getDialog() != null)
	      {
		// "InvidDBField.establish(): finalizeAddElement refused"
		// "Couldn''t establish a new linkage in vector field {0} in object {1} because the custom plug in code 
		// for this object refused to approve the operation:\n\n{2}"
		return Ganymede.createErrorDialog(ts.l("establish.no_add_sub"),
						  ts.l("establish.no_add_text",
						       getName(), getOwner().getLabel(), retVal.getDialog().getText()));
	      }
	    else
	      {
		// "InvidDBField.establish(): finalizeAddElement refused"
		// "Couldn''t establish a new linkage in vector field {0} in object {1} because the custom plug in code 
		// for this object refused to approve the operation."
		return Ganymede.createErrorDialog(ts.l("establish.no_add_sub"),
						  ts.l("establish.no_add_text2", getName(), getOwner().getLabel()));
	      }
	  }
      }
    else
      {
	// ok, since we're scalar, *we* need to be unbound from *our*
	// existing target in order to be free to point back to our
	// friend who is trying to establish a link to us

	if (value != null)
	  {
	    tmp = (Invid) value;
	    
	    if (tmp.equals(newInvid))
	      {
		// "InvidDBField.establish(): schema logic error"
		// "The reverse link field field {0} in object {1} refused the pointer binding 
		// because it already points back to the object requesting binding.  This sugests that multiple fields in the originating 
		// object {2} {3} are trying to link to one scalar field in we, the target, which can''t work.  If one of the fields in {3} 
		// were ever cleared or changed, we''d be cleared and the symmetric relationship would be broken.\n\n
		// Have your adopter check the schema."
		return Ganymede.createErrorDialog(ts.l("establish.schema_sub"),
						  ts.l("establish.schema_scalar_text",
						       getName(), getOwner().getLabel(), newObject.getTypeName(), newObject.getLabel()));
	      }

	    retVal = unbind(tmp, local);

	    if (retVal != null && !retVal.didSucceed())
	      {
		return retVal;
	      }
	  }

	ReturnVal newRetVal = eObj.finalizeSetValue(this, newInvid);

	if (newRetVal == null || newRetVal.didSucceed())
	  {
	    value = newInvid;
	    qr = null;

	    if (retVal != null)
	      {
		return retVal.unionRescan(newRetVal);
	      }
	    else
	      {
		return newRetVal;
	      }
	  }
	else
	  {
	    retVal = bind(null, tmp, local); // undo, should always work

	    if (retVal != null && !retVal.didSucceed())	
	      {
		throw new RuntimeException("couldn't rebind a value " + tmp + " we just unbound.. sync error");
	      }

	    if (newRetVal.getDialog() != null)
	      {
		// "InvidDBField.establish(): finalizeSetValue refused"
		// "Couldn''t establish a new linkage in field {0} in object {1} because the custom plug in code 
		// for this object refused to approve the operation:\n\n{2}"
		return Ganymede.createErrorDialog(ts.l("establish.no_set_sub"),
						  ts.l("establish.no_set_text",
						       getName(), getOwner().getLabel(), newRetVal.getDialog().getText()));
	      }
	    else
	      {
		// "InvidDBField.establish(): finalizeSetValue refused"
		// "Couldn''t establish a new linkage in field {0} in object {1} because the custom plug in code 
		// for this object refused to approve the operation."
		return Ganymede.createErrorDialog(ts.l("establish.no_set_sub"),
						  ts.l("establish.no_set_text2",
						       getName(), getOwner().getLabel()));
	      }
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
    boolean asymBackPointer;
    boolean result = true;

    /* -- */

    if (getFieldDef().isSymmetric())
      {
	targetField = getFieldDef().getTargetField();
	asymBackPointer = false;
      }
    else
      {
	asymBackPointer = true;	// we'll test Ganymede.db.backPointers
	targetField = -1;
      }

    if (isVector())
      {
	Vector values = getVectVal();

	// test for all values in our vector

	for (int i = 0; i < values.size(); i++)
	  {
	    temp = (Invid) values.elementAt(i);

	    if (temp == null)
	      {
		Ganymede.debug("HEEEEEYYYYY!!!!!");
	      }

	    if (asymBackPointer)
	      {
		synchronized (Ganymede.db.backPointers)
		  {
		    Hashtable backpointers = (Hashtable) Ganymede.db.backPointers.get(temp);

		    if (backpointers == null)
		      {
			// "*** InvidDBField.test(): No backpointer hash at all for Invid {0} pointed to from : {1} in field {2}"
			Ganymede.debug(ts.l("test.no_backpointers", temp, objectName, getName()));
			result = false;

			continue;
		      }

		    if (!backpointers.containsKey(myInvid))
		      {
			// "*** InvidDBField.test(): backpointer hash doesn''t contain {0} for Invid {1} pointed to from {2} in field {3}"
			Ganymede.debug(ts.l("test.no_contains", myInvid, temp, objectName, getName()));
			result = false;

			continue;
		      }
		  }
	      }
	    else
	      {
		// find the object that this invid points to

		target = session.viewDBObject(temp);

		if (target == null)
		  {
		    // "*** InvidDBField.test(): Invid pointer to null object {0} located: {1} in field {2}"
		    Ganymede.debug(ts.l("test.pointer_to_null_object", temp, objectName, getName()));
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

		    // "*** InvidDBField.test(): schema error!  back-reference field not an invid field!!\n\t>{0}:{1}, referenced from {2}:{3}"
		    Ganymede.debug(ts.l("test.bad_symmetry", owner.lookupLabel(target), fieldName, objectName, getName()));
		    result = false;

		    continue;
		  }

		if (backField == null)
		  {
		    // "InvidDBField.test(): Object {0}, field {1} is targeting a field, {2} in object {3} which does not exist!"
		    Ganymede.debug(ts.l("test.pointer_to_null_field", objectName, getName(), Integer.toString(targetField), target));
		    result = false;

		    continue;
		  }
		else if (!backField.isDefined())
		  {
		    // "InvidDBField.test(): Object {0}, field {1} is targeting a field, {2} in object {3} which is not defined!"
		    Ganymede.debug(ts.l("test.pointer_to_undefined_field", objectName, getName(), backField.getName(), target));
		    result = false;

		    continue;
		  }

		if (backField.isVector())
		  {
		    if (backField.getVectVal() == null)
		      {
			// "*** InvidDBField.test(): Null back-link invid found for invid {0} in object {1} in field {2}"
			Ganymede.debug(ts.l("test.empty_backlink", temp, objectName, getName()));
			result = false;
		    
			continue;
		      }
		    else
		      {
			boolean found = false;
			Invid testInv;
			Vector backValues = backField.getVectVal();

			/* -- */

			for (int j = 0; !found && (j < backValues.size()); j++)
			  {
			    testInv = (Invid) backValues.elementAt(j);

			    if (myInvid.equals(testInv))
			      {
				found = true;
			      }
			  }

			if (!found)
			  {
			    // "*** InvidDBField.test(): No back-link invid found for invid {0} in object {1}:{2} in {3}"
			    Ganymede.debug(ts.l("test.no_symmetry", temp, objectName, getName(), backField.getName()));
			    result = false;
			
			    continue;
			  }
		      }
		  }
		else
		  {
		    if ((backField.value == null) || !(backField.value.equals(myInvid)))
		      {
			// "*** InvidDBField.test(): No back-link invid found for invid {0} in object {1}:{2} in {3}"
			Ganymede.debug(ts.l("test.no_symmetry", temp, objectName, getName(), backField.getName()));
			result = false;
		    
			continue;
		      }
		  }
	      }
	  }
      }
    else			// scalar invid field case
      {
	temp = (Invid) value;

	if (asymBackPointer)
	  {
	    synchronized (Ganymede.db.backPointers)
	      {
		Hashtable backpointers = (Hashtable) Ganymede.db.backPointers.get(temp);

		if (backpointers == null)
		  {
		    Ganymede.debug(ts.l("test.no_backpointers", temp, objectName, getName()));
		    result = false;
		  }

		if (!backpointers.containsKey(myInvid))
		  {
		    Ganymede.debug(ts.l("test.no_contains", myInvid, temp, objectName, getName()));
		    result = false;
		  }
	      }
	  }
	else
	  {
	    if (temp != null)
	      {
		target = session.viewDBObject(temp);

		if (target == null)
		  {
		    Ganymede.debug(ts.l("test.pointer_to_null_object", temp, objectName, getName()));
		    return false;
		  }

		try
		  {
		    backField = (InvidDBField) target.getField(targetField);
		  }
		catch (ClassCastException ex)
		  {
		    String fieldName = ((DBField) target.getField(targetField)).getName();

		    Ganymede.debug(ts.l("test.bad_symmetry",  owner.lookupLabel(target), fieldName, objectName, getName()));
		    return false;
		  }

		if (backField == null)
		  {
		    Ganymede.debug(ts.l("test.pointer_to_null_field", objectName, getName(), Integer.toString(targetField), target));
		    return false;
		  }
		else if (!backField.isDefined())
		  {
		    Ganymede.debug(ts.l("test.pointer_to_undefined_field", objectName, getName(), backField.getName(), target));
		    return false;
		  }
	
		if (backField.isVector())
		  {
		    Vector backValues = backField.getVectVal();

		    if (backValues == null)
		      {
			Ganymede.debug(ts.l("test.empty_backlink", temp, objectName, getName()));
			return false;
		      }
		    else
		      {
			boolean found = false;
			Invid testInv;

			for (int j = 0; !found && (j < backValues.size()); j++)
			  {
			    testInv = (Invid) backValues.elementAt(j);

			    if (myInvid.equals(testInv))
			      {
				found = true;
			      }
			  }

			if (!found)
			  {
			    Ganymede.debug(ts.l("test.no_symmetry", temp, objectName, getName(), backField.getName()));
			    return false;
			  }
		      }
		  }
		else
		  {
		    if ((backField.value == null) || !(backField.value.equals(myInvid)))
		      {
			Ganymede.debug(ts.l("test.no_symmetry", temp, objectName, getName(), backField.getName()));
			return false;
		      }
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
   * The Invid we are passed must refer to a valid object in the
   * database.  The remote object will be checked out for
   * editing and a backpointer will placed in it.  If this field
   * previously held a pointer to another object, that other
   * object will be checked out and its pointer to us cleared.
   *
   * The ReturnVal object returned encodes success or failure, and may
   * optionally pass back a dialog.
   *
   * @param value the value to set this field to, and Invid
   * @param local if true, this operation will be performed without regard
   * to permissions limitations.
   * @param noWizards If true, wizards will be skipped
   *
   * @see arlut.csd.ganymede.server.DBSession
   * 
   */

  public synchronized ReturnVal setValue(Object value, boolean local, boolean noWizards)
  {
    DBEditObject eObj;
    Invid oldRemote, newRemote;
    ReturnVal retVal = null, newRetVal;
    String checkkey = null;
    boolean checkpointed = false;

    /* -- */

    if (!isEditable(local))
      {
	// "Don''t have permission to change field {0} in object {1}"
	return Ganymede.createErrorDialog("InvidDBField.setValue()",
					  ts.l("global.no_perms", getName(), owner.getLabel()));
      }

    if (isVector())
      {
	// "Scalar method called on a vector field: {0} in object {1}"
 	throw new IllegalArgumentException(ts.l("global.oops_vector", getName(), owner.getLabel()));
      }

    if ((this.value == null && value == null) ||
	(this.value != null && this.value.equals(value)))
      {
	if (debug)
	  {
	    Ganymede.debug("InvidDBField.setValue(): no change");
	  }

	return null;		// no change
      }

    retVal = verifyNewValue(value, local);

    if (retVal != null && !retVal.didSucceed())
      {
	return retVal;
      }

    // we now know that value is an invid
    
    oldRemote = (Invid) this.value;
    newRemote = (Invid) value;

    eObj = (DBEditObject) owner;

    if (!noWizards && !local && eObj.getGSession().enableOversight)
      {
	// Wizard check
	
	retVal = eObj.wizardHook(this, DBEditObject.SETVAL, value, null);

	// if a wizard intercedes, we are going to let it take the ball.
	
	if (retVal != null && !retVal.doNormalProcessing)
	  {
	    return retVal;
	  }
      }

    checkkey = "setValue" + getName() + owner.getLabel();

    eObj.getSession().checkpoint(checkkey); // may block if another thread has checkpointed this transaction

    checkpointed = true;

    try
      {
	// try to do the binding

	if (newRemote != null)
	  {
	    newRetVal = bind(oldRemote, newRemote, local);
	    
	    if (newRetVal != null && !newRetVal.didSucceed())
	      {
		return newRetVal;
	      }
	    
	    if (retVal != null)
	      {
		retVal.unionRescan(newRetVal);
	      }
	    else
	      {
		retVal = newRetVal;
	      }
	  }
	else if (oldRemote != null)
	  {
	    newRetVal = unbind(oldRemote, local);
	    
	    if (newRetVal != null && !newRetVal.didSucceed())
	      {
		return newRetVal;
	      }
	    
	    if (retVal != null)
	      {
		retVal.unionRescan(newRetVal);
	      }
	    else
	      {
		retVal = newRetVal;
	      }
	  }

	// check our owner, do it.  Checking our owner should
	// be the last thing we do.. if it returns true, nothing
	// should stop us from running the change to completion
	
	newRetVal = eObj.finalizeSetValue(this, value);
	
	if (newRetVal == null || newRetVal.didSucceed())
	  {
	    this.value = value;
	    qr = null;
	    
	    // success!

	    eObj.getSession().popCheckpoint(checkkey);
	    checkpointed = false;
	    
	    if (retVal != null)
	      {
		return retVal.unionRescan(newRetVal);
	      }
	    else
	      {
		return newRetVal;
	      }
	  }
	else
	  {
	    return newRetVal;
	  }
      }
    finally
      {
	if (checkpointed)
	  {
	    eObj.getSession().rollback(checkkey);
	  }
      }
  }

  /**
   *
   * Sets the value of an element of this field, if a vector.
   *
   * The Invid we are passed must refer to a valid object in the
   * database.  The remote object will be checked out for
   * editing and a backpointer will placed in it.  If this field
   * previously held a pointer to another object, that other
   * object will be checked out and its pointer to us cleared.
   *
   * The ReturnVal object returned encodes success or failure, and may
   * optionally pass back a dialog.
   *
   * It is an error to call this method on an edit in place vector,
   * or on a scalar field.  An IllegalArgumentException will be thrown
   * in these cases.
   *
   * @param index The index of the element in this field to change.
   * @param submittedValue The value to put into this vector.
   * @param local if true, this operation will be performed without regard
   * to permissions limitations.
   * @param noWizards If true, wizards will be skipped
   *
   * @see arlut.csd.ganymede.server.DBSession
   *
   */
  
  public synchronized ReturnVal setElement(int index, Object submittedValue, boolean local, boolean noWizards)
  {
    DBEditObject eObj;
    Invid oldRemote, newRemote;
    ReturnVal retVal = null, newRetVal;
    String checkkey = null;
    boolean checkpointed = false;

    /* -- */

    // DBField.setElement(), DBField.setElementLocal() check the index and value
    // params for us.

    if (!isVector())
      {
	throw new IllegalArgumentException(ts.l("global.oops_scalar", getName(), owner.getLabel()));
      }

    if (isEditInPlace())
      {
	// "Can''t manually set element in edit-in-place vector: {0} in object {1}"
	throw new IllegalArgumentException(ts.l("setElement.edit_in_place", getName(), owner.getLabel()));
      }

    if (!isEditable(local))
      {
	return Ganymede.createErrorDialog("InvidDBField.setElement()",
					  ts.l("global.no_perms", getName(), owner.getLabel()));
      }

    Vector values = getVectVal();

    int oldIndex = values.indexOf(submittedValue);

    if (oldIndex == index)
      {
	return null;		// no-op
      }
    else if (oldIndex != -1)
      {
	return getDuplicateValueDialog("setElement", submittedValue); // duplicate
      }

    if (this.value.equals(values.elementAt(index)))
      {
	if (debug)
	  {
	    Ganymede.debug("InvidDBField.setElement(): no change");
	  }

	return null;		// no change
      }

    retVal = verifyNewValue(submittedValue, local);

    if (retVal != null && !retVal.didSucceed())
      {
	return retVal;
      }

    eObj = (DBEditObject) owner;

    if (!local && eObj.getGSession().enableOversight)
      {
	// Wizard check

	retVal = eObj.wizardHook(this, DBEditObject.SETELEMENT, new Integer(index), submittedValue);

	// if a wizard intercedes, we are going to let it take the ball.
	
	if (retVal != null && !retVal.doNormalProcessing)
	  {
	    return retVal;
	  }
      }

    oldRemote = (Invid) values.elementAt(index);
    newRemote = (Invid) submittedValue;

    checkkey = "setElement" + getName() + owner.getLabel();

    eObj.getSession().checkpoint(checkkey); // may block if another thread has checkpoint this transaction

    checkpointed = true;

    try
      {
	// try to do the binding

	newRetVal = bind(oldRemote, newRemote, local);
	
	if (newRetVal != null && !newRetVal.didSucceed())
	  {
	    return newRetVal;
	  }
	
	if (retVal != null)
	  {
	    retVal.unionRescan(newRetVal);
	  }
	else
	  {
	    retVal = newRetVal;
	  }
	
	// check our owner, do it.  Checking our owner should
	// be the last thing we do.. if it returns true, nothing
	// should stop us from running the change to completion
	
	newRetVal = eObj.finalizeSetElement(this, index, submittedValue);
	
	if (newRetVal == null || newRetVal.didSucceed())
	  {
	    values.setElementAt(submittedValue, index);
	    qr = null;

	    // success!
	    
	    eObj.getSession().popCheckpoint(checkkey);
	    checkpointed = false;
	    
	    if (retVal != null)
	      {
		return retVal.unionRescan(newRetVal);
	      }
	    else
	      {
		return newRetVal;
	      }
	  }
	else
	  {
	    return newRetVal;
	  }
      }
    finally
      {
	if (checkpointed)
	  {
	    eObj.getSession().rollback(checkkey);
	  }
      }
  }

  /**
   *
   * Adds an element to the end of this field, if a vector.
   *
   * The Invid we are passed must refer to a valid object in the
   * database.  The remote object will be checked out for
   * editing and a backpointer will placed in it.  If this field
   * previously held a pointer to another object, that other
   * object will be checked out and its pointer to us cleared.
   *
   * The ReturnVal object returned encodes success or failure, and may
   * optionally pass back a dialog.
   *
   * It is an error to call this method on an edit in place vector,
   * or on a scalar field.  An IllegalArgumentException will be thrown
   * in these cases.
   *
   * @param submittedValue The value to put into this vector.
   * @param local if true, this operation will be performed without regard
   * to permissions limitations.
   * 
   */

  public synchronized ReturnVal addElement(Object submittedValue, boolean local, boolean noWizards)
  {
    DBEditObject eObj;
    Invid remote;
    ReturnVal retVal = null, newRetVal;
    String checkkey = null;
    boolean checkpointed = false;

    /* -- */

    if (!isEditable(local))	// *sync* on GanymedeSession possible
      {
	return Ganymede.createErrorDialog("InvidDBField.addElement()",
					  ts.l("global.no_perms", getName(), owner.getLabel()));
      }

    if (isEditInPlace())
      {
	return Ganymede.createErrorDialog("InvidDBField.addElement()",
					  ts.l("addElement.edit_in_place", getName(), owner.getLabel()));
      }

    if (!isVector())
      {
	throw new IllegalArgumentException(ts.l("global.oops_scalar", getName(), owner.getLabel()));
      }

    Vector values = getVectVal();

    // don't both adding something we've already got

    if (values.contains(submittedValue))
      {
	return getDuplicateValueDialog("addElement", submittedValue); // duplicate
      }

    retVal = verifyNewValue(submittedValue, local);

    if (retVal != null && !retVal.didSucceed())
      {
	return retVal;
      }

    if (size() >= getMaxArraySize())
      {
	// "InvidDBField.addElement() - vector overflow"
	// "Field {0} already at or beyond array size limit."
	return Ganymede.createErrorDialog(ts.l("addElement.overflow_sub"),
					  ts.l("addElement.overflow_text", getName()));
      }

    remote = (Invid) submittedValue;

    eObj = (DBEditObject) owner;

    if (!noWizards && !local && eObj.getGSession().enableOversight)
      {
	// Wizard check

	retVal = eObj.wizardHook(this, DBEditObject.ADDELEMENT, submittedValue, null);

	// if a wizard intercedes, we are going to let it take the ball.
	
	if (retVal != null && !retVal.doNormalProcessing)
	  {
	    return retVal;
	  }
      }

    // check to make sure that we're not trying to add a remote
    // reference that we can't safely link to without breaking a
    // symmetric relationship.

    newRetVal = checkBindConflict(remote);

    if (newRetVal != null)
      {
	return newRetVal;
      }

    checkkey = "addElement" + getName() + owner.getLabel();

    eObj.getSession().checkpoint(checkkey); // may block if another thread has already checkpointed this transaction

    checkpointed = true;

    try
      {
	newRetVal = bind(null, remote, local);

	if (newRetVal != null && !newRetVal.didSucceed())
	  {
	    return newRetVal;
	  }

	if (retVal != null)
	  {
	    retVal.unionRescan(newRetVal);
	  }
	else
	  {
	    retVal = newRetVal;
	  }

	newRetVal = eObj.finalizeAddElement(this, submittedValue);

	if (newRetVal == null || newRetVal.didSucceed())
	  {
	    values.addElement(submittedValue);
	    qr = null;

	    // success!

	    eObj.getSession().popCheckpoint(checkkey);
	    checkpointed = false;

	    if (retVal != null)
	      {
		return retVal.unionRescan(newRetVal);
	      }
	    else
	      {
		return newRetVal;
	      }
	  } 
	else
	  {
	    return newRetVal;
	  }
      }
    finally
      {
	if (checkpointed)
	  {
	    eObj.getSession().rollback(checkkey);
	  }
      }
  }

  /**
   * Adds a set of elements to the end of this field, if a
   * vector.  Using addElements() to add a sequence of items
   * to a field may be many times more efficient than calling
   * addElement() repeatedly, as addElements() can do a single
   * server checkpoint before attempting to add all the values.
   *
   * The Invid we are passed must refer to a valid object in the
   * database.  The remote object will be checked out for
   * editing and a backpointer will placed in it.  If this field
   * previously held a pointer to another object, that other
   * object will be checked out and its pointer to us cleared.
   *
   * It is an error to call this method on an edit in place vector,
   * or on a scalar field.  An IllegalArgumentException will be thrown
   * in these cases.
   *
   * Server-side method only
   *
   * The ReturnVal object returned encodes success or failure, and
   * may optionally pass back a dialog.
   *
   * @param submittedValues Values to be added
   * @param local If true, permissions checking will be skipped
   * @param noWizards If true, wizards will be skipped
   * @param partialSuccessOk If true, addElements will add any values that
   * it can, even if some values are refused by the server logic.  Any
   * values that are skipped will be reported in a dialog passed back
   * in the returned ReturnVal
   */

  public synchronized ReturnVal addElements(Vector submittedValues, boolean local, 
					    boolean noWizards,
					    boolean partialSuccessOk)
  {
    boolean success = false;
    String checkkey = null;
    ReturnVal retVal = null;
    ReturnVal newRetVal = null;
    DBEditObject eObj;
    Vector values;
    Vector approvedValues = new Vector();

    /* -- */

    if (debug)
      {
	System.err.println("InvidDBField.addElements(" + VectorUtils.vectorString(submittedValues) + ")");
      }

    if (isEditInPlace())
      {
	// "Can''t manually add elements to edit-in-place vector: {0} in object {1}"
	return Ganymede.createErrorDialog("InvidDBField.addElements()",
					  ts.l("addElements.edit_in_place", getName(), owner.getLabel()));
      }

    if (!isEditable(local))	// *sync* on GanymedeSession possible
      {
	return Ganymede.createErrorDialog("InvidDBField.addElements()",
					  ts.l("global.no_perms", getName(), owner.getLabel()));
      }

    if (!isVector())
      {
	throw new IllegalArgumentException(ts.l("global.oops_scalar", getName(), owner.getLabel()));
      }

    values = getVectVal(); // cast once

    if (submittedValues == null || submittedValues.size() == 0)
      {
	return Ganymede.createErrorDialog(ts.l("addElements.error_sub"),
					  ts.l("addElements.null_empty_param", getName()));
      }

    // can we add this many values?

    if (size() + submittedValues.size() > getMaxArraySize())
      {
	return Ganymede.createErrorDialog(ts.l("addElements.error_sub"),
					  ts.l("addElements.overflow_text", getName(), Integer.toString(submittedValues.size()),
					       Integer.toString(size()), Integer.toString(getMaxArraySize())));
      }

    // Don't allow adding values we've already got

    Vector duplicateValues = VectorUtils.intersection(getVectVal(), submittedValues);

    if (duplicateValues.size() > 0)
      {
	if (!partialSuccessOk)
	  {
	    return getDuplicateValuesDialog("addElements", VectorUtils.vectorString(duplicateValues));
	  }
	else
	  {
	    // we use difference because we know that Ganymede vector
	    // fields are not allowed to contain duplications

	    submittedValues = VectorUtils.difference(submittedValues, getVectVal());
	  }
      }

    // check to see if all of the submitted values are acceptable in
    // type and in identity.  if partialSuccessOk, we won't complain
    // unless none of the submitted values are acceptable

    StringBuffer errorBuf = new StringBuffer();

    for (int i = 0; i < submittedValues.size(); i++)
      {
	retVal = verifyNewValue(submittedValues.elementAt(i), local);

	if (retVal != null && !retVal.didSucceed())
	  {
	    if (!partialSuccessOk)
	      {
		return retVal;
	      }
	    else
	      {
		if (retVal.getDialog() != null)
		  {
		    if (errorBuf.length() != 0)
		      {
			errorBuf.append("\n\n");
		      }

		    errorBuf.append(retVal.getDialog().getText());
		  }
	      }
	  }
	else
	  {
	    approvedValues.addElement(submittedValues.elementAt(i));
	  }
      }

    // if we weren't able to get any copied, report

    if (approvedValues.size() == 0)
      {
	// "AddElements Error"
	return Ganymede.createErrorDialog(ts.l("addElements.subject"),
					  errorBuf.toString());
      }

    // see if our container wants to intercede in the adding operation

    eObj = (DBEditObject) owner;

    if (!noWizards && !local && eObj.getGSession().enableOversight)
      {
	// Wizard check

	retVal = eObj.wizardHook(this, DBEditObject.ADDELEMENTS, approvedValues, null);

	// if a wizard intercedes, we are going to let it take the ball.

	if (retVal != null && !retVal.doNormalProcessing)
	  {
	    return retVal;
	  }
      }

    // check to make sure that we're not trying to add a remote
    // reference that we can't safely link to without breaking a
    // symmetric relationship.

    for (int i = 0; i < approvedValues.size(); i++)
      {
	Invid remote = (Invid) approvedValues.elementAt(i);

	newRetVal = checkBindConflict(remote);

	if (newRetVal != null)
	  {
	    return newRetVal;
	  }
      }

    checkkey = "addElements" + getName() + owner.getLabel();

    if (debug)
      {
	System.err.println("InvidDBField.addElements(): checkpointing " + checkkey);
      }

    eObj.getSession().checkpoint(checkkey); // may block if another thread has checkpointed this transaction

    if (debug)
      {
	System.err.println("InvidDBField.addElements(): completed checkpointing " + checkkey);
      }

    try
      {
	if (debug)
	  {
	    System.err.println("InvidDBField.addElements(): binding");
	  }

	boolean any_success = false;
	Vector failed_bindings = null;

	for (int i = 0; i < approvedValues.size(); i++)
	  {
	    Invid remote = (Invid) approvedValues.elementAt(i);

	    newRetVal = bind(null, remote, local); // bind us to the target field

	    if (newRetVal != null && !newRetVal.didSucceed())
	      {
		if (!partialSuccessOk)
		  {
		    return newRetVal;
		  }
		else
		  {
		    if (retVal.getDialog() != null)
		      {
			if (errorBuf.length() != 0)
			  {
			    errorBuf.append("\n\n");
			  }
			
			errorBuf.append(retVal.getDialog().getText());
		      }

		    if (failed_bindings == null)
		      {
			failed_bindings = new Vector();
		      }

		    failed_bindings.addElement(remote);
		  }
	      }
	    else
	      {
		any_success = true;
	      }

	    if (retVal != null)
	      {
		retVal.unionRescan(newRetVal);
	      }
	    else
	      {
		retVal = newRetVal;
	      }
	  }
	
	if (!any_success)
	  {
	    // "AddElements Error"
	    return Ganymede.createErrorDialog(ts.l("addElements.subject"),
					      errorBuf.toString());
	  }
	
	if (failed_bindings != null)
	  {
	    // we use difference because we know that Ganymede vector
	    // fields are not allowed to contain duplications

	    approvedValues = VectorUtils.difference(approvedValues, failed_bindings);
	  }

	if (debug)
	  {
	    System.err.println("InvidDBField.addElements(): all new elements bound");
	  }

	// Okay, see if the DBEditObject is willing to allow all of
	// these elements to be added.  If we're allowing
	// partialSuccessOk (for cloning), we'll want to query the
	// DBEditObject about each item to be added, one at a time
	
	if (partialSuccessOk)
	  {
	    any_success = false;

	    for (int i = 0; i < approvedValues.size(); i++)
	      {
		newRetVal = eObj.finalizeAddElement(this, approvedValues.elementAt(i));

		if (newRetVal == null || newRetVal.didSucceed())
		  {
		    values.addElement(approvedValues.elementAt(i));
		    any_success = true;
		  }
		else
		  {
		    if (newRetVal.getDialog() != null)
		      {
			if (errorBuf.length() != 0)
			  {
			    errorBuf.append("\n\n");
			  }
			
			errorBuf.append(newRetVal.getDialog().getText());
		      }
		  }
	      }

	    if (!any_success)
	      {
		// "AddElements Error"
		return Ganymede.createErrorDialog(ts.l("addElements.subject"),
						  errorBuf.toString());
	      }
	  }
	else
	  {
	    newRetVal = eObj.finalizeAddElements(this, approvedValues);

	    if (newRetVal == null || newRetVal.didSucceed()) 
	      {
		if (debug)
		  {
		    System.err.println("InvidDBField.addElements(): finalize approved");
		  }

		for (int i = 0; i < approvedValues.size(); i++)
		  {
		    values.addElement(approvedValues.elementAt(i));
		  }
	      }
	    else
	      {
		return newRetVal;
	      }
	  }

	qr = null;
	success = true;

	// if retVal is not null, we may have some rescan
	// information from our previous activity which we'll want
	// to return, otherwise we'll want to return the results
	// from newRetVal.
	
	if (retVal != null)
	  {
	    newRetVal = retVal.unionRescan(newRetVal);
	  }

	if (newRetVal == null)
	  {
	    newRetVal = new ReturnVal(true, true);
	  }

	// if we were not able to copy some of the values (and we
	// had partialSuccessOk set), encode a description of what
	// happened along with the success code

	if (errorBuf.length() != 0)
	  {
	    newRetVal.setDialog(new JDialogBuff("Warning",
						errorBuf.toString(),
						StringDialog.getDefaultOk(),
						null,
						"ok.gif"));
	  }
	
	return newRetVal;
      }
    finally
      {
	if (success)
	  {
	    if (debug)
	      {
		System.err.println("InvidDBField.addElements(): popping checkpoint " + checkkey);
	      }

	    eObj.getSession().popCheckpoint(checkkey);
	  }
	else
	  {
	    // undo the bindings

	    if (debug)
	      {
		System.err.println("InvidDBField.addElements(): rolling back checkpoint " + checkkey);
	      }

	    eObj.getSession().rollback(checkkey);
	  }
      }
  }


  /**
   * Creates and adds a new embedded object in this
   * field, if it is an edit-in-place vector.
   *
   * Returns a {@link arlut.csd.ganymede.common.ReturnVal ReturnVal} which
   * conveys a success or failure result.  If the createNewEmbedded()
   * call was successful, the ReturnVal will contain
   * {@link arlut.csd.ganymede.common.Invid Invid} and {@link
   * arlut.csd.ganymede.rmi.db_object db_object}, which can be retrieved
   * using the ReturnVal {@link arlut.csd.ganymede.common.ReturnVal#getInvid() getInvid()} 
   * and {@link arlut.csd.ganymede.common.ReturnVal#getObject() getObject()}
   * methods..
   *
   * @see arlut.csd.ganymede.rmi.invid_field
   */

  public ReturnVal createNewEmbedded() throws NotLoggedInException, GanyPermissionsException
  {
    return createNewEmbedded(false);
  }

  /**
   * Creates and adds a new embedded object in this
   * field, if it is an edit-in-place vector.
   *
   * Returns a {@link arlut.csd.ganymede.common.ReturnVal ReturnVal} which
   * conveys a success or failure result.  If the createNewEmbedded()
   * call was successful, the ReturnVal will contain
   * {@link arlut.csd.ganymede.common.Invid Invid} and {@link
   * arlut.csd.ganymede.rmi.db_object db_object}, which can be retrieved
   * using the ReturnVal {@link arlut.csd.ganymede.common.ReturnVal#getInvid() getInvid()} 
   * and {@link arlut.csd.ganymede.common.ReturnVal#getObject() getObject()}
   * methods..
   *
   * @param local If true, we don't check permission to edit this
   * field before creating the new object.  
   */

  public synchronized ReturnVal createNewEmbedded(boolean local) throws NotLoggedInException, GanyPermissionsException
  {
    ReturnVal retVal = null;

    /* -- */

    if (!isEditable(local))
      {
	return Ganymede.createErrorDialog("InvidDBField.createNewEmbedded()",
					  ts.l("global.no_perms", getName(), owner.getLabel()));
      }

    if (!isVector())
      {
	throw new IllegalArgumentException(ts.l("global.oops_scalar", getName(), owner.getLabel()));
      }

    Vector values = getVectVal();

    if (!isEditInPlace())
      {
	// "Edit-in-place method called on a referential invid field {0} in object {1}"
	throw new IllegalArgumentException(ts.l("createNewEmbedded.non_embedded", getName(), owner.getLabel()));
      }

    if (size() >= getMaxArraySize())
      {
	return Ganymede.createErrorDialog("InvidDBField.createNewEmbedded()",
					  ts.l("addElement.overflow_text", getName()));
      }

    DBEditObject eObj = (DBEditObject) owner;

    // have our owner create a new embedded object
    // for us 

    retVal = eObj.createNewEmbeddedObject(this);

    if (retVal == null)
      {
	// "Couldn''t create new embedded object"
	// "A null value was returned by the createNewEmbeddedObject() call in the {0} field.\n\nThis may be due to a customization problem."
	return Ganymede.createErrorDialog(ts.l("createNewEmbedded.failure_sub"),
					  ts.l("createNewEmbedded.failure_text", getName()));
      }
    else if (!retVal.didSucceed())
      {
	return retVal;
      }

    Invid newObj = retVal.getInvid();

    if (newObj == null)
      {
	// "Couldn''t create new embedded object"
	// "An error occurred in trying to create a new embedded object in the {0} field.\n\nThis may be due to a permissions problem."
	return Ganymede.createErrorDialog(ts.l("createNewEmbedded.failure_sub"),
					  ts.l("createNewEmbedded.null_embedded", getName()));
      }

    // now we need to do the binding as appropriate.

    // Note that we are just taking it for granted that we can edit
    // the newly created object.  This is the right thing to do.  The
    // permissions system in GanymedeSession wouldn't know how to
    // check this operation until we link the newly embedded object
    // into its container anyway.

    DBEditObject embeddedObj = ((DBEditObject) owner).getSession().editDBObject(newObj); // *sync* DBSession DBObject

    if (embeddedObj == null)
      {
	throw new NullPointerException("gah, null embedded obj!");
      }

    // bind the object to its container.. note that ContainerField
    // is a standard built-in field for embedded objects and as
    // such it doesn't have the specific details as to the containing
    // object's binding recorded.  We'll have to do the bidirectional
    // binding ourselves, in two steps.

    // we have to use setFieldValueLocal() here because the
    // permissions system uses the ContainerField to determine rights
    // to modify the field.. since we are just now setting the
    // container, the permissions system will fail if we don't bypass
    // it by using the local variant.

    retVal = embeddedObj.setFieldValueLocal(SchemaConstants.ContainerField, // *sync* DBField
					    owner.getInvid());

    if (retVal != null && !retVal.didSucceed())
      {
	return retVal;
      }
    else if (debug)
      {
	InvidDBField invf = (InvidDBField)  embeddedObj.getField(SchemaConstants.ContainerField);

	if (debug)
	  {
	    System.err.println("-- Created a new embedded object in " + owner.getLabel() + 
			       ", set it's container pointer to " + invf.getValueString());
	  }
      }

    // finish the binding.  Note that we are directly modifying values
    // here rather than going to this.addElement().  If we did
    // this.addElement(), we might get a redundant attempt to do the
    // invid binding, as the containing field may indeed have the
    // reverse pointer in the object's container field specified in
    // the schema.  Doing it this way, we don't have to worry about
    // whether the admins got this part of the schema right.

    if (!local && eObj.getGSession().enableOversight)
      {
	// Wizard check

	retVal = eObj.wizardHook(this, DBEditObject.ADDELEMENT, newObj, null);

	// if a wizard intercedes, we are going to let it take the ball.
	
	if (retVal != null && !retVal.doNormalProcessing)
	  {
	    return retVal;
	  }
      }

    ReturnVal newRetVal = eObj.finalizeAddElement(this, newObj);

    if (newRetVal == null || newRetVal.didSucceed())
      {
	values.addElement(newObj);
	qr = null;

	// now we need to initialize the new embedded object, since we
	// defer that activity for embedded objects until after we
	// get the embedded object linked to the parent

	DBSession session = eObj.getSession();
	String ckp_label = eObj.getLabel() + "addEmbed";

	session.checkpoint(ckp_label); // may block if another thread has checkpointed this transaction
	boolean checkpointed = true;

	try
	  {
	    retVal = embeddedObj.initializeNewObject();

	    if (retVal == null || retVal.didSucceed())
	      {
		// sweet, success, forget the checkpoint

		session.popCheckpoint(ckp_label);
		checkpointed = false;

		if (retVal == null)
		  {
		    retVal = new ReturnVal(true);
		  }

		retVal.setInvid(newObj);
		retVal.setObject(embeddedObj);
	    
		return retVal.unionRescan(newRetVal);
	      }
	    else
	      {
		return retVal;
	      }
	  }
	finally
	  {
	    // ups, something tanked.  rollback if it happened
	    // before we popped

	    if (checkpointed)
	      {
		session.rollback(ckp_label);
	      }
	  }
      } 
    else
      {
	embeddedObj.setFieldValue(SchemaConstants.ContainerField, null); // *sync* DBField

	if (newRetVal.getDialog() != null)
	  {
	    return Ganymede.createErrorDialog(ts.l("createNewEmbedded.failure_sub"),
					      ts.l("createNewEmbedded.refused_creation", newRetVal.getDialog().getText()));
	  }
	else
	  {
	    return Ganymede.createErrorDialog(ts.l("createNewEmbedded.failure_sub"),
					      ts.l("createNewEmbedded.refused_creation_no_text"));
	  }
      }
  }

  /**
   *
   * Return the object type that this invid field is constrained to point to, if set
   *
   * -1 means there is no restriction on target type.
   *
   * -2 means there is no restriction on target type, but there is a specified symmetric field.
   *
   * @see arlut.csd.ganymede.rmi.invid_field
   */

  public short getTargetBase()
  {
    return getFieldDef().getTargetBase();
  }

  /**
   * Returns an actual reference to the object type targeted by
   * this invid field, or null if no specific object type is
   * targeted.
   */

  public DBObjectBase getTargetBaseDef()
  {
    short targetBaseType = getTargetBase();

    if (targetBaseType < 0)
      {
	return null;
      }

    return getFieldDef().base.getStore().getObjectBase(targetBaseType);
  }

  /**
   * Return the numeric id code for the field that this invid field
   * is set to point to, if any.  If -1 is returned, this invid field
   * does not point to a specific field, and so has no symmetric
   * relationship. 
   */

  public short getTargetField()
  {
    return getFieldDef().getTargetField();
  }

  /**
   * Returns an actual reference to the field definition targeted by
   * this invid field, or null if no specific field type is
   * targeted.
   */

  public DBObjectBaseField getTargetFieldDef()
  {
    // if we're not pointing to a symmetric field,
    // return null

    if (!getFieldDef().isSymmetric())
      {
	return null;
      }

    // if we're not pointing to a specific field, also return null.
    // in practice, this will occur with owner groups whose 'object
    // owned' field can point to the 'owner' field of any non-embedded
    // object

    if (getTargetBase() < 0)
      {
	return null;
      }

    // we've got a specific field type in a specific object type, find
    // it

    DBObjectBase targetBase = getTargetBaseDef();

    return (DBObjectBaseField) targetBase.getField(getTargetField());
  }

  /**
   * Deletes an element of this field, if a vector.
   *
   * The object pointed to by the Invid in the element to be deleted 
   * will be checked out of the database and its pointer to us cleared.
   *
   * Returns null on success, non-null on failure.
   *
   * If non-null is returned, the ReturnVal object
   * will include a dialog specification that the
   * client can use to display the error condition.
   */

  public synchronized ReturnVal deleteElement(int index, boolean local, boolean noWizards)
  {
    DBEditObject eObj;
    Invid remote;
    ReturnVal retVal = null, newRetVal;
    String checkkey = null;
    boolean checkpointed = false;

    /* -- */

    if (!isEditable(local))
      {
	return Ganymede.createErrorDialog("InvidDBField.deleteElement()",
					  ts.l("global.no_perms", getName(), owner.getLabel()));
      }

    if (!isVector())
      {
	throw new IllegalArgumentException(ts.l("global.oops_scalar", getName(), owner.getLabel()));
      }

    Vector values = getVectVal();

    remote = (Invid) values.elementAt(index);

    eObj = (DBEditObject) owner;

    checkkey = "delElement" + getName() + owner.getLabel();

    if (!noWizards && !local && eObj.getGSession().enableOversight)
      {
	// Wizard check

	retVal = eObj.wizardHook(this, DBEditObject.DELELEMENT, new Integer(index), null);

	// if a wizard intercedes, we are going to let it take the ball.
	
	if (retVal != null && !retVal.doNormalProcessing)
	  {
	    return retVal;
	  }
      }

    // ok, we're going to handle it.  Checkpoint
    // so we can easily undo any changes that we make
    // if we have to return failure.

    if (debug)
      {
	System.err.println("][ InvidDBField.deleteElement() checkpointing " + checkkey);
      }

    eObj.getSession().checkpoint(checkkey); // may block if another thread has checkpointed this transaction

    checkpointed = true;

    if (debug)
      {
	System.err.println("][ InvidDBField.deleteElement() checkpointed " + checkkey);
      }

    try
      {
	// if we are an edit in place object, we don't want to do an
	// unbinding.. we'll do a deleteDBObject() below, instead.  The
	// reason for this is that the deleteDBObject() code requires that
	// the SchemaConstants.ContainerField field be intact to properly
	// check permissions for embedded objects.

	if (!getFieldDef().isEditInPlace())
	  {
	    newRetVal = unbind(remote, local);

	    if (newRetVal != null && !newRetVal.didSucceed())
	      {
		return newRetVal;
	      }

	    if (retVal != null)
	      {
		retVal.unionRescan(newRetVal);
	      }
	    else
	      {
		retVal = newRetVal;
	      }
	  }

	// finalizeDeleteElement() just gives the DBEditObject a chance to
	// approve or disapprove deleting an element from this field
    
	newRetVal = eObj.finalizeDeleteElement(this, index);

	if (newRetVal == null || newRetVal.didSucceed())
	  {
	    values.removeElementAt(index);

	    qr = null;	// Clear the cache to force the choices to be read again

	    if (retVal != null)
	      {
		retVal.unionRescan(newRetVal);
	      }
	    else
	      {
		retVal = newRetVal;
	      }

	    // if we are an editInPlace field, unlinking this object means
	    // that we should go ahead and delete the object.

	    if (getFieldDef().isEditInPlace())
	      {
		newRetVal = eObj.getSession().deleteDBObject(remote);

		if (newRetVal != null && !newRetVal.didSucceed())
		  {
		    return newRetVal;	// go ahead and return our error code
		  }

		if (retVal != null)
		  {
		    retVal.unionRescan(newRetVal);
		  }
		else
		  {
		    retVal = newRetVal;
		  }
	      }

	    // success

	    eObj.getSession().popCheckpoint(checkkey);
	    checkpointed = false;

	    return retVal;
	  }
	else
	  {
	    if (newRetVal.getDialog() != null)
	      {
		// "InvidDBField.deleteElement() - custom code rejected element deletion"
		// "Custom code refused deletion of element {0} from field {1} in object {2}.\n\n{3}"
		return Ganymede.createErrorDialog(ts.l("deleteElement.rejected"),
						  ts.l("deleteElement.no_finalize", Integer.toString(index), 
						       getName(), owner.getLabel(), newRetVal.getDialog().getText()));
	      }
	    else
	      {
		// "InvidDBField.deleteElement() - custom code rejected element deletion"
		// "Custom code refused deletion of element {0} from field {1} in object {2}."
		return Ganymede.createErrorDialog(ts.l("deleteElement.rejected"),
						  ts.l("deleteElement.no_finalize_no_text",
						       Integer.toString(index), getName(), owner.getLabel()));
	      }
	  }
      }
    finally
      {
	if (checkpointed)
	  {
	    eObj.getSession().rollback(checkkey);
	  }
      }
  }

  /**
   * Removes a set of elements from this field, if a
   * vector.  Using deleteElements() to remove a sequence of items
   * from a field may be many times more efficient than calling
   * deleteElement() repeatedly, as removeElements() can do a single
   * server checkpoint before attempting to remove all the values.
   *
   * The ReturnVal object returned encodes success or failure, and
   * may optionally pass back a dialog.
   *
   * Server-side method only
   */

  public synchronized ReturnVal deleteElements(Vector valuesToDelete, boolean local, boolean noWizards)
  {
    DBEditObject eObj;
    ReturnVal retVal = null, newRetVal;
    boolean success = false;
    String checkkey = null;
    Vector currentValues;

    /* -- */

    if (debug)
      {
	System.err.println("InvidDBField.deleteElements(" + VectorUtils.vectorString(valuesToDelete) + ")");
      }

    if (!isEditable(local))
      {
	return Ganymede.createErrorDialog("InvidDBField.deleteElements()",
					  ts.l("global.no_perms", getName(), owner.getLabel()));
      }

    if (!isVector())
      {
	throw new IllegalArgumentException(ts.l("global.oops_scalar", getName(), owner.getLabel()));
      }

    currentValues = getVectVal();

    // see if we are being asked to remove items not in our vector

    Vector notPresent = VectorUtils.minus(valuesToDelete, currentValues);

    if (notPresent.size() != 0)
      {
	return Ganymede.createErrorDialog("InvidDBField.deleteElements()",
					  ts.l("deleteElements.not_found", getName(), VectorUtils.vectorString(notPresent)));
      }

    // see if our container wants to intercede in the removing operation

    eObj = (DBEditObject) owner;

    if (!noWizards && !local && eObj.getGSession().enableOversight)
      {
	// Wizard check

	retVal = eObj.wizardHook(this, DBEditObject.DELELEMENTS, valuesToDelete, null);

	// if a wizard intercedes, we are going to let it take the ball.
	
	if (retVal != null && !retVal.doNormalProcessing)
	  {
	    return retVal;
	  }
      }

    // ok, we're going to handle it.  Checkpoint
    // so we can easily undo any changes that we make
    // if we have to return failure.

    checkkey = "delElements" + getName() + owner.getLabel();

    if (debug)
      {
	System.err.println("][ InvidDBField.deleteElements() checkpointing " + checkkey);
      }
    
    eObj.getSession().checkpoint(checkkey); // may block if another thread has checkpointed this transaction

    if (debug)
      {
	System.err.println("][ InvidDBField.deleteElements() checkpointed " + checkkey);
      }

    // if we are an edit in place object, we don't want to do an
    // unbinding.. we'll do a deleteDBObject() below, instead.  The
    // reason for this is that the deleteDBObject() code requires that
    // the SchemaConstants.ContainerField field be intact to properly
    // check permissions for embedded objects.

    if (!getFieldDef().isEditInPlace())
      {
	try
	  {
	    // do all the remote unbinding, as needed

	    for (int i = 0; i < valuesToDelete.size(); i++)
	      {
		Invid remote = (Invid) valuesToDelete.elementAt(i);

		newRetVal = unbind(remote, local);

		if (newRetVal != null && !newRetVal.didSucceed())
		  {
		    return newRetVal; // abort.  the finally clause will uncheckpoint
		  }

		if (retVal != null)
		  {
		    retVal.unionRescan(newRetVal);
		  }
		else
		  {
		    retVal = newRetVal;
		  }
	      }

	    // check to make sure our container is okay with us deleting
	    // all of these values

	    newRetVal = eObj.finalizeDeleteElements(this, valuesToDelete);

	    if (newRetVal == null || newRetVal.didSucceed())
	      {
		// our container is okay, go ahead and remove

		for (int i = 0; i < valuesToDelete.size(); i++)
		  {
		    currentValues.removeElement(valuesToDelete.elementAt(i));
		  }

		qr = null;
		success = true;

		// if retVal is not null, we may have some rescan
		// information from our previous activity which we'll want
		// to return, otherwise we'll want to return the results
		// from newRetVal.

		if (retVal != null)
		  {
		    return retVal.unionRescan(newRetVal);
		  }
		else
		  {
		    return newRetVal;
		  }
	      }
	    else
	      {
		return newRetVal;
	      }
	  }
	finally
	  {
	    // if we've had success, clear the checkpoint, else rollback

	    if (success)
	      {
		eObj.getSession().popCheckpoint(checkkey);
	      }
	    else
	      {
		// undo the bindings
		
		eObj.getSession().rollback(checkkey);
	      }
	  }
      }
    else			// deleting embedded objects
      {
	try
	  {
	    // check to make sure our container is okay with us deleting
	    // all of these values

	    retVal = eObj.finalizeDeleteElements(this, valuesToDelete);

	    if (retVal == null || retVal.didSucceed())
	      {
		for (int i = 0; i < valuesToDelete.size(); i++)
		  {
		    Invid remote = (Invid) valuesToDelete.elementAt(i);

		    newRetVal = eObj.getSession().deleteDBObject(remote);

		    if (newRetVal != null && !newRetVal.didSucceed())
		      {
			return newRetVal;
		      }

		    if (retVal != null)
		      {
			retVal.unionRescan(newRetVal);
		      }
		    else
		      {
			retVal = newRetVal;
		      }
		  }

		success = true;
	      }

	    return retVal;
	  }
	finally
	  {
	    if (success)
	      {
		eObj.getSession().popCheckpoint(checkkey);
	      }
	    else
	      {
		// undo the bindings
		
		eObj.getSession().rollback(checkkey);
	      }
	  }
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
   * @see arlut.csd.ganymede.rmi.invid_field 
   *
   */

  public boolean limited()
  {
    return getFieldDef().isTargetRestricted();
  }

  /**
   *
   * Returns a QueryResult encoded list of the current values
   * stored in this field.
   *
   * @see arlut.csd.ganymede.rmi.invid_field
   *
   */

  public synchronized QueryResult encodedValues()
  {
    QueryResult results = new QueryResult();
    Invid invid;
    String label;
    DBObject object;
    GanymedeSession gsession = null;

    /* -- */

    if (!isVector())
      {
	throw new IllegalArgumentException(ts.l("global.oops_scalar", getName(), owner.getLabel()));
      }

    Vector values = getVectVal();

    gsession = owner.getGSession();

    if (gsession == null)
      {
	gsession = Ganymede.internalSession;
      }

    for (int i = 0; i < values.size(); i++)
      {
	invid = (Invid) values.elementAt(i);

	if (gsession != null)
	  {
	    object = gsession.getSession().viewDBObject(invid);

	    if (object == null)
	      {
		Ganymede.debug(ts.l("encodedValues.bad_invid", owner.getLabel(), getName(), invid));

		label = invid.toString();
	      }
	    else
	      {
		// use lookupLabel because our owner may wish to construct a custom
		// label for the object.. different objects may display the name
		// of a referenced object differently.
		
		if (owner instanceof DBEditObject)
		  {
		    label = owner.lookupLabel(object);
		  }
		else
		  {
		    label = owner.getBase().getObjectHook().lookupLabel(object);
		  }
	      }
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
   * Returns true if the only valid values for this invid field are in
   * the QueryRersult returned by choices().  In particular, if
   * mustChoose() returns true, &lt;none&gt; is not an acceptable
   * choice for this field after the field's value is initially set.
   *
   * @see arlut.csd.ganymede.rmi.invid_field
   */

  public boolean mustChoose()
  {
    if (owner instanceof DBEditObject)
      {
	return ((DBEditObject) owner).mustChoose(this);
      }
    else
      {
	throw new IllegalArgumentException(ts.l("global.non_editable"));
      }
  }

  /**
   * Returns a StringBuffer encoded list of acceptable invid values
   * for this field.
   *
   * @see arlut.csd.ganymede.rmi.invid_field
   */

  public QueryResult choices() throws NotLoggedInException
  {
    return choices(true);	// by default, the filters are on
  }

  /**
   * Returns a StringBuffer encoded list of acceptable invid values
   * for this field.
   *
   * @see arlut.csd.ganymede.rmi.invid_field
   */

  public QueryResult choices(boolean applyFilter) throws NotLoggedInException
  {
    DBEditObject eObj;

    /* -- */

    if (!isEditable(true))
      {
	throw new IllegalArgumentException(ts.l("global.non_editable"));
      }

    // if choices is called, the client has asked to get
    // a new copy of the choice list for this field.  assume
    // that the client's asking because it was told to ask
    // via a rescan command in a ReturnVal from the server,
    // so we need to clear the qr cache

    eObj = (DBEditObject) owner;

    qr = eObj.obtainChoiceList(this); // non-filtered

    if (applyFilter)
      {
	qr = eObj.getGSession().filterQueryResult(qr);
      }

    return qr;
  }

  /**
   * This method returns a key that can be used by the client
   * to cache the value returned by choices().  If the client
   * already has the key cached on the client side, it
   * can provide the choice list from its cache rather than
   * calling choices() on this object again.
   *
   * If there is no caching key, this method will return null.
   */

  public Object choicesKey()
  {
    if (owner instanceof DBEditObject)
      {
	Object key = ((DBEditObject) owner).obtainChoicesKey(this);

	// we have to be careful not to let the client try to use
	// its cache if our choices() method will return items that
	// they would normally not be able to access

	if (key != null)
	  {
	    if (((DBEditObject) owner).choiceListHasExceptions(this))
	      {
		return null;
	      }
	  }

	return key;
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

  public ReturnVal verifyNewValue(Object o)
  {
    return verifyNewValue(o, false);
  }

  public ReturnVal verifyNewValue(Object o, boolean local)
  {
    DBEditObject eObj;
    Invid inv;

    /* -- */

    if (debug)
      {
	System.err.print("InvidDBField.verifyNewValue(");

	if (o instanceof Invid)
	  {
	    System.err.print(Ganymede.internalSession.viewObjectLabel((Invid) o));
	  }
	else
	  {
	    System.err.print(o);
	  }
	
	System.err.println(")");
      }

    if (!isEditable(true))
      {
	return Ganymede.createErrorDialog("InvidDBField.verifyNewValue()",
					  ts.l("global.no_perms", getName(), owner.getLabel()));
      }

    eObj = (DBEditObject) owner;

    if (!verifyTypeMatch(o))
      {
	return Ganymede.createErrorDialog("InvidDBField.verifyNewValue()",
					  ts.l("verifyNewValue.bad_type", o, getName(), owner.getLabel()));
      }

    inv = (Invid) o;

    if (inv != null)
      {
	if (limited() && (getTargetBase() != -2) &&
	    (inv.getType() != getTargetBase()))
	  {
	    // the invid points to an object of the wrong type

	    return Ganymede.createErrorDialog("InvidDBField.verifyNewValue()",
					      ts.l("verifyNewValue.bad_object_type", inv, getName(), owner.getLabel(), Integer.toString(getTargetBase())));
	  }

	if (!local && mustChoose())
	  {
	    if (qr == null && eObj.getSession().isInteractive())
	      {
		try
		  {
		    qr = eObj.obtainChoiceList(this); // allow any value, even if filtered
		  }
		catch (NotLoggedInException ex)
		  {
		    return Ganymede.createErrorDialog("InvidDBField.verifyNewValue()",
						      ts.l("global.not_logged_in"));
		  }
	      }

	    if (qr != null)
	      {
		if (debug)
		  {
		    Ganymede.debug("InvidDBField.verifyNewValue(): searching for matching choice");
		  }

		if (!qr.containsInvid(inv))
		  {
		    String invLabel = Ganymede.internalSession.viewObjectLabel(inv);

		    if (invLabel == null)
		      {
			invLabel = inv.toString();
		      }

		    if (debug)
		      {
			System.err.println("InvidDBField.verifyNewValue(" + invLabel + "): didn't match against");
			System.err.println(qr);
		      }

		    return Ganymede.createErrorDialog("InvidDBField.verifyNewValue()",
						      ts.l("verifyNewValue.bad_choice", invLabel, getName(), owner.getLabel()));
		  }
	      }
	  }
      }

    // have our parent make the final ok on the value

    return eObj.verifyNewValue(this, o);
  }
}
