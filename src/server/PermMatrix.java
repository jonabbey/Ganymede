/*

   PermMatrix.java

   This class stores a read-only matrix of PermEntry bits, organized by
   object type and field id's.
   
   Created: 3 October 1997
   Version: $Revision: 1.9 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import java.util.*;
import java.rmi.*;
import java.io.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                      PermMatrix

------------------------------------------------------------------------------*/

/**
 *
 * This class stores a read-only matrix of PermEntry bits, organized by
 * object type and field id's.
 *
 */

public class PermMatrix implements java.io.Serializable {

  static final boolean debug = false;

  static final long serialVersionUID = 7354985227082627640L;

  /**
   *
   * This utility method extracts the DBObjectBase name from a coded
   * permission entry held in a PermMatrix/PermissionMatrixDBField
   * Matrix.
   * 
   */

  public static String decodeBaseName(String entry)
  {
    int sepIndex;
    short basenum;
    DBObjectBase base;
    String basename;

    /* -- */

    sepIndex = entry.indexOf(':');

    if (sepIndex != -1)
      {
	try
	  {
	    basenum = new Short(entry.substring(0, sepIndex)).shortValue();
	    base = (DBObjectBase) Ganymede.db.getObjectBase(basenum);

	    if (base == null)
	      {
		basename = "INVALID: " + entry;
	      }
	    else
	      {
		basename = base.getName();
	      }
	  }
	catch (NumberFormatException ex)
	  {
	    basename = entry;
	  }
      }
    else
      {
	basename = entry;
      }

    return basename;
  }

  /**
   *
   * This method extracts the DBObjectBaseField name from a coded
   * permission entry held in a PermMatrix/PermissionMatrixDBField
   * Matrix.
   * 
   */

  public static String decodeFieldName(String entry)
  {
    int sepIndex;
    short basenum;
    DBObjectBase base;
    String basename;
    
    String fieldId;
    short fieldnum;
    DBObjectBaseField field;
    String fieldname;

    /* -- */

    sepIndex = entry.indexOf(':');

    if (sepIndex != -1)
      {
	try
	  {
	    basenum = new Short(entry.substring(0, sepIndex)).shortValue();
	    base = (DBObjectBase) Ganymede.db.getObjectBase(basenum);

	    if (base == null)
	      {
		fieldname = "[error " + entry + "]";
	      }
	    else
	      {
		fieldId = entry.substring(sepIndex+1);
	    
		if (fieldId.equals(":"))
		  {
		    fieldname = "[base]";
		  }
		else if (fieldId.equals("default"))
		  {
		    fieldname = "[Default]";
		  }
		else
		  {
		    try
		      {
			fieldnum = new Short(fieldId).shortValue();

			field = (DBObjectBaseField) base.getField(fieldnum);

			if (field == null)
			  {
			    fieldname = "invalid:" + fieldId;
			  }
			else
			  {
			    fieldname = field.getName();
			  }
		      }
		    catch (NumberFormatException ex)
		      {
			fieldname = fieldId;
		      }
		  }
	      }
	  }
	catch (NumberFormatException ex)
	  {
	    fieldname = "[error " + entry + "]";
	  }
      }
    else
      {
	fieldname = "[error " + entry + "]";
      }

    return fieldname;
  }

  /**
   *
   * This method returns true if the given PermMatrix /
   * PermissionMatrixDBField key refers to a currently valid 
   * DBObjectBase/DBObjectBaseField in the loaded schema.
   *
   */

  public static boolean isValidCode(String entry)
  {
    int sepIndex;
    short basenum;
    DBObjectBase base;
    
    String fieldId;
    short fieldnum;
    DBObjectBaseField field;

    /* -- */

    sepIndex = entry.indexOf(':');

    if (sepIndex == -1)
      {
	return false;
      }

    try
      {
	basenum = new Short(entry.substring(0, sepIndex)).shortValue();
	base = (DBObjectBase) Ganymede.db.getObjectBase(basenum);

	if (base == null)
	  {
	    return false;
	  }
	else
	  {
	    fieldId = entry.substring(sepIndex+1);
	    
	    if (!fieldId.equals(":") && !fieldId.equals("default"))
	      {
		try
		  {
		    fieldnum = new Short(fieldId).shortValue();
		    
		    field = (DBObjectBaseField) base.getField(fieldnum);
		    
		    if (field == null)
		      {
			return false;
		      }
		  }
		catch (NumberFormatException ex)
		  {
		    return false;
		  }
	      }
	  }
      }
    catch (NumberFormatException ex)
      {
	return false;
      }

    return true;
  }

  // ---

  private Hashtable matrix;

  /* -- */

  public PermMatrix()
  {
    matrix = new Hashtable();
  }

  public PermMatrix(PermissionMatrixDBField field)
  {
    if (field.matrix != null)
      {
	this.matrix = (Hashtable) field.matrix.clone();
      }
    else
      {
	this.matrix = new Hashtable();
      }
  }

  public PermMatrix(PermMatrix orig)
  {
    this.matrix = (Hashtable) orig.matrix.clone();
  }

  /**
   *
   * This method combines this PermMatrix with that
   * of orig.  The returned PermMatrix will allow
   * any access either of the source PermMatrices
   * would.
   *
   */

  public PermMatrix union(PermissionMatrixDBField orig)
  {
    return union(new PermMatrix(orig));	// this will cause a redundant copy, but who cares?
  }

  /**
   *
   * This method combines this PermMatrix with that
   * of orig.  The returned PermMatrix will allow
   * any access either of the source PermMatrices
   * would.
   *
   */

  public synchronized PermMatrix union(PermMatrix orig)
  {
    PermMatrix result;
    Enumeration enum;
    PermEntry entry1, entry2, entry3;
    Object key;

    /* -- */

    if (orig == null)
      {
	return new PermMatrix(this);
      }

    result = new PermMatrix(orig);

    // now go through our matrix and for any entries in this.matrix,
    // put the union of that and the matching entry already in result
    // into result.

    enum = this.matrix.keys();

    while (enum.hasMoreElements())
      {
	key = enum.nextElement();

	entry1 = (PermEntry) this.matrix.get(key);
	entry2 = (PermEntry) result.matrix.get(key);

	if (entry2 != null)
	  {
	    result.matrix.put(key, entry1.union(entry2));
	  }
	else
	  {
	    if (!isBasePerm((String)key))
	      {
		// We are union'ing a field entry.. since orig doesn't
		// contain an explicit record for this field while our
		// matrix does, see if we can find a record for the
		// containing base in field and union that with this entry..
		// this will serve to maintain the permission inheritance
		// issues

		entry3 = (PermEntry) result.matrix.get(baseEntry((String)key));

		if (entry3 == null)
		  {
		    // there is no entry for this field's base.. put in
		    // the original from our matrix

		    result.matrix.put(key, entry1);
		  }
		else
		  {
		    // there is an entry for this field's base.. put it in
		    // so that we properly handle permission inheritance

		    result.matrix.put(key, entry1.union(entry3));
		  }
	      }
	    else
	      {
		result.matrix.put(key, entry1);
	      }
	  }
      }

    // result now contains all of the records from orig, with all of
    // the records from this.matrix union'ed in.  The only problem now
    // is that it's possible that orig contained field records that
    // this.matrix didn't have a match for, which means that we should
    // have unioned in the corresponding base record from this.matrix
    // at that point, but we didn't since we were looping over the
    // entries in this.matrix rather than in orig.

    enum = result.matrix.keys();

    while (enum.hasMoreElements())
      {
	key = enum.nextElement();

	entry1 = (PermEntry) result.matrix.get(key);
	entry2 = (PermEntry) this.matrix.get(key);

	if (entry2 != null)
	  {
	    continue; // we already took care of this above
	  }
	else
	  {
	    if (!isBasePerm((String)key))
	      {
		// This is the case we are concerned with.. the result
		// matrix has a field entry that we don't have a match
		// for in this.matrix.. we need to check to see
		// if we have a base entry for the corresponding base
		// that we need to union in to reflect the default base
		// -> field inheritance

		entry3 = (PermEntry) this.matrix.get(baseEntry((String)key));

		if (entry3 == null)
		  {
		    continue;	// no inheritance to be had here
		  }
		else
		  {
		    // there is an entry for this field's base.. put it in
		    // so that we properly handle permission inheritance

		    result.matrix.put(key, entry1.union(entry3));
		  }
	      }
	  }
      }

    return result;
  }

  /**
   *
   * Returns a PermEntry object representing this PermMatrix's 
   * permissions on the field &lt;fieldID&gt; in base &lt;baseID&gt;<br><br>
   *
   * If there is no entry in this PermMatrix for the given field, the
   * default will be returned, if any has been set.
   *
   * @see arlut.csd.ganymede.PermMatrix 
   */

  public PermEntry getPerm(short baseID, short fieldID)
  {
    PermEntry result;

    result = (PermEntry) matrix.get(matrixEntry(baseID, fieldID));

    if (result == null)
      {
	result = (PermEntry) matrix.get(matrixEntry(baseID, "default"));
      }

    return result;
  }

  /**
   *
   * Returns a PermEntry object representing this PermMatrix's 
   * permissions on the base &lt;baseID&gt;
   *
   * @see arlut.csd.ganymede.PermMatrix
   */

  public PermEntry getPerm(short baseID)
  {
    PermEntry result;

    /* -- */

    result = (PermEntry) matrix.get(matrixEntry(baseID));

    return result;
  }

  /**
   *
   * Returns a PermEntry object representing this PermMatrix's 
   * permissions on the field <field> in base <base>
   *
   * @see arlut.csd.ganymede.PermMatrix
   */

  public PermEntry getPerm(Base base, BaseField field)
  {
    try
      {
	return getPerm(base.getTypeID(), field.getID());
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("caught remote: " + ex);
      }
  }

  /**
   *
   * Returns a PermEntry object representing this PermMatrix's 
   * permissions on the base &lt;base&gt;
   *
   * @see arlut.csd.ganymede.PermMatrix
   */

  public PermEntry getPerm(Base base)
  {
    PermEntry result;

    /* -- */

    try
      {
	result = (PermEntry) matrix.get(matrixEntry(base.getTypeID()));
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("caught remote: " + ex);
      }

    return result;
  }

  public void debugdump()
  {
    Enumeration enum;
    String key;
    PermEntry entry;
    short basenum;
    String basename;
    Hashtable baseHash = new Hashtable();
    Vector vec;

    /* -- */

    System.err.println("PermMatrix DebugDump");

    enum = matrix.keys();

    while (enum.hasMoreElements())
      {
	key = (String) enum.nextElement();

	entry = (PermEntry) matrix.get(key);

	basename = decodeBaseName(key);

	if (baseHash.containsKey(basename))
	  {
	    vec = (Vector) baseHash.get(basename);
	  }
	else
	  {
	    vec = new Vector();
	    baseHash.put(basename, vec);
	  }

	vec.addElement(decodeFieldName(key) + " " + entry.toString());
      }

    enum = baseHash.keys();

    while (enum.hasMoreElements())
      {
	key = (String) enum.nextElement();

	System.err.println("Base - " + key);

	vec = (Vector) baseHash.get(key);

	for (int i = 0; i < vec.size(); i++)
	  {
	    System.err.println("\t" + vec.elementAt(i));
	  }
      }
  }


  private String matrixEntry(short baseID, short fieldID)
  {
    return (baseID + ":" + fieldID);
  }

  private String matrixEntry(short baseID, String text)
  {
    return (baseID + ":" + text);
  }
  
  private String matrixEntry(short baseID)
  {
    return (baseID + "::");
  }

  private boolean isBasePerm(String matrixEntry)
  {
    return (matrixEntry.indexOf(':') != matrixEntry.lastIndexOf(':'));
  }

  private short entryBase(String matrixEntry)
  {
    if (matrixEntry.indexOf(':') == -1)
      {
	throw new IllegalArgumentException("not a valid matrixEntry");
      }

    String baseStr = matrixEntry.substring(0, matrixEntry.indexOf(':'));

    try
      {
	return Short.parseShort(baseStr);
      }
    catch (NumberFormatException ex)
      {
	throw new RuntimeException("bad string format:" + ex);
      }
  }
  
  private short entryField(String matrixEntry)
  {
    if (matrixEntry.indexOf(':') == -1)
      {
	throw new IllegalArgumentException("not a valid matrixEntry");
      }

    if (isBasePerm(matrixEntry))
      {
	throw new IllegalArgumentException("not a field matrixEntry");
      }

    String fieldStr = matrixEntry.substring(matrixEntry.lastIndexOf(':')+1);

    try
      {
	return Short.parseShort(fieldStr);
      }
    catch (NumberFormatException ex)
      {
	throw new RuntimeException("bad string format:" + ex);
      }
  }

  private String baseEntry(String matrixEntry)
  {
    if (isBasePerm(matrixEntry))
      {
	return matrixEntry;
      }
    else
      {
	return matrixEntry(entryBase(matrixEntry));
      }
  }
}
