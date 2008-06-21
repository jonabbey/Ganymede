/*

   JythonServer.java

   The JythonServer class is used to provide a tcp port on the
   Ganymede server which can be telnetted to in order to interact with
   a Python (Jython) console.
   
   Created: 19 July 2004
   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

   Module By: Deepak Giridharagopal, deepak@brownman.org

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996-2008
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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.RemoteException;
import java.util.Arrays;

import org.python.core.PyException;
import org.python.core.PySystemState;
import org.python.util.InteractiveConsole;

import arlut.csd.Util.TranslationService;

/*------------------------------------------------------------------------------
                                                                           class
                                                                    JythonServer

------------------------------------------------------------------------------*/

/**
 * The JythonServer class is used to provide a tcp port on the
 * Ganymede server which can be telnetted to in order to interact with
 * a Python (Jython) console.
 *
 * @author Deepak Giridharagopal <deepak@brownman.org>
 */

public class JythonServer extends Thread {

  /* The server's listener socket */ 
  private ServerSocket sock = null;
  
  /* boolean flag for indicating that we should stop listening for new 
   * connections */
  private boolean shutdownRequested = false;
  
  /* Lock to synchronize on when changing the shutdown flag */
  private Object lock = new Object();

  public JythonServer()
  {
    super("JythonServer");
  }
   
  public void run(int portNumber)
  {
    try
      {
        listen(portNumber);
      }
    catch (IOException ex)
      {
        ex.printStackTrace();
      }
  }

  private void listen(int portNumber) throws IOException 
  {
    Socket s;
    Thread t;
    
    PySystemState.initialize();
    
    try
      {
        sock = new ServerSocket(portNumber);
      }
    catch (IOException e)
      {
        System.err.println("Could not listen on port: 4444.");
        return;
      }

    while (true)
      {
        s = sock.accept();
        
        synchronized (lock)
          {
            if (shutdownRequested)
              {
                break;
              }
          }

        t = new JythonServerWorker(s, new JythonServerProtocol(s));
        t.start();
      }

    sock.close();
  }
  
  public void shutdown()
  {
    synchronized (lock) 
    {
      shutdownRequested = true;
    }
    
    /* Now close the ServerSocket */
    try
      {
        new Socket(sock.getInetAddress(), sock.getLocalPort()).close();
      }
    catch (IOException ex)
      {
        Ganymede.stackTrace(ex);
      }
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                            JythonServerProtocol

------------------------------------------------------------------------------*/
  
/**
 * Implementation of the server I/O protocol. Principally, it reads in lines
 * of input from the client and execs them inside a Jython interpreter.
 *
 * @author Deepak Giridharagopal <deepak@brownman.org>
 */

class JythonServerProtocol {

  /**
   * <p>TranslationService object for handling string localization in
   * the Ganymede server.</p>
   */

  private static TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.server.JythonServerProtocol");

  public static String doneString = null;

  /**
   * <p>The session associated with this telnet connection.</p>
   */
  public GanymedeSession session;
  
  private Socket socket;
  private InteractiveConsole interp;
  private StringWriter buffer;
  private String prompt = ">>> ";

  public JythonServerProtocol(Socket sock) 
  {
    JythonServerProtocol.doneString = ts.l("global.done");

    this.socket = sock;
    
    buffer = new StringWriter(64);
    
    interp = new InteractiveConsole();
    interp.setOut(buffer);
    interp.setErr(buffer);
    
    /* Import the additional Jython library routines */
    interp.exec("import sys");
    interp.exec("sys.path.append( sys.prefix + '" + System.getProperty("file.separator") + "' + 'jython-lib.jar' )");
    
    /* Seed the interpreter with a pointer to important Ganymede classes */
    interp.exec("from arlut.csd.ganymede.server import *");
    interp.exec("from arlut.csd.ganymede.common import *");
  }

  public void createSession(String personaName)
  {
    try
      {
      	/* Snag the appropriate Admin Persona from the database */
        DBObject persona = (DBObject) ((DBObjectBase) Ganymede.db.get("Admin Persona")).get(personaName);
        
        /* If there is a user associated with this persona, snag it */
        DBObject user = null;
        InvidDBField userField = (InvidDBField) persona.get("User");
        if (userField != null)
          {
            user = (DBObject) userField.getVal();
          }
        
        /* Now we have all we need to create the session */
        session = new GanymedeSession(personaName, user, persona, false, true);
      }
    catch (RemoteException ex)
      {
        Ganymede.stackTrace(ex);
        return;
      }

    interp.set("session", session);
  }
  
  public String processInput(String input)
  {
    String output;
    boolean moreInputRequired;
    
    if (input == null)
      {
	// '\nHello {0}\nWelcome to the Ganymede Jython interpreter!\n\nType "quit" to exit.\n{1}'
	return ts.l("processInput.greeting", socket.getInetAddress().getHostAddress(), prompt);
      }
    
    if (input.equals(ts.l("processInput.quitcommand")))
      {
        return doneString;
      }

    try
      {
        moreInputRequired = interp.push(input);
        if (moreInputRequired)
          {
            return "... ";
          }
        
        buffer.flush();
        output = buffer.toString();
        interp.resetbuffer();
        buffer.getBuffer().setLength(0);
      }
    catch (PyException pex) 
      {
      	output = buffer.toString() + "\n" + pex.toString();
      	interp.resetbuffer();
      	buffer.getBuffer().setLength(0);
      }
    
    return output + prompt;
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                              JythonServerWorker

------------------------------------------------------------------------------*/
  
/**
 * Handles passing input and output to the above Protocol class.
 *
 * @author Deepak Giridharagopal <deepak@brownman.org>
 */

class JythonServerWorker extends Thread {

  /**
   * <p>TranslationService object for handling string localization in
   * the Ganymede server.</p>
   */

  private static TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.server.JythonServerWorker");
  
  private Socket socket = null;
  private JythonServerProtocol protocol = null;

  public JythonServerWorker(Socket socket, JythonServerProtocol protocol) 
  {
    super("JythonServerWorker");
    this.socket = socket;
    this.protocol = protocol;
  }

  public void run()
  {
    try
      {
        OutputStream rawOutput = socket.getOutputStream();
        InputStream rawInput = socket.getInputStream();
        PrintWriter out = new PrintWriter(rawOutput, true);
        BufferedReader in = new BufferedReader(new InputStreamReader(rawInput));
      
        String inputLine, outputLine;
        
        /* Check to make sure that this telnet session is from localhost */
        InetAddress clientAddress = socket.getInetAddress();
        if (!clientAddress.equals(java.net.InetAddress.getByName(Ganymede.serverHostProperty))
            && !clientAddress.getHostAddress().equals("127.0.0.1"))
          {
            // Permission denied
            out.println(ts.l("run.denied_not_localhost"));
            out.flush();
            socket.close();
            return;
          }

        /* Check to see if logins are allowed */

        String error = Ganymede.server.lSemaphore.increment();

        if (error != null)
          {
            if (error.equals("shutdown"))
              {
                // "ERROR: The server is currently waiting to shut down.  No logins will be accepted until the server has restarted"
                out.print(ts.l("run.nologins_shutdown"));
              }
            else
              {
                // "ERROR: Can't log in to the Ganymede server.. semaphore disabled: {0}"
                out.print(ts.l("run.nologins_semaphore", error));
              }

            out.print("\n");
            out.flush();
            socket.close();

            return;
          }
	
        try
          {
	    // "Username"
            out.print(ts.l("run.username"));
	    out.print(": ");
            out.flush();
            String loginName = in.readLine();

            /* Telnet terminal codes */
            
            /* IAC WILL ECHO */
            byte[] echoOff = { (byte)255, (byte)251, (byte)1 };
            /* IAC DO ECHO */
            byte[] echoOffResponse = { (byte)255, (byte)253, (byte)1 };
            /* IAC WONT ECHO */
            byte[] echoOn  = { (byte)255, (byte)252, (byte)1 };
            /* IAC DONT ECHO */
            byte[] echoOnResponse = { (byte)255, (byte)254, (byte)1 };
            /* Holds the client response to each terminal code */
            byte[] responseBuffer = new byte[3];

	    // "Password" 
            out.print(ts.l("run.password"));
	    out.print(": ");
            out.flush();

            /* Disable client-side character echo while the user types in 
             * the password  */
            rawOutput.write(echoOff);
            rawOutput.flush();

            int chars_read = 0;

            while (chars_read < 3)
              {
                chars_read += rawInput.read(responseBuffer, chars_read, 3-chars_read);
              }

            if (!Arrays.equals(responseBuffer, echoOffResponse))
              {
              	out.print("Your telnet client won't properly suppress character echo.");
              	out.flush();
              	socket.close();
              	return;
              }

            String password = in.readLine();

            /* Now re-enable client-side character echo so we can conduct
             * business as usual */
            rawOutput.write(echoOn);
            rawOutput.flush();

            chars_read = 0;

            while (chars_read < 3)
              {
                chars_read += rawInput.read(responseBuffer, chars_read, 3-chars_read);
              }

            if (!Arrays.equals(responseBuffer, echoOnResponse))
              {
              	out.print("Your telnet client won't properly resume character echo.");
              	out.flush();
              	socket.close();
              	return;
              }

            /* Authenticate the user */
            int validationResult = Ganymede.server.validateAdminUser(loginName,
                                                                     password);

            /* A result of 3 means that this user has interpreter access
             * privileges. Anything else means that we give 'em the boot. */
            if (validationResult != 3)
              {
                try
                  {
                    Thread.currentThread().sleep(3000);
                  }
                catch (InterruptedException ex)
                  {
                    /* Move along */
                  }

		// "Permission denied."
                out.print(ts.l("run.denied"));
		out.print("\n");
                out.flush();
                socket.close();
                return;
              }
    
            /* Send the HELO */
            outputLine = protocol.processInput(null);
            out.print(outputLine);
            out.flush();

            /* Setup the interpreter session variable */
            protocol.createSession(loginName);

            /* Here is the read-eval-print loop */
            while ((inputLine = in.readLine()) != null)
              {
                outputLine = protocol.processInput(inputLine);
                out.print(outputLine);
                out.flush();
                if (outputLine.equals(JythonServerProtocol.doneString))
                  break;
              }
            out.close();
            in.close();
            socket.close();
          }
        finally
          {
            Ganymede.server.lSemaphore.decrement();

            /* Make sure to register the logout */

            try
              {
                protocol.session.logout();
              }
            catch (Exception e)
              {
                e.printStackTrace(); // Move along
              }
          }
      }
    catch (IOException e)
      {
        e.printStackTrace();
      }
  }
}
