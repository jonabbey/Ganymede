
package arlut.csd.ganymede.client;

import java.awt.*;
import java.awt.event.*;
import com.sun.java.swing.*;
import arlut.csd.JDialog.JCenterDialog;

public class messageDialog extends JCenterDialog implements ActionListener{

  private final boolean debug = false;

  JEditorPane
    text;

  JButton
    ok;

  gclient 
    gc;

  /*
  GridBagLayout
    gbl;

  GridBagConstraints
    gbc;
  */
  public messageDialog(gclient gc, String title, Image image)
  {
    super(gc, title, true);

    this.gc = gc;
    
    //gbl = new GridBagLayout();
    //gbc = new GridBagConstraints();

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
