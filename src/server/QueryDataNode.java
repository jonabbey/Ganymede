/*

   QueryDataNode.java

   Created: 10 July 1997
   Release: $Name:  $
   Version: $Revision: 1.16 $
   Last Mod Date: $Date: 1999/03/23 06:22:15 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999  The University of Texas at Austin.

   Contact information

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

/*------------------------------------------------------------------------------
                                                                           class
                                                                   QueryDataNode

------------------------------------------------------------------------------*/

public class QueryDataNode extends QueryNode {

  static final long serialVersionUID = 1435603665496067800L;

  static public final byte FIRST = 1;

  static public final byte EQUALS = 1;
  static public final byte LESS = 2;
  static public final byte LESSEQ = 3;
  static public final byte GREAT = 4;
  static public final byte GREATEQ = 5;
  static public final byte NOCASEEQ = 6; // case insensitive string equals
  static public final byte STARTSWITH = 7;
  static public final byte ENDSWITH = 8;
  static public final byte DEFINED = 9;
  static public final byte MATCHES = 10;
  static public final byte NOCASEMATCHES = 11;
  static public final byte LAST = 11;

  static public final byte FIRSTVECOP = 0;
  
  static public final byte NONE = 0;
  static public final byte CONTAINS = 1;
  static public final byte LENGTHEQ = 4;
  static public final byte LENGTHGR = 5;
  static public final byte LENGTHLE = 6;

  static public final byte LASTVECOP = 6;

  static public final short INVIDVAL = -2;

  /* - */

  String fieldname;
  short fieldId;
  byte comparator;
  byte arrayOp;
  Object value;

  transient Object regularExpression = null;

  /* -- */

  /**
   * Field comparison node constructor.
   *
   * This constructor creates a query node that will be matched
   * against a field in an object.
   *
   */

  public QueryDataNode(String fieldname, byte comparator, byte vecOp, Object value)
  {
    this.fieldname = fieldname;
    this.fieldId = -1;

    if ((comparator < FIRST || comparator > LAST) &&
	(vecOp == NONE))
      {
	throw new IllegalArgumentException("bad comparator value: " + comparator);
      }

    this.comparator = comparator;
    this.value = value;

    if (vecOp < FIRSTVECOP || vecOp > LASTVECOP)
      {
	throw new IllegalArgumentException("bad vector operator value: " + vecOp);
      }

    this.arrayOp = vecOp;
  }

  /**
   * Field comparison node constructor.
   *
   * This constructor creates a query node that will be matched
   * against a field in an object.
   *
   */

  public QueryDataNode(String fieldname, byte comparator, Object value)
  {
    this.fieldname = fieldname;
    this.fieldId = -1;

    if (comparator < FIRST || comparator > LAST)
      {
	throw new IllegalArgumentException("bad comparator value: " + comparator);
      }

    this.comparator = comparator;
    this.value = value;

    this.arrayOp = NONE;
  }

  /**
   * Field comparison node constructor.
   *
   * This constructor creates a query node that will be matched
   * against a field in an object.
   *
   * If fieldID == -1, the labels of objects in the database will be
   * taken as the field for comparison's sake.
   *
   * If fieldID == -2, the Invid of objects in the database will be
   * taken as the field for comparison's sake.
   * 
   */

  public QueryDataNode(short fieldId, byte comparator, byte vecOp, Object value)
  {
    this.fieldname = null;
    this.fieldId = fieldId;

    if ((comparator < FIRST || comparator > LAST) &&
	(vecOp == NONE))
      {
	throw new IllegalArgumentException("bad comparator value: " + comparator);
      }

    this.comparator = comparator;
    this.value = value;

    if (vecOp < FIRSTVECOP || vecOp > LASTVECOP)
      {
	throw new IllegalArgumentException("bad vector operator value: " + vecOp);
      }

    this.arrayOp = vecOp;
  }

  /**
   * Field comparison node constructor.
   *
   * This constructor creates a query node that will be matched
   * against a field in an object.
   *
   * If fieldID == -1, the labels of objects in the database will be
   * taken as the field for comparison's sake.
   *
   * If fieldID == -2, the Invid of objects in the database will be
   * taken as the field for comparison's sake.
   * 
   */

  public QueryDataNode(short fieldId, byte comparator, Object value)
  {
    this.fieldname = null;
    this.fieldId = fieldId;

    if (comparator < FIRST || comparator > LAST)
      {
	throw new IllegalArgumentException("bad comparator value: " + comparator);
      }

    this.comparator = comparator;
    this.value = value;

    this.arrayOp = NONE;
  }

  /**
   * Default field comparison node constructor.
   *
   * This constructor creates a query node that will be matched
   * against an object's primary label field.
   *
   */

  public QueryDataNode(byte comparator, Object value)
  {
    this.fieldname = null;
    this.fieldId = -1;

    if (comparator < FIRST || comparator > LAST)
      {
	throw new IllegalArgumentException("bad comparator value: " + comparator);
      }

    this.comparator = comparator;
    this.value = value;

    this.arrayOp = NONE;
  }

  /**
   *
   * Diagnostic aid.
   *
   */

  public String toString()
  {
    StringBuffer result = new StringBuffer();

    /* -- */

    if (fieldname != null)
      {
	result.append(fieldname);
      }
    else
      {
	result.append("<");
	result.append(Short.toString(fieldId));
	result.append(">");
      }

    result.append(" ");

    if (arrayOp == NONE)
      {
	switch (comparator)
	  {
	  case EQUALS:
	    result.append("EQUALS");
	    break;

	  case LESS:
	    result.append("LESS");
	    break;

	  case LESSEQ:
	    result.append("LESSEQ");
	    break;

	  case GREAT:
	    result.append("GREAT");
	    break;

	  case GREATEQ:
	    result.append("GREATEQ");
	    break;

	  case NOCASEEQ:
	    result.append("NOCASEEQ");
	    break;

	  case STARTSWITH:
	    result.append("STARTSWITH");
	    break;

	  case ENDSWITH:
	    result.append("ENDSWITH");
	    break;

	  case DEFINED:
	    result.append("DEFINED");
	    break;

	  case MATCHES:
	    result.append("MATCHES");
	    break;

	  case NOCASEMATCHES:
	    result.append("NOCASEMATCHES");
	    break;
	  }
      }
    else
      {
	switch (arrayOp)
	  {
	  case CONTAINS:
	    result.append("CONTAINS");

	    switch (comparator)
	      {
	      case EQUALS:
		result.append("/EQUALS");
		break;

	      default:
		result.append("/?");
	      }

	    break;

	  case LENGTHEQ:
	    result.append("LENGTHEQ");
	    break;

	  case LENGTHGR:
	    result.append("LENGTHGR");
	    break;

	  case LENGTHLE:
	    result.append("LENGTHLE");
	    break;
	  }
      }

    result.append(" ");

    result.append(value.toString());

    return result.toString();
  }
}
