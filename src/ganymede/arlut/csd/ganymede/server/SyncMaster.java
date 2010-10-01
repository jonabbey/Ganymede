/*

   SyncMaster.java

   An interface used to inject additional context information into a
   Sync Channel XML file for the benefit of the servicing program.

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
								       interface
								      SyncMaster

------------------------------------------------------------------------------*/

/**
 * <p>This interface is used to expand the list of objects (and fields)
 * that are emitted to an incremental sync channel output stream.</p>
 *
 * <p>The idea for this interface is that we may need to provide
 * additional context for an incremental synchronization.</p>
 *
 * <p>Imagine a transaction in which a user is modified.  A Sync Channel
 * is defined which specifies an interest in one of the user object's
 * fields which was modified by the transaction.  The Sync Channel
 * will write out an XML file describing all the changes to the user
 * object that the Sync Channel definition is interested in.</p>
 *
 * <p>What happens if the synchronization software that reads that XML
 * file needs information to complete the synchronization that comes
 * from other objects in the Ganymede data store?  Those objects may
 * not have changed in the transaction, so the Sync Channel will not
 * know to include that infromation in the XML.</p>
 *
 * <p>That's where the SyncMaster interface comes in.  A Sync Channel can
 * be configured with the fully qualified classname of a Java class
 * implementing this interface.  When this is done, the Sync Channel
 * calls augment() on the SyncMaster for every object to be written to
 * the XML file.  The augment() method examines each DBEditObject and
 * will call methods on the FieldBook parameter to request other
 * objects and fields to be included in the XML transaction file for
 * the benefit of the script servicing the Sync Channel queue.</p>
 *
 * <p>The objects added to the XML file by a SyncMaster will be contained
 * in a &lt;context_objects&gt; element, unless the context object
 * itself was modified in the transaction.  Sync Channels that need
 * context augmentation should have queue service programs that can
 * deal with these objects being written out either in the
 * &lt;context_objects&gt; element or in an &lt;object_delta&gt;
 * element along with the rest of the changes made by the transaction.</p>
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
