/*

   JDefaultOwnerDialog.java

   A dialog to choose filtering options.
   
   Created: ??
   Release: $Name:  $
   Version: $Revision: 1.4 $
   Last Mod Date: $Date: 2000/02/11 07:09:26 $
   Module By: Mike Mulvaney

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999  The University of Texas at Austin.

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
   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.

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
    else if (e.getOperationType() == JValueObject.ADDVECTOR)
      {
	Vector newElements = (Vector) e.getValue();

	for (int i = 0; i < newElements.size(); i++)
	  {
	    chosen.addElement(newElements.elementAt(i));
	  }
      }
    else if (e.getOperationType() == JValueObject.DELETE)
      {
	if (debug)
	  {
	    System.out.println("removing element");
	  }
	chosen.removeElement(e.getValue());
      }
    else if (e.getOperationType() == JValueObject.DELETEVECTOR)
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
