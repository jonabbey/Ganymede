/*
 
  JErrorDialog.java

  A simple dialog giving an error message.

  */

package arlut.csd.JDialog;

import java.awt.Image;
import java.awt.Frame;

/**
 * Simple way to throw up a StringDialog.
 *
 */
public class JErrorDialog {

  StringDialog d;

  public JErrorDialog(Frame parent, String message)
  {
    this(parent, "Error", message, null);
  }

  public JErrorDialog(Frame parent, String message, Image icon)
  {
    this(parent, "Error", message, icon);
  }

  public JErrorDialog(Frame parent, String title, String message)
  {
    this(parent, title, message, null);
  }

  public JErrorDialog(Frame parent, String title, String message, Image icon)
  {
    d = new StringDialog(parent, title, message, "Ok", null, icon);
    d.DialogShow();
  }

  public void setVisible(boolean visible)
  {
    d.setVisible(visible);
  }

}
