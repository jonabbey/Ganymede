/*

   adminSession.java

   Client side remote interface.

   Client side interface definition for the Ganymede adminSession Object.  The
   Ganymede adminSession object holds the state for the Ganymede Admin console.

   Created: 28 May 1996

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

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

package arlut.csd.ganymede.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

import java.util.Date;

import arlut.csd.ganymede.common.ReturnVal;

/*------------------------------------------------------------------------------
                                                                       interface
                                                                    adminSession

------------------------------------------------------------------------------*/

/**
 * <p>adminSession is an RMI interface to the Ganymede server's
 * {@link arlut.csd.ganymede.server.GanymedeAdmin GanymedeAdmin} class.  adminSession
 * is the remote interface used by the admin console to send system commands
 * to the Ganymede server.</P>
 *
 * @author Jonathan Abbey, jonabbey@arlut.utexas.edu, ARL:UT
 */

public interface adminSession extends Remote {

  /**
   * Disconnect the remote admin console associated with this object
   */

  void        logout() throws RemoteException;

  /**
   * <p>This method is used to allow the admin console to retrieve a
   * remote reference to a {@link
   * arlut.csd.ganymede.server.serverAdminAsyncResponder}, which will
   * allow the admin console to poll the server for asynchronous
   * messages from the server.</p>
   *
   * <p>This is used to allow the server to send admin notifications
   * to the console, even if the console is behind a network or
   * personal system firewall.  The serverAdminAsyncResponder blocks
   * while there is no message to send, and the console will poll for
   * new messages.</p>
   */

  AdminAsyncResponder getAsyncPort() throws RemoteException;

  /**
   * <P>This method lets the admin console explicitly request
   * a refresh.  Upon being called, the server will call several
   * methods on the admin console's {@link arlut.csd.ganymede.admin Admin}
   * interface to pass current status information to the console.</P>
   */

  void        refreshMe() throws RemoteException;

  /**
   * <p>This method is called by admin console code to force
   * a complete rebuild of all external builds.  This means that
   * all databases will have their last modification timestamp
   * cleared and all builder tasks will be scheduled for immediate
   * execution.</p>
   */

  ReturnVal     forceBuild() throws RemoteException;

  /**
   * Kicks a user off of the Ganymede server on behalf of this admin console
   */

  ReturnVal     kill(String user) throws RemoteException;

  /**
   * Kicks all users off of the Ganymede server on behalf of this admin console
   */

  ReturnVal     killAll() throws RemoteException;

  /**
   * <p>Shuts down the server cleanly, on behalf of this admin console.</p>
   *
   * @param waitForUsers if true, shutdown will be deferred until all
   * users are logged out.  No new users will be allowed to login.
   *
   * @param reason Message to be logged and displayed to any users connected.
   */

  ReturnVal     shutdown(boolean waitForUsers, String reason) throws RemoteException;

  /**
   * <P>Dumps the current state of the db to disk.</p>
   */

  ReturnVal     dumpDB() throws RemoteException;

  /**
   * <p>Runs a potentially long-running verification suite on the
   * Ganymede server database's invid links.</p>
   */

  ReturnVal        runInvidTest() throws RemoteException;

  /**
   * <p>Runs a potentially long-running verification and repair
   * operation on the Ganymede server's invid database links.</p>
   *
   * <p>Removes any invid pointers in the Ganymede database whose
   * targets are not properly defined.  This should not ever happen
   * unless there is a bug some place in the server.</p>
   */

  ReturnVal     runInvidSweep() throws RemoteException;

  /**
   * Runs a verification on the integrity of embedded objects and
   * their containers
   */

  ReturnVal        runEmbeddedTest() throws RemoteException;

  /**
   * <p>Removes any embedded objects which do not have containers.</p>
   */

  ReturnVal     runEmbeddedSweep() throws RemoteException;

  /**
   * <p>Causes a pre-registered task in the Ganymede server
   * to be executed as soon as possible.  This method call
   * will have no effect if the task is currently running.</p>
   *
   * @param name The name of the task to run
   */

  ReturnVal     runTaskNow(String name) throws RemoteException;

  /**
   * <p>Causes a running task to be interrupted as soon as possible.
   * Ganymede tasks need to be specifically written to be able
   * to respond to interruption, so it is not guaranteed that the
   * task named will always be able to safely or immediately respond
   * to a stopTask() command.</p>
   *
   * @param name The name of the task to interrupt
   */

  ReturnVal     stopTask(String name) throws RemoteException;

  /**
   * <P>Causes a registered task to be made ineligible for execution
   * until {@link
   * arlut.csd.ganymede.rmi.adminSession#enableTask(java.lang.String)
   * enableTask()} is called.  This method will not stop a task that
   * is currently running.</P>
   *
   * @param name The name of the task to disable
   */

  ReturnVal     disableTask(String name) throws RemoteException;

  /**
   * <P>Causes a task that was temporarily disabled by
   * {@link arlut.csd.ganymede.rmi.adminSession#disableTask(java.lang.String) disableTask()}
   * to be available for execution again.</P>
   *
   * @param name The name of the task to enable
   */

  ReturnVal     enableTask(String name) throws RemoteException;

  /**
   * <p>Locks the server to prevent client logins and edits the server
   * schema.</p>
   *
   * <p>This method will return a {@link
   * arlut.csd.ganymede.rmi.SchemaEdit SchemaEdit} remote reference to
   * the admin console, which will present a graphical schema editor
   * using this remote reference.  The server will remain locked until
   * the admin console commits or cancels the schema editing session,
   * either through affirmative action or through the death of the
   * admin console or the network connection.  The {@link
   * arlut.csd.ganymede.server.DBSchemaEdit DBSchemaEdit} class on the
   * server coordinates everything.</p>
   */

  SchemaEdit  editSchema() throws RemoteException;

  /**
   * <p>Retrieves a multi-line String containing information about
   * user and administrator login and logout data from the server's
   * log.</p>
   *
   * <p>If the provided startDate is null, the server will return all
   * login and logout activity since the server was last started.</p>
   *
   * <p>Otherwise, the server will return information about all logins
   * and logouts that occurred after startDate.</p>
   */

  String getLoginHistory(Date startDate) throws RemoteException;
}
