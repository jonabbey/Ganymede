/*

   userNetgroupCustom.java

   This file is a management class for user netgroup objects in
   Ganymede.
   
   Created: 3 April 2000
   Release: $Name:  $
   Version: $Revision: 1.1 $
   Last Mod Date: $Date: 2000/04/04 05:17:41 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999  The University of Texas at Austin.

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
   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.

*/

package arlut.csd.ganymede.custom;

import arlut.csd.ganymede.*;
import arlut.csd.Util.*;

import java.io.*;
import java.util.*;
import java.rmi.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                              userNetgroupCustom

------------------------------------------------------------------------------*/

public class userNetgroupCustom extends DBEditObject implements SchemaConstants,
								userNetgroupSchema {
  
  static final boolean debug = true;
  static final boolean debug2 = false;

  /**
   *
   * Customization Constructor
   *
   */

  public userNetgroupCustom(DBObjectBase objectBase)
  {
    super(objectBase);
  }

  /**
   *
   * Create new object constructor
   *
   */

  public userNetgroupCustom(DBObjectBase objectBase, Invid invid, DBEditSet editset)
  {
    super(objectBase, invid, editset);
  }

  /**
   *
   * Check-out constructor, used by DBObject.createShadow()
   * to pull out an object for editing.
   *
   */

  public userNetgroupCustom(DBObject original, DBEditSet editset)
  {
    super(original, editset);
  }

  /**
   *
   * Customization method to control whether a specified field
   * is required to be defined at commit time for a given object.<br><br>
   *
   * To be overridden in DBEditObject subclasses.
   * 
   */

  public boolean fieldRequired(DBObject object, short fieldid)
  {
    return (fieldid == NETGROUPNAME);
  }

  /**
   *
   * This method is a hook for subclasses to override to
   * pass the phase-two commit command to external processes.<br><br>
   *
   * For normal usage this method would not be overridden.  For
   * cases in which change to an object would result in an external
   * process being initiated whose success or failure would not
   * affect the successful commit of this DBEditObject in the
   * Ganymede server, the process invokation should be placed here,
   * rather than in commitPhase1().<br><br>
   *
   * Subclasses that override this method may wish to make this method 
   * synchronized.
   *
   * @see arlut.csd.ganymede.DBEditSet
   */

  public void commitPhase2()
  {
    switch (getStatus())
      {
      case DROPPING:
	break;

      case CREATING:
	break;

      case DELETING:
	handleGroupDelete(original.getLabel());
	break;
	
      case EDITING:
	String name = getLabel();
	String oldname = original.getLabel();

	if (!name.equals(oldname))
	  {
	    handleGroupRename(oldname, name);
	  }
      }

    return;
  }

  /**
   * This method handles external actions for deleting a user.
   */

  private void handleGroupDelete(String name)
  {
    String deleteFilename;
    File deleteHandler = null;
    boolean success = false;

    /* -- */

    // if the system log is null, we're running in the direct loader, and we
    // don't want to create anything external.

    // This would be unusual for a delete, but..

    if (Ganymede.log == null)
      {
	return;
      }

    if (debug)
      {
	System.err.println("userNetgroupCustom.handleGroupDelete(): group " + name +
			   "is being deleted");
      }

    deleteFilename = System.getProperty("ganymede.builder.scriptlocation");

    if (deleteFilename != null)
      {
	// make sure we've got the path separator at the end of
	// deleteFilename, add our script name
	
	deleteFilename = PathComplete.completePath(deleteFilename) + "/scripts/user_netgroup_deleter";
	
	deleteHandler = new File(deleteFilename);
      }
    else
      {
	Ganymede.debug("userNetgroupCustom.handleGroupDelete(): Couldn't find " +
		       "ganymede.builder.scriptlocation property");
      }

    if (deleteHandler.exists())
      {
	try
	  {
	    String execLine = deleteFilename + " " + name;

	    if (debug)
	      {
		System.err.println("handleGroupDelete: running " + execLine);
	      }

	    int result;
	    Process p = java.lang.Runtime.getRuntime().exec(execLine);

	    try
	      {
		if (debug)
		  {
		    System.err.println("handleGroupDelete: blocking");
		  }

		p.waitFor();

		if (debug)
		  {
		    System.err.println("handleGroupDelete: done");
		  }

		result = p.exitValue();

		if (result != 0)
		  {
		    Ganymede.debug("Couldn't handle externals for deleting group " + name + 
				   "\n" + deleteFilename + 
				   " returned a non-zero result: " + result);
		  }

		success = true;
	      }
	    catch (InterruptedException ex)
	      {
		Ganymede.debug("Couldn't handle externals for deleting group " + name + 
			       ex.getMessage());
	      }
	  }
	catch (IOException ex)
	  {
	    Ganymede.debug("Couldn't handle externals for deleting group " + name + 
			   ex.getMessage());
	  }
      }
  }

  /**
   * This method handles external actions for renaming a group.
   */

  private void handleGroupRename(String orig, String newname)
  {
    String renameFilename;
    File renameHandler = null;

    /* -- */

    // if the system log is null, we're running in the direct loader, and we
    // don't want to create anything external.

    if (Ganymede.log == null)
      {
	return;
      }

    if (debug)
      {
	System.err.println("userNetgroupCustom.handleGroupRename(): user " + orig +
			   "is being renamed to " + newname);
      }

    renameFilename = System.getProperty("ganymede.builder.scriptlocation");

    if (renameFilename != null)
      {
	// make sure we've got the path separator at the end of
	// renameFilename, add our script name
	    
	renameFilename = PathComplete.completePath(renameFilename) + "/scripts/user_netgroup_namer";
	    
	renameHandler = new File(renameFilename);
      }
    else
      {
	Ganymede.debug("userNetgroupCustom.handleGroupRename(): Couldn't find " +
		       "ganymede.builder.scriptlocation property");
      }

    if (renameHandler.exists())
      {
	try
	  {
	    String execLine = renameFilename + " " + orig + " " + newname;

	    if (debug)
	      {
		System.err.println("handleGroupRename: running " + execLine);
	      }

	    int result;
	    Process p = java.lang.Runtime.getRuntime().exec(execLine);

	    try
	      {
		if (debug)
		  {
		    System.err.println("handleGroupRename: blocking");
		  }

		p.waitFor();

		if (debug)
		  {
		    System.err.println("handleGroupRename: done");
		  }

		result = p.exitValue();

		if (result != 0)
		  {
		    Ganymede.debug("Couldn't handle externals for renaming group " + orig + 
				   " to " + newname + "\n" + renameFilename + 
				   " returned a non-zero result: " + result);
		  }
	      }
	    catch (InterruptedException ex)
	      {
		Ganymede.debug("Couldn't handle externals for renaming group " + orig + 
			       " to " + 
			       newname + "\n" + 
			       ex.getMessage());
	      }
	  }
	catch (IOException ex)
	  {
	    Ganymede.debug("Couldn't handle externals for renaming group " + orig + 
			   " to " + 
			   newname + "\n" + 
			   ex.getMessage());
	  }
      }
  }
}
