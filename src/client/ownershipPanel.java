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

public class ownershipPanel extends JPanel implements ItemListener {

  private final static boolean debug = true;

  invid_field
    field;

  boolean
    editable;
  
  framePanel
    parent;

  JPanel
    center;

  JComboBox
    bases;

  Enumeration
    baseList;

  Hashtable
    objects_owned,   // (Short)Base type -> (Vector)list of objects [all objects]
    paneHash;        // (String) base name -> objectPane holding base objects

  CardLayout
    cards;

  Vector
    result = null;

  public ownershipPanel(invid_field field, boolean editable, framePanel parent)
  {
    this.field = field;
    this.editable = editable;
    this.parent = parent;

    setLayout(new BorderLayout());

    cards = new CardLayout();
    center = new JPanel(false);
    center.setLayout(cards);
    add("Center", center);

    try
      {
	result = field.encodedValues().getListHandles();
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Can't get encoded Values: " + rx);
      }

    // Get the objects owned, and sort by type
    objects_owned = new Hashtable();
    for (int i = 0; i< result.size(); i++)
      {
	listHandle lh = (listHandle)result.elementAt(i);
       
	Invid invid = (Invid)lh.getObject();
	Short type = new Short(invid.getType());

	if (objects_owned.containsKey(type))
	  {
	    ((Vector)objects_owned.get(type)).addElement(lh);
	  }
	else
	  {
	    if (debug)
	      {
		System.out.println("Creating new vector!");
	      }
	    Vector v = new Vector();
	    v.addElement(lh);
	    objects_owned.put(type, v);
	  }
      }
	
  
    // Build the combo box from the baseHash keys
    JPanel bp = new JPanel(false);
    bases = new JComboBox();
    bp.add(bases);
    baseList = parent.getgclient().getBaseHash().keys();
    paneHash = new Hashtable();
    try
      {
	while (baseList.hasMoreElements())
	  {
	   
	    Base b = (Base)baseList.nextElement();
	    if (b.isEmbedded())
	      {
		if (debug)
		  {
		    System.out.println("Skipping embedded field");
		  }
	      }
	    else
	      {
		String name = b.getName();
		bases.addItem (name);
		objectPane p = new objectPane(editable, 
					      (Vector)objects_owned.get(new Short(b.getTypeID())),
					      this,
					      b.getTypeID(),
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

    add("North", bp);
    cards.first(center);
  }


  public void itemStateChanged(ItemEvent event)
  {

    String item = (String)event.getItem();

    if (debug)
      {
	System.out.println("Item changed: " + item);
      }

    objectPane op = (objectPane)paneHash.get(item);
    if (! op.isCreated())
      {
	parent.getgclient().setStatus("Downloading objects for thi sbase");
	op.create();
      }
    cards.show(center, item);

  }
}

class objectPane extends JPanel implements JsetValueCallback{

  private final static boolean debug = true;

  boolean
    stringSelector_loaded = false;

  tStringSelector 
    ss;

  boolean
    editable;

  Vector
    owned,
    possible;

  short
    type;

  QueryResult 
    result;

  ownershipPanel
    parent;

  invid_field
    field;

  // Most of the work is in the create() method, only called after this panel is shown
  public objectPane(boolean editable, Vector owned, ownershipPanel parent, short type, invid_field field)
  {
    this.field = field;
    this.editable = editable;
    this.owned = owned;
    this.type = type;
    this.parent = parent;
  }

  public void create()
  {
    setLayout(new BorderLayout());



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
    add("Center", ss);

    stringSelector_loaded = true;

    parent.parent.getgclient().setStatus("Done.");
  }

  public boolean isCreated()
  {
    return stringSelector_loaded;
  }


  public boolean setValuePerformed(JValueObject e)
    {
      boolean returnValue = false;
      if (e.getOperationType() == JValueObject.ADD)
	{
	  if (debug)
	    {
	      System.out.println("Adding object to list");
	    }

	  try
	    {
	      field.addElement((Invid)e.getValue());
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
	      returnValue = field.deleteElement(e.getValue());
	    }
	  catch (RemoteException rx)
	    {
	      throw new RuntimeException("Could not delete value from list: " + rx);
	    }
	}
      if (debug)
	{
	  System.out.println("returnValue = " + returnValue);
	}

      return returnValue;
    }

}
