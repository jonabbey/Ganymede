package com.jclark.xml.parse;

/**
 * Information about a markup declaration.
 * @see com.jclark.xml.parse.base.Application#markupDeclaration
 * @version $Revision: 1.1 $ $Date: 2000/01/26 04:31:59 $
 */
public interface MarkupDeclarationEvent {
  static int ATTRIBUTE = 0;
  static int ELEMENT = 1;
  static int GENERAL_ENTITY = 2;
  static int PARAMETER_ENTITY = 3;
  static int NOTATION = 4;
  int getType();
  String getName();
  String getAttributeName();
  DTD getDTD();
}
