/*

   ReturnVal.java

   This class is a serializable return code that is returned from
   most Ganymede server operations that need to pass back some
   sort of status information to the client.  
   
   Created: 27 January 1998
   Version: $Revision: 1.5 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import java.util.Vector;

import arlut.csd.JDialog.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                       ReturnVal

------------------------------------------------------------------------------*/

/**
 *
 * This class provides a report on the status of the client's requested operation.
 * It is intended to be returned by a call on the server to make a change to
 * the database.
 *
 * Included in this object is a general success code, a list of field id's that
 * need to be rescanned in the relevant object, if applicable, a dialog resource
 * that can provide a description of a dialog box to be presented to the user,
 * and an optional callback that the client can call with the results of the
 * dialog box if necessary.
 *
 * Note that operations that succeed without needing any further information
 * or action on the part of the client will simply return null.
 *
 * If a non-null ReturnVal object is passed back, one of two things may be true.
 * didSucceed() may return true, in which case the operation was successful, but
 * there may be an informational dialog returned and/or a list of fields that
 * need to be updated in the relevant object in response to the successful update.
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

  public static final byte NONE = 0;
  public static final byte EXPIRATIONSET = 1;
  public static final byte LAST = 1;

  // ---

  boolean success;
  byte status;
  private StringBuffer rescanList;
  private JDialogBuff dialog;
  private Ganymediator callback;

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
   * If the operation was not successful, this method should
   * return a dialog box describing the problem and, potentially,
   * asking for more information to complete the operation.
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
   * results to the callback returned by this method.
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
   * to the server be reprocessed.
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

  // ---------------------------------------------------------------------------
  // server side operations
  // ---------------------------------------------------------------------------

  public ReturnVal(boolean success)
  {
    this.success = success;
    rescanList = null;
    dialog = null;
    callback = null;
    status = NONE;
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

}
