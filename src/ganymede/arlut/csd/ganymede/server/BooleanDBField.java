/*
   GASH 2

   BooleanDBField.java

   The GANYMEDE object storage system.

   Created: 2 July 1996

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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Vector;

import arlut.csd.Util.TranslationService;
import arlut.csd.ganymede.common.ReturnVal;
import arlut.csd.ganymede.rmi.boolean_field;

/*------------------------------------------------------------------------------
                                                                           class
                                                                  BooleanDBField

------------------------------------------------------------------------------*/

/**
 * <p>BooleanDBField is a subclass of {@link
 * arlut.csd.ganymede.server.DBField DBField} for the storage and
 * handling of boolean fields in the {@link
 * arlut.csd.ganymede.server.DBStore DBStore} on the Ganymede
 * server.</p>
 *
 * <p>The Ganymede client talks to BooleanDBFields through the {@link
 * arlut.csd.ganymede.rmi.boolean_field boolean_field} RMI
 * interface.</p>
 */

public class BooleanDBField extends DBField implements boolean_field {

  /**
   * TranslationService object for handling string localization in the
   * Ganymede server.
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.server.BooleanDBField");

  public static final String trueStr = ts.l("global.true"); // "True"
  public static final String falseStr = ts.l("global.false"); // "False"

  /**
   * <p>Receive constructor.  Used to create a BooleanDBField from a
   * {@link arlut.csd.ganymede.server.DBStore DBStore}/{@link
   * arlut.csd.ganymede.server.DBJournal DBJournal} DataInput
   * stream.</p>
   */

  BooleanDBField(DBObject owner, DataInput in, DBObjectBaseField definition) throws IOException
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

  BooleanDBField(DBObject owner, DBObjectBaseField definition)
  {
    super(owner, definition.getID());

    this.value = null;
  }

  /**
   *
   * Copy constructor.
   *
   */

  public BooleanDBField(DBObject owner, BooleanDBField field)
  {
    super(owner, field.getID());

    this.value = field.value;
  }

  /**
   *
   * Scalar value constructor.
   *
   */

  public BooleanDBField(DBObject owner, boolean value, DBObjectBaseField definition)
  {
    super(owner, definition.getID());

    this.value = Boolean.valueOf(value);
  }

  /**
   *
   * Vector value constructor.
   *
   */

  public BooleanDBField(DBObject owner, Vector values, DBObjectBaseField definition)
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
    out.writeBoolean(value());
  }

  void receive(DataInput in, DBObjectBaseField definition) throws IOException
  {
    value = Boolean.valueOf(in.readBoolean());
  }

  /**
   * <p>This method is used when the database is being dumped, to write
   * out this field to disk.</p>
   */

  synchronized void emitXML(XMLDumpContext xmlOut) throws IOException
  {
    xmlOut.indent();

    xmlOut.startElement(this.getXMLName());
    xmlOut.write(value() ? "true" : "false");
    xmlOut.endElement(this.getXMLName());
  }

  /**
   *
   * Returns true if this field has a value associated
   * with it, or false if it is an unfilled 'placeholder'.
   *
   * @see arlut.csd.ganymede.rmi.db_field
   *
   */

  public boolean isDefined()
  {
    return value();
  }

  // ****
  //
  // type-specific accessor methods
  //
  // ****

  public boolean value()
  {
    if (value == null)
      {
        return false;
      }

    return ((Boolean) value).booleanValue();
  }

  public boolean value(int index)
  {
    throw new IllegalArgumentException("vector accessor called on scalar");
  }

  /**
   * <p>This method returns a text encoded value for this BooleanDBField
   * without checking permissions.</p>
   *
   * <p>This method avoids checking permissions because it is used on
   * the server side only and because it is involved in the {@link
   * arlut.csd.ganymede.server.DBObject#getLabel() getLabel()} logic
   * for {@link arlut.csd.ganymede.server.DBObject DBObject}.</p>
   *
   * <p>If this method checked permissions and the getPerm() method
   * failed for some reason and tried to report the failure using
   * object.getLabel(), as it does at present, the server could get
   * into an infinite loop.</p>
   */

  public synchronized String getValueString()
  {
    if (value == null)
      {
        return "null";
      }

    return (this.value() ? trueStr: falseStr);
  }

  /**
   *
   * The normal boolean getValueString() encoding is adequate.
   *
   *
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

  public String getDiffString(DBField orig)
  {
    BooleanDBField origB;

    /* -- */

    if (!(orig instanceof BooleanDBField))
      {
        throw new IllegalArgumentException("bad field comparison");
      }

    origB = (BooleanDBField) orig;

    if (origB.value() != this.value())
      {
        // "\tOld: {0}\n\tNew: {1}"
        return ts.l("getDiffString.comparison",
                    origB.value() ? trueStr: falseStr,
                    this.value() ? trueStr: falseStr);
      }
    else
      {
        return null;
      }
  }

  // ****
  //
  // boolean_field methods
  //
  // ****

  /**
   *
   * Returns true if this field is defined to have the true/false
   * values associated with labels.
   *
   * @see arlut.csd.ganymede.rmi.boolean_field
   */

  public boolean labeled()
  {
    return getFieldDef().isLabeled();
  }

  /**
   *
   * Returns the true label if this field is defined to have the true/false
   * values associated with labels.
   *
   * @see arlut.csd.ganymede.rmi.boolean_field
   */

  public String trueLabel()
  {
    return getFieldDef().getTrueLabel();
  }

  /**
   *
   * Returns the false label if this field is defined to have the true/false
   * values associated with labels.
   *
   * @see arlut.csd.ganymede.rmi.boolean_field
   */

  public String falseLabel()
  {
    return getFieldDef().getFalseLabel();
  }

  // ****
  //
  // Overridable methods for implementing intelligent behavior
  //
  // ****

  public boolean verifyTypeMatch(Object o)
  {
    return ((o == null) || (o instanceof Boolean));
  }

  public ReturnVal verifyNewValue(Object o)
  {
    DBEditObject eObj;

    /* -- */

    if (!isEditable(true))
      {
        // "Boolean Field Error"
        // "Don''t have permission to edit field {0} in object {1}."
        return Ganymede.createErrorDialog(ts.l("verifyNewValue.error_subj"),
                                          ts.l("verifyNewValue.error_perm", getName(), this.getOwner().getLabel()));
      }

    eObj = (DBEditObject) this.getOwner();

    if (!verifyTypeMatch(o))
      {
        // "Boolean Field Error"
        // "Submitted value {0} is not a boolean!  Major client error while trying to edit field {0} in object {1}."
        return Ganymede.createErrorDialog(ts.l("verifyNewValue.error_subj"),
                                          ts.l("verifyNewValue.error_type", getName(), this.getOwner().getLabel()));
      }

    // have our parent make the final ok on the value

    return eObj.verifyNewValue(this, o);
  }
}

