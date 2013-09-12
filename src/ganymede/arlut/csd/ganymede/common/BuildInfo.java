/*

   BuildInfo.java

   Created: 12 September 2013

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

package arlut.csd.ganymede.common;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import arlut.csd.Util.TranslationService;

/*------------------------------------------------------------------------------
                                                                           class
                                                                       BuildInfo

------------------------------------------------------------------------------*/

/**
 * <p>This class provides access to build-time resources.</p>
 */

public class BuildInfo {

  /**
   * <p>TranslationService object for handling string localization in
   * the Ganymede server.</p>
   */

  static final public TranslationService ts =
    TranslationService.getTranslationService("arlut.csd.ganymede.common.BuildInfo");

  private final static Properties properties = new Properties();
  private static boolean loaded = false;
  private static SimpleDateFormat dateParser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssz");

  // ---

  private synchronized static void loadProps()
  {
    if (!loaded)
      {
        try
          {
            properties.load(BuildInfo.class.getResourceAsStream("build.properties"));
            loaded = true;
          }
        catch (IOException ex)
          {
          }
      }
  }

  public static Date getBuildDate()
  {
    loadProps();

    String dateString = properties.getProperty("build_date");

    try
      {
        return dateParser.parse(dateString);
      }
    catch (ParseException ex)
      {
        return null;
      }
  }

  public static String getBuildJVM()
  {
    loadProps();

    return properties.getProperty("build_jvm");
  }

  public static String getBuildHost()
  {
    loadProps();

    return properties.getProperty("build_host");
  }

  public static String getReleaseString()
  {
    Date buildDate = getBuildDate();

    // "EEE, dd MMM yyyy HH:mm:ss z"
    SimpleDateFormat dateFormatter = new SimpleDateFormat(ts.l("date_format"));
    String dateString = dateFormatter.format(buildDate);

    // "Built ${0} on ${1} with Java ${2}"
    return ts.l("release_string", dateString, getBuildJVM(), getBuildHost());
  }
}
