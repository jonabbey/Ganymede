package com.jclark.xml.output;

import java.io.Writer;
import java.io.IOException;
import java.io.CharConversionException;
import java.io.OutputStream;

/**
 * An XMLWriter that encodes characters in UTF-8.
 * Methods are not synchronized: wrap this in a SyncXMLWriter
 * if you need to use this concurrently from multiple threads.
 * @version $Revision: 1.1 $ $Date: 2000/01/26 04:31:49 $
 */

public class UTF8XMLWriter extends XMLWriter {
  private OutputStream out;
  private boolean inStartTag = false;
  private boolean quoteWhitespace = false;
  private static final int DEFAULT_BUF_LENGTH = 4*1024;
  private byte[] buf = new byte[DEFAULT_BUF_LENGTH];
  private int bufUsed = 0;
  private String sysLineSeparator;
  private String lineSeparator;
  private boolean minimizeEmptyElements;
  private boolean minimizeEmptyElementsHtml;

  static final public int MINIMIZE_EMPTY_ELEMENTS = 1;
  static final public int MINIMIZE_EMPTY_ELEMENTS_HTML = 2;
  static final private int DEFAULT_OPTIONS = 0;
  /**
   * Create an XML writer that will write in UTF-8 to the specified
   * OutputStream with the specified options.
   */
  public UTF8XMLWriter(OutputStream out, int options) {
    super(out);
    this.out = out;
    if ((options & MINIMIZE_EMPTY_ELEMENTS_HTML) != 0)
      this.minimizeEmptyElements = this.minimizeEmptyElementsHtml = true;
    else if ((options & MINIMIZE_EMPTY_ELEMENTS) != 0)
      this.minimizeEmptyElements = true;
    sysLineSeparator = lineSeparator = System.getProperty("line.separator");
  }

  /**
   * Create an XML writer that will write in UTF-8 to the specified
   * OutputStream with the default options.
   */
  public UTF8XMLWriter(OutputStream out) {
    this(out, DEFAULT_OPTIONS);
  }

  public void writeUTF8(byte[] buf, int off, int len) throws IOException {
    if (inStartTag)
      finishStartTag();
    while (--len >= 0) {
      byte b = buf[off++];
      switch (b) {
      case (byte)'\n':
	writeRaw(lineSeparator);
	break;
      case (byte)'&':
	writeRaw("&amp;");
	break;
      case (byte)'<':
	writeRaw("&lt;");
	break;
      case (byte)'>':
	writeRaw("&gt;");
	break;
      case (byte)'"':
	writeRaw("&quot;");
	break;
      default:
	put(b);
	break;
      }
    }
  }

  public void write(char cbuf[], int off, int len) throws IOException {
    if (len == 0)
      return;
    if (inStartTag)
      finishStartTag();
    do {
      try {
	writeQuote(cbuf[off++]);
      }
      catch (CharConversionException e) {
	if (len-- == 0)
	  throw e;
	writeSurrogatePair(cbuf[off - 1], cbuf[off]);
	off++;
      }
    } while (--len > 0);
  }

  public void write(char c) throws IOException {
    if (inStartTag)
      finishStartTag();
    writeQuote(c);
  }

  public void write(String str) throws IOException {
    int len = str.length();
    if (len == 0)
      return;
    if (inStartTag)
      finishStartTag();
    writeQuote(str, 0, len);
  }

  public void write(String str, int off, int len) throws IOException {
    if (len == 0)
      return;
    if (inStartTag)
      finishStartTag();
    writeQuote(str, off, len);
  }
  
  private final void writeQuote(String str, int off, int len) throws IOException {
    for (; off < len; off++) {
      try {
	writeQuote(str.charAt(off));
      }
      catch (CharConversionException e) {
	if (++off == len)
	  throw e;
	writeSurrogatePair(str.charAt(off - 1), str.charAt(off));
      }
    }
  }

  private final void writeQuote(char c) throws IOException {
    switch (c) {
    case '\n':
      writeRaw(quoteWhitespace ? "&#10;" : lineSeparator);
      break;
    case '&':
      writeRaw("&amp;");
      break;
    case '<':
      writeRaw("&lt;");
      break;
    case  '>':
      writeRaw("&gt;");
      break;
    case '"':
      writeRaw("&quot;");
      break;
    case '\r':
      if (quoteWhitespace || !(out instanceof ReplacementTextOutputStream))
	writeRaw("&#13;");
      else
	put((byte)'\r');
      break;
    case '\t':
      if (quoteWhitespace)
	writeRaw("&#9;");
      else
	put((byte)'\t');
      break;
    default:
      if (c < 0x80)
	put((byte)c);
      else
	writeMB(c);
    }
  }

  private void writeRaw(String str) throws IOException {
    final int n = str.length();
    for (int i = 0; i < n; i++) {
      char c = str.charAt(i);
      if (c < 0x80)
	put((byte)c);
      else {
	try {
	  writeMB(str.charAt(i));
	}
	catch (CharConversionException e) {
	  if (++i == n)
	    throw e;
	  writeSurrogatePair(c, str.charAt(i));
	}
      }
    }
  }

  private final void writeMB(char c) throws IOException {
    switch (c & 0xF800) {
    case 0:
      put((byte)(((c >> 6) & 0x1F) | 0xC0));
      put((byte)((c & 0x3F) | 0x80));
      break;
    default:
      put((byte)(((c >> 12) & 0xF) | 0xE0));
      put((byte)(((c >> 6) & 0x3F) | 0x80));
      put((byte)((c & 0x3F) | 0x80));
      break;
    case 0xD800:
      throw new CharConversionException();
    }
  }
  
  private final void writeSurrogatePair(char c1, char c2) throws IOException {
    if ((c1 & 0xFC00) != 0xD800 || (c2 & 0xFC00) != 0xDC00)
      throw new CharConversionException();
    int c = ((c1 & 0x3FF) << 10) | (c2 & 0x3FF);
    c += 0x10000;
    put((byte)(((c >> 18) & 0x7) | 0xF0));
    put((byte)(((c >> 12) & 0x3F) | 0x80));
    put((byte)(((c >> 6) & 0x3F) | 0x80));
    put((byte)((c & 0x3F) | 0x80));
  }

  public void startElement(String name) throws IOException {
    if (inStartTag)
      finishStartTag();
    put((byte)'<');
    writeRaw(name);
    inStartTag = true;
  }

  private void finishStartTag() throws IOException {
    inStartTag = false;
    put((byte)'>');
  }

  public void attribute(String name, String value) throws IOException {
    if (!inStartTag)
      throw new IllegalStateException("attribute outside of start-tag");
    put((byte)' ');
    writeRaw(name);
    put((byte)'=');
    put((byte)'"');
    quoteWhitespace = true;
    writeQuote(value, 0, value.length());
    quoteWhitespace = false;
    put((byte)'"');
  }

  public void startAttribute(String name) throws IOException {
    if (!inStartTag)
      throw new IllegalStateException("attribute outside of start-tag");
    inStartTag = false;
    quoteWhitespace = true;
    put((byte)' ');
    writeRaw(name);
    put((byte)'=');
    put((byte)'"');
  }

  public void endAttribute() throws IOException {
    put((byte)'"');
    inStartTag = true;
    quoteWhitespace = false;
  }

  public void endElement(String name) throws IOException {
    if (inStartTag) {
      inStartTag = false;
      if (minimizeEmptyElements) {
	if (minimizeEmptyElementsHtml)
	  put((byte)' ');
	put((byte)'/');
	put((byte)'>');
	return;
      }
      put((byte)'>');
    }
    put((byte)'<');
    put((byte)'/');
    writeRaw(name);
    put((byte)'>');
  }

  public void processingInstruction(String target, String data) throws IOException {
    if (inStartTag)
      finishStartTag();
    put((byte)'<');
    put((byte)'?');
    writeRaw(target);
    if (data.length() > 0) {
      put((byte)' ');
      writeMarkup(data);
    }
    put((byte)'?');
    put((byte)'>');
  }

  public void comment(String body) throws IOException {
    if (inStartTag)
      finishStartTag();
    writeRaw("<!--");
    writeMarkup(body);
    writeRaw("-->");
  }

  public void entityReference(boolean isParam, String name) throws IOException {
    if (inStartTag)
      finishStartTag();
    put(isParam ? (byte)'%' : (byte)'&');
    writeRaw(name);
    put((byte)';');
  }

  public void characterReference(int n) throws IOException {
    if (inStartTag)
      finishStartTag();
    writeRaw("&#");
    writeRaw(Integer.toString(n));
    put((byte)';');
  }

  public void cdataSection(String content) throws IOException {
    if (inStartTag)
      finishStartTag();
    writeRaw("<![CDATA[");
    writeMarkup(content);
    writeRaw("]]>");
  }

  public void markup(String str) throws IOException {
    if (inStartTag)
      finishStartTag();
    writeMarkup(str);
  }

  private static class ReplacementTextOutputStream extends OutputStream {
    private OutputStream out;
    ReplacementTextOutputStream(OutputStream out) {
      this.out = out;
    }
    public void write(int b) throws IOException {
      switch (b) {
      case '"':
      case '\'':
      case '%':
      case '&':
      case '\r':
	out.write('&');
	out.write('#');
	{
	  String s = Integer.toString(b);
	  for (int i = 0; i < s.length(); i++)
	    out.write(s.charAt(i));
	}
	out.write(';');
	break;
      default:
	out.write(b);
	break;
      }
    }
    public void close() throws IOException {
      out.close();
    }
    public void flush() throws IOException {
      out.flush();
    }
    OutputStream getOutputStream() {
      return out;
    }
  }

  public void startReplacementText() throws IOException {
    flushBuf();
    out = new ReplacementTextOutputStream(out);
    lineSeparator = "\n";
  }

  public void endReplacementText() throws IOException {
    flushBuf();
    out = ((ReplacementTextOutputStream)out).getOutputStream();
    if (!(out instanceof ReplacementTextOutputStream))
      lineSeparator = sysLineSeparator;
  }

  private void writeMarkup(String str) throws IOException {
    int len = str.length();
    for (int i = 0; i < len; i++) {
      char c = str.charAt(i);
      if (c == '\n')
	writeRaw(lineSeparator);
      else {
	try {
	  if (c < 0x80)
	    put((byte)c);
	  else
	    writeMB(c);
	}
	catch (CharConversionException e) {
	  if (++i == len)
	    throw e;
	  writeSurrogatePair(c, str.charAt(i));
	}
      }
    }
  }

  private final void put(byte b) throws IOException {
    if (bufUsed == buf.length)
      flushBuf();
    buf[bufUsed++] = b;
  }

  private final void flushBuf() throws IOException {
    out.write(buf, 0, bufUsed);
    bufUsed = 0;
  }

  public void flush() throws IOException {
    if (bufUsed != 0)
      flushBuf();
    out.flush();
  }

  public void close() throws IOException {
    if (out != null) {
      if (bufUsed != 0)
	flushBuf();
      out.close();
      out = null;
      buf = null;
    }
  }
}
