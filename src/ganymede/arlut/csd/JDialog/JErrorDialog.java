/*

   JErrorDialog.java

   A simple dialog giving an error message.

   Created: 5 October 1998

   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

   Module By: Michael Mulvaney

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996 - 2008
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
import java.awt.Image;

import arlut.csd.Util.TranslationService;

/**
 * Simple way to throw up a StringDialog.
 */

public class JErrorDialog {

  /**
   * <p>TranslationService object for handling string localization in
   * the Ganymede server.</p>
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.JDialog.JErrorDialog");

  // ---

  StringDialog d;

  public JErrorDialog(Frame parent, String message)
  {
    this(parent, ts.l("global.error"), message, null);
  }

  public JErrorDialog(Frame parent, String message, Image icon)
  {
    this(parent, ts.l("global.error"), message, icon);
  }

  public JErrorDialog(Frame parent, String title, String message)
  {
    this(parent, title, message, null);
  }

  public JErrorDialog(Frame parent, String title, String message, Image icon)
  {
    d = new StringDialog(parent, title, message, ts.l("global.ok"), null, icon);
    d.showDialog();
  }

  public void setVisible(boolean visible)
  {
    d.setVisible(visible);
  }
}
