/*
   GASHAdminDispatch.java

   Logical interface class that provides the connectivity between the
   Ganymede admin console and the server, bidirectionally.

   Created: 28 May 1996
   Version: $Revision: 1.5 $
   Last Mod Date: $Date: 2003/09/09 03:01:42 $
   Release: $Name:  $

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999, 2000, 2001, 2002, 2003
   The University of Texas at Austin.

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
   Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
   02111-1307, USA

*/

package arlut.csd.ganymede;

import javax.swing.*;
import javax.swing.border.*;

import java.awt.*;
import java.awt.event.*;
import java.applet.*;

import java.rmi.*;
import java.rmi.server.*;

import java.lang.reflect.InvocationTargetException;
import java.io.*;
import java.util.*;

import arlut.csd.JDataComponent.*;
import arlut.csd.JTable.*;
import arlut.csd.JDialog.*;
import arlut.csd.Util.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                               GASHAdminDispatch

------------------------------------------------------------------------------*/

/**
 * <p>Logical interface class that provides the connectivity between the
 * Ganymede admin console and the server, bidirectionally.  This class is part
 * of the Ganymede admin console code, not the Ganymede server.</p>
 */

class GASHAdminDispatch implements Runnable {

  static final boolean debug = false;

  private GASHAdminFrame frame = null;
  private Server server = null;	// remote reference
  private adminSession aSession = null;	// remote reference

  private boolean tasksLoaded = false;
  private Vector tasksKnown = null;

  private AdminAsyncResponder asyncPort = null;	// remote reference

  private Thread asyncPollThread = null;
  private volatile boolean okayToPoll = false;

  /* -- */

  public GASHAdminDispatch(Server server)
  {
    this.server = server;
  }

  /**
   * <p>This method connects the admin console to the server RMI reference
   * that was provided to the GASHAdminDispatch constructor.  If the
   * server returns a failure message, connect() will pop up a dialog
   * providing the error text.  If the connection failed through a 
   * RemoteException, it will be passed up for the calling code to handle.</p>
   */

  public boolean connect(String name, String pass) throws RemoteException
  { 
    try
      {
	ReturnVal retVal = handleReturnVal(server.admin(name, pass));

	if (retVal.didSucceed())
	  {
	    aSession = retVal.getAdminSession();

	    if (aSession == null)
	      {
		throw new IllegalArgumentException();
	      }
	  }
	else
	  {
	    return false;
	  }
      }
    catch (NullPointerException ex)
      {
	System.err.println("Error: Didn't get server reference.  Exiting now.");
	return false;
      }

    if (debug)
      {
	System.err.println("Got Admin");
      }

    return true;
  }

  public void setFrame(GASHAdminFrame f)
  {
    if (frame == null)
      {
	frame = f;
      }
    else
      {
	System.err.println("I already have a frame, thank you very much.");
      }
  }

  public void startAsyncPoller() throws RemoteException
  {
    asyncPort = aSession.getAsyncPort();

    if (asyncPort == null)
      {
	throw new RemoteException("Couldn't find admin console asyncPort");
      }

    okayToPoll = true;
    asyncPollThread = new Thread(this, "Async Server Poll Thread");
    asyncPollThread.start();
  }

  public void stopAsyncPoller()
  {
    okayToPoll = false;
  }

  /**
   * <p>This method spins continuously, polling the server for {@link
   * arlut.csd.ganymede.adminAsyncMessage adminAsyncMessages}.  The
   * server will block until something happens, then download a set of
   * adminAsyncMessages.  This run method will then dispatch those messages
   * to the appropriate GASHAdminDispatch methods for propagation into the
   * admin console's GUI.</p>
   */

  public void run()
  {
    adminAsyncMessage events[] = null;
    adminAsyncMessage event = null;

    /* -- */

    if (asyncPort == null)
      {
	return;
      }

    try
      {
	while (okayToPoll)
	  {
	    events = asyncPort.getNextMsgs(); // will block on server

	    if (events == null || events.length == 0)
	      {
		return;
	      }

	    for (int i = 0; i < events.length; i++)
	      {
		event = events[i];

		switch (event.getMethod())
		  {
		  case adminAsyncMessage.SETSERVERSTART:
		    setServerStart((Date) event.getParam(0));
		    break;

		  case adminAsyncMessage.SETLASTDUMPTIME:
		    setLastDumpTime((Date) event.getParam(0));
		    break;

		  case adminAsyncMessage.SETTRANSACTIONS:
		    setTransactionsInJournal(event.getInt(0));
		    break;

		  case adminAsyncMessage.SETOBJSCHECKOUT:
		    setObjectsCheckedOut(event.getInt(0));
		    break;

		  case adminAsyncMessage.SETLOCKSHELD:
		    setLocksHeld(event.getInt(0));
		    break;

		  case adminAsyncMessage.CHANGESTATE:
		    changeState(event.getString(0));
		    break;

		  case adminAsyncMessage.CHANGESTATUS:
		    changeStatus(((StringBuffer) event.getParam(0)).toString());
		    break;

		  case adminAsyncMessage.CHANGEADMINS:
		    changeAdmins(event.getString(0));
		    break;

		  case adminAsyncMessage.CHANGEUSERS:
		    changeUsers(event.getParams());
		    break;

		  case adminAsyncMessage.CHANGETASKS:
		    changeTasks(event.getParams());
		    break;

		  case adminAsyncMessage.SETMEMORYSTATE:
		    setMemoryState(event.getLong(0), event.getLong(1));
		    break;

		  case adminAsyncMessage.FORCEDISCONNECT:
		    forceDisconnect(event.getString(0));
		    break;

		  default:
		    System.err.println("Unrecognized adminAsyncMessage: " + event);
		  }
	      }
	  }
      }
    catch (RemoteException ex)
      {
      }
    finally
      {
	asyncPort = null;
      }
  }

  /**
   * <p>This method is remotely called by the Ganymede server to set the server start
   * date in the admin console.</p>
   */

  public void setServerStart(Date date)
  {
    if (debug)
      {
	System.err.println("GASHAdminDispatch.setServerStart()");
      }

    if (frame == null)
      {
	return;
      }

    final Date lDate = date;

    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
	frame.startField.setText(lDate.toString());
      }
    });
  }

  /**
   * <p>This method is remotely called by the Ganymede server to set the last dump
   * date in the admin console.</p>
   */

  public void setLastDumpTime(Date date)
  {
    if (debug)
      {
	System.err.println("GASHAdminDispatch.setLastDumpTime()");
      }

    if (frame == null)
      {
	return;
      }

    final Date lDate = date;
    
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
	if (lDate == null)
	  {
	    frame.dumpField.setText("no dump since server start");
	  }
	else
	  {
	    frame.dumpField.setText(lDate.toString());
	  }
      }
    });
  }

  /**
   * <p>This method is remotely called by the Ganymede server to set the number of
   * transactions in the server's journal in the admin console.</p>
   */

  public void setTransactionsInJournal(int trans)
  {
    if (debug)
      {
	System.err.println("GASHAdminDispatch.setTransactionsInJournal()");
      }

    if (frame == null)
      {
	return;
      }

    final int lTrans = trans;
    
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
	frame.journalField.setText("" + lTrans);
      }
    });
  }

  /**
   * <p>This method is remotely called by the Ganymede server to set the number of
   * objects checked out in the admin console.</p>
   */

  public void setObjectsCheckedOut(int objs)
  {
    if (debug)
      {
	System.err.println("GASHAdminDispatch.setObjectsCheckedOut()");
      }

    if (frame == null)
      {
	return;
      }

    final int lObjs = objs;
    
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
	frame.checkedOutField.setText("" + lObjs);
      }
    });
  }

  /**
   * <p>This method is remotely called by the Ganymede server to set the number of
   * locks held in the admin console.</p>
   */

  public void setLocksHeld(int locks)
  {
    if (debug)
      {
	System.err.println("GASHAdminDispatch.setLocksHeld()");
      }

    if (frame == null)
      {
	return;
      }

    final int lLocks = locks;

    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
	frame.locksField.setText("" + lLocks);
      }
    });
  }

  /**
   * <p>This method is remotely called by the Ganymede server to update the
   * memory statistics display in the admin console.</p>
   */

  public void setMemoryState(long freeMemory, long totalMemory)
  {
    if (debug)
      {
	System.err.println("GASHAdminDispatch.setMemoryState()");
      }

    if (frame == null)
      {
	return;
      }

    final long lFreeMemory = freeMemory;
    final long lTotalMemory = totalMemory;

    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
	frame.memField.setText((lTotalMemory - lFreeMemory) + " / " + lFreeMemory + " / " + lTotalMemory);
      }
    });
  }

  /**
   * <p>This method is remotely called by the Ganymede server to add to the
   * admin console's log display.</p>
   *
   * @param status A string to add to the console's log display, with the
   * trailing newline included.
   */

  public void changeStatus(String status)
  {
    if (debug)
      {
	System.err.println("GASHAdminDispatch.changeStatus()");
      }

    if (frame == null)
      {
	return;
      }

    final String lStatus = status;

    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
	frame.statusArea.append(lStatus);
	frame.statusArea.setCaretPosition(frame.statusArea.getText().length());
      }
    });
  }

  /**
   * <p>This method is remotely called by the Ganymede server to update the
   * number of admin consoles attached to the server.</p>
   */

  public void changeAdmins(String adminStatus)
  {
    if (debug)
      {
	System.err.println("GASHAdminDispatch.changeAdmins()");
      }

    if (frame == null)
      {
	return;
      }

    final String lAdminStatus = adminStatus;

    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
	frame.adminField.setText(lAdminStatus);
      }
    });
  }

  /**
   * <p>This method is remotely called by the Ganymede server to update the
   * admin console's server state display.</p>
   */

  public void changeState(String state)
  {
    if (debug)
      {
	System.err.println("GASHAdminDispatch.changeState()");
      }

    if (frame == null)
      {
	return;
      }

    final String lState = state;

    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
	frame.stateField.setText(lState);
      }
    });
  }

  /**
   * <p>This method is remotely called by the Ganymede server to update the
   * admin console's connected user table.</p>
   *
   * @param entries a Vector of {@link arlut.csd.ganymede.AdminEntry AdminEntry}
   * login description objects.
   */

  public void changeUsers(Object entries[])
  {
    if (debug)
      {
	System.err.println("GASHAdminDispatch.changeUsers()");
      }

    if (frame == null)
      {
	return;
      }

    /* -- */

    final Object localEntries[] = entries;

    // And refresh our table.. we'll wait until this succeeds so we
    // don't get the server sending us more updates before the table
    // finishes drawing

    try
      {
	SwingUtilities.invokeAndWait(new Runnable() {
	  public void run() {

	    AdminEntry e;
	    frame.table.clearCells();
    
	    // Process entries from the server

	    for (int i = 0; i < localEntries.length; i++)
	      {
		e = (AdminEntry) localEntries[i];

		frame.table.newRow(e.username);

		if (e.personaName == null || e.personaName.equals(""))
		  {
		    frame.table.setCellText(e.username, 0, e.username, false);
		  }
		else
		  {
		    frame.table.setCellText(e.username, 0, e.personaName, false);
		  }

		frame.table.setCellText(e.username, 1, e.hostname, false);
		frame.table.setCellText(e.username, 2, e.status, false);
		frame.table.setCellText(e.username, 3, e.connecttime, false);
		frame.table.setCellText(e.username, 4, e.event, false);
		frame.table.setCellText(e.username, 5, Integer.toString(e.objectsCheckedOut), false);
	      }

	    frame.table.refreshTable();
	  }
	});
      }
    catch (InvocationTargetException ite)
      {
	ite.printStackTrace();
      }
    catch (InterruptedException ie)
      {
	ie.printStackTrace();
      }
  }

  /**
   * <p>This method is remotely called by the Ganymede server to update the
   * admin console's task table.</p>
   *
   * @param tasks a Vector of {@link arlut.csd.ganymede.scheduleHandle scheduleHandle}
   * objects describing the tasks registered in the Ganymede server.
   */

  public void changeTasks(Object tasks[])
  {
    if (debug)
      {
	System.err.println("GASHAdminDispatch.changeTasks()");
      }

    if (frame == null)
      {
	return;
      }

    scheduleHandle handle;
    String intervalString;

    /* -- */

    if (!tasksLoaded)
      {
	// System.err.println("changeTasks: tasks size = " + tasks.size());
    
	// Sort entries according to their incep date,
	// to prevent confusion if new tasks are put into
	// the server-side hashes, and as they are shuffled
	// from hash to hash
	
	(new QuickSort(tasks, 
		       new arlut.csd.Util.Compare() 
		       {
			 public int compare(Object a, Object b) 
			 {
			   scheduleHandle aH, bH;
				
			   aH = (scheduleHandle) a;
			   bH = (scheduleHandle) b;
				
			   if (aH.incepDate.before(bH.incepDate))
			     {
			       return -1;
			     }
			   else if (aH.incepDate.after(bH.incepDate))
			     {
			       return 1;
			     }
			   else
			     {
			       return 0;
			     }
			 }
		       }
		       )).sort();
      }

    Vector taskNames = new Vector();

    // now reload the table with the current stats

    for (int i = 0; i < tasks.length; i++)
      {
	handle = (scheduleHandle) tasks[i];

	taskNames.addElement(handle.name);

	if (!frame.taskTable.containsKey(handle.name))
	  {
	    frame.taskTable.newRow(handle.name);
	  }

	frame.taskTable.setCellText(handle.name, 0, handle.name, false); // task name

	if (handle.isRunning)
	  {
	    frame.taskTable.setCellText(handle.name, 1, "Running", false);
	    frame.taskTable.setCellColor(handle.name, 1, Color.blue, false);
	    frame.taskTable.setCellBackColor(handle.name, 1, Color.white, false);
	  }
	else if (handle.suspend)
	  {
	    frame.taskTable.setCellText(handle.name, 1, "Suspended", false);
	    frame.taskTable.setCellColor(handle.name, 1, Color.red, false);
	    frame.taskTable.setCellBackColor(handle.name, 1, Color.white, false);
	  }
	else if (handle.startTime != null)
	  {
	    frame.taskTable.setCellText(handle.name, 1, "Scheduled", false);
	    frame.taskTable.setCellColor(handle.name, 1, Color.black, false);
	    frame.taskTable.setCellBackColor(handle.name, 1, Color.white, false);
	  }
	else
	  {
	    frame.taskTable.setCellText(handle.name, 1, "Waiting", false);
	    frame.taskTable.setCellColor(handle.name, 1, Color.black, false);
	    frame.taskTable.setCellBackColor(handle.name, 1, Color.white, false);
	  }

	if (handle.lastTime != null)
	  {
	    frame.taskTable.setCellText(handle.name, 2, handle.lastTime.toString(), false);
	  }

	if (handle.startTime != null)
	  {
	    frame.taskTable.setCellText(handle.name, 3, handle.startTime.toString(), false);
	  }
	else
	  {
	    frame.taskTable.setCellText(handle.name, 3, "On Demand", false);
	  }

	frame.taskTable.setCellText(handle.name, 4, handle.intervalString, false);
      }

    // and take any rows out that are gone

    if (tasksKnown == null)
      {
	tasksKnown = taskNames;
      }
    else
      {
	Vector removedTasks = VectorUtils.difference(tasksKnown, taskNames);

	for (int i = 0; i < removedTasks.size(); i++)
	  {
	    frame.taskTable.deleteRow(removedTasks.elementAt(i), false);
	  }

	tasksKnown = taskNames;
      }

    // And refresh our table.. we'll wait until this succeeds so we
    // don't get the server sending us more updates before the table
    // finishes drawing

    try
      {
	SwingUtilities.invokeAndWait(new Runnable() {
	  public void run() {
	    frame.taskTable.refreshTable();
	  }
	});
      }
    catch (InvocationTargetException ite)
      {
	ite.printStackTrace();
      }
    catch (InterruptedException ie)
      {
	ie.printStackTrace();
      }
  }

  /**
   * <p>This method is called by admin console code to force
   * a complete rebuild of all external builds.  This means that
   * all databases will have their last modification timestamp
   * cleared and all builder tasks will be scheduled for immediate
   * execution.</p>
   */

  public void forceBuild() throws RemoteException
  {
    handleReturnVal(aSession.forceBuild());
  }

  public void disconnect() throws RemoteException
  {
    aSession.logout();
  }

  /**
   *
   * Callback: The server can tell us to disconnect if the server is 
   * going down.
   *
   */

  public void forceDisconnect(String reason)
  {
    changeStatus("Disconnected: " + reason + "\n");
    server = null;
  }

  // ------------------------------------------------------------
  // convenience methods for our GASHAdminFrame
  // ------------------------------------------------------------

  void refreshMe() throws RemoteException
  {
    aSession.refreshMe();
  }

  void kill(String username) throws RemoteException
  {
    handleReturnVal(aSession.kill(username));
  }

  void runTaskNow(String taskName) throws RemoteException
  {
    handleReturnVal(aSession.runTaskNow(taskName));
  }

  void stopTask(String taskName) throws RemoteException
  {
    handleReturnVal(aSession.stopTask(taskName));
  }

  void disableTask(String taskName) throws RemoteException
  {
    handleReturnVal(aSession.disableTask(taskName));
  }

  void enableTask(String taskName) throws RemoteException
  {
    handleReturnVal(aSession.enableTask(taskName));
  }

  void killAll() throws RemoteException
  {
    handleReturnVal(aSession.killAll());
  }

  boolean shutdown(boolean waitForUsers) throws RemoteException
  {
    ReturnVal retVal = handleReturnVal(aSession.shutdown(waitForUsers));

    return (retVal == null || retVal.didSucceed());
  }

  void dumpDB() throws RemoteException
  {
    handleReturnVal(aSession.dumpDB());
  }

  void runInvidTest() throws RemoteException
  {
    handleReturnVal(aSession.runInvidTest());
  }

  void runInvidSweep() throws RemoteException
  {
    handleReturnVal(aSession.runInvidSweep());
  }

  void runEmbeddedTest() throws RemoteException
  {
    handleReturnVal(aSession.runEmbeddedTest());
  }

  void runEmbeddedSweep() throws RemoteException
  {
    handleReturnVal(aSession.runEmbeddedSweep());
  }

  GASHSchema pullSchema() throws RemoteException
  {
    SchemaEdit editor = null;

    /* -- */

    if (debug)
      {
	System.err.println("Trying to get SchemaEdit handle");
      }

    try
      {
	editor = aSession.editSchema();
      }
    catch (RemoteException ex)
      {
	System.err.println("editSchema() exception: " + ex);
      }

    if (editor == null)
      {
	System.err.println("null editor handle");
	return null;
      }
    else
      {
	if (debug)
	  {
	    System.err.println("Got SchemaEdit handle");
	  }

	// the GASHSchema constructor pops itself up at the end of
	// initialization
	
	return new GASHSchema("Schema Editor", editor, this);
      }
  }

  /**
   * <p>This method is designed to be called by the GASHSchema schema editor when
   * it closes down.</p>
   */

  public void clearSchemaEditor()
  {
    schemaMI.setEnabled(true);
    GASHAdminFrame.schemaEditor = null;
  }

  /**
   * <p>This method takes a ReturnVal object from the server and, if necessary,
   * runs through a wizard interaction sequence, possibly displaying several
   * dialogs before finally returning a final result code.</p>
   *
   * <p>Use the ReturnVal returned from this function after this function is
   * called to determine the ultimate success or failure of any operation
   * which returns ReturnVal, because a wizard sequence may determine the
   * ultimate result.</p>
   *
   * <p>This method should not be synchronized, since handleReturnVal
   * may pop up modal (thread-blocking) dialogs, and if we we
   * synchronize this, some Swing or AWT code seems to block on our
   * synchronization when we do pop-up dialogs.  It's not any of my
   * code, so I assume that AWT tries to synchronize on the frame when
   * parenting a new dialog.</p> 
   */

  public ReturnVal handleReturnVal(ReturnVal retVal)
  {
    Hashtable dialogResults;
    DialogRsrc resource;

    /* -- */

    if (debug)
      {
	System.err.println("GASHAdminDispatch.handleReturnVal(): Entering");
      }

    while ((retVal != null) && (retVal.getDialog() != null))
      {
	if (debug)
	  {
	    System.err.println("GASHAdminDispatch.handleReturnVal(): retrieving dialog");
	  }

	JDialogBuff jdialog = retVal.getDialog();

	if (debug)
	  {
	    System.err.println("GASHAdminDispatch.handleReturnVal(): extracting dialog");
	  }

	if (frame == null)
	  {
	    resource = jdialog.extractDialogRsrc(new JFrame());
	  }
	else
	  {
	    resource = jdialog.extractDialogRsrc(frame);
	  }

	if (debug)
	  {
	    System.err.println("GASHAdminDispatch.handleReturnVal(): constructing dialog");
	  }

	StringDialog dialog = new StringDialog(resource);

	if (debug)
	  {
	    System.err.println("GASHAdminDispatch.handleReturnVal(): displaying dialog");
	  }

	// display the Dialog sent to us by the server, get the
	// result of the user's interaction with it.
	    
	dialogResults = dialog.DialogShow();

	if (debug)
	  {
	    System.err.println("GASHAdminDispatch.handleReturnVal(): dialog done");
	  }

	if (retVal.getCallback() != null)
	  {
	    try
	      {
		if (debug)
		  {
		    System.err.println("GASHAdminDispatch.handleReturnVal(): Sending result to callback: " + dialogResults);
		  }

		// send the dialog results to the server

		retVal = retVal.getCallback().respond(dialogResults);

		if (debug)
		  {
		    System.err.println("GASHAdminDispatch.handleReturnVal(): Received result from callback.");
		  }
	      }
	    catch (RemoteException ex)
	      {
		throw new RuntimeException("Caught remote exception: " + ex.getMessage());
	      }
	  }
	else
	  {
	    if (debug)
	      {
		System.err.println("GASHAdminDispatch.handleReturnVal(): No callback, breaking");
	      }

	    break;		// we're done
	  }
      }

    if (debug)
      {
	if (retVal != null)
	  {
	    if (retVal.didSucceed())
	      {
		System.err.println("GASHAdminDispatch.handleReturnVal(): returning success code");
	      }
	    else
	      {
		System.err.println("GASHAdminDispatch.handleReturnVal(): returning failure code");
	      }
	  }
	else
	  {
	    System.err.println("GASHAdminDispatch.handleReturnVal(): returning null retVal (success)");
	  }
      }

    return retVal;
  }
}
