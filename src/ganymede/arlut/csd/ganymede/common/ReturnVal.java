/*

   ReturnVal.java

   This class is a serializable return code that is returned from
   most Ganymede server operations that need to pass back some
   sort of status information to the client.  
   
   Created: 27 January 1998

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

package arlut.csd.ganymede.common;

import java.rmi.Remote;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
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
 * <p>When a ReturnVal is returned, the {@link
 * arlut.csd.ganymede.common.ReturnVal#didSucceed() didSucceed()}
 * determines whether the operation was considered to have been
 * successful.  There may be a good bit of additional metadata passed
 * back with the successful result, including an informational dialog
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
 * <p>ReturnVal is not thread safe, so don't use it in multiple
 * concurrent threads.</p>
 *
 * @see arlut.csd.JDialog.JDialogBuff
 * @see arlut.csd.JDialog.DialogRsrc
 * @see arlut.csd.JDialog.StringDialog
 * @see arlut.csd.ganymede.rmi.Ganymediator
 * */

public final class ReturnVal implements java.io.Serializable {

  static final boolean debug = false;
  static final long serialVersionUID = 5358187112973957394L;

  /**
   * Sentinel object representing an order to have the client refresh
   * all objects.
   */

  private static final Vector<Short> all = new Vector<Short>();

  /**
   * TranslationService object for handling string localization in
   * the Ganymede system.
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.common.ReturnVal");

  /**
   * static factory method for returning a ReturnVal indicating
   * success.  Because setter methods may be made on the ReturnVal
   * that we return, we'll create a new one each time.
   */

  static final public ReturnVal success()
  {
    return new ReturnVal(true, true);
  }

  /**
   * Simple static helper method that Ganymede code can use to verify
   * that a ReturnVal-returning operation did succeed.
   */

  static public boolean didSucceed(ReturnVal retVal)
  {
    return retVal == null || retVal.didSucceed();
  }

  /**
   * Simple static helper method that Ganymede code can use to verify
   * that a ReturnVal-returning method involved transforming a
   * supplied value.
   */

  static public boolean hasTransformedValue(ReturnVal retVal)
  {
    return retVal != null && retVal.hasTransformedValue();
  }

  /**
   * Simple static helper method that Ganymede code can use to verify
   * that a ReturnVal is either null (indicating unconditional
   * success) or doNormalProcessing set.
   */

  static public boolean isDoNormalProcessing(ReturnVal retVal)
  {
    return retVal == null || retVal.doNormalProcessing;
  }

  /**
   * Simple static helper method that checks both for failure and for
   * wizard intercept.  Useful for server-side code that checks for
   * wizard interaction.
   */

  static public boolean wizardHandled(ReturnVal retVal)
  {
    return !ReturnVal.didSucceed(retVal) || !ReturnVal.isDoNormalProcessing(retVal);
  }

  /**
   * This static method is responsible for intelligently merging a
   * pair of ReturnVal objects, ensuring that the appropriate
   * information from each is propagated forward.
   *
   * The logic in this method is critical for the proper chaining of
   * results in server-side code.
   */

  static final public ReturnVal merge(ReturnVal retVal, ReturnVal retVal2)
  {
    if (retVal2 == null)
      {
        return retVal;
      }

    if (retVal == null)
      {
        return retVal2;
      }

    if (retVal.isCompatible(retVal2))
      {
	return retVal;
      }

    // if one of the ReturnVals indicated a failure, pass the failure
    // through, and we'll just forget the success result as immaterial

    if (retVal.didSucceed() != retVal2.didSucceed())
      {
        if (!retVal.didSucceed())
          {
            return retVal;
          }
        else
          {
            return retVal2;
          }
      }

    // okay, we've got compatible success results, let's create a
    // result we can work with going forward

    ReturnVal result = new ReturnVal(retVal.didSucceed());

    if (retVal.didSucceed())
      {
	// we know that both of the ReturnVals that we're merging were
	// successful, so we'll want to merge all rescan information from
	// both.

	result.unionRescan(retVal);
	result.unionRescan(retVal2);
      }

    // doNormalProcessing is meant to be a signal that the normal
    // course of action is not to be followed.  We'll want to preserve
    // that signal going forward.
    //
    // Generally, we use doNormalProcessing for a few different
    // semantic interpretations.. one is for DBEditObject.wizardHook()
    // to signal to the field setting methods that an exception to the
    // default behavior is desired, and that the field logic should
    // let the wizard handle it.  The second is to indicate whether an
    // attempted transaction commit should be considered as retryable
    // by the client.  The third, most minor one, is when an object is
    // cloned but certain fields could not be successfully or
    // completely cloned during the process.
    //
    // A wizard can't really do anything useful if doNormalProcessing
    // is set to false unless the retVal has a Ganymediator returned
    // to be responsible for further processing, so we'll check for
    // that first.  If we see a wizard active, we'll pass its dialog
    // and ganymediator information through to our result.

    if (retVal.callback != null)
      {
        result.callback = retVal.callback;
        result.dialog = retVal.dialog;
        result.doNormalProcessing = retVal.doNormalProcessing;
      }
    else if (retVal2.callback != null)
      {
        result.callback = retVal2.callback;
        result.dialog = retVal2.dialog;
        result.doNormalProcessing = retVal2.doNormalProcessing;
      }
    else
      {
	// if we're not involved in a wizard bit, we'll pass back a
	// true doNormalProcessing only if both of our inputs had
	// doNormalProcessing.

	result.doNormalProcessing = retVal.doNormalProcessing && retVal2.doNormalProcessing;

	// no wizard?  look to see if either or both of the ReturnVals
	// that we are merging have any dialog information, and put
	// either or both of them together in the result.

	if (retVal.dialog != null && retVal2.dialog != null)
	  {
	    // ugh, we've got two dialogs that need to be merged, so we'll
	    // have to do something about that.
	    //
	    // if either one was an error dialog, we'll prioritize that as
	    // far as the title is concerned.  Otherwise, the first
	    // ReturnVal we're merging will have priority for the title,
	    // image, and text.

	    if ("error.gif".equals(retVal2.dialog.getImageName()))
	      {
		result.dialog = retVal2.dialog;
		result.dialog.appendText(retVal.dialog.getText());
	      }
	    else
	      {
		result.dialog = retVal.dialog;
		result.dialog.appendText(retVal2.dialog.getText());
	      }
	  }
        else if (retVal.dialog != null)
          {
            result.dialog = retVal.dialog;
          }
        else if (retVal2.dialog != null)
          {
            result.dialog = retVal2.dialog;
          }
      }

    // if either provide a newObjectInvid, or remoteObjectRef, we'll
    // want to include that.  if both try to provide either of those,
    // we'll have to throw an exception and give up.

    if (retVal.newObjectInvid != null || retVal2.newObjectInvid != null)
      {
        if (retVal.newObjectInvid != null && retVal2.newObjectInvid == null)
          {
            result.newObjectInvid = retVal.newObjectInvid;
            result.newLabel = retVal.newLabel;
          }
        else if (retVal2.newObjectInvid != null && retVal.newObjectInvid == null)
          {
            result.newObjectInvid = retVal2.newObjectInvid;
            result.newLabel = retVal2.newLabel;
          }
        else if (retVal2.newObjectInvid == retVal.newObjectInvid)  // remember we intern Invids
          {
            result.newObjectInvid = retVal.newObjectInvid;

            // they both agree on the invid, but what about the label?
            //
            // we'll give priority to the first ReturnVal if it was
            // not null.. otherwise we'll leave it null

            if (retVal.newLabel == null)
              {
                result.newLabel = retVal2.newLabel;
              }
            else
              {
                result.newLabel = retVal.newLabel;
              }
          }
        else
          {
            throw new RuntimeException("Couldn't merge ReturnVals with conflicting newObjectInvids.");
          }
      }

    // and the same basic logic for the remoteObjectRef.

    if (retVal.remoteObjectRef != null || retVal2.remoteObjectRef != null)
      {
        if (retVal.remoteObjectRef != null && retVal2.remoteObjectRef == null)
          {
            result.remoteObjectRef = retVal.remoteObjectRef;
          }
        else if (retVal2.remoteObjectRef != null && retVal.remoteObjectRef == null)
          {
            result.remoteObjectRef = retVal2.remoteObjectRef;
          }
        else if (retVal2.remoteObjectRef == retVal.remoteObjectRef)
          {
            // they both agree, who cares

            result.remoteObjectRef = retVal.remoteObjectRef;
          }
        else
          {
            throw new RuntimeException("Couldn't merge ReturnVals with conflicting remoteObjectRefs.");
          }
      }

    return result;
  }

  // ---

  /**
   * If true, the operation that this ReturnVal is reporting on
   * succeeded.
   */

  boolean success;

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

  private StringBuffer rescanList = null;

  /**
   * <p>A Serializable Dialog Definition</p>
   */

  private JDialogBuff dialog = null;

  /**
   * <p>A Remote handle to a Wizard object on the server</p>
   */

  private Ganymediator callback = null;

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

  /**
   * <p>Maps Invids to a Vector of Shorts representing fields in the
   * Invid objects that need to be refreshed by the client.</p>
   *
   * <p>Used on the client-side post-serialization.</p>
   */

  private transient HashMap<Invid,Vector<Short>> rescanHash = null;

  /**
   * This field is set if the verifyNewValue() method transforms a
   * value during the input.
   *
   * Server-side only.
   */

  private transient Object transformedValue = null;

  /**
   * This field is set if the verifyNewValue() method transforms a
   * value during the input.
   *
   * Server-side only.
   */

  private transient boolean transformedSet = false;

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

  public Vector<Invid> getRescanObjectsList()
  {
    if (!doRescan())
      {
	return new Vector<Invid>();
      }

    breakOutRescanList();

    return new Vector<Invid>(rescanHash.keySet()); // copy
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

    if (rescanHash.containsKey(objID) && rescanHash.get(objID) == all)
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
  
  public Vector<Short> getRescanList(Invid objID)
  {
    if (!doRescan())
      {
	return null;
      }

    breakOutRescanList();

    Vector<Short> result = rescanHash.get(objID);

    if (result == null || result == all)
      {
	return null;
      }

    return result;
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
    StringBuilder buffer = new StringBuilder();

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
   * a HashMap (rescanHash) that maps Invid's to either Vector of
   * Short's or "all".</p>
   */

  private void breakOutRescanList()
  {
    if (rescanHash != null)
      {
	return;
      }

    rescanHash = new HashMap<Invid, Vector<Short>>();
    decodeRescanList(rescanList, rescanHash);
  }

  /**
   * <p>This method takes a StringBuffer encoded as follows:</p>
   *
   * <pre>263:170|all|271:131|31|57|286:41|all|310:4|134|13|92|</pre>
   *
   * <p>and returns a HashMap mapping Invid's to the rescan information
   * for that Invid, where the rescan information will either be the
   * String "all", indicating that all fields need to be rescanned, or
   * a Vector of Short's specifying field id's to be rescanned for
   * that object.</p>
   *
   * @param buffer The StringBuffer to be decoded.
   * @param original The HashMap to put the results into.. this method
   * will put into original the Union of the field rescan information
   * specified in original and the rescan information held in buffer.
   *  
   * @return A reference to original.
   */

  private HashMap<Invid, Vector<Short>> decodeRescanList(StringBuffer buffer, HashMap<Invid, Vector<Short>> original)
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
	    original.put(invid, all);
	  }
	else
	  {
	    Vector vec;
	    Short fieldID = Short.valueOf(atom);

	    if (original.containsKey(invid) && original.get(invid) != all)
	      {
		vec = (Vector<Short>) original.get(invid);

		if (!vec.contains(fieldID))
		  {
		    vec.addElement(fieldID);
		  }
	      }
	    else if (!original.containsKey(invid))
	      {
		vec = new Vector<Short>();
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
    StringBuilder result = new StringBuilder("ReturnVal [");

    /* -- */

    if (dialog != null)
      {
	result.append("\"");
	result.append(dialog.getText());
	result.append("\"");
      }
    else
      {
	result.append("\"\"");
      }

    if (didSucceed())
      {
	result.append(", success");
      }
    else
      {
	result.append(", failure");
      }

    if (doNormalProcessing)
      {
	result.append(", normal");
      }
    else
      {
	result.append(", abnormal");
      }

    if (newObjectInvid != null)
      {
	result.append(", invid set");
      }
    else
      {
	result.append(", invid not set");
      }

    if (remoteObjectRef != null)
      {
	result.append(", remote obj set");
      }
    else
      {
	result.append(", remote obj not set");
      }

    if (callback != null)
      {
	result.append(", callback set");
      }
    else
      {
	result.append(", callback not set");
      }

    if (rescanList != null)
      {
	result.append(", rescan set");
      }
    else
      {
	result.append(", rescan not set");
      }

    result.append("]");

    return result.toString();
  }

  // ---------------------------------------------------------------------------
  // server side operations
  // ---------------------------------------------------------------------------

  /**
   * Base constructor
   */

  public ReturnVal(boolean success, boolean doNormalProcessing)
  {
    this.success = success;
    this.doNormalProcessing = doNormalProcessing;
  }

  /**
   * Short-cut constructor
   */

  public ReturnVal(boolean success)
  {
    this(success, success);     // we now have doNormalProcessing set to the same as success 28 Feb 2008
  }

  public void clear()
  {
    rescanList = null;
    dialog = null;
    callback = null;
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
	    HashMap<Invid,Vector<Short>> result = new HashMap<Invid,Vector<Short>>();

	    decodeRescanList(retVal.rescanList, result);
	    decodeRescanList(rescanList, result);

	    encodeRescanList(result);
	  }
      }

    return this;
  }

  /**
   * <p>This method takes a HashMap mapping Invid's to Vectors
   * of Short field identifiers or the String "all" and generates
   * the StringBuffer to be serialized down to the client.</p>
   *
   * <p>For use on the server-side.</p>
   */

  private void encodeRescanList(HashMap<Invid,Vector<Short>> rescanTable)
  {
    if (rescanList == null)
      {
	rescanList = new StringBuffer();
      }
    else
      {
	rescanList.setLength(0);
      }

    for (Map.Entry<Invid, Vector<Short>> entry: rescanTable.entrySet())
      {
	Invid invid = entry.getKey();
	Vector<Short> fields = entry.getValue();

	rescanList.append(invid.toString());
	rescanList.append("|");
	
	if (fields == all)
	  {
	    rescanList.append("all|");
	  }
	else
	  {
	    for (Short field: fields)
	      {
		rescanList.append(field.toString());
		rescanList.append("|");
	      }
	  }
      }
  }

  public ReturnVal setSuccess(boolean didSucceed)
  {
    this.success = didSucceed;

    return this;
  }

  /**
   * This method controls whether or not this ReturnVal will return a
   * 'my label changed!' message to the client.
   */

  public ReturnVal setObjectLabelChanged(Invid objInvid, String newLabel)
  {
    if (newObjectInvid != null)
      {
	throw new RuntimeException();
      }

    this.newObjectInvid = objInvid;
    this.newLabel = newLabel;

    return this;
  }

  /**
   * <p>This method makes a note in this ReturnVal to have the
   * client rescan all fields in object objID.</p>
   *
   * <p>For use on the server-side.</p>
   */

  public synchronized ReturnVal setRescanAll(Invid objID)
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

    return this;
  }

  /**
   * <p>This method makes a note in this ReturnVal to have the
   * client rescan field fieldID in object objID.</p>
   *
   * <p>For use on the server-side.</p>
   */

  public synchronized ReturnVal addRescanField(Invid objID, short fieldID)
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

    return this;
  }

  /** 
   * <p>This method attaches a remote reference to a
   * {@link arlut.csd.ganymede.rmi.Ganymediator} 
   * wizard-handler to this ReturnVal for extraction by the client.</p>
   *
   * <p>For use on the server-side.</p>
   */

  public ReturnVal setCallback(Ganymediator callback)
  {
    this.callback = callback;

    return this;
  }

  /**
   * This method sets up a basic error text dialog for this ReturnVal.
   *
   * Unlike {@link
   * arlut.csd.ganymede.server.Ganymede#createErrorDialog(java.lang.String,
   * java.lang.String)}, this method does not write the error text to
   * stderr.
   */

  public ReturnVal setErrorText(String body)
  {
    this.setErrorText(ts.l("setErrorText.default_title"), body);

    return this;
  }

  /**
   * This method sets up a basic error text dialog for this ReturnVal.
   *
   * Unlike {@link
   * arlut.csd.ganymede.server.Ganymede#createErrorDialog(java.lang.String,
   * java.lang.String)}, this method does not write the error text to
   * stderr.
   */

  public ReturnVal setErrorText(String title, String body)
  {
    this.dialog = new JDialogBuff(title, body, 
				  ts.l("setErrorText.ok"),
				  null,
				  "error.gif");

    return this;
  }

  /**
   * This method sets up a basic info text dialog for this ReturnVal.
   *
   * Unlike {@link
   * arlut.csd.ganymede.server.Ganymede#createErrorDialog(java.lang.String,
   * java.lang.String)}, this method does not write the error text to
   * stderr.
   */

  public ReturnVal setInfoText(String body)
  {
    this.setInfoText(ts.l("setInfoText.default_title"), body);

    return this;
  }

  /**
   * This method sets up a basic info text dialog for this ReturnVal.
   *
   * Unlike {@link
   * arlut.csd.ganymede.server.Ganymede#createErrorDialog(java.lang.String,
   * java.lang.String)}, this method does not write the error text to
   * stderr.
   */

  public ReturnVal setInfoText(String title, String body)
  {
    this.dialog = new JDialogBuff(title, body, 
				  ts.l("setInfoText.ok"),
				  null,
				  "ok.gif");

    return this;
  }

  /**
   * <p>This method attaches a dialog definition to this ReturnVal
   * for extraction by the client.</p>
   *
   * <p>For use on the server-side.</p>
   */

  public ReturnVal setDialog(JDialogBuff dialog)
  {
    this.dialog = dialog;

    return this;
  }

  /**
   * <p>This method is used to set an Invid that the client
   * can retrieve from us in those cases where a method
   * on the server really does need to return an Invid
   * _and_ a return val.</p>
   *
   * <p>For use on the server-side.</p>
   */

  public ReturnVal setInvid(Invid invid)
  {
    this.newObjectInvid = invid;

    return this;
  }

  /** 
   * <p>This method is used to set a {@link
   * arlut.csd.ganymede.rmi.db_object db_object} reference that the client
   * can retrieve from us in those cases where a method on the server
   * really does need to return a db_object _and_ a return val.</p>
   *
   * <p>For use on the server-side.</p> 
   */

  public ReturnVal setObject(db_object object)
  {
    this.remoteObjectRef = object;

    return this;
  }

  /** 
   * <p>This method is used to set a {@link
   * arlut.csd.ganymede.rmi.Session Session} reference that the client
   * can retrieve from us at login time.</p>
   *
   * <p>For use on the server-side.</p> 
   */

  public ReturnVal setSession(Session session)
  {
    this.remoteObjectRef = session;

    return this;
  }

  /** 
   * <p>This method is used to set a {@link
   * arlut.csd.ganymede.rmi.XMLSession XMLSession} reference that the client
   * can retrieve from us at login time.</p>
   *
   * <p>For use on the server-side.</p> 
   */

  public ReturnVal setXMLSession(XMLSession session)
  {
    this.remoteObjectRef = session;

    return this;
  }

  /** 
   * <p>This method is used to set a {@link
   * arlut.csd.ganymede.rmi.FileTransmitter FileTransmitter} reference that the client
   * can retrieve from us.</p>
   *
   * <p>For use on the server-side.</p> 
   */

  public ReturnVal setFileTransmitter(FileTransmitter transmitter)
  {
    this.remoteObjectRef = transmitter;

    return this;
  }

  /** 
   * <p>This method is used to set a {@link
   * arlut.csd.ganymede.rmi.adminSession adminSession} reference that the
   * admin console can retrieve from us at console connect time.</p>
   *
   * <p>For use on the server-side.</p> 
   */

  public ReturnVal setAdminSession(adminSession session)
  {
    this.remoteObjectRef = session;

    return this;
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
   * This version of setTransformedValueObject() only sets the
   * transformed value to be returned, but an additional call will
   * need to be made on this ReturnVal to set a refresh order for the
   * field which triggered this ReturnVal.
   */

  public ReturnVal setTransformedValueObject(Object obj)
  {
    this.transformedSet = true;
    this.transformedValue = obj;

    return this;
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

  public ReturnVal setTransformedValueObject(Object obj, Invid invid, short fieldId)
  {
    this.transformedSet = true;
    this.transformedValue = obj;

    requestRefresh(invid, fieldId);

    return this;
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

  public ReturnVal requestRefresh(Invid invid, short fieldId)
  {
    // create a temporary ReturnVal so that we can union it in

    ReturnVal tempRetVal = new ReturnVal(true);

    HashMap<Invid,Vector<Short>> rescanInfo = new HashMap<Invid,Vector<Short>>(1);

    Vector<Short> fields = new Vector<Short>(1);
    fields.addElement(Short.valueOf(fieldId));

    rescanInfo.put(invid, fields);

    tempRetVal.encodeRescanList(rescanInfo);

    return this.unionRescan(tempRetVal);
  }

  /**
   * This private helper for the static merge() method is used to
   * determine whether two ReturnVals are trivially identical, in
   * which case the merge() method will not need to create a new
   * result object.
   *
   * Useful especially in avoiding extra work when merging simple
   * ReturnVal.success() objects.
   */

  private boolean isCompatible(ReturnVal retVal)
  {
    if ((success != retVal.success) ||
	(doNormalProcessing != retVal.doNormalProcessing) ||
	(newObjectInvid != null) || 
	(newLabel != null) ||
	(retVal.newObjectInvid != null) ||
	(retVal.newLabel != null) ||
	(remoteObjectRef != null) ||
	(retVal.remoteObjectRef != null) ||
	(rescanList != null) ||
	(retVal.rescanList != null) ||
	(dialog != null) ||
	(retVal.dialog != null) ||
	(callback != null) ||
	(retVal.callback != null))
      {
	return false;
      }

    return true;
  }
}
