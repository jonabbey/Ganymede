/*

   ReturnVal.java

   This class is a serializable return code that is returned from
   most Ganymede server operations that need to pass back some
   sort of status information to the client.  
   
   Created: 27 January 1998
   Version: $Revision: 1.23 $ %D%
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu
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
 * This class provides a report on the status of the client's
 * requested operation.  It is intended to be returned by a call on
 * the server to make a change to the database.<br><br>
 *
 * Included in this object is a general success code, a list of
 * objects and fields that need to be rescanned, if applicable, a
 * dialog resource that can provide a description of a dialog box to
 * be presented to the user, and an optional callback that the client
 * can call with the results of the dialog box if necessary.<br><br>
 *
 * Note that operations that succeed without needing any further information
 * or action on the part of the client will simply return null.<br><br>
 *
 * If a non-null ReturnVal object is passed back, one of two things
 * may be true.  didSucceed() may return true, in which case the
 * operation was successful, but there may be an informational dialog
 * returned and/or a list of objects and fields that need to be
 * updated in response to the successful update.  <br><br>
 *
 * Alternatively, didSucceed() may return false, in which case the
 * operation either could not succeed or is incomplete.  In this case,
 * doRescan() will return false, and getDialog() should return a valid
 * JDialogBuff().  If the operation is simply incomplete pending more
 * data from the user, getCallback() will return a non-null value.  In
 * this case, the user should be presented the dialog box, and the
 * results of that dialog should be passed to the callback.  The
 * callback will in return pass back another ReturnVal object.  The
 * server may walk the user through an iterative set of dialogs to
 * finally complete the desired operation.
 *
 * @see arlut.csd.JDialog.JDialogBuff
 * @see arlut.csd.JDialog.DialogRsrc
 * @see arlut.csd.JDialog.StringDialog
 * @see arlut.csd.ganymede.Ganymediator
 * */

public class ReturnVal implements java.io.Serializable {

  static final boolean debug = false;

  static final long serialVersionUID = -3207496023172951056L;

  public static final byte NONE = 0;
  public static final byte EXPIRATIONSET = 1;
  public static final byte DELETED = 2;
  public static final byte LAST = 2;

  // ---

  boolean success;
  byte status;

  /**
   *
   * A Serializable Invid that can be returned in response to certain
   * operations on the server.
   *
   */

  Invid newObjectInvid = null;

  /**
   *
   * A Remote handle to a db_object on the server returned for use by
   * the client.
   * 
   */

  db_object remoteObjectRef = null;

  /**
   *
   * A Serializable StringBuffer representation of objects and fields
   * that need to be rescanned.
   * 
   */

  private StringBuffer rescanList;

  /**
   *
   * A Serializable Dialog Definition
   *
   */

  private JDialogBuff dialog;

  /**
   *
   * A Remote handle to a Wizard object on the server
   *
   */

  private Ganymediator callback;

  /**
   *
   * Maps Invid's to RescanBuf's.  Used on the client-side
   * post-serialization.
   * 
   */

  private transient Hashtable rescanHash = null;

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
   * This method returns true if this ReturnVal encodes rescan
   * information for one or more fields in on or more objects.<br><br>
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
   * This method returns a Vector of Invid objects, corresponding to
   * those objects which need to have some field rescan work done.
   * 
   */

  public Vector getRescanObjectsList()
  {
    Vector result = new Vector();
    Enumeration enum;

    /* -- */

    if (!doRescan())
      {
	return result;
      }

    breakOutRescanList();

    enum = rescanHash.keys();

    while (enum.hasMoreElements())
      {
	result.addElement(enum.nextElement());
      }
    
    return result;
  }

  /**
   *
   * This method returns true if the server is requesting that all
   * fields in the object referenced by the client's preceding call 
   * to the server be reprocessed.
   *
   */

  public boolean rescanAll(Invid objID)
  {
    if (!doRescan())
      {
	return false;
      }

    breakOutRescanList();

    if (rescanHash.containsKey(objID) && rescanHash.get(objID).equals("all"))
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
  
  public Vector getRescanList(Invid objID)
  {
    if (!doRescan())
      {
	return null;
      }

    breakOutRescanList();

    Object result = rescanHash.get(objID);

    if (result == null || result.equals("all"))
      {
	return null;
      }

    return (Vector) result;
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

    return buffer.toString();
  }

  /**
   *
   * This private method converts the rescanList StringBuffer to
   * a Hashtable (rescanHash) that maps Invid's to either Vector of
   * Short's or "all".
   *
   */

  private void breakOutRescanList()
  {
    if (rescanHash != null)
      {
	return;
      }

    rescanHash = new Hashtable();
    decodeRescanList(rescanList, rescanHash);
  }

  /**
   *
   * This method takes a StringBuffer encoded as follows:
   *
   * 263:170|all|271:131|31|57|286:41|all|310:4|134|13|92|
   *
   * and returns a Hashtable mapping Invid's to the rescan information
   * for that Invid, where the rescan information will either be the
   * String "all", indicating that all fields need to be rescanned, or
   * a Vector of Short's specifying field id's to be rescanned for
   * that object.
   *
   * @param buffer The StringBuffer to be decoded.
   * @param original The Hashtable to put the results into.. this method
   * will put into original the Union of the field rescan information
   * specified in original and the rescan information held in buffer.
   *  
   * @return A reference to original.
   */

  private Hashtable decodeRescanList(StringBuffer buffer, Hashtable original)
  {
    if (buffer == null)
      {
	return null;
      }

    if (original == null)
      {
	throw new IllegalArgumentException("Can't have a null original hash.");
      }

    /* - */

    int lastIndex = 0;
    int nextIndex;
    String tmpString = buffer.toString();
    String atom;
    Invid invid = null;

    /* -- */

    while (lastIndex < tmpString.length())
      {
	nextIndex = tmpString.indexOf('|', lastIndex);

	atom = tmpString.substring(lastIndex, nextIndex);

	if (atom.indexOf(':') != -1)
	  {
	    invid = new Invid(atom);
	  }
	else if (atom.equals("all"))
	  {
	    original.put(invid, atom);
	  }
	else
	  {
	    Vector vec;
	    Short fieldID = new Short(atom);

	    if (original.containsKey(invid) && !original.get(invid).equals("all"))
	      {
		vec = (Vector) original.get(invid);

		if (!vec.contains(fieldID))
		  {
		    vec.addElement(fieldID);
		  }
	      }
	    else if (!original.containsKey(invid))
	      {
		vec = new Vector();
		vec.addElement(fieldID);

		original.put(invid, vec);
	      }

	    // else we've already got 'all' specified for this invid, so we
	    // don't need to do anything else.
	  }

	lastIndex = nextIndex + 1;
      }

    return original;
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
    dialog = null;
    callback = null;
    status = NONE;
  }

  public void clear()
  {
    rescanList = null;
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

  public synchronized ReturnVal unionRescan(ReturnVal retVal)
  {
    if ((retVal == null) || (retVal == this))
      {
	return this;
      }

    // add any rescan fields requested by retVal

    if (retVal.rescanList != null)
      {
	// if our rescanList is null, take theirs.

	if (rescanList == null)
	  {
	    rescanList = new StringBuffer();
	    rescanList.append(retVal.rescanList.toString());
	  }
	else
	  {
	    Hashtable result = new Hashtable();

	    decodeRescanList(retVal.rescanList, result);
	    decodeRescanList(rescanList, result);

	    encodeRescanList(result);
	  }
      }

    return this;
  }

  /**
   *
   * This method takes a Hashtable mapping Invid's to Vectors
   * of Short field identifiers or the String "all" and generates
   * the StringBuffer to be serialized down to the client.
   *
   */

  private void encodeRescanList(Hashtable rescanTable)
  {
    Enumeration enum;
    Invid invid;

    /* -- */

    if (rescanList == null)
      {
	rescanList = new StringBuffer();
      }
    else
      {
	rescanList.setLength(0);
      }

    enum = rescanTable.keys();

    while (enum.hasMoreElements())
      {
	invid = (Invid) enum.nextElement();

	rescanList.append(invid.toString());
	rescanList.append("|");
	
	if (rescanTable.get(invid).equals("all"))
	  {
	    rescanList.append("all|");
	  }
	else
	  {
	    Vector fields = (Vector) rescanTable.get(invid);

	    for (int i = 0; i < fields.size(); i++)
	      {
		rescanList.append(fields.elementAt(i).toString());
		rescanList.append("|");
	      }
	  }
      }
  }

  public void setStatus(byte status)
  {
    if (status < NONE || status > LAST)
      {
	throw new IllegalArgumentException("invalid status code");
      }

    this.status = status;
  }

  /**
   *
   * This method makes a note in this ReturnVal to have the
   * client rescan all fields in object objID.
   *
   */

  public synchronized void setRescanAll(Invid objID)
  {
    if (debug)
      {
	System.err.println("ReturnVal.setRescanAll(" + objID + ")");
      }

    if (rescanList == null)
      {
	rescanList = new StringBuffer();
      }

    rescanList.append(objID.toString());
    rescanList.append("|all|");
  }

  /**
   *
   * This method makes a note in this ReturnVal to have the
   * client rescan field fieldID in object objID.
   *
   */

  public synchronized void addRescanField(Invid objID, short fieldID)
  {
    if (debug)
      {
	System.err.println("ReturnVal.addRescanField(" + objID + ", " + fieldID + ")");
      }

    if (rescanList == null)
      {
	rescanList = new StringBuffer();
      }

    rescanList.append(objID.toString());
    rescanList.append("|");
    rescanList.append(fieldID);
    rescanList.append("|");
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
