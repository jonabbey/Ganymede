/*

   ownerPanel.java

   The individual frames in the windowPanel.
   
   Created: 9 September 1997
   Version: $Revision: 1.13 $ %D%
   Module By: Michael Mulvaney
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.client;

import java.awt.*;
import java.rmi.*;
import java.util.*;

import com.sun.java.swing.*;
import com.sun.java.swing.border.*;

import arlut.csd.ganymede.*;

import arlut.csd.JDataComponent.*;

public class ownerPanel extends JPanel implements JsetValueCallback, Runnable {

  private final static boolean debug = true;

  boolean
    editable;

  invid_field
    field;

  framePanel
   fp;

  gclient
    gc;

  JPanel
    holdOnPanel;

  /* -- */

  public ownerPanel(invid_field field, boolean editable, framePanel fp)
  {
    if (debug)
      {
	System.out.println("Adding ownerPanel");
      }
    
    this.editable = editable;
    this.field = field;
    this.fp = fp;

    gc = fp.wp.gc;

    setLayout(new BorderLayout());

    setBorder(new EmptyBorder(new Insets(5,5,5,5)));

    holdOnPanel = new JPanel();
    holdOnPanel.add(new JLabel("Loading ownerPanel, please wait.", 
			       new ImageIcon(fp.getWaitImage()), SwingConstants.CENTER));
      
    add("Center", holdOnPanel);
    invalidate();
    fp.validate();
    
    Thread thread = new Thread(this);
    thread.start();
  }

  public void run()
  {
    System.out.println("Starting new thread");

    if (field == null)
      {
	JLabel l = new JLabel("There is no owner for this object.");
	add("Center", l);
      }
    else
      {
	try
	  {
	    tStringSelector ownerList = createInvidSelector(field);
	    ownerList.setBorder(new LineBorder(Color.black));
	    ownerList.setVisibleRowCount(-1);
	    remove(holdOnPanel);
	    add("Center", ownerList);
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Could not addDateField: " + rx);
	  }
      }

    invalidate();
    fp.validate();
  }

  private tStringSelector createInvidSelector(invid_field field) throws RemoteException
  {
    QueryResult
      results,
      choiceResults = null;

    Vector
      currentOwners = null,
      availableOwners = null;

    /* -- */

    if (debug)
      {
	System.out.println("Adding StringSelector, its a vector of invids!");
      }

    if (editable)
      {
	Object key = field.choicesKey();
	
	if ((key != null) && (fp.getgclient().cachedLists.containsKey(key)))
	  {
	    if (debug)
	      {
		System.out.println("Using cached copy...");
	      }
	    
	    availableOwners = (Vector)fp.getgclient().cachedLists.get(key);
	  }
	else
	  {
	    if (debug)
	      {
		System.out.println("Downloading copy");
	      }

	    availableOwners = field.choices().getListHandles();
	    
	    if (key != null)
	      {
		if (debug)
		  {
		    System.out.println("Saving this under key: " + key);
		  }
		
		fp.getgclient().cachedLists.put(key, availableOwners);
	      }
	  }
      }

    currentOwners = field.encodedValues().getListHandles();

    // availableOwners might be null
    // if editable is false, availableOwners will be null

    tStringSelector ss = new tStringSelector(availableOwners, 
					     currentOwners, 
					     this, editable, 
					     100, "Owners", "Owner Groups");
    if (editable)
      {
	ss.setCallback(this);
      }

    return ss;
  }

  public boolean setValuePerformed(JValueObject o)
  {
    ReturnVal retVal;
    boolean succeeded = false;

    /* -- */

    if (o.getSource() instanceof tStringSelector)
      {
	if (o.getOperationType() == JValueObject.ERROR)
	  {
	    fp.getgclient().setStatus((String)o.getValue());
	  }
	else if (o.getValue() instanceof Invid)
	  {
	    Invid invid = (Invid)o.getValue();
	    int index = o.getIndex();

	    try
	      {
		if (o.getOperationType() == JValueObject.ADD)
		  {
		    retVal = field.addElement(invid);

		    succeeded = (retVal == null) ? true : retVal.didSucceed();

		    if (retVal != null)
		      {
			gc.handleReturnVal(retVal);
		      }
		  }
		else if (o.getOperationType() == JValueObject.DELETE)
		  {
		    retVal = field.deleteElement(invid);

		    succeeded = (retVal == null) ? true : retVal.didSucceed();

		    if (retVal != null)
		      {
			gc.handleReturnVal(retVal);
		      }
		  }
	      }
	    catch (RemoteException rx)
	      {
		throw new RuntimeException("Could not change owner field: " + rx);
	      }
	  }
      }
    else
      {
	System.out.println("Where did this setValuePerformed come from?");
      }
    
    if (succeeded)
      {
	fp.getgclient().somethingChanged();
      }
    
    return succeeded;
  }
}
