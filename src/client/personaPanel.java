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


public class personaPanel extends JPanel implements ActionListener{
  
  final static boolean debug = true;

  framePanel
    parent;

  invid_field
    field;

  boolean
    editable;
  
  JButton
    next,
    previous,
    add,
    delete;

  JScrollPane
    middle;

  Vector
    panels,
    personas = null;

  int 
    total,
    current = -1;

  JLabel
    XofX;

  public personaPanel(invid_field field, boolean editable, framePanel parent) 
  {
    this.field = field;
    this.editable = editable;
    this.parent = parent;

    setLayout(new BorderLayout());

    // Create the button panel for the bottom
    JPanel bottom = new JPanel(false);
    bottom.setLayout(new BoxLayout(bottom, BoxLayout.X_AXIS));

    add = new JButton("Create");
    add.setMaximumSize(add.getPreferredSize());
    add.addActionListener(this);
    delete = new JButton("Delete");
    delete.setMaximumSize(delete.getPreferredSize());
    delete.addActionListener(this);

    bottom.add(Box.createHorizontalGlue());
    bottom.add(add);
    bottom.add(delete);
    bottom.add(Box.createHorizontalGlue());

    // Create button panel for top (next, previous)
    JPanel top = new JPanel(false);
    top.setLayout(new BoxLayout(top, BoxLayout.X_AXIS));
    
    next = new JButton("Next");
    next.setMaximumSize(next.getPreferredSize());
    next.addActionListener(this);
    previous = new JButton("Previous");
    previous.setMaximumSize(previous.getPreferredSize());
    previous.addActionListener(this);

    XofX = new JLabel();

    top.add(previous);
    top.add(Box.createHorizontalGlue());
    top.add(XofX);
    top.add(Box.createHorizontalGlue());
    top.add(next);

    add("North", top);
    add("South", bottom);

    // Create the middle, content pane
    middle = new JScrollPane();
    add("Center", middle);

    try
      {
	personas = field.getValues();
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not get values for persona field: " + rx);
      }

    panels = new Vector();

    total = personas.size();

    for (int i = 0; i< total; i++)
      {
	personaContainer pc = new personaContainer((Invid)personas.elementAt(i), editable, this);
	Thread t = new Thread(pc);
	t.start();

	panels.addElement(pc);
	middle.setViewportView(pc);
      }
    
    XofX.setText("1 of " + panels.size());
    next.setEnabled(personas.size() > 1);
    previous.setEnabled(personas.size() > 1);

  }

  public void actionPerformed(ActionEvent e)
  {
    if (debug)
      {
	System.out.println(e.getActionCommand());
      }

    if (e.getActionCommand().equals("Create"))
      {
	personaContainer pc = new personaContainer(null, editable, this, true);
	Thread t = new Thread(pc);
	t.start();

	panels.addElement(pc);
	middle.setViewportView(pc);

	current = panels.lastIndexOf(pc);
	pc.waitForLoad();

	if (debug)
	  {
	    System.out.println("Showing: " + current);
	  }

	middle.setViewportView((JPanel)panels.elementAt(current));

	if (panels.size() > 1)
	  {
	    if (debug)
	      {
		System.out.println("Enabling buttons");
	      }
	    next.setEnabled(true);
	    previous.setEnabled(true);
	  }
	else
	  {
	    if (debug)
	      {
		System.out.println("Disabling buttons");
	      } 
	    next.setEnabled(false);
	    previous.setEnabled(false);
	  }
      }
    else if (e.getActionCommand().equals("Delete"))
      {
	Invid invid = ((personaContainer)panels.elementAt(current)).getInvid();
	try
	  {
	    Invid user = parent.object.getInvid();
	    
	    parent.object.getField(SchemaConstants.UserAdminPersonae).deleteElement(invid);
	    parent.getgclient().getSession().remove_db_object(invid);

	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Could not delete this persona: " + rx);
	  }
	panels.removeElementAt(current);
	updateLabel();

	middle.setViewportView(new JLabel());
	middle.invalidate();
	validate();

      }
    else if (e.getActionCommand().equals("Next"))
      {
	current++;
	if (current >= panels.size())
	  {
	    if (debug)
	      {
		System.out.println("setting current to 0");
	      }
	    current = 0;
	  }

	((personaContainer)panels.elementAt(current)).waitForLoad();

	if (debug)
	  {
	    System.out.println("Showing: " + current);
	  }

	middle.setViewportView((JPanel)panels.elementAt(current));
	updateLabel();

	middle.getViewport().invalidate();
	middle.invalidate();
	validate();
      }
    else if (e.getActionCommand().equals("Previous"))
      {
	current--;
	if (current < 0)
	  {
	    if (debug)
	      {
		System.out.println("setting current to personas.size");
	      }
	    current = personas.size() - 1;
	  }

	((personaContainer)panels.elementAt(current)).waitForLoad();

	if (debug)
	  {
	    System.out.println("Showing: " + current);
	  }

	middle.setViewportView((JPanel)panels.elementAt(current));
	updateLabel();

	middle.invalidate();
	validate();
      }

  }

  public void updateLabel()
  {
    XofX.setText((current + 1) + " of " + panels.size());
  }

} 

class personaContainer extends JPanel implements Runnable{

  private final static boolean debug = true;
  
  boolean loaded = false;

  Invid
    invid;

  personaPanel
    parent;

  boolean
    createNew,
    editable;



  public personaContainer(Invid invid, boolean editable,personaPanel parent)
  {
    this(invid, editable, parent, false);
  }

  public personaContainer(Invid invid, boolean editable, personaPanel parent, boolean createNew)
  {
    this.invid = invid;
    this.parent = parent;
    this.editable = editable;
    this.createNew = createNew;

    setLayout(new BorderLayout());
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
	    // First set up the back linking
	    db_object newObject = parent.parent.getgclient().getSession().create_db_object(SchemaConstants.PersonaBase);
	    Invid user = parent.parent.object.getInvid();
	    
	    parent.parent.object.getField(SchemaConstants.UserAdminPersonae).addElement(newObject.getInvid());
	    newObject.getField(SchemaConstants.PersonaAssocUser).setValue(user);
	    // Then add that puppy
	    add("Center", new containerPanel(newObject,
					     editable,
					     parent.parent.getgclient(), parent.parent.getWindowPanel(), parent.parent));
	  }
	else if (editable)
	  {
	    add("Center", new containerPanel(parent.parent.getgclient().getSession().edit_db_object(invid), 
					     editable,
					     parent.parent.getgclient(), parent.parent.getWindowPanel(), parent.parent));
	  }
	else
	  {
	    add("Center", new containerPanel(parent.parent.getgclient().getSession().view_db_object(invid), 
					     editable,
					     parent.parent.getgclient(), parent.parent.getWindowPanel(), parent.parent));
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

    parent.updateLabel();

    invalidate();
    parent.invalidate();
    parent.parent.validate();

    this.notifyAll();
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
