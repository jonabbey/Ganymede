/*
 * Created on Jul 19, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package arlut.csd.ddroid.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.ServerSocket;
import java.net.Socket;

import org.python.core.PyException;
import org.python.core.PySystemState;
import org.python.util.PythonInterpreter;

/**
 * @author Deepak Giridharagopal <deepak@arlut.utexas.edu>
 *
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
   
  public void run()
  {
    try
      {
        listen();
      }
    catch (IOException ex)
      {
        ex.printStackTrace();
      }
  }

  private void listen() throws IOException 
  {
    Socket s;
    Thread t;
    
    PySystemState.initialize();
    
    try
      {
        sock = new ServerSocket(4444);
      }
    catch (IOException e)
      {
        System.err.println("Could not listen on port: 4444.");
        System.exit(-1);
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


  
/**
 * @author Deepak Giridharagopal <deepak@arlut.utexas.edu>
 *
 * Implementation of the server I/O protocol. Principally, it reads in lines
 * of input from the client and execs them inside a Jython interpreter.
 */
class JythonServerProtocol {
  private Socket socket;
  private PythonInterpreter interp;
  private StringWriter buffer;
  private String prompt = ">>> ";

  public JythonServerProtocol(Socket sock) 
  {
    this.socket = sock;
    
    buffer = new StringWriter(64);
    
    interp = new PythonInterpreter(null, new PySystemState());
    interp.setOut(buffer);
    interp.setErr(buffer);
    
    /* Import the additional Jython library routines */
    interp.exec("import sys");
    interp.exec("sys.path.append( sys.prefix + '" + System.getProperty("file.separator") + "' + 'jython-lib.jar' )");
    
    /* Seed the interpreter with a pointer to our DBStore */
    interp.set("db", Ganymede.db);
  }
  
  public String processInput(String input)
  {
    String output;
    
    if (input == null)
      {
        return "\nHello " + socket.getInetAddress().toString() + "\n" + 
               "Welcome to the Directory Droid Jython interpreter!\n\n" +
               "Type \"quit\" to exit.\n" + prompt;
      }
    
    if (input.equals("quit"))
      {
        return "Goodbye.";
      }
      
    try
      {
        interp.exec(input);
        buffer.flush();
        output = buffer.toString();
        buffer.getBuffer().setLength(0);
      }
    catch (PyException pex) 
      {
        output = pex.toString();
      }
    
    return output + prompt;
  }
  
}



/**
 * @author deepak
 *
 * Handles passing input and output to the above Protocol class.
 */
class JythonServerWorker extends Thread {
  
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
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(
            socket.getInputStream()));

        String inputLine, outputLine;

        /* Send the HELO */
        outputLine = protocol.processInput(null);
        out.print(outputLine);
        out.flush();

        /* Here is the read-eval-print loop */
        while ((inputLine = in.readLine()) != null)
          {
            outputLine = protocol.processInput(inputLine);
            out.print(outputLine);
            out.flush();
            if (outputLine.equals("Goodbye."))
              break;
          }
        out.close();
        in.close();
        socket.close();
      }
    catch (IOException e)
      {
        e.printStackTrace();
      }
  }
}