/*

   createObjectDialog.java

   This class is the dialog that is presented to the user when they go to
   create a new object in the client.
   
   Created: 17 September 1998
   Release: $Name:  $
   Version: $Revision: 1.11 $
   Last Mod Date: $Date: 1999/04/14 19:04:39 $
   Module By: Mike Mulvaney

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

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import java.util.*;
import arlut.csd.ganymede.*;
import arlut.csd.JDataComponent.*;
import arlut.csd.JDialog.JCenterDialog;

/*------------------------------------------------------------------------------
                                                                           class
                                                              createObjectDialog

------------------------------------------------------------------------------*/

/**
 * <p>Dialog used to create new objects from the client's toolbar or menu.  The
 * dialog shows the user a list of object types and prompts the user to pick
 * an object type to create.  If the user selects one and clicks ok, we'll
 * try to create a new object for the user and put up a window for the user
 * to edit the new object with if we succeed.</p>
 *
 * @version $Revision: 1.11 $ $Date: 1999/04/14 19:04:39 $ $Name:  $
 * @author Mike Mulvaney
 */

public class createObjectDialog extends JCenterDialog implements ActionListener {

  private boolean debug = false;

  private gclient
    gc;

  JComboBox
    types;

  GridBagLayout
    gbl = new GridBagLayout();

  GridBagConstraints
    gbc = new GridBagConstraints();

  JButton
    ok,
    cancel;

  /* -- */

  public createObjectDialog(gclient client) 
  {
    super(client, "Create Object", true);

    this.gc = client;

    JPanel p = new JPanel(gbl);

    gbc.insets = new Insets(3,3,3,3);

    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.gridheight = 2;
    JLabel image = new JLabel(new ImageIcon(gc.createDialogImage));
    gbl.setConstraints(image, gbc);
    p.add(image);

    gbc.gridheight = 1;
    gbc.ipadx = 8;
    gbc.ipady = 8;
    JMultiLineLabel text = new JMultiLineLabel("Choose the type of object\nyou wish to create");
    gbc.gridx = 1;
    gbc.gridwidth = 2;
    gbl.setConstraints(text, gbc);
    p.add(text);

    gbc.gridwidth = 1;
    gbc.ipadx = gbc.ipady = 0;

    // get list of types from gclient

    Vector bases = client.getBaseList();
    Hashtable baseToShort = client.getBaseToShort();
    Hashtable baseNames = client.getBaseNames();

    Base thisBase = null;

    Vector listHandles = new Vector();

    try
      {
	for (int i = 0; i < bases.size(); i++)
	  {
	    thisBase = (Base)bases.elementAt(i);
	    
	    String name = (String)baseNames.get(thisBase);
		
	    // For some reason, baseNames.get is returning null sometimes.

	    if (name == null)
	      {
		name = thisBase.getName();
	      }
		
	    if (name.startsWith("Embedded:"))
	      {
		if (debug)
		  {
		    System.out.println("Skipping embedded field: " + name);
		  }
	      }
	    else if (thisBase.canCreate(null))
	      {
		listHandle lh = new listHandle(name, (Short)baseToShort.get(thisBase));
		listHandles.addElement(lh);
	      }
	  }
      }
    catch (java.rmi.RemoteException rx)
      {
	throw new RuntimeException("Could not check to see if the base was creatable: " + rx);
      }

    listHandles = gc.sortListHandleVector(listHandles);
    types = new JComboBox(listHandles);
    types.setKeySelectionManager(new TimedKeySelectionManager());

    // Ideally, we'd really like for our JComboBox's pop-ups to be
    // able to go beyond the borders of our dialog.  Unfortunately,
    // the Swing library, up to and including Swing 1.1 beta 3, is
    // hideously broken when it comes to handling pop-ups in dialogs.

    // By leaving it lightweight, our pop-up will get truncated to the
    // dialog's edges, but at least it will be fully displayed, with a
    // scrollable menu region that fits within our dialog.

    // **  types.setLightWeightPopupEnabled(false);

    JLabel l = new JLabel("Type of object:");

    gbc.gridx = 1;
    gbc.gridy = 1;
    gbl.setConstraints(l, gbc);
    p.add(l);

    gbc.gridx = 2;
    gbl.setConstraints(types, gbc);
    p.add(types);
    
    JPanel buttonP = new JPanel();
    ok = new JButton("Ok");
    ok.addActionListener(this);
    cancel = new JButton("Cancel");
    cancel.addActionListener(this);
    buttonP.add(ok);
    buttonP.add(cancel);

    gbc.insets = new Insets(4,4,4,4);

    gbc.gridy = 3;
    gbc.gridx = 0;
    gbc.gridwidth = 3;
    gbc.fill = GridBagConstraints.BOTH;
    javax.swing.JSeparator sep = new javax.swing.JSeparator();
    gbl.setConstraints(sep, gbc);
    p.add(sep);

    gbc.gridy = 4;
    gbc.gridx = 0;
    gbl.setConstraints(buttonP, gbc);
    p.add(buttonP);
    this.setContentPane(p);

    pack();

    setVisible(true);
  }

  public void actionPerformed(ActionEvent e) 
  {
    if (e.getSource() == ok) 
      {
	listHandle choice = (listHandle)types.getSelectedItem();

	Short type = (Short)choice.getObject();

	if (type.shortValue() >= 0)
	  {
	    gc.createObject(type.shortValue());
	  }
	else
	  {
	    System.out.println("No type chosen");
	  }
	
	setVisible(false);
      }
    else if (e.getSource() == cancel) 
      {
	setVisible(false);
      }
  }
}
