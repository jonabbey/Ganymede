/*
   GASH 2

   BooleanDBField.java

   The GANYMEDE object storage system.

   Created: 2 July 1996
   Release: $Name:  $
   Version: $Revision: 1.26 $
   Last Mod Date: $Date: 2000/08/22 06:43:39 $
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
import java.lang.reflect.*;

import com.jclark.xml.output.*;
import arlut.csd.Util.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                  BooleanDBField

------------------------------------------------------------------------------*/

/**
 * <P>BooleanDBField is a subclass of {@link arlut.csd.ganymede.DBField DBField}
 * for the storage and handling of boolean
 * fields in the {@link arlut.csd.ganymede.DBStore DBStore} on the Ganymede
 * server.</P>
 *
 * <P>The Ganymede client talks to BooleanDBFields through the
 * {@link arlut.csd.ganymede.boolean_field boolean_field} RMI interface.</P> 
 */

public class BooleanDBField extends DBField implements boolean_field {

  /**
   *  Dynamically loaded RMI proxy class definition for this class.
   */

  static Class proxyClass = null;

  /**
   *  Dynamically loaded constructor for instances of the RMI proxy
   */

  static java.lang.reflect.Constructor proxyConstructor = null;

  // ---

  /**
   * <P>Receive constructor.  Used to create a BooleanDBField from a
   * {@link arlut.csd.ganymede.DBStore DBStore}/{@link arlut.csd.ganymede.DBJournal DBJournal}
   * DataInput stream.</P>
   */

  BooleanDBField(DBObject owner, DataInput in, DBObjectBaseField definition) throws IOException
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

  BooleanDBField(DBObject owner, DBObjectBaseField definition)
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

  public BooleanDBField(DBObject owner, BooleanDBField field)
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

  public BooleanDBField(DBObject owner, boolean value, DBObjectBaseField definition)
  {
    this.owner = owner;
    this.definition = definition;
    this.value = new Boolean(value);
  }

  /**
   *
   * Vector value constructor.
   *
   */

  public BooleanDBField(DBObject owner, Vector values, DBObjectBaseField definition)
  {
    throw new IllegalArgumentException("vector constructor called on scalar field");
  }

  public Object clone()
  {
    return new BooleanDBField(owner, this);
  }
  /**
   * <p>This method creates an RMI proxy object that channels db_field
   * calls to a GanymedeSession object over RMI rather than over a
   * direct RMI field reference.  This is valuable because it can
   * dramatically decrease the number of references that have to be
   * managed through distributed garbage collection, increasing the
   * scalability of the server at the cost of a constant time increase
   * in initial communications for downloading the proxy.</p>
   */

  public db_field getProxy()
  {
    Object proxy = null;

    /* -- */

    synchronized (getClass())
      {
	if (proxyClass == null)
	  {
	    // load

	    try
	      {
		proxyClass = Class.forName("arlut.csd.ganymede.boolean_fieldRemote");
	      }
	    catch (ClassNotFoundException ex)
	      {
		System.err.println("DBField.getProxy(): couldn't find arlut.csd.ganymede.boolean_fieldRemote");
		ex.printStackTrace();

		return null;
	      }
	  }

	if (proxyConstructor != null)
	  {
	    Class constructParams[] = new Class[3];

	    try
	      {
		constructParams[0] = Class.forName("arlut.csd.ganymede.Invid");
		constructParams[1] = short.class;
		constructParams[2] = Class.forName("arlut.csd.ganymede.Session");
	      }
	    catch (ClassNotFoundException ex)
	      {
		System.err.println("DBField.getProxy(): couldn't find proxy constructor: " + ex.getMessage());
		ex.printStackTrace();

		return null;
	      }
	    
	    try
	      {
		proxyConstructor = proxyClass.getConstructor(constructParams);
	      }
	    catch (NoSuchMethodException ex)
	      {
		ex.printStackTrace();
	      }
	    catch (SecurityException ex)
	      {
		ex.printStackTrace();
	      }
	  }

	Object params[] = new Object[3];

	params[0] = getOwner().getInvid();
	params[1] = new Short(getID());
	params[2] = getOwner().getGSession();
	
	try
	  {
	    proxy = proxyConstructor.newInstance(params);
	  }
	catch (InstantiationException ex)
	  {
	    ex.printStackTrace();
	  }
	catch (IllegalArgumentException ex)
	  {
	    ex.printStackTrace();
	  }
	catch (IllegalAccessException ex)
	  {
	    ex.printStackTrace();
	  }
	catch (InvocationTargetException ex)
	  {
	    ex.printStackTrace();
	  }
	
	return (db_field) proxy;
      }
  }

  void emit(DataOutput out) throws IOException
  {
    out.writeBoolean(value());
  }

  void receive(DataInput in) throws IOException
  {
    value = new Boolean(in.readBoolean());
  }

  /**
   * <p>This method is used when the database is being dumped, to write
   * out this field to disk.  It is mated with receiveXML().</p>
   */

  synchronized void emitXML(XMLDumpContext xmlOut) throws IOException
  {
    xmlOut.indent();

    xmlOut.startElement(this.getXMLName());
    xmlOut.startElement("boolean");
    xmlOut.attribute("val", value() ? "true" : "false");
    xmlOut.endElement("boolean");
    xmlOut.endElement(this.getXMLName());
  }

  /**
   *
   * Returns true if this field has a value associated
   * with it, or false if it is an unfilled 'placeholder'.
   *
   * @see arlut.csd.ganymede.db_field
   *
   */

  public boolean isDefined()
  {
    return value();
  }

  /**
   * <P>This method is used to mark a field as undefined when it is
   * checked out for editing.  Different subclasses of
   * {@link arlut.csd.ganymede.DBField DBField} may
   * implement this in different ways, if simply setting the field's
   * value member to null is not appropriate.  Any namespace values claimed
   * by the field will be released, and when the transaction is
   * committed, this field will be released.</P>
   */

  public synchronized ReturnVal setUndefined(boolean local)
  {
    if (isEditable(local))
      {
	value = Boolean.FALSE;
	return null;
      }

    return Ganymede.createErrorDialog("Permissions Error",
				      "Don't have permission to clear this permission matrix field\n" +
				      getName());
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
   * <P>This method returns a text encoded value for this BooleanDBField
   * without checking permissions.</P>
   *
   * <P>This method avoids checking permissions because it is used on
   * the server side only and because it is involved in the 
   * {@link arlut.csd.ganymede.DBObject#getLabel() getLabel()}
   * logic for {@link arlut.csd.ganymede.DBObject DBObject}, 
   * which is invoked from {@link arlut.csd.ganymede.GanymedeSession GanymedeSession}'s
   * {@link arlut.csd.ganymede.GanymedeSession#getPerm(arlut.csd.ganymede.DBObject) getPerm()} 
   * method.</P>
   *
   * <P>If this method checked permissions and the getPerm() method
   * failed for some reason and tried to report the failure using
   * object.getLabel(), as it does at present, the server could get
   * into an infinite loop.</P>
   */

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
    
    return (this.value() ? "True" : "False");
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
    BooleanDBField origB;
    StringBuffer result = new StringBuffer();

    /* -- */

    if (!(orig instanceof BooleanDBField))
      {
	throw new IllegalArgumentException("bad field comparison");
      }

    origB = (BooleanDBField) orig;

    if (origB.value() != this.value())
      {
	result.append("\tOld: ");
	result.append(origB.value() ? "True" : "False");
	result.append("\n\tNew: ");
	result.append(this.value() ? "True" : "False");
	
	return result.toString();
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
   * @see arlut.csd.ganymede.boolean_field
   */

  public boolean labeled()
  {
    return definition.isLabeled();
  }

  /**
   *
   * Returns the true label if this field is defined to have the true/false
   * values associated with labels.
   *
   * @see arlut.csd.ganymede.boolean_field
   */

  public String trueLabel()
  {
    return definition.getTrueLabel();
  }

  /**
   *
   * Returns the false label if this field is defined to have the true/false
   * values associated with labels.
   *
   * @see arlut.csd.ganymede.boolean_field
   */

  public String falseLabel()
  {
    return definition.getFalseLabel();
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
	return Ganymede.createErrorDialog("Boolean Field Error",
					  "Don't have permission to edit field " + getName() +
					  " in object " + owner.getLabel());
      }

    eObj = (DBEditObject) owner;

    if (!verifyTypeMatch(o))
      {
	return Ganymede.createErrorDialog("Boolean Field Error",
					  "Submitted value " + o + " is not a boolean!  Major client error while" +
					  " trying to edit field " + getName() +
					  " in object " + owner.getLabel());
      }

    // have our parent make the final ok on the value

    return eObj.verifyNewValue(this, o);
  }

}

