/*

   dhcpNetworkCustom.java

   This file is a management class for DHCP Network objects in Ganymede.
   
   Created: 10 October 2007
   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996-2008
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

import arlut.csd.JDialog.JDialogBuff;
import arlut.csd.ganymede.common.GanyPermissionsException;
import arlut.csd.ganymede.common.Invid;
import arlut.csd.ganymede.common.NotLoggedInException;
import arlut.csd.ganymede.common.QueryResult;
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
import arlut.csd.ganymede.server.InvidDBField;
import arlut.csd.ganymede.server.IPDBField;
import arlut.csd.ganymede.server.StringDBField;

/*------------------------------------------------------------------------------
                                                                           class
                                                                 dhcpNetworkCustom

------------------------------------------------------------------------------*/

public class dhcpNetworkCustom extends DBEditObject implements SchemaConstants, dhcpNetworkSchema {

  /**
   *
   * Customization Constructor
   *
   */

  public dhcpNetworkCustom(DBObjectBase objectBase)
  {
    super(objectBase);
  }

  /**
   *
   * Create new object constructor
   *
   */

  public dhcpNetworkCustom(DBObjectBase objectBase, Invid invid, DBEditSet editset)
  {
    super(objectBase, invid, editset);
  }

  /**
   *
   * Check-out constructor, used by DBObject.createShadow()
   * to pull out an object for editing.
   *
   */

  public dhcpNetworkCustom(DBObject original, DBEditSet editset)
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
        case dhcpNetworkSchema.NAME:
          return true;
      }

    if (fieldid == dhcpNetworkSchema.GUEST_RANGE)
      {
	return object.isDefined(dhcpNetworkSchema.ALLOW_REGISTERED_GUESTS);
      }

    // If network name is _GLOBAL_ we dont want a network number or mask, or allow registered guests.... just options.
    // otherwise network number and mask are required.

    String name = (String) object.getFieldValueLocal(dhcpNetworkSchema.NAME);

    if (!name.equals("_GLOBAL_"))
      {
	switch (fieldid)
	  {
	    case dhcpNetworkSchema.NETWORK_NUMBER:
	    case dhcpNetworkSchema.NETWORK_MASK:
	      return true;
	  }	
      }

    return false;
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
   *
   * @return A ReturnVal indicating success or failure.  May
   * be simply 'null' to indicate success if no feedback need
   * be provided.
   */

  public ReturnVal initializeNewObject()
  {
    return null;
	/*
    try
      {
        ReturnVal retVal = null;
        InvidDBField invf = (InvidDBField) getField(dhcpNetworkSchema.OPTIONS);

        try
          {
            retVal = invf.createNewEmbedded(true);
          }
        catch (GanyPermissionsException ex)
          {
            return Ganymede.createErrorDialog("permissions", "permissions error creating embedded object" + ex);
          }

        return retVal;
      }
    catch (NotLoggedInException ex)
      {
	return Ganymede.loginError(ex);
      }
	*/    
  }

  /**
   * Customization method to verify whether a specific field
   * in object should be cloned using the basic field-clone
   * logic.
   *
   * To be overridden on necessity in DBEditObject subclasses.
   *
   * <b>*PSEUDOSTATIC*</b>
   */

  public boolean canCloneField(DBSession session, DBObject object, DBField field)
  {
    if (field.getID() == dhcpNetworkSchema.NETWORK_NUMBER)
      {
        return false;           // let's not mess with the net number field
      }

    return super.canCloneField(session, object, field);
  }

  /**
   * <p>Hook to allow the cloning of an object.  If this object type
   * supports cloning (which should be very much customized for this
   * object type.. creation of the ancillary objects, which fields to
   * clone, etc.), this customization method will actually do the work.</p>
   *
   * @param session The DBSession that the new object is to be created in
   * @param origObj The object we are cloning
   * @param local If true, fields that have choice lists will not be checked against
   * those choice lists and read permissions for each field will not be consulted.
   * The canCloneField() method will still be consulted, however.
   *
   * @return A standard ReturnVal status object.  May be null on success, or
   * else may carry a dialog with information on problems and a success flag.
   */

  public ReturnVal cloneFromObject(DBSession session, DBObject origObj, boolean local)
  {
    try
      {
	boolean problem = false;
	ReturnVal tmpVal;
	StringBuffer resultBuf = new StringBuffer();
	ReturnVal retVal = super.cloneFromObject(session, origObj, local);

	if (retVal != null)
          {
            if (!retVal.didSucceed())
              {
                return retVal;
              }
            
            if (retVal.getDialog() != null)
              {
                resultBuf.append("\n\n");
                resultBuf.append(retVal.getDialog().getText());

                problem = true;
              }
          }

	// and clone the embedded objects.
        //
        // Remember, dhcpNetworkCustom.initializeNewObject() will create
        // a single embedded option object as part of the normal dhcp
        // network creation process.  We'll put this (single)
        // automatically created embedded object into the newOnes
        // vector, then create any new embedded options necessary when
        // cloning a multiple option dhcp network.

	InvidDBField newOptions = (InvidDBField) getField(dhcpNetworkSchema.OPTIONS);
	InvidDBField oldOptions = (InvidDBField) origObj.getField(dhcpNetworkSchema.OPTIONS);

	Vector newOnes = (Vector) newOptions.getValuesLocal().clone();
	Vector oldOnes = (Vector) oldOptions.getValuesLocal().clone();

	DBObject origOption;
	DBEditObject workingOption;
	int i;

	for (i = 0; i < newOnes.size(); i++)
	  {
	    workingOption = (DBEditObject) session.editDBObject((Invid) newOnes.elementAt(i));
	    origOption = session.viewDBObject((Invid) oldOnes.elementAt(i));
	    tmpVal = workingOption.cloneFromObject(session, origOption, local);

	    if (tmpVal != null && tmpVal.getDialog() != null)
	      {
		resultBuf.append("\n\n");
		resultBuf.append(tmpVal.getDialog().getText());

		problem = true;
	      }
	  }

	Invid newInvid;

	if (i < oldOnes.size())
	  {
	    for (; i < oldOnes.size(); i++)
	      {
		try
		  {
		    tmpVal = newOptions.createNewEmbedded(local);
		  }
		catch (GanyPermissionsException ex)
		  {
		    tmpVal = Ganymede.createErrorDialog("permissions",
                                                        "permissions failure creating embedded option " + ex);
		  }

		if (!tmpVal.didSucceed())
		  {
		    if (tmpVal != null && tmpVal.getDialog() != null)
		      {
			resultBuf.append("\n\n");
			resultBuf.append(tmpVal.getDialog().getText());

			problem = true;
		      }
		    continue;
		  }

		newInvid = tmpVal.getInvid();

		workingOption = (DBEditObject) session.editDBObject(newInvid);
		origOption = session.viewDBObject((Invid) oldOnes.elementAt(i));
		tmpVal = workingOption.cloneFromObject(session, origOption, local);

		if (tmpVal != null && tmpVal.getDialog() != null)
		  {
		    resultBuf.append("\n\n");
		    resultBuf.append(tmpVal.getDialog().getText());

		    problem = true;
		  }
	      }
	  }

	retVal = new ReturnVal(true, !problem);

	if (problem)
	  {
	    retVal.setDialog(new JDialogBuff("Possible Clone Problems", resultBuf.toString(),
					     "Ok", null, "ok.gif"));
	  }

	return retVal;
      }
    catch (NotLoggedInException ex)
      {
	return Ganymede.loginError(ex);
      }
  }



  /**
   * Customization method to verify whether the user should be able to
   * see a specific field in a given object.  Instances of 
   * {@link arlut.csd.ganymede.server.DBField DBField} will
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
   * To be overridden on necessity in DBEditObject subclasses.
   *
   * <b>*PSEUDOSTATIC*</b>
   */

  public boolean canSeeField(DBSession session, DBField field)
  {
    String name = (String) field.getOwner().getFieldValueLocal(dhcpNetworkSchema.NAME);
    if (name != null && name.equals("_GLOBAL_"))
      {
	if ( field.getID() == dhcpNetworkSchema.NETWORK_NUMBER ||
	     field.getID() == dhcpNetworkSchema.NETWORK_MASK ||
	     field.getID() == dhcpNetworkSchema.ALLOW_REGISTERED_GUESTS ||
	     field.getID() == dhcpNetworkSchema.GUEST_RANGE ||
	     field.getID() == dhcpNetworkSchema.GUEST_OPTIONS  
	   )
	  {
	    return false;
	  }
      }
    
    Boolean allow_registered_guests = (Boolean) field.getOwner().getFieldValueLocal(dhcpNetworkSchema.ALLOW_REGISTERED_GUESTS);
    if (allow_registered_guests == null || !allow_registered_guests.booleanValue())
      {
	if ( field.getID() == dhcpNetworkSchema.GUEST_RANGE ||
	     field.getID() == dhcpNetworkSchema.GUEST_OPTIONS  
	   )
	  {
	    return false;
	  }
      }
    

    return super.canSeeField(session, field);
  }

  /**
   * This method is called after the set value operation has been ok'ed
   * by any appropriate wizard code.
   */

  public synchronized ReturnVal finalizeSetValue(DBField field, Object value)
  {
    // If the name is _GLOBAL_ it does not get a network number, netmask or allow registered guests.
    // by when the username field is being changed.

    if (field.getID() == dhcpNetworkSchema.NAME)
      {
	ReturnVal result = new ReturnVal(true,true);

	String name = (String) value;
	if (name != null && name.equals("_GLOBAL_"))
	  {
	    getSession().checkpoint("clearing _global_ fields");
	    
	    try
	      {
		IPDBField network_number = (IPDBField) getField(dhcpNetworkSchema.NETWORK_NUMBER);
		result = ReturnVal.merge(result, network_number.setValueLocal(null));
		
		IPDBField network_mask = (IPDBField) getField(dhcpNetworkSchema.NETWORK_MASK);
		result = ReturnVal.merge(result, network_mask.setValueLocal(null));	
		
		DBField allow_registered_guests = (DBField) getField(dhcpNetworkSchema.ALLOW_REGISTERED_GUESTS);
		result = ReturnVal.merge(result, allow_registered_guests.setValueLocal(false));
		
		StringDBField guest_range = (StringDBField) getField(dhcpNetworkSchema.GUEST_RANGE);
		result = ReturnVal.merge(result, guest_range.setValueLocal(null));
		
		DBField guest_options = (DBField) getField(dhcpNetworkSchema.GUEST_OPTIONS);
		try
		  {
		    result = ReturnVal.merge(result, guest_options.deleteAllElements());
		  }
		catch (GanyPermissionsException ex)
		  {
		    return Ganymede.createErrorDialog("permissions", "permissions error deleting embedded object" + ex);
		  }
	      }	    
	    finally
	      {
		if (!ReturnVal.didSucceed(result))
		  {
		    getSession().rollback("clearing _global_ fields");
		  }
		else
		  {
		    getSession().popCheckpoint("clearing _global_ fields");
		  }
	      } 
	  }	    

	result.addRescanField(field.getOwner().getInvid(), dhcpNetworkSchema.NETWORK_NUMBER);
	result.addRescanField(field.getOwner().getInvid(), dhcpNetworkSchema.NETWORK_MASK);
	result.addRescanField(field.getOwner().getInvid(), dhcpNetworkSchema.ALLOW_REGISTERED_GUESTS);
	result.addRescanField(field.getOwner().getInvid(), dhcpNetworkSchema.GUEST_RANGE);
	result.addRescanField(field.getOwner().getInvid(), dhcpNetworkSchema.GUEST_OPTIONS);	
	return result;
      }

    // If the allow register guests checkbox is changed, hide/show the field and options next.

    if (field.getID() == dhcpNetworkSchema.ALLOW_REGISTERED_GUESTS)
      {
	ReturnVal result = new ReturnVal(true,true);

	if (value == null || Boolean.FALSE.equals(value))
	  {
	    getSession().checkpoint("clearing guest fields");

	    try
	      {
		StringDBField guest_range = (StringDBField) getField(dhcpNetworkSchema.GUEST_RANGE);
		result = ReturnVal.merge(result, guest_range.setValueLocal(null));

		DBField guest_options = (DBField) getField(dhcpNetworkSchema.GUEST_OPTIONS);
		try
		  {
		    result = ReturnVal.merge(result, guest_options.deleteAllElements());
		  }
		catch (GanyPermissionsException ex)
		  {
                    return Ganymede.createErrorDialog("permissions", "permissions error deleting embedded object" + ex);
		  }
	      }	    
	    finally
	      {
		if (!ReturnVal.didSucceed(result))
		  {
		    getSession().rollback("clearing guest fields");
		  }
		else
		  {
		    getSession().popCheckpoint("clearing guest fields");
		  }
	      }
	  }

	result.addRescanField(field.getOwner().getInvid(), dhcpNetworkSchema.GUEST_RANGE);
	result.addRescanField(field.getOwner().getInvid(), dhcpNetworkSchema.GUEST_OPTIONS);

	return result;
      }
    
    return null;
  }

}
