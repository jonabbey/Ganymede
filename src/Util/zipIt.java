/*

   zipIt.java

   This utility class allows the Ganymede server to conveniently store
   a bunch of pre-existing files into a zip archive for the purposes
   of backups.
   
   Created: 3 February 1999
   Release: $Name:  $
   Version: $Revision: 1.4 $
   Last Mod Date: $Date: 1999/02/03 23:11:30 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999  The University of Texas at Austin.

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
import java.util.zip.*;
import java.io.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                           zipIt

------------------------------------------------------------------------------*/

/**
 *
 *  This utility class allows the Ganymede server to conveniently store
 *  a bunch of pre-existing files into a zip archive for the purposes
 *  of backups.
 *
 */

public class zipIt {

  /**
   *
   * This method creates a zip file, and adds a vector of Files to the zip file,
   * without capturing any path information in the file names held in the
   * zip file.
   *
   * @param outputFile Name of the zip file to be created
   * @param files Vector of Strings naming files to be added to the
   * zip file.
   *
   */

  public static void createZipFile(String outputFileName, Vector fileNames) throws IOException
  {
    File outputFile;
    Vector files = new Vector();

    /* -- */

    outputFile = new File(outputFileName);

    for (int i = 0; i < fileNames.size(); i++)
      {
	files.addElement(new File((String) fileNames.elementAt(i)));
      }

    zipIt.createZipFile(outputFile, files);
  }

  /**
   *
   * This method creates a zip file, and adds a vector of Files to the zip file,
   * without capturing any path information in the file names held in the
   * zip file.
   *
   * @param outputFile File object representing the zip file to be created
   * @param files Vector of File objects representing the files to be added to the
   * zip file.
   *
   */

  public static void createZipFile(File outputFile, Vector files) throws IOException
  {
    ZipOutputStream zipOut = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(outputFile)));

    try
      {
	for (int i = 0; i < files.size(); i++)
	  {
	    File inFile = (File) files.elementAt(i);

	    if (inFile.isDirectory())
	      {
		continue;
	      }

	    addZipEntry(zipOut, inFile);
	  }
      }
    finally
      {
	zipOut.close();
      }
  }

  /**
   *
   * This method adds a new file to an existing ZipOutputStream.
   *
   * @param zipOut ZipOutputStream to add zipIn to.
   * @param zipIn A File to be added to the ZipOutputStream.
   *
   */

  private static void addZipEntry(ZipOutputStream zipOut, File zipIn) throws IOException
  {
    BufferedInputStream inStream = new BufferedInputStream(new FileInputStream(zipIn));
    byte buffer[] = new byte[1024];
    int length;

    /* -- */

    zipOut.putNextEntry(new ZipEntry(zipIn.getName()));

    try
      {
	length = inStream.read(buffer);
	
	while (length != -1)
	  {
	    zipOut.write(buffer, 0, length);
	    length = inStream.read(buffer);
	  }
      }
    finally
      {
	zipOut.closeEntry();
      }
  }

  /**
   *
   * Test rig
   *
   */

  public static void main (String args[])
  {
    String zipName = args[0];
    Vector inFiles = new Vector();

    for (int i = 1; i < args.length; i++)
      {
	inFiles.addElement(args[i]);
      }

    try
      {
	createZipFile(zipName, inFiles);
      }
    catch (IOException ex)
      {
	throw new RuntimeException(ex.getMessage());
      }
  }
}
