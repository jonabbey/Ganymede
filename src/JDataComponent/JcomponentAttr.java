/*

   JcomponentAttr.java

   Created: 13 Aug 1996
   Release: $Name:  $
   Version: $Revision: 1.4 $
   Last Mod Date: $Date: 1999/01/22 18:03:57 $
   Module By: Navin Manohar

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999  The University of Texas at Austin.

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
   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.

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
      return;

    if (attributes == null)
      return;

    this.c.setFont(attributes.font);
    this.c.setForeground(attributes.fg);
    this.c.setBackground(attributes.bg);

  }
    
    

  // sets the attributes of the component c based on the
  // values given in JcomponentAttr object
  public static void setAttr(Component c,JcomponentAttr attributes)
  {
    if (c == null)
      return;
    
    if (attributes == null)
      return;

    c.setFont(attributes.font);
    c.setForeground(attributes.fg);
    c.setBackground(attributes.bg);

  }
}











