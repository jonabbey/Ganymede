/*

   PackageResources.java

   This is a resource used for loading resources.

   Taken from Java Developer's Journal, Volume 2, Issue 5.
   Heavily modified by Jon Abbey and Mike Mulvaney
   
   Created: 9 July 1997


   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

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

package arlut.csd.Util;

import java.applet.Applet;
import java.applet.AppletContext;
import java.awt.Component;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.io.InputStream;
import java.net.URL;

/*------------------------------------------------------------------------------
                                                                           class
                                                                PackageResources

------------------------------------------------------------------------------*/

/**
 * <p>Utility class to provide resource-loading services.  Methods in this class
 * can load images and other resources from either CLASSPATH or a jar file,
 * depending on how the program using this class is run.</p>
 *
 * @version $Id$
 * @author Jonathan Abbey
 */

public class PackageResources {

  static AppletContext context = null;
  static boolean nevertryagain = false;

  static final boolean debug = false;

  // ---

  /**
   * <p>Loads a generic resource by its filename from either CLASSPATH or a jar file,
   * depending on how the code calling this method was run.</p>
   *
   * @param strResource Filename of resource to be loaded
   * @param refClass Parent Class, used to find path to image
   */
  
  public static URL getPackageResource(String strResource, Class refClass) 
  {
    ClassLoader cl;
    String strPackageName, filePackageName, str;
    int i;
    URL url;
    
    /* -- */

    if (refClass != null)
      {
        cl = refClass.getClassLoader();
        strPackageName = refClass.getName();
        i = strPackageName.lastIndexOf('.' );

        if (i == -1)
          {
            strPackageName = "";
          }
        else
          {
            strPackageName = strPackageName.substring(0,i);
          }

        filePackageName = strPackageName.replace('.','/');

        str = (filePackageName.length() > 0 ? filePackageName + "/" : "") + strResource;
      }
    else
      {
        cl = null;
        str = strResource;
      }

    if (debug)
      {
        System.err.println("PackageResources: trying to get str " + str);
      }

    if (cl == null)
      {
        url = ClassLoader.getSystemResource(str);
      }
    else
      {
        url = cl.getResource(str);
      }

    return url;
  }

  /**
   * <p>Loads a generic resource by its filename from either CLASSPATH or a jar file,
   * depending on how the code calling this method was run.</p>
   *
   * @param strResource Filename of resource to be loaded
   * @param refClass Parent Class, used to find path to image
   */
  
  public static InputStream getPackageResourceAsStream(String strResource, Class refClass) 
  {
    ClassLoader cl;
    String strPackageName, filePackageName, str;
    int i;
    InputStream stream;
    
    /* -- */

    if (refClass != null)
      {
        cl = refClass.getClassLoader();
        strPackageName = refClass.getName();
        i = strPackageName.lastIndexOf('.' );

        if (i == -1)
          {
            strPackageName = "";
          }
        else
          {
            strPackageName = strPackageName.substring(0,i);
          }

        filePackageName = strPackageName.replace('.','/');

        str = (filePackageName.length() > 0 ? filePackageName + "/" : "") + strResource;
      }
    else
      {
        cl = null;
        str = strResource;
      }

    if (debug)
      {
        System.err.println("PackageResources: trying to get str " + str);
      }
    
    if (cl == null)
      {
        stream = ClassLoader.getSystemResourceAsStream(str);
      }
    else
      {
        stream = cl.getResourceAsStream(str);
      }
    
    return stream;
  }

  /**
   * <p>Loads an image by its filename from either CLASSPATH or a jar file,
   * depending on how the code calling this method was run.</p>
   *
   * @param comp Parent component, used for Util.waitforimage
   * @param imageName Name of image to be loaded
   * @param refClass Parent Class, used to find path to image
   */

  public static Image getImageResource(Component comp, String imageName, Class refClass) 
  {
    Image image = null;
    URL url;
    Component ptr;

    /* -- */

    url = getPackageResource(imageName, refClass);
    return getImageResource(comp, url);
  }

  /**
   * <p>Loads an image by its filename from either CLASSPATH or a jar file,
   * depending on how the code calling this method was run.</p>
   *
   * @param comp Parent component, used for Util.waitforimage
   * @param url URL of the image to load
   */

  public static Image getImageResource(Component comp, URL url) 
  {
    Image image = null;
    Component ptr;

    if (context == null && !nevertryagain)
      {
        ptr = comp;
        
        while (ptr != null && (!(ptr instanceof Applet)))
          {
            ptr = ptr.getParent();
          }
        
        if (ptr != null)
          {
            try
              {
                context = ((Applet) ptr).getAppletContext();
              }
            catch (NullPointerException ex)
              {
                context = null;
                nevertryagain = true;
              }
          }
      }

    if (debug)
      {
        System.err.println("PackageResources.getImageResouce(): Trying to fetch image from URL: " + url);
      }

    try
      {
        if (context != null)
          {
            image = context.getImage(url);
          }
        else
          {
            image = Toolkit.getDefaultToolkit().getImage(url);
          }
      }
    catch (NullPointerException ex)
      {
        throw new RuntimeException("caught nullptr trying to load image, url = " + url);
      }

    if (debug)
      {
        System.err.println("PackageResources.getImageResouce(): Waiting for image.");
      }

    waitForImage(comp, image);

    if (debug)
      {
        System.err.println("PackageResources.getImageResouce(): Returning image.");
      }
    
    return image;
  }

  // from gjt 1.1

  /**
   * Helper method to handle the MediaTracker for image loading.
   */

  public static void waitForImage(Component component, 
                                  Image image) 
  {
    MediaTracker tracker = new MediaTracker(component);

    try 
      {
        tracker.addImage(image, 0);
        tracker.waitForID(0);
      }
    catch(InterruptedException e)
      {
        throw new RuntimeException("waitForImage failed:" + e.getMessage());
      }
  }
}

