package com.jclark.xml.parse.awt;

import java.awt.AWTException;
import com.jclark.xml.parse.*;

/**
 * An extension of <code>Application</code> that restricts methods
 * to throwing <code>AWTException</code>.
 *
 * @version $Revision: 1.1 $ $Date: 2000/01/26 04:32:04 $
 */
public interface Application extends com.jclark.xml.parse.base.Application {
  /**
   * Reports the start of the document.
   * This is called once per well-formed document before any other methods.
   */
  void startDocument() throws AWTException;
  /**
   * Reports the end of the prolog.
   * Called before the start of the first element.
   */
  void endProlog(EndPrologEvent event) throws AWTException;

  /**
   * Reports the start of an element.
   * This includes both start-tags and empty elements.
   */
  void startElement(StartElementEvent event) throws AWTException;
  /**
   * Reports character data.
   */
  void characterData(CharacterDataEvent event) throws AWTException;
  /**
   * Reports the end of a element.
   * This includes both end-tags and empty elements.
   */
  void endElement(EndElementEvent event) throws AWTException;

  /**
   * Reports a processing instruction.
   * Note that processing instructions can occur before or after the
   * document element.
   */
  void processingInstruction(ProcessingInstructionEvent event) throws AWTException;

  /**
   * Reports the end of the document.
   * Called once per well-formed document, after all other methods.
   * Not called if the document is not well-formed.
   */
  void endDocument() throws AWTException;
  /**
   * Reports a comment.
   * Note that comments can occur before or after the
   * document element.
   */
  void comment(CommentEvent event) throws AWTException;

  /**
   * Reports the start of a CDATA section.
   */
  void startCdataSection(StartCdataSectionEvent event) throws AWTException;
  
  /**
   * Reports the end of a CDATA section.
   */
  void endCdataSection(EndCdataSectionEvent event) throws AWTException;

  /**
   * Reports the start of an entity reference.
   * This event will be followed by the result of parsing
   * the entity's replacement text.
   * This is not called for entity references in attribute values.
   */
  void startEntityReference(StartEntityReferenceEvent event) throws AWTException;

  /**
   * Reports the start of an entity reference.
   * This event follow's the result of parsing
   * the entity's replacement text.
   * This is not called for entity references in attribute values.
   */
  void endEntityReference(EndEntityReferenceEvent event) throws AWTException;

  /**
   * Reports the start of the document type declaration.
   */
  void startDocumentTypeDeclaration(StartDocumentTypeDeclarationEvent event) throws AWTException;

  /**
   * Reports the end of the document type declaration.
   */
  void endDocumentTypeDeclaration(EndDocumentTypeDeclarationEvent event) throws AWTException;
  /**
   * Reports a markup declaration.
   */
  void markupDeclaration(MarkupDeclarationEvent event) throws AWTException;
}
