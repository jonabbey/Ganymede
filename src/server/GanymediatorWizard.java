/*

   GanymediatorWizard.java

   This class is a template for all Wizards implemented on the server.
   Custom plug-in GanymediatorWizards may be authored to handle
   step-by-step interactions with a user.  GanymediatorWizards are
   typically created on the server when a client attempts an operation
   that requires a bit of hand-holding on the part of the server.  A
   remote handle to the newly instantiated GanymediatorWizard is returned to
   the client, which presents a matching dialog to the user, retrieves input,
   and calls the respond() method.  The respond() method takes the input from
   the user and considers whether it has enough information to perform the
   initially requested action.  If not, it will update its internal state
   to keep track of where it is with respect to the user, and will return
   another ReturnVal which requests the client present another dialog and
   call back this GanymediatorWizard to continue along the process.
   
   Created: 29 January 1998
   Release: $Name:  $
   Version: $Revision: 1.15 $
   Last Mod Date: $Date: 2001/06/01 01:35:01 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999  The University of Texas at Austin.

   Contact information

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

import java.lang.reflect.*;
import java.rmi.*;
import java.io.*;
import java.rmi.server.*;
import java.util.Hashtable;
import java.util.Enumeration;

import arlut.csd.JDialog.JDialogBuff;

/*------------------------------------------------------------------------------
                                                                           class
                                                              GanymediatorWizard

------------------------------------------------------------------------------*/

/** 
 * <p>This class is a template for all Wizards implemented on the
 * server.  Custom plug-in GanymediatorWizards may be authored to
 * handle step-by-step interactions with a user.  GanymediatorWizards
 * are typically created on the server when a client attempts an
 * operation that requires a bit of hand-holding on the part of the
 * server.  A remote handle to the newly instantiated
 * GanymediatorWizard is returned to the client, which presents a
 * matching dialog to the user, retrieves input, and calls the {@link
 * arlut.csd.ganymede.GanymediatorWizard#respond(java.util.Hashtable)
 * respond()} method.  The respond() method takes the input from the
 * user and considers whether it has enough information to perform the
 * initially requested action.  If not, it will update its internal
 * state to keep track of where it is with respect to the user, and
 * will return another {@link arlut.csd.ganymede.ReturnVal ReturnVal}
 * which requests the client present another dialog and call back this
 * GanymediatorWizard to continue along the process.</p>
 *
 * <p>After a GanymediatorWizard is constructed, the respond() method
 * will first call processDialog0().  processDialog0() can generate a
 * return value using the {@link
 * arlut.csd.ganymede.GanymediatorWizard#continueOn(java.lang.String,
 * java.lang.String, java.lang.String, java.lang.String,
 * java.lang.String) continueOn()} method to return a dialog that will
 * ask the user for more information, or it can use the {@link
 * arlut.csd.ganymede.GanymediatorWizard#fail(java.lang.String,
 * java.lang.String, java.lang.String, java.lang.String,
 * java.lang.String) fail()} or {@link
 * arlut.csd.ganymede.GanymediatorWizard#success(java.lang.String,
 * java.lang.String, java.lang.String, java.lang.String,
 * java.lang.String) success()} to return a dialog with an encoded
 * success or failure indication, or it can return null to indicate
 * success with no dialog.  If the return result from processDialog0()
 * was generated using continueOn(), the client's response to that
 * dialog will be forwarded to processDialog1().</p>
 *
 * <p>For processDialog1() and after, the wizard can use the
 * {@link arlut.csd.ganymede.GanymediatorWizard#getKeys() getKeys()}
 * method to get an enumeration of labeled values passed back from
 * the client, and {@link arlut.csd.ganymede.GanymediatorWizard#getParam(java.lang.Object) getParam()}
 * to get the value for a specific key.  Based on values passed back from
 * the client, processDialog1() can decide to continue the interaction,
 * or to return a dialog indicating success or failure, or a silent
 * success result.</p>
 *
 * <p>Typically, a continueOn() will cause the wizard system to
 * proceed to the next highest processDialogXX() method.  If a wizard
 * needs to skip to a specific step, it can use the {@link
 * arlut.csd.ganymede.GanymediatorWizard#setNextState(int)
 * setNextState()} method to set the number for the next processDialog
 * method before returning a continueOn() result.</p>
 *
 * <p>If at any time after processDialog0() the user hits cancel on
 * a dialog, the GanymediatorWizard respond() mechanism will call
 * the wizard's {@link arlut.csd.ganymede.GanymediatorWizard#cancel() cancel()}
 * method to retrieve a final dialog for the user.</p>
 *
 * <p>A wizard may only have 99 steps, from processDialog0() to
 * processDialog98().</p>
 *
 * <p>The {@link arlut.csd.ganymede.GanymedeSession GanymedeSession}
 * class keeps track of the client's active wizard.  It is an error
 * for there to be more than one wizard active at a time for a given
 * client.  respond() is responsible for calling {@link
 * arlut.csd.ganymede.GanymediatorWizard#unregister unregister()} when
 * a wizard is through talking to the client.</p>
 *
 * <p>Server-side code that is meant to return a ReturnVal object
 * will pass control to a wizard by constructing a new wizard subclass
 * and doing a</p>
 *
 * <p><code>return wizard.respond(null);</code></p>
 *
 * <p>to return the results of processDialog0() to the client.  From that
 * point on, the client will communicate back to the wizard as
 * required to iterate through the processDialog steps.</p>
 *
 * @see arlut.csd.ganymede.ReturnVal
 * @see arlut.csd.ganymede.Ganymediator
 *
 * @version $Revision: 1.15 $ $Date: 2001/06/01 01:35:01 $ $Name:  $
 * @author Jonathan Abbey, jonabbey@arlut.utexas.edu, ARL:UT 
 */

public abstract class GanymediatorWizard extends UnicastRemoteObject implements Ganymediator {

  public final static int STARTUP = 0;
  public final static int DONE = 99;	// we'll never have a wizard with > 99 steps, right?

  // ---

  protected boolean active = false;
  public int state = 0;
  protected GanymedeSession session = null;

  protected Hashtable returnHash;
  boolean stateSet = false;
  ReturnVal result = new ReturnVal(false);

  /* -- */

  /**
   *
   * Constructor
   *
   */

  public GanymediatorWizard(GanymedeSession session) throws RemoteException
  {
    super();			// UnicastRemoteObject initialization

    if (session == null)
      {
	throw new IllegalArgumentException("can't construct: null session");
      }

    this.session = session;

    active = session.registerWizard(this);

    if (!active)
      {
	throw new RuntimeException("error, couldn't register wizard with session");
      }

    state = 0;
  }

  /**
   *
   * This method is used to provide feedback to the server from a client
   * in response to a specific request.  Calls to this method drive the
   * wizard from state to state.<br><br>
   *
   * This method is not final, and adopters are free to totally
   * override this method in their wizard classes to provide custom
   * logic.  Generally, though, adopters are encouraged to take
   * advantage of the state machine implemented in this method and use
   * methods called processDialog1(), processDialog2(), and so on to
   * handle the dialogs at each stage of a wizard sequence.
   *
   * @param returnHash a hashtable mapping strings to values.  The strings
   * are titles of fields specified in a dialog that was provided to the
   * client.  If returnHash is null, this corresponds to the user hitting
   * cancel on such a dialog.
   *
   * @see arlut.csd.ganymede.Ganymediator
   * @see arlut.csd.ganymede.ReturnVal
   * */

  public ReturnVal respond(Hashtable returnHash)
  {
    this.returnHash = returnHash;

    /* - */

    ReturnVal result;

    /* -- */

    // clear stateSet, so that the custom wizard code
    // can call setNextState() in getStartDialog().

    stateSet = false;

    if (state == 0 || returnHash != null)
      {
	try
	  {
	    result = callDialog(state);
	  }
	catch (Throwable x)
	  {
	    this.unregister();
	    return Ganymede.createErrorDialog("Wizard Error",
					      "The wizard handling this sequence has thrown an exception: \n" +
					      x.getMessage());
	  }

	if (result == null || 
	    (!result.didSucceed() &&
	     result.getCallback() == null) ||
	    result.didSucceed())
	  {
	    this.unregister();
	  }
	else if (!stateSet)
	  {
	    state++;
	  }

	stateSet = false;
	
	return result;
      }
    else
      {	
	result = cancel();

	if (result == null || 
	    (!result.didSucceed() &&
	     result.getCallback() == null) ||
	    result.didSucceed())
	  {
	    this.unregister();
	  }

	return result;
      }
  }

  /**
   *
   * This method provides a default response if a user
   * hits cancel on a wizard dialog.  This should be
   * subclassed if a wizard wants to provide a more
   * detailed cancel response.
   *
   */

  public ReturnVal cancel()
  {
    return fail("Operation Canceled",
		"Operation Canceled",
		"OK", null, "ok.gif");
  }

  /**
   *
   * This method should be called by a processDialog*()
   * method that does not intend to proceed to the next
   * state.  By default, if a processDialog() returns
   * a ReturnVal that does not indicate completion
   * or failure _without_ the callback being set,
   * respond() will call the next processDialog() method
   * in the ordinal sequence.
   *
   */

  protected void setNextState(int state)
  {
    this.state = state;
    
    stateSet = true;
  }

  /**
   *
   * This method returns a ReturnVal that indicates that the 
   * wizard sequence has not yet finished.
   *
   */

  protected ReturnVal continueOn(String title, String body,
				 String ok, String cancel, String image)
  {
    result.clear();
    result.success = false;
    result.setDialog(new JDialogBuff(title, body, ok, cancel, image));
    result.setCallback(this);

    return result;
  }

  /**
   *
   * This method returns a ReturnVal that indicates that the 
   * wizard sequence has finished without success.
   *
   */

  protected ReturnVal fail(String title, String body,
			   String ok, String cancel, String image)
  {
    unregister();

    result.clear();
    result.success = false;
    result.setDialog(new JDialogBuff(title, body, ok, cancel, image));
    result.setCallback(null);

    return result;
  }

  /**
   *
   * This method returns a ReturnVal that indicates that the 
   * wizard sequence has terminated successfully.
   *
   */

  protected ReturnVal success(String title, String body,
			      String ok, String cancel, String image)
  {
    unregister();

    result.clear();
    result.success = true;
    result.setDialog(new JDialogBuff(title, body, ok, cancel, image));

    return result;
  }

  /**
   *
   * This method allows a processDialog*() method
   * in a GanymediatorWizard subclass to get access
   * to a value returned to the wizard from a 
   * previous dialog.
   *
   */

  protected Object getParam(Object key)
  {
    return returnHash.get(key);
  }

  /**
   *
   * This method allows a processDialog*() method
   * in a GanymediatorWizard subclass to get access
   * to an enum of keys returned to the wizard from a 
   * previous dialog.
   *
   */

  protected Enumeration getKeys()
  {
    return returnHash.keys();
  }

  /**
   *
   * This method allows a processDialog*() method
   * in a GanymediatorWizard subclass to get access
   * to an enum of values returned to the wizard from a 
   * previous dialog.
   *
   */

  protected Enumeration getElements()
  {
    return returnHash.elements();
  }

  /**
   *
   * This method uses the Java Reflection API to call a method named
   * processDialogX() in the derived class, where X is a positive integer
   * corresponding to &lt;state&gt;.
   * 
   */

  public ReturnVal callDialog(int state)
  {
    Method dialogMethod = null;
    ReturnVal localResult;

    /* -- */

    try
      {
	dialogMethod = this.getClass().getDeclaredMethod("processDialog" + state, null);
      }
    catch (NoSuchMethodException ex)
      {
	return Ganymede.createErrorDialog("Ganymede Wizard Error",
					  "GanymediatorWizard.callDialog(): Couldn't find processDialog" + state);
      }

    try
      {
	localResult = (ReturnVal) dialogMethod.invoke(this, null);
      }
    catch (InvocationTargetException ex)
      {
	InvocationTargetException invex = (InvocationTargetException) ex;
	Throwable original = invex.getTargetException();

	StringWriter stringTarget = new StringWriter();
	
	original.printStackTrace(new PrintWriter(stringTarget));

	unregister();
	return Ganymede.createErrorDialog("Ganymede Wizard Error",
					  "GanymediatorWizard.callDialog(): Invocation error in state " + 
					  state + "\n\n" + stringTarget.toString());
      }
    catch (IllegalAccessException ex)
      {
	unregister();
	return Ganymede.createErrorDialog("Ganymede Wizard Error",
					  "GanymediatorWizard.callDialog(): Illegal Access error in state " + state +
					  "\n" + ex.getMessage());
      }

    return localResult;
  }

  /**
   *
   * This method is used to inform this GanymediatorWizard that the client
   * has logged out or otherwise aborted the transaction that this wizard
   * is part of, and that it should release any references to checked out
   * DBEditObjects.
   *
   */

  public void unregister()
  {
    if (state != DONE && session.isWizardActive(this))
      {
	active = false;
	session.unregisterWizard(this);
	state = DONE;
      }
  }

  /**
   * This method is used to allow server code to determine whether
   * this GanymediatorWizard is in the middle of an interaction
   * with the client.
   * 
   */

  public boolean isActive()
  {
    return active;
  }

  /**
   *
   * This method lets us know where the wizard is in its process
   *
   */

  public int getState()
  {
    return state;
  }

  /**
   * <P>This method starts off the wizard process.</P>
   *
   * <P>This method will always be overridden in GanymediatorWizard
   * subclasses.  It is critical that if this method returns null
   * (indicating that the wizard doesn't need to interact with
   * the client), that this method calls unregister() to clear the
   * wizard from the GanymedeSession.</P>
   *
   * @deprecated getStartDialog() has been deprecated.. use
   * processDialog0() in your GanymediatorWizard subclasses instead.
   */

  public final ReturnVal getStartDialog()
  {
    return Ganymede.createErrorDialog("error, getStartDialog has been deprecated",
				      "error, getStartDialog has been deprecated");
  }
}
