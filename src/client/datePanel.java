/*

   datePanel.java

   The tab that holds date information.
   
   Created: 9 September 1997
   Version: $Revision: 1.5 $ %D%
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
import tablelayout.*;
import arlut.csd.ganymede.*;

import arlut.csd.JDataComponent.*;
import arlut.csd.JCalendar.*;

public class datePanel extends JBufferedPane implements ActionListener, JsetValueCallback {

  boolean 
    editable;

  framePanel
    parent;

  date_field
    field;

  JBufferedPane
    top_pane,
    bottom_pane;

  JBorderedPane
    top_border;

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

  public datePanel(date_field field, String label, boolean editable, framePanel parent)
  {
    this.editable = editable;
    this.field = field;
    this.label = label;
    this.parent = parent;
    
    setBuffered(false);

    setInsets(new Insets(5,5,5,5));

    top_pane = new JBufferedPane(false);
    top_pane.setLayout(new BorderLayout());
    top_pane.setMaximumSize(top_pane.getPreferredSize());
    bottom_pane = new JBufferedPane(false);
    bottom_pane.setLayout(new BoxLayout(bottom_pane, BoxLayout.Y_AXIS));

    top_border = new JBorderedPane();
    top_border.setLayout(new BorderLayout());
    top_border.setBorder(BorderFactory.createTitledBorder(top_pane, label + " Date"));
    top_border.add("Center", top_pane);

    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    add(top_border);
    add(bottom_pane);
    add(Box.createVerticalGlue());

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
  }

  void create_editable_panel()
  {
    JdateField df = new JdateField();
  
    df.setEditable(editable);

    my_Calendar = new GregorianCalendar(_myTimeZone,Locale.getDefault());
    
    try
      {
	topLabel = new JLabel();
	topLabel.setInsets(new Insets(5,1,5,1));

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
	JBorderedPane cal_border = new JBorderedPane();
	cal_border.setBorder(BorderFactory.createGroovedBorder());
	cal_border.setLayout(new BorderLayout());
	cal_border.add("Center", cal);
	bottom_pane.add(cal_border);
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
      System.out.println("Action performed in datePanel");
      if (e.getActionCommand().equals("Clear"))
	{
	  boolean ok = false;
	  try
	    {
	      ok = field.setValue(null);
	    }
	  catch (RemoteException rx)
	    {
	      throw new RuntimeException("Could not clear date field: " + rx);
	    }
	  if (ok)
	    {
	      cal.clear();
	      topLabel.setText(label + " will be cleared after commit.");
	      parent.parent.getgclient().somethingChanged = true;
	    }
	  else
	    {
	      parent.parent.parent.setStatus("Server says:  Could not clear date field.");
	      try
		{
		  System.err.println("last error: " + parent.parent.parent.getSession().getLastError());
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
      boolean ok = false;
      if (o.getSource() == cal)
	{
	  Date d = (Date)o.getValue();
	  System.out.println("Removal Calendar says: " + d.toString());
	  try
	    {
	      ok = field.setValue(d);
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
	  parent.parent.getgclient().somethingChanged = true;
	}
      return ok;
    }


}//datePanel
