/*

   FieldOptionDBField.java

   This class defines the field option matrix field used to support
   the GanymedeBuilderQueue message queueing build system.
   
   Created: 25 January 2005
   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996-2005
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
 * <P>FieldOptionDBField is a subclass of {@link
 * arlut.csd.ganymede.server.DBField DBField} for the storage and
 * handling of Ganymede metadata.  In particular, the
 * FieldOptionDBField is used to allow the association of option
 * strings with each {@link arlut.csd.ganymede.server.DBObjectBase
 * DBObjectBase} and each {@link
 * arlut.csd.ganymede.server.DBObjectBaseField DBObjectBaseField}
 * on the Ganymede server.</P>
 *
 * <P>The Ganymede client talks to FieldOptionDBFields through the
 * {@link arlut.csd.ganymede.rmi.field_option_field field_option_field} RMI interface.</P> 
 *
 * <P>This class differs a bit from most subclasses of {@link
 * arlut.csd.ganymede.server.DBField DBField} in that the normal setValue()/getValue()
 * methods are non-functional.  Instead, there are special methods used to set or
 * access field option information from the specially coded Hashtable held by
 * a FieldOptionDBField.  This Hashtable maps strings encoded by the
 * {@link arlut.csd.ganymede.server.FieldOptionDBField#matrixEntry(short, short)
 * matrixEntry()} methods to Strings.</P>
 *
 * <P>FieldOptionDBField is used to support the GanymedeBuilderQueue mechanism for
 * doing delta-based builds.</P>
 */

public class FieldOptionDBField extends DBField implements field_option_field {

  static final boolean debug = false;

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

    return Ganymede.createErrorDialog("Permissions Error",
				      "Don't have permission to clear this field option field\n" +
				      getName());
  }

  /**
   * <P>This utility method extracts the {@link
   * arlut.csd.ganymede.server.DBObjectBase DBObjectBase} name from a
   * coded permission entry internally held in a FieldOptionDBField
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
   * {@link arlut.csd.ganymede.server.DBObjectBaseField DBObjectBaseField} name from a coded
   * field option entry key.</P>
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
   * a {@link arlut.csd.ganymede.server.DBObjectBase DBObjectBase} and
   * not a field within a DBObjectBase.</P>
   */

  public static boolean isBase(String matrixEntry)
  {
    return (matrixEntry.indexOf("::") != -1);
  }

  /**
   * <P>This method returns true if the given FieldOptionDBField key
   * refers to a currently valid {@link
   * arlut.csd.ganymede.server.DBObjectBase DBObjectBase}/ {@link
   * arlut.csd.ganymede.server.DBObjectBaseField DBObjectBaseField} in
   * the loaded schema.</P>
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
   * <P>Receive constructor.  Used to create a FieldOptionDBField from a
   * {@link arlut.csd.ganymede.server.DBStore DBStore}/{@link arlut.csd.ganymede.server.DBJournal DBJournal}
   * DataInput stream.</P>
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
   * <P>No-value constructor.  Allows the construction of a
   * 'non-initialized' field, for use where the 
   * {@link arlut.csd.ganymede.server.DBObjectBase DBObjectBase}
   * definition indicates that a given field may be present,
   * but for which no value has been stored in the 
   * {@link arlut.csd.ganymede.server.DBStore DBStore}.</P>
   *
   * <P>Used to provide the client a template for 'creating' this
   * field if so desired.</P>
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
    return Ganymede.createErrorDialog("Field Option Field Error",
				      "setValue() not allowed on FieldOptionDBField.");
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

    if (!(target instanceof FieldOptionDBField))
      {
	return Ganymede.createErrorDialog("Copy field error",
					  "Can't copy field " + getName() +
					  ", target is not a FieldOptionDBField.");
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
   * we don't allow setValue.. FieldOptionDBField doesn't allow
   * direct setting of the entire matrix.. just use the get() and set()
   * methods below.
   *
   */

  public ReturnVal setValue(Object value, boolean local, boolean noWizards)
  {
    return Ganymede.createErrorDialog("Server: Error in FieldOptionDBField.setValue()",
				      "Error.. can't call setValue() on a FieldOptionDBField");
  }

  /**
   * <p>This method is used to return a copy of this field, with the field's owner
   * set to newOwner.</p>
   */

  public DBField getCopy(DBObject newOwner)
  {
    return new FieldOptionDBField(newOwner, this);
  }

  public Object clone()
  {
    return new FieldOptionDBField(owner, this);
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
   * <p>This method is used when the database is being dumped, to write
   * out this field to disk.  It is mated with receiveXML().</p>
   */

  void emitXML(XMLDumpContext xmlOut) throws IOException
  {
    this.emitXML(xmlOut, true);
  }

  /**
   * <p>This method is used when the database is being dumped, to write
   * out this field to disk.  It is mated with receiveXML().</p>
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

  /**
   * <p>This method is used when this field has changed, and its
   * changes need to be written to a Sync Channel.</p>
   *
   * <p>The assumptions of this method are that both this field and
   * the orig field are defined (i.e., non-null, non-empty), and that
   * orig is of the same class as this field.  It is an error to call
   * this method with null dump or orig parameters.</p>
   *
   * <p>It is also an error to call this method when this field is not
   * currently being edited in a DBEditObject, as emitXMLDelta() may
   * depend on context from the editing object.</p>
   *
   * <p>It is the responsibility of the code that calls this method to
   * determine that this field differs from orig.  If this field and
   * orig have no changes between them, the output is undefined.</p>
   */

  synchronized void emitXMLDelta(XMLDumpContext xmlOut, DBField orig) throws IOException
  {
    if (!(this.getOwner() instanceof DBEditObject))
      {
	throw new IllegalStateException();
      }

    xmlOut.startElementIndent(this.getXMLName());

    xmlOut.indentOut();

    xmlOut.startElementIndent("delta");
    xmlOut.attribute("state", "before");
    ((FieldOptionDBField) orig).emitXML(xmlOut, false);
    xmlOut.endElement("delta");
    
    xmlOut.startElementIndent("delta");
    xmlOut.attribute("state", "after");
    emitXML(xmlOut, false);
    xmlOut.endElement("delta");

    xmlOut.indentIn();

    xmlOut.endElementIndent(this.getXMLName());
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
	    result.append(decodeBaseName(key) + " " + decodeFieldName(key) +
			  " -- \"" + optionA + "\", was \"" + optionB + "\"");
	    result.append("\n");
	  }
      }

    for (int i = 0; i < newKeys.size(); i++)
      {
	String key = (String) newKeys.elementAt(i);

	optionA = (String) matrix.get(key);

	if (isBase(key))
	  {	
	    result.append(decodeBaseName(key) + " " + decodeFieldName(key) +
			  " -- \"" + optionA + "\"");
	    result.append("\n");
	  }
	else
	  {
	    result.append(decodeBaseName(key) + " " + decodeFieldName(key) +
			  " -- \"" + optionA + "\" (was undefined)");
	    result.append("\n");
	  }
      }

    for (int i = 0; i < lostKeys.size(); i++)
      {
	String key = (String) lostKeys.elementAt(i);

	optionB = (String) origFO.matrix.get(key);

	result.append(decodeBaseName(key) + " " + decodeFieldName(key) + 
		      " -- Lost " + optionB);
	result.append("\n");
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
   * <P>Resets the options in this FieldOptionDBField to the empty
   * set.  Used by non-interactive clients to reset the Field Option
   * to a known state before setting field options.</P>
   *
   * <P>Returns null on success, or a failure-coded ReturnVal
   * on permissions failure.</P>
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
	return Ganymede.createErrorDialog("Permissions failure",
					  "You don't have permissions to reset " + toString() +
					  "'s field options.");
      }
  }

  /**
   * <P>Sets the option String for base &lt;base&gt;,
   * field &lt;field&gt; to &lt;option&gt;</P>
   *
   * <P>This operation will fail if this
   * FieldOptionDBField is not editable.</P>
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
	return Ganymede.createErrorDialog("Couldn't process setOption",
					  ex.getMessage());
      }
  }

  /**
   * <P>Sets the option string for base &lt;baseID&gt;,
   * field &lt;fieldID&gt; to String &lt;option&gt;.</P>
   *
   * <P>This operation will fail if this
   * FieldOptionDBField is not editable.</P>
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
	return Ganymede.createErrorDialog("Permissions Error",
					  "You don't have permissions to edit this field");
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
   * <P>Sets the option for base &lt;baseID&gt;
   * to String &lt;option&gt;.</P>
   *
   * <P>This operation will fail if this
   * FieldOptionDBField is not editable.</P>
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
	return Ganymede.createErrorDialog("Couldn't process setOption",
					  ex.getMessage());
      }
  }

  /**
   * <P>Sets the option for base &lt;baseID&gt;
   * to Stringu &lt;option&gt;</P>
   *
   * <P>This operation will fail if this
   * FieldOptionDBField is not editable.</P>
   *
   * @see arlut.csd.ganymede.rmi.field_option_field
   */

  public synchronized ReturnVal setOption(short baseID, String option)
  {
    if (!isEditable())
      {
	return Ganymede.createErrorDialog("Permissions Error",
					  "You don't have permissions to edit this field");
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
   * <P>This internal method is used to cull out any entries in this
   * field options field that are non-operative, either by referring
   * to an object/field type that does not exist, or by being
   * redundant.</P>
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
   * <P>Method to generate a key for use in our internal
   * Hashtable, used to encode the option for a given {@link
   * arlut.csd.ganymede.server.DBObjectBase DBObjectBase} and {@link
   * arlut.csd.ganymede.server.DBObjectBaseField
   * DBObjectBaseField}.</P>
   */

  public static String matrixEntry(short baseID, short fieldID)
  {
    return (baseID + ":" + fieldID);
  }

  /**
   * <P>Method to generate a key for use in our internal
   * Hashtable, used to encode the option for a given {@link
   * arlut.csd.ganymede.server.DBObjectBase DBObjectBase}.</P>
   */
  
  public static String matrixEntry(short baseID)
  {
    return (baseID + "::");
  }

  /**
   * <P>This method is used to basically dump state out of this field
   * so that the {@link arlut.csd.ganymede.server.DBEditSet DBEditSet}
   * {@link arlut.csd.ganymede.server.DBEditSet#checkpoint(java.lang.String) checkpoint()}
   * code can restore it later
   * if need be.</P>
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

    if (!(oldval instanceof FieldOptionMatrixCkPoint))
      {
	throw new RuntimeException("Invalid rollback on field " + 
				   getName() + ", not a FieldOptionMatrixCkPoint");
      }

    if (oldval == null)
      {
	this.setUndefined(true);
      }
    else
      {
	this.matrix = ((FieldOptionMatrixCkPoint) oldval).matrix;
      }
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
 * <P>Helper class used to handle checkpointing of a 
 * {@link arlut.csd.ganymede.server.FieldOptionDBField FieldOptionDBField}'s
 * state.
 *
 * <P>See {@link arlut.csd.ganymede.server.FieldOptionDBField#checkpoint() 
 * FieldOptionDBField.checkpoint()} for more detail.</P>
 */

class FieldOptionMatrixCkPoint {

  Hashtable matrix;

  /* -- */

  public FieldOptionMatrixCkPoint(FieldOptionDBField field)
  {
    this.matrix = (Hashtable) field.matrix.clone();
  }
}
