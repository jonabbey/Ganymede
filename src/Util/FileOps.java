/*

   FileOps.java

   This utility class provides a number of static methods for doing
   file operations.
   
   Created: 2 December 2000
   Release: $Name:  $
   Version: $Revision: 1.3 $
   Last Mod Date: $Date: 2000/12/10 22:53:45 $
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
                                                                         FileOps

------------------------------------------------------------------------------*/

/**
 *
 * This utility class provides a number of static methods for doing
 * file operations.
 *
 */

public class FileOps {

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

  public static boolean deleteFile(String filename) throws IOException
  {
    File file = new File(filename);
    file.delete();

    return true;
  }

  public static boolean deleteDirectory(String directoryName) throws IOException
  {
    boolean success = true;
    String filenames[];
    File file;

    /* -- */

    file = new File(directoryName);

    if (!file.isDirectory())
      {
	throw new IOException("Error, deleteDirectory called on non-directory " + directoryName);
      }

    filenames = file.list();

    if (filenames == null)
      {
	return success;
      }

    directoryName = PathComplete.completePath(directoryName);

    for (int i = 0; i < filenames.length; i++)
      {
	File subfile = new File(directoryName, filenames[i]);

	if (subfile.isDirectory())
	  {
	    try
	      {
		if (!deleteDirectory(directoryName + filenames[i]))
		  {
		    success = false;
		  }
	      }
	    catch (IOException ex)
	      {
		ex.printStackTrace();
		success = false;
	      }
	  }
	else
	  {
	    try
	      {
		if (!deleteFile(directoryName + filenames[i]))
		  {
		    success = false;
		  }
	      }
	    catch (IOException ex)
	      {
		ex.printStackTrace();
		success = false;
	      }
	  }
      }

    // all sub files should be deleted, delete the directory

    if (success)
      {
	file.delete();
      }

    return success;
  }

  /**
   * <P>This method is used to run an external process line for the Ganymede
   * server.  This method waits until the external command completes before
   * returning, and all file handles opened to communicate with the
   * process will be closed before returning.</P>
   */

  public static int runProcess(String commandLine) throws IOException
  {
    Process p = java.lang.Runtime.getRuntime().exec(commandLine);

    try
      {
	p.waitFor();
	
	return p.exitValue();
      }
    catch (InterruptedException ex)
      {
	throw new IOException("Were were interrupted while waiting for the process to complete: " + ex.getMessage());
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

    /*
      File x = new File(args[0]);
      
      try
      {
      if (x.isDirectory())
      {
      result = deleteDirectory(args[0]);
      }
      else
      {
      result = deleteFile(args[0]);
      }
      }
      catch (IOException ex)
      {
      ex.printStackTrace();
      }
    */

    /*
      String inName = args[0];
      String outName = args[1];

      try
      {
      result = FileOps.copyFile(inName, outName);
      }
      catch (IOException ex)
      {
      ex.printStackTrace();
      }
    */
      
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
