/*

   QueryResult.java

   This class is a serializable object-list result object, which
   conveys results from a query/list operation along with methods that
   can be used to extract the results out of the query/list.
   
   Created: 1 October 1997
   Version: $Revision: 1.11 $ %D%
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

  // ---

  // for use pre-serialized

  transient Hashtable invidHash = null;
  transient private boolean forTransport = true;

  // for transport

  StringBuffer buffer;

  // for use post-serialized

  transient private boolean unpacked = false;

  transient Vector handles = null;
  transient Vector labelList = null;

  /* -- */

  public QueryResult()
  {
    buffer = new StringBuffer();
    invidHash = new Hashtable();
  }

  /**
   *
   * Constructor.
   *
   * @param forTransport If true, this QueryResult will prepare information
   * fed into it for transport by maintaining a StringBuffer.
   *
   */

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

    addRow(object.getInvid(), object.getLabel(),
	   object.isInactivated(),
	   object.willExpire(),
	   object.willBeRemoved());
  }

  /**
   *
   * This method is used to add an object's information to
   * the QueryResult's serializable buffer.  It is intended
   * to be called on the server.  
   *
   */

  public void addRow(Invid invid, String label)
  {
    addRow(invid, label, false, false, false);
  }

  /**
   *
   * This method is used to add an object's information to
   * the QueryResult's serializable buffer.  It is intended
   * to be called on the server.  
   *
   */

  public synchronized void addRow(Invid invid, String label,
				  boolean inactive,
				  boolean expirationSet,
				  boolean removalSet)
  {
    if (debug)
      {
	System.err.println("QueryResult: addRow(" + invid + "," + label + ")");
      }

    if (label == null)
      {
	throw new NullPointerException("QueryResult.addRow(): null label passed in");
      }

    if (forTransport)
      {
	// encode any true bits

	if (inactive)
	  {
	    buffer.append("A");
	  }

	if (expirationSet)
	  {
	    buffer.append("B");
	  }

	if (removalSet)
	  {
	    buffer.append("C");
	  }

	buffer.append("|");

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

  // ***
  //
  // The following methods are intended to be called on a QueryResult
  // after it has been serialized and passed from the server to the
  // client.
  //
  // ***

  /**
   *
   * This method is used by arlut.csd.ganymede.client.objectList to
   * get access to the raw vector of ObjectHandle's post-serialization.
   *
   * Note that this method does not clone our handles vector, we'll just
   * assume that whatever the objectList class on the client does to this
   * vector, we're not going to disturb anyone else who will be looking
   * at the handle list on this query result object. 
   *
   */

  public Vector getHandles()
  {
    if (!unpacked)
      {
	unpackBuffer();
      }

    return handles;
  }

  public Invid getInvid(int row)
  {
    if (!unpacked)
      {
	unpackBuffer();
      }

    return ((ObjectHandle) handles.elementAt(row)).getInvid();
  }

  public Vector getLabels()
  {
    if (!unpacked)
      {
	unpackBuffer();
      }

    if (labelList == null)
      {
	labelList = new Vector();

	for (int i = 0; i < handles.size(); i++)
	  {
	    labelList.addElement(((ObjectHandle) handles.elementAt(i)).getLabel());
	  }
      }

    return labelList;
  }
  
  public String getLabel(int row)
  {
    if (!unpacked)
      {
	unpackBuffer();
      }

    return ((ObjectHandle) handles.elementAt(row)).getLabel();
  }

  public int size()
  {
    if (!unpacked)
      {
	unpackBuffer();
      }

    return handles.size();
  }

  /**
   * returns a Vector of listhandles
   *
   * listHandle gives Label, Invid.
   */

  public synchronized Vector getListHandles()
  {
    Vector valueHandles = new Vector();
    ObjectHandle handle;

    /* -- */

    if (!unpacked)
      {
	unpackBuffer();
      }
    
    for (int i = 0; i < handles.size(); i++)
      {
	handle = (ObjectHandle) handles.elementAt(i);
	valueHandles.addElement(handle.getListHandle());
      }
    
    return valueHandles;
  }

  /**
   * Returns the listHandle for this row.
   */

  public listHandle getListHandle(int row)
  {
    ObjectHandle handle;

    /* -- */

    if (!unpacked)
      {
	unpackBuffer();
      }

    handle = (ObjectHandle) handles.elementAt(row);

    return handle.getListHandle();
  }

  // ***
  //
  // pre-serialization (server-side) methods
  //
  // ***

  /**
   *
   * This method is provided for the server to optimize
   * it's QueryResult loading operations, and is not
   * intended for use post-serialization.
   *
   */

  public synchronized boolean containsInvid(Invid invid)
  {
    return invidHash.contains(invid);
  }

  /**
   *
   * This is a pre-serialization method for concatenating
   * another (for transport) QueryResult to ourself.
   *
   */
  
  public void append(QueryResult result)
  {
    buffer.append(result.buffer.toString());
  }

  // ***
  //
  // private methods
  //
  // ***

  /**
   *
   * Private method to handle building up our datastructure
   * on the post-serialization side.
   *
   */

  private synchronized void unpackBuffer()
  {
    char[] chars;
    String results = buffer.toString();
    StringBuffer tempString = new StringBuffer();
    int index = 0;
    int rows = 0;

    String label;
    Invid invid;
    boolean inactive, expirationSet, removalSet;

    /* -- */

    // prepare our handle vector

    handles = new Vector();

    // turn our serialized buffer into an array of chars
    // for fast processor

    chars = results.toCharArray();

    if (debug)
      {
	System.err.println("*** unpacking buffer");
      }

    // now read in all the result lines

    while (index < chars.length)
      {
	inactive = false;
	expirationSet = false;
	removalSet = false;

	// first read in the bits

	while (chars[index] != '|')
	  {
	    if (chars[index] == 'A')
	      {
		inactive = true;
	      }
	    else if (chars[index] == 'B')
	      {
		expirationSet = true;
	      }
	    else if (chars[index] == 'C')
	      {
		removalSet = true;
	      }

	    index++;
	  }

	index++;		// skip separator |

	// now read in the Invid

	tempString.setLength(0); // truncate the buffer

	while (chars[index] != '|')
	  {
	    if (chars[index] == '\n')
	      {
		throw new RuntimeException("parse error in row" + rows);
	      }
	    
	    tempString.append(chars[index++]);
	  }

	if (tempString.toString().length() != 0)
	  {
	    invid = new Invid(tempString.toString());
	  }
	else
	  {
	    invid = null;
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

	label = tempString.toString();

	handles.addElement(new ObjectHandle(label, invid, 
					    inactive, expirationSet, removalSet));

	rows++;
	index++; // skip newline
      }

    unpacked = true;
  }
}
