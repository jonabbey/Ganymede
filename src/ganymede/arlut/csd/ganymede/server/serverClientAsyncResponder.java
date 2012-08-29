/*

   serverClientAsyncResponder.java

   serverClientAsyncResponder is a partial server-side Client proxy object
   which buffers async client updates, coalescing update events as
   needed to maximize server efficiency.  The client is responsible for
   calling the getNextMsg() method on serverClientAsyncResponder
   in a loop.  At each call from the client, the getNextMsg() method
   will block until one or more async messages from the server are queued,
   whereupon getNextMsg() will return them to the client.

   This makes it possible for the server to provide asynchronous
   notification to the client, even if the client is running behind a
   system-level firewall.
   
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

import arlut.csd.ganymede.common.clientAsyncMessage;
import arlut.csd.ganymede.rmi.ClientAsyncResponder;

/*------------------------------------------------------------------------------
                                                                           class
                                                      serverClientAsyncResponder

------------------------------------------------------------------------------*/

/**
 * <p>serverClientAsyncResponder is a partial server-side Client proxy object
 * which buffers async client updates, coalescing update events as
 * needed to maximize server efficiency.  The client is responsible for
 * calling the getNextMsg() method on serverClientAsyncResponder
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
 * @author Jonathan Abbey, jonabbey@arlut.utexas.edu, ARL:UT
 */

public class serverClientAsyncResponder implements ClientAsyncResponder {

  /**
   * <p>How many events we'll queue up before deciding that the
   * Client isn't responding.</p>
   */

  private static final int maxBufferSize = 15; // we shouldn't even need this many

  // ---

  /**
   * <p>Our queue of {@link arlut.csd.ganymede.common.clientAsyncMessage clientAsyncMessage} objects.</p>
   */

  private final clientAsyncMessage[] eventBuffer;

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

  private clientAsyncMessage lookUp[];

  /* -- */

  public serverClientAsyncResponder() throws RemoteException
  {
    eventBuffer = new clientAsyncMessage[maxBufferSize];
    lookUp = new clientAsyncMessage[clientAsyncMessage.LAST - clientAsyncMessage.FIRST + 1];

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
   * <p>This method is used to submit a message to be sent to the
   * client.  If the serverClientAsyncResponder has already had its shutdown()
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
    params[0] = Integer.valueOf(type);
    params[1] = message;

    addEvent(new clientAsyncMessage(clientAsyncMessage.SENDMESSAGE, params));
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
   * serverClientAsyncResponder to refuse any more event
   * submissions.</p>
   */

  public void shutdown(String message) throws RemoteException
  {
    if (done)
      {
        return;
      }

    addEvent(new clientAsyncMessage(clientAsyncMessage.SHUTDOWN, message));

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

  public clientAsyncMessage getNextMsg()
  {
    clientAsyncMessage event;

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
            return null;        // but see finally, below
          }

        event = dequeue();

        // clear the direct pointer to this event so that
        // replaceEvent() will know that we don't have an event of
        // this kind in our buffer anymore.
        
        lookUp[event.getMethod()] = null;
      }

    return event;
  }

  /**
   * <p>private helper method in serverClientAsyncResponder, used to add an event to
   * the proxy's event buffer.  If the buffer already contains an event
   * of the same type as newEvent, both events will be sent to the
   * Client, in chronological order.</p>
   */

  private void addEvent(clientAsyncMessage newEvent) throws RemoteException
  {
    if (done)
      {
        throw new RemoteException("serverClientAsyncResponder: console disconnected");
      }

    synchronized (eventBuffer)
      {
        if (ebSz >= maxBufferSize)
          {
            throwOverflow();
          }
        
        enqueue(newEvent);
        
        eventBuffer.notify();   // wake up getNextMsg()
      }
  }

  /**
   * <p>private helper method in serverClientAsyncResponder, used to add an
   * event to the proxy's event buffer.  If the buffer already
   * contains an event of the same type as newEvent, the old event's
   * contents will be replaced with the new, and the remote client
   * will never be notified of the old event's contents.</p>
   */

  private void replaceEvent(clientAsyncMessage newEvent) throws RemoteException
  {
    if (done)
      {
        throw new RemoteException("serverClientAsyncResponder: console disconnected");
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

        eventBuffer.notify();   // wake up getNextMsg()
      }
  }

  /**
   * <p>This method throws a remoteException which describes the state
   * of the event buffer.  This is called from addEvent and
   * replaceEvent.</p>
   */

  private void throwOverflow() throws RemoteException
  {
    StringBuilder buffer = new StringBuilder();

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
    
    throw new RemoteException("serverClientAsyncResponder buffer overflow:" + buffer.toString());
  }

  /**
   * private enqueue method.
   */

  private void enqueue(clientAsyncMessage item)
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

  private clientAsyncMessage dequeue()
  {
    synchronized (eventBuffer)
      {
        clientAsyncMessage result = eventBuffer[dequeuePtr];
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
