/*

   ReturnVal.java

   This class is a serializable return code that is returned from
   most Ganymede server operations that need to pass back some
   sort of status information to the client.  
   
   Created: 27 January 1998
   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996-2006
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

package arlut.csd.ganymede.common;

import java.rmi.Remote;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import arlut.csd.JDialog.JDialogBuff;
import arlut.csd.ganymede.rmi.Ganymediator;
import arlut.csd.ganymede.rmi.Session;
import arlut.csd.ganymede.rmi.XMLSession;
import arlut.csd.ganymede.rmi.FileTransmitter;
import arlut.csd.ganymede.rmi.adminSession;
import arlut.csd.ganymede.rmi.db_object;

import arlut.csd.Util.TranslationService;

/*------------------------------------------------------------------------------
                                                                           class
                                                                       ReturnVal

------------------------------------------------------------------------------*/

/**
 * <p>This class provides a report on the status of the client's
 * requested operation.  It is intended to be returned by a call on
 * the server to make a change to the database.</p>
 *
 * <p>Included in this object is a general success code, a list of
 * objects and fields that need to be rescanned, if applicable, a
 * dialog resource that can provide a description of a dialog box to
 * be presented to the user, and an optional callback that the client
 * can call with the results of the dialog box if necessary.</p>
 *
 * <p>Note that operations that succeed without needing any further information
 * or action on the part of the client will simply return null.</p>
 *
 * <p>If a non-null ReturnVal object is passed back, one of two things
 * may be true.  {@link arlut.csd.ganymede.common.ReturnVal#didSucceed() didSucceed()}
 * may return true, in which case the
 * operation was successful, but there may be an informational dialog
 * returned and/or a list of objects and fields that need to be
 * updated in response to the successful update.</p>
 *
 * <p>Alternatively, {@link arlut.csd.ganymede.common.ReturnVal#didSucceed() didSucceed()}
 *  may return false, in which case the
 * operation either could not succeed or is incomplete.  In this case,
 * {@link arlut.csd.ganymede.common.ReturnVal#doRescan() doRescan()} will return false, and
 * {@link arlut.csd.ganymede.common.ReturnVal#getDialog() getDialog()} should return a valid
 * {@link arlut.csd.JDialog.JDialogBuff JDialogBuff}.  If the operation is
 * simply incomplete pending more
 * data from the user, {@link arlut.csd.ganymede.common.ReturnVal#getCallback() getCallback()}
 * will return a non-null value.  In
 * this case, the user should be presented the dialog box, and the
 * results of that dialog should be passed to the callback.  The
 * callback will in return pass back another ReturnVal object.  The
 * server may walk the user through an iterative set of dialogs to
 * finally complete the desired operation.</p>
 *
 * @see arlut.csd.JDialog.JDialogBuff
 * @see arlut.csd.JDialog.DialogRsrc
 * @see arlut.csd.JDialog.StringDialog
 * @see arlut.csd.ganymede.rmi.Ganymediator
 * */

public class ReturnVal implements java.io.Serializable {

  static final boolean debug = false;

  static final long serialVersionUID = 5358187112973957394L;

  public static final byte NONE = 0;
  public static final byte EXPIRATIONSET = 1;
  public static final byte DELETED = 2;
  public static final byte LAST = 2;

  /**
   * TranslationService object for handling string localization in
   * the Ganymede system.
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.common.ReturnVal");

  // ---

  boolean success;
  byte status;

  /**
   * <p>A Serializable Invid that can be returned in response to certain
   * operations on the server.</p>
   */

  Invid newObjectInvid = null;

  /**
   * <p>A remote handle to an RMI reference of various kinds ({@link arlut.csd.ganymede.rmi.db_object db_object},
   * {@link arlut.csd.ganymede.rmi.Session Session}, {@link arlut.csd.ganymede.rmi.XMLSession XMLSession})
   * on the server returned for use by the client.</p>
   */

  private Remote remoteObjectRef = null;

  /**
   * <p>A Serializable StringBuffer representation of objects and fields
   * that need to be rescanned.</p>
   */

  private StringBuffer rescanList;

  /**
   * <p>A Serializable Dialog Definition</p>
   */

  private JDialogBuff dialog;

  /**
   * <p>A Remote handle to a Wizard object on the server</p>
   */

  private Ganymediator callback;

  /**
   * <p>Maps Invid's to RescanBuf's.  Used on the client-side
   * post-serialization.</p>
   */

  private transient Hashtable rescanHash = null;

  /**
   * This field is set if the verifyNewValue() method transforms a
   * value during the input.
   */

  private transient Object transformedValue = null;

  /**
   * This field is set if the verifyNewValue() method transforms a
   * value during the input.
   */

  private transient boolean transformedSet = false;

  /**
   * This variable will be non-null if the operation being reported on
   * changed the object's label.  The GUI client will look for this
   * variable in order to trigger a fix-up of all pointers to the
   * object that was modified by the action that resulted in this
   * ReturnVal.
   *
   * If newLabel is not null, newObjectInvid must be set to point to
   * the Invid which is being relabeled.
   */

  private String newLabel = null;

  /**
   * <p>This boolean variable is used to convey a context-specific
   * flag indicating whether the attempted operation requires
   * exceptional handling.  Some examples of this include the
   * determination whether the field code that invoked wizardHook on a
   * DBEditObject subclass should continue with its normal process or
   * whether it should immediately return this ReturnVal to the
   * (client-side) caller.  It is also used to decide whether a
   * failure to commit a transaction is retryable or not.</p>
   */

  public boolean doNormalProcessing;

  /* -- */

  // client side access

  /**
   * <p>This method returns the general success code for the
   * preceding operation.  If didSucceed() is true, doRescan()
   * should be checked.</p>
   */

  public boolean didSucceed()
  {
    return success;
  }

  /**
   * <p>Certain operations may set status codes.</p>
   */

  public byte getObjectStatus()
  {
    return status;
  }

  /**
   * <p>This method is used to get an Invid that the server
   * wants to return to the client.  Used particularly for
   * {@link arlut.csd.ganymede.rmi.invid_field#createNewEmbedded() invid_field.createNewEmbedded()}.
   * Return null if no Invid was set.</p>
   *
   * @see arlut.csd.ganymede.rmi.invid_field
   * @see arlut.csd.ganymede.server.InvidDBField
   */

  public Invid getInvid()
  {
    return newObjectInvid;
  }

  /** 
   * <p>This method is used to get a remote {@link
   * arlut.csd.ganymede.rmi.db_object db_object} reference that the server
   * wants to return to the client.  Used particularly for
   * Session.create_db_object() / Session.edit_db_object(), or null if
   * no db_object was returned.</p>
   *
   * @see arlut.csd.ganymede.rmi.Session 
   */

  public db_object getObject()
  {
    return (db_object) remoteObjectRef;
  }

  /** 
   * <p>This method is used to get a remote {@link
   * arlut.csd.ganymede.rmi.Session Session} reference that the server
   * wants to return to the client.  Used to return the results
   * of a remote login attempt.  May be null if the login attempt
   * failed.</p>
   */

  public Session getSession()
  {
    return (Session) remoteObjectRef;
  }

  /** 
   * <p>This method is used to get a remote {@link
   * arlut.csd.ganymede.rmi.XMLSession XMLSession} reference that the server
   * wants to return to the client.  Used to return the results
   * of a remote xml login attempt.  May be null if the login attempt
   * failed.</p>
   */

  public XMLSession getXMLSession()
  {
    return (XMLSession) remoteObjectRef;
  }

  /** 
   * <p>This method is used to get a remote {@link
   * arlut.csd.ganymede.rmi.FileTransmitter FileTransmitter} reference that the server
   * wants to return to the client.  Used to provide XML dump results
   * to a remote xmlclient.   May be null if permissions refused the
   * dump attempt.</p>
   */

  public FileTransmitter getFileTransmitter()
  {
    return (FileTransmitter) remoteObjectRef;
  }

  /** 
   * <p>This method is used to get a remote {@link
   * arlut.csd.ganymede.rmi.adminSession adminSession} reference that the
   * server wants to return to the admin console.  Used to return the
   * results of a remote admin console connect attempt.  May be null
   * if the connect attempt failed.</p>
   */

  public adminSession getAdminSession()
  {
    return (adminSession) remoteObjectRef;
  }

  /**
   * <p>If the operation was not successful, this method should
   * return a dialog box describing the problem and, potentially,
   * asking for more information to complete the operation.</p>
   *
   * <p>This method should be checked after all calls to the server that
   * return non-null ReturnVal objects.</p>
   */

  public JDialogBuff getDialog()
  {
    return dialog;
  }

  /**
   * <p>If the operation was not successful, this method should return
   * a the text of any encoded dialog box describing the problem.  This
   * method is intended for text-mode clients that do not support the
   * full callback/wizard features that the
   * {@link arlut.csd.JDialog.JDialogBuff JDialogBuff} 
   * class supports.</p>
   *
   * <p>This method (or getDialog() for GUI clients) should be checked
   * after all calls to the server that return non-null ReturnVal
   * objects.</p> 
   */

  public String getDialogText()
  {
    if (dialog == null)
      {
	return null;
      }
    else
      {
	return dialog.getText();
      }
  }

  /**
   * <p>If the server is asking for more interaction from the user
   * to complete this operation, this method will return an RMI
   * handle to a callback on the server.  The client should
   * popup the dialog box specified by getDialog() and pass the
   * results to the callback returned by this method.</p>
   *
   * <p>This method will return null if getDialog() returns
   * null, and need not be checked in that case.</p>
   */

  public Ganymediator getCallback()
  {
    return callback;
  }

  /**
   * <p>This method returns true if this ReturnVal encodes rescan
   * information for one or more fields in on or more objects.</p>
   *
   * <p>This method will never return true if didSucceed() returns
   * false, and need not be checked in that case.</p>
   */

  public boolean doRescan()
  {
    return !(rescanList == null);
  }

  /**
   * <p>This method returns a Vector of Invid objects, corresponding to
   * those objects which need to have some field rescan work done.</p>
   */

  public Vector getRescanObjectsList()
  {
    Vector result = new Vector();
    Enumeration en;

    /* -- */

    if (!doRescan())
      {
	return result;
      }

    breakOutRescanList();

    en = rescanHash.keys();

    while (en.hasMoreElements())
      {
	result.addElement(en.nextElement());
      }
    
    return result;
  }

  /**
   * If this method returns true, the object that was modified by the
   * operation resulting in this ReturnVal changed the object's label
   * field.  The client will use this as a signal to refresh displayed
   * links to the object's Invid.
   */

  public boolean objectLabelChanged()
  {
    return (this.newLabel != null);
  }

  /**
   * Returns a non-null String if the object pointed to by getInvid()
   * has had its label changed.  The returned String is the new label.
   */

  public String getNewLabel()
  {
    return this.newLabel;
  }

  /**
   * <p>This method returns true if the server is requesting that all
   * fields in the object referenced by the client's preceding call 
   * to the server be reprocessed.</p>
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
   * <p>This method returns a Vector of Short() objects if the server
   * provided an explicit list of fields that need to be reprocessed,
   * or null if all or no fields need to be processed.</p>
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
   * <p>This method returns an encoded string representing
   * the objects and fields to be rescanned by the
   * client in response to this ReturnVal.</p>
   *
   * <p>To be used for debugging.</p>
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
   * <p>This private method converts the rescanList StringBuffer to
   * a Hashtable (rescanHash) that maps Invid's to either Vector of
   * Short's or "all".</p>
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
   * <p>This method takes a StringBuffer encoded as follows:</p>
   *
   * <pre>263:170|all|271:131|31|57|286:41|all|310:4|134|13|92|</pre>
   *
   * <p>and returns a Hashtable mapping Invid's to the rescan information
   * for that Invid, where the rescan information will either be the
   * String "all", indicating that all fields need to be rescanned, or
   * a Vector of Short's specifying field id's to be rescanned for
   * that object.</p>
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
	    invid = Invid.createInvid(atom);
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

  public String toString()
  {
    String result = "ReturnVal [";

    /* -- */

    if (dialog != null)
      {
	result = result + "\"" + dialog.getText() + "\"";
      }
    else
      {
	result = result + "\"\"";
      }

    if (didSucceed())
      {
	result = result + ", success";
      }
    else
      {
	result = result + ", failure";
      }

    if (doNormalProcessing)
      {
	result = result + ", normal";
      }
    else
      {
	result = result + ", abnormal";
      }

    if (newObjectInvid != null)
      {
	result = result + ", invid set";
      }
    else
      {
	result = result + ", invid not set";
      }

    if (remoteObjectRef != null)
      {
	result = result + ", remote obj set";
      }
    else
      {
	result = result + ", remote obj not set";
      }

    if (callback != null)
      {
	result = result + ", callback set";
      }
    else
      {
	result = result + ", callback not set";
      }

    if (rescanList != null)
      {
	result = result + ", rescan set";
      }
    else
      {
	result = result + ", rescan not set";
      }

    result = result + "]";

    return result;
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
   * <p>unionRescan merges field and object rescan requests from
   * the supplied ReturnVal with and rescan requests we contain.</p>
   *
   * <p>It is used to allow multiple sources in InvidDBField to contribute
   * rescan requests.</p>
   *
   * <p>This method returns this so you can do a cascading return.</p>
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
   * <p>This method takes a Hashtable mapping Invid's to Vectors
   * of Short field identifiers or the String "all" and generates
   * the StringBuffer to be serialized down to the client.</p>
   *
   * <p>For use on the server-side.</p>
   */

  private void encodeRescanList(Hashtable rescanTable)
  {
    Enumeration en;
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

    en = rescanTable.keys();

    while (en.hasMoreElements())
      {
	invid = (Invid) en.nextElement();

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

  public void setSuccess(boolean didSucceed)
  {
    this.success = didSucceed;
  }

  /**
   * This method controls whether or not this ReturnVal will return a
   * 'my label changed!' message to the client.
   */

  public void setObjectLabelChanged(Invid objInvid, String newLabel)
  {
    if (newObjectInvid != null)
      {
	throw new RuntimeException();
      }

    this.newObjectInvid = objInvid;
    this.newLabel = newLabel;
  }

  /**
   * <p>This method makes a note in this ReturnVal to have the
   * client rescan all fields in object objID.</p>
   *
   * <p>For use on the server-side.</p>
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
   * <p>This method makes a note in this ReturnVal to have the
   * client rescan field fieldID in object objID.</p>
   *
   * <p>For use on the server-side.</p>
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

  /** 
   * <p>This method attaches a remote reference to a
   * {@link arlut.csd.ganymede.rmi.Ganymediator} 
   * wizard-handler to this ReturnVal for extraction by the client.</p>
   *
   * <p>For use on the server-side.</p> */

  public void setCallback(Ganymediator callback)
  {
    this.callback = callback;
  }

  /**
   * This method sets up a basic error text dialog for this ReturnVal.
   *
   * Unlike {@link
   * arlut.csd.ganymede.server.Ganymede#createErrorDialog(java.lang.String,
   * java.lang.String)}, this method does not write the error text to
   * stderr.
   */

  public void setErrorText(String body)
  {
    this.setErrorText(ts.l("setErrorText.default_title"), body);
  }

  /**
   * This method sets up a basic error text dialog for this ReturnVal.
   *
   * Unlike {@link
   * arlut.csd.ganymede.server.Ganymede#createErrorDialog(java.lang.String,
   * java.lang.String)}, this method does not write the error text to
   * stderr.
   */

  public void setErrorText(String title, String body)
  {
    this.dialog = new JDialogBuff(title, body, 
				  ts.l("setErrorText.ok"),
				  null,
				  "error.gif");
  }

  /**
   * This method sets up a basic info text dialog for this ReturnVal.
   *
   * Unlike {@link
   * arlut.csd.ganymede.server.Ganymede#createErrorDialog(java.lang.String,
   * java.lang.String)}, this method does not write the error text to
   * stderr.
   */

  public void setInfoText(String body)
  {
    this.setInfoText(ts.l("setInfoText.default_title"), body);
  }

  /**
   * This method sets up a basic info text dialog for this ReturnVal.
   *
   * Unlike {@link
   * arlut.csd.ganymede.server.Ganymede#createErrorDialog(java.lang.String,
   * java.lang.String)}, this method does not write the error text to
   * stderr.
   */

  public void setInfoText(String title, String body)
  {
    this.dialog = new JDialogBuff(title, body, 
				  ts.l("setInfoText.ok"),
				  null,
				  "ok.gif");
  }

  /**
   * <p>This method attaches a dialog definition to this ReturnVal
   * for extraction by the client.</p>
   *
   * <p>For use on the server-side.</p>
   */

  public void setDialog(JDialogBuff dialog)
  {
    this.dialog = dialog;
  }

  /**
   * <p>This method is used to set an Invid that the client
   * can retrieve from us in those cases where a method
   * on the server really does need to return an Invid
   * _and_ a return val.</p>
   *
   * <p>For use on the server-side.</p>
   */

  public void setInvid(Invid invid)
  {
    this.newObjectInvid = invid;
  }

  /** 
   * <p>This method is used to set a {@link
   * arlut.csd.ganymede.rmi.db_object db_object} reference that the client
   * can retrieve from us in those cases where a method on the server
   * really does need to return a db_object _and_ a return val.</p>
   *
   * <p>For use on the server-side.</p> 
   */

  public void setObject(db_object object)
  {
    this.remoteObjectRef = object;
  }

  /** 
   * <p>This method is used to set a {@link
   * arlut.csd.ganymede.rmi.Session Session} reference that the client
   * can retrieve from us at login time.</p>
   *
   * <p>For use on the server-side.</p> 
   */

  public void setSession(Session session)
  {
    this.remoteObjectRef = session;
  }

  /** 
   * <p>This method is used to set a {@link
   * arlut.csd.ganymede.rmi.XMLSession XMLSession} reference that the client
   * can retrieve from us at login time.</p>
   *
   * <p>For use on the server-side.</p> 
   */

  public void setXMLSession(XMLSession session)
  {
    this.remoteObjectRef = session;
  }

  /** 
   * <p>This method is used to set a {@link
   * arlut.csd.ganymede.rmi.FileTransmitter FileTransmitter} reference that the client
   * can retrieve from us.</p>
   *
   * <p>For use on the server-side.</p> 
   */

  public void setFileTransmitter(FileTransmitter transmitter)
  {
    this.remoteObjectRef = transmitter;
  }

  /** 
   * <p>This method is used to set a {@link
   * arlut.csd.ganymede.rmi.adminSession adminSession} reference that the
   * admin console can retrieve from us at console connect time.</p>
   *
   * <p>For use on the server-side.</p> 
   */

  public void setAdminSession(adminSession session)
  {
    this.remoteObjectRef = session;
  }

  /**
   * This method is intended to be used by {@link
   * arlut.csd.ganymede.server.DBEditObject#verifyNewValue(arlut.csd.ganymede.server.DBField,
   * java.lang.Object)}, when the verifyNewValue() method wants to
   * take the submitted input and canonicalize it.
   *
   * Code in the Ganymede server (mostly the base logic in DBField)
   * which calls the verifyNewValue() method should respond to a
   * transformed value by substituting the transformed value for the
   * originally submitted value.
   *
   * If a value is transformed, setTranformedValueObject() will also
   * set the ReturnVal so that it encodes a rescan of the field in
   * question so the client will refresh it.
   */

  public void setTransformedValueObject(Object obj, Invid invid, short fieldId)
  {
    this.transformedSet = true;
    this.transformedValue = obj;

    requestRefresh(invid, fieldId);
  }

  /**
   * This method returns true if the code returning this ReturnVal
   * wants to substitute a canonicalized value for the value submitted
   * to verifyNewValue().
   *
   * Code in the Ganymede server (mostly the base logic in DBField)
   * which calls the verifyNewValue() method should respond if this
   * method returns true by substituting the transformed value for the
   * originally submitted value.
   */

  public boolean hasTransformedValue()
  {
    return this.transformedSet;
  }

  /**
   * Code in the Ganymede server (mostly the base logic in DBField)
   * which calls the verifyNewValue() method should respond to a
   * transformed value by substituting the transformed value for the
   * originally submitted value.
   */

  public Object getTransformedValueObject()
  {
    return this.transformedValue;
  }

  /**
   * This method causes this ReturnVal to request that the field we're
   * manipulating will be refreshed by the client.
   */

  public void requestRefresh(Invid invid, short fieldId)
  {
    // create a temporary ReturnVal so that we can union it in

    ReturnVal tempRetVal = new ReturnVal(true);

    Hashtable rescanInfo = new Hashtable(1);
    rescanInfo.put(invid, new Short(fieldId));

    tempRetVal.encodeRescanList(rescanInfo);
    this.unionRescan(tempRetVal);
  }

}
