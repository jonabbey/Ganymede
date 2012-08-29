/*

   personaContainer.java

   a panel for handling User's personae.
   
   Created: 15 January 1999


   Module By: Mike Mulvaney

   -----------------------------------------------------------------------
            
   Ganymede Directory Management System
 
   Copyright (C) 1996 - 2005
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


package arlut.csd.ganymede.client;

import java.rmi.RemoteException;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;

import arlut.csd.Util.TranslationService;
import arlut.csd.ganymede.common.Invid;
import arlut.csd.ganymede.rmi.db_object;

/*------------------------------------------------------------------------------
                                                                           class
                                                                personaContainer

------------------------------------------------------------------------------*/

/**
 * Make sure you add this tab to a JTabbedPane BEFORE you call run()!
 */

class personaContainer extends JScrollPane implements Runnable{

  private final static boolean debug = false;

  /**
   * TranslationService object for handling string localization in the
   * Ganymede client.
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.client.personaContainer");
  
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

    progressBar = new JProgressBar();
    progressPane = new JPanel();

    // "Loading..."
    progressPane.add(new JLabel(ts.l("global.loading_label")));
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
        System.err.println("Starting new thread");
      }

    try
      {
        String label = object.getLabel();

        if ((label != null) && (!label.equals("null")))
          {
            pp.middle.setTitleAt(pp.panels.indexOf(this), label);
          }

        pp.middle.repaint();
        
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
    catch (Exception rx)
      {
        // "Exception caught in the client while trying to load a persona from the server into a container panel."
        gclient.client.processExceptionRethrow(rx, ts.l("run.exception"));
      }

    loaded = true;

    if (debug)
      {
        System.err.println("Done with thread in personaPanel");
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
                System.err.println("presona panel waiting for load!");
              }

            this.wait(1000);

            if (System.currentTimeMillis() - startTime > 200000)
              {
                System.err.println("Something went wrong loading the persona panel. " +
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
