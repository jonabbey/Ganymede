package com.jclark.xml.parse.awt;

import java.awt.AWTException;

import com.jclark.xml.parse.CharacterDataEvent;
import com.jclark.xml.parse.CommentEvent;
import com.jclark.xml.parse.EndCdataSectionEvent;
import com.jclark.xml.parse.EndDocumentTypeDeclarationEvent;
import com.jclark.xml.parse.EndElementEvent;
import com.jclark.xml.parse.EndEntityReferenceEvent;
import com.jclark.xml.parse.EndPrologEvent;
import com.jclark.xml.parse.MarkupDeclarationEvent;
import com.jclark.xml.parse.ProcessingInstructionEvent;
import com.jclark.xml.parse.StartCdataSectionEvent;
import com.jclark.xml.parse.StartDocumentTypeDeclarationEvent;
import com.jclark.xml.parse.StartElementEvent;
import com.jclark.xml.parse.StartEntityReferenceEvent;

/**
 * A default implementation of <code>Application</code>.
 * All methods do nothing.
 * @version $Revision: 1.1 $ $Date: 2000/01/26 04:32:05 $
 */
public class ApplicationImpl implements Application {
  public void startDocument() throws AWTException { }
  public void endDocument() throws AWTException { }
  public void startElement(StartElementEvent event) throws AWTException { }
  public void characterData(CharacterDataEvent event) throws AWTException { }
  public void endElement(EndElementEvent event) throws AWTException { }
  public void processingInstruction(ProcessingInstructionEvent pi) throws AWTException { }
  public void endProlog(EndPrologEvent event) throws AWTException { }
  public void comment(CommentEvent event) throws AWTException { }
  public void startCdataSection(StartCdataSectionEvent event) throws AWTException { }
  public void endCdataSection(EndCdataSectionEvent event) throws AWTException { }
  public void startEntityReference(StartEntityReferenceEvent event) throws AWTException { }
  public void endEntityReference(EndEntityReferenceEvent event) throws AWTException { }
  public void startDocumentTypeDeclaration(StartDocumentTypeDeclarationEvent event) throws AWTException { }
  public void endDocumentTypeDeclaration(EndDocumentTypeDeclarationEvent event) throws AWTException { }
  public void markupDeclaration(MarkupDeclarationEvent event) throws AWTException { }
}
