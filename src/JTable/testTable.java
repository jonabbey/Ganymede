/*
  testTable.java

  A test framework for the baseTable GUI component

  Created: 5 June 1996
  Version: $Revision: 1.1 $ %D%
  Module By: Jonathan Abbey -- jonabbey@arlut.utexas.edu
  Applied Research Laboratories, The University of Texas at Austin

*/

import java.awt.*;
import java.applet.*;
import java.util.*;
import csd.Table.*;

/*------------------------------------------------------------------------------
                                                                    public class
                                                                       testTable

------------------------------------------------------------------------------*/

public class testTable extends Applet {

  gridTable table;

  static testTable applet = null;
  static Frame frame = null;

  Panel p1 = null;
  TextField statusField = null;

  String headers[] = {"User", "Status", "Random 1", "Random 2"};
  int colWidths[] = {100,100,100,100};

  /* -- */
  
  public void init() 
  {
  }

  public void resize() 
  {
    resize(600,300);
  }

  // Our primary constructor.  This will always be called, either
  // from main(), below, or by the environment building our applet.

  public testTable() 
  {
    setLayout(new BorderLayout());

    statusField = new TextField("baseTable Testing", 40);
    statusField.setEditable(false);
    statusField.setBackground(Color.red);
    statusField.setForeground(Color.white);
    add("North", statusField);

    table = new gridTable(colWidths, headers);
    add("Center", table);
  }

  public void start()
  {
    table.setCellText(0,0, "jonabbey",false);
    table.setCellText(1,0, "okay",false);
    table.setCellText(2,0, "3.14159",false);
    table.setCellText(3,0, "Pineapples in summer",true);
    table.setCellText(0,1, "root",false);
    table.setCellText(1,1, "anonymous",false);
    table.setCellText(2,1, "csdsun1.arlut.utexas.edu",false);
    table.setCellText(3,1, "Alaska",true);
    table.setCellText(0,2, "navin",false);
    table.setCellText(1,2, "student",false);
    table.setCellText(2,2, "Java",false);
    table.setCellText(3,2, "Computer Science Dept.",true);
    table.setCellText(0,3, "imkris",false);
    table.setCellText(1,3, "full time",false);
    table.setCellText(2,3, "Accounting",false);
    table.setCellText(3,3, "Texas A&M",true);
    table.setCellText(0,4, "jonabbey",false);
    table.setCellText(1,4, "okay",false);
    table.setCellText(2,4, "3.14159",false);
    table.setCellText(3,4, "Pineapples in summer",true);
    table.setCellText(0,5, "root",false);
    table.setCellText(1,5, "anonymous",false);
    table.setCellText(2,5, "csdsun1.arlut.utexas.edu",false);
    table.setCellText(3,5, "Alaska",true);
    table.setCellText(0,6, "navin",false);
    table.setCellText(1,6, "student",false);
    table.setCellText(2,6, "Java",false);
    table.setCellText(3,6, "Computer Science Dept.",true);
    table.setCellText(0,7, "imkris",false);
    table.setCellText(1,7, "full time",false);
    table.setCellText(2,7, "Accounting",false);
    table.setCellText(3,7, "Texas A&M",true);
  }

  public static void main(String[] argv)
  {
    /* Define the applet */

    Frame frame = new Frame("baseTable Test");
    applet = new testTable();

    /* present the applet */

    frame.add("Center", applet);
    frame.resize(300, 300);
    frame.show();  
    applet.init();
    applet.start();
  }
}
 
