/*

   PermissionMatrixDBField.java

   This class defines the permission matrix field used in the
   'Role' DBObjectBase class.
   
   Created: 27 June 1997
   Release: $Name:  $
   Version: $Revision: 1.44 $
   Last Mod Date: $Date: 2000/08/22 06:43:48 $
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
import java.rmi.*;
import java.lang.reflect.*;
import arlut.csd.JDialog.*;

import com.jclark.xml.output.*;
import arlut.csd.Util.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                         PermissionMatrixDBField

------------------------------------------------------------------------------*/

/** 
 * <P>PermissionMatrixDBField is a subclass of {@link
 * arlut.csd.ganymede.DBField DBField} for the storage and handling of
 * permission matrix fields (used only in the Role
 * {@link arlut.csd.ganymede.DBObject DBObjects}) in the
 * {@link arlut.csd.ganymede.DBStore DBStore} on the Ganymede
 * server.</P>
 *
 * <P>The Ganymede client talks to PermissionMatrixDBFields through the
 * {@link arlut.csd.ganymede.perm_field perm_field} RMI interface.</P> 
 *
 * <P>This class differs a bit from most subclasses of {@link
 * arlut.csd.ganymede.DBField DBField} in that the normal setValue()/getValue()
 * methods are non-functional.  Instead, there are special methods used to set or
 * access permission information from the specially coded Hashtable held by
 * a PermissionMatrixDBField.  This Hashtable maps strings encoded by the
 * {@link arlut.csd.ganymede.PermissionMatrixDBField#matrixEntry(short, short)
 * matrixEntry()} methods to {@link arlut.csd.ganymede.PermEntry PermEntry}
 * objects, which hold create, edit, view, and delete bits.</P>
 *
 * <P>PermissionMatrixDBField's methods encode part of the server's permissions
 * logic, including the restrictions on what bits can be set in a Role's
 * permission matrix based on the rights granted in the client's
 * {@link arlut.csd.ganymede.GanymedeSession GanymedeSession}.  We determine
 * what GanymedeSession we are operating in for that case 
 * by asking our {@link arlut.csd.ganymede.DBEditObject DBEditObject} owner.</P>
 */

public class PermissionMatrixDBField extends DBField implements perm_field {

  static final boolean debug = false;

  /**
   *  Dynamically loaded RMI proxy class definition for this class.
   */

  static Class proxyClass = null;

  /**
   *  Dynamically loaded constructor for instances of the RMI proxy
   */

  static java.lang.reflect.Constructor proxyConstructor = null;

  // ---

  boolean defined = false;

  /**
   *
   * Returns true if this field has a value associated
   * with it, or false if it is an unfilled 'placeholder'.
   *
   * @see arlut.csd.ganymede.db_field
   *
   */

  public boolean isDefined()
  {
    return defined;
  }

  /**
   * <P>This method is used to mark a field as undefined when it is
   * checked out for editing.  Different subclasses of
   * {@link arlut.csd.ganymede.DBField DBField} may
   * implement this in different ways, if simply setting the field's
   * value member to null is not appropriate.  Any namespace values claimed
   * by the field will be released, and when the transaction is
   * committed, this field will be released.</P>
   */

  public synchronized ReturnVal setUndefined(boolean local)
  {
    if (isEditable(local))
      {
	defined = false;
	return null;
      }

    return Ganymede.createErrorDialog("Permissions Error",
				      "Don't have permission to clear this permission matrix field\n" +
				      getName());
  }

  /**
   * <P>This utility method extracts the 
   * {@link arlut.csd.ganymede.DBObjectBase DBObjectBase} name from a coded
   * permission entry held in a
   * {@link arlut.csd.ganymede.PermMatrix PermMatrix}/PermissionMatrixDBField
   * Matrix.</P>
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
   * <P>This utility method extracts the 
   * {@link arlut.csd.ganymede.DBObjectBaseField DBObjectBaseField} name from a coded
   * permission entry held in a
   * {@link arlut.csd.ganymede.PermMatrix PermMatrix}/PermissionMatrixDBField
   * Matrix.</P>
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
   * <P>Returns true if the given String encodes the identity of
   * a {@link arlut.csd.ganymede.DBObjectBase DBObjectBase} and
   * not a field within a DBObjectBase.</P>
   */

  private boolean isBasePerm(String matrixEntry)
  {
    return (matrixEntry.indexOf("::") != -1);
  }

  /**
   * <P>This method returns true if the given
   * {@link arlut.csd.ganymede.PermMatrix PermMatrix}/
   * PermissionMatrixDBField key refers to a currently valid 
   * {@link arlut.csd.ganymede.DBObjectBase DBObjectBase}/
   * {@link arlut.csd.ganymede.DBObjectBaseField DBObjectBaseField}
   * in the loaded schema.</P>
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
	    
	    if (!fieldId.equals(":"))
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
   * contents held in PermissionMatrixDBField me.
   *
   */

  public void debugdump(PermissionMatrixDBField me)
  {
    debugdump(me.matrix);
  }

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
    System.err.println(debugdecode(matrix));
  }

  /**
   * This method generates a string version of the debugdump
   * output.
   */

  static String debugdecode(Hashtable matrix)
  {
    StringBuffer result = new StringBuffer();
    Enumeration enum;
    String key;
    PermEntry entry;
    String basename;
    Hashtable baseHash = new Hashtable();
    Vector vec;

    /* -- */

    result.append("PermMatrix DebugDump\n");

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

	vec.addElement(decodeFieldName(key) + " -- " + entry.toString());
      }

    enum = baseHash.keys();

    while (enum.hasMoreElements())
      {
	key = (String) enum.nextElement();

	//	result.append("\nBase - " + key + "\n");

	vec = (Vector) baseHash.get(key);

	for (int i = 0; i < vec.size(); i++)
	  {
	    result.append(key + ":" + vec.elementAt(i) + "\n");
	  }

	result.append("\n");
      }

    return result.toString();
  }

  // ---

  Hashtable matrix;

  /* -- */

  /**
   * <P>Receive constructor.  Used to create a PermissionMatrixDBField from a
   * {@link arlut.csd.ganymede.DBStore DBStore}/{@link arlut.csd.ganymede.DBJournal DBJournal}
   * DataInput stream.</P>
   */

  PermissionMatrixDBField(DBObject owner, 
			  DataInput in,
			  DBObjectBaseField definition) throws IOException
  {
    super();			// initialize UnicastRemoteObject

    value = null;
    this.owner = owner;
    this.definition = definition;
    receive(in);
  }

  /**
   * <P>No-value constructor.  Allows the construction of a
   * 'non-initialized' field, for use where the 
   * {@link arlut.csd.ganymede.DBObjectBase DBObjectBase}
   * definition indicates that a given field may be present,
   * but for which no value has been stored in the 
   * {@link arlut.csd.ganymede.DBStore DBStore}.</P>
   *
   * <P>Used to provide the client a template for 'creating' this
   * field if so desired.</P>
   */

  PermissionMatrixDBField(DBObject owner, DBObjectBaseField definition)
  {
    this.owner = owner;
    this.definition = definition;
    
    matrix = new Hashtable();
    defined = false;
    value = null;
  }

  /**
   *
   * Copy constructor.
   *
   */

  public PermissionMatrixDBField(DBObject owner, PermissionMatrixDBField field)
  {
    Object key;
    PermEntry entry;

    /* -- */

    value = null;

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

  /**
   * <p>This method creates an RMI proxy object that channels db_field
   * calls to a GanymedeSession object over RMI rather than over a
   * direct RMI field reference.  This is valuable because it can
   * dramatically decrease the number of references that have to be
   * managed through distributed garbage collection, increasing the
   * scalability of the server at the cost of a constant time increase
   * in initial communications for downloading the proxy.</p>
   */

  public db_field getProxy()
  {
    Object proxy = null;

    /* -- */

    synchronized (getClass())
      {
	if (proxyClass == null)
	  {
	    // load

	    try
	      {
		proxyClass = Class.forName("arlut.csd.ganymede.perm_fieldRemote");
	      }
	    catch (ClassNotFoundException ex)
	      {
		System.err.println("DBField.getProxy(): couldn't find arlut.csd.ganymede.perm_fieldRemote");
		ex.printStackTrace();

		return null;
	      }
	  }

	if (proxyConstructor != null)
	  {
	    Class constructParams[] = new Class[3];

	    try
	      {
		constructParams[0] = Class.forName("arlut.csd.ganymede.Invid");
		constructParams[1] = short.class;
		constructParams[2] = Class.forName("arlut.csd.ganymede.Session");
	      }
	    catch (ClassNotFoundException ex)
	      {
		System.err.println("DBField.getProxy(): couldn't find proxy constructor: " + ex.getMessage());
		ex.printStackTrace();

		return null;
	      }
	    
	    try
	      {
		proxyConstructor = proxyClass.getConstructor(constructParams);
	      }
	    catch (NoSuchMethodException ex)
	      {
		ex.printStackTrace();
	      }
	    catch (SecurityException ex)
	      {
		ex.printStackTrace();
	      }
	  }

	Object params[] = new Object[3];

	params[0] = getOwner().getInvid();
	params[1] = new Short(getID());
	params[2] = getOwner().getGSession();
	
	try
	  {
	    proxy = proxyConstructor.newInstance(params);
	  }
	catch (InstantiationException ex)
	  {
	    ex.printStackTrace();
	  }
	catch (IllegalArgumentException ex)
	  {
	    ex.printStackTrace();
	  }
	catch (IllegalAccessException ex)
	  {
	    ex.printStackTrace();
	  }
	catch (InvocationTargetException ex)
	  {
	    ex.printStackTrace();
	  }
	
	return (db_field) proxy;
      }
  }

  // we never allow setValue

  public boolean verifyTypeMatch(Object v)
  {
    return false;
  }

  // we never allow setValue

  public ReturnVal verifyNewValue(Object v)
  {
    return Ganymede.createErrorDialog("Permission Matrix Field Error",
				      "setValue() not allowed on PermissionMatrixDBField.");
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
   * <p>This method copies the current value of this DBField
   * to target.  The target DBField must be contained within a
   * checked-out DBEditObject in order to be updated.  Any actions
   * that would normally occur from a user manually setting a value
   * into the field will occur.</p>
   *
   * @param target The DBField to copy this field's contents to.
   * @param local If true, permissions checking is skipped.
   */

  public synchronized ReturnVal copyFieldTo(DBField target, boolean local)
  {
    if (!local)
      {
	if (!verifyReadPermission())
	  {
	    return Ganymede.createErrorDialog("Copy field error", 
					      "Can't copy field " + getName() + ", no read privileges");
	  }
      }
	
    if (!target.isEditable(local))
      {
	return Ganymede.createErrorDialog("Copy field error", 
					  "Can't copy field " + getName() + ", no write privileges");
      }

    if (!(target instanceof PermissionMatrixDBField))
      {
	return Ganymede.createErrorDialog("Copy field error",
					  "Can't copy field " + getName() +
					  ", target is not a PermissionMatrixDBField.");
      }

    // doing a simple clone of the hashtable is okay, since both the
    // keys and values of the matrix are treated as immutable (they
    // are replaced, not changed in-place)

    ((PermissionMatrixDBField) target).matrix = (Hashtable) this.matrix.clone();

    return null;
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
   * We always return null here..
   *
   */

  public Object getValue()
  {
    return null;
  }

  /** 
   *
   * Returns an Object carrying the value held in this field.<br><br>
   *
   * This is intended to be used within the Ganymede server, it bypasses
   * the permissions checking that getValues() does.
   *
   */

  public Object getValueLocal()
  {
    return null;
  }

  /**
   *
   * we don't allow setValue.. PermissionMatrixDBField doesn't allow
   * direct setting of the entire matrix.. just use the get() and set()
   * methods below.
   *
   */

  public ReturnVal setValue(Object value, boolean local, boolean noWizards)
  {
    return Ganymede.createErrorDialog("Server: Error in PermissionMatrixDBField.setValue()",
				      "Error.. can't call setValue() on a PermissionMatrixDBField");
  }

  public Object clone()
  {
    return new PermissionMatrixDBField(owner, this);
  }

  synchronized void emit(DataOutput out) throws IOException
  {
    Enumeration keys;
    PermEntry pe;
    String key;
    Vector removals = null;

    /* -- */

    if (debug)
      {
	debugdump(matrix);
      }

    // If we have invalid entries, we're just going to throw them out,
    // forget they even existed..  this is reasonable
    // because matrix is private to this class.. PermissionMatrixDBField
    // is responsible for maintaining the meaningful content of the permissions
    // entered into it, not the particulars of the matrix.

    // The permisison matrix bits generally become invalid after
    // schema editing.  Since normally the database/schema needs to be
    // dumped after changing the schema, this is an appropriate place
    // to do the cleanup.

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

  /**
   * <p>This method is used when the database is being dumped, to write
   * out this field to disk.  It is mated with receiveXML().</p>
   */

  synchronized void emitXML(XMLDumpContext xmlOut) throws IOException
  {
    Enumeration enum, enum2;
    String key;
    PermEntry entry;
    String basename;
    Hashtable baseHash = new Hashtable();
    Hashtable innerTable;

    /* -- */

    // build up a hashtable structure so we get all the permission
    // entries grouped by base.

    enum = matrix.keys();

    while (enum.hasMoreElements())
      {
	key = (String) enum.nextElement();

	entry = (PermEntry) matrix.get(key);

	basename = decodeBaseName(key);

	if (baseHash.containsKey(basename))
	  {
	    innerTable = (Hashtable) baseHash.get(basename);
	  }
	else
	  {
	    innerTable = new Hashtable();
	    baseHash.put(basename, innerTable);
	  }

	innerTable.put(decodeFieldName(key), entry);
      }

    xmlOut.startElementIndent(this.getXMLName());
    xmlOut.indentOut();

    xmlOut.startElementIndent("permissions");
    xmlOut.indentOut();

    enum = baseHash.keys();

    while (enum.hasMoreElements())
      {
	key = (String) enum.nextElement();

	innerTable = (Hashtable) baseHash.get(key);
	entry = (PermEntry) innerTable.get("[base]");

	xmlOut.startElementIndent(arlut.csd.Util.XMLUtils.XMLEncode(key));
	xmlOut.indentOut();

	if (entry != null)
	  {
	    xmlOut.attribute("perm", entry.getXMLCode());
	  }

	enum2 = innerTable.keys();

	while (enum2.hasMoreElements())
	  {
	    String fieldKey = (String) enum2.nextElement();

	    if (fieldKey.equals("[base]"))
	      {
		continue;	// we've already wrote perms for the base
	      }

	    PermEntry fieldEntry = (PermEntry) innerTable.get(fieldKey);

	    xmlOut.startElementIndent(arlut.csd.Util.XMLUtils.XMLEncode(fieldKey));
	    xmlOut.attribute("perm", fieldEntry.getXMLCode());
	    xmlOut.endElement(arlut.csd.Util.XMLUtils.XMLEncode(fieldKey));
	  }

	xmlOut.indentIn();
	xmlOut.endElementIndent(arlut.csd.Util.XMLUtils.XMLEncode(key));
      }

    xmlOut.indentIn();
    xmlOut.endElementIndent("permissions");

    xmlOut.indentIn();
    xmlOut.endElementIndent(this.getXMLName());
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
   * <P>Returns a String representing the change in value between this
   * field and orig.  This String is intended for logging and email,
   * not for any sort of programmatic activity.  The format of the
   * generated string is not defined, but is intended to be suitable
   * for inclusion in a log entry and in an email message.</P>
   *
   * <P>If there is no change in the field, null will be returned.</P>
   */

  public String getDiffString(DBField orig)
  {
    StringBuffer result = new StringBuffer();
    PermissionMatrixDBField origP;

    /* -- */

    if (!(orig instanceof PermissionMatrixDBField))
      {
	throw new IllegalArgumentException("bad field comparison");
      }

    clean();

    origP = (PermissionMatrixDBField) orig;

    if (origP.equals(this))
      {
	return null;
      }
    
    Vector myKeys = new Vector();
    Vector origKeys = new Vector();

    Enumeration enum = matrix.keys();

    while (enum.hasMoreElements())
      {
	myKeys.addElement(enum.nextElement());
      }

    enum = origP.matrix.keys();

    while (enum.hasMoreElements())
      {
	origKeys.addElement(enum.nextElement());
      }

    PermEntry entryA = null;
    PermEntry entryB = null;

    Vector keptKeys = arlut.csd.Util.VectorUtils.intersection(myKeys, origKeys);
    Vector newKeys = arlut.csd.Util.VectorUtils.difference(myKeys, keptKeys);
    Vector lostKeys = arlut.csd.Util.VectorUtils.difference(origKeys, keptKeys);

    for (int i = 0; i < keptKeys.size(); i++)
      {
	String key = (String) keptKeys.elementAt(i);

	entryA = (PermEntry) matrix.get(key);
	entryB = (PermEntry) origP.matrix.get(key);

	if (!entryA.equals(entryB))
	  {
	    result.append(decodeBaseName(key) + " " + decodeFieldName(key) +
			  " -- " + entryA.difference(entryB));
	    result.append("\n");
	  }
      }

    for (int i = 0; i < newKeys.size(); i++)
      {
	String key = (String) newKeys.elementAt(i);

	entryA = (PermEntry) matrix.get(key);

	if (isBasePerm(key))
	  {	
	    result.append(decodeBaseName(key) + " " + decodeFieldName(key) +
			  " -- " + entryA.difference(null));
	    result.append("\n");
	  }
	else
	  {
	    result.append(decodeBaseName(key) + " " + decodeFieldName(key) +
			  " -- " + entryA.difference(null) + " (was undefined)");
	    result.append("\n");
	  }
      }

    for (int i = 0; i < lostKeys.size(); i++)
      {
	String key = (String) lostKeys.elementAt(i);

	entryB = (PermEntry) origP.matrix.get(key);

	result.append(decodeBaseName(key) + " " + decodeFieldName(key) + 
		      " -- Lost " + entryB);
	result.append("\n");
      }

    return result.toString();
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
   * <P>Return a serializable, read-only copy of the maximum permissions
   * that can be set for this field's permission matrix.  This matrix
   * is drawn from the union of delegatable roles that the client's
   * adminPersona is a member of.</P>
   * 
   * <P>This method will return null if this perm_field is not associated
   * with an object that is being edited, or if the client is logged
   * into the server as supergash.</P>
   * 
   * @see arlut.csd.ganymede.perm_field
   */

  public PermMatrix getTemplateMatrix()
  {
    if (!(owner instanceof DBEditObject))
      {
	return null;
      }

    if (owner.gSession.isSuperGash())
      {
	return null;
      }

    if (getID() == SchemaConstants.RoleMatrix)
      {
	return owner.gSession.delegatablePersonaPerms;
      }

    if (getID() == SchemaConstants.RoleDefaultMatrix)
      {
	return owner.gSession.delegatableDefaultPerms;
      }

    return null;
  }

  /**
   * Returns a PermEntry object representing this 
   * {@link arlut.csd.ganymede.PermMatrix PermMatrix}'s 
   * permissions on the field &lt;fieldID&gt; in base &lt;baseID&gt;
   *
   * @see arlut.csd.ganymede.perm_field
   * @see arlut.csd.ganymede.PermMatrix
   */

  public PermEntry getPerm(short baseID, short fieldID)
  {
    return (PermEntry) matrix.get(matrixEntry(baseID, fieldID));
  }

  /**
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
   * Returns a PermEntry object representing this PermMatrix's 
   * permissions on the field &lt;field&gt; in base &lt;base&gt;
   *
   * @see arlut.csd.ganymede.perm_field
   * @see arlut.csd.ganymede.PermMatrix
   */

  public PermEntry getPerm(Base base, BaseField field)
  {
    try
      {
	return (PermEntry) matrix.get(matrixEntry(base.getTypeID(), 
						  field.getID()));
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("caught remote: " + ex);
      }
  }

  /**
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
   * <P>Resets the permissions in this PermissionMatrixDBField to
   * the empty set.  Used by non-interactive clients to reset
   * the Permission Matrix to a known state before setting
   * permissions.</P>
   *
   * <P>Returns null on success, or a failure-coded ReturnVal
   * on permissions failure.</P>
   */

  public ReturnVal resetPerms()
  {
    if (isEditable())
      {
	matrix.clear();
	matrix = new Hashtable();
	return null;
      }
    else
      {
	return Ganymede.createErrorDialog("Permissions failure",
					  "You don't have permissions to reset " + toString() +
					  "'s permission matrix.");
      }
  }

  /**
   * <P>Sets the permission entry for base &lt;base&gt;,
   * field &lt;field&gt; to PermEntry &lt;entry&gt;</P>
   *
   * <P>This operation will fail if this
   * PermissionMatrixDBField is not editable.</P>
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
   * <P>Sets the permission entry for base &lt;baseID&gt;,
   * field &lt;fieldID&gt; to PermEntry &lt;entry&gt;.</P>
   *
   * <P>This operation will fail if this
   * PermissionMatrixDBField is not editable.</P>
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
   * <P>Sets the permission entry for base &lt;baseID&gt;
   * to PermEntry &lt;entry&gt;.</P>
   *
   * <P>This operation will fail if this
   * PermissionMatrixDBField is not editable.</P>
   *
   * @see arlut.csd.ganymede.perm_field
   * @see arlut.csd.ganymede.PermMatrix
   */

  public ReturnVal setPerm(Base base, PermEntry entry)
  {
    // no need for synchronization, since we call a synchronized
    // setPerm() call

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
   * <P>Sets the permission entry for this matrix for base &lt;baseID&gt;
   * to PermEntry &lt;entry&gt;</P>
   *
   * <P>This operation will fail if this
   * PermissionMatrixDBField is not editable.</P>
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
   * <P>Sets the permission entry for all fields in base &lt;baseID&gt;
   * to PermEntry &lt;entry&gt;</P>
   *
   * <P>This operation will fail if this
   * PermissionMatrixDBField is not editable.</P>
   *
   * @param includeBuiltins if true, this will set the permissions for the
   * built-in fields to entry as well as the custom fields.
   */

  public synchronized ReturnVal setFieldPerms(short baseID, PermEntry entry, boolean includeBuiltins)
  {
    if (!isEditable())
      {
	return Ganymede.createErrorDialog("Permissions Error",
					  "You don't have permissions to edit this field");
      }

    Vector fields = owner.getBase().getFields(includeBuiltins);

    for (int i = 0; i < fields.size(); i++)
      {
	short fieldID = ((DBObjectBaseField) fields.elementAt(i)).getID();

	if (allowablePermEntry(baseID, fieldID, entry))
	  {
	    matrix.put(matrixEntry(baseID, fieldID), entry);
	  }
      }

    defined = true;

    return null;
  }

  /**
   * <P>This internal method is used to cull out any entries in this
   * permissions field that are non-operative, either by referring to
   * an object/field type that does not exist, or by being redundant.</P>
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
  }

  /**
   * <P>Private method to generate a key for use in
   * our internal Hashtable, used to encode the 
   * permission for a given {@link arlut.csd.ganymede.DBObjectBase
   * DBObjectBase} and {@link arlut.csd.ganymede.DBObjectBaseField
   * DBObjectBaseField}.</P>
   */

  private String matrixEntry(short baseID, short fieldID)
  {
    return (baseID + ":" + fieldID);
  }

  /**
   * <P>Private method to generate a key for use in
   * our internal Hashtable, used to encode the
   * permission for a given {@link arlut.csd.ganymede.DBObjectBase
   * DBObjectBase}.</P>
   */
  
  private String matrixEntry(short baseID)
  {
    return (baseID + "::");
  }

  /**
   * <P>This method is used to basically dump state out of this field
   * so that the {@link arlut.csd.ganymede.DBEditSet DBEditSet}
   * {@link arlut.csd.ganymede.DBEditSet#checkpoint(java.lang.String) checkpoint()}
   * code can restore it later
   * if need be.</P>
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
   * <P>This method is used to basically force state into this field.</P>
   *
   * <P>It is used to place a value or set of values that were known to
   * be good during the current transaction back into this field,
   * without creating or changing this DBField's object identity.</P>
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
   * <P>This method is used to check that the given operation can be set by the
   * current administrator.</P>
   *
   * <P>If fieldID &lt; 0, entry will be checked against the administrator's
   * applicable base permissions.</P>
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
    
    if (getID() == SchemaConstants.RoleMatrix)
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
    else if (getID() == SchemaConstants.RoleDefaultMatrix)
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
	throw new RuntimeException("Error, don't recognize field id.. should be 'Owned Object Bits' or " +
				   "'Default Bits'.");
      }
  }

  /**
   *
   * This method does a dump to System.err of the permission
   * contents held in this field.
   *
   */

  public void debugdump()
  {
    debugdump(this);
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                               PermMatrixCkPoint

------------------------------------------------------------------------------*/

/**
 * <P>Helper class used to handle checkpointing of a 
 * {@link arlut.csd.ganymede.PermissionMatrixDBField PermissionMatrixDBField}'s
 * state.
 *
 * <P>See {@link arlut.csd.ganymede.PermissionMatrixDBField#checkpoint() 
 * PermissionMatrixDBField.checkpoint()} for more detail.</P>
 */

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
