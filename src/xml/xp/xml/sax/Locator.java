package com.jclark.xml.sax;

import java.net.URL;

/**
 * An extension of org.xml.sax.Locator.
 * @version $Revision: 1.1 $ $Date: 2000/01/26 04:32:10 $
 */
interface Locator extends org.xml.sax.Locator {
  /**
   * Returns the URL for the current document event or null if
   * none is available.
   * This is the URL that should be used as the base URL for
   * resolving relative URLs in the document event.
   * This corresponds to the URL returned by <code>getEntityBase</code>
   * in com.jclark.xml.parse.ParseLocation.
   * The <code>getSystemId</code> method corresponds
   * to <code>getEntityLocation</code>.
   * @see com.jclark.xml.parse.ParseLocation#getEntityBase
   * @see com.jclark.xml.parse.ParseLocation#getEntityLocation
   */
  URL getURL();
  /**
   * Returns the byte index of the first byte of the first character
   * of the document event, or -1 if no byte index is available.
   * The index of the first byte is 0.
   */
  long getByteIndex();
}
