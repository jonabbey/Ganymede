/*

   personaPanel.java

   a panel for handling User's personae.
   
   Created: 6 October 1997
   Version: $Revision: 1.18 $ %D%
   Module By: Mike Mulvaney
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.client;

import java.awt.*;
import java.awt.event.*;
import java.rmi.*;
import java.util.*;

import arlut.csd.ganymede.*; 
import arlut.csd.JDialog.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                    personaPanel

------------------------------------------------------------------------------*/

public class personaPanel extends JPanel implements ActionListener, ChangeListener{
  
  boolean debug = false;

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

  boolean
    fieldIsEditable = false;

  /* -- */

  public personaPanel(invid_field field, boolean editable, framePanel fp) 
  {
    this.field = field;
    this.editable = editable;
    this.fp = fp;

    gc = fp.wp.gc;
    debug = gc.debug;

    setLayout(new BorderLayout());
    
    try
      {
	fieldIsEditable = field.isEditable();
      }
    catch(RemoteException rx)
      {
	throw new RuntimeException("Could not call field.isEditable in personaPanel: " + rx);
      }

    if (editable && fieldIsEditable)
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
    middle = new JTabbedPane(JTabbedPane.TOP);

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
	boolean thisOneEditable = false;

	try
	  {
	    thisOneEditable = editable && field.isEditable();

	    Invid thisInvid = (Invid)personas.elementAt(i);

	    if (thisOneEditable)
	      {
		ReturnVal rv = gc.handleReturnVal(gc.getSession().edit_db_object(thisInvid));
		db_object ob = rv.getObject();

		if (ob == null)
		  {
		    if (debug)
		      {
			System.out.println("Whoa, got a null object(edit), trying to go to non-editable, cover me.");
		      }
		    
		    ReturnVal Vrv = gc.handleReturnVal(gc.getSession().view_db_object(thisInvid));
		    ob = Vrv.getObject();
		    
		    if (ob == null)
		      {
			System.out.println("That didn't work...its still not giving me anything back.  Giving up.");
		      }
		    else
		      {
			pc = new personaContainer(thisInvid, i, false, this, ob); //Now I know it is not editable
		      }
		  }
		else
		  {
		    pc = new personaContainer(thisInvid, i, thisOneEditable, this, ob);
		  }
	      }
	    else
	      {
		ReturnVal rv = gc.handleReturnVal(gc.getSession().view_db_object(thisInvid));
		db_object ob = rv.getObject();

		if (ob == null)
		  {
		    System.out.println("Whoa, got a null object(view), skipping.");
		  }
		else
		  {
		    pc = new personaContainer(thisInvid, i, thisOneEditable, this, ob);
		  }
	      }		
      	  }
	catch (RemoteException rx)
	  {
	    if (debug)
	      {
		gc.showErrorMessage("Could not check if the field is editable: " + rx);
	      }
	  }
	
	panels.put(new Integer(i), pc);
	middle.addTab("Persona " + i, pc);

	Thread t = new Thread(pc);
	t.start();
      }

    // Show the first one(will just be a progress bar for now)

    middle.addChangeListener(this);

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
	gc.setWaitCursor();
	int index = middle.getTabCount();
	// Make sure the default owner is chosen
 	    
	try
	  {
	    if (!fp.getgclient().defaultOwnerChosen())
	      {
		fp.getgclient().chooseDefaultOwner(false);
	      }
	    
	    // Create the object
	    ReturnVal rv = fp.getgclient().handleReturnVal(fp.getgclient().getSession().create_db_object(SchemaConstants.PersonaBase));
	    db_object newObject = rv.getObject();
	    Invid user = fp.getObjectInvid();

	    gc.somethingChanged();
	    
	    // Tell the user about the persona

	    fp.object.getField(SchemaConstants.UserAdminPersonae).addElement(newObject.getInvid());

	    // Tell the persona about the user

	    newObject.getField(SchemaConstants.PersonaAssocUser).setValue(user);
	    
	    personaContainer pc = new personaContainer(newObject.getInvid(), index, editable, this, newObject);
	    middle.addTab("New Persona " + index, pc);

	    pc.run();
	    
	    panels.put(new Integer(index), pc);
	    //Thread t = new Thread(pc);
	    //t.start();

	    pc.waitForLoad();
	    
	    if (debug)
	      {
		System.out.println("Showing: " + index);
	      }
	    
	    middle.setSelectedIndex(index);
	  }
	catch (NullPointerException ne)
	  {
	    gc.showErrorMessage("You don't have permission to create objects of this type.");
	    add.setEnabled(false);
	    gc.setNormalCursor();
	    return;
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Could not create new persona: " + rx);
	  }

	gc.setNormalCursor();
      }
    else if (e.getActionCommand().equals("Delete"))
      {
	gc.setWaitCursor();
	boolean removed = false;
	boolean deleted = false;

	personaContainer pc = (personaContainer)panels.get(new Integer(middle.getSelectedIndex()));

	Invid invid = pc.getInvid();

	if (invid == null) 
	  {
	    throw new NullPointerException("invid is null");
	  }

	StringDialog d = new StringDialog(gc, 
					  "Confirm deletion",
					  "Are you sure you want to delete persona " + 
					  middle.getTitleAt(middle.getSelectedIndex()) + "?",
					  true);

	gc.setNormalCursor();

	if (d.DialogShow() == null)
	  {
	    if (debug)
	      {
		System.out.println("Cancelled.");
	      }

	    return;
	  }

	gc.somethingChanged();

	if (debug)
	  {
	    System.out.println("invid to delete: " + invid);
	  }
		
	gc.setWaitCursor();

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
		gc.setStatus("Deleted the object ok");
	      }
	    else
	      {
	       gc.setStatus("Could not delete the object.");
	      }
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Could not delete this persona: " + rx);
	  }

	if (deleted && removed)
	  {
	    if (debug)
	      {
		System.out.println("Selected number: " + middle.getSelectedIndex());
		System.out.println("Deleting number: " + pc.index);
	      }

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
	
	gc.setNormalCursor();
      }
  }

  public void stateChanged(ChangeEvent e)
  {
    personaContainer pc = (personaContainer)middle.getSelectedComponent();

    if (delete != null)
      {
	delete.setEnabled(pc.isEditable());
      }
  }
} 

/*------------------------------------------------------------------------------
                                                                           class
                                                                personaContainer

------------------------------------------------------------------------------*/

/**
 * Make sure you add this tab to a JTabbedPane BEFORE you call run()!
 */

class personaContainer extends JScrollPane implements Runnable{

  private final static boolean debug = false;
  
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

  db_object object;

  /* -- */

  public personaContainer(Invid invid, int index, boolean editable, personaPanel pp, db_object object)
  {
    if (object == null)
      {
	throw new IllegalArgumentException("Got a null object in personaContainer.");
      }

    this.invid = invid;
    this.object = object;
    this.index = index;
    this.pp = pp;
    gc = pp.gc;
    this.editable = editable;
    this.createNew = createNew;

    progressBar = new JProgressBar();
    progressPane = new JPanel();
    progressPane.add(new JLabel("Loading..."));
    progressPane.add(progressBar);
    getVerticalScrollBar().setUnitIncrement(15);
    setViewportView(progressPane);
  }

  public boolean isEditable()
  {
    return editable;
  }

  public synchronized void run()
  {
    if (debug)
      {
	System.out.println("Starting new thread");
      }

    try
      {
	String label = object.getLabel();

	if ((label != null) && (!label.equals("null")))
	  {
	    pp.middle.setTitleAt(index, label);
	    pp.middle.repaint();
	  }
	
	containerPanel cp = new containerPanel(object,
					       editable,
					       pp.fp.getgclient(), 
					       pp.fp.getWindowPanel(), 
					       pp.fp,
					       progressBar,
					       this);
	cp.setBorder(pp.empty);
	setViewportView(cp);
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

    pp.invalidate();
    pp.fp.validate();

  }

  public synchronized void waitForLoad()
  {
    long startTime = System.currentTimeMillis();

    while (!loaded)
      {
	try
	  {
	    if (debug)
	      {
		System.out.println("presona panel waiting for load!");
	      }

	    this.wait(1000);

	    if (System.currentTimeMillis() - startTime > 200000)
	      {
		System.out.println("Something went wrong loading the persona panel. " +
				   " The wait for load thread was taking too long, so I gave up on it.");
		break;
	      }
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
