/**
 * Copyright (c) 2002, Simone Bordet
 * All rights reserved.
 *
 * This software is distributable under the BSD license.
 * See the terms of the BSD license in the documentation provided with this software.
 */

package foxtrot.pumps;

import java.awt.AWTEvent;

/**
 * Filters AWT events pumped by {@link foxtrot.EventPump EventPump}s before they're dispatched.
 *
 * @see EventFilterable
 * @author <a href="mailto:biorn_steedom@users.sourceforge.net">Simone Bordet</a>
 * @version $Revision: 1.1 $
 */
public interface EventFilter
{
   /**
    * Callback called by {@link foxtrot.EventPump EventPump}s to filter the given AWT event. <br>
    * This method is called before the event is actually dispatched
    * but after the event has been removed from the EventQueue.
    *
    * @param event The event to filter
    * @return True if the event should be dispatched, false otherwise
    */
   public boolean accept(AWTEvent event);
}
