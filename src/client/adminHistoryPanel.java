/*

   adminHistoryPanel.java

   The tab that holds history information.
   
   Created: 9 September 1997
   Release: $Name:  $
   Version: $Revision: 1.3 $
   Last Mod Date: $Date: 1999/03/17 05:31:47 $
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

import javax.swing.*;
import javax.swing.border.*;
import arlut.csd.ganymede.*;

import arlut.csd.JDataComponent.*;
import arlut.csd.JCalendar.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                               adminHistoryPanel

------------------------------------------------------------------------------*/

public class adminHistoryPanel extends JPanel implements ActionListener, JsetValueCallback{

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

  public adminHistoryPanel(Invid invid, gclient gc)
    {
      this.invid = invid;
      this.gc = gc;

      setLayout(new BorderLayout());
      
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

      JPanel p = new JPanel(new BorderLayout());
      titledBorder = new TitledBorder("History");
      p.setBorder(titledBorder);
      historyText = new JTextArea();
      historyText.setBackground(Color.white);
      historyText.setEditable(false);

      p.add("Center", new JScrollPane(historyText));


      add("North", topPanel);
      add("Center", p);
    }
  

  public void actionPerformed(ActionEvent e)
  {
    if (e.getActionCommand().equals("Show history"))
      {
	try
	  {
	    gc.setWaitCursor();
	    historyText.setText((gc.getSession().viewAdminHistory(invid, selectedDate)).toString());
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
	    throw new RuntimeException("Could not get object history. " + rx);
	  }

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

}
