/*

   DumpResultCol.java

   This class is a simple data carrier for column definitions carried
   in the DumpResult class.  It is used to encapsulate a few pieces of
   information about each column, namely the name of the field held in
   the column, the Ganymede field id held in the column, and the
   Ganymede field type held in the column.
   
   Created: 5 November 2007

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
            
   Ganymede Directory Management System
 
   Copyright (C) 1996-2010
   The University of Texas at Austin

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
   along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/

package arlut.csd.ganymede.common;

/*------------------------------------------------------------------------------
                                                                           class
                                                                   DumpResultCol

------------------------------------------------------------------------------*/

/**
 * This class is a simple data carrier for column definitions carried
 * in the DumpResult class.  It is used to encapsulate a few pieces of
 * information about each column, namely the name of the field held in
 * the column, the Ganymede field id held in the column, and the
 * Ganymede field type held in the column.
 */

public class DumpResultCol {

  private String fieldName;
  private short fieldId;
  private short fieldType;

  /* -- */

  public DumpResultCol(String fieldName, short fieldId, short fieldType)
  {
    this.fieldName = fieldName;
    this.fieldId = fieldId;
    this.fieldType = fieldType;
  }

  /**
   * Returns the name of the data field that this column corresponds
   * to.
   */

  public String getName()
  {
    return this.fieldName;
  }

  /**
   * Returns the field id for this column.
   */

  public short getFieldId()
  {
    return this.fieldId;
  }

  /**
   * Returns the field id for this column, but with the value
   * encapsulated in a Short object.
   */


  public Short getFieldIdValue()
  {
    return Short.valueOf(this.fieldId);
  }

  /**
   * Returns the field type for this column, to be interpreted
   * according to the values enumerated in the {@link
   * arlut.csd.ganymede.common.FieldType FieldType} interface.
   */

  public short getFieldType()
  {
    return this.fieldType;
  }

  /**
   * Returns the field type for this column, to be interpreted
   * according to the values enumerated in the {@link
   * arlut.csd.ganymede.common.FieldType FieldType} interface, but
   * with the value encapsulated in a Short object.
   */

  public Short getFieldTypeValue()
  {
    return Short.valueOf(this.fieldType);
  }
}
