/*

   gResultTable.java

   This module is designed to provide a tabular view of the results
   of a query.
   
   Created: 14 July 1997
   Version: $Revision: 1.1 $ %D%
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

public class gResultTable extends JInternalFrame implements rowSelectCallback {

  public gResultTable(Session session, Vector results) throws RemoteException
  {
    super();			// JInternalFrame init

    // --

    Result result;
    Enumeration enum;
    db_field[] fields;
    db_object obj;
    int i, j;

    /* -- */

    setLayout(new BorderLayout());

    Hashtable fieldsPresent = new Hashtable();

    // get all fields present in our results

    System.err.println("result size = " + results.size());
    
    for (i = 0; i < results.size(); i++)
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
	colMap.put(new Short(fields[i].getDisplayOrder()), new Short((short) i));
	headers[i] = fields[i].getName();
	colWidths[i] = 50;
      }
    
    rowTable table = new rowTable(colWidths, headers, this, null);

    add("Center", table);

    enum = results.elements();

    while (enum.hasMoreElements())
      {
	result = (Result) enum.nextElement();

	obj = result.getObject();

	table.newRow(obj);

	fields = obj.listFields();

	for (i = 0; i < fields.length; i++)
	  {
	    table.setCellText(obj, 
			      ((Short) colMap.get(new Short(fields[i].getDisplayOrder()))).shortValue(), 
			      fields[i].getValueString(), false);
	  }
      }

    this.setTitle("Query Results");
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
  }
}
