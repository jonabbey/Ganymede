/*

   windowSizer.java

   Utility class to manage saving and restoring of window sizes in the
   Ganymede client.

   Created: 21 January 2006

   Module By: Jonathan Abbey

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996 - 2014
   The University of Texas at Austin

   Ganymede is a registered trademark of The University of Texas at Austin

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

package arlut.csd.ganymede.common;

import java.awt.Frame;
import java.util.prefs.Preferences;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.UIManager;

/*------------------------------------------------------------------------------
                                                                           class
                                                                     windowSizer

------------------------------------------------------------------------------*/

/**
 * <p>This utility class handles saving and restoring window sizes for
 * the Ganymede client, using the Java Preferences API that was
 * introduced in Java 1.4.</p>
 */

public class windowSizer {

  static final String SIZESAVED = "size_saved";
  static final String MAXIMIZED = "maximized";
  static final String XPOS = "x_position";
  static final String YPOS = "y_position";
  static final String WIDTH = "window_width";
  static final String HEIGHT = "window_height";
  static final String LOOKANDFEEL = "look_and_feel";

  private Preferences prefEngine = null;

  public windowSizer(Preferences prefs)
  {
    this.prefEngine = prefs;
  }

  /**
   * <p>This method saves the given window's size and position in the
   * system-dependent Java preferences store.</p>
   *
   * <p>This is not guaranteed to work, and may in fact be a no-op if
   * no preferences store is available.</p>
   */

  public void saveSize(JFrame window)
  {
    if (prefEngine == null)
      {
        return;
      }

    int status = window.getExtendedState();

    prefEngine.putBoolean(key(window, SIZESAVED), true);

    if ((status & Frame.MAXIMIZED_BOTH) != 0)
      {
        prefEngine.putBoolean(key(window, MAXIMIZED), true);
      }
    else
      {
        prefEngine.putBoolean(key(window, MAXIMIZED), false);
        prefEngine.putInt(key(window, XPOS), window.getX());
        prefEngine.putInt(key(window, YPOS), window.getY());
        prefEngine.putInt(key(window, WIDTH), window.getWidth());
        prefEngine.putInt(key(window, HEIGHT), window.getHeight());
      }
  }

  /**
   * <p>This method retrieves the given window's size and position
   * from the system-dependent Java preferences store.</p>
   *
   * <p>This is not guaranteed to work.  If no preferences are found
   * for the window's position and size, restoreSize() will return
   * false and no actions will be performed on the window.  If the
   * preferences are found, the given window will be sized to match
   * that saved in the preferences.</p>
   */

  public boolean restoreSize(JFrame window)
  {
    if (prefEngine == null || !prefEngine.getBoolean(key(window, SIZESAVED), false))
      {
        return false;
      }

    int xpos = prefEngine.getInt(key(window, XPOS), -1);
    int ypos = prefEngine.getInt(key(window, YPOS), -1);
    int width = prefEngine.getInt(key(window, WIDTH), -1);
    int height = prefEngine.getInt(key(window, HEIGHT), -1);

    boolean locationSet = false;

    // set the non-maximized size first before we maximize the window
    // (if need be).  This way, if the user de-maximizes the window,
    // we'll have an appropriate fallback size for them to go to.

    if (xpos != -1 && ypos != -1 && width != -1 && height != -1)
      {
        window.setBounds(xpos, ypos, width, height);
        locationSet = true;
      }

    if (prefEngine.getBoolean(key(window, MAXIMIZED), false))
      {
        window.setExtendedState(Frame.MAXIMIZED_BOTH);
        locationSet = true;
      }

    if (!locationSet)
      {
        return false;
      }

    return true;
  }

  /**
   * <p>This method saves the given dialog's size and position in the
   * system-dependent Java preferences store.</p>
   *
   * <p>This is not guaranteed to work, and may in fact be a no-op if
   * no preferences store is available.</p>
   */

  public void saveSize(JDialog dialog)
  {
    if (prefEngine == null)
      {
        return;
      }

    prefEngine.putBoolean(key(dialog, SIZESAVED), true);

    prefEngine.putInt(key(dialog, XPOS), dialog.getX());
    prefEngine.putInt(key(dialog, YPOS), dialog.getY());
    prefEngine.putInt(key(dialog, WIDTH), dialog.getWidth());
    prefEngine.putInt(key(dialog, HEIGHT), dialog.getHeight());
  }

  /**
   * <p>This method retrieves the given dialog's size and position
   * from the system-dependent Java preferences store.</p>
   *
   * <p>This is not guaranteed to work.  If no preferences are found
   * for the dialog's position and size, restoreSize() will return
   * false and no actions will be performed on the dialog.  If the
   * preferences are found, the given dialog will be sized to match
   * that saved in the preferences.</p>
   */

  public boolean restoreSize(JDialog dialog)
  {
    if (prefEngine == null || !prefEngine.getBoolean(key(dialog, SIZESAVED), false))
      {
        return false;
      }

    int xpos = prefEngine.getInt(key(dialog, XPOS), -1);
    int ypos = prefEngine.getInt(key(dialog, YPOS), -1);
    int width = prefEngine.getInt(key(dialog, WIDTH), -1);
    int height = prefEngine.getInt(key(dialog, HEIGHT), -1);

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

  /**
   * This method saves the selected look and feel to the
   * system-dependent Java preferences store.
   */

  public void saveLookAndFeel()
  {
    if (prefEngine == null)
      {
        return;
      }

    String selectedLookAndFeel = UIManager.getLookAndFeel().getClass().getName();

    prefEngine.put(LOOKANDFEEL, selectedLookAndFeel);
  }

  /**
   * This method looks up the look and feel selection stored in the
   * system-dependent Java preferences store, and applies it to this
   * application.
   */

  public boolean restoreLookAndFeel()
  {
    if (prefEngine == null)
      {
        return false;
      }

    String savedLookAndFeel = prefEngine.get(LOOKANDFEEL, null);

    if (savedLookAndFeel == null)
      {
        // our default look and feel will be Nimbus, unless we're
        // running on the Mac

        if (!("Mac OS X".equals(System.getProperty("os.name"))))
          {
            for (UIManager.LookAndFeelInfo info: UIManager.getInstalledLookAndFeels())
              {
                if ("Nimbus".equals(info.getName()))
                  {
                    savedLookAndFeel = info.getClassName();
                    break;
                  }
              }
          }
      }

    if (savedLookAndFeel != null)
      {
        try
          {
            UIManager.setLookAndFeel(savedLookAndFeel);
          }
        catch (javax.swing.UnsupportedLookAndFeelException ex)
          {
          }
        catch (Exception ex)
          {
          }
        finally
          {
            return true;
          }
      }

    return false;
  }

  private String key(JFrame window, String keyName)
  {
    return window.getClass().getName() + ":" + keyName;
  }

  private String key(JDialog dialog, String keyName)
  {
    return dialog.getClass().getName() + ":" + keyName;
  }
}
