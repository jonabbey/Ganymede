/*
  LAFMenu.java

  A simple menu to change look and feel.

*/

package arlut.csd.JDataComponent;

import javax.swing.*;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Container;
import java.awt.Component;

public class LAFMenu extends JMenu implements ActionListener{

  private final boolean debug = false;

  JsetValueCallback my_parent;

  boolean allowCallback = false;

  Container 
    root = null;

  public LAFMenu(Container root)
  {
    this(root, "Look");
  }

  public LAFMenu(Container root, String title)
  {
    super(title);
    if (debug)
      {
	System.out.println("Current look and feel: " + UIManager.getLookAndFeel().getName());
      }

    this.root = root;

    ButtonGroup group = new ButtonGroup();

    UIManager.LookAndFeelInfo[] info = UIManager.getInstalledLookAndFeels();
    for (int i = 0; i < info.length; i++)
      {
	JCheckBoxMenuItem mi = new JCheckBoxMenuItem(info[i].getName());
	mi.setActionCommand(info[i].getClassName());
	if (debug)
	  {
	    System.out.println(info[i].getClassName());
	  }
	group.add(mi);
	//mi.setEnabled(info[i].isSupportedLookAndFeel());
	mi.addActionListener(this);
	add(mi);
      }

    updateMenu();

  }

  public void setCallback(JsetValueCallback parent)
  {
    if (parent == null)
      {
	throw new IllegalArgumentException("Invalid Parameter: parent cannot be null");
      }
    
    my_parent = parent;

    allowCallback = true;
  }

  public void setLAF(String look)
  {
    try 
      {
	UIManager.setLookAndFeel(look);
	if (root != null)
	  {
	    root.invalidate();
	    SwingUtilities.updateComponentTreeUI(root);
	    root.validate();
	  }
      } 
    catch (javax.swing.UnsupportedLookAndFeelException e)
      {
	if (allowCallback)
	  {
	    try
	      {
		my_parent.setValuePerformed(new JValueObject(this,
							     "That look and feel is unsupported on this platform.",
							     JValueObject.ERROR));
	      }
	    catch (java.rmi.RemoteException rx)
	      {
		System.out.println("Caught a remote exception; " + rx);
	      }

	  }
	System.out.println("That look and feel is not supported on this platform.");
	updateMenu();
      }
    catch ( java.lang.InstantiationException e)
      {
	System.out.println("Exception: " + e);
	updateMenu();
      }
    catch (java.lang.ClassNotFoundException e)
      {
	System.out.println("Exception: " + e);
	updateMenu();
      }
    catch (java.lang.IllegalAccessException e)
      {
	System.out.println("Exception: " + e);
	updateMenu();
      }
    
  }
  
  public void actionPerformed(ActionEvent e)
    {
      String current = UIManager.getLookAndFeel().toString();
      if ((current != null) && (current.indexOf(e.getActionCommand()) > 0))
	{
	  if (debug)
	    {
	      System.out.println("Attempt to change to current LAF, I see your trick!");
	    }
	  return;
	}

      try
	{
	  setLAF(e.getActionCommand());
	}
      catch (Exception ex)
	{
	  if (debug)
	    {
	      System.out.println("Something went wrong switching look and feels: " + ex);
	    }

	  updateMenu();
	}

    }

  public void updateMenu()
  {

    String current = UIManager.getLookAndFeel().toString().toLowerCase();
    
    Component[] items = getMenuComponents();
    for (int i = 0; i < items.length; i++)
      {
	JCheckBoxMenuItem mi = (JCheckBoxMenuItem)items[i];
	String text = mi.getActionCommand().toLowerCase();
	if (current.indexOf(text) > -1)
	  {
	    mi.setSelected(true);
	    break;
	  }
	else
	  {
	    mi.setSelected(false);
	  }
      }
	   

  }


}
