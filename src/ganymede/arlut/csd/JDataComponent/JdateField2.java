
/*
   JdateField2.java

   This class defines a date input field object and calendar pulldown widget.

   Created: 20 Jul 2010

   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

   Module By: James Darren Ratcliff

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
   along with this program; if not, write to the Free Software
   Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
   02111-1307, USA

*/

package arlut.csd.JDataComponent;
  
import java.awt.BorderLayout; 
import java.util.Date; 
  
import javax.swing.UIManager;
  
import org.jdesktop.swingx.JXDatePicker; 
import org.jdesktop.swingx.JXPanel; 
import org.jdesktop.swingx.JXMonthView;
import org.jdesktop.swingx.plaf.basic.SpinningCalendarHeaderHandler;
import org.jdesktop.swingx.plaf.basic.CalendarHeaderHandler;

/** 
 */ 

public class JdateField2 extends JXPanel
{
  private JXDatePicker datePicker;       
      
  public JdateField2()
  {
    this(null, null, null);
  }

  public JdateField2(Date date)
  {
    this(date, null, null);
  }

  public JdateField2(Date date, Date minDate, Date maxDate) { 
    if (date == null) date = new Date();
    initComponents(date, minDate, maxDate); 
  }    

  //------------------- inti ui       
  private void initComponents(Date date, Date minDate, Date maxDate) { 

    // Adds a year spinner to the MonthView object.
    UIManager.put(CalendarHeaderHandler.uiControllerID, 
		  "org.jdesktop.swingx.plaf.basic.SpinningCalendarHeaderHandler");
    // Moves the year spinner after month arrows.
    UIManager.put(SpinningCalendarHeaderHandler.ARROWS_SURROUND_MONTH, Boolean.TRUE);
    
    // Creates a new picker and sets the current date to today 
    datePicker = new JXDatePicker(date); 
    datePicker.setName("datePicker"); 
    
    JXMonthView monthView = datePicker.getMonthView();
    if (minDate != null) monthView.setLowerBound(minDate);
    if (maxDate != null) monthView.setUpperBound(maxDate);
    monthView.setZoomable(true);
    
    add(datePicker, BorderLayout.CENTER);
  } 
  
  public Date getDate() { 
    return datePicker.getDate(); 
  } 

}
