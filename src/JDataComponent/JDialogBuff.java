/*

   JDialogBuff.java

   Serializable resource class for use with StringDialog.java
   
   Created: 27 January 1998
   Version: $Revision: 1.1 $ %D%
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

public class JDialogBuff implements java.io.Serializable {
  
  StringBuffer buffer;

  /* -- */

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

    addChunk("@1", Title);
    addChunk("@2", Text);
    addChunk("@3", OK);
    addChunk("@4", Cancel);
    addChunk("@5", image);
  }

  /**
   *
   * Adds a labeled text field
   *
   * @param string String to use as the label
   */

  public void addString(String string)
  {
    addChunk("@string", string);
  }

  /**
   * 
   * Adds a labeled check box field
   *
   * @param string String to use as the label
   */
  
  public void addBoolean(String string)
  {
    addChunk("@boolean", string);
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
    addChunk("@choice>" + label, choices);
  }

  /**
   *
   * Adds a one line separator to the dialog
   *
   */

  public void addSeparator()
  {
    addChunk("@separator", (String) null);
  }

  /**
   *
   * Adds a text-hidden password string field to the dialog
   *
   * @param label String to use as label
   */

  public void addPassword(String label)
  {
    addChunk("@pass", label);
  }

  // client side code

  public DialogRsrc extractDialogRsrc(Frame frame)
  { 
    Vector chunks = retrieveChunks();

    String title;
    String text;
    String okText;
    String cancelText;
    String imageName;

    DialogRsrc retVal;

    JDialogBuffChunk chunk;

    /* -- */

    if (chunks.size() < 5)
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
	chunk = (JDialogBuffChunk) chunks.elementAt(index);

	if (chunk.label.equals("@string"))
	  {
	    retVal.addString((String) chunk.value);
	  }
	else if (chunk.label.equals("@boolean"))
	  {
	    retVal.addBoolean((String) chunk.value);
	  }
	else if (chunk.label.equals("@separator"))
	  {
	    retVal.addSeparator();
	  }
	else if (chunk.label.equals("@pass"))
	  {
	    retVal.addPassword((String) chunk.value);
	  }
	else if (chunk.label.startsWith("@choice>"))
	  {
	    String choiceLabel = chunk.label.substring(8); // after @choice>

	    retVal.addChoice(choiceLabel, (Vector) chunk.value);
	  }
	else 
	  {
	    throw new RuntimeException("unrecognized chunk" + chunk.label);
	  }

	index++;
      }

    return retVal;
  }

  private Vector retrieveChunks()
  {
    Vector labels = new Vector();
    Vector operands = new Vector(); // each operand may be a string or a vector
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
		if (tempVect == null)
		  {
		    tempVect = new Vector();
		    tempVect.addElement(tempString.toString());
		  }
	      }
	    else
	      {
		if (tempVect != null)
		  {
		    tempVect.addElement(tempString.toString());
		  }
		else
		  {
		    operands.addElement(tempString.toString());
		  }
	      }
	  }

	if (tempVect != null)
	  {
	    operands.addElement(tempString.toString());
	  }
	else if (tempString.length() == 0)
	  {
	    operands.addElement(null);
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
	results.addElement(new JDialogBuffChunk((String) labels.elementAt(i), operands.elementAt(i)));
      }

    return results;
  }

  // private method for use on the server

  private void addChunk(String label, String operand)
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

  private void addChunk(String label, Vector operands)
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

  /* -- */

  public JDialogBuffChunk(String label, Object value)
  {
    this.label = label;
    this.value = value;
  }
}
