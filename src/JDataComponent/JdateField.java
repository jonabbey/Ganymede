

/*
   JdateField.java

   
   Created: 31 Jul 1996
   Version: $Revision: 1.1 $ %D%
   Module By: Navin Manohar
   Applied Research Laboratories, The University of Texas at Austin

*/


package arlut.csd.JDataComponent;

import java.util.*;
import java.lang.String;
import java.awt.*;
import java.text.*;
import java.net.*;
import java.rmi.*;
import java.awt.event.*;

import com.sun.java.swing.*;

import jdj.PackageResources;

import arlut.csd.JCalendar.*;
import arlut.csd.JDataComponent.*;

import oreilly.Dialog.*;

import gjt.Box;

/*******************************************************************
                                                      JdateField()

 This class defines a date input field object.

*******************************************************************/

public class JdateField extends JPanel implements JsetValueCallback,ActionListener {

  private boolean
    allowCallback = false,
    changed = false, 
    limited,
    unset;

  private JsetValueCallback callback = null;

  private JcomponentAttr valueAttr = null;
        
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
    pCal;

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
    this(null,true,
	 false,null,null,
	 new JcomponentAttr(null,new Font("Helvetica",Font.PLAIN,12),Color.black,Color.gray));
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
   * @param cAttr object used to control the foreground/background/font of this JdateField
   */

  public JdateField(Date date,
		    boolean iseditable,
		    boolean islimited,
		    Date minDate,
		    Date maxDate,
		    JcomponentAttr cAttr)
  { 
    if (date == null)
      {
	my_date = null; // new Date();
      }
    else
      {
	my_date = new Date(date.getTime());
      }

    if (cAttr == null)
      {
	throw new IllegalArgumentException("Invalid Parameter: component attributes are null");
      }

    //    _myTimeZone.setStartRule(Calendar.APRIL,1,Calendar.SUNDAY,2*60*60*1000);
    //_myTimeZone.setEndRule(Calendar.OCTOBER,-1,Calendar.SUNDAY,2*60*60*1000);
    
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
    
    valueAttr =  new JcomponentAttr(null,new Font("Helvetica",Font.PLAIN,12),Color.black,Color.gray);
   
    _date = new JstringField(10,10,valueAttr,iseditable,false,"1234567890/",null,this);

    add(_date,"Center");

    JPanel buttonPanel = new JPanel();
    buttonPanel.setLayout(new BorderLayout());

    Image img = PackageResources.getImageResource(this, "i043.gif", getClass());

    _calendarButton = new JButton(new ImageGlyph(img));
    _calendarButton.addActionListener(this);

    buttonPanel.add(_calendarButton,"West");

    _clearButton = new JButton("Clear");
    _clearButton.addActionListener(this);

    buttonPanel.add(_clearButton, "Center");

    add(buttonPanel, "East");

    // create the pop up calendar

    if (my_date != null)
      {
	_myCalendar.setTime(my_date);
      }

    pCal = new JpopUpCalendar(_myCalendar,this);

    setEditable(iseditable);

    unset = true;

    setDate(my_date);

    setValueAttr(cAttr,true);
  }
   

  public void actionPerformed(ActionEvent e) 
  {
    boolean retval = false;
    Object c = e.getSource();

    /* -- */

    if (c == _calendarButton)
      {
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
	    retval=callback.setValuePerformed(new JValueObject(this,null));
	    changed = false;
	  }
	catch (java.rmi.RemoteException re) 
	  {
	    // throw up an information dialog here
	    
	    InfoDialog _infoD = new InfoDialog(new JFrame(),true,"Date Field Error","There was an error communicating with the server!\n"+re.getMessage());
	    
	    _infoD.show();
	  }

	if (retval == true)
	  {
	    setDate(null);
	  }
      }
  }
  
   /**
   * Contructor that creates a JdateField based on the Date object it is given.  This
   * constructor can be used if the JdateField will be making callbacks to pass its data
   * to the appropriate container.
   *
   * @param parent the container which implements the callback function for this JdateField
   * @param cAttr object used to control the foreground/background/font of this JdateField
   * @param date the Date object to use
   * @param iseditable true if the datefield can be edited by the user
   * @param islimited true if there is to be a restriction on the range of dates
   * @param minDate the oldest possible date that can be entered into this JdateField
   * @param maxDate the newest possible date that can be entered into this JdateField
   * @param cAttr object used to control the foreground/background/font of this JdateField
   */

  public JdateField(Date date,
		   boolean iseditable,
		   boolean islimited,
		   Date minDate,
		   Date maxDate,
		   JcomponentAttr cAttr,
		   JsetValueCallback parent)
  {
    this(date,iseditable,islimited,minDate,maxDate,cAttr);

    setCallback(parent);
    
    _date.setCallback(this);
  }

 /************************************************************/
  ///////////////////
  // Class Methods //
  ///////////////////

  /**
   *
   *
   */
  public void setEditable(boolean editable)
  {
    _date.setEditable(editable);
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
    if (d == null)
      {
	_date.setText("");
	unset = true;
	my_date = null;
	changed = true;
	return;
      }

    System.err.println("setDate() called: " + d);
        
    if (limited)
      {
	if (d.after(maxDate) || d.before(minDate))
	  {
	    throw new IllegalArgumentException("Invalid Parameter: date out of range");
	  }
      }

    String s = _dateformat.format(d);

    System.err.println("formatted date = " + s);

    //
    // ???
    //
    //    if (_date.getValue() == null) 
    //      {
    //	_date.setText(s);
    //      }

    _date.setText(s);

    my_date = d;

    unset = false;
    changed = true;
  }

  /**
   * returns the JcomponentAttr object associated with this JcheckboxField
   *
   */
  public JcomponentAttr getValueAttr()
  {
    return this.valueAttr;
  }

 
  /**
   * sets the background color for the JentryField
   * and forces a repaint
   *
   * @param color the color which will be used
   */
  public void setBackground(Color color)
  {
    setValueBackColor(color,true);
  }

  
  /**
   * sets the background color for the JentryField
   *
   * @param color the color which will be used
   * @param repaint true if the value component needs to be repainted
   */
  public void setValueBackColor(Color color,boolean repaint)
  {
    valueAttr.setBackground(color);
    
    setValueAttr(valueAttr,repaint);
  }
  
  
  /**
   * sets the attributes for the JentryField
   *
   * @param attrib the attributes which will be used
   * @param repaint true if the label component needs to be repainted
   */
  public void setValueAttr(JcomponentAttr attributes,boolean repaint)
  {
    this.valueAttr = attributes;

    super.setFont(attributes.font);
    super.setForeground(attributes.fg);
    super.setBackground(attributes.bg);

    if (repaint)
      {
	this.repaint();
      }
  }


 /**
   *  sets the font for the JentryField and
   *  forces a repaint
   *
   * @param f the font which will be used
   */
  public void setFont(Font f)
  {
    setValueFont(f,true);
  }
  
 /**
   *  sets the font for the JentryField
   *
   * @param f the font which will be used
   * @param repaint true if the value component needs to be repainted
   */
  public void setValueFont(Font f,boolean repaint)
  {
    valueAttr.setFont(f);

    setValueAttr(valueAttr,repaint);
  }

 /**
   * sets the foreground color for the JentryField
   * and forces a repaint.
   *
   * @param color the color which will be used
   */
  public void setForeground(Color color)
  {
    setValueForeColor(color,true);    
  }

 /**
   * sets the foreground color for the JentryField
   *
   * @param color the color which will be used
   * @param repaint true if the value component needs to be repainted
   */
  public void setValueForeColor(Color color,boolean repaint)
  {
    valueAttr.setForeground(color);

    setValueAttr(valueAttr,repaint);
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
	// _date component to loose focus.  This means that we need to update
	// the date value (using the value in _date) and then propogate that value
	// up to the server object.
	
	Date d = null;
      
	try 
	  {
	    d = _dateformat.parse((String)obj);
	  }
	catch (Exception ex) 
	  {
	    // throw up an information dialog here
	    
	    InfoDialog _infoD = new InfoDialog(new JFrame(),true,"Date Field Error","The date you have typed is invalid!\n\nProper format:  MM/DD/YYYY   10/01/1997");
	    
	    _infoD.show();
	    
	    return retval;
	  }

	if (d != null) 
	  {
	    if (limited) 
	      {
		if (d.after(maxDate) || d.before(minDate))
		  {
		    
		    // This means that the date chosen was not within the limits specified by the
		    // constructor.  Therefore, we just reset the selected Components of the chooser
		    // to what they were before they were changed.
		    
		    InfoDialog _infoD = new InfoDialog(new JFrame(),true,
						       "Date Field Error",
						       "The date you have typed is out of range!\n\nValid Range: " + 
						       _dateformat.format(minDate) + 
						       " to " +
						       _dateformat.format(maxDate));
		    _infoD.show();
		    
		    return retval;
		  }
	      }
	    else
	      {
		setDate(d);
		_myCalendar.setTime(d);
		pCal.update();
	      }
	  }
	else 
	  {
	    return retval;
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
		
		InfoDialog _infoD = new InfoDialog(new JFrame(),true,
						   "Date Field Error",
						   "There was an error communicating with the server!\n" +
						   e.getMessage());
		_infoD.show();
	      }
	  }
      }
    else if (comp == pCal) 
      {
	System.out.println("setValuePerformed called by Calendar");

	if (!(obj instanceof Date))
	  {
	    throw new RuntimeException("Error: Invalid value embedded in JValueObject");
	  }
	
	// The user has triggered an update of the date value
	// in the _date field by choosing a date from the 
	// JpopUpCalendar

	//	_date.setText(_dateformat.format(_myCalendar.getTime()));

	if (allowCallback)
	  {
	    // Do a callback to talk to the server
	    
	    try 
	      {
		System.out.println("setValuePerformed called by Calendar --- passing up to container");
		retval=callback.setValuePerformed(new JValueObject(this,my_date));
		changed = false;
	      }
	    catch (java.rmi.RemoteException re) 
	      {
		// throw up an information dialog here
		
		InfoDialog _infoD = new InfoDialog(new JFrame(),true,"Date Field Error","There was an error communicating with the server!\n"+re.getMessage());
		
		_infoD.show();
	      }
	  }

	if (retval == true)
	  {
	    System.err.println("Setting date from calendar to " + _myCalendar.getTime());
	    setDate(_myCalendar.getTime());
	  }
      }
    
    return retval;
  }

  /************************************************************/

  /**
   *
   *
   */
  public void processFocusEvent(FocusEvent e)
  {
    super.processFocusEvent(e);

    switch (e.getID())
      {
      case FocusEvent.FOCUS_LOST:
	{
	 
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
		
		  InfoDialog _infoD = new InfoDialog(new JFrame(),true,"Date Field Error","There was an error communicating with the server!\n"+re.getMessage());
		
		  _infoD.show();
		}
	    }
    
	  // Now, the new value has propogated to the server, so we set
	  // changed to false, so that the next time we loose focus from
	  // this widget, we won't unnecessarily update the server value
	  // if nothing has changed locally.

	  changed = false;

	  break;
	}
      case FocusEvent.FOCUS_GAINED:
	{
	  // nothing yet
	}
      }
  }
}

