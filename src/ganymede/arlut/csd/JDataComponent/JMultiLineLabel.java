/*

   JMultiLineLabel.java

   A simple label supporting multiple lines.

   Created: 28 January 1998
   Release: $Name:  $
   Version: $Revision: 1.10 $
   Last Mod Date: $Date: 2003/03/14 01:17:44 $
   Module By: Michael Mulvaney

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999, 2000, 2001, 2002, 2003
   The University of Texas at Austin.

   Contact information

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

package arlut.csd.JDataComponent;

import java.awt.*;
import java.util.*;
import javax.swing.*;

import arlut.csd.Util.WordWrap;

/*------------------------------------------------------------------------------
                                                                           class
                                                                 JMultiLineLabel

------------------------------------------------------------------------------*/

public class JMultiLineLabel extends JTextArea {

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
    metric = getFontMetrics(getFont());

    setEditable(false);
    setOpaque(false);
    setBorder(null); 
    setLineWrap(true);
    setWrapStyleWord(true);

    setText(label);
  }

  // Public functions

  public void setWrapLength(int val)
  {
    columns = val;
    revalidate();
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

    /* -- */

    // First, find out how wide this puppy should be, based on our
    // preferred wrap

    width = getLongestLineWidth(wrap(getText()));
    
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
	return null;
      }
    
    return (WordWrap.wrap(text, columns, null));
  }

  /**
   * <p>We're pretending to be a label, so please don't give us focus.</p>
   */

  public boolean isFocusTraversable()
  {
    return false;
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
