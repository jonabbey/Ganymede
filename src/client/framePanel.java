/*

   framePanel.java

   The individual frames in the windowPanel.
   
   Created: 4 September 1997
   Version: $Revision: 1.6 $ %D%
   Module By: Michael Mulvaney
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.client;

import java.awt.*;
import java.rmi.*;
import java.util.*;

import com.sun.java.swing.*;
import com.sun.java.swing.event.*;
import tablelayout.*;
import arlut.csd.ganymede.*;

import arlut.csd.JDataComponent.*;

public class framePanel extends JInternalFrame implements ChangeListener {
  
  final static boolean debug = true;

  final static int GENERAL = 0;
  final static int DATES = 1;
  final static int HISTORY = 2;
  final static int OWNER = 3;

  JTabbedPane 
    pane;

  // Each tab consists of a JBufferedPanel(with a P at the end of the variable 
  // name), inside a JScrollPane(without the P).
  
  JBufferedPane
    generalP = new JBufferedPane();
  
  JScrollPane
    general,
    dates,
    history,
    owner;

  boolean
    general_created = false,
    dates_created = false,
    history_created = false,
    owner_created = false;
  
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
      pane.addChangeListener(this);

      // Create all the panes
      general = new JScrollPane();
      dates = new JScrollPane();
      history = new JScrollPane();
      owner = new JScrollPane();

      try
	{
	  fields = object.listFields();
	}
      catch (RemoteException rx)
	{
	  throw new RuntimeException("Could not get list of fields in framPanel: " + rx);
	}

      // Add the panels to the tabbedPane
      pane.addTab("General", null, general);
      pane.addTab("Dates", null, dates);
      pane.addTab("History", null, history);
      pane.addTab("Owner", null, owner);
      pane.setSelectedIndex(0);

      contentPane.add("Center", pane);
    }

  void create_general_panel()
    {
      general.setViewportView(new containerPanel(object, editable, parent.parent, parent));
      general_created = true;
      parent.invalidate();
    }

  void create_dates_panel()
    {
      if (fields != null)
	{
	  int type = -1;
	  date_field exp = null;
	  date_field rem = null;
	  try
	    {
	      for (int i = 0; i < fields.length ; i++)
		{
		  type = fields[i].getID();
		  
		  if (type == SchemaConstants.ExpirationField)
		    {
		      exp = (date_field)fields[i];
		    }
		  else if (type == SchemaConstants.RemovalField)
		    {
		      rem = (date_field)fields[i];
		    }
		}
	    }
	  catch (RemoteException rx)
	    {
	      throw new RuntimeException("Could not create date panel: " + rx);
	    }
	  
	  dates.setViewportView(new datePanel(exp, rem, editable));
	  
	}
      dates_created = true;
      parent.invalidate();
    }


  void create_history_panel()
    {

      history.setViewportView(new historyPanel());
      history_created = true;
      parent.invalidate();
    }

  void create_owner_panel()
    {
      if (fields != null)
	{
	  try
	    {
	      for (int i = 0; i < fields.length ; i++)
		{
		  if (fields[i].getID() == SchemaConstants.OwnerListField)
		    {
		      owner.setViewportView(new ownerPanel((invid_field)fields[i], editable));
		      break;
		    }
		}
	    }
	  catch (RemoteException rx)
	    {
	      throw new RuntimeException("Could not generate Owner field: " + rx);
	    }
	}
      owner_created = true;
      parent.invalidate();
    }


  // For the ChangeListener
  public void stateChanged(ChangeEvent e)
    {
      if (general_created && owner_created && dates_created && history_created)
	{
	  pane.removeChangeListener(this);
	  return;
	}
      
      switch (pane.getSelectedIndex())
	{
	case GENERAL:
	  if (! general_created)
	    {
	      parent.parent.setStatus("Creating general panel");
	      create_general_panel();
	    }
	  break;
	case DATES:
	  if (! dates_created)
	    {
	      parent.parent.setStatus("Creating dates panel");
	      create_dates_panel();
	    }
	  break;

	case HISTORY:
	  if (! history_created)
	    {
	      parent.parent.setStatus("Creating history panel");
	      create_history_panel();
	    }
	  break;
	  
	case OWNER:
	  if (! owner_created)
	    {
	      parent.parent.setStatus("Creating owner panel");
	      create_owner_panel();
	    }
	  break;

	default:
	  System.err.println("Unknown pane index: " + pane.getSelectedIndex());
	}
      parent.parent.setStatus("Done");
      
    }


 
  
}//framePanel
