/*
   GASH 2

   DateDBField.java

   The GANYMEDE object storage system.

   Created: 2 July 1996
   Release: $Name:  $
   Version: $Revision: 1.20 $
   Last Mod Date: $Date: 2000/01/08 03:28:57 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999, 2000
   The University of Texas at Austin.

   Contact information

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

/*------------------------------------------------------------------------------
                                                                           class
                                                                     DateDBField

------------------------------------------------------------------------------*/

/**
 * <P>DateDBField is a subclass of {@link arlut.csd.ganymede.DBField DBField}
 * for the storage and handling of Date
 * fields in the {@link arlut.csd.ganymede.DBStore DBStore} on the Ganymede
 * server.</P>
 *
 * <P>The Ganymede client talks to DateDBFields through the
 * {@link arlut.csd.ganymede.date_field date_field} RMI interface.</P> 
 *
 * <P>Ganymede uses the standard Java Date class, which can encode dates
 * from roughly 300 million years B.C. to 300 million years A.D., with
 * millisecond resolution.  No Y2k problems here. ;-)</P>
 */

public class DateDBField extends DBField implements date_field {

  /**
   * <P>Receive constructor.  Used to create a DateDBField from a
   * {@link arlut.csd.ganymede.DBStore DBStore}/{@link arlut.csd.ganymede.DBJournal DBJournal}
   * DataInput stream.</P>
   */

  DateDBField(DBObject owner, DataInput in, DBObjectBaseField definition) throws IOException
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

  DateDBField(DBObject owner, DBObjectBaseField definition)
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

  public DateDBField(DBObject owner, DateDBField field)
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

  public DateDBField(DBObject owner, Date value, DBObjectBaseField definition)
  {
    this.owner = owner;
    this.definition = definition;
    this.value = value;
  }

  /**
   *
   * Vector value constructor.
   *
   */

  public DateDBField(DBObject owner, Vector values, DBObjectBaseField definition)
  {
    throw new IllegalArgumentException("vector constructor called on scalar field");
  }

  public Object clone()
  {
    return new DateDBField(owner, this);
  }

  void emit(DataOutput out) throws IOException
  {
    out.writeLong(((Date) value).getTime());
  }

  void receive(DataInput in) throws IOException
  {
    value = new Date(in.readLong());
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

  public synchronized String getValueString()
  {
    /* -- */

    if (!verifyReadPermission())
      {
	throw new IllegalArgumentException("permission denied to read this field");
      }

    if (value == null)
      {
	return "null";
      }
    
    return this.value.toString();
  }

  /**
   *
   * Date fields need a special encoding that can be easily
   * reversed to obtain a date object on the client for sorting
   * and selection purposes.
   *
   */

  public synchronized String getEncodingString()
  {
    if (!verifyReadPermission())
      {
	throw new IllegalArgumentException("permission denied to read this field");
      }

    if (value == null)
      {
	return "null";
      }

    return Long.toString(((Date) this.value).getTime());
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
    DateDBField origD;
    StringBuffer result = new StringBuffer();

    /* -- */

    if (!(orig instanceof DateDBField))
      {
	throw new IllegalArgumentException("bad field comparison");
      }

    origD = (DateDBField) orig;

    if (!origD.value().equals(this.value()))
      {
	result.append("\tOld: ");
	result.append(origD.value.toString());
	result.append("\n\tNew: ");
	result.append(this.value.toString());
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
  // date_field methods
  //
  // ****

  /**
   * <P>Returns true if this date field has a minimum and/or maximum date
   * set.</P>
   *
   * <P>We depend on our owner's 
   * {@link arlut.csd.ganymede.DBEditObject#isDateLimited(arlut.csd.ganymede.DBField) isDateLimited()}
   * method to tell us whether this Date field should be limited or not
   * in this editing context.</P>
   *
   * @see arlut.csd.ganymede.date_field
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

    return eObj.isDateLimited(this);
  }

  /**
   * <P>Returns the earliest date acceptable for this field</P>
   *
   * <P>We depend on our owner's 
   * {@link arlut.csd.ganymede.DBEditObject#minDate(arlut.csd.ganymede.DBField) minDate()}
   * method to tell us what the earliest acceptable Date for this field is.</P>
   *
   * @see arlut.csd.ganymede.date_field
   */

  public Date minDate()
  {
    DBEditObject eObj;

    /* -- */

    if (!isEditable(true))
      {
	throw new IllegalArgumentException("not applicable to a non-editable field/object");
      }

    eObj = (DBEditObject) owner;

    return eObj.minDate(this);
  }

  /**
   * <P>Returns the latest date acceptable for this field</P>
   *
   * <P>We depend on our owner's 
   * {@link arlut.csd.ganymede.DBEditObject#maxDate(arlut.csd.ganymede.DBField) maxDate()}
   * method to tell us what the earliest acceptable Date for this field is.</P>
   *
   * @see arlut.csd.ganymede.date_field
   */

  public Date maxDate()
  {
    DBEditObject eObj;

    /* -- */

    if (!isEditable(true))
      {
	throw new IllegalArgumentException("not applicable to a non-editable field/object");
      }

    eObj = (DBEditObject) owner;

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
    Vector v;
    boolean ok = true;

    /* -- */

    if (!isEditable(true))
      {
	return Ganymede.createErrorDialog("Date Field Error",
					  "Don't have permission to edit field " + getName() +
					  " in object " + owner.getLabel());
      }

    eObj = (DBEditObject) owner;

    if (!verifyTypeMatch(o))
      {
	return Ganymede.createErrorDialog("Date Field Error",
					  "Submitted value " + o + " is not a date!  Major client error while" +
					  " trying to edit field " + getName() +
					  " in object " + owner.getLabel());
      }

    if (o == null)
      {
	return eObj.verifyNewValue(this, o);
      }

    d = (Date) o;

    if (limited())
      {
	d2 = minDate();

	if (d2 != null)
	  {
	    if (d.before(d2))
	      {
		return Ganymede.createErrorDialog("Date Field Error",
						  "Submitted date  " + d + " is out of range for field " +
						  getName() + " in object " + owner.getLabel() + 
						  ".  This field will not accept dates before " + d2);
	      }
	  }

	d2 = maxDate();
	if (d2 != null)
	  {
	    if (d.after(d2))
	      {
		return Ganymede.createErrorDialog("Date Field Error",
						  "Submitted date  " + d + " is out of range for field " +
						  getName() + " in object " + owner.getLabel() + 
						  ".  This field will not accept dates after " + d2);
	      }
	  }
      }

    // have our parent make the final ok on the value

    return eObj.verifyNewValue(this, o);
  }
}
