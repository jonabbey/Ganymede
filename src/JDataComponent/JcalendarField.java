
/*
   JcalendarField.java

   This class defines a date input field object.

   Created: 28 June 2002
   Version: $Revision: 1.2 $
   Last Mod Date: $Date: 2002/07/03 02:50:16 $
   Release: $Name:  $

   Module By: Navin Manohar

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999, 2000, 2001, 2002
   The University of Texas at Austin.

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

import java.util.*;
import java.lang.String;
import java.awt.*;
import java.text.*;
import java.net.*;
import java.rmi.*;
import java.awt.event.*;

import javax.swing.*;

import arlut.csd.Util.PackageResources;

import arlut.csd.JCalendar.*;
import arlut.csd.JDataComponent.*;
import arlut.csd.JDialog.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                  JcalendarField

------------------------------------------------------------------------------*/

/**
 *
 * This class defines a date input field object.
 *
 */

public class JcalendarField extends JPanel implements JsetValueCallback {

  static final boolean debug = false;

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

  private SimpleDateFormat
    _dateformat = new SimpleDateFormat("MM/dd/yyyy");

  //  protected SimpleTimeZone _myTimeZone = new SimpleTimeZone(-6*60*60*1000,"CST");

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
	my_date = null; // new Date();
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

		retval=callback.setValuePerformed(new JValueObject(this,my_date));
		changed = false;
	      }
	    catch (java.rmi.RemoteException re) 
	      {
		// throw up an information dialog here
		
		JErrorDialog _infoD = new JErrorDialog(new JFrame(),
						       "Date Field Error",
						       "There was an error communicating with the server!\n"+
						       re.getMessage());
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

