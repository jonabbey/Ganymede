/**
 *
 * JCenterDialog.java
 *
 * A subclass of JDialog that centers itself when pack() is called.
 *
 *
 */

package arlut.csd.JDialog;

import java.awt.*;
import javax.swing.*;

public class JCenterDialog extends JDialog {

  private final boolean debug = false;

  Frame frame;

  public JCenterDialog()
  {
    this(null, null, false);
  }

  public JCenterDialog(Frame frame) 
  {
    this(frame, null, false);
  }

  public JCenterDialog(Frame frame, String title) 
  {
    this(frame, title, false);
  }
  public JCenterDialog(Frame frame, String title, boolean modal) 
  {
    super(frame, title, modal);
    
    this.frame = frame;
  }

  private void place(int width, int height)
  {
    if (frame != null)
      {
	Rectangle r = frame.getBounds();
	
	if (debug)
	  {
	    System.out.println("Bounds: " + r);
	  }
	
	// Sometimes a new JFrame() is passed in, and it won't have
	// anything interesting for bounds I don't think they are
	// null, but they are all 0 or something.  Might as well make
	// sure they are not null anyway.

	if ((r != null) && ((r.width != 0) && (r.height != 0)))
	  {
	    if (debug)
	      {
		System.out.println("Dialog is " + width + " wide and " + height + " tall.");
	      }
	    
	    setLocation(r.width/2 + r.x - width/2, r.height/2 + r.y - height/2);
	    if (debug)
	      {
		int loc = r.width/2 + r.x - width/2;
		int locy = r.height/2 + r.y - height/2;
		System.out.println("Setting location to : " + loc + "," + locy);
	      }
	  }
	else if (debug)
	  {
	    System.out.println("getBounds() returned null.");
	  }
      }
    else if (debug)
      {
	System.out.println("Parent frame is null.");
      }

  }

  /**
   *
   * This version of pack is for dialogs where you want it to keep the preferred size.
   */

  public void pack()
  {
    super.pack();
    pack(getSize().width,getSize().height);
  }

  /**
   * This pack is for dialogs where you want to set the size.
   */

  public void pack(int width, int height)
  {
    place(width, height);
    super.pack();
    setSize(width, height);
  }

}
