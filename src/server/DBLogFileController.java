/*

   DBLogFileController.java

   This controller class manages the recording and retrieval of
   DBLogEvents for the DBLog class, using an on-disk text file for the
   storage format.
   
   Created: 18 February 2003
   Release: $Name:  $
   Version: $Revision: 1.1 $
   Last Mod Date: $Date: 2003/02/22 03:58:23 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999, 2000, 2001, 2002, 2003
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

package arlut.csd.ganymede;

import java.net.*;
import java.io.*;
import java.util.*;
import arlut.csd.Util.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                             DBLogFileController

------------------------------------------------------------------------------*/

/**
 * <p>This controller class manages the recording and retrieval of
 * {@link arlut.csd.ganymede.DBLogEvent DBLogEvents} for the {@link
 * arlut.csd.ganymede.DBLog DBLog} class, using an on-disk text file for
 * the storage format.</p>
 *
 * <p>The file format used by this controller is the classic Ganymede log style,
 * in which each event is stored on a line, with parameters separated by
 * pipe characters.  This file format is documented in the server doc
 * directory at doc/logDesign.html, or on the web at
 * http://www.arlut.utexas.edu/gash2/design/logDesign.html</p>
 *
 * @version $Revision: 1.1 $ $Date: 2003/02/22 03:58:23 $
 * @author Jonathan Abbey, jonabbey@arlut.utexas.edu, ARL:UT
 */

public class DBLogFileController implements DBLogController {

  String logFileName = null;
  FileOutputStream logStream = null;
  PrintWriter logWriter = null;

  /**
   *
   * We use this buffer everywhere in this class to avoid creating
   * StringBuffers as much as we can.  Note that everything in this
   * class that uses the multibuffer must be sure not to call other
   * methods that themselves use the multibuffer in a way that would
   * conflict.  We're buying efficiency with some fragility here.
   *
   */

  SharedStringBuffer multibuffer;

  /* -- */

  public DBLogFileController(String filename) throws IOException
  {
    logFileName = filename;
    logStream = new FileOutputStream(logFileName, true); // append
    logWriter = new PrintWriter(logStream, true); // auto-flush on newline

    logWriter.println();	// emit newline to terminate any incomplete entry
  }

  /**
   * <p>This method writes the given event to the persistent storage managed by
   * this controller.</p>
   *
   * @param event The DBLogEvent to be recorded
   */

  public void writeEvent(DBLogEvent event)
  {
    logWriter.print(event.time.getTime());
    writeSep(logWriter);
    writeStr(logWriter, event.time.toString());
    writeSep(logWriter);
    writeStr(logWriter, event.eventClassToken);
    writeSep(logWriter);

    if (event.admin != null)
      {
	writeStr(logWriter, event.admin.toString());
      }

    writeSep(logWriter);

    if (event.adminName != null)
      {
	writeStr(logWriter, event.adminName);
      }

    writeSep(logWriter);

    if (event.transactionID != null)
      {
	writeStr(logWriter, event.transactionID);
      }

    writeSep(logWriter);

    if (event.objects != null)
      {
	for (int i = 0; i < event.objects.size(); i++)
	  {
	    if (i > 0)
	      {
		logWriter.print(',');
	      }

	    writeStr(logWriter, event.objects.elementAt(i).toString());
	  }
      }

    writeSep(logWriter);

    if (event.description != null)
      {
	writeStr(logWriter, event.description);
      }

    writeSep(logWriter);

    logWriter.println(event.notifyList);
  }

  /**
   * <P>This method is used to scan the persistent log storage for log events that match
   * invid and that have occurred since sinceTime.</P>
   *
   * @param invid If not null, retrieveHistory() will only return events involving
   * this object invid.
   *
   * @param sinceTime if not null, retrieveHistory() will only return events
   * occuring on or after the time specified in this Date object.
   *
   * @param keyOnAdmin if true, rather than returning a string containing events
   * that involved &lt;invid&gt;, retrieveHistory() will return a string containing events
   * performed on behalf of the administrator with invid &lt;invid&gt;.
   *
   * @param fullTransactions if true, the buffer returned will include all events in any
   * transactions that involve the given invid.  if false, only those events in a transaction
   * directly affecting the given invid will be returned.
   *
   * @return A human-readable multiline string containing a list of history events
   */

  public StringBuffer retrieveHistory(Invid invid, Date sinceTime, boolean keyOnAdmin, boolean fullTransactions)
  {
    StringBuffer buffer = new StringBuffer();
    DBLogEvent event = null;
    String line;
    String transactionID = null;

    boolean afterSinceTime = false;
    String dateString;
    long timeCode;
    Date time;

    BufferedReader in = null;
    FileReader reader = null;

    // if we don't have a log helper or we aren't looking for a
    // specific invid, just read from the file directly

    if (Ganymede.logHelperProperty != null && invid != null)
      {
	java.lang.Runtime runtime = java.lang.Runtime.getRuntime();

	try
	  {
	    java.lang.Process helperProcess;

	    if (keyOnAdmin)
	      {
		helperProcess = runtime.exec(Ganymede.logHelperProperty + " -a " + invid.toString());
	      }
	    else
	      {
		helperProcess = runtime.exec(Ganymede.logHelperProperty + " " + invid.toString());
	      }

	    in = new BufferedReader(new InputStreamReader(helperProcess.getInputStream()));
	  }
	catch (IOException ex)
	  {
	    System.err.println("DBLog.retrieveHistory(): Couldn't use helperProcess " + Ganymede.logHelperProperty);
	    in = null;
	  }
      }
    
    if (in == null)
      {
	try
	  {
	    reader = new FileReader(logFileName);
	  }
	catch (FileNotFoundException ex)
	  {
	    return null;
	  }

	in = new BufferedReader(reader);
      }

    /* -- */

    try
      {
	while (true)
	  {
	    line = in.readLine();

	    if (line == null)
	      {
		break;
	      }

	    if (line.trim().equals(""))
	      {
		continue;
	      }

	    // check to see if we've gotten to the requested start point

	    if (sinceTime != null && !afterSinceTime)
	      {
		dateString = line.substring(0, line.indexOf('|'));
    
		try
		  {
		    timeCode = new Long(dateString).longValue();
		  }
		catch (NumberFormatException ex)
		  {
		    throw new IOException("couldn't parse time code");
		  }
		
		time = new Date(timeCode);
		
		if (time.before(sinceTime))
		  {
		    continue;	// don't even bother parsing the rest of the line
		  }
		else
		  {
		    afterSinceTime = true;
		  }
	      }

	    if (event == null)
	      {
		event = new DBLogEvent(line);
	      }
	    else
	      {
		event.loadLine(line);
	      }

	    boolean found = false;

	    if (keyOnAdmin)
	      {
		if (event.admin != null)
		  {
		    found = invid.equals(event.admin);
		  }
	      }
	    else
	      {
		for (int i = 0; !found && i < event.objects.size(); i++)
		  {
		    if (invid.equals((Invid) event.objects.elementAt(i)))
		      {
			found = true;
		      }
		  }
		
		if (transactionID != null)
		  {
		    if (transactionID.equals(event.transactionID))
		      {
			if (fullTransactions || event.eventClassToken.equals("finishtransaction"))
			  {
			    found = true;
			  }
		      }
		  }
	      }

	    if (found)
	      {
		if (event.eventClassToken.equals("starttransaction"))
		  {
		    transactionID = event.transactionID;

		    String tmp2 = "---------- Transaction " + event.time.toString() + ": " + event.adminName + 
		      " ----------\n";
		    
		    buffer.append(tmp2);
		  }
		else if (event.eventClassToken.equals("finishtransaction"))
		  {
		    String tmp2 = "---------- End Transaction " + event.time.toString() + ": " + event.adminName + 
		      " ----------\n\n";
		    
		    buffer.append(tmp2);
		    transactionID = null;
		  }
		else if (transactionID != null)
		  {
		    String tmp = event.eventClassToken + "\n" +
		      WordWrap.wrap(event.description, 78, "\t") + "\n";
		
		    buffer.append(tmp);
		  }
		else
		  {
		    String tmp = event.time.toString() + ": " + event.adminName + "  " + event.eventClassToken +
		      WordWrap.wrap(event.description, 78, "\t") + "\n";

		    buffer.append(tmp);
		  }
	      }
	  }
      }
    catch (IOException ex)
      {
	// eof
      }
    finally
      {
	try
	  {
	    in.close();
	  }
	catch (IOException ex)
	  {
	    // shrug
	  }

	try
	  {
	    if (reader != null)
	      {
		reader.close();
	      }
	  }
	catch (IOException ex)
	  {
	    // shrug
	  }
      }

    return buffer;
  }

  /**
   *
   * Write a field separator to the log file
   *
   */

  private final void writeSep(PrintWriter logWriter)
  {
    logWriter.print('|');
  }

  /**
   *
   * Write a field to the log file, escaping it for safety
   *
   */

  private final void writeStr(PrintWriter logWriter, String in)
  {
    escapeStr(in);

    logWriter.write(multibuffer.getValue(), 0, multibuffer.length());
  }

  /**
   *
   * This method makes the provided String safe for inclusion
   * in the log file.
   *
   */

  private final SharedStringBuffer escapeStr(String in)
  {
    char[] ary = in.toCharArray();

    /* -- */

    multibuffer.setLength(0);

    // do it

    for (int i = 0; i < ary.length; i++)
      {
	if (ary[i] == '\n')
	  {
	    multibuffer.append("\\n");
	  }
	else if (ary[i] == '\\')
	  {
	    multibuffer.append("\\\\");
	  }
	else
	  {
	    multibuffer.append(ary[i]);
	  }
      }

    return multibuffer;
  }

  /**
   *
   * find the index of the next field separator, taking into account
   * escaped chars
   *
   */

  private int scanSep(char[] line, int startIndex)
  {
    int i;

    /* -- */

    for (i = startIndex; (i < line.length) && (line[i] != '|'); i++)
      {
	if (line[i] == '\\')	// skip backslashing
	  {
	    i++;
	  }
      }

    return i;
  }

  /**
   *
   * scan the string for this field, decoding backslash escapes in the
   * process 
   *
   */

  private String readNextField(char[] line, int startIndex)
  {
    boolean doit = true;

    /* -- */

    multibuffer.setLength(0);

    for (int i = startIndex; (i < line.length) && (line[i] != '|'); i++)
      {
	doit = true;

	if (line[i] == '\\')	// skip backslashing
	  {
	    i++;

	    if (line[i] == 'n')
	      {
		multibuffer.append('\n');
		doit = false;
	      }
	  }

	if (doit)
	  {
	    multibuffer.append(line[i]);
	  }
      }

    return multibuffer.toString();
  }

  /**
   *
   * This method reads a vector of invid's from a log file line
   *
   * @param line The character array containing the Invid's to be extracted
   * @param startIndex Where to start scanning the Invid's from in the line
   * @param result The vector to place the results in, or null if this
   * method should create its own result vector.
   *
   */

  private Vector readObjectVect(char[] line, int startIndex, Vector result)
  {
    int i;

    /* -- */

    if (result == null)
      {
	result = new Vector();
      }
    else
      {
	result.removeAllElements();
      }

    multibuffer.setLength(0);

    for (i = startIndex; (i < line.length) && (line[i] != '|'); i++)
      {
	if (line[i] == '\\')	// skip backslashing
	  {
	    i++;		// assume no newlines in object list
	  }

	if (line[i] == ',')
	  {
	    if (multibuffer.length() != 0)
	      {
		result.addElement(Invid.createInvid(multibuffer.toString()));
		multibuffer.setLength(0); // clear for next
	      }
	  }
	else
	  {
	    multibuffer.append(line[i]);
	  }
      }

    try
      {
	if (line[i] == '|')
	  {
	    if (multibuffer.length() != 0)
	      {
		result.addElement(Invid.createInvid(multibuffer.toString()));
	      }
	  }
      }
    catch (IndexOutOfBoundsException ex)
      {
	System.err.println("bad parse on line " + new String(line));
      }

    return result;
  }

  /**
   *
   * This method reads a vector of email addresses from a log file line
   *
   * @param line The character array containing the addresses's to be extracted
   * @param startIndex Where to start scanning the addresses's from in the line
   * @param result The vector to place the results in, or null if this
   * method should create its own result vector.
   *
   */

  private Vector readNotifyVect(char[] line, int startIndex, Vector result)
  {
    int i;

    /* -- */

    if (result == null)
      {
	result = new Vector();
      }
    else
      {
	result.removeAllElements();
      }

    multibuffer.setLength(0);

    for (i = startIndex; (i < line.length) && (line[i] != '|'); i++)
      {
	if (line[i] == '\\')	// skip backslashing
	  {
	    i++;
	  }

	if (line[i] == ',' || line[i] == '|')
	  {
	    if (multibuffer.length() != 0)
	      {
		result.addElement(multibuffer.toString().trim());
		multibuffer.setLength(0); // clear for next
	      }
	  }
	else
	  {
	    multibuffer.append(line[i]);
	  }
      }

    return result;
  }

  /**
   * <p>This method shuts down this controller, freeing up any resources used by this
   * controller.</p>
   */
  
  public void close()
  {
    logWriter.close();
  }
}
