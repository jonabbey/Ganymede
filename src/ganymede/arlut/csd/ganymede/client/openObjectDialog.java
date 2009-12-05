/*

   createObjectDialog.java

   A Dialog to open an object from the database for a variety of operations.

   Created: 31 October 1997

   Module By: Mike Mulvaney

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2009
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
   along with this program; if not, write to the Free Software
   Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
   02111-1307, USA

*/

package arlut.csd.ganymede.client;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import arlut.csd.JDataComponent.TimedKeySelectionManager;
import arlut.csd.JDataComponent.listHandle;
import arlut.csd.JDialog.StandardDialog;
import arlut.csd.JDialog.StringDialog;
import arlut.csd.Util.TranslationService;
import arlut.csd.Util.VecQuickSort;
import arlut.csd.ganymede.common.BaseDump;
import arlut.csd.ganymede.common.Invid;
import arlut.csd.ganymede.common.Query;
import arlut.csd.ganymede.common.QueryDataNode;
import arlut.csd.ganymede.common.QueryResult;

/*------------------------------------------------------------------------------
                                                                           class
                                                                openObjectDialog

------------------------------------------------------------------------------*/

/**
 *
 * A Dialog to open an object from the database for a variety of operations.
 *
 */

public class openObjectDialog extends StandardDialog implements ActionListener, MouseListener {

  private final static boolean debug = false;

  /**
   * TranslationService object for handling string localization in the
   * Ganymede client.
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.client.openObjectDialog");

  static final String DEFAULT_OPEN = "open_default_type";

  static final private String OPEN_OBJ = "Go";
  static final private String CANCEL = "Cancel";

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
    titleL,
    iconL;

  ImageIcon
    icon;

  String selectedBaseName = null;
  String selectedObjectName = null;
  boolean selectedFound = false;

  /* -- */

  /**
   *
   * This is the constructor for openObjectDialog.  The thing worth
   * noting about this is that when this object is first constructed,
   * it checks gclient to see if an object node is selected in the
   * gclient's tree.  If so, it will set the object's type and name in
   * the appropriate GUI fields as they are constructed.
   *
   * Note that this field-setting only occurs when this dialog is
   * first constructed.  I would have had to rewrite a lot of this
   * class to make it so that the base/object name fields could have
   * been set after the fact, so I just didn't.  gclient is set up to
   * dispose of an old instance of this and recreate one as necessary.
   * It's a shameful hack, but it works, and it's not worth investing
   * much more time here to avoid the redundant dialog creation.
   *
   */

  public openObjectDialog(gclient client)
  {
    super(client, ts.l("init.dialog_title"), Dialog.ModalityType.DOCUMENT_MODAL); // "Open object"

    this.client = client;

    InvidNode selectedNode = client.getSelectedObjectNode();

    if (selectedNode != null)
      {
	selectedBaseName = selectedNode.getTypeText();

	// if we get the handle and query it for its label, we
	// avoid getting the "(inactive") tacked on for inactive
	// objects.

	selectedObjectName = selectedNode.getHandle().getLabel();

        selectedFound = true;
      }
    else
      {
	if (gclient.prefs != null)
	  {
	    selectedBaseName = gclient.prefs.get(DEFAULT_OPEN, null);
	  }
      }

    gbl = new GridBagLayout();
    gbc = new GridBagConstraints();

    getContentPane().setLayout(new BorderLayout());
    middle = new JPanel(gbl);
    getContentPane().add(middle, BorderLayout.CENTER);
    gbc.insets = new Insets(4,4,4,4);

    // Set up the dialog's icon (by default, no icon).
    // gclient actually specifies the image
    // by calling openObjectDialog's setIcon method.

    this.icon = new ImageIcon();
    iconL = new JLabel(icon);
    iconL.setBorder(new EmptyBorder(10,15,10,15));

    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.gridwidth = 1;
    gbl.setConstraints(iconL, gbc);

    middle.add(iconL);

    // "Choose object:"
    titleL = new JLabel(ts.l("init.initial_title_label"), SwingConstants.CENTER);
    titleL.setFont(new Font("Helvetica", Font.BOLD, 14));
    titleL.setOpaque(true);
    titleL.setBorder(client.emptyBorder5);

    gbc.gridx = 1;
    gbc.gridy = 0;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbl.setConstraints(titleL, gbc);

    middle.add(titleL);

    gbc.fill = GridBagConstraints.NONE;
    gbc.gridwidth = 1;

    type = new JComboBox();
    type.setKeySelectionManager(new TimedKeySelectionManager());

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

    BaseDump thisBase = null;

    Vector listHandles = new Vector();

    for (int i = 0; i < bases.size(); i++)
      {
	thisBase = (BaseDump) bases.elementAt(i);
	String name = (String) baseNames.get(thisBase);

	if (!thisBase.isEmbedded())
	  {
	    listHandle lh = new listHandle(name, (Short) baseToShort.get(thisBase));
	    listHandles.addElement(lh);
	  }
      }

    listHandles = client.sortListHandleVector(listHandles);

    for (int i = 0; i < listHandles.size(); i++)
      {
	type.addItem(listHandles.elementAt(i));
      }

    if (selectedBaseName != null)
      {
	for (int i = 0; i < listHandles.size(); i++)
	  {
	    listHandle lh = (listHandle) listHandles.elementAt(i);

	    if (lh.getLabel().equals(selectedBaseName))
	      {
		type.setSelectedItem(lh);
		break;
	      }
	  }
      }

    gbc.gridx = 0;
    gbc.gridy = 1;
    gbc.fill = GridBagConstraints.NONE;
    gbc.gridwidth = 2;

    // "Object Type:"
    JLabel oType = new JLabel(ts.l("init.type_label"));

    gbl.setConstraints(oType, gbc);
    middle.add(oType);

    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.gridx = 2;
    gbl.setConstraints(type, gbc);
    middle.add(type);

    // "Object Name:"
    JLabel editTextL = new JLabel(ts.l("init.name_label"));

    gbc.fill = GridBagConstraints.NONE;
    gbc.gridwidth = 2;
    gbc.gridx = 0;
    gbc.gridy = 2;
    gbl.setConstraints(editTextL, gbc);
    middle.add(editTextL);

    text = new JTextField(20);

    if (selectedFound && selectedObjectName != null)
      {
	text.setText(selectedObjectName);
      }

    text.addActionListener(this);

    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.gridx = 2;
    gbl.setConstraints(text, gbc);
    middle.add(text);

    ok = new JButton(StringDialog.getDefaultOk());
    ok.setActionCommand(OPEN_OBJ);
    ok.addActionListener(this);

    JButton neverMind = new JButton(StringDialog.getDefaultCancel());
    neverMind.setActionCommand(CANCEL);
    neverMind.addActionListener(this);

    if (isRunningOnMac())
      {
	JPanel macPanel = new JPanel();
	macPanel.setLayout(new BorderLayout());

	JPanel buttonP = new JPanel();

	buttonP.add(neverMind);
	buttonP.add(ok);

	macPanel.add(buttonP, BorderLayout.EAST);
	getContentPane().add(macPanel, BorderLayout.SOUTH);
      }
    else
      {
	JPanel buttonP = new JPanel();

	buttonP.add(ok);
	buttonP.add(neverMind);

	getContentPane().add(buttonP, BorderLayout.SOUTH);
      }

    setBounds(150,100, 200,100);
  }

  public Invid chooseInvid()
  {
    invalidate();
    validate();
    pack();

    type.requestFocus();

    setVisible(true);

    return invid;
  }

  public void setText(String text)
  {
    titleL.setText(text);
  }

  public void setIcon(ImageIcon icon)
  {
    iconL.setIcon(icon);
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

    if (gclient.prefs != null)
      {
	gclient.prefs.put(DEFAULT_OPEN, getTypeString());
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
    else if (e.getActionCommand().equals(OPEN_OBJ))
      {
	String string = text.getText();

	if ((string == null) || (string.equals("")))
	  {
	    // "Error, no object label provided to object open dialog."
	    client.setStatus(ts.l("actionPerformed.name_missing_status"));
	    return;
	  }

	if ((currentObject != null) && (string.equals(currentObject.getLabel())))
	  {
	    // This was set from the listbox, and hasn't been changed.

	    invid = (Invid)currentObject.getObject();
	    close(true);

	    return;
	  }

	// We're looking up a new one.  Deselect the selected node in
	// the tree, since we're going to be working with a different
	// object.

	client.tree.unselectAllNodes(true);
	client.tree.refresh();
	currentObject = null;

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

	    // "Matching Objects"
	    pane.setBorder(new TitledBorder(ts.l("actionPerformed.matching_border")));
	  }

	listHandle lh = (listHandle) type.getSelectedItem();
	Short baseID = (Short) lh.getObject();

	if (debug)
	  {
	    System.out.println("BaseID = " + baseID + ", string = " + string);
	  }

	// "Searching for object named {0}."
	client.setStatus(ts.l("actionPerformed.searching_status", string));

	// First see if this exactly matches something, then do the STARTSWITH stuff

	try
	  {
	    invid = client.session.findLabeledObject(string, baseID, false);

	    if (invid != null)
	      {
		// "Found object named {0}."

		client.setStatus(ts.l("actionPerformed.found_status", string));
		close(true);

		return;
	      }
	  }
	catch (java.rmi.RemoteException ex)
	  {
	    close(false);
	    client.processExceptionRethrow(ex, "Remote Exception calling findLabeledObject()");
	  }

	// no direct match, let's look for a prefix match

	QueryDataNode node = new QueryDataNode(QueryDataNode.EQUALS, string);
	QueryResult edit_query = null;
	Vector edit_invids = null;

	try
	  {
	    if (debug)
	      {
		System.out.println("Looking for Startswith...");
	      }

	    // "Searching for objects whose names begin with {0}."
	    client.setStatus(ts.l("actionPerformed.searching_prefix_status", string));

	    node = new QueryDataNode(QueryDataNode.STARTSWITH, string);
	    edit_query = null;

	    edit_query = client.session.query(new Query(baseID.shortValue(), node, editableOnly));

	    edit_invids = edit_query.getListHandles();

	    // and add a direct match of a different type, if it exists

	    invid = client.session.findLabeledObject(string, baseID, true);

	    if (invid != null)
	      {
		String matchLabel = client.session.viewObjectLabel(invid);

		edit_invids.add(0, new listHandle("(" + client.getObjectType(invid) + ") " + string, invid));
	      }

	    if (edit_invids.size() == 1)
	      {
		invid = (Invid)((listHandle)edit_invids.elementAt(0)).getObject();
		close(true);
	      }
	    else if (edit_invids.size() == 0)
	      {
		// "Error Finding Object"
		// "No editable object starts with that string."
		// "No viewable object starts with that string."
		client.showErrorMessage(ts.l("actionPerformed.error_title"),
					editableOnly ?
					ts.l("actionPerformed.no_editable_text") :
					ts.l("actionPerformed.no_viewable_text"));
		return;
	      }
	    else
	      {
		(new VecQuickSort(edit_invids,
				  new Comparator() {
				    public int compare(Object a, Object b)
				    {
				      listHandle aF, bF;
				      
				      aF = (listHandle) a;
				      bF = (listHandle) b;
				      int comp = 0;

				      comp =  aF.toString().compareToIgnoreCase(bF.toString());

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
	catch (java.rmi.RemoteException rx)
	  {
	    close(false);
	    client.processExceptionRethrow(rx, "Remote Exception opening object");
	  }
      }
    else if (e.getActionCommand().equals(CANCEL))
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
