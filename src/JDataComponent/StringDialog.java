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

/**
 * This class represents a customizable dialog.  
 *
 * <p>For simple dialogs, use the included
 * constructors.  For more complicated dialogs, including check boxes, choice lists, and text
 * fields, use a DialogRsrc. </p>
 *
 * <p>The ShowDialog method shows the current dialog, and returns a Hashtable of results.</p>
 *
 * @see DialogRsrc
 */
public class StringDialog extends JDialog implements ActionListener, JsetValueCallback, ItemListener {

  static final boolean debug = false;

  // --

  Hashtable
    componentHash,
    valueHash;

  boolean
    done;

  JLabel
    imageCanvas;

  JComponent
    firstComp;   //This is the first component in the dialog.  It will request the focus.

  JButton 
    OKButton,
    CancelButton;

  JPanel 
    panel,
    mainPanel,
    dataPanel,
    buttonPanel;

  JMultiLineLabel
    textLabel;

  TableLayout table;
  Image image;

  Vector objects;

  Vector
    components;

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
    this(new DialogRsrc(frame, Title, Text, OK, Cancel, (Image) null));
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

    if (debug)
      {
	System.err.println("StringDialog constructor");
      }
      
    componentHash = new Hashtable();
    valueHash = new Hashtable();

    objects = Resource.getObjects();
    components = new Vector(objects.size());

    mainPanel = new JPanel();
    mainPanel.setLayout(new BorderLayout());

    dataPanel = new JPanel(new BorderLayout());
    dataPanel.setBorder(BorderFactory.createEmptyBorder(0,5,0,5));
    mainPanel.add("Center", dataPanel);

    // Set up the text box at the top

    JLabel titleLabel = new JLabel(Resource.title, SwingConstants.CENTER);
    EmptyBorder eb5 = (EmptyBorder)BorderFactory.createEmptyBorder(5,5,5,5);
    titleLabel.setBorder(eb5);
    titleLabel.setFont(new Font("Helvetica", Font.BOLD, 14));
    titleLabel.setOpaque(true);
    JPanel tPanel = new JPanel();
    tPanel.add(titleLabel);
    mainPanel.add("North", tPanel);

    textLabel = new JMultiLineLabel(Resource.getText());
    //textLabel = new JLabel(Resource.getText());
    textLabel.setBorder(eb5);
    dataPanel.add("North", textLabel);

    image = Resource.getImage();

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
    JPanel southPanel = new JPanel(new BorderLayout());
    southPanel.setBorder(BorderFactory.createEmptyBorder(5,3,0,3));
    southPanel.add("Center", buttonPanel);
    southPanel.add("North", new arlut.csd.JDataComponent.JSeparator());

    mainPanel.add("South", southPanel);

    if (image != null)
      {
	//	System.out.println("add image");
	imageCanvas = new JLabel(new ImageIcon(image));
	JPanel imagePanel = new JPanel();
	imagePanel.add(imageCanvas);
	mainPanel.add("West", imagePanel);
      }

    this.setSize(600, 600);

    mainPanel.setBorder(new EtchedBorder());

    getContentPane().add(mainPanel);

    if (debug)
      {
	System.err.println("StringDialog: adding objects");
      }

    // add stuff to panel here

    if (objects != null)
      {
	if (debug)
	  {
	    System.out.println("objects != null");
	  }

	int numberOfObjects = objects.size();
	
	if (numberOfObjects > 0) 
	  {
	    if (debug)
	      {
		System.out.println("objects.size() > 0"); 
	      }

	    panel = new JInsetPanel();
	    table = new TableLayout(false);
	    panel.setLayout(table);
	    table.rowSpacing(10);
	      
	    dataPanel.add("Center", panel); 
	      
	    for (int i = 0; i < numberOfObjects; ++i) 
	      {
		Object element = objects.elementAt(i);

		if (debug)
		  {
		    System.out.println("number: " + numberOfObjects + " current: " + i);
		  }

		if (element instanceof stringThing)
		  {
		    if (debug)
		      {
			System.out.println("Adding string field(JstringField)");
		      }
		    
		    stringThing st = (stringThing)element;
		    JstringField sf = new JstringField();
		    sf.setText(st.getValue());
		    sf.setEditable(true);
		    sf.setCallback(this); 

		    if (i == (numberOfObjects - 1)) // This is the last object
		      {
			sf.addActionListener(new ActionListener() {
			  public void actionPerformed(ActionEvent e)
			    {
			      OKButton.doClick();
			    }
			});

		      }

		    addRow(panel, sf, st.getLabel(), i);
		    
		    componentHash.put(sf, st.getLabel());
		    valueHash.put(st.getLabel(), "");
		      
		  }
		else if (element instanceof passwordThing)
		  {
		    if (debug)
		      {
			System.out.println("Adding password field(JpasswordField)");
		      }

		    passwordThing pt = (passwordThing)element;
		    if (pt.isNew())
		      {
			if (debug)
			  {
			    System.out.println("This password is new.");
			  }

			JpassField sf = new JpassField(null, true, 10,100,true);
			
			sf.setCallback(this); 
			addRow(panel, sf, pt.getLabel(), i);
			
			componentHash.put(sf, pt.getLabel());
		      }
		    else
		      {
			if (debug)
			  {
			    System.out.println("This password is not new.");
			  }

			JpasswordField sf = new JpasswordField();
			sf.setEditable(true);

			sf.setCallback(this); 

			if (i == (numberOfObjects - 1)) // This is the last object
			  {
			    if (debug)
			      {
				System.out.println("Adding action listener.");
			      }
			    sf.addActionListener(new ActionListener() {
			      public void actionPerformed(ActionEvent e)
				{
				  OKButton.doClick();
				}
			    });
			    
			  }
			
			addRow(panel, sf, pt.getLabel(), i);
			
			componentHash.put(sf, pt.getLabel());
		      }

		    valueHash.put(pt.getLabel(), "");
		    
		  }
		else if (element instanceof booleanThing)
		  {
		    if (debug)
		      {
			System.out.println("Adding boolean field (JcheckboxField)");
		      }

		    booleanThing bt = (booleanThing)element;
		    JCheckBox cb = new JCheckBox();
		    cb.setSelected(bt.getValue());
		    cb.addItemListener(this);

		    addRow(panel, cb, bt.getLabel(), i);
		      
		    componentHash.put(cb, bt.getLabel());
		    valueHash.put(bt.getLabel(), new Boolean(bt.getValue()));
		  }
		else if (element instanceof choiceThing)
		  {
		    if (debug)
		      {
			System.out.println("Adding choice field (JComboBox)");
		      }

		    choiceThing ct = (choiceThing)element;
		    JComboBox ch = new JComboBox();

		    if (debug)
		      {
			System.out.println("Getting choice lists");
		      }

		    Vector items = ct.getItems();

		    if (debug)
		      {
			System.out.println("Got choice lists");
		      }

		    if (items == null)
		      {
			if (debug)
			  {
			    System.out.println("Nothing to add to Choice, empty vector");
			  }
		      }
		    else
		      {
			int total = items.size();

			for (int j = 0; j < total ; ++j)
			  {
			    //String str = (String)items.elementAt(j);
			    ch.addItem(items.elementAt(j));
			  }

			if (ct.getSelectedItem() != null)
			  {
			    ch.setSelectedItem(ct.getSelectedItem());
			  }

			ch.addItemListener(this);
			addRow(panel, ch, ct.getLabel(), i);

			componentHash.put(ch, ct.getLabel());
			if (ch.getSelectedItem() != null)
			  {
			    valueHash.put(ct.getLabel(), ch.getSelectedItem());
			  }
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

	    if (debug)
	      {
		System.err.println("Created components, registering callbacks.");
	      }

	    for (int i = 0; i < components.size(); i++)
	      {
		JComponent c = (JComponent)components.elementAt(i);
		if (debug)
		  {
		    System.out.println("Checking component: " + c);
		  }

		if (c instanceof JstringField)
		  {
		    JstringField sf = (JstringField) c;
		    if (i == components.size() -1) // last one!
		      {
			sf.addActionListener(
					     new ActionListener()
					     {
					       public void actionPerformed(ActionEvent e) {
						 OKButton.doClick();
					       }
					     });
		      }
		    else
		      {
			sf.addActionListener(
					     new ActionListener()
					     {
					       public void actionPerformed(ActionEvent e) {
						 JComponent thisComp = (JComponent)e.getSource();
						 
						 ((JComponent)components.elementAt(components.indexOf(thisComp) + 1)).requestFocus();
					       }
					     });
		      }
		    
		    if (i == 0) // first object
		      {
			firstComp = sf;
		      }
		    
		    
		  }
		else if (c instanceof JpasswordField)
		  {
		    if (debug)
		      {
			System.out.println("This is a JpasswordField, number " + i);
		      }

		    JpasswordField pf = (JpasswordField) c;
		    if (i == components.size() -1)
		      {
			pf.addActionListener(new ActionListener()
					     {
					       public void actionPerformed(ActionEvent e) {
						 OKButton.doClick();
					       }
					     });
		      }
		    else
		      {
			pf.addActionListener(new ActionListener()
					     {
					       public void actionPerformed(ActionEvent e) {
						 JComponent thisComp = (JComponent)e.getSource();
						 
						 ((JComponent)components.elementAt(components.indexOf(thisComp) + 1)).requestFocus();
					       }
					     });
		      }
		    if (i == 0) // first object
		      {
			firstComp = pf;
		      }
		  }
		else if (c instanceof JComboBox)
		  {
		    if (i == 0)
		      {
			firstComp = c;
		      }
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

    if (Resource.frame != null)
      {
	Rectangle r = Resource.frame.getBounds();
	
	if (debug)
	  {
	    System.out.println("Bounds: " + r);
	  }
	
	if ((r != null) && ((r.width != 0) && (r.height != 0)))
	  {
	    // Sometimes a new JFrame() is passed in, and it won't have anything interesting for bounds
	    // I don't think they are null, but they are all 0 or something.  Might as well make sure they are not
	    // null anyway.
	    int width = getPreferredSize().width;
	    int height = getPreferredSize().height;
	    
	    setLocation(r.width/2 - r.x - width/2, r.height/2 - r.y - height/2);
	    if (debug)
	      {
		int loc = r.width/2 - r.x - width/2;
		int locy = r.height/2 - r.y - height/2;
		System.out.println("Setting location to : " + loc + "," + locy);
	      }
	  }
	else if (debug)
	  {
	    System.out.println("getBounds() returned null.");
	  }
      }
    else if (debug)
      {
	System.out.println("Parent frame is null.");
      }

    
    addSpace(panel, 10, components.size() + 1);

  }


  /**
   * Display the dialog box.
   *
   * Use this instead of Dialog.show().  If Hashtable returned is null,
   * then the cancel button was clicked.  Otherwise, it will contain a 
   * hash of labels(String) to results (Object).
   *
   * @return HashTable of labels to values
   */

  public Hashtable DialogShow()
  {
    /*
    SwingUtilities.invokeLater(new Runnable() 
			       {
				 public void run()
				   {
				     firstComp.requestFocus();
				   }
			       });
    */

    pack();

    repaint();
    //    show();

    setVisible(true);		// thread will halt here
    //show();

    if (debug)
      {
	System.out.println("Done invoking.");
      }

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

    if (obj instanceof JComboBox)
      {
	String label = (String)componentHash.get(obj);
	JComboBox ch = (JComboBox) obj;
	if (valueHash != null)
	  {
	    valueHash.put(label, ch.getSelectedItem());
	  }
	
	if (debug)
	  {
	    System.out.println(ch.getSelectedItem() + " chosen");
	  }
      }
    else if (obj instanceof JCheckBox)
      {
	String label = (String)componentHash.get(obj);

	if (label == null)
	  {
	    System.out.println("in setValuePerformed from JcheckboxField: label = null");
	    return;
	  }

	JCheckBox cb = (JCheckBox)obj;
	Boolean answer = new Boolean(cb.isSelected());
	if (valueHash != null)
	  {
	    valueHash.put(label, answer);
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
	if (valueHash != null)
	  {
	    valueHash.put(label, sf.getText());
	  }


	if (debug)
	  {
	    System.out.println("Setting " + label + " to " + sf.getText());
	  }
      }
    else if (comp instanceof JpasswordField)
      {
	String label = (String)componentHash.get(comp);
	JpasswordField sf = (JpasswordField)comp;

	if (debug)
	  {
	    System.out.println("label is " + label);
	  }
	
	if (valueHash != null)
	  {
	    valueHash.put(label, sf.getText());
	  }

	if (debug)
	  {
	    System.out.println("Setting " + label + " to " + sf.getText());
	  }
      }
    else if (comp instanceof JpassField)
      {
	String label = (String)componentHash.get(comp);
	JpassField sf = (JpassField)comp;

	if (debug)
	  {
	    System.out.println("label is " + label);
	  }
	
	if (valueHash != null)
	  {
	    valueHash.put(label, (String)v.getValue());
	  }

	if (debug)
	  {
	    System.out.println("Setting " + label + " to " + (String)v.getValue());
	  }
      }
    return true;
  }

  private final void addRow(JPanel parent, JComponent comp,  String label, int row)
  {
    components.addElement(comp);

    JLabel l = new JLabel(label);
    
    parent.add("0 " + row + " rhwHW", l);
    parent.add("1 " + row + " lhH", comp);
    parent.invalidate();
  }

  /**
   * Activator cuts off the bottom 10 pixels or so, so add some space to make up for it.
   */
  private final void addSpace(JPanel parent, int space, int row)
  {
    parent.add("0 " + row + " rhH", Box.createVerticalStrut(space));
  }

  private final void addSeparator(JPanel parent, Component comp, int row)
  {
    parent.add("0 " + row + " 2 1 hH", comp);
  }
  
} // JStringDialog


