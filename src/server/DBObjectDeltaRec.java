/*

   DBObjectDeltaRec.java

   This class is used to represent a record of changes that need to be
   made to a DBObject in the DBStore.
 
   This class will be used in to handle writing and reading records of
   changes made to objects in the Ganymede journal file.
   
   Created: 11 June 1998
   Version: $Revision: 1.7 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import java.util.*;
import java.io.*;
import java.rmi.RemoteException;

/*------------------------------------------------------------------------------
                                                                           class
                                                                DBObjectDeltaRec

------------------------------------------------------------------------------*/

/**
 *
 * This class is used to represent a record of changes that need to be
 * made to a DBObject in the DBStore.<br><br>
 *
 * This class will be used in to handle writing and reading records of
 * changes made to objects in the Ganymede journal file.
 *
 */

public class DBObjectDeltaRec implements FieldType {

  static final boolean debug = false;

  // ---

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

	    fieldRecs.addElement(new fieldDeltaRec(fieldDef.getID(),
						   currentField));

	    continue;
	  }

	// it's a vector.. use the DBField.getVectorDiff() method
	// to generate a vector diff.

	fieldRecs.addElement(currentField.getVectorDiff(origField));
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
    String fieldName = null;
    String status = null;

    /* -- */

    status = "Reading invid";

    try
      {
	invid = new Invid(in);

	baseDef = Ganymede.db.getObjectBase(invid.getType());

	if (debug)
	  {
	    System.err.println(">*> Reading delta rec for " + baseDef.getName() +
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
	    status = "Reading field code for field " + i;

	    fieldcode = in.readShort();

	    fieldDef = (DBObjectBaseField) baseDef.getField(fieldcode);

	    typecode = fieldDef.getType();

	    fieldName = fieldDef.getName();

	    if (debug)
	      {
		System.err.println(">> DBObjectDeltaRec():  Reading delta for field " + 
				   fieldName + ":" + fieldcode);
	      }

	    status = "Reading deletion boolean for field " + i;

	    if (in.readBoolean())
	      {
		// we're deleting this field

		fieldRecs.addElement(new fieldDeltaRec(fieldcode, null));
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

	    if (debug)
	      {
		System.err.println(">> DBObjectDeltaRec(): field " + i + 
				   ", field = " + fieldName);

		if (scalar)
		  {
		    System.err.println(">> DBObjectDeltaRec(): " + fieldName +
				       " is a scalar record");
		  }
		else
		  {
		    System.err.println(">> DBObjectDeltaRec(): field " + 
				       fieldName + " is a vector diff record");
		  }
	      }

	    if (scalar)
	      {
		// ok, need to identify the type and read in a field

		switch (typecode)
		  {
		  case BOOLEAN:
		    status = "Reading boolean field (" + fieldName + ":" + fieldcode + ") for field " + i;

		    if (debug)
		      {
			System.err.println(status);
		      }

		    fieldRecs.addElement(new fieldDeltaRec(fieldcode, 
							   new BooleanDBField(null, in, fieldDef)));
		    break;

		  case NUMERIC:
		    status = "Reading numeric field (" + fieldName + ":" + fieldcode + ") for field " + i;

		    if (debug)
		      {
			System.err.println(status);
		      }

		    fieldRecs.addElement(new fieldDeltaRec(fieldcode, 
							   new NumericDBField(null, in, fieldDef)));
		    break;

		  case DATE:
		    status = "Reading date field (" + fieldName + ":" + fieldcode + ") for field " + i;

		    if (debug)
		      {
			System.err.println(status);
		      }

		    fieldRecs.addElement(new fieldDeltaRec(fieldcode, 
							   new DateDBField(null, in, fieldDef)));
		    break;

		  case STRING:
		    status = "Reading string field (" + fieldName + ":" + fieldcode + ") for field " + i;

		    if (debug)
		      {
			System.err.println(status);
		      }

		    fieldRecs.addElement(new fieldDeltaRec(fieldcode, 
							   new StringDBField(null, in, fieldDef)));
		    break;

		  case INVID:
		    status = "Reading invid field (" + fieldName + ":" + fieldcode + ") for field " + i;

		    if (debug)
		      {
			System.err.println(status);
		      }

		    fieldRecs.addElement(new fieldDeltaRec(fieldcode, 
							   new InvidDBField(null, in, fieldDef)));
		    break;

		  case PERMISSIONMATRIX:
		    status = "Reading perm field (" + fieldName + ":" + fieldcode + ") for field " + i;

		    if (debug)
		      {
			System.err.println(status);
		      }

		    fieldRecs.addElement(new fieldDeltaRec(fieldcode, 
							   new PermissionMatrixDBField(null, in, fieldDef)));
		    break;

		  case PASSWORD:
		    status = "Reading password field (" + fieldName + ":" + fieldcode + ") for field " + i;

		    if (debug)
		      {
			System.err.println(status);
		      }

		    fieldRecs.addElement(new fieldDeltaRec(fieldcode, 
							   new PasswordDBField(null, in, fieldDef)));
		    break;

		  case IP:
		    status = "Reading IP field (" + fieldName + ":" + fieldcode + ") for field " + i;

		    if (debug)
		      {
			System.err.println(status);
		      }

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
			
			value = new Invid(in);
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
			    bytes[k] = new Byte(in.readByte());
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
			value = new Invid(in);
			break;

		      case IP:

			status = "Reading IP deletion " + j + " for field " + i + ":" + fieldName;

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
    catch (IOException ex)
      {
	System.err.println("DBObjectDeltaRec constructor: IOException in state " + status);
	ex.printStackTrace();

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

    fieldDeltaRec fdRec;
    Object value;

    for (int i = 0; i < fieldRecs.size(); i++)
      {
	fdRec = (fieldDeltaRec) fieldRecs.elementAt(i);

	// write out our field code.. this will be used by the loader
	// code to determine what kind of field this is, and what
	// kind of data types need to be loaded.

	if (debug)
	  {
	    System.err.println("Emitting field " + fdRec.fieldcode + " for invid " + invid.toString());
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
	    else if (value instanceof IPwrap)
	      {
		Byte[] bytes = ((IPwrap) value).address;

		out.writeByte(bytes.length);

		for (int k = 0; k < bytes.length; k++)
		  {
		    out.writeByte(bytes[k].byteValue());
		  }
	      }
	    else
	      {
		Ganymede.debug("DBObjectDeltaRec.emit(): Error!  Unrecognized element in vector!");
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
	    else if (value instanceof IPwrap)
	      {
		Byte[] bytes = ((IPwrap) value).address;

		out.writeByte(bytes.length);

		for (int k = 0; k < bytes.length; k++)
		  {
		    out.writeByte(bytes[k].byteValue());
		  }
	      }
	  }
      }
  }

  /**
   *
   * This method takes an object in its original state, and returns a
   * new copy of the object with the changes emboddied in this
   * DBObjectDeltaRec applied to it.
   *
   */

  public DBObject applyDelta(DBObject original)
  {
    if (!original.getInvid().equals(invid))
      {
	throw new IllegalArgumentException("Error, object identity mismatch");
      }

    /* - */

    DBObject copy;
    fieldDeltaRec fieldRec;
    Object value;

    /* -- */

    try
      {
	copy = new DBObject(original, null);
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("Error, RMI system failure: " + ex.getMessage());
      }

    // now process the fieldDeltaRec's.

    for (int i = 0; i < fieldRecs.size(); i++)
      {
	fieldRec = (fieldDeltaRec) fieldRecs.elementAt(i);

	// are we clearing this field?

	if (!fieldRec.vector && fieldRec.scalarValue == null)
	  {
	    copy.fields.removeNoSync(fieldRec.fieldcode);

	    continue;
	  }

	// are we doing a replace?

	if (!fieldRec.vector)
	  {
	    fieldRec.scalarValue.setOwner(copy);
	    copy.fields.putNoSync(fieldRec.scalarValue);

	    continue;
	  }

	// ok, we must be doing a vector mod

	// remember that we are intentionally bypassing the DBField's
	// add/remove logic, as we assume that this DBObjectDeltaRec
	// was generated as a result of a properly checked operation.

	// Also, since we know that only string, invid, and IP fields
	// can be vectors, we don't have to worry about the password
	// and permission matrix special-case logic.

	DBField field = (DBField) copy.getField(fieldRec.fieldcode);

	for (int j = 0; j < fieldRec.addValues.size(); j++)
	  {
	    value = fieldRec.addValues.elementAt(j);

	    if (value instanceof IPwrap)
	      {
		value = ((IPwrap) value).address;
	      }

	    field.values.addElement(value);
	  }

	for (int j = 0; j < fieldRec.delValues.size(); j++)
	  {
	    value = fieldRec.delValues.elementAt(j);

	    if (value instanceof IPwrap)
	      {
		value = ((IPwrap) value).address;
	      }

	    field.values.removeElement(value);
	  }
      }

    return copy;
  }
}
