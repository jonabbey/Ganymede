/*
 
  JErrorDialog.java

  A simple dialog giving an error message.

  */

package arlut.csd.JDialog;

import java.awt.Frame;
import java.awt.Image;

import arlut.csd.Util.TranslationService;

/**
 * Simple way to throw up a StringDialog.
 *
 */
public class JErrorDialog {

  /**
   * <p>TranslationService object for handling string localization in
   * the Ganymede server.</p>
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.JDialog.JErrorDialog");

  // ---

  StringDialog d;

  public JErrorDialog(Frame parent, String message)
  {
    this(parent, ts.l("global.error"), message, null);
  }

  public JErrorDialog(Frame parent, String message, Image icon)
  {
    this(parent, ts.l("global.error"), message, icon);
  }

  public JErrorDialog(Frame parent, String title, String message)
  {
    this(parent, title, message, null);
  }

  public JErrorDialog(Frame parent, String title, String message, Image icon)
  {
    d = new StringDialog(parent, title, message, ts.l("global.ok"), null, icon);
    d.DialogShow();
  }

  public void setVisible(boolean visible)
  {
    d.setVisible(visible);
  }
}
