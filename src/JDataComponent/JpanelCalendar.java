/*
   JpanelCalendar.java

   A GUI Calendar for use with the arlut.csd.JDataComponent JdateField class.

   Created: 17 March 1997
   Version: $Revision: 1.13 $
   Last Mod Date: $Date: 2002/02/02 22:01:19 $
   Release: $Name:  $

   Module By: Navin Manohar, Michael Mulvaney, and Jonathan Abbey

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

package arlut.csd.JCalendar;

import arlut.csd.JDialog.*;
import arlut.csd.JDataComponent.*;
import arlut.csd.Util.PackageResources;

import java.awt.*;
import java.util.*;
import java.text.*;

import java.awt.event.*;
import java.rmi.*;

import javax.swing.*;
import javax.swing.border.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                  JpanelCalendar
	
------------------------------------------------------------------------------*/

/**
 * <p>A GUI Calendar for use with the {@link
 * arlut.csd.JDataComponent.JdateField JdateField} class.</p>
 */

public class JpanelCalendar extends JPanel implements ActionListener {

  static final boolean debug = false;

  static final int leapDays[] = {31,29,31,30,31,30,31,31,30,31,30,31};
  static final int monthDays[] = {31,28,31,30,31,30,31,31,30,31,30,31};
  static final String[] month_names = {"January", "February", "March", "April", 
				       "May", "June", "July", "August",
				       "September", "October", "November", "December"};
  // ---

  /**
   * What time do we have set?
   */

  protected GregorianCalendar selectedDate_calendar;

  /**
   * What time do we have visible?  Month, year, etc.
   */

  protected GregorianCalendar visibleDate_calendar;

  /**
   * <p>Who do we notify when the user changes the date through
   * direct manipulation of the calendar?</p>
   */

  protected JsetValueCallback callback;

  /**
   * <p>If we are contained in a pop-up, this will refer to
   * the dialog frame, so that the close button can close
   * it.</p>
   */

  protected JpopUpCalendar pCal = null;
  protected JButton closeButton;

  /**
   * <p>The meat of the calendar.  This array of JdateButton's
   * are both the display and the main user interface element for the
   * JpanelCalendar.</p>
   */

  protected JdateButton _datebuttonArray[] = new JdateButton[37];

  private JMonthYearPanel monthYearPanel = null;
  private JPanel buttonPanel = null;
  private JTimePanel timePanel;
  
  protected Font todayFont = new Font("sansserif", Font.BOLD, 12);
  protected Font notTodayFont = new Font("serif", Font.PLAIN, 12);

  /**
   * <p>If true, we will allow the calendar to be used to change the
   * date/time selected.  If false, we will be display only.</p>
   */

  private boolean editable;

  /**
   * <p>Used to control whether we show a selected day in the
   * calendar.  If false, no date has been set, and we'll
   * show all calendar pages with no dates highlighed.</p>
   */

  private boolean dateIsSet;

  /**
   * <p>If true, we'll show the time of day in the calendar, and allow
   * the user to edit the time of day if we are editable.  If false,
   * we'll show the the date only.</p>
   */

  private boolean showTime;

  /**
   * <p>If true, we'll allow the use to change the month and year
   * displayed in the calendar.</p>
   */

  private boolean allowMonthChange;

  /**
   * <p>If true, we'll try to render the calendar in a compressed
   * form, with less space for the calendar buttons.</p>
   */

  private boolean compact;

  /**
   * <p>The last known good date.  If we attempt to pass a date change
   * back to our client and the attempt fails, this is the date we'll
   * revert to afterwards.</p>
   */

  private Date previousDate;

  /* -- */

  /**
   * Lots of constructors.  
   *
   */

  public JpanelCalendar(GregorianCalendar parentCalendar, JsetValueCallback callback, boolean editable)
  {
    this(parentCalendar, callback, true, false, editable);
  }

  /**
   * Constructors.
   *
   * @param showTime If true, then the "Choose a time" part will be there.  Also, if true, time will appear in date at top.
   *
   */
  public JpanelCalendar(GregorianCalendar parentCalendar, JsetValueCallback callback, 
			boolean showTime, boolean editable)
  {
    this(parentCalendar, callback, showTime, false, editable);
  }

  public JpanelCalendar(JpopUpCalendar pC,GregorianCalendar parentCalendar, 
			JsetValueCallback callback, boolean editable) 
  {
    this(pC, parentCalendar, callback, true, false, editable);
  }

  public JpanelCalendar(JpopUpCalendar pC,GregorianCalendar parentCalendar, 
			JsetValueCallback callback, boolean showTime, 
			boolean editable) 
  {
    this(pC, parentCalendar, callback, showTime, false, editable);
  }

  /**
   *
   *
   * @param compact If true, calendar will be drawn smaller.
   */

  public JpanelCalendar(JpopUpCalendar pC,
			GregorianCalendar parentCalendar, 
			JsetValueCallback callback, 
			boolean showTime, boolean compact,
			boolean editable) 
  {
    this(parentCalendar,callback, showTime, compact, editable);

    if (pC == null)
      {
	throw new IllegalArgumentException("popUpCalendar parameter is null");
      }

    pCal = pC;
    
    closeButton = new JButton("Close");
    buttonPanel.add(closeButton,"East");

    closeButton.addActionListener(this);
  }

  /**
   * <p>The main constructor.  Here's where all the magic happens.</p>
   */

  public JpanelCalendar(GregorianCalendar parentCalendar, 
			JsetValueCallback callback,  
			boolean showTime, boolean compact,
			boolean editable)
  {
    if (parentCalendar == null)
      {
	// if we weren't given a calendar object, create a default
	// calendar, which will be initialized to current date/time.

	parentCalendar = new GregorianCalendar(); 
	dateIsSet = false;
      }
    else
      {
	dateIsSet = true;
      }

    this.showTime = showTime;
    this.compact = compact;
    this.editable = editable;

    if (debug)
      {
	if (editable)
	  {
	    System.err.println("JpanelCalendar.editable == true!");
	  }
	else
	  {
	    System.err.println("JpanelCalendar.editable == false!");
	  }
      }

    this.callback = callback;
    
    selectedDate_calendar = parentCalendar;

    previousDate = selectedDate_calendar.getTime();
    dateIsSet = true;

    // we start off with our visible calendar page the same as our
    // selected date calendar

    visibleDate_calendar = (GregorianCalendar) selectedDate_calendar.clone();

    setLayout(new BorderLayout());

    // The centerPanel will actually contain everything

    JPanel centerPanel = new JPanel(false);

    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();
    centerPanel.setLayout(gbl);
    
    monthYearPanel = new JMonthYearPanel(this);

    gbc.gridy = 0;
    gbc.gridx = 0;
    gbc.anchor = GridBagConstraints.CENTER;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbl.setConstraints(monthYearPanel, gbc);
    centerPanel.add(monthYearPanel);

    gbc.ipadx = 5;
    gbc.insets = new Insets(0,2,0,2);

    gbc.gridy = 1;
    JLabel sun = new JLabel("SUN");
    sun.setFont(todayFont);
    gbc.gridx = 0;
    gbc.gridwidth = 1;
    gbl.setConstraints(sun, gbc);
    centerPanel.add(sun);

    JLabel mon = new JLabel("MON");
    mon.setFont(todayFont);
    gbc.gridx = 1;
    gbl.setConstraints(mon, gbc);
    centerPanel.add(mon);

    JLabel tue = new JLabel("TUE");
    tue.setFont(todayFont);
    gbc.gridx = 2;
    gbl.setConstraints(tue, gbc);
    centerPanel.add(tue);

    JLabel wed = new JLabel("WED");
    wed.setFont(todayFont);
    gbc.gridx = 3;
    gbl.setConstraints(wed, gbc);
    centerPanel.add(wed);

    JLabel thu = new JLabel("THU");
    thu.setFont(todayFont);
    gbc.gridx = 4;
    gbl.setConstraints(thu, gbc);
    centerPanel.add(thu);

    JLabel fri = new JLabel("FRI");
    fri.setFont(todayFont);
    gbc.gridx = 5;
    gbl.setConstraints(fri, gbc);
    centerPanel.add(fri);

    JLabel sat = new JLabel("SAT");
    sat.setFont(todayFont);
    gbc.gridx = 6;
    gbl.setConstraints(sat, gbc);
    centerPanel.add(sat);

    gbc.gridy = 2;
    gbc.gridx = 0;
    gbc.ipadx = 0;

    for (int i=0;i<37;i++) 
      {
	gbc.gridx = i % 7;
	gbc.gridy = (i / 7) + 2;

	_datebuttonArray[i] = new JdateButton(this, compact);
	gbl.setConstraints(_datebuttonArray[i], gbc);
	centerPanel.add(_datebuttonArray[i]);
      }

    JPanel testPanel = new JPanel();
    testPanel.add(centerPanel);
    add(testPanel,"Center");

    // Next, the south ( this part will contain only a label for now)

    //    add(new Label("Please Choose a Date",SwingConstants.CENTER),"South");

    JPanel southPanel = new JPanel();
    southPanel.setLayout(new BorderLayout());

    buttonPanel = new JPanel();
    buttonPanel.setLayout(new BorderLayout());

    southPanel.add(buttonPanel,"North");

    if (showTime)
      {
	timePanel = new JTimePanel(this);

	if (editable)
	  {
	    timePanel.setBorder(new TitledBorder("Please choose a time of day:"));
	  }
	else
	  {
	    timePanel.setBorder(new TitledBorder("Time of day:"));
	  }

	southPanel.add(timePanel,"Center");
      }

    gbc.gridy++;
    gbc.gridx = 0;
    gbc.ipadx = 0;
    gbc.anchor = GridBagConstraints.CENTER;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbl.setConstraints(southPanel, gbc);

    centerPanel.add(southPanel);

    writeDates();

    setAllowMonthChange(editable);
  }

  /**
   * <p>This is the main programmatic entry point for setting the date selected
   * in this calendar widget.  Calling this method will update the selected
   * time/date to that passed, and will redraw the calendar with the selected
   * time/date shown.</p>
   *
   * <p>Calling this method will not trigger a callback to report the date
   * change to our client.</p>
   */
 
  public synchronized void setDate(Date date)
  {
    if (date == null)
      {
	if (debug)
	  {
	    System.err.println("JpanelCalendar.setDate(): null value");
	  }

	dateIsSet = false;

	// and re-draw

	if (debug)
	  {
	    System.err.println("JpanelCalendar.setDate(): writeDates after cleared");
	  }

	writeDates();
      }
    else
      {
	dateIsSet = true;

	if (debug)
	  {
	    System.err.println("JpanelCalendar.setDate(): setting to " + date);
	  }

	// refresh the visible calendar page (month/year) to the newly set date

	visibleDate_calendar.setTime(date);

	// refresh the recorded time

	selectedDate_calendar.setTime(date);

	if (debug)
	  {
	    System.err.println("JpanelCalendar.setDate(): calling writeDates()");
	  }

	// do the calendar calculations to update the display

	Calendar c = Calendar.getInstance();
	c.setTime(date);

	// refresh the month visible in the month combo box

	monthYearPanel.setMonth(c.get(Calendar.MONTH));

	// set the year and re-draw.. note that setYear calls writeDates()
	// for us so we don't have to do that here.

	setYear(c.get(Calendar.YEAR));

	if (debug)
	  {
	    System.err.println("JpanelCalendar.setDate(): returned from writeDates()");
	  }
      }

    this.previousDate = date;
  }

  /**
   * <p>This method returns the selected date held in this calendar.</p>
   */

  public Date getDate()
  {
    return selectedDate_calendar.getTime();
  }

  /**
   * <p>Returns true if this calendar is configured to allow editing
   * of the selected date.</p>
   */

  public boolean isEditable()
  {
    return editable;
  }

  /**
   * <p>This method returns the month of the year
   * currently being displayed in the calendar gui,
   * in the range 0-11.</p>
   */

  public int getVisibleMonth()
  {
    return visibleDate_calendar.get(Calendar.MONTH);
  }

  /**
   * <p>This method returns the month of the year for the selected
   * date, in the range 0-11.</p>
   */

  public int getSelectedMonth()
  {
    return visibleDate_calendar.get(Calendar.MONTH);
  }

  /**
   * <p>This method returns the year being displayed
   * in the calendar gui.</p>
   */

  public int getVisibleYear()
  {
    return visibleDate_calendar.get(Calendar.YEAR);
  }

  /**
   * <p>This method returns the year being displayed
   * in the calendar gui.</p>
   */

  public int getSelectedYear()
  {
    return selectedDate_calendar.get(Calendar.YEAR);
  }

  private void setYear(int year)
  {
    monthYearPanel.setYear(year);

    writeDates();
  }

  public void clear()
  {
    setDate(null);
  }

  /**
   * <p>This method may be used to enable or disable the month and year changing
   * buttons.  If this method is called with a false parameter, the calendar
   * will not allow changing of the month/year.</p>
   *
   * <p>By default, the JpanelCalendar allows month flipping in editable calendars
   * and does not allow it in non-editable calendars.</p>
   */

  public void setAllowMonthChange(boolean okay)
  {
    allowMonthChange = okay;
    monthYearPanel.setAllowMonthChange(allowMonthChange);
  }

  /**
   * <p>This method may be used to query the calendar to see if the
   * month and year changing buttons are enabled.  If this method
   * returns false, the calendar does not allow changing of the
   * month/year.</p>
   */

  public boolean getAllowMonthChange()
  {
    return allowMonthChange;
  }

  /** 
   * This will update the visibleDate_calendar according to the current
   * year and month visible in the GUI controls.
   *
   * Call this from writeDates().
   */

  void updateDate()
  {
    // we actually don't care about the date, since visibleDate_calendar only
    // tracks the month and year shown in the calendar

    visibleDate_calendar.set(monthYearPanel.getYear(), monthYearPanel.getMonth(), 1);
  }

  /**
   * <p>This method forces the calendar to jump to the page containing the
   * selected date.</p>
   */

  public void displaySelectedPage()
  {
    visibleDate_calendar.setTime(selectedDate_calendar.getTime());

    monthYearPanel.setMonth(visibleDate_calendar.get(Calendar.MONTH));
    monthYearPanel.setYear(visibleDate_calendar.get(Calendar.YEAR));

    writeDates();
  }

  /**
   * <p>This method returns a string describing the month and year
   * of the currently selected date</p>
   */

  public String getSelectedMonthString()
  {
    return month_names[selectedDate_calendar.get(Calendar.MONTH)] + " " + 
      selectedDate_calendar.get(Calendar.YEAR);
  }

  /**
   * Our buttons call us back here.
   */

  public void actionPerformed(ActionEvent e) 
  {
    if (e.getSource() == closeButton)
      {
	if (debug)
	  {
	    System.err.println("Closing pCal");
	  }
	
	pCal.setVisible(false);
      }
  }

  /**
   * This method takes the current time held in selectedDate_calendar and refreshes
   * the calendar with it.
   */

  public void update() 
  {
    displaySelectedPage();
    timePanel.update();
  }

  /**
   * <p>This method updates the calendar buttons from the time information held
   * in visibleDate_calendar.</p>
   */

  protected synchronized void writeDates() 
  {
    updateDate();

    // get a local copy of the calendar object indicating the month and year
    // shown

    GregorianCalendar temp  = (GregorianCalendar) visibleDate_calendar.clone();

    // find the first day of the month

    temp.add(Calendar.DATE,-(temp.get(Calendar.DATE)-1));

    // this is presumably here for a bug workaround
	
    temp.setTime(temp.getTime());
   
    int startDay = temp.get(Calendar.DAY_OF_WEEK);

    for (int i = 0;i<startDay-1;i++)
      {
	_datebuttonArray[i].hideYourself();
      }

    int numDays;
    
    if (temp.isLeapYear(temp.get(Calendar.YEAR)))
      {
	numDays = leapDays[temp.get(Calendar.MONTH)];
      }
    else
      {
	numDays = monthDays[temp.get(Calendar.MONTH)];
      }

    int day = 1;

    for (int i =startDay-1;i<(startDay+numDays-1);i++,day++)
      {
	_datebuttonArray[i].showYourself();

	if (dateIsSet &&
	    visibleDate_calendar.get(Calendar.YEAR) == selectedDate_calendar.get(Calendar.YEAR) &&
	    visibleDate_calendar.get(Calendar.MONTH) == selectedDate_calendar.get(Calendar.MONTH) &&
	    selectedDate_calendar.get(Calendar.DATE) == day)
	  {
	    _datebuttonArray[i].setForeground(Color.red);
	    _datebuttonArray[i].setFont(todayFont);
	  }
	else 
	  {
	    _datebuttonArray[i].setForeground(Color.black);
	    _datebuttonArray[i].setFont(notTodayFont);
	  }

	_datebuttonArray[i].setText(Integer.toString(day,10));
      }

    for (int i=startDay+numDays-1;i<37;i++)
      {
	_datebuttonArray[i].hideYourself();
      }

    if (showTime)
      {
	timePanel.update();
      }

    validate();
  }

  /**
   *
   * This method handles the final processing of any calendar button
   * pushed.
   *
   */

  public void buttonPressed(JdateButton _bttn) 
  {
    if (debug)
      {
	System.err.println("buttonPressed");
      }

    if (!editable)
      {
	return;
      }

    if (_bttn == null) 
      {
	throw new IllegalArgumentException("The dateButton parameter is null");
      }

    if (_bttn.getText().equals(""))
      {
	return;			// no-op
      }
    
    int date = Integer.parseInt(_bttn.getText(),10);

    if (debug)
      {
	System.err.println("setting date to day " + date);
      }

    visibleDate_calendar.set(Calendar.DATE,date);
    
    visibleDate_calendar.setTime(visibleDate_calendar.getTime()); // this sets all the fields properly
    
    selectedDate_calendar.setTime(visibleDate_calendar.getTime());

    if (debug)
      {
	System.err.println("selectedDate_calendar = " + selectedDate_calendar.getTime());
      }

    // clear the button that was pressed previously
    
    writeDates();

    try 
      {
	if (callback != null)
	  {
	    // we're going to count on our parent doing an error dialog display if
	    // needed.

	    if (!callback.setValuePerformed(new JValueObject(this, selectedDate_calendar.getTime())))
	      {
		if (debug)
		  {
		    System.err.println("JpanelCalendar.buttonPressed(): oops, unacceptable date.. reverting to " + 
				       previousDate);
		  }

		setDate(previousDate);

		if (debug)
		  {
		    System.err.println("JpanelCalendar.buttonPressed(): reverted date.");
		  }
	      }
	  }
      }
    catch (RemoteException re) 
      {
      }
  }

  /**
   *
   * This method is used to update the calendar's notion of time from
   * information passed in from the time panel.
   *
   */

  public void timeChanged(String _field,int _value)
  {
    if (!editable)
      {
	return;
      }

    if (_field == null)
      {
	throw new IllegalArgumentException("_field is null");
      }

    if (_field.equals("hour") ) 
      {
	visibleDate_calendar.set(Calendar.HOUR_OF_DAY,_value);
	selectedDate_calendar.set(Calendar.HOUR_OF_DAY,_value);
      }
    else if (_field.equals("min")) 
      {
	visibleDate_calendar.set(Calendar.MINUTE,_value);
	selectedDate_calendar.set(Calendar.MINUTE,_value);
      }
    else if (_field.equals("sec")) 
      {
	visibleDate_calendar.set(Calendar.SECOND,_value);
	selectedDate_calendar.set(Calendar.SECOND,_value);
      }
    else
      {
	return;
      }

    try 
      {
	if (!callback.setValuePerformed(new JValueObject(this, selectedDate_calendar.getTime())))
	  {
	    // constructing a JErrorDialog causes it to be shown.
	    
	    new JErrorDialog(new JFrame(),
			     "Date Out Of Range",
			     "The date you have chosen is out of the acceptable range.");
	    
	    setDate(previousDate);
	  }
      }
    catch (RemoteException re) 
      {
      }
  }

  /**
   *
   * Command line test rig
   *
   */

  public static void main(String[] argv)
  {
    JFrame frame = new JFrame();

    frame.getContentPane().add(new JpanelCalendar(new GregorianCalendar(), null, true, true));

    frame.setSize(300,200);
    frame.pack();
    frame.show();
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                                     JdateButton

------------------------------------------------------------------------------*/

/**
 * <p>This class represents a single button in the composite {@link
 * arlut.csd.JCalendar.JpanelCalendar JpanelCalendar} widget.  Each
 * day of the month will be represented on-screen by one of these
 * buttons.  If the calendar is non-editable, the buttens will be
 * non-responsive when pressed, but will still be used to form the
 * calendar's display.</p>
 */

class JdateButton extends JButton implements ActionListener, MouseListener {

  JpanelCalendar my_parent = null;

  Color
    normalFG = Color.black,
    highlightFG = Color.darkGray,
    bg;
  
  /* -- */

  public JdateButton(JpanelCalendar parent)
  {
    this(parent, false);
  }

  public JdateButton(JpanelCalendar parent, boolean compact)
  {
    if (parent == null)
      {
	throw new IllegalArgumentException("The parameter parent is null");
      }

    if (compact)
      {
	setMargin(new Insets(1,1,1,1));
	setBorderPainted(false);
      }
    else
      {
	setMargin(new Insets(3,5,3,5));
      }

    my_parent = parent;

    addActionListener(this);
    addMouseListener(this);

    bg = getBackground();
  }

  public void showYourself()
  {
    setBorderPainted(true);
    super.setForeground(normalFG);
  }

  public void hideYourself()
  {
    setText("");
    setBorderPainted(false);
    super.setForeground(getBackground());
  }

  public void setForeground(Color fg)
  {
    normalFG = fg;
    super.setForeground(fg);
  }

  public void actionPerformed(ActionEvent e)
  {
    if (my_parent == null)
      {
	throw new NullPointerException("dateButton: null parent");
      }

    my_parent.buttonPressed(this);
  }

  public void mouseClicked(MouseEvent e) {}
  public void mouseEntered(MouseEvent e)
  {
    //super.setForeground(highlightFG);
    if (isBorderPainted())
      {
	super.setBackground(Color.lightGray.brighter());
      }
  }

  public void mouseExited(MouseEvent e)
  {
    //super.setForeground(normalFG);
    if (isBorderPainted())
      {
	super.setBackground(bg);
      }
  }

  public void mousePressed(MouseEvent e) {}
  public void mouseReleased(MouseEvent e) {}

}

/*------------------------------------------------------------------------------
                                                                           class
                                                                      JTimePanel
	
------------------------------------------------------------------------------*/

/**
 * <p>This class displays the time at the bottom of the composite {@link
 * arlut.csd.JCalendar.JpanelCalendar JpanelCalendar} widget.  If the
 * calendar widget is editable, this panel will allow the user to set
 * the time corresponding with the date shown in the calendar widget.</p>
 *
 * <p>The numeric fields contained in this panel will transmit the
 * change in time back to the JpanelCalendar's client whenever the focus
 * exits one of the numeric fields.</p>
 */

class JTimePanel extends JPanel implements JsetValueCallback {

  JpanelCalendar container;

  JnumberField _hour = null;
  JnumberField _min = null;
  JnumberField _sec = null;

  /* -- */

  public JTimePanel(JpanelCalendar parent)
  {
    if (parent == null)
      {
	throw new IllegalArgumentException("The parameter parent is null");
      }
    
    container = parent;

    _hour = new JnumberField(3,true,true,0,23,this);
    _min = new JnumberField(3,true,true,0,59,this);
    _sec = new JnumberField(3,true,true,0,59,this);

    if (!parent.isEditable())
      {
	_hour.setEditable(false);
	_min.setEditable(false);
	_sec.setEditable(false);
      }

    GridLayout g = new GridLayout(1,1);
    g.setHgap(0);
    g.setVgap(0);

    setLayout(g);

    JPanel p = new JPanel();

    FlowLayout fL = new FlowLayout();
    fL.setHgap(0);
    fL.setVgap(0);

    p.setLayout(fL);

    JLabel l1 = new JLabel(":",SwingConstants.CENTER);
    JLabel l2 = new JLabel(":",SwingConstants.CENTER);

    l1.setFont(new Font("Helvetica",Font.BOLD,14));
    l2.setFont(new Font("Helvetica",Font.BOLD,14));

    p.add(_hour);
    p.add(l1);
    p.add(_min);
    p.add(l2);
    p.add(_sec);
    
    JPanel panel = new JPanel(new BorderLayout());
    panel.add("West", new JLabel("Time:"));
    panel.add("Center", p);
    
    add(panel);

    update();
  }

  /**
   * <p>This method updates the numeric fields from
   * our JpanelCalendar's current selected time.</p>
   */

  public void update() 
  {
    GregorianCalendar cal = new GregorianCalendar(); 
    cal.setTime(container.getDate());

    _hour.setValue(cal.get(Calendar.HOUR_OF_DAY));
    _min.setValue(cal.get(Calendar.MINUTE));
    _sec.setValue(cal.get(Calendar.SECOND));
  }

  /**
   * <p>Process callbacks from the numeric fields.</p>
   */

  public boolean setValuePerformed(JValueObject valueObj)
  {
    Component comp = valueObj.getSource();

    Object obj = valueObj.getValue();

    if (comp == null)
      {
	throw new RuntimeException("comp is null");
      }

    if (obj == null)
      {
	throw new RuntimeException("obj is null");
      }

    if (comp != _hour && comp != _min && comp != _sec)
      {
	throw new RuntimeException("processEvent called from invalid component");
      }

    int val = ((Integer)obj).intValue();

    if (comp == _hour)
      {
	container.timeChanged("hour",val);
      }
    else if (comp == _min)
      {
	container.timeChanged("min",val);
      }
    else 
      {
	container.timeChanged("sec",val);
      }

    return true;
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                                    JYearChooser
	
------------------------------------------------------------------------------*/

/**
 * <p>This class provides a 'spinner' widget for displaying and allowing the
 * editing of the year in the composite {@link
 * arlut.csd.JCalendar.JpanelCalendar JpanelCalendar} widget.</p>
 *
 * <p>JYearChooser is incorporated into JpanelCalendar by way of
 * the {@link arlut.csd.JCalendar.JMonthYearPanel JMonthYearPanel} widget.</p>
 */

class JYearChooser extends JPanel implements ActionListener {

  static final boolean debug = false;

  // ---

  JnumberField
    year_field;

  JButton
    up,
    down;

  JMonthYearPanel
    callback;

  /* -- */

  public JYearChooser(int year, JMonthYearPanel parent)
  {
    this.callback = parent;

    year_field = new JnumberField(4);
    year_field.setValue(year);
    year_field.setEditable(true);
    
    Image upImage = PackageResources.getImageResource(this, "up.gif", getClass());
    Image downImage = PackageResources.getImageResource(this, "down.gif", getClass());
      
    Insets my_insets = new Insets(0,0,0,0);
    up = new JButton(new ImageIcon(upImage));
    up.setMargin(my_insets);
    up.addActionListener(this);
    down = new JButton(new ImageIcon(downImage));
    down.setMargin(my_insets);
    down.addActionListener(this);
    
    JPanel buttonP = new JPanel(new GridLayout(2,1));
    buttonP.add(up);
    buttonP.add(down);
    
    setLayout(new BorderLayout());
    add("Center", year_field);
    add("East", buttonP);
  }

  public void actionPerformed(ActionEvent e)
  {
    if (e.getSource() == up)
      {
	int year = year_field.getValue().intValue();
	year_field.setValue(year+1);
	callback.updateYear(year+1);
      }
    else if (e.getSource() == down)
      {
	int year = year_field.getValue().intValue();
	year_field.setValue(year-1);
	callback.updateYear(year-1);
      }
  }

  public Integer getYear()
  {
    return year_field.getValue();
  }

  public void setYear(int year)
  {
    if (debug)
      {
	System.out.println("Setting to year: " + year);
      }

    year_field.setValue(year);
  }

  public void setAllowMonthChange(boolean doit)
  {
    up.setEnabled(doit);
    down.setEnabled(doit);
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                                 JMonthYearPanel
	
------------------------------------------------------------------------------*/

/**
 * <p>This class displays the year/month gui controls at the top of
 * the composite {@link arlut.csd.JCalendar.JpanelCalendar
 * JpanelCalendar} widget.  If the calendar widget is editable, this
 * panel will allow the user to change the month and year year
 * corresponding with the date shown in the calendar widget.</p>
 */

class JMonthYearPanel extends JPanel implements ActionListener, ItemListener {

  static final boolean debug = false;

  // ---

  private boolean editable;
  private JButton _prevdate;
  private JButton _nextdate;
  private JYearChooser year;
  private JComboBox month;
  private JpanelCalendar container;
  private int currentMonth;
  private int currentYear;
  private JLabel mYLabel;

  private Font titleFont = new Font("serif", Font.BOLD, 14);

  /* -- */

  public JMonthYearPanel(JpanelCalendar parent)
  {
    this.container = parent;

    currentMonth = container.getVisibleMonth();
    currentYear = container.getVisibleYear();

    editable = container.getAllowMonthChange();

    if (editable)
      {
	initializeEditable();
      }
    else
      {
	initializeNonEditable();
      }
  }

  /**
   * <p>This method sets the month in the JMonthYearPanel.  The
   * appropriate range is from 0 to 11.</p>
   *
   * <p>Calling this method will update the display, but it will
   * not trigger a callback to the JpanelCalendar.</p>
   */

  public void setMonth(int index)
  {
    if (index < 0 || index > 11)
      {
	throw new IllegalArgumentException("month out of range: " + index);
      }

    currentMonth = index;

    if (editable)
      {
	month.setSelectedIndex(currentMonth);
      }
    else
      {
	mYLabel.setText(JpanelCalendar.month_names[currentMonth] + " " + currentYear);
      }
  }

  /**
   * <p>Returns the month currently selected in the JMonthYearPanel.
   * This is not necessarily the same as the month of the currently
   * selected day in the calendar.</p>
   */

  public int getMonth()
  {
    return currentMonth;
  }

  /**
   * <p>This method returns a human readable string containing
   * the month shown in the JMonthYearPanel.</p>
   */

  public String getMonthString()
  {
    return JpanelCalendar.month_names[currentMonth];
  }

  /**
   * <p>This method sets the year in the JMonthYearPanel.</p>
   *
   * <p>Calling this method will update the display, but it will
   * not trigger a callback to the JpanelCalendar.</p>
   */

  public void setYear(int index)
  {
    currentYear = index;

    if (editable)
      {
	year.setYear(currentYear);
      }
    else
      {
	mYLabel.setText(JpanelCalendar.month_names[currentMonth] + " " + currentYear);
      }
  }

  /**
   * <p>This method passes changes from the JYearChooser up to
   * the parent calendar widget.</p>
   */

  public void updateYear(int index)
  {
    currentYear = index;
    performCallback();
  }

  /**
   * <p>Returns the year currently selected in the JMonthYearPanel.
   * This is not necessarily the same as the year of the currently
   * selected day in the calendar.</p>
   */

  public int getYear()
  {
    return currentYear;
  }

  /**
   * <p>This method initializes or re-initializes this panel for editing.</p>
   */

  public void initializeEditable()
  {
    this.removeAll();

    setLayout(new BorderLayout());

    _prevdate = new JButton("<<");
    _prevdate.setToolTipText("Previous Month");
    _nextdate = new JButton(">>");
    _nextdate.setToolTipText("Next Month");

    _prevdate.addActionListener(this);
    _nextdate.addActionListener(this);

    month = new JComboBox();
    month.setKeySelectionManager(new TimedKeySelectionManager());

    for (int i = 0; i < JpanelCalendar.month_names.length; i++)
      {
	month.addItem(JpanelCalendar.month_names[i]);
      }

    month.setSelectedIndex(currentMonth);
    month.addItemListener(this);

    year = new JYearChooser(currentYear, this);

    JPanel middlePanel = new JPanel(new BorderLayout());
    middlePanel.add("Center", month);
    middlePanel.add("East", year);

    add(_prevdate, "West");
    add(_nextdate, "East");
    add(middlePanel, "Center");

    validate();
  }

  /**
   * <p>This method initializes or re-initializes this panel for
   * display.</p>
   */

  public void initializeNonEditable()
  {
    this.removeAll();

    setLayout(new BorderLayout());

    mYLabel = new JLabel(JpanelCalendar.month_names[currentMonth] + " " + currentYear);
    mYLabel.setFont(titleFont);
    add(mYLabel, "Center");

    validate();
  }

  /**
   * <p>This method toggles this panel from display mode to editing mode
   * or vice-versa.  If allow is true, this panel will be editable, and
   * the year and month will be changable by the user.</p>
   */

  public void setAllowMonthChange(boolean allow)
  {
    if (this.editable == allow)
      {
	return;
      }

    this.editable = allow;

    if (editable)
      {
	initializeEditable();
      }
    else
      {
	initializeNonEditable();
      }
  }

  public void actionPerformed(ActionEvent e)
  {
    if (e.getSource() == _nextdate) 
      {
	currentMonth++;

	if (currentMonth > 11)
	  {
	    currentMonth = 0;

	    if (debug)
	      {
		System.out.println("Going back to Jan");
	      }

	    setYear(currentYear + 1);
	  }

	month.setSelectedIndex(currentMonth);

	performCallback();
      }
    else if (e.getSource() == _prevdate) 
      {
	currentMonth--;

	if (currentMonth < 0)
	  {
	    currentMonth = 11;

	    if (debug)
	      {
		System.out.println("Going back to Dec");
	      }

	    setYear(currentYear - 1);
	  }

	month.setSelectedIndex(currentMonth);

	performCallback();
      }
  }

  /**
   * This is called when our month is changed
   */

  public void itemStateChanged(ItemEvent e)
  {
    if (e.getStateChange() == ItemEvent.SELECTED)
      {
	if (month.getSelectedIndex() != currentMonth)
	  {
	    int index = month.getSelectedIndex();

	    if (index < 0 || index > 11)
	      {
		throw new IllegalArgumentException("month out of range: " + index);
	      }

	    currentMonth = index;
	    performCallback();
	  }
      }
  }

  private void performCallback()
  {
    container.writeDates();
  }
}
