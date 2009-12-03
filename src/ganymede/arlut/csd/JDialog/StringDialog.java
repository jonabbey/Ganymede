/*

   StringDialog.java

   A configurable Dialog box.

   Created: 16 June 1997

   Module By: Michael Mulvaney

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996 - 2009
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

package arlut.csd.JDialog;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Date;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;

import arlut.csd.JDataComponent.JMultiLineLabel;
import arlut.csd.JDataComponent.JcalendarField;
import arlut.csd.JDataComponent.JpassField;
import arlut.csd.JDataComponent.JpasswordField;
import arlut.csd.JDataComponent.JstringArea;
import arlut.csd.JDataComponent.JstringField;
import arlut.csd.JDataComponent.TimedKeySelectionManager;

import arlut.csd.Util.TranslationService;

/*------------------------------------------------------------------------------
                                                                           class
                                                                    StringDialog

------------------------------------------------------------------------------*/

/**
 * <p>A simple customizable modal dialog with support for a variety of
 * data field components.</p>
 *
 * <p>For simple dialogs, use the included constructors.  For more
 * complicated dialogs, including check boxes, choice lists, and text
 * fields, use a {@link arlut.csd.JDialog.DialogRsrc DialogRsrc} object
 * to pass in a pre-defined dialog definition.</p>
 *
 * <p>The ShowDialog method shows the current dialog, and returns a
 * Hashtable of results, which map the label used in the dialog for
 * individual data fields with the value entered into that field.</p>
 *
 * @see DialogRsrc
 * @version $Id$
 * @author Mike Mulvaney
 */

public class StringDialog extends JCenterDialog implements ActionListener, WindowListener {

  static final boolean debug = false;

  /**
   * <p>TranslationService object for handling string localization in
   * the Ganymede server.</p>
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.JDialog.StringDialog");

  static final String ok = ts.l("global.ok"); // "Ok"
  static final String cancel = ts.l("global.cancel"); // "Cancel"

  /**
   * Returns the localized Ok string for the language used by the
   * client's Java environment.
   */

  public static String getDefaultOk()
  {
    return ok;
  }

  /**
   * Returns the localized Cancel string for the language used by the
   * client's Java environment.
   */

  public static String getDefaultCancel()
  {
    return cancel;
  }

  // --

  DialogRsrc resource = null;

  Hashtable
    componentHash,
    valueHash;

  boolean
    done;

  JLabel
    imageCanvas;

  JButton
    OKButton,
    CancelButton;

  JPanel
    panel,
    mainPanel,
    dataPanel,
    buttonPanel;

  JMultiLineLabel
    textLabel;

  GridBagLayout
    gbl,
    compgbl;

  GridBagConstraints
    gbc,
    compgbc;

  Image image;

  Vector objects;

  Vector
    components;

  /* -- */

  /**
   *
   * Simple constructor for a small dialog box
   *
   * @param frame Parent frame of the Dialog Box
   * @param Title Title of the Dialog Box
   * @param Text Text shown in the Dialog Box
   * @param ShowCancel if true, show a "Cancel" button
   *
   */

  public StringDialog(Frame frame, String Title, String Text, boolean ShowCancel)
  {
    this(frame, Title, Text, ts.l("global.ok"), ShowCancel ? ts.l("global.cancel") : null, null);
  }

  /**
   *
   * Simple constructor for a small dialog box with a Cancel button
   *
   * @param frame Parent frame of the Dialog Box
   * @param Title Title of the Dialog Box
   * @param Text Text shown in the Dialog Box
   *
   */

  public StringDialog(Frame frame, String Title, String Text)
  {
    this(frame, Title, Text, ts.l("global.ok"), ts.l("global.cancel"), null);
  }

  /**
   * Simple constructor for a small dialog box
   *
   * @param frame Parent frame of the Dialog Box
   * @param Title Title of the Dialog Box
   * @param Text Text shown in the Dialog Box
   * @param OK String for "OK" button
   * @param Cancel String for "Cancel" button
   *
   */

  public StringDialog(Frame frame, String Title, String Text, String OK, String Cancel)
  {
    this(new DialogRsrc(frame, Title, Text, OK, Cancel, (Image) null));
  }

  /**
   * Simple constructor for a small dialog box
   *
   * @param frame Parent frame of the Dialog Box
   * @param Title Title of the Dialog Box
   * @param Text Text shown in the Dialog Box
   * @param OK String for "OK" button
   * @param Cancel String for "Cancel" button
   * @param image Image to display next to text
   */

  public StringDialog(Frame frame, String Title, String Text, String OK, String Cancel, Image image)
  {
    this(new DialogRsrc(frame, Title, Text, OK, Cancel, image));
  }

  /**
   *
   * Constructor for more complicated StringDialog.
   *
   * @param Resource Sets resource for Dialog box.
   *
   */

  public StringDialog(DialogRsrc Resource)
  {
    super(Resource.frame, Resource.title, true);
    this.resource = Resource;

    create();
  }

  public Dimension getPreferredSize()
  {
    // Let's make sure we don't try to get too big by default.

    Dimension size = super.getPreferredSize();

    if (size.width > 1000)
      {
	size.width = 1000;
      }

    if (size.height > 600)
      {
	size.height = 600;
      }

    return size;
  }

  private void create()
  {
    this.addWindowListener(this);

    if (debug)
      {
	System.err.println("StringDialog constructor");
      }

    mainPanel = new JPanel();
    mainPanel.setBorder(new CompoundBorder(new EtchedBorder(),
					   new EmptyBorder(10, 10, 10, 10)));
    mainPanel.setLayout(new BorderLayout());
    setContentPane(mainPanel);

    // set it as a modal sheet on the Mac

    getRootPane().putClientProperty("apple.awt.documentModalSheet", Boolean.TRUE);

    //
    // Title at top of dialog
    //

    //    JLabel titleLabel = new JLabel(resource.title == null ? "": resource.title, SwingConstants.CENTER);
    //    titleLabel.setFont(new Font("Helvetica", Font.BOLD, 14));
    //    mainPanel.add(titleLabel, "North");

    //
    // Image on left hand side
    //

    image = resource.getImage();
    JPanel imagePanel = new JPanel();

    if (image != null)
      {
	imageCanvas = new JLabel(new ImageIcon(image));
	imagePanel.add(imageCanvas);
      }
    else
      {
	imagePanel.add(Box.createGlue());
      }

    mainPanel.add(imagePanel, "West");

    //
    // Text message under title
    //

    if (resource.getText() != null && !resource.getText().trim().equals(""))
      {
	textLabel = new JMultiLineLabel(resource.getText());

	JScrollPane pane = new JScrollPane(textLabel,
					   JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
					   JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

	pane.getViewport().setOpaque(false);
	pane.setViewportBorder(null);

	pane.setBorder(new EmptyBorder(10,10,10,10));

	mainPanel.add(pane, "Center");
      }

    // now we need to create our south panel

    JPanel southPanel = new JPanel();
    southPanel.setLayout(new BorderLayout());
    mainPanel.add(southPanel, "South");

    //
    // Now to build the south panel

    //
    // Our data object panel
    //

    panel = createElementPanel();
    southPanel.add(panel, "Center");

    //
    // ButtonPanel takes up the bottom of the dialog
    //

    buttonPanel = new JPanel();
    buttonPanel.setLayout(new BorderLayout());

    JPanel flowPanel = new JPanel();

    OKButton = new JButton(resource.getOkText());
    OKButton.addActionListener(this);

    // if cancel is null, don't put it on there

    CancelButton = null;

    if (resource.getCancelText() != null)
      {
	CancelButton = new JButton(resource.getCancelText());
	CancelButton.addActionListener(this);
	flowPanel.add(CancelButton);
      }

    boolean runningOnMac = "Mac OS X".equals(System.getProperty("os.name"));

    if (runningOnMac)
      {
	// On the Mac, we place the Cancel button to the left of the Ok
	// button.

	if (CancelButton != null)
	  {
	    flowPanel.add(CancelButton);
	  }

	flowPanel.add(OKButton);
      }
    else
      {
	// otherwise, Ok is to the left.

	flowPanel.add(OKButton);

	if (CancelButton != null)
	  {
	    flowPanel.add(CancelButton);
	  }
      }

    buttonPanel.add(new JSeparator(), "North");

    if (runningOnMac)
      {
	JPanel macPanel = new JPanel();
	macPanel.setLayout(new BorderLayout());
	macPanel.add(flowPanel, BorderLayout.EAST); // right align

	buttonPanel.add(macPanel, "South");
      }
    else
      {
	buttonPanel.add(flowPanel, "South");
      }

    southPanel.add(buttonPanel, "South");

    registerCallbacks();
    pack();
  }

  private JPanel createElementPanel()
  {
    JPanel panel = new JPanel();
    panel.setBorder(null);

    compgbc = new GridBagConstraints();
    compgbl = new GridBagLayout();

    compgbc.insets = new Insets(0,4,0,4);
    panel.setLayout(compgbl);

    componentHash = new Hashtable();
    valueHash = new Hashtable();

    objects = resource.getObjects();
    components = new Vector(objects.size());

    int numberOfObjects = objects.size();

    if (numberOfObjects == 0)
      {
	if (debug)
	  {
	    System.err.println("no fields to add to StringDialog");
	  }

	return panel;
      }

    for (int i = 0; i < numberOfObjects; ++i)
      {
	Object element = objects.elementAt(i);

	if (debug)
	  {
	    System.err.println("number: " + numberOfObjects + " current: " + i);
	  }

	if (element instanceof stringThing)
	  {
	    if (debug)
	      {
		System.err.println("Adding string field(JstringField)");
	      }

	    stringThing st = (stringThing) element;

	    if (st.isMultiline())
	      {
		JstringArea sa = new JstringArea(5, 40);
		sa.setText(st.getValue());
		sa.setEditable(true);

		addRow(panel, sa, st.getLabel(), i);

		componentHash.put(sa, st.getLabel());
		valueHash.put(st.getLabel(), "");
	      }
	    else
	      {
		JstringField sf = new JstringField();
		sf.setText(st.getValue());
		sf.setEditable(true);

		addRow(panel, sf, st.getLabel(), i);

		componentHash.put(sf, st.getLabel());
		valueHash.put(st.getLabel(), "");
	      }
	  }
	else if (element instanceof dateThing)
	  {
	    if (debug)
	      {
		System.err.println("Adding date field(JcalendarField)");
	      }

	    dateThing dt = (dateThing) element;

	    JcalendarField dateField;
	    Date currentDate;
	    Date minDate = new Date();

	    if (dt.getDate() != null)
	      {
		currentDate = dt.getDate();

		if (dt.getMaxDate() != null)
		  {
		    if (currentDate.after(dt.getMaxDate()))
		      {
			currentDate = dt.getMaxDate();
		      }
		  }
	      }
	    else
	      {
		if (dt.getMaxDate() != null)
		  {
		    currentDate = dt.getMaxDate();
		  }
		else
		  {
		    currentDate = null;
		  }
	      }

	    if (dt.getMaxDate() != null)
	      {
		dateField = new JcalendarField(currentDate, true, true,
					       minDate, dt.getMaxDate());
	      }
	    else
	      {
		dateField = new JcalendarField(currentDate, true, false,
					       null, null);
	      }

	    addRow(panel, dateField, dt.getLabel(), i);

	    componentHash.put(dateField, dt.getLabel());
	    valueHash.put(dt.getLabel(), currentDate);
	  }
	else if (element instanceof passwordThing)
	  {
	    if (debug)
	      {
		System.err.println("Adding password field(JpasswordField)");
	      }

	    passwordThing pt = (passwordThing)element;

	    // new password, so we're not trying to validate against
	    // an existing password.. we'll want to give the user a
	    // double password field so that they can validate

	    if (pt.isNew())
	      {
		if (debug)
		  {
		    System.err.println("This password is new.");
		  }

		JpassField sf = new JpassField(null,10,100,true);

		addRow(panel, sf, pt.getLabel(), i);

		componentHash.put(sf, pt.getLabel());
	      }
	    else
	      {
		if (debug)
		  {
		    System.err.println("This password is not new.");
		  }

		JpasswordField sf = new JpasswordField();
		sf.setEditable(true);

		addRow(panel, sf, pt.getLabel(), i);
		componentHash.put(sf, pt.getLabel());
	      }

	    valueHash.put(pt.getLabel(), "");
	  }
	else if (element instanceof booleanThing)
	  {
	    if (debug)
	      {
		System.err.println("Adding boolean field (JcheckboxField)");
	      }

	    booleanThing bt = (booleanThing)element;
	    JCheckBox cb = new JCheckBox();
	    cb.setSelected(bt.getValue());

	    addRow(panel, cb, bt.getLabel(), i);

	    componentHash.put(cb, bt.getLabel());
	    valueHash.put(bt.getLabel(), Boolean.valueOf(bt.getValue()));
	  }
	else if (element instanceof choiceThing)
	  {
	    if (debug)
	      {
		System.err.println("Adding choice field (JComboBox)");
	      }

	    choiceThing ct = (choiceThing)element;
	    JComboBox ch = new JComboBox();
	    ch.setKeySelectionManager(new TimedKeySelectionManager());

	    if (debug)
	      {
		System.err.println("Getting choice lists");
	      }

	    Vector items = ct.getItems();

	    if (debug)
	      {
		System.err.println("Got choice lists");
	      }

	    if (items == null)
	      {
		if (debug)
		  {
		    System.err.println("Nothing to add to Choice, empty vector");
		  }
	      }
	    else
	      {
		int total = items.size();

		for (int j = 0; j < total ; ++j)
		  {
		    ch.addItem(items.elementAt(j));
		  }

		if (ct.getSelectedItem() != null)
		  {
		    if (!items.contains(ct.getSelectedItem()))
		      {
			ch.addItem(ct.getSelectedItem());
		      }

		    ch.setSelectedItem(ct.getSelectedItem());
		  }

		addRow(panel, ch, ct.getLabel(), i);

		componentHash.put(ch, ct.getLabel());

		if (ch.getSelectedItem() != null)
		  {
		    valueHash.put(ct.getLabel(), ch.getSelectedItem());
		  }
	      }
	  }
	else
	  {
	    System.err.println("StringDialog constructor: Item " + i + " is of unknown type");
	  }
      }

    return panel;
  }

  /**
   *
   * We want to make it so that when the user hits enter on the last
   * string or password field in the dialog, the ok button is clicked.
   *
   */

  protected void registerCallbacks()
  {
    for (int i = 0; i < components.size(); i++)
      {
	JComponent c = (JComponent)components.elementAt(i);

	if (i == 0)
	  {
	    // not sure if this does us any good on X Windows, as
	    // focus is usually managed by clicking or rolling the
	    // mouse on to the dialog window.. might help on Win32.

	    c.setRequestFocusEnabled(true);
	    c.requestFocus();
	  }

	if (debug)
	  {
	    System.err.println("Checking component: " + c);
	  }

	if (c instanceof JstringField)
	  {
	    JstringField sf = (JstringField) c;

	    if (i == components.size() -1) // last one!
	      {
		sf.addActionListener
		  (
		   new ActionListener()
		   {
		     public void actionPerformed(ActionEvent e) {
		       OKButton.doClick();
		     }
		   });
	      }
	    else
	      {
		sf.addActionListener
		  (
		   new ActionListener()
		   {
		     public void actionPerformed(ActionEvent e) {
		       JComponent thisComp = (JComponent)e.getSource();

		       ((JComponent)components.elementAt(components.indexOf(thisComp) + 1)).requestFocus();
		     }
		   });
	      }
	  }
	else if (c instanceof JpasswordField)
	  {
	    if (debug)
	      {
		System.err.println("This is a JpasswordField, number " + i);
	      }

	    JpasswordField pf = (JpasswordField) c;

	    if (i == components.size() -1)
	      {
		pf.addActionListener
		  (
		   new ActionListener()
		   {
		     public void actionPerformed(ActionEvent e) {
		       OKButton.doClick();
		     }
		   });
	      }
	    else
	      {
		pf.addActionListener
		  (
		   new ActionListener()
		   {
		     public void actionPerformed(ActionEvent e) {
		       JComponent thisComp = (JComponent)e.getSource();

		       ((JComponent)components.elementAt(components.indexOf(thisComp) + 1)).requestFocus();
		     }
		   });
	      }
	  }
      }
  }

  /**
   * <p>Set the word wrap length of the main text area in this StringDialog.</p>
   */

  public void setWrapLength(int wrapLength)
  {
    textLabel.setWrapLength(wrapLength);
  }

  public void setFont(Font font)
  {
    textLabel.setFont(font);
  }

  /**
   * <p>Display the dialog box, locks this thread while the dialog is being
   * displayed, and returns a hashtable of data field values when the
   * user closes the dialog box.</p>
   *
   * <p>Use this instead of Dialog.show().  If Hashtable returned is null,
   * then the cancel button was clicked.  Otherwise, it will contain a
   * hash of labels(String) to results (Object).</p>
   *
   * @return HashTable of labels to values
   */

  public Hashtable showDialog()
  {
    mainPanel.revalidate();
    this.pack();
    this.setVisible(true);

    // at this point we're frozen, since we're a modal dialog.. we'll continue
    // at this point when the ok or cancel buttons are pressed.

    if (debug)
      {
	System.err.println("Done invoking.");
      }

    return valueHash;
  }

  /**
   *
   * Handle the ok and cancel buttons.
   *
   */

  public synchronized void actionPerformed(ActionEvent e)
  {
    if (e.getSource() == OKButton)
      {
	loadValueHash();
      }
    else
      {
	valueHash = null;
      }

    // pop down so that showDialog() can proceed to completion.

    done = true;

    setVisible(false);
  }

  /**
   *
   * This method is responsible for scanning all of the input fields
   * in this dialog and loading their values into valueHash for
   * showDialog() to return.
   *
   */

  private void loadValueHash()
  {
    for (int i = 0; i < components.size(); i++)
      {
	JComponent c = (JComponent)components.elementAt(i);
	String label = (String) componentHash.get(c);

	if (debug)
	  {
	    System.err.println("Loading value for field: " + label);
	  }

	try
	  {
	    if (c instanceof JstringField)
	      {
		JstringField sf = (JstringField) c;

		valueHash.put(label, sf.getText());
	      }
	    else if (c instanceof JstringArea)
	      {
		JstringArea sA = (JstringArea) c;

		valueHash.put(label, sA.getText());
	      }
	    else if (c instanceof JpasswordField)
	      {
		JpasswordField pf = (JpasswordField) c;

		valueHash.put(label, pf.getPassword());
	      }
	    else if (c instanceof JpassField)
	      {
		JpassField pf = (JpassField) c;

		valueHash.put(label, pf.getPassword());
	      }
	    else if (c instanceof JcalendarField)
	      {
		JcalendarField dF = (JcalendarField) c;

		valueHash.put(label, dF.getDate());
	      }
	    else if (c instanceof JCheckBox)
	      {
		JCheckBox cb = (JCheckBox) c;

		valueHash.put(label, Boolean.valueOf(cb.isSelected()));
	      }
	    else if (c instanceof JComboBox)
	      {
		JComboBox combo = (JComboBox) c;

		valueHash.put(label, combo.getSelectedItem());
	      }
	  }
	catch (NullPointerException ex)
	  {
	  }
      }
  }

  /**
   *
   * Convenience method to add a GUI component to this dialog.
   *
   */

  private final void addRow(JPanel parent, JComponent comp,  String label, int row)
  {
    components.addElement(comp);

    compgbc.gridwidth = 1;
    compgbc.fill = GridBagConstraints.NONE;
    compgbc.anchor = GridBagConstraints.WEST;

    compgbc.gridy = row;
    compgbc.gridx = 0;
    compgbc.weightx = 0.0;

    JLabel l = new JLabel(label, SwingConstants.LEFT);

    compgbl.setConstraints(l, compgbc);
    parent.add(l);

    compgbc.gridx = 1;
    compgbc.weightx = 1.0;
    compgbc.fill = GridBagConstraints.HORIZONTAL;

    compgbl.setConstraints(comp, compgbc);
    parent.add(comp);

    parent.invalidate();
  }

  // WindowListener methods

  public void windowActivated(WindowEvent event)
  {
  }

  public void windowClosed(WindowEvent event)
  {
  }

  public synchronized void windowClosing(WindowEvent event)
  {
    if (!done)
      {
	if (debug)
	  {
	    System.err.println("Window is closing and we haven't done a cancel.");
	  }

	// by setting valueHash to null, we're basically treating
	// this window close as a cancel.

	valueHash = null;
      }

    done = true;
    this.setVisible(false);
  }

  public void windowDeactivated(WindowEvent event)
  {
  }

  public void windowDeiconified(WindowEvent event)
  {
  }

  public void windowIconified(WindowEvent event)
  {
  }

  public void windowOpened(WindowEvent event)
  {
  }
}
