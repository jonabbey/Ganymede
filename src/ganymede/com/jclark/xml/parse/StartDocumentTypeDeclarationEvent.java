package com.jclark.xml.parse;

/**
 * Information about the start of a document type declaration.
 * @see com.jclark.xml.parse.base.Application#startDocumentTypeDeclaration
 * @version $Revision: 1.1 $ $Date: 2000/01/26 04:32:02 $
 */
public interface StartDocumentTypeDeclarationEvent {
  /**
   * Returns the DTD being declared.
   */
  DTD getDTD();
}
