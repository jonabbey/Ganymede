/*

   GanymedeAdmin.java

   GanymedeAdmin is the server-side implementation of the adminSession
   interface;  GanymedeAdmin provides the means by which privileged users
   can carry out privileged operations on the Ganymede server, including
   status monitoring and administrative activities.
   
   Created: 17 January 1997
   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996-2008
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
   along with this program; if not, write to the Free Software
   Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
   02111-1307, USA

*/

package arlut.csd.ganymede.server;

import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.Unreferenced;
import java.util.Date;
import java.util.Enumeration;
import java.util.Vector;

import arlut.csd.Util.VectorUtils;
import arlut.csd.ganymede.common.ReturnVal;
import arlut.csd.ganymede.rmi.AdminAsyncResponder;
import arlut.csd.ganymede.rmi.SchemaEdit;
import arlut.csd.ganymede.rmi.adminSession;

import arlut.csd.Util.TranslationService;

/*------------------------------------------------------------------------------
                                                                           class
                                                                   GanymedeAdmin

------------------------------------------------------------------------------*/

/**
 * GanymedeAdmin is the server-side implementation of the
 * {@link arlut.csd.ganymede.rmi.adminSession adminSession}
 * interface;  GanymedeAdmin provides the means by which privileged users
 * can carry out privileged operations on the Ganymede server, including
 * status monitoring and administrative activities.
 *
 * GanymedeAdmin is actually a dual purpose class.  One the one hand,
 * GanymedeAdmin implements {@link arlut.csd.ganymede.rmi.adminSession adminSession},
 * providing a hook for the admin console to talk to.  On the other,
 * GanymedeAdmin contains a lot of static fields and methods which the
 * server code uses to communicate information to any admin consoles
 * that are attached to the server at any given time.
 *
 * @version $Id$
 * @author Jonathan Abbey, jonabbey@arlut.utexas.edu, ARL:UT
 */

final class GanymedeAdmin implements adminSession, Unreferenced {

  private static final boolean debug = false;

  /**
   * TranslationService object for handling string localization in
   * the Ganymede server.
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.server.GanymedeAdmin");

  /**
   * Static vector of GanymedeAdmin instances, used to
   * keep track of the attached admin consoles.  
   */

  private static Vector consoles = new Vector();

  /**
   * Static vector of GanymedeAdmin instances for which
   * remote exceptions were caught in the static
   * update methods.  Used by
   * {@link arlut.csd.ganymede.server.GanymedeAdmin#detachBadConsoles() detachBadConsoles()}
   * to remove consoles that we were not able to communicate with.
   */

  private static Vector badConsoles = new Vector();

  /**
   * The overall server state.. 'normal operation', 'shutting down', etc.
   */

  private static String state;

  /**
   * Timestamp that the Ganymede server last consolidated its journal
   * file and dumped its database to disk.
   */

  private static Date lastDumpDate;

  /**
   * Free memory statistic that the server sends to admin consoles
   */

  private static long freeMem;

  /**
   * Total memory statistic that the server sends to admin consoles
   */

  private static long totalMem;

  /* -----====================--------------------====================-----

			         static methods

     -----====================--------------------====================----- */

  /**
   * This static method handles sending disconnect messages
   * to all attached consoles and cleaning up the
   * GanymedeAdmin.consoles Vector.
   */

  public static void closeAllConsoles(String reason)
  {
    GanymedeAdmin temp;

    /* -- */

    if (debug)
      {
	System.err.println("GanymedeAdmin.closeAllConsoles: waiting for sync");
      }

    synchronized (GanymedeAdmin.consoles)
      {
	if (debug)
	  {
	    System.err.println("GanymedeAdmin.closeAllConsoles: got sync");
	  }

	for (int i = 0; i < GanymedeAdmin.consoles.size(); i++)
	  {
	    temp = (GanymedeAdmin) GanymedeAdmin.consoles.elementAt(i);

	    try
	      {
		temp.forceDisconnect(reason);
	      }
	    catch (RemoteException ex)
	      {
		// don't worry about it
	      }
	  }

	GanymedeAdmin.consoles.removeAllElements();
      }
  }

  /**
   * This static method is used to send debug log info to
   * the consoles.  It is used by
   * {@link arlut.csd.ganymede.server.Ganymede#debug(java.lang.String) Ganymede.debug()}
   * to append information to the console logs.
   */

  public static void setStatus(String status)
  {
    GanymedeAdmin temp;
    String stampedLine;

    synchronized (GanymedeServer.lSemaphore)
      {
	if (GanymedeServer.lSemaphore.checkEnabled() == null)
	  {
	    // "{0, Date} [{1, number, #}] {2}\n"
	    stampedLine = ts.l("setStatus.enabled_template", new Date(), new Integer(GanymedeServer.lSemaphore.getCount()), status);
	  }
	else
	  {
	    // "{0, Date} [*] {1}\n"
	    stampedLine = ts.l("setStatus.disabled_template", new Date(), status);
	  }
      }

    /* -- */

    synchronized (GanymedeAdmin.consoles)
      {
	for (int i = 0; i < consoles.size(); i++)
	  {
	    temp = (GanymedeAdmin) consoles.elementAt(i);

	    try
	      {
		temp.asyncPort.changeStatus(stampedLine);
	      }
	    catch (RemoteException ex)
	      {
		handleConsoleRMIFailure(temp, ex);
	      }
	  }
      }

    detachBadConsoles();
  }

  /**
   * This static method sends an updated console count figure to
   * all of the attached admin consoles.
   */

  public static void setConsoleCount()
  {
    GanymedeAdmin temp;
    String message;

    /* -- */

    synchronized (GanymedeAdmin.consoles)
      {
	if (consoles.size() > 1)
	  {
	    // "{0, number, #} consoles attached"
	    message = ts.l("setConsoleCount.multiple_attached", new Integer(consoles.size()));
	  }
	else
	  {
	    // "1 console attached"
	    message = ts.l("setConsoleCount.single_attached");
	  }

	for (int i = 0; i < consoles.size(); i++)
	  {
	    temp = (GanymedeAdmin) consoles.elementAt(i);

	    try
	      {
		temp.asyncPort.changeAdmins(message);
	      }
	    catch (RemoteException ex)
	      {
		handleConsoleRMIFailure(temp, ex);
	      }
	  }
      }

    detachBadConsoles();
  }

  /**
   * This static method is used to send the current transcount
   * to the consoles.
   */

  public static void updateTransCount()
  {
    GanymedeAdmin temp;

    /* -- */

    synchronized (GanymedeAdmin.consoles)
      {
	for (int i = 0; i < consoles.size(); i++)
	  {
	    temp = (GanymedeAdmin) consoles.elementAt(i);

	    try
	      {
		temp.doUpdateTransCount();
	      }
	    catch (RemoteException ex)
	      {
		handleConsoleRMIFailure(temp, ex);
	      }
	  }
      }

    detachBadConsoles();
  }

  /**
   * This static method is used to send the last dump time to
   * the consoles.
   */

  public static void updateLastDump(Date date)
  {
    GanymedeAdmin.lastDumpDate = date;
    updateLastDump();
  }

  /**
   * This static method updates the last dump time to all
   * consoles.
   */

  public static void updateLastDump()
  {
    GanymedeAdmin temp;

    /* -- */

    synchronized (GanymedeAdmin.consoles)
      {
	for (int i = 0; i < consoles.size(); i++)
	  {
	    temp = (GanymedeAdmin) consoles.elementAt(i);

	    try
	      {
		temp.doUpdateLastDump();
	      }
	    catch (RemoteException ex)
	      {
		handleConsoleRMIFailure(temp, ex);
	      }
	  }
      }

    detachBadConsoles();
  }

  /**
   * This static method is used to update and transmit the server's
   * memory status to the consoles.
   */

  public static void updateMemState(long freeMem, long totalMem)
  {
    GanymedeAdmin.freeMem = freeMem;
    GanymedeAdmin.totalMem = totalMem;
    updateMemState();
  }

  /**
   * This static method is used to send the server's memory status to all
   * connected admin consoles.
   */

  public static void updateMemState()
  {
    GanymedeAdmin temp;

    /* -- */

    synchronized (GanymedeAdmin.consoles)
      {
	for (int i = 0; i < consoles.size(); i++)
	  {
	    temp = (GanymedeAdmin) consoles.elementAt(i);

	    try
	      {
		temp.doUpdateMemState();
	      }
	    catch (RemoteException ex)
	      {
		handleConsoleRMIFailure(temp, ex);
	      }
	  }
      }

    detachBadConsoles();
  }

  /**
   * This static method updates the objects checked out count on all
   * consoles.
   */

  public static void updateCheckedOut()
  {
    GanymedeAdmin temp;

    /* -- */

    synchronized (GanymedeAdmin.consoles)
      {
	for (int i = 0; i < consoles.size(); i++)
	  {
	    temp = (GanymedeAdmin) consoles.elementAt(i);

	    try
	      {
		temp.doUpdateCheckedOut();
	      }
	    catch (RemoteException ex)
	      {
		handleConsoleRMIFailure(temp, ex);
	      }
	  }
      }

    detachBadConsoles();
  }

  /**
   * This static method updates the locks held count on all consoles.
   */

  public static void updateLocksHeld()
  {
    GanymedeAdmin temp;

    /* -- */

    synchronized (GanymedeAdmin.consoles)
      {
	for (int i = 0; i < consoles.size(); i++)
	  {
	    temp = (GanymedeAdmin) consoles.elementAt(i);

	    try
	      {
		temp.doUpdateLocksHeld();
	      }
	    catch (RemoteException ex)
	      {
		handleConsoleRMIFailure(temp, ex);
	      }
	  }
      }

    detachBadConsoles();
  }

  /**
   * This static method changes the system state and
   * sends it out to the consoles
   */

  public static void setState(String state)
  {
    GanymedeAdmin temp;

    /* -- */

    GanymedeAdmin.state = state;

    synchronized (GanymedeAdmin.consoles)
      {
	for (int i = 0; i < consoles.size(); i++)
	  {
	    temp = (GanymedeAdmin) consoles.elementAt(i);

	    try
	      {
		temp.doSetState();
	      }
	    catch (RemoteException ex)
	      {
		handleConsoleRMIFailure(temp, ex);
	      }
	  }
      }

    detachBadConsoles();
  }

  /**
   * This static method is used to update the list of connnected
   * users that appears in any admin consoles attached to the Ganymede
   * server.
   */

  public static void refreshUsers()
  {
    GanymedeAdmin temp;
    Vector entries;

    /* -- */

    // figure out the vector we want to pass along

    entries = GanymedeServer.getUserTable();

    // update the consoles

    synchronized (GanymedeAdmin.consoles)
      {
	for (int i = 0; i < consoles.size(); i++)
	  {
	    temp = (GanymedeAdmin) consoles.elementAt(i);
	    
	    try
	      {
		temp.doRefreshUsers(entries);
	      }
	    catch (RemoteException ex)
	      {
		handleConsoleRMIFailure(temp, ex);
	      }
	  }
      }

    detachBadConsoles();
  }

  /**
   * This static method is used to update the list of connnected
   * users that appears in any admin consoles attached to the Ganymede
   * server.
   */

  public static void refreshTasks()
  {
    GanymedeAdmin temp;
    Vector scheduleHandles;

    /* -- */
    
    if (Ganymede.scheduler == null)
      {
	return;
      }

    scheduleHandles = Ganymede.scheduler.reportTaskInfo();

    synchronized (GanymedeAdmin.consoles)
      {
	for (int i = 0; i < consoles.size(); i++)
	  {
	    temp = (GanymedeAdmin) consoles.elementAt(i);

	    try
	      {
		temp.doRefreshTasks(scheduleHandles);
	      }
	    catch (RemoteException ex)
	      {
		handleConsoleRMIFailure(temp, ex);
	      }
	  }
      }

    detachBadConsoles();
  }

  /**
   * This private static method handles communications link
   * failures.  Note that the serverAdminAsyncResponder will handle
   * single instances of admin console RemoteExceptions so that only
   * two RemoteExceptions in sequence will raise a
   * RemoteException.
   *
   * Any code that calls this method should call detachBadConsoles()
   * once it has exited any loops over the static consoles vector to
   * actually expunge any failed consoles from our consoles vector.
   */

  private final static void handleConsoleRMIFailure(GanymedeAdmin console, RemoteException ex)
  {
    // don't use Ganymede.debug so that we can avoid infinite loops
    // during error handling

    System.err.println("Communications failure to " + console.toString());
    VectorUtils.unionAdd(badConsoles, console);
  }

  /**
   * This private static method is called to remove any consoles
   * that have experienced RMI failures from the static
   * GanymedeAdmin.consoles vector.  This method should never be
   * called from within a loop over GanymedeAdmin.consoles.
   */
   
  private static void detachBadConsoles()
  {
    GanymedeAdmin temp;

    /* -- */

    synchronized (GanymedeAdmin.consoles)
      {
	for (int i=0; i < badConsoles.size(); i++)
	  {
	    temp = (GanymedeAdmin) badConsoles.elementAt(i);

	    // the logout() method will cause the console to remove
	    // itself from the static GanymedeAdmin.consoles vecotr,
	    // which is why we are synchronized on
	    // GanymedeAdmin.consoles here.

	    // "error communicating with console"
	    temp.logout(ts.l("detachBadConsoles.error"));
	  }

	badConsoles.setSize(0);
      }
  }

  /* --- */

  /**
   * The name that the admin console authenticated with.  We
   * keep it here rather than asking the console later so that
   * the console can't decide it should call itself 'supergash'
   * at some later point.
   */

  private String adminName;

  /**
   * The name or ip address of the system that this admin console
   * is attached from.
   */

  private String clientHost;

  /**
   * If true, the admin console is attached with full privileges to
   * run tasks, shut down the server, and so on.  If false, the user
   * just has privileges to watch the server's operation.
   */

  private boolean fullprivs = false;

  /**
   * A server-side asyncPort that maintains an event queue for the
   * admin console attached to this GanymedeAdmin object.
   */

  private serverAdminAsyncResponder asyncPort;
  
  /* -- */

  /**
   * This is the GanymedeAdmin constructor, used to create a new
   * server-side admin console attachment.
   *
   * Admin is an RMI remote object exported by the client in the
   * form of a callback.
   *
   * This constructor is called from
   * {@link arlut.csd.ganymede.rmi.Server#admin(java.lang.String username, java.lang.String password) admin()},
   * which is responsible for authenticating the name and password before
   * calling this constructor.
   */

  public GanymedeAdmin(boolean fullprivs, String adminName, String clientHost) throws RemoteException
  {
    Ganymede.rmi.publishObject(this);

    // if the memoryStatusTask hasn't previously recorded the free and
    // total memory, get those statistics so we can provide them to
    // the console

    if (GanymedeAdmin.freeMem == 0 && GanymedeAdmin.totalMem == 0)
      {
	GanymedeAdmin.freeMem = Runtime.getRuntime().freeMemory();
	GanymedeAdmin.totalMem = Runtime.getRuntime().totalMemory();
      }

    this.asyncPort = new serverAdminAsyncResponder();
    this.fullprivs = fullprivs;
    this.adminName = adminName;
    this.clientHost = clientHost;

    consoles.addElement(this);	// this can block if we are currently looping on consoles

    try
      {
	setConsoleCount();
	asyncPort.setServerStart(Ganymede.startTime);
	doUpdateTransCount();
	doUpdateTransCount();
	doUpdateLastDump();
	doUpdateCheckedOut();
	doUpdateLocksHeld();
	doUpdateMemState();
	doSetState();
	
	doRefreshUsers(GanymedeServer.getUserTable());
	doRefreshTasks(Ganymede.scheduler.reportTaskInfo());
      }
    catch (RemoteException ex)
      {
	handleConsoleRMIFailure(this, ex);
      }
  }

  /**
   * This private method is used to update this admin console
   * handle's transaction count.
   */

  private void doUpdateTransCount() throws RemoteException
  {
    asyncPort.setTransactionsInJournal(Ganymede.db.journal.transactionsInJournal);
  }

  /**
   * This private method updates the last dump time on this admin
   * console
   */

  private void doUpdateLastDump() throws RemoteException
  {
    asyncPort.setLastDumpTime(GanymedeAdmin.lastDumpDate);
  }

  /**
   * This private method updates the memory statistics display on this
   * admin console
   */

  private void doUpdateMemState() throws RemoteException
  {
    asyncPort.setMemoryState(GanymedeAdmin.freeMem, GanymedeAdmin.totalMem);
  }

  /**
   * This private method updates the objects checked out display on
   * this admin console 
   */

  private void doUpdateCheckedOut() throws RemoteException
  {
    asyncPort.setObjectsCheckedOut(Ganymede.db.objectsCheckedOut);
  }

  /**
   * This private method updates the number of locks held display on
   * this admin console
   */

  private void doUpdateLocksHeld() throws RemoteException
  {
    asyncPort.setLocksHeld(Ganymede.db.lockSync.getLockCount());
  }

  /**
   * This private method updates the server state display on
   * this admin console
   */

  private void doSetState() throws RemoteException
  {
    asyncPort.changeState(GanymedeAdmin.state);
  }

  /**
   * This private method updates the user status table on
   * this admin console
   */

  private void doRefreshUsers(Vector entries) throws RemoteException
  {
    asyncPort.changeUsers(entries);
  }

  /**
   * This private method updates the task status table on
   * this admin console
   */

  private void doRefreshTasks(Vector scheduleHandles) throws RemoteException
  {
    asyncPort.changeTasks(scheduleHandles);
  }

  /**
   * This public method forces a disconnect of the remote admin console
   * and cleans up the asyncPort.
   *
   * This method *does not* handle removing this GanymedeAdmin
   * console object from the static GanymedeAdmin.consoles Vector, so
   * that it can safely be called from a loop over
   * GanymedeAdmin.consoles in closeAllConsoles() when the server is
   * being shut down.
   */

  public void forceDisconnect(String reason) throws RemoteException
  {
    asyncPort.forceDisconnect(reason);
  }

  public String toString()
  {
    if (fullprivs)
      {
	// "{0} on {1} with full access"
	return ts.l("toString.fullprivs", adminName, clientHost);
      }
    else
      {
	// "{0} on {1} with monitor access"
	return ts.l("toString.not_fullprivs", adminName, clientHost);
      }
  }

  /* -----====================--------------------====================-----

			    remotely callable methods

     -----====================--------------------====================----- */

  /**
   * Disconnect the remote admin console associated with this
   * object
   *
   * This method is part of the {@link
   * arlut.csd.ganymede.rmi.adminSession adminSession} remote interface,
   * and may be called remotely by attached admin consoles.
   *
   * No server-side code should call this method from a thread that
   * is looping over the static GanymedeAdmin.consoles Vector, or else
   * the Vector will be changed from within the loop, possibly
   * resulting in an exception being thrown.
   */

  public void logout()
  {
    this.logout(null);
  }

  /**
   * Disconnect the remote admin console associated with this
   * object
   *
   * This method is part of the {@link
   * arlut.csd.ganymede.rmi.adminSession adminSession} remote interface,
   * and may be called remotely by attached admin consoles.
   *
   * No server-side code should call this method from a thread that
   * is looping over the static GanymedeAdmin.consoles Vector, or else
   * the Vector will be changed from within the loop, possibly
   * resulting in an exception being thrown.
   */

  public void logout(String reason)
  {
    if (asyncPort.isAlive())
      {
	asyncPort.shutdown();
      }

    synchronized (GanymedeAdmin.consoles)
      {
	if (consoles.contains(this))
	  {
	    consoles.removeElement(this);

	    String eventStr = null;

	    if (reason == null)
	      {
		// "Admin console {0} detached from {1}"
		eventStr = ts.l("logout.without_reason", adminName, clientHost);
	      }
	    else
	      {
		// "Admin console {0} detached from {1}: {2}"
		eventStr = ts.l("logout.with_reason", adminName, clientHost, reason);
	      }

	    if (Ganymede.log != null)
	      {
		Ganymede.log.logSystemEvent(new DBLogEvent("admindisconnect",
							   eventStr,
							   null,
							   adminName,
							   null,
							   null));
	      }

	    setConsoleCount();
	  }
      }
  }

  /**
   * This method is called when the Java RMI system detects that this
   * remote object is no longer referenced by any remote objects.
   *
   * This method handles abnormal logouts and time outs for us.  By
   * default, the 1.1 RMI time-out is 10 minutes.
   *
   * @see java.rmi.server.Unreferenced
   */

  public void unreferenced()
  {
    // "RMI timeout/dead console"
    this.logout(ts.l("unreferenced.dead"));
  }

  /**
   * This method is used to allow the admin console to retrieve a remote reference to
   * a {@link arlut.csd.ganymede.server.serverAdminAsyncResponder}, which will allow
   * the admin console to poll the server for asynchronous messages from the server.
   *
   * This is used to allow the server to send admin notifications
   * to the console, even if the console is behind a network or
   * personal system firewall.  The serverAdminAsyncResponder blocks
   * while there is no message to send, and the console will poll for
   * new messages.
   */

  public AdminAsyncResponder getAsyncPort() throws RemoteException
  {
    // we need to create a temp variable defined in terms of the
    // interface so that RMI won't freak out and try to serialize the
    // serverAdminAsyncResponder.

    AdminAsyncResponder myAsyncPort = (AdminAsyncResponder) asyncPort;
    return myAsyncPort;
  }

  /**
   * This method lets the admin console explicitly request a
   * refresh.  Upon being called, the server will call several methods
   * on the admin console's {@link
   * arlut.csd.ganymede.server.serverAdminAsyncResponder
   * serverAdminAsyncResponder} interface to pass current status
   * information to the console.
   *
   * This method is part of the {@link
   * arlut.csd.ganymede.rmi.adminSession adminSession} remote interface,
   * and may be called remotely by attached admin consoles.
   */

  public void refreshMe() throws RemoteException
  {
    asyncPort.setServerStart(Ganymede.startTime);

    if (consoles.size() > 1)
      {
	// "{0, number, #} consoles attached"
	asyncPort.changeAdmins(ts.l("setConsoleCount.multiple_attached", new Integer(consoles.size())));
      }
    else
      {
	// "1 console attached"
	asyncPort.changeAdmins(ts.l("setConsoleCount.single_attached"));
      }

    doUpdateTransCount();
    doUpdateLastDump();
    doUpdateCheckedOut();
    doUpdateLocksHeld();
    doUpdateMemState();
    doSetState();

    doRefreshUsers(GanymedeServer.getUserTable());
    doRefreshTasks(Ganymede.scheduler.reportTaskInfo());
  }

  /**
   * This method is called by admin console code to force
   * a complete rebuild of all external builds.  This means that
   * all databases will have their last modification timestamp
   * cleared and all builder tasks will be scheduled for immediate
   * execution.
   *
   * This method is part of the {@link
   * arlut.csd.ganymede.rmi.adminSession adminSession} remote interface,
   * and may be called remotely by attached admin consoles.
   */

  public ReturnVal forceBuild()
  {
    if (!fullprivs)
      {
	// "Permissions Denied
	// "You do not have permissions to force a full rebuild."
	return Ganymede.createErrorDialog(ts.l("forceBuild.denied_title"),
					  ts.l("forceBuild.denied_text"));
      }

    // "Admin console forcing full network build..."
    Ganymede.debug(ts.l("forceBuild.proceeding"));

    Ganymede.forceBuilderTasks();

    return null;
  }

  /**
   * Kick all users off of the Ganymede server on behalf of this
   * admin console
   *
   * This method is part of the {@link
   * arlut.csd.ganymede.rmi.adminSession adminSession} remote interface,
   * and may be called remotely by attached admin consoles.
   */

  public ReturnVal killAll()
  {
    if (!fullprivs)
      {
	// "Permissions Denied"
	// "You do not have permissions to knock all users off of the server"
	return Ganymede.createErrorDialog(ts.l("killAll.denied_title"),
					  ts.l("killAll.denied_text"));
      }

    // "Admin console disconnecting you"
    GanymedeServer.server.killAllUsers(ts.l("killAll.message_to_users"));

    return null;
  }

  /**
   * Kick a user off of the Ganymede server on behalf of this admin
   * console
   *
   * This method is part of the {@link
   * arlut.csd.ganymede.rmi.adminSession adminSession} remote interface,
   * and may be called remotely by attached admin consoles.
   */

  public ReturnVal kill(String user)
  {
    if (!fullprivs)
      {
	// "Permissions Denied"
	// "You do not have permission to forcibly disconnect user {0}."
	return Ganymede.createErrorDialog(ts.l("kill.denied_title"),
					  ts.l("kill.denied_text", user));
      }

    // "Admin console disconnecting you"
    if (GanymedeServer.server.killUser(user, ts.l("kill.message_to_user")))
      {
	return null;
      }

    // "Kill Error"
    // "I couldn''t find any active user named {0}."
    return Ganymede.createErrorDialog(ts.l("kill.error_title"),
				      ts.l("kill.error_text", user));
  }

  /**
   * shutdown the server cleanly, on behalf of this admin console.
   *
   * @param waitForUsers if true, shutdown will be deferred until all users are logged
   * out.  No new users will be allowed to login.
   *
   * This method is part of the {@link
   * arlut.csd.ganymede.rmi.adminSession adminSession} remote interface,
   * and may be called remotely by attached admin consoles.
   */

  public ReturnVal shutdown(boolean waitForUsers)
  {
    if (!fullprivs)
      {
	// "Permissions Denied"
	// "You do not have permission to shut down the Ganymede server."
	return Ganymede.createErrorDialog(ts.l("shutdown.denied_title"),
					  ts.l("shutdown.denied_text"));
      }

    if (waitForUsers)
      {
	GanymedeServer.setShutdown();

	// "Server Set For Shutdown"
	// "The server is prepared for shut down.  Shutdown will commence as soon as all current users log out."
	return Ganymede.createInfoDialog(ts.l("shutdown.advisory_title"),
					 ts.l("shutdown.advisory_text"));
      }
    else
      {
	return GanymedeServer.shutdown(); // we may never return if the shutdown succeeds.. the client
                                          // will catch an exception in that case.
      }
  }

  /**
   * dump the current state of the db to disk
   *
   * This method is part of the {@link
   * arlut.csd.ganymede.rmi.adminSession adminSession} remote interface,
   * and may be called remotely by attached admin consoles.
   */

  public ReturnVal dumpDB()
  {
    if (!fullprivs)
      {
	// "Permissions Denied"
	// "You do not have permission to execute a database dump."
	return Ganymede.createErrorDialog(ts.l("dumpDB.denied_title"),
					  ts.l("dumpDB.denied_text"));
      }

    // "Dumping Database"
    setState(ts.l("dumpDB.dump_state"));

    try
      {
	Ganymede.db.dump(Ganymede.dbFilename, true, true); // release, archive
      }
    catch (IOException ex)
      {
	// "Database Dump Error"
	// "Database could not be dumped successfully: {0}"
	return Ganymede.createErrorDialog(ts.l("dumpDB.error_title"),
					  ts.l("dumpDB.error_text", ex.toString()));
      }
    catch (InterruptedException ex)
      {
	// "Database Dump Error"
	// "Database could not be dumped successfully: {0}"
	return Ganymede.createErrorDialog(ts.l("dumpDB.error_title"),
					  ts.l("dumpDB.error_text", ex.toString()));
      }
    finally
      {
	// "Normal Operation"
	setState(DBStore.normal_state);
      }

    Ganymede.debug(ts.l("dumpDB.dumped"));

    return null;
  }

  /**
   * run a long-running verification suite on the Ganymede server
   * database's invid links
   *
   * This method is part of the {@link
   * arlut.csd.ganymede.rmi.adminSession adminSession} remote interface,
   * and may be called remotely by attached admin consoles.
   */

  public ReturnVal runInvidTest()
  {
    if (!fullprivs)
      {
	// "Permissions Denied"
	// "You do not have permission to execute an Invid integrity test on the server."
	return Ganymede.createErrorDialog(ts.l("runInvidTest.denied_title"),
					  ts.l("runInvidTest.denied_text"));
      }

    // "Running Invid Test"
    GanymedeAdmin.setState(ts.l("runInvidTest.running_state"));
	 
    if (Ganymede.server.checkInvids())
      {
	// "Invid Test completed successfully, no problems boss."
	Ganymede.debug(ts.l("runInvidTest.good_result"));
      }
    else
      {
	// "Invid Test encountered problems.  Oi, you're in the soup now, boss."
	Ganymede.debug(ts.l("runInvidTest.bad_result"));
      }

    // "Normal Operation"
    GanymedeAdmin.setState(DBStore.normal_state);

    return null;
  }

  /**
   * run a long-running verification and repair operation on the Ganymede
   * server's invid database links   
   *
   * Removes any invid pointers in the Ganymede database whose
   * targets are not properly defined.  This should not ever happen
   * unless there is a bug some place in the server.
   *
   * This method is part of the {@link
   * arlut.csd.ganymede.rmi.adminSession adminSession} remote interface,
   * and may be called remotely by attached admin consoles.
   */

  public ReturnVal runInvidSweep()
  {
    if (!fullprivs)
      {
	// "Permissions Denied"
	// "You do not have permission to execute an Invid sweep on the server."
	return Ganymede.createErrorDialog(ts.l("runInvidSweep.denied_title"),
					  ts.l("runInvidSweep.denied_text"));
      }

    // "Running Invid Sweep"
    GanymedeAdmin.setState(ts.l("runInvidSweep.running_state"));
    Ganymede.debug(ts.l("runInvidSweep.running_state"));
	 
    Ganymede.server.sweepInvids();

    // "Normal Operation"
    GanymedeAdmin.setState(DBStore.normal_state);
    Ganymede.debug(DBStore.normal_state);

    return null;
  }

  /**
   * run a verification on the integrity of embedded objects and
   * their containers
   *
   * This method is part of the {@link
   * arlut.csd.ganymede.rmi.adminSession adminSession} remote interface,
   * and may be called remotely by attached admin consoles.
   */

  public ReturnVal runEmbeddedTest()
  {
    if (!fullprivs)
      {
	// "Permissions Denied"
	// "You do not have permission to execute an embedded objects integrity test on the server."
	return Ganymede.createErrorDialog(ts.l("runEmbeddedTest.denied_title"),
					  ts.l("runEmbeddedTest.denied_text"));
      }

    // "Running Embedded Test"
    GanymedeAdmin.setState(ts.l("runEmbeddedTest.running_state"));
	 
    if (Ganymede.server.checkEmbeddedObjects())
      {
	// "Embedded Objects Test completed successfully, no problems boss."
	Ganymede.debug(ts.l("runEmbeddedTest.good_result"));
      }
    else
      {
	// "Embedded Objects Test encountered problems.  Oi, you're in the soup now, boss."
	Ganymede.debug(ts.l("runEmbeddedTest.bad_result"));
      }

    // "Normal Operation"
    GanymedeAdmin.setState(DBStore.normal_state);

    return null;
  }

  /**
   * Removes any embedded objects which do not have containers.
   *
   * This method is part of the {@link
   * arlut.csd.ganymede.rmi.adminSession adminSession} remote interface,
   * and may be called remotely by attached admin consoles.
   */

  public ReturnVal runEmbeddedSweep()
  {
    if (!fullprivs)
      {
	// "Permissions Denied"
	// "You do not have permission to execute an Embedded Objects sweep on the server."
	return Ganymede.createErrorDialog(ts.l("runEmbeddedSweep.denied_title"),
					  ts.l("runEmbeddedSweep.denied_text"));
      }

    return Ganymede.server.sweepEmbeddedObjects();
  }

  /**
   * Causes a pre-registered task in the Ganymede server
   * to be executed as soon as possible.  This method call
   * will have no effect if the task is currently running.
   *
   * @param name The name of the task to run
   *
   * This method is part of the {@link
   * arlut.csd.ganymede.rmi.adminSession adminSession} remote interface,
   * and may be called remotely by attached admin consoles.
   */

  public ReturnVal runTaskNow(String name)
  {
    if (!fullprivs)
      {
	// "Permissions Denied"
	// "You do not have permission to execute tasks on the server."
	return Ganymede.createErrorDialog(ts.l("runTaskNow.denied_title"),
					  ts.l("runTaskNow.denied_text"));
      }

    if (Ganymede.scheduler.runTaskNow(name))
      {
	return null;
      }

    // "Couldn''t run task {0}.  Some sort of error on the server?"
    return Ganymede.createErrorDialog(ts.l("runTaskNow.error", name));
  }

  /**
   * Causes a running task to be interrupted as soon as possible.
   * Ganymede tasks need to be specifically written to be able
   * to respond to interruption, so it is not guaranteed that the
   * task named will always be able to safely or immediately respond
   * to a stopTask() command.
   *
   * @param name The name of the task to interrupt
   *
   * This method is part of the {@link
   * arlut.csd.ganymede.rmi.adminSession adminSession} remote interface,
   * and may be called remotely by attached admin consoles.
   */

  public ReturnVal stopTask(String name)
  {
    if (!fullprivs)
      {
	// "Permissions Denied"
	// "You do not have permission to stop tasks on the server."
	return Ganymede.createErrorDialog(ts.l("stopTask.denied_title"),
					  ts.l("stopTask.denied_text"));
      }

    if (Ganymede.scheduler.stopTask(name))
      {
	return null;
      }

    // "Couldn''t stop task {0}.  Perhaps the task wasn't running?"
    return Ganymede.createErrorDialog(ts.l("stopTask.error", name));
  }

  /**
   * Causes a registered task to be made ineligible for execution
   * until {@link arlut.csd.ganymede.server.GanymedeAdmin#enableTask(java.lang.String) enableTask()}
   * is called.  This method will not stop a task that is currently
   * running.
   *
   * @param name The name of the task to disable
   *
   * This method is part of the {@link
   * arlut.csd.ganymede.rmi.adminSession adminSession} remote interface,
   * and may be called remotely by attached admin consoles.
   */

  public ReturnVal disableTask(String name)
  {
    if (!fullprivs)
      {
	// "Permissions Denied"
	// "You do not have permission to disable tasks on the server."
	return Ganymede.createErrorDialog(ts.l("disableTask.denied_title"),
					  ts.l("disableTask.denied_text"));
      }

    if (Ganymede.scheduler.disableTask(name))
      {
	return null;
      }

    // "Couldn''t disable task {0}.  Some sort of error on the server?"
    return Ganymede.createErrorDialog(ts.l("disableTask.error", name));
  }

  /**
   * Causes a task that was temporarily disabled by
   * {@link arlut.csd.ganymede.server.GanymedeAdmin#disableTask(java.lang.String) disableTask()}
   * to be available for execution again.
   *
   * @param name The name of the task to enable
   *
   * This method is part of the {@link
   * arlut.csd.ganymede.rmi.adminSession adminSession} remote interface,
   * and may be called remotely by attached admin consoles.
   */

  public ReturnVal enableTask(String name)
  {
    if (!fullprivs)
      {
	// "Permissions Denied"
	// "You do not have permission to re-enable tasks on the server."
	return Ganymede.createErrorDialog(ts.l("enableTask.denied_title"),
					  ts.l("enableTask.denied_text"));
      }

    if (Ganymede.scheduler.enableTask(name))
      {
	return null; 
      }

    // "Couldn''t enable task {0}.  Perhaps the task isn't registered?"
    return Ganymede.createErrorDialog(ts.l("enableTask.error", name));
  }

  /**
   * Lock the server to prevent client logins and edit the server
   * schema.  This method will return a {@link
   * arlut.csd.ganymede.rmi.SchemaEdit SchemaEdit} remote reference to the
   * admin console, which will present a graphical schema editor using
   * this remote reference.  The server will remain locked until the
   * admin console commits or cancels the schema editing session,
   * either through affirmative action or through the death of the
   * admin console or the network connection.  The {@link
   * arlut.csd.ganymede.server.DBSchemaEdit DBSchemaEdit} class on the server
   * coordinates everything.
   *
   * This method is part of the {@link
   * arlut.csd.ganymede.rmi.adminSession adminSession} remote interface,
   * and may be called remotely by attached admin consoles.
   */

  public SchemaEdit editSchema()
  {
    Enumeration en;
    DBObjectBase base;

    /* -- */

    if (!fullprivs)
      {
	// "Attempt made to edit schema by a non-privileged console: {0}"
	Ganymede.debug(ts.l("editSchema.no_privs", this.toString()));
	return null;
      }

    // "entering editSchema"
    Ganymede.debug(ts.l("editSchema.entering"));

    // synchronize on server so we don't get logins while we're checking
    // things out

    try
      {
	// "schema edit"
	String semaphoreCondition = GanymedeServer.lSemaphore.disable(ts.l("editSchema.semaphore_token"), true, 0);

	if (semaphoreCondition != null)
	  {
	    // "Admin console {0} can''t edit schema.  Ganymede login semaphore already locked with condition "{1}"."
	    Ganymede.debug(ts.l("editSchema.semaphore_error", this.toString(), semaphoreCondition));

	    return null;
	  }
      }
    catch (InterruptedException ex)
      {
	ex.printStackTrace();
	throw new RuntimeException(ex.getMessage());
      }

    // okay at this point we've asserted our interest in editing the
    // schema and made sure that no one is logged in or can log in.
    // Now we just need to make sure that we don't have any of the
    // bases locked by anything that is skipping the semaphore, such
    // as tasks.

    // In fact, I believe that the server is now safe against lock
    // races due to all tasks that might involve DBObjectBase access
    // being guarded by the loginSemaphore, but there is little cost
    // in sync'ing here.

    // All the DBLock establish methods synchronize on the DBLockSync
    // object referenced by Ganymede.db.lockSync, so we are safe
    // against lock establish race conditions by synchronizing this
    // section on Ganymede.db.lockSync.

    synchronized (Ganymede.db.lockSync)
      {
	// "Admin console {0} entering editSchema synchronization block."
	Ganymede.debug(ts.l("editSchema.synchronizing", this.toString()));

	en = Ganymede.db.objectBases.elements();

	if (en != null)
	  {
	    while (en.hasMoreElements())
	      {
		base = (DBObjectBase) en.nextElement();
		
		if (base.isLocked())
		  {
		    // "Admin console {0} can''t edit Schema, lock held on {1}."
		    Ganymede.debug(ts.l("editSchema.locked_base", this.toString(), base.getName()));
		    
		    // "schema edit"
		    GanymedeServer.lSemaphore.enable(ts.l("editSchema.semaphore_token"));
		    
		    return null;
		  }
	      }
	  }
	
	// should be okay
	
	// "Ok to create DBSchemaEdit for admin console {0}."
	Ganymede.debug(ts.l("editSchema.okay_to_go", this.toString()));
	
	// "Schema Edit In Progress"
	GanymedeAdmin.setState(ts.l("editSchema.edit_state"));
	
	try
	  {
	    DBSchemaEdit result = new DBSchemaEdit();
	    return result;
	  }
	catch (RemoteException ex)
	  {
	    // "schema edit"
	    GanymedeServer.lSemaphore.enable(ts.l("editSchema.semaphore_token"));
	    return null;
	  }
      }
  }

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

  public String getLoginHistory(Date startDate)
  {
    return Ganymede.log.retrieveHistory(null, startDate, null, false, false, true).toString();
  }
}
