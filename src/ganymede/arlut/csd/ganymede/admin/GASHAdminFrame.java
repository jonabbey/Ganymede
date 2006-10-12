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
 
   Copyright (C) 1996-2006
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
import java.util.prefs.Preferences;

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

import arlut.csd.ganymede.common.windowSizer;
import arlut.csd.JDataComponent.JFocusRootPanel;
import arlut.csd.JDataComponent.JMultiLineLabel;
import arlut.csd.JDialog.DialogRsrc;
import arlut.csd.JDialog.JCenterDialog;
import arlut.csd.JDialog.StringDialog;
import arlut.csd.JDialog.messageDialog;
import arlut.csd.JDialog.aboutGanyDialog;
import arlut.csd.JTable.rowSelectCallback;
import arlut.csd.JTable.rowTable;
import arlut.csd.Util.PackageResources;
import arlut.csd.Util.TranslationService;

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

  /**
   * TranslationService object for handling string localization in
   * the Ganymede admin console.
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.admin.GASHAdminFrame");

  /**
   * Preferences object for the Ganymede admin console.  Using this
   * object, we can save and retrieve preferences data from a
   * system-dependent backing-store.. the Registry on Windows, a XML
   * file under ~/.java/user-prefs/ on Linux/Unix/Mac, and
   * who-knows-what on other platforms.
   */

  public static final Preferences prefs = Preferences.userNodeForPackage(GASHAdminFrame.class);
  public static final windowSizer sizer = new windowSizer(prefs);

  static final String SPLITTER_POS = "admin_splitter_pos";

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
  JMenuItem showCreditsMI = null;

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

  String headers[] = {ts.l("global.user_col_0"), // "User"
		      ts.l("global.user_col_1"), // "System"
		      ts.l("global.user_col_2"), // "Status"
		      ts.l("global.user_col_3"), // "Connect Time"
		      ts.l("global.user_col_4"), // "Last Event"
		      ts.l("global.user_col_5")}; // "Objects Checked Out"

  int colWidths[] = {100,100,100,100,100,100};

  // resources for the sync task monitor table

  rowTable syncTaskTable = null;

  String syncTaskHeaders[] = {ts.l("global.task_col_0"), // "Task"
			      ts.l("global.task_col_1"), // "Status"
			      ts.l("global.task_col_2"), // "Last Run"
			      ts.l("global.task_col_3"), // "Next Run"
			      ts.l("global.task_col_4")}; // "Interval"
  int syncTaskColWidths[] = {100,100,100,100,100};

  // resources for the task monitor table

  rowTable taskTable = null;

  String taskHeaders[] = {ts.l("global.task_col_0"), // "Task"
			  ts.l("global.task_col_1"), // "Status"
			  ts.l("global.task_col_2"), // "Last Run"
			  ts.l("global.task_col_3"), // "Next Run"
			  ts.l("global.task_col_4")}; // "Interval"
  int taskColWidths[] = {100,100,100,100,100};

  JPopupMenu taskPopMenu = null;
  JMenuItem runNowMI = null;
  JMenuItem stopTaskMI = null;
  JMenuItem disableTaskMI = null;
  JMenuItem enableTaskMI = null;
  JSplitPane splitterPane = null;
  
  GASHAdmin loginPanel;

  String
    aboutMessage = null;

  aboutGanyDialog about = null;

  /* -- */

  /**
   *
   * Constructor
   *
   */

  public GASHAdminFrame(String title, GASHAdmin loginPanel)
  {
    super(title);

    this.loginPanel = loginPanel;

    mbar = new JMenuBar();

    // "Control"
    controlMenu = new JMenu(ts.l("init.control_menu"), false); // no tear-off

    if (ts.hasPattern("init.control_menu_key_optional"))
      {
	controlMenu.setMnemonic((int) ts.l("init.control_menu_key_optional").charAt(0));
      }

    // "Clear Log Panel"
    clearLogMI = new JMenuItem(ts.l("init.control_menu_0"));

    if (ts.hasPattern("init.control_menu_0_key_optional"))
      {
	clearLogMI.setMnemonic((int) ts.l("init.control_menu_0_key_optional").charAt(0));
      }

    clearLogMI.addActionListener(this);

    // "Force Build"
    forceBuildMI = new JMenuItem(ts.l("init.control_menu_1"));

    if (ts.hasPattern("init.control_menu_1_key_optional"))
      {
	forceBuildMI.setMnemonic((int) ts.l("init.control_menu_1_key_optional").charAt(0));
      }

    forceBuildMI.setMnemonic('f');
    forceBuildMI.addActionListener(this);

    // "Kill Off All Users"
    killAllMI = new JMenuItem(ts.l("init.control_menu_2"));

    if (ts.hasPattern("init.control_menu_2_key_optional"))
      {
	killAllMI.setMnemonic((int) ts.l("init.control_menu_2_key_optional").charAt(0));
      }

    killAllMI.addActionListener(this);

    // "Edit Schema"
    schemaMI = new JMenuItem(ts.l("init.control_menu_3"));

    if (ts.hasPattern("init.control_menu_3_key_optional"))
      {
	schemaMI.setMnemonic((int) ts.l("init.control_menu_3_key_optional").charAt(0));
      }

    schemaMI.addActionListener(this);

    // "Shutdown Ganymede"
    shutdownMI = new JMenuItem(ts.l("init.control_menu_4"));

    if (ts.hasPattern("init.control_menu_4_key_optional"))
      {
	shutdownMI.setMnemonic((int) ts.l("init.control_menu_4_key_optional").charAt(0));
      }

    shutdownMI.addActionListener(this);

    // "Dump Database"
    dumpMI = new JMenuItem(ts.l("init.control_menu_5"));

    if (ts.hasPattern("init.control_menu_5_key_optional"))
      {
	dumpMI.setMnemonic((int) ts.l("init.control_menu_5_key_optional").charAt(0));
      }

    dumpMI.addActionListener(this);

    // "Quit Console"
    quitMI = new JMenuItem(ts.l("init.control_menu_6"));

    if (ts.hasPattern("init.control_menu_6_key_optional"))
      {
	quitMI.setMnemonic((int) ts.l("init.control_menu_6_key_optional").charAt(0));
      }

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

    // "Debug"
    debugMenu = new JMenu(ts.l("init.debug_menu"), false); // no tear-off

    if (ts.hasPattern("init.debug_menu_key_optional"))
      {
	debugMenu.setMnemonic((int) ts.l("init.debug_menu_key_optional").charAt(0));
      }

    // "Test Invid Integrity"
    runInvidTestMI = new JMenuItem(ts.l("init.debug_menu_0"));

    if (ts.hasPattern("init.debug_menu_0_key_optional"))
      {
	runInvidTestMI.setMnemonic((int) ts.l("init.debug_menu_0_key_optional").charAt(0));
      }

    runInvidTestMI.addActionListener(this);

    // "Repair Invid Integrity"
    runInvidSweepMI = new JMenuItem(ts.l("init.debug_menu_1"));

    if (ts.hasPattern("init.debug_menu_1_key_optional"))
      {
	runInvidSweepMI.setMnemonic((int) ts.l("init.debug_menu_1_key_optional").charAt(0));
      }

    runInvidSweepMI.addActionListener(this);

    // "Test Embedded Integrity"
    runEmbeddedTestMI = new JMenuItem(ts.l("init.debug_menu_2"));

    if (ts.hasPattern("init.debug_menu_2_key_optional"))
      {
	runEmbeddedTestMI.setMnemonic((int) ts.l("init.debug_menu_2_key_optional").charAt(0));
      }

    runEmbeddedTestMI.addActionListener(this);

    // "Repair Embedded Integrity"
    runEmbeddedSweepMI = new JMenuItem(ts.l("init.debug_menu_3"));

    if (ts.hasPattern("init.debug_menu_3_key_optional"))
      {
	runEmbeddedSweepMI.setMnemonic((int) ts.l("init.debug_menu_3_key_optional").charAt(0));
      }

    runEmbeddedSweepMI.addActionListener(this);

    debugMenu.add(runInvidTestMI);
    debugMenu.add(runInvidSweepMI);
    debugMenu.add(runEmbeddedTestMI);
    debugMenu.add(runEmbeddedSweepMI);

    // "Help"
    helpMenu = new JMenu(ts.l("init.help_menu"));

    if (ts.hasPattern("init.help_menu_key_optional"))
      {
	helpMenu.setMnemonic((int) ts.l("init.help_menu_key_optional").charAt(0));
      }

    // "About Ganymede"
    showAboutMI = new JMenuItem(ts.l("init.help_menu_0"));

    if (ts.hasPattern("init.help_menu_0_key_optional"))
      {
	showAboutMI.setMnemonic((int) ts.l("init.help_menu_0_key_optional").charAt(0));
      }

    showAboutMI.addActionListener(this);
    helpMenu.add(showAboutMI);

    // "Ganymede Credits"
    showCreditsMI = new JMenuItem(ts.l("init.help_menu_1"));

    if (ts.hasPattern("init.help_menu_1_key_optional"))
      {
	showCreditsMI.setMnemonic((int) ts.l("init.help_menu_1_key_optional").charAt(0));
      }

    showCreditsMI.addActionListener(this);
    helpMenu.add(showCreditsMI);

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

    // "Ganymede Server Host:"

    hostLabel = new JLabel(ts.l("init.hostlabel"));

    if (loginPanel.isSSL())
      {
	// "{0}  [SSL]"
	hostField = new JTextField(ts.l("init.urlssl", GASHAdmin.url), 60);
      }
    else
      {
	// "{0}  [NO SSL]"
	hostField = new JTextField(ts.l("init.urlnossl", GASHAdmin.url), 60);
      }

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

    // "Admin consoles connected to server:"
    adminLabel = new JLabel(ts.l("init.console_count"));

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

    // "Server State:"
    stateLabel = new JLabel(ts.l("init.server_state"));

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

    // "Server Start Time:"
    startLabel = new JLabel(ts.l("init.start_time"));

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

    // "Last Dump Time:"
    dumpLabel = new JLabel(ts.l("init.lastdump"));

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

    // "In-use / Free / Total Memory:"
    memLabel = new JLabel(ts.l("init.memory"));

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

    // "Transactions in Journal:"
    journalLabel = new JLabel(ts.l("init.trans_count"));

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

    // "Objects Checked Out:"
    checkedOutLabel = new JLabel(ts.l("init.objects_out"));

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

    // "Locks Held:"
    locksLabel = new JLabel(ts.l("init.locks"));

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
    // "Ganymede Server"
    topBox.setBorder(new TitledBorder(ts.l("init.title")));
    topGBL.setConstraints(topBox, topGBC);
    getContentPane().add(topBox);

    // set up our middle text area

    topGBC.fill = GridBagConstraints.BOTH;
    topGBC.weighty = 1.0;

    // "Ganymede Admin Console\n"
    statusArea = new JTextArea(ts.l("init.start_log_msg"), 6, 50);
    statusArea.setEditable(false);
    JScrollPane statusAreaPane = new JScrollPane(statusArea);

    JPanel statusBox = new JPanel(new java.awt.BorderLayout());
    statusBox.add("Center", statusAreaPane);

    // "Server Log"
    statusBox.setBorder(new TitledBorder(ts.l("init.server_log_title")));

    //    topGBL.setConstraints(statusBox, topGBC);
    //    getContentPane().add(statusBox);

    // bottom area, a tab pane with tables for things

    // create our user table

    popMenu = new JPopupMenu();

    // "Kill User"
    killUserMI = new JMenuItem(ts.l("init.killUserPopup"));
    popMenu.add(killUserMI);

    table = new rowTable(colWidths, headers, this, false, popMenu, false);
    JPanel tableBox = new JPanel(new BorderLayout());
    tableBox.add("Center", table);

    // "Users Connected"
    tableBox.setBorder(new TitledBorder(ts.l("init.users_title")));

    //
    // create background task monitor
    //

    taskPopMenu = new JPopupMenu();

    // "Run Task Now"
    runNowMI = new JMenuItem(ts.l("init.runNowPopup"));

    // "Stop Running Task"
    stopTaskMI = new JMenuItem(ts.l("init.stopTaskPopup"));

    // "Disable Task"
    disableTaskMI = new JMenuItem(ts.l("init.disableTaskPopup"));

    // "Enable Task"
    enableTaskMI = new JMenuItem(ts.l("init.enableTaskPopup"));

    taskPopMenu.add(runNowMI);
    taskPopMenu.add(stopTaskMI);
    taskPopMenu.add(disableTaskMI);
    taskPopMenu.add(enableTaskMI);

    // first the sync monitor

    syncTaskTable = new rowTable(syncTaskColWidths, syncTaskHeaders, this, false, taskPopMenu, false);

    // 0b5a0e
    syncTaskTable.setHeadBackColor(new java.awt.Color(7,212,16), false);

    JPanel syncTaskBox = new JPanel(new java.awt.BorderLayout());
    syncTaskBox.add("Center", syncTaskTable);
    syncTaskBox.setBorder(new TitledBorder(ts.l("init.sync_title")));

    // then the miscellaneous task monitor

    taskTable = new rowTable(taskColWidths, taskHeaders, this, false, taskPopMenu, false);
    taskTable.setHeadBackColor(Color.red, false);
			  
    JPanel taskBox = new JPanel(new java.awt.BorderLayout());
    taskBox.add("Center", taskTable);

    // "Task Monitor"
    taskBox.setBorder(new TitledBorder(ts.l("init.task_title")));

    // and put them into our tab pane

    tabPane = new JTabbedPane();

    // "Users Connected"
    tabPane.addTab(ts.l("init.users_title"), tableBox);

    // "Sync Monitor"
    tabPane.addTab(ts.l("init.sync_title"), syncTaskBox);

    // "Task Monitor"
    tabPane.addTab(ts.l("init.task_title"), taskBox);

    // and put the tab pane into our frame with the
    // same constraints that the text area had

    //    topGBL.setConstraints(tabPane, topGBC);
    //    getContentPane().add(tabPane);

    splitterPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, statusBox, tabPane);
    splitterPane.setOneTouchExpandable(true);

    splitterPane.setDividerLocation(0.75);

    // we want the top component, our log panel, to get almost all of
    // the extra size when we are resized

    splitterPane.setResizeWeight(0.85); 

    topGBL.setConstraints(splitterPane, topGBC);
    getContentPane().add(splitterPane);

    pack();

    if (!sizer.restoreSize(this))
      {
	this.setLocationRelativeTo(null); // center frame
	sizer.saveSize(this);	// save an initial size before the user might maximize
      }

    int splitterPos = prefs.getInt(SPLITTER_POS, -1);

    this.setVisible(true);

    if (splitterPos != -1)
      {
	splitterPane.setDividerLocation(splitterPos);

        System.err.println("Setting divider location to " + splitterPos);
      }

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
	if (loginPanel.quitButton != null)
	  {
	    loginPanel.quitButton.setEnabled(true);
	  }

	loginPanel.loginButton.setEnabled(true);
	saveWindowPrefs();
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
	    // "Ganymede Server Dump"
	    // "Are you sure you want to schedule\na full dump of the Ganymede database to disk?"

	    dumpDialog = new StringDialog(this,
					  ts.l("actionPerformed.dump_title"),
					  ts.l("actionPerformed.dump_question"),
					  ts.l("global.yes"), ts.l("global.no"), question);
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
	// "Invid Test"
	// "Are you sure you want to trigger a full Invid consistency test?\nIt may take awhile."

	StringDialog invidTestDialog = new StringDialog(this,
							ts.l("actionPerformed.invid_title"),
							ts.l("actionPerformed.invid_question"),
							ts.l("global.yes"), ts.l("global.no"), question);

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
	// "Invid Sweep"
	// "Are you sure you want to trigger a full Invid fixup sweep?\nIt may take awhile."
	StringDialog invidTestDialog = new StringDialog(this,
							ts.l("actionPerformed.invidsweep_title"),
							ts.l("actionPerformed.invidsweep_question"),
							ts.l("global.yes"), ts.l("global.no"), question);

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
	// "Embedded Object Consistency Test"
	// "Are you sure you want to trigger a full embedded object consistency test?"
	StringDialog invidTestDialog = new StringDialog(this,
							ts.l("actionPerformed.embedded_title"),
							ts.l("actionPerformed.embedded_question"),
							ts.l("global.yes"), ts.l("global.no"), question);

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
	// "Embedded Object Sweep"
	// "Are you sure you want to trigger a full embedded object consistency fixup sweep?"

	StringDialog invidTestDialog = new StringDialog(this,
							ts.l("actionPerformed.embedded_sweep_title"),
							ts.l("actionPerformed.embedded_sweep_question"),
							ts.l("global.yes"), ts.l("global.no"), question);

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
	    if (loginPanel.quitButton != null)
	      {
		loginPanel.quitButton.setEnabled(true);
	      }

	    loginPanel.loginButton.setEnabled(true);

	    saveWindowPrefs();
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

	// "Force Disconnect"
	// "Are you sure you want to force all connected users to log out from the Ganymede server?"

	killAllDLGR = new DialogRsrc(this,
				     ts.l("actionPerformed.killall_title"),
				     ts.l("actionPerformed.killall_question"),
				     ts.l("global.yes"), ts.l("global.no"), question);
    
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

	try
	  {
            schemaMI.setEnabled(false);

	    GASHAdminFrame.schemaEditor = adminDispatch.pullSchema();
	  }
	catch (RemoteException ex)
	  {
	    exceptionHandler(ex);
	  }
        finally
          {
            if (GASHAdminFrame.schemaEditor == null)
              {
                schemaMI.setEnabled(true);
              }
          }
      }
    else if (event.getSource() == showAboutMI)
      {
	showAboutMessage();
      }
    else if (event.getSource() == showCreditsMI)
      {
	showCreditsMessage();
      }
    else if (event.getSource() == clearLogMI)
      {
	statusArea.setText("");
      }
  }

  private void saveWindowPrefs()
  {
    sizer.saveSize(this);
    prefs.putInt(SPLITTER_POS, splitterPane.getDividerLocation());
  }

  /**
   * Shows the About... dialog.
   */

  public void showAboutMessage()
  {
    if (about == null)
      {
	about = new aboutGanyDialog(this, "About Ganymede");
      }

    about.loadAboutText();
    about.setVisible(true);
  }

  /**
   * Shows the Credits dialog.
   */

  public void showCreditsMessage()
  {
    if (about == null)
      {
	about = new aboutGanyDialog(this, "About Ganymede");
      }

    about.loadCreditsText();
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

	// "Force Disconnect"
	// "Are you sure you want to force user {0} to be logged out from the Ganymede server?"

	if (new StringDialog(this,
			     ts.l("rowMenuPerformed.kill_title"),
			     ts.l("rowMenuPerformed.kill_question", key),
			     ts.l("global.yes"), ts.l("global.no"), question).DialogShow() != null)
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
   * Method to handle properly logging out if the main admin
   * frame is closed by the window system.
   *
   * We do an enableEvents(AWT.WINDOW_EVENT_MASK) in the
   * GASHAdminFrame constructor to activate this method.
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
	saveWindowPrefs();
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
 * GUI dialog for presenting server shutdown options in the admin console.
 */

class consoleShutdownDialog extends JCenterDialog implements ActionListener, WindowListener {

  private final static boolean debug = false;

  /**
   * TranslationService object for handling string localization in
   * the Ganymede admin console.
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.admin.consoleShutdownDialog");

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

  JButton
    button1, button2, button3;

  private int result = 0;
  private boolean done = false;

  /* -- */

  public consoleShutdownDialog(Frame frame)
  {
    // "Confirm Ganymede Server Shutdown?"
    super(frame, ts.l("global.title"), true);

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
    // Text message under title
    //

    // "Are you sure you want to shut down the Ganymede server\nrunning at {0}?"

    textLabel = new JMultiLineLabel(ts.l("global.question", GASHAdmin.url));
    
    gbc.gridy = 0;
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
    gbc.gridy = 2;
    gbc.gridwidth = 2;
    gbc.gridheight = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbl.setConstraints(sep, gbc);
    mainPanel.add(sep);

    //
    // ButtonPanel takes up the bottom of the dialog
    //

    buttonPanel = new JFocusRootPanel();

    button1 = new JButton(ts.l("global.right_now_button"));
    button1.addActionListener(this);
    buttonPanel.add(button1);

    button2 = new JButton(ts.l("global.later_button"));
    button2.addActionListener(this);
    buttonPanel.add(button2);

    button3 = new JButton(ts.l("global.no_button"));
    button3.addActionListener(this);
    buttonPanel.add(button3);

    //
    // buttonPanel goes underneath
    //

    gbc.gridx = 0;
    gbc.gridy = 3;
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
    gbc.gridy = 0;
    gbc.gridwidth = 1;
    gbc.gridheight = 2;
    gbc.weightx = 0.0;
    gbc.fill = GridBagConstraints.NONE;
    gbl.setConstraints(imagePanel, gbc);
    mainPanel.add(imagePanel);

    pack();
  }

  /**
   * Display the dialog box, locks this thread while the dialog is being
   * displayed, and returns a hashtable of data field values when the
   * user closes the dialog box.
   *
   * Use this instead of Dialog.show().  If Hashtable returned is null,
   * then the cancel button was clicked.  Otherwise, it will contain a 
   * hash of labels(String) to results (Object).
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
