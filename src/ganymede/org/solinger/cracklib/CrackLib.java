/*
   CrackLib.java

   This is a reformatted copy of Justin Chapweske's Artistic Licensed
   Java port of Alec Muffett's cracklib, circa version 0.5.

   All changes to this file relative to that located in the original
   archive from

   http://sourceforge.net/projects/solinger/

   are for the purpose of keeping with the code formatting rules we
   use in the Ganymede project, and to implement Ganymede-compatible
   localization support for the message strings.

*/

package org.solinger.cracklib;

import java.io.*;

public class CrackLib {
  public static final int MINDIFF = 5;
  public static final int MINLEN = 6;
  public static final int MAXSTEP = 4;

  public static final String[] destructors = new String[] {
    ":",                        // noop - must do this to test raw word.

    "[",                        // trimming leading/trailing junk
    "]",
    "[[",
    "]]",
    "[[[",
    "]]]",

    "/?p@?p",                   // purging out punctuation/symbols/junk
    "/?s@?s",
    "/?X@?X",

    // attempt reverse engineering of password strings

    "/$s$s",
    "/$s$s/0s0o",
    "/$s$s/0s0o/2s2a",
    "/$s$s/0s0o/2s2a/3s3e",
    "/$s$s/0s0o/2s2a/3s3e/5s5s",
    "/$s$s/0s0o/2s2a/3s3e/5s5s/1s1i",
    "/$s$s/0s0o/2s2a/3s3e/5s5s/1s1l",
    "/$s$s/0s0o/2s2a/3s3e/5s5s/1s1i/4s4a",
    "/$s$s/0s0o/2s2a/3s3e/5s5s/1s1i/4s4h",
    "/$s$s/0s0o/2s2a/3s3e/5s5s/1s1l/4s4a",
    "/$s$s/0s0o/2s2a/3s3e/5s5s/1s1l/4s4h",
    "/$s$s/0s0o/2s2a/3s3e/5s5s/4s4a",
    "/$s$s/0s0o/2s2a/3s3e/5s5s/4s4h",
    "/$s$s/0s0o/2s2a/3s3e/5s5s/4s4a",
    "/$s$s/0s0o/2s2a/3s3e/5s5s/4s4h",
    "/$s$s/0s0o/2s2a/3s3e/1s1i",
    "/$s$s/0s0o/2s2a/3s3e/1s1l",
    "/$s$s/0s0o/2s2a/3s3e/1s1i/4s4a",
    "/$s$s/0s0o/2s2a/3s3e/1s1i/4s4h",
    "/$s$s/0s0o/2s2a/3s3e/1s1l/4s4a",
    "/$s$s/0s0o/2s2a/3s3e/1s1l/4s4h",
    "/$s$s/0s0o/2s2a/3s3e/4s4a",
    "/$s$s/0s0o/2s2a/3s3e/4s4h",
    "/$s$s/0s0o/2s2a/3s3e/4s4a",
    "/$s$s/0s0o/2s2a/3s3e/4s4h",
    "/$s$s/0s0o/2s2a/5s5s",
    "/$s$s/0s0o/2s2a/5s5s/1s1i",
    "/$s$s/0s0o/2s2a/5s5s/1s1l",
    "/$s$s/0s0o/2s2a/5s5s/1s1i/4s4a",
    "/$s$s/0s0o/2s2a/5s5s/1s1i/4s4h",
    "/$s$s/0s0o/2s2a/5s5s/1s1l/4s4a",
    "/$s$s/0s0o/2s2a/5s5s/1s1l/4s4h",
    "/$s$s/0s0o/2s2a/5s5s/4s4a",
    "/$s$s/0s0o/2s2a/5s5s/4s4h",
    "/$s$s/0s0o/2s2a/5s5s/4s4a",
    "/$s$s/0s0o/2s2a/5s5s/4s4h",
    "/$s$s/0s0o/2s2a/1s1i",
    "/$s$s/0s0o/2s2a/1s1l",
    "/$s$s/0s0o/2s2a/1s1i/4s4a",
    "/$s$s/0s0o/2s2a/1s1i/4s4h",
    "/$s$s/0s0o/2s2a/1s1l/4s4a",
    "/$s$s/0s0o/2s2a/1s1l/4s4h",
    "/$s$s/0s0o/2s2a/4s4a",
    "/$s$s/0s0o/2s2a/4s4h",
    "/$s$s/0s0o/2s2a/4s4a",
    "/$s$s/0s0o/2s2a/4s4h",
    "/$s$s/0s0o/3s3e",
    "/$s$s/0s0o/3s3e/5s5s",
    "/$s$s/0s0o/3s3e/5s5s/1s1i",
    "/$s$s/0s0o/3s3e/5s5s/1s1l",
    "/$s$s/0s0o/3s3e/5s5s/1s1i/4s4a",
    "/$s$s/0s0o/3s3e/5s5s/1s1i/4s4h",
    "/$s$s/0s0o/3s3e/5s5s/1s1l/4s4a",
    "/$s$s/0s0o/3s3e/5s5s/1s1l/4s4h",
    "/$s$s/0s0o/3s3e/5s5s/4s4a",
    "/$s$s/0s0o/3s3e/5s5s/4s4h",
    "/$s$s/0s0o/3s3e/5s5s/4s4a",
    "/$s$s/0s0o/3s3e/5s5s/4s4h",
    "/$s$s/0s0o/3s3e/1s1i",
    "/$s$s/0s0o/3s3e/1s1l",
    "/$s$s/0s0o/3s3e/1s1i/4s4a",
    "/$s$s/0s0o/3s3e/1s1i/4s4h",
    "/$s$s/0s0o/3s3e/1s1l/4s4a",
    "/$s$s/0s0o/3s3e/1s1l/4s4h",
    "/$s$s/0s0o/3s3e/4s4a",
    "/$s$s/0s0o/3s3e/4s4h",
    "/$s$s/0s0o/3s3e/4s4a",
    "/$s$s/0s0o/3s3e/4s4h",
    "/$s$s/0s0o/5s5s",
    "/$s$s/0s0o/5s5s/1s1i",
    "/$s$s/0s0o/5s5s/1s1l",
    "/$s$s/0s0o/5s5s/1s1i/4s4a",
    "/$s$s/0s0o/5s5s/1s1i/4s4h",
    "/$s$s/0s0o/5s5s/1s1l/4s4a",
    "/$s$s/0s0o/5s5s/1s1l/4s4h",
    "/$s$s/0s0o/5s5s/4s4a",
    "/$s$s/0s0o/5s5s/4s4h",
    "/$s$s/0s0o/5s5s/4s4a",
    "/$s$s/0s0o/5s5s/4s4h",
    "/$s$s/0s0o/1s1i",
    "/$s$s/0s0o/1s1l",
    "/$s$s/0s0o/1s1i/4s4a",
    "/$s$s/0s0o/1s1i/4s4h",
    "/$s$s/0s0o/1s1l/4s4a",
    "/$s$s/0s0o/1s1l/4s4h",
    "/$s$s/0s0o/4s4a",
    "/$s$s/0s0o/4s4h",
    "/$s$s/0s0o/4s4a",
    "/$s$s/0s0o/4s4h",
    "/$s$s/2s2a",
    "/$s$s/2s2a/3s3e",
    "/$s$s/2s2a/3s3e/5s5s",
    "/$s$s/2s2a/3s3e/5s5s/1s1i",
    "/$s$s/2s2a/3s3e/5s5s/1s1l",
    "/$s$s/2s2a/3s3e/5s5s/1s1i/4s4a",
    "/$s$s/2s2a/3s3e/5s5s/1s1i/4s4h",
    "/$s$s/2s2a/3s3e/5s5s/1s1l/4s4a",
    "/$s$s/2s2a/3s3e/5s5s/1s1l/4s4h",
    "/$s$s/2s2a/3s3e/5s5s/4s4a",
    "/$s$s/2s2a/3s3e/5s5s/4s4h",
    "/$s$s/2s2a/3s3e/5s5s/4s4a",
    "/$s$s/2s2a/3s3e/5s5s/4s4h",
    "/$s$s/2s2a/3s3e/1s1i",
    "/$s$s/2s2a/3s3e/1s1l",
    "/$s$s/2s2a/3s3e/1s1i/4s4a",
    "/$s$s/2s2a/3s3e/1s1i/4s4h",
    "/$s$s/2s2a/3s3e/1s1l/4s4a",
    "/$s$s/2s2a/3s3e/1s1l/4s4h",
    "/$s$s/2s2a/3s3e/4s4a",
    "/$s$s/2s2a/3s3e/4s4h",
    "/$s$s/2s2a/3s3e/4s4a",
    "/$s$s/2s2a/3s3e/4s4h",
    "/$s$s/2s2a/5s5s",
    "/$s$s/2s2a/5s5s/1s1i",
    "/$s$s/2s2a/5s5s/1s1l",
    "/$s$s/2s2a/5s5s/1s1i/4s4a",
    "/$s$s/2s2a/5s5s/1s1i/4s4h",
    "/$s$s/2s2a/5s5s/1s1l/4s4a",
    "/$s$s/2s2a/5s5s/1s1l/4s4h",
    "/$s$s/2s2a/5s5s/4s4a",
    "/$s$s/2s2a/5s5s/4s4h",
    "/$s$s/2s2a/5s5s/4s4a",
    "/$s$s/2s2a/5s5s/4s4h",
    "/$s$s/2s2a/1s1i",
    "/$s$s/2s2a/1s1l",
    "/$s$s/2s2a/1s1i/4s4a",
    "/$s$s/2s2a/1s1i/4s4h",
    "/$s$s/2s2a/1s1l/4s4a",
    "/$s$s/2s2a/1s1l/4s4h",
    "/$s$s/2s2a/4s4a",
    "/$s$s/2s2a/4s4h",
    "/$s$s/2s2a/4s4a",
    "/$s$s/2s2a/4s4h",
    "/$s$s/3s3e",
    "/$s$s/3s3e/5s5s",
    "/$s$s/3s3e/5s5s/1s1i",
    "/$s$s/3s3e/5s5s/1s1l",
    "/$s$s/3s3e/5s5s/1s1i/4s4a",
    "/$s$s/3s3e/5s5s/1s1i/4s4h",
    "/$s$s/3s3e/5s5s/1s1l/4s4a",
    "/$s$s/3s3e/5s5s/1s1l/4s4h",
    "/$s$s/3s3e/5s5s/4s4a",
    "/$s$s/3s3e/5s5s/4s4h",
    "/$s$s/3s3e/5s5s/4s4a",
    "/$s$s/3s3e/5s5s/4s4h",
    "/$s$s/3s3e/1s1i",
    "/$s$s/3s3e/1s1l",
    "/$s$s/3s3e/1s1i/4s4a",
    "/$s$s/3s3e/1s1i/4s4h",
    "/$s$s/3s3e/1s1l/4s4a",
    "/$s$s/3s3e/1s1l/4s4h",
    "/$s$s/3s3e/4s4a",
    "/$s$s/3s3e/4s4h",
    "/$s$s/3s3e/4s4a",
    "/$s$s/3s3e/4s4h",
    "/$s$s/5s5s",
    "/$s$s/5s5s/1s1i",
    "/$s$s/5s5s/1s1l",
    "/$s$s/5s5s/1s1i/4s4a",
    "/$s$s/5s5s/1s1i/4s4h",
    "/$s$s/5s5s/1s1l/4s4a",
    "/$s$s/5s5s/1s1l/4s4h",
    "/$s$s/5s5s/4s4a",
    "/$s$s/5s5s/4s4h",
    "/$s$s/5s5s/4s4a",
    "/$s$s/5s5s/4s4h",
    "/$s$s/1s1i",
    "/$s$s/1s1l",
    "/$s$s/1s1i/4s4a",
    "/$s$s/1s1i/4s4h",
    "/$s$s/1s1l/4s4a",
    "/$s$s/1s1l/4s4h",
    "/$s$s/4s4a",
    "/$s$s/4s4h",
    "/$s$s/4s4a",
    "/$s$s/4s4h",
    "/0s0o",
    "/0s0o/2s2a",
    "/0s0o/2s2a/3s3e",
    "/0s0o/2s2a/3s3e/5s5s",
    "/0s0o/2s2a/3s3e/5s5s/1s1i",
    "/0s0o/2s2a/3s3e/5s5s/1s1l",
    "/0s0o/2s2a/3s3e/5s5s/1s1i/4s4a",
    "/0s0o/2s2a/3s3e/5s5s/1s1i/4s4h",
    "/0s0o/2s2a/3s3e/5s5s/1s1l/4s4a",
    "/0s0o/2s2a/3s3e/5s5s/1s1l/4s4h",
    "/0s0o/2s2a/3s3e/5s5s/4s4a",
    "/0s0o/2s2a/3s3e/5s5s/4s4h",
    "/0s0o/2s2a/3s3e/5s5s/4s4a",
    "/0s0o/2s2a/3s3e/5s5s/4s4h",
    "/0s0o/2s2a/3s3e/1s1i",
    "/0s0o/2s2a/3s3e/1s1l",
    "/0s0o/2s2a/3s3e/1s1i/4s4a",
    "/0s0o/2s2a/3s3e/1s1i/4s4h",
    "/0s0o/2s2a/3s3e/1s1l/4s4a",
    "/0s0o/2s2a/3s3e/1s1l/4s4h",
    "/0s0o/2s2a/3s3e/4s4a",
    "/0s0o/2s2a/3s3e/4s4h",
    "/0s0o/2s2a/3s3e/4s4a",
    "/0s0o/2s2a/3s3e/4s4h",
    "/0s0o/2s2a/5s5s",
    "/0s0o/2s2a/5s5s/1s1i",
    "/0s0o/2s2a/5s5s/1s1l",
    "/0s0o/2s2a/5s5s/1s1i/4s4a",
    "/0s0o/2s2a/5s5s/1s1i/4s4h",
    "/0s0o/2s2a/5s5s/1s1l/4s4a",
    "/0s0o/2s2a/5s5s/1s1l/4s4h",
    "/0s0o/2s2a/5s5s/4s4a",
    "/0s0o/2s2a/5s5s/4s4h",
    "/0s0o/2s2a/5s5s/4s4a",
    "/0s0o/2s2a/5s5s/4s4h",
    "/0s0o/2s2a/1s1i",
    "/0s0o/2s2a/1s1l",
    "/0s0o/2s2a/1s1i/4s4a",
    "/0s0o/2s2a/1s1i/4s4h",
    "/0s0o/2s2a/1s1l/4s4a",
    "/0s0o/2s2a/1s1l/4s4h",
    "/0s0o/2s2a/4s4a",
    "/0s0o/2s2a/4s4h",
    "/0s0o/2s2a/4s4a",
    "/0s0o/2s2a/4s4h",
    "/0s0o/3s3e",
    "/0s0o/3s3e/5s5s",
    "/0s0o/3s3e/5s5s/1s1i",
    "/0s0o/3s3e/5s5s/1s1l",
    "/0s0o/3s3e/5s5s/1s1i/4s4a",
    "/0s0o/3s3e/5s5s/1s1i/4s4h",
    "/0s0o/3s3e/5s5s/1s1l/4s4a",
    "/0s0o/3s3e/5s5s/1s1l/4s4h",
    "/0s0o/3s3e/5s5s/4s4a",
    "/0s0o/3s3e/5s5s/4s4h",
    "/0s0o/3s3e/5s5s/4s4a",
    "/0s0o/3s3e/5s5s/4s4h",
    "/0s0o/3s3e/1s1i",
    "/0s0o/3s3e/1s1l",
    "/0s0o/3s3e/1s1i/4s4a",
    "/0s0o/3s3e/1s1i/4s4h",
    "/0s0o/3s3e/1s1l/4s4a",
    "/0s0o/3s3e/1s1l/4s4h",
    "/0s0o/3s3e/4s4a",
    "/0s0o/3s3e/4s4h",
    "/0s0o/3s3e/4s4a",
    "/0s0o/3s3e/4s4h",
    "/0s0o/5s5s",
    "/0s0o/5s5s/1s1i",
    "/0s0o/5s5s/1s1l",
    "/0s0o/5s5s/1s1i/4s4a",
    "/0s0o/5s5s/1s1i/4s4h",
    "/0s0o/5s5s/1s1l/4s4a",
    "/0s0o/5s5s/1s1l/4s4h",
    "/0s0o/5s5s/4s4a",
    "/0s0o/5s5s/4s4h",
    "/0s0o/5s5s/4s4a",
    "/0s0o/5s5s/4s4h",
    "/0s0o/1s1i",
    "/0s0o/1s1l",
    "/0s0o/1s1i/4s4a",
    "/0s0o/1s1i/4s4h",
    "/0s0o/1s1l/4s4a",
    "/0s0o/1s1l/4s4h",
    "/0s0o/4s4a",
    "/0s0o/4s4h",
    "/0s0o/4s4a",
    "/0s0o/4s4h",
    "/2s2a",
    "/2s2a/3s3e",
    "/2s2a/3s3e/5s5s",
    "/2s2a/3s3e/5s5s/1s1i",
    "/2s2a/3s3e/5s5s/1s1l",
    "/2s2a/3s3e/5s5s/1s1i/4s4a",
    "/2s2a/3s3e/5s5s/1s1i/4s4h",
    "/2s2a/3s3e/5s5s/1s1l/4s4a",
    "/2s2a/3s3e/5s5s/1s1l/4s4h",
    "/2s2a/3s3e/5s5s/4s4a",
    "/2s2a/3s3e/5s5s/4s4h",
    "/2s2a/3s3e/5s5s/4s4a",
    "/2s2a/3s3e/5s5s/4s4h",
    "/2s2a/3s3e/1s1i",
    "/2s2a/3s3e/1s1l",
    "/2s2a/3s3e/1s1i/4s4a",
    "/2s2a/3s3e/1s1i/4s4h",
    "/2s2a/3s3e/1s1l/4s4a",
    "/2s2a/3s3e/1s1l/4s4h",
    "/2s2a/3s3e/4s4a",
    "/2s2a/3s3e/4s4h",
    "/2s2a/3s3e/4s4a",
    "/2s2a/3s3e/4s4h",
    "/2s2a/5s5s",
    "/2s2a/5s5s/1s1i",
    "/2s2a/5s5s/1s1l",
    "/2s2a/5s5s/1s1i/4s4a",
    "/2s2a/5s5s/1s1i/4s4h",
    "/2s2a/5s5s/1s1l/4s4a",
    "/2s2a/5s5s/1s1l/4s4h",
    "/2s2a/5s5s/4s4a",
    "/2s2a/5s5s/4s4h",
    "/2s2a/5s5s/4s4a",
    "/2s2a/5s5s/4s4h",
    "/2s2a/1s1i",
    "/2s2a/1s1l",
    "/2s2a/1s1i/4s4a",
    "/2s2a/1s1i/4s4h",
    "/2s2a/1s1l/4s4a",
    "/2s2a/1s1l/4s4h",
    "/2s2a/4s4a",
    "/2s2a/4s4h",
    "/2s2a/4s4a",
    "/2s2a/4s4h",
    "/3s3e",
    "/3s3e/5s5s",
    "/3s3e/5s5s/1s1i",
    "/3s3e/5s5s/1s1l",
    "/3s3e/5s5s/1s1i/4s4a",
    "/3s3e/5s5s/1s1i/4s4h",
    "/3s3e/5s5s/1s1l/4s4a",
    "/3s3e/5s5s/1s1l/4s4h",
    "/3s3e/5s5s/4s4a",
    "/3s3e/5s5s/4s4h",
    "/3s3e/5s5s/4s4a",
    "/3s3e/5s5s/4s4h",
    "/3s3e/1s1i",
    "/3s3e/1s1l",
    "/3s3e/1s1i/4s4a",
    "/3s3e/1s1i/4s4h",
    "/3s3e/1s1l/4s4a",
    "/3s3e/1s1l/4s4h",
    "/3s3e/4s4a",
    "/3s3e/4s4h",
    "/3s3e/4s4a",
    "/3s3e/4s4h",
    "/5s5s",
    "/5s5s/1s1i",
    "/5s5s/1s1l",
    "/5s5s/1s1i/4s4a",
    "/5s5s/1s1i/4s4h",
    "/5s5s/1s1l/4s4a",
    "/5s5s/1s1l/4s4h",
    "/5s5s/4s4a",
    "/5s5s/4s4h",
    "/5s5s/4s4a",
    "/5s5s/4s4h",
    "/1s1i",
    "/1s1l",
    "/1s1i/4s4a",
    "/1s1i/4s4h",
    "/1s1l/4s4a",
    "/1s1l/4s4h",
    "/4s4a",
    "/4s4h",
    "/4s4a",
    "/4s4h"};

  public static final String[] constructors = {
    ":",
    "r",
    "d",
    "f",
    "dr",
    "fr",
    "rf"};

  public static final boolean gTry(String rawtext, String password)
  {
    /* use destructors to turn password into rawtext */
    /* note use of Reverse() to save duplicating all rules */

    String mp;

    for (int i=0; i<destructors.length; i++)
      {
	if ((mp = Rules.mangle(password, destructors[i])) == null)
	  {
	    continue;
	  }

	if (mp.equals(rawtext))
	  {
	    return true;
	  }

	if (Rules.reverse(mp).equals(rawtext))
	  {
	    return true;
	  }
      }

    for (int i=0;i<constructors.length; i++)
      {
	if ((mp = Rules.mangle(rawtext, constructors[i])) == null)
	  {
	    continue;
	  }

	if (mp.equals(rawtext))
	  {
	    return true;
	  }
      }

    return false;
  }

  public static final String fascistLook(Packer p, String password) throws IOException
  {
    int notfound = p.size();

    if (password.length() < 4)
      {
	return ("it's WAY too short");
      }

    if (password.length() < MINLEN)
      {
	return ("it is too short");
      }

    String junk = new String(password.substring(0,1));

    for (int i=1; i<password.length();i++)
      {
	if (junk.indexOf(password.charAt(i)) == -1)
	  {
	    junk = junk+password.charAt(i);
	  }
      }

    if (junk.length() < MINDIFF)
      {
	return ("it does not contain enough DIFFERENT characters");
      }

    if ((password = password.trim()).length() == 0)
      {
	return ("it is all whitespace");
      }

    if (Rules.pMatch("aadddddda", password))
      {
	// smirk
	return ("it looks like a National Insurance number.");
      }

    for (int i=0;i<destructors.length; i++)
      {
	String mp;

	if ((mp = Rules.mangle(password, destructors[i])) == null)
	  {
	    continue;
	  }

	if (p.find(mp) != -1)
	  {
	    return ("it is based on the dictionary word :"+mp);
	  }
      }

    password = Rules.reverse(password);

    for (int i=0; i<destructors.length; i++)
      {
	String mp;

	if ((mp = Rules.mangle(password, destructors[i])) == null)
	  {
	    continue;
	  }

	if (p.find(mp) != -1)
	  {
	    return ("it is based on the dictionary word :"+mp);
	  }
      }

    return null;
  }

  public static final void usage()
  {
    System.err.println("Packer -dump <dict> | -make <dict> <wordlist>"
		       + " | -find <dict> <word>");
  }

  public static void main(String[] args) throws Exception
  {
    if (args.length == 3 && args[0].equals("-check"))
      {
	Packer p = new Packer(args[1],"r");

	try
	  {
	    String msg = fascistLook(p,args[2]);
	    if (msg != null)
	      {
		System.out.println(msg);
	      }
	    else
	      {
		System.out.println(args[2]+"looks good to me!");
	      }
	  }
	finally
	  {
	    p.close();
	  }
      }
    else
      {
	usage();
	System.exit(1);
      }
  }
}

