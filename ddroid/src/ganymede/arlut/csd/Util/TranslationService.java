/*

   TranslationService.java

   Utility class to provide localized string assembly services.
   This class is designed to use localized properties files to do
   string lookup and synthesis.

   Created: 7 May 2004

   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   Last Mod Date: $Date$
   SVN URL: $HeadURL$

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Directory Droid Directory Management System
 
   Copyright (C) 1996 - 2004
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
   along with this program; if not, write to the Free Software
   Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
   02111-1307, USA

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
 * <p>Things to think about for TranslationServices..</p>
 *
 * <p>We'd like a way to force all TranslationServices objects to
 * reload their bundles, but Java's ResourceBundle and
 * PropertyResourceBundle classes really don't support that.  It would
 * be nice to be able to cause Directory Droid to reload its
 * localization strings on demand, rather than forcing a
 * stop/restart, but we'd have to recreate the locale seeking logic
 * for locating and loading the property files to do this.</p>
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
  Object[] singleArgs, doubleArgs, tripleArgs, quadArgs;
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

    this.singleArgs = new Object[1];
    this.doubleArgs = new Object[2];
    this.tripleArgs = new Object[3];
    this.quadArgs = new Object[4];
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
   * <p>This method takes a localization key and returns the localized
   * string that matches it in this TranslationService.</p>
   *
   * <p>This heavily overloaded method is called 'l' for localize, and
   * it has such a short name so that I can use it everywhere in
   * Directory Droid without concern.</p>
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
   * <p>This heavily overloaded method is called 'l' for localize, and
   * it has such a short name so that I can use it everywhere in
   * Directory Droid without concern.</p>
   */

  public synchronized String l(String key, Object param)
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

    this.singleArgs[0] = param;

    result = this.format(pattern, singleArgs);

    this.singleArgs[0] = null;

    return result;
  }

  /**
   * <p>This method takes a localization key and parameters and
   * creates a localized string out of them.</p>
   *
   * <p>This heavily overloaded method is called 'l' for localize, and
   * it has such a short name so that I can use it everywhere in
   * Directory Droid without concern.</p>
   */

  public synchronized String l(String key, Object param, Object param2)
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

    this.doubleArgs[0] = param; 
    this.doubleArgs[1] = param2; 

    result = this.format(pattern, doubleArgs);

    this.doubleArgs[0] = null;
    this.doubleArgs[1] = null;

    return result;
  }

  /**
   * <p>This method takes a localization key and parameters and
   * creates a localized string out of them.</p>
   *
   * <p>This heavily overloaded method is called 'l' for localize, and
   * it has such a short name so that I can use it everywhere in
   * Directory Droid without concern.</p>
   */

  public synchronized String l(String key, Object param, Object param2, Object param3)
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

    this.tripleArgs[0] = param; 
    this.tripleArgs[1] = param2; 
    this.tripleArgs[2] = param3; 

    result = this.format(pattern, tripleArgs);

    this.tripleArgs[0] = null;
    this.tripleArgs[1] = null; 
    this.tripleArgs[2] = null; 

    return result;
  }

  /**
   * <p>This method takes a localization key and parameters and
   * creates a localized string out of them.</p>
   *
   * <p>This heavily overloaded method is called 'l' for localize, and
   * it has such a short name so that I can use it everywhere in
   * Directory Droid without concern.</p>
   */

  public synchronized String l(String key, Object param, Object param2, Object param3, Object param4)
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
    
    this.quadArgs[0] = param; 
    this.quadArgs[1] = param2; 
    this.quadArgs[2] = param3;
    this.quadArgs[3] = param4;

    result = this.format(pattern, quadArgs);

    this.quadArgs[0] = null;
    this.quadArgs[1] = null;
    this.quadArgs[2] = null;
    this.quadArgs[3] = null;

    return result;
  }

  /**
   * <p>This method takes a localization key and parameters and
   * creates a localized string out of them.</p>
   *
   * <p>This heavily overloaded method is called 'l' for localize, and
   * it has such a short name so that I can use it everywhere in
   * Directory Droid without concern.</p>
   */

  public synchronized String l(String key, Object param, Object param2, Object param3, Object param4, Object param5)
  {
    String pattern = null;
    String result;
    Object quintArgs[] = new Object[5];

    /* -- */

    try
      {
	pattern = bundle.getString(key);
      }
    catch (MissingResourceException ex)
      {
	return null;
      }
    
    quintArgs[0] = param;
    quintArgs[1] = param2;
    quintArgs[2] = param3;
    quintArgs[3] = param4;
    quintArgs[4] = param5;

    result = this.format(pattern, quintArgs);

    return result;
  }

  /**
   * <p>This method takes a localization key and parameters and
   * creates a localized string out of them.</p>
   *
   * <p>This heavily overloaded method is called 'l' for localize, and
   * it has such a short name so that I can use it everywhere in
   * Directory Droid without concern.</p>
   */

  public synchronized String l(String key, Object param, Object param2, Object param3, Object param4, Object param5, Object param6)
  {
    String pattern = null;
    String result;
    Object sextArgs[] = new Object[6];

    /* -- */

    try
      {
	pattern = bundle.getString(key);
      }
    catch (MissingResourceException ex)
      {
	return null;
      }
    
    sextArgs[0] = param;
    sextArgs[1] = param2;
    sextArgs[2] = param3;
    sextArgs[3] = param4;
    sextArgs[4] = param5;
    sextArgs[5] = param6;

    result = this.format(pattern, sextArgs);

    return result;
  }

  public String toString()
  {
    return("TranslationService: " + resourceName + ", locale = " + ourLocale.toString());
  }

  private String format(String pattern, Object params[])
  {
    if (formatter == null)
      {
	formatter = new MessageFormat(pattern);
      }
    else if (pattern != this.lastPattern)
      {
	formatter.applyPattern(pattern);
	lastPattern = pattern;
      }

    String result = formatter.format(pattern, params);

    if (wordWrapCols > 0 && result.length() > wordWrapCols)
      {
	result = WordWrap.wrap(result, wordWrapCols);
      }

    return result;
  }
}
