/*

   JcomponentAttr.java

   Created: 13 Aug 1996
   Version: 1.196/11/01
   Module By: Navin Manohar
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.JDataComponent;

import java.awt.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                   JcomponentAttr

------------------------------------------------------------------------------*/

public class JcomponentAttr {

  /* -- */

  public Component c;
  public Font font;
  public FontMetrics fontMetric;
  public Color fg;
  public Color bg;

  public JcomponentAttr(Component c, Font font, Color fg, Color bg)
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
      }
    this.fg = fg;
    this.bg = bg;
  }

  public JcomponentAttr(Component c)
  {
    this.c = c;
    this.font = null;
    this.fontMetric = null;
    this.fg = null;
    this.bg = null;
  }

  public void calculateMetrics()
  {
    if (font == null || c == null)
      {
	fontMetric = null;
      }
    else
      {
	fontMetric = c.getFontMetrics(font);
      }
  } 

  public void setComponent(Component c)
  {
    this.c = c;
  }

  public void setFont(Font font)
  {
    this.font = font;
    calculateMetrics();
  }

  public void setBackground(Color color)
  {
    this.bg = color;
  }

  public void setForeground(Color color)
  {
    this.fg = color;
  }

  //sets the attributes of the instance variable c based
  // on the values given in the JcomponentAttr object

  void setAttr(JcomponentAttr attributes)
  {
    if (this.c == null)
      throw new NullPointerException();

    if (attributes == null)
      throw new IllegalArgumentException("Invalid Paramter: attributes is null");

    this.c.setFont(attributes.font);
    this.c.setForeground(attributes.fg);
    this.c.setBackground(attributes.bg);

  }
    
    

  // sets the attributes of the component c based on the
  // values given in JcomponentAttr object
  public static void setAttr(Component c,JcomponentAttr attributes)
  {
    if (c == null)
      throw new IllegalArgumentException("Invalid Paramter: component handle is null");
    
    if (attributes == null)
      throw new IllegalArgumentException("Invalid Paramter: attributes handle is null");
    c.setFont(attributes.font);
    c.setForeground(attributes.fg);
    c.setBackground(attributes.bg);

  }
}











