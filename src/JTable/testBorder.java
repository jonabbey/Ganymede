/*

   testBorder.java

   This class is used to allow the table in testTable to be
   sized randomly within a static sized panel.
   
   Created: 31 January 1997
   Version: $Revision: 1.1 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

import java.awt.*;
import java.util.*;

public class testBorder extends Panel {

  int xsize, ysize;
  int xinset, yinset;
  Random randomizer = null;

  public testBorder(int xsize, int ysize, Component borderMe)
  {
    this.xsize = xsize;
    this.ysize = ysize;
    randomizer = new Random();
    setLayout(new BorderLayout());
    add("Center", borderMe);
  }

  public Insets insets()
  {
    return new Insets(xinset, yinset, xinset, yinset);
  }

  public void paint(Graphics g)
  {
    Dimension mySize = getSize();
    Insets myInsets = getInsets();

    g.setColor(SystemColor.desktop);

    g.fillRect(0,0, mySize.width, myInsets.top);

    g.fillRect(0,0, myInsets.left, mySize.height);

    g.fillRect(mySize.width - myInsets.right, 0,
	       myInsets.right, mySize.height);

    g.fillRect(0, mySize.height - myInsets.bottom,
	       mySize.width, mySize.height);
  }

  public void randomize()
  {
    xinset = (int) (xsize * randomizer.nextFloat() * 0.25);
    yinset = (int) (ysize * randomizer.nextFloat() * 0.25);
    this.invalidate();
  }
  
}
