/*

   gResultTable.java

   This module is designed to provide a tabular view of the results
   of a query.
   
   Created: 14 July 1997
   Release: $Name:  $
   Version: $Revision: 1.33 $
   Last Mod Date: $Date: 2001/05/30 22:56:57 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999  The University of Texas at Austin.

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
   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.

*/

package arlut.csd.ganymede.client;

import java.awt.*;
import java.awt.event.*;
import java.rmi.*;
import java.io.*;
import java.util.*;
import java.text.*;

import arlut.csd.ganymede.*;
import arlut.csd.JTable.*;
import arlut.csd.JDialog.*;
import arlut.csd.Util.*;

import javax.swing.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                    gResultTable

------------------------------------------------------------------------------*/

/**
 * <p>Client internal window for displaying the results of a 
 * query {@link arlut.csd.ganymede.Session#dump(arlut.csd.ganymede.Query) dump}
 * in a table form.</p>
 *
 * <p>This window is created when {@link arlut.csd.ganymede.client.windowPanel windowPanel}'s
 * {@link arlut.csd.ganymede.client.windowPanel#addTableWindow(arlut.csd.ganymede.Session,
 * arlut.csd.ganymede.Query, arlut.csd.ganymede.DumpResult, java.lang.String) addTableWindow}
 * method is called.</p>
 * 
 * <p>Note that windowPanel's addTableWindow method is called from 
 * {@link arlut.csd.ganymede.client.gclient gclient}'s actionPerformed method,
 * which spawns a separate thread in which the query is performed and
 * the gResultTable window is created.</p>
 *
 * <p>Constructors for this class take a {@link arlut.csd.ganymede.Query Query} object
 * describing the query that this table was generated from, and a
 * {@link arlut.csd.ganymede.DumpResult DumpResult} object actually containing the dump
 * results from the Ganymede server.  gResultTable can resubmit the dump query to the
 * server if the user chooses to refresh the query, but normally the dump query
 * is performed by gclient.</p>
 *
 * @version $Revision: 1.33 $ $Date: 2001/05/30 22:56:57 $ $Name:  $
 * @author Jonathan Abbey, jonabbey@arlut.utexas.edu
 */

public class gResultTable extends JInternalFrame implements rowSelectCallback, ActionListener {
  
  static final boolean debug = false;

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

    this.setTitle("Query Results");

    this.wp = wp;
    this.session = session;
    this.query = query;

    popMenu = new JPopupMenu();
    viewMI = new JMenuItem("View Entry");
    editMI = new JMenuItem("Edit Entry");
    cloneMI = new JMenuItem("Clone Entry");
    deleteMI = new JMenuItem("Delete Entry");
    inactivateMI = new JMenuItem("Inactivate Entry");

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
    if (event.getActionCommand().equals("Refresh Query"))
      {
	refreshQuery();
	toolbar.requestFocus();
	return;
      }

    if (event.getActionCommand().equals("Save Report"))
      {
	sendReport(false);
	toolbar.requestFocus();
	return;
      }
    
    if (event.getActionCommand().equals("Mail Report"))
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

    formatChoices.addElement("Tab separated ASCII");
    formatChoices.addElement("Comma separated ASCII");
    formatChoices.addElement("HTML");

    dialog.setFormatChoices(formatChoices);
    
    if (!dialog.showDialog())
      {
	return;
      }

    format = dialog.getFormat();

    if (format.equals("HTML"))
      {
	report = generateHTMLRep();
      }
    else if (format.equals("Comma separated ASCII"))
      {
	report = generateTextRep(',');
      }
    else if (format.equals("Tab separated ASCII"))
      {
	report = generateTextRep('\t');
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
	    if (format.equals("HTML"))
	      {
		session.sendHTMLMail(addresses, dialog.getSubject(), null, report);
	      }
	    else
	      {
		session.sendMail(addresses, dialog.getSubject(), report);
	      }
	  }
	catch (RemoteException ex)
	  {
	    wp.gc.showErrorMessage("Trouble sending report", "Could not send mail.");
	    throw new RuntimeException("Couldn't mail report " + ex);
	  }

	return;
      }
    else
      {
	JFileChooser chooser = new JFileChooser();
	File file;
	PrintWriter writer = null;

	chooser.setDialogType(JFileChooser.SAVE_DIALOG);
	chooser.setDialogTitle("Save Report As");

	int returnValue = chooser.showDialog(wp.gc, null);

	if (!(returnValue == JFileChooser.APPROVE_OPTION))
	  {
	    return;
	  }

	file = chooser.getSelectedFile();
    
	if (file.exists())
	  {
	    StringDialog d = new StringDialog(wp.gc, "Warning, file ", 
					      file.getName() + 
					      " exists.  Are you sure you want to replace this file?",
					      "Overwrite", "Cancel", null);
	    Hashtable result = d.DialogShow();

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
	    wp.gc.showErrorMessage("Trouble saving", "Could not open the file.");
	    return;
	  }

	writer.println(report.toString());
	writer.close();
      }
  }

  public void refreshQuery()
  {
    DumpResult buffer = null;

    /* -- */

    Vector results = null;

    setStatus("Querying server", 0);

    if (query != null)
      {
	try
	  {
	    buffer = session.dump(query);
	  }
	catch (RemoteException ex)
	  {
	    throw new RuntimeException("caught remote exception in refreshQuery " + ex);
	  }

	if (buffer == null)
	  {
	    if (debug)
	      {
		System.err.println("null query dump result");
	      }

	    setStatus("No results");
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
    Vector headerVect = new Vector();
    String[] headers;
    int [] colWidths;
    Invid invid;
    String tempString = null;
    Object cellResult;
    Object data = null;
    DateFormat format = new SimpleDateFormat("M/d/yyyy");

    /* -- */

    setStatus("Loading table", 0);

    headerVect = results.getHeaders();
    headers = new String[headerVect.size()];
    used = new boolean[headerVect.size()];

    if (debug)
      {
	System.err.println("gResultTable: " + headers.length + " headers returned by query");
      }
    
    for (int i = 0; i < headers.length; i++)
      {
	headers[i] = (String) headerVect.elementAt(i);
	used[i] = false;
      }

    colWidths = new int[headerVect.size()];

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

    for (int i = 0; i < results.resultSize(); i++)
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

    setStatus("Sorting table on first column", 0);

    table.resort(0, true, false);

    setStatus("Query Complete.");

    // the first time we're called, the table will not be visible, so we
    // don't want to refresh it here..

    if (!firstTime)
      {
	table.refreshTable();
      }
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
    Enumeration enum = table.keys();
    Object rowKey;
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

	rowKey = enum.nextElement();

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
   *
   * This method generates a sepChar-separated dump of the table's
   * contents, one line per row of the table.
   *
   */

  StringBuffer generateTextRep(char sepChar)
  {
    StringBuffer result = new StringBuffer();
    Vector headers = table.getTableHeaders();
    int colcount = headers.size();
    int size = table.getRowCount();
    Object rowKey;
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

    JButton b = new JButton("  Mail  ", new ImageIcon(mailIcon));
    b.setMnemonic('M');
    b.setFont(new Font("SansSerif", Font.PLAIN, 10));
    b.setMargin(insets);
    b.setActionCommand("Mail Report");
    b.setVerticalTextPosition(b.BOTTOM);
    b.setHorizontalTextPosition(b.CENTER);
    b.setToolTipText("Email results");
    b.addActionListener(this);
    toolBarTemp.add(b);

    // No save feature if running from applet
    if (!glogin.isApplet())
      {
	b = new JButton("  Save  ", new ImageIcon(saveIcon));
	b.setMnemonic('S');
	b.setFont(new Font("SansSerif", Font.PLAIN, 10));
	b.setMargin(insets);
	b.setActionCommand("Save Report");
	b.setVerticalTextPosition(b.BOTTOM);
	b.setHorizontalTextPosition(b.CENTER);
	b.setToolTipText("Save results");
	b.addActionListener(this);
	toolBarTemp.add(b);
      }
    
    b = new JButton("Refresh", new ImageIcon(refreshIcon));
    b.setMnemonic('R');
    b.setFont(new Font("SansSerif", Font.PLAIN, 10));
    b.setMargin(insets);
    b.setActionCommand("Refresh Query");
    b.setVerticalTextPosition(b.BOTTOM);
    b.setHorizontalTextPosition(b.CENTER);
    b.setToolTipText("Refresh query");
    b.addActionListener(this);
    toolBarTemp.add(b);

    return toolBarTemp;
  }

  JToolBar getToolBar() 
    {
      return toolbar;
    }
}
