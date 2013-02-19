/*

   DBObjectDeltaRec.java

   This class is used to represent a record of changes that need to be
   made to a DBObject in the DBStore.

   This class will be used in to handle writing and reading records of
   changes made to objects in the Ganymede journal file.

   Created: 11 June 1998

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

package arlut.csd.ganymede.server;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Vector;

import arlut.csd.ganymede.common.FieldType;
import arlut.csd.ganymede.common.Invid;

/*------------------------------------------------------------------------------
                                                                           class
                                                                DBObjectDeltaRec

------------------------------------------------------------------------------*/

/**
 * <p>This class is used to represent a record of changes that need to
 * be made to a DBObject in the DBStore.</p>
 *
 * <p>This class will be used in to handle writing and reading records
 * of changes made to objects in the Ganymede journal file.</p>
 */

public final class DBObjectDeltaRec implements FieldType {

  private Invid invid = null;
  private Vector<fieldDeltaRec> fieldRecs = new Vector<fieldDeltaRec>();

  /* -- */

  /**
   * <p>This DBObjectDeltaRec constructor is used to generate a delta
   * record that records the difference between two objects for the
   * Ganymede journal</p>
   */

  public DBObjectDeltaRec(DBObject oldObj, DBObject newObj)
  {
    if (oldObj == null || newObj == null)
      {
        throw new IllegalArgumentException("Got a null object parameter" +
                                           ((oldObj == null) ? " old " : "") +
                                           ((newObj == null) ? " new " : ""));
      }

    if (!oldObj.getInvid().equals(newObj.getInvid()))
      {
        throw new IllegalArgumentException("Old and New object id's don't match");
      }

    this.invid = oldObj.getInvid();
    DBObjectBase objectBase = oldObj.getBase();

    // algorithm: iterate over base.getFieldsInFieldOrder() to find
    // all fields possibly contained in the object.. for each field,
    // check to see if the value has changed.  if so, create a
    // fieldDeltaRec for it.

    // note that we're counting on objectBase.sortedFields not being
    // changed while we're iterating here.. this is an ok assumption,
    // since only the schema editor will trigger changes in
    // sortedFields.

    for (DBObjectBaseField fieldDef: objectBase.getFieldsInFieldOrder())
      {
        DBField origField = (DBField) oldObj.getField(fieldDef.getID());
        DBField currentField = (DBField) newObj.getField(fieldDef.getID());

        if ((origField == null || !origField.isDefined()) &&
            (currentField == null || !currentField.isDefined()))
          {
            // no change.. was null/undefined, still is.

            continue;
          }

        if (currentField == null || !currentField.isDefined())
          {
            // lost this field

            this.fieldRecs.add(new fieldDeltaRec(fieldDef.getID(), null));

            continue;
          }

        if (origField == null || !origField.isDefined())
          {
            // we gained this field

            this.fieldRecs.add(new fieldDeltaRec(fieldDef.getID(), currentField));

            continue;
          }

        if (currentField.equals(origField))
          {
            // no changes, we don't need to write this one out.

            continue;
          }

        // at this point, we know we need to write out a change
        // record..  the only question now is whether it is for a
        // scalar or a vector.

        if (!fieldDef.isArray())
          {
            // got a scalar.. save this field entire to write out
            // when we emit to the journal

            this.fieldRecs.add(new fieldDeltaRec(fieldDef.getID(),
                                                 currentField));

            continue;
          }

        // it's a vector.. use the DBField.getVectorDiff() method
        // to generate a vector diff.

        this.fieldRecs.add(currentField.getVectorDiff(origField));
      }
  }

  /**
   * This DBObjectDeltaRec constructor is used to load a delta record
   * from a Journal stream.
   */

  public DBObjectDeltaRec(DataInput in) throws IOException
  {
    try
      {
        this.invid = Invid.createInvid(in);
        DBObjectBase baseDef = Ganymede.db.getObjectBase(this.invid.getType());

        if (baseDef == null)
          {
            throw new RuntimeException("Unrecognized object type in journal: " +
                                       this.invid.getType());
          }

        DBObject obj = Ganymede.db.viewDBObject(this.invid);

        int fieldcount = in.readInt();

        for (int i = 0; i < fieldcount; i++)
          {
            short fieldcode = in.readShort();
            DBObjectBaseField fieldDef = baseDef.getFieldDef(fieldcode);

            if (fieldDef == null)
              {
                throw new RuntimeException("Unrecognized field identifier in journal " + fieldcode +
                                           " for object type " + baseDef.getName());
              }

            short typecode = fieldDef.getType();
            String fieldName = fieldDef.getName();

            if (in.readBoolean())
              {
                // we're deleting this field

                this.fieldRecs.add(new fieldDeltaRec(fieldcode, null));
                continue;
              }

            if (in.readShort() != typecode)
              {
                throw new RuntimeException("Error, field type mismatch in journal file");
              }

            boolean scalar = in.readBoolean();

            if (scalar)
              {
                this.fieldRecs.add(new fieldDeltaRec(fieldcode, DBField.readField(obj, in, fieldDef)));
              }
            else
              {
                // read in additions and deletions for a vector field

                fieldDeltaRec fieldRec = new fieldDeltaRec(fieldcode);
                Object value = null;

                int size = in.readInt(); // additions

                for (int j = 0; j < size; j++)
                  {
                    // we only support vectors of strings, invids, and
                    // ip addresses

                    switch (typecode)
                      {
                      case STRING:
                        value = in.readUTF();
                        break;

                      case INVID:
                        value = Invid.createInvid(in);
                        break;

                      case IP:
                        byte bytelength = in.readByte();
                        Byte[] bytes = new Byte[bytelength];

                        for (int k = 0; k < bytelength; k++)
                          {
                            bytes[k] = Byte.valueOf(in.readByte());
                          }

                        value = bytes;
                      }

                    fieldRec.addValue(value);
                  }

                size = in.readInt(); // deletions

                for (int j = 0; j < size; j++)
                  {
                    switch (typecode)
                      {
                      case STRING:
                        value = in.readUTF();
                        break;

                      case INVID:
                        value = Invid.createInvid(in);
                        break;

                      case IP:
                        byte bytelength = in.readByte();
                        Byte[] bytes = new Byte[bytelength];

                        for (int k = 0; k < bytelength; k++)
                          {
                            bytes[k] = Byte.valueOf(in.readByte());
                          }

                        value = bytes;
                      }

                    fieldRec.delValue(value);
                  }

                this.fieldRecs.add(fieldRec);
              }
          }
      }
    catch (IOException ex)
      {
        Ganymede.logError(ex);
        throw ex;
      }
  }

  /**
   * This method emits this delta rec to a file
   */

  public void emit(DataOutput out) throws IOException
  {
    DBObjectBase baseDef = Ganymede.db.getObjectBase(this.invid.getType());

    this.invid.emit(out);
    out.writeInt(this.fieldRecs.size());

    for (fieldDeltaRec fdRec: this.fieldRecs)
      {
        // write out our field code.. this will be used by the loader
        // code to determine what kind of field this is, and what
        // kind of data types need to be loaded.

        out.writeShort(fdRec.fieldcode);

        // are we deleting?

        if (!fdRec.vector && fdRec.scalarValue == null)
          {
            out.writeBoolean(true);
            continue;
          }

        // no, we're redefining this field

        out.writeBoolean(false);

        // write out our field type code.. this will be used by the loader
        // to verify that the schema hasn't undergone an incompatible
        // change since the journal was written.

        DBObjectBaseField fieldDef = baseDef.getFieldDef(fdRec.fieldcode);

        out.writeShort(fieldDef.getType());

        if (fdRec.scalarValue != null)
          {
            out.writeBoolean(true); // scalar redefinition
            fdRec.scalarValue.emit(out);

            continue;
          }

        out.writeBoolean(false); // vector mod

        // write out what is being added to this vector

        if (fdRec.addValues == null)
          {
            out.writeInt(0);
          }
        else
          {
            out.writeInt(fdRec.addValues.size());

            for (Object value: fdRec.addValues)
              {
                if (value instanceof String)
                  {
                    out.writeUTF((String) value);
                  }
                else if (value instanceof Invid)
                  {
                    ((Invid) value).emit(out);
                  }
                else if (value instanceof IPwrap)
                  {
                    Byte[] bytes = ((IPwrap) value).address;

                    out.writeByte(bytes.length);

                    for (int i = 0; i < bytes.length; i++)
                      {
                        out.writeByte(bytes[i].byteValue());
                      }
                  }
                else
                  {
                    Ganymede.debug("DBObjectDeltaRec.emit(): Error!  Unrecognized element in vector!");
                  }
              }
          }

        // write out what is being removed from this vector

        if (fdRec.delValues == null)
          {
            out.writeInt(0);
          }
        else
          {
            out.writeInt(fdRec.delValues.size());

            for (Object value: fdRec.delValues)
              {
                if (value instanceof String)
                  {
                    out.writeUTF((String) value);
                  }
                else if (value instanceof Invid)
                  {
                    Invid invid = (Invid) value;

                    out.writeShort(invid.getType());
                    out.writeInt(invid.getNum());
                  }
                else if (value instanceof IPwrap)
                  {
                    Byte[] bytes = ((IPwrap) value).address;

                    out.writeByte(bytes.length);

                    for (int i = 0; i < bytes.length; i++)
                      {
                        out.writeByte(bytes[i].byteValue());
                      }
                  }
              }
          }
      }
  }

  /**
   * <p>This method takes an object in its original state, and returns
   * a new copy of the object with the changes embodied in this
   * DBObjectDeltaRec applied to it.</p>
   */

  public DBObject applyDelta(DBObject original)
  {
    if (!original.getInvid().equals(this.invid))
      {
        throw new IllegalArgumentException("Error, object identity mismatch");
      }

    DBObject copy = new DBObject(original, null);

    for (fieldDeltaRec fieldRec: this.fieldRecs)
      {
        if (!fieldRec.vector && fieldRec.scalarValue == null)
          {
            copy.clearField(fieldRec.fieldcode);

            continue;
          }

        if (!fieldRec.vector)
          {
            fieldRec.scalarValue.setOwner(copy);

            if (copy.getField(fieldRec.fieldcode) != null)
              {
                copy.replaceField(fieldRec.scalarValue);
              }
            else
              {
                copy.addField(fieldRec.scalarValue);
              }

            continue;
          }

        // ok, we must be doing a vector mod

        // remember that we are intentionally bypassing the DBField's
        // add/remove logic, as we assume that this DBObjectDeltaRec
        // was generated as a result of a properly checked operation.

        // Also, since we know that only string, invid, and IP fields
        // can be vectors, we don't have to worry about the password
        // and permission matrix special-case logic.

        DBField field = copy.retrieveField(fieldRec.fieldcode);

        if (fieldRec.addValues != null)
          {
            for (Object value: fieldRec.addValues)
              {
                if (value instanceof IPwrap)
                  {
                    value = ((IPwrap) value).address;
                  }

                field.getVectVal().add(value);
              }
          }

        if (fieldRec.delValues != null)
          {
            for (Object value: fieldRec.delValues)
              {
                if (value instanceof IPwrap)
                  {
                    value = ((IPwrap) value).address;
                  }

                field.getVectVal().remove(value);
              }
          }
      }

    return copy;
  }

  public Invid getInvid()
  {
    return this.invid;
  }

  public String toString()
  {
    StringBuilder buf = new StringBuilder();

    buf.append("DBObjectDeltaRec: invid ");
    buf.append(this.invid);
    buf.append("\n");

    for (fieldDeltaRec fr: this.fieldRecs)
      {
        buf.append(fr.toString());
        buf.append("\n");
      }

    return buf.toString();
  }
}
