/*

 JstringListBox.java

 An implementation of JListBox used to display strings.

 Created: 21 Aug 1997
 Version: $Revision: 1.17 $ %D%
 Module By: Mike Mulvaney
 Applied Research Laboratories, The University of Texas at Austin


*/

package arlut.csd.JDataComponent;

import com.sun.java.swing.*;
import com.sun.java.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Vector;
import arlut.csd.Util.VecQuickSort;

public class JstringListBox extends JList implements ActionListener, ListSelectionListener, MouseListener {

  static final boolean debug = false;

  // -- 

  int 
    numberOfStrings = 0;

  DefaultListModel 
    model = new DefaultListModel();

  boolean
    sorted,
    allowCallback = false;

  JsetValueCallback 
    my_parent;

  JPopupMenu
    popup = null;

  /* -- */

  /**
   *
   * Default Constructor
   *
   */

  public JstringListBox()
  {
    this(null, false, null);
  }

  /**
   * Constructor
   *
   * @param items Vector of items (Strings or listHandles) to show in the list
   */
  public JstringListBox(Vector items)
  {
    this(items, false, null);
  }

  /**
   *
   * Constructor 
   *
   * @param items Vector of items (Strings or listHandles) to show in the list
   * @param sorted If true, JstringListBox will not sort the vector(it is already sorted)
   *
   */

  public JstringListBox(Vector items, boolean sorted)
  {
    this(items, sorted, null);
  }

  /**
   *
   * Constructor 
   *
   * @param items Vector of items (Strings or listHandles) to show in the list
   * @param sorted If true, JstringListBox will not sort the vector(it is already sorted)
   * @param popup JPopupMenu that will be shown on right click.  Callback is of type PARAMETER
   *
   */

  public JstringListBox(Vector items, boolean sorted, JPopupMenu popup)
  {
    this.sorted = sorted;
    this.popup = popup;

    if (popup != null)
      {
	Component[] c = popup.getComponents();
	for (int i = 0; i < c.length; i++)
	  {
	    if (c[i] instanceof JMenuItem)
	      {
		JMenuItem pm = (JMenuItem)c[i];
		pm.addActionListener(this);
	      }
	    else
	      {
		throw new IllegalArgumentException("Hey, you are supposed to use JMenuItems in JPopupMenus, buddy.");
	      }
	  }
      }
	
    //model setSize(items.size());
    //System.out.println("Setting size to " + items.size());

    if ((items != null) && (items.size() > 0))
      {	
	if (items.elementAt(0) instanceof String)
	  {
	    if (!sorted)
	      {
		if (debug)
		  {
		    System.out.println("Sorting...");
		  }

		(new VecQuickSort(items,
				  new arlut.csd.Util.Compare() 
				  {
				    public int compare(Object a, Object b) 
				      {
					String aF, bF;
					
					aF = (String) a;
					bF = (String) b;
					int comp = 0;
					
					comp =  aF.compareTo(bF);
					
					if (comp < 0)
					  {
					    return -1;
					  }
					else if (comp > 0)
					  {
					    return 1;
					  }
					else
					  {
					    return 0;
					  }
				      }
				  }
				  )
		  ).sort();

		  if (debug)
		    {
		      System.out.println("Done sorting strings");
		    }
	      } 
	    String string;
	    for (int i=0 ; i<items.size() ; i++)
	      {
		string = (String)items.elementAt(i);
		if (debug)
		  {
		    System.err.println("JstringListBox: adding string " + string);
		  }
		
		insertHandleAt(new listHandle(string, string), i);
	      }
	  }
	else if (items.elementAt(0) instanceof listHandle)
	  {
	    if (!sorted)
	      {
		if (debug)
		  {
		    System.out.println("Sorting...");
		  }

		(new VecQuickSort(items,
				  new arlut.csd.Util.Compare() 
				  {
				    public int compare(Object a, Object b) 
				      {
					listHandle aF, bF;
					
					aF = (listHandle) a;
					bF = (listHandle) b;
					int comp = 0;
					
					comp =  aF.getLabel().compareTo(bF.getLabel());
					
					if (comp < 0)
					  {
					    return -1;
					  }
					else if (comp > 0)
					  { 
					    return 1;
					  } 
					else
					  { 
					    return 0;
					  }
				      }
				  }
				  )	      
		  ).sort();

		  if (debug)
		    {
		      System.out.println("Done sorting.");
		    }
	      }
	    
	    for (int i=0 ; i<items.size() ; i++)
	      {
		if (debug)
		  {
		    System.err.println("JstringListBox: adding listhandle " + ((listHandle) items.elementAt(i)).getLabel());
		  }
		
		insertHandleAt((listHandle)items.elementAt(i), i);
	      }
	  }
	else
	  {
	    System.out.println("Unsupported item type passed to JstringListBox in a vector(needs to be String or ListHandle)");
	  }
      
      }
    setModel(model);
    
    addMouseListener(this);

    // I don't know if these things do anything.

    setPrototypeCellValue("This is just used to calculate cell height");
    setFixedCellWidth(15);
    setSelectionMode(DefaultListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
  }

  /** Reload the list box.
   */

  public void reload(Vector items, boolean sorted)
  {
    model.removeAllElements();
    if ((items != null) && (items.size() > 0))
      {
	if (items.elementAt(0) instanceof listHandle)
	  {
	    if (!sorted)
	      {
		items = sortListHandleVector(items);
	      }

	    for (int i = 0; i < items.size(); i++)
	      {
		insertHandleAt((listHandle)items.elementAt(i), i);
	      }
	  }
	else  //It must be a string, or it will throw a ClassCastException
	  {
	    if (!sorted)
	      {
		items = sortStringVector(items);
	      }

	    for (int i = 0; i < items.size(); i++)
	      {
		String s = (String)items.elementAt(i);
		insertHandleAt(new listHandle(s,s), i);
	      }
	  }
	
      }
    else 
      {
	if (debug)
	  {
	    System.out.println("no items to add");
	  }
      }
  }


  /**
   * Convenience method to set the size on the model.
   *
   */
  public void setSize(int size)
  {
    model.setSize(size);
  }

  /**
   * Register a parent to receive callbacks.
   *
   * There are several kinds of type you might get back:
   * <ul>
   * <li><b>ADD</b> This is really a selection event
   * <li><b>INSERT</b> This is a double click
   * <li><b>PARAMETER</b> This is a PopupMenu event.  The parameter will be the ActionCommand from the JMenuItem.
   * </ul>
   */

  public void setCallback(JsetValueCallback parent)
  {
    if (debug)
      {
	System.out.println("Setting callback in JstringListBox");
      }

    if (parent == null)
      {
	throw new IllegalArgumentException("Invalid Parameter: parent cannot be null");
      }
    
    getSelectionModel().addListSelectionListener(this);

    my_parent = parent;

    allowCallback = true;
  }

  /**
   * Add an item to the list box.
   *
   * @param o Can be a String or listHandle.
   */
  public void addItem(Object o)
  {
    listHandle lh = null;
    if (o instanceof String)
      {
	lh = new listHandle((String)o, (String)o);
      }
    else if (o instanceof listHandle)
      {
	lh = (listHandle)o;
      }
    else
      {
	System.err.println("You gave me an object that is neither String nor listHandle: " + o);
      }

    int i = 0;

    int total = model.getSize();
    while ((i < total) && (lh.getLabel().compareTo(((listHandle)model.getElementAt(i)).getLabel()) > 0))
      {
	i++;
      }
      
    insertHandleAt(lh, i);
    setSelectedValue(lh, true);
  }

  /**
   * Use this one to skip the sorting.  Called by all the add methods.
   *
   */
  public void insertHandleAt(listHandle handle, int row)
  {
    model.insertElementAt(handle, row);
  }
  /**
   * Remove an item from list.
   *
   * @param o can be listHandle or String
   */
  public void removeItem(Object o)
  {
    if (o instanceof listHandle)
      {
	model.removeElement((listHandle)o);
      }
    else if (o instanceof String)
      {
	if (debug)
	  {
	    System.out.println("I am guessing you want me to remove a label...");
	  }

	removeLabel((String) o);
      }
    else
      {
	if (debug)
	  {
	    System.out.println("Ok, i will look for this object in the listHnaldes.");
	  }

	for (int i = 0; i < model.getSize(); i++)
	  {
	    if ((((listHandle)model.elementAt(i)).getObject()).equals(o))
	      {
		model.removeElementAt(i);
		break;
	      }
	  }
      }
  }

  /**
   * Remove an object by label
   *
   * @param s Label of object to remove.
   */

  public void removeLabel(String s)
  {
    for (int i = 0; i < model.getSize(); i++)
      {
	if ((((listHandle)model.elementAt(i)).getLabel()).equals(s))
	  {
	    model.removeElementAt(i);
	    break; //Assume there is only one?
	  }
      }
  }
  /**
   */
  public int getSizeOfList()
  {
    return model.getSize();
  }

  /**
   * Returns true if the list contains an object with the specified label.
   */
  public boolean containsLabel(String string)
  {
    return containsString(string);
  }

  /**
   * Returns true if the list contains an object with the specified label.
   *
   * Since everything is a listHandle internally, this is the same as containsLabel
   */

  public boolean containsString(String string)
  {
    for (int i = 0; i < model.getSize(); i++)
      {
	if ((((listHandle)model.elementAt(i)).getLabel()).equals(string))
	  {
	    return true;
	  }
      }
    return false;
  }

  /**
   * Returns true if the item is in the list
   *
   * @param o Can be a String(label) or listHandle
   */

  public boolean containsItem(Object o)
  {
    if (o instanceof String)
      {
	return containsLabel((String)o);
      }
    else if (o instanceof listHandle)
      {
	return model.contains(o);
      }
    else
      {
	for (int i = 0; i < model.getSize(); i++)
	  {
	    if (((listHandle)model.elementAt(i)).getObject().equals(o))
	      {
		return true;
	      }
	  }
      }
    return false;

  }
   
  /**
   * This selects the item with the given label.
   *
   */

  public void setSelectedLabel(String s)
  {
    int size = model.getSize();
    listHandle lh = null;
    for (int i = 0; i < size; i++)
      {
	lh = (listHandle)model.getElementAt(i);
	if (lh.getLabel().equals(s))
	  {
	    setSelected(lh);
	    break;
	  }
      }

  }

  /**
   * Sets the selected item.
   *
   * @param o Can be listHandle or String
   */

  public void setSelected(Object o)
  {
    if (o instanceof listHandle)
      {
	setSelectedValue(o, true); // use the JList.setSelectedValue(Object, boolean shouldScroll)
      }
    else if (o instanceof String)
      {
	setSelectedLabel((String)o);
      }
    else
      {
	System.out.println("What kinf od object? " + o);
      }

  }

  /**
   * This returns just the label.
   */
  public String getSelectedLabel()
  {
    return ((listHandle)model.elementAt(getSelectedIndex())).getLabel();
  }

  /**
   * This returns the whole listHandle.
   */
  public listHandle getSelectedHandle()
  {
    return (listHandle)model.elementAt(getSelectedIndex());
  }

  /**
   * Returns all the selected handles.
   */

  public Vector getSelectedHandles()
  {
    Vector v = new Vector();
    Object[] values = getSelectedValues();
    
    for (int i =0; i < values.length; i++)
      {
	v.addElement(values[i]);
      }
    return v;
  }

  /**
   *This returns the object, without the label.
   */
  public Object getSelectedItem()
  {
    try
      {
	listHandle lh = (listHandle)model.elementAt(getSelectedIndex());
	return lh.getObject();

      }
    catch (Exception e)
      {
	return null;
      }
  }

  // For the ListSelectionListener

  public void valueChanged(ListSelectionEvent e)
  {
    if (allowCallback)
      {
	boolean ok = false;

	try 
	  {
	    ok = my_parent.setValuePerformed(new JValueObject(this, 
							      getSelectedIndex(),
							      JValueObject.ADD));
	  }
	catch (java.rmi.RemoteException rx)
	  {
	    throw new RuntimeException("Could not setValuePerformed: " + rx);
	  }
	if (ok)
	  {
	    //do something
	  }
	else
	  {
	    //put it back
	  }
      }
    else
      {
	if (debug)
	  {
	    System.out.println("allowCallback = false");
	  }
      }
  }

  public void mouseClicked(MouseEvent e)
  {
    if (allowCallback)
      {
	if (SwingUtilities.isLeftMouseButton(e))
	  {
	    if (e.getClickCount() == 2)
	      {
		boolean ok = false;
	    
		int index = locationToIndex(e.getPoint());
		if (debug)
		  {
		    System.out.println("Double clicked on Item " + index);
		  }
		try
		  {
		    ok = my_parent.setValuePerformed(new JValueObject(this, 
								      index,
								      JValueObject.INSERT));
		  }
		catch (java.rmi.RemoteException rx)
		  {
		    throw new RuntimeException("Double click produced: " + rx);
		  }
	    
		if (debug)
		  {
		    System.out.println("setValue from JstringListBox=" + ok);
		  }
	      }
	  }
      }
  }

  public void mouseEntered(MouseEvent e)
    {

    }
  public void mouseExited(MouseEvent e)
    {

    }
  public void mousePressed(MouseEvent e)
    {
      if (SwingUtilities.isRightMouseButton(e))
	{
	  if (debug)
	    {
	      System.out.println("Its a popup trigger!");
	    }

	  if (popup != null)
	    {
	      popup.show(e.getComponent(), e.getX(), e.getY());
	    }

	  e.consume();
	}
      
    }
  public void mouseReleased(MouseEvent e)
    {

    }

  public void actionPerformed(ActionEvent e)
  {
    if (allowCallback)
      {
	if (e.getSource() instanceof JMenuItem)
	  {
	    String string = ((JMenuItem)e.getSource()).getActionCommand();
	    try
	      {
		my_parent.setValuePerformed(new JValueObject(this,
							     getSelectedIndex(),
							     JValueObject.PARAMETER,
							     getSelectedItem(),
							     string));
	      }
	    catch (java.rmi.RemoteException rx)
	      {
		throw new RuntimeException("Could not set value from JstringListBox: " + rx);
	      }

	  }

      }
  }

  /**
   * sort a vector of listHandles
   *
   * @param v Vector to be sorted
   * @return Vector of sorted listHandles(sorted by label)
   */
  private Vector sortListHandleVector(Vector v)
  {
    (new VecQuickSort(v, 
		      new arlut.csd.Util.Compare() {
      public int compare(Object a, Object b) 
	{
	  listHandle aF, bF;
	  
	  aF = (listHandle) a;
	  bF = (listHandle) b;
	  int comp = 0;
	  
	  comp =  aF.toString().compareTo(bF.toString());
	  
	  if (comp < 0)
	    {
	      return -1;
	    }
	  else if (comp > 0)
	    { 
	      return 1;
	    } 
	  else
	    { 
	      return 0;
	    }
	}
    })).sort();
    
    return v;
  }
  /**
   * Sort a vector of Strings
   *
   * @return Vector of sorted Strings.
   */
  public Vector sortStringVector(Vector v)
  {
    (new VecQuickSort(v, 
		      new arlut.csd.Util.Compare() {
      public int compare(Object a, Object b) 
	{
	  String aF, bF;
	  
	  aF = (String) a;
	  bF = (String) b;
	  int comp = 0;
	  
	  comp =  aF.compareTo(bF);
	  
	  if (comp < 0)
	    {
	      return -1;
	    }
	  else if (comp > 0)
	    { 
	      return 1;
	    } 
	  else
	    { 
	      return 0;
	    }
	}
    })).sort();
    
    return v;
  }

}
