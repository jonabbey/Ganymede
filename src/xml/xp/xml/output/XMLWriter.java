package com.jclark.xml.output;

import java.io.Writer;
import java.io.IOException;

/**
 * An extension of <code>Writer</code> for writing XML documents.
 * The normal <code>write</code> methods write character data,
 * automatically escaping markup characters.
 * @version $Revision: 1.1 $ $Date: 2000/01/26 04:31:49 $
 */

public abstract class XMLWriter extends Writer {

  protected XMLWriter(Object lock) {
    super(lock);
  }

  /**
   * Starts an element.
   * This may be followed by zero or more calls to <code>attribute</code>.
   * The start-tag will be closed by the first following call to any method
   * other than <code>attribute</code>.
   */
  public abstract void startElement(String name) throws IOException;

  /**
   * Writes an attribute.
   * This is not legal if there have been calls to methods other than
   * <code>attribute</code> since the last call to <code>startElement</code>,
   * unless inside a <code>startAttribute</code>, <code>endAttribute</code>
   * pair.
   */
  public abstract void attribute(String name, String value) throws IOException;

  /**
   * Starts an attribute.
   * This writes the attribute name, <code>=</code> and the opening
   * quote.
   * This provides an alternative to <code>attribute</code>
   * that allows markup to be included in the attribute value.
   * The value of the attribute is written using the normal
   * <code>write</code> methods;
   * <code>endAttribute</code> must be called at the end
   * of the attribute value.
   * Entity and character references can be written using
   * <code>entityReference</code> and <code>characterReference</code>.
   */
  public abstract void startAttribute(String name) throws IOException;

  /**
   * Ends an attribute.
   * This writes the closing quote of the attribute value.
   */
  public abstract void endAttribute() throws IOException;

  /**
   * Ends an element.
   * This may output an end-tag or close the current start-tag as an
   * empty element.
   */
  public abstract void endElement(String name) throws IOException;

  /**
   * Writes a processing instruction.
   * If <code>data</code> is non-empty a space will be inserted automatically
   * to separate it from the <code>target</code>.
   */
  public abstract void processingInstruction(String target, String data) throws IOException;

  /**
   * Writes a comment.
   */
  public abstract void comment(String body) throws IOException;

  /**
   * Writes an entity reference.
   */
  public abstract void entityReference(boolean isParam, String name) throws IOException;

  /**
   * Writes a character reference.
   */
  public abstract void characterReference(int n) throws IOException;

  /**
   * Writes a CDATA section.
   */
  public abstract void cdataSection(String content) throws IOException;

  /**
   * Starts the replacement text for an internal entity.
   * The replacement text must be ended with
   * <code>endReplacementText</code>.
   * This enables an extra level of escaping that protects
   * against the process of constructing an entity's replacement
   * text from the literal entity value.
   * See Section 4.5 of the XML Recommendation.
   * Between a call to <code>startReplacementText</code>
   * and <code>endReplacementText</code>, the argument to
   * <code>markup</code> would specify entity replacement text;
   * these would be escaped so that when processed as
   * a literal entity value, the specified entity replacement text
   * would be constructed.
   * This call does not itself cause anything to be written.
   */
  public abstract void startReplacementText() throws IOException;

  /**
   * Ends the replacement text for an internal entity.
   * This disables the extra level of escaping enabled by
   * <code>startReplacementText</code>.
   * This call does not itself cause anything to be written.
   */
  public abstract void endReplacementText() throws IOException;

  /**
   * Writes markup.  The characters in the string will be written as is
   * without being escaped (except for any escaping enabled by
   * <code>startReplacementText</code>).
   */
  public abstract void markup(String str) throws IOException;
}
