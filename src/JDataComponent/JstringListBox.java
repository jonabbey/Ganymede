/*

 JstringListBox.java

 An implementation of JListBox used to display strings.

 Created: 21 Aug 1997
 Version: $Revision: 1.5 $ %D%
 Module By: Mike Mulvaney
 Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.JDataComponent;

import com.sun.java.swing.*;
import com.sun.java.swing.event.*;
import java.awt.*;
import java.util.Vector;

public class JstringListBox extends JListBox implements ListCellRenderer, ListSelectionListener{ 

  static final boolean debug = false;

  // -- 

  int 
    numberOfStrings = 0;

  DefaultListModel 
    model = new DefaultListModel();

  JLabel 
    label = new JLabel();

  boolean
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

  /**
   *
   * Constructor with list of initial items
   *
   */

  public JstringListBox(Vector items)
  {
    
    label.setBackground(Color.white);
    //model.setSize(items.size());
    //System.out.println("Setting size to " + items.size());

    if ((items != null) && (items.size() > 0))
      {	
	if (items.elementAt(0) instanceof String)
	  {
	    for (int i=0 ; i<items.size() ; i++)
	      {
		
		if (debug)
		  {
		    System.err.println("JstringListBox: adding string " + (String) items.elementAt(i));
		  }
		
		addString((String)items.elementAt(i));
		
	      }
	  }
	else if (items.elementAt(0) instanceof listHandle)
	  {
	    for (int i=0 ; i<items.size() ; i++)
	      {
		if (debug)
		  {
		    System.err.println("JstringListBox: adding listhandle " + ((listHandle) items.elementAt(i)).getLabel());
		  }
		
		addHandle((listHandle)items.elementAt(i));
	      }
	  }
	else
	  {
	    System.out.println("Unsupported item type");
	  }
      

      }
    setModel(model);
    
    setCellRenderer(this);
  }

  public void setSize(int size)
  {
    model.setSize(size);
  }

  public void setCallback(JsetValueCallback parent)
  {
    System.out.println("Setting callback in JstringListBox");

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
    model.addElement(new listHandle(label));
  }

  public void addHandle(listHandle handle)
  {
    addHandle(handle, false);
  }

  public void addHandle(listHandle handle, boolean redraw)
  {
    model.addElement(handle);
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

  // These two set up how the label is drawn.

  public void configureListCellRenderer(Object value, int index)
  {
    if (label == null)
      {
	if (debug)
	  {
	    System.out.println("label is null!");
	  }
	return;
      }
    if ((listHandle)model.elementAt(index) == null)
      {
	if (debug)
	  {
	    System.out.println("element at " + index + " is null");
	  }
	return;
      }
    if (debug)
      {
	System.out.println("element at" + index + " is " + ((listHandle)model.elementAt(index)).getLabel());
      }
    label.setText("");
    label.setText(((listHandle)model.elementAt(index)).getLabel());
  }

  public Component getListCellRendererComponent()
  {
    return label;
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
							      JValueObject.INSERT));
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
	System.out.println("allowCallback = false");
      }
  }
}
