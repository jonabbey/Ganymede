/*

   historyPanel.java

   The tab that holds history information.
   
   Created: 9 September 1997
   Version: $Revision: 1.5 $ %D%
   Module By: Michael Mulvaney
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.client;

import java.awt.*;
import java.awt.event.*;
import java.rmi.*;
import java.util.*;

import com.sun.java.swing.*;
import com.sun.java.swing.border.*;
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

  /* -- */

  public historyPanel(Invid invid, gclient gc)
  {
    this.invid = invid;
    this.gc = gc;

    setLayout(new BorderLayout());
      
    JPanel topPanel = new JPanel(false);
    
    JPanel leftPanel = new JPanel(false);
    leftPanel.setBorder(new TitledBorder("Restrict dates"));
    selectDate = new JButton("Set starting date");
    selectDate.addActionListener(this);
    leftPanel.add(selectDate);
    
    clearDate = new JButton("Clear date");
    clearDate.addActionListener(this);
    leftPanel.add(clearDate);
    
    topPanel.add(leftPanel);
      
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
    else if (e.getActionCommand().equals("Clear date"))
      {
	selectedDate = null;
      }
    else if (e.getActionCommand().equals("Set starting date"))
      {
	// show popup
	if (popupCal == null)
	  {
	    popupCal = new JpopUpCalendar(new GregorianCalendar(), this);
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
