/*
  StringDialog.java
  
  A configurable Dialog box

*/

package arlut.csd.JDialog;

import arlut.csd.JDataComponent.*;

import tablelayout.*;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import com.sun.java.swing.*;
import com.sun.java.swing.border.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                    StringDialog

------------------------------------------------------------------------------*/

public class StringDialog extends Dialog implements ActionListener, JsetValueCallback, ItemListener {

  static final boolean debug = false;

  // --

  Hashtable
    componentHash,
    valueHash;

  boolean
    done;

  JLabel
    imageCanvas;

  JButton 
    OKButton,
    CancelButton;

  JPanel 
    panel,
    textPanel,
    mainPanel,
    buttonPanel;

  JLabel
    textLabel;

  TableLayout table;
  Image image;

  Vector objects;

  public StringDialog(Frame frame, String Title, String Text, boolean ShowCancel)
  {
    this (frame, Title, Text, "Ok", ShowCancel ? "Cancel" : null, null);
  }

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

    // Set up the text box at the top

    textPanel = new JPanel();
    textPanel.setLayout(new BorderLayout());

    //textLabel = new JMultiLineLabel(Resource.getText());
    textLabel = new JLabel(Resource.getText());
    textPanel.add("Center", textLabel);
      
    image = Resource.getImage();

    if (image != null)
      {
	//	System.out.println("add image");
	imageCanvas = new JLabel(new ImageIcon(image));
	textPanel.add("West", imageCanvas);
      }

    buttonPanel = new JPanel();

    OKButton = new JButton(Resource.OKText);
    OKButton.addActionListener(this);
    buttonPanel.add(OKButton);

    // if cancel is null, don't put it on there

    if (Resource.CancelText != null)
      {
	CancelButton = new JButton(Resource.CancelText);
	CancelButton.addActionListener(this);
	buttonPanel.add(CancelButton);
      }

    mainPanel = new JPanel();

    mainPanel.setLayout(new BorderLayout());
    mainPanel.add("North", textPanel);
      
    mainPanel.add("South", buttonPanel);
    this.setSize(600, 600);

    mainPanel.setBorder(new EtchedBorder());

    add(mainPanel);

    // add stuff to panel here

    if (objects != null)
      {
	// System.out.println("objects != null");
	int numberOfObjects = objects.size();

	if (numberOfObjects > 0) 
	  {
	    // System.out.println("objects.size() > 0"); 
	    panel = new JInsetPanel();
	    table = new TableLayout(false);
	    panel.setLayout(table);
	    table.rowSpacing(10);
	      
	    mainPanel.add("Center", panel); 
	      
	    for (int i = 0; i < numberOfObjects; ++i) 
	      {
		Object element = objects.elementAt(i);

		if (element instanceof stringThing)
		  {
		    stringThing st = (stringThing)element;
		    JstringField sf = new JstringField();
		    sf.setEditable(true);
		    sf.setCallback(this); 
		    addRow(panel, sf, st.getLabel(), i);
		    
		    if (i == 0)
		      {
			sf.requestFocus();
		      }
  
		    componentHash.put(sf, st.getLabel());
		    valueHash.put(st.getLabel(), "");
		      
		  }
		else if (element instanceof passwordThing)
		  {
		    if (debug)
		      {
			System.out.println("Adding password field(JstringField)");
		      }

		    passwordThing pt = (passwordThing)element;
		    JpasswordField sf = new JpasswordField();
		    //		    sf.setEchoChar('*');
		    sf.setEditable(true);
		    sf.setCallback(this); 
		    addRow(panel, sf, pt.getLabel(), i);

		    if (i == 0)
		      {
			sf.requestFocus();
		      }  
		      
		    componentHash.put(sf, pt.getLabel());
		    valueHash.put(pt.getLabel(), "");
		  }
		else if (element instanceof booleanThing)
		  {
		    booleanThing bt = (booleanThing)element;
		    JcheckboxField cb = new JcheckboxField();
		    cb.setCallback(this);
		    cb.setSelected(bt.getDefault().booleanValue());
		    addRow(panel, cb, bt.getLabel(), i);
		      
		    if (i == 0)
		      {
			cb.requestFocus();
		      }
  
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

			if (i == 0)
			  {
			    ch.requestFocus();
			  }
  
			  
			componentHash.put(ch, ct.getLabel());
			valueHash.put(ct.getLabel(), (String)items.elementAt(0));
		      }
		  }
		//		else if (element instanceof Separator)
		//		  {
		//		    Separator sep = (Separator)element;
		//		    addSeparator(panel, sep, i);
		//		  }
		else
		  {
		    System.out.println("Item " + i + " is of unknown type");
		  }
	      }
	  }
	else if (debug)
	  {
	    System.out.println("No objects to add to StringDialog");
	  }
      }
    else if (debug)
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

    repaint();
    show();
      
    this.dispose();
    
    return valueHash;
  }

  public  void actionPerformed(ActionEvent e)
  {
    //    System.out.println("There was some action performed in StringDialog");

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
	
	if (debug)
	  {
	    System.out.println(ch.getSelectedItem() + " chosen");
	  }
      }
    else
      {
	System.out.println("Unknown item type generated action");
      }
  }

  public boolean setValuePerformed(JValueObject v)
  {
    Component comp = v.getSource();

    if (comp instanceof JstringField)
      {
	String label = (String)componentHash.get(comp);
	JstringField sf = (JstringField)comp;
	valueHash.put(label, sf.getText());

	if (debug)
	  {
	    System.out.println("Setting " + label + " to " + sf.getText());
	  }
      }
    else if (comp instanceof JpasswordField)
      {
	String label = (String)componentHash.get(comp);
	JpasswordField sf = (JpasswordField)comp;
	valueHash.put(label, sf.getText());

	if (debug)
	  {
	    System.out.println("Setting " + label + " to " + sf.getText());
	  }
      }
    else if (comp instanceof JcheckboxField)
      {
	String label = (String)componentHash.get(comp);
	JcheckboxField cbf = (JcheckboxField)comp;
	Boolean answer = new Boolean(cbf.getValue());
	valueHash.put(label, answer);
      }
    return true;
  }

  void addRow(JPanel parent, Component comp,  String label, int row)
  {
    Label l = new Label(label);
    
    parent.add("0 " + row + " rhwHW", l);
    parent.add("1 " + row + " lhH", comp);
    parent.invalidate();
  }

  void addSeparator(JPanel parent, Component comp, int row)
  {
    parent.add("0 " + row + " 2 1 hH", comp);
  }
  
} // JStringDialog


