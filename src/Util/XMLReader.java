/*
   GASH 2

   XMLReader.java

   The GANYMEDE object storage system.

   Created: 7 March 2000
   Release: $Name:  $
   Version: $Revision: 1.16 $
   Last Mod Date: $Date: 2000/06/15 04:43:21 $
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

package arlut.csd.Util;

import java.util.Vector;
import java.io.*;
import org.xml.sax.*;
import com.jclark.xml.sax.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                       XMLReader

------------------------------------------------------------------------------*/

/**
 * <P>This class is intended to serve as a stream-oriented proxy, allowing
 * the Ganymede server to read XML entity and character data from a SAX parser
 * entity by entity, rather than through the use of a callback interface, as is
 * traditionally done with SAX.</P>
 *
 * <P>When instantiated, the XMLReader creates a background thread that receives
 * SAX events from James Clark's XP XML parser.  These SAX events are converted
 * to {@link arlut.csd.Util.XMLItem XMLItem} objects and saved in an internal
 * buffer.  The user of the XMLReader class calls getNextItem() to retrieve
 * these XMLItem objects from the XMLReader buffer, in order of receipt.</P>
 *
 * <P>The background parse thread is throttled back as needed to avoid overflowing
 * the XMLReader's internal buffer.</P>
 */

public class XMLReader implements org.xml.sax.DocumentHandler, 
				  org.xml.sax.ErrorHandler, Runnable {
  
  public final static boolean debug = false;

  private org.xml.sax.Parser parser;
  private org.xml.sax.InputSource inputSource;
  private org.xml.sax.Locator locator;
  private Vector buffer;
  private int bufferSize;
  private Thread inputThread;
  private boolean done = false;
  private XMLItem pushback;
  private XMLElement halfElement;
  private boolean skipWhiteSpace;

  /* -- */

  /**
   * @param xmlFilename Name of the file to read
   * @param bufferSize How many items the XMLReader will buffer in its
   * data structures at one time
   * @param skipWhiteSpace If true, the no-param getNextItem() and peekNextItem()
   * methods will jump over any all-whitespace character data between other
   * elements.
   */

  public XMLReader(String xmlFilename, int bufferSize, boolean skipWhiteSpace) throws IOException
  {
    parser = new com.jclark.xml.sax.Driver();
    parser.setDocumentHandler(this);

    BufferedInputStream inStream = new BufferedInputStream(new FileInputStream(xmlFilename));
    inputSource = new InputSource(inStream);

    buffer = new Vector();
    this.bufferSize = bufferSize;
    this.skipWhiteSpace = skipWhiteSpace;

    inputThread = new Thread(this);

    inputThread.start();
  }

  /**
   * <P>getNextItem() returns the next {@link arlut.csd.Util.XMLItem XMLItem}
   * from the XMLReader's buffer.  If the background thread's parsing has fallen
   * behind, getNextItem() will block until either data is made available from
   * the parse thread, or the XMLReader is closed.</P>
   *
   * <P>getNextItem() returns null when there are no more XML elements or character
   * data to be read from the XMLReader stream.</P>
   *
   * @param skipWhiteSpaceChars if true, getNextItem() will silently eat any
   * all-whitespace character data.
   */

  public XMLItem getNextItem(boolean skipWhiteSpaceChars)
  {
    XMLItem value = null;
    boolean finished = false;

    /* -- */

    synchronized (buffer)
      {
	while (!finished)
	  {
	    finished = true;	// assume we won't be seeing whitespace chars

	    while (!done && pushback == null && buffer.size() == 0)
	      {
		try
		  {
		    buffer.wait();
		  }
		catch (InterruptedException ex)
		  {
		    throw new RuntimeException("interrupted, can't wait for buffer to fill.");
		  }
	      }

	    if (done && pushback == null && buffer.size() == 0)
	      {
		return null;
	      }

	    if (pushback != null)
	      {
		value = pushback;
		pushback = null;
	      }
	    else
	      {
		value = (XMLItem) buffer.elementAt(0);
		buffer.removeElementAt(0);
		buffer.notifyAll();
	      }

	    if (skipWhiteSpaceChars)
	      {
		// if we are skipping all-whitespace XMLCharData, we'll set
		// finished to false if containsNonWhitespace() returns false.

		if (value instanceof XMLCharData)
		  {
		    finished = ((XMLCharData) value).containsNonWhitespace();
		  }
	      }
	  }

	if (debug)
	  {
	    System.err.println("XMLReader.getNextItem() returning " + value);
	  }

	return value;
      }
  }

  /**
   * <P>getNextItem() returns the next {@link arlut.csd.Util.XMLItem XMLItem}
   * from the XMLReader's buffer.  If the background thread's parsing has fallen
   * behind, getNextItem() will block until either data is made available from
   * the parse thread, or the XMLReader is closed.</P>
   *
   * <P>getNextItem() returns null when there are no more XML elements or character
   * data to be read from the XMLReader stream.</P>
   */

  public XMLItem getNextItem()
  {
    return getNextItem(this.skipWhiteSpace);
  }

  /**
   * <P>peekNextItem() returns the next {@link arlut.csd.Util.XMLItem XMLItem}
   * from the XMLReader's buffer.  If the background thread's parsing has fallen
   * behind, peekNextItem() will block until either data is made available from
   * the parse thread, or the XMLReader is closed.</P>
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
    XMLItem value = null;
    boolean finished = false;

    /* -- */

    synchronized (buffer)
      {
	while (!finished)
	  {
	    finished = true;	// unless we eat whitespace

	    // wait until there's data to be had

	    while (!done && pushback == null && buffer.size() == 0)
	      {
		try
		  {
		    buffer.wait();
		  }
		catch (InterruptedException ex)
		  {
		    throw new RuntimeException("interrupted, can't wait for buffer to fill.");
		  }
	      }

	    // if we're out of data and there will be no more, exit

	    if (done && pushback == null && buffer.size() == 0)
	      {
		return null;
	      }

	    // identify the next value

	    if (pushback != null)
	      {
		value = pushback;
	      }
	    else
	      {
		value = (XMLItem) buffer.elementAt(0);
	      }

	    if (skipWhiteSpaceChars)
	      {
		if ((value instanceof XMLCharData) &&
		    !((XMLCharData) value).containsNonWhitespace())
		  {
		    getNextItem(false);	// consume the whitespace
		    finished = false; // loop again
		  }
	      }
	  } // while (!finished)

	if (debug)
	  {
	    System.err.println("XMLReader.peekNextItem() returning " + value);
	  }

	return value;
      }
  }

  /**
   * <P>peekNextItem() returns the next {@link arlut.csd.Util.XMLItem XMLItem}
   * from the XMLReader's buffer.  If the background thread's parsing has fallen
   * behind, peekNextItem() will block until either data is made available from
   * the parse thread, or the XMLReader is closed.</P>
   *
   * <P>peekNextItem() returns null when there are no more XML elements or character
   * data to be read from the XMLReader stream.</P>
   */

  public XMLItem peekNextItem()
  {
    return peekNextItem(this.skipWhiteSpace);
  }

  /**
   * <P>pushbackItem() may be used to push the most recently read XMLItem back
   * onto the XMLReader's buffer.  The XMLReader code guarantees that there
   * will be room to handle a single item pushback, but two pushbacks in a row
   * with no getNextItem() call in between will cause an exception to be thrown.</P>
   */

  public void pushbackItem(XMLItem item)
  {
    synchronized (buffer)
      {
	if (pushback != null)
	  {
	    throw new RuntimeException("can't pushback.. buffer overflow");
	  }

	pushback = item;
	buffer.notifyAll();	// in case we have multiple threads consuming
      }
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
    String result = null;
    XMLElement openElement;
    String tagName;
    XMLItem nextItem;

    /* -- */

    if (!(openItem instanceof XMLElement))
      {
	throw new IllegalArgumentException("getFollowingString() needs to be given an XMLElement.");
      }

    openElement = (XMLElement) openItem;

    // if we have no character data between the open and close tag,
    // the reader will have reported the openItem as being an empty
    // element.

    if (openElement.isEmpty())
      {
	return null;
      }

    // okay, we know there's something before we get to the close
    // element..  handle it.

    tagName = openElement.getName();
    nextItem = getNextItem(skipWhiteSpace);

    if (nextItem instanceof XMLCharData)
      {
	if (skipWhiteSpace)
	  {
	    result = nextItem.getCleanString();
	  }
	else
	  {
	    result = nextItem.getString();
	  }
      }

    // and get to the close tag, skipping over whatever gets in our
    // way

    while (nextItem != null && !nextItem.matchesClose(tagName))
      {
	//System.err.println(">>> " + tagName + " seeking: " + nextItem);
	nextItem = getNextItem(skipWhiteSpace);
      }
    
    if (nextItem == null)
      {
	throw new IllegalArgumentException("unexpected end of stream");
      }

    return result;
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
    XMLItem next = peekNextItem(true);

    return next instanceof XMLCharData;
  }

  /**
   * <P>close() causes the XMLReader to terminate its operations as soon
   * as possible.  Once close() has been called, the background XML parser
   * will terminate with a SAXException the next time a SAX callback is
   * performed.
   */

  public void close()
  {
    synchronized (buffer)
      {
	done = true;
	buffer.notifyAll();	// to wake up any sleepers if the buffer is full
      }
  }

  public void run()
  {
    try
      {
	parser.parse(inputSource);
      }
    catch (SAXException ex)
      {
	close();
	ex.printStackTrace();
	throw new RuntimeException("XMLReader parse error: " + ex.getMessage());
      }
    catch (IOException ex)
      {
	close();
	ex.printStackTrace();
	throw new RuntimeException("XMLReader io error: " + ex.getMessage());
      }
  }

  /**
   * <p>This is a private helper method used to move a completed
   * halfElement XMLElement (which stays half-completed until we know
   * whether the SAX parser will give us an immediately following
   * close element, in which case we want to mark the halfElement as
   * empty and eat the subsequent close) into the XMLReader's primary
   * buffer.</p>
   */

  private final void completeElement() throws SAXException
  {
    if (halfElement != null)
      {
	buffer.addElement(halfElement);

	halfElement = null;
	
	buffer.notifyAll();
      }
  }

  /**
   * <p>The locator allows the application to determine the end
   * position of any document-related event, even if the parser is
   * not reporting an error.  Typically, the application will
   * use this information for reporting its own errors (such as
   * character content that does not match an application's
   * business rules).  The information returned by the locator
   * is probably not sufficient for use with a search engine.</p>
   *
   * <p>Note that the locator will return correct information only
   * during the invocation of the events in this interface.  The
   * application should not attempt to use it at any other time.</p>
   *
   * @param locator An object that can return the location of
   *                any SAX document event.
   * @see org.xml.sax.Locator */

  public void setDocumentLocator(org.xml.sax.Locator locator)
  {
    this.locator = locator;
  }

  /**
   * Receive notification of the beginning of a document.
   *
   * <p>The SAX parser will invoke this method only once, before any
   * other methods in this interface or in DTDHandler (except for
   * setDocumentLocator).</p>
   *
   * @exception org.xml.sax.SAXException Any SAX exception, possibly
   *            wrapping another exception.
   */

  public void startDocument() throws SAXException
  {
    synchronized (buffer)
      {
	while (!done && buffer.size() >= bufferSize)
	  {
	    try
	      {
		buffer.wait();
	      }
	    catch (InterruptedException ex)
	      {
		throw new SAXException("parse thread interrupted, can't wait for buffer to drain.");
	      }
	  }

	completeElement();

	if (done)
	  {
	    throw new SAXException("parse thread halted.. app code closed XMLReader stream.");
	  }
	
	buffer.addElement(new XMLStartDocument());
	buffer.notifyAll();
      }
  }

  /**
   * Receive notification of the end of a document.
   *
   * <p>The SAX parser will invoke this method only once, and it will
   * be the last method invoked during the parse.  The parser shall
   * not invoke this method until it has either abandoned parsing
   * (because of an unrecoverable error) or reached the end of
   * input.</p>
   *
   * @exception org.xml.sax.SAXException Any SAX exception, possibly
   *            wrapping another exception.
   */

  public void endDocument() throws SAXException
  {
    // note that the XML parser will close the input stream as needed
    // when the parser finishes.

    synchronized (buffer)
      {
	while (!done && buffer.size() >= bufferSize)
	  {
	    try
	      {
		buffer.wait();
	      }
	    catch (InterruptedException ex)
	      {
		throw new SAXException("parse thread interrupted, can't wait for buffer to drain.");
	      }
	  }

	completeElement();

	if (done)
	  {
	    throw new SAXException("parse thread halted.. app code closed XMLReader stream.");
	  }
	
	buffer.addElement(new XMLEndDocument());
	done = true;
	buffer.notifyAll();
      }
  }

  /**
   * Receive notification of the beginning of an element.
   *
   * <p>The Parser will invoke this method at the beginning of every
   * element in the XML document; there will be a corresponding
   * endElement() event for every startElement() event (even when the
   * element is empty). All of the element's content will be
   * reported, in order, before the corresponding endElement()
   * event.</p>
   *
   * <p>If the element name has a namespace prefix, the prefix will
   * still be attached.  Note that the attribute list provided will
   * contain only attributes with explicit values (specified or
   * defaulted): #IMPLIED attributes will be omitted.</p>
   *
   * @param name The element type name.
   * @param atts The attributes attached to the element, if any.
   * @exception org.xml.sax.SAXException Any SAX exception, possibly
   *            wrapping another exception.
   * @see #endElement
   * @see org.xml.sax.AttributeList 
   */

  public void startElement(String name, AttributeList atts) throws SAXException
  {
    synchronized (buffer)
      {
	while (!done && buffer.size() >= bufferSize)
	  {
	    try
	      {
		buffer.wait();
	      }
	    catch (InterruptedException ex)
	      {
		throw new SAXException("parse thread interrupted, can't wait for buffer to drain.");
	      }
	  }

	completeElement();

	if (done)
	  {
	    throw new SAXException("parse thread halted.. app code closed XMLReader stream.");
	  }
	
	halfElement = new XMLElement(name, atts);

	buffer.notifyAll();
      }
  }

  /**
   * Receive notification of the end of an element.
   *
   * <p>The SAX parser will invoke this method at the end of every
   * element in the XML document; there will be a corresponding
   * startElement() event for every endElement() event (even when the
   * element is empty).</p>
   *
   * <p>If the element name has a namespace prefix, the prefix will
   * still be attached to the name.</p>
   *
   * @param name The element type name
   * @exception org.xml.sax.SAXException Any SAX exception, possibly
   *            wrapping another exception.
   */

  public void endElement(String name) throws SAXException
  {
    synchronized (buffer)
      {
	while (!done && buffer.size() >= bufferSize)
	  {
	    try
	      {
		buffer.wait();
	      }
	    catch (InterruptedException ex)
	      {
		throw new SAXException("parse thread interrupted, can't wait for buffer to drain.");
	      }
	  }

	if (halfElement != null && halfElement.matches(name))
	  {
	    halfElement.setEmpty();
	    completeElement();
	    return;
	  }

	completeElement();
	
	if (done)
	  {
	    throw new SAXException("parse thread halted.. app code closed XMLReader stream.");
	  }
	
	buffer.addElement(new XMLCloseElement(name));
	buffer.notifyAll();
      }
  }

  /**
   * Receive notification of character data.
   *
   * <p>The Parser will call this method to report each chunk of
   * character data.  SAX parsers may return all contiguous character
   * data in a single chunk, or they may split it into several
   * chunks; however, all of the characters in any single event
   * must come from the same external entity, so that the Locator
   * provides useful information.</p>
   *
   * <p>The application must not attempt to read from the array
   * outside of the specified range.</p>
   *
   * <p>Note that some parsers will report whitespace using the
   * ignorableWhitespace() method rather than this one (validating
   * parsers must do so).</p>
   *
   * @param ch The characters from the XML document.
   * @param start The start position in the array.
   * @param length The number of characters to read from the array.
   * @exception org.xml.sax.SAXException Any SAX exception, possibly
   *            wrapping another exception.
   * @see #ignorableWhitespace 
   * @see org.xml.sax.Locator
   */

  public void characters(char ch[], int start, int length) throws SAXException
  {
    synchronized (buffer)
      {
	while (!done && buffer.size() >= bufferSize)
	  {
	    try
	      {
		buffer.wait();
	      }
	    catch (InterruptedException ex)
	      {
		throw new SAXException("parse thread interrupted, can't wait for buffer to drain.");
	      }
	  }

	completeElement();

	if (done)
	  {
	    throw new SAXException("parse thread halted.. app code closed XMLReader stream.");
	  }
	
	buffer.addElement(new XMLCharData(ch, start, length));
	buffer.notifyAll();
      }
  }

  /**
   * Receive notification of ignorable whitespace in element content.
   *
   * <p>Validating Parsers must use this method to report each chunk
   * of ignorable whitespace (see the W3C XML 1.0 recommendation,
   * section 2.10): non-validating parsers may also use this method
   * if they are capable of parsing and using content models.</p>
   *
   * <p>SAX parsers may return all contiguous whitespace in a single
   * chunk, or they may split it into several chunks; however, all of
   * the characters in any single event must come from the same
   * external entity, so that the Locator provides useful
   * information.</p>
   *
   * <p>The application must not attempt to read from the array
   * outside of the specified range.</p>
   *
   * @param ch The characters from the XML document.
   * @param start The start position in the array.
   * @param length The number of characters to read from the array.
   * @exception org.xml.sax.SAXException Any SAX exception, possibly
   *            wrapping another exception.
   * @see #characters
   */

  public void ignorableWhitespace(char ch[], int start, int length) throws SAXException
  {
    synchronized (buffer)
      {
	while (!done && buffer.size() >= bufferSize)
	  {
	    try
	      {
		buffer.wait();
	      }
	    catch (InterruptedException ex)
	      {
		throw new SAXException("parse thread interrupted, can't wait for buffer to drain.");
	      }
	  }

	completeElement();

	if (done)
	  {
	    throw new SAXException("parse thread halted.. app code closed XMLReader stream.");
	  }
	
	buffer.addElement(new XMLCharData(ch, start, length));
	buffer.notifyAll();
      }
  }

  /**
   * Receive notification of a processing instruction.
   *
   * <p>The Parser will invoke this method once for each processing
   * instruction found: note that processing instructions may occur
   * before or after the main document element.</p>
   *
   * <p>A SAX parser should never report an XML declaration (XML 1.0,
   * section 2.8) or a text declaration (XML 1.0, section 4.3.1)
   * using this method.</p>
   *
   * @param target The processing instruction target.
   * @param data The processing instruction data, or null if
   *        none was supplied.
   * @exception org.xml.sax.SAXException Any SAX exception, possibly
   *            wrapping another exception.
   */

  public void processingInstruction(String target, String data) throws SAXException
  {
  }

  /**
   * Receive notification of a warning.
   *
   * <p>SAX parsers will use this method to report conditions that
   * are not errors or fatal errors as defined by the XML 1.0
   * recommendation.  The default behaviour is to take no action.</p>
   *
   * <p>The SAX parser must continue to provide normal parsing events
   * after invoking this method: it should still be possible for the
   * application to process the document through to the end.</p>
   *
   * @param exception The warning information encapsulated in a
   *                  SAX parse exception.
   * @exception org.xml.sax.SAXException Any SAX exception, possibly
   *            wrapping another exception.
   * @see org.xml.sax.SAXParseException 
   */

  public void warning(SAXParseException exception) throws SAXException
  {
    synchronized (buffer)
      {
	while (!done && buffer.size() >= bufferSize)
	  {
	    try
	      {
		buffer.wait();
	      }
	    catch (InterruptedException ex)
	      {
		throw new SAXException("parse thread interrupted, can't wait for buffer to drain.");
	      }
	  }

	completeElement();

	if (done)
	  {
	    throw new SAXException("parse thread halted.. app code closed XMLReader stream.");
	  }
	
	buffer.addElement(new XMLWarning(exception, locator));
	buffer.notifyAll();
      }
  }

  /**
   * Receive notification of a recoverable error.
   *
   * <p>This corresponds to the definition of "error" in section 1.2
   * of the W3C XML 1.0 Recommendation.  For example, a validating
   * parser would use this callback to report the violation of a
   * validity constraint.  The default behaviour is to take no
   * action.</p>
   *
   * <p>The SAX parser must continue to provide normal parsing events
   * after invoking this method: it should still be possible for the
   * application to process the document through to the end.  If the
   * application cannot do so, then the parser should report a fatal
   * error even if the XML 1.0 recommendation does not require it to
   * do so.</p>
   *
   * @param exception The error information encapsulated in a
   *                  SAX parse exception.
   * @exception org.xml.sax.SAXException Any SAX exception, possibly
   *            wrapping another exception.
   * @see org.xml.sax.SAXParseException 
   */

  public void error(SAXParseException exception) throws SAXException
  {
    synchronized (buffer)
      {
	while (!done && buffer.size() >= bufferSize)
	  {
	    try
	      {
		buffer.wait();
	      }
	    catch (InterruptedException ex)
	      {
		throw new SAXException("parse thread interrupted, can't wait for buffer to drain.");
	      }
	  }

	completeElement();

	if (done)
	  {
	    throw new SAXException("parse thread halted.. app code closed XMLReader stream.");
	  }
	
	buffer.addElement(new XMLError(exception, locator, false));
	buffer.notifyAll();
      }
  }

  /**
   * Receive notification of a non-recoverable error.
   *
   * <p>This corresponds to the definition of "fatal error" in
   * section 1.2 of the W3C XML 1.0 Recommendation.  For example, a
   * parser would use this callback to report the violation of a
   * well-formedness constraint.</p>
   *
   * <p>The application must assume that the document is unusable
   * after the parser has invoked this method, and should continue
   * (if at all) only for the sake of collecting addition error
   * messages: in fact, SAX parsers are free to stop reporting any
   * other events once this method has been invoked.</p>
   *
   * @param exception The error information encapsulated in a
   *                  SAX parse exception.  
   * @exception org.xml.sax.SAXException Any SAX exception, possibly
   *            wrapping another exception.
   * @see org.xml.sax.SAXParseException
   */

  public void fatalError(SAXParseException exception) throws SAXException
  {
    synchronized (buffer)
      {
	while (!done && buffer.size() >= bufferSize)
	  {
	    try
	      {
		buffer.wait();
	      }
	    catch (InterruptedException ex)
	      {
		throw new SAXException("parse thread interrupted, can't wait for buffer to drain.");
	      }
	  }

	completeElement();

	if (done)
	  {
	    throw new SAXException("parse thread halted.. app code closed XMLReader stream.");
	  }
	
	buffer.addElement(new XMLError(exception, locator, true));
	done = true;
	buffer.notifyAll();
      }
  }
}
