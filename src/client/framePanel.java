/*

   framePanel.java

   The individual frames in the windowPanel.
   
   Created: 4 September 1997
   Version: $Revision: 1.8 $ %D%
   Module By: Michael Mulvaney
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.client;

import java.awt.*;
import java.rmi.*;
import java.util.*;

import com.sun.java.swing.*;
import com.sun.java.swing.event.*;

import jdj.PackageResources;
//import tablelayout.*;
import arlut.csd.ganymede.*;

import arlut.csd.JDataComponent.*;

public class framePanel extends JInternalFrame implements ChangeListener, Runnable {
  
  final static boolean debug = true;

  // Indexs for the tabs in the JTabbedPane
  // These numbers have to correspond to the order they are added as tabs,
  // so they are set to current++ when one is added.  -1 means not added yet,
  // but there is also a boolean for this.
  int 
    current = 0,
    GENERAL = -1,
    REMOVAL_DATE = -1,
    EXPIRATION_DATE = -1,
    HISTORY = -1,
    OWNER = -1,
    NOTES = -1;
  

  JTabbedPane 
    pane;

  // Each of these panes is one of the tabs in the tabbedPane
  JScrollPane 
    general,     // Holds a containerPanel in the ViewportView
    expiration_date,       // Holds a datePanel
    removal_date, 
    owner,       // Holds an ownerPanel
    notes;       // holds a notePanel

  boolean
  // _created booleans are true after the corresponding panes are created
    general_created = false,
    expiration_date_created = false,
    removal_date_created = false,
    owner_created = false,
    notes_created = false,
  // _added booleans are true after the tabs are actually added to the JTabbedPane
    expiration_date_tab_added = false,
    removal_date_tab_added = false,
    notes_panel_tab_added = false;
  
  date_field
    exp_field,
    rem_field,
    creation_date_field,
    modification_date_field;

  string_field
    notes_field,
    creator_field,
    modifier_field;

  Container
    contentPane;

  db_field[]
    fields;

  JTextArea
    notesArea;

  int 
    row = 0;

  boolean 
    editable;

  db_object
    object;

  windowPanel
    parent;

  String 
    title,
    last_modified_by;

  Date
    last_modification_date;

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

      setFrameIcon(new ImageIcon((Image)PackageResources.getImageResource(this, "folder-red.gif", getClass())));

      
      Thread thread = new Thread(this);
      thread.start();

    }

  public void run()
    {
      if (debug)
	{
	  System.out.println("Starting thread in framePanel");
	}


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
      owner = new JScrollPane();
      notes = new JScrollPane();

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
      pane.addTab("Owner", null, owner);
      OWNER = current++;

      //
      // Check for dates and notes, if they are set
      /*
      if (fields != null)
	{
	  int type = -1;
	  try
	    {
	      boolean expiration_found = false;
	      boolean removal_found = false;
	      boolean notes_found = false;
	      boolean creation_date_found = false;
	      boolean creator_found = false;
	      boolean modification_date_found = false;
	      boolean modifier_found = false;
	      
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

		  else if (type == SchemaConstants.RemovalField)
		    {
		      rem_field = (date_field)fields[i];
		      removal_found = true;
		      if (rem_field.getValue() != null)
			{
			  addRemovalDatePanel();
			}
		    }

		  else if (type == SchemaConstants.NotesField)
		    {
		      notes_field = (string_field)fields[i];
		      notes_found = true;
		    }
		  
		  else if (type == SchemaConstants.CreationDateField)
		    {
		      creation_date_field = (date_field)fields[i];
		      creation_date_found = true;
		      

		    }
		  else if (type == SchemaConstants.CreatorField)
		    {
		      creator_field = (string_field)fields[i];
		      creator_found = true;

		    }
		  else if (type == SchemaConstants.ModificationDateField)
		    {
		      modification_date_field = (date_field)fields[i];
		      modification_date_found = true;
		    }
		  else if (type == SchemaConstants.ModifierField)
		    {
		      modifier_field = (string_field)fields[i];
		      modifier_found = true;

		    }

		  if (creation_date_found && creator_found && notes_found)
		    {
		      addNotesPanel();
		    }

		  if (notes_found && removal_found && expiration_found &&
		       modifier_found && creator_found && creation_date_found &&
		       modification_date_found) // Stop looking
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

	*/

      try
	{
	  exp_field = (date_field)object.getField(SchemaConstants.ExpirationField);
	  rem_field = (date_field)object.getField(SchemaConstants.RemovalField);
	  notes_field = (string_field)object.getField(SchemaConstants.NotesField);
	  creation_date_field = (date_field)object.getField(SchemaConstants.CreationDateField);
	  creator_field = (string_field)object.getField(SchemaConstants.CreatorField);
	  modification_date_field = (date_field)object.getField(SchemaConstants.ModificationDateField);
	  modifier_field = (string_field)object.getField(SchemaConstants.ModifierField);

	}
      catch (RemoteException rx)
	{
	  throw new RuntimeException("Could not get field information: " + rx);
	}


      showGeneralTab();
      contentPane.add("Center", pane);

      /*
      JBufferedPane bottom = new JBufferedPane(false);

      try
	{
	  last_modification_date = (Date)modification_date_field.getValue();
	  last_modified_by = (String)modifier_field.getValue();
	}
      catch (RemoteException rx)
	{
	  throw new RuntimeException("Could not get last modified field: " + rx);
	}
      
      //bottom.add(new JLabel("Last modified on: " + last_modification_date + " by " + last_modified_by, JLabel.LEFT));
	

      //contentPane.add("South", bottom);
      */
      contentPane.invalidate();
      parent.validate();
    }

  void create_general_panel()
    {
      if (debug)
	{
	  System.out.println("Creating general panel");
	}
      
      containerPanel cp = new containerPanel(object, editable, parent.parent, parent, this);
      cp.setInsets(new Insets(5,5,10,5));

      general.setViewportView(cp);
      general_created = true;
      general.invalidate();
      validate();
    }

  public void validate_general()
    {
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
		      owner.setViewportView(new ownerPanel((invid_field)fields[i], editable, this));
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

  void create_notes_panel()
    {
      if (debug)
	{
	  System.out.println("Creating notes panel");
	}

      notes.setViewportView(new notesPanel(notes_field, creator_field, creation_date_field, modifier_field,
					  modification_date_field, editable));

      notes.invalidate();
      validate();
    }


  /**
   * These add the tabs to the framePanel, but they don't create the content
   *
   * The create_ methods create the actual panes, after the pane is selected.
   * If you want to create a panel, call addWhateverPanel, then showWhateverPanel.
   */
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

  public void addNotesPanel()
    {
      if (! notes_panel_tab_added)
	{
	  if (debug)
	    {
	      System.out.println("Adding notes tab");
	    }
	  
	  pane.addTab("Notes", null, notes);
	  NOTES = current++;
	  notes_panel_tab_added = true;
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

  public void showNotesTab()
    {
      if (NOTES == -1)
	{
	  System.out.println("Notes tab has not been created.");
	  return;
	}
      pane.setSelectedIndex(NOTES);
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
      if (general_created && owner_created && expiration_date_created && removal_date_created && notes_created)
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
      else if (index == NOTES)
	{
	  if (! notes_created)
	    {
	      parent.parent.setStatus("Creating notes panel");
	      create_notes_panel();
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
