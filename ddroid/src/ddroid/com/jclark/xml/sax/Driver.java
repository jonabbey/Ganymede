package com.jclark.xml.sax;

import com.jclark.xml.parse.*;
import com.jclark.xml.parse.base.*;
import java.net.URL;
import java.net.MalformedURLException;
import java.io.InputStream;
import java.io.Reader;
import java.io.IOException;
import java.util.Enumeration;
import org.xml.sax.SAXException;

/**
 * An implementation of SAX 1.0 on top of the com.jclark.xml.parse package.
 * Note that:
 * <UL>
 * <LI>the <code>Locator</code> will provide information only for
 * <code>startElement</code> and <code>processingInstruction</code>
 * events;
 * <LI>the line and column number returned by <code>Locator</code>
 * will correspond to the first character of the document event
 * not the character following the document event as specified by SAX;
 * <LI>neither a <code>Locator</code> nor a <code>SAXParseException</code>
 * will provide information about an entity's public identifier;
 * <LI>the <code>Locator</code> object will be an instance of
 * <code>com.jclark.xml.sax.Locator</code> which extends
 * <code>org.xml.sax.Locator</code>;
 * <LI>the only kind of error that is reported is a fatal error.
 * </UL>
 * @see Locator
 * @version $Revision: 1.1 $ $Date: 2000/01/26 04:32:10 $
 */
public class Driver extends ApplicationImpl
  implements org.xml.sax.Parser, EntityManager, org.xml.sax.AttributeList, Locator {
  private org.xml.sax.EntityResolver entityResolver;
  private org.xml.sax.DocumentHandler documentHandler;
  private org.xml.sax.ErrorHandler errorHandler;
  private org.xml.sax.DTDHandler dtdHandler;
  private Parser parser = new ParserImpl();

  private StartElementEvent event;
  private LocatedEvent locatedEvent;
  private static final int INIT_DATA_BUF_SIZE = 80;
  private char[] dataBuf = new char[INIT_DATA_BUF_SIZE];
  private int dataBufUsed = 0;
  private final static int UNKNOWN_INDEX = -2;
  private int idAttributeIndex;

  public Driver() {
    org.xml.sax.HandlerBase handler = new org.xml.sax.HandlerBase();
    this.documentHandler = handler;
    this.dtdHandler = handler;
    this.errorHandler = handler;
    this.entityResolver = handler;
    parser.setApplication(this);
    parser.setEntityManager(this);
  }

  public void setEntityResolver(org.xml.sax.EntityResolver resolver) {
    this.entityResolver = resolver;
  }

  public void setDocumentHandler(org.xml.sax.DocumentHandler handler) {
    this.documentHandler = handler;
  }

  public void setDTDHandler(org.xml.sax.DTDHandler handler) {
    this.dtdHandler = handler;
  }

  public void setErrorHandler(org.xml.sax.ErrorHandler handler) {
    this.errorHandler = handler;
  }

  public void setLocale(java.util.Locale locale) {
    parser.setLocale(locale);
  }

  public void parse(String systemId) throws SAXException, IOException {
    parse(new org.xml.sax.InputSource(systemId));
  }

  public void parse(org.xml.sax.InputSource in) throws SAXException, IOException {

    documentHandler.setDocumentLocator(this);
    try {
      parser.parseDocument(openInputSource(in));
    }
    catch (WrapperException e) {
      throw e.getWrapped();
    }

    catch (NotWellFormedException e) {
      errorHandler.fatalError(
	new org.xml.sax.SAXParseException(e.getMessageWithoutLocation(),
					  null,
					  e.getEntityLocation(),
					  e.getLineNumber(),
					  e.getColumnNumber()));
    }

    catch (ApplicationException e) {
      throw (SAXException)e.getException();
    }


  }

  public void startDocument() throws SAXException {
    documentHandler.startDocument();
  }

  public void startElement(StartElementEvent event) throws SAXException {
    flushData();
    this.event = event;
    this.locatedEvent = event;
    this.idAttributeIndex = UNKNOWN_INDEX;
    documentHandler.startElement(event.getName(), this);
    this.locatedEvent = null;
  }

  public void characterData(CharacterDataEvent event) {
    int need = event.getLengthMax() + dataBufUsed;
    if (need > dataBuf.length) {
      int newLength = dataBuf.length << 1;
      while (need > newLength)
	newLength <<= 1;
      char[] tem = dataBuf;
      dataBuf = new char[newLength];
      if (dataBufUsed > 0)
	System.arraycopy(tem, 0, dataBuf, 0, dataBufUsed);
    }
    dataBufUsed += event.copyChars(dataBuf, dataBufUsed);
  }

  private final void flushData() throws SAXException {
    if (dataBufUsed > 0) {
      documentHandler.characters(dataBuf, 0, dataBufUsed);
      dataBufUsed = 0;
    }
      
  }

  public void endElement(EndElementEvent event) throws SAXException {
    flushData();
    documentHandler.endElement(event.getName());
  }

  public void processingInstruction(ProcessingInstructionEvent event) throws SAXException {
    flushData();
    this.locatedEvent = event;
    documentHandler.processingInstruction(event.getName(),
					  event.getInstruction());
    this.locatedEvent = null;
  }

  public void endProlog(EndPrologEvent event) throws SAXException {
    if (dtdHandler == null)
      return;

    DTD dtd = event.getDTD();

    for (Enumeration enum = dtd.entityNames(DTD.NOTATION);
	 enum.hasMoreElements(); ) {
      String name = (String)enum.nextElement();
      Entity entity = dtd.getEntity(DTD.NOTATION, name);
      String systemId = entity.getSystemId();
      if (systemId != null) {
	try {
	  systemId = new URL(entity.getBase(), systemId).toString();
	}
	catch (MalformedURLException e) { }
      }
      dtdHandler.notationDecl(name, entity.getPublicId(), systemId);
    }
    for (Enumeration enum = dtd.entityNames(DTD.GENERAL_ENTITY);
	 enum.hasMoreElements();) {
      String name = (String)enum.nextElement();
      Entity entity = dtd.getEntity(DTD.GENERAL_ENTITY, name);
      String notationName = entity.getNotationName();
      if (notationName != null) {
	String systemId = entity.getSystemId();
	if (systemId != null) {
	  try {
	    systemId = new URL(entity.getBase(), systemId).toString();
	  }
	  catch (MalformedURLException e) { }
	}
	dtdHandler.unparsedEntityDecl(name,
				      entity.getPublicId(),
				      systemId,
				      notationName);
      }
    }
  }

  public void endDocument() throws SAXException {
    flushData();
    documentHandler.endDocument();
  }

  public OpenEntity open(String systemId, URL baseURL, String publicId) throws IOException {
    if (entityResolver != null) {
      try {
	String s = systemId;
	try {
	  s = new URL(baseURL, systemId).toString();
	}
	catch (MalformedURLException e) { }
	org.xml.sax.InputSource inputSource
	  = entityResolver.resolveEntity(publicId, s);
	if (inputSource != null)
	  return openInputSource(inputSource);
      }
      catch (SAXException e) {
	throw new WrapperException(e);
      }
    }
    URL url = new URL(baseURL, systemId);
    return new OpenEntity(url.openStream(),
			  url.toString(),
			  url,
			  null);
  }

  private OpenEntity openInputSource(org.xml.sax.InputSource inputSource) throws IOException {
    Reader reader = inputSource.getCharacterStream();
    String encoding;
    InputStream in;
    if (reader != null) {
      in = new ReaderInputStream(reader);
      encoding = "UTF-16";
    }
    else {
      in = inputSource.getByteStream();
      encoding = inputSource.getEncoding();
    }
    String systemId = inputSource.getSystemId();
    URL url = null;
    if (in == null) {
      if (systemId == null)
	return null;
      url = new URL(systemId);
      in = url.openStream();
    }
    else if (systemId != null) {
      try {
	url = new URL(systemId);
      }
      catch (MalformedURLException e) { }
    }
    else
      systemId = "(internal)";
    return new OpenEntity(in, systemId, url, encoding);
  }

  public int getLength() {
    return event.getAttributeCount();
  }

  public String getName(int i) {
    return event.getAttributeName(i);
  }

  public String getValue(int i) {
    return event.getAttributeValue(i);
  }

  public String getValue(String name) {
    return event.getAttributeValue(name);
  }

  public String getType(int i) {
    if (idAttributeIndex == UNKNOWN_INDEX)
      idAttributeIndex = event.getIdAttributeIndex();
    return i == idAttributeIndex ? "ID" : "CDATA";
  }

  public String getType(String name) {
    if (idAttributeIndex == UNKNOWN_INDEX)
      idAttributeIndex = event.getIdAttributeIndex();
    if (idAttributeIndex >= 0
	&& event.getAttributeName(idAttributeIndex).equals(name))
      return "ID";
    return "CDATA";
  }

  public String getSystemId() {
    if (locatedEvent == null)
      return null;
    return locatedEvent.getLocation().getEntityLocation();
  }

  public URL getURL() {
    if (locatedEvent == null)
      return null;
    return locatedEvent.getLocation().getEntityBase();
  }

  public String getPublicId() {
    return null;
  }

  public int getLineNumber() {
    if (locatedEvent == null)
      return -1;
    return locatedEvent.getLocation().getLineNumber();
  }

  public int getColumnNumber() {
    if (locatedEvent == null)
      return -1;
    int col = locatedEvent.getLocation().getColumnNumber();
    if (col < 0)
      return col;
    return col + 1;
  }

  public long getByteIndex() {
    if (locatedEvent == null)
      return -1;
    return locatedEvent.getLocation().getByteIndex();
  }
}
