/*
   GASH 2

   DBStore.java

   The GANYMEDE object storage system.

   Created: 2 July 1996
   Version: $Revision: 1.3 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package csd.DBStore;

import java.io.*;
import java.util.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                         DBStore

------------------------------------------------------------------------------*/

/**
 * <p>DBStore is the main data store class.  Any code that intends to make use
 * of the csd.DBStore package needs to instantiate an object of type DBStore.
 *
 * A user can have any number of DBStore objects active, but there is probably
 * no good reason for doing so since a single DBStore can store and cross reference
 * up to 32k different kinds of objects.</p>
 *
 */

public class DBStore {

  // type identifiers used in the object store

  static final short FIRST = 0;
  static final short BOOLEAN = 0;
  static final short NUMERIC = 1;
  static final short DATE = 2;
  static final short STRING = 3;
  static final short INVID = 4;
  static final short BOOLEANARRAY = 5;
  static final short NUMERICARRAY = 6;
  static final short DATEARRAY = 7;
  static final short STRINGARRAY = 8;
  static final short INVIDARRAY = 9;
  static final short FINAL =9;

  static final String id_string = "Gstore";
  static final byte major_version = 0;
  static final byte minor_version = 0;

  /* - */

  Hashtable objectBases;	// hash mapping object type to DBObjectBase's
  Hashtable lockHash;		// identifier keys for current locks
  Vector nameSpaces;		// unique valued hashes

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
   *
   */

  public void load(String filename)
  {
    FileInputStream inStream = null;
    DataInputStream in;

    DBObjectBase tempBase;
    short baseCount;
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
	
	baseCount = in.readShort();
	
	objectBases = new Hashtable(baseCount);

	// Actually read in the object bases
	
	for (short i = 0; i < baseCount; i++)
	  {
	    tempBase = new DBObjectBase(in, this);
	    
	    objectBases.put(new Integer(tempBase.type_code), tempBase);
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
  }

  /**
   *
   * Dump the database to disk
   *
   * This method dumps the entire database to disk.  The thread that calls the
   * dump method will be suspended until there are no threads performing update
   * writes to the in-memory database.  In practice this will likely never be
   * a long interval.
   *
   * The dump is guaranteed to be transaction consistent.
   *
   * @param filename Name of the database file to emit
   *
   * @see csd.DBStore.DBEditSet
   *
   */

  public void dump(String filename) throws IOException
  {
    FileOutputStream outStream = null;
    DataOutputStream out = null;
    Enumeration basesEnum;
    short baseCount;
    DBDumpLock lock = null;

    /* -- */

    try
      {
	lock = new DBDumpLock(this);
	lock.establish("System");	// wait until we get our lock 

	outStream = new FileOutputStream(filename);
	out = new DataOutputStream(outStream);

	out.writeUTF(id_string);
	out.writeByte(major_version);
	out.writeByte(minor_version);

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
	if (lock != null)
	  {
	    lock.release();
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
}

