/*
   xmlobject.java

   This class is a data holding structure that is intended to hold
   object and field data for an XML object element for xmlclient.

   --

   Created: 2 May 2000
   Version: $Revision: 1.4 $
   Last Mod Date: $Date: 2001/03/24 07:42:25 $
   Release: $Name:  $

   Module By: Jonathan Abbey

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999, 2000
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

import arlut.csd.Util.*;
import org.xml.sax.*;
import java.rmi.*;
import java.rmi.server.*;
import java.util.Vector;
import java.util.Hashtable;

/*------------------------------------------------------------------------------
                                                                           class
                                                                       xmlobject

------------------------------------------------------------------------------*/

/**
 * <p>This class is a data holding structure that is intended to hold
 * object and field data for an XML object element for
 * {@link arlut.csd.ganymede.GanymedeXMLSession GanymedeXMLSession}.</p>
 *
 * @version $Revision: 1.4 $ $Date: 2001/03/24 07:42:25 $ $Name:  $
 * @author Jonathan Abbey
 */

public class xmlobject {

  final static boolean debug = false;

  /**
   * <p>The local identifier string for this object</p>
   */

  String id = null;

  /**
   * <p>Descriptive typeString for this object.  This is the
   * contents of the &lt;object&gt;'s type attribute, in
   * XML (underscores for spaces) encoding.</p>
   */

  String typeString = null;

  /**
   * <p>Action mode for this object, should be null,
   * "create", "edit", "delete", or "inactivate". 
   */

  String actionMode = null;

  /**
   * <p>The short object type id for this object type.</p>
   *
   * <p>Will be null if undefined.</p>
   */

  Short type = null;

  /**
   * <p>The server-side object identifier for this object.  Will
   * be null until we create or locate this object in the server.</p>
   */

  Invid invid = null;

  /**
   * <p>If true, the invid for this field is known to not exist on the
   * server.</p>
   */

  boolean knownNonExistent = false;

  /**
   * <p>The object number, if known.  This may be used to identify
   * an object on the server if the object is not thought to have
   * a unique identifier string.</p>
   *
   * <p>Will be negative one if undefined.</p>
   */

  int num = -1;

  /**
   * <p>Hashtable mapping non-XML-coded {@link arlut.csd.ganymede.xmlfield xmlfield}
   * names to xmlfield objects.</p>
   */

  Hashtable fields = null;

  /**
   * <p>Reference to server-side object, if we have already created it/got a reference
   * to it from the server.</p>
   */

  db_object objref = null;

  /**
   * <p>Create only flag.  If this flag is true, this object was explicitly specified
   * as a new object to be created, rather than one that should be created only
   * if an object with the same type/id pair isn't found on the server.</p>
   */

  boolean forceCreate = false;

  /**
   * <p>If true, this object was an embedded object</p>
   */

  boolean embedded = false;

  /**
   * <p>Reference to the GanymedeXMLSession working with us.</p>
   */

  GanymedeXMLSession xSession;

  /* -- */

  /**
   * <p>This constructor takes the XMLElement defining an object to
   * be created or manipulated on the server and loads all information
   * for this object into the xmlobject created.</p>
   *
   * <p>This constructor reads all elements from the xmlclient
   * XML stream up to and including the matching close element for
   * this object.</p>
   */
  
  public xmlobject(XMLElement openElement, GanymedeXMLSession xSession) throws SAXException
  {
    this(openElement, false, xSession);
  }

  /**
   * <p>This constructor takes the XMLElement defining an object to
   * be created or manipulated on the server and loads all information
   * for this object into the xmlobject created.</p>
   *
   * <p>This constructor reads all elements from the xmlclient
   * XML stream up to and including the matching close element for
   * this object.</p>
   */
  
  public xmlobject(XMLElement openElement, boolean embedded, GanymedeXMLSession xSession) throws SAXException
  {
    this.embedded = embedded;
    this.xSession = xSession;

    // handle any attributes in the element

    actionMode = openElement.getAttrStr("action");
    typeString = openElement.getAttrStr("type");

    try
      {
	type = new Short(xSession.getTypeNum(typeString));
      }
    catch (NullPointerException ex)
      {
	xSession.err.println("\n\nERROR: Unrecognized object type \"" + openElement.getAttrStr("type") + "\"");
      }

    id = openElement.getAttrStr("id"); // may be null

    Integer numInt = openElement.getAttrInt("num");

    if (numInt != null)
      {
	num = numInt.intValue();
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
	throw new NullPointerException("XMLObject error: can't " + actionMode + 
				       " a non-empty <object> element.");
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
	    xSession.err.println("Unrecognized XML content in object " + 
			       openElement + ":" + nextItem);
	  }

	nextItem = xSession.getNextItem();
      }

    if (nextItem instanceof XMLEndDocument)
      {
	throw new RuntimeException("Ran into end of XML file while parsing data object " + 
				   this.toString());
      }
  }

  public String getMode()
  {
    return actionMode;
  }

  /**
   * <p>This method causes this object to be created on
   * the server.</p>
   *
   * <p>This method uses the standard {@link arlut.csd.ganymede.ReturnVal ReturnVal}
   * return semantics.</p>
   */

  public ReturnVal createOnServer(Session session)
  {
    ReturnVal result;

    /* -- */

    try
      {
	result = session.create_db_object(getType());
      }
    catch (RemoteException ex)
      {
	ex.printStackTrace();
	throw new RuntimeException(ex.getMessage());
      }
    
    if (result != null && !result.didSucceed())
      {
	return result;
      }
    else
      {
	objref = result.getObject();

	try
	  {
	    this.setInvid(objref.getInvid());
	  }
	catch (RemoteException ex)
	  {
	    ex.printStackTrace();
	    throw new RuntimeException(ex.getMessage());
	  }

	return null;
      }
  }

  /**
   * <p>This method causes this object to be checked out for editing
   * on the server.</p>
   *
   * <p>This method uses the standard {@link arlut.csd.ganymede.ReturnVal ReturnVal}
   * return semantics.</p> */

  public ReturnVal editOnServer(Session session)
  {
    ReturnVal result;
    Invid localInvid;

    /* -- */
    
    // just to check our logic.. we shouldn't be getting a create and
    // an edit directive on the same object from the XML file

    if (objref != null)
      {
	throw new RuntimeException("Error, have already edited this xmlobject: " +
				   this.toString());
      }

    localInvid = getInvid();

    if (localInvid != null)
      {
	try
	  {
	    result = session.edit_db_object(localInvid);
	  }
	catch (RemoteException ex)
	  {
	    ex.printStackTrace();
	    throw new RuntimeException(ex.getMessage());
	  }

	if (result.didSucceed())
	  {
	    objref = result.getObject();
	    return result;
	  }
	else
	  {
	    return result;
	  }
      }
    else
      {
	throw new RuntimeException("Couldn't find object on server to edit it: " + 
				   this.toString());
      }
  }

  /**
   * <p>This method uploads non-Invid field information contained in
   * this object up to the Ganymede server.</p>
   *
   * <p>This method skips any Invid fields, which will need to be resolved
   * in a second pass.</p> 
   *
   * @param mode 0 to register all non-invids, 1 to register just invids, 2 to register both
   */

  public ReturnVal registerFields(int mode)
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

	// on mode 0, we register everything but invid's.  on mode 1,
	// we only register invid's.  on mode 2, we register
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

	if (result != null && !result.didSucceed())
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
   * <p>This method returns an invid for this xmlobject record,
   * performing a lookup on the server if necessary.</p> 
   */

  public Invid getInvid()
  {
    if (invid == null && !knownNonExistent)
      {
	// if we were given a number, assume they really do
	// mean for us to edit a pre-existing object with
	// that number, and don't argue

	if (num != -1)
	  {
	    invid = new Invid(type.shortValue(), num);
	  }
	else if (id != null)
	  {
	    // try to look it up on the server

	    if (debug)
	      {
		xSession.err.println("Calling findLabeledObject() on " + type.shortValue() + ":" + id + "[3]");
	      }

	    invid = xSession.session.findLabeledObject(id, type.shortValue());

	    if (invid == null)
	      {
		knownNonExistent = true;
	      }

	    if (debug)
	      {
		xSession.err.println("Called findLabeledObject() on " + type.shortValue() + ":" + id + "[3]");
		xSession.err.println("findLabeledObject() returned " + invid + "[3]");
	      }
	  }
      }

    return invid;
  }

  /**
   * <p>This method sets the invid for this object, if it is discovered
   * from the server during processing.  Used to provide invids for
   * newly created embedded objects, for instance.<p>
   */

  public void setInvid(Invid invid)
  {
    this.invid = invid;
    this.knownNonExistent = false;
  }

  /**
   * <p>This method returns a field definition for a named field.
   * The fieldName string is assumed to be underscore-for-space XML
   * encoded.</p>
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
