/*

   JDefaultOwnerDialog.java

   A dialog to choose filtering options.
   
   Created: ??
   Version: $Revision: 1.2 $ %D%
   Module By: Mike Mulvaney
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.client;

import arlut.csd.ganymede.*;
import arlut.csd.JDataComponent.*;
import arlut.csd.JDialog.JErrorDialog;

import javax.swing.*;

import java.awt.*;
import java.awt.event.*;
import java.rmi.*;
import java.util.Vector;

/*------------------------------------------------------------------------------
                                                                           class
                                                             JDefaultOwnerDialog

------------------------------------------------------------------------------*/

public class JDefaultOwnerDialog extends JDialog implements ActionListener, JsetValueCallback{

  private final boolean debug = false;

  JButton done;

  Vector
    chosen = new Vector(),
    available;

  gclient gc;

  ReturnVal retVal = null;

  public JDefaultOwnerDialog(gclient gc, Vector groups)
    {
      super(gc, "Select DefaultOwner", true);

      this.gc = gc;
      this.available = groups;

      getContentPane().setLayout(new BorderLayout());
      getContentPane().add("North", new JLabel("Select default owner for new objects"));

      // Maybe I should use null instead of chosen?
      StringSelector ss = new StringSelector(available, chosen, this, true, true, true);
      ss.setCallback(this);
      getContentPane().add("Center", ss);

      done = new JButton("Ok");
      done.addActionListener(this);

      JPanel p = new JPanel();
      p.add(done);
      p.setBorder(gc.raisedBorder);

      getContentPane().add("South", p);

      setBounds(50,50,50,50);
      pack();

    }

  public boolean setValuePerformed(JValueObject e)
  {
    if (e.getOperationType() == JValueObject.ADD)
      {
	if (debug)
	  {
	    System.out.println("Adding element");
	  }
	chosen.addElement(e.getValue());
      }
    else if (e.getOperationType() == JValueObject.DELETE)
      {
	if (debug)
	  {
	    System.out.println("removing element");
	  }
	chosen.removeElement(e.getValue());
      }

    return true;
  }

  public void actionPerformed(ActionEvent e)
  {
    if (e.getSource() == done)
      {
	if (chosen.size() == 0)
	  {
	    JErrorDialog d = new JErrorDialog(gc, "You must choose a default owner group.");
	    return;
	  }

	try
	  {
	    retVal = gc.getSession().setDefaultOwner(chosen);
	    
	    gc.handleReturnVal(retVal);

	    if ((retVal == null) || retVal.didSucceed())
	      {
		this.setVisible(false);
	      }
	    else
	      {
		System.out.println("Could not set default owner.");
		this.setVisible(false);
	      }
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Could not set filter: " + rx);
	  }
      }

  }

  /**
   * Shows the dialog, and returns the ReturnVal/
   *
   * Use this instead of setVisible().
   */
  public ReturnVal chooseOwner()
  {
    setVisible(true);

    return retVal;
  }
}
