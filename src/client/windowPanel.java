/*
  The window that holds the frames in the client.

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

public class windowPanel extends JPanel implements ActionListener, InternalFrameListener, JsetValueCallback{

  JLayeredPane 
    lc;

  int 
    topLayer = 0,
    windowCount = 0;

  JTitledPane 
    panel;

  Hashtable
    objectHash;

  Vector
    windowList;

  Menu
    windowMenu;

  public windowPanel(Menu windowMenu)
    {
      System.out.println("Initializing windowPanel");
      objectHash = new Hashtable();
      windowList = new Vector();
      this.setBuffered(true);
      this.windowMenu = windowMenu;
      setLayout(new BorderLayout());
      lc = new JLayeredPane();

      add("Center", lc);


    }

  public void addWindow(db_object object)
    {
      this.addWindow(object, false);
    }

  public void addWindow(db_object object, boolean editable)
    {
      if (object == null)
	{
	  System.err.println("null object passed to addWindow");
	  return;
	}

      System.out.println("Adding new internalFrame");
      JInternalFrame w = new JInternalFrame();
      w.setMaxable(true);
      w.setResizable(true);
      if (editable)
	{
	  w.setClosable(false);
	}
      else
	{
	  w.setClosable(true);
	}


      // First figure out the title, and put it in the hash
      String title = "Null";
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
      int numberOfTitles = 0;
      if (title == null)
	{
	  title = "Null";
	}
      for (int i = 0; i< windowList.size() ; i++)
	{
	  String t = null;
	  t = ((JInternalFrame)windowList.elementAt(i)).getTitle();
	  if (( t == null) || t.equals(""))
	    {
	      System.out.println("t is null");
	    }
	  else
	    {
	      if (t.lastIndexOf("-") == -1)
		{
		  if (t.equals(title))
		    {
		      numberOfTitles++;
		    }
		}
	      else if (t.substring(0, (t.lastIndexOf("-"))).equals(title))
		{
		  numberOfTitles++;
		}
	    }
	}
      if (numberOfTitles > 0)
	{
	  title = title + "- " + numberOfTitles;
	}
      System.out.println("Setting title to " + title);
      w.setTitle(title);
      windowList.addElement(w);

      w.addFrameListener(this);
      w.setLayout(new BorderLayout());

      panel = new JTitledPane();
      panel.setLayout(new TableLayout(false));

      // Get the list of fields
      db_field[] fields = null;
      try
	{
	  fields = object.listFields();
	}
      catch (RemoteException rx)
	{
	  throw new RuntimeException("Could not get the fields: " + rx);
	}
      if ((fields != null) && (fields.length > 0))
	{
	  for (int i = 0; i < fields.length ; i++)
	    {
	      String type = null;
	      String name = null;
	      try
		{
		  type = fields[i].getTypeDesc();
		  System.out.println("Field type desc: " + type);
		  name = fields[i].getName();
		}
	      catch  (RemoteException rx)
		{
		  throw new RuntimeException("Could not get field info: " + rx);
		}
	      if (type == null)
		{
		  System.err.println("Could not get field information");
		}
	      else if (type.equals("string"))
		{
		  JstringField sf = new JstringField(20,
						     19,
						     new JcomponentAttr(null,new Font("Helvetica",Font.PLAIN,12),Color.black,Color.white),
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

		  addRow(panel, sf, name, i);
		}
	      else if (type.equals("date"))
		{
		  JdateField df = new JdateField();
		  objectHash.put(df, fields[i]);
		  df.setEditable(editable);
		  df.setCallback(this);
		  addRow(panel, df, name, i);
		}
	      else
		{
		  JLabel label = new JLabel(type);
		  addRow(panel, label, name, i);
		}
	    }
	}

      JViewport vp = new JViewport();
      vp.add(panel);
      
      JScrollPane scrollpane = new JScrollPane();
      scrollpane.setViewport(vp);
      w.add(scrollpane);
      //w.setBounds(20,20, panel.getPreferredSize().width, panel.getPreferredSize().height);
      w.setBounds(windowCount*20, windowCount*20, 300,300);
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
      //lc.repaint();
    }

  public void addTableWindow(Session session, Vector results)
    {
      gResultTable rt = null;
      try
	{
	  rt = new gResultTable(session, results);
	  //windowList.add("Window " + windowCount);
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
	  if (windowCount > 10)
	    {
	      windowCount = 0;
	    }
	  else
	    {
	      windowCount++;
	    }
	  rt.setResizable(true);
	  rt.setClosable(true);
	  rt.setMaxable(true);
	  lc.add(rt);
	  lc.moveToFront(rt);
	  updateMenu();
	  //lc.repaint();
	}

    }

  /**
   * Closes all the windows
   *
   */
   public void closeAll()
    {
      System.out.println("Removing all windows");
      int size = windowList.size();
      System.out.println("There are " + windowList.size() + " windows to close");
      for (int i = size - 1; i > -1; i--)
	{
	  System.out.println("Closing number " + i);
	  JInternalFrame w = (JInternalFrame)windowList.elementAt(i);
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
      System.out.println("Removing editable windows");
      for (int i = windowList.size() - 1; i > -1; i--)
	{
	  JInternalFrame w = (JInternalFrame)windowList.elementAt(i);
	  
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
      System.out.println("Removing non-editable windows");
      for (int i = windowList.size() - 1; i > -1; i--)
	{
	  JInternalFrame w = (JInternalFrame)windowList.elementAt(i);

	  if (w.isClosable())
	    {
	      w.close();
	    }
	}
    }

  public Menu updateMenu()
    {
      windowMenu.removeAll();
      for (int i = 0; i< windowList.size(); i++)
	{
	  MenuItem MI = new MenuItem(((JInternalFrame)windowList.elementAt(i)).getTitle());
	  MI.addActionListener(this);
	  windowMenu.add(MI);
	}
      return windowMenu;
    }

  // Event handlers
  public boolean setValuePerformed(JValueObject v)
    {
      System.out.println("setValuePerformed");
      if (v.getSource() instanceof JstringField)
	{
	  System.out.println((String)v.getValue());
	  db_field field = (db_field)objectHash.get(v.getSource());
	  try
	    {
	      System.out.println(field.getTypeDesc() + " set to " + v.getValue());
	      return field.setValue(v.getValue());
	    }
	  catch (RemoteException rx)
	    {
	      throw new IllegalArgumentException("Could not set field value: " + rx);
	    }
	}
      else if (v.getSource() instanceof JdateField)
	{
	  System.out.println("date field changed");
	}
      else
	{
	  System.out.println("Value performed from unknown source");
	}
      return true;
    }

  public void actionPerformed(ActionEvent e)
    {
      System.out.println("Action performed");
      if (e.getSource() instanceof MenuItem)
	{
	  String label = ((MenuItem)e.getSource()).getLabel();
	  for (int i = 0; i < windowList.size() ; i++)
	    {
	      if (((JInternalFrame)windowList.elementAt(i)).getTitle().equals(label))
		{
		  ((JInternalFrame)windowList.elementAt(i)).moveToFront();
		  break;
		}

	    }

	}
    }

  public  void frameDidClose(InternalFrameEvent e)
    {
      System.out.println("frameDidClose - removing " + e.getInternalFrame().getTitle());
      
      windowList.removeElement(e.getInternalFrame());
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
       System.out.println("Adding a line to a Panel");
      JLabel l = new JLabel(label);
      
      parent.add("0 " + row + " lhwHW", l);
      parent.add("1 " + row + " lhwHW", comp);
      
    }

  void addRow(JPanel parent, Component comp,  String label, int row)
    {
       System.out.println("Adding a line to a JPanel");
      JLabel l = new JLabel(label);
      
      parent.add("0 " + row + " lhwHW", l);
      parent.add("1 " + row + " lhwHW", comp);
      
    }

 void addRow(JComponent parent, Component comp,  String label, int row)
    {
      
      JLabel l = new JLabel(label);
      
      parent.add("0 " + row + " lhwHW", l);
      parent.add("1 " + row + " lhwHW", comp);
      
    }
  

}


