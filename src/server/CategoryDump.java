/*

   CategoryDump.java

   This class is intended to serve as a stub to hold basic
   information about server side categories for the client
   to process locally.

   Note that even though this class is implementing a remote
   interface, it is doing so for the purpose of providing
   a consistent interface for the client, not for actual
   remote access.  Thus, we are not extending UnicastRemoteObject
   as we would if we were truly a remote object.
   
   Created: 12 February 1998
   Version: $Revision: 1.4 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import java.rmi.*;
import java.util.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                    CategoryDump

------------------------------------------------------------------------------*/

/**
 *
 *  <p>This class is intended to serve as a stub to hold basic
 *  information about server side categories for the client
 *  to process locally.</p>
 *
 *  <p>Note that even though this class is implementing a remote
 *  interface, it is doing so for the purpose of providing
 *  a consistent interface for the client, not for actual
 *  remote access.  Thus, we are not extending UnicastRemoteObject
 *  as we would if we were truly a remote object.</p>
 *
 */

public class CategoryDump implements Category, CategoryNode {

  CategoryDump parent;
  String name;
  int displayOrder;
  Vector contents = new Vector();

  private int lastIndex = -1;

  /* -- */

  public CategoryDump(CategoryDump parent, char[] src, int index)
  {
    String token;
    CategoryDump catChild;
    BaseDump baseChild;

    /* -- */

    this.parent = parent;

    if (parent == null)
      {
	// skip the 'cat' chunk if we're the root
	getChunk(src, index);
      }
    else
      {
	lastIndex = index;
      }

    this.name = getChunk(src, lastIndex);

    // getChunk() updates lastIndex for us

    try
      {
	this.displayOrder = Integer.valueOf(getChunk(src, lastIndex)).intValue();
      }
    catch (NumberFormatException ex)
      {
	throw new RuntimeException("couldn't parse display order chunk " + ex);
      }

    token = getChunk(src, lastIndex);

    if (token.equals("<"))
      {
	// we've got contents

	token = getChunk(src, lastIndex);

	if (token.equals("cat"))
	  {
	    catChild = new CategoryDump(this, src, lastIndex);
	    lastIndex = catChild.getLastIndex();
	    contents.addElement(catChild);
	  }
	else
	  {
	    baseChild = new BaseDump(this, src, lastIndex);
	    lastIndex = baseChild.getLastIndex();
	    contents.addElement(baseChild);
	  }

	// get the close category chunk

	token = getChunk(src, lastIndex);
      }
    
    if (!token.equals(">"))
      {
	throw new RuntimeException("parse error, couldn't find end of category in dump: " + token);
      }
  }

  public int getLastIndex()
  {
    return lastIndex;
  }

  /**
   *
   * Returns the name of this category.
   *
   */

  public String getName() 
  {
    return name;
  }

  /**
   *
   * Returns the full path to this category, with levels
   * in the hierarchy separated by '/'s.
   *
   */

  public String getPath() 
  {
    if (parent != null)
      {
	return parent.getPath() + "/" + name;
      }
    else
      {
	return "/" + name;
      }
  }

  /**
   *
   * Sets the name of this node.  The name must not include a '/'
   * character, but all other characters are acceptable.
   *
   */

  public boolean setName(String newName) 
  {
    throw new IllegalArgumentException("can't call modification methods on CategoryDump.");
  }

  /**
   *
   * This method tells the CategoryNode what it's containing
   * category is.
   *
   */

  public void setCategory(Category category)
  {
    throw new IllegalArgumentException("can't call modification methods on CategoryDump.");
  }

  /**
   *
   * This method returns the category that this
   * category node belongs to.  If this is the DBStore's
   * root category, this will return null.
   *
   */

  public Category getCategory() 
  {
    return parent;
  }

  /**
   *
   * Returns child nodes
   *
   */

  public Vector getNodes() 
  {
    return contents;
  }

  /**
   *
   * Returns a subcategory of name <name>.
   *
   */

  public CategoryNode getNode(String name) 
  {
    CategoryNode candidate;

    /* -- */

    for (int i = 0; i < contents.size(); i++)
      {
	candidate = (CategoryNode) contents.elementAt(i);
	
	try
	  {
	    if (candidate.getName().equals(name))
	      {
		return candidate;
	      }
	  }
	catch (RemoteException ex)
	  {
	    throw new RuntimeException("caught remote: " + ex);
	  }
      }

    return null;
  }

  /**
   *
   * This method is used to place a Category Node under us.  The node
   * will be placed according to the node's displayOrder value, if resort
   * and/or adjustNodes are true.
   *
   * @param node Node to place under this category
   * @param sort If true, the nodes under this category will be resorted after insertion
   * @param adjustNodes If true, the nodes under this category will have their displayOrder recalculated.
   * this should not be done lightly, and not at all if any more nodes with precalculated or saved
   * displayOrder's are to be later inserted.
   *
   */

  public void addNode(CategoryNode node, boolean resort, boolean adjustNodes) 
  {
    throw new IllegalArgumentException("can't call modification methods on CategoryDump.");
  }

  /**
   *
   * This method is used to remove a Category Node from under us.
   *
   * Note that removeNode assumes that it can recalculate the
   * displayOrder values for other nodes in this category.  This
   * method should not be called if other nodes with prefixed
   * displayOrder values are still to be added to this category, as
   * from the DBStore file.
   * 
   */

  public void removeNode(CategoryNode node) 
  {
    throw new IllegalArgumentException("can't call modification methods on CategoryDump.");
  }

  /**
   *
   * This method is used to remove a Category Node from under us.
   *
   * Note that removeNode assumes that it can recalculate the
   * displayOrder values for other nodes in this category.  This
   * method should not be called if other nodes with prefixed
   * displayOrder values are still to be added to this category, as
   * from the DBStore file.
   * 
   */

  public void removeNode(String name) 
  {
    throw new IllegalArgumentException("can't call modification methods on CategoryDump.");
  }

  /**
   *
   * This creates a new subcategory under this category,
   * with displayOrder after the last item currently in the
   * category.  This method should only be called when
   * there are no nodes left to be added to the category
   * with prefixed displayOrder values.
   *
   */

  public Category newSubCategory() 
  {
    throw new IllegalArgumentException("can't call modification methods on CategoryDump.");
  }

  /**
   *
   * This method returns true if this
   * is a subcategory of cat.
   *
   */

  public boolean isUnder(Category cat) 
  {
    if (cat == null)
      {
	return false;
      }

    if (cat.equals(this))
      {
	return true;
      }

    if (parent == null)
      {
	return false;
      }
    else
      {
	return parent.isUnder(cat);
      }
  }

  /**
   *
   * Gets the order of this node in the containing category
   *
   * @see arlut.csd.ganymede.CategoryNode
   *
   */

  public int getDisplayOrder() 
  {
    return displayOrder;
  }

  /**
   *
   * Sets the order of this node in the containing category
   *
   * @see arlut.csd.ganymede.CategoryNode
   *
   */

  public void setDisplayOrder(int order) 
  {
    throw new IllegalArgumentException("can't call modification methods on CategoryDump.");
  }

  // ***
  //
  // private methods
  //
  // ***

  private String getChunk(char[] chars, int startDex)
  {
    StringBuffer result = new StringBuffer();

    /* -- */

    for (lastIndex = startDex; lastIndex < chars.length; lastIndex++)
      {
	if (chars[lastIndex] == '|')
	  {
	    lastIndex++;
	    return result.toString();
	  }
	else if (chars[lastIndex] == '\\')
	  {
	    result.append(chars[++lastIndex]);
	  }
	else
	  {
	    result.append(chars[lastIndex]);
	  }
      }

    throw new RuntimeException("Ran out of chunk data: " + result.toString());
  }
}
