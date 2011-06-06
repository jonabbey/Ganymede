/*

   GanymedeUncaughtExceptionHandler.java

   Created: 26 June 2008


   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996-2011
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
   along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/

package arlut.csd.ganymede.server;

import arlut.csd.Util.StringUtils;

/*------------------------------------------------------------------------------
                                                                           class
                                                GanymedeUncaughtExceptionHandler

------------------------------------------------------------------------------*/

/**
 * <p>This class is used to provide the default uncaught exception
 * handler for the Ganymede server.  With an object of this class
 * bound as the system's default Thread.UncaughtExceptionHandler, any
 * exceptions which lead to thread death on the server can be properly
 * logged and reported.</p>
 *
 * <p>GanymedeUncaughtExceptionHandler requires Java 5 to function,
 * but we don't want to make the Ganymede code base depend on Java 5
 * yet.  As such, the Ganymede server's startup sequence uses Java
 * Reflection to create and register this handler if we are using an
 * appropriate version of Java.</p>
 *
 * @author Jonathan Abbey, jonabbey@arlut.utexas.edu
 */

public class GanymedeUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

  /**
   * <p>We call this static method through Java Reflection in the
   * Ganymede server startup path.  That allows the Ganymede server
   * startup to dynamically decide whether
   * GanymedeUncaughtExceptionHandler was compiled or not, without
   * making the compilation of arlut.csd.ganymede.server.Ganymede
   * itself dependent on whether or not this class was compiled at
   * build time.</p>
   */

  public static void setup()
  {
    Thread.setDefaultUncaughtExceptionHandler(new GanymedeUncaughtExceptionHandler());
  }

  // ---

  public void uncaughtException(Thread t, Throwable ex)
  {
    // We need to make sure that we didn't somehow cause the exception
    // we're responding to by calling Ganymede.logError().
    //
    // If we detect that this method previously participated in
    // throwing this exception, we'll just print it out and return,
    // rather than doing whatever Ganymede.logError() does.

    StackTraceElement traces[] = ex.getStackTrace();

    for (StackTraceElement traceElement: traces)
      {
	if (traceElement.getClassName().equals("arlut.csd.ganymede.server.GanymedeUncaughtExceptionHandler") &&
	    traceElement.getMethodName().equals("uncaughtException"))
	  {
	    try
	      {
		System.err.println("Exception loop processing:\n");
		ex.printStackTrace();
	      }
	    finally
	      {
		return;
	      }
	  }
      }

    if (!StringUtils.isEmpty(Ganymede.bugReportAddressProperty))
      {
	try
	  {
	    StringBuffer bugReport = new StringBuffer();

	    bugReport.append("\nSERVER ERROR DETECTED:\nexception trace == \"");
	    bugReport.append(Ganymede.stackTrace(ex));
	    bugReport.append("\"\n");

	    Ganymede.internalSession.sendMail(Ganymede.bugReportAddressProperty, "Ganymede Server Bug Report", bugReport);
	  }
	catch (Throwable ex2)
	  {
	  }
      }

    Ganymede.logError(ex);
  }
}
