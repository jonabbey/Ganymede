/*

   DBJournal.java

   Class to handle the journal file for the DBStore.
   
   Created: 3 December 1996
   Release: $Name:  $
   Version: $Revision: 1.39 $
   Last Mod Date: $Date: 2001/01/12 01:47:40 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999, 2000
   The University of Texas at Austin.

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
   along with this program; if not, write to the Free Software
   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.

*/

package arlut.csd.ganymede;

import java.io.*;
import java.util.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                       DBJournal

------------------------------------------------------------------------------*/

/**
 * <P>The DBJournal class is used to provide journalling of changes to the
 * {@link arlut.csd.ganymede.DBStore DBStore}
 * during operations.  The Journal file will contain a complete list of all
 * changes made since the last dump of the complete DBStore.  The Journal file
 * is composed of a header block followed by a number of transactions.</P>
 *
 * <P>Each transaction consists of a number of object modification records, each
 * record specifying the creation, deletion, or modification of a particular
 * object.  At the end of the transaction, a marker indicates the completion of
 * the transaction.  At DBStore startup time, the journal is read in and all
 * complete transactions recorded are performed on the main DBStore.</P>
 *
 * <P>Generally, if the DBStore was shut down correctly, the entire memory
 * structure of the DBStore will be cleanly dumped out and the Journal will
 * be removed.  The Journal is intended to insure that the DBStore remains
 * transaction consistent if the server running Ganymede crashes during
 * runtime.</P>
 *
 * <P>See the {@link arlut.csd.ganymede.DBEditSet DBEditSet} class for
 * more information on Ganymede transactions.</P>
 */

public class DBJournal implements ObjectStatus {

  static boolean debug = false;

  public static void setDebug(boolean val)
  {
    debug = val;
  }

  static final String id_string = "GJournal";
  static final byte major_version = 2;
  static final byte minor_version = 0;

  static final String OPENTRANS = "open";
  static final String CLOSETRANS = "close";

  static final byte CREATE = 1;
  static final byte EDIT = 2;
  static final byte DELETE = 3;

  // instance variables

  String filename;
  RandomAccessFile jFile = null;
  DBStore store = null;
  boolean dirty = false;	// dirty is true if the journal has any
				// transactions written out
  int transactionsInJournal = 0;

  /* -- */

  static void initialize(DataOutput out) throws IOException
  {
    out.writeUTF(DBJournal.id_string);
    out.writeShort(DBJournal.major_version);
    out.writeShort(DBJournal.minor_version);
    out.writeLong(System.currentTimeMillis());
  }

  /* -- */

  /**
   *
   * <p>The DBJournal constructor takes a filename and creates a DBJournal object.
   * If the file named does not exist, the DBJournal constructor will create
   * the file and write the DBJournal header, leaving the file pointer pointing
   * to the end of the file.  If the Journal file does exist, the constructor
   * will advance the file pointer to the end of the file.</p>
   *
   * <p>In either case, the file will be made ready for new transactions to be
   * written.</p>
   *
   */
  
  public DBJournal(DBStore store, String filename) throws IOException
  {
    this.store = store;
    this.filename = filename;

    if (this.store == null)
      {
	throw new IllegalArgumentException("bad parameter, store == null");
      }

    if (this.filename == null)
      {
	throw new IllegalArgumentException("bad parameter, filename == null");
      }

    if (debug)
      {
	System.err.println("Initializing DBStore Journal:" + filename);
      }

    File file = new File(filename);

    if (!file.exists())
      {
	// create an empty Journal file

	if (debug)
	  {
	    System.err.println("Creating Journal File");
	  }

	jFile = new RandomAccessFile(filename, "rw");

	if (debug)
	  {
	    System.err.println("Writing DBStore Journal header");
	  }
	initialize(jFile);

	dirty = false;
      }
    else
      {
	// open an existing Journal file and prepare to
	// write to the end of it

	if (debug)
	  {
	    System.err.println("Opening Journal File for Append");
	  }

	jFile = new RandomAccessFile(filename, "rw");

	// look to see if there are any transactions in the journal

	readHeaders();

	try
	  {
	    if (jFile.readUTF().compareTo(OPENTRANS) != 0)
	      {
		if (debug)
		  {
		    System.err.println("DBJournal constructor: open string mismatch");
		  }
		throw new IOException();
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
   *
   * The reset() method is used to copy the journal file to a safe location and
   * truncate it.  reset() should be called immediately after the DBStore is
   * dumped to disk and before the DumpLock is relinquished.
   *
   */

  public synchronized void reset() throws IOException
  {
    String newname;

    /* - */

    if (debug)
      {
	System.err.println("DBJournal: Resetting Journal File");
      }

    if (jFile != null)
      {
	jFile.close();
      }

    File file = new File(filename);

    newname = filename + ".old";

    if (debug)
      {
	System.err.println("DBJournal: saving old Journal as " + newname);
      }

    file.renameTo(new File(newname));

    if (debug)
      {
	System.err.println("DBJournal: creating fresh Journal " + filename);
      }

    jFile = new RandomAccessFile(filename, "rw");
    initialize(jFile);

    dirty = false;

    transactionsInJournal = 0;
    GanymedeAdmin.updateTransCount();

    if (Ganymede.log != null)
      {
	Ganymede.log.logSystemEvent(new DBLogEvent("journalreset",
						   "Ganymede Journal Reset",
						   null,
						   null,
						   null,
						   null));
      }
  }

  /**
   * <P>The load() method reads in all transactions in the current DBStore Journal
   * and makes the appropriate changes to the DBStore Object Bases.  This
   * method should be called after the main body of the DBStore is loaded
   * and before the DBStore is put into production mode.</P>
   *
   * <P>load() will return true if the on-disk journal was in a consistent state,
   * with no incomplete transactions.  If load() encounters EOF in the middle
   * of attempting to read in a transaction record, load() will return false.  In
   * either case, any valid and complete transaction records will be processed
   * and integrated into the DBStore.</P>
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
		if (debug)
		  {
		    System.err.println("DBJournal.load(): Transaction open string mismatch");
		  }

		throw new IOException("DBJournal.load(): Transaction open string mismatch");
	      }
	    else
	      {
		if (debug)
		  {
		    System.err.println("DBJournal.load(): Transaction open string match OK");
		  }
	      }
	    
	    status = "Reading transaction time";

	    EOFok = false;

	    transaction_time = jFile.readLong();
	    transactionDate = new Date(transaction_time);

	    status = "Reading object count";

	    //	    if (debug)
	    //	      {

	    System.err.println("Transaction: " + transactionDate);

	    //	      }

	    // read object information

	    object_count = jFile.readInt();

	    if (debug)
	      {
		System.err.println("Objects In Transaction: " + object_count);
	      }

	    if (object_count > 0)
	      {
		dirty = true;
	      }

	    for (int i = 0; i < object_count; i++)
	      {
		status = "Reading operation code for object " + i;

		operation = jFile.readByte();

		status = "Reading object type for object " + i;

		obj_type = jFile.readShort();
		base = (DBObjectBase) store.objectBases.get(new Short(obj_type));

		switch (operation)
		  {
		  case CREATE:
		
		    status = "Reading created object " + i;

		    obj = new DBObject(base, jFile, true);
		
		    System.err.print("Create: " + obj.getInvid());

		    if (debug)
		      {
			obj.print(System.err);
		      }

		    entries.addElement(new JournalEntry(base, obj.getID(), obj));

		    break;

		  case EDIT:

		    status = "Reading edited object " + i;

		    DBObjectDeltaRec delta = new DBObjectDeltaRec(jFile);

		    short typecode = delta.invid.getType();
		    int objnum = delta.invid.getNum();

		    DBObject original = Ganymede.db.getObjectBase(typecode).objectTable.get(objnum);

		    obj = delta.applyDelta(original);

		    if (!base.objectTable.containsKey(obj.getID()))
		      {
			System.err.println("DBJournal.load(): modified object in the journal does not previously exist in DBStore.");
		      }

		    if (debug)
		      {
			System.err.print("Edit: ");
			obj.print(System.err);
		      }

		    entries.addElement(new JournalEntry(base, obj.getID(), obj));

		    break;

		  case DELETE:

		    status = "Reading deleted object " + i;

		    obj_id = jFile.readShort();

		    if (debug)
		      {
			System.err.println("Delete: " + base.object_name + ":" + obj_id);
		      }
		
		    entries.addElement(new JournalEntry(base, obj_id, null));
		    break;
		  }
	      }

	    status = "Reading close transaction information";

	    if ((jFile.readUTF().compareTo(CLOSETRANS) != 0) || 
		(jFile.readLong() != transaction_time))
	      {
		throw new IOException("Transaction close timestamp mismatch");
	      }

	    status = "Finished transaction";

	    if (debug)
	      {
		System.err.println("Transaction " + transactionDate + " successfully read from Journal.");
		System.err.println("Integrating transaction into DBStore memory image.");
	      }

	    // okay, process this transaction

	    System.err.println("Processing " + entries.size());

	    for (int i = 0; i < entries.size(); i++)
	      {
		((JournalEntry) entries.elementAt(i)).process(store);
	      }

	    EOFok = true;
	  }
      }
    catch (EOFException ex)
      {
	if (EOFok)
	  {
	    if (debug)
	      {
		System.err.println("All transactions processed successfully");
	      }

	    return true;
	  }
	else
	  {
	    if (debug)
	      {
		System.err.println("DBJournal file unexpectedly ended: state = " + status);
	      }

	    // ok, the journal ended badly, but aside from losing a partial
	    // transaction, we should be ok.

	    return false;
	  }
      }
  }

  /**
   *
   * The writeTransaction() method actually performs the full work of
   * writing out a transaction to the DBStore Journal.
   * writeTransaction() should be called before the changes are
   * actually finalized in the DBStore Object Bases.  If
   * writeTransaction() is not able to successfully write the complete
   * transaction log to the Journal file, an IOException will be
   * thrown.  The Journal entries are structured so that if a Journal
   * entry can't be completed, the transaction's whole entry will
   * be ignored at load time.
   *
   */

  public synchronized boolean writeTransaction(DBEditSet editset) throws IOException
  {
    Enumeration enum;
    DBEditObject eObj;
    long transaction_time = 0;
    Date now;

    /* - */

    now = new Date();
    transaction_time = now.getTime();

    if (debug)
      {
	System.err.println("Writing transaction to the Journal : " + now);
      }

    jFile.writeUTF(OPENTRANS);
    jFile.writeLong(transaction_time);

    if (editset.objects != null)
      {
	if (debug)
	  {
	    System.err.println("Objects in Transaction: " + editset.objects.size());
	  }

	jFile.writeInt(editset.objects.size());

	enum = editset.objects.elements();

	while (enum.hasMoreElements())
	  {
	    eObj = (DBEditObject) enum.nextElement();

	    switch (eObj.getStatus())
	      {
	      case CREATING:
		jFile.writeByte(CREATE);
		jFile.writeShort(eObj.objectBase.type_code);
		eObj.emit(jFile);

		if (debug)
		  {
		    System.err.print("Creating object:\n\t");
		    eObj.print(System.err);
		  }
		break;

	      case EDITING:
		jFile.writeByte(EDIT);
		jFile.writeShort(eObj.objectBase.type_code);

		DBObjectDeltaRec delta = new DBObjectDeltaRec(eObj.original, eObj);
		delta.emit(jFile);
		
		if (debug)
		  {
		    System.err.print("Editing object:\n\t");
		    eObj.print(System.err);
		  }

		break;

	      case DELETING:
		jFile.writeByte(DELETE);
		jFile.writeShort(eObj.objectBase.type_code);
		jFile.writeShort(eObj.getID());

		if (debug)
		  {
		    System.err.print("Deleting object:\n\t");
		    System.err.println(eObj.objectBase.object_name + " : " + eObj.getID());
		  }
		break;

	      case DROPPING:
		if (debug)
		  {
		    System.err.print("Dropping object:\n\t");
		    eObj.print(System.err);
		  }
		break;
	      }
	  }

	dirty = true;
      }
    else
      {
	jFile.writeInt(0);
      }

    // write out the end of transaction stamp.. the transaction_time
    // is used to verify that we completed this write okay.

    jFile.writeUTF(CLOSETRANS);
    jFile.writeLong(transaction_time);

    if (debug)
      {
	System.err.println("Transaction " + now + " successfully written to Journal.");
      }

    transactionsInJournal++;
    GanymedeAdmin.updateTransCount();

    return true;
  }

  /**
   *  returns true if the journal does not contain any transactions
   *
   */

  public boolean isClean()
  {
    return !dirty;
  }

  /**
   *
   * <p>This method is used to read and check the first few fields of the journal
   * as a sanity check on journal open/load.</p>
   *
   * <p>This method <b>MUST NOT</b> be called after the journal is open and active,
   * or else the journal will become corrupted.</p>
   *
   */

  void readHeaders() throws IOException
  {
    if (debug)
      {
	System.err.println("DBJournal: Loading transactions from " + filename);
      }

    jFile.seek(0);
  
    if (DBJournal.id_string.compareTo(jFile.readUTF()) != 0)
      {
	throw new RuntimeException("Error, id_string mismatch.. wrong file type?");
      }

    if (jFile.readShort() != DBJournal.major_version)
      {
	throw new RuntimeException("Error, major_version mismatch.. wrong file type?");
      }

    // skip past the next two bits

    jFile.readShort();		// minor version doesn't matter so much

    if (debug)
      {
	System.err.println("DBJournal file created " + new Date(jFile.readLong()));
      }
    else
      {
	jFile.readLong();		// date is there for others to look at
      }
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                                    JournalEntry

------------------------------------------------------------------------------*/

/**
 * <P>This class holds data corresponding to a modification record for a single
 * object in the server's {@link arlut.csd.ganymede.DBJournal DBJournal} class.</P>
 */

class JournalEntry {

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
    DBObjectBaseField
      definition;

    DBObject 
      badObj;

    DBNameSpaceHandle
      currentHandle;

    /* -- */

    if (obj == null)
      {
	// delete the object
	
	badObj = base.objectTable.get(id);

	if (badObj == null)
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

	    // We do need to unregister any backlinks from the DBStore
	    // backPointers hash structure, however.

	    badObj.unsetBackPointers();

	    // and we need to clear out any namespace pointers

	    db_field[] tempFields = badObj.listFields();

	    for (int i = 0; i < tempFields.length; i++)
	      {
		DBField _field = (DBField) tempFields[i];

		definition = _field.getFieldDef();

		if (definition.namespace != null)
		  {
		    if (_field.isVector())
		      {
			for (int j = 0; j < _field.size(); j++)
			  {
			    currentHandle = (DBNameSpaceHandle) definition.namespace.uniqueHash.get(_field.key(j));

			    if (currentHandle.getField(Ganymede.internalSession) != _field)
			      {
				throw new RuntimeException("Error, namespace mismatch in DBJournal code [" + j + "]");
			      }

			    definition.namespace.uniqueHash.remove(_field.key(j));
			  }
		      }
		    else
		      {
			currentHandle = (DBNameSpaceHandle) definition.namespace.uniqueHash.get(_field.key());

			if (currentHandle.getField(Ganymede.internalSession) != _field)
			  {
			    throw new RuntimeException("Error, namespace mismatch in DBJournal code");
			  }

			definition.namespace.uniqueHash.remove(_field.key());
		      }
		  }
	      }
	  }

	base.objectTable.remove(id);
      }
    else
      {
	// put the new object in place

	// First we need to clear any back links from the old version
	// of the object, if there was any such.

	badObj = base.objectTable.get(id);

	if (badObj != null)
	  {
	    badObj.unsetBackPointers();
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

	    if (definition.namespace != null)
	      {
		if (_field.isVector())
		  {
		    // mark the elements in the vector in the namespace
		    // note that we don't use the namespace mark method here, 
		    // because we are just setting up the namespace, not
		    // manipulating it in the context of an editset

		    for (int j = 0; j < _field.size(); j++)
		      {
			definition.namespace.uniqueHash.put(_field.key(j), 
							    new DBNameSpaceHandle(null, true, _field));
		      }
		  }
		else
		  {
		    // mark the scalar value in the namespace
		    definition.namespace.uniqueHash.put(_field.key(), new DBNameSpaceHandle(null, true, _field));
		  }
	      }
	  }

	base.objectTable.put(obj);

	// update the backpointers for this object

	obj.setBackPointers();

	// keep our base's maxid up to date for
	// any newly created objects in the journal

	if (id > base.maxid)
	  {
	    base.maxid = id;
	  }
      }
  }
}
