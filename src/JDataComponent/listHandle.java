/*
  listHandle.java
  A wrapper for Strings and Objects for use in JstringListBox.

  Mike Mulvaney
  Aug 25, 1997
*/

package arlut.csd.JDataComponent;

public class listHandle {

  String
    label;

  boolean 
    custom = false;

  Object
    object = null;

  public listHandle(String label)
  {
    this(label, null, false);
  }
  
  public listHandle(String label, Object object)
  {
    this(label, object, false);
  }

  public listHandle(String label, Object object, boolean custom)
  {
    this.label = label;
    this.object = object;
    this.custom = custom;
  }

  public String getLabel()
  {
    return label;
  }

  public void setLabel(String label)
  {
    this.label = label;
  }

  public Object getObject()
  {
    return object;
  }

  public void setObject(Object object)
  {
    this.object = object;
  }

  /**
   * Returns the value of this listHandle.
   *
   * If the object has not been set, getValue() returns the label.  Otherwise,
   * the object is returned.
   */

  public Object getValue()
  {
    if (object == null)
      {
	return label;
      }
    return object;
  }
  public boolean isCustom()
  {
    return custom;
  }

  public void setCustom(boolean isCustom)
  {
    this.custom = custom;
  }
}
