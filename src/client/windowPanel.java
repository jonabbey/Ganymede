/*

   windowPanel.java

   The window that holds the frames in the client.
   
   Created: 11 July 1997
   Version: $Revision: 1.9 $ %D%
   Module By: Michael Mulvaney
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.client;

import tablelayout.*;
import com.sun.java.swing.*;


import java.awt.*;
import java.awt.event.*;
import java.rmi.*;
import java.util.*;

import arlut.csd.ganymede.*;
import arlut.csd.ganymede.client.*;

import arlut.csd.JDataComponent.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                     windowPanel

------------------------------------------------------------------------------*/

public class windowPanel extends JPanel implements ActionListener, InternalFrameListener, JsetValueCallback{

  static final boolean debug = true;

  // --

  gclient
    parent;

  JLayeredPane 
    lc;

  int 
    topLayer = 0,
    windowCount = 0;


  Hashtable
    objectHash = new Hashtable(),
    windowList = new Hashtable();

  Menu
    windowMenu;

  WindowBar 
    windowBar = null;

  /* -- */

  /**
   *
   * windowPanel constructor
   *
   */

  public windowPanel(gclient parent, Menu windowMenu)
  {
    if (debug)
      {
	System.out.println("Initializing windowPanel");
      }

    this.parent = parent;
    this.windowMenu = windowMenu;

    this.setBuffered(true);

    setLayout(new BorderLayout());
    lc = new JLayeredPane();

    add("Center", lc);

    //windowBar.addButton("Test");
    //add("South", windowBar);
  }

  /**
   * Get the parent gclient
   */
  public gclient getgclient()
    {
      return parent;
    }

  /**
   *
   * Attach a WindowBar object to this windowPanel
   *
   */

  public void addWindowBar(WindowBar windowBar)
  {
    this.windowBar = windowBar;
  }

  /**
   *
   * Create a new view-only window in this windowPanel.
   *
   * @param object an individual object from the server to show
   * in this window
   *
   */

  public void addWindow(db_object object)
  {
    this.addWindow(object, false);
  }

  /**
   *
   * Create a new editable or view-only window in this windowPanel.
   *
   * @param object an individual object from the server to show
   * in this window
   * @param editable if true, the object will be presented as editable
   *
   */

  public void addWindow(db_object object, boolean editable)
  {
    String 
      title, temp;

    JInternalFrame 
      w;

    JPanel 
      jpanel;

    JTitledPane 
      panel;

    TableLayout 
      layout;

    db_field[] 
      fields = null;

    JScrollPane
      scrollpane;

    JstringField
      sf;

    /* -- */

    if (object == null)
      {
	System.err.println("null object passed to addWindow");
	return;
      }

    if (editable)
      {
	parent.setStatus("Opening object for edit");
	
	if (debug)
	  {
	    System.out.println("Setting status for edit");
	  }
      }
    else
      {
	parent.setStatus("Getting object for viewing");

	if (debug)
	  {
	    System.out.println("Setting status for viewing");
	  }
      }

    parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

    if (debug)
      {
	System.out.println("Adding new internalFrame");
      }

    w = new JInternalFrame();

    w.setMaxable(true);
    w.setResizable(true);
    w.setClosable(!editable);

    // First figure out the title, and put it in the hash
    
    try
      {
	title = object.getLabel();

	if (editable)
	  {
	    title = "Edit: " + title;
	  }
	else
	  {
	    title = "View: " + title;
	  }
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not get label of object: " + rx);
      }

    // Create a unique title for the new window

    temp = title;
    int num = 2;

    while (windowList.containsKey(title))
      {
	title = temp + num++;
      }

    w.setTitle(title);
    w.addFrameListener(this);
    w.setLayout(new BorderLayout());
    
    windowList.put(title, w);
      
    if (windowBar != null)
      {
	windowBar.addButton(title);
      }

    //System.out.println("   adding to windowBar " + title);

    jpanel = new JPanel();
    jpanel.setLayout(new BorderLayout());

    panel = new JTitledPane();
    layout = new TableLayout(false);
    layout.rowSpacing(5);
    panel.setLayout(layout);

    // Get the list of fields

    try
      {
	fields = object.listFields();
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not get the fields: " + rx);
      }

    if (debug)
      {
	System.out.println("Entering big loop");
      }

    if ((fields != null) && (fields.length > 0))
      {
	short type = -1;
	String name;
	boolean isVector = false;

	for (int i = 0; i < fields.length ; i++)
	  {
	    type = -1;
	    name = null;
	    isVector = false;

	    try
	      {
		type = fields[i].getType();
		name = fields[i].getName();

		if (debug)
		  {
		    System.out.println("Name: " + name + "Field type desc: " + type);
		  }
		
		isVector = fields[i].isVector();
	      }
	    catch  (RemoteException rx)
	      {
		throw new RuntimeException("Could not get field info: " + rx);
	      }

	    if (isVector)
	      {
		if (debug)
		  {
		    System.out.println("Adding vector panel");
		  }

		if (fields[i] == null)
		  {
		    System.out.println("fields[i] is null");
		  }
		else
		  {
		    vectorPanel vp = new vectorPanel(fields[i], this, editable);
		    addRow(panel, vp, name, i);
		  }
	      }
	    else
	      {

		switch (type)
		  {
		  case -1:

		    System.err.println("Could not get field information");

		    break;

		  case FieldType.STRING:

		    sf = new JstringField(20,
					  19,
					  new JcomponentAttr(null,
							     new Font("Helvetica",Font.PLAIN,12),
							     Color.black,Color.white),
					  true,
					  false,
					  null,
					  null);

		    objectHash.put(sf, fields[i]);

		    try
		      {
			sf.setText((String)fields[i].getValue());
		      }
		    catch (RemoteException rx)
		      {
			throw new RuntimeException("Could not get value for field: " + rx);
		      }
		      
		    sf.setCallback(this);
		    sf.setEditable(editable);
		      
		    try
		      {
			sf.setToolTipText((String)fields[i].getComment());
			//System.out.println("Setting tool tip to " + (String)fields[i].getComment());
		      }
		    catch (RemoteException rx)
		      {
			throw new RuntimeException("Could not get tool tip text: " + rx);
		      }
		      
		    addRow(panel, sf, name, i);

		    break;

		  case FieldType.PASSWORD:

		    if (editable)
		      {
			JpassField pf = new JpassField(parent, true, 10, 8, editable);
			objectHash.put(pf, fields[i]);
		      
			pf.setCallback(this);
		      
			try
			  {
			    pf.setToolTipText((String)fields[i].getComment());
			    //System.out.println("Setting tool tip to " + (String)fields[i].getComment());
			  }
			catch (RemoteException rx)
			  {
			    throw new RuntimeException("Could not get tool tip text: " + rx);
			  }
		      
			addRow(panel, pf, name, i);
		      }
		    else
		      {
			sf = new JstringField(20,
					      19,
					      new JcomponentAttr(null,
								 new Font("Helvetica",Font.PLAIN,12),
								 Color.black,Color.white),
					      true,
					      false,
					      null,
					      null);

			objectHash.put(sf, fields[i]);

			// the server won't give us an unencrypted password, we're clear here

			try
			  {
			    sf.setText((String)fields[i].getValue());
			  }
			catch (RemoteException rx)
			  {
			    throw new RuntimeException("Could not get value for field: " + rx);
			  }
		      
			sf.setEditable(false);

			addRow(panel, sf, name, i);
		      }
		    
		    break;

		  case FieldType.DATE:

		    JdateField df = new JdateField();

		    objectHash.put(df, fields[i]);
		    df.setEditable(editable);
		    df.setCallback(this);

		    try
		      {
			Date date = ((Date)fields[i].getValue());

			if (date != null)
			  {
			    df.setDate(date);
			  }
		      }
		    catch (RemoteException rx)
		      {
			throw new RuntimeException("Could not get date: " + rx);
		      }
		      
		    addRow(panel, df, name, i);

		    break;

		  case FieldType.BOOLEAN:

		    //JcheckboxField cb = new JcheckboxField();
		    JCheckbox cb = new JCheckbox();
		    objectHash.put(cb, fields[i]);
		    cb.setEnabled(editable);
		    cb.addActionListener(this);
		    //cb.setCallback(this);

		    try
		      {		 
			cb.setSelected(((Boolean)fields[i].getValue()).booleanValue());
		      }
		    catch (RemoteException rx)
		      {
			throw new RuntimeException("Could not set checkbox value: " + rx);
		      }
		    catch (NullPointerException ex)
		      {
			System.out.println("Null pointer: " + ex);
		      }
		    addRow(panel, cb, name, i); 

		    break;

		  case FieldType.PERMISSIONMATRIX:

		    if (debug)
		      {
			System.out.println("Adding perm matrix");
		      }

		    perm_button pb = new perm_button((perm_field) fields[i],
						     editable,
						     parent.baseHash);

		    addRow(panel, pb, name, i);

		    break;

		  default:

		    JLabel label = new JLabel("Field type ID = " + type);
		    addRow(panel, label, name, i);
		  }
	      }
	  }
      }

    if (debug)
      {
	System.out.println("Done with loop");
      }

    //panel.setSize(500, 500);
    
    //JViewport vp = new JViewport();
    //vp.add(panel);
    
    jpanel.add("Center", panel);
    
    //panel.setSize(500,500);
    
    scrollpane = new JScrollPane();

    //scrollpane.setViewport(vp);

    scrollpane.getViewport().add(jpanel);
    w.add(scrollpane);

    //w.setBounds(20,20, panel.getPreferredSize().width, panel.getPreferredSize().height);
    
    w.setBounds(windowCount*20, windowCount*20, 400,250);

    if (windowCount > 10)
      {
	windowCount = 0;
      }
    else
      {
	windowCount++;
      }

    w.setLayer(topLayer);
            
    lc.add(w);
    lc.moveToFront(w);
    updateMenu();

    parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    parent.setStatus("Done.");

  }

  public void addTableWindow(Session session, Query query, Vector results, String title)
  {
    gResultTable 
      rt = null;

    String 
      temp = title;

    int
      num;

    /* -- */

    parent.setStatus("Querying object types");
    parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

    try
      {
	rt = new gResultTable(this, session, query, results);
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("could not make results table: " + rx);
      }

    if (rt == null)
      {
	System.out.println("rt == null");
      }
    else
      {
	rt.setLayer(topLayer);
	rt.setBounds(windowCount*20, windowCount*20, 500,500);
	rt.setResizable(true);
	rt.setClosable(true);
	rt.setMaxable(true);
	rt.addFrameListener(this);

	if (windowCount > 10)
	  {
	    windowCount = 0;
	  }
	else
	  {
	    windowCount++;
	  }

	// Figure out the title

	temp = title;
	num = 2;

	while (windowList.containsKey(title))
	  {
	    title = temp + num++;
	  }
	  
	//System.out.println("Setting title to " + title);

	rt.setTitle(title);

	windowList.put(title, rt);
	windowBar.addButton(title);
	  
	lc.add(rt);
	lc.moveToFront(rt);
	updateMenu();
	parent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	parent.setStatus("Done.");
      }
  }

  /**
   * Closes all the windows
   *
   */

  public void closeAll()
  {
    Enumeration windows = windowList.keys();      

    while (windows.hasMoreElements())
      {
	JInternalFrame w = (JInternalFrame)windowList.get(windows.nextElement());
	w.close();
      }
  }

  /**
   * Closes all windows that are open for editing.  
   *
   * <p>This should be called by the parent when the transaction is canceled, to get rid of
   * windows that might confuse the user.</p>
   */

  public void closeEditables()
  {
    JInternalFrame w;
    Enumeration windows;

    /* -- */
    
    windows = windowList.keys();      

    while (windows.hasMoreElements())
      {
	w = (JInternalFrame)windowList.get(windows.nextElement());
	  
	// This seems backwards, but only non-editable windows are closable.
	// So if isClosable is false, then it is editable, and we should
	// close it.

	if (w.isClosable())
	  {
	    //This is a view window
	  }
	else
	  {
	    w.close();
	  }
      }
  }
  
  /**
   * Closes all non-editable windows
   *
   */

  public void closeNonEditables()
  {
    JInternalFrame w;
    Enumeration windows;

    /* -- */

    windows = windowList.keys();

    while (windows.hasMoreElements())
      {
	w = (JInternalFrame)windowList.get(windows.nextElement());

	if (w.isClosable())
	  {
	    w.close();
	  }
      }
  }

  public Menu updateMenu()
  {
    Enumeration windows;
    Object obj;
    MenuItem MI;

    /* -- */

    windowMenu.removeAll();
    windows = windowList.keys();      

    while (windows.hasMoreElements())
      {
	obj = windowList.get(windows.nextElement());
	MI = null;

	if (obj instanceof JInternalFrame)
	  {
	    MI = new MenuItem(((JInternalFrame)obj).getTitle());
	  }
	else if (obj instanceof gResultTable)
	  {
	    MI = new MenuItem(((gResultTable)obj).getTitle());
	  }

	if (MI != null)
	  {
	    MI.addActionListener(this);
	    windowMenu.add(MI);
	  }
      }

    return windowMenu;
  }

  public void showWindow(String title)
  {
    Object obj = windowList.get(title);

    if (obj instanceof JInternalFrame)
      {
	((JInternalFrame)obj).moveToFront();
      }
    else if (obj instanceof gResultTable)
      {
	((gResultTable)obj).moveToFront();
      }
  }

  public void refreshTableWindows()
  {
    Object obj;
    Enumeration enum = windowList.keys();

    while (enum.hasMoreElements())
      {
	obj = windowList.get(enum.nextElement());
	if (obj instanceof gResultTable)
	  {
	    ((gResultTable)obj).refreshQuery();
	  }
      }
  }

  // Event handlers

  public boolean setValuePerformed(JValueObject v)
  {
    if (v.getSource() instanceof JstringField)
      {
	System.out.println((String)v.getValue());
	db_field field = (db_field)objectHash.get(v.getSource());

	try
	  {
	    System.out.println(field.getTypeDesc() + " trying to set to " + v.getValue());

	    if (field.setValue(v.getValue()))
	      {
		parent.somethingChanged = true;
		return true;
	      }
	    else
	      {
		System.err.println("Could not change field, reverting to " + (String)field.getValue());
		((JstringField)v.getSource()).setText((String)field.getValue());
		return false;
	      }
	  }
	catch (RemoteException rx)
	  {
	    throw new IllegalArgumentException("Could not set field value: " + rx);
	  }
      }
    else if (v.getSource() instanceof JpassField)
      {
	System.out.println((String)v.getValue());
	pass_field field = (pass_field)objectHash.get(v.getSource());

	try
	  {
	    System.out.println(field.getTypeDesc() + " trying to set to " + v.getValue());

	    if (field.setPlainTextPass((String)v.getValue()))
	      {
		parent.somethingChanged = true;
		return true;
	      }
	    else
	      {
		System.err.println("Could not change field");
		return false;
	      }
	  }
	catch (RemoteException rx)
	  {
	    throw new IllegalArgumentException("Could not set field value: " + rx);
	  }
 
      }
    else if (v.getSource() instanceof JdateField)
      {
	System.out.println("date field changed");
	db_field field = (db_field)objectHash.get(v.getSource());

	try
	  {
	    parent.somethingChanged = true;
	    return field.setValue(((JdateField)v.getSource()).getDate());
	  }
	catch (RemoteException rx)
	  {
	    throw new IllegalArgumentException("Could not set field value: " + rx);
	  }
      }
    else if (v.getSource() instanceof vectorPanel)
      {
	System.out.println("Something happened in the vector panel");
	parent.somethingChanged = true;
      }
    else
      {
	System.out.println("Value performed from unknown source");
      }
    return true;
  }

  public void actionPerformed(ActionEvent e)
  {
    if (e.getSource() instanceof MenuItem)
      {
	String label = ((MenuItem)e.getSource()).getLabel();
	showWindow(label);
      }
    else if (e.getSource() instanceof JCheckbox)
      {
	db_field field = (db_field)objectHash.get(e.getSource());

	try
	  {
	      
	    if (field.setValue(new Boolean(((JCheckbox)e.getSource()).isSelected())))
	      {
		parent.somethingChanged = true;
	      }
	    else
	      {
		System.err.println("Could not change checkbox, resetting it now");
		((JCheckbox)e.getSource()).setSelected(((Boolean)field.getValue()).booleanValue());
	      }
	  }
	catch (RemoteException rx)
	  {
	    throw new IllegalArgumentException("Could not set field value: " + rx);
	  }
      }
  }

  public  void frameDidClose(InternalFrameEvent e)
  {
    String oldTitle = e.getInternalFrame().getTitle();

    //System.out.println(" Removing button- " + oldTitle);

    windowList.remove(oldTitle);
    windowBar.removeButton(oldTitle);
    updateMenu();
  }

  public  void frameDidMaximize(InternalFrameEvent e)
  {
    System.out.println("frameDidMaximize");
  }

  public  void frameDidMinimize(InternalFrameEvent e)
  {
    System.out.println("frameDidMinimize");
  }

  public  void frameDidIconify(InternalFrameEvent e)
  {
    System.out.println("frameDidIconify");
  }

  public  void frameDidDeiconify(InternalFrameEvent e)
  {
    System.out.println("frameDidDeiconify");
  }
   
  public  void frameDidBecomeMain(InternalFrameEvent e)
  {
    System.out.println("frameDidBecomeMain");
  }
   
  public  void frameDidLoseMain(InternalFrameEvent e)
  {
    System.out.println("frameDidLoseMain");
  }
   
  public  void frameDidSize(InternalFrameEvent e)
  {
    System.out.println("frameDidSize");
  }
  
  public  void frameDidMove(InternalFrameEvent e)
  {
    System.out.println("frameDidMove");
  }

  // Convenience methods
  void addRow(Panel parent, Component comp,  String label, int row)
  {
    JLabel l = new JLabel(label);
    parent.add("0 " + row + " lhwHW", l);
    parent.add("1 " + row + " lhwHW", comp);
  }

  void addRow(JPanel parent, Component comp,  String label, int row)
  {
    JLabel l = new JLabel(label);
    parent.add("0 " + row + " lhwHW", l);
    parent.add("1 " + row + " lhwHW", comp);
  }

  void addRow(JComponent parent, Component comp,  String label, int row)
  {
    JLabel l = new JLabel(label);
    parent.add("0 " + row + " lthwHW", l);
    parent.add("1 " + row + " lthwHW", comp);
  }
}

