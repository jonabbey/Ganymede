/*

   framePanel.java

   The individual frames in the windowPanel.
   
   Created: 4 September 1997
   Version: $Revision: 1.7 $ %D%
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

  // Indexs for the tabs in the JTabbedPane
  // These numbers have to correspond to the order they are added as tabs,
  // so they are set to current++ when one is added.
  int 
    current = 0,
    GENERAL = -1,
    REMOVAL_DATE = -1,
    EXPIRATION_DATE = -1,
    HISTORY = -1,
    OWNER = -1;

  JTabbedPane 
    pane;

  // Each of these panes is one of the tabs in the tabbedPane
  JScrollPane 
    general,     // Holds a containerPanel in the ViewportView
    expiration_date,       // Holds a datePanel
    removal_date, 
    history,     // Holds a historyPanel
    owner;       // Holds an ownerPanel

  boolean
  // _created booleans are true after the corresponding panes are created
    general_created = false,
    expiration_date_created = false,
    removal_date_created = false,
    history_created = false,
    owner_created = false,
  // _added booleans are true after the tabs are actually added to the JTabbedPane
    expiration_date_tab_added = false,
    removal_date_tab_added = false;
  
  date_field
    exp_field,
    rem_field;

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
      expiration_date = new JScrollPane();
      removal_date = new JScrollPane();
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
      GENERAL = current++;
      pane.addTab("History", null, history);
      HISTORY = current++;
      pane.addTab("Owner", null, owner);
      OWNER = current++;


      if (fields != null)
	{
	  int type = -1;
	  try
	    {
	      boolean expiration_found = false;
	      boolean removal_found = false;
	      for (int i = 0; i < fields.length ; i++)
		{
		  type = fields[i].getID();
		  
		  if (type == SchemaConstants.ExpirationField)
		    {
		      exp_field = (date_field)fields[i];
		      expiration_found = true;
		      if (exp_field.getValue() != null)
			{
			  addExpirationDatePanel();
			}
		    }
		  if (type == SchemaConstants.RemovalField)
		    {
		      rem_field = (date_field)fields[i];
		      removal_found = true;
		      if (rem_field.getValue() != null)
			{
			  addRemovalDatePanel();
			}
		    }
		  if (removal_found && expiration_found) // Stop looking
		    {
		      break;
		    }
		}
	    }
	  catch (RemoteException rx)
	    {
	      throw new RuntimeException("Could not create date panel: " + rx);
	    }      
	}
      showGeneralTab();
      contentPane.add("Center", pane);
    }

  void create_general_panel()
    {
      if (debug)
	{
	  System.out.println("Creating general panel");
	}
      general.setViewportView(new containerPanel(object, editable, parent.parent, parent));
      general_created = true;
      general.invalidate();
      validate();
    }

  void create_expiration_date_panel()
    {
      if (debug)
	{
	  System.out.println("Creating date panel");
	}
      expiration_date.setViewportView(new datePanel(exp_field, "Expiration date", editable, this));
	  
      expiration_date_created = true;
      expiration_date.invalidate();
      validate();
    }

  void create_removal_date_panel()
    {
      if (debug)
	{
	  System.out.println("Creating removal date panel");
	}
      removal_date.setViewportView(new datePanel(rem_field, "Removal Date", editable, this));
	  
      removal_date_created = true;
      removal_date.invalidate();
      validate();
    }


  void create_history_panel()
    {
      if (debug)
	{
	  System.out.println("Creating history panel");
	}
      history.setViewportView(new historyPanel());
      history_created = true;

      history.invalidate();
      validate();
    }

  void create_owner_panel()
    {
      if (debug)
	{
	  System.out.println("Creating owner panel");
	}
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

      owner.invalidate();
      validate();
    }

  public void addExpirationDatePanel()
    {
      if (! expiration_date_tab_added)
	{
	  if (debug)
	    {
	      System.out.println("Adding date tabs");
	    }
	  
	  pane.addTab("Expiration", null, expiration_date);
	  EXPIRATION_DATE = current++;
	  expiration_date_tab_added = true;
	}
    }
  public void addRemovalDatePanel()
    {
      if (! removal_date_tab_added)
	{
	  if (debug)
	    {
	      System.out.println("Adding removal date tabs");
	    }
	  
	  pane.addTab("Removal", null, removal_date);
	  REMOVAL_DATE = current++;
	  removal_date_tab_added = true;
	}
    }


	
  public void showGeneralTab()
    {
      if (GENERAL == -1)
	{
	  System.out.println("General tab has not been created.");
	  return;
	}
      pane.setSelectedIndex(GENERAL);
    }

  public void showOwnerTab()
    {
      if (OWNER == -1)
	{
	  System.out.println("Owner tab has not been created.");
	  return;
	}
      pane.setSelectedIndex(OWNER);
    }

  public void showHistoryTab()
    {
      if (HISTORY == -1)
	{
	  System.out.println("History tab has not been created.");
	  return;
	}
      pane.setSelectedIndex(HISTORY);
    }

  public void showExpirationDateTab()
    {
      if (EXPIRATION_DATE == -1)
	{
	  System.out.println("Expiration date tab has not been created.");
	  return;
	}
      pane.setSelectedIndex(EXPIRATION_DATE);
    }

  public void showRemovalDateTab()
    {
      if (REMOVAL_DATE == -1)
	{
	  System.out.println("Removal date tab has not been created.");
	  return;
	}
      pane.setSelectedIndex(REMOVAL_DATE);
    }

  // For the ChangeListener
  public void stateChanged(ChangeEvent e)
    {
      if (general_created && owner_created && expiration_date_created && removal_date_created && history_created)
	{
	  pane.removeChangeListener(this);
	  return;
	}
      
      int index = pane.getSelectedIndex();
	
      if (index == GENERAL)
	{
	  if (! general_created)
	    {
	      parent.parent.setStatus("Creating general panel");
	      create_general_panel();
	    }
	}
      else if (index == EXPIRATION_DATE)
	{
	  if (! expiration_date_created)
	    {
	      parent.parent.setStatus("Creating dates panel");
	      create_expiration_date_panel();
	    }
	}
      else if (index == REMOVAL_DATE)
	{
	  if (! removal_date_created)
	    {
	      parent.parent.setStatus("Creating dates panel");
	      create_removal_date_panel();
	    }
	}
      else if (index == HISTORY)
	{
	  if (! history_created)
	    {
	      parent.parent.setStatus("Creating history panel");
	      create_history_panel();
	    }
	}
      else if (index == OWNER)
	{
	  if (! owner_created)
	    {
	      parent.parent.setStatus("Creating owner panel");
	      create_owner_panel();
	    }
	}
      else
	{
	  System.err.println("Unknown pane index: " + pane.getSelectedIndex());
	}
      parent.parent.setStatus("Done");
      
    }


 
  
}//framePanel
