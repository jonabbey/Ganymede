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
   * <p>The entrypoint used by Thread.UncaughtExceptionHandler</p>
   */

  public void uncaughtException(Thread t, Throwable ex)
  {
    this.reportException(t, null, ex);
  }

  /**
   * <p>Used to log and report the exception passed.</p>
   */

  public void reportException(Throwable ex)
  {
    this.reportException(Thread.currentThread(), null, ex);
  }

  /**
   * <p>Used to log and report the exception passed with the context
   * message contextMesg.</p>
   */

  public void reportException(String contextMesg, Throwable ex)
  {
    this.reportException(Thread.currentThread(), contextMesg, ex);
  }

  /**
   * <p>Used to report an exception ex on thread t with context
   * message contextMesg.</p>
   *
   * <p>If reportException() somehow caused the exception passed in,
   * we will not re-process it in full, but instead will simply print
   * it to stderr in an attempt to avoid perseveration on the
   * exception.</p>
   */

  public void reportException(Thread t, String contextMesg, Throwable ex)
  {
    // We need to make sure that we didn't somehow cause the exception
    // we're responding.
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

    String mesg = null;
    String trace = Ganymede.stackTrace(ex);

    if (contextMesg != null)
      {
	mesg = contextMesg + "\n\n" + trace;
      }
    else
      {
	mesg = trace;
      }

    try
      {
	// everything here may fail if the server hasn't proceeded far
	// enough in startup.

	if (!StringUtils.isEmpty(Ganymede.bugReportAddressProperty))
	  {
	    try
	      {
		StringBuffer bugReport = new StringBuffer();

		bugReport.append("\nSERVER ERROR DETECTED:\nexception trace == \"");
		bugReport.append(trace);
		bugReport.append("\"\n");

		Ganymede.internalSession.sendMail(Ganymede.bugReportAddressProperty, "Ganymede Server Bug Report", bugReport);
	      }
	    catch (Throwable ex2)
	      {
	      }
	  }

	GanymedeAdmin.logAppend(mesg);
      }
    catch (Throwable th)
      {
      }
    finally
      {
	System.err.println(mesg);
      }
  }
}
