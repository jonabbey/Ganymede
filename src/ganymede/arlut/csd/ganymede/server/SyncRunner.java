/*

   SyncRunner.java

   This class is used in the Ganymede server to handle sync channel
   synchronization.  It is responsible for acting as a Runnable in the
   Ganymede scheduler (when run, it executes the external service program for
   a given sync channel queue) and for tracking the data associated with the
   sync channel.

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

import arlut.csd.ganymede.common.Invid;
import arlut.csd.ganymede.common.ObjectStatus;
import arlut.csd.ganymede.common.SchemaConstants;

import arlut.csd.Util.FileOps;
import arlut.csd.Util.TranslationService;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;

import com.jclark.xml.output.UTF8XMLWriter;

/*------------------------------------------------------------------------------
									   class
								      SyncRunner

------------------------------------------------------------------------------*/

/**
 * <p>This class is used in the Ganymede server to handle Sync
 * Channel-style incremental synchronization.  Unlike the full-state
 * {@link arlut.csd.ganymede.server.GanymedeBuilderTask} system,
 * SyncRunner is not designed to be customized through subclassing.
 * Instead, all SyncRunner objects generate a common XML file format
 * for writing out synchronization data.  SyncRunner objects are
 * registered in the server for execution automatically when a
 * <code>Sync Channel</code> object (using the {@link
 * arlut.csd.ganymede.server.syncChannelCustom} DBEditObject subclass
 * for management) is created in the Ganymede data store.  This
 * <code>Sync Channel</code> object provides all of the configuration
 * controls which determine where the XML synchronization files are
 * written, what external program should be used to process the files,
 * and what data needs to be written out for synchronization.</p>
 *
 * <p>Like {@link arlut.csd.ganymede.server.GanymedeBuilderTask},
 * SyncRunner synchronization is done in a split phase manner, in
 * which step 1 writes out data files and step 2 executes an external
 * script to process the files.  Unlike GanymedeBuilderTask,
 * SyncRunner's step 1 is done synchronously with transaction commit.
 * Every time a transaction is committed, the Ganymede server compares
 * the objects involved in the transaction against every registered
 * <code>Sync Channel</code> object.  If any of the objects or fields
 * created, deleted, or edited during the transaction matches against
 * a Sync Channel's so-called <code>Field Options</code> matrix of
 * objects and fields to sync, that Sync Channel will write an XML
 * file to the directory configured in the Sync Channel's
 * <code>Queue Directory</code> field.</p>
 *
 * <p>Each XML file that is created in a Sync Channel output directory
 * is given a unique filename, based on the number of transactions
 * committed by the Ganymede server since the server's database was
 * first initialized.  The first transaction committed is transaction
 * 0, the second is 1, and so forth.  These transaction numbers are
 * global in the Ganymede server.  Every time any transaction is
 * committed, the transaction number is incremented, whether or not
 * any Sync Channels match against the transaction.</p>
 *
 * <p>These XML files are written out synchronously with the
 * transaction commit.  What this means is that the transaction is not
 * counted as successfully committed until all SyncRunners that match
 * against the transaction successfully write and flush their XML
 * files to the proper output directory.  If a problem prevents any of
 * the SyncRunners from successfully writing its XML files, the
 * transaction will be aborted as if it never happened.  All of this
 * is done with proper ACID transactional guarantees.  The SyncRunner
 * implementation is designed so that the Ganymede server can be
 * killed at any time without leaving a transaction partially
 * committed between the Sync Channels and the Ganymede journal file.
 * Either a transaction is successfully recorded to all relevant Sync
 * Channels and the journal, or it will not be recorded to any of
 * them.</p>
 *
 * <p>All of this behavior corresponds to the logic that is
 * implemented in the {@link
 * arlut.csd.ganymede.server.GanymedeBuilderTask#builderPhase1()}
 * method in the GanymedeBuilderTask class.  The SyncRunner equivalent
 * to the {@link
 * arlut.csd.ganymede.server.GanymedeBuilderTask#builderPhase2()}
 * method is provided by the <code>Service Program</code> specified in
 * the corresponding <code>Sync Channel</code> object in the Ganymede
 * data store.</p>
 *
 * <p>Whenever any transaction is successfully committed, each Sync
 * Runner is scheduled for execution by the {@link
 * arlut.csd.ganymede.server.GanymedeScheduler}.  When the Sync
 * Runner's {@link arlut.csd.ganymede.server.SyncRunner#run()} method
 * is called, it runs its external <code>Service Program</code>,
 * passing the most recently committed transaction number as a single
 * command line argument.  The <code>Service Program</code> is meant
 * to examine the <code>Queue Directory</code> specified in the
 * <code>Sync Channel</code> object, and to process any XML files with
 * numbers less than or equal to its command line argument.  As is the
 * case with GanymedeBuilderTask, the GanymedeScheduler will not
 * relaunch a SyncRunner until the previous execution of the
 * SyncRunner completes.  Unlike GanymedeBuilderTask, the Ganymede
 * server does not prevent new files from being written out while the
 * <code>Service Program</code> is being executed.  It is the
 * responsibility of the <code>Service Program</code> to ignore any
 * XML files that it finds with transaction numbers higher than that
 * passed to it on the command line.</p>
 *
 * <p>In this way, the Sync Channel system allows transactions to be
 * committed at a rapid rate, while allowing the <code>Service
 * Program</code> to take as little or as much time as is required to
 * process transactions.  The principle of back-to-back builds is very
 * much part of this Ganymede synchronization mechanism as well.</p>
 *
 * <p>Because the Sync Channel transaction files are generated while
 * the transaction is being committed, the Sync Channel writing code
 * has complete access to the before and after state of all objects in
 * the transaction.  This before and after information is incorporated
 * into each XML file, so that the external <code>Service
 * Program</code> has access to the change context in order to apply
 * the appropriate changes against the directory service target.</p>
 *
 * <p>Because Sync Channel synchronization is based on applying
 * changes to a directory service target, it works best on directory
 * services that can be updated incrementally, like LDAP.  It is not
 * designed for systems that require full-state replacement in order
 * to make changes at all, such as NIS or classical DNS.  Even for
 * systems that can accept incremental changes, however, the use of
 * discrete deltas for Ganymede synchronization can be problematic.
 * If an XML transaction file cannot successfully be applied to a
 * directory service target, the Ganymede server has no way of knowing
 * this, because the Service Program is not run until sometime after
 * the transaction has been successfully committed.</p>
 *
 * <p>In order to cope with this, the Sync Channel system has
 * provisions for doing 'full-state' XML dumps as well.  You can do
 * this manually by using the Ganymede xmlclient.  The command you
 * want looks like</p>
 *
 * <pre>
 *   xmlclient -dumpdata sync=Users > full_users_sync.xml
 * </pre>
 *
 * <p>The effect of this command is to dump all data in the server
 * that matches the filters for the 'Users' Sync Channel to the
 * full_users_sync.xml file.  Note that in order for this to work, you
 * should make sure that your Sync Channel's name does not include any
 * whitespace.  Java's command line parsing logic is fundamentally
 * broken, and makes it impossible to portably process command line
 * parameters with internal whitespace.  Note as well that, as with
 * all xmlclient dump operations, the database is locked against
 * transaction commits while this is running, so this operation can
 * only be done by a supergash-level account.</p>
 *
 * <p>You would use such a full state sync file for those cases where
 * the incremental synchronization system experiences a loss of
 * synchronization between the Ganymede server and the target
 * directory service.  The idea is that your external Service Program
 * should in some way be able to recognize that it is being given a
 * full state dump, and undertake a more lengthy and computationally
 * expensive process of reconciliation to bring the directory service
 * target into compliance with the data in the Ganymede server.</p>
 * 
 * <p>See the Ganymede synchronization guide for more details on all
 * of this.</p>
 */

public class SyncRunner implements Runnable {

  /**
   * <p>TranslationService object for handling string localization in
   * the Ganymede server.</p>
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.server.SyncRunner");

  private static Runtime runtime = null;

  /**
   * XML version major id
   */

  static final byte major_xml_sync_version = 1;

  /**
   * XML version minor id
   */

  static final byte minor_xml_sync_version = 0;

  // ---

  private Invid syncChannelInvid;
  private String name;
  private String directory;
  private String serviceProgram;
  private int transactionNumber;
  private boolean includePlaintextPasswords;
  private Hashtable matrix;

  /* -- */

  public SyncRunner(DBObject syncChannel)
  {
    // on startup, we'll take the last known good transaction number
    // for our baseline
    this.transactionNumber = Ganymede.db.getTransactionNumber();

    updateInfo(syncChannel);
  }

  public synchronized void updateInfo(DBObject syncChannel)
  {
    if (syncChannel.getTypeID() != SchemaConstants.SyncChannelBase)
      {
	// "Error, passed the wrong kind of DBObject."
	throw new IllegalArgumentException(ts.l("updateInfo.typeError"));
      }

    this.syncChannelInvid = syncChannel.getInvid();
    this.name = (String) syncChannel.getFieldValueLocal(SchemaConstants.SyncChannelName);
    this.directory = (String) syncChannel.getFieldValueLocal(SchemaConstants.SyncChannelDirectory);
    this.serviceProgram = (String) syncChannel.getFieldValueLocal(SchemaConstants.SyncChannelServicer);
    this.includePlaintextPasswords = syncChannel.isSet(SchemaConstants.SyncChannelPlaintextOK);

    FieldOptionDBField f = (FieldOptionDBField) syncChannel.getField(SchemaConstants.SyncChannelFields);

    this.matrix = (Hashtable) f.matrix.clone();
  }

  /**
   * <p>Used to set the number of the last transaction known to have been
   * safely persisted to disk (journal and sync channels) at the time this
   * method is called.</p>
   */

  public void setTransactionNumber(int trans)
  {
    if (trans >= this.transactionNumber)
      {
	this.transactionNumber = trans;
      }
    else
      {
	// "Error, can''t set the persisted transaction number to a lower number than previously seen."
	throw new IllegalArgumentException(ts.l("setTransactionNumber.badNumber"));
      }
  }

  /**
   * <p>Returns the number of the last transaction known to have been
   * safely persisted to disk (journal and sync channels) at the time this
   * method is called.</p>
   */

  public int getTransactionNumber()
  {
    return this.transactionNumber;
  }

  public String getName()
  {
    return name;
  }

  public String getDirectory()
  {
    return directory;
  }

  public String getServiceProgram()
  {
    return serviceProgram;
  }

  /**
   * <p>This method writes out the differential transaction record to
   * the sync channel defined by this SyncRunner object.  The
   * transaction record will only include those objects and fields
   * that are specified in the Sync Channel database object that this
   * SyncRunner was initialized with.</p>
   *
   * @param transRecord A transaction description record describing the transaction we are writing
   * @param objectList An array of DBEditObjects that the transaction has checked out at commit time
   * @param transaction The DBEditSet that is being committed.
   */

  public void writeSync(DBJournalTransaction transRecord, DBEditObject[] objectList,
			DBEditSet transaction) throws IOException
  {
    XMLDumpContext xmlOut = null;

    /* -- */

    for (int i = 0; i < objectList.length; i++)
      {
	DBEditObject syncObject = objectList[i];

	if (shouldInclude(syncObject))
	  {
	    if (xmlOut == null)
	      {
		xmlOut = createXMLSync(transRecord);
		xmlOut.setDeltaSyncing(true);
	      }

	    switch (syncObject.getStatus())
	      {
	      case ObjectStatus.CREATING:
		xmlOut.startElementIndent("object_delta");

		xmlOut.indentOut();

		xmlOut.startElementIndent("before");
		xmlOut.endElement("before");

		xmlOut.startElementIndent("after");
		xmlOut.indentOut();
		syncObject.emitXML(xmlOut);
		xmlOut.indentIn();
		xmlOut.endElementIndent("after");
		
		xmlOut.indentIn();
		xmlOut.endElementIndent("object_delta");

		break;

	      case ObjectStatus.DELETING:
		xmlOut.startElementIndent("object_delta");

		xmlOut.indentOut();

		xmlOut.startElementIndent("before");
		xmlOut.indentOut();
		syncObject.getOriginal().emitXML(xmlOut);
		xmlOut.indentIn();
		xmlOut.endElementIndent("before");

		xmlOut.startElementIndent("after");
		xmlOut.endElement("after");
		
		xmlOut.indentIn();
		xmlOut.endElementIndent("object_delta");
		break;

	      case ObjectStatus.EDITING:
		syncObject.emitXMLDelta(xmlOut);
		break;
	      }
	  }
      }

    if (xmlOut != null)
      {
	xmlOut.indentIn();
	xmlOut.endElementIndent("transaction");
	xmlOut.skipLine();
	xmlOut.close();		// close() automatically flushes before closing
      }

    setTransactionNumber(transRecord.getTransactionNumber());
  }

  /**
   * <p>If we go to commit a transaction and we find that we can't
   * write a sync to its sync channel for some reason, we'll need to
   * go back and erase the sync files that we did write out.  This method
   * is responsible for wielding the axe.</p>
   *
   * @param transRecord A transaction description record describing the transaction we are clearing from this sync channel
   */

  public void unSync(DBJournalTransaction transRecord) throws IOException
  {
    File syncFile = new File(this.getDirectory() + File.separator + String.valueOf(transRecord.getTransactionNumber()));

    if (syncFile.exists())
      {
	// "SyncRunner {0} deleting left over transaction fragment {1}"
	Ganymede.debug(ts.l("unSync.deleting", this.getName(), syncFile.getPath()));
	syncFile.delete();
      }

    this.transactionNumber = transRecord.getTransactionNumber() - 1;
  }

  /**
   * <p>This private helper method creates the {@link
   * arlut.csd.ganymede.server.XMLDumpContext} that writeSync() will
   * write to.</p>
   *
   * @param transRecord A transaction description record describing the transaction we are writing
   */

  private XMLDumpContext createXMLSync(DBJournalTransaction transRecord) throws IOException
  {
    FileOutputStream outStream = null;
    BufferedOutputStream bufStream = null;

    outStream = new FileOutputStream(this.getDirectory() + File.separator + String.valueOf(transRecord.getTransactionNumber()));
    bufStream = new BufferedOutputStream(outStream);

    XMLDumpContext xmlOut = new XMLDumpContext(new UTF8XMLWriter(outStream, UTF8XMLWriter.MINIMIZE_EMPTY_ELEMENTS),
					       includePlaintextPasswords, // whether we include plaintext passwords when we have alternate hashes
					       false, // don't include creator/modifier data
					       this);

    xmlOut.startElement("transaction");
    xmlOut.attribute("major_version", Byte.toString(major_xml_sync_version));
    xmlOut.attribute("minor_version", Byte.toString(major_xml_sync_version));
    xmlOut.attribute("channel", getName());

    if (transRecord.getUsername() != null)
      {
	xmlOut.attribute("user", transRecord.getUsername());
      }

    xmlOut.attribute("number", String.valueOf(transRecord.getTransactionNumber()));
    xmlOut.attribute("time", String.valueOf(transRecord.getTime()));
    xmlOut.indentOut();

    return xmlOut;
  }

  /**
   * <p>Returns true if the Sync Channel attached to this SyncRunner
   * is configured to write plain text passwords.</p>
   */

  public boolean includePlaintextPasswords()
  {
    return includePlaintextPasswords;
  }

  /**
   * <p>Returns true if this sync channel is configured to ever
   * include objects of the given baseID.</p>
   */

  public boolean mayInclude(short baseID)
  {
    String x = getOption(baseID);

    return (x != null && x.equals("1"));
  }

  /**
   * <p>Returns true if this sync channel is configured to ever
   * include objects of the given baseID.</p>
   */

  public boolean mayInclude(DBObject object)
  {
    String x = getOption(object.getTypeID());

    return (x != null && x.equals("1"));
  }

  /**
   * <p>Returns true if the DBEditObject passed in needs to be synced
   * to this channel.</p>
   */

  public boolean shouldInclude(DBEditObject object)
  {
    if (!mayInclude(object))
      {
	return false;
      }

    if (object.getStatus() == ObjectStatus.DROPPING)
      {
	return false;
      }

    DBObject origObj = object.getOriginal();

    Vector fieldCopies = object.getFieldVect();

    for (int i = 0; i < fieldCopies.size(); i++)
      {
	DBField memberField = (DBField) fieldCopies.elementAt(i);
	DBField origField;

	if (origObj == null)
	  {
	    origField = null;
	  }
	else
	  {
	    origField = (DBField) origObj.getField(memberField.getID());
	  }

	// created

	if (origField == null && memberField.isDefined())
	  {
	    String fieldOption = getOption(memberField);

	    if (fieldOption != null && !fieldOption.equals("0"))
	      {
		return true;
	      }
	  }

	// deleted

	if (!memberField.isDefined() && origField != null)
	  {
	    String fieldOption = getOption(memberField);

	    if (fieldOption != null && !fieldOption.equals("0"))
	      {
		return true;
	      }
	  }

	// changed

	if (memberField.isDefined() && origField != null)
	  {
	    // check to see if the field has changed and whether we
	    // should include it.  The 'hasChanged()' check is
	    // required because this shouldInclude() call will always
	    // return true if memberField is defined as an 'always
	    // include' field, whereas in this object-level
	    // shouldInclude() check loop, we are looking to see
	    // whether a field that we care about was changed.

	    // Basically the hasChanged() call checks to see if the
	    // field changed, and the shouldInclude() call checks to
	    // see if it's a field that we care about.

	    if (memberField.hasChanged(origField) && shouldInclude(memberField, origField))
	      {
		return true;
	      }
	  }
      }

    return false;
  }

  /**
   * <p>Returns true if the given field needs to be sent to this sync
   * channel.  This method is responsible for doing the determination
   * only if both field and origField are not null and isDefined().</p>
   */

  public boolean shouldInclude(DBField newField, DBField origField)
  {
    String fieldOption;

    /* -- */

    if (!newField.isDefined())
      {
	// "newField is undefined"
	throw new IllegalArgumentException(ts.l("shouldInclude.badNewField"));
      }

    if (origField == null)
      {
	// "origField is null"
	throw new NullPointerException(ts.l("shouldInclude.badOrigField"));
      }

    switch (newField.getID())
      {
      case SchemaConstants.CreationDateField:
      case SchemaConstants.CreatorField:
      case SchemaConstants.ModificationDateField:
      case SchemaConstants.ModifierField:
	return false;
      }

    fieldOption = getOption(newField);

    if (fieldOption == null || fieldOption.equals("0"))
      {
	return false;
      }
    else if (fieldOption.equals("2"))
      {
	return true;
      }
    else if (fieldOption.equals("1"))
      {
	if (newField.hasChanged(origField))
	  {
	    return true;
	  }
      }

    return false;
  }

  /**
   * <p>Returns true if the given type of field may be included in
   * this sync channel.  The hasChanged parameter should be set to
   * true if the field being tested was changed in the current
   * transaction, or false if it remains unchanged.</p>
   *
   * <p>This differs from shouldInclude on DBField in that this method
   * leaves it to the caller to decide whether the field has changed.</p>
   */

  public boolean mayInclude(DBField field, boolean hasChanged)
  {
    return this.mayInclude(field.getOwner().getTypeID(), field.getID(), hasChanged);
  }

  /**
   * <p>Returns true if the given type of field may be included in
   * this sync channel.  The hasChanged parameter should be set to
   * true if the field being tested was changed in the current
   * transaction, or false if it remains unchanged.</p>
   *
   * <p>This differs from shouldInclude on DBField in that this method
   * leaves it to the caller to decide whether the field has changed.</p>
   */

  public boolean mayInclude(short baseID, short fieldID, boolean hasChanged)
  {
    String x = getOption(baseID, fieldID);

    if (hasChanged)
      {
	return (x != null && (x.equals("1") || x.equals("2")));
      }
    else
      {
	return (x != null && x.equals("2"));
      }
  }

  /**
   * <p>Returns the option string, if any, for the given base and field.
   * This option string should be one of three values:</p>
   *
   * <p>"0", meaning the field is never included in this sync channel, nor
   * should it be examined to make a decision about whether a given
   * object is written to this sync channel.</p>
   *
   * <p>"1", meaning that the field is included in this sync channel if
   * it has changed, and that the object that includes the field
   * should be written to the sync channel if this field was changed
   * in the object.  If a field has an option string of "1" but has
   * not changed in a given transaction, that field won't trigger the
   * object to be written to the sync channel.</p>
   *
   * <p>"2", meaning that the field is always included in this sync
   * channel if the object that it is contained in is sent to this
   * sync channel, even if it wasn't changed in the transaction.  If
   * this field was changed in a given transaction, that will suffice
   * to cause an object that is changed in any fashion during a
   * transaction to be sent to this sync channel.  In this sense, it
   * is like "1", but with the added feature that it will "ride along"
   * with its object to the sync channel, even if it wasn't changed
   * during the transaction.</p>
   *
   * <p>If "1" or "2" is true for a field in a given object type, the
   * corresponding option string for the object's type should be 1,
   * signaling that at least some fields in the object should be sent
   * to this sync channel when the object is involved in a
   * transaction.</p>
   */

  private String getOption(DBField field)
  {
    return (String) matrix.get(FieldOptionDBField.matrixEntry(field.getOwner().getTypeID(), field.getID()));
  }

  /**
   * <p>Returns the option string, if any, for the given base and field.
   * This option string should be one of three values:</p>
   *
   * <p>"0", meaning the field is never included in this sync channel, nor
   * should it be examined to make a decision about whether a given
   * object is written to this sync channel.</p>
   *
   * <p>"1", meaning that the field is included in this sync channel if
   * it has changed, and that the object that includes the field
   * should be written to the sync channel if this field was changed
   * in the object.  If a field has an option string of "1" but has
   * not changed in a given transaction, that field won't trigger the
   * object to be written to the sync channel.</p>
   *
   * <p>"2", meaning that the field is always included in this sync
   * channel if the object that it is contained in is sent to this
   * sync channel, even if it wasn't changed in the transaction.  If
   * this field was changed in a given transaction, that will suffice
   * to cause an object that is changed in any fashion during a
   * transaction to be sent to this sync channel.  In this sense, it
   * is like "1", but with the added feature that it will "ride along"
   * with its object to the sync channel, even if it wasn't changed
   * during the transaction.</p>
   *
   * <p>If "1" or "2" is true for a field in a given object type, the
   * corresponding option string for the object's type should be 1,
   * signaling that at least some fields in the object should be sent
   * to this sync channel when the object is involved in a
   * transaction.</p>
   */

  private String getOption(short baseID, short fieldID)
  {
    return (String) matrix.get(FieldOptionDBField.matrixEntry(baseID, fieldID));
  }

  /**
   * <p>Returns the option string, if any, for the given base.  This option
   * string should be one of two values:</p>
   *
   * <p>"0", meaning that this object type will never be sent to this
   * sync channel.</p>
   *
   * <p>"1", meaning that this object type may be sent to this sync
   * channel if any field contained within it which has an option
   * string of "1" or "2" was changed during the transaction.
   */

  private String getOption(short baseID)
  {
    return (String) matrix.get(FieldOptionDBField.matrixEntry(baseID));
  }

  public void run()
  {
    int myTransactionNumber;
    String myName, myServiceProgram, invocation;
    String shutdownState = null;
    File file;

    /* -- */

    synchronized (this)
      {
	myName = getName();
	myTransactionNumber = getTransactionNumber();
	myServiceProgram = getServiceProgram();
	invocation = myServiceProgram + " " + String.valueOf(myTransactionNumber);
      }

    // increment the shutdownSemaphore so that the system knows we are
    // in the middle of running a critical task

    try
      {
	shutdownState = GanymedeServer.shutdownSemaphore.increment(0);
	GanymedeBuilderTask.incPhase2(true); // so that the client sees the phase 2 icon rolling

	if (shutdownState != null)
	  {
	    // "Refusing to run Sync Channel {0}, due to shutdown condition: {1}"
	    Ganymede.debug(ts.l("run.shutting_down", this.getName(), shutdownState));
	    return;
	  }
      }
    catch (InterruptedException ex)
      {
	// will never happen, since we are giving 0 to
	// increment
      }

    try
      {
	// "SyncRunner {0} running"
	Ganymede.debug(ts.l("run.running", myName));

	if (getServiceProgram() != null)
	  {
	    file = new File(getServiceProgram());
	    
	    if (file.exists())
	      {
		if (runtime == null)
		  {
		    runtime = Runtime.getRuntime();
		  }
		
		try
		  {
		    FileOps.runProcess(invocation);
		  }
		catch (IOException ex)
		  {
		    // "Couldn''t exec SyncRunner {0}''s service program "{1}" due to IOException: {2}"
		    Ganymede.debug(ts.l("run.ioException", myName, myServiceProgram, ex));
		  }
		catch (InterruptedException ex)
		  {
		    // "Failure during exec of SyncRunner {0}''s service program "{1}""
		    Ganymede.debug(ts.l("run.interrupted", myName, myServiceProgram));
		  }
	      }
	    else
	      {
		// ""{0}" doesn''t exist, not running external service program for SyncRunner {1}"
		Ganymede.debug(ts.l("run.nonesuch", myServiceProgram, myName));
	      }
	  }
	else
	  {
	    // "No external service program defined for SyncRunner {0}, not servicing {0}!"
	    Ganymede.debug(ts.l("run.undefined", myName));
	  }
      }
    finally
      {
	GanymedeServer.shutdownSemaphore.decrement();
	GanymedeBuilderTask.decPhase2(true); // so that the client no longer sees the phase 2 icon rolling
      }

    // "SyncRunner {0} finished"
    Ganymede.debug(ts.l("run.done", myName));
  }

  public String toString()
  {
    return this.getName();
  }
}
