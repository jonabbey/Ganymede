/*

   BaseListTransport.java

   This class is intended to provide a serializable object that
   can be used to bulk-dump a static description of the object
   types on the server to the client.
   
   Created: 2 March 1998
   Release: $Name:  $
   Version: $Revision: 1.7 $
   Last Mod Date: $Date: 2000/11/21 12:57:23 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999, 2000
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
   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.

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
  private transient Object session;

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

    // we sync on Ganymede.db to make sure that no one adds or deletes
    // any object bases while we're creating our BaseListTransport.
    // We could use the loginSemaphore, but that would be a bit heavy
    // for our purposes here.

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
    addChunk(node.getPath());
    addChunk(String.valueOf(node.getTypeID()));
    addChunk(String.valueOf(node.getLabelField()));
    addChunk(node.getLabelFieldName());
    addChunk(String.valueOf(node.canInactivate()));
    addChunk(String.valueOf(node.canCreate(((GanymedeSession) session))));
    addChunk(String.valueOf(node.isEmbedded()));
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
