/*
   tableAttr.java

   A JDK 1.1 table Swing component.

   Created: 4 June 1996

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2010
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
   along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/

package arlut.csd.JTable;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;

/*------------------------------------------------------------------------------
                                                                           class
                                                                       tableAttr

------------------------------------------------------------------------------*/

public class tableAttr {

  final static boolean debug = false;

  public final static int
    JUST_LEFT = 0,
    JUST_RIGHT = 1,
    JUST_CENTER = 2,
    JUST_INHERIT = 3;

  /* -- */

  public Component c;
  public Font font;
  public FontMetrics fontMetric;
  public int height, baseline;
  public Color fg;
  public Color bg;
  public int align;

  public tableAttr(Component c, Font font, Color fg, Color bg, int align)
  {
    this.c = c;
    this.font = font;

    if (c != null)
      {
	calculateMetrics();
      }
    else
      {
	if (debug)
	  {
	    System.err.println("Null component c, setting height and baseline to 0");
	  }

	this.fontMetric = null;
	height = 0;
	baseline = 0;
      }

    this.fg = fg;
    this.bg = bg;
    this.align = align;
  }

  public tableAttr(Component c)
  {
    this.c = c;
    this.font = null;
    this.fontMetric = null;
    this.fg = null;
    this.bg = null;
    this.align = JUST_INHERIT;
  }

  public void calculateMetrics()
  {
    if (font == null)
      {
	if (debug)
	  {
	    System.err.println("font null ptr");
	    System.err.println("Setting baseline and height to 0");
	  }
	
	fontMetric = null;
	baseline = 0;
	height = 0;
      }
    else
      {
	try
	  {
	    fontMetric = c.getFontMetrics(font);
	  }
	catch (NullPointerException ex)
	  {
	    if (debug)
	      {
		System.err.println("font null ptr");
		System.err.println("c = " + c);
		System.err.println("font = " + font);
	      }

	    baseline=0;
	    height=0;
	    return;
	  }

	baseline = fontMetric.getMaxAscent();
	height = baseline + fontMetric.getMaxDescent();
      }
  } 

  public void setFont(Font font)
  {
    this.font = font;
    calculateMetrics();
  }
}
