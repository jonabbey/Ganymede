/*
   GASH 2

   clientDesktopMgr.java

   The GANYMEDE object storage system.

   Created: 25 September 2000
   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996-2004
   The University of Texas at Austin

   Contact information

   Web site: http://www.arlut.utexas.edu/gash2
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

package arlut.csd.ganymede.client;

import java.awt.Rectangle;

import javax.swing.DefaultDesktopManager;
import javax.swing.JComponent;

/*------------------------------------------------------------------------------
                                                                           class
                                                                clientDesktopMgr

------------------------------------------------------------------------------*/

/**
 * <p>clientDesktopMgr is a subclass of DefaultDesktopManager which checks to
 * make sure that a inner frame resize doesn't result in the frame being stretched
 * so that its title bar is above the desktop's visible area.</p>
 *
 * @version $Id$
 * @author Jonathan Abbey, jonabbey@arlut.utexas.edu, ARL:UT 
 */

public final class clientDesktopMgr extends DefaultDesktopManager {

  int x, y, w, h;

  /* -- */

  public void beginDraggingFrame(JComponent frame)
  {
    Rectangle rect = frame.getBounds();

    x = rect.x;
    y = rect.y;
    w = rect.width;
    h = rect.height;

    super.beginDraggingFrame(frame);
  }

  public void dragFrame(JComponent frame, int x, int y)
  {
    if (x < 0)
      {
	x = 0;
      }

    if (y < 0)
      {
	y = 0;
      }

    this.x = x;
    this.y = y;

    super.dragFrame(frame, x, y);
  }

  public void endDraggingFrame(JComponent frame)
  {
    setBoundsForFrame(frame, x, y, frame.getWidth(), frame.getHeight());

    super.endDraggingFrame(frame);
  }

  public void beginResizingFrame(JComponent frame, int dir)
  {
    Rectangle rect = frame.getBounds();

    x = rect.x;
    y = rect.y;
    w = rect.width;
    h = rect.height;

    super.beginResizingFrame(frame, dir);
  }

  public void resizeFrame(JComponent frame, int x, int y, int w, int h)
  {
    if (x < 0)
      {
	w = w + x;
	x = 0;
      }

    if (y < 0)
      {
	h = h + y;
	y = 0;
      }

    this.x = x;
    this.y = y;
    this.w = w;
    this.h = h;

    super.resizeFrame(frame, x, y, w, h);
  }

  public void endResizingFrame(JComponent frame)
  {
    setBoundsForFrame(frame, x, y, w, h);

    super.endResizingFrame(frame);
  }
}
