/*

   ExceptionDialog.java

   This class provides a dialog for the client to use to throw up an
   exception, and to allow the user to report the bug to the server.
   
   Created: 8 March 2005

   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

   Module By: Jonathan Abbey

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996-2005
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

package arlut.csd.ganymede.client;

import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Hashtable;
import java.util.Vector;

import arlut.csd.JDialog.StringDialog;
import arlut.csd.Util.TranslationService;

/*------------------------------------------------------------------------------
                                                                           class
                                                                 ExceptionDialog

------------------------------------------------------------------------------*/

/**
 * This class provides a dialog for the client to use to throw up an
 * exception, and to allow the user to report the bug to the server.
 */

public class ExceptionDialog {

  /**
   * TranslationService object for handling string localization in
   * the Ganymede system.
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.client.ExceptionDialog");

  // ---

  private StringDialog d;
  private boolean requestedReport = false;

  public ExceptionDialog(Frame parent, String message)
  {
    this(parent, ts.l("init.error"), message, null);
  }

  public ExceptionDialog(Frame parent, String message, Image icon)
  {
    this(parent, ts.l("init.error"), message, icon);
  }

  public ExceptionDialog(Frame parent, String title, String message)
  {
    this(parent, title, message, null);
  }

  public ExceptionDialog(Frame parent, String title, String message, Image icon)
  {
    d = new StringDialog(parent, title, message, ts.l("init.reportException"), ts.l("init.cancel"), icon);
    d.setWrapLength(180);	// give a generous wrap length so that
				// we size our dialog reasonably

    Hashtable results = d.DialogShow();	// StringDialog is modal, so we'll block here

    if (results != null)
      {
	this.requestedReport = true;
      }
  }

  public void setVisible(boolean visible)
  {
    d.setVisible(visible);
  }

  /**
   * If this method returns true, the user asked us to send the
   * exception report to the server.
   */

  public boolean didRequestReport()
  {
    return this.requestedReport;
  }
}

