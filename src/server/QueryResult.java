/*

   QueryResult.java

   This class is a serializable object-list result object, which
   conveys results from a query/list operation along with methods that
   can be used to extract the results out of the query/list.
   
   Created: 1 October 1997
   Release: $Name:  $
   Version: $Revision: 1.29 $
   Last Mod Date: $Date: 2002/11/01 02:24:18 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999, 2000, 2001, 2002
   The University of Texas at Austin.

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
   along with this program; if not, write to the Free Software
   Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
   02111-1307, USA

*/

package arlut.csd.ganymede;

import java.util.*;

import arlut.csd.JDataComponent.listHandle;
import arlut.csd.Util.*;

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

  static final long serialVersionUID = 7593411645538822028L;

  static final boolean debug = false;

  public static Compare comparator = new arlut.csd.Util.Compare() 
    {
      public int compare(Object o_a, Object o_b) 
	{
	  ObjectHandle a, b;
	  
	  a = (ObjectHandle) o_a;
	  b = (ObjectHandle) o_b;
	  int comp = 0;
	  
	  comp = a.getLabel().compareTo(b.getLabel());
	  
	  if (comp < 0)
	    {
	      return -1;
	    }
	  else if (comp > 0)
	    { 
	      return 1;
	    } 
	  else
	    { 
	      return 0;
	    }
	}
    };

  // ---

  // for use pre-serialized

  transient Hashtable invidHash = null;
  transient Hashtable labelHash = null;
  private boolean forTransport = true;

  // for transport

  StringBuffer buffer;

  // for use post-serialized

  transient private boolean unpacked = false;

  transient Vector handles = null;
  transient Vector labelList = null;
  transient Vector invidList = null;

  transient VecSortInsert inserter;

  /* -- */

  public QueryResult()
  {
    buffer = new StringBuffer();
    invidHash = new Hashtable();
    labelHash = new Hashtable();
    handles = new Vector();
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
   * to be called on the server, but may also be called on
   * the client for result augmentation.
   *
   */

  public void addRow(Invid invid, String label, boolean editable)
  {
    addRow(invid, label, false, false, false, editable);
  }

  /**
   *
   * This method is used to add an object's information to
   * the QueryResult's serializable buffer.  It is intended
   * to be called on the server, but may also be called on
   * the client for result augmentation.
   *
   */

  public void addRow(ObjectHandle handle)
  {
    addRow(handle.invid, handle.label, handle.inactive,
	   handle.expirationSet, handle.removalSet,
	   handle.editable);
  }

  /**
   *
   * This method is used to add an object's information to
   * the QueryResult's serializable buffer.  It is intended
   * to be called on the server, but may also be called on
   * the client for result augmentation.
   *
   */

  public synchronized void addRow(Invid invid, String label,
				  boolean inactive,
				  boolean expirationSet,
				  boolean removalSet,
				  boolean editable)
  {
    if (debug)
      {
	System.err.println("QueryResult: addRow(" + invid + "," + label + ")");
      }

    if (label == null)
      {
	throw new NullPointerException("QueryResult.addRow(): null label passed in");
      }

    // don't add an object we've already got here

    if (invid != null && containsInvid(invid))
      {
	return;
      }
    else if (invid == null && containsLabel(label))
      {
	return;
      }

    handles.addElement(new ObjectHandle(label, invid, 
					inactive, expirationSet, 
					removalSet, editable));

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

	if (editable)
	  {
	    buffer.append("D");
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
	invidHash.put(invid, label);
	invidList.addElement(invid);
      }

    if (label != null)
      {
	labelHash.put(label, label);
	labelList.addElement(label);
      }

    unpacked = false;
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
   * get access to the raw and sorted vector of ObjectHandle's post-serialization.<br><br>
   *
   * Note that this method does not clone our handles vector, we'll just
   * assume that whatever the objectList class on the client does to this
   * vector, we're not going to disturb anyone else who will be looking
   * at the handle list on this query result object. 
   *
   */

  public Vector getHandles()
  {
    if (forTransport && !unpacked)
      {
	unpackBuffer();
      }

    return handles;
  }

  public Invid getInvid(int row)
  {
    if (forTransport && !unpacked)
      {
	unpackBuffer();
      }

    return ((ObjectHandle) handles.elementAt(row)).getInvid();
  }

  public Vector getInvids()
  {
    if (forTransport && !unpacked)
      {
	unpackBuffer();
      }

    if (invidList == null)
      {
	invidList = new Vector();

	for (int i = 0; i < handles.size(); i++)
	  {
	    invidList.addElement(((ObjectHandle) handles.elementAt(i)).getInvid());
	  }
      }

    return invidList;
  }

  public Vector getLabels()
  {
    if (forTransport && !unpacked)
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
    if (forTransport && !unpacked)
      {
	unpackBuffer();
      }

    return ((ObjectHandle) handles.elementAt(row)).getLabel();
  }

  public boolean isForTransport()
  {
    return forTransport;
  }

  public int size()
  {
    if (forTransport && !unpacked)
      {
	unpackBuffer();
      }

    return handles.size();
  }

  /**
   * Returns a complete {@link arlut.csd.JDataComponent.listHandle listHandle}
   * Vector representation of the results included in this QueryResult.
   */

  public Vector getListHandles()
  {
    return getListHandles(true, true);
  }

  /**
   * Returns a (possibly filtered) {@link arlut.csd.JDataComponent.listHandle listHandle}
   * Vector representation of the results included in this QueryResult.
   *
   * @param includeInactives if false, inactive objects' handles won't be included
   * in the returned vector
   * @param includeNonEditables if false, non-editable objects' handles won't be included
   * in the returned vector
   */

  public synchronized Vector getListHandles(boolean includeInactives,
					    boolean includeNonEditables)
  {
    Vector valueHandles = new Vector();
    ObjectHandle handle;

    /* -- */

    if (forTransport && !unpacked)
      {
	unpackBuffer();
      }
    
    for (int i = 0; i < handles.size(); i++)
      {
	handle = (ObjectHandle) handles.elementAt(i);

	if ((includeInactives || !handle.isInactive()) &&
	    (includeNonEditables || handle.isEditable()))
	  {
	    valueHandles.addElement(handle.getListHandle());
	  }
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

    if (forTransport && !unpacked)
      {
	unpackBuffer();
      }

    handle = (ObjectHandle) handles.elementAt(row);

    return handle.getListHandle();
  }

  /**
   * Returns the ObjectHandle for this row.
   */

  public ObjectHandle getObjectHandle(int row)
  {
    ObjectHandle handle;

    /* -- */

    if (forTransport && !unpacked)
      {
	unpackBuffer();
      }

    handle = (ObjectHandle) handles.elementAt(row);

    return handle;
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
    return invidHash.containsKey(invid);
  }

  /**
   *
   * This method is provided for the server to optimize
   * it's QueryResult loading operations, and is not
   * intended for use post-serialization.
   *
   */

  public synchronized boolean containsLabel(String label)
  {
    return labelHash.containsKey(label);
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
    unpacked = false;

    Enumeration enum = result.invidHash.keys();

    while (enum.hasMoreElements())
      {
	Object key = enum.nextElement();
	Object val = result.invidHash.get(key);

	this.invidHash.put(key, val);
      }

    enum = result.labelHash.keys();

    while (enum.hasMoreElements())
      {
	Object key = enum.nextElement();
	Object val = result.labelHash.get(key);

	this.labelHash.put(key, val);
      }
  }

  /**
   *
   * This method returns a QueryResult which holds the intersection
   * of the contents of this QueryResult and the contents of
   * operand.
   *
   */

  public synchronized QueryResult intersection(QueryResult operand)
  {
    QueryResult result = new QueryResult(forTransport);
    ObjectHandle handle;

    /* -- */

    if (operand == null || operand.size() == 0)
      {
	return result;
      }

    for (int i = 0; i < handles.size(); i++)
      {
	handle = (ObjectHandle) handles.elementAt(i);

	if (handle.getInvid() != null)
	  {
	    if (operand.containsInvid(handle.getInvid()))
	      {
		result.addRow(handle);
	      }
	  }
	else
	  {
	    if (operand.containsLabel(handle.getLabel()))
	      {
		result.addRow(handle);
	      }
	  }
      }

    return result;
  }

  // ***
  //
  // private methods
  //
  // ***

  /**
   *
   * Private method to handle building up our datastructure
   * on the post-serialization side.  Sorts the handles vector
   * as it is extracted.
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
    boolean inactive, expirationSet, removalSet, editable;

    /* -- */

    if (debug)
      {
	System.err.println("QueryResult.unpackBuffer()");
      }

    // prepare our handle vector

    handles = new Vector();
    
    inserter = new VecSortInsert(comparator);

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
	editable = false;

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
	    else if (chars[index] == 'D')
	      {
		editable = true;
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
	    invid = Invid.createInvid(tempString.toString());
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

	inserter.insert(handles, new ObjectHandle(label, invid,
						  inactive, expirationSet,
						  removalSet, editable));

	rows++;
	index++; // skip newline
      }

    unpacked = true;
  }

  /**
   *
   * For debug.
   * 
   */

  public String getBuffer()
  {
    return buffer.toString();
  }
}
