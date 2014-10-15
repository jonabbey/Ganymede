/*

   FieldTemplate.java

   This class is a serializable object to return all the static
   information defining a field.

   Created: 5 November 1997

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2013
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

package arlut.csd.ganymede.common;

import java.rmi.RemoteException;

import arlut.csd.ganymede.rmi.Base;
import arlut.csd.ganymede.rmi.BaseField;

/*------------------------------------------------------------------------------
                                                                           class
                                                                   FieldTemplate

------------------------------------------------------------------------------*/

/**
 * <p>This class is a serializable object used to return all the
 * static information the client's {@link
 * arlut.csd.ganymede.client.containerPanel containerPanel} needs to
 * render a specific kind of field.  A FieldTemplate is basically a
 * summary of the information that can be retrieved through the {@link
 * arlut.csd.ganymede.rmi.BaseField} remote interface.</p>
 *
 * <p>The {@link arlut.csd.ganymede.common.FieldInfo FieldInfo} object
 * is used to return the value information associated with an actual
 * instance of a field.</p>
 */

public class FieldTemplate implements java.io.Serializable, FieldType {

  static final long serialVersionUID = -4052464245469186312L;

  // ---

  // common field data

  String name;
  String comment;
  short type;
  short fieldID;
  boolean vector;
  String tabName;

  // perm_editor needs us to store this for it

  short baseID;

  // built in?

  boolean builtIn;

  // array attributes

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
  boolean multiLine = false;
  String regexpPat = null;

  // invid attributes

  boolean editInPlace = false;
  short allowedTarget = -1;

  /* -- */

  public FieldTemplate(BaseField fieldDef)
  {
    try
      {
        name = fieldDef.getName();
        comment = fieldDef.getComment();
        type = fieldDef.getType();
        fieldID = fieldDef.getID();
        tabName = fieldDef.getTabName();

        Base base = fieldDef.getBase();
        baseID = base.getTypeID();

        vector = fieldDef.isArray();

        if (vector)
          {
            limit = fieldDef.getMaxArraySize();
          }

        builtIn = fieldDef.isBuiltIn();

        switch (type)
          {
          case BOOLEAN:
            labeled = fieldDef.isLabeled();

            if (labeled)
              {
                trueLabel = fieldDef.getTrueLabel();
                falseLabel = fieldDef.getFalseLabel();
              }
            break;

          case STRING:
            minLength = fieldDef.getMinLength();
            maxLength = fieldDef.getMaxLength();
            okChars = fieldDef.getOKChars();
            badChars = fieldDef.getBadChars();
            multiLine = fieldDef.isMultiLine();
            regexpPat = fieldDef.getRegexpPat();
            break;

          case INVID:
            editInPlace = fieldDef.isEditInPlace();
            allowedTarget = fieldDef.getTargetBase();
            break;
          }
      }
    catch (RemoteException ex)
      {
        throw new RuntimeException(ex.getMessage());
      }
  }

  /**
   *
   * Returns a type identifier for this field.
   *
   * @see arlut.csd.ganymede.common.FieldType
   *
   */

  public short getType()
  {
    return type;
  }

  /**
   *
   * Returns the field number for this field.
   *
   */

  public short getID()
  {
    return fieldID;
  }

  /**
   *
   * Returns the object type number for the object containing this
   * field.
   *
   */

  public short getBaseID()
  {
    return baseID;
  }

  /**
   *
   * Returns the name of this field.
   *
   */

  public String getName()
  {
    return name;
  }

  /**
   *
   * Returns a description of this field suitable for a tooltip.
   *
   */

  public String getComment()
  {
    return comment;
  }

  /**
   *
   * Returns the name of the tab that is to contain this field on the client.
   *
   */

  public String getTabName()
  {
    return tabName;
  }

  /**
   *
   * Returns true if this field is a vector.
   *
   */

  public boolean isArray()
  {
    return vector;
  }

  // type identification convenience methods

  /**
   *
   * Returns true if this field is of boolean type
   *
   * @see arlut.csd.ganymede.rmi.BaseField
   */

  public boolean isBoolean()
  {
    return (type == BOOLEAN);
  }

  /**
   *
   * Returns true if this field is of numeric type
   *
   * @see arlut.csd.ganymede.rmi.BaseField
   */

  public boolean isNumeric()
  {
    return (type == NUMERIC);
  }

  /**
   *
   * Returns true if this field is of date type
   *
   * @see arlut.csd.ganymede.rmi.BaseField
   */

  public boolean isDate()
  {
    return (type == DATE);
  }

  /**
   *
   * Returns true if this field is of string type
   *
   * @see arlut.csd.ganymede.rmi.BaseField
   */

  public boolean isString()
  {
    return (type == STRING);
  }

  /**
   *
   * Returns true if this field is of invid type
   *
   * @see arlut.csd.ganymede.rmi.BaseField
   */

  public boolean isInvid()
  {
    return (type == INVID);
  }

  /**
   *
   * Returns true if this field is of permission matrix type
   *
   * @see arlut.csd.ganymede.rmi.BaseField
   */

  public boolean isPermMatrix()
  {
    return (type == PERMISSIONMATRIX);
  }

  /**
   *
   * Returns true if this field is of password type
   *
   * @see arlut.csd.ganymede.rmi.BaseField
   */

  public boolean isPassword()
  {
    return (type == PASSWORD);
  }

  /**
   *
   * Returns true if this field is of IP type
   *
   * @see arlut.csd.ganymede.rmi.BaseField
   */

  public boolean isIP()
  {
    return (type == IP);
  }

  /**
   *
   * Returns true if this field is of float type
   *
   * @see arlut.csd.ganymede.rmi.BaseField
   */

  public boolean isFloat()
  {
    return (type == FLOAT);
  }

  /**
   *
   * Returns true if this field is of field option type
   *
   * @see arlut.csd.ganymede.rmi.BaseField
   */

  public boolean isFieldOptions()
  {
    return (type == FIELDOPTIONS);
  }

  /**
   *
   * Returns true if this field is one of the standard
   * fields that are part of all objects held in
   * the Ganymede server.
   *
   */

  public boolean isBuiltIn()
  {
    return builtIn;
  }

  /**
   *
   * If this field is a BOOLEAN, returns true
   * if this field should be presented as a
   * pair of radio boxes labeled by getTrueLabel() and
   * getFalseLabel().
   *
   */

  public boolean isLabeled()
  {
    return labeled;
  }

  /**
   *
   * If this field is a BOOLEAN and isLabeled() is true,
   * returns the text to be associated with the true
   * choice.
   *
   */

  public String getTrueLabel()
  {
    return trueLabel;
  }

  /**
   *
   * If this field is a BOOLEAN and isLabeled() is true,
   * returns the text to be associated with the false
   * choice.
   *
   */

  public String getFalseLabel()
  {
    return falseLabel;
  }

  /**
   *
   * If this field is a STRING, returns the minimum acceptable length
   * for this field.
   *
   */

  public short getMinLength()
  {
    return minLength;
  }

  /**
   *
   * If this field is a STRING, returns the maximum acceptable length
   * for this field.
   *
   */

  public short getMaxLength()
  {
    return maxLength;
  }

  /**
   *
   * If this field is a STRING and a limited set of characters are
   * acceptable in this field, returns those characters.
   *
   */

  public String getOKChars()
  {
    return okChars;
  }

  /**
   *
   * If this field is a STRING and a limited set of characters are
   * not acceptable in this field, returns those characters.
   *
   */

  public String getBadChars()
  {
    return badChars;
  }

  /**
   *
   * If this field is a STRING and a regular expression has been
   * set to limit acceptable strings in this field, returns the
   * regexp patter string.
   *
   */

  public String getRegexpPat()
  {
    return regexpPat;
  }

  /**
   *
   * If this field is a STRING and this field has been configured to
   * be a multiline string field, this method will return true.
   *
   */

  public boolean isMultiLine()
  {
    return multiLine;
  }

  /**
   *
   * If this field is an INVID, returns true if this field refers
   * to an 'embedded' object.
   *
   */

  public boolean isEditInPlace()
  {
    return editInPlace;
  }

  /**
   *
   * If this field is an INVID and is configured to point to an object
   * of a certain type, returns the object type this field points to.<br><br>
   *
   * This method will return a negative value if this field can point
   * to objects of differing types.
   *
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
   * debug instrumentation
   */

  public String toString()
  {
    StringBuilder result = new StringBuilder();

    /* -- */

    result.append("Field template: ");
    result.append(getName());
    result.append("<");
    result.append(getID());
    result.append(">");

    if (isBoolean())
      {
        result.append(" boolean");
      }
    else if (isNumeric())
      {
        result.append(" numeric");
      }
    else if (isDate())
      {
        result.append(" date");
      }
    else if (isString())
      {
        result.append(" string, ");

        result.append("minlength = ");
        result.append(getMinLength());
        result.append(", maxlength = ");
        result.append(getMaxLength());
        result.append(", okChars = ");
        result.append(getOKChars());
        result.append(", badChars = ");
        result.append(getBadChars());
        result.append(", regexp = ");
        result.append(getRegexpPat());
      }
    else if (isInvid())
      {
        result.append(" invid");
      }
    else if (isPermMatrix())
      {
        result.append(" permmatrix");
      }
    else if (isPassword())
      {
        result.append(" password");
      }
    else if (isIP())
      {
        result.append(" ip");
      }
    else if (isFloat())
      {
        result.append(" float");
      }

    return result.toString();
  }
}
