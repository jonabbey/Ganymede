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

import gjt.ImageCanvas;


//import gjt.ButtonPanel;
import gjt.*;

public class StringDialog extends Dialog implements ActionListener, setValueCallback, ItemListener {

  Hashtable
    componentHash,
    valueHash;

  boolean
    done;

  ImageCanvas
    imageCanvas;

  Button OKButton;
  Button CancelButton;
  Panel panel;
  EtchedBorder panelBorder;
  EtchedBorder textBorder;
  EtchedBorder mainBorder;
  ButtonPanel buttonPanel;
  Panel mainPanel;
  TableLayout table;
  Image image;

  Vector objects;

  /**
   * Simple constructor for a small dialog box
   *
   * @param frame Parent frame of the Dialog Box
   * @param Title Title of the Dialog Box
   * @param Text Text shown in the Dialog Box
   *
   */
  public StringDialog(Frame frame, String Title, String Text)
    {
      this(frame, Title, Text, "Ok", "Cancel", null);
    }

 /**
   * Simple constructor for a small dialog box
   *
   * @param frame Parent frame of the Dialog Box
   * @param Title Title of the Dialog Box
   * @param Text Text shown in the Dialog Box
   * @param OK String for "OK" button
   * @param Cancel String for "Cancel" button
   *
   */

  public StringDialog(Frame frame, String Title, String Text, String OK, String Cancel)
    {
      this(new DialogRsrc(frame, Title, Text, OK, Cancel, null));
    }
  

 /**
   * Simple constructor for a small dialog box
   *
   * @param frame Parent frame of the Dialog Box
   * @param Title Title of the Dialog Box
   * @param Text Text shown in the Dialog Box
   * @param OK String for "OK" button
   * @param Cancel String for "Cancel" button
   * @param image Image to display next to text
   */
  public StringDialog(Frame frame, String Title, String Text, String OK, String Cancel, Image image)
    {
      this(new DialogRsrc(frame, Title, Text, OK, Cancel, image));
    }

  /**
   * Constructor for more complicated StringDialog.
   *
   *@param Resource Sets resource for Dialog box.
   */
  public StringDialog(DialogRsrc Resource) 
    {
      
      super(Resource.frame, Resource.title, true);
      
      componentHash = new Hashtable();
      valueHash = new Hashtable();

      objects = Resource.getObjects();


      //Set up the text box at the top
      Panel textPanel = new Panel();
      textPanel.setLayout(new BorderLayout());
      MultiLineLabel textLabel = new MultiLineLabel(Resource.getText());
      
      textPanel.add("Center", textLabel);
      
      textBorder = new EtchedBorder(textPanel, 2, 5);
      
      image = Resource.getImage();
      if (image != null)
	{
	  System.out.println("add image");
	  imageCanvas = new ImageCanvas(image);
	  textPanel.add("West", imageCanvas);
	}

      buttonPanel = new ButtonPanel();
      OKButton = buttonPanel.add(Resource.OKText);
      OKButton.addActionListener(this);

      //if cancel is null, don't put it on there
      if (Resource.CancelText != null)
	{
	  CancelButton = buttonPanel.add(Resource.CancelText);
	  CancelButton.addActionListener(this);
	}

      OKButton.addActionListener(this);


      //EtchedBorder panelBorder = new EtchedBorder(panel, 2, 5);
      
      mainPanel = new Panel();

      
      mainPanel.setLayout(new BorderLayout());
      mainPanel.add("North", textBorder);
      
      mainPanel.add("South", buttonPanel);
      this.setSize(600, 600);

      mainBorder = new EtchedBorder(mainPanel, 3, 10);
      
      add(mainBorder);


      //add stuff to panel here
      if (objects != null)
	{
	  System.out.println("objects != null");
	  
	  int numberOfObjects = objects.size();
	  if (numberOfObjects > 0) 
	    {
	      System.out.println("objects.size() > 0"); 
	      panel = new InsetPanel();
	      table = new TableLayout(false);
	      panel.setLayout(table);
	      table.rowSpacing(10);
	      
	      mainPanel.add("Center", panel); 
	      
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
		  else if (element instanceof passwordThing)
		    {
		      passwordThing pt = (passwordThing)element;
		      stringField sf = new stringField();
		      sf.setEchoChar('*');
		      sf.setEditable(true);
		      sf.setCallback(this); 
		      addRow(panel, sf, pt.getLabel(), i);
		      
		      componentHash.put(sf, pt.getLabel());
		      valueHash.put(pt.getLabel(), "");


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
	}
      else
	{
	  System.out.println("null objects vector");
	}

    

      //Having problems with setting the prefered size of the 
      // table layout
      pack();
     
      
    }


  public void setVisible(boolean b)
    {
      super.setVisible(b);
    }

  /**
   * Display the dialog box.
   *
   * Use this instead of Dialog.show();
   * @returns valueHash HashTable of labels to values
   */

  public Hashtable DialogShow()
    {
      pack();
      if (image != null)
	{
	  imageCanvas.setSize(image.getHeight(null), image.getWidth(null));
	}
      repaint();
      show();
      
      this.dispose();

      return valueHash;
    }
  /*
  public void paint(Graphics g)
    {
      System.out.println("Painting");
      if (image != null)
	{
	  System.out.println("image != null");
	  imageCanvas.setSize(image.getHeight(null), image.getWidth(null));
	  Graphics ig = imageCanvas.getGraphics();
	  ig.drawImage(image, 0, 0, null);
	}
      else
	{
	  System.out.println("image == null");
	}
    }

  public void update(Graphics g)
    {
      paint(g);
    }
    */
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


