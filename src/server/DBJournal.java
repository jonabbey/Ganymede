/*

   DBJournal.java

   Class to handle the journal file for the DBStore.
   
   Created: 3 December 1996
   Version: $Revision: 1.2 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package csd.DBStore;

import java.io.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                       DBJournal

------------------------------------------------------------------------------*/

/**
 *
 * <p>The DBJournal class is used to provide journalling of changes to the DBStore
 * during operations.  The Journal file will contain a complete list of all
 * changes made since the last dump of the complete DBStore.  The Journal file
 * is composed of a header block followed by a number of transactions.  </p>
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
 * runtime. </p>
 *
 */

public class DBJournal {

  static final String id_string = "GJournal";
  static final byte major_version = 0;
  static final byte minor_version = 0;

  static final String OPENTRANS = "open";
  static final String CLOSETRANS = "close";

  String filename;

  RandomAccessFile jFile = null;

  DBStore store = null;

  /* -- */

  static void initialize(DataOutput out)
  {
    out.writeUTF(DBJournal.id_string);
    out.writeShort(DBJournal.major_version);
    out.writeShort(DBJournal.minor_version);
    out.writeLong((new Date()).getTime());
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
  
  public DBJournal(DBStore store, String filename)
  {
    this.store = store;
    this.filename = filename;

    if (this.store == null || this.filename == null)
      {
	throw new RuntimeException("bad parameter");
      }

    File file = new File(filename);

    if (!file.exists())
      {
	// create an empty Journal file

	jFile = new RandomAccessFile(filename, "rw");
	initialize(jFile);
      }
    else
      {
	// open an existing Journal file and prepare to
	// write to the end of it

	jFile = new RandomAccessFile(filename, "rw");
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

  public synchronized boolean reset()
  {
    if (jFile != null)
      {
	jFile.close();
      }

    File file = new File(filename);

    file.renameTo(new File(filename + new Date()));

    jFile = new RandomAccessFile(filename, "rw");
    initialize(jFile);
  }

  /**
   * The load() method reads in all transactions in the current DBStore Journal
   * and makes the appropriate changes to the DBStore Object Bases.  This
   * method should be called after the main body of the DBStore is loaded
   * and before the DBStore is put into production mode.
   *
   */

  public synchronized boolean load(DBStore store) throws IOException;
  {
    long transaction_time = 0;
    int object_count = 0;
    boolean EOFok = true;
    Vector entries = null;
    short obj_type, obj_id;
    DBObjectBase base;
    DBObject obj;

    /* - */

    entries = new Vector();

    jFile.seek(0);
  
    if (DBJournal.id_string.compareTo(jFile.readUTF()))
      {
	throw new RuntimeException("Error, id_string mismatch.. wrong file type?");
      }

    if (jFile.readShort() != DBJournal.major_version)
      {
	throw new RuntimeException("Error, major_version mismatch.. wrong file type?");
      }

    // skip past the next two bits

    jFile.readShort();		// minor version doesn't matter so much
    jFile.readLong();		// date is there for others to look at

    // start reading and applying the changes

    try
      {
	if (jFile.readUTF().compareTo(OPENTRANS) != 0)
	  {
	    throw IOEXCEPTION;
	  }

	EOFok = false;

	transaction_time = jFile.readLong();

	// read newly created objects

	object_count = jFile.readInt();

	for (int i = 0; i < object_count; i++)
	  {
	    obj_type = jFile.readShort();
	    base = store.objectBases.get(new Integer(obj_type));

	    obj = new DBObject(base, jFile);

	    entries.addElement(new JournalEntry(base, obj.id, obj));
	  }

	// read modified objects

	object_count = jFile.readInt();

	for (int i = 0; i < object_count; i++)
	  {
	    obj_type = jFile.readShort();
	    base = store.objectBases.get(new Integer(obj_type));

	    obj = new DBObject(base, jFile);

	    entries.addElement(new JournalEntry(base, obj.id, obj));
	  }

	// read deleted objects

	object_count = jFile.readInt();

	for (int i = 0; i < object_count; i++)
	  {
	    obj_type = jFile.readShort();
	    base = store.objectBases.get(new Integer(obj_type));
	    obj_id = jFile.readShort();

	    entries.addElement(new JournalEntry(base, obj_id, null));
	  }

	if ((jFile.readUTF().compareTo(CLOSETRANS) != 0) || 
	    (jFile.readLong() != transaction_time))
	  {
	    throw new IOException();
	  }

	// okay, process this transaction

	for (int i = 0; i < entries.size(); i++)
	  {
	    ((JournalEntry) entries.elementAt(i)).process();
	  }

	EOFok = true;
      }
    catch (EOFException ex)
      {
	if (EOFok)
	  {
	    return true;
	  }
	else
	  {
	    throw new IOException();
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

  public synchronized boolean writeTransaction(DBEditSet editset) throws IOException;
  {
    Enumeration enum;
    DBObject obj;
    long transaction_time = 0;

    /* - */

    transaction_time = (new Date()).getTime();

    jFile.writeUTF(OPENTRANS);
    jFile.writeLong(transaction_time);

    if (editset.objectsCreated != null)
      {
	jFile.writeInt(editset.objectsCreated.size());

	enum = editset.objectsCreated.elements();

	while (enum.hasMoreElements())
	  {
	    obj = (DBObject) enum.nextElement();
	    jFile.writeShort(obj.objectBase.type_code);
	    obj.emit();
	  }
      }
    else
      {
	jFile.writeInt(0);
      }

    // note that objectsChanged is actually a vector of
    // shadowObjects.. we assume that writeTransaction is
    // called before the shadows are replaced.  We actually
    // don't care about whether or not objectsChanged are
    // actually shadows, as long as they are what we want
    // to be writing out

    if (editset.objectsChanged != null)
      {
	jFile.writeInt(editset.objectsChanged.size());

	enum = editset.objectsChanged.elements();

	while (enum.hasMoreElements())
	  {
	    obj = ((DBObject) enum.nextElement());
	    jFile.writeShort(obj.objectBase.type_code);
	    obj.emit();
	  }
      }
    else
      {
	jFile.writeInt(0);
      }

    if (editset.objectsDeleted != null)
      {
	jFile.writeInt(editset.objectsDeleted.size());

	enum = editset.objectsDeleted.elements();

	while (enum.hasMoreElements())
	  {
	    obj = (DBObject) enum.nextElement();
	    jFile.writeShort(obj.objectBase.type_code);
	    jFile.writeShort(obj.id);
	  }
      }
    else
      {
	jFile.writeInt(0);
      }

    // write out the end of transaction stamp.. the transaction_time
    // is used to verify that we completed this write okay.

    jFile.writeUTF(CLOSETRANS);
    jFile.writeLong(transaction_time);

    return true;
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                                    JournalEntry

------------------------------------------------------------------------------*/

class JournalEntry {

  DBObjectBase base;
  short id;
  DBObject obj;			// if null, we'll delete

  /* -- */
  
  public JournalEntry(DBObjectBase base, short id, DBObject obj)
  {
    this.base = base;
    this.id = id;
    this.obj = obj;
  }

  void process(DBStore store)
  {
    if (obj == null)
      {
	// delete the object
	base.remove(new Integer(id));
      }
    else
      {
	// put the new object in place
	base.put(new Integer(id), obj);
      }
  }
}
