/*

   JFilterDialog.java

   This class defines a dialog used to set filter options for queries
   by the client.
   
   Created: 3 March 1998
   Release: $Name:  $
   Version: $Revision: 1.4 $
   Last Mod Date: $Date: 1999/01/22 18:04:10 $
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
