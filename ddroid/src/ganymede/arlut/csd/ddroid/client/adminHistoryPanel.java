/*

   adminHistoryPanel.java

   The tab that holds history information.
   
   Created: 9 September 1997
   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

   Module By: Michael Mulvaney

   -----------------------------------------------------------------------
	    
   Directory Droid Directory Management System
 
   Copyright (C) 1996-2004
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

package arlut.csd.ddroid.client;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;
import java.rmi.RemoteException;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

import arlut.csd.JCalendar.JpopUpCalendar;
import arlut.csd.JDataComponent.JValueObject;
import arlut.csd.JDataComponent.JsetValueCallback;
import arlut.csd.ddroid.common.Invid;

/*------------------------------------------------------------------------------
                                                                           class
                                                               adminHistoryPanel

------------------------------------------------------------------------------*/

/**
 * <P>The adminHistoryPanel is used in the Directory Droid client when the user is
 * editing or viewing an admin persona object.  The adminHistoryPanel provides
 * the user with the ability to get a report of all actions taken by the admin
 * in question from the server's logs.</P>
 */

public class adminHistoryPanel extends JPanel implements ActionListener, JsetValueCallback{

  JPanel
    historyTextPanel;

  CardLayout
    historyTextCard = new CardLayout();

  JTextArea
    historyText;

  JpopUpCalendar
    popupCal = null;

  JButton
    clearDate,
    showHistory,
    selectDate;

  Invid
    invid;

  gclient gc;

  Date
    selectedDate;

  TitledBorder
    titledBorder;

  StringBuffer
    historyBuffer;

  /* -- */

  public adminHistoryPanel(Invid invid, gclient gc)
  {
    this.invid = invid;
    this.gc = gc;
    
    setLayout(new BorderLayout());
    
    historyTextPanel = new JPanel(historyTextCard);

    // create our fixed top panel

    JPanel topPanel = new JPanel(false);
    selectDate = new JButton("Set starting date");
    selectDate.addActionListener(this);
    topPanel.add(selectDate);
    
    clearDate = new JButton("Clear date");
    clearDate.addActionListener(this);
    topPanel.add(clearDate);
    
    showHistory = new JButton("Show history");
    showHistory.addActionListener(this);
    
    topPanel.add(showHistory);

    add("North", topPanel);

    // create our history-display panel, add it to our card layout

    historyTextPanel.setBorder(new TitledBorder("History"));

    JPanel p = new JPanel(new BorderLayout());
    historyText = new JTextArea();
    historyText.setBackground(Color.white);
    historyText.setEditable(false);
    
    p.add("Center", new JScrollPane(historyText));
    
    historyTextPanel.add("text", p);

    // create our wait-display panel, add it to our card layout

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

    // choose the text panel for now, until such time as we
    // go into wait mode

    historyTextCard.show(historyTextPanel, "text");

    // and add the whole thing

    add("Center", historyTextPanel);
  }

  public void actionPerformed(ActionEvent e)
  {
    if (e.getActionCommand().equals("Show history"))
      {
	loadHistory();
      }
    else if (e.getActionCommand().equals("Clear date"))
      {
	selectedDate = null;
      }
    else if (e.getActionCommand().equals("Set starting date"))
      {
	// show popup

	if (popupCal == null)
	  {
	    popupCal = new JpopUpCalendar(new GregorianCalendar(), this, true);
	  }

	popupCal.setVisible(true);
      }
  }

  public boolean setValuePerformed(JValueObject e)
  {
    if (e.getSource() == popupCal)
      {
	Date value = (Date)e.getValue();

	if (value.equals(selectedDate))
	  {
	    System.out.println("You are already looking at this one.");
	    return false;
	  }

	selectedDate = value;
      }

    return true;
  }

  public void showWait()
  {
    showHistory.setEnabled(false);
    historyTextCard.show(historyTextPanel, "wait");
  }

  public void showText(String text)
  {
    showHistory.setEnabled(true);
    historyTextCard.show(historyTextPanel, "text");
    historyText.setText(text);
  }

  public void loadHistory()
  {
    final adminHistoryPanel me = this;

    /* -- */

    Thread historyThread = new Thread(new Runnable() {
      public void run() {
	try
	  {
	    try
	      {
		SwingUtilities.invokeAndWait(new Runnable() {
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

	    historyBuffer = gc.getSession().viewAdminHistory(invid, selectedDate);
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Could not get object history.");
	  }
	finally
	  {
	    SwingUtilities.invokeLater(new Runnable() {
	      public void run() {
		me.showText(historyBuffer.toString());
	      }
	    });
	  }
      }}, "History loader thread");

    historyThread.start();
  }

}
