/*

   serverAdminAsyncResponder.java

   serverAdminAsyncResponder is a partial server-side Admin proxy
   object which buffers async admin console updates, coalescing update
   events as needed to maximize server efficiency.  Consoles are
   responsible for calling the getNextMsgs() method on
   serverAdminAsyncResponder in a loop.  At each call from the console,
   the getNextMsgs() method will block until one or more async messages
   from the server are queued, whereupon getNextMsgs() will return them
   to the console.

   This makes it possible for the server to provide asynchronous
   notification to the admin consoles, even if the consoles are
   running behind a system-level firewall.
   
   Created: 4 September 2003
   Release: $Name:  $
   Version: $Revision: 1.2 $
   Last Mod Date: $Date: 2003/09/06 03:52:55 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999, 2000, 2001, 2002, 2003
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
   Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
   02111-1307, USA

*/

package arlut.csd.ganymede;

import java.util.*;
import java.io.*;
import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.server.Unreferenced;

/*------------------------------------------------------------------------------
                                                                           class
                                                       serverAdminAsyncResponder

------------------------------------------------------------------------------*/

/**
 * <p>serverAdminAsyncResponder is a partial server-side Client proxy object
 * which buffers async client updates, coalescing update events as
 * needed to maximize server efficiency.  The client is responsible for
 * calling the getNextMsg() method on serverAdminAsyncResponder
 * in a loop.  At each call from the client, the getNextMsg() method
 * will block until one or more async messages from the server are queued,
 * whereupon getNextMsg() will return them to the client.</p>
 *
 * <p>This makes it possible for the server to provide asynchronous
 * notification to the client, even if the client is running behind a
 * system-level firewall.</p>
 *
 * @see arlut.csd.ganymede.clientAsyncMessage
 *
 * @version $Revision: 1.2 $ $Date: 2003/09/06 03:52:55 $
 * @author Jonathan Abbey, jonabbey@arlut.utexas.edu, ARL:UT
 */

public class serverAdminAsyncResponder implements AdminAsyncResponder, Remote {

  /**
   * <p>Our queue of {@link arlut.csd.ganymede.adminAsyncMessage adminAsyncMessage} objects.</p>
   */

  private final adminAsyncMessage[] eventBuffer;

  /**
   * <p>Index pointer to the slot for the next item to be place in</p>
   */

  private int enqueuePtr = 0;

  /**
   * <p>Index pointer to the slot for the next item to be pulled from</p>
   */

  private int dequeuePtr = 0;

  /**
   * <p>Current counter for how many events we have queued</p>
   */

  private int ebSz = 0;

  /**
   * <p>How many events we'll queue up before deciding that the
   * Client isn't responding.</p>
   */

  private final int maxBufferSize = 15; // we shouldn't even need this many

  /**
   * <p>If true, we have been told to shut down, and our
   * background commThread will exit as soon as it can
   * clear its event queue.</p>.</p>
   */

  private volatile boolean done = false;

  /**
   * <p>If our commThread receives a remote exception when communicating
   * with a remote Client, this field will become non-null, and no more
   * communications will be attempted with that client.</p>
   */

  private String errorCondition;

  /**
   * <p>Handy direct look-up table for events in eventBuffer</p>
   */

  private adminAsyncMessage lookUp[];

  /* -- */

  public serverAdminAsyncResponder()
  {
    eventBuffer = new adminAsyncMessage[maxBufferSize];
    lookUp = new adminAsyncMessage[adminAsyncMessage.LAST - adminAsyncMessage.FIRST + 1];
  }

  /**
   * <p>Returns true if we are open for communications from the client.</p>
   */

  public boolean isAlive()
  {
    return !done;
  }

  /**
   * <p>This method is used to submit a message to be sent to the
   * client.  If the serverAdminAsyncResponder has already had its shutdown()
   * method called, sendMessage() will silently fail, and the message
   * will not be queued.</p>
   */

  public void sendMessage(int type, String message) throws RemoteException
  {
    if (done)
      {
	return;
      }

    Object params[] = new Object[2];
    params[0] = new Integer(type);
    params[1] = message;

    addEvent(new adminAsyncMessage(adminAsyncMessage.SENDMESSAGE, params));
  }

  /**
   * <p>This method is used to shutdown the responder, without sending
   * a message to the client so notifying it.</p>
   */

  public void shutdown()
  {
    if (this.done)
      {
	return;
      }

    synchronized (eventBuffer)
      {
	this.done = true;
	eventBuffer.notifyAll(); // let the client drain and exit
      }
  }

  /**
   * <p>This method is used to send a shutdown command to the client.
   * This method will also set done to true, causing the
   * serverAdminAsyncResponder to refuse any more event
   * submissions.</p>
   */

  public void shutdown(String message) throws RemoteException
  {
    if (done)
      {
	return;
      }

    addEvent(new adminAsyncMessage(adminAsyncMessage.SHUTDOWN, message));

    synchronized (eventBuffer)
      {
	this.done = true;
	eventBuffer.notifyAll(); // let the client drain and exit
      }
  }

  /**
   * <p>Blocks waiting for the next asynchronous message from the
   * server.  Returns null if isAlive() is false.</p>
   */

  public adminAsyncMessage getNextMsg()
  {
    adminAsyncMessage event;

    /* -- */

    synchronized (eventBuffer)
      {
	while (!done && ebSz == 0)
	  {
	    try
	      {
		eventBuffer.wait();
	      }
	    catch (InterruptedException ex)
	      {
	      }
	  }

	if (done && ebSz == 0)
	  {
	    return null;	// but see finally, below
	  }

	event = dequeue();

	// clear the direct pointer to this event so that
	// replaceEvent() will know that we don't have an event of
	// this kind in our buffer anymore.
	
	lookUp[event.method] = null;
      }

    return event;
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
    replaceEvent(adminAsyncMessage.SETSERVERSTART, date);
  }

  /**
   * <p>This method is called by the Ganymede server to set the last dump
   * date in the admin console.</p>
   */

  public void setLastDumpTime(Date date) throws RemoteException
  {
    replaceEvent(adminAsyncMessage.SETLASTDUMPTIME, date);
  }

  /**
   * <p>This method is called by the Ganymede server to set the number of
   * transactions in the server's journal in the admin console.</p>
   */

  public void setTransactionsInJournal(int trans) throws RemoteException
  {
    replaceEvent(adminAsyncMessage.SETTRANSACTIONS, new Integer(trans));
  }

  /**
   * <p>This method is called by the Ganymede server to set the number of
   * objects checked out in the admin console.</p>
   */

  public void setObjectsCheckedOut(int objs) throws RemoteException
  {
    replaceEvent(adminAsyncMessage.SETOBJSCHECKOUT, new Integer(objs));
  }

  /**
   * <p>This method is called by the Ganymede server to set the number of
   * locks held in the admin console.</p>
   */

  public void setLocksHeld(int locks) throws RemoteException
  {
    replaceEvent(adminAsyncMessage.SETLOCKSHELD, new Integer(locks));
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

    replaceEvent(adminAsyncMessage.SETMEMORYSTATE, parmAry);
  }

  /**
   * <p>This method is called by the Ganymede server to update the
   * admin console's server state display.</p>
   */

  public void changeState(String state) throws RemoteException
  {
    replaceEvent(adminAsyncMessage.CHANGESTATE, state);
  }

  /**
   * <p>This method is called by the Ganymede server to add to the
   * admin console's log display.</p>
   */

  public void changeStatus(String status) throws RemoteException
  {
    adminAsyncMessage newLogEvent;

    /* -- */

    if (done)
      {
	// we have to throw a remote exception, since that's what
	// the GanymedeAdmin code expects to receive as a signal
	// that an admin console needs to be dropped
	
	throw new RemoteException("serverAdminProxy: console disconnected");
      }

    synchronized (eventBuffer)
      {
	// if we have another changeStatus event in the eventBuffer,
	// go ahead and append the new log entry directly to its
	// StringBuffer

	if (lookUp[adminAsyncMessage.CHANGESTATUS] != null)
	  {
	    // coalesce this append to the log message

	    StringBuffer buffer = (StringBuffer) lookUp[adminAsyncMessage.CHANGESTATUS].param;
	    buffer.append(status);
	    return;
	  }

	// if we didn't find an event to append to, go ahead and add a
	// new CHANGESTATUS log update event to the eventBuffer

	newLogEvent = new adminAsyncMessage(adminAsyncMessage.CHANGESTATUS, new StringBuffer().append(status));

	// queue the log evennt

	addEvent(newLogEvent);

	// if we didn't get an exception on the addEvent call, save a
	// pointer to the newLogEvent so that later calls to
	// changeStatus can directly append more text until such time
	// as our commThread can send the log event to the console

	lookUp[adminAsyncMessage.CHANGESTATUS] = newLogEvent;
      }
  }

  /**
   * <p>This method is called by the Ganymede server to update the
   * number of admin consoles attached to the server.</p>
   */

  public void changeAdmins(String adminStatus) throws RemoteException
  {
    replaceEvent(adminAsyncMessage.CHANGEADMINS, adminStatus);
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
    replaceEvent(adminAsyncMessage.CHANGEUSERS, entries);
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
    replaceEvent(adminAsyncMessage.CHANGETASKS, tasks);
  }

  /**
   * <p>private helper method in serverAdminAsyncResponder, used to add an event to
   * the proxy's event buffer.  If the buffer already contains an event
   * of the same type as newEvent, both events will be sent to the
   * Client, in chronological order.</p>
   */

  private void addEvent(adminAsyncMessage newEvent) throws RemoteException
  {
    if (done)
      {
	throw new RemoteException("serverAdminAsyncResponder: console disconnected");
      }

    synchronized (eventBuffer)
      {
	if (ebSz >= maxBufferSize)
	  {
	    throwOverflow();
	  }
	
	enqueue(newEvent);
	
	eventBuffer.notify();	// wake up getNextMsg()
      }
  }

  /**
   * <p>private helper method in serverAdminAsyncResponder, used to add an
   * event to the proxy's event buffer.  If the buffer already
   * contains an event of the same type as newEvent, the old event's
   * contents will be replaced with the new, and the remote client
   * will never be notified of the old event's contents.</p>
   */

  private void replaceEvent(adminAsyncMessage newEvent) throws RemoteException
  {
    if (done)
      {
	throw new RemoteException("serverAdminAsyncResponder: console disconnected");
      }

    synchronized (eventBuffer)
      {
	// if we have an instance of this event on our eventBuffer,
	// update its parameter with the new event's info.

	if (lookUp[newEvent.getMethod()] != null)
	  {
	    lookUp[newEvent.getMethod()] = newEvent;
	    return;
	  }

	// okay, we don't have an event of matching type on our eventBuffer
	// queue.  Check for overflow and add the element ourselves.

	if (ebSz >= maxBufferSize)
	  {
	    throwOverflow();
	  }

	enqueue(newEvent);

	// remember that we have an event of this type on our eventBuffer
	// for direct lookup by later replaceEvent calls.

	lookUp[newEvent.getMethod()] = newEvent;

	eventBuffer.notify();	// wake up getNextMsg()
      }
  }

  /**
   * <p>This method throws a remoteException which describes the state
   * of the event buffer.  This is called from addEvent and
   * replaceEvent.</p>
   */

  private void throwOverflow() throws RemoteException
  {
    StringBuffer buffer = new StringBuffer();

    synchronized (eventBuffer)
      {
	int i = dequeuePtr;
	int count = 0;

	while (count < ebSz)
	  {
	    buffer.append(i);
	    buffer.append(": ");
	    buffer.append(eventBuffer[i]);
	    buffer.append("\n");

	    count++;
	    i++;

	    if (i >= maxBufferSize)
	      {
		i = 0;
	      }
	  }
      }
    
    throw new RemoteException("serverAdminAsyncResponder buffer overflow:" + buffer.toString());
  }

  /**
   * private enqueue method.
   */

  private void enqueue(adminAsyncMessage item)
  {
    synchronized (eventBuffer)
      {
	eventBuffer[enqueuePtr] = item;
	
	if (++enqueuePtr >= maxBufferSize)
	  {
	    enqueuePtr = 0;
	  }
	
	ebSz++;
      }
  }

  /**
   * private dequeue method.  assumes that the calling code will check
   * bounds.
   */

  private adminAsyncMessage dequeue()
  {
    synchronized (eventBuffer)
      {
	adminAsyncMessage result = eventBuffer[dequeuePtr];
	eventBuffer[dequeuePtr] = null;
	
	if (++dequeuePtr >= maxBufferSize)
	  {
	    dequeuePtr = 0;
	  }
	
	ebSz--;
	
	return result;
      }
  }
}
