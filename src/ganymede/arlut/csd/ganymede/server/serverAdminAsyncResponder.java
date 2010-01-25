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

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996-2010
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
   along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/

package arlut.csd.ganymede.server;

import java.rmi.RemoteException;
import java.util.Date;
import java.util.Vector;

import arlut.csd.ganymede.common.adminAsyncMessage;
import arlut.csd.ganymede.rmi.AdminAsyncResponder;

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
 * @see arlut.csd.ganymede.common.clientAsyncMessage
 *
 * @version $Id$
 * @author Jonathan Abbey, jonabbey@arlut.utexas.edu, ARL:UT
 */

public class serverAdminAsyncResponder implements AdminAsyncResponder {

  /**
   * <p>How many events we'll queue up before deciding that the
   * Client isn't responding.</p>
   */

  private static final int maxBufferSize = 25; // we shouldn't even need this many, since we replaceEvent

  // ---

  /**
   * <p>Our queue of {@link arlut.csd.ganymede.common.adminAsyncMessage adminAsyncMessage} objects.</p>
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

  private int lookUp[];

  /* -- */

  public serverAdminAsyncResponder() throws RemoteException
  {
    eventBuffer = new adminAsyncMessage[maxBufferSize];
    lookUp = new int[adminAsyncMessage.LAST - adminAsyncMessage.FIRST + 1];

    for (int i = 0; i < lookUp.length; i++)
      {
	lookUp[i] = -1;
      }

    Ganymede.rmi.publishObject(this);
  }

  /**
   * <p>Returns true if we are open for communications from the client.</p>
   */

  public boolean isAlive()
  {
    return !done;
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
   * <p>Blocks waiting for the next asynchronous message from the
   * server.  Returns null if isAlive() is false.</p>
   */

  public adminAsyncMessage[] getNextMsgs()
  {
    adminAsyncMessage event;
    Vector items = new Vector();

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

	while (ebSz > 0)
	  {
	    event = dequeue();
	    items.addElement(event);
	  }
      }

    adminAsyncMessage[] events = new adminAsyncMessage[items.size()];

    for (int i = 0; i < events.length; i++)
      {
	events[i] = (adminAsyncMessage) items.elementAt(i);
      }

    return events;
  }

  /**
   *
   * Callback: The server can tell us to disconnect if the server is 
   * going down.
   *
   */

  public void forceDisconnect(String reason) throws RemoteException
  {
    adminAsyncMessage message = new adminAsyncMessage(adminAsyncMessage.FORCEDISCONNECT, reason);
    addEvent(message);

    shutdown();
  }

  /**
   * <p>This method is called by the Ganymede server to set the server start
   * date in the admin console.</p>
   */

  public void setServerStart(Date date) throws RemoteException
  {
    adminAsyncMessage message = new adminAsyncMessage(adminAsyncMessage.SETSERVERSTART, date);
    replaceEvent(message);
  }

  /**
   * <p>This method is called by the Ganymede server to set the last dump
   * date in the admin console.</p>
   */

  public void setLastDumpTime(Date date) throws RemoteException
  {
    adminAsyncMessage message = new adminAsyncMessage(adminAsyncMessage.SETLASTDUMPTIME, date);
    replaceEvent(message);
  }

  /**
   * <p>This method is called by the Ganymede server to set the number of
   * transactions in the server's journal in the admin console.</p>
   */

  public void setTransactionsInJournal(int trans) throws RemoteException
  {
    adminAsyncMessage message = new adminAsyncMessage(adminAsyncMessage.SETTRANSACTIONS, trans);
    replaceEvent(message);
  }

  /**
   * <p>This method is called by the Ganymede server to set the number of
   * objects checked out in the admin console.</p>
   */

  public void setObjectsCheckedOut(int objs) throws RemoteException
  {
    adminAsyncMessage message = new adminAsyncMessage(adminAsyncMessage.SETOBJSCHECKOUT, objs);
    replaceEvent(message);
  }

  /**
   * <p>This method is called by the Ganymede server to set the number of
   * locks held in the admin console.</p>
   */

  public void setLocksHeld(int locks) throws RemoteException
  {
    adminAsyncMessage message = new adminAsyncMessage(adminAsyncMessage.SETLOCKSHELD, locks);
    replaceEvent(message);
  }

  /**
   * <p>This method is called by the Ganymede server to update the memory
   * status display in the admin console.</p>
   */

  public void setMemoryState(long freeMem, long totalMem) throws RemoteException
  {
    Object[] parmAry = new Object[2];

    parmAry[0] = Long.valueOf(freeMem);
    parmAry[1] = Long.valueOf(totalMem);

    adminAsyncMessage message = new adminAsyncMessage(adminAsyncMessage.SETMEMORYSTATE, parmAry);

    replaceEvent(message);
  }

  /**
   * <p>This method is called by the Ganymede server to update the
   * admin console's server state display.</p>
   */

  public void changeState(String state) throws RemoteException
  {
    adminAsyncMessage message = new adminAsyncMessage(adminAsyncMessage.CHANGESTATE, state);
    replaceEvent(message);
  }

  /**
   * <p>This method is called by the Ganymede server to add to the
   * admin console's log display.</p>
   */

  public void logAppend(String status) throws RemoteException
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
	// if we have another logAppend event in the eventBuffer,
	// go ahead and append the new log entry directly to its
	// StringBuffer

	if (lookUp[adminAsyncMessage.LOGAPPEND] != -1)
	  {
	    // coalesce this append to the log message

	    StringBuffer buffer = (StringBuffer) eventBuffer[lookUp[adminAsyncMessage.LOGAPPEND]].getParam(0);
	    buffer.append(status);
	    return;
	  }

	// if we didn't find an event to append to, go ahead and add a
	// new LOGAPPEND log update event to the eventBuffer

	newLogEvent = new adminAsyncMessage(adminAsyncMessage.LOGAPPEND, new StringBuffer().append(status));

	// queue the log event, keep a pointer to it in lookUp so we
	// can quickly find it next time

	lookUp[adminAsyncMessage.LOGAPPEND] = addEvent(newLogEvent);
      }
  }

  /**
   * <p>This method is called by the Ganymede server to update the
   * number of admin consoles attached to the server.</p>
   */

  public void changeAdmins(String adminStatus) throws RemoteException
  {
    adminAsyncMessage message = new adminAsyncMessage(adminAsyncMessage.CHANGEADMINS, adminStatus);
    replaceEvent(message);
  }

  /**
   * <p>This method is called by the Ganymede server to update the
   * admin console's connected user table.</p>
   *
   * @param entries a Vector of {@link arlut.csd.ganymede.common.AdminEntry AdminEntry}
   * login description objects.
   */

  public void changeUsers(Vector entries) throws RemoteException
  {
    Object params[] = new Object[entries.size()];
    entries.copyInto(params);

    replaceEvent(new adminAsyncMessage(adminAsyncMessage.CHANGEUSERS, params));
  }

  /**
   * <p>This method is called by the Ganymede server to update the
   * admin console's task table.</p>
   *
   * @param tasks a Vector of {@link arlut.csd.ganymede.common.scheduleHandle scheduleHandle}
   * objects describing the tasks registered in the Ganymede server.
   */

  public void changeTasks(Vector tasks) throws RemoteException
  {
    Object params[] = new Object[tasks.size()];
    tasks.copyInto(params);

    replaceEvent(new adminAsyncMessage(adminAsyncMessage.CHANGETASKS, params));
  }

  /**
   * <p>private helper method in serverAdminAsyncResponder, used to add an event to
   * the proxy's event buffer.  If the buffer already contains an event
   * of the same type as newEvent, both events will be sent to the
   * Client, in chronological order.</p>
   */

  private int addEvent(adminAsyncMessage newEvent) throws RemoteException
  {
    int result;

    /* -- */

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
	
	result = enqueue(newEvent);
	
	eventBuffer.notify();	// wake up getNextMsg()
      }

    return result;
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
	// if we have an instance of this event type on our
	// eventBuffer, replace it

	if (lookUp[newEvent.getMethod()] != -1)
	  {
	    eventBuffer[lookUp[newEvent.getMethod()]] = newEvent;
	    return;
	  }

	// okay, we don't have an event of matching type on our eventBuffer
	// queue.  Check for overflow and add the element ourselves.

	if (ebSz >= maxBufferSize)
	  {
	    throwOverflow();
	  }

	// remember that we have an event of this type on our eventBuffer
	// for direct lookup by later replaceEvent calls.

	lookUp[newEvent.getMethod()] = enqueue(newEvent);

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
   *
   * @return The index that the item was placed at in the eventBuffer
   */

  private int enqueue(adminAsyncMessage item)
  {
    int result;

    /* -- */

    synchronized (eventBuffer)
      {
	result = enqueuePtr;
	eventBuffer[enqueuePtr] = item;
	
	if (++enqueuePtr >= maxBufferSize)
	  {
	    enqueuePtr = 0;
	  }
	
	ebSz++;
      }

    return result;
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

	// if we're dequeueing something that we've been using
	// replaceEvent with, replace the dequeue'd event with the
	// latest of that type from the lookUp array.

	if (lookUp[result.getMethod()] != -1)
	  {
	    if (lookUp[result.getMethod()] != dequeuePtr)
	      {
		System.err.println("serverAdminAsyncResponder:dequeue() lookUp mismatch");
	      }

	    lookUp[result.getMethod()] = -1;
	  }
	
	if (++dequeuePtr >= maxBufferSize)
	  {
	    dequeuePtr = 0;
	  }
	
	ebSz--;
	return result;
      }
  }
}
