/*

   JDialogBuff.java

   Serializable resource class for use with StringDialog.java
   
   Created: 27 January 1998
   Version: $Revision: 1.14 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.JDialog;

import java.util.*;
import java.awt.Frame;

/*------------------------------------------------------------------------------
                                                                           class
                                                                      JDialogBuff

------------------------------------------------------------------------------*/

/**
 *
 * This class is a serializable description of a dialog object that a server
 * is asking a client to present.  The client retrieves an object of type
 * JDialogBuff from the server via RMI, then calls extractDialogRsrc()
 * in order to get a DialogRsrc object.  This object can then be used on
 * the client to construct an dialog box.
 *
 * If you don't need to send a dialog definition object across an RMI
 * link, just construct a DialogRsrc directly.
 *
 */

public class JDialogBuff implements java.io.Serializable {

  static final boolean debug = false;

  // ---
  
  StringBuffer buffer;		// serialized dialog resource description

  /* -- */

  // client side code

  public DialogRsrc extractDialogRsrc(Frame frame)
  { 
    if (debug)
      {
	System.err.println("JDialogBuff: entering extractDialogRsrc()");
      }

    Vector chunks = retrieveChunks();

    String title;
    String text;
    String okText;
    String cancelText;
    String imageName;

    DialogRsrc retVal;

    JDialogBuffChunk chunk;

    /* -- */


    if (chunks == null || chunks.size() < 5)
      {
	throw new RuntimeException("error, can't extract dialog resource.. mandatory fields missing");
      }

    chunk = (JDialogBuffChunk) chunks.elementAt(0);

    title = (String) chunk.value;

    chunk = (JDialogBuffChunk) chunks.elementAt(1);

    text = (String) chunk.value;

    chunk = (JDialogBuffChunk) chunks.elementAt(2);

    okText = (String) chunk.value;

    chunk = (JDialogBuffChunk) chunks.elementAt(3);

    cancelText = (String) chunk.value;

    chunk = (JDialogBuffChunk) chunks.elementAt(4);

    imageName = (String) chunk.value;

    retVal = new DialogRsrc(frame, title, text, okText, cancelText, imageName);

    if (chunks.size() == 5)
      {
	return retVal;
      }

    // now, we've got some parameters to pass in

    int index = 5;

    while (index < chunks.size())
      {
	if (debug)
	  {
	    System.err.println("JDialogBuff: retrieving chunk " + index);
	  }

	chunk = (JDialogBuffChunk) chunks.elementAt(index);

	// System.err.println("Retrieving extra chunk: " + chunk);

	if (chunk.label.equals("@string"))
	  {
	    if (debug)
	      {
		System.err.println("JDialogBuff: retrieving string chunk");
	      }

	    retVal.addString((String) chunk.value, (String)chunk.initialValue);
	  }
	else if (chunk.label.equals("@boolean"))
	  {
	    if (debug)
	      {
		System.err.println("JDialogBuff: retrieving boolean chunk");
	      }

	    retVal.addBoolean((String) chunk.value, new Boolean((String)chunk.initialValue).booleanValue());
	  }
	else if (chunk.label.equals("@separator"))
	  {
	    if (debug)
	      {
		System.err.println("JDialogBuff: retrieving separator chunk");
	      }

	    retVal.addSeparator();
	  }
	else if (chunk.label.equals("@pass"))
	  {
	    if (debug)
	      {
		System.err.println("JDialogBuff: retrieving password chunk:  " + chunk.value + " new: " + chunk.initialValue);
	      }

	    retVal.addPassword((String) chunk.value, new Boolean((String)chunk.initialValue).booleanValue()); // initialValue in this case is a boolean
	  }
	else if (chunk.label.startsWith("@choice>"))
	  {
	    if (debug)
	      {
		System.err.println("JDialogBuff: retrieving choice chunk");
	      }

	    String choiceLabel = chunk.label.substring(8); // after @choice>

	    if (debug)
	      {
		System.err.println("JDialogBuff: choice label = " + choiceLabel +
				   " value = " + chunk.value + " init = " + chunk.initialValue);
	      }

	    retVal.addChoice(choiceLabel, (Vector) chunk.value, (String)chunk.initialValue);

	    if (debug)
	      {
		System.err.println("JDialogBuff: added choice values:  " + chunk.value);
	      }
	  }
	else 
	  {
	    throw new RuntimeException("unrecognized chunk" + chunk.label);
	  }

	index++;
      }

    // to speed GC

    chunks = null;

    return retVal;
  }

  // server-side constructors

  /**
   * Constructor for JDialogBuff
   *
   * @param Title String for title of Dialog box.
   * @param Text String for message at top of dialog box.
   *
   */

  public JDialogBuff(String Title, String Text)
  {
    this(Title, Text, "Ok", "Cancel", null);
  }

  /** 
   * Constructor with special "Ok" and "Cancel" strings
   *
   * @param Title String for title of Dialog box.
   * @param Text String for message at top of dialog box.
   * @param OK String for Ok button 
   * @param Cancel String for Cancel button
   */

  public JDialogBuff(String Title, String Text, String OK, String Cancel)
  {
    this(Title, Text, OK, Cancel, null);
  }

  /** 
   * Constructor with special "Ok" and "Cancel" strings
   *
   * @param Title String for title of Dialog box.
   * @param Text String for message at top of dialog box.
   * @param OK String for Ok button 
   * @param Cancel String for Cancel button
   * @param image Filename of image to display next to text
   */

  public JDialogBuff(String Title, String Text, String OK, String Cancel, String image)
  {

    buffer = new StringBuffer();

    addChunk("@1", Title, null);
    addChunk("@2", Text, null);
    addChunk("@3", OK, null);
    addChunk("@4", Cancel, null);
    addChunk("@5", image, null);
  }

  /**
   *
   * Adds a labeled text field
   *
   * @param string String to use as the label
   */

  public void addString(String string)
  {
    addString(string, (String)null);
  }

  /**
   *
   * Adds a labeled text field
   *
   * @param string String to use as the label
   * @param value Initial value for string
   */

  public void addString(String string, String value)
  {
    addChunk("@string", string, value);
  }

  /**
   * 
   * Adds a labeled check box field
   *
   * @param string String to use as the label
   */
  
  public void addBoolean(String string)
  {
    addBoolean(string, new Boolean(false));
  }

  /**
   * 
   * Adds a labeled check box field
   *
   * @param string String to use as the label
   * @param value Initial value
   */
  
  public void addBoolean(String string, Boolean value)
  {
    addChunk("@boolean", string, value.toString());
  }

  /**
   *
   * Adds a choice field to the dialog
   *
   * @param label String to use as the label
   * @param choices Vector of Strings to add to the choice 
   */
  
  public void addChoice(String label, Vector choices)
  {
    addChoice(label, choices, null);
  }

  /**
   *
   * Adds a choice field to the dialog
   *
   * @param label String to use as the label
   * @param choices Vector of Strings to add to the choice 
   */
  
  public void addChoice(String label, Vector choices, String selectedValue)
  {
    addChunk("@choice>" + label, choices, selectedValue);
  }

  /**
   *
   * Adds a one line separator to the dialog
   *
   */

  public void addSeparator()
  {
    addChunk("@separator", (String) null, (String) null);
  }

  /**
   *
   * Adds a text-hidden password string field to the dialog
   *
   * @param label String to use as label
   */

  public void addPassword(String label)
  {
    addPassword(label, false);
  }

  /**
   *
   * Adds a text-hidden password string field to the dialog
   *
   * @param label String to use as label
   * @param value Initial value
   */

  public void addPassword(String label, boolean isNew)
  {
    addChunk("@pass", label, new Boolean(isNew).toString());
  }

  // from here on down, it's strictly for internal processing. -------------

  private Vector retrieveChunks()
  {
    Vector labels = new Vector();
    Vector operands = new Vector(); // each operand may be a string or a vector
    Vector values = new Vector();  // This is the initial value, may be null
    Vector tempVect = null;

    Vector results = new Vector();

    StringBuffer tempString = new StringBuffer();
    int index = 0;

    /* -- */
    
    if (buffer == null)
      {
	return null;
      }

    char[] chars = buffer.toString().toCharArray();

    while (index < chars.length)
      {
	// first read in the label

	tempString.setLength(0); // truncate the buffer

	while (chars[index] != ':')
 	  {
 	    if (chars[index] == '\n')
	      {
 		throw new RuntimeException("parse error in row" + labels.size());
	      }
	    
	    // if we have a backslashed character, take the backslashed char
	    // as a literal
	    
 	    if (chars[index] == '\\')
 	      {
 		index++;
 	      }
	    
	    tempString.append(chars[index++]);
	  }

	if (tempString.toString().length() != 0)
	  {
	    labels.addElement(tempString.toString());
	  }
	else
	  {
	    throw new RuntimeException("parse error in entry " + labels.size());
	  }

	if (debug)
	  {
	    System.err.println("retrieved chunk " + tempString.toString());
	  }

	index++;		// skip over :

	// next get the value

	tempString.setLength(0);

	while (chars[index] != ':')
 	  {
 	    if (chars[index] == '\n')
	      {
 		//throw new RuntimeException("parse error in row" + labels.size());
		System.err.println("Got a new line, keeping it.");
	      }
	    
	    // if we have a backslashed character, take the backslashed char
	    // as a literal
	    
 	    else if (chars[index] == '\\')
 	      {
 		index++;
 	      }
	    
	    tempString.append(chars[index++]);
	  }

	if (tempString.toString().length() != 0)
	  {
	    values.addElement(tempString.toString());
	  }
	else
	  {
	    values.addElement((String)null);
	  }

	if (debug)
	  {
	    System.err.println("retrieved chunk " + tempString.toString());
	  }

	index++;		// skip over :

	// now read in the operand(s) for this invid

	tempString.setLength(0); // truncate the buffer
	tempVect = null;

	while (chars[index] != '|')
	  {
	    while (chars[index] != '|' && chars[index] != ',')
	      {
		// if we have a backslashed character, take the backslashed char
		// as a literal
	    
		if (chars[index] == '\\')
		  {
		    index++;
		  }
	    
		tempString.append(chars[index++]);
	      }

	    // if we get a non-backslashed comma, we're in a vector

	    if (chars[index] == ',')
	      {
		index++;

		if (tempVect == null)
		  {
		    if (debug)
		      {
			System.err.println("retrieveChunks(): end of first vector chunk: " + tempString.toString());
		      }

		    tempVect = new Vector();

		    if (tempString.length() != 0)
		      {
			tempVect.addElement(tempString.toString());
		      }
		  }
		else
		  {
		    if (debug)
		      {
			System.err.println("retrieveChunks(): end of a middle vector chunk: " + tempString.toString());
		      }

		    if (tempString.length() != 0)
		      {
			tempVect.addElement(tempString.toString());
		      }
		  }

		tempString.setLength(0);
	      }
	  }

	if (tempVect != null)
	  {
	    if (debug)
	      {
		System.err.println("retrieveChunks(): end of a last vector chunk: " + tempString.toString());
	      }

	    if (tempString.length() != 0)
	      {
		tempVect.addElement(tempString.toString());
	      }
	    operands.addElement(tempVect);
	  }
	else
	  {
	    if (debug)
	      {
		System.err.println("retrieveChunks(): end of scalar chunk: " + tempString.toString());
	      }

	    if (tempString.length() == 0)
	      {
		operands.addElement(null);
	      }
	    else
	      {
		operands.addElement(tempString.toString());
	      }
	  }

	index++; // skip |
      }

    // now return a vector of chunks

    if (labels.size() != operands.size())
      {
	throw new RuntimeException("error, labels.size " + labels.size()
				   + " does not equal operands.size " + operands.size());
      }

    for (int i = 0; i < labels.size(); i++)
      {
	results.addElement(new JDialogBuffChunk((String) labels.elementAt(i), operands.elementAt(i),(String)  values.elementAt(i)));
	
	if (debug)
	  {
	    System.out.println("Adding chunk: " + (String) labels.elementAt(i) + ":" +  operands.elementAt(i) +":"+  values.elementAt(i));
	  }
      }

    // to speed GC

    labels = operands = tempVect = null;
    tempString = null;

    // and we're done

    return results;
  }

  // private method for use on the server

  private void addChunk(String label, String operand, String value)
  {
    char[] chars;

    /* -- */

    //    System.err.println("Server adding chunk " + label + ":" + operand);

    if (buffer == null)
      {
	buffer = new StringBuffer();
      }

    // add our label

    chars = label.toCharArray();
	
    for (int j = 0; j < chars.length; j++)
      {
	if (chars[j] == '|')
	  {
	    buffer.append("\\|");
	  }
	else if (chars[j] == ':')
	  {
	    buffer.append("\\:");
	  }
	else if (chars[j] == '\n')
	  {
	    buffer.append("\\\n");
	  }
	else if (chars[j] == '\\')
	  {
	    buffer.append("\\\\");
	  }
	else
	  {
	    buffer.append(chars[j]);
	  }
      }

    buffer.append(":");

    // now the value

    if (value != null)
      {
	chars = value.toCharArray();
	
	for (int j = 0; j < chars.length; j++)
	  {
	    if (chars[j] == '|')
	      {
		buffer.append("\\|");
	      }
	    else if (chars[j] == ':')
	      {
		buffer.append("\\:");
	      }
	    else if (chars[j] == ',')
	      {
		buffer.append("\\,");
	      }
	    else if (chars[j] == '\n')
	      {
		buffer.append("\\\n");
	      }
	    else if (chars[j] == '\\')
	      {
		buffer.append("\\\\");
	      }
	    else
	      {
		buffer.append(chars[j]);
	      }
	  }
      }

    buffer.append(":");

    // and now our operand, if any

    if (operand != null)
      {
	chars = operand.toCharArray();
	
	for (int j = 0; j < chars.length; j++)
	  {
	    if (chars[j] == '|')
	      {
		buffer.append("\\|");
	      }
	    else if (chars[j] == ':')
	      {
		buffer.append("\\:");
	      }
	    else if (chars[j] == ',')
	      {
		buffer.append("\\,");
	      }
	    else if (chars[j] == '\n')
	      {
		buffer.append("\\\n");
	      }
	    else if (chars[j] == '\\')
	      {
		buffer.append("\\\\");
	      }
	    else
	      {
		buffer.append(chars[j]);
	      }
	  }
      }

    buffer.append("|");
  }

  private void addChunk(String label, Vector operands, String value)
  {
    char[] chars;

    /* -- */

    if (buffer == null)
      {
	buffer = new StringBuffer();
      }

    // add our label

    chars = label.toCharArray();
	
    for (int j = 0; j < chars.length; j++)
      {
	if (chars[j] == '|')
	  {
	    buffer.append("\\|");
	  }
	else if (chars[j] == ':')
	  {
	    buffer.append("\\:");
	  }
	else if (chars[j] == '\n')
	  {
	    buffer.append("\\\n");
	  }
	else if (chars[j] == '\\')
	  {
	    buffer.append("\\\\");
	  }
	else
	  {
	    buffer.append(chars[j]);
	  }
      }

    buffer.append(":");

    // now the value

    if (value != null)
      {
	chars = value.toCharArray();
	
	for (int j = 0; j < chars.length; j++)
	  {
	    if (chars[j] == '|')
	      {
		buffer.append("\\|");
	      }
	    else if (chars[j] == ':')
	      {
		buffer.append("\\:");
	      }
	    else if (chars[j] == ',')
	      {
		buffer.append("\\,");
	      }
	    else if (chars[j] == '\n')
	      {
		buffer.append("\\\n");
	      }
	    else if (chars[j] == '\\')
	      {
		buffer.append("\\\\");
	      }
	    else
	      {
		buffer.append(chars[j]);
	      }
	  }
      }

    buffer.append(":");

    // and now our operand, if any

    if (operands != null)
      {
	String operand;

	for (int i = 0; i < operands.size(); i++)
	  {
	    if (i > 0)
	      {
		buffer.append(",");
	      }

	    operand = (String) operands.elementAt(i);
	    
	    chars = operand.toCharArray();
	
	    for (int j = 0; j < chars.length; j++)
	      {
		if (chars[j] == '|')
		  {
		    buffer.append("\\|");
		  }
		else if (chars[j] == ':')
		  {
		    buffer.append("\\:");
		  }
		else if (chars[j] == ',')
		  {
		    buffer.append("\\,");
		  }
		else if (chars[j] == '\n')
		  {
		    buffer.append("\\\n");
		  }
		else if (chars[j] == '\\')
		  {
		    buffer.append("\\\\");
		  }
		else
		  {
		    buffer.append(chars[j]);
		  }
	      }
	  }

	// if we have a singleton vector, add a comma
	// so our retrieveChunks() code will correctly
	// identify this operand as a Vector.  

	// May God Have Mercy On My Soul For This Hack I Now Commit.

	if (operands.size() <= 1)
	  {
	    buffer.append(",");
	  }
      }

    buffer.append("|");
  }

}//JDialogBuff

/*------------------------------------------------------------------------------
                                                                           class
                                                                JDialogBuffChunk

------------------------------------------------------------------------------*/

class JDialogBuffChunk {

  String label;
  Object value;
  String initialValue;

  /* -- */

  public JDialogBuffChunk(String label, Object value, String initialValue)
  {
    this.label = label;
    this.value = value;
    this.initialValue = initialValue;
  }

  public String toString()
  {
    return label + ":" + value;
  }
}
