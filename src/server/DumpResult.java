/*

   DumpResult.java

   This class is a serializable dump result object, which conveys
   results from a dump operation along with methods that can be
   used to extract the results  out of the dump.
   
   Created: 25 September 1997
   Version: $Revision: 1.2 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import java.util.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                      DumpResult

------------------------------------------------------------------------------*/

public class DumpResult implements java.io.Serializable {

  static final boolean debug = false;

  // --

  StringBuffer buffer;
  private boolean unpacked = false;

  // for use pre-serialized

  transient Vector fieldDefs = null;

  // for use post-serialized

  transient Vector headers = null;
  transient Vector types = null;
  transient Vector invids = null;
  transient Vector rows = null;

  /* -- */

  public DumpResult(Vector fieldDefs)
  {
    DBObjectBaseField field;
    char[] chars;
    Vector typeList = new Vector();

    /* -- */

    this.fieldDefs = fieldDefs;
    buffer = new StringBuffer();

    for (int i = 0; i < fieldDefs.size(); i++)
      {
	field = (DBObjectBaseField) fieldDefs.elementAt(i);

	// need to also check here for permission restrictions on 
	// field visibility

	chars = field.getName().toCharArray();
	
	for (int j = 0; j < chars.length; j++)
	  {
	    if (chars[j] == '|')
	      {
		buffer.append("\\|");
	      }
	    else if (chars[j] == '\n')
	      {
		buffer.append("\\\n");
	      }
	    else if (chars[j] == '\\')
	      {
		buffer.append("\\\\");
	      }
	    else
	      {
		buffer.append(chars[j]);
	      }
	  }
	
	buffer.append("|");
      }

    buffer.append("\n");

    for (int i = 0; i < fieldDefs.size(); i++)
      {
	field = (DBObjectBaseField) fieldDefs.elementAt(i);
	buffer.append(field.getType());
	buffer.append("|");
      }

    buffer.append("\n");
  }

  /**
   *
   * This method is used to add an object's information to
   * the dumpResult's serializable buffer.  It is intended
   * to be called on the server.  
   *
   */

  public void addRow(DBObject object, GanymedeSession owner)
  {
    DBObjectBaseField fieldDef;
    DBField field;
    char[] chars;

    /* -- */

    if (debug)
      {
	System.err.println("DumpResult: addRow(" + object.getLabel() + ")");
      }

    buffer.append(object.getInvid().toString());
    buffer.append("|");

    for (int i = 0; i < fieldDefs.size(); i++)
      {
	fieldDef = (DBObjectBaseField) fieldDefs.elementAt(i);

	// make sure we have permission to see this field

	if (!owner.getPerm(object, fieldDef.getID()).isVisible())
	  {
	    buffer.append("|");
	    continue;
	  }
	
	field = (DBField) object.getField(fieldDef.getID());

	if (field == null)
	  {
	    buffer.append("|");
	    continue;
	  }

	// we use getEncodingString() here primarily so that
	// our dates are encoded in a fashion that can be
	// sorted on the client, and which can be presented in
	// whatever fashion the client chooses.

	chars = field.getEncodingString().toCharArray();
		
	for (int j = 0; j < chars.length; j++)
	  {
	    if (chars[j] == '|')
	      {
		buffer.append("\\|");
	      }
	    else if (chars[j] == '\n')
	      {
		buffer.append("\\\n");
	      }
	    else if (chars[j] == '\\')
	      {
		buffer.append("\\\\");
	      }
	    else
	      {
		buffer.append(chars[j]);
	      }
	  }
	
	buffer.append("|");
      }

    buffer.append("\n");
  }

  //
  //
  // The following methods are intended to be called on a DumpResult
  // after it has been serialized and passed from the server to the
  // client.
  //
  //

  public Vector getHeaders()
  {
    if (!unpacked)
      {
	unpackBuffer();
      }

    return headers;
  }

  public Vector getTypes()
  {
    if (!unpacked)
      {
	unpackBuffer();
      }

    return types;
  }

  public Vector getInvids()
  {
    if (!unpacked)
      {
	unpackBuffer();
      }

    return invids;
  }

  public Invid getInvid(int row)
  {
    if (!unpacked)
      {
	unpackBuffer();
      }

    return (Invid) invids.elementAt(row);
  }

  public Vector getRows()
  {
    if (!unpacked)
      {
	unpackBuffer();
      }

    return rows;
  }
  
  public Vector getFieldRow(int row)
  {
    if (!unpacked)
      {
	unpackBuffer();
      }

    return (Vector) rows.elementAt(row);
  }

  public Object getResult(int row, int col)
  {
    if (!unpacked)
      {
	unpackBuffer();
      }

    return ((Vector) rows.elementAt(row)).elementAt(col);
  }

  public int resultSize()
  {
    if (!unpacked)
      {
	unpackBuffer();
      }

    return rows.size();
  }

  private synchronized void unpackBuffer()
  {
    char[] chars;
    String results = buffer.toString();
    StringBuffer tempString = new StringBuffer();
    int index = 0;
    Vector rowVect;
    short currentFieldType = -1;

    /* -- */

    headers = new Vector();
    types = new Vector();
    invids = new Vector();
    rows = new Vector();

    chars = results.toCharArray();

    // read in the header definition line

    if (debug)
      {
	System.err.println("*** unpacking buffer");
      }
    
    while (chars[index] != '\n')
      {
	tempString.setLength(0); // truncate the buffer

	while (chars[index] != '|')
	  {
	    if (chars[index] == '\n')
	      {
		throw new RuntimeException("parse error in header list");
	      }

	    // if we have a backslashed character, take the backslashed char
	    // as a literal

	    if (chars[index] == '\\')
	      {
		index++;
	      }

	    tempString.append(chars[index++]);
	  }

	index++;		// skip past |

	if (debug)
	  {
	    System.err.println("Header[" + headers.size() + "]: " + tempString.toString());
	  }

	headers.addElement(tempString.toString());
      }

    index++;			// skip past \n

    // read in the types line

    if (debug)
      {
	System.err.println("*** unpacking types line");
      }
    
    while (chars[index] != '\n')
      {
	tempString.setLength(0); // truncate the buffer

	while (chars[index] != '|')
	  {
	    if (chars[index] == '\n')
	      {
		throw new RuntimeException("parse error in header list");
	      }

	    // if we have a backslashed character, take the backslashed char
	    // as a literal

	    if (chars[index] == '\\')
	      {
		index++;
	      }

	    tempString.append(chars[index++]);
	  }

	index++;		// skip past |
	
	if (debug)
	  {
	    System.err.println("Type[" + types.size() + "]: " + tempString.toString());
	  }

	try
	  {
	    types.addElement(Short.valueOf(tempString.toString()));
	    //	    currentFieldType = ((Short) types.lastElement()).shortValue();
	  }
	catch (NumberFormatException ex)
	  {
	    throw new RuntimeException("Ay Carumba!  Number Parse Error! " + ex);
	  }
      }

    index++;			// skip past \n

    // now read in all the result lines

    while (index < chars.length)
      {
	// first read in the Invid

	tempString.setLength(0); // truncate the buffer

	if (debug)
	  {
	    System.err.println("*** Unpacking row " + rows.size());
	  }

	while (chars[index] != '|')
	  {
	    // if we have a backslashed character, take the backslashed char
	    // as a literal
	    
	    if (chars[index] == '\n')
	      {
		throw new RuntimeException("parse error in row");
	      }
	    
	    tempString.append(chars[index++]);
	  }

	invids.addElement(new Invid(tempString.toString()));

	index++;		// skip over |

	// now read in the fields for this invid

	rowVect = new Vector();

	while (chars[index] != '\n')
	  {
	    tempString.setLength(0); // truncate the buffer

	    while (chars[index] != '|')
	      {
		// if we have a backslashed character, take the backslashed char
		// as a literal

		if (chars[index] == '\n')
		  {
		    throw new RuntimeException("parse error in header list");
		  }

		if (chars[index] == '\\')
		  {
		    index++;
		  }

		tempString.append(chars[index++]);
	      }

	    index++;		// skip |

	    currentFieldType = ((Short) types.elementAt(rowVect.size())).shortValue();

	    switch (currentFieldType)
	      {
	      case FieldType.DATE:

		if (debug)
		  {
		    System.err.println("parsing date: " + tempString.toString());
		  }

		if (tempString.toString().equals("null") || tempString.toString().equals(""))
		  {
		    rowVect.addElement(null);
		  }
		else
		  {
		    try
		      {
			rowVect.addElement(new Date(Long.parseLong(tempString.toString())));
		      }
		    catch (NumberFormatException ex)
		      {
			throw new RuntimeException("couldn't parse Long encoding (" + tempString.toString()+"): " + ex);
		      }
		  }
		break;
		
	      case FieldType.NUMERIC:

		if (tempString.toString().equals("null") ||
		    tempString.toString().equals(""))
		  {
		    rowVect.addElement(null);
		  }
		else
		  {
		    try
		      {
			rowVect.addElement(new Integer(tempString.toString()));
		      }
		    catch (NumberFormatException ex)
		      {
			throw new RuntimeException("couldn't parse numeric encoding for string *" + 
						   tempString.toString() + "* :" + ex);
		      }
		  }
		break;

	      default:
		rowVect.addElement(tempString.toString());
	      }
	  }

	rows.addElement(rowVect);

	index++; // skip newline
      }

    unpacked = true;
  }
}
