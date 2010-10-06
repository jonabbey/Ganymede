/*

   field_option_field.java

   Client-side interface to the FieldOptionDBField class.
   
   Created: 25 January 2005

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996 - 2010
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

package arlut.csd.ganymede.rmi;

import java.rmi.RemoteException;

import arlut.csd.ganymede.common.FieldOptionMatrix;
import arlut.csd.ganymede.common.ReturnVal;
import arlut.csd.ganymede.common.SyncPrefEnum;

/*------------------------------------------------------------------------------
                                                                       interface
                                                              field_option_field

------------------------------------------------------------------------------*/

/**
 * <p>Client-side remote interface to the {@link
 * arlut.csd.ganymede.server.FieldOptionDBField FieldOptionDBField}
 * class.</p> 
 */

public interface field_option_field extends db_field {

  /**
   * <p>Return a serializable, read-only copy of this field's option
   * matrix</p>
   */

  public FieldOptionMatrix getMatrix() throws RemoteException;

  /**
   * <p>Returns a SyncPrefEnum representing this field option field's
   * option on the field &lt;field&gt; in base
   * &lt;base&gt;<br><br></p>
   */

  public SyncPrefEnum getOption(short baseID, short fieldID) throws RemoteException;

  /**
   * <p>Returns a SyncPrefEnum representing this field option field's 
   * option on the base &lt;base&gt;</p>
   */

  public SyncPrefEnum getOption(short baseID) throws RemoteException;

  /**
   * <p>Returns a SyncPrefEnum representing this field option field's 
   * option on the field &lt;field&gt; in base &lt;base&gt;</p>
   */

  public SyncPrefEnum getOption(Base base, BaseField field) throws RemoteException;

  /**
   * <p>Returns a SyncPrefEnum object representing this field option field's 
   * option on the base &lt;base&gt;</p>
   */

  public SyncPrefEnum getOption(Base base) throws RemoteException;

  /**
   * <p>Resets the options in this FieldOptionDBField to the empty
   * set.  Used by non-interactive clients to reset the SyncPrefEnum
   * values to a known state before setting options.</p>
   *
   * <p>Returns null on success, or a failure-coded ReturnVal
   * on permissions failure.</p>
   */

  public ReturnVal resetOptions() throws RemoteException;

  /**
   * <p>Sets the SyncPrefEnum for this matrix for base &lt;baseID&gt;,
   * field &lt;fieldID&gt; to &lt;option&gt;.</p>
   *
   * <p>This operation will fail if this field option field is not
   * associated with a currently checked-out-for-editing
   * {@link arlut.csd.ganymede.server.FieldOptionDBField FieldOptionDBField}.</p>
   */

  public ReturnVal setOption(short baseID, short fieldID, SyncPrefEnum option) throws RemoteException;

  /**
   * <p>Sets the SyncPrefEnum for this matrix for base &lt;baseID&gt;
   * to &lt;option&gt;</p>
   *
   * <p>This operation will fail if this field option field is not
   * associated with a currently checked-out-for-editing
   * {@link arlut.csd.ganymede.server.FieldOptionDBField FieldOptionDBField}.</p>
   */

  public ReturnVal setOption(short baseID, SyncPrefEnum option) throws RemoteException;

  /**
   * <p>Sets the SyncPrefEnum for this matrix for base &lt;baseID&gt;,
   * field &lt;fieldID&gt; to &lt;option&gt;.</p>
   *
   * <p>This operation will fail if this field option field is not
   * associated with a currently checked-out-for-editing
   * {@link arlut.csd.ganymede.server.FieldOptionDBField FieldOptionDBField}.</p>
   */

  public ReturnVal setOption(Base base, BaseField field, SyncPrefEnum option) throws RemoteException;

  /**
   * <p>Sets the SyncPrefEnum for this matrix for base &lt;baseID&gt;
   * to &lt;option&gt;</p>
   *
   * <p>This operation will fail if this field option field is not
   * associated with a currently checked-out-for-editing
   * {@link arlut.csd.ganymede.server.FieldOptionDBField FieldOptionDBField}.</p>
   */

  public ReturnVal setOption(Base base, SyncPrefEnum option) throws RemoteException;
}
