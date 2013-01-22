/*

   ErrorTypeEnum.java

   This Enum is used to label certain types of error conditions that
   can be transmitted by a ReturnVal from the server to the client

   Created: 5 October 2011

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2013
   The University of Texas at Austin

   Ganymede is a registered trademark of The University of Texas at Austin

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

import arlut.csd.Util.TranslationService;

/*------------------------------------------------------------------------------
                                                                            enum
                                                                   ErrorTypeEnum

------------------------------------------------------------------------------*/

/**
 * <p>This Enum is used to label certain types of error conditions
 * that can be transmitted by a {@link
 * arlut.csd.ganymede.common.ReturnVal} from the server to the
 * client</p>
 *
 * <p>As a Java 5 enum, this class is inherently serializable, and the
 * Ganymede server's RMI API involves the transmission of ErrorTypeEnum
 * values to and from Ganymede clients.</p>
 */

public enum ErrorTypeEnum {

  /**
   * <p>An error type that is not associated with any special handling
   * in the client.</p>
   */

  UNSPECIFIED,

  /**
   * <p>If a ReturnVal's error type is SHOWOBJECT, that signifies that
   * the client should call {@link
   * arlut.csd.ganymede.common.ReturnVal#getInvid()} to identify the
   * object that should be brought to the front before showing the
   * error dialog encoded in the ReturnVal.  The client will respond
   * to this by bringing any create or edit window on that object to
   * the front so the user knows which object the error relates to,
   * even if the object is not complete enough to have a label.</p>
   */

  SHOWOBJECT,

  /**
   * <p>Returned if a login attempt failed due to bad credentials.</p>
   */

  BADCREDS;
}
