package com.jclark.xml.parse;

import java.net.URL;
import java.io.IOException;

/**
 * This interface is used by the parser to access external entities.
 * @see Parser
 * @version $Revision: 1.1 $ $Date: 2000/01/26 04:31:57 $
 */
public interface EntityManager {
  /**
   * Opens an external entity.
   * @param systemId the system identifier specified in the entity declaration
   * @param baseURL the base URL relative to which the system identifier
   * should be resolved; null if no base URL is available
   * @param publicId the public identifier specified in the entity declaration;
   * null if no public identifier was specified
   */
  OpenEntity open(String systemId, URL baseURL, String publicId) throws IOException;
}
