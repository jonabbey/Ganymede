/*
 personaPanel.java

 a panel for handling User's personae.
 
 Module by: Mike Mulvaney
 Created: 10/6/97

 */

package arlut.csd.ganymede.client;

import java.awt.*;
import java.awt.event.*;
import java.rmi.*;
import java.util.*;

import arlut.csd.ganymede.*; 
import com.sun.java.swing.*;
import com.sun.java.swing.border.*;

public class personaPanel extends JPanel implements ActionListener{
  
  final static boolean debug = true;

  framePanel
    fp;

  gclient
    gc;

  invid_field
    field;

  boolean
    editable;
  
  JButton
    add,
    delete;

  JTabbedPane
    middle;

  Vector
    personas = null;

  int 
    total,
    current = -1;

  Hashtable
    panels = new Hashtable();

  EmptyBorder
    empty = new EmptyBorder(new Insets(7,7,7,7));

  /* -- */

  public personaPanel(invid_field field, boolean editable, framePanel fp) 
  {
    this.field = field;
    this.editable = editable;
    this.fp = fp;

    gc = fp.wp.gc;

    setLayout(new BorderLayout());

    if (editable)
      {
	// Create the button panel for the bottom
	JPanel bottom = new JPanel(false);

	add = new JButton("Create");
	add.addActionListener(this);
	delete = new JButton("Delete");
	delete.addActionListener(this);

	bottom.add(add);
	bottom.add(delete);
	
	add("South", bottom);
      }

    // Create the middle, content pane
    middle = new JTabbedPane(JTabbedPane.BOTTOM);
    JPanel middleP = new JPanel(new BorderLayout());
    middleP.setBorder(new TitledBorder("Personas"));
    middleP.add("Center", middle);

    add("Center", middleP);

    try
      {
	personas = field.getValues();
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not get values for persona field: " + rx);
      }

    total = personas.size();

    for (int i = 0; i< total; i++)
      {
	personaContainer pc = null;
	try
	  {
	    pc = new personaContainer((Invid)personas.elementAt(i), i, editable && field.isEditable(), this);
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Could not check if the field is editable: " + rx);
	  }

	panels.put(new Integer(i), pc);
	Thread t = new Thread(pc);
	t.start();

	middle.addTab("Persona " + i, pc);
      }

    // Show the first one(will just be a progress bar for now)

    if (total > 0)
      {
	middle.setSelectedIndex(0);
      }
  }

  public void actionPerformed(ActionEvent e)
  {
    ReturnVal retVal;

    /* -- */

    if (debug)
      {
	System.out.println(e.getActionCommand());
      }

    if (e.getActionCommand().equals("Create"))
      {
	int index = middle.getTabCount();
	personaContainer pc = new personaContainer(null, index, editable, this, true);

	panels.put(new Integer(index), pc);
	Thread t = new Thread(pc);
	t.start();

	middle.addTab("New Persona " + index, pc);

	pc.waitForLoad();

	if (debug)
	  {
	    System.out.println("Showing: " + index);
	  }

	middle.setSelectedIndex(index);

      }
    else if (e.getActionCommand().equals("Delete"))
      {
	boolean removed = false;
	boolean deleted = false;

	personaContainer pc = (personaContainer)panels.get(new Integer(middle.getSelectedIndex()));
	Invid invid = pc.getInvid();

	if (debug)
	  {
	    System.out.println("invid to delete: " + invid);
	  }

	try
	  {
	    Invid user = fp.getObjectInvid();

	    retVal = fp.object.getField(SchemaConstants.UserAdminPersonae).deleteElement(invid);

	    removed = (retVal == null) ? true : retVal.didSucceed();

	    if (retVal != null)
	      {
		gc.handleReturnVal(retVal);
	      }

	    if (removed)
	      {
		if (debug)
		  {
		    System.out.println("removed the element from the field ok");
		  }

		retVal = fp.getgclient().getSession().remove_db_object(invid);

		deleted = (retVal == null) ? true : retVal.didSucceed();
		
		if (retVal != null)
		  {
		    gc.handleReturnVal(retVal);
		  }
	      }
	    else
	      {
		if (debug)
		  {
		    System.out.println("could not remove the element from the field");
		  }
	      }

	    if (deleted)
	      {
		System.out.println("Deleted the object ok");
	      }
	    else
	      {
		System.out.println("Could not delete the object.");
	      }
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Could not delete this persona: " + rx);
	  }

	if (deleted && removed)
	  {
	    System.out.println("Selected number: " + middle.getSelectedIndex());
	    System.out.println("Deleting number: " + pc.index);
	    middle.removeTabAt(pc.index);
	    //middle.invalidate();
	    //validate();
	  }
	else
	  {
	    if (debug)
	      {
		System.out.println("Could not fully remove the object.");
	      }
	  }
      }
  }
} 

/*------------------------------------------------------------------------------
                                                                           class
                                                                personaContainer

------------------------------------------------------------------------------*/

class personaContainer extends JScrollPane implements Runnable{

  private final static boolean debug = true;
  
  boolean loaded = false;

  Invid
    invid;

  personaPanel
    pp;

  gclient
    gc;

  boolean
    createNew,
    editable;

  int
    index;

  JProgressBar
    progressBar;

  JPanel
    progressPane;

  /* -- */

  public personaContainer(Invid invid, int index, boolean editable,personaPanel pp)
  {
    this(invid, index, editable, pp, false);
  }

  public personaContainer(Invid invid, int index, boolean editable, personaPanel pp, boolean createNew)
  {
    this.invid = invid;
    this.index = index;
    this.pp = pp;
    gc = pp.gc;
    this.editable = editable;
    this.createNew = createNew;

    progressBar = new JProgressBar();
    progressPane = new JPanel();
    progressPane.add(new JLabel("Loading..."));
    progressPane.add(progressBar);
    setViewportView(progressPane);
  }

  public synchronized void run()
  {
    if (debug)
      {
	System.out.println("Starting new thread");
      }

    try
      {
	if (createNew)
	  {
	    // Make sure the default owner is chosen
	    
	    if (!pp.fp.getgclient().defaultOwnerChosen())
	      {
		pp.fp.getgclient().chooseDefaultOwner(false);
	      }
	    
	    // First set up the back linking
	    db_object newObject = pp.fp.getgclient().getSession().create_db_object(SchemaConstants.PersonaBase);
	    Invid user = pp.fp.getObjectInvid();
	    
	    pp.fp.object.getField(SchemaConstants.UserAdminPersonae).addElement(newObject.getInvid());
	    newObject.getField(SchemaConstants.PersonaAssocUser).setValue(user);
	    
	    pp.middle.setTitleAt(index, "New persona");
	    // Then add that puppy
	    containerPanel cp = new containerPanel(newObject,
						   editable,
						   pp.fp.getgclient(), pp.fp.getWindowPanel(), 
						   pp.fp, progressBar);
	    cp.setBorder(pp.empty);
	    setViewportView(cp);
	  }
	else if (editable)
	  {
	    boolean thisPanelEditable = editable;
	    db_object object = pp.fp.getgclient().getSession().edit_db_object(invid);
	    if (object == null)
	      {
		object = pp.fp.getgclient().getSession().view_db_object(invid);
		thisPanelEditable = false;
	      }

	    pp.middle.setTitleAt(index, object.getLabel());
	    containerPanel cp = new containerPanel(object,
						   thisPanelEditable,
						   pp.fp.getgclient(), 
						   pp.fp.getWindowPanel(), 
						   pp.fp,
						   progressBar);
	    cp.setBorder(pp.empty);
	    setViewportView(cp);
	  }
	else
	  {
	    db_object object = pp.fp.getgclient().getSession().view_db_object(invid);
	    pp.middle.setTitleAt(index, object.getLabel());
	    containerPanel cp = new containerPanel(object,
						   editable,
						   pp.fp.getgclient(), 
						   pp.fp.getWindowPanel(), 
						   pp.fp,
						   progressBar);
	    cp.setBorder(pp.empty);
	    setViewportView(cp);
	  }
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not load persona into container panel: " + rx);
      }

    loaded = true;

    if (debug)
      {
	System.out.println("Done with thread in personaPanel");
      }

    this.invalidate();
    pp.validate();

  }

  public synchronized void waitForLoad()
  {
    while (! loaded)
      {
	try
	  {
	    this.wait();
	  }
	catch (InterruptedException e)
	  {
	    throw new RuntimeException("Interrupted while waiting for personaContainer to load: " + e);
	  }
      }
  }

  public Invid getInvid()
  {
    return invid;
  }
}
