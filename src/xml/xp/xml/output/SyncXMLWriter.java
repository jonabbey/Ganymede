package com.jclark.xml.output;

import java.io.Writer;
import java.io.IOException;

public class SyncXMLWriter extends XMLWriter {
  private XMLWriter w;

  public SyncXMLWriter(XMLWriter w) {
    super(w);
    this.w = w;
  }

  public void write(char cbuf[], int off, int len) throws IOException {
    synchronized (lock) {
      w.write(cbuf, off, len);
    }
  }

  public void write(String str) throws IOException {
    synchronized (lock) {
      w.write(str);
    }
  }

  public void write(int c) throws IOException {
    synchronized (lock) {
      w.write(c);
    }
  }

  public void write(String str, int off, int len) throws IOException {
    synchronized (lock) {
      w.write(str, off, len);
    }
  }


  public void close() throws IOException {
    synchronized (lock) {
      w.close();
    }
  }

  public void flush() throws IOException {
    synchronized (lock) {
      w.flush();
    }
  }

  public void startElement(String name) throws IOException {
    synchronized (lock) {
      w.startElement(name);
    }
  }

  public void attribute(String name, String value) throws IOException {
    synchronized (lock) {
      w.attribute(name, value);
    }
  }

  public void endElement(String name) throws IOException {
    synchronized (lock) {
      w.endElement(name);
    }
  }

  public void processingInstruction(String target, String data) throws IOException {
    synchronized (lock) {
      w.processingInstruction(target, data);
    }
  }

  public void comment(String str) throws IOException {
    synchronized (lock) {
      w.comment(str);
    }
  }

  public void entityReference(boolean isParam, String name) throws IOException {
    synchronized (lock) {
      w.entityReference(isParam, name);
    }
  }

  public void characterReference(int n) throws IOException {
    synchronized (lock) {
      w.characterReference(n);
    }
  }

  public void cdataSection(String content) throws IOException {
    synchronized (lock) {
      w.cdataSection(content);
    }
  }

  public void markup(String str) throws IOException {
    synchronized (lock) {
      w.markup(str);
    }
  }

  public void startReplacementText() throws IOException {
    synchronized (lock) {
      w.startReplacementText();
    }
  }

  public void endReplacementText() throws IOException {
    synchronized (lock) {
      w.endReplacementText();
    }
  }

  public void startAttribute(String name) throws IOException {
    synchronized (lock) {
      w.startAttribute(name);
    }
  }

  public void endAttribute() throws IOException {
    synchronized (lock) {
      w.endAttribute();
    }
  }
  
}
