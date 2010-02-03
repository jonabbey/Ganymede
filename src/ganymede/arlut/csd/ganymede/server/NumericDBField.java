/*
   GASH 2

   NumericDBField.java

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
import java.util.Vector;

import arlut.csd.ganymede.common.ReturnVal;
import arlut.csd.ganymede.rmi.num_field;

import arlut.csd.Util.TranslationService;

/*------------------------------------------------------------------------------
                                                                           class
                                                                  NumericDBField

------------------------------------------------------------------------------*/

/**
 * <P>NumericDBField is a subclass of {@link arlut.csd.ganymede.server.DBField DBField}
 * for the storage and handling of numeric
 * fields in the {@link arlut.csd.ganymede.server.DBStore DBStore} on the Ganymede
 * server.</P>
 *
 * <P>The Ganymede client talks to NumericDBFields through the
 * {@link arlut.csd.ganymede.rmi.num_field num_field} RMI interface.</P> 
 */

public class NumericDBField extends DBField implements num_field {

  /**
   * TranslationService object for handling string localization in the
   * Ganymede server.
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.server.NumericDBField");

  /**
   * <P>Receive constructor.  Used to create a NumericDBField from a
   * {@link arlut.csd.ganymede.server.DBStore DBStore}/{@link arlut.csd.ganymede.server.DBJournal DBJournal}
   * DataInput stream.</P>
   */

  NumericDBField(DBObject owner, DataInput in, DBObjectBaseField definition) throws IOException
  {
    value = null;
    this.owner = owner;
    this.fieldcode = definition.getID();
    receive(in, definition);
  }

  /**
   * <P>No-value constructor.  Allows the construction of a
   * 'non-initialized' field, for use where the 
   * {@link arlut.csd.ganymede.server.DBObjectBase DBObjectBase}
   * definition indicates that a given field may be present,
   * but for which no value has been stored in the 
   * {@link arlut.csd.ganymede.server.DBStore DBStore}.</P>
   *
   * <P>Used to provide the client a template for 'creating' this
   * field if so desired.</P>
   */

  NumericDBField(DBObject owner, DBObjectBaseField definition)
  {
    this.owner = owner;
    this.fieldcode = definition.getID();
    
    value = null;
  }

  /**
   *
   * Copy constructor.
   *
   */

  public NumericDBField(DBObject owner, NumericDBField field)
  {
    this.owner = owner;
    this.fieldcode = field.getID();
    
    value = field.value;
  }

  /**
   *
   * Scalar value constructor.
   *
   */

  public NumericDBField(DBObject owner, int value, DBObjectBaseField definition)
  {
    this.owner = owner;
    this.fieldcode = definition.getID();
    this.value = Integer.valueOf(value);
  }

  /**
   *
   * Vector value constructor.
   *
   */

  public NumericDBField(DBObject owner, Vector values, DBObjectBaseField definition)
  {
    throw new IllegalArgumentException("vector constructor called on scalar field");
  }

  public Object clone() throws CloneNotSupportedException
  {
    throw new CloneNotSupportedException();
  }

  void emit(DataOutput out) throws IOException
  {
    out.writeInt(((Integer) value).intValue());
  }

  void receive(DataInput in, DBObjectBaseField definition) throws IOException
  {
    value = Integer.valueOf(in.readInt());
  }

  /**
   * <p>This method is used when the database is being dumped, to write
   * out this field to disk.</p>
   */

  synchronized void emitXML(XMLDumpContext xmlOut) throws IOException
  {
    xmlOut.startElementIndent(this.getXMLName());
    emitIntXML(xmlOut, value());
    xmlOut.endElement(this.getXMLName());
  }

  public void emitIntXML(XMLDumpContext xmlOut, int value) throws IOException
  {
    xmlOut.write(java.lang.Integer.toString(value));
  }

  // ****
  //
  // type-specific accessor methods
  //
  // ****

  public int value()
  {
    if (isVector())
      {
	throw new IllegalArgumentException("scalar accessor called on vector field");
      }

    return ((Integer) value).intValue();
  }

  public int value(int index)
  {
    // numeric can't be a vector field

    throw new IllegalArgumentException("vector accessor called on scalar field");
  }

  public synchronized String getValueString()
  {
    if (value == null)
      {
	return "null";
      }

    return Integer.toString(this.value());
  }

  /**
   *
   * For numbers, our default getValueString() encoding is adequate.
   *
   */

  public String getEncodingString()
  {
    return getValueString();
  }

  /**
   * <P>Returns a String representing the change in value between this
   * field and orig.  This String is intended for logging and email,
   * not for any sort of programmatic activity.  The format of the
   * generated string is not defined, but is intended to be suitable
   * for inclusion in a log entry and in an email message.</P>
   *
   * <P>If there is no change in the field, null will be returned.</P>
   */

  public String getDiffString(DBField orig)
  {
    NumericDBField origN;
    StringBuilder result = new StringBuilder();

    /* -- */

    if (!(orig instanceof NumericDBField))
      {
	throw new IllegalArgumentException("bad field comparison");
      }

    origN = (NumericDBField) orig;

    if (origN.value() != this.value())
      {
	// "\tOld: {0,number,#}\n"
	result.append(ts.l("getDiffString.old", origN.getValueLocal()));

	// "\tNew: {0,number,#}\n"
	result.append(ts.l("getDiffString.new", this.getValueLocal()));

	return result.toString();
      }
    else
      {
	return null;
      }
  }

  // ****
  //
  // num_field specific methods
  //
  // ****

  /**
   *
   * Returns true if this field has max/min
   * limitations.
   *
   * @see arlut.csd.ganymede.rmi.num_field
   *
   */

  public boolean limited()
  {
    DBEditObject eObj;

    /* -- */

    if (!isEditable(true))
      {
	throw new IllegalArgumentException("not applicable to a non-editable field/object");
      }

    eObj = (DBEditObject) owner;

    return eObj.isIntLimited(this);
  }

  /**
   *
   * Returns the minimum acceptable value for this field if this field
   * has max/min limitations.  
   *
   * @see arlut.csd.ganymede.rmi.num_field
   * 
   */

  public int getMinValue()
  {
    DBEditObject eObj;

    /* -- */

    if (!isEditable(true))
      {
	throw new IllegalArgumentException("not applicable to a non-editable field/object");
      }

    eObj = (DBEditObject) owner;

    return eObj.minInt(this);
  }

  /**
   *
   * Returns the maximum acceptable value for this field if this field
   * has max/min limitations.
   *
   * @see arlut.csd.ganymede.rmi.num_field
   * 
   */

  public int getMaxValue()
  {
    DBEditObject eObj;

    /* -- */

    if (!isEditable(true))
      {
	throw new IllegalArgumentException("not applicable to a non-editable field/object");
      }

    eObj = (DBEditObject) owner;

    return eObj.maxInt(this);
  }

  // ****
  //
  // Overridable methods for implementing intelligent behavior
  //
  // ****

  public boolean verifyTypeMatch(Object o)
  {
    return ((o == null) || (o instanceof Integer));
  }

  /**
   * Overridable method to verify that an object submitted to this
   * field has an appropriate value.
   *
   * This check is more limited than that of verifyNewValue().. all it
   * does is make sure that the object parameter passes the simple
   * value constraints of the field.  verifyNewValue() does that plus
   * a bunch more, including calling to the DBEditObject hook for the
   * containing object type to see whether it happens to feel like
   * accepting the new value or not.
   *
   * verifyBasicConstraints() is used to double check for values that
   * are already in fields, in addition to being used as a likely
   * component of verifyNewValue() to verify new values.
   */

  public ReturnVal verifyBasicConstraints(Object o)
  {
    if (!verifyTypeMatch(o))
      {
	// "Submitted value {0} is not a number!  Major client error while trying to edit field {1} in object {2}."
	return Ganymede.createErrorDialog(ts.l("verifyBasicConstraints.error_title"),
					  ts.l("verifyBasicConstraints.type_error",
					       o, this.getName(), owner.getLabel()));
      }

    return null;
  }

  public ReturnVal verifyNewValue(Object o)
  {
    DBEditObject eObj;
    Integer I;
    ReturnVal retVal;

    /* -- */

    eObj = (DBEditObject) owner;

    if (o == null)
      {
	return eObj.verifyNewValue(this, null);  // explicit for FindBugs
      }

    retVal = verifyBasicConstraints(o);

    if (!ReturnVal.didSucceed(retVal))
      {
	return retVal;
      }

    I = (Integer) o;

    if (limited())
      {
	if (getMinValue() > I.intValue())
	  {
	    return Ganymede.createErrorDialog(ts.l("verifyNewValue.error_title"),
					      ts.l("verifyNewValue.over_range",
						   I, this.getName(), owner.getLabel(),
						   Integer.valueOf(this.getMinValue())));
	  }

	if (getMaxValue() < I.intValue())
	  {
	    return Ganymede.createErrorDialog(ts.l("verifyNewValue.error_title"),
					      ts.l("verifyNewValue.under_range",
						   I, this.getName(), owner.getLabel(),
						   Integer.valueOf(this.getMinValue())));
	  }
      }

    // have our parent make the final ok on the value

    return eObj.verifyNewValue(this, o);
  }

}
