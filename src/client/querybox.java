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

  qChoice baseChoice = new qChoice();
  
  // - text fields

  qTextField inputField = new qTextField(6);

  // - misc. variables

  int row = 0;
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


  qChoice getChoiceFields (Base base)
    {
      /* Method to return a choice menu containing the fields for
       * a particular base
       */

    BaseField basefield;
    qChoice myChoice = new qChoice();
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

  qChoice isNot = new qChoice();
      isNot.add("is");
      isNot.add("is not");
      
  qChoice selectedOp = new qChoice();    

  qTextField newInput = new qTextField(6); 

  //Label separate; // used purely for aesthetics

      //* -- *//

      Choice tempo = getChoiceFields(base);
      myAry[0] = tempo;
      currentField = tempo.getSelectedItem();  
      selectedOp = getOpChoice(currentField);

      myAry[1] = new Label("   ");
      myAry[2] = isNot; 
      myAry[3] = new Label("   ");
      myAry[4] = selectedOp; 
      myAry[5] = new Label("   ");
      myAry[6] = newInput; 
      
      addRow(this.myAry, true, inner_choice);
     
      
    }
  
  qChoice getOpChoice (String field)
    {
      /*
       * A choice menu will be returned, and that choice
       * will correspond to the legal choices for the 
       * field passed as the parameter to the method
       */
      
     qChoice intChoice = new qChoice();
        intChoice.add("="); 
        intChoice.add(">="); 
        intChoice.add("<="); 
        intChoice.add("<"); 
        intChoice.add(">");
  
      qChoice dateChoice = new qChoice();
	dateChoice.add("Same Day As");
	dateChoice.add("Same Week As");
	dateChoice.add("Same Month As");
	dateChoice.add("Before");
	dateChoice.add("After");

	return intChoice;  // This WILL return the correct choice
    }


  void addRow (Component[] myRow, boolean visible, Panel myPanel) 
    {
      for (int n = 0; n < myRow.length; n++)
	{
	  myRow[n].setVisible(visible);
	  myPanel.add( n + "  " + this.row + " lhwHW", myRow[n]);
	}
      
      this.row++;
      System.out.println("Row: " + this.row);
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
  
  public void actionPerformed(ActionEvent e){
     
    if (e.getSource() == OkButton) {
      System.out.println("Ok was pushed");      
      myshow(false);
      return;
    } else if (e.getSource() == CancelButton){
      System.out.println("Cancel was pushed");
      myshow(false);
      return;
    } 

    if (e.getSource() == addButton){

    System.out.println("Add Button Clicked");
    System.out.println("Here's the defaultBase: " + defaultBase);
	
    }

    if (e.getSource() == removeButton){
     
    System.out.println("Remove Button Clicked");
    System.out.println("Here's the defaultBase: " + defaultBase);
	  
    }
    
  } 
  
  public void removeRow(Component[] myRow, Panel myPanel){


    for (int n = 0; n < myRow.length; n++)
      {
	myPanel.remove(myRow[n]);
      }
    
    this.row--;
    System.out.println("Row: " + this.row);
  }


  public void itemStateChanged(ItemEvent e)
    {
   
      /* -- */

      /* Ok, we're assuming that the change is coming from 
       * base choice menu. Not a bad assumption, but we'll have 
       * incluce a couple other possibilities: the editable
       * checkbox and the fields choice menu.
       */
      
      if (e.getSource() == baseChoice)
	{

	  // First, change the base
	  
	  Base defaultBase = (Base) myHash.get(baseChoice.getSelectedItem());
	  this.defaultBase = defaultBase;

	  // remove for all entries in vector of component arrays

	  removeRow(myAry, inner_choice);
	  myAry[0] = getChoiceFields(defaultBase);
	  addRow(myAry, true, inner_choice);
	 
	  // remove all rows. Then, 
	    
	  Rows = null; // Kill the current vector of Rows
		  
	}
	
      //if (compString.equals("Group")){
      //    {
      //      System.out.println("Ninja: " + myChoice.getSelectedItem());
      //    }
	 
    }
}


/*------------------------------------------------------------------------------
                                                                           class 
                                                                         qChoice

------------------------------------------------------------------------------*/

class qChoice extends Choice {
  
  int qRow; // keps track of which row the choice menu is located in
  
  public int getRow()
    {
      return this.qRow;
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
      return this.qRow;
    }

}
