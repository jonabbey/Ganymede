/*
   GASH 2

   FloatDBField.java

   The GANYMEDE object storage system.

   Created: 29 October 1999
   Release: $Name:  $
   Version: $Revision: 1.11 $
   Last Mod Date: $Date: 2001/01/12 01:12:33 $
   Module By: John Knutson, johnk@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999, 2000, 2001
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
                                                                    FloatDBField

------------------------------------------------------------------------------*/

/**
 * <P>FloatDBField is a subclass of {@link arlut.csd.ganymede.DBField DBField}
 * for the storage and handling of float
 * fields in the {@link arlut.csd.ganymede.DBStore DBStore} on the Ganymede
 * server.</P>
 *
 * <P>The Ganymede client talks to FloatDBFields through the
 * {@link arlut.csd.ganymede.float_field float_field} RMI interface.</P> 
 */

public class FloatDBField extends DBField implements float_field {

  /**
   * <P>Receive constructor.  Used to create a FloatDBField from a
   * {@link arlut.csd.ganymede.DBStore DBStore}/{@link arlut.csd.ganymede.DBJournal DBJournal}
   * DataInput stream.</P>
   */

  FloatDBField(DBObject owner, DataInput in, DBObjectBaseField definition) throws IOException
  {
    value = null;
    this.owner = owner;
    this.fieldcode = definition.getID();
    receive(in, definition);
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

  FloatDBField(DBObject owner, DBObjectBaseField definition)
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

  public FloatDBField(DBObject owner, FloatDBField field)
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

  public FloatDBField(DBObject owner, double value, DBObjectBaseField definition)
  {
    this.owner = owner;
    this.fieldcode = definition.getID();
    this.value = new Double(value);
  }

  /**
   *
   * Vector value constructor.
   *
   */

  public FloatDBField(DBObject owner, Vector values, DBObjectBaseField definition)
  {
    throw new IllegalArgumentException("vector constructor called on scalar field");
  }

  public Object clone()
  {
    return new FloatDBField(owner, this);
  }

  void emit(DataOutput out) throws IOException
  {
    out.writeDouble(((Double) value).doubleValue());
  }

  void receive(DataInput in, DBObjectBaseField definition) throws IOException
  {
    value = new Double(in.readDouble());
  }

  /**
   * <p>This method is used when the database is being dumped, to write
   * out this field to disk.  It is mated with receiveXML().</p>
   */

  synchronized void emitXML(XMLDumpContext xmlOut) throws IOException
  {
    xmlOut.startElementIndent(this.getXMLName());
    emitDoubleXML(xmlOut, value());
    xmlOut.endElement(this.getXMLName());
  }

  public void emitDoubleXML(XMLDumpContext xmlOut, double value) throws IOException
  {
    xmlOut.startElement("float");
    xmlOut.attribute("val", java.lang.Double.toString(value));
    xmlOut.endElement("float");
  }

  // ****
  //
  // type-specific accessor methods
  //
  // ****

  public double value()
  {
    if (isVector())
      {
	throw new IllegalArgumentException("scalar accessor called on vector field");
      }

    return ((Double) value).doubleValue();
  }

  public double value(int index)
  {
    // float can't be a vector field

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

    return Double.toString(this.value());
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
    FloatDBField origN;
    StringBuffer result = new StringBuffer();

    /* -- */

    if (!(orig instanceof FloatDBField))
      {
	throw new IllegalArgumentException("bad field comparison");
      }

    origN = (FloatDBField) orig;

    if (origN.value() != this.value())
      {
	result.append("\tOld: ");
	result.append(Double.toString(origN.value()));
	result.append("\n\tNew: ");
	result.append(Double.toString(this.value()));
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
  // float_field specific methods
  //
  // ****

  /**
   *
   * Returns true if this field has max/min
   * limitations.
   *
   * @see arlut.csd.ganymede.float_field
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

    return eObj.isFloatLimited(this);
  }

  /**
   *
   * Returns the minimum acceptable value for this field if this field
   * has max/min limitations.  
   *
   * @see arlut.csd.ganymede.float_field
   * 
   */

  public double getMinValue()
  {
    DBEditObject eObj;

    /* -- */

    if (!isEditable(true))
      {
	throw new IllegalArgumentException("not applicable to a non-editable field/object");
      }

    eObj = (DBEditObject) owner;

    return eObj.minFloat(this);
  }

  /**
   *
   * Returns the maximum acceptable value for this field if this field
   * has max/min limitations.
   *
   * @see arlut.csd.ganymede.float_field
   * 
   */

  public double getMaxValue()
  {
    DBEditObject eObj;

    /* -- */

    if (!isEditable(true))
      {
	throw new IllegalArgumentException("not applicable to a non-editable field/object");
      }

    eObj = (DBEditObject) owner;

    return eObj.maxFloat(this);
  }

  // ****
  //
  // Overridable methods for implementing intelligent behavior
  //
  // ****

  public boolean verifyTypeMatch(Object o)
  {
    return ((o == null) || (o instanceof Double));
  }

  public ReturnVal verifyNewValue(Object o)
  {
    DBEditObject eObj;
    Double I;

    /* -- */

    eObj = (DBEditObject) owner;

    if (!verifyTypeMatch(o))
      {
	return Ganymede.createErrorDialog("Float Field Error",
					  "Submitted value " + o + " is not a double!  Major client error while" +
					  " trying to edit field " + getName() +
					  " in object " + owner.getLabel());
      }

    if (o == null)
      {
	return eObj.verifyNewValue(this, o);
      }

    I = (Double) o;

    if (limited())
      {
	if (getMinValue() > I.doubleValue())
	  {
	    return Ganymede.createErrorDialog("Float Field Error",
					      "Submitted float  " + I + " is out of range for field " +
					      getName() + " in object " + owner.getLabel() + 
					      ".  This field will not accept floats less than " + getMinValue());
	  }

	if (getMaxValue() < I.doubleValue())
	  {
	    return Ganymede.createErrorDialog("Float Field Error",
					      "Submitted float  " + I + " is out of range for field " +
					      getName() + " in object " + owner.getLabel() + 
					      ".  This field will not accept floats greater than " + getMaxValue());
	  }
      }

    // have our parent make the final ok on the value

    return eObj.verifyNewValue(this, o);
  }

}
