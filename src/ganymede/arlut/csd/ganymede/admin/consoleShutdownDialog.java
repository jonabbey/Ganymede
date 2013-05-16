/*
   consoleShutdownDialog.java

   A simple dialog for shutting down the Ganymede Server. 

   Created: 15 May 2013 

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu, James Ratcliff falazar@arlut.utexas.edu

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2013
   The University of Texas at Austin

   Ganymede is a registered trademark of The University of Texas at Austin

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

package arlut.csd.ganymede.admin;

//import java.awt.Dialog;
//import java.awt.Dimension;
//import java.awt.EventQueue;
//import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
//import java.io.IOException;
/*
import java.io.PrintWriter;
import java.io.StringWriter;
import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.util.prefs.Preferences;
*/

import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
//import javax.swing.JDialog;
//import javax.swing.JFrame;
import javax.swing.JLabel;
//import javax.swing.JMenu;
//import javax.swing.JMenuBar;
//import javax.swing.JMenuItem;
import javax.swing.JPanel;
//import javax.swing.JPopupMenu;
//import javax.swing.JScrollPane;
import javax.swing.JSeparator;
//import javax.swing.JSplitPane;
//import javax.swing.JTabbedPane;
//import javax.swing.JTextPane;
import javax.swing.JTextArea;
//import javax.swing.JTextField;
//import javax.swing.SwingConstants;
//import javax.swing.SwingUtilities;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
//import javax.swing.border.TitledBorder;

//import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
//import javax.swing.text.MutableAttributeSet;
//import javax.swing.text.Position;
//import javax.swing.text.SimpleAttributeSet;
//import javax.swing.text.StyleConstants;

//import arlut.csd.ganymede.common.windowSizer;
//import arlut.csd.JDataComponent.JSetValueObject;
//import arlut.csd.JDataComponent.JValueObject;
//import arlut.csd.JDataComponent.JErrorValueObject;
//import arlut.csd.JDataComponent.JsetValueCallback;
import arlut.csd.JDataComponent.JFocusRootPanel;
import arlut.csd.JDataComponent.JMultiLineLabel;
//import arlut.csd.JDataComponent.LAFMenu;
/*
import arlut.csd.JDialog.DialogRsrc;
import arlut.csd.JDialog.JErrorDialog;
import arlut.csd.JDialog.StringDialog;
import arlut.csd.JDialog.messageDialog;
*/
import arlut.csd.JDialog.StandardDialog;

//import arlut.csd.JDialog.aboutGanyDialog;
//import arlut.csd.JDialog.aboutJavaDialog;
//import arlut.csd.JTable.rowSelectCallback;
//import arlut.csd.JTable.rowTable;
import arlut.csd.Util.PackageResources;
import arlut.csd.Util.TranslationService;

//import apple.dts.samplecode.osxadapter.OSXAdapter;
 



/*------------------------------------------------------------------------------
                                                                           class
                                                           consoleShutdownDialog

------------------------------------------------------------------------------*/

/**
 * GUI dialog for presenting server shutdown options in the admin console.
 */

class consoleShutdownDialog extends StandardDialog implements ActionListener, WindowListener {

  private final static boolean debug = false;

  /**
   * TranslationService object for handling string localization in
   * the Ganymede admin console.
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.admin.consoleShutdownDialog");

  GridBagLayout
    gbl;

  GridBagConstraints
    gbc;

  JButton
    now, later, cancel;

  JPanel
    mainPanel, imagePanel, buttonPanel;

  JMultiLineLabel
    textLabel;

  Image image;

  JLabel
    imageCanvas, reasonLabel;

  JTextArea 
    reasonField;

  JButton
    button1, button2, button3;

  private int result = 0;
  private boolean done = false;

  /* -- */

  public consoleShutdownDialog(Frame frame)
  {
    // "Confirm Ganymede Server Shutdown?"
    super(frame, ts.l("global.title"), StandardDialog.ModalityType.DOCUMENT_MODAL);

    this.addWindowListener(this);

    mainPanel = new JPanel();
    mainPanel.setBorder(new CompoundBorder(new EtchedBorder(),
                                           new EmptyBorder(10, 10, 10, 10)));
    gbl = new GridBagLayout();
    gbc = new GridBagConstraints();
    gbc.insets = new Insets(4,4,4,4);

    mainPanel.setLayout(gbl);
    setContentPane(mainPanel);


    //
    // Image on left hand side
    //

    image = PackageResources.getImageResource(frame, "question.gif", frame.getClass());
    imagePanel = new JPanel();

    if (image != null)
      {
        imageCanvas = new JLabel(new ImageIcon(image));
        imagePanel.add(imageCanvas);
      }
    else
      {
        imagePanel.add(Box.createGlue());
      }
 
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.gridwidth = 1;
    gbc.gridheight = GridBagConstraints.REMAINDER;
    gbc.weightx = 0.0;
    gbc.anchor = GridBagConstraints.NORTH;
    gbl.setConstraints(imagePanel, gbc);
    mainPanel.add(imagePanel);

    //
    // Text message under title
    //

    // "Are you sure you want to shut down the Ganymede server\nrunning at {0}?"

    textLabel = new JMultiLineLabel(ts.l("global.question", GASHAdmin.server_url));
    gbc.gridx = 1;
    gbc.gridy = 0;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.gridheight = 1;
    gbc.anchor = GridBagConstraints.WEST; // TODO TEST
    gbl.setConstraints(textLabel, gbc);
    mainPanel.add(textLabel);


    //
    // Separator goes all the way accross
    //

    JSeparator sep = new JSeparator();
    gbc.gridx = 1;
    gbc.gridy = 1;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.gridheight = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbl.setConstraints(sep, gbc);
    mainPanel.add(sep);


    // Label:   Reason:
    reasonLabel = new JLabel(ts.l("global.shutdown_reason_label"));
    gbc.gridx = 1;
    gbc.gridy = 2;
    gbc.gridwidth = 1;
    gbc.gridheight = 1;
    gbc.fill = GridBagConstraints.NONE;
    gbc.anchor = GridBagConstraints.NORTH;
    gbl.setConstraints(reasonLabel, gbc);
    mainPanel.add(reasonLabel);


    // JAMES DEBUG AREA
    // Textarea for reason of shutdown.
    reasonField = new JTextArea(4,40);
    gbc.gridx = 2;
    gbc.gridy = 2;
    gbc.gridwidth = 1;
    gbc.gridheight = 1;
    gbc.fill = GridBagConstraints.BOTH;
    gbl.setConstraints(reasonField, gbc);
    mainPanel.add(reasonField);


    //
    // ButtonPanel takes up the bottom of the dialog
    //

    buttonPanel = new JFocusRootPanel();

    button1 = new JButton(ts.l("global.right_now_button"));
    button1.addActionListener(this);
    buttonPanel.add(button1);

    button2 = new JButton(ts.l("global.later_button"));
    button2.addActionListener(this);
    buttonPanel.add(button2);

    button3 = new JButton(ts.l("global.no_button"));
    button3.addActionListener(this);
    buttonPanel.add(button3);

    //
    // buttonPanel goes underneath
    //

    gbc.gridx = 1;
    gbc.gridy = 5;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.gridheight = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbl.setConstraints(buttonPanel, gbc);
    mainPanel.add(buttonPanel);


    pack();
  }

  /**
   * <p>Display the dialog box, locks this thread while the dialog is being
   * displayed, and returns a hashtable of data field values when the
   * user closes the dialog box.</p>
   *
   * <p>Use this instead of Dialog.show().  If Hashtable returned is null,
   * then the cancel button was clicked.  Otherwise, it will contain a
   * hash of labels(String) to results (Object).</p>
   *
   * @return HashTable of labels to values
   */

  public int showDialog()
  {
    mainPanel.revalidate();

    this.setVisible(true);

    // at this point we're frozen, since we're a modal dialog.. we'll continue
    // at this point when the ok or cancel buttons are pressed.

    if (debug)
      {
        System.err.println("Done invoking.");
      }

    return result;
  }

  public synchronized void actionPerformed(ActionEvent e)
  {
    if (e.getSource() == button1)
      {
        result = 1;
      }
    else if (e.getSource() == button2)
      {
        result = 2;
      }
    else if (e.getSource() == button3)
      {
        result = 0;
      }
    else
      {
        return;
      }

    // pop down so that showDialog() can proceed to completion.

    done = true;

    setVisible(false);
  }


  public String getReasonField()
  {
    return this.reasonField.getText();
  }


  // WindowListener methods

  public void windowActivated(WindowEvent event)
  {
  }

  public void windowClosed(WindowEvent event)
  {
  }

  public synchronized void windowClosing(WindowEvent event)
  {
    if (!done)
      {
        if (debug)
          {
            System.err.println("Window is closing and we haven't done a cancel.");
          }

        // by setting valueHash to null, we're basically treating
        // this window close as a cancel.

        result = 0;
      }

    done = true;

    this.setVisible(false);
  }

  public void windowDeactivated(WindowEvent event)
  {
  }

  public void windowDeiconified(WindowEvent event)
  {
  }

  public void windowIconified(WindowEvent event)
  {
  }

  public void windowOpened(WindowEvent event)
  {
    button3.requestFocus();
  }
}
