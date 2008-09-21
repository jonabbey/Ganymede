/*

   gResultTable.java

   This module is designed to provide a tabular view of the results
   of a query.
   
   Created: 14 July 1997
   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996 - 2008
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

package arlut.csd.ganymede.client;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Font;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.rmi.RemoteException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JInternalFrame;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JToolBar;

import arlut.csd.JDialog.StringDialog;
import arlut.csd.JTable.rowSelectCallback;
import arlut.csd.JTable.rowTable;
import arlut.csd.Util.PackageResources;
import arlut.csd.Util.TranslationService;
import arlut.csd.ganymede.common.DumpResult;
import arlut.csd.ganymede.common.DumpResultCol;
import arlut.csd.ganymede.common.Invid;
import arlut.csd.ganymede.common.Query;
import arlut.csd.ganymede.common.RegexpException;
import arlut.csd.ganymede.common.ReturnVal;
import arlut.csd.ganymede.rmi.FileTransmitter;
import arlut.csd.ganymede.rmi.Session;


/*------------------------------------------------------------------------------
                                                                           class
                                                                    gResultTable

------------------------------------------------------------------------------*/

/**
 * Client internal window for displaying the results of a 
 * query {@link arlut.csd.ganymede.rmi.Session#dump(arlut.csd.ganymede.common.Query) dump}
 * in a table form.
 *
 * This window is created when {@link arlut.csd.ganymede.client.windowPanel windowPanel}'s
 * {@link arlut.csd.ganymede.client.windowPanel#addTableWindow(arlut.csd.ganymede.rmi.Session,arlut.csd.ganymede.common.Query,arlut.csd.ganymede.common.DumpResult) addTableWindow}
 * method is called.
 * 
 * Note that windowPanel's addTableWindow method is called from 
 * {@link arlut.csd.ganymede.client.gclient gclient}'s actionPerformed method,
 * which spawns a separate thread in which the query is performed and
 * the gResultTable window is created.
 *
 * Constructors for this class take a {@link arlut.csd.ganymede.common.Query Query} object
 * describing the query that this table was generated from, and a
 * {@link arlut.csd.ganymede.common.DumpResult DumpResult} object actually containing the dump
 * results from the Ganymede server.  gResultTable can resubmit the dump query to the
 * server if the user chooses to refresh the query, but normally the dump query
 * is performed by gclient.
 *
 * @version $Id: f85df3f1c13ba280eaebd0ed7584d1fde262f4df $
 * @author Jonathan Abbey, jonabbey@arlut.utexas.edu
 */

public class gResultTable extends JInternalFrame implements rowSelectCallback, ActionListener {
  
  static final boolean debug = false;

  /**
   * TranslationService object for handling string localization in the
   * Ganymede client.
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.client.gResultTable");

  static final String
    refresh_query = ts.l("global.refresh_query"), // "Refresh Query"
    save_report = ts.l("global.save_report"), // "Save Report"
    mail_report = ts.l("global.mail_report"); // "Mail Report"

  static final String
    tab_option = ts.l("global.tab_option"), // "Tab separated ASCII"
    csv_option = ts.l("global.csv_option"), // "Comma separated ASCII"
    html_option = ts.l("global.html_option"), // "HTML"
    xml_option = ts.l("global.xml_option");  // "Ganymede XML File"

  static final String TABLE_SAVE = "query_table_default_dir";

  // ---

  /**
   * Reference to the desktop pane containing the client's internal windows.  Used to access
   * some GUI resources.
   */

  windowPanel wp;

  /**
   * Main remote interface for communications with the server.  Used to resubmit the
   * query on query refresh.
   */

  Session session;

  /**
   * The actual Query used to create this gResultTable.  Used if the user asks that the
   * query be refreshed.
   */

  Query query;

  /**
   * The GUI table component.
   */

  rowTable table = null;

  /**
   * A cache of header definitions that we can use to manage column
   * deletion.
   */

  Vector headerObjects = null;

  /**
   * The contentPane for this internal window.  We place the rowTable in this
   * container.
   */

  Container contentPane;

  JPopupMenu popMenu;
  JMenuItem viewMI;
  JMenuItem editMI;
  JMenuItem deleteMI;
  JMenuItem inactivateMI;
  JMenuItem cloneMI;

  JToolBar toolbar;

  /* -- */

  /**
   * Constructor for gResultTable.  Creates the GUI table, loads it,
   * and presents it.  DumpResult is dissociated when this constructor is
   * through with it, to aid GC.
   */

  public gResultTable(windowPanel wp, Session session, Query query, DumpResult results) throws RemoteException
  {
    super();			// JInternalFrame init

    this.wp = wp;
    this.session = session;
    this.query = query;

    popMenu = new JPopupMenu();
    viewMI = new JMenuItem(ts.l("init.view")); // "View Entry"
    editMI = new JMenuItem(ts.l("init.edit")); // "Edit Entry"
    cloneMI = new JMenuItem(ts.l("init.clone")); // "Clone Entry"
    deleteMI = new JMenuItem(ts.l("init.delete")); // "Delete Entry"
    inactivateMI = new JMenuItem(ts.l("init.inactivate")); // "Inactivate Entry"

    popMenu.add(viewMI);
    popMenu.add(editMI);
    popMenu.add(cloneMI);
    popMenu.add(deleteMI);
    popMenu.add(inactivateMI);

    contentPane = getContentPane();

    contentPane.setLayout(new BorderLayout());

    toolbar = createToolBar();
    contentPane.add("North",toolbar);
    toolbar.grabFocus();

    loadResults(results);
  }

  public void actionPerformed(ActionEvent event)
  {
    if (event.getActionCommand().equals(refresh_query))
      {
	refreshQuery();
	toolbar.requestFocus();
	return;
      }

    if (event.getActionCommand().equals(save_report))
      {
	sendReport(false);
	toolbar.requestFocus();
	return;
      }
    
    if (event.getActionCommand().equals(mail_report))
      {
	sendReport(true);
	toolbar.requestFocus();
	return;
      }
  }

  public void sendReport(boolean mailit)
  {
    SaveDialog dialog = new SaveDialog(wp.gc, mailit);
    Vector formatChoices = new Vector();
    String addresses;
    String format;
    StringBuffer report = null;

    /* -- */

    formatChoices.addElement(tab_option);
    formatChoices.addElement(csv_option);
    formatChoices.addElement(html_option);
    formatChoices.addElement(xml_option);

    dialog.setFormatChoices(formatChoices);
    
    if (!dialog.showDialog())
      {
	return;
      }

    format = dialog.getFormat();

    if (format.equals(html_option))
      {
	report = generateHTMLRep();
      }
    else if (format.equals(csv_option))
      {
	report = generateTextRep(',');
      }
    else if (format.equals(tab_option))
      {
	report = generateTextRep('\t');
      }
    else if (format.equals(xml_option))
      {
        try
          {
            report = generateXMLReport();
          }
        catch (RemoteException ex)
          {
            this.wp.getgclient().processException(ex);
          }
      }
    else
      {
	throw new RuntimeException("Error, SaveDialog returned unrecognized format! " + format);
      }

    if (mailit)
      {
	addresses = dialog.getRecipients();

	try
	  {
	    if (format.equals(html_option))
	      {
		session.sendHTMLMail(addresses, dialog.getSubject(), null, report);
	      }
	    else
	      {
		session.sendMail(addresses, dialog.getSubject(), report);
	      }
	  }
	catch (Exception ex)
	  {
	    wp.gc.processException(ex, "Could not send mail.");
	  }

	return;
      }
    else
      {
	JFileChooser chooser = new JFileChooser();
	File file;
	PrintWriter writer = null;

	chooser.setDialogType(JFileChooser.SAVE_DIALOG);

	// "Save Report As"
	chooser.setDialogTitle(ts.l("sendReport.dialog_title"));

	String defaultPath = gclient.prefs.get(TABLE_SAVE, null);

	if (defaultPath != null)
	  {
	    chooser.setCurrentDirectory(new File(defaultPath));
	  }

	int returnValue = chooser.showDialog(wp.gc, null);

	if (!(returnValue == JFileChooser.APPROVE_OPTION))
	  {
	    return;
	  }

	file = chooser.getSelectedFile();
	File directory = chooser.getCurrentDirectory();

	try
	  {
	    gclient.prefs.put(TABLE_SAVE, directory.getCanonicalPath());
	  }
	catch (java.io.IOException ex)
	  {
	    // we don't really care if we can't save the directory
	    // path in our preferences all that much.
	  }
    
	if (file.exists())
	  {
	    // "Warning, file {0} already exists.
	    // "Warning, file {0} already exists.  Are you sure you want to replace this file?"
	    // "Yes, Overwrite"
	    StringDialog d = new StringDialog(wp.gc,
					      ts.l("sendReport.file_exists_title", file.getName()),
					      ts.l("sendReport.file_exists", file.getName()),
					      ts.l("sendReport.overwrite"),
					      StringDialog.getDefaultCancel(),
					      null);
	    Hashtable result = d.showDialog();

	    if (result == null)
	      {
		return;
	      }
	  }

	try
	  {
	    writer = new PrintWriter(new BufferedWriter(new FileWriter(file)));
	  }
	catch (java.io.IOException e)
	  {
	    // "Trouble saving"
	    // "Could not open the file."
	    wp.gc.showErrorMessage(ts.l("sendReport.save_problem_subj"),
				   ts.l("sendReport.save_problem_text"));
	    return;
	  }

	writer.print(report.toString());
	writer.close();
      }
  }

  public void refreshQuery()
  {
    DumpResult buffer = null;

    /* -- */

    // "Querying server"
    setStatus(ts.l("refreshQuery.querying_status"), 0);

    if (query != null)
      {
	try
	  {
	    buffer = session.dump(query);
	  }
	catch (Exception ex)
	  {
	    this.wp.getgclient().processException(ex);
	  }

	if (buffer == null)
	  {
	    if (debug)
	      {
		System.err.println("null query dump result");
	      }

	    // "No results"
	    setStatus(ts.l("refreshQuery.no_result_status"));
	  }
	else
	  {
	    loadResults(buffer);
	  }
      }
  }

  /**
   *
   * This method loads the DumpResult into a table.  The DumpResult is
   * dissociated when this method is through with it, to aid GC.
   *
   */

  public void loadResults(DumpResult results)
  {
    boolean firstTime = true;
    boolean[] used;
    String[] headers;
    int [] colWidths;
    Invid invid;
    String tempString = null;
    Object cellResult;
    Object data = null;
    DateFormat format = new SimpleDateFormat("M/d/yyyy");
    int rows = 0;

    /* -- */

    // "Loading table"
    setStatus(ts.l("loadResults.loading_status"), 0);

    headerObjects = new Vector(results.getHeaderObjects());
    rows = results.resultSize();
    headers = new String[headerObjects.size()];
    used = new boolean[headerObjects.size()];

    if (debug)
      {
	System.err.println("gResultTable: " + headerObjects.size() + " headers returned by query");
      }
    
    for (int i = 0; i < headerObjects.size(); i++)
      {
	headers[i] = ((DumpResultCol) headerObjects.elementAt(i)).getName();
	used[i] = false;
      }

    colWidths = new int[headerObjects.size()];

    for (int i = 0; i < colWidths.length; i++)
      {
	colWidths[i] = 50;
      }

    // now we can initialize the table
    
    if (table == null)
      {
	table = new rowTable(colWidths, headers, this, popMenu, true);
	contentPane.add("Center", table);
      }
    else
      {
	table.reinitialize(colWidths, headers);
	firstTime = false;
      }

    // now read in all the result lines

    for (int i = 0; i < rows; i++)
      {
	invid = results.getInvid(i);
	table.newRow(invid);

	for (int j = 0; j < headers.length; j++)
	  {
	    cellResult = results.getResult(i, j);

	    if (cellResult == null)
	      {
		table.setCellText(invid, j, "", false);
	      }
	    else
	      {
		if (cellResult instanceof Integer)
		  {
		    tempString = cellResult.toString();
		    data = cellResult;
		  }
		else if (cellResult instanceof Double)
		  {
		    tempString = cellResult.toString();
		    data = cellResult;
		  }
		else if (cellResult instanceof Date)
		  {
		    tempString = format.format(cellResult);
		    data = cellResult;
		  }
		else if (cellResult instanceof String)
		  {
		    tempString = (String) cellResult;
		    data = null;
		  }
		else
		  {
		    System.err.println("ERROR! Unknown result type in cell " + i + "," + j + ": "+ cellResult);
		  }

		if (tempString.equals(""))
		  {
		    table.setCellText(invid, j, "", false);
		  }
		else
		  {
		    used[j] = true;
		    table.setCellText(invid, j, tempString, data, false);
		  }
	      }
	  }
      }

    // we're done with the results now.. break it apart and forget about it.

    results.dissociate();

    // we have to do this backwards so that we don't
    // change the index of a column we'll later delete

    for (int i = used.length-1; i >= 0 ; i--)
      {
	if (!used[i])
	  {
	    table.deleteColumn(i,true);
	  }
      }

    // sort by the first column in ascending order

    // "Sorting table on first column"
    setStatus(ts.l("loadResults.sorting_status"), 0);

    table.resort(0, true, false);

    // "Query Complete."
    setStatus(ts.l("loadResults.complete_status"));

    // the first time we're called, the table will not be visible, so we
    // don't want to refresh it here..

    if (!firstTime)
      {
	table.refreshTable();
      }

    // set our title with the result count

    String queryType = null;

    if (query.objectName != null)
      {
	queryType = query.objectName;
      }
    else if (query.objectType != -1)
      {
	queryType = wp.getgclient().loader.getObjectType(query.objectType);
      }
    else
      {
	// "<unknown>"
	queryType = ts.l("loadResults.unknown_query_type");
      }

    // "Query: [{0}] results: {1,num,#} entries"
    wp.setWindowTitle(this, ts.l("loadResults.window_title",
				 queryType,
				 new Integer(rows)));
  }

  public void rowSelected(Object key)
  {
  }

  public void rowDoubleSelected(Object key)
  {
  }

  public void rowUnSelected(Object key, boolean endSelected)
  {
  }

  public void rowMenuPerformed(Object key, java.awt.event.ActionEvent event)
  {
    if (event.getSource() == viewMI)
      {
	wp.getgclient().viewObject((Invid) key);
      }
    else if (event.getSource() == editMI)
      {
	wp.getgclient().editObject((Invid)key);
      }
    else if (event.getSource() == deleteMI)
      {
	wp.getgclient().deleteObject((Invid)key, true);
      }
    else if (event.getSource() == inactivateMI)
      {
	wp.getgclient().inactivateObject((Invid)key);
      }
    else if (event.getSource() == cloneMI)
      {
	wp.getgclient().cloneObject((Invid)key);
      }
  }

  public void colMenuPerformed(int menuCol, java.awt.event.ActionEvent event)
  {
    if (event.getActionCommand().equals(rowTable.delColStr))
      {
        DumpResultCol colHeader = (DumpResultCol) headerObjects.elementAt(menuCol);

        headerObjects.removeElementAt(menuCol);

        query.removeField(colHeader.getFieldId());
      }
  }

  /**
   *
   * This method generates an HTML representation of the table's
   * contents.
   *
   */

  StringBuffer generateHTMLRep()
  {
    StringBuffer result = new StringBuffer();
    Vector headers = table.getTableHeaders();
    int colcount = headers.size();
    int size = table.getRowCount();
    String cellText;
    String date = (new Date()).toString();

    /* -- */

    // we just have to hope that the table isn't going to
    // change while we're creating things.  There's actually
    // a possibility for problems here, since the user is
    // free to mess with the table while we work, but since
    // we are likely to be dispatched from the GUI thread,
    // this may not be an issue.

    result.append("<HTML>\n");
    result.append("<HEAD>\n");
    result.append("<TITLE>Ganymede Table Dump - ");
    result.append(date);
    result.append("</TITLE>\n");
    result.append("</HEAD>\n");
    result.append("<BODY BGCOLOR=\"#FFFFFF\">\n");
    result.append("<H1>Ganymede Table Dump - ");
    result.append(date);
    result.append("</H1>\n");
    result.append("<HR>\n");
    result.append("<TABLE BORDER>\n");
    result.append("<TR>\n");
    
    for (int i = 0; i < headers.size(); i++)
      {
	result.append("<TH>");
	result.append(headers.elementAt(i));
	result.append("</TH>\n");
      }

    result.append("</TR>\n");

    for (int i = 0; i < size; i++)
      {
	result.append("<TR>\n");

	for (int j = 0; j < colcount; j++)
	  {
	    result.append("<TD>");

	    cellText = table.getCellText(j, i);

	    if (cellText != null)
	      {
		cellText = escapeHTML(cellText);
		result.append(cellText);
	      }

	    result.append("</TD>\n");
	  }

	result.append("</TR>\n");
      }

    result.append("</TABLE><HR></BODY></HTML>\n");

    return result;
  }

  /**
   * This method generates a sepChar-separated dump of the table's
   * contents, one line per row of the table.
   */

  StringBuffer generateTextRep(char sepChar)
  {
    StringBuffer result = new StringBuffer();
    Vector headers = table.getTableHeaders();
    int colcount = headers.size();
    int size = table.getRowCount();
    String cellText;

    /* -- */

    // we just have to hope that the table isn't going to
    // change while we're creating things.  There's actually
    // a possibility for problems here, since the user is
    // free to mess with the table while we work, but since
    // we are likely to be dispatched from the GUI thread,
    // this may not be an issue.

    for (int i = 0; i < size; i++)
      {
	for (int j = 0; j < colcount; j++)
	  {
	    if (j > 0)
	      {
		result.append(sepChar);
	      }

	    cellText = table.getCellText(j, i);

	    if (cellText != null)
	      {
		cellText = escapeString(cellText, sepChar);
		result.append(cellText);
	      }
	  }

	result.append("\n");
      }

    return result;
  }

  /**
   * This method generates a Ganymede XML data file containing the
   * table's contents.
   */

  StringBuffer generateXMLReport() throws RemoteException
  {
    ReturnVal retVal = session.runXMLQuery(query);

    if (!retVal.didSucceed())
      {
        this.wp.getgclient().handleReturnVal(retVal);
        return null;
      }

    FileTransmitter transmitter = retVal.getFileTransmitter();

    if (transmitter != null)
      {
        ByteArrayOutputStream outstream = new ByteArrayOutputStream();
        byte[] chunk = transmitter.getNextChunk();

        while (chunk != null)
          {
            outstream.write(chunk, 0, chunk.length);
            chunk = transmitter.getNextChunk();
          }

        transmitter.end();

        return new StringBuffer(outstream.toString());
      }

    return null;
  }

  /**
   *
   * This helper method makes a field string safe to emit
   * to a sepChar separated text file.
   *
   */

  String escapeString(String string, char sepChar)
  {
    char[] chars;
    StringBuffer buffer = new StringBuffer();

    /* -- */

    chars = string.toCharArray();

    for (int j = 0; j < chars.length; j++)
      {
	if (chars[j] == '\\')
	  {
	    buffer.append("\\\\");
	  }
	else if (chars[j] == '\t')
	  {
	    buffer.append("    ");
	  }
	else if (chars[j] == '\n')
	  {
	    buffer.append("\\n");
	  }
	else if (chars[j] == sepChar)
	  {
	    buffer.append("\\");
	    buffer.append(sepChar);
	  }
	else
	  {
	    buffer.append(chars[j]);
	  }
      }

    return buffer.toString();
  }

  /**
   *
   * This helper method makes a field string safe to emit
   * to an HTML file.
   *
   */

  String escapeHTML(String string)
  {
    char[] chars;
    StringBuffer buffer = new StringBuffer();

    /* -- */

    chars = string.toCharArray();

    for (int j = 0; j < chars.length; j++)
      {
	if (chars[j] == '<')
	  {
	    buffer.append("&lt;");
	  }
	else if (chars[j] == '>')
	  {
	    buffer.append("&gt;");
	  }
	else
	  {
	    buffer.append(chars[j]);
	  }
      }

    return buffer.toString();
  }

  public void dispose()
  {
    if (table != null)
      {
        table.dispose();
        table = null;
      }
  }

  // -- helper functions

  private final void setStatus(String s)
  {
    wp.gc.setStatus(s);
  }
  
  private final void setStatus(String s, int timeLimit)
  {
    wp.gc.setStatus(s, timeLimit);
  }

  /**
   * Creates and initializes the JInternalFrame's toolbar.
   */

  private JToolBar createToolBar()
  {
    Image mailIcon = PackageResources.getImageResource(this, "queryTB_mail.gif", getClass());
    Image saveIcon = PackageResources.getImageResource(this, "queryTB_save.gif", getClass());
    Image refreshIcon = PackageResources.getImageResource(this, "queryTB_refresh.gif", getClass());

    Insets insets = new Insets(0,0,0,0);
    JToolBar toolBarTemp = new JToolBar();

    toolBarTemp.setBorderPainted(true);
    toolBarTemp.setFloatable(false);
    toolBarTemp.setMargin(insets);

    // "Mail"
    JButton b = new JButton(ts.l("createToolBar.mail_button"), new ImageIcon(mailIcon));

    // "M"
    if (ts.hasPattern("createToolBar.mail_button_mnemonic_optional"))
      {
	b.setMnemonic((int) ts.l("createToolBar.mail_button_mnemonic_optional").charAt(0));
      }

    b.setFont(new Font("SansSerif", Font.PLAIN, 10));
    b.setMargin(insets);
    b.setActionCommand("Mail Report");
    b.setVerticalTextPosition(b.BOTTOM);
    b.setHorizontalTextPosition(b.CENTER);

    // "Email results"
    if (ts.hasPattern("createToolBar.mail_button_tooltip_optional"))
      {
	b.setToolTipText(ts.l("createToolBar.mail_button_tooltip_optional"));
      }

    b.addActionListener(this);
    toolBarTemp.add(b);

    // No save feature if running from applet
    if (!glogin.isApplet())
      {
	// "Save"
	b = new JButton(ts.l("createToolBar.save_button"), new ImageIcon(saveIcon));

	// "S"
	if (ts.hasPattern("createToolBar.save_button_mnemonic_optional"))
	  {
	    b.setMnemonic((int) ts.l("createToolBar.save_button_mnemonic_optional").charAt(0));
	  }

	b.setFont(new Font("SansSerif", Font.PLAIN, 10));
	b.setMargin(insets);
	b.setActionCommand("Save Report");
	b.setVerticalTextPosition(b.BOTTOM);
	b.setHorizontalTextPosition(b.CENTER);

	// "Save results"
	if (ts.hasPattern("createToolBar.save_button_tooltip_optional"))
	  {
	    b.setToolTipText(ts.l("createToolBar.save_button_tooltip_optional"));
	  }

	b.addActionListener(this);
	toolBarTemp.add(b);
      }
    
    // "Refresh"
    b = new JButton(ts.l("createToolBar.refresh_button"), new ImageIcon(refreshIcon));

    // "R"
    if (ts.hasPattern("createToolBar.refresh_button_mnemonic_optional"))
      {
	b.setMnemonic((int) ts.l("createToolBar.refresh_button_mnemonic_optional").charAt(0));
      }

    b.setFont(new Font("SansSerif", Font.PLAIN, 10));
    b.setMargin(insets);
    b.setActionCommand("Refresh Query");
    b.setVerticalTextPosition(b.BOTTOM);
    b.setHorizontalTextPosition(b.CENTER);

    // "Refresh query"
    if (ts.hasPattern("createToolBar.refersh_button_tooltip_optional"))
      {
	b.setToolTipText(ts.l("createToolBar.refresh_button_tooltip_optional"));
      }

    b.addActionListener(this);
    toolBarTemp.add(b);

    return toolBarTemp;
  }

  JToolBar getToolBar() 
  {
    return toolbar;
  }

  /**
   * Give access to the protected paramString() method of our
   * ancestors for debug.
   */

  public String paramString()
  {
    return super.paramString();
  }
}
