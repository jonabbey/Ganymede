/*
  A panel with some insets.
*/

package arlut.csd.JDataComponent;

import java.awt.*;
import javax.swing.*;

public class JInsetPanel extends JPanel {
  
  int top, left, bottom, right;

  public JInsetPanel()
  {
    this(5, 5, 5, 5);
  }

  public JInsetPanel(int top, int left, int bottom, int right)
  {
    this.top = top;
    this.left = left;
    this.bottom = bottom;
    this.right = right;
  }

  public Insets getInsets()
  {
    Insets inset = new Insets(top, left, bottom, right);
    return inset;
  }

}//InsetPanel
