/*********************************************************************

This code was released by James Driscoll into the public domain.

See http://www.io.com/~maus/JavaPage.html for the statement of
public domain release.

@version 1.00 7/17/96
@author James Driscoll maus@io.com


Usage -

Version History:
0.8 5/15/96  - First version
0.9 5/16/96  - fixed date code, added localhost to HELO,
               fixed Subject bug
0.91 7/10/96  - Yet another date fix, for European TimeZones.  Man, they
                gotta fix that code...
1.00 7/17/96  - renamed to Qsmtp, as I have plans for the SMTP code,
                and I want to get this out and announced.  Also cleaned it
                up and commented out the DEBUG code (for size, just in case
                the compiler didn't optimize it out on your machine - mine
                didn't (Symantec Cafe Lite, you get what you pay for, and
                I paid for a book)).
1.01 9/18/96  - Fixed the call to getLocalHost local, which 1.02 JDK didn't
                like (Cafe Lite didn't mind, though).  Think I'll be using
                JDK for all compliations from now on.  Also, added a close
                method, since finalize() is not guarenteed to be called(!).
1.1 12/26/96 -  Fixed problem with EOL, I was using the Unix EOL, not the
                network end of line.  A fragile mail server was barfing.
                I can't beleive I wrote this - that's what half a year will do.
                Also, yanked out the debug code.  It annoyed me.
1.11 12/27/97 - Forgot to flush(), println used to do that for me...

-- 

Modifications by Jonathan Abbey (jonabbey@arlut.utexas.edu):

Mods integrated with 1.11 on 19 January 1999

Made this class open and close connection to the mailer during the
sendMsg() method, rather than having to do a separate close() and
recreate a new Qsmtp object to send an additional message.

Modified the sendMsg() to_address parameter to support a vector of
addresses.

Added the sendHTMLmsg() method to allow for sending MIME-attached
html pages.

Added the extraHeaders parameter to sendMsg() to support sendHTMLmsg().

Modified the code to use the 1.1 io and text formatting classes.

Added javadocs (9 June 1999).

***********************************************************************/

package Qsmtp;			// thanks javac 1.4

import java.net.*;
import java.io.*;
import java.util.*;
import java.text.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                           Qsmtp

------------------------------------------------------------------------------*/

/**
 * <P>SMTP mailer class, used to send email messages (with optional HTML MIME
 * attachments) through direct TCP/IP communication with Internet SMTP mail
 * servers.</P>
 *
 * <P>The Qsmtp constructors take an address for a SMTP mail server, and all 
 * messages subsequently sent out by the Qstmp object are handled by
 * that SMTP server.</P>
 *
 * <P>Once created, a Qsmtp object can be used to send any number of messages
 * through that mail server.  Each call to 
 * {@link Qsmtp#sendmsg(java.lang.String, java.util.Vector, java.lang.String, 
 * java.lang.String) sendmsg} or
 * {@link Qsmtp#sendHTMLmsg(java.lang.String, java.util.Vector, java.lang.String, 
 * java.lang.String, java.lang.String, java.lang.String) sendHTMLmsg} opens a
 * separate SMTP connection to the designated mail server and transmits a
 * single message.</P>
 *
 * <P>Because this class opens a socket to a potentially remote TCP/IP server,
 * this class may not function properly when used within an applet.</P>
 */

public class Qsmtp implements Runnable {

  static final boolean debug = false;
  static final int DEFAULT_PORT = 25;
  static final String EOL = "\r\n"; // network end of line

  // --

  private String hostid = null;
  private InetAddress address = null;
  private int port = DEFAULT_PORT;

  private Vector queuedMessages = new Vector();
  private boolean threaded = false;
  private Thread backgroundThread;

  /* -- */

  public Qsmtp(String hostid)
  {
    this.hostid = hostid;
  }

  public Qsmtp(String hostid, int port)
  {
    this.hostid = hostid;
    this.port = port;
  }

  public Qsmtp(InetAddress address)
  {
    this(address, DEFAULT_PORT);
  }

  public Qsmtp(InetAddress address, int port)
  {
    this.address = address;
    this.port = port;
  }

  /** 
   * <P>After this method is called, all further sendMsg() calls will
   * not directly send mail themselves, but will rather queue the mail
   * for sending by a back-ground thread.</P>
   *
   * <P>One result of this is that after this is called, the sendMsg()
   * methods will never throw Protcol or IO Exceptions, and no
   * success/failure results will be returned.</P> 
   *
   * <P>If this method is called while an previous background thread
   * that was ordered to stop by stopThreaded() is still shutting
   * down, this method will block until the old background thread
   * dies and the new background thread can be established.</P>
   */

  public synchronized void goThreaded()
  {
    if (this.threaded)
      {
	return;
      }

    while (backgroundThread != null)
      {
	try
	  {
	    this.wait();
	  }
	catch (InterruptedException ex)
	  {
	  }
      }

    backgroundThread = new Thread(this);
    this.threaded = true;
    backgroundThread.start();
  }

  /**
   * <P>Calling this method turns off the background thread and
   * returns Qsmtp to normal blocking operation.</P>
   */

  public synchronized void stopThreaded()
  {
    if (debug)
      {
	System.err.println("Qstmp.stopThreaded()");
      }

    if (!this.threaded)
      {
	return;
      }

    if (backgroundThread != null)
      {
	this.threaded = false;

	if (debug)
	  {
	    System.err.println("Qstmp.stopThreaded() - waking background thread");
	  }
	
	synchronized (queuedMessages)
	  {
	    queuedMessages.notifyAll();
	  }

	// the background thread will kill itself off cleanly

	try
	  {
	    if (debug)
	      {
		System.err.println("Qstmp.stopThreaded() - waiting for background thread to die");
	      }

	    // the backgroundThread variable is cleared when the
	    // background thread terminates.  If that happens before
	    // we wait for it, catch the NullPointerException and move
	    // on.

	    try
	      {
		backgroundThread.join(); // wait for our email sending thread to drain
	      }
	    catch (NullPointerException ex)
	      {
		return;
	      }

	    if (debug)
	      {
		System.err.println("Qstmp.stopThreaded() - background thread completed");
	      }
	  }
	catch (InterruptedException ex)
	  {
	    return;		// oh, well.
	  }
      }
  }

  /**
   * <P>Sends a plain ASCII mail message</P>
   *
   * @param from_address Who is sending this message?
   * @param to_addresses Vector of string addresses to send this message to
   * @param subject Subject for this message
   * @param message The text for the mail message
   *
   * @exception IOException
   */

  public synchronized void sendmsg(String from_address, Vector to_addresses,
				   String subject, String message) throws IOException
  {
    sendmsg(from_address, to_addresses, subject, message, null);
  }

  /**
   * <P>Sends a message with a MIME-attached HTML message</P>
   *
   * <p>In a perfect world, we'd do a generic MIME-capable mail system here, but
   * as it is, we only support HTML.</p>
   *
   * @param from_address Who is sending this message?
   * @param to_addresses Vector of string addresses to send this message to
   * @param subject Subject for this message
   * @param htmlBody A string containing the HTML document to be sent
   * @param htmlFilename The name to label the HTML document with, will
   * show up in mail clients
   * @param textBody The text for the non-HTML part of the mail message
   *
   * @exception IOException
   */

  public synchronized void sendHTMLmsg(String from_address, Vector to_addresses,
				       String subject, String htmlBody, String htmlFilename,
				       String textBody) throws IOException
  {
    Vector MIMEheaders = new Vector();
    String separator = "B24FDA77DFMIMEISNEAT4976B1CA5E8A49";
    StringBuffer buffer = new StringBuffer();

    /* -- */

    MIMEheaders.addElement("MIME-Version: 1.0");
    MIMEheaders.addElement("Content-Type: multipart/mixed; boundary=\"" + separator + "\"");

    buffer.append("This is a multi-part message in MIME format.\n");
    
    if (textBody != null)
      {
	buffer.append("--");
	buffer.append(separator);
	buffer.append("\nContent-Type: text/plain; charset=us-ascii\n");
	buffer.append("Content-Transfer-Encoding: 7bit\n\n");
	buffer.append(textBody);
	buffer.append("\n");
      }

    if (htmlBody != null)
      {
	buffer.append("--");
	buffer.append(separator);
	buffer.append("\nContent-Type: text/html; charset=us-ascii\n");
	buffer.append("Content-Transfer-Encoding: 7bit\n");
	
	if (htmlFilename != null && !htmlFilename.equals(""))
	  {
	    buffer.append("Content-Disposition: inline; filename=\"");
	    buffer.append(htmlFilename);
	    buffer.append("\"\n\n");
	  }
	else
	  {
	    buffer.append("Content-Disposition: inline;\n\n");
	  }

	buffer.append(htmlBody);
	buffer.append("\n");
      }

    buffer.append("--");
    buffer.append(separator);
    buffer.append("--\n\n");

    sendmsg(from_address, to_addresses, subject, buffer.toString(), MIMEheaders);
  }

  /**
   * <P>Sends a mail message with some custom-specified envelope headers.  Used
   * internally by the other Qsmtp sendmsg and sendHTMLmsg methods.</P>
   *
   * @param from_address Who is sending this message?
   * @param to_addresses Vector of string addresses to send this message to
   * @param subject Subject for this message
   * @param message The text for the mail message
   * @param extraHeaders Vector of string headers to include in the message's
   * envelope
   *
   * @exception IOException
   */

  public synchronized void sendmsg(String from_address, Vector to_addresses, 
				   String subject, String message,
				   Vector extraHeaders) throws IOException
  {
    messageObject msgObj = new messageObject(from_address, to_addresses,
					     subject, message, extraHeaders);

    if (threaded)
      {
	synchronized (queuedMessages)
	  {
	    queuedMessages.addElement(msgObj);
	    queuedMessages.notify();
	  }
      }
    else
      {
	dispatchMessage(msgObj);
      }
  }

  /**
   * <P>Main worker routine for the background thread which handles mail-outs.</P>
   */

  public void run()
  {
    messageObject message = null;

    /* -- */

    if (debug)
      {
	System.err.println("Qsmtp: background thread starting");
      }

    try
      {
	while (threaded)
	  {
	    message = null;

	    synchronized (queuedMessages)
	      {
		if (queuedMessages.size() > 0)
		  {
		    message = (messageObject) queuedMessages.firstElement();
		    queuedMessages.removeElementAt(0);
		  }
		else
		  {
		    message = null;

		    try
		      {
			queuedMessages.wait(); // wait until something is queued
		      }
		    catch (InterruptedException ex)
		      {
			// ??
		      }

		    if (debug)
		      {
			System.err.println("Qsmtp: background thread woke up");
		      }
		  }
	      }

	    if (message != null)
	      {
		try
		  {
		    dispatchMessage(message);
		  }
		catch (IOException ex)
		  {
		    System.err.println("Qstmp: dispatch thread found error when sending mail:\n");
		    System.err.println(message.toString());
		    ex.printStackTrace();
		    System.err.println();
		  }
	      }
	  }
      }
    finally
      {
	try
	  {
	    // clear out any remaining messages

	    if (!threaded)
	      {
		if (debug)
		  {
		    System.err.println("Qsmtp: background thread stopping.. clearing mail queue");
		  }

		synchronized (queuedMessages)
		  {
		    while (queuedMessages.size() > 0)
		      {
			message = (messageObject) queuedMessages.firstElement();
			queuedMessages.removeElementAt(0);

			try
			  {
			    if (debug)
			      {
				System.err.println("Qsmtp: background thread sending mail");
			      }

			    dispatchMessage(message);
			  }
			catch (IOException ex)
			  {
			    System.err.println("Qstmp: dispatch thread found error when sending mail:\n");
			    System.err.println(message.toString());
			    ex.printStackTrace();
			    System.err.println();
			  }
		      }
		  }
	      }
	  }
	finally
	  {
	    this.backgroundThread = null;
	  }
      }

    if (debug)
      {
	System.err.println("Qsmtp: background thread finishing");
      }
  }

  /**
   * <P>This method handles the actual mail-out</P>
   *
   * @exception IOException
   */

  private void dispatchMessage(messageObject msgObj) throws IOException
  {
    String rstr;
    String sstr;

    InetAddress local;

    String from_address = msgObj.from_address;
    Vector to_addresses = msgObj.to_addresses;
    String subject = msgObj.subject;
    String message = msgObj.message;
    Vector extraHeaders = msgObj.extraHeaders;

    DataInputStream replyStream = null;
    BufferedReader reply = null;
    PrintWriter send = null;
    Socket sock = null;

    /* -- */

    try 
      {
	local = InetAddress.getLocalHost();
      }
    catch (UnknownHostException ioe) 
      {
	System.err.println("No local IP address found - is your network up?");
	throw ioe;
      }

    if (to_addresses == null ||
	to_addresses.size() == 0)
      {
	return;
      }

    // initialize connection to our SMTP mailer

    if (hostid != null)
      {
	sock = new Socket(hostid, port);
      }
    else
      {
	sock = new Socket(address, port);
      }


    try
      {
	replyStream = new DataInputStream(sock.getInputStream());
	reply = new BufferedReader(new InputStreamReader(replyStream));
	send = new PrintWriter(sock.getOutputStream(), true);

	rstr = reply.readLine();

	if (!rstr.startsWith("220")) 
	  {
	    throw new ProtocolException(rstr);
	  }

	while (rstr.indexOf('-') == 3) 
	  {
	    rstr = reply.readLine();

	    if (!rstr.startsWith("220")) 
	      {
		throw new ProtocolException(rstr);
	      }
	  }

	String host = local.getHostName();

	send.print("HELO " + host);
	send.print(EOL);
	send.flush();

	rstr = reply.readLine();
	if (!rstr.startsWith("250")) 
	  {
	    throw new ProtocolException(rstr);
	  }

	sstr = "MAIL FROM: " + from_address ;
	send.print(sstr);
	send.print(EOL);
	send.flush();

	rstr = reply.readLine();
	if (!rstr.startsWith("250")) 
	  {
	    throw new ProtocolException(rstr);
	  }

	boolean successRcpt = false;

	for (int i = 0; i < to_addresses.size(); i++)
	  {
	    sstr = "RCPT TO: " + (String) to_addresses.elementAt(i);
	    send.print(sstr);
	    send.print(EOL);
	    send.flush();

	    rstr = reply.readLine();

	    if (!rstr.startsWith("250")) 
	      {
		// don't throw an exception here.. we're in a loop and
		// we want to get the mail sent to others.

		System.err.println("Qsmtp.dispatchMessage(): " + rstr + " received for address " +
				   (String) to_addresses.elementAt(i));
	      }
	    else
	      {
		successRcpt = true;
	      }
	  }

	// if none of our addresses was accepted, just return.  Note
	// that our finally {} clause will clean up for us.

	if (!successRcpt)
	  {
	    return;
	  }

	send.print("DATA");
	send.print(EOL);
	send.flush();

	rstr = reply.readLine();
	if (!rstr.startsWith("354")) 
	  {
	    throw new ProtocolException(rstr);
	  }

	send.print("From: " + from_address);
	send.print(EOL);

	StringBuffer targetString = new StringBuffer();

	for (int i = 0; i < to_addresses.size(); i++)
	  {
	    if (i > 0)
	      {
		targetString.append(", ");
	      }

	    targetString.append((String) to_addresses.elementAt(i));
	  }

	send.print("To: " + targetString.toString());
	send.print(EOL);
	send.print("Subject: " + subject);
	send.print(EOL);
    
	// Create Date - we'll cheat by assuming that local clock is right
    
	Date today_date = new Date();
	send.print("Date: " + formatDate(today_date));
	send.print(EOL);
	send.flush();

	// Warn the world that we are on the loose - with the comments header:

	send.print("Comment: Unauthenticated sender");
	send.print(EOL);
	send.print("X-Mailer: JNet Qsmtp");
	send.print(EOL);

	if (extraHeaders != null)
	  {
	    String header;

	    for (int i = 0; i < extraHeaders.size(); i++)
	      {
		header = (String) extraHeaders.elementAt(i);
		send.print(header);
		send.print(EOL);
		send.flush();
	      }
	  }

	// Sending a blank line ends the header part.

	send.print(EOL);

	// Now send the message proper
	send.print(message);
	send.print(EOL);
	send.print(".");
	send.print(EOL);
	send.flush();
    
	rstr = reply.readLine();
	if (!rstr.startsWith("250")) 
	  {
	    throw new ProtocolException(rstr);
	  }

	// close our mailer connection

	send.print("QUIT");
	send.print(EOL);
	send.flush();
      }
    finally
      {
	try
	  {
	    if (replyStream != null)
	      {
		replyStream.close();
	      }
	  }
	catch (IOException ex)
	  {
				// shrug
	  }

	if (send != null)
	  {
	    send.close();
	  }

	try
	  {
	    sock.close();
	  }
	catch (IOException ex)
	  {
				// shrug
	  }
      }
  }

  /**
   * <p>This method returns a properly mail-formatted date string.</p>
   */

  public static String formatDate(Date date)
  {
    DateFormat formatter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", 
						java.util.Locale.US);
    return formatter.format(date);
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                                   messageObject

------------------------------------------------------------------------------*/

/**
 * <P>Data-holding object used by the Qstmp class to queue messages for mailing
 * on a separate thread.</P>
 */

class messageObject {
  
  String from_address;
  Vector to_addresses;
  String subject;
  String message;
  Vector extraHeaders;

  /* -- */

  messageObject(String from_address, Vector to_addresses, 
		String subject, String message,
		Vector extraHeaders)
  {
    this.from_address = from_address;
    this.to_addresses = to_addresses;
    this.subject = subject;
    this.message = message;
    this.extraHeaders = extraHeaders;
  }

  public String toString()
  {
    StringBuffer buffer = new StringBuffer();

    buffer.append("From: ");
    buffer.append(from_address);
    buffer.append("\n");

    buffer.append("To: ");

    if (to_addresses != null)
      {
	for (int i = 0; i < to_addresses.size(); i++)
	  {
	    if (i > 0)
	      {
		buffer.append(", ");
	      }

	    buffer.append(to_addresses.elementAt(i));
	  }
      }

    buffer.append("\nSubject: ");
    buffer.append(subject);
    buffer.append("\n");

    if (extraHeaders != null)
      {
	for (int i = 0; i < extraHeaders.size(); i++)
	  {
	    buffer.append(extraHeaders.elementAt(i));
	    buffer.append("\n");
	  }
      }

    buffer.append("Message:\n");
    buffer.append(message);

    return buffer.toString();
  }
}
