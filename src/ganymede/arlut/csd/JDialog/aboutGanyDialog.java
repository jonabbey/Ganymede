/*

   aboutGanyDialog.java

   A dialog class used to display information about Ganymede
   
   Created: 4 March 2005

   Module By: Jonathan Abbey

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
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;

import arlut.csd.JDataComponent.JMultiLineLabel;
import arlut.csd.Util.PackageResources;
import arlut.csd.Util.TranslationService;

/*------------------------------------------------------------------------------
                                                                           class
                                                                 aboutGanyDialog

------------------------------------------------------------------------------*/

/**
 *
 * A dialog class used to display information about Ganymede
 *   
 * @author Jonathan Abbey
 *
 */

public class aboutGanyDialog extends JDialog {

  private final static boolean debug = false;

  /**
   * <p>TranslationService object for handling string localization in
   * the Ganymede server.</p>
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.JDialog.aboutGanyDialog");

  // ---

  private JMultiLineLabel aboutTextbox = null;
  private JMultiLineLabel creditsTextbox = null;
  private JScrollPane scrollpane = null;
  private GridBagLayout gbl = null;
  private GridBagConstraints gbc = null;
  private JTabbedPane tabPane = null;

  /* -- */

  public aboutGanyDialog(JFrame frame, String title)
  {
    super(frame, title, false); // not modal, thanks

    gbl = new GridBagLayout();
    gbc = new GridBagConstraints();

    JPanel pane = new JPanel();
    pane.setLayout(gbl);
    pane.setOpaque(true);

    ImageIcon logo = new ImageIcon(PackageResources.getImageResource(frame,
                                                                     "small_ganymede_title.png",
                                                                     getClass()));
    JLabel pictureLabel = new JLabel(logo);

    tabPane = new JTabbedPane();

    addTab(ts.l("init.about_tab"), ts.l("init.aboutText",
                                        arlut.csd.Util.SVNVersion.getReleaseString()));

    gbc.anchor = GridBagConstraints.NORTHWEST;
    gbc.fill = GridBagConstraints.NONE;
    gbc.gridy = 0;
    gbc.gridx = 0;
    gbc.gridwidth = 1;
    gbc.gridheight = 1;
    gbl.setConstraints(pictureLabel, gbc);
    pane.add(pictureLabel);

    gbc.fill = GridBagConstraints.BOTH;
    gbc.anchor = GridBagConstraints.CENTER;
    gbc.gridy = 1;
    gbc.weighty = 1.0;
    gbc.weightx = 1.0;
    gbc.insets = new Insets(5,5,5,5);
    gbl.setConstraints(tabPane, gbc);
    pane.add(tabPane);

    this.setContentPane(pane);

    Dimension minimumSize = new Dimension(0, 450);

    this.setMinimumSize(minimumSize);

    super.pack();

    // we add the credits and license tab after packing so that we
    // don't cause the dialog to get really tall in a futile attempt
    // to encompass the whole credits file and/or GPL

    addTab(ts.l("init.credits_tab"), ts.l("init.creditsText"));
    addTab(ts.l("init.license_tab"), ts.l("init.licenseText"));

    setLocationRelativeTo(frame);
  }

  private void addTab(String title, String text)
  {
    JMultiLineLabel textbox = new JMultiLineLabel(true);
    textbox.setOpaque(true);
    textbox.setText(text);
    textbox.setCaretPosition(0);

    JScrollPane scrollpane = new JScrollPane(textbox,
                                             JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                             JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    scrollpane.setBorder(null);
    scrollpane.setViewportBorder(null);
    scrollpane.getViewport().setOpaque(true);

    tabPane.addTab(title, null, scrollpane);
  }

  public void setVisible(boolean state)
  {
    super.setVisible(state);
  }
}
