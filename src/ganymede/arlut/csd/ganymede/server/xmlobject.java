/*
   xmlobject.java

   This class is a data holding structure that is intended to hold
   object and field data for an XML object element for xmlclient.

   --

   Created: 2 May 2000
   Version: $Revision$
   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$


   Module By: Jonathan Abbey

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996-2008
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

import java.rmi.RemoteException;
import java.util.Hashtable;
import java.util.Vector;

import org.xml.sax.SAXException;

import arlut.csd.Util.TranslationService;
import arlut.csd.Util.XMLElement;
import arlut.csd.Util.XMLEndDocument;
import arlut.csd.Util.XMLItem;
import arlut.csd.ganymede.common.FieldTemplate;
import arlut.csd.ganymede.common.Invid;
import arlut.csd.ganymede.common.NotLoggedInException;
import arlut.csd.ganymede.common.ReturnVal;
import arlut.csd.ganymede.rmi.Session;
import arlut.csd.ganymede.rmi.db_object;

/*------------------------------------------------------------------------------
                                                                           class
                                                                       xmlobject

------------------------------------------------------------------------------*/

/**
 * <p>This class is a data holding structure that is intended to hold
 * object and field data for an XML object element for
 * {@link arlut.csd.ganymede.server.GanymedeXMLSession GanymedeXMLSession}.
 *
 * @version $Id$
 * @author Jonathan Abbey
 */

public class xmlobject {

  final static boolean debug = false;

  /**
   * TranslationService object for handling string localization in the Ganymede
   * server.
   */

  static TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.server.xmlobject");

  /**
   * The local identifier string for this object
   */

  String id = null;

  /**
   * Descriptive typeString for this object.  This is the
   * contents of the &lt;object&gt;'s type attribute, in
   * XML (underscores for spaces) encoding.
   */

  String typeString = null;

  /**
   * Action mode for this object, should be null,
   * "create", "edit", "delete", or "inactivate". 
   */

  String actionMode = null;

  /**
   * The short object type id for this object type.
   *
   * Will be null if undefined.
   */

  Short type = null;

  /**
   * The server-side object identifier for this object.  Will
   * be null until we create or locate this object in the server.
   */

  Invid invid = null;

  /**
   * The prospective invid to create this object as.  Will only be set
   * if the object in question has an oid attribute, and if the server
   * has the -magic_import flag set.
   */

  private Invid oidCreateInvid = null;

  /**
   * If true, the invid for this field is known to not exist on the
   * server.
   */

  boolean knownNonExistent = false;

  /**
   * The object number, if known.  This may be used to identify
   * an object on the server if the object is not thought to have
   * a unique identifier string.
   *
   * Will be negative one if undefined.
   */

  int num = -1;

  /**
   * Hashtable mapping non-XML-coded {@link arlut.csd.ganymede.server.xmlfield xmlfield}
   * names to xmlfield objects.
   */

  Hashtable fields = null;

  /**
   * Reference to server-side object, if we have already created it/got a reference
   * to it from the server.
   */

  db_object objref = null;

  /**
   * Create only flag.  If this flag is true, this object was explicitly specified
   * as a new object to be created, rather than one that should be created only
   * if an object with the same type/id pair isn't found on the server.
   */

  boolean forceCreate = false;

  /**
   * Reference to the GanymedeXMLSession working with us.
   */

  GanymedeXMLSession xSession;

  /* -- */

  /**
   * This constructor takes the XMLElement defining an object to
   * be created or manipulated on the server and loads all information
   * for this object into the xmlobject created.
   *
   * This constructor reads all elements from the xmlclient
   * XML stream up to and including the matching close element for
   * this object.
   */
  
  public xmlobject(XMLElement openElement, GanymedeXMLSession xSession) throws SAXException
  {
    this.xSession = xSession;

    // handle any attributes in the element

    actionMode = openElement.getAttrStr("action");

    if (actionMode != null)
      {
	actionMode = actionMode.intern();
      }

    typeString = openElement.getAttrStr("type");

    if (typeString != null)
      {
	typeString = typeString.intern();
      }

    try
      {
	type = new Short(xSession.getTypeNum(typeString));
      }
    catch (NullPointerException ex)
      {
	// "\n\nERROR: Unrecognized object type "{0}""
	xSession.err.println(ts.l("init.unrecognized_type", openElement.getAttrStr("type")));
      }

    id = openElement.getAttrStr("id"); // may be null

    if (id != null)
      {
	id = id.intern();
      }

    Integer numInt = openElement.getAttrInt("num");

    if (numInt != null)
      {
	num = numInt.intValue();
      }

    // If the server was run with the -magic_import flag, we'll record
    // the contents of the oid attribute as the oidCreateInvid.  If it
    // turns out that we wind up trying to create this object, we'll
    // try to force the oidCreateInvid as the invid for the newly
    // created object.

    // This is used for dumping the full state of a Ganymede server
    // and loading it into another.  We *will not* use the oid
    // attribute to select pre-existing objects to edit on the loading
    // server.

    if (Ganymede.allowMagicImport)
      {
	String oidString = openElement.getAttrStr("oid");

	if (oidString != null)
	  {
	    oidCreateInvid = Invid.createInvid(oidString);
	  }
      }

    // if we get an inactivate or delete request, our object element
    // might be empty, in which case, deal.

    if (openElement.isEmpty())
      {
	return;
      }

    // if we're deleting or inactivating an object, we can't handle
    // any subelements.. so complain if we are in those modes

    if ("delete".equals(actionMode) || "inactivate".equals(actionMode))
      {
	// "XMLObject error: can''t {0} a non-empty <object> element."
	throw new NullPointerException(ts.l("init.cant_operate", actionMode));
      }

    // okay, we should contain some fields, then

    fields = new Hashtable();

    XMLItem nextItem = xSession.getNextItem();

    while (!nextItem.matchesClose("object") && !(nextItem instanceof XMLEndDocument))
      {
	if (nextItem instanceof XMLElement)
	  {
	    // the xmlfield constructor will consume all elements up
	    // to and including the matching field close element

	    xmlfield field = new xmlfield(this, (XMLElement) nextItem);

	    //	    xSession.err.println("Added new field: " + field.toString());	

	    fields.put(field.getName(), field);
	  }
	else
	  {
	    // "Unrecognized XML content in object {0}: {1}"
	    xSession.err.println(ts.l("init.unrecognized_xml", openElement, nextItem));
	  }

	nextItem = xSession.getNextItem();
      }

    if (nextItem instanceof XMLEndDocument)
      {
	// "Ran into end of XML file while parsing data object {0}"
	throw new RuntimeException(ts.l("init.early_end", this.toString()));
      }
  }

  public String getMode()
  {
    return actionMode;
  }

  /**
   * This method causes this object to be created on
   * the server.
   *
   * This method uses the standard {@link arlut.csd.ganymede.common.ReturnVal ReturnVal}
   * return semantics.
   */

  public ReturnVal createOnServer(GanymedeSession session) throws NotLoggedInException
  {
    ReturnVal result;

    /* -- */

    result = session.create_db_object(getType(), false, oidCreateInvid);

    if (!ReturnVal.didSucceed(result))
      {
	return result;
      }

    objref = result.getObject();

    try
      {
        Invid myInvid = objref.getInvid();

        setInvid(myInvid);

        xSession.rememberSeenInvid(myInvid);
      }
    catch (RemoteException ex)
      {
        ex.printStackTrace();
        throw new RuntimeException(ex.getMessage());
      }

    return null;
  }

  /**
   * This method causes this object to be checked out for editing
   * on the server.
   *
   * This method uses the standard {@link arlut.csd.ganymede.common.ReturnVal ReturnVal}
   * return semantics.
   */

  public ReturnVal editOnServer(GanymedeSession session) throws NotLoggedInException
  {
    ReturnVal result;
    Invid localInvid;

    /* -- */
    
    // just to check our logic.. we shouldn't be getting a create and
    // an edit directive on the same object from the XML file

    if (objref != null)
      {
	// "xmlobject editOnServer(): Encountered duplicate xmlobject for creating or editing: {0}"
	return Ganymede.createErrorDialog(ts.l("editOnServer.duplicate", this.toString()));
      }

    localInvid = getInvid();

    if (localInvid != null)
      {
        result = session.edit_db_object(localInvid);

	if (ReturnVal.didSucceed(result))
	  {
	    objref = result.getObject();

	    // We'll check for duplicates here.  This check is subtly
	    // different from the one above, and its purpose is to make
	    // sure that the GanymedeXMLSession storeObject() wasn't
	    // fooled by a case sensitivity issue.  Different case labels
	    // may or may not map to the same Invid in the DBStore,
	    // depending on the configuration of the object's label
	    // handling.

	    if (xSession.haveSeenInvid(localInvid))
	      {
		// "xmlobject editOnServer(): Encountered duplicate xmlobject for creating or editing: {0}"
		return Ganymede.createErrorDialog(ts.l("editOnServer.duplicate", objref.toString()));
	      }

	    xSession.rememberSeenInvid(localInvid);
          }

        return result;
      }
    else
      {
	throw new RuntimeException("Couldn't find object on server to edit it: " + 
				   this.toString());
      }
  }

  /**
   * This method uploads field information contained in this object
   * up to the Ganymede server.  Unfortunately, we can't necessarily
   * upload all the field information all at once, as we have to
   * create all the objects and set enough information into them that
   * they can properly be addressed, before we can set all the invid
   * fields.  The mode paramater controls this, allowing this method
   * to be called in multiple passes.
   *
   * @param mode 0 to register all non-invids, 1 to register just invids, 2 to register both
   */

  public ReturnVal registerFields(int mode) throws NotLoggedInException
  {
    ReturnVal result = null;

    /* -- */

    if (mode < 0 || mode > 2)
      {
	throw new IllegalArgumentException("mode must be 0, 1, or 2.");
      }

    if (debug)
      {
	xSession.err.println("Registering fields [" + mode + "] for object " + this.toString(false));
      }

    // we want to create/register the fields in their display order..
    // this is to cohere with the expectations of custom server-side
    // code, which may need to have higher fields set before accepting
    // choices for lower fields

    Vector templateVector = Ganymede.db.getObjectBase(type).getFieldTemplateVector();

    for (int i = 0; i < templateVector.size(); i++)
      {
	FieldTemplate template = (FieldTemplate) templateVector.elementAt(i);

	xmlfield field = (xmlfield) fields.get(template.getName());

	if (field == null)
	  {
	    // missing field, no big deal.  just skip it.

	    continue;
	  }

	// on mode 0, we register everything but invid's (embedded
	// objects do not count as invids for this purpose).  on mode
	// 1, we only register invid's.  on mode 2, we register
	// everything.

	if (field.fieldDef.isInvid() && !field.fieldDef.isEditInPlace() && mode == 0)
	  {
	    // skip invid's

	    continue;
	  }
	else if ((!field.fieldDef.isInvid() || field.fieldDef.isEditInPlace()) && mode == 1)
	  {
	    // skip non-invid's

	    continue;
	  }

	result = field.registerOnServer();

	if (!ReturnVal.didSucceed(result))
	  {
	    return result;
	  }
      }

    return null;
  }

  public short getType()
  {
    return type.shortValue();
  }

  /**
   * This method returns an invid for this xmlobject record,
   * performing a lookup on the server if necessary.
   *
   * The first time getInvid() is called, we'll try to find the
   * Invid from the DBStore by doing a look-up of the xml object's
   * label (if we're not given a num attribute).  getInvid() stores
   * the Invid upon first lookup as a side effect.
   */

  public Invid getInvid() throws NotLoggedInException
  {
    if (invid == null && !knownNonExistent)
      {
	// if we were given a number, assume they really do
	// mean for us to edit a pre-existing object with
	// that number, and don't argue

	if (num != -1)
	  {
	    invid = Invid.createInvid(type.shortValue(), num);
	  }
	else if (id != null)
	  {
	    // try to look it up on the server

	    if (debug)
	      {
		xSession.err.println("xmlobject.getInvid() calling findLabeledObject() on " + type.shortValue() + ":" + id + "[3]");
	      }

	    invid = xSession.session.findLabeledObject(id, type.shortValue());

	    if (invid == null)
	      {
		if (debug)
		  {
		    xSession.err.println("xmlobject.getInvid() deciding known non existent on " + type + ":" + id);
		  }

		knownNonExistent = true;
	      }

	    if (debug)
	      {
		xSession.err.println("xmlobject called findLabeledObject() on " + type.shortValue() + ":" + id + "[3]");
		xSession.err.println("findLabeledObject() returned " + invid + "[3]");
	      }
	  }
      }

    return invid;
  }

  /**
   * This method sets the invid for this object, if it is discovered
   * from the server during processing.  Used to provide invids for
   * newly created embedded objects, for instance.
   */

  public void setInvid(Invid invid)
  {
    this.invid = invid;
    this.knownNonExistent = false;
  }

  /**
   * This method returns a field definition for a named field.
   * The fieldName string is assumed to be underscore-for-space XML
   * encoded.
   */

  public FieldTemplate getFieldDef(String fieldName)
  {
    return xSession.getFieldTemplate(type, fieldName);
  }

  public String toString()
  {
    return this.toString(false);
  }

  public String toString(boolean showAll)
  {
    StringBuffer result = new StringBuffer();

    result.append("<object type=\"");
    result.append(typeString);
    result.append("\"");

    if (id != null)
      {
	result.append(" id=\"");
	result.append(id);
	result.append("\"");
      }

    if (num != -1)
      {
	result.append(" num=\"");
	result.append(num);
	result.append("\"");
      }

    result.append(">");

    if (showAll)
      {
	result.append("\n");

	// add the fields in the server's display order

	Vector templateVector = xSession.getTemplateVector(type);

	for (int i = 0; i < templateVector.size(); i++)
	  {
	    FieldTemplate template = (FieldTemplate) templateVector.elementAt(i);

	    xmlfield field = (xmlfield) fields.get(template.getName());

	    if (field == null)
	      {
		// missing field, no big deal.  just skip it.

		continue;
	      }

	    result.append("\t" + field + "\n");
	  }

	result.append("</object>");
      }

    return result.toString();
  }
}
