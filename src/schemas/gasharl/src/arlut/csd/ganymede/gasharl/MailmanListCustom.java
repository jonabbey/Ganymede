/*

   MailmanListCustom.java

   Custom plug-in for managing fields in the MailmanList object type.

   Created: 25 June 1999
   Last Mod Date: $Date: 2004-12-01 01:53:51 -0600 (Wed, 01 Dec 2004) $
   Last Revision Changed: $Rev: 5857 $
   Last Changed By: $Author: broccol $
   SVN URL: $HeadURL: http://tools.arlut.utexas.edu/svn/ganymede/trunk/ganymede/src/schemas/gasharl/src/arlut/csd/ganymede/gasharl/MailmanListCustom.java $

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996 - 2004
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

import java.util.Vector;

import arlut.csd.ganymede.common.GanyPermissionsException;
import arlut.csd.ganymede.common.Invid;
import arlut.csd.ganymede.common.NotLoggedInException;
import arlut.csd.ganymede.common.ReturnVal;
import arlut.csd.ganymede.common.SchemaConstants;
import arlut.csd.ganymede.server.DBEditObject;
import arlut.csd.ganymede.server.DBEditSet;
import arlut.csd.ganymede.server.DBField;
import arlut.csd.ganymede.server.DBObject;
import arlut.csd.ganymede.server.DBObjectBase;
import arlut.csd.ganymede.server.DBSession;
import arlut.csd.ganymede.server.Ganymede;
import arlut.csd.ganymede.server.GanymedeSession;
import arlut.csd.ganymede.server.StringDBField;

/*------------------------------------------------------------------------------
									   class
							       MailmanListCustom

------------------------------------------------------------------------------*/

/**
 *   Custom plug-in for managing fields in the MailmanList object type.
 */

public class MailmanListCustom extends DBEditObject implements SchemaConstants, MailmanListSchema {

  /**
   *
   * Customization Constructor
   *
   */

  public MailmanListCustom(DBObjectBase objectBase)
  {
    super(objectBase);
  }

  /**
   *
   * Create new object constructor
   *
   */

  public MailmanListCustom(DBObjectBase objectBase, Invid invid, DBEditSet editset)
  {
    super(objectBase, invid, editset);
  }

  /**
   *
   * Check-out constructor, used by DBObject.createShadow()
   * to pull out an object for editing.
   *
   */

  public MailmanListCustom(DBObject original, DBEditSet editset)
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
    switch (fieldid)
      {
	case MailmanListSchema.NAME:
	case MailmanListSchema.OWNEREMAIL:
	case MailmanListSchema.PASSWORD:
	case MailmanListSchema.SERVER:
	case MailmanListSchema.ALIASES:

	return true;
      }

    return false;
  }



  /**
   * Handle the wizards for dealing with users that have this as the home group.
   */

  public ReturnVal wizardHook(DBField field, int operation, Object param1, Object param2)
  {
    ReturnVal retVal = null;

    if (field.getID() == NAME)
    {
	if (operation == SETVAL)
	{
	  retVal = handleCreateMailmanList((String) param1);
	}
    }

    return retVal;
  }

  /**
   * Handle the wizards for dealing with users that have this as the home group.
   * Used for XML non-interactive client
   */

  public ReturnVal preCommitHook()
  {
    switch (getStatus())
    {
      case DROPPING:
	break;

      case CREATING:
      case EDITING:

	// handle creating the aliases needed for the mailmanlist
	String name = getLabel();
	return handleCreateMailmanList(name);

      case DELETING:
	break;
    }

    return null;
  }


  /**
   * Handle dealing with Mailman Lists: created all the associated aliases.
   * Mailman needs 10 total email addresses to be registered.
   */

  public ReturnVal handleCreateMailmanList(String name)
  {
    ReturnVal retVal = new ReturnVal(true, true);
    StringDBField stringfield;

    stringfield = (StringDBField) getField(ALIASES);

    boolean succeed = false;

    // set a checkpoint so we can verify all the aliases
    // needed are not currently being used.
    String checkPointKey = "MailmanListAliases";
    DBSession dbSession = getGSession().getSession();
    dbSession.checkpoint(checkPointKey);

    try
      {
	while (stringfield.size() > 0)
	  {
	    retVal = stringfield.deleteElementLocal(0);

	    if (retVal != null && !retVal.didSucceed())
	      {
		return retVal;
	      }
	  }

	String[] aliases = {name+"-admin", name+"-bounce", name+"-confirm", name+"-join",
			    name+"-leave", name+"-owner", name+"-request", name+"-subscribe", name+"-unsubscribe"};

	for (int i = 0; i < aliases.length; i++)
	  {
	    retVal = stringfield.addElementLocal(aliases[i]);
	    if (retVal != null && !retVal.didSucceed())
	      {
		return retVal;
	      }
	  }

	succeed = true;
      }
    finally
      {
	if (succeed)
	  {
	    dbSession.popCheckpoint(checkPointKey);
	    
	    // tell client to refresh to see the reactivation results
	    retVal = new ReturnVal(true, true);
	    retVal.addRescanField(this.getInvid(), MailmanListSchema.ALIASES);
	    
	    return retVal;
	  }
	else
	  {
	    if (!dbSession.rollback(checkPointKey))
	      {
		return Ganymede.createErrorDialog("MailmanListCustom: Error",
						  "Ran into a problem during alias creation, and rollback failed");
	      }
	  }
      }
  }
}
