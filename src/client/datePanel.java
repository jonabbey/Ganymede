/*

   datePanel.java

   The tab that holds date information.
   
   Created: 9 September 1997
   Release: $Name:  $
   Version: $Revision: 1.19 $
   Last Mod Date: $Date: 2002/01/29 05:39:20 $
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
import java.text.*;

import javax.swing.*;
import javax.swing.border.*;

import arlut.csd.ganymede.*;
import arlut.csd.JDataComponent.*;
import arlut.csd.JCalendar.*;

import arlut.csd.JDialog.*;

public class datePanel extends JPanel implements ActionListener, JsetValueCallback, Runnable {

  final static boolean debug = false;

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
   * Label field used only in editable context.
   *
   */

  JLabel    topLabel;

  /**
   *
   * Label field used only in non-editable context.
   *
   */

  JLabel    noneditable_dateLabel;
    
  protected GregorianCalendar 
    my_Calendar;

  protected TimeZone
    _myTimeZone = SimpleTimeZone.getDefault();

  SimpleDateFormat
    dateformat,
    timeformat;

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
    thread.start();
  }

  public void run()
  {
    setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

    top_pane = new JPanel(false);
    top_pane.setLayout(new BorderLayout());
    top_pane.setMaximumSize(top_pane.getPreferredSize());

    dateformat = new SimpleDateFormat("MMM dd, yyyy",Locale.getDefault());
    timeformat = new SimpleDateFormat("MMM dd, yyyy, 'at' HH:mm a", Locale.getDefault());

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
	add("Center", cal);
      }

    add("North", top_pane);

    invalidate();
    fp.validate();
  }

  void create_editable_panel()
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

	    topLabel.setText(label + ": " + dateformat.format(date));
	  }
	else
	  {
	    topLabel.setText(label + " has not been set.");
	  }
	
	cal = new JpanelCalendar(my_Calendar, this, true, true);

	top_pane.add("Center", topLabel);
	clear = new JButton("Clear date");
	clear.setActionCommand("Clear");
	clear.addActionListener(this);

	if (debug)
	  {
	    System.out.println("adding clear button to top_pane");
	  }

	top_pane.add("East", clear);
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not get date: " + rx);
      }
  }

  // Maybe this should have a calendar, inactivated, showing the date of the thing?
  // only if the date is set, of course.

  void create_non_editable_panel()
  {
    try
      {
	if (field != null)
	  {
	    this.name = template.getName();

	    Date d = (Date) field.getValue();

	    if (d != null)
	      {
		noneditable_dateLabel = new JLabel(this.name + " is set to: " + d.toString());
	      }
	    else
	      {
		noneditable_dateLabel = new JLabel("No date is set");
	      }
	  }
	else
	  {
	    noneditable_dateLabel = new JLabel("No date is set");
	  }
	
	top_pane.add("Center", noneditable_dateLabel);
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not check visibility: " + rx);
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
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Could not clear date field: " + rx);
	  }

	if (ok)
	  {
	    cal.clear();
	    fp.wp.getgclient().somethingChanged();
	  }
	else
	  {
	    setStatus("Server says:  Could not clear date field.");
	  }
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
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Could not set Value in removal field: " + rx);
	  }
	
	if (ok)
	  {
	    topLabel.setText(label + ": " + dateformat.format(d));
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
	newLabel = label + ": " + dateformat.format(newDate);
      }
    else
      {
	newLabel = "No date is set";
      }

    if (cal != null)
      {
	cal.setDate(newDate);
	topLabel.setText(newLabel);
      }
    else
      {
	noneditable_dateLabel.setText(newLabel);
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
	int type = template.getType();
	Invid invid = fp.getObjectInvid();

	if (debug)
	  {
	    System.err.println("datePanel: resetting date to " + date);
	  }
	
	this.setDate(date);
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("datePanel.refresh(): error, couldn't refresh date panel.");
      }
  }

  private final void setStatus(String s)
  {
    gc.setStatus(s);
  }

}//datePanel
