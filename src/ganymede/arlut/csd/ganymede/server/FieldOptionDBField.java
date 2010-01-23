/*

   FieldOptionDBField.java

   This class defines the field option matrix field used to support
   the SyncRunner message queueing build system.  Each object and
   field type defined in the Ganymede schema can have an option value
   attached to it in this field type.  These option values are used to
   control how objects and fields are processed with respect to a
   specific SyncChannel.
   
   Created: 25 January 2005

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996-2010
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
   along with this program; if not, write to the Free Software
   Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
   02111-1307, USA

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
import arlut.csd.ganymede.common.FieldOptionMatrix;
import arlut.csd.ganymede.common.ReturnVal;
import arlut.csd.ganymede.common.SchemaConstants;
import arlut.csd.ganymede.rmi.Base;
import arlut.csd.ganymede.rmi.BaseField;
import arlut.csd.ganymede.rmi.field_option_field;

/*------------------------------------------------------------------------------
                                                                           class
                                                              FieldOptionDBField

------------------------------------------------------------------------------*/

/** 
 * FieldOptionDBField is a subclass of {@link
 * arlut.csd.ganymede.server.DBField DBField} for the storage and
 * handling of Ganymede metadata.  In particular, the
 * FieldOptionDBField is used to allow the association of option
 * strings with each {@link arlut.csd.ganymede.server.DBObjectBase
 * DBObjectBase} and each {@link
 * arlut.csd.ganymede.server.DBObjectBaseField DBObjectBaseField} on
 * the Ganymede server.
 *
 * The option strings held by this data type, for the purpose of the
 * Sync Channels, are "1", "2", and "3".  Where "1" corresponds to
 * "Never", "2" corresponds to "When Changed", and "3" corresponds to
 * "Always".
 *
 * The Ganymede client talks to FieldOptionDBFields through the {@link
 * arlut.csd.ganymede.rmi.field_option_field field_option_field} RMI
 * interface.
 *
 * This class differs a bit from most subclasses of {@link
 * arlut.csd.ganymede.server.DBField DBField} in that the normal
 * setValue()/getValue() methods are non-functional.  Instead, there
 * are special methods used to set or access field option information
 * from the specially coded Hashtable held by a FieldOptionDBField.
 * This Hashtable maps strings encoded by the {@link
 * arlut.csd.ganymede.server.FieldOptionDBField#matrixEntry(short,
 * short) matrixEntry()} methods to Strings.
 *
 * FieldOptionDBField is used to support the SyncRunner
 * mechanism for doing delta-based builds.
 */

public class FieldOptionDBField extends DBField implements field_option_field {

  static final boolean debug = false;

  /**
   * TranslationService object for handling string localization in the
   * Ganymede server.
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.server.FieldOptionDBField");

  // ---

  /**
   *
   * Returns true if this field has a value associated
   * with it, or false if it is an unfilled 'placeholder'.
   *
   * @see arlut.csd.ganymede.rmi.db_field
   *
   */

  public boolean isDefined()
  {
    return this.matrix.size() > 0;
  }

  /**
   * This method is used to mark a field as undefined when it is
   * checked out for editing.  Different subclasses of {@link
   * arlut.csd.ganymede.server.DBField DBField} may implement this in
   * different ways, if simply setting the field's value member to
   * null is not appropriate.  Any namespace values claimed by the
   * field will be released, and when the transaction is committed,
   * this field will be released.
   *
   * Note that this method is really only intended for those fields
   * which have some significant internal structure to them, such as
   * permission matrix, field option matrix, and password fields.
   *
   * NOTE: There is, at present, no defined DBEditObject callback
   * method that tracks generic field nullification.  This means that
   * if your code uses setUndefined on a PermissionMatrixDBField,
   * FieldOptionDBField, or PasswordDBField, the plugin code is not
   * currently given the opportunity to review and refuse that
   * operation.  Caveat Coder.
   */

  public synchronized ReturnVal setUndefined(boolean local)
  {
    if (isEditable(local))
      {
	matrix.clear();
	return null;
      }

    // "Field Option Field Error"
    // "Don''t have permission to clear field options field "{0}"."
    return Ganymede.createErrorDialog(ts.l("global.error_title"),
				      ts.l("setUndefined.error", getName()));
  }

  /**
   * This utility method extracts the {@link
   * arlut.csd.ganymede.server.DBObjectBase DBObjectBase} name from a
   * coded permission entry internally held in a FieldOptionDBField
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
   * field option entry key.
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

  public static boolean isBase(String matrixEntry)
  {
    return (matrixEntry.indexOf("::") != -1);
  }

  /**
   * This method returns true if the given FieldOptionDBField key
   * refers to a currently valid {@link
   * arlut.csd.ganymede.server.DBObjectBase DBObjectBase}/ {@link
   * arlut.csd.ganymede.server.DBObjectBaseField DBObjectBaseField} in
   * the loaded schema.
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
   *
   * This method does a dump to System.err of the field option
   * contents held in FieldOptionDBField me.
   *
   */

  static public void debugdump(FieldOptionDBField me)
  {
    debugdump(me.matrix);
  }

  static public void debugdump(FieldOptionMatrix matrix)
  {
    debugdump(matrix.matrix);
  }

  /**
   *
   * This method does a dump to System.err of the field option
   * contents held in matrix.
   *
   */

  static private void debugdump(Hashtable matrix)
  {
    System.err.println(debugdecode(matrix));
  }

  /**
   * This method generates a string version of the debugdump
   * output.
   */

  static public String debugdecode(Hashtable matrix)
  {
    StringBuffer result = new StringBuffer();
    Enumeration en;
    String key;
    String option;
    String basename;
    Hashtable baseHash = new Hashtable();
    Vector vec;

    /* -- */

    result.append("FieldOption DebugDump\n");

    en = matrix.keys();

    while (en.hasMoreElements())
      {
	key = (String) en.nextElement();

	option = (String) matrix.get(key);

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

	vec.addElement(decodeFieldName(key) + " -- " + option);
      }

    en = baseHash.keys();

    while (en.hasMoreElements())
      {
	key = (String) en.nextElement();

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
   * Receive constructor.  Used to create a FieldOptionDBField from a
   * {@link arlut.csd.ganymede.server.DBStore DBStore}/{@link arlut.csd.ganymede.server.DBJournal DBJournal}
   * DataInput stream.
   */

  FieldOptionDBField(DBObject owner, 
		     DataInput in,
		     DBObjectBaseField definition) throws IOException
  {
    value = null;
    this.owner = owner;
    this.fieldcode = definition.getID();
    receive(in, definition);
  }

  /**
   * No-value constructor.  Allows the construction of a
   * 'non-initialized' field, for use where the 
   * {@link arlut.csd.ganymede.server.DBObjectBase DBObjectBase}
   * definition indicates that a given field may be present,
   * but for which no value has been stored in the 
   * {@link arlut.csd.ganymede.server.DBStore DBStore}.
   *
   * Used to provide the client a template for 'creating' this
   * field if so desired.
   */

  FieldOptionDBField(DBObject owner, DBObjectBaseField definition)
  {
    this.owner = owner;
    this.fieldcode = definition.getID();
    
    matrix = new Hashtable();
    value = null;
  }

  /**
   *
   * Copy constructor.
   *
   */

  public FieldOptionDBField(DBObject owner, FieldOptionDBField field)
  {
    Object key;
    String option;

    /* -- */

    value = null;

    if (debug)
      {
	System.err.println("FieldOptionDBField: Copy constructor");
      }

    this.fieldcode = field.getID();
    this.owner = owner;
    this.matrix = new Hashtable(field.matrix.size());

    Enumeration en = field.matrix.keys();

    while (en.hasMoreElements())
      {
	key = en.nextElement();

	option = (String) field.matrix.get(key);

	if (debug)
	  {
	    System.err.println("FieldOptionDBField: copying " + key + ", contents: " + option);
	  }

	this.matrix.put(key, option);
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
    // "Field Option Field Error"
    // "Error.. verifyNewValue() method not supported on FieldOptionDBField."
    return Ganymede.createErrorDialog(ts.l("global.error_title"),
				      ts.l("verifyNewValue.error_text"));
  }

  /**
   * We don't expect these fields to ever be stored in a hash.
   */

  public int hashCode()
  {
    throw new UnsupportedOperationException();
  }

  /**
   *
   * fancy equals method really does check for value equality
   *
   */

  public synchronized boolean equals(Object obj)
  {
    FieldOptionDBField fodb;
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

    fodb = (FieldOptionDBField) obj;

    if (matrix.size() != fodb.matrix.size())
      {
	return false;
      }

    keys = matrix.keys();

    while (keys.hasMoreElements())
      {
	key = (String) keys.nextElement();

	try
	  {
	    if (!(matrix.get(key).equals(fodb.matrix.get(key))))
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
	    // "Error Copying Field Option Field"
	    // "Can''t copy field {0}, no read privileges on source."
	    return Ganymede.createErrorDialog(ts.l("copyFieldTo.error_subj"),
					      ts.l("copyFieldTo.no_read", getName()));
	  }
      }

    if (!target.isEditable(local))
      {
	// "Error Copying Field Option Field"
	// "Can''t copy field {0}, no write privileges on target."
	return Ganymede.createErrorDialog(ts.l("copyFieldTo.error_subj"),
					  ts.l("copyFieldTo.no_write", getName()));
      }

    if (!(target instanceof FieldOptionDBField))
      {
	// "Error Copying Field Option Field"
	// "Can''t copy field {0}, target is not a FieldOptionDBField."
	return Ganymede.createErrorDialog(ts.l("copyFieldTo.error_subj"),
					  ts.l("copyFieldTo.bad_param", getName()));
      }

    // doing a simple clone of the hashtable is okay, since both the
    // keys and values of the matrix are treated as immutable (they
    // are replaced, not changed in-place)

    ((FieldOptionDBField) target).matrix = (Hashtable) this.matrix.clone();

    return null;
  }

  /**
   *
   * we don't really want to hash according to our field option
   * contents, so just hash according to our containing object's
   * i.d.
   *
   */

  public Object key()
  {
    return Integer.valueOf(owner.getID());
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
   * we don't allow setValue.. FieldOptionDBField doesn't allow
   * direct setting of the entire matrix.. just use the get() and set()
   * methods below.
   *
   */

  public ReturnVal setValue(Object value, boolean local, boolean noWizards)
  {
    // "Field Option Field Error"
    // "Error.. setValue() on method not supported on FieldOptionDBField."
    return Ganymede.createErrorDialog(ts.l("global.error_title"),
				      ts.l("setValue.error_text"));
  }

  public Object clone() throws CloneNotSupportedException
  {
    throw new CloneNotSupportedException();
  }

  synchronized void emit(DataOutput out) throws IOException
  {
    Enumeration keys;
    String key;
    String option;

    /* -- */

    if (debug)
      {
	debugdump(matrix);
      }

    // If we have invalid entries, we're just going to throw them out,
    // forget they even existed..  this is reasonable because matrix
    // is private to this class.. FieldOptionDBField is responsible
    // for maintaining the meaningful content of the options entered
    // into it, not the particulars of the matrix.

    // The field option matrix strings generally become invalid after
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
	option = (String) matrix.get(key);

	out.writeUTF(key);
	out.writeUTF(option);
      }
  }

  synchronized void receive(DataInput in, DBObjectBaseField definition) throws IOException
  {
    int tableSize;
    String key;
    String option;

    /* -- */

    tableSize = in.readInt();

    if (tableSize <= 0)
      {
	matrix = new Hashtable();
      }
    else
      {
	matrix = new Hashtable(tableSize * 2 + 1);
      }
    
    for (int i = 0; i < tableSize; i++)
      {
	key = in.readUTF();
	option = in.readUTF().intern();
	matrix.put(key, option);
      }
  }

  /**
   * This method is used when the database is being dumped, to write
   * out this field to disk.  It is mated with receiveXML().
   */

  void emitXML(XMLDumpContext xmlOut) throws IOException
  {
    this.emitXML(xmlOut, true);
  }

  /**
   * This method is used when the database is being dumped, to write
   * out this field to disk.  It is mated with receiveXML().
   */

  synchronized void emitXML(XMLDumpContext xmlOut, boolean writeSurroundContext) throws IOException
  {
    Enumeration en, enum2;
    String key;
    String option;
    String basename;
    Hashtable baseHash = new Hashtable();
    Hashtable innerTable;

    /* -- */

    // build up a hashtable structure so we get all the field options
    // grouped by base.

    en = matrix.keys();

    while (en.hasMoreElements())
      {
	key = (String) en.nextElement();
	option = (String) matrix.get(key);

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

	innerTable.put(decodeFieldName(key), option);
      }

    if (writeSurroundContext)
      {
	xmlOut.startElementIndent(this.getXMLName());
	xmlOut.indentOut();
      }

    xmlOut.startElementIndent("options");
    xmlOut.indentOut();

    en = baseHash.keys();

    while (en.hasMoreElements())
      {
	key = (String) en.nextElement();

	innerTable = (Hashtable) baseHash.get(key);
	option = (String) innerTable.get("[base]");

	xmlOut.startElementIndent(arlut.csd.Util.XMLUtils.XMLEncode(key));
	xmlOut.indentOut();

	if (option != null)
	  {
	    xmlOut.attribute("option", option);
	  }

	enum2 = innerTable.keys();

	while (enum2.hasMoreElements())
	  {
	    String fieldKey = (String) enum2.nextElement();

	    if (fieldKey.equals("[base]"))
	      {
		continue;	// we've already wrote field options for the base
	      }

	    String fieldOption = (String) innerTable.get(fieldKey);

	    xmlOut.startElementIndent(arlut.csd.Util.XMLUtils.XMLEncode(fieldKey));
	    xmlOut.attribute("option", fieldOption);
	    xmlOut.endElement(arlut.csd.Util.XMLUtils.XMLEncode(fieldKey));
	  }

	xmlOut.indentIn();
	xmlOut.endElementIndent(arlut.csd.Util.XMLUtils.XMLEncode(key));
      }

    xmlOut.indentIn();
    xmlOut.endElementIndent("options");

    if (writeSurroundContext)
      {
	xmlOut.indentIn();
	xmlOut.endElementIndent(this.getXMLName());
      }
  }

  public synchronized String getValueString()
  {
    StringBuffer result = new StringBuffer();
    String key = null;
    String option = null;

    /* -- */

    clean();

    Enumeration en = matrix.keys();

    while (en.hasMoreElements())
      {
	key = (String) en.nextElement();
	option = (String) matrix.get(key);

	if (isBase(key))
	  {	
	    result.append(decodeBaseName(key) + " " + decodeFieldName(key) +
			  " -- " + option);
	    result.append("\n");
	  }
	else
	  {
	    result.append(decodeBaseName(key) + " " + decodeFieldName(key) +
			  " -- " + option);
	    result.append("\n");
	  }
      }

    return result.toString();
  }

  /**
   *
   * We don't try and give a comprehensive encoding string for field
   * option matrices, let's just give enough so they know what we are.
   *
   */

  public String getEncodingString()
  {
    if (!verifyReadPermission())
      {
	throw new IllegalArgumentException("permission denied to read this field");
      }

    return "FieldOptions";
  }

  /**
   * Returns a String representing the change in value between this
   * field and orig.  This String is intended for logging and email,
   * not for any sort of programmatic activity.  The format of the
   * generated string is not defined, but is intended to be suitable
   * for inclusion in a log entry and in an email message.
   *
   * If there is no change in the field, null will be returned.
   */

  public String getDiffString(DBField orig)
  {
    StringBuffer result = new StringBuffer();
    FieldOptionDBField origFO;

    /* -- */

    if (!(orig instanceof FieldOptionDBField))
      {
	throw new IllegalArgumentException("bad field comparison");
      }

    clean();

    origFO = (FieldOptionDBField) orig;

    if (origFO.equals(this))
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

    en = origFO.matrix.keys();

    while (en.hasMoreElements())
      {
	origKeys.addElement(en.nextElement());
      }

    String optionA = null;
    String optionB = null;

    Vector keptKeys = arlut.csd.Util.VectorUtils.intersection(myKeys, origKeys);
    Vector newKeys = arlut.csd.Util.VectorUtils.difference(myKeys, keptKeys);
    Vector lostKeys = arlut.csd.Util.VectorUtils.difference(origKeys, keptKeys);

    for (int i = 0; i < keptKeys.size(); i++)
      {
	String key = (String) keptKeys.elementAt(i);

	optionA = (String) matrix.get(key);
	optionB = (String) origFO.matrix.get(key);

	if (!optionA.equals(optionB))
	  {
	    // "{0} {1} -- "{2}", was "{3}"\n"
	    result.append(ts.l("getDiffString.changed_pattern",
			       decodeBaseName(key), decodeFieldName(key),
			       optionA, optionB));
	  }
      }

    for (int i = 0; i < newKeys.size(); i++)
      {
	String key = (String) newKeys.elementAt(i);

	optionA = (String) matrix.get(key);

	if (isBase(key))
	  {
	    // "{0} {1} -- {2}\n"
	    result.append(ts.l("getDiffString.new_pattern",
			       decodeBaseName(key), decodeFieldName(key), optionA));
	  }
	else
	  {
	    // "{0} {1} -- {2} (was undefined)\n"
	    result.append(ts.l("getDiffString.new_pattern2",
			       decodeBaseName(key), decodeFieldName(key), optionA));
	  }
      }

    for (int i = 0; i < lostKeys.size(); i++)
      {
	String key = (String) lostKeys.elementAt(i);

	optionB = (String) origFO.matrix.get(key);

	// "{0} {1} -- Lost {2}\n"
	result.append(ts.l("getDiffString.old_pattern", decodeBaseName(key), decodeFieldName(key), optionB));
      }

    return result.toString();
  }

  /**
   *
   * Return a serializable, read-only copy of this field's field
   * option matrix
   *
   * @see arlut.csd.ganymede.rmi.field_option_field
   *
   */

  public FieldOptionMatrix getMatrix()
  {
    return new FieldOptionMatrix(this.matrix);
  }

  /**
   * Returns the option string, if any, for the given
   * base and field.
   *
   * @see arlut.csd.ganymede.rmi.field_option_field
   */

  public String getOption(short baseID, short fieldID)
  {
    return (String) matrix.get(matrixEntry(baseID, fieldID));
  }

  /**
   * Returns the option string, if any, for the given
   * base.
   *
   * @see arlut.csd.ganymede.rmi.field_option_field
   */

  public String getOption(short baseID)
  {
    return (String) matrix.get(matrixEntry(baseID));
  }

  /**
   * Returns the option string, if any, for the given
   * base and field.
   *
   * @see arlut.csd.ganymede.rmi.field_option_field
   */

  public String getOption(Base base, BaseField field)
  {
    try
      {
	return (String) matrix.get(matrixEntry(base.getTypeID(), 
					       field.getID()));
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("caught remote: " + ex);
      }
  }

  /*
   * Returns the option string, if any, for the given
   * base.
   *
   * @see arlut.csd.ganymede.rmi.field_option_field
   */

  public String getOption(Base base)
  {
    try
      {
	return (String) matrix.get(matrixEntry(base.getTypeID()));
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("caught remote: " + ex);
      }
  }

  /**
   * Resets the options in this FieldOptionDBField to the empty
   * set.  Used by non-interactive clients to reset the Field Option
   * to a known state before setting field options.
   *
   * Returns null on success, or a failure-coded ReturnVal
   * on permissions failure.
   */

  public ReturnVal resetOptions()
  {
    if (isEditable())
      {
	matrix.clear();
	matrix = new Hashtable();
	return null;
      }
    else
      {
	// "Field Option Field Error"
	// "You don''t have permissions to reset {0}''s field options."
	return Ganymede.createErrorDialog(ts.l("global.error_title"),
					  ts.l("resetOptions.error_text", toString()));
      }
  }

  /**
   * Sets the option String for base &lt;base&gt;,
   * field &lt;field&gt; to &lt;option&gt;
   *
   * This operation will fail if this
   * FieldOptionDBField is not editable.
   *
   * @see arlut.csd.ganymede.rmi.field_option_field
   */

  public ReturnVal setOption(Base base, BaseField field, String option)
  {
    try
      {
	return setOption(base.getTypeID(), field.getID(), option);
      }
    catch (RemoteException ex)
      {
	// "FieldOptionDBField couldn''t process setOption()"
	return Ganymede.createErrorDialog(ts.l("setOption.error_title"),
					  ex.getMessage());
      }
  }

  /**
   * Sets the option string for base &lt;baseID&gt;,
   * field &lt;fieldID&gt; to String &lt;option&gt;.
   *
   * This operation will fail if this
   * FieldOptionDBField is not editable.
   *
   * @param baseID the object type to set the option string for
   * @param fieldID the field to set the option string for.  If
   * fieldID < 0, the option will be applied to the object as a whole
   * rather than any individual field within the object
   *
   * @see arlut.csd.ganymede.rmi.field_option_field
   */

  public synchronized ReturnVal setOption(short baseID, short fieldID, String option)
  {
    if (!isEditable())
      {
	// "Field Option Permissions Error"
	// "You don''t have permission to edit this field."
	return Ganymede.createErrorDialog(ts.l("global.perm_error_title"),
					  ts.l("global.perm_error_text"));
      }

    if (option == null || option.equals(""))
      {
	matrix.remove(matrixEntry(baseID, fieldID));

	if (debug)
	  {
	    System.err.println("FieldOptionDBField: base " + 
			       baseID + ", field " + fieldID + " cleared.");
	  }
      }
    else
      {
	matrix.put(matrixEntry(baseID, fieldID), option.intern());

	if (debug)
	  {
	    System.err.println("FieldOptionDBField: base " + 
			       baseID + ", field " + fieldID + " set to \"" + option + "\"");
	  }
      }

    return null;
  }

  /**
   * Sets the option for base &lt;baseID&gt;
   * to String &lt;option&gt;.
   *
   * This operation will fail if this
   * FieldOptionDBField is not editable.
   *
   * @see arlut.csd.ganymede.rmi.field_option_field
   */

  public ReturnVal setOption(Base base, String option)
  {
    // no need for synchronization, since we call a synchronized
    // setOption() call

    try
      {
	return setOption(base.getTypeID(), option);
      }
    catch (RemoteException ex)
      {
	// "FieldOptionDBField couldn''t process setOption()."
	return Ganymede.createErrorDialog(ts.l("setOption.error_title"), ex.getMessage());
      }
  }

  /**
   * Sets the option for base &lt;baseID&gt;
   * to Stringu &lt;option&gt;
   *
   * This operation will fail if this
   * FieldOptionDBField is not editable.
   *
   * @see arlut.csd.ganymede.rmi.field_option_field
   */

  public synchronized ReturnVal setOption(short baseID, String option)
  {
    if (!isEditable())
      {
	// "Field Option Permissions Error"
	// "You don''t have permission to edit this field."
	return Ganymede.createErrorDialog(ts.l("global.perm_error_title"),
					  ts.l("global.perm_error_text"));
      }

    if (option == null || option.equals(""))
      {
	matrix.remove(matrixEntry(baseID));

	if (debug)
	  {
	    System.err.println("FieldOptionDBField: base " + 
			       baseID + " cleared.");
	  }
      }
    else
      {
	matrix.put(matrixEntry(baseID), option.intern());
	
	if (debug)
	  {
	    System.err.println("FieldOptionDBField: base " + 
			       baseID + " set to \"" + option + "\"");
	  }
      }

    return null;
  }

  /**
   * This internal method is used to cull out any entries in this
   * field options field that are non-operative, either by referring
   * to an object/field type that does not exist, or by being
   * redundant.
   */

  private void clean()
  {
    Enumeration keys;
    String key;
    String option;

    /* -- */

    keys = matrix.keys();

    while (keys.hasMoreElements())
      {
	key = (String) keys.nextElement();
	option = (String) matrix.get(key);

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
		System.err.println("**** FieldOptionDBField.clean(): throwing out invalid entry " + 
				   decodeBaseName(key) + " " + 
				   decodeFieldName(key) + " ---- " + 
				   option);
	      }
	  }
      }
  }

  /**
   * Method to generate a key for use in our internal
   * Hashtable, used to encode the option for a given {@link
   * arlut.csd.ganymede.server.DBObjectBase DBObjectBase} and {@link
   * arlut.csd.ganymede.server.DBObjectBaseField
   * DBObjectBaseField}.
   */

  public static String matrixEntry(short baseID, short fieldID)
  {
    return (baseID + ":" + fieldID);
  }

  /**
   * Method to generate a key for use in our internal
   * Hashtable, used to encode the option for a given {@link
   * arlut.csd.ganymede.server.DBObjectBase DBObjectBase}.
   */
  
  public static String matrixEntry(short baseID)
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
	return new FieldOptionMatrixCkPoint(this);
      }
    else
      {
	return null;
      }
  }

  /**
   * This method is used to basically force state into this field.
   *
   * It is used to place a value or set of values that were known to
   * be good during the current transaction back into this field,
   * without creating or changing this DBField's object identity.
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

    if (oldval instanceof FieldOptionMatrixCkPoint)
      {
	this.matrix = ((FieldOptionMatrixCkPoint) oldval).matrix;
        return;
      }

    throw new RuntimeException("Invalid rollback on field " + 
                               getName() + ", not a FieldOptionMatrixCkPoint");
  }

  /**
   *
   * This method does a dump to System.err of the field option
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
                                                        FieldOptionMatrixCkPoint

------------------------------------------------------------------------------*/

/**
 * Helper class used to handle checkpointing of a 
 * {@link arlut.csd.ganymede.server.FieldOptionDBField FieldOptionDBField}'s
 * state.
 *
 * See {@link arlut.csd.ganymede.server.FieldOptionDBField#checkpoint() 
 * FieldOptionDBField.checkpoint()} for more detail.
 */

class FieldOptionMatrixCkPoint {

  Hashtable matrix;

  /* -- */

  public FieldOptionMatrixCkPoint(FieldOptionDBField field)
  {
    this.matrix = (Hashtable) field.matrix.clone();
  }
}
