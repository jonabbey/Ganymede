/*

   framePanel.java

   The individual frames in the windowPanel.
   
   Created: 4 September 1997
   Version: $Revision: 1.32 $ %D%
   Module By: Michael Mulvaney
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.client;

import java.awt.*;
import java.io.*;
import java.awt.event.*;
import java.rmi.*;
import java.util.*;
import java.beans.PropertyVetoException;

import com.sun.java.swing.*;
import com.sun.java.swing.preview.*;  // This is for the FileChooser
import com.sun.java.swing.border.*;
import com.sun.java.swing.event.*;

import jdj.PackageResources;
import arlut.csd.ganymede.*;

import arlut.csd.JDataComponent.*;
import arlut.csd.JDialog.*;

/**
 * A framePanel is the interal window holding an object.  This class manages the
 * tabbed pane, and the creation of each of the panels in the window.
 */

public class framePanel extends JInternalFrame implements ChangeListener, Runnable, ActionListener {
  
  // This will be loaded from gclient anyway.
  boolean debug = true;

  // Indexes for the tabs in the JTabbedPane These numbers have to
  // correspond to the order they are added as tabs, so they are set
  // to current++ when one is added.  -1 means not added yet.(to the
  // pane) An index of > -1 does NOT mean the pane has been created.

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

  // We'll show a progressBar while the general panel is loading.  The
  // progressBar is contained in the progressPanel, which will be
  // removed when the general panel is finished loading.
  
  JProgressBar
    progressBar;

  JPanel
    progressPanel;

  JTabbedPane 
    pane;

  // Each of these panes is one of the tabs in the tabbedPane.  Some
  // objects don't use every scrollpane.

  JScrollPane 
    general,     // Holds a containerPanel in the ViewportView
    expiration_date,       // Holds a datePanel
    removal_date, 
    owner,       // Holds an ownerPanel
    notes,       // holds a notePanel
    admin_history, // holds an adminHistoryPanel (only for adminPersonae)
    objects_owned;  // Holds an ownershipPanel

  JPanel
    history,      // holds an historyPanel
    personae;

  Vector
    // contains all of the containerPanels.  This is used to tell the
    // containerPanels to stop loading.
    containerPanels = new Vector(), 

    // fieldTemplates
    templates, 

    // contains the Integers that have been created.  These Integers
    // are the index number of the panes.
    createdList = new Vector(); 

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

  boolean 
    editable;

  //There can be only one.
  db_object
    object;

  windowPanel
    wp;

  gclient
    gc;

  notesPanel
    my_notesPanel = null;

  // Invid of the object edited.  DO NOT access invid directly; use
  // getObjectInvid().  invid will be null until the first time
  // getObjectInvid() is called.

  private Invid
    invid = null;

  boolean isCreating;

  public framePanel(db_object object, boolean editable, windowPanel winP, String title, boolean isCreating)
    {
      this.wp = winP;
      this.object = object;
      this.editable = editable;
      this.gc = winP.gc;
      this.isCreating = isCreating;

      debug = wp.gc.debug;

      setStatus("Building window.");

      // Window properties
      setMaximizable(true);
      setResizable(true);
      setClosable(true);
      setIconifiable(true);
      setTitle(title);

      //setFrameIcon(new ImageIcon((Image)PackageResources.getImageResource(this, "folder-red.gif", getClass())));

      progressPanel = new JPanel();
      progressBar = new JProgressBar();
      progressPanel.add(new JLabel("Loading..."));
      progressPanel.add(progressBar);
      progressPanel.setForeground(Color.black);
      progressPanel.setBackground(Color.lightGray);

      setContentPane(progressPanel);

      Thread thread = new Thread(this);
      thread.start();

    }

  /**
   * Always called in the constructor, as a separate thread
   *
   * It is important to note that adding a panel is not the same as
   * creating the panel.  Adding a panel simply means adding the tab
   * to the tab pane, and is accomplished through addXXXTab().  Those
   * methods are all called here, in the run method, with the
   * exception of the expiration date and removal date tabs, which can
   * be added later if one of those dates is set.
   *
   * The createTab(int) method actually fills out the tab.  This
   * method is called when a tab is clicked on for the first time, to
   * avoid creating unessesary panels. 
   *
   * The only panel created in run() is the general panel.
   *
   */

  public void run()
    {
      if (debug)
	{
	  println("Starting thread in framePanel");
	}



      // windowPanel wants to know if framePanel is changed
      // Maybe this should be replaced with InternalFrameListener?
      addInternalFrameListener(getWindowPanel());
      
      // Now setup the framePanel layout
      pane = new JTabbedPane();
      
      // Add the panels to the tabbedPane (add just the panels that every object has.)
      general = new JScrollPane();
      pane.addTab("General", null, general);
      general_index = current++;
      owner = new JScrollPane();
      pane.addTab("Owner", null, owner);
      owner_index = current++;
      
      // Check to see if this gets an objects_owned panel
      //
      // Only OwnerBase objects get an objects_owned panel.  The
      // supergash OwnerBase does not get an objects_owned panel.
      //
      // Only user objects get a persona panel.

      short id = -1;
      try
	{
	  id = object.getTypeID();
	  if (id == SchemaConstants.OwnerBase)
	    {
	      if (getObjectInvid().equals(new Invid((short)0, 1)))
		{
		  if (debug)
		    {
		      System.out.println("framePanel:  Supergash doesn't get an ownership panel.");
		    }
		}
	      else
		{
		  objects_owned = new JScrollPane();
		  pane.addTab("Objects Owned", null, objects_owned);
		  objects_owned_index = current++;
		}
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

      templates = gc.getTemplateVector(id);

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
      //history = new JScrollPane();
      history = new JPanel(new BorderLayout());
      pane.addTab("History", null, history);
      history_index = current++;

      if (id == SchemaConstants.PersonaBase)
	{
	  admin_history = new JScrollPane();
	  pane.addTab("Admin History", null, admin_history);
	  admin_history_index = current++;
	}

      // Only add the date panels if the date has been set.  In order
      // to set the date, use the menu items.

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

      createPanel(general_index);
      showTab(general_index);
      pane.setForeground(Color.black);
      pane.setBackground(Color.lightGray);
      setContentPane(pane);


      // Need to add the menubar at the end, so the user doesn't get
      // into the menu items before the tabbed pane is all set up
      JMenuBar mb = createMenuBar(editable);
      setMenuBar(mb);

      pane.invalidate();
      validate();
    }

  /**
   * Stop loading all the container panels in this framePanel.
   *
   * This will not make the window go away, it simply tells all the
   * containerPanels inside to stop loading.
   *
   * windowPanel(the porpertyChangeListener/InternalFrameListener)
   * calls this when a frame is closed.
   * 
   */

  public void stopLoading()
  {
    if (debug)
      {
	println("Stopping all the containerPAnels.");
      }

    final Vector finalCPS = containerPanels;

    Runnable r = new Runnable() {
      public void run() {
	for (int i = 0; i < containerPanels.size(); i++)
	  {
	    if (debug)
	      {
		println("Telling a containerPanel to stop loading.");
	      }
	    
	    containerPanel cp = (containerPanel)containerPanels.elementAt(i);
	    cp.stopLoading();
	  }
      }};

    Thread t = new Thread(r);
    t.start();
  }

  /**
   */
  public boolean isEditable()
  {
    return editable;
  }

  /**
   * Return the invid of the object contained in this frame panel.
   *
   * If the invid has not been loaded, this method will load it first.
   *
   * @return The invid of the object in this frame panel.  
   */

  public Invid getObjectInvid()
  {
    if (invid == null)
      {
	try
	  {
	    invid = object.getInvid();
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Could not get object's invid");
	  }
      }
      

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

  /**
   * Refresh the tab that is showing.
   *
   * Currently, this only refreshes the general panel.  Other panels
   * will generate a nice dialog telling the user to go away.
   */

  public void refresh()
  {
    Component c = pane.getSelectedComponent();
    
    if (c instanceof JScrollPane)
      {
	Component comp = ((JScrollPane)c).getViewport().getView();
	if (comp instanceof containerPanel)
	  {
	    ((containerPanel)comp).updateAll();
	  }
	else
	  {
	    getgclient().showErrorMessage("Not implemented yet", "Sorry, you can only refresh the panel containing the general panel at this time.");
	  }
      }
    
  }
    

  /**
   * Print a nasty-looking image of the frame.
   *
   * This hardly works.
   */
  public void printObject()
  {
    PrintJob j = Toolkit.getDefaultToolkit().getPrintJob(getgclient(), "Print window", new Properties());

    if (j == null)
      {
	if (debug)
	  {
	    println("Cancelled");
	  }

	return;
      }

    Graphics page = j.getGraphics();

    int index = pane.getSelectedIndex();
    if (index < 0)
      {
	if (debug)
	  {
	    println("No pane selected?");
	  }
      }
    else
      {
	if (debug)
	  {
	    println("Printing " + index);
	  }

	// The thinking here is that some components other than the
	// scrollpane might be a little bit smarter about what they
	// should be printing.

	if (pane.getComponentAt(index) instanceof JScrollPane)
	  {
	    if (debug)
	      {
		println("Printing out the contents of the ScrollPane.");
	      }

	    JScrollPane sp = (JScrollPane)pane.getComponentAt(index);
	    sp.getViewport().getView().print(page);
	  }
	else if (debug)
	  {
	    println("The selected index is not a scrollpane.");
	    showErrorMessage("The current selection cannot be printed.");
	  }
      }

    page.dispose();
    j.end();

  }

  
  public void sendMail()
  {
    SaveDialog dialog = new SaveDialog(gc, true);
    boolean showHistory = false;
    Date startDate = null;
    String address;
    StringBuffer body;
    //Vector choices = new Vector();

    /* -- */

    //choices.addElement("Html");
    //choices.addElement("Plain text");
    //choices.addElement("Tab separated");
    //dialog.setFormatChoices(choices);

    if (!dialog.showDialog())
      {
	if (debug)
	  {
	    System.out.println("Dialog returned false, returning in sendMail()");
	  }

	return;
      }

    System.out.println("Format: " + dialog.getFormat());

    showHistory = dialog.isShowHistory();
    if (showHistory)
      {
	startDate = dialog.getStartDate();
      }

    address = dialog.getRecipients();
    if ((address == null) || (address.equals("")))
      {
	gc.showErrorMessage("You must specify at least one recipient.");
	return;
      }
    
    body = encodeObjectToStringBuffer(showHistory, startDate);

    if (debug)
      {
	System.out.println("Mailing: \nTo: " + address + "\n\n" + body.toString());
      }

    try
      {
	gc.getSession().sendMail(address, "Ganymede results", body);
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("sending mail: " + rx);
      }
  }

  public void save()
  {
    SaveDialog dialog = new SaveDialog(gc, false);
    JFileChooser chooser = new JFileChooser();
    int returnValue;
    Date startDate = null;
    boolean showHistory = false;
    File file;

    FileOutputStream fos = null;
    PrintWriter writer = null;

    /* -- */

    if (!dialog.showDialog())
      {
	if (debug)
	  {
	    System.out.println("dialog returned false, returning");
	  }

	return;
      }

    gc.setWaitCursor();

    showHistory = dialog.isShowHistory();
    if (showHistory)
      {
	startDate = dialog.getStartDate();
      }

    chooser.setDialogType(JFileChooser.SAVE_DIALOG);
    chooser.setDialogTitle("Save window as");

    returnValue = chooser.showDialog(gc, null);

    if (!(returnValue == JFileChooser.APPROVE_OPTION))
      {
	return;
      }

    file = chooser.getSelectedFile();
    
    if (file.exists())
      {
	StringDialog d = new StringDialog(gc, "Warning", file.getName() + " exists.  Are you sure you want to replace this file?",
					  "Overwrite", "Cancel", null);
	Hashtable result = d.DialogShow();

	if (result == null)
	  {
	    if (debug)
	      {
		System.out.println("The file exists, and I am backing out.");
	      }

	    return;
	  }
      }

    try
      {
	fos = new FileOutputStream(file);
        writer = new PrintWriter(fos);
      }
    catch (java.io.IOException e)
      {
	gc.showErrorMessage("Trouble saving", "Could not open the file.");
	return;
      }

    writer.println(encodeObjectToStringBuffer(showHistory, startDate).toString());
    writer.close();

    gc.setNormalCursor();
  }

  private StringBuffer encodeObjectToStringBuffer(boolean showHistory, Date startDate)
  {
    StringBuffer buffer = new StringBuffer();
    FieldTemplate template;
    db_field field;

    // Loop through all the fields, and get their values
    try
      {
	for (int i = 0; i < templates.size();  ++i)
	  {
	    template = (FieldTemplate)templates.elementAt(i);
	    field = object.getField(template.getID());

	    if(!(field == null))
	      {
		buffer.append(template.getName() + "\t" + field.getValueString() + System.getProperty("line.separator")); 
	      }
	    else
	      {
		buffer.append(template.getName() + "\tUndefined" + System.getProperty("line.separator"));
		if (debug)
		  {
		    System.out.println("Field is null: " + template.getName());
		  }
	      }
	  }
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not get field info for encoding: " + rx);
      }

    if (showHistory)
      {
	try
	  {
	    buffer.append("\nHistory:\n\n" + gc.getSession().viewObjectHistory(getObjectInvid(), startDate).toString());
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("RemoteException getting history: " + rx);
	  }
      }


    return buffer;
  }

  public Image getWaitImage()
  {
    return wp.getWaitImage();
  }

  /* Private stuff */
  JMenuBar createMenuBar(boolean editable)
  {
    // Took out the "Edit" menu, that's what all the commented out
    // stuff is.


    // Adding a menu bar, checking it out
    JMenuBar menuBar = new JMenuBar();
    menuBar.setBorderPainted(true);
    //menuBar.setBackground(ClientColor.WindowBG.darker());
    
    JMenu fileM = new JMenu("File");
    //JMenu editM = new JMenu("Edit");
    menuBar.add(fileM);
    //menuBar.add(editM);
    
    /*
    JMenuItem iconifyMI = new JMenuItem("Iconify");
    iconifyMI.addActionListener(this);

    JMenuItem closeMI = null;
    closeMI = new JMenuItem("Close");
    closeMI.addActionListener(this);
    */

    JMenu deleteM = new JMenu("Delete");
    JMenuItem reallyDeleteMI = new JMenuItem("Yes, I'm sure");
    deleteM.add(reallyDeleteMI);
    reallyDeleteMI.setActionCommand("ReallyDelete");
    reallyDeleteMI.addActionListener(this);
      
    JMenuItem inactivateMI = new JMenuItem("Inactivate");
    inactivateMI.addActionListener(this);

    JMenuItem refreshMI = new JMenuItem("Refresh");
    refreshMI.addActionListener(this);

    JMenuItem setExpirationMI = null;
    JMenuItem setRemovalMI = null;
    if (editable)
      {
	setExpirationMI = new JMenuItem("Set Expiration Date");
	setExpirationMI.addActionListener(this);
	
        setRemovalMI = new JMenuItem("Set Removal Date");
	setRemovalMI.addActionListener(this);
      }

    JMenuItem printMI = new JMenuItem("Print");
    printMI.addActionListener(this);

    JMenuItem saveMI = null;
    if (!gc.isApplet())
      {
	saveMI = new JMenuItem("Save");
	saveMI.addActionListener(this);
      }

    JMenuItem mailMI = new JMenuItem("Mail to...");
    mailMI.addActionListener(this);
    

    fileM.add(inactivateMI);
    fileM.add(deleteM);
    fileM.addSeparator();
    fileM.add(printMI);

    if (!gc.isApplet())
      {
	fileM.add(saveMI);
      }
    fileM.add(mailMI);

    fileM.add(refreshMI);
    if (editable)
      {
	fileM.addSeparator();
	fileM.add(setExpirationMI);
	fileM.add(setRemovalMI);
      }

    fileM.addSeparator();
    //fileM.add(iconifyMI);
    //fileM.add(closeMI);

    if (!editable)
      {
	JMenuItem editMI = new JMenuItem("Edit");
	editMI.addActionListener(this);
	
	fileM.add(editMI);
	
      }

    if (debug)
      {
	println("Returning menubar.");
      }
    
    return menuBar;
  }



  //This need to be changed to show the progress bar
  void create_general_panel()
    {
      if (debug)
	{
	  println("Creating general panel");
	}
      
      containerPanel cp = new containerPanel(object, editable, wp.gc, wp, this, progressBar, false, isCreating);
      containerPanels.addElement(cp);
      cp.load();
      cp.setBorder(wp.emptyBorder10);

      general.getVerticalScrollBar().setUnitIncrement(15);
      general.setViewportView(cp);
      //general.setViewportView(progressBar);
      createdList.addElement(new Integer(general_index));
      setStatus("Done");
      
    }

  void create_expiration_date_panel()
    {
      if (debug)
	{
	  println("Creating date panel");
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

      expiration_date.getVerticalScrollBar().setUnitIncrement(15);
      expiration_date.setViewportView(new datePanel(exp_field, "Expiration date", editable, this));
	  
      createdList.addElement(new Integer(expiration_date_index));

      setStatus("Done");
      
    }

  void create_removal_date_panel()
    {
      if (debug)
	{
	  println("Creating removal date panel");
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

      removal_date.getVerticalScrollBar().setUnitIncrement(15);
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
	  println("Creating owner panel");
	}

      try
	{
	  owner.getVerticalScrollBar().setUnitIncrement(15);
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

      //history.getVerticalScrollBar().setUnitIncrement(15);
      //history.setViewportView(new historyPanel(getObjectInvid(), 
      history.add("Center", new historyPanel(getObjectInvid(), 
					       getgclient(), 
					       creator_field, 
					       creation_date_field, 
					       modifier_field,
					       modification_date_field));
	
      createdList.addElement(new Integer(history_index));
      
      history.invalidate();
      validate();

      setStatus("Done");
    }


  void create_admin_history_panel()
    {
      setStatus("Creating admin history panel");
      admin_history.getVerticalScrollBar().setUnitIncrement(15);
      admin_history.setViewportView(new adminHistoryPanel(getObjectInvid(), getgclient()));
	
      createdList.addElement(new Integer(admin_history_index));
      
      admin_history.invalidate();
      validate();

      setStatus("Done");
    }

  void create_notes_panel()
    {
      if (debug)
	{
	  println("Creating notes panel");
	}

      my_notesPanel = new notesPanel(notes_field, 
				     editable, this);

      notes.getVerticalScrollBar().setUnitIncrement(15);
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
	  println("Creating ownership panel");
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
	  objects_owned.getVerticalScrollBar().setUnitIncrement(15);
	  objects_owned.setViewportView(null_oo);
	}
      else
	{
	  objects_owned.getVerticalScrollBar().setUnitIncrement(15);
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
	  println("Creating personae panel()");
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
	      println("Adding date tabs");
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
	      println("Adding removal date tabs");
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
	  println("Adding notes tab");
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
		      println("Setting notes test to *" + notesText + "*.");
		    }

		  ImageIcon noteIcon = new ImageIcon((Image)PackageResources.getImageResource(this, "note02.gif", getClass()));
		  
		  pane.setIconAt(notes_index, noteIcon);
		}
	      else if (debug)
		{
		  println("Empty notes");
		}
	    }
	  else if (debug)
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
	println("This tab has not been added to the pane yet.");
      }
    pane.setSelectedIndex(index);
  }

  public void actionPerformed(ActionEvent e)
  {
    if (debug)
      {
	println("Menu item action: " + e.getActionCommand());
      }
    
    if (e.getActionCommand().equals("Edit"))
      {
	if (debug)
	  {
	    println("edit button clicked");
	  }

	gc.editObject(getObjectInvid());
      }
    else if (e.getActionCommand().equals("Save"))
      {
	if (debug)
	  {
	    println("Saving...:");
	  }

	save();
      }
    else if (e.getActionCommand().equals("Mail to..."))
      {
	sendMail();
      }

    // There is no clone.  If there ever is, this would be useful.  But there isn't
    else if (e.getActionCommand().equals("Clone"))
      {

	if (debug)
	  {
	    println("Opening new edit window on the cloned object");
	  }
	showErrorMessage("Sorry", "Clone is not implemented");
      }
    else if (e.getActionCommand().equals("ReallyDelete"))
      {
	if (debug)
	  {
	    println("Deleting object");
	  }

	if (gc.deleteObject(getObjectInvid()))
	  {
	    try
	      {
		setClosed(true);
		
	      }
	    catch (PropertyVetoException ex)
	      {
		throw new RuntimeException("JInternalFrame will not close: " + ex);
	      }
	  }
	else
	  {
	    showErrorMessage("Error", "Could not delete object.");
	  }
      }
    else if (e.getActionCommand().equals("Close"))
      {
	try
	  {
	    setClosed(true);
	    
	  }
	catch (PropertyVetoException ex)
	  {
	    throw new RuntimeException("JInternalFrame will not close: " + ex);
	  }
      }
    else if (e.getActionCommand().equals("Iconify"))
      {
	try
	  {
	    setIcon(true);
	  }
	catch (PropertyVetoException ex)
	  {
	    throw new RuntimeException("JInternalFrame will not change icon: " + ex);
	  }
      }
    else if (e.getActionCommand().equals("Print"))
      {
	printObject();
      }
    else if (e.getActionCommand().equals("Refresh"))
      {
	refresh();
      }
    else if (e.getActionCommand().equals("Inactivate"))
      {
	boolean success = gc.inactivateObject(getObjectInvid());
	
	if (success)
	  {
	    try
	      {
		setClosed(true);
		
	      }
	    catch (PropertyVetoException ex)
	      {
		throw new RuntimeException("JInternalFrame will not close: " + ex);
	      }		
	  }
	else
	  {
	    showErrorMessage("Could not inactivate object.");
	  }

      }
    else if (e.getActionCommand().equals("Set Expiration Date"))
      {
	addExpirationDatePanel();
	showTab(expiration_date_index);
      }
    else if (e.getActionCommand().equals("Set Removal Date"))
      {
	addRemovalDatePanel();
	showTab(removal_date_index);
      }
    else
      {
	System.err.println("Unknow action event: " + e);
      }

  }

  // For the ChangeListener
  public void stateChanged(ChangeEvent e)
    {
      int index = pane.getSelectedIndex();

      if (!createdList.contains(new Integer(index)))
	{
	  createPanel(index);
	}
    }

  /**
   * This checks to see if the panel is created, and creates it if needed.
   */

  public void createPanel(int index)
    {
      wp.gc.setWaitCursor();
      if (debug)
	{
	  println("index = " + index + " general_index= " + general_index);
	}
	
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

      wp.gc.setNormalCursor();
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

  /* 
   * Use this instead of System.out.println, in case we want to direct
   * that stuff somewhere else sometime.  Plus, it is easier to type.  
   */
  
  private void println(String s)
  {
    System.out.println(s);
  }

  /*
   * Give the gclient an error message.
   */
  private void showErrorMessage(String message)
  {
    showErrorMessage("Error", message);
  }

  private void showErrorMessage(String title, String message)
  {
    gc.showErrorMessage(title, message);
  }

}
