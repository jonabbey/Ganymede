
/*
   JdateField.java

   This class defines a date input field object.

   Created: 31 Jul 1996
   Version: $Revision: 1.31 $
   Last Mod Date: $Date: 2001/06/26 06:11:25 $
   Release: $Name:  $

   Module By: Navin Manohar

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999, 2000, 2001
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
                                                                      JdateField

------------------------------------------------------------------------------*/

/**
 *
 * This class defines a date input field object.
 *
 */

public class JdateField extends JPanel implements JsetValueCallback, ActionListener {

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

  private JstringField 
    _date;

  private JButton 
    _calendarButton, 
    _clearButton;

  private JpopUpCalendar 
    pCal = null;

  protected GregorianCalendar 
    _myCalendar;

  protected SimpleTimeZone
    _myTimeZone = (SimpleTimeZone)(SimpleTimeZone.getDefault());

  private SimpleDateFormat
    _dateformat = new SimpleDateFormat("MM/dd/yyyy");

  //  protected SimpleTimeZone _myTimeZone = new SimpleTimeZone(-6*60*60*1000,"CST");

  //////////////////
  // Constructors //
  //////////////////

  /**
   * Minimal Constructor for JdateField.  This will construct a JdateField
   * with no value.
   *  
   */
  
  public JdateField()
  {
    this(null,true,false,null,null);
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
   * @param minDate the oldest possible date that can be entered into this JdateField
   * @param maxDate the newest possible date that can be entered into this JdateField
   */

  public JdateField(Date date,
		    boolean iseditable,
		    boolean islimited,
		    Date minDate,
		    Date maxDate,
		    JsetValueCallback parent)
  {
    this(date,iseditable,islimited,minDate,maxDate);

    setCallback(parent);
    
    _date.setCallback(this);
  }
  
  /**
   * Contructor that creates a JdateField based on the date it is given.  It is also
   * possible to set restrictions on the range of dates for this JdateField when
   * using this constructor
   *
   * @param date the Date object to use
   * @param islimited true if there is to be a restriction on the range of dates
   * @param minDate the oldest possible date that can be entered into this JdateField
   * @param maxDate the newest possible date that can be entered into this JdateField
   */

  public JdateField(Date date,
		    boolean iseditable,
		    boolean islimited,
		    Date minDate,
		    Date maxDate)
  { 
    if (debug)
      {
	System.err.println("JdateField(): date = " + date);
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
    
    // max date size: 04/45/1998

    _date = new JstringField(12, // make it a bit wider than needed
			     10,
			     iseditable,
			     false,
			     "1234567890/",
			     null,
			     this);

    add(_date,"West");

    JPanel buttonPanel = new JPanel();
    buttonPanel.setLayout(new BorderLayout());

    Image img = PackageResources.getImageResource(this, 
						  "calendar.gif", 
						  getClass());
    Image img_dn = PackageResources.getImageResource(this, 
						  "calendar_dn.gif", 
						  getClass());


    _calendarButton = new JButton(new ImageIcon(img));
    _calendarButton.setPressedIcon(new ImageIcon(img_dn));
    _calendarButton.setFocusPainted(false);
    _calendarButton.addActionListener(this);

    buttonPanel.add(_calendarButton,"West");

    // don't need the clear button if it is not editable

    if (iseditable)
      {
	_clearButton = new JButton("Clear");
	_clearButton.addActionListener(this);
	
	buttonPanel.add(_clearButton, "Center");
      }

    add(buttonPanel, "East");

    if (my_date != null)
      {
	_myCalendar.setTime(my_date);
      }

    setEditable(iseditable);

    unset = true;

    setDate(my_date);

    invalidate();
    validate();
  }

  public void actionPerformed(ActionEvent e) 
  {
    boolean retval = false;
    Object c = e.getSource();

    /* -- */

    if (c == _calendarButton)
      {
	if (pCal == null)
	  {
	    pCal = new JpopUpCalendar(_myCalendar, this, iseditable);
	  }

	if (pCal.isVisible())
	  {
	    pCal.setVisible(false);
	  }
	else
	  {
	    if (my_date == null)
	      {
		my_date = new Date();
		_myCalendar.setTime(my_date);
	      }

	    pCal.show();
	  }
      }
    else if (c == _clearButton)
      {
	try 
	  {
	    if (callback != null)
	      {
		retval=callback.setValuePerformed(new JValueObject(this,null));
	      }
	    changed = false;
	  }
	catch (java.rmi.RemoteException re) 
	  {
	    // throw up an information dialog here
	    
	    JErrorDialog _infoD = new JErrorDialog(new JFrame(),
						   "Date Field Error",
						   "There was an error communicating with the server!\n" +
						   re.getMessage());
	  }

	if (retval == true)
	  {
	    setDate(null);
	  }
      }
  }

  /**
   * May this field be edited?
   */

  public void setEditable(boolean editable)
  {
    _date.setEditable(editable);
    this.iseditable = editable;
  }

  /**
   * Passes enabled to all components in the date field.
   */

  public void setEnabled(boolean enabled)
  {
    try
      {
	//	_calendarButton.setEnabled(enabled);
	//_calendarButton.setVisible(enabled);
	_clearButton.setEnabled(enabled);
	_clearButton.setVisible(enabled);
	//	_date.setEnabled(enabled);
      }
    catch (NullPointerException e) {}  // the buttons might still be null
  }

  /**
   * returns the date associated with this JdateField
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
   * sets the date value of this JdateField
   *
   * @param d the date to use
   */

  public void setDate(Date d)
  {
    this.setDate(d, true);
  }

  /**
   * sets the date value of this JdateField
   *
   * @param d the date to use
   */

  public void setDate(Date d, boolean checkLimits)
  {
    if (d == null)
      {
	_date.setText("");
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

    String s = _dateformat.format(d);

    if (debug)
      {
	System.err.println("formatted date = " + s);
      }

    _date.setText(s);

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
    if (pCal != null)
      {
	pCal.setVisible(false);
      }

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

    if (comp == _date) 
      {
	if (!(obj instanceof String))
	  {
	    throw new RuntimeException("Error: Invalid value embedded in JValueObject");
	  }

	// The user has pressed Tab or clicked elsewhere which has caused the
	// _date component to lose focus.  This means that we need to update
	// the date value (using the value in _date) and then propogate that value
	// up to the server object.
	
	Date d = null;

	if (obj != null && !((String) obj).equals(""))
	  {
	    try 
	      {
		d = _dateformat.parse((String)obj);
	      }
	    catch (Exception ex) 
	      {
		// throw up an information dialog here
	    
		JErrorDialog _infoD = new JErrorDialog(new JFrame(),
						       "Date Field Error",
						       "The date you have typed is invalid!\n\n" +
						       "Proper format:  MM/DD/YYYY   10/01/1997");
		
		
		return retval;
	      }

	    if (d != null) 
	      {
		if (limited) 
		  {
		    if (d.after(maxDate) || d.before(minDate))
		      {
			// This means that the date chosen was not
			// within the limits specified by the
			// constructor.  Therefore, we just reset the
			// selected Components of the chooser to what
			// they were before they were changed.
		    
			JErrorDialog _infoD = new JErrorDialog(new JFrame(),
							   "Date Field Error",
							   "The date you have typed is out of range!\n\nValid Range: " + 
							   _dateformat.format(minDate) + 
							   " to " +
							   _dateformat.format(maxDate));
			return retval;
		      }
		  }

		try
		  {
		    setDate(d);
		  }
		catch (IllegalArgumentException ex)
		  {
		    return false; // out of range
		  }

		_myCalendar.setTime(d);
		
		if (pCal != null)
		  {
		    pCal.update();
		  }
	      }
	    else 
	      {
		return retval;
	      }
	  }
	else
	  {
	    d = null;
	  }

	// Now, the date value needs to be propogated up to the server
	
	if (allowCallback) 
	  {
	    try 
	      {
		retval = callback.setValuePerformed(new JValueObject(this,d));
	      }
	    catch (RemoteException e)
	      {
		// throw up an information dialog here
		
		JErrorDialog _infoD = new JErrorDialog(new JFrame(),
						   "Date Field Error",
						   "There was an error communicating with the server!\n" +
						   e.getMessage());
	      }
	  }
      }
    else if (comp == pCal) 
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

  /**
   *
   * Tie into the focus event handling.
   *
   */

  public void processFocusEvent(FocusEvent e)
  {
    super.processFocusEvent(e);

    switch (e.getID())
      {
      case FocusEvent.FOCUS_LOST:
	// When the JdateField looses focus, any changes
	// made to the value in the JdateField must be
	// propogated to the db_field on the server.
	
	// But first, if nothing in the JstringField has changed
	// then there is no reason to do anything.
	
	if (!changed)
	  {
	    break;
	  }
	
	if (!unset && allowCallback)
	  {
	    // Do a callback to talk to the server
	    
	    try 
	      {
		callback.setValuePerformed(new JValueObject(this,my_date));
	      }
	    catch (java.rmi.RemoteException re) 
	      {
		// throw up an information dialog here
		
		JErrorDialog _infoD = new JErrorDialog(new JFrame(),
						       "Date Field Error",
						       "There was an error communicating with the server!\n" + 
						       re.getMessage());
	      }
	  }
	
	// Now, the new value has propogated to the server, so we set
	// changed to false, so that the next time we loose focus from
	// this widget, we won't unnecessarily update the server value
	// if nothing has changed locally.
	
	changed = false;

	break;

      case FocusEvent.FOCUS_GAINED:
	break;
      }
  }
}

