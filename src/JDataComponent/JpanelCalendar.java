/*
   JpanelCalendar.java

   A GUI Calendar for use with the arlut.csd.JDataComponent JdateField class.

   Created: 17 March 1997
   Version: $Revision: 1.2 $
   Last Mod Date: $Date: 1999/01/20 18:08:03 $
   Release: $Name:  $

   Module By: Navin Manohar, Michael Mulvaney, and Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin
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
 *
 * A GUI Calendar for use with the arlut.csd.JDataComponent JdateField class.
 *
 */

public class JpanelCalendar extends JPanel implements ActionListener, ItemListener {

  static final boolean debug = false;

  // ---

  /**
   *
   * What time do we have set?
   *
   */

  protected GregorianCalendar my_calendar;

  /**
   *
   * What time do we have visible?  Month, year, etc.
   *
   */

  protected GregorianCalendar temp_calendar;
  protected SimpleDateFormat _dateformat = null;
  protected JsetValueCallback parent;

  protected JpopUpCalendar pCal = null;

  protected JdateButton _datebuttonArray[] = new JdateButton[37];

  private JPanel sPa = null;
  protected JButton _prevdate;
  protected JButton _nextdate;
  protected JButton _timeShow;
  protected JButton _close;

  protected JTimePanel _tPanel;
  
  protected int leapDays[] = {31,29,31,30,31,30,31,31,30,31,30,31};
  protected int monthDays[] = {31,28,31,30,31,30,31,31,30,31,30,31};

  protected Font todayFont = new Font("sansserif", Font.BOLD, 12);
  protected Font notTodayFont = new Font("serif", Font.PLAIN, 12);

  int
    current_day,
    current_year;

  boolean
    dateIsSet,
    compact,
    showTime;

  JPanel
    centerPanel;

  JComboBox
    month;

  JYearChooser
    year;

  GridBagLayout
    gbl;

  GridBagConstraints
    gbc;

  Date
    previousDate;

  /* -- */

  /**
   * Lots of constructors.  
   *
   */

  public JpanelCalendar(GregorianCalendar parentCalendar, JsetValueCallback callback)
  {
    this(parentCalendar, callback, true, false);
  }

  /**
   * Constructors.
   *
   * @param showTime If true, then the "Choose a time" part will be there.  Also, if true, time will appear in date at top.
   *
   */
  public JpanelCalendar(GregorianCalendar parentCalendar, JsetValueCallback callback, boolean showTime)
  {
    this(parentCalendar, callback, showTime, false);
  }

  public JpanelCalendar(JpopUpCalendar pC,GregorianCalendar parentCalendar, JsetValueCallback callback) 
  {
    this(pC, parentCalendar, callback, true, false);
  }

  public JpanelCalendar(JpopUpCalendar pC,GregorianCalendar parentCalendar, JsetValueCallback callback, boolean showTime) 
  {
    this(pC, parentCalendar, callback, showTime, false);
  }

  /**
   *
   *
   * @param compact If true, calendar will be drawn smaller.
   */

  public JpanelCalendar(JpopUpCalendar pC,
			GregorianCalendar parentCalendar, 
			JsetValueCallback callback, 
			boolean showTime, boolean compact) 
  {
    this(parentCalendar,callback, showTime, compact);

    if (pC == null)
      {
	throw new IllegalArgumentException("popUpCalendar parameter is null");
      }

    pCal = pC;
    
    _close = new JButton("Close");
    sPa.add(_close,"East");

    _close.addActionListener(this);
  }

  public JpanelCalendar(GregorianCalendar parentCalendar, 
			JsetValueCallback callback,  
			boolean showTime, boolean compact)
  {
    if (parentCalendar == null)
      {
	parentCalendar = new GregorianCalendar();
	dateIsSet = false;
      }
    else
      {
	dateIsSet = true;
      }

    this.showTime = showTime;
    this.compact = compact;

    parent = callback;
    
    my_calendar = parentCalendar;

    previousDate = my_calendar.getTime();
    dateIsSet = true;

    temp_calendar = (GregorianCalendar)my_calendar.clone();

    current_year = temp_calendar.get(Calendar.YEAR);
    current_day = temp_calendar.get(Calendar.DAY_OF_MONTH);

    if (debug)
      {
	System.out.println("Year: " + current_year + " day: " + current_day);
      }

    if (showTime)
      {
	_dateformat = new SimpleDateFormat("MMM yyyy  [hh:mm:ss z]",Locale.getDefault());
      }
    else
      {
	_dateformat = new SimpleDateFormat("MMM yyyy",Locale.getDefault());
      }

    _dateformat.setTimeZone(my_calendar.getTimeZone());

    setLayout(new BorderLayout());

    // First, the north 

    JPanel p1 = new JPanel(false);
    p1.setLayout(new BorderLayout());

    _prevdate = new JButton("<<");
    _prevdate.setToolTipText("Previous Month");
    _nextdate = new JButton(">>");
    _nextdate.setToolTipText("Next Month");

    _prevdate.addActionListener(this);
    
    _nextdate.addActionListener(this);

    month = new JComboBox();
    month.addItem("January");
    month.addItem("Febuary");
    month.addItem("March");
    month.addItem("April");
    month.addItem("May");
    month.addItem("June");
    month.addItem("July");
    month.addItem("August");
    month.addItem("September");
    month.addItem("October");
    month.addItem("November");
    month.addItem("December");

    month.setSelectedIndex(temp_calendar.get(Calendar.MONTH));
    month.addItemListener(this);

    year = new JYearChooser(temp_calendar.get(Calendar.YEAR), this);

    p1.add(_prevdate,"West");
    p1.add(_nextdate,"East");

    JPanel middlePanel = new JPanel(new BorderLayout());
    middlePanel.add("Center", month);
    middlePanel.add("East", year);

    p1.add(middlePanel,"Center");

    JPanel p2 = new JPanel(false);
    p2.setLayout(new GridLayout(1,7));
    
    JPanel northPanel = new JPanel(false);
    northPanel.setLayout(new GridLayout(2,1));
    northPanel.add(p1);
    northPanel.add(p2);

    add(northPanel,"North");

    // Next, the center ( this part contains a bunch of buttons with numbers)

    centerPanel = new JPanel(false);
    gbl = new GridBagLayout();
    gbc = new GridBagConstraints();
    centerPanel.setLayout(gbl);
    
    gbc.gridy = 0;
    gbc.anchor = GridBagConstraints.CENTER;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.insets = new Insets(0,2,0,2);

    gbc.ipadx = 5;

    JLabel sun = new JLabel("SUN");
    sun.setFont(todayFont);
    gbc.gridx = 0;
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

    gbc.gridy = 1;
    gbc.gridx = 0;
    gbc.ipadx = 0;

    for (int i=0;i<37;i++) 
      {
	gbc.gridx = i % 7;
	gbc.gridy = (i / 7) + 1;

	_datebuttonArray[i] = new JdateButton(this, compact);
	gbl.setConstraints(_datebuttonArray[i], gbc);
	centerPanel.add(_datebuttonArray[i]);
      }

    add(centerPanel,"Center");

    // Next, the south ( this part will contain only a label for now)

    //    add(new Label("Please Choose a Date",SwingConstants.CENTER),"South");

    JPanel southPanel = new JPanel();
    southPanel.setLayout(new BorderLayout());

    sPa = new JPanel();
    sPa.setLayout(new BorderLayout());
    southPanel.add(sPa,"North");

    if (showTime)
      {
	_tPanel = new JTimePanel(this);
	_tPanel.setBorder(new TitledBorder("Please choose a time:"));

	southPanel.add(_tPanel,"Center");
	
	add(southPanel,"South");
      }

    writeDates();
  }
 
  public synchronized void setDate(Date date)
  {
    Calendar c = Calendar.getInstance();

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

	c.setTime(date);

	current_year = c.get(Calendar.YEAR);
	current_day = c.get(Calendar.DAY_OF_MONTH);

	// refresh the month visible in the month combo box

	month.setSelectedIndex(c.get(Calendar.MONTH));

	// refresh the calendar page (month/year) visible

	temp_calendar.setTime(date);

	// refresh the recorded time

	my_calendar.setTime(date);

	if (debug)
	  {
	    System.err.println("JpanelCalendar.setDate(): calling writeDates()");
	  }

	// and re-draw

	writeDates();

	if (debug)
	  {
	    System.err.println("JpanelCalendar.setDate(): returned from writeDates()");
	  }
      }

    this.previousDate = date;
  }

  public void setYear(int year)
  {
    current_year = year;
    this.year.setYear(year);  //tihs.year is a JYearChooser
    writeDates();
  }

  public void clear()
  {
    setDate(null);
  }

  /** 
   * This will update the temp_calendar according to the current
   * year and month visible in the GUI controls.
   *
   * Call this from writeDates().
   */

  void updateDate()
  {
    temp_calendar.set(current_year, month.getSelectedIndex(), current_day);
  }

  /**
   *
   * Our buttons call us back here.
   */

  public void actionPerformed(ActionEvent e) 
  {
    if (e.getSource() == _nextdate) 
      {
	int current_month = month.getSelectedIndex() + 1;

	if (current_month > 11)
	  {
	    current_month = 0;

	    if (debug)
	      {
		System.out.println("Going back to Jan");
	      }

	    setYear(year.getYear().intValue() + 1);
	  }

	month.setSelectedIndex(current_month);
      }
    else if (e.getSource() == _prevdate) 
      {
	int current_month = month.getSelectedIndex() - 1;

	if (current_month < 0)
	  {
	    current_month = 11;

	    if (debug)
	      {
		System.out.println("Going back to Dec");
	      }

	    setYear(year.getYear().intValue() - 1);
	  }

	month.setSelectedIndex(current_month);
      }
    else if (e.getSource() == _close)
      {
	if (debug)
	  {
	    System.err.println("Closing pCal");
	  }

	pCal.setVisible(false);
      }
  }

  /**
   *
   * This is called when our month is changed
   *
   */

  public void itemStateChanged(ItemEvent e)
  {
    if (e.getStateChange() == ItemEvent.SELECTED)
      {
	writeDates();
      }
  }

  /**
   *
   * This method takes the current time held in my_calendar and refreshes
   * the calendar with it.
   * 
   */

  public void update() 
  {
    temp_calendar.setTime(my_calendar.getTime()); // this sets all the fields properly
    
    writeDates();
  }

  /**
   *
   * This method updates the calendar buttons from the time information held
   * in temp_calendar.
   *
   */

  protected synchronized void writeDates() 
  {
    updateDate();

    // get a local copy of the calendar object indicating the month and year
    // shown

    GregorianCalendar temp  = (GregorianCalendar) temp_calendar.clone();

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
	    temp_calendar.get(Calendar.YEAR) == my_calendar.get(Calendar.YEAR) &&
	    temp_calendar.get(Calendar.MONTH) == my_calendar.get(Calendar.MONTH) &&
	    my_calendar.get(Calendar.DATE) == day)
	  {
	    if (debug)
	      {
		System.err.println("JpanelCalendar.writeDates(): setting date to " + i);
	      }

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
	_tPanel.update();
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

    if (_bttn == null) 
      {
	throw new IllegalArgumentException("The dateButton parameter is null");
      }
    
    int date = Integer.parseInt(_bttn.getText(),10);

    if (debug)
      {
	System.err.println("setting date to day " + date);
      }

    temp_calendar.set(Calendar.DATE,date);
    
    temp_calendar.setTime(temp_calendar.getTime()); // this sets all the fields properly
    
    my_calendar.setTime(temp_calendar.getTime());

    if (debug)
      {
	System.err.println("my_calendar = " + my_calendar.getTime());
      }

    // clear the button that was pressed previously
    
    writeDates();

    try 
      {
	if (parent != null)
	  {
	    if (!parent.setValuePerformed(new JValueObject(this, my_calendar.getTime())))
	      {
		// constructing a JErrorDialog causes it to be shown, and our thread
		// to be suspended

		new JErrorDialog(new JFrame(),
				 "Date Out Of Range",
				 "The date you have chosen is out of the acceptable range.");

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
    if (_field == null)
      {
	throw new IllegalArgumentException("_field is null");
      }

    if (_field.equals("hour") ) 
      {
	temp_calendar.set(Calendar.HOUR_OF_DAY,_value);
	my_calendar.set(Calendar.HOUR_OF_DAY,_value);
      }
    else if (_field.equals("min")) 
      {
	temp_calendar.set(Calendar.MINUTE,_value);
	my_calendar.set(Calendar.MINUTE,_value);
      }
    else if (_field.equals("sec")) 
      {
	temp_calendar.set(Calendar.SECOND,_value);
	my_calendar.set(Calendar.SECOND,_value);
      }
    else
      {
	return;
      }

    try 
      {
	if (!parent.setValuePerformed(new JValueObject(this, my_calendar.getTime())))
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

    frame.getContentPane().add(new JpanelCalendar(new GregorianCalendar(), null));

    frame.setSize(300,200);
    frame.pack();
    frame.show();
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                                     JdateButton

------------------------------------------------------------------------------*/

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
    setText("00");
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

class JTimePanel extends JPanel implements JsetValueCallback {

  JpanelCalendar _parent;

  JnumberField _hour = null;
  JnumberField _min = null;
  JnumberField _sec = null;

  GregorianCalendar temp = null; 

  public JTimePanel(JpanelCalendar parent)
  {
    if (parent == null)
      {
	throw new IllegalArgumentException("The parameter parent is null");
      }
    
    _parent = parent;


    temp = _parent.temp_calendar;

    _hour = new JnumberField(3,true,true,0,23,this);
    _min = new JnumberField(3,true,true,0,59,this);
    _sec = new JnumberField(3,true,true,0,59,this);

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

  public void update() 
  {
    _hour.setValue(temp.get(Calendar.HOUR_OF_DAY));
    _min.setValue(temp.get(Calendar.MINUTE));
    _sec.setValue(temp.get(Calendar.SECOND));
  }

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
	_parent.timeChanged("hour",val);
      }
    else if (comp == _min)
      {
	_parent.timeChanged("min",val);
      }
    else 
      {
	_parent.timeChanged("sec",val);
      }

    return true;
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                                    JYearChooser
	
------------------------------------------------------------------------------*/

class JYearChooser extends JPanel implements ActionListener {

  static final boolean debug = false;

  // ---

  JnumberField
    year_field;

  JButton
    up,
    down;

  JpanelCalendar
    cal;

  /* -- */

  public JYearChooser(int year, JpanelCalendar parent)
  {
    this.cal = parent;

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
	year_field.setValue(year + 1);
	cal.setYear(year + 1);
      }
    else if (e.getSource() == down)
      {
	int year = year_field.getValue().intValue();
	year_field.setValue(year - 1);
	cal.setYear(year  - 1);
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
}

