/*

   JFilterDialog.java

   This class defines a dialog used to set filter options for queries
   by the client.
   
   Created: 3 March 1998
   Version: $Revision: 1.3 $ %D%
   Module By: Mike Mulvaney
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.client;

import arlut.csd.ganymede.*;
import arlut.csd.JDataComponent.*;

import javax.swing.*;

import java.awt.*;
import java.awt.event.*;
import java.rmi.*;
import java.util.Vector;

/*------------------------------------------------------------------------------
                                                                           class
                                                                   JFilterDialog

------------------------------------------------------------------------------*/

public class JFilterDialog extends JDialog implements ActionListener, JsetValueCallback{

  private final boolean debug = false;
  JButton clear, done;
  Vector filter, available = null;
  gclient gc;

  /* -- */

  public JFilterDialog(gclient gc)
  {
    super(gc, "Select Query Filter");

    this.gc = gc;

    filter = new Vector();

    getContentPane().setLayout(new BorderLayout());

    JLabel l =  new JLabel("Select owner groups to show.", JLabel.CENTER);
    JPanel lp = new JPanel(new BorderLayout());

    lp.add("Center", l);
    lp.setBorder(gc.statusBorderRaised);
    getContentPane().add("North", lp);

    try
      {
	available = gc.getSession().getOwnerGroups().getListHandles();
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not get Owner groups: " + rx);
      }
    
    StringSelector ss = new StringSelector(available, filter, this, true, true, true);
    ss.setCallback(this);

    getContentPane().add("Center", ss);

    clear = new JButton("Clear");
    clear.addActionListener(this);
    done = new JButton("Ok");
    done.addActionListener(this);
    
    JPanel p = new JPanel(false);
    p.setBorder(gc.statusBorderRaised);
    p.add(clear);
    p.add(done);
    
    getContentPane().add("South", p);

    setBounds(50,50,50,50);
    pack();
    show();
  }

  public boolean setValuePerformed(JValueObject e)
  {
    if (e.getOperationType() == JValueObject.ADD)
      {
	if (debug)
	  {
	    System.out.println("Adding element");
	  }

	filter.addElement(e.getValue());
      }
    else if (e.getOperationType() == JValueObject.DELETE)
      {
	if (debug)
	  {
	    System.out.println("removing element");
	  }

	filter.removeElement(e.getValue());
	
      }	
    return true;
  }

  public void actionPerformed(ActionEvent e)
  {
    if (e.getSource() == clear)
      {
	System.out.println("clear this!");
      }
    else if (e.getSource() == done)
      {
	try
	  {
	    ReturnVal retVal = gc.getSession().filterQueries(filter);
	    gc.handleReturnVal(retVal);

	    if ((retVal == null) || (retVal.didSucceed()))
	      {
		this.setVisible(false);
	      }
	    else
	      {
		this.setVisible(false);
		gc.showErrorMessage("Could not set filter query.");
	      }
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Could not set filter: " + rx);
	  }
      }
  }
}
