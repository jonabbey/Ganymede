package com.jclark.xml.parse;

import java.io.IOException;
import java.util.Locale;
import com.jclark.xml.parse.base.Application;

/**
 * A parser for XML documents.
 * @version $Revision: 1.1 $ $Date: 2000/01/26 04:31:53 $
 */
public class DocumentParser {
  /**
   * Parses an XML document.
   *
   * @param entity the document entity of the XML document; the InputStream
   * of the document entity will be closed after parsing
   * @param entityManager the EntityManager to be used to access external
   * entities referenced in the document
   * @param application the Application which will receive information
   * about the XML document
   * @param locale the Locale to be used for error messages
   *
   * @exception NotWellFormedException if the document is not well-formed
   * @exception IOException if an IO error occurs
   * @exception ApplicationException if any of the <code>Application</code>
   * methods throw an Exception
   */
  public static void parse(OpenEntity entity,
			   EntityManager entityManager,
			   Application application,
			   Locale locale)
   throws ApplicationException, IOException {
     new EntityParser(entity, entityManager, application, locale, null)
         .parseDocumentEntity();
  }
}
