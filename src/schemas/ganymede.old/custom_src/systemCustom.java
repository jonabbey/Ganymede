/*

   systemCustom.java

   This file is a management class for system objects in Ganymede.
   
   Created: 15 October 1997
   Release: $Name:  $
   Version: $Revision: 1.8 $
   Last Mod Date: $Date: 1999/01/22 18:04:32 $
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

import java.util.*;
import java.rmi.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                    systemCustom

------------------------------------------------------------------------------*/

public class systemCustom extends DBEditObject implements SchemaConstants {
  
  static final boolean debug = false;
  static QueryResult shellChoices = new QueryResult();
  static Date shellChoiceStamp = null;

  /**
   *
   * This method provides a hook that can be used to indicate that a
   * particular field's value should be filtered by a particular
   * subclass of DBEditObject.  This is intended to allow, for instance,
   * that the Admin object's name field, if null, can have the owning
   * system's name interposed.
   *
   */

  /**
   *
   * Customization Constructor
   *
   */

  public systemCustom(DBObjectBase objectBase) throws RemoteException
  {
    super(objectBase);
  }

  /**
   *
   * Create new object constructor
   *
   */

  public systemCustom(DBObjectBase objectBase, Invid invid, DBEditSet editset) throws RemoteException
  {
    super(objectBase, invid, editset);
  }

  /**
   *
   * Check-out constructor, used by DBObject.createShadow()
   * to pull out an object for editing.
   *
   */

  public systemCustom(DBObject original, DBEditSet editset) throws RemoteException
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
      case systemSchema.SYSTEMNAME:
      case systemSchema.INTERFACES:
      case systemSchema.DNSDOMAIN:
      case systemSchema.SYSTEMTYPE:
	return true;
      }

    return false;
  }

  public Object obtainChoicesKey(DBField field)
  {
    DBObjectBase base = Ganymede.db.getObjectBase((short) 272);	// system types

    /* -- */

    if (field.getID() == systemSchema.VOLUMES)
      {
	return null;		// no choices for volumes
      }
    
    if (field.getID() != systemSchema.SYSTEMTYPE)	// system type field
      {
	return super.obtainChoicesKey(field);
      }
    else
      {
	// we put a time stamp on here so the client
	// will know to call obtainChoiceList() afresh if the
	// system types base has been modified

	return "System Type:" + base.getTimeStamp();
      }
  }

  /**
   *
   * This method provides a hook that can be used to generate
   * choice lists for invid and string fields that provide
   * such.  String and Invid DBFields will call their owner's
   * obtainChoiceList() method to get a list of valid choices.
   * 
   */

  public QueryResult obtainChoiceList(DBField field)
  {
    if (field.getID() == systemSchema.VOLUMES)
      {
	return null;		// no choices for volumes
      }

    if (field.getID() != systemSchema.SYSTEMTYPE) // system type field
      {
	return super.obtainChoiceList(field);
      }

    Query query = new Query((short) 272, null, false); // list all system types

    query.setFiltered(false);	// don't care if we own the system types

    return editset.getSession().getGSession().query(query);
  }

  /**
   *
   * This method provides a hook that a DBEditObject subclass
   * can use to indicate whether a given field can only
   * choose from a choice provided by obtainChoiceList()
   *
   */

  public boolean mustChoose(DBField field)
  {
    if (field.getID() == systemSchema.SYSTEMTYPE)
      {
	return true;
      }

    return super.mustChoose(field);
  }
  
  /**
   *
   * Hook to have this object create a new embedded object
   * in the given field.  
   *
   */

  public Invid createNewEmbeddedObject(InvidDBField field)
  {
    DBEditObject newObject;
    DBObjectBase targetBase;
    DBObjectBaseField fieldDef;

    /* -- */

    if (field.getID() == systemSchema.INTERFACES) // interface field
      {
	fieldDef = field.getFieldDef();
	
	if (fieldDef.getTargetBase() > -1)
	  {
	    newObject = getSession().createDBObject(fieldDef.getTargetBase(), null, null);

	    // link it in

	    newObject.setFieldValue(SchemaConstants.ContainerField, getInvid());
	    
	    return newObject.getInvid();
	  }
	else
	  {
	    throw new RuntimeException("error in schema.. interface field target base not restricted..");
	  }
      }
    else
      {
	return null;		// default
      }
  }
}
