/*

   JFilterDialog.java

   This class defines a dialog used to set filter options for queries
   by the client.
   
   Created: 3 March 1998
   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

   Module By: Mike Mulvaney

   -----------------------------------------------------------------------
	    
   Directory Droid Directory Management System
 
   Copyright (C) 1996-2004
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

import arlut.csd.ddroid.common.*;
import arlut.csd.ddroid.rmi.*;

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
  JButton cancel, done;
  Vector filter, available = null;
  gclient gc;
  boolean changed = false;

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
    
    StringSelector ss = new StringSelector(this, true, true, true);
    ss.update(available, true, null, filter, true, null);
    ss.setCallback(this);

    getContentPane().add("Center", ss);

    done = new JButton("Ok");
    done.addActionListener(this);
    cancel = new JButton("Cancel");
    cancel.addActionListener(this);
    
    JPanel p = new JPanel(false);
    p.setBorder(gc.statusBorderRaised);
    p.add(done);
    p.add(cancel);
    
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

	changed = true;
	filter.addElement(e.getValue());
      }
    else if (e.getOperationType() == JValueObject.ADDVECTOR)
      {
	if (debug)
	  {
	    System.out.println("Adding elements");
	  }

	changed = true;

	Vector newValues = (Vector) e.getValue();

	for (int i = 0; i < newValues.size(); i++)
	  {
	    filter.addElement(newValues.elementAt(i));
	  }
      }
    else if (e.getOperationType() == JValueObject.DELETE)
      {
	if (debug)
	  {
	    System.out.println("removing element");
	  }

	changed = true;

	filter.removeElement(e.getValue());
      }
    else if (e.getOperationType() == JValueObject.DELETEVECTOR)
      {
	if (debug)
	  {
	    System.out.println("Removing elements");
	  }

	changed = true;

	Vector newValues = (Vector) e.getValue();

	for (int i = 0; i < newValues.size(); i++)
	  {
	    filter.removeElement(newValues.elementAt(i));
	  }
      }	
    return true;
  }

  public void actionPerformed(ActionEvent e)
  {
    if (e.getSource() == done)
      {
	try
	  {
	    ReturnVal retVal = gc.getSession().filterQueries(filter);
	    gc.handleReturnVal(retVal);

	    if ((retVal == null) || (retVal.didSucceed()))
	      {
		if (changed)
		  {
		    gc.updateAfterFilterChange();
		  }

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

	changed = false;
      }
    else if (e.getSource() == cancel)
      {
	this.setVisible(false);
	changed = false;
      }
  }
}
