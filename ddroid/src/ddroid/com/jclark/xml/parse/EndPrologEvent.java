package com.jclark.xml.parse;

/**
 * Information about the prolog.
 * @see com.jclark.xml.parse.base.Application#endProlog
 * @version $Revision: 1.1 $ $Date: 2000/01/26 04:31:56 $
 */
public interface EndPrologEvent {
  /**
   * Returns the DTD.
   * This will not be null even if there was no DOCTYPE declaration.
   */
  DTD getDTD();
}
