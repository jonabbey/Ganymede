/*
  StringDialog.java
  
  A configurable Dialog box

*/

package arlut.csd.JDialog;

import arlut.csd.JDataComponent.*;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.border.*;

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
public class StringDialog extends JCenterDialog implements ActionListener, JsetValueCallback, ItemListener {

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

  GridBagLayout
    gbl,
    compgbl;

  GridBagConstraints
    gbc,
    compgbc;
  
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

    gbl = new GridBagLayout();
    gbc = new GridBagConstraints();

    gbc.insets = new Insets(4,4,4,4);

    mainPanel = new JPanel();
    mainPanel.setBorder(new EtchedBorder());
    mainPanel.setLayout(gbl);
    setContentPane(mainPanel);

    //
    // Title at top of dialog
    //

    JLabel titleLabel = new JLabel(Resource.title, SwingConstants.CENTER);
    titleLabel.setFont(new Font("Helvetica", Font.BOLD, 14));

    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.gridwidth = 2;
    gbc.gridheight = 1;
    gbl.setConstraints(titleLabel, gbc);
    mainPanel.add(titleLabel);

    //
    // Text message under title
    //

    textLabel = new JMultiLineLabel(Resource.getText());
    
    gbc.gridy = 1;
    gbc.gridx = 1;
    gbc.gridwidth = 1;
    gbc.gridheight = 1;
    gbl.setConstraints(textLabel, gbc);
    mainPanel.add(textLabel);

    //
    // ButtonPanel takes up the bottom of the dialog
    //

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

    //
    // Separator goes all the way accross
    // 

    arlut.csd.JDataComponent.JSeparator sep = new     arlut.csd.JDataComponent.JSeparator();

    gbc.gridx = 0;
    gbc.gridy = 3;
    gbc.gridwidth = 2;
    gbc.gridheight = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbl.setConstraints(sep, gbc);
    mainPanel.add(sep);

    //
    // buttonPanel goes underneath
    //

    gbc.gridx = 0;
    gbc.gridy = 4;
    gbc.gridwidth = 2;
    gbc.gridheight = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbl.setConstraints(buttonPanel, gbc);
    mainPanel.add(buttonPanel);

    //
    // Image on left hand side
    //

    image = Resource.getImage();
    JPanel imagePanel = new JPanel();

    if (image != null)
      {
	imageCanvas = new JLabel(new ImageIcon(image));
	imagePanel.add(imageCanvas);
      }
    else
      {
	imagePanel.add(Box.createGlue());
      }

    gbc.gridx = 0;
    gbc.gridy = 1;
    gbc.gridwidth = 1;
    gbc.gridheight = 2;
    gbl.setConstraints(imagePanel, gbc);
    mainPanel.add(imagePanel);


    if (debug)
      {
	System.err.println("StringDialog: adding objects");
      }

    // We have to make the panel, even if it is empty.  This is
    // because we have to add space to to it for the stupid activator.
    panel = new JPanel();

    gbc.gridx = 1;
    gbc.gridy = 2;
    gbc.gridwidth = 1;
    gbc.gridheight = 1;
    JScrollPane scroller = new JScrollPane(panel);
    scroller.setBorder(null);
    gbl.setConstraints(scroller, gbc);
    mainPanel.add(scroller);

    // The panel uses its own grid bag stuff
    compgbc = new GridBagConstraints();
    compgbl = new GridBagLayout();

    compgbc.insets = new Insets(0,4,0,4);
    panel.setLayout(compgbl);
    



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
    
    pack();
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

    pack();
    show();

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

    compgbc.gridwidth = 1;
    compgbc.fill = GridBagConstraints.NONE;
    compgbc.anchor = GridBagConstraints.WEST;
    
    compgbc.gridy = row;
    compgbc.gridx = 0;
    compgbc.weightx = 0.0;

    JLabel l = new JLabel(label, SwingConstants.LEFT);

    compgbl.setConstraints(l, compgbc);
    compgbc.gridwidth = GridBagConstraints.REMAINDER;
    parent.add(l);

    compgbc.gridx = 1;
    compgbc.weightx = 1.0;

    compgbl.setConstraints(comp, compgbc);
    parent.add(comp);

    //parent.add("0 " + row + " rhwHW", l);
    //parent.add("1 " + row + " lhH", comp);

    parent.invalidate();
  }

  /**
   * Activator cuts off the bottom 10 pixels or so, so add some space to make up for it.
   */
  private final void addSpace(JPanel parent, int space, int row)
  {
    // Do nothing.  See nothing.  Hear nothing.
    //parent.add("0 " + row + " rhH", Box.createVerticalStrut(space));
  }

  private final void addSeparator(JPanel parent, Component comp, int row)
  {
    compgbc.gridy = row;
    compgbc.gridx = 1;
    compgbc.gridwidth = GridBagConstraints.REMAINDER;
    compgbc.fill = GridBagConstraints.HORIZONTAL;

    compgbl.setConstraints(parent, gbc);
    parent.add("0 " + row + " 2 1 hH", comp);
  }
  
} // JStringDialog


