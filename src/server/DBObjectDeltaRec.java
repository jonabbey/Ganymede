/*

   DBObjectDeltaRec.java

   This class is used to represent a record of changes that need to be
   made to a DBObject in the DBStore.
 
   This class will be used in two separate contexts in the Ganymede
   server.  In the first case, this class will be used to handle writing
   and reading records of changes made to objects in the Ganymede journal
   file.  In the second case, this class will be used to keep track of
   changes made to vector fields in a non-exclusive check-out context.
   
   Created: 11 June 1998
   Version: $Revision: 1.1 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import java.util.*;
import java.io.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                DBObjectDeltaRec

------------------------------------------------------------------------------*/

/**
 *
 * This class is used to represent a record of changes that need to be
 * made to a DBObject in the DBStore.<br><br>
 *
 * This class will be used in two separate contexts in the Ganymede
 * server.  In the first case, this class will be used to handle writing
 * and reading records of changes made to objects in the Ganymede journal
 * file.  In the second case, this class will be used to keep track of
 * changes made to vector fields in a non-exclusive check-out context.
 *
 */

public class DBObjectDeltaRec implements FieldType {

  static final boolean debug = true;

  // ---

  DBSession owner = null;	// will be null if this is a journal rec

  Invid invid = null;
  Vector fieldRecs = new Vector(); // changes

  /* -- */

  /**
   *
   * This DBObjectDeltaRec constructor is used to generate a delta record
   * that records the difference between two objects for the Ganymede journal
   *
   *
   */

  public DBObjectDeltaRec(DBObject oldObj, DBObject newObj)
  {
    if (oldObj == null || newObj == null)
      {
	throw new IllegalArgumentException("Got a null object parameter " +
					   ((oldObj == null) ? " old " : "") +
					   ((newObj == null) ? " new " : ""));
      }

    if (oldObj.getTypeID() != newObj.getTypeID())
      {
	throw new IllegalArgumentException("Old and New object types don't match");
      }

    /* - */

    this.invid = oldObj.getInvid();

    DBObjectBase objectBase = oldObj.getBase();
    DBObjectBaseField fieldDef;
    DBField origField, currentField;

    /* -- */

    // algorithm: iterate over base.sortedFields to find all fields
    // possibly contained in the object.. for each field, check to
    // see if the value has changed.  if so, create a fieldDeltaRec
    // for it.

    // note that we're counting on objectBase.sortedFields not being
    // changed while we're iterating here.. this is an ok assumption,
    // since only the loader and the schema editor will trigger changes
    // in sortedFields.
    
    if (debug)
      {
	System.err.println("Entering deltarec creation for objects " + 
			   oldObj.getLabel() + " and " + newObj.getLabel());
      }

    Enumeration enum = objectBase.sortedFields.elements();

    while (enum.hasMoreElements())
      {
	fieldDef = (DBObjectBaseField) enum.nextElement();

	if (debug)
	  {
	    System.err.println("Comparing field " + fieldDef.getName());
	  }

	origField = (DBField) oldObj.getField(fieldDef.getID());
	currentField = (DBField) newObj.getField(fieldDef.getID());

	if ((origField == null || !origField.defined) && 
	    (currentField == null || !currentField.defined))
	  {
	    // no change.. was null/undefined, still is.

	    continue;
	  }

	if (currentField == null || !currentField.defined)
	  {
	    // lost this field

	    fieldRecs.addElement(new fieldDeltaRec(fieldDef.getID(), null));

	    continue;
	  }

	if (origField == null || !origField.defined)
	  {
	    // we gained this field

	    fieldRecs.addElement(new fieldDeltaRec(fieldDef.getID(), currentField));

	    continue;
	  }

	if (!fieldDef.isArray())
	  {
	    // got a scalar.. save this field entire to write out
	    // when we emit to the journal

	    fieldRecs.addElement(new fieldDeltaRec(fieldDef.getID(),
						   currentField));

	    continue;
	  }

	// ok.. at this point, we've got a vector and we need to compute
	// what has been added and what has been taken away from the
	// original vector.

	fieldDeltaRec deltaRec = new fieldDeltaRec(fieldDef.getID());

	Vector oldValues = origField.values;
	Vector newValues = currentField.values;
	Object compareValue;

	for (int i = 0; i < oldValues.size(); i++)
	  {
	    compareValue = oldValues.elementAt(i);

	    if (!newValues.contains(compareValue))
	      {
		deltaRec.delValue(compareValue);
	      }
	  }

	for (int i = 0; i < newValues.size(); i++)
	  {
	    compareValue = newValues.elementAt(i);

	    if (!oldValues.contains(compareValue))
	      {
		deltaRec.addValue(compareValue);
	      }
	  }

	fieldRecs.addElement(deltaRec);
      }
  }

  /**
   *
   * This DBObjectDeltaRec constructor is used to load a delta record
   * from a Journal stream.
   *
   *
   */

  public DBObjectDeltaRec(DataInput in) throws IOException
  {
    int num;
    short fieldcode;
    short typecode;
    boolean scalar;
    DBObjectBase baseDef;
    DBObjectBaseField fieldDef;

    /* -- */

    invid = new Invid(in);

    baseDef = Ganymede.db.getObjectBase(invid.getType());

    int fieldcount = in.readInt();

    for (int i = 0; i < fieldcount; i++)
      {
	fieldcode = in.readShort();
	scalar = in.readBoolean();

	fieldDef = (DBObjectBaseField) baseDef.getField(fieldcode);

	if (!scalar && !fieldDef.isArray())
	  {
	    // field deletion

	    fieldRecs.addElement(new fieldDeltaRec(fieldcode, null));
	    continue;
	  }

	typecode = fieldDef.getType();

	if (scalar)
	  {
	    // ok, need to identify the type and read in a field

	    switch (typecode)
	      {
	      case BOOLEAN:
		fieldRecs.addElement(new fieldDeltaRec(fieldcode, 
						       new BooleanDBField(null, in, fieldDef)));
		break;

	      case NUMERIC:
		fieldRecs.addElement(new fieldDeltaRec(fieldcode, 
						       new NumericDBField(null, in, fieldDef)));
		break;

	      case DATE:
		fieldRecs.addElement(new fieldDeltaRec(fieldcode, 
						       new DateDBField(null, in, fieldDef)));
		break;

	      case STRING:
		fieldRecs.addElement(new fieldDeltaRec(fieldcode, 
						       new StringDBField(null, in, fieldDef)));
		break;

	      case INVID:
		fieldRecs.addElement(new fieldDeltaRec(fieldcode, 
						       new InvidDBField(null, in, fieldDef)));
		break;

	      case PERMISSIONMATRIX:
		fieldRecs.addElement(new fieldDeltaRec(fieldcode, 
						       new PermissionMatrixDBField(null, in, fieldDef)));
		break;

	      case PASSWORD:
		fieldRecs.addElement(new fieldDeltaRec(fieldcode, 
						       new PasswordDBField(null, in, fieldDef)));
		break;

	      case IP:
		fieldRecs.addElement(new fieldDeltaRec(fieldcode, 
						       new IPDBField(null, in, fieldDef)));
		break;
	      }
	  }
	else
	  {
	    // ok, we have a vector delta chunk

	    fieldDeltaRec fieldRec = new fieldDeltaRec(fieldcode);
	    Object value = null;

	    // read in the additions

	    int size = in.readInt();

	    for (int j = 0; j < size; j++)
	      {
		// we only support 3 vector field types

		switch (typecode)
		  {
		  case STRING:
		    value = in.readUTF();
		    break;

		  case INVID:
		    value = new Invid(in);
		    break;

		  case IP:

		    byte bytelength = in.readByte();

		    Byte[] bytes = new Byte[bytelength];
		    
		    for (int k = 0; k < bytelength; k++)
		      {
			bytes[k] = new Byte(in.readByte());
		      }

		    value = bytes;
		  }
		
		fieldRec.addValue(value);
	      }

	    // read in the deletions

	    size = in.readInt();

	    for (int j = 0; j < size; j++)
	      {
		// we only support 3 vector field types

		switch (typecode)
		  {
		  case STRING:
		    value = in.readUTF();
		    break;

		  case INVID:
		    value = new Invid(in);
		    break;

		  case IP:

		    byte bytelength = in.readByte();

		    Byte[] bytes = new Byte[bytelength];
		    
		    for (int k = 0; k < bytelength; k++)
		      {
			bytes[k] = new Byte(in.readByte());
		      }

		    value = bytes;
		  }
		
		fieldRec.delValue(value);
	      }

	    // and save this field

	    fieldRecs.addElement(fieldRec);
	  }
      }
  }

  /**
   *
   * This DBObjectDeltaRec constructor is used to initialize a delta record
   * for a non-exclusive vector field edit.
   *
   */

  public DBObjectDeltaRec(DBSession owner, DBObject original)
  {
    this.owner = owner;
  }

  /**
   *
   * This method emits this delta rec to a file
   *
   */

  public void emit(DataOutput out) throws IOException
  {
    // write the object id

    invid.emit(out);

    // write the fieldrec count

    out.writeInt(fieldRecs.size());
    
    // now let's write out the fields

    fieldDeltaRec fdRec;
    Object value;

    for (int i = 0; i < fieldRecs.size(); i++)
      {
	fdRec = (fieldDeltaRec) fieldRecs.elementAt(i);

	// write out our field code.. this will be used by the loader
	// code to determine what kind of field this is, and what
	// kind of data types need to be loaded.

	out.writeShort(fdRec.fieldcode);
	out.writeBoolean(fdRec.scalarValue != null);

	if (fdRec.scalarValue != null)
	  {
	    fdRec.scalarValue.emit(out);

	    continue;
	  }

	if (fdRec.vector)
	  {
	    // write out what is being added to this vector

	    out.writeInt(fdRec.addValues.size());

	    for (int j = 0; j < fdRec.addValues.size(); j++)
	      {
		value = fdRec.addValues.elementAt(j);

		// we only support 3 vector field types

		if (value instanceof String)
		  {
		    out.writeUTF((String) value);
		  }
		else if (value instanceof Invid)
		  {
		    ((Invid) value).emit(out);
		  }
		else if (value instanceof Byte[])
		  {
		    Byte[] bytes = (Byte[]) value;

		    out.writeByte(bytes.length);

		    for (int k = 0; k < bytes.length; k++)
		      {
			out.writeByte(bytes[k].byteValue());
		      }
		  }
	      }

	    // write out what is being removed from this vector

	    out.writeInt(fdRec.delValues.size());

	    for (int j = 0; j < fdRec.delValues.size(); j++)
	      {
		value = fdRec.delValues.elementAt(j);

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
		else if (value instanceof Byte[])
		  {
		    Byte[] bytes = (Byte[]) value;

		    out.writeByte(bytes.length);

		    for (int k = 0; k < bytes.length; k++)
		      {
			out.writeByte(bytes[k].byteValue());
		      }
		  }
	      }
	  }
      }
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                                   fieldDeltaRec

------------------------------------------------------------------------------*/

class fieldDeltaRec {

  short fieldcode;
  boolean vector;
  DBField scalarValue = null;
  Vector addValues = null;
  Vector delValues = null;

  /* -- */

  /**
   *
   * Scalar value constructor
   *
   */

  fieldDeltaRec(short fieldcode, DBField scalar)
  {
    this.fieldcode = fieldcode;
    this.scalarValue = scalar;
    vector = false;
  }

  /**
   *
   * Vector constructor
   *
   */

  fieldDeltaRec(short fieldcode)
  {
    this.fieldcode = fieldcode;
    vector = true;
    addValues = new Vector();
    delValues = new Vector();
  }

  void addValue(Object value)
  {
    if (delValues.contains(value))
      {
	delValues.removeElement(value);
      }
    else if (!addValues.contains(value))
      {
	addValues.addElement(value);
      }

    return;
  }

  void delValue(Object value)
  {
    if (addValues.contains(value))
      {
	addValues.removeElement(value);
      }
    else if (!delValues.contains(value))
      {
	delValues.addElement(value);
      }

    return;
  }

}
