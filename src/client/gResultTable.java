/*

   gResultTable.java

   This module is designed to provide a tabular view of the results
   of a query.
   
   Created: 14 July 1997
   Version: $Revision: 1.12 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.client;

import java.awt.*;
import java.awt.event.*;
import java.rmi.*;
import java.util.*;
import java.text.*;

import arlut.csd.ganymede.*;
import arlut.csd.JTable.*;
import arlut.csd.Util.*;

import com.sun.java.swing.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                    gResultTable

------------------------------------------------------------------------------*/

public class gResultTable extends JInternalFrame implements rowSelectCallback, ActionListener {

  windowPanel wp;
  PopupMenu popMenu;
  MenuItem viewMI;
  MenuItem editMI;
  Session session;
  Query query;
  rowTable table = null;
  JButton refreshButton;

  Container
    contentPane;

  /* -- */

  public gResultTable(windowPanel wp, Session session, Query query, DumpResult results) throws RemoteException
  {
    super();			// JInternalFrame init

    this.setTitle("Query Results");

    this.wp = wp;
    this.session = session;
    this.query = query;

    popMenu = new PopupMenu();
    viewMI = new MenuItem("View Entry");
    editMI = new MenuItem("Edit Entry");

    popMenu.add(viewMI);
    popMenu.add(editMI);

    contentPane = getContentPane();

    contentPane.setLayout(new BorderLayout());

    JPanel buttonPanel = new JPanel();
    refreshButton = new JButton("Refresh Query");
    refreshButton.addActionListener(this);
    buttonPanel.add(refreshButton);
    
    contentPane.add("South", buttonPanel);

    loadResults(results);
  }

  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == refreshButton)
      {
	refreshQuery();
      }
  }

  public void refreshQuery()
  {
    DumpResult buffer = null;

    /* -- */

    Vector results = null;

    setStatus("Querying server");

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
	    System.err.println("null query dump result");
	    setStatus("No results");
	  }
	else
	  {
	    loadResults(buffer);
	  }
      }
  }

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
    DateFormat format = DateFormat.getDateInstance(DateFormat.SHORT);

    /* -- */

    setStatus("Loading table");

    headerVect = results.getHeaders();
    headers = new String[headerVect.size()];
    used = new boolean[headerVect.size()];

    System.err.println("gResultTable: " + headers.length + " headers returned by query");
    
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
	table = new rowTable(colWidths, headers, this, popMenu);
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

    setStatus("Sorting table on first column");

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
	try
	  {
	    wp.addWindow(session.view_db_object((Invid) key));
	  }
	catch (RemoteException ex)
	  {
	    throw new RuntimeException("remote exception viewing table row " + ex);
	  }
      }
    else if (event.getSource() == editMI)
      {
	db_object eObj = null;

	try
	  {
	    eObj = session.edit_db_object((Invid) key);
	  }
	catch (RemoteException ex)
	  {
	    throw new RuntimeException("remote exception editing table row " + ex);
	  }
	
	if (eObj != null)
	  {
	    wp.addWindow(eObj,true);
	  }
      }
  }
  
  private final void setStatus(String s)
  {
    wp.gc.setStatus(s);
  }
}
