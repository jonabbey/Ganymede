/*
  StringDialog.java
  
  A configurable Dialog box

*/

package arlut.csd.Dialog;

import arlut.csd.DataComponent.*;

import tablelayout.*;

import java.awt.*;
import java.awt.event.*;
import arlut.csd.Dialog.*;
import java.util.*;


//import gjt.ButtonPanel;
import gjt.*;

public class StringDialog extends Dialog implements ActionListener, setValueCallback {

  Hashtable
    componentHash,
    valueHash;

  boolean
    done;

  Button OKButton;
  Button CancelButton;
  Panel panel;
  EtchedBorder panelBorder;
  EtchedBorder textBorder;
  EtchedBorder mainBorder;
  ButtonPanel buttonPanel;
  Panel mainPanel;
  //ActionListener listener;

  Vector objects;

  public StringDialog(DialogRsrc Resource) 
    {
      
      super(Resource.frame, Resource.title, true);
      
      componentHash = new Hashtable();
      valueHash = new Hashtable();

      objects = Resource.getObjects();

      Label textLabel = new Label(Resource.getText());
      textBorder = new EtchedBorder(textLabel, 2, 5);

      panel = new Panel();
      panel.setLayout(new TableLayout(false));

     
      
      //add stuff to panel here
      int numberOfObjects = objects.size();
      System.out.println("There are " + numberOfObjects + " things in objects");
      if (numberOfObjects > 0) 
	{
	  for(int i = 0; i < numberOfObjects; ++i) 
	    {
	      System.out.println("Dealing with object " + i);
	      Object element = objects.elementAt(i);
	      if (element instanceof stringThing)
		{
		  System.out.println("Adding a string thing to table");
		  stringThing st = (stringThing)element;
		  stringField sf = new stringField();
		  sf.setCallback(this); 
		  addRow(panel, sf, st.getLabel(), i);

		  componentHash.put(sf, st.getLabel());
		  valueHash.put(st.getLabel(), "");

		}
	      else if (element instanceof booleanThing)
		{
		  System.out.println("Adding a boolean thing to table");
		  booleanThing bt = (booleanThing)element;
		  checkboxField cb = new checkboxField();
		  cb.setCallback(this);
		  //cb.setState(bt.getDefault().booleanValue());
		  addRow(panel, cb, bt.getLabel(), i);

		  componentHash.put(cb, bt.getLabel());
		  valueHash.put(bt.getLabel(), bt.getDefault());

		}
	      else if (element instanceof choiceThing)
		{
		  System.out.println("Adding a choice thing to table");
		  choiceThing ct = (choiceThing)element;
		  Choice ch = new Choice();
		  //choice.setCallback(this);
		  //iterate through vector add use choice.add
		  Vector items = ct.getItems();
		  int total = items.size();
		  for (int j = 0; j < total ; ++j)
		    {
		      String str = (String)items.elementAt(j);
		      ch.add(str);

		    }
		  
		  addRow(panel, ch, ct.getLabel(), i);

		  componentHash.put(ch, ct.getLabel());
		  valueHash.put(ct.getLabel(), "");

		}
	      else if (element instanceof Separator)
		{
		  System.out.println("Adding a Separator");
		  Separator sep = (Separator)element;
		  //sep.setInsets(0, 10);
		  addSeparator(panel, sep, i);
		}
	      else
		{
		  System.out.println("Item " + i + " is of unknown type");
		}
	      
	    }

	}
      else 
	{
	  System.out.println("No objects to add to StringDialog");
	}

      buttonPanel = new ButtonPanel();
      OKButton = buttonPanel.add(Resource.OKText);
      CancelButton = buttonPanel.add(Resource.CancelText);
  
      OKButton.addActionListener(this);
      CancelButton.addActionListener(this);

      //EtchedBorder panelBorder = new EtchedBorder(panel, 2, 5);
      
      mainPanel = new Panel();

      //add it and pack it
      mainPanel.setLayout(new BorderLayout());
      mainPanel.add("North", textBorder);
      mainPanel.add("Center", panel); 
      mainPanel.add("South", buttonPanel);
      this.setSize(600, 600);

      mainBorder = new EtchedBorder(mainPanel, 3, 10);
      
      add(mainBorder);

      //Having problems with setting the prefered size of the 
      // table layout
      pack();
     
      
    }


  public void setVisible(boolean b)
    {
      super.setVisible(b);
    }


  public Hashtable DialogShow()
    {
      pack();
      show();
      this.dispose();

      System.out.println("Done showing Dialog");

      return valueHash;
    }


  public  void actionPerformed(ActionEvent e)
    {
      System.out.println("There was some action performed in StringDialog");
      if (e.getSource() == OKButton)
	{
	  System.out.println("OKButton clicked, returning Hashtable");

	}
      else
	{
	  System.out.println("CancelButton clicked, returning null Hashtable");


	  valueHash = null;
	}

      setVisible(false);
    }

  public boolean setValuePerformed(ValueObject v)
    {
      Component comp = v.getSource();

      System.out.println("Something changed");

      if (comp instanceof stringField)
	{
	  String label = (String)componentHash.get(comp);
	  stringField sf = (stringField)comp;
	  valueHash.put(label, sf.getText());
	}
      else if (comp instanceof checkboxField)
	{
	  String label = (String)componentHash.get(comp);
	  checkboxField cbf = (checkboxField)comp;
	  Boolean answer = new Boolean(cbf.getValue());
	  valueHash.put(label, answer);
	}
      return true;
    }

  void addRow(Panel parent, Component comp,  String label, int row)
    {
      Label l = new Label(label);
    
      parent.add("0 " + row + " rhwHW", l);
      parent.add("1 " + row + " lhH", comp);
      parent.invalidate();
    }

  void addSeparator(Panel parent, Component comp, int row)
    {
      parent.add("0 " + row + " 2 1 hH", comp);
    }
  
}//StringDialog


