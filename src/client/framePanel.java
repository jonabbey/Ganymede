/*

   framePanel.java

   The individual frames in the windowPanel.
   
   Created: 4 September 1997
   Version: $Revision: 1.19 $ %D%
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
  // so they are set to current++ when one is added.  -1 means not added yet.(to the pane)
  // An index of > -1 does NOT mean the pane has been created.
  public int 
    current = 0,
    general_index = -1,
    removal_date_index = -1,
    expiration_date_index = -1,
    history_index = -1,
    owner_index = -1,
    admin_history_index = -1,
    notes_index = -1,
    objects_owned_index = -1,
    personae_index = -1;

  JProgressBar
    progressBar;

  JPanel
    progressPanel;

  JTabbedPane 
    pane;

  Color
    vectorBG = Color.lightGray;

  boolean
    darkNow = true;

  // Each of these panes is one of the tabs in the tabbedPane
  JScrollPane 
    general,     // Holds a containerPanel in the ViewportView
    expiration_date,       // Holds a datePanel
    removal_date, 
    owner,       // Holds an ownerPanel
    notes,       // holds a notePanel
    history,      // holds an historyPanel
    admin_history, // holds an adminHistoryPanel (only for adminPersonae)
    objects_owned;  // Holds an ownershipPanel

  Image
    waitImage = null;

  JPanel
    personae;

  Vector
    createdList = new Vector(); // contains the Integers that have been created

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

  int
    row = 0;

  boolean 
    editable;

  db_object
    object;

  windowPanel
    wp;

  String 
    title,
    last_modified_by;

  Date
    last_modification_date;

  notesPanel
    my_notesPanel = null;

  Invid
    invid;

  public framePanel(db_object object, boolean editable, windowPanel winP, String title)
    {
      if (debug)
	{
	  System.out.println("Adding new framePanel");
	}
    
      this.title = title;
      this.wp = winP;
      this.object = object;
      this.editable = editable;

      setStatus("Building window.");

      // Window properties
      setMaximizable(true);
      setResizable(true);
      setClosable(!editable);
      setIconifiable(true);

      //setFrameIcon(new ImageIcon((Image)PackageResources.getImageResource(this, "folder-red.gif", getClass())));

      progressPanel = new JPanel();
      progressBar = new JProgressBar();
      progressPanel.add(new JLabel("Loading..."));
      progressPanel.add(progressBar);

      getContentPane().add("Center", progressPanel);

      try
	{
	  invid = object.getInvid();
	}
      catch (RemoteException rx)
	{
	  throw new RuntimeException("Could not get object's invid");
	}
      
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
      
      System.out.println("Creating menu bar");
      JMenuBar mb = wp.createMenuBar(editable, object, this);
      setMenuBar(mb);

      // Now setup the framePanel layout
      pane = new JTabbedPane();
      
      // Add the panels to the tabbedPane
      System.out.println("Adding general_index " + current);
      general = new JScrollPane();
      pane.addTab("General", null, general);
      general_index = current++;
      System.out.println("general_index=" + general_index);
      owner = new JScrollPane();
      pane.addTab("Owner", null, owner);
      owner_index = current++;
      
      // Check to see if this gets an objects_owned panel
      short id = -1;
      try
	{
	  id = object.getTypeID();
	  if (id == SchemaConstants.OwnerBase)
	    {
	      objects_owned = new JScrollPane();
	      pane.addTab("Objects Owned", null, objects_owned);
	      objects_owned_index = current++;
	    }
	  else if (id == SchemaConstants.UserBase)
	    {
	      personae = new JPanel(false);
	      pane.addTab("Personae", null, personae);
	      personae_index = current++;
	    }
	}
      catch (RemoteException rx)
	{
	  throw new RuntimeException("Could not check if this is ownerbase: " + rx);
	}


      // Add the notes panel
      try
	{
	  notes_field = (string_field)object.getField(SchemaConstants.NotesField);
	}
      catch (RemoteException rx)
	{
	  throw new RuntimeException("Could not get notes_field: " + rx);
	}

      notes = new JScrollPane();
      addNotesPanel();

      // Add the history tab
      history = new JScrollPane();
      pane.addTab("History", null, history);
      history_index = current++;

      if (id == SchemaConstants.PersonaBase)
	{
	  admin_history = new JScrollPane();
	  pane.addTab("Admin History", null, admin_history);
	  admin_history_index = current++;
	}

      // Do we need to show expiration or removal dates?

      try
	{
	  exp_field = (date_field)object.getField(SchemaConstants.ExpirationField);
	  rem_field = (date_field)object.getField(SchemaConstants.RemovalField);
	  
	  if ((exp_field != null) && (exp_field.getValue() != null))
	    {
	      addExpirationDatePanel();
	    }

	  if ((rem_field != null) && (rem_field.getValue() != null))
	    {
	      addRemovalDatePanel();
	    }
	}
      catch (RemoteException rx)
	{
	  throw new RuntimeException("Could not get date fields: " + rx);
	}

     

      pane.addChangeListener(this);

      // Create all the panes

      createPanel(general_index);
      showTab(general_index);
      contentPane.remove(progressPanel);
      contentPane.add("Center", pane);

      contentPane.invalidate();
      System.out.println("Calling validate in the fp");
      validate();
      System.out.println("Done Calling validate in the fp");
    }

  /**
   * Return the invid of the object contained in this frame panel.
   */

  public Invid getObjectInvid()
  {
    return invid;
  }


  /**
   * Note that this might be null.
   *
   */

  public notesPanel getNotesPanel()
    {
      return my_notesPanel;
    }

  /*
  public void invalidate()
  {
    System.out.println("--Invalidate framePanel");
    super.invalidate();
  }

  public void validate()
  {
    System.out.println("--validate framePanel");
    super.validate();
  }
  */
    

  public void printObject()
  {
    PrintJob j = Toolkit.getDefaultToolkit().getPrintJob(getgclient(), "Print window", new Properties());

    if (j == null)
      {
	System.out.println("Cancelled");
	return;
      }

    Graphics page = j.getGraphics();

    int index = pane.getSelectedIndex();
    if (index < 0)
      {
	System.out.println("No pane selected?");
      }
    else
      {
	System.out.println("Printing " + index);
	pane.getComponentAt(index).print(page);
      }

    page.dispose();
    j.end();

  }

  public Image getWaitImage()
  {
    if (waitImage == null)
      {
	waitImage = PackageResources.getImageResource(this, "atwork01.gif", getClass());
      }
    
    return waitImage;

  }
  //This need to be changed to show the progress bar
  void create_general_panel()
    {
      if (debug)
	{
	  System.out.println("Creating general panel");
	}
      
      containerPanel cp = new containerPanel(object, editable, wp.gc, wp, this, progressBar);

      cp.setBorder(wp.emptyBorder10);

      general.setViewportView(cp);
      //general.setViewportView(progressBar);
      createdList.addElement(new Integer(general_index));
      setStatus("Done");
      
    }

  void create_expiration_date_panel()
    {
      if (debug)
	{
	  System.out.println("Creating date panel");
	}

      if (exp_field == null)
	{
	  try
	    {
	      exp_field = (date_field)object.getField(SchemaConstants.ExpirationField);
	    }
	  catch (RemoteException rx)
	    {
	      throw new RuntimeException("Could not get removal field: " + rx);
	    }
	}

      expiration_date.setViewportView(new datePanel(exp_field, "Expiration date", editable, this));
	  
      createdList.addElement(new Integer(expiration_date_index));

      setStatus("Done");
      
    }

  void create_removal_date_panel()
    {
      if (debug)
	{
	  System.out.println("Creating removal date panel");
	}

      if (rem_field == null)
	{
	  try
	    {
	      rem_field = (date_field)object.getField(SchemaConstants.RemovalField);
	    }
	  catch (RemoteException rx)
	    {
	      throw new RuntimeException("Could not get removal field: " + rx);
	    }
	}

      removal_date.setViewportView(new datePanel(rem_field, "Removal Date", editable, this));
	  
      createdList.addElement(new Integer(removal_date_index));

      setStatus("Done");

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
	
      createdList.addElement(new Integer(owner_index));

      setStatus("Done");
      
      owner.invalidate();
      validate();
    }

  void create_history_panel()
    {
      setStatus("Creating history panel");
      history.setViewportView(new historyPanel(invid, getgclient()));
	
      createdList.addElement(new Integer(history_index));
      
      history.invalidate();
      validate();

      setStatus("Done");
    }


  void create_admin_history_panel()
    {
      setStatus("Creating admin history panel");
      admin_history.setViewportView(new adminHistoryPanel(invid, getgclient()));
	
      createdList.addElement(new Integer(admin_history_index));
      
      admin_history.invalidate();
      validate();

      setStatus("Done");
    }

  void create_notes_panel()
    {
      if (debug)
	{
	  System.out.println("Creating notes panel");
	}

      try
	{
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

      createdList.addElement(new Integer(notes_index));
      setStatus("Done");

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

      if (oo == null)
	{
	  JPanel null_oo = new JPanel();
	  null_oo.add(new JLabel("There are no objects owned here."));
	  objects_owned.setViewportView(null_oo);
	}
      else
	{
	  objects_owned.setViewportView(new ownershipPanel(oo, editable, this));
	  createdList.addElement(new Integer(objects_owned_index));
	}

      objects_owned.invalidate();
      validate();

      setStatus("Done");
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
      createdList.addElement(new Integer(personae_index));
      
      personae.invalidate();
      validate();

      setStatus("Done");
    }

  /**
   * These add the tabs to the framePanel, but they don't create the content
   *
   * The create_ methods create the actual panes, after the pane is selected.
   * If you want to create a panel, call addWhateverPanel, then showWhateverPanel.
   */
  public void addExpirationDatePanel()
    {
      if (expiration_date_index == -1)
	{
	  if (debug)
	    {
	      System.out.println("Adding date tabs");
	    }
	  expiration_date = new JScrollPane();
	  pane.addTab("Expiration", null, expiration_date);
	  expiration_date_index = current++;
	}
    }
  public void addRemovalDatePanel()
    {
      if (removal_date_index == -1)
	{
	  if (debug)
	    {
	      System.out.println("Adding removal date tabs");
	    }
	  
	  removal_date = new JScrollPane();
	  pane.addTab("Removal", null, removal_date);
	  removal_date_index = current++;
	}
    }

  public void addNotesPanel()
    {
      if (debug)
	{
	  System.out.println("Adding notes tab");
	}
      pane.addTab("Notes", null, notes);
      notes_index = current++;
      
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

		  System.out.println("Adding the noteIcon");
		  ImageIcon noteIcon = new ImageIcon((Image)PackageResources.getImageResource(this, "note02.gif", getClass()));
		  
		  pane.setIconAt(notes_index, noteIcon);
		}
	      else
		{
		  System.out.println("Empty notes");
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

  //
  // showPanel(int) will generate the actual panel.  I don't know if we need
  // showPanel after each of these, or just for the first one.  It is kinda
  // a hack anyway, since the whole framePanel changed so many times.  Maybe
  // the whole thing should be overhauled.
  //

  // I don't know if need these at all.  There could be one showTab(int).  Why use these
  // anyway?

  public void showTab(int index)
  {
    if (index == -1)
      {
	System.out.println("This tab has not been added to the pane yet.");
      }
    pane.setSelectedIndex(index);
  }

  // For the ChangeListener
  public void stateChanged(ChangeEvent e)
    {
      int index = pane.getSelectedIndex();

      if (!createdList.contains(new Integer(index)))
	{
	  System.out.println("Creating a new pane.");
	  createPanel(index);
	}
    }

  /**
   * This checks to see if the panel is created, and creates it if needed.
   */

  public void createPanel(int index)
    {
      System.out.println("index = " + index + " general_index= " + general_index);
	
      if (index == general_index)
	{
	  setStatus("Creating general panel");
	  create_general_panel();
	}
      else if (index == expiration_date_index)
	{
	  setStatus("Creating dates panel");
	  create_expiration_date_panel();
	}
      else if (index == removal_date_index)
	{
	  setStatus("Creating dates panel");
	  create_removal_date_panel();
	}
      else if (index == notes_index)
	{
	  setStatus("Creating notes panel");
	  create_notes_panel();
	}
      else if (index == owner_index)
	{
	  setStatus("Creating owner panel");
	  create_owner_panel();
	}
      else if (index == objects_owned_index)
	{
	  setStatus("Creating objects owned panel");
	  create_objects_owned_panel();
	}
      else if (index == personae_index)
	{
	  setStatus("Creating persona panel");
	  create_personae_panel();
	}
      else if (index == history_index)
	{
	  setStatus("Creating history panel.");
	  create_history_panel();
	}
      else if (index == admin_history_index)
	{
	  setStatus("Creating admin history");
	  create_admin_history_panel();
	}
      else
	{
	  System.err.println("Unknown pane index: " + pane.getSelectedIndex());
	}
      
    }

  // Convienence methods

  final gclient getgclient()
    {
      return getWindowPanel().getgclient();
    }

  final windowPanel getWindowPanel()
    {
      return wp;
    }

  private final void setStatus(String status)
    {
      wp.gc.setStatus(status);
    }
}
	
