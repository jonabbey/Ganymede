/*

   LAFMenu.java

   A simple menu to change look and feel.
   
   Created: 4 April 1998

   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

   Module By: Mike Mulvaney, mulvaney@arlut.utexas.edu

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

package arlut.csd.JDataComponent;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/*------------------------------------------------------------------------------
                                                                           class
                                                                         LAFMenu

------------------------------------------------------------------------------*/

/**
 * <p>This class is a simple menu which can be added to a Swing app to
 * show (and switch between) a list of installed Swing look and feel
 * themes.</p>
 */

public class LAFMenu extends JMenu implements ActionListener
{
  public static boolean debug = false;

  // --

  JsetValueCallback my_parent;
  boolean allowCallback = false;

  Container root = null;

  public LAFMenu(Container root)
  {
    this(root, "Look");
  }

  public LAFMenu(Container root, String title)
  {
    super(title);

    if (debug)
      {
	System.out.println("Current look and feel: " + UIManager.getLookAndFeel().getName());
      }

    this.root = root;

    ButtonGroup group = new ButtonGroup();

    UIManager.LookAndFeelInfo[] info = UIManager.getInstalledLookAndFeels();

    for (int i = 0; i < info.length; i++)
      {
	JCheckBoxMenuItem mi = new JCheckBoxMenuItem(info[i].getName());
	mi.setActionCommand(info[i].getClassName());

	if (debug)
	  {
	    System.out.println(info[i].getClassName());
	  }

	group.add(mi);
	//mi.setEnabled(info[i].isSupportedLookAndFeel());
	mi.addActionListener(this);
	add(mi);
      }

    updateMenu();
  }

  /**
   * <p>This method is used to register a severely old-school
   * Ganymede-style pre-JDK 1.1 callback so that this code can cause
   * the Directory Droid code base to throw up an error dialog if an attempt
   * to change the look and feel using this menu fails.</p>
   */

  public void setCallback(JsetValueCallback parent)
  {
    if (parent == null)
      {
	throw new IllegalArgumentException("Invalid Parameter: parent cannot be null");
      }
    
    my_parent = parent;

    allowCallback = true;
  }

  public void setLAF(String look)
  {
    try 
      {
	UIManager.setLookAndFeel(look);

	if (root != null)
	  {
	    root.invalidate();
	    SwingUtilities.updateComponentTreeUI(root);
	    root.validate();
	  }
      } 
    catch (javax.swing.UnsupportedLookAndFeelException e)
      {
	if (allowCallback)
	  {
	    try
	      {
		my_parent.setValuePerformed(new JErrorValueObject(this,
								  "That look and feel is unsupported on this platform."));
	      }
	    catch (java.rmi.RemoteException rx)
	      {
	      }
	  }

	if (debug)
	  {
	    System.out.println("That look and feel is not supported on this platform.");
	  }

	updateMenu();
      }
    catch ( java.lang.InstantiationException e)
      {
	updateMenu();
      }
    catch (java.lang.ClassNotFoundException e)
      {
	updateMenu();
      }
    catch (java.lang.IllegalAccessException e)
      {
	updateMenu();
      }
  }
  
  public void actionPerformed(ActionEvent e)
  {
    String current = UIManager.getLookAndFeel().toString();

    if (current != null && current.indexOf(e.getActionCommand()) > 0)
      {
	if (debug)
	  {
	    System.out.println("Attempt to change to current LAF, I see your trick!");
	  }

	return;
      }
    
    try
      {
	setLAF(e.getActionCommand());
      }
    catch (Exception ex)
      {
	if (debug)
	  {
	    System.out.println("Something went wrong switching look and feels: " + ex);
	  }
	
	updateMenu();
      }
  }

  public void updateMenu()
  {
    String current = UIManager.getLookAndFeel().toString().toLowerCase();
    
    Component[] items = getMenuComponents();

    for (int i = 0; i < items.length; i++)
      {
	JCheckBoxMenuItem mi = (JCheckBoxMenuItem)items[i];
	String text = mi.getActionCommand().toLowerCase();

	if (current.indexOf(text) > -1)
	  {
	    mi.setSelected(true);
	    break;
	  }
	else
	  {
	    mi.setSelected(false);
	  }
      }
  }
}
