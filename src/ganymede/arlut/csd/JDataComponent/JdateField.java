
/*
   JdateField.java

   This class defines a date input field object.

   Created: 31 Jul 1996

   Module By: Navin Manohar

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2010
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
   along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/

package arlut.csd.JDataComponent;

import java.awt.BorderLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.rmi.RemoteException;

import java.text.ParseException;

import java.util.Calendar;
import java.util.Date;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.JFormattedTextField;
import javax.swing.JTextField;

import org.jdesktop.swingx.JXDatePicker;
import org.jdesktop.swingx.JXMonthView;
import org.jdesktop.swingx.plaf.basic.SpinningCalendarHeaderHandler;
import org.jdesktop.swingx.plaf.basic.CalendarHeaderHandler;

import arlut.csd.JDialog.StandardDialog;
import arlut.csd.JDialog.JErrorDialog;
import arlut.csd.Util.PackageResources;
import arlut.csd.Util.TranslationService;

/*------------------------------------------------------------------------------
                                                                           class
                                                                      JdateField

------------------------------------------------------------------------------*/

/**
 * This class defines a Date/Time GUI component that ties into the
 * JsetValueCallback that the Ganymede clients use internally.
 */

public class JdateField extends JPanel implements ActionListener, FocusListener
{
  static final boolean debug = false;

  /**
   * TranslationService object for handling string localization in the
   * Ganymede client.
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.JDataComponent.JdateField");

  // ---

  /**
   * The SwingX date picker GUI component we're building JdateField around.
   */

  private JXDatePicker datePicker;

  /**
   * datePicker's internal text editing field, that we apply a
   * FocusListener to.
   */

  private JFormattedTextField datef;

  /**
   * A text editing component for the time of day for this JdateField,
   * if we are set up to provide time display/editing.
   */

  private JTextField timef;

  /**
   * The callback that we use to communicate date changes in this
   * field to the Ganymede client, etc.
   */

  private JsetValueCallback callback = null;

  /**
   * The last date that we had from the Ganymede server.  We will
   * revert to this date if our callback refuses our change.
   */

  private Date original_date;

  /**
   * The date that we most recently have from our GUI components.
   * This will be promoted into original_date once a callback carrying
   * this date succeeds.
   */

  private Date curr_date;

  /* -- */

  /**
   * Minimal Constructor for JdateField.  This will construct a JdateField
   * with no value.
   */

  public JdateField()
  {
    this(null,true,false,true,null,null);
  }

  /**
   * Contructor that creates a JdateField based on the Date object it is given.  This
   * constructor can be used if the JdateField will be making callbacks to pass its data
   * to the appropriate container.
   *
   * @param parent the container which implements the callback function for this JdateField
   * @param date the Date object to use
   * @param iseditable true if the datefield can be edited by the user
   * @param islimited true if there is to be a restriction on the range of dates
   * @param usetime If true, this JdateField will display a time edit box next to the date edit field.
   * @param minDate the oldest possible date that can be entered into this JdateField
   * @param maxDate the newest possible date that can be entered into this JdateField
   */

  public JdateField(Date date,
		    boolean iseditable,
		    boolean islimited,
		    boolean usetime,
		    Date minDate,
		    Date maxDate,
		    JsetValueCallback parent)
  {
    this(date,iseditable,islimited,usetime,minDate,maxDate);

    setCallback(parent);
  }

  /**
   * Contructor that creates a JdateField based on the date it is given.  It is also
   * possible to set restrictions on the range of dates for this JdateField when
   * using this constructor
   *
   * @param date the Date object to use
   * @param islimited true if there is to be a restriction on the range of dates
   * @param usetime If true, this JdateField will display a time edit box next to the date edit field.
   * @param minDate the oldest possible date that can be entered into this JdateField
   * @param maxDate the newest possible date that can be entered into this JdateField
   */

  public JdateField(Date date,
		    boolean iseditable,
		    boolean islimited,
		    boolean usetime,
		    Date minDate,
		    Date maxDate)
  {
    if (debug)
      {
	System.err.println("JdateField(): date = " + date);
      }

    if (date == null)
      {
	curr_date = original_date = null;
      }
    else
      {
	curr_date = original_date = new Date(date.getTime());
      }

    if (islimited)
      {
	if (minDate == null)
	  {
	    throw new IllegalArgumentException("Invalid Parameter: minDate cannot be null");
	  }

	if (maxDate == null)
	  {
	    throw new IllegalArgumentException("Invalid Parameter: maxDate canot be null");
	  }
      }

    setLayout(new BorderLayout());

    JPanel buttonPanel = new JPanel();
    buttonPanel.setLayout(new BorderLayout());

    // Adds a year spinner to the MonthView object.
    UIManager.put(CalendarHeaderHandler.uiControllerID,
		  "org.jdesktop.swingx.plaf.basic.SpinningCalendarHeaderHandler");

    // Moves the year spinner after month arrows.
    UIManager.put(SpinningCalendarHeaderHandler.ARROWS_SURROUND_MONTH, Boolean.TRUE);

    datePicker = new JXDatePicker(date);
    datePicker.setName("datePicker");
    datePicker.setEditable(iseditable);
    datePicker.addActionListener(this);

    JXMonthView monthView = datePicker.getMonthView();

    if (minDate != null)
      {
	monthView.setLowerBound(minDate);
      }

    if (maxDate != null)
      {
	monthView.setUpperBound(maxDate);
      }

    monthView.setZoomable(true);

    datef = datePicker.getEditor();

    if (iseditable)
      {
	datef.addFocusListener(this);
      }

    buttonPanel.add(datePicker, "West");

    // Add in a time input.

    timef = new JTextField(5);
    timef.setEditable(iseditable);

    // Add focus listener.

    if (usetime)
      {
	if (iseditable)
	  {
	    timef.addFocusListener(this);
	  }

	add(timef);
      }

    add(buttonPanel, "East");

    setDate(curr_date);

    invalidate();
    validate();
  }

  /**
   * FocusListener method to react to focus loss on the datePicker
   * and/or timef text editing widgets.
   */

  public void focusLost(FocusEvent e)
  {
    Object c = e.getSource();

    if (c == timef)
      {
	setTimeOnly(timef.getText());

	updateServer();
      }
    else if (c == datef)
      {
	try
	  {
	    datePicker.getEditor().commitEdit();

	    setDateOnly(datePicker.getDate());

	    updateServer();
	  }
	catch (ParseException pe)
	  {
	  }
      }
  }

  /**
   * Required by the FocusListener interface.
   */

  public void focusGained(FocusEvent e)
  {
  }

  /**
   * ActionListener method we use to trigger on mouse clicks on the drop-down calendar widget.
   */

  public void actionPerformed(ActionEvent e)
  {
    Object c = e.getSource();

    if (c == datePicker)
      {
	setDateOnly(datePicker.getDate());
	updateServer();
      }
  }

  /**
   * Returns the date associated with this JdateField
   */

  public Date getDate()
  {
    return curr_date;
  }

  /**
   * Sets the minimum and maximum dates allowed in this JdateField.
   */

  public void setLimits(Date minDate, Date maxDate)
  {
    JXMonthView monthView = datePicker.getMonthView();

    if (minDate != null)
      {
	monthView.setLowerBound(minDate);
      }

    if (maxDate != null)
      {
	monthView.setUpperBound(maxDate);
      }
  }

  /**
   * Sets the calendar date of this JdateField, keeping the time of
   * day previously set.
   *
   * Calling this method does not trigger the callback.
   *
   * @param date The Date to load into this JdateField.
   */

  private void setDateOnly(Date date)
  {
    if (date != null)
      {
	if (debug)
	  {
	    System.err.println("setDateOnly() called: " + date);
	  }

	Calendar cal = Calendar.getInstance();
	cal.setTime(curr_date);

	int hour = cal.get(Calendar.HOUR_OF_DAY);
	int minute = cal.get(Calendar.MINUTE);

	cal.setTime(date);
	cal.set(Calendar.HOUR_OF_DAY, hour);
	cal.set(Calendar.MINUTE, minute);

	setDate(cal.getTime());
      }
    else
      {
	setDate(null);
      }
  }

  /**
   * Sets the time of day for this JdateField, keeping the calendar
   * date previously set.
   *
   * Calling this method does not trigger the callback.
   *
   * @param timeStr The string to extract time of day from.
   */

  private void setTimeOnly(String timeStr)
  {
    if (timeStr == null)
      {
	return;
      }

    String[] splt = timeStr.split(":");

    if (splt.length < 2)
      {
	return;
      }

    Calendar cal = Calendar.getInstance();
    cal.setTime(curr_date);
    cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(splt[0]));
    cal.set(Calendar.MINUTE, Integer.parseInt(splt[1]));

    setDate(cal.getTime());
  }

  /**
   * Sets the date and time value of this JdateField.
   *
   * Calling this method does not trigger the callback.
   *
   * @paramd date The Date to extract the calendar date and time of
   * day from.
   */

  public void setDate(Date date)
  {
    if (debug)
      {
	System.err.println("setDate() called: " + date);
      }

    datePicker.setDate(date);

    if (date != null)
      {
	Calendar cal = Calendar.getInstance();
	cal.setTime(date);

	String hour = prefixZero(Integer.toString(cal.get(Calendar.HOUR_OF_DAY)));
	String minute = prefixZero(Integer.toString(cal.get(Calendar.MINUTE)));

	timef.setText(hour + ":" + minute);
      }
    else
      {
	timef.setText("");
      }

    curr_date = date;

    if (original_date == null)
      {
        original_date = date;
      }
  }

  /**
   * Attaches this JdateField component to the callback we use to
   * notify on date/time change input.
   */

  public void setCallback(JsetValueCallback callback)
  {
    this.callback = callback;
  }

  /**
   * Propagate the date value up to the server object.
   */

  public void updateServer()
  {
    if (callback == null)
      {
	return;
      }

    if (curr_date == null || curr_date.compareTo(original_date) == 0)
      {
	return;
      }

    boolean retval = false;

    try
      {
	retval = callback.setValuePerformed(new JSetValueObject(this, curr_date));
      }
    catch (RemoteException ex)
      {
	// throw up an information dialog here
	// "Date Field Error"
	// "There was an error communicating with the server!\n{0}"
	new JErrorDialog(new JFrame(),
			 ts.l("global.error_subj"),
			 ts.l("global.error_text", ex.getMessage()),
			 StandardDialog.ModalityType.DOCUMENT_MODAL);
      }

    // if setValuePerformed() didn't work, revert the date,

    if (!retval)
      {
	setDate(original_date);
	return;
      }

    // Now, the new value has propagated to the server, so reset
    // original date, so that the next time we loose focus from
    // this widget, we won't unnecessarily update the server value
    // if nothing has changed locally.

    original_date = curr_date;
  }

  private String prefixZero(String str)
  {
    if (str.length() < 2)
      {
	str = "0" + str;
      }

    return str;
  }
}

