/*

 JstringListBox.java

 An implementation of JListBox used to display strings.

 Created: 21 Aug 1997
 Version: $Revision: 1.14 $ %D%
 Module By: Mike Mulvaney
 Applied Research Laboratories, The University of Texas at Austin



 Note:
   There needs to be a new version of this, JhandleListBox maybe, that
   just uses listHandles.  That would make it A LOT cleaner, and easier
   for the StringSelector.  That is the task for Monday.

*/

package arlut.csd.JDataComponent;

import com.sun.java.swing.*;
import com.sun.java.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Vector;
import arlut.csd.Util.VecQuickSort;

public class JstringListBox extends JList implements ListSelectionListener, MouseListener {

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

  /* -- */

  /**
   *
   * Default Constructor
   *
   */

  public JstringListBox()
  {
    this(null);
  }

  public JstringListBox(Vector items)
  {
    this(items, false);
  }

  /**
   *
   * Constructor with list of initial items
   *
   */

  public JstringListBox(Vector items, boolean sorted)
  {
    this.sorted = sorted;
    
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

    // let it know that we want 

    setPrototypeCellValue("This is just used to calculate cell height");
    setFixedCellWidth(15);
    setSelectionMode(DefaultListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

  }

  public void setSize(int size)
  {
    model.setSize(size);
  }

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

  public void removeItem(Object o)
  {
    if (o instanceof listHandle)
      {
	model.removeElement((listHandle)o);
      }
    else if (o instanceof String)
      {
	System.out.println("I am guessing you want me to remove a label...");
	removeLabel((String) o);
      }
    else
      {
	System.out.println("Ok, i will look for this object in the listHnaldes.");
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

  public int getSizeOfList()
  {
    return model.getSize();
  }

  public boolean containsLabel(String string)
  {
    return containsString(string);
  }

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
    listHandle lh = (listHandle)model.elementAt(getSelectedIndex());
    return lh.getObject();
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

  public void mouseEntered(MouseEvent e)
    {

    }
  public void mouseExited(MouseEvent e)
    {

    }
  public void mousePressed(MouseEvent e)
    {

    }
  public void mouseReleased(MouseEvent e)
    {

    }

}
