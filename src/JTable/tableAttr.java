/*

   tableAttr.java

   A GUI component

   Created: 4 June 1996
   Version: $Revision: 1.1 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package csd;

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
	fontMetric = c.getFontMetrics(font);
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
