/**
 * 
 *  Color definitions for the ganymede client.
 *
 */

package arlut.csd.ganymede.client;

import java.awt.Color;
import java.awt.SystemColor;

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

  public final static Color background = Color.white;

  public final static Color desktop = SystemColor.desktop;
  public final static Color menu = SystemColor.menu;
  public final static Color menuText = Color.black;
  public final static Color scrollbar = SystemColor.scrollbar;
  public final static Color text = SystemColor.text;
  public final static Color window = SystemColor.window;
  public final static Color windowBorder = SystemColor.windowBorder;
  public final static Color windowText = SystemColor.windowText;
}
