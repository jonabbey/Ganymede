/*

   SyncPrefEnum.java

   This Enum is used to record the synchronization configuration for a
   specific object or field in a Ganymede server Sync Channel.
   
   Created: 5 October 2010

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

import arlut.csd.Util.TranslationService;

/*------------------------------------------------------------------------------
                                                                            enum
                                                                    SyncPrefEnum

------------------------------------------------------------------------------*/

/**
 * Enum of configuration choices for a specific object type or field in
 */

public enum SyncPrefEnum {

  NEVER,
  ALWAYS,
  WHENCHANGED;

  /**
   * TranslationService object for handling string localization in the
   * Ganymede system.
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.common.SyncPrefEnum");

  public static SyncPrefEnum find(String val)
  {
    if (val.equals("0"))
      {
	return NEVER;
      }

    if (val.equals("1"))
      {
	return WHENCHANGED;
      }

    if (val.equals("2"))
      {
	return ALWAYS;
      }

    throw new IllegalArgumentException("bad option string");
  }

  public String toString()
  {
    switch (this)
      {
      case NEVER:
	return ts.l("never");

      case WHENCHANGED:
	return ts.l("whenchanged");

      case ALWAYS:
	return ts.l("always");

      default:
	return null;		// no-op
      }
  }

  /**
   * Returns the on-disk string representation of this enum value.
   */

  public String str()
  {
    switch (this)
      {
      case NEVER:
	return "0";
      
      case WHENCHANGED:
	return "1";

      case ALWAYS:   
	return "2";

      default:
	return null;		// no-op
      }
  }

  /**
   * Returns the numerical representation of this enum value, to match
   * our previous numeric coding.
   */

  public int ord()
  {
    switch (this)
      {
      case NEVER:
	return 0;
      
      case WHENCHANGED:
	return 1;

      case ALWAYS:   
	return 2;

      default:
	return -1;		// no-op
      }
  }
}
