/*

   personaContainer.java

   a panel for handling User's personae.
   
   Created: 15 January 1999
   Version: $Revision: 1.6 $
   Last Mod Date: $Date: 2001/10/11 23:26:37 $
   Module By: Mike Mulvaney

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


package arlut.csd.ganymede.client;

import arlut.csd.ganymede.*;

import java.awt.*;
import java.rmi.*;
import java.util.*;

import arlut.csd.JDataComponent.*;
import arlut.csd.Util.*;

import javax.swing.*;
import javax.swing.border.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                personaContainer

------------------------------------------------------------------------------*/

/**
 * Make sure you add this tab to a JTabbedPane BEFORE you call run()!
 */

class personaContainer extends JScrollPane implements Runnable{

  private final static boolean debug = false;
  
  boolean loaded = false;

  Invid
    invid;

  personaPanel
    pp;

  gclient
    gc;

  boolean
    createNew,
    editable;

  JProgressBar
    progressBar;

  JPanel
    progressPane;

  db_object object;

  /* -- */

  public personaContainer(Invid invid, boolean editable, personaPanel pp, db_object object)
  {
    if (object == null)
      {
	throw new IllegalArgumentException("Got a null object in personaContainer.");
      }

    this.invid = invid;
    this.object = object;
    this.pp = pp;
    gc = pp.gc;
    this.editable = editable;
    this.createNew = createNew;

    progressBar = new JProgressBar();
    progressPane = new JPanel();
    progressPane.add(new JLabel("Loading..."));
    progressPane.add(progressBar);
    getVerticalScrollBar().setUnitIncrement(15);
    setViewportView(progressPane);
  }

  public boolean isEditable()
  {
    return editable;
  }

  public synchronized void run()
  {
    if (debug)
      {
	System.out.println("Starting new thread");
      }

    try
      {
	String label = object.getLabel();

	if ((label != null) && (!label.equals("null")))
	  {
	    pp.middle.setTitleAt(pp.panels.indexOf(this), label);
	    pp.middle.repaint();
	  }
	
	containerPanel cp = new containerPanel(object, invid,
					       editable,
					       pp.fp.getgclient(), 
					       pp.fp.getWindowPanel(), 
					       pp.fp,
					       progressBar,
					       this);
	cp.setBorder(pp.empty);
	setViewportView(cp);
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not load persona into container panel: " + rx);
      }

    loaded = true;

    if (debug)
      {
	System.out.println("Done with thread in personaPanel");
      }

    pp.invalidate();
    pp.fp.validate();
  }

  public synchronized void waitForLoad()
  {
    long startTime = System.currentTimeMillis();

    while (!loaded)
      {
	try
	  {
	    if (debug)
	      {
		System.out.println("presona panel waiting for load!");
	      }

	    this.wait(1000);

	    if (System.currentTimeMillis() - startTime > 200000)
	      {
		System.out.println("Something went wrong loading the persona panel. " +
				   " The wait for load thread was taking too long, so I gave up on it.");
		break;
	      }
	  }
	catch (InterruptedException e)
	  {
	    throw new RuntimeException("Interrupted while waiting for personaContainer to load: " + e);
	  }
      }
  }

  public Invid getInvid()
  {
    return invid;
  }
}
