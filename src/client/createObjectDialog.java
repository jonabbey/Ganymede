/*

 */

package arlut.csd.ganymede.client;

import java.awt.*;
import java.awt.event.*;
import com.sun.java.swing.*;

import java.util.*;
import arlut.csd.ganymede.*;
import arlut.csd.JDataComponent.*;


public class createObjectDialog extends JDialog implements ActionListener {

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

  public createObjectDialog(gclient client) {
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

	    // We don't want Admin Persona to show up in there.
	    if (thisBase.getTypeID() != SchemaConstants.PersonaBase)
	      {
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
      }
    catch (java.rmi.RemoteException rx)
      {
	throw new RuntimeException("Could not check to see if the base was creatable: " + rx);
      }

    listHandles = gc.sortListHandleVector(listHandles);
    types = new JComboBox(listHandles);
    types.setLightWeightPopupEnabled(false);

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
    com.sun.java.swing.JSeparator sep = new com.sun.java.swing.JSeparator();
    gbl.setConstraints(sep, gbc);
    p.add(sep);

    gbc.gridy = 4;
    gbc.gridx = 0;
    gbl.setConstraints(buttonP, gbc);
    p.add(buttonP);
    this.setContentPane(p);

    setSize(200,200);
    pack();
    
  }

  public void actionPerformed(ActionEvent e) {
    if (e.getSource() == ok) 
      {
	listHandle choice = (listHandle)types.getSelectedItem();
	Short type = (Short)choice.getObject();
	if (type.shortValue() >= 0)
	  {
	    gc.createObject(type.shortValue(), true);
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
