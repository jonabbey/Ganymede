/*

   adminSession.java

   Client side remote interface.

   Client side interface definition for the Ganymede adminSession Object.  The
   Ganymede adminSession object holds the state for the Ganymede Admin console.

   Created: 28 May 1996
   Release: $Name:  $
   Version: $Revision: 1.15 $
   Last Mod Date: $Date: 2000/03/07 23:04:06 $
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

import java.rmi.*;
import java.util.Date;

/*------------------------------------------------------------------------------
                                                                       interface
                                                                    adminSession

------------------------------------------------------------------------------*/

/**
 * <p>adminSession is an RMI interface to the Ganymede server's
 * {@link arlut.csd.ganymede.GanymedeAdmin GanymedeAdmin} class.  adminSession
 * is the remote interface used by the admin console to send system commands
 * to the Ganymede server.</P>
 *
 * @version $Revision: 1.15 $ %D%
 * @author Jonathan Abbey, jonabbey@arlut.utexas.edu, ARL:UT
 */

public interface adminSession extends Remote {

  /**
   *
   * Disconnect the remote admin console associated with this object
   *
   */

  void        logout() throws RemoteException;

  /**
   * <P>This method lets the admin console explicitly request
   * a refresh.  Upon being called, the server will call several
   * methods on the admin console's {@link arlut.csd.ganymede.Admin Admin}
   * interface to pass current status information to the console.</P>
   */

  void        refreshMe() throws RemoteException;

  /**
   * Kick a user off of the Ganymede server on behalf of this admin console
   */

  ReturnVal     kill(String user) throws RemoteException;

  /**
   * Kick all users off of the Ganymede server on behalf of this admin console
   */

  ReturnVal     killAll() throws RemoteException;

  /**
   * <p>shutdown the server cleanly, on behalf of this admin console.</p>
   */

  ReturnVal     shutdown(boolean waitForUsers) throws RemoteException;

  /**
   * <P>dump the current state of the db to disk</P>
   */

  ReturnVal     dumpDB() throws RemoteException;

  /**
   * <P>dump the current db schema to disk</P>
   */

  ReturnVal     dumpSchema() throws RemoteException;

  /**
   * <P>This method causes the server to reload any registered
   * custom classes, and can be run after a schema edit
   * to cause the new classes to take over management of
   * their respective object types.</P>
   *
   * <P>It's not clear how well this actually works.. I think
   * that custom classes that have already been loaded will
   * not be reloaded by the class loader, and classes that
   * have not been loaded previously will need to already
   * be present in the jar file that the server is running out
   * of, so this method is probably not useful in practice.</P>
   *
   * <P>Better to shutdown and restart the server.</P>
   */

  ReturnVal     reloadCustomClasses() throws RemoteException;

  /**
   *
   * run a long-running verification suite on the invid links
   *
   */

  ReturnVal        runInvidTest() throws RemoteException;

  /**
   * <P>Removes any invid pointers in the Ganymede database whose
   * targets are not properly defined.  Useful after dumping a
   * schema with {@link arlut.csd.ganymede.adminSession#dumpSchema() dumpSchema()},
   * which can leave lingering invid pointers as a result of
   * the simplicity of its data filtering.</P>
   */

  ReturnVal     runInvidSweep() throws RemoteException;

  /**
   * <P>Causes a pre-registered task in the Ganymede server
   * to be executed as soon as possible.  This method call
   * will have no effect if the task is currently running.</P>
   */

  ReturnVal     runTaskNow(String name) throws RemoteException;

  /**
   * <P>Causes a running task to be stopped as soon as possible.
   * This is not always a safe operation, as the task is stopped
   * abruptly, with possible consequences.  Use with caution.</P>
   */

  ReturnVal     stopTask(String name) throws RemoteException;

  /**
   * <P>Causes a registered task to be made ineligible for execution
   * until {@link arlut.csd.ganymede.adminSession#enableTask(java.lang.String) enableTask()}
   * is called.  This method will not stop a task that is currently
   * running.</P>
   */

  ReturnVal     disableTask(String name) throws RemoteException;

  /**
   * <P>Causes a task that was temporarily disabled by
   * {@link arlut.csd.ganymede.adminSession#disableTask(java.lang.String) disableTask()}
   * to be available for execution again.</P>
   */

  ReturnVal     enableTask(String name) throws RemoteException;

  /**
   *
   * lock the server and edit the schema
   *
   */

  SchemaEdit  editSchema() throws RemoteException;
}
