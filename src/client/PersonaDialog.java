/*

   PersonaDialog.java

   A subclass of JCenterDialog that allows switching among personas
   
   Created: 17 February 1999
   Release: $Name:  $
   Version: $Revision: 1.3 $
   Last Mod Date: $Date: 1999/03/04 19:52:44 $
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

  private JPasswordField
    password;

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


    // Main dialog container

    JPanel pane = new JPanel(new BorderLayout());
    this.setContentPane(pane);

    JPanel topPanel = new JPanel(new BorderLayout()); 
    JPanel buttonPanel = new JPanel();

    pane.add("Center", topPanel); // Personae go here.
    pane.add("South", buttonPanel); // "OK" button.
    

    // "OK" button

    login = new JButton("OK");
    buttonPanel.add(login);
    login.addActionListener(personaListener);
    login.addActionListener(this);


    // Panel to hold persona radiobuttons and pass field

    JPanel personaPanel = new JPanel(new BorderLayout());
    personaPanel.setBorder(new TitledBorder(new EtchedBorder(),
					"Select Persona",
					TitledBorder.LEFT,
					TitledBorder.TOP));

    JLabel image = new JLabel(new ImageIcon(gc.personaIcon));
    image.setBorder(new EmptyBorder(new Insets(10,15,0,0)));
    image.setVerticalAlignment(JLabel.TOP);
    personaPanel.add("West", image);

    topPanel.add("Center", personaPanel);
    topPanel.add("South", new JSeparator());

    password = new JPasswordField(10);
    password.addActionListener(this);

    JPanel passPanel = new JPanel(new BorderLayout());
    passPanel.add("West",new JLabel("Password: "));
    passPanel.add("Center",password);
    
    Box personaBox = Box.createVerticalBox();
    personaGroupRB = new ButtonGroup();
    
    if (debug) { System.out.println("Adding persona stuff"); }
    
    
    // Build up persona list as radiobutton group
    
    for (int i= personae.size()-1;i >=0; i--)
      {
	String p = (String)personae.elementAt(i);
	JRadioButton rb = new JRadioButton(p);
	if (p.equals(currentPersonaString))
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


  // updatePassField called when RadioButton is toggled.
  // Updates focus and textfield appropriately.

  void updatePassField(String newPersona) {
    this.newPersona = newPersona;

    if (newPersona.equals(my_username)) {
      password.setText("");
      password.setEditable(false);
      password.setBackground(Color.lightGray);
      login.requestFocus();
    }
    else {
      password.setText("");
      password.setEditable(true);
      password.setBackground(Color.white);
      password.requestFocus();
    }
  }

  public void updatePersonaMenu()
  {
    if (debug) { System.out.println("--Updating persona menu"); }

    if (personaGroupRB != null)
      {
	Enumeration RButtons = personaGroupRB.getElements();
	
	while (RButtons.hasMoreElements())
	  {
	    // Weed out nonRadioButtons if exist
	    Object next = RButtons.nextElement();
	    if (!(next instanceof JRadioButton)) {
	      break;
	    }
	    else {
	      JRadioButton rb = (JRadioButton)next;
	      if (rb.getActionCommand().equals(currentPersonaString))
		{
		  if (debug) { System.out.println("Calling setState(true)"); }
		  
		  rb.removeActionListener(personaListener);
		  rb.doClick();
		  rb.requestFocus();
		  rb.addActionListener(personaListener);
		  updatePassField(currentPersonaString);
		  break; // Selecting this one sets others to false
		}
	    }
	  }
      }
  }


  // Access to most recent RButton selection. 
  String getNewPersona(){
    return newPersona;
  }

}

