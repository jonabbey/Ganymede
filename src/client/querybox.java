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

  static final boolean debug = false;

  // --

  Hashtable baseHash, changeHash;

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

  // - Choice menu

  Choice baseChoice = new Choice();
  
  // - variables

  int row = 0;  

  /* -- */


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
      baseChoice.addItem("first Base");
      baseChoice.addItem("second Base");      
      baseChoice.setBackground(Color.white);
      base_panel.add(baseChoice);
      base_panel.add(new Label("     "));
      base_panel.add(new Checkbox("Editable"));

      choice_pane.setSize(100,100);
      inner_choice.setLayout(new TableLayout(false));
      inner_choice.setBackground(Color.white);
      choice_pane.add(inner_choice);

      query_panel.add("Center", choiceBox);
      query_panel.add("North", baseBox);
   
      this.myshow(true);
    }

  public querybox (Hashtable baseHash, Frame parent, String myTitle) {
    this(null, baseHash, parent, myTitle);
  } 

 
  void addRow (Component[] myRow, boolean visible, Panel myPanel) 
    {
      for (int n = 0; n < myRow.length; n++)
	{
	  myRow[n].setVisible(visible);
	  myPanel.add( n + "  " + this.row + " lhwHW", myRow[n]);
	}
      
      this.row++;
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
  } 
  
  public void itemStateChanged(ItemEvent e)
    {
   
      /* -- */

      // To handle the state of the checkboxes
    
    
    }

}


