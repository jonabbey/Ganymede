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

public class windowPanel extends JPanel implements ActionListener{

  JLayeredPane lc;
  

  public windowPanel()
    {
      System.out.println("Initializing windowPanel");
      setLayout(new BorderLayout());
      lc = new JLayeredPane();

      Label test = new Label("There should be a window under this!");
      add("North", test);
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

      Panel panel = new Panel();
      panel.setLayout(new TableLayout());

      addRow(panel, new Label("test"), "this is a", 0);

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
		  Label asdf = new Label(fields[i].getTypeDesc());
		  addRow(panel, asdf, fields[i].getName(), i+1);
		}
	    }
	  catch (RemoteException rx)
	    {
	      throw new RuntimeException("Could not get field info: " + rx);
	    }
	}
      w.add(panel);
      w.add(new Label("adding this lable"));
      //w.setBounds(20,20, panel.getPreferredSize().width, panel.getPreferredSize().height);
      w.setBounds(5,5,300,300);
      w.setLayer(0);
      lc.add(w);
    }

  // Event handlers
  public void actionPerformed(ActionEvent e)
    {
      System.out.println("Action performed");
    }

  // Convenience methods
  void addRow(Panel parent, Component comp,  String label, int row)
    {
      Label l = new Label(label);
      
      parent.add("0 " + row + " lhwHW", l);
      parent.add("1 " + row + " lhwHW", comp);
      
    }



}
