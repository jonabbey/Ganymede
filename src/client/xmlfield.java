/*
   xmlfield.java

   This class is a data holding structure that is intended to hold
   object and field data for an XML object element for xmlclient.

   --

   Created: 2 May 2000
   Version: $Revision: 1.6 $
   Last Mod Date: $Date: 2000/05/25 23:59:23 $
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
   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.  

*/

package arlut.csd.ganymede.client;

import arlut.csd.ganymede.*;
import arlut.csd.Util.*;
import org.xml.sax.*;
import java.util.*;
import java.text.*;
import java.rmi.*;
import java.rmi.server.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                        xmlfield

------------------------------------------------------------------------------*/

/**
 * <p>This class is a data holding structure that is intended to hold
 * object and field data for an XML object element for
 * {@link arlut.csd.ganymede.client.xmlclient xmlclient}.</p>
 *
 * @version $Revision: 1.6 $ $Date: 2000/05/25 23:59:23 $ $Name:  $
 * @author Jonathan Abbey
 */

public class xmlfield implements FieldType {

  static DateFormat formatter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss",
						     java.util.Locale.US);

  /**
   * <p>Definition record for this field type</p>
   */
  
  FieldTemplate fieldDef;

  /**
   * <p>The xmlobject that contains us.</p>
   */

  xmlobject owner;

  Object value = null;		// if scalar
  Vector setValues = null;	// if vector
  Vector delValues = null;	// if vector
  Vector addValues = null;	// if vector

  /* -- */

  public xmlfield(xmlobject owner, XMLElement openElement) throws SAXException
  {
    XMLItem nextItem;

    /* -- */

    this.owner = owner;
    String name = openElement.getName();
    fieldDef = owner.getFieldDef(name);

    //    System.err.println("parsing " + openElement);

    if (fieldDef == null)
      {
	System.err.println("Did not recognize field " + name + " in object " + owner);

	if (openElement.isEmpty())
	  {
	    throw new NullPointerException("void field def");
	  }
	else
	  {
	    skipToEndField(name);

	    throw new NullPointerException("void field def");
	  }
      }

    if (fieldDef.getType() == FieldType.BOOLEAN)
      {
	nextItem = xmlclient.xc.getNextItem();

	if (nextItem.matchesClose(name))
	  {
	    value = null;
	    return;
	  }
	else
	  {
	    Boolean bValue = parseBoolean(nextItem);

	    if (bValue != null)
	      {
		value = bValue;
	      }
	  }
      }
    else if (fieldDef.getType() == FieldType.NUMERIC)
      {
	nextItem = xmlclient.xc.getNextItem();

	if (nextItem.matchesClose(name))
	  {
	    value = null;
	    return;
	  }
	else
	  {
	    Integer iValue = parseNumeric(nextItem);

	    if (iValue != null)
	      {
		value = iValue;
	      }
	  }
      }
    else if (fieldDef.getType() == FieldType.DATE)
      {
	nextItem = xmlclient.xc.getNextItem();

	if (nextItem.matchesClose(name))
	  {
	    value = null;
	    return;
	  }
	else
	  {
	    // System.err.println("Parsing date for item " + nextItem);

	    Date dValue = parseDate(nextItem);

	    if (dValue != null)
	      {
		value = dValue;
	      }
	  }
      }
    else if (fieldDef.getType() == FieldType.STRING)
      {
	if (!fieldDef.isArray())
	  {
	    value = xmlclient.xc.reader.getFollowingString(openElement, true);

	    // getFollowingString automatically consumes the field
	    // close element after the string text

	    return;
	  }
	else
	  {
	    processVectorElements(openElement);

	    // processVectorElements automatically consumes the field
	    // close element after the vector elements

	    return;
	  }
      }
    else if (fieldDef.getType() == FieldType.INVID)
      {
	if (!fieldDef.isArray())
	  {
	    nextItem = xmlclient.xc.getNextItem();
	   
	    if (nextItem.matchesClose(name))
	      {
		value = null;
		return;
	      }
	    else
	      {
		value = new xInvid(nextItem);
	      }
	  }
	else
	  {
	    processVectorElements(openElement);

	    // processVectorElements automatically consumes the field
	    // close element after the vector elements

	    return;
	  }
      }
    else if (fieldDef.getType() == FieldType.PERMISSIONMATRIX)
      {
	nextItem = xmlclient.xc.getNextItem();

	if (!nextItem.matches("permissions"))
	  {
	    System.err.println("Unrecognized tag while parsing data for a permissions field: " + nextItem);

	    skipToEndField(name);

	    throw new NullPointerException("void field def");	    
	  }
	else
	  {
	    setValues = new Vector();
	    
	    nextItem = xmlclient.xc.getNextItem();

	    while (!nextItem.matchesClose("permissions") && !(nextItem instanceof XMLEndDocument))
	      {
		xPerm permElement = new xPerm(nextItem, true);

		if (permElement != null)
		  {
		    setValues.addElement(permElement);
		  }

		nextItem = xmlclient.xc.getNextItem();
	      }
	  }
      }
    else if (fieldDef.getType() == FieldType.PASSWORD)
      {
	nextItem = xmlclient.xc.getNextItem();

	if (nextItem.matchesClose(name))
	  {
	    value = null;
	    return;
	  }
	else
	  {
	    try
	      {
		xPassword pValue = new xPassword(nextItem);
		value = pValue;
	      }
	    catch (NullPointerException ex)
	      {
		ex.printStackTrace();
	      }
	  }
      }
    else if (fieldDef.getType() == FieldType.IP)
      {
	if (!fieldDef.isArray())
	  {
	    nextItem = xmlclient.xc.getNextItem();

	    if (nextItem.matchesClose(name))
	      {
		value = null;
		return;
	      }
	    else
	      {
		value = parseIP(nextItem);
	      }
	  }
	else
	  {
	    processVectorElements(openElement);

	    // processVectorElements automatically consumes the field
	    // close element after the vector elements

	    return;
	  }
      }
    else if (fieldDef.getType() == FieldType.FLOAT)
      {
	nextItem = xmlclient.xc.getNextItem();

	if (nextItem.matchesClose(name))
	  {
	    value = null;
	    return;
	  }
	else
	  {
	    Double fValue = parseFloat(nextItem);

	    if (fValue != null)
	      {
		value = fValue;
	      }
	  }
      }

    skipToEndField(name);
  }

  /**
   * <p>This method is called to process a set of values
   * for the various kinds of vector fields.  This method
   * consumes all XMLItems up to and including the field
   * termination item.</p>
   */

  private void processVectorElements(XMLElement openElement) throws SAXException
  {
    XMLItem nextItem;
    boolean setMode = false;
    boolean canDoSetMode = true;
    Stack modeStack = new Stack();

    /* -- */

    // by default, we add

    modeStack.push("add");

    nextItem = xmlclient.xc.getNextItem();

    while (!nextItem.matchesClose(openElement.getName()) && !(nextItem instanceof XMLEndDocument))
      {
	if ((nextItem.matches("add") || nextItem.matches("delete")) && !nextItem.isEmpty())
	  {
	    if (setMode)
	      {
		throw new RuntimeException("xmlclient: error, can't enter " + nextItem.getName() +
					   " mode with a previous <set> directive in field " + openElement);
	      }
		    
	    canDoSetMode = false;
		    
	    modeStack.push(nextItem.getName());
	  }
	else if (nextItem.matches("set") && !nextItem.isEmpty())
	  {
	    if (canDoSetMode)
	      {
		setMode = true;
		setValues = new Vector();
		modeStack.push("set");
	      }
	    else
	      {
		throw new RuntimeException("xmlclient: error, can't enter set" +
					   " mode with a previous mode directive in field " + openElement);
	      }

	    nextItem = xmlclient.xc.getNextItem();
	  }
	else if (nextItem.matchesClose("add") || nextItem.matchesClose("delete"))
	  {
	    if (modeStack.peek().equals(nextItem.getName()))
	      {
		modeStack.pop();
	      }
	    else
	      {
		throw new RuntimeException("Error, found a mismatched </" +
					   nextItem.getName() + "> while parsing a vector field.");
	      }

	    nextItem = xmlclient.xc.getNextItem();
	  }
	else if (nextItem.matchesClose("set"))
	  {
	    // okay.. we're actually not going to do anything
	    // here, because set mode is really exclusive within
	    // a field definition

	    nextItem = xmlclient.xc.getNextItem();
	  }
	else
	  {
	    Object newValue = null;

	    if (fieldDef.getType() == FieldType.STRING)
	      {
		newValue = parseStringVecItem(nextItem);
	      }
	    else if (fieldDef.getType() == FieldType.INVID)
	      {
		if (fieldDef.isEditInPlace() && nextItem.matches("object") && nextItem.getAttrStr("type") != null)
		  {
		    // we've got an embedded object.. create it, and record it in
		    // the xmlclient's createdObjects vector.

		    newValue = new xmlobject((XMLElement) nextItem);

		    xmlclient.xc.createdObjects.addElement(newValue);
		  }
		else
		  {
		    newValue = new xInvid(nextItem);
		  }
	      }
	    else if (fieldDef.getType() == FieldType.IP)
	      {
		newValue = parseIP(nextItem);
	      }
		    
	    if (newValue != null)
	      {
		if (setMode)
		  {
		    // we made sure to create setValues when we
		    // entered setMode, so that we can cope with
		    // <field><set></set></field> to clear all
		    // elements in a field.

		    setValues.addElement(newValue);
		  }
		else if (modeStack.peek().equals("add"))
		  {
		    if (addValues == null)
		      {
			addValues = new Vector();
		      }

		    addValues.addElement(newValue);
		  }
		else if (modeStack.peek().equals("delete"))
		  {
		    if (delValues == null)
		      {
			delValues = new Vector();
		      }

		    delValues.addElement(newValue);
		  }
	      }
	    else
	      {
		System.err.println("xmlfield WARNING: couldn't get vector value for " + nextItem +
				   "in xml field object " + openElement);
	      }
		    
	    nextItem = xmlclient.xc.getNextItem();
	  }
      }
  }

  /**
   * <p>This private helper method works through the XML file
   * until the close element tag that terminates this field
   * definition is found.</p>
   */

  private void skipToEndField(String name) throws SAXException
  {
    XMLItem nextItem = xmlclient.xc.getNextItem();
    
    while (!(nextItem.matchesClose(name) || (nextItem instanceof XMLEndDocument)))
      {
	nextItem = xmlclient.xc.getNextItem();
      }
  }

  public Boolean parseBoolean(XMLItem item) throws SAXException
  {
    if (!item.matches("boolean"))
      {
	System.err.println("Unrecognized XML item found when boolean expected: " + item);
	return null;
      }

    if (!item.isEmpty())
      {
	System.err.println("Error, found a non-empty boolean field value element: " + item);
      }

    return new Boolean(item.getAttrBoolean("val"));
  }

  public Integer parseNumeric(XMLItem item) throws SAXException
  {
    if (!item.matches("int"))
      {
	System.err.println("Unrecognized XML item found when int expected: " + item);
	return null;
      }

    if (!item.isEmpty())
      {
	System.err.println("Error, found a non-empty int field value element: " + item);
      }

    return item.getAttrInt("val");
  }

  public Date parseDate(XMLItem item) throws SAXException
  {
    String formattedDate;
    String timecodeStr;
    long timecode = -1;
    Date result1 = null;
    Date result2 = null;

    /* -- */

    if (!item.matches("date"))
      {
	System.err.println("Unrecognized XML item found when date expected: " + item);
	return null;
      }

    if (!item.isEmpty())
      {
	System.err.println("Error, found a non-empty date field value element: " + item);
      }

    formattedDate = item.getAttrStr("val");

    if (formattedDate != null)
      {
	try
	  {
	    result1 = formatter.parse(formattedDate);
	  }
	catch (ParseException ex)
	  {
	    System.err.println("Error, could not parse date entity val " + formattedDate + " in element " + item);
	    System.err.println(ex.getMessage());
	  }
      }

    timecodeStr = item.getAttrStr("timecode");

    if (timecodeStr != null)
      {
	try
	  {
	    timecode = java.lang.Long.parseLong(timecodeStr);
	    result2 = new Date(timecode);
	  }
	catch (NumberFormatException ex)
	  {
	    System.err.println("Error, could not parse date numeric timecode " + 
			       timecodeStr + " in element " + item);
	    System.err.println(ex.getMessage());
	  }
      }

    if (result2 != null && result1 != null && !result1.equals(result2))
      {
	System.err.println("Warning, date element " + item + " is not internally consistent.");
	System.err.println("Ignoring date string \"" + formattedDate + "\".");
	System.err.println("Using timecode data string \"" + formatter.format(result2) + "\".");

	return result2;
      }

    if (result2 != null)
      {
	return result2;
      }

    if (result1 != null)
      {
	return result1;
      }

    return null;
  }

  public String parseStringVecItem(XMLItem item) throws SAXException
  {
    if (!item.matches("string"))
      {
	System.err.println("Unrecognized XML item found when vector string element expected: " + item);
	return null;
      }

    if (!item.isEmpty())
      {
	System.err.println("Error, found a non-empty vector string element: " + item);
      }

    return item.getAttrStr("val");
  }

  public String parseIP(XMLItem item) throws SAXException
  {
    if (!item.matches("ip"))
      {
	System.err.println("Unrecognized XML item found when ip expected: " + item);
	return null;
      }

    if (!item.isEmpty())
      {
	System.err.println("Error, found a non-empty ip field value element: " + item);
      }

    return item.getAttrStr("val");
  }

  public Double parseFloat(XMLItem item) throws SAXException
  {
    String valStr;
    Double result = null;

    /* -- */

    if (!item.matches("float"))
      {
	System.err.println("Unrecognized XML item found when float expected: " + item);
	return null;
      }

    if (!item.isEmpty())
      {
	System.err.println("Error, found a non-empty float field value element: " + item);
      }

    valStr = item.getAttrStr("val");

    if (valStr == null)
      {
	System.err.println("Error, float element " + item + " has no val attribute.");
	return null;
      }

    try
      {
	result = java.lang.Double.valueOf(valStr);
      }
    catch (NumberFormatException ex)
      {
	System.err.println("Error, float element " + item + " has a malformed val attribute.");
	return null;
      }

    return result;
  }

  /**
   * <p>This method is responsible for propagating this field's data
   * values to the server.</p>
   *
   * <p>Returns a {@link arlut.csd.ganymede.ReturnVal ReturnVal}
   * indicating the result of the server
   * operation.  This result may be null on success, or it may
   * be an encoded success or failure message in the normal
   * arlut.csd.ganymede.ReturnVal way.</p>
   *
   * <p>This method will throw an exception if the xmlobject that
   * contains this xmlfield has not established a remote
   * {@link arlut.csd.ganymede.db_object db_object} reference to
   * the server through which the editing can be performed.</p>
   */

  public ReturnVal registerOnServer()
  {
    ReturnVal result = null;

    /* -- */

    try
      {
	if (fieldDef.isBoolean() || fieldDef.isNumeric() || fieldDef.isDate() ||
	    fieldDef.isFloat() ||
	    (!fieldDef.isArray() &&
	     (fieldDef.isString() || fieldDef.isIP())))
	  {
	    // typical scalar, nothing fancy

	    return owner.objref.setFieldValue(fieldDef.getID(), value);
	  }
	else if (fieldDef.isArray() && (fieldDef.isString() || fieldDef.isIP()))
	  {
	    db_field field = owner.objref.getField(fieldDef.getID());

	    if (setValues != null)
	      {
		// need to explicitly delete all elements, then set new ones

		result = field.deleteElements(field.getValues());

		if (result != null && !result.didSucceed())
		  {
		    return result;
		  }

		if (setValues.size() > 0)
		  {
		    return field.addElements(setValues);
		  }
		else
		  {
		    // skip a pointless server call

		    return new ReturnVal(true);
		  }
	      }
	    else if (addValues != null)
	      {
		return field.addElements(addValues);
	      }
	    else if (delValues != null)
	      {
		return field.deleteElements(delValues);
	      }
	  }
	else if (fieldDef.isPassword())
	  {
	    xPassword xp = (xPassword) value;
	    pass_field field = (pass_field) owner.objref.getField(fieldDef.getID());

	    result = field.setPlainTextPass(xp.plaintext);

	    if (result != null && !result.didSucceed())
	      {
		return result;
	      }

	    result = field.setCryptPass(xp.crypttext);

	    if (result != null && !result.didSucceed())
	      {
		return result;
	      }

	    return field.setMD5CryptedPass(xp.md5text);
	  }
	else if (fieldDef.isInvid())
	  {
	    if (!fieldDef.isArray())
	      {
		xInvid invidValue = (xInvid) value;

		return owner.objref.setFieldValue(fieldDef.getID(), invidValue.getInvid());
	      }
	    else
	      {
		db_field field = owner.objref.getField(fieldDef.getID());

		/* -- */

		if (setValues != null)
		  {
		    // need to explicitly delete all elements, then set new ones
		    
		    result = field.deleteElements(field.getValues());
		    
		    if (result != null && !result.didSucceed())
		      {
			return result;
		      }

		    Vector newValues = new Vector();

		    for (int i = 0; i < setValues.size(); i++)
		      {
			Invid invid;

			/* -- */
			
			if (setValues.elementAt(i) instanceof xmlobject)
			  {
			    invid = ((xmlobject) setValues.elementAt(i)).getInvid();
			  }
			else
			  {
			    invid = ((xInvid) setValues.elementAt(i)).getInvid();
			  }

			if (invid == null)
			  {
			    System.err.println("Error, couldn't resolve invid reference " + setValues.elementAt(i));
			    return new ReturnVal(false);
			  }

			newValues.addElement(invid);
		      }

		    if (newValues.size() > 0)
		      {
			return field.addElements(newValues);
		      }
		    else
		      {
			// skip a pointless server call

			return new ReturnVal(true);
		      }
		  }
		else if (addValues != null)
		  {
		    Vector newValues = new Vector();

		    for (int i = 0; i < addValues.size(); i++)
		      {
			Invid invid;

			/* -- */
			
			if (addValues.elementAt(i) instanceof xmlobject)
			  {
			    invid = ((xmlobject) addValues.elementAt(i)).getInvid();
			  }
			else
			  {
			    invid = ((xInvid) addValues.elementAt(i)).getInvid();
			  }

			if (invid == null)
			  {
			    System.err.println("Error, couldn't resolve invid reference " + addValues.elementAt(i));
			    return new ReturnVal(false);
			  }

			newValues.addElement(invid);
		      }

		    return field.addElements(newValues);
		  }
		else if (delValues != null)
		  {
		    Vector oldValues = new Vector();

		    for (int i = 0; i < delValues.size(); i++)
		      {
			Invid invid;

			/* -- */
			
			if (delValues.elementAt(i) instanceof xmlobject)
			  {
			    invid = ((xmlobject) delValues.elementAt(i)).getInvid();
			  }
			else
			  {
			    invid = ((xInvid) delValues.elementAt(i)).getInvid();
			  }

			if (invid == null)
			  {
			    System.err.println("Error, couldn't resolve invid reference " + delValues.elementAt(i));
			    return new ReturnVal(false);
			  }

			oldValues.addElement(invid);
		      }

		    return field.deleteElements(oldValues);
		  }
	      }
	  }
	else if (fieldDef.isPermMatrix())
	  {
	    perm_field field = (perm_field) owner.objref.getField(fieldDef.getID());

	    if (setValues != null)
	      {
		// first, clear out any permissions set

		field.resetPerms();

		// now set the permissions
		
		for (int i = 0; i < setValues.size(); i++)
		  {
		    xPerm perm = (xPerm) setValues.elementAt(i);

		    short baseId = xmlclient.xc.getTypeNum(perm.getName());

		    result = field.setPerm(baseId, perm.getPermEntry());

		    if (result != null && !result.didSucceed())
		      {
			return result;
		      }
		    
		    if (perm.fields != null)
		      {
			Enumeration fieldPerms = perm.fields.elements();

			while (fieldPerms.hasMoreElements())
			  {
			    xPerm fieldPerm = (xPerm) fieldPerms.nextElement();

			    Hashtable fieldHash = xmlclient.xc.getFieldHash(perm.getName());

			    if (fieldHash == null)
			      {
				System.err.println("Error, can't process field permissions for object base " + 
						   XMLUtils.XMLDecode(perm.getName()) + ", base not found.");
				return new ReturnVal(false);
			      }

			    FieldTemplate permFieldDef = xmlclient.xc.getObjectFieldType(fieldHash, fieldPerm.getName());

			    if (permFieldDef == null)
			      {
				System.err.println("Error, can't process field permissions for field " +
						   XMLUtils.XMLDecode(fieldPerm.getName()) + " in object base " + 
						   XMLUtils.XMLDecode(perm.getName()) + ", base not found.");
				return new ReturnVal(false);
			      }

			    result = field.setPerm(baseId, permFieldDef.getID(), fieldPerm.getPermEntry());

			    if (result != null && !result.didSucceed())
			      {
				return result;
			      }
			  }
		      }
		  }
	      }

	    return null;	// success!
	  }
      }
    catch (RemoteException ex)
      {
	ex.printStackTrace();
	throw new RuntimeException(ex.getMessage());
      }

    return null;
  }

  /**
   * <p>Returns the non-XML-encoded name of this field.</p>
   */

  public String getName()
  {
    return fieldDef.getName();
  }

  /**
   * <p>Debug diagnostics</p>
   */

  public String toString()
  {
    StringBuffer result = new StringBuffer();

    /* -- */

    result.append(fieldDef.getName());

    if (value != null)
      {
	result.append(", value = ");
	result.append(value.toString());
      }
    else
      {
	if (setValues != null)
	  {
	    result.append(", setValues = \"");
	    result.append(arlut.csd.Util.VectorUtils.vectorString(setValues));
	    result.append("\"");
	  }
	else if (delValues != null)
	  {
	    result.append(", delValues = \"");
	    result.append(arlut.csd.Util.VectorUtils.vectorString(delValues));
	    result.append("\"");
	  }
	else if (addValues != null)
	  {
	    result.append(", addValues = \"");
	    result.append(arlut.csd.Util.VectorUtils.vectorString(delValues));
	    result.append("\"");
	  }
      }

    return result.toString();
  }
}

/**
 * <p>This class is used by the Ganymede XML client to represent
 * an invid object reference field value.  This field value
 * may or may not be fully resolved to an actual invid on the
 * server, depending on what stage of processing the XML
 * client is in.</p>
 */

class xInvid {

  short typeId;
  String objectId;
  int num = -1;

  /* -- */

  public xInvid(XMLItem item)
  {
    if (!item.matches("invid"))
      {
	System.err.println("Unrecognized XML item found when invid element expected: " + item);
	throw new NullPointerException("Bad item!");
      }

    if (!item.isEmpty())
      {
	System.err.println("Error, found a non-empty invid element: " + item);
      }

    String typeString = item.getAttrStr("type");
    objectId = item.getAttrStr("id");

    if (typeString == null)
      {
	System.err.println("Missing or malformed invid type in element: " + item);
	throw new NullPointerException("Bad item!");
      }

    if (objectId == null)
      {
	Integer iNum = item.getAttrInt("num");

	if (iNum != null)
	  {
	    num = iNum.intValue();
	  }
      }

    try
      {
	typeId = xmlclient.xc.getTypeNum(typeString);
      }
    catch (NullPointerException ex)
      {
	System.err.println("Unknown target type " + typeString + 
			   " in invid field element: " + item);
	throw new NullPointerException("Bad item!");
      }
  }

  public xInvid(String type, String id)
  {
    try
      {
	this.typeId = xmlclient.xc.getTypeNum(type);
      }
    catch (NullPointerException ex)
      {
	System.err.println("Unknown target type " + type + 
			   " in xInvid constructor");
	throw new NullPointerException("Bad item!");
      }

    this.objectId = id;
  }

  /**
   * <p>This method resolves and returns the Invid for
   * this xInvid place holder, talking to the server
   * if necessary to resolve an id string.</p>
   */

  public Invid getInvid()
  {
    if (objectId != null)
      {
	return xmlclient.xc.getInvid(typeId, objectId);
      }
    else if (num != -1)
      {
	return new Invid(typeId, num);
      }
    else
      {
	return null;
      }
  }

  public String toString()
  {
    StringBuffer result = new StringBuffer();

    /* -- */

    result.append("<invid type=\"");
    result.append(xmlclient.xc.getTypeName(typeId));
    result.append("\" ");
    
    if (objectId != null)
      {
	result.append("id=\"");
	result.append(objectId);
	result.append("\"/>");
      }
    else if (num != -1)
      {
	result.append("num=\"");
	result.append(num);
	result.append("\"/>");
      }
    else
      {
	result.append("id=\"???\"/>");
      }

    return result.toString();
  }
}

/**
 * <p>This class is used by the Ganymede XML client to represent
 * a password field value.</p>
 */

class xPassword {

  String plaintext;
  String crypttext;
  String md5text;

  /* -- */

  public xPassword(XMLItem item)
  {
    if (!item.matches("password"))
      {
	System.err.println("Unrecognized XML item found when password element expected: " + item);
	throw new NullPointerException("Bad item!");
      }

    if (!item.isEmpty())
      {
	System.err.println("Error, found a non-empty password element: " + item);
      }

    plaintext = item.getAttrStr("plaintext");
    crypttext = item.getAttrStr("crypt");
    md5text = item.getAttrStr("md5crypt");
  }

  public xPassword(String plaintext, String crypttext, String md5text)
  {
    this.plaintext = plaintext;
    this.crypttext = crypttext;
    this.md5text = md5text;
  }

  public String toString()
  {
    StringBuffer result = new StringBuffer();

    /* -- */

    result.append("<password");
    
    if (plaintext != null)
      {
	result.append(" plaintext=\"");
	result.append(plaintext);
	result.append("\"");
      }
    
    if (crypttext != null)
      {
	result.append(" crypt=\"");
	result.append(crypttext);
	result.append("\"");
      }
    
    if (md5text != null)
      {
	result.append(" md5crypt=\"");
	result.append(md5text);
	result.append("\"");
      }

    result.append("/>");

    return result.toString();
  }
}


/**
 * <p>This class is used by the Ganymede XML client to represent
 * a permission field value.</p>
 *
 * <p>xPerm are slightly recursive, in that a top-level xPerm
 * is used to represent the permissions for a type of object
 * at the object level, and contain in turn xPerm objects used
 * for the individual fields defined within that object.</p>
 */

class xPerm {

  /**
   * <p>String describing this xPerm's contents.  This String
   * is held in XMLEncoded form.  That is, spaces in the Ganymede
   * object type and/or field name have been replaced with
   * underscores.</p>
   */

  String label = null;

  /**
   * <p>If this xPerm is representing an object type as a whole,
   * fields will map field names to xPerm objects.  If this
   * xPerm is representing permissions for a field, this variable
   * will be null.</p>
   */

  Hashtable fields = null;

  boolean view = false;
  boolean edit = false;
  boolean create = false;
  boolean delete = false;

  /* -- */

  /**
   * <p>xPerm constructor.  When the constructor is called, the
   * xPerm reads the next item from the xmlclient's 
   * {@link arlut.csd.ganymede.client.xmlclient#getNextItem() getNextItem()}
   * method and uses it to initialize the xPerm.</p>
   *
   * <p>If objectType is true, the xPerm constructor assumes that
   * it is reading an entire object type's permission data.  In this
   * case, it will load the permission data for the object, including
   * all of the field permissions data contained within the top-level
   * object.</p>
   */

  public xPerm(XMLItem item, boolean objectType) throws SAXException
  {
    while (!(item instanceof XMLElement))
      {
	System.err.println("Unrecognized element encountered in xPerm constructor, skipping: " + item);
	item = xmlclient.xc.getNextItem();
      }

    label = ((XMLElement) item).getName();

    String permbits = item.getAttrStr("perm");

    if (permbits == null)
      {
	System.err.println("No perm attributes found for xPerm item " + item);
      }
    else
      {
	view = !((permbits.indexOf('v') != -1) && (permbits.indexOf('V') != -1));
	edit = !((permbits.indexOf('e') != -1) && (permbits.indexOf('E') != -1));
	create = !((permbits.indexOf('c') != -1) && (permbits.indexOf('C') != -1));
	delete = !((permbits.indexOf('d') != -1) && (permbits.indexOf('D') != -1));
      }

    if (objectType && !item.isEmpty())
      {
	fields = new Hashtable();
	item = xmlclient.xc.getNextItem();

	while (!item.matchesClose(label) && !(item instanceof XMLEndDocument))
	  {
	    xPerm fieldperm = new xPerm(item, false);
	    fields.put(fieldperm.getName(), fieldperm);

	    item = xmlclient.xc.getNextItem();
	  }
      }
  }

  public String getName()
  {
    return label;
  }

  public PermEntry getPermEntry()
  {
    return new PermEntry(view, edit, create, delete);
  }
}
