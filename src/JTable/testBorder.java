/*

  testBorder.java

  This class is used to allow the table in testTable to be
  sized randomly within a static sized panel.

  Copyright (C) 1997  The University of Texas at Austin.

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
  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
   
  Created: 31 January 1997
  Version: $Revision: 1.2 $ %D%
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
