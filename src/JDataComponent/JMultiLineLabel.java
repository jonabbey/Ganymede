/*

   JMultiLineLabel.java

   A simple label supporting multiple lines.

   Created: 28 January 1998
   Version: $Revision: 1.1 $ %D%
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

public class JMultiLineLabel extends JPanel {

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
  
  int[]
    line_widths;

  String 
    text;  //This is the unwrapped line of text.

  String[]
    lines;

  JLabel[]
    labels;

  boolean
    haveMeasured = false;

  GridBagLayout gbl;
  GridBagConstraints gbc;

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

    this.alignment = alignment;
    this.text = label;
    gbl = new GridBagLayout();
    gbc = new GridBagConstraints();

    setLayout(gbl);
    gbc.gridx = 0;

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

    switch (alignment)
      {
      case LEFT:
	gbc.anchor = GridBagConstraints.WEST;
	break;
      case RIGHT:
	gbc.anchor = GridBagConstraints.EAST;
	break;

      case CENTER:
      default:
	gbc.anchor = GridBagConstraints.CENTER;
	break;
      }

    StringTokenizer t = new StringTokenizer(label, "\n");
    num_lines = t.countTokens(); // Should this be +1?

    labels = new JLabel[num_lines];
    lines = new String[num_lines];
    line_widths = new int[num_lines];
    this.removeAll();

    for (int i =0; i < num_lines; i++)
      {
	lines[i] = t.nextToken().trim();
	labels[i] = new JLabel(lines[i], alignment);

	if (debug)
	  {
	    System.out.println("Adding new label");
	  }

	gbc.gridy = i;
	gbl.setConstraints(labels[i], gbc);
	add(labels[i]);
      }
  }
}
