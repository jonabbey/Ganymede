/*

   FieldOptionMatrix.java

   This class stores a read-only matrix of option strings, organized
   by object type and field id's.
   
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

package arlut.csd.ganymede.common;

import java.rmi.RemoteException;
import java.util.Enumeration;
import java.util.Hashtable;

import arlut.csd.ganymede.rmi.Base;
import arlut.csd.ganymede.rmi.BaseField;
import arlut.csd.ganymede.server.FieldOptionDBField;

/*------------------------------------------------------------------------------
                                                                           class
                                                               FieldOptionMatrix

------------------------------------------------------------------------------*/

/**
 * <P>Serializable field option matrix object, used to handle
 * field options for a SyncRunner.</P>
 *
 * <P>This class stores a read-only Hashtable of Strings, organized by
 * object type and field id's.</P>
 *
 * <P>The keys to the Hashtable are Strings that are encoded by the
 * static matrixEntry() methods defined in this class.  I probably
 * could have used some sort of class object for the key, but then I
 * would have had to define a key() of similar complexity to the
 * matrixEntry() and decode methods anyway, as well as some sort of
 * on-disk representation for the Ganymede.db file.</P>
 *
 * <P>Here's some examples of the key encoding algorithm:</P>
 *
 * <UL>
 * <LI><CODE>3::/CODE> - Object type 3, option for object itself</LI>
 * <LI><CODE>3:10</CODE> - Object type 3, option for field 10</LI>
 * </UL>
 *
 * <P>FieldOptionMatrix is used on the client in the Field Option dialog,
 * and on the server in both
 * {@link arlut.csd.ganymede.server.FieldOptionDBField FieldOptionDBField}
 * and {@link arlut.csd.ganymede.server.SyncRunner SyncRunner}.</P> */

public class FieldOptionMatrix implements java.io.Serializable {

  static final boolean debug = false;

  // ---

  public Hashtable matrix;

  /* -- */

  public FieldOptionMatrix()
  {
    matrix = new Hashtable();
  }

  public FieldOptionMatrix(Hashtable orig)
  {
    if (orig == null)
      {
	this.matrix = new Hashtable();
      }
    else
      {
	this.matrix = (Hashtable) orig.clone();
      }
  }

  public FieldOptionMatrix(FieldOptionMatrix orig)
  {
    this.matrix = (Hashtable) orig.matrix.clone();
  }

  /**
   * <P>Returns a String representing this FieldOptionMatrix's 
   * option on the field &lt;fieldID&gt; in base &lt;baseID&gt;</P>
   *
   * <P>If there is no entry in this FieldOptionMatrix for the given
   * field, getOption() will return null.</P>
   */

  public String getOption(short baseID, short fieldID)
  {
    return (String) matrix.get(matrixEntry(baseID, fieldID));
  }

  /**
   * <P>Returns a String object representing this FieldOptionMatrix's 
   * option on the base &lt;baseID&gt;</P>
   */

  public String getOption(short baseID)
  {
    return (String) matrix.get(matrixEntry(baseID));
  }

  /**
   * <P>Returns a String representing this FieldOptionMatrix's 
   * option on the field &lt;field&gt; in base &lt;base&gt;</P>
   */

  public String getOption(Base base, BaseField field)
  {
    try
      {
	return getOption(base.getTypeID(), field.getID());
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("caught remote: " + ex);
      }
  }

  /**
   * <P>Returns a String representing this FieldOptionMatrix's 
   * option on the base &lt;base&gt;</P>
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
   * <P>Private method to generate a key for use in
   * our internal Hashtable, used to encode the 
   * option for a given {@link arlut.csd.ganymede.server.DBObjectBase
   * DBObjectBase} and {@link arlut.csd.ganymede.server.DBObjectBaseField
   * DBObjectBaseField}.</P>
   */

  private String matrixEntry(short baseID, short fieldID)
  {
    return (baseID + ":" + fieldID);
  }

  /**
   * <P>Private method to generate a key for use in
   * our internal Hashtable, used to encode the
   * option for a given {@link arlut.csd.ganymede.server.DBObjectBase
   * DBObjectBase}.</P>
   */
  
  private String matrixEntry(short baseID)
  {
    return (baseID + "::");
  }

  /**
   * <P>Returns true if the given String encodes the identity of
   * a {@link arlut.csd.ganymede.server.DBObjectBase DBObjectBase} and
   * not a field within a DBObjectBase.</P>
   */

  private boolean isBase(String matrixEntry)
  {
    return (matrixEntry.indexOf("::") != -1);
  }

  /**
   * <P>Private helper method used to decode a hash key generated
   * by the matrixEntry() methods.</P>
   *
   * @return Returns the {@link arlut.csd.ganymede.server.DBObjectBase DBObjectBase}
   * object id encoded by the given String.
   */

  private short entryBase(String matrixEntry)
  {
    if (matrixEntry.indexOf(':') == -1)
      {
	throw new IllegalArgumentException("not a valid matrixEntry");
      }

    String baseStr = matrixEntry.substring(0, matrixEntry.indexOf(':'));

    try
      {
	return Short.parseShort(baseStr);
      }
    catch (NumberFormatException ex)
      {
	throw new RuntimeException("bad string format:" + ex);
      }
  }

  /**
   * <P>Private helper method used to decode a hash key generated
   * by the matrixEntry() methods.</P>
   *
   * @return Returns the
   * {@link arlut.csd.ganymede.server.DBObjectBaseField DBObjectBaseField}
   * object id encoded by the given String.
   */
  
  private short entryField(String matrixEntry)
  {
    if (matrixEntry.indexOf(':') == -1)
      {
	throw new IllegalArgumentException("not a valid matrixEntry");
      }

    if (isBase(matrixEntry))
      {
	throw new IllegalArgumentException("not a field matrixEntry");
      }

    String fieldStr = matrixEntry.substring(matrixEntry.lastIndexOf(':')+1);

    try
      {
	return Short.parseShort(fieldStr);
      }
    catch (NumberFormatException ex)
      {
	throw new RuntimeException("bad string format:" + ex);
      }
  }

  /**
   * <P>Private helper method used to generate a matrixEntry() encoded String
   * for a single  {@link arlut.csd.ganymede.server.DBObjectBase DBObjectBase} from
   * a matrixEntry() encoded String that also includes a field specification.</P>
   */

  private String baseEntry(String matrixEntry)
  {
    if (isBase(matrixEntry))
      {
	return matrixEntry;
      }
    else
      {
	return matrixEntry(entryBase(matrixEntry));
      }
  }

  /**
   * <P>Debugging output</P>
   */

  public String toString()
  {
    try
      {
	return FieldOptionDBField.debugdecode(matrix);
      }
    catch (Throwable ex)
      {
	return super.toString();
      }
  }
}
