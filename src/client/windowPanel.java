/*
  The window that holds the frames in the client.

*/

package arlut.csd.ganymede.client;

import tablelayout.*;
import com.sun.java.swing.*;

import java.awt.*;
import java.awt.event.*;
import java.rmi.*;

import arlut.csd.ganymede.*;
import arlut.csd.ganymede.client.*;

import arlut.csd.JDataComponent.*;

public class windowPanel extends JPanel implements ActionListener, InternalFrameListener, JsetValueCallback{

  JLayeredPane lc;
  int topLayer = 0;
  JTitledPane panel;

  public windowPanel()
    {
      System.out.println("Initializing windowPanel");
      setLayout(new BorderLayout());
      lc = new JLayeredPane();

      add("Center", lc);


    }

  public void makeNewWindow(db_object object)
    {
      this.makeNewWindow(object, false);
    }

  public void makeNewWindow(db_object object, boolean editable)
    {
      System.out.println("Adding new internalFrame");
      JInternalFrame w = new JInternalFrame();
      w.setMaxable(true);
      w.setTitle("This is the title");
      w.setResizable(true);      
      w.addFrameListener(this);
      w.setLayout(new BorderLayout());

      //      OpaqueJPanel panel = new OpaqueJPanel();
      panel = new JTitledPane();
      panel.setLayout(new TableLayout());

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
      System.out.println(" number of fields: " + fields.length);
      if ((fields != null) && (fields.length > 0))
	{
	  try
	    {
	      for (int i = 0; i < fields.length ; i++)
		{
		  System.out.println("Adding " + fields[i].getName());
		  //JstringField asdf = new JstringField();
		  JTextField asdf = new JTextField();

		  //asdf.setCallback(this);
		  
		  //asdf.setEditable(true);
		  //asdf.setText(fields[i].getTypeDesc());
		  /*if (fields[i].getTypeDesc().equals("string"))
		    {
		      //asdf.setText((String)fields[i].getValue());
		      asdf.setText("Heya");
		    }
		  else
		    {
		      asdf.setText(fields[i].getTypeDesc());
		    }
		    */
		  addRow(panel, asdf, fields[i].getName(), i);
		}
	    }
	  catch (RemoteException rx)
	    {
	      throw new RuntimeException("Could not get field info: " + rx);
	    }
	}

      //w.setContentView(panel);
      w.add(panel);
      //w.setBounds(20,20, panel.getPreferredSize().width, panel.getPreferredSize().height);
      w.setBounds(5,5,300,300);
      w.setLayer(topLayer);
      //lc.setLayer(w, topLayer);
      System.out.println("Setting layer to " + topLayer);

      lc.add(w);
      lc.repaint();
    }

  // Event handlers
  public boolean setValuePerformed(JValueObject v)
    {
      System.out.println("setValuePerformed");
      return true;
    }

  public void actionPerformed(ActionEvent e)
    {
      System.out.println("Action performed");
    }

  public  void frameDidClose(InternalFrameEvent e)
    {
      System.out.println("frameDidClose");
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
      /*      if (e.getInternalFrame().getLayer() < topLayer)
	{
	  e.getInternalFrame().setLayer(++topLayer);
	}
       */
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

  void addRow(OpaqueJPanel parent, Component comp,  String label, int row)
    {
      System.out.println("Adding a line to a OpaqueJPanel");
      JLabel l = new JLabel(label);
      
      parent.add("0 " + row + " lhwHW", l);
      parent.add("1 " + row + " lhwHW", comp);
      
    }

 void addRow(JComponent parent, Component comp,  String label, int row)
    {
      System.out.println("Adding a line to a JComponent");
      JLabel l = new JLabel(label);
      
      parent.add("0 " + row + " lhwHW", l);
      parent.add("1 " + row + " lhwHW", comp);
      
    }
  

}


