/*

JythonTask.java

This class acts as a proxy for a separate Builder Task (a class that handles 
processing DDroid's data for use in DNS/LDAP/Mail server/etc.) that's written
in Jython. This task takes one parameter, a URI that points to the actual Jython
code. The code is downloaded, loaded, and executed as if it was any other 
DDroid native-Java task.

Created: 22 July 2004
Last Mod Date: $Date$
Last Revision Changed: $Rev$
Last Changed By: $Author$
SVN URL: $HeadURL$

Module By: Deepak Giridharagopal <deepak@arlut.utexas.edu>

-----------------------------------------------------------------------
      
Directory Droid Directory Management System

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
package arlut.csd.ddroid.server;

import java.util.Vector;

import org.python.core.PyException;
import org.python.core.PySystemState;
import org.python.util.PythonInterpreter;

import arlut.csd.ddroid.common.Invid;
import arlut.csd.ddroid.common.SchemaConstants;

/**
 * <p>
 * This class acts as a proxy for a separate Builder Task (a class that handles
 * processing DDroid's data for use in DNS/LDAP/Mail server/etc.) that's written
 * in Jython. This task doesn't do much on its own; it simply bootstraps a
 * Jython interpreter to a state where it can execute a "proper" builder task
 * written in Jython and stored at an external location.
 * </p>
 * <p>
 * Every JythonTask needs to be registered in the DDroid database via the task
 * object type. They all require a single option string: the URI of the "real"
 * builder task that is written in Jython. The URI can point to a resource using
 * any protocol the Jython <code>urllib</code> module can handle (http://,
 * file://, ftp://, etc).
 * </p>
 * <p>
 * The "real" task, written in Jython, is ideally a subclass of
 * {@link arlut.csd.ddroid.server.GanymedeBuilderTask GanymedeBuilderTask}
 * (though it can simply be any class that implements the
 * {@link java.lang.Runnable Runnable}interface). This class will bootstrap a
 * new Jython interpreter, load the class defined at the specified URI, and then
 * execute that task's {@link java.lang.Runnable.run run}method.
 * </p>
 * <p>
 * It's important to mention that <b>no state is stored in-between runs of a
 * Jython task</b>. A new interpreter is initialized every time the task is run,
 * and that interpreter is not shared by any other Jython task. So any variables
 * that exist solely in the context of the Jython interpreter should be considered
 * <b>transient</b> (not in the Java sense of the word, but in the English sense
 * of the word).
 * </p>
 */
public class JythonTask implements Runnable {

  /**
   * <p>The invid of this actual task definition. This is needed so we can snag this
   * task's option strings from the database.</p>
   */
  private Invid taskDefObjInvid;

  public JythonTask(Invid invid)
  {
    taskDefObjInvid = invid;
  }

  /**
   * <p>
   * This method is the one invoked when the scheduler runs this task. It creates a new
   * Jython interpreter, runs the bootstrapping code, and then executes the external
   * Jython task.
   * </p>
   */
  public final void run()
  {
    String uri;
    PythonInterpreter interp;
    
    uri = getURI();
    if (uri == null)
      {
        throw new RuntimeException("Unable to get URI of the the Jython code for this task.");
      }
    
    /* Initialize the interpreter */
    interp = new PythonInterpreter(null, new PySystemState());
    
    try
      {
        /* Import the additional Jython library routines */
        interp.exec("import sys");
        interp.exec("sys.path.append( sys.prefix + '" + System.getProperty("file.separator") + "' + 'jython-lib.jar' )");
       
        /* Launch the actual task */
        interp.exec("import JythonTaskBootstrapper");
        interp.set("uri", uri);
        interp.exec("JythonTaskBootstrapper.run(uri)");
      }
    catch (PyException pex)
      {
        throw new RuntimeException(pex.toString());
      }
      
    interp = null;    
  }
  
  /**
   * <p>
   * Parses this task's option string to grab the URI of the proxied Jython task.
   * If there is more than one option string defined, the first one wins.
   * </p>
   * 
   * @return The URI of the actual task (written in Jython) that does the work
   */
  public String getURI()
  {
    if (taskDefObjInvid != null)
      {
        DBObject taskDefObj = Ganymede.internalSession.session.viewDBObject(taskDefObjInvid);
        
        Vector options = taskDefObj.getFieldValuesLocal(SchemaConstants.TaskOptionStrings);

        if (options == null || options.size() == 0)
          {
            return null;
          }

        // dup the vector for safety, since we are getting direct
        // access to the Vector in the database

        return (String) options.firstElement();
      }

    return null;
  }
}
