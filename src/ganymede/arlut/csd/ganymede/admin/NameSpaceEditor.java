/*

   NameSpaceEditor.java

   Base Editor component for GASHSchema.
   
   Created: 14 August 1997

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

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

package arlut.csd.ganymede.admin;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.RemoteException;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import arlut.csd.JDataComponent.JstringField;
import arlut.csd.JDataComponent.JLabelPanel;
import arlut.csd.JDataComponent.JStretchPanel;
import arlut.csd.ganymede.rmi.Base;
import arlut.csd.ganymede.rmi.BaseField;
import arlut.csd.ganymede.rmi.NameSpace;
import arlut.csd.ganymede.rmi.SchemaEdit;

/*------------------------------------------------------------------------------
                                                                           class
                                                                 NameSpaceEditor

------------------------------------------------------------------------------*/

public class NameSpaceEditor extends JStretchPanel implements ActionListener {

  static final boolean debug = false;
  
  SpaceNode node;
  NameSpace space;
  JstringField nameS;
  JList spaceL;
  JCheckBox caseCB;
  JLabelPanel nameJPanel;
  GASHSchema owner;
  String currentNameSpaceLabel = null;
  
  /* -- */

  NameSpaceEditor(GASHSchema owner)
  {
    if (owner == null)
      {
	throw new IllegalArgumentException("owner must not be null");
      }

    if (debug)
      {
	System.err.println("NameSpaceEditor constructed");
      }

    this.owner = owner;

    nameS = new JstringField(20, 100, false, false, null, null);

    caseCB = new JCheckBox();
    caseCB.setEnabled(false);

    spaceL = new JList();
    spaceL.setBorder(BorderFactory.createLineBorder(Color.black));

    nameJPanel = new JLabelPanel();
    nameJPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
    nameJPanel.addRow("Namespace:", nameS);
    nameJPanel.addRow("Case Insensitive:", caseCB);
    nameJPanel.addRow("Fields In This Space:", spaceL);

    setComponent(nameJPanel);
  }

  public void editNameSpace(SpaceNode node)
  {
    this.node = node;
    space = node.getSpace();
    
    try
      {
	nameS.setText(space.getName());
	caseCB.setSelected(space.isCaseInsensitive());
	currentNameSpaceLabel = space.getName();
	refreshSpaceList();
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Remote Exception gettin gNameSpace attributes " + rx);
      }
  }

  public void actionPerformed(ActionEvent e)
  {
    if (debug)
      {
	System.out.println("action Performed in NameSpaceEditor");
      }
  }

  public void refreshSpaceList()
  {
    spaceL.removeAll();
    SchemaEdit se = owner.getSchemaEdit();
    Base[] bases = null;

    try
      {
	bases = se.getBases(); // we want to find all fields that refer to this namespace
      }
    catch (RemoteException rx)
      {
	throw new IllegalArgumentException("Exception: can't get bases: " + rx);
      }

    Vector fields = null;
    Vector spaceV = new Vector();
    BaseField currentField = null;
    String thisBase = null;
    String thisSpace = null;

    if ((bases == null) || (currentNameSpaceLabel == null))
      {
	System.out.println("bases or currentNameSpaceLabel is null");
      }
    else
      {
	if (debug)
	  {
	    System.out.println("currentNameSpaceLabel= " + currentNameSpaceLabel);
	  }
	  
	for (int i = 0; i < bases.length; i++)
	  {
	    try
	      {
		thisBase = bases[i].getName();
		fields = bases[i].getFields();
	      }
	    catch (RemoteException rx)
	      {
		throw new IllegalArgumentException("exception getting fields: " + rx);
	      }

	    if (fields == null)
	      {
		if (debug)
		  {
		    System.out.println("fields == null");
		  }
	      }
	    else
	      {
		for (int j = 0; j < fields.size(); j++)
		  {
		    try 
		      {
			currentField = (BaseField)fields.elementAt(j);

			if (currentField.isString())
			  {
			    thisSpace = currentField.getNameSpaceLabel();

			    if ((thisSpace != null) && (thisSpace.equals(currentNameSpaceLabel)))
			      {
				if (debug)
				  {
				    System.out.println("Adding to spaceV: " + thisBase +
						       ":" + currentField.getName());;
				  }

				spaceV.addElement(thisBase + ":" + currentField.getName());
			      }
			  }
		      }
		    catch (RemoteException rx)
		      {
			throw new IllegalArgumentException("Exception generating spaceL: " + rx);
		      }
		    
		  }

		spaceL.setListData(spaceV);
	      }
	  }
      }
  }

  /**
   * GC-aiding dissolution method.  Should be called on GUI thread.
   */

  public void cleanup()
  {
    this.node = null;
    this.space = null;	// remote reference
    this.nameS = null;
    this.spaceL = null;
    this.caseCB = null;

    if (this.nameJPanel != null)
      {
	this.nameJPanel.cleanup();
	this.nameJPanel = null;
      }

    this.owner = null;
    this.currentNameSpaceLabel = null;


    // and clean up the AWT's linkages

    this.removeAll();		// should be done on GUI thread
  }
}
