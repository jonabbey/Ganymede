/*

   gResultTable.java

   This module is designed to provide a tabular view of the results
   of a query.
   
   Created: 14 July 1997
   Version: $Revision: 1.6 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.client;

import java.awt.*;
import java.awt.event.*;
import java.rmi.*;
import java.util.*;

import arlut.csd.ganymede.*;
import arlut.csd.JTable.*;
import arlut.csd.Util.*;

import com.sun.java.swing.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                    gResultTable

------------------------------------------------------------------------------*/

public class gResultTable extends JInternalFrame implements rowSelectCallback, ActionListener {

  windowPanel parent;
  PopupMenu popMenu;
  MenuItem viewMI;
  MenuItem editMI;
  Session session;
  Query query;
  rowTable table = null;
  JButton refreshButton;

  /* -- */

  public gResultTable(windowPanel parent, Session session, Query query, String results) throws RemoteException
  {
    super();			// JInternalFrame init

    this.setTitle("Query Results");

    this.parent = parent;
    this.session = session;
    this.query = query;

    popMenu = new PopupMenu();
    viewMI = new MenuItem("View Entry");
    editMI = new MenuItem("Edit Entry");

    popMenu.add(viewMI);
    popMenu.add(editMI);

    setLayout(new BorderLayout());

    JPanel buttonPanel = new JPanel();
    refreshButton = new JButton("Refresh Query");
    refreshButton.addActionListener(this);
    buttonPanel.add(refreshButton);
    
    add("South", buttonPanel);

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
    StringBuffer buffer = null;

    /* -- */

    Vector results = null;

    parent.parent.setStatus("Querying server");

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
	    parent.parent.setStatus("No results");
	  }
	else
	  {
	    loadResults(buffer.toString());
	  }
      }
  }

  public void loadResults(String results)
  {
    boolean firstTime = true;
    boolean[] used;
    Vector headerVect = new Vector();
    String[] headers;
    int [] colWidths;
    int index = 0;
    char[] chars;
    StringBuffer tempString = new StringBuffer();
    Invid invid;
    int col;
    int rows = 0;

    /* -- */

    System.err.println("result size = " + results.length());

    parent.parent.setStatus("Loading table");

    chars = results.toCharArray();

    // read in the header definition line

    System.err.println("Reading in header line");
    
    while (chars[index] != '\n')
      {
	tempString.setLength(0); // truncate the buffer

	while (chars[index] != '|')
	  {
	    if (chars[index] == '\n')
	      {
		throw new RuntimeException("parse error in header list");
	      }

	    // if we have a backslashed character, take the backslashed char
	    // as a literal

	    if (chars[index] == '\\')
	      {
		index++;
	      }

	    tempString.append(chars[index++]);
	  }

	index++;		// skip past |

	//	System.err.println("Header[" + headerVect.size() + "]: " + tempString.toString());

	headerVect.addElement(tempString.toString());
      }

    index++;			// skip past \n

    headers = new String[headerVect.size()];
    used = new boolean[headerVect.size()];
    
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
	add("Center", table);
      }
    else
      {
	table.reinitialize(colWidths, headers);
	firstTime = false;
      }

    // now read in all the result lines

    while (index < chars.length)
      {
	// first read in the Invid

	tempString.setLength(0); // truncate the buffer

	// System.err.println("Parsing row " + rows++);

	while (chars[index] != '|')
	  {
	    // if we have a backslashed character, take the backslashed char
	    // as a literal
	    
	    if (chars[index] == '\n')
	      {
		throw new RuntimeException("parse error in row");
	      }
	    
	    tempString.append(chars[index++]);
	  }

	//	System.err.println("Invid string: " + tempString.toString());

	invid = new Invid(tempString.toString());

	table.newRow(invid);

	index++;		// skip over |

	// now read in the fields

	col = 0;

	while (chars[index] != '\n')
	  {
	    tempString.setLength(0); // truncate the buffer

	    while (chars[index] != '|')
	      {
		// if we have a backslashed character, take the backslashed char
		// as a literal

		if (chars[index] == '\n')
		  {
		    throw new RuntimeException("parse error in header list");
		  }

		if (chars[index] == '\\')
		  {
		    index++;
		  }

		tempString.append(chars[index++]);
	      }

	    index++;		// skip |

	    //	    System.err.println("val: " + tempString.toString());

	    if (tempString.toString() == null || tempString.toString().equals(""))
	      {
		table.setCellText(invid, col++, "", false);
	      }
	    else
	      {
		used[col] = true;
		table.setCellText(invid, col++, tempString.toString(), false);
	      }
	  }

	index++; // skip newline
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

    parent.parent.setStatus("Sorting table on first column");

    table.resort(0, true, false);

    parent.parent.setStatus("Query Complete.");

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
	    parent.addWindow(session.view_db_object((Invid) key));
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
	    parent.addWindow(eObj,true);
	  }
      }
  }
}
