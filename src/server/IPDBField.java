/*
   GASH 2

   IPDBField.java

   The GANYMEDE object storage system.

   Created: 4 Sep 1997
   Version: $Revision: 1.13 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import java.io.*;
import java.util.*;
import java.rmi.*;

import arlut.csd.JDialog.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                       IPDBField

------------------------------------------------------------------------------*/

public class IPDBField extends DBField implements ip_field {

  static final boolean debug = false;
  private static String IPv4allowedChars = "1234567890.";
  private static String IPv6allowedChars = "1234567890.ABCDEF:";

  // --

  /**
   *
   * Receive constructor.  Used to create a BooleanDBField from a DBStore/DBJournal
   * DataInput stream.
   *
   */

  IPDBField(DBObject owner, DataInput in, DBObjectBaseField definition) throws IOException, RemoteException
  {
    value = values = null;
    this.owner = owner;
    this.definition = definition;
    receive(in);
  }

  /**
   *
   * No-value constructor.  Allows the construction of a
   * 'non-initialized' field, for use where the DBObjectBase
   * definition indicates that a given field may be present,
   * but for which no value has been stored in the DBStore.
   *
   * Used to provide the client a template for 'creating' this
   * field if so desired.
   *
   */

  IPDBField(DBObject owner, DBObjectBaseField definition) throws RemoteException
  {
    this.owner = owner;
    this.definition = definition;
    
    defined = false;
    value = null;

    if (isVector())
      {
	values = new Vector();
      }
    else
      {
	values = null;
      }
  }

  /**
   *
   * Copy constructor.
   *
   */

  public IPDBField(DBObject owner, IPDBField field) throws RemoteException
  {
    this.owner = owner;
    definition = field.definition;
    
    if (isVector())
      {
	values = (Vector) field.values.clone();
	value = null;
      }
    else
      {
	value = field.value;
	values = null;
      }

    defined = true;
  }

  /**
   *
   * Scalar value constructor.
   *
   */

  public IPDBField(DBObject owner, Date value, DBObjectBaseField definition) throws RemoteException
  {
    if (definition.isArray())
      {
	throw new IllegalArgumentException("scalar constructor called on vector field");
      }

    this.owner = owner;
    this.definition = definition;
    this.value = value;

    if (value != null)
      {
	defined = true;
      }
    else
      {
	defined = false;
      }

    values = null;
  }

  /**
   *
   * Vector value constructor.
   *
   */

  public IPDBField(DBObject owner, Vector values, DBObjectBaseField definition) throws RemoteException
  {
    if (!definition.isArray())
      {
	throw new IllegalArgumentException("vector constructor called on scalar field");
      }

    this.owner = owner;
    this.definition = definition;

    if (values == null)
      {
	this.values = new Vector();
	defined = false;
      }
    else
      {
	this.values = (Vector) values.clone();
	defined = true;
      }

    defined = true;
    value = null;
  }

  public Object clone()
  {
    try
      {
	return new IPDBField(owner, this);
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("couldn't clone date field: " + ex);
      }
  }

  void emit(DataOutput out) throws IOException
  {
    if (isVector())
      {
	out.writeShort(values.size());

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

  void receive(DataInput in) throws IOException
  {
    int count;

    /* -- */

    if (isVector())
      {
	count = in.readShort();
	values = new Vector(count);
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

    defined = true;
  }


  // ****
  //
  // type-specific accessor methods
  //
  // ****

  /**
   *
   * Sets the value of this field, if a scalar.
   *
   * The ReturnVal object returned encodes
   * success or failure, and may optionally
   * pass back a dialog.
   *
   */

  public ReturnVal setValue(Object value, boolean local)
  {
    DBNameSpace ns;
    DBEditObject eObj;
    Byte[] bytes;

    /* -- */

    if (!isEditable(local))
      {
	throw new IllegalArgumentException("don't have permission to change field /  non-editable object");
      }

    if (isVector())
      {
	throw new IllegalArgumentException("scalar method called on a vector field");
      }

    if (value == null)
      {
	bytes = null;
      }
    else
      {
	if (value instanceof String)
	  {
	    String input = (String) value;

	    if (input.indexOf(':') == -1)
	      {
		bytes = genIPV4bytes(input);
	      }
	    else
	      {
		bytes = genIPV6bytes(input);
	      }
	  }
	else if (!(value instanceof Byte[]))
	  {
	    throw new IllegalArgumentException("invalid type argument");
	  }
	else
	  {
	    bytes = (Byte[]) value;
	  }
      }

    if (!verifyNewValue(bytes))
      {
	return Ganymede.createErrorDialog("Server: Error in IPDBField.setValue()",
					  "Invalid ip value\n" + getLastError());
      }

    eObj = (DBEditObject) owner;

    // check to see if we can do the namespace manipulations implied by this
    // operation

    ns = getNameSpace();

    if (ns != null)
      {
	unmark(this.value);	// our old value

	if (!mark(bytes))
	  {
	    if (this.value != null)
	      {
		mark(this.value); // we aren't clearing the old value after all
	      }

	    setLastError("value " + bytes + " already taken in namespace");

	    return Ganymede.createErrorDialog("Server: Error in IPDBField.setValue()",
					      "IP address already in use\n" + getLastError());
	  }
      }

    // check our owner, do it.  Checking our owner should
    // be the last thing we do.. if it returns true, nothing
    // should stop us from running the change to completion

    this.newValue = bytes;

    if (eObj.finalizeSetValue(this, bytes))
      {
	if (bytes != null)
	  {
	    this.value = bytes;
	    defined = true;
	  }
	else
	  {
	    this.value = null;
	    defined = false;	// the key
	  }

	this.newValue = null;

	return null;
      }
    else
      {
	// our owner disapproved of the operation,
	// undo the namespace manipulations, if any,
	// and finish up.

	this.newValue = null;

	if (ns != null)
	  {
	    unmark(bytes);
	    mark(this.value);
	  }

	return Ganymede.createErrorDialog("Server: Error in IPDBField.setValue()",
					  "Could not finalize IP address\n" + getLastError());
      }
  }

  /**
   *
   * Sets the value of an element of this field, if a vector.
   *
   * The ReturnVal object returned encodes
   * success or failure, and may optionally
   * pass back a dialog.
   *
   */
  
  public ReturnVal setElement(int index, Object value, boolean local)
  {
    DBNameSpace ns;
    DBEditObject eObj;
    Byte[] bytes;

    /* -- */

    if (!isEditable(local))
      {
	throw new IllegalArgumentException("don't have permission to change field /  non-editable object");
      }

    if (!isVector())
      {
	throw new IllegalArgumentException("vector accessor called on scalar field");
      }

    if ((index < 0) || (index > values.size()))
      {
	throw new IllegalArgumentException("invalid index " + index);
      }

    if (value instanceof String)
      {
	String input = (String) value;

	if (input.indexOf(':') == -1)
	  {
	    bytes = genIPV4bytes(input);
	  }
	else
	  {
	    bytes = genIPV6bytes(input);
	  }
      }
    else if (!(value instanceof Byte[]))
      {
	throw new IllegalArgumentException("invalid type argument");
      }
    else
      {
	bytes = (Byte[]) value;
      }

    if (!verifyNewValue(bytes))
      {
	return Ganymede.createErrorDialog("Server: Error in IPDBField.setElement()",
					  "Improper IP address\n" + getLastError());
      }

    eObj = (DBEditObject) owner;

    // check to see if we can do the namespace manipulations implied by this
    // operation

    ns = getNameSpace();

    if (ns != null)
      {
	unmark(values.elementAt(index));

	if (!mark(bytes))
	  {
	    mark(values.elementAt(index)); // we aren't clearing the old value after all

	    setLastError("value " + value + " already taken in namespace");

	    return Ganymede.createErrorDialog("Server: Error in IPDBField.setElement()",
					      "IP address already in use\n" + getLastError());
	  }
      }

    // check our owner, do it.  Checking our owner should
    // be the last thing we do.. if it returns true, nothing
    // should stop us from running the change to completion

    if (eObj.finalizeSetElement(this, index, bytes))
      {
	values.setElementAt(bytes, index);

	defined = true;
	
	return null;
      }
    else
      {
	// our owner disapproved of the operation,
	// undo the namespace manipulations, if any,
	// and finish up.

	if (ns != null)
	  {
	    unmark(bytes);
	    mark(values.elementAt(index));
	  }

	return Ganymede.createErrorDialog("Server: Error in IPDBField.setElement()",
					  "Could not finalize IP address\n" + getLastError());
      }
  }

  /**
   *
   * Adds an element to the end of this field, if a vector.
   *
   * The ReturnVal object returned encodes
   * success or failure, and may optionally
   * pass back a dialog.
   *
   */

  public ReturnVal addElement(Object value, boolean local)
  {
    DBNameSpace ns;
    DBEditObject eObj;
    Byte[] bytes;

    /* -- */

    if (!isEditable(local))
      {
	throw new IllegalArgumentException("don't have permission to change field /  non-editable object");
      }

    if (!isVector())
      {
	throw new IllegalArgumentException("vector accessor called on scalar field");
      }

    if (value instanceof String)
      {
	String input = (String) value;

	if (input.indexOf(':') == -1)
	  {
	    bytes = genIPV4bytes(input);
	  }
	else
	  {
	    bytes = genIPV6bytes(input);
	  }
      }
    else if (!(value instanceof Byte[]))
      {
	throw new IllegalArgumentException("invalid type argument");
      }
    else
      {
	bytes = (Byte[]) value;
      }

    // verifyNewValue should setLastError for us.

    if (!verifyNewValue(bytes))
      {
	return Ganymede.createErrorDialog("Server: Error in IPDBField.addElement()",
					  "Improper IP address\n" + getLastError());
      }

    if (size() >= getMaxArraySize())
      {
	setLastError("Field " + getName() + " already at or beyond array size limit");

	return Ganymede.createErrorDialog("Server: Error in IPDBField.addElement()",
					  "Field " + getName() + " already at or beyond array size limit");
      }

    eObj = (DBEditObject) owner;

    ns = getNameSpace();

    if (ns != null)
      {
	if (!mark(bytes))
	  {
	    setLastError("value " + value + " already taken in namespace");

	    return Ganymede.createErrorDialog("Server: Error in IPDBField.addElement()",
					     "IP address already in use\n" + getLastError());
	  }
      }

    if (eObj.finalizeAddElement(this, bytes)) 
      {
	values.addElement(bytes);
	defined = true;
	return null;
      } 
    else
      {
	if (ns != null)
	  {
	    unmark(bytes);
	  }

	return Ganymede.createErrorDialog("Server: Error in IPDBField.addElement()",
					  "Could not finalize IP address\n" + getLastError());
      }
  }

  public Byte[] value()
  {
    if (isVector())
      {
	throw new IllegalArgumentException("scalar accessor called on vector");
      }

    return (Byte[]) value;
  }

  public Byte[] value(int index)
  {
    if (!isVector())
      {
	throw new IllegalArgumentException("vector accessor called on scalar");
      }

    return (Byte[]) values.elementAt(index);
  }

  public synchronized String getValueString()
  {
    String result = "";

    /* -- */

    if (!verifyReadPermission())
      {
	throw new IllegalArgumentException("permission denied to read this field");
      }

    if (!isVector())
      {
	if (value == null)
	  {
	    return "null";
	  }

	if (isIPV6())
	  {
	    Byte[] element = (Byte[]) value;
	    
	    return genIPV6string(element);
	  }
	else
	  {
	    Byte[] element = (Byte[]) value;
	    
	    return genIPV4string(element);
	  }
      }

    int size = size();

    for (int i = 0; i < size; i++)
      {
	Byte[] element = (Byte[]) values.elementAt(i);

	if (!result.equals(""))
	  {
	    result = result + ", ";
	  }

	if (isIPV6())
	  {
	    result = result + genIPV6string(element);
	  }
	else
	  {
	    result = result + genIPV4string(element);
	  }
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
   *
   * Returns a String representing the change in value between this
   * field and orig.  This String is intended for logging and email,
   * not for any sort of programmatic activity.  The format of the
   * generated string is not defined, but is intended to be suitable
   * for inclusion in a log entry and in an email message.
   *
   * If there is no change in the field, null will be returned.
   * 
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
	Vector 
	  added = new Vector(),
	  deleted = new Vector();

	Enumeration enum;

	Byte[] elementA, elementB;

	boolean found = false;

	/* -- */

	// find elements in the orig field that aren't in our present field

	enum = origI.values.elements();

	while (enum.hasMoreElements())
	  {
	    elementA = (Byte[]) enum.nextElement();

	    found = false;

	    for (int i = 0; !found && i < values.size(); i++)
	      {
		elementB = (Byte[]) values.elementAt(i);

		if (elementA.length == elementB.length)
		  {
		    found = true;

		    for (int j = 0; j < elementA.length; j++)
		      {
			if (elementA[j] != elementB[j])
			  {
			    found = false;
			  }
		      }
		  }
	      }

	    if (!found)
	      {
		deleted.addElement(elementA);
	      }
	  }

	// find elements in present our field that aren't in the orig field

	enum = values.elements();

	while (enum.hasMoreElements())
	  {
	    elementA = (Byte[]) enum.nextElement();

	    found = false;

	    for (int i = 0; !found && i < origI.values.size(); i++)
	      {
		elementB = (Byte[]) origI.values.elementAt(i);

		if (elementA.length == elementB.length)
		  {
		    found = true;

		    for (int j = 0; j < elementA.length; j++)
		      {
			if (elementA[j] != elementB[j])
			  {
			    found = false;
			  }
		      }
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
	    Byte[] x;

	    if (deleted.size() != 0)
	      {
		result.append("\tDeleted: ");
	    
		for (int i = 0; i < deleted.size(); i++)
		  {
		    if (i > 0)
		      {
			result.append(", ");
		      }
		    
		    x = (Byte[]) deleted.elementAt(i);

		    if (x.length == 4)
		      {
			result.append(genIPV4string(x));
		      }
		    else 
		      {
			result.append(genIPV6string(x));
		      }
		  }

		result.append("\n");
	      }

	    if (added.size() != 0)
	      {
		result.append("\tAdded: ");
	    
		for (int i = 0; i < added.size(); i++)
		  {
		    if (i > 0)
		      {
			result.append(", ");
		      }

		    x = (Byte[]) deleted.elementAt(i);

		    if (x.length == 4)
		      {
			result.append(genIPV4string(x));
		      }
		    else 
		      {
			result.append(genIPV6string(x));
		      }
		  }

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
	
	if (x.length == 4)
	  {
	    result.append(genIPV4string(x));
	  }
	else 
	  {
	    result.append(genIPV6string(x));
	  }

	result.append("\n\tNew: ");

	if (y.length == 4)
	  {
	    result.append(genIPV4string(x));
	  }
	else 
	  {
	    result.append(genIPV6string(x));
	  }
	
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
   * Returns true if the value stored in this IP field is an IPV4 address
   *
   * @see arlut.csd.ganymede.ip_field
   *
   */

  public boolean isIPV4()
  {
    if (isVector())
      {
	throw new IllegalArgumentException("scalar accessor called on vector field");
      }

    Byte[] element = (Byte[]) value;

    return (element.length == 4);
  }

  /**
   *
   * Returns true if the value stored in the given element of this IP
   * field is an IPV4 address.
   *
   * @param index Array index for the value to be checked
   *
   * @see arlut.csd.ganymede.ip_field
   * 
   */

  public boolean isIPV4(short index)
  {
    if (!isVector())
      {
	throw new IllegalArgumentException("vector accessor called on scalar field");
      }

    Byte[] element = (Byte[]) values.elementAt(index);

    return (element.length == 4);
  }

  /**
   *
   * Returns true if the value stored in this IP field is an IPV6 address
   *
   * @see arlut.csd.ganymede.ip_field
   *
   */

  public boolean isIPV6()
  {
    if (isVector())
      {
	throw new IllegalArgumentException("scalar accessor called on vector field");
      }

    Byte[] element = (Byte[]) value;

    return (element.length == 16);
  }

  /**
   *
   * Returns true if the value stored in the given element of this IP
   * field is an IPV6 address.
   *
   * @param index Array index for the value to be checked
   *
   * @see arlut.csd.ganymede.ip_field
   * 
   */

  public boolean isIPV6(short index)
  {
    if (!isVector())
      {
	throw new IllegalArgumentException("vector accessor called on scalar field");
      }

    Byte[] element = (Byte[]) values.elementAt(index);

    return (element.length == 16);
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

  public boolean verifyNewValue(Object o)
  {
    DBEditObject eObj;
    Date d, d2;
    Vector v;
    boolean ok = true;

    /* -- */

    if (!isEditable(true))
      {
	return false;
      }

    eObj = (DBEditObject) owner;

    if (!verifyTypeMatch(o))
      {
	setLastError("type mismatch");
	return false;
      }

    // have our parent make the final ok on the value

    return eObj.verifyNewValue(this, o);
  }

  // 
  // helper methods for the encoding/decoding methods
  // to follow.
  //

  private final static byte u2s(int x)
  {
    if ((x < 0) || (x > 255))
      {
	throw new IllegalArgumentException("Out of range: " + x);
      }

    return (byte) (x - 128);
  }

  private final static short s2u(byte b)
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
    char[] charAry;
    StringBuffer temp;
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
