/*
   GASH 2

   IPDBField.java

   The GANYMEDE object storage system.

   Created: 4 Sep 1997
   Version: $Revision: 1.3 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import java.io.*;
import java.util.*;
import java.rmi.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                       IPDBField

------------------------------------------------------------------------------*/

public class IPDBField extends DBField implements ip_field {

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
	    Short[] posElements = new Short[element.length];

	    for (int i = 0; i < posElements.length; i++)
	      {
		posElements[i] = new Short((short) (element[i].shortValue() + 128));
	      }

	    return (posElements[0].toString() + "." + posElements[1].toString() + "." + 
		    posElements[2].toString() + "." + posElements[3].toString());
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
	    Short[] posElements = new Short[element.length];

	    for (int j = 0; j < posElements.length; j++)
	      {
		posElements[j] = new Short((short) (element[j].shortValue() + 128));
	      }

	    result = result + (posElements[0].toString() + "." + posElements[1].toString() + "." + 
			       posElements[2].toString() + "." + posElements[3].toString());
	  }
      }

    return result;
  }

  // ****
  //
  // ip_field methods
  //
  // ****

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

    if (!isEditable())
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

	result.append("::ffff:"); // this is IPV6's compatibility mode

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
	return "::ffff:" + absoctets[12].toString() + "." + absoctets[13].toString() + "." +
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
    
    return result.toString();
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
