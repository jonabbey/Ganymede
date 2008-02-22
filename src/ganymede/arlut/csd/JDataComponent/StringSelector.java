/*

   StringSelector.java

   A two list box for adding strings to lists.

   Created: 10 October 1997

   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   Last Mod Date: $Date$
   SVN URL: $HeadURL$

   Module By: Mike Mulvaney, Jonathan Abbey

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996-2008
   The University of Texas at Austin

   Contact information

   Web site: http://www.arlut.utexas.edu/gash2
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
   Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
   02111-1307, USA

*/

package arlut.csd.JDataComponent;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.rmi.RemoteException;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import arlut.csd.Util.Compare;
import arlut.csd.Util.TranslationService;
import arlut.csd.Util.VectorUtils;

/*------------------------------------------------------------------------------
                                                                           class
                                                                  StringSelector


------------------------------------------------------------------------------*/

/**
 * <p>A two-paneled GUI component for adding or removing strings
 * and/or labeled objects from a list, with an optional list of
 * available strings and/or objects to choose from.</p>
 *
 * <p>StringSelector consists of one or (optionally) two {@link
 * arlut.csd.JDataComponent.JstringListBox JstringListBox} panels and
 * allows the user to move values back and forth between the two
 * panels.  Pop-up menus can be attached to each panel, allowing the
 * user to command the client to view or edit objects referenced in
 * either panel.  Objects in both panels are sorted alphabetically by
 * label.</p>
 *
 * <p>The setCallback() method takes an object implementing the {@link
 * arlut.csd.JDataComponent.JsetValueCallback JsetValueCallback}
 * interface in order to provide live notification of changes
 * performed by the user.  The JsetValueCallback implementation is
 * given the opportunity to approve any change made by the user before
 * the GUI is updated to show the change.  The JsetValueCallback
 * interface is also used to pass pop-up menu commands to the
 * client.</p>
 *
 * @see JstringListBox
 * @see JsetValueCallback
 *
 * @version $Id$
 * @author Mike Mulvaney, Jonathan Abbey
 */

public class StringSelector extends JPanel implements ActionListener, JsetValueCallback {

  static final boolean debug = true;

  /**
   * TranslationService object for handling string localization in the
   * Ganymede system.
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.JDataComponent.StringSelector");

  // --

  JsetValueCallback
    my_callback;

  JButton
    add,
    remove;

  JstringListBox
    in, 
    out = null;

  JPanel
    inPanel = new JPanel(),
    outPanel = new JPanel();

  JButton
    inTitle = new JButton(),
    outTitle = new JButton();

  String
    org_in = ts.l("global.items_in"), // "Members"
    org_out = ts.l("global.items_out");	// "Available"

  JButton
    addCustom;

  JstringField 
    custom = null;

  Container
    parent;

  private boolean
    replacingValue = false,
    editable,
    canChoose,
    mustChoose;

  private int
    minRows = -1,
    maxRows = -1,
    currentRows = -1;

  /* -- */

  /**
   *
   * Fully specified Constructor for StringSelector
   *
   * @param parent AWT container that the StringSelector will be contained in.
   * @param editable If false, this string selector is for display only
   * @param canChoose Choice must be made from vector of choices
   * @param mustChoose Vector of choices is available
   *
   */

  public StringSelector(Container parent, boolean editable, boolean canChoose,
			boolean mustChoose)
  {
    if (debug)
      {
	System.err.println("-Adding new StringSelector-");
      }
    
    setBorder(new javax.swing.border.EtchedBorder());

    this.parent = parent;
    this.editable = editable;
    this.canChoose = canChoose;
    this.mustChoose = mustChoose;
    
    setLayout(new BorderLayout());

    // lists holds the outPanel and inPanel.

    GridBagLayout
      gbl = new GridBagLayout();

    GridBagConstraints
      gbc = new GridBagConstraints();

    JPanel lists = new JPanel();
    lists.setLayout(gbl);

    // Set up the inPanel, which holds the in list and button

    // JstringListBox does the sorting

    in = new JstringListBox();
    in.setCallback(this);

    this.currentRows = in.getVisibleRowCount();

    BevelBorder
      bborder = new BevelBorder(BevelBorder.RAISED);

    inPanel.setBorder(bborder);
    inPanel.setLayout(new BorderLayout());

    inPanel.add("Center", new JScrollPane(in));

    inTitle.setText(org_in.concat(" : 0"));
    //    inTitle.setHorizontalAlignment( SwingConstants.LEFT );
    //    inTitle.setMargin( new Insets(0,0,0,0) );
    inTitle.addActionListener(this);

    inPanel.add("North", inTitle);

    if (editable)
      {
	if (canChoose)
	  {
	    // "Remove >>"
	    remove = new JButton(ts.l("global.remove_and_remember_button"));
	  }
	else
	  {
	    // "Remove"
	    remove = new JButton(ts.l("global.remove_and_forget_button"));
	  }

	remove.setEnabled(false);
	remove.setOpaque(true);
	remove.setActionCommand("Remove");
	remove.addActionListener(this);
	inPanel.add("South", remove);
      }

    gbc.fill = gbc.BOTH;
    gbc.gridwidth = 1;
    gbc.weightx = 1.0;
    gbc.weighty = 1.0;
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbl.setConstraints(inPanel, gbc);

    lists.add(inPanel);

    // JstringListBox does the sorting

    if (editable && canChoose)
      {
	// Set up the outPanel.
	// If we need an out box, build it now.

	outTitle.setText(org_out.concat(": 0"));
	//	outTitle.setHorizontalAlignment( SwingConstants.LEFT );
	//	outTitle.setMargin( new Insets(0,0,0,0) );
	outTitle.addActionListener(this);

	out = new JstringListBox();
	out.setCallback(this);

	// "<< Add"
	add = new JButton(ts.l("global.add_choice_button"));
	add.setEnabled(false);
	add.setOpaque(true);
	add.setActionCommand("Add");
	add.addActionListener(this);
	
	outPanel.setBorder(bborder);
	outPanel.setLayout(new BorderLayout());
	outPanel.add("Center", new JScrollPane(out));
	outPanel.add("North", outTitle);
	outPanel.add("South", add);

	gbc.fill = gbc.BOTH;
	gbc.gridwidth = 1;
	gbc.weightx = 1.0;
	gbc.weighty = 1.0;
	gbc.gridx = 1;
	gbc.gridy = 0;
	gbl.setConstraints(outPanel, gbc);

	lists.add(outPanel);
      }

    add("Center", lists);

    if (editable)
      {
	custom = new JstringField();
	custom.setBorder(new EmptyBorder(new Insets(0,0,0,4)));
	custom.addActionListener(new ActionListener() 
				 {
				   public void actionPerformed(ActionEvent e)
				     {
				       addCustom.doClick();
				     }
				 });

	JPanel customP = new JPanel();
	customP.setLayout(new BorderLayout());
	customP.add("Center", custom);

	if (!(mustChoose && out == null))
	  {
	    // "Add"
	    addCustom = new JButton(ts.l("global.add_button"));
	    addCustom.setEnabled(false);
	    addCustom.setActionCommand("AddNewString");
	    addCustom.addActionListener(this);
	    customP.add("West", addCustom);

	    // we only want this add button to be active when the user
	    // has entered something in the text field.  Some users
	    // have been confused by the add button just sitting there
	    // active.

	    custom.getDocument().addDocumentListener(new DocumentListener()
						     {
						       public void changedUpdate(DocumentEvent x) {}
						       public void insertUpdate(DocumentEvent x) 
							 {
							   if (x.getDocument().getLength() > 0)
							     {
							       addCustom.setEnabled(true);
							     }
							 }
						       
						       public void removeUpdate(DocumentEvent x) 
							 {
							   if (x.getDocument().getLength() == 0)
							     {
							       addCustom.setEnabled(false);
							     }
							 }
						     });
	  }

	// if we know they can only type things from the list,
	// implement choice completion

	if (mustChoose)
	  {
	    custom.addKeyListener(new KeyAdapter()
				  {
				    public void keyReleased(KeyEvent ke)
				      {
					int curLen;
					String curVal;

					curVal = custom.getText();
    
					if (curVal != null)
					  {
					    curLen = curVal.length();
					  }
					else
					  {
					    curLen = 0;
					  }

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
					    return;
					  }

					if (curLen > 0)
					  {
					    listHandle item;

					    int matching = 0;
					    String matchingItem = null;

					    Enumeration en = out.model.elements();

					    while (en.hasMoreElements())
					      {
						item = (listHandle) en.nextElement();

						if (item.toString().equals(curVal))
						  {
						    // they've typed the full thing here, stop.

						    out.setSelectedLabel(matchingItem, true);
						    custom.setText(curVal);
						    return;
						  }
						else if (item.toString().startsWith(curVal))
						  {
						    matching++;
						    matchingItem = item.toString();
						  }
					      }

					    if (matching == 1)
					      {
						out.setSelectedLabel(matchingItem, true);
						custom.setText(matchingItem);
						custom.select(curLen, matchingItem.length());
						return;
					      }
					  }
				      }
				    });
	  }

	add("South", customP);
      }

    // provide a small default width

    setCellWidth(50);

    invalidate();
    parent.validate();

    if (debug)
      {
	System.err.println("Done creating ss");
      }
  }

  // Public methods ------------------------------------------------------------

  /**
   * <p>This method sets the width of the in and out rows.</p>
   *
   * @param width How many columns wide should each box be?  If <= 0, the
   * StringSelector will auto-size the columns
   */

  public void setCellWidth(int width)
  {
    in.setCellWidth(width);

    if (out != null)
      {
	out.setCellWidth(width);
      }

    invalidate();
    parent.validate();
  }

  /**
   * <p>This method sets the width of the in and out rows.</p>
   *
   * @param template A string to use as the required size of the cells
   * in the in and out boxes.
   */

  public void setCellWidth(String template)
  {
    in.setCellWidth(template);

    if (out != null)
      {
	out.setCellWidth(template);
      }

    invalidate();
    parent.validate();
  }

  /**
   * <p>This method sets the titles for the in and out boxes.  The
   * StringSelector will show these titles, followed by a colon and
   * the current count of elements in the in and/or out boxes.</p>
   */

  public void setTitles(String inString, String outString)
  {
    org_in = inString;
    org_out = outString;
  }

  /**
   * <p>This method attaches popup menus to the in box and out
   * box.</p>
   *
   * <p>The two popup menus must be distinct, so that they can be
   * attached to separate JstringListBoxes in the StringSelector,
   * without having each JstringListBox listening to the same popup
   * menu.</p>
   */

  public void setPopups(JPopupMenu inPopup, JPopupMenu outPopup)
  {
    if (inPopup != null && inPopup == outPopup)
      {
	throw new IllegalArgumentException("Need two different JPopupMenus");
      }

    in.registerPopupMenu(inPopup);

    if (out != null)
      {
	out.registerPopupMenu(outPopup);
      }
  }

  /**
   * <P>Returns true if this StringSelector is editable.</P>
   *
   * <P>Non-editable StringSelector's only have the chosen list.
   * Editable StringSelector's have both the chosen and available
   * lists.</P>
   */

  public boolean isEditable()
  {
    return editable;
  }

  /**
   * Update the StringSelector.
   *
   * @param available A Vector of {@link java.lang.String} or {@link
   * arlut.csd.JDataComponent.listHandle} objects that may be chosen by the user.
   * @param sortAvailable If true, the available strings or handles should be sorted
   * for presentation.
   * @param availComparator The {@link java.util.Comparator} object for evaluating
   * the sort order for items in the available Vector.
   * @param chosen A Vector of {@link java.lang.String} or {@link
   * arlut.csd.JDataComponent.listHandle} objects that have previously
   * been chosen for membership.
   * @param sortChosen If true, the previously chosen strings or handles should be sorted
   * for presentation.
   * @param chosenComparator The {@link java.util.Comparator} object for evaluating
   * the sort order for items in the previously chosen Vector.
   */

  public synchronized void update(Vector available, boolean sortAvailable, Comparator availComparator,
				  Vector chosen, boolean sortChosen, Comparator chosenComparator)
  {
    if (available == null)
      {
	if (out != null)
	  {
	    out.model.removeAllElements();
	  }
      }

    // If there is no out box, then we don't need to worry about available stuff

    if (out != null)
      {
	try
	  {
	    out.load(VectorUtils.difference(available,chosen), -1, sortAvailable, availComparator);
	  }
	catch (Exception e)
	  {
	    e.printStackTrace();
	    throw new RuntimeException(e);
	  }
      }

    try
      {
	in.load(chosen, -1, sortChosen, chosenComparator);
      }
    catch (Exception e)
      {
	e.printStackTrace();
	throw new RuntimeException(e);
      }

    updateTitles();
    recalcSize();
  }

  /**
   * This method is used to change the dynamically label of an object in this
   * StringSelector.
   */

  public synchronized void relabelObject(Object object, String newLabel)
  {
    in.relabelObject(object, newLabel);

    if (out != null)
      {
	out.relabelObject(object, newLabel);
      }
  }

  /**
   * Change the text on the add button.
   */

  public void setButtonText(String text)
  {
    if (addCustom == null)
      {
	return;
      }

    addCustom.setText(text);
    validate();
  }

  /**
   * This method sets the minimum number of rows this StringSelector
   * should display.  When set, this StringSelector will automatically
   * resize itself between the value of minRows and maxRows.
   */

  public void setMinimumRowCount(int numRows)
  {
    this.minRows = numRows;

    if (this.maxRows == -1)
      {
        setMaximumRowCount(8);  // default is 8
      }
  }

  /**
   * This method sets the maximum number of rows this StringSelector
   * should display.  When set, this StringSelector will automatically
   * resize itself between the value of minRows and maxRows.
   */

  public void setMaximumRowCount(int numRows)
  {
    this.maxRows = numRows;

    if (this.minRows == -1)
      {
        setMinimumRowCount(4);  // default is 4
      }
  }

  /**
   * This method adjusts the number of rows shown in this StringSelector.
   */

  public void setVisibleRowCount(int numRows)
  {
    this.setVisibleRowCount(numRows, true);
  }
    
  /**
   * This method adjusts the number of rows shown in this
   * StringSelector.  If refresh is true, the StringSelector will be
   * immediately resized.
   */

  public void setVisibleRowCount(int numRows, boolean refresh)
  {
    this.currentRows = numRows;

    in.setVisibleRowCount(this.currentRows);

    if (out != null)
      {
        out.setVisibleRowCount(this.currentRows);
      }

    if (refresh)
      {
        invalidate();
        parent.validate();
      }
  }

  /**
   * <p>Connects this StringSelector to an implementaton of the
   * {@link arlut.csd.JDataComponent.JsetValueCallback JsetValueCallback} interface
   * in order to provide live notification of changes performed by the user.  The
   * JsetValueCallback implementation is given the opportunity to approve any change
   * made by the user before the GUI is updated to show the change.  The JsetValueCallback
   * interface is also used to pass pop-up menu commands to the client.</p>
   *
   * <p>StringSelector uses the following subclasses of
   * {@link arlut.csd.JDataComponent.JValueObject JValueObject} to pass status updates to
   * the callback.
   *
   * <ul>
   * <li>{@link arlut.csd.JDataComponent.JParameterValueObject} Action from a PopupMenu.  The Parameter is the ActionCommand
   * string for the pop-up menu item selected, and the value is the object
   * (or string if no object defined) associated with the item selected when the pop-up menu was fired.</li>
   * <li>{@link arlut.csd.JDataComponent.JAddValueObject} Object has been added to the selected list.  Value is the object (or string) added.</li>
   * <li>{@link arlut.csd.JDataComponent.JDeleteValueObject} Object has been removed from chosen list.  Value is the object (or string) removed.</li>
   * <li>{@link arlut.csd.JDataComponent.JErrorValueObject} Something went wrong.  Value is the error message to be displayed to the user in whatever
   * fashion is appropriate.</li>
   * </ul>
   * </p>
   *
   * @see JsetValueCallback
   * @see JValueObject
   *
   */

  public void setCallback(JsetValueCallback parent)
  {
    my_callback = parent;
  }

  /**
   * <p>Returns a Vector of {@link arlut.csd.JDataComponent.listHandle listHandle}
   * objects corresponding to the currently selected members.</p>
   */

  public Vector getChosenHandles()
  {
    return in.getHandles();
  }

  /**
   * <p>Returns a Vector of Strings corresponding to the currently
   * selected members.</p> 
   */

  public Vector getChosenStrings()
  {
    Vector inVector = in.getHandles();
    Vector result = new Vector();

    if (inVector == null)
      {
	return result;
      }

    for (int i = 0; i < inVector.size(); i++)
      {
	listHandle handle = (listHandle) inVector.elementAt(i);

	result.addElement(handle.toString());
      }
    
    return result;
  }

  // ActionListener methods -------------------------------------------------

  /**
   *
   * This method handles events from the Add and Remove
   * buttons, and from hitting enter/loss of focus in the
   * custom JstringField.
   *
   */

  public void actionPerformed(ActionEvent e)
  {
    if (!editable)
      {
	return;
      }

    if (e.getSource() == inTitle)
      {
	in.setSelectionInterval( 0, in.getModel().getSize()-1 );

	if (out != null)
	  {
	    out.clearSelection();
	  }
      }

    if (e.getSource() == outTitle)
      {
	out.setSelectionInterval( 0, out.getModel().getSize()-1 );
	in.clearSelection();
      }

    if (e.getActionCommand().equals("Add"))
      {
	if (debug)
	  {
	    System.err.println("StringSelector: add Action");
	  }

	addItems();
      }
    else if (e.getActionCommand().equals("Remove"))
      {
	if (debug)
	  {
	    System.err.println("StringSelector: remove Action");
	  }

	removeItems();
      }
    else if (e.getActionCommand().equals("AddNewString"))
      {
	if (debug)
	  {
	    System.err.println("StringSelector: addNewString Action");
	  }

	addNewString();
      }
  }

  // JsetValueCallback methods -------------------------------------------------

  public boolean setValuePerformed(JValueObject o)
  {
    if (o.getSource() == custom)
      {
	if (!editable)
	  {
	    return false;
	  }

	addCustom.doClick();
	return true;
      }
    else if (o instanceof JParameterValueObject)  // from the popup menu
      {
	if (my_callback != null)
	  {
	    try
	      {
		my_callback.setValuePerformed(new JParameterValueObject(this,
									o.getIndex(),
									o.getValue(),
									o.getParameter()));
	      }
	    catch (java.rmi.RemoteException rx)
	      {
		System.err.println("could not setValuePerformed from StringSelector: " + rx);
	      }

	    return true;
	  }	
      }
    else if (o.getSource() == in)
      {
	if (!editable)
	  {
	    return false;
	  }

	if (o instanceof JInsertValueObject)
	  {
	    remove.doClick();
	    return true;
	  }
	else if (o instanceof JAddValueObject)		// selection
	  {
	    if (add != null)
	      {
		add.setEnabled(false);
	      }

	    if (remove != null)
	      {
		remove.setEnabled(true);
	      }

	    if (out != null)
	      {
		out.clearSelection();
	      }
	    
	    return true;
	  }
      }
    else if (o.getSource() == out)
      {
	if (o instanceof JInsertValueObject)
	  {
	    add.doClick();
	    return true;
	  }
	else if (o instanceof JAddValueObject)
	  {
	    add.setEnabled(true);
	    remove.setEnabled(false);
	    in.clearSelection();
	    custom.setText("");

	    return true;
	  }
      }
    else
      {	
	if (!editable)
	  {
	    return false;
	  }

	if (debug)
	  {
	    System.err.println("set value in stringSelector");
	  }
	
	System.err.println("Unknown object generated setValuePerformed in stringSelector.");
	
	return false;
      }

    return false;  // should never really get here.
  }

  // Private methods ------------------------------------------------------------

  /**
   * <p>This method moves one or more selected items from the out list to the in list.</p>
   */

  private void addItems()
  {
    boolean ok = false;
    Vector handles;

    /* -- */

    if (out == null)
      {
	System.err.println("Can't figure out the handle.  No out box to get it from.");
	return;
      }

    handles = out.getSelectedHandles();

    if (handles == null)
      {
	System.err.println("Error.. got addItem with outSelected == null");
	return;
      }
    
    if (handles.size() > 1)
      {
	if (my_callback != null)
	  {
	    Vector objVector = new Vector(handles.size());

	    for (int i = 0; i < handles.size(); i++)
	      {
		objVector.addElement(((listHandle) handles.elementAt(i)).getObject());
	      }

	    ok = false;		// if we get an exception, that's not okay

	    try
	      {
		ok = my_callback.setValuePerformed(new JAddVectorValueObject(this, objVector));
	      }
	    catch (RemoteException rx)
	      {
		throw new RuntimeException("Could not setValuePerformed: " + rx);
	      }
	  }
	else
	  {
	    ok = true;
	  }

	if (ok)
	  {
	    // if replacingValue was set, we'll have been refreshed by
	    // a callback from the setValuePerformed() call, above,
	    // before we get to here, so don't do anything to further
	    // mess with our state.

	    if (replacingValue)
	      {
		replacingValue = false;
		return;
	      }

	    in.clearSelection();

	    for (int i = 0; i < handles.size(); i++)
	      {
		putItemIn((listHandle)handles.elementAt(i));
	      }
	  }
	else
	  {
	    if (debug)
	      {
		System.err.println("setValuePerformed returned false");
	      }
	  }
      }
    else
      {
	if (my_callback != null)
	  {
	    ok = false;

	    try
	      {
		ok = my_callback.setValuePerformed(new JAddValueObject(this,
								       ((listHandle)handles.elementAt(0)).getObject()));
	      }
	    catch (RemoteException rx)
	      {
		throw new RuntimeException("Could not setValuePerformed: " + rx);
	      }
	  }
	else
	  {
	    ok = true;
	  }

	if (ok)
	  {
	    if (replacingValue)
	      {
		replacingValue = false;
		return;
	      }

	    in.clearSelection();

	    putItemIn((listHandle)handles.elementAt(0));
	  }
	else
	  {
	    if (debug)
	      {
		System.err.println("setValuePerformed returned false");
	      }
	  }
      }

    updateTitles();
    recalcSize();
    invalidate();
    parent.validate();
  }

  /**
   * <p>This method moves one or more selected items from the in list to the out list.</p>
   */

  private void removeItems()
  {
    Vector handles;
    boolean ok;

    /* -- */

    handles = in.getSelectedHandles();

    if (handles == null)
      {
	System.err.println("Error.. got removeItem with inSelected == null");
	return;
      }

    if (handles.size() > 1)
      {
	if (my_callback != null)
	  {
	    Vector objVector = new Vector(handles.size());

	    for (int i = 0; i < handles.size(); i++)
	      {
		objVector.addElement(((listHandle) handles.elementAt(i)).getObject());
	      }

	    ok = false;

	    try
	      {
		ok = my_callback.setValuePerformed(new JDeleteVectorValueObject(this, objVector));
	      }
	    catch (RemoteException rx)
	      {
		throw new RuntimeException("Could not setValuePerformed: " + rx);
	      }
	  }
	else
	  {
	    ok = true;
	  }

	if (ok)
	  {
	    if (replacingValue)
	      {
		replacingValue = false;
		return;
	      }

	    for (int i = 0; i < handles.size(); i++)
	      {
		takeItemOut((listHandle)handles.elementAt(i));
	      }
	  }
	else
	  {
	    if (debug)
	      {
		System.err.println("setValuePerformed returned false");
	      }
	  }
      }
    else
      {
	if (my_callback != null)
	  {
	    ok = false;

	    try
	      {
		ok = my_callback.setValuePerformed(new JDeleteValueObject(this, 
									  ((listHandle)handles.elementAt(0)).getObject()));
	      }
	    catch (RemoteException rx)
	      {
		throw new RuntimeException("Could not setValuePerformed: " + rx);
	      }
	  }
	else
	  {
	    ok = true;
	  }

	if (ok)
	  {
	    if (replacingValue)
	      {
		replacingValue = false;
		return;
	      }

	    takeItemOut((listHandle)handles.elementAt(0));
	  }
	else
	  {
	    if (debug)
	      {
		System.err.println("setValuePerformed returned false");
	      }
	  }
      }

    updateTitles();
    recalcSize();
    invalidate();
    parent.validate();
  }

  /**
   * <p>This method moves a single item from the outlist to the inlist.</p>
   *
   * <p>This method is the opposite of
   * {@link arlut.csd.JDataComponent.StringSelector#takeItemOut(arlut.csd.JDataComponent.listHandle) takeItemOut}.</p>
   *
   */

  private void putItemIn(listHandle item)
  {
    if (debug)
      {
	System.err.println("Add: " + item);
      }

    if (!editable)
      {
	return;
      }

    if (canChoose)
      {
	if (out != null)
	  {
	    out.removeItem(item);
	  }

	if (debug)
	  {
	    System.err.println("Adding handle");
	  }

	// We only want to put it in if it's not already there.
	// Sometimes this happens in Ganymede if we update a field
	// before we are changing.  It happens like this: the "add"
	// button is clicked.  Then the return value decides to update
	// this field, which loads the value in the in box.  Then it
	// returns true, and then the value is already in there.  So
	// if we add it again, we get two of them.  Got it?

	if (!in.containsItem(item))
	  {
	    in.addItem(item);
	  }

	if (debug)
	  {
	    System.err.println("Done Adding handle");
	  }
      }
    else
      {
	throw new RuntimeException("Can't add something from the out box to a non-canChoose StringSelector!");
      }
  }

  /**
   * <p>This method moves a single item from the outlist to the inlist.</p>
   *
   * <p>This method is the opposite of
   * {@link arlut.csd.JDataComponent.StringSelector#putItemIn(arlut.csd.JDataComponent.listHandle) putItemIn}.</p>
   */
  
  private void takeItemOut(listHandle item)
  {
    if (debug)
      {
	System.err.println("Remove." + item);
      }

    if (!editable)
      {
	return;
      }

    in.removeItem(item);

    // If the item is already in there, don't add it.

    if ((out != null)  &&  (! out.containsItem(item)))
      {
	out.addItem(item);
      }

    remove.setEnabled(false);

    recalcSize();

    in.invalidate();

    if (out != null)
      {
	out.invalidate();
      }

    invalidate();

    if (parent.getParent() != null)
      {
	parent.getParent().validate();
      }
    else
      {
	parent.validate();
      }
  }

  /**
   * <p>This method handles the processing to add an item entered in
   * the custom text entry box.</p>
   */

  private void addNewString()
  {
    String inputText = custom.getText();

    if (inputText.equals("") || in.containsLabel(inputText))
      {
	if (debug)
	  {
	    System.err.println("That one's already in there.  No soup for you!");
	  }

	return;
      }

    if (debug)
      {
        System.err.println("addNewString(\"" + inputText + "\")");
      }

    if (out != null && mustChoose) 
      {	    
	// Check to see if it is in there

	if (debug)
	  {
	    System.err.println("Checking to see if this is a viable option");
	  }
	    
	if (out.containsLabel(inputText)) 
	  {
	    out.setSelectedLabel(inputText);
	    listHandle handle = out.getSelectedHandle();
	    
	    boolean ok;
	    
	    if (my_callback != null)
	      {
		ok = false;
		
		try
		  {
		    ok = my_callback.setValuePerformed(new JAddValueObject(this, handle.getObject()));
		  }
		catch (RemoteException rx)
		  {
		    throw new RuntimeException("Could not setValuePerformed: " + rx);
		  }
	      }
	    else
	      {
		ok = true;
	      }
	    
	    if (ok)
	      {
		if (replacingValue)
		  {
		    custom.setText("");
		    replacingValue = false;
		    return;
		  }

		in.clearSelection();

		putItemIn(handle);
		custom.setText("");
	      }
	  }
	else  //It's not in the outbox.
	  {
	    if (my_callback != null)
	      {
		try
		  {
		    // "Sorry, you must enter strings from the list of
		    // available choices.  Please choose from the
		    // available list."

		    my_callback.setValuePerformed(new JErrorValueObject(this,
									ts.l("addNewString.bad_choice")));
		  }
		catch (RemoteException rx)
		  {
		    throw new RuntimeException("Could not tell parent what is wrong: " + rx);
		  }
	      }
	  }
      }
    else
      {
	// not mustChoose, so you can stick it in there.  But see,
	// I need to see if it's in there first, because if it is,
	// IF IT IS, then you have to move the String over.  HA!

	if ((out != null) && out.containsLabel(inputText))
	  {
	    out.setSelectedLabel(inputText);
	    listHandle handle = out.getSelectedHandle();
		
	    boolean ok;
		
	    if (my_callback != null)
	      {
		ok = false;

		try
		  {
		    ok = my_callback.setValuePerformed(new JAddValueObject(this, handle.getObject()));
		  }
		catch (RemoteException rx)
		  {
		    throw new RuntimeException("Could not setValuePerformed: " + rx);
		  }
	      }
	    else
	      {
		ok = true;
	      }

	    if (ok)
	      {
		if (replacingValue)
		  {
		    custom.setText("");
		    replacingValue = false;
		    return;
		  }

		in.clearSelection();

		putItemIn(handle);
		custom.setText("");
	      }	
	  }
	else 
	  {
	    // Not in the out box, send up the String as-is, with no attached data

            if (debug)
              {
                System.err.println("addNewString() -- stand alone");
              }
		
	    boolean ok;

	    if (my_callback != null)
	      {
		ok = false;

		try
		  {
		    ok = my_callback.setValuePerformed(new JAddValueObject(this, inputText));
		  }
		catch (RemoteException rx)
		  {
		    throw new RuntimeException("Could not setValuePerformed: " + rx);
		  }
	      }
	    else
	      {
		ok = true;
	      }
		
	    if (ok)
	      {
		if (replacingValue)
		  {
		    custom.setText("");
		    replacingValue = false;

                    if (debug)
                      {
                        System.err.println("addNewString() -- replacing");
                      }

		    return;
		  }

                if (debug)
                  {
                    System.err.println("addNewString() -- adding");
                  }

		in.clearSelection();
		putItemIn(new listHandle(inputText, inputText));
		custom.setText("");
	      }
	    else
	      {
		if (debug)
		  {
		    System.err.println("setValuePerformed returned false.");
		  }
	      }
	  }
	    
	validate();
      }

    updateTitles();
    recalcSize();
    in.invalidate();
    invalidate();
    validate();
  }

  /**
   * <p>This method handles updating the item counts in the in and out displays.</p>
   */

  private void updateTitles()
  {
    inTitle.setText(org_in.concat(" : " + in.getSizeOfList()));

    if (out != null)
      {
	outTitle.setText(org_out.concat(" : " + out.getSizeOfList()));
      }
  }

  /**
   * This method handles resizing the StringSelector if appropriate.
   */

  private void recalcSize()
  {
    if (debug)
      {
	System.err.println("recalcSize()");
      }

    if (minRows == -1 || maxRows == -1)
      {
        return;
      }

    int lowerBound = minRows;

    if (in.getSizeOfList() > lowerBound)
      {
        lowerBound = in.getSizeOfList();
      }

    if (out != null)
      {
	int halfOptions = out.getSizeOfList() / 2;

        if (halfOptions > lowerBound)
          {
            lowerBound = halfOptions;
          }
      }

    if (lowerBound > maxRows)
      {
        lowerBound = maxRows;
      }

    if (debug)
      {
	System.err.println("StringSelector.recalcSize(): currentRows == " + currentRows);
	System.err.println("StringSelector.recalcSize(): lowerBound == " + lowerBound);
      }

    if (currentRows != lowerBound)
      {
        setVisibleRowCount(lowerBound, false);
      }
  }

  /**
   * This method is intended to be called if the setValuePerformed()
   * callback that we call out to decides that it wants to ignore what
   * we requested and alter the value of the server field in some
   * other way.
   *
   * If this method is called during the time that we are calling an
   * external setValuePerformed(), we'll make a note of it so that the
   * appropriate code above won't try to do a graphical update with
   * bad data.
   */

  public void substituteValueByCallBack(JsetValueCallback callback)
  {
    if (callback != this.my_callback)
      {
	throw new IllegalStateException();
      }

    this.replacingValue = true;
  }

  /**
   * debug rig
   */

  public static void main(String[] args) {
    /*try {
      UIManager.setLookAndFeel( UIManager.getCrossPlatformLookAndFeelClassName() );
    } 
    catch (Exception e) { }*/

    JFrame frame = new JFrame("SwingApplication");

    Vector v1 = new Vector();
    Vector v2 = new Vector();
    for ( int i=0; i < 10; i++ )
      {
	v1.addElement( Integer.toString( i ) );
	v2.addElement( Integer.toString( 20-i ) );
      }

    StringSelector ss = new StringSelector( frame, 
					    true, 
					    true, 
					    true);

    ss.update(v1, true, null, v2, true, null);
	
    frame.getContentPane().add(ss, BorderLayout.CENTER);

    frame.addWindowListener(new WindowAdapter() 
    {
      public void windowClosing(WindowEvent e) 
      {
        System.exit(0);
      }
    });
    
    frame.pack();
    frame.setVisible(true);
  }
}
