/*

   StringDialog.java

   A configurable Dialog box.
   
   Created: 16 June 1997
   Release: $Name:  $
   Version: $Revision: 1.51 $
   Last Mod Date: $Date: 1999/03/22 22:37:55 $
   Module By: Michael Mulvaney

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

package arlut.csd.JDialog;

import arlut.csd.JDataComponent.*;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.border.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                    StringDialog

------------------------------------------------------------------------------*/

/**
 *
 * <p>A simple customizable dialog with support for a variety of data
 * field components.</p>
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
 * @version $Revision: 1.51 $ $Date: 1999/03/22 22:37:55 $ $Name:  $
 * @author Mike Mulvaney 
 */

public class StringDialog extends JCenterDialog implements ActionListener, WindowListener {

  static final boolean debug = false;

  // --

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
    this (frame, Title, Text, "Ok", ShowCancel ? "Cancel" : null, null);
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
    this(frame, Title, Text, "Ok", "Cancel", null);
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
    this.addWindowListener(this);

    if (debug)
      {
	System.err.println("StringDialog constructor");
      }
      
    componentHash = new Hashtable();
    valueHash = new Hashtable();

    objects = Resource.getObjects();
    components = new Vector(objects.size());

    gbl = new GridBagLayout();
    gbc = new GridBagConstraints();

    gbc.insets = new Insets(4,4,4,4);

    mainPanel = new JPanel();
    mainPanel.setBorder(new CompoundBorder(new EtchedBorder(),
					   new EmptyBorder(10, 10, 10, 10)));
    mainPanel.setLayout(gbl);
    setContentPane(mainPanel);

    //
    // Title at top of dialog
    //

    JLabel titleLabel = new JLabel(Resource.title, SwingConstants.CENTER);
    titleLabel.setFont(new Font("Helvetica", Font.BOLD, 14));

    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.gridwidth = 2;
    gbc.gridheight = 1;
    gbl.setConstraints(titleLabel, gbc);
    mainPanel.add(titleLabel);

    //
    // Text message under title
    //

    textLabel = new JMultiLineLabel(Resource.getText());
    
    gbc.gridy = 1;
    gbc.gridx = 1;
    gbc.gridwidth = 1;
    gbc.gridheight = 1;
    gbl.setConstraints(textLabel, gbc);
    mainPanel.add(textLabel);

    //
    // Separator goes all the way accross
    // 

    arlut.csd.JDataComponent.JSeparator sep = new arlut.csd.JDataComponent.JSeparator();

    gbc.gridx = 0;
    gbc.gridy = 3;
    gbc.gridwidth = 2;
    gbc.gridheight = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbl.setConstraints(sep, gbc);
    mainPanel.add(sep);

    //
    // ButtonPanel takes up the bottom of the dialog
    //

    buttonPanel = new JPanel();

    OKButton = new JButton(Resource.OKText);
    OKButton.addActionListener(this);
    buttonPanel.add(OKButton);

    // if cancel is null, don't put it on there

    if (Resource.CancelText != null)
      {
	CancelButton = new JButton(Resource.CancelText);
	CancelButton.addActionListener(this);
	buttonPanel.add(CancelButton);
      }

    //
    // buttonPanel goes underneath
    //

    gbc.gridx = 0;
    gbc.gridy = 4;
    gbc.gridwidth = 2;
    gbc.gridheight = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbl.setConstraints(buttonPanel, gbc);
    mainPanel.add(buttonPanel);

    //
    // Image on left hand side
    //

    image = Resource.getImage();
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

    gbc.gridx = 0;
    gbc.gridy = 1;
    gbc.gridwidth = 1;
    gbc.gridheight = 2;
    gbc.weightx = 0.0;
    gbc.fill = GridBagConstraints.NONE;
    gbl.setConstraints(imagePanel, gbc);
    mainPanel.add(imagePanel);

    if (debug)
      {
	System.err.println("StringDialog: adding objects");
      }

    // We have to make the panel, even if it is empty.  This is
    // because we have to add space to to it for the stupid activator.

    panel = new JPanel();
    panel.setBorder(null);

    gbc.gridx = 1;
    gbc.gridy = 2;
    gbc.gridwidth = 1;
    gbc.gridheight = 1;
    gbc.weightx = 1.0;
    //    gbc.weighty = 1.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbl.setConstraints(panel, gbc);
    mainPanel.add(panel);

    // The panel uses its own grid bag stuff

    compgbc = new GridBagConstraints();
    compgbl = new GridBagLayout();

    compgbc.insets = new Insets(0,4,0,4);
    panel.setLayout(compgbl);
    
    // add stuff to panel here

    if (objects == null)
      {
	if (debug)
	  {
	    System.out.println("null objects vector");
	  }

	pack();
	return;
      }

    int numberOfObjects = objects.size();
	
    if (numberOfObjects == 0) 
      {
	if (debug)
	  {
	    System.out.println("no fields to add to StringDialog");
	  }

	pack();
	return;
      }

    for (int i = 0; i < numberOfObjects; ++i) 
      {
	Object element = objects.elementAt(i);

	if (debug)
	  {
	    System.out.println("number: " + numberOfObjects + " current: " + i);
	  }

	if (element instanceof stringThing)
	  {
	    if (debug)
	      {
		System.out.println("Adding string field(JstringField)");
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
		System.out.println("Adding date field(JdateField)");
	      }
		    
	    dateThing dt = (dateThing) element;

	    JdateField dateField;
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
		dateField = new JdateField(currentDate, true, true,
					   minDate, dt.getMaxDate());
	      }
	    else
	      {
		dateField = new JdateField(currentDate, true, false,
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
		System.out.println("Adding password field(JpasswordField)");
	      }

	    passwordThing pt = (passwordThing)element;

	    if (pt.isNew())
	      {
		if (debug)
		  {
		    System.out.println("This password is new.");
		  }

		JpassField sf = new JpassField(null, true, 10,100,true);
			
		addRow(panel, sf, pt.getLabel(), i);
			
		componentHash.put(sf, pt.getLabel());
	      }
	    else
	      {
		if (debug)
		  {
		    System.out.println("This password is not new.");
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
		System.out.println("Adding boolean field (JcheckboxField)");
	      }

	    booleanThing bt = (booleanThing)element;
	    JCheckBox cb = new JCheckBox();
	    cb.setSelected(bt.getValue());

	    addRow(panel, cb, bt.getLabel(), i);
		      
	    componentHash.put(cb, bt.getLabel());
	    valueHash.put(bt.getLabel(), new Boolean(bt.getValue()));
	  }
	else if (element instanceof choiceThing)
	  {
	    if (debug)
	      {
		System.out.println("Adding choice field (JComboBox)");
	      }

	    choiceThing ct = (choiceThing)element;
	    JComboBox ch = new JComboBox();

	    if (debug)
	      {
		System.out.println("Getting choice lists");
	      }

	    Vector items = ct.getItems();

	    if (debug)
	      {
		System.out.println("Got choice lists");
	      }

	    if (items == null)
	      {
		if (debug)
		  {
		    System.out.println("Nothing to add to Choice, empty vector");
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
	    System.out.println("StringDialog constructor: Item " + i + " is of unknown type");
	  }
      }

    registerCallbacks();
    
    pack();
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
	    System.out.println("Checking component: " + c);
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
		System.out.println("This is a JpasswordField, number " + i);
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

  public Hashtable DialogShow()
  {
    mainPanel.revalidate();
    show();

    // at this point we're frozen, since we're a modal dialog.. we'll continue
    // at this point when the ok or cancel buttons are pressed.

    if (debug)
      {
	System.out.println("Done invoking.");
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

    // pop up down so that DialogShow() can proceed to completion.

    done = true;

    setVisible(false);
  }

  /**
   *
   * This method is responsible for scanning all of the input fields
   * in this dialog and loading their values into valueHash for
   * DialogShow() to return.
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
	    System.out.println("Loading value for field: " + label);
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
	    
		valueHash.put(label, new String(pf.getPassword()));
	      }
	    else if (c instanceof JdateField)
	      {
		JdateField dF = (JdateField) c;

		valueHash.put(label, dF.getDate());
	      }
	    else if (c instanceof JCheckBox)
	      {
		JCheckBox cb = (JCheckBox) c;

		valueHash.put(label, new Boolean(cb.isSelected()));
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
