/*

   gResultTable.java

   This module is designed to provide a tabular view of the results
   of a query.
   
   Created: 14 July 1997
   Version: $Revision: 1.3 $ %D%
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

  public gResultTable(windowPanel parent, Session session, Query query, Vector results) throws RemoteException
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
    Vector results = null;

    if (query != null)
      {
	try
	  {
	    results = session.query(query);
	  }
	catch (RemoteException ex)
	  {
	    throw new RuntimeException("caught remote exception in refreshQuery " + ex);
	  }

	if (results != null)
	  {
	    loadResults(results);
	  }
      }
  }

  public void loadResults(Vector results)
  {
    int i, j;
    db_field[] fields;
    db_object obj;
    Hashtable fieldsPresent = new Hashtable();
    Result result;
    Enumeration enum;
    boolean firstTime = true;

    /* -- */

    System.err.println("result size = " + results.size());
    
    for (i = 0; i < results.size(); i++)
      {
	try
	  {
	    fields = ((Result) results.elementAt(i)).getObject().listFields();

	    System.err.println("result " + i + ", fields length = " + fields.length);

	    for (j = 0; j < fields.length; j++)
	      {
		System.err.println("field: " + fields[j].getName());
		if (!fieldsPresent.containsKey(fields[j].getName()))
		  {
		    fieldsPresent.put(fields[j].getName(), fields[j]);
		  }
	      }
	  }
	catch (RemoteException ex)
	  {
	    throw new RuntimeException("caught remote " + ex);
	  }
      }

    fields = new db_field[fieldsPresent.size()];

    System.err.println("fieldsPresent.size == " + fieldsPresent.size());

    enum = fieldsPresent.elements();

    i = 0;
    while (enum.hasMoreElements())
      {
	fields[i++] = (db_field) enum.nextElement();
      }
	
    // sort the fields by display order.. perhaps this should be done for us on
    // the server?

    (new QuickSort(fields,  
		   new arlut.csd.Util.Compare()
		   {
		     public int compare(Object a, Object b) 
		       {
			 db_field aF, bF;
			 
			 aF = (db_field) a;
			 bF = (db_field) b;
			     
			 try
			   {
			     if (aF.getDisplayOrder() < bF.getDisplayOrder())
			       {
				 return -1;
			       }
			     else if (aF.getDisplayOrder() > bF.getDisplayOrder())
			       {
				 return 1;
			       }
			     else
			       {
				 return 0;
			       }
			   }
			 catch (RemoteException ex)
			   {
			     throw new RuntimeException("couldn't compare fields " + ex);
			   }
		       }
		   }
		   )
    ).sort();
    
    // build up our headers.. note that here we're assuming that our first result will
    // have all the fields that any of the results do.. this is not really a valid
    // assumption.  To do it right, we'd need to check the Base for this object type
    // and get a list of field names that we can reasonably expect to see in the
    // result list.. 
    
    String[] headers = new String[fields.length];
    int [] colWidths = new int[fields.length];
    Hashtable colMap = new Hashtable();

    System.err.println("Found " + fields.length + " fields");

    for (i = 0; i < fields.length; i++)
      {
	try
	  {
	    colMap.put(new Short(fields[i].getDisplayOrder()), new Short((short) i));
	    headers[i] = fields[i].getName();
	    colWidths[i] = 50;
	  }
	catch (RemoteException ex)
	  {
	    throw new RuntimeException("caught remote " + ex);
	  }
      }
    
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

    enum = results.elements();

    while (enum.hasMoreElements())
      {
	result = (Result) enum.nextElement();

	obj = result.getObject();

	table.newRow(obj);

	try
	  {
	    fields = obj.listFields();

	    for (i = 0; i < fields.length; i++)
	      {
		table.setCellText(obj, 
				  ((Short) colMap.get(new Short(fields[i].getDisplayOrder()))).shortValue(), 
				  fields[i].getValueString(), false);
	      }
	  }
	catch (RemoteException ex)
	  {
	    throw new RuntimeException("caught remote " + ex);
	  }
      }

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
	parent.addWindow((db_object)key);
      }
    else if (event.getSource() == editMI)
      {
	db_object eObj = null;

	try
	  {
	    eObj = session.edit_db_object(((db_object) key).getInvid());
	  }
	catch (RemoteException ex)
	  {
	  }
	
	if (eObj != null)
	  {
	    parent.addWindow(eObj,true);
	  }
      }
  }
}
