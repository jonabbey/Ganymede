/*

   PermissionMatrixDBField.java

   This class defines the permission matrix field used in the
   'Admin' DBObjectBase class.
   
   Created: 27 June 1997
   Version: $Revision: 1.23 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import java.io.*;
import java.util.*;
import java.rmi.*;

import arlut.csd.JDialog.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                         PermissionMatrixDBField

------------------------------------------------------------------------------*/

public class PermissionMatrixDBField extends DBField implements perm_field {

  static final boolean debug = false;

  /**
   *
   * This utility method extracts the DBObjectBase name from a coded
   * permission entry held in a PermMatrix/PermissionMatrixDBField
   * Matrix.
   * 
   */

  public static String decodeBaseName(String entry)
  {
    int sepIndex;
    short basenum;
    DBObjectBase base;
    String basename;

    /* -- */

    sepIndex = entry.indexOf(':');

    if (sepIndex != -1)
      {
	try
	  {
	    basenum = new Short(entry.substring(0, sepIndex)).shortValue();
	    base = (DBObjectBase) Ganymede.db.getObjectBase(basenum);

	    if (base == null)
	      {
		basename = "INVALID: " + entry;
	      }
	    else
	      {
		basename = base.getName();
	      }
	  }
	catch (NumberFormatException ex)
	  {
	    basename = entry;
	  }
      }
    else
      {
	basename = entry;
      }

    return basename;
  }

  /**
   *
   * This method extracts the DBObjectBaseField name from a coded
   * permission entry held in a PermMatrix/PermissionMatrixDBField
   * Matrix.
   * 
   */

  public static String decodeFieldName(String entry)
  {
    int sepIndex;
    short basenum;
    DBObjectBase base;
    String basename;
    
    String fieldId;
    short fieldnum;
    DBObjectBaseField field;
    String fieldname;

    /* -- */

    sepIndex = entry.indexOf(':');

    if (sepIndex != -1)
      {
	try
	  {
	    basenum = new Short(entry.substring(0, sepIndex)).shortValue();
	    base = (DBObjectBase) Ganymede.db.getObjectBase(basenum);

	    if (base == null)
	      {
		fieldname = "[error " + entry + "]";
	      }
	    else
	      {
		fieldId = entry.substring(sepIndex+1);
	    
		if (fieldId.equals(":"))
		  {
		    fieldname = "[base]";
		  }
		else if (fieldId.equals("default"))
		  {
		    fieldname = "[Default]";
		  }
		else
		  {
		    try
		      {
			fieldnum = new Short(fieldId).shortValue();

			field = (DBObjectBaseField) base.getField(fieldnum);

			if (field == null)
			  {
			    fieldname = "invalid:" + fieldId;
			  }
			else
			  {
			    fieldname = field.getName();
			  }
		      }
		    catch (NumberFormatException ex)
		      {
			fieldname = fieldId;
		      }
		  }
	      }
	  }
	catch (NumberFormatException ex)
	  {
	    fieldname = "[error " + entry + "]";
	  }
      }
    else
      {
	fieldname = "[error " + entry + "]";
      }

    return fieldname;
  }

  /**
   *
   * This method returns true if the given PermMatrix /
   * PermissionMatrixDBField key refers to a currently valid 
   * DBObjectBase/DBObjectBaseField in the loaded schema.
   *
   */

  public static boolean isValidCode(String entry)
  {
    int sepIndex;
    short basenum;
    DBObjectBase base;
    
    String fieldId;
    short fieldnum;
    DBObjectBaseField field;

    /* -- */

    sepIndex = entry.indexOf(':');

    if (sepIndex == -1)
      {
	return false;
      }

    try
      {
	basenum = new Short(entry.substring(0, sepIndex)).shortValue();
	base = (DBObjectBase) Ganymede.db.getObjectBase(basenum);

	if (base == null)
	  {
	    return false;
	  }
	else
	  {
	    fieldId = entry.substring(sepIndex+1);
	    
	    if (!fieldId.equals(":") && !fieldId.equals("default"))
	      {
		try
		  {
		    fieldnum = new Short(fieldId).shortValue();
		    
		    field = (DBObjectBaseField) base.getField(fieldnum);
		    
		    if (field == null)
		      {
			return false;
		      }
		  }
		catch (NumberFormatException ex)
		  {
		    return false;
		  }
	      }
	  }
      }
    catch (NumberFormatException ex)
      {
	return false;
      }

    return true;
  }

  /**
   *
   * This method does a dump to System.err of the permission
   * contents held in matrix.
   *
   */

  public static void debugdump(PermMatrix matrix)
  {
    debugdump(matrix.matrix);
  }

  /**
   *
   * This method does a dump to System.err of the permission
   * contents held in matrix.
   *
   */

  private static void debugdump(Hashtable matrix)
  {
    Enumeration enum;
    String key;
    PermEntry entry;
    short basenum;
    String basename;
    Hashtable baseHash = new Hashtable();
    Vector vec;

    /* -- */

    System.err.println("PermMatrix DebugDump");

    enum = matrix.keys();

    while (enum.hasMoreElements())
      {
	key = (String) enum.nextElement();

	entry = (PermEntry) matrix.get(key);

	basename = decodeBaseName(key);

	if (baseHash.containsKey(basename))
	  {
	    vec = (Vector) baseHash.get(basename);
	  }
	else
	  {
	    vec = new Vector();
	    baseHash.put(basename, vec);
	  }

	vec.addElement(decodeFieldName(key) + " " + entry.toString());
      }

    enum = baseHash.keys();

    while (enum.hasMoreElements())
      {
	key = (String) enum.nextElement();

	System.err.println("Base - " + key);

	vec = (Vector) baseHash.get(key);

	for (int i = 0; i < vec.size(); i++)
	  {
	    System.err.println("\t" + vec.elementAt(i));
	  }
      }
  }

  // ---

  Hashtable matrix;

  /* -- */

  /**
   *
   * Receive constructor.  Used to create a PermissionMatrixDBField
   * from a DBStore/DBJournal DataInput stream.
   * 
   */

  PermissionMatrixDBField(DBObject owner, 
			  DataInput in,
			  DBObjectBaseField definition) throws IOException, RemoteException
  {
    super();			// initialize UnicastRemoteObject

    value = values = null;
    this.owner = owner;
    this.definition = definition;
    receive(in);
  }

  /**
   *
   * No-value constructor.  Allows the construction of a
   * 'non-initialized' field, for use where the DBObjectBase
   * definition indicates that a given field may be present,
   * but for which no value has been stored in the DBStore.<br><br>
   *
   * Used to provide the client a template for 'creating' this
   * field if so desired.
   *
   */

  PermissionMatrixDBField(DBObject owner, 
			  DBObjectBaseField definition) throws RemoteException
  {
    super();			// initialize UnicastRemoteObject

    this.owner = owner;
    this.definition = definition;
    
    matrix = new Hashtable();
    defined = false;
    value = null;
    values = null;
  }

  /**
   *
   * Copy constructor.
   *
   */

  public PermissionMatrixDBField(DBObject owner, 
				 PermissionMatrixDBField field) throws RemoteException
  {
    super();			// initialize UnicastRemoteObject

    // --

    Object key;
    PermEntry entry;

    /* -- */

    value = values = null;

    if (debug)
      {
	System.err.println("PermissionMatrixDBField: Copy constructor");
      }

    this.definition = field.definition;
    this.owner = owner;
    this.matrix = new Hashtable(field.matrix.size());

    Enumeration enum = field.matrix.keys();

    while (enum.hasMoreElements())
      {
	key = enum.nextElement();

	if (debug)
	  {
	    System.err.print("PermissionMatrixDBField: copying ");

	    if (key instanceof DBObjectBase)
	      {
		System.err.print("base " + ((DBObjectBase) key).getName());
	      }
	    else if (key instanceof DBObjectBaseField)
	      {
		System.err.print("basefield " + ((DBObjectBaseField) key).getName());
	      }
	    else
	      {
		System.err.print("unrecognized key");
	      }
	  }

	entry = new PermEntry((PermEntry) field.matrix.get(key));

	if (debug)
	  {
	    System.err.println(" contents: " + entry);
	  }

	this.matrix.put(key, entry);
      }

    defined = true;
  }

  // we never allow setValue

  public boolean verifyTypeMatch(Object v)
  {
    return false;
  }

  // we never allow setValue

  public boolean verifyNewValue(Object v)
  {
    return false;
  }

  /**
   *
   * fancy equals method really does check for value equality
   *
   */

  public synchronized boolean equals(Object obj)
  {
    PermissionMatrixDBField pmdb;
    Enumeration keys;
    String key;
    
    /* -- */

    if (!(obj.getClass().equals(this.getClass())))
      {
	return false;
      }

    pmdb = (PermissionMatrixDBField) obj;

    if (matrix.size() != pmdb.matrix.size())
      {
	return false;
      }

    keys = matrix.keys();

    while (keys.hasMoreElements())
      {
	key = (String) keys.nextElement();

	try
	  {
	    if (!(matrix.get(key).equals(pmdb.matrix.get(key))))
	      {
		return false;
	      }
	  }
	catch (NullPointerException ex)
	  {
	    return false;
	  }
      }
    
    return true;
  }

  /**
   *
   * we don't really want to hash according to our permission
   * contents, so just hash according to our containing object's
   * i.d.
   *
   */

  public Object key()
  {
    return new Integer(owner.getID());
  }

  /**
   *
   * we don't allow setValue.. PermissionMatrixDBField doesn't allow
   * direct setting of the entire matrix.. just use the get() and set()
   * methods below.
   *
   */

  public ReturnVal setValue(Object value, boolean local)
  {
    return Ganymede.createErrorDialog("Server: Error in PermissionMatrixDBField.setValue()",
				      "Error.. can't call setValue() on a PermissionMatrixDBField");
  }

  public Object clone()
  {
    try
      {
	return new PermissionMatrixDBField(owner, this);
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("Couldn't create PermissionMatrixDBField: " + ex);
      }
  }

  synchronized void emit(DataOutput out) throws IOException
  {
    Enumeration keys;
    PermEntry pe;
    String key;
    Vector removals = null;

    /* -- */

    // If we have invalid entries, we're just going to throw them out,
    // forget they even existed..  this is reasonable
    // because matrix is private to this class.. PermissionMatrixDBField
    // is responsible for maintaining the meaningful content of the permissions
    // entered into it, not the particulars of the matrix.

    // The permisison matrix bits generally become invalid after
    // schema editing.  Since normally the database/schema needs to be
    // dumped after changing the schema, this is an appropriate place
    // to do the cleanup.

    if (debug)
      {
	debugdump(matrix);
      }

    clean();

    // now actually emit stuff.

    out.writeInt(matrix.size());

    keys = matrix.keys();

    while (keys.hasMoreElements())
      {
	key = (String) keys.nextElement();
	pe = (PermEntry) matrix.get(key);

	out.writeUTF(key);
	pe.emit(out);
      }
  }

  synchronized void receive(DataInput in) throws IOException
  {
    int tableSize;
    PermEntry pe;
    String key;

    /* -- */

    tableSize = in.readInt();
    matrix = new Hashtable(tableSize);
    
    for (int i = 0; i < tableSize; i++)
      {
	key = in.readUTF();
	pe = new PermEntry(in);
	matrix.put(key, pe);
      }

    defined = true;
  }

  public synchronized String getValueString()
  {
    if (!verifyReadPermission())
      {
	throw new IllegalArgumentException("permission denied to read this field");
      }

    if (value == null)
      {
	return "null";
      }

    return "PermissionMatrix";
  }

  /**
   *
   * The default getValueString() encoding is acceptable.
   *
   */

  public String getEncodingString()
  {
    return getValueString();
  }

  /**
   *
   * Returns a String representing the change in value between this
   * field and orig.  This String is intended for logging and email,
   * not for any sort of programmatic activity.  The format of the
   * generated string is not defined, but is intended to be suitable
   * for inclusion in a log entry and in an email message.<br><br>
   *
   * If there is no change in the field, null will be returned.
   * 
   */

  public String getDiffString(DBField orig)
  {
    PermissionMatrixDBField origP;
    StringBuffer result = new StringBuffer();

    /* -- */

    if (!(orig instanceof PermissionMatrixDBField))
      {
	throw new IllegalArgumentException("bad field comparison");
      }

    origP = (PermissionMatrixDBField) orig;

    if (!origP.equals(this))
      {
	return "\tPermission Matrix Changes.. Descriptive Code Not Yet Implemented";
      }
    else
      {
	return null;
      }
  }

  /**
   *
   * Return a serializable, read-only copy of this field's permission
   * matrix
   *
   * @see arlut.csd.ganymede.perm_field
   *
   */

  public PermMatrix getMatrix()
  {
    return new PermMatrix(this);
  }

  /**
   *
   * Returns a PermEntry object representing this PermMatrix's 
   * permissions on the field &lt;fieldID&gt; in base &lt;baseID&gt;
   *
   * @see arlut.csd.ganymede.perm_field
   * @see arlut.csd.ganymede.PermMatrix
   */

  public PermEntry getPerm(short baseID, short fieldID)
  {
    PermEntry result;

    /* -- */

    result = (PermEntry) matrix.get(matrixEntry(baseID, fieldID));

    if (result == null)
      {
	result = (PermEntry) matrix.get(matrixEntry(baseID, "default"));
      }

    return result;
  }

  /**
   *
   * Returns a PermEntry object representing this PermMatrix's 
   * permissions on the base &lt;baseID&gt;
   *
   * @see arlut.csd.ganymede.perm_field
   * @see arlut.csd.ganymede.PermMatrix
   */

  public PermEntry getPerm(short baseID)
  {
    return (PermEntry) matrix.get(matrixEntry(baseID));
  }

  /**
   *
   * Returns a PermEntry object representing this PermMatrix's 
   * permissions on the field &lt;field&gt; in base &lt;base&gt;
   *
   * @see arlut.csd.ganymede.perm_field
   * @see arlut.csd.ganymede.PermMatrix
   */

  public PermEntry getPerm(Base base, BaseField field)
  {
    PermEntry result;

    /* -- */

    try
      {
	result = (PermEntry) matrix.get(matrixEntry(base.getTypeID(), 
						    field.getID()));
	if (result == null)
	  {
	    result = (PermEntry) matrix.get(matrixEntry(base.getTypeID(), 
							"default"));
	  }

	return result;
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("caught remote: " + ex);
      }
  }

  /**
   *
   * Returns a PermEntry object representing this PermMatrix's 
   * permissions on the base &lt;base&gt;
   *
   * @see arlut.csd.ganymede.perm_field
   * @see arlut.csd.ganymede.PermMatrix
   */

  public PermEntry getPerm(Base base)
  {
    try
      {
	return (PermEntry) matrix.get(matrixEntry(base.getTypeID()));
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("caught remote: " + ex);
      }
  }

  /**
   *
   * Sets the permission entry for this matrix for base &lt;base&gt;,
   * field &lt;field&gt; to PermEntry &lt;entry&gt;<br><br>
   *
   * This operation will fail if this permission matrix is not
   * associated with a currently checked-out-for-editing
   * PermissionMatrixDBField.
   *
   * @see arlut.csd.ganymede.perm_field
   * @see arlut.csd.ganymede.PermEntry
   */

  public ReturnVal setPerm(Base base, BaseField field, PermEntry entry)
  {
    try
      {
	return setPerm(base.getTypeID(), field.getID(), entry);
      }
    catch (RemoteException ex)
      {
	return Ganymede.createErrorDialog("Couldn't process setPerm",
					  ex.getMessage());
      }
  }

  /**
   *
   * Sets the permission entry for this matrix for base &lt;baseID&gt;,
   * field &lt;fieldID&gt; to PermEntry &lt;entry&gt;.<br><br>
   *
   * This operation will fail if this permission matrix is not
   * associated with a currently checked-out-for-editing
   * PermissionMatrixDBField.
   *
   * @see arlut.csd.ganymede.perm_field
   * @see arlut.csd.ganymede.PermEntry 
   */

  public synchronized ReturnVal setPerm(short baseID, short fieldID, PermEntry entry)
  {
    if (isEditable())
      {
	if (allowablePermEntry(baseID, fieldID, entry))
	  {
	    matrix.put(matrixEntry(baseID, fieldID), entry);
	  }
	else 
	  {
	    DBObjectBase base = Ganymede.db.getObjectBase(baseID);
	    DBObjectBaseField field = (DBObjectBaseField) base.getField(fieldID);

	    String baseName = base.getName();
	    String fieldName = field.getName();

	    return Ganymede.createErrorDialog("Permissions Error",
					      "You can't set privileges for base " + baseName +
					      ", field " + fieldName + " that you yourself do not have.");
	  }
      }
    else
      {
	throw new IllegalArgumentException("not an editable field");
      }

    if (debug)
      {
	System.err.println("PermissionMatrixDBField: base " + 
			   baseID + ", field " + fieldID + " set to " + entry);
      }

    defined = true;

    return null;
  }

  /**
   *
   * Sets the permission entry for this matrix for base &lt;baseID&gt;
   * to PermEntry &lt;entry&gt;.<br><br>
   *
   * This operation will fail if this permission matrix is not
   * associated with a currently checked-out-for-editing
   * PermissionMatrixDBField.
   *
   * @see arlut.csd.ganymede.perm_field
   * @see arlut.csd.ganymede.PermMatrix
   */

  public ReturnVal setPerm(Base base, PermEntry entry)
  {
    try
      {
	return setPerm(base.getTypeID(), entry);
      }
    catch (RemoteException ex)
      {
	return Ganymede.createErrorDialog("Couldn't process setPerm",
					  ex.getMessage());
      }
  }

  /**
   *
   * Sets the permission entry for this matrix for base &lt;baseID&gt;
   * to PermEntry &lt;entry&gt;<br><br>
   *
   * This operation will fail if this permission matrix is not
   * associated with a currently checked-out-for-editing
   * PermissionMatrixDBField.
   *
   * @see arlut.csd.ganymede.perm_field
   * @see arlut.csd.ganymede.PermEntry
   */

  public synchronized ReturnVal setPerm(short baseID, PermEntry entry)
  {
    if (isEditable())
      {
	if (allowablePermEntry(baseID, (short) -1, entry))
	  {
	    matrix.put(matrixEntry(baseID), entry);
	  }
	else 
	  {
	    DBObjectBase base = Ganymede.db.getObjectBase(baseID);
	    String baseName = base.getName();

	    return Ganymede.createErrorDialog("Permissions Error",
					      "You can't set privileges for base " + baseName +
					      " that you yourself do not have.");
	  }
      }
    else
      {
	throw new IllegalArgumentException("not an editable field");
      }

    if (debug)
      {
	System.err.println("PermissionMatrixDBField: base " + baseID + " set to " + entry);
      }

    defined = true;

    return null;
  }

  /**
   * Sets the default permission entry to apply to fields under base
   * &lt;baseID&gt; to PermEntry &lt;entry&gt;  Once the default fields
   * permission is set for a base, all new fields created under that
   * base will take the default permissions entry.  If the schema editor
   * is used to explicitly set the permissions for a field (even to 
   * no-permissions), that will override the default.<br><br>
   *
   * This operation will fail if this permission matrix is not
   * associated with a currently checked-out-for-editing
   * PermissionMatrixDBField.<br><br>
   *
   * This operation may only be performed by a GanymedeSession 
   * with supergash-level privileges, for security's sake.
   *
   * @see arlut.csd.ganymede.perm_field
   * @see arlut.csd.ganymede.PermEntry 
   */

  public ReturnVal setDefaultFieldsPerm(Base base, PermEntry entry)
  {
    try
      {
	return setDefaultFieldsPerm(base.getTypeID(), entry);
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("caught remote: " + ex);
      }
  }

  /**
   * Sets the default permission entry to apply to fields under base
   * &lt;baseID&gt; to PermEntry &lt;entry&gt;  Once the default fields
   * permission is set for a base, all new fields created under that
   * base will take the default permissions entry.  If the schema editor
   * is used to explicitly set the permissions for a field (even to 
   * no-permissions), that will override the default.<br><br>
   *
   * This operation will fail if this permission matrix is not
   * associated with a currently checked-out-for-editing
   * PermissionMatrixDBField.<br><br>
   *
   * This operation may only be performed by a GanymedeSession 
   * with supergash-level privileges, for security's sake.
   *
   * @see arlut.csd.ganymede.perm_field
   * @see arlut.csd.ganymede.PermEntry 
   */

  public ReturnVal setDefaultFieldsPerm(short baseID, PermEntry entry)
  {
    if (isEditable())
      {
	// it's too hard to make sure that setting all the bits under
	// a base is acceptable for non-supergash admins.. we only
	// do this during bulk-loading anyway, so just reject it
	// if we're not supergash.

	if (owner.gSession != null && owner.gSession.isSuperGash())
	  {
	    matrix.put(matrixEntry(baseID, "default"), entry);
	  }
	else 
	  {
	    return Ganymede.createErrorDialog("Permissions Error",
					      "Only supergash can bulk-load permissions under a base.");
	  }
      }
    else
      {
	throw new IllegalArgumentException("not an editable field");
      }

    if (debug)
      {
	System.err.println("PermissionMatrixDBField: base " + baseID + " set to " + entry);
      }

    defined = true;

    return null;
  }

  /**
   *
   * This internal method is used to cull out any entries in this
   * permissions field that are non-operative, either by referring to
   * an object/field type that does not exist, or by being redundant.
   *  
   */

  private void clean()
  {
    Enumeration keys;
    PermEntry pe;
    String key;

    /* -- */

    keys = matrix.keys();

    while (keys.hasMoreElements())
      {
	key = (String) keys.nextElement();
	pe = (PermEntry) matrix.get(key);

	// If we have invalid entries, we're just going to throw them out,
	// forget they even existed..  this is only remotely reasonable
	// because matrix is private to this class, and because these
	// invalid entries could serve no useful purpose, and will only
	// become invalid after schema editing in any case.  Since
	// normally the database/schema needs to be dumped after changing
	// the schema, this is an appropriate place to do the cleanup.

	if (!isValidCode(key))
	  {
	    matrix.remove(key);

	    if (debug)
	      {
		System.err.println("**** PermissionMatrixDBField.clean(): throwing out invalid entry " + 
				   decodeBaseName(key) + " " + 
				   decodeFieldName(key) + " ---- " + 
				   pe.toString());
	      }
	  }
      }

    // okay, now we want to clean out redundant or non-sensical
    // entries

    String basename;
    Hashtable baseHash = new Hashtable();
    Vector vec;

    /* -- */

    // generate a mapping of basename -> Vector(base/field key)

    keys = matrix.keys();

    while (keys.hasMoreElements())
      {
	key = (String) keys.nextElement();
	pe = (PermEntry) matrix.get(key);

	basename = decodeBaseName(key);

	if (baseHash.containsKey(basename))
	  {
	    vec = (Vector) baseHash.get(basename);
	  }
	else
	  {
	    vec = new Vector();
	    baseHash.put(basename, vec);
	  }

	vec.addElement(key);
      }

    // for each basename, if it has no perms, and none of the
    // base/field keys under it have no perms, clear out those
    // entries.

    keys = baseHash.keys();

    while (keys.hasMoreElements())
      {
	key = (String) keys.nextElement();

	boolean deleteit = true;

	vec = (Vector) baseHash.get(key);

	for (int i = 0; i < vec.size(); i++)
	  {
	    if (!((PermEntry) matrix.get(vec.elementAt(i))).equals(PermEntry.noPerms))
	      {
		deleteit = false;
	      }
	  }

	// if none of the entries for the given base grant any
	// permissions, ditch them all.

	if (deleteit)
	  {
	    for (int i = 0; i < vec.size(); i++)
	      {
		String entryKey = (String) vec.elementAt(i);

		if (debug)
		  {
		    System.err.println("**** PermissionMatrixDBField.clean(): throwing out pointless entry " + 
				       decodeBaseName(entryKey) + " " + 
				       decodeFieldName(entryKey) + " ---- " + 
				       matrix.get(entryKey).toString());
		  }

		matrix.remove(entryKey);
	      }

	  }
      }

    // for each basename, remove any field entries that are merely duplicative
    // of the base's default

    keys = baseHash.keys();

    while (keys.hasMoreElements())
      {
	key = (String) keys.nextElement();

	short basenum;

	try
	  {
	    basenum = Ganymede.db.getObjectBase(key).getTypeID();
	  }
	catch (NullPointerException ex)
	  {
	    continue;
	  }

	String defaultKey = matrixEntry(basenum, "default");
	String baseKey = matrixEntry(basenum);
	PermEntry defaultEntry = (PermEntry) matrix.get(defaultKey);

	if (defaultEntry == null)
	  {
	    continue;
	  }

	// okay, this base has a default record.. get the entries for this base

	vec = (Vector) baseHash.get(key);

	for (int i = 0; i < vec.size(); i++)
	  {
	    String entryKey = (String) vec.elementAt(i);

	    // we don't want to test the default entry itself, nor the
	    // entry for the base as a whole.

	    if (defaultKey.equals(entryKey) || baseKey.equals(entryKey))
	      {
		continue;
	      }

	    if (debug)
	      {
		System.err.println("**** PermissionMatrixDBField.clean(): throwing out redundant entry " + 
				   decodeBaseName(entryKey) + " " + 
				   decodeFieldName(entryKey) + " ---- " + 
				   matrix.get(entryKey).toString());
	      }

	    if (matrix.get(entryKey).equals(defaultEntry))
	      {
		matrix.remove(entryKey);
	      }
	  }
      }
  }

  private String matrixEntry(short baseID, String text)
  {
    return (baseID + ":" + text);
  }

  private String matrixEntry(short baseID, short fieldID)
  {
    return (baseID + ":" + fieldID);
  }
  
  private String matrixEntry(short baseID)
  {
    return (baseID + "::");
  }

  /**
   *
   * This method is used to basically dump state out of this field
   * so that the DBEditSet checkpoint() code can restore it later
   * if need be.
   *
   */

  public synchronized Object checkpoint()
  {
    if (matrix != null)
      {
	return new PermMatrixCkPoint(this);
      }
    else
      {
	return null;
      }
  }

  /**
   *
   * This method is used to basically force state into this field.<br><br>
   *
   * It is used to place a value or set of values that were known to
   * be good during the current transaction back into this field,
   * without creating or changing this DBField's object identity.
   *
   */

  public synchronized void rollback(Object oldval)
  {
    if (!(owner instanceof DBEditObject))
      {
	throw new RuntimeException("Invalid rollback on field " + 
				   getName() + ", not in an editable context");
      }

    if (!(oldval instanceof PermMatrixCkPoint))
      {
	throw new RuntimeException("Invalid rollback on field " + 
				   getName() + ", not a PermMatrixCkPoint");
      }

    if (oldval == null)
      {
	this.defined = false;
      }
    else
      {
	this.matrix = ((PermMatrixCkPoint) oldval).matrix;
	this.defined = true;
      }
  }

  /**
   *
   * This method is used to check that the given operation can be set by the
   * current administrator.<br><br>
   *
   * If fieldID &lt; 0, entry will be checked against the administrator's
   * applicable base permissions.
   *
   */

  public boolean allowablePermEntry(short baseID, short fieldID, PermEntry entry)
  {
    if (owner.gSession == null)
      {
	return false;
      }
    
    if (owner.gSession.isSuperGash())
      {
	return true;
      }

    if (entry == null)
      {
	throw new IllegalArgumentException("entry is null");
      }
    
    if (getName().equals("Owned Object Bits"))
      {
	if (owner.gSession.personaPerms == null)
	  {
	    return false;
	  }

	PermEntry adminPriv;

	if (fieldID < 0)
	  {
	    adminPriv = (PermEntry) owner.gSession.delegatablePersonaPerms.getPerm(baseID);
	  }
	else
	  {
	    adminPriv = (PermEntry) owner.gSession.delegatablePersonaPerms.getPerm(baseID, fieldID);
	  }

	// the adminPriv should have all the bits set that we are seeking to set

	return entry.equals(entry.intersection(adminPriv));
      }
    else if (getName().equals("Default Bits"))
      {
	if (owner.gSession.defaultPerms == null)
	  {
	    return false;
	  }

	PermEntry adminPriv;

	if (fieldID < 0)
	  {
	    adminPriv = (PermEntry) owner.gSession.delegatableDefaultPerms.getPerm(baseID);
	  }
	else
	  {
	    adminPriv = (PermEntry) owner.gSession.delegatableDefaultPerms.getPerm(baseID, fieldID);
	  }

	// the adminPriv should have all the bits set that we are seeking to set

	return entry.equals(entry.intersection(adminPriv));
      }
    else
      {
	throw new RuntimeException("Error, don't recognize field name.. should be 'Owned Object Bits' or " +
				   "'Default Bits'.");
      }
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                               PermMatrixCkPoint

------------------------------------------------------------------------------*/

class PermMatrixCkPoint {

  Hashtable matrix = new Hashtable();

  /* -- */

  public PermMatrixCkPoint(PermissionMatrixDBField field)
  {
    Enumeration enum = field.matrix.keys();
    Object key;
    PermEntry val;

    /* -- */

    while (enum.hasMoreElements())
      {
	key = enum.nextElement();
	val = (PermEntry) field.matrix.get(key);

	matrix.put(key, new PermEntry(val));
      }
  }

}

