/*
   GASHAdminFrame.java

   Admin console for the Java RMI Gash Server

   Created: 7 September 2003
   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996-2004
   The University of Texas at Austin

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

package arlut.csd.ganymede.admin;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;

import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import arlut.csd.JDataComponent.JFocusRootPanel;
import arlut.csd.JDataComponent.JMultiLineLabel;
import arlut.csd.JDialog.DialogRsrc;
import arlut.csd.JDialog.JCenterDialog;
import arlut.csd.JDialog.StringDialog;
import arlut.csd.JDialog.messageDialog;
import arlut.csd.JTable.rowSelectCallback;
import arlut.csd.JTable.rowTable;
import arlut.csd.Util.PackageResources;

/*------------------------------------------------------------------------------
                                                                           class
                                                                  GASHAdminFrame

------------------------------------------------------------------------------*/

/**
 *
 * GASHAdminFrame is the main class for the Ganymede admin console.  The
 * GASHAdminFrame constructor is the first piece of common code that is executed
 * both in an applet context and as a stand-alone app.
 *
 */

public class GASHAdminFrame extends JFrame implements ActionListener, rowSelectCallback {

  static GASHAdminDispatch adminDispatch = null;
  static GASHSchema schemaEditor = null;
  static final boolean debug = false;
  static String debugFilename = null;

  // ---

  Image question = null;

  JMenuBar mbar = null;
  JMenu controlMenu = null;
  JMenuItem forceBuildMI = null;
  JMenuItem clearLogMI = null;
  JMenuItem quitMI = null;
  JMenuItem dumpMI = null;
  JMenuItem killAllMI = null;
  JMenuItem schemaMI = null;
  JMenuItem shutdownMI = null;

  JMenu debugMenu = null;
  JMenuItem runInvidTestMI = null;
  JMenuItem runInvidSweepMI = null;
  JMenuItem runEmbeddedTestMI = null;
  JMenuItem runEmbeddedSweepMI = null;

  JMenu helpMenu = null;
  JMenuItem showAboutMI = null;

  JPopupMenu popMenu = null;
  JMenuItem killUserMI = null;

  JPanel topPanel = null;

  JTabbedPane tabPane = null;

  StringDialog
    dumpDialog = null;

  consoleShutdownDialog
    shutdownDialog = null;

  String killVictim = null;

  JLabel hostLabel = null;
  JTextField hostField = null;

  JLabel adminLabel = null;
  JTextField adminField = null;

  JLabel stateLabel = null;
  JTextField stateField = null;

  JLabel startLabel = null;
  JTextField startField = null;

  JLabel dumpLabel = null;
  JTextField dumpField = null;

  JLabel journalLabel = null;
  JTextField journalField = null;

  JLabel checkedOutLabel = null;
  JTextField checkedOutField = null;

  JLabel locksLabel = null;
  JTextField locksField = null;

  JLabel memLabel = null;
  JTextField memField = null;

  JTextArea statusArea = null;

  // resources for the users connected table
  
  rowTable table = null;

  String headers[] = {"User", "System", "Status", "Connect Time", "Last Event", "Objects Checked Out"};
  int colWidths[] = {100,100,100,100,100,100};

  // resources for the task monitor table

  rowTable taskTable = null;

  String taskHeaders[] = {"Task", "Status", "Last Run", "Next Run", "Interval"};
  int taskColWidths[] = {100,100,100,100,100};

  JPopupMenu taskPopMenu = null;
  JMenuItem runNowMI = null;
  JMenuItem stopTaskMI = null;
  JMenuItem disableTaskMI = null;
  JMenuItem enableTaskMI = null;
  
  GASHAdmin adminPanel;

  String
    aboutMessage = null;

  messageDialog
    about = null;

  /* -- */

  /**
   *
   * Constructor
   *
   */

  public GASHAdminFrame(String title, GASHAdmin adminPanel)
  {
    super(title);

    this.adminPanel = adminPanel;

    mbar = new JMenuBar();
    controlMenu = new JMenu("Control", false); // no tear-off
    controlMenu.setMnemonic('c');

    clearLogMI = new JMenuItem("Clear Log Panel");
    clearLogMI.setMnemonic('l');
    clearLogMI.addActionListener(this);

    forceBuildMI = new JMenuItem("Force Build");
    forceBuildMI.setMnemonic('f');
    forceBuildMI.addActionListener(this);

    killAllMI = new JMenuItem("Kill Off All Users");
    killAllMI.setMnemonic('k');
    killAllMI.addActionListener(this);

    schemaMI = new JMenuItem("Edit Schema");
    schemaMI.setMnemonic('e');
    schemaMI.addActionListener(this);

    shutdownMI = new JMenuItem("Shutdown Ganymede");
    shutdownMI.setMnemonic('s');
    shutdownMI.addActionListener(this);

    dumpMI = new JMenuItem("Dump Database");
    dumpMI.setMnemonic('d');
    dumpMI.addActionListener(this);

    quitMI = new JMenuItem("Quit Console");
    quitMI.setMnemonic('q');
    quitMI.addActionListener(this);

    controlMenu.add(clearLogMI);
    controlMenu.add(forceBuildMI);
    controlMenu.add(killAllMI);
    controlMenu.add(schemaMI);
    controlMenu.add(shutdownMI);
    controlMenu.addSeparator();
    controlMenu.add(dumpMI);
    controlMenu.addSeparator();
    controlMenu.add(new arlut.csd.JDataComponent.LAFMenu(this)); // ??
    controlMenu.add(quitMI);

    debugMenu = new JMenu("Debug", false); // no tear-off
    debugMenu.setMnemonic('d');

    runInvidTestMI = new JMenuItem("Test Invid Integrity");
    runInvidTestMI.addActionListener(this);

    runInvidSweepMI = new JMenuItem("Repair Invid Integrity");
    runInvidSweepMI.addActionListener(this);

    runEmbeddedTestMI = new JMenuItem("Test Embedded Integrity");
    runEmbeddedTestMI.addActionListener(this);

    runEmbeddedSweepMI = new JMenuItem("Repair Embedded Integrity");
    runEmbeddedSweepMI.addActionListener(this);

    debugMenu.add(runInvidTestMI);
    debugMenu.add(runInvidSweepMI);
    debugMenu.add(runEmbeddedTestMI);
    debugMenu.add(runEmbeddedSweepMI);

    helpMenu = new JMenu("Help");
    helpMenu.setMnemonic('h');
    
    showAboutMI = new JMenuItem("About Ganymede");
    showAboutMI.setMnemonic('a');
    showAboutMI.addActionListener(this);
    helpMenu.add(showAboutMI);

    mbar.add(controlMenu);
    mbar.add(debugMenu);
    mbar.add(Box.createGlue());
    mbar.add(helpMenu);

    setJMenuBar(mbar);

    question = PackageResources.getImageResource(this, "question.gif", getClass());

    java.awt.GridBagLayout topGBL = new java.awt.GridBagLayout();
    java.awt.GridBagConstraints topGBC = new java.awt.GridBagConstraints();

    getContentPane().setLayout(topGBL);

    // set up our top panel, containing a labeled
    // text field showing the server we're connected
    // to.

    /* Ganymede Server Host */

    hostLabel = new JLabel("Ganymede Server Host:");

    hostField = new JTextField(GASHAdmin.url, 40);
    hostField.setEditable(false);

    topPanel = new JPanel();

    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();

    topPanel.setLayout(gbl);

    gbc.insets = new java.awt.Insets(2,1,2,1);

    gbc.anchor = GridBagConstraints.EAST;
    gbc.weightx = 0;
    gbc.fill = GridBagConstraints.NONE;
    gbc.gridwidth = 1;
    gbc.gridheight = 1;
    gbl.setConstraints(hostLabel, gbc);
    topPanel.add(hostLabel);

    gbc.anchor = GridBagConstraints.WEST;

    gbc.weightx = 100;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.gridheight = 1;
    gbl.setConstraints(hostField, gbc);
    topPanel.add(hostField);

    /* Admin consoles connected to server */

    adminLabel = new JLabel("Admin consoles connected to server:");

    adminField = new JTextField("", 40);
    adminField.setEditable(false);

    gbc.anchor = GridBagConstraints.EAST;
    gbc.weightx = 0;
    gbc.fill = GridBagConstraints.NONE;
    gbc.gridwidth = 1;
    gbc.gridheight = 1;
    gbl.setConstraints(adminLabel, gbc);
    topPanel.add(adminLabel);

    gbc.anchor = GridBagConstraints.WEST;

    gbc.weightx = 100;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.gridheight = 1;
    gbl.setConstraints(adminField, gbc);
    topPanel.add(adminField);

    /* Server State */

    stateLabel = new JLabel("Server State:");

    stateField = new JTextField("", 40);
    stateField.setEditable(false);

    gbc.anchor = GridBagConstraints.EAST;
    gbc.weightx = 0;
    gbc.fill = GridBagConstraints.NONE;
    gbc.gridwidth = 1;
    gbc.gridheight = 1;
    gbl.setConstraints(stateLabel, gbc);
    topPanel.add(stateLabel);

    gbc.anchor = GridBagConstraints.WEST;

    gbc.weightx = 100;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.gridheight = 1;
    gbl.setConstraints(stateField, gbc);
    topPanel.add(stateField);

    /* Server Start Time */

    startLabel = new JLabel("Server Start Time:");

    startField = new JTextField("", 40);
    startField.setEditable(false);

    gbc.anchor = GridBagConstraints.EAST;
    gbc.weightx = 0;
    gbc.fill = GridBagConstraints.NONE;
    gbc.gridwidth = 1;
    gbc.gridheight = 1;
    gbl.setConstraints(startLabel, gbc);
    topPanel.add(startLabel);

    gbc.anchor = GridBagConstraints.WEST;

    gbc.weightx = 100;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.gridheight = 1;
    gbl.setConstraints(startField, gbc);
    topPanel.add(startField);

    /* Last Dump Time */

    dumpLabel = new JLabel("Last Dump Time:");

    dumpField = new JTextField("", 40);
    dumpField.setEditable(false);

    gbc.anchor = GridBagConstraints.EAST;
    gbc.weightx = 0;
    gbc.fill = GridBagConstraints.NONE;
    gbc.gridwidth = 1;
    gbc.gridheight = 1;
    gbl.setConstraints(dumpLabel, gbc);
    topPanel.add(dumpLabel);

    gbc.anchor = GridBagConstraints.WEST;

    gbc.weightx = 100;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.gridheight = 1;
    gbl.setConstraints(dumpField, gbc);
    topPanel.add(dumpField);

    /* In-use / Free / Total Memory */

    memLabel = new JLabel("In-use / Free / Total Memory:");

    gbc.anchor = GridBagConstraints.EAST;
    gbc.weightx = 0;
    gbc.fill = GridBagConstraints.NONE;
    gbc.gridwidth = 1;
    gbc.gridheight = 1;
    gbl.setConstraints(memLabel, gbc);
    topPanel.add(memLabel);

    gbc.anchor = GridBagConstraints.WEST;

    memField = new JTextField("", 40);
    memField.setEditable(false);

    gbc.weightx = 100;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.gridheight = 1;
    gbl.setConstraints(memField, gbc);
    topPanel.add(memField);

    /* Transactions in Journal */

    journalLabel = new JLabel("Transactions in Journal:");

    journalField = new JTextField("", 40);
    journalField.setEditable(false);

    gbc.anchor = GridBagConstraints.EAST;
    gbc.weightx = 0;
    gbc.fill = GridBagConstraints.NONE;
    gbc.gridwidth = 1;
    gbc.gridheight = 1;
    gbl.setConstraints(journalLabel, gbc);
    topPanel.add(journalLabel);

    gbc.anchor = GridBagConstraints.WEST;

    gbc.weightx = 100;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.gridheight = 1;
    gbl.setConstraints(journalField, gbc);
    topPanel.add(journalField);

    /* Objects Checked Out */

    checkedOutLabel = new JLabel("Objects Checked Out:");

    checkedOutField = new JTextField("", 40);
    checkedOutField.setEditable(false);

    gbc.anchor = GridBagConstraints.EAST;
    gbc.weightx = 0;
    gbc.fill = GridBagConstraints.NONE;
    gbc.gridwidth = 1;
    gbc.gridheight = 1;
    gbl.setConstraints(checkedOutLabel, gbc);
    topPanel.add(checkedOutLabel);

    gbc.anchor = GridBagConstraints.WEST;

    gbc.weightx = 100;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.gridheight = 1;
    gbl.setConstraints(checkedOutField, gbc);
    topPanel.add(checkedOutField);

    /* Locks held */

    locksLabel = new JLabel("Locks held:");

    locksField = new JTextField("", 40);
    locksField.setEditable(false);

    gbc.anchor = GridBagConstraints.EAST;
    gbc.weightx = 0;
    gbc.fill = GridBagConstraints.NONE;
    gbc.gridwidth = 1;
    gbc.gridheight = 1;
    gbl.setConstraints(locksLabel, gbc);
    topPanel.add(locksLabel);

    gbc.anchor = GridBagConstraints.WEST;

    gbc.weightx = 100;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.gridheight = 1;
    gbl.setConstraints(locksField, gbc);
    topPanel.add(locksField);

    topGBC.fill = GridBagConstraints.HORIZONTAL;
    topGBC.gridwidth = GridBagConstraints.REMAINDER;
    topGBC.gridheight = 1;
    topGBC.gridx = 0;
    topGBC.weightx = 1.0;
    
    JPanel topBox = new JPanel(new BorderLayout());
    topBox.add("Center",topPanel);
    topBox.setBorder(new TitledBorder("Ganymede Server"));
    topGBL.setConstraints(topBox, topGBC);
    getContentPane().add(topBox);

    // set up our middle text area

    topGBC.fill = GridBagConstraints.BOTH;
    topGBC.weighty = 1.0;

    statusArea = new JTextArea("Ganymede Admin Console\n", 6, 50);
    statusArea.setEditable(false);
    JScrollPane statusAreaPane = new JScrollPane(statusArea);

    JPanel statusBox = new JPanel(new java.awt.BorderLayout());
    statusBox.add("Center", statusAreaPane);
    statusBox.setBorder(new TitledBorder("Server Log"));

    //    topGBL.setConstraints(statusBox, topGBC);
    //    getContentPane().add(statusBox);

    // bottom area, a tab pane with tables for things

    // create our user table

    popMenu = new JPopupMenu();

    killUserMI = new JMenuItem("Kill User");
    popMenu.add(killUserMI);

    table = new rowTable(colWidths, headers, this, false, popMenu, false);
    JPanel tableBox = new JPanel(new BorderLayout());
    tableBox.add("Center", table);
    tableBox.setBorder(new TitledBorder("Users Connected"));

    // create background task monitor

    taskPopMenu = new JPopupMenu();

    runNowMI = new JMenuItem("Run Task Now");
    stopTaskMI = new JMenuItem("Stop Running Task");
    disableTaskMI = new JMenuItem("Disable Task");
    enableTaskMI = new JMenuItem("Enable Task");

    taskPopMenu.add(runNowMI);
    taskPopMenu.add(stopTaskMI);
    taskPopMenu.add(disableTaskMI);
    taskPopMenu.add(enableTaskMI);

    taskTable = new rowTable(taskColWidths, taskHeaders, this, false, taskPopMenu, false);
    taskTable.setHeadBackColor(Color.red, false);

    JPanel taskBox = new JPanel(new java.awt.BorderLayout());
    taskBox.add("Center", taskTable);
    taskBox.setBorder(new TitledBorder("Task Monitor"));

    // and put them into our tab pane

    tabPane = new JTabbedPane();
    tabPane.addTab("Users Connected", tableBox);
    tabPane.addTab("Task Monitor", taskBox);

    // and put the tab pane into our frame with the
    // same constraints that the text area had

    //    topGBL.setConstraints(tabPane, topGBC);
    //    getContentPane().add(tabPane);

    JSplitPane splitterPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, statusBox, tabPane);
    splitterPane.setOneTouchExpandable(true);
    splitterPane.setDividerLocation(0.5);

    topGBL.setConstraints(splitterPane, topGBC);
    getContentPane().add(splitterPane);

    pack();
    this.setVisible(true);

    // along with processWindowEvent(), this method allows us
    // to properly handle window system close events.

    enableEvents(AWTEvent.WINDOW_EVENT_MASK);

    if (debugFilename != null)
      {
	try
	  {
	    RemoteServer.setLog(new FileOutputStream(debugFilename));
	  }
	catch (IOException ex)
	  {
	    System.err.println("couldn't open RMI debug log: " + ex);
	  }
      }
  }

  public void setDispatch(GASHAdminDispatch ad)
  {
    GASHAdminFrame.adminDispatch = ad;
  }

  /**
   * local convenience method to handle disconnecting the admin console
   */

  void disconnect()
  {
    try
      {
	adminDispatch.disconnect();
      }
    catch (RemoteException ex)
      {
	System.err.println("Couldn't logout cleanly: " + ex);
      }
    finally
      {
	adminPanel.quitButton.setEnabled(true);
	adminPanel.loginButton.setEnabled(true);
	setVisible(false);

	// This shouldn't kill everything off, but it does for now.  Need to fix this later.
	    
	if (!GASHAdmin.WeAreApplet)
	  {
	    System.exit(0);
	  }
      }
  }

  // our button / dialog handler

  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == forceBuildMI)
      {
	try
	  {
	    adminDispatch.forceBuild();
	  }
	catch (RemoteException ex)
	  {
	    exceptionHandler(ex);
	  }
      }
    else if (event.getSource() == quitMI)
      {
	if (debug)
	  {
	    System.err.println("Quitting");
	  }

	this.disconnect();
      }
    else if (event.getSource() == dumpMI)
      {
	if (dumpDialog == null)
	  {
	    dumpDialog = new StringDialog(this,
					  "Ganymede Server Dump",
					  "Are you sure you want to schedule \na full dump of the Ganymede database?", 
					  "Yes", "No", question);
	  }

	if (dumpDialog.DialogShow() != null)
	  {
	    if (debug)
	      {
		System.err.println("Affirmative dump request");
	      }

	    try
	      {
		adminDispatch.dumpDB();
	      }
	    catch (RemoteException ex)
	      {
		exceptionHandler(ex);
	      }
	  }
      }
    else if (event.getSource() == runInvidTestMI)
      {
	StringDialog invidTestDialog = new StringDialog(this,
							"Invid Test",
							"Are you sure you want to trigger a full invid consistency test?\n"+
							"It may take awhile.",
							"Yes", "No", question);

	if (invidTestDialog.DialogShow() != null)
	  {
	    if (debug)
	      {
		System.err.println("Affirmative invid test request");
	      }

	    try
	      {
		adminDispatch.runInvidTest();
	      }
	    catch (RemoteException ex)
	      {
		exceptionHandler(ex);
	      }
	  }
      }
    else if (event.getSource() == runInvidSweepMI)
      {
	StringDialog invidTestDialog = new StringDialog(this,
							"Invid Sweep",
							"Are you sure you want to trigger a full invid sweep?\n"+
							"It may take awhile.",
							"Yes", "No", question);

	if (invidTestDialog.DialogShow() != null)
	  {
	    if (debug)
	      {
		System.err.println("Affirmative invid sweep request");
	      }

	    try
	      {
		adminDispatch.runInvidSweep();
	      }
	    catch (RemoteException ex)
	      {
		exceptionHandler(ex);
	      }
	  }
      }
    else if (event.getSource() == runEmbeddedTestMI)
      {
	StringDialog invidTestDialog = new StringDialog(this,
							"Embedded Object Test",
							"Are you sure you want to trigger a full embedded object consistency test?",
							"Yes", "No", question);

	if (invidTestDialog.DialogShow() != null)
	  {
	    if (debug)
	      {
		System.err.println("Affirmative Embedded test request");
	      }

	    try
	      {
		adminDispatch.runEmbeddedTest();
	      }
	    catch (RemoteException ex)
	      {
		exceptionHandler(ex);
	      }
	  }
      }
    else if (event.getSource() == runEmbeddedSweepMI)
      {
	StringDialog invidTestDialog = new StringDialog(this,
							"Embedded Object Sweep",
							"Are you sure you want to trigger a full embedded object consistency sweep?",
							"Yes", "No", question);

	if (invidTestDialog.DialogShow() != null)
	  {
	    if (debug)
	      {
		System.err.println("Affirmative Embedded Sweep request");
	      }

	    try
	      {
		adminDispatch.runEmbeddedSweep();
	      }
	    catch (RemoteException ex)
	      {
		exceptionHandler(ex);
	      }
	  }
      }
    else if (event.getSource() == shutdownMI)
      {
	boolean waitForUsers=false;

	shutdownDialog = new consoleShutdownDialog(this);

	int result = shutdownDialog.DialogShow();

	if (result == 0)
	  {
	    return;
	  }

	if (result == 2)
	  {
	    waitForUsers = true;
	  }

	boolean success = true;

	try
	  {
	    success = adminDispatch.shutdown(waitForUsers);
	  }
	catch (RemoteException ex)
	  {
	    adminDispatch.forceDisconnect("Couldn't talk to server" + ex);
	  }

	// if we are going to delay shutting down until all users log
	// out, don't close down the admin console.  We don't
	// currently provide a way to clear the
	// shutdown-on-users-logged-out, but it's still useful to be
	// able to continue to monitor things while we're waiting for
	// users to trickle off.

	if (!waitForUsers && success)
	  {
	    adminPanel.quitButton.setEnabled(true);
	    adminPanel.loginButton.setEnabled(true);
	    setVisible(false);
		
	    if (!GASHAdmin.WeAreApplet)
	      {
		System.exit(0);
	      }
	  }
      }
    else if (event.getSource() == killAllMI)
      {
	DialogRsrc killAllDLGR;
	StringDialog killAllDLG;

	killAllDLGR = new DialogRsrc(this,
				     "Are you sure you want to log out all users?", 
				     "Are you sure you want to log out all users?", 
				     "Yes", "No", question);
    
	killAllDLG = new StringDialog(killAllDLGR);
	
	if (killAllDLG.DialogShow() == null)
	  {
	    return;
	  }

	try
	  {
	    adminDispatch.killAll();
	  }
	catch (RemoteException ex)
	  {
	    exceptionHandler(ex);
	  }
      }
    else if (event.getSource() == schemaMI)
      {
	if (GASHAdminFrame.schemaEditor != null)
	  {
	    return;
	  }

	schemaMI.setEnabled(false);

	try
	  {
	    GASHAdminFrame.schemaEditor = adminDispatch.pullSchema();
	  }
	catch (RemoteException ex)
	  {
	    exceptionHandler(ex);
	  }
      }
    else if (event.getSource() == showAboutMI)
      {
	showAboutMessage();
      }
    else if (event.getSource() == clearLogMI)
      {
	statusArea.setText("");
      }
  }

  /**
   * Shows the About... dialog.
   */

  public void showAboutMessage()
  {
    if (about == null)
      {
	if (aboutMessage == null)
	  {
	    StringBuffer buffer = new StringBuffer();

	    buffer.append("<head></head>");
	    buffer.append("<body>");
	    buffer.append("<h1>Ganymede Directory Management System</h1><p>");
	    buffer.append("<p>Release number: ");
	    buffer.append(arlut.csd.Util.SVNVersion.getReleaseString());
	    buffer.append("<br>Copyright (C) 1996, 1997, 1998, 1999, 2000, 2001, 2002, 2003, 2004<br>The University of Texas at Austin</p>");
	    buffer.append("<p>Ganymede is licensed and distributed under the GNU General Public License ");
	    buffer.append("and comes with ABSOLUTELY NO WARRANTY.</p>");
	    buffer.append("<p>This is free software, and you are welcome to redistribute it ");
	    buffer.append("under the conditions of the GNU General Public License.</p>");
	    buffer.append("<p>Written by Jonathan Abbey, Michael Mulvaney, Navin Manohar, ");
	    buffer.append("Brian O'Mara, and Erik Grostic.</p>");
	    buffer.append("<br><p>Visit the Ganymede web site at http://www.arlut.utexas.edu/gash2/</p>");
	    buffer.append("</body>");

	    aboutMessage = buffer.toString();
	  }

	about = new messageDialog(this, "About Ganymede",  null);
	about.setHtmlText(aboutMessage);
      }

    about.setVisible(true);
  }

  // methods for the rowSelectCallback

  public void rowSelected(Object key)
  {
  }

  public void rowDoubleSelected(Object key)
  {
  }

  public void rowUnSelected(Object key, boolean finalState)
  {
  }

  public void rowMenuPerformed(Object key, ActionEvent e)
  {
    if (debug)
      {
	System.err.println("rowMenuPerformed");
      }

    if (e.getSource() == killUserMI)
      {
	if (debug)
	  {
	    System.err.println("kill " + key + " selected");
	  }

	killVictim = (String) key;

	if (new StringDialog(this,
			     "Confirm User Kill",
			     "Are you sure you want to disconnect user " + key + "?",
			     "Yes", "No", question).DialogShow() != null)
	  {
	    if (debug)
	      {
		System.err.println("Affirmative kill request");
	      }

	    if (killVictim != null)
	      {
		try
		  {
		    adminDispatch.kill(killVictim);
		  }
		catch (RemoteException ex)
		  {
		    exceptionHandler(ex);
		  }
	      }
	    killVictim = null;
	  }
	else
	  {
	    if (debug)
	      {
		System.err.println("Negative kill request");
	      }
	    killVictim = null;
	  }
      }
    else if (e.getSource() == runNowMI)
      {
	try
	  {
	    adminDispatch.runTaskNow((String) key);
	  }
	catch (RemoteException ex)
	  {
	    exceptionHandler(ex);
	  }
      }
    else if (e.getSource() == stopTaskMI)
      {
	try
	  {
	    adminDispatch.stopTask((String) key);
	  }
	catch (RemoteException ex)
	  {
	    exceptionHandler(ex);
	  }
      }
    else if (e.getSource() == disableTaskMI)
      {
	try
	  {
	    adminDispatch.disableTask((String) key);
	  }
	catch (RemoteException ex)
	  {
	    exceptionHandler(ex);
	  }
      }
    else if (e.getSource() == enableTaskMI)
      {
	try
	  {
	    adminDispatch.enableTask((String) key);
	  }
	catch (RemoteException ex)
	  {
	    exceptionHandler(ex);
	  }
      }
  }

  /**
   * <P>Method to handle properly logging out if the main admin
   * frame is closed by the window system.</P>
   *
   * <P>We do an enableEvents(AWT.WINDOW_EVENT_MASK) in the
   * GASHAdminFrame constructor to activate this method.</P>
   */

  protected void processWindowEvent(WindowEvent e) 
  {
    if (e.getID() == WindowEvent.WINDOW_CLOSING)
      {
	// make sure that we cancel any schema editing in process if
	// we have it open and we are made to close the main window

	if (GASHAdminFrame.schemaEditor != null)
	  {
	    GASHAdminFrame.schemaEditor.cancel();
	  }

	disconnect();
      }
	
    super.processWindowEvent(e);
  }

  private void exceptionHandler(Throwable ex)
  {
    adminDispatch.changeStatus("******************** " +
			       "Error occurred while communicating with the server " +
			       "********************\n");
    StringWriter stringTarget = new StringWriter();
    PrintWriter writer = new PrintWriter(stringTarget);
    
    ex.printStackTrace(writer);
    writer.close();

    adminDispatch.changeStatus(stringTarget.toString());

    adminDispatch.changeStatus("****************************************" +
			       "****************************************\n");
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                           consoleShutdownDialog

------------------------------------------------------------------------------*/

/**
 * <P>GUI dialog for presenting server shutdown options in the admin console.</P>
 */

class consoleShutdownDialog extends JCenterDialog implements ActionListener, WindowListener {

  private final static boolean debug = false;

  GridBagLayout
    gbl;
  
  GridBagConstraints
    gbc;

  JButton
    now, later, cancel;

  JPanel
    mainPanel, imagePanel, buttonPanel;

  JMultiLineLabel 
    textLabel;

  Image image;

  JLabel
    imageCanvas;

  String body = "Shut down the Ganymede server?";
  String buttonText[] = {"Yes, Immediately",
			 "Yes, When Users Log Off",
			 "No, Cancel"};

  JButton
    button1, button2, button3;

  private int result = 0;
  private boolean done = false;

  /* -- */

  public consoleShutdownDialog(Frame frame)
  {
    super(frame, "Confirm Ganymede Server Shutdown?", true);

    this.addWindowListener(this);

    gbl = new GridBagLayout();
    gbc = new GridBagConstraints();

    gbc.insets = new Insets(4,4,4,4);

    mainPanel = new JPanel();
    mainPanel.setBorder(new CompoundBorder(new EtchedBorder(),
					   new EmptyBorder(10, 10, 10, 10)));
    mainPanel.setLayout(gbl);
    setContentPane(mainPanel);

    //
    // Title at top of dialog
    //

    JLabel titleLabel = new JLabel("Confirm Ganymede Server Shutdown?", SwingConstants.CENTER);
    titleLabel.setFont(new Font("Helvetica", Font.BOLD, 14));

    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.gridwidth = 2;
    gbc.gridheight = 1;
    gbl.setConstraints(titleLabel, gbc);
    mainPanel.add(titleLabel);

    //
    // Text message under title
    //

    textLabel = new JMultiLineLabel(body);
    
    gbc.gridy = 1;
    gbc.gridx = 1;
    gbc.gridwidth = 1;
    gbc.gridheight = 1;
    gbl.setConstraints(textLabel, gbc);
    mainPanel.add(textLabel);

    //
    // Separator goes all the way accross
    // 

    JSeparator sep = new JSeparator();

    gbc.gridx = 0;
    gbc.gridy = 3;
    gbc.gridwidth = 2;
    gbc.gridheight = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbl.setConstraints(sep, gbc);
    mainPanel.add(sep);

    //
    // ButtonPanel takes up the bottom of the dialog
    //

    buttonPanel = new JFocusRootPanel();

    button1 = new JButton(buttonText[0]);
    button1.addActionListener(this);
    buttonPanel.add(button1);

    button2 = new JButton(buttonText[1]);
    button2.addActionListener(this);
    buttonPanel.add(button2);

    button3 = new JButton(buttonText[2]);
    button3.addActionListener(this);
    buttonPanel.add(button3);

    //
    // buttonPanel goes underneath
    //

    gbc.gridx = 0;
    gbc.gridy = 4;
    gbc.gridwidth = 2;
    gbc.gridheight = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbl.setConstraints(buttonPanel, gbc);
    mainPanel.add(buttonPanel);

    //
    // Image on left hand side
    //

    image = arlut.csd.Util.PackageResources.getImageResource(frame, "question.gif", frame.getClass());
    imagePanel = new JPanel();

    if (image != null)
      {
	imageCanvas = new JLabel(new ImageIcon(image));
	imagePanel.add(imageCanvas);
      }
    else
      {
	imagePanel.add(Box.createGlue());
      }

    gbc.gridx = 0;
    gbc.gridy = 1;
    gbc.gridwidth = 1;
    gbc.gridheight = 2;
    gbc.weightx = 0.0;
    gbc.fill = GridBagConstraints.NONE;
    gbl.setConstraints(imagePanel, gbc);
    mainPanel.add(imagePanel);

    pack();
  }

  /**
   * <p>Display the dialog box, locks this thread while the dialog is being
   * displayed, and returns a hashtable of data field values when the
   * user closes the dialog box.</p>
   *
   * <p>Use this instead of Dialog.show().  If Hashtable returned is null,
   * then the cancel button was clicked.  Otherwise, it will contain a 
   * hash of labels(String) to results (Object).</p>
   *
   * @return HashTable of labels to values
   */

  public int DialogShow()
  {
    mainPanel.revalidate();
    this.setVisible(true);

    // at this point we're frozen, since we're a modal dialog.. we'll continue
    // at this point when the ok or cancel buttons are pressed.

    if (debug)
      {
	System.err.println("Done invoking.");
      }

    return result;
  }

  public synchronized void actionPerformed(ActionEvent e)
  {
    if (e.getSource() == button1)
      {
	result = 1;
      }
    else if (e.getSource() == button2)
      {
	result = 2;
      }
    else if (e.getSource() == button3)
      {
	result = 0;
      }
    else
      {
	return;
      }

    // pop down so that DialogShow() can proceed to completion.

    done = true;

    setVisible(false);
  }

  // WindowListener methods

  public void windowActivated(WindowEvent event)
  {
  }

  public void windowClosed(WindowEvent event)
  {
  }

  public synchronized void windowClosing(WindowEvent event)
  {
    if (!done)
      {
	if (debug)
	  {
	    System.err.println("Window is closing and we haven't done a cancel.");
	  }

	// by setting valueHash to null, we're basically treating
	// this window close as a cancel.
	
	result = 0;
      }

    done = true;

    this.setVisible(false);
  }

  public void windowDeactivated(WindowEvent event)
  {
  }

  public void windowDeiconified(WindowEvent event)
  {
  }

  public void windowIconified(WindowEvent event)
  {
  }

  public void windowOpened(WindowEvent event)
  {
    button3.requestFocus();
  }
}
