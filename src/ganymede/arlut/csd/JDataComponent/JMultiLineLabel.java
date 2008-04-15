/*

   JMultiLineLabel.java

   A simple label supporting multiple lines.

   Created: 28 January 1998

   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

   Module By: Michael Mulvaney

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996-2005
   The University of Texas at Austin

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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.util.StringTokenizer;

import javax.swing.plaf.ComponentUI;
import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

import arlut.csd.Util.WordWrap;

/*------------------------------------------------------------------------------
                                                                           class
                                                                 JMultiLineLabel

------------------------------------------------------------------------------*/

/**
 * <p>A simple word-wrapping multiline label.  Actually implemented as
 * a JTextArea, as that Swing class has built-in support for the word
 * wrapping.</p>
 */

public class JMultiLineLabel extends JTextArea {

  /**
   * Controls the default word wrap length.  The default value of 128
   * columns should hopefully be wide enough to handle exception
   * traces gracefully.
   */

  int 
    columns = 128;

  /*
   * Constructors
   */

  public JMultiLineLabel()
  {
    this(null);
  }

  public JMultiLineLabel(String label)
  {
    setEditable(false);
    setOpaque(false);
    setBorder(null); 
    setLineWrap(true);
    setWrapStyleWord(true);

    this.setBackground((Color)UIManager.get("Label.background"));
    this.setForeground((Color)UIManager.get("Label.foreground"));
    this.setFont((Font)UIManager.get("Label.font"));

    setText(label);
  }

  /**
   * If our UI Component delegate is changed, we'll want to go ahead
   * and rework the color scheme to go along with the new look and
   * feel.
   */

  public void updateUI()
  {
    super.updateUI();

    this.setFont((Font)UIManager.get("Label.font"));
    this.setForeground((Color)UIManager.get("Label.foreground"));
    this.setBackground((Color)UIManager.get("Label.background"));

    this.setOpaque(false);
    this.setBorder(null);

    repaint();
  }

  public void update(Graphics g)
  {
    this.setFont((Font)UIManager.get("Label.font"));
    this.setForeground((Color)UIManager.get("Label.foreground"));
    this.setBackground((Color)UIManager.get("Label.background"));

    super.update(g);
  }

  // Public functions

  public void setWrapLength(int val)
  {
    columns = val;
    revalidate();
  }

  public Dimension getPreferredSize()
  {
    int 
      width,
      height;

    /* -- */

    // First, find out how wide this puppy should be, based on our
    // preferred wrap.  Note that the wrap algorithm we're using may
    // not be precisely the same as that used by JTextArea, so this
    // calculation might not match exactly what you'd think it should
    // be.

    Insets x = this.getInsets();

    width = getLongestLineWidth(wrap(getText())) + x.left + x.right;

    // Now, the height
    height = super.getPreferredSize().height;

    return new Dimension(width, height);
  }

  public Dimension getPreferredScrollableViewportSize()
  {
    return this.getPreferredSize();
  }

  /**
   * Local convenience method to wrap the String provided to the
   * number of columns last set by {@link
   * arlut.csd.JDataComponent#setWrapLength(int)}.
   *
   * If no call to setWrapLength() has been made, the wrap length
   * defaults to 128 characters.
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

  public boolean isFocusable()
  {
    return false;
  }

  ///////////////////////////////////////////////////////////////////////////////
  // Private functions
  ///////////////////////////////////////////////////////////////////////////////

  /**
   * <p>This function is designed to return the number of pixels wide
   * this JMultiLineLabel needs to be in order to fit all the lines if
   * they are wrapped by the {@link arlut.csd.Util.WordWrap} algorithm at
   * the fixed wrap length set for this JMultiLineLabel.</p>
   *
   * <p>In theory, this should be enough for getPreferredSize() to give
   * us the elbow room we need, but for some reason that doesn't seem to be
   * happening properly when we are used in StringDialog.</p>
   */

  private int getLongestLineWidth(String wrappedText)
  {
    int length = 0;
    int maxLength = 0;
    StringTokenizer tk = new StringTokenizer(wrappedText, "\n");

    Font myFont = getFont();
    FontMetrics myFontMetrics = getFontMetrics(myFont);

    while (tk.hasMoreElements())
      {
	String nextToken = WordWrap.deTabify((String) tk.nextElement());

	length = myFontMetrics.getStringBounds(nextToken, getGraphics()).getBounds().width;

	if (length > maxLength)
	  {
	    maxLength = length;
	  }
      }

    return maxLength;
  }

  public static void main(String[] argv)
  {
    JMultiLineLabel x = new JMultiLineLabel("This is a break.  This string is so long that I expect to see it broken up, and the size demanded be based on the specified wrap length.  Blah blah blah bunch of lines all over the place, should be pretty long, i don't know, but I think it should wrap now.");

    System.err.println("The dimensions of x are " + x.getPreferredSize());
    JFrame frame = new JFrame();
    frame.getContentPane().add(x);
    frame.pack();
    frame.setVisible(true);
    System.err.println("The new dimensions of x are " + x.getPreferredSize());
    x.setWrapLength(40);
    System.err.println("The new new dimensions of x are " + x.getPreferredSize());
    x.setWrapLength(400);
    System.err.println("The new new new dimensions of x are " + x.getPreferredSize());
  }
}
