/*

  tableCell.java

  A JDK 1.1 table AWT component.

  Copyright (C) 1997, 1998, 1999  The University of Texas at Austin.

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
  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.

  Created: 15 January 1999
  Version: $Revision: 1.1 $
  Last Mod Date: $Date: 1999/01/16 01:27:04 $
  Module By: Jonathan Abbey -- jonabbey@arlut.utexas.edu
  Applied Research Laboratories, The University of Texas at Austin

*/
package arlut.csd.JTable;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                       tableCell

------------------------------------------------------------------------------*/

/**
 *
 * tableCell represents the contents of a single cell in the table.
 *
 * This class is responsible for the mechanics of word wrapping.
 *
 */

class tableCell {

  static final boolean debug = false;

  // --

  /**
   *
   * The non-word-wrapped text for this cell
   *
   */

  String origText;

  /**
   *
   * The word-wrapped text for this cell
   *
   */

  String text;

  /**
   *
   * We may have a binary representation of the text displayed in this
   * cell recorded for convenience.
   *
   */

  Object data = null;

  /**
   *
   * An individual cell may have a special font and colors set.. if so,
   * we'll track it with this variable.
   *
   */

  tableAttr attr;

  /**
   *
   * Remember whether we are selected or not.
   *
   */

  boolean selected;

  /**
   *
   * What column is this cell in?  We use this not only to handle
   * column-specific attributes, but also to get a reference to our
   * baseTable when we need it for internal calculations.
   *
   */

  tableCol col;

  int
    nominalWidth,		// width before any wrapping
    currentWidth,		// width of rightmost pixel of real text in this
				// cell, after wrapping
    lastOfficialWidth = 0;	// what were we last wrapped to?

  /**
   *
   * How many rows has this cell been wrapped across?
   *
   */

  private int rowSpan;

  /* -- */

  public tableCell(tableCol col, String text, tableAttr attr)
  {
    this.col = col;
    this.attr = attr;
    this.selected = false;
    this.nominalWidth = 0;
    this.currentWidth = 0;
    this.setText(text);

    calcRowSpan();
  }

  public tableCell(tableCol col, String text)
  {
    this(col, text, null);
  }

  public tableCell(tableCol col)
  {
    this(col, null, null);
  }

  /**
   *
   * This method reinitializes the cell to its virgin state.
   *
   */

  public void clear()
  {
    this.selected = false;
    this.nominalWidth = 0;
    this.currentWidth = 0;
    this.setText(null);
  }

  /**
   *
   * This method sets the text for this cell.
   *
   */

  public void setText(String newText)
  {
    origText = text = newText;
    
    if (origText != null)
      {
	currentWidth = nominalWidth = getMetrics().stringWidth(origText);
    
	if (lastOfficialWidth != 0)
	  {
	    this.wrap(lastOfficialWidth);
	  }
      }
    else
      {
	currentWidth = nominalWidth = tableCanvas.mincolwidth;
      }

    calcRowSpan();
  }

  /**
   *
   * This method is used to record a piece of random attendant
   * data with this cell.  It is used by rowTable to facilitate
   * type-specific sorting.
   *
   */

  public final void setData(Object data)
  {
    this.data = data;
  }

  /**
   *
   * This method retrieves a piece of random attendant
   * data held with this cell.  It is used by rowTable to facilitate
   * type-specific sorting.
   *
   */

  public final Object getData()
  {
    return data;
  }

  /**
   *
   * This method refreshes the cell's measurements, and should
   * be called after the fontmetrics for this cell have changed.
   *
   */

  public void refresh()
  {
    currentWidth = nominalWidth = getMetrics().stringWidth(origText);
    
    if (lastOfficialWidth != 0)
      {
	this.wrap(lastOfficialWidth);
      }

    calcRowSpan();
  }

  /**
   *
   * Return the fontmetrics that apply to this cell.
   *
   */

  public FontMetrics getMetrics()
  {
    if (attr != null && attr.fontMetric !=null)
      {
	return attr.fontMetric;
      }
    else if (col != null && col.attr != null && col.attr.fontMetric != null)
      {
	return col.attr.fontMetric;
      }
    else 
      {
	return col.rt.tableAttrib.fontMetric;
      }
  }

  /**
   *
   * Return this cell's font
   *
   */

  public Font getFont()
  {
    if (attr != null && attr.fontMetric !=null)
      {
	return attr.font;
      }
    else if (col != null && col.attr != null && col.attr.font != null)
      {
	return col.attr.font;
      }
    else 
      {
	return col.rt.tableAttrib.font;
      }
  }

  /**
   *
   * Return this cell's justification
   *
   */

  public int getJust()
  {
    if (attr != null && attr.align != tableAttr.JUST_INHERIT)
      {
	return attr.align;
      }
    else if (col != null && col.attr != null && col.attr.align != tableAttr.JUST_INHERIT)
      {
	return col.attr.align;
      }
    else 
      {
	return col.rt.tableAttrib.align;
      }
  }

  /**
   *
   * This method returns the width of this cell at its widest point,
   * after word wrapping has been performed.
   *
   */

  public int getCurrentWidth()
  {
    return currentWidth;
  }

  /**
   *
   * This method returns the width that the cell's current text would
   * have if not wrapped.
   * 
   */

  public int getNominalWidth()
  {
    return nominalWidth;
  }

  /**
   *
   * This method returns the nth row of this
   * cell's text, where the first row is 0.
   *
   */

  public String getText(int n)
  {
    if (text == null)
      {
	return null;
      }

    if (n+1 > rowSpan)
      {
	return "";
      }
    else
      {
	int pos, oldpos = -1;

	for (int i = 0; i < n; i++)
	  {
	    pos = text.indexOf('\n', oldpos + 1);

	    if (pos != -1)
	      {
		oldpos = pos;
	      }
	  }

	if (text.indexOf('\n', oldpos+1) == -1)
	  {
	    return text.substring(oldpos+1);
	  }
	else
	  {
	    return text.substring(oldpos+1, text.indexOf('\n', oldpos+1));
	  }
      }
  }

  /**
   *
   * This method wraps the contained text to a certain
   * number of pixels.
   *
   * @param wrap_length The width of the cell to wrap to, in pixels
   *
   */

  public synchronized void wrap(int wrap_length)
  {
    char[] 
      charAry;

    int 
      p,
      p2,
      marker;

    StringBuffer
      result = new StringBuffer();

    FontMetrics
      fm;

     /* -- */

    // if we're empty, don't bother trying to wrap anything

    if (text == null)
      {
	return;
      }

    if (wrap_length < 5)
      {
	throw new IllegalArgumentException("bad params: wrap_length specified as " + wrap_length);
      }

    // if the adjustment is a small enough reduction that it won't affect our
    // line breaking, just return.  Likewise, if we were already unwrapped
    // and our cell width just got bigger, we don't need to wrap.

    if (((wrap_length > currentWidth) && (wrap_length <= lastOfficialWidth)) ||
	((currentWidth == nominalWidth) && (wrap_length >= nominalWidth)))
      {
	return;
      }
    else
      {
	lastOfficialWidth = wrap_length;
      }

    fm = getMetrics();

    if (debug)
      {
	System.err.println("String size = " + origText.length());
      }

    this.currentWidth = 0;
    this.rowSpan = 1;

    // figure out what we want to do about wrapping the origText

    charAry = origText.toCharArray();

    p = marker = 0;

    // each time through the loop, p starts out pointing to the same char as marker

    int localWidth;
    
    while (marker < charAry.length)
      {
	localWidth = 0;

	while ((p < charAry.length) && (charAry[p] != '\n') && (localWidth + fm.charWidth(charAry[p]) < wrap_length))
	  {
	    localWidth += fm.charWidth(charAry[p++]);
	  }

	// now p points to the character that terminated the loop.. either
	// the first character that extends past the desired wrap_length,
	// or the first newline after marker, or it will have overflowed
	// to be == charAry.length

	// remember what our current needs are after wrapping

	if (localWidth > this.currentWidth)
	  {
	    this.currentWidth = localWidth;
	  }
	
	if (p == charAry.length)
	  {
	    if (debug)
	      {
		System.err.println("At completion..");
	      }

	    result.append(origText.substring(marker, p));
	    text = result.toString();

	    return;
	  }

	if (debug)
	  {
	    System.err.println("Step 1: p = " + p + ", marker = " + marker);
	  }

	if (charAry[p] == '\n')
	  {
	    /* We've got a newline.  This newline is bound to have
	       terminated the while loop above.  Step p and marker past
	       the newline and continue on with our loop. */

	    result.append(origText.substring(marker, p));

	    if (debug)
	      {
		System.err.println("found natural newline.. current result = " + result.toString());
	      }

	    p = marker = p+1;
	    rowSpan++;

	    continue;
	  }

	if (debug)
	  {
	    System.err.println("Step 2: hit wrap length, back searching for whitespace break point");
	  }

	p2 = p;

	/* We've either hit the end of the string, or we've
	   gotten past the wrap_length.  Back p2 up to the last space
	   before the wrap_length, if there is such a space.

	   Note that if the next character in the string (the character
	   immediately after the break point) is a space, we don't need
	   to back up at all.  We'll just print up to our current
	   location, do the newline, and skip to the next line. */
	
	if (p < charAry.length)
	  {
	    if (!isspace(charAry[p]))
	      {
		/* back p2 up to the last white space before the break point */

		while ((p2 > marker) && !isspace(charAry[p2]))
		  {
 		    p2--;
		  }
	      }
	  }

	// now we're guaranteed that p2 points to our break character,
	// or that p2 == marker, indicating no whitespace in this row
	// to split on

	/* If the line was completely filled (no place to break),
	   we'll just copy the whole line out and force a break. */

	if (p2 == marker)
	  {
	    p2 = p-1;

	    if (debug)
	      {
		System.err.println("Step 3: no opportunity for break, forcing..");
	      }
	  }
	else
	  {
	    if (debug)
	      {
		System.err.println("Step 3: found break at column " + p2);
	      }
	  }

	if (!isspace(charAry[p2]))
	  {
	    /* If weren't were able to back up to a space, copy
	       out the whole line, including the break character 
	       (in this case, we'll be making the string one
	       character longer by inserting a newline). */

	    if (debug)
	      {
		System.err.println("appending: marker = " + marker + ", p2 = " + p2 + "+1");
	      }
	    
	    result.append(origText.substring(marker, p2+1));
	  }
	else
	  {
	    /* The break character is whitespace.  We'll
	       copy out the characters up to but not
	       including the break character, which
	       we will effectively replace with a
	       newline. */

	    if (debug)
	      {
		System.err.println("appending: marker = " + marker + ", p2 = " + p2);
	      }

	    result.append(origText.substring(marker, p2));
	  }

	/* If we have not reached the end of the string, newline */

	if (p < charAry.length) 
	  {
	    result.append("\n");
	    rowSpan++;
	  }

	p = marker = p2 + 1;
      }

    text = result.toString();
  }

  private boolean isspace(char c)
  {
    return (c == '\n' || c == ' ' || c == '\t');
  }

  private void calcRowSpan()
  {
    if (text == null)
      {
	rowSpan = 1;
	return;
      }

    char[] cAry = text.toCharArray();

    rowSpan = 1;

    for (int i = 0; i < cAry.length; i++)
      {
	if (cAry[i] == '\n')
	  {
	    rowSpan++;
	  }
      }
  }

  /**
   *
   * This method returns the number of lines this cell desires
   * to occupy.
   *
   */

  public int getRowSpan()
  {
    return rowSpan;
  }
}
