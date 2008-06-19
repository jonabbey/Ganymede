
/*
   JcalendarField.java

   This class defines a date input field object, suitable for use in a
   StringDialog class.  This differs from JdateField in that the
   calendar is displayed in-line, rather than as a popup window, and
   in that there is no separate button to clear the date in the field.

   Created: 28 June 2002

   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

   Module By: Navin Manohar

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996-2008
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

package arlut.csd.JDataComponent;

import java.awt.BorderLayout;
import java.awt.Component;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

import javax.swing.JFrame;
import javax.swing.JPanel;

import arlut.csd.JCalendar.JpanelCalendar;
import arlut.csd.JDialog.JErrorDialog;
import arlut.csd.Util.TranslationService;

/*------------------------------------------------------------------------------
                                                                           class
                                                                  JcalendarField

------------------------------------------------------------------------------*/

/**
 * This class defines a date input field object, suitable for use in a
 * StringDialog class.  This differs from JdateField in that the
 * calendar is displayed in-line, rather than as a popup window, and
 * in that there is no separate button to clear the date in the field.
 */

public class JcalendarField extends JPanel implements JsetValueCallback {

  static final boolean debug = false;

  /**
   * TranslationService object for handling string localization in the
   * Ganymede client.
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.JDataComponent.JcalendarField");

  // ---

  private boolean
    allowCallback = false,
    changed = false, 
    limited,
    unset,
    iseditable;

  private JsetValueCallback callback = null;

  protected Date 
    my_date,
    old_date;

  private Date 
    maxDate,
    minDate;

  private JpanelCalendar
    pCal = null;

  protected GregorianCalendar 
    _myCalendar;

  protected TimeZone
    _myTimeZone = (TimeZone)(SimpleTimeZone.getDefault());

  private SimpleDateFormat _dateformat = new SimpleDateFormat(ts.l("init.date_format")); // "MM/dd/yyyy"

  //////////////////
  // Constructors //
  //////////////////

  /**
   * Minimal Constructor for JcalendarField.  This will construct a JcalendarField
   * with no value.
   *  
   */
  
  public JcalendarField()
  {
    this(null,true,false,null,null);
  }

  /**
   * Contructor that creates a JcalendarField based on the Date object it is given.  This
   * constructor can be used if the JcalendarField will be making callbacks to pass its data
   * to the appropriate container.
   *
   * @param parent the container which implements the callback function for this JcalendarField
   * @param date the Date object to use
   * @param iseditable true if the datefield can be edited by the user
   * @param islimited true if there is to be a restriction on the range of dates
   * @param minDate the oldest possible date that can be entered into this JcalendarField
   * @param maxDate the newest possible date that can be entered into this JcalendarField
   */

  public JcalendarField(Date date,
			boolean iseditable,
			boolean islimited,
			Date minDate,
			Date maxDate,
			JsetValueCallback parent)
  {
    this(date,iseditable,islimited,minDate,maxDate);

    setCallback(parent);
  }
  
  /**
   * Contructor that creates a JcalendarField based on the date it is given.  It is also
   * possible to set restrictions on the range of dates for this JcalendarField when
   * using this constructor
   *
   * @param date the Date object to use
   * @param islimited true if there is to be a restriction on the range of dates
   * @param minDate the oldest possible date that can be entered into this JcalendarField
   * @param maxDate the newest possible date that can be entered into this JcalendarField
   */

  public JcalendarField(Date date,
			boolean iseditable,
			boolean islimited,
			Date minDate,
			Date maxDate)
  { 
    if (debug)
      {
	System.err.println("JcalendarField(): date = " + date);
      }

    this.iseditable = iseditable;

    if (date == null)
      {
	my_date = new Date();
      }
    else
      {
	my_date = new Date(date.getTime());
      }

    _myCalendar = new GregorianCalendar(_myTimeZone,Locale.getDefault());
    _myCalendar.setTime(my_date);
    
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

	limited = true;

	this.minDate = minDate;
	this.maxDate = maxDate;
      }       
   
    setLayout(new BorderLayout());

    pCal = new JpanelCalendar(_myCalendar, this, iseditable);

    // max date size: 04/45/1998

    add(pCal,"Center");

    unset = true;

    setDate(my_date);

    invalidate();
    validate();
  }

  /**
   * returns the date associated with this JcalendarField
   */

  public Date getDate()
  {
    if (unset)
      {
	return null;
      }

    return my_date;
  }

  /**
   * sets the date value of this JcalendarField
   *
   * @param d the date to use
   */

  public void setDate(Date d)
  {
    this.setDate(d, true);
  }

  /**
   * sets the date value of this JcalendarField
   *
   * @param d the date to use
   */

  public void setDate(Date d, boolean checkLimits)
  {
    if (d == null)
      {
	unset = true;
	my_date = null;
	changed = true;
	return;
      }

    if (debug)
      {
	System.err.println("setDate() called: " + d);
      }
        
    if (checkLimits && limited)
      {
	if (d.after(maxDate) || d.before(minDate))
	  {
	    throw new IllegalArgumentException("Invalid Parameter: date out of range");
	  }
      }

    my_date = d;

    if (my_date != null)
      {
	_myCalendar.setTime(my_date);
      }

    unset = false;
    changed = true;
  }

  /**
   *
   * This method is to be called when the containerPanel holding this
   * date field is being closed down.  This method is responsible for
   * popping down any connected calendar panel.
   * 
   */

  public void unregister()
  {
    callback = null;
  }

  /**
   *  sets the parent of this component for callback purposes
   *
   */

  public void setCallback(JsetValueCallback callback)
  {
    if (callback == null)
      {
	throw new IllegalArgumentException("Invalid Parameter: callback is null");
      }
    
    this.callback = callback;
    
    allowCallback = true;
  }

  /**
   *
   * This is the callback that the JentryField uses to notify us if the
   * user entered something in the text field.
   *
   */

  public boolean setValuePerformed(JValueObject valueObj)
  {
    boolean retval = false;
    Component comp = valueObj.getSource();
    Object obj = valueObj.getValue();

    /* -- */

    if (comp == pCal) 
      {
	if (debug)
	  {
	    System.out.println("setValuePerformed called by Calendar");
	  }

	if (!(obj instanceof Date))
	  {
	    throw new RuntimeException("Error: Invalid value embedded in JValueObject");
	  }

	old_date = getDate();
	
	try
	  {
	    setDate((Date) obj);
	  }
	catch (IllegalArgumentException ex)
	  {
	    return false;	// out of range
	  }

	// The user has triggered an update of the date value
	// in the _date field by choosing a date from the 
	// JpopUpCalendar

	if (allowCallback)
	  {
	    // Do a callback to talk to the server

	    try 
	      {
		if (debug)
		  {
		    System.out.println("setValuePerformed called by Calendar --- passing up to container");
		  }

		retval=callback.setValuePerformed(new JSetValueObject(this,my_date));
		changed = false;
	      }
	    catch (java.rmi.RemoteException re) 
	      {
		// throw up an information dialog here

		// "Calendar Field Error"
		// "There was an error communicating with the server!\n{0}"
		new JErrorDialog(new JFrame(),
				 ts.l("global.error_subj"),
				 ts.l("global.error_text", re.getMessage()));
	      }

	    if (!retval)
	      {
		if (debug)
		  {
		    System.err.println("Resetting date to " + old_date);
		  }
		
		setDate(old_date, false);

		return false;
	      }
	  }
	else
	  {
	    setDate((Date) obj);
	    _myCalendar.setTime((Date) obj);

	    // no callback, so we ok the date

	    return true;
	  }
      }
    
    return retval;
  }
}

