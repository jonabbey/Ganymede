package com.jclark.xml.sax;

import java.io.Reader;
import java.io.InputStream;
import java.io.IOException;

/**
 * An InputStream of the UTF-16 encoding of a Reader.
 *
 * @version $Revision: 1.1 $ $Date: 2000/01/26 04:32:11 $
 */
public class ReaderInputStream extends InputStream {
  private Reader reader;
  private static final int BUF_SIZE = 4096;
  private char[] buf = new char[BUF_SIZE];
  private int bufIndex = 0;
  private int bufEnd = 1;
  /* true if we have read the first nibble of the character at bufIndex
     but not yet read the second */
  private boolean nibbled = false;

  public ReaderInputStream(Reader reader) {
    this.reader = reader;
    buf[0] = '\ufeff';
  }

  public synchronized int read() throws IOException {
    if (nibbled) {
      nibbled = false;
      return buf[bufIndex++] & 0xff;
    }
    while (bufIndex == bufEnd) {
      bufIndex = 0;
      bufEnd = reader.read(buf, 0, buf.length);
      if (bufEnd < 0) {
	bufEnd = 0;
	return -1;
      }
    }
    nibbled = true;
    return buf[bufIndex] >> 8;
  }

  public synchronized int read(byte b[], int off, int len) throws IOException {
    if (len <= 0)
      return 0;
    int startOff = off;
    if (nibbled) {
      nibbled = false;
      if (b != null)
	b[off] = (byte)(buf[bufIndex] & 0xff);
      bufIndex++;
      off++;
      len--;
    }
    while (len > 0) {
      if (bufIndex == bufEnd) {
	bufIndex = 0;
	bufEnd = reader.read(buf, 0, buf.length);
	if (bufEnd < 0) {
	  bufEnd = 0;
	  if (off != startOff)
	    break;
	  return -1;
	}
	if (bufEnd == 0)
	  return off - startOff;
      }
      if (len == 1) {
	if (b != null)
	  b[off] = (byte)(buf[bufIndex] >> 8);
	off++;
	nibbled = true;
	break;
      }
      if (b != null) {
	b[off++] = (byte)(buf[bufIndex] >> 8);
	b[off++] = (byte)(buf[bufIndex] & 0xff);
      }
      else
	off += 2;
      len -= 2;
      bufIndex++;
    }
    return off - startOff;
  }

  public synchronized void close() throws IOException {
    reader.close();
  }
}
