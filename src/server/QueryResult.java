/*

   QueryResult.java

   This class is a serializable object-list result object, which
   conveys results from a query/list operation along with methods that
   can be used to extract the results out of the query/list.
   
   Created: 1 October 1997
   Version: $Revision: 1.9 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import java.util.*;

import arlut.csd.JDataComponent.listHandle;

/*------------------------------------------------------------------------------
                                                                           class
                                                                     QueryResult

------------------------------------------------------------------------------*/

/**
 *
 *  This class is a serializable object-list result object, which
 *  conveys results from a query/list operation along with methods that
 *  can be used to extract the results out of the query/list.
 *
 */

public class QueryResult implements java.io.Serializable {

  static final boolean debug = false;

  // for use pre-serialized

  transient Hashtable invidHash = null;
  transient boolean forTransport = true;

  // for transport

  StringBuffer buffer;

  // for use post-serialized

  transient private boolean unpacked = false;

  transient Vector invids = null;
  transient Vector labels = null;

  /* -- */

  public QueryResult()
  {
    buffer = new StringBuffer();
    invidHash = new Hashtable();
  }

  public QueryResult(boolean forTransport)
  {
    this();
    this.forTransport = forTransport;
  }

  /**
   *
   * This method is used to add an object's information to
   * the QueryResult's serializable buffer.  It is intended
   * to be called on the server.  
   *
   */

  public synchronized void addRow(DBObject object)
  {
    if (debug)
      {
	System.err.println("QueryResult: addRow(" + object.getLabel() + ")");
      }

    addRow(object.getInvid(), object.getLabel());
  }

  /**
   *
   * This method is used to add an object's information to
   * the QueryResult's serializable buffer.  It is intended
   * to be called on the server.  
   *
   */

  public synchronized void addRow(Invid invid, String label)
  {
    if (debug)
      {
	System.err.println("QueryResult: addRow(" + invid + "," + label + ")");
      }

    if (forTransport)
      {
	if (invid != null)
	  {
	    buffer.append(invid.toString());       
	  }
	
	buffer.append("|");
	char[] chars = label.toCharArray();
	
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
	
	buffer.append("\n");
      }

    if (invid != null)
      {
	// Remember that we have this invid already added

	invidHash.put(invid, label);
      }
  }

  //
  //
  // The following methods are intended to be called on a QueryResult
  // after it has been serialized and passed from the server to the
  // client.
  //
  //

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

  public Vector getLabels()
  {
    if (!unpacked)
      {
	unpackBuffer();
      }

    return labels;
  }
  
  public String getLabel(int row)
  {
    if (!unpacked)
      {
	unpackBuffer();
      }

    return (String) labels.elementAt(row);
  }

  public int size()
  {
    if (!unpacked)
      {
	unpackBuffer();
      }

    return labels.size();
  }

  /**
   * returns a Vector of listhandles
   *
   * listHandle gives Label, Invid.
   */

  public synchronized Vector getListHandles()
  {
    Vector valueHandles = new Vector();

    /* -- */
    
    for (int i = 0; i < size(); i++)
      {
	valueHandles.addElement(new listHandle(getLabel(i), getInvid(i)));
      }
    
    return valueHandles;
  }

  public synchronized boolean containsInvid(Invid invid)
  {
    return invidHash.contains(invid);
  }

  /**
   * Returns the listHandle for this row.
   */
  public listHandle getListHandle(int row)
  {
    return new listHandle(getLabel(row), getInvid(row));
  }
  
  public void append(QueryResult result)
  {
    buffer.append(result.buffer.toString());
  }

  private synchronized void unpackBuffer()
  {
    char[] chars;
    String results = buffer.toString();
    StringBuffer tempString = new StringBuffer();
    int index = 0;

    /* -- */

    invids = new Vector();
    labels = new Vector();

    chars = results.toCharArray();

    // read in the header definition line

    if (debug)
      {
	System.err.println("*** unpacking buffer");
      }

    // now read in all the result lines

    while (index < chars.length)
      {
	// first read in the Invid

	tempString.setLength(0); // truncate the buffer

	if (debug)
	  {
	    System.err.println("*** Unpacking row " + labels.size());
	  }

	while (chars[index] != '|')
	  {
	    if (chars[index] == '\n')
	      {
		throw new RuntimeException("parse error in row" + labels.size());
	      }
	    
	    tempString.append(chars[index++]);
	  }

	if (tempString.toString().length() != 0)
	  {
	    invids.addElement(new Invid(tempString.toString()));
	  }
	else
	  {
	    invids.addElement(null);
	  }

	index++;		// skip over |

	// now read in the label for this invid

	tempString.setLength(0); // truncate the buffer

	while (chars[index] != '\n')
	  {
	    // if we have a backslashed character, take the backslashed char
	    // as a literal
	    
	    if (chars[index] == '\\')
	      {
		index++;
	      }
	    
	    tempString.append(chars[index++]);
	  }

	labels.addElement(tempString.toString());

	index++; // skip newline
      }

    unpacked = true;
  }
}
