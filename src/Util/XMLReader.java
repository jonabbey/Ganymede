/*
   GASH 2

   XMLReader.java

   The GANYMEDE object storage system.

   Created: 7 March 2000
   Release: $Name:  $
   Version: $Revision: 1.5 $
   Last Mod Date: $Date: 2000/03/09 17:09:52 $
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
 * SAX events from James Clark's XP XML parse.  These SAX events are converted
 * to {@link arlut.csd.Util.XMLItem XMLItem} objects and saved in an internal
 * buffer.  The user of the XMLReader class calls getNextItem() to retrieve
 * these XMLItem objects from the XMLReader buffer, in order of receipt.</P>
 *
 * <P>The background parse thread is throttled back as needed to avoid overflowing
 * the XMLReader's internal buffer.</P>
 */

public class XMLReader implements org.xml.sax.DocumentHandler, 
				  org.xml.sax.ErrorHandler, Runnable {

  private org.xml.sax.Parser parser;
  private org.xml.sax.InputSource inputSource;
  private org.xml.sax.Locator locator;
  private Vector buffer;
  private int bufferSize;
  private Thread inputThread;
  private boolean done = false;
  private XMLItem pushback;

  /* -- */

  public XMLReader(String xmlFilename, int bufferSize) throws IOException
  {
    parser = new com.jclark.xml.sax.Driver();
    parser.setDocumentHandler(this);

    BufferedInputStream inStream = new BufferedInputStream(new FileInputStream(xmlFilename));
    inputSource = new InputSource(inStream);

    buffer = new Vector();
    this.bufferSize = bufferSize;

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
   */

  public XMLItem getNextItem()
  {
    XMLItem value;

    /* -- */

    synchronized (buffer)
      {
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
    XMLItem value;

    /* -- */

    synchronized (buffer)
      {
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
	  }
	else
	  {
	    value = (XMLItem) buffer.elementAt(0);
	  }

	return value;
      }
  }

  /**
   * <P>pushbackItem() may be used to push the most recently read XMLItem back
   * onto the XMLReader's buffer.  The XMLReader code guarantees that their
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
   * @see org.xml.sax.Locator
   */

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

	if (done)
	  {
	    throw new SAXException("parse thread halted.. app code closed XMLReader stream.");
	  }
	
	buffer.addElement(new XMLElement(name, atts));
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

/*------------------------------------------------------------------------------
                                                                           class
                                                                         XMLItem

------------------------------------------------------------------------------*/

/**
 * <P>Abstract base class for XML data held in the 
 * {@link arlut.csd.Util.XMLReader XMLReader} class's buffer.</P>
 */

abstract class XMLItem {
}

/*------------------------------------------------------------------------------
                                                                           class
                                                                      XMLElement

------------------------------------------------------------------------------*/

/**
 * <P>Element Start class for XML data held in the 
 * {@link arlut.csd.Util.XMLReader XMLReader} class's buffer.</P>
 */

class XMLElement extends XMLItem {

  String name;
  AttributeList atts;

  /* -- */

  XMLElement(String name, AttributeList atts)
  {
    this.name = name;
    this.atts = atts;
  }

  public String getName()
  {
    return name;
  }

  public AttributeList getAttributeList()
  {
    return atts;
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                                 XMLCloseElement

------------------------------------------------------------------------------*/

/**
 * <P>Element Close class for XML data held in the 
 * {@link arlut.csd.Util.XMLReader XMLReader} class's buffer.</P>
 */

class XMLCloseElement extends XMLItem {

  String name;

  /* -- */

  XMLCloseElement(String name)
  {
    this.name = name;
  }

  public String getName()
  {
    return name;
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                                  XMLEndDocument

------------------------------------------------------------------------------*/

/**
 * <P>Document Close class for XML data held in the 
 * {@link arlut.csd.Util.XMLReader XMLReader} class's buffer.</P>
 */

class XMLEndDocument extends XMLItem {

  XMLEndDocument()
  {
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                                     XMLCharData

------------------------------------------------------------------------------*/

/**
 * <P>Character Data class for XML data held in the 
 * {@link arlut.csd.Util.XMLReader XMLReader} class's buffer.</P>
 */

class XMLCharData extends XMLItem {

  String data;

  /* -- */

  XMLCharData(char ch[], int start, int length)
  {
    data = new String(ch, start, length);
  }

  public String getString()
  {
    return data;
  }

  /**
   * <P>This method returns the character data for this XMLItem with
   * leading and trailing whitespace filtered out.</P>
   */

  public String getCleanString()
  {
    return data.trim();
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                                        XMLError

------------------------------------------------------------------------------*/

/**
 * <P>Error class for XML data held in the 
 * {@link arlut.csd.Util.XMLReader XMLReader} class's buffer.</P>
 */

class XMLError extends XMLItem {

  String error;
  int lineNumber;
  int columnNumber;
  boolean fatal;

  /* -- */

  XMLError(SAXParseException exception, org.xml.sax.Locator locator, boolean fatal)
  {
    error = exception.getMessage();
    lineNumber = locator.getLineNumber();
    columnNumber = locator.getColumnNumber();
    this.fatal = fatal;
  }

  public boolean isFatal()
  {
    return fatal;
  }

  public String getMessage()
  {
    return error;
  }

  public int getLineNumber()
  {
    return lineNumber;
  }

  public int getColumnNumber()
  {
    return columnNumber;
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                                      XMLWarning

------------------------------------------------------------------------------*/

/**
 * <P>Warning class for XML data held in the 
 * {@link arlut.csd.Util.XMLReader XMLReader} class's buffer.</P>
 */

class XMLWarning extends XMLItem {

  String error;
  int lineNumber;
  int columnNumber;

  /* -- */

  XMLWarning(SAXParseException exception, org.xml.sax.Locator locator)
  {
    error = exception.getMessage();
    lineNumber = locator.getLineNumber();
    columnNumber = locator.getColumnNumber();
  }

  public String getMessage()
  {
    return error;
  }

  public int getLineNumber()
  {
    return lineNumber;
  }

  public int getColumnNumber()
  {
    return columnNumber;
  }
}
