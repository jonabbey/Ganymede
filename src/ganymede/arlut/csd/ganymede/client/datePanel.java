/*

   datePanel.java

   A tab that holds date information, typically for expiration and/or
   removal fields.
   
   Created: 9 September 1997

   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   Last Mod Date: $Date$
   SVN URL: $HeadURL$

   Module By: Michael Mulvaney

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996 - 2008
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

package arlut.csd.ganymede.client;

import java.awt.BorderLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.border.EmptyBorder;

import arlut.csd.JCalendar.JpanelCalendar;
import arlut.csd.JDataComponent.JValueObject;
import arlut.csd.JDataComponent.JsetValueCallback;
import arlut.csd.Util.TranslationService;
import arlut.csd.ganymede.common.FieldTemplate;
import arlut.csd.ganymede.common.ReturnVal;
import arlut.csd.ganymede.rmi.date_field;

/*------------------------------------------------------------------------------
                                                                           class
                                                                       datePanel

------------------------------------------------------------------------------*/

/**
 * GUI date panel used in the Ganymede client to allow the display
 * and/or editing of the standard expiration and removal time fields
 * that are defined on all object types in the Ganymede server.
 *
 * The datePanel is contained within a {@link
 * arlut.csd.ganymede.client.framePanel framePanel} in the Ganymede
 * client.  See the {@link arlut.csd.ganymede.client.gclient gclient}
 * class for more information on the structure of the Ganymede client.
 */

public class datePanel extends JPanel implements ActionListener, JsetValueCallback, Runnable {

  final static boolean debug = false;

  /**
   * TranslationService object for handling string localization in the
   * Ganymede client.
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.client.datePanel");

  // ---

  boolean 
    editable;

  framePanel
    fp;

  gclient
    gc;

  date_field
    field;

  JPanel
    top_pane,
    bottom_pane;

  JpanelCalendar
    cal;

  JButton
    clear;

  FieldTemplate
    template;

  /**
   *
   * Display button used only in editable context.
   *
   */

  JButton    topButton;

  /**
   *
   * Display label used only in non-editable context.
   *
   */

  JLabel    topLabel;

  protected GregorianCalendar 
    my_Calendar;

  protected TimeZone
    _myTimeZone = SimpleTimeZone.getDefault();

  SimpleDateFormat
    dateformat;

  String
    name,
    label;

  JPanel
    holder,
    actual;

  JProgressBar 
    progressBar;

  /* -- */

  public datePanel(date_field field, FieldTemplate template, String label, boolean editable, framePanel fp)
  {
    this.editable = editable;
    this.field = field;
    this.label = label;
    this.fp = fp;
    this.template = template;

    gc = fp.wp.gc;

    setLayout(new BorderLayout());

    invalidate();
    fp.validate();

    Thread thread = new Thread(this);
    thread.setPriority(Thread.NORM_PRIORITY);
    thread.start();
  }

  public void run()
  {
    setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

    top_pane = new JPanel(false);
    top_pane.setLayout(new BorderLayout());
    top_pane.setMaximumSize(top_pane.getPreferredSize());

    dateformat = new SimpleDateFormat("MMM dd, yyyy",Locale.getDefault());

    if (editable)
      {
	create_editable_panel();
      }
    else
      {
	create_non_editable_panel();
      }

    if (debug)
      {
	System.out.println("Done with thread in datePanel.");
      }

    if (cal != null)  // in a non-editable the calendar might not be there
      {
	add(cal, "Center");
      }

    add(top_pane, "North");

    invalidate();
    fp.validate();
  }

  void create_editable_panel()
  {
    my_Calendar = new GregorianCalendar(_myTimeZone,Locale.getDefault());
    
    try
      {
	topButton = new JButton();
	topButton.setActionCommand("back");
	topButton.addActionListener(this);
	topButton.setToolTipText("Click here to return to this date");

	this.name = template.getName();

	if ((field != null) && (field.getValue() != null))
	  {
	    Date date = ((Date)field.getValue());
	    my_Calendar.setTime(date);

	    // "{0}: {1}"
	    topButton.setText(ts.l("global.short_pattern",
				   label,
				   dateformat.format(date)));
	  }
	else
	  {
	    // "{0} has not been set."
	    topButton.setText(ts.l("global.not_set_pattern", label));
	  }
	
	cal = new JpanelCalendar(my_Calendar, this, true, true);

	top_pane.add(topButton, "Center");

	// "Clear date"
	clear = new JButton(ts.l("create_editable_panel.clear_button"));
	clear.setActionCommand("Clear");
	clear.addActionListener(this);

	if (debug)
	  {
	    System.out.println("adding clear button to top_pane");
	  }

	top_pane.add(clear, "East");
      }
    catch (Exception rx)
      {
	gclient.client.processExceptionRethrow(rx);
      }
  }

  void create_non_editable_panel()
  {
    my_Calendar = new GregorianCalendar(_myTimeZone,Locale.getDefault());
    
    try
      {
	topLabel = new JLabel();
	topLabel.setBorder(new EmptyBorder(new Insets(5,1,5,1)));

	this.name = template.getName();

	if ((field != null) && (field.getValue() != null))
	  {
	    Date date = ((Date)field.getValue());
	    my_Calendar.setTime(date);

	    // "{0}: {1}"
	    topLabel.setText(ts.l("global.short_pattern",
				  label,
				  dateformat.format(date)));
	  }
	else
	  {
	    // "{0} has not been set."
	    topLabel.setText(ts.l("global.not_set_pattern", label));
	  }
	
	cal = new JpanelCalendar(my_Calendar, this, true, false); // non-editable

	top_pane.add(topLabel, "Center");
      }
    catch (Exception rx)
      {
	gclient.client.processExceptionRethrow(rx, "Could not get date in datePanel: ");
      }
  }
  
  public void actionPerformed(ActionEvent e)
  {
    ReturnVal retVal;

    /* -- */

    if (debug)
      {
	System.out.println("Action performed in datePanel");
      }

    if (e.getActionCommand().equals("Clear"))
      {
	boolean ok = false;

	try
	  {
	    retVal = field.setValue(null);

	    ok = (retVal == null) ? true : retVal.didSucceed();

	    if (retVal != null)
	      {
		gc.handleReturnVal(retVal);
	      }
	  }
	catch (Exception rx)
	  {
	    gc.processExceptionRethrow(rx);
	  }

	if (ok)
	  {
	    cal.clear();
	    fp.wp.getgclient().somethingChanged();
	  }
	else
	  {
	    // "Server says:  Could not clear date field."
	    setStatus(ts.l("actionPerformed.failed_status"));
	  }
      }

    if (e.getActionCommand().equals("back"))
      {
	cal.displaySelectedPage();
      }
  }

  public boolean setValuePerformed(JValueObject o)
  {
    ReturnVal retVal;
    boolean ok = false;

    /* -- */

    if (o.getSource() == cal)
      {
	Date d = (Date)o.getValue();

	if (debug)
	  {
	    System.out.println("Removal Calendar says: " + d.toString());
	  }

	try
	  {
	    retVal = field.setValue(d);

	    ok = (retVal == null) ? true : retVal.didSucceed();

	    if (retVal != null)
	      {
		gc.handleReturnVal(retVal);
	      }
	  }
	catch (Exception rx)
	  {
	    gc.processExceptionRethrow(rx);
	  }
	
	if (ok)
	  {
	    if (editable)
	      {
		// "{0}: {1}"
		topButton.setText(ts.l("global.short_pattern",
				       label,
				       dateformat.format(d)));
	      }
	    else
	      {
		// "{0}: {1}"
		topLabel.setText(ts.l("global.short_pattern",
				      label,
				      dateformat.format(d)));
	      }
	  }
      }

    if (ok)
      {
	gc.somethingChanged();
      }

    return ok;
  }

  /**
   *
   * This method is used to refresh the date held and/or displayed
   * in this date panel.
   *
   */

  public void setDate(Date newDate)
  {
    String newLabel;

    /* -- */

    if (newDate != null)
      {
	// "{0}: {1}"
	newLabel = ts.l("global.short_pattern",
			label,
			dateformat.format(newDate));
      }
    else
      {
	// "No date is set"
	newLabel = ts.l("setDate.no_date_label");
      }

    if (cal != null)
      {
	cal.setDate(newDate);

	if (editable)
	  {
	    topButton.setText(newLabel);
	  }
      }
    else
      {
	topLabel.setText(newLabel);
      }
  }

  /**
   *
   * This method is called to refresh this field
   *
   */

  public void refresh()
  {
    if (field == null)
      {
	throw new IllegalArgumentException("Don't have date field.");
      }

    try
      {
	Date date = ((Date)field.getValue());

	if (debug)
	  {
	    System.err.println("datePanel: resetting date to " + date);
	  }
	
	this.setDate(date);
      }
    catch (Exception ex)
      {
	gc.processExceptionRethrow(ex);
      }
  }

  private final void setStatus(String s)
  {
    gc.setStatus(s);
  }

  public void dispose()
  {
    removeAll();
    fp = null;
    gc = null;
    field = null;

    if (top_pane != null)
      {
	top_pane.removeAll();
	top_pane = null;
      }

    if (bottom_pane != null)
      {
	bottom_pane.removeAll();
	bottom_pane = null;
      }

    cal = null;
    clear = null;
    template = null;
  }

}//datePanel
