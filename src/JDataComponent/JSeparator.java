/**
   JSeparator.java

   A simple separator.

*/

package arlut.csd.JDataComponent;

import java.awt.*;
import javax.swing.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                      JSeparator

------------------------------------------------------------------------------*/

public class JSeparator extends JComponent {

  public final static int DEFAULT_THICKNESS = 2;

  // ---

  int thickness;

  Color
    darker = null,
    brighter = null;

  /* -- */

  public JSeparator()
  {
    this(JSeparator.DEFAULT_THICKNESS);
  }

  public JSeparator(int width)
  {
    this(width, null, null);
  }

  public JSeparator(int width, Color highlight, Color back)
  {
    thickness = width;
    darker = back;
    brighter = highlight;
  }

  // Count on the Layout manager to get it right

  public Dimension getPreferredSize()
  {
    return new Dimension(thickness + 4, thickness + 4);
  }

  public Dimension getMinimumSize()
  {
    return getPreferredSize();
  }

  public void paint(Graphics g)
  {
    if (darker == null)
      {
	darker = getParent().getBackground().darker();
      }
    
    if (brighter == null)
      {
	brighter = getParent().getBackground().brighter();
      }

    Dimension size = getSize();

    if (size.width > size.height)
      {
	paintHorizontal(g, size, darker, brighter);
      }
    else
      {
	paintVertical(g, size, darker, brighter);
      }
  }

  private void paintHorizontal(Graphics g, Dimension size, Color top, Color bottom)
  {
    g.setColor(top);
    int y = (size.height/2) - (thickness/2);

    while (y < (size.height/2))
      {
	g.drawLine(0, y, size.width, y);
	++y;
      }

    g.setColor(bottom);
    y = size.height/2;

    while (y < ((size.height/2) + (thickness/2)))
      {
	g.drawLine(0, y, size.width, y);
	++y;
      }
  }

  private void paintVertical(Graphics g, Dimension size, Color left, Color right)
  {
    g.setColor(left);
    int i = (size.width/2) - (thickness/2);

    while (i < (size.width/2))
      {
	g.drawLine(i, 0, i, size.height);
	++i;
      }

    g.setColor(right);
    i = size.width/2;

    while (i < ((size.width/2) + (thickness/2)))
      {
	g.drawLine(i,0,i, size.height);
	i++;
      }
  }

  public final static void main(String[] argv)
  {
    JFrame f = new JFrame();
    f.getContentPane().setLayout(new BorderLayout());

    f.getContentPane().add("East", new JSeparator());
    f.getContentPane().add("South", new JSeparator(10));
    f.getContentPane().add("Center", new JMultiLineLabel("Hi there, how ya doing.\nThis is a test of the separator\nthat yousee on the top and\nbottom of this frame."));

    f.pack();
    f.show();
  }
}
