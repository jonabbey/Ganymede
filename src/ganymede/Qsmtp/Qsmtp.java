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
sendmsg() method, rather than having to do a separate close() and
recreate a new Qsmtp object to send an additional message.

Modified the sendmsg() to_address parameter to support a vector of
addresses.

Added the sendHTMLmsg() method to allow for sending MIME-attached
html pages.

Added the extraHeaders parameter to sendmsg() to support sendHTMLmsg().

Modified the code to use the 1.1 io and text formatting classes.

Added javadocs (9 June 1999).

***********************************************************************/

package Qsmtp;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ProtocolException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

/*------------------------------------------------------------------------------
                                                                           class
                                                                           Qsmtp

------------------------------------------------------------------------------*/

/**
 * <p>SMTP mailer class, used to send email messages (with optional HTML MIME
 * attachments) through direct TCP/IP communication with Internet SMTP mail
 * servers.</p>
 *
 * <p>The Qsmtp constructors take an address for a SMTP mail server, and all
 * messages subsequently sent out by the Qsmtp object are handled by
 * that SMTP server.</p>
 *
 * <p>Once created, a Qsmtp object can be used to send any number of messages
 * through that mail server.  Each call to
 * {@link Qsmtp#sendmsg(java.lang.String, java.util.List, java.lang.String,
 * java.lang.String) sendmsg} or
 * {@link Qsmtp#sendHTMLmsg(java.lang.String, java.util.List, java.lang.String,
 * java.lang.String, java.lang.String, java.lang.String) sendHTMLmsg} opens a
 * separate SMTP connection to the designated mail server and transmits a
 * single message.</p>
 *
 * <p>Because this class opens a socket to a potentially remote TCP/IP server,
 * this class may not function properly when used within an applet.</p>
 */

public class Qsmtp implements Runnable {

  static final boolean debug = false;
  static final int DEFAULT_PORT = 25;
  static final String EOL = "\r\n"; // network end of line
  static final public int messageTimeout = 15000;  // 15 seconds
  static final private Random randomizer = new Random();

  /**
   * This method returns a properly mail-formatted date string.
   */

  static public String formatDate(Date date)
  {
    DateFormat formatter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z",
                                                java.util.Locale.US);
    return formatter.format(date);
  }

  // ---

  private String hostid = null;
  private InetAddress address = null;
  private int port = DEFAULT_PORT;

  private List<messageObject> queuedMessages = new ArrayList<messageObject>();
  private volatile boolean threaded = false;
  private volatile Thread backgroundThread;

  /* -- */

  public Qsmtp(String hostid)
  {
    this(hostid, DEFAULT_PORT);
  }

  public Qsmtp(InetAddress address)
  {
    this(address, DEFAULT_PORT);
  }

  public Qsmtp(String hostid, int port)
  {
    this.hostid = hostid;
    this.port = port;
  }

  public Qsmtp(InetAddress address, int port)
  {
    this.address = address;
    this.port = port;
  }

  /**
   * <p>After this method is called, all further sendmsg() calls will
   * not directly send mail themselves, but will rather queue the mail
   * for sending by a back-ground thread.</p>
   *
   * <p>One result of this is that after this is called, the sendmsg()
   * methods will never throw Protcol or IO Exceptions, and no
   * success/failure results will be returned.</p>
   *
   * <p>If this method is called while an previous background thread
   * that was ordered to stop by stopThreaded() is still shutting
   * down, this method will block until the old background thread
   * dies and the new background thread can be established.</p>
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

    backgroundThread = new Thread(this, "Ganymede Mail Thread");
    this.threaded = true;
    backgroundThread.start();
  }

  /**
   * <p>Calling this method turns off the background thread and
   * returns Qsmtp to normal blocking operation.</p>
   */

  public synchronized void stopThreaded()
  {
    if (debug)
      {
        System.err.println("Qsmtp.stopThreaded()");
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
            System.err.println("Qsmtp.stopThreaded() - waking background thread");
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
                System.err.println("Qsmtp.stopThreaded() - waiting for background thread to die");
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
            finally
              {
                if (debug)
                  {
                    System.err.println("Qsmtp.stopThreaded() - background thread completed");
                  }
              }
          }
        catch (InterruptedException ex)
          {
            return;             // oh, well.
          }
      }
  }

  /**
   * This method is called to shutdown this mailer object.  Once it is
   * shut down, no more mail can be sent through it.
   */

  public synchronized void close()
  {
    if (backgroundThread != null)
      {
        // shut down the background thread.. we might block here

        this.stopThreaded();
        backgroundThread = null;
      }
  }

  /**
   * <p>Sends a plain ASCII mail message</p>
   *
   * @param from_address Who is sending this message?
   * @param to_addresses List of string addresses to send this message to
   * @param subject Subject for this message
   * @param message The text for the mail message
   *
   * @return True if the message was successfully sent to the
   * mailhost, false otherwise.
   */

  public synchronized boolean sendmsg(String from_address, List<String> to_addresses,
                                      String subject, String message) throws IOException
  {
    return sendmsg(from_address, to_addresses, from_address, subject, message, null);
  }

  /**
   * <p>Sends a plain ASCII mail message</p>
   *
   * @param from_address Who is sending this message?
   * @param to_addresses List of string addresses to send this message to
   * @param from_address_desc A more elaborate version of the from address, with optional leading <Description> section
   * @param subject Subject for this message
   * @param message The text for the mail message
   *
   * @return True if the message was successfully sent to the
   * mailhost, false otherwise.
   */

  public synchronized boolean sendmsg(String from_address, List<String> to_addresses,
                                      String from_address_desc,
                                      String subject, String message) throws IOException
  {
    return sendmsg(from_address, to_addresses, from_address_desc, subject, message, null);
  }

  /**
   * <p>Sends a message with a MIME-attached HTML message</p>
   *
   * <p>In a perfect world, we'd do a generic MIME-capable mail system here, but
   * as it is, we only support HTML.</p>
   *
   * @param from_address Who is sending this message?
   * @param to_addresses List of string addresses to send this message to
   * @param subject Subject for this message
   * @param htmlBody A string containing the HTML document to be sent
   * @param htmlFilename The name to label the HTML document with, will
   * show up in mail clients
   * @param textBody The text for the non-HTML part of the mail message
   *
   * @return True if the message was successfully sent to the
   * mailhost, false otherwise.
   */

  public synchronized boolean sendHTMLmsg(String from_address, List<String> to_addresses,
                                          String subject, String htmlBody, String htmlFilename,
                                          String textBody) throws IOException
  {
    return this.sendHTMLmsg(from_address, to_addresses, from_address, subject, htmlBody, htmlFilename, textBody);
  }

  /**
   * <p>Sends a message with a MIME-attached HTML message</p>
   *
   * <p>In a perfect world, we'd do a generic MIME-capable mail system here, but
   * as it is, we only support HTML.</p>
   *
   * @param from_address Who is sending this message?
   * @param to_addresses List of string addresses to send this message to
   * @param from_address_desc A more elaborate version of the from address, with optional leading <Description> section
   * @param subject Subject for this message
   * @param htmlBody A string containing the HTML document to be sent
   * @param htmlFilename The name to label the HTML document with, will
   * show up in mail clients
   * @param textBody The text for the non-HTML part of the mail message
   *
   * @return True if the message was successfully sent to the
   * mailhost, false otherwise.
   */

  public synchronized boolean sendHTMLmsg(String from_address, List<String> to_addresses,
                                          String from_address_desc,
                                          String subject, String htmlBody, String htmlFilename,
                                          String textBody) throws IOException
  {
    List<String> MIMEheaders = new ArrayList<String>();
    String separator = generateRandomBoundary();
    StringBuilder buffer = new StringBuilder();

    /* -- */

    MIMEheaders.add("MIME-Version: 1.0");
    MIMEheaders.add("Content-Type: multipart/mixed; boundary=\"" + separator + "\"");

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

    return sendmsg(from_address, to_addresses, from_address_desc, subject, buffer.toString(), MIMEheaders);
  }

  /**
   * Convenience method to return a unique MIME separator for a given
   * HTML attachment message
   */

  private String generateRandomBoundary()
  {
    return Integer.toHexString(randomizer.nextInt()) + "MIMEISNEAT" + Integer.toHexString(randomizer.nextInt());
  }

  /**
   * <p>Sends a mail message with some custom-specified envelope headers.  Used
   * internally by the other Qsmtp sendmsg and sendHTMLmsg methods.</p>
   *
   * @param from_address Who is sending this message?
   * @param to_addresses List of string addresses to send this message to
   * @param subject Subject for this message
   * @param message The text for the mail message
   * @param extraHeaders List of string headers to include in the message's
   * envelope
   *
   * @return True if the message was successfully sent to the
   * mailhost, false otherwise.
   */

  public synchronized boolean sendmsg(String from_address,
                                      List<String> to_addresses,
                                      String subject, String message,
                                      List<String> extraHeaders)
  {
    return this.sendmsg(from_address, to_addresses, from_address, subject, message, extraHeaders);
  }

  /**
   * <p>Sends a mail message with some custom-specified envelope headers.  Used
   * internally by the other Qsmtp sendmsg and sendHTMLmsg methods.</p>
   *
   * @param from_address Who is sending this message?
   * @param to_addresses List of string addresses to send this message to
   * @param from_address_desc A more elaborate version of the from address, with optional leading <Description> section
   * @param subject Subject for this message
   * @param message The text for the mail message
   * @param extraHeaders List of string headers to include in the message's
   * envelope
   *
   * @return True if the message was successfully sent to the
   * mailhost, false otherwise.
   */

  public synchronized boolean sendmsg(String from_address,
                                      List<String> to_addresses,
                                      String from_address_desc,
                                      String subject, String message,
                                      List<String> extraHeaders)
  {
    messageObject msgObj = new messageObject(from_address, to_addresses, from_address_desc,
                                             subject, message, extraHeaders);

    if (threaded)
      {
        synchronized (queuedMessages)
          {
            queuedMessages.add(msgObj);
            queuedMessages.notify();
          }

        return true;
      }
    else
      {
        return dispatchMessage(msgObj);
      }
  }

  /**
   * <p>Main worker routine for the background thread which handles mail-outs.</p>
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
                    message = queuedMessages.remove(0);
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
                int count = 0;

                while (threaded && !dispatchMessage(message))
                  {
                    long delay = count++ * 1000;

                    System.err.println("Qsmtp mailer thread: failure sending message");
                    System.err.println("Will try to send the message again in " + count + " seconds.");

                    try
                      {
                        Thread.currentThread().sleep(delay);
                      }
                    catch (InterruptedException ex)
                      {
                        // no-op
                      }

                    System.err.println("Retrying mail transmission.. internal mail queue has " + queuedMessages.size() + " elements.");
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
                        message = queuedMessages.remove(0);

                        if (debug)
                          {
                            System.err.println("Qsmtp: background thread sending mail");
                          }

                        dispatchMessage(message);  // if it fails, it fails.. we still need to shut down.
                      }
                  }
              }
          }
        finally
          {
            this.backgroundThread = null;
            this.threaded = false;
          }
      }

    System.err.println("Qsmtp: background thread finishing");
  }

  /**
   * <p>This method handles the actual mail-out</p>
   *
   * @return false if any problems occurred during transmission which
   * should necessitate a retry of the message transmission, true
   * otherwise.
   */

  private boolean dispatchMessage(messageObject msgObj)
  {
    String rstr;
    String sstr;

    InetAddress local;

    String from_address = msgObj.from_address;
    List<String> to_addresses = msgObj.to_addresses;
    String from_address_desc = msgObj.from_address_desc;
    String subject = msgObj.subject;
    String message = msgObj.message;
    List<String> extraHeaders = msgObj.extraHeaders;

    DataInputStream replyStream = null;
    BufferedReader reply = null;
    PrintWriter send = null;

    Socket sock = null;

    /* -- */

    if (to_addresses == null ||
        to_addresses.size() == 0)
      {
        return true;            // we can't do anything here, no need to retry
      }

    try
      {
        local = InetAddress.getLocalHost();
      }
    catch (UnknownHostException ioe)
      {
        System.err.println("No local IP address found - is your network up?");
        ioe.printStackTrace();  // get it into our log

        return true;        // trying again won't help this message
      }

    try
      {
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
            sock.setSoTimeout(Qsmtp.messageTimeout);

            replyStream = new DataInputStream(sock.getInputStream());
            reply = new BufferedReader(new InputStreamReader(replyStream));
            send = new PrintWriter(sock.getOutputStream(), true);

            rstr = scanLine(reply);

            if (!rstr.startsWith("220"))
              {
                throw new ProtocolException(rstr);
              }

            while (rstr.indexOf('-') == 3)
              {
                rstr = scanLine(reply);

                if (!rstr.startsWith("220"))
                  {
                    throw new ProtocolException(rstr);
                  }
              }

            String host = local.getHostName();

            send.print("HELO " + host);
            send.print(EOL);
            send.flush();

            rstr = scanLine(reply);

            if (!rstr.startsWith("250"))
              {
                throw new ProtocolException(rstr);
              }

            sstr = "MAIL FROM: " + from_address ;
            send.print(sstr);
            send.print(EOL);
            send.flush();

            rstr = scanLine(reply);

            if (!rstr.startsWith("250"))
              {
                throw new ProtocolException(rstr);
              }

            boolean successRcpt = false;

            for (String address: to_addresses)
              {
                sstr = "RCPT TO: " + address;
                send.print(sstr);
                send.print(EOL);
                send.flush();

                rstr = scanLine(reply);

                if (!rstr.startsWith("250"))
                  {
                    // don't throw an exception here.. we're in a loop and
                    // we want to get the mail sent to others.

                    System.err.println("Qsmtp.dispatchMessage(): " + rstr + " received for address " + address);
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
                System.err.println("Qsmtp: dispatchMessage() couldn't find acceptable recipients for message:\n");
                System.err.println(msgObj.toString());

                return true;    // no sense trying again, really
              }

            send.print("DATA");
            send.print(EOL);
            send.flush();

            rstr = scanLine(reply);

            if (!rstr.startsWith("354"))
              {
                throw new ProtocolException(rstr);
              }

            send.print("From: " + from_address_desc);
            send.print(EOL);

            StringBuilder targetString = new StringBuilder();

            for (int i = 0; i < to_addresses.size(); i++)
              {
                if (i > 0)
                  {
                    targetString.append(", ");
                  }

                targetString.append(to_addresses.get(i));
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
                for (String header: extraHeaders)
                  {
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

            rstr = scanLine(reply);

            try
              {
                if (rstr.startsWith("4"))
                  {
                    System.err.println("Qsmtp: dispatchMessage found transient error result " + rstr + " when sending mail:\n");
                    System.err.println(msgObj.toString());
                    System.err.println();

                    return false;   // transient failure, will retry
                  }

                if (rstr.startsWith("5"))
                  {
                    System.err.println("Qsmtp: dispatchMessage found permanent error result " + rstr + " when sending mail:\n");
                    System.err.println(msgObj.toString());
                    System.err.println("Qsmtp: will not retry message transmission");
                    System.err.println();

                    return true;    // permanent failure, will not retry
                  }

                if (!rstr.startsWith("2"))
                  {
                    throw new ProtocolException(rstr);
                  }
              }
            finally
              {
                // close our mailer connection

                try
                  {
                    send.print("QUIT");
                    send.print(EOL);
                    send.flush();
                  }
                catch (Throwable ioex)
                  {
                    // we'll want to rethrow the original exception,
                    // not one that happened here
                  }
              }
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
    catch (Throwable ex)
      {
        System.err.println("Qsmtp: dispatchMessage found error when sending mail:\n");
        System.err.println(msgObj.toString());
        ex.printStackTrace();
        System.err.println();

        return false;        // don't propagate up any further, though
      }

    return true;
  }

  private String scanLine(BufferedReader reply) throws IOException, ProtocolException
  {
    String line = reply.readLine();

    if (line == null)
      {
        throw new ProtocolException("SMTP connection closed abruptly");
      }

    return line;
  }

}

/*------------------------------------------------------------------------------
                                                                           class
                                                                   messageObject

------------------------------------------------------------------------------*/

/**
 * <p>Data-holding object used by the Qsmtp class to queue messages for mailing
 * on a separate thread.</p>
 */

class messageObject {

  String from_address;
  List<String> to_addresses;
  String from_address_desc;
  String subject;
  String message;
  List<String> extraHeaders;

  /* -- */

  messageObject(String from_address, List<String> to_addresses,
                String from_address_desc,
                String subject, String message,
                List<String> extraHeaders)
  {
    this.from_address = from_address;
    this.to_addresses = to_addresses;
    this.from_address_desc = from_address_desc;
    this.subject = subject;
    this.message = message;
    this.extraHeaders = extraHeaders;
  }

  public String toString()
  {
    StringBuilder buffer = new StringBuilder();

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

            buffer.append(to_addresses.get(i));
          }
      }

    buffer.append("\nSubject: ");
    buffer.append(subject);
    buffer.append("\n");

    if (extraHeaders != null)
      {
        for (String header: extraHeaders)
          {
            buffer.append(header);
            buffer.append("\n");
          }
      }

    buffer.append("Message:\n");
    buffer.append(message);

    return buffer.toString();
  }
}
