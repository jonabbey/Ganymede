/*

   ownerPanel.java

   The individual frames in the windowPanel.
   
   Created: 9 September 1997

   Module By: Michael Mulvaney

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996 - 2005
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

package arlut.csd.ganymede.client;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Insets;
import java.rmi.RemoteException;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import arlut.csd.JDataComponent.JAddValueObject;
import arlut.csd.JDataComponent.JAddVectorValueObject;
import arlut.csd.JDataComponent.JErrorValueObject;
import arlut.csd.JDataComponent.JDeleteValueObject;
import arlut.csd.JDataComponent.JDeleteVectorValueObject;
import arlut.csd.JDataComponent.JParameterValueObject;
import arlut.csd.JDataComponent.JValueObject;
import arlut.csd.JDataComponent.JsetValueCallback;
import arlut.csd.JDataComponent.StringSelector;
import arlut.csd.JDataComponent.listHandle;
import arlut.csd.Util.TranslationService;
import arlut.csd.ganymede.common.Invid;
import arlut.csd.ganymede.common.QueryResult;
import arlut.csd.ganymede.common.ReturnVal;
import arlut.csd.ganymede.rmi.invid_field;

/**
 * GUI panel for displaying the list of owners for a given object in
 * the client.  This panel is created in association with the "Owners"
 * tab in framePanel.
 *
 * @version $Id$
 * @author Mike Mulvaney
 */

public class ownerPanel extends JPanel implements JsetValueCallback {

  /**
   * TranslationService object for handling string localization in the
   * Ganymede client.
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.client.ownerPanel");

  boolean debug = false;

  /**
   * If true, we'll allow attempts to edit the owner list for this object.
   */

  boolean
    editable;

  /**
   * Reference to an object's owners field on the server.
   */

  invid_field
    field;

  /**
   * Object window we're contained in.
   */

  framePanel
   fp;

  /**
   * Reference to the client's gclient object, used for utility code.
   */

  gclient
    gc;

  /**
   * Panel to hold our working guy image while we download information from
   * the server.
   */

  JPanel
    holdOnPanel;

  /**
   * The actual string selector
   */

  StringSelector ownerList;

  /* -- */

  public ownerPanel(invid_field field, boolean editable, framePanel fp)
  {
    this.editable = editable;
    this.field = field;
    this.fp = fp;

    gc = fp.wp.gc;

    if (!debug)
      {
	debug = gc.debug;
      }

    if (debug)
      {
	System.err.println("Adding ownerPanel");
      }

    setLayout(new BorderLayout());

    setBorder(new EmptyBorder(new Insets(5,5,5,5)));

    holdOnPanel = new JPanel();

    // "Loading ownerPanel, please wait."
    holdOnPanel.add(new JLabel(ts.l("init.loading_label"),
			       new ImageIcon(fp.getWaitImage()),
			       SwingConstants.CENTER));
    
    add("Center", holdOnPanel);
    invalidate();
    fp.validate();

    if (field == null)
      {
	// we'll only get this if we are view-only and there are no owners
	// for this object registered.. in this case, the object is
	// effectively owned by supergash.

	if (debug)
	  {
	    System.err.println("ownerPanel: field is null, there is no owner for this object.");
	  }

	// "No owners assigned.  Supergash-level admins share ownership of this object."
	JLabel l = new JLabel(ts.l("init.no_owner_label"),
			      SwingConstants.CENTER);

	remove(holdOnPanel);
	add("Center", l);
      }
    else
      {
	if (debug)
	  {
	    System.err.println("ownerPanel: field is not null, creating invid selector.");
	  }

	try
	  {
	    ownerList = createInvidSelector(field);
	    ownerList.setBorder(new LineBorder(Color.black));
	    remove(holdOnPanel);
	    add("Center", ownerList);
	  }
	catch (Exception rx)
	  {
	    gc.processExceptionRethrow(rx);
	  }
      }

    invalidate();
    fp.validate();
  }

  private StringSelector createInvidSelector(invid_field field) throws RemoteException
  {
    Vector
      currentOwners = null,
      availableOwners = null;

    objectList
      list;

    /* -- */

    if (debug)
      {
	System.err.println("Adding StringSelector, its a vector of invids!");
      }

    if (editable)
      {
	QueryResult choices = field.choices(false);

	if (choices != null)
	  {
	    list = new objectList(choices);
		
	    availableOwners = list.getListHandles(false);
	  }
      }

    currentOwners = field.encodedValues().getListHandles();

    // availableOwners might be null
    // if editable is false, availableOwners will be null

    JPopupMenu invidTablePopup = new JPopupMenu();

    // "View object"
    JMenuItem viewObj = new JMenuItem(ts.l("createInvidSelector.view_popup"));
    viewObj.setActionCommand("View object");

    // "Edit object"
    JMenuItem editObj = new JMenuItem(ts.l("createInvidSelector.edit_popup"));
    editObj.setActionCommand("Edit object");
    invidTablePopup.add(viewObj);
    invidTablePopup.add(editObj);

    JPopupMenu invidTablePopup2 = new JPopupMenu();

    // "View object"
    JMenuItem viewObj2 = new JMenuItem(ts.l("createInvidSelector.view_popup"));
    viewObj2.setActionCommand("View object");

    // "Edit object"
    JMenuItem editObj2 = new JMenuItem(ts.l("createInvidSelector.edit_popup"));
    editObj2.setActionCommand("Edit object");
    invidTablePopup2.add(viewObj2);
    invidTablePopup2.add(editObj2);

    // We don't want the supergash owner group to show up anywhere,
    // because everything is owned by supergash.

    if (debug)
      {
	System.err.println("ownerPanel: Taking out supergash");
      }

    if (availableOwners != null)
      {
	Invid supergash = Invid.createInvid((short)0, 1); // This is supergash

	for (int i = 0; i < availableOwners.size(); i++)
	  {
	    listHandle l = (listHandle)availableOwners.elementAt(i);

	    if (supergash.equals(l.getObject()))
	      {
		availableOwners.removeElementAt(i);
		break;
	      }
	  }
      }

    if (debug)
      {
	System.err.println("ownerPanel: creating string selector");
      }

    StringSelector ss = new StringSelector(this, editable, true, true);
    
    ss.setCellWidth(0);

    // "Owners"
    // "Owner Groups"
    ss.setTitles(ts.l("createInvidSelector.column_title1"),
		 ts.l("createInvidSelector.column_title2"));

    ss.setPopups(invidTablePopup, invidTablePopup2);
    ss.update(availableOwners, true, null, currentOwners, true, null);
    ss.setCallback(this);

    return ss;
  }

  /**
   * Updates the contents of a vector {@link
   * arlut.csd.ganymede.rmi.invid_field invid_field} value selector
   * against the current contents of the field on the server.
   */

  public void updateInvidStringSelector() throws RemoteException
  {
    Vector available = null;
    Vector chosen = null;

    /* -- */

    // Only editable fields have available vectors

    if (ownerList.isEditable())
      {
	QueryResult choicesV = field.choices(false);
	    
	// if we got a null result, assume we have no choices
	// otherwise, we're going to cache this result
	    
	if (choicesV == null)
	  {
	    available = new Vector();
	  }
	else
	  {
	    available = choicesV.getListHandles();
	  }

	// hide the supergash object choice

	if (available != null)
	  {
	    Invid supergash = Invid.createInvid((short)0, 1); // This is supergash
	    
	    for (int i = 0; i < available.size(); i++)
	      {
		listHandle l = (listHandle)available.elementAt(i);
		
		if (supergash.equals(l.getObject()))
		  {
		    available.removeElementAt(i);
		    break;
		  }
	      }
	  }
      }
    
    QueryResult res = field.encodedValues();

    if (res != null)
      {
	chosen = res.getListHandles();
      }

    try
      {
	ownerList.update(available, true, null, chosen, false, null);
      }
    catch (Exception e)
      {
	throw new RuntimeException(e.getMessage());
      }
  }

  public boolean setValuePerformed(JValueObject o)
  {
    ReturnVal retVal;
    boolean succeeded = false;

    /* -- */

    if (!(o.getSource() instanceof StringSelector))
      {
	System.err.println("Where did this setValuePerformed come from?");
	return false;
      }

    if (o instanceof JErrorValueObject)
      {
	fp.getgclient().setStatus((String)o.getValue());
      }
    else if (o instanceof JParameterValueObject)
      {  // From the popup menu
	
	JParameterValueObject v = (JParameterValueObject) o; // because this code was originally used with a v
	String command = (String)v.getParameter();
	  
	if (command.equals("Edit object"))
	  {
	    if (debug)
	      {
		System.err.println("Edit object: " + v.getValue());
	      }
		
	    if (v.getValue() instanceof listHandle)
	      {
		Invid invid = (Invid)((listHandle)v.getValue()).getObject();
		    
		gc.editObject(invid);
	      }
	    else if (v.getValue() instanceof Invid)
	      {
		if (debug)
		  {
		    System.err.println("It's an invid!");
		  }
		    
		Invid invid = (Invid)v.getValue();
		    
		gc.editObject(invid);
	      }
		
	    retVal = null;
	  }
	else if (command.equals("View object"))
	  {
	    if (debug)
	      {
		System.err.println("View object: " + v.getValue());
	      }
		
	    if (v.getValue() instanceof Invid)
	      {
		Invid invid = (Invid)v.getValue();
		    
		gc.viewObject(invid);
	      }
		
	    retVal = null;
	  }
	else
	  {
	    System.err.println("Unknown action command from popup: " + command);
	  }
      } // end of popup processing, now it's just an add or remove kind of thing.
    else
      {
	Invid invid = null;	// have to init for javac

	if (o.getValue() instanceof Invid)
	  {
	    invid = (Invid) o.getValue();
	  }
	
	try
	  {
	    if (o instanceof JAddValueObject)
	      {
		retVal = field.addElement(invid);

		if (retVal != null)
		  {
		    gc.handleReturnVal(retVal);
		  }

		succeeded = (retVal == null) ? true : retVal.didSucceed();
	      }
	    else if (o instanceof JAddVectorValueObject)
	      {
		retVal = field.addElements((Vector) o.getValue());

		if (retVal != null)
		  {
		    gc.handleReturnVal(retVal);
		  }

		succeeded = (retVal == null) ? true : retVal.didSucceed();
	      }
	    else if (o instanceof JDeleteValueObject)
	      {
		retVal = field.deleteElement(invid);

		if (retVal != null)
		  {
		    gc.handleReturnVal(retVal);
		  }

		succeeded = (retVal == null) ? true : retVal.didSucceed();
	      }
	    else if (o instanceof JDeleteVectorValueObject)
	      {
		retVal = field.deleteElements((Vector) o.getValue());

		if (retVal != null)
		  {
		    gc.handleReturnVal(retVal);
		  }

		succeeded = (retVal == null) ? true : retVal.didSucceed();
	      }
	  }
	catch (Exception rx)
	  {
	    gc.processExceptionRethrow(rx);
	  }
      }
    
    if (succeeded)
      {
	fp.getgclient().somethingChanged();
      }
    
    return succeeded;
  }

  public void dispose()
  {
    field = null;
    fp = null;
    gc = null;
    removeAll();
    holdOnPanel = null;
  }
}
