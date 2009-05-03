/*
   GASH 2

   DBObjectBaseField.java

   The GANYMEDE object storage system.

   Created: 27 August 1996
   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996 - 2009
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
import java.io.PrintWriter;
import java.rmi.RemoteException;
import java.util.Date;
import java.util.Enumeration;

import arlut.csd.JDialog.JDialogBuff;
import arlut.csd.Util.StringUtils;
import arlut.csd.Util.TranslationService;
import arlut.csd.Util.XMLItem;
import arlut.csd.Util.XMLUtils;
import arlut.csd.ganymede.common.FieldTemplate;
import arlut.csd.ganymede.common.FieldType;
import arlut.csd.ganymede.common.ReturnVal;
import arlut.csd.ganymede.common.SchemaConstants;
import arlut.csd.ganymede.rmi.Base;
import arlut.csd.ganymede.rmi.BaseField;

/*------------------------------------------------------------------------------
                                                                           class
                                                               DBObjectBaseField

------------------------------------------------------------------------------*/

/**
 * <p>An entry in the Ganymede server's {@link arlut.csd.ganymede.server.DBStore DBStore}
 * schema dictionary.  DBStore contains a collection of 
 * {@link arlut.csd.ganymede.server.DBObjectBase DBObjectBase} objects, which define
 * the schema information for a particular type of object held in the Ganymede
 * database.  A DBObjectBaseField is contained within a DBObjectBase, and defines
 * the name, id, type, and constraints of a particular field that can be held
 * in {@link arlut.csd.ganymede.server.DBObject DBObjects} of that type, including
 * a controlling {@link arlut.csd.ganymede.server.DBNameSpace DBNameSpace}, if
 * appropriate.</P>
 *
 * <P>Each {@link arlut.csd.ganymede.server.DBField DBField} held in the server's
 * database holds a reference to a DBObjectBaseField, and the DBField's methods
 * will consult the DBObjectBaseField during run-time to make decisions based
 * on specified constraints defined in the DBObjectBaseField.</P>
 *
 * <P>The Ganymede schema editor uses the {@link arlut.csd.ganymede.rmi.BaseField BaseField}
 * remote interface to make changes to a DBObjectBaseField's constraint information
 * during schema editing. The Ganymede client may also use the BaseField interface
 * to learn about the field's type information, but it may also download a
 * {@link arlut.csd.ganymede.common.FieldTemplate FieldTemplate} that carries a
 * DBObjectBaseField's type information in an efficiently retrieved summary.</P>
 */

public final class DBObjectBaseField implements BaseField, FieldType, Comparable {

  static final boolean debug = false;

  /**
   * <p>TranslationService object for handling string localization in
   * the Ganymede server.</p>
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.server.DBObjectBaseField");

  static final ReturnVal warning1 = genWarning1();
  static final ReturnVal warning2 = genWarning2();

  /**
   * Object type definition for the database object class we are member of
   */

  private DBObjectBase base;

  /**
   * name of this field
   */

  private String field_name = null;

  /**
   * id of this field in the current object type
   */

  private short field_code = -1;

  /**
   * {@link arlut.csd.ganymede.common.FieldType Field Type} for this field
   */

  private short field_type = -1;

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

  private boolean visibility = true;

  /**
   * string to be displayed in the client as a tooltip explaining this field
   */

  private String comment = null;

  /**
   * name of the tab this field should be placed in on the client
   */

  private String tabName = null;

  /**
   * true if this field is an array type
   */

  private boolean array = false;

  // array attributes

  /**
   * max length of array
   */

  private short limit = Short.MAX_VALUE;

  // boolean attributes

  private boolean labeled = false;
  private String trueLabel = null;
  private String falseLabel = null;

  // string attributes

  private short minLength = 0;
  private short maxLength = Short.MAX_VALUE;
  private String okChars = null;
  private String badChars = null;
  private DBNameSpace namespace = null;
  private boolean multiLine = false;

  /**
   * Regular Expression string for input-filtering
   * in {@link arlut.csd.ganymede.server.StringDBField}s.
   */

  private String regexpPat = null;	// introduced in ganymede.db version 1.14

  /**
   * Text description of the meaning of the regexpPat,
   * if defined
   */

  private String regexpDesc = null;

  /**
   * Compiled regular expression for input-filtering
   * in {@link arlut.csd.ganymede.server.StringDBField}s.
   */

  private java.util.regex.Pattern regexp = null;

  // invid attributes

  private boolean editInPlace = false;
  private short allowedTarget = -1;	// no target restrictions
  private short targetField = -1;	// no field symmetry.. we use the DBStore backPointers structure by default

  /**
   * If this is not null, then we have gotten information on this
   * Invid DBObjectBaseField pointing to a type of object from an XML
   * file, and we'll need to do type resolution once the schema is
   * completely loaded from an XML stream.  Once this happens,
   * allowedTarget will be set properly, and allowedTargetStr will be
   * set to null.
   */

  private String allowedTargetStr = null;

  /**
   * If this is not null, then we have gotten information on this
   * Invid DBObjectBaseField linked to a field from an XML file, and
   * we'll need to do type resolution once the schema is completely
   * loaded from an XML stream.  Once this happens, targetField will
   * be set properly, and targetFieldStr will be set to null.  
   */

  private String targetFieldStr = null;

  // password attributes

  private boolean crypted = true;	// UNIX encryption is the default.
  private boolean md5crypted = false;	// OpenBSD style md5crypt() is not
  private boolean apachemd5crypted = false;	// Apache style md5crypt() is not
  private boolean winHashed = false;	// Windows NT/Samba hashes are not
  private boolean sshaHashed = false;	// SSHA hash is not either
  private boolean shaUnixCrypted = false;	// SHA Unix Crypt is not either
  private boolean storePlaintext = false; // nor is plaintext

  /**
   * If the password field is to use the shaUnixCrypt algorithm, which
   * variant shall we use?  If useShaUnixCrypted512 is true, we'll use
   * the SHA512 version, if it is false, we'll use the SHA256 version.
   */

  private boolean useShaUnixCrypted512 = false;

  /**
   * If the password field is to use the shaUnixCrypt algorithm, how
   * many rounds shall we specify?
   */

  private int shaUnixCryptRounds = 5000;

  // schema editing

  /**
   * If we are being edited, this will point to an instance
   * of a server-side schema editing class.  */

  private DBSchemaEdit editor;

  /**
   * Downloadable FieldTemplate representing the constant field type
   * attributes represented by this DBObjectBaseField.  This template
   * is regenerated whenever clearEditor() is called, upon schema
   * editing completion.
   */

  private FieldTemplate template;

  /**
   * <P>A three state flag used by isInUse() to report whether or
   * not a particular field is in use in the loaded database.</P>
   */

  private Boolean inUseCache = null;

  /**
   * <p>Timestamp for the last time a field of this type was changed
   * in a transaction, across all {@link
   * arlut.csd.ganymede.server.DBObject DBObjects} in the {@link
   * arlut.csd.ganymede.server.DBObjectBase DBObjectBase} that contains this
   * field definition.</p>
   *
   * <p>Used to allow subclasses of {@link
   * arlut.csd.ganymede.server.GanymedeBuilderTask
   * GanymedeBuilderTask} to decide whether they want to trigger a
   * particular build or sub-build.</p>
   */

  private Date lastChange;

  /**
   * This field is used to handle field order sorting when
   * we read an old (pre-2.0) ganymede.db file.
   */

  int tmp_displayOrder = -1;

  /* -- */

  /**
   * Generic field constructor.
   */

  DBObjectBaseField(DBObjectBase base) throws RemoteException
  {
    this.base = base;
    this.editor = base.getEditor();
    
    field_name = "";
    comment = "";

    tabName = ts.l("receive.default_tab_name");	 // "General"
    
    field_code = -1;
    field_type = -1;
    lastChange = new Date();

    Ganymede.rmi.publishObject(this);
  }

  /**
   * Receive constructor, for binary loading from ganymede.db.
   */

  DBObjectBaseField(DataInput in, DBObjectBase base) throws IOException, RemoteException
  {
    this(base);

    receive(in);
    template = new FieldTemplate(this);
  }

  /**
   * Copy constructor, used during schema editing.
   *
   * <b>IMPORTANT: BE SURE TO ALWAYS EDIT THIS METHOD IF YOU ADD ANY
   * FIELDS TO THIS CLASS!</b>
   */

  DBObjectBaseField(DBObjectBaseField original, DBObjectBase newBase) throws RemoteException
  {
    this(newBase);

    // Note that this method does direct property access for all
    // copying, rather than using our setters, thereby bypassing the
    // state checks for permissions, etc.

    field_name = original.field_name; // name of this field
    field_code = original.field_code; // id of this field in the current object
    field_type = original.field_type; // data type contained herein

    visibility = original.visibility;

    comment = original.comment;
    array = original.array;	// true if this field is an array type
    limit = original.limit;

    tabName = original.tabName;

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
    regexpDesc = original.regexpDesc;
    regexp = original.regexp;

    editInPlace = original.editInPlace;
    allowedTarget = original.allowedTarget;
    targetField = original.targetField;

    crypted = original.crypted;
    md5crypted = original.md5crypted;
    apachemd5crypted = original.apachemd5crypted;
    winHashed = original.winHashed;
    sshaHashed = original.sshaHashed;
    shaUnixCrypted = original.shaUnixCrypted;
    useShaUnixCrypted512 = original.useShaUnixCrypted512;
    shaUnixCryptRounds = original.shaUnixCryptRounds;
    storePlaintext = original.storePlaintext;

    inUseCache = null;

    // We'll just re-use the original's FieldTemplate for the time
    // being.. when the SchemaEditor is done, it will call
    // clearEditor() on our DBObjectBase, which will create a new
    // FieldTemplate for us.

    template = original.template;
  }

  /**
   * <P>This method is used to allow objects in this base to notify us
   * when instances of fields of this kind are changed.  It is called
   * from the {@link arlut.csd.ganymede.server.DBEditSet DBEditSet}
   * commit() method.</P>
   */

  void updateTimeStamp()
  {
    lastChange = new Date();
  }

  /**
   * <P>Returns a Date object containing the time that any changes were
   * committed to instances of fields specified by this DBObjectBaseField.</P> 
   */

  public Date getTimeStamp()
  {
    return lastChange;
  }

  /**
   * Clears the editor reference from this DBObjectBaseField when
   * schema editing is completed and updates the saved FieldTemplate.
   */

  public void clearEditor()
  {
    this.editor = null;

    this.template = new FieldTemplate(this);
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

    if (comment == null)
      {
	out.writeUTF("");
      }
    else
      {
	out.writeUTF(comment);
      }

    if (tabName == null)
      {
	out.writeUTF("");
      }
    else
      {
	out.writeUTF(tabName);	// added at file version 2.12
      }

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

	if (regexpDesc == null)
	  {
	    out.writeUTF("");	// added at file version 2.2
	  }
	else
	  {
	    out.writeUTF(regexpDesc); // added at file version 2.2
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
	out.writeBoolean(apachemd5crypted);
	out.writeBoolean(winHashed);
	out.writeBoolean(sshaHashed);

	// at 2.13 we introduce shaUnixCrypt support

	out.writeBoolean(shaUnixCrypted);
	out.writeBoolean(useShaUnixCrypted512);
	out.writeInt(shaUnixCryptRounds);

	out.writeBoolean(storePlaintext);
      }
  }

  /**
   * <p>This method is used when the database is being loaded, to read
   * in this field definition from disk.  It is mated with emit().</p>
   */

  synchronized void receive(DataInput in) throws IOException
  {
    // we use setName to filter out any fieldname chars that wouldn't
    // be acceptable as an XML entity name character.

    field_name = in.readUTF();
    field_code = in.readShort();
    field_type = in.readShort();

    if (base.getStore().isLessThan(2,16))
      {
	in.readUTF();		// vestigal classname
      }

    comment = in.readUTF();

    // we stopped keeping the editable and removable flags in the
    // ganymede.db file at 1.17

    if (base.getStore().isLessThan(2,0))
      {
	in.readBoolean();	// skip editable
	in.readBoolean();	// skip removable
      }

    // at file version 2.12, we introduce tab names per object base
    // field

    if (base.getStore().isAtLeast(2,12))
      {
	tabName = in.readUTF();

	if (tabName.equals(""))
	  {
	    tabName = ts.l("receive.default_tab_name");	// "General"
	  }
      }
    else
      {
	tabName = ts.l("receive.default_tab_name");	// "General"
      }

    // at file version 1.6, we introduced field visibility

    if (base.getStore().isAtLeast(1,6))
      {
	visibility = in.readBoolean();
      }
    else
      {
	visibility = true;
      }

    // at file version 1.7, we introduced an explicit built-in flag
    // we took it out at 2.0

    if (base.getStore().isBetweenRevs(1,7,2,0))
      {
	in.readBoolean();	// skip builtIn
      }

    // between file versions 1.1 and 1.17, we had a field_order
    // field

    if (base.getStore().isBetweenRevs(1,1,2,0))
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
	
	if (base.getStore().isAtLeast(1,9))
	  {
	    multiLine = in.readBoolean();
	  }
	else
	  {
	    multiLine = false;
	  }

	// at file version 1.14, we introduced regexps for string fields
	
	if (base.getStore().isAtLeast(1,14))
	  {
	    setRegexpPat(in.readUTF());
	  }
	else
	  {
	    setRegexpPat(null);
	  }

	// at file version 2.2, we introduced a description field for regexps

	if (base.getStore().isAtLeast(2,2))
	  {
	    setRegexpDesc(in.readUTF());
	  }
	else
	  {
	    setRegexpDesc(null);
	  }
      }
    else if (isNumeric())
      {
	String nameSpaceId;

	/* - */

	// at 1.8 we introduced namespaces for number fields

	if (base.getStore().isAtLeast(1,8))
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

	if (base.getStore().isAtLeast(1,8))
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

	if (!editInPlace)
	  {
	    targetField = in.readShort();
	  }
	else
	  {
	    // we no longer allow edit-in-place invid fields to target
	    // an explicit field in the embedded object.  the
	    // relationship with the container field in the embedded
	    // object is now always implicit.

	    in.readShort();
	  }

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

	if (base.getStore().isAtLeast(1,16))
	  {
	    md5crypted = in.readBoolean();
	  }
	else
	  {
	    md5crypted = false;
	  }

	// at 2.4 we introduce apachemd5crypted

	if (base.getStore().isAtLeast(2,4))
	  {
	    apachemd5crypted = in.readBoolean();
	  }
	else
	  {
	    apachemd5crypted = false;
	  }

	// at 2.1 we introduced winHashed

	if (base.getStore().isAtLeast(2,1))
	  {
	    winHashed = in.readBoolean();
	  }
	else
	  {
	    winHashed = false;
	  }

	// at 2.5 we introduced sshaHashed

	if (base.getStore().isAtLeast(2,5))
	  {
	    sshaHashed = in.readBoolean();
	  }
	else
	  {
	    sshaHashed = false;
	  }

	// at 2.13 we introduce shaUnixCrypted

	if (base.getStore().isAtLeast(2,13))
	  {
	    shaUnixCrypted = in.readBoolean();
	    useShaUnixCrypted512 = in.readBoolean();
	    shaUnixCryptRounds = in.readInt();
	  }
	else
	  {
	    shaUnixCrypted = false;
	    useShaUnixCrypted512 = false;
	    shaUnixCryptRounds = 5000;
	  }

	// at 1.10 we introduced storePlaintext

	if (base.getStore().isAtLeast(1,10))
	  {
	    storePlaintext = in.readBoolean();
	  }
	else
	  {
	    storePlaintext = false;
	  }
      }
  }

  /**
   * <p>This method is used when the database is being dumped, to write
   * out this field definition to disk.  It is mated with setXML().</p>
   */

  synchronized void emitXML(XMLDumpContext xmlOut) throws IOException
  {
    boolean nonEmpty = false;

    /* -- */

    xmlOut.startElementIndent("fielddef");
    xmlOut.attribute("name", XMLUtils.XMLEncode(field_name));
    xmlOut.attribute("id", java.lang.Short.toString(field_code));

    xmlOut.indentOut();

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
      case FieldType.FIELDOPTIONS:
	xmlOut.attribute("type", "options");
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

	    if (regexpDesc != null && !regexpDesc.equals(""))
	      {
		xmlOut.attribute("desc", regexpDesc);
	      }

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
		targetObjectBase = base.getStore().getObjectBase(allowedTarget);

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

	if (apachemd5crypted)
	  {
	    xmlOut.startElementIndent("apacheMd5crypted");
	    xmlOut.endElement("apacheMd5crypted");
	  }

	if (winHashed)
	  {
	    xmlOut.startElementIndent("winHashed");
	    xmlOut.endElement("winHashed");
	  }

	if (sshaHashed)
	  {
	    xmlOut.startElementIndent("sshaHashed");
	    xmlOut.endElement("sshaHashed");
	  }

	if (shaUnixCrypted)
	  {
	    xmlOut.startElementIndent("shaUnixCrypted");
	    xmlOut.attribute("type", useShaUnixCrypted512 ? "512" : "256");
	    xmlOut.attribute("rounds", java.lang.Integer.toString(shaUnixCryptRounds));
	    xmlOut.endElement("shaUnixCrypted");
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

  synchronized ReturnVal setXML(XMLItem root, boolean doLinkResolve, PrintWriter err)
  {
    XMLItem item;
    Integer field_codeInt;
    boolean typeRead = false;
    boolean _visibility = true;
    String _comment = null;
    ReturnVal retVal = null;

    /* -- */

    if (!isEditing())
      {
	// "Not in a schema editing context."
	throw new IllegalStateException(ts.l("global.not_editing_schema"));
      }

    if (root == null || !root.matches("fielddef"))
      {
	// "DBObjectBaseField.setXML(): next element != open fielddef: {0}"
	throw new IllegalArgumentException(ts.l("setXML.bad_nextitem", root));
      }

    field_codeInt = root.getAttrInt("id");

    if (field_codeInt == null)
      {
	// "XML"
	// "fielddef does not define id attr:\n{0}"
	return Ganymede.createErrorDialog(ts.l("global.xmlErrorTitle"),
					  ts.l("setXML.no_id", root.getTreeString()));
      }

    // extract the short

    short _fieldID = field_codeInt.shortValue();

    // we don't allow the xml file to specify global fields

    if (_fieldID < 100)
      {
	// "XML"
	// "fielddef defines an id attr out of range.. must be >= 100 for custom fields:\n{0}"
	return Ganymede.createErrorDialog(ts.l("global.xmlErrorTitle"),
					  ts.l("setXML.bad_id", root.getTreeString()));
      }

    // we have to set the id before we do anything else, since most
    // setters refuse to set if the field id isn't in a safe range

    retVal = setID(_fieldID);

    if (!ReturnVal.didSucceed(retVal))
      {
	// "XML"
	// "fielddef could not have its id set:\n{0}\n{1}"
	return Ganymede.createErrorDialog(ts.l("global.xmlErrorTitle"),
					  ts.l("setXML.id_failure", root.getTreeString(), retVal.getDialogText()));
      }

    // swap names if needed.. the DBObjectBase.setXML() will have checked for unique field
    // names before calling us

    retVal = setName(XMLUtils.XMLDecode(root.getAttrStr("name")), true);

    if (!ReturnVal.didSucceed(retVal))
      {
	// "XML"
	// "fielddef could not have its name set:\n{0}\n{1}"
	return Ganymede.createErrorDialog(ts.l("global.xmlErrorTitle"),
					  ts.l("setXML.name_failure", root.getTreeString(), retVal.getDialogText()));
      }

    // look at the nodes under root, set up this field def based on
    // them

    XMLItem children[] = root.getChildren();

    for (int i = 0; i < children.length; i++)
      {
	item = children[i];

	if (item.matches("comment"))
	  {
	    XMLItem commentChildren[] = item.getChildren();

	    if (commentChildren == null)
	      {
		_comment = null;
		continue;
	      }

	    if (commentChildren.length != 1)
	      {
		// "XML"
		// "unrecognized children in comment block:\n{0}"
		return Ganymede.createErrorDialog(ts.l("global.xmlErrorTitle"),
						  ts.l("setXML.bad_commentChild", root.getTreeString()));
	      }

	    _comment = commentChildren[0].getString();
	  }
	else if (item.matches("invisible"))
	  {
	    _visibility = false;	// need a setter?
	  }
	else if (item.matches("typedef"))
	  {
	    if (typeRead)
	      {
		// "XML"
		// "redundant type definition for this field:\n{0}"
		return Ganymede.createErrorDialog(ts.l("global.xmlErrorTitle"),
						  ts.l("setXML.dup_type", root.getTreeString()));
	      }

	    if (item.getAttrStr("type") == null)
	      {
		// "XML"
		// "typedef tag does not contain type attribute:\n{0}"
		return Ganymede.createErrorDialog(ts.l("global.xmlErrorTitle"),
						  ts.l("setXML.missing_type", root.getTreeString()));
	      }

	    if (item.getAttrStr("type").equals("float"))
	      {
		// float has no subchildren, all we need to
		// do is attempt to do a setType

		retVal = setType(FieldType.FLOAT);

		if (!ReturnVal.didSucceed(retVal))
		  {
		    return retVal;
		  }
	      }
	    else if (item.getAttrStr("type").equals("options"))
	      {
		//  has no subchildren, all we need to
		// do is attempt to do a setType

		retVal = setType(FieldType.FIELDOPTIONS);

		if (!ReturnVal.didSucceed(retVal))
		  {
		    return retVal;
		  }
	      }
	    else if (item.getAttrStr("type").equals("date"))
	      {
		// date has no subchildren, all we need to
		// do is attempt to do a setType

		retVal = setType(FieldType.DATE);

		if (!ReturnVal.didSucceed(retVal))
		  {
		    return retVal;
		  }
	      }
	    else if (item.getAttrStr("type").equals("permmatrix"))
	      {
		// permmatrix has no subchildren, all we need to
		// do is attempt to do a setType

		retVal = setType(FieldType.PERMISSIONMATRIX);

		if (!ReturnVal.didSucceed(retVal))
		  {
		    return retVal;
		  }
	      }
	    else if (item.getAttrStr("type").equals("string"))
	      {
		retVal = doStringXML(item);

		if (!ReturnVal.didSucceed(retVal))
		  {
		    return retVal;
		  }
	      }
	    else if (item.getAttrStr("type").equals("invid"))
	      {
		retVal = doInvidXML(item, doLinkResolve);

		if (!ReturnVal.didSucceed(retVal))
		  {
		    return retVal;
		  }
	      }
	    else if (item.getAttrStr("type").equals("numeric"))
	      {
		retVal = doNumericXML(item);

		if (!ReturnVal.didSucceed(retVal))
		  {
		    return retVal;
		  }
	      }
	    else if (item.getAttrStr("type").equals("password"))
	      {
		retVal = doPasswordXML(item);

		if (!ReturnVal.didSucceed(retVal))
		  {
		    return retVal;
		  }
	      }
	    else if (item.getAttrStr("type").equals("ip"))
	      {		
		retVal = doIPXML(item);

		if (!ReturnVal.didSucceed(retVal))
		  {
		    return retVal;
		  }
	      }
	    else if (item.getAttrStr("type").equals("boolean"))
	      {
		retVal = doBooleanXML(item);

		if (!ReturnVal.didSucceed(retVal))
		  {
		    return retVal;
		  }
	      }
	    else
	      {
		// "XML"
		// "typedef tag does not contain recognizable type attribute: {0} in fielddef tree:\n{1}"
		return Ganymede.createErrorDialog(ts.l("global.xmlErrorTitle"),
						  ts.l("setXML.unrecognized_type", item, root.getTreeString()));
	      }

	    typeRead = true;
	  }
	else
	  {
	    // "XML"
	    // "unrecognized XML item: {0} in fielddef tree:\n{1}"
	    return Ganymede.createErrorDialog(ts.l("global.xmlErrorTitle"),
					      ts.l("setXML.unrecognized_item", item, root.getTreeString()));
	  }
      }

    // set the options

    retVal = setComment(_comment);
    
    if (!ReturnVal.didSucceed(retVal))
      {
	// "XML"
	// "fielddef could not set comment:\n{0}\n{1}"
	return Ganymede.createErrorDialog(ts.l("global.xmlErrorTitle"),
					  ts.l("setXML.failed_field_comment", root.getTreeString(), retVal.getDialogText()));
      }

    visibility = _visibility;
    
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
    short _minlength = 0;
    short _maxlength = java.lang.Short.MAX_VALUE;
    String _okChars = null;
    String _badChars = null;
    String _regexp = null;
    String _regexp_desc = null;
    boolean _multiline = false;
    String _namespace = null;
    ReturnVal retVal;

    /* -- */

    if (!root.getAttrStr("type").equals("string"))
      {
	// "bad XMLItem tree:\n{0}"
	throw new IllegalArgumentException(ts.l("global.badItemTree", root));
      }

    retVal = setType(FieldType.STRING);

    if (!ReturnVal.didSucceed(retVal))
      {
	return retVal;
      }

    // the <typedef type="string"> node can have children
    // of its own

    XMLItem typeChildren[] = root.getChildren();

    if (typeChildren != null)
      {
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
		_regexp_desc = child.getAttrStr("desc");
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
		// "XML"
		// "Unrecognized string typedef entity: {0}\nIn field def:\n{1}"
		return Ganymede.createErrorDialog(ts.l("global.xmlErrorTitle"),
						  ts.l("doStringXML.bad_string_typedef_item",
						       child,
						       root.getTreeString()));
	      }
	  }
      }

    // now do all the setting

    retVal = setArray(_vect);

    if (!ReturnVal.didSucceed(retVal))
      {
	// "XML"
	// "fielddef could not set vector bit to {0}:\n{1}\n{2}"
	return Ganymede.createErrorDialog(ts.l("global.xmlErrorTitle"),
					  ts.l("doStringXML.bad_vector_op",
					       Boolean.valueOf(_vect),
					       root.getTreeString(),
					       retVal.getDialogText()));
      }
		
    if (_vect)
      {
	retVal = setMaxArraySize(_maxSize);
	
	if (!ReturnVal.didSucceed(retVal))
	  {
	    // "XML"
	    // "fielddef could not set vector maximum size: {0,number,#}\n{1}\n{2}"
	    return Ganymede.createErrorDialog(ts.l("global.xmlErrorTitle"),
					      ts.l("doStringXML.bad_vector_limit",
						   Integer.valueOf(_maxSize),
						   root.getTreeString(),
						   retVal.getDialogText()));
	  }
      }

    retVal = setMinLength(_minlength);

    if (!ReturnVal.didSucceed(retVal))
      {
	// "XML"
	// "fielddef could not set min length: {0,number,#}\n{1}\n{2}"
	return Ganymede.createErrorDialog(ts.l("global.xmlErrorTitle"),
					  ts.l("doStringXML.bad_min_length",
					       Integer.valueOf(_minlength),
					       root.getTreeString(),
					       retVal.getDialogText()));
      }

    retVal = setMaxLength(_maxlength);

    if (!ReturnVal.didSucceed(retVal))
      {
	// "XML"
	// "fielddef could not set max length: {0,number,#}\n{1}\n{2}"
	return Ganymede.createErrorDialog(ts.l("global.xmlErrorTitle"),
					  ts.l("doStringXML.bad_max_length",
					       Integer.valueOf(_maxlength),
					       root.getTreeString(),
					       retVal.getDialogText()));
      }

    retVal = setOKChars(_okChars);
    
    if (!ReturnVal.didSucceed(retVal))
      {
	// "XML"
	// "fielddef could not set ok chars: {0}\n{1}\n{2}"
	return Ganymede.createErrorDialog(ts.l("global.xmlErrorTitle"),
					  ts.l("doStringXML.bad_ok_chars",
					       _okChars,
					       root.getTreeString(),
					       retVal.getDialogText()));
      }
    
    retVal = setBadChars(_badChars);
    
    if (!ReturnVal.didSucceed(retVal))
      {
	// "XML"
	// "fielddef could not set bad chars: {0}\n{1}\n{2}"
	return Ganymede.createErrorDialog(ts.l("global.xmlErrorTitle"),
					  ts.l("doStringXML.bad_bad_chars",
					       _badChars,
					       root.getTreeString(),
					       retVal.getDialogText()));
      }

    retVal = setRegexpPat(_regexp);
    
    if (!ReturnVal.didSucceed(retVal))
      {
	// "XML"
	// "fielddef could not set regular expression: {0}\n{1}\n{2}"
	return Ganymede.createErrorDialog(ts.l("global.xmlErrorTitle"),
					  ts.l("doStringXML.bad_regexp",
					       _regexp,
					       root.getTreeString(),
					       retVal.getDialogText()));
      }

    retVal = setRegexpDesc(_regexp_desc);

    if (!ReturnVal.didSucceed(retVal))
      {
	// "XML"
	// "fielddef could not set regular expression description: {0}\n{1}\n{2}"
	return Ganymede.createErrorDialog(ts.l("global.xmlErrorTitle"),
					  ts.l("doStringXML.bad_regexp_desc",
					       _regexp_desc,
					       root.getTreeString(),
					       retVal.getDialogText()));
      }

    retVal = setMultiLine(_multiline);
    
    if (!ReturnVal.didSucceed(retVal))
      {
	// "XML"
	// "fielddef could not set multiline: {0}\n{1}\n{2}"
	return Ganymede.createErrorDialog(ts.l("global.xmlErrorTitle"),
					  ts.l("doStringXML.bad_multiline",
					       Boolean.valueOf(_multiline),
					       root.getTreeString(),
					       retVal.getDialogText()));
      }

    retVal = setNameSpace(_namespace);

    if (!ReturnVal.didSucceed(retVal))
      {
	// "XML"
	// "fielddef could not set namespace: {0}\n{1}\n{2}"
	return Ganymede.createErrorDialog(ts.l("global.xmlErrorTitle"),
					  ts.l("doStringXML.bad_namespace",
					       _namespace,
					       root.getTreeString(),
					       retVal.getDialogText()));					  
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
	// "bad XMLItem tree:\n{0}"
	throw new IllegalArgumentException(ts.l("global.badItemTree", root));
      }

    retVal = setType(FieldType.BOOLEAN);

    if (!ReturnVal.didSucceed(retVal))
      {
	return retVal;
      }

    // the <typedef type="boolean"> node can have children
    // of its own

    XMLItem typeChildren[] = root.getChildren();

    if (typeChildren != null)
      {
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
		// "XML"
		// "Unrecognized boolean typedef entity: {0}\nIn field def:\n{1}"
		return Ganymede.createErrorDialog(ts.l("global.xmlErrorTitle"),
						  ts.l("doBooleanXML.bad_boolean_typedef_item",
						       child,
						       root.getTreeString()));
	      }
	  }
      }

    // now do all the setting

    retVal = setLabeled(_labeled);

    if (!ReturnVal.didSucceed(retVal))
      {
	// "XML"
	// "fielddef could not set labeled bit to {0}:\n{1}\n{2}"
	return Ganymede.createErrorDialog(ts.l("global.xmlErrorTitle"),
					  ts.l("doBooleanXML.bad_labeled_bit",
					       Boolean.valueOf(_labeled),
					       root.getTreeString(),
					       retVal.getDialogText()));
      }
		
    if (_labeled)
      {
	retVal = setTrueLabel(_trueLabel);
	
	if (!ReturnVal.didSucceed(retVal))
	  {
	    // "XML"
	    // "fielddef could not set true label to {0}\n{1}\n{2}"
	    return Ganymede.createErrorDialog(ts.l("global.xmlErrorTitle"),
					      ts.l("doBooleanXML.bad_true_label",
						   _trueLabel,
						   root.getTreeString(),
						   retVal.getDialogText()));
	  }

	retVal = setFalseLabel(_falseLabel);
	
	if (!ReturnVal.didSucceed(retVal))
	  {
	    // "XML"
	    // "fielddef could not set false label to {0}\n{1}\n{2}"
	    return Ganymede.createErrorDialog(ts.l("global.xmlErrorTitle"),
					      ts.l("doBooleanXML.bad_false_label",
						   _falseLabel,
						   root.getTreeString(),
						   retVal.getDialogText()));
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
    short _minlength = 0;
    short _maxlength = java.lang.Short.MAX_VALUE;
    String _okChars = null;
    String _badChars = null;
    boolean _crypted = false;
    boolean _plaintext = false;
    boolean _md5crypted = false;
    boolean _apachemd5crypted = false;
    boolean _winHashed = false;
    boolean _sshaHashed = false;
    boolean _shaUnixCrypted = false;
    boolean _shaUnixCrypt512 = false;
    int _shaUnixCryptRounds = 5000;
    ReturnVal retVal;

    /* -- */

    if (!root.getAttrStr("type").equals("password"))
      {
	// "bad XMLItem tree:\n{0}"
	throw new IllegalArgumentException(ts.l("global.badItemTree", root));
      }

    retVal = setType(FieldType.PASSWORD);

    if (!ReturnVal.didSucceed(retVal))
      {
	return retVal;
      }

    // the <typedef type="password"> node can have children
    // of its own

    XMLItem typeChildren[] = root.getChildren();

    if (typeChildren != null)
      {
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
	    else if (child.matches("apacheMd5crypted"))
	      {
		_apachemd5crypted = true;
	      }
	    else if (child.matches("winHashed"))
	      {
		_winHashed = true;
	      }
	    else if (child.matches("sshaHashed"))
	      {
		_sshaHashed = true;
	      }
	    else if (child.matches("shaUnixCrypted"))
	      {
		_shaUnixCrypted = true;

		String typeVal = child.getAttrStr("type");

		if (typeVal == null || typeVal.equals("256"))
		  {
		    _shaUnixCrypt512 = false;
		  }
		else if (typeVal.equals("512"))
		  {
		    _shaUnixCrypt512 = true;
		  }
		else
		  {
		    return Ganymede.createErrorDialog(ts.l("global.xmlErrorTitle"),
						      ts.l("doPasswordXML.bad_sha_unix_type",
							   child, root.getTreeString()));
		  }

		Integer roundCount = child.getAttrInt("rounds");

		if (roundCount != null)
		  {
		    _shaUnixCryptRounds = roundCount.intValue();

		    if (_shaUnixCryptRounds < 1000 || _shaUnixCryptRounds > 999999999)
		      {
			return Ganymede.createErrorDialog(ts.l("global.xmlErrorTitle"),
							  ts.l("doPasswordXML.bad_sha_unix_rounds",
							       child, root.getTreeString()));
		      }
		  }
	      }
	    else if (child.matches("plaintext"))
	      {
		_plaintext = true;
	      }
	    else
	      {
		// "XML"
		// "Unrecognized password typedef entity: {0}\nIn field def:\n{1}"
		return Ganymede.createErrorDialog(ts.l("global.xmlErrorTitle"),
						  ts.l("doPasswordXML.bad_password_typedef_item",
						       child, root.getTreeString()));
	      }
	  }
      }

    // now do all the setting

    retVal = setMinLength(_minlength);

    if (!ReturnVal.didSucceed(retVal))
      {
	// "XML"
	// "fielddef could not set min length: {0,number,#}\n{1}\n{2}"
	return Ganymede.createErrorDialog(ts.l("global.xmlErrorTitle"),
					  ts.l("doPasswordXML.bad_min_length",
					       Integer.valueOf(_minlength),
					       root.getTreeString(), retVal.getDialogText()));
      }

    retVal = setMaxLength(_maxlength);

    if (!ReturnVal.didSucceed(retVal))
      {
	// "XML"
	// "fielddef could not set max length: {0,number,#}\n{1}\n{2}"
	return Ganymede.createErrorDialog(ts.l("global.xmlErrorTitle"),
					  ts.l("doPasswordXML.bad_max_length", Integer.valueOf(_maxlength),
					       root.getTreeString(), retVal.getDialogText()));
      }

    retVal = setOKChars(_okChars);
    
    if (!ReturnVal.didSucceed(retVal))
      {
	// "XML"
	// "fielddef could not set ok chars: {0}\n{1}\n{2}"
	return Ganymede.createErrorDialog(ts.l("global.xmlErrorTitle"),
					  ts.l("doPasswordXML.bad_ok_chars", _okChars,
					       root.getTreeString(), retVal.getDialogText()));
      }
    
    retVal = setBadChars(_badChars);
    
    if (!ReturnVal.didSucceed(retVal))
      {
	// "XML"
	// "fielddef could not set bad chars: {0}\n{1}\n{2}"
	return Ganymede.createErrorDialog(ts.l("global.xmlErrorTitle"),
					  ts.l("doPasswordXML.bad_bad_chars", _badChars,
					       root.getTreeString(), retVal.getDialogText()));
      }

    retVal = setCrypted(_crypted);

    if (!ReturnVal.didSucceed(retVal))
      {
	// "XML"
	// "fielddef could not set crypted flag: {0}\n{1}\n{2}"
	return Ganymede.createErrorDialog(ts.l("global.xmlErrorTitle"),
					  ts.l("doPasswordXML.bad_crypted", Boolean.valueOf(_crypted),
					       root.getTreeString(), retVal.getDialogText()));
      }

    retVal = setMD5Crypted(_md5crypted);

    if (!ReturnVal.didSucceed(retVal))
      {
	// "XML"
	// "fielddef could not set md5 crypted flag: {0}\n{1}\n{2}"
	return Ganymede.createErrorDialog(ts.l("global.xmlErrorTitle"),
					  ts.l("doPasswordXML.bad_md5_crypted", Boolean.valueOf(_md5crypted),
					       root.getTreeString(), retVal.getDialogText()));
      }

    retVal = setApacheMD5Crypted(_apachemd5crypted);

    if (!ReturnVal.didSucceed(retVal))
      {
	// "XML"
	// "fielddef could not set md5 crypted flag: {0}\n{1}\n{2}"
	return Ganymede.createErrorDialog(ts.l("global.xmlErrorTitle"),
					  ts.l("doPasswordXML.bad_apache_md5_crypted",
					       Boolean.valueOf(_apachemd5crypted),
					       root.getTreeString(),
					       retVal.getDialogText()));
      }

    retVal = setWinHashed(_winHashed);

    if (!ReturnVal.didSucceed(retVal))
      {
	// "XML"
	// "fielddef could not set windows hashing flag: {0}\n{1}\n{2}"
	return Ganymede.createErrorDialog(ts.l("global.xmlErrorTitle"),
					  ts.l("doPasswordXML.bad_windows_hashed",
					       Boolean.valueOf(_winHashed),
					       root.getTreeString(),
					       retVal.getDialogText()));
      }

    retVal = setSSHAHashed(_sshaHashed);

    if (!ReturnVal.didSucceed(retVal))
      {
	// "XML"
	// "fielddef could not set SSHA hashing flag: {0}\n{1}\n{2}"
	return Ganymede.createErrorDialog(ts.l("global.xmlErrorTitle"),
					  ts.l("doPasswordXML.bad_ssha_hashed",
					       Boolean.valueOf(_sshaHashed),
					       root.getTreeString(),
					       retVal.getDialogText()));
      }

    retVal = setShaUnixCrypted(_shaUnixCrypted);

    if (!ReturnVal.didSucceed(retVal))
      {
	// "XML"
	// "fielddef could not set SHA Unix Crypt hashing flag: {0}\n{1}\n{2}"
	return Ganymede.createErrorDialog(ts.l("global.xmlErrorTitle"),
					  ts.l("doPasswordXML.bad_sha_unix_crypted",
					       Boolean.valueOf(_shaUnixCrypted),
					       root.getTreeString(),
					       retVal.getDialogText()));
      }

    if (_shaUnixCrypted)
      {
	retVal = setShaUnixCrypted512(_shaUnixCrypt512);

	if (!ReturnVal.didSucceed(retVal))
	  {
	    // we should already have caught any XML error above,
	    // here, so I'm not going to bother wrapping the error

	    return retVal;
	  }
	
	retVal = setShaUnixCryptRounds(_shaUnixCryptRounds);

	if (!ReturnVal.didSucceed(retVal))
	  {
	    // we should already have caught any XML error above,
	    // here, so I'm not going to bother wrapping the error

	    return retVal;
	  }
      }

    retVal = setPlainText(_plaintext);

    if (!ReturnVal.didSucceed(retVal))
      {
	// "XML"
	// "fielddef could not set plaintext flag: {0}\n{1}\n{2}"
	return Ganymede.createErrorDialog(ts.l("global.xmlErrorTitle"),
					  ts.l("doPasswordXML.bad_plaintext",
					       Boolean.valueOf(_plaintext),
					       root.getTreeString(),
					       retVal.getDialogText()));
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
	// "bad XMLItem tree:\n{0}"
	throw new IllegalArgumentException(ts.l("global.badItemTree", root));
      }

    retVal = setType(FieldType.IP);

    if (!ReturnVal.didSucceed(retVal))
      {
	return retVal;
      }

    // the <typedef type="ip"> node can have children
    // of its own

    XMLItem typeChildren[] = root.getChildren();

    if (typeChildren != null)
      {
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
		// "XML"
		// "Unrecognized IP typedef entity: {0}\nIn field def:\n{1}"
		return Ganymede.createErrorDialog(ts.l("global.xmlErrorTitle"),
						  ts.l("doIPXML.bad_ip_typedef_item",
						       child, root.getTreeString()));
	      }
	  }
      }

    // now do all the setting

    retVal = setArray(_vect);
  
    if (!ReturnVal.didSucceed(retVal))
      {
	// "XML"
	// "fielddef could not set vector bit to {0}:\n{1}\n{2}"
	return Ganymede.createErrorDialog(ts.l("global.xmlErrorTitle"),
					  ts.l("doIPXML.bad_vector_op",
					       Boolean.valueOf(_vect),
					       root.getTreeString(),
					       retVal.getDialogText()));
      }
		
    if (_vect)
      {
	retVal = setMaxArraySize(_maxSize);
	
	if (!ReturnVal.didSucceed(retVal))
	  {
	    // "XML"
	    // "fielddef could not set vector maximum size: {0,number,#}\n{1}\n{2}"
	    return Ganymede.createErrorDialog(ts.l("global.xmlErrorTitle"),
					      ts.l("doIPXML.bad_vector_limit",
						   Integer.valueOf(_maxSize),
						   root.getTreeString(),
						   retVal.getDialogText()));
	  }
      }

    retVal = setNameSpace(_namespace);

    if (!ReturnVal.didSucceed(retVal))
      {
	// "XML"
	// "fielddef could not set namespace: {0}\n{1}\n{2}"
	return Ganymede.createErrorDialog(ts.l("global.xmlErrorTitle"),
					  ts.l("doIPXML.bad_namespace",
					       _namespace,
					       root.getTreeString(),
					       retVal.getDialogText()));
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

    if (!root.getAttrStr("type").equals("numeric"))
      {
	// "bad XMLItem tree:\n{0}"
	throw new IllegalArgumentException(ts.l("global.badItemTree", root));
      }

    retVal = setType(FieldType.NUMERIC);

    if (!ReturnVal.didSucceed(retVal))
      {
	return retVal;
      }

    // the <typedef type="numeric"> node can have children
    // of its own

    XMLItem typeChildren[] = root.getChildren();

    if (typeChildren != null)
      {
	for (int j = 0; j < typeChildren.length; j++)
	  {
	    XMLItem child = typeChildren[j];
	    
	    if (child.matches("namespace"))
	      {
		_namespace = child.getAttrStr("val");
	      }
	    else
	      {
		// "XML"
		// "Unrecognized numeric typedef entity: {0}\nIn field def:\n{1}"
		return Ganymede.createErrorDialog(ts.l("global.xmlErrorTitle"),
						  ts.l("doNumericXML.bad_numeric_typedef_item",
						       child, root.getTreeString()));
	      }
	  }
      }

    // now do all the setting

    retVal = setNameSpace(_namespace);

    if (!ReturnVal.didSucceed(retVal))
      {
	// "XML"
	// "fielddef could not set namespace: {0}\n{1}\n{2}"
	return Ganymede.createErrorDialog(ts.l("global.xmlErrorTitle"),
					  ts.l("doNumericXML.bad_namespace", _namespace,
					       root.getTreeString(), retVal.getDialogText()));
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
	// "bad XMLItem tree:\n{0}"
	throw new IllegalArgumentException(ts.l("global.badItemTree", root));
      }

    retVal = setType(FieldType.INVID);

    if (!ReturnVal.didSucceed(retVal))
      {
	return retVal;
      }

    // the <typedef type="invid"> node can have children
    // of its own

    XMLItem typeChildren[] = root.getChildren();

    if (typeChildren != null)
      {
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
		_targetobjectStr = XMLUtils.XMLDecode(child.getAttrStr("name"));
		_targetobject = child.getAttrInt("id");
	    
		if (_targetobjectStr == null && _targetobject == null)
		  {
		    // "XML"
		    // "targetobject item does not specify name or id: {0}\n{1}\n{2}"
		    return Ganymede.createErrorDialog(ts.l("global.xmlErrorTitle"),
						      ts.l("doInvidXML.bad_target_def",
							   child, root.getTreeString(), retVal.getDialogText()));
		  }
	      }
	    else if (child.matches("targetfield"))
	      {
		_targetfieldStr = XMLUtils.XMLDecode(child.getAttrStr("name"));
		_targetfield = child.getAttrInt("id");

		if (_targetfieldStr == null && _targetfield == null)
		  {
		    // "XML"
		    // "targetfield item does not specify name or id: {0}\n{1}\n{2}"
		    return Ganymede.createErrorDialog(ts.l("global.xmlErrorTitle"),
						      ts.l("doInvidXML.bad_target_field_def",
							   child, root.getTreeString(), retVal.getDialogText()));
		  }
	      }
	    else if (child.matches("embedded"))
	      {
		_embedded = true;
	      }
	    else
	      {
		// "XML"
		// "Unrecognized invid typedef entity: {0}\nIn field def:\n{1}"
		return Ganymede.createErrorDialog(ts.l("global.xmlErrorTitle"),
						  ts.l("doInvidXML.bad_invid_typedef_item",
						       child, root.getTreeString()));
	      }
	  }
      }

    // now do all the setting

    retVal = setArray(_vect);

    if (!ReturnVal.didSucceed(retVal))
      {
	// "XML"
	// "fielddef could not set vector bit to {0}:\n{1}\n{2}"
	return Ganymede.createErrorDialog(ts.l("global.xmlErrorTitle"),
					  ts.l("doInvidXML.bad_vector_op",
					       Boolean.valueOf(_vect), root.getTreeString(), retVal.getDialogText()));
      }
		
    if (_vect)
      {
	retVal = setMaxArraySize(_maxSize);
	
	if (!ReturnVal.didSucceed(retVal))
	  {
	    // "XML"
	    // "fielddef could not set vector maximum size: {0,number,#}\n{1}\n{2}"
	    return Ganymede.createErrorDialog(ts.l("global.xmlErrorTitle"),
					      ts.l("doInvidXML.bad_vector_limit", Integer.valueOf(_maxSize),
						   root.getTreeString(), retVal.getDialogText()));
	  }
      }

    if (doLinkResolve)
      {
	// first we try to set the target object type, if any

	if (_targetobjectStr != null)
	  {
	    if (_targetobjectStr.equals("*any*"))
	      {
		retVal = setTargetBase((short)-2);
	      }
	    else
	      {
		retVal = setTargetBase(_targetobjectStr);
	      }

	    if (!ReturnVal.didSucceed(retVal))
	      {
		// "XML"
		// "fielddef could not set invid target base: {0}\n{1}\n{2}"
		return Ganymede.createErrorDialog(ts.l("global.xmlErrorTitle"),
						  ts.l("doInvidXML.bad_invid_target_base",
						       _targetobjectStr,
						       root.getTreeString(),
						       retVal.getDialogText()));
	      }
	  }
	else if (_targetobject != null)
	  {
	    retVal = setTargetBase(_targetobject.shortValue());

	    if (!ReturnVal.didSucceed(retVal))
	      {
		// "XML"
		// "fielddef could not set invid target base: {0,number,#}\n{1}\n{2}"
		return Ganymede.createErrorDialog(ts.l("global.xmlErrorTitle"),
						  ts.l("doInvidXML.bad_invid_target_base_num",
						       _targetobject,
						       root.getTreeString(),
						       retVal.getDialogText()));
	      }
	  }
	else // both null
	  {
	    retVal = setTargetBase(null);

	    if (!ReturnVal.didSucceed(retVal))
	      {
		// "XML"
		// "fielddef could not clear invid target base:\n{0}\n{1}"
		return Ganymede.createErrorDialog(ts.l("global.xmlErrorTitle"),
						  ts.l("doInvidXML.bad_null_target_base",
						       root.getTreeString(),
						       retVal.getDialogText()));
	      }
	  }

	// then we try to set the target field, if any.  we don't
	// track target fields for edit-in-place invid fields, though.

	if (_targetfieldStr != null && !_embedded)
	  {
	    retVal = setTargetField(_targetfieldStr);

	    if (!ReturnVal.didSucceed(retVal))
	      {
		// "XML"
		// "fielddef could not set invid target field: {0}\n{1}\n{2}"
		return Ganymede.createErrorDialog(ts.l("global.xmlErrorTitle"),
						  ts.l("doInvidXML.bad_invid_target_field",
						       _targetfieldStr,
						       root.getTreeString(),
						       retVal.getDialogText()));
	      }
	  }
	else if (_targetfield != null && !_embedded)
	  {
	    retVal = setTargetField(_targetfield.shortValue());

	    if (!ReturnVal.didSucceed(retVal))
	      {
		// "XML"
		// "fielddef could not set invid target field: {0,number,#}\n{1}\n{2}"
		return Ganymede.createErrorDialog(ts.l("global.xmlErrorTitle"),
						  ts.l("doInvidXML.bad_invid_target_field_num",
						       _targetfield,
						       root.getTreeString(),
						       retVal.getDialogText()));
	      }
	  }
	else // both null
	  {
	    retVal = setTargetField(null);

	    if (!ReturnVal.didSucceed(retVal))
	      {
		// "XML"
		// "fielddef could not clear invid target field:\n{0}\n{1}"
		return Ganymede.createErrorDialog(ts.l("global.xmlErrorTitle"),
						  ts.l("doInvidXML.bad_null_target_field",
						       root.getTreeString(),
						       retVal.getDialogText()));
	      }
	  }
      }

    retVal = setEditInPlace(_embedded);

    if (!ReturnVal.didSucceed(retVal))
      {
	// "XML"
	// "fielddef could not set embedded status: {0}\n{1}\n{2}"
	return Ganymede.createErrorDialog(ts.l("global.xmlErrorTitle"),
					  ts.l("doInvidXML.bad_embedded_status",
					       Boolean.valueOf(_embedded),
					       root.getTreeString(),
					       retVal.getDialogText()));
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
   * @see arlut.csd.ganymede.rmi.BaseField
   */

  public boolean isRemovable()
  {
    return !isSystemField();
  }

  /**
   * <p>This method returns true if this field
   * is intended to be visible to the client normally,
   * false otherwise.</p>
   *
   * @see arlut.csd.ganymede.rmi.BaseField
   */

  public boolean isVisible()
  {
    return visibility;
  }

  /**
   * Method to set the visibility or invisibility of this field.
   *
   * Used by the DBStore to mark certain scratch fields as being
   * permanently hidden without having to set a custom DBEditObject
   * subclass to declare the non-visibility of the field.
   */

  public void setVisibility(boolean visibility)
  {
    securityCheck();

    this.visibility = visibility;
  }
  
  /**
   * Server-side method used to set the status of this field's
   * isInUseCache.
   */

  public void setIsInUse(Boolean val)
  {
    inUseCache = val;
  }

  /**
   * This method returns true if there is any concern that there
   * are fields of this type in use in the database.  The schema
   * editing system uses this method to prevent incompatible
   * modifications to fields that are in use in the database.
   *
   * This method will always return false when the DBObjectBase is
   * newly created, is being initialized, or is being loaded.
   *
   * At other times, this method may seek through the entire
   * collection of objects held in the containing DBObjectBase to see
   * if any instances of this field exist.
   */

  private boolean isInUse()
  {
    switch (base.getEditingMode())
      {
      case CREATING:
      case INITIALIZING:
      case LOADING:
	return false;
      }

    if (inUseCache == null)
      {
	inUseCache = Boolean.valueOf(((DBObjectBase) this.getBase()).fieldInUse(this));
      }

    return inUseCache.booleanValue();
  }

  /**
   * <p>Returns the Base we are a part of.</p>
   *
   * @see arlut.csd.ganymede.rmi.BaseField
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
   * @see arlut.csd.ganymede.rmi.BaseField
   */

  public String getName()
  {
    return field_name;
  }

  /**
   * <p>Sets the name of this field</p>
   *
   * @param name The new name to put in this field
   * @see arlut.csd.ganymede.rmi.BaseField
   */

  public ReturnVal setName(String name)
  {
    return setName(name, false);
  }

  /**
   * <p>Sets the name of this field</p>
   *
   * @param name The new name to put in this field
   * @param swapIfNeeded If true, attempting to set this field's name
   * to a name that is already taken in the object will result in this
   * field's taking the new name from the other field and giving that
   * other field its own name.  Only intended for use by setXML(), which
   * has higher-level code to check for uniqueness of names in an XML
   * schema definition.
   */

  public synchronized ReturnVal setName(String name, boolean swapIfNeeded)
  {
    securityCheck();

    // if we aren't loading, don't allow messing with the global fields

    if (isEditingProtectedField())
      {
	// "Schema Editing Error"
	// "Can''t change the name of a system field."
	return Ganymede.createErrorDialog(ts.l("global.schema_editing_error"),
					  ts.l("setName.system_field"));
      }
    
    if (name == null || name.equals(""))
      {
	// "Schema Editing Error"
	// "Can''t have a null or empty name."
	return Ganymede.createErrorDialog(ts.l("global.schema_editing_error"),
					  ts.l("setName.null_name"));
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

    DBObjectBaseField otherField = (DBObjectBaseField) ((DBObjectBase) getBase()).getField(name);

    if (otherField != null)
      {
	if (!swapIfNeeded)
	  {
	    // "Schema Editing Error"
	    // "Can''t set a duplicate field name, "{0}" is already taken."
	    return Ganymede.createErrorDialog(ts.l("global.schema_editing_error"),
					      ts.l("setName.duplicate_name", name));
	  }
	else
	  {
	    // the xml schema code is setting this name, and it will have checked
	    // to make sure the name is unique.. we'll give the field that already
	    // has the name we want our name in trade, and then take the new name
	    // ourselves.  the xml schema code will fix it up when it goes to
	    // set the name on the other.

	    String oldName = this.field_name;
	    this.field_name = name;

	    ReturnVal retVal = otherField.setName(oldName);

	    if (!ReturnVal.didSucceed(retVal))
	      {
		return retVal;
	      }
	  }
      }

    field_name = name;

    return null;
  }

  /**
   * Returns the name of the tab that is to contain this field on the client.
   *
   * @see arlut.csd.ganymede.rmi.BaseField
   */

  public String getTabName()
  {
    return tabName;
  }

  /**
   * Sets the name of the tab that is to contain this field on the client.
   *
   * @see arlut.csd.ganymede.rmi.BaseField
   */

  public synchronized ReturnVal setTabName(String s)
  {
    securityCheck();

    if (s == null || s.equals(""))
      {
	// "Schema Editing Error"
	// "Can''t have a null or empty tab name."
	return Ganymede.createErrorDialog(ts.l("global.schema_editing_error"),
					  ts.l("setTabName.null_name"));
      }

    this.tabName = s;
    
    return null;
  }      

  /**
   * <p>Returns the comment defined in the schema for this field</p>
   *
   * @see arlut.csd.ganymede.rmi.BaseField
   */

  public String getComment()
  {
    return comment;
  }

  /**
   * <p>Sets the comment defined in the schema for this field</p>
   *
   * @see arlut.csd.ganymede.rmi.BaseField
   */

  public synchronized ReturnVal setComment(String s)
  {
    securityCheck();

    if (s == null || s.equals(""))
      {
	comment = null;
      }
    else
      {
	comment = s;
      }
    
    return null;
  }      

  /**
   * <p>Returns the field type</p>
   *
   * <p>Where type is one of the following
   * constants defined in the {@link arlut.csd.ganymede.common.FieldType FieldType}
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
   *   static short FIELDOPTIONS = 9;
   * </pre>
   *
   * @see arlut.csd.ganymede.server.DBStore
   * @see arlut.csd.ganymede.rmi.BaseField
   */

  public short getType()
  {
    return field_type;
  }

  /**
   * <p>Sets the {@link arlut.csd.ganymede.common.FieldType field type}
   * for this field.  Changing the basic type of a field that is already being
   * used in the server will cause very bad things to happen.  The
   * right way to change an existing field is to delete the field, commit
   * the schema edit, edit the schema again, and recreate the field with
   * the desired field type.</P>
   *
   * <p>If the new field type is not string, invid, or IP, the field
   * will be made a scalar field.</p>
   *
   * @see arlut.csd.ganymede.rmi.BaseField
   */

  public synchronized ReturnVal setType(short type)
  {
    securityCheck();

    if (type < FIRSTFIELD || type > LASTFIELD)
      {
	// "Type argument out of range"
	throw new IllegalStateException(ts.l("setType.bad_type"));
      }

    // if no change, no problem.

    if (type == field_type)
      {
	return null;
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

    // don't allow global fields to be messed with

    if (isEditingProtectedSystemField())
      {
	// "Can''t change the type of a system field: {0}"
	return Ganymede.createErrorDialog(ts.l("global.schema_editing_error"),
					  ts.l("global.system_field_change_attempt", this.toString()));
      }

    if (isInUse())
      {
	// "Can''t change the type of a field which is in use in the database: {0}"
	return Ganymede.createErrorDialog(ts.l("global.schema_editing_error"),
					  ts.l("setType.in_use", this.toString()));
      }

    if (isInvid())
      {
	// need to check to make sure no other invid field definitions are
	// pointing to this field somehow, else changing type might break
	// that other field definition
      }

    field_type = type;

    // only strings, invids, and ip fields can be vectors

    if (!(isString() || isInvid() || isIP()))
      {
	array = false;
      }

    return null;
  }

  // type identification convenience methods

  /**
   * <p>Returns true if this field is of boolean type</p>
   *
   * @see arlut.csd.ganymede.rmi.BaseField
   */

  public boolean isBoolean()
  {
    return (field_type == BOOLEAN);
  }

  /**
   * <p>Returns true if this field is of numeric type</p>
   *
   * @see arlut.csd.ganymede.rmi.BaseField
   */

  public boolean isNumeric()
  {
    return (field_type == NUMERIC);
  }

  /**
   * <p>Returns true if this field is of float type</p>
   *
   * @see arlut.csd.ganymede.rmi.BaseField
   */

  public boolean isFloat()
  {
    return (field_type == FLOAT);
  }

  /**
   * <p>Returns true if this field is of float type</p>
   *
   * @see arlut.csd.ganymede.rmi.BaseField
   */

  public boolean isFieldOptions()
  {
    return (field_type == FIELDOPTIONS);
  }

  /**
   * <p>Returns true if this field is of date type</p>
   *
   * @see arlut.csd.ganymede.rmi.BaseField
   */

  public boolean isDate()
  {
    return (field_type == DATE);
  }

  /**
   * <p>Returns true if this field is of string type</p>
   *
   * @see arlut.csd.ganymede.rmi.BaseField
   */

  public boolean isString()
  {
    return (field_type == STRING);
  }

  /**
   * <p>Returns true if this field is of invid type</p>
   *
   * @see arlut.csd.ganymede.rmi.BaseField
   */

  public boolean isInvid()
  {
    return (field_type == INVID);
  }

  /**
   * <p>Returns true if this field is of permission matrix type</p>
   *
   * @see arlut.csd.ganymede.rmi.BaseField
   */

  public boolean isPermMatrix()
  {
    return (field_type == PERMISSIONMATRIX);
  }

  /**
   * <p>Returns true if this field is of password type</p>
   *
   * @see arlut.csd.ganymede.rmi.BaseField
   */

  public boolean isPassword()
  {
    return (field_type == PASSWORD);
  }

  /**
   * <p>Returns true if this field is of IP type</p>
   *
   * @see arlut.csd.ganymede.rmi.BaseField
   */

  public boolean isIP()
  {
    return (field_type == IP);
  }
  
  /**
   * <p>Returns true if this field is a vector field, false otherwise.</p>
   *
   * @see arlut.csd.ganymede.rmi.BaseField
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
   * @see arlut.csd.ganymede.rmi.BaseField 
   */

  public synchronized ReturnVal setArray(boolean b)
  {
    securityCheck();

    if (isEditingProtectedSystemField())
      {
	// array-ness is way too critical to be edited, even in mildly variable system
	// fields like username in the user object

	// "Can''t change the vector status of a system field: {0}"
	return Ganymede.createErrorDialog(ts.l("global.schema_editing_error"),
					  ts.l("setArray.any_system_field", this.toString()));
      }

    if (b != array && isInUse())
      {
	// "Can''t change the vector status of a field which is in use in the database: {0}"
	return Ganymede.createErrorDialog(ts.l("global.schema_editing_error"),
					  ts.l("setArray.in_use", this.toString()));
      }

    if (b && !(isString() || isInvid() || isIP()))
      {
	// "Can''t set this field type ({0}) to be a vector field: {1}"
	return Ganymede.createErrorDialog(ts.l("global.schema_editing_error"),
					  ts.l("setArray.bad_type", this.getTypeDesc(), this.toString()));
      }

    array = b;

    return null;
  }

  /**
   * <p>Returns id code for this field.  Each field in a
   * {@link arlut.csd.ganymede.server.DBObject DBObject}
   * has a unique code which identifies the field.  This code represents
   * the field in the on-disk data store, and is used by 
   * {@link arlut.csd.ganymede.server.DBEditObject DBEditObject}
   * to choose what field to change in the setField method.</p>
   *
   * @see arlut.csd.ganymede.rmi.BaseField
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
    securityCheck();

    if (id < 0)
      {
	// "Field id number {0,number,#} is out of range: {1}."
	return Ganymede.createErrorDialog(ts.l("global.schema_editing_error"),
					  ts.l("setID.out_of_range", Integer.valueOf(id),
					       this.toString()));
      }

    // no change, no problem

    if (id == field_code)
      {
	return null;
      }

    if (field_code >= 0)
      {
	// "Can''t change field id number for a previously created field definition: {0}."
	return Ganymede.createErrorDialog(ts.l("global.schema_editing_error"),
					  ts.l("setID.already_set", this.toString()));
      }

    if (base.getField(id) != null)
      {
	// "Can''t set field id number {0,number,#} on field {1}.  That field id number is already in use by another field definition."
	return Ganymede.createErrorDialog(ts.l("global.schema_editing_error"),
					  ts.l("setID.in_use", Integer.valueOf(id),
					       this.toString()));
      }

    field_code = id;

    return null;
  }

  /**
   * <p>Returns the object definition that this field is defined under.</p>
   *
   * @see arlut.csd.ganymede.rmi.BaseField
   */

  public DBObjectBase base()
  {
    return base;
  }

  // **
  // array attribute methods
  // **

  /**
   * <p>Returns the array size limitation for this field if it is an array field</p>
   *
   * @see arlut.csd.ganymede.rmi.BaseField
   */

  public short getMaxArraySize()
  {
    if (!array)
      {
	throw new IllegalStateException(ts.l("global.not_array", this.toString()));
      }

    return limit;
  }

  /**
   * <p>Set the maximum number of values allowed in this vector field.</p>
   *
   * @see arlut.csd.ganymede.rmi.BaseField 
   */

  public synchronized ReturnVal setMaxArraySize(short limit)
  {
    securityCheck();

    if (!array)
      {
	throw new IllegalStateException(ts.l("global.not_array", this.toString()));
      }

    // no change, no problem

    if (limit == this.limit)
      {
	return null;
      }

    if (isEditingProtectedSystemField())
      {
	// array sizes need not be screwed with in the system fields

	// "Can''t change the vector limits of a system field: {0}"
	return Ganymede.createErrorDialog(ts.l("global.schema_editing_error"),
					  ts.l("setMaxArraySize.any_system_field", this.toString()));
      }

    this.limit = limit;

    if (isEditing() && isInUse())
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
   * @see arlut.csd.ganymede.rmi.BaseField
   */

  public boolean isLabeled()
  {
    if (!isBoolean())
      {
	throw new IllegalStateException(ts.l("global.not_boolean", this.toString()));
      }
    
    return labeled;
  }

  /**
   * <p>Turn labeled choices on/off for a boolean field.</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not a boolean type.</p>
   *
   * @see arlut.csd.ganymede.rmi.BaseField
   */

  public synchronized ReturnVal setLabeled(boolean b)
  {
    securityCheck();

    if (isEditingProtectedField())
      {
	return Ganymede.createErrorDialog(ts.l("global.schema_editing_error"),
					  ts.l("global.system_field", this.toString()));
      }

    if (!isBoolean())
      {
	throw new IllegalStateException(ts.l("global.not_boolean", this.toString()));
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
   * @see arlut.csd.ganymede.rmi.BaseField
   */

  public String getTrueLabel()
  {
    if (isLabeled())
      {
	return trueLabel;
      }

    throw new IllegalStateException(ts.l("global.not_labeled_boolean", this.toString()));
  }

  /**
   * <p>Sets the label associated with the true choice for this
   * boolean field.</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not a labeled boolean type.</p>
   *
   * @see arlut.csd.ganymede.rmi.BaseField
   */

  public synchronized ReturnVal setTrueLabel(String label)
  {
    securityCheck();

    if (isEditingProtectedField())
      {
	return Ganymede.createErrorDialog(ts.l("global.schema_editing_error"),
					  ts.l("global.system_field", this.toString()));
      }

    if (isLabeled())
      {
	trueLabel = label;
      }
    else
      {
	throw new IllegalStateException(ts.l("global.not_labeled_boolean", this.toString()));
      }

    return null;
  }

  /**
   * <p>Returns the false Label if this is a labeled boolean field</p> 
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not a labeled boolean type.</p>
   *
   * @see arlut.csd.ganymede.rmi.BaseField
   */

  public String getFalseLabel()
  {
    if (isLabeled())
      {
	return falseLabel;
      }

    throw new IllegalStateException(ts.l("global.not_labeled_boolean", this.toString()));
  }

  /**
   * <p>Sets the label associated with the false choice for this
   * boolean field.</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not a labeled boolean type.</p>
   *
   * @see arlut.csd.ganymede.rmi.BaseField
   */

  public synchronized ReturnVal setFalseLabel(String label)
  {
    securityCheck();

    if (isEditingProtectedField())
      {
	return Ganymede.createErrorDialog(ts.l("global.schema_editing_error"),
					  ts.l("global.system_field", this.toString()));
      }

    if (isLabeled())
      {
	falseLabel = label;
      }
    else
      {
	throw new IllegalStateException(ts.l("global.not_labeled_boolean", this.toString()));
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
   * @see arlut.csd.ganymede.rmi.BaseField
   */

  public short getMinLength()
  {
    if (!isString() && !isPassword())
      {
	throw new IllegalStateException(ts.l("global.not_string_or_password", this.toString()));
      }

    return minLength;
  }

  /**
   * <p>Sets the minimum acceptable length for this string or password field.</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not a string or password type.</p>
   *
   * @see arlut.csd.ganymede.rmi.BaseField
   */

  public synchronized ReturnVal setMinLength(short val)
  {
    securityCheck();

    if (!isString() && !isPassword())
      {
	throw new IllegalStateException(ts.l("global.not_string_or_password", this.toString()));
      }

    // no change, no problem

    if (val == minLength)
      {
	return null;
      }

    if (isEditingProtectedField())
      {
	return Ganymede.createErrorDialog(ts.l("global.schema_editing_error"),
					  ts.l("global.system_field", this.toString()));
      }
    
    minLength = val;

    if (isEditing() && isInUse())
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
   * @see arlut.csd.ganymede.rmi.BaseField
   */

  public short getMaxLength()
  {
    if (!isString() && !isPassword())
      {
	throw new IllegalStateException(ts.l("global.not_string_or_password", this.toString()));
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
   * @see arlut.csd.ganymede.rmi.BaseField 
   */

  public synchronized ReturnVal setMaxLength(short val)
  {
    securityCheck();

    if (!isString() && !isPassword())
      {
	throw new IllegalStateException(ts.l("global.not_string_or_password", this.toString()));
      }

    // no change, no problem

    if (val == maxLength)
      {
	return null;
      }

    if (isEditingProtectedField())
      {
	return Ganymede.createErrorDialog(ts.l("global.schema_editing_error"),
					  ts.l("global.system_field", this.toString()));
      }
    
    maxLength = val;

    if (isEditing() && isInUse())
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
   * @see arlut.csd.ganymede.rmi.BaseField
   */

  public String getOKChars()
  {
    if (!isString() && !isPassword())
      {
	throw new IllegalStateException(ts.l("global.not_string_or_password", this.toString()));
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
   * @see arlut.csd.ganymede.rmi.BaseField
   */

  public synchronized ReturnVal setOKChars(String s)
  {
    securityCheck();

    if (isEditingProtectedField())
      {
	return Ganymede.createErrorDialog(ts.l("global.schema_editing_error"),
					  ts.l("global.system_field", this.toString()));
      }

    if (!isString() && !isPassword())
      {
	throw new IllegalStateException(ts.l("global.not_string_or_password", this.toString()));
      }

    if (s != null && s.equals(""))
      {
	okChars = null;
      }
    else
      {
	okChars = s;
      }

    if (isEditing() && isInUse())
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
   * @see arlut.csd.ganymede.rmi.BaseField
   */

  public String getBadChars()
  {
    if (!isString() && !isPassword())
      {
	throw new IllegalStateException(ts.l("global.not_string_or_password", this.toString()));
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
   * @see arlut.csd.ganymede.rmi.BaseField 
   */

  public synchronized ReturnVal setBadChars(String s)
  {
    securityCheck();

    if (!isString() && !isPassword())
      {
	throw new IllegalStateException(ts.l("global.not_string_or_password", this.toString()));
      }

    if (isEditingProtectedField())
      {
	return Ganymede.createErrorDialog(ts.l("global.schema_editing_error"),
					  ts.l("global.system_field", this.toString()));
      }

    if (s != null && s.equals(""))
      {
	badChars = null;
      }
    else
      {
	badChars = s;
      }

    if (isEditing() && isInUse())
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
   * @see arlut.csd.ganymede.rmi.BaseField
   */

  public boolean isMultiLine()
  {
    if (!isString())
      {
	throw new IllegalStateException(ts.l("global.not_string", this.toString()));
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
   * @see arlut.csd.ganymede.rmi.BaseField 
   */

  public synchronized ReturnVal setMultiLine(boolean b)
  {
    securityCheck();

    if (!isString())
      {
	throw new IllegalStateException(ts.l("global.not_string", this.toString()));
      }

    if (isEditingProtectedField())
      {
	return Ganymede.createErrorDialog(ts.l("global.schema_editing_error"),
					  ts.l("global.system_field", this.toString()));
      }

    multiLine = b;

    return null;
  }

  /**
   * Getter for internal code.  We don't need a matching setter, since
   * we set the regexp through a regexp string via setRegexpPat().
   */

  public java.util.regex.Pattern getRegexp()
  {
    return this.regexp;
  }

  /**
   * <p>Returns the regexp pattern string constraining this string
   * field.</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not a string type.</p>
   *
   * @see arlut.csd.ganymede.rmi.BaseField
   */

  public String getRegexpPat()
  {
    if (!isString())
      {
	throw new IllegalStateException(ts.l("global.not_string", this.toString()));
      }

    return regexpPat;
  }

  /**
   * <p>Returns the text description of the regexp pattern string
   * constraining this string field.</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not a string type.</p>
   *
   * @see arlut.csd.ganymede.rmi.BaseField 
   */

  public String getRegexpDesc()
  {
    if (!isString())
      {
	throw new IllegalStateException(ts.l("global.not_string", this.toString()));
      }

    return regexpDesc;
  }

  /**
   * <p>Sets the regexp pattern string constraining this string field.</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not a string type.</p>
   *
   * @see arlut.csd.ganymede.rmi.BaseField 
   */

  public synchronized ReturnVal setRegexpPat(String s)
  {
    securityCheck();

    if (!isString())
      {
	throw new IllegalStateException(ts.l("global.not_string", this.toString()));
      }

    if (isEditingProtectedField())
      {
	return Ganymede.createErrorDialog(ts.l("global.schema_editing_error"),
					  ts.l("global.system_field", this.toString()));
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
	    regexp = java.util.regex.Pattern.compile(s);
	  }
	catch (java.util.regex.PatternSyntaxException ex)
	  {
	    // "Schema Editing Error"
	    // "Bad regular expression syntax: {0}\n{1}"
	    return Ganymede.createErrorDialog(ts.l("global.schema_editing_error"),
					      ts.l("setRegexpPat.bad_pattern", s, ex));
	  }

	regexpPat = s;

	if (isEditing() && isInUse())
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
   * <p>Sets the text descriptionf or the regexp pattern string
   * constraining this string field.</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not a string type.</p>
   *
   * @see arlut.csd.ganymede.rmi.BaseField 
   */

  public synchronized ReturnVal setRegexpDesc(String s)
  {
    securityCheck();

    if (!isString())
      {
	throw new IllegalStateException(ts.l("global.not_string", this.toString()));
      }

    if (isEditingProtectedField())
      {
	return Ganymede.createErrorDialog(ts.l("global.schema_editing_error"),
					  ts.l("global.system_field", this.toString()));
      }

    if (s == null || s.equals(""))
      {
	regexpDesc = null;
      }
    else
      {
	regexpDesc = s;
      }

    return null;
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
   * @see arlut.csd.ganymede.rmi.BaseField
   */

  public String getNameSpaceLabel()
  {
    // several pieces of code have already been written to expect a null
    // value for a field's namespace if none is defined, regardless of
    // field type.  No need for us to be overly fastidious here.

    if (namespace != null)
      {
	return namespace.getName();
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
   * @see arlut.csd.ganymede.rmi.BaseField 
   */

  public synchronized ReturnVal setNameSpace(String nameSpaceId)
  {
    securityCheck();

    if (!isString() && !isNumeric() && !isIP())
      {
	// "Can''t set a namespace constraint on this kind of field ({0}): {1}"
	throw new IllegalStateException(ts.l("setNameSpace.bad_type", this.getTypeDesc(), this.toString()));
      }

    // if we are not loading, don't allow a built-in universal field
    // to be messed with

    if (isEditingProtectedField())
      {
	return Ganymede.createErrorDialog(ts.l("global.schema_editing_error"),
					  ts.l("global.system_field", this.toString()));
      }

    // no change, no problem

    if ((nameSpaceId == null || nameSpaceId.equals("")) && namespace == null)
      {
	return null;
      }

    if (namespace != null && nameSpaceId != null && !nameSpaceId.equals(""))
      {
	DBNameSpace matchingSpace = base.getStore().getNameSpace(nameSpaceId);

	if (matchingSpace == namespace)
	  {
	    return null;
	  }
      }

    // see about doing the setting

    if (nameSpaceId == null || nameSpaceId.equals(""))
      {
	// wouldn't it be nice if java had decent support for declared data structures?

	if ((base.getTypeID() == SchemaConstants.UserBase && getID() == SchemaConstants.UserUserName) ||
	    (base.getTypeID() == SchemaConstants.PersonaBase && getID() == SchemaConstants.PersonaLabelField) ||
	    (base.getTypeID() == SchemaConstants.OwnerBase && getID() == SchemaConstants.OwnerNameField) ||
	    (base.getTypeID() == SchemaConstants.EventBase && getID() == SchemaConstants.EventToken) ||
	    (base.getTypeID() == SchemaConstants.RoleBase && getID() == SchemaConstants.RoleName) ||
	    (base.getTypeID() == SchemaConstants.TaskBase && getID() == SchemaConstants.TaskName) ||
	    (base.getTypeID() == SchemaConstants.SyncChannelBase && getID() == SchemaConstants.SyncChannelName))
	  {
	    return Ganymede.createErrorDialog(ts.l("global.schema_editing_error"),
					      ts.l("setNameSpace.need_namespace", this.toString()));
	  }

	if (isEditing())
	  {
	    if (!namespace.isSchemaEditInProgress())
	      {
		namespace.schemaEditCheckout();
	      }
	    
	    namespace.schemaEditUnregister(base.getTypeID(), getID());
	  }

	namespace = null;

	return null;
      }
    else
      {
	// this field is associated with a namespace.

	Enumeration values;
	DBNameSpace oldNamespace, tmpNS;
	
	/* -- */

	oldNamespace = namespace;
	
	values = base.getStore().nameSpaces.elements();
	namespace = null;

	while (values.hasMoreElements() && (namespace == null))
	  {
	    tmpNS = (DBNameSpace) values.nextElement();

	    if (tmpNS.getName().equalsIgnoreCase(nameSpaceId))
	      {
		namespace = tmpNS;
	      }
	  }

	if (isEditing())
	  {
	    if (oldNamespace != null && oldNamespace != namespace)
	      {
		if (!oldNamespace.isSchemaEditInProgress())
		  {
		    oldNamespace.schemaEditCheckout();
		  }
		
		oldNamespace.schemaEditUnregister(base.getTypeID(), getID());
	      }
	    
	    if (namespace != null && namespace != oldNamespace)
	      {
		if (!namespace.isSchemaEditInProgress())
		  {
		    namespace.schemaEditCheckout();
		  }
		
		// make sure that we can allocate all values already attached to this
		// field
		
		boolean success = true;
                DBField lastFieldTried = null;
		
		for (DBObject obj: base.getObjects())
		  {
		    lastFieldTried = (DBField) obj.getField(getID());
		    
		    if (lastFieldTried == null)
		      {
			continue;
		      }
		    
		    if (!this.isArray())
		      {
			success = namespace.schemaEditRegister(lastFieldTried.key(), lastFieldTried);
		      }
		    else
		      {
			for (int i = 0; success && i < lastFieldTried.size(); i++)
			  {
			    success = namespace.schemaEditRegister(lastFieldTried.key(i), lastFieldTried);
			  }
		      }

		    if (!success)
		      {
			String fieldDesc = lastFieldTried.toString();
			String content = lastFieldTried.getValueString();

			namespace.schemaEditUnregister(base.getTypeID(), getID());
			namespace = oldNamespace;

			// "Can''t set namespace constraint {0} on field
			// {1} without violating namespace uniqueness
			// constraint on previously registered
			// values.\nField {2} had a conflict.\Value(s) in
			// conflict:{3}"
			return Ganymede.createErrorDialog(ts.l("global.schema_editing_error"),
							  ts.l("setNameSpace.can_not_apply",
							       nameSpaceId,
							       this.toString(),
							       fieldDesc,
							       content));
		      }
		  }
	      }
	  }

	// if we didn't find it, complain.

	if (namespace == null)
	  {
	    // "Error, could not find a namespace called {0} to set on field {1}."
	    return Ganymede.createErrorDialog(ts.l("global.schema_editing_error"),
					      ts.l("setNameSpace.no_such_namespace", nameSpaceId, this.toString()));
	  }
      }

    return null;
  }

  // **
  // invid attribute methods
  // **

  /**
   * <p>Returns true if this is an invid field which is intended as an editInPlace
   * reference for the client's rendering.</p>
   *
   * @see arlut.csd.ganymede.rmi.BaseField
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
   * @see arlut.csd.ganymede.rmi.BaseField
   */

  public ReturnVal setEditInPlace(boolean b)
  {
    securityCheck();

    if (!isInvid())
      {
	throw new IllegalStateException(ts.l("global.not_invid", this.toString()));
      }

    // no change, no harm

    if (b == editInPlace)
      {
	return null;
      }

    if (isEditingProtectedSystemField())
      {
	// "Schema Editing Error"
	// "Can''t change the type of a system field: {0}."
	return Ganymede.createErrorDialog(ts.l("global.schema_editing_error"),
					  ts.l("global.system_field_change_attempt", this.toString()));
      }

    if (isInUse())
      {
	// "Schema Editing Error"
	// "Can''t change the editInPlace status type of an Invid field which is in use in the database: {0}."
	return Ganymede.createErrorDialog(ts.l("global.schema_editing_error"),
					  ts.l("setEditInPlace.in_use", this.toString()));
      }
    
    editInPlace = b;

    if (editInPlace)
      {
	// we don't target specific fields with embedded invid
	// fields.. the relationship with the container field in
	// edit-in-place objects is implicit with embedded invid
	// fields.

	targetField = -1;
      }

    return null;
  }

  /**
   * <p>Returns true if this is a target restricted invid field</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not an invid type.</p>
   *
   * @see arlut.csd.ganymede.rmi.BaseField
   */

  public boolean isTargetRestricted()
  {
    if (!isInvid())
      {
	throw new IllegalStateException(ts.l("global.not_invid", this.toString()));
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
   * @see arlut.csd.ganymede.rmi.BaseField
   */

  public short getTargetBase()
  {
    if (!isInvid())
      {
	throw new IllegalStateException(ts.l("global.not_invid", this.toString()));
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
   * @see arlut.csd.ganymede.rmi.BaseField 
   */

  public synchronized ReturnVal setTargetBase(short val)
  {
    securityCheck();

    if (!isInvid())
      {
	throw new IllegalStateException(ts.l("global.not_invid", this.toString()));
      }

    // no change, no harm

    if (val == allowedTarget)
      {
	return null;
      }

    if (isEditingProtectedSystemField())
      {
	return Ganymede.createErrorDialog(ts.l("global.schema_editing_error"),
					  ts.l("global.system_field_change_attempt", this.toString()));
      }

    // -1 and -2 are valid possible targets

    if (val == -1 || val == -2)
      {
	allowedTarget = val;
	return null;
      }

    // if we're field 0 (owner field) and we're being told to point to
    // the owner group base, we'll go ahead.

    if (field_code == 0 && val == 0)
      {
	allowedTarget = val;
	return null;
      }

    if (isEditing())
      {
	DBObjectBase b = (DBObjectBase) editor.getBase(val);

	if (b == null)
	  {
	    // "Schema Editing Error"
	    // "Can''t set the target base to base number {0,number,#}.  No such base is defined: {0}."
	    return Ganymede.createErrorDialog(ts.l("global.schema_editing_error"),
					      ts.l("setTargetBase.bad_target_num", Integer.valueOf(val)));
	  }
      }

    allowedTarget = val;

    if (isEditing() && isInUse())
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
   * @see arlut.csd.ganymede.rmi.BaseField 
   */

  public synchronized ReturnVal setTargetBase(String baseName)
  {
    securityCheck();

    if (!isInvid())
      {
	throw new IllegalStateException(ts.l("global.not_invid", this.toString()));
      }

    if (baseName == null)
      {
	if (allowedTarget == -1)
	  {
	    return null;		// no change, no harm
	  }

	if (isEditingProtectedSystemField())
	  {
	    return Ganymede.createErrorDialog(ts.l("global.schema_editing_error"),
					      ts.l("global.system_field_change_attempt", this.toString()));
	  }

	allowedTarget = -1;

	if (isEditing() && isInUse())
	  {
	    return warning2;
	  }
	else
	  {
	    return null;
	  }
      }

    if (isEditing())
      {
	DBObjectBase b = (DBObjectBase) editor.getBase(baseName);

	if (b == null)
	  {
	    // "Schema Editing Error"
	    // "Can''t set the target base for invid field {1} to base {0}.  No such base is defined."
	    return Ganymede.createErrorDialog(ts.l("global.schema_editing_error"),
					      ts.l("setTargetBase.bad_target", baseName, this.toString()));
	  }

	if (b.getTypeID() == allowedTarget)
	  {
	    return null;	// no change, no harm
	  }

	if (isEditingProtectedSystemField())
	  {
	    return Ganymede.createErrorDialog(ts.l("global.schema_editing_error"),
					      ts.l("global.system_field_change_attempt", this.toString()));
	  }

	allowedTarget = b.getTypeID();

	if (isInUse())
	  {
	    return warning2;
	  }
	else
	  {
	    return null;
	  }
      }
    else
      {
	DBObjectBase b = (DBObjectBase) base.getStore().getObjectBase(baseName);

	// we're loading here.. i don't expect the DBStore
	// initializeSchema() method to actually use base names, but
	// if it does for some reason and that base hasn't been
	// created yet, we're well within our rights to throw a
	// NullPointerException here. -- jon

	allowedTarget = b.getTypeID();

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
   * @see arlut.csd.ganymede.rmi.BaseField
   */

  public boolean isSymmetric()
  {
    if (!isInvid())
      {
	throw new IllegalStateException(ts.l("global.not_invid", this.toString()));
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
   * @see arlut.csd.ganymede.rmi.BaseField
   */

  public short getTargetField()
  {
    if (!isInvid())
      {
	throw new IllegalStateException(ts.l("global.not_invid", this.toString()));
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
   * @see arlut.csd.ganymede.rmi.BaseField
   */

  public synchronized ReturnVal setTargetField(short val)
  {
    securityCheck();

    if (!isInvid())
      {
	throw new IllegalStateException(ts.l("global.not_invid", this.toString()));
      }

    if (isEditInPlace() && val != -1)
      {
	// "Can''t set target field on an embedded invid field {0}."
	throw new IllegalStateException(ts.l("setTargetField.no_embedded_target_field", this.toString()));
      }

    if (val == targetField)
      {
	return null;		// no change, no harm
      }

    if (isEditingProtectedSystemField())
      {
	return Ganymede.createErrorDialog(ts.l("global.schema_editing_error"),
					  ts.l("global.system_field_change_attempt", this.toString()));
      }

    if (val < 0)
      {
	targetField = val;

	if (isEditing() && isInUse())
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
	// "Can''t set target field on a non-symmetric invid field {0} to {1,number,#}"
	return Ganymede.createErrorDialog(ts.l("global.schema_editing_error"),
					  ts.l("setTargetField.asymmetry_num", this.toString(), Integer.valueOf(val)));
      }

    if (isEditing())
      {
	DBObjectBase b = (DBObjectBase) editor.getBase(allowedTarget);

	// we're looking up the object that we have pre-selected.. we
	// should always set a target object before trying to set a
	// field

	if (b == null)
	  {
	    // "Can''t find object type {0,number,#} in order to set target field for {2} to {1,number,#}"
	    return Ganymede.createErrorDialog(ts.l("global.schema_editing_error"),
					      ts.l("setTargetField.bad_base_num", Integer.valueOf(allowedTarget), Integer.valueOf(val),
						   this.toString()));
	  }
	
	DBObjectBaseField bF = b.getFieldDef(val);

	if (bF == null)
	  {
	    // "Can''t find target field numbered {0,number,#} in order to set target field for {1}."
	    return Ganymede.createErrorDialog(ts.l("global.schema_editing_error"),
					      ts.l("setTargetField.bad_target_field_num", Integer.valueOf(val),
						   this.toString()));
	  }
      }

    // if we're loading rather than editing, we'll go ahead and set it
    // regardless of whether the target base and field have been
    // created yet

    targetField = val;

    if (isEditing() && isInUse())
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
   * @see arlut.csd.ganymede.rmi.BaseField
   */

  public synchronized ReturnVal setTargetField(String fieldName)
  {
    Base b;
    BaseField bF;

    /* -- */

    securityCheck();

    if (!isInvid())
      {
	throw new IllegalStateException(ts.l("global.not_invid", this.toString()));
      }

    if (fieldName == null || fieldName.equals(""))
      {
	if (targetField == -1)
	  {
	    return null;		// no change, no harm
	  }

	if (isEditingProtectedSystemField())
	  {
	    return Ganymede.createErrorDialog(ts.l("global.schema_editing_error"),
					      ts.l("global.system_field_change_attempt", this.toString()));
	  }

	targetField = -1;

	if (isEditing() && isInUse())
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
	// "Can''t set target field on a non-symmetric invid field {0} to {1}"
	return Ganymede.createErrorDialog(ts.l("global.schema_editing_error"),
					  ts.l("setTargetField.asymmetry", this.toString(), fieldName));
      }

    if (isEditing())
      {
	b = editor.getBase(allowedTarget);
      }
    else
      {
	b = base.getStore().getObjectBase(allowedTarget);
      }

    try
      {
	if (b == null)
	  {
	    // "Can''t find object type {0,number,#} in order to set target field for {2} to {1}"
	    return Ganymede.createErrorDialog(ts.l("global.schema_editing_error"),
					      ts.l("setTargetField.bad_base", Integer.valueOf(allowedTarget),
						   fieldName, this.toString()));
	  }
	
	bF = b.getField(fieldName);

	if (bF == null)
	  {
	    // "Can''t find target field named {0} in order to set target field for {1}."
	    return Ganymede.createErrorDialog(ts.l("global.schema_editing_error"),
					      ts.l("setTargetField.bad_target_field", fieldName, this.toString()));
	  }

	if (bF.getID() == targetField)
	  {
	    return null;	// no change, no harm, no warning needed
	  }

	// remember, system fields are initialized outside of the
	// context of the editing system, there should never be a
	// reason to call setTargetField() on a system field when
	// editing

	if (isEditingProtectedSystemField())
	  {
	    return Ganymede.createErrorDialog(ts.l("global.schema_editing_error"),
					      ts.l("global.system_field_change_attempt", this.toString()));
	  }

	targetField = bF.getID();
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("caught remote: " + ex);
      }

    if (isEditing() && isInUse())
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
   * @see arlut.csd.ganymede.rmi.BaseField
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
   * @see arlut.csd.ganymede.rmi.BaseField
   */

  public ReturnVal setCrypted(boolean b)
  {    
    securityCheck();

    if (!isPassword())
      {
	throw new IllegalStateException(ts.l("global.not_password", this.toString()));
      }

    crypted = b;

    return null;
  }

  /** 
   * <p>This method returns true if this is a password field that
   * stores passwords in OpenBSD/FreeBSD/PAM md5crypt() format, and
   * can thus accept pre-crypted passwords.</p>
   *
   * @see arlut.csd.ganymede.rmi.BaseField 
   */

  public boolean isMD5Crypted()
  {
    return md5crypted;
  }

  /**
   * <p>This method is used to specify that this password field should
   * store passwords in OpenBSD/FreeBSD/PAM md5crypt() format.  If
   * passwords are stored in md5crypt() format, they will not be kept
   * in plaintext on disk, unless isPlainText() returns true.</p>
   *
   * <p>setMD5Crypted() is not mutually exclusive with any other
   * encryption or plaintext options.</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not a password type.</p>
   *
   * @see arlut.csd.ganymede.rmi.BaseField 
   */

  public ReturnVal setMD5Crypted(boolean b)
  {    
    securityCheck();

    if (!isPassword())
      {
	throw new IllegalStateException(ts.l("global.not_password", this.toString()));
      }

    md5crypted = b;

    return null;
  }

  /** 
   * <p>This method returns true if this is a password field that
   * stores passwords in Apache md5crypt() format, and
   * can thus accept pre-crypted passwords.</p>
   *
   * @see arlut.csd.ganymede.rmi.BaseField 
   */

  public boolean isApacheMD5Crypted()
  {
    return apachemd5crypted;
  }

  /**
   * <p>This method is used to specify that this password field should
   * store passwords in Apache md5crypt() format.  If
   * passwords are stored in Apache md5crypt() format, they will not be kept
   * in plaintext on disk, unless isPlainText() returns true.</p>
   *
   * <p>setApacheMD5Crypted() is not mutually exclusive with any other
   * encryption or plaintext options.</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not a password type.</p>
   *
   * @see arlut.csd.ganymede.rmi.BaseField 
   */

  public ReturnVal setApacheMD5Crypted(boolean b)
  {
    securityCheck();

    if (!isPassword())
      {
	throw new IllegalStateException(ts.l("global.not_password", this.toString()));
      }

    apachemd5crypted = b;

    return null;
  }

  /** 
   * <p>This method returns true if this is a password field that will
   * store passwords in the two hashing formats used by Samba/Windows,
   * the older 14-char LANMAN hash, and the newer md5/Unicode hash
   * used by Windows NT.  If passwords are stored in the windows
   * hashing formats, they will not be kept in plaintext on disk,
   * unless isPlainText() returns true.</p>
   *
   * @see arlut.csd.ganymede.rmi.BaseField 
   */

  public boolean isWinHashed()
  {
    return winHashed;
  }

  /**
   * <p>This method is used to specify that this password field should
   * store passwords in the Samba/Windows hashing formats.</p>
   *
   * <p>setWinHashed() is not mutually exclusive with any other
   * encryption or plaintext options.</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not a password type.</p>
   *
   * @see arlut.csd.ganymede.rmi.BaseField 
   */

  public ReturnVal setWinHashed(boolean b)
  {
    securityCheck();

    if (!isPassword())
      {
	throw new IllegalStateException(ts.l("global.not_password", this.toString()));
      }

    winHashed = b;

    return null;
  }

  /** 
   * <p>This method returns true if this is a password field that will
   * store passwords in the Netscape SSHA (salted SHA) hash format,
   * used in LDAP. If passwords are stored in the SSHA hashing format,
   * they will not be kept in plaintext on disk, unless isPlainText()
   * returns true.</p>
   *
   * @see arlut.csd.ganymede.rmi.BaseField 
   */

  public boolean isSSHAHashed()
  {
    return sshaHashed;
  }

  /**
   * <p>This method is used to specify that this password field should
   * store passwords in the Netscape SSHA (salted SHA) LDAP format.</p>
   *
   * <p>setSSHAHashed() is not mutually exclusive with any other
   * encryption or plaintext options.</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not a password type.</p>
   *
   * @see arlut.csd.ganymede.rmi.BaseField 
   */

  public ReturnVal setSSHAHashed(boolean b)
  {    
    securityCheck();

    if (!isPassword())
      {
	throw new IllegalStateException(ts.l("global.not_password", this.toString()));
      }

    sshaHashed = b;

    return null;
  }

  /** 
   * This method returns true if this is a password field that will
   * store passwords in the SHA Unix Crypt format, specified by Ulrich
   * Drepper at http://people.redhat.com/drepper/sha-crypt.html.
   *
   * If passwords are stored in the SHA Unix Crypt format, they will
   * not be kept in plaintext on disk, unless isPlainText() returns
   * true.
   *
   * @see arlut.csd.ganymede.rmi.BaseField 
   */

  public boolean isShaUnixCrypted()
  {
    return shaUnixCrypted;
  }

  /**
   * This method is used to specify that this password field should
   * store passwords in the SHA Unix Crypt format, specified by Ulrich
   * Drepper at http://people.redhat.com/drepper/sha-crypt.html.
   *
   * setShaUnixCrypted() is not mutually exclusive with any other
   * encryption or plaintext options.
   *
   * This method will throw an IllegalArgumentException if this field
   * definition is not a password type.
   *
   * @see arlut.csd.ganymede.rmi.BaseField 
   */

  public ReturnVal setShaUnixCrypted(boolean b)
  {    
    securityCheck();

    if (!isPassword())
      {
	throw new IllegalStateException(ts.l("global.not_password", this.toString()));
      }

    shaUnixCrypted = b;

    return null;
  }

  /** 
   * This method returns true if this is a shaUnixCrypted password
   * field that will store passwords using the SHA512 variant of the
   * SHA Unix Crypt format, specified by Ulrich Drepper at
   * http://people.redhat.com/drepper/sha-crypt.html.
   *
   * @see arlut.csd.ganymede.rmi.BaseField 
   */

  public boolean isShaUnixCrypted512()
  {
    return useShaUnixCrypted512;
  }

  /**
   * This method is used to specify that this password field should
   * store passwords in the SHA512 variant of the SHA Unix Crypt
   * format, specified by Ulrich Drepper at
   * http://people.redhat.com/drepper/sha-crypt.html.
   *
   * This method will throw an IllegalArgumentException if this field
   * definition is not a ShaUnixCrypt using password type.
   *
   * @see arlut.csd.ganymede.rmi.BaseField 
   */

  public ReturnVal setShaUnixCrypted512(boolean b)
  {
    securityCheck();

    if (!isPassword())
      {
	throw new IllegalStateException(ts.l("global.not_password", this.toString()));
      }

    if (!shaUnixCrypted)
      {
	throw new IllegalStateException(ts.l("global.not_shacrypt", this.toString()));
      }

    useShaUnixCrypted512 = b;

    return null;
  }

  /** 
   * This method returns the complexity factor (in number of rounds)
   * to be applied to password hash text generated in this password
   * field definition by the SHA Unix Crypt format, specified by
   * Ulrich Drepper at
   * http://people.redhat.com/drepper/sha-crypt.html.
   *
   * @see arlut.csd.ganymede.rmi.BaseField 
   */

  public int getShaUnixCryptRounds()
  {
    return shaUnixCryptRounds;
  }

  /**
   * This method is used to specify the complexity factor (in number
   * of rounds) to be applied to password hash text generated in this
   * password field definition by the SHA Unix Crypt format, specified
   * by Ulrich Drepper at
   * http://people.redhat.com/drepper/sha-crypt.html.
   *
   * This method will throw an IllegalArgumentException if this field
   * definition is not a ShaUnixCrypt using password type.
   *
   * @see arlut.csd.ganymede.rmi.BaseField 
   */

  public ReturnVal setShaUnixCryptRounds(int n)
  {
    securityCheck();

    if (!isPassword())
      {
	throw new IllegalStateException(ts.l("global.not_password", this.toString()));
      }

    if (!shaUnixCrypted)
      {
	throw new IllegalStateException(ts.l("global.not_shacrypt", this.toString()));
      }

    if (n < 1000)
      {
	n = 1000;
      }
    else if (n > 999999999)
      {
	n = 999999999;
      }

    shaUnixCryptRounds = n;

    return null;
  }

  /**
   * <p>This method returns true if this is a password field that
   * will keep a copy of the password in plaintext in the Ganymede
   * server's on-disk database.</p>
   *
   * @see arlut.csd.ganymede.rmi.BaseField
   */

  public boolean isPlainText()
  {
    return storePlaintext;
  }

  /**
   * <p>This method is used to specify that this password field
   * should keep a copy of the password in plaintext on disk,
   * even if other hash methods are in use which could be
   * used for Ganymede login authentication.  If no hash methods
   * are enabled for this password field, plaintext will be stored
   * on disk even if isPlainText() returns false for this field definition.</p>
   *
   * <p>This method will throw an IllegalArgumentException if
   * this field definition is not a password type.</p>
   *
   * @see arlut.csd.ganymede.rmi.BaseField
   */

  public ReturnVal setPlainText(boolean b)
  {
    securityCheck();

    if (!isPassword())
      {
	throw new IllegalStateException(ts.l("global.not_password", this.toString()));
      }

    storePlaintext = b;

    return null;
  }

  // general convenience methods

  /**
   * Indicates whether methods on this DBObjectBaseField are being
   * called in the context of the server being loaded.
   *
   * If this method returns false, many of the data setter methods in
   * this class will only be permitted if this DBObjectBaseField is
   * connected with a non-null DBSchemaEdit object.
   *
   * In general, isLoading() and isEditing() should never be true at
   * the same time.
   */

  private boolean isLoading()
  {
    return base.getStore().loading;
  }

  /**
   * Indicates whether methods on this DBObjectBaseField are being
   * called in the context of the schema being edited.
   *
   * If this method returns false, many of the data setter methods in
   * this class will only be permitted if the DBStore is in loading
   * mode.
   *
   * In general, isEditing() and isLoading() should never be true at
   * the same time.
   */

  private boolean isEditing()
  {
    return this.editor != null;
  }

  /**
   * Returns true if we are attempting to edit a field that is
   * protected and has previously been consolidated into the database.
   */

  private boolean isEditingProtectedField()
  {
    switch (base.getEditingMode())
      {
      case LOCKED:
	return true;

      case INITIALIZING:
      case LOADING:
      case CREATING:
	return false;

      case EDITING:
	return !isEditable();

      default:
	throw new RuntimeException("Unrecognized editing mode");
      }
  }

  /**
   * Returns true if we are attempting to edit a field that is
   * protected and has previously been consolidated into the database.
   */

  private boolean isEditingProtectedSystemField()
  {
    switch (base.getEditingMode())
      {
      case LOCKED:
	return true;

      case INITIALIZING:
      case LOADING:
      case CREATING:
	return false;

      case EDITING:
	return isSystemField();

      default:
	throw new RuntimeException("Unrecognized editing mode");
      }
  }

  /**
   * Checks to see if we are in either a loading or editing context.
   *
   * If we are in neither, we throw an exception up, so that a
   * modified client can't screw with our schema without appropriate
   * authorization.
   *
   * This is necessary because we make RMI references to
   * DBObjectBaseField objects available to all Ganymede RMI clients,
   * most of which have not been granted permission to modify our
   * schema.
   */

  private void securityCheck()
  {
    if (!isLoading() && !isEditing())
      {
	// "Not in a schema editing context."
	throw new IllegalStateException(ts.l("global.not_editing_schema"));
      }
  }

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

    // basically we want to identify all fields in the Ganymede
    // built-in object types that we have to be especially paranoid
    // about.

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

      case SchemaConstants.SyncChannelBase:

	switch (getID())
	  {
	  case SchemaConstants.SyncChannelName:
	  case SchemaConstants.SyncChannelDirectory:
	  case SchemaConstants.SyncChannelServicer:
	  case SchemaConstants.SyncChannelFields:
	  case SchemaConstants.SyncChannelPlaintextOK:
	    return true;
	  }
      }

    return false;
  }

  /**
   * <p>This method is intended to produce a human readable
   * representation of this field definition's simple type attribute.
   * This method should not be used programatically to determine this
   * field's type information.</p>
   *
   * <p>This method is only for human information, and the precise
   * results returned are subject to change at any time.</p>
   *
   * @see arlut.csd.ganymede.rmi.BaseField 
   */

  public String getTypeDesc()
  {
    String result;

    switch (field_type)
      {
      case BOOLEAN:
	result = ts.l("getTypeDesc.boolean"); // "boolean"
	break;

      case NUMERIC:
	result = ts.l("getTypeDesc.numeric"); // "numeric"
	break;

      case FLOAT:
	result = ts.l("getTypeDesc.float"); // "float"
	break;

      case FIELDOPTIONS:
	result = ts.l("getTypeDesc.field_option"); // "field options"
	break;

      case DATE:
	result = ts.l("getTypeDesc.date"); // "date"
	break;

      case STRING:
	result = ts.l("getTypeDesc.string"); // "string"
	break;

      case INVID:
	result = ts.l("getTypeDesc.invid"); // "invid"
	break;

      case PERMISSIONMATRIX:
	result = ts.l("getTypeDesc.permission_matrix"); // "permission matrix"
	break;

      case PASSWORD:
	result = ts.l("getTypeDesc.password"); // "password"
	break;

      case IP:
	result = ts.l("getTypeDesc.ip_field"); // "i.p. field"
	break;

      default:
	// "<<bad type code: " + field_type + " >>"
	result = ts.l("getTypeDesc.bad_code", Integer.valueOf(field_type));
      }

    if (array)
      {
	return result + "[]";
      }
    else
      {
	return result;
      }
  }

  // misc object and interface methods

  public String toString()
  {
    return base.getName() + ":" + field_name;
  }

  /**
   * <p>Returns the type id for this field definition as
   * a Short, suitable for use in a hash.</p>
   */

  public Short getKey()
  {
    return Short.valueOf(field_code);
  }

  /**
   * This method implements the Comparable interface.
   *
   * We are comparable in terms of the field id number for this field.
   *
   * The o parameter can be a Short, a short (using Java 5
   * autoboxing), or another DBObjectBaseField.
   */

  public int compareTo(Object o)
  {
    if (o instanceof Number)
      {
	return field_code - ((Number) o).shortValue();
      }
    else
      {
	DBObjectBaseField otherField = (DBObjectBaseField) o;

	return field_code - otherField.field_code;
      }
  }

  // static warning methods

  private static ReturnVal genWarning1()
  {
    ReturnVal retVal = new ReturnVal(true);

    // "Schema Editor"
    //
    // "The requested change in this field's allowed options has been made and will be put into effect if you commit your schema change.
    //
    // This schema change will only affect new values entered into this field in the database.  Pre-existing fields of this kind in
    // the database may or may not satisfy your new constraint."
    //

    retVal.setDialog(new JDialogBuff(ts.l("genWarning1.title"),
				     ts.l("genWarning1.text"),
				     ts.l("genWarning1.ok"), // "OK"
				     null,
				     "ok.gif"));

    return retVal;
  }

  private static ReturnVal genWarning2()
  {
    ReturnVal retVal = new ReturnVal(true);

    // "Schema Editor"
    //
    // "The requested change in this field's allowed options has been made and will be put into effect if you commit your schema change.
    //
    // Because this schema change is being made while there are fields of this type active in the database, there may be a chance
    // that this change will affect database consistency."
    //

    retVal.setDialog(new JDialogBuff(ts.l("genWarning2.title"),
				     ts.l("genWarning2.text"),
				     ts.l("genWarning2.ok"),
				     null,
				     "ok.gif"));

    return retVal;
  }
}
