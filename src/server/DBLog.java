/*

   DBLog.java

   This class manages recording events in the system log and generating
   reports from the system log based on specific criteria.
   
   Created: 31 October 1997
   Version: $Revision: 1.5 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import java.net.*;
import java.io.*;
import java.util.*;

import Qsmtp;
import arlut.csd.Util.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                      DBLog.java

------------------------------------------------------------------------------*/

public class DBLog {

  static final boolean debug = false;

  // -- 

  String signature = null;
  String logFileName = null;
  File logFile = null;
  FileOutputStream logStream = null;
  PrintWriter logWriter = null;
  Date currentTime = new Date();
  boolean closed = false;
  Hashtable eventCodes = new Hashtable(); // maps event codes strings to DBObjects

  Qsmtp mailer;

  /* -- */

  public DBLog(String filename, GanymedeSession session) throws IOException
  {
    // get the signature to append to mail messages

    loadSignature();

    logFileName = filename;
    logStream = new FileOutputStream(logFileName, true); // append
    logWriter = new PrintWriter(logStream, true); // auto-flush on newline

    logWriter.println();	// emit newline to terminate any incomplete entry

    // now we need to initialize our hash of DBObjects so that we can do
    // speedy lookup of event codes without having to synchronize on
    // the main objectBase hashes during logging

    if (debug)
      {
	System.err.println("Opened log file.. searching for event objects");
      }

    Vector eventCodeVector = session.internalQuery(new Query(SchemaConstants.EventBase));
    Result entry;

    if (eventCodeVector != null)
      {
	for (int i = 0; i < eventCodeVector.size(); i++)
	  {
	    if (debug)
	      {
		System.err.println("Processing event object # " + i);
	      }

	    entry = (Result) eventCodeVector.elementAt(i);
	    
	    eventCodes.put(entry.toString(), new eventType((DBObject)session.view_db_object(entry.getInvid())));
	  }
      }

    // initalize our mailer

    mailer = new Qsmtp(Ganymede.mailHostProperty);
  }

  /**
   *
   * This method closes out the log file.
   *
   */

  synchronized void close() throws IOException
  {
    logWriter.close();
    logStream.close();
    closed = true;

    if (debug)
      {
	System.err.println("DBLog:" + logFileName + " closed.");
      }
  }

  /**
   * This method is used to handle an email notification event, where
   * the mail title should reflect detailed information about the
   * event, and extra descriptive information is to be sent out.
   *
   * mailNotify() will send the message to the owners of any objects
   * referenced by event if mailToOwners is true.
   *
   * description and/or title may be null, in which case the proper
   * strings will be extracted from the event's database record
   *
   * @param event A single event to be logged, with its own timestamp.
   * 
   */

  public synchronized void mailNotify(String title, String description,
				      DBLogEvent event, boolean mailToOwners)
  {
    eventType type;

    /* -- */

    if (closed)
      {
	throw new RuntimeException("log already closed.");
      }

    if (debug)
      {
	System.err.println("Writing log event " + event.eventClassToken);
      }

    if (mailToOwners)
      {
	event.augmentNotifyVect();
      }

    currentTime.setTime(System.currentTimeMillis());
    event.writeEntry(logWriter, currentTime, null);
    
    type = (eventType) eventCodes.get(event.eventClassToken);

    if (type == null)
      {
	return;
      }

    if (type.mail)
      {
	if (debug)
	  {
	    System.err.println("Attempting to email log event " + event.eventClassToken);
	  }

	// prepare our message, word wrap it

	String message = type.description + "\n\n" + event.description + "\n\n";

	if (description != null)
	  {
	    message = message + description + "\n\n";
	  }

	message = arlut.csd.Util.WordWrap.wrap(message, 78);
	
	message = message + signature;

	// get our list of recipients

	Vector emailList = new Vector();

	// first, get the list of recipients from the event's list

	if (event.notifyVect != null)
	  {
	    for (int i = 0; i < event.notifyVect.size(); i++)
	      {
		emailList.addElement(event.notifyVect.elementAt(i));
	      }
	  }

	// then add any we have that the database says should be told 
	// for this event type

	if (type.addressVect != null)
	  {
	    for (int i = 0; i < type.addressVect.size(); i++)
	      {
		emailList.addElement(type.addressVect.elementAt(i));
	      }
	  }

	// and now..

	try
	  {
	    // bombs away!

	    mailer.sendmsg("jonabbey@arlut.utexas.edu", 
			   emailList,
			   "Ganymede: " + ((title == null) ? type.name : title),
			   message);
	  }
	catch (UnknownHostException ex)
	  {
	    throw new RuntimeException("Couldn't figure address " + ex);
	  }
	catch (IOException ex)
	  {
	    throw new RuntimeException("IO problem " + ex);
	  }

	if (debug)
	  {
	    System.err.println("Completed emailing log event " + event.eventClassToken);
	  }
      }
  }

  /**
   *
   * This method is used to log an event such as server shutdown/restart,
   * user log-in, persona change, etc.  Basically any thing not associated
   * with a transaction.
   *
   * @param event A single event to be logged, with its own timestamp.
   *
   */

  public synchronized void logSystemEvent(DBLogEvent event)
  {
    eventType type;

    /* -- */

    if (closed)
      {
	throw new RuntimeException("log already closed.");
      }

    if (debug)
      {
	System.err.println("Writing log event " + event.eventClassToken);
      }

    currentTime.setTime(System.currentTimeMillis());
    event.writeEntry(logWriter, currentTime, null);
    
    type = (eventType) eventCodes.get(event.eventClassToken);

    if (type == null)
      {
	return;
      }

    if (type.mail)
      {
	if (debug)
	  {
	    System.err.println("Attempting to email log event " + event.eventClassToken);
	  }

	// prepare our message, word wrap it

	String message = type.description + "\n\n" + event.description + "\n\n";

	message = arlut.csd.Util.WordWrap.wrap(message, 78);
	
	message = message + signature;

	// get our list of recipients

	Vector emailList = new Vector();

	if (event.notifyVect != null)
	  {
	    for (int i = 0; i < event.notifyVect.size(); i++)
	      {
		emailList.addElement(event.notifyVect.elementAt(i));
	      }
	  }

	if (type.addressVect != null)
	  {
	    for (int i = 0; i < type.addressVect.size(); i++)
	      {
		emailList.addElement(type.addressVect.elementAt(i));
	      }
	  }

	// and now..

	try
	  {
	    // bombs away!

	    mailer.sendmsg("jonabbey@arlut.utexas.edu", 
			   emailList,
			   "Ganymede: " + type.name,
			   message);
	  }
	catch (UnknownHostException ex)
	  {
	    throw new RuntimeException("Couldn't figure address " + ex);
	  }
	catch (IOException ex)
	  {
	    throw new RuntimeException("IO problem " + ex);
	  }

	if (debug)
	  {
	    System.err.println("Completed emailing log event " + event.eventClassToken);
	  }
      }
  }

  /**
   *
   * This method is used to log all events associated with a transaction.
   *
   * @param logEvents a Vector of DBLogEvent objects.
   *
   */

  public synchronized void logTransaction(Vector logEvents, String adminName, 
					  Invid admin)
  {
    StringBuffer buffer = new StringBuffer();
    String transactionID;
    boolean found;
    DBLogEvent event;
    Enumeration enum;
    Hashtable mailOuts = new Hashtable();

    /* -- */

    if (closed)
      {
	throw new RuntimeException("log already closed.");
      }

    currentTime.setTime(System.currentTimeMillis());
    transactionID = adminName + ":" + currentTime.getTime();

    Vector objects = new Vector();

    // get a list of all objects affected by this transaction
    
    for (int i = 0; i < logEvents.size(); i++)
      {
	event = (DBLogEvent) logEvents.elementAt(i); 

	for (int j = 0; j < event.objects.size(); j++)
	  {
	    Invid inv = (Invid) event.objects.elementAt(j);

	    if (!objects.contains(inv))
	      {
		objects.addElement(inv);
	      }
	  }
      }

    new DBLogEvent("starttransaction",
		   "Start Transaction",
		   admin,
		   adminName,
		   objects,
		   null).writeEntry(logWriter, currentTime, transactionID);

    for (int i = 0; i < logEvents.size(); i++)
      {
	event = (DBLogEvent) logEvents.elementAt(i);

	if (debug)
	  {
	    System.err.println("Writing: " + event.description);
	  }

	appendMailOut(event, mailOuts);
	event.writeEntry(logWriter, currentTime, transactionID);
      }

    new DBLogEvent("finishtransaction",
		   "Finish Transaction",
		   admin,
		   adminName,
		   null,
		   null).writeEntry(logWriter, currentTime, transactionID);

    enum = mailOuts.elements();

    while (enum.hasMoreElements())
      {
	MailOut mailout = (MailOut) enum.nextElement();
	String description = arlut.csd.Util.WordWrap.wrap(mailout.toString(), 78) + "\n" + signature;

	if (debug)
	  {
	    System.err.println("Sending mail to " + (String) mailout.addresses.elementAt(0));
	  }

	try
	  {
	    mailer.sendmsg("jonabbey@arlut.utexas.edu", 
			   mailout.addresses,
			   "Ganymede: transaction notification",
			   description);
	  }
	catch (UnknownHostException ex)
	  {
	    throw new RuntimeException("Couldn't figure address " + ex);
	  }
	catch (IOException ex)
	  {
	    throw new RuntimeException("IO problem " + ex);
	  }
      }
  }

  /**
   *
   * This method is used to scan the log file for log events that match
   * invid and that have occurred since sinceTime.
   *
   * @param invid If not null, retrieveHistory() will only return events involving
   * this object invid.
   *
   * @param sinceTime if not null, retrieveHistory() will only return events
   * occuring on or after the time specified in this Date object.
   *
   * @param keyOnAdmin if true, rather than returning a string containing events
   * that involved <invid>, retrieveHistory() will return a string containing events
   * performed on behalf of the administrator with invid <invid>.
   *
   * @return A human-readable multiline string containing a list of history events
   *
   */

  public synchronized StringBuffer retrieveHistory(Invid invid, Date sinceTime, boolean keyOnAdmin)
  {
    FileReader reader;

    try
      {
	reader = new FileReader(logFileName);
      }
    catch (FileNotFoundException ex)
      {
	return null;
      }

    BufferedReader in = new BufferedReader(reader);
    StringBuffer buffer = new StringBuffer();
    DBLogEvent event;
    String line;
    String transactionID = null;

    boolean afterSinceTime = false;
    String dateString;
    long timeCode;
    Date time;

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

	    event = new DBLogEvent(line);

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
		for (int i = 0; i < event.objects.size(); i++)
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
			found = true;
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

    try
      {
	in.close();
	reader.close();
      }
    catch (IOException ex)
      {
	// shrug
      }

    return buffer;
  }

  // -----

  /**
   *
   * This method takes a DBLogEvent object, scans it to determine
   * what mailing lists should be notified of the event in the
   * context of a transaction, and adds a description of the
   * passed in event to each MailOut object held in map.
   *
   * That is, the map passed in maps each discrete recipient
   * list to a running MailOut object which has the complete
   * text that will be mailed to that recipient when the
   * transaction's records are mailed out.
   *
   */

  private void appendMailOut(DBLogEvent event, Hashtable map)
  {
    Enumeration enum;
    String str;
    MailOut mailout;

    /* -- */

    event.augmentNotifyVect();
    
    enum = event.notifyVect.elements();

    while (enum.hasMoreElements())
      {
	str = (String) enum.nextElement();

	if (debug)
	  {
	    System.err.println("Going to be mailing to " + str);
	  }

	mailout = (MailOut) map.get(str);

	if (mailout == null)
	  {
	    mailout = new MailOut(str);
	    mailout.append(event.description);
	    map.put(str, mailout);
	  }
	else
	  {
	    mailout.append("\n");
	    mailout.append(event.description);
	  }
      }
  }

  /**
   *
   * This method gets the signature file loaded.
   *
   */

  private void loadSignature() throws IOException
  {
    StringBuffer buffer = new StringBuffer();
    FileInputStream in = new FileInputStream(Ganymede.signatureFileProperty);
    BufferedInputStream inBuf = new BufferedInputStream(in);
    int c;
    
    /* -- */

    c = inBuf.read();

    while (c != -1)
      {
	buffer.append((char) c);
	c = inBuf.read();
      }

    inBuf.close();
    in.close();

    signature = buffer.toString();

    if (debug)
      {
	System.err.println("Loaded signature string:");
	System.err.println(signature);
	System.err.println("----");
      }
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                                       eventType

------------------------------------------------------------------------------*/

/**
 *
 * This class is used to store event information derived from the Ganymede
 * database.
 *
 */

class eventType {

  String token;
  String name;
  String description;
  boolean mail;
  String addresses;
  Vector addressVect;
  boolean ccToSelf;

  private DBField f;

  /* -- */

  eventType(DBObject obj)
  {
    token = getString(obj, SchemaConstants.EventToken);
    name = getString(obj, SchemaConstants.EventName);
    description = getString(obj, SchemaConstants.EventDescription);
    mail = getBoolean(obj, SchemaConstants.EventMailBoolean);
    addresses = getAddresses(obj);
    ccToSelf = getBoolean(obj, SchemaConstants.EventMailToSelf);
  }

  private String getString(DBObject obj, short fieldId)
  {
    f = (DBField) obj.getField(fieldId);

    if (f == null)
      {
	return "";
      }

    return (String) f.getValueLocal();
  }

  private boolean getBoolean(DBObject obj, short fieldId)
  {
    f = (DBField) obj.getField(fieldId);

    if (f == null)
      {
	return false;
      }

    return ((Boolean) f.getValueLocal()).booleanValue();
  }

  /**
   *
   * This method takes an event definition object and extracts
   * a list of email addresses to which mail will be sent when
   * an event of this type is logged.
   *
   */

  private String getAddresses(DBObject obj)
  {
    StringBuffer result = new StringBuffer();
    InvidDBField invF;
    Enumeration enum;
    Invid tmpI;
    String tmpS;

    /* -- */

    invF = (InvidDBField) obj.getField(SchemaConstants.EventMailList);
    
    if (invF == null)
      {
	addressVect = new Vector(); // empty vect
	return "";
      }

    addressVect = new Vector();

    enum = invF.values.elements();

    while (enum.hasMoreElements())
      {
	tmpI = (Invid) enum.nextElement();

	if (tmpI != null)
	  {
	    tmpS = Ganymede.internalSession.viewObjectLabel(tmpI);

	    addressVect.addElement(tmpS);	    
	  }
      }

    for (int i = 0; i < addressVect.size(); i++)
      {
	if (i > 0)
	  {
	    result.append(", ");
	  }

	result.append((String) addressVect.elementAt(i));
      }

    return result.toString();
  }

}

/*------------------------------------------------------------------------------
                                                                           class
								         MailOut

------------------------------------------------------------------------------*/

/**
 *
 * This class is used to store event information derived from the Ganymede
 * database.
 *
 */

class MailOut {

  StringBuffer description = new StringBuffer();
  Vector addresses;

  /* -- */

  MailOut(String address)
  {
    if (address == null)
      {
	throw new NullPointerException("bad address");
      }

    addresses = new Vector();
    addresses.addElement(address);
  }

  void append(String text)
  {
    description.append(text);
  }

  public String toString()
  {
    return description.toString();
  }
}
