/*

   framePanel.java

   The individual frames in the windowPanel.
   
   Created: 4 September 1997
   Version: $Revision: 1.3 $ %D%
   Module By: Michael Mulvaney
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.client;

import java.awt.*;
import java.rmi.*;
import java.util.Date;

import com.sun.java.swing.*;
import tablelayout.*;
import arlut.csd.ganymede.*;

import arlut.csd.JDataComponent.*;

public class framePanel extends JInternalFrame {
  
  final static boolean debug = true;

  JTabbedPane 
    pane;

  JBufferedPane
    general,
    dates,
    history;    

  db_field[]
    fields;

  int 
    row = 0;

  boolean 
    editable;

  public framePanel(db_object object, boolean editable, windowPanel parent, String title)
    {
      if (debug)
	{
	  System.out.println("Adding new framePanel");
	}
    
      this.editable = editable;
  
      // Window properties
      
      setMaxable(true);
      setResizable(true);
      setClosable(!editable);
      setIconable(true);
      setLayout(new BorderLayout());
      
      // windowPanel wants to know if framePanel is changed
      addPropertyChangeListener(parent);
      
      setBackground(ClientColor.WindowBG);
      
      setTitle(title);
      
      JMenuBar mb = parent.createMenuBar(editable, object, this);
      setMenuBar(mb);

      // Now setup the framePanel layout
      pane = new JTabbedPane();

      try
	{
	  fields = object.listFields();
	}
      catch (RemoteException rx)
	{
	  throw new RuntimeException("Could not get list of fields in framPanel: " + rx);
	}

      // First panel is a container panel
      general = new JBufferedPane();
      general.setInsets(new Insets(2,2,2,2));
      general.setLayout(new BorderLayout());
      general.setBackground(ClientColor.WindowBG);
      general.add("Center", new containerPanel(object, editable, parent.parent, parent));

      // Second panel contains some dates
      dates = createDatePanel();
      dates.add(new JLabel("date stuff"));

      // Third panel is history information
      history = createHistoryPanel();
      history.add(new JLabel("history stuff"));
      
      // Add the panels to the tabbedPane
      pane.addTab("General", null, general);
      pane.addTab("Dates", null, dates);
      pane.addTab("History", null, history);
      pane.setSelectedIndex(0);

      setLayout(new BorderLayout());
      add("Center", pane);
    }

  JBufferedPane createDatePanel() 
    {
      JBufferedPane panel = new JBufferedPane();
      panel.setLayout(new TableLayout(false));
      if (fields != null)
	{
	  int type = -1;
	  for (int i = 0; i < fields.length ; i++)
	    {
	      try
		{
		  type = fields[i].getID();
		}
	      catch (RemoteException rx)
		{
		  throw new RuntimeException("Can't get type");
		}
	      if (type == SchemaConstants.ExpirationField)
		{
		  try
		    {
		      addDateField(fields[i], panel);
		    }
		  catch (RemoteException rx)
		    {
		      throw new RuntimeException("Could not addDateField: " + rx);
		    }
		}
	      else if (type == SchemaConstants.RemovalField)
		{
		  try
		    {
		      addDateField(fields[i], panel);
		    }
		  catch (RemoteException rx)
		    {
		      throw new RuntimeException("Could not addDateField: " + rx);
		    }
		  
		}
	    }
	}
      
      return panel;
    }

  JBufferedPane createHistoryPanel()
    {
      JBufferedPane panel = new JBufferedPane();
      
      JTextField tf = new JTextField();
      panel.setLayout(new BorderLayout());
      panel.add("Center", tf);
      
      return panel;
      
    }
  
  private void addDateField(db_field field, Container panel) throws RemoteException
  {
    JdateField df = new JdateField();
    
    //objectHash.put(df, field);
    df.setEditable(editable);
    
    try
      {
	Date date = ((Date)field.getValue());
	
	if (date != null)
	  {
	    df.setDate(date);
	  }
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not get date: " + rx);
      }
    
    // note that we set the callback after we initially set the
    // date, to avoid having the callback triggered on a listing

    //df.setCallback(this);
    
    try
      {
	addRow(panel, df, field.getName());
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not check visibility");
      }
    
  }
  
  void addRow(Container parent, Component comp,  String label)
    {
      JLabel l = new JLabel(label);
      comp.setBackground(ClientColor.ComponentBG);
      parent.add("0 " + row + " lthwHW", l);
      parent.add("1 " + row + " lthwHW", comp);
      
      row++;
    }
  
}//framePanel
