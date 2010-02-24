/*

   SyncMaster.java

   This interface is used to expand the list of objects (and fields)
   that are emitted to an incremental sync channel output stream.

   The idea for this interface is that an incremental Sync Channel can
   be defined in the Ganymede system that calls out to an
   implementation of this interface to allow the Ganymede object
   information that is passed to the sync channel through the
   'fieldoption' field controlling what gets sent upon changes to be
   augmented by additional object context that the external sync
   channel scripting may depend on.

   Created: 22 February 2010

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

package arlut.csd.ganymede.server;

import arlut.csd.ganymede.common.FieldBook;
import arlut.csd.ganymede.common.Invid;

/*------------------------------------------------------------------------------
									   class
								      SyncMaster

------------------------------------------------------------------------------*/

/**
 * This interface is used to expand the list of objects (and fields)
 * that are emitted to an incremental sync channel output stream.
 *
 * The idea for this interface is that an incremental Sync Channel can
 * be defined in the Ganymede system that calls out to an
 * implementation of this interface to allow the Ganymede object
 * information that is passed to the sync channel through the
 * 'fieldoption' field controlling what gets sent upon changes to be
 * augmented by additional object context that the external sync
 * channel scripting may depend on.
 *
 * The Ganymede server's 'Sync Channel' object type will be expanded
 * with a class name field to allow for specification of an
 * implementation of the SyncMaster interface that will be consulted
 * when performing a write to the Sync Channel.
 */

public interface SyncMaster {

  /**
   * The augment() method optionally adds DBObject and DBField
   * identifiers to the FieldBook book parameter if the SyncMaster
   * decides that the additional DBObject/DBFields need to be written
   * to a delta sync channel in response to the changes made to obj.
   */

  public void augment(FieldBook book, DBEditObject obj);
}
