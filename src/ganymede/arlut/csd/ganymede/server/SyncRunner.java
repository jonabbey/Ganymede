/*

   SyncRunner.java

   This class is used in the Ganymede server to handle sync channel
   synchronization.  It is responsible for acting as a Runnable in the
   Ganymede scheduler (when run, it executes the external service program for
   a given sync channel queue) and for tracking the data associated with the
   sync channel.
   
   Created: 2 February 2005
   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996-2005
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

package arlut.csd.ganymede.server;

import arlut.csd.ganymede.common.Invid;
import arlut.csd.ganymede.common.SchemaConstants;

import arlut.csd.Util.FileOps;
import arlut.csd.Util.TranslationService;

import java.io.File;
import java.io.IOException;

/*------------------------------------------------------------------------------
                                                                           class
                                                                      SyncRunner

------------------------------------------------------------------------------*/

/**
 * <p>This class is used in the Ganymede server to handle sync channel
 * synchronization.  It is responsible for acting as a Runnable in the
 * Ganymede scheduler (when run, it executes the external service program for
 * a given sync channel queue) and for tracking the data associated with the
 * sync channel.</p>
 */

public class SyncRunner implements Runnable {

  /**
   * <p>TranslationService object for handling string localization in
   * the Ganymede server.</p>
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.server.SyncRunner");

  private static Runtime runtime = null;

  // ---

  private Invid syncChannelInvid;
  private String name;
  private String directory;
  private String serviceProgram;
  private int transactionNumber;

  /* -- */

  public SyncRunner(DBObject syncChannel)
  {
    updateInfo(syncChannel);
  }

  public void updateInfo(DBObject syncChannel)
  {
    if (syncChannel.getID() != SchemaConstants.SyncChannelBase)
      {
	// "Error, passed the wrong kind of DBObject."
	throw new IllegalArgumentException(ts.l("updateInfo.typeError"));
      }

    this.syncChannelInvid = syncChannel.getInvid();
    this.name = (String) syncChannel.getFieldValueLocal(SchemaConstants.SyncChannelName);
    this.directory = (String) syncChannel.getFieldValueLocal(SchemaConstants.SyncChannelDirectory);
    this.serviceProgram = (String) syncChannel.getFieldValueLocal(SchemaConstants.SyncChannelServicer);
  }

  /**
   * <p>Used to set the number of the last transaction known to have been
   * safely persisted to disk (journal and sync channels) at the time this
   * method is called.</p>
   */

  public void setTransactionNumber(int trans)
  {
    if (trans >= this.transactionNumber)
      {
	this.transactionNumber = trans;
      }
    else
      {
	// "Error, can''t set the persisted transaction number to a lower number than previously seen."
	throw new IllegalArgumentException(ts.l("setTransactionNumber.badNumber"));
      }
  }

  /**
   * <p>Returns the number of the last transaction known to have been
   * safely persisted to disk (journal and sync channels) at the time this
   * method is called.</p>
   */

  public int getTransactionNumber()
  {
    return this.transactionNumber;
  }

  public String getName()
  {
    return name;
  }

  public String getDirectory()
  {
    return directory;
  }

  public String getServiceProgram()
  {
    return serviceProgram;
  }

  public void run()
  {
    int myTransactionNumber = getTransactionNumber();
    String myServiceProgram = getServiceProgram();
    String invocation = myServiceProgram + " " + String.valueOf(myTransactionNumber);

    File
      file;

    /* -- */

    // "SyncChannel {0} running"
    Ganymede.debug(ts.l("run.running", getName()));

    file = new File(myServiceProgram);

    if (file.exists())
      {
	if (runtime == null)
	  {
	    runtime = Runtime.getRuntime();
	  }

	try
	  {
	    FileOps.runProcess(invocation);
	  }
	catch (IOException ex)
	  {
	    // "Couldn''t exec SyncChannel {0}''s service program "{1}" due to IOException: {2}"
	    Ganymede.debug(ts.l("run.ioException", getName(), myServiceProgram, ex));
	  }
	catch (InterruptedException ex)
	  {
	    // "Failure during exec of SyncChannel {0}''s service program "{1}""
	    Ganymede.debug(ts.l("run.interrupted", getName(), myServiceProgram));
	  }
      }
    else
      {
	// ""{0}" doesn''t exist, not running external service program for SyncChannel {1}"
	Ganymede.debug(ts.l("run.nonesuch", myServiceProgram, getName()));
      }

    // "SyncChannel {1} finished"
    Ganymede.debug(ts.l("run.done", getName()));
  }
}
