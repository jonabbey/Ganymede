/*

   helpPanel.java

   A simple panel for showing html help stuff.
   
   Created: 4 March 1998
   Release: $Name:  $
   Version: $Revision: 1.4 $
   Last Mod Date: $Date: 1999/01/22 18:04:15 $
   Module By: Michael Mulvaney

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999  The University of Texas at Austin.

   Contact information

   Author Email: ganymede_author@arlut.utexas.edu
   Email mailing list: ganymede@arlut.utexas.edu

   US Mail:

   Computer Science Division
   Applied Research Laboratories
   The University of Texas at Austin
   PO Box 8029, Austin TX 78713-8029

   Telephone: (512) 835-3200

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

*/

package arlut.csd.ganymede.client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Stack;

/*------------------------------------------------------------------------------
                                                                           class
                                                                       helpPanel

------------------------------------------------------------------------------*/

public class helpPanel extends JDialog implements ActionListener{

  public String
    INDEX, SEARCH, START;

  String
    currentPage;

  JEditorPane
    document;

  gclient gc;

  JButton
    back,
    search,
    index,
    close;

  Stack
    history = new Stack();

  /* -- */

  public helpPanel(Frame parent)
  {
    super(parent);

    if (glogin.helpBase != null)
      {
	INDEX = glogin.helpBase + "help_index.html";
	SEARCH = glogin.helpBase + "help_search.html";
	START = glogin.helpBase + "help_start.html";
      }
    else
      {
	throw new RuntimeException("no help base set on server");
      }

    getContentPane().setLayout(new BorderLayout());

    try
      {
	document = new JEditorPane(START);
	document.setEditable(false);
      }
    catch (java.io.IOException x)
      {
	throw new RuntimeException("Could not load page: " + x);
      }

    currentPage = START;
    history.push(START);

    JScrollPane sp = new JScrollPane();
    sp.getViewport().add(document);
    getContentPane().add("Center", sp);

    back = new JButton("Back");
    back.addActionListener(this);
    search = new JButton("Search");
    search.addActionListener(this);
    index = new JButton("Index");
    index.addActionListener(this);
    close = new JButton("Close");
    close.addActionListener(this);

    JPanel wp = new JPanel(false);
    wp.add(back);
    wp.add(search);
    wp.add(index);

    JPanel closeP = new JPanel();
    closeP.add(close);

    JPanel bp = new JPanel(new BorderLayout());
    bp.add("East", closeP);
    bp.add("West", wp);
    
    getContentPane().add("North", bp);

    pack();
    setBounds(100,100,450,250);
    setVisible(true);
  }

  public void goBack()
  {
    if (history.empty())
      {
	System.out.println("History is empty");
      }
    else
      {
	String newPage = (String)history.pop();
	System.out.println("loading page: " + newPage);
	loadPage(newPage, false);
      }
  }

  public void loadPage(String page)
  {
    loadPage(page, true);
  }

  public void loadPage(String page, boolean keepInHistory)
  {
    if (currentPage == page)
      {
	System.out.println("Can't fool me, Eric!  You are trying to load the page you are on.");
      }
    else
      {
	try
	  {
	    document.setPage(page);

	    if (currentPage != null && keepInHistory)
	      {
		history.push(currentPage);
	      }

	    currentPage = page;
	  }
	catch (java.io.IOException e)
	  {
	    throw new RuntimeException("Could not change page to " + page + ": " + e);
	  }
      }
  }

  public void actionPerformed(ActionEvent e)
  {
    if (e.getSource() == close)
      {
	this.setVisible(false);
	this.dispose();
      }
    else if (e.getSource() == back)
      {
	goBack();
      }
    else if (e.getSource() == index)
      {
	loadPage(INDEX);
      }
    else if (e.getSource() == search)
      {
	loadPage(SEARCH);
      }
    else
      {
	System.out.println("action from unknown source");
      }
  }

}
