/*

   datePanel.java

   The tab that holds date information.
   
   Created: 9 September 1997
   Version: $Revision: 1.2 $ %D%
   Module By: Michael Mulvaney
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.client;

import java.awt.*;
import java.rmi.*;
import java.util.*;

import com.sun.java.swing.*;
import tablelayout.*;
import arlut.csd.ganymede.*;

import arlut.csd.JDataComponent.*;

public class datePanel extends JBufferedPane {

  boolean 
    editable;

  int
    row = 0;

  public datePanel(date_field expiration, date_field removal, boolean editable)
  {
    this.editable = editable;

    TableLayout layout = new TableLayout(false);
    setLayout(layout);

    JdateField rem_df = new JdateField();
    JdateField exp_df = new JdateField();

    //objectHash.put(df, field);
    exp_df.setEditable(editable);
    rem_df.setEditable(editable);
    
    try
      {
	if (expiration != null)
	  {
	    Date exp_date = ((Date)expiration.getValue());
	    
	    if (exp_date != null)
	      {
		exp_df.setDate(exp_date);
	      }
	  }
	
	if (removal != null)
	  {
	    Date rem_date = ((Date)removal.getValue());
	    
	    if (rem_date != null)
	      {
		rem_df.setDate(rem_date);
	      }
	  }
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not get date: " + rx);
      }
    
    // note that we set the callback after we initially set the
    // date, to avoid having the callback triggered on a listing

    //df.setCallback(this);
    
    try
      {
	if (expiration != null)
	  {
	    addRow(exp_df, expiration.getName());
	  }
	else
	  {
	    addRow(new JLabel("No expiration date is set"), "");
	  }
	if (removal != null)
	  {
	    addRow(rem_df, removal.getName());
	  }
	else
	  {
	    addRow(new JLabel("No removal date is set"), "");
	  }
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not check visibility");
      }
    
  }
  
  void addRow(Component comp,  String label)
    {
      JLabel l = new JLabel(label);
      comp.setBackground(ClientColor.ComponentBG);
      add("0 " + row + " lthwHW", l);
      add("1 " + row + " lthwHW", comp);
      
      row++;
    }


}//datePanel
