/*

   modalDialog.java

   A dialog base class for centered, modal dialogs.
   
   Created: 4 December 2009

   Module By: Jonathan Abbey

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996-2009
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

import java.awt.Dialog;
import java.awt.Frame;

import javax.swing.JDialog;

/*------------------------------------------------------------------------------
                                                                           class
                                                                     modalDialog

------------------------------------------------------------------------------*/

/**
 * A dialog base class for centered, modal dialogs.  On the Mac, these
 * dialogs will appear as sheets.
 *   
 * @author Jonathan Abbey
 */

public class modalDialog extends JDialog {

  private boolean already_shown = false;
  private Frame frame;

  public modalDialog(Frame frame, String title)
  {
    super(frame, title, Dialog.ModalityType.DOCUMENT_MODAL);

    this.frame = frame;
  }

  public void setVisible(boolean state)
  {
    if (state && !already_shown)
      {
	if ("Mac OS X".equals(System.getProperty("os.name")))
	  {
	    // set it as a modal sheet on the Mac

	    this.setLocationRelativeTo(null);
	    getRootPane().putClientProperty("apple.awt.documentModalSheet", Boolean.TRUE);
	  }
	else
	  {
	    this.setLocationRelativeTo(frame);
	  }

	already_shown = true;
      }

    super.setVisible(state);
  }
}
