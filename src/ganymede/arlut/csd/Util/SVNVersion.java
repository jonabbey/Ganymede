/*

   SVNVersion.java

   Created: 16 November 2001

   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   Last Mod Date: $Date$
   SVN URL: $HeadURL$

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996 - 2007
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
   along with this program; if not, write to the Free Software
   Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
   02111-1307, USA

*/

package arlut.csd.Util;

/*------------------------------------------------------------------------------
                                                                           class
                                                                      SVNVersion

------------------------------------------------------------------------------*/

/**
 * <p>This class records the SVN branching information for Ganymede,
 * and may be referenced by server or client code.</p>
 *
 * <p>This class should be recompiled after SVN export in order for
 * the version information to be accurate.  The logic in this class
 * looks for a HeadURL branch substring of the form ganymede_001001013
 * for version 1.0.13, and uses that string to provide the release
 * number information provided by this class's static methods.</p>
 */

public class SVNVersion {

  static String SVN_release_name = "$HeadURL$";
  static String SVN_release_date = "$Date$";
  static String release_string = SVNVersion.parseRelease("ganymede_002000000", SVN_release_date);

  // ---

  /**
   * <p>Returns a composite string containing release information based on the
   * last SVN export and compile of the SVNVersion class.</p>
   */

  public static String getReleaseString()
  {
    if (release_string != null)
      {
	return release_string;
      }

    release_string = SVNVersion.parseRelease(SVN_release_name, SVN_release_date);

    return release_string;
  }
  
  /**
   * <p>This method parses SVN HeadURL and Date tokens and returns a string which
   * describes the release number and date.</p>
   */

  public static String parseRelease(String SVN_release_name, String SVN_release_date)
  {
    if (SVN_release_name.length () <= 9)
      {
	return "version unknown" + " - " + SVN_release_date; 
      }

    String release_name = null;
    String release_number = "version unknown";
    String release_date;

    // cut off leading '$HeadURL: ', clean up whitespace
	
    release_name = SVN_release_name.substring(9, SVN_release_name.length()-1);
    release_name = release_name.trim();

    // cut off leading '$Date: ', clean up whitespace

    release_date = SVN_release_date.substring(6, SVN_release_date.length()-1);
    release_date = release_date.trim();
	
    // we use ganymede_XXXYYYZZZ for our SVN release tags/branches, so
    // we'll see if we can find "ganymede_" in our HeadURL path

    int branch_match = release_name.indexOf("ganymede_");

    if (branch_match != -1)
      {
	try
	  {
	    release_number = release_name.substring(branch_match + 1, branch_match+10);

	    // convert XXXYYYZZZ style version number to x.y.z
	
	    // i.e., 001000008 to 1.0.8
	
	    String a = release_number.substring(0, 3);
	    String b = release_number.substring(3, 6);
	    String c = release_number.substring(6, 9);

	    int ia, ib, ic;

	    ia = Integer.parseInt(a);
	    ib = Integer.parseInt(b);
	    ic = Integer.parseInt(c);

	    release_number = ia + "." + ib + "." + ic;
	  }
	catch (IndexOutOfBoundsException ex)
	  {
	    // we've got a format error, no worries, leave
	    // release_number set to "version unknown".
	  }
	catch (NumberFormatException ex)
	  {
	    // we weren't able to parse the numeric string, probably
	    // we had a malformed ganymede path element in our SVN
	    // HeadURL.. so we'll also leave our release_number set to
	    // "version unknown".
	  }
      }

    return release_number + "\n" + release_date;
  }
}
