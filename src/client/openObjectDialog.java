/*

   createObjectDialog.java

   A Dialog to open an object from the database for a variety of operations.
   
   Created: 31 October 1997
   Release: $Name:  $
   Version: $Revision: 1.22 $
   Last Mod Date: $Date: 1999/01/22 18:04:17 $
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
import javax.swing.border.TitledBorder;
import java.util.Vector;
import java.util.Hashtable;

import arlut.csd.ganymede.*;
import arlut.csd.JDataComponent.*;
import arlut.csd.Util.VecQuickSort;
import arlut.csd.JDialog.JCenterDialog;

/*------------------------------------------------------------------------------
                                                                           class
                                                                openObjectDialog

------------------------------------------------------------------------------*/

/**
 *
 * A Dialog to open an object from the database for a variety of operations.
 *
 */

public class openObjectDialog extends JCenterDialog implements ActionListener, MouseListener {

  private final static boolean debug = false;

  boolean 
    editableOnly = false;

  long
    lastClick = 0;

  Invid 
    invid = null;

  GridBagLayout
    gbl;
  
  GridBagConstraints
    gbc;

  gclient
    client;

  JPanel 
    middle;

  JList
    list = null;

  JScrollPane
    pane = null;

  JComboBox
    type;

  JButton
    ok;

  JTextField 
    text;

  listHandle
    lastObject = null,
    currentObject = null;

  JLabel
    titleL;

  String lastValue = null;

  String selectedBaseName = null;
  String selectedObjectName = null;
  boolean selectedFound = false;

  /* -- */

  /**
   *
   * This is the constructor for openObjectDialog.  The thing worth noting
   * about this is that when this object is first constructed, it checks
   * gclient to see if an object node is selected in the gclient's tree.
   * If so, it will set the object's type and name in the appropriate
   * GUI fields as they are constructed.<BR><BR>
   *
   * Note that this field-setting only occurs when this dialog is first
   * constructed.  I would have had to rewrite a lot of this class to
   * make it so that the base/object name fields could have been set
   * after the fact, so I just didn't.  gclient is set up to dispose
   * of an old instance of this and recreate one as necessary.  It's
   * a shameful hack, but it works, and it's not worth investing much
   * more time here to avoid the redundant dialog creation.
   *
   */

  public openObjectDialog(gclient client)
  {
    super(client, "Open object", true);

    this.client = client;

    InvidNode selectedNode = client.getSelectedObjectNode();

    if (selectedNode != null)
      {
	selectedBaseName = selectedNode.getTypeText();
	selectedObjectName = selectedNode.getText();
      }

    gbl = new GridBagLayout();
    gbc = new GridBagConstraints();
    
    getContentPane().setLayout(new BorderLayout());
    middle = new JPanel(gbl);
    getContentPane().add(middle, BorderLayout.CENTER);
    gbc.insets = new Insets(4,4,4,4);
    
    titleL = new JLabel("Choose invid:", SwingConstants.CENTER);
    titleL.setFont(new Font("Helvetica", Font.BOLD, 14));
    titleL.setOpaque(true);
    titleL.setBorder(client.emptyBorder5);
    
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.gridwidth = 4;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbl.setConstraints(titleL, gbc);
    middle.add(titleL);
    
    gbc.fill = GridBagConstraints.NONE;
    
    type = new JComboBox();

    // Ideally, we'd really like for our JComboBox's pop-ups to be
    // able to go beyond the borders of our dialog.  Unfortunately,
    // the Swing library, up to and including Swing 1.1 beta 3, is
    // hideously broken when it comes to handling pop-ups in dialogs.

    // By leaving it lightweight, our pop-up will get truncated to the
    // dialog's edges, but at least it will be fully displayed, with a
    // scrollable menu region that fits within our dialog.

    // **   type.setLightWeightPopupEnabled(false);

    Vector bases = client.getBaseList();
    Hashtable baseToShort = client.getBaseToShort();
    Hashtable baseNames = client.getBaseNames();

    Base thisBase = null;

    Vector listHandles = new Vector();

    for (int i = 0; i < bases.size(); i++)
      {
	thisBase = (Base)bases.elementAt(i);
	String name = (String) baseNames.get(thisBase);
	
	if (name.startsWith("Embedded:"))
	  {
	    if (debug)
	      {
		System.out.println("Skipping embedded field: " + name);
	      }
	  }
	else
	  {
	    listHandle lh = new listHandle(name, (Short) baseToShort.get(thisBase));
	    listHandles.addElement(lh);
	  }
      }

    //
    //
    // We do need to sort the list handles, but gclient doesn't have this method
    //
    //

    listHandles = client.sortListHandleVector(listHandles);

    if (selectedBaseName != null)
      {
	// the first thing we add will start off selected.. add
	// the currently selected object type first,

	for (int i = 0; i < listHandles.size(); i++)
	  {
	    listHandle lh = (listHandle) listHandles.elementAt(i);

	    if (lh.getLabel().equals(selectedBaseName))
	      {
		type.addItem(lh);
		selectedFound = true;
	      }
	  }

	// and now add the rest, in case they change their mind.

	for (int i = 0; i < listHandles.size(); i++)
	  {
	    listHandle lh = (listHandle) listHandles.elementAt(i);

	    if (!lh.getLabel().equals(selectedBaseName))
	      {
		type.addItem(lh);
	      }
	  }
      }
    else
      {
	for (int i = 0; i < listHandles.size(); i++)
	  {
	    type.addItem(listHandles.elementAt(i));
	  }
      }

    gbc.gridx = 0;
    gbc.gridy = 1;

    gbc.fill = GridBagConstraints.NONE;
    gbc.gridwidth = 2;

    JLabel oType = new JLabel("Object Type:"); 

    gbl.setConstraints(oType, gbc);
    middle.add(oType);

    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.gridx = 2;
    gbl.setConstraints(type, gbc);
    middle.add(type);
	
    text = new JTextField(20);

    if (selectedFound && selectedObjectName != null)
      {
	text.setText(selectedObjectName);
      }

    text.addActionListener(this);
    JLabel editTextL = new JLabel("Object Name:");
    
    gbc.fill = GridBagConstraints.NONE;
    gbc.gridwidth = 2;
    gbc.gridx = 0;
    gbc.gridy = 2;
    gbl.setConstraints(editTextL, gbc);
    middle.add(editTextL);

    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridwidth = GridBagConstraints.REMAINDER;    
    gbc.gridx = 2;
    gbl.setConstraints(text, gbc);
    middle.add(text);
    
    JPanel buttonP = new JPanel();
    
    ok = new JButton("Ok");
    ok.setActionCommand("Find Object with this name");
    ok.addActionListener(this);
    buttonP.add(ok);

    JButton neverMind = new JButton("Cancel");
    neverMind.setActionCommand("Nevermind finding this object");
    neverMind.addActionListener(this);
    buttonP.add(neverMind);
	
    getContentPane().add(buttonP, BorderLayout.SOUTH);	
        
    setBounds(150,100, 200,100);
  }

  public Invid chooseInvid()
  {
    pack();
    type.requestFocus();

    setVisible(true);

    return invid;
  }

  public void setText(String text)
  {
    titleL.setText(text);
  }

  public void setReturnEditableOnly(boolean editableOnly)
  {
    this.editableOnly = editableOnly;
  }

  public String getTypeString()
  {
    listHandle lh = (listHandle)type.getSelectedItem();
    return lh.getLabel();
  }

  public void close(boolean foundOne)
  {
    // Make sure we return null if we didn't find one

    if (!foundOne)
      {
	invid = null;
      }

    setVisible(false);
    
    text.setText("");

    if (list != null)
      {
	if (debug)
	  {
	    System.out.println("Removing the list");
	  }
	pane.remove(list);
      }

    if (pane != null)
      {
	if (debug)
	  {
	    System.out.println("Removing pane");
	  }
	middle.remove(pane);
      }

    if (debug)
      {
	System.out.println("Nulling the pane and list");
      }

    pane = null;
    list = null;
  }

  public void actionPerformed(ActionEvent e)
  {
    if (debug)
      {
	System.out.println("Action performed: " + e.getActionCommand());
      }

    if (e.getSource() == text)
      {
	ok.doClick();
      }
    else if (e.getActionCommand().equals("Find Object with this name"))
      {
	String string = text.getText();

	if ((string == null) || (string.equals("")))
	  {
	    client.setStatus("Error, I need to have a name of an object to work with.");
	    return;
	  }

	if ((currentObject != null) && (string.equals(currentObject.getLabel())))
	  {
	    //This was set from the listbox, and hasn't been changed.
	    
	    invid = (Invid)currentObject.getObject();
	    close(true);
	  }
	else
	  {
	    if (list == null)
	      {
		list = new JList();
		list.setBorder(client.lineBorder);
		list.setModel(new DefaultListModel());
		list.addMouseListener(this);
	      }
	    else
	      {
		((DefaultListModel)list.getModel()).clear();
	      }

	    if (pane == null)
	      {
		pane = new JScrollPane(list);
		pane.setBorder(new TitledBorder("Matching objects"));
	      }

	    if (type == null)
	      {
		System.out.println("edit object type = null");
	      }
	    
	    listHandle lh = (listHandle) type.getSelectedItem();
	    Short baseID = (Short) lh.getObject();
	    
	    if (debug)
	      {
		System.out.println("BaseID = " + baseID + ", string = " + string);
	      }
	    
	    // First see if this exactly matches something, then do the STARTSWITH stuff

	    QueryDataNode node = new QueryDataNode(QueryDataNode.EQUALS, string);  
	    QueryResult edit_query = null;
	    Vector edit_invids = null;

	    try
	      {
		if (debug) 
		  {
		    System.out.println("Trying exact match...");
		  }

		client.setStatus("Searching for object named " + string);

		edit_query = client.session.query(new Query(baseID.shortValue(), node, editableOnly));

		if (edit_query != null)
		  {
		    edit_invids = edit_query.getListHandles();

		    if (debug) 
		      {
			System.out.println("edit_invids: " + edit_invids.size());
		      }
		  }
		
		if ((edit_invids != null ) && (edit_invids.size() == 1))
		  {
		    if (debug)
		      {
			System.out.println("Found it, exact match");
		      }

		    invid = (Invid)((listHandle)edit_invids.elementAt(0)).getObject();
		    close(true);
	    
		  }
		else
		  {
		    if (debug) 
		      {
			System.out.println("Looking for Startswith...");
		      }

		    client.setStatus("Searching for objects beginning with " + string);

		    node = new QueryDataNode(QueryDataNode.STARTSWITH, string);  
		    edit_query = null;
		    
		    edit_query = client.session.query(new Query(baseID.shortValue(), node, editableOnly));
		    
		    edit_invids = edit_query.getListHandles();
		    
		    if (edit_invids.size() == 1)
		      {
			invid = (Invid)((listHandle)edit_invids.elementAt(0)).getObject();
			close(true);
		      }
		    else if (edit_invids.size() == 0)
		      {
			client.showErrorMessage("Error finding object",
						editableOnly ?
						"No editable object starts with that string." :
						"No viewable object starts with that string.");
			return;
		      }
		    else
		      {
			(new VecQuickSort(edit_invids, 
					  new arlut.csd.Util.Compare() {
			  public int compare(Object a, Object b) 
			    {
			      listHandle aF, bF;
			      
			      aF = (listHandle) a;
			      bF = (listHandle) b;
			      int comp = 0;
			      
			      comp =  aF.toString().compareTo(bF.toString());
			      
			      if (comp < 0)
				{
				  return -1;
				}
			      else if (comp > 0)
				{ 
				  return 1;
				} 
			      else
				{ 
				  return 0;
				}
			    }
			})).sort();
			
			DefaultListModel model = (DefaultListModel)list.getModel();

			for (int i = 0; i < edit_invids.size(); i++)
			  {
			    model.addElement(edit_invids.elementAt(i));
			  }
			
			gbc.gridx = 0;
			gbc.gridy = 3;
			gbc.gridwidth = GridBagConstraints.REMAINDER;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbl.setConstraints(pane, gbc);
			
			middle.add(pane);
			pack();
		      }
		  }
	      }
	    catch (java.rmi.RemoteException rx)
	      {
		throw new RuntimeException("Could not get query: " + rx);
	      }
	  }
      }
    else if (e.getActionCommand().equals("Nevermind finding this object"))
      {
	close(false);
      }
  }

  public void mouseClicked(MouseEvent e)
  {
    currentObject = (listHandle) list.getSelectedValue();
    
    if ((e.getWhen() - lastClick < 500)  && (currentObject == lastObject))
      {
	invid = (Invid)((listHandle)currentObject).getObject();
	close(true);
      }
    else
      {
	text.setText(currentObject.toString());
      }

    lastClick = e.getWhen();
    lastObject = currentObject;

  }

  public void mousePressed(MouseEvent e) {}
  public void mouseReleased(MouseEvent e) {}
  public void mouseEntered(MouseEvent e) {}
  public void mouseExited(MouseEvent e) {}

}
