/*

   JDefaultOwnerDialog.java

   A dialog to choose filtering options.
   
   Created: ??
   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

   Module By: Mike Mulvaney

   -----------------------------------------------------------------------
	    
   Directory Droid Directory Management System
 
   Copyright (C) 1996 - 2004
   The University of Texas at Austin

   Contact information

   Author Email: ganymede_author@arlut.utexas.edu
   Email mailing list: ganymede@arlut.utexas.edu

   US Mail:

   Computer Science Division
   Applied Research Laboratories
   The University of Texas at Austin
   PO Box 8029, Austin TX 78713-8029

   Telephone: (512) 835-3200

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
   Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
   02111-1307, USA

*/

package arlut.csd.ddroid.client;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.RemoteException;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import arlut.csd.JDataComponent.JAddValueObject;
import arlut.csd.JDataComponent.JAddVectorValueObject;
import arlut.csd.JDataComponent.JDeleteValueObject;
import arlut.csd.JDataComponent.JDeleteVectorValueObject;
import arlut.csd.JDataComponent.JValueObject;
import arlut.csd.JDataComponent.JsetValueCallback;
import arlut.csd.JDataComponent.StringSelector;
import arlut.csd.JDialog.JErrorDialog;
import arlut.csd.ddroid.common.ReturnVal;

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
      StringSelector ss = new StringSelector(this, true, true, true);
      ss.update(available, true, null, chosen, true, null);
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
    if (e instanceof JAddValueObject)
      {
	if (debug)
	  {
	    System.out.println("Adding element");
	  }

	chosen.addElement(e.getValue());
      }
    else if (e instanceof JAddVectorValueObject)
      {
	Vector newElements = (Vector) e.getValue();

	for (int i = 0; i < newElements.size(); i++)
	  {
	    chosen.addElement(newElements.elementAt(i));
	  }
      }
    else if (e instanceof JDeleteValueObject)
      {
	if (debug)
	  {
	    System.out.println("removing element");
	  }
	chosen.removeElement(e.getValue());
      }
    else if (e instanceof JDeleteVectorValueObject)
      {
	Vector newElements = (Vector) e.getValue();

	for (int i = 0; i < newElements.size(); i++)
	  {
	    chosen.removeElement(newElements.elementAt(i));
	  }
      }

    return true;
  }

  public void actionPerformed(ActionEvent e)
  {
    if (e.getSource() == done)
      {
	if (chosen.size() == 0)
	  {
	    new JErrorDialog(gc, "You must choose a default owner group.");
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
