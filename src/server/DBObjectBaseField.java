/*
   GASH 2

   DBObjectBaseField.java

   The GANYMEDE object storage system.

   Created: 27 August 1996
   Release: $Name:  $
   Version: $Revision: 1.77 $
   Last Mod Date: $Date: 2000/10/29 09:09:45 $
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
    
    field_code = -1;
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
   * Receive constructor, for binary loading from ganymede.db.
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

    // we use setName to filter out any fieldname chars that wouldn't
    // be acceptable as an XML entity name character.

    setName(in.readUTF());	
    field_code = in.readShort();
    field_type = in.readShort();
    setClassName(in.readUTF());
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

  synchronized void emitXML(XMLDumpContext xmlOut) throws IOException
  {
    boolean nonEmpty = false;

    /* -- */

    xmlOut.startElementIndent("fielddef");
    xmlOut.attribute("name", XMLUtils.XMLEncode(field_name));
    xmlOut.attribute("id", java.lang.Short.toString(field_code));

    xmlOut.indentOut();

    if (classname != null && !classname.equals(""))
      {
	xmlOut.startElementIndent("classname");
	xmlOut.attribute("name", classname);
	xmlOut.endElement("classname");
      }

    if (comment != null && !comment.equals(""))
      {
	xmlOut.startElementIndent("comment");
	xmlOut.write(comment);
	xmlOut.endElement("comment");
      }

    if (!visibility)
      {
	xmlOut.startElementIndent("invisible");
	xmlOut.endElement("invisible");
      }

    xmlOut.startElementIndent("typedef");
    
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

    xmlOut.indentOut();

    if (array)
      {
	nonEmpty = true;
	xmlOut.startElementIndent("vector");

	// A limit of 32767 does not need to be specified, as that is
	// the highest vector size allowed, and so is implicit.

	if (limit != Short.MAX_VALUE)
	  {
	    xmlOut.attribute("maxSize", java.lang.Short.toString(limit));
	  }

	xmlOut.endElement("vector");
      }

    if (isBoolean())
      {
	if (labeled)
	  {
	    nonEmpty = true;
	    xmlOut.startElementIndent("labeled");
	    xmlOut.attribute("true", trueLabel);
	    xmlOut.attribute("false", falseLabel);
	    xmlOut.endElement("labeled");
	  }
      }
    else if (isString())
      {
	nonEmpty = true;

	if (minLength != 0)
	  {
	    xmlOut.startElementIndent("minlength");
	    xmlOut.attribute("val", java.lang.Short.toString(minLength));
	    xmlOut.endElement("minlength");
	  }

	// A limit of 32767 does not need to be specified, as that is
	// the largest string size allowed, and so is implicit.

	if (maxLength != Short.MAX_VALUE)
	  {
	    xmlOut.startElementIndent("maxlength");
	    xmlOut.attribute("val", java.lang.Short.toString(maxLength));
	    xmlOut.endElement("maxlength");
	  }

	if (okChars != null && !okChars.equals(""))
	  {
	    xmlOut.startElementIndent("okchars");
	    xmlOut.attribute("val", okChars);
	    xmlOut.endElement("okchars");
	  }

	if (badChars != null && !badChars.equals(""))
	  {
	    xmlOut.startElementIndent("badchars");
	    xmlOut.attribute("val", badChars);
	    xmlOut.endElement("badchars");
	  }
	
	if (namespace != null)
	  {
	    xmlOut.startElementIndent("namespace");
	    xmlOut.attribute("val", namespace.getName());
	    xmlOut.endElement("namespace");
	  }

	if (regexpPat != null && !regexpPat.equals(""))
	  {
	    xmlOut.startElementIndent("regexp");
	    xmlOut.attribute("val", regexpPat);
	    xmlOut.endElement("regexp");
	  }

	if (multiLine)
	  {
	    xmlOut.startElementIndent("multiline");
	    xmlOut.endElement("multiline");
	  }
      }
    else if (isNumeric())
      {
	if (namespace != null)
	  {
	    nonEmpty = true;

	    xmlOut.startElementIndent("namespace");
	    xmlOut.attribute("val", namespace.getName());
	    xmlOut.endElement("namespace");
	  }
      }
    else if (isIP())
      {
	if (namespace != null)
	  {
	    nonEmpty = true;

	    xmlOut.startElementIndent("namespace");
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

	    xmlOut.startElementIndent("targetobject");

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
		    xmlOut.attribute("name", XMLUtils.XMLEncode(targetObjectName));
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

		xmlOut.startElementIndent("targetfield");

		if (targetObjectBase != null)
		  {
		    DBObjectBaseField targetFieldDef = (DBObjectBaseField) targetObjectBase.getField(targetField);

		    if (targetFieldDef != null)
		      {
			xmlOut.attribute("name", XMLUtils.XMLEncode(targetFieldDef.getName()));
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
		xmlOut.startElementIndent("embedded");
		xmlOut.endElement("embedded");
	      }
	  }
      }
    else if (isPassword())
      {
	nonEmpty = true;

	if (minLength != 0)
	  {
	    xmlOut.startElementIndent("minlength");
	    xmlOut.attribute("val", java.lang.Short.toString(minLength));
	    xmlOut.endElement("minlength");
	  }
	
	if (maxLength != Short.MAX_VALUE)
	  {
	    xmlOut.startElementIndent("maxlength");
	    xmlOut.attribute("val", java.lang.Short.toString(maxLength));
	    xmlOut.endElement("maxlength");
	  }

	if (okChars != null && !okChars.equals(""))
	  {
	    xmlOut.startElementIndent("okchars");
	    xmlOut.attribute("val", okChars);
	    xmlOut.endElement("okchars");
	  }

	if (badChars != null && !badChars.equals(""))
	  {
	    xmlOut.startElementIndent("badchars");
	    xmlOut.attribute("val", badChars);
	    xmlOut.endElement("badchars");
	  }

	if (crypted)
	  {
	    xmlOut.startElementIndent("crypted");
	    xmlOut.endElement("crypted");
	  }

	if (md5crypted)
	  {
	    xmlOut.startElementIndent("md5crypted");
	    xmlOut.endElement("md5crypted");
	  }

	if (storePlaintext)
	  {
	    xmlOut.startElementIndent("plaintext");
	    xmlOut.endElement("plaintext");
	  }
      }
    
    xmlOut.indentIn();

    if (nonEmpty)
      {
	xmlOut.indent();
      }

    xmlOut.endElement("typedef");

    xmlOut.indentIn();
    xmlOut.endElementIndent("fielddef");
  }

  /**
   * <P>This method is used to read the definition for this
   * DBObjectBaseField from a &lt;fielddef&gt; XMLItem tree.</P>
   */

  synchronized ReturnVal setXML(XMLItem root, boolean doLinkResolve)
  {
    XMLItem item, nextItem;
    Integer field_codeInt;
    boolean typeRead = false;
    ReturnVal retVal = null;
    boolean vectorSet = false;

    /* -- */

    if (root == null || !root.matches("fielddef"))
      {
	throw new IllegalArgumentException("DBObjectBaseField.receiveXML(): next element != open fielddef: " +
					   root);
      }

    retVal = setName(root.getAttrStr("name"));

    if (retVal != null && !retVal.didSucceed())
      {
	return Ganymede.createErrorDialog("xml",
					  "fielddef could not have its name set: \n" +
					  root.getTreeString() + "\n" +
					  retVal.getDialogText());
      }

    field_codeInt = root.getAttrInt("id");

    if (field_codeInt == null)
      {
	return Ganymede.createErrorDialog("xml",
					  "fielddef does not define id attr: \n" + 
					  root.getTreeString());
      }

    // extract the short

    retVal = setID(field_codeInt.shortValue());

    if (retVal != null && !retVal.didSucceed())
      {
	return Ganymede.createErrorDialog("xml",
					  "fielddef could not have its id set: \n" +
					  root.getTreeString() + "\n" +
					  retVal.getDialogText());
      }

    // look at the nodes under root, set up this field def based on
    // them

    XMLItem children[] = root.getChildren();

    for (int i = 0; i < children.length; i++)
      {
	item = children[i];

	if (item.matches("classname"))
	  {
	    setClassName(item.getAttrStr("name"));
	  }
	else if (item.matches("comment"))
	  {
	    XMLItem commentChildren[] = item.getChildren();

	    if (commentChildren == null)
	      {
		comment = null;
		continue;
	      }

	    if (commentChildren.length != 1)
	      {
		return Ganymede.createErrorDialog("xml",
						  "unrecognized children in comment block: \n" + 
						  root.getTreeString());
	      }

	    retVal = setComment(commentChildren[0].getString());

	    if (retVal != null && !retVal.didSucceed())
	      {
		return Ganymede.createErrorDialog("xml",
						  "fielddef could not set comment: \n" +
						  root.getTreeString() + "\n" +
						  retVal.getDialogText());
	      }
	  }
	else if (item.matches("invisible"))
	  {
	    visibility = false;	// need a setter?
	  }
	else if (item.matches("typedef"))
	  {
	    if (typeRead)
	      {
		return Ganymede.createErrorDialog("xml",
						  "redundant type definition for this field: \n" + 
						  root.getTreeString());
	      }

	    if (item.getAttrStr("type") == null)
	      {
		return Ganymede.createErrorDialog("xml",
						  "typedef tag does not contain type attribute: \n" + 
						  root.getTreeString());
	      }

	    if (item.getAttrStr("type").equals("float"))
	      {
		// float has no subchildren, all we need to
		// do is attempt to do a setType

		retVal = setType(FieldType.FLOAT);

		if (retVal != null && !retVal.didSucceed())
		  {
		    return retVal;
		  }
	      }
	    else if (item.getAttrStr("type").equals("date"))
	      {
		// date has no subchildren, all we need to
		// do is attempt to do a setType

		retVal = setType(FieldType.DATE);

		if (retVal != null && !retVal.didSucceed())
		  {
		    return retVal;
		  }
	      }
	    else if (item.getAttrStr("type").equals("permmatrix"))
	      {
		// permmatrix has no subchildren, all we need to
		// do is attempt to do a setType

		retVal = setType(FieldType.PERMISSIONMATRIX);

		if (retVal != null && !retVal.didSucceed())
		  {
		    return retVal;
		  }
	      }
	    else if (item.getAttrStr("type").equals("string"))
	      {
		retVal = doStringXML(item);

		if (retVal != null && !retVal.didSucceed())
		  {
		    return retVal;
		  }
	      }
	    else if (item.getAttrStr("type").equals("invid"))
	      {
		retVal = doInvidXML(item, doLinkResolve);

		if (retVal != null && !retVal.didSucceed())
		  {
		    return retVal;
		  }
	      }
	    else if (item.getAttrStr("type").equals("numeric"))
	      {
		retVal = doNumericXML(item);

		if (retVal != null && !retVal.didSucceed())
		  {
		    return retVal;
		  }
	      }
	    else if (item.getAttrStr("type").equals("password"))
	      {
		retVal = doPasswordXML(item);

		if (retVal != null && !retVal.didSucceed())
		  {
		    return retVal;
		  }
	      }
	    else if (item.getAttrStr("type").equals("ip"))
	      {		
		retVal = doIPXML(item);

		if (retVal != null && !retVal.didSucceed())
		  {
		    return retVal;
		  }
	      }
	    else if (item.getAttrStr("type").equals("boolean"))
	      {
		retVal = doBooleanXML(item);

		if (retVal != null && !retVal.didSucceed())
		  {
		    return retVal;
		  }
	      }
	    else
	      {
		throw new IllegalArgumentException("typedef tag does not contain type attribute: " +
						   item);
	      }

	    typeRead = true;
	  }
	else
	  {
	    System.err.println("DBObjectBaseField.receiveXML(): unrecognized XML item inside fielddef: " +
			       item);
	  }
      }

    return null;
  }

  /**
   * <p>This method takes care of doing everything required to
   * take an XMLItem tree &lt;fielddef type="string"&gt; and
   * update this field's schema information to match.</p>
   *
   * @return A failure ReturnVal if the schema for this field
   * could not be set to match.
   */

  private ReturnVal doStringXML(XMLItem root)
  {
    boolean _vect = false;
    short _maxSize = java.lang.Short.MAX_VALUE;
    short _minlength = -1;
    short _maxlength = -1;
    String _okChars = null;
    String _badChars = null;
    String _regexp = null;
    boolean _multiline = false;
    String _namespace = null;
    ReturnVal retVal;

    /* -- */

    if (!root.getAttrStr("type").equals("string"))
      {
	throw new IllegalArgumentException("bad XMLItem tree");
      }

    retVal = setType(FieldType.STRING);

    if (retVal != null && !retVal.didSucceed())
      {
	return retVal;
      }

    // the <typedef type="string"> node can have children
    // of its own

    XMLItem typeChildren[] = root.getChildren();
		
    for (int j = 0; j < typeChildren.length; j++)
      {
	XMLItem child = typeChildren[j];

	if (child.matches("vector"))
	  {
	    _vect = true;

	    Integer vectSize = child.getAttrInt("maxSize");

	    if (vectSize != null)
	      {
		_maxSize = vectSize.shortValue();
	      }
	  }
	else if (child.matches("minlength"))
	  {
	    Integer val = child.getAttrInt("val");

	    if (val != null)
	      {
		_minlength = val.shortValue();
	      }
	  }
	else if (child.matches("maxlength"))
	  {
	    Integer val = child.getAttrInt("val");

	    if (val != null)
	      {
		_maxlength = val.shortValue();
	      }
	  }
	else if (child.matches("okchars"))
	  {
	    _okChars = child.getAttrStr("val");
	  }
	else if (child.matches("badchars"))
	  {
	    _badChars = child.getAttrStr("val");
	  }
	else if (child.matches("regexp"))
	  {
	    _regexp = child.getAttrStr("val");
	  }
	else if (child.matches("multiline"))
	  {
	    _multiline = true;
	  }
	else if (child.matches("namespace"))
	  {
	    _namespace = child.getAttrStr("val");
	  }
	else
	  {
	    return Ganymede.createErrorDialog("xml",
					      "Unrecognized string typedef entity: " + child +
					      "\nIn field def:\n" + root.getTreeString());
	  }
      }

    // now do all the setting

    retVal = setArray(_vect);

    if (retVal != null && !retVal.didSucceed())
      {
	return Ganymede.createErrorDialog("xml",
					  "fielddef could not set vector bit to " + _vect + ": \n" +
					  root.getTreeString() + "\n" +
					  retVal.getDialogText());
      }
		
    if (_vect)
      {
	retVal = setMaxArraySize(_maxSize);
	
	if (retVal != null && !retVal.didSucceed())
	  {
	    return Ganymede.createErrorDialog("xml",
					      "fielddef could not set vector maximum size: " + _maxSize + "\n" +
					      root.getTreeString() + "\n" +
					      retVal.getDialogText());
	  }
      }

    retVal = setMinLength(_minlength);

    if (retVal != null && !retVal.didSucceed())
      {
	return Ganymede.createErrorDialog("xml",
					  "fielddef could not set min length: " + _minlength + "\n" +
					  root.getTreeString() + "\n" +
					  retVal.getDialogText());
      }

    retVal = setMaxLength(_maxlength);

    if (retVal != null && !retVal.didSucceed())
      {
	return Ganymede.createErrorDialog("xml",
					  "fielddef could not set max length: " + _maxlength + "\n" +
					  root.getTreeString() + "\n" +
					  retVal.getDialogText());
      }

    retVal = setOKChars(_okChars);
    
    if (retVal != null && !retVal.didSucceed())
      {
	return Ganymede.createErrorDialog("xml",
					  "fielddef could not set ok chars: " + _okChars + "\n" +
					  root.getTreeString() + "\n" +
					  retVal.getDialogText());
      }
    
    retVal = setBadChars(_badChars);
    
    if (retVal != null && !retVal.didSucceed())
      {
	return Ganymede.createErrorDialog("xml",
					  "fielddef could not set bad chars: " + _badChars + "\n" +
					  root.getTreeString() + "\n" +
					  retVal.getDialogText());
      }

    retVal = setRegexpPat(_regexp);
    
    if (retVal != null && !retVal.didSucceed())
      {
	return Ganymede.createErrorDialog("xml",
					  "fielddef could not set regular expression: " + _regexp + "\n" +
					  root.getTreeString() + "\n" +
					  retVal.getDialogText());
      }

    retVal = setMultiLine(_multiline);
    
    if (retVal != null && !retVal.didSucceed())
      {
	return Ganymede.createErrorDialog("xml",
					  "fielddef could not set multiline: " + _multiline + "\n" +
					  root.getTreeString() + "\n" +
					  retVal.getDialogText());
      }

    retVal = setNameSpace(_namespace);

    if (retVal != null && !retVal.didSucceed())
      {
	return Ganymede.createErrorDialog("xml",
					  "fielddef could not set namespace: " + _namespace + "\n" +
					  root.getTreeString() + "\n" +
					  retVal.getDialogText());
      }

    return null;
  }

  /**
   * <p>This method takes care of doing everything required to
   * take an XMLItem tree &lt;fielddef type="boolean"&gt; and
   * update this field's schema information to match.</p>
   *
   * @return A failure ReturnVal if the schema for this field
   * could not be set to match.
   */

  private ReturnVal doBooleanXML(XMLItem root)
  {
    boolean _labeled = false;
    String _trueLabel = null;
    String _falseLabel = null;
    ReturnVal retVal;

    /* -- */

    if (!root.getAttrStr("type").equals("boolean"))
      {
	throw new IllegalArgumentException("bad XMLItem tree");
      }

    retVal = setType(FieldType.BOOLEAN);

    if (retVal != null && !retVal.didSucceed())
      {
	return retVal;
      }

    // the <typedef type="boolean"> node can have children
    // of its own

    XMLItem typeChildren[] = root.getChildren();
		
    for (int j = 0; j < typeChildren.length; j++)
      {
	XMLItem child = typeChildren[j];

	if (child.matches("labeled"))
	  {
	    _labeled = true;
	    _trueLabel = child.getAttrStr("true");
	    _falseLabel = child.getAttrStr("true");
	  }
	else
	  {
	    return Ganymede.createErrorDialog("xml",
					      "Unrecognized boolean typedef entity: " + child +
					      "\nIn field def:\n" + root.getTreeString());
	  }
      }

    // now do all the setting

    retVal = setLabeled(_labeled);

    if (retVal != null && !retVal.didSucceed())
      {
	return Ganymede.createErrorDialog("xml",
					  "fielddef could not set labeled bit to " + _labeled + ": \n" +
					  root.getTreeString() + "\n" +
					  retVal.getDialogText());
      }
		
    if (_labeled)
      {
	retVal = setTrueLabel(_trueLabel);
	
	if (retVal != null && !retVal.didSucceed())
	  {
	    return Ganymede.createErrorDialog("xml",
					      "fielddef could not set true label to: " + _trueLabel + "\n" +
					      root.getTreeString() + "\n" +
					      retVal.getDialogText());
	  }

	retVal = setFalseLabel(_falseLabel);
	
	if (retVal != null && !retVal.didSucceed())
	  {
	    return Ganymede.createErrorDialog("xml",
					      "fielddef could not set false label to: " + _falseLabel + "\n" +
					      root.getTreeString() + "\n" +
					      retVal.getDialogText());
	  }
      }

    return null;
  }

  /**
   * <p>This method takes care of doing everything required to
   * take an XMLItem tree &lt;fielddef type="password"&gt; and
   * update this field's schema information to match.</p>
   *
   * @return A failure ReturnVal if the schema for this field
   * could not be set to match.
   */

  private ReturnVal doPasswordXML(XMLItem root)
  {
    short _minlength = -1;
    short _maxlength = -1;
    String _okChars = null;
    String _badChars = null;
    boolean _crypted = false;
    boolean _plaintext = false;
    boolean _md5crypted = false;
    ReturnVal retVal;

    /* -- */

    if (!root.getAttrStr("type").equals("password"))
      {
	throw new IllegalArgumentException("bad XMLItem tree");
      }

    retVal = setType(FieldType.PASSWORD);

    if (retVal != null && !retVal.didSucceed())
      {
	return retVal;
      }

    // the <typedef type="password"> node can have children
    // of its own

    XMLItem typeChildren[] = root.getChildren();
		
    for (int j = 0; j < typeChildren.length; j++)
      {
	XMLItem child = typeChildren[j];

	if (child.matches("minlength"))
	  {
	    Integer val = child.getAttrInt("val");

	    if (val != null)
	      {
		_minlength = val.shortValue();
	      }
	  }
	else if (child.matches("maxlength"))
	  {
	    Integer val = child.getAttrInt("val");

	    if (val != null)
	      {
		_maxlength = val.shortValue();
	      }
	  }
	else if (child.matches("okchars"))
	  {
	    _okChars = child.getAttrStr("val");
	  }
	else if (child.matches("badchars"))
	  {
	    _badChars = child.getAttrStr("val");
	  }
	else if (child.matches("crypted"))
	  {
	    _crypted = true;
	  }
	else if (child.matches("md5crypted"))
	  {
	    _md5crypted = true;
	  }
	else if (child.matches("plaintext"))
	  {
	    _plaintext = true;
	  }
	else
	  {
	    return Ganymede.createErrorDialog("xml",
					      "Unrecognized password typedef entity: " + child +
					      "\nIn field def:\n" + root.getTreeString());
	  }
      }

    // now do all the setting

    retVal = setMinLength(_minlength);

    if (retVal != null && !retVal.didSucceed())
      {
	return Ganymede.createErrorDialog("xml",
					  "fielddef could not set min length: " + _minlength + "\n" +
					  root.getTreeString() + "\n" +
					  retVal.getDialogText());
      }

    retVal = setMaxLength(_maxlength);

    if (retVal != null && !retVal.didSucceed())
      {
	return Ganymede.createErrorDialog("xml",
					  "fielddef could not set max length: " + _maxlength + "\n" +
					  root.getTreeString() + "\n" +
					  retVal.getDialogText());
      }

    retVal = setOKChars(_okChars);
    
    if (retVal != null && !retVal.didSucceed())
      {
	return Ganymede.createErrorDialog("xml",
					  "fielddef could not set ok chars: " + _okChars + "\n" +
					  root.getTreeString() + "\n" +
					  retVal.getDialogText());
      }
    
    retVal = setBadChars(_badChars);
    
    if (retVal != null && !retVal.didSucceed())
      {
	return Ganymede.createErrorDialog("xml",
					  "fielddef could not set bad chars: " + _badChars + "\n" +
					  root.getTreeString() + "\n" +
					  retVal.getDialogText());
      }

    retVal = setCrypted(_crypted);

    if (retVal != null && !retVal.didSucceed())
      {
	return Ganymede.createErrorDialog("xml",
					  "fielddef could not set crypted flag: " + _crypted + "\n" +
					  root.getTreeString() + "\n" +
					  retVal.getDialogText());
      }

    retVal = setMD5Crypted(_md5crypted);

    if (retVal != null && !retVal.didSucceed())
      {
	return Ganymede.createErrorDialog("xml",
					  "fielddef could not set md5 crypted flag: " + _md5crypted + "\n" +
					  root.getTreeString() + "\n" +
					  retVal.getDialogText());
      }

    retVal = setPlainText(_plaintext);

    if (retVal != null && !retVal.didSucceed())
      {
	return Ganymede.createErrorDialog("xml",
					  "fielddef could not set plaintext flag: " + _plaintext + "\n" +
					  root.getTreeString() + "\n" +
					  retVal.getDialogText());
      }

    return null;
  }

  /**
   * <p>This method takes care of doing everything required to
   * take an XMLItem tree &lt;fielddef type="ip"&gt; and
   * update this field's schema information to match.</p>
   *
   * @return A failure ReturnVal if the schema for this field
   * could not be set to match.
   */

  private ReturnVal doIPXML(XMLItem root)
  {
    boolean _vect = false;
    short _maxSize = java.lang.Short.MAX_VALUE;
    String _namespace = null;
    ReturnVal retVal;

    /* -- */

    if (!root.getAttrStr("type").equals("ip"))
      {
	throw new IllegalArgumentException("bad XMLItem tree");
      }

    retVal = setType(FieldType.IP);

    if (retVal != null && !retVal.didSucceed())
      {
	return retVal;
      }

    // the <typedef type="ip"> node can have children
    // of its own

    XMLItem typeChildren[] = root.getChildren();
		
    for (int j = 0; j < typeChildren.length; j++)
      {
	XMLItem child = typeChildren[j];

	if (child.matches("vector"))
	  {
	    _vect = true;

	    Integer vectSize = child.getAttrInt("maxSize");

	    if (vectSize != null)
	      {
		_maxSize = vectSize.shortValue();
	      }
	  }
	else if (child.matches("namespace"))
	  {
	    _namespace = child.getAttrStr("val");
	  }
	else
	  {
	    return Ganymede.createErrorDialog("xml",
					      "Unrecognized ip typedef entity: " + child +
					      "\nIn field def:\n" + root.getTreeString());
	  }
      }

    // now do all the setting

    retVal = setArray(_vect);

    if (retVal != null && !retVal.didSucceed())
      {
	return Ganymede.createErrorDialog("xml",
					  "fielddef could not set vector bit to " + _vect + ": \n" +
					  root.getTreeString() + "\n" +
					  retVal.getDialogText());
      }
		
    if (_vect)
      {
	retVal = setMaxArraySize(_maxSize);
	
	if (retVal != null && !retVal.didSucceed())
	  {
	    return Ganymede.createErrorDialog("xml",
					      "fielddef could not set vector maximum size: " + _maxSize + "\n" +
					      root.getTreeString() + "\n" +
					      retVal.getDialogText());
	  }
      }

    retVal = setNameSpace(_namespace);

    if (retVal != null && !retVal.didSucceed())
      {
	return Ganymede.createErrorDialog("xml",
					  "fielddef could not set namespace: " + _namespace + "\n" +
					  root.getTreeString() + "\n" +
					  retVal.getDialogText());
      }

    return null;
  }

  /**
   * <p>This method takes care of doing everything required to
   * take an XMLItem tree &lt;fielddef type="numeric"&gt; and
   * update this field's schema information to match.</p>
   *
   * @return A failure ReturnVal if the schema for this field
   * could not be set to match.
   */

  private ReturnVal doNumericXML(XMLItem root)
  {
    String _namespace = null;
    ReturnVal retVal;

    /* -- */

    if (!root.getAttrStr("type").equals("ip"))
      {
	throw new IllegalArgumentException("bad XMLItem tree");
      }

    retVal = setType(FieldType.NUMERIC);

    if (retVal != null && !retVal.didSucceed())
      {
	return retVal;
      }

    // the <typedef type="numeric"> node can have children
    // of its own

    XMLItem typeChildren[] = root.getChildren();
		
    for (int j = 0; j < typeChildren.length; j++)
      {
	XMLItem child = typeChildren[j];

	if (child.matches("namespace"))
	  {
	    _namespace = child.getAttrStr("val");
	  }
	else
	  {
	    return Ganymede.createErrorDialog("xml",
					      "Unrecognized numeric typedef entity: " + child +
					      "\nIn field def:\n" + root.getTreeString());
	  }
      }

    // now do all the setting

    retVal = setNameSpace(_namespace);

    if (retVal != null && !retVal.didSucceed())
      {
	return Ganymede.createErrorDialog("xml",
					  "fielddef could not set namespace: " + _namespace + "\n" +
					  root.getTreeString() + "\n" +
					  retVal.getDialogText());
      }

    return null;
  }

  /**
   * <p>This method takes care of doing everything required to
   * take an XMLItem tree &lt;fielddef type="invid"&gt; and
   * update this field's schema information to match.</p>
   *
   * @return A failure ReturnVal if the schema for this field
   * could not be set to match.
   */

  private ReturnVal doInvidXML(XMLItem root, boolean doLinkResolve)
  {
    boolean _vect = false;
    short _maxSize = java.lang.Short.MAX_VALUE;
    boolean _embedded = false;
    String _targetobjectStr = null;
    Integer _targetobject = null;
    String _targetfieldStr = null;
    Integer _targetfield = null;
    ReturnVal retVal;

    /* -- */

    if (!root.getAttrStr("type").equals("invid"))
      {
	throw new IllegalArgumentException("bad XMLItem tree");
      }

    retVal = setType(FieldType.INVID);

    if (retVal != null && !retVal.didSucceed())
      {
	return retVal;
      }

    // the <typedef type="ip"> node can have children
    // of its own

    XMLItem typeChildren[] = root.getChildren();
		
    for (int j = 0; j < typeChildren.length; j++)
      {
	XMLItem child = typeChildren[j];

	if (child.matches("vector"))
	  {
	    _vect = true;

	    Integer vectSize = child.getAttrInt("maxSize");

	    if (vectSize != null)
	      {
		_maxSize = vectSize.shortValue();
	      }
	  }
	else if (child.matches("targetobject"))
	  {
	    _targetobjectStr = child.getAttrStr("name");
	    _targetobject = child.getAttrInt("id");
	    
	    if (_targetobjectStr == null && _targetobject == null)
	      {
		return Ganymede.createErrorDialog("xml",
						  "targetobject item does not specify name or id: " + child + "\n" +
						  root.getTreeString() + "\n" +
						  retVal.getDialogText());
	      }
	  }
	else if (child.matches("targetfield"))
	  {
	    _targetfieldStr = child.getAttrStr("name");
	    _targetfield = child.getAttrInt("id");

	    if (_targetfieldStr == null && _targetfield == null)
	      {
		return Ganymede.createErrorDialog("xml",
						  "targetfield item does not specify name or id: " + child + "\n" +
						  root.getTreeString() + "\n" +
						  retVal.getDialogText());
	      }
	  }
	else if (child.matches("embedded"))
	  {
	    _embedded = true;
	  }
	else
	  {
	    return Ganymede.createErrorDialog("xml",
					      "Unrecognized invid typedef entity: " + child +
					      "\nIn field def:\n" + root.getTreeString());
	  }
      }

    // now do all the setting

    retVal = setArray(_vect);

    if (retVal != null && !retVal.didSucceed())
      {
	return Ganymede.createErrorDialog("xml",
					  "fielddef could not set vector bit to " + _vect + ": \n" +
					  root.getTreeString() + "\n" +
					  retVal.getDialogText());
      }
		
    if (_vect)
      {
	retVal = setMaxArraySize(_maxSize);
	
	if (retVal != null && !retVal.didSucceed())
	  {
	    return Ganymede.createErrorDialog("xml",
					      "fielddef could not set vector maximum size: " + _maxSize + "\n" +
					      root.getTreeString() + "\n" +
					      retVal.getDialogText());
	  }
      }

    if (doLinkResolve)
      {
	// first we try to set the target object type, if any

	if (_targetobjectStr != null)
	  {
	    retVal = setTargetBase(_targetobjectStr);

	    if (retVal != null && !retVal.didSucceed())
	      {
		return Ganymede.createErrorDialog("xml",
						  "fielddef could not set invid target base: " + _targetobjectStr + "\n" +
						  root.getTreeString() + "\n" +
						  retVal.getDialogText());
	      }
	  }
	else if (_targetobject != null)
	  {
	    retVal = setTargetBase(_targetobject.shortValue());

	    if (retVal != null && !retVal.didSucceed())
	      {
		return Ganymede.createErrorDialog("xml",
						  "fielddef could not set invid target base: " + _targetobject + "\n" +
						  root.getTreeString() + "\n" +
						  retVal.getDialogText());
	      }
	  }
	else // both null
	  {
	    retVal = setTargetBase(null);

	    if (retVal != null && !retVal.didSucceed())
	      {
		return Ganymede.createErrorDialog("xml",
						  "fielddef could not clear invid target base: \n" +
						  root.getTreeString() + "\n" +
						  retVal.getDialogText());
	      }
	  }

	// then we try to set the target field, if any

	if (_targetfieldStr != null)
	  {
	    retVal = setTargetBase(_targetfieldStr);

	    if (retVal != null && !retVal.didSucceed())
	      {
		return Ganymede.createErrorDialog("xml",
						  "fielddef could not set invid target field: " + _targetfieldStr + "\n" +
						  root.getTreeString() + "\n" +
						  retVal.getDialogText());
	      }
	  }
	else if (_targetfield != null)
	  {
	    retVal = setTargetField(_targetfield.shortValue());

	    if (retVal != null && !retVal.didSucceed())
	      {
		return Ganymede.createErrorDialog("xml",
						  "fielddef could not set invid target field: " + _targetfield + "\n" +
						  root.getTreeString() + "\n" +
						  retVal.getDialogText());
	      }
	  }
	else // both null
	  {
	    retVal = setTargetField(null);

	    if (retVal != null && !retVal.didSucceed())
	      {
		return Ganymede.createErrorDialog("xml",
						  "fielddef could not clear invid target field: \n" +
						  root.getTreeString() + "\n" +
						  retVal.getDialogText());
	      }
	  }
      }

    retVal = setEditInPlace(_embedded);

    if (retVal != null && !retVal.didSucceed())
      {
	return Ganymede.createErrorDialog("xml",
					  "fielddef could not set embedded status: \n" +
					  root.getTreeString() + "\n" +
					  retVal.getDialogText());
      }

    return null;
  }

  // ----------------------------------------------------------------------

  /**
   * <p>This method returns true if this field definition can be edited
   * in the schema editor.</p>
   *
   * <p>This method will return false for the built-in universal field
   * types that the server is dependent on for its own
   * functioning, because those really have no reason to be edited at
   * all.. even the field names needn't be edited by anyone, since
   * they aren't shown in the client.</p>
   *
   * @see arlut.csd.ganymede.BaseField 
   */

  public boolean isEditable()
  {
    return getID() > SchemaConstants.FinalSystemField;
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
    if (!base.store.loading && editor == null)
      {
	throw new IllegalArgumentException("not in an schema editing context");
      }

    // if we aren't loading, don't allow messing with the global fields

    if (editor != null && !isEditable())
      {
	return Ganymede.createErrorDialog("Error",
					  "Can't change the name of a system field.");
      }
    
    if (name == null || name.equals(""))
      {
	return Ganymede.createErrorDialog("error",
					  "can't have a null or empty name");
      }

    // make sure we strip any chars that would cause this object name
    // to not be a valid XML entity name character.  We make an
    // exception for spaces, which we will replace with underscores as
    // an XML char.

    name = StringUtils.strip(name,
			     "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 .-").trim();
    
    // no change, no problem

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

    /* -- */

    if (!base.store.loading && editor == null)
      {
	throw new IllegalArgumentException("not in an schema editing context");
      }

    // if we're not loading, don't allow global fields to be messed with

    if (editor != null && !isEditable())
      {
	return Ganymede.createErrorDialog("Error",
					  "Can't change the class name of a system field.");
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
    if (!base.store.loading && editor == null)
      {
	throw new IllegalArgumentException("not editing");
      }

    // if we're not loading, don't allow global fields to be messed with

    if (editor != null && !isEditable())
      {
	return Ganymede.createErrorDialog("Error",
					  "Can't edit system field.");
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
    if (!base.store.loading && editor == null)
      {
	throw new IllegalArgumentException("not editing");
      }

    if (type < FIRSTFIELD || type > LASTFIELD)
      {
	throw new IllegalArgumentException("type argument out of range");
      }

    // if no change, no problem.

    if (type == field_type)
      {
	return null;
      }

    // don't allow global fields to be messed with

    if (editor != null && !isEditable())
      {
	return Ganymede.createErrorDialog("Error",
					  "Can't edit system field.");
      }

    // now we need to delineate those fields whose types must not be
    // changed due to server requirements, even if we don't have an
    // instance of the field in current use

    // note that isEditable() checks for one of the universal fields..
    // isSystemField() checks for non-universal fields in any of the
    // system mandatory objects that we want to protect

    // note that we don't just rule out all type setting on all fields
    // in these object types, as it is permissible to add fields to
    // the mandatory types

    if (isSystemField())
      {
	return Ganymede.createErrorDialog("Error",
					  "Can't change the type of a system field.");
      }

    if (isInUse())
      {
	return Ganymede.createErrorDialog("Error",
					  "Can't change the type of a field which is in use in the database.");
      }

    if (isInvid())
      {
	// need to check to make sure no other invid field definitions are
	// pointing to this field somehow, else changing type might break
	// that other field definition
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

    // no change, no problem

    if (b == array)
      {
	return null;
      }

    if (editor != null && !isEditable())
      {
	return Ganymede.createErrorDialog("Error",
					  "Can't edit system field.");
      }

    // array-ness is way too critical to be edited, even in mildly variable system
    // fields like username in the user object

    if (isSystemField())
      {
	return Ganymede.createErrorDialog("Error",
					  "can't change the vector status of a system field.");
      }

    if (isInUse())
      {
	return Ganymede.createErrorDialog("Error",
					  "can't change the vector status of a field in use.");
      }

    if (b && !(isString() || isInvid() || isIP()))
      {
	return Ganymede.createErrorDialog("Error",
					  "can't set this field type to vector");
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
   * <p>This method is used to set this field's id in the containing
   * DBObjectBase.  This method will return a failure if an id is
   * selected which is already in use in another field in this object
   * definition.</p> 
   */

  public ReturnVal setID(short id)
  {
    if (!base.store.loading && editor == null)
      {
	throw new IllegalArgumentException("not in an schema editing context");
      }

    if (id < 0)
      {
	return Ganymede.createErrorDialog("error",
					  "field id number " + id + " out of range.");
      }

    // no change, no problem

    if (id == field_code)
      {
	return null;
      }

    if (base.getField(id) != null)
      {
	return Ganymede.createErrorDialog("error",
					  "field id number " + id + " is already in use.");
      }

    if (field_code >= 0)
      {
	return Ganymede.createErrorDialog("error",
					  "can't change an established field id number");
      }

    field_code = id;

    return null;
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

    // no change, no problem

    if (limit == this.limit)
      {
	return null;
      }

    if (editor != null && !isEditable())
      {
	return Ganymede.createErrorDialog("Error",
					  "Can't edit system field.");
      }

    // array sizes need not be screwed with in the system fields

    if (isSystemField())
      {
	return Ganymede.createErrorDialog("Error",
					  "can't change the vector limits of a system field.");
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

    if (editor != null && !isEditable())
      {
	return Ganymede.createErrorDialog("Error",
					  "Can't edit system field.");
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

    if (editor != null && !isEditable())
      {
	return Ganymede.createErrorDialog("Error",
					  "Can't edit system field.");
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

    if (editor != null && !isEditable())
      {
	return Ganymede.createErrorDialog("Error",
					  "Can't edit system field.");
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

    // no change, no problem

    if (val == minLength)
      {
	return null;
      }

    if (editor != null && !isEditable())
      {
	return Ganymede.createErrorDialog("Error",
					  "Can't edit system field.");
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

    // no change, no problem

    if (val == maxLength)
      {
	return null;
      }

    if (editor != null && !isEditable())
      {
	return Ganymede.createErrorDialog("Error",
					  "Can't edit system field.");
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

    if (editor != null && !isEditable())
      {
	return Ganymede.createErrorDialog("Error",
					  "Can't edit system field.");
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

    if (editor != null && !isEditable())
      {
	return Ganymede.createErrorDialog("Error",
					  "Can't edit system field.");
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

    if (editor != null && !isEditable())
      {
	return Ganymede.createErrorDialog("Error",
					  "Can't edit system field.");
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

    if (editor != null && !isEditable())
      {
	return Ganymede.createErrorDialog("Error",
					  "Can't edit system field.");
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

    // if we are not loading, don't allow a built-in universal field
    // to be messed with

    if (editor != null && !isEditable())
      {
	return Ganymede.createErrorDialog("Error",
					  "Can't edit system field.");
      }

    if (!isString() && !isNumeric() && !isIP())
      {
	throw new IllegalArgumentException("this field type does not accept a namespace constraint");
      }

    // no change, no problem

    if ((nameSpaceId == null || nameSpaceId.equals("")) && namespace == null)
      {
	return null;
      }

    if (namespace != null && nameSpaceId != null && !nameSpaceId.equals(""))
      {
	DBNameSpace matchingSpace = base.store.getNameSpace(nameSpaceId);

	if (matchingSpace == namespace)
	  {
	    return null;
	  }
      }

    // see about doing the setting

    if (nameSpaceId == null || nameSpaceId.equals(""))
      {
	// wouldn't it be nice if java had decent support for declared data structures?

	if (base.getTypeID() == SchemaConstants.UserBase &&
	    getID() == SchemaConstants.UserUserName)
	  {
	    return Ganymede.createErrorDialog("Error",
					      "Proper functioning of the Ganymede server " +
					      "depends on user names being unique.");
	  }

	if (base.getTypeID() == SchemaConstants.PersonaBase &&
	    getID() == SchemaConstants.PersonaLabelField)
	  {
	    return Ganymede.createErrorDialog("Error",
					      "Proper functioning of the Ganymede server " +
					      "depends on persona labels being unique.");
	  }

	if (base.getTypeID() == SchemaConstants.OwnerBase &&
	    getID() == SchemaConstants.OwnerNameField)
	  {
	    return Ganymede.createErrorDialog("Error",
					      "Proper functioning of the Ganymede server " +
					      "depends on owner group labels being unique.");
	  }

	if (base.getTypeID() == SchemaConstants.EventBase &&
	    getID() == SchemaConstants.EventToken)
	  {
	    return Ganymede.createErrorDialog("Error",
					      "Proper functioning of the Ganymede server " +
					      "depends on event tokens being unique.");
	  }

	if (base.getTypeID() == SchemaConstants.RoleBase &&
	    getID() == SchemaConstants.RoleName)
	  {
	    return Ganymede.createErrorDialog("Error",
					      "Proper functioning of the Ganymede server " +
					      "depends on Role names being unique.");
	  }

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

    // no change, no harm

    if (b == editInPlace)
      {
	return null;
      }
    
    if (isSystemField())
      {
	return Ganymede.createErrorDialog("Error",
					  "Can't change the type of a system field.");
      }
    
    if (isInUse())
      {
	return Ganymede.createErrorDialog("Error",
					  "Can't change the editInPlace status type of an " +
					  "invid field which is in use in the database.");
      }
    
    editInPlace = b;

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

    // no change, no harm

    if (val == allowedTarget)
      {
	return null;
      }

    if (isSystemField())
      {
	return Ganymede.createErrorDialog("Error",
					  "Can't change the type of a system field.");
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
	if (allowedTarget == -1)
	  {
	    return null;		// no change, no harm
	  }

	if (isSystemField())
	  {
	    return Ganymede.createErrorDialog("Error",
					      "Can't change the type of a system field.");
	  }

	allowedTarget = -1;

	if (isInUse())
	  {
	    return warning2;
	  }
	else
	  {
	    return null;
	  }
      }

    b = editor.getBase(baseName);

    try
      {
	if (b != null)
	  {
	    if (b.getTypeID() == allowedTarget)
	      {
		return null;	// no change, no harm
	      }

	    if (isSystemField())
	      {
		return Ganymede.createErrorDialog("Error",
						  "Can't change the type of a system field.");
	      }

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

    if (val == targetField)
      {
	return null;		// no change, no harm
      }

    if (isSystemField())
      {
	return Ganymede.createErrorDialog("Error",
					  "Can't change the type of a system field.");
      }

    if (val < 0)
      {
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

    if (allowedTarget == -1)
      {
	return Ganymede.createErrorDialog("schema edit error",
					  "Can't set target field on non-symmetric invid field " + 
					  this.toString() + " to " + val);
      }

    try
      {
	b = editor.getBase(allowedTarget);

	// we're looking up the object that we have pre-selected.. we
	// should always set a target object before trying to set a
	// field

	if (b == null)
	  {
	    return Ganymede.createErrorDialog("schema edit error",
					      "Can't find container base in order to set target field for " + 
					      this.toString() + " to " + val);
	  }
	
	bF = b.getField(val);

	if (bF == null)
	  {
	    return Ganymede.createErrorDialog("schema edit error",
					      "Can't find numbered target field to set invid field " + 
					      this.toString() + " to point to field #" + val);
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

    if (fieldName == null || fieldName.equals(""))
      {
	if (targetField == -1)
	  {
	    return null;		// no change, no harm
	  }

	if (isSystemField())
	  {
	    return Ganymede.createErrorDialog("Error",
					      "Can't change the type of a system field.");
	  }

	targetField = -1;

	if (isInUse())
	  {
	    return warning2;
	  }
	else
	  {
	    return null;
	  }
      }

    // look for fieldName in the base currently specified in
    // allowedTarget

    if (allowedTarget == -1 && fieldName != null && !fieldName.equals(""))
      {
	return Ganymede.createErrorDialog("schema edit error",
					  "Can't set target field on non-symmetric invid field " + 
					  this.toString() + " to " + fieldName);
      }

    b = editor.getBase(allowedTarget);

    try
      {
	if (b == null)
	  {
	    return Ganymede.createErrorDialog("schema edit error",
					      "Can't find container base in order to set target field for " + 
					      this.toString() + " to " + fieldName);
	  }
	
	bF = b.getField(fieldName);

	if (bF == null)
	  {
	    return Ganymede.createErrorDialog("schema edit error",
					      "Can't find naned target field to set invid field " + 
					      this.toString() + " to point to field " + fieldName);
	  }

	if (bF.getID() == targetField)
	  {
	    return null;	// no change, no harm, no warning needed
	  }

	// remember, system fields are initialized outside of the
	// context of the loading system, there should never be a
	// reason to call setTargetField() on a aystem field

	if (isSystemField())
	  {
	    return Ganymede.createErrorDialog("Error",
					      "Can't change the type of a system field.");
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
    if (!base.store.loading && editor == null)
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
    if (!base.store.loading && editor == null)
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
    if (!base.store.loading && editor == null)
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
   * <p>This method checks to see if this DBObjectBaseField corresponds to
   * a system field
   */

  private boolean isSystemField()
  {
    if (getID() <= SchemaConstants.FinalSystemField)
      {
	return true;
      }

    // wouldn't it be nice if java had decent support for declared data structures?

    // basically we want to identify all fields in the built-in types
    // that we have to be especially paranoid about.

    switch (base.getTypeID())
      {
      case SchemaConstants.OwnerBase:

	switch (getID())
	  {
	  case SchemaConstants.OwnerNameField:
	  case SchemaConstants.OwnerMembersField:
	  case SchemaConstants.OwnerObjectsOwned:
	  case SchemaConstants.OwnerCcAdmins:
	  case SchemaConstants.OwnerExternalMail:
	    return true;
	  }

	break;

      case SchemaConstants.PersonaBase:

	switch (getID())
	  {
	  case SchemaConstants.PersonaNameField:
	  case SchemaConstants.PersonaPasswordField:
	  case SchemaConstants.PersonaGroupsField:
	  case SchemaConstants.PersonaAssocUser:
	  case SchemaConstants.PersonaPrivs:
	  case SchemaConstants.PersonaAdminConsole:
	  case SchemaConstants.PersonaAdminPower:
	  case SchemaConstants.PersonaMailAddr:
	  case SchemaConstants.PersonaLabelField:
	    return true;
	  }

	break;

      case SchemaConstants.RoleBase:

	switch (getID())
	  {
	  case SchemaConstants.RoleName:
	  case SchemaConstants.RoleMatrix:
	  case SchemaConstants.RolePersonae:
	  case SchemaConstants.RoleDefaultMatrix:
	  case SchemaConstants.RoleDelegatable:
	    return true;
	  }

	break;

      case SchemaConstants.UserBase:

	switch (getID())
	  {
	  case SchemaConstants.UserUserName:
	  case SchemaConstants.UserPassword:
	  case SchemaConstants.UserAdminPersonae:
	    return true;
	  }

	break;

      case SchemaConstants.EventBase:

	switch (getID())
	  {
	  case SchemaConstants.EventToken:
	  case SchemaConstants.EventName:
	  case SchemaConstants.EventDescription:
	  case SchemaConstants.EventMailBoolean:
	  case SchemaConstants.EventMailToSelf:
	  case SchemaConstants.EventMailOwners:
	  case SchemaConstants.EventExternalMail:
	    return true;
	  }

	break;

      case SchemaConstants.ObjectEventBase:

	switch (getID())
	  {
	  case SchemaConstants.ObjectEventToken:
	  case SchemaConstants.ObjectEventName:
	  case SchemaConstants.ObjectEventDescription:
	  case SchemaConstants.ObjectEventMailToSelf:
	  case SchemaConstants.ObjectEventObjectName:
	  case SchemaConstants.ObjectEventMailOwners:
	  case SchemaConstants.ObjectEventObjectType:
	  case SchemaConstants.ObjectEventExternalMail:
	    return true;
	  }

	break;

      case SchemaConstants.TaskBase:

	switch (getID())
	  {
	  case SchemaConstants.TaskName:
	  case SchemaConstants.TaskClass:
	  case SchemaConstants.TaskRunOnCommit:
	  case SchemaConstants.TaskRunPeriodically:
	  case SchemaConstants.TaskPeriodUnit:
	  case SchemaConstants.TaskPeriodCount:
	  case SchemaConstants.TaskPeriodAnchor:
	    return true;
	  }

	break;
      }

    return false;
  }

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
		    catch (NullPointerException ex)
		      {
			System.err.println("Error, " + this.toString() +
					   " couldn't lookup targetField (" + 
					   targetField + ")");
			System.err.println("In base " + base.toString());
			System.err.println(ex.getMessage());
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
