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
   Version: $Revision: 1.1 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import java.rmi.*;
import java.rmi.server.*;
import java.util.Hashtable;

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

  /**
   *
   * Constructor
   *
   */

  public GanymediatorWizard(GanymedeSession session) throws RemoteException
  {
    super();			// UnicastRemoteObject initialization
  }

  /**
   *
   * This method is used to provide feedback to the server from a client
   * in response to a specific request. 
   *
   * @param returnHash a hashtable mapping strings to values.  The strings
   * are titles of fields specified in a dialog that was provided to the
   * client.  If returnHash is null, this corresponds to the user hitting
   * cancel on such a dialog.
   *
   * @see arlut.csd.ganymede.Ganymediator
   * @see arlut.csd.ganymede.ReturnVal
   *
   */

  public abstract ReturnVal respond(Hashtable returnHash);

  /**
   *
   * This method is used to inform this GanymediatorWizard that the client
   * has logged out or otherwise aborted the transaction that this wizard
   * is part of, and that it should release any references to checked out
   * DBEditObjects.
   *
   */

  public abstract void unregister();

  /**
   * This method is used to allow server code to determine whether
   * this GanymediatorWizard is in the middle of an interaction
   * with the client.
   * 
   */

  public boolean isActive()
  {
    return false;		// we can't be instantiated, after all
  }

}
