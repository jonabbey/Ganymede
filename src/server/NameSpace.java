/*

   NameSpace.java

   Remote interface for NameSpace viewing/editing
   
   Created: 21 April 1997
   Version: $Revision: 1.1 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import java.rmi.*;

/*------------------------------------------------------------------------------
                                                                       interface
                                                                       NameSpace

------------------------------------------------------------------------------*/

/**
 *
 * Client side interface for the Ganymede DBNameSpace class.  This interface
 * allows the admin console to be able to view the name of the namespace and
 * view/set the case sensitivity of this namespace.
 *
 */

public interface NameSpace extends Remote {
  public String getName() throws RemoteException;
  public boolean isCaseInsensitive() throws RemoteException;
  public boolean setName(String name) throws RemoteException;
  public void setInsensitive(boolean b) throws RemoteException;
}
