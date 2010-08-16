/*
   historyPanel.java

   Tab panel for the Ganymede client that shows historical metadata
   and allows retrieval of history text for object windows.
   
   Created: 9 September 1997

   Module By: Michael Mulvaney

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996-2010
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
   along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/

package arlut.csd.ganymede.client;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
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
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.border.TitledBorder;

import arlut.csd.JDataComponent.JValueObject;
import arlut.csd.JDataComponent.JResetDateObject;
import arlut.csd.JDataComponent.JSetValueObject;
import arlut.csd.JDataComponent.JdateField;
import arlut.csd.JDataComponent.JsetValueCallback;
import arlut.csd.Util.TranslationService;
import arlut.csd.ganymede.common.Invid;
import arlut.csd.ganymede.common.SchemaConstants;
import arlut.csd.ganymede.rmi.date_field;
import arlut.csd.ganymede.rmi.string_field;


/*------------------------------------------------------------------------------
                                                                           class
                                                                    historyPanel

------------------------------------------------------------------------------*/

/**
 * historyPanel is the the panel class that implements the contents of
 * the history tab in the client's framePanel windows.
 */

public class historyPanel extends JPanel implements ActionListener, JsetValueCallback {

  /**
   * TranslationService object for handling string localization in the
   * Ganymede client.
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.client.historyPanel");

  private JTextArea
    historyText;

  private JComboBox
    choiceBox;

  private JdateField
    startDateField;

  private JButton
    showHistory;

  private JPanel
    historyTextPanel;

  /**
   * We use a CardLayout so that we can display a 'man at work'
   * animated icon during history retrieval, then switch over to a
   * JScrollPane containing the retrieved history text once the
   * history retrieval operation is complete.
   */

  private CardLayout
    historyTextCard = new CardLayout();

  private Invid
    invid;

  private gclient gc;

  private Date
    createdDate = null,
    selectedDate = null;

  private StringBuffer
    historyBuffer = new StringBuffer();

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

    final date_field myCreation_date_field = creation_date_field;

    createdDate = (Date) FoxtrotAdapter.post(new foxtrot.Task()
      {
	public Object run() throws Exception
	{
	  return myCreation_date_field.getValue();
	}
      });

    selectedDate = createdDate;

    setLayout(new BorderLayout());

    JPanel topPanel = new JPanel(new BorderLayout());

    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.anchor = GridBagConstraints.WEST;
    gbc.insets = new Insets(0,2,0,2);
    gbc.fill = GridBagConstraints.NONE;

    JPanel fillPanel = new JPanel(new BorderLayout());

    JPanel buttonPanel = new JPanel(gbl);

    String[] choices = null;

    if (invid.getType() == SchemaConstants.PersonaBase)
      {
	choices = new String[3];

	choices[0] = ts.l("global.show_history"); // "Show History"
	choices[1] = ts.l("global.show_trans_history");	// "Show Transactional History"
	choices[2] = ts.l("global.show_admin_history");	// "Show Admin History"
      }
    else
      {
	choices = new String[2];

	choices[0] = ts.l("global.show_history"); // "Show History"
	choices[1] = ts.l("global.show_trans_history");	// "Show Transactional History"
      }

    int index = 0;

    choiceBox = new JComboBox(choices);
    choiceBox.setSelectedIndex(0);
    gbc.gridx = index++;
    gbl.setConstraints(choiceBox, gbc);
    buttonPanel.add(choiceBox);

    // "since:"
    JLabel startLabel = new JLabel(ts.l("init.since"));
    gbc.gridx = index++;
    gbl.setConstraints(startLabel, gbc);
    buttonPanel.add(startLabel);

    startDateField = new JdateField(createdDate, true, true, false, createdDate, new Date());
    gbc.gridx = index++;
    gbl.setConstraints(startDateField, gbc);
    buttonPanel.add(startDateField);


    // "Go"
    showHistory = new JButton(ts.l("init.show_history_button"));
    showHistory.addActionListener(this);
    gbc.gridx = index++;
    gbl.setConstraints(showHistory, gbc);
    buttonPanel.add(showHistory);

    JPanel midPanel = new JPanel(new BorderLayout());
    midPanel.add("West",  new datesPanel(creator_field, creation_date_field, 
					 modifier_field, modification_date_field));;

    topPanel.add("North", midPanel);
    // "Creation/Modification"
    topPanel.setBorder(new TitledBorder(ts.l("init.top_panel_border")));
    
    JPanel p = new JPanel(new BorderLayout());

    // "Detailed History"
    p.setBorder(new TitledBorder(ts.l("init.bottom_panel_border")));
    fillPanel.add("West", buttonPanel);
    p.add("North", fillPanel);
    
    historyText = new JTextArea();
    historyText.setEditable(false);

    historyTextPanel = new JPanel(historyTextCard);
    historyTextPanel.add("text", new JScrollPane(historyText));

    ImageIcon waitImage = new ImageIcon(gc.wp.getWaitImage());
    JLabel waitLabel = new JLabel(waitImage);

    JPanel waitPanel = new JPanel(new BorderLayout());

    // "Waiting for history from server..."
    JLabel waitText = new JLabel(ts.l("init.waiting_text"));

    JPanel topwaitPanel = new JPanel(new FlowLayout());
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

    add("Center", split);
  }
  
  public void actionPerformed(ActionEvent e)
  {
    if (e.getSource() == showHistory)
      {
	loadHistory();
      }
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
	String choice = (String) choiceBox.getSelectedItem();

	if (choice.equals(ts.l("global.show_history")) ||
	    choice.equals(ts.l("global.show_trans_history")))
	  {
	    final boolean showAll = choice.equals(ts.l("global.show_trans_history"));

	    historyBuffer = (StringBuffer) FoxtrotAdapter.post(new foxtrot.Task()
	      {
		public Object run() throws Exception
		{
		  // JAMES MODIFY
		  return gc.getSession().viewObjectHistory(invid, startDateField.getDate(), showAll);
		}
	      });

	    showText(historyBuffer.toString());
	  }
	else if (choice.equals(ts.l("global.show_admin_history")))
	  {
	    historyBuffer = (StringBuffer) FoxtrotAdapter.post(new foxtrot.Task()
	      {
		public Object run() throws Exception
		{
		  return gc.getSession().viewObjectHistory(invid, startDateField.getDate());
		}
	      });
	  }
      }
    finally
      {
	showText(historyBuffer.toString());
      }
  }

  // JAMES NOTE, must incorporate this also....hmmmm...
  public boolean setValuePerformed(JValueObject e)
  {
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

  /**
   * This method is called to break down this historyPanel object.
   * Any auxiliary windows open will be closed as well.
   */


  public void dispose()
  {
    removeAll();
    historyText = null;
    showHistory = null;

    if (historyTextPanel != null)
      {
	historyTextPanel.removeAll();
	historyTextPanel = null;
      }

    startDateField = null;
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
 * Component panel used in the Ganymede client to hold modification
 * and creation date information in the client's {@link
 * arlut.csd.ganymede.client.historyPanel historyPanel} tab component.
 */

class datesPanel extends JPanel {

  static final boolean debug = false;

  /**
   * TranslationService object for handling string localization in the
   * Ganymede client.
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.client.datesPanel");

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
	// "No Creator set for this object."
	createdBy = new JLabel(ts.l("init.no_creator"));
      }
    else
      {
	createdBy = new JLabel(creator);
      }

    // "Created By:"
    addRow(createdBy, ts.l("init.created_by"));
    
    if (creation_date == null)
      {
	// "No Creation Date has been set for this object."
	createdOn = new JLabel(ts.l("init.no_creation_date"));
      }
    else
      {
	createdOn = new JLabel(ts.l("init.time_date_format", creation_date));
      }

    // "Created On:"
    addRow(createdOn, ts.l("init.created_on"));
    
    if (modifier == null)
      {
	// "No information about the last modifier."
	modifiedBy = new JLabel(ts.l("init.no_modifier"));
      }
    else
      {
	modifiedBy = new JLabel(modifier);
      }

    // "Last Modified By:"
    addRow(modifiedBy, ts.l("init.last_modified_by"));
    
    if (mod_date == null)
      {
	// "No last modification date"
	modifiedOn = new JLabel(ts.l("init.no_modification_date"));
      }
    else
      {
	modifiedOn = new JLabel(ts.l("init.time_date_format", mod_date));
      }

    // "Last Modified On:"
    addRow(modifiedOn, ts.l("init.last_modified_on"));
  }

  void addRow(JLabel comp, String title)
  {
    JLabel l = new JLabel(title);

    /* -- */
    
    gbc.gridwidth = 1;
    gbc.gridy = row;

    gbc.gridx = 0;
    gbc.fill = GridBagConstraints.NONE;

    gbl.setConstraints(l, gbc);
    add(l);

    gbc.gridx = 1;

    gbl.setConstraints(comp, gbc);
    add(comp);

    row++;
  }
}
