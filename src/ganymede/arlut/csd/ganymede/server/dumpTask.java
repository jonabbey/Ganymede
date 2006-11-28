/*

   dumpTask.java

   Runnable class to do a journal sync.  Issued by the GanymedeScheduler

   Created: 20 May 2004

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

   Web site: http://www.arlut.utexas.edu/gash2
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

import java.io.IOException;

import arlut.csd.Util.TranslationService;

/*------------------------------------------------------------------------------
                                                                           class
                                                                        dumpTask

------------------------------------------------------------------------------*/

/**
 * <p>Runnable class to do a journal sync.  Issued by the 
 * {@link arlut.csd.ganymede.server.GanymedeScheduler GanymedeScheduler}.</p>
 */

class dumpTask implements Runnable {

  static TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.server.dumpTask");

  public dumpTask()
  {
  }

  public void run()
  {
    boolean started = false;
    boolean completed = false;
    boolean gotSemaphore = false;

    /* -- */

    try
      {
	if (Ganymede.db.journal.isClean())
	  {
	    Ganymede.debug(ts.l("deferring"));
	    return;
	  }

        String error = GanymedeServer.lSemaphore.increment();
	
        if (error != null)
          {
            Ganymede.debug(ts.l("semaphore_disabled", error));
            return;
          }
        else
          {
            gotSemaphore = true;
          }

	started = true;
	Ganymede.debug(ts.l("running", new Integer(Ganymede.db.journal.transactionsInJournal)));

	try
	  {
	    Ganymede.db.dump(Ganymede.dbFilename, true, false);
	  }
	catch (IOException ex)
	  {
	    Ganymede.debug(ts.l("dump_error", ex.getMessage()));
	  }
	catch (InterruptedException ex)
	  {
	    Ganymede.debug(ts.l("dump_interrupted_error", ex.getMessage()));
	  }

	completed = true;
      }
    finally
      {
	// we'll go through here if our task was stopped
	// note that the DBStore dump code will handle
	// thread death ok.

	if (started && !completed)
	  {
	    Ganymede.debug(ts.l("forced_stop"));
	  }

	if (gotSemaphore)
	  {
	    GanymedeServer.lSemaphore.decrement();
	  }

	Ganymede.debug(ts.l("completed"));
      }
  }
}
