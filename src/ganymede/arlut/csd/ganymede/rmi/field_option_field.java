/*

   field_option_field.java

   Client-side interface to the FieldOptionDBField class.
   
   Created: 25 January 2005
   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996 - 2005
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

package arlut.csd.ganymede.rmi;

import java.rmi.RemoteException;

import arlut.csd.ganymede.common.FieldOptionMatrix;
import arlut.csd.ganymede.common.ReturnVal;

/*------------------------------------------------------------------------------
                                                                       interface
                                                              field_option_field

------------------------------------------------------------------------------*/

/**
 * <P>Client-side remote interface to the {@link
 * arlut.csd.ganymede.server.FieldOptionDBField FieldOptionDBField}
 * class.</P> 
 */

public interface field_option_field extends db_field {

  /**
   *
   * Return a serializable, read-only copy of this field's option
   * matrix
   *
   */

  public FieldOptionMatrix getMatrix() throws RemoteException;

  /**
   * <P>Returns a String object representing this field option field's 
   * option on the field &lt;field&gt; in base &lt;base&gt;<br><br></P>
   */

  public String getOption(short baseID, short fieldID) throws RemoteException;

  /**
   *
   * Returns a String object representing this field option field's 
   * option on the base &lt;base&gt;
   */

  public String getOption(short baseID) throws RemoteException;

  /**
   * <P>Returns a String object representing this field option field's 
   * option on the field &lt;field&gt; in base &lt;base&gt;</P>
   */

  public String getOption(Base base, BaseField field) throws RemoteException;

  /**
   * <P>Returns a String object representing this field option field's 
   * option on the base &lt;base&gt;</P>
   */

  public String getOption(Base base) throws RemoteException;

  /**
   * <P>Resets the options in this FieldOptionDBField to
   * the empty set.  Used by non-interactive clients to reset
   * the option strings to a known state before setting
   * options.</P>
   *
   * <P>Returns null on success, or a failure-coded ReturnVal
   * on permissions failure.</P>
   */

  public ReturnVal resetOptions() throws RemoteException;

  /**
   * <P>Sets the option string for this matrix for base &lt;baseID&gt;,
   * field &lt;fieldID&gt; to String &lt;option&gt;.</P>
   *
   * <P>This operation will fail if this field option field is not
   * associated with a currently checked-out-for-editing
   * {@link arlut.csd.ganymede.server.FieldOptionDBField FieldOptionDBField}.</P>
   */

  public ReturnVal setOption(short baseID, short fieldID, String option) throws RemoteException;

  /**
   * <P>Sets the option string for this matrix for base &lt;baseID&gt;
   * to String &lt;option&gt;</P>
   *
   * <P>This operation will fail if this field option field is not
   * associated with a currently checked-out-for-editing
   * {@link arlut.csd.ganymede.server.FieldOptionDBField FieldOptionDBField}.</P>
   */

  public ReturnVal setOption(short baseID, String option) throws RemoteException;

  /**
   * <P>Sets the option string for this matrix for base &lt;baseID&gt;,
   * field &lt;fieldID&gt; to String &lt;entry&gt;.</P>
   *
   * <P>This operation will fail if this field option field is not
   * associated with a currently checked-out-for-editing
   * {@link arlut.csd.ganymede.server.FieldOptionDBField FieldOptionDBField}.</P>
   */

  public ReturnVal setOption(Base base, BaseField field, String option) throws RemoteException;

  /**
   * <P>Sets the option string for this matrix for base &lt;baseID&gt;
   * to String &lt;entry&gt;</P>
   *
   * <P>This operation will fail if this field option field is not
   * associated with a currently checked-out-for-editing
   * {@link arlut.csd.ganymede.server.FieldOptionDBField FieldOptionDBField}.</P>
   */

  public ReturnVal setOption(Base base, String option) throws RemoteException;
}
