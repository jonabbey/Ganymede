/*

   SysInfo.java

   Created: 1 January 2008


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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Properties;

/*------------------------------------------------------------------------------
                                                                           class
                                                                         SysInfo

------------------------------------------------------------------------------*/

/**
 * This class provides convenient access to descriptive system data
 * from the JVM.
 */

public class SysInfo {

  /**
   * Returns a string containing all system properties defined in the
   * Java environment.
   */

  public static String getSysInfo()
  {
    Properties sysProps = java.lang.System.getProperties();

    StringWriter stringTarget = new StringWriter();
    PrintWriter writer = new PrintWriter(stringTarget);

    sysProps.list(writer);
    
    writer.close();

    return stringTarget.toString();
  }

  /**
   * Helpful command line system properties dump rig.
   */

  public static void main(String argv[])
  {
    System.out.println(getSysInfo());
  }
}

