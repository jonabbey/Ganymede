/*

   ReturnVal.java

   This class is a serializable return code that is returned from
   most Ganymede server operations that need to pass back some
   sort of status information to the client.  
   
   Created: 27 January 1998
   Version: $Revision: 1.20 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import java.util.Vector;
import java.util.Hashtable;
import java.util.Enumeration;

import arlut.csd.JDialog.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                       ReturnVal

------------------------------------------------------------------------------*/

/**
 *
 * This class provides a report on the status of the client's requested operation.
 * It is intended to be returned by a call on the server to make a change to
 * the database.<br><br>
 *
 * Included in this object is a general success code, a list of field id's that
 * need to be rescanned in the relevant object, if applicable, a dialog resource
 * that can provide a description of a dialog box to be presented to the user,
 * and an optional callback that the client can call with the results of the
 * dialog box if necessary.<br><br>
 *
 * Note that operations that succeed without needing any further information
 * or action on the part of the client will simply return null.<br><br>
 *
 * If a non-null ReturnVal object is passed back, one of two things may be true.
 * didSucceed() may return true, in which case the operation was successful, but
 * there may be an informational dialog returned and/or a list of fields that
 * need to be updated in the relevant object in response to the successful update.
 * <br><br>
 *
 * Alternatively, didSucceed() may return false, in which case the operation either
 * could not succeed or is incomplete.  In this case, doRescan() will return false,
 * and getDialog() should return a valid JDialogBuff().  If the operation is
 * simply incomplete pending more data from the user, getCallback() will return
 * a non-null value.  In this case, the user should be presented the dialog box,
 * and the results of that dialog should be passed to the callback.  The callback
 * will in return pass back another ReturnVal object.  The server may walk the
 * user through an iterative set of dialogs to finally complete the desired
 * operation.
 *
 * @see arlut.csd.JDialog.JDialogBuff
 * @see arlut.csd.JDialog.DialogRsrc
 * @see arlut.csd.JDialog.StringDialog
 * @see arlut.csd.ganymede.Ganymediator
 *
 */

public class ReturnVal implements java.io.Serializable {

  static final long serialVersionUID = -4832305680354355493L;

  static final boolean debug = false;

  public static final byte NONE = 0;
  public static final byte EXPIRATIONSET = 1;
  public static final byte DELETED = 2;
  public static final byte LAST = 2;

  // ---

  boolean success;
  byte status;
  Invid newObjectInvid = null;
  db_object remoteObjectRef = null;

  private StringBuffer rescanList;
  private JDialogBuff dialog;
  private Ganymediator callback;

  private Hashtable objRescanHash;

  /**
   *
   * This boolean variable is used on the server side only,
   * to determine whether the field code that invoked 
   * wizardHook on a DBEditObject subclass should continue
   * with its normal process or whether it should immediately
   * return this ReturnVal to the (client-side) caller.
   *
   */

  public boolean doNormalProcessing;

  /* -- */

  // client side access

  /**
   *
   * This method returns the general success code for the
   * preceding operation.  If didSucceed() is true, doRescan()
   * should be checked.
   *
   */

  public boolean didSucceed()
  {
    return success;
  }

  /**
   *
   * Certain operations may set status codes.
   *
   */

  public byte getObjectStatus()
  {
    return status;
  }

  /**
   *
   * This method is used to get an Invid that the server
   * wants to return to the client.  Used particularly
   * for invid_field.createNewEmbedded().  Return null
   * if no Invid was set.
   *
   * @see arlut.csd.ganymede.invid_field
   * @see arlut.csd.ganymede.InvidDBField
   *
   */

  public Invid getInvid()
  {
    return newObjectInvid;
  }

  /**
   * This method is used to get a remote db_object reference that the
   * server wants to return to the client.  Used particularly for
   * Session.create_db_object() / Session.edit_db_object(), or null if
   * no db_object was returned.
   *
   * @see arlut.csd.ganymede.Session
   *
   */

  public db_object getObject()
  {
    return remoteObjectRef;
  }

  /**
   *
   * If the operation was not successful, this method should
   * return a dialog box describing the problem and, potentially,
   * asking for more information to complete the operation.<br><br>
   *
   * This method be checked after all calls to the server that
   * return non-null ReturnVal objects.
   *
   */

  public JDialogBuff getDialog()
  {
    return dialog;
  }

  /**
   *
   * If the server is asking for more interaction from the user
   * to complete this operation, this method will return an RMI
   * handle to a callback on the server.  The client should
   * popup the dialog box specified by getDialog() and pass the
   * results to the callback returned by this method.<br><br>
   *
   * This method will return null if getDialog() returns
   * null, and need not be checked in that case.
   *
   */

  public Ganymediator getCallback()
  {
    return callback;
  }

  /**
   *
   * This method returns true if the server is requesting that 
   * fields in the object referenced by the client's preceding call 
   * to the server be reprocessed.<br><br>
   *
   * This method will never return true if didSucceed() returns
   * false, and need not be checked in that case.
   *
   */

  public boolean doRescan()
  {
    return !(rescanList == null);
  }

  /**
   *
   * This method returns true if the server is requesting that all
   * fields in the object referenced by the client's preceding call 
   * to the server be reprocessed.
   *
   */

  public boolean rescanAll()
  {
    if (rescanList != null &&
	rescanList.toString().equals("all"))
      {
	return true;
      }

    return false;
  }

  /**
   *
   * This method returns a Vector of Short() objects if the server
   * provided an explicit list of fields that need to be reprocessed,
   * or null if all or no fields need to be processed.
   *
   */
  
  public Vector getRescanList()
  {
    if (rescanList == null ||
	rescanList.toString().equals("all"))
      {
	return null;
      }

    Vector results = new Vector();
    int index = 0;
    String temp1 = rescanList.toString();
    String temp2;

    while (temp1.indexOf('|', index) != -1)
      {
	temp2 = temp1.substring(index, temp1.indexOf('|', index));

	try
	  {
	    results.addElement(new Short(temp2));
	  }
	catch (NumberFormatException ex)
	  {
	    throw new RuntimeException("bad numeric value " + temp2 + "\n" + ex.getMessage());
	  }

	// let's get the next bit

	index = temp1.indexOf('|', index) + 1;
      }

    return results;
  }

  /**
   *
   * This method returns an encoded string representing
   * the objects and fields to be rescanned by the
   * client in response to this ReturnVal.<br><br>
   *
   * To be used for debugging.
   *
   */

  public String dumpRescanInfo()
  {
    StringBuffer buffer = new StringBuffer();

    /* -- */

    if (rescanList != null)
      {
	buffer.append("dumpRescanInfo(): ");
	buffer.append(rescanList.toString());
      }
    else
      {
	buffer.append("none in this object");
      }

    if (objRescanHash != null)
      {
	Enumeration keys = objRescanHash.keys();

	while (keys.hasMoreElements())
	  {
	    Object key = keys.nextElement();
	    ReturnVal retVal = (ReturnVal) objRescanHash.get(key);

	    buffer.append("\nRescan info for object " + key + ":\n");
	    buffer.append(retVal.dumpRescanInfo());
	  }
      }

    return buffer.toString();
  }

  /**
   *
   * This method returns a hashtable mapping invid's to
   * ReturnVal objects.  The intent of this is to allow
   * a server method to make changes to a number of
   * objects that the client might be concerned with (be
   * currently displaying, etc.), and to provide details
   * on status changes for those objects.<br><br>
   *
   * For instance, a method might make a change on the
   * server that would oblige the client to perform
   * a refresh on a particular field in another object
   * that it is displaying.  In such a case, the hashtable
   * returned by this method would map the invid of the
   * object to a ReturnVal that specified a list of
   * fields to rescan.<br><br>
   *
   * The ReturnVal's encoded for other objects will
   * not specify a dialog or callback.  Likewise, the
   * success value has undefined meaning.
   *
   * @return null if there was no associated object returnval's
   * specified by the server for this ReturnVal.
   * 
   */
  
  public Hashtable getObjResultSet()
  {
    return objRescanHash;
  }

  // ---------------------------------------------------------------------------
  // server side operations
  // ---------------------------------------------------------------------------

  public ReturnVal(boolean success)
  {
    this(success, false);
  }

  public ReturnVal(boolean success, boolean doNormalProcessing)
  {
    this.success = success;
    this.doNormalProcessing = doNormalProcessing;
    rescanList = null;
    objRescanHash = null;
    dialog = null;
    callback = null;
    status = NONE;
  }

  public void clear()
  {
    rescanList = null;
    objRescanHash = null;
    dialog = null;
    callback = null;
    status = NONE;
    newObjectInvid = null;
    remoteObjectRef = null;
  }

  /**
   *
   * unionRescan merges field and object rescan requests from
   * the supplied ReturnVal with and rescan requests we contain.<br><br>
   *
   * It is used to allow multiple sources in InvidDBField to contribute
   * rescan requests.<br><br>
   *
   * This method returns this so you can do a cascading return.
   *
   */

  public ReturnVal unionRescan(ReturnVal retVal)
  {
    if (retVal == null)
      {
	return this;
      }

    // add any rescan fields requested by retVal

    if (retVal.rescanList != null)
      {
	if (rescanList == null)
	  {
	    rescanList = new StringBuffer();
	  }
	
	if (retVal.rescanList.toString().equals("all"))
	  {
	    if (debug)
	      {
		System.err.println("ReturnVal.unionRescan(): setting full rescan");
	      }

	    setRescanAll();
	  }
	else
	  {
	    if (debug)
	      {
		System.err.println("ReturnVal.unionRescan(): adding field rescan buf:" +
				   retVal.rescanList.toString());
	      }

	    rescanList.append(retVal.rescanList.toString());
	  }
      }

    // add any rescan objects requested by retVal

    if (retVal.objRescanHash != null)
      {
	Enumeration enum = retVal.objRescanHash.keys();

	Invid objid;
	ReturnVal otherobj;

	while (enum.hasMoreElements())
	  {
	    objid = (Invid) enum.nextElement();
	    otherobj = (ReturnVal) retVal.objRescanHash.get(objid);

	    if (debug)
	      {
		System.err.println("ReturnVal.unionRescan(): adding rescan object " + objid);
	      }

	    addRescanObject(objid, otherobj);
	  }
      }

    return this;
  }

  public void setStatus(byte status)
  {
    if (status < NONE || status > LAST)
      {
	throw new IllegalArgumentException("invalid status code");
      }

    this.status = status;
  }

  public void setRescanAll()
  {
    if (rescanList == null)
      {
	rescanList = new StringBuffer();
      }
    else
      {
	rescanList.setLength(0);
      }

    rescanList.append("all");
  }

  public void addRescanField(short fieldID)
  {
    if (rescanList == null)
      {
	rescanList = new StringBuffer();
      }
    else
      {
	if (rescanList.toString().equals("all"))
	  {
	    return;
	  }
      }

    if (debug)
      {
	System.err.println("ReturnVal.addRescanField(" + fieldID+")");
      }

    rescanList.append(fieldID);
    rescanList.append("|");
  }

  public void addRescanObject(Invid objid, ReturnVal retVal)
  {
    if (objRescanHash == null)
      {
	objRescanHash = new Hashtable();
      }

    objRescanHash.put(objid, retVal);
  }

  public void setCallback(Ganymediator callback)
  {
    this.callback = callback;
  }

  public void setDialog(JDialogBuff dialog)
  {
    this.dialog = dialog;
  }

  /**
   *
   * This method is used to set an Invid that the client
   * can retrieve from us in those cases where a method
   * on the server really does need to return an Invid
   * _and_ a return val.
   *
   */

  public void setInvid(Invid invid)
  {
    this.newObjectInvid = invid;
  }

  /**
   *
   * This method is used to set a db_object reference that the client
   * can retrieve from us in those cases where a method on the server
   * really does need to return a db_object _and_ a return val.
   * 
   */

  public void setObject(db_object object)
  {
    this.remoteObjectRef = object;
  }
}
