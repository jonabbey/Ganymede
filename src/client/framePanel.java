/*

   framePanel.java

   The individual frames in the windowPanel.
   
   Created: 4 September 1997
   Version: $Revision: 1.5 $ %D%
   Module By: Michael Mulvaney
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.client;

import java.awt.*;
import java.rmi.*;
import java.util.*;

import com.sun.java.swing.*;
import tablelayout.*;
import arlut.csd.ganymede.*;

import arlut.csd.JDataComponent.*;

public class framePanel extends JInternalFrame {
  
  final static boolean debug = true;

  JTabbedPane 
    pane;

  // Each tab consists of a JBufferedPanel(with a P at the end of the variable 
  // name), inside a JScrollPane(without the P).
  
  JBufferedPane
    generalP = new JBufferedPane(),
    datesP = new JBufferedPane();
  
  JScrollPane
    general,
    dates,
    history,
    owner;

  Container
    contentPane;

  db_field[]
    fields;

  int 
    row = 0;

  boolean 
    editable;

  db_object
    object;

  windowPanel
    parent;

  String 
    title;

  public framePanel(db_object object, boolean editable, windowPanel parent, String title)
    {
      if (debug)
	{
	  System.out.println("Adding new framePanel");
	}
    
      this.title = title;
      this.parent = parent;
      this.object = object;
      this.editable = editable;
  
      // Window properties
      
      setMaximizable(true);
      setResizable(true);
      setClosable(!editable);
      setIconifiable(true);

      contentPane = getContentPane();

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

      // This creates all the panels we need
      try
	{
	  createPanels(object.listFields());
	}
      catch (RemoteException rx) 
	{
	  throw new RuntimeException("Could not create panels for framePanel: " + rx);
	}
      // Add the panels to the tabbedPane
      pane.addTab("General", null, general);
      pane.addTab("Dates", null, dates);
      pane.addTab("History", null, history);
      pane.addTab("Owner", null, owner);
      pane.setSelectedIndex(0);

      contentPane.add("Center", pane);
    }

  // This makes 4 scrollPanes: general, dates, history, owner
  void createPanels(db_field[] fields) throws RemoteException
    {
      
      // First panel is a container panel
      generalP = new JBufferedPane();
      generalP.setInsets(new Insets(2,2,2,2));
      generalP.setLayout(new BorderLayout());
      generalP.setBackground(ClientColor.WindowBG);
      System.out.println("Adding the container Panel");
      generalP.add("Center", new containerPanel(object, editable, parent.parent, parent));
      
      general = new JScrollPane();
      general.setViewportView(generalP);

      // Second panel contains some dates
      // fourth panel is owner list
      // Make them both on the same pass through

      owner = new JScrollPane();
      
      dates = new JScrollPane();
      dates.setViewportView(datesP);
      
      datesP.setInsets(new Insets(5,5,5,5));
      datesP.setLayout(new TableLayout(false));
      if (fields != null)
	{
	  int type = -1;
	  for (int i = 0; i < fields.length ; i++)
	    {
	      type = fields[i].getID();
	      
	      if (type == SchemaConstants.ExpirationField)
		{
		  try
		    {
		      addDateField(fields[i], datesP);
		    }
		  catch (RemoteException rx)
		    {
		      throw new RuntimeException("Could not addDateField: " + rx);
		    }
		}
	      else if (type == SchemaConstants.RemovalField)
		{
		  addDateField(fields[i], datesP);
		}
	      else if (type == SchemaConstants.OwnerListField)
		{
		  owner.setViewportView(new ownerPanel((invid_field)fields[i], editable));
		}
	    }
	}

      
      // Third panel is history information
      history = new JScrollPane();
      history.setViewportView(new historyPanel());

      
       
    }



 
  
}//framePanel
