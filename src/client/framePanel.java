/*

   framePanel.java

   The individual frames in the windowPanel.
   
   Created: 4 September 1997
   Version: $Revision: 1.10 $ %D%
   Module By: Michael Mulvaney
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.client;

import java.awt.*;
import java.rmi.*;
import java.util.*;

import com.sun.java.swing.*;
import com.sun.java.swing.border.*;
import com.sun.java.swing.event.*;

import jdj.PackageResources;
//import tablelayout.*;
import arlut.csd.ganymede.*;

import arlut.csd.JDataComponent.*;

public class framePanel extends JInternalFrame implements ChangeListener, Runnable {
  
  final static boolean debug = true;

  // Indexes for the tabs in the JTabbedPane
  // These numbers have to correspond to the order they are added as tabs,
  // so they are set to current++ when one is added.  -1 means not added yet.
  int 
    current = 0,
    GENERAL = -1,
    REMOVAL_DATE = -1,
    EXPIRATION_DATE = -1,
    HISTORY = -1,
    OWNER = -1,
    NOTES = -1,
    OBJECTS_OWNED = -1,
    PERSONAE = -1;
  

  JTabbedPane 
    pane;

  // Each of these panes is one of the tabs in the tabbedPane
  JScrollPane 
    general,     // Holds a containerPanel in the ViewportView
    expiration_date,       // Holds a datePanel
    removal_date, 
    owner,       // Holds an ownerPanel
    notes,       // holds a notePanel
    objects_owned;  // Holds an ownershipPanel

  JPanel
    personae;

  boolean
  // _created booleans are true after the corresponding panes are created
    general_created = false,
    expiration_date_created = false,
    removal_date_created = false,
    owner_created = false,
    notes_created = false,
    objects_owned_created = false,
    personae_created = false;;
  
  date_field
    exp_field,
    rem_field,
    creation_date_field,
    modification_date_field;

  string_field
    notes_field,
    creator_field,
    modifier_field;

  invid_field 
    objects_owned_field;

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

  notesPanel
    my_notesPanel = null;

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

      parent.parent.setStatus("Building window.");

      // Window properties
      setMaximizable(true);
      setResizable(true);
      setClosable(!editable);
      setIconifiable(true);

      //setFrameIcon(new ImageIcon((Image)PackageResources.getImageResource(this, "folder-red.gif", getClass())));

      
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

      addPropertyChangeListener(getWindowPanel());
      
      setBackground(ClientColor.WindowBG);
      
      setTitle(title);
      
      JMenuBar mb = parent.createMenuBar(editable, object, this);
      setMenuBar(mb);

      // Now setup the framePanel layout
      pane = new JTabbedPane();
      pane.addChangeListener(this);

      // Create all the panes

      
      
      //fields = (Vector)parent.parent.getBaseHash().get(object);
	

      // Add the panels to the tabbedPane
      general = new JScrollPane();
      pane.addTab("General", null, general);
      GENERAL = current++;
      owner = new JScrollPane();
      pane.addTab("Owner", null, owner);
      OWNER = current++;
      
      // Check to see if this gets an objects_owned panel
      try
	{
	  short id = object.getTypeID();
	  if (id == SchemaConstants.OwnerBase)
	    {
	      objects_owned = new JScrollPane();
	      pane.addTab("Objects Owned", null, objects_owned);
	      OBJECTS_OWNED = current++;
	    }
	  else if (id == SchemaConstants.UserBase)
	    {
	      personae = new JPanel(false);
	      pane.addTab("Personae", null, personae);
	      PERSONAE = current++;
	    }
	}
      catch (RemoteException rx)
	{
	  throw new RuntimeException("Could not check if this is ownerbase: " + rx);
	}


      // Add the notes panel
      notes = new JScrollPane();
      addNotesPanel();

      showGeneralTab();
      contentPane.add("Center", pane);

      contentPane.invalidate();
      parent.validate();
    }

  /**
   * Note that this might be null.
   *
   */

  public notesPanel getNotesPanel()
    {
      return my_notesPanel;
    }

  void create_general_panel()
    {
      if (debug)
	{
	  System.out.println("Creating general panel");
	}
      
      containerPanel cp = new containerPanel(object, editable, parent.parent, parent, this);

      cp.setBorder(new EmptyBorder(new Insets(5,5,10,5)));

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

      try
	{
	  owner.setViewportView(new ownerPanel((invid_field)object.getField(SchemaConstants.OwnerListField), editable, this));
	}
      catch (RemoteException rx)
	{
	  throw new RuntimeException("Could not generate Owner field: " + rx);
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

      my_notesPanel = new notesPanel(notes_field, creator_field, creation_date_field, 
				     modifier_field,
				     modification_date_field, editable, this);

      notes.setViewportView(my_notesPanel);

      notes_created = true;

      notes.invalidate();
      validate();
    }

  void create_objects_owned_panel()
    {
      if (debug)
	{
	  System.out.println("Creating ownership panel");
	}

      invid_field oo = null;
      try
	{
	  oo = (invid_field)object.getField(SchemaConstants.OwnerObjectsOwned);
	}
      catch (RemoteException rx)
	{
	  throw new RuntimeException("Could not get owner objects owned: " + rx);
	}

      objects_owned.setViewportView(new ownershipPanel(oo, editable, this));
      objects_owned_created = true;

      objects_owned.invalidate();
      validate();

    }

  void create_personae_panel()
    {
      if (debug)
	{
	  System.out.println("Creating personae panel()");
	}

      invid_field p = null;
      try
	{
	  p = (invid_field)object.getField(SchemaConstants.UserAdminPersonae);
	}
      catch (RemoteException rx)
	{
	  throw new RuntimeException("Could not get persona field: " + rx);
	}
      personae.setLayout(new BorderLayout());
      personae.add("Center", new personaPanel(p, editable, this));
      personae_created = true;
      
      personae.invalidate();
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
      if (EXPIRATION_DATE == -1)
	{
	  if (debug)
	    {
	      System.out.println("Adding date tabs");
	    }
	  expiration_date = new JScrollPane();
	  pane.addTab("Expiration", null, expiration_date);
	  EXPIRATION_DATE = current++;
	}
    }
  public void addRemovalDatePanel()
    {
      if (REMOVAL_DATE == -1)
	{
	  if (debug)
	    {
	      System.out.println("Adding removal date tabs");
	    }
	  
	  removal_date = new JScrollPane();
	  pane.addTab("Removal", null, removal_date);
	  REMOVAL_DATE = current++;
	}
    }

  public void addNotesPanel()
    {
      if (debug)
	{
	  System.out.println("Adding notes tab");
	}
      pane.addTab("Notes", null, notes);
      NOTES = current++;
      
      try
	{
	  if (notes_field != null)
	    {
	      String notesText = (String)notes_field.getValue();
	      if ((notesText != null) && (! notesText.trim().equals("")))
		{
		  if (debug)
		    {
		      System.out.println("Setting notes test to *" + notesText + "*.");
		    }
		  
		  ImageIcon noteIcon = new ImageIcon((Image)PackageResources.getImageResource(this, "note02.gif", getClass()));
		  
		  pane.setIconAt(NOTES, noteIcon);
		}
	    }
	  else
	    {
	      System.err.println("notes_field is null in framePanel");
	    }
	}
      catch (RemoteException rx)
	{
	  throw new RuntimeException("Could not get notes Text: " + rx);
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
      else if (index == OBJECTS_OWNED)
	{
	  if (! objects_owned_created)
	    {
	      setStatus("Creating objects owned panel");
	      create_objects_owned_panel();
	    }
	}
      else if (index == PERSONAE)
	{
	  if (! personae_created)
	    {
	      setStatus("Creating persona panel");
	      create_personae_panel();
	    }
	}
      else
	{
	  System.err.println("Unknown pane index: " + pane.getSelectedIndex());
	}
      
      parent.parent.setStatus("Done");
      
    }

  // Convienence methods

  gclient getgclient()
    {
      return getWindowPanel().getgclient();
    }

  windowPanel getWindowPanel()
    {
      return parent;
    }

  private void setStatus(String status)
    {
      parent.parent.setStatus(status);
    }
      }
	  //framePanel
