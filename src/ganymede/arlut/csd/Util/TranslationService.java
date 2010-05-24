/*

   TranslationService.java

   Utility class to provide localized string assembly services.
   This class is designed to use localized properties files to do
   string lookup and synthesis.

   Created: 7 May 2004

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996-2010
   The University of Texas at Austin

   Contact information

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

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/*------------------------------------------------------------------------------
                                                                           class
                                                              TranslationService

------------------------------------------------------------------------------*/

/**
 * <p>Utility class to provide localized string assembly services.
 * This class is designed to use localized properties files to do
 * string lookup and synthesis.</p>
 *
 * <p>The Ganymede source tree uses TranslationService pervasively to
 * handle translation for all text message strings in the server,
 * client, and admin console.</p>
 *
 * <p>This class must be used in conjunction with a particular usage
 * convention in order to support automatic validation of message
 * files with the 'ant validate' task in the Ganymede build.xml file.</p>
 *
 * <p>That convention is that every class that needs language
 * translation services must declare a static final TranslationService
 * object named 'ts' which is initialized with the fully qualified
 * name of the class to which it belongs.</p>
 *
 * <p>Actual translation calls should all be in the form of
 * ts.l("messageName"), possibly with extra parameters, such as
 * ts.l("messageName", arg1, arg2), and so forth.  The 'l' method of
 * TranslationService is designed to be as compact as possible so that
 * it can be used wherever you would normally use a string.  It
 * returns a String formatted according to whatever language-sensitive
 * property files are defined in Ganymede's src/resources
 * directory.</p>
 *
 * <p>If you follow these conventions, the 'ant validate' task will be
 * able to automatically analyze your code and cross reference it
 * against the message property files under src/resources.  'ant
 * validate' will warn you if you are missing messages, or if the
 * messages are malformed, or if the messages specify a different
 * number of parameters than the source code which users the messages
 * is expecting.</p>
 *
 * <p>So, for use within Ganymede, please be sure always to follow
 * these conventions.</p>
 *
 * @version $Id$
 * @author Jonathan Abbey
 */

public class TranslationService {

  /**
   * <p>This is a factory method for creating TranslationService
   * objects.  We use a factory method so that we have the option
   * later on of tracking the TranslationService objects we create and
   * possibly supporting some kind of reload functionality, howsomever
   * primitive.</p>
   *
   * @param resourceName The package-qualified resource class name to
   * load.  We automatically support the use of properties files here.
   *
   * @param locale A Locale object controlling the particular region we're
   * going to try and get translations for, though we'll fallback to our
   * generic translation classes (i.e., the English ones) if we don't have
   * any localization classes for the given Locale.
   */

  public static TranslationService getTranslationService(String resourceName, Locale locale) throws MissingResourceException
  {
    return new TranslationService(resourceName, locale);
  }

  /**
   * <p>This is a factory method for creating TranslationService
   * objects.  We use a factory method so that we have the option
   * later on of tracking the TranslationService objects we create and
   * possibly supporting some kind of reload functionality, howsomever
   * primitive.</p>
   *
   * @param resourceName The package-qualified resource class name to
   * load.  We automatically support the use of properties files here.
   */

  public static TranslationService getTranslationService(String resourceName) throws MissingResourceException
  {
    return new TranslationService(resourceName, null);
  }

  // ---

  ResourceBundle bundle = null;
  Locale ourLocale = null;
  MessageFormat formatter = null;
  String lastPattern = null;
  String resourceName;
  int wordWrapCols=0;

  /* -- */

  /**
   * <p>We've declared the constructor private so as to force use of
   * the static factory method.</p>
   *
   * @param resourceName The package-qualified resource class name to
   * load.  We automatically support the use of properties files here.
   *
   * @param locale A Locale object controlling the particular region we're
   * going to try and get translations for, though we'll fallback to our
   * generic translation classes (i.e., the English ones) if we don't have
   * any localization classes for the given Locale.
   */

  private TranslationService(String resourceName, Locale locale) throws MissingResourceException
  {
    if (locale != null)
      {
	this.ourLocale = locale;
      }
    else
      {
	this.ourLocale = Locale.getDefault();
      }

    this.resourceName = resourceName;

    bundle = ResourceBundle.getBundle(resourceName, ourLocale);
  }

  /**
   * <p>This method sets the desired word wrap columns for this
   * TranslationService.  Any formatted strings returned by this
   * TranslationService will be wrapped to the number of columns
   * specified.</p>
   *
   * <p>If cols is 0, word wrapping will be disabled.</p>
   *
   * @return The number of columns that this TranslationService was previously
   * wrapping at
   */

  public int setWordWrap(int cols)
  {
    int old_value = this.wordWrapCols;

    if (cols <= 0)
      {
	wordWrapCols = 0;
      }
    else
      {
	wordWrapCols = cols;
      }

    return old_value;
  }

  /**
   * <p>This method returns true if this TranslationService has a
   * non-empty, non-null resource string corresponding to the key
   * parameter.</p>
   */

  public boolean hasPattern(String key)
  {
    String pattern;

    /* -- */

    try
      {
	pattern = bundle.getString(key);
      }
    catch (MissingResourceException ex)
      {
	return false;
      }

    if (pattern.equals(""))
      {
	return false;
      }

    return true;
  }

  /**
   * <p>This method takes a localization key and returns the localized
   * string that matches it in this TranslationService.</p>
   *
   * <p>This heavily overloaded method is called 'l' for localize, and
   * it has such a short name so that I can use it everywhere in
   * Ganymede without concern.</p>
   */

  public String l(String key)
  {
    String pattern = null;
    String result;

    /* -- */

    try
      {
	pattern = bundle.getString(key);
      }
    catch (MissingResourceException ex)
      {
	return null;
      }

    // we don't actually need to use format with no template
    // substitutions, but if we don't, then we'll have different
    // property file rules for how to handle single quotes in the two
    // cases.  Easier just to always use format and insist that all
    // single quotes in the property files are doubled up.

    // Thanks Sun, for such a brain-dead quoting system.  What was
    // wrong with backslash-escape, again?

    result = this.format(pattern, null);

    return result;
  }

  /**
   * <p>This method takes a localization key and a parameter and
   * creates a localized string out of them.</p>
   *
   * <p>This method is called 'l' for localize, and it has such a
   * short name so that I can use it everywhere in Ganymede with
   * minimal source code disruption.</p>
   *
   * This method obviously requires Java 5 due to its use of varargs.
   */

  public String l(String key, Object... params)
  {
    String pattern = null;

    /* -- */

    try
      {
	pattern = bundle.getString(key);
      }
    catch (MissingResourceException ex)
      {
	return null;
      }

    return this.format(pattern, params);
  }

  public String toString()
  {
    return("TranslationService: " + resourceName + ", locale = " + ourLocale.toString());
  }

  private String format(String pattern, Object params[])
  {
    String result = null;

    // we have to synchronize to protect the formatter object

    synchronized (this)
      {
        if (formatter == null)
          {
            formatter = new MessageFormat(pattern);
          }
        else if (!pattern.equals(this.lastPattern))
          {
            formatter.applyPattern(pattern);
            lastPattern = pattern;
          }

        result = formatter.format(pattern, params);
      }

    if (wordWrapCols > 0 && result.length() > wordWrapCols)
      {
	result = WordWrap.wrap(result, wordWrapCols);
      }

    return result;
  }
}
