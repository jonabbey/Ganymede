/*

   ClientColor.java

   Color definitions for the ganymede client.
   
   Created: 5 November 1998

   Module By: Michael Mulvaney

   -----------------------------------------------------------------------
            
   Ganymede Directory Management System
 
   Copyright (C) 1996-2010
   The University of Texas at Austin

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

package arlut.csd.ganymede.client;

import java.awt.Color;
import java.awt.SystemColor;

/*------------------------------------------------------------------------------
                                                                           class
                                                                     ClientColor

------------------------------------------------------------------------------*/

/**
 *  Color definitions for the Ganymede client.
 */

public class ClientColor {

  // Some color RGB's

  public final static Color Brass = new Color(181,166,66);
  public final static Color Copper = new Color(217,135,25);
  public final static Color Bronze = new Color(166,125,61);
  public final static Color Wood = new Color(133,94,66);
  public final static Color DimGrey = new Color(84,84,84);
  public final static Color Khaki = new Color(159,159,95);
  public final static Color Sienna = new Color(142, 107, 35);
  public final static Color Brick = new Color(12763045);

  // Use these in the client
  public final static Color vectorTitles = Color.blue;
  public final static Color vectorFore = Color.white;
  public final static Color vectorTitlesInvalid = Color.red;
  public final static Color buttonBG = Color.lightGray;

  public final static Color BG = Color.lightGray;
  public final static Color WindowBG = Color.lightGray;
  public final static Color ButtonPanelBG = Khaki;
  public final static Color ComponentBG = Color.lightGray.brighter();

  public final static Color background = Color.lightGray;

  public final static Color desktop = SystemColor.desktop;
  public final static Color menu = SystemColor.menu;
  public final static Color menuText = Color.black;
  public final static Color scrollbar = SystemColor.scrollbar;
  public final static Color text = SystemColor.text;
  public final static Color window = SystemColor.window;
  public final static Color windowBorder = SystemColor.windowBorder;
  public final static Color windowText = SystemColor.windowText;
}
