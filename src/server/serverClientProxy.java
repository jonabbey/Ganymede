/*

   serverClientProxy.java

   serverClientProxy is a partial server-side Client proxy object
   which buffers async client updates, coalescing update events as
   needed to maximize server efficiency.  Each serverClientProxy
   object has a background thread which communicates with a remote
   Client in the background, allowing the Ganymede server's operations
   to be asynchronous with respect to Client messages.
   
   Created: 16 February 2000
   Release: $Name:  $
   Version: $Revision: 1.6 $
   Last Mod Date: $Date: 2003/03/11 20:27:45 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999, 2000, 2001, 2002
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
                                                                serverClientProxy

------------------------------------------------------------------------------*/

/**
 * <p>serverClientProxy is a partial server-side Client proxy object
 * which buffers async client updates, coalescing update events as
 * needed to maximize server efficiency.  Each serverClientProxy
 * object has a background thread which communicates with a remote
 * Client in the background, allowing the Ganymede server's operations
 * to be asynchronous with respect to Client messages.</p>
 *
 * @see arlut.csd.ganymede.clientEvent
 *
 * @version $Revision: 1.6 $ $Date: 2003/03/11 20:27:45 $
 * @author Jonathan Abbey, jonabbey@arlut.utexas.edu, ARL:UT */

public class serverClientProxy implements Runnable {

  /**
   * <p>Used to generate a unique name for our background thread.</p>
   */

  private static int classCounter = 0;

  /**
   * <p>Our remote reference to the Client</p>
   */

  private Client client;

  /**
   * <p>Our background communications thread, which is responsible for
   * calling the remote Client via RMI.</p>
   */

  private Thread commThread;

  /**
   * <p>Our queue of {@link arlut.csd.ganymede.clientEvent clientEvent} objects.</p>
   */

  private final clientEvent[] eventBuffer;

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

  private clientEvent lookUp[];

  /* -- */

  public serverClientProxy(Client client)
  {
    this.client = client;
    eventBuffer = new clientEvent[maxBufferSize];
    lookUp = new clientEvent[clientEvent.LAST - clientEvent.FIRST + 1];

    commThread = new Thread(this, "Ganymede Client Messaging Proxy Thread-" + classCounter++);
    commThread.start();
  }

  /**
   * <p>Returns true if we are successfully in communications with the
   * attached Client.</p>
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
   * <p>This method is used to submit a message to be sent to the
   * client.  If the serverClientProxy has already had its shutdown()
   * method called, sendMessage() will silently fail, and the message
   * will not be queued.</p>
   */

  public void sendMessage(int type, String message) throws RemoteException
  {
    if (done)
      {
	return;
      }

    addEvent(new clientEvent(clientEvent.SENDMESSAGE, new Integer(type), message));
  }

  /**
   * <p>The serverClientProxy's background thread's run() method.  This method
   * runs in the Client proxy thread to read events from this console's
   * serverClientProxy eventBuffer and send them down to the Client.</p>
   */

  public void run()
  {
    clientEvent event;

    /* -- */

    try
      {
	while (true)
	  {
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
		    return;	// but see finally, below
		  }

		event = dequeue();

		// clear the direct pointer to this event so that
		// changeStatus() and replaceEvent() will know that we
		// don't have an event of this kind in our buffer anymore.

		lookUp[event.method] = null;
	      }

	    try
	      {
		event.dispatch(client);

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
	// normally, we won't get here unless done is set, but if we
	// hit an exception or error above, we might get kicked down
	// here, so we'll go ahead and set it true.

	done = true;	

	// let's aid garbage collection

	synchronized (eventBuffer)
	  {
	    for (int i = 0; i < maxBufferSize; i++)
	      {
		eventBuffer[i] = null;
	      }
	  }

	// we may get a thread that missed the done check adding to
	// eventBuffer after the above, but that's not fatal.. it'll
	// just be something for the gc to handle

	client = null;
	lookUp = null;
	commThread = null;
	errorCondition = null;
      }
  }

  /**
   * <p>private helper method in serverClientProxy, used to add an event to
   * the proxy's event buffer.  If the buffer already contains an event
   * of the same type as newEvent, both events will be sent to the
   * Client, in chronological order.</p>
   */

  private void addEvent(clientEvent newEvent) throws RemoteException
  {
    if (done)
      {
	throw new RemoteException("serverClientProxy: console disconnected");
      }

    synchronized (eventBuffer)
      {
	if (ebSz >= maxBufferSize)
	  {
	    throwOverflow();
	  }

	enqueue(newEvent);
	
	eventBuffer.notify();	// wake up commThread
      }
  }

  /**
   * <p>private helper method in serverClientProxy, used to add an
   * event to the proxy's event buffer.  If the buffer already
   * contains an event of the same type as newEvent, the old event's
   * contents will be replaced with the new, and the remote client
   * will never be notified of the old event's contents.</p>
   */

  private void replaceEvent(clientEvent newEvent) throws RemoteException
  {
    if (done)
      {
	throw new RemoteException("serverClientProxy: console disconnected");
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

	if (ebSz >= maxBufferSize)
	  {
	    throwOverflow();
	  }

	enqueue(newEvent);

	// remember that we have an event of this type on our eventBuffer
	// for direct lookup by later replaceEvent calls.

	lookUp[newEvent.method] = newEvent;

	eventBuffer.notify();	// wake up commThread
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
    
    throw new RemoteException("serverClientProxy buffer overflow:" + buffer.toString());
  }

  /**
   * private enqueue method.
   */

  private void enqueue(clientEvent item)
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

  private clientEvent dequeue()
  {
    synchronized (eventBuffer)
      {
	clientEvent result = eventBuffer[dequeuePtr];
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

/*------------------------------------------------------------------------------
                                                                           class
                                                                     clientEvent

------------------------------------------------------------------------------*/

/**
 * <p>The client class is used on the Ganymede server by the
 * {@link arlut.csd.ganymede.serverClientProxy serverClientProxy} class, which
 * uses it to queue up async method calls to a remote client.</p>
 *
 * <p>clientEvent objects are never sent to a remote client. rather,
 * they are queued up in the Ganymede server by the serverClientProxy class so
 * that a background communications thread can read client events off of a queue
 * and make the appropriate RMI calls to an attached client.</p>
 */

class clientEvent {

  static final byte FIRST = 0;
  static final byte SENDMESSAGE = 1;
  static final byte LAST = 1;


  /* --- */

  /**
   * <p>Identifies what RMI call is going to need to be made to the
   * remote Client.</p>
   */

  byte method;

  /**
   * <p>First Generic RMI call parameter to be sent to the remote Client.</p>
   */

  Object param;

  /**
   * <p>Second Generic RMI call parameter to be sent to the remote
   * Client.  If an RMI call normally takes more than two
   * parameter, param2 should be a Vector which contains the
   * 2nd and subsequent parameters internally.</p> 
   */

  Object param2;

  /* -- */

  public clientEvent(byte method, Object param, Object param2)
  {
    if (method < FIRST || method > LAST)
      {
	throw new IllegalArgumentException("bad method code: " + method);
      }

    this.method = method;
    this.param = param;
    this.param2 = param2;
  }

  public String toString()
  {
    StringBuffer result = new StringBuffer();

    switch (method)
      {
      case SENDMESSAGE:
	result.append("sendMessage");
	break;
	
      default:
	result.append("??");
      }

    result.append("(");
    result.append(param);
    result.append(")");

    return result.toString();
  }

  public void dispatch(Client client) throws RemoteException
  {
    switch (method)
      {
      case SENDMESSAGE:
	client.sendMessage(((Integer) param).intValue(), (String) param2);
	break;
      }
  }
}
