/*
  A panel with some insets.
*/

package arlut.csd.JDialog;

import java.awt.Insets;

import javax.swing.JPanel;

public class JInsetPanel extends JPanel {
  
  int top, left, bottom, right;
  
  Insets inset;

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
      
      inset = new Insets(top, left, bottom, right);

    }

  public Insets getInsets()
    {
      return inset;
    }

}//InsetPanel
