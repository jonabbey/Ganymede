/*

   Ganymediator.java

   This interface provides a means whereby clients can provide
   additional information to the server in response to a dialog
   request.
   
   Created: 27 January 1998
   Version: $Revision: 1.2 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import java.rmi.*;
import java.util.Hashtable;

/*------------------------------------------------------------------------------
                                                                       interface
                                                                    Ganymediator

------------------------------------------------------------------------------*/

/**
 *
 * This interface provides a means whereby clients can provide
 * additional information to the server in response to a dialog
 * request.
 *
 * @see arlut.csd.JDialog.StringDialog
 */

public interface Ganymediator extends Remote {

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
   */

  public ReturnVal respond(Hashtable returnHash) throws RemoteException;

}
