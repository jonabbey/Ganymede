/*
  StringDialog.java
  
  A configurable Dialog box

*/

package arlut.csd.Dialog;

import arlut.csd.DataComponent.*;
import arlut.csd.Dialog.*;

import tablelayout.*;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import oreilly.Label.*;


//import gjt.ButtonPanel;
import gjt.*;

public class StringDialog extends Dialog implements ActionListener, setValueCallback, ItemListener {

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

      MultiLineLabel textLabel = new MultiLineLabel(Resource.getText());
      textBorder = new EtchedBorder(textLabel, 2, 5);

      panel = new InsetPanel();
      TableLayout table = new TableLayout(false);
      panel.setLayout(table);
      table.rowSpacing(10);
     
      
      //add stuff to panel here
      int numberOfObjects = objects.size();
      if (numberOfObjects > 0) 
	{
	  for(int i = 0; i < numberOfObjects; ++i) 
	    {
	      Object element = objects.elementAt(i);
	      if (element instanceof stringThing)
		{
		  stringThing st = (stringThing)element;
		  stringField sf = new stringField();
		  sf.setEditable(true);
		  sf.setCallback(this); 
		  addRow(panel, sf, st.getLabel(), i);

		  componentHash.put(sf, st.getLabel());
		  valueHash.put(st.getLabel(), "");

		}
	      else if (element instanceof booleanThing)
		{
		  booleanThing bt = (booleanThing)element;
		  checkboxField cb = new checkboxField();
		  cb.setCallback(this);
		  cb.setState(bt.getDefault().booleanValue());
		  addRow(panel, cb, bt.getLabel(), i);

		  componentHash.put(cb, bt.getLabel());
		  valueHash.put(bt.getLabel(), bt.getDefault());

		}
	      else if (element instanceof choiceThing)
		{
		  choiceThing ct = (choiceThing)element;
		  Choice ch = new Choice();
		  Vector items = ct.getItems();
		  if (items == null)
		    {
		      System.out.println("Nothing to add to Choice, empty vector");
		    }
		  else
		    {
		      int total = items.size();
		      for (int j = 0; j < total ; ++j)
			{
			  String str = (String)items.elementAt(j);
			  ch.add(str);
			  
			}
		      ch.addItemListener(this);
		      addRow(panel, ch, ct.getLabel(), i);
		      
		      componentHash.put(ch, ct.getLabel());
		      valueHash.put(ct.getLabel(), (String)items.elementAt(0));
		    }
		}
	      else if (element instanceof Separator)
		{
		  Separator sep = (Separator)element;
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

      return valueHash;
    }


  public  void actionPerformed(ActionEvent e)
    {
      System.out.println("There was some action performed in StringDialog");
      if (e.getSource() == OKButton)
	{
	  //System.out.println("OKButton clicked, returning Hashtable");

	}
      else
	{
	  //System.out.println("CancelButton clicked, returning null Hashtable");


	  valueHash = null;
	}

      setVisible(false);
    }

  public void itemStateChanged(ItemEvent e)
    {
      Object obj = e.getSource();

      

      if (obj instanceof Choice)
	{
	  String label = (String)componentHash.get(obj);
	  Choice ch = (Choice)obj;
	  valueHash.put(label, ch.getSelectedItem());
	  System.out.println(ch.getSelectedItem() + " chosen");
	}
      else
	{
	  System.out.println("Unknown item type generated action");
	}

    }

  public boolean setValuePerformed(ValueObject v)
    {
      Component comp = v.getSource();

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


