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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Vector;
import java.util.Map;

/**
 * @author deepak
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class QueryResultContainer implements List, Serializable {
  static final long serialVersionUID = -8277390343373928867L;

  /**
   * An ordered list of headers that correspond to each column in this result
   * set.
   */
  List headers = null;
  
  /**
   * An ordered list of Shorts that correspond to the DBField ID's of each
   * column in this result set.
   */
  List types = null;
  
  /**
   * A list of Object[]. This represents the actual result set. The list of
   * rows is sorted in the order each row was added to the result set via
   * addRow(). The length of each row is equal to the number of column headers
   * for this result set. The types of the objects contained in each cell of
   * each row depends on the type of the DBField that represents that cell's
   * value. A StringDBField column, for example, will yield String objects while
   * a NumericDBField will yield an Integer.
   */
  List rows = null;
  
  /**
   * An ordered list of ObjectHandles that correspond to the DBObject
   * represented in each row of the result set. There is one handle per
   * row.
   */
  Vector handles = null;
  
  /**
   * Optimization structure that speeds up obtaining a list of labels for each
   * object referenced in the result set. This list has the same order of the
   * rows of the result set.
   */
  transient Vector labelList = null;
   
   /**
   * Optimization structure that speeds up obtaining a list of invids for each
   * object referenced in the result set. This list has the same order of the
   * rows of the result set.
   */ 
  transient Vector invidList = null;
  
  /**
   * Optimization structure that speeds up obtaining an Invid given a DBObject's
   * label. This is not a general-purpose lookup method, as it only applies to
   * DBObjects referenced by this result set.
   * 
   * FIXME: There is an outstanding bug here. Using a Hash for this purpose
   * assumes that there is a 1-to-1 mapping between labels and the DBObjects
   * they represent. However, it's possible to have multiple DBObjects with the
   * same Invids. In other words, the size of the keyset (or value set) of the
   * hash may be smaller than the number of rows.
   */
  transient Map labelHash = null;
  
  /**
   * Optimization structure that speeds up obtaining an object's label given that
   * DBObject's invid. This is not a general-purpose lookup method, as it only
   * applies to DBObjects referenced by this result set.
   */
  transient Map invidHash = null;

  public QueryResultContainer()
  {
    handles = new Vector();
    headers = new ArrayList();
    types = new ArrayList();
    rows = new ArrayList();
  }
  
  /**
   * Adds a field header to this query result. Field headers are used to map
   * column numbers in the result set to column identifiers; the first field
   * header added using this method will be the name of column 0, the second
   * will be the name of column 1, etc.
   */
   
  public void addField(String fieldName, Short fieldID)
  {
    headers.add(fieldName);
    types.add(fieldID);
  }

  /**
   *
   * This method is used to add an object's information to
   * the QueryResult's set of objects that matched the original
   * query.
   *
   */

  public void addRow(Invid invid, String label, Object[] row, boolean editable)
  {
    addRow(invid, label, row, false, false, false, editable);
  }

  /**
   *
   * This method is used to add an object's information to
   * the QueryResult's set of objects that matched the original
   * query.
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
   * the QueryResult's set of objects that matched the original
   * query.
   *
   */

  public synchronized void addRow(Invid invid, String label,
				  Object[] row,
				  boolean inactive,
				  boolean expirationSet,
				  boolean removalSet,
				  boolean editable)
  {
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

    rows.add(row);
  }

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
    return handles;
  }

  /**
   * Grab the Invid of the object represented in the given row of the
   * result set.
   * 
   * @param row
   * @return
   */

  public Invid getInvid(int row)
  {
    return ((ObjectHandle) handles.elementAt(row)).getInvid();
  }

  /**
   * Grab the list of Invids that are contained in this result set.
   * The invids returned are in the same order that they were added
   * to this result set.
   * 
   * @return
   */

  public Vector getInvids()
  {
    if (invidList == null)
      {
      	rebuildTransients();
      }

    return invidList;
  }

  /**
   * Grab a list of the labels of each object contained in this result set.
   * The labels returned are in the same order their corresponding objects were 
   * added to this result set.
   * 
   * @return
   */

  public Vector getLabels()
  {
    if (labelList == null)
      {
      	rebuildTransients();
      }

    return labelList;
  }

  /**
   * Grabs the label for the object represented in the given row of the result
   * set.
   * 
   * @param row
   * @return
   */
  
  public String getLabel(int row)
  {
    return ((ObjectHandle) handles.elementAt(row)).getLabel();
  }

  /**
   * Returns the ObjectHandle for this row.
   */

  public ObjectHandle getObjectHandle(int row)
  {
    return (ObjectHandle) handles.elementAt(row);
  }

  /**
   * Returns boolean showing if the result set contains the given Invid.
   */

  public synchronized boolean containsInvid(Invid invid)
  {
    if (invidHash == null)
      {
      	rebuildTransients();
      }
    
    return invidHash.containsKey(invid);
  }

  /**
   * Returns boolean showing if the result set contains an object with the 
   * given label.
   */

  public synchronized boolean containsLabel(String label)
  {
    if (labelHash == null)
      {
      	rebuildTransients();
      }
    
    return labelHash.containsKey(label);
  }

  /**
   * <p>This method can be called on the client to obtain a {@link
   * java.util.Vector Vector} of field names, used to generate the
   * list of column headers in the GUI client.</p>
   */

  public List getHeaders()
  {
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
    return rows;
  }


  /**
   * <p>This method can be called on the client to obtain a {@link
   * java.util.Vector Vector} containing the data values returned for
   * the object at row <i>row</i>, in field order matching the field
   * names and types returned by {@link
   * arlut.csd.ddroid.common.QueryResultContainer getHeaders()} and
   * {@link arlut.csd.ddroid.common.QueryResultContainer
   * getTypes()}.</p>
   */
  
  public Vector getFieldRow(int rowNumber)
  {
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
    Object[] r = (Object[]) rows.get(row);
    return r[col];
  }

  /**
   * <p>This method can be called on the client to determine the
   * number of objects encoded in this result set.</p>
   */

  public int resultSize()
  {
    return rows.size();
  }
  
  /**
   * Used to rebuild any ancillary data-structures that aren't preserved during
   * serialization/deserialization.
   * 
   */
  
  private void rebuildTransients()
  {
    invidList = new Vector(handles.size());
    labelList = new Vector(handles.size());
    invidHash = new HashMap();
    labelHash = new HashMap();

    Invid invid;
    String label;
    for (int i = 0; i < handles.size(); i++)
      {
        invid = ((ObjectHandle) handles.elementAt(i)).getInvid();
        label = ((ObjectHandle) handles.elementAt(i)).getLabel();
        invidList.addElement(invid);
        labelList.addElement(label);
        invidHash.put(invid, label);
        labelHash.put(label, invid);
      }
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
    return rows.contains(o);
  }
  
  /* 
   * @see java.util.Collection#containsAll(java.util.Collection)
   */
  public boolean containsAll(Collection c)
  {
    return rows.containsAll(c);
  }
  
  /* 
   * @see java.util.List#get(int)
   */
  public Object get(int index)
  {
    return rows.get(index);
  }
  
  /* 
   * @see java.util.List#indexOf(java.lang.Object)
   */
  public int indexOf(Object o)
  {
    return rows.indexOf(o);
  }
  
  /* 
   * @see java.util.Collection#isEmpty()
   */
  public boolean isEmpty()
  {
    return rows.isEmpty();
  }
  
  /* 
   * @see java.util.Collection#iterator()
   */
  public Iterator iterator()
  {
    return rows.iterator();
  }
  
  /* 
   * @see java.util.List#lastIndexOf(java.lang.Object)
   */
  public int lastIndexOf(Object o)
  {
    return rows.lastIndexOf(o);
  }
  
  /* 
   * @see java.util.List#listIterator()
   */
  public ListIterator listIterator()
  {
    return rows.listIterator();
  }
  
  /* 
   * @see java.util.List#listIterator(int)
   */
  public ListIterator listIterator(int index)
  {
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
    return rows.size();
  }
  
  /* 
   * @see java.util.List#subList(int, int)
   */
  public List subList(int fromIndex, int toIndex)
  {
    return rows.subList(fromIndex, toIndex);
  }
  
  /* 
   * @see java.util.Collection#toArray()
   */
  public Object[] toArray()
  {
    return rows.toArray();
  }
  
  /* 
   * @see java.util.Collection#toArray(java.lang.Object[])
   */
  public Object[] toArray(Object[] a)
  {
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
