/*
   JchoiceList.java

   
   Created: 1 Oct 1996
   Version: $Revision: 1.1 $ %D%
   Module By: Navin Manohar
   Applied Research Laboratories, The University of Texas at Austin
*/


package arlut.csd.JDataComponent;

import java.awt.*;
import java.awt.event.*;

/**********************************************************************
JchoiceList

This class is basically a java.awt.List object with some added
functionality.  When the user makes a selection, a callback is
made to send the string representation of that selection to whatever
component in attached to the JchoiceList.

**********************************************************************/
public class JchoiceList extends Panel {

  JchoiceInterface my_cint = null;

  List l;
  Button restore;
  Button apply;

  boolean changed = false;

  public JchoiceList()
  {
    setLayout(new BorderLayout());

    l = new List();
    restore = new Button("Restore");

    add("North",l);
    add("South",restore);
  }

  public JchoiceList(JchoiceInterface cint)
  {
    this();

    if (cint == null)
      throw new IllegalArgumentException("Illegal Argument: The handle to JchoiceInterface is null");

    my_cint = cint;
  }


  public void attach(JchoiceInterface cint)
  {
    if (cint == null)
      throw new IllegalArgumentException("Illegal Argument: The handle to JchoiceInterface is null");

    if (my_cint != null)
      my_cint.unAttach();

    my_cint = cint;
    clear();
  }

  public void detach()
  {
    my_cint = null;
    clear();
  }

  public void clear()
  {
    l.removeAll();
  }
  
  public void setChoices(String[] choices)
  {
    if (choices == null)
      throw new IllegalArgumentException("Illegal Argument: The array of choices is null");

    clear();

    for (int i=0;i<choices.length;i++)
	l.addItem(choices[i]);
  }

  public void actionPerformed(ActionEvent evt)
    {
      if (my_cint == null)
	return;

      if (evt.getSource() == restore)
	{
	  my_cint.restoreValue();
	  my_cint.notifyComponent();
	  changed = true;
	  return;
	}
      if (evt.getSource() == l)
	{
	  my_cint.setVal(evt.paramString());
	  my_cint.notifyComponent();
	  changed = true;
	}
    }
}





