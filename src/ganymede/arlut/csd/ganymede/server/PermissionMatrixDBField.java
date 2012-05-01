/*

   PermissionMatrixDBField.java

   This class defines the permission matrix field used in the
   'Role' DBObjectBase class.
   
   Created: 27 June 1997

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2012
   The University of Texas at Austin

   Ganymede is a registered trademark of The University of Texas at Austin

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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import arlut.csd.Util.TranslationService;
import arlut.csd.ganymede.common.PermEntry;
import arlut.csd.ganymede.common.PermMatrix;
import arlut.csd.ganymede.common.ReturnVal;
import arlut.csd.ganymede.common.SchemaConstants;
import arlut.csd.ganymede.rmi.Base;
import arlut.csd.ganymede.rmi.BaseField;
import arlut.csd.ganymede.rmi.perm_field;

/*------------------------------------------------------------------------------
                                                                           class
                                                         PermissionMatrixDBField

------------------------------------------------------------------------------*/

/** 
 * <p>PermissionMatrixDBField is a subclass of {@link
 * arlut.csd.ganymede.server.DBField DBField} for the storage and handling of
 * permission matrix fields (used only in the Role
 * {@link arlut.csd.ganymede.server.DBObject DBObjects}) in the
 * {@link arlut.csd.ganymede.server.DBStore DBStore} on the Ganymede
 * server.</p>
 *
 * <p>The Ganymede client talks to PermissionMatrixDBFields through the
 * {@link arlut.csd.ganymede.rmi.perm_field perm_field} RMI interface.</p>
 *
 * <p>This class differs a bit from most subclasses of {@link
 * arlut.csd.ganymede.server.DBField DBField} in that the normal setValue()/getValue()
 * methods are non-functional.  Instead, there are special methods used to set or
 * access permission information from the specially coded Hashtable held by
 * a PermissionMatrixDBField.  This Hashtable maps strings encoded by the
 * {@link arlut.csd.ganymede.server.PermissionMatrixDBField#matrixEntry(short, short)
 * matrixEntry()} methods to {@link arlut.csd.ganymede.common.PermEntry PermEntry}
 * objects, which hold create, edit, view, and delete bits.</p>
 *
 * <p>PermissionMatrixDBField's methods encode part of the server's permissions
 * logic, including the restrictions on what bits can be set in a Role's
 * permission matrix based on the rights granted in the client's
 * {@link arlut.csd.ganymede.server.GanymedeSession GanymedeSession}.  We determine
 * what GanymedeSession we are operating in for that case 
 * by asking our {@link arlut.csd.ganymede.server.DBEditObject DBEditObject} owner.</p>
 */

public class PermissionMatrixDBField extends DBField implements perm_field {

  static final boolean debug = false;

  /**
   * TranslationService object for handling string localization in the
   * Ganymede server.
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.server.PermissionMatrixDBField");

  // ---

  /**
   * Returns true if this field has a value associated
   * with it, or false if it is an unfilled 'placeholder'.
   *
   * @see arlut.csd.ganymede.rmi.db_field
   */

  public boolean isDefined()
  {
    return matrix.size() > 0;
  }

  /**
   * <p>This method is used to mark a field as undefined when it is
   * checked out for editing.  Different subclasses of {@link
   * arlut.csd.ganymede.server.DBField DBField} may implement this in
   * different ways, if simply setting the field's value member to
   * null is not appropriate.  Any namespace values claimed by the
   * field will be released, and when the transaction is committed,
   * this field will be released.</p>
   *
   * <p>Note that this method is really only intended for those fields
   * which have some significant internal structure to them, such as
   * permission matrix, field option matrix, and password fields.</p>
   *
   * <p>NOTE: There is, at present, no defined DBEditObject callback
   * method that tracks generic field nullification.  This means that
   * if your code uses setUndefined on a PermissionMatrixDBField,
   * FieldOptionDBField, or PasswordDBField, the plugin code is not
   * currently given the opportunity to review and refuse that
   * operation.  Caveat Coder.</p>
   */

  public synchronized ReturnVal setUndefined(boolean local)
  {
    if (isEditable(local))
      {
	matrix.clear();
	return null;
      }

    // "Don''t have permission to clear this permission matrix field\n{0}"
    return Ganymede.createErrorDialog(ts.l("setUndefined.error", getName()));
  }

  /**
   * This utility method extracts the 
   * {@link arlut.csd.ganymede.server.DBObjectBase DBObjectBase} name from a coded
   * permission entry held in a
   * {@link arlut.csd.ganymede.common.PermMatrix PermMatrix}/PermissionMatrixDBField
   * Matrix.
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
	    basenum = Short.valueOf(entry.substring(0, sepIndex)).shortValue();
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
   * This utility method extracts the 
   * {@link arlut.csd.ganymede.server.DBObjectBaseField DBObjectBaseField} name from a coded
   * permission entry held in a
   * {@link arlut.csd.ganymede.common.PermMatrix PermMatrix}/PermissionMatrixDBField
   * Matrix.
   */

  public static String decodeFieldName(String entry)
  {
    int sepIndex;
    short basenum;
    DBObjectBase base;
    
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
	    basenum = Short.valueOf(entry.substring(0, sepIndex)).shortValue();
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
			fieldnum = Short.valueOf(fieldId).shortValue();

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
   * Returns true if the given String encodes the identity of
   * a {@link arlut.csd.ganymede.server.DBObjectBase DBObjectBase} and
   * not a field within a DBObjectBase.
   */

  private boolean isBasePerm(String matrixEntry)
  {
    return (matrixEntry.indexOf("::") != -1);
  }

  /**
   * This method returns true if the given
   * {@link arlut.csd.ganymede.common.PermMatrix PermMatrix}/
   * PermissionMatrixDBField key refers to a currently valid 
   * {@link arlut.csd.ganymede.server.DBObjectBase DBObjectBase}/
   * {@link arlut.csd.ganymede.server.DBObjectBaseField DBObjectBaseField}
   * in the loaded schema.
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
	basenum = Short.valueOf(entry.substring(0, sepIndex)).shortValue();
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
		    fieldnum = Short.valueOf(fieldId).shortValue();
		    
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
   * This method does a dump to System.err of the permission
   * contents held in PermissionMatrixDBField me.
   */

  public void debugdump(PermissionMatrixDBField me)
  {
    debugdump(me.matrix);
  }

  static public void debugdump(PermMatrix matrix)
  {
    debugdump(matrix.getMatrix());
  }

  /**
   * This method does a dump to System.err of the permission
   * contents held in matrix.
   */

  static private void debugdump(Map<String, PermEntry> matrix)
  {
    System.err.println(debugdecode(matrix));
  }

  /**
   * This method generates a string version of the debugdump
   * output.
   */

  static public String debugdecode(Map<String, PermEntry> matrix)
  {
    StringBuilder result = new StringBuilder();
    Enumeration en;
    PermEntry entry;
    String basename;
    Hashtable<String, Vector<String>> baseHash = new Hashtable<String, Vector<String>>();
    Vector<String> vec;

    /* -- */

    result.append("PermMatrix DebugDump\n");

    for (String key: matrix.keySet())
      {
	entry = matrix.get(key);

	basename = decodeBaseName(key);

	if (baseHash.containsKey(basename))
	  {
	    vec = baseHash.get(basename);
	  }
	else
	  {
	    vec = new Vector<String>();
	    baseHash.put(basename, vec);
	  }

	vec.add(decodeFieldName(key) + " -- " + entry.toString());
      }

    for (String key: baseHash.keySet())
      {
	vec = baseHash.get(key);

	for (String val: vec)
	  {
	    result.append(key + ":" + val + "\n");
	  }

	result.append("\n");
      }

    return result.toString();
  }

  // ---

  Hashtable matrix;

  /* -- */

  /**
   * Receive constructor.  Used to create a PermissionMatrixDBField from a
   * {@link arlut.csd.ganymede.server.DBStore DBStore}/{@link arlut.csd.ganymede.server.DBJournal DBJournal}
   * DataInput stream.
   */

  PermissionMatrixDBField(DBObject owner, 
			  DataInput in,
			  DBObjectBaseField definition) throws IOException
  {
    value = null;
    this.owner = owner;
    this.fieldcode = definition.getID();
    receive(in, definition);
  }

  /**
   * <p>No-value constructor.  Allows the construction of a
   * 'non-initialized' field, for use where the 
   * {@link arlut.csd.ganymede.server.DBObjectBase DBObjectBase}
   * definition indicates that a given field may be present,
   * but for which no value has been stored in the 
   * {@link arlut.csd.ganymede.server.DBStore DBStore}.</p>
   *
   * <p>Used to provide the client a template for 'creating' this
   * field if so desired.</p>
   */

  PermissionMatrixDBField(DBObject owner, DBObjectBaseField definition)
  {
    this.owner = owner;
    this.fieldcode = definition.getID();
    
    matrix = new Hashtable();
    value = null;
  }

  /**
   * Copy constructor.
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

    this.fieldcode = field.getID();
    this.owner = owner;
    this.matrix = new Hashtable(field.matrix.size());

    Enumeration en = field.matrix.keys();

    while (en.hasMoreElements())
      {
	key = en.nextElement();

	entry = (PermEntry) field.matrix.get(key);

	if (debug)
	  {
	    System.err.println("PermissionMatrixDBField: copying " + key + ", contents: " + entry);
	  }

	this.matrix.put(key, entry);
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
   * We don't expect these fields to ever be stored in a hash.
   */

  public int hashCode()
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Fancy equals method really does check for value equality.
   */

  public synchronized boolean equals(Object obj)
  {
    PermissionMatrixDBField pmdb;
    Enumeration keys;
    String key;
    
    /* -- */

    if (obj == null)
      {
        return false;
      }

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
   * This method copies the current value of this DBField
   * to target.  The target DBField must be contained within a
   * checked-out DBEditObject in order to be updated.  Any actions
   * that would normally occur from a user manually setting a value
   * into the field will occur.
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
	    // "Copy Field Error"
	    // "Can''t copy field {0}, no read privileges."
	    return Ganymede.createErrorDialog(ts.l("copyFieldTo.error_subj"),
					      ts.l("copyFieldTo.no_read", getName()));
	  }
      }
	
    if (!target.isEditable(local))
      {
	// "Copy Field Error"
	// "Can''t copy field {0}, no write privileges."
	return Ganymede.createErrorDialog(ts.l("copyFieldTo.error_subj"),
					  ts.l("copyFieldTo.no_write", getName()));
      }

    if (!(target instanceof PermissionMatrixDBField))
      {
	// "Copy Field Error"
	// "Can''t copy field {0}, target is not a PermissionMatrixDBField"
	return Ganymede.createErrorDialog(ts.l("copyFieldTo.error_subj"),
					  ts.l("copyFieldTo.bad_param", getName()));
      }

    // doing a simple clone of the hashtable is okay, since both the
    // keys and values of the matrix are treated as immutable (they
    // are replaced, not changed in-place)

    ((PermissionMatrixDBField) target).matrix = (Hashtable) this.matrix.clone();

    return null;
  }

  /**
   * We don't really want to hash according to our permission
   * contents, so just hash according to our containing object's
   * i.d.
   */

  public Object key()
  {
    return Integer.valueOf(owner.getID());
  }

  /**
   * We always return null here..
   */

  public Object getValue()
  {
    return null;
  }

  /** 
   * <p>Returns an Object carrying the value held in this field.</p>
   *
   * <p>This is intended to be used within the Ganymede server, it bypasses
   * the permissions checking that getValues() does.</p>
   */

  public Object getValueLocal()
  {
    return null;
  }

  /**
   * <p>We don't allow setValue().</p>
   *
   * <p>PermissionMatrixDBField doesn't allow
   * direct setting of the entire matrix.. just use the get() and
   * set() methods below.</p>
   */

  public ReturnVal setValue(Object value, boolean local, boolean noWizards)
  {
    // "Server: Error in PermissionMatrixDBField.setValue()"
    // "Error.. can''t call setValue() on a PermissionMatrixDBField."
    return Ganymede.createErrorDialog(ts.l("setValue.error_subj"),
				      ts.l("setValue.error_text"));
  }

  public Object clone() throws CloneNotSupportedException
  {
    throw new CloneNotSupportedException();
  }

  synchronized void emit(DataOutput out) throws IOException
  {
    Enumeration keys;
    PermEntry pe;
    String key;

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

  synchronized void receive(DataInput in, DBObjectBaseField definition) throws IOException
  {
    int tableSize;
    PermEntry pe;
    String key;

    /* -- */

    tableSize = in.readInt();

    if (tableSize <= 0)
      {
	matrix = new Hashtable();
      }
    else
      {
	matrix = new Hashtable(tableSize);
      }
    
    for (int i = 0; i < tableSize; i++)
      {
	key = in.readUTF();
	pe = PermEntry.getPermEntry(in);
	matrix.put(key, pe);
      }
  }

  void emitXML(XMLDumpContext dump) throws IOException
  {
    this.emitXML(dump, true);
  }

  synchronized void emitXML(XMLDumpContext xmlOut, boolean writeSurroundContext) throws IOException
  {
    Enumeration en, enum2;
    String key;
    PermEntry entry;
    String basename;
    Hashtable baseHash = new Hashtable();
    Hashtable innerTable;

    /* -- */

    // build up a hashtable structure so we get all the permission
    // entries grouped by base.

    en = matrix.keys();

    while (en.hasMoreElements())
      {
	key = (String) en.nextElement();

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

    if (writeSurroundContext)
      {
	xmlOut.startElementIndent(this.getXMLName());
	xmlOut.indentOut();
      }

    xmlOut.startElementIndent("permissions");
    xmlOut.indentOut();

    en = baseHash.keys();

    while (en.hasMoreElements())
      {
	key = (String) en.nextElement();

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

    if (writeSurroundContext)
      {
	xmlOut.indentIn();
	xmlOut.endElementIndent(this.getXMLName());
      }
  }

  public synchronized String getValueString()
  {
    StringBuilder result = new StringBuilder();
    PermEntry entry = null;
    String key = null;

    /* -- */

    clean();

    Enumeration en = matrix.keys();

    while (en.hasMoreElements())
      {
	key = (String) en.nextElement();
	entry = (PermEntry) matrix.get(key);

	if (isBasePerm(key))
	  {	
	    result.append(decodeBaseName(key) + " " + decodeFieldName(key) +
			  " -- " + entry.difference(null));
	    result.append("\n");
	  }
	else
	  {
	    result.append(decodeBaseName(key) + " " + decodeFieldName(key) +
			  " -- " + entry.difference(null));
	    result.append("\n");
	  }
      }

    return result.toString();
  }

  /**
   * We don't try and give a comprehensive encoding string for permission
   * matrices, let's just give enough so they know what we are.
   */

  public String getEncodingString()
  {
    if (!verifyReadPermission())
      {
	throw new IllegalArgumentException("permission denied to read this field");
      }

    return "PermissionMatrix";
  }

  /**
   * <p>Returns a String representing the change in value between this
   * field and orig.  This String is intended for logging and email,
   * not for any sort of programmatic activity.  The format of the
   * generated string is not defined, but is intended to be suitable
   * for inclusion in a log entry and in an email message.</p>
   *
   * <p>If there is no change in the field, null will be returned.</p>
   */

  public String getDiffString(DBField orig)
  {
    StringBuilder result = new StringBuilder();
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

    Enumeration en = matrix.keys();

    while (en.hasMoreElements())
      {
	myKeys.addElement(en.nextElement());
      }

    en = origP.matrix.keys();

    while (en.hasMoreElements())
      {
	origKeys.addElement(en.nextElement());
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

	// "{0} {1} -- {2}\n"
	result.append(ts.l("getValueString.new_pattern",
			   decodeBaseName(key),
			   decodeFieldName(key),
			   entryA.difference(null)));
      }

    for (int i = 0; i < lostKeys.size(); i++)
      {
	String key = (String) lostKeys.elementAt(i);

	entryB = (PermEntry) origP.matrix.get(key);

	// "{0} {1} -- {2}\n"
	result.append(ts.l("getValueString.old_pattern",
			   decodeBaseName(key),
			   decodeFieldName(key),
			   entryB));
      }

    return result.toString();
  }

  /**
   * Return a serializable, read-only copy of this field's permission
   * matrix
   *
   * @see arlut.csd.ganymede.rmi.perm_field
   */

  public PermMatrix getMatrix()
  {
    return new PermMatrix(this.matrix);
  }

  /**
   * <p>Return a serializable, read-only copy of the maximum permissions
   * that can be set for this field's permission matrix.  This matrix
   * is drawn from the union of delegatable roles that the client's
   * adminPersona is a member of.</p>
   * 
   * <p>This method will return null if this perm_field is not associated
   * with an object that is being edited, or if the client is logged
   * into the server as supergash.</p>
   * 
   * @see arlut.csd.ganymede.rmi.perm_field
   */

  public PermMatrix getTemplateMatrix()
  {
    if (!(owner instanceof DBEditObject))
      {
	return null;
      }

    if (owner.gSession.getPermManager().isSuperGash())
      {
	return null;
      }

    if (getID() == SchemaConstants.RoleMatrix)
      {
	return owner.gSession.getPermManager().delegatablePersonaPerms;
      }

    if (getID() == SchemaConstants.RoleDefaultMatrix)
      {
	return owner.gSession.getPermManager().delegatableDefaultPerms;
      }

    return null;
  }

  /**
   * Returns a PermEntry object representing this 
   * {@link arlut.csd.ganymede.common.PermMatrix PermMatrix}'s 
   * permissions on the field &lt;fieldID&gt; in base &lt;baseID&gt;
   *
   * @see arlut.csd.ganymede.rmi.perm_field
   * @see arlut.csd.ganymede.common.PermMatrix
   */

  public PermEntry getPerm(short baseID, short fieldID)
  {
    return (PermEntry) matrix.get(matrixEntry(baseID, fieldID));
  }

  /**
   * Returns a PermEntry object representing this PermMatrix's 
   * permissions on the base &lt;baseID&gt;
   *
   * @see arlut.csd.ganymede.rmi.perm_field
   * @see arlut.csd.ganymede.common.PermMatrix
   */

  public PermEntry getPerm(short baseID)
  {
    return (PermEntry) matrix.get(matrixEntry(baseID));
  }

  /**
   * Returns a PermEntry object representing this PermMatrix's 
   * permissions on the field &lt;field&gt; in base &lt;base&gt;
   *
   * @see arlut.csd.ganymede.rmi.perm_field
   * @see arlut.csd.ganymede.common.PermMatrix
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
   * @see arlut.csd.ganymede.rmi.perm_field
   * @see arlut.csd.ganymede.common.PermMatrix
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
   * <p>Resets the permissions in this PermissionMatrixDBField to
   * the empty set.  Used by non-interactive clients to reset
   * the Permission Matrix to a known state before setting
   * permissions.</p>
   *
   * <p>Returns null on success, or a failure-coded ReturnVal
   * on permissions failure.</p>
   *
   * @see arlut.csd.ganymede.rmi.perm_field
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
	// "Permissions Failure"
	// "You don''t have permissions to reset {0}''s permission matrix."
	return Ganymede.createErrorDialog(ts.l("resetPerms.error_subj"),
					  ts.l("resetPerms.error_text", toString()));
      }
  }

  /**
   * <p>Sets the permission entry for base &lt;base&gt;,
   * field &lt;field&gt; to PermEntry &lt;entry&gt;</p>
   *
   * <p>This operation will fail if this
   * PermissionMatrixDBField is not editable.</p>
   *
   * @see arlut.csd.ganymede.rmi.perm_field
   * @see arlut.csd.ganymede.common.PermEntry
   */

  public ReturnVal setPerm(Base base, BaseField field, PermEntry entry)
  {
    try
      {
	return setPerm(base.getTypeID(), field.getID(), entry);
      }
    catch (RemoteException ex)
      {
	// "Couldn''t process setPerm(): {0}"
	return Ganymede.createErrorDialog(ts.l("setPerm.error_text", ex.getMessage()));
      }
  }

  /**
   * <p>Sets the permission entry for base &lt;baseID&gt;,
   * field &lt;fieldID&gt; to PermEntry &lt;entry&gt;.</p>
   *
   * <p>This operation will fail if this
   * PermissionMatrixDBField is not editable.</p>
   *
   * @param baseID the object type to set permissions for
   * @param fieldID the field to set permissions for.  If fieldID < 0,
   * the permission will be applied to the object as a whole rather
   * than any individual field within the object
   *
   * @see arlut.csd.ganymede.rmi.perm_field
   * @see arlut.csd.ganymede.common.PermEntry 
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

	    // "You can''t set privileges for base {0}, field {1}, that you yourself do not have."
	    return Ganymede.createErrorDialog(ts.l("setPerm.delegation_error", baseName, fieldName));
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

    return null;
  }

  /**
   * <p>Sets the permission entry for base &lt;baseID&gt;
   * to PermEntry &lt;entry&gt;.</p>
   *
   * <p>This operation will fail if this
   * PermissionMatrixDBField is not editable.</p>
   *
   * @see arlut.csd.ganymede.rmi.perm_field
   * @see arlut.csd.ganymede.common.PermMatrix
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
	// "Couldn''t process setPerm(): {0}"
	return Ganymede.createErrorDialog(ts.l("setPerm.error_text", ex.getMessage()));
      }
  }

  /**
   * <p>Sets the permission entry for this matrix for base &lt;baseID&gt;
   * to PermEntry &lt;entry&gt;</p>
   *
   * <p>This operation will fail if this
   * PermissionMatrixDBField is not editable.</p>
   *
   * @see arlut.csd.ganymede.rmi.perm_field
   * @see arlut.csd.ganymede.common.PermEntry
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

	    // "You can''t set privileges for base {0} that you yourself do not have."
	    return Ganymede.createErrorDialog(ts.l("setPerm.base_delegation_error", baseName));
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

    return null;
  }

  /**
   * This internal method is used to cull out any entries in this
   * permissions field that are non-operative, either by referring to
   * an object/field type that does not exist, or by being redundant.
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
   * Private method to generate a key for use in
   * our internal Hashtable, used to encode the 
   * permission for a given {@link arlut.csd.ganymede.server.DBObjectBase
   * DBObjectBase} and {@link arlut.csd.ganymede.server.DBObjectBaseField
   * DBObjectBaseField}.
   */

  private String matrixEntry(short baseID, short fieldID)
  {
    return (baseID + ":" + fieldID);
  }

  /**
   * Private method to generate a key for use in
   * our internal Hashtable, used to encode the
   * permission for a given {@link arlut.csd.ganymede.server.DBObjectBase
   * DBObjectBase}.
   */
  
  private String matrixEntry(short baseID)
  {
    return (baseID + "::");
  }

  /**
   * This method is used to basically dump state out of this field
   * so that the {@link arlut.csd.ganymede.server.DBEditSet DBEditSet}
   * {@link arlut.csd.ganymede.server.DBEditSet#checkpoint(java.lang.String) checkpoint()}
   * code can restore it later
   * if need be.
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
   * <p>This method is used to basically force state into this field.</p>
   *
   * <p>It is used to place a value or set of values that were known to
   * be good during the current transaction back into this field,
   * without creating or changing this DBField's object identity.</p>
   */

  public synchronized void rollback(Object oldval)
  {
    if (!(owner instanceof DBEditObject))
      {
	throw new RuntimeException("Invalid rollback on field " + 
				   getName() + ", not in an editable context");
      }

    if (oldval == null)
      {
	this.setUndefined(true);
        return;
      }

    if ((oldval instanceof PermMatrixCkPoint))
      {
        this.matrix = ((PermMatrixCkPoint) oldval).matrix;
        return;
      }

    throw new RuntimeException("Invalid rollback on field " + 
                               getName() + ", not a PermMatrixCkPoint");

  }

  /**
   * <p>This method is used to check that the given operation can be set by the
   * current administrator.</p>
   *
   * <p>If fieldID &lt; 0, entry will be checked against the
   * administrator's applicable object/base permissions, rather than
   * those for the field itself.</p>
   */

  public boolean allowablePermEntry(short baseID, short fieldID, PermEntry entry)
  {
    if (owner.gSession == null)
      {
	return false;
      }

    if (owner.gSession.getPermManager().isSuperGash())
      {
	return true;
      }

    if (entry == null)
      {
	throw new IllegalArgumentException("entry is null");
      }
    
    if (getID() == SchemaConstants.RoleMatrix)
      {
	if (owner.gSession.getPermManager().getPersonaPerms() == null)
	  {
	    return false;
	  }

	PermEntry adminPriv;

	if (fieldID < 0)
	  {
	    adminPriv = (PermEntry) owner.gSession.getPermManager().getDelegatablePersonaPerms().getPerm(baseID);
	  }
	else
	  {
	    adminPriv = (PermEntry) owner.gSession.getPermManager().getDelegatablePersonaPerms().getPerm(baseID, fieldID);
	  }

	// the adminPriv should have all the bits set that we are seeking to set

	return entry.equals(entry.intersection(adminPriv));
      }
    else if (getID() == SchemaConstants.RoleDefaultMatrix)
      {
	if (owner.gSession.getPermManager().getDefaultPerms() == null)
	  {
	    return false;
	  }

	PermEntry adminPriv;

	if (fieldID < 0)
	  {
	    adminPriv = (PermEntry) owner.gSession.getPermManager().getDelegatableDefaultPerms().getPerm(baseID);
	  }
	else
	  {
	    adminPriv = (PermEntry) owner.gSession.getPermManager().getDelegatableDefaultPerms().getPerm(baseID, fieldID);
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
   * This method does a dump to System.err of the permission
   * contents held in this field.
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
 * <p>Helper class used to handle checkpointing of a 
 * {@link arlut.csd.ganymede.server.PermissionMatrixDBField PermissionMatrixDBField}'s
 * state.</p>
 *
 * <p>See {@link arlut.csd.ganymede.server.PermissionMatrixDBField#checkpoint() 
 * PermissionMatrixDBField.checkpoint()} for more detail.</p>
 */

class PermMatrixCkPoint {

  Hashtable matrix;

  /* -- */

  public PermMatrixCkPoint(PermissionMatrixDBField field)
  {
    this.matrix = (Hashtable) field.matrix.clone();
  }
}
