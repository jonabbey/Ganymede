/*

   interfaceCustom.java

   This file is a management class for interface objects in Ganymede.
   
   Created: 15 October 1997
   Version: $Revision: 1.9 $ %D%
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
  
  static final boolean debug = true;

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
   * This method returns a key that can be used by the client
   * to cache the value returned by choices().  If the client
   * already has the key cached on the client side, it
   * can provide the choice list from its cache rather than
   * calling choices() on this object again.<br><br>
   *
   * If there is no caching key, this method will return null.
   *
   */

  public Object obtainChoicesKey(DBField field)
  {
    if (field.getID() == interfaceSchema.IPNET)
      {
	return null;		// no caching net choices, thankyouverymuch
      }
    
    return super.obtainChoicesKey(field);
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

  public boolean finalizeSetValue(DBField field, Object value)
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

	if (debug)
	  {
	    System.err.println("interfaceCustom.finalizeSetValue(): about to check net stuff");
	  }
	
	if (!sysObj.freeNet((Invid) field.getOldValue()))
	  {
	    if (debug)
	      {
		System.err.println("interfaceCustom.finalizeSetValue(): couldn't free old net num");
	      }
	
	    return false;
	  }
	else if (!sysObj.allocNet((Invid) value))
	  {
	    if (debug)
	      {
		System.err.println("interfaceCustom.finalizeSetValue(): couldn't alloc new net num");
	      }
	
	    sysObj.allocNet((Invid) field.getOldValue()); // take it back
	    return false;
	  }

	// set our address.. the wizardHook will have instructed the
	// client to rescan the address field.

	if (debug)
	  {
	    System.err.println("interfaceCustom.finalizeSetValue(): getting address");
	  }

	IPDBField ipfield = (IPDBField) getField(interfaceSchema.ADDRESS);
	Byte[] address = getParentObj().getAddress((Invid) value);
	
	if (address != null)
	  {
	    // this will work or not, the client's rescan will show the
	    // result regardless

	    if (debug)
	      {
		System.err.print("interfaceCustom.finalizeSetValue(): setting address to ");

		for (int j = 0; j < address.length; j++)
		  {
		    if (j > 0)
		      {
			System.err.print(".");
		      }
		    
		    System.err.print(s2u(address[j].byteValue()));
		  }
		
		System.err.println();
	      }

	    ipfield.setValueLocal(address);
	  }
	else
	  {
	    System.err.println("interfaceCustom.finalizeSetValue(): null address from parent"); 
	  }
      }

    if (field.getID() == interfaceSchema.ADDRESS)
      {
	if (!matchNet((Byte[]) value, (Invid) getFieldValueLocal(interfaceSchema.IPNET)))
	  {
	    // need to find a net that matches the new address, if we can.

	    Vector ipNetVec = sysObj.getAvailableNets();
	    boolean found = false;
	    ReturnVal retVal = null;

	    for (int i = 0; i < ipNetVec.size(); i++)
	      {
		if (matchNet((Byte[]) value, (Invid) ipNetVec.elementAt(i)))
		  {
		    retVal = setFieldValue(interfaceSchema.IPNET, (Invid) ipNetVec.elementAt(i));
		    found = true;
		    break;
		  }
	      }

	    if (found = true)
	      {
		if (retVal != null && !retVal.didSucceed())
		  {
		    return false;
		  }
	      }
	    else
	      {
		// no IPnet to match.. fail

		return false;
	      }
	    
	    return true;
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
	// our siblings.. we want all our siblings to
	// rescan their choices for the IPNET field
	
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

	if (debug)
	  {
	    System.err.println("interfaceCustom.wizardHook(): requesting ADDRESS field rescan");
	  }

	return result;
      }

    if (field.getID() == interfaceSchema.ADDRESS && operation == SETVAL)
      {
	if (!matchNet((Byte[]) param1, (Invid) getFieldValueLocal(interfaceSchema.IPNET)))
	  {
	    // the finalize code will have to try to find a matching IPNET

	    ReturnVal result = new ReturnVal(true, true);
	    result.addRescanField(interfaceSchema.IPNET);
	  }
      }

    return null;
  }

  /**
   *
   * This private helper method compares the address given with the
   * IP network referenced by netInvid, returning true if the
   * address specified fits with the network referenced by netInvid.<br><br>
   *
   * This code, like the rest of the GASH network schema code, currently
   * assumes that all IP networks are 'Class C', where the first 3 octets
   * of an IPv4 address are the net number and the last octet is the
   * host id.
   *
   */

  private boolean matchNet(Byte[] address, Invid netInvid)
  {
    try
      {
	DBObject netObj = getSession().viewDBObject(netInvid);
	Byte[] netNum = (Byte[]) netObj.getFieldValueLocal(networkSchema.NETNUMBER);
	
	for (int i = 0; i < netNum.length; i++)
	  {
	    if (!netNum[i].equals(address[i]))
	      {
		return false;
	      }
	  }
      }
    catch (NullPointerException ex)
      {
	Ganymede.debug("interfaceCustom.matchNet: NullPointer " + ex.getMessage());
	return false;
      }
    
    return true;
  }

  private systemCustom getParentObj()
  {
    if (sysObj == null)
      {    
	Invid sysInvid = (Invid) getFieldValueLocal(SchemaConstants.ContainerField);

	// we *have* to use editDBObject() here because we need access to the custom
	// object.. it makes no sense for us to be pulled out for editing without
	// our parent also being edited.

	sysObj = (systemCustom) getSession().editDBObject(sysInvid);
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

  /**
   *
   * This method maps an int value between 0 and 255 inclusive
   * to a legal signed byte value.
   *
   */

  public final static byte u2s(int x)
  {
    if ((x < 0) || (x > 255))
      {
	throw new IllegalArgumentException("Out of range: " + x);
      }

    return (byte) (x - 128);
  }

  /**
   *
   * This method maps a u2s-encoded signed byte value to an
   * int value between 0 and 255 inclusive.
   *
   */

  public final static short s2u(byte b)
  {
    return (short) (b + 128);
  }
}
