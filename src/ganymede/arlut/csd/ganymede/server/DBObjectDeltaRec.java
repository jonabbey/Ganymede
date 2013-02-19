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

  static final boolean debug = false;

  // ---

  Invid invid = null;
  Vector<fieldDeltaRec> fieldRecs = new Vector<fieldDeltaRec>(); // changes

  /* -- */

  /**
   * This DBObjectDeltaRec constructor is used to generate a delta record
   * that records the difference between two objects for the Ganymede journal
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

    /* - */

    this.invid = oldObj.getInvid();

    DBObjectBase objectBase = oldObj.getBase();
    DBField origField, currentField;

    /* -- */

    // algorithm: iterate over base.getFieldsInFieldOrder() to find
    // all fields possibly contained in the object.. for each field,
    // check to see if the value has changed.  if so, create a
    // fieldDeltaRec for it.

    // note that we're counting on objectBase.sortedFields not being
    // changed while we're iterating here.. this is an ok assumption,
    // since only the loader and the schema editor will trigger changes
    // in sortedFields.

    if (debug)
      {
        System.err.println("Entering deltarec creation for objects " +
                           oldObj.getLabel() + " and " + newObj.getLabel());
      }

    for (DBObjectBaseField fieldDef: objectBase.getFieldsInFieldOrder())
      {
        if (debug)
          {
            System.err.println("Comparing field " + fieldDef.getName());
          }

        origField = (DBField) oldObj.getField(fieldDef.getID());
        currentField = (DBField) newObj.getField(fieldDef.getID());

        if ((origField == null || !origField.isDefined()) &&
            (currentField == null || !currentField.isDefined()))
          {
            // no change.. was null/undefined, still is.

            continue;
          }

        if (currentField == null || !currentField.isDefined())
          {
            // lost this field

            fieldRecs.add(new fieldDeltaRec(fieldDef.getID(), null));

            continue;
          }

        if (origField == null || !origField.isDefined())
          {
            // we gained this field

            fieldRecs.add(new fieldDeltaRec(fieldDef.getID(), currentField));

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

            fieldRecs.add(new fieldDeltaRec(fieldDef.getID(),
                                            currentField));

            continue;
          }

        // it's a vector.. use the DBField.getVectorDiff() method
        // to generate a vector diff.

        fieldRecs.add(currentField.getVectorDiff(origField));
      }
  }

  /**
   *
   * This DBObjectDeltaRec constructor is used to load a delta record
   * from a Journal stream.
   *
   */

  public DBObjectDeltaRec(DataInput in) throws IOException
  {
    short fieldcode;
    short typecode;
    boolean scalar;
    DBObjectBase baseDef;
    DBObjectBaseField fieldDef;
    String fieldName = null;
    String status = null;
    DBObject obj = null;
    /* -- */

    status = "Reading invid";

    boolean debug = true;

    try
      {
        invid = Invid.createInvid(in);
        baseDef = Ganymede.db.getObjectBase(invid.getType());
        obj = Ganymede.db.viewDBObject(invid);

        if (debug)
          {
            System.err.println("\n>*> Reading delta rec for " + baseDef.getName() +
                               " <" + invid.getNum() + ">");
          }

        status = "Reading field count";
        int fieldcount = in.readInt();

        if (debug)
          {
            System.err.println(">> DBObjectDeltaRec(): " + fieldcount +
                               " fields in on-disk delta rec.");
          }

        for (int i = 0; i < fieldcount; i++)
          {
            status = "\nReading field code for field " + i;
            if (debug)
              {
                System.err.println(status);
              }
            fieldcode = in.readShort();
            fieldDef = (DBObjectBaseField) baseDef.getField(fieldcode);
            typecode = fieldDef.getType();
            fieldName = fieldDef.getName();
            status = "Reading deletion boolean for field " + i;
            if (in.readBoolean())
              {
                // we're deleting this field
                if (debug)
                  {
                    System.err.println("Reading field deletion record field (" + fieldName + ":" + fieldcode + ") for field " + i);
                  }

                fieldRecs.add(new fieldDeltaRec(fieldcode, null));
                continue;
              }

            // okay, we've got a field redefinition.. check the type
            // code to make sure we don't have an incompatible journal
            // entry
            status = "Reading type code for field " + i;
            if (in.readShort() != typecode)
              {
                throw new RuntimeException("Error, field type mismatch in journal file");
              }

            // ok.. now, is it a total redefinition, or a vector delta record?
            status = "Reading scalar/vector delta boolean for field " + i;
            scalar = in.readBoolean();

            if (scalar)
              {
                fieldDeltaRec f_r = null;
                status = "Reading field (" + fieldName + ":" + fieldcode +") for field " + i;
                if (debug)
                  {
                    System.err.println(status);
                  }

                f_r = new fieldDeltaRec(fieldcode, DBField.readField(obj, in, fieldDef));
                fieldRecs.add(f_r);

                if (debug)
                  {
                    System.err.println("Value: " + f_r.toString());
                  }
              }
            else
              {
                // ok, we have a vector delta chunk
                fieldDeltaRec fieldRec = new fieldDeltaRec(fieldcode);
                Object value = null;

                // read in the additions
                status = "Reading vector addition count for field " + fieldName + "(" + i + ")";
                if (debug)
                  {
                    System.err.println(status);
                  }

                int size = in.readInt();

                if (debug)
                  {
                    System.err.println(">> DBObjectDeltaRec(): reading " + size + " additions.");
                  }

                for (int j = 0; j < size; j++)
                  {
                    // we only support 3 vector field types
                    switch (typecode)
                      {
                      case STRING:
                        status = "Reading string addition " + j + " for field " + i + ":" + fieldName;
                        if (debug)
                          {
                            System.err.println(status);
                          }
                        value = in.readUTF();
                        break;

                      case INVID:
                        status = "Reading invid addition " + j + " for field " + i + ":" + fieldName;
                        if (debug)
                          {
                            System.err.println(status);
                          }
                        value = Invid.createInvid(in);
                        break;

                      case IP:
                        status = "Reading ip addition " + j + " for field " + i + ":" + fieldName;
                        if (debug)
                          {
                            System.err.println(status);
                          }
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

                // read in the deletions
                status = "Reading vector deletion count for field " + i;
                if (debug)
                  {
                    System.err.println(status);
                  }

                size = in.readInt();
                if (debug)
                  {
                    System.err.println(">> DBObjectDeltaRec(): reading " + size + " deletions.");
                  }

                for (int j = 0; j < size; j++)
                  {
                    // we only support 3 vector field types

                    switch (typecode)
                      {
                      case STRING:
                        status = "Reading string deletion " + j + " for field " + i + ":" + fieldName;
                        value = in.readUTF();
                        break;

                      case INVID:
                        status = "Reading invid deletion " + j + " for field " + i + ":" + fieldName;
                        value = Invid.createInvid(in);
                        break;

                      case IP:
                        status = "Reading IP deletion " + j + " for field " + i + ":" + fieldName;
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
                // and save this field
                fieldRecs.add(fieldRec);
              }
          }
      }
    catch (IOException ex)
      {
        System.err.println("DBObjectDeltaRec constructor: IOException in state " + status);
        Ganymede.logError(ex);
        throw ex;
      }
  }

  /**
   *
   * This method emits this delta rec to a file
   *
   */

  public void emit(DataOutput out) throws IOException
  {
    DBObjectBase baseDef;
    DBObjectBaseField fieldDef;

    /* -- */

    // write the object id

    baseDef = Ganymede.db.getObjectBase(invid.getType());

    if (debug)
      {
        System.err.println("Emitting delta rec for invid " + invid.toString());
      }

    invid.emit(out);

    // write the fieldrec count

    out.writeInt(fieldRecs.size());

    if (debug)
      {
        System.err.println("Emitting " + fieldRecs.size() + " field records for invid " + invid.toString());
      }

    // now let's write out the fields

    for (fieldDeltaRec fdRec: fieldRecs)
      {
        // write out our field code.. this will be used by the loader
        // code to determine what kind of field this is, and what
        // kind of data types need to be loaded.

        if (debug)
          {
            System.err.println("Emitting fieldDeltaRec:\n\t" + fdRec.toString());
          }

        out.writeShort(fdRec.fieldcode);

        // are we deleting?

        if (!fdRec.vector && fdRec.scalarValue == null)
          {
            // yes, we're deleting this field
            out.writeBoolean(true);
            continue;
          }

        // no, we're redefining this field

        out.writeBoolean(false);

        // write out our field type code.. this will be used by the loader
        // to verify that the schema hasn't undergone an incompatible
        // change since the journal was written.

        fieldDef = (DBObjectBaseField) baseDef.getField(fdRec.fieldcode);

        out.writeShort(fieldDef.getType());

        if (fdRec.scalarValue != null)
          {
            out.writeBoolean(true); // scalar redefinition
            fdRec.scalarValue.emit(out);

            continue;
          }

        out.writeBoolean(false); // vector mod

        // write out what is being added to this vector

        if (debug)
          {
            System.err.println("====== Emitting " + fdRec.addValues.size() + " addition elements");
          }

        if (fdRec.addValues == null)
          {
            out.writeInt(0);
          }
        else
          {
            out.writeInt(fdRec.addValues.size());

            for (Object value: fdRec.addValues)
              {
                // we only support 3 vector field types

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
                // we only support 3 vector field types

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
   * This method takes an object in its original state, and returns a
   * new copy of the object with the changes embodied in this
   * DBObjectDeltaRec applied to it.
   */

  public DBObject applyDelta(DBObject original)
  {
    if (!original.getInvid().equals(invid))
      {
        throw new IllegalArgumentException("Error, object identity mismatch");
      }

    DBObject copy = new DBObject(original, null);

    // now process the fieldDeltaRec's.

    for (fieldDeltaRec fieldRec: fieldRecs)
      {
        // are we clearing this field?

        if (!fieldRec.vector && fieldRec.scalarValue == null)
          {
            copy.clearField(fieldRec.fieldcode);

            continue;
          }

        // are we doing a replace or add?

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

  public String toString()
  {
    StringBuilder buf = new StringBuilder();

    buf.append("DBObjectDeltaRec: invid ");
    buf.append(invid);
    buf.append("\n");

    for (fieldDeltaRec fr: fieldRecs)
      {
        buf.append(fr.toString());
        buf.append("\n");
      }

    return buf.toString();
  }
}
