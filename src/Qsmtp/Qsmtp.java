/*********************************************************************

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

There are a few things still to do to get this to work properly -

1)  fix the TZ stuff - getTimezoneOffset returns wierd values.
    For now, I'm going to let it be and hope that the bug that returns one
    miniute less than valid gets fixed in the class itself.  The next best
    solution is for this is to write a wrapper class that fixes the Date class.
    Not a good solution if you want it to be a lightweight applet machine.

***********************************************************************/

import java.net.*;
import java.io.*;
import java.util.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                           Qsmtp

------------------------------------------------------------------------------*/

public class Qsmtp {

  static final int DEFAULT_PORT = 25;
  static final boolean DEBUG = false;

  // --
  
  protected DataInputStream replyStream = null;
  protected BufferedReader reply = null;
  protected PrintWriter send = null;
  protected Socket sock = null;

  private String hostid = null;
  private InetAddress address = null;
  private int port = DEFAULT_PORT;

  /* -- */
  
  public Qsmtp( String hostid)
  {
    this.hostid = hostid;
  }

  public Qsmtp( String hostid, int port)
  {
    this.hostid = hostid;
    this.port = port;
  }

  public Qsmtp( InetAddress address )
  {
    this.address = address;
  }

  public Qsmtp( InetAddress address, int port )
  {
    this.address = address;
    this.port = port;
  }

  public void sendmsg( String from_address, Vector to_addresses,
		       String subject, String message) throws IOException, ProtocolException 
  {
    sendmsg(from_address, to_addresses, subject, message, null);
  }

  public void sendmsg( String from_address, Vector to_addresses,
		       String subject, String message, Vector extraHeaders ) throws IOException, ProtocolException 
  {
    String sstr;

    InetAddress local;

    /* -- */

    if (to_addresses == null ||
	to_addresses.size() == 0)
      {
	return;
      }

    // initialize connection to our SMTP mailer

    if (hostid != null)
      {
	sock = new Socket( hostid, port );
      }
    else
      {
	sock = new Socket( address, port );
      }
    replyStream = new DataInputStream( sock.getInputStream() );
    reply = new BufferedReader(new InputStreamReader(replyStream));
    send = new PrintWriter( sock.getOutputStream(), true );

    String rstr = reply.readLine();

    if (DEBUG) System.out.println(rstr);

    if (!rstr.startsWith("220"))
      {
	throw new ProtocolException(rstr);
      }

    while (rstr.indexOf('-') == 3) 
      {
	rstr = reply.readLine();

	if (DEBUG) System.out.println(rstr);

	if (!rstr.startsWith("220")) 
	  {
	    throw new ProtocolException(rstr);
	  }
      }

    // ok, socket initialized

    if (DEBUG) System.out.println("Socket initialized");

    // prepare to send mail

    local = InetAddress.getLocalHost();
    String host = local.getHostName();
    send.println("HELO " + host);

    //        send.println("HELO smtp");

    if (DEBUG) System.out.println("HELO " + host);

    rstr = reply.readLine();

    if (DEBUG) System.out.println("** " + rstr);

    if (!rstr.startsWith("250")) 
      {
	throw new ProtocolException(rstr);
      }

    sstr = "MAIL FROM: " + from_address ;
    send.println(sstr);

    if (DEBUG) System.out.println(sstr);

    rstr = reply.readLine();

    if (DEBUG) System.out.println(rstr);

    if (!rstr.startsWith("250")) 
      {
	throw new ProtocolException(rstr);
      }

    for (int i = 0; i < to_addresses.size(); i++)
      {
	sstr = "RCPT TO: " + (String) to_addresses.elementAt(i);
	send.println(sstr);
	if (DEBUG) System.out.println(sstr);

	rstr = reply.readLine();

	if (DEBUG) System.out.println(rstr);

	if (!rstr.startsWith("250")) 
	  {
	    throw new ProtocolException(rstr);
	  }
      }

    send.println("DATA");

    if (DEBUG) System.out.println("DATA");

    rstr = reply.readLine();

    if (DEBUG) System.out.println(rstr);

    if (!rstr.startsWith("354")) 
      {
	throw new ProtocolException(rstr);
      }

    send.println("From: " + from_address);

    if (DEBUG) System.out.println("From: " + from_address);

    StringBuffer targetString = new StringBuffer();

    for (int i = 0; i < to_addresses.size(); i++)
      {
	if (i > 0)
	  {
	    targetString.append(", ");
	  }

	targetString.append((String) to_addresses.elementAt(i));
      }

    send.println("To: " + targetString.toString());

    if (DEBUG) System.out.println("To: " + targetString.toString());

    send.println("Subject: " + subject);

    if (DEBUG) System.out.println("Subject: " + subject);
    
    // Create Date - we'll cheat by assuming that local clock is right
    
    Date today_date = new Date();

    send.println("Date: " + today_date.toGMTString());

    if (DEBUG) System.out.println("Date: " + today_date.toGMTString());

    // send.println("Date: " + msgDateFormat(today_date));

    // if (DEBUG) System.out.println("Date: " + msgDateFormat(today_date));
    
    // Warn the world that we are on the loose - with the comments header:

    //    send.println("Comment: Unauthenticated sender");
    send.println("X-Mailer: JNet Qsmtp");

    if (extraHeaders != null)
      {
	String header;

	for (int i = 0; i < extraHeaders.size(); i++)
	  {
	    header = (String) extraHeaders.elementAt(i);
	    send.println(header);
	  }
      }
    
    // Sending a blank line ends the header part.
    send.println("");

    if (DEBUG) System.out.println("");

    // Now send the message proper
    send.println(message);

    if (DEBUG) System.out.println(message);

    send.println(".");

    if (DEBUG) System.out.println(".");

    rstr = reply.readLine();

    if (DEBUG) System.out.println(rstr);

    if (!rstr.startsWith("250")) 
      {
	throw new ProtocolException(rstr);
      }

    // close our mailer con

    send.println("QUIT");
    sock.close();
  }

  /**
   *
   * In a perfect world, we'd do a generic MIME-capable mail system here.
   *
   */

  public void sendHTMLmsg( String from_address, Vector to_addresses,
			   String subject, String htmlBody, String htmlFilename,
			   String textBody ) throws IOException, ProtocolException 
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

  private String msgDateFormat( Date senddate) 
  {
    Calendar cal = Calendar.getInstance();
    String formatted = "hold";
    String Day[] = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
    String Month[] = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

    /* -- */

    cal.setTime(senddate);

    formatted = Day[cal.get(Calendar.DAY_OF_WEEK)] + ", ";
    formatted = formatted + String.valueOf(cal.get(Calendar.DATE)) + " ";
    formatted = formatted + Month[cal.get(Calendar.MONTH)] + " ";

    if (cal.get(Calendar.YEAR) > 99)
      {
	formatted = formatted + String.valueOf(cal.get(Calendar.YEAR) + 1900) + " ";
      }
    else
      {
	formatted = formatted + String.valueOf(cal.get(Calendar.YEAR)) + " ";
      }

    if (cal.get(Calendar.HOUR) < 10)
      {
	formatted = formatted + "0";
      }

    formatted = formatted + String.valueOf(cal.get(Calendar.HOUR)) + ":";

    if (cal.get(Calendar.MINUTE) < 10) 
      {
	formatted = formatted + "0";
      }

    formatted = formatted + String.valueOf(cal.get(Calendar.MINUTE)) + ":";

    if (cal.get(Calendar.SECOND) < 10) 
      {
	formatted = formatted + "0";
      }

    formatted = formatted + String.valueOf(cal.get(Calendar.SECOND)) + " ";

    return formatted;
  }
}
