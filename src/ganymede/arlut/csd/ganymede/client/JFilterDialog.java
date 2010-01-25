/*

   JFilterDialog.java

   This class defines a dialog used to set filter options for queries
   by the client.
   
   Created: 3 March 1998

   Module By: Mike Mulvaney

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996-2010
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
   along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/

package arlut.csd.ganymede.client;

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
import arlut.csd.JDialog.StringDialog;
import arlut.csd.Util.TranslationService;
import arlut.csd.ganymede.common.ReturnVal;

/*------------------------------------------------------------------------------
                                                                           class
                                                                   JFilterDialog

------------------------------------------------------------------------------*/

public class JFilterDialog extends JDialog implements ActionListener, JsetValueCallback{

  /**
   * TranslationService object for handling string localization in the
   * Ganymede client.
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.client.JFilterDialog");

  private final static boolean debug = false;
  JButton cancel, ok;
  Vector filter, available = null;
  gclient gc;
  boolean changed = false;

  /* -- */

  public JFilterDialog(gclient gc)
  {
    // "Select Query Filter"
    super(gc, ts.l("init.dialog_title"));

    this.gc = gc;

    filter = new Vector();

    getContentPane().setLayout(new BorderLayout());

    // "Select Owner Groups to show."
    JLabel l =  new JLabel(ts.l("init.dialog_label"), JLabel.CENTER);
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

    ok = new JButton(StringDialog.getDefaultOk());
    ok.addActionListener(this);
    cancel = new JButton(StringDialog.getDefaultCancel());
    cancel.addActionListener(this);

    if (glogin.isRunningOnMac())
      {
	JPanel p = new JPanel();
	p.add(cancel);
	p.add(ok);

	JPanel macPanel = new JPanel();
	macPanel.setLayout(new BorderLayout());
	macPanel.add(p, BorderLayout.EAST);

	macPanel.setBorder(gc.raisedBorder);
    
	getContentPane().add("South", macPanel);
      }
    else
      {
	JPanel p = new JPanel();
	p.add(ok);
	p.add(cancel);
	p.setBorder(gc.raisedBorder);
    
	getContentPane().add("South", p);
      }

    setBounds(50,50,50,50);
    pack();
    this.setVisible(true);
  }

  public boolean setValuePerformed(JValueObject e)
  {
    if (e instanceof JAddValueObject)
      {
	if (debug)
	  {
	    System.out.println("Adding element");
	  }
	
	changed = true;
	filter.addElement(e.getValue());
      }
    else if (e instanceof JAddVectorValueObject)
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
    else if (e instanceof JDeleteValueObject)
      {
	if (debug)
	  {
	    System.out.println("removing element");
	  }

	changed = true;

	filter.removeElement(e.getValue());
      }
    else if (e instanceof JDeleteVectorValueObject)
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
    if (e.getSource() == ok)
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
		// "Could not set Query Filter."
		gc.showErrorMessage(ts.l("actionPerformed.error"));
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
