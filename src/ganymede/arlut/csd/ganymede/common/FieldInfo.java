/*

   FieldInfo.java

   This class is a serializable object to return all the information
   the container panel needs to render a field.

   Created: 4 November 1997

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2013
   The University of Texas at Austin

   Ganymede is a registered trademark of The University of Texas at Austin

   Contact information

   Web site: http://www.arlut.utexas.edu/gash2
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

import java.rmi.RemoteException;
import java.util.Vector;

import arlut.csd.ganymede.rmi.db_field;
import arlut.csd.ganymede.server.PasswordDBField;

/*------------------------------------------------------------------------------
                                                                           class
                                                                       FieldInfo

------------------------------------------------------------------------------*/

/**
 * <p>This class is a serializable object used to return all the value
 * information the client's {@link
 * arlut.csd.ganymede.client.containerPanel containerPanel} needs to
 * render a specific field instance, including the current value held
 * in this field and the current editability/visibility this field has
 * with respect to the user's {@link
 * arlut.csd.ganymede.server.GanymedeSession GanymedeSession}..</p>
 *
 * <p>The {@link arlut.csd.ganymede.common.FieldTemplate
 * FieldTemplate} object is used to return the invariant (during the
 * client's connection) type information associated with the field
 * generically across all objects of the type containing this
 * field.</p>
 *
 * @author Jonathan Abbey, jonabbey@arlut.utexas.edu
 */

public class FieldInfo implements java.io.Serializable {

  static final long serialVersionUID = -4457805568492289591L;

  // ---

  db_field
    field;

  short
    ID;

  boolean
    defined,
    editable,
    visible;

  Object
    value;

  /* -- */

  /**
   * <p>This constructor takes a {@link arlut.csd.ganymede.rmi.db_field
   * db_field} interface instead of a {@link
   * arlut.csd.ganymede.server.DBField DBField} object so that we can
   * avoid referencing a server-side object in a transport class, but
   * we will create FieldInfo objects from DBFields on the server side.</p>
   */

  public FieldInfo(db_field field) throws GanyPermissionsException
  {
    this.field = field;

    try
      {
        defined = field.isDefined();
        editable = field.isEditable();
        visible = field.isVisible();

        ID = field.getID();

        if (field instanceof PasswordDBField)
          {
            // n.b. getValueString() returns a description of what
            // types of password encodings are present in the password
            // field, but doesn't return any actual plain or hashtext
            //
            // getValueString() is actually not part of the db_field
            // interface, as it is a server-side only method, so we
            // have to cast.

            value = ((PasswordDBField) field).getValueString();
          }
        else if (!field.isVector())
          {
            value = field.getValue(); // can throw GanyPermissionsException on perms failure
          }
        else
          {
            value = field.getValues();// can throw GanyPermissionsException on perms failure
          }
      }
    catch (GanyPermissionsException ex)
      {
        throw ex;
      }
    catch (RemoteException ex)
      {
        throw new RuntimeException(ex.getMessage());
      }
  }

  /**
   * <p>Returns the a remote reference to the field on the server.</p>
   */

  public db_field getField()
  {
    return field;
  }

  /**
   * <p>Returns the field's id number within the containing {@link
   * arlut.csd.ganymede.server.DBObject DBObject} on the server.</p>
   */

  public short getID()
  {
    return ID;
  }

  /**
   * <p>Returns the field's id number within the containing {@link
   * arlut.csd.ganymede.server.DBObject DBObject} on the server as a
   * boxed java.lang.Short, suitable for use in a Hashtable or Vector or
   * the like.</p>
   */

  public Short getIDObj()
  {
    return Short.valueOf(ID);
  }

  public boolean isDefined()
  {
    return defined;
  }

  public boolean isEditable()
  {
    return editable;
  }

  public boolean isVisible()
  {
    return visible;
  }

  /**
   * <p>Returns the value of this field.  The Object may be a String,
   * a Date, a Double, an Integer, an {@link
   * arlut.csd.ganymede.common.Invid Invid}, or even a {@link
   * java.util.Vector Vector} in the case of a multi-value field.</p>
   */

  public Object getValue()
  {
    if (Invid.hasAllocator())
      {
        if (value instanceof Invid)
          {
            value = ((Invid) value).intern();
          }
        else if (value instanceof Vector)
          {
            Vector myVect = (Vector) value;

            synchronized (myVect)
              {
                for (int i = 0; i < myVect.size(); i++)
                  {
                    Object elem = myVect.elementAt(i);

                    if (!(elem instanceof Invid))
                      {
                        break;      // we've got a non invid vector, abort
                      }
                    else
                      {
                        Invid myInvid = (Invid) elem;

                        if (myInvid.isInterned())
                          {
                            // this is designed as a fast-fail, since we
                            // know that a vector of Invids fresh from
                            // de-serialization won't be interned at all

                            break;
                          }
                        else
                          {
                            myVect.setElementAt(myInvid.intern(), i);
                          }
                      }
                  }
              }
          }
      }

    return value;
  }
}
