/*

   framePanel.java

   The individual frames in the windowPanel.
   
   Created: 4 September 1997
   Version: $Revision: 1.1 $ %D%
   Module By: Michael Mulvaney
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.client;

import java.awt.*;
import com.sun.java.swing.*;

public class framePanel extends JInternalFrame {
  
  final static boolean debug = true;

  public framePanel(db_object object, boolean editable, windowPanel parent, String title)
    {
      if (debug)
	{
	  System.out.println("Adding new framePanel");
	}
      
      setMaxable(true);
      setResizable(true);
      setClosable(!editable);
      setIconable(true);
      setLayout(new BorderLayout());

      addPropertyChangeListener(parent);
      
      Image fi = PackageResources.getImageResource(this, "dynamite.gif", getClass());
      setFrameIcon(new ImageIcon(fi));
      setBackground(ClientColor.WindowBG);
      
      setTitle(title);
	  
      setLayout(new BorderLayout());
        JInsetPanel center = new JInsetPanel(1,1,1,1);
    center.setLayout(new BorderLayout());
    center.setBackground(ClientColor.WindowBG);
    center.add("Center", new containerPanel(object, editable, parent, this));
    w.add("Center", center);

    if (editable)
      {
	if (debug)
	  {
	    System.out.println("This is editable, you get no button");
	  }
      }
    else
      {
	if (debug)
	  {
	    System.out.println("Not editable, you get some buttons");
	  }

      }

    JMenuBar mb = createMenuBar(editable, object, w);
    w.setMenuBar(mb);

    //System.out.println("   adding to windowBar " + title);
    w.setBounds(windowCount*20, windowCount*20, 400,250);
    



    }

}
