/*

  PasswordApplet.java

  A simple password change utility in applet form.

  Created: 28 January 1998
   Release: $Name:  $
   Version: $Revision: 1.3 $
   Last Mod Date: $Date: 1999/01/22 18:04:21 $
  Module By: Michael Mulvaney

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

package arlut.csd.ganymede.client.password;

import java.applet.*;
import java.awt.*;
import java.awt.event.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                  PasswordApplet

------------------------------------------------------------------------------*/

public class PasswordApplet extends Applet implements ActionListener{

  PasswordClient
    client;

  TextField
    usernameField = new TextField(10),
    oldPasswordField = new TextField(10),
    newPasswordField1 = new TextField(10),
    newPasswordField2 = new TextField(10);

  Button
    okB = null,
    change = new Button("Change Password");

  Dialog d = null;
  Label dLabel;

  GridBagLayout
    gbl;
  
  GridBagConstraints
    gbc;

  String serverhost;

  /* -- */

  public void init()
  {
    serverhost = getParameter("ganymede.serverhost");

    if (serverhost == null || serverhost.equals(""))
      {
	serverhost = getCodeBase().getHost();
      }

    try
      {
	client = new PasswordClient("//" + serverhost + "/ganymede.server");
      }
    catch (java.rmi.RemoteException rx)
      {
	add(new Label("Could not login to server:" + rx));
	return;
      }

    setLayout(new BorderLayout());

    Panel panel = new Panel();
    add("Center", panel);

    gbl = new GridBagLayout();
    gbc = new GridBagConstraints();

    gbc.anchor = GridBagConstraints.NORTHWEST;
    gbc.insets = new Insets(2,2,2,2);
    
    panel.setLayout(gbl);

    gbc.gridx = 0;
    gbc.gridy = 0;
    Label u = new Label("Username:");
    gbl.setConstraints(u, gbc);
    panel.add(u);
    gbc.gridx = 1;
    gbl.setConstraints(usernameField, gbc);
    panel.add(usernameField);

    gbc.gridx = 0;
    gbc.gridy = 1;

    Label op = new Label("Current Password:");
    gbl.setConstraints(op, gbc);
    panel.add(op);

    gbc.gridx = 1;
    gbl.setConstraints(oldPasswordField, gbc);
    oldPasswordField.setEchoChar('*');
    panel.add(oldPasswordField);

    gbc.gridx = 0;
    gbc.gridy = 2;
    
    Label np = new Label("New Password:");
    gbl.setConstraints(np, gbc);
    panel.add(np);
    gbc.gridx = 1;
    gbl.setConstraints(newPasswordField1, gbc);
    newPasswordField1.setEchoChar('*');
    panel.add(newPasswordField1);

    gbc.gridx = 0;
    gbc.gridy = 3;
    Label ver = new Label("Verify:");
    gbl.setConstraints(ver, gbc);
    panel.add(ver);
    gbc.gridx = 1;
    gbl.setConstraints(newPasswordField2, gbc);
    newPasswordField2.setEchoChar('*');
    panel.add(newPasswordField2);

    change.addActionListener(this);
    add("South", change);

    System.out.println(getPreferredSize());
  }

  public void actionPerformed(ActionEvent e)
  {
    boolean ok = false;

    if (newPasswordField2.getText().equals(newPasswordField1.getText()))
      {
	ok = client.changePassword(usernameField.getText(), oldPasswordField.getText(), newPasswordField2.getText());
      }
    else
      {
	System.out.println("New passwords are not the smae");
	displayDialog("New passwords are not the same.");
      }
    
    if (ok)
      {
	System.out.println("Password change successful.");
	displayDialog("Password change successful.");

	oldPasswordField.setText("");
	newPasswordField1.setText("");
	newPasswordField2.setText("");
      }
    else
      {
	displayDialog("Password change failed.");
      }
  }

  public void displayDialog(String message)
  {
    if (d == null)
      {
	Component frame = getParent();

	while (!(frame instanceof Frame))
	  {
	    frame = ((Component)frame).getParent();
	  }

	d = new Dialog((Frame)frame, "Password change");
	  
	d.setLayout(new BorderLayout());
	  
	dLabel = new Label(message);
	d.add("Center", dLabel);
	  
	okB = new Button("ok");
	okB.addActionListener(new ActionListener () {
	  public void actionPerformed(ActionEvent e) 
	    {
	      d.setVisible(false);
	    }});
	Panel okP = new Panel();
	okP.add(okB);
	d.add("South", okP);
      }
    else
      {
	dLabel.setText(message);
      }
    
    d.pack();
    
    d.show();
  }
}
