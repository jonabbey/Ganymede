/*

   BrowserLauncher.java

   Created: 7 January 2009

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996-2010
   The University of Texas at Austin

   Contact information

   Web site: http://www.arlut.utexas.edu/gash2
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

package arlut.csd.Util;

import java.net.URI;
import java.lang.reflect.Method;

/*------------------------------------------------------------------------------
                                                                           class
                                                                 BrowserLauncher

------------------------------------------------------------------------------*/

/**
 * This utility class provides methods for displaying a URL in a web
 * browser on the user's desktop.
 */

public class BrowserLauncher {

  private static Class desktopClass = null;
  private static Object desktopObject = null;

  /**
   * Returns true if we are running under Java 6 or later with support
   * for the java.awt.Desktop class.
   *
   * Under Java 5, we don't have access to the java.awt.Desktop class,
   * so we will return false.
   *
   * N.B. this method could be crafted to take advantage of the
   * java.applet.AppletContext and/or javax.jnlp.BasicService classes,
   * if we are being as an applet or a Java Web Start application
   * under Java 5, but we are not yet providing this support.
   */

  public static synchronized boolean isWebBrowserSupported()
  {
    if (desktopObject == null)
      {
	try
	  {
	    desktopClass = Class.forName("java.awt.Desktop");

	    Method getDesktopMethod = desktopClass.getMethod("getDesktop", new Class [] {});

	    desktopObject = getDesktopMethod.invoke(null, (Object[]) null);
	  }
	catch (RuntimeException ex)
	  {
	    // to make FindBugs happier
	  }
	catch (Exception ex)
	  {
	  }
      }

    return desktopObject != null;
  }

  /**
   * Invokes a browser to bring up the given URL.
   *
   * May throw an IllegalArgumentException if the url parameter is not
   * well-formed.
   */

  public static void browse(String url)
  {
    if (!isWebBrowserSupported())
      {
	return;
      }

    URI urlObj = URI.create(url);

    try
      {
	Method browseMethod = desktopClass.getMethod("browse", new Class [] {java.net.URI.class});

	browseMethod.invoke(desktopObject, new Object [] {urlObj});
      }
    catch (RuntimeException ex)
      {
	// catch RuntimeException separately to make FindBugs
	// happier.. in fact, if we can't get the browser going on
	// this platform, we don't really care all that much.
      }
    catch (Exception ex)
      {
	// Ditto for any non-RuntimeExceptions.
      }
  }
}

