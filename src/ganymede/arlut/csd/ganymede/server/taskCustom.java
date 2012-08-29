/*

   taskCustom.java

   This file is a management class for task records in Ganymede.
   
   Created: 5 February 1999

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
            
   Ganymede Directory Management System
 
   Copyright (C) 1996-2010
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
   along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/

package arlut.csd.ganymede.server;

import java.rmi.RemoteException;

import arlut.csd.ganymede.common.Invid;
import arlut.csd.ganymede.common.NotLoggedInException;
import arlut.csd.ganymede.common.QueryResult;
import arlut.csd.ganymede.common.ReturnVal;
import arlut.csd.ganymede.common.SchemaConstants;

/*------------------------------------------------------------------------------
                                                                           class
                                                                      taskCustom

------------------------------------------------------------------------------*/

/**
 *
 * This class customizes DBEditObject for handling fields in the Ganymede
 * server's task object type.
 *
 */

public class taskCustom extends DBEditObject implements SchemaConstants {

  static final QueryResult choiceList = new QueryResult(true);

  static
  {
    choiceList.addRow(null, GanymedeScheduler.minutes_str, false);
    choiceList.addRow(null, GanymedeScheduler.hours_str, false);
    choiceList.addRow(null, GanymedeScheduler.days_str, false);
    choiceList.addRow(null, GanymedeScheduler.weeks_str, false);
  }

  /* -- */

  /**
   *
   * Customization Constructor
   *
   */

  public taskCustom(DBObjectBase objectBase) throws RemoteException
  {
    super(objectBase);
  }

  /**
   *
   * Create new object constructor
   *
   */

  public taskCustom(DBObjectBase objectBase, Invid invid, DBEditSet editset) throws RemoteException
  {
    super(objectBase, invid, editset);
  }

  /**
   *
   * Check-out constructor, used by DBObject.createShadow()
   * to pull out an object for editing.
   *
   */

  public taskCustom(DBObject original, DBEditSet editset) throws RemoteException
  {
    super(original, editset);
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
    if (field.getID() == SchemaConstants.TaskPeriodUnit)
      {
        return true;
      }

    return super.mustChoose(field);
  }

  /**
   *
   * This method provides a hook that can be used to generate
   * choice lists for invid and string fields that provide
   * such.  String and Invid DBFields will call their owner's
   * obtainChoiceList() method to get a list of valid choices.
   *
   * This method will provide a reasonable default for targeted
   * invid fields.
   * 
   */

  public QueryResult obtainChoiceList(DBField field) throws NotLoggedInException
  {
    if (field.getID() == SchemaConstants.TaskPeriodUnit)
      {
        return choiceList;
      }

    return super.obtainChoiceList(field);
  }

  /**
   *
   * Customization method to control whether a specified field
   * is required to be defined at commit time for a given object.<br><br>
   *
   * To be overridden in DBEditObject subclasses.<br><br>
   *
   * Note that this method will not be called if the controlling
   * GanymedeSession's enableOversight is turned off, as in
   * bulk loading.<br><br>
   *
   * <b>*PSEUDOSTATIC*</b>
   *
   */

  public boolean fieldRequired(DBObject object, short fieldid)
  {
    if (fieldid == SchemaConstants.TaskName || fieldid == SchemaConstants.TaskClass)
      {
        return true;
      }

    if (fieldid == SchemaConstants.TaskPeriodUnit)
      {
        return object.isSet(SchemaConstants.TaskRunPeriodically);
      }

    return false;
  }

  /**
   *
   * This method provides a hook that a DBEditObject subclass
   * can use to indicate that a given Numeric field has a restricted
   * range of possibilities.
   *
   */

  public boolean isIntLimited(DBField field)
  {
    if (field.getID() == SchemaConstants.TaskPeriodCount)
      {
        return true;
      }

    return super.isIntLimited(field);
  }

  /**
   *
   * This method is used to specify the minimum acceptable value
   * for the specified field.
   *
   */

  public int minInt(DBField field)
  {
    if (field.getID() == SchemaConstants.TaskPeriodCount)
      {
        return 0;
      }

    return super.minInt(field);
  }

  /**
   *
   * This method allows the DBEditObject to have executive approval
   * of any scalar set operation, and to take any special actions
   * in reaction to the set.. if this method returns true, the
   * DBField that called us will proceed to make the change to
   * it's value.  If this method returns false, the DBField
   * that called us will not make the change, and the field
   * will be left unchanged.<br><br>
   *
   * The DBField that called us will take care of all possible checks
   * on the operation (including a call to our own verifyNewValue()
   * method.  Under normal circumstances, we won't need to do anything
   * here.<br><br>
   *
   * If we do return false, we should set editset.setLastError to
   * provide feedback to the client about what we disapproved of.
   *  
   */

  public ReturnVal finalizeSetValue(DBField field, Object value)
  {
    // if we have either of the check boxes toggled, we're going
    // to toggle the other.  We'll also signal the client to
    // refresh all the fields in this object so as to show the
    // status change for the other check box and to hide or reveal
    // the fields for periodic execution

    if (field.getID() == SchemaConstants.TaskRunOnCommit)
      {
        boolean boolVal = ((Boolean) value).booleanValue();

        ReturnVal result = new ReturnVal(true);
        result.setRescanAll(field.getOwner().getInvid());
                
        if (boolVal)
          {
            setFieldValueLocal(SchemaConstants.TaskRunPeriodically, Boolean.FALSE);
          }

        return result;
      }

    if (field.getID() == SchemaConstants.TaskRunPeriodically)
      {
        boolean boolVal = ((Boolean) value).booleanValue();

        ReturnVal result = new ReturnVal(true);
        result.setRescanAll(field.getOwner().getInvid());

        if (boolVal)
          {
            setFieldValueLocal(SchemaConstants.TaskRunOnCommit, Boolean.FALSE);
          }

        return result;
      }

    return null;
  }

  /**
   *
   * Customization method to verify whether the user should be able to
   * see a specific field in a given object.  Instances of DBField will
   * wind up calling up to here to let us override the normal visibility
   * process.<br><br>
   *
   * Note that it is permissible for session to be null, in which case
   * this method will always return the default visiblity for the field
   * in question.<br><br>
   *
   * If field is not from an object of the same base as this DBEditObject,
   * an exception will be thrown.<br><br>
   *
   * To be overridden in DBEditObject subclasses.<br><br>
   *
   * <b>*PSEUDOSTATIC*</b>
   * 
   */

  public boolean canSeeField(DBSession session, DBField field)
  {
    if (field.getFieldDef().base() != this.objectBase)
      {
        throw new IllegalArgumentException("field/object mismatch");
      }

    if (field.getOwner() instanceof DBEditObject)
      {
        DBEditObject myObj = (DBEditObject) field.getOwner();

        switch (field.getID())
          {
          case SchemaConstants.TaskPeriodUnit:
          case SchemaConstants.TaskPeriodCount:
          case SchemaConstants.TaskPeriodAnchor:
            
            return myObj.isSet(SchemaConstants.TaskRunPeriodically);
          }
      }

    // by default, return the field definition's visibility

    return field.getFieldDef().isVisible(); 
  }

  /**
   *
   * This method is a hook for subclasses to override to
   * pass the phase-two commit command to external processes.<br><br>
   *
   * For normal usage this method would not be overridden.  For
   * cases in which change to an object would result in an external
   * process being initiated whose <b>success or failure would not
   * affect the successful commit of this DBEditObject in the
   * Ganymede server</b>, the process invokation should be placed here,
   * rather than in commitPhase1().<br><br>
   *
   * Subclasses that override this method may wish to make this method 
   * synchronized.
   *
   * @see arlut.csd.ganymede.server.DBEditSet
   */

  public void commitPhase2()
  {
    String origName = null, taskName;

    /* -- */

    if (original != null)
      {
        origName = (String) original.getFieldValueLocal(SchemaConstants.TaskName);
      }

    taskName = (String) getFieldValueLocal(SchemaConstants.TaskName);

    switch (getStatus())
      {
      case DROPPING:
        return;

      case DELETING:
        Ganymede.scheduler.unregisterTask(origName);

        break;

      case EDITING:
        if (!origName.equals(taskName))
          {
            // we changed our task name.. ditch the old record

            Ganymede.scheduler.unregisterTask(origName);

            // and re-register ourselves appropriately

            Ganymede.scheduler.registerTaskObject(this);
          }
        else
          {
            // no name change, go ahead and reschedule ourselves

            Ganymede.scheduler.registerTaskObject(this);
          }
        break;

      case CREATING:
        Ganymede.scheduler.registerTaskObject(this);
      }
  }

}
