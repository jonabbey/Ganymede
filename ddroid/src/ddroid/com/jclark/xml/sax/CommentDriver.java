package com.jclark.xml.sax;

import com.jclark.xml.parse.*;

/**
 * An special version of the SAX driver that reports comments
 * as processing instructions with a null target.
 */
  
public class CommentDriver extends Driver {
  public void comment(final CommentEvent event)
    throws org.xml.sax.SAXException {
    processingInstruction(new ProcessingInstructionEvent() {
      public ParseLocation getLocation() {
	return event.getLocation();
      }
      public String getName() {
	return null;
      }
      public String getInstruction() {
	return event.getComment();
      }
    });
  }
}
