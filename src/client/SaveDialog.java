/*

   SaveDialog.java

   Dialog for saving or mailing a table from dialog.
   
   Created: ??
   Version: $Revision: 1.4 $
   Last Mod Date: $Date: 1999/10/20 04:34:25 $
   Release: $Name:  $

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

import javax.swing.*;

import java.awt.*;
import java.awt.event.*;

import java.util.*;

import arlut.csd.JCalendar.*;
import arlut.csd.JDataComponent.*;
import arlut.csd.JDialog.JCenterDialog;

/*------------------------------------------------------------------------------
                                                                           class
                                                                      SaveDialog

------------------------------------------------------------------------------*/

/**
 * <p>Client dialog for saving or mailing the results of a query operation.</p>
 */

public class SaveDialog extends JCenterDialog implements ActionListener {

  private final boolean debug = false;

  boolean
    addedFormatChoice = false,
    returnValue = false;
  
  GridBagLayout gbl = new GridBagLayout();
  GridBagConstraints gbc = new GridBagConstraints();

  JButton 
    ok,
    cancel;

  JTextField
    subject = new JTextField(20),
    recipients = new JTextField(20);

  JComboBox
    formats = null;

  // This is the panel that holds everything, layed out by gbl

  JPanel 
    panel;

  formatButtonPanel
    formatPanel;

  Image
    saveImage = arlut.csd.Util.PackageResources.getImageResource(this, "SaveDialog.gif", getClass());

  /* -- */

  /**
   * Main Constructor.
   *
   * @param owner Parent frame
   *
   * @param forMail If true, the dialog will show the recipients field
   * and the ok button will say "mail".  Otherwise, it says "save".
   */

  SaveDialog(Frame owner, boolean forMail)
  {
    super(owner, forMail ? "Send Mail" : "Save format", true);

    panel = new JPanel(gbl);

    gbc.insets = new Insets(6,6,6,6);
    
    gbc.gridwidth = 1;
    gbc.fill = GridBagConstraints.NONE;

    if (forMail)
      {
	gbc.anchor = GridBagConstraints.WEST;

	gbc.gridy = 0;
	gbc.gridx = 0;
	gbc.gridheight = 2;
	JLabel image = new JLabel(new ImageIcon(saveImage));
	gbl.setConstraints(image, gbc);
	panel.add(image);

	gbc.gridheight = 1;
	gbc.gridx = 1;
	gbc.gridy = 0;
	gbc.fill = GridBagConstraints.NONE;
	JLabel rec = new JLabel("To:");
	gbl.setConstraints(rec, gbc);
	panel.add(rec);

	gbc.gridx = 2;
	gbc.fill = GridBagConstraints.HORIZONTAL;
	gbc.weightx = 1;
	gbl.setConstraints(recipients, gbc);
	panel.add(recipients);

	gbc.gridy = 1;
	gbc.gridx = 1;
	gbc.weightx = 0;
	gbc.fill = GridBagConstraints.NONE;
	JLabel sub = new JLabel("Subject:");
	gbl.setConstraints(sub, gbc);
	panel.add(sub);

	gbc.gridx = 2;
	gbc.weightx = 1;
	gbc.fill = GridBagConstraints.HORIZONTAL;
	subject.setText("Query report");
	gbl.setConstraints(subject, gbc);
	panel.add(subject);
      }

    // Row 3 is for the format choices

    arlut.csd.JDataComponent.JSeparator sep = new arlut.csd.JDataComponent.JSeparator();
    gbc.gridwidth = 3;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridy = 4;
    gbc.gridx = 0;
    gbl.setConstraints(sep, gbc);
    panel.add(sep);

    JPanel buttonPanel = new JPanel();
    ok = new JButton(forMail ? "Mail" : "Save");
    ok.addActionListener(this);

    cancel = new JButton("Cancel");
    cancel.addActionListener(this);

    buttonPanel.add(ok);
    buttonPanel.add(cancel);

    gbc.gridx = 0;
    gbc.gridy = 5;
    gbc.gridwidth = 3;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbl.setConstraints(buttonPanel, gbc);
    panel.add(buttonPanel);

    setContentPane(panel);

    pack();
  }

  /**
   * <p>Show the dialog.</p>
   *
   * <p>Use this instead of calling setVisible(true) yourself.  You need to get the boolean
   * return from this method, in order to know if the user pressed "Ok" or "Cancel".
   *
   * @return True if user pressed "Ok".
   */

  public boolean showDialog()
  {
    setVisible(true);
    if (debug)
      {
	System.out.println("Returning " + returnValue);
      }

    return returnValue;
  }

  /**
   * String of recipients for the mail.
   *
   * This is not formatted in any way, so you get whatever the user typed in.
   */

  public String getRecipients()
  {
    return recipients.getText();
  }

  /**
   * Returns the text for the subject of the mail.
   *
   */
  public String getSubject()
  {
    return subject.getText();
  }

  /**
   * Set the choices for the format choices.
   *
   * @param choices Vector of Strings of the different choices.
   *
   * <P>Usually we send in a Vector of strings like "HTML", "Plain text", etc.</p>
   */
  public void setFormatChoices(Vector choices)
  {
    //formats = new JComboBox(choices);

    if (!addedFormatChoice)
      {
	addFormatChoiceButtons(choices);
      }
  }

  /**
   * Returns the choice of format.
   *
   * This will be one of the Strings in setFormatChoicse(), unless
   * something went horribly awry.
   */
  public String getFormat()
  {
    //return (String)formats.getSelectedItem();
    return formatPanel.getSelectedFormat();
  }

  // This one adds the format choice.
  private void addFormatChoice()
  {
    gbc.gridy = 3;
    gbc.gridx = 0;
    gbc.fill = GridBagConstraints.NONE;
    gbc.gridwidth = 1;

    JLabel l = new JLabel("Format:");
    gbl.setConstraints(l, gbc);
    panel.add(l);

    gbc.gridx = 1;

    gbl.setConstraints(formats, gbc);
    panel.add(formats);

    addedFormatChoice = true;
    pack();
  }

  // This really just adds in a new formatButtonPanel
  private void addFormatChoiceButtons(Vector choices)
  {
    gbc.gridy = 3;
    gbc.gridx = 0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridwidth = 3;

    formatPanel  = new formatButtonPanel(choices);
    gbl.setConstraints(formatPanel, gbc);
    panel.add(formatPanel);

    addedFormatChoice = true;
    pack();
  }

  public void actionPerformed(ActionEvent e)
  {
    if (e.getSource() == ok)
      {
	if (debug)
	  {
	    System.out.println("ok");
	  }

	returnValue = true;
	setVisible(false);
      }
    else if (e.getSource() == cancel)
      {
	if (debug)
	  {
	    System.out.println("cancel");
	  }

	returnValue = false;
	setVisible(false);
      }
  }
}

/**
 * <p>Client panel that holds the choice of formats for
 * {@link arlut.csd.ganymede.client.SaveDialog SaveDialog}.</p>
 */

class formatButtonPanel extends JPanel {

  Hashtable modelToLabel = new Hashtable();

  ButtonGroup
    group = new ButtonGroup();

  GridBagLayout
    layout = new GridBagLayout();

  GridBagConstraints
    constraints = new GridBagConstraints();

  formatButtonPanel(Vector choices)
  {
    setLayout(layout);

    String s;
    
    setBorder(BorderFactory.createTitledBorder("Format"));
    constraints.gridx = 0;
    constraints.anchor = GridBagConstraints.WEST;

    for (int i = 0; i < choices.size(); i++)
      {
	s = (String)choices.elementAt(i);

	JRadioButton b = new JRadioButton(s);

	if (i == 0)
	  {
	    b.setSelected(true);
	  }

	modelToLabel.put(b.getModel(), s);
	group.add(b);

	constraints.gridy = i;
	layout.setConstraints(b, constraints);
	add(b);
      }
  }

  public String getSelectedFormat()
  {
    return (String)modelToLabel.get(group.getSelection());
  }
}

