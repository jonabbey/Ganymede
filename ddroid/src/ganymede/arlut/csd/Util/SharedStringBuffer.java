/*
 * @(#)StringBuffer.java	1.35 98/04/22
 *
 * Copyright 1994-1997 by Sun Microsystems, Inc.,
 * 901 San Antonio Road, Palo Alto, California, 94303, U.S.A.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information
 * of Sun Microsystems, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Sun.
 */

package arlut.csd.Util;

/**
 * A hack on java.lang.StringBuffer to allow an instance of
 * StringBuffer to be effectively re-used without as much overhead.
 * Also provides a public getValue() method to allow
 * reusedStringBuffer's to be written out to disk without forcing a
 * duplication of the char array.
 *
 * @author	Arthur van Hoff, Jonathan Abbey (jonabbey@arlut.utexas.edu)
 * @see     java.io.ByteArrayOutputStream
 * @see     java.lang.String
 */
 
public final class SharedStringBuffer implements java.io.Serializable {

  /** The value is used for character storage. */
  private char value[];
  
  /** The count is the number of characters in the buffer. */
  private int count;
  
  /**
   * Constructs a string buffer with no characters in it and an 
   * initial capacity of 16 characters. 
   *
   */
  
  public SharedStringBuffer() 
  {
    this(16);
  }

  /**
   * Constructs a string buffer with no characters in it and an 
   * initial capacity specified by the <code>length</code> argument. 
   *
   * @param      length   the initial capacity.
   * @exception  NegativeArraySizeException  if the <code>length</code>
   *               argument is less than <code>0</code>.
   */
  
  public SharedStringBuffer(int length) 
  {
    value = new char[length];
  }

  /**
   * Constructs a string buffer so that it represents the same 
   * sequence of characters as the string argument. The initial 
   * capacity of the string buffer is <code>16</code> plus the length 
   * of the string argument. 
   *
   * @param   str   the initial contents of the buffer.
   */
  
  public SharedStringBuffer(String str) 
  {
    this(str.length() + 16);
    append(str);
  }

  /**
   * Returns the length (character count) of this string buffer.
   *
   * @return  the number of characters in this string buffer.
   */

  public int length()
  {
    return count;
  }

  /**
   * Returns the current capacity of the String buffer. The capacity
   * is the amount of storage available for newly inserted
   * characters; beyond which an allocation will occur.
   *
   * @return  the current capacity of this string buffer.
   */

  public int capacity() 
  {
    return value.length;
  }

  /**
   * Copies the buffer value.  This is normally only called when shared
   * is true.  It should only be called from a synchronized method.
   */

  private final void copy() 
  {
    char newValue[] = new char[value.length];
    System.arraycopy(value, 0, newValue, 0, count);
    value = newValue;
  }

  /**
   * Ensures that the capacity of the buffer is at least equal to the
   * specified minimum.
   * If the current capacity of this string buffer is less than the 
   * argument, then a new internal buffer is allocated with greater 
   * capacity. The new capacity is the larger of: 
   * <ul>
   * <li>The <code>minimumCapacity</code> argument. 
   * <li>Twice the old capacity, plus <code>2</code>. 
   * </ul>
   * If the <code>minimumCapacity</code> argument is nonpositive, this
   * method takes no action and simply returns.
   *
   * @param   minimumCapacity   the minimum desired capacity.
   */

  public synchronized void ensureCapacity(int minimumCapacity) 
  {
    if (minimumCapacity > value.length)
      {
	expandCapacity(minimumCapacity);
      }
  }

  /**
   * This implements the expansion semantics of ensureCapacity but is
   * unsynchronized for use internally by methods which are already
   * synchronized.
   *
   * @see arlut.csd.Util.SharedStringBuffer#ensureCapacity(int)
   */
  
  private void expandCapacity(int minimumCapacity) 
  {
    int newCapacity = (value.length + 1) * 2;

    if (minimumCapacity > newCapacity) 
      {
	newCapacity = minimumCapacity;
      }
	
    char newValue[] = new char[newCapacity];
    System.arraycopy(value, 0, newValue, 0, count);
    value = newValue;
  }

  /**
   * Sets the length of this String buffer.
   * If the <code>newLength</code> argument is less than the current 
   * length of the string buffer, the string buffer is truncated to 
   * contain exactly the number of characters given by the 
   * <code>newLength</code> argument. 
   * <p>
   * If the <code>newLength</code> argument is greater than or equal 
   * to the current length, sufficient null characters 
   * (<code>'&#92;u0000'</code>) are appended to the string buffer so that 
   * length becomes the <code>newLength</code> argument. 
   * <p>
   * The <code>newLength</code> argument must be greater than or equal 
   * to <code>0</code>. 
   *
   * @param      newLength   the new length of the buffer.
   * @exception  StringIndexOutOfBoundsException  if the
   *               <code>newLength</code> argument is invalid.
   * @see        arlut.csd.Util.SharedStringBuffer#length()
   */

  public synchronized void setLength(int newLength) 
  {
    if (newLength < 0) 
      {
	throw new StringIndexOutOfBoundsException(newLength);
      }
	
    if (newLength > value.length) 
      {
	expandCapacity(newLength);
      }

    if (count < newLength)
      {
	for (; count < newLength; count++) 
	  {
	    value[count] = '\0';
	  }
      } 
    else 
      {
	count = newLength;
      }
  }

  /**
   * Returns the character at a specific index in this string buffer. 
   * <p>
   * The first character of a string buffer is at index 
   * <code>0</code>, the next at index <code>1</code>, and so on, for 
   * array indexing. 
   * <p>
   * The index argument must be greater than or equal to 
   * <code>0</code>, and less than the length of this string buffer. 
   *
   * @param      index   the index of the desired character.
   * @return     the character at the specified index of this string buffer.
   * @exception  StringIndexOutOfBoundsException  if the index is invalid.
   * @see        arlut.csd.Util.SharedStringBuffer#length()
   */

  public synchronized char charAt(int index) 
  {
    if ((index < 0) || (index >= count)) 
      {
	throw new StringIndexOutOfBoundsException(index);
      }
    return value[index];
  }
  
  /**
   * Characters are copied from this string buffer into the 
   * destination character array <code>dst</code>. The first character to 
   * be copied is at index <code>srcBegin</code>; the last character to 
   * be copied is at index <code>srcEnd-1.</code> The total number of 
   * characters to be copied is <code>srcEnd-srcBegin</code>. The 
   * characters are copied into the subarray of <code>dst</code> starting 
   * at index <code>dstBegin</code> and ending at index:
   * <p><blockquote><pre>
   *     dstbegin + (srcEnd-srcBegin) - 1
   * </pre></blockquote>
   *
   * @param      srcBegin   start copying at this offset in the string buffer.
   * @param      srcEnd     stop copying at this offset in the string buffer.
   * @param      dst        the array to copy the data into.
   * @param      dstBegin   offset into <code>dst</code>.
   * @exception  StringIndexOutOfBoundsException  if there is an invalid
   *               index into the buffer.
   */

  public synchronized void getChars(int srcBegin, int srcEnd, char dst[], int dstBegin) 
  {
    if ((srcBegin < 0) || (srcBegin >= count)) 
      {
	throw new StringIndexOutOfBoundsException(srcBegin);
      }

    if ((srcEnd < 0) || (srcEnd > count)) 
      {
	throw new StringIndexOutOfBoundsException(srcEnd);
      }

    if (srcBegin < srcEnd) 
      {
	System.arraycopy(value, srcBegin, dst, dstBegin, srcEnd - srcBegin);
      }
  }

  /**
   * The character at the specified index of this string buffer is set 
   * to <code>ch</code>. 
   * <p>
   * The offset argument must be greater than or equal to 
   * <code>0</code>, and less than the length of this string buffer. 
   *
   * @param      index   the index of the character to modify.
   * @param      ch      the new character.
   * @exception  StringIndexOutOfBoundsException  if the index is invalid.
   * @see        arlut.csd.Util.SharedStringBuffer#length()
   */

  public synchronized void setCharAt(int index, char ch) 
  {
    if ((index < 0) || (index >= count)) 
      {
	throw new StringIndexOutOfBoundsException(index);
      }
    
    value[index] = ch;
  }

  /**
   * Appends the string representation of the <code>Object</code> 
   * argument to this string buffer. 
   * <p>
   * The argument is converted to a string as if by the method 
   * <code>String.valueOf</code>, and the characters of that 
   * string are then appended to this string buffer. 
   *
   * @param   obj   an <code>Object</code>.
   * @return  this string buffer.
   * @see     java.lang.String#valueOf(java.lang.Object)
   * @see     arlut.csd.Util.SharedStringBuffer#append(java.lang.String)
   */
  
  public synchronized SharedStringBuffer append(Object obj) 
  {
    return append(String.valueOf(obj));
  }

  /**
   * Appends the string to this string buffer. 
   * <p>
   * The characters of the <code>String</code> argument are appended, in 
   * order, to the contents of this string buffer, increasing the 
   * length of this string buffer by the length of the argument. 
   *
   * @param   str   a string.
   * @return  this string buffer.
   */

  public synchronized SharedStringBuffer append(String str) 
  {
    if (str == null) 
      {
	str = String.valueOf(str);
      }

    int len = str.length();
    int newcount = count + len;

    if (newcount > value.length)
      {
	expandCapacity(newcount);
      }

    str.getChars(0, len, value, count);
    count = newcount;
    return this;
  }

  /**
   * Appends the string representation of the <code>char</code> array 
   * argument to this string buffer. 
   * <p>
   * The characters of the array argument are appended, in order, to 
   * the contents of this string buffer. The length of this string 
   * buffer increases by the length of the argument. 
   *
   * @param   str   the characters to be appended.
   * @return  this string buffer.
   */

  public synchronized SharedStringBuffer append(char str[]) 
  {
    int len = str.length;
    int newcount = count + len;

    if (newcount > value.length)
      {
	expandCapacity(newcount);
      }
    
    System.arraycopy(str, 0, value, count, len);
    count = newcount;
    return this;
  }

  /**
   * Appends the string representation of a subarray of the 
   * <code>char</code> array argument to this string buffer. 
   * <p>
   * Characters of the character array <code>str</code>, starting at 
   * index <code>offset</code>, are appended, in order, to the contents 
   * of this string buffer. The length of this string buffer increases 
   * by the value of <code>len</code>. 
   *
   * @param   str      the characters to be appended.
   * @param   offset   the index of the first character to append.
   * @param   len      the number of characters to append.
   * @return  this string buffer.
   */

  public synchronized SharedStringBuffer append(char str[], int offset, int len) 
  {
    int newcount = count + len;

    if (newcount > value.length)
      {
	expandCapacity(newcount);
      }

    System.arraycopy(str, offset, value, count, len);
    count = newcount;
    return this;
  }

  /**
   * Appends the string representation of the <code>boolean</code> 
   * argument to the string buffer. 
   * <p>
   * The argument is converted to a string as if by the method 
   * <code>String.valueOf</code>, and the characters of that 
   * string are then appended to this string buffer. 
   *
   * @param   b   a <code>boolean</code>.
   * @return  this string buffer.
   * @see     java.lang.String#valueOf(boolean)
   * @see     arlut.csd.Util.SharedStringBuffer#append(java.lang.String)
   */

  public SharedStringBuffer append(boolean b) 
  {
    return append(String.valueOf(b));
  }

  /**
   * Appends the string representation of the <code>char</code> 
   * argument to this string buffer. 
   * <p>
   * The argument is appended to the contents of this string buffer. 
   * The length of this string buffer increases by <code>1</code>. 
   *
   * @param   ch   a <code>char</code>.
   * @return  this string buffer.
   */

  public synchronized SharedStringBuffer append(char c) 
  {
    int newcount = count + 1;

    if (newcount > value.length)
      {
	expandCapacity(newcount);
      }

    value[count++] = c;
    return this;
  }

  /**
   * Appends the string representation of the <code>int</code> 
   * argument to this string buffer. 
   * <p>
   * The argument is converted to a string as if by the method 
   * <code>String.valueOf</code>, and the characters of that 
   * string are then appended to this string buffer. 
   *
   * @param   i   an <code>int</code>.
   * @return  this string buffer.
   * @see     java.lang.String#valueOf(int)
   * @see     arlut.csd.Util.SharedStringBuffer#append(java.lang.String)
   */

  public SharedStringBuffer append(int i) 
  {
    return append(String.valueOf(i));
  }
  
  /**
   * Appends the string representation of the <code>long</code> 
   * argument to this string buffer. 
   * <p>
   * The argument is converted to a string as if by the method 
   * <code>String.valueOf</code>, and the characters of that 
   * string are then appended to this string buffer. 
   *
   * @param   l   a <code>long</code>.
   * @return  this string buffer.
   * @see     java.lang.String#valueOf(long)
   * @see     arlut.csd.Util.SharedStringBuffer#append(java.lang.String)
   */

  public SharedStringBuffer append(long l) 
  {
    return append(String.valueOf(l));
  }

  /**
   * Appends the string representation of the <code>float</code> 
   * argument to this string buffer. 
   * <p>
   * The argument is converted to a string as if by the method 
   * <code>String.valueOf</code>, and the characters of that 
   * string are then appended to this string buffer. 
   *
   * @param   f   a <code>float</code>.
   * @return  this string buffer.
   * @see     java.lang.String#valueOf(float)
   * @see     arlut.csd.Util.SharedStringBuffer#append(java.lang.String)
   */

  public SharedStringBuffer append(float f) 
  {
    return append(String.valueOf(f));
  }

  /**
   * Appends the string representation of the <code>double</code> 
   * argument to this string buffer. 
   * <p>
   * The argument is converted to a string as if by the method 
   * <code>String.valueOf</code>, and the characters of that 
   * string are then appended to this string buffer. 
   *
   * @param   d   a <code>double</code>.
   * @return  this string buffer.
   * @see     java.lang.String#valueOf(double)
   * @see     arlut.csd.Util.SharedStringBuffer#append(java.lang.String)
   */

  public SharedStringBuffer append(double d) 
  {
    return append(String.valueOf(d));
  }

  /**
   * Inserts the string representation of the <code>Object</code> 
   * argument into this string buffer. 
   * <p>
   * The second argument is converted to a string as if by the method 
   * <code>String.valueOf</code>, and the characters of that 
   * string are then inserted into this string buffer at the indicated 
   * offset. 
   * <p>
   * The offset argument must be greater than or equal to 
   * <code>0</code>, and less than or equal to the length of this 
   * string buffer. 
   *
   * @param      offset   the offset.
   * @param      b        an <code>Object</code>.
   * @return     this string buffer.
   * @exception  StringIndexOutOfBoundsException  if the offset is invalid.
   * @see        java.lang.String#valueOf(java.lang.Object)
   * @see        arlut.csd.Util.SharedStringBuffer#insert(int, java.lang.String)
   * @see        arlut.csd.Util.SharedStringBuffer#length()
   */

  public synchronized SharedStringBuffer insert(int offset, Object obj) 
  {
    return insert(offset, String.valueOf(obj));
  }

  /**
   * Inserts the string into this string buffer. 
   * <p>
   * The characters of the <code>String</code> argument are inserted, in 
   * order, into this string buffer at the indicated offset. The length 
   * of this string buffer is increased by the length of the argument. 
   * <p>
   * The offset argument must be greater than or equal to 
   * <code>0</code>, and less than or equal to the length of this 
   * string buffer. 
   *
   * @param      offset   the offset.
   * @param      str      a string.
   * @return     this string buffer.
   * @exception  StringIndexOutOfBoundsException  if the offset is invalid.
   * @see        arlut.csd.Util.SharedStringBuffer#length()
   */

  public synchronized SharedStringBuffer insert(int offset, String str) 
  {
    if ((offset < 0) || (offset > count)) 
      {
	throw new StringIndexOutOfBoundsException();
      }

    int len = str.length();
    int newcount = count + len;

    if (newcount > value.length)
      {
	expandCapacity(newcount);
      }
    
    System.arraycopy(value, offset, value, offset + len, count - offset);
    str.getChars(0, len, value, offset);
    count = newcount;
    return this;
  }

  /**
   * Inserts the string representation of the <code>char</code> array 
   * argument into this string buffer. 
   * <p>
   * The characters of the array argument are inserted into the 
   * contents of this string buffer at the position indicated by 
   * <code>offset</code>. The length of this string buffer increases by 
   * the length of the argument. 
   *
   * @param      offset   the offset.
   * @param      ch       a character array.
   * @return     this string buffer.
   * @exception  StringIndexOutOfBoundsException  if the offset is invalid.
   */

  public synchronized SharedStringBuffer insert(int offset, char str[]) 
  {
    if ((offset < 0) || (offset > count)) 
      {
	throw new StringIndexOutOfBoundsException();
      }

    int len = str.length;
    int newcount = count + len;

    if (newcount > value.length)
      {
	expandCapacity(newcount);
      }

    System.arraycopy(value, offset, value, offset + len, count - offset);
    System.arraycopy(str, 0, value, offset, len);
    count = newcount;
    return this;
  }

  /**
   * Inserts the string representation of the <code>boolean</code> 
   * argument into this string buffer. 
   * <p>
   * The second argument is converted to a string as if by the method 
   * <code>String.valueOf</code>, and the characters of that 
   * string are then inserted into this string buffer at the indicated 
   * offset. 
   * <p>
   * The offset argument must be greater than or equal to 
   * <code>0</code>, and less than or equal to the length of this 
   * string buffer. 
   *
   * @param      offset   the offset.
   * @param      b        a <code>boolean</code>.
   * @return     this string buffer.
   * @exception  StringIndexOutOfBoundsException  if the offset is invalid.
   * @see        java.lang.String#valueOf(boolean)
   * @see        arlut.csd.Util.SharedStringBuffer#insert(int, java.lang.String)
   * @see        arlut.csd.Util.SharedStringBuffer#length()
   */

  public SharedStringBuffer insert(int offset, boolean b) 
  {
    return insert(offset, String.valueOf(b));
  }

  /**
   * Inserts the string representation of the <code>char</code> 
   * argument into this string buffer. 
   * <p>
   * The second argument is inserted into the contents of this string 
   * buffer at the position indicated by <code>offset</code>. The length 
   * of this string buffer increases by one. 
   * <p>
   * The offset argument must be greater than or equal to 
   * <code>0</code>, and less than or equal to the length of this 
   * string buffer. 
   *
   * @param      offset   the offset.
   * @param      ch       a <code>char</code>.
   * @return     this string buffer.
   * @exception  StringIndexOutOfBoundsException  if the offset is invalid.
   * @see        arlut.csd.Util.SharedStringBuffer#length()
   */

  public synchronized SharedStringBuffer insert(int offset, char c) 
  {
    int newcount = count + 1;

    if (newcount > value.length)
      {
	expandCapacity(newcount);
      }

    System.arraycopy(value, offset, value, offset + 1, count - offset);
    value[offset] = c;
    count = newcount;
    return this;
  }

  /**
   * Inserts the string representation of the second <code>int</code> 
   * argument into this string buffer. 
   * <p>
   * The second argument is converted to a string as if by the method 
   * <code>String.valueOf</code>, and the characters of that 
   * string are then inserted into this string buffer at the indicated 
   * offset. 
   * <p>
   * The offset argument must be greater than or equal to 
   * <code>0</code>, and less than or equal to the length of this 
   * string buffer. 
   *
   * @param      offset   the offset.
   * @param      b        an <code>int</code>.
   * @return     this string buffer.
   * @exception  StringIndexOutOfBoundsException  if the offset is invalid.
   * @see        java.lang.String#valueOf(int)
   * @see        arlut.csd.Util.SharedStringBuffer#insert(int, java.lang.String)
   * @see        arlut.csd.Util.SharedStringBuffer#length()
   */

  public SharedStringBuffer insert(int offset, int i) 
  {
    return insert(offset, String.valueOf(i));
  }

  /**
   * Inserts the string representation of the <code>long</code> 
   * argument into this string buffer. 
   * <p>
   * The second argument is converted to a string as if by the method 
   * <code>String.valueOf</code>, and the characters of that 
   * string are then inserted into this string buffer at the indicated 
   * offset. 
   * <p>
   * The offset argument must be greater than or equal to 
   * <code>0</code>, and less than or equal to the length of this 
   * string buffer. 
   *
   * @param      offset   the offset.
   * @param      b        a <code>long</code>.
   * @return     this string buffer.
   * @exception  StringIndexOutOfBoundsException  if the offset is invalid.
   * @see        java.lang.String#valueOf(long)
   * @see        arlut.csd.Util.SharedStringBuffer#insert(int, java.lang.String)
   * @see        arlut.csd.Util.SharedStringBuffer#length()
   */

  public SharedStringBuffer insert(int offset, long l) 
  {
    return insert(offset, String.valueOf(l));
  }

  /**
   * Inserts the string representation of the <code>float</code> 
   * argument into this string buffer. 
   * <p>
   * The second argument is converted to a string as if by the method 
   * <code>String.valueOf</code>, and the characters of that 
   * string are then inserted into this string buffer at the indicated 
   * offset. 
   * <p>
   * The offset argument must be greater than or equal to 
   * <code>0</code>, and less than or equal to the length of this 
   * string buffer. 
   *
   * @param      offset   the offset.
   * @param      b        a <code>float</code>.
   * @return     this string buffer.
   * @exception  StringIndexOutOfBoundsException  if the offset is invalid.
   * @see        java.lang.String#valueOf(float)
   * @see        arlut.csd.Util.SharedStringBuffer#insert(int, java.lang.String)
   * @see        arlut.csd.Util.SharedStringBuffer#length()
   */

  public SharedStringBuffer insert(int offset, float f) 
  {
    return insert(offset, String.valueOf(f));
  }

  /**
   * Inserts the string representation of the <code>double</code> 
   * argument into this string buffer. 
   * <p>
   * The second argument is converted to a string as if by the method 
   * <code>String.valueOf</code>, and the characters of that 
   * string are then inserted into this string buffer at the indicated 
   * offset. 
   * <p>
   * The offset argument must be greater than or equal to 
   * <code>0</code>, and less than or equal to the length of this 
   * string buffer. 
   *
   * @param      offset   the offset.
   * @param      b        a <code>double</code>.
   * @return     this string buffer.
   * @exception  StringIndexOutOfBoundsException  if the offset is invalid.
   * @see        java.lang.String#valueOf(double)
   * @see        arlut.csd.Util.SharedStringBuffer#insert(int, java.lang.String)
   * @see        arlut.csd.Util.SharedStringBuffer#length()
   */

  public SharedStringBuffer insert(int offset, double d) 
  {
    return insert(offset, String.valueOf(d));
  }

  /**
   * The character sequence contained in this string buffer is 
   * replaced by the reverse of the sequence. 
   *
   * @return  this string buffer.
   */

  public synchronized SharedStringBuffer reverse() 
  {
    int n = count - 1;

    for (int j = (n-1) >> 1; j >= 0; --j) 
      {
	char temp = value[j];
	value[j] = value[n - j];
	value[n - j] = temp;
      }

    return this;
  }

  /**
   * Converts to a string representing the data in this string buffer.
   * A new <code>String</code> object is allocated and initialized to 
   * contain the character sequence currently represented by this 
   * string buffer. This <code>String</code> is then returned. Subsequent 
   * changes to the string buffer do not affect the contents of the 
   * <code>String</code>. 
   *
   * @return  a string representation of the string buffer.
   */

  public String toString()
  {
    return new String(value, 0, count);
  }

  /**
   *
   * Return direct access to the char[] array in this object.  It is
   * the responsibility of the calling code to make sure that it is
   * executing in a context in which no other code will touch this
   * array or SharedStringBuffer while the the reference to this
   * array is in use.
   *
   */

  public final char[] getValue()
  {
    return value;
  }
}
