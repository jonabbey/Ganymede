/*
   xmlfield.java

   This class is a data holding structure that is intended to hold
   object and field data for an XML object element for xmlclient.

   --

   Created: 2 May 2000

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2013
   The University of Texas at Austin

   Ganymede is a registered trademark of The University of Texas at Austin

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
   along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/

package arlut.csd.ganymede.server;

import java.rmi.RemoteException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Stack;
import java.util.Vector;

import org.xml.sax.SAXException;

import arlut.csd.Util.TranslationService;
import arlut.csd.Util.VectorUtils;
import arlut.csd.Util.XMLCharData;
import arlut.csd.Util.XMLElement;
import arlut.csd.Util.XMLEndDocument;
import arlut.csd.Util.XMLItem;
import arlut.csd.Util.XMLUtils;
import arlut.csd.ganymede.common.FieldTemplate;
import arlut.csd.ganymede.common.FieldType;
import arlut.csd.ganymede.common.Invid;
import arlut.csd.ganymede.common.NotLoggedInException;
import arlut.csd.ganymede.common.PermEntry;
import arlut.csd.ganymede.common.ReturnVal;
import arlut.csd.ganymede.common.SyncPrefEnum;
import arlut.csd.ganymede.rmi.pass_field;
import arlut.csd.ganymede.rmi.perm_field;
import arlut.csd.ganymede.rmi.field_option_field;

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
 * @author Jonathan Abbey
 */

public class xmlfield implements FieldType {

  final static boolean debug = false;

  /**
   * <p>TranslationService object for handling string localization in
   * the Ganymede server.</p>
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.server.xmlfield");

  /**
   * <p>Array of formatters that we use for generating and parsing
   * date fields in Ganymede XML files</p>
   */

  static private final DateFormat[] formatters = new DateFormat[6];

  static
  {
    // 0 = mail-style date with timezone
    // 1 = mail-style date no timezone
    // 2 = UNIX date output, with timezone "Tue Jul 11 01:04:55 CDT 2000"
    // 3 = UNIX date output, without timezone
    // 4 = no-comma style 0
    // 5 = no-comma style 1

    formatters[0] = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
    formatters[1] = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss");
    formatters[2] = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy");
    formatters[3] = new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy");
    formatters[4] = new SimpleDateFormat("EEE dd MMM yyyy HH:mm:ss z");
    formatters[5] = new SimpleDateFormat("EEE dd MMM yyyy HH:mm:ss");
  }

  /**
   * <p>constant string for the addIfNotPresent mode</p>
   */

  static final String ADDIFNOTPRESENT = "addIfNotPresent";

  /**
   * <p>constant string for the add mode</p>
   */

  static final String ADD = "add";

  /**
   * <p>constant string for the set mode</p>
   */

  static final String SET = "set";

  /**
   * <p>constant string for the delete mode</p>
   */

  static final String DELETE = "delete";

  /**
   * <p>Definition record for this field type</p>
   */

  FieldTemplate fieldDef;

  /**
   * <p>The xmlobject that contains us.</p>
   */

  xmlobject owner;

  // the following hold data values for this field

  Object value = null;          // if scalar
  Vector setValues = null;      // if vector
  Vector delValues = null;      // if vector
  Vector addValues = null;      // if vector
  Vector addIfNotPresentValues = null; // if vector

  /* -- */

  public xmlfield(xmlobject owner, XMLElement openElement) throws SAXException
  {
    XMLItem nextItem;

    /* -- */

    this.owner = owner;
    String elementName = openElement.getName();
    fieldDef = owner.getFieldDef(elementName);

    //    owner.xSession.err.println("parsing " + openElement);

    if (fieldDef == null)
      {
        owner.xSession.err.println("\nDid not recognize field " + elementName + " in object " + owner);

        if (openElement.isEmpty())
          {
            throw new NullPointerException("void field def for field element " + elementName);
          }
        else
          {
            skipToEndField(elementName);

            throw new NullPointerException("void field def for field element " + elementName);
          }
      }

    if (fieldDef.getType() == FieldType.BOOLEAN)
      {
        if (openElement.isEmpty())
          {
            value = null;
            return;
          }

        nextItem = owner.xSession.getNextItem();

        if (nextItem.matchesClose(elementName))
          {
            value = null;
            return;
          }
        else
          {
            if (nextItem.matches(ADD) || nextItem.matches(ADDIFNOTPRESENT) || nextItem.matches(DELETE))
              {
                // "Error, can''t use vector operator {0} in scalar field: {1}."

                throw new RuntimeException(ts.l("constructor.scalar_error", nextItem.toString(), fieldDef.getName()));
              }

            value = parseBoolean(nextItem);

            // fall through to skipToEndField()
          }
      }
    else if (fieldDef.getType() == FieldType.NUMERIC)
      {
        if (openElement.isEmpty())
          {
            value = null;
            return;
          }

        nextItem = owner.xSession.getNextItem();

        if (nextItem.matchesClose(elementName))
          {
            value = null;
            return;
          }
        else
          {
            if (nextItem.matches(ADD) || nextItem.matches(ADDIFNOTPRESENT) || nextItem.matches(DELETE))
              {
                // "Error, can''t use vector operator {0} in scalar field: {1}."

                throw new RuntimeException(ts.l("constructor.scalar_error", nextItem.toString(), fieldDef.getName()));
              }

            Integer iValue = parseNumeric(nextItem);

            if (iValue != null)
              {
                value = iValue;
              }

            // fall through to skipToEndField()
          }
      }
    else if (fieldDef.getType() == FieldType.DATE)
      {
        if (openElement.isEmpty())
          {
            value = null;
            return;
          }

        nextItem = owner.xSession.getNextItem();

        if (nextItem.matchesClose(elementName))
          {
            // <field></field> == clear the value

            value = null;
            return;
          }
        else
          {
            // owner.xSession.err.println("Parsing date for item " + nextItem);

            if (nextItem.matches(ADD) || nextItem.matches(ADDIFNOTPRESENT) || nextItem.matches(DELETE))
              {
                // "Error, can''t use vector operator {0} in scalar field: {1}."

                throw new RuntimeException(ts.l("constructor.scalar_error", nextItem.toString(), fieldDef.getName()));
              }

            Date dValue = parseDate(nextItem);

            // parseDate will always return a non-null value, or
            // a RuntimeException if the date element couldn't
            // be parsed

            if (dValue != null)
              {
                value = dValue;
              }

            // fall through to skipToEndField()
          }
      }
    else if (fieldDef.getType() == FieldType.STRING)
      {
        if (!fieldDef.isArray())
          {
            if (openElement.isEmpty())
              {
                value = null;
                return;
              }

            nextItem = owner.xSession.reader.peekNextItem();

            if (nextItem.matchesClose(elementName))
              {
                nextItem = owner.xSession.getNextItem(); // consume it

                value = null;
                return;
              }
            else if (nextItem.matches("string"))
              {
                // we've got a <string>-encoded scalar rather than
                // free-standing plain text

                nextItem = owner.xSession.getNextItem(); // consume it
                value = parseStringVecItem(nextItem);

                // fall through to skipToEndField() below
              }
            else if (nextItem.matches(ADD) || nextItem.matches(ADDIFNOTPRESENT) || nextItem.matches(DELETE))
              {
                // "Error, can''t use vector operator {0} in scalar field: {1}."

                throw new RuntimeException(ts.l("constructor.scalar_error", nextItem.toString(), fieldDef.getName()));
              }
            else
              {
                value = owner.xSession.reader.getFollowingString(openElement, false);

                if (value != null)
                  {
                    value = ((String) value).intern();
                  }

                // getFollowingString automatically consumes the field
                // close element after the string text

                return;
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
    else if (fieldDef.getType() == FieldType.INVID)
      {
        if (!fieldDef.isArray())
          {
            if (openElement.isEmpty())
              {
                value = null;
                return;
              }

            nextItem = owner.xSession.getNextItem();

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

                if (nextItem.matches(ADD) || nextItem.matches(ADDIFNOTPRESENT) || nextItem.matches(DELETE))
                  {
                    // "Error, can''t use vector operator {0} in scalar field: {1}."
                    throw new RuntimeException(ts.l("constructor.scalar_error", nextItem.toString(), fieldDef.getName()));
                  }

                value = new xInvid(nextItem);

                // fall through to skipToEndField()
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
        nextItem = owner.xSession.getNextItem();

        if (!nextItem.matches("permissions"))
          {
            owner.xSession.err.println("\nUnrecognized tag while parsing data for a permissions field: " + nextItem);

            skipToEndField(elementName);

            throw new NullPointerException("void field def");
          }
        else
          {
            setValues = new Vector();

            nextItem = owner.xSession.getNextItem();

            while (!nextItem.matchesClose("permissions") && !(nextItem instanceof XMLEndDocument))
              {
                xPerm permElement = new xPerm(nextItem, true);

                if (permElement != null)
                  {
                    setValues.addElement(permElement);
                  }

                nextItem = owner.xSession.getNextItem();
              }

            if (nextItem instanceof XMLEndDocument)
              {
                throw new RuntimeException("Ran into end of XML file while processing permission field " + elementName);
              }

            // fall through to skipToEndField()
          }
      }
    else if (fieldDef.getType() == FieldType.PASSWORD)
      {
        if (openElement.isEmpty())
          {
            value = null;
            return;
          }

        nextItem = owner.xSession.getNextItem();

        if (nextItem.matchesClose(elementName))
          {
            value = null;
            return;
          }
        else
          {
            if (nextItem.matches(ADD) || nextItem.matches(ADDIFNOTPRESENT) || nextItem.matches(DELETE))
              {
                // "Error, can''t use vector operator {0} in scalar field: {1}."
                throw new RuntimeException(ts.l("constructor.scalar_error", nextItem.toString(), fieldDef.getName()));
              }

            try
              {
                value = new xPassword(nextItem);
              }
            catch (NullPointerException ex)
              {
                Ganymede.logError(ex);
              }

            // fall through to skipToEndField()
          }
      }
    else if (fieldDef.getType() == FieldType.IP)
      {
        if (!fieldDef.isArray())
          {
            nextItem = owner.xSession.getNextItem();

            if (nextItem.matchesClose(elementName))
              {
                value = null;
                return;
              }
            else
              {
                if (nextItem.matches(ADD) || nextItem.matches(ADDIFNOTPRESENT) || nextItem.matches(DELETE))
                  {
                    // "Error, can''t use vector operator {0} in scalar field: {1}."
                    throw new RuntimeException(ts.l("constructor.scalar_error", nextItem.toString(), fieldDef.getName()));
                  }

                value = parseIP(nextItem);

                // fall through to skipToEndField()
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
        nextItem = owner.xSession.getNextItem();

        if (nextItem.matchesClose(elementName))
          {
            value = null;
            return;
          }
        else
          {
            if (nextItem.matches(ADD) || nextItem.matches(ADDIFNOTPRESENT) || nextItem.matches(DELETE))
              {
                // "Error, can''t use vector operator {0} in scalar field: {1}."
                throw new RuntimeException(ts.l("constructor.scalar_error", nextItem.toString(), fieldDef.getName()));
              }

            Double fValue = parseFloat(nextItem);

            if (fValue != null)
              {
                value = fValue;
              }

            // fall through to skipToEndField()
          }
      }
    else if (fieldDef.getType() == FieldType.FIELDOPTIONS)
      {
        nextItem = owner.xSession.getNextItem();

        if (!nextItem.matches("options"))
          {
            owner.xSession.err.println("\nUnrecognized tag while parsing data for a field options field: " + nextItem);

            skipToEndField(elementName);

            throw new NullPointerException("void field def");
          }
        else
          {
            setValues = new Vector();

            nextItem = owner.xSession.getNextItem();

            while (!nextItem.matchesClose("options") && !(nextItem instanceof XMLEndDocument))
              {
                xOption optionElement = new xOption(nextItem, true);

                if (optionElement != null)
                  {
                    setValues.addElement(optionElement);
                  }

                nextItem = owner.xSession.getNextItem();
              }

            if (nextItem instanceof XMLEndDocument)
              {
                throw new RuntimeException("Ran into end of XML file while processing options field " + elementName);
              }

            // fall through to skipToEndField()
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

    /**
       modeStack is used to keep track of what vector mode we are currently in..
       the list of reasonable values are

       "addIfNotPresent" -- Add those elements listed that are not already there
       "add" -- Add, although with invids we will ignore adds if they are already present
       "set" -- Set the list of elements to the provided list
       "delete" -- Delete the listed elements

    */

    Stack modeStack = new Stack();

    /* -- */

    // by default, we add

    modeStack.push(ADDIFNOTPRESENT);

    nextItem = owner.xSession.getNextItem();

    while (!nextItem.matchesClose(openElement.getName()) && !(nextItem instanceof XMLEndDocument))
      {
        if (((nextItem.matches(ADDIFNOTPRESENT) || nextItem.matches(ADD) ||
              nextItem.matches(DELETE)) && !nextItem.isEmpty()))
          {
            if (setMode)
              {
                owner.xSession.err.println("\nxmlclient: error, can't enter " + nextItem.getName() +
                                   " mode with a previous <set> directive in field " + openElement);

                throw new RuntimeException("xmlclient: error, can't enter " + nextItem.getName() +
                                           " mode with a previous <set> directive in field " + openElement);
              }

            canDoSetMode = false;
            modeStack.push(nextItem.getName());

            nextItem = owner.xSession.getNextItem();
          }
        else if (nextItem.matches(SET))
          {
            if (canDoSetMode)
              {
                setMode = true;
                setValues = new Vector();

                if (!nextItem.isEmpty())
                  {
                    modeStack.push(SET);
                  }
              }
            else
              {
                owner.xSession.err.println("\nxmlclient: error, can't enter set" +
                                   " mode with a previous mode directive in field " + openElement);

                throw new RuntimeException("xmlclient: error, can't enter set" +
                                           " mode with a previous mode directive in field " + openElement);
              }

            nextItem = owner.xSession.getNextItem();
          }
        else if (nextItem.matchesClose(ADD) || nextItem.matchesClose(DELETE) || nextItem.matchesClose(ADDIFNOTPRESENT))
          {
            if (modeStack.size() > 1 && modeStack.peek().equals(nextItem.getName()))
              {
                // we checked for modeStack.size() > 1 to cover the
                // initial modeStack.push(ADDIFNOTPRESENT)

                modeStack.pop();
              }
            else
              {
                owner.xSession.err.println("\nError, found a mismatched </" +
                                   nextItem.getName() + "> while parsing a vector field.");

                throw new RuntimeException("Error, found a mismatched </" +
                                           nextItem.getName() + "> while parsing a vector field.");
              }

            nextItem = owner.xSession.getNextItem();
          }
        else if (nextItem.matchesClose(SET))
          {
            // okay.. we're actually not going to do anything
            // here, because set mode is really exclusive within
            // a field definition

            if (modeStack.peek().equals(nextItem.getName()))
              {
                modeStack.pop();
              }

            nextItem = owner.xSession.getNextItem();
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

                    newValue = new xmlobject((XMLElement) nextItem, owner.xSession, this);
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
                else if (modeStack.peek().equals(ADD))
                  {
                    if (addValues == null)
                      {
                        addValues = new Vector();
                      }

                    canDoSetMode = false;

                    addValues.addElement(newValue);
                  }
                else if (modeStack.peek().equals(ADDIFNOTPRESENT))
                  {
                    if (addIfNotPresentValues == null)
                      {
                        addIfNotPresentValues = new Vector();
                      }

                    canDoSetMode = false;

                    addIfNotPresentValues.addElement(newValue);
                  }
                else if (modeStack.peek().equals(DELETE))
                  {
                    if (delValues == null)
                      {
                        delValues = new Vector();
                      }

                    canDoSetMode = false;

                    delValues.addElement(newValue);
                  }
              }
            else
              {
                owner.xSession.err.println("xmlfield WARNING: couldn't get vector value for " + nextItem +
                                   "in xml field object " + openElement);
              }

            nextItem = owner.xSession.getNextItem();
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
    XMLItem nextItem = owner.xSession.getNextItem();

    while (!(nextItem.matchesClose(elementName) || (nextItem instanceof XMLEndDocument)))
      {
        nextItem = owner.xSession.getNextItem();
      }

    if (nextItem instanceof XMLEndDocument)
      {
        owner.xSession.err.println("\nRan into end of XML file while processing field " + elementName);
        throw new RuntimeException("Ran into end of XML file while processing field " + elementName);
      }
  }

  public Boolean parseBoolean(XMLItem item) throws SAXException
  {
    if (item instanceof XMLCharData)
      {
        String val = item.getString();

        if (val.equalsIgnoreCase("true") || val.equalsIgnoreCase("t"))
          {
            return Boolean.TRUE;
          }
        else if (val.equalsIgnoreCase("false") || val.equalsIgnoreCase("f"))
          {
            return Boolean.FALSE;
          }
        else
          {
            throw new RuntimeException("\nUnrecognized character string found when boolean expected: " + item);
          }
      }

    if (!item.matches("boolean"))
      {
        throw new RuntimeException("\nUnrecognized XML item found when boolean expected: " + item);
      }

    if (!item.isEmpty())
      {
        owner.xSession.err.println("\nError, found a non-empty boolean field value element: " + item);
      }

    return Boolean.valueOf(item.getAttrBoolean("val"));
  }

  public Integer parseNumeric(XMLItem item) throws SAXException
  {
    if (item instanceof XMLCharData)
      {
        String val = item.getString();

        try
          {
            return Integer.valueOf(val);
          }
        catch (NumberFormatException ex)
          {
            owner.xSession.err.println("\nUnrecognized character string found when integer numeric value expected: " + item);
            return null;
          }
      }

    if (!item.matches("int"))
      {
        owner.xSession.err.println("\nUnrecognized XML item found when int expected: " + item);
        return null;
      }

    if (!item.isEmpty())
      {
        owner.xSession.err.println("\nError, found a non-empty int field value element: " + item);
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
        throw new RuntimeException("\nUnrecognized XML item found when date expected: " + item);
      }

    if (!item.isEmpty())
      {
        owner.xSession.err.println("\nError, found a non-empty date field value element: " + item);
      }

    formattedDate = item.getAttrStr("val");

    if (formattedDate != null)
      {
        for (int i = 0; i < formatters.length && result1 == null; i++)
          {
            try
              {
                synchronized (formatters[i])
                  {
                    result1 = formatters[i].parse(formattedDate);
                  }
              }
            catch (ParseException ex)
              {
              }
          }

        if (result1 == null)
          {
            owner.xSession.err.println("\nError, could not parse date entity val " + formattedDate + " in element " + item);
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
            owner.xSession.err.println("\nError, could not parse date numeric timecode " +
                               timecodeStr + " in element " + item);
            owner.xSession.err.println(ex.getMessage());
          }
      }

    if (result2 != null && result1 != null)
      {
        // test to see if the two time stamps are within a second or
        // so of each other.. the Ganymede client doesn't always
        // round times to the nearest second when setting times
        // in date fields, but the time string in 'val', if present,
        // will only have second resolution

        long timediff = result2.getTime() - result1.getTime();

        if (timediff < -1000 || timediff > 1000)
          {
            owner.xSession.err.println("\nWarning, date element " + item + " is not internally consistent.");
            owner.xSession.err.println("Ignoring date string \"" + formattedDate + "\", which was parsed as ");

            synchronized (formatters[0])
              {
                owner.xSession.err.println(formatters[0].format(result1));
                owner.xSession.err.println("Using timecode data string \"" + formatters[0].format(result2) + "\".");
              }
          }

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
        owner.xSession.err.println("\nUnrecognized XML item found when vector string element expected: " + item);
        return null;
      }

    if (!item.isEmpty())
      {
        owner.xSession.err.println("\nError, found a non-empty vector string element: " + item);
      }

    String result = item.getAttrStr("val");

    if (result != null)
      {
        result = result.intern();
      }

    return result;
  }

  public String parseIP(XMLItem item) throws SAXException
  {
    if (!item.matches("ip"))
      {
        owner.xSession.err.println("\nUnrecognized XML item found when ip expected: " + item);
        return null;
      }

    if (!item.isEmpty())
      {
        owner.xSession.err.println("\nError, found a non-empty ip field value element: " + item);
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
        owner.xSession.err.println("\nUnrecognized XML item found when float expected: " + item);
        return null;
      }

    if (!item.isEmpty())
      {
        owner.xSession.err.println("\nError, found a non-empty float field value element: " + item);
      }

    valStr = item.getAttrStr("val");

    if (valStr == null)
      {
        owner.xSession.err.println("\nError, float element " + item + " has no val attribute.");
        return null;
      }

    try
      {
        result = java.lang.Double.valueOf(valStr);
      }
    catch (NumberFormatException ex)
      {
        owner.xSession.err.println("\nError, float element " + item + " has a malformed val attribute.");
        return null;
      }

    return result;
  }

  /**
   * <p>This method is responsible for propagating this field's data
   * values to the server.</p>
   *
   * <p>Returns a {@link arlut.csd.ganymede.common.ReturnVal ReturnVal}
   * indicating the result of the server
   * operation.  This result may be null on success, or it may
   * be an encoded success or failure message in the normal
   * arlut.csd.ganymede.common.ReturnVal way.</p>
   *
   * <p>This method will throw an exception if the xmlobject that
   * contains this xmlfield has not established a remote
   * {@link arlut.csd.ganymede.rmi.db_object db_object} reference to
   * the server through which the editing can be performed.</p>
   */

  public ReturnVal registerOnServer()
  {
    ReturnVal result = null;

    /* -- */

    if (debug)
      {
        owner.xSession.err.println("Registering field " + this.toString());
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
            DBField field = (DBField) owner.objref.getField(fieldDef.getID());

            if (setValues != null)
              {
                // delete any values that are currently in the field
                // but which are not in our setValues vector, then add
                // any that are missing

                Vector currentValues = field.getValuesLocal();
                Vector removeValues = VectorUtils.minus(currentValues, setValues);
                Vector newValues = VectorUtils.minus(setValues, currentValues);

                if (removeValues.size() != 0)
                  {
                    result = field.deleteElements(removeValues);

                    if (!ReturnVal.didSucceed(result))
                      {
                        return result;
                      }
                  }

                if (newValues.size() > 0)
                  {
                    return ReturnVal.merge(result, field.addElements(newValues));
                  }
                else
                  {
                    // skip a pointless server call if we are doing a
                    // <set></set> to clear the field, or if we have
                    // already synchronized the field by deleting
                    // elements

                    return ReturnVal.merge(result, null);
                  }
              }
            else
              {
                if (addValues != null)
                  {
                    result = field.addElements(addValues);

                    if (!ReturnVal.didSucceed(result))
                      {
                        return result;
                      }
                  }

                if (addIfNotPresentValues != null)
                  {
                    Vector newValues = VectorUtils.difference(addIfNotPresentValues, field.getValuesLocal());

                    if (newValues.size() != 0)
                      {
                        result = field.addElements(newValues);

                        if (!ReturnVal.didSucceed(result))
                          {
                            return result;
                          }
                      }
                  }

                if (delValues != null)
                  {
                    result = field.deleteElements(delValues);

                    if (!ReturnVal.didSucceed(result))
                      {
                        return result;
                      }
                  }
              }
          }
        else if (fieldDef.isPassword())
          {
            xPassword xp = (xPassword) value;
            PasswordDBField field = (PasswordDBField) owner.objref.getField(fieldDef.getID());

            if (xp == null)
              {
                return field.setUndefined(false);
              }

            // set anything we can.. note that if we transmit null for
            // any of the password hash options, it will null the
            // password out entirely, so we don't want to transmit a
            // null unless all password options are all null.

            if (xp.plaintext != null)
              {
                // setting plaintext will cause the server to generate
                // all other hashes, so we will just return here

                return field.setPlainTextPass(xp.plaintext);
              }

            // okay, set whatever hashes we were given.. note that if
            // we see something like <password/>, with no attributes
            // set, we'll wind up clearing the password field entirely

            return field.setAllHashes(xp.crypttext, xp.md5text,
                                      xp.apachemd5text, xp.lanman,
                                      xp.ntmd4, xp.sshatext, xp.shaunixcrypt, xp.bcrypt,
                                      false, false);
          }
        else if (fieldDef.isInvid())
          {
            if (!fieldDef.isArray())
              {
                // scalar invid fields are never embedded/editInPlace

                xInvid invidValue = (xInvid) value;

                if (invidValue == null)
                  {
                    return owner.objref.setFieldValue(fieldDef.getID(), null);
                  }

                return owner.objref.setFieldValue(fieldDef.getID(), invidValue.getInvid());
              }
            else if (!fieldDef.isEditInPlace())
              {
                InvidDBField field = (InvidDBField) owner.objref.getField(fieldDef.getID());

                /* -- */

                /* note that we use VectorUtils.difference() here
                   rather than VectorUtils.minus(), as we don't allow
                   duplicate invid's in an invid field. */

                if (setValues != null)
                  {
                    Vector currentValues = field.getValuesLocal();
                    Vector invidValues = getExtantInvids(setValues);
                    Vector removeValues = VectorUtils.difference(currentValues, invidValues);
                    Vector newValues = VectorUtils.difference(invidValues, currentValues);

                    if (removeValues.size() > 0)
                      {
                        result = field.deleteElements(removeValues);

                        if (!ReturnVal.didSucceed(result))
                          {
                            return result;
                          }
                      }

                    if (newValues.size() > 0)
                      {
                        return ReturnVal.merge(result, field.addElements(newValues));
                      }
                    else
                      {
                        // skip a pointless server call if we are doing a
                        // <set></set> to clear the field, or if we have
                        // already synchronized the field by deleting
                        // elements

                        return ReturnVal.merge(result, null);
                      }
                  }
                else
                  {
                    if (addIfNotPresentValues != null)
                      {
                        Vector invidValues = getExtantInvids(addIfNotPresentValues);
                        Vector newValues = VectorUtils.difference(invidValues, field.getValuesLocal());

                        if (newValues.size() != 0)
                          {
                            result = ReturnVal.merge(result, field.addElements(newValues));

                            if (!ReturnVal.didSucceed(result))
                              {
                                return result;
                              }
                          }
                      }

                    if (addValues != null)
                      {
                        result = ReturnVal.merge(result, field.addElements(getExtantInvids(addValues)));

                        if (!ReturnVal.didSucceed(result))
                          {
                            return result;
                          }
                      }

                    if (delValues != null)
                      {
                        result = ReturnVal.merge(result, field.deleteElements(getExtantInvids(delValues)));

                        if (!ReturnVal.didSucceed(result))
                          {
                            return result;
                          }
                      }
                  }
              }
            else                // *** edit in place / embedded object case ***
              {
                InvidDBField field = (InvidDBField) owner.objref.getField(fieldDef.getID());

                /* -- */

                Vector currentValues = field.getValuesLocal();
                Vector needToBeEdited = null;
                Vector needToBeCreated = null;
                Vector needToBeRemoved = null;

                if (setValues != null)
                  {
                    needToBeEdited = getExtantObjects(setValues);
                    needToBeCreated = getNonRegisteredObjects(setValues);
                    needToBeRemoved = VectorUtils.difference(currentValues, getExtantInvids(setValues));
                  }
                else
                  {
                    if (addIfNotPresentValues != null || addValues != null)
                      {
                        needToBeCreated = VectorUtils.union(getNonRegisteredObjects(addIfNotPresentValues),
                                                            getNonRegisteredObjects(addValues));
                        needToBeEdited = VectorUtils.union(getExtantObjects(addIfNotPresentValues),
                                                           getExtantObjects(addValues));
                      }

                    if (delValues != null)
                      {
                        needToBeRemoved = getExtantInvids(delValues);
                      }
                  }

                if (needToBeCreated != null)
                  {
                    if (debug)
                      {
                        owner.xSession.err.println("Need to create " + needToBeCreated.size() + " embedded objects");
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
                            owner.xSession.err.println("Creating embedded object " + object);
                          }

                        result = ReturnVal.merge(result, field.createNewEmbedded());

                        if (!ReturnVal.didSucceed(result))
                          {
                            String msg = result.getDialogText();

                            if (msg != null)
                              {
                                owner.xSession.err.println("Error creating new embedded " + object + ", reason: " + msg);
                              }
                            else
                              {
                                owner.xSession.err.println("Error creating " + object + ", no reason given.");
                              }
                          }
                        else
                          {
                            object.setInvid(result.getInvid());
                            object.objref = result.getObject();

                            // now that we've copied the object
                            // carrier info out, clear the return val
                            // so that the ReturnVal.merge() will work
                            // properly the next time through the
                            // loop.

                            result.setInvid(null);
                            result.setObject(null);

                            // store this embedded object so we can
                            // resolve xinvid references to it

                            if (!owner.xSession.storeObject(object))
                              {
                                // "ERROR: Ran into a name conflict when attempting to record an embedded xml object: {0}"
                                throw new RuntimeException(ts.l("registerOnServer.conflict_resolving", object.toString()));
                              }
                          }

                        // remember that we created this embedded
                        // object, so that we can refer to it
                        // elsewhere by its id

                        owner.xSession.rememberEmbeddedObject(object);

                        // register any non-invids on this embedded
                        // object.. this will trigger the creation of
                        // any more embedded objects recursively if
                        // need be

                        result = ReturnVal.merge(result, object.registerFields(0));

                        if (!ReturnVal.didSucceed(result))
                          {
                            return result;
                          }
                      }
                  }

                if (needToBeEdited != null)
                  {
                    if (debug)
                      {
                        owner.xSession.err.println("Need to edit " + needToBeEdited.size() + " embedded objects");
                      }

                    for (int i = 0; i < needToBeEdited.size(); i++)
                      {
                        xmlobject object = (xmlobject) needToBeEdited.elementAt(i);

                        if (debug)
                          {
                            owner.xSession.err.println("Editing embedded object " + object);
                          }

                        result = ReturnVal.merge(result, object.editOnServer(owner.xSession.session));

                        if (!ReturnVal.didSucceed(result))
                          {
                            String msg = result.getDialogText();

                            if (msg != null)
                              {
                                owner.xSession.err.println("Error editing previous embedded " + object +
                                                   ", reason: " + msg);
                              }
                            else
                              {
                                owner.xSession.err.println("Error editing previous embedded " + object +
                                                   ", no reason given.");
                              }
                          }

                        // remember that we edited this embedded
                        // object so that we can fixup any invids
                        // after all is said and done

                        owner.xSession.rememberEmbeddedObject(object);

                        // register any non-invids on this embedded
                        // object.. this will trigger the creation of
                        // any more embedded objects recursively if
                        // need be

                        result = ReturnVal.merge(result, object.registerFields(0));

                        if (!ReturnVal.didSucceed(result))
                          {
                            return result;
                          }
                      }
                  }

                if (needToBeRemoved != null)
                  {
                    if (debug)
                      {
                        owner.xSession.err.println("Need to remove " + needToBeRemoved.size() + " embedded objects");
                      }

                    for (int i = 0; i < needToBeRemoved.size(); i++)
                      {
                        Invid invid = (Invid) needToBeRemoved.elementAt(i);

                        result = ReturnVal.merge(result, field.deleteElement(invid));

                        if (!ReturnVal.didSucceed(result))
                          {
                            String msg = result.getDialogText();

                            if (msg != null)
                              {
                                owner.xSession.err.println("Error deleting embedded " + invid +
                                                   ", reason: " + msg);
                              }
                            else
                              {
                                owner.xSession.err.println("Error deleting previous embedded " + invid +
                                                   ", no reason given.");
                              }

                            return result;
                          }
                      }

                    return result;
                  }
              }
          }
        else if (fieldDef.isFieldOptions())
          {
            field_option_field field = (field_option_field) owner.objref.getField(fieldDef.getID());

            if (setValues != null)
              {
                // first, clear out any options set

                field.resetOptions();

                // now set the options

                for (int i = 0; i < setValues.size(); i++)
                  {
                    xOption option = (xOption) setValues.elementAt(i);

                    short baseId = owner.xSession.getTypeNum(option.getName());

                    result = ReturnVal.merge(result, field.setOption(baseId, option.getOption()));

                    if (!ReturnVal.didSucceed(result))
                      {
                        return result;
                      }

                    if (option.fields != null)
                      {
                        for (xOption fieldOption: option.fields.values())
                          {
                            Hashtable<String, FieldTemplate> fieldHash = owner.xSession.getFieldHash(option.getName());

                            if (fieldHash == null)
                              {
                                owner.xSession.err.println("Error, can't process field options for object base " +
                                                   XMLUtils.XMLDecode(option.getName()) + ", base not found.");
                                return new ReturnVal(false);
                              }

                            FieldTemplate optionFieldDef = owner.xSession.getObjectFieldType(fieldHash, fieldOption.getName());

                            if (optionFieldDef == null)
                              {
                                owner.xSession.err.println("Error, can't process field options for field " +
                                                   XMLUtils.XMLDecode(fieldOption.getName()) + " in object base " +
                                                   XMLUtils.XMLDecode(option.getName()) + ", base not found.");
                                return new ReturnVal(false);
                              }

                            result = ReturnVal.merge(result,
                                                     field.setOption(baseId,
                                                                     optionFieldDef.getID(),
                                                                     fieldOption.getOption()));

                            if (!ReturnVal.didSucceed(result))
                              {
                                return result;
                              }
                          }
                      }
                  }
              }

            return null;        // success!
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

                    short baseId = owner.xSession.getTypeNum(perm.getName());

                    result = field.setPerm(baseId, perm.getPermEntry());

                    if (!ReturnVal.didSucceed(result))
                      {
                        return result;
                      }

                    if (perm.fields != null)
                      {
                        for (xPerm fieldPerm: perm.fields.values())
                          {
                            Hashtable<String, FieldTemplate> fieldHash = owner.xSession.getFieldHash(perm.getName());

                            if (fieldHash == null)
                              {
                                owner.xSession.err.println("Error, can't process field permissions for object base " +
                                                   XMLUtils.XMLDecode(perm.getName()) + ", base not found.");
                                return new ReturnVal(false);
                              }

                            FieldTemplate permFieldDef = owner.xSession.getObjectFieldType(fieldHash, fieldPerm.getName());

                            if (permFieldDef == null)
                              {
                                owner.xSession.err.println("Error, can't process field permissions for field " +
                                                   XMLUtils.XMLDecode(fieldPerm.getName()) + " in object base " +
                                                   XMLUtils.XMLDecode(perm.getName()) + ", base not found.");
                                return new ReturnVal(false);
                              }

                            result = field.setPerm(baseId, permFieldDef.getID(), fieldPerm.getPermEntry());

                            if (!ReturnVal.didSucceed(result))
                              {
                                return result;
                              }
                          }
                      }
                  }
              }

            return null;        // success!
          }
      }
    catch (RemoteException ex)
      {
        Ganymede.logError(ex);
        throw new RuntimeException(ex.getMessage());
      }

    return null;
  }

  /**
   * <p>This method is used by the {@link
   * arlut.csd.ganymede.server.GanymedeXMLSession} to cause this field
   * to attempt to do lookups on all labeled xInvids in this field, in
   * an attempt to get the Invids for them.  If a lookup cannot be
   * resolved when this method is called, it will be left unresolved
   * for a later round, after we have created the objects we need to
   * create.</p>
   *
   * <p>Note in the code for this method that we don't care about the
   * actual invids returned, we're just wanting to make sure that we
   * try to look them up on the server at this point in time, before
   * we apply any object renaming in the processing of our containing
   * xml transaction.</p>
   */

  public void dereferenceInvids() throws NotLoggedInException
  {
    if (getType() != FieldType.INVID)
      {
        // "dereferenceInvids() called on a non-Invid field."
        throw new RuntimeException(ts.l("dereferenceInvids.bad_type"));
      }

    if (fieldDef.isEditInPlace())
      {
        throw new RuntimeException("ASSERT: dereferenceInvids() called on an embedded object field.");
      }

    try
      {
        if (!isArray())
          {
            if (value != null)
              {
                ((xInvid) value).getInvid(false); // try to resolve
              }
          }
        else
          {
            if (setValues != null)
              {
                for (int i = 0; i < setValues.size(); i++)
                  {
                    xInvid xi = (xInvid) setValues.elementAt(i);
                    xi.getInvid(false); // try to resolve
                  }
              }

            if (delValues != null)
              {
                for (int i = 0; i < delValues.size(); i++)
                  {
                    xInvid xi = (xInvid) delValues.elementAt(i);
                    xi.getInvid(false); // try to resolve
                  }
              }

            if (addValues != null)
              {
                for (int i = 0; i < addValues.size(); i++)
                  {
                    xInvid xi = (xInvid) addValues.elementAt(i);
                    xi.getInvid(false); // try to resolve
                  }
              }

            if (addIfNotPresentValues != null)
              {
                for (int i = 0; i < addIfNotPresentValues.size(); i++)
                  {
                    xInvid xi = (xInvid) addIfNotPresentValues.elementAt(i);
                    xi.getInvid(false); // try to resolve
                  }
              }
          }
      }
    catch (ClassCastException ex)
      {
        Ganymede.logError(ex, "Error processing xmlfield dereferenceInvids()." +
                          "xmlfield is " + this.toString());
      }
  }

  /**
   * <p>This private helper method takes a Vector of xInvid and
   * xmlobject objects (in the embedded object case) and returns
   * a Vector of Invid objects.  If any xmlobjects in the input
   * Vector did not map to pre-existing objects on the server,
   * then no invid will be returned for those elements, and as
   * a result, the returned vector may be smaller than the
   * input.</p>
   */

  private Vector getExtantInvids(Vector values) throws NotLoggedInException
  {
    Invid invid;
    Vector invids = new Vector();

    /* -- */

    if (values == null)
      {
        return invids;
      }

    // if we're an embedded object field, we'll contain xmlobjects

    for (int i=0; i < values.size(); i++)
      {
        Object x = values.elementAt(i);

        if (x instanceof xInvid)
          {
            invid = ((xInvid) x).getInvid();

            if (debug && invid == null)
              {
                owner.xSession.err.println("Couldn't find an invid from an xInvid.getInvid() call on " + x);
              }
          }
        else if (x instanceof xmlobject)
          {
            invid = ((xmlobject) x).getInvid();

            if (debug && invid == null)
              {
                owner.xSession.err.println("Couldn't find an invid from an xmlobject.getInvid() call on " + x);
              }
          }
        else
          {
            owner.xSession.err.println("Unrecognized XML element while processing Invid vector: " + x);
            continue;
          }

        if (invid != null)
          {
            invids.addElement(invid);
          }
        else
          {
            owner.xSession.err.println("Couldn't find invid for " + x);
          }
      }

    return invids;
  }

  /**
   * <p>This private helper method takes a Vector of xInvid and
   * xmlobject objects (in the embedded object case) and returns
   * a Vector of xmlobjects that exist on the server.  Any xInvid
   * objects in the input Vector, along with any xmlobject objects
   * which do not correspond to pre-existing objects on the server
   * will be omitted from the returned vector.</p>
   */

  private Vector getExtantObjects(Vector values) throws NotLoggedInException
  {
    Vector objects = new Vector();

    /* -- */

    if (values == null)
      {
        return objects;
      }

    // if we're an embedded object field, we'll contain xmlobjects

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
                owner.xSession.err.println("Couldn't find invid for " + x);
              }
          }
        else
          {
            owner.xSession.err.println("Unrecognized XML element while processing Invid vector: " + x);
            continue;
          }
      }

    return objects;
  }

  /**
   * <p>This private helper method takes a Vector of xInvid and
   * xmlobject objects and returns a Vector of xInvids and xmlobjects
   * that could not be resolved on the server.</p>
   */

  private Vector getNonRegisteredObjects(Vector values) throws NotLoggedInException
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
            owner.xSession.err.println("Unrecognized XML element while processing Invid vector: " + x);
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
   * <p>Returns the {@link arlut.csd.ganymede.common.FieldType} short for
   * the type of field represented by this xmlfield.</p>
   */

  public short getType()
  {
    return fieldDef.getType();
  }

  /**
   * <p>Returns true if this field is a vector field.</p>
   */

  public boolean isArray()
  {
    return fieldDef.isArray();
  }

  /**
   * <p>Debug diagnostics</p>
   */

  public String toString()
  {
    StringBuilder result = new StringBuilder();

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
 * This class is used by the Ganymede XML client to represent an invid
 * object reference field value.  This field value may or may not be
 * fully resolved to an actual invid on the server, depending on what
 * stage of processing the XML client is in.
 */

class xInvid {

  /**
   * TranslationService object for handling string localization in the
   * Ganymede system.
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.server.xInvid");

  /**
   * <p>The numeric type id for the object type this xInvid is meant
   * to point to.</p>
   *
   * <p>In the XML file, this field is derived from the type
   * attribute, after doing an object type lookup in the server's data
   * structures.</p>
   */

  short typeId;

  /**
   * <p>The id string for this xInvid from the XML file.  Will be used
   * to resolve this xInvid to an actual {@link
   * arlut.csd.ganymede.common.Invid Invid} on the server, if set.</p>
   *
   * <p>In the XML file, this field is taken from the id
   * attribute.</p>
   */

  String objectId;

  /**
   * <p>This polymorphic object field is intended to contain the
   * actual on-server Invid corresponding to this xInvid object.</p>
   *
   * <p>invidPtr will contain an actual {@link
   * arlut.csd.ganymede.common.Invid} object if this <invid> element
   * could be matched against a pre-existing Invid on the Ganymede
   * server.</p>
   *
   * <p>If no server-side Invid can be found, invidPtr may instead
   * point to the {@link arlut.csd.ganymede.server.xmlobject} object
   * which will eventually be given the Invid we're interested in when
   * it is integrated into the server's data store.</p>
   */

  private Object invidPtr;

  /**
   * The numeric object id, if specified in the XML file for this
   * xInvid.
   */

  int num = -1;

  /* -- */

  public xInvid(XMLItem item)
  {
    if (!item.matches("invid"))
      {
        getXSession().err.println("Unrecognized XML item found when invid element expected: " + item);

        throw new NullPointerException("Bad item!");
      }

    if (!item.isEmpty())
      {
        getXSession().err.println("Error, found a non-empty invid element: " + item);

        throw new NullPointerException("Bad item!");
      }

    String typeString = item.getAttrStr("type");

    if (typeString == null)
      {
        getXSession().err.println("Missing or malformed invid type in element: " + item);

        throw new NullPointerException("Bad item!");
      }
    else
      {
        typeString = typeString.intern();
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
            getXSession().err.println("Unknown object target in invid field element: " + item);
            throw new NullPointerException("Bad item!");
          }
      }
    else
      {
        // XXX note that this is very expensive in terms of the
        // Permanent Generation memory zone when using Sun's HotSpot
        // VM in 1.4 or 1.5.  We already did this in the Ganymede
        // DBStore itself to reduce long term heap usage, so this
        // isn't anything special here, but if you run the Ganymede
        // server on a VM that can't handle large blocks of interned
        // Strings, you might want to turn this one off in particular,
        // along with the StringDBField interning. XXX

        objectId = objectId.intern();
      }

    try
      {
        typeId = getXSession().getTypeNum(typeString);
      }
    catch (NullPointerException ex)
      {
        getXSession().err.println("Unknown target type " + typeString +
                           " in invid field element: " + item);
        throw new NullPointerException("Bad item!");
      }
  }

  /**
   * This method resolves and returns the Invid for this xInvid place
   * holder, talking to the server if necessary to resolve an id
   * string.
   */

  public Invid getInvid() throws NotLoggedInException
  {
    return getInvid(true);
  }

  /**
   * <p>This method resolves and returns the Invid for this xInvid
   * place holder, talking to the server if necessary to resolve an id
   * string.</p>
   *
   * @param noReally If false, we're being called by the xmlfield
   * dereferenceInvids method, and we don't need to consider it a
   * failure if we can't get either an Invid or xmlobject reference.
   * If true, we're being called at a time when we really do need to
   * have some kind of proper lookup, so we'll throw an exception
   * if we can't find something to match this xInvid.
   */

  public Invid getInvid(boolean noReally) throws NotLoggedInException
  {
    if (invidPtr != null)
      {
        if (invidPtr instanceof Invid)
          {
            return (Invid) invidPtr;
          }
        else if (invidPtr instanceof xmlobject)
          {
            Invid deref = ((xmlobject) invidPtr).getInvid();

            if (deref != null)
              {
                invidPtr = deref;
                return (Invid) invidPtr;
              }
            else
              {
                return null;
              }
          }
      }

    if (objectId != null)
      {
        invidPtr = getXSession().getInvid(typeId, objectId);

        if (invidPtr == null)
          {
            // we couldn't get a direct Invid reference, so set this
            // invid reference to point to the xmlobject that matches
            // it

            invidPtr = getXSession().getXMLObjectTarget(typeId, objectId);

            // if noReally is true, we aren't merely doing a
            // speculative dereference.. complain and shout.

            if (invidPtr == null && noReally)
              {
                // "xInvid.getInvid(): Couldn''t find any {0} objects labeled {1}."
                throw new RuntimeException(ts.l("getInvid.bad_label",
                                                getXSession().getTypeName(typeId),
                                                objectId));
              }

            // even if we found an xmlobject, we still don't know the
            // Invid, so we'll return null for now

            return null;
          }
        else
          {
            return (Invid) invidPtr;
          }
      }
    else if (num != -1)
      {
        invidPtr = Invid.createInvid(typeId, num);

        return (Invid) invidPtr;
      }

    return null;
  }

  public String toString()
  {
    StringBuilder result = new StringBuilder();

    /* -- */

    result.append("<invid type=\"");
    result.append(getXSession().getTypeName(typeId));
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

  /**
   * This is a very hack-ish way of getting a GanymedeXMLSession
   * reference without having to store it redundantly in all of the
   * (possibly hundreds of thousands) xInvid objects stored in the
   * server during xml transaction processing.
   */

  private GanymedeXMLSession getXSession()
  {
    if (java.lang.Thread.currentThread() instanceof GanymedeXMLSession)
      {
        return (GanymedeXMLSession) java.lang.Thread.currentThread();
      }

    return null;
  }
}

/**
 * <p>This class is used by the Ganymede XML client to represent
 * a password field data value.</p>
 *
 * <p>This class has five separate value fields, for the
 * possible password formats supported by Ganymede, but in fact
 * only one of them at a time should be anything other than
 * null, as setting any of these attributes on a Ganymede
 * password field clears the others, with the exception of the
 * paired NT/Samba hash formats.</p>
 */

class xPassword {

  String plaintext;
  String crypttext;
  String md5text;
  String apachemd5text;
  String lanman;
  String ntmd4;
  String sshatext;
  String shaunixcrypt;
  String bcrypt;

  /* -- */

  public xPassword(XMLItem item) throws SAXException
  {
    if (!item.matches("password"))
      {
        getXSession().err.println("Unrecognized XML item found when password element expected: " + item);
        throw new NullPointerException("Bad item!");
      }

    if (!item.isEmpty())
      {
        getXSession().err.println("Error, found a non-empty password element: " + item);

        throw new NullPointerException("Bad item!");
      }

    plaintext = item.getAttrStr("plaintext");
    crypttext = item.getAttrStr("crypt");
    md5text = item.getAttrStr("md5crypt");
    apachemd5text = item.getAttrStr("apachemd5crypt");
    lanman = item.getAttrStr("lanman");
    ntmd4 = item.getAttrStr("ntmd4");
    sshatext = item.getAttrStr("ssha");
    shaunixcrypt = item.getAttrStr("shaUnixCrypt");
    bcrypt = item.getAttrStr("bCrypt");
  }

  public String toString()
  {
    StringBuilder result = new StringBuilder();

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

    if (apachemd5text != null)
      {
        result.append(" apachemd5crypt=\"");
        result.append(apachemd5text);
        result.append("\"");
      }

    if (lanman != null)
      {
        result.append(" lanman=\"");
        result.append(lanman);
        result.append("\"");
      }

    if (ntmd4 != null)
      {
        result.append(" ntmd4=\"");
        result.append(ntmd4);
        result.append("\"");
      }

    if (sshatext != null)
      {
        result.append(" ssha=\"");
        result.append(sshatext);
        result.append("\"");
      }

    if (shaunixcrypt != null)
      {
        result.append(" shaUnixCrypt=\"");
        result.append(shaunixcrypt);
        result.append("\"");
      }

    if (bcrypt != null)
      {
        result.append(" bCrypt=\"");
        result.append(bcrypt);
        result.append("\"");
      }

    result.append("/>");

    return result.toString();
  }

  private GanymedeXMLSession getXSession()
  {
    if (java.lang.Thread.currentThread() instanceof GanymedeXMLSession)
      {
        return (GanymedeXMLSession) java.lang.Thread.currentThread();
      }

    return null;
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

  Hashtable<String, xPerm> fields = null;

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
        getXSession().err.println("Unrecognized element encountered in xPerm constructor, skipping: " + item);
        item = getXSession().getNextItem();
      }

    label = ((XMLElement) item).getName();

    if (label != null)
      {
        label = label.intern();
      }

    String permbits = item.getAttrStr("perm");

    if (permbits == null)
      {
        getXSession().err.println("No perm attributes found for xPerm item " + item);
      }
    else
      {
        view = (permbits.indexOf('v') != -1) || (permbits.indexOf('V') != -1);
        edit = (permbits.indexOf('e') != -1) || (permbits.indexOf('E') != -1);
        create = (permbits.indexOf('c') != -1) || (permbits.indexOf('C') != -1);
        delete = (permbits.indexOf('d') != -1) || (permbits.indexOf('D') != -1);
      }

    if (objectType && !item.isEmpty())
      {
        fields = new Hashtable<String, xPerm>();
        item = getXSession().getNextItem();

        while (!item.matchesClose(label) && !(item instanceof XMLEndDocument))
          {
            xPerm fieldperm = new xPerm(item, false);
            fields.put(fieldperm.getName(), fieldperm);

            item = getXSession().getNextItem();
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
    return PermEntry.getPermEntry(view, edit, create, delete);
  }

  private GanymedeXMLSession getXSession()
  {
    if (java.lang.Thread.currentThread() instanceof GanymedeXMLSession)
      {
        return (GanymedeXMLSession) java.lang.Thread.currentThread();
      }

    return null;
  }
}

/**
 * <p>This class is used by the Ganymede XML client to represent
 * a permission field value.</p>
 *
 * <p>xOption are slightly recursive, in that a top-level xOption
 * is used to represent the permissions for a type of object
 * at the object level, and contain in turn xOption objects used
 * for the individual fields defined within that object.</p>
 */

class xOption {

  /**
   * <p>String describing this xOption's contents.  This String
   * is held in XMLEncoded form.  That is, spaces in the Ganymede
   * object type and/or field name have been replaced with
   * underscores.</p>
   */

  String label = null;

  /**
   * <p>If this xOption is representing an object type as a whole,
   * fields will map field names to xOption objects.  If this
   * xOption is representing permissions for a field, this variable
   * will be null.</p>
   */

  Hashtable<String, xOption> fields = null;

  /**
   * <p>The option for this xOption.</p>
   */

  SyncPrefEnum option;

  /* -- */

  /**
   * <p>xOption constructor.  When the constructor is called, the
   * xOption reads the next item from the xmlclient's
   * {@link arlut.csd.ganymede.client.xmlclient#getNextItem() getNextItem()}
   * method and uses it to initialize the xOption.</p>
   *
   * <p>If objectType is true, the xOption constructor assumes that
   * it is reading an entire object type's permission data.  In this
   * case, it will load the permission data for the object, including
   * all of the field permissions data contained within the top-level
   * object.</p>
   */

  public xOption(XMLItem item, boolean objectType) throws SAXException
  {
    while (!(item instanceof XMLElement))
      {
        getXSession().err.println("Unrecognized element encountered in xOption constructor, skipping: " + item);
        item = getXSession().getNextItem();
      }

    label = ((XMLElement) item).getName();

    if (label != null)
      {
        label = label.intern();
      }

    SyncPrefEnum myOption = SyncPrefEnum.find(item.getAttrStr("option"));

    if (myOption == null)
      {
        getXSession().err.println("No perm attributes found for xOption item " + item);
      }
    else
      {
        this.option = myOption;
      }

    if (objectType && !item.isEmpty())
      {
        fields = new Hashtable<String, xOption>();
        item = getXSession().getNextItem();

        while (!item.matchesClose(label) && !(item instanceof XMLEndDocument))
          {
            xOption fieldoption = new xOption(item, false);
            fields.put(fieldoption.getName(), fieldoption);

            item = getXSession().getNextItem();
          }

        if (item instanceof XMLEndDocument)
          {
            throw new RuntimeException("Ran into end of XML file while parsing options object " + label);
          }
      }
  }

  public String getName()
  {
    return label;
  }

  public SyncPrefEnum getOption()
  {
    return this.option;
  }

  private GanymedeXMLSession getXSession()
  {
    if (java.lang.Thread.currentThread() instanceof GanymedeXMLSession)
      {
        return (GanymedeXMLSession) java.lang.Thread.currentThread();
      }

    return null;
  }
}

