/*

   messageDialog.java

   A dialog class used to display text and HTML with an optional
   image on the left side, used for the about.. and motd features
   in the Ganymede client.
   
   Created: 16 September 1998
   Version: $Revision: 1.5 $ %D%
   Module By: Mike Mulvaney
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.client;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import arlut.csd.JDialog.JCenterDialog;

/*------------------------------------------------------------------------------
                                                                           class
                                                                   messageDialog

------------------------------------------------------------------------------*/

/**
 *
 * A dialog class used to display text and HTML with an optional
 * image on the left side, used for the about.. and motd features
 * in the Ganymede client.
 *   
 * @version $Revision: 1.5 $ %D%
 * @author Mike Mulvaney
 *
 */

public class messageDialog extends JCenterDialog implements ActionListener{

  private final static boolean debug = false;

  // ---

  JEditorPane
    text;

  JButton
    ok;

  gclient 
    gc;

  /* -- */

  public messageDialog(gclient gc, String title, Image image)
  {
    super(gc, title, true);

    this.gc = gc;
    
    // There are three panels.  pane will become the content pane, so
    // it is the top container.  It contains two other panels:
    // topPanel, and buttonPanel.  topPanel contains everything but
    // the ok button, which is in the buttonPanel.  I do this because
    // it is hard to put a button in the center of a panel with the
    // GridBagLayout.

    JPanel topPanel = new JPanel(new BorderLayout()); // gbl?
    JPanel buttonPanel = new JPanel();
    JPanel pane = new JPanel(new BorderLayout());
    
    JLabel picture = null;

    if (image == null)
      {
	picture = new JLabel();
      }
    else
      {
	picture = new JLabel(new ImageIcon(image));
      }

    text = new JEditorPane();
    text.setEditable(false);
    text.setForeground(java.awt.Color.black);

    topPanel.add("Center", new JScrollPane(text));
    topPanel.add("West", picture);
    topPanel.add("South", new JSeparator());

    // Create the button Panel for the bottom
    ok = new JButton("Ok");
    ok.addActionListener(this);
    buttonPanel.add(ok);

    pane.add("Center", topPanel);
    pane.add("South", buttonPanel);
    this.setContentPane(pane);

    layout(450,200);
  }

  /**
   *
   * Load this message dialog with HTML content.
   *
   * @param s An HTML document held within a string.
   *
   */

  public void setHtmlText(String s)
  {
    if (debug)
      {
	System.out.println("Setting text to: " + s);
      }
    
    text.setContentType("text/html");
    text.setText(s);
    layout(450,300);
  }

  /**
   *
   * Load this message dialog with Unicode content.
   *
   * @param s The message content to be displayed.
   *
   */

  public void setPlainText(String s)
  {
    text.setContentType("text/plain");
    text.setText(s);
    layout(450,300);
  }

  public void actionPerformed(ActionEvent e)
  {
    setVisible(false);
  }

  public void layout(int width, int height)
  {
    // Uses a special pack in JCenterDialog
    pack(width, height);
  }
}
