/*

   Execer.java

   This module provides a convenient method to execute an external
   process and to take care of promptly closing down file handles,
   etc.
   
   Created: 13 July 2001
   Release: $Name:  $
   Version: $Revision: 1.1 $
   Last Mod Date: $Date: 2001/07/13 18:38:33 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999, 2000, 2001
   The University of Texas at Austin.

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

package arlut.csd.Util;

import java.io.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                          Execer

------------------------------------------------------------------------------*/

/**
 * <p>This module provides a convenient method to execute an external
 * process and to take care of promptly closing down file handles,
 * etc.</p>
 *
 * @version $Revision: 1.1 $ $Date: 2001/07/13 18:38:33 $ $Name:  $
 * @author Jonathan Abbey, jonabbey@arlut.utexas.edu, ARL:UT
 */

public class Execer {

  final static boolean debug = false;

  // ---

  public static int exec(String commandLine) throws InterruptedException, IOException
  {
    int result;
    Process p = java.lang.Runtime.getRuntime().exec(commandLine);

    try
      {
	p.waitFor();
	
	result = p.exitValue();

	return result;
      }
    finally
      {
	// the following is mentioned as a work-around for the
	// fact that Process keeps its file descriptors open by
	// default until Garbage Collection
		    
	try
	  {
	    p.getInputStream().close();
	  }
	catch (NullPointerException ex)
	  {
	  }
	catch (IOException ex)
	  {
	  }
		
	try
	  {
	    p.getOutputStream().close();
	  }
	catch (NullPointerException ex)
	  {
	  }
	catch (IOException ex)
	  {
	  }
		
	try
	  {
	    p.getErrorStream().close();
	  }
	catch (NullPointerException ex)
	  {
	  }
	catch (IOException ex)
	  {
	  }

	p.destroy();
      }
  }
}
