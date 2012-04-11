/*

   FileOps.java

   This utility class provides a number of static methods for doing
   file operations.

   Created: 2 December 2000

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2012
   The University of Texas at Austin

   Ganymede is a registered trademark of The University of Texas at Austin

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

import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;


/*------------------------------------------------------------------------------
                                                                           class
                                                                         FileOps

------------------------------------------------------------------------------*/

/**
 * This utility class provides a number of static methods for doing
 * file operations.
 */

public class FileOps {

  private static int MAXSIZE = 0xFFFFF; // 16 megabytes

  /**
   * Copies a file named inputFileName to the location outputFileName
   * in an operating system independent fashion.
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

    return file.delete();
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
        return file.delete();
      }
    else
      {
        return false;
      }
  }

  /**
   * <p>This method is used to run an external process line for the
   * Ganymede server.  This method waits until the external command
   * completes before returning, and all file handles opened to
   * communicate with the process will be closed before returning.</p>
   *
   * <p>While this method is waiting for the process to exit, it will
   * spin on the stdout and stderr from the process, consuming and
   * discarding anything written by the subprocess so as to prevent
   * the subprocess from blocking due to its output piling up.</p>
   */

  public static int runProcess(String[] cmdArgs) throws IOException, InterruptedException
  {
    Process p = java.lang.Runtime.getRuntime().exec(cmdArgs);

    return runProcess(p);
  }

  /**
   * <p>This method is used to run an external process line for the
   * Ganymede server.  This method waits until the external command
   * completes before returning, and all file handles opened to
   * communicate with the process will be closed before returning.</p>
   *
   * <p>While this method is waiting for the process to exit, it will
   * spin on the stdout and stderr from the process, consuming and
   * discarding anything written by the subprocess so as to prevent
   * the subprocess from blocking due to its output piling up.</p>
   */

  public static int runProcess(String commandLine) throws IOException, InterruptedException
  {
    Process p = java.lang.Runtime.getRuntime().exec(commandLine);

    return runProcess(p);
  }

  /**
   * <p>This method is used to run an external process line for the
   * Ganymede server.  This method waits until the external command
   * completes before returning, and all file handles opened to
   * communicate with the process will be closed before returning.</p>
   *
   * <p>While this method is waiting for the process to exit, it will
   * spin on the stdout and stderr from the process, consuming and
   * discarding anything written by the subprocess so as to prevent
   * the subprocess from blocking due to its output piling up.</p>
   */

  private static int runProcess(Process p) throws IOException, InterruptedException
  {
    InputStream iStream = p.getInputStream();
    InputStream eStream = p.getErrorStream();

    byte[] buffer = new byte[4096]; // our own skip buffer

    try
      {
        while (true)
          {
            // this bletcherousness is so that we can consume anything
            // the sub process writes to its stdout or stderr, rather
            // than allowing the subprocess to block waiting in vain
            // for us to read from it.

            // really if we see anything coming from subprocesses
            // here, it means that somebody didn't properly do
            // redirection on their sync stuff.

            try
              {
                return p.exitValue();
              }
            catch (IllegalThreadStateException ex)
              {
                while (iStream.available() > 0 || eStream.available() > 0)
                  {
                    try
                      {
                        iStream.read(buffer, 0, (int) Math.min(buffer.length, iStream.available()));
                      }
                    catch (IOException exc)
                      {
                        // so we couldn't eat the bytes, what else can we do?
                      }

                    try
                      {
                        eStream.read(buffer, 0, (int) Math.min(buffer.length, eStream.available()));
                      }
                    catch (IOException exc)
                      {
                        // screw you, copper
                      }
                  }
              }

            try
              {
                Thread.currentThread().sleep(100);      // 100 milliseconds
              }
            catch (InterruptedException ex)
              {
                // screw you, copper
              }
          }
      }
    finally
      {
        FileOps.cleanupProcess(p);
      }
  }

  /**
   * <p>This method is used to run an external process line for the
   * Ganymede server.  This method waits until the external command
   * completes before returning, and all file handles opened to
   * communicate with the process will be closed before returning.</p>
   *
   * @return a List comprising the Integer result code, the String
   * stdout text, and the String Stderr text.
   */

  public static List runCaptureOutputProcess(String[] cmdArgs) throws IOException, InterruptedException
  {
    Process p = java.lang.Runtime.getRuntime().exec(cmdArgs);

    return runCaptureOutputProcess(p);
  }

  /**
   * <p>This method is used to run an external process line for the
   * Ganymede server.  This method waits until the external command
   * completes before returning, and all file handles opened to
   * communicate with the process will be closed before returning.</p>
   *
   * @return a List comprising the Integer result code, the String
   * stdout text, and the String Stderr text.
   */

  public static List runCaptureOutputProcess(String commandLine) throws IOException, InterruptedException
  {
    Process p = java.lang.Runtime.getRuntime().exec(commandLine);

    return runCaptureOutputProcess(p);
  }

  /**
   * <p>This method is used to run an external process line for the
   * Ganymede server.  This method waits until the external command
   * completes before returning, and all file handles opened to
   * communicate with the process will be closed before returning.</p>
   *
   * @return a List comprising the Integer result code, the String
   * stdout text, and the String Stderr text.
   */

  private static List runCaptureOutputProcess(Process p) throws IOException, InterruptedException
  {
    InputStream iStream = p.getInputStream();
    InputStream eStream = p.getErrorStream();

    StringBuffer stdoutBuffer = new StringBuffer();
    StringBuffer stderrBuffer = new StringBuffer();

    boolean done = false;
    int result_code = 0;

    try
      {
        while (!done)
          {
	    slurpStreams(iStream, eStream, stdoutBuffer, stderrBuffer);

            try
              {
                result_code = p.exitValue();
		done = true;
              }
            catch (IllegalThreadStateException ex)
              {
		slurpStreams(iStream, eStream, stdoutBuffer, stderrBuffer);
              }

            try
              {
                Thread.currentThread().sleep(100);      // 100 milliseconds
              }
            catch (InterruptedException ex)
              {
                // screw you, copper
              }
          }
      }
    finally
      {
	slurpStreams(iStream, eStream, stdoutBuffer, stderrBuffer);

        FileOps.cleanupProcess(p);

        List resultList = new ArrayList(3);

        resultList.add(new Integer(result_code));
        resultList.add(stdoutBuffer.toString());
        resultList.add(stderrBuffer.toString());

        return resultList;
      }
  }

  /**
   * Read everything we can from the passed stdout and stderr streams,
   * copy the data into outBuffer and errBuffer.
   */

  private static void slurpStreams(InputStream outStream, InputStream errStream, StringBuffer outBuffer, StringBuffer errBuffer)
  {
    byte[] buffer = new byte[4096];

    try
      {
	while (outStream.available() > 0 || errStream.available() > 0)
	  {
	    try
	      {
		int count = (int) Math.min(buffer.length, outStream.available());

		if (outBuffer.length() < MAXSIZE)
		  {
		    outBuffer.append(new String(buffer, 0, outStream.read(buffer, 0, count)));
		  }
		else
		  {
		    outStream.read(buffer, 0, count);
		  }
	      }
	    catch (IOException exc)
	      {
		// so we couldn't eat the bytes, what else can we do?
	      }

	    try
	      {
		int count = (int) Math.min(buffer.length, errStream.available());

		if (errBuffer.length() < MAXSIZE)
		  {
		    errBuffer.append(new String(buffer, 0, errStream.read(buffer, 0, count)));
		  }
		else
		  {
		    errStream.read(buffer, 0, count);
		  }
	      }
	    catch (IOException exc)
	      {
		// screw you, copper
	      }
	  }
      }
    catch (IOException ex)
      {
	// shrug
      }
  }

  /**
   * <p>This method shuts down / cleans up all resources related to
   * Process p.  The following is mentioned as a work-around for the
   * fact that Process keeps its file descriptors open by default
   * until Garbage Collection.</p>
   */

  public static void cleanupProcess(Process p)
  {
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
