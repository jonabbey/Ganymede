package com.jclark.xml.tok;

/**
 * Thrown to indicate that the subarray being tokenized is not the
 * complete encoding of one or more characters, but might be if
 * more bytes were added.
 * @version $Revision: 1.1 $ $Date: 2000/01/26 04:32:16 $
 */
public class PartialCharException extends PartialTokenException {
  private int leadByteIndex;
  PartialCharException(int leadByteIndex) {
    this.leadByteIndex = leadByteIndex;
  }
  /**
   * Returns the index of the first byte that is not part of the complete
   * encoding of a character.
   */
  public int getLeadByteIndex() {
    return leadByteIndex;
  }
}
