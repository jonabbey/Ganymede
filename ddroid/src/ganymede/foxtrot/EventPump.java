/**
 * Copyright (c) 2002, Simone Bordet
 * All rights reserved.
 *
 * This software is distributable under the BSD license.
 * See the terms of the BSD license in the documentation provided with this software.
 */

package foxtrot;

/**
 * {@link Worker} uses an implementation of this interface to pump AWT events from the
 * standard AWT Event Queue while executing {@link Task}s. <br>
 * Implementations are required to provide a parameterless public constructor.
 *
 * @author <a href="mailto:biorn_steedom@users.sourceforge.net">Simone Bordet</a>
 * @version $Revision: 1.2 $
 */
public interface EventPump
{
   /**
    * Pumps AWT events from the standard AWT Event Queue and dispatches the events until the
    * given <code>task</code> is {@link Task#isCompleted completed}; <strong>must</strong> be
    * called from the Event Dispatch Thread. <br>
    * No Exceptions (included RuntimeExceptions) or Errors should escape this method:
    * it must return <strong>only</strong> when the task is completed.
    */
   public void pumpEvents(Task task);
}
