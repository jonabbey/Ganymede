/*

   copyFile.java

   This utility class allows the Ganymede server to conveniently store
   a bunch of pre-existing files into a zip archive for the purposes
   of backups.
   
   Created: 2 December 2000
   Release: $Name:  $
   Version: $Revision: 1.3 $
   Last Mod Date: $Date: 2000/12/02 09:49:01 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999, 2000
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
   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.

*/

package arlut.csd.Util;

import java.util.*;
import java.io.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                        copyFile

------------------------------------------------------------------------------*/

/**
 *
 * This utility class allows the Ganymede server to copy files that builder
 * tasks are overwriting for the purpose of making a backup archive.
 *
 */

public class copyFile {

  /**
   *
   *
   */

  public static boolean copyFile(String inputFileName, String outputFileName) throws IOException
  {
    File outFile = new File(outputFileName);

    if (outFile.exists())
      {
	throw new IllegalArgumentException("Error, copyFile called with a pre-existing target");
      }

    BufferedInputStream inStream = new BufferedInputStream(new FileInputStream(inputFileName));
    BufferedOutputStream outStream = new BufferedOutputStream(new FileOutputStream(outputFileName));
    byte buffer[] = new byte[32767];
    int length = 0;

    /* -- */

    try
      {
	length = inStream.read(buffer);
	
	while (length != -1)
	  {
	    outStream.write(buffer, 0, length);
	    length = inStream.read(buffer);
	  }
      }
    finally
      {
	outStream.close();
	inStream.close();

	if (length == -1)
	  {
	    return true;
	  }
	else
	  {
	    return false;
	  }
      }
  }

  /**
   *
   * Test rig
   *
   */

  public static void main (String args[])
  {
    boolean result = false;
    String inName = args[0];
    String outName = args[1];

    try
      {
	result = copyFile(inName, outName);
      }
    catch (IOException ex)
      {
	ex.printStackTrace();
      }

    if (result)
      {
	System.exit(0);
      }
    else
      {
	System.exit(1);
      }
  }
}
