/*
   GASH 2

   IPDBField.java

   The GANYMEDE object storage system.

   Created: 4 Sep 1997

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

package arlut.csd.ganymede.server;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Vector;

import arlut.csd.ganymede.common.GanyPermissionsException;
import arlut.csd.ganymede.common.IPAddress;
import arlut.csd.ganymede.common.ReturnVal;
import arlut.csd.ganymede.rmi.ip_field;
import arlut.csd.Util.TranslationService;
import arlut.csd.Util.VectorUtils;

/*------------------------------------------------------------------------------
                                                                           class
                                                                       IPDBField

------------------------------------------------------------------------------*/

/**
 * <p>IPDBField is a subclass of {@link
 * arlut.csd.ganymede.server.DBField DBField} for the storage and
 * handling of IPv4/IPv6 address fields in the {@link
 * arlut.csd.ganymede.server.DBStore DBStore} on the Ganymede
 * server.</p>
 *
 * <p>The Ganymede client talks to IPDBFields through the {@link
 * arlut.csd.ganymede.rmi.ip_field ip_field} RMI interface.</p>
 */

public class IPDBField extends DBField implements ip_field {

  /**
   * <p>TranslationService object for handling string localization in
   * the Ganymede server.</p>
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.server.IPDBField");

  // --

  /**
   * <p>Receive constructor.  Used to create a IPDBField from a {@link
   * arlut.csd.ganymede.server.DBStore DBStore}/{@link
   * arlut.csd.ganymede.server.DBJournal DBJournal} DataInput
   * stream.</p>
   */

  IPDBField(DBObject owner, DataInput in, DBObjectBaseField definition) throws IOException
  {
    super(owner, definition.getID());

    this.value = null;
    receive(in, definition);
  }

  /**
   * <p>No-value constructor.  Allows the construction of a
   * 'non-initialized' field, for use where the
   * {@link arlut.csd.ganymede.server.DBObjectBase DBObjectBase}
   * definition indicates that a given field may be present,
   * but for which no value has been stored in the
   * {@link arlut.csd.ganymede.server.DBStore DBStore}.</p>
   *
   * <p>Used to provide the client a template for 'creating' this
   * field if so desired.</p>
   */

  IPDBField(DBObject owner, DBObjectBaseField definition)
  {
    super(owner, definition.getID());

    if (isVector())
      {
        this.value = new Vector();
      }
    else
      {
        this.value = null;
      }
  }

  /**
   * Copy constructor.
   */

  public IPDBField(DBObject owner, IPDBField field)
  {
    super(owner, field.getID());

    if (isVector())
      {
        this.value = new Vector(field.getVectVal());
      }
    else
      {
        this.value = field.value;
      }
  }

  /**
   * Scalar value constructor.
   */

  public IPDBField(DBObject owner, Date value, DBObjectBaseField definition)
  {
    super(owner, definition.getID());

    if (definition.isArray())
      {
        throw new IllegalArgumentException("scalar constructor called on vector field");
      }

    this.value = value;
  }

  /**
   * Vector value constructor.
   */

  public IPDBField(DBObject owner, Vector values, DBObjectBaseField definition)
  {
    super(owner, definition.getID());

    if (!definition.isArray())
      {
        throw new IllegalArgumentException("vector constructor called on scalar field");
      }

    if (values == null)
      {
        this.value = new Vector();
      }
    else
      {
        this.value = new Vector(values);
      }
  }

  @Override public Object clone() throws CloneNotSupportedException
  {
    throw new CloneNotSupportedException();
  }

  @Override void emit(DataOutput out) throws IOException
  {
    if (isVector())
      {
        Vector<IPAddress> values = (Vector<IPAddress>) getVectVal();

        out.writeInt(values.size());

        for (IPAddress addr: values)
          {
            addr.emit(out);
          }
      }
    else
      {
        ((IPAddress) value).emit(out);
      }
  }

  @Override void receive(DataInput in, DBObjectBaseField definition) throws IOException
  {
    if (definition.isArray())
      {
        int count;

        if (Ganymede.db.isLessThan(2,3))
          {
            count = in.readShort();
          }
        else
          {
            count = in.readInt();
          }

        Vector<IPAddress> values = new Vector<IPAddress>(count);

        for (int i = 0; i < count; i++)
          {
            values.add(IPAddress.readIPAddr(in));
          }

        this.value = values;
      }
    else
      {
        this.value = IPAddress.readIPAddr(in);
      }
  }

  /**
   * <p>This method is used when the database is being dumped, to write
   * out this field to disk.</p>
   */

  @Override synchronized void emitXML(XMLDumpContext xmlOut) throws IOException
  {
    xmlOut.startElementIndent(this.getXMLName());

    if (!isVector())
      {
        emitIPXML(xmlOut, (IPAddress) this.value);
      }
    else
      {
        Vector<IPAddress> values = (Vector<IPAddress>) this.value;

        for (IPAddress addr: values)
          {
            xmlOut.indentOut();

            xmlOut.indent();
            emitIPXML(xmlOut, addr);

            xmlOut.indentIn();
          }

        xmlOut.indent();
      }

    xmlOut.endElement(this.getXMLName());
  }

  public void emitIPXML(XMLDumpContext xmlOut, IPAddress value) throws IOException
  {
    xmlOut.startElement("ip");
    xmlOut.attribute("val", value.toString());
    xmlOut.endElement("ip");
  }

  @Override public synchronized String getValueString()
  {
    if (!isVector())
      {
        return ((IPAddress) this.value).toString();
      }

    Vector<IPAddress> values = (Vector<IPAddress>) this.value;

    return VectorUtils.vectorString(values);
  }

  /**
   * The normal getValueString() encoding of IP addresses is acceptable.
   */

  @Override public String getEncodingString()
  {
    return getValueString();
  }

  /**
   * <p>Returns a String representing the change in value between this
   * field and orig.  This String is intended for logging and email,
   * not for any sort of programmatic activity.  The format of the
   * generated string is not defined, but is intended to be suitable
   * for inclusion in a log entry and in an email message.</p>
   *
   * <p>If there is no change in the field, null will be returned.</p>
   */

  @Override public synchronized String getDiffString(DBField orig)
  {
    if (!(orig instanceof IPDBField))
      {
        throw new IllegalArgumentException("bad field comparison");
      }

    if (orig == this)
      {
        return null;
      }

    if (isVector())
      {
        Vector added = VectorUtils.difference(getVectVal(), orig.getVectVal());
        Vector deleted = VectorUtils.difference(orig.getVectVal(), getVectVal());

        // were there any changes at all?

        if (deleted.size() == 0 && added.size() == 0)
          {
            return null;
          }
        else
          {
            StringBuilder result = new StringBuilder();

            if (deleted.size() != 0)
              {
                // "\tDeleted: {0}\n"
                result.append(ts.l("getDiffString.deleted", VectorUtils.vectorString(deleted, ", ")));
              }

            if (added.size() != 0)
              {
                // "\tAdded: {0}\n"
                result.append(ts.l("getDiffString.added", VectorUtils.vectorString(added, ", ")));
              }

            return result.toString();
          }
      }
    else
      {
        if (orig.value.equals(this.value))
          {
            return null;
          }

        StringBuilder result = new StringBuilder();

        // "\tOld: {0}\n"
        result.append(ts.l("getDiffString.old", orig.value));

        // "\tNew: {0}\n"
        result.append(ts.l("getDiffString.new", this.value));

        return result.toString();
      }
  }

  // ****
  //
  // ip_field methods
  //
  // ****

  /**
   * Returns true if this field is permitted to hold IPv6 addresses.
   */

  public boolean v6Allowed()
  {
    DBEditObject eObj = (DBEditObject) this.owner;

    return eObj.isIPv6OK(this);
  }

  /**
   * Returns true if the value stored in this IP field is an IPV4
   * address.  If no value has been set for this field, false is
   * returned.
   *
   * @see arlut.csd.ganymede.rmi.ip_field
   */

  public boolean isIPV4()
  {
    if (isVector())
      {
        throw new IllegalArgumentException("scalar accessor called on vector field");
      }

    IPAddress addr = (IPAddress) this.value;

    return addr == null ? false : addr.isIPv4();
  }

  /**
   * Returns true if the value stored in the given element of this IP
   * field is an IPV4 address, if no value is set, this method will
   * return false.
   *
   * @param index Array index for the value to be checked
   *
   * @see arlut.csd.ganymede.rmi.ip_field
   */

  public boolean isIPV4(short index)
  {
    if (!isVector())
      {
        throw new IllegalArgumentException("vector accessor called on scalar field");
      }

    IPAddress addr = (IPAddress) getVectVal().get(index);

    return addr == null ? false : addr.isIPv4();
  }

  /**
   * Returns true if the value stored in this IP field is an IPV6 address.  If
   * this field has no value set, this method will return false by default.
   *
   * @see arlut.csd.ganymede.rmi.ip_field
   */

  public boolean isIPV6()
  {
    if (isVector())
      {
        throw new IllegalArgumentException("scalar accessor called on vector field");
      }

    IPAddress addr = (IPAddress) this.value;

    return addr == null ? false : addr.isIPv6();
  }

  /**
   * Returns true if the value stored in the given element of this IP
   * field is an IPV6 address.  If no value is stored in this field,
   * false is returned.
   *
   * @param index Array index for the value to be checked
   *
   * @see arlut.csd.ganymede.rmi.ip_field
   */

  public boolean isIPV6(short index)
  {
    if (!isVector())
      {
        throw new IllegalArgumentException("vector accessor called on scalar field");
      }

    IPAddress addr = (IPAddress) getVectVal().get(index);

    return addr == null ? false : addr.isIPv6();
  }

  // ****
  //
  // Overridable methods for implementing intelligent behavior
  //
  // ****

  @Override public boolean verifyTypeMatch(Object o)
  {
    return o == null || o instanceof IPAddress;
  }

  @Override public ReturnVal verifyNewValue(Object o)
  {
    DBEditObject eObj = (DBEditObject) this.owner;

    if (!isEditable(true))
      {
        return Ganymede.createErrorDialog(this.getGSession(),
                                          "IP Field Error",
                                          "Don't have permission to edit field " + getName() +
                                          " in object " + eObj.getLabel());
      }

    if (!verifyTypeMatch(o))
      {
        return Ganymede.createErrorDialog(this.getGSession(),
                                          "IP Field Error",
                                          "Submitted value " + o + " is not a IP address!  Major client error while" +
                                          " trying to edit field " + getName() +
                                          " in object " + eObj.getLabel());
      }

    // have our parent make the final ok on the value

    return eObj.verifyNewValue(this, o);
  }


  /**
   * Returns a {@link arlut.csd.ganymede.server.fieldDeltaRec fieldDeltaRec}
   * object listing the changes between this field's state and that
   * of the prior oldField state.
   */

  @Override public fieldDeltaRec getVectorDiff(DBField oldField)
  {
    if (!isVector())
      {
        // "Vector method called on a scalar field: {1} in object {0}"
        throw new IllegalArgumentException(ts.l("global.oops_scalar",
                                                this.owner.getLabel(), getName()));
      }

    if (oldField == null)
      {
        // "Can''t compare fields.. oldField is null"
        throw new IllegalArgumentException(ts.l("getVectorDiff.null_old"));
      }

    if ((oldField.getID() != getID()) ||
        (oldField.getObjTypeID() != getObjTypeID()))
      {
        // "Can''t compare fields.. incompatible field ids"
        throw new IllegalArgumentException(ts.l("getVectorDiff.bad_type"));
      }

    /* - */

    fieldDeltaRec deltaRec = new fieldDeltaRec(getID());
    Vector oldValues = oldField.getVectVal();
    Vector newValues = getVectVal();
    Vector addedValues = VectorUtils.difference(newValues, oldValues);
    Vector deletedValues = VectorUtils.difference(oldValues, newValues);

    deltaRec.addValues = addedValues;
    deltaRec.delValues = deletedValues;

    return deltaRec;
  }
}
