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
   Version: $Revision: 1.9 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

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
 * This class is a template for all Wizards implemented on the server.
 * Custom plug-in GanymediatorWizards may be authored to handle
 * step-by-step interactions with a user.  GanymediatorWizards are
 * typically created on the server when a client attempts an operation
 * that requires a bit of hand-holding on the part of the server.  A
 * remote handle to the newly instantiated GanymediatorWizard is returned to
 * the client, which presents a matching dialog to the user, retrieves input,
 * and calls the respond() method.  The respond() method takes the input from
 * the user and considers whether it has enough information to perform the
 * initially requested action.  If not, it will update its internal state
 * to keep track of where it is with respect to the user, and will return
 * another ReturnVal which requests the client present another dialog and
 * call back this GanymediatorWizard to continue along the process.
 *
 * @see arlut.csd.ganymede.ReturnVal
 * @see arlut.csd.ganymede.Ganymediator
 */

public abstract class GanymediatorWizard extends UnicastRemoteObject implements Ganymediator {

  public final static int STARTUP = 0;
  public final static int DONE = 99;	// we'll never have a wizard with > 99 steps, right?

  // ---

  protected boolean active = false;
  public int state = 1;
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

    state = 1;
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

    if (returnHash != null)
      {
	result = callDialog(state);

	if (result == null || 
	    (!result.didSucceed() &&
	     result.getCallback() == null) ||
	    result.didSucceed())
	  {
	    this.unregister();
	    state = DONE;
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
	    state = DONE;
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
   * wizard sequence has finished without success.
   *
   */

  protected ReturnVal fail(String title, String body,
			   String ok, String cancel, String image)
  {
    result.clear();
    result.success = false;
    result.setDialog(new JDialogBuff(title, body, ok, cancel, image));
    result.setCallback(null);

    return result;
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
   * wizard sequence has terminated successfully.
   *
   */

  protected ReturnVal success(String title, String body,
			      String ok, String cancel, String image)
  {
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
	return Ganymede.createErrorDialog("Ganymede Wizard Error",
					  "GanymediatorWizard.callDialog(): Invocation error in state " + 
					  state + "\n\n" + stringTarget.toString());
      }
    catch (IllegalAccessException ex)
      {
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
    if (session.isWizardActive(this))
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
   *
   * This method starts off the wizard process
   *
   */

  public ReturnVal getStartDialog()
  {
    return null;
  }
}
