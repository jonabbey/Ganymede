/*

   PersonaDialog.java

   A subclass of JCenterDialog that allows switching among personas
   
   Created: 17 February 1999
   Release: $Name:  $
   Version: $Revision: 1.1 $
   Last Mod Date: $Date: 1999/02/23 23:50:45 $
   Module By: Brian O'Mara

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

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import arlut.csd.JDialog.JCenterDialog;

public class PersonaDialog extends JCenterDialog implements ActionListener{

  private final static boolean debug = false;

  // ---

  Vector 
    personae;

  JButton
    login;

  gclient 
    gc;

  JLabel 
    passLabel;

  JPasswordField
    password;

  JPanel 
    passPanel,
    personaPanel;

  Box 
    personaBox;
  
  PersonaListener 
    personaListener;

  ButtonGroup 
    personaGroupRB;
  /* -- */
  String 
    my_username,
    currentPersonaString,
    newPersona;

  public PersonaDialog(gclient gc)
  {
    super(gc, "Choose Persona", true);

    this.gc = gc;
    this.personae = gc.personae;
    this.personaListener = gc.personaListener;
    this.my_username = gc.my_username;
    this.currentPersonaString = gc.currentPersonaString;

    personaPanel = new JPanel(new BorderLayout());
    personaPanel.setBorder(new TitledBorder(new EtchedBorder(),
					"Select Persona",
					TitledBorder.LEFT,
					TitledBorder.TOP));

    passLabel = new JLabel("Password:");
    password = new JPasswordField(10);
    password.addActionListener(this);

    passPanel = new JPanel(new BorderLayout());
    passPanel.add("West",passLabel);
    passPanel.add("Center",password);
    personaBox = Box.createVerticalBox();
    personaGroupRB = new ButtonGroup();

    if ((personae != null)  && personae.size() > 1)
      {
	if (debug)
	  {
	    System.out.println("Adding persona stuff");
	  }
	
	for (int i= personae.size()-1;i >=0; i--)
	  {
	    String p = (String)personae.elementAt(i);
	    JRadioButton rb = new JRadioButton(p);
	    if (p.equals(my_username))
	      {
		currentPersonaString = p;
		rb.doClick(); // note that only selects initial persona "username".
		              // Does no action b/c actionlisterer not added yet. 
	      }

	    rb.addActionListener(personaListener);
	    personaGroupRB.add(rb);
	    personaBox.add(rb);
	  }
	personaBox.add(Box.createVerticalStrut(25));
	personaBox.add(passPanel);
	personaPanel.add("North", personaBox);
      }
    else if (debug)
      {
	System.out.println("No personas.");
      }

    JPanel topPanel = new JPanel(new BorderLayout()); 
    JPanel buttonPanel = new JPanel();
    JPanel pane = new JPanel(new BorderLayout());

    topPanel.add("Center", personaPanel);
    topPanel.add("South", new JSeparator());

    // Create the button Panel for the bottom
    login = new JButton("OK");
    login.addActionListener(personaListener);
    login.addActionListener(this);
    buttonPanel.add(login);

    pane.add("Center", topPanel);
    pane.add("South", buttonPanel);
    this.setContentPane(pane);
    pack();
    updatePassField(currentPersonaString);
	
  }


  public void actionPerformed(ActionEvent e)
    {
      // Hitting enter after typing pass is like clicking OK.
      if (e.getSource() instanceof JPasswordField) {
	login.doClick();
      }
      else {
	// Clicking OK hides the dialog
	setVisible(false);
      }
    }

  public void layout(int width, int height)
  {
    // Uses a special pack in JCenterDialog
    pack(width, height);
  }

  public ButtonGroup getButtonGroup() {
    return personaGroupRB;
  }

  String getPasswordField() {
    return new String(password.getPassword());
  }

  // Called when RadioButton is toggled.
  // Updates focus and testbox appropriately.
  void updatePassField(String newPersona) {
    this.newPersona = newPersona;
    if (newPersona.equals(my_username)) {
      password.setText("");
      password.setEnabled(false);
      password.setEditable(false);
      password.setBackground(Color.lightGray);
      login.requestFocus();
    }
    else {
      password.setText("");
      password.setEnabled(true);
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

