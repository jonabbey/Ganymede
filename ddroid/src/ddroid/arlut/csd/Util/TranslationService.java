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

import java.util.*;
import java.text.MessageFormat;

/*------------------------------------------------------------------------------
                                                                           class
                                                              TranslationService

------------------------------------------------------------------------------*/

/**
 * <p>Utility class to provide localized string assembly services.
 * This class is designed to use localized properties files to do
 * string lookup and synthesis.</p>
 *
 * @version $Id$
 * @author Jonathan Abbey
 */


public class TranslationService {

  // - Statics

  public static TranslationService getTranslationService(String resourceName, Locale locale) throws MissingResourceException
  {
    return new TranslationService(resourceName, locale);
  }

  // ---

  ResourceBundle bundle = null;
  Locale ourLocale = null;
  MessageFormat formatter = null;
  String lastPattern = null;
  String lastKey = null;
  Object[] singleArgs, doubleArgs, tripleArgs;
  String resourceName;

  /* -- */

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
  }

  /**
   * <p>This method takes a {@link java.text.MessageFormat MessageFormat}
   * pattern and a parameter and creates a localized string out of them.</p>
   *
   * <p>This heavily overloaded method is called 'l' for localize, and
   * it has such a short name so that I can use it everywhere in
   * Ganymede without concern.</p>
   */

  public synchronized String l(String key, String param)
  {
    String pattern = null;

    /* -- */

    if (this.lastKey == null || key != this.lastKey)
      {
	try
	  {
	    pattern = bundle.getString(key);
	  }
	catch (MissingResourceException ex)
	  {
	    return null;
	  }

	this.lastKey = key;
      }

    if (formatter == null)
      {
	formatter = new MessageFormat(pattern);
      }
    else if (pattern != this.lastPattern)
      {
	formatter.applyPattern(pattern);
      }

    this.lastPattern = pattern;

    this.singleArgs[0] = param; 

    return formatter.format(pattern, singleArgs);
  }

  /**
   * <p>This method takes a {@link java.text.MessageFormat MessageFormat}
   * pattern and a parameter and creates a localized string out of them.</p>
   *
   * <p>This heavily overloaded method is called 'l' for localize, and
   * it has such a short name so that I can use it everywhere in
   * Ganymede without concern.</p>
   */

  public synchronized String l(String key, int param)
  {
    String pattern = null;

    /* -- */

    if (this.lastKey == null || key != this.lastKey)
      {
	try
	  {
	    pattern = bundle.getString(key);
	  }
	catch (MissingResourceException ex)
	  {
	    return null;
	  }

	this.lastKey = key;
      }

    if (formatter == null)
      {
	formatter = new MessageFormat(pattern);
      }
    else if (pattern != this.lastPattern)
      {
	formatter.applyPattern(pattern);
      }

    this.lastPattern = pattern;

    this.singleArgs[0] = new Integer(param); 

    return formatter.format(pattern, singleArgs);
  }

  /**
   * <p>This method takes a {@link java.text.MessageFormat MessageFormat}
   * pattern and parameters and creates a localized string out of them.</p>
   *
   * <p>This heavily overloaded method is called 'l' for localize, and
   * it has such a short name so that I can use it everywhere in
   * Ganymede without concern.</p>
   */

  public synchronized String l(String key, String param, String param2)
  {
    String pattern = null;

    /* -- */

    if (this.lastKey == null || key != this.lastKey)
      {
	try
	  {
	    pattern = bundle.getString(key);
	  }
	catch (MissingResourceException ex)
	  {
	    return null;
	  }

	this.lastKey = key;
      }

    if (formatter == null)
      {
	formatter = new MessageFormat(pattern);
      }
    else if (pattern != this.lastPattern)
      {
	formatter.applyPattern(pattern);
      }

    this.lastPattern = pattern;

    this.doubleArgs[0] = param; 
    this.doubleArgs[0] = param2; 

    return formatter.format(pattern, doubleArgs);
  }

  /**
   * <p>This method takes a {@link java.text.MessageFormat MessageFormat}
   * pattern and parameters and creates a localized string out of them.</p>
   *
   * <p>This heavily overloaded method is called 'l' for localize, and
   * it has such a short name so that I can use it everywhere in
   * Ganymede without concern.</p>
   */

  public synchronized String l(String key, String param, int param2)
  {
    String pattern = null;

    /* -- */

    if (this.lastKey == null || key != this.lastKey)
      {
	try
	  {
	    pattern = bundle.getString(key);
	  }
	catch (MissingResourceException ex)
	  {
	    return null;
	  }

	this.lastKey = key;
      }

    if (formatter == null)
      {
	formatter = new MessageFormat(pattern);
      }
    else if (pattern != this.lastPattern)
      {
	formatter.applyPattern(pattern);
      }

    this.lastPattern = pattern;

    this.doubleArgs[0] = param; 
    this.doubleArgs[0] = new Integer(param2); 

    return formatter.format(pattern, doubleArgs);
  }

  /**
   * <p>This method takes a {@link java.text.MessageFormat MessageFormat}
   * pattern and parameters and creates a localized string out of them.</p>
   *
   * <p>This heavily overloaded method is called 'l' for localize, and
   * it has such a short name so that I can use it everywhere in
   * Ganymede without concern.</p>
   */

  public synchronized String l(String key, String param, String param2, String param3)
  {
    String pattern = null;

    /* -- */

    if (this.lastKey == null || key != this.lastKey)
      {
	try
	  {
	    pattern = bundle.getString(key);
	  }
	catch (MissingResourceException ex)
	  {
	    return null;
	  }

	this.lastKey = key;
      }

    if (formatter == null)
      {
	formatter = new MessageFormat(pattern);
      }
    else if (pattern != this.lastPattern)
      {
	formatter.applyPattern(pattern);
      }

    this.lastPattern = pattern;

    this.tripleArgs[0] = param; 
    this.tripleArgs[0] = param2; 
    this.tripleArgs[0] = param3; 

    return formatter.format(pattern, tripleArgs);
  }

  /**
   * <p>This method takes a {@link java.text.MessageFormat MessageFormat}
   * pattern and parameters and creates a localized string out of them.</p>
   *
   * <p>This heavily overloaded method is called 'l' for localize, and
   * it has such a short name so that I can use it everywhere in
   * Ganymede without concern.</p>
   */

  public synchronized String l(String key, String param, String param2, int param3)
  {
    String pattern = null;

    /* -- */

    if (this.lastKey == null || key != this.lastKey)
      {
	try
	  {
	    pattern = bundle.getString(key);
	  }
	catch (MissingResourceException ex)
	  {
	    return null;
	  }

	this.lastKey = key;
      }

    if (formatter == null)
      {
	formatter = new MessageFormat(pattern);
      }
    else if (pattern != this.lastPattern)
      {
	formatter.applyPattern(pattern);
      }

    this.lastPattern = pattern;

    this.tripleArgs[0] = param; 
    this.tripleArgs[0] = param2; 
    this.tripleArgs[0] = new Integer(param3); 

    return formatter.format(pattern, tripleArgs);
  }

  public String toString()
  {
    return("TranslationService: " + resourceName + ", locale = " + ourLocale.toString());
  }
}

