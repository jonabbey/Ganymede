/*

  tableAttr.java

  A JDK 1.1 table AWT component.

  Copyright (C) 1997  The University of Texas at Austin.

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

  Created: 4 June 1996
  Version: $Revision: 1.6 $ %D%
  Module By: Jonathan Abbey
  Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.JTable;

import java.awt.*;
import java.util.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                       tableAttr

------------------------------------------------------------------------------*/

public class tableAttr {

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
	    System.err.println("font null ptr");
	    System.err.println("c = " + c);
	    System.err.println("font = " + font);
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
