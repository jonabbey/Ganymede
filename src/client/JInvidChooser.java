
/*

   JInvidChooser.java

   Like a JComboBox, but just for Invid's.  It has a couple of pretty
   buttons on the sides.
   
   Created: ?
   Version: $Revision: 1.8 $ %D%
   Module By: Mike Mulvaney
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.client;

import arlut.csd.ganymede.*;
import arlut.csd.JDataComponent.*;
import com.sun.java.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.util.Vector;


/*------------------------------------------------------------------------------
                                                                           class
                                                                   JInvidChooser

------------------------------------------------------------------------------*/

/**
 * Like a JComboBox, but just for Invid's.  It has a couple of pretty
 * buttons on the sides.
 */

public class JInvidChooser extends JPanelCombo implements ActionListener, ItemListener {

  private final static boolean debug = false;

  JMenuItem
    view,
    create;

  JMenu
    menu;

  JMenuBar
    menuBar;

  containerPanel
    cp;
  
  private short 
    type;

  private boolean
    removedNone = false,
    allowNone = true;

  private listHandle
    noneHandle = new listHandle("<none>", null);

  public JInvidChooser(containerPanel parent, short objectType)
  {
    this(null, parent, objectType);
  }

  public JInvidChooser(Vector objects, containerPanel parent, short objectType)
  {
    super(objects);

    cp = parent;
    type = objectType;

    //super();

    //Insets insets = new Insets(0,0,0,0);
    menu = new JMenu("click");
    menuBar = new JMenuBar();
    menuBar.add(menu);

    view = new JMenuItem("View");
    view.addActionListener(this);

    menu.add(view);

    if (objectType > -1) // If it is -1, then it doesn't have a target
      {
	create = new JMenuItem("New");
	create.addActionListener(this);
	menu.add(create);
      }

    add("Center", getCombo());
    add("East", menuBar);
  }

  public Invid getSelectedInvid()
  {
    listHandle lh = (listHandle) getSelectedItem();
    return (Invid) lh.getObject();
  }

  /**
   *
   * Set the allowNone bit.
   *
   * If allowNone is true, then <none> will remain as a choice in the
   * chooser.  If it is false, <none> will only be included in the
   * beginning if nothing is set; it will be removed as soon as
   * anything is chosen.  
   */

  public void setAllowNone(boolean allow)
  {
    // If we used to allow, but now we don't, we need to take out the
    // noneHandle if it is not selected.

    if (allowNone && (!allow) && (!removedNone))
      {
	Object item = getCombo().getSelectedItem();
	if ((item != null) && (!item.equals(noneHandle)))
	  {
	    if (debug)
	      {
		System.out.println("taking out <none>");
	      }

	    try
	      {
		getCombo().removeItem(noneHandle);
		removedNone = true;

		if (debug)
		  {
		    System.out.println("+setting removedNone to true");
		  }
	      }
	    catch (IllegalArgumentException ia)
	      {
		// none handle wasn't in there...
		removedNone = false;
	      }

	    getCombo().removeItemListener(this);
	  }
	else if (debug)
	  {
	    System.out.println("<none> is selected, I will wait.");
	  }
      }

    // Now if we are allowing none, but we weren't before, and we took
    // the none handle out, we have to put it back in

    if (removedNone && allow && !allowNone)
      {
	boolean found = false;

	for (int i = 0; i < getCombo().getItemCount(); i++)
	  {
	    if (getCombo().getItemAt(i).equals(noneHandle))
	      {
		found = true;
		break;
	      }
	  }

	if (!found)
	  {
	    if (debug)
	      {
		System.out.println("Putting none back in.");
	      }

	    getCombo().addItem(noneHandle);
	  }

	removedNone = false;

	if (debug)
	  {
	    System.out.println("+setting removedNone to false");
	  }
      }

    // The item listener will remove the <none> if it is deselected.

    if ((!allow) && (!removedNone))
      {
	if (debug)
	  {
	    System.out.println("Adding item listener.");
	  }

	getCombo().addItemListener(this);
      }

    allowNone = allow;
  }

  /**
   *
   * Get the allowNone bit.<br><br>
   *
   * If allowNone is true, then &lt;none&gt; will remain as a choice in the
   * chooser.  If it is false, &lt;none&gt; will only be included in the
   * beginning if nothing is set; it will be removed as soon as
   * anything is chosen.  
   */

  public boolean isAllowNone()
  {
    return allowNone;
  }

  public void itemStateChanged(ItemEvent e)
  {
    if (allowNone)
      {
	if (debug)
	  {
	    System.out.println("None is ok, not changing.");
	  }

	return;
      }

    if (e.getStateChange() == ItemEvent.DESELECTED)
      {
	if (debug)
	  {
	    System.out.println("Something was deslected, was it the noneHandle?");
	  }

	if ((e.getItem() != null) && e.getItem().equals(noneHandle))
	  {
	    if (getCombo().getSelectedItem() == null)
	      {
		if (debug)
		  {
		    System.out.println("There isn't even a noneHanedle in this one!");
		  }
	      }
	    else if (getCombo().getSelectedItem().equals(noneHandle))
	      {
		if (debug)
		  {
		    System.out.println("noneHandle is still selected.");
		  }
	      }
	    else
	      {
		if (debug)
		  {
		    System.out.println("Yes, it is the noneHandle.  Removing now.");
		  }
		
		try
		  {
		    getCombo().removeItem(noneHandle);
		  }
		catch (ArrayIndexOutOfBoundsException ae)
		  {
		    // noneHAndle wasn't in there.
		    System.out.println("ArrayIndexOutOfBoundsException: must not be in there.");
		  }
		catch (IllegalArgumentException asdf)
		  {
		    // why does Swing throw this?  To be difficult, I think.
		    System.out.println("IllegalArgumentException: must not be in there.  Or Swing's screwing with us.");
		  }
		
		getCombo().removeItemListener(this);
	      }
	  }

      }
  }

  public void actionPerformed(ActionEvent e)
  {
    if (e.getSource() == view)
      {
	listHandle lh = (listHandle)getSelectedItem();
	Invid invid = (Invid)lh.getObject();
	
	if (invid == null)
	  {
	    showErrorMessage("You don't have permission to view that object.");
	  }
	else
	  {
	    cp.gc.viewObject(invid);
	  }
      }
    else if (e.getSource() == create)
      {
	db_object object = null;
	
	try
	  {
	    object = cp.gc.createObject(type, false);

	    Invid invid = object.getInvid();
	    listHandle lh = new listHandle("New Object", invid);
	    invid_field field = (invid_field)cp.objectHash.get(this);
	    if (field == null)
	      {
		showErrorMessage("I can't create a new object, because I can't find the invid_field for this chooser.");
		return;
	      }
	    getCombo().addItem(lh);

	    field.setValue(invid);
	    getCombo().setSelectedItem(lh);

	  }
	catch (NullPointerException nx)
	  {
	    showErrorMessage("Got that pesky null pointer exception.  Permissions problem?");
	    return;
	  }
	catch (RuntimeException re)
	  {
	    showErrorMessage("Something when wrong creating the new object.  Perhaps you don't have the permission to creat objects of that type.");
	    return;
	  }
	catch (java.rmi.RemoteException rx)
	  {
	    throw new RuntimeException("I can't add this object in to the JComboBox: " + rx);
	  }

	if (object != null)
	  {
	    cp.gc.showNewlyCreatedObject(object, null, new Short(type));
	  }

      }

  }
  
  private final void  showErrorMessage(String message) {
    showErrorMessage("Error", message);
  }

  private final void  showErrorMessage(String title, String message) {
    cp.getgclient().showErrorMessage(title,message);
  }

}
