package com.jclark.xml.parse;

/**
 * Information about the end of a document type declaration.
 * @see com.jclark.xml.parse.base.Application#endDocumentTypeDeclaration
 * @version $Revision: 1.1 $ $Date: 2000/01/26 04:31:54 $
 */
public interface EndDocumentTypeDeclarationEvent {
  /**
   * Returns the DTD that was declared.
   */
  DTD getDTD();
}
