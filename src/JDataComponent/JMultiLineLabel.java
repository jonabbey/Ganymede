/*

   JMultiLineLabel.java

   A simple label supporting multiple lines.

   Created: 28 January 1998
   Version: $Revision: 1.3 $ %D%
   Module By: Michael Mulvaney
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.JDataComponent;

import java.awt.*;
import java.util.*;
import com.sun.java.swing.*;

import arlut.csd.Util.WordWrap;

/*------------------------------------------------------------------------------
                                                                           class
                                                                 JMultiLineLabel

------------------------------------------------------------------------------*/

public class JMultiLineLabel extends JTextArea {

  final static boolean debug = false;

  // ---

  /**
   * Alignment stuff.
   */
  public final static int LEFT = SwingConstants.LEFT;
  public final static int RIGHT = SwingConstants.RIGHT;
  public final static int CENTER = SwingConstants.CENTER;

  int 
    margin_height = 5,
    margin_width = 5,
    alignment,
    num_lines,
    line_ascent,
    line_height,
    max_width;

  String 
    text;  //This is the unwrapped line of text.

  String[]
    lines;

  boolean
    haveMeasured = false;

  /*
   * Constructors
   */

  public JMultiLineLabel(String label)
  {
    this(label, JMultiLineLabel.LEFT);
  }

  public JMultiLineLabel(String label, int alignment)
  {
    if (debug)
      {
	System.out.println("Starting new JMLL");
      }

    this.text = label;

    setEditable(false);
    setOpaque(false);

    wrap(40);  // This does a newLabel call
  }

  // Public functions

  public void setText(String s)
  {
    this.text = s;
    newLabel(s);
  }

  public int getAlignment()
  {
    return alignment;
  }
  
  public void setAlignment(int a)
  {
    alignment = a;
    repaint();
  } 

  /**
   * Insert new lines in the string
   *
   * @lineLength Number of characters to wrap the line at.
   */
  public void wrap(int lineLength)
  {
    if (text == null)
      {
	System.out.println("Whoa, text is null");
      }
    
    this.text = WordWrap.wrap(text, lineLength, null);
    newLabel(this.text);
  }

  ///////////////////////////////////////////////////////////////////////////////
  // Private functions
  ///////////////////////////////////////////////////////////////////////////////

  protected void newLabel(String label)
  {
    if (debug)
      {
	System.out.println("newLabel");
      }
    
    

    super.setText(label);
  }

  public static void main(String[] argv)
  {
    JFrame frame = new JFrame();
    frame.getContentPane().add(new JMultiLineLabel("This is a \n\n\n break.  bunch of lines all over the place, should be pretty long, i don't know, but I think it should wrap \n now."));
    frame.pack();
    frame.show();
  }
}
