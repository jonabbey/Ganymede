/*
   GASH 2

   DBStore.java

   The GANYMEDE object storage system.

   Created: 2 July 1996
   Version: $Revision: 1.9 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import java.io.*;
import java.util.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                         DBStore

------------------------------------------------------------------------------*/

/**
 * <p>DBStore is the main data store class.  Any code that intends to make use
 * of the arlut.csd.ganymede package needs to instantiate an object of type DBStore.
 *
 * A user can have any number of DBStore objects active, but there is probably
 * no good reason for doing so since a single DBStore can store and cross reference
 * up to 32k different kinds of objects.</p>
 *
 */

public class DBStore {

  // type identifiers used in the object store

  public static final short FIRST = 0;
  public static final short BOOLEAN = 0;
  public static final short NUMERIC = 1;
  public static final short DATE = 2;
  public static final short STRING = 3;
  public static final short INVID = 4;
  public static final short LAST = 4;

  static final String id_string = "Gstore";
  static final byte major_version = 1;
  static final byte minor_version = 0;

  static final boolean debug = true;

  /* - */

  Hashtable objectBases;	// hash mapping object type to DBObjectBase's
  Hashtable lockHash;		// identifier keys for current locks
  Vector nameSpaces;		// unique valued hashes

  DBJournal journal = null;

  /* -- */

  /**
   *
   * This is the constructor for DBStore.
   *
   * Currently, once you construct a DBStore object, all you can do to
   * initialize it is call load().  This API needs to be extended to
   * provide for programmatic bootstrapping, or another tool needs
   * to be produced for the purpose.
   *
   */

  public DBStore()
  {
    objectBases = null;
    lockHash = null;
    nameSpaces = new Vector();
  }

  /**
   *
   * Load the database from disk.
   *
   * Currently, this method is the only way to peform intialization
   * of the database.  This method loads both the database type
   * definition and database contents from a single disk file.
   *
   * @param filename Name of the database file
   * @see arlut.csd.ganymede.DBJournal
   *
   */

  public void load(String filename)
  {
    FileInputStream inStream = null;
    DataInputStream in;

    DBObjectBase tempBase;
    short baseCount, namespaceCount;
    String namespaceID;
    boolean caseInsensitive;
    String file_id;
    byte file_major, file_minor;

    /* -- */

    try
      {
	inStream = new FileInputStream(filename);
	in = new DataInputStream(inStream);

	try
	  {
	    file_id = in.readUTF();
	    
	    if (!file_id.equals(id_string))
	      {
		System.err.println("DBStore initialization error: DBStore id mismatch for " + filename);
		throw new RuntimeException("DBStore initialization error (" + filename + ")");
	      }
	  }
	catch (IOException ex)
	  {
 	    System.err.println("DBStore initialization error: DBStore id read failure for " + filename);
	    System.err.println("IOException: " + ex);
	    throw new RuntimeException("DBStore initialization error (" + filename + ")");
	  }

	file_major = in.readByte();
	file_minor = in.readByte();

	if (file_major != major_version)
	  {
	    System.err.println("DBStore initialization error: major version mismatch");
	    throw new Error("DBStore initialization error (" + filename + ")");
	  }

	// read in the namespace definitions

	namespaceCount = in.readShort();

	for (int i = 0; i < namespaceCount; i++)
	  {
	    nameSpaces.addElement(new DBNameSpace(in));
	  }
	
	baseCount = in.readShort();
	
	objectBases = new Hashtable(baseCount);

	// Actually read in the object bases
	
	for (short i = 0; i < baseCount; i++)
	  {
	    tempBase = new DBObjectBase(in, this);
	    
	    objectBases.put(new Short(tempBase.type_code), tempBase);
	  }
      }
    catch (IOException ex)
      {
	System.err.println("DBStore initialization error: couldn't properly process " + filename);
	System.err.println("IOException: " + ex);
	throw new RuntimeException("DBStore initialization error (" + filename + ")");
      }
    finally
      {
	if (inStream != null)
	  {
	    try
	      {
		inStream.close();
	      }
	    catch (IOException ex)
	      {
	      }
	  }
      }

    lockHash = new Hashtable(baseCount);

    try 
      {
	journal = new DBJournal(this, "journal"); // need to parametrize filename
      }
    catch (IOException ex)
      {
	// what do we really want to do here?

	throw new RuntimeException("couldn't initialize journal");
      }

    if (!journal.clean())
      {
	try
	  {
	    if (!journal.load())
	      {
		throw new RuntimeException("problem loading journal");
	      }
	    else
	      {
		// go ahead and consolidate the journal into the DBStore
		// before we really get under way.

		if (!journal.clean())
		  {
		    dump(filename, true);
		  }
	      }
	  }
	catch (IOException ex)
	  {
	    // what do we really want to do here?
	    
	    throw new RuntimeException("couldn't load journal");
	  }
      }
  }

  /**
   *
   * Dump the database to disk
   *
   * This method dumps the entire database to disk.  The thread that calls the
   * dump method will be suspended until there are no threads performing update
   * writes to the in-memory database.  In practice this will likely never be
   * a long interval.  Note that this method *will* dump the database, even
   * if no changes have been made.  You should check the DBStore journal's 
   * clean() method to determine whether or not a dump is really needed.
   *
   * The dump is guaranteed to be transaction consistent.
   *
   * @param filename Name of the database file to emit
   * @param releaseLock boolean.  If releaseLock==false, dump() will not release
   *                              the dump lock when it is done with the dump.  This
   *                              is intended to allow for a clean shut down.  For
   *                              non-terminal dumps, releaseLock should be true.
   *
   * @see arlut.csd.ganymede.DBEditSet
   * @see arlut.csd.ganymede.DBJournal
   *
   */

  public void dump(String filename, boolean releaseLock) throws IOException
  {
    File dbFile = null;
    FileOutputStream outStream = null;
    DataOutputStream out = null;
    Enumeration basesEnum;
    short baseCount, namespaceCount;
    DBDumpLock lock = null;
    DBNameSpace ns;

    /* -- */

    if (debug)
      {
	System.err.println("DBStore: Dumping");
      }

    lock = new DBDumpLock(this);
    lock.establish("System");	// wait until we get our lock 
    
    // Move the old version of the file to a backup
    
    try
      {
	dbFile = new File(filename);
	if (dbFile.isFile())
	  {
	    dbFile.renameTo(new File(filename + ".bak"));
	  }

	// and dump the whole thing

	outStream = new FileOutputStream(filename);
	out = new DataOutputStream(outStream);

	out.writeUTF(id_string);
	out.writeByte(major_version);
	out.writeByte(minor_version);

	namespaceCount = (short) nameSpaces.size();

	out.writeShort(namespaceCount);

	for (int i = 0; i < namespaceCount; i++)
	  {
	    ns = (DBNameSpace) nameSpaces.elementAt(i);
	    ns.emit(out);
	  }

	baseCount = (short) objectBases.size();

	out.writeShort(baseCount);
	
	basesEnum = objectBases.elements();

	while (basesEnum.hasMoreElements())
	  {
	    ((DBObjectBase) basesEnum.nextElement()).emit(out);
	  } 
      }
    catch (IOException ex)
      {
	System.err.println("DBStore error dumping to " + filename);
	throw ex;
      }
    finally
      {
	if (releaseLock)
	  {
	    if (lock != null)
	      {
		lock.release();
	      }
	  }

	if (out != null)
	  {
	    out.close();
	  }
	   
	if (outStream != null)
	  {
	    outStream.close();
	  }	      
      }

    if (journal != null)
      {
	journal.reset();
      }
  }

  /**
   *
   * <p>Get a session handle on this database</p>
   *
   * <p>The key parameter is intended to provide a handle for
   * whatever code integrates a DBStore to use to keep track of
   * things.  This may be superfluous with the mere presence of 
   * a DBSession.. I will probably change this, possibly to use
   * a logging interface instance, possibly not.</p>
   *
   * @param key Identifying key
   *
   */

  public DBSession login(Object key)
  {
    return new DBSession(this, key);
  }

  /**
   *
   * <p>Do a printable dump of the object databases</p>
   *
   * @param out PrintStream to print to
   *
   */

  public void printBases(PrintStream out)
  {
    Enumeration enum;

    /* -- */

    enum = objectBases.elements();

    while (enum.hasMoreElements())
      {
	((DBObjectBase) enum.nextElement()).print(out);
      }
  }

  /**
   *
   * Returns the object definition class for the id class.
   *
   * @param id Type id for the base to be returned
   *
   */

  public DBObjectBase getObjectBase(Short id)
  {
    return (DBObjectBase) objectBases.get(id);
  }
}

