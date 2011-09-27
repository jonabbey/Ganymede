/*

   DBJournal.java

   Class to handle the journal file for the DBStore.
   
   Created: 3 December 1996

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996-2011
   The University of Texas at Austin

   Contact information

   Web site: http://www.arlut.utexas.edu/gash2
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

package arlut.csd.ganymede.server;

import java.io.DataOutput;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Date;
import java.util.Vector;

import arlut.csd.Util.StringUtils;
import arlut.csd.Util.TranslationService;
import arlut.csd.ganymede.common.ObjectStatus;
import arlut.csd.ganymede.rmi.db_field;

/*------------------------------------------------------------------------------
                                                                           class
                                                                       DBJournal

------------------------------------------------------------------------------*/

/**
 * <p>The DBJournal class is used to provide journalling of changes to the
 * {@link arlut.csd.ganymede.server.DBStore DBStore}
 * during operations.  The Journal file will contain a complete list of all
 * changes made since the last dump of the complete DBStore.  The Journal file
 * is composed of a header block followed by a number of transactions.</p>
 *
 * <p>Each transaction consists of a number of object modification records, each
 * record specifying the creation, deletion, or modification of a particular
 * object.  At the end of the transaction, a marker indicates the completion of
 * the transaction.  At DBStore startup time, the journal is read in and all
 * complete transactions recorded are performed on the main DBStore.</p>
 *
 * <p>Generally, if the DBStore was shut down correctly, the entire memory
 * structure of the DBStore will be cleanly dumped out and the Journal will
 * be removed.  The Journal is intended to insure that the DBStore remains
 * transaction consistent if the server running Ganymede crashes during
 * runtime.</p>
 *
 * <p>See the {@link arlut.csd.ganymede.server.DBEditSet DBEditSet} class for
 * more information on Ganymede transactions.</p>
 *
 * <p>Nota bene: this class includes synchronized methods which serialize
 * operations on the Ganymede transaction journal.  The DBJournal
 * monitor is intended to be the innermost monitor for operations
 * involving the DBStore and DBJournal objects.  Synchronized methods
 * in DBJournal must not call synchronized methods on DBStore, as
 * synchronized DBStore methods can and will call methods on
 * DBJournal.</p>
 */

public class DBJournal implements ObjectStatus {

  static boolean debug = false;
  static DBStore store = null;

  public static void setDebug(boolean val)
  {
    debug = val;
  }

  /**
   * <p>TranslationService object for handling string localization in
   * the Ganymede server.</p>
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.server.DBJournal");

  static final String id_string = "GJournal";
  static final short major_version = 2;
  static final short minor_version = 3;

  static final String OPENTRANS = "open";
  static final String CLOSETRANS = "close";
  static final String FINALIZE = "finalize";

  static final byte CREATE = 1;
  static final byte EDIT = 2;
  static final byte DELETE = 3;

  // instance variables

  String filename;
  RandomAccessFile jFile = null;
  boolean dirty = false;	// dirty is true if the journal has any
				// transactions written out

  short file_major_version = -1;
  short file_minor_version = -1;

  short file_dbstore_major_version = -1;
  short file_dbstore_minor_version = -1;

  int transactionsInJournal = 0;

  private DBJournalTransaction incompleteTransaction = null;

  /* -- */

  static void initialize(DataOutput out) throws IOException
  {
    out.writeUTF(DBJournal.id_string);
    out.writeShort(DBJournal.major_version);
    out.writeShort(DBJournal.minor_version);
    out.writeShort(store.major_version);
    out.writeShort(store.minor_version);
    out.writeLong(System.currentTimeMillis());
  }

  /* -- */

  /**
   * <p>The DBJournal constructor takes a filename and creates a DBJournal object.
   * If the file named does not exist, the DBJournal constructor will create
   * the file and write the DBJournal header, leaving the file pointer pointing
   * to the end of the file.  If the Journal file does exist, the constructor
   * will advance the file pointer to the end of the file.</p>
   *
   * <p>In either case, the file will be made ready for new transactions to be
   * written.</p>
   */
  
  public DBJournal(DBStore store, String filename) throws IOException
  {
    this.store = store;
    this.filename = filename;

    if (this.store == null)
      {
	// "bad parameter, store == null"
	throw new IllegalArgumentException(ts.l("init.badstore"));
      }

    if (this.filename == null)
      {
	// "bad parameter, filename == null"
	throw new IllegalArgumentException(ts.l("init.badfile"));
      }

    // "Initializing DBStore Journal: {0}"
    debug(ts.l("init.initing", filename));

    File file = new File(filename);

    if (!file.exists())
      {
	// create an empty Journal file	
	// "Creating Journal File"
	debug(ts.l("init.creating"));

	jFile = new RandomAccessFile(filename, "rw");

	// "Writing DBStore Journal header"
	debug(ts.l("init.writing"));

	initialize(jFile);

	dirty = false;
      }
    else
      {
	// open an existing Journal file and prepare to
	// write to the end of it
	// "Opening Journal File for Append"
	debug(ts.l("init.opening"));

	jFile = new RandomAccessFile(filename, "rw");

	// look to see if there are any transactions in the journal
	readHeaders();

	try
	  {
	    if (jFile.readUTF().compareTo(OPENTRANS) != 0)
	      {
		// "DBJournal constructor: open string mismatch"
		throw new IOException(ts.l("init.badheader"));
	      }
	    else
	      {
		dirty = true;	// we have some transactions in the existing journal
	      }
	  }
	catch (EOFException ex)
	  {
	    dirty = false;	// we have no transactions in the existing journal
	  }

	// from now on, write to append to the end of the file

	jFile.seek(jFile.length());
      }
  }

  /**
   * <p>This method returns true if the disk file being loaded by this DBStore
   * has a version number greater than major.minor.</p>
   */

  public boolean isAtLeast(int major, int minor)
  {
    if (this.file_major_version < 0)
      {
	// "DBJournal.isAtLeast() called before journal loaded."
	throw new RuntimeException(ts.l("isAtLeast.notloaded"));
      }

    return (this.file_major_version > major || 
	    (this.file_major_version == major && this.file_minor_version >= minor));
  }


  /**
   * <p>This method returns true if the disk file being loaded by this DBStore
   * has a version number earlier than major.minor.</p>
   */

  public boolean isLessThan(int major, int minor)
  {
    if (this.file_major_version < 0)
      {
	// "DBJournal.isLessThan() called before journal loaded."
	throw new RuntimeException(ts.l("isLessThan.notloaded"));
      }

    return (this.file_major_version < major || 
	    (this.file_major_version == major && this.file_minor_version < minor));
  }

  /**
   * <p>This method returns true if the disk file being loaded by this DBStore
   * has a version number equal to major.minor.</p>
   */

  public boolean isAtRev(int major, int minor)
  {
    if (this.file_major_version < 0)
      {
	// "DBJournal.isAtRev() called before journal loaded."
	throw new RuntimeException(ts.l("isAtRev.notloaded"));
      }

    return (this.file_major_version == major && this.file_minor_version == minor);
  }

  /**
   * <p>This method returns true if the disk file being loaded by this
   * DBStore has a version number between greater than or equal to
   * major1.minor1 and less than major2.minor2.</p>
   */

  public boolean isBetweenRevs(int major1, int minor1, int major2, int minor2)
  {
    if (this.file_major_version < 0)
      {
	// "DBJournal.isBetweenRevs() called before journal loaded."
	throw new RuntimeException(ts.l("isBetweenRevs.notloaded"));
      }

    if (this.file_major_version == major1 && this.file_minor_version >= minor1)
      {
	return true;
      }

    if (this.file_major_version == major2 && this.file_minor_version < minor2)
      {
	return true;
      }

    if (this.file_major_version > major1 && this.file_major_version < major2)
      {
	return true;
      }

    return false;
  }

  /**
   * The reset() method is used to copy the journal file to a safe location and
   * truncate it.  reset() should be called immediately after the DBStore is
   * dumped to disk and before the DumpLock is relinquished.
   */

  public synchronized void reset() throws IOException
  {
    String newname;

    /* - */

    // "DBJournal: Resetting Journal File"
    debug(ts.l("reset.resetting"));

    if (jFile != null)
      {
	jFile.close();
      }

    File file = new File(filename);

    newname = filename + ".old";

    // "DBJournal: saving old Journal as {0}"
    debug(ts.l("reset.savingold", newname));

    if (!file.renameTo(new File(newname)))
      {
	throw new IOException("Couldn't rename " + file.getPath() + " to " + newname);
      }

    // "DBJournal: creating fresh Journal {0}"
    debug(ts.l("reset.freshness", filename));

    jFile = new RandomAccessFile(filename, "rw");
    initialize(jFile);
    jFile.getFD().sync();

    dirty = false;

    transactionsInJournal = 0;
    GanymedeAdmin.updateTransCount();

    if (Ganymede.log != null)
      {
	Ganymede.log.logSystemEvent(new DBLogEvent("journalreset",
						   ts.l("reset.logstring"), // "Ganymede Journal Reset"
						   null,
						   null,
						   null,
						   null));
      }
  }

  /**
   * <p>The load() method reads in all transactions in the current
   * DBStore Journal and makes the appropriate changes to the DBStore
   * Object Bases.  This method should be called after the main body
   * of the DBStore is loaded and before the DBStore is put into
   * production mode.</p>
   *
   * <p>load() will return true if the on-disk journal was in a
   * consistent state, with no incomplete transactions.  If load()
   * encounters EOF in the middle of attempting to read in a
   * transaction record, load() will return false.  In either case,
   * any valid and complete transaction records will be processed and
   * integrated into the DBStore.</p>
   */

  public synchronized boolean load() throws IOException
  {
    long transaction_time = 0;
    Date transactionDate = null;
    int object_count = 0;
    boolean EOFok = true;
    Vector entries = null;
    byte operation;
    short obj_type, obj_id;
    DBObjectBase base;
    DBObject obj;
    String status = null;
    int nextTransactionNumber = 0;
    /* - */

    entries = new Vector();

    // skip past the journal header block
    readHeaders();

    // start reading and applying the changes
    try
      {
	while (true)
	  {
	    if (jFile.readUTF().compareTo(OPENTRANS) != 0)
	      {
		// "DBJournal.load(): Transaction open string mismatch"
		debug(ts.l("load.openmismatch"));
		throw new IOException(ts.l("load.openmismatch"));
	      }
	    else
	      {
		// "DBJournal.load(): Transaction open string match OK"
		debug(ts.l("load.okmatch"));
	      }
	    
	    EOFok = false;

	    // "Reading transaction time"
	    status = ts.l("load.readingtime");
	    transaction_time = jFile.readLong();
	    transactionDate = new Date(transaction_time);

	    debug(ts.l("load.showtime", transactionDate));

	    if (isAtLeast(2,1))
	      {
		nextTransactionNumber = jFile.readInt();
		Ganymede.db.updateTransactionNumber(nextTransactionNumber);
		debug("nextTransactionNumber:"+nextTransactionNumber);
	      }

	    // "Reading object count"
	    status = ts.l("load.readingobjcount");

	    // read object information
	    object_count = jFile.readInt();

	    debug("Objects In Transaction: " + object_count);

	    if (object_count > 0)
	      {
		dirty = true;
	      }

	    for (int i = 0; i < object_count; i++)
	      {
		Integer iObj = Integer.valueOf(i);

		// "Reading operation code for object {0}"
		status = ts.l("load.readingopcode", iObj);
		operation = jFile.readByte();

		// "Reading object type for object {0}"
		status = ts.l("load.readingtype", iObj);
		obj_type = jFile.readShort();
		base = (DBObjectBase) store.objectBases.get(Short.valueOf(obj_type));

		switch (operation)
		  {
		  case CREATE:		
		    // "Reading created object {0}"
		    status = ts.l("load.readingcreated", iObj);

		    obj = new DBObject(base, jFile, true);

		    if (debug)
		      {
			// "Create: {0}"
			debug(ts.l("load.create", obj.getInvid()));
			printObject(obj);
		      }

		    entries.addElement(new JournalEntry(base, obj.getID(), obj));
		    break;

		  case EDIT:
		    // "Reading edited object {0}"
		    status = ts.l("load.readingedited", iObj);

		    DBObjectDeltaRec delta = new DBObjectDeltaRec(jFile);
		    DBObject original = DBStore.viewDBObject(delta.invid);

		    obj = delta.applyDelta(original);

		    // we have to do the delta.toString() after we apply the delta so that
		    // the scalarValue fields get parented

		    if (debug)
		      {
			// "Delta read:\n\t{0}\n"
			debug("\n"+ts.l("load.deltaread", StringUtils.replaceStr(delta.toString(),"\n","\n\t")));

			// "DBJournal.load(): original object, before delta edit:"
			debug(ts.l("load.original"));
			printObject(original);

			// "DBJournal.load(): object after delta edit:"
			debug(ts.l("load.postdelta"));
			printObject(obj);
		      }

		    entries.addElement(new JournalEntry(base, obj.getID(), obj));
		    break;

		  case DELETE:
		    // "Reading deleted object {0}"
		    status = ts.l("load.readingdeleted", iObj);

		    obj_id = jFile.readShort();

		    // "Delete: {0}:{1}"
		    debug(ts.l("load.delete", base.getName(), Short.valueOf(obj_id)));
		
		    entries.addElement(new JournalEntry(base, obj_id, null));
		    break;
		  }
	      }

	    // "Reading close transaction information"
	    status = ts.l("load.readingclosed");


	    String closeDebug = jFile.readUTF();
	    Long transTimeDebug = jFile.readLong();
	    debug("closeDebug and transTimeDebug:"+ closeDebug +"*"+ transTimeDebug.toString()+"* transaction_time:"+transaction_time);

	    //if ((jFile.readUTF().compareTo(CLOSETRANS) != 0) || 
	    //	(jFile.readLong() != transaction_time))
	    if ((closeDebug.compareTo(CLOSETRANS) != 0) || 
		(transTimeDebug != transaction_time))
	      {
		// "Transaction close timestamp mismatch"
		throw new IOException(ts.l("load.badclosed"));
	      }

	    // we've read in all of the transaction's changes.. now we
	    // need to see whether all active sync channels were
	    // successfully written to.  if we don't see the complete
	    // finalize token stream, we'll need to undo the
	    // transaction after all, by clearing the offending
	    // transaction from whatever of the sync channels it was
	    // written to

	    if (isAtLeast(2,2))
	      {
		boolean success = false;

		try
		  {
		    success = ((jFile.readUTF().compareTo(FINALIZE) == 0) &&
			       (jFile.readLong() == transaction_time) &&
			       (jFile.readInt() == nextTransactionNumber));

		    if (!success)
		      {
			// "DBJournal: transaction {0} not finalized in journal, rejecting"
			throw new IOException(ts.l("load.notfinalized", Integer.valueOf(nextTransactionNumber)));
		      }
		  }
		catch (IOException ex)
		  {
		    // we didn't get the transaction finalized, but it
		    // had been persisted, which means that some of
		    // the sync channels may possibly have had some
		    // transactions written out.  we should have
		    // cleaned up after any incomplete writes to the
		    // sync channels, but if the server was abnormally
		    // terminated at the wrong time, we may have some
		    // transactions lingering.  we'll record enough
		    // information in the incompleteTransaction member
		    // variable so that Ganymede.java can clean them
		    // up

		    this.incompleteTransaction = new DBJournalTransaction(transaction_time, -1, nextTransactionNumber, null);

		    // then we'll throw this exception back up

		    throw ex;
		  }
	      }

	    // "Finished transaction"
	    status = ts.l("load.finished");

	    // "Transaction {0} successfully read from Journal.\nIntegrating transaction into DBStore memory image."
	    debug(ts.l("load.success", transactionDate));

	    // "Processing {0} objects"
	    debug(ts.l("load.processing", Integer.valueOf(entries.size())));

	    // okay, process this transaction
	    for (int i = 0; i < entries.size(); i++)
	      {
		((JournalEntry) entries.elementAt(i)).process(store);
	      }

	    // clear the entries we've now processed
	    entries.setSize(0);

	    EOFok = true;
	  }
      }
    catch (EOFException ex)
      {
	if (EOFok)
	  {
	    // "All transactions processed successfully"
	    debug(ts.l("load.allclear"));
	    return true;
	  }
	else
	  {
	    // "DBJournal file unexpectedly ended: state = {0}"
	    debug(ts.l("load.failure", status));

	    // ok, the journal ended badly, but aside from losing a partial
	    // transaction, we should be ok.
	    return false;
	  }
      }
  }

  /**
   * <p>The writeTransaction() method performs the work of persisting a
   * transaction to the DBStore Journal.  writeTransaction() should be
   * called before the changes are actually finalized in the DBStore
   * Object Bases.  If writeTransaction() is not able to successfully
   * write the complete transaction log to the Journal file, an
   * IOException will be thrown.</p>
   *
   * <p>Note that writeTransaction() does not mark the transaction in
   * the on-disk journal file as finalized.  If the server is
   * abnormally terminated (or if the disk fills) between the time that
   * writeTransaction() is called and the time that
   * finalizeTransaction() is called, the server on startup will not consider
   * the transaction to have been finally put to rest, and the system will attempt
   * to push the unfinalized change in the journal to the sync channels.</p>
   *
   * @return On success, a {@link
   * arlut.csd.ganymede.server.DBJournalTransaction} recording the
   * transaction's time stamp and integral transaction number is
   * returned.  This DBJournalTransaction can be passed back to
   * finalizeTransaction() when writes to the sync channels are
   * completed.
   */

  public synchronized DBJournalTransaction writeTransaction(DBEditSet transaction) throws IOException
  {
    DBEditObject eObj;
    DBJournalTransaction transRecord;
    Date now;
    DBEditObject[] objects = transaction.getObjectList();

    /* - */

    now = new Date();
    transRecord = new DBJournalTransaction(now.getTime(),
					   jFile.getFilePointer(),
					   Ganymede.db.getNextTransactionNumber(),
					   transaction.getUsername());

    // Writing transaction to the Journal : {0}
    debug(ts.l("writeTransaction.writing", now));

    try
      {
	jFile.writeUTF(OPENTRANS);
	jFile.writeLong(transRecord.getTime());
	jFile.writeInt(transRecord.getTransactionNumber());

	// "Objects in Transaction: {0}"
	debug(ts.l("writeTransaction.objcount", Integer.valueOf(objects.length)));
	
	jFile.writeInt(objects.length);
	
	for (int i = 0; i < objects.length; i++)
	  {
	    eObj = objects[i];
	    
	    switch (eObj.getStatus())
	      {
	      case CREATING:
		jFile.writeByte(CREATE);
		jFile.writeShort(eObj.objectBase.getTypeID());
		eObj.emit(jFile);
		
		if (debug)
		  {
		    // "Creating object:"
		    System.err.println(ts.l("writeTransaction.creating"));
		    printObject(eObj);
		  }

		break;
		
	      case EDITING:
		jFile.writeByte(EDIT);
		jFile.writeShort(eObj.objectBase.getTypeID());
		
		DBObjectDeltaRec delta = new DBObjectDeltaRec(eObj.original, eObj);
		delta.emit(jFile);
		
		if (debug)
		  {
		    // "Wrote object edit record:\n\t{0}"
		    System.err.print(ts.l("writeTransaction.wroteobjedit",
					  StringUtils.replaceStr(delta.toString(),"\n","\n\t")));		    
		  }
		
		break;
		
	      case DELETING:
		jFile.writeByte(DELETE);
		jFile.writeShort(eObj.objectBase.getTypeID());
		jFile.writeShort(eObj.getID());
		
		// "Wrote object deletion record:\n\t{0} : {1}"
		debug(ts.l("writeTransaction.wroteobjdel", eObj.objectBase.getName(),Integer.valueOf(eObj.getID())));
		break;
		
	      case DROPPING:
		if (debug)
		  {
		    // "Dropping object:"
		    System.err.println(ts.l("writeTransaction.dropping"));		    
		    printObject(eObj);
		  }

		break;
	      }
	  }
	
	dirty = true;
	
	// write out the end of transaction stamp.. the transaction_time
	// is used to verify that we completed this write okay.
	jFile.writeUTF(CLOSETRANS);
	jFile.writeLong(transRecord.getTime());
	
	// "Transaction {0} persisted to Journal."
	debug(ts.l("writeTransaction.written", now));
      }
    catch (IOException ex)
      {
	// oops, did we run out of disk space?  roll back what we've
	// written
	try
	  {
	    undoTransaction(transRecord);
	  }
	catch (IOException inex)
	  {
	    // ***
	    // *** Error in commit_finalizeTransaction()!  Couldn''t undo a transaction in the
	    // *** journal file after catching an exception!
	    // ***
	    // *** The journal may not be completely recoverable!
	    // ***
	    //
	    // {0}
	    Ganymede.debug(ts.l("writeTransaction.badundo", inex.toString()));
	  }
      }
    
    return transRecord;
  }

  /**
   * The finalizeTransaction() method actually performs the full work of
   * writing out a transaction to the DBStore Journal.
   * writeTransaction() should be called before the changes are
   * actually finalized in the DBStore Object Bases.  If
   * writeTransaction() is not able to successfully write the complete
   * transaction log to the Journal file, an IOException will be
   * thrown.  The Journal entries are structured so that if a Journal
   * entry can't be completed, the transaction's whole entry will
   * be ignored at load time.
   */

  public synchronized void finalizeTransaction(DBJournalTransaction transRecord) throws IOException
  {
    jFile.writeUTF(FINALIZE);
    jFile.writeLong(transRecord.getTime());
    jFile.writeInt(transRecord.getTransactionNumber());

    // and push the blocks to disk

    jFile.getFD().sync();

    transactionsInJournal++;
    GanymedeAdmin.updateTransCount();
  }

  /**
   * <p>The undoTransaction() method is responsible for rolling back the
   * most recent transaction written to the journal, in the event that
   * writing to the sync channels or finalizing the transaction fails
   * (due to a lack of disk space, presumably).</p>
   */

  public synchronized void undoTransaction(DBJournalTransaction transRecord) throws IOException
  {
    jFile.setLength(transRecord.getOffset()-1);
    jFile.seek(transRecord.getOffset());

    // try to make sure the disk operations we just did are committed
    // to disk

    jFile.getFD().sync();

    Ganymede.db.undoNextTransactionNumber(transRecord.getTransactionNumber());
  }

  /**
   * <p>If the journal contained a persisted but not finalized
   * transaction when we tried to load it, this method will return a
   * DBJournalTransaction containing enough information that the
   * Ganymede main() method can iterate over the defined sync channels
   * and clear out any lingering xml sync for that transaction.</p>
   *
   * <p>This can only happen if the server is abnormally terminated in
   * a very narrow window, but in the one case in 10,000 where an
   * abnormal shutdown hit right in that window, we'll go ahead and
   * worry about it.</p>
   */

  public DBJournalTransaction getIncompleteTransaction()
  {
    return this.incompleteTransaction;
  }

  /**
   * <p>After the Ganymede main() method has tried to clean out any
   * remaining bits of the non-finalized transaction, it will need
   * to clear the incompleteTransaction record.  This method does
   * that. </p>
   */
  
  public void clearIncompleteTransaction()
  {
    this.incompleteTransaction = null;
  }

  /**
   * Returns true if the journal does not contain any transactions.
   */

  public boolean isClean()
  {
    return !dirty;
  }

  /**
   * <p>This method is used to read and check the first few fields of the journal
   * as a sanity check on journal open/load.</p>
   *
   * <p>This method <b>MUST NOT</b> be called after the journal is open and active,
   * or else the journal will become corrupted.</p>
   */

  void readHeaders() throws IOException
  {
    // "DBJournal: Loading transactions from {0}"
    debug(ts.l("readHeaders.loading", filename));

    jFile.seek(0);
  
    if (DBJournal.id_string.compareTo(jFile.readUTF()) != 0)
      {
	// "Error, id_string mismatch.. wrong file type?"
	throw new RuntimeException(ts.l("readHeaders.badid"));
      }

    file_major_version = jFile.readShort();
    file_minor_version = jFile.readShort();

    debug("Journal file major minor version:"+file_major_version+","+file_minor_version);

    // At version 2,3: Fail if DBStore version greater than equal 2/22
    // and DBJournal <= 2/2, because we changed the structure of the
    // DBJournal file after this.

    if (store.isAtLeast(2,23) && isLessThan(2,3))
      {
	// "Error, journal version mismatch.. wrong file type?"
	throw new RuntimeException("Mismatch Version between DBStore and DBJournal. ");
      }

    if ((file_major_version > DBJournal.major_version) ||
	(file_major_version == DBJournal.major_version && file_minor_version > minor_version))
      {
	// "Error, journal version mismatch.. wrong file type?"
	throw new RuntimeException(ts.l("readHeaders.badversion"));
      }

    // At journal version 2,3 we save the dbversion in the headers also.

    if (isAtLeast(2,3))
      {
	file_dbstore_major_version = jFile.readShort();
	file_dbstore_minor_version = jFile.readShort();

	if (file_dbstore_major_version != store.file_major || file_dbstore_minor_version != store.file_minor)
	  {
	    throw new RuntimeException("DBStore and Journal file versions do not match. ");
	  }	
      }

    if (debug)
      {
	// "DBJournal file created {0}"
	debug(ts.l("readHeaders.created", new Date(jFile.readLong())));
      }
    else
      {
	// JAMES QUESTION - why is this only not in debug mode.... odd??!?!!
	jFile.readLong();		// date is there for others to look at
      }
  }

  private void printObject(DBObject obj)
  {
    String objectStr = obj.getPrintString();    
    objectStr = StringUtils.replaceStr(objectStr, "\n", "\n\t");    
    System.err.println("\t" + objectStr);
  }

  /**
   * This is a convenience method used by server-side code to send
   * debug output to stderr and to any attached admin consoles.
   */

  static public void debug(String string)
  {
    if (debug)
      {
	System.err.println(string);
      }
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                                    JournalEntry

------------------------------------------------------------------------------*/

/**
 * <P>This class holds data corresponding to a modification record for
 * a single object in the server's {@link
 * arlut.csd.ganymede.server.DBJournal DBJournal} class.</P>
 */

class JournalEntry {

  static boolean debug = false;

  // ---

  DBObjectBase base;
  int id;
  DBObject obj;			// if null, we'll delete

  /* -- */
  
  public JournalEntry(DBObjectBase base, int id, DBObject obj)
  {
    this.base = base;
    this.id = id;
    this.obj = obj;
  }

  void process(DBStore store)
  {
    DBObjectBaseField definition;
    DBObject oldObject;

    /* -- */

    debug("JournalEntry.process():"+
	  "\t" + StringUtils.replaceStr(this.toString(), "\n", "\n\t"));

    if (obj == null)
      {
	// delete the object
	oldObject = base.getObject(id);

	if (oldObject == null)
	  {
	    Ganymede.debug("Warning.. journal is instructing us to delete an object not in the database");
	  }
	else
	  {
	    // Remove the object.  If any of the fields are namespace
	    // restricted, see if any of the values currently held in
	    // such a field are registered in a namespace.  If so, and
	    // if the field we're deleting is the one currently taking
	    // that value's slot in the namespace, remove the value
	    // from the namespace hash.

	    // NOTE: we don't want to do a full-up object delete here
	    // because that would just get put into a new transaction.
	    // All invid linking issues are taken care of when this
	    // transaction was written to the journal.. the invid
	    // unbinding process would automatically include the other
	    // objects in their post-commit state, so we don't have
	    // to worry about it here.

	    // We do need to unregister the former asymmetric links
	    // from the DBStore aSymLinkTracker, however.

	    oldObject.unregisterAsymmetricLinks();

	    // and we need to clear out any namespace pointers

	    db_field[] tempFields = oldObject.listFields();

	    for (int i = 0; i < tempFields.length; i++)
	      {
		DBField _field = (DBField) tempFields[i];

		definition = _field.getFieldDef();

		if (definition.getNameSpace() != null)
		  {
		    if (_field.isVector())
		      {
			for (int j = 0; j < _field.size(); j++)
			  {
			    if (definition.getNameSpace().lookupPersistent(_field.key(j)) != _field)
			      {
				throw new RuntimeException("Error, namespace mismatch in DBJournal code [" + j + "]");
			      }

			    definition.getNameSpace().removeHandle(_field.key(j));
			  }
		      }
		    else
		      {
			if (definition.getNameSpace().lookupPersistent(_field.key()) != _field)
			  {
			    throw new RuntimeException("Error, namespace mismatch in DBJournal code");
			  }

			definition.getNameSpace().removeHandle(_field.key());
		      }
		  }
	      }
	  }

	base.remove(id);
      }
    else
      {
	// put the new object in place

	// First we need to clear any back links from the old version
	// of the object, if there was any such.

	oldObject = base.getObject(id);

	if (oldObject != null)
	  {
	    oldObject.unregisterAsymmetricLinks();
	  }

	// Second, we need to go through and put these values in the namespace.. note that
	// we don't bother checking to see if the values are already allocated in the
	// namespace.. we are assuming that the transaction would not have been written
	// out to disk with improper namespace management.  We pretty much have to make
	// this assumption here unless we're going to go to the trouble of doing multiple
	// passes through the set of changes in this transaction, first unmarking any
	// values freed by object deletion or changes, then going through and allocating
	// new values.  We may still wind up doing this. 

	db_field[] tempFields = obj.listFields();

	for (int i = 0; i < tempFields.length; i++)
	  {
	    DBField _field = (DBField) tempFields[i];
	    definition = _field.getFieldDef();

	    if (definition.getNameSpace() != null)
	      {
		if (_field.isVector())
		  {
		    // mark the elements in the vector in the namespace
		    // note that we don't use the namespace mark method here, 
		    // because we are just setting up the namespace, not
		    // manipulating it in the context of an editset

		    for (int j = 0; j < _field.size(); j++)
		      {
			definition.getNameSpace().receiveValue(_field.key(j), _field);
		      }
		  }
		else
		  {
		    // mark the scalar value in the namespace
		    definition.getNameSpace().receiveValue(_field.key(), _field);
		  }
	      }
	  }

	// update the backpointers for this object

	obj.registerAsymmetricLinks();

	base.put(obj);
      }
  }

  public String toString()
  {
    if (base != null && obj != null)
      {
	return obj.getPrintString();
      }
    else if (base != null)
      {
	return "base: " + base.toString() + "\n" +
	  "id: " + id + "\n" +
	  "obj: null";
      }
    else if (obj != null)
      {
	return "base: null\n" +
	  "id: " + id + "\n" +
	  "obj: \n" + obj.getPrintString();
      }
    else
      {
	return "base: null\n" +
	  "id: " + id + "\n" +
	  "obj: null";
      }
  }

  /**
   * This is a convenience method used by server-side code to send
   * debug output to stderr and to any attached admin consoles.
   */

  static public void debug(String string)
  {
    if (debug)
      {
	System.err.println(string);
      }
  }
}
