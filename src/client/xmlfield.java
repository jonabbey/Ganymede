/*
   xmlfield.java

   This class is a data holding structure that is intended to hold
   object and field data for an XML object element for xmlclient.

   --

   Created: 2 May 2000
   Version: $Revision: 1.2 $
   Last Mod Date: $Date: 2000/05/17 00:06:00 $
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
   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.  */

package arlut.csd.ganymede.client;

import arlut.csd.ganymede.*;
import arlut.csd.Util.*;

import org.xml.sax.*;
import java.util.*;
import java.text.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                        xmlfield

------------------------------------------------------------------------------*/

/**
 * <p>This class is a data holding structure that is intended to hold
 * object and field data for an XML object element for
 * {@link arlut.csd.ganymede.client.xmlclient xmlclient}.</p>
 *
 * @version $Revision: 1.2 $ $Date: 2000/05/17 00:06:00 $ $Name:  $
 * @author Jonathan Abbey
 */

public class xmlfield implements FieldType {

  static DateFormat formatter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss",
						     java.util.Locale.US);

  /**
   * <p>Definition record for this field type</p>
   */
  
  FieldTemplate fieldDef;

  Object value;			// if scalar
  Vector setValues;		// if vector
  Vector delValues;		// if vector
  Vector addValues;		// if vector

  /* -- */

  public xmlfield(xmlobject owner, XMLElement openElement) throws SAXException
  {
    XMLItem nextItem;

    String name = openElement.getName();
    fieldDef = owner.getFieldDef(name);

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

	Boolean bValue = parseBoolean(nextItem);

	if (bValue != null)
	  {
	    value = bValue;
	    return;
	  }
      }
    else if (fieldDef.getType() == FieldType.NUMERIC)
      {
	nextItem = xmlclient.xc.getNextItem();

	Integer iValue = parseNumeric(nextItem);

	if (iValue != null)
	  {
	    value = iValue;
	    return;
	  }
      }
    else if (fieldDef.getType() == FieldType.DATE)
      {
	nextItem = xmlclient.xc.getNextItem();

	Date dValue = parseDate(nextItem);

	if (dValue != null)
	  {
	    value = dValue;
	    return;
	  }
      }
    else if (fieldDef.getType() == FieldType.STRING)
      {
	if (!fieldDef.isArray())
	  {
	    value = xmlclient.xc.reader.getFollowingString(openElement, true);
	  }
	else
	  {
	    setValues = new Vector();
	    delValues = new Vector();
	    addValues = new Vector();

	    Stack modeStack = new Stack();

	    modeStack.push("set");
	    nextItem = xmlclient.xc.getNextItem();

	    while (!nextItem.matchesClose(openElement.getName()) && !(nextItem instanceof XMLEndDocument))
	      {
		if (nextItem.matches("add") && !nextItem.isEmpty())
		  {
		    modeStack.push("add");
		    nextItem = xmlclient.xc.getNextItem();
		  }
		else if (nextItem.matches("delete") && !nextItem.isEmpty())
		  {
		    modeStack.push("add");
		    nextItem = xmlclient.xc.getNextItem();
		  }
		else if (nextItem.matchesClose("add"))
		  {
		    if (modeStack.peek().equals("add"))
		      {
			modeStack.pop();
		      }
		    else
		      {
			System.err.println("Error, found a mismatched </add> while parsing a string list.");
		      }

		    nextItem = xmlclient.xc.getNextItem();
		  }
		else if (nextItem.matchesClose("delete"))
		  {
		    if (modeStack.peek().equals("delete"))
		      {
			modeStack.pop();
		      }
		    else
		      {
			System.err.println("Error, found a mismatched </delete> while parsing a string list.");
		      }

		    nextItem = xmlclient.xc.getNextItem();
		  }
		else
		  {
		    String element = parseStringVecItem(nextItem);
		    
		    if (element != null)
		      {
			if (modeStack.peek().equals("set"))
			  {
			    setValues.addElement(element);
			  }
			else if (modeStack.peek().equals("add"))
			  {
			    addValues.addElement(element);
			  }
			else if (modeStack.peek().equals("delete"))
			  {
			    delValues.addElement(element);
			  }
		      }
		    
		    nextItem = xmlclient.xc.getNextItem();
		  }
	      }
	  }
      }
    else if (fieldDef.getType() == FieldType.INVID)
      {
	if (!fieldDef.isArray())
	  {
	    nextItem = xmlclient.xc.getNextItem();

	    value = new xInvid(nextItem);
	  }
	else
	  {
	    setValues = new Vector();
	    delValues = new Vector();
	    addValues = new Vector();

	    Stack modeStack = new Stack();

	    modeStack.push("set");
	    nextItem = xmlclient.xc.getNextItem();

	    while (!nextItem.matchesClose(openElement.getName()) && !(nextItem instanceof XMLEndDocument))
	      {
		if (nextItem.matches("add") && !nextItem.isEmpty())
		  {
		    modeStack.push("add");
		    nextItem = xmlclient.xc.getNextItem();
		  }
		else if (nextItem.matches("delete") && !nextItem.isEmpty())
		  {
		    modeStack.push("add");
		    nextItem = xmlclient.xc.getNextItem();
		  }
		else if (nextItem.matchesClose("add"))
		  {
		    if (modeStack.peek().equals("add"))
		      {
			modeStack.pop();
		      }
		    else
		      {
			System.err.println("Error, found a mismatched </add> while parsing an invid list.");
		      }

		    nextItem = xmlclient.xc.getNextItem();
		  }
		else if (nextItem.matchesClose("delete"))
		  {
		    if (modeStack.peek().equals("delete"))
		      {
			modeStack.pop();
		      }
		    else
		      {
			System.err.println("Error, found a mismatched </delete> while parsing an invid list.");
		      }

		    nextItem = xmlclient.xc.getNextItem();
		  }
		else
		  {
		    xInvid invidElement = new xInvid(nextItem);
		    
		    if (invidElement != null)
		      {
			if (modeStack.peek().equals("set"))
			  {
			    setValues.addElement(invidElement);
			  }
			else if (modeStack.peek().equals("add"))
			  {
			    addValues.addElement(invidElement);
			  }
			else if (modeStack.peek().equals("delete"))
			  {
			    delValues.addElement(invidElement);
			  }
		      }
		    
		    nextItem = xmlclient.xc.getNextItem();
		  }
	      }
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

	try
	  {
	    xPassword pValue = new xPassword(nextItem);
	    value = pValue;
	    return;
	  }
	catch (NullPointerException ex)
	  {
	    ex.printStackTrace();
	  }
      }
    else if (fieldDef.getType() == FieldType.IP)
      {
	if (!fieldDef.isArray())
	  {
	    nextItem = xmlclient.xc.getNextItem();

	    value = parseIP(nextItem);
	  }
	else
	  {
	    setValues = new Vector();
	    delValues = new Vector();
	    addValues = new Vector();

	    Stack modeStack = new Stack();

	    modeStack.push("set");
	    nextItem = xmlclient.xc.getNextItem();

	    while (!nextItem.matchesClose(openElement.getName()) && !(nextItem instanceof XMLEndDocument))
	      {
		if (nextItem.matches("add") && !nextItem.isEmpty())
		  {
		    modeStack.push("add");
		    nextItem = xmlclient.xc.getNextItem();
		  }
		else if (nextItem.matches("delete") && !nextItem.isEmpty())
		  {
		    modeStack.push("add");
		    nextItem = xmlclient.xc.getNextItem();
		  }
		else if (nextItem.matchesClose("add"))
		  {
		    if (modeStack.peek().equals("add"))
		      {
			modeStack.pop();
		      }
		    else
		      {
			System.err.println("Error, found a mismatched </add> while parsing an invid list.");
		      }

		    nextItem = xmlclient.xc.getNextItem();
		  }
		else if (nextItem.matchesClose("delete"))
		  {
		    if (modeStack.peek().equals("delete"))
		      {
			modeStack.pop();
		      }
		    else
		      {
			System.err.println("Error, found a mismatched </delete> while parsing an invid list.");
		      }

		    nextItem = xmlclient.xc.getNextItem();
		  }
		else
		  {
		    String ipString = parseIP(nextItem);
		    
		    if (ipString != null)
		      {
			if (modeStack.peek().equals("set"))
			  {
			    setValues.addElement(ipString);
			  }
			else if (modeStack.peek().equals("add"))
			  {
			    addValues.addElement(ipString);
			  }
			else if (modeStack.peek().equals("delete"))
			  {
			    delValues.addElement(ipString);
			  }
		      }
		    
		    nextItem = xmlclient.xc.getNextItem();
		  }
	      }
	  }
      }
    else if (fieldDef.getType() == FieldType.FLOAT)
      {
	nextItem = xmlclient.xc.getNextItem();

	Double fValue = parseFloat(nextItem);

	if (fValue != null)
	  {
	    value = fValue;
	    return;
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
    
    while (!nextItem.matchesClose(name) && !(nextItem instanceof XMLEndDocument))
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
  Invid resolved;

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

    if (typeString == null || objectId == null)
      {
	System.err.println("Missing or malformed invid attribute in element: " + item);
	throw new NullPointerException("Bad item!");
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

    resolved = null;
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
    resolved = null;
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
   * <p>String describing this xPerm's contents.</p>
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
