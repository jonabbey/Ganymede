/*

   historyPanel.java

   The tab that holds history information.
   
   Created: 9 September 1997
   Release: $Name:  $
   Version: $Revision: 1.11 $
   Last Mod Date: $Date: 1999/01/22 18:04:15 $
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

package arlut.csd.ganymede.client;

import java.awt.*;
import java.awt.event.*;
import java.rmi.*;
import java.util.*;
import java.text.SimpleDateFormat;

import javax.swing.*;
import javax.swing.border.*;
import arlut.csd.ganymede.*;

import arlut.csd.JDataComponent.*;
import arlut.csd.JCalendar.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                    historyPanel

------------------------------------------------------------------------------*/

public class historyPanel extends JPanel implements ActionListener, JsetValueCallback{

  JTextArea
    historyText;

  JButton
    showHistory;

  JdateField
    selectDate;

  Invid
    invid;

  gclient gc;

  Date
    selectedDate;

  TitledBorder
    titledBorder;

  /* -- */
  
  public historyPanel(Invid invid, 
		      gclient gc, 
		      string_field    creator_field, 
		      date_field      creation_date_field,
		      string_field    modifier_field,
		      date_field      modification_date_field)
  {
    this.invid = invid;
    this.gc = gc;

    setLayout(new BorderLayout());
      
    JPanel topPanel = new JPanel(new BorderLayout());
    JPanel buttonPanel = new JPanel(false);

    JPanel leftPanel = new JPanel(false);
    leftPanel.add(new JLabel("Restrict dates:"));
    selectDate = new JdateField();
    selectDate.setCallback(this);
    leftPanel.add(selectDate);
    
    buttonPanel.add(leftPanel);
      
    showHistory = new JButton("Show history");
    showHistory.addActionListener(this);
    
    buttonPanel.add(showHistory);
    topPanel.add("North", new datesPanel(creator_field, creation_date_field, 
					 modifier_field, modification_date_field));
    
    JPanel p = new JPanel(new BorderLayout());
    titledBorder = new TitledBorder("Detailed History");
    p.setBorder(titledBorder);
    p.add("North", buttonPanel);
    
    historyText = new JTextArea();
    historyText.setBackground(Color.white);
    historyText.setEditable(false);
    
    p.add("Center", new JScrollPane(historyText));

    JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topPanel, p);
    topPanel.setMinimumSize(new Dimension(1,1));
    add("Center", split);
  }
  
  public void actionPerformed(ActionEvent e)
  {
    if (e.getSource() == showHistory)
      {
	try
	  {
	    gc.setWaitCursor();
	    historyText.setText((gc.getSession().viewObjectHistory(invid, selectedDate)).toString());

	    if (selectedDate != null)
	      {
		titledBorder.setTitle("History: starting from " + selectedDate);
	      }
	    else
	      {
		titledBorder.setTitle("History");
	      }

	    gc.setNormalCursor();
	  }
	catch (RemoteException rx)
	  {
	    gc.setNormalCursor();
	    throw new RuntimeException("Could not get object history.");
	  }
      }
  }

  public boolean setValuePerformed(JValueObject e)
  {
    if (e.getSource() == selectDate)
      {
	Date value = (Date)e.getValue();
	selectedDate = value;
	return true;
      }

    return false;
  }

  /**
   *
   * This method is called to clean up any auxiliary windows when our
   * framePanel is closed in the client.
   * 
   */

  public void unregister()
  {
    if (selectDate != null)
      {
	selectDate.unregister();
      }
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                                      datesPanel

------------------------------------------------------------------------------*/

class datesPanel extends JPanel {

  static final boolean debug = false;

  string_field notes_field;

  JTextField
    createdBy,
    modifiedBy,
    createdOn,
    modifiedOn;

  GridBagLayout
    gbl;

  GridBagConstraints
    gbc;

  int row = 0;

  /* -- */
  
  public datesPanel(string_field    creator_field, 
		    date_field      creation_date_field,
		    string_field    modifier_field,
		    date_field      modification_date_field)
  {
    if (debug)
      {
	System.out.println("Creating notes panel");
      }

    String creator = null;
    Date creation_date = null;
    String modifier = null;
    Date mod_date = null;
    
    SimpleDateFormat dateformat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    /* -- */
    
    this.notes_field = notes_field;

    setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
    
    gbc = new GridBagConstraints();
    gbl = new GridBagLayout();
    setLayout(gbl);
    
    gbc.anchor = GridBagConstraints.NORTHWEST;
    gbc.insets = new Insets(6,6,6,6);
    
    try
      {
	if (creator_field != null)
	  {
	    creator = (String) creator_field.getValue();
	  }

	if (creation_date_field != null)
	  {
	    creation_date = (Date) creation_date_field.getValue();
	  }

	if (modifier_field != null)
	  {
	    modifier = (String) modifier_field.getValue();
	  }

	if (modification_date_field != null)
	  {
	    mod_date = (Date) modification_date_field.getValue();
	  }
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not get creation info: " + rx);
      }
    
    createdBy = new JTextField(30);
    
    if (creator == null)
      {
	createdBy.setText("No creator set for this object.");
      }
    else
      {
	createdBy.setText(creator);
      }

    addRow(createdBy, "Created By:");
    
    createdOn = new JTextField(30);
    
    if (creation_date == null)
      {
	createdOn.setText("No creation date has been set for this object.");
      }
    else
      {
	createdOn.setText(dateformat.format(creation_date));
      }
    
    addRow(createdOn, "Created On:");
    
    modifiedBy = new JTextField(30);

    if (modifier == null)
      {
	modifiedBy.setText("No information about the last modifier.");
      }
    else
      {
	modifiedBy.setText(modifier);
      }
    
    addRow(modifiedBy, "Modified By:");
    
    modifiedOn = new JTextField(30);

    if (mod_date == null)
      {
	modifiedOn.setText("No last modification date");
      }
    else
      {
	modifiedOn.setText(dateformat.format(mod_date));
      }
    
    addRow(modifiedOn, "Modified on:");
  }

  void addRow(JComponent comp, String title)
  {
    JLabel l = new JLabel(title);

    /* -- */
    
    gbc.weightx = 0.0;
    gbc.fill = GridBagConstraints.NONE;

    gbc.gridwidth = 1;
    gbc.gridx = 0;
    gbc.gridy = row;
    gbl.setConstraints(l, gbc);
    add(l);

    gbc.gridx = 1;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbl.setConstraints(comp, gbc);
    add(comp);

    row++;
  }
}
