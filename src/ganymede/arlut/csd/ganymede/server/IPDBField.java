/*
   GASH 2

   IPDBField.java

   The GANYMEDE object storage system.

   Created: 4 Sep 1997
   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996-2005
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
   along with this program; if not, write to the Free Software
   Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
   02111-1307, USA

*/

package arlut.csd.ganymede.server;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Random;
import java.util.Vector;

import arlut.csd.ganymede.common.GanyPermissionsException;
import arlut.csd.ganymede.common.ReturnVal;
import arlut.csd.ganymede.rmi.ip_field;
import arlut.csd.JDialog.JDialogBuff;
import arlut.csd.Util.TranslationService;
import arlut.csd.Util.VectorUtils;

/*------------------------------------------------------------------------------
                                                                           class
                                                                       IPDBField

------------------------------------------------------------------------------*/

/**
 * <P>IPDBField is a subclass of {@link arlut.csd.ganymede.server.DBField DBField}
 * for the storage and handling of IPv4/IPv6 address
 * fields in the {@link arlut.csd.ganymede.server.DBStore DBStore} on the Ganymede
 * server.</P>
 *
 * <P>The Ganymede client talks to IPDBFields through the
 * {@link arlut.csd.ganymede.rmi.ip_field ip_field} RMI interface.</P> 
 *
 * <P>Note that wherever Ganymede manipulates IP addresses, it does so
 * in terms of unsigned byte arrays.  Since Java does not provide an
 * unsigned byte type, Ganymede uses the {@link
 * arlut.csd.ganymede.server.IPDBField#s2u(byte) s2u()} and {@link
 * arlut.csd.ganymede.server.IPDBField#u2s(int) u2s()} static methods defined
 * in this class to convert from the signed Java byte to the Ganymede
 * 0-255 IP octet range.</P>
 */

public class IPDBField extends DBField implements ip_field {

  /**
   * <p>TranslationService object for handling string localization in
   * the Ganymede server.</p>
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.server.IPDBField");

  static final boolean debug = false;
  private static String IPv4allowedChars = "1234567890.";
  private static String IPv6allowedChars = "1234567890.ABCDEF:";

  // --

  /**
   * <P>Receive constructor.  Used to create a IPDBField from a
   * {@link arlut.csd.ganymede.server.DBStore DBStore}/{@link arlut.csd.ganymede.server.DBJournal DBJournal}
   * DataInput stream.</P>
   */

  IPDBField(DBObject owner, DataInput in, DBObjectBaseField definition) throws IOException
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

  IPDBField(DBObject owner, DBObjectBaseField definition)
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
   *
   * Copy constructor.
   *
   */

  public IPDBField(DBObject owner, IPDBField field)
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
   *
   * Scalar value constructor.
   *
   */

  public IPDBField(DBObject owner, Date value, DBObjectBaseField definition)
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
   *
   * Vector value constructor.
   *
   */

  public IPDBField(DBObject owner, Vector values, DBObjectBaseField definition)
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

  /**
   * <p>This method is used to return a copy of this field, with the field's owner
   * set to newOwner.</p>
   */

  public DBField getCopy(DBObject newOwner)
  {
    return new IPDBField(newOwner, this);
  }

  public Object clone()
  {
    return new IPDBField(owner, this);
  }

  void emit(DataOutput out) throws IOException
  {
    if (isVector())
      {
	Vector values = getVectVal();

	out.writeInt(values.size());

	for (int i = 0; i < values.size(); i++)
	  {
	    Byte[] element = (Byte[]) values.elementAt(i);

	    out.writeByte(element.length);

	    for (int j = 0; j < element.length; j++)
	      {
		out.writeByte(element[j].byteValue());
	      }
	  }
      }
    else
      {
	Byte[] element = (Byte[]) value;

	out.writeByte(element.length);

	for (int j = 0; j < element.length; j++)
	  {
	    out.writeByte(element[j].byteValue());
	  }
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
	    byte length = in.readByte();

	    Byte[] element = new Byte[length];
	    
	    for (int j = 0; j < length; j++)
	      {
		element[j] = new Byte(in.readByte());
	      }

	    values.addElement(element);
	  }
      }
    else
      {
	byte length = in.readByte();
	    
	Byte[] element = new Byte[length];
	    
	for (int j = 0; j < length; j++)
	  {
	    element[j] = new Byte(in.readByte());
	  }

	value = element;
      }
  }

  /**
   * <p>This method is used when the database is being dumped, to write
   * out this field to disk.  It is mated with receiveXML().</p>
   */

  synchronized void emitXML(XMLDumpContext xmlOut) throws IOException
  {
    xmlOut.startElementIndent(this.getXMLName());

    if (!isVector())
      {
	emitIPXML(xmlOut, value());
      }
    else
      {
	Vector values = getVectVal();

	for (int i = 0; i < values.size(); i++)
	  {
	    xmlOut.indentOut();
	    xmlOut.indent();
	    xmlOut.indentIn();

	    emitIPXML(xmlOut, (Byte[]) values.elementAt(i));
	  }
	
	xmlOut.indent();
      }

    xmlOut.endElement(this.getXMLName());
  }

  public void emitIPXML(XMLDumpContext xmlOut, Byte[] value) throws IOException
  {
    xmlOut.startElement("ip");
    xmlOut.attribute("val", genIPString(value));
    xmlOut.endElement("ip");
  }

  public void emitIPXML(XMLDumpContext xmlOut, IPwrap value) throws IOException
  {
    xmlOut.startElement("ip");
    xmlOut.attribute("val", value.toString());
    xmlOut.endElement("ip");
  }

  /**
   * <p>Sub-class hook to support elements for which the default
   * equals() test is inadequate, such as IP addresses (represented
   * as arrays of Byte[] objects.</p>
   *
   * <p>Returns -1 if the value was not found in this field.</p>
   *
   * <p>This method assumes that the calling method has already verified
   * that this is a vector field.</p>
   */

  public int indexOfValue(Object value)
  {
    if (!(value instanceof Byte[]))
      {
	return -1;
      }

    Byte[] foreignBytes = (Byte[]) value;

    Vector values = getVectVal();

    for (int i = 0; i < values.size(); i++)
      {
	Byte[] localBytes = (Byte[]) values.elementAt(i);
	
	if (equalTest(localBytes, foreignBytes))
	  {
	    return i;
	  }
      }

    return -1;
  }

  /**
   *
   * Equality test.
   *
   */

  private boolean equalTest(Byte[] localBytes, Byte[] foreignBytes)
  {
    if ((foreignBytes == null) && (localBytes == null))
      {
	return true;
      }

    if (((foreignBytes == null) && (localBytes != null)) ||
	((foreignBytes != null) && (localBytes == null)))
      {
	return false;
      }

    if (foreignBytes.length != localBytes.length)
      {
	return false;
      }

    for (int i = 0; i < localBytes.length; i++)
      {
	try
	  {
	    if (!foreignBytes[i].equals(localBytes[i]))
	      {
		return false;
	      }
	  }
	catch (NullPointerException ex)
	  {
	    return false;
	  }
      }

    return true;
  }


  // ****
  //
  // type-specific accessor methods
  //
  // ****

  /**
   * <P>Sets the value of this field, if a scalar.</P>
   *
   * <P>The {@link arlut.csd.ganymede.common.ReturnVal ReturnVal} object returned encodes
   * success or failure, and may optionally
   * pass back a dialog.</P>
   *
   * <P>Note that IPDBField needs its own setValue() method
   * (rather than using {@link arlut.csd.ganymede.server.DBField#setValue(java.lang.Object, boolean)
   * DBField.setValue()}
   * because it needs to be able to accept either a Byte[] array or
   * a String with IP information in either IPv4 or IPv6 encoding.</P>
   */

  public ReturnVal setValue(Object value, boolean local, boolean noWizards) throws GanyPermissionsException
  {
    DBNameSpace ns;
    DBEditObject eObj;
    Byte[] bytes;
    ReturnVal retVal = null;
    ReturnVal newRetVal = null;

    /* -- */

    if (!isEditable(local))
      {
	// "Can''t change IP field {1} in object {0}, due to a lack of permissions or due to the object being in a non-editable state."
	throw new GanyPermissionsException(ts.l("global.no_write_perms", owner.getLabel(), getName()));
      }

    if (isVector())
      {
	// "Scalar method called on a vector field: {1} in object {0}"
	throw new IllegalArgumentException(ts.l("global.oops_vector", owner.getLabel(), getName()));
      }

    if (value == null)
      {
	bytes = null;
      }
    else
      {
	bytes = genIPbytes(value);
      }

    retVal = verifyNewValue(bytes);
      
    if (retVal != null && !retVal.didSucceed())
      {
	return retVal;
      }

    eObj = (DBEditObject) owner;

    if (!noWizards && !local && eObj.getGSession().enableOversight)
      {
	// Wizard check

	retVal = eObj.wizardHook(this, DBEditObject.SETVAL, bytes, null);

	// if a wizard intercedes, we are going to let it take the ball.
	
	if (retVal != null && !retVal.doNormalProcessing)
	  {
	    return retVal;
	  }
      }

    // check to see if we can do the namespace manipulations implied by this
    // operation

    unmark(this.value);	// our old value

    if (bytes != null)
      {
	if (!mark(bytes))
	  {
	    if (this.value != null)
	      {
		mark(this.value); // we aren't clearing the old value after all
	      }

	    // "Server: Error in IPDBField.setValue()"
	    // "IP address {0} already in use in the server"
	    return Ganymede.createErrorDialog(ts.l("setValue.oops"),
					      ts.l("global.already_in_use", genIPString(bytes)));
	  }
      }

    // check our owner, do it.  Checking our owner should
    // be the last thing we do.. if it returns true, nothing
    // should stop us from running the change to completion

    newRetVal = eObj.finalizeSetValue(this, bytes);

    if (newRetVal == null || newRetVal.didSucceed())
      {
	if (bytes != null)
	  {
	    this.value = bytes;
	  }
	else
	  {
	    this.value = null;
	  }

	// if the return value from the wizard was not null,
	// it might have included rescan information, which
	// we'll want to combine with that from our 
	// finalizeSetValue() call.

	if (retVal != null)
	  {
	    return retVal.unionRescan(newRetVal);
	  }
	else
	  {
	    return newRetVal;		// success
	  }
      }
    else
      {
	// our owner disapproved of the operation,
	// undo the namespace manipulations, if any,
	// and finish up.

	unmark(bytes);
	mark(this.value);

	return newRetVal;
      }
  }


  /**
   * <P>Sets the value of this field, if a vector.</P>
   *
   * <P>The {@link arlut.csd.ganymede.common.ReturnVal ReturnVal} object returned encodes
   * success or failure, and may optionally
   * pass back a dialog.</P>
   *
   * <p>Note that vector fields in Ganymede are not allowed to contain
   * duplicate values.</p>
   *
   * <P>Note that IPDBField needs its own setElement() method
   * (rather than using {@link arlut.csd.ganymede.server.DBField#setElement(int, java.lang.Object, boolean)
   * DBField.setElement()}
   * because it needs to be able to accept either a Byte[] array or
   * a String with IP information in either IPv4 or IPv6 encoding.</P>
   */
  
  public ReturnVal setElement(int index, Object value, boolean local, boolean noWizards) throws GanyPermissionsException
  {
    DBNameSpace ns;
    DBEditObject eObj;
    Byte[] bytes;
    ReturnVal retVal = null;
    ReturnVal newRetVal = null;

    /* -- */

    if (!isEditable(local))
      {
	// "Can''t change IP field {1} in object {0}, due to a lack of permissions or due to the object being in a non-editable state."
	throw new GanyPermissionsException(ts.l("global.no_write_perms", owner.getLabel(), getName()));
      }

    if (!isVector())
      {
	// "Vector method called on a scalar field: {1} in object {0}"
	throw new IllegalArgumentException(ts.l("global.oops_scalar", owner.getLabel(), getName()));
      }

    if (value == null)
      {
	// "Server: Error in IPDBField.setElement()"
	// "Null value passed to {0}:{1}.setElement()"
	return Ganymede.createErrorDialog(ts.l("setElement.oops"),
					  ts.l("setElement.null_value", owner.getLabel(), getName()));
      }

    Vector values = getVectVal();

    if ((index < 0) || (index > values.size()))
      {
	throw new ArrayIndexOutOfBoundsException(index);
      }

    bytes = genIPbytes(value);

    // make sure we're not adding a duplicate item

    int oldIndex = this.indexOfValue(bytes);

    if (oldIndex == index)
      {
	return null;		// no-op
      }
    else if (oldIndex != -1)
      {
	return getDuplicateValueDialog("setElement", genIPString(bytes)); // duplicate
      }

    retVal = verifyNewValue(bytes);
      
    if (retVal != null && !retVal.didSucceed())
      {
	return retVal;
      }

    eObj = (DBEditObject) owner;

    if (!noWizards && !local && eObj.getGSession().enableOversight)
      {
	// Wizard check

	retVal = eObj.wizardHook(this, DBEditObject.SETELEMENT, new Integer(index), value);

	// if a wizard intercedes, we are going to let it take the ball.

	if (retVal != null && !retVal.doNormalProcessing)
	  {
	    return retVal;
	  }
      }

    // check to see if we can do the namespace manipulations implied by this
    // operation

    unmark(values.elementAt(index));

    if (bytes != null)
      {
	if (!mark(bytes))
	  {
	    mark(values.elementAt(index)); // we aren't clearing the old value after all
	    
	    // "Server: Error in IPDBField.setElement()"
	    // "IP address {0} already in use in the server"
	    return Ganymede.createErrorDialog(ts.l("setElement.oops"),
					      ts.l("global.already_in_use", genIPString(bytes)));
	  }
      }

    // check our owner, do it.  Checking our owner should
    // be the last thing we do.. if it returns true, nothing
    // should stop us from running the change to completion

    newRetVal = eObj.finalizeSetElement(this, index, bytes);

    if (newRetVal == null || newRetVal.didSucceed())
      {
	values.setElementAt(bytes, index);

	// if the return value from the wizard was not null,
	// it might have included rescan information, which
	// we'll want to combine with that from our 
	// finalizeSetElement() call.

	if (retVal != null)
	  {
	    return retVal.unionRescan(newRetVal);
	  }
	else
	  {
	    return newRetVal;		// success
	  }
      }
    else
      {
	// our owner disapproved of the operation,
	// undo the namespace manipulations, if any,
	// and finish up.

	unmark(bytes);
	mark(values.elementAt(index));

	return newRetVal;
      }
  }

  /**
   * <P>Adds an element to the end of this field, if a vector.</P>
   *
   * <P>The {@link arlut.csd.ganymede.common.ReturnVal ReturnVal} object returned encodes
   * success or failure, and may optionally
   * pass back a dialog.</P>
   *
   * <p>Note that vector fields in Ganymede are not allowed to contain
   * duplicate values.</p>
   *
   * <P>Note that IPDBField needs its own addElement() method
   * (rather than using {@link arlut.csd.ganymede.server.DBField#addElement(java.lang.Object, boolean)
   * DBField.addElement()}
   * because it needs to be able to accept either a Byte[] array or
   * a String with IP information in either IPv4 or IPv6 encoding.</P>
   */

  public ReturnVal addElement(Object submittedValue, boolean local, boolean noWizards) throws GanyPermissionsException
  {
    DBNameSpace ns;
    DBEditObject eObj;
    Byte[] bytes;
    ReturnVal retVal = null;
    ReturnVal newRetVal = null;

    /* -- */

    if (!isEditable(local))
      {
	// "Can''t change IP field {1} in object {0}, due to a lack of permissions or due to the object being in a non-editable state."
	throw new GanyPermissionsException(ts.l("global.no_write_perms", owner.getLabel(), getName()));
      }

    if (!isVector())
      {
	// "Vector method called on a scalar field: {1} in object {0}"
	throw new IllegalArgumentException(ts.l("global.oops_scalar", owner.getLabel(), getName()));
      }

    if (submittedValue == null)
      {
	// "Null value passed to {0}:{1}.addElement()"
	throw new IllegalArgumentException(ts.l("addElement.null_value", owner.getLabel(), getName()));
      }

    bytes = genIPbytes(submittedValue);

    retVal = verifyNewValue(bytes);

    if (retVal != null && !retVal.didSucceed())
      {
	return retVal;
      }

    // make sure we're not duplicating an item

    if (indexOfValue(bytes) != -1)
      {
	return getDuplicateValueDialog("addElement", genIPString(bytes)); // duplicate
      }

    if (size() >= getMaxArraySize())
      {
	// "Server: Error in IPDBField.addElement()"
	// "Error in IPDBField.addElement(): Field {1} already at or beyond array size limit in object {0}"
	return Ganymede.createErrorDialog(ts.l("addElement.oops"),
					  ts.l("addElement.too_big", owner.getLabel(), getName()));
      }

    eObj = (DBEditObject) owner;

    if (!noWizards && !local && eObj.getGSession().enableOversight)
      {
	// Wizard check

	retVal = eObj.wizardHook(this, DBEditObject.ADDELEMENT, value, null);

	// if a wizard intercedes, we are going to let it take the ball.

	if (retVal != null && !retVal.doNormalProcessing)
	  {
	    return retVal;
	  }
      }

    if (!mark(bytes))
      {
	// "Server: Error in IPDBField.addElement()"
	// "IP address {0} already in use in the server"
	return Ganymede.createErrorDialog(ts.l("addElement.oops"),
					  ts.l("global.already_in_use", genIPString(bytes)));
      }

    newRetVal = eObj.finalizeAddElement(this, bytes);

    if (newRetVal == null || newRetVal.didSucceed())
      {
	getVectVal().addElement(bytes);

	// if the return value from the wizard was not null,
	// it might have included rescan information, which
	// we'll want to combine with that from our 
	// finalizeAddElement() call.

	if (retVal != null)
	  {
	    return retVal.unionRescan(newRetVal);
	  }
	else
	  {
	    return newRetVal;		// success
	  }
      } 
    else
      {
	unmark(bytes);

	return newRetVal;
      }
  }

  /**
   * <p>Adds a set of elements to the end of this field, if a
   * vector.  Using addElements() to add a sequence of items
   * to a field may be many times more efficient than calling
   * addElement() repeatedly, as addElements() can do a single
   * server checkpoint before attempting to add all the values.</p>
   *
   * <P>Server-side method only</P>
   *
   * <p>The ReturnVal object returned encodes success or failure, and
   * may optionally pass back a dialog. If a success code is returned,
   * all values were added.  If failure is returned, no values
   * were added.</p>
   *
   * <p>Note that vector fields in Ganymede are not allowed to contain
   * duplicate values.</p>
   *
   * @param submittedValues Values to be added
   * @param local If true, permissions checking will be skipped
   * @param noWizards If true, wizards will be skipped
   * @param copyFieldMode If true, addElements will add any values
   * that it can, even if some values are refused by the server logic.
   * Any values that are skipped will be reported in a dialog passed
   * back in the returned ReturnVal.  This is intended to support
   * vector field cloning, in which we add what values may be cloned,
   * and skip the rest.
   */

  public synchronized ReturnVal addElements(Vector submittedValues, boolean local, 
					    boolean noWizards, boolean copyFieldMode) throws GanyPermissionsException
  {
    ReturnVal retVal = null;
    ReturnVal newRetVal = null;
    DBNameSpace ns;
    DBEditObject eObj;
    DBEditSet editset;
    Vector approvedValues = new Vector();

    /* -- */

    if (!isEditable(local))	// *sync* on GanymedeSession possible
      {
	// "Can''t change IP field {1} in object {0}, due to a lack of permissions or due to the object being in a non-editable state."
	throw new GanyPermissionsException(ts.l("global.no_write_perms", owner.getLabel(), getName()));
      }

    if (!isVector())
      {
	// "Vector method called on a scalar field: {1} in object {0}"
	throw new IllegalArgumentException(ts.l("global.oops_scalar", owner.getLabel(), getName()));
      }

    if (submittedValues == null || submittedValues.size() == 0)
      {
	// "Server: Error in IPDBField.addElements()"
	// "IPDBField.addElements(): Can''t add a null or empty vector to field {1} in object {0}"
	return Ganymede.createErrorDialog(ts.l("addElements.oops"),
					  ts.l("addElements.null_value", owner.getLabel(), getName()));
      }

    if (submittedValues == getVectVal())
      {
	// "Error, IPDBField.addElements(): attempt to add our own contents back to us"
	throw new IllegalArgumentException(ts.l("addElements.self_error"));
      }

    // can we add this many values?  if not, just go ahead and reject,
    // even if copyFieldMode is set to false.  we shouldn't have
    // too many things to stuff into this field since we *should* just
    // be copying from a field with the same limits defined

    if (size() + submittedValues.size() > getMaxArraySize())
      {
	// "Server: Error in IPDBField.addElements()"
	// "Error in IPDBField.addElements(): Field {1} in object {0} is limited to {2} items.  Can't add the {3} additional items requested."
	return Ganymede.createErrorDialog(ts.l("addElements.oops"),
					  ts.l("addElements.too_big", owner.getLabel(), getName(), new Integer(getMaxArraySize()),
					       new Integer(submittedValues.size())));
      }

    // check for duplicate values.. if we're not in copyFieldMode, we
    // simply won't allow duplicates to be added.  If we are in
    // copyFieldMode, we'll filter out any duplicates and proceed

    Vector wrappedCurrValues = getWrappedVector(getVectVal());
    Vector wrappedAddValues = getWrappedVector(submittedValues);

    Vector duplicateValues = VectorUtils.intersection(wrappedCurrValues, wrappedAddValues);

    if (duplicateValues.size() > 0)
      {
	if (!copyFieldMode)
	  {
	    return getDuplicateValuesDialog("addElements", VectorUtils.vectorString(duplicateValues));
	  }
	else
	  {
	    Vector wrappedNonDuplicates = VectorUtils.difference(wrappedAddValues, wrappedCurrValues);

	    submittedValues = new Vector(wrappedNonDuplicates.size());

	    for (int i = 0; i < wrappedNonDuplicates.size(); i++)
	      {
		IPwrap wrapper = (IPwrap) wrappedNonDuplicates.elementAt(i);
		submittedValues.addElement(wrapper.address);
	      }
	  }
      }

    // check to see if all of the submitted values are acceptable in
    // type and in identity.  if copyFieldMode, we won't complain
    // unless none of the submitted values are acceptable.

    StringBuffer errorBuf = new StringBuffer();

    for (int i = 0; i < submittedValues.size(); i++)
      {
	Byte[] bytes = genIPbytes(submittedValues.elementAt(i));
	retVal = verifyNewValue(bytes);

	if (retVal != null && !retVal.didSucceed())
	  {
	    if (!copyFieldMode)
	      {
		return retVal;
	      }
	    else
	      {
		if (retVal.getDialog() != null)
		  {
		    if (errorBuf.length() != 0)
		      {
			errorBuf.append("\n\n");
		      }

		    errorBuf.append(retVal.getDialog().getText());
		  }
	      }
	  }
	else
	  {
	    approvedValues.addElement(bytes);
	  }
      }

    if (approvedValues.size() == 0)
      {
	return Ganymede.createErrorDialog("AddElements Error",
					  errorBuf.toString());
      }

    // see if our container wants to intercede in the adding operation

    eObj = (DBEditObject) owner;
    editset = eObj.getEditSet();

    if (!noWizards && !local && eObj.getGSession().enableOversight)
      {
	// Wizard check

	retVal = eObj.wizardHook(this, DBEditObject.ADDELEMENTS, approvedValues, null);

	// if a wizard intercedes, we are going to let it take the ball.

	if (retVal != null && !retVal.doNormalProcessing)
	  {
	    return retVal;
	  }
      }

    // check to see if the all of the values being added are
    // acceptable to a namespace constraint

    ns = getNameSpace();

    if (ns != null)
      {
	synchronized (ns)
	  {
	    for (int i = 0; i < approvedValues.size(); i++)
	      {
		if (!ns.testmark(editset, approvedValues.elementAt(i)))
		  {
		    return getConflictDialog("DBField.addElements()", approvedValues.elementAt(i));
		  }
	      }
	
	    for (int i = 0; i < approvedValues.size(); i++)
	      {
		if (!ns.mark(editset, approvedValues.elementAt(i), this))
		  {
		    throw new RuntimeException("error: testmark / mark inconsistency");
		  }
	      }
	  }
      }

    // okay, see if the DBEditObject is willing to allow all of these
    // elements to be added

    newRetVal = eObj.finalizeAddElements(this, approvedValues);

    if (newRetVal == null || newRetVal.didSucceed()) 
      {
	// okay, we're allowed to do it, so go go go!

	for (int i = 0; i < approvedValues.size(); i++)
	  {
	    getVectVal().addElement(approvedValues.elementAt(i));
	  }

	// if the return value from the wizard was not null,
	// it might have included rescan information, which
	// we'll want to combine with that from our 
	// finalizeAddElement() call.

	if (retVal != null)
	  {
	    newRetVal = retVal.unionRescan(newRetVal);
	  }

	if (newRetVal == null)
	  {
	    newRetVal = new ReturnVal(true, true);
	  }

	// if we were not able to copy some of the values (and we
	// had copyFieldMode set), encode a description of what
	// happened along with the success code
	
	if (errorBuf.length() != 0)
	  {
	    newRetVal.setDialog(new JDialogBuff("Warning",
						errorBuf.toString(),
						"Ok",
						null,
						"ok.gif"));
	  }

	return newRetVal;
      } 
    else
      {
	if (ns != null)
	  {
	    // go back through and unmark everything.  we have a duty
	    // to try and unmark everything to keep things consistent,
	    // but if we get any error along the way, we'll want to
	    // throw an example up to let someone know.  we won't try
	    // and capture and transmit multiple errors, because this
	    // kind of error really shouldn't happen

	    RuntimeException badBoy = null;

	    for (int i = 0; i < approvedValues.size(); i++)
	      {
		if (!ns.unmark(editset, approvedValues.elementAt(i), this))
		  {
		    if (badBoy == null)
		      {
			// "Error encountered attempting to dissociate reserved value {0} from field {1}.  This should not have happened, and must be due to a server error."
			throw new RuntimeException(ts.l("global.really_bad_unmark", approvedValues.elementAt(i), this));
		      }
		  }
	      }

	    if (badBoy != null)
	      {
		throw badBoy;
	      }
	  }

	// return the error dialog created by finalizeAddElements

	return newRetVal;
      }
  }

  public Byte[] value()
  {
    if (isVector())
      {
	// "Scalar method called on a vector field: {1} in object {0}"
	throw new IllegalArgumentException(ts.l("global.oops_vector", owner.getLabel(), getName()));
      }

    return (Byte[]) value;
  }

  public Byte[] value(int index)
  {
    if (!isVector())
      {
	// "Vector method called on a scalar field: {1} in object {0}"
	throw new IllegalArgumentException(ts.l("global.oops_scalar", owner.getLabel(), getName()));
      }

    return (Byte[]) getVectVal().elementAt(index);
  }

  public synchronized String getValueString()
  {
    String result = "";

    /* -- */

    if (!isVector())
      {
	if (value == null)
	  {
	    return "null";
	  }

	return genIPString((Byte []) value);
      }

    Vector values = getVectVal();

    int size = size();

    for (int i = 0; i < size; i++)
      {
	Byte[] element = (Byte[]) values.elementAt(i);

	if (!result.equals(""))
	  {
	    result = result + ",";
	  }

	result = result + genIPString(element);
      }

    return result;
  }

  /**
   *
   * The normal getValueString() encoding of IP addresses is acceptable.
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

  public synchronized String getDiffString(DBField orig)
  {
    StringBuffer result = new StringBuffer();
    IPDBField origI;

    /* -- */

    if (!(orig instanceof IPDBField))
      {
	throw new IllegalArgumentException("bad field comparison");
      }

    origI = (IPDBField) orig;

    if (isVector())
      {
	Vector wrappedValues = getWrappedVector(getVectVal());
	Vector wrappedOrigValues = getWrappedVector(origI.getVectVal());
	Vector added = VectorUtils.difference(wrappedValues, wrappedOrigValues);
	Vector deleted = VectorUtils.difference(wrappedOrigValues, wrappedValues);

	// were there any changes at all?

	if (deleted.size() == 0 && added.size() == 0)
	  {
	    return null;
	  }
	else
	  {
	    if (deleted.size() != 0)
	      {
		result.append("\tDeleted: ");
		result.append(VectorUtils.vectorString(deleted));
		result.append("\n");
	      }

	    if (added.size() != 0)
	      {
		result.append("\tAdded: ");
		result.append(VectorUtils.vectorString(added));
		result.append("\n");
	      }

	    return result.toString();
	  }
      }
    else
      {
	Byte[] x = (Byte[]) origI.value;
	Byte[] y = (Byte[]) this.value;

	if (x.length == y.length)
	  {
	    boolean nochange = true;

	    for (int j = 0; j < x.length; j++)
	      {
		if (x[j] != y[j])
		  {
		    nochange = false;
		  }
	      }

	    if (nochange)
	      {
		return null;
	      }
	  }

	result.append("\tOld: ");
	result.append(genIPString(x));
	result.append("\n\tNew: ");
	result.append(genIPString(y));
	result.append("\n");
	
	return result.toString();
      }
  }

  // ****
  //
  // ip_field methods
  //
  // ****

  /**
   *
   * Returns true if this field is permitted to hold IPv6 addresses.
   *
   */

  public boolean v6Allowed()
  {
    DBEditObject eObj = (DBEditObject) owner;

    return eObj.isIPv6OK(this);
  }

  /**
   *
   * Returns true if the value stored in this IP field is an IPV4
   * address.  If no value has been set for this field, false is
   * returned.
   *
   * @see arlut.csd.ganymede.rmi.ip_field
   *
   */

  public boolean isIPV4()
  {
    if (isVector())
      {
	throw new IllegalArgumentException("scalar accessor called on vector field");
      }

    Byte[] element = (Byte[]) value;

    if (element == null)
      {
	return false;
      }
    else
      {
	return (element.length == 4);
      }
  }

  /**
   *
   * Returns true if the value stored in the given element of this IP
   * field is an IPV4 address, if no value is set, this method will
   * return false.
   *
   * @param index Array index for the value to be checked
   *
   * @see arlut.csd.ganymede.rmi.ip_field
   * 
   */

  public boolean isIPV4(short index)
  {
    if (!isVector())
      {
	throw new IllegalArgumentException("vector accessor called on scalar field");
      }

    Byte[] element = (Byte[]) getVectVal().elementAt(index);

    if (element == null)
      {
	return false;
      }
    else
      {
	return (element.length == 4);
      }
  }

  /**
   *
   * Returns true if the value stored in this IP field is an IPV6 address.  If
   * this field has no value set, this method will return false by default.
   *
   * @see arlut.csd.ganymede.rmi.ip_field
   *
   */

  public boolean isIPV6()
  {
    if (isVector())
      {
	throw new IllegalArgumentException("scalar accessor called on vector field");
      }

    Byte[] element = (Byte[]) value;

    if (element == null)
      {
	return false;
      }
    else
      {
	return (element.length == 16);
      }
  }

  /**
   *
   * Returns true if the value stored in the given element of this IP
   * field is an IPV6 address.  If no value is stored in this field,
   * false is returned.
   *
   * @param index Array index for the value to be checked
   *
   * @see arlut.csd.ganymede.rmi.ip_field
   * 
   */

  public boolean isIPV6(short index)
  {
    if (!isVector())
      {
	throw new IllegalArgumentException("vector accessor called on scalar field");
      }

    Byte[] element = (Byte[]) getVectVal().elementAt(index);

    if (element == null)
      {
	return false;
      }
    else
      {
	return (element.length == 16);
      }
  }

  // ****
  //
  // Overridable methods for implementing intelligent behavior
  //
  // ****

  public boolean verifyTypeMatch(Object o)
  {
    return ((o == null) ||
	    (o instanceof Byte[] &&
	     ((((Byte[]) o).length == 4) ||
	      (((Byte[]) o).length == 16))));
  }

  public ReturnVal verifyNewValue(Object o)
  {
    DBEditObject eObj;

    /* -- */

    if (!isEditable(true))
      {
	return Ganymede.createErrorDialog("IP Field Error",
					  "Don't have permission to edit field " + getName() +
					  " in object " + owner.getLabel());
      }

    eObj = (DBEditObject) owner;

    if (!verifyTypeMatch(o))
      {
	return Ganymede.createErrorDialog("IP Field Error",
					  "Submitted value " + o + " is not a IP address!  Major client error while" +
					  " trying to edit field " + getName() +
					  " in object " + owner.getLabel());
      }

    // have our parent make the final ok on the value

    return eObj.verifyNewValue(this, o);
  }


  /**
   * Returns a {@link arlut.csd.ganymede.server.fieldDeltaRec fieldDeltaRec} 
   * object listing the changes between this field's state and that
   * of the prior oldField state.
   */

  public fieldDeltaRec getVectorDiff(DBField oldField)
  {
    if (!isVector())
      {
	// "Vector method called on a scalar field: {1} in object {0}"
	throw new IllegalArgumentException(ts.l("global.oops_scalar", owner.getLabel(), getName()));
      }

    if (oldField == null)
      {
	// "Can''t compare fields.. oldField is null"
	throw new IllegalArgumentException(ts.l("getVectorDiff.null_old"));
      }

    if ((oldField.getID() != getID()) ||
	(oldField.getObjTypeID() != getObjTypeID()))
      {
	// "Can''t compare fields.. incompatible field ids"
	throw new IllegalArgumentException(ts.l("getVectorDiff.bad_type"));
      }

    /* - */

    fieldDeltaRec deltaRec = new fieldDeltaRec(getID());
    Vector oldValues = getWrappedVector(oldField.getVectVal());
    Vector newValues = getWrappedVector(getVectVal());
    Vector addedValues = VectorUtils.difference(newValues, oldValues);
    Vector deletedValues = VectorUtils.difference(oldValues, newValues);

    deltaRec.addValues = getUnwrappedVector(addedValues);
    deltaRec.delValues = getUnwrappedVector(deletedValues);
    return deltaRec;
  }

  // 
  // helper methods for the encoding/decoding methods
  // to follow.
  //

  /**
   *
   * This method maps an int value between 0 and 255 inclusive
   * to a legal signed byte value.
   *
   */

  public final static byte u2s(int x)
  {
    if ((x < 0) || (x > 255))
      {
	throw new IllegalArgumentException("Out of range: " + x);
      }

    return (byte) (x - 128);
  }

  /**
   *
   * This method maps a u2s-encoded signed byte value to an
   * int value between 0 and 255 inclusive.
   *
   */

  public final static short s2u(byte b)
  {
    return (short) (b + 128);
  }

  /**
   *  determines whether a given character is valid or invalid for a JIPField
   *
   * @param ch the character which is being tested for its validity
   */

  private static final boolean isAllowedV4(char ch)
  {
    return !(IPv4allowedChars.indexOf(ch) == -1);
  }

  /**
   *  determines whether a given character is valid or invalid for a JIPField
   *
   * @param ch the character which is being tested for its validity
   */

  private static final boolean isAllowedV6(char ch)
  {
    return !(IPv6allowedChars.indexOf(ch) == -1);
  }

  public static String genIPString(Byte[] octets)
  {
    if (octets == null)
      {
	return null;
      }

    if (octets.length == 4)
      {
	return genIPV4string(octets);
      }
    
    return genIPV6string(octets);
  }

  /**
   * <p>This method is used take either a String or Byte array
   * representing an IP address and converts it to a Byte array
   * if conversion is necessary.</p>
   *
   * <p>If the input parameter's type is wrong, a ClassCastException
   * will be thrown.</p>
   */

  public static Byte[] genIPbytes(Object input)
  {
    if (input instanceof Byte[])
      {
	return (Byte[]) input;
      }
    else if (input instanceof String)
      {
	String ipStr = (String) input;

	if (ipStr.indexOf(':') == -1)
	  {
	    return genIPV4bytes(ipStr);
	  }
	else
	  {
	    return genIPV6bytes(ipStr);
	  }
      }
    else
      {
	throw new ClassCastException(ts.l("genIPbytes.class_error", input.getClass()));
      }
  }

  /**
   * <p>This method wraps an entire Vector of Byte[] arrays so that
   * proper equality testing can be done using the VectorUtils
   * methods.</p>
   */

  public static Vector getWrappedVector(Vector input)
  {
    if (input == null)
      {
	return null;
      }

    Vector results = new Vector(input.size());

    for (int i = 0; i < input.size(); i++)
      {
	try
	  {
	    results.addElement(genIPbytes(input.elementAt(i)));
	  }
	catch (ClassCastException ex)
	  {
	    // "{0}\nCan't wrap elements in getWrappedVector()."
	    throw new IllegalArgumentException(ts.l("getWrappedVector.type_error", ex.getMessage()));
	  }
      }

    return results;
  }

  /**
   * <p>This method unwraps an entire Vector of {@link
   * arlut.csd.ganymede.server.IPwrap} objects and returns a Vector
   * of compact Byte arrays.</p>
   */

  public static Vector getUnwrappedVector(Vector input)
  {
    if (input == null)
      {
	return null;
      }

    Vector results = new Vector(input.size());

    for (int i = 0; i < input.size(); i++)
      {
	try
	  {
	    results.addElement(((IPwrap) input.elementAt(i)).address);
	  }
	catch (ClassCastException ex)
	  {
	    // "{0}\nCan't wrap elements in getUnwrappedVector()."
	    throw new IllegalArgumentException(ts.l("getUnwrappedVector.type_error", ex.getMessage()));
	  }
      }

    return results;
  }

  /**
   *
   * This method takes an IPv4 string in standard format
   * and generates an array of 4 bytes that the Ganymede server
   * can accept.
   *
   */

  public static Byte[] genIPV4bytes(String input)
  {
    Byte[] result = new Byte[4];
    Vector octets = new Vector();
    char[] cAry;
    int length = 0;
    int dotCount = 0;
    StringBuffer temp = new StringBuffer();

    /* -- */

    if (input == null)
      {
	throw new IllegalArgumentException("null input");
      }

    /*
      The string will be of the form 255.255.255.255,
      with each dot separated element being a 8 bit octet
      in decimal form.  Trailing bytes may be excluded, in
      which case the bytes will be left as 0.
      */

    // initialize the result array

    for (int i = 0; i < 4; i++)
      {
	result[i] = new Byte(u2s(0));
      }

    input = input.trim();

    if (input.equals(""))
      {
	return result;
      }

    cAry = input.toCharArray();

    for (int i = 0; i < cAry.length; i++)
      {
	if (!isAllowedV4(cAry[i]))
	  {
	    throw new IllegalArgumentException("bad char in input: " + input);
	  }

	if (cAry[i] == '.')
	  {
	    dotCount++;
	  }
      }

    if (dotCount > 3)
      {
	throw new IllegalArgumentException("too many dots for an IPv4 address: " + input);
      }

    while (length < cAry.length)
      {
	temp.setLength(0);

	while ((length < cAry.length) && (cAry[length] != '.'))
	  {
	    temp.append(cAry[length++]);
	  }

	length++;		// skip the .

	octets.addElement(temp.toString());
      }

    for (int i = 0; i < octets.size(); i++)
      {
	result[i] = new Byte(u2s(Integer.parseInt((String) octets.elementAt(i))));
      }

    return result;
  }

  /**
   *
   * This method generates a standard string representation
   * of an IPv4 address from an array of 4 octets.
   *
   *
   */

  public static String genIPV4string(Byte[] octets)
  {
    StringBuffer result = new StringBuffer();
    Short absoctets[];

    /* -- */

    if (octets.length != 4)
      {
	throw new IllegalArgumentException("bad number of octets.");
      }

    absoctets = new Short[octets.length];

    for (int i = 0; i < octets.length; i++)
      {
	absoctets[i] = new Short((short) (octets[i].shortValue() + 128)); // don't want negative values
      }

    result.append(absoctets[0].toString());
    result.append(".");
    result.append(absoctets[1].toString());
    result.append(".");
    result.append(absoctets[2].toString());
    result.append(".");
    result.append(absoctets[3].toString());
    
    return result.toString();
  }

  /**
   *
   * This method takes an IPv6 string in any of the standard RFC 1884
   * formats or a standard IPv4 string and generates an array of 16
   * bytes that the Ganymede server can accept as an IPv6 address.
   * 
   */

  public static Byte[] genIPV6bytes(String input)
  {
    Byte[] result = new Byte[16];
    Byte[] ipv4bytes = null;
    Vector segments = new Vector();
    char[] cAry;

    int
      length = 0,		// how far into the input have we processed?
      dotCount = 0,		// how many dots for the IPv4 portion?
      colonCount = 0,		// how many colons for the IPv6 portion?
      doublecolon = 0,		// how many double colons?
      tailBytes = 0,		// how many trailing bytes do we have?
      v4v6boundary = 0;		// what is the index of the last char of the v6 portion?

    StringBuffer temp = new StringBuffer();

    /* -- */

    if (input == null)
      {
	throw new IllegalArgumentException("null input");
      }

    /*
      The string may be in one of 3 principle forms.

      The first is that of a standard IPv4 address, in which
      case genIPV6bytes will generate an IPv6 compatible
      address with the assistance of the genIPV4bytes method.

      The second is that of a standard IPv6 address, with 8
      16 bit segments in hex form, :'s between the segments.
      A pair of :'s in immediate succession indicates a range
      of 0 segments have been ommitted.  Since only one
      pair of adjacent :'s may appear in a valid IPv6 address,
      it is possible by looking at the other segments specified
      to determine the extent of the collapsed segments and
      properly insert 0 bytes to fill the collapsed region.

      The third is that of a mixed IPv6/IPv4 address.  In
      this form, the last 4 bytes of the address may be
      specified in IPv4 dotted decimal form.
      */

    // initialize the result array

    for (int i = 0; i < 16; i++)
      {
	result[i] = new Byte(u2s(0));
      }

    // trim the input

    input = input.trim();

    v4v6boundary = input.length();

    if (input.equals("") || input.equals("::"))
      {
	return result;
      }

    cAry = input.toCharArray();

    for (int i = 0; i < cAry.length; i++)
      {
	if (!isAllowedV6(cAry[i]))
	  {
	    throw new IllegalArgumentException("bad char in input: " + input);
	  }

	if (cAry[i] == '.')
	  {
	    dotCount++;
	  }

	if (cAry[i] == ':')
	  {
	    colonCount++;

	    if (i > 0 && (cAry[i-1] == ':'))
	      {
		doublecolon++;
	      }
	  }
      }

    if (dotCount > 3)
      {
	throw new IllegalArgumentException("too many dots for a mixed IPv4/IPv6 address: " + input);
      }

    if (colonCount > 7)
      {
	throw new IllegalArgumentException("too many colons for an IPv6 address: " + input);
      }

    if (doublecolon > 1)
      {
	throw new IllegalArgumentException("error: more than one double-colon.  Invalid IPv6 address: " + input);
      }

    if (dotCount > 0 && colonCount > 6)
      {
	throw new IllegalArgumentException("invalid mixed IPv4/IPv6 address: " + input);
      }

    if ((colonCount == 0) && (dotCount != 0))
      {
	// we've got an IPv4 address where we would like an IPv6 address.  Convert it.

	ipv4bytes = genIPV4bytes(input);

	result[10] = new Byte(u2s(255));
	result[11] = new Byte(u2s(255));
	result[12] = ipv4bytes[0];
	result[13] = ipv4bytes[1];
	result[14] = ipv4bytes[2];
	result[15] = ipv4bytes[3];
	
	return result;
      }

    if (dotCount > 0)
      {
	// we've got a mixed address.. get the v4 bytes from the end.

	v4v6boundary = input.lastIndexOf(':');

	try
	  {
	    ipv4bytes = genIPV4bytes(input.substring(v4v6boundary + 1));
	  }
	catch (Exception ex)
	  {
	    throw new IllegalArgumentException("couldn't do mixed IPv4 parsing: " + ex);
	  }

	result[12] = ipv4bytes[0];
	result[13] = ipv4bytes[1];
	result[14] = ipv4bytes[2];
	result[15] = ipv4bytes[3];

	tailBytes = 4;
      }

    // note that the v4v6boundary will be the length of the input
    // string if we are processing a pure v6 address

    while (length < v4v6boundary)
      {
	temp.setLength(0);

	while ((length < v4v6boundary) && (cAry[length] != ':'))
	  {
	    temp.append(cAry[length++]);
	  }

	length++;		// skip the :

	segments.addElement(temp.toString());
      }

    // okay, we now have a vector of segment strings, with (possibly)
    // an empty string where we had a pair of colons in succession.  Find
    // out if we have a colon pair, and determine the region that it
    // is compressing.

    String tmp;
    boolean beforeRegion = true;
    int compressBoundary = 0;

    for (int i = 0; i < segments.size(); i++)
      {
	tmp = (String) segments.elementAt(i);
	
	if (tmp.equals(""))
	  {
	    beforeRegion = false;

	    compressBoundary = i;
	  }
	else if (!beforeRegion)
	  {
	    tailBytes += 2;
	  }
      }

    // compressBoundary won't be 0 if we had a leading ::,
    // because we would have 2 empties in a row.

    if (compressBoundary == 0)
      {
	compressBoundary = segments.size();
      }
    else if (compressBoundary == 1)
      {
	// if the :: is leading the string, we want to get rid
	// of the extra empty string

	tmp = (String) segments.elementAt(0);

	if (tmp.equals(""))
	  {
	    segments.removeElementAt(0);
	    compressBoundary--;
	  }
      }

    int tailOffset = 16 - tailBytes;

    if (debug)
      {
	System.err.println("tailBytes = " + tailBytes);
	System.err.println("tailOffset = " + tailOffset);
      }

    // 

    for (int i = 0; i < compressBoundary; i++)
      {
	tmp = (String) segments.elementAt(i);

	if (tmp.length() < 4)
	  {
	    int l = tmp.length();

	    for (int j = 4; j > l; j--)
	      {
		tmp = "0" + tmp;
	      }
	  }

	if (tmp.equals(""))
	  {
	    throw new Error("logic error");
	  }

	result[i * 2] = new Byte(u2s(Integer.parseInt(tmp.substring(0, 2), 16)));
	result[(i * 2) + 1] = new Byte(u2s(Integer.parseInt(tmp.substring(2, 4), 16)));

	if (debug)
	  {
	    System.err.println("Byte " + (i*2) + " = " + s2u(result[i*2].byteValue()));
	    System.err.println("Byte " + ((i*2)+1) + " = " + s2u(result[(i*2)+1].byteValue()));
	  }
      }

    int x;

    for (int i = compressBoundary+1; i < segments.size(); i++)
      {
	x = i - compressBoundary - 1;
	tmp = (String) segments.elementAt(i);

	if (tmp.length() < 4)
	  {
	    int l = tmp.length();

	    for (int j = 4; j > l; j--)
	      {
		tmp = "0" + tmp;
	      }
	  }

	if (tmp.equals(""))
	  {
	    throw new Error("logic error");
	  }

	result[tailOffset + (x * 2)] = new Byte(u2s(Integer.parseInt(tmp.substring(0, 2), 16)));
	result[tailOffset + (x * 2) + 1] = new Byte(u2s(Integer.parseInt(tmp.substring(2, 4), 16)));

	if (debug)
	  {
	    System.err.println("Byte " + (tailOffset + (x*2)) + " = " + s2u(result[(tailOffset + (x*2))].byteValue()));
	    System.err.println("Byte " + (tailOffset + ((x*2)+1)) + " = " + s2u(result[tailOffset + (x*2) + 1].byteValue()));
	  }

      }

    return result;
  }

  /**
   *
   * This method takes an array of 4 or 16 bytes and generates an
   * optimal RFC 1884 string encoding suitable for display.
   *
   */

  public static String genIPV6string(Byte[] octets)
  {
    StringBuffer result = new StringBuffer();
    int[] stanzas;
    String[] stanzaStrings;
    int i, j;
    int loCompress, hiCompress;
    Short absoctets[];

    /* -- */

    absoctets = new Short[octets.length];

    for (i = 0; i < octets.length; i++)
      {
	absoctets[i] = new Short((short) (octets[i].shortValue() + 128)); // don't want negative values

	//	System.err.println("Converting byte " + octets[i].intValue() + " to abs " + absoctets[i].intValue());
      }

    if (absoctets.length == 4)
      {
	// okay, here's the easy one..

	result.append("::FFFF:"); // this is IPV6's compatibility mode

	result.append(absoctets[0].toString());
	result.append(".");
	result.append(absoctets[1].toString());
	result.append(".");
	result.append(absoctets[2].toString());
	result.append(".");
	result.append(absoctets[3].toString());

	return result.toString();
      }

    if (absoctets.length != 16)
      {
	throw new IllegalArgumentException("bad number of octets.");
      }

    // now for the challenge..

    stanzas = new int[8];

    for (i = 0; i < 8; i++)
      {
	stanzas[i] = (absoctets[i*2].intValue()*256) + absoctets[(i*2) + 1].intValue();
      }

    stanzaStrings = new String[8];

    // generate hex strings for each 16 bit sequence

    for (i = 0; i < 8; i++)
      {
	stanzaStrings[i] = Integer.toString(stanzas[i], 16); // generate the 4 hex digits

	//	System.err.println("Hex for " + stanzas[i] + " is " + stanzaStrings[i]);
      }
    
    // okay, we've got 8 stanzas.. now we have to determine
    // if we want to collapse any part of it to ::

    loCompress = hiCompress = -1;

    i = 0;

    while (i < 8)
      {
	if (i < 7 && (stanzas[i] == 0) && (stanzas[i+1] == 0))
	  {
	    int localLo, localHi;

	    localLo = i;

	    for (j = i; (j<8) && (stanzas[j] == 0); j++)
	      {
		// just counting up
	      }
	    
	    localHi = j-1;

	    if ((localHi - localLo) > (hiCompress - loCompress))
	      {
		hiCompress = localHi;
		loCompress = localLo;
		
		i = localHi;	// continue our outer loop after this block
	      }
	  }

	i++;
      }

    // System.err.println("loCompress = " + loCompress);
    // System.err.println("hiCompress = " + hiCompress);

    // okay, we've calculated our compression block, if any..
    // let's also check to see if we want to represent the
    // last 4 bytes in dotted decimal IPv4 form

    if ((loCompress == 0) && (hiCompress == 5))
      {
	return "::" + absoctets[12].toString() + "." + absoctets[13].toString() + "." +
	  absoctets[14].toString() + "." + absoctets[15].toString();
      }
    else if ((loCompress == 0) && (hiCompress == 4) && 
	     (absoctets[10].shortValue() == 255) && (absoctets[11].shortValue() == 255))
      {
	return "::FFFF:" + absoctets[12].toString() + "." + absoctets[13].toString() + "." +
	  absoctets[14].toString() + "." + absoctets[15].toString();
      }

    // nope, we're gonna go all the way in IPv6 form..

    if (loCompress != hiCompress)
      {
	// we've got a compressed area

	i = 0;

	while (i < loCompress)
	  {
	    if (i > 0)
	      {
		result.append(":");
	      }
	    
	    result.append(stanzaStrings[i++]);
	  }

	result.append("::");
	
	j = hiCompress + 1;

	while (j < 8)
	  {
	    if (j > (hiCompress+1))
	      {
		result.append(":");
	      }
	    
	    result.append(stanzaStrings[j++]);
	  }
      }
    else
      {
	// no compressed area

	for (i = 0; i < 8; i++)
	  {
	    if (i > 0)
	      {
		result.append(":");
	      }
	    
	    result.append(stanzaStrings[i]);
	  }
      }
    
    return result.toString().toUpperCase();
  }

  /**
   *
   * Test rig
   *
   */

  public static void main(String argv[])
  {
    Byte[] octets;
    Random rand = new Random();

    /* -- */

    octets = new Byte[16];

    for (int i = 0; i < 16; i++)
      {
	octets[i] = new Byte((byte) -128);
      }

    System.out.println("All zero v6 string: " + genIPV6string(octets));

    octets[15] = new Byte((byte) -127);

    System.out.println("Trailing 1 string: " + genIPV6string(octets));

    byte[] randbytes = new byte[16];

    rand.nextBytes(randbytes);

    for (int i = 4; i < 16; i++)
      {
	octets[i] = new Byte(randbytes[i]);
      }

    System.out.println("4 Leading zero rand string: " + genIPV6string(octets));

    for (int i = 0; i < 16; i++)
      {
	octets[i] = new Byte((byte) -128);
      }

    rand.nextBytes(randbytes);

    for (int i = 0; i < 8; i++)
      {
	if (rand.nextInt() > 0)
	  {
	    octets[i*2] = new Byte(randbytes[i]);
	    octets[(i*2)+1] = new Byte(randbytes[i]);

	    System.out.print("**");
	  }
	else
	  {
	    System.out.print("00");
	  }
      }

    System.out.println();

    System.out.println("Random compression block string: " + genIPV6string(octets));

    for (int i = 0; i < 12; i++)
      {
	octets[i] = new Byte((byte) -128);
      }

    rand.nextBytes(randbytes);

    for (int i = 12; i < 16; i++)
      {
	octets[i] = new Byte(randbytes[i]);
      }

    System.out.println("IPv4 compatible string (A): " + genIPV6string(octets));

    for (int i = 0; i < 10; i++)
      {
	octets[i] = new Byte((byte) -128);
      }

    octets[10] = new Byte((byte) 127);
    octets[11] = new Byte((byte) 127);

    rand.nextBytes(randbytes);

    for (int i = 12; i < 16; i++)
      {
	octets[i] = new Byte(randbytes[i]);
      }

    System.out.println("IPv4 compatible string (B): " + genIPV6string(octets));
  }
}
