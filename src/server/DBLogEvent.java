/*

   DBLogEvent.java

   This class stores a complete record of a single sub-transactional event,
   to be emitted to the DBLog log file, or sent to a set of users via
   email..
   
   Created: 31 October 1997
   Version: $Revision: 1.9 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import java.io.*;
import java.util.*;
import arlut.csd.Util.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                 DBLogEvent.java

------------------------------------------------------------------------------*/

/**
 * This class stores a complete record of a single sub-transactional event,
 * to be emitted to the DBLog log file, or sent to a set of users via
 * email..
 */

public class DBLogEvent {

  static final boolean debug = false;

  // ---

  String eventClassToken;
  String description;
  Invid admin;
  String adminName;
  Vector objects;
  String notifyList;
  Vector notifyVect;

  // the following are used when loading an event from the on-disk log

  String transactionID;		// used for DBLog.retrieveHistory()
  Date time = null;		// used for DBLog.retrieveHistory()

  boolean augmented = false;

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

  /**
   *
   * @param eventClassToken a short string specifying a DBObject
   * record describing the general category for the event
   * @param description Descriptive text to be entered in the record of the event
   * @param admin Invid pointing to the adminPersona that fired the event, if any
   * @param adminName String containing the name of the adminPersona that fired the event, if any
   * @param objects A vector of invids of objects involved in this event.
   * @param notifyList A vector of Strings listing email addresses to send notification
   * of this event to.
   * 
   */

  public DBLogEvent(String eventClassToken, String description,
		    Invid admin, String adminName,
		    Vector objects, Vector notifyList)
  {
    this(eventClassToken, description, admin, adminName, objects, notifyList, null);
  }

  /**
   *
   * @param eventClassToken a short string specifying a DBObject
   * record describing the general category for the event
   * @param description Descriptive text to be entered in the record of the event
   * @param admin Invid pointing to the adminPersona that fired the event, if any
   * @param adminName String containing the name of the adminPersona that fired the event, if any
   * @param objects A vector of invids of objects involved in this event.
   * @param notifyList A vector of Strings listing email addresses to send notification
   * of this event to.
   * @param multibuffer A SharedStringBuffer that this event can use to avoid an extra object create.
   * If you provide a StringBuffer here, be aware that this object may use it both when this
   * object is being constructed, and by the writeEvent() method.  The code that uses this
   * DBLogEvent may do whatever it likes with the buffer between method calls to this DBLogEvent,
   * but any method called on this object might clobber the buffer.
   * 
   * 
   */

  public DBLogEvent(String eventClassToken, String description,
		    Invid admin, String adminName,
		    Vector objects, Vector notifyList,
		    SharedStringBuffer multibuffer)
  {
    this.eventClassToken = eventClassToken;
    this.description = description;
    this.admin = admin;
    this.adminName = adminName;
    this.objects = objects;
    this.multibuffer = multibuffer;

    if (this.multibuffer == null)
      {
	multibuffer = this.multibuffer = new SharedStringBuffer();
      }
    else
      {
	this.multibuffer.setLength(0);
      }

    this.notifyVect = notifyList;

    if (notifyList != null)
      {
	for (int i = 0; i < notifyList.size(); i++)
	  {
	    if (i > 0)
	      {
		multibuffer.append(", ");
	      }
	    
	    multibuffer.append((String) notifyList.elementAt(i));
	  }

	this.notifyList = multibuffer.toString();
      }
    else
      {
	this.notifyList = "";
	notifyVect = null;
      }
  }

  /**
   *
   * Constructor to construct a DBLogEvent from a log file line.
   *
   * @param line A line from the Ganymede log file
   *
   */
  
  public DBLogEvent(String line) throws IOException
  {
    this(line, null);
  }

  /**
   *
   * Constructor to construct a DBLogEvent from a log file line.
   *
   * @param line A line from the Ganymede log file
   * @param multibuffer A SharedStringBuffer that this event can use to avoid an extra object create
   *
   */

  public DBLogEvent(String line, SharedStringBuffer multibuffer) throws IOException
  {
    this.multibuffer = multibuffer;

    if (this.multibuffer == null)
      {
	multibuffer = this.multibuffer = new SharedStringBuffer();
      }

    loadLine(line);
  }

  /**
   *
   * This method sets the fields for this DBLogEvent from a logfile line.<br><br>
   *
   * Note that this method is designed to reuse as many of this
   * DBLogEvent's fields as possible, so if external code keeps
   * references to this.time or this.objects or this.notifyVect, that
   * code should either not call loadLine(), or should make its own
   * copies of those objects.
   *  
   */

  public void loadLine(String line) throws IOException
  {
    int i, j;
    String dateString;
    char[] cary;
    long timeCode;
    String tmp;

    /* -- */

    if (line == null || (line.trim().equals("")))
      {
	throw new IOException("empty log line");
      }

    multibuffer.setLength(0);

    //    System.out.println("Trying to create DBLogEvent: " + line);

    cary = line.toCharArray();

    i = line.indexOf('|');
    dateString = line.substring(0, i);
    
    try
      {
	timeCode = new Long(dateString).longValue();
      }
    catch (NumberFormatException ex)
      {
	throw new IOException("couldn't parse time code");
      }
    
    if (this.time == null)
      {
	this.time = new Date(timeCode);
      }
    else
      {
	this.time.setTime(timeCode);
      }
    
    j = i+1;
    i = scanSep(cary, j);	// find next |, skip human readable date
    
    j = i+1;
    i = scanSep(cary, j);
    
    this.eventClassToken = readNextField(cary, j);
    
    j = i+1;
    i = scanSep(cary, j);
    
    tmp = readNextField(cary, j);
    
    if (!tmp.equals(""))
      {
	this.admin = new Invid(tmp);	// get admin invid
      }
    else
      {
	// we have to be sure to do this.

	this.admin = null;
      }

    j = i+1;
    i = scanSep(cary, j);

    this.adminName = readNextField(cary, j); // get admin name
    
    j = i+1;
    i = scanSep(cary, j);

    this.transactionID = readNextField(cary, j); // get transaction id

    j = i+1;
    i = scanSep(cary, j);

    // read the object invid list.. re-use this.objects if it already
    // exists

    this.objects = readObjectVect(cary, j, this.objects);

    j = i+1;
    i = scanSep(cary, j);

    this.description = readNextField(cary, j); // get text description

    j = i+1;
    i = scanSep(cary, j);

    // read the email address list.. re-use this.notifyVect if it
    // already exists

    this.notifyVect = readNotifyVect(cary, j, this.notifyVect);

    if (notifyVect != null)
      {
	multibuffer.setLength(0);

	for (int k = 0; k < notifyVect.size(); k++)
	  {
	    if (i > 0)
	      {
		multibuffer.append(", ");
	      }
	    
	    multibuffer.append((String) notifyVect.elementAt(k));
	  }

	this.notifyList = multibuffer.toString();
      }

    this.augmented = true;
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
		result.addElement(new Invid(multibuffer.toString()));
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
		result.addElement(new Invid(multibuffer.toString()));
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
   *
   * This method writes out this event to a log stream
   *
   */

  public void writeEntry(PrintWriter logWriter, Date currentTime, String transactionID)
  {
    logWriter.print(currentTime.getTime());
    writeSep(logWriter);
    writeStr(logWriter, currentTime.toString());
    writeSep(logWriter);
    writeStr(logWriter, eventClassToken);
    writeSep(logWriter);

    if (admin != null)
      {
	writeStr(logWriter, admin.toString());
      }

    writeSep(logWriter);

    if (adminName != null)
      {
	writeStr(logWriter, adminName);
      }

    writeSep(logWriter);

    if (transactionID != null)
      {
	writeStr(logWriter, transactionID);
      }

    writeSep(logWriter);

    if (objects != null)
      {
	for (int i = 0; i < objects.size(); i++)
	  {
	    if (i > 0)
	      {
		logWriter.print(',');
	      }

	    writeStr(logWriter, objects.elementAt(i).toString());
	  }
      }

    writeSep(logWriter);

    if (description != null)
      {
	writeStr(logWriter, description);
      }

    writeSep(logWriter);

    logWriter.println(notifyList);
  }

  /**
   *
   * This method is used by DBLog to set the list of email targets
   * that this event will need to be mailed to.
   * 
   */

  public synchronized void setMailTargets(Vector mailTargets)
  {
    this.notifyVect = mailTargets;

    if (mailTargets == null)
      {
	return;
      }

    // we want to set the notifyList String as well as the
    // notifyVect vector..
    
    this.multibuffer.setLength(0);

    for (int i = 0; i < notifyVect.size(); i++)
      {
	if (i > 0)
	  {
	    this.multibuffer.append(", ");
	  }
	    
	this.multibuffer.append((String) notifyVect.elementAt(i));
      }
    
    this.notifyList = multibuffer.toString();
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
	else if (ary[i] == '|')
	  {
	    multibuffer.append("\\|");
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
   * Debug rig.. this will scan a log file and attempt to parse lines out of it
   *
   */

  static public void main(String argv[])
  {
    FileReader reader;

    try
      {
	reader = new FileReader("/home/broccol/gash2/code/arlut/csd/ganymede/db/log");
      }
    catch (FileNotFoundException ex)
      {
	return;
      }

    BufferedReader in = new BufferedReader(reader);
    StringBuffer buffer = new StringBuffer();
    DBLogEvent event = null;
    String line;
    Invid invid = new Invid((short) 3, 336);

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

	    try
	      {
		event = new DBLogEvent(line);
	      }
	    catch (StringIndexOutOfBoundsException ex)
	      {
		System.err.println("Caught exception: " + ex + " on line " + line);
		throw ex;
	      }

	    boolean found = false;

	    for (int i = 0; i < event.objects.size(); i++)
	      {
		if (invid.equals((Invid) event.objects.elementAt(i)))
		  {
		    found = true;
		  }
	      }

	    if (found)
	      {
		String tmp = event.time.toString() + ": " + event.adminName + " - " + 
		  event.eventClassToken + "\n" +
		  WordWrap.wrap(event.description, 78, "\t") + "\n";
		
		buffer.append(tmp);
	      }
	  }
      }
    catch (IOException ex)
      {
	System.out.println("IO exception at last");
	// eof
      }

    try
      {
	in.close();
	reader.close();
      }
    catch (IOException ex)
      {
	// shrug
      }

    System.out.println(buffer.toString());
  }
}
