/*

   datePanel.java

   The tab that holds date information.
   
   Created: 9 September 1997
   Version: $Revision: 1.11 $ %D%
   Module By: Michael Mulvaney
   Applied Research Laboratories, The University of Texas at Austin

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

  JProgressBar 
    progressBar;

  public datePanel(date_field field, String label, boolean editable, framePanel fp)
  {
    this.editable = editable;
    this.field = field;
    this.label = label;
    this.fp = fp;

    gc = fp.wp.gc;

    setLayout(new BorderLayout());
    
    progressBar = new JProgressBar();
    progressBar.setMinimum(0);
    progressBar.setMaximum(6);
    progressBar.setValue(0);

    holder = new JPanel(false);
    holder.add(new JLabel("Loading datePane"));
    holder.add(progressBar);

    add("Center", holder);

    invalidate();
    fp.validate();
    progressBar.setValue(1);

    Thread thread = new Thread(this);
    thread.start();
  }

  public void run()
  {
    setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

    top_pane = new JPanel(false);
    top_pane.setLayout(new BorderLayout());
    top_pane.setMaximumSize(top_pane.getPreferredSize());

    progressBar.setValue(2);

    dateformat = new SimpleDateFormat("MMM dd, yyyy",Locale.getDefault());
    timeformat = new SimpleDateFormat("MMM dd, yyyy, at HH:mm a", Locale.getDefault());

    progressBar.setValue(3);

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
    JdateField df = new JdateField();
  
    df.setEditable(editable);

    my_Calendar = new GregorianCalendar(_myTimeZone,Locale.getDefault());
    
    progressBar.setValue(4);

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
	
	progressBar.setValue(5);

	cal = new JpanelCalendar(my_Calendar, this, true, true);

	progressBar.setValue(6);

	top_pane.add("Center", topLabel);
	clear = new JButton("Clear date");
	clear.setActionCommand("Clear");
	clear.addActionListener(this);
	System.out.println("adding clear button to top_pane");
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
	progressBar.setValue(4);
	if (field != null)
	  {
	    Date d = (Date)field.getValue();

	    progressBar.setValue(5);
	    if (d != null)
	      {
		top_pane.add("Center", new JLabel(field.getName() + " is set to: " + d.toString()));
	      }
	    else
	      {
		top_pane.add("North", new JLabel("No date is set"));
	      }
	  }
	else
	  {
	    progressBar.setValue(5);
	    top_pane.add("North", new JLabel("No date is set"));
	  }
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not check visibility: " + rx);
      }

    progressBar.setValue(6);
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
	    fp.wp.getgclient().somethingChanged();
	    Invid invid = fp.getObjectInvid();
	    try
	      {
		int type = field.getType();
		if (type == SchemaConstants.ExpirationField)
		  {
		    fp.wp.getgclient().addToExpireHash(invid);
		  }
		else if (type == SchemaConstants.RemovalField)
		  {
		    fp.wp.getgclient().addToRemoveHash(invid);		
		  }
	      }
	    catch (RemoteException rx)
	      {
		throw new RuntimeException("Could not tell the gclient: " + rx);
	      }
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
	    Invid invid = fp.getObjectInvid();
	    try
	      {
		int type = field.getType();
		if (type == SchemaConstants.ExpirationField)
		  {
		    fp.wp.getgclient().addToExpireHash(invid);
		  }
		else if (type == SchemaConstants.RemovalField)
		  {
		    fp.wp.getgclient().addToRemoveHash(invid);		
		  }
	      }
	    catch (RemoteException rx)
	      {
		throw new RuntimeException("Could not tell the client: " + rx);
	      }
	  }
      }

    if (ok)
      {
	gc.somethingChanged();
      }

    return ok;
  }

  private final void setStatus(String s)
  {
    gc.setStatus(s);
  }

}//datePanel
