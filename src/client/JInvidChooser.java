/*

   JInvidChooser.java

   A fancy custom JComboBox thing for Scalar Invid fields.
   
   Created: 26 October 1999
   Release: $Name:  $
   Version: $Revision: 1.22 $
   Last Mod Date: $Date: 1999/10/27 06:08:55 $
   Module By: Michael Mulvaney, Jonathan Abbey

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
 * <p>A GUI component for choosing an Invid for a scalar invid_field.</p>
 *
 * @version $Revision: 1.22 $ $Date: 1999/10/27 06:08:55 $ $Name:  $
 * @author Jonathan Abbey
 */

public class JInvidChooser extends JPanelCombo implements ActionListener, ItemListener {

  private final static boolean debug = false;

  // ---

  JButton
    view;

  containerPanel
    cp;
  
  private short 
    type;

  private boolean
    removedNone = false,
    allowNone = true;

  private listHandle
    noneHandle = new listHandle("<none>", null);

  JInvidChooserFieldEditor editor;

  /* -- */

  /**
   *
   * @param parent The general or embedded object panel that contains us
   * @param objectType object type number, used to support creating a new
   * object by the use of the 'new' button if enabled.
   */

  public JInvidChooser(containerPanel parent, short objectType)
  {
    this(null, parent, objectType);
  }

  /**
   * @param objects A vector of {@link arlut.csd.JDataComponent.listHandle listHandle}
   * objects representing labeled Invid choices for the user to choose among.
   * @param parent The general or embedded object panel that contains us
   * @param objectType object type number, used to support creating a new
   * object by the use of the 'new' button if enabled.
   */

  public JInvidChooser(Vector objects, containerPanel parent, short objectType)
  {
    super(objects);

    cp = parent;
    type = objectType;

    JPanel buttonPanel = new JPanel();
    buttonPanel.setLayout(new BorderLayout());

    view = new JButton("View");
    view.addActionListener(this);

    editor = new JInvidChooserFieldEditor(this);
    getCombo().setEditor(editor);
    getCombo().setEditable(true);

    getCombo().addItemListener(this);

    if (getSelectedInvid() == null)
      {
	view.setEnabled(false);
      }

    buttonPanel.add("West", view);

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

    if (editor != null && editor.theField != null)
      {
	if (!lh.toString().equals(editor.theField.getText()))
	  {
	    System.err.println("JInvidChooser: " + lh.toString() + " does not equal " + 
			       editor.theField.getText());
	    return null;
	  }
      }

    return (Invid) lh.getObject();
  }

  /**
   * <p>Set the allowNone bit.</p>
   *
   * <p>If allowNone is true, then &lt;none&gt; will remain as a choice in the
   * chooser.  If it is false, &lt;none&gt; will only be included in the
   * beginning if nothing is set; it will be removed as soon as
   * anything is chosen.</p>
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
    // keep non selection events to ourselves

    if (e.getStateChange() != ItemEvent.SELECTED)
      {
	return;
      }
    
    if (debug)
      {
	System.err.println("JInvidChooser.itemStateChanged(" + e.toString() + ")");
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
  }
  
  private final void  showErrorMessage(String message) {
    showErrorMessage("Error", message);
  }

  private final void  showErrorMessage(String title, String message) {
    cp.getgclient().showErrorMessage(title,message);
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                        JInvidChooserFieldEditor

------------------------------------------------------------------------------*/

/**
 * <p>A combobox editor class to provide intelligent keyboard handling for
 * the {@link arlut.csd.ganymede.client.JInvidChooser JInvidChooser} scalar
 * invid field gui component.</p>
 */

class JInvidChooserFieldEditor extends KeyAdapter implements ComboBoxEditor, ActionListener {

  static final boolean debug = false;

  // ---

  JTextField theField;
  Object curItem;
  JComboBox box;
  JInvidChooser chooser;
  Vector actionListeners = new Vector();
  String lastGoodString = null;
  boolean lastGoodMatched = false;
  int lastGoodIndex = -1;

  /* -- */

  public JInvidChooserFieldEditor(JInvidChooser chooser)
  {
    this.chooser = chooser;
    this.box = chooser.getCombo();
    theField = new JTextField();
    theField.addKeyListener(this);
    theField.addActionListener(this);
  }

  public void setItem(Object anObject)
  {
    curItem = anObject;

    if (curItem != null)
      {
	String str;

	str = curItem.toString();
	theField.setText(str);
	lastGoodString = str;
	lastGoodMatched = true;
      }
    else
      {
	theField.setText("");
	lastGoodString = "";
	lastGoodMatched = false;
      }
  }

  public Component getEditorComponent()
  {
    return theField;
  }

  public Object getItem()
  {
    return box.getSelectedItem();
  }

  public void selectAll()
  {
    theField.selectAll();
  }

  public void addActionListener(ActionListener l)
  {
    actionListeners.addElement(l);
  }

  public void removeActionListener(ActionListener l)
  {
    actionListeners.removeElement(l);
  }

  /**
   * <p>Tap into the text field's key release to see if
   * we can complete the user's selection.  Note that
   * we are doing this without synchronizing on the text
   * field's own user interface.. to do this properly, we might
   * ought to be doing this with a document model on the text
   * field, but this works ok.  Since we're keying on key release,
   * we can expect to be called after the text field has processed
   * the key press.</p>
   */

  public void keyReleased(KeyEvent ke)
  {
    int curLen;
    String curVal;
    JButton viewButton = chooser.view;

    /* -- */

    curVal = theField.getText();
    
    if (curVal != null)
      {
	curLen = curVal.length();
      }
    else
      {
	curLen = 0;
      }

    // ignore arrow keys, delete, shift

    int keyCode = ke.getKeyCode();

    switch (keyCode)
      {
      case KeyEvent.VK_UP:
      case KeyEvent.VK_DOWN:
      case KeyEvent.VK_LEFT:
      case KeyEvent.VK_RIGHT:
      case KeyEvent.VK_SHIFT:

      case KeyEvent.VK_DELETE:
      case KeyEvent.VK_BACK_SPACE:
	viewButton.setEnabled(false);
	return;
      }

    if (curLen > 0)
      {
	String item;
	int max = box.getItemCount();

	int matching = 0;
	String matchingItem = null;
	int matchingIndex = -1;
	
	for (int i = 0; i < max; i++)
	  {
	    item = box.getItemAt(i).toString();

	    if (item.equals(curVal))
	      {
		// found it

		lastGoodString = curVal;
		lastGoodMatched = true;
		lastGoodIndex = i;

		chooser.cp.gc.setWaitCursor();
	    
		try
		  {
		    box.setSelectedIndex(lastGoodIndex);
		  }
		finally
		  {
		    chooser.cp.gc.setNormalCursor();
		  }

		viewButton.setEnabled(true);

		return;
	      }
	    else if (item.startsWith(curVal))
	      {
		matching++;
		matchingItem = item;
		matchingIndex = i;
		lastGoodString = curVal;

		lastGoodIndex = -1;    // nothing definitively selected
	      }
	  }

	if (matching == 1)
	  {
	    lastGoodIndex = matchingIndex;
	    lastGoodMatched = true;

	    setItem(matchingItem); // will set lastGoodString

	    chooser.cp.gc.setWaitCursor();
	    
	    try
	      {
		box.setSelectedIndex(lastGoodIndex);
	      }
	    finally
	      {
		chooser.cp.gc.setNormalCursor();
	      }

	    theField.select(curLen, matchingItem.length());

	    viewButton.setEnabled(true);

	    return;
	  }
	else if (matching == 0)		       // no match, don't let them have that char
	  {
	    // this is really kind of weak, since we're not actually
	    // rejecting this with a document model, but it seems to
	    // work..

	    Toolkit.getDefaultToolkit().beep();

	    if (lastGoodMatched)
	      {
		chooser.cp.gc.setWaitCursor();

		try
		  {
		    box.setSelectedIndex(lastGoodIndex);
		  }
		finally
		  {
		    chooser.cp.gc.setNormalCursor();
		  }

		setItem(box.getSelectedItem());
		viewButton.setEnabled(true);
	      }
	    else
	      {
		theField.setText(lastGoodString);

		viewButton.setEnabled(false);
	      }
	  }
	else
	  {
	    // too many matching, we don't yet have a unique prefix

	    lastGoodMatched = false;
	    lastGoodIndex = -1;
	  }
      }
  }

  /**
   * <p>Handle the user hitting return in the editable area.. if they hit return
   * without a reasonable value, revert the combo.</p>
   */

  public void actionPerformed(ActionEvent e)
  {
    String value = theField.getText();
    String item;

    int max = box.getItemCount();

    boolean found = false;

    for (int i = 0; !found && i < max; i++)
      {
	item = box.getItemAt(i).toString();

	if (item.equals(value))
	  {
	    found = true;
	    lastGoodIndex = i;

	    chooser.cp.gc.setWaitCursor();

	    try
	      {
		box.setSelectedIndex(i);   // this will cause the combo box to send an update
	      }
	    finally
	      {
		chooser.cp.gc.setNormalCursor();
	      }
	  }
      }

    if (!found)
      {
	if (lastGoodMatched)
	  {
	    chooser.cp.gc.setWaitCursor();

	    try
	      {
		box.setSelectedIndex(lastGoodIndex);
	      }
	    finally
	      {
		chooser.cp.gc.setNormalCursor();
	      }

	    setItem(box.getSelectedItem());
	    chooser.view.setEnabled(true);
	  }
	else
	  {
	    theField.setText(lastGoodString);

	    chooser.view.setEnabled(false);
	  }
      }
  }
}

