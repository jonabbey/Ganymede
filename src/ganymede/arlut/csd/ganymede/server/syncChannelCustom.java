/*

   syncChannelCustom.java

   This file is a management class for sync channel definitions in Ganymede.
   
   Created: 2 February 2005
   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996-2005
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

package arlut.csd.ganymede.server;

import java.rmi.RemoteException;

import arlut.csd.ganymede.common.Invid;
import arlut.csd.ganymede.common.NotLoggedInException;
import arlut.csd.ganymede.common.QueryResult;
import arlut.csd.ganymede.common.ReturnVal;
import arlut.csd.ganymede.common.SchemaConstants;

import arlut.csd.Util.TranslationService;

/*------------------------------------------------------------------------------
                                                                           class
                                                               syncChannelCustom

------------------------------------------------------------------------------*/

/**
 * <p>This class is a {@link arlut.csd.ganymede.server.DBEditObject}
 * subclass for handling fields in the Ganymede server's sync channel
 * object type.  It contains special logic for handling the set up and
 * configuration of {@link arlut.csd.ganymede.server.SyncRunners} in
 * the Ganymede server.</p>
 */

public class syncChannelCustom extends DBEditObject implements SchemaConstants {

  /**
   * <p>TranslationService object for handling string localization in
   * the Ganymede server.</p>
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.server.syncChannelCustom");

  /**
   *
   * Customization Constructor
   *
   */

  public syncChannelCustom(DBObjectBase objectBase) throws RemoteException
  {
    super(objectBase);
  }

  /**
   *
   * Create new object constructor
   *
   */

  public syncChannelCustom(DBObjectBase objectBase, Invid invid, DBEditSet editset) throws RemoteException
  {
    super(objectBase, invid, editset);
  }

  /**
   *
   * Check-out constructor, used by DBObject.createShadow()
   * to pull out an object for editing.
   *
   */

  public syncChannelCustom(DBObject original, DBEditSet editset) throws RemoteException
  {
    super(original, editset);
  }

  /**
   * Initializes a newly created DBEditObject.
   *
   * When this method is called, the DBEditObject has been created,
   * its ownership set, and all fields defined in the controlling
   * {@link arlut.csd.ganymede.server.DBObjectBase DBObjectBase}
   * have been instantiated without defined
   * values.  If this DBEditObject is an embedded type, it will
   * have been linked into its parent object before this method
   * is called.
   *
   * This method is responsible for filling in any default
   * values that can be calculated from the 
   * {@link arlut.csd.ganymede.server.DBSession DBSession}
   * associated with the editset defined in this DBEditObject.
   *
   * If initialization fails for some reason, initializeNewObject()
   * will return a ReturnVal with an error result..  If the owning
   * GanymedeSession is not in bulk-loading mode (i.e.,
   * GanymedeSession.enableOversight is true), {@link
   * arlut.csd.ganymede.server.DBSession#createDBObject(short, arlut.csd.ganymede.common.Invid, java.util.Vector)
   * DBSession.createDBObject()} will checkpoint the transaction
   * before calling this method.  If this method returns a failure code, the
   * calling method will rollback the transaction.  This method has no
   * responsibility for undoing partial initialization, the
   * checkpoint/rollback logic will take care of that.
   *
   * If enableOversight is false, DBSession.createDBObject() will not
   * checkpoint the transaction status prior to calling initializeNewObject(),
   * so it is the responsibility of this method to handle any checkpointing
   * needed.
   *
   * This method should be overridden in subclasses.
   */

  public ReturnVal initializeNewObject()
  {
    // we don't want to do any of this initialization during
    // bulk-loading.

    if (!getGSession().enableOversight)
      {
	return null;
      }

    // a newly created Sync Channel object have its type set to manual

    StringDBField typeField = (StringDBField) getField(SchemaConstants.SyncChannelTypeString);
    
    typeField.setValueLocal(ts.l("global.manual"));

    return null;
  }

  /**
   * This method provides a hook that a DBEditObject subclass
   * can use to indicate whether a given string field can only
   * choose from a choice provided by obtainChoiceList()
   */

  public boolean mustChoose(DBField field)
  {
    if (field.getID() == SchemaConstants.SyncChannelTypeString)
      {
	// we want to force type choosing

	return true;
      }

    return super.mustChoose(field);
  }

  /**
   * This method provides a hook that can be used to generate
   * choice lists for invid and string fields that provide
   * such.  String and Invid DBFields will call their owner's
   * obtainChoiceList() method to get a list of valid choices.
   */

  public QueryResult obtainChoiceList(DBField field) throws NotLoggedInException
  {
    if (field.getID() == SchemaConstants.SyncChannelTypeString)
      {
	QueryResult syncTypes = new QueryResult(true);

	syncTypes.addRow(ts.l("global.manual"));
	syncTypes.addRow(ts.l("global.incremental"));
	syncTypes.addRow(ts.l("global.fullstate"));
	
	return syncTypes;
      }
    
    return super.obtainChoiceList(field);
  }

  /**
   * customization method to control whether a specified field
   * is required to be defined at commit time for a given object.
   *
   * To be overridden in DBEditObject subclasses.
   *
   * Note that this method will not be called if the controlling
   * GanymedeSession's enableOversight is turned off, as in
   * bulk loading.
   *
   * <b>*PSEUDOSTATIC*</b>
   *
   */

  public boolean fieldRequired(DBObject object, short fieldid)
  {
    switch (fieldid)
      {
      case SchemaConstants.SyncChannelName:
      case SchemaConstants.SyncChannelTypeString:
      case SchemaConstants.SyncChannelTypeNum:
	return true;
      }

    int type = ((Integer) object.getFieldValueLocal(SchemaConstants.SyncChannelTypeNum)).intValue();

    switch (fieldid)
      {
      case SchemaConstants.SyncChannelDirectory:

	if (type == 1)
	  {
	    return true;
	  }

	break;

      case SchemaConstants.SyncChannelFullStateFile:

	if (type == 2)
	  {
	    return true;
	  }

	break;

      case SchemaConstants.SyncChannelServicer:

	if (type != 0)
	  {
	    return true;
	  }

	break;
      }

    // We'll allow SyncChannelFields and SyncChannelPlaintextOK to be
    // false or undefined

    return false;
  }

  /**
   * This method allows the DBEditObject to have executive approval
   * of any scalar set operation, and to take any special actions
   * in reaction to the set.. if this method returns true, the
   * DBField that called us will proceed to make the change to
   * it's value.  If this method returns false, the DBField
   * that called us will not make the change, and the field
   * will be left unchanged.
   *
   * The DBField that called us will take care of all possible checks
   * on the operation (including a call to our own verifyNewValue()
   * method.  Under normal circumstances, we won't need to do anything
   * here.
   *
   * If we do return false, we should set editset.setLastError to
   * provide feedback to the client about what we disapproved of.
   *  
   */

  public ReturnVal finalizeSetValue(DBField field, Object value)
  {
    // if we change the SyncChannelTypeString field, we'll need to
    // hide or reveal the appropriate fields, by telling the client to
    // refresh its display of this object

    if (field.getID() == SchemaConstants.SyncChannelTypeString)
      {
	NumericDBField indexField = (NumericDBField) getField(SchemaConstants.SyncChannelTypeNum);

	String type = (String) value;

	if (type.equals(ts.l("global.manual")))
	  {
	    indexField.setValueLocal(new Integer(0));
	  }
	else if (type.equals(ts.l("global.incremental")))
	  {
	    indexField.setValueLocal(new Integer(1));
	  }
	else if (type.equals(ts.l("global.fullstate")))
	  {
	    indexField.setValueLocal(new Integer(2));
	  }
	else
	  {
	    /* this shouldn't happen if mustChoose is set, but just in case.. */

	    // Error, couldn''t recognize Sync Channel type string "{0}".
	    return Ganymede.createErrorDialog(ts.l("finalizeSetValue.unrecognized_type_token", type));
	  }

	ReturnVal result = new ReturnVal(true);
	result.setRescanAll(field.getOwner().getInvid());
	return result;
      }

    return null;
  }

  /**
   * Customization method to verify whether the user should be able to
   * see a specific field in a given object.  Instances of DBField will
   * wind up calling up to here to let us override the normal visibility
   * process.
   *
   * Note that it is permissible for session to be null, in which case
   * this method will always return the default visiblity for the field
   * in question.
   *
   * If field is not from an object of the same base as this DBEditObject,
   * an exception will be thrown.
   *
   * To be overridden in DBEditObject subclasses.
   *
   * <b>*PSEUDOSTATIC*</b>
   */

  public boolean canSeeField(DBSession session, DBField field)
  {
    if (field.getFieldDef().base != this.objectBase)
      {
	throw new IllegalArgumentException("field/object mismatch");
      }

    try
      {
	DBObject myObj = field.getOwner();
	int type = -1;

	// this may throw a NullPointerException if there is no
	// numeric type index set
 
	type = ((Integer) myObj.getFieldValueLocal(SchemaConstants.SyncChannelTypeNum)).intValue();

	if (field.getID() == SchemaConstants.SyncChannelDirectory)
	  {
	    return (type == 1);
	  }
	else if (field.getID() == SchemaConstants.SyncChannelFullStateFile)
	  {
	    return (type == 2);
	  }
	else if (field.getID() == SchemaConstants.SyncChannelServicer)
	  {
	    return (type != 0);
	  }
      }
    catch (Throwable ex)
      {
	ex.printStackTrace();
      }

    // never show the hidden index field

    if (field.getID() == SchemaConstants.SyncChannelTypeNum)
      {
	return false;
      }

    // by default, return the field definition's visibility

    return field.getFieldDef().isVisible(); 
  }

  /**
   * This method provides a hook that a DBEditObject subclass
   * can use to indicate that a given
   * {@link arlut.csd.ganymede.server.NumericDBField NumericDBField}
   * has a restricted range of possibilities.
   */

  public boolean isIntLimited(DBField field)
  {
    if (field.getID() == SchemaConstants.SyncChannelTypeNum)
      {
	return true;
      }

    return super.isIntLimited(field);
  }

  /**
   * This method is used to specify the minimum acceptable value
   * for the specified
   * {@link arlut.csd.ganymede.server.NumericDBField NumericDBField}.
   */

  public int minInt(DBField field)
  {
    if (field.getID() == SchemaConstants.SyncChannelTypeNum)
      {
	return 0;
      }

    return super.minInt(field);
  }

  /**
   * This method is used to specify the maximum acceptable value
   * for the specified    
   * {@link arlut.csd.ganymede.server.NumericDBField NumericDBField}.
   */

  public int maxInt(DBField field)
  {
    if (field.getID() == SchemaConstants.SyncChannelTypeNum)
      {
	return 2;
      }

    return super.maxInt(field);
  }

  /**
   * This method is a hook for subclasses to override to
   * pass the phase-two commit command to external processes.
   *
   * For normal usage this method would not be overridden.  For
   * cases in which change to an object would result in an external
   * process being initiated whose <b>success or failure would not
   * affect the successful commit of this DBEditObject in the
   * Ganymede server</b>, the process invokation should be placed here,
   * rather than in commitPhase1().
   *
   * Subclasses that override this method may wish to make this method 
   * synchronized.
   *
   * @see arlut.csd.ganymede.server.DBEditSet
   */

  public void commitPhase2()
  {
    String origName = null, channelName = null;
    int origType = 0, type = 0;

    /* -- */

    if (original != null)
      {
	origName = (String) original.getFieldValueLocal(SchemaConstants.SyncChannelName);
	origType = ((Integer) original.getFieldValueLocal(SchemaConstants.SyncChannelTypeNum)).intValue();
      }

    channelName = (String) getFieldValueLocal(SchemaConstants.SyncChannelName);
    type = ((Integer) getFieldValueLocal(SchemaConstants.SyncChannelTypeNum)).intValue();

    switch (getStatus())
      {
      case DROPPING:
	return;

      case DELETING:
	if (origType == 1 || origType == 2)
	  {
	    Ganymede.unregisterSyncChannel(origName);
	  }
	break;

      case EDITING:
	Ganymede.unregisterSyncChannel(origName);
	Ganymede.registerSyncChannel(new SyncRunner(this));
	break;

      case CREATING:
	Ganymede.registerSyncChannel(new SyncRunner(this));
      }
  }
}
