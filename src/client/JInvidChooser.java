
/*

   JInvidChooser.java

   Like a JComboBox, but just for Invid's.  It has a couple of pretty
   buttons on the sides.
   
   Created: Before May 7, 1998
   Release: $Name:  $
   Version: $Revision: 1.19 $
   Last Mod Date: $Date: 1999/03/23 06:20:30 $
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

import arlut.csd.ganymede.*;
import arlut.csd.JDataComponent.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.util.Vector;


/*------------------------------------------------------------------------------
                                                                           class
                                                                   JInvidChooser

------------------------------------------------------------------------------*/

/**
 * A GUI component for choosing an Invid for a scalar invid_field.  
 */

public class JInvidChooser extends JPanelCombo implements ActionListener, ItemListener {

  private final static boolean debug = false;

  // ---

  JButton
    view,
    create;

  containerPanel
    cp;
  
  private short 
    type;

  private boolean
    removedNone = false,
    allowNone = true;

  private listHandle
    noneHandle = new listHandle("<none>", null);

  /* -- */

  /**
   *
   * @param parent The general or embedded object panel that contains us
   * @param objectType object type number, used to support creating a new
   * object by the use of the 'new' button if enabled.
   * @param showNew if true, the JInvidChooser will include a 'new' button
   * to enable creation of a new object of the type referenced by this JInvidChooser.
   */

  public JInvidChooser(containerPanel parent, short objectType, boolean showNew)
  {
    this(null, parent, objectType, showNew);
  }

  /**
   * @param objects A vector of {@link arlut.csd.JDataComponent.listHandle listHandle}
   * objects representing labeled Invid choices for the user to choose among.
   * @param parent The general or embedded object panel that contains us
   * @param objectType object type number, used to support creating a new
   * object by the use of the 'new' button if enabled.
   * @param showNew if true, the JInvidChooser will include a 'new' button
   * to enable creation of a new object of the type referenced by this JInvidChooser.
   */

  public JInvidChooser(Vector objects, containerPanel parent, short objectType, boolean showNew)
  {
    super(objects);

    getCombo().addItemListener(this);

    cp = parent;
    type = objectType;

    JPanel buttonPanel = new JPanel();
    buttonPanel.setLayout(new BorderLayout());

    view = new JButton("View");
    view.addActionListener(this);

    if (getSelectedInvid() == null)
      {
	view.setEnabled(false);
      }

    // If the objectType is -1 or less, we have no information about
    // what type of object can be pointed to by this JInvidChooser, so
    // we certainly can't offer to create a new one for the user.

    if (showNew && objectType > -1) 
      {
	create = new JButton("New");
	create.addActionListener(this);
      }

    buttonPanel.add("West", view);

    if (create != null)
      {
	buttonPanel.add("East", create);
      }

    // JPanelCombo already added the combo to the west.

    add("East", buttonPanel);
  }

  public Invid getSelectedInvid()
  {
    listHandle lh = (listHandle) getSelectedItem();

    if (lh == null)
      {
	return null;
      }

    return (Invid) lh.getObject();
  }

  /**
   *
   * <p>Set the allowNone bit.</p>
   *
   * <p>If allowNone is true, then &lt;none&gt; will remain as a choice in the
   * chooser.  If it is false, &lt;none&gt; will only be included in the
   * beginning if nothing is set; it will be removed as soon as
   * anything is chosen.</p>
   *
   */

  public void setAllowNone(boolean allow)
  {
    if (debug)
      {
	System.out.println("JInvidChooser: setAllowNone(" + allow +")");
      }

    // If we used to allow, but now we don't, we need to take out the
    // noneHandle if it is not selected.

    if (allowNone && (!allow) && (!removedNone))
      {
	Object item = getCombo().getSelectedItem();

	if ((item != null) && (!item.equals(noneHandle)))
	  {
	    if (debug)
	      {
		System.out.println("taking out <none>");
	      }

	    try
	      {
		getCombo().removeItem(noneHandle);
		removedNone = true;

		if (debug)
		  {
		    System.out.println("+setting removedNone to true");
		  }
	      }
	    catch (IllegalArgumentException ia)
	      {
		// none handle wasn't in there...
		removedNone = false;
	      }
	  }
	else if (debug)
	  {
	    System.out.println("<none> is selected, I will wait.");
	  }
      }

    // Now if we are allowing none, but we weren't before, and we took
    // the none handle out, we have to put it back in

    if (removedNone && allow && !allowNone)
      {
	boolean found = false;

	for (int i = 0; i < getCombo().getItemCount(); i++)
	  {
	    if (getCombo().getItemAt(i).equals(noneHandle))
	      {
		found = true;
		break;
	      }
	  }

	if (!found)
	  {
	    if (debug)
	      {
		System.out.println("Putting none back in.");
	      }

	    getCombo().addItem(noneHandle);
	  }

	removedNone = false;

	if (debug)
	  {
	    System.out.println("+setting removedNone to false");
	  }
      }

    allowNone = allow;
  }

  /**
   *
   * <p>Get the allowNone bit.</p>
   *
   * <p>If allowNone is true, then &lt;none&gt; will remain as a choice in the
   * chooser.  If it is false, &lt;none&gt; will only be included in the
   * beginning if nothing is set; it will be removed as soon as
   * anything is chosen.</p>
   */

  public boolean isAllowNone()
  {
    return allowNone;
  }

  /**
   *
   * ItemListener method
   *
   */

  public void itemStateChanged(ItemEvent e)
  {
    if (e.getStateChange() != ItemEvent.SELECTED)
      {
	return;
      }

    view.setEnabled(getSelectedInvid() != null);
  }

  /**
   *
   * ActionListener method
   *
   */

  public void actionPerformed(ActionEvent e)
  {
    if (e.getSource() == view)
      {
	listHandle lh = (listHandle) getSelectedItem();

	if (lh != null)
	  {
	    Invid invid = (Invid) lh.getObject();
	
	    if (invid == null)
	      {
		showErrorMessage("You don't have permission to view that object.");
	      }
	    else
	      {
		cp.gc.viewObject(invid);
	      }
	  }
      }
    else if (e.getSource() == create)
      {
	db_object object = null;
	
	try
	  {
	    object = cp.gc.createObject(type, false);

	    Invid invid = object.getInvid();
	    listHandle lh = new listHandle("New Object", invid);
	    invid_field field = (invid_field)cp.objectHash.get(this);
	    if (field == null)
	      {
		showErrorMessage("I can't create a new object, because I can't find the invid_field for this chooser.");
		return;
	      }
	    getCombo().addItem(lh);

	    field.setValue(invid);
	    getCombo().setSelectedItem(lh);

	  }
	catch (NullPointerException nx)
	  {
	    showErrorMessage("Got that pesky null pointer exception.  Permissions problem?");
	    return;
	  }
	catch (RuntimeException re)
	  {
	    showErrorMessage("Something when wrong creating the new object. " +
			     "Perhaps you don't have the permission to creat objects of that type.");
	    return;
	  }
	catch (java.rmi.RemoteException rx)
	  {
	    throw new RuntimeException("I can't add this object in to the JComboBox: " + rx);
	  }

	if (object != null)
	  {
	    cp.gc.showNewlyCreatedObject(object, null, new Short(type));
	  }
      }
  }
  
  private final void  showErrorMessage(String message) {
    showErrorMessage("Error", message);
  }

  private final void  showErrorMessage(String title, String message) {
    cp.getgclient().showErrorMessage(title,message);
  }
}
