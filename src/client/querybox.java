/*

   querybox

   Description.
   
   Created: 23 July 1997
   Version: $Revision: 1.22 $ %D%
   Module By: Erik Grostic
              Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.client;

import arlut.csd.ganymede.*;
import arlut.csd.JDataComponent.*;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.rmi.RemoteException;

import tablelayout.*;
import arlut.csd.JDataComponent.*;

import com.sun.java.swing.*;
import com.sun.java.swing.border.*;

/*------------------------------------------------------------------------------
                                                                           class 
                                                                        querybox

------------------------------------------------------------------------------*/

/**
 *
 * This class implements a modal dialog that is popped up to generate
 * a Query object that will be used by the rest of the ganymede.client
 * package to submit the query to the server for handling.
 *
 * Once an instance of querybox is constructed, the client code will
 * call myShow() to pop up the dialog and retrieve the Query object.
 * 
 * If the user chooses not to submit a Query after all, myShow() will
 * return null.
 *  
 */

class querybox extends JDialog implements ActionListener, ItemListener {
  
  static final boolean debug = false;
  static final int MAXCOMPONENTS = 8; // the number of components that
                                      // can appear in a query choice
  // --

  JFrame optionsFrame = null;	// to hold the frame that we popup to get a list of
				// desired fields in the query's results

  Hashtable 
    baseHash,			// Key: Bases      *--*  Value: Fields  
    shortHash;			// Key: Base ID    *--*  Value: Corresponding Base
  
  Hashtable baseIDHash = new Hashtable();  
            // Key: Fieldname
            //Value: Short ID of corresponding base
  
  Hashtable fieldHash = new Hashtable();  
            // Key: Fieldname
            // Value: baseField

  Hashtable  nameHash = new Hashtable();   
            // Key: embedded fieldname (with slashes)
            // Value: field after last slash 

  Hashtable myHash = new Hashtable(); // allows you to look up a base with its
                                      // name as the key

  // - Buttons
  
  JButton OkButton = new JButton ("Submit");
  JButton CancelButton = new JButton("Cancel");
  JButton addButton = new JButton("Add Choices");
  JButton removeButton = new JButton("Remove Choices");
  JButton displayButton = new JButton("Options");
  JButton optionClose = new JButton("Close");

  //----------- more Buttons, this time used by the save/load menu

  JButton qSave = new JButton("Save");
  JButton qLoad = new JButton("Load");
  JButton qDone = new JButton("Done");
  JButton qCancel = new JButton("Cancel");
  JButton lCancel = new JButton("Cancel");
  JButton lRename = new JButton("Rename");
  JButton lSelect = new JButton("Select");

  //-----------


  // - Panels and Panes

  JPanel query_panel = new JPanel();

  JPanel base_panel = new JPanel();


  JPanel inner_choice = new JPanel();
  JPanel outer_choice = new JPanel();
 
  JScrollPane choice_pane = new JScrollPane();

  JPanel Choice_Buttons = new JPanel();
  JPanel query_Buttons = new JPanel();

  JCheckBox editBox = new JCheckBox("Editable");

  // - Choice menus
  
  qChoice baseChoice = new qChoice(); // there's only one of these
  qfieldChoice fieldChoice; // but there's liable to be a whole slew of these
  qaryChoice opChoice;
  qaryChoice intChoice;
  qaryChoice dateChoice;
  qaryChoice vectorChoice;
  qaryChoice isNot; // will either be a boolean choice, or a list of operators
                    // depending on whether the field is an array

  // - imput fields

  JTextField inputField = new JTextField(12);
  JTextField saveText;
  JTextField dateField;
  
  // - Vectors

  Vector fieldOptions = new Vector(); // keeps track of which fields will
                                      // be used in the query results 

  Vector Rows = new Vector(); // store the rows 

  Vector Embedded = new Vector(); // keep track of fields referring to 
                                  // embedded objects
  // - Booleans

  boolean 
      editOnly,
      booleanField;

  // - ints

  int row = 0;
  int readLength;  // for use in the reading of queries

  // - other variables

  Query returnVal;
  Base defaultBase;
  Component[] myAry = new Component[MAXCOMPONENTS]; // stores a single row
 
  // - Strings

  String
    currentBase,
    currentField,
    baseName;

  // -- read query stuff (strings)
  
    String
      queryString; // qeuryString is used in the loading of queries

  // - Dialog Boxes

  JDialog saveBox;
  JDialog loadBox;

  // our dialog's content pane

  private Container contentPane;

  /* -- */

  /**
   *
   * Primary constructor.
   *
   * @param defaultBase The object base that will be initially selected.
   *                    May be null.
   *
   * @param baseHash A hash mapping bases to a vector of field definitions,
   *                 to save a bunch of redundant calls to the server.
   *
   * @param shortHash A hash mapping short ID's to Base objects, used to
   *                  resolve base linking in embedded object hierarchies.
   *
   * @param parent The frame that this querybox is to be connected to.
   *
   * @param DialogTitle The title for this dialog.
   *
   */

  public querybox (Base defaultBase, Hashtable baseHash, Hashtable shortHash,
		   Frame parent, String DialogTitle)
  {
    super(parent, DialogTitle, true); // the boolean value is to make the dialog modal
      
    Vector Bases;
    
    /* -- */
    
    System.out.println("Hi! I'm your happy query friend!");

    contentPane = this.getContentPane();

    // Main constructor for the querybox window
    
    this.baseHash = baseHash;  
    this.shortHash = shortHash;
    try
      {
	// We have to go get a real Base reference to the server, through the
	// shortHash.  The "base" that we have here is actually a BaseDump, so
	// we can't call the getFields type methods on it.

	this.defaultBase = (Base)shortHash.get(new Short(defaultBase.getTypeID()));
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Exception loading base: " + rx);
      }
    
    // - Define the main window
    
    contentPane.setLayout(new BorderLayout());
    contentPane.setBackground(Color.white);
    
    OkButton.addActionListener(this);
    OkButton.setBackground(Color.lightGray);
    CancelButton.addActionListener(this);
    CancelButton.setBackground(Color.lightGray);
    Choice_Buttons.setLayout(new FlowLayout ());
    Choice_Buttons.add(OkButton);
    Choice_Buttons.add(CancelButton);
    contentPane.add("South", Choice_Buttons); 
    
    editBox.addItemListener(this);
    editBox.setSelected(false);
    this.editOnly = false;
      
    query_panel.setLayout(new BorderLayout());
    query_panel.setBackground(Color.lightGray); 
    contentPane.add("Center", query_panel); 
    
    // - Define the inner window with the query choice buttons
      
    addButton.addActionListener(this);
    addButton.setBackground(Color.lightGray);
    removeButton.addActionListener(this);
    removeButton.setBackground(Color.lightGray);
    query_Buttons.setLayout(new FlowLayout ());
    query_Buttons.add(addButton);
    query_Buttons.add(removeButton);
    query_panel.add("South", query_Buttons);  
    
    // - Define the two inner choice windows

    base_panel.setSize(100,100);
    base_panel.setLayout(new FlowLayout());
    base_panel.setBorder(new TitledBorder("Base and Field Menu"));
     
    // - Create the choice window containing the fields 

    Enumeration enum = baseHash.keys();
      
    try
      {
	while (enum.hasMoreElements())
	  {
	    Base key = (Base) enum.nextElement();

	    // we want to ignore embedded objects -- for now
	    
	    if (key.isEmbedded())
	      {
		// get a base that works...this embedded would cause
		// problems [null pointer exceptions, that kind of thing]
		
		continue;
	      }
	    else
	      {
		String choiceToAdd = new String(key.getName());

		baseChoice.addItem(choiceToAdd);
		myHash.put(choiceToAdd, key);
	      }
	  
	    if (defaultBase != null)
	      {
		baseChoice.setSelectedItem(defaultBase.getName());
		this.baseName = defaultBase.getName();
	      }
	    else 
	      { 
		// no default given. pick the one that's there.
	    
		currentBase = (String) baseChoice.getSelectedItem();
		this.defaultBase = (Base) myHash.get(currentBase);
		defaultBase = this.defaultBase;
		this.baseName = defaultBase.getName();
	      }
	  }
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("caught remote exception: " + ex);
      }
      
    displayButton.addActionListener(this);
    baseChoice.setBackground(Color.white);
    baseChoice.addItemListener(this);
    base_panel.add(baseChoice);
    base_panel.add(new JLabel("  "));
    base_panel.add(editBox);
    base_panel.add(new JLabel("  "));
    base_panel.add(displayButton);
    
    inner_choice.setLayout(new TableLayout(false));
    inner_choice.setBackground(Color.white);
    
    outer_choice.setLayout(new FlowLayout());
    outer_choice.setBackground(Color.white);
    outer_choice.add(inner_choice);

    choice_pane.setBorder(new TitledBorder("Query Fields"));
    choice_pane.setViewportView(outer_choice);

    query_panel.add("North", base_panel);
    query_panel.add("Center", choice_pane);
      
    addChoiceRow(defaultBase); // adds the initial row   
    
    this.pack();

  }

  /**
   *
   * Alternate Constructor. Used when no default query is provided 
   *
   * @param baseHash A hash mapping bases to a vector of field definitions,
   *                 to save a bunch of redundant calls to the server.
   *
   * @param shortHash A hash mapping short ID's to Base objects, used to
   *                  resolve base linking in embedded object hierarchies.
   *
   * @param parent The frame that this querybox is to be connected to.
   *
   * @param DialogTitle The title for this dialog.
   *
   */

  public querybox (Hashtable baseHash, Hashtable shortHash, 
                   Frame parent, String myTitle) 
  {
    this(null, baseHash, shortHash, parent, myTitle);
  } 

  ///////////////////////
  //   Public Methods  //
  ///////////////////////

  /**
   *
   * This is the main interface to the querybox, and is used to
   * synchronously display the querybox and return the Query
   * generated by it. 
   *
   */

  public Query myshow()
  {
    // Method to set the querybox to visible or invisible
    
    setSize(800,250);
    setVisible(true);		// our thread will wait at this point
    
    return this.returnVal; // once setVisible is set to false
                           // the program executes this return line
  }

  ////////////////////////
  //   Private Methods  //
  ////////////////////////

  /**
   *
   * This internal method is used to create a frame which will
   * present a matrix of checkboxes corresponding to the fields
   * available in the specified object base.  The user will
   * be able to select various checkboxes to control which fields
   * are to be returned by the query generated by this querybox.
   *
   */

  private JFrame createOptionFrame (Base base)
  {
    /* Method to return a choice menu containing the fields for
     * a particular base, along with the query saving options
     */
    
    JScrollPane option_pane = new JScrollPane();
    option_pane.setBorder(new TitledBorder("Return Options"));

    JPanel save_panel = new JPanel(); // holds query options
    save_panel.setBorder(new TitledBorder("Query Options"));

    JPanel option_panel = new JPanel();
    JPanel choice_option = new JPanel(); // basically holds the Close button
    JPanel contain_panel = new JPanel(); // Holds the boxes

    BaseField basefield;
    JFrame myFrame = new JFrame("Options");
    JCheckBox newCheck; 
    JPanel inner_panel = new JPanel();
    
    Vector tmpAry;
    
    /* -- */
      
    myFrame.setSize(500,300);
  
    optionClose.setBackground(Color.lightGray);
    optionClose.addActionListener(this);
    
    choice_option.setBackground(Color.white);
    choice_option.setLayout(new FlowLayout());
    choice_option.add(optionClose);
    
    contain_panel.setLayout(new BorderLayout());
    contain_panel.add("South", save_panel);
    contain_panel.add("Center", option_pane);
    
    option_panel.setLayout(new BorderLayout());
    option_panel.add("South",choice_option);
    option_panel.add("Center", contain_panel);
  
    option_pane.setViewportView(inner_panel);
    inner_panel.setLayout(new TableLayout());
    
    save_panel.setLayout(new FlowLayout());
    save_panel.add(qSave);
    save_panel.add(new JLabel("   "));
    save_panel.add(qLoad);

    qSave.addActionListener(this);
    qLoad.addActionListener(this);

    try
      {
	// We have to go get a real Base reference to the server, through the
	// shortHash.  The "base" that we have here is actually a BaseDump, so
	// we can't call the getFields type methods on it.
	Vector fields = ((Base)shortHash.get(new Short(base.getTypeID()))).getFields();
	tmpAry = new Vector();

	int count = 0;
	int tmpRow = 0;
	  
	for (int j=0; fields != null && (j < fields.size()); j++) 
	  {	
	    basefield = (BaseField) fields.elementAt(j);
	    String Name = basefield.getName();	   
	    newCheck = new JCheckBox(Name);
	    newCheck.setSelected(true);
	      
	    if (count <= 2) // we've got space in the current row)
	      {
		tmpAry.insertElementAt(newCheck, count);
		count ++;
	      }
	    else 
	      {
		// add the row to the panel
		  
		for (int n = 0; n < tmpAry.size(); n++)
		  {
		    inner_panel.add( n + "  " + tmpRow + " lhwHW", (Component) tmpAry.elementAt(n));
		  }
		  
		// Here we're saving the row of components in the fieldOptions vector
		 
		if (! tmpAry.isEmpty())
		  {
		    fieldOptions.insertElementAt(tmpAry, tmpRow);
		    tmpAry = new Vector();
		  }

		count = 0;
		tmpRow ++;
		tmpAry.removeAllElements(); // Clear out the other array elements
		tmpAry.insertElementAt(newCheck, count);
		count++;
	      }     
	  }
	  
	// Now add the final row
	
	for (int n = 0; n < tmpAry.size(); n++)
	  {
	    inner_panel.add( n + "  " + tmpRow + " lhwHW", (Component) tmpAry.elementAt(n));
	  }

	// Here we're saving the row of components in the fieldOptions vector
	// (again)
	
	fieldOptions.insertElementAt(tmpAry, tmpRow);
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("caught remote exception: " + ex);	
      }
            
    //option_pane.add(option_panel);
    myFrame.getContentPane().add(option_panel);
      
    // overkill?

    myFrame.invalidate();
    myFrame.validate();
    myFrame.repaint();

    return myFrame;  
  }

  /**
   *
   * A companion to the following getChoiceFields method.
   * It allows fields with references to embedded objects
   * to display the appropriate sub-fields. 
   * 
   * It is a recursive method, and can handle any number
   * of layers of embedding. The fields are stored in
   * a 'global' vector (as strings)
   *
   */
  
  private void getEmbedded (Vector fields, String basePrefix, Short lowestBase)
  {
    Base tempBase;
    BaseField tempField;
    String myName;
    Short tempIDobj;
    short tempID;
      
    /* -- */
    
    try
      {
	// Examine each field and if it's not referring to an embedded,
	// then add it's name + basePrefix to the string vector
     
	for (int j=0; fields != null && (j < fields.size()); j++)
	  { 
	    tempField = (BaseField) fields.elementAt(j);
	      
	    if (! tempField.isEditInPlace())
	      {	 
		if (! (tempField.getID() == 0 || tempField.getID() == 8))
		  {
		    // ignore containing objects and the like...

		    myName = tempField.getName();
		    myName = basePrefix + "/" + myName;  // slap on the prefix
		    Embedded.addElement(myName);

		    fieldHash.put(myName, tempField);
		   
		    // Also, save the information on the target base
		    // in a hashtable
		      
		    // the ID will be used in creating the query for the 
		    // edit-in-place
		    
		    // if tempIDobj isn't null, then we've got 
		    // something beneath an edit in place. Add the
		    // id of the lowest level base to the baseIDHash
		    
		    if (lowestBase != null)
		      {
			baseIDHash.put(myName, lowestBase);
		      }
		  }
	      }
	    else
	      {
		// since it does refer to an embedded, call getEmbedded again,
		// with tempBase.getFields(), basePrefix/tempBase, 

		myName = tempField.getName();
		myName = basePrefix + "/" + myName;  // slap on the prefix
		  
		tempID = tempField.getTargetBase();

		tempIDobj = new Short(tempID);

		// get the base from the ShortHash
		  
		tempIDobj = new Short(tempID);
		tempBase = (Base) shortHash.get(tempIDobj);
		getEmbedded(tempBase.getFields(), basePrefix, tempIDobj);
	      }
	  }
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("caught remote exception: " + ex);	
      }
  }

  /**
   *
   * Internal method to return a choice menu containing the fields for
   * a particular base
   *
   */
    
  private qfieldChoice getChoiceFields (Base base)
  {
    short inVid;
    Base tempBase;                            // Used when handeling embedded objs
    BaseField basefield;
    qfieldChoice myChoice = new qfieldChoice();
    int i = 0; // counter variable

    /* -- */

    myChoice.addItemListener(this);
    myChoice.qRow = this.row;
    
    try
      {

	// We have to go get a real Base reference to the server, through the
	// shortHash.  The "base" that we have here is actually a BaseDump, so
	// we can't call the getFields type methods on it.

	Vector fields = ((Base)shortHash.get(new Short(base.getTypeID()))).getFields();
	Vector EIPfields = new Vector();
		  
	for (int j=0; fields != null && (j < fields.size()); j++) 
	  {
	    basefield = (BaseField) fields.elementAt(j);
	    
	    if (basefield.isEditInPlace())
	      {
		// add it to EIPfields
		 
		EIPfields.addElement(basefield);
		String fieldName = basefield.getName();
		getEmbedded(EIPfields, fieldName, null);
		EIPfields.removeElement(basefield);
	      }
	    else
	      {

		if (! (basefield.getID() == 0 || basefield.getID() == 8))
		  {
		    // ignore containing objects and the like...

		String Name = basefield.getName();
		myChoice.addItem(Name);
		
		// To avoid a whole bunch of string comparisons, 
		// we'll put the name of the field in the nameHash
		// with it's own name as the value (since it's not
		// an edit-in-place)
		  
		nameHash.put(Name, Name);
	      
		  }
	      }
	  }
	  
	if (! Embedded.isEmpty())
	  {
	    for (int k = 0; (k < Embedded.size()); k ++)
	      {
		String embedName = (String) Embedded.elementAt(k);

		myChoice.addItem(embedName); // make it so #1

		// Ok, let's try some string processing -- removing the
		// slashes.
		  
		int first = embedName.lastIndexOf("/") + 1;
		int last = embedName.length();
		String noSlash = embedName.substring(first, last);
		System.out.println("Feild Name for embedded: " + noSlash);

		// Add the slash-less name to the name hash, with the key
		// being the slash filled name
		  
		nameHash.put(embedName, noSlash);
	      }

	    Embedded.removeAllElements();
	  }
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("caught remote exception: " + ex);	
      }

    return myChoice;  
  }

  /**
   *
   * This internal method creates a new row of components and adds 'em
   * to the choice window
   *
   */
  
  private void addChoiceRow (Base base)
  {
    JLabel label1 = new JLabel("   ");
    JLabel label2 = new JLabel("     ");
    JLabel label3 = new JLabel("     ");
    JLabel label4 = new JLabel("     ");
    
    /* -- */

    myAry = new Component[MAXCOMPONENTS];

    try
      {
	// We have to go get a real Base reference to the server, through the
	// shortHash.  The "base" that we have here is actually a BaseDump, so
	// we can't call the getFields type methods on it.

	fieldChoice = getChoiceFields((Base)shortHash.get(new Short(base.getTypeID())));
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not load fieldChoice: " + rx);
      }

    currentField = (String) fieldChoice.getSelectedItem();  
    opChoice = getOpChoice(currentField);
    isNot = getIsNot(currentField);
    opChoice.qRow = this.row;
    Component newInput = getInputField(currentField);
      
    // - set visible to false so they don't appear
    // as they're added. just for looks.

    fieldChoice.setVisible(false);
    isNot.setVisible(false);
    opChoice.setVisible(false);
    newInput.setVisible(false);
    label1.setVisible(false);
    label2.setVisible(false);
    label3.setVisible(false);
    label4.setVisible(false);
      
    myAry[0] = label1;
    myAry[1] = fieldChoice;
    myAry[2] = label2;
    myAry[3] = isNot; 
    myAry[4] = label3;
    myAry[5] = opChoice; 
    myAry[6] = label4;
    myAry[7] = newInput; 
    
    addRow(myAry, true, inner_choice, this.row);
  }

  /**
   *
   * This internal method generates the GUI choice component
   * displaying the 'is/is not' strings appropriate type for the type
   * of field in the currently selected object base with name <field>.
   * 
   */

  private qaryChoice getIsNot(String field)
  {
    qaryChoice returnChoice = new qaryChoice();
    returnChoice.qRow = this.row;

    // Look it up in the fieldHash. if it's there, then goodie!

    BaseField myField = (BaseField) fieldHash.get(field);
    
    // If the field contains slashes, then we'll remove 'em
    // by using the name hash
    
    field = (String) nameHash.get(field);
    
    // easy as cake
    
    if (myField == null)
      {
	// It's not an edit in place.
	
      }
    else 
      {
	//System.out.println("Yes, brothers and sisters, we have an EIP!!!");
      }
  
    returnChoice.addItem("is");
    returnChoice.addItem("is not");
 
    returnChoice.addItemListener(this);
    return returnChoice;
  }  
  

  /**
   *
   * This internal method generates the GUI choice component
   * displaying the 'does/does not' strings appropriate type for the type
   * of field selected. Purely for grammatical purposes
   * 
   */

  private qaryChoice getDoesNot ()
    {
      qaryChoice returnChoice = new qaryChoice();
      returnChoice.qRow = this.row;
      
      returnChoice.addItem("does");
      returnChoice.addItem("does not");
      
      System.out.println("Deos not selected");

      returnChoice.addItemListener(this);
      return returnChoice;

     
    }
  
  /**
   *
   * This internal method generates a GUI input component of
   * the appropriate type for the field in the
   * currently selected object base with name <field>.
   *
   */

  private Component getInputField (String field)
  {
    try 
      {
	BaseField myField = (BaseField) fieldHash.get(field); 
	
	// If the field contains slashes, then we'll remove 'em
	// by using the name hash

	field = (String) nameHash.get(field);

	System.out.println("And We have put gotten it from NameHash: " + field);
	    
	// easy as pie
	  
	if (myField == null)
	  {
	    myField = this.defaultBase.getField(field);  

	    // Probably fix this...make it bring up an error dialog or
	    // something
   
	    inputField = new JTextField(12);
	    
	    return inputField; 
	  }

	if (myField.isDate())
	  {
	    dateField = new JTextField("dd/mm/yyyy");
	    
	    return dateField;   
	  }
	else if (myField.isBoolean())
	  {
	    JCheckBox boolBox = new JCheckBox("True");
	    System.out.println("It's a Boolean!");
	    boolBox.setSelected(true);

	    return boolBox;
	  }
	else if (myField.isIP())
	  {
	    JIPField IPField = new JIPField(true); // allow V6
	    
	    return IPField;
	  }
	else 
	  {
	    // It ain't no date
	    
	    inputField = new JTextField(12);
	    
	    return inputField; 
	  }
      }      
    catch (RemoteException ex)
      {
	throw new RuntimeException("caught remote exception: " + ex);	
      }
  }

  /**
   *
   * This internal method generates a GUI choice component with
   * the appropriate possibilities loaded for the field in the
   * currently selected object base with name <field>.
   *
   */

  private qaryChoice getOpChoice (String field)
  {
    try 
      {
	BaseField myField = (BaseField) fieldHash.get(field);

	// Check to see if the damn thing has slashes in it

	field = (String) nameHash.get(field);

	// easy as fish

	if (myField == null)
	  {
	    // It's not an edit in place. Engage.
	    
	    myField = this.defaultBase.getField(field);  
	  }
	
	intChoice = new qaryChoice();
	intChoice.addItem("="); 
	intChoice.addItem(">="); 
	intChoice.addItem("<="); 
	intChoice.addItem("<"); 
	intChoice.addItem(">");
	intChoice.addItem("= [Case Insensitive]");
	intChoice.addItem("Start With");
	intChoice.addItem("End With");
	intChoice.addItemListener(this);

	// Do a nice null test to make sure stuff isn't screwey
	  
	if (myField == null)
	  {
	    System.out.println("MYFIELD IS NULL! LIFE REALLY SUCKS!!");
	    
	    return intChoice;
	  }
	
	// NOTE - HANDLE VECTORS, IPs, etc

	if (myField.isDate())
	  {
	    dateChoice = new qaryChoice();
	    dateChoice.addItem("Same Day As");
	    dateChoice.addItem("Same Week As");
	    dateChoice.addItem("Same Month As");
	    dateChoice.addItem("Before");
	    dateChoice.addItem("After");

	    dateChoice.addItemListener(this);
	    return dateChoice;   
	  }
	else if (myField.isNumeric())
	  {
	    //System.out.println("Field: It's a number!");
	    return intChoice; 
	  }
	else if (myField.isArray())
	  {
	    vectorChoice = new qaryChoice();
	    vectorChoice.addItem("Contains All");
	    vectorChoice.addItem("Contains Any");
	    vectorChoice.addItem("Contains None");
	    vectorChoice.addItem("Length <");
	    vectorChoice.addItem("Length >");
	    vectorChoice.addItem("Length =");
	    vectorChoice.addItemListener(this);

	    return vectorChoice;
	  }
	else 
	  {
	    return intChoice; // Numeric operators are the default
	  }
      }      
    catch (RemoteException ex)
      {
	throw new RuntimeException("caught remote exception: " + ex);	
      }
  }


  /**
   *
   * This is an internal method to add a row to the main
   * query composition panel.
   *
   */

  private void addRow (Component[] myRow, boolean visible, JPanel myPanel, int Row) 
  {
    for (int n = 0; n < myRow.length; n++)
      {
	myPanel.add( n + "  " + Row + " lhwHW", myRow[n]);
      }

    for (int i = 0; i < myRow.length; i++)
      {
	if (myAry[i] != null)
	  {
	    myAry[i].setVisible(visible);
	  }
      }

    this.row ++;
    this.Rows.insertElementAt(myRow, Row); // add component array to vector
    
    // make sure the scroll pane is correctly spacing things
    
    choice_pane.invalidate();
    choice_pane.validate();   
  }

  /**
   *
   * This is an internal method to remove a row from the main
   * query composition panel.
   *
   */

  private void removeRow(Component[] myRow, JPanel myPanel, int Row)
  {
    for (int n = 0; n < myRow.length ; n++)
      {
	myPanel.remove(myRow[n]);
      }  
  
    this.Rows.removeElementAt(Row); // remove row from vector of rows
    this.row--;
  }

  /**
   *
   * This internal method takes the current state of the rows in the
   * main query composition panel and generates an appropriate Query
   * structure from them.
   *  
   */
  
  private Query createQuery()
  {
    // * -- * //
    
    Query myQuery;
    QueryNode myNode, tempNode;
    QueryNotNode notNode;
    QueryDataNode dataNode;
    QueryAndNode andNode;
    Object value;
    byte opValue;
    Component[] tempAry;

    JComboBox
      tempChoice1,
      tempChoice2,
      tempChoice3;

    String notValue,
           fieldName,
           operator;

    BaseField tempField;
    Integer tempInt = new Integer(0);
    JTextField tempText = new JTextField();
    JTextField tempDate = new JTextField();
    JCheckBox tempBox = new JCheckBox(); 
    JIPField tempIP = new JIPField(true); // allow possible V6 IPs

    boolean editInPlace;
    Short baseID;

    // -- //

    int allRows = this.Rows.size();
    System.out.println("NUMBER OF ROWS: " + allRows);
       
    tempAry = (Component[]) this.Rows.elementAt(0); // This is the first row in the Vector

    tempChoice1 = (JComboBox) tempAry[1];
    fieldName = (String) tempChoice1.getSelectedItem();

    tempChoice2 = (JComboBox) tempAry[3];
    notValue = (String) tempChoice2.getSelectedItem();

    tempChoice3 =  (JComboBox) tempAry[5];
    operator = (String)tempChoice3.getSelectedItem();
    
    Object tempObj = tempAry[7];

    if (tempObj instanceof JTextField)
      {
	tempText = (JTextField) tempAry[7];    
      }
    else if (tempObj instanceof Date)
      {
	tempDate = (JTextField) tempAry[7];
      }
    else if (tempObj instanceof JCheckBox)
      {
	tempBox = (JCheckBox) tempAry[7];
      }
    else if (tempObj instanceof JIPField)
      {
	tempIP = (JIPField) tempAry[7];
      }

    else 
      { 
	// default
	tempText = (JTextField) tempAry[7];    
      }

    // -- set the type for the text entered in the JTextField
    
    try
      {      
	/* Here's some code to deal with edit-in-place fields again
	 * what we need to do here is get the actual field from the 
	 * name, using the shortID of the base preovided by the
	 * fieldname Hash.
	 *
	 */ 

	baseID = (Short) baseIDHash.get(fieldName);
	
	if (baseID != null)
	  {
	    // The hash has a defined value for the base ID, therefore
	    // it's an edit in place
	    
	    tempField = (BaseField) fieldHash.get(fieldName);
	    fieldName = (String) nameHash.get(fieldName); // keep only the last field 
	    editInPlace = true;
	  }
	else
	  {
	    tempField = defaultBase.getField(fieldName);
	    editInPlace = false;
	  }

	if (tempField.isNumeric())
	  {
	    value = new Integer(tempText.getText());
	  }
	else if (tempField.isDate())
	  {
	    // **NOTE: This will have to be implemented when we decide how
	    // ** we're going to do dates


	    value = new Date();
	  }
	else if (tempField.isBoolean())
	  {
	    value = new Boolean(tempBox.isSelected());
	  }
	else if (tempField.isIP())
	  {
	    value = tempIP.getValue();
	  }
	else 
	  {
	    value = tempText.getText(); // default is string
	  }

	// -- get the correct operator
	// -- Note: you'll have to do this agian for vectors.
        //    Don't do them here!!


	if (! tempField.isArray()){

	if (operator == "=")
	  {
	    opValue = 1;
	  } 
	else if (operator == "<")
	  {
	    opValue = 2;
	  } 
	else if (operator == "<=")
	  {
	    opValue = 3;
	  } 
	else if (operator == ">") 
	  {
	    opValue = 4;
	  } 
	else if (operator == ">=") 
	  {
	    opValue = 5;
	  } 
	else if (operator.equals("= [Case Insesitive]"))
	  {
	    opValue = 6;
	  }
	else if (operator.equals("Start With"))
	  {
	    opValue = 7;
	  }
	else if (operator.equals("End With"))
	  {
	    opValue = 8;
	  }
	else
	  {
	    opValue = 9; // UNDEFINED
	  }    
	}

	else{

	  // we have a vector, so use vector operators
	  
	if (operator.equals("Contains Any"))
	  {
	    opValue = 1;
	  } 
	else if (operator.equals("Contains All"))
	  {
	    opValue = 2;
	  } 
	else if (operator.equals("Contains None"))
	  {
	    opValue = 3;
	  } 
	else if (operator.equals("Length =")) 
	  {
	    opValue = 4;
	  } 
	else if (operator.equals("Length >")) 
	  {
	    opValue = 5;
	  } 
	else if (operator.equals("Length <"))
	  {
	    opValue = 6;
	  }
	else
	  {
	    opValue = 7; // Undefined
	  } 
	}

	// -- if not is true then add a not node
    
	dataNode = new QueryDataNode(fieldName, opValue, value);
    
	if (notValue == "is not" || notValue == "does not")
	  {
	    notNode = new QueryNotNode(dataNode); // if NOT then add NOT node
	    myNode = notNode;
	  } 
	else 
	  {
	    myNode = dataNode;
	  }
	

	if (allRows == 1)
	  {
	    // Special case -- return only a single query node
	    
	    // We'll also have to use a different query constructor if
	    // we're dealin with an edit in place field

	    if (baseID != null)
	      {
		short baseid = baseID.shortValue();

		if (notValue == "is not" || notValue == "does not")
		  {
		    myQuery = new Query(baseid, myNode, editOnly); // Use the NOT node (see above)
		  } 
		else
		  { // "NOT" not selected
		    myQuery = new Query(baseid, myNode, editOnly); // just use the DATA node (see above)
		  }
		
		myQuery.setReturnType(defaultBase.getTypeID());

		// Degug stuff

		System.out.println("My Query: Embedded");
		System.out.println("------------------");
		System.out.println("");
		System.out.println("Field Name: " + fieldName);

		Base upWithPeople = (Base) shortHash.get(baseID); 
		String bName = upWithPeople.getName();

		System.out.println("Low Base: " + bName);
		System.out.println("Top Base: " + defaultBase.getName());
		System.out.println("Operator: " + opValue);
		System.out.println("Value: " + value);
	      }
	    else
	      {
		if (notValue == "is not" || notValue == "does not")
		  {
		    myQuery = new Query(baseName, myNode, editOnly); // Use the NOT node (see above)
		  } 
		else
		  { // "NOT" not selected
		    myQuery = new Query(baseName, myNode, editOnly); // just use the DATA node (see above)
		  }
	      }
	  }
	else // Multiple Rows
	  {
	    /* we have the first node already, so we can simply use AND nodes to 
	     * attach the query nodes to one another
	     */
	    
	    for (int i = 1; i < allRows; i ++) 
	      {
		tempAry = (Component[]) this.Rows.elementAt(i);

		tempChoice1 = (JComboBox) tempAry[1];
		fieldName = (String) tempChoice1.getSelectedItem();

		tempChoice2 = (JComboBox) tempAry[3];
		notValue = (String) tempChoice2.getSelectedItem();

		tempChoice3 =  (JComboBox) tempAry[5];
		operator = (String) tempChoice3.getSelectedItem();
	 
		if (tempObj instanceof JTextField)
		  {
		    tempText = (JTextField) tempAry[7];    
		  }
		else if (tempObj instanceof Date)
		  {
		    tempDate = (JTextField) tempAry[7];
		  }
		else if (tempObj instanceof JCheckBox)
		  {
		    tempBox = (JCheckBox) tempAry[7];
		  }
		else if (tempObj instanceof JIPField)
		  {
		    tempIP = (JIPField) tempAry[7];
		  }
		
		else 
		  { 
		    // default
		    tempText = (JTextField) tempAry[7];    
		  }
		
		// Now that we have tempObj, find out what it is and
		// use that information to assign a value to the query.


		if (tempObj instanceof JTextField)
		  {
		    tempText = (JTextField) tempAry[7];    
		  }
		else if (tempObj instanceof Date)
		  {
		    tempDate = (JTextField) tempAry[7];
		  }
		else if (tempObj instanceof JCheckBox)
		  {
		    tempBox = (JCheckBox) tempAry[7];
		  }
		else if (tempObj instanceof JIPField)
		  {
		    tempIP = (JIPField) tempAry[7];
		  }
		
		else 
		  { 
		    // default
		    tempText = (JTextField) tempAry[7];    
		  }

		
	/* Here's some code to deal with edit-in-place fields again
	 * what we need to do here is get the actual field from the 
	 * name, using the shortID of the base preovided by the
	 * fieldname Hash.
	 *
	 */ 
		
		baseID = (Short) baseIDHash.get(fieldName);
		
		if (baseID != null)
		  {
		    // The hash has a defined value for the base ID, therefore
		    // it's an edit in place
		    
		    tempField = (BaseField) fieldHash.get(fieldName);
		    fieldName = (String) nameHash.get(fieldName); // keep only the last field 
		    editInPlace = true;
		  }
		else
		  {
		    tempField = defaultBase.getField(fieldName);
		    editInPlace = false;
		  }
		
		if (tempField.isNumeric())
		  {
		    value = new Integer(tempText.getText());
		  }
		else if (tempField.isDate())
		  {
		    // Fix THIS!!!!
		    value = new Date();
		  }
		else if (tempField.isBoolean())
		  {
		    value = new Boolean(tempBox.isSelected());
		  }
		else if (tempField.isIP())
		  {
		    value = tempIP.getValue();
		  }
		else 
		  {
		    value = tempText.getText(); // default is string
		  }
		
		// -- get the correct operator
    
		if (operator == "=")
		  {
		    opValue = 1;
		  } 
		else if (operator == "<")
		  {
		    opValue = 2;
		  } 
		else if (operator == "<=")
		  {
		    opValue = 3;
		  } 
		else if (operator == ">") 
		  {
		    opValue = 4;
		  } 
		else if (operator == ">=") 
		  {
		    opValue = 5;
		  } 
		else 
		  {
		    opValue = 7; // UNDEFINED
		  }  
 
		// -- if not is true then add a not node
		
		dataNode = new QueryDataNode(fieldName, opValue, value);
		
		if (notValue == "is not" || notValue == "does not")
		  {
		    notNode = new QueryNotNode(dataNode); // if NOT then add NOT node
		    tempNode = notNode;
		  } 
		else 
		  {
		    tempNode = dataNode;
		  }    
	    
		andNode = new QueryAndNode(myNode, tempNode);
		myNode = andNode;
	      }
	
	    // Again, use the alternate constructor for EIP fields

	    if (baseID != null)
	      {
		short baseid = baseID.shortValue();
		myQuery = new Query(baseid, myNode, editOnly);
  
		// make sure the server knows what the top level base is
	    
		myQuery.setReturnType(defaultBase.getTypeID()); 
	      }
	    else
	      {
		myQuery = new Query(baseName, myNode, editOnly);
	      }
	  }

	// if we have popped up the optionsFrame, go ahead and
	// explicitly specify the fields to be returned.  Otherwise,
	// we'll skip this step and allow the server to return to us
	// the default fields.
	
	if (optionsFrame != null)
	  {
	    myQuery = setFields(myQuery);
	  }

	// TESTING dumpToString
	
	System.out.println("Results: " + myQuery.dumpToString());

	// TESTING readQuery

	readQuery(myQuery.dumpToString());

	return myQuery;
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("caught remote exception: " + ex);	
      }
  }

  /**
   *
   * This method sets what fields should be returned by the Query.  Note
   * that this should only be called if the user has explicitly requested
   * a non-standard list of return fields, as the server will automatically
   * hide a bunch of undesired fields if we have not called addField() on
   * a newly constructed Query object.
   *
   */
  
  public Query setFields(Query someQuery)
  {
    BaseField tempField;
    short tempShort;
    String tempString;
    JCheckBox tempBox;
    Vector tempVector;

    /* -- */

    // Time to make the doughnuts -- uh, or set the return options for the fields

    System.out.println("Here's how many rows we've got " + fieldOptions.size());

    fieldLoop :for (int x = 0; x < fieldOptions.size(); x ++)
      {
	try
	  {
	    tempVector = (Vector) fieldOptions.elementAt(x);
	    
	    for (int y = 0; y < tempVector.size(); y ++)
	      {
		// here we process each checkbox in the row
		
		tempBox = (JCheckBox) tempVector.elementAt(y);

		if (tempBox.isSelected())
		  {
		    // the box has been checked -- we want this field

		    tempString = tempBox.getText();
		    tempField = defaultBase.getField(tempString);
			
		    // Sometimes this next lines gives us fits...why?

		    if (tempField == null) 
		      {  
			System.out.println("It's a cold, null world,");
			System.out.println("Thetrefore, we're breaking out of loop");
			break fieldLoop;
		      }
		    else 
		      {
			tempShort = tempField.getID();
			someQuery.addField(tempShort); // add the field to the query return
			
			System.out.println("Setting Return: " + tempField.getName());
		      }
		  }
		else 
		  {
		    // else just skip this box
		    
		    System.out.println("Skipping " + tempBox.getText()); 
		  }
	      }
	  }
	catch (RemoteException ex)
	  {
	    throw new RuntimeException("caught remote exception: " + ex);	
	  }
      }

    return someQuery;
  }

  /////////////////////
  // Event Handlers //
  /////////////////////

  /**
   *
   * This is the standard ActionListener callback method.  This method
   * catches events from the various buttons used by querybox.
   * 
   */
  
  public void actionPerformed(ActionEvent e)
  {
    if (e.getSource() == qSave)
      {
	System.out.println("Save Clicked");

	saveBox = new JDialog(optionsFrame, "Save As", true);

	JPanel button_panel = new JPanel();
	JPanel mid_panel = new JPanel();

	button_panel.setLayout(new FlowLayout());
	button_panel.add(qDone);
	button_panel.add(new JLabel("   "));
	button_panel.add(qCancel);

	saveText = new JTextField(10);
	mid_panel.add(saveText);


	Font f = new Font("TimesRoman", Font.BOLD, 14);
	JLabel SaveJLabel = new JLabel("Enter Name For Query");
	SaveJLabel.setFont(f);
	
	saveBox.getContentPane().setLayout(new BorderLayout());
	saveBox.getContentPane().add("North", SaveJLabel);
	saveBox.getContentPane().add("Center", mid_panel);
	saveBox.getContentPane().add("South", button_panel);
	saveBox.setSize(300, 135);

	qDone.addActionListener(this);
	qCancel.addActionListener(this);

	saveBox.setVisible(true);
      }		   
    
    if (e.getSource() == qCancel)
      {
	// Cancel the save transaction and close the save dialog

	saveBox.setVisible(false);

      }
    
    if (e.getSource() == qDone)
      {
	// -- We want to save the current query under the current name

	String outPut = saveText.getText();
	System.out.println("save name is: " + outPut);

	saveBox.setVisible(false);
      }

    if (e.getSource() == qLoad)
      {
	// We gots to create the dialog for loading queries.
	
	System.out.println("Load Button Clicked");
	
	JLabel l;
	int n;
	JPanel listPanel = new JPanel();
	JPanel centeringPanel = new JPanel();

	GridBagLayout gbl = new GridBagLayout();
	GridBagConstraints gbc = new GridBagConstraints();
  	JPanel button_panel = new JPanel();
	JPanel outPanel = new JPanel();

	button_panel.setLayout(gbl);
	button_panel.setSize(100, 50);
	button_panel.setBackground(Color.white);
	
	centeringPanel.setLayout(new FlowLayout());
	loadBox = new JDialog(optionsFrame, "Load Query", true);	
	List queryList = new List(10);

	l = new JLabel("");
	gbc.gridy = n = 0;
	gbl.setConstraints(l, gbc);
	button_panel.add(l);

	n = 6;

	gbc.gridy = n++;
	gbl.setConstraints(lSelect, gbc);
	button_panel.add(lSelect);
	lSelect.addActionListener(this);
	lSelect.setBackground(Color.lightGray);

	gbc.gridy = n++;
	gbl.setConstraints(lRename, gbc);
	button_panel.add(lRename);
	lRename.addActionListener(this);
	lRename.setBackground(Color.lightGray);

	gbc.gridy = n++;
	gbl.setConstraints(lCancel, gbc);
	button_panel.add(lCancel);
	lCancel.addActionListener(this);
	lCancel.setBackground(Color.lightGray);

	n = 17;
	l = new JLabel("");
	gbc.gridy = n;
	gbl.setConstraints(l, gbc);
	button_panel.add(l);
 
	// -- Add the elements to the choice list

	listPanel.setBackground(Color.blue);
	listPanel.setLayout(new BorderLayout());
	listPanel.add(queryList);

	// - add identifying label
	
	Font f = new Font("TimesRoman", Font.BOLD, 14);
	JLabel qJLabel = new JLabel("Saved Queries");
	qJLabel.setFont(f);
	qJLabel.setForeground(Color.white);
	centeringPanel.add(qJLabel);
	listPanel.add("North", centeringPanel);

	queryList.setBackground(Color.white);
	queryList.setForeground(Color.blue);
	queryList.setSize(100, 100);
	queryList.add("Testing 1");
	queryList.add("Testing 2");
	queryList.add("Testing 3");

	listPanel.add("Center", queryList);

	JSplitPane loadPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listPanel, button_panel);

	loadBox.setSize(275, 175);
	loadBox.getContentPane().add(loadPane);
	loadBox.setVisible(true);
      }

    if (e.getSource() == displayButton)
      {
	System.out.println("Field Display Selected");

	if (optionsFrame == null)
	  {
	    optionsFrame = createOptionFrame(defaultBase);
	  }

	optionsFrame.setVisible(true);
      }
    
    if (e.getSource() == optionClose)
      {
	// this check shouldn't be necessary, but..

	if (optionsFrame != null)
	  {
	    optionsFrame.setVisible(false);
	  }
      }

    if (e.getSource() == lCancel)
      {
	loadBox.setVisible(false);
      }

    if (e.getSource() == OkButton) 
      {
	System.out.println("You will submit");   

	if (optionsFrame != null)
	  {
	    optionsFrame.setVisible(false);
	  }

	this.returnVal = createQuery();
	setVisible(false);	// close down
      } 
    else if (e.getSource() == CancelButton)
      {
	System.out.println("Cancel was pushed");

	if (optionsFrame != null)
	  {
	    optionsFrame.setVisible(false);
	  }

	this.returnVal = null;
	setVisible(false);
      } 

    if (e.getSource() == addButton)
      {
	addChoiceRow(defaultBase);
      }

    if (e.getSource() == removeButton)
      {
	if (this.row <= 1)
	  {
	    System.out.println("Error: cannot remove any more rows");
	  }  
	else
	  {
	    Component[] tempAry = (Component[]) this.Rows.elementAt(this.row - 1);
	    removeRow(tempAry, inner_choice, this.row - 1);
	    tempAry = null;
	  } 
      }
  } 

  /**
   *
   * This is the standard ItemListener callback method.  This method
   * catches events from Checkboxes and various choice components.
   * 
   */
  
  public void itemStateChanged(ItemEvent e)
  {
    if (e.getSource() == editBox)
      {
	this.editOnly = editBox.isSelected();
	System.out.println("Edit Box Clicked: " + editOnly);
      }

    if (e.getSource() == baseChoice)
      {
	System.out.println("Base selected");
	// First, change the base
	  
	Base defaultBase = (Base) myHash.get(baseChoice.getSelectedItem());
	this.defaultBase = defaultBase;

	try
	  {      
	    this.baseName = defaultBase.getName();
	  }
	catch (RemoteException ex)
	  {
	    throw new RuntimeException("caught remote exception: " + ex);	
	  }
	  
	// remove for all entries in vector of component arrays

	for (int i = this.row - 1; i > -1; i--)
	  {
	    Component[] tempRow = (Component[]) this.Rows.elementAt(i);
	    removeRow(tempRow, inner_choice, i);
	    System.out.println("Removing Row: " + i);
	    this.Rows.setSize(i);
	  }
	  
	addChoiceRow(defaultBase);

	// Now update optionsFrame

	if (optionsFrame != null)
	  {
	    optionsFrame.setVisible(false);
	    optionsFrame.removeAll();
	    optionsFrame = null; // we'll reload it next time the popup is requested
	  }
      }
	
    if (e.getSource() instanceof qfieldChoice)
      {
	System.out.println("Field Selected");

	qfieldChoice source = (qfieldChoice) e.getSource();

	String fieldName = (String) source.getSelectedItem();

	int currentRow = source.getRow();

	if (currentRow >= Rows.size())
	  {
	    return;		// to handle Swing's value-set notification
	  }

	System.out.println("Current Row " + currentRow);

	Component[] tempRow = (Component[]) Rows.elementAt(currentRow); 
	
	if (tempRow == null)
	  {
	    System.out.println("OOOOOPS!!!");
	  }

	removeRow(tempRow, inner_choice, currentRow);
	opChoice = getOpChoice(fieldName);
	Component newInput = getInputField(fieldName);
	tempRow[3] = getIsNot(fieldName);
	tempRow[5] = opChoice;
	tempRow[7] = newInput;
	addRow(tempRow, true, inner_choice, currentRow);
      }
    
    else if (e.getSource() instanceof qaryChoice)
      {
	System.out.println("Other Choice selected (presumably operator choice)");

	qaryChoice source = (qaryChoice) e.getSource();
	
	String opName = (String) source.getSelectedItem();
	System.out.println("Opname: " + opName);
	int currentRow = source.getRow();
	Component[] tempRow = (Component[]) Rows.elementAt(currentRow);
	
	if ((opName.equalsIgnoreCase("Start With")) ||
	    (opName.equalsIgnoreCase("End With"))	    )
	  {
	    // For grammatical purposes, we're changing the "is/is not"
	    // choice to "does/does not"
	    
	    tempRow[3].setVisible(false);
	    tempRow[3] = getDoesNot();    
	    
	  }
	
	else 
	  {
	    JComboBox tempChoice = (JComboBox) tempRow[1];
	    String myField = (String) tempChoice.getSelectedItem();
	    tempRow[3].setVisible(false);
	    tempRow[3] = getIsNot(myField);
	    System.out.println("NO START WITH");
	    
	  }

	removeRow(tempRow, inner_choice, currentRow);
	addRow(tempRow, true, inner_choice, currentRow); // make sure the new 
	                                                 // component makes it into the 
	                                                 //row
	

	if ((opName.equalsIgnoreCase("Contains All")) || 
	    (opName.equalsIgnoreCase("Contains None")) ||
	    (opName.equalsIgnoreCase("Contains Any")))
	  {
	    // disable the is/is not choice cause it makes
	    // no sense in this contaxt
	    
	    // NOTE: this doesn't seem to work yet...

	    if (tempRow[3].isEnabled())
	      {	
		tempRow[3].setEnabled(false);
	      }
	  }
	else
	  {
	    // make sure everything's enabled

	    if (! tempRow[3].isEnabled())
	      {
		tempRow[3].setEnabled(true);
	      }
	  }
      }
    else 
      {
	// ?? what the heck is it? We don't recognize this component
      }
  }



  public Query readQuery (String savedQuery)
    {
      /** 
       *
       * This method takes a as its parameter query and sets the 
       * gui components of the querybox to reflect the state of 
       * that query.
       */
      
      String temp,
	     name,
	     base,
	     edit;

      int baseInt,
	  nameBreak,
	  baseBreak,
          editBreak,
	  queryBreak;
      
      temp = savedQuery;

      nameBreak = temp.indexOf(":"); // this will get the first 
                                     // index of the ':' character 
      System.out.println("--------");

      name = temp.substring(0, nameBreak); 
      System.out.println("Name Found: " + name);

      temp = temp.substring(nameBreak + 1, temp.length()); // The '+1' removes the colon

      baseBreak = temp.indexOf(":");
      base = temp.substring(0, baseBreak);
      System.out.println("Here's base: " + base);
      
      temp = temp.substring(baseBreak + 1, temp.length());
      editBreak = temp.indexOf(":");
      edit = temp.substring(0, editBreak);
      System.out.println("And Edit: " + edit);

      temp = temp.substring(editBreak + 1, temp.length());
      System.out.println("And the Rest: " + temp);
      System.out.println("--------");

      // Time to create the query....

      // Step One: Create the root Node. Then we can make a nice query from it.
       
	 
      // First, check and see if the base is in short or string form. This is done
      // by looking to see if a '#' sign is the first character (note: be sure to
      // ignore backslashed characters


      // <<< Insert well written and robust code here >>>


      /* Step Two: Figure out what the base is, and what form it's in (ie, baseID,
       * baseName, etc), and get the other pertinent variables. We'll use these to 
       * create the query object.
       */
      
    
      this.queryString = temp;   // decodeString will use the querystring to read
                                 // the query
                              
      QueryNode root = decodeString();
      boolean Edit;
      

      if (edit.equals("true"))
	{
	  Edit = true;
	}
      else
	{
	  Edit = false;
	}

      Query returnQuery = new Query(base, root, Edit);

      System.out.println("");
      System.out.println("______________");
      System.out.println("And Here it is: " + returnQuery.dumpToString());

      return returnQuery;
      
    }

      
  private QueryNode decodeString()
  
    {
      /* This method will break down the LISP-y section of the
       * query-string. In keeping with the theme, it is 
       * recursive. The nodes of the query are set as it goes along.
       *
       * The outer method is a helper method, while the inner method
       * rDecode really odes all the work
       */

      QueryNode returnNode;
      String temp;
     
      this.readLength = this.queryString.length();
      returnNode = null;

      if (this.queryString.startsWith("("))
	  {
	    System.out.println("YAY!!");
	    	  
	    temp = this.queryString.substring(1, this.readLength - 1); // remove the outer 
	                                                 // parens
	    
	    this.readLength = this.readLength - 2;

	    returnNode = rDecode(); // Begin the begin (ie do the recursive stuff)
	  }

   
	    return returnNode;  
      
    }
  
  private QueryNode rDecode () 
    {
      
      QueryNode myNode = null;
      String temp = this.queryString;

      System.out.println("Here's the rest: " + temp);
      
      /* While we're recursing, we are chopping off bits of this.queryString,
	 which resides high (in the querybox object) above all this madness.
	 
	 So, let's begin by seeing what this level holds:
	 
	 1) if the first char is another paren, then call rDecode again after 
	 removing it
	 
	 2) if the next char is an operator (=, =<, =>, <, >, etc) then it's a
	 data note. Here, we have to get the fieldname and value too, and with
	 the fieldname we have to test to see if it's in Short or String form
	 
	 3) if the string starts with 'not' then make a not node and set it's
	 child to rDecode()
	 
	 4) if temp begins with 'and' or 'or', then we have to set both 
	 children to rDecode(). The queryString will be 
	 shortened as we go along.
	 
      */
      
      if (temp.startsWith("(") || temp.startsWith(")"))
	{
	  System.out.println("Parenthesis encountered. Destroy it, captain");
	  
	  this.queryString = this.queryString.substring(1, this.readLength); // length defined in helper
	  
	  this.readLength --;
	  myNode = rDecode();
	
	}
      
      else if (temp.startsWith("not")) 

	{
	  // make a not node, cut the 'not' off of the queryString and
	  // set the child of the not to rDecode(queryString) and remove ')'

	  myNode = new QueryNotNode(rDecode());
	  
	  if (this.queryString.startsWith(")"))
	    {
	      this.queryString = this.queryString.substring(1, this.readLength); 
	    }

	  return myNode;

	}

         else if (temp.startsWith("and")) 

	{
	  // make aa and node, cut the 'and' off of the queryString and
	  // set the child of the not to rDecode(queryString) and remove ')'
	  
	  myNode = new QueryAndNode(rDecode(), rDecode());

	  if (this.queryString.startsWith(")"))
	    {
	      this.queryString = this.queryString.substring(1, this.readLength); 
	    }
	  
	  return myNode;
	}

      else if (temp.startsWith("or")) 

	{
	  // make an or node, cut the 'or' off of the queryString and
	  // set the child of the not to rDecode(queryString) and remove ')'
	   
	  myNode = new QueryOrNode(rDecode(), rDecode());

	  if (this.queryString.startsWith(")"))
	    {
	      this.queryString = this.queryString.substring(1, this.readLength); 
	    }

	  return myNode;

	}

      else 

	{
	  // It should be a data node. Lets see what happens.
	  
	  byte comparator = 0;
	  String value = null;
	  String fieldname = null;
	  

	  if (temp.startsWith("="))
	    { 
	      comparator = 1;
	      System.out.println("Equals Found (=)");
	      this.queryString = this.queryString.substring(1, this.readLength);
	      this.readLength = this.readLength - 1;
	    }

	  else if (temp.startsWith("<"))
	    {
	      comparator = 2;
	      this.queryString = this.queryString.substring(1, this.readLength);
	      this.readLength = this.readLength - 1;
	    } 
	  else if (temp.startsWith("<="))
	    {
	      comparator = 3;
	      this.queryString = this.queryString.substring(2, this.readLength);
	      this.readLength = this.readLength - 2;
	    }
	  else if (temp.startsWith(">"))
	    {
	      comparator = 4;
	      this.queryString = this.queryString.substring(2, this.readLength);
	      this.readLength = this.readLength - 2;
	    }
	  else if (temp.startsWith(">="))
	    {
	      comparator = 5;
	      this.queryString = this.queryString.substring(2, this.readLength);
	      this.readLength = this.readLength - 2;
	    }

	  else if (temp.startsWith("= [Case Insensitive]"))
	    {
	      comparator = 6;
	      this.queryString = this.queryString.substring(20, this.readLength);
	      this.readLength = this.readLength - 20;
	    }
	  
	  else if (temp.startsWith("Start With"))
	    {
	      comparator = 7;
	      this.queryString = this.queryString.substring(10, this.readLength);
	      this.readLength = this.readLength - 10;
	    }
	  
	  else if (temp.startsWith("End With"))
	    {
	      comparator = 8;
	      this.queryString = this.queryString.substring(8, this.readLength);
	      this.readLength = this.readLength - 8;
	    }

	  else
	    {
	      System.out.println("Help! rDecode operator not found: " + temp);
	    }
	  
	  // We've got the operator. Now we have to get the fieldname and value
	  
	  boolean fieldDone,
	          valueDone;

	  int nextIndex;

	  fieldDone = valueDone = false;

	  while (! (fieldDone && valueDone))
	    {
	      if ((this.queryString.startsWith(" ") || (this.queryString.startsWith("("))))
		{
		  // ignore parens and whitespace;
		  
		  this.queryString = this.queryString.substring(1, this.readLength);
		  this.readLength --;

		  System.out.println("Continue");

		  continue;

		}

	      else if (this.queryString.startsWith("fieldname"))
		{
		  // Ok, space up to the fieldname in the string and 
		  // get it. Also, be sure to look for # signs.

		  System.out.println("Field");

		  this.queryString = this.queryString.substring(10, this.readLength);
		  
		  // 10 chars for 'fieldname' + whiteSpace;

		  this.readLength = this.readLength - 10;

		  nextIndex = this.queryString.indexOf(")");
		  
		  // Put backslash check here;
  
		  fieldname = this.queryString.substring(0, nextIndex);
		  
		  this.queryString = this.queryString.substring(nextIndex + 1, this.readLength);

		  this.readLength = this.readLength - (fieldname.length() + 1);

		  System.out.println("Fieldname is: " + fieldname);

		  fieldDone = true;
		}
	      
	      else if (this.queryString.startsWith("value"))
		
		{
		  // Ok, space up to the Value in the string and 
		  // get that.

		  System.out.println("Value");
		  
		  this.queryString = this.queryString.substring(6, this.readLength);
		  
		  // 6 chars for 'value' + whiteSpace;

		  this.readLength = this.readLength - 6;

		  nextIndex = this.queryString.indexOf(")");
		  
		  // Put backslash check here;
  
		  value = this.queryString.substring(0, nextIndex);
		  
		  this.queryString = this.queryString.substring(nextIndex + 1, this.readLength);

		  this.readLength = this.readLength - (value.length() + 1);

		  System.out.println("value is: " + value);

		  valueDone = true;
		}
	      
	      
	      else
		{
		  System.err.println("Error: Unknown Character Found in rDecode");
		  break;
		}
	    }
	  	
	  myNode = new QueryDataNode (fieldname, comparator, value);
	  
	  
	  if (this.queryString.startsWith(")"))
	    {
	      this.queryString = this.queryString.substring(1, this.readLength); 
	    }

	  System.out.println("We're in the DataNode method. Returning DataNode");

	  if (myNode == null)
	    {
	      System.out.println("Uh oh, myNode is null. bad");
	    }

	}

      return myNode;
      
      // return null;  // If this happens it's bad.
    }
}

/*------------------------------------------------------------------------------
                                                                           class 
                                                                         qChoice

------------------------------------------------------------------------------*/

class qChoice extends JComboBox {
  
  int qRow; // keeps track of which row the choice menu is located in
  
  public int getRow()
  {
    return qRow;
  }
}

/*------------------------------------------------------------------------------
                                                                           class 
                                                                    qfieldChoice

------------------------------------------------------------------------------*/

class qfieldChoice extends JComboBox {
  
  int qRow; // keeps track of which row the choice menu is located in
  
  public int getRow()
  {
    return qRow;
  }
}

/*------------------------------------------------------------------------------
                                                                           class 
                                                                     qbaseChoice

------------------------------------------------------------------------------*/

class qbaseChoice extends JComboBox {
  
  int qRow; // keeps track of which row the choice menu is located in
  
  public int getRow()
  {
    return qRow;
  }
}

/*------------------------------------------------------------------------------
                                                                           class 
                                                                      qaryChoice

------------------------------------------------------------------------------*/

class qaryChoice extends JComboBox {
  
  int qRow; // keeps track of which row the choice menu is located in
 
  public int getRow()
  {
    return qRow;
  }
}
