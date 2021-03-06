/*

   StringUtils.java

   Created: 24 March 2000

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2013
   The University of Texas at Austin

   Ganymede is a registered trademark of The University of Texas at Austin

   Contact information

   Web site: http://www.arlut.utexas.edu/gash2
   Author Email: ganymede_author@arlut.utexas.edu
   Email mailing list: ganymede@arlut.utexas.edu

   US Mail:

   Computer Science Division
   Applied Research Laboratories
   The University of Texas at Austin
   PO Box 8029, Austin TX 78713-8029

   Telephone: (512) 835-3200

   This program is free software; you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation; either version 2 of the License, or
   (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/

package arlut.csd.Util;

import java.util.regex.Pattern;

/*------------------------------------------------------------------------------
                                                                           class
                                                                     StringUtils

------------------------------------------------------------------------------*/

/**
 * <p>This class contains a variety of utility String manipulating static
 * methods for use in Ganymede.</p>
 */

public class StringUtils {

  private static Pattern lineSplit = Pattern.compile("\n");

  /**
   * Takes a String, and returns a new String that has prefix
   * prepended to each line in the original String.
   */

  public static String insertPrefixPerLine(String input, String prefix)
  {
    if (input == null)
      {
        input = "";
      }

    String[] lines = lineSplit.split(input, -1);
    StringBuilder builder = new StringBuilder();

    int upperLimit;

    if (input.endsWith("\n"))
      {
        upperLimit = lines.length - 1;
      }
    else
      {
        upperLimit = lines.length;
      }

    for (int i = 0; i < upperLimit; i++)
      {
        builder.append(prefix);
        builder.append(lines[i]);
        builder.append("\n");
      }

    return builder.toString();
  }

  /**
   * Simple method to exchange null strings for empty strings.
   */

  public static String emptyNull(String inputString)
  {
    if (inputString == null)
      {
        return "";
      }

    return inputString;
  }

  /**
   * Returns true if inputString is null or equal to the empty string
   * or contains nothing but whitespace.
   */

  public static boolean isEmpty(String inputString)
  {
    return inputString == null || inputString.trim().length() == 0;
  }

  /**
   * <p>This method strips out any characters from inputString that are
   * not present in legalChars.</p>
   *
   * <p>This method will always return a non-null String.</p>
   */

  public static String strip(String inputString, String legalChars)
  {
    if (inputString == null || legalChars == null)
      {
        return "";
      }

    StringBuilder buffer = new StringBuilder();

    for (int i = 0; i < inputString.length(); i++)
      {
        char c = inputString.charAt(i);

        if (legalChars.indexOf(c) != -1)
          {
            buffer.append(c);
          }
      }

    return buffer.toString();
  }

  /**
   * <p>This method tests to see if inputString consists of only characters
   * contained within the legalChars string.  If inputString contains
   * no characters not contained within legalChars, containsOnly() will
   * return true, otherwise it will return false.</p>
   *
   * <p>Note that containsOnly will always return true if inputString
   * is null or empty.</p>
   */

  public static boolean containsOnly(String inputString, String legalChars)
  {
    if (inputString == null || inputString.length() == 0)
      {
        return true;
      }

    if (legalChars == null || legalChars.length() == 0)
      {
        return false;
      }

    for (int i = 0; i < inputString.length(); i++)
      {
        char c = inputString.charAt(i);

        if (legalChars.indexOf(c) == -1)
          {
            return false;
          }
      }

    return true;
  }

  /**
   * <p>Returns true if inputString contains each and every character
   * in searchChars.</p>
   *
   * <p>Note that containsAll will always return true if searchChars
   * is null or empty.</p>
   */

  public static boolean containsAll(String inputString, String searchChars)
  {
    if (searchChars == null || searchChars.length() == 0)
      {
        return true;
      }

    if (inputString == null || inputString.length() == 0)
      {
        return false;
      }

    for (int i = 0; i < searchChars.length(); i++)
      {
        char c = searchChars.charAt(i);

        if (inputString.indexOf(c) == -1)
          {
            return false;
          }
      }

    return true;
  }

  /**
   * <p>This method takes an inputString and counts the number of times
   * that patternString occurs within it.</p>
   */

  public static int count(String inputString, String patternString)
  {
    int index = 0;
    int count = 0;

    /* -- */

    while (true)
      {
        index = inputString.indexOf(patternString, index);

        if (index == -1)
          {
            break;
          }
        else
          {
            index += patternString.length();
            count++;
          }
      }

    return count;
  }

  /**
   * <p>This method takes an input string and inserts back-slash
   * escapes to protect single quote, double quote, newlines, and
   * back-slash characters.</p>
   *
   * <p>This breaks horribly if the input string is already escaped,
   * of course.</p>
   */

  public static String escape(String inputString)
  {
    StringBuilder result = new StringBuilder();
    char[] inAry = inputString.toCharArray();

    /* -- */

    for (int i = 0; i < inAry.length; i++)
      {
        char c = inAry[i];

        switch (c)
          {
          case '\\':
            result.append("\\\\");
            break;

          case '\n':
            result.append("\\n");
            break;

          case '\'':
            result.append("\\'");
            break;

          case '"':
            result.append("\\\"");
            break;

          default:
            result.append(c);
          }
      }

    return result.toString();
  }

  /**
   * <p>This method takes an input string and handles back-slash
   * escaping of single quotes, double quotes, newline sequence (\n),
   * and \ itself.</p>
   */

  public static String de_escape(String inputString)
  {
    boolean escaping = false;
    char[] inAry = inputString.toCharArray();
    char[] resultAry = new char[inputString.length()];
    int index = 0;

    /* -- */

    for (int i = 0; i < inAry.length; i++)
      {
        char c = inAry[i];

        if (!escaping)
          {
            if (c == '\\')
              {
                escaping = true;
                continue;
              }

            resultAry[index++] = c;
          }
        else
          {
            switch (c)
              {
              case 'n':
                resultAry[index++] = '\n';
                break;

              case '\\':
                resultAry[index++] = '\\';
                break;

              case '\'':
                resultAry[index++] = '\'';
                break;

              case '\"':
                resultAry[index++] = '\"';
                break;

              default:
                resultAry[index++] = c;
              }
          }
      }

    return new String(resultAry, 0, index);
  }

  /**
   * <p>This method takes a string and, if the string starts and
   * ends with either " or ', returns the contained string.</p>
   *
   * <p>Returns the original input string if the inputString's first
   * and last characters were not matching single or double quote
   * characters.</p>
   */

  public static String dequote(String inputString)
  {
    int last = inputString.length()-1;

    if ((inputString.charAt(0) == '"' && inputString.charAt(last) == '"') ||
        (inputString.charAt(0) == '\'' && inputString.charAt(last) == '\''))
      {
        return inputString.substring(1, last);
      }

    return inputString;
  }

  /**
   * <p>This method takes a (possibly multiline) inputString
   * containing subsequences matching splitString and returns
   * an array of Strings which contain the contents of the inputString
   * between instances of the splitString.  The splitString divider
   * will not be returned in the split strings.</p>
   *
   * <p>In particular, this can be used to split a multiline String
   * into an array of Strings by using a splitString of "\n".  The
   * resulting strings will not include their terminating newlines.</p>
   */

  public static String[] split(String inputString, String splitString)
  {
    Pattern splitPat = Pattern.compile(Pattern.quote(splitString));

    return splitPat.split(inputString, -1);
  }

  /**
   * <p>This method behaves like String.replace(), but replaces
   * substrings rather than chars.</p>
   */

  public static String replaceStr(String inputString, String splitString, String joinString)
  {
    StringBuilder buffer = new StringBuilder();
    String[] elems = split(inputString, splitString);

    for (int i = 0; i < elems.length; i++)
      {
        if (i != 0)
          {
            buffer.append(joinString);
          }

        if (elems[i] != null)
          {
            buffer.append(elems[i]);
          }
      }

    return buffer.toString();
  }

  /**
   * <p>This static method can be used to compare two string
   * variables, whether they are null or not.</p>
   */

  public static boolean stringEquals(String stringA, String stringB)
  {
    if (stringA == null)
      {
        return stringB == null;
      }

    if (stringB == null)
      {
        return false;
      }

    return stringA.equals(stringB);
  }

  /**
   * <p>Returns inputString with whatever characters from
   * desiredEnding are required to ensure that the resulting String
   * ends with desiredEnding.</p>
   *
   * <p>I.e., if inputString is "Hi!" (or "Hi!\n") and desiredEnding
   * is "\n\n", then ensureEndsWith() will return "Hi!\n\n".  If
   * inputString is "Hi!\n\n", "Hi!\n\n\n", etc., then inputString
   * will be returned as is, because it already ends with
   * desiredEnding.</p>
   */

  public static String ensureEndsWith(String inputString, String desiredEnding)
  {
    if (inputString.endsWith(desiredEnding))
      {
        return inputString;
      }

    for (int i = 1; i < desiredEnding.length(); i++)
      {
        String suffix = desiredEnding.substring(i);

        if (inputString.endsWith(suffix))
          {
            return inputString + desiredEnding.substring(desiredEnding.length() - i);
          }
      }

    return inputString + desiredEnding;
  }

  /**
   * <p>Appends the minimum number of characters to 'in' necessary to
   * ensure that 'in' ends with the contents of desiredEnding.</p>
   */

  public static void ensureEndsWith(StringBuffer in, String desiredEnding)
  {
    if (endsWith(in, desiredEnding))
      {
        return;
      }

    for (int i = 1; i < desiredEnding.length(); i++)
      {
        String suffix = desiredEnding.substring(i);

        if (endsWith(in, suffix))
          {
            in.append(desiredEnding.substring(desiredEnding.length() - i));
            return;
          }
      }

    in.append(desiredEnding);
  }

  /**
   * <p>Returns true if the StringBuffer parameter ends with the
   * ending String.</p>
   */

  public static boolean endsWith(StringBuffer in, String ending)
  {
    return (in.length() >= ending.length() &&
            (in.substring(in.length() - ending.length()).equals(ending)));
  }

  /**
   * <p>Appends the minimum number of characters to 'in' necessary to
   * ensure that 'in' ends with the contents of desiredEnding.</p>
   */

  public static void ensureEndsWith(StringBuilder in, String desiredEnding)
  {
    if (endsWith(in, desiredEnding))
      {
        return;
      }

    for (int i = 1; i < desiredEnding.length(); i++)
      {
        String suffix = desiredEnding.substring(i);

        if (endsWith(in, suffix))
          {
            in.append(desiredEnding.substring(desiredEnding.length() - i));
            return;
          }
      }

    in.append(desiredEnding);
  }

  /**
   * <p>Returns true if the StringBuilder parameter ends with the
   * ending String.</p>
   */

  public static boolean endsWith(StringBuilder in, String ending)
  {
    return (in.length() >= ending.length() &&
            (in.substring(in.length() - ending.length()).equals(ending)));
  }

  /**
   * <p>Test rig</p>
   */

  public static void main(String argv[])
  {
    System.out.println("\n-------------------- String insertPrefixPerLine Tests --------------------\n");

    String[] prefixText = {null,
                           "",
                           "Hi, my name\nis adam\nI am a test string\n\nHappy Birthday!\n",
                           "Hi, my name\nis adam\nI am a test string\n\nHappy Birthday!",
                           "Hi, my name\nis adam\nI am a test string\n\nHappy Birthday!\n\n",
                           "",
                           "\n"};
    String[] prefixResult = {"test: \n",
                             "test: \n",
                             "test: Hi, my name\ntest: is adam\ntest: I am a test string\ntest: \ntest: Happy Birthday!\n",
                             "test: Hi, my name\ntest: is adam\ntest: I am a test string\ntest: \ntest: Happy Birthday!\n",
                             "test: Hi, my name\ntest: is adam\ntest: I am a test string\ntest: \ntest: Happy Birthday!\ntest: \n",
                             "test: \n",
                             "test: \n"};

    for (int i = 0; i < prefixText.length; i++)
      {
        if (prefixResult[i].equals(insertPrefixPerLine(prefixText[i], "test: ")))
          {
            System.out.println("Pass " + (i + 1));
          }
        else
          {
            System.out.println("Fail " + (i + 1));

            System.out.println("Got:");
            System.out.println("*" + insertPrefixPerLine(prefixText[i], "test: ") + "*");

            System.out.println("Wanted:");
            System.out.println("*" + prefixResult[i] + "*");
          }
      }

    System.out.println("\n-------------------- String ensureEndsWith Tests --------------------\n");

    String test = "Hi!";
    String test2 = "Hi!n";
    String test3 = "Hi!nn";
    String test4 = "Hi!nnn";

    if (ensureEndsWith(test, "nn").equals(test3))
      {
        System.out.println("Pass 1");
      }
    else
      {
        System.out.println("Fail 1");
      }

    if (ensureEndsWith(test2, "nn").equals(test3))
      {
        System.out.println("Pass 2");
      }
    else
      {
        System.out.println("Fail 2");
      }

    if (ensureEndsWith(test3, "nn").equals(test3))
      {
        System.out.println("Pass 3");
      }
    else
      {
        System.out.println("Fail 3");
      }

    if (ensureEndsWith(test4, "nn").equals(test4))
      {
        System.out.println("Pass 4");
      }
    else
      {
        System.out.println("Fail 4");
      }

    System.out.println("\n-------------------- StringBuffer ensureEndsWith Tests --------------------\n");

    StringBuffer testBuf = new StringBuffer(test);
    ensureEndsWith(testBuf, "nn");

    if (testBuf.toString().equals(test3))
      {
        System.out.println("Pass 1");
      }
    else
      {
        System.out.println("Fail 1");
      }

    testBuf = new StringBuffer(test2);
    ensureEndsWith(testBuf, "nn");

    if (testBuf.toString().equals(test3))
      {
        System.out.println("Pass 2");
      }
    else
      {
        System.out.println("Fail 2");
      }

    testBuf = new StringBuffer(test3);
    ensureEndsWith(testBuf, "nn");

    if (testBuf.toString().equals(test3))
      {
        System.out.println("Pass 3");
      }
    else
      {
        System.out.println("Fail 3");
      }

    testBuf = new StringBuffer(test4);
    ensureEndsWith(testBuf, "nn");

    if (testBuf.toString().equals(test4))
      {
        System.out.println("Pass 4");
      }
    else
      {
        System.out.println("Fail 4");
      }

    System.out.println("\n-------------------- StringBuilder ensureEndsWith Tests --------------------\n");

    StringBuilder testBuild = new StringBuilder(test);
    ensureEndsWith(testBuild, "nn");

    if (testBuild.toString().equals(test3))
      {
        System.out.println("Pass 1");
      }
    else
      {
        System.out.println("Fail 1");
      }

    testBuild = new StringBuilder(test2);
    ensureEndsWith(testBuild, "nn");

    if (testBuild.toString().equals(test3))
      {
        System.out.println("Pass 2");
      }
    else
      {
        System.out.println("Fail 2");
      }

    testBuild = new StringBuilder(test3);
    ensureEndsWith(testBuild, "nn");

    if (testBuild.toString().equals(test3))
      {
        System.out.println("Pass 3");
      }
    else
      {
        System.out.println("Fail 3");
      }

    testBuild = new StringBuilder(test4);
    ensureEndsWith(testBuild, "nn");

    if (testBuild.toString().equals(test4))
      {
        System.out.println("Pass 4");
      }
    else
      {
        System.out.println("Fail 4");
      }

    System.out.println("\n-------------------- split() Tests --------------------\n");

    test = "10.8.[100-21].[1-253]\n10.3.[4-8].[1-253]\n129.116.[224-227].[1-253]";

    String results[] = StringUtils.split(test, "\n");

    for (int i = 0; i < results.length; i++)
      {
        System.out.println(results[i]);
        String results2[] = StringUtils.split(results[i], ".");

        for (int j = 0; j < results2.length; j++)
          {
            System.out.println("\t" + results2[j]);
          }
      }

    results = StringUtils.split("...", ".");

    if (results.length == 4)
      {
        System.out.println("Pass split test 1");
      }
    else
      {
        System.out.println("Fail split test 1");
      }

    for (int i = 0; i < results.length; i++)
      {
        if (results[i] == null)
          {
            System.out.println("Fail split test 2 [" + i + "]");
          }
        else
          {
            System.out.println("Pass split test 2 [" + i + "]");
          }
      }

    System.out.println("\n-------------------- containsOnly() Tests --------------------\n");


    if (StringUtils.containsOnly(test, "0123456789.[-]\n"))
      {
        System.out.println("Pass test 1");
      }
    else
      {
        System.out.println("Fail test 1");
      }

    if (!StringUtils.containsOnly(test, "0123456789"))
      {
        System.out.println("Pass test 2");
      }
    else
      {
        System.out.println("Fail test 2");
      }

    if (StringUtils.containsOnly("", "abcdefg"))
      {
        System.out.println("Pass test 3");
      }
    else
      {
        System.out.println("Fail test 3");
      }

    if (!StringUtils.containsOnly("test", null))
      {
        System.out.println("Pass test 4");
      }
    else
      {
        System.out.println("Fail test 4");
      }

    System.out.println("\n-------------------- containsAll() Tests --------------------\n");

    if (StringUtils.containsAll("test", null))
      {
        System.out.println("Pass test 5");
      }
    else
      {
        System.out.println("Fail test 5");
      }

    if (StringUtils.containsAll("test", ""))
      {
        System.out.println("Pass test 6");
      }
    else
      {
        System.out.println("Fail test 6");
      }

    if (StringUtils.containsAll("test", "t"))
      {
        System.out.println("Pass test 7");
      }
    else
      {
        System.out.println("Fail test 7");
      }

    if (StringUtils.containsAll("test", "tset"))
      {
        System.out.println("Pass test 8");
      }
    else
      {
        System.out.println("Fail test 8");
      }

    if (!StringUtils.containsAll("test", "test!"))
      {
        System.out.println("Pass test 9");
      }
    else
      {
        System.out.println("Fail test 9");
      }
  }
}
