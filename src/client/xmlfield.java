/*
   xmlfield.java

   This class is a data holding structure that is intended to hold
   object and field data for an XML object element for xmlclient.

   --

   Created: 2 May 2000
   Version: $Revision: 1.17 $
   Last Mod Date: $Date: 2000/07/11 04:57:36 $
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
 * {@link arlut.csd.ganymede.client.xmlclient xmlclient}.  This
 * class is also responsible for actually registering its data
 * on the server on demand.</p>
 *
 * @version $Revision: 1.17 $ $Date: 2000/07/11 04:57:36 $ $Name:  $
 * @author Jonathan Abbey
 */

public class xmlfield implements FieldType {

  final static boolean debug = false;

  /**
   * <p>Formatter that we use for generating and parsing date fields</p>
   */

  static DateFormat[] formatters = null;

  /**
   * <p>Definition record for this field type</p>
   */
  
  FieldTemplate fieldDef;

  /**
   * <p>The xmlobject that contains us.</p>
   */

  xmlobject owner;

  // the following hold data values for this field

  Object value = null;		// if scalar
  Vector setValues = null;	// if vector
  Vector delValues = null;	// if vector
  Vector addValues = null;	// if vector
  boolean registered = false;

  /* -- */

  public xmlfield(xmlobject owner, XMLElement openElement) throws SAXException
  {
    XMLItem nextItem;

    /* -- */

    this.owner = owner;
    String elementName = openElement.getName();
    fieldDef = owner.getFieldDef(elementName);

    //    System.err.println("parsing " + openElement);

    if (fieldDef == null)
      {
	System.err.println("\nDid not recognize field " + elementName + " in object " + owner);

	if (openElement.isEmpty())
	  {
	    throw new NullPointerException("void field def");
	  }
	else
	  {
	    skipToEndField(elementName);

	    throw new NullPointerException("void field def");
	  }
      }

    if (fieldDef.getType() == FieldType.BOOLEAN)
      {
	nextItem = xmlclient.xc.getNextItem();

	if (nextItem.matchesClose(elementName))
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

	if (nextItem.matchesClose(elementName))
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

	if (nextItem.matchesClose(elementName))
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
	   
	    if (nextItem.matchesClose(elementName))
	      {
		value = null;
		return;
	      }
	    else
	      {
		// scalar invid fields are never embedded/edit in
		// place, so we know that any value we found should be
		// an <invid>

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
	    System.err.println("\nUnrecognized tag while parsing data for a permissions field: " + nextItem);

	    skipToEndField(elementName);

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

	    if (nextItem instanceof XMLEndDocument)
	      {
		throw new RuntimeException("Ran into end of XML file while processing permission field " + elementName);
	      }
	  }
      }
    else if (fieldDef.getType() == FieldType.PASSWORD)
      {
	nextItem = xmlclient.xc.getNextItem();

	if (nextItem.matchesClose(elementName))
	  {
	    value = null;
	    return;
	  }
	else
	  {
	    try
	      {
		value = new xPassword(nextItem);
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

	    if (nextItem.matchesClose(elementName))
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

	if (nextItem.matchesClose(elementName))
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

    // if we get here, we haven't yet consumed the field close
    // element.. go ahead and eat everything up to and including the
    // field close element.

    skipToEndField(elementName);
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
		System.err.println("\nxmlclient: error, can't enter " + nextItem.getName() +
				   " mode with a previous <set> directive in field " + openElement);

		throw new RuntimeException("xmlclient: error, can't enter " + nextItem.getName() +
					   " mode with a previous <set> directive in field " + openElement);
	      }
		    
	    canDoSetMode = false;
	    modeStack.push(nextItem.getName());

	    nextItem = xmlclient.xc.getNextItem();
	  }
	else if (nextItem.matches("set"))
	  {
	    if (canDoSetMode)
	      {
		setMode = true;
		setValues = new Vector();

		if (!nextItem.isEmpty())
		  {
		    modeStack.push("set");
		  }
	      }
	    else
	      {
		System.err.println("\nxmlclient: error, can't enter set" +
				   " mode with a previous mode directive in field " + openElement);

		throw new RuntimeException("xmlclient: error, can't enter set" +
					   " mode with a previous mode directive in field " + openElement);
	      }

	    nextItem = xmlclient.xc.getNextItem();
	  }
	else if (nextItem.matchesClose("add") || nextItem.matchesClose("delete"))
	  {
	    if (modeStack.size() > 1 && modeStack.peek().equals(nextItem.getName()))
	      {
		// we checked for modeStack.size() > 1 to cover the
		// initial modeStack.push("add")

		modeStack.pop();
	      }
	    else
	      {
		System.err.println("\nError, found a mismatched </" +
				   nextItem.getName() + "> while parsing a vector field.");

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

	    if (modeStack.peek().equals(nextItem.getName()))
	      {
		modeStack.pop();
	      }

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
		if (fieldDef.isEditInPlace() && nextItem.matches("object"))
		  {
		    // we've got an embedded object.. add it, we'll process it later

		    newValue = new xmlobject((XMLElement) nextItem, true);
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
			canDoSetMode = false;
		      }

		    addValues.addElement(newValue);
		  }
		else if (modeStack.peek().equals("delete"))
		  {
		    if (delValues == null)
		      {
			delValues = new Vector();
			canDoSetMode = false;
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

    if (nextItem instanceof XMLEndDocument)
      {
	throw new RuntimeException("Ran into end of XML file while processing vector field " + openElement);
      }
  }

  /**
   * <p>This private helper method works through the XML file
   * until the close element tag that terminates this field
   * definition is found.</p>
   */

  private void skipToEndField(String elementName) throws SAXException
  {
    XMLItem nextItem = xmlclient.xc.getNextItem();
    
    while (!(nextItem.matchesClose(elementName) || (nextItem instanceof XMLEndDocument)))
      {
	nextItem = xmlclient.xc.getNextItem();
      }

    if (nextItem instanceof XMLEndDocument)
      {
	System.err.println("\nRan into end of XML file while processing field " + elementName);
	throw new RuntimeException("Ran into end of XML file while processing field " + elementName);
      }
  }

  public Boolean parseBoolean(XMLItem item) throws SAXException
  {
    if (!item.matches("boolean"))
      {
	System.err.println("\nUnrecognized XML item found when boolean expected: " + item);
	return null;
      }

    if (!item.isEmpty())
      {
	System.err.println("\nError, found a non-empty boolean field value element: " + item);
      }

    return new Boolean(item.getAttrBoolean("val"));
  }

  public Integer parseNumeric(XMLItem item) throws SAXException
  {
    if (!item.matches("int"))
      {
	System.err.println("\nUnrecognized XML item found when int expected: " + item);
	return null;
      }

    if (!item.isEmpty())
      {
	System.err.println("\nError, found a non-empty int field value element: " + item);
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

    if (formatters == null)
      {
	formatters = new DateFormat[6];

	// 0 = mail-style date with timezone
	// 1 = mail-style date no timezone
	// 2 = UNIX date output, with timezone
	// 3 = UNIX date output, without timezone
	// 4 = no-comma style 0
	// 4 = no-comma style 1

	formatters[0] = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
	formatters[1] = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss");
	formatters[2] = new SimpleDateFormat("EEE dd MMM HH:mm:ss z yyyy");
	formatters[3] = new SimpleDateFormat("EEE dd MMM HH:mm:ss yyyy");
	formatters[4] = new SimpleDateFormat("EEE dd MMM yyyy HH:mm:ss z");
	formatters[5] = new SimpleDateFormat("EEE dd MMM yyyy HH:mm:ss");
      }

    if (!item.matches("date"))
      {
	System.err.println("\nUnrecognized XML item found when date expected: " + item);
	return null;
      }

    if (!item.isEmpty())
      {
	System.err.println("\nError, found a non-empty date field value element: " + item);
      }

    formattedDate = item.getAttrStr("val");

    if (formattedDate != null)
      {
	for (int i = 0; i < formatters.length && result1 == null; i++)
	  {
	    try
	      {
		result1 = formatters[i].parse(formattedDate);
	      }
	    catch (ParseException ex)
	      {
	      }
	  }

	if (result1 == null)
	  {
	    System.err.println("\nError, could not parse date entity val " + formattedDate + " in element " + item);
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
	    System.err.println("\nError, could not parse date numeric timecode " + 
			       timecodeStr + " in element " + item);
	    System.err.println(ex.getMessage());
	  }
      }

    if (result2 != null && result1 != null && !result1.equals(result2))
      {
	System.err.println("\nWarning, date element " + item + " is not internally consistent.");
	System.err.println("Ignoring date string \"" + formattedDate + "\".");
	System.err.println("Using timecode data string \"" + formatters[0].format(result2) + "\".");

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

    throw new RuntimeException("Couldn't get valid date value from " + item.toString());
  }

  public String parseStringVecItem(XMLItem item) throws SAXException
  {
    if (!item.matches("string"))
      {
	System.err.println("\nUnrecognized XML item found when vector string element expected: " + item);
	return null;
      }

    if (!item.isEmpty())
      {
	System.err.println("\nError, found a non-empty vector string element: " + item);
      }

    return item.getAttrStr("val");
  }

  public String parseIP(XMLItem item) throws SAXException
  {
    if (!item.matches("ip"))
      {
	System.err.println("\nUnrecognized XML item found when ip expected: " + item);
	return null;
      }

    if (!item.isEmpty())
      {
	System.err.println("\nError, found a non-empty ip field value element: " + item);
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
	System.err.println("\nUnrecognized XML item found when float expected: " + item);
	return null;
      }

    if (!item.isEmpty())
      {
	System.err.println("\nError, found a non-empty float field value element: " + item);
      }

    valStr = item.getAttrStr("val");

    if (valStr == null)
      {
	System.err.println("\nError, float element " + item + " has no val attribute.");
	return null;
      }

    try
      {
	result = java.lang.Double.valueOf(valStr);
      }
    catch (NumberFormatException ex)
      {
	System.err.println("\nError, float element " + item + " has a malformed val attribute.");
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

    if (debug)
      {
	System.err.println("Registering field " + this.toString());
      }

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
		// delete any values that are currently in the field
		// but which are not in our setValues vector, then add
		// any that are missing

		Vector currentValues = field.getValues();
		Vector removeValues = VectorUtils.difference(currentValues, setValues);
		Vector newValues = VectorUtils.difference(setValues, currentValues);

		if (removeValues.size() != 0)
		  {
		    result = field.deleteElements(removeValues);

		    if (result != null && !result.didSucceed())
		      {
			return result;
		      }
		  }

		if (newValues.size() > 0)
		  {
		    return field.addElements(newValues);
		  }
		else
		  {
		    // skip a pointless server call if we are doing a
		    // <set></set> to clear the field, or if we have
		    // already synchronized the field by deleting
		    // elements

		    return null;
		  }
	      }
	    
	    if (addValues != null)
	      {
		result = field.addElements(addValues);

		if (result != null && !result.didSucceed())
		  {
		    return result;
		  }
	      }

	    if (delValues != null)
	      {
		result = field.deleteElements(delValues);

		if (result != null && !result.didSucceed())
		  {
		    return result;
		  }
	      }
	  }
	else if (fieldDef.isPassword())
	  {
	    xPassword xp = (xPassword) value;
	    pass_field field = (pass_field) owner.objref.getField(fieldDef.getID());

	    // set anything we can.. note that if we transmit null for
	    // any of the three password options, it will null the
	    // password out entirely, so we don't want to transmit a
	    // null unless all three password options are all null.

	    if (xp.plaintext != null)
	      {
		result = field.setPlainTextPass(xp.plaintext);

		// setting plaintext will cause the server to
		// generate its own crypt and md5 text, so we
		// will just return here

		return result;
	      }

	    // we can't set both crypttext and md5 text, because
	    // setting one clears the other

	    if (xp.crypttext != null)
	      {
		if (xp.md5text != null)
		  {
		    System.err.println("Warning, setting crypt() hash and ignoring md5Crypt hash " +
				       "for password in object " +
				       owner);
		  }

		result = field.setCryptPass(xp.crypttext);
		
		if (result != null && !result.didSucceed())
		  {
		    return result;
		  }
	      }
	    else if (xp.md5text != null)
	      {
		result = field.setMD5CryptedPass(xp.md5text);
		
		if (result != null && !result.didSucceed())
		  {
		    return result;
		  }
	      }

	    // if we have to, clear the password out.  We do this if we see
	    // something like <password/> instead of <password plaintext="pass"/>

	    if (xp.plaintext == null && xp.crypttext == null && xp.md5text == null)
	      {
		result = field.setPlainTextPass(null);

		if (result != null && !result.didSucceed())
		  {
		    return result;
		  }
	      }

	    // we'll never get here, but the java compiler isn't smart enough to detect
	    // that

	    return null;
	  }
	else if (fieldDef.isInvid())
	  {
	    if (!fieldDef.isArray())
	      {
		// scalar invid fields are never embedded/editInPlace

		xInvid invidValue = (xInvid) value;

		return owner.objref.setFieldValue(fieldDef.getID(), invidValue.getInvid());
	      }
	    else if (!fieldDef.isEditInPlace())
	      {
		invid_field field = (invid_field) owner.objref.getField(fieldDef.getID());

		/* -- */

		if (setValues != null)
		  {
		    Vector currentValues = field.getValues();
		    Vector invidValues = getExtantInvids(setValues);
		    Vector removeValues = VectorUtils.difference(currentValues, invidValues);
		    Vector newValues = VectorUtils.difference(invidValues, currentValues);

		    if (removeValues.size() > 0)
		      {
			result = field.deleteElements(removeValues);
			
			if (result != null && !result.didSucceed())
			  {
			    return result;
			  }
		      }

		    if (newValues.size() > 0)
		      {
			return field.addElements(newValues);
		      }
		    else
		      {
			// skip a pointless server call if we are doing a
			// <set></set> to clear the field, or if we have
			// already synchronized the field by deleting
			// elements
			
			return null;
		      }
		  }
	    
		if (addValues != null)
		  {
		    result = field.addElements(getExtantInvids(addValues));
		    
		    if (result != null && !result.didSucceed())
		      {
			return result;
		      }
		  }
		
		if (delValues != null)
		  {
		    result = field.deleteElements(getExtantInvids(delValues));
		    
		    if (result != null && !result.didSucceed())
		      {
			return result;
		      }
		  }
	      }
	    else		// edit in place / embedded object case 
	      {
		invid_field field = (invid_field) owner.objref.getField(fieldDef.getID());

		/* -- */
		
		Vector currentValues = field.getValues();
		Vector needToBeEdited = null;
		Vector needToBeCreated = null;
		Vector needToBeRemoved = null;

		if (setValues != null)
		  {
		    needToBeEdited = getExtantObjects(setValues);
		    needToBeCreated = getNonRegisteredObjects(setValues);
		    needToBeRemoved = VectorUtils.difference(currentValues, getExtantInvids(setValues));
		  }

		if (addValues != null)
		  {
		    needToBeEdited = getExtantObjects(addValues);
		    needToBeCreated = getNonRegisteredObjects(addValues);
		  }

		if (delValues != null)
		  {
		    needToBeRemoved = getExtantInvids(delValues);
		  }

		if (needToBeCreated != null)
		  {
		    if (debug)
		      {
			System.err.println("Need to create " + needToBeCreated.size() + " embedded objects");
		      }

		    for (int i = 0; i < needToBeCreated.size(); i++)
		      {
			Object x = needToBeCreated.elementAt(i);

			if (x instanceof xInvid)
			  {
			    throw new RuntimeException("Error, could not process <invid> " + 
						       "element in embedded invid field: " +
						       x.toString());
			  }

			xmlobject object = (xmlobject) x;

			if (debug)
			  {
			    System.err.println("Creating embedded object " + object);
			  }

			result = field.createNewEmbedded();

			if (result != null && !result.didSucceed())
			  {
			    String msg = result.getDialogText();
				
			    if (msg != null)
			      {
				System.err.println("Error creating new embedded " + object + ", reason: " + msg);
			      }
			    else
			      {
				System.err.println("Error creating " + object + ", no reason given.");
			      }
			  }
			else
			  {
			    object.setInvid(result.getInvid());
			    object.objref = result.getObject();
			  }

			// remember that we created this embedded
			// object, so that we can refer to it
			// elsewhere by its id

			xmlclient.xc.embeddedObjects.addElement(object);

			// register any non-invids on this embedded
			// object.. this will trigger the creation of
			// any more embedded objects recursively if
			// need be

			result = object.registerFields(0);

			if (result != null && !result.didSucceed())
			  {
			    return result;
			  }
		      }
		  }

		if (needToBeEdited != null)
		  {
		    if (debug)
		      {
			System.err.println("Need to edit " + needToBeEdited.size() + " embedded objects");
		      }

		    for (int i = 0; i < needToBeEdited.size(); i++)
		      {
			xmlobject object = (xmlobject) needToBeEdited.elementAt(i);

			if (debug)
			  {
			    System.err.println("Editing embedded object " + object);
			  }

			result = object.editOnServer(xmlclient.xc.session);
			
			if (result != null && !result.didSucceed())
			  {
			    String msg = result.getDialogText();
			    
			    if (msg != null)
			      {
				System.err.println("Error editing previous embedded " + object + 
						   ", reason: " + msg);
			      }
			    else
			      {
				System.err.println("Error editing previous embedded " + object +
						   ", no reason given.");
			      }
			  }

			// remember that we edited this embedded
			// object so that we can fixup any invids
			// after all is said and done

			xmlclient.xc.embeddedObjects.addElement(object);

			// register any non-invids on this embedded
			// object.. this will trigger the creation of
			// any more embedded objects recursively if
			// need be

			result = object.registerFields(0);

			if (result != null && !result.didSucceed())
			  {
			    return result;
			  }
		      }
		  }

		if (needToBeRemoved != null)
		  {
		    if (debug)
		      {
			System.err.println("Need to remove " + needToBeRemoved.size() + " embedded objects");
		      }

		    for (int i = 0; i < needToBeRemoved.size(); i++)
		      {
			Invid invid = (Invid) needToBeRemoved.elementAt(i);

			result = field.deleteElement(invid);

			if (result != null && !result.didSucceed())
			  {
			    String msg = result.getDialogText();
			    
			    if (msg != null)
			      {
				System.err.println("Error deleting embedded " + invid + 
						   ", reason: " + msg);
			      }
			    else
			      {
				System.err.println("Error deleting previous embedded " + invid +
						   ", no reason given.");
			      }
			  }
		      }
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
   * <P>This private helper method takes a Vector of xInvid and
   * xmlobject objects (in the embedded object case) and returns
   * a Vector of Invid objects.  If any xmlobjects in the input
   * Vector did not map to pre-existing objects on the server,
   * then no invid will be returned for those elements, and as
   * a result, the returned vector may be smaller than the
   * input.</P>
   */ 

  private Vector getExtantInvids(Vector values)
  {
    Invid invid;
    Vector invids = new Vector();

    /* -- */

    if (values == null)
      {
	return invids;
      }

    // if we're an embedded object field, 

    for (int i=0; i < values.size(); i++)
      {
	Object x = values.elementAt(i);

	if (x instanceof xInvid)
	  {
	    invid = ((xInvid) x).getInvid();
	  }
	else if (x instanceof xmlobject)
	  {
	    invid = ((xmlobject) x).getInvid();
	  }
	else
	  {
	    System.err.println("Unrecognized XML element while processing Invid vector: " + x);
	    continue;
	  }

	if (invid != null)
	  {
	    invids.addElement(invid);
	  }
	else if (debug)
	  {
	    System.err.println("Couldn't find invid for " + x);
	  }
      }

    return invids;
  }

  /**
   * <P>This private helper method takes a Vector of xInvid and
   * xmlobject objects (in the embedded object case) and returns
   * a Vector of xmlobjects that exist on the server.  Any xInvid
   * objects in the input Vector, along with any xmlobject objects
   * which do not correspond to pre-existing objects on the server
   * will be omitted from the returned vector.</P>
   */ 

  private Vector getExtantObjects(Vector values)
  {
    Vector objects = new Vector();
    Invid invid;

    /* -- */

    if (values == null)
      {
	return objects;
      }

    // if we're an embedded object field, 

    for (int i=0; i < values.size(); i++)
      {
	Object x = values.elementAt(i);
	
	if (x instanceof xInvid)
	  {
	    continue;
	  }
	else if (x instanceof xmlobject)
	  {
	    if (((xmlobject) x).getInvid() != null)
	      {
		objects.addElement(x);
	      }
	    else if (debug)
	      {
		System.err.println("Couldn't find invid for " + x);
	      }
	  }
	else
	  {
	    System.err.println("Unrecognized XML element while processing Invid vector: " + x);
	    continue;
	  }
      }

    return objects;
  }

  /**
   * <P>This private helper method takes a Vector of xInvid and
   * xmlobject objects and returns a Vector of xInvids and xmlobjects
   * that could not be resolved on the server.</P>
   */

  private Vector getNonRegisteredObjects(Vector values)
  {
    Vector objects = new Vector();
    Invid invid;

    /* -- */

    if (values == null)
      {
	return objects;
      }

    for (int i=0; i < values.size(); i++)
      {
	Object x = values.elementAt(i);

	if (x instanceof xInvid)
	  {
	    invid = ((xInvid) x).getInvid();
	  }
	else if (x instanceof xmlobject)
	  {
	    invid = ((xmlobject) x).getInvid();
	  }
	else
	  {
	    System.err.println("Unrecognized XML element while processing Invid vector: " + x);
	    continue;
	  }

	if (invid == null)
	  {
	    objects.addElement(x);
	  }
      }

    return objects;
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
	    result.append(arlut.csd.Util.VectorUtils.vectorString(addValues));
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

  /**
   * <p>The numeric type id for the object type this xInvid is
   * meant to point to.</p>
   *
   * <p>In the XML file, this field is derived from the type attribute,
   * after doing an object type lookup on the server by way of
   * the Ganymede client {@link arlut.csd.ganymede.client.Loader Loader}
   * object.</p>
   */

  short typeId;

  /**
   * <p>The id string for this xInvid from the XML file.  Will
   * be used to resolve this xInvid to an actual {@link arlut.csd.ganymede.Invid Invid}
   * on the server, if set.</p>
   *
   * <p>In the XML file, this field is taken from the id attribute.</p>
   */

  String objectId;

  /**
   * <p>The numeric object id, if specified in the XML file for this
   * xInvid.</p>
   */

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
	throw new NullPointerException("Bad item!");
      }

    String typeString = item.getAttrStr("type");

    if (typeString == null)
      {
	System.err.println("Missing or malformed invid type in element: " + item);
	throw new NullPointerException("Bad item!");
      }

    objectId = item.getAttrStr("id");

    if (objectId == null)
      {
	Integer iNum = item.getAttrInt("num");

	if (iNum != null)
	  {
	    num = iNum.intValue();
	  }
	else
	  {
	    System.err.println("Unknown object target in invid field element: " + item);
	    throw new NullPointerException("Bad item!");
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
 *
 * <p>This class has three separate value fields, for the
 * possible password formats supported by Ganymede, but in fact
 * only one of them at a time should be anything other than
 * null, as setting any of these attributes on a Ganymede
 * password field clears the others.</p>
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

	if (item instanceof XMLEndDocument)
	  {
	    throw new RuntimeException("Ran into end of XML file while parsing permission object " + label);
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
