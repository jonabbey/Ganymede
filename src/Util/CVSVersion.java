/*

   CVSVersion.java

   Created: 16 November 2001
   Release: $Name:  $
   Version: $Revision: 1.7 $
   Last Mod Date: $Date: 2001/11/17 02:59:51 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999, 2000, 2001
   The University of Texas at Austin.

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
   along with this program; if not, write to the Free Software
   Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
   02111-1307, USA

*/

package arlut.csd.Util;

/*------------------------------------------------------------------------------
                                                                           class
                                                                      CVSVersion

------------------------------------------------------------------------------*/

/**
 * <p>This class records the CVS release information for Ganymede, and may be
 * referenced by server or client code.</p>
 *
 * <p>This class must be recompiled after CVS export in order for the
 * version information to be accurate.</p>
 */

public class CVSVersion {

  static String CVS_release_name = "$Name:  $";
  static String CVS_release_date = "$Date: 2001/11/17 02:59:51 $";
  static String release_string = null;

  // ---

  /**
   * <p>Returns a composite string containing release information based on the
   * last CVS export and compile of the CVSVersion class.</p>
   */

  public static String getReleaseString()
  {
    if (release_string != null)
      {
	return release_string;
      }

    release_string = CVSVersion.parseRelease(CVS_release_name, CVS_release_date);

    return release_string;
  }
  
  /**
   * <p>This method parses CVS Name and Date tokens and returns a string which
   * describes the release number and date.</p>
   */

  public static String parseRelease(String CVS_release_name, String CVS_release_date)
  {
    if (CVS_release_name.length () <= 9)
      {
	return "version unknown" + " - " + CVS_release_date; 
      }

    String release_name = null;
    String release_number = "version unknown";
    String release_date;

    // cut off leading '$Name:  $', clean up whitespace
	
    release_name = CVS_release_name.substring(6, CVS_release_name.length()-1);
    release_name = release_name.trim();

    // cut off leading '$Date: 2001/11/17 02:59:51 $', clean up whitespace

    release_date = CVS_release_date.substring(6, CVS_release_date.length()-1);
    release_date = release_date.trim();
	
    // we use ganymede_XXX for our CVS tags
    
    if (release_name.indexOf('_') != -1)
      {
	release_number = release_name.substring(release_name.indexOf('_') + 1, 
						release_name.length());
      }

    if (release_number.length() == 9)
      {
	// convert XXXYYYZZZ style version number to x.y.z
	
	// i.e., 001000008 to 1.0.8
	
	String a = release_number.substring(0, 3);
	String b = release_number.substring(3, 6);
	String c = release_number.substring(6, 9);

	int ia, ib, ic;

	try
	  {
	    ia = Integer.parseInt(a);
	  }
	catch (NumberFormatException ex)
	  {
	    ia = -1;
	  }

	try
	  {
	    ib = Integer.parseInt(b);
	  }
	catch (NumberFormatException ex)
	  {
	    ib = -1;
	  }

	try
	  {
	    ic = Integer.parseInt(c);
	  }
	catch (NumberFormatException ex)
	  {
	    ic = -1;
	  }

	release_number = ia + "." + ib + "." + ic;
      }

    return release_number + " - " + release_date;
  }
}
