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

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.rmi.RemoteException;

import tablelayout.*;

//import com.sun.java.swing.*;

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
  static final int MAXCOMPONENTS = 7; // the number of components that
                                      // can appear in a query choice

  // --

  Hashtable 
  
    baseHash;

   // - Buttons
  
  Button OkButton = new Button ("Submit");
  Button CancelButton = new Button("Cancel");
  Button addButton = new Button("Add Choices");
  Button removeButton = new Button("Remove Choices");
 
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
  qfieldChoice fieldChoice; // but there's libel to be a whole clew of these
  qChoice opChoice; // and these too
  qChoice intChoice;
  qChoice dateChoice;

  // - text fields

  qTextField inputField = new qTextField(6);

  // - misc. variables

  int row = 0;
  boolean editOnly;
  Base defaultBase;
  Component[] myAry = new Component[MAXCOMPONENTS]; // stores a single row
  Vector Rows = new Vector(); // store the rows 
  Hashtable myHash = new Hashtable(); // allows you to look up a base with its
                                      // name as the key
 
  // - these strings store all the important stuff about the current choice row

  String
    currentBase,
    currentField,
    currentOp,
    currentText;

  /* -- */


  //////////////////
  // Constructors //
  //////////////////

  public querybox (Base defaultBase, Hashtable baseHash, Frame parent, String DialogTitle)
    {
      super(parent, DialogTitle, true); // the boolean value is to make the dialog modal
      
      Vector Bases;
     
      /* -- */
      
      // Main constructor for the querybox window
      
      this.baseHash = baseHash;  
      this.defaultBase = defaultBase;

      // - Define the main window

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
	    String choiceToAdd = new String(key.getName());
	    baseChoice.addItem(choiceToAdd);
	    myHash.put(choiceToAdd, key);
	  }
	  
	  if (defaultBase != null)
	    {
	      baseChoice.select(defaultBase.getName()) ; 
	      System.out.println("Default base given: " + defaultBase);
	    }
	  else 
	    { 
	      // no default given. pick the one that's there.
	    
	      currentBase = baseChoice.getSelectedItem();
	      this.defaultBase = (Base) myHash.get(currentBase);
	      defaultBase = this.defaultBase;
	      String test = defaultBase.getName();
	    }
	}
      catch (RemoteException ex)
	{
	  throw new RuntimeException("caught remote exception: " + ex);
	}
         
      baseChoice.setBackground(Color.white);
      baseChoice.addItemListener(this);
      base_panel.add(baseChoice);
      base_panel.add(new Label("     "));
      base_panel.add(editBox);

      choice_pane.setSize(100,100);
      inner_choice.setLayout(new TableLayout(false));
      inner_choice.setBackground(Color.white);
      choice_pane.add(inner_choice);

      query_panel.add("Center", choiceBox);
      query_panel.add("North", baseBox);
      
      addChoiceRow(defaultBase); // adds the initial row
    }

  public querybox (Hashtable baseHash, Frame parent, String myTitle) {
    this(null, baseHash, parent, myTitle);
  } 

 
  ////////////////
  //   Methods  //
  ////////////////


  qfieldChoice getChoiceFields (Base base)
    {
      /* Method to return a choice menu containing the fields for
       * a particular base
       */

    BaseField basefield;
    qfieldChoice myChoice = new qfieldChoice();
    myChoice.addItemListener(this);

    myChoice.qRow = this.row;

    int i = 0; // counter variable

    try
      {
	Vector fields = base.getFields();
      	for (int j=0; fields != null && (j < fields.size()); j++) 
	  {
	    basefield = (BaseField) fields.elementAt(j);
	    String Name = basefield.getName();
	    myChoice.add(Name);
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

      qChoice isNot = new qChoice();
      isNot.add("is");
      isNot.add("is not");
      isNot.qRow = this.row;

      qTextField newInput = new qTextField(6); 
      newInput.qRow = this.row;

      //* -- *//

      fieldChoice = getChoiceFields(base);
      myAry[0] = fieldChoice;
      currentField = fieldChoice.getSelectedItem();  
      opChoice = getOpChoice(currentField);
      opChoice.qRow = this.row;

      myAry[1] = new Label("   ");
      myAry[2] = isNot; 
      myAry[3] = new Label("   ");
      myAry[4] = opChoice; 
      myAry[5] = new Label("   ");
      myAry[6] = newInput; 
      
      addRow(myAry, true, inner_choice, this.row);
         
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
	  BaseField myField = this.defaultBase.getField(field);  
      
	  intChoice = new qChoice();
	  intChoice.add("="); 
	  intChoice.add(">="); 
	  intChoice.add("<="); 
	  intChoice.add("<"); 
	  intChoice.add(">");
  
	  if (myField.isDate() == true){
	    //System.out.println("Field: It's a date!!!!");

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
	
	  else 
	    {
	      return intChoice; // FIX ME
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
	  myRow[n].setVisible(visible);
	  myPanel.add( n + "  " + Row + " lhwHW", myRow[n]);
	}

      this.row ++;
      this.Rows.setSize(this.row);
      this.Rows.setElementAt(myRow, this.row - 1); // add component array to vector


      inner_choice.invalidate();
      inner_choice.validate();
     
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
  
  public void myshow(boolean truth_value){
    
    // Method to set the perm_editor to visible or invisible
    
    setSize(500,500);
    setVisible(truth_value); 
  }

  public void removeRow(Component[] myRow, Panel myPanel){
  

    for (int n = 0; n < myRow.length ; n++)
      {
	myPanel.remove(myRow[n]);
      }
    
    this.Rows.removeElementAt(this.row - 1); // remove row from vector of rows
    this.row--;
    System.out.println("Total Number of rows: " + this.row);
    this.Rows.setSize(this.row);
  }
  

  public Query createQuery(){

    Query myQuery;
    int allRows = this.Rows.size();
    System.out.println("Look at How man Rows we have: " + allRows);

    if (allRows == 1)
      {
	// Special case -- return only a single query node

	Component[] tempAry = (Component[]) this.Rows.elementAt(allRows - 1);
	Choice tempChoice1 = (Choice) tempAry[0];
	String fieldName = tempChoice1.getSelectedItem();
	Choice tempChoice2 =  (Choice) tempAry[4];
	String operator = tempChoice2.getSelectedItem();
	TextField tempText = (TextField) tempAry[6];
	Object value = tempText.getText();

	//QeuryDataNode singleData = 

	myQuery = new Query("loser");


      }

    else 
      {
	myQuery = new Query("loser");

	for (int i = 1; i > allRows; i ++)
	  {
	    // create the query

	  }
      }

    return myQuery;
    
  }


  /////////////////////
  // Event Handelers //
  /////////////////////
  
  public void actionPerformed(ActionEvent e){
     
    if (e.getSource() == OkButton) {
      System.out.println("You will submit");      
      createQuery();
      myshow(false);
      return;
    } else if (e.getSource() == CancelButton){
      System.out.println("Cancel was pushed");
      myshow(false);
      return;
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
	  removeRow(tempAry, inner_choice);
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
	
      }

      if (e.getSource() == baseChoice)
	{

	  // First, change the base
	  
	  Base defaultBase = (Base) myHash.get(baseChoice.getSelectedItem());
	  this.defaultBase = defaultBase;

	  // remove for all entries in vector of component arrays

	  for (int i = this.row - 1; i > -1; i--)
	    {
	      Component[] tempRow = (Component[]) this.Rows.elementAt(i);
	      removeRow(tempRow, inner_choice);
	      System.out.println("Removing Row: " + i);
	      this.Rows.setSize(i);
	    }
	  
	  addChoiceRow(defaultBase);
   	  
	}
	
      if (e.getSource() instanceof qfieldChoice){

	qfieldChoice source = (qfieldChoice) e.getSource();

	String fieldName = source.getSelectedItem();

	int currentRow = source.getRow();

	System.out.println("Current ROw " + currentRow);

	Component[] tempRow = (Component[]) Rows.elementAt(currentRow); 
	
	removeRow(tempRow, inner_choice);
	opChoice = getOpChoice(fieldName);
	tempRow[4] = opChoice;
	addRow(tempRow, true, inner_choice, currentRow);
	this.invalidate();
	this.validate();
      }
	 
    }
}


/*------------------------------------------------------------------------------
                                                                           class 
                                                                    qfieldChoice

------------------------------------------------------------------------------*/

class qfieldChoice extends Choice {
  
  int qRow; // keps track of which row the choice menu is located in
  
  public int getRow()
    {
      return qRow;
    }

}

/*------------------------------------------------------------------------------
                                                                           class 
                                                                         qChoice

------------------------------------------------------------------------------*/

class qbaseChoice extends Choice {
  
  int qRow; // keps track of which row the choice menu is located in
  
  public int getRow()
    {
      return qRow;
    }

}

/*------------------------------------------------------------------------------
                                                                           class 
                                                                      qTextField

------------------------------------------------------------------------------*/

class qTextField extends TextField {
  
  int qRow; // keeps track of which row the choice menu is located in

  public qTextField(int size)
    {
      super(size); // Would you like that super-sized for just 39 cents?
    }

  public int getRow()
    {
      return qRow;
    }

}
