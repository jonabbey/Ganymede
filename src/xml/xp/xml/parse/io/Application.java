package com.jclark.xml.parse.io;

import java.io.IOException;
import com.jclark.xml.parse.*;

/**
 * An extension of <code>Application</code> that restricts
 * methods to throwing <code>IOException</code>.
 *
 * @version $Revision: 1.1 $ $Date: 2000/01/26 04:32:08 $
 */
public interface Application extends com.jclark.xml.parse.base.Application {
  /**
   * Reports the start of the document.
   * This is called once per well-formed document before any other methods.
   */
  void startDocument() throws IOException;
  /**
   * Reports the end of the prolog.
   * Called before the start of the first element.
   */
  void endProlog(EndPrologEvent event) throws IOException;

  /**
   * Reports the start of an element.
   * This includes both start-tags and empty elements.
   */
  void startElement(StartElementEvent event) throws IOException;
  /**
   * Reports character data.
   */
  void characterData(CharacterDataEvent event) throws IOException;
  /**
   * Reports the end of a element.
   * This includes both end-tags and empty elements.
   */
  void endElement(EndElementEvent event) throws IOException;

  /**
   * Reports a processing instruction.
   * Note that processing instructions can occur before or after the
   * document element.
   */
  void processingInstruction(ProcessingInstructionEvent event) throws IOException;

  /**
   * Reports the end of the document.
   * Called once per well-formed document, after all other methods.
   * Not called if the document is not well-formed.
   */
  void endDocument() throws IOException;

  /**
   * Reports a comment.
   * Note that comments can occur before or after the
   * document element.
   */
  void comment(CommentEvent event) throws IOException;

  /**
   * Reports the start of a CDATA section.
   */
  void startCdataSection(StartCdataSectionEvent event) throws IOException;
  
  /**
   * Reports the end of a CDATA section.
   */
  void endCdataSection(EndCdataSectionEvent event) throws IOException;

  /**
   * Reports the start of an entity reference.
   * This event will be followed by the result of parsing
   * the entity's replacement text.
   * This is not called for entity references in attribute values.
   */
  void startEntityReference(StartEntityReferenceEvent event) throws IOException;

  /**
   * Reports the start of an entity reference.
   * This event follow's the result of parsing
   * the entity's replacement text.
   * This is not called for entity references in attribute values.
   */
  void endEntityReference(EndEntityReferenceEvent event) throws IOException;

  /**
   * Reports the start of the document type declaration.
   */
  void startDocumentTypeDeclaration(StartDocumentTypeDeclarationEvent event) throws IOException;

  /**
   * Reports the end of the document type declaration.
   */
  void endDocumentTypeDeclaration(EndDocumentTypeDeclarationEvent event) throws IOException;
  /**
   * Reports a markup declaration.
   */
  void markupDeclaration(MarkupDeclarationEvent event) throws IOException;
}
