/*

   JMultiLineLabel.java

   A simple label supporting multiple lines.

   Created: 28 January 1998
   Version: $Revision: 1.4 $ %D%
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

  String[]
    lines;

  boolean
    haveMeasured = false;

  int 
    columns = 42;

  FontMetrics
    metric;

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

    metric = getFontMetrics(getFont());
    
    setEditable(false);
    setOpaque(false);
    
    // The JTextArea has an etched border around it, so get rid of it.
    //setBorder(BorderFactory.createEmptyBorder(0,0,0,0); 
    setBorder(null); 

    // Find out the length of the string.  If the length is less than
    // the default number of columns, make the number of columns the
    // same as the string.

    if (label != null)
      {
	int length = label.length();
	System.out.println("String is " + length + " chars long.");
	if (length < columns)
	  {
	    columns = length;
	  }
      }

    setText(wrap(label));
    
  }

  // Public functions

  public void setText(String s)
  {
    super.setText(wrap(s));
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

  public Dimension getPreferredSize()
  {
    int 
      width,
      height;

    String text = wrap(getText());

    /* -- */

    // First, find out how wide this puppy is
    width = getLongestLineWidth(text);
    
    // Now, the height
    height = super.getPreferredSize().height;

    return new Dimension(width, height);

  }

  /**
   * Insert new lines in the string
   *
   * @lineLength Number of characters to wrap the line at.
   */
  public String wrap(String text)
  {
    if (text == null)
      {
	System.out.println("Whoa, text is null");
      }
    
    return (WordWrap.wrap(text, columns, null));

  }

  ///////////////////////////////////////////////////////////////////////////////
  // Private functions
  ///////////////////////////////////////////////////////////////////////////////

  private int getLongestLineWidth(String wrappedText)
  {
    int length = 0;
    int maxLength = 0;
    StringTokenizer tk = new StringTokenizer(wrappedText, "\n");

    while (tk.hasMoreElements())
      {
	length = metric.stringWidth((String)tk.nextElement());
	if (length > maxLength)
	  {
	    maxLength = length;
	  }
      }

    return maxLength;
  }

  public static void main(String[] argv)
  {
    JFrame frame = new JFrame();
    frame.getContentPane().add(new JMultiLineLabel("This is a break.  bunch of lines all over the place, should be pretty long, i don't know, but I think it should wrap now."));
    frame.pack();
    frame.show();
  }
}
