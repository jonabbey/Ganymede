/*
  testTable.java

  A test framework for the baseTable GUI component

  Copyright (C) 1997  The University of Texas at Austin.

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation; either version 2 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software
  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.

  Created: 5 June 1996
  Version: $Revision: 1.8 $ %D%
  Module By: Jonathan Abbey -- jonabbey@arlut.utexas.edu
  Applied Research Laboratories, The University of Texas at Austin

*/

import java.awt.*;
import java.awt.event.*;
import java.applet.*;
import java.util.*;
import arlut.csd.JTable.*;

/*------------------------------------------------------------------------------
                                                                    public class
                                                                       testTable

------------------------------------------------------------------------------*/

public class testTable extends Applet implements arlut.csd.JTable.rowSelectCallback, ActionListener {

  static final boolean debug = false;
  static testTable applet = null;
  static Frame frame = null;

  static final int Xsize = 600;
  static final int Ysize = 300;

  /* - */

  arlut.csd.JTable.rowTable table;

  testBorder tBorder = null;
  TextField statusField = null;
  Panel southPanel = null;
  TextField labelField = null;
  Button resize = null;

  String headers[] = {"User", "Status", "Random 1", "Random 2"};
  int colWidths[] = {100,100,100,100};

  /* -- */
  
  public void init() 
  {
    if (debug)
      {
	System.err.println("testTable.init()");
      }

    table.newRow("jonabbey");
    table.setCellText("jonabbey", 0, "jonabbey",false);
    table.setCellText("jonabbey", 1, "okay",false);
    table.setCellText("jonabbey", 2, "3.14159",false);
    table.setCellText("jonabbey", 3, "Pineapples in summer",true);
    table.newRow("root");
    table.setCellText("root", 0, "root",false);
    table.setCellText("root", 1, "anonymous",false);
    table.setCellText("root", 2, "csdsun1.arlut.utexas.edu",false);
    table.setCellText("root", 3, "Alaska",true);
    table.newRow("navin");
    table.setCellText("navin", 0, "navin",false);
    table.setCellText("navin", 1, "student",false);
    table.setCellText("navin", 2, "Java",false);
    table.setCellText("navin", 3, "Computer Science Dept.",true);
    table.newRow("imkris");
    table.setCellText("imkris", 0, "imkris",false);
    table.setCellText("imkris", 1, "full time",false);
    table.setCellText("imkris", 2, "Accounting",false);
    table.setCellText("imkris" ,3, "Texas A&M",true);
    table.newRow("jonabbey2");
    table.setCellText("jonabbey2", 0, "jonabbey2",false);
    table.setCellText("jonabbey2", 1, "okay",false);
    table.setCellText("jonabbey2", 2, "3.14159",false);
    table.setCellText("jonabbey2", 3, "Pineapples in summer",true);
    table.newRow("root2");
    table.setCellText("root2", 0, "root2",false);
    table.setCellText("root2", 1, "anonymous",false);
    table.setCellText("root2", 2, "csdsun1.arlut.utexas.edu",false);
    table.setCellText("root2", 3, "Alaska",true);
    table.newRow("navin2");
    table.setCellText("navin2", 0, "navin2",false);
    table.setCellText("navin2", 1, "student",false);
    table.setCellText("navin2", 2, "Java",false);
    table.setCellText("navin2", 3, "Computer Science Dept.",true);
    table.newRow("imkris2");
    table.setCellText("imkris2", 0, "imkris2",false);
    table.setCellText("imkris2", 1, "full time",false);
    table.setCellText("imkris2", 2, "Accounting",false);
    table.setCellText("imkris2" ,3, "Texas A&M",true);


    if (debug)
      {
	System.err.println("exiting testTable.init()");
      }
  }

  public void resize() 
  {
    resize(Xsize, Ysize);
  }

  // Our primary constructor.  This will always be called, either
  // from main(), below, or by the environment building our applet.

  public testTable() 
  {
    setLayout(new BorderLayout());

    statusField = new TextField("rowTable Testing", 40);
    statusField.setEditable(false);
    statusField.setBackground(Color.red);
    statusField.setForeground(Color.white);
    add("North", statusField);

    if (debug)
      {
	System.err.println("testTable constructor: constructing gridTable");
      }

    table = new arlut.csd.JTable.rowTable(colWidths, headers, this, null);

    if (debug)
      {
	System.err.println("testTable constructor: constructed gridTable");
      }

    tBorder = new testBorder(Xsize, Ysize, table);

    add("Center", tBorder);

    if (debug)
      {
	System.err.println("testTable constructor: table added to applet");
      }
    
    southPanel = new Panel();
    southPanel.setLayout(new BorderLayout());
    
    labelField = new TextField("boop", 30);
    labelField.setEditable(false);
    labelField.setBackground(SystemColor.text);
    labelField.setForeground(SystemColor.textText);

    resize = new Button("resize");
    resize.addActionListener(this);

    southPanel.add("West", labelField);
    southPanel.add("East", resize);

    add("South", southPanel);
    
  }

  public void start()
  {

  }

  // ActionListener methods

  public void actionPerformed(java.awt.event.ActionEvent event)
  {
    // we want to resize the table

    tBorder.randomize();
    this.invalidate();
    this.validate();
  }

  // rowSelectCallback methods

  public void rowSelected(Object key)
  {
    labelField.setText(key + " selected");
  }

  public void rowDoubleSelected(Object key)
  {
    labelField.setText(key + " double selected");
  }

  public void rowUnSelected(Object key, boolean endSelected)
  {
    labelField.setText(key + " unselected");
  }

  public void rowMenuPerformed(Object key, ActionEvent e)
  {
  }

  public static void main(String[] argv)
  {
    /* Define the applet */

    Frame frame = new Frame("baseTable Test");
    applet = new testTable();

    /* present the applet */

    if (debug)
      {
	System.err.println("XX adding applet to frame");
      }

    frame.add("Center", applet);

    if (debug)
      {
	System.err.println("XX resizing frame");
      }

    frame.resize(300, 300);

    if (debug)
      {
	System.err.println("XX showing frame");
      }

    frame.show();  

    applet.init();

    if (debug)
      {
	System.err.println("XX starting applet");
      }

    applet.start();
  }
}
 
