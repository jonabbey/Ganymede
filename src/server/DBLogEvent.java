/*

   DBLogEvent.java

   This class stores a complete record of a single sub-transactional event,
   to be emitted to the DBLog log file, or sent to a set of users via
   email..
   
   Created: 31 October 1997
   Version: $Revision: 1.2 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import java.io.*;
import java.util.*;
import arlut.csd.Util.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                      DBLog.java

------------------------------------------------------------------------------*/

/**
 *
 * This class defines the data to be recorded in a log/mail record.
 *
 *
 */

public class DBLogEvent {

  static final boolean debug = false;

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
    this.eventClassToken = eventClassToken;
    this.description = description;
    this.admin = admin;
    this.adminName = adminName;
    this.objects = objects;

    this.notifyVect = notifyList;

    if (notifyList != null)
      {
	StringBuffer list = new StringBuffer();

	for (int i = 0; i < notifyList.size(); i++)
	  {
	    if (i > 0)
	      {
		list.append(", ");
	      }
	    
	    list.append((String) notifyList.elementAt(i));
	  }

	this.notifyList = list.toString();
      }
    else
      {
	this.notifyList = "";
	notifyVect = null;
      }
  }
  
  public DBLogEvent(String line) throws IOException
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
    
    time = new Date(timeCode);
    
    j = i+1;
    i = scanSep(cary, j);	// find next |, skip human readable date
    
    j = i+1;
    i = scanSep(cary, j);
    
    eventClassToken = readNextField(cary, j);
    
    j = i+1;
    i = scanSep(cary, j);
    
    tmp = readNextField(cary, j);
    
    if (!tmp.equals(""))
      {
	admin = new Invid(tmp);	// get admin invid
      }

    j = i+1;
    i = scanSep(cary, j);

    adminName = readNextField(cary, j); // get admin name
    
    j = i+1;
    i = scanSep(cary, j);

    transactionID = readNextField(cary, j); // get transaction id

    j = i+1;
    i = scanSep(cary, j);

    objects = readObjectVect(cary, j);

    j = i+1;
    i = scanSep(cary, j);

    description = readNextField(cary, j); // get text description

    j = i+1;
    i = scanSep(cary, j);

    notifyVect = readNotifyVect(cary, j);

    if (notifyVect != null)
      {
	StringBuffer list = new StringBuffer();

	for (int k = 0; k < notifyVect.size(); k++)
	  {
	    if (i > 0)
	      {
		list.append(", ");
	      }
	    
	    list.append((String) notifyVect.elementAt(k));
	  }

	this.notifyList = list.toString();
      }

    augmented = true;
  }

  // find the index of the next field separator, taking into account
  // escaped chars

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

  // scan the string for this field, decoding backslash escapes in the
  // process

  private String readNextField(char[] line, int startIndex)
  {
    StringBuffer buffer = new StringBuffer();
    boolean doit = true;

    /* -- */

    for (int i = startIndex; (i < line.length) && (line[i] != '|'); i++)
      {
	doit = true;

	if (line[i] == '\\')	// skip backslashing
	  {
	    i++;

	    if (line[i] == 'n')
	      {
		buffer.append('\n');
		doit = false;
	      }
	  }

	if (doit)
	  {
	    buffer.append(line[i]);
	  }
      }

    return buffer.toString();
  }

  private Vector readObjectVect(char[] line, int startIndex)
  {
    Vector result = new Vector();
    StringBuffer buffer = new StringBuffer();
    int i;

    /* -- */

    for (i = startIndex; (i < line.length) && (line[i] != '|'); i++)
      {
	if (line[i] == '\\')	// skip backslashing
	  {
	    i++;		// assume no newlines in object list
	  }

	if (line[i] == ',')
	  {
	    if (buffer.length() != 0)
	      {
		result.addElement(new Invid(buffer.toString()));
		buffer.setLength(0); // clear for next
	      }
	  }
	else
	  {
	    buffer.append(line[i]);
	  }
      }

    try
      {
	if (line[i] == '|')
	  {
	    if (buffer.length() != 0)
	      {
		result.addElement(new Invid(buffer.toString()));
	      }
	  }
      }
    catch (IndexOutOfBoundsException ex)
      {
	System.err.println("bad parse on line " + new String(line));
      }

    return result;
  }

  private Vector readNotifyVect(char[] line, int startIndex)
  {
    Vector result = new Vector();
    StringBuffer buffer = new StringBuffer();
    int i;

    /* -- */

    for (i = startIndex; (i < line.length) && (line[i] != '|'); i++)
      {
	if (line[i] == '\\')	// skip backslashing
	  {
	    i++;
	  }

	if (line[i] == ',' || line[i] == '|')
	  {
	    if (buffer.length() != 0)
	      {
		result.addElement(buffer.toString().trim());
		buffer.setLength(0); // clear for next
	      }
	  }
	else
	  {
	    buffer.append(line[i]);
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
   * This method augments the notify list vector with a list of
   * all non-supergash personae that have ownership
   *
   */

  public void augmentNotifyVect()
  {
    Enumeration enum, enum2;
    Invid invid, invid2, invid3;
    InvidDBField invidField, invidField2;
    DBObject object, object2;
    Vector vect;
    
    /* -- */

    if (augmented)
      {
	return;
      }

    if (debug)
      {
	System.err.println("augmentNotifyVect: entering");
      }

    if (notifyVect == null)
      {
	notifyVect = new Vector();
      }

    // examine the list of invid's that are involved in this event

    enum = objects.elements();

    while (enum.hasMoreElements())
      {
	invid = (Invid) enum.nextElement();
	object = (DBObject) Ganymede.internalSession.view_db_object(invid);

	if (object == null)
	  {
	    if (debug)
	      {
		System.err.println("augmentNotifyVect: Couldn't find invid " + invid.toString());
	      }

	    continue;
	  }

	if (object.isEmbedded())
	  {
	    if (debug)
	      {
		System.err.println("augmentNotifyVect: Skipping Embeded invid " + invid.toString());
	      }

	    continue;
	  }

	invidField = (InvidDBField) object.getField(SchemaConstants.OwnerListField);
	
	if (invidField == null)
	  {
	    if (debug)
	      {
		System.err.println("augmentNotifyVect: Couldn't access owner list for invid " + invid.toString());
	      }

	    continue;
	  }

	vect = invidField.getValues();

	if (vect == null)
	  {
	    if (debug)
	      {
		System.err.println("augmentNotifyVect: Empty owner list for invid " + invid.toString());
	      }

	    continue;
	  }

	enum2 = vect.elements(); // this object's owner list

	while (enum2.hasMoreElements())
	  {
	    invid2 = (Invid) enum2.nextElement();

	    if (invid2.getNum() == 0)
	      {
		continue;	// don't want supergash
	      }

	    object2 = (DBObject) Ganymede.internalSession.view_db_object(invid2);

	    if (object2 == null)
	      {
		if (debug)
		  {
		    System.err.println("augmentNotifyVect: Couldn't look up owner group " + 
				       invid2.toString() + " for invid " + invid.toString());
		  }

		continue;
	      }

	    // now, what mail lists should we add?

	    invidField2 = (InvidDBField) object2.getField(SchemaConstants.OwnerMailList);

	    if (invidField2 == null)
	      {
		if (debug)
		  {
		    System.err.println("augmentNotifyVect: No mail list defined for owner group " + 
				       invid2.toString() + " for invid " + invid.toString());
		  }

		continue;
	      }

	    Vector tmpVect = invidField2.getValues();

	    if (tmpVect == null)
	      {
		continue;
	      }

	    for (int i = 0; i < tmpVect.size(); i++)
	      {
		invid3 = (Invid) tmpVect.elementAt(i);

		notifyVect.addElement(Ganymede.internalSession.viewObjectLabel(invid3));
	      }
	  }
      }

    // now update notifyList

    if (notifyVect != null)
      {
	StringBuffer list = new StringBuffer();

	for (int i = 0; i < notifyVect.size(); i++)
	  {
	    if (i > 0)
	      {
		list.append(", ");
	      }
	    
	    list.append((String) notifyVect.elementAt(i));
	  }

	this.notifyList = list.toString();
      }

    augmented = true;
  }


  private final void writeSep(PrintWriter logWriter)
  {
    logWriter.print('|');
  }

  private final void writeStr(PrintWriter logWriter, String in)
  {
    logWriter.print(escapeStr(in));
  }

  private final String escapeStr(String in)
  {
    char[] ary = in.toCharArray();
    StringBuffer result = new StringBuffer();

    /* -- */

    for (int i = 0; i < ary.length; i++)
      {
	if (ary[i] == '\n')
	  {
	    result.append("\\n");
	  }
	else if (ary[i] == '|')
	  {
	    result.append("\\|");
	  }
	else
	  {
	    result.append(ary[i]);
	  }
      }

    return result.toString();
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
