/*
   GASH 2

   loginSemaphore.java

   This class provides a handy counting semaphore used to arbitrate user
   login access to the Ganymede server.

   Created: 26 January 2000
   Release: $Name:  $
   Version: $Revision: 1.6 $
   Last Mod Date: $Date: 2002/01/26 19:36:33 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999, 2000
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

/*------------------------------------------------------------------------------
                                                                           class
                                                                  loginSemaphore

------------------------------------------------------------------------------*/

/**
 * <P>This class provides a handy counting semaphore used to arbitrate user
 * login access to the {@link arlut.csd.ganymede.GanymedeServer GanymedeServer}.
 * The server uses a single loginSemaphore to safely handle schema edits
 * and server shutdowns.</P>
 */

public final class loginSemaphore {

  static final boolean debug = false;

  /**
   * How many users are logged in on this semaphore?
   */
  
  private int count = 0;

  /**
   * If this var is not null, we are disabled.  The String here
   * will hold the reason why.
   */

  private String disableMsg = null;

  /* -- */

  public loginSemaphore()
  {
  }

  /**
   * <p>disables logins</p>
   *
   * <p>This method turns off user logins in Ganymede.  A piece of code in the
   * Ganymede server can call disable() on this object to signal that no further
   * logins or schema edits should be allowed.</p>
   *
   * @param message An explanation of why logins are being disabled.
   *
   * @param waitForZero If true, disable may block until the count of users
   * logged in goes to zero.  If false, disable will disable further increments,
   * but the disable call itself will not block until this time
   *
   * @param millis If waitForZero is true, tells us about our blocking behavior..
   * if millis &lt; 0, we will block as long as necessary.  if millis = 0, we will
   * not block.  if millis &gt; 0, we will block no more than that number of milliseconds.
   *
   * @return returns null if the disable was successful, or else a descriptive string
   * if the disable couldn't be carried out
   */

  public synchronized String disable(String message, boolean waitForZero, long millis) throws InterruptedException
  {
    if (message == null)
      {
	throw new IllegalArgumentException("loginSemaphore error: disable message must != null");
      }

    if (!waitForZero)
      {
	if (disableMsg != null)
	  {
	    return "Logins already disabled: " + disableMsg;
	  }
	else
	  {
	    disableMsg = message;
	    return null;
	  }
      }

    if (count == 0 && disableMsg == null)
      {
	disableMsg = message;
	return null;
      }
    else if (millis == 0)
      {
	if (count != 0)
	  {
	    return "Login count not zero: " + count;
	  }
	else			// disableMsg != null
	  {
	    return "Logins already disabled: " + disableMsg;
	  }
      }
    else if (millis < 0)	// block indefinitely
      {
	while (true)
	  {
	    // we already know from above that we have to wait, so
	    // we'll start the loop waiting

	    wait();

	    if (count == 0 && disableMsg == null)
	      {
		disableMsg = message;
		return null;
	      }
	  }
      }
    else			// don't block more than millis
      {
	long waitTime = millis;
	long startTime = System.currentTimeMillis();
	long timeSoFar = 0;
	    
	/* -- */

	while (true)
	  {
	    // we already know from above that we have to wait, so
	    // we'll start the loop waiting

	    wait(waitTime);

	    if (count == 0 && disableMsg == null)
	      {
		disableMsg = message;
		return null;
	      }
		
	    timeSoFar = System.currentTimeMillis() - startTime;

	    if (timeSoFar > millis)	// timed out
	      {
		return "Timeout";
	      }
	    else
	      {
		waitTime = millis - timeSoFar;
	      }
	  }
      }
  }

  /**
   * <p>re-enables logins.</p>
   *
   * @param message Should be identical to the message used to disable
   * logins, to verify that the right code is doing the re-enabling.
   *
   * @exception java.lang.IllegalStateException throws an IllegalStateException
   * if the message parameter did not match that used to disable the semaphore
   */

  public synchronized void enable(String message)
  {
    if (message == null)
      {
	throw new IllegalArgumentException("loginSemaphore error: enable message must != null");
      }

    if (message.equals(disableMsg))
      {
	disableMsg = null;
	notifyAll();		// wake up incrementers
      }
    else
      {
	throw new IllegalStateException(disableMsg);
      }
  }

  /**
   * <p>Gated enabled test.  If this method returns null, logins are allowed
   * at the time checkEnabled() is called.  This method is to be used by admin
   * consoles, which should not connect to the server during schema editing or
   * server shut down, but which should not affect the login count for reasons
   * of blocking a schema edit disable, say.</p>
   *
   * @return null if logins are currently enabled, or a message string if they
   * are disabled.
   */

  public synchronized String checkEnabled()
  {
    return disableMsg;
  }

  /**
   * <p>Returns a count of the number of users logged in on this semaphore</p>
   */

  public int getCount()
  {
    return count;
  }

  /**
   * <p>Attempt to increment the login count</p>
   *
   * @param millis Controls blocking behavior on this call.. if millis &lt; 0,
   * we will block forever until the semaphore is re-enabled.  if millis == 0,
   * no blocking will be peformed.  if millis &gt; 0, we will not block for longer
   * than that number of milliseconds.
   * 
   * @return An explanation of why the increment could not be carried out, or null
   * if the increment was successful.
   */

  public synchronized String increment(long millis) throws InterruptedException
  {
    try
      {
	if (disableMsg == null)
	  {
	    count++;
	    return null;
	  }

	if (millis == 0)	// don't block.. just fail
	  {
	    return disableMsg;
	  }
	else if (millis < 0)	// block indefinitely
	  {
	    while (true)
	      {
		// we already know from above that we have to wait, so
		// we'll start the loop waiting

		wait();	// can throw InterruptedException

		if (disableMsg == null)
		  {
		    count++;
		    return null;
		  }
	      }
	  }
	else			// block a limited time
	  {
	    long waitTime = millis;
	    long startTime = System.currentTimeMillis();
	    long timeSoFar = 0;
	    
	    /* -- */

	    while (true)
	      {
		// we already know from above that we have to wait, so
		// we'll start the loop waiting

		wait(waitTime);	// can throw InterruptedException

		if (disableMsg == null)
		  {
		    count++;
		    return null;
		  }

		timeSoFar = System.currentTimeMillis() - startTime;

		if (timeSoFar > millis)	// timed out
		  {
		    return disableMsg;
		  }
		else
		  {
		    waitTime = millis - timeSoFar;
		  }
	      }
	  }
      }
    finally
      {
	if (debug)
	  {
	    // get a stack trace for the increment

	    try
	      {
		throw new RuntimeException("semaphore increment");
	      }
	    catch (RuntimeException ex)
	      {
		ex.printStackTrace();
	      }
	  }
	
      }
  }

  /**
   * <p>Decrement the login count.</p>
   *
   * <p>This can be done when the semaphore is enabled or disabled, but if
   * the semaphore count is already zero, an IllegalStateException will
   * be thrown.</p>
   */

  public synchronized void decrement()
  {
    if (count == 0)
      {
	throw new IllegalStateException("Error, decrement called on empty loginSemaphore");
      }

    try
      {
	count--;
	notifyAll();		// wake up disablers
      }
    finally
      {
	if (debug)
	  {
	    // get a stack trace for the increment

	    try
	      {
		throw new RuntimeException("semaphore decrement");
	      }
	    catch (RuntimeException ex)
	      {
		ex.printStackTrace();
	      }
	  }
	
      }
  }
}
