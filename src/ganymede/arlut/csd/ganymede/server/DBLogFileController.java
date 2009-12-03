/*

   DBLogFileController.java

   This controller class manages the recording and retrieval of
   DBLogEvents for the DBLog class, using an on-disk text file for the
   storage format.
   
   Created: 18 February 2003

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996-2009
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

package arlut.csd.ganymede.server;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Vector;

import arlut.csd.Util.ArrayUtils;
import arlut.csd.Util.TranslationService;
import arlut.csd.Util.WordWrap;
import arlut.csd.ganymede.common.Invid;
import arlut.csd.ganymede.common.SchemaConstants;

/*------------------------------------------------------------------------------
                                                                           class
                                                             DBLogFileController

------------------------------------------------------------------------------*/

/**
 * This controller class manages the recording and retrieval of {@link
 * arlut.csd.ganymede.server.DBLogEvent DBLogEvents} for the {@link
 * arlut.csd.ganymede.server.DBLog DBLog} class, using an on-disk text
 * file for the storage format.
 *
 * The file format used by this controller is the classic Ganymede log
 * style, in which each event is stored on a line, with parameters
 * separated by pipe characters.  This file format is documented in
 * the server doc directory at doc/logDesign.html, or on the web at
 * http://www.arlut.utexas.edu/gash2/design/logDesign.html
 *
 * @author Jonathan Abbey, jonabbey@arlut.utexas.edu, ARL:UT
 */

public class DBLogFileController implements DBLogController {

  /**
   * TranslationService object for handling string localization in the
   * Ganymede server.
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.server.DBLogFileController");

  String logFileName = null;
  PrintWriter logWriter = null;

  /* -- */

  /**
   * This constructor should be used for normal purposes in order to have
   * full log file functionality.
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
   * This constructor is used to connect a DBLogFileController to an already
   * existing PrintWriter.  Unlike with the filename constructor, this version
   * does not emit a newline to terminate any incomplete entry in the PrintWriter's
   * stream.  This constructor is thus suitable for use with StringWriters.
   *
   * Note that if this constructor is used, the retrieveHistory() method will
   * throw an IllegalArgumentException, as there will be no file available to read
   * from.
   */

  public DBLogFileController(PrintWriter logWriter)
  {
    this.logFileName = null;
    this.logWriter = logWriter;
  }

  /**
   * This method writes the given event to the persistent storage
   * managed by this controller.
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
   * This method is used to scan the persistent log storage for log
   * events that match invid and that have occurred since
   * sinceTime.
   *
   * @param invid If not null, retrieveHistory() will only return
   * events involving this object invid.
   *
   * @param sinceTime if not null, retrieveHistory() will only return
   * events occuring on or after the time specified in this Date
   * object.
   *
   * @param beforeTime if not null, retrieveHistory() will only return
   * events occurring on or before the time specified in this Date
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
   * @param getLoginEvents if true, this method will return only login
   * and logout events.  if false, this method will return no login
   * and logout events.
   *
   * @return A human-readable multiline string containing a list of
   * history events
   */

  public synchronized StringBuffer retrieveHistory(Invid invid, Date sinceTime, Date beforeTime,
						   boolean keyOnAdmin,
                                                   boolean fullTransactions,
                                                   boolean getLoginEvents)
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
    long beforeLong = 0;
    long timeCode;

    BufferedReader in = null;
    FileReader reader = null;

    if (sinceTime != null)
      {
	sinceLong = sinceTime.getTime();
      }

    if (beforeTime != null)
      {
        beforeLong = beforeTime.getTime();
      }

    // Java can be pretty slow at doing String operations.  String is
    // an immutable class, so doing a lot of String operations is more
    // bulky and costly in Java than it is in Perl.
    //
    // So we have an external Perl accelerator for log data retrieval,
    // known as logscan.pl.  It can efficiently filter out events
    // based on matching Invid, as well as perform a binary search on
    // the log file to find the appropriate starting point if a
    // timecode is presented
    //
    // The calling sequence for logscan.pl has recently changed.
    // Formerly, if only took -a to control whether it should be
    // looking at administrator Invids or object Invids, along with
    // the Invid to match as the last parameter on the command line.
    //
    // The new version of logscan.pl can be called in the same way for
    // backwards compatibility, or it can be called with the following
    // parameters, in any order:
    //
    // -a search for administrator invids rather than object invids
    // -l only search for login/logout events (if -l is not given,
    // login/logout events will always be skipped)
    // -s <java time code> only return events on or after this time code
    // -e <java time code> only return events on or before this time code
    //
    // That is, with the new accelerator, logscan.pl can help winnow
    // out time codes (through the use of a binary search) rather than
    // just matching on invids.

    if (Ganymede.logHelperProperty != null)
      {
        String[] invidArgs = null;
        String[] adminArgs = null;
        String[] startArgs = null;
        String[] beforeArgs = null;
        String[] loginArg = null;

        if (invid != null)
          {
            invidArgs = new String[] {"-i", invid.toString()};
          }

        if (keyOnAdmin)
          {
            adminArgs = new String[] {"-a"};
          }

        if (sinceLong != 0)
          {
            startArgs = new String[] {"-s", Long.toString(sinceLong)};
          }

        if (beforeLong != 0)
          {
            beforeArgs = new String[] {"-e", Long.toString(beforeLong)};
          }

        if (getLoginEvents)
          {
            loginArg = new String[] {"-l"};
          }

        String[] paramArgs = (String[]) ArrayUtils.concat(new String[] {Ganymede.logHelperProperty},
                                                          adminArgs,
                                                          startArgs,
                                                          beforeArgs,
                                                          loginArg,
                                                          invidArgs);  // invid must be last for back compat
        
	java.lang.Runtime runtime = java.lang.Runtime.getRuntime();

	try
	  {
	    java.lang.Process helperProcess;

            helperProcess = runtime.exec(paramArgs);

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

            if ((sinceTime != null && !afterSinceTime) || beforeTime != null)
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

                if (sinceTime != null && timeCode < sinceLong)
                  {
                    afterSinceTime = true;
                    continue; // don't even bother parsing the rest of the line
                  }

                if (beforeTime != null && timeCode > beforeLong)
                  {
                    break;
                  }
              }

	    event = parseEvent(line);

            if (invid.getType() == SchemaConstants.UserBase)
              {
                if (event.eventClassToken.equals("normallogin") ||
                    event.eventClassToken.equals("normallogout") ||
                    event.eventClassToken.equals("abnormallogout"))
                  {
                    if (!getLoginEvents)
                      {
                        continue;       // we don't want to show login/logout activity here
                      }
                  }
                else if (getLoginEvents)
                  {
                    continue;   // we don't want to show non-login/logout activity here
                  }
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

		    // "---------- Transaction {0}: {1} ----------\n\n"
		    buffer.append(ts.l("retrieveHistory.start_trans", event.time, event.adminName));
		  }
		else if (event.eventClassToken.equals("finishtransaction"))
		  {
		    transactionID = null;

		    // "---------- End Transaction {0}: {1} ----------\n\n"
		    buffer.append(ts.l("retrieveHistory.end_trans", event.time, event.adminName));
		  }
		else if (event.eventClassToken.equals("comment"))
		  {
		    // "\n\n{0}\n"
		    buffer.append(ts.l("retrieveHistory.comment",
				       WordWrap.wrap(event.description, 78)));
		  }
		else if (transactionID != null)
		  {
		    // "{0}\n{1}\n"
		    buffer.append(ts.l("retrieveHistory.entry",
				       event.eventClassToken, WordWrap.wrap(event.description, 78, "\t")));
		  }
		else
		  {
		    // "{0,date}: {1} {2}{3}\n"
		    buffer.append(ts.l("retrieveHistory.standalone_entry",
				       event.time, event.adminName, event.eventClassToken, WordWrap.wrap(event.description,78, "\t")));
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
   * This method sets the fields for this DBLogEvent from a logfile
   * line.
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
   * @return  The vector to place the results in, or null if this
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
   * @return The vector to place the results in, or null if this
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
   * This method shuts down this controller, freeing up any resources used by this
   * controller.
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
