/*

   serverAdminProxy.java

   serverAdminProxy is a server-side Admin object which buffers console
   status updates, coalescing update events as needed to maximize server
   efficiency.  Each serverAdminProxy object has a background thread which
   communicates with an admin console in the background, allowing the
   Ganymede server's operations to be asynchronous with respect to admin
   console updates.
   
   Created: 31 January 2000
   Release: $Name:  $
   Version: $Revision: 1.17 $
   Last Mod Date: $Date: 2001/02/08 22:52:14 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999, 2000, 2001
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
   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.

*/

package arlut.csd.ganymede;

import java.util.*;
import java.io.*;
import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.server.Unreferenced;

/*------------------------------------------------------------------------------
                                                                           class
                                                                serverAdminProxy

------------------------------------------------------------------------------*/

/**
 * <p>serverAdminProxy is a server-side Admin object which buffers console
 * status updates, coalescing update events as needed to maximize server
 * efficiency.  Each serverAdminProxy object has a background thread which
 * communicates with an admin console in the background, allowing the
 * Ganymede server's operations to be asynchronous with respect to admin
 * console updates.</p>
 *
 * @see arlut.csd.ganymede.adminEvent
 *
 * @version $Revision: 1.17 $ $Date: 2001/02/08 22:52:14 $
 * @author Jonathan Abbey, jonabbey@arlut.utexas.edu, ARL:UT
 */

public class serverAdminProxy implements Admin, Runnable {

  /**
   * <p>Our background communications thread, which is responsible for
   * calling the admin console via RMI.</p>
   */

  private Thread commThread;

  /**
   * <p>Our queue of {@link arlut.csd.ganymede.adminEvent adminEvent} objects.</p>
   */

  private Vector eventBuffer;

  /**
   * <p>How many events we'll queue up before deciding that the
   * admin console isn't responding.</p>
   */

  private int maxBufferSize = 15; // only 10 kinds of things, most of which we coalesce/replace

  /**
   * <p>Our remote reference to the admin console client</p>
   */

  private Admin remoteConsole;

  /**
   * <p>If true, we have been told to shut down, and our
   * background commThread will exit as soon as it can
   * clear its event queue.</p>.</p>
   */

  private boolean done = false;

  /**
   * <p>If our commThread receives a remote exception when communicating
   * with an admin console, this field will become non-null, and no more
   * communications will be attempted with that console.</p>
   */

  private String errorCondition;

  /**
   * <p>Handy direct look-up table for events in eventBuffer</p>
   */

  private adminEvent lookUp[];

  /* -- */

  public serverAdminProxy(Admin remoteConsole)
  {
    this.remoteConsole = remoteConsole;
    eventBuffer = new Vector();
    lookUp = new adminEvent[adminEvent.LAST - adminEvent.FIRST + 1];

    commThread = new Thread(this, "admin console proxy");
    commThread.start();
  }

  /**
   * <p>Returns true if we are successfully in communications with the
   * attached admin console.</p>
   */

  public boolean isAlive()
  {
    return !done;
  }

  /**
   * <p>This method shuts down the background thread.</p>
   */

  public void shutdown()
  {
    if (done)
      {
	return;
      }

    synchronized (eventBuffer)
      {
	this.done = true;
	eventBuffer.notifyAll(); // let the commThread drain and exit
      }
  }

  /**
   * <p>This method is called by the Ganymede server to obtain the username
   * given when the admin console was started.</p>
   */

  public String getName() throws RemoteException
  {
    return remoteConsole.getName();
  }

  /**
   * <p>This method is called by the Ganymede server to obtain the password
   * given when the admin console was started.</p>
   */

  public String getPassword() throws RemoteException
  {
    return remoteConsole.getPassword();
  }

  /**
   *
   * Callback: The server can tell us to disconnect if the server is 
   * going down.
   *
   */

  public void forceDisconnect(String reason) throws RemoteException
  {
    try
      {
	remoteConsole.forceDisconnect(reason);
      }
    catch (RemoteException ex)
      {
      }

    shutdown();
  }

  /**
   * <p>This method is called by the Ganymede server to set the server start
   * date in the admin console.</p>
   */

  public void setServerStart(Date date) throws RemoteException
  {
    replaceEvent(new adminEvent(adminEvent.SETSERVERSTART, date));
  }

  /**
   * <p>This method is called by the Ganymede server to set the last dump
   * date in the admin console.</p>
   */

  public void setLastDumpTime(Date date) throws RemoteException
  {
    replaceEvent(new adminEvent(adminEvent.SETLASTDUMPTIME, date));
  }

  /**
   * <p>This method is called by the Ganymede server to set the number of
   * transactions in the server's journal in the admin console.</p>
   */

  public void setTransactionsInJournal(int trans) throws RemoteException
  {
    replaceEvent(new adminEvent(adminEvent.SETTRANSACTIONS, new Integer(trans)));
  }

  /**
   * <p>This method is called by the Ganymede server to set the number of
   * objects checked out in the admin console.</p>
   */

  public void setObjectsCheckedOut(int objs) throws RemoteException
  {
    replaceEvent(new adminEvent(adminEvent.SETOBJSCHECKOUT, new Integer(objs)));
  }

  /**
   * <p>This method is called by the Ganymede server to set the number of
   * locks held in the admin console.</p>
   */

  public void setLocksHeld(int locks) throws RemoteException
  {
    replaceEvent(new adminEvent(adminEvent.SETLOCKSHELD, new Integer(locks)));
  }

  /**
   * <p>This method is called by the Ganymede server to update the memory
   * status display in the admin console.</p>
   */

  public void setMemoryState(long freeMem, long totalMem) throws RemoteException
  {
    long[] parmAry = new long[2];

    parmAry[0] = freeMem;
    parmAry[1] = totalMem;

    replaceEvent(new adminEvent(adminEvent.SETMEMORYSTATE, parmAry));
  }

  /**
   * <p>This method is called by the Ganymede server to update the
   * admin console's server state display.</p>
   */

  public void changeState(String state) throws RemoteException
  {
    replaceEvent(new adminEvent(adminEvent.CHANGESTATE, state));
  }

  /**
   * <p>This method is called by the Ganymede server to add to the
   * admin console's log display.</p>
   */

  public void changeStatus(String status) throws RemoteException
  {
    this.changeStatus(status, false);
  }

  /**
   * <p>This method is called by the Ganymede server to add to the
   * admin console's log display.</p>
   */

  public void changeStatus(String status, boolean timeLabelled) throws RemoteException
  {
    StringBuffer stampedLineBuffer;
    adminEvent newLogEvent;

    /* -- */

    if (done)
      {
	// we have to throw a remote exception, since that's what
	// the GanymedeAdmin code expects to receive as a signal
	// that an admin console needs to be dropped

	throw new RemoteException("serverAdminProxy: console disconnected");
      }

    stampedLineBuffer = new StringBuffer();

    if (!timeLabelled)
      {
	stampedLineBuffer.append((new Date()).toString());
	stampedLineBuffer.append(" ");
	stampedLineBuffer.append(status);
	stampedLineBuffer.append("\n");
      }
    else
      {
	stampedLineBuffer.append(status);
      }

    synchronized (eventBuffer)
      {
	// if we have another changeStatus event in the eventBuffer,
	// go ahead and append the new log entry directly to its
	// StringBuffer

	if (lookUp[adminEvent.CHANGESTATUS] != null)
	  {
	    // coalesce this append to the log message

	    StringBuffer buffer = (StringBuffer) lookUp[adminEvent.CHANGESTATUS].param;
	    buffer.append(stampedLineBuffer.toString());
	    return;
	  }

	// if we didn't find an event to append to, go ahead and add a
	// new CHANGESTATUS log update event to the eventBuffer

	newLogEvent = new adminEvent(adminEvent.CHANGESTATUS, stampedLineBuffer);

	// queue the log evennt

	addEvent(newLogEvent);

	// if we didn't get an exception on the addEvent call, save a
	// pointer to the newLogEvent so that later calls to
	// changeStatus can directly append more text until such time
	// as our commThread can send the log event to the console

	lookUp[adminEvent.CHANGESTATUS] = newLogEvent;
      }
  }

  /**
   * <p>This method is called by the Ganymede server to update the
   * number of admin consoles attached to the server.</p>
   */

  public void changeAdmins(String adminStatus) throws RemoteException
  {
    replaceEvent(new adminEvent(adminEvent.CHANGEADMINS, adminStatus));
  }

  /**
   * <p>This method is called by the Ganymede server to update the
   * admin console's connected user table.</p>
   *
   * @param entries a Vector of {@link arlut.csd.ganymede.AdminEntry AdminEntry}
   * login description objects.
   */

  public void changeUsers(Vector entries) throws RemoteException
  {
    replaceEvent(new adminEvent(adminEvent.CHANGEUSERS, entries));
  }

  /**
   * <p>This method is called by the Ganymede server to update the
   * admin console's task table.</p>
   *
   * @param tasks a Vector of {@link arlut.csd.ganymede.scheduleHandle scheduleHandle}
   * objects describing the tasks registered in the Ganymede server.
   */

  public void changeTasks(Vector tasks) throws RemoteException
  {
    replaceEvent(new adminEvent(adminEvent.CHANGETASKS, tasks));
  }

  /**
   * <p>The serverAdminProxy's background thread's run() method.  This method
   * runs in the admin console proxy thread to read events from this console's
   * serverAdminProxy eventBuffer and send them down to the admin console.</p>
   */

  public void run()
  {
    adminEvent event;

    /* -- */

    try
      {
	while (!done || (eventBuffer.size() != 0))
	  {
	    synchronized (eventBuffer)
	      {
		if (eventBuffer.size() == 0)
		  {
		    try
		      {
			eventBuffer.wait();
		      }
		    catch (InterruptedException ex)
		      {
		      }
		
		    continue;
		  }

		event = (adminEvent) eventBuffer.elementAt(0);
		eventBuffer.removeElementAt(0);

		// clear the direct pointer to this event so that
		// changeStatus() and replaceEvent() will know that we
		// don't have an event of this kind in our buffer anymore.

		lookUp[event.method] = null;
	      }

	    try
	      {
		event.dispatch(remoteConsole);

		// if we didn't get an exception above, clear the
		// errorCondition variable to indicate a successful RMI
		// call.

		errorCondition = null; 
	      }
	    catch (RemoteException ex)
	      {
		// if we get two RemoteExceptions in a row from
		// dispatch, throw in the towel, we're done.

		if (errorCondition != null)
		  {
		    return;	// but see finally, below
		  }
		else
		  {
		    errorCondition = Ganymede.stackTrace(ex);
		    System.err.println(errorCondition);
		  }
	      }
	  }
      }
    finally
      {
	done = true;	

	// now we need to aid garbage collection

	eventBuffer.setSize(0);

	// we may get a thread that missed the done check adding to
	// eventBuffer after the above, but that's not fatal.. it'll
	// just be something for the gc to handle

	remoteConsole = null;
	lookUp = null;
	commThread = null;
	errorCondition = null;
      }
  }

  /**
   * <p>private helper method in serverAdminProxy, used to add an event to
   * the proxy's event buffer.  If the buffer already contains an event
   * of the same type as newEvent, both events will be sent to the
   * admin console, in chronological order.</p>
   */

  private void addEvent(adminEvent newEvent) throws RemoteException
  {
    if (done)
      {
	// we have to throw a remote exception, since that's what
	// the GanymedeAdmin code expects to receive as a signal
	// that an admin console needs to be dropped

	throw new RemoteException("serverAdminProxy: console disconnected");
      }

    synchronized (eventBuffer)
      {
	if (eventBuffer.size() >= maxBufferSize)
	  {
	    throwOverflow();
	  }

	eventBuffer.addElement(newEvent);
	
	eventBuffer.notify();	// wake up commThread
      }
  }

  /**
   * <p>private helper method in serverAdminProxy, used to add an
   * event to the proxy's event buffer.  If the buffer already
   * contains an event of the same type as newEvent, the old event's
   * contents will be replaced with the new, and the admin console
   * will never be notified of the old event's contents.</p>
   */

  private void replaceEvent(adminEvent newEvent) throws RemoteException
  {
    if (done)
      {
	// we have to throw a remote exception, since that's what
	// the GanymedeAdmin code expects to receive as a signal
	// that an admin console needs to be dropped

	throw new RemoteException("serverAdminProxy: console disconnected");
      }

    synchronized (eventBuffer)
      {
	// if we have an instance of this event on our eventBuffer,
	// update its parameter with the new event's info.

	if (lookUp[newEvent.method] != null)
	  {
	    lookUp[newEvent.method].param = newEvent.param;
	    return;
	  }

	// okay, we don't have an event of matching type on our eventBuffer
	// queue.  Check for overflow and add the element ourselves.

	if (eventBuffer.size() >= maxBufferSize)
	  {
	    throwOverflow();
	  }

	eventBuffer.addElement(newEvent);

	// remember that we have an event of this type on our eventBuffer
	// for direct lookup by later replaceEvent calls.

	lookUp[newEvent.method] = newEvent;

	eventBuffer.notify();	// wake up commThread
      }
  }

  /**
   * <p>This method throws a remoteException which describes the state
   * of the event buffer.  This is called from addEvent and
   * replaceEvent.  The {@link arlut.csd.ganymede.GanymedeAdmin
   * GanymedeAdmin} code that calls the {@link
   * arlut.csd.ganymede.Admin Admin} proxy methods in serverAdminProxy
   * take repeated remote exceptions as an indication that they need
   * to detach the admin console, which is why we use RemoteException.</p>
   */

  private void throwOverflow() throws RemoteException
  {
    StringBuffer buffer = new StringBuffer();
    
    for (int i = 0; i < eventBuffer.size(); i++)
      {
	buffer.append(i);
	buffer.append(": ");
	buffer.append(eventBuffer.elementAt(i));
	buffer.append("\n");
      }
    
    throw new RemoteException("serverAdminProxy buffer overflow:" + buffer.toString());
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                                      adminEvent

------------------------------------------------------------------------------*/

/**
 * <p>The adminEvent class is used on the Ganymede server by the
 * {@link arlut.csd.ganymede.serverAdminProxy serverAdminProxy} class, which
 * uses it to queue up method calls to a remote admin console.</p>
 *
 * <p>adminEvent objects are never sent to a remote admin console. rather,
 * they are queued up in the Ganymede server by the serverAdminProxy class so
 * that a background communications thread can read adminEvents off of a queue
 * and make the appropriate RMI calls to an attached admin console.</p>
 */

class adminEvent {

  static final byte FIRST = 0;
  static final byte SETSERVERSTART = 0;
  static final byte SETLASTDUMPTIME = 1;
  static final byte SETTRANSACTIONS = 2;
  static final byte SETOBJSCHECKOUT = 3;
  static final byte SETLOCKSHELD = 4;
  static final byte CHANGESTATE = 5;
  static final byte CHANGESTATUS = 6;
  static final byte CHANGEADMINS = 7;
  static final byte CHANGEUSERS = 8;
  static final byte CHANGETASKS = 9;
  static final byte SETMEMORYSTATE = 10;
  static final byte LAST = 10;


  /* --- */

  /**
   * <p>Identifies what RMI call is going to need to be made to the
   * remote admin console.</p>
   */

  byte method;

  /**
   * <p>Generic RMI call parameter to be sent to the remote admin
   * console.  If an RMI call normally takes more than one parameter,
   * param should be some sort of composite object (Vector, Array,
   * object) which contains the parameters internally.</p>
   */

  Object param;

  /* -- */

  public adminEvent(byte method, Object param)
  {
    if (method < FIRST || method > LAST)
      {
	throw new IllegalArgumentException("bad method code: " + method);
      }

    this.method = method;
    this.param = param;
  }

  public String toString()
  {
    StringBuffer result = new StringBuffer();

    switch (method)
      {
      case SETSERVERSTART: 
	result.append("setServerStart");
	break;
	
      case SETLASTDUMPTIME:
	result.append("setLastDumpTime");
	break;

      case SETTRANSACTIONS:
	result.append("setTransactionsInJournal");
	break;

      case SETOBJSCHECKOUT:
	result.append("setObjectsCheckedOut");
	break;

      case SETLOCKSHELD:
	result.append("setLocksHeld");
	break;

      case CHANGESTATE:
	result.append("changeState");
	break;

      case CHANGESTATUS:
	result.append("changeStatus");
	break;

      case CHANGEADMINS:
	result.append("changeAdmins");
	break;

      case CHANGEUSERS:
	result.append("changeUsers");
	break;

      case CHANGETASKS:
	result.append("changeTasks");
	break;

      case SETMEMORYSTATE:
	result.append("setMemoryState");
	
      default:
	result.append("??");
      }

    result.append("(");
    result.append(param);
    result.append(")");

    return result.toString();
  }

  public void dispatch(Admin remoteConsole) throws RemoteException
  {
    switch (method)
      {
      case SETSERVERSTART:
	remoteConsole.setServerStart((Date) param);
	break;

      case SETLASTDUMPTIME:
	remoteConsole.setLastDumpTime((Date) param);
	break;

      case SETTRANSACTIONS:
	remoteConsole.setTransactionsInJournal(((Integer) param).intValue());
	break;

      case SETOBJSCHECKOUT:
	remoteConsole.setObjectsCheckedOut(((Integer) param).intValue());
	break;

      case SETLOCKSHELD:
	remoteConsole.setLocksHeld(((Integer) param).intValue());
	break;

      case CHANGESTATE:
	remoteConsole.changeState((String) param);
	break;

      case CHANGESTATUS:
	remoteConsole.changeStatus(((StringBuffer) param).toString());
	break;

      case CHANGEADMINS:
	remoteConsole.changeAdmins((String) param);
	break;

      case CHANGEUSERS:
	remoteConsole.changeUsers((Vector) param);
	break;

      case CHANGETASKS:
	remoteConsole.changeTasks((Vector) param);
	break;

      case SETMEMORYSTATE:
	long[] parms = (long[]) param;

	remoteConsole.setMemoryState(parms[0], parms[1]);
      }
  }
}
