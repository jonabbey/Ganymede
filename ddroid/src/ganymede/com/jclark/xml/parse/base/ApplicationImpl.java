package com.jclark.xml.parse.base;

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
 * @version $Revision: 1.1 $ $Date: 2000/01/26 04:32:07 $
 */
public class ApplicationImpl implements Application {
  public void startDocument() throws Exception { }
  public void endDocument() throws Exception { }
  public void startElement(StartElementEvent event) throws Exception { }
  public void characterData(CharacterDataEvent event) throws Exception { }
  public void endElement(EndElementEvent event) throws Exception { }
  public void processingInstruction(ProcessingInstructionEvent event) throws Exception { }
  public void endProlog(EndPrologEvent event) throws Exception { }
  public void comment(CommentEvent event) throws Exception { }
  public void startCdataSection(StartCdataSectionEvent event) throws Exception { }
  public void endCdataSection(EndCdataSectionEvent event) throws Exception { }
  public void startEntityReference(StartEntityReferenceEvent event) throws Exception { }
  public void endEntityReference(EndEntityReferenceEvent event) throws Exception { }
  public void startDocumentTypeDeclaration(StartDocumentTypeDeclarationEvent event) throws Exception { }
  public void endDocumentTypeDeclaration(EndDocumentTypeDeclarationEvent event) throws Exception { }
  public void markupDeclaration(MarkupDeclarationEvent event) throws Exception { }
}
