/*

   interfaceCustom.java

   This file is a management class for interface objects in Ganymede.
   
   Created: 15 October 1997
   Version: $Revision: 1.6 $ %D%
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

    // ok, we are returning the list of choices for what
    // ipNet's to put this entry into.  We don't want it
    // to include any ipNets that already have an entry
    // for this system.

    // first, get the list of map entries contained in this
    // object.

    Vector netsToSkip = new Vector();
    DBObject entry;
    Invid netInvid;

    Vector entries = getSiblingInvids();

    for (int i = 0; i < entries.size(); i++)
      {
	entry = getSession().viewDBObject((Invid) entries.elementAt(i));

	netInvid = (Invid) entry.getFieldValueLocal(interfaceSchema.IPNET);

	if (netInvid != null)
	  {
	    netsToSkip.addElement(netInvid);
	  }
      }
    
    // ok, netsToSkip has a list of invid's to skip in our choice list.

    // now, we need to find the list of IPnet's available in the room
    // that the system is registered in.

    QueryResult netList = new QueryResult();

    try
      {
	// find the system that contains us
	
	Invid sysInvid = (Invid) getFieldValueLocal(SchemaConstants.ContainerField);
	entry = getSession().viewDBObject(sysInvid);
	
	// get the room object that the system is associated with
	
	Invid roomInvid = (Invid) entry.getFieldValueLocal(systemSchema.ROOM);
	entry = getSession().viewDBObject(roomInvid);
	
	// and get the list of networks in that room
	
	Vector ipNetVec = entry.getFieldValuesLocal(roomSchema.NETWORKS);
	String netName;
	
	for (int i = 0; i < ipNetVec.size(); i++)
	  {
	    netInvid = (Invid) ipNetVec.elementAt(i);
	    netName = getGSession().viewObjectLabel(netInvid);
	    
	    netList.addRow(netInvid, netName, true);
	  }
      }
    catch (NullPointerException ex)
      {
	// ok, no big deal
      }

    QueryResult result = new QueryResult();

    // go through baseList, anything that we don't skip because
    // other map entries in this user object have taken that
    // map gets added to result.

    for (int i = 0; i < netList.size(); i++)
      {
	netInvid = netList.getInvid(i);

	if (!netsToSkip.contains(netInvid))
	  {
	    result.addRow(netList.getObjectHandle(i));
	  }
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
   * This is the hook that DBEditObject subclasses use to interpose wizards when
   * a field's value is being changed.
   *
   */

  public ReturnVal wizardHook(DBField field, int operation, Object param1, Object param2)
  {
    if ((field.getID() != mapEntrySchema.MAP) ||
	(operation != DBEditObject.SETVAL))
      {
	return null;		// by default, we just ok whatever
      }

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
