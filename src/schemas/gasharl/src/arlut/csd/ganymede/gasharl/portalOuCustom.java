/*

   portalOuCustom.java

   This file is a management class for the portal ou object type in
   the gasharl schema kit.

   Created: 13 June 2012

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2012
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

package arlut.csd.ganymede.gasharl;

import java.util.Vector;

import arlut.csd.ganymede.common.GanyPermissionsException;
import arlut.csd.ganymede.common.Invid;
import arlut.csd.ganymede.common.NotLoggedInException;
import arlut.csd.ganymede.common.QueryResult;
import arlut.csd.ganymede.common.ReturnVal;
import arlut.csd.ganymede.common.SchemaConstants;
import arlut.csd.ganymede.server.DBEditObject;
import arlut.csd.ganymede.server.DBEditSet;
import arlut.csd.ganymede.server.DBField;
import arlut.csd.ganymede.server.DBObject;
import arlut.csd.ganymede.server.DBObjectBase;
import arlut.csd.ganymede.server.Ganymede;
import arlut.csd.ganymede.server.InvidDBField;

/*------------------------------------------------------------------------------
                                                                           class
                                                                  portalOuCustom

------------------------------------------------------------------------------*/

/**
 * Management class for the portal ou object type in the gasharl
 * schema kit.
 */

public class portalOuCustom extends DBEditObject implements SchemaConstants, mapSchema {

  /**
   *
   * Customization Constructor
   *
   */

  public portalOuCustom(DBObjectBase objectBase)
  {
    super(objectBase);
  }

  /**
   *
   * Create new object constructor
   *
   */

  public portalOuCustom(DBObjectBase objectBase, Invid invid, DBEditSet editset)
  {
    super(objectBase, invid, editset);
  }

  /**
   *
   * Check-out constructor, used by DBObject.createShadow()
   * to pull out an object for editing.
   *
   */

  public portalOuCustom(DBObject original, DBEditSet editset)
  {
    super(original, editset);
  }

  /**
   * <p>Customization method to control whether a specified field
   * is required to be defined at commit time for a given object.</p>
   *
   * <p>To be overridden in DBEditObject subclasses.</p>
   */

  public boolean fieldRequired(DBObject object, short fieldid)
  {
    if (fieldid == portalOuSchema.OU ||
	fieldid == portalOuSchema.NAME)
      {
	return true;
      }

    return false;
  }


  /**
   * <p>This method provides a hook that can be used by subclasses of
   * DBEditObject to return a list of attribute names and attribute
   * values to include when writing out &lt;invid&gt; elements that
   * point to them during a sync channel operation.  This method
   * differs from {@link
   * arlut.csd.ganymede.server.DBEditObject#getForeignSyncKeys(arlut.csd.ganymede.common.Invid,
   * arlut.csd.ganymede.server.DBObject,
   * arlut.csd.ganymede.server.DBObject, java.lang.String, boolean)}
   * in that this method adds extra attributes to &lt;invid&gt;
   * elements pointing to objects of this kind, where
   * getForeignSyncKeys() adds extra attributes to &lt;invid&gt;
   * elements that point from objects of this kind.</p>
   *
   * <p>The point of this is to allow DBEditObject subclasses to
   * inject additional data into a transactional sync record so that
   * external sync channel service code can have enough information to
   * identify a relationship that was made or broken within the
   * transaction.  For instance, if you are synchronizing to an LDAP
   * structure with the XML Sync Channel mechanism, you might want all
   * of your &lt;invid&gt; elements that point to Users to include a
   * dn attribute that provides the fully qualified LDAP DN for the
   * User object.</p>
   *
   * <p>The array returned should have an even number of values.  The
   * first value should be an attribute name, the second should be
   * the value for that attribute name, the third should be another
   * attribute name, and etc.</p>
   *
   * <p>It is an error to return an attribute name that conflicts with
   * the set pre-defined for use with the &lt;invid&gt; XML element.
   * This includes <code>type</code>, <code>num</code>, <code>id</code>,
   * and <code>oid</code>.</p>
   *
   * <p>To be overridden on necessity in DBEditObject subclasses.</p>
   *
   * <p><b>*PSEUDOSTATIC*</b></p>
   *
   * @param myObj The object that is being targetted by an &lt;invid&gt; element.
   * @param syncChannel The name of the Sync Channel that the &lt;invid&gt; is being written to.
   * @param forceOriginal If true and the foreign sync keys are being
   * generated in a transactional context (as in the incremental Sync
   * Channel writes), getMySyncKeys() will attempt to resolve the
   * extra attributes against the original version of any objects resolved during
   * generation of the extra attributes.
   */

  public String[] getMyExtraInvidAttributes(DBObject myObj, String syncChannel, boolean forceOriginal)
  {
    String[] results = new String[2];
    results[0] = "ou";
    results[1] = (String) myObj.getFieldValueLocal(portalOuSchema.OU);

    return results;
  }
}
