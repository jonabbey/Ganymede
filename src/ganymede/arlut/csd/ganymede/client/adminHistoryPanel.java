/*

   adminHistoryPanel.java

   The tab that holds history information.
   
   Created: 9 September 1997
   Last Commit: $Format:%cd$

   Module By: Michael Mulvaney

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
import java.util.Date;
import java.util.GregorianCalendar;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.TitledBorder;

import arlut.csd.JCalendar.JpopUpCalendar;
import arlut.csd.JDataComponent.JValueObject;
import arlut.csd.JDataComponent.JResetDateObject;
import arlut.csd.JDataComponent.JSetValueObject;
import arlut.csd.JDataComponent.JsetValueCallback;
import arlut.csd.Util.TranslationService;
import arlut.csd.ganymede.common.Invid;
import arlut.csd.ganymede.common.ReturnVal;
import arlut.csd.ganymede.common.SchemaConstants;
import arlut.csd.ganymede.rmi.db_object;

/*------------------------------------------------------------------------------
                                                                           class
                                                               adminHistoryPanel

------------------------------------------------------------------------------*/

/**
 * The adminHistoryPanel is used in the Ganymede client when the user
 * is editing or viewing an admin persona object.  The
 * adminHistoryPanel provides the user with the ability to get a
 * report of all actions taken by the admin in question from the
 * server's logs.
 */

public class adminHistoryPanel extends JPanel implements ActionListener, JsetValueCallback{

  /**
   * TranslationService object for handling string localization in the
   * Ganymede client.
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.client.adminHistoryPanel");

  JPanel
    historyTextPanel;

  /**
   * We use a CardLayout so that we can display a 'man at work'
   * animated icon during history retrieval, then switch over to a
   * JScrollPane containing the retrieved history text once the
   * history retrieval operation is complete.
   */

  CardLayout
    historyTextCard = new CardLayout();

  JTextArea
    historyText;

  JpopUpCalendar
    popupCal = null;

  JButton
    showHistory,
    selectDate;

  Invid
    invid;

  gclient gc;

  Date
    createdDate,
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

    final Invid myInvid = invid;
    final gclient myGc = gc;

    try
      {
	try
	  {
	    createdDate = (Date) foxtrot.Worker.post(new foxtrot.Task()
	      {
		public Object run() throws Exception
		{
		  ReturnVal retVal = myGc.getSession().view_db_object(myInvid);
		  db_object adminObj = retVal.getObject();
		  
		  return adminObj.getFieldValue(SchemaConstants.CreationDateField);
		}
	      });
	  }
	catch (java.security.AccessControlException ex)
	  {
	    ReturnVal retVal = myGc.getSession().view_db_object(myInvid);
	    db_object adminObj = retVal.getObject();

	    createdDate = (Date) adminObj.getFieldValue(SchemaConstants.CreationDateField);
	  }
      }
    catch (Exception rx)
      {
        gc.processExceptionRethrow(rx, "Could not get admin persona to look at.");
      }

    selectedDate = createdDate;
    
    setLayout(new BorderLayout());
    
    historyTextPanel = new JPanel(historyTextCard);

    // create our fixed top panel

    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.anchor = GridBagConstraints.WEST;
    gbc.insets = new Insets(0,2,0,2);
    gbc.fill = GridBagConstraints.NONE;

    JPanel topPanel = new JPanel(gbl);

    // "Set starting date"
    selectDate = new JButton(ts.l("init.start_date_button"));
    selectDate.setActionCommand("Set starting date");
    selectDate.addActionListener(this);
    gbc.gridx = 0;
    gbl.setConstraints(selectDate, gbc);
    topPanel.add(selectDate);

    // "Show history"
    showHistory = new JButton(ts.l("init.show_history_button"));
    showHistory.setActionCommand("Show history");
    showHistory.addActionListener(this);
    gbc.gridx = 1;
    gbl.setConstraints(showHistory, gbc);
    topPanel.add(showHistory);

    add("North", topPanel);

    // create our history-display panel, add it to our card layout

    // "History"
    historyTextPanel.setBorder(new TitledBorder(ts.l("init.history_border")));

    historyText = new JTextArea();
    historyText.setBackground(Color.white);
    historyText.setEditable(false);
    
    historyTextPanel.add("text", new JScrollPane(historyText));

    // create our wait-display panel, add it to our card layout

    ImageIcon waitImage = new ImageIcon(gc.wp.getWaitImage());
    JLabel waitLabel = new JLabel(waitImage);

    JPanel waitPanel = new JPanel(new BorderLayout());
    waitPanel.setBackground(java.awt.Color.white);

    // "Waiting for history from server..."
    JLabel waitText = new JLabel(ts.l("init.waiting_label"));
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
    else if (e.getActionCommand().equals("Set starting date"))
      {
	// show popup

	if (popupCal == null)
	  {
            GregorianCalendar cal = new GregorianCalendar();
            cal.setTime(selectedDate);

	    popupCal = new JpopUpCalendar(gc, cal, this, true);
	  }

	popupCal.setVisible(true);
      }
  }

  public boolean setValuePerformed(JValueObject e)
  {
    if (e.getSource() != popupCal)
      {
        return true;            // er, sure, whatever
      }

    if (e instanceof JSetValueObject)
      {
	Date value = (Date)e.getValue();

        if (value.before(createdDate))
          {
            return false;
          }

	if (value.equals(selectedDate))
	  {
	    return false;
	  }

	selectedDate = value;
      }
    else if (e instanceof JResetDateObject)
      {
        JResetDateObject resetObject = (JResetDateObject) e;

        resetObject.setTransformedDate(createdDate);
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
    showWait();

    try
      {
	try
	  {
	    historyBuffer = (StringBuffer) foxtrot.Worker.post(new foxtrot.Task()
	      {
		public Object run() throws Exception
		{
		  return gc.getSession().viewAdminHistory(invid, selectedDate);
		}
	      });
	  }
	catch (java.security.AccessControlException ex)
	  {
	    historyBuffer = gc.getSession().viewAdminHistory(invid, selectedDate);
	  }
      }
    catch (Exception rx)
      {
        gc.processExceptionRethrow(rx, "Could not get object history.");
      }
    finally
      {
        showText(historyBuffer.toString());
      }
  }

  public void dispose()
  {
    removeAll();

    if (historyTextPanel != null)
      {
	historyTextPanel.removeAll();
	historyTextPanel = null;
      }

    historyTextCard = null;
    historyText = null;
    popupCal = null;
    showHistory = null;
    selectDate = null;
    invid = null;
    gc = null;
    selectedDate = null;
    titledBorder = null;
    historyBuffer = null;
  }
}
