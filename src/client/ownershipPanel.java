/*

   ownerPanel.java

   The individual frames in the windowPanel.
   
   Created: 9 September 1997
   Version: 1.4 97/09/30
   Module By: Michael Mulvaney
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.client;

import java.awt.*;
import java.awt.event.*;
import java.rmi.*;
import java.util.*;

import com.sun.java.swing.*;
import arlut.csd.ganymede.*;

import arlut.csd.JDataComponent.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                  ownershipPanel

------------------------------------------------------------------------------*/

public class ownershipPanel extends JPanel implements ItemListener {

  private final static boolean debug = true;

  invid_field
    field;

  boolean
    editable;
  
  framePanel
    parent;

  gclient
    gc;

  JPanel
    center;

  JComboBox
    bases;

  Hashtable
    objects_owned,   // (Short)Base type -> (Vector)list of objects [all objects]
    paneHash;        // (String) base name -> objectPane holding base objects

  CardLayout
    cards;

  Vector
    owners = null,
    result = null;

  JPanel
    holder;

  QueryDataNode
    node;

  public ownershipPanel(invid_field field, boolean editable, framePanel parent)
  {
    this.field = field;
    this.editable = editable;
    this.parent = parent;

    gc = parent.wp.gc;

    setLayout(new BorderLayout());

    holder = new JPanel();
    holder.add(new JLabel("Loading ownershipPanel."));
    add("Center", holder);

    cards = new CardLayout();
    center = new JPanel(false);
    center.setLayout(cards);

    // Build the combo box from the baseList
    JPanel bp = new JPanel(false);
    bases = new JComboBox();
    bp.add(bases);

    Vector baseList = parent.getgclient().getBaseList();
    Hashtable baseNames = parent.getgclient().getBaseNames();
    Hashtable baseToShort = parent.getgclient().getBaseToShort();
    paneHash = new Hashtable();

    try
      {
	for (int i = 0; i < baseList.size(); i++)
	  {
	    Base b = (Base)baseList.elementAt(i);

	    if (b.isEmbedded())
	      {
		if (debug)
		  {
		    System.out.println("Skipping embedded field");
		  }
	      }
	    else
	      {
		String name = (String)baseNames.get(b);
		bases.addItem (name);
		objectPane p = new objectPane(editable, 
					      this,
					      ((Short)baseToShort.get(b)).shortValue(),
					      field);
		paneHash.put(name, p);
		center.add(name, p);
	      }
	  }
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("could not load the combobox: " + rx);
      }

    bases.addItemListener(this);

    remove(holder);
    add("North", bp);
    add("Center", center);

    invalidate();
    parent.validate();
    System.out.println("Done in thread, she's loaded!");

    JPanel emptyP = new JPanel();
    center.add("empty", emptyP);

    cards.show(center, "empty");
  }

  public void itemStateChanged(ItemEvent event)
  {
    if (event.getStateChange() == ItemEvent.DESELECTED)
      {
	System.out.println("I DON'T CARE IF YOU ARE DESELECTED!");
      }
    else if (event.getStateChange() == ItemEvent.SELECTED)
      {
	String item = (String)event.getItem();
	
	if (debug)
	  {
	    System.out.println("Item selected: " + item);
	  }
	
	objectPane op = (objectPane) paneHash.get(item);
	
	if (!op.isStarted())
	  {
	    parent.getgclient().setStatus("Downloading objects for thi sbase");
	    Thread thread = new Thread(op);
	    thread.start();
	  }
	
	cards.show(center, item);
      }
    else
      {
	System.out.println("What the hell kind of item event is this? " + event);
      }
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                                      objectPane

------------------------------------------------------------------------------*/

class objectPane extends JPanel implements JsetValueCallback, Runnable{

  private final static boolean debug = true;

  boolean
    stringSelector_loaded = false;

  tStringSelector 
    ss;

  boolean
    editable;

  Vector
    owned = null,
    possible;

  short
    type;

  QueryResult 
    result;

  ownershipPanel
    parent;

  invid_field
    field;

  JPanel
    filler;

  boolean
    isStarted = false;

  gclient
    gc;

  /* -- */

  // Most of the work is in the create() method, only called after this panel is shown

  public objectPane(boolean editable, ownershipPanel parent, short type, invid_field field)
  {
    this.field = field;
    this.editable = editable;
    this.type = type;
    this.parent = parent;

    gc = parent.gc;
    
    setLayout(new BorderLayout());
    filler = new JPanel();
    filler.add(new JLabel("Creating panel, please wait."));

    add("Center", filler);
  }

  public boolean isStarted()
  {
    return isStarted;
  }

  public void run()
  {
    isStarted = true;

    System.out.println("Loading one of the panels");

    // Get the list of selected choices

    try
      {
	QueryResult qResult;
	db_object object = parent.parent.object;

	// go back to the framePanel to get the invid

	QueryDataNode node = new QueryDataNode(SchemaConstants.OwnerListField, 
					       QueryDataNode.EQUALS, 
					       QueryDataNode.CONTAINSANY, 
					       parent.parent.getObjectInvid());

	qResult = parent.parent.getgclient().getSession().query(new Query(type, node));

	if (debug)
	  {
	    if (qResult == null)
	      {
		System.out.println("Hey, the qResult is null.");
	      }
	    else
	      {
		System.out.println("Found " + qResult.size() + " matching items.");
	      }
	  }
	
	owned = qResult.getListHandles();
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not get Query: " + rx);
      }

    // Get the list of possible objects

    Short key = new Short(type);

    try
      {
	if ((key != null) && (parent.parent.getgclient().cachedLists.containsKey(key)))
	  {
	    if (debug)
	      {
		System.out.println("using cached copy");
	      }

	    possible = (Vector)parent.parent.getgclient().cachedLists.get(key);
	  }
	else
	  {
	    parent.parent.getgclient().setStatus("Downloading list of all objects, hold yer horses");

	    result = parent.parent.getgclient().getSession().query(new Query(type));
	    possible = result.getListHandles();
    
	    if (key != null)
	      {
		if (debug)
		  {
		    System.out.println("Adding new key to cachedList: " + key);
		  }

		parent.parent.getgclient().cachedLists.put(key, possible);
	      }
	  }

      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not get QueryResult for all objects: " + rx);
      }

    
    if (debug)
      {
	if (owned != null)
	  {
	    System.out.println("Creating string selector: owned: " + owned.size());
	  }

	if (possible != null)
	  {
	    System.out.println(" possible: " + possible.size());
	  }

	if ((owned == null) && (possible == null))
	  {
	    System.out.println("Both owned and possible are null");
	  }
      }

    ss = new tStringSelector(possible, owned, this, editable,100);

    ss.setCallback(this);
    remove(filler);
    add("Center", ss);
    
    invalidate();
    parent.validate();
    stringSelector_loaded = true;

    System.out.println("Done with thread, panel is loaded.");
    parent.parent.getgclient().setStatus("Done.");
  }

  public boolean isCreated()
  {
    return stringSelector_loaded;
  }

  /**
   *
   * Callback for our stringSelector
   *
   */

  public boolean setValuePerformed(JValueObject e)
  {
    ReturnVal retVal;
    boolean succeeded = false;

    /* -- */

    if (e.getOperationType() == JValueObject.ADD)
      {
	if (debug)
	  {
	    System.out.println("Adding object to list");
	  }

	try
	  {
	    retVal = field.addElement((Invid)e.getValue());

	    succeeded = (retVal == null) ? true : retVal.didSucceed();

	    if (retVal != null)
	      {
		gc.handleReturnVal(retVal);
	      }
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Could not add value to list: " + rx);
	  }
      }
    else if (e.getOperationType() == JValueObject.DELETE)
      {
	if (debug)
	  {
	    System.out.println("Deleting object from list");
	  }

	try
	  {
	    retVal = field.deleteElement(e.getValue());

	    succeeded = (retVal == null) ? true : retVal.didSucceed();

	    if (retVal != null)
	      {
		gc.handleReturnVal(retVal);
	      }
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Could not delete value from list: " + rx);
	  }
      }

    if (debug)
      {
	System.out.println("returnValue = " + succeeded);
      }
    
    return succeeded;
  }

}
