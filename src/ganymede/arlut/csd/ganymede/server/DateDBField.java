/*
   GASH 2

   DateDBField.java

   The GANYMEDE object storage system.

   Created: 2 July 1996

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2013
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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

import arlut.csd.ganymede.common.ReturnVal;
import arlut.csd.ganymede.rmi.date_field;

import arlut.csd.Util.TranslationService;

/*------------------------------------------------------------------------------
                                                                           class
                                                                     DateDBField

------------------------------------------------------------------------------*/

/**
 * <p>DateDBField is a subclass of {@link
 * arlut.csd.ganymede.server.DBField DBField} for the storage and
 * handling of Date fields in the {@link
 * arlut.csd.ganymede.server.DBStore DBStore} on the Ganymede
 * server.</p>
 *
 * <p>The Ganymede client talks to DateDBFields through the {@link
 * arlut.csd.ganymede.rmi.date_field date_field} RMI interface.</p>
 *
 * <p>Ganymede uses the standard Java Date class, which can encode
 * dates from roughly 300 million years B.C. to 300 million years
 * A.D., with millisecond resolution.  No Y2k problems here. ;-)</p>
 */

public class DateDBField extends DBField implements date_field {

  static DateFormat formatter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss",
                                                     java.util.Locale.US);

  /**
   * TranslationService object for handling string localization in
   * the Ganymede server.
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.server.DateDBField");

  // ---

  /**
   * <p>Receive constructor.  Used to create a DateDBField from a
   * {@link arlut.csd.ganymede.server.DBStore DBStore}/{@link
   * arlut.csd.ganymede.server.DBJournal DBJournal} DataInput
   * stream.</p>
   */

  DateDBField(DBObject owner, DataInput in, DBObjectBaseField definition) throws IOException
  {
    super(owner, definition.getID());

    this.value = null;
    receive(in, definition);
  }

  /**
   * <p>No-value constructor.  Allows the construction of a
   * 'non-initialized' field, for use where the {@link
   * arlut.csd.ganymede.server.DBObjectBase DBObjectBase} definition
   * indicates that a given field may be present, but for which no
   * value has been stored in the {@link
   * arlut.csd.ganymede.server.DBStore DBStore}.</p>
   *
   * <p>Used to provide the client a template for 'creating' this
   * field if so desired.</p>
   */

  DateDBField(DBObject owner, DBObjectBaseField definition)
  {
    super(owner, definition.getID());

    this.value = null;
  }

  /**
   *
   * Copy constructor.
   *
   */

  public DateDBField(DBObject owner, DateDBField field)
  {
    super(owner, field.getID());

    if (field.value instanceof Date)
      {
        this.value = new Date(((Date) field.value).getTime());
      }
    else
      {
        this.value = field.value;
      }
  }

  /**
   *
   * Scalar value constructor.
   *
   */

  public DateDBField(DBObject owner, Date value, DBObjectBaseField definition)
  {
    super(owner, definition.getID());

    this.value = value;
  }

  /**
   *
   * Vector value constructor.
   *
   */

  public DateDBField(DBObject owner, Vector values, DBObjectBaseField definition)
  {
    super(owner, definition.getID());

    throw new IllegalArgumentException("vector constructor called on scalar field");
  }

  public Object clone() throws CloneNotSupportedException
  {
    throw new CloneNotSupportedException();
  }

  void emit(DataOutput out) throws IOException
  {
    out.writeLong(((Date) value).getTime());
  }

  void receive(DataInput in, DBObjectBaseField definition) throws IOException
  {
    value = new Date(in.readLong());
  }

  /**
   * <p>This method is used when the database is being dumped, to write
   * out this field to disk.</p>
   */

  synchronized void emitXML(XMLDumpContext xmlOut) throws IOException
  {
    xmlOut.startElementIndent(this.getXMLName());
    emitDateXML(xmlOut, value());
    xmlOut.endElement(this.getXMLName());
  }

  public void emitDateXML(XMLDumpContext xmlOut, Date value) throws IOException
  {
    xmlOut.startElement("date");

    synchronized (formatter)
      {
        xmlOut.attribute("val", formatter.format(value));
      }

    xmlOut.attribute("timecode", java.lang.Long.toString(value.getTime()));
    xmlOut.endElement("date");
  }

  // ****
  //
  // type-specific accessor methods
  //
  // ****

  public Date value()
  {
    return (Date) value;
  }

  public Date value(int index)
  {
    throw new IllegalArgumentException("vector accessor called on scalar");
  }

  /**
   * This method returns a text encoded value for this DBField
   * without checking permissions.
   */

  public synchronized String getValueString()
  {
    /* -- */

    if (value == null)
      {
        // "null"
        return ts.l("getValueString.null");
      }

    // pass the date through the localized default formatter rather
    // than using the toString() method.

    // "{0,date,EEE, MMM d yyyy hh:mm:ss aaa zz}"
    return ts.l("getValueString.date", this.value);
  }

  /**
   * Date fields need a special encoding that can be easily
   * reversed to obtain a date object on the client for sorting
   * and selection purposes.
   */

  public synchronized String getEncodingString()
  {
    if (value == null)
      {
        return "null";
      }

    return Long.toString(((Date) this.value).getTime());
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

  public String getDiffString(DBField orig)
  {
    DateDBField origD;
    StringBuilder result = new StringBuilder();

    /* -- */

    if (!(orig instanceof DateDBField))
      {
        throw new IllegalArgumentException("bad field comparison");
      }

    origD = (DateDBField) orig;

    if (!origD.value().equals(this.value()))
      {
        // "\tOld: {0,date,EEE, MMM d yyyy hh:mm:ss aaa zz}\n"
        result.append(ts.l("getDiffString.old", origD.value));

        // "\tNew: {0,date,EEE, MMM d yyyy hh:mm:ss aaa zz}\n"
        result.append(ts.l("getDiffString.new", this.value));

        return result.toString();
      }
    else
      {
        return null;
      }
  }

  // ****
  //
  // date_field methods
  //
  // ****

  /**
   * <p>Returns true if this date field has a minimum and/or maximum date
   * set.</p>
   *
   * <p>We depend on our owner's
   * {@link arlut.csd.ganymede.server.DBEditObject#isDateLimited(arlut.csd.ganymede.server.DBField) isDateLimited()}
   * method to tell us whether this Date field should be limited or not
   * in this editing context.</p>
   *
   * @see arlut.csd.ganymede.rmi.date_field
   */

  public boolean limited()
  {
    DBEditObject eObj;

    /* -- */

    if (!isEditable(true))
      {
        throw new IllegalArgumentException("not applicable to a non-editable field/object");
      }

    eObj = (DBEditObject) this.owner;

    return eObj.isDateLimited(this);
  }

  /**
   * <p>Returns the earliest date acceptable for this field</p>
   *
   * <p>We depend on our owner's {@link
   * arlut.csd.ganymede.server.DBEditObject#minDate(arlut.csd.ganymede.server.DBField)
   * minDate()} method to tell us what the earliest acceptable Date
   * for this field is.</p>
   *
   * @see arlut.csd.ganymede.rmi.date_field
   */

  public Date minDate()
  {
    DBEditObject eObj;

    /* -- */

    if (!isEditable(true))
      {
        throw new IllegalArgumentException("not applicable to a non-editable field/object");
      }

    eObj = (DBEditObject) this.owner;

    return eObj.minDate(this);
  }

  /**
   * <p>Returns the latest date acceptable for this field</p>
   *
   * <p>We depend on our owner's {@link
   * arlut.csd.ganymede.server.DBEditObject#maxDate(arlut.csd.ganymede.server.DBField)
   * maxDate()} method to tell us what the earliest acceptable Date
   * for this field is.</p>
   *
   * @see arlut.csd.ganymede.rmi.date_field
   */

  public Date maxDate()
  {
    DBEditObject eObj;

    /* -- */

    if (!isEditable(true))
      {
        throw new IllegalArgumentException("not applicable to a non-editable field/object");
      }

    eObj = (DBEditObject) this.owner;

    return eObj.maxDate(this);
  }

  // ****
  //
  // Overridable methods for implementing intelligent behavior
  //
  // ****

  public boolean verifyTypeMatch(Object o)
  {
    return ((o == null) || (o instanceof Date));
  }

  public ReturnVal verifyNewValue(Object o)
  {
    DBEditObject eObj;
    Date d, d2;

    /* -- */

    if (!isEditable(true))
      {
        // "Date Field Error"
        // "Don''t have permission to edit field {0} in object {1}."
        return Ganymede.createErrorDialog(ts.l("verifyNewValue.error_title"),
                                          ts.l("verifyNewValue.bad_perm", getName(), this.owner.getLabel()));
      }

    eObj = (DBEditObject) this.owner;

    if (!verifyTypeMatch(o))
      {
        // "Date Field Error"
        // "Type error.  Submitted value {0} is not a Date!  Major client error while trying to edit field {1} in object {2}."
        return Ganymede.createErrorDialog(ts.l("verifyNewValue.error_title"),
                                          ts.l("verifyNewValue.bad_type", o, getName(), this.owner.getLabel()));
      }

    if (o == null)
      {
        return eObj.verifyNewValue(this, null);  // FindBugs explicit
      }

    d = (Date) o;

    if (limited())
      {
        d2 = minDate();

        if (d2 != null)
          {
            if (d.before(d2))
              {
                // "Date Field Error"
                // "Submitted Date {0,date,EEE, MMM d yyyy hh:mm:ss aaa zz} is out of range for field {1} in object {2}.\n
                // This field will not accept dates before {3,date,EEE, MMM d yyyy hh:mm:ss aaa zz}."
                return Ganymede.createErrorDialog(ts.l("verifyNewValue.error_title"),
                                                  ts.l("verifyNewValue.under_range", d, getName(), this.owner.getLabel(), d2));
              }
          }

        d2 = maxDate();
        if (d2 != null)
          {
            if (d.after(d2))
              {
                // "Date Field Error"
                // "Submitted Date {0,date,EEE, MMM d yyyy hh:mm:ss aaa zz} is out of range for field {1} in object {2}.\n
                // This field will not accept dates after {3,date,EEE, MMM d yyyy hh:mm:ss aaa zz}."
                return Ganymede.createErrorDialog(ts.l("verifyNewValue.error_title"),
                                                  ts.l("verifyNewValue.over_range", d, getName(), this.owner.getLabel(), d2));
              }
          }
      }

    // have our parent make the final ok on the value

    return eObj.verifyNewValue(this, o);
  }
}
