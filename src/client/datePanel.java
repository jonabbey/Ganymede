/*

   datePanel.java

   The tab that holds date information.
   
   Created: 9 September 1997
   Version: $Revision: 1.3 $ %D%
   Module By: Michael Mulvaney
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.client;

import java.awt.*;
import java.awt.event.*;
import java.rmi.*;
import java.util.*;

import com.sun.java.swing.*;
import tablelayout.*;
import arlut.csd.ganymede.*;

import arlut.csd.JDataComponent.*;
import arlut.csd.JCalendar.*;

public class datePanel extends JBufferedPane implements ActionListener, JsetValueCallback {

  boolean 
    editable;

  int
    row = 0;

  date_field
    expiration,
    removal;

  JBufferedPane
    expirationPane,
    removalPane;

  JBorderedPane
    expirationBorder,
    removalBorder;

  JpopUpCalendar 
    pCal = null;

  protected GregorianCalendar 
    _myCalendar;

  protected SimpleTimeZone
    _myTimeZone = (SimpleTimeZone)(SimpleTimeZone.getDefault());

  public datePanel(date_field expiration, date_field removal, boolean editable)
  {
    this.editable = editable;
    this.expiration = expiration;
    this.removal = removal;

    setInsets(new Insets(5,5,5,5));

    expirationPane = new JBufferedPane();

    //expirationPane.setLayout(new BorderLayout());
    expirationBorder = new JBorderedPane();
    expirationBorder.setLayout(new BorderLayout());
    expirationBorder.setBorder(BorderFactory.createTitledBorder(expirationPane, "Expiration Date"));
    expirationBorder.add("Center", expirationPane);

    removalPane = new JBufferedPane(); 

    removalBorder = new JBorderedPane();
    removalBorder.setLayout(new BorderLayout());
    removalBorder.setBorder(BorderFactory.createTitledBorder(removalPane, "Removal Date"));
    removalBorder.add("Center", removalPane);

    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    add(expirationBorder);
    add(removalBorder);
    add(Box.createVerticalGlue());

    if (editable)
      {
	create_editable_panel();
      }
    else
      {
	create_non_editable_panel();
      }
  }

  void create_editable_panel()
  {
    JdateField rem_df = new JdateField();
    JdateField exp_df = new JdateField();
    
    //objectHash.put(df, field);
    exp_df.setEditable(editable);
    rem_df.setEditable(editable);

    _myCalendar = new GregorianCalendar(_myTimeZone,Locale.getDefault());
    
    try
      {
	//
	// First expiration stuff
	//

	if ((expiration != null) && (expiration.getValue() != null))
	  {
	    Date exp_date = ((Date)expiration.getValue());
	    
	    if (exp_date != null)
	      {
		exp_df.setDate(exp_date);
	      }
	    
	    expirationPane.add("Center", exp_df);
	  }
	else
	  {
	    JButton new_exp = new JButton("Set expiration date");
	    new_exp.addActionListener(this);
	    new_exp.setActionCommand("set_expiration");
	    expirationPane.add("South", new_exp);
	    JpanelCalendar cal = new JpanelCalendar(_myCalendar, this);
	    expirationPane.add("Center", cal);

	  }

	//
	//  Now Removal stuff
	//
	
	if ((removal != null) && (removal.getValue() != null))
	  {
	    Date rem_date = ((Date)removal.getValue());
	    
	    if (rem_date != null)
	      {
		rem_df.setDate(rem_date);
	      }
	    
	    removalPane.add(rem_df);
	  }
	else
	  {
	    JButton new_rem = new JButton("Set removal date");
	    new_rem.setActionCommand("set_removal");
	    new_rem.addActionListener(this);
	    removalPane.add(new_rem);
	  }
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not get date: " + rx);
      }
    
    // note that we set the callback after we initially set the
    // date, to avoid having the callback triggered on a listing

    //df.setCallback(this);

  }

  void create_non_editable_panel()
  {
    try
      {
	if (expiration != null)
	  {
	    expirationPane.add("Center", new JLabel(((Date)expiration.getValue()).toString()));
	  }
	else
	  {
	    expirationPane.add("North", new JLabel("No expiration date is set"));
	  }
	if (removal != null)
	  {
	    removalPane.add(new JLabel(((Date)removal.getValue()).toString()));
	  }
	else
	  {
	    removalPane.add(new JLabel("No removal date is set"));
	  }
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not check visibility");
      }
    
  }
  
  public void actionPerformed(ActionEvent e)
    {
      System.out.println("Action performed in datePanel");
      if (pCal == null)
	{
	  pCal = new JpopUpCalendar(_myCalendar,this);
	}
      
      if (pCal.isVisible())
	{
	  pCal.setVisible(false);
	}
          
      if (e.getActionCommand().equals("set_expiration"))
	{
	  System.out.println("Set expiration date clicked");

	  pCal.show();

	}
      else if (e.getActionCommand().equals("set_removal"))
	{
	  System.out.println("Set removal date clicked");

	  pCal.show();
	}
      else
	{
	  System.out.println("Unknown action command in datePanel");
	}

    }

  public boolean setValuePerformed(JValueObject o)
    {
      if (o.getSource() == pCal)
	{
	  System.out.println("Calendar says: " + ((Date)o.getValue()).toString());
	}
      return true;
    }


}//datePanel
