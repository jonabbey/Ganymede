/*

   aboutJavaDialog.java

   A dialog class used to display information about the JDK being used.
   
   Created: 7 July 2008

   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

   Module By: Jonathan Abbey

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996-2008
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

import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;

import arlut.csd.JDataComponent.JMultiLineLabel;
import arlut.csd.Util.PackageResources;
import arlut.csd.Util.TranslationService;

/*------------------------------------------------------------------------------
                                                                           class
                                                                 aboutJavaDialog

------------------------------------------------------------------------------*/

/**
 *
 * A dialog class used to display information about the JDK being used.
 *   
 * @version $Id$
 * @author Jonathan Abbey
 *
 */

public class aboutJavaDialog extends JCenterDialog implements ActionListener {

  /**
   * <p>TranslationService object for handling string localization in
   * the Ganymede server.</p>
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.JDialog.aboutJavaDialog");

  private final static boolean debug = false;

  private static String versionString = null;

  /**
   * This static method is used to generate the Java version string
   * from System properties.
   *
   * We expect other classes to call this method when they need to get
   * a reportable Java version info string.
   */

  public static synchronized String getVersionInfoString()
  {
    if (versionString == null)
      {
        // "JDK Information\n\nJava Vendor: {0}\nJava VM Name: {1}\nJava Version: {2}\nJava VM Version: {3}\n\nOS Name: {4}\nOS Version: {5}\nSystem Architecture: {6}"

        versionString = ts.l("getVersionInfoString.version_string",
                             System.getProperty("java.vendor"),
                             System.getProperty("java.vm.name"),
                             System.getProperty("java.version"),
                             System.getProperty("java.vm.version"),
                             System.getProperty("os.name"),
                             System.getProperty("os.version"),
                             System.getProperty("os.arch"));
      }

    return versionString;
  }

  // ---

  private JMultiLineLabel textbox = null;
  private JScrollPane scrollpane = null;
  private GridBagLayout gbl = null;
  private GridBagConstraints gbc = null;
  private JButton ok = null;

  /* -- */

  public aboutJavaDialog(JFrame frame, String title)
  {
    super(frame, title, false);	// not modal, thanks

    gbl = new GridBagLayout();
    gbc = new GridBagConstraints();

    JPanel pane = new JPanel();
    pane.setLayout(gbl);
    pane.setOpaque(true);
    pane.setBackground(java.awt.Color.white);

    ImageIcon logo = new ImageIcon(PackageResources.getImageResource(frame,
								     "small_ganymede_title.gif",
								     getClass()));
    JLabel pictureLabel = new JLabel(logo);

    textbox = new JMultiLineLabel();

    JScrollPane scrollPane = new JScrollPane(textbox,
					     JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
					     JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    scrollPane.setBorder(null);
    scrollPane.setViewportBorder(null);
    scrollPane.getViewport().setOpaque(true);
    scrollPane.getViewport().setBackground(java.awt.Color.white);

    ok = new JButton(StringDialog.ok);  // localized
    ok.addActionListener(this);

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
    gbl.setConstraints(scrollPane, gbc);
    pane.add(scrollPane);

    gbc.fill = GridBagConstraints.NONE;
    gbc.anchor = GridBagConstraints.SOUTH;    
    gbc.gridy = 2;
    gbc.weighty = 0.0;
    gbc.insets = new Insets(0,0,0,0);
    gbl.setConstraints(ok, gbc);
    pane.add(ok);

    this.setContentPane(pane);
    this.setBackground(java.awt.Color.white);

    textbox.setText(getVersionInfoString());

    pack();
  }

  public void setVisible(boolean state)
  {
    super.setVisible(state);

    if (state)
      {
	ok.requestFocus();
      }
  }

  public void actionPerformed(ActionEvent e)
  {
    setVisible(false);
  }
}
