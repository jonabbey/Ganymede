/*
   GASH 2

   DBObjectBaseField.java

   The GANYMEDE object storage system.

   Created: 27 August 1996
   Version: $Revision: 1.4 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package csd.DBStore;

import java.io.*;
import java.util.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                               DBObjectBaseField

------------------------------------------------------------------------------*/

/**
 * An entry in the DBObjectBase dictionary.  This class defines the type of
 * an object field, along with any namespace information pertaining to the field.
 */

public class DBObjectBaseField {

  DBObjectBase base;  
  String field_name;
  short field_code;
  short field_type;
  DBNameSpace namespace;
  short limit;

  /* -- */

  DBObjectBaseField(DBObjectBase base)
  {
    this.base = base;
    field_name = null;
    field_code = 0;
    field_type = 0;
    limit = -1;
    namespace = null;
  }

  DBObjectBaseField(DataInput in, DBObjectBase base) throws IOException
  {
    this.base = base;
    receive(in);
  }

  void emit(DataOutput out) throws IOException
  {
    out.writeUTF(field_name);
    out.writeShort(field_code);
    out.writeShort(field_type);
    if (namespace == null)
      {
	out.writeUTF("");
      }
    else
      {
	out.writeUTF(namespace.name);
      }

    out.writeShort(limit);
  }

  void receive(DataInput in) throws IOException
  {
    String nameSpaceId;

    /* -- */

    field_name = in.readUTF();
    field_code = in.readShort();
    field_type = in.readShort();

    // read in our namespace identifier.  We use this to associate this field
    // with a (potentially preexisting) namespace.

    nameSpaceId = in.readUTF();

    if (nameSpaceId.equals(""))
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

	    if (tmpNS.name.equalsIgnoreCase(nameSpaceId))
	      {
		namespace = tmpNS;
	      }
	  }

	// if we didn't find it we've got a new namespace

	if (namespace == null)
	  {
	    namespace = new DBNameSpace(nameSpaceId);
	    base.store.nameSpaces.addElement(namespace);
	  }
      }

    // read in the limit constraint (for an invid, this is the type of
    // object the invid has to point to.  for a string or string
    // array, it is a string length limit).  Will be -1 if there
    // is no constraint.

    limit = in.readShort();
  }

  /**
   *
   * Returns the name of this field
   *
   */

  public String name()
  {
    return field_name;
  }

  /**
   *
   * <p>Returns the field type</p>
   *
   * <p>Where type is one of the following
   * constants defined in the DBStore class:</p>
   *
   *   static final short BOOLEAN = 0;
   *   static final short NUMERIC = 1;
   *   static final short DATE = 2;
   *   static final short STRING = 3;
   *   static final short INVID = 4;
   *   static final short BOOLEANARRAY = 5;
   *   static final short NUMERICARRAY = 6;
   *   static final short DATEARRAY = 7;
   *   static final short STRINGARRAY = 8;
   *   static final short INVIDARRAY = 9;
   *
   * @see csd.DBStore.DBStore
   */

  public short type()
  {
    return field_type;
  }

  /**
   *
   * <p>Returns id code for this field.  Each field in a DBObject
   * has a unique code which identifies the field.  This code represents
   * the field in the on-disk data store, and is used by DBEditObject
   * to choose what field to change in the setField method.</p>
   *
   */

  public short id()
  {
    return field_code;
  }

  /**
   *
   * <p>Returns a short limit constraint.  This limit only applies to two
   * kinds of fields, invid's and string's.  For invid's, the limit restricts
   * what sort of object this field can point to.  For strings, the limit is
   * a string length limit.</p>
   *
   */

  public short limit()
  {
    return limit;
  }

  public void print(PrintStream out)
  {
    out.print(field_name + "(" + field_code + "):");
    switch (field_type)
      {
      case DBStore.BOOLEAN:
	out.print("boolean");
	break;
      case DBStore.NUMERIC:
	out.print("numeric");
	break;
      case DBStore.DATE:
	out.print("date");
	break;
      case DBStore.STRING:
	out.print("string");
	if (limit != -1)
	  {
	    out.print(", " + limit + " chars max");
	  }
	break;
      case DBStore.INVID:
	out.print("invid");
	if (limit != -1)
	  {
	    out.print(", type " + limit + " only");
	  }
	break;
      case DBStore.BOOLEANARRAY:
	out.print("booleanArray");
	break;
      case DBStore.NUMERICARRAY:
	out.print("numericArray");
	break;
      case DBStore.DATEARRAY:
	out.print("dateArray");
	break;
      case DBStore.STRINGARRAY:
	out.print("stringArray");
	if (limit != -1)
	  {
	    out.print(", " + limit + " chars max");
	  }
	break;
      case DBStore.INVIDARRAY:
	out.print("invidArray");
	if (limit != -1)
	  {
	    out.print(", type " + limit + " only");
	  }
	break;
      }

    if (namespace != null)
      {
	out.print(" <" + namespace.name + ">");
      }

    out.println();
  }
}
