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
   along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/

package arlut.csd.ganymede.server;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import arlut.csd.Util.TranslationService;
import arlut.csd.ganymede.common.FieldOptionMatrix;
import arlut.csd.ganymede.common.ReturnVal;
import arlut.csd.ganymede.common.SchemaConstants;
import arlut.csd.ganymede.common.SyncPrefEnum;
import arlut.csd.ganymede.rmi.Base;
import arlut.csd.ganymede.rmi.BaseField;
import arlut.csd.ganymede.rmi.field_option_field;

/*------------------------------------------------------------------------------
                                                                           class
                                                              FieldOptionDBField

------------------------------------------------------------------------------*/

/** 
 * <p>FieldOptionDBField is a subclass of {@link
 * arlut.csd.ganymede.server.DBField DBField} for the storage and
 * handling of Ganymede metadata.  In particular, the
 * FieldOptionDBField is used to allow the association of {@link
 * arlut.csd.ganymede.common.SyncPrefEnum SyncPrefEnum} values with
 * each {@link arlut.csd.ganymede.server.DBObjectBase DBObjectBase}
 * and each {@link arlut.csd.ganymede.server.DBObjectBaseField
 * DBObjectBaseField} on the Ganymede server.</p>
 *
 * <p>The Ganymede client talks to FieldOptionDBFields through the {@link
 * arlut.csd.ganymede.rmi.field_option_field field_option_field} RMI
 * interface.</p>
 *
 * <p>This class differs a bit from most subclasses of {@link
 * arlut.csd.ganymede.server.DBField DBField} in that the normal
 * setValue()/getValue() methods are non-functional.  Instead, there
 * are special methods used to set or access field option information
 * from the specially coded Map held by a FieldOptionDBField.
 * This Map maps strings encoded by the {@link
 * arlut.csd.ganymede.server.FieldOptionDBField#matrixEntry(short,
 * short) matrixEntry()} methods to Strings.</p>
 *
 * <p>FieldOptionDBField is used to support the SyncRunner
 * mechanism for doing delta-based builds.</p>
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
   * Returns true if this field has a value associated
   * with it, or false if it is an unfilled 'placeholder'.
   *
   * @see arlut.csd.ganymede.rmi.db_field
   */

  public boolean isDefined()
  {
    return this.matrix.size() > 0;
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
   * This method does a dump to System.err of the field option
   * contents held in FieldOptionDBField me.
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
   * This method does a dump to System.err of the field option
   * contents held in matrix.
   */

  static private void debugdump(Map<String, SyncPrefEnum> matrix)
  {
    System.err.println(debugdecode(matrix));
  }

  /**
   * This method generates a string version of the debugdump
   * output.
   */

  static public String debugdecode(Map<String, SyncPrefEnum> matrix)
  {
    StringBuilder result = new StringBuilder();
    Map<String, List<String>> baseHash = new HashMap<String, List<String>>();
    List<String> list = null;

    /* -- */

    result.append("FieldOption DebugDump\n");

    for (Map.Entry<String, SyncPrefEnum> entry : matrix.entrySet())
      {
	String basename = decodeBaseName(entry.getKey());

	if (baseHash.containsKey(basename))
	  {
	    list = baseHash.get(basename);
	  }
	else
	  {
	    list = new ArrayList<String>();
	    baseHash.put(basename, list);
	  }

	list.add(decodeFieldName(entry.getKey()) + " -- " + entry.getValue());
      }

    for (Map.Entry<String, List<String>> entry : baseHash.entrySet())
      {
	for (int i = 0; i < entry.getValue().size(); i++)
	  {
	    result.append(entry.getKey() + ":" + entry.getValue().get(i) + "\n");
	  }

	result.append("\n");
      }

    return result.toString();
  }

  // ---

  Map<String, SyncPrefEnum> matrix;

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

  FieldOptionDBField(DBObject owner, DBObjectBaseField definition)
  {
    this.owner = owner;
    this.fieldcode = definition.getID();
    
    matrix = new HashMap<String, SyncPrefEnum>();
    value = null;
  }

  /**
   * Copy constructor.
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

    // strings are immutable, so we can safely copy the matrix

    synchronized (field.matrix)
      {
	this.matrix = new HashMap<String, SyncPrefEnum>(field.matrix);
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
   * fancy equals method really does check for value equality
   */

  public boolean equals(Object obj)
  {
    FieldOptionDBField fodb;
    
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

    synchronized (fodb.matrix)
      {
	return this.matrix.equals(fodb.matrix); 
      }
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

    ((FieldOptionDBField) target).matrix = new HashMap<String, SyncPrefEnum>(this.matrix);

    return null;
  }

  /**
   * We don't really want to hash according to our field option
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
   * We don't allow setValue.. FieldOptionDBField doesn't allow
   * direct setting of the entire matrix.. just use the get() and set()
   * methods below.
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

    for (Map.Entry<String, SyncPrefEnum> entry : matrix.entrySet())
      {
	out.writeUTF(entry.getKey());
	out.writeUTF(entry.getValue().str());
      }
  }

  synchronized void receive(DataInput in, DBObjectBaseField definition) throws IOException
  {
    int tableSize;

    /* -- */

    tableSize = in.readInt();

    if (tableSize <= 0)
      {
	matrix = new HashMap<String, SyncPrefEnum>();
      }
    else
      {
	matrix = new HashMap<String, SyncPrefEnum>(tableSize * 2 + 1);
      }
    
    for (int i = 0; i < tableSize; i++)
      {
	matrix.put(in.readUTF(), SyncPrefEnum.find(in.readUTF()));
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
    Map<String, Map<String, SyncPrefEnum>> baseHash = new HashMap<String, Map<String, SyncPrefEnum>>();
    Map<String, SyncPrefEnum> innerTable;

    /* -- */

    // build up a hashtable structure so we get all the field options
    // grouped by base.

    for (Map.Entry<String, SyncPrefEnum> entry: matrix.entrySet())
      {
	String basename = decodeBaseName(entry.getKey());
	String fieldname = decodeFieldName(entry.getKey());

	if (baseHash.containsKey(basename))
	  {
	    innerTable = baseHash.get(basename);
	  }
	else
	  {
	    innerTable = new HashMap<String, SyncPrefEnum>();
	    baseHash.put(basename, innerTable);
	  }

	innerTable.put(fieldname, entry.getValue());
      }

    if (writeSurroundContext)
      {
	xmlOut.startElementIndent(this.getXMLName());
	xmlOut.indentOut();
      }

    xmlOut.startElementIndent("options");
    xmlOut.indentOut();

    for (Map.Entry<String, Map<String, SyncPrefEnum>> entry: baseHash.entrySet())
      {
	innerTable = entry.getValue();

	String outerElementName = arlut.csd.Util.XMLUtils.XMLEncode(entry.getKey());

	xmlOut.startElementIndent(outerElementName);
	xmlOut.indentOut();

	if (innerTable.containsKey("[base]"))
	  {
	    xmlOut.attribute("option", innerTable.get("[base]").toString());
	  }

	for (Map.Entry<String, SyncPrefEnum> innerEntry: innerTable.entrySet())
	  {
	    if (innerEntry.getKey().equals("[base]"))
	      {
		continue;	// we've already written field options for the base
	      }

	    String elementName = arlut.csd.Util.XMLUtils.XMLEncode(innerEntry.getKey());

	    xmlOut.startElementIndent(elementName);
	    xmlOut.attribute("option", innerEntry.getValue().str());
	    xmlOut.endElement(elementName);
	  }

	xmlOut.indentIn();
	xmlOut.endElementIndent(outerElementName);
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
    StringBuilder result = new StringBuilder();

    /* -- */

    clean();

    for (Map.Entry<String, SyncPrefEnum> entry: matrix.entrySet())
      {
	result.append(decodeBaseName(entry.getKey()) + " " + decodeFieldName(entry.getKey()) +
		      " -- " + entry.getValue());
	result.append("\n");
      }

    return result.toString();
  }

  /**
   * We don't try and give a comprehensive encoding string for field
   * option matrices, let's just give enough so they know what we are.
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
    
    Set<String> myKeys = new HashSet<String>(matrix.keySet());
    Set<String> newKeys = new HashSet<String>(myKeys);

    Set<String> origKeys = new HashSet<String>(origFO.matrix.keySet());
    Set<String> keptKeys = new HashSet<String>(origKeys);
    Set<String> lostKeys = new HashSet<String>(origKeys);

    keptKeys.retainAll(myKeys);
    newKeys.removeAll(keptKeys);
    lostKeys.removeAll(keptKeys);

    for (String key : keptKeys)
      {
	SyncPrefEnum optionA = matrix.get(key);
	SyncPrefEnum optionB = origFO.matrix.get(key);

	if (optionA != optionB)
	  {
	    // "\t{0} {1} -- "{2}", was "{3}"\n"
	    result.append(ts.l("getDiffString.changed_pattern",
			       decodeBaseName(key), decodeFieldName(key),
			       optionA, optionB));
	  }
      }

    for (String key : newKeys)
      {
	SyncPrefEnum optionA = matrix.get(key);

	if (isBase(key))
	  {
	    // "\t{0} {1} -- {2}\n"
	    result.append(ts.l("getDiffString.new_pattern",
			       decodeBaseName(key), decodeFieldName(key), optionA));
	  }
	else
	  {
	    // "\t{0} {1} -- {2} (was undefined)\n"
	    result.append(ts.l("getDiffString.new_pattern2",
			       decodeBaseName(key), decodeFieldName(key), optionA));
	  }
      }

    for (String key : lostKeys)
      {
	SyncPrefEnum optionB = origFO.matrix.get(key);

	// "\t{0} {1} -- Lost {2}\n"
	result.append(ts.l("getDiffString.old_pattern", decodeBaseName(key), decodeFieldName(key), optionB));
      }

    return result.toString();
  }

  /**
   * Return a serializable, read-only copy of this field's field
   * option matrix
   *
   * @see arlut.csd.ganymede.rmi.field_option_field
   */

  public FieldOptionMatrix getMatrix()
  {
    return new FieldOptionMatrix(this.matrix);
  }

  /**
   * <p>Returns a SyncPrefEnum representing this field option field's
   * option on the field &lt;field&gt; in base
   * &lt;base&gt;<br><br></p>
   *
   * @see arlut.csd.ganymede.rmi.field_option_field
   */

  public SyncPrefEnum getOption(short baseID, short fieldID)
  {
    return matrix.get(matrixEntry(baseID, fieldID));
  }

  /**
   * <p>Returns a SyncPrefEnum representing this field option field's 
   * option on the base &lt;base&gt;</p>
   *
   * @see arlut.csd.ganymede.rmi.field_option_field
   */

  public SyncPrefEnum getOption(short baseID)
  {
    return matrix.get(matrixEntry(baseID));
  }

  /**
   * <p>Returns a SyncPrefEnum representing this field option field's 
   * option on the field &lt;field&gt; in base &lt;base&gt;</p>
   *
   * @see arlut.csd.ganymede.rmi.field_option_field
   */

  public SyncPrefEnum getOption(Base base, BaseField field)
  {
    try
      {
	return matrix.get(matrixEntry(base.getTypeID(), field.getID()));
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("caught remote: " + ex);
      }
  }

  /**
   * <p>Returns a SyncPrefEnum object representing this field option field's 
   * option on the base &lt;base&gt;</p>
   *
   * @see arlut.csd.ganymede.rmi.field_option_field
   */

  public SyncPrefEnum getOption(Base base)
  {
    try
      {
	return matrix.get(matrixEntry(base.getTypeID()));
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("caught remote: " + ex);
      }
  }

  /**
   * <p>Resets the options in this FieldOptionDBField to the empty
   * set.  Used by non-interactive clients to reset the Field Option
   * to a known state before setting field options.</p>
   *
   * <p>Returns null on success, or a failure-coded ReturnVal
   * on permissions failure.</p>
   */

  public ReturnVal resetOptions()
  {
    if (isEditable())
      {
	matrix.clear();
	matrix = new HashMap<String, SyncPrefEnum>();
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
   * <p>Sets the SyncPrefEnum for this matrix for base &lt;baseID&gt;,
   * field &lt;fieldID&gt; to &lt;option&gt;.</p>
   *
   * <p>This operation will fail if this field option field is not
   * associated with a currently checked-out-for-editing
   * {@link arlut.csd.ganymede.server.FieldOptionDBField FieldOptionDBField}.</p>
   *
   * @see arlut.csd.ganymede.rmi.field_option_field
   */

  public ReturnVal setOption(Base base, BaseField field, SyncPrefEnum option)
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
   * <p>Sets the SyncPrefEnum for this matrix for base &lt;baseID&gt;
   * to &lt;option&gt;</p>
   *
   * <p>This operation will fail if this field option field is not
   * associated with a currently checked-out-for-editing
   * {@link arlut.csd.ganymede.server.FieldOptionDBField FieldOptionDBField}.</p>
   *
   * @see arlut.csd.ganymede.rmi.field_option_field
   */

  public synchronized ReturnVal setOption(short baseID, short fieldID, SyncPrefEnum option)
  {
    if (!isEditable())
      {
	// "Field Option Permissions Error"
	// "You don''t have permission to edit this field."
	return Ganymede.createErrorDialog(ts.l("global.perm_error_title"),
					  ts.l("global.perm_error_text"));
      }

    if (option == null)
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
	matrix.put(matrixEntry(baseID, fieldID), option);

	if (debug)
	  {
	    System.err.println("FieldOptionDBField: base " + 
			       baseID + ", field " + fieldID + " set to \"" + option + "\"");
	  }
      }

    return null;
  }

  /**
   * <p>Sets the SyncPrefEnum for this matrix for base &lt;baseID&gt;,
   * field &lt;fieldID&gt; to &lt;option&gt;.</p>
   *
   * <p>This operation will fail if this field option field is not
   * associated with a currently checked-out-for-editing
   * {@link arlut.csd.ganymede.server.FieldOptionDBField FieldOptionDBField}.</p>
   *
   * @see arlut.csd.ganymede.rmi.field_option_field
   */

  public ReturnVal setOption(Base base, SyncPrefEnum option)
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
   * <p>Sets the SyncPrefEnum for this matrix for base &lt;baseID&gt;
   * to &lt;option&gt;</p>
   *
   * <p>This operation will fail if this field option field is not
   * associated with a currently checked-out-for-editing
   * {@link arlut.csd.ganymede.server.FieldOptionDBField FieldOptionDBField}.</p>
   *
   * @see arlut.csd.ganymede.rmi.field_option_field
   */

  public synchronized ReturnVal setOption(short baseID, SyncPrefEnum option)
  {
    if (!isEditable())
      {
	// "Field Option Permissions Error"
	// "You don''t have permission to edit this field."
	return Ganymede.createErrorDialog(ts.l("global.perm_error_title"),
					  ts.l("global.perm_error_text"));
      }

    if (option == null)
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
	matrix.put(matrixEntry(baseID), option);
	
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
    Iterator<String> iterator = matrix.keySet().iterator();

    while (iterator.hasNext())
      {
	String key = iterator.next();

	// If we have invalid entries, we're just going to throw them out,
	// forget they even existed..  this is only remotely reasonable
	// because matrix is private to this class, and because these
	// invalid entries could serve no useful purpose, and will only
	// become invalid after schema editing in any case.  Since
	// normally the database/schema needs to be dumped after changing
	// the schema, this is an appropriate place to do the cleanup.

	if (!isValidCode(key))
	  {
	    if (debug)
	      {
		System.err.println("**** FieldOptionDBField.clean(): throwing out invalid entry " + 
				   decodeBaseName(key) + " " + 
				   decodeFieldName(key) + " ---- " + 
				   matrix.get(key));
	      }

	    iterator.remove();
	  }
      }
  }

  /**
   * Method to generate a key for use in our internal
   * HashMap, used to encode the option for a given {@link
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
   * HashMap, used to encode the option for a given {@link
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

    if (oldval instanceof FieldOptionMatrixCkPoint)
      {
	this.matrix = ((FieldOptionMatrixCkPoint) oldval).matrix;
        return;
      }

    throw new RuntimeException("Invalid rollback on field " + 
                               getName() + ", not a FieldOptionMatrixCkPoint");
  }

  /**
   * This method does a dump to System.err of the field option
   * contents held in this field.
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
 * <p>Helper class used to handle checkpointing of a 
 * {@link arlut.csd.ganymede.server.FieldOptionDBField FieldOptionDBField}'s
 * state.</p>
 *
 * <p>See {@link arlut.csd.ganymede.server.FieldOptionDBField#checkpoint() 
 * FieldOptionDBField.checkpoint()} for more detail.</p>
 */

class FieldOptionMatrixCkPoint {

  Map<String, SyncPrefEnum> matrix;

  /* -- */

  public FieldOptionMatrixCkPoint(FieldOptionDBField field)
  {
    this.matrix = new HashMap<String, SyncPrefEnum>(field.matrix);
  }
}
