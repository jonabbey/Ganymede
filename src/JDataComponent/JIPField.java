/*

   JIPField.java

   An IPv4/IPv6 data display / entry widget for Ganymede
   
   Created: 13 October 1997
   Version: $Revision: 1.10 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.JDataComponent;

import java.util.*;
import java.awt.*;
import java.awt.event.*;

import java.rmi.*;

import com.sun.java.swing.*;
import com.sun.java.swing.text.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                        JIPField

------------------------------------------------------------------------------*/

/**
 *
 * This class is an IPv4/IPv6 data display/entry widget for Ganymede.  Its purpose
 * is to allow the viewing and editing of either 4 or 16 byte Internet addresses
 * and subnet masks.<br><br>
 *
 * Note that wherever Ganymede manipulates IP addresses, it does so in terms
 * of unsigned bytes.  Since Java does not provide an unsigned byte type, Ganymede
 * uses the s2u() and u2s() static methods defined in this class to convert from
 * the signed Java byte to the Ganymede 0-255 IP octet range.
 *
 */

public class JIPField extends JentryField {

  public static final boolean debug = false;

  public static int DEFAULT_COLS = 20;

  private static String IPv4allowedChars = "1234567890.";
  private static String IPv6allowedChars = "1234567890.ABCDEF:";

  // --

  String value;
  boolean allowV6;

  /**  Constructors ***/

 /**
   * Base constructor for JIPField
   * 
   * @param columns number of colums in the JIPField
   * @param valueAttr used to determine the foregoudn/background/font for this JIPField
   * @param is_editable true if this JIPField is editable
   */

  public JIPField(JcomponentAttr valueAttr,
		  boolean is_editable,
		  boolean allowV6)
  {
    super(DEFAULT_COLS);
    
    this.valueAttr = valueAttr;
    this.allowV6 = allowV6;
    
    //    setText(null);

    setEditable(is_editable);  // will this JIPField be editable or not?

    setEnabled(true);

    if (valueAttr != null)
      {
	JcomponentAttr.setAttr(this,valueAttr);
      }

    enableEvents(AWTEvent.KEY_EVENT_MASK); 
  }
  
  /**
   * Constructor which uses default fonts,no parent,
   * default column size, and default foregound/background
   * colors.
   */

  public JIPField(boolean allowV6)
  {
    this(new JcomponentAttr(null,new Font("Courier",Font.PLAIN,12),
			    Color.black,Color.white),
	 true,
	 allowV6);
  }

  /**
    * Constructor that allows for the creation of a JIPField
    * that knows about its parent.
    *
    * @param cols number of colums in the JIPField
    * @param valueAttr used to determine the foregoudn/background/font for this JIPField
    * @param parent the container within which this JIPField is contained
    *        (This container will implement an interface that will utilize the
    *         data contained within this JIPField.)
    *
    */

  public JIPField(JcomponentAttr valueAttr,
		  boolean is_editable,
		  JsetValueCallback callback,
		  boolean allowV6)
  {
    this(valueAttr,is_editable, allowV6);

    setCallback(callback);
  }
  
 /************************************************************/
 // JIPField methods

  /**
   *  returns the character located at position n in the JIPField value
   *
   * @param n position in the JIPField value from which to retrieve character
   */

  public char getCharAt(int n)
  {
    return this.getText().charAt(n);
  }

  /**
   *  returns the character located at position n in the JIPField value
   *
   * @param n position in the JIPField value from which to retrieve character
   */

  public void setValue(Byte[] bytes)
  {
    if (bytes == null)
      {
	setText("");
	return;
      }
    
    if (bytes.length == 4)
      {
	setText(genIPV4string(bytes));
      }
    else
      {
	setText(genIPV6string(bytes));
      }

    return;
  }

  public Byte[] getValue()
  {
    String str;

    /* -- */

    str = getText();

    if (str.indexOf(':') != -1)
      {
	if (allowV6)
	  {
	    return genIPV6bytes(str);
	  }
	else
	  {
	    throw new IllegalArgumentException("IPv6 Addresses not allowed in this field");
	  }
      }
    else
      {
	return genIPV4bytes(str);
      }
  }

  /**
   * When the JIPField looses focus, any changes made to 
   * the value in the JIPField need to be propogated to the
   * server.  This method will handle that functionality.
   *
   */

  public void sendCallback()
  {
    String str;
    Byte[] bytes;

    /* -- */

    // if nothing in the JIPField has changed,
    // we don't need to worry about this event.
    
    str = getText();
    
    if (value == null || !value.equals(str))
      {
	try
	  {
	    if (str.indexOf(':') != -1)
	      {
		if (allowV6)
		  {
		    bytes = genIPV6bytes(str);
		  }
		else
		  {
		    throw new IllegalArgumentException("IPv6 Addresses not allowed in this field");
		  }
	      }
	    else
	      {
		bytes = genIPV4bytes(str);
	      }
	  }
	catch (IllegalArgumentException ex)
	  {
	    reportError(ex.getMessage());
	    return;
	  }
      }
    else
      {
	if (debug)
	  {
	    System.err.println("JIPField.processFocusEvent: no change, ignoring");
	  }
	
	return;
      }
    
    if (allowCallback) 
      {
	boolean b = false;
	
	try 
	  {
	    if (debug)
	      {
		System.err.println("JIPField.processFocusEvent: making callback");
	      }
	    
	    b = my_parent.setValuePerformed(new JValueObject(this, bytes));
	  }
	catch (RemoteException re)
	  {
	    throw new RuntimeException("failure in callback dispatch: " + re); 
	  }
	
	if (!b) 
	  {
	    if (debug)
	      {
		System.err.println("JIPField.processFocusEvent: setValue rejected");
		
		if (value == null)
		  {
		    System.err.println("JIPField.processFocusEvent: resetting to empty string");
		  }
		else
		  {
		    System.err.println("JIPField.processFocusEvent: resetting to " + value);
		  }
	      }
	    
	    if (value == null)
	      {
		setText("");
	      }
	    else
	      {
		setText(value);
	      }
	    
	    changed = false;
	  }
	else 
	  {
	    if (debug)
	      {
		System.err.println("JIPField.processFocusEvent: setValue accepted");
	      }
	    
	    if (bytes == null)
	      {
		value = "";
		setText(value);
	      }
	    else if (bytes.length == 4)
	      {
		value = genIPV4string(bytes);
		setText(value);
	      }
	    else if (bytes.length == 16)
	      {
		value = genIPV6string(bytes);
		setText(value);
	      }
	    else
	      {
		throw new RuntimeException("JIPField: bad bytes calculated");
	      }
	    
	    changed = true;
	  }
      }
  }

  /**
   *
   * This private method is used to report an error condition to the user.
   *
   */

  private void reportError(String error)
  {
    if (allowCallback) 
      {
	try 
	  {
	    if (debug)
	      {
		System.err.println("JIPField.processFocusEvent: making callback");
	      }
	    
	    my_parent.setValuePerformed(new JValueObject(this, error, JValueObject.ERROR));
	  }
	catch (RemoteException ex)
	  {
	    throw new RuntimeException("failure in error report: " + ex); 
	  }
      }
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

    // initialize the result array so we'll return 0.0.0.0
    // if nothing was entered

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
	    throw new IllegalArgumentException("bad char for IPv4 address in input: " + input);
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
	if (debug)
	  {
	    System.err.println("JIPField.genIPV4bytes(): byte " + i + " = " + 
			       (String) octets.elementAt(i));
	  }

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

	if (i > 0)
	  {
	    result.append(".");
	  }

	result.append(absoctets[i].toString());
      }
    
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
	    throw new IllegalArgumentException("bad char for IPv6 address in input: " + input);
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
    
    return result.toString().toUpperCase();
  }

  public static void main(String argv[])
  {
    if (argv.length != 1)
      {
	System.out.println("No input.\nUsage: java arlut.csd.JDataComponent.JIPField");
	System.exit(0);
      }

    Byte[] results = genIPV6bytes(argv[0]);
    short s;

    if (debug)
      {
	for (int i = 0; i < results.length; i++)
	  {
	    s = s2u(results[i].byteValue());
	    
	    if (i > 0 && i%2 == 0)
	      {
		System.out.print(":");
	      }
	    
	    System.out.print(Integer.toHexString(s));
	  }
	
	System.out.println();
      }

    System.out.println(genIPV6string(results));

    System.exit(0);
  }
}
