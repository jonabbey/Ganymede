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
 * <p>Enum of synchronization choices for a specific object type or field
 * in the Ganymede server.</p>
 *
 * <p>SyncPrefEnum values are contained in a <a
 * href="../../../../../synchronization/index.html">Sync Channel</a>
 * object's Sync Data {@link arlut.csd.ganymede.server.FieldOptionDBField
 * FieldOptionDBField} field, and control whether a given object or
 * field should be written to that Sync Channel.</p>
 *
 * <p>Note that a Sync Channel may have an associated {@link
 * arlut.csd.ganymede.server.SyncMaster SyncMaster} class registered,
 * which can expand on the set of objects and fields written to a Sync
 * Channel.  The Sync Master class is free to add objects and fields
 * to the XML transaction file to provide context, and could be used
 * if the three-way logic implemented by SyncPrefEnum and the {@link
 * arlut.csd.ganymede.server.SyncRunner} class is not sufficient for
 * your needs.</p>
 *
 * <p>As a Java 5 enum, this class is inherently serializable, and the
 * Ganymede server's RMI API involves the transmission of SyncPrefEnum
 * values to and from Ganymede clients.</p>
 */

public enum SyncPrefEnum {

  /**
   * <p>Don't write an object or field out.</p>
   *
   * <p>When used to control an object-level synchronization, NEVER
   * means that the object should not be written to the Sync Channel,
   * no matter what was done to the object during a transaction.</p>
   *
   * <p>When used to control synchronization for a specific field,
   * NEVER means that the field should not be written to the Sync
   * Channel, even if certain other fields in a modified WHENCHANGED
   * object are written out to the channel.</p>
   */

  NEVER,

  /**
   * <p>Write an object or field out if it changed.</p>
   *
   * <p>When used to control an object-level synchronization,
   * WHENCHANGED means that the object should be written to the Sync
   * Channel if any WHENCHANGED or ALWAYS fields contained within it
   * are modified by a transaction.</p>
   *
   * <p>When used to control synchronization for a specific field,
   * WHENCHANGED means that the field should be written to the Sync
   * Channel if it was changed in a transaction.</p>
   */

  WHENCHANGED,

  /**
   * <p>Write a field out if it or any other WHENCHANGED or ALWAYS
   * field in the object is changed.</p>
   *
   * <p>When used to control synchronization for a specific field,
   * ALWAYS means that the field should always be included whenever
   * any part of the WHENCHANGED object that it is contained in is
   * written to the Sync Channel.</p>
   *
   * <p>ALWAYS is like WHENCHANGED plus; if an ALWAYS field is changed
   * by a transaction, that will suffice to cause the containing
   * object to be considered as changed for the purposes of a Sync
   * Channel.</p>
   *
   * <p>Note: ALWAYS is only meaningful at the field level in a Sync
   * Channel's Sync Data FieldOptionDBField field.  The client
   * provides no way to set ALWAYS for an object type, only NEVER and
   * WHENCHANGED.</p>
   */

  ALWAYS;

  /**
   * TranslationService object for handling string localization in the
   * Ganymede system.
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.common.SyncPrefEnum");

  /**
   * The localized names for these enum values.  The order of these
   * labels in the array is significant, and must match the integral
   * value we associate with these enum values.
   */

  static public final String labels[] = {ts.l("global.never"), // "Never"
					 ts.l("global.whenchanged"), // "When Changed"
					 ts.l("global.always")}; // "Always"

  /**
   * Full state sync channel fields only should have never and always.
   */

  static public final String fullStateLabels[] = {ts.l("global.never"), // "Never"
						  ts.l("global.always")}; // "Always"

  /**
   * Locates and returns a SyncPrefEnum instance given the old numeric
   * strings for the values that we used before SyncPrefEnum and / or
   * the labels.
   */

  public static SyncPrefEnum find(String val)
  {
    if (val.equals("0") || val.equals(labels[0]))
      {
	return NEVER;
      }

    if (val.equals("1") || val.equals(labels[1]))
      {
	return WHENCHANGED;
      }

    if (val.equals("2") || val.equals(labels[2]))
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
	return labels[0];

      case WHENCHANGED:
	return labels[1];

      case ALWAYS:
	return labels[2];

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
