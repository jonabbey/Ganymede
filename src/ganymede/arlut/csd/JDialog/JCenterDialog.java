/*

   JCenterDialog.java

   A subclass of JDialog that centers itself when pack() is called.
   
   Created: 17 September 1998

   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

   Module By: Michael Mulvaney

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996 - 2009
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

package arlut.csd.JDialog;

import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.GraphicsDevice;
import java.awt.Rectangle;

import javax.swing.JDialog;

/*------------------------------------------------------------------------------
                                                                           class
                                                                   JCenterDialog

------------------------------------------------------------------------------*/

/**
 * A subclass of JDialog that centers itself with respect to its parent Frame
 * when pack() is called.
 */

public class JCenterDialog extends JDialog {

  private final static boolean debug = false;

  private final static int offset = 40;

  Frame frame;

  /* -- */

  public JCenterDialog()
  {
    this(null, null, false);
  }

  public JCenterDialog(Frame frame) 
  {
    this(frame, null, false);
  }

  public JCenterDialog(Frame frame, String title) 
  {
    this(frame, title, false);
  }

  public JCenterDialog(Frame frame, String title, boolean modal) 
  {
    super(frame, title, modal);
    
    this.frame = frame;
  }

  /**
   *
   * This version of pack is for dialogs where you want it to keep the preferred size.
   *
   */

  public void pack()
  {
    super.pack();
    place(getSize().width, getSize().height);

    // on NT with the version of Swing in JDK 1.2, it helps to repack
    // the dialog after placing it..

    super.pack();
  }

  /**
   *
   * If you want to set the size of this dialog to a certain size before it
   * is centered, you need to use this pack method.
   *
   */

  public void pack(int width, int height)
  {
    super.pack();
    setSize(width, height);
    place(width, height);
  }

  /**
   * This method places the dialog in the center of the frame that
   * this dialog is attached to, unless centering the dialog would
   * place it outside the bounds of the screen.
   */

  private void place(int width, int height)
  {
    if (frame != null)
      {
	Rectangle r = frame.getBounds();
	
	if (debug)
	  {
	    System.out.println("Bounds: " + r);
	  }
	
	// Sometimes a new JFrame() is passed in, and it won't have
	// anything interesting for bounds I don't think they are
	// null, but they are all 0 or something.  Might as well make
	// sure they are not null anyway.

	if ((r != null) && ((r.width != 0) && (r.height != 0)))
	  {
	    if (debug)
	      {
		System.out.println("Dialog is " + width + " wide and " + height + " tall.");
	      }

	    int locx = r.width/2 + r.x - width/2;
	    int locy = r.height/2 + r.y - height/2;

	    // make sure that we don't put the dialog off screen if
	    // the dialog is bigger than the frame it's attached to..

	    Rectangle screenSize = getVirtualScreenBounds();

	    if (locx >= (screenSize.x + offset) &&
		locy >= (screenSize.y + offset) &&
		locx <= (screenSize.x + screenSize.width - offset) &&
		locy <= (screenSize.y + screenSize.height - offset))
	      {
		setLocation(locx, locy);

		if (debug)
		  {
		    System.out.println("Setting location to : " + locx + "," + locy);
		  }
	      }
	  }
	else if (debug)
	  {
	    System.out.println("getBounds() returned null.");
	  }
      }
    else if (debug)
      {
	System.out.println("Parent frame is null.");
      }
  }

  /**
   * This method returns the bounds of the screen(s) available in the
   * environment.
   *
   * Taken from the javadoc for java.awt.GraphicsConfiguration
   */

  private Rectangle getVirtualScreenBounds()
  {
    Rectangle virtualBounds = new Rectangle();

    GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();

    GraphicsDevice[] gs = ge.getScreenDevices();

    for (GraphicsDevice gd: gs)
      {
	GraphicsConfiguration gcs[] = gd.getConfigurations();

	for (GraphicsConfiguration gc: gcs)
	  {
	    virtualBounds = virtualBounds.union(gc.getBounds());
	  }
      }

    return virtualBounds;
  }
}
