/*

   ContractUpdateTask.java

   This class is intended to dump the Ganymede datastore to GASH.
   
   Created: 9 March 1999
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

package arlut.csd.ganymede.gasharl;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.Vector;

import arlut.csd.ganymede.common.Invid;
import arlut.csd.ganymede.common.NotLoggedInException;
import arlut.csd.ganymede.common.Query;
import arlut.csd.ganymede.common.QueryDataNode;
import arlut.csd.ganymede.common.Result;
import arlut.csd.ganymede.server.DBEditObject;
import arlut.csd.ganymede.server.GanymedeSession;

/*------------------------------------------------------------------------------
                                                                           class
                                                              ContractUpdateTask

------------------------------------------------------------------------------*/

/**
 *
 * This class is a task for scanning a text file describing available contracts
 * generated by the ARL personnel office.
 *
 *
 * @author Jonathan Abbey jonabbey@arlut.utexas.edu
 *
 */

public class ContractUpdateTask implements Runnable {

  static final boolean debug = true;
  Calendar myCal = Calendar.getInstance();

  /* -- */

  public final synchronized void run()
  {
    String line;

    int index, index2;

    String name;
    String number;
    Date startDate;
    Date stopDate;
    String description;

    GanymedeSession session = null;

    BufferedReader in = null;

    DBEditObject object = null;

    /* -- */

    try
      {
	try
	  {
	    session = new GanymedeSession();
	    session.openTransaction("ContractUpdateTask");
	    session.enableOversight(false);
	    session.enableWizards(false);
	  }
	catch (java.rmi.RemoteException ex)
	  {
	    throw new RuntimeException("bizarro remote exception " + ex);
	  }
	
	try
	  {
	    in = new BufferedReader(new FileReader("/home/broccol/activecontracts.dat"));

	    line = in.readLine();

	    while (line != null)
	      {
		index = 0;

		index2 = line.indexOf(';', index);
		name = line.substring(index, index2);
		index = index2 + 1;

		index2 = line.indexOf(';', index);
		number = line.substring(index, index2);
		index = index2 + 1;

		index2 = line.indexOf(';', index);
		startDate = dateParse(line.substring(index, index2));
		index = index2 + 1;

		index2 = line.indexOf(';', index);
		stopDate = dateParse(line.substring(index, index2));
		index = index2 + 1;

		index2 = line.indexOf(';', index);
		description = line.substring(index, index2);

		if (debug)
		  {
		    System.err.println("Name: " + name + "\n" +
				       "Number: " + number + "\n" +
				       "Start: " + startDate + "\n" +
				       "Stop: " + stopDate + "\n" +
				       "Description: " + description + "\n");
		  }

		if (session != null)
		  {
		    Query q = new Query((short) 280, new QueryDataNode((short) 257, QueryDataNode.EQUALS, number), false);

		    Vector results = session.internalQuery(q);

		    if (results.size() == 1)
		      {
			Invid x = ((Result) results.elementAt(0)).getInvid();

			object = (DBEditObject) session.edit_db_object(x).getObject();

			if (debug)
			  {
			    System.err.println("Editing existing " + object);
			  }
		      }
		    else
		      {
			object = (DBEditObject) session.create_db_object((short) 280).getObject();

			if (debug)
			  {
			    System.err.println("Creating contract " + name);
			  }
		      }

		    object.setFieldValueLocal((short) 256, name);
		    object.setFieldValueLocal((short) 257, number);
		    object.setFieldValueLocal((short) 258, startDate);
		    object.setFieldValueLocal((short) 259, stopDate);
		    object.setFieldValueLocal((short) 260, description);
		  }
	    
		line = in.readLine();
	      }
	  }
	catch (IOException ex)
	  {
	  }
      }
    finally
      {
	// we need the finally in case our thread is stopped

	if (session != null)
	  {
	    try
	      {
		session.commitTransaction();
		session.logout();
	      }
	    catch (NotLoggedInException ex)
	      {
		// internal session, won't happen
	      }
	  }

	if (in != null)
	  {
	    try
	      {
		in.close();
	      }
	    catch (IOException ex)
	      {
		System.err.println("unknown IO exception caught: " + ex);
	      }
	  }
      }
  }

  /**
   *
   * The ARL activecontracts.dat file uses dates like
   *
   * 19990531
   *
   */

  private Date dateParse(String input) throws NumberFormatException
  {
    int year = Integer.parseInt(input.substring(0,4));
    int month = Integer.parseInt(input.substring(4,6));
    int day = Integer.parseInt(input.substring(6,8));

    if (year == 0)
      {
	return null;
      }

    myCal.clear();
    myCal.set(year, month-1, day);

    return myCal.getTime();
  }

  public static void main(String argv[])
  {
    new ContractUpdateTask().run();
  }
}
