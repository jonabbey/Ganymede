/*
   GASH 2

   XMLLoadContext.java

   The GANYMEDE object storage system.

   Created: 27 March 2000
   Release: $Name:  $
   Version: $Revision: 1.1 $
   Last Mod Date: $Date: 2000/03/29 01:30:07 $
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

import java.io.*;
import java.util.*;
import java.text.*;

import arlut.csd.Util.*;	// XMLReader and attendant classes

/*------------------------------------------------------------------------------
                                                                           class
                                                                  XMLLoadContext

------------------------------------------------------------------------------*/

/**
 * <P>This class is used on the server to handle importing data from an
 * XML file.  All of the actual disk i/o is handled by the
 * {@link arlut.csd.Util.XMLReader XMLReader} class.</P>
 */

public class XMLLoadContext {

  XMLReader xmlIn;

  /* -- */
  
  public XMLLoadContext(XMLReader xmlIn)
  {
    this.xmlIn = xmlIn;
  }

  /**
   * <P>getNextItem() returns the next {@link arlut.csd.Util.XMLItem
   * XMLItem} from the {@link arlut.csd.Util.XMLReader XMLReader}'s
   * buffer.  If the background thread's parsing has fallen behind,
   * getNextItem() will block until either data is made available from
   * the parse thread, or the XMLReader is closed.</P>
   *
   * <P>getNextItem() returns null when there are no more XML elements or character
   * data to be read from the XMLReader stream.</P>
   *
   * @param skipWhiteSpaceChars if true, getNextItem() will silently eat any
   * all-whitespace character data.  
   */

  public XMLItem getNextItem(boolean skipWhiteSpace)
  {
    return xmlIn.getNextItem(skipWhiteSpace);
  }

  /**
   * <P>getNextItem() returns the next {@link arlut.csd.Util.XMLItem
   * XMLItem} from the {@link arlut.csd.Util.XMLReader XMLReader}'s
   * buffer.  If the background thread's parsing has fallen behind,
   * getNextItem() will block until either data is made available from
   * the parse thread, or the XMLReader is closed.</P>
   *
   * <P>getNextItem() returns null when there are no more XML elements or character
   * data to be read from the XMLReader stream.</P> 
   */

  public XMLItem getNextItem()
  {
    return xmlIn.getNextItem();
  }

  /**
   * <P>peekNextItem() returns the next {@link arlut.csd.Util.XMLItem
   * XMLItem} from the {@link arlut.csd.Util.XMLReader XMLReader}'s
   * buffer.  If the background thread's parsing has fallen behind,
   * peekNextItem() will block until either data is made available
   * from the parse thread, or the XMLReader is closed.</P>
   *
   * <P>peekNextItem() returns null when there are no more XML elements or character
   * data to be read from the XMLReader stream.</P>
   *
   * @param skipWhiteSpaceChars if true, peekNextItem() will silently eat any
   * all-whitespace character data.  Any all-whitespace character data eaten
   * in this way will be taken out of the XMLReader buffer, and no subsequent
   * peekNextItem() or getNextItem(), with skipWhiteSpaceChars true or false,
   * will return that item.  
   */

  public XMLItem peekNextItem(boolean skipWhiteSpaceChars)
  {
    return xmlIn.peekNextItem(skipWhiteSpaceChars);
  }

  /**
   * <P>peekNextItem() returns the next {@link arlut.csd.Util.XMLItem
   * XMLItem} from the {@link arlut.csd.Util.XMLReader XMLReader}'s
   * buffer.  If the background thread's parsing has fallen behind,
   * peekNextItem() will block until either data is made available
   * from the parse thread, or the XMLReader is closed.</P>
   *
   * <P>peekNextItem() returns null when there are no more XML elements or character
   * data to be read from the XMLReader stream.</P> 
   */

  public XMLItem peekNextItem()
  {
    return xmlIn.peekNextItem();
  }

  /**
   * <P>pushbackItem() may be used to push the most recently read
   * XMLItem back onto the {@link arlut.csd.Util.XMLReader
   * XMLReader}'s buffer.  The XMLReader code guarantees that their
   * will be room to handle a single item pushback, but two pushbacks
   * in a row with no getNextItem() call in between will cause an
   * exception to be thrown.</P> 
   */

  public void pushbackItem(XMLItem item)
  {
    xmlIn.pushbackItem(item);
  }

  /**
   * <P>This method is intended to be called in the situation where we
   * have some text between an open and close tag, as in '<open>Some string</open>'.</P>
   *
   * <P>getFollowingString() does not expect there to be any other XML 
   * elements between the open and close element in the stream.</P>
   *
   * <P>getFollowingString() expects the openElement to have already been consumed
   * from the reader at the time that it is called, and will consume the
   * close element before returning.</P>
   *
   * <P>If there is no character data between openElement and the matching closeElement,
   * null will be returned.</P>
   */

  public String getFollowingString(XMLItem openItem, boolean skipWhiteSpace)
  {
    return xmlIn.getFollowingString(openItem, skipWhiteSpace);
  }

  /**
   * <P>This method returns true if the next thing to be read in the
   * input stream is non-whitespace character data rather than an
   * open or close element tag.</P>
   *
   * <P>Calling this method has the side effect that if the next
   * data in the stream is a block of all-whitespace
   * character data, that all-whitespace character data will be
   * silently eaten.</P>
   *
   * <P>This method goes well with getFollowingString();  you can
   * call this method first to verify that the next data is indeed
   * char data, then call getFollowingString() to get all of it.</P>
   */

  public boolean isNextCharData()
  {
    return xmlIn.isNextCharData();
  }

  /**
   * <P>close() causes the {@link arlut.csd.Util.XMLReader XMLReader}
   * to terminate its operations as soon as possible.  Once close()
   * has been called, the background XML parser will terminate with a
   * SAXException the next time a SAX callback is performed.  
   */

  public void close()
  {
    xmlIn.close();
  }
}
