/*
   GASH 2

   DBObjectBaseField.java

   The GANYMEDE object storage system.

   Created: 27 August 1996
   Release: $Name:  $
   Version: $Revision: 1.68 $
   Last Mod Date: $Date: 2000/03/22 06:24:09 $
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
import java.rmi.server.UnicastRemoteObject;

import gnu.regexp.*;
import com.jclark.xml.output.*;
import arlut.csd.Util.*;
import arlut.csd.JDialog.JDialogBuff;

/*------------------------------------------------------------------------------
                                                                           class
                                                               DBObjectBaseField

------------------------------------------------------------------------------*/

/**
 * <p>An entry in the Ganymede server's {@link arlut.csd.ganymede.DBStore DBStore}
 * schema dictionary.  DBStore contains a collection of 
 * {@link arlut.csd.ganymede.DBObjectBase DBObjectBase} objects, which define
 * the schema information for a particular type of object held in the Ganymede
 * database.  A DBObjectBaseField is contained within a DBObjectBase, and defines
 * the name, id, type, and constraints of a particular field that can be held
 * in {@link arlut.csd.ganymede.DBObject DBObjects} of that type, including
 * a controlling {@link arlut.csd.ganymede.DBNameSpace DBNameSpace}, if
 * appropriate.</P>
 *
 * <P>Each {@link arlut.csd.ganymede.DBField DBField} held in the server's
 * database holds a reference to a DBObjectBaseField, and the DBField's methods
 * will consult the DBObjectBaseField during run-time to make decisions based
 * on specified constraints defined in the DBObjectBaseField.</P>
 *
 * <P>The Ganymede schema editor uses the {@link arlut.csd.ganymede.BaseField BaseField}
 * remote interface to make changes to a DBObjectBaseField's constraint information
 * during schema editing. The Ganymede client may also use the BaseField interface
 * to learn about the field's type information, but it may also download a
 * {@link arlut.csd.ganymede.FieldTemplate FieldTemplate} that carries a
 * DBObjectBaseField's type information in an efficiently retrieved summary.</P>
 */

public final class DBObjectBaseField extends UnicastRemoteObject implements BaseField, FieldType {

  static final ReturnVal warning1 = genWarning1();
  static final ReturnVal warning2 = genWarning2();

  /**
   * Object type definition for the database object class we are member of
   */

  DBObjectBase base;

  /**
   * name of this field
   */

  String field_name = null;

  /**
   * id of this field in the current object type
   */

  short field_code;

  /**
   * {@link arlut.csd.ganymede.FieldType Field Type} for this field
   */

  short field_type;

  /**
   * Should this field be displayed to the client?  May be false for
   * some fields intended for 'scratch-pad' use, as in serving as
   * anchors for compound namespace use.  (i.e., the case where two
   * fields in an object are considered together for namespace use..
   * in this case, a hidden field might be defined with custom code
   * updating the hidden field whenever either of the two visible
   * fields are changed.  The hidden field will have a value of
   * XY where X is the contents of field 1 and Y the contents of
   * field 2.  Oh, never mind.)
   */

  boolean visibility = true;

  /**
   * name of class to manage user interactions with this field
   */

  String classname = null;

  /**
   * string to be displayed in the client as a tooltip explaining this field
   */

  String comment = null;

  /**
   * class object containing the code managing dbfields of this type
   */

  Class classdef;

  /**
   * true if this field is an array type
   */

  boolean array = false;

  /**
   * true if we're in the middle of loading
   */

  boolean loading = false;

  // array attributes

  /**
   * max length of array
   */

  short limit = Short.MAX_VALUE;

  // boolean attributes

  boolean labeled = false;
  String trueLabel = null;
  String falseLabel = null;

  // string attributes

  short minLength = 0;
  short maxLength = Short.MAX_VALUE;
  String okChars = null;
  String badChars = null;
  DBNameSpace namespace = null;
  boolean multiLine = false;

  /**
   * Regular Expression string for input-filtering
   * in {@link arlut.csd.ganymede.StringDBField}s.
   */

  String regexpPat = null;	// introduced in ganymede.db version 1.14

  /**
   * Compiled regular expression for input-filtering
   * in {@link arlut.csd.ganymede.StringDBField}s.
   */

  gnu.regexp.RE regexp = null;

  // invid attributes

  boolean editInPlace = false;
  short allowedTarget = -1;	// no target restrictions
  short targetField = -1;	// no field symmetry.. we use the DBStore backPointers structure by default

  /**
   * If this is not null, then we have gotten information on this
   * Invid DBObjectBaseField pointing to a type of object from an XML
   * file, and we'll need to do type resolution once the schema is
   * completely loaded from an XML stream.  Once this happens,
   * allowedTarget will be set properly, and allowedTargetStr will be
   * set to null.
   */

  String allowedTargetStr = null;

  /**
   * If this is not null, then we have gotten information on this
   * Invid DBObjectBaseField linked to a field from an XML file, and
   * we'll need to do type resolution once the schema is completely
   * loaded from an XML stream.  Once this happens, targetField will
   * be set properly, and targetFieldStr will be set to null.  
   */

  String targetFieldStr = null;

  // password attributes

  boolean crypted = true;	// UNIX encryption is the default.
  boolean md5crypted = false;	// OpenBSD style md5crypt() is not
  boolean storePlaintext = false; // nor is plaintext

  // schema editing

  /**
   * If we are being edited, this will point to an instance
   * of a server-side schema editing class.  */

  DBSchemaEdit editor;

  /**
   * Downloadable FieldTemplate representing the constant field type
   * attributes represented by this DBObjectBaseField.  This template
   * is regenerated whenever clearEditor() is called, upon schema
   * editing completion.
   */

  FieldTemplate template;

  // for DBBaseFieldTable

  DBObjectBaseField next = null;

  /**
   * <P>A three state flag used by isInUse() to report whether or
   * not a particular field is in use in the loaded database.</P>
   */

  Boolean inUseCache = null;

  /**
   * This field is used to handle field order sorting when
   * we read an old (pre-2.0) ganymede.db file.
   */

  int tmp_displayOrder = -1;

  /* -- */

  /**
   *
   * Generic field constructor.
   *
   */

  DBObjectBaseField(DBObjectBase base) throws RemoteException
  {
    this.base = base;
    field_name = "";
    classname = "";
    comment = "";
    
    field_code = 0;
    field_type = 0;
    editor = null;
  }

  /**
   *
   * Editing base constructor.  This constructor is used to create a new
   * field definition object in an editing context. 
   *
   */

  DBObjectBaseField(DBObjectBase base, DBSchemaEdit editor) throws RemoteException
  {
    this(base);
    this.editor = editor;
    inUseCache = new Boolean(false);
  }

  /**
   *
   * Receive constructor.
   *
   */

  DBObjectBaseField(DataInput in, DBObjectBase base) throws IOException, RemoteException
  {
    this(base);
    receive(in);
    template = new FieldTemplate(this);
  }

  /**
   * <p>Copy constructor, used during schema editing.</p>
   *
   * <p><b>IMPORTANT: BE SURE TO ALWAYS EDIT THIS METHOD IF YOU ADD ANY FIELDS
   * TO THIS CLASS!</b></p>
   */

  DBObjectBaseField(DBObjectBaseField original, DBSchemaEdit editor) throws RemoteException
  {
    this(original.base);

    field_name = original.field_name; // name of this field
    field_code = original.field_code; // id of this field in the current object
    field_type = original.field_type; // data type contained herein

    visibility = original.visibility;

    classname = original.classname; // name of class to manage user interactions with this field
    comment = original.comment;
    classdef = original.classdef; // class object containing the code managing dbfields of this type
    array = original.array;	// true if this field is an array type
    limit = original.limit;

    labeled = original.labeled;
    trueLabel = original.trueLabel;
    falseLabel = original.falseLabel;

    minLength = original.minLength;
    maxLength = original.maxLength;
    okChars = original.okChars;
    badChars = original.badChars;
    namespace = original.namespace; // we point to the original namespace.. not a problem, since they are immutable
    multiLine = original.multiLine;
    regexpPat = original.regexpPat;
    regexp = original.regexp;

    editInPlace = original.editInPlace;
    allowedTarget = original.allowedTarget;
    targetField = original.targetField;

    crypted = original.crypted;
    md5crypted = original.md5crypted;
    storePlaintext = original.storePlaintext;

    inUseCache = null;

    // We'll just re-use the original's FieldTemplate for the time
    // being.. when the SchemaEditor is done, it will call
    // clearEditor() on our DBObjectBase, which will create a new
    // FieldTemplate for us.

    template = original.template;
    this.editor = editor;
  }

  /**
   * <p>This method is used when the database is being dumped, to write
   * out this field definition to disk.  It is mated with receive().</p>
   */

  synchronized void emit(DataOutput out) throws IOException
  {
    out.writeUTF(field_name);
    out.writeShort(field_code);
    out.writeShort(field_type);
    out.writeUTF(classname);
    out.writeUTF(comment);

    out.writeBoolean(visibility); // added at file version 1.6

    out.writeBoolean(array);

    if (array)
      {
	out.writeShort(limit);
      }

    if (isBoolean())
      {
	out.writeBoolean(labeled);
	if (labeled)
	  {
	    out.writeUTF(trueLabel);
	    out.writeUTF(falseLabel);
	  }
      }
    else if (isString())
      {
	out.writeShort(minLength);
	out.writeShort(maxLength);

	if (okChars == null)
	  {
	    out.writeUTF("");
	  }
	else
	  {
	    out.writeUTF(okChars);
	  }
	
	if (badChars == null)
	  {
	    out.writeUTF("");
	  }
	else
	  {
	    out.writeUTF(badChars);
	  }

	if (namespace != null)
	  {
	    out.writeUTF(namespace.getName());
	  }
	else
	  {
	    out.writeUTF("");
	  }

	out.writeBoolean(multiLine); // added at file version 1.9

	if (regexpPat == null)
	  {
	    out.writeUTF("");	// added at file version 1.14
	  }
	else
	  {
	    out.writeUTF(regexpPat); // added at file version 1.14
	  }
      }
    else if (isNumeric())
      {
	if (namespace != null)
	  {
	    out.writeUTF(namespace.getName());
	  }
	else
	  {
	    out.writeUTF("");
	  }
      }
    else if (isIP())
      {
	if (namespace != null)
	  {
	    out.writeUTF(namespace.getName());
	  }
	else
	  {
	    out.writeUTF("");
	  }
      }
    else if (isInvid())
      {
	out.writeShort(allowedTarget);
	out.writeBoolean(editInPlace);
	out.writeShort(targetField);
      }
    else if (isPassword())
      {
	out.writeShort(minLength);
	out.writeShort(maxLength);

	if (okChars == null)
	  {
	    out.writeUTF("");
	  }
	else
	  {
	    out.writeUTF(okChars);
	  }
	
	if (badChars == null)
	  {
	    out.writeUTF("");
	  }
	else
	  {
	    out.writeUTF(badChars);
	  }

	out.writeBoolean(crypted);
	out.writeBoolean(md5crypted);
	out.writeBoolean(storePlaintext);
      }
  }

  /**
   * <p>This method is used when the database is being loaded, to read
   * in this field definition from disk.  It is mated with emit().</p>
   */

  synchronized void receive(DataInput in) throws IOException
  {
    loading = true;

    field_name = in.readUTF();
    field_code = in.readShort();
    field_type = in.readShort();
    classname = in.readUTF();

    if (classname != null && !classname.equals(""))
      {
	try 
	  {
	    classdef = Class.forName(classname);
	  }
	catch (ClassNotFoundException ex)
	  {	    
	    System.err.println("DBObjectBaseField.receive(): class definition could not be found: " + ex);
	    classdef = null;
	  }
      }

    comment = in.readUTF();

    // we stopped keeping the editable and removable flags in the
    // ganymede.db file at 1.17

    if (base.store.file_major == 1 && base.store.file_minor <= 17)
      {
	in.readBoolean();	// skip editable
	in.readBoolean();	// skip removable
      }

    // at file version 1.6, we introduced field visibility

    if ((base.store.file_major > 1) || (base.store.file_minor >= 6))
      {
	visibility = in.readBoolean();
      }
    else
      {
	visibility = true;
      }

    // at file version 1.7, we introduced an explicit built-in flag
    // we took it out at 2.0

    if (base.store.file_major == 1 && 
	base.store.file_minor >= 7 && 
	base.store.file_minor <= 17)
      {
	in.readBoolean();	// skip builtIn
      }

    // between file versions 1.1 and 1.17, we had a field_order
    // field

    if (base.store.file_major == 1 && 
	base.store.file_minor >= 1 && 
	base.store.file_minor <= 17)
      {
	tmp_displayOrder = in.readShort();		// skip field_order
      }
    else
      {
	tmp_displayOrder = -1;
      }

    array = in.readBoolean();

    if (array)
      {
	limit = in.readShort();
      }
    else
      {
	limit = 1;
      }

    if (isBoolean())
      {
	labeled = in.readBoolean();
	if (labeled)
	  {
	    trueLabel = in.readUTF();
	    falseLabel = in.readUTF();
	  }
      }
    else if (isString())
      {
	String nameSpaceId;

	/* - */

	minLength = in.readShort();
	maxLength = in.readShort();
	okChars = in.readUTF();
	
	if (okChars.equals(""))
	  {
	    okChars = null;
	  }

	badChars = in.readUTF();

	if (badChars.equals(""))
	  {
	    badChars = null;
	  }

	nameSpaceId = in.readUTF();
	
	if (!nameSpaceId.equals(""))
	  {
	    setNameSpace(nameSpaceId);
	  }

	// at file version 1.9, we introduced multiLine
	
	if ((base.store.file_major > 1) || (base.store.file_minor >= 9))
	  {
	    multiLine = in.readBoolean();
	  }
	else
	  {
	    multiLine = false;
	  }

	// at file version 1.14, we introduced regexps for string fields
	
	if ((base.store.file_major > 1) || (base.store.file_minor >= 14))
	  {
	    setRegexpPat(in.readUTF());
	  }
	else
	  {
	    setRegexpPat(null);
	  }
      }
    else if (isNumeric())
      {
	String nameSpaceId;

	/* - */

	// at 1.8 we introduced namespaces for number fields

	if ((base.store.file_major > 1) || (base.store.file_minor >= 8))
	  {
	    nameSpaceId = in.readUTF();
	    
	    if (!nameSpaceId.equals(""))
	      {
		setNameSpace(nameSpaceId);
	      }
	  }
      }
    else if (isIP())
      {
	String nameSpaceId;

	/* - */

	// at 1.8 we introduced namespaces for IP fields

	if ((base.store.file_major > 1) || (base.store.file_minor >= 8))
	  {
	    nameSpaceId = in.readUTF();
	    
	    if (!nameSpaceId.equals(""))
	      {
		setNameSpace(nameSpaceId);
	      }
	  }
      }
    else if (isInvid())
      {
	allowedTarget = in.readShort();
	editInPlace = in.readBoolean();
	targetField = in.readShort();

	// In DBStore file version 1.17 we dropped the use of the back
	// links field.  Some folks apparently used the schema editor
	// to set an Invid Field to point to the backlinks field
	// explicitly (rather than 'none').  We handle that here so
	// that the code that takes care of handling asymmetric fields
	// doesn't get confused.

	if (targetField == SchemaConstants.BackLinksField)
	  {
	    targetField = -1;
	  }
      }
    else if (isPassword())
      {
	minLength = in.readShort();
	maxLength = in.readShort();
	okChars = in.readUTF();
	
	if (okChars.equals(""))
	  {
	    okChars = null;
	  }

	badChars = in.readUTF();

	if (badChars.equals(""))
	  {
	    badChars = null;
	  }

	crypted = in.readBoolean();

	// at 1.16 we introduce md5crypted

	if ((base.store.file_major >1) || (base.store.file_minor >= 16))
	  {
	    md5crypted = in.readBoolean();
	  }
	else
	  {
	    md5crypted = false;
	  }

	if ((base.store.file_major >1) || (base.store.file_minor >= 10))
	  {
	    storePlaintext = in.readBoolean();
	  }
	else
	  {
	    storePlaintext = false;
	  }
      }

    loading = false;
  }

  /**
   * <p>This method is used when the database is being dumped, to write
   * out this field definition to disk.  It is mated with receive().</p>
   */

  synchronized void emitXML(XMLWriter xmlOut, int indentLevel) throws IOException
  {
    boolean nonEmpty = false;

    /* -- */

    XMLUtils.indent(xmlOut, indentLevel);
    indentLevel++;

    xmlOut.startElement("fielddef");
    xmlOut.attribute("name", field_name);
    xmlOut.attribute("id", java.lang.Short.toString(field_code));

    if (classname != null && !classname.equals(""))
      {
	XMLUtils.indent(xmlOut, indentLevel);
	xmlOut.startElement("classname");
	xmlOut.attribute("name", classname);
	xmlOut.endElement("classname");
      }

    if (comment != null && !comment.equals(""))
      {
	XMLUtils.indent(xmlOut, indentLevel);
	xmlOut.startElement("comment");
	xmlOut.write(comment);
	xmlOut.endElement("comment");
      }

    if (!visibility)
      {
	XMLUtils.indent(xmlOut, indentLevel);
	xmlOut.startElement("invisible");
	xmlOut.endElement("invisible");
      }

    XMLUtils.indent(xmlOut, indentLevel);
    xmlOut.startElement("typedef");
    
    switch (field_type)
      {
      case FieldType.BOOLEAN:
	xmlOut.attribute("type", "boolean");
	break;
      case FieldType.NUMERIC:
	xmlOut.attribute("type", "numeric");
	break;
      case FieldType.FLOAT:
	xmlOut.attribute("type", "float");
	break;
      case FieldType.DATE:
	xmlOut.attribute("type", "date");
	break;
      case FieldType.STRING:
	xmlOut.attribute("type", "string");
	break;
      case FieldType.INVID:
	xmlOut.attribute("type", "invid");
	break;
      case FieldType.PERMISSIONMATRIX:
	xmlOut.attribute("type", "permmatrix");
	break;
      case FieldType.PASSWORD:
	xmlOut.attribute("type", "password");
	break;
      case FieldType.IP:
	xmlOut.attribute("type", "ip");
	break;
      default:
	throw new RuntimeException("emitXML: unrecognized field type:" + field_type);
      }

    indentLevel++;

    if (array)
      {
	nonEmpty = true;
	XMLUtils.indent(xmlOut, indentLevel);
	xmlOut.startElement("vector");
	xmlOut.attribute("maxSize", java.lang.Short.toString(limit));
	xmlOut.endElement("vector");
      }

    if (isBoolean())
      {
	if (labeled)
	  {
	    nonEmpty = true;
	    XMLUtils.indent(xmlOut, indentLevel);
	    xmlOut.startElement("labeled");
	    xmlOut.attribute("true", trueLabel);
	    xmlOut.attribute("false", falseLabel);
	    xmlOut.endElement("labeled");
	  }
      }
    else if (isString())
      {
	nonEmpty = true;

	XMLUtils.indent(xmlOut, indentLevel);
	xmlOut.startElement("minlength");
	xmlOut.attribute("val", java.lang.Short.toString(minLength));
	xmlOut.endElement("minlength");

	XMLUtils.indent(xmlOut, indentLevel);
	xmlOut.startElement("maxlength");
	xmlOut.attribute("val", java.lang.Short.toString(maxLength));
	xmlOut.endElement("maxlength");

	if (okChars != null && !okChars.equals(""))
	  {
	    XMLUtils.indent(xmlOut, indentLevel);
	    xmlOut.startElement("okchars");
	    xmlOut.attribute("val", okChars);
	    xmlOut.endElement("okchars");
	  }

	if (badChars != null && !badChars.equals(""))
	  {
	    XMLUtils.indent(xmlOut, indentLevel);
	    xmlOut.startElement("badchars");
	    xmlOut.attribute("val", badChars);
	    xmlOut.endElement("badchars");
	  }
	
	if (namespace != null)
	  {
	    XMLUtils.indent(xmlOut, indentLevel);
	    xmlOut.startElement("namespace");
	    xmlOut.attribute("val", namespace.getName());
	    xmlOut.endElement("namespace");
	  }

	if (regexpPat != null && !regexpPat.equals(""))
	  {
	    XMLUtils.indent(xmlOut, indentLevel);
	    xmlOut.startElement("regexp");
	    xmlOut.attribute("val", regexpPat);
	    xmlOut.endElement("regexp");
	  }

	if (multiLine)
	  {
	    XMLUtils.indent(xmlOut, indentLevel);
	    xmlOut.startElement("multiline");
	    xmlOut.endElement("multiline");
	  }
      }
    else if (isNumeric())
      {
	if (namespace != null)
	  {
	    nonEmpty = true;

	    XMLUtils.indent(xmlOut, indentLevel);
	    xmlOut.startElement("namespace");
	    xmlOut.attribute("val", namespace.getName());
	    xmlOut.endElement("namespace");
	  }
      }
    else if (isIP())
      {
	if (namespace != null)
	  {
	    nonEmpty = true;

	    XMLUtils.indent(xmlOut, indentLevel);
	    xmlOut.startElement("namespace");
	    xmlOut.attribute("val", namespace.getName());
	    xmlOut.endElement("namespace");
	  }
      }
    else if (isInvid())
      {
	if (allowedTarget != -1)
	  {
	    DBObjectBase targetObjectBase = null;
	    nonEmpty = true;

	    XMLUtils.indent(xmlOut, indentLevel);
	    xmlOut.startElement("targetobject");

	    if (allowedTarget == -2)
	      {
		xmlOut.attribute("name", "*any*");
	      }
	    else
	      {
		String targetObjectName = null;
		targetObjectBase = base.store.getObjectBase(allowedTarget);

		if (targetObjectBase != null)
		  {
		    targetObjectName = targetObjectBase.getName();
		  }

		if (targetObjectName != null)
		  {
		    xmlOut.attribute("name", targetObjectName);
		  }
		else
		  {
		    xmlOut.attribute("id", java.lang.Short.toString(allowedTarget));
		  }
	      }

	    xmlOut.endElement("targetobject");

	    if (targetField != -1 && targetField != SchemaConstants.BackLinksField)
	      {
		boolean wroteLabel = false;

		XMLUtils.indent(xmlOut, indentLevel);

		xmlOut.startElement("targetfield");

		if (targetObjectBase != null)
		  {
		    DBObjectBaseField targetFieldDef = (DBObjectBaseField) targetObjectBase.getField(targetField);

		    if (targetFieldDef != null)
		      {
			xmlOut.attribute("name", targetFieldDef.getName());
			wroteLabel = true;
		      }
		  }

		if (!wroteLabel)
		  {
		    xmlOut.attribute("id", java.lang.Short.toString(targetField));
		  }

		xmlOut.endElement("targetfield");
	      }

	    if (editInPlace)
	      {
		XMLUtils.indent(xmlOut, indentLevel);
		xmlOut.startElement("embedded");
		xmlOut.endElement("embedded");
	      }
	  }
      }
    else if (isPassword())
      {
	nonEmpty = true;

	XMLUtils.indent(xmlOut, indentLevel);
	xmlOut.startElement("minlength");
	xmlOut.attribute("val", java.lang.Short.toString(minLength));
	xmlOut.endElement("minlength");

	XMLUtils.indent(xmlOut, indentLevel);
	xmlOut.startElement("maxlength");
	xmlOut.attribute("val", java.lang.Short.toString(maxLength));
	xmlOut.endElement("maxlength");

	if (okChars != null && !okChars.equals(""))
	  {
	    XMLUtils.indent(xmlOut, indentLevel);
	    xmlOut.startElement("okchars");
	    xmlOut.attribute("val", okChars);
	    xmlOut.endElement("okchars");
	  }

	if (badChars != null && !badChars.equals(""))
	  {
	    XMLUtils.indent(xmlOut, indentLevel);
	    xmlOut.startElement("badchars");
	    xmlOut.attribute("val", badChars);
	    xmlOut.endElement("badchars");
	  }

	if (crypted)
	  {
	    XMLUtils.indent(xmlOut, indentLevel);
	    xmlOut.startElement("crypted");
	    xmlOut.endElement("crypted");
	  }

	if (md5crypted)
	  {
	    XMLUtils.indent(xmlOut, indentLevel);
	    xmlOut.startElement("md5crypted");
	    xmlOut.endElement("md5crypted");
	  }

	if (storePlaintext)
	  {
	    XMLUtils.indent(xmlOut, indentLevel);
	    xmlOut.startElement("plaintext");
	    xmlOut.endElement("plaintext");
	  }
      }

    indentLevel--;

    if (nonEmpty)
      {
	XMLUtils.indent(xmlOut, indentLevel);
      }

    xmlOut.endElement("typedef");

    indentLevel--;
    XMLUtils.indent(xmlOut, indentLevel);
    xmlOut.endElement("fielddef");
  }

  /**
   * <P>This method is used to read the definition for this
   * DBObjectBaseField from an XMLReader stream.  When this method is
   * called, the <fielddef> open element should be the very next item
   * in the reader stream.  This method will consume every element in
   * the reader stream up to and including the matching </fielddef>
   * element.</P>
   *
   * <P>If important expectations about the state of the XML stream
   * are not met, an IllegalArgumentException will be thrown, and
   * the stream will be left in an indeterminate state.</P>
   */

  synchronized void receiveXML(XMLReader reader)
  {
    XMLItem item, nextItem;
    Integer field_codeInt;
    boolean typeRead = false;

    /* -- */

    item = reader.getNextItem(true);

    if (item == null || !item.matches("fielddef"))
      {
	throw new IllegalArgumentException("DBObjectBaseField.receiveXML(): next element != open fielddef: " +
					   item);
      }

    // neither of these should be null

    field_name = item.getAttrStr("name");
    field_codeInt = item.getAttrInt("id");

    if (field_name == null)
      {
	throw new IllegalArgumentException("DBObjectBaseField.receiveXML(): fielddef does not define name attr.: " +
					   item);
      }

    if (field_codeInt == null)
      {
	throw new IllegalArgumentException("DBObjectBaseField.receiveXML(): fielddef does not define id attr.: " +
					   item);
      }

    // extract the short

    field_code = field_codeInt.shortValue();

    // and loop until we get to the fielddef close or eof

    item = reader.getNextItem(true);

    while (item != null && !item.matchesClose("fielddef"))
      {
	if (item.matches("classname"))
	  {
	    classname = item.getAttrStr("name");
	  }
	else if (item.matches("comment"))
	  {
	    comment = reader.getFollowingString(item, true);
	  }
	else if (item.matches("invisible"))
	  {
	    visibility = false;
	  }
	else if (item.matches("typedef"))
	  {
	    if (typeRead)
	      {
		throw new IllegalArgumentException("redundant type definition for this field");
	      }

	    if (item.getAttrStr("type") == null)
	      {
		throw new IllegalArgumentException("typedef tag does not contain type attribute: " + 
						   item);
	      }

	    if (item.getAttrStr("type").equals("boolean"))
	      {
		field_type = FieldType.BOOLEAN;
	      }
	    else if (item.getAttrStr("type").equals("numeric"))
	      {
		field_type = FieldType.NUMERIC;
	      }
	    else if (item.getAttrStr("type").equals("float"))
	      {
		field_type = FieldType.FLOAT;
	      }
	    else if (item.getAttrStr("type").equals("date"))
	      {
		field_type = FieldType.DATE;
	      }
	    else if (item.getAttrStr("type").equals("string"))
	      {
		field_type = FieldType.STRING;
	      }
	    else if (item.getAttrStr("type").equals("invid"))
	      {
		field_type = FieldType.INVID;
	      }
	    else if (item.getAttrStr("type").equals("permmatrix"))
	      {
		field_type = FieldType.PERMISSIONMATRIX;
	      }
	    else if (item.getAttrStr("type").equals("password"))
	      {
		field_type = FieldType.PASSWORD;
	      }
	    else if (item.getAttrStr("type").equals("ip"))
	      {
		field_type = FieldType.IP;
	      }
	    else
	      {
		throw new IllegalArgumentException("typedef tag does not contain type attribute: " +
						   item);
	      }

	    typeRead = true;

	    item = reader.getNextItem(true);

	    while (item != null && !item.matchesClose("typedef"))
	      {
		if (item.matches("vector"))
		  {
		    if (!isString() && !isInvid() && !isIP())
		      {
			// need to flesh out this error handling 

			System.err.println("Ignoring vector element.. not a string, invid, or ip field.");
		      }
		    else
		      {
			Integer vectSize = item.getAttrInt("maxSize");

			if (vectSize == null)
			  {
			    throw new IllegalArgumentException("vector tag does not contain maxSize attribute");
			  }

			limit = vectSize.shortValue();
		      }
		  }
		else if (item.matches("labeled") && isBoolean())
		  {
		    trueLabel = item.getAttrStr("true");
		    falseLabel = item.getAttrStr("false");
		  }
		else if (item.matches("minlength") && 
			 (isString() || isPassword()))
		  {
		    Integer val = item.getAttrInt("val");

		    if (val == null)
		      {
			System.err.println("minlength tag unexpectedly missing val attribute");
		      }

		    minLength = val.shortValue();
		  }
		else if (item.matches("maxlength") && 
			 (isString() || isPassword()))
		  {
		    Integer val = item.getAttrInt("val");

		    if (val == null)
		      {
			System.err.println("maxlength tag unexpectedly missing val attribute");
		      }

		    maxLength = val.shortValue();
		  }
		else if (item.matches("okchars") && 
			 (isString() || isPassword()))
		  {
		    okChars = item.getAttrStr("val");
		  }
		else if (item.matches("badchars") && 
			 (isString() || isPassword()))
		  {
		    badChars = item.getAttrStr("val");
		  }
		else if (item.matches("regexp") && isString())
		  {
		    regexpPat = item.getAttrStr("val");
		  }
		else if (item.matches("multiline") && isString())
		  {
		    multiLine = true;
		  }
		else if (item.matches("namespace") && 
			 (isString() || isNumeric() || isIP()))
		  {
		    String nameSpaceId = item.getAttrStr("val");

		    if (nameSpaceId != null && !nameSpaceId.equals(""))
		      {
			ReturnVal retVal = setNameSpace(nameSpaceId);

			if (retVal != null && !retVal.didSucceed())
			  {
			    System.err.println("DBObjectBaseField.receiveXML(): namespace definition for " +
					       nameSpaceId + " not found.");
			  }
		      }
		  }
		else if (item.matches("targetobject") && isInvid())
		  {
		    allowedTargetStr = item.getAttrStr("name");
		    Integer targetInt = item.getAttrInt("id");

		    if (allowedTargetStr == null && targetInt != null)
		      {
			allowedTarget = targetInt.shortValue();
		      }
		    else if (allowedTargetStr == null && targetInt == null)
		      {
			System.err.println("DBObjectBaseField.receiveXML(): missing required attrs in <targetobject>: " +
					   item);
		      }

		    // else we've got allowedTargetStr defined and we'll
		    // go back through and do the resolution when the
		    // schema is fully loaded
		  }
		else if (item.matches("targetfield") && isInvid())
		  {
		    targetFieldStr = item.getAttrStr("name");
		    Integer targetInt = item.getAttrInt("id");

		    if (targetFieldStr == null && targetInt != null)
		      {
			targetField = targetInt.shortValue();
		      }
		    else if (targetFieldStr == null && targetInt == null)
		      {
			System.err.println("DBObjectBaseField.receiveXML(): missing required attrs in <targetfield>: " +
					   item);
		      }

		    // else we've got targetFieldStr defined and we'll
		    // go back through and do the resolution when the
		    // schema is fully loaded
		  }
		else if (item.matches("embedded") && isInvid())
		  {
		    editInPlace = true;
		  }
		else if (item.matches("crypted") && isPassword())
		  {
		    crypted = true;
		  }
		else if (item.matches("md5crypted") && isPassword())
		  {
		    md5crypted = true;
		  }
		else if (item.matches("plaintext") && isPassword())
		  {
		    storePlaintext = true;
		  }
		else
		  {
		    System.err.println("DBObjectBaseField.receiveXML(): unrecognized XML item inside typedef: " + 
				       item);
		  }
	      }
	  }
	else
	  {
	    System.err.println("DBObjectBaseField.receiveXML(): unrecognized XML item inside fielddef: " +
			       item);
	  }
	
	item = reader.getNextItem(true);
      }

    if (item == null)
      {
	throw new IllegalArgumentException("DBObjectBaseField.receiveXML(): hit end of stream prior to </fielddef>");
      }
  }

  // ----------------------------------------------------------------------

  /**
   * <p>This method returns true if this field definition can be edited
   * in the schema editor.</p>
   *
   * <p>This method will return false for the built-in field types
   * that the server is dependent on for its own functioning.</p>
   *
   * @see arlut.csd.ganymede.BaseField 
   */

  public boolean isEditable()
  {
    return true;
  }

  /**
   * <p>This method returns true if this field is one of the
   * system fields present in all objects.</p>
   */

  public boolean isBuiltIn()
  {
    return this.getID() < 100;
  }
  
  /**
   * <p>This method returns true if this field definition can be removed
   * by the schema editor.</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public boolean isRemovable()
  {
    return this.getID() >= 256;	// fields with id's below 256 are server-essential
  }

  /**
   * <p>This method returns true if this field
   * is intended to be visible to the client normally,
   * false otherwise.</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public boolean isVisible()
  {
    return visibility;
  }

  /**
   * <P>This method returns true if there are any fields of this type
   * in the database.  The schema editing system uses this method to
   * prevent incompatible modifications to fields that are in use
   * in the database.</P>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public boolean isInUse()
  {
    if (inUseCache == null)
      {
	inUseCache = new Boolean(((DBObjectBase) this.getBase()).fieldInUse(this));
      }

    return inUseCache.booleanValue();
  }

  /**
   * <p>Returns the Base we are a part of.</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public Base getBase()
  {
    return base;
  }

  /**
   * <p>Returns a FieldTemplate serializable field definition object
   * for this field.</p>
   */

  public FieldTemplate getTemplate()
  {
    if (template == null)
      {
	template = new FieldTemplate(this);
      }
    
    return template;
  }

  /**
   * <p>Returns the name of this field</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public String getName()
  {
    return field_name;
  }

  /**
   * <p>Sets the name of this field</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public synchronized ReturnVal setName(String name)
  {
    if (editor == null)
      {
	throw new IllegalArgumentException("not editing");
      }
    
    if (name == null || name.equals(""))
      {
	throw new IllegalArgumentException("can't have a null or empty name");
      }

    if (name.equals(field_name))
      {
	return null;
      }

    try
      {
	if (getBase().getField(name) != null)
	  {
	    return Ganymede.createErrorDialog("Schema Editing Error",
					      "That name is already taken.");
	  }
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException(ex.getMessage());
      }

    field_name = name;

    return null;
  }

  /**
   * <p>Returns the name of the class managing instances of this field</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public String getClassName()
  {
    return classname;
  }

  /**
   * <p>Sets the name of the class managing instances of this field</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public synchronized ReturnVal setClassName(String name)
  {
    Class newclassdef;

    if (editor == null)
      {
	throw new IllegalArgumentException("not editing");
      }

    if (!name.equals(classname))
      {
	try 
	  {
	    newclassdef = Class.forName(name);

	    // won't get here if name was bad

	    classname = name;	
	    classdef = newclassdef;
	  }
	catch (ClassNotFoundException ex)
	  {	    
	    System.err.println("DBObjectBaseField.setClassName(): class definition could not be found: " + ex);
	  }
      }

    return null;
  }

  /**
   * <p>Returns the Class object managing instances of this field</p>
   */

  public Class getClassDef()
  {
    return classdef;
  }

  /**
   * <p>Returns the comment defined in the schema for this field</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public String getComment()
  {
    return comment;
  }

  /**
   * <p>Sets the comment defined in the schema for this field</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public synchronized ReturnVal setComment(String s)
  {
    if (editor == null)
      {
	throw new IllegalArgumentException("not editing");
      }

    comment = s;
    
    return null;
  }      

  /**
   * <p>Returns the field type</p>
   *
   * <p>Where type is one of the following
   * constants defined in the {@link arlut.csd.ganymede.FieldType FieldType}
   * interface:</p>
   *
   * <pre>
   *   static short BOOLEAN = 0;
   *   static short NUMERIC = 1;
   *   static short DATE = 2;
   *   static short STRING = 3;
   *   static short INVID = 4;
   *   static short PERMISSIONMATRIX = 5;
   *   static short PASSWORD = 6;
   *   static short IP = 7;
   *   static short FLOAT = 8;
   * </pre>
   *
   * @see arlut.csd.ganymede.DBStore
   * @see arlut.csd.ganymede.BaseField
   */

  public short getType()
  {
    return field_type;
  }

  /**
   * <p>Sets the {@link arlut.csd.ganymede.FieldType field type}
   * for this field.  Changing the basic type of a field that is already being
   * used in the server will cause very bad things to happen.  The
   * right way to change an existing field is to delete the field, commit
   * the schema edit, edit the schema again, and recreate the field with
   * the desired field type.</P>
   *
   * <p>If the new field type is not string, invid, or IP, the field
   * will be made a scalar field.</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public synchronized ReturnVal setType(short type)
  {
    if (editor == null)
      {
	throw new IllegalArgumentException("not editing");
      }

    if (isInUse())
      {
	return Ganymede.createErrorDialog("Error",
					  "Can't change the type of a field which is in use in the database.");
      }

    if (isInvid())
      {
	// need to check to make sure no other invid field definitions are
	// pointing to this field somehow
      }

    if (type < FIRSTFIELD || type > LASTFIELD)
      {
	throw new IllegalArgumentException("type argument out of range");
      }

    field_type = type;

    // only strings, invids, and ip fields can be vectors

    if (!((isString() || isInvid() || isIP())))
      {
	array = false;
      }

    return null;
  }

  // type identification convenience methods

  /**
   * <p>Returns true if this field is of boolean type</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public boolean isBoolean()
  {
    return (field_type == BOOLEAN);
  }

  /**
   * <p>Returns true if this field is of numeric type</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public boolean isNumeric()
  {
    return (field_type == NUMERIC);
  }

  /**
   * <p>Returns true if this field is of float type</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public boolean isFloat()
  {
    return (field_type == FLOAT);
  }

  /**
   * <p>Returns true if this field is of date type</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public boolean isDate()
  {
    return (field_type == DATE);
  }

  /**
   * <p>Returns true if this field is of string type</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public boolean isString()
  {
    return (field_type == STRING);
  }

  /**
   * <p>Returns true if this field is of invid type</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public boolean isInvid()
  {
    return (field_type == INVID);
  }

  /**
   * <p>Returns true if this field is of permission matrix type</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public boolean isPermMatrix()
  {
    return (field_type == PERMISSIONMATRIX);
  }

  /**
   * <p>Returns true if this field is of password type</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public boolean isPassword()
  {
    return (field_type == PASSWORD);
  }

  /**
   * <p>Returns true if this field is of IP type</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public boolean isIP()
  {
    return (field_type == IP);
  }
  
  /**
   * <p>Returns true if this field is a vector field, false otherwise.</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public boolean isArray()
  {
    return array;
  }

  /**
   * <p>Set this field to be a vector or scalar.  If b is true, this field will
   * be a vector, if false, scalar.</p>
   *
   * <p>Only strings, invid's, and ip fields may be vectors.  Attempting to 
   * setArray(true) for other field types will cause an IllegalArgumentException
   * to be thrown.</p>
   *
   * <p>It may be possible to compatibly handle the conversion from
   * scalar to vector, but a vector to scalar change is an incompatible
   * change.</p>
   *
   * @see arlut.csd.ganymede.BaseField 
   */

  public synchronized ReturnVal setArray(boolean b)
  {
    if (editor == null)
      {
	throw new IllegalArgumentException("not editing");
      }

    if (isInUse())
      {
	return Ganymede.createErrorDialog("Error",
					  "can't change the vector status of a field in use.");
      }

    if (b && !(isString() || isInvid() || isIP()))
      {
	throw new IllegalArgumentException("can't set this field type to vector");
      }

    array = b;

    return null;
  }

  /**
   * <p>Returns id code for this field.  Each field in a
   * {@link arlut.csd.ganymede.DBObject DBObject}
   * has a unique code which identifies the field.  This code represents
   * the field in the on-disk data store, and is used by 
   * {@link arlut.csd.ganymede.DBEditObject DBEditObject}
   * to choose what field to change in the setField method.</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public short getID()
  {
    return field_code;
  }

  /**
   * <p>Returns the type id for this field definition as
   * a Short, suitable for use in a hash.</p>
   */

  public Short getKey()
  {
    return new Short(field_code);
  }

  /**
   * <p>Returns the object definition that this field is defined under.</p>
   */

  public DBObjectBase base()
  {
    return base;
  }

  synchronized ReturnVal setBase(DBObjectBase base)
  {
    if (editor == null)
      {
	throw new IllegalArgumentException("not editing");
      }

    this.base = base;

    return null;
  }

  // **
  // array attribute methods
  // **

  /**
   * <p>Returns the array size limitation for this field if it is an array field</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public short getMaxArraySize()
  {
    if (!array)
      {
	throw new IllegalArgumentException("not an array field");
      }

    return limit;
  }

  /**
   * <p>Set the maximum number of values allowed in this vector field.</p>
   *
   * @see arlut.csd.ganymede.BaseField 
   */

  public synchronized ReturnVal setMaxArraySize(short limit)
  {
    if (editor == null)
      {
	throw new IllegalArgumentException("not editing");
      }

    if (!array)
      {
	throw new IllegalArgumentException("not an array field");
      }

    this.limit = limit;

    if (isInUse())
      {
	return warning1;
      }
    else
      {
	return null;
      }
  }

  // **
  // boolean attribute methods
  // **

  /**
   * <p>Returns true if this is a boolean field with labels</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public boolean isLabeled()
  {
    if (!isBoolean())
      {
	throw new IllegalArgumentException("not a boolean field");
      }
    
    return labeled;
  }

  /**
   * <p>Turn labeled choices on/off for a boolean field.</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not a boolean type.</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public synchronized ReturnVal setLabeled(boolean b)
  {
    if (editor == null)
      {
	throw new IllegalArgumentException("not editing");
      }

    if (!isBoolean())
      {
	throw new IllegalArgumentException("not a boolean field");
      }
    
    labeled = b;

    return null;
  }

  /**
   * <p>Returns the true Label if this is a labeled boolean field</p> 
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not a labeled boolean type.</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public String getTrueLabel()
  {
    if (isLabeled())
      {
	return trueLabel;
      }

    throw new IllegalArgumentException("not a labeled boolean field");
  }

  /**
   * <p>Sets the label associated with the true choice for this
   * boolean field.</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not a labeled boolean type.</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public synchronized ReturnVal setTrueLabel(String label)
  {
    if (editor == null)
      {
	throw new IllegalArgumentException("not editing");
      }

    if (isLabeled())
      {
	trueLabel = label;
      }
    else
      {
	throw new IllegalArgumentException("not a labeled boolean field");
      }

    return null;
  }

  /**
   * <p>Returns the false Label if this is a labeled boolean field</p> 
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not a labeled boolean type.</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public String getFalseLabel()
  {
    if (isLabeled())
      {
	return falseLabel;
      }

    throw new IllegalArgumentException("not a labeled boolean field");
  }

  /**
   * <p>Sets the label associated with the false choice for this
   * boolean field.</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not a labeled boolean type.</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public synchronized ReturnVal setFalseLabel(String label)
  {
    if (editor == null)
      {
	throw new IllegalArgumentException("not editing");
      }

    if (isLabeled())
      {
	falseLabel = label;
      }
    else
      {
	throw new IllegalArgumentException("not a labeled boolean field");
      }

    return null;
  }

  // **
  // string attribute methods
  // **

  /**
   * <p>Returns the minimum acceptable string length if this is a string or
   * password field.</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not a string or password type.</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public short getMinLength()
  {
    if (!isString() && !isPassword())
      {
	throw new IllegalArgumentException("not a string field");
      }

    return minLength;
  }

  /**
   * <p>Sets the minimum acceptable length for this string or password field.</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not a string or password type.</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public synchronized ReturnVal setMinLength(short val)
  {
    if (editor == null)
      {
	throw new IllegalArgumentException("not editing");
      }

    if (!isString() && !isPassword())
      {
	throw new IllegalArgumentException("not a string field");
      }
    
    minLength = val;

    if (isInUse())
      {
	return warning1;
      }
    else
      {
	return null;
      }
  }

  /**
   * <p>Returns the maximum acceptable string length if this is a string 
   * or password field.</p>
   * 
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not a string or password type.</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public short getMaxLength()
  {
    if (!isString() && !isPassword())
      {
	throw new IllegalArgumentException("not a string field");
      }

    return maxLength;
  }

  /** 
   * <p>Sets the maximum acceptable length for this string or
   * password field.</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not a string or password type.</p>
   *
   * @see arlut.csd.ganymede.BaseField 
   */

  public synchronized ReturnVal setMaxLength(short val)
  {
    if (editor == null)
      {
	throw new IllegalArgumentException("not editing");
      }

    if (!isString() && !isPassword())
      {
	throw new IllegalArgumentException("not a string field");
      }
    
    maxLength = val;

    if (isInUse())
      {
	return warning1;
      }
    else
      {
	return null;
      }
  }

  /**
   * <p>Returns the set of acceptable characters if this is a string field.</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not a string or password type.</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public String getOKChars()
  {
    if (!isString() && !isPassword())
      {
	throw new IllegalArgumentException("not a string field");
      }

    return okChars;
  }

  /**
   * <p>Sets the set of characters that are allowed in this string or 
   * password field.  If s is null, all characters by default 
   * are acceptable.</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not a string or password type.</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public synchronized ReturnVal setOKChars(String s)
  {
    if (editor == null)
      {
	throw new IllegalArgumentException("not editing");
      }

    if (!isString() && !isPassword())
      {
	throw new IllegalArgumentException("not a string field");
      }

    okChars = s;

    if (isInUse())
      {
	return warning1;
      }
    else
      {
	return null;
      }
  }

  /**
   * <p>Returns the set of unacceptable characters if this is a 
   * string or password field.</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not a string or password type.</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public String getBadChars()
  {
    if (!isString() && !isPassword())
      {
	throw new IllegalArgumentException("not a string field");
      }


    return badChars;
  }

  /**
   * <p>Sets the set of characters that are specifically disallowed in
   * this string or password field.</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not a string or password type.</p>
   *
   * @see arlut.csd.ganymede.BaseField 
   */

  public synchronized ReturnVal setBadChars(String s)
  {
    if (editor == null)
      {
	throw new IllegalArgumentException("not editing");
      }

    if (!isString() && !isPassword())
      {
	throw new IllegalArgumentException("not a string field");
      }

    badChars = s;

    if (isInUse())
      {
	return warning1;
      }
    else
      {
	return null;
      }
  }

  /**
   * <p>Returns true if this string field is intended to be a multi-line
   * field.</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not a string type.</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public boolean isMultiLine()
  {
    if (!isString())
      {
	throw new IllegalArgumentException("not a string field");
      }

    return multiLine;
  }

  /**
   * <p>Sets whether or not this string field should be presented as a
   * multiline field.</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not a string type.</p>
   *
   * @see arlut.csd.ganymede.BaseField 
   */

  public synchronized ReturnVal setMultiLine(boolean b)
  {
    if (editor == null)
      {
	throw new IllegalArgumentException("not editing");
      }

    if (!isString())
      {
	throw new IllegalArgumentException("not a string field");
      }

    multiLine = b;

    return null;
  }

  /**
   * <p>Returns the regexp pattern string constraining this string
   * field.</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not a string type.</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public String getRegexpPat()
  {
    if (!isString())
      {
	throw new IllegalArgumentException("not a string field");
      }

    return regexpPat;
  }

  /**
   * <p>Sets the regexp pattern string constraining this string field.</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not a string type.</p>
   *
   * @see arlut.csd.ganymede.BaseField 
   */

  public synchronized ReturnVal setRegexpPat(String s)
  {
    if (editor == null && !loading)
      {
	throw new IllegalArgumentException("not editing");
      }

    if (!isString())
      {
	throw new IllegalArgumentException("not a string field");
      }

    if (s == null || s.equals(""))
      {
	regexpPat = null;
	regexp = null;

	return null;
      }
    else
      {
	try
	  {
	    regexp = new gnu.regexp.RE(s);
	  }
	catch (gnu.regexp.REException ex)
	  {
	    return Ganymede.createErrorDialog("Schema Editing Error",
					      "Bad regexp syntax.");
	  }

	regexpPat = s;

	if (isInUse())
	  {
	    return warning1;
	  }
	else
	  {
	    return null;
	  }
      }
  }

  /** 
   * <p>Returns the DBNameSpace that this string, numeric, or IP
   * field is associated with.</p> 
   */

  public DBNameSpace getNameSpace()
  {
    // several pieces of code have already been written to expect a null
    // value for a field's namespace if none is defined, regardless of
    // field type.  No need for us to be overly fastidious here.

    return namespace;
  }

  /**
   * <p>Returns the label of this string, numeric, or IP field's namespace.</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public String getNameSpaceLabel()
  {
    // several pieces of code have already been written to expect a null
    // value for a field's namespace if none is defined, regardless of
    // field type.  No need for us to be overly fastidious here.

    if (namespace != null)
      {
	return namespace.name;
      }
    else
      {
	return null;
      }
  }

  /**
   * <p>Set a namespace constraint for this string, numeric, or
   * IP field.</p>
   *
   * <p>Note that this is intended to be called from the Schema Editor,
   * and won't take effect until the next time the system is stopped
   * and reloaded.</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not a string, numeric, or IP type.</p>
   *
   * @see arlut.csd.ganymede.BaseField 
   */

  public synchronized ReturnVal setNameSpace(String nameSpaceId)
  {
    if (editor == null && !loading)
      {
	throw new IllegalArgumentException("not editing");
      }

    if (!isString() && !isNumeric() && !isIP())
      {
	throw new IllegalArgumentException("this field type does not accept a namespace constraint");
      }

    if (nameSpaceId == null)
      {
	namespace = null;
      }
    else if (nameSpaceId.equals(""))
      {
	namespace = null;
      }
    else
      {
	// this field is associated with a namespace.

	Enumeration values;
	DBNameSpace tmpNS;
	
	/* -- */
	
	values = base.store.nameSpaces.elements();
	namespace = null;

	while (values.hasMoreElements() && (namespace == null))
	  {
	    tmpNS = (DBNameSpace) values.nextElement();

	    if (tmpNS.getName().equalsIgnoreCase(nameSpaceId))
	      {
		namespace = tmpNS;
	      }
	  }

	// if we didn't find it, complain.

	if (namespace == null)
	  {
	    return Ganymede.createErrorDialog("Schema Editing Error",
					      "**** Unknown namespace id <" + 
					      nameSpaceId + "> specified for field " + 
					      base.toString() + ", field: " + toString());
	  }
      }

    if (isInUse())
      {
	return warning1;
      }
    else
      {
	return null;
      }
  }

  /**
   * <p>This method is used internally to set a namespace constraint.</p>
   *
   * <p>It does not appear that this method is currently used.. rather that
   * {@link arlut.csd.ganymede.DBObjectBaseField#setNameSpace(java.lang.String)
   * setNameSpace(string)} is.</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not a string, numeric, or IP type.</p>
   */

  ReturnVal setNameSpace(DBNameSpace namespace)
  {
    if (!isString() && !isNumeric() && !isIP())
      {
	throw new IllegalArgumentException("not a string/numeric field");
      }

    this.namespace = namespace;

    if (isInUse())
      {
	return warning1;
      }
    else
      {
	return null;
      }
  }

  // **
  // invid attribute methods
  // **

  /**
   * <p>Returns true if this is an invid field which is intended as an editInPlace
   * reference for the client's rendering.</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public boolean isEditInPlace()
  {
    return editInPlace;
  }

  /**
   * <p>Sets whether or not this field is intended as an editInPlace
   * reference for the client's rendering.</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not an invid type.</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public ReturnVal setEditInPlace(boolean b)
  {
    if (!isInvid())
      {
	throw new IllegalArgumentException("not an invid field");
      }
    else
      {
	if (isInUse())
	  {
	    return Ganymede.createErrorDialog("Error",
					      "Can't change the editInPlace status type of an " +
					      "invid field which is in use in the database.");
	  }

	editInPlace = b;
      }

    return null;
  }

  /**
   * <p>Returns true if this is a target restricted invid field</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not an invid type.</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public boolean isTargetRestricted()
  {
    if (!isInvid())
      {
	throw new IllegalArgumentException("not an invid field");
      }

    return (allowedTarget != -1);
  }

  /**
   * <p>Return the object type that this invid field is constrained to point to, if set</p>
   *
   * <p>-1 means there is no restriction on target type.</p>
   *
   * <p>-2 means there is no restriction on target type, but there is a specified symmetric field.</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not an invid type.</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public short getTargetBase()
  {
    if (!isInvid())
      {
	throw new IllegalArgumentException("not an invid field");
      }

    return allowedTarget;
  }

  /**
   * <p>Sets the allowed target object code of this invid field to &lt;val&gt;.
   * If val is -1, this invid field can point to objects of any type.</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not an invid type.</p>
   *
   * @see arlut.csd.ganymede.BaseField 
   */

  public synchronized ReturnVal setTargetBase(short val)
  {
    Base b;

    /* -- */

    if (editor == null)
      {
	throw new IllegalArgumentException("not editing");
      }

    if (!isInvid())
      {
	throw new IllegalArgumentException("not an invid field");
      }

    if (val < 0)
      {
	allowedTarget = val;
	return null;
      }
    
    b = base.editor.getBase(val);

    if (b != null)
      {
	allowedTarget = val;
      }
    else
      {
	throw new IllegalArgumentException("not a valid base id");
      }

    if (isInUse())
      {
	return warning2;
      }
    else
      {
	return null;
      }
  }

  /**
   * <p>Sets the allowed target object code of this invid field to &lt;baseName&gt;.
   * If val is null, this invid field can point to objects of any type.</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not an invid type.</p>
   *
   * @see arlut.csd.ganymede.BaseField 
   */

  public synchronized ReturnVal setTargetBase(String baseName)
  {
    Base b;

    /* -- */

    if (editor == null)
      {
	throw new IllegalArgumentException("not editing");
      }

    if (!isInvid())
      {
	throw new IllegalArgumentException("not an invid field");
      }

    if (baseName == null)
      {
	allowedTarget = -1;
	return null;
      }

    b = editor.getBase(baseName);

    try
      {
	if (b != null)
	  {
	    allowedTarget = b.getTypeID();
	  }
	else
	  {
	    throw new IllegalArgumentException("not a valid base name");
	  }
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("caught remote except: " + ex);
      }

    if (isInUse())
      {
	return warning2;
      }
    else
      {
	return null;
      }
  }

  /**
   * <p>If this field is a target restricted invid field, this method will return
   * true if this field has a symmetry relationship to the target</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not an invid type.</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public boolean isSymmetric()
  {
    if (!isInvid())
      {
	throw new IllegalArgumentException("not an invid field");
      }

    return ((allowedTarget != -1) && (targetField != -1));
  }

  /**
   * <p>If this field is a target restricted invid field, this method will return
   * a short indicating the field in the target object that the symmetry relation
   * applies to.</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not an invid type.</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public short getTargetField()
  {
    if (!isInvid())
      {
	throw new IllegalArgumentException("not an invid field");
      }

    return targetField;
  }

  /**
   * <p>Sets the field of the target object of this invid field that should
   * be managed in the symmetry relationship if isSymmetric().  If
   * val == -1, the targetField will be set to a value representing
   * no selection.</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not an invid type.</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public synchronized ReturnVal setTargetField(short val)
  {
    Base b;
    BaseField bF;

    /* -- */

    if (editor == null)
      {
	throw new IllegalArgumentException("not editing");
      }

    if (!isInvid())
      {
	throw new IllegalArgumentException("not an invid field");
      }

    if (val < 0)
      {
	targetField = val;
	return null;
      }

    if (allowedTarget == -1)
      {
	throw new IllegalArgumentException("not a symmetry maintained field");
      }

    try
      {
	b = editor.getBase(allowedTarget);

	if (b == null)
	  {
	    throw new IllegalArgumentException("invalid target base");
	  }
	
	bF = b.getField(val);

	if (bF == null)
	  {
	    throw new IllegalArgumentException("invalid target field in base " + b.getName());
	  }
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("caught remote: " + ex);
      }

    targetField = val;

    if (isInUse())
      {
	return warning2;
      }
    else
      {
	return null;
      }
  }

  /**
   * <p>Sets the field of the target object of this invid field that should
   * be managed in the symmetry relationship if isSymmetric().  If &lt;fieldName&gt;
   * is null, the targetField will be cleared.</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not an invid type.</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public synchronized ReturnVal setTargetField(String fieldName)
  {
    Base b;
    BaseField bF;

    /* -- */

    if (editor == null)
      {
	throw new IllegalArgumentException("not editing");
      }

    if (!isInvid())
      {
	throw new IllegalArgumentException("not an invid field");
      }

    if (fieldName == null)
      {
	targetField = -1;
	return null;
      }

    // look for fieldName in the base currently specified in
    // allowedTarget

    if (allowedTarget == -1)
      {
	throw new IllegalArgumentException("not a symmetry maintained field");
      }

    b = editor.getBase(allowedTarget);

    try
      {
	if (b == null)
	  {
	    throw new IllegalArgumentException("invalid target base");
	  }
	
	bF = b.getField(fieldName);

	if (bF == null)
	  {
	    throw new IllegalArgumentException("invalid target field in base " + b.getName());
	  }

	targetField = bF.getID();
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("caught remote: " + ex);
      }

    if (isInUse())
      {
	return warning2;
      }
    else
      {
	return null;
      }
  }

  /**
   * <p>This method returns true if this is a password field that
   * stores passwords in UNIX crypt format, and can thus accept
   * pre-crypted passwords.</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public boolean isCrypted()
  {
    return crypted;
  }

  /**
   * <p>This method is used to specify that this password field
   * should store passwords in UNIX crypt format.  If passwords
   * are stored in UNIX crypt format, they will not be kept in
   * plaintext on disk, regardless of the setting of setPlainText().</p>
   *
   * <p>setCrypted() is not mutually exclusive with setMD5Crypted().</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not a password type.</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public ReturnVal setCrypted(boolean b)
  {    
    if (editor == null)
      {
	throw new IllegalArgumentException("not editing");
      }

    if (!isPassword())
      {
	throw new IllegalArgumentException("not an password field");
      }

    crypted = b;

    return null;
  }

  /** 
   * <p>This method returns true if this is a password field that
   * stores passwords in OpenBSD/FreeBSD/PAM md5crypt() format, and
   * can thus accept pre-crypted passwords.</p>
   *
   * @see arlut.csd.ganymede.BaseField 
   */

  public boolean isMD5Crypted()
  {
    return md5crypted;
  }

  /**
   * <p>This method is used to specify that this password field should
   * store passwords in OpenBSD/FreeBSD/PAM md5crypt() format.  If
   * passwords are stored in md5crypt() format, they will not be kept
   * in plaintext on disk, regardless of the setting of setPlainText().</p>
   *
   * <p>setMD5Crypted() is not mutually exclusive with setCrypted().</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not a password type.</p>
   *
   * @see arlut.csd.ganymede.BaseField 
   */

  public ReturnVal setMD5Crypted(boolean b)
  {    
    if (editor == null)
      {
	throw new IllegalArgumentException("not editing");
      }

    if (!isPassword())
      {
	throw new IllegalArgumentException("not an password field");
      }

    md5crypted = b;

    return null;
  }

  /**
   * <p>This method returns true if this is a password field that
   * will keep a copy of the password in plaintext.</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public boolean isPlainText()
  {
    return storePlaintext;
  }

  /**
   * <p>This method is used to specify that this password field
   * should keep a copy of the password in plaintext, in
   * addition to a UNIX crypted copy.  If crypted is
   * false, plaintext will be treated as true, whether
   * or not this is explicitly set by the schema editor.</p>
   *
   * <p>If crypted or md5crypted is true, fields of this type will never retain
   * the plaintext password information on disk.  Plaintext 
   * password information will only be retained in the on-disk
   * ganymede.db file if crypted and md5crypted are both false.</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not a password type.</p>
   *
   * @see arlut.csd.ganymede.BaseField
   */

  public ReturnVal setPlainText(boolean b)
  {    
    if (editor == null)
      {
	throw new IllegalArgumentException("not editing");
      }

    if (!isPassword())
      {
	throw new IllegalArgumentException("not a password field");
      }

    storePlaintext = b;

    return null;
  }

  // general convenience methods

  /**
   * <p>This method is intended to produce a human readable
   * representation of this field definition's type attributes.  This
   * method should not be used programatically to determine this
   * field's type information.</p>
   *
   * <p>This method is only for human information, and the precise
   * results returned are subject to change at any time.</p>
   *
   * @see arlut.csd.ganymede.BaseField 
   */

  public String getTypeDesc()
  {
    String result;

    switch (field_type)
      {
      case BOOLEAN:
	result = "boolean";
	break;

      case NUMERIC:
	result = "numeric";
	break;

      case FLOAT:
	result = "float";
	break;

      case DATE:
	result = "date";
	break;

      case STRING:

	result = "string <min: " + minLength + ", max:" + maxLength + ">";
	
	if (okChars != null)
	  {
	    result += ", okChars: '" + okChars + "'";
	  }

	if (badChars != null)
	  {
	    result += ", badChars: '" + badChars + "'";
	  }

	if (namespace != null)
	  {
	    result += ", namespace: " + namespace.getName();
	  }

	if (regexpPat != null)
	  {
	    result += ", regexpPat: '" + regexpPat + "'";
	  }

	break;

      case INVID:
	result = "invid";

	if (editInPlace)
	  {
	    result += " <edit-in-place> ";
	  }

	if (allowedTarget >= 0)
	  {
	    DBObjectBase refBase;

	    refBase = base.store.getObjectBase(allowedTarget);

	    if (refBase != null)
	      {
		result += ", --> [" + refBase.getName() + "] ";
		
		if (targetField != -1)
		  {
		    try
		      {
			result += ", <-- [" + refBase.getField(targetField).getName() + "] ";
		      }
		    catch (RemoteException ex)
		      {
			throw new RuntimeException("caught remote: " + ex);
		      }
		    catch (NullPointerException ex)
		      {
			result += ", <-- [INVALID FIELD TARGET!!] ";
		      }
		  }
	      }
	    else
	      {
		result += ", --> [INVALID BASE!!] ";
	      }
	  }
	else if (allowedTarget == -1)
	  {
	    result += ", --> [any]";
	  }
	else if (allowedTarget == -2)
	  {
	    result += ", --> [any]";

	    // if allowed Target == -2 and targetField != -1, we assume
	    // that we've got a field that's guaranteed to be present in
	    // all bases, including our parent.
	    
	    if (targetField != -1)
	      {
		try
		  {
		    result += ", <-- [" + base.getField(targetField).getName() + "] ";
		  }
		catch (RemoteException ex)
		  {
		    throw new RuntimeException("caught remote: " + ex);
		  }
	      }
	  }
	
	break;

      case PERMISSIONMATRIX:
	result = "permission matrix";
	break;

      case PASSWORD:
	result = "password";

	if (crypted)
	  {
	    result += " <crypted>";
	  }

	if (md5crypted)
	  {
	    result += " <md5 crypted>";
	  }

	if (storePlaintext)
	  {
	    result += " <plaintext>";
	  }

	break;

      case IP:
	result = "i.p. field";
	break;

      default:
	result = "<<bad type code>>";
      }

    if (array)
      {
	return result + " array [" + limit + "]";
      }
    else
      {
	return result;
      }
  }

  /**
   * <p>This method is intended to produce a human readable
   * representation of this field definition's type attributes.  This
   * method should not be used programatically to determine this
   * field's type information.</p>
   *
   * <p>This method is only for human elucidation, and the precise
   * results returned are subject to change at any time.</p>
   *
   * @see arlut.csd.ganymede.BaseField 
   */

  public String getTypeDescHTML()
  {
    String result;

    switch (field_type)
      {
      case BOOLEAN:
	result = "<td>boolean</td>";
	break;

      case NUMERIC:
	result = "<td>numeric</td>";
	break;

      case FLOAT:
	result = "<td>float</td>";
	break;

      case DATE:
	result = "<td>date</td>";
	break;

      case STRING:
	result = "<td>string</td>";
	break;

      case INVID:
	result = "<td>invid</td>";
	break;

      case PERMISSIONMATRIX:
	result = "<td>permission matrix</td>";
	break;

      case PASSWORD:
	result = "<td>password</td>";
	break;

      case IP:
	result = "<td>i.p. field</td>";
	break;

      default:
	result = "<td>&lt;&lt;bad type code&gt;&gt;</td>";
      }

    if (array)
      {
	result += "<td>[0.." + limit + "]</td>";
      }
    else
      {
	result += "<td><FONT COLOR=\"#FF0000\">N</font></td>";
      }

    if (namespace != null)
      {
	result += "<td>" + namespace.getName() + "</td>";
      }
    else
      {
	result += "<td><FONT COLOR=\"#FF0000\">N</font></td>";
      }

    // generate the notes field

    result += "<td>";

    switch (field_type)
      {
      case STRING:

	result += "min: " + minLength + ", max: " + maxLength;

	if (okChars != null)
	  {
	    result += " okChars: '" + okChars + "'";
	  }

	if (badChars != null)
	  {
	    result += " badChars: '" + badChars + "'";
	  }

	if (regexpPat != null)
	  {
	    result += " regexpPat: '" + regexpPat + "'";
	  }
	
	break;
	
      case INVID:

	if (editInPlace)
	  {
	    result += "edit-in-place ";
	  }

	if (allowedTarget >= 0)
	  {
	    DBObjectBase refBase;

	    refBase = base.store.getObjectBase(allowedTarget);

	    if (refBase == null)
	      {
		result += "targets [INVALID OBJECT TYPE]";
	      }
	    else
	      {
		result += "targets [" + refBase.getName() + "] ";

		if (targetField != -1)
		  {
		    try
		      {
			result += "reverse link [" + refBase.getField(targetField).getName() + "] ";
		      }
		    catch (RemoteException ex)
		      {
			throw new RuntimeException("caught remote: " + ex);
		      }
		  }
	      }
	  }
	else if (allowedTarget == -1)
	  {
	    result += "targets [any]";
	  }
	else if (allowedTarget == -2)
	  {
	    result += "targets [any] ";

	    // if allowed Target == -2 and targetField != -1, we assume
	    // that we've got a field that's guaranteed to be present in
	    // all bases, including our parent.
	    
	    if (targetField != -1)
	      {
		try
		  {
		    result += "reverse link [" + base.getField(targetField).getName() + "] ";
		  }
		catch (RemoteException ex)
		  {
		    throw new RuntimeException("caught remote: " + ex);
		  }
	      }

	  }
	break;

      case PASSWORD:

	if (crypted)
	  {
	    result += "crypted";
	  }

	if (md5crypted)
	  {
	    result += " md5crypted";
	  }

	if (storePlaintext)
	  {
	    result += " plaintext";
	  }

	break;

      default:
	break;
      }
    
    result += "</td>";
    return result;
  }

  /**
   * <p>This method is used when the Ganymede server dumps its schema.
   * It prints an HTML description of this field type to the PrintWriter
   * specified.</p>
   *
   * <p>This method was written in concert with the other DBStore objects'
   * printHTML methods, and assumes that it will be run in the context
   * of the full DBStore.printCategoryTreeHTML() method.</p>
   */

  public void printHTML(PrintWriter out)
  {
    out.print("<td>" + field_name + "</td><td>" + field_code + "</td>");
    out.print(getTypeDescHTML());
    out.println();
  }

  /**
   * <p>This method is used when the Ganymede server dumps its schema.
   * It prints an ASCII description of this field type to the PrintWriter
   * specified.</p>
   *
   * <p>This method was written in concert with the other DBStore objects'
   * print methods, and assumes that it will be run in the context
   * of the full DBStore.printBases() method.</p>
   */

  public void print(PrintWriter out, String indent)
  {
    out.print(indent + field_name + "(" + field_code + "):");
    out.print(indent + getTypeDesc());
    out.println();
  }

  public String toString()
  {
    return base.getName() + ":" + field_name;
  }

  private static ReturnVal genWarning1()
  {
    ReturnVal retVal = new ReturnVal(true);
    retVal.setDialog(new JDialogBuff("Schema Editor",
				     "The requested change in this field's " +
				     "allowed options has been made " +
				     "and will be put into effect if you commit " +
				     "your schema change.\n\n" +
				     "This schema change will only affect new values " +
				     "entered into this field in the database.  Pre-existing " +
				     "fields of this kind in the database may or may not " +
				     "satisfy your new constraint.",
				     "OK",
				     null,
				     "ok.gif"));

    return retVal;
  }

  private static ReturnVal genWarning2()
  {
    ReturnVal retVal = new ReturnVal(true);
    retVal.setDialog(new JDialogBuff("Schema Editor",
				     "The requested change in this field's " +
				     "allowed options has been made " +
				     "and will be put into effect if you commit " +
				     "your schema change.\n\n" +
				     "Because this schema change is being made while " +
				     "there are fields of this type active in the database, there " +
				     "may be a chance that this change will affect database " +
				     "consistency.",
				     "OK",
				     null,
				     "ok.gif"));

    return retVal;
  }
}
