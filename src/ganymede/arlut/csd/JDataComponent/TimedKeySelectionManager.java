/*
   TimedKeySelectionManager.java

   Derived from example in David M. Geary's graphic JAVA 2 Mastering the JFC
   3rd Edition (ISBN 0-13-079667-0)

   Created: April 13, 1999
   Release: $Name:  $
   Version: $Revision: 1.2 $
   Last Mod Date: $Date: 1999/07/22 05:33:22 $
   Module By: Jonathan Abbey 

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

package arlut.csd.JDataComponent;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                        TimedKeySelectionManager

------------------------------------------------------------------------------*/

/**
 * <p>A JComboBox KeySelectionManager subclass that can be attached to
 * a JComboBox to allow typing multiple characters of an item name to
 * find it within the JComboBox's model.  That is, rather than only
 * keying on the first character in the item's name, TimedKeySelectionManager
 * allows a user to type the full name of an object to select it,
 * as long as the user doesn't hesitate for more than half a second
 * between keystrokes.</p>
 *
 * @version $Revision: 1.2 $ $Date: 1999/07/22 05:33:22 $ $Name:  $
 * @author David M. Geary, Graphic Java Mastering the JFC Volume II: Swing
 */

public class TimedKeySelectionManager implements JComboBox.KeySelectionManager {

  private final static boolean debug = false;

  /* - */

  private String searchString = new String();
  private long lastTime;
  private int start = 0;

  /* -- */

  public int selectionForKey(char key, ComboBoxModel model)
  {
    updateSearchString(model, key);

    if (debug)
      {
	System.err.println("TimedKeySelectionManager: searchString = " + searchString);
	System.err.println("TimedKeySelectionManager: start = " + start);
      }

    int selection = search(model, start);

    // if we didn't find anything and we didn't start at the beginning, loop.

    if (selection == -1 && start != 0)
      {
	selection = search(model, 0);
      }

    start = selection + 1;

    if (debug)
      {
	System.err.println("TimedKeySelectionManager: selection index = " + selection);
      }

    return selection;
  }

  /**
   * <p>Performs a case-insensitive search, seeking a matching string
   * in the ComboBoxModel.</p>
   */
  
  private int search(ComboBoxModel model, int start)
  {
    int size = model.getSize();

    for (int i = start; i < size; i++)
      {
	String s = getString(model, i).toLowerCase();

	if (s.startsWith(searchString))
	  {
	    if (debug)
	      {
		System.err.println("TimedKeySelectionManager.search(): searched through " + (i - start));
	      }

	    return i;
	  }
      }

    if (debug)
      {
	System.err.println("TimedKeySelectionManager.search(): searched through " + (size - start) + ", XX");
      }

    return -1;
  }

  private String getString(ComboBoxModel model, int index)
  {
    return model.getElementAt(index).toString();
  }

  private String getSelectedString(ComboBoxModel model)
  {
    return model.getSelectedItem().toString();
  }

  private void updateSearchString(ComboBoxModel model, char key)
  {
    long time = System.currentTimeMillis();

    if (time - lastTime < 500)
      {
	searchString += key;
      }
    else
      {
	searchString = "" + key;
      }

    searchString = searchString.toLowerCase();

    lastTime = time;
  }
}
