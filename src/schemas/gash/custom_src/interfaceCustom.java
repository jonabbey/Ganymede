/*

   interfaceCustom.java

   This file is a management class for interface objects in Ganymede.
   
   Created: 15 October 1997
   Version: $Revision: 1.8 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.custom;

import arlut.csd.ganymede.*;

import java.util.*;
import java.rmi.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                 interfaceCustom

------------------------------------------------------------------------------*/

public class interfaceCustom extends DBEditObject implements SchemaConstants {
  
  static final boolean debug = false;

  // ---

  systemCustom sysObj = null;

  /**
   *
   * This method provides a hook that can be used to indicate that a
   * particular field's value should be filtered by a particular
   * subclass of DBEditObject.  This is intended to allow, for instance,
   * that the Admin object's name field, if null, can have the owning
   * interface's name interposed.
   *
   */

  /**
   *
   * Customization Constructor
   *
   */

  public interfaceCustom(DBObjectBase objectBase) throws RemoteException
  {
    super(objectBase);
  }

  /**
   *
   * Create new object constructor
   *
   */

  public interfaceCustom(DBObjectBase objectBase, Invid invid, DBEditSet editset) throws RemoteException
  {
    super(objectBase, invid, editset);
  }

  /**
   *
   * Check-out constructor, used by DBObject.createShadow()
   * to pull out an object for editing.
   *
   */

  public interfaceCustom(DBObject original, DBEditSet editset) throws RemoteException
  {
    super(original, editset);
    getParentObj();
  }

  /**
   *
   * Hook to allow intelligent generation of labels for DBObjects
   * of this type.  Subclasses of DBEditObject should override
   * this method to provide for custom generation of the
   * object's label type
   *
   */

  public String getLabelHook(DBObject object)
  {
    StringBuffer result = new StringBuffer();

    /* -- */

    String name = (String) object.getFieldValueLocal(interfaceSchema.NAME);

    if (name != null)
      {
	result.append(name);
      }

    IPDBField ipfield = (IPDBField) object.getField(interfaceSchema.ADDRESS);

    if (ipfield != null)
      {
	if (result.length() != 0)
	  {
	    result.append(":");
	  }
	
	result.append(ipfield.getValueString(true));
      }

    if (result.length() == 0)
      {
	return null;
      }
    else
      {
	return result.toString();
      }
  }

  /**
   *
   * This method provides a hook that can be used to generate
   * choice lists for invid and string fields that provide
   * such.  String and Invid DBFields will call their owner's
   * obtainChoiceList() method to get a list of valid choices.<br><br>
   *
   * This method will provide a reasonable default for targetted
   * invid fields.
   * 
   */

  public QueryResult obtainChoiceList(DBField field)
  {
    if (field.getID() != interfaceSchema.IPNET)
      {
	return super.obtainChoiceList(field);
      }

    QueryResult result = new QueryResult();

    // get the vector of currently available nets from our containing
    // System

    sysObj = getParentObj();
    Vector ipNetVec = sysObj.getAvailableNets();

    for (int i = 0; i < ipNetVec.size(); i++)
      {
	result.addRow((ObjectHandle) ipNetVec.elementAt(i));
      }

    if (debug)
      {
	System.err.println("interfaceCustom: net choice for invid " + getInvid() + ":\n" +
			   result.getBuffer());
      }
    
    return result;
  }

  /**
   *
   * This method allows the DBEditObject to have executive approval
   * of any vector set operation, and to take any special actions
   * in reaction to the set.. if this method returns true, the
   * DBField that called us will proceed to make the change to
   * it's vector.  If this method returns false, the DBField
   * that called us will not make the change, and the field
   * will be left unchanged.<br><br>
   *
   * The DBField that called us will take care of all possible
   * checks on the operation (including vector bounds, etc.),
   * acceptable values as appropriate (including a call to our
   * own verifyNewValue() method.  Under normal circumstances,
   * we won't need to do anything here.<br><br>
   *
   * If we do return false, we should set editset.setLastError to
   * provide feedback to the client about what we disapproved of.
   * 
   */

  public boolean finalizeSetElement(DBField field, int index, Object value)
  {
    if (field.getID() == interfaceSchema.IPNET)
      {
	// we don't want to mess with the available-network
	// management code if we are doing bulk-loading.

	if (!gSession.enableOversight)
	  {
	    return true;
	  }

	// we're changing the IP net.. make sure our parent is ok with us
	// taking the new net.
	
	sysObj = getParentObj();
	
	// free the old net for others to use.. note that at this point, 
	// field.getOldValue() holds the field's old value and value is the
	// proposed new value
	
	if (!sysObj.freeNet((Invid) field.getOldValue()))
	  {
	    return false;
	  }
	else if (!sysObj.allocNet((Invid) value))
	  {
	    sysObj.allocNet((Invid) field.getOldValue()); // take it back
	    return false;
	  }

	// set our address.. the wizardHook will have instructed the
	// client to rescan the address field.

	IPDBField ipfield = (IPDBField) getField(interfaceSchema.ADDRESS);
	Byte[] address = getParentObj().getAddress((Invid) value);
	
	if (address != null)
	  {
	    // this will work or not, the client's rescan will show the
	    // result regardless

	    ipfield.setValueLocal(address);
	  }
      }

    return true;
  }

  /**
   *
   * This is the hook that DBEditObject subclasses use to interpose wizards when
   * a field's value is being changed.
   *
   */

  public ReturnVal wizardHook(DBField field, int operation, Object param1, Object param2)
  {
    if (field.getID() == interfaceSchema.IPNET && operation == SETVAL)
      {
	// ok, we want to go ahead and approve the operation,
	// but we want to cause the client to rescan the IPNET
	// field in all of our siblings so that their choice
	// list gets updated to not show whatever net *we*
	// just chose.

	// First, we create a ReturnVal that will apply to
	// our siblings
	
	ReturnVal rescanPlease = new ReturnVal(true); // bool doesn't matter
	rescanPlease.addRescanField(interfaceSchema.IPNET);

	// second, we create a ReturnVal which will cause
	// the field.setValue() call which triggered us
	// to continue normal processing, and return
	// our list of rescan preferences to the client.

	ReturnVal result = new ReturnVal(true, true);
	result.addRescanField(interfaceSchema.ADDRESS);

	Vector entries = getSiblingInvids();

	for (int i = 0; i < entries.size(); i++)
	  {
	    if (debug)
	      {
		System.err.println("interfaceCustom.wizardHook(): adding object " + 
				   entries.elementAt(i) + " for rescan");
	      }
	    result.addRescanObject((Invid) entries.elementAt(i), rescanPlease);
	  }



	return result;
      }

    return null;
  }

  private systemCustom getParentObj()
  {
    if (sysObj == null)
      {    
	Invid sysInvid = (Invid) getFieldValueLocal(SchemaConstants.ContainerField);
	sysObj = (systemCustom) getSession().viewDBObject(sysInvid);
      }

    return sysObj;
  }

  private Vector getSiblingInvids()
  {
    Vector result;
    Invid sysInvid = (Invid) getFieldValueLocal(SchemaConstants.ContainerField);
    DBObject sysObj = getSession().viewDBObject(sysInvid);

    result = (Vector) sysObj.getFieldValuesLocal(systemSchema.INTERFACES).clone();
    
    // we are not our own sibling.

    result.removeElement(getInvid());

    if (debug)
      {
	System.err.println("interfaceCustom.getSiblingInvids(): " + getInvid() +
			   " has return value: " + result);
      }

    return result;
  }
}
