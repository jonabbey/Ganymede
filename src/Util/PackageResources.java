/*

   Parser.java

   This is a resource used for loading resources.

   Taken from Java Developer's Journal, Volume 2, Issue 5.
   Heavily modified by Jon Abbey and Mike Mulvaney
   
   Created: 9 July 1997
   Version: $Revision: 1.1 $
   Last Mod Date: $Date: 1999/01/20 17:50:08 $
   Release: $Name:  $

   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.Util;

import java.net.*;
import java.awt.*;
import java.applet.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                PackageResources

------------------------------------------------------------------------------*/

public class PackageResources {

  static AppletContext context = null;
  static boolean nevertryagain = false;

  static final boolean debug = false;

  // ---
  
  public static URL getPackageResource(String strResource, Class refClass) 
  {
    ClassLoader cl;
    String strPackageName, filePackageName, str;
    int i;
    URL url;
    
    /* -- */

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
   * Load an image
   *
   * @param comp Parent component, used for Util.waitforimage
   * @param imageName Name of image to be loaded
   * @param refClass Parent Class, used to find path to image
   *
   */

  public static Image getImageResource(Component comp, String imageName, Class refClass) 
  {
    Image image = null;
    URL url;
    Component ptr;

    /* -- */

    url = getPackageResource(imageName, refClass);

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
	throw new RuntimeException("caught nullptr trying to load image " + imageName + ", url = " + url);
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

