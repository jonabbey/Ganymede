package com.jclark.xml.parse;

/**
 * Information about the end of an element.
 * @see com.jclark.xml.parse.base.Application#endElement
 * @version $Revision: 1.1 $ $Date: 2000/01/26 04:31:55 $
 */
public interface EndElementEvent {
  /**
   * Returns the element type name.
   */
  String getName();
  
}
