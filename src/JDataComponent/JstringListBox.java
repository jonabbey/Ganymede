/*

 JstringListBox.java

 An implementation of JListBox used to display strings.

 Created: 21 Aug 1997
 Version: $Revision: 1.12 $ %D%
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
	    if (sorted)
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
	    for (int i=0 ; i<items.size() ; i++)
	      {
		if (debug)
		  {
		    System.err.println("JstringListBox: adding string " + (String) items.elementAt(i));
		  }
		
		insertHandleAt(new listHandle((String)items.elementAt(i)), i);
	      }
	  }
	else if (items.elementAt(0) instanceof listHandle)
	  {
	    if (sorted)
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
    setFixedCellWidth(-1);

    System.err.println("cell height is " + getFixedCellHeight());
    System.err.println("cell width is " + getFixedCellWidth());
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

  public void addString(String label)
  {
    addString(label, false);
  }

  public void addString(String label, boolean redraw)
  {
    int i = 0;
    if (sorted)
      {
	int total = model.getSize();
	while ((i < total) && (label.compareTo(((listHandle)model.getElementAt(i)).getLabel()) > 0))
	  {
	    i++;
	  }
      }

    insertHandleAt(new listHandle(label), i);
    
  }

  public void addHandle(listHandle handle)
  {
    addHandle(handle, false);
  }

  public void addHandle(listHandle handle, boolean redraw)
  {
    int i = 0;
    if (sorted)
      {
	int total = model.getSize();
	while ((i < total) && (handle.getLabel().compareTo(((listHandle)model.getElementAt(i)).getLabel()) > 0))
	  {
	    i++;
	  }
      }

    insertHandleAt(handle, i);
  }

  /**
   * Use this one to skip the sorting.  Called by all the add methods.
   *
   */
  public void insertHandleAt(listHandle handle, int row)
  {
    model.insertElementAt(handle, row);
  }

  public void removeString(String item)
  {
    removeString(item, false);
  }

  public void removeString(String item, boolean redraw)
  {
    //labels.removeElement(item);
    model.removeElement(item);
    if (redraw)
      {
	invalidate();
	validate();
      }
  }

  public void removeHandle(listHandle item)
  {
    removeHandle(item, false);
  }

  public void removeHandle(listHandle item, boolean redraw)
  {
    model.removeElement(item);
    if (redraw)
      {
	invalidate();
      }
  }

  public int getSizeOfList()
  {
    return model.getSize();
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

  public boolean containsHandle(listHandle h)
  {
    for (int i = 0; i < model.getSize(); i++)
      {
	if ((listHandle)model.elementAt(i) == h)
	  {
	    return true;
	  }
      }
    return false;
  }
  
  public String getSelectedString()
  {
    return ((listHandle)model.elementAt(getSelectedIndex())).getLabel();
  }

  public listHandle getSelectedHandle()
  {
    return (listHandle)model.elementAt(getSelectedIndex());
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
