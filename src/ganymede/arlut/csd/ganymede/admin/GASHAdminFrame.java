/*
   GASHAdminFrame.java

   Admin console for the Java RMI Gash Server

   Created: 7 September 2003

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2013
   The University of Texas at Austin

   Ganymede is a registered trademark of The University of Texas at Austin

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
   along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/

package arlut.csd.ganymede.admin;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.EventQueue;
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
import javax.swing.JDialog;
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
import javax.swing.JTextPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.Position;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import arlut.csd.ganymede.common.windowSizer;
import arlut.csd.JDataComponent.JSetValueObject;
import arlut.csd.JDataComponent.JValueObject;
import arlut.csd.JDataComponent.JErrorValueObject;
import arlut.csd.JDataComponent.JsetValueCallback;
import arlut.csd.JDataComponent.JFocusRootPanel;
import arlut.csd.JDataComponent.JMultiLineLabel;
import arlut.csd.JDataComponent.LAFMenu;
import arlut.csd.JDialog.DialogRsrc;
import arlut.csd.JDialog.JErrorDialog;
import arlut.csd.JDialog.StringDialog;
import arlut.csd.JDialog.messageDialog;
import arlut.csd.JDialog.StandardDialog;
import arlut.csd.JDialog.aboutGanyDialog;
import arlut.csd.JDialog.aboutJavaDialog;
import arlut.csd.JTable.rowSelectCallback;
import arlut.csd.JTable.rowTable;
import arlut.csd.Util.PackageResources;
import arlut.csd.Util.TranslationService;

import apple.dts.samplecode.osxadapter.OSXAdapter;

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

public class GASHAdminFrame extends JFrame implements ActionListener, rowSelectCallback, JsetValueCallback {

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

  public static final Preferences prefs;

  // If we're running as an applet, we might not be able to
  // successfully load our static Preferences reference.  Make sure
  // that we don't block this class' static initialization if we can't
  // get Preferences.

  static
  {
    Preferences _prefs = null;

    try
      {
        _prefs = Preferences.userNodeForPackage(GASHAdminFrame.class);
      }
    catch (Throwable ex)
      {
        ex.printStackTrace();
      }

    prefs = _prefs;
  }

  public static final windowSizer sizer = new windowSizer(prefs);

  static final String SPLITTER_POS = "admin_splitter_pos";
  static final String STATUS_AREA_HEIGHT = "status_area_pos";
  static final String TAB_AREA_HEIGHT = "tab_area_height";

  static final boolean debug = false;

  // ---

  GASHAdminDispatch adminDispatch = null;
  GASHSchema schemaEditor = null;

  Image errorImage = null;
  Image question = null;

  JMenuBar mbar = null;

  JMenu controlMenu = null;

  JMenuItem forceBuildMI = null;
  final String FORCEBUILD = "force build";

  JMenuItem clearLogMI = null;
  final String CLEARLOG = "clear log";

  JMenuItem quitMI = null;
  final String QUIT = "quit";

  JMenuItem dumpMI = null;
  final String DUMP = "dump";

  JMenuItem killAllMI = null;
  final String KILLALL = "kill all";

  JMenuItem schemaMI = null;
  final String SCHEMA = "edit schema";

  JMenuItem shutdownMI = null;
  final String SHUTDOWN = "shutdown";

  JMenu debugMenu = null;

  JMenuItem runInvidTestMI = null;
  final String TESTINVIDS = "test invids";

  JMenuItem runInvidSweepMI = null;
  final String SWEEPINVIDS = "sweep invids";

  JMenuItem runEmbeddedTestMI = null;
  final String TESTEMBEDDED = "test embedded";

  JMenuItem runEmbeddedSweepMI = null;
  final String REPAIREMBEDDED = "repair embedded";

  JMenu helpMenu = null;

  JMenuItem showAboutMI = null;
  final String ABOUT = "about";

  JMenuItem showJavaVersionMI = null;
  final String JAVAVERSION = "java version";

  JPopupMenu popMenu = null;

  JMenuItem killUserMI = null;
  final String KILLUSER = "kill user";

  // and some for our task-related popup menus

  final String RUNTASK = "run task";
  final String STOPTASK = "stop task";
  final String DISABLETASK = "disable task";
  final String ENABLETASK = "enable task";

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

  JLabel usedMemLabel = null;
  JTextField usedMemField = null;

  JLabel freeMemLabel = null;
  JTextField freeMemField = null;

  JLabel totalMemLabel = null;
  JTextField totalMemField = null;

  JTextPane statusArea = null;

  // resources for the users connected table

  rowTable table = null;

  String headers[] = {ts.l("global.user_col_0"), // "User"
                      ts.l("global.user_col_1"), // "System"
                      ts.l("global.user_col_3"), // "Connect Time"
                      ts.l("global.user_col_4"), // "Last Event"
                      ts.l("global.user_col_5")}; // "Objects Checked Out"

  int colWidths[] = {50,50,50,300,50};

  // resources for the sync task monitor table

  rowTable syncTaskTable = null;

  String syncTaskHeaders[] = {ts.l("global.task_col_0"), // "Task"
                              ts.l("global.task_col_5"), // "Type"
                              ts.l("global.task_col_6"), // "Status"
                              ts.l("global.task_col_1"), // "Scheduling Status"
                              ts.l("global.task_col_2")}; // "Last Run"
  int syncTaskColWidths[] = {50,50,100,50,50};

  // resources for the scheduled task monitor table

  rowTable taskTable = null;

  String taskHeaders[] = {ts.l("global.task_col_0"), // "Task"
                          ts.l("global.task_col_1"), // "Status"
                          ts.l("global.task_col_4"), // "Interval"
                          ts.l("global.task_col_3"), // "Next Run"
                          ts.l("global.task_col_2")}; // "Last Run"

  int taskColWidths[] = {50,50,50,50,50};

  // resources for the manual task monitor table

  rowTable manualTaskTable = null;

  String manualTaskHeaders[] = {ts.l("global.task_col_0"), // "Task"
                                ts.l("global.task_col_1"), // "Status"
                                ts.l("global.task_col_2")}; // "Last Run"
  int manualTaskColWidths[] = {50,50,50};

  JSplitPane splitterPane = null;

  GASHAdmin loginPanel;

  String
    aboutMessage = null;

  aboutGanyDialog about = null;

  aboutJavaDialog java_ver_dialog = null;

  LAFMenu LandFMenu = null;

  private JPanel statusBox = null;

  /* -- */

  /**
   *
   * Constructor
   *
   */

  public GASHAdminFrame(String title, GASHAdmin loginPanel, String debugFilename, GASHAdminDispatch adminDispatch)
  {
    super(title);

    this.loginPanel = loginPanel;

    // If we're running on the Mac, let's try to fit in a bit better.

    if (GASHAdmin.isRunningOnMac())
      {
        System.setProperty("apple.laf.useScreenMenuBar", "true");
      }

    mbar = new JMenuBar();

    // "Control"
    controlMenu = new JMenu(ts.l("init.control_menu"), false); // no tear-off

    if (ts.hasPattern("init.control_menu_key_optional"))
      {
        controlMenu.setMnemonic((int) ts.l("init.control_menu_key_optional").charAt(0));
      }

    // "Clear Log Panel"
    clearLogMI = new JMenuItem(ts.l("init.control_menu_0"));
    clearLogMI.setActionCommand(CLEARLOG);

    if (ts.hasPattern("init.control_menu_0_key_optional"))
      {
        clearLogMI.setMnemonic((int) ts.l("init.control_menu_0_key_optional").charAt(0));
      }

    clearLogMI.addActionListener(this);

    // "Force Build"
    forceBuildMI = new JMenuItem(ts.l("init.control_menu_1"));
    forceBuildMI.setActionCommand(FORCEBUILD);

    if (ts.hasPattern("init.control_menu_1_key_optional"))
      {
        forceBuildMI.setMnemonic((int) ts.l("init.control_menu_1_key_optional").charAt(0));
      }

    forceBuildMI.setMnemonic('f');
    forceBuildMI.addActionListener(this);

    // "Kill Off All Users"
    killAllMI = new JMenuItem(ts.l("init.control_menu_2"));
    killAllMI.setActionCommand(KILLALL);

    if (ts.hasPattern("init.control_menu_2_key_optional"))
      {
        killAllMI.setMnemonic((int) ts.l("init.control_menu_2_key_optional").charAt(0));
      }

    killAllMI.addActionListener(this);

    // "Edit Schema"
    schemaMI = new JMenuItem(ts.l("init.control_menu_3"));
    schemaMI.setActionCommand(SCHEMA);

    if (ts.hasPattern("init.control_menu_3_key_optional"))
      {
        schemaMI.setMnemonic((int) ts.l("init.control_menu_3_key_optional").charAt(0));
      }

    schemaMI.addActionListener(this);

    // "Shutdown Ganymede"
    shutdownMI = new JMenuItem(ts.l("init.control_menu_4"));
    shutdownMI.setActionCommand(SHUTDOWN);

    if (ts.hasPattern("init.control_menu_4_key_optional"))
      {
        shutdownMI.setMnemonic((int) ts.l("init.control_menu_4_key_optional").charAt(0));
      }

    shutdownMI.addActionListener(this);

    // "Dump Database"
    dumpMI = new JMenuItem(ts.l("init.control_menu_5"));
    dumpMI.setActionCommand(DUMP);

    if (ts.hasPattern("init.control_menu_5_key_optional"))
      {
        dumpMI.setMnemonic((int) ts.l("init.control_menu_5_key_optional").charAt(0));
      }

    dumpMI.addActionListener(this);

    if (!GASHAdmin.isRunningOnMac())
      {
        // "Quit Console"
        quitMI = new JMenuItem(ts.l("init.control_menu_6"));
        quitMI.setActionCommand(QUIT);

        if (ts.hasPattern("init.control_menu_6_key_optional"))
          {
            quitMI.setMnemonic((int) ts.l("init.control_menu_6_key_optional").charAt(0));
          }

        quitMI.addActionListener(this);
      }

    controlMenu.add(clearLogMI);
    controlMenu.add(forceBuildMI);
    controlMenu.add(killAllMI);
    controlMenu.add(schemaMI);
    controlMenu.add(shutdownMI);
    controlMenu.addSeparator();
    controlMenu.add(dumpMI);
    controlMenu.addSeparator();

    LandFMenu = new arlut.csd.JDataComponent.LAFMenu(this);
    LandFMenu.setCallback(this);

    controlMenu.add(LandFMenu);

    if (!GASHAdmin.isRunningOnMac())
      {
        controlMenu.add(quitMI);
      }

    // "Debug"
    debugMenu = new JMenu(ts.l("init.debug_menu"), false); // no tear-off

    if (ts.hasPattern("init.debug_menu_key_optional"))
      {
        debugMenu.setMnemonic((int) ts.l("init.debug_menu_key_optional").charAt(0));
      }

    // "Test Invid Integrity"
    runInvidTestMI = new JMenuItem(ts.l("init.debug_menu_0"));
    runInvidTestMI.setActionCommand(TESTINVIDS);

    if (ts.hasPattern("init.debug_menu_0_key_optional"))
      {
        runInvidTestMI.setMnemonic((int) ts.l("init.debug_menu_0_key_optional").charAt(0));
      }

    runInvidTestMI.addActionListener(this);

    // "Repair Invid Integrity"
    runInvidSweepMI = new JMenuItem(ts.l("init.debug_menu_1"));
    runInvidSweepMI.setActionCommand(SWEEPINVIDS);

    if (ts.hasPattern("init.debug_menu_1_key_optional"))
      {
        runInvidSweepMI.setMnemonic((int) ts.l("init.debug_menu_1_key_optional").charAt(0));
      }

    runInvidSweepMI.addActionListener(this);

    // "Test Embedded Integrity"
    runEmbeddedTestMI = new JMenuItem(ts.l("init.debug_menu_2"));
    runEmbeddedTestMI.setActionCommand(TESTEMBEDDED);

    if (ts.hasPattern("init.debug_menu_2_key_optional"))
      {
        runEmbeddedTestMI.setMnemonic((int) ts.l("init.debug_menu_2_key_optional").charAt(0));
      }

    runEmbeddedTestMI.addActionListener(this);

    // "Repair Embedded Integrity"
    runEmbeddedSweepMI = new JMenuItem(ts.l("init.debug_menu_3"));
    runEmbeddedSweepMI.setActionCommand(REPAIREMBEDDED);

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

    if (!GASHAdmin.isRunningOnMac())
      {
        // "About Ganymede"
        showAboutMI = new JMenuItem(ts.l("init.help_menu_0"));
        showAboutMI.setActionCommand(ABOUT);

        if (ts.hasPattern("init.help_menu_0_key_optional"))
          {
            showAboutMI.setMnemonic((int) ts.l("init.help_menu_0_key_optional").charAt(0));
          }

        showAboutMI.addActionListener(this);
        helpMenu.add(showAboutMI);

        helpMenu.addSeparator();
      }

    // "Java Version"
    showJavaVersionMI = new JMenuItem(ts.l("init.help_menu_1"));
    showJavaVersionMI.setActionCommand(JAVAVERSION);

    if (ts.hasPattern("init.help_menu_1_key_optional"))
      {
        showJavaVersionMI.setMnemonic((int) ts.l("init.help_menu_1_key_optional").charAt(0));
      }

    showJavaVersionMI.addActionListener(this);
    helpMenu.add(showJavaVersionMI);

    mbar.add(controlMenu);
    mbar.add(debugMenu);
    mbar.add(Box.createGlue());
    mbar.add(helpMenu);

    setJMenuBar(mbar);

    question = PackageResources.getImageResource(this, "question.gif", getClass());

    getContentPane().setLayout(new BorderLayout());

    // set up our top panel, containing a labeled
    // text field showing the server we're connected
    // to.

    /* Ganymede Server Host */

    // "Ganymede Server Host:"

    hostLabel = new JLabel(ts.l("init.hostlabel"));

    if (loginPanel.isSSL())
      {
        // "{0}  [SSL]"
        hostField = new JTextField(ts.l("init.urlssl", GASHAdmin.server_url), 60);
      }
    else
      {
        // "{0}  [NO SSL]"
        hostField = new JTextField(ts.l("init.urlnossl", GASHAdmin.server_url), 60);
      }

    hostField.setEditable(false);

    topPanel = new JPanel();

    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();

    topPanel.setLayout(gbl);

    gbc.insets = new java.awt.Insets(2,1,2,1);

    gbc.anchor = GridBagConstraints.EAST;
    gbc.weightx = 0;
    gbc.gridy = 0;
    gbc.gridx = 0;
    gbc.fill = GridBagConstraints.NONE;
    gbc.gridwidth = 1;
    gbc.gridheight = 1;
    gbl.setConstraints(hostLabel, gbc);
    topPanel.add(hostLabel);

    gbc.anchor = GridBagConstraints.WEST;

    gbc.weightx = 100;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.gridy = 0;
    gbc.gridx = 1;
    gbc.gridwidth = 3;
    gbc.gridheight = 1;
    gbl.setConstraints(hostField, gbc);
    topPanel.add(hostField);

    /* Admin consoles connected to server */

    // "Admin Consoles:"
    adminLabel = new JLabel(ts.l("init.console_count"));

    adminField = new JTextField("", 40);
    adminField.setEditable(false);

    gbc.anchor = GridBagConstraints.EAST;
    gbc.gridy = 0;
    gbc.gridx = 4;
    gbc.weightx = 0;
    gbc.fill = GridBagConstraints.NONE;
    gbc.gridwidth = 1;
    gbc.gridheight = 1;
    gbl.setConstraints(adminLabel, gbc);
    topPanel.add(adminLabel);

    gbc.anchor = GridBagConstraints.WEST;

    gbc.weightx = 100;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.gridy = 0;
    gbc.gridx = 5;
    gbc.gridwidth = 1;
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
    gbc.gridy = 1;
    gbc.gridx = 0;
    gbc.fill = GridBagConstraints.NONE;
    gbc.gridwidth = 1;
    gbc.gridheight = 1;
    gbl.setConstraints(stateLabel, gbc);
    topPanel.add(stateLabel);

    gbc.anchor = GridBagConstraints.WEST;

    gbc.weightx = 100;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.gridy = 1;
    gbc.gridx = 1;
    gbc.gridwidth = 1;
    gbc.gridheight = 1;
    gbl.setConstraints(stateField, gbc);
    topPanel.add(stateField);

    /* Server Start Time */

    // "Server Start Time:"
    startLabel = new JLabel(ts.l("init.start_time"));

    startField = new JTextField("", 40);
    startField.setEditable(false);

    gbc.anchor = GridBagConstraints.EAST;
    gbc.gridy = 1;
    gbc.gridx = 2;
    gbc.weightx = 0;
    gbc.fill = GridBagConstraints.NONE;
    gbc.gridwidth = 1;
    gbc.gridheight = 1;
    gbl.setConstraints(startLabel, gbc);
    topPanel.add(startLabel);

    gbc.anchor = GridBagConstraints.WEST;

    gbc.weightx = 100;
    gbc.gridy = 1;
    gbc.gridx = 3;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.gridwidth = 1;
    gbc.gridheight = 1;
    gbl.setConstraints(startField, gbc);
    topPanel.add(startField);

    /* Last Dump Time */

    // "Last Dump Time:"
    dumpLabel = new JLabel(ts.l("init.lastdump"));

    dumpField = new JTextField("", 40);
    dumpField.setEditable(false);

    gbc.anchor = GridBagConstraints.EAST;
    gbc.gridy = 1;
    gbc.gridx = 4;
    gbc.weightx = 0;
    gbc.fill = GridBagConstraints.NONE;
    gbc.gridwidth = 1;
    gbc.gridheight = 1;
    gbl.setConstraints(dumpLabel, gbc);
    topPanel.add(dumpLabel);

    gbc.anchor = GridBagConstraints.WEST;

    gbc.weightx = 100;
    gbc.gridy = 1;
    gbc.gridx = 5;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.gridwidth = 1;
    gbc.gridheight = 1;
    gbl.setConstraints(dumpField, gbc);
    topPanel.add(dumpField);

    /* In-use / Free / Total Memory */

    // "Memory In Use:"
    usedMemLabel = new JLabel(ts.l("init.usedMemory"));

    gbc.anchor = GridBagConstraints.EAST;
    gbc.gridy = 2;
    gbc.gridx = 0;
    gbc.weightx = 0;
    gbc.fill = GridBagConstraints.NONE;
    gbc.gridwidth = 1;
    gbc.gridheight = 1;
    gbl.setConstraints(usedMemLabel, gbc);
    topPanel.add(usedMemLabel);

    usedMemField = new JTextField("", 40);
    usedMemField.setEditable(false);

    gbc.anchor = GridBagConstraints.WEST;
    gbc.weightx = 100;
    gbc.gridy = 2;
    gbc.gridx = 1;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.gridwidth = 1;
    gbc.gridheight = 1;
    gbl.setConstraints(usedMemField, gbc);
    topPanel.add(usedMemField);

    // "Free Memory:"
    freeMemLabel = new JLabel(ts.l("init.freeMemory"));

    gbc.anchor = GridBagConstraints.EAST;
    gbc.gridy = 2;
    gbc.gridx = 2;
    gbc.weightx = 0;
    gbc.fill = GridBagConstraints.NONE;
    gbc.gridwidth = 1;
    gbc.gridheight = 1;
    gbl.setConstraints(freeMemLabel, gbc);
    topPanel.add(freeMemLabel);

    freeMemField = new JTextField("", 40);
    freeMemField.setEditable(false);

    gbc.anchor = GridBagConstraints.WEST;
    gbc.weightx = 100;
    gbc.gridy = 2;
    gbc.gridx = 3;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.gridwidth = 1;
    gbc.gridheight = 1;
    gbl.setConstraints(freeMemField, gbc);
    topPanel.add(freeMemField);

    // "Total Process Memory:"
    totalMemLabel = new JLabel(ts.l("init.totalMemory"));

    gbc.anchor = GridBagConstraints.EAST;
    gbc.gridy = 2;
    gbc.gridx = 4;
    gbc.weightx = 0;
    gbc.fill = GridBagConstraints.NONE;
    gbc.gridwidth = 1;
    gbc.gridheight = 1;
    gbl.setConstraints(totalMemLabel, gbc);
    topPanel.add(totalMemLabel);

    totalMemField = new JTextField("", 40);
    totalMemField.setEditable(false);

    gbc.anchor = GridBagConstraints.WEST;
    gbc.weightx = 100;
    gbc.gridy = 2;
    gbc.gridx = 5;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.gridwidth = 1;
    gbc.gridheight = 1;
    gbl.setConstraints(totalMemField, gbc);
    topPanel.add(totalMemField);

    /* Transactions in Journal */

    // "Transactions in Journal:"
    journalLabel = new JLabel(ts.l("init.trans_count"));

    journalField = new JTextField("", 40);
    journalField.setEditable(false);

    gbc.anchor = GridBagConstraints.EAST;
    gbc.gridy = 3;
    gbc.gridx = 0;
    gbc.weightx = 0;
    gbc.fill = GridBagConstraints.NONE;
    gbc.gridwidth = 1;
    gbc.gridheight = 1;
    gbl.setConstraints(journalLabel, gbc);
    topPanel.add(journalLabel);

    gbc.anchor = GridBagConstraints.WEST;
    gbc.gridy = 3;
    gbc.gridx = 1;
    gbc.weightx = 100;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.gridwidth = 1;
    gbc.gridheight = 1;
    gbl.setConstraints(journalField, gbc);
    topPanel.add(journalField);

    /* Locks held */

    // "Locks Waiting / Held:"
    locksLabel = new JLabel(ts.l("init.locks"));

    locksField = new JTextField("", 40);
    locksField.setEditable(false);

    gbc.anchor = GridBagConstraints.EAST;
    gbc.gridy = 3;
    gbc.gridx = 2;
    gbc.weightx = 0;
    gbc.fill = GridBagConstraints.NONE;
    gbc.gridwidth = 1;
    gbc.gridheight = 1;
    gbl.setConstraints(locksLabel, gbc);
    topPanel.add(locksLabel);

    gbc.anchor = GridBagConstraints.WEST;

    gbc.gridy = 3;
    gbc.gridx = 3;
    gbc.weightx = 100;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.gridwidth = 1;
    gbc.gridheight = 1;
    gbl.setConstraints(locksField, gbc);
    topPanel.add(locksField);


    /* Objects Checked Out */

    // "Objects Checked Out:"
    checkedOutLabel = new JLabel(ts.l("init.objects_out"));

    checkedOutField = new JTextField("", 40);
    checkedOutField.setEditable(false);

    gbc.gridy = 3;
    gbc.gridx = 4;
    gbc.anchor = GridBagConstraints.EAST;
    gbc.weightx = 0;
    gbc.fill = GridBagConstraints.NONE;
    gbc.gridwidth = 1;
    gbc.gridheight = 1;
    gbl.setConstraints(checkedOutLabel, gbc);
    topPanel.add(checkedOutLabel);

    gbc.anchor = GridBagConstraints.WEST;

    gbc.weightx = 100;
    gbc.gridy = 3;
    gbc.gridx = 5;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.gridwidth = 1;
    gbc.gridheight = 1;
    gbl.setConstraints(checkedOutField, gbc);
    topPanel.add(checkedOutField);


    JPanel topBox = new JPanel(new BorderLayout());
    topBox.add("Center",topPanel);

    // "Ganymede Server"
    topBox.setBorder(new TitledBorder(ts.l("init.title")));

    getContentPane().add(topBox, BorderLayout.NORTH);

    // set up our middle text area

    // "Ganymede Admin Console\n"
    statusArea = new JTextPane();
    appendLogText(ts.l("init.start_log_msg"));
    statusArea.setEditable(false);
    JScrollPane statusAreaPane = new JScrollPane(statusArea);

    statusBox = new JPanel(new java.awt.BorderLayout());
    statusBox.add("Center", statusAreaPane);

    // "Server Log"
    statusBox.setBorder(new TitledBorder(ts.l("init.server_log_title")));

    // bottom area, a tab pane with tables for things

    // create our user table

    JPopupMenu popMenu = new JPopupMenu();

    // "Kill User"
    killUserMI = new JMenuItem(ts.l("init.killUserPopup"));
    killUserMI.setActionCommand(KILLUSER);
    popMenu.add(killUserMI);

    table = new rowTable(colWidths, headers, this, false, popMenu, false);
    JPanel tableBox = new JPanel(new BorderLayout());
    tableBox.add("Center", table);

    //
    // create task monitors
    //

    // first the sync monitor

    JPopupMenu syncTaskPopMenu = new JPopupMenu();

    // "Run Task Now"
    JMenuItem runNowMI = new JMenuItem(ts.l("init.runNowPopup"));
    runNowMI.setActionCommand(RUNTASK);

    // "Stop Running Task"
    JMenuItem stopTaskMI = new JMenuItem(ts.l("init.stopTaskPopup"));
    stopTaskMI.setActionCommand(STOPTASK);

    // "Disable Task"
    JMenuItem disableTaskMI = new JMenuItem(ts.l("init.disableTaskPopup"));
    disableTaskMI.setActionCommand(DISABLETASK);

    // "Enable Task"
    JMenuItem enableTaskMI = new JMenuItem(ts.l("init.enableTaskPopup"));
    enableTaskMI.setActionCommand(ENABLETASK);

    syncTaskPopMenu.add(runNowMI);
    syncTaskPopMenu.add(stopTaskMI);
    syncTaskPopMenu.add(disableTaskMI);
    syncTaskPopMenu.add(enableTaskMI);

    syncTaskTable = new rowTable(syncTaskColWidths, syncTaskHeaders, this, false, syncTaskPopMenu, false);

    // 0b5a0e
    syncTaskTable.setHeadBackColor(new java.awt.Color(7,212,16), false);

    JPanel syncTaskBox = new JPanel(new java.awt.BorderLayout());
    syncTaskBox.add("Center", syncTaskTable);

    // then the scheduled task monitor

    JPopupMenu taskPopMenu = new JPopupMenu();

    // we need to create new JMenuItems for the task table,
    // independent from the sync task table, so we're re-assigning
    // these variables

    // "Run Task Now"
    runNowMI = new JMenuItem(ts.l("init.runNowPopup"));
    runNowMI.setActionCommand(RUNTASK);

    // "Stop Running Task"
    stopTaskMI = new JMenuItem(ts.l("init.stopTaskPopup"));
    stopTaskMI.setActionCommand(STOPTASK);

    // "Disable Task"
    disableTaskMI = new JMenuItem(ts.l("init.disableTaskPopup"));
    disableTaskMI.setActionCommand(DISABLETASK);

    // "Enable Task"
    enableTaskMI = new JMenuItem(ts.l("init.enableTaskPopup"));
    enableTaskMI.setActionCommand(ENABLETASK);

    taskPopMenu.add(runNowMI);
    taskPopMenu.add(stopTaskMI);
    taskPopMenu.add(disableTaskMI);
    taskPopMenu.add(enableTaskMI);

    taskTable = new rowTable(taskColWidths, taskHeaders, this, false, taskPopMenu, false);
    taskTable.setHeadBackColor(Color.red, false);

    JPanel taskBox = new JPanel(new java.awt.BorderLayout());
    taskBox.add("Center", taskTable);

    // then the manual task monitor

    JPopupMenu manualTaskPopMenu = new JPopupMenu();

    // "Run Task Now"
    runNowMI = new JMenuItem(ts.l("init.runNowPopup"));
    runNowMI.setActionCommand(RUNTASK);

    // "Stop Running Task"
    stopTaskMI = new JMenuItem(ts.l("init.stopTaskPopup"));
    stopTaskMI.setActionCommand(STOPTASK);

    manualTaskPopMenu.add(runNowMI);
    manualTaskPopMenu.add(stopTaskMI);

    manualTaskTable = new rowTable(manualTaskColWidths, manualTaskHeaders, this, false, manualTaskPopMenu, false);
    manualTaskTable.setHeadBackColor(Color.gray, false);

    JPanel manualTaskBox = new JPanel(new java.awt.BorderLayout());
    manualTaskBox.add("Center", manualTaskTable);

    // and put them into our tab pane

    tabPane = new JTabbedPane();

    // "Users Connected"
    tabPane.addTab(ts.l("init.users_title"), tableBox);

    // "Sync Monitor"
    tabPane.addTab(ts.l("init.sync_title"), syncTaskBox);

    // "Scheduled Task Monitor"
    tabPane.addTab(ts.l("init.task_title"), taskBox);

    // "Manual Task Monitor"
    tabPane.addTab(ts.l("init.manual_task_title"), manualTaskBox);

    // and put the tab pane into our frame with the
    // same constraints that the text area had

    int statusAreaHeight = -1;
    int tabAreaHeight = -1;
    int dividerLoc = -1;

    if (prefs != null)
      {
        statusAreaHeight = prefs.getInt(STATUS_AREA_HEIGHT, -1);
        tabAreaHeight = prefs.getInt(TAB_AREA_HEIGHT, -1);
        dividerLoc = prefs.getInt(SPLITTER_POS, -1);

        if (debug)
          {
            System.err.println("statusAreaHeight = " + statusAreaHeight);
            System.err.println("tabAreaHeight = " + tabAreaHeight);
            System.err.println("dividerLoc = " + dividerLoc);
          }
      }

    if (GASHAdmin.isRunningOnMac())
      {
        MacOSXController controller = new MacOSXController(this);

        try
          {
            OSXAdapter.setQuitHandler(controller, MacOSXController.class.getMethod("handleQuit", (Class[]) null));
            OSXAdapter.setAboutHandler(controller, MacOSXController.class.getMethod("handleAbout", (Class[]) null));
          }
        catch (NoSuchMethodException ex)
          {
            // we shouldn't get an exception here at runtime unless
            // we've made a mistake in the MacOSXController class.
          }
      }

    /*********************************************************************************

                                        NOTE!

        This whole JSplitPane sizing business is *very* *very* finicky!!

       If you mess with any of the following sizing logic or operation
       ordering, you are likely to see a failure to properly recreate the last
       saved vertical split position on admin console restart on one platform
       or another.

       All of this took a *long* time to get right, so mess with it at your peril.

    ********************************************************************************/

    splitterPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, statusBox, tabPane);
    splitterPane.setContinuousLayout(false);
    splitterPane.setOneTouchExpandable(true);
    getContentPane().add(splitterPane, BorderLayout.CENTER);

    // Set the icon on the sync monitor so that we won't have our
    // layout changed after we get our first adminDispatch callback on
    // the sync monitor tab.

    adminDispatch.setFrame(this);
    setDispatch(adminDispatch);

    tabPane.setIconAt(1, adminDispatch.getOkIcon());
    tabPane.setMinimumSize(new Dimension(0, 100));

    statusBox.setMinimumSize(new Dimension(0, 100));

    if (statusAreaHeight != -1 && tabAreaHeight != -1)
      {
        statusBox.setPreferredSize(new Dimension(0, statusAreaHeight));
        tabPane.setPreferredSize(new Dimension(0, tabAreaHeight));
      }

    if (!sizer.restoreSize(this))
      {
        statusBox.setPreferredSize(new Dimension(0, 200));
        tabPane.setPreferredSize(new Dimension(0, 200));

        this.pack();

        sizer.saveSize(this);   // save an initial size before the user might maximize
      }

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

    // along with processWindowEvent(), this method allows us
    // to properly handle window system close events.

    enableEvents(AWTEvent.WINDOW_EVENT_MASK);

    // and adjust the splitter pane with our saved divider location

    if (statusAreaHeight != -1)
      {
        if (debug)
          {
            System.err.println("Setting dividerLoc to " + dividerLoc);
          }

        splitterPane.setDividerLocation(dividerLoc);
      }

    // these break things on JDK 7
    //
    //    invalidate();
    //    validateTree();

    setVisible(true);
  }

  public void setDispatch(GASHAdminDispatch ad)
  {
    adminDispatch = ad;
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

  /**
   * <p>Add text from the server log to the log text pane.</p>
   *
   * <p>This method looks for patterns in the Ganymede server's log text,
   * and will break the text apart to apply coloring to make different
   * pieces of the log text stand out.</p>
   */

  public void appendStyledLogText(String text)
  {
    String[] lines = text.split("\n");

    for (String line: lines)
      {
        if (line.matches(".*\\[.\\d\\].*"))
          {
            String date = line.substring(0, line.indexOf('['));
            String count = line.substring(line.indexOf('[') + 1, line.indexOf(']')).trim();
            String remnant = line.substring(line.indexOf(']') + 2);

            int countVal = 0;

            try
              {
                countVal = Integer.valueOf(count);
              }
            catch (NumberFormatException ex)
              {
              }

            appendLogText(date, Color.black, Color.white);
            appendLogText(" [", Color.black, Color.white);

            if (countVal == 0)
              {
                appendLogText(count, Color.black, Color.white);
              }
            else
              {
                appendLogText(count, Color.blue, Color.white);
              }

            appendLogText("] ", Color.black, Color.white);
            appendLogText(remnant + "\n", Color.black, Color.white);
          }
        else if (line.matches(".*\\[\\*\\].*"))
          {
            String date = line.substring(0, line.indexOf('['));
            String remnant = line.substring(line.indexOf(']') + 2);

            appendLogText(date, Color.black, Color.white);
            appendLogText(" [", Color.black, Color.white);
            appendLogText("*", Color.red, Color.white);
            appendLogText("] ", Color.black, Color.white);
            appendLogText(remnant + "\n", Color.black, Color.white);
          }
        else
          {
            appendLogText(line + "\n", Color.black, Color.white);
          }
      }

    statusArea.setCaretPosition(statusArea.getDocument().getEndPosition().getOffset() - 1);
  }

  public void appendLogText(String text)
  {
    appendLogText(text, Color.black, Color.white);
  }

  public void appendLogText(String text, Color foreground, Color background)
  {
    Document doc = statusArea.getDocument();
    Position end = doc.getEndPosition();
    MutableAttributeSet attr = new SimpleAttributeSet();

    StyleConstants.setForeground(attr, foreground);
    StyleConstants.setBackground(attr, background);

    try
      {
        statusArea.getDocument().insertString(end.getOffset() - 1, text, attr);
      }
    catch (BadLocationException ex)
      {
        throw new RuntimeException(ex);
      }
  }

  // our button / dialog handler

  public void actionPerformed(ActionEvent event)
  {
    if (FORCEBUILD.equals(event.getActionCommand()))
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
    else if (QUIT.equals(event.getActionCommand()))
      {
        if (debug)
          {
            System.err.println("Quitting");
          }

        this.disconnect();
      }
    else if (DUMP.equals(event.getActionCommand()))
      {
        if (dumpDialog == null)
          {
            // "Ganymede Server Dump"
            // "Are you sure you want to schedule\na full dump of the Ganymede database to disk?"

            dumpDialog = new StringDialog(this,
                                          ts.l("actionPerformed.dump_title"),
                                          ts.l("actionPerformed.dump_question"),
                                          ts.l("global.yes"), ts.l("global.no"), question, StandardDialog.ModalityType.DOCUMENT_MODAL);
          }

        if (dumpDialog.showDialog() != null)
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
    else if (TESTINVIDS.equals(event.getActionCommand()))
      {
        // "Invid Test"
        // "Are you sure you want to trigger a full Invid consistency test?\nIt may take awhile."

        StringDialog invidTestDialog = new StringDialog(this,
                                                        ts.l("actionPerformed.invid_title"),
                                                        ts.l("actionPerformed.invid_question"),
                                                        ts.l("global.yes"), ts.l("global.no"), question, StandardDialog.ModalityType.DOCUMENT_MODAL);

        if (invidTestDialog.showDialog() != null)
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
    else if (SWEEPINVIDS.equals(event.getActionCommand()))
      {
        // "Invid Sweep"
        // "Are you sure you want to trigger a full Invid fixup sweep?\nIt may take awhile."
        StringDialog invidTestDialog = new StringDialog(this,
                                                        ts.l("actionPerformed.invidsweep_title"),
                                                        ts.l("actionPerformed.invidsweep_question"),
                                                        ts.l("global.yes"), ts.l("global.no"), question, StandardDialog.ModalityType.DOCUMENT_MODAL);

        if (invidTestDialog.showDialog() != null)
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
    else if (TESTEMBEDDED.equals(event.getActionCommand()))
      {
        // "Embedded Object Consistency Test"
        // "Are you sure you want to trigger a full embedded object consistency test?"
        StringDialog invidTestDialog = new StringDialog(this,
                                                        ts.l("actionPerformed.embedded_title"),
                                                        ts.l("actionPerformed.embedded_question"),
                                                        ts.l("global.yes"), ts.l("global.no"), question, StandardDialog.ModalityType.DOCUMENT_MODAL);

        if (invidTestDialog.showDialog() != null)
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
    else if (REPAIREMBEDDED.equals(event.getActionCommand()))
      {
        // "Embedded Object Sweep"
        // "Are you sure you want to trigger a full embedded object consistency fixup sweep?"

        StringDialog invidTestDialog = new StringDialog(this,
                                                        ts.l("actionPerformed.embedded_sweep_title"),
                                                        ts.l("actionPerformed.embedded_sweep_question"),
                                                        ts.l("global.yes"), ts.l("global.no"), question, StandardDialog.ModalityType.DOCUMENT_MODAL);

        if (invidTestDialog.showDialog() != null)
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
    else if (SHUTDOWN.equals(event.getActionCommand()))
      {
        boolean waitForUsers=false;

        shutdownDialog = new consoleShutdownDialog(this);

        int result = shutdownDialog.showDialog();

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
    else if (KILLALL.equals(event.getActionCommand()))
      {
        DialogRsrc killAllDLGR;
        StringDialog killAllDLG;

        // "Force Disconnect"
        // "Are you sure you want to force all connected users to log out from the Ganymede server?"

        killAllDLGR = new DialogRsrc(this,
                                     ts.l("actionPerformed.killall_title"),
                                     ts.l("actionPerformed.killall_question"),
                                     ts.l("global.yes"), ts.l("global.no"), question);

        killAllDLG = new StringDialog(killAllDLGR, StandardDialog.ModalityType.DOCUMENT_MODAL);

        if (killAllDLG.showDialog() == null)
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
    else if (SCHEMA.equals(event.getActionCommand()))
      {
        if (schemaEditor != null)
          {
            return;
          }

        try
          {
            schemaMI.setEnabled(false);

            schemaEditor = adminDispatch.pullSchema();
          }
        catch (RemoteException ex)
          {
            exceptionHandler(ex);
          }
        finally
          {
            if (schemaEditor == null)
              {
                schemaMI.setEnabled(true);
              }
          }
      }
    else if (JAVAVERSION.equals(event.getActionCommand()))
      {
        showJavaVersion();
      }
    else if (ABOUT.equals(event.getActionCommand()))
      {
        showAboutMessage();
      }
    else if (CLEARLOG.equals(event.getActionCommand()))
      {
        statusArea.setText("");
      }
  }

  private void saveWindowPrefs()
  {
    if (prefs != null)
      {
        sizer.saveSize(this);
        prefs.putInt(STATUS_AREA_HEIGHT, statusBox.getHeight());
        prefs.putInt(TAB_AREA_HEIGHT, tabPane.getHeight());
        prefs.putInt(SPLITTER_POS, splitterPane.getDividerLocation());
      }
  }

  /**
   * Shows the Java Version dialog.
   */

  public void showJavaVersion()
  {
    if (java_ver_dialog == null)
      {
        java_ver_dialog = new aboutJavaDialog(this, ts.l("showJavaVersion.dialog_title"));  // "Java Version"
      }

    java_ver_dialog.setVisible(true);
  }

  /**
   * Shows the About... dialog.
   */

  public void showAboutMessage()
  {
    if (about == null)
      {
        about = new aboutGanyDialog(this, ts.l("showAboutMessage.dialog_title"));  // "About Ganymede"
      }

    about.setVisible(true);
  }

  /**
   * This method comprises the JsetValueCallback interface, and is how
   * some data-carrying components notify us when something changes.
   *
   * @see arlut.csd.JDataComponent.JsetValueCallback
   * @see arlut.csd.JDataComponent.JValueObject
   */

  public boolean setValuePerformed(JValueObject o)
  {
    if (o instanceof JErrorValueObject)
      {
        showErrorMessage((String)o.getValue());
      }
    else if (o instanceof JSetValueObject && o.getSource() == LandFMenu)
      {
        sizer.saveLookAndFeel();

        if (about != null)
          {
            SwingUtilities.updateComponentTreeUI(about);
          }

        if (java_ver_dialog != null)
          {
            SwingUtilities.updateComponentTreeUI(java_ver_dialog);
          }

        if (shutdownDialog != null)
          {
            SwingUtilities.updateComponentTreeUI(java_ver_dialog);
          }

        if (dumpDialog != null)
          {
            SwingUtilities.updateComponentTreeUI(dumpDialog);
          }

        if (schemaEditor != null)
          {
            SwingUtilities.updateComponentTreeUI(schemaEditor);
          }

        if (loginPanel != null)
          {
            SwingUtilities.updateComponentTreeUI(loginPanel);
          }
      }
    else
      {
        if (debug)
          {
            System.err.println("I don't know what to do with this setValuePerformed: " + o);
          }

        return false;
      }

    return true;
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

    if (KILLUSER.equals(e.getActionCommand()))
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
                             ts.l("global.yes"), ts.l("global.no"), question, StandardDialog.ModalityType.DOCUMENT_MODAL).showDialog() != null)
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
    else if (RUNTASK.equals(e.getActionCommand()))
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
    else if (STOPTASK.equals(e.getActionCommand()))
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
    else if (DISABLETASK.equals(e.getActionCommand()))
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
    else if (ENABLETASK.equals(e.getActionCommand()))
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

  public void colMenuPerformed(int menuCol, ActionEvent e)
  {
  }

  /**
   * <p>Method to handle properly logging out if the main admin
   * frame is closed by the window system.</p>
   *
   * <p>We do an enableEvents(AWT.WINDOW_EVENT_MASK) in the
   * GASHAdminFrame constructor to activate this method.</p>
   */

  protected void processWindowEvent(WindowEvent e)
  {
    if (e.getID() == WindowEvent.WINDOW_CLOSING)
      {
        // make sure that we cancel any schema editing in process if
        // we have it open and we are made to close the main window

        if (schemaEditor != null)
          {
            schemaEditor.cancel();
          }

        disconnect();
        saveWindowPrefs();
      }

    super.processWindowEvent(e);
  }

  private void exceptionHandler(Throwable ex)
  {
    adminDispatch.logAppend("******************** " +
                            "Error occurred while communicating with the server " +
                            "********************\n");
    StringWriter stringTarget = new StringWriter();
    PrintWriter writer = new PrintWriter(stringTarget);

    ex.printStackTrace(writer);
    writer.close();

    adminDispatch.logAppend(stringTarget.toString());

    adminDispatch.logAppend("****************************************" +
                            "****************************************\n");
  }

  /**
   * <p>Loads and returns the error Image for use in client dialogs.</p>
   *
   * <p>Once the image is loaded, it is cached for future calls to
   * getErrorImage().</p>
   */

  public final Image getErrorImage()
  {
    if (errorImage == null)
      {
        errorImage = PackageResources.getImageResource(this, "error.gif", getClass());
      }

    return errorImage;
  }

  /**
   * Pops up an error dialog with the default title.
   */

  public final void showErrorMessage(String message)
  {
    // "Error"
    showErrorMessage(ts.l("global.error"), message);
  }

  /**
   * Pops up an error dialog.  Pre-defines the icon for the dialog as
   * the standard Ganymede error icon.
   */

  public final void showErrorMessage(String title, String message)
  {
    showErrorMessage(title, message, getErrorImage());
  }

  /**
   * Show an error dialog.
   *
   * @param title title of dialog.
   * @param message Text of dialog.
   * @param icon optional icon to display.
   */

  public final void showErrorMessage(String title, String message, Image icon)
  {
    if (debug)
      {
        System.err.println("Error message: " + message);
      }

    final GASHAdminFrame my_frame = this;
    final String Title = title;
    final String Message = message;
    final Image fIcon = icon;

    EventQueue.invokeLater(new Runnable()
                               {
                                 public void run()
                                   {
                                     new JErrorDialog(my_frame, Title, Message, fIcon, StandardDialog.ModalityType.DOCUMENT_MODAL); // implicit show
                                   }
                               });
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                           consoleShutdownDialog

------------------------------------------------------------------------------*/

/**
 * GUI dialog for presenting server shutdown options in the admin console.
 */

class consoleShutdownDialog extends StandardDialog implements ActionListener, WindowListener {

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
    super(frame, ts.l("global.title"), StandardDialog.ModalityType.DOCUMENT_MODAL);

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

    textLabel = new JMultiLineLabel(ts.l("global.question", GASHAdmin.server_url));

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

  public int showDialog()
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

    // pop down so that showDialog() can proceed to completion.

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
