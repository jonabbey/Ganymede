/*
   JSpacer.java

   This class defines an invisible space holder that can be placed
   into a GridBagLayout-managed container to make sure that a certain
   minimum row or column size is maintained.
   
   Created: 20 August 2004

   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Directory Droid Directory Management System
 
   Copyright (C) 1996 - 2004
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

import java.awt.Dimension;
import javax.swing.JComponent;

/*------------------------------------------------------------------------------
                                                                           class
                                                                     JSpacer

------------------------------------------------------------------------------*/

/**
 * <p>This class defines an invisible space holder that can be placed
 * into a GridBagLayout-managed container to make sure that a certain
 * minimum row or column size is maintained.</p>
 */

public class JSpacer extends JComponent {

  private int height;
  private int width;
  private Dimension size;

  public JSpacer(int height, int width)
  {
    this.height = height;
    this.width = width;
    this.size = new Dimension(height, width);
  }

  public synchronized int getWidth()
  {
    return width;
  }

  public synchronized int getHeight()
  {
    return height;
  }

  public Dimension getMinimumSize()
  {
    return size;
  }

  public Dimension getMaximumSize()
  {
    return size;
  }

  public Dimension getPreferredSize()
  {
    return size;
  }

  public synchronized Dimension getSize(Dimension x)
  {
    if (x == null)
      {
	return size;
      }
    else
      {
	x.width = this.width;
	x.height = this.height;

	return x;
      }
  }

  public synchronized void setPreferredSize(Dimension input)
  {
    this.height = input.height;
    this.width = input.width;
    this.size = input;
  }

  public synchronized void setSpacerSize(int x, int y)
  {
    this.width = x;
    this.height = y;
    this.size = new Dimension(x,y);
  }
}