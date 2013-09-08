/*
   booleanSempahore.java

   Handy, simple synchronized flag class

   Created: 29 March 2001

   Module By: Jonathan Abbey

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2013
   The University of Texas at Austin

   Ganymede is a registered trademark of The University of Texas at Austin

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

package arlut.csd.Util;

/*------------------------------------------------------------------------------
                                                                           class
                                                                booleanSempahore

------------------------------------------------------------------------------*/

/**
 * <p>Simple, synchronized boolean flag class.</p>
 *
 * <p>This class is useful for providing a reliable boolean flag that
 * can be examined by separate threads without worry over funky memory
 * model behavior on multiprocessor systems, etc.</p>
 *
 * <p>This class is serializable only because the Ganymede {@link
 * arlut.csd.ganymede.common.scheduleHandle} class includes a
 * booleanSempahore for server-side use, and {@link
 * arlut.csd.ganymede.common.scheduleHandle} objects are themselves
 * serializable.  Obviously, we don't really care too much about fancy
 * multithreaded semantics when we are being serialized.</p>
 */

public class booleanSemaphore implements java.io.Serializable {

  static final long serialVersionUID = -3446577789138582378L;

  private boolean state;

  /* -- */

  public booleanSemaphore(boolean initialState)
  {
    this.state = initialState;
  }

  public synchronized boolean isSet()
  {
    return state;
  }

  /**
   * <p>Simple bidirectional test and set.</p>
   *
   * @return The value that the booleanSemaphore had
   * before set() was called.
   */

  public synchronized boolean set(boolean b)
  {
    boolean old;

    /* -- */

    old = state;
    state = b;

    this.notifyAll();

    return old;
  }

  /**
   * <p>Safe, simple method to wait until this boolean semaphore has
   * been cleared.  If the semaphore is already cleared at the time
   * this method is called, this method will return immediately.</p>
   *
   * <p>Note that this method will not time out.</p>
   */

  public synchronized void waitForCleared()
  {
    while (state)
      {
        try
          {
            wait();
          }
        catch (InterruptedException ex)
          {
          }
      }
  }

  /**
   * <p>Safe, simple method to wait until this boolean semaphore has
   * been cleared, or until at least millis milliseconds have passed.
   * If the semaphore is already cleared at the time this method is
   * called, this method will return immediately.</p>
   *
   * @return the state of the semaphore at the time the method
   * returns.. this will be false if the semaphore was cleared, or
   * true if the wait timed out before the semaphore was cleared
   */

  public synchronized boolean waitForCleared(long millis)
  {
    if (!state)
      {
        return state;
      }

    long waitTime = millis;
    long startTime = System.currentTimeMillis();
    long timeSoFar = 0;

    /* -- */

    while (state)
      {
        // we already know from above that we have to wait, so
        // we'll start the loop waiting

        try
          {
            wait(waitTime);
          }
        catch (InterruptedException ex)
          {
          }

        timeSoFar = System.currentTimeMillis() - startTime;

        if (timeSoFar > millis) // timed out
          {
            return state;
          }
        else
          {
            waitTime = millis - timeSoFar;
          }
      }

    return state;
  }

  /**
   * <p>Safe, simple method to wait until this boolean semaphore has
   * been set. If the semaphore is already set at the time this method
   * is called, this method will return immediately.</p>
   *
   * <p>Note that this method will not time out.</p>
   */

  public synchronized void waitForSet()
  {
    while (!state)
      {
        try
          {
            wait();
          }
        catch (InterruptedException ex)
          {
          }
      }
  }

  /**
   * <p>Safe, simple method to wait until this boolean semaphore has
   * been cleared, or until at least millis milliseconds have
   * passed. If the semaphore is already set at the time this method
   * is called, this method will return immediately.</p>
   *
   * @return the state of the semaphore at the time the method
   * returns.. this will be true if the semaphore was set, or false if
   * the wait timed out before the semaphore was set
   */

  public synchronized boolean waitForSet(long millis)
  {
    if (state)
      {
        return state;
      }

    long waitTime = millis;
    long startTime = System.currentTimeMillis();
    long timeSoFar = 0;

    /* -- */

    while (!state)
      {
        // we already know from above that we have to wait, so
        // we'll start the loop waiting

        try
          {
            wait(waitTime);
          }
        catch (InterruptedException ex)
          {
          }

        timeSoFar = System.currentTimeMillis() - startTime;

        if (timeSoFar > millis) // timed out
          {
            return state;
          }
        else
          {
            waitTime = millis - timeSoFar;
          }
      }

    return state;
  }
}
