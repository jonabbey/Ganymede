/*

   datePanel.java

   The tab that holds date information.
   
   Created: 9 September 1997
   Version: $Revision: 1.9 $ %D%
   Module By: Michael Mulvaney
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.client;

import java.awt.*;
import java.awt.event.*;
import java.rmi.*;
import java.util.*;
import java.text.*;

import com.sun.java.swing.*;
import com.sun.java.swing.border.*;

import arlut.csd.ganymede.*;
import arlut.csd.JDataComponent.*;
import arlut.csd.JCalendar.*;

import arlut.csd.JDialog.*;

public class datePanel extends JPanel implements ActionListener, JsetValueCallback, Runnable {

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

  JLabel
    topLabel;
    
  protected GregorianCalendar 
    my_Calendar;

  protected SimpleTimeZone
    _myTimeZone = (SimpleTimeZone)(SimpleTimeZone.getDefault());

  SimpleDateFormat
    dateformat,
    timeformat;

  String
    label;

  JPanel
    holder,
    actual;

  public datePanel(date_field field, String label, boolean editable, framePanel fp)
  {
    this.editable = editable;
    this.field = field;
    this.label = label;
    this.fp = fp;

    gc = fp.wp.gc;

    setLayout(new BorderLayout());
    
    holder = new JPanel(false);
    holder.add(new JLabel("Loading datePanel, please wait."));

    invalidate();
    fp.validate();

    actual = new JPanel(new BorderLayout());

    Thread thread = new Thread(this);
    thread.start();
  }

  public void run()
  {
    setBorder(new EmptyBorder(new Insets(5,5,5,5)));

    top_pane = new JPanel(false);
    top_pane.setLayout(new BorderLayout());
    top_pane.setMaximumSize(top_pane.getPreferredSize());
    bottom_pane = new JPanel(false);
    bottom_pane.setLayout(new BoxLayout(bottom_pane, BoxLayout.Y_AXIS));

    actual.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    actual.add(top_pane);
    actual.add(bottom_pane);
    actual.add(Box.createVerticalGlue());

    dateformat = new SimpleDateFormat("MMM dd, yyyy",Locale.getDefault());
    timeformat = new SimpleDateFormat("MMM dd, yyyy, at HH:mm a", Locale.getDefault());

    if (editable)
      {
	create_editable_panel();
      }
    else
      {
	create_non_editable_panel();
      }

    System.out.println("Done with thread in datePanel.");
    remove(holder);
    add(BorderLayout.CENTER, actual);

    invalidate();
    fp.validate();
  }

  void create_editable_panel()
  {
    JdateField df = new JdateField();
  
    df.setEditable(editable);

    my_Calendar = new GregorianCalendar(_myTimeZone,Locale.getDefault());
    
    try
      {
	topLabel = new JLabel();
	topLabel.setBorder(new EmptyBorder(new Insets(5,1,5,1)));

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
	
	cal = new JpanelCalendar(my_Calendar, this, false, true);
	bottom_pane.add(cal);
	bottom_pane.add(Box.createGlue());

	top_pane.add("Center", topLabel);
	clear = new JButton("Clear date");
	clear.setActionCommand("Clear");
	clear.addActionListener(this);
	top_pane.add("East", clear);
      }

    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not get date: " + rx);
      }
    
  }

  void create_non_editable_panel()
  {
    try
      {
	if (field != null)
	  {
	    Date d = (Date)field.getValue();

	    if (d != null)
	      {
		top_pane.add("Center", new JLabel(d.toString()));
	      }
	    else
	      {
		top_pane.add("North", new JLabel("No expiration date is set"));
	      }
	  }
	else
	  {
	    top_pane.add("North", new JLabel("No expiration date is set"));
	  }
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not check visibility");
      }
  }
  
  public void actionPerformed(ActionEvent e)
  {
    ReturnVal retVal;

    /* -- */

    System.out.println("Action performed in datePanel");

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
	    topLabel.setText(label + " will be cleared after commit.");
	    fp.wp.getgclient().somethingChanged = true;
	  }
	else
	  {
	    setStatus("Server says:  Could not clear date field.");

	    try
	      {
		System.err.println("last error: " + gc.getSession().getLastError());
	      }
	    catch (RemoteException rx)
	      {
		throw new RuntimeException("Could not get last error: " + rx);
	      }
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
	System.out.println("Removal Calendar says: " + d.toString());

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
	gc.somethingChanged = true;
      }

    return ok;
  }

  private final void setStatus(String s)
  {
    gc.setStatus(s);
  }

}//datePanel
