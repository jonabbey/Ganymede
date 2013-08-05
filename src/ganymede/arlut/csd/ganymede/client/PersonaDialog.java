/*

   PersonaDialog.java

   A subclass of JDialog that allows switching among personas

   Created: 17 February 1999

   Module By: Brian O'Mara

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2012
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

package arlut.csd.ganymede.client;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import arlut.csd.JDialog.StandardDialog;
import arlut.csd.JDialog.StringDialog;
import arlut.csd.Util.TranslationService;

/*------------------------------------------------------------------------------
                                                                           class
                                                                   PersonaDialog

------------------------------------------------------------------------------*/

/**
 * Persona selection dialog
 *
 * @version $Id$
 * @author Brian O'Mara
 */

public class PersonaDialog extends StandardDialog implements ActionListener {

  public final static boolean debug = false;

  /**
   * TranslationService object for handling string localization in the
   * Ganymede client.
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.client.PersonaDialog");

  // ---

  Vector
    personae;

  JButton
    login;

  gclient
    gc;

  private JPasswordField
    password;

  ActionListener
    personaListener;

  ButtonGroup
    personaGroupRB;

  String
    my_username,
    currentPersonaString,
    newPersona;

  public boolean requirePassword = false;
  public boolean changedOK = false;

  /* -- */

  public PersonaDialog(gclient gc, boolean requirePassword)
  {
    // "Choose Persona"
    super(gc, ts.l("init.title"), StandardDialog.ModalityType.DOCUMENT_MODAL);

    this.requirePassword = requirePassword;
    this.gc = gc;

    this.personae = gc.personae;
    this.personaListener = gc.personaListener;
    this.my_username = gc.my_username;
    this.currentPersonaString = gc.currentPersonaString;

    if (requirePassword)
      {
        enableEvents(AWTEvent.WINDOW_EVENT_MASK);
      }

    // Main dialog container

    JPanel pane = new JPanel(new BorderLayout());
    this.setContentPane(pane);

    JPanel topPanel = new JPanel(new BorderLayout());
    JPanel buttonPanel = new JPanel();

    pane.add("Center", topPanel); // Personae go here.
    pane.add("South", buttonPanel); // "OK" button.

    // "OK" button

    login = new JButton(StringDialog.getDefaultOk());
    buttonPanel.add(login);
    login.addActionListener(personaListener);
    login.addActionListener(this);

    // Panel to hold persona radiobuttons and pass field

    JPanel personaPanel = new JPanel(new BorderLayout());

    // "Select Persona"
    personaPanel.setBorder(new TitledBorder(new EtchedBorder(),
                                            ts.l("init.border_title"),
                                            TitledBorder.LEFT,
                                            TitledBorder.TOP));

    JLabel image = new JLabel(new ImageIcon(gc.personaIcon));

    if (requirePassword)
      {
        image.setBorder(new EmptyBorder(new Insets(10,15,0,15)));
        image.setVerticalAlignment(JLabel.TOP);

        JPanel topPersonaPanel = new JPanel(new BorderLayout());

        // "<html><br>The Ganymede server timed you out due to inactivity.<br><br>You will have to re-authenticate with your password in order to continue using Ganymede.</html>"
        JLabel explanation = new JLabel(ts.l("init.timed_out_msg"));

        explanation.setBorder(new EmptyBorder(new Insets(0,0,0,10)));

        topPersonaPanel.add("West", image);
        topPersonaPanel.add("Center", explanation);
        personaPanel.add("North", topPersonaPanel);
      }
    else
      {
        image.setBorder(new EmptyBorder(new Insets(10,15,0,0)));
        image.setVerticalAlignment(JLabel.TOP);
        personaPanel.add("West", image);
      }

    topPanel.add("Center", personaPanel);
    topPanel.add("South", new JSeparator());

    password = new JPasswordField(10);
    password.addActionListener(this);

    JPanel passPanel = new JPanel(new BorderLayout());

    // "Password:\ "
    passPanel.add("West", new JLabel(ts.l("init.password_field")));
    passPanel.add("Center",password);

    Box personaBox = Box.createVerticalBox();
    personaGroupRB = new ButtonGroup();

    if (debug) { System.out.println("Adding persona stuff"); }

    // Build up persona list as radiobutton group

    for (int i= personae.size()-1;i >=0; i--)
      {
        String p = (String)personae.elementAt(i);
        JRadioButton rb = new JRadioButton(p);

        // Note that all strings (incl actionCommand) should be _compared_ as
        // lowercase. Since there is a need for caps to be displayed (title, etc),
        // I didn't just convert to lowercase.

        if (p.toLowerCase().equals(currentPersonaString.toLowerCase()))
          {
            rb.doClick();
            // No actionListener yet, so just selects current username
          }

        rb.addActionListener(personaListener);
        personaGroupRB.add(rb);
        personaBox.add(rb);
      }

    // Give a little space b/t buttons and pass field
    personaBox.add(Box.createVerticalStrut(25));

    personaBox.add(passPanel);
    personaPanel.add("Center", personaBox);

    pack();
    pane.revalidate(); // Win95 browser fix??
    updatePassField(currentPersonaString);
  }

  public void actionPerformed(ActionEvent e)
  {
    // Hitting enter after typing pass is like clicking OK.

    if (e.getSource() instanceof JPasswordField)
      {
        // do this so the PersonaListener gets it

        login.doClick();
      }
    else
      {
        // Clicking OK hides the dialog, as long as
        // we aren't forcing a password

        if (requirePassword && getPasswordField().equals(""))
          {
            if (debug)
              {
                System.err.println("Rejecting actionPerformed.doClick() due to password");
              }

            return;
          }

        setHidden(true);
      }
  }

  // intercept window closure, make sure we're ok

  protected void processWindowEvent(WindowEvent e)
  {
    if (!requirePassword || changedOK)
      {
        super.processWindowEvent(e);
      }
    else if (e.getID() == WindowEvent.WINDOW_CLOSING)
      {
        super.processWindowEvent(e);

        if (requirePassword && !changedOK)
          {
            gc.logout();
          }
      }
    else
      {
        super.processWindowEvent(e);
      }
  }

  public void setHidden(boolean bool)
  {
    if (debug)
      {
        System.err.println("setHidden");
      }

    bool = !bool;
    login.setEnabled(bool);
    password.setEnabled(bool);
    this.setVisible(bool);
  }

  public ButtonGroup getButtonGroup()
  {
    return personaGroupRB;
  }

  String getPasswordField()
  {
    return new String(password.getPassword());
  }

  // updatePassField called when RadioButton is toggled.
  // Updates focus and textfield appropriately.

  void updatePassField(String newPersona)
  {
    this.newPersona = newPersona;

    // If still same persona or base user (no ":" in name) then disable password field

    if (!requirePassword &&
        ((newPersona.toLowerCase()).equals(currentPersonaString.toLowerCase()) ||
         (newPersona.indexOf(":") < 0)))
      {
        password.setText("");
        password.setEditable(false);
        password.setBackground(Color.lightGray);
        password.requestFocus();
      }
    else
      {
        password.setText("");
        password.setEditable(true);
        password.setBackground(Color.white);
        password.requestFocus();
      }
  }

  // Access to most recent RButton selection.
  String getNewPersona(){
    return newPersona;
  }
}
