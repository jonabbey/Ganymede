/*

   BaseListTransport.java

   This class is intended to provide a serializable object that
   can be used to bulk-dump a static description of the object
   types on the server to the client.
   
   Created: 2 March 1998
   Version: $Revision: 1.2 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

import java.util.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                               BaseListTransport

------------------------------------------------------------------------------*/

/**
 *
 * This class is intended to provide a serializable object that
 * can be used to bulk-dump a static description of the category
 * and base structures on the server to the client.
 *
 */

public class BaseListTransport implements java.io.Serializable {

  static final long serialVersionUID = -5281402897372108079L;

  // ---

  private StringBuffer buffer;
  private transient int lastIndex = 0;
  private transient GanymedeSession session;

  /* -- */

  /**
   *
   * Server side constructor for the full category tree
   *
   */

  public BaseListTransport(GanymedeSession session)
  {
    Enumeration bases;
    DBObjectBase base;

    /* -- */

    this.session = session;

    synchronized (Ganymede.db)
      {
	bases = Ganymede.db.objectBases.elements();

	while (bases.hasMoreElements())
	  {
	    base = (DBObjectBase) bases.nextElement();
	    addBaseInfo(base);
	  }
      }
  }

  /**
   *
   * Client side accessor
   *
   */

  public Vector getBaseList()
  {
    String token;
    BaseDump baseChild;
    char[] src;
    Vector results = new Vector();

    /* -- */

    src = buffer.toString().toCharArray();
    lastIndex = 0;

    while (lastIndex < src.length)
      {
	token = getChunk(src, lastIndex);

	if (!token.equals("base"))
	  {
	    throw new RuntimeException("buffer format exception");
	  }

	baseChild = new BaseDump(this, src, lastIndex);
	lastIndex = baseChild.getLastIndex();
	results.addElement(baseChild);
      }

    return results;
  }

  // ***
  //
  // private methods, server side
  //
  // ***

  private void addBaseInfo(DBObjectBase node)
  {
    addChunk("base");
    addChunk(node.getName());
    addChunk(String.valueOf(node.getTypeID()));
    addChunk(String.valueOf(node.getLabelField()));
    addChunk(node.getLabelFieldName());
    addChunk(String.valueOf(node.canInactivate()));
    addChunk(String.valueOf(node.canCreate(session)));
    addChunk(String.valueOf(node.isEmbedded()));
    addChunk(String.valueOf(node.getDisplayOrder()));
  }

  private void addChunk(String text)
  {
    char[] chars;

    /* -- */

    //    System.err.println("Server adding chunk " + label + ":" + operand);

    if (buffer == null)
      {
	buffer = new StringBuffer();
      }

    // add our label

    if (text != null)
      {
	chars = text.toCharArray();
      }
    else
      {
	buffer.append("|");
	return;
      }
	
    for (int j = 0; j < chars.length; j++)
      {
	if (chars[j] == '|')
	  {
	    buffer.append("\\|");
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
