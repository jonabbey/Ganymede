/*

   DBLogFileController.java

   This controller class manages the recording and retrieval of
   DBLogEvents for the DBLog class, using an on-disk text file for the
   storage format.
   
   Created: 18 February 2003
   Release: $Name:  $
   Version: $Revision: 1.2 $
   Last Mod Date: $Date: 2003/02/27 00:01:49 $
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
 * <p>The file format used by this controller is the classic Ganymede log
 * style, in which each event is stored on a line, with parameters
 * separated by pipe characters.  This file format is documented in the
 * server doc directory at doc/logDesign.html, or on the web at
 * http://www.arlut.utexas.edu/gash2/design/logDesign.html</p>
 *
 * @version $Revision: 1.2 $ $Date: 2003/02/27 00:01:49 $
 * @author Jonathan Abbey, jonabbey@arlut.utexas.edu, ARL:UT
 */

public class DBLogFileController implements DBLogController {

  String logFileName = null;
  PrintWriter logWriter = null;

  /* -- */

  /**
   * <p>This constructor should be used for normal purposes in order to have
   * full log file functionality.</p>
   */

  public DBLogFileController(String filename) throws IOException
  {
    FileOutputStream logStream = null;

    /* -- */

    logFileName = filename;
    logStream = new FileOutputStream(logFileName, true); // append
    logWriter = new PrintWriter(logStream, true); // auto-flush on newline

    logWriter.println();	// emit newline to terminate any incomplete entry
  }

  /**
   * <p>This constructor is used to connect a DBLogFileController to an already
   * existing PrintWriter.  Unlike with the filename constructor, this version
   * does not emit a newline to terminate any incomplete entry in the PrintWriter's
   * stream.  This constructor is thus suitable for use with StringWriters.</p>
   *
   * <p>Note that if this constructor is used, the retrieveHistory() method will
   * throw an IllegalArgumentException, as there will be no file available to read
   * from.</p>
   */

  public DBLogFileController(PrintWriter logWriter)
  {
    this.logFileName = null;
    this.logWriter = logWriter;
  }

  /**
   * <p>This method writes the given event to the persistent storage
   * managed by this controller.</p>
   *
   * @param event The DBLogEvent to be recorded
   */

  public synchronized void writeEvent(DBLogEvent event)
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
   * <P>This method is used to scan the persistent log storage for log
   * events that match invid and that have occurred since
   * sinceTime.</P>
   *
   * @param invid If not null, retrieveHistory() will only return
   * events involving this object invid.
   *
   * @param sinceTime if not null, retrieveHistory() will only return
   * events occuring on or after the time specified in this Date
   * object.
   *
   * @param keyOnAdmin if true, rather than returning a string
   * containing events that involved &lt;invid&gt;, retrieveHistory()
   * will return a string containing events performed on behalf of the
   * administrator with invid &lt;invid&gt;.
   *
   * @param fullTransactions if true, the buffer returned will include
   * all events in any transactions that involve the given invid.  if
   * false, only those events in a transaction directly affecting the
   * given invid will be returned.
   *
   * @return A human-readable multiline string containing a list of
   * history events
   */

  public synchronized StringBuffer retrieveHistory(Invid invid, Date sinceTime, 
						   boolean keyOnAdmin, boolean fullTransactions)
  {
    if (logFileName == null)
      {
	throw new IllegalArgumentException("DBLogFileController: no filename specified");
      }

    StringBuffer buffer = new StringBuffer();
    DBLogEvent event = null;
    String line;
    String transactionID = null;

    boolean afterSinceTime = false;
    String dateString;
    long sinceLong = 0;
    long timeCode;
    Date time;

    BufferedReader in = null;
    FileReader reader = null;

    if (sinceTime != null)
      {
	sinceLong = sinceTime.getTime();
      }

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
		helperProcess = runtime.exec(Ganymede.logHelperProperty +
					     " -a " + invid.toString());
	      }
	    else
	      {
		helperProcess = runtime.exec(Ganymede.logHelperProperty + 
					     " " + invid.toString());
	      }

	    in = new BufferedReader(new InputStreamReader(helperProcess.getInputStream()));
	  }
	catch (IOException ex)
	  {
	    System.err.println("DBLog.retrieveHistory(): Couldn't use helperProcess " + 
			       Ganymede.logHelperProperty);
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

		if (timeCode < sinceLong)
		  {
		    continue;	// don't even bother parsing the rest of the line
		  }
		
		time = new Date(timeCode);
		afterSinceTime = true;
	      }

	    event = parseEvent(line);

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

		    String tmp2 = "---------- Transaction " + 
		      event.time.toString() +
		      ": " + 
		      event.adminName + 
		      " ----------\n";
		    
		    buffer.append(tmp2);
		  }
		else if (event.eventClassToken.equals("finishtransaction"))
		  {
		    String tmp2 = "---------- End Transaction " + 
		      event.time.toString() +
		      ": " + 
		      event.adminName + 
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
		    String tmp = event.time.toString() +
		      ": " + 
		      event.adminName +
		      "  " + 
		      event.eventClassToken +
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
   * <p>This method sets the fields for this DBLogEvent from a logfile
   * line.</p>
   */

  public DBLogEvent parseEvent(String line) throws IOException
  {
    int i, j;
    String dateString;
    char[] cary;
    long timeCode;
    String tmp;

    DBLogEvent event = new DBLogEvent();

    /* -- */

    if (line == null || (line.trim().equals("")))
      {
	throw new IOException("empty log line");
      }

    StringBuffer buf = new StringBuffer();

    //    System.out.println("Trying to create DBLogEvent: " + line);

    cary = line.toCharArray();

    i = line.indexOf('|');

    if (i == -1)
      {
	throw new IOException("malformed log line: " + line);
      }

    dateString = line.substring(0, i);
    
    try
      {
	timeCode = new Long(dateString).longValue();
      }
    catch (NumberFormatException ex)
      {
	throw new IOException("couldn't parse time code");
      }
    
    event.time = new Date(timeCode);
    
    j = i+1;
    i = scanSep(cary, j);	// find next |, skip human readable date
    
    j = i+1;
    i = scanSep(cary, j);
    
    event.eventClassToken = readNextField(cary, j);
    
    j = i+1;
    i = scanSep(cary, j);
    
    tmp = readNextField(cary, j);
    
    if (!tmp.equals(""))
      {
	event.admin = Invid.createInvid(tmp);	// get admin invid
      }
    else
      {
	// we have to be sure to do this.

	event.admin = null;
      }

    j = i+1;
    i = scanSep(cary, j);

    event.adminName = readNextField(cary, j); // get admin name
    
    j = i+1;
    i = scanSep(cary, j);

    event.transactionID = readNextField(cary, j); // get transaction id

    j = i+1;
    i = scanSep(cary, j);

    // read the object invid list.. re-use event.objects if it already
    // exists

    event.objects = readObjectVect(cary, j);

    j = i+1;
    i = scanSep(cary, j);

    event.description = readNextField(cary, j); // get text description

    j = i+1;
    i = scanSep(cary, j);

    // read the email address list..

    event.notifyVect = readNotifyVect(cary, j);

    if (event.notifyVect != null)
      {
	StringBuffer buf2 = new StringBuffer();

	for (int k = 0; k < event.notifyVect.size(); k++)
	  {
	    if (k > 0)
	      {
		buf2.append(", ");
	      }
	    
	    buf2.append((String) event.notifyVect.elementAt(k));
	  }

	event.notifyList = buf2.toString();
      }

    event.augmented = true;

    return event;
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
    logWriter.write(escapeStr(in));
  }

  /**
   *
   * This method makes the provided String safe for inclusion
   * in the log file.
   *
   */

  private final String escapeStr(String in)
  {
    char[] ary = in.toCharArray();

    /* -- */

    StringBuffer buf = new StringBuffer(ary.length);

    // do it

    for (int i = 0; i < ary.length; i++)
      {
	if (ary[i] == '\n')
	  {
	    buf.append("\\n");
	  }
	else if (ary[i] == '\\')
	  {
	    buf.append("\\\\");
	  }
	else
	  {
	    buf.append(ary[i]);
	  }
      }

    return buf.toString();
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
    StringBuffer buf = new StringBuffer(line.length);

    for (int i = startIndex; (i < line.length) && (line[i] != '|'); i++)
      {
	if (line[i] == '\\')	// skip backslashing
	  {
	    i++;

	    if (line[i] == 'n')
	      {
		buf.append('\n');
		continue;
	      }
	  }

	buf.append(line[i]);
      }

    return buf.toString();
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

  private Vector readObjectVect(char[] line, int startIndex)
  {
    int i;

    Vector result = new Vector();

    /* -- */

    StringBuffer buf = new StringBuffer();

    for (i = startIndex; (i < line.length) && (line[i] != '|'); i++)
      {
	if (line[i] == '\\')	// skip backslashing
	  {
	    i++;		// assume no newlines in object list
	  }

	if (line[i] == ',')
	  {
	    if (buf.length() != 0)
	      {
		result.addElement(Invid.createInvid(buf.toString()));
		buf = new StringBuffer();
	      }
	  }
	else
	  {
	    buf.append(line[i]);
	  }
      }

    try
      {
	if (line[i] == '|')
	  {
	    if (buf.length() != 0)
	      {
		result.addElement(Invid.createInvid(buf.toString()));
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

  private Vector readNotifyVect(char[] line, int startIndex)
  {
    int i;

    Vector result = new Vector();

    /* -- */

    StringBuffer buf = new StringBuffer();

    for (i = startIndex; (i < line.length) && (line[i] != '|'); i++)
      {
	if (line[i] == '\\')	// skip backslashing
	  {
	    i++;
	  }

	if (line[i] == ',' || line[i] == '|')
	  {
	    if (buf.length() != 0)
	      {
		result.addElement(buf.toString());
		buf = new StringBuffer();
	      }
	  }
	else
	  {
	    buf.append(line[i]);
	  }
      }

    return result;
  }

  /**
   * <p>This method shuts down this controller, freeing up any resources used by this
   * controller.</p>
   */
  
  public synchronized void close()
  {
    if (logWriter != null)
      {
	logWriter.close();
      }

    logWriter = null;
    logFileName = null;
  }
}
