/*
   historyPanel.java

   The tab that holds history information.
   
   Created: 9 September 1997
   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

   Module By: Michael Mulvaney

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996-2005
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

package arlut.csd.ganymede.client;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.border.TitledBorder;

import arlut.csd.JDataComponent.JValueObject;
import arlut.csd.JDataComponent.JdateField;
import arlut.csd.JDataComponent.JsetValueCallback;
import arlut.csd.ganymede.common.Invid;
import arlut.csd.ganymede.rmi.date_field;
import arlut.csd.ganymede.rmi.string_field;

/*------------------------------------------------------------------------------
                                                                           class
                                                                    historyPanel

------------------------------------------------------------------------------*/

public class historyPanel extends JPanel implements ActionListener, JsetValueCallback {

  JTextArea
    historyText;

  JButton
    showHistory,
    showFullHistory;

  JPanel
    historyTextPanel;

  CardLayout
    historyTextCard = new CardLayout();

  JdateField
    selectDate;

  TitledBorder
    titledBorder;

  Invid
    invid;

  gclient gc;

  Date
    selectedDate = null;

  StringBuffer
    historyBuffer;

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

    showHistory = new JButton("Show history");
    showHistory.setToolTipText("Show all changes made to this specific object");
    showHistory.addActionListener(this);
    
    buttonPanel.add(showHistory);

    showFullHistory = new JButton("Show Transactional History");
    showFullHistory.setToolTipText("Show all transactions in which this object was changed");
    showFullHistory.addActionListener(this);
    
    buttonPanel.add(showFullHistory);

    //    JPanel rightPanel = new JPanel(false);
    //    rightPanel.add(new JLabel("Since:"));
    //    selectDate = new JdateField();
    //    selectDate.setCallback(this);
    //    rightPanel.add(selectDate);
    
    // buttonPanel.add(rightPanel);

    JPanel midPanel = new JPanel(new BorderLayout());
    midPanel.add("West",  new datesPanel(creator_field, creation_date_field, 
					 modifier_field, modification_date_field));;

    topPanel.add("North", midPanel);
    topPanel.setBorder(new TitledBorder("Creation/Modification"));
    
    JPanel p = new JPanel(new BorderLayout());
    titledBorder = new TitledBorder("Detailed History");
    p.setBorder(titledBorder);
    p.add("North", buttonPanel);
    
    historyText = new JTextArea();
    historyText.setBackground(Color.white);
    historyText.setEditable(false);

    historyTextPanel = new JPanel(historyTextCard);
    historyTextPanel.add("text", new JScrollPane(historyText));

    ImageIcon waitImage = new ImageIcon(gc.wp.getWaitImage());
    JLabel waitLabel = new JLabel(waitImage);

    JPanel waitPanel = new JPanel(new BorderLayout());
    waitPanel.setBackground(java.awt.Color.white);

    JLabel waitText = new JLabel("Waiting for history from server...");
    waitText.setForeground(java.awt.Color.black);
    waitText.setFont(Font.getFont("Courier"));

    JPanel topwaitPanel = new JPanel(new FlowLayout());
    topwaitPanel.setBackground(java.awt.Color.white);
    topwaitPanel.add(waitText);

    waitPanel.add("North", topwaitPanel);
    waitPanel.add("Center", waitLabel);
    waitPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(java.awt.Color.black),
							   BorderFactory.createEmptyBorder(5,5,5,5)));

    historyTextPanel.add("wait", waitPanel);

    historyTextCard.show(historyTextPanel, "text");

    p.add("Center", historyTextPanel);

    JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topPanel, p);
    split.setOneTouchExpandable(true);
    //    topPanel.setMinimumSize(new Dimension(1,1));
    //    split.setDividerLocation(100);
    add("Center", split);
  }
  
  public void actionPerformed(ActionEvent e)
  {
    if (e.getSource() == showHistory || e.getSource() == showFullHistory)
      {
	loadHistory(e.getSource() == showFullHistory);
      }
  }

  public void showWait()
  {
    showHistory.setEnabled(false);
    showFullHistory.setEnabled(false);
    historyTextCard.show(historyTextPanel, "wait");
  }

  public void showText(String text)
  {
    showHistory.setEnabled(true);
    showFullHistory.setEnabled(true);
    historyTextCard.show(historyTextPanel, "text");
    historyText.setText(text);
  }

  public void loadHistory(boolean fullHistory)
  {
    final historyPanel me = this;
    final boolean showAll = fullHistory;

    /* -- */

    Thread historyThread = new Thread(new Runnable() {
      public void run() {
	try
	  {
	    try
	      {
		EventQueue.invokeAndWait(new Runnable() {
		  public void run() {
		    me.showWait();
		  }
		});
	      }
	    catch (InvocationTargetException ite)
	      {
		ite.printStackTrace();
	      }
	    catch (InterruptedException ie)
	      {
		ie.printStackTrace();
	      }

	    historyBuffer = gc.getSession().viewObjectHistory(invid, selectedDate, showAll);
	  }
	catch (Exception rx)
	  {
	    gc.processExceptionRethrow(rx, "Could not get object history.");
	  }
	finally
	  {
	    EventQueue.invokeLater(new Runnable() {
	      public void run() {
		me.showText(historyBuffer.toString());
	      }
	    });
	  }
      }}, "History loader thread");

    historyThread.setPriority(Thread.NORM_PRIORITY);
    historyThread.start();
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


  /**
   * This method is called to break down this historyPanel object.
   * Any auxiliary windows open will be closed as well.
   */


  public void dispose()
  {
    removeAll();
    historyText = null;
    showHistory = null;
    showFullHistory = null;

    if (historyTextPanel != null)
      {
	historyTextPanel.removeAll();
	historyTextPanel = null;
      }

    if (selectDate != null)
      {
	selectDate.unregister();
	selectDate = null;
      }

    titledBorder = null;
    invid = null;
    gc = null;
    selectedDate = null;
    historyBuffer = null;
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                                      datesPanel

------------------------------------------------------------------------------*/

/**
 * <p>Component panel used in the Ganymede client to hold modification and creation
 * date information in the client's
 * {@link arlut.csd.ganymede.client.historyPanel historyPanel} tab component.</p>
 *
 */

class datesPanel extends JPanel {

  static final boolean debug = false;

  string_field notes_field;

  JLabel
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
    
    SimpleDateFormat dateformat = new SimpleDateFormat();

    /* -- */
    
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
    catch (Exception rx)
      {
	gclient.client.processExceptionRethrow(rx, "Could not get object creation info: ");
      }
    
    if (creator == null)
      {
	createdBy = new JLabel("No creator set for this object.");
      }
    else
      {
	createdBy = new JLabel(creator);
      }

    addRow(createdBy, "Created By:");
    
    if (creation_date == null)
      {
	createdOn = new JLabel("No creation date has been set for this object.");
      }
    else
      {
	createdOn = new JLabel(dateformat.format(creation_date));
      }
    
    addRow(createdOn, "Created On:");
    
    if (modifier == null)
      {
	modifiedBy = new JLabel("No information about the last modifier.");
      }
    else
      {
	modifiedBy = new JLabel(modifier);
      }
    
    addRow(modifiedBy, "Last Modified By:");
    
    if (mod_date == null)
      {
	modifiedOn = new JLabel("No last modification date");
      }
    else
      {
	modifiedOn = new JLabel(dateformat.format(mod_date));
      }
    
    addRow(modifiedOn, "Last Modified on:");
  }

  void addRow(JLabel comp, String title)
  {
    JLabel l = new JLabel(title);

    /* -- */
    
    //    comp.setBorder(BorderFactory.createLineBorder(java.awt.Color.black));
    //    comp.setBackground(java.awt.Color.white);
    //    comp.setForeground(java.awt.Color.black);
    //    comp.setOpaque(true);

    comp.setForeground(java.awt.Color.black);
    comp.setFont(Font.getFont("Courier"));

    gbc.gridwidth = 1;
    gbc.gridy = row;

    gbc.gridx = 0;
    //    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.NONE;

    gbl.setConstraints(l, gbc);
    add(l);

    gbc.gridx = 1;
    //    gbc.weightx = 4.0;
    //    gbc.fill = GridBagConstraints.HORIZONTAL;

    gbl.setConstraints(comp, gbc);
    add(comp);

    row++;
  }
}
