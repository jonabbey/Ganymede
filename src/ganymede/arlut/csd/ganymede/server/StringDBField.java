/*
   GASH 2

   StringDBField.java

   The GANYMEDE object storage system.

   Created: 2 July 1996

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------

   Ganymede Directory Management System
 
   Copyright (C) 1996-2010
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
   along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/

package arlut.csd.ganymede.server;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Vector;

import arlut.csd.ganymede.common.NotLoggedInException;
import arlut.csd.ganymede.common.QueryResult;
import arlut.csd.ganymede.common.ReturnVal;
import arlut.csd.ganymede.rmi.db_field;
import arlut.csd.ganymede.rmi.string_field;

import arlut.csd.Util.TranslationService;
import arlut.csd.Util.VectorUtils;

/*------------------------------------------------------------------------------
                                                                           class
                                                                   StringDBField

------------------------------------------------------------------------------*/

/**
 * <p>StringDBField is a subclass of DBField for the storage and handling of string
 * fields in the {@link arlut.csd.ganymede.server.DBStore DBStore} on the Ganymede
 * server.</p>
 *
 * <p>The Ganymede client talks to StringDBFields through the
 * {@link arlut.csd.ganymede.rmi.string_field string_field} RMI interface.</p> 
 */

public class StringDBField extends DBField implements string_field {

  /**
   * TranslationService object for handling string localization in the
   * Ganymede server.
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.server.StringDBField");

  /**
   * <p>Receive constructor.  Used to create a StringDBField from a
   * {@link arlut.csd.ganymede.server.DBStore DBStore}/{@link arlut.csd.ganymede.server.DBJournal DBJournal}
   * DataInput stream.</p>
   */

  StringDBField(DBObject owner, DataInput in, DBObjectBaseField definition) throws IOException
  {
    value = null;
    this.owner = owner;
    this.fieldcode = definition.getID();
    receive(in, definition);
  }

  /**
   * <p>No-value constructor.  Allows the construction of a
   * 'non-initialized' field, for use where the 
   * {@link arlut.csd.ganymede.server.DBObjectBase DBObjectBase}
   * definition indicates that a given field may be present,
   * but for which no value has been stored in the 
   * {@link arlut.csd.ganymede.server.DBStore DBStore}.</p>
   *
   * <p>Used to provide the client a template for 'creating' this
   * field if so desired.</p>
   */

  StringDBField(DBObject owner, DBObjectBaseField definition)
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
   * Copy constructor.
   */

  public StringDBField(DBObject owner, StringDBField field)
  {
    this.owner = owner;
    this.fieldcode = field.getID();
    
    if (isVector())
      {
        value = field.getVectVal().clone();
      }
    else
      {
        value = field.value;
      }
  }

  /**
   * Scalar value constructor.
   */

  public StringDBField(DBObject owner, String value, DBObjectBaseField definition)
  {
    if (definition.isArray())
      {
        throw new IllegalArgumentException("scalar constructor called on vector field");
      }

    this.owner = owner;
    this.fieldcode = definition.getID();
    this.value = value;
  }

  /**
   * Vector value constructor.
   */

  public StringDBField(DBObject owner, Vector values, DBObjectBaseField definition)
  {
    if (!definition.isArray())
      {
        throw new IllegalArgumentException("vector constructor called on scalar field");
      }

    this.owner = owner;
    this.fieldcode = definition.getID();

    if (values == null)
      {
        value = new Vector();
      }
    else
      {
        value = values.clone();
      }
  }

  public Object clone() throws CloneNotSupportedException
  {
    throw new CloneNotSupportedException();
  }

  void emit(DataOutput out) throws IOException
  {
    if (isVector())
      {
        Vector values = getVectVal();

        int count = 0;

        for (int i = 0; i < values.size(); i++)
          {
            if (!values.elementAt(i).equals(""))
              {
                count++;
              }
          }

        out.writeInt(count);

        for (int i = 0; i < values.size(); i++)
          {
            if (!values.elementAt(i).equals(""))
              {
                out.writeUTF((String) values.elementAt(i));
              }
          }
      }
    else
      {
        out.writeUTF((String)value);
      }
  }

  void receive(DataInput in, DBObjectBaseField definition) throws IOException
  {
    int count;

    /* -- */

    if (definition.isArray())
      {
        if (Ganymede.db.isLessThan(2,3))
          {
            count = in.readShort();
          }
        else
          {
            count = in.readInt();
          }

        value = new Vector(count);

        Vector values = (Vector) value;

        for (int i = 0; i < count; i++)
          {
            values.addElement(in.readUTF().intern());
          }
      }
    else
      {
        value = in.readUTF().intern();
      }
  }

  /**
   * <p>This method is used when the database is being dumped, to write
   * out this field to disk.</p>
   */

  synchronized void emitXML(XMLDumpContext xmlOut) throws IOException
  {
    xmlOut.startElementIndent(this.getXMLName());

    if (!isVector())
      {
        xmlOut.write(value());  // for scalar fields, just write the string in place
      }
    else
      {
        Vector values = getVectVal();

        for (int i = 0; i < values.size(); i++)
          {
            xmlOut.indentOut();
            xmlOut.indent();
            xmlOut.indentIn();
            emitStringXML(xmlOut, (String) values.elementAt(i));
          }

        xmlOut.indent();
      }

    xmlOut.endElement(this.getXMLName());
  }

  public void emitStringXML(XMLDumpContext xmlOut, String value) throws IOException
  {
    xmlOut.startElement("string");
    xmlOut.attribute("val", value);
    xmlOut.endElement("string");
  }

  // ****
  //
  // type specific value accessors
  //
  // ****

  public String value()
  {
    if (isVector())
      {
        throw new IllegalArgumentException("scalar accessor called on vector field");
      }

    return (String) value;
  }

  public String value(int index)
  {
    if (!isVector())
      {
        throw new IllegalArgumentException("vector accessor called on scalar");
      }

    return (String) getVectVal().elementAt(index);
  }

  /**
   * <p>This method returns a text encoded value for this StringDBField
   * without checking permissions.</p>
   *
   * <p>This method avoids checking permissions because it is used on
   * the server side only and because it is involved in the 
   * {@link arlut.csd.ganymede.server.DBObject#getLabel() getLabel()}
   * logic for {@link arlut.csd.ganymede.server.DBObject DBObject}, 
   * which is invoked from {@link arlut.csd.ganymede.server.GanymedeSession GanymedeSession}'s
   * {@link arlut.csd.ganymede.server.GanymedeSession#getPerm(arlut.csd.ganymede.server.DBObject) getPerm()} 
   * method.</p>
   *
   * <p>If this method checked permissions and the getPerm() method
   * failed for some reason and tried to report the failure using
   * object.getLabel(), as it does at present, the server could get
   * into an infinite loop.</p>
   */

  public synchronized String getValueString()
  {
    if (!isVector())
      {
        if (value == null)
          {
            return "null";
          }

        return this.value();
      }

    int size = size();
    
    if (size == 0)
      {
        return "";
      }
    
    String entries[] = new String[size];
    
    for (int i = 0; i < size; i++)
      {
        entries[i] = this.value(i);
      }
    
    java.util.Arrays.sort(entries,
                          new Comparator()
                          {
                            public int compare(Object a, Object b)
                            {
                              String aS, bS;
                              
                              aS = (String) a;
                              bS = (String) b;
                              
                              return aS.compareTo(bS);
                            }
                          }
                          );
    
    StringBuilder result = new StringBuilder();
    
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

  /**
   * <p>For strings, we don't care about having a reversible encoding,
   * because we can sort and select normally based on the getValueString()
   * result.</p>
   */

  public String getEncodingString()
  {
    return getValueString();
  }

  /**
   * <p>Returns a String representing the change in value between this
   * field and orig.  This String is intended for logging and email,
   * not for any sort of programmatic activity.  The format of the
   * generated string is not defined, but is intended to be suitable
   * for inclusion in a log entry and in an email message.</p>
   *
   * <p>If there is no change in the field, null will be returned.</p>
   */

  public synchronized String getDiffString(DBField orig)
  {
    StringBuilder result = new StringBuilder();
    StringDBField origS;

    /* -- */

    if (!(orig instanceof StringDBField))
      {
        throw new IllegalArgumentException("bad field comparison");
      }

    origS = (StringDBField) orig;

    if (isVector())
      {
        Vector 
          added = new Vector(),
          deleted = new Vector();

        Vector values = getVectVal();
        Vector origValues = origS.getVectVal();

        Enumeration en;

        String elementA, elementB;

        boolean found = false;

        /* -- */

        // find elements in the orig field that aren't in our present field

        en = origValues.elements();

        while (en.hasMoreElements())
          {
            elementA = (String) en.nextElement();

            found = false;

            for (int i = 0; !found && i < values.size(); i++)
              {
                elementB = (String) values.elementAt(i);

                if (elementA.equals(elementB))
                  {
                    found = true;
                  }
              }

            if (!found)
              {
                deleted.addElement(elementA);
              }
          }

        // find elements in present our field that aren't in the orig field

        en = values.elements();

        while (en.hasMoreElements())
          {
            elementA = (String) en.nextElement();

            found = false;

            for (int i = 0; !found && i < origValues.size(); i++)
              {
                elementB = (String) origValues.elementAt(i);

                if (elementA.equals(elementB))
                  {
                    found = true;
                  }
              }

            if (!found)
              {
                added.addElement(elementA);
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
                StringBuilder itemList = new StringBuilder();
            
                for (int i = 0; i < deleted.size(); i++)
                  {
                    if (i > 0)
                      {
                        itemList.append(", ");
                      }

                    itemList.append((String) deleted.elementAt(i));
                  }

                // "\tDeleted: {0}\n"
                result.append(ts.l("getDiffString.deleted", itemList.toString()));
              }

            if (added.size() != 0)
              {
                StringBuilder itemList = new StringBuilder();

                for (int i = 0; i < added.size(); i++)
                  {
                    if (i > 0)
                      {
                        itemList.append(", ");
                      }

                    itemList.append((String) added.elementAt(i));
                  }

                // "\tAdded: {0}\n"
                result.append(ts.l("getDiffString.added", itemList.toString()));
              }

            return result.toString();
          }
      }
    else
      {
        if (origS.value().equals(this.value()))
          {
            return null;
          }
        else
          {
            // "\tOld: {0}\n"
            result.append(ts.l("getDiffString.old", origS.value()));

            // "\tNew: {0}\n"
            result.append(ts.l("getDiffString.new", this.value()));
        
            return result.toString();
          }
      }
  }

  /**
   * <p>Returns true if this field has a value associated
   * with it, or false if it is an unfilled 'placeholder'.</p>
   *
   * @see arlut.csd.ganymede.rmi.db_field
   */

  public synchronized boolean isDefined()
  {
    if (isVector())
      {
        if (value != null && getVectVal().size() > 0)
          {
            return true;
          }
        else
          {
            return false;
          }
      }
    else
      {
        if (value != null && !((String) value).equals(""))
          {
            return true;
          }
        else
          {
            return false;
          }
      }
  }

  // ****
  //
  // string_field methods 
  //
  // ****

  /**
   *
   * Returns the maximum acceptable string length
   * for this field.
   *
   * @see arlut.csd.ganymede.rmi.string_field
   *
   */

  public int maxSize()
  {
    return getFieldDef().getMaxLength();
  }

  /**
   *
   * Returns the minimum acceptable string length
   * for this field.
   *
   * @see arlut.csd.ganymede.rmi.string_field
   *
   */

  public int minSize()
  {
    return getFieldDef().getMinLength();
  }

  /**
   *
   * Returns true if the client should echo characters
   * entered into the string field.
   *
   * @see arlut.csd.ganymede.rmi.string_field
   *
   */
  
  public boolean showEcho()
  {
    return true;
  }

  /**
   *
   * Returns true if this field has a list of recommended
   * options for choices from the choices() method.
   *
   * @see arlut.csd.ganymede.rmi.string_field
   *
   */

  public boolean canChoose() throws NotLoggedInException
  {
    if (owner instanceof DBEditObject)
      {
        return (((DBEditObject) owner).obtainChoiceList(this) != null);
      }
    else
      {
        return false;
      }
  }

  /**
   *
   * Returns true if the only valid values
   * for this string field are in the
   * vector returned by choices().
   *
   * @see arlut.csd.ganymede.rmi.string_field
   *
   */

  public boolean mustChoose() throws NotLoggedInException
  {
    if (!canChoose())
      {
        return false;
      }

    if (owner instanceof DBEditObject)
      {
        return ((DBEditObject) owner).mustChoose(this);
      }

    return false;
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
   * <p>Returns a list of recommended and/or mandatory choices 
   * for this field.  This list is dynamically generated by
   * subclasses of {@link arlut.csd.ganymede.server.DBEditObject DBEditObject};
   * this method should not need
   * to be overridden.</p>
   *
   * @see arlut.csd.ganymede.rmi.string_field
   */

  public QueryResult choices() throws NotLoggedInException
  {
    if (!(owner instanceof DBEditObject))
      {
        throw new IllegalArgumentException("can't get choice list on non-editable object");
      }

    return ((DBEditObject) owner).obtainChoiceList(this);
  }

  /**
   * <p>This method returns a key that can be used by the client
   * to cache the value returned by choices().  If the client
   * already has the key cached on the client side, it
   * can provide the choice list from its cache rather than
   * calling choices() on this object again.</p>
   *
   * <p>If there is no caching key, this method will return null.</p>
   */

  public Object choicesKey()
  {
    if (owner instanceof DBEditObject)
      {
        return ((DBEditObject) owner).obtainChoicesKey(this);
      }
    else
      {
        return null;
      }
  }

  /**
   * <p>Returns a string containing the list of acceptable characters.
   * If the string is null, it should be interpreted as meaning all
   * characters not listed in disallowedChars() are allowable by
   * default.</p>
   *
   * @see arlut.csd.ganymede.rmi.string_field
   */

  public String allowedChars()
  {
    return getFieldDef().getOKChars();
  }

  /**
   * <p>Returns a string containing the list of forbidden
   * characters for this field.  If the string is null,
   * it should be interpreted as meaning that no characters
   * are specifically disallowed.</p>
   *
   * @see arlut.csd.ganymede.rmi.string_field
   */

  public String disallowedChars()
  {
    return getFieldDef().getBadChars();
  }

  /**
   * <p>Convenience method to identify if a particular
   * character is acceptable in this field.</p>
   *
   * @see arlut.csd.ganymede.rmi.string_field
   */

  public boolean allowed(char c)
  {
    if (allowedChars() != null && (allowedChars().indexOf(c) == -1))
      {
        return false;
      }

    if (disallowedChars() != null && (disallowedChars().indexOf(c) != -1))
      {
        return false;
      }
    
    return true;
  }

  // ****
  //
  // Overridable methods for implementing intelligent behavior
  //
  // ****


  public boolean verifyTypeMatch(Object o)
  {
    return ((o == null) || (o instanceof String));
  }

  /**
   * <p>Overridable method to verify that an object submitted to this
   * field has an appropriate value.</p>
   *
   * <p>This check is more limited than that of verifyNewValue().. all it
   * does is make sure that the object parameter passes the simple
   * value constraints of the field.  verifyNewValue() does that plus
   * a bunch more, including calling to the DBEditObject hook for the
   * containing object type to see whether it happens to feel like
   * accepting the new value or not.</p>
   *
   * <p>verifyBasicConstraints() is used to double check for values that
   * are already in fields, in addition to being used as a likely
   * component of verifyNewValue() to verify new values.</p>
   */

  public ReturnVal verifyBasicConstraints(Object o)
  {
    if (!verifyTypeMatch(o))
      {
        // "Submitted value {0} is not a String!  Major client error while trying to edit field {1} in object {2}."
        return Ganymede.createErrorDialog(ts.l("verifyBasicConstraints.error_title"),
                                          ts.l("verifyBasicConstraints.type_error",
                                               o, this.getName(), owner.getLabel()));
      }

    String s = (String) o;

    if (s.length() > maxSize())
      {
        // string too long

        // "String value {0} is too long for field {1} in object {2}.  Strings in this field must be less than or equal to {3,number,#} characters long."
        
        return Ganymede.createErrorDialog(ts.l("verifyBasicConstraints.error_title"),
                                          ts.l("verifyBasicConstraints.overlength",
                                               s, this.getName(), owner.getLabel(), Integer.valueOf(this.maxSize())));
      }

    if (s.length() < minSize())
      {
        // string too short

        // "String value {0} is too short for field {1} in object {2}.  Strings in this field must be greater than or equal to {3,number,#} characters long."
        
        return Ganymede.createErrorDialog(ts.l("verifyBasicConstraints.error_title"),
                                          ts.l("verifyBasicConstraints.underlength",
                                               s, this.getName(), owner.getLabel(), Integer.valueOf(this.minSize())));
      }

    if (getFieldDef().getRegexp() != null)
      {
        if (!getFieldDef().getRegexp().matcher(s).find())
          {
            String desc = getFieldDef().getRegexpDesc();

            if (desc == null || desc.equals(""))
              {
                /*
                  String value "{0}" does not conform to the regular expression pattern established for string field {1} in object {2}.\n\n\
                  This string field only accepts strings matching the following regular expression pattern:\n\n\
                  "{3}"
                */

                return Ganymede.createErrorDialog(ts.l("verifyBasicConstraints.error_title"),
                                                  ts.l("verifyBasicConstraints.regexp_nodesc",
                                                       s, this.getName(), owner.getLabel(), getFieldDef().getRegexpPat()));
              }
            else
              {
                /*
                  String value "{0}" does not conform to the regular expression pattern established for string field {1} in object {2}.\n\n\
                  This string field only accepts strings matching the following criteria:\n\n\
                  "{3}"
                */

                return Ganymede.createErrorDialog(ts.l("verifyBasicConstraints.error_title"),
                                                  ts.l("verifyBasicConstraints.regexp_desc",
                                                       s, this.getName(), owner.getLabel(), desc));
              }
          }
      }

    if (allowedChars() != null && !allowedChars().equals(""))
      {
        String okChars = allowedChars();
        
        for (int i = 0; i < s.length(); i++)
          {
            if (okChars.indexOf(s.charAt(i)) == -1)
              {
                // "String value "{0}" contains a character '{1}' which is not allowed in field {2} in object {3}."
                return Ganymede.createErrorDialog(ts.l("verifyBasicConstraints.error_title"),
                                                  ts.l("verifyBasicConstraints.bad_char",
                                                       s, Character.valueOf(s.charAt(i)), this.getName(), owner.getLabel()));
              }
          }
      }
    
    if (disallowedChars() != null && !disallowedChars().equals(""))
      {
        String badChars = disallowedChars();
        
        for (int i = 0; i < s.length(); i++)
          {
            if (badChars.indexOf(s.charAt(i)) != -1)
              {
                // "String value "{0}" contains a character '{1}' which is not allowed in field {2} in object {3}."
                return Ganymede.createErrorDialog(ts.l("verifyBasicConstraints.error_title"),
                                                  ts.l("verifyBasicConstraints.bad_char",
                                                       s, Character.valueOf(s.charAt(i)), this.getName(), owner.getLabel()));
              }
          }
      }

    return null;
  }

  /**
   * <p>Overridable method to verify that an object
   * submitted to this field has an appropriate
   * value.</p>
   *
   * <p>This method is intended to make the final go/no go decision about
   * whether a given value is appropriate to be placed in this field,
   * by whatever means (vector add, vector replacement, scalar
   * replacement).</p>
   *
   * <p>This method is expected to call the {@link
   * arlut.csd.ganymede.server.DBEditObject#verifyNewValue(arlut.csd.ganymede.server.DBField,
   * java.lang.Object)} method on {@link
   * arlut.csd.ganymede.server.DBEditObject} in order to allow custom
   * plugin classes to deny any given value that the plugin might not
   * care for, for whatever reason.  Otherwise, the go/no-go decision
   * will be made based on the checks performed by {@link
   * arlut.csd.ganymede.server.DBField#verifyBasicConstraints(java.lang.Object)}.</p>
   *
   * <p>The ReturnVal that is returned may have transformedValue set, in
   * which case the code that calls this verifyNewValue() method
   * should consider transformedValue as replacing the 'o' parameter
   * as the value that verifyNewValue wants to be put into this field.
   * This usage of transformedValue is for canonicalizing input data.</p>
   */

  public ReturnVal verifyNewValue(Object o)
  {
    DBEditObject eObj;
    String s;
    QueryResult qr;
    ReturnVal retVal = null;

    /* -- */

    if (!isEditable(true))
      {
        // "Don''t have permission to edit field {0} in object {1}."
        return Ganymede.createErrorDialog(ts.l("verifyNewValue.non_editable", getName(), owner.getLabel()));
      }

    eObj = (DBEditObject) owner;

    // for a null value, have the DBEditObject plugin check it and
    // give the yea/nay.

    if (o == null)
      {
        return eObj.verifyNewValue(this, null);  // explicit for FindBugs
      }

    retVal = verifyBasicConstraints(o);

    if (!ReturnVal.didSucceed(retVal))
      {
        return retVal;
      }

    s = (String) o;

    try
      {
        if (mustChoose())
          {
            qr = choices();
            
            if (!qr.containsLabel(s))
              {
                // "String value "{0}" is not a valid choice for field {1} in object {2}."

                return Ganymede.createErrorDialog(ts.l("verifyNewValue.invalid_choice",
                                                       s, getName(), owner.getLabel()));
              }
          }
      }
    catch (NotLoggedInException ex)
      {
        return Ganymede.loginError(ex);
      }

    // have our parent make the final ok on the value

    return eObj.verifyNewValue(this, s);
  }
}
