/*
 * Created on Sep 16, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package arlut.csd.ddroid.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Vector;

/**
 * @author deepak
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class QueryResultContainer implements Serializable {
  static final boolean debug = false;
  static final long serialVersionUID = -8277390343373928867L;

  private boolean forTransport = true;
  public StringBuffer buffer = null;
  private boolean unpacked = false;

  List headers = null;
  List types = null;
  List rows = null;
  Vector handles = null;
  
  transient Vector labelList = null;
  transient Vector invidList = null;

  /* -- */

  public QueryResultContainer()
  {
    buffer = new StringBuffer();
    handles = new Vector();
    headers = new ArrayList();
    types = new ArrayList();
    rows = new ArrayList();
  }
  
  public QueryResultContainer(boolean forTransport)
  {
    this();
    this.forTransport = forTransport;
  }

  public void addField(String fieldName, Short fieldID)
  {
    headers.add(fieldName);
    types.add(fieldID);
  }

  /**
   *
   * This method is used to add an object's information to
   * the QueryResult's serializable buffer.  It is intended
   * to be called on the server, but may also be called on
   * the client for result augmentation.
   *
   */

  public void addRow(Invid invid, String label, Object[] row, boolean editable)
  {
    addRow(invid, label, row, false, false, false, editable);
  }

  /**
   *
   * This method is used to add an object's information to
   * the QueryResult's serializable buffer.  It is intended
   * to be called on the server, but may also be called on
   * the client for result augmentation.
   *
   */

  public void addRow(ObjectHandle handle, Object[] row)
  {
    addRow(handle.invid, handle.label, row, handle.inactive,
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
				  Object[] row,
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

    if (invid != null)
      {
	if (invidList != null)
	  {
	    invidList.addElement(invid);
	  }
      }

    if (label != null)
      {
	if (labelList != null)
	  {
	    labelList.addElement(label);
	  }
      }
      
    rows.add(row);
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
   * This method is used by arlut.csd.ddroid.client.objectList to
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
    return getInvids().contains(invid);
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
    return getLabels().contains(label);
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
  

  /**
   * <p>This method can be called on the client to obtain a {@link
   * java.util.Vector Vector} of field names, used to generate the
   * list of column headers in the GUI client.</p>
   */

  public List getHeaders()
  {
    if (!unpacked)
      {
	unpackBuffer();
      }

    return headers;
  }

  /**
   * <p>This method can be called on the client to obtain a {@link
   * java.util.Vector Vector} of field type {@link java.lang.Short
   * Shorts} (enumerated in the {@link
   * arlut.csd.ddroid.common.FieldType FieldType} static constants),
   * identifying the types of fields for each column in the
   * DumpResult.</p>
   */

  public List getTypes()
  {
    if (!unpacked)
      {
	unpackBuffer();
      }

    return types;
  }

  /**
   * <p>This method can be called on the client to obtain a {@link
   * java.util.Vector Vector} of Vectors, each of which contains
   * the data values returned for each object, in field order matching
   * the field names and types returned by {@link arlut.csd.ddroid.common.DumpResult#getHeaders getHeaders()}
   * and {@link arlut.csd.ddroid.common.DumpResult#getTypes getTypes()}.</p>
   */

  public List getRows()
  {
    if (!unpacked)
      {
	unpackBuffer();
      }

    return rows;
  }


  /**
   * <p>This method can be called on the client to obtain a {@link
   * java.util.Vector Vector} containing the data values returned for
   * the object at row <i>row</i>, in field order matching the field
   * names and types returned by {@link
   * arlut.csd.ddroid.common.DumpResult#getHeaders getHeaders()} and
   * {@link arlut.csd.ddroid.common.DumpResult#getTypes
   * getTypes()}.</p>
   */
  
  public Vector getFieldRow(int rowNumber)
  {
    if (!unpacked)
      {
	unpackBuffer();
      }

    Object[] row = (Object[]) rows.get(rowNumber);
    return new Vector(Arrays.asList(row));
  }

  /**
   * <p>This method can be called on the client to obtain an Object
   * encoding the result value for the <i>col</i>th field in the
   * <i>row</i>th object.</p>  These Objects may be a {@link java.lang.Double Double}
   * for Float fields, an {@link java.lang.Integer Integer} for Numeric fields,
   * a {@link java.util.Date Date} for Date fields, or a String for other
   * fields.</p>
   */

  public Object getResult(int row, int col)
  {
    if (!unpacked)
      {
	unpackBuffer();
      }

    Object[] r = (Object[]) rows.get(row);
    return r[col];
  }

  /**
   * <p>This method can be called on the client to determine the
   * number of objects encoded in this DumpResult.</p>
   */

  public int resultSize()
  {
    if (!unpacked)
      {
	unpackBuffer();
      }

    return rows.size();
  }
  
  private void unpackBuffer()
  {
    unpacked = true;
  }

  /* ------------------------------------------------------------------------
   * This is the start of the List interface implementation
   * 
   */
  
  /* This is a no-op since a DumpResult is immutable.
   * 
   * @see java.util.List#add(int, java.lang.Object)
   */
  public void add(int index, Object element)
  {
    throw new UnsupportedOperationException();
  }
  
  /* This is a no-op since a DumpResult is immutable.
   * 
   * @see java.util.Collection#add(java.lang.Object)
   */
  public boolean add(Object o)
  {
    throw new UnsupportedOperationException();
  }
  
  /* This is a no-op since DumpResult is immutable.
   * 
   * @see java.util.Collection#addAll(java.util.Collection)
   */
  public boolean addAll(Collection c)
  {
    throw new UnsupportedOperationException();
  }
  
  /* This is a no-op since DumpResult is immutable.
   * 
   * @see java.util.List#addAll(int, java.util.Collection)
   */
  public boolean addAll(int index, Collection c)
  {
    throw new UnsupportedOperationException();
  }
  
  /* This is a no-op since DumpResult is immutable.
   * 
   * @see java.util.Collection#clear()
   */
  public void clear()
  {
    throw new UnsupportedOperationException();
  }
  
  /* 
   * @see java.util.Collection#contains(java.lang.Object)
   */
  public boolean contains(Object o)
  {
    if (!unpacked)
      {
	unpackBuffer();
      }
    return rows.contains(o);
  }
  
  /* 
   * @see java.util.Collection#containsAll(java.util.Collection)
   */
  public boolean containsAll(Collection c)
  {
    if (!unpacked)
      {
	unpackBuffer();
      }
    return rows.containsAll(c);
  }
  
  /* 
   * @see java.util.List#get(int)
   */
  public Object get(int index)
  {
    if (!unpacked)
      {
	unpackBuffer();
      }
    return rows.get(index);
  }
  
  /* 
   * @see java.util.List#indexOf(java.lang.Object)
   */
  public int indexOf(Object o)
  {
    if (!unpacked)
      {
	unpackBuffer();
      }
    return rows.indexOf(o);
  }
  
  /* 
   * @see java.util.Collection#isEmpty()
   */
  public boolean isEmpty()
  {
    if (!unpacked)
      {
	unpackBuffer();
      }
    return rows.isEmpty();
  }
  
  /* 
   * @see java.util.Collection#iterator()
   */
  public Iterator iterator()
  {
    if (!unpacked)
      {
	unpackBuffer();
      }
    return rows.iterator();
  }
  
  /* 
   * @see java.util.List#lastIndexOf(java.lang.Object)
   */
  public int lastIndexOf(Object o)
  {
    if (!unpacked)
      {
	unpackBuffer();
      }
    return rows.lastIndexOf(o);
  }
  
  /* 
   * @see java.util.List#listIterator()
   */
  public ListIterator listIterator()
  {
    if (!unpacked)
      {
	unpackBuffer();
      }
    return rows.listIterator();
  }
  
  /* 
   * @see java.util.List#listIterator(int)
   */
  public ListIterator listIterator(int index)
  {
    if (!unpacked)
      {
	unpackBuffer();
      }
    return rows.listIterator(index);
  }
  
  /* This is a no-op since DumpResult is immutable.
   * 
   * @see java.util.List#remove(int)
   */
  public Object remove(int index)
  {
    throw new UnsupportedOperationException();
  }
  
  /* This is a no-op since DumpResult is immutable.
   *
   * @see java.util.Collection#remove(java.lang.Object)
   */
  public boolean remove(Object o)
  {
    throw new UnsupportedOperationException();
  }
  
  /* This is a no-op since DumpResult is immutable.
   * 
   * @see java.util.Collection#removeAll(java.util.Collection)
   */
  public boolean removeAll(Collection c)
  {
    throw new UnsupportedOperationException();
  }
  
  /* This is a no-op since DumpResult is immutable.
   * 
   * @see java.util.Collection#retainAll(java.util.Collection)
   */
  public boolean retainAll(Collection c)
  {
    throw new UnsupportedOperationException();
  }
  
  /* This is a no-op since DumpResult is immutable.
   * 
   * @see java.util.List#set(int, java.lang.Object)
   */
  public Object set(int index, Object element)
  {
    throw new UnsupportedOperationException();
  }
  
  /* 
   * @see java.util.Collection#size()
   */
  public int size()
  {
    if (!unpacked)
      {
	unpackBuffer();
      }
    return rows.size();
  }
  
  /* 
   * @see java.util.List#subList(int, int)
   */
  public List subList(int fromIndex, int toIndex)
  {
    if (!unpacked)
      {
	unpackBuffer();
      }
    return rows.subList(fromIndex, toIndex);
  }
  
  /* 
   * @see java.util.Collection#toArray()
   */
  public Object[] toArray()
  {
    if (!unpacked)
      {
	unpackBuffer();
      }
    return rows.toArray();
  }
  
  /* 
   * @see java.util.Collection#toArray(java.lang.Object[])
   */
  public Object[] toArray(Object[] a)
  {
    if (!unpacked)
      {
	unpackBuffer();
      }
    return rows.toArray(a);
  }

  /*
   * @see java.lang.Object#toString()
   */
  public String toString()
  {
    StringBuffer result = new StringBuffer();
    
    for (Iterator iter = headers.iterator(); iter.hasNext();)
      {
      	result.append((String) iter.next() + ":\t");
      }

    result.append("\n");

    Object[] row;
    for (Iterator iter = rows.iterator(); iter.hasNext();)
      {
      	row = (Object[]) iter.next();
      	for (int i = 0; i < row.length; i++)
      	  {
      	    result.append(row[i].toString() + "\t");
      	  }
      	result.append("\n");
      }
    
    return result.toString();
  }
}
