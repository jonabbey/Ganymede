/*
   GASH 2

   NumericDBField.java

   The GANYMEDE object storage system.

   Created: 2 July 1996
   Release: $Name:  $
   Version: $Revision: 1.24 $
   Last Mod Date: $Date: 2000/03/24 21:27:26 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

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

package arlut.csd.ganymede;

import java.io.*;
import java.util.*;
import java.rmi.*;

import com.jclark.xml.output.*;
import arlut.csd.Util.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                  NumericDBField

------------------------------------------------------------------------------*/

/**
 * <P>NumericDBField is a subclass of {@link arlut.csd.ganymede.DBField DBField}
 * for the storage and handling of numeric
 * fields in the {@link arlut.csd.ganymede.DBStore DBStore} on the Ganymede
 * server.</P>
 *
 * <P>The Ganymede client talks to NumericDBFields through the
 * {@link arlut.csd.ganymede.num_field num_field} RMI interface.</P> 
 */

public class NumericDBField extends DBField implements num_field {

  /**
   * <P>Receive constructor.  Used to create a NumericDBField from a
   * {@link arlut.csd.ganymede.DBStore DBStore}/{@link arlut.csd.ganymede.DBJournal DBJournal}
   * DataInput stream.</P>
   */

  NumericDBField(DBObject owner, DataInput in, DBObjectBaseField definition) throws IOException
  {
    value = null;
    this.owner = owner;
    this.definition = definition;
    receive(in);
  }

  /**
   * <P>No-value constructor.  Allows the construction of a
   * 'non-initialized' field, for use where the 
   * {@link arlut.csd.ganymede.DBObjectBase DBObjectBase}
   * definition indicates that a given field may be present,
   * but for which no value has been stored in the 
   * {@link arlut.csd.ganymede.DBStore DBStore}.</P>
   *
   * <P>Used to provide the client a template for 'creating' this
   * field if so desired.</P>
   */

  NumericDBField(DBObject owner, DBObjectBaseField definition)
  {
    this.owner = owner;
    this.definition = definition;
    
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
    definition = field.definition;
    
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
    this.definition = definition;
    this.value = new Integer(value);
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

  public Object clone()
  {
    return new NumericDBField(owner, this);
  }

  void emit(DataOutput out) throws IOException
  {
    out.writeInt(((Integer) value).intValue());
  }

  void receive(DataInput in) throws IOException
  {
    value = new Integer(in.readInt());
  }

  /**
   * <p>This method is used when the database is being dumped, to write
   * out this field to disk.  It is mated with receiveXML().</p>
   */

  synchronized void emitXML(XMLWriter xmlOut, int indentLevel) throws IOException
  {
    /* -- */

    XMLUtils.indent(xmlOut, indentLevel);

    xmlOut.startElement(this.getXMLName());
    emitIntXML(xmlOut, value());
    xmlOut.endElement(this.getXMLName());
  }

  public void emitIntXML(XMLWriter xmlOut, int value) throws IOException
  {
    xmlOut.startElement("int");
    xmlOut.attribute("val", java.lang.Integer.toString(value));
    xmlOut.endElement("int");
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
    if (!verifyReadPermission())
      {
	throw new IllegalArgumentException("permission denied to read this field");
      }

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
    StringBuffer result = new StringBuffer();

    /* -- */

    if (!(orig instanceof NumericDBField))
      {
	throw new IllegalArgumentException("bad field comparison");
      }

    origN = (NumericDBField) orig;

    if (origN.value() != this.value())
      {
	result.append("\tOld: ");
	result.append(Integer.toString(origN.value()));
	result.append("\n\tNew: ");
	result.append(Integer.toString(this.value()));
	result.append("\n");
	
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
   * @see arlut.csd.ganymede.num_field
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
   * @see arlut.csd.ganymede.num_field
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
   * @see arlut.csd.ganymede.num_field
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

  public ReturnVal verifyNewValue(Object o)
  {
    DBEditObject eObj;
    Integer I;

    /* -- */

    eObj = (DBEditObject) owner;

    if (!verifyTypeMatch(o))
      {
	return Ganymede.createErrorDialog("Numeric Field Error",
					  "Submitted value " + o + " is not an integer!  Major client error while" +
					  " trying to edit field " + getName() +
					  " in object " + owner.getLabel());
      }

    if (o == null)
      {
	return eObj.verifyNewValue(this, o);
      }

    I = (Integer) o;

    if (limited())
      {
	if (getMinValue() > I.intValue())
	  {
	    return Ganymede.createErrorDialog("Numeric Field Error",
					      "Submitted integer  " + I + " is out of range for field " +
					      getName() + " in object " + owner.getLabel() + 
					      ".  This field will not accept integers less than " + getMinValue());
	  }

	if (getMaxValue() < I.intValue())
	  {
	    return Ganymede.createErrorDialog("Numeric Field Error",
					      "Submitted integer  " + I + " is out of range for field " +
					      getName() + " in object " + owner.getLabel() + 
					      ".  This field will not accept integers greater than " + getMaxValue());
	  }
      }

    // have our parent make the final ok on the value

    return eObj.verifyNewValue(this, o);
  }

}
