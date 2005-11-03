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
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996 - 2005
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

package arlut.csd.ganymede.client;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.RemoteException;
import java.util.Vector;

import javax.swing.BorderFactory;
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
import arlut.csd.JDialog.JCenterDialog;
import arlut.csd.JDialog.JErrorDialog;
import arlut.csd.JDialog.StringDialog;
import arlut.csd.Util.TranslationService;
import arlut.csd.ganymede.common.ReturnVal;

/*------------------------------------------------------------------------------
                                                                           class
                                                             JDefaultOwnerDialog

------------------------------------------------------------------------------*/

public class JDefaultOwnerDialog extends JCenterDialog implements ActionListener, JsetValueCallback{

  private final static boolean debug = false;

  /**
   * TranslationService object for handling string localization in the
   * Ganymede client.
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.client.JDefaultOwnerDialog");

  /**
   * We'll remember our list of chosen owner groups in a static
   * fashion, so that we can re-present the list if the user brings
   * the dialog back up.
   */

  private static Vector last_chosen;

  /**
   * We'll create ourselves a static monitor object to use to
   * synchronize manipulations of the global static last_chosen
   * Vector.
   */

  private static Object last_chosen_lock = new Object();

  // ---

  JButton done;

  Vector
    chosen,
    available;

  gclient gc;

  private StringSelector ss;

  ReturnVal retVal = null;

  private boolean group_chosen = false;

  public JDefaultOwnerDialog(gclient gc, Vector groups)
  {
    // "Select Default Owner"
    super(gc, ts.l("init.title"), true);

    this.gc = gc;
    this.available = groups;

    getContentPane().setLayout(new BorderLayout());

    // "Select default owner for newly created objects"
    JLabel dialog_banner = new JLabel(ts.l("init.label"));
    dialog_banner.setBorder(BorderFactory.createEmptyBorder(3,3,3,3));
    getContentPane().add("North", dialog_banner);

    // did we previously have a list of chosen owner group invid's
    // selected?

    chosen = new Vector();

    synchronized(JDefaultOwnerDialog.last_chosen_lock)
      {
	if (JDefaultOwnerDialog.last_chosen != null)
	  {
	    chosen.addAll(JDefaultOwnerDialog.last_chosen);
	  }
      }

    // Maybe I should use null instead of chosen?
    ss = new StringSelector(this, true, true, true);
    ss.update(available, true, null, chosen, true, null);
    ss.setCallback(this);
    getContentPane().add("Center", ss);
    
    done = new JButton(StringDialog.getDefaultOk());
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
	    // "You must choose a default Owner Group."
	    new JErrorDialog(gc, ts.l("actionPerformed.no_owner_error"));
	    return;
	  }

	try
	  {
	    retVal = gc.getSession().setDefaultOwner(chosen);
	    
	    retVal = gc.handleReturnVal(retVal);

	    if (retVal == null || retVal.didSucceed())
	      {
		this.group_chosen = true;

		synchronized (JDefaultOwnerDialog.last_chosen_lock)
		  {
		    if (JDefaultOwnerDialog.last_chosen == null)
		      {
			JDefaultOwnerDialog.last_chosen = new Vector();
		      }
		    else
		      {
			JDefaultOwnerDialog.last_chosen.clear();
		      }

		    JDefaultOwnerDialog.last_chosen.addAll(ss.getChosenHandles());
		  }
	      }

	    this.setVisible(false);
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Could not set filter: " + rx);
	  }
      }
  }

  /**
   * Shows the dialog, and returns the ReturnVal after we make our
   * call to the server to set the default owner list.  If the dialog
   * was closed manually (by clicking the 'close window' button), this
   * method will a failure result.
   *
   * Use this instead of setVisible().
   */
  public ReturnVal chooseOwner()
  {
    this.group_chosen = false;

    // we're a modal dialog, so we'll block here until our visibility
    // is closed.

    setVisible(true);

    // Our modal dialog has been made non-visible, either through the
    // user clicking on the done button or through the user manually
    // closing the dialog window.  If we haven't set the chosen flag
    // to true, we'll need to encode a failure result.

    if (!this.group_chosen && (retVal == null || retVal.didSucceed()))
      {
	retVal = new ReturnVal(false);
      }

    return retVal;
  }

  /**
   * This method makes the JDefaultOwnerDialog forget the last set of
   * default owner invids chosen.  The next time a JDefaultOwnerDialog
   * instance is created, it will not pre-select the previous list of
   * owners.
   */

  public static void clear()
  {
    synchronized (JDefaultOwnerDialog.last_chosen_lock)
      {
	JDefaultOwnerDialog.last_chosen = null;
      }
  }
}
