/*

   querybox

   Description.
   
   Created: 23 July 1997
   Version: 1.0 97/07/30
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

import gjt.Box;



/*------------------------------------------------------------------------------
                                                                           class 
                                                                        querybox

------------------------------------------------------------------------------*/

class querybox extends Dialog implements ActionListener, ItemListener {

  
  /////////////////////
  // Class Variables //
  /////////////////////


  static final boolean debug = false;
  static final int MAXCOMPONENTS = 8; // the number of components that
                                      // can appear in a query choice

  // --

  Frame optionsFrame = new Frame("Options Frame");

  Hashtable 
  
  baseHash,    // Key: Bases      *--*  Value: Fields  
   shortHash;  // Key: Base ID    *--*  Value: Corresponding Base
  

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
  
  Button OkButton = new Button ("Submit");
  Button CancelButton = new Button("Cancel");
  Button addButton = new Button("Add Choices");
  Button removeButton = new Button("Remove Choices");
  Button displayButton = new Button("Return Options");
  Button optionClose = new Button("Close");
 
  // - Panels, Boxes and Panes

  Panel query_panel = new Panel();
  Panel base_panel = new Panel();
  Panel inner_choice = new Panel();
 
  ScrollPane choice_pane = new ScrollPane();
  
  Box choiceBox = new Box(choice_pane, "Query Fields");
  Box baseBox = new Box(base_panel, "Base and Field Menu");
 
  Panel Choice_Buttons = new Panel(); 
  Panel query_Buttons = new Panel();

  Checkbox editBox = new Checkbox("Editable");

  // - Choice menus
  
  qChoice baseChoice = new qChoice(); // there's only one of these
  qfieldChoice fieldChoice; // but there's libel to be a whole slew of these
  qChoice opChoice; // and these too
  qChoice intChoice;
  qChoice dateChoice;
  qaryChoice isNot; // will either be a boolean choice, or a list of operators
                    // depending on whether the field is an array

  // - imput fields

  TextField inputField = new TextField(6);
  TextField dateField;

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

  // - other variables

  int row = 0;
  Query returnVal;
  Base defaultBase;
  Component[] myAry = new Component[MAXCOMPONENTS]; // stores a single row
 
  // - Strings

  String
    currentBase,
    currentField,
    baseName;

  /* -- */


  //////////////////
  // Constructors //
  //////////////////

  public querybox (Base defaultBase, Hashtable baseHash, Hashtable shortHash,
                    Frame parent, String DialogTitle)
    {
      super(parent, DialogTitle, true); // the boolean value is to make the dialog modal
      
      Vector Bases;
     
      /* -- */
      
      // Main constructor for the querybox window
      

      this.baseHash = baseHash;  
      this.defaultBase = defaultBase;
      this.shortHash = shortHash;

      // - Define the main window

      optionsFrame.setVisible(false); // do not display return options window yet
                                      
      this.setLayout(new BorderLayout());   
      this.setBackground(Color.white);
     
      OkButton.addActionListener(this);
      OkButton.setBackground(Color.lightGray);
      CancelButton.addActionListener(this);
      CancelButton.setBackground(Color.lightGray);
      Choice_Buttons.setLayout(new FlowLayout ());
      Choice_Buttons.add(OkButton);
      Choice_Buttons.add(CancelButton);
      this.add("South", Choice_Buttons); 

      editBox.addItemListener(this);
      editBox.setState(false);
      this.editOnly = false;
      
      query_panel.setLayout(new BorderLayout());
      query_panel.setBackground(Color.lightGray); 
      this.add("Center", query_panel); 
    
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
     
      // - Create the choice window containint the fields 

      Enumeration enum = baseHash.keys();
      
      try
	{
	  while (enum.hasMoreElements()){
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
	      baseChoice.select(defaultBase.getName()) ; 
	      this.baseName = defaultBase.getName();
	    }
	  else 
	    { 
	      // no default given. pick the one that's there.
	    
	      currentBase = baseChoice.getSelectedItem();
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
      base_panel.add(new Label("  "));
      base_panel.add(editBox);
      base_panel.add(new Label("  "));
      base_panel.add(displayButton);

      inner_choice.setLayout(new TableLayout(false));
      inner_choice.setBackground(Color.white);
      choice_pane.add(inner_choice);

      query_panel.add("North", baseBox);
      query_panel.add("Center", choiceBox);
      
      addChoiceRow(defaultBase); // adds the initial row   
           
  // - Define the frame that will contain the return options
      
      optionsFrame = createOptionFrame(defaultBase);
    }

  // - ALternate Constructor. Used when no default query is provided

  public querybox (Hashtable baseHash, Hashtable shortHash, 
                   Frame parent, String myTitle) 
    {
      this(null, baseHash, shortHash, parent, myTitle);
    } 

 
  ////////////////
  //   Methods  //
  ////////////////

 
  Frame createOptionFrame (Base base)
    {
      /* Method to return a choice menu containing the fields for
       * a particular base
       */

      ScrollPane option_pane = new ScrollPane();
      
      Panel option_panel = new Panel();
      Panel choice_option = new Panel(); // basically holds the Close button
      
      BaseField basefield;
      Frame myFrame = new Frame("Return Options");
      Checkbox newCheck; 
      Panel inner_panel = new Panel();
      
      Vector tmpAry;
      
      /* -- */
      
      myFrame.setSize(380,250);
      
      optionClose.setBackground(Color.lightGray);
      optionClose.addActionListener(this);
      
      choice_option.setBackground(Color.gray);
      choice_option.setLayout(new FlowLayout());
      choice_option.add(optionClose);
      
      inner_panel.setBackground(Color.white);
      option_panel.setLayout(new BorderLayout());
      option_panel.add("South",choice_option);
      option_panel.add("Center", inner_panel);
      
      inner_panel.setLayout(new TableLayout());
    
      try
	{
	  Vector fields = base.getFields();
	  tmpAry = new Vector();

	  int count = 0;
	  int tmpRow = 0;
	  
	  for (int j=0; fields != null && (j < fields.size()); j++) 
	    {	
	      basefield = (BaseField) fields.elementAt(j);
	      String Name = basefield.getName();	   
	      newCheck = new Checkbox(Name);
	      newCheck.setState(true);
	      
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
            
      option_pane.add(option_panel);
      myFrame.add(option_pane);
      
      // overkill?

      myFrame.invalidate();
      myFrame.validate();
      myFrame.repaint();

      return myFrame;  
    }
  
  void getEmbedded (Vector fields, String basePrefix, Short lowestBase)
    {
      /* A companion to the following getChoiceFields method.
       * It allows fields with references to embedded objects
       * to display the appropriate sub-fields. 
       * 
       * It is a recursive method, and can handle any number
       * of layers of embedding. The fields are stored in
       * a 'global' vector (as strings)
       */

      Base tempBase;
      BaseField tempField;
      String myName;
      Short tempIDobj;
      short tempID;
      
      // * -- *
      
      try
	{

	  for (int j=0; fields != null && (j < fields.size()); j++)
	    { 
	      // Examine each field and if it's not referring to an embedded,
	      // then add it's name + basePrefix to the string vector
     
	      tempField = (BaseField) fields.elementAt(j);
	      
	      if (! tempField.isEditInPlace())
		{	 
		  if (tempField.getID() != 0){

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
		    
		    if (lowestBase != null){
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
    
    qfieldChoice getChoiceFields (Base base)

    {
      /* Method to return a choice menu containing the fields for
       * a particular base
       */

      short inVid;
      Base tempBase;                            // Used when handeling embedded objs
      BaseField basefield;
      qfieldChoice myChoice = new qfieldChoice();
      myChoice.addItemListener(this);

      myChoice.qRow = this.row;

      int i = 0; // counter variable

      try
	{
	  Vector fields = base.getFields();
	  Vector EIPfields = new Vector();
		  
	  for (int j=0; fields != null && (j < fields.size()); j++) 
	    {
	      basefield = (BaseField) fields.elementAt(j);
	    
	      if (basefield.isEditInPlace() == true)
		{
		  // add it to EIPfields
		 
		  EIPfields.addElement(basefield);
		  String fieldName = basefield.getName();
		  getEmbedded(EIPfields, fieldName, null);
		  EIPfields.removeElement(basefield);
		 
		}
	      else
		{
		  String Name = basefield.getName();
		  myChoice.add(Name);
		  
		  // To avoid a whole bunch of string comparisons, 
		  // we'll put the name of the field in the nameHash
		  // with it's own name as the value (since it's not
		  // an edit-in-place)
		  
		  nameHash.put(Name, Name);

		}
	    }
	  
	  if (! Embedded.isEmpty())
	    {
	      for (int k = 0; (k < Embedded.size()); k ++)
		{
		  String embedName = (String) Embedded.elementAt(k);

		  myChoice.add(embedName); // make it so #1

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
  
  void addChoiceRow (Base base)
    {
      /* This creates a new row of components and adds 'em to the 
       * choice window
       */
      
      myAry = new Component[MAXCOMPONENTS];
  
      //qTextField newInput = new qTextField(6); 
      //newInput.qRow = this.row;
     
      Label label1 = new Label("      ");
      Label label2 = new Label("      ");
      Label label3 = new Label("      ");
      Label label4 = new Label("      ");
      
      //* -- *//

      fieldChoice = getChoiceFields(base);
    
      currentField = fieldChoice.getSelectedItem();  
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

    qaryChoice getIsNot(String field){
 
      qaryChoice returnChoice = new qaryChoice();
      returnChoice.qRow = this.row;

      try 
	{  
	  // Look it up in the fieldHash. if it's there, then goodie!

	  BaseField myField = (BaseField) fieldHash.get(field);

	  // If the field contains slashes, then we'll remove 'em
	  // by using the name hash
    
	  field = (String) nameHash.get(field);
	  
	  // easy as cake
	 
	  if (myField == null){

	    // It's not an edit in place.

	    myField = this.defaultBase.getField(field);  
	  }

	  else 
	    {
	      System.out.println("Yes, brothers and sisters, we have an EIP!!!");

	    }
	  if (myField == null){

	    System.out.println("LIFE SUCKS!!");
	    myField = this.defaultBase.getField(field);  
	  }

	  if (myField.isArray()){

	    returnChoice.add("Length");
	    returnChoice.add("Contains");
	    returnChoice.add("Does Not Contain");       
	  }
	  else 
	    {
	    returnChoice.add("is");
	    returnChoice.add("is not");
	  }
	  returnChoice.addItemListener(this);
	  return returnChoice;
	}  
      
      catch (RemoteException ex)
	{
	  throw new RuntimeException("caught remote exception: " + ex);	
	}
      
}

      Component getInputField (String field){

	try 
	  {
	    BaseField myField = (BaseField) fieldHash.get(field); 

	    // If the field contains slashes, then we'll remove 'em
	    // by using the name hash

	    field = (String) nameHash.get(field);

	    System.out.println("And We have put gotten it from NameHash: " + field);
	    
	    // easy as pie
	  
	    if (myField == null){
       	      myField = this.defaultBase.getField(field);  

	      // Probably fix this...make it bring up an error dialog or
	      // something
   
	      inputField = new qTextField(6);
	    
	      return inputField; 

	    }

	    if (myField.isDate()){
	   
	      dateField = new TextField("dd/mm/yyyy");

	      return dateField;   
	    }

	    else if (myField.isBoolean()){
	    
	      Checkbox boolBox = new Checkbox("True");
	      boolBox.setState(true);

	      return boolBox;
	    }

	    else if (myField.isIP()){
	  
	      JIPField IPField = new JIPField(true); // allow V6

	      return IPField;
	    }

	    else {
	    
	      // It ain't no date
	    
	      inputField = new qTextField(6);
	    
	      return inputField; 
	    }
	  }      
      catch (RemoteException ex)
	{
	  throw new RuntimeException("caught remote exception: " + ex);	
	}
  }


  qChoice getOpChoice (String field)
    {
      /*
       * A choice menu will be returned, and that choice
       * will correspond to the legal choices for the 
       * field passed as the parameter to the method
       */
      
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

	  intChoice = new qChoice();
	  intChoice.add("="); 
	  intChoice.add(">="); 
	  intChoice.add("<="); 
	  intChoice.add("<"); 
	  intChoice.add(">");
  
	  // Do a nice null test to make sure stuff isn't screwey
	  
	  if (myField == null){
	    System.out.println("MYFIELD IS NULL! LIFE REALLY SUCKS!!");

	    return intChoice;
	    
	  }



	  if (myField.isDate() == true){
	  
	    dateChoice = new qChoice();
	    dateChoice.add("Same Day As");
	    dateChoice.add("Same Week As");
	    dateChoice.add("Same Month As");
	    dateChoice.add("Before");
	    dateChoice.add("After");

	    return dateChoice;   
	  }

	  else if (myField.isNumeric() == true){
	    //System.out.println("Field: It's a number!");
	    return intChoice; 
	  }
	
	  // NOTE - HANDLE VECTORS, IPs, etc


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

  void addRow (Component[] myRow, boolean visible, Panel myPanel, int Row) 
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
  
  void setRowVisible (Component[] myAry, boolean b)
    {
      /* This takes an array of components and sets their visibility values to
       * true or false, depending on the boolean value passed to the method
       */
      
      for (int i = 0; i < myAry.length; i++)
	{
	  if (myAry[i] != null)
	    {
	      myAry[i].setVisible(b);
	    }
	}
    }
  
  public Query myshow(){
    
    // Method to set the perm_editor to visible or invisible
    
    setSize(600,300);
    setVisible(true);  

    return this.returnVal; // once setVisible is set to false
                           // the program executes this return line
  }

  public void removeRow(Component[] myRow, Panel myPanel, int Row){
  
    for (int n = 0; n < myRow.length ; n++)
      {
	myPanel.remove(myRow[n]);
      }  
  
    this.Rows.removeElementAt(Row); // remove row from vector of rows
    this.row--;

  }
  
  public Query createQuery(){
    
    // * -- * //
    
    Query myQuery;
    QueryNode myNode, tempNode;
    QueryNotNode notNode;
    QueryDataNode dataNode;
    QueryAndNode andNode;
    Object value;
    byte opValue;
    Component[] tempAry;
    Choice tempChoice1,
           tempChoice2,
           tempChoice3;
    String notValue,
           fieldName,
           operator;
    TextField tempText = new TextField();
    TextField tempDate = new TextField();

    BaseField tempField;
    Integer tempInt = new Integer(0);
    Checkbox tempBox = new Checkbox();

    boolean editInPlace;
    Short baseID;

    // -- //

    int allRows = this.Rows.size();
    System.out.println("NUMBER OF ROWS: " + allRows);
       
    tempAry = (Component[]) this.Rows.elementAt(0); // This is the first row in the Vector
    tempChoice1 = (Choice) tempAry[1];
    fieldName = tempChoice1.getSelectedItem();
    tempChoice2 = (Choice) tempAry[3];
    notValue = tempChoice2.getSelectedItem();
    tempChoice3 =  (Choice) tempAry[5];
    operator = tempChoice3.getSelectedItem();
    
    Object tempObj = tempAry[7];

    if (tempObj instanceof qTextField)
      {
	tempText = (qTextField) tempAry[7];    
      }
    else if (tempObj instanceof Date)
      {
	tempDate = (qTextField) tempAry[7];
      }
    else if (tempObj instanceof Checkbox)
      {
	tempBox = (Checkbox) tempAry[7];
      }
    else 
      { // default
	tempText = (qTextField) tempAry[7];    
      }

    // -- set the type for the text entered in the TextField
    
    try
      {      
	/* Here's some code to deal with edit-in-place fields again
	 * what we need to do here is get the actual field from the 
	 * name, using the shortID of the base preovided by the
	 * fieldname Hash.
	 *
	 */ 

	baseID = (Short) baseIDHash.get(fieldName);
	
	if (baseID != null){
	  // The hash has a defined value for the base ID, therefore
	  // it's an edit in place

	  System.out.println("Creating Edit In Pace Query!");

	  tempField = (BaseField) fieldHash.get(fieldName);
	  fieldName = (String) nameHash.get(fieldName); // keep only the last field 
	  editInPlace = true;

	}

	else {	 
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
	    value = new Boolean(tempBox.getState());
	  }
	else 
	  {
	    value = tempText.getText(); // default is string
	  }
	//    }
	//catch (RemoteException ex)
	//{
	///throw new RuntimeException("caught remote exception: " + ex);	
	// }
    
    // -- get the correct operator
    
    if (operator == "="){
      opValue = 1;
    } else if (operator == "<"){
      opValue = 2;
    } else if (operator == "<="){
      opValue = 3;
    } else if (operator == ">") {
      opValue = 4;
    } else if (operator == ">=") {
      opValue = 5;
    } else {
      opValue = 7; // UNDEFINED
    }    
    
    // -- if not is true then add a not node
    
    dataNode = new QueryDataNode(fieldName, opValue, value);
    
    if (notValue == "is not")
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

	    if (notValue == "is not")
	      {
		myQuery = new Query(baseid, myNode, editOnly); // Use the NOT node (see above)
	  
	      } else { // "NOT" not selected
	  
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

	    if (notValue == "is not")
	      {
		myQuery = new Query(baseName, myNode, editOnly); // Use the NOT node (see above)
		
	      } else { // "NOT" not selected
		
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
	    tempChoice1 = (Choice) tempAry[1];
	    fieldName = tempChoice1.getSelectedItem();
	    tempChoice2 = (Choice) tempAry[3];
	    notValue = tempChoice2.getSelectedItem();
	    tempChoice3 =  (Choice) tempAry[5];
	    operator = tempChoice3.getSelectedItem();

	 
	    tempText = (TextField) tempAry[7];
    
	    // -- set the type for the text entered in the TextField

	    //try
	      // {      
		tempField = defaultBase.getField(fieldName);
		
		if (tempField.isNumeric())
		  {
		    value = new Integer(tempText.getText());
		  }
		else if (tempField.isDate())
		  {
		    value = new Date();
		  }
		else 
		  {
		    value = tempText.getText(); // default is string
		  }
		//     }
		//catch (RemoteException ex)
		//{
		//		throw new RuntimeException("caught remote exception: " + ex);	
		//}
	    

	    // -- get the correct operator
    
	    if (operator == "="){
	      opValue = 1;
	    } else if (operator == "<"){
	      opValue = 2;
	    } else if (operator == "<="){
	      opValue = 3;
	    } else if (operator == ">") {
	      opValue = 4;
	    } else if (operator == ">=") {
	      opValue = 5;
	    } else {
	      opValue = 7; // UNDEFINED
	    }     
 
	    // -- if not is true then add a not node
	    
	    dataNode = new QueryDataNode(fieldName, opValue, value);
	    
	    if (notValue == "is not")
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

    // We'll still use the highest base for the call to set the
    // return fields


    myQuery = setFields(myQuery);
    return myQuery;
    
        }
    catch (RemoteException ex)
      {
	throw new RuntimeException("caught remote exception: " + ex);	
      }
    

  }
  
  
  public Query setFields (Query someQuery)
    {
      BaseField tempField;
      short tempShort;
      String tempString;
      Checkbox tempBox;
      Vector tempVector;

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
		
		  tempBox = (Checkbox) tempVector.elementAt(y);

		  if (tempBox.getState() == true)
		    // the box has been checked -- we want this field
		  
		    {
		      tempString = tempBox.getLabel();
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
		    
		  else {

		    // else just skip this box
	      
		    System.out.println("Skipping " + tempBox.getLabel()); 
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
  // Event Handelers //
  /////////////////////
  
  public void actionPerformed(ActionEvent e){
     
    if (e.getSource() == displayButton){
      System.out.println("Field Display Selected");
      optionsFrame.setVisible(true);
    }
    
    if (e.getSource() == optionClose){
      optionsFrame.setVisible(false);
    }

    if (e.getSource() == OkButton) {
      System.out.println("You will submit");   
      optionsFrame.setVisible(false); // close the options frame if it isn't closed already
      this.returnVal = createQuery();
      setVisible(false);
    } else if (e.getSource() == CancelButton){
      System.out.println("Cancel was pushed");
      this.returnVal = null;
      setVisible(false);
    } 

    if (e.getSource() == addButton){

      addChoiceRow(defaultBase);
	
    }

    if (e.getSource() == removeButton){

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
  
  public void itemStateChanged(ItemEvent e)
    {
   
      /* -- */

      
      if (e.getSource() == editBox){
	
	this.editOnly = editBox.getState();
	System.out.println("Edit Box Clicked: " + editOnly);
	// IMPLEMENT ME!!!
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
	  
	  optionsFrame.removeAll();
	  optionsFrame.setVisible(false);
	  optionsFrame = createOptionFrame(defaultBase);	  
	}
	
      if (e.getSource() instanceof qfieldChoice){
	
	System.out.println("Field Selected");

	qfieldChoice source = (qfieldChoice) e.getSource();

	String fieldName = source.getSelectedItem();

	int currentRow = source.getRow();

	System.out.println("Current Row " + currentRow);

	Component[] tempRow = (Component[]) Rows.elementAt(currentRow); 
	
	if (tempRow == null){
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
	
      else if (e.getSource() instanceof qaryChoice){
	
	System.out.println("QCHOICE SELECTED!");
	
	qaryChoice source = (qaryChoice) e.getSource();
	
	String opName = source.getSelectedItem();
	System.out.println("Opname: " + opName);
	int currentRow = source.getRow();
	Component[] tempRow = (Component[]) Rows.elementAt(currentRow);
	
	if ((opName.equalsIgnoreCase("Contains")) || 
	    (opName.equalsIgnoreCase("Does Not Contain")))
	  {
	    // disable the numeric operator choice cause it makes
	    // no sense in this contaxt

	    if (tempRow[5].isEnabled())
	      {	
		tempRow[5].setEnabled(false);
	      }
	  }
	else
	  {
	    // make sure everything's enabled
	    if (! tempRow[5].isEnabled())
	      {
		tempRow[5].setEnabled(true);
	      }
	    
	  }
      }

      else 
	{
	  // ?? what the heck is it?  
	}

    }
}


/*------------------------------------------------------------------------------
                                                                           class 
                                                                    qfieldChoice

------------------------------------------------------------------------------*/

class qfieldChoice extends Choice {
  
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

class qbaseChoice extends Choice {
  
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

class qaryChoice extends Choice {
  
  int qRow; // keeps track of which row the choice menu is located in
            // (deja vu...)
 
  public int getRow()
    {
      return qRow;
    }

}
