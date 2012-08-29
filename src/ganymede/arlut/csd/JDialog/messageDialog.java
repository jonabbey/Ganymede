/*

   messageDialog.java

   A dialog class used to display text and HTML with an optional
   image on the left side, used for the about.. and motd features
   in the Ganymede client.

   Created: 16 September 1998

   Module By: Mike Mulvaney

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2010
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

package arlut.csd.JDialog;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Image;

import javax.swing.ImageIcon;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/*------------------------------------------------------------------------------
                                                                           class
                                                                   messageDialog

------------------------------------------------------------------------------*/

/**
 *
 * A dialog class used to display text and HTML with an optional
 * image on the left side, used for the about.. and motd features
 * in the Ganymede client.
 *
 * @version $Id$
 * @author Mike Mulvaney
 *
 */

public class messageDialog extends StandardDialog {

  JEditorPane
    text;

  JScrollPane
    scrollpane;

  /* -- */

  public messageDialog(JFrame frame, String title, Image image)
  {
    this(frame, title, image, StandardDialog.ModalityType.MODELESS); // not modal
  }

  public messageDialog(JFrame frame, String title, Image image, StandardDialog.ModalityType modality)
  {
    super(frame, title, modality);

    JPanel topPanel = new JPanel(new BorderLayout());
    JPanel pane = new JPanel(new BorderLayout());

    JLabel picture = null;

    if (image == null)
      {
        picture = new JLabel();
      }
    else
      {
        picture = new JLabel(new ImageIcon(image));
      }

    text = new JEditorPane();
    text.setEditable(false);

    scrollpane = new JScrollPane(text);

    topPanel.add("Center", scrollpane);
    topPanel.add("West", picture);

    pane.add("Center", topPanel);
    this.setContentPane(pane);
    this.setPreferredSize(new Dimension(450,200));
  }

  /**
   *
   * Load this message dialog with HTML content.
   *
   * @param s An HTML document held within a string.
   *
   */

  public void setHtmlText(String s)
  {
    text.setContentType("text/html");
    text.setText(s);
    this.setPreferredSize(new Dimension(550,400));
    text.setCaretPosition(0);
  }

  /**
   *
   * Load this message dialog with Unicode content.
   *
   * @param s The message content to be displayed.
   *
   */

  public void setPlainText(String s)
  {
    text.setContentType("text/plain");
    text.setText(s);
    this.setPreferredSize(new Dimension(550,400));
    text.setCaretPosition(0);
  }

  public void setVisible(boolean state)
  {
    pack();

    super.setVisible(state);

    if (state)
      {
        text.setCaretPosition(0);
      }
  }
}
