/*

   windowSizer.java

   Utility class to manage saving and restoring of window sizes in the
   Ganymede client.
   
   Created: 21 January 2006
   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

   Module By: Jonathan Abbey

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996 - 2006
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

import java.awt.Frame;
import javax.swing.JDialog;
import javax.swing.JFrame;

/*------------------------------------------------------------------------------
                                                                           class
                                                                     windowSizer

------------------------------------------------------------------------------*/

/**
 * This utility class handles saving and restoring window sizes for
 * the Ganymede client, using the Java Preferences API that was
 * introduced in Java 1.4.
 */

public class windowSizer {

  static final String SIZESAVED = "size_saved";
  static final String MAXIMIZED = "maximized";
  static final String XPOS = "x_position";
  static final String YPOS = "y_position";
  static final String WIDTH = "window_width";
  static final String HEIGHT = "window_height";

  /**
   * This method saves the given window's size and position in the 
   * system-dependent Java preferences store.
   *
   * This is not guaranteed to work, and may in fact be a no-op if no
   * preferences store is available.
   */

  public static void saveSize(JFrame window)
  {
    int status = window.getExtendedState();

    gclient.prefs.putBoolean(key(window, SIZESAVED), true);

    if ((status & Frame.MAXIMIZED_BOTH) != 0)
      {
	gclient.prefs.putBoolean(key(window, MAXIMIZED), true);
      }
    else
      {
	gclient.prefs.putBoolean(key(window, MAXIMIZED), false);
	gclient.prefs.putInt(key(window, XPOS), window.getX());
	gclient.prefs.putInt(key(window, YPOS), window.getY());
	gclient.prefs.putInt(key(window, WIDTH), window.getWidth());
	gclient.prefs.putInt(key(window, HEIGHT), window.getHeight());
      }
  }

  /**
   * This method retrieves the given window's size and position from
   * the system-dependent Java preferences store.
   *
   * This is not guaranteed to work.  If no preferences are found for
   * the window's position and size, restoreSize() will return false
   * and no actions will be performed on the window.  If the
   * preferences are found, the given window will be sized to match
   * that saved in the preferences.
   */

  public static boolean restoreSize(JFrame window)
  {
    if (!gclient.prefs.getBoolean(key(window, SIZESAVED), false))
      {
	return false;
      }

    int xpos = gclient.prefs.getInt(key(window, XPOS), -1);
    int ypos = gclient.prefs.getInt(key(window, YPOS), -1);
    int width = gclient.prefs.getInt(key(window, WIDTH), -1);
    int height = gclient.prefs.getInt(key(window, HEIGHT), -1);

    boolean locationSet = false;

    // set the non-maximized size first before we maximize the window
    // (if need be).  This way, if the user de-maximizes the window,
    // we'll have an appropriate fallback size for them to go to.

    if (xpos != -1 && ypos != -1 && width != -1 && height != -1)
      {
	window.setBounds(xpos, ypos, width, height);
	locationSet = true;
      }

    if (gclient.prefs.getBoolean(key(window, MAXIMIZED), false))
      {
	window.setExtendedState(Frame.MAXIMIZED_BOTH);
      }
    else
      {
	if (!locationSet)
	  {
	    return false;
	  }
      }

    return true;
  }

  /**
   * This method saves the given dialog's size and position in the 
   * system-dependent Java preferences store.
   *
   * This is not guaranteed to work, and may in fact be a no-op if no
   * preferences store is available.
   */

  public static void saveSize(JDialog dialog)
  {
    gclient.prefs.putBoolean(key(dialog, SIZESAVED), true);

    gclient.prefs.putInt(key(dialog, XPOS), dialog.getX());
    gclient.prefs.putInt(key(dialog, YPOS), dialog.getY());
    gclient.prefs.putInt(key(dialog, WIDTH), dialog.getWidth());
    gclient.prefs.putInt(key(dialog, HEIGHT), dialog.getHeight());
  }

  /**
   * This method retrieves the given dialog's size and position from
   * the system-dependent Java preferences store.
   *
   * This is not guaranteed to work.  If no preferences are found for
   * the dialog's position and size, restoreSize() will return false
   * and no actions will be performed on the dialog.  If the
   * preferences are found, the given dialog will be sized to match
   * that saved in the preferences.
   */

  public static boolean restoreSize(JDialog dialog)
  {
    if (!gclient.prefs.getBoolean(key(dialog, SIZESAVED), false))
      {
	return false;
      }

    int xpos = gclient.prefs.getInt(key(dialog, XPOS), -1);
    int ypos = gclient.prefs.getInt(key(dialog, YPOS), -1);
    int width = gclient.prefs.getInt(key(dialog, WIDTH), -1);
    int height = gclient.prefs.getInt(key(dialog, HEIGHT), -1);

    boolean locationSet = false;

    // set the non-maximized size first before we maximize the window
    // (if need be).  This way, if the user de-maximizes the window,
    // we'll have an appropriate fallback size for them to go to.

    if (xpos != -1 && ypos != -1 && width != -1 && height != -1)
      {
	dialog.setBounds(xpos, ypos, width, height);
	return true;
      }

    return false;
  }

  private static String key(JFrame window, String keyName)
  {
    return window.getClass().getName() + ":" + keyName;
  }

  private static String key(JDialog dialog, String keyName)
  {
    return dialog.getClass().getName() + ":" + keyName;
  }
}
