/*

   SystemLoader.java

   Class to load and store the data from system lines in the
   GASH hosts_info file
   
   Created: 18 October 1997
   Release: $Name:  $
   Version: $Revision: 1.5 $
   Last Mod Date: $Date: 1999/03/01 22:27:28 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

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

package arlut.csd.ganymede.loader;

import java.io.*;
import java.util.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                    SystemLoader

------------------------------------------------------------------------------*/

public class SystemLoader {

  Hashtable systems = new Hashtable();

  private StreamTokenizer tokens;
  private BufferedReader inReader;

  /* -- */

  public SystemLoader(String filename) throws IOException
  {
    system systemTmp;
    interfaceObj interfaceTmp;
    int interfaceCount = 0;

    /* -- */

    try
      {
	inReader = new BufferedReader(new FileReader(filename));
      }
    catch (FileNotFoundException ex)
      {
	System.err.println("Couldn't find " + filename);
      }

    try
      {
	tokens = new StreamTokenizer(inReader);

	tokens.resetSyntax();
	tokens.wordChars(0, Integer.MAX_VALUE);
	tokens.eolIsSignificant(true);
	tokens.whitespaceChars(' ', ' ');
	tokens.whitespaceChars('\t', '\t');
	tokens.ordinaryChar(':');	// field separator
	tokens.ordinaryChar(',');	// field separator
	tokens.ordinaryChar('>');	// marker for type 2 lines
	tokens.ordinaryChar('\n');

	while (tokens.ttype != StreamTokenizer.TT_EOF)
	  {
	    tokens.nextToken();

	    if (tokens.ttype == StreamTokenizer.TT_EOF)
	      {
		continue;
	      }

	    if (tokens.ttype == '>')
	      {
		// read an interface

		System.out.print(">");
		interfaceCount++;

		try
		  {
		    interfaceTmp = new interfaceObj(tokens);
		    systemTmp = (system) systems.get(interfaceTmp.systemName);
		    systemTmp.interfaces.addElement(interfaceTmp);
		  }
		catch (IOException ex)
		  {
		    // oops

		    interfaceTmp = null;
		    System.err.println("IOException reading an interface");
		  }
	      }
	    else
	      {
		tokens.pushBack();

		System.out.print(".");

		try
		  {
		    systemTmp = new system(tokens);
		    //		systemTmp.display();
		  }
		catch (IOException ex)
		  {
		    // oops

		    systemTmp = null;
		
		    System.err.println("IOException reading a system");
		  }

		if (systemTmp != null)
		  {
		    systems.put(systemTmp.systemName, systemTmp);
		  }
	      }
	  }
      }
    finally
      {
	try
	  {
	    inReader.close();
	  }
	catch (IOException ex)
	  {
	    System.err.println("unknown IO exception caught: " + ex);
	  }
      }

    System.err.println("\nTotal interfaces created: " + interfaceCount);
  }

  /**
   *
   * The gash hosts_info file has the domain name attached to all system
   * and host names.  We don't want them in our database, so we provide
   * this handy function to strip any domain info off of a host name.
   *
   */

  public static String stripDomain(String name)
  {
    if (name.indexOf('.') == -1)
      {
	return name;
      }
    else
      {
	return name.substring(0, name.indexOf('.'));
      }
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                                          system

------------------------------------------------------------------------------*/

class system {

  String systemName;
  Vector aliases = new Vector();
  Vector admins = new Vector();
  String room;
  String type;			// system category
  String manu;			// manufacturer
  String model;			// model
  String os;			// o.s.
  String user;			// associated user (optional)
  Vector interfaces = new Vector();

  /* -- */

  system(StreamTokenizer tokens) throws IOException
  {
    int token;
    String tmp;

    /* -- */

    token = tokens.nextToken();

    systemName = getString(tokens);
    systemName = SystemLoader.stripDomain(systemName);

    token = tokens.nextToken();

    if (tokens.ttype != ',')
      {
	System.err.println("system parse error 1 for system " + systemName);
      }

    // skip past the next , which will separate our interface names from
    // our system aliases.  We'll get the interfaces linked in via the
    // back references in the interface lines

    token = tokens.nextToken();

    while (tokens.ttype != ',' && tokens.ttype != StreamTokenizer.TT_EOF &&
	   tokens.ttype != StreamTokenizer.TT_EOL)
      {
	token = tokens.nextToken();
      }

    if (tokens.ttype != ',')
      {
	System.err.println("system parse error 1.5 for system " + systemName);
      }

    token = tokens.nextToken();

    while (tokens.ttype != ':' && tokens.ttype != StreamTokenizer.TT_EOF &&
	   tokens.ttype != StreamTokenizer.TT_EOL)
      {
	tmp = getString(tokens);

	if (tmp == null)
	  {
	    System.err.println("system parse error 2 for system " + systemName);
	  }
	
	aliases.addElement(tmp);

	token = tokens.nextToken();
      }

    if (tokens.ttype != ':')
      {
	System.err.println("system parse error 3 for system " + systemName);
	return;
      }

    token = tokens.nextToken();

    while (tokens.ttype != ':' && tokens.ttype != StreamTokenizer.TT_EOF &&
	   tokens.ttype != StreamTokenizer.TT_EOL)
      {
	if (tokens.ttype != ',')
	  {
	    tmp = getString(tokens);

	    if (tmp == null)
	      {
		System.err.println("system parse error 4 for system " + systemName);
	      }

	    admins.addElement(tmp);
	  }

	token = tokens.nextToken();
      }

    if (tokens.ttype != ':')
      {
	System.err.println("system parse error 5 for system " + systemName);
	return;
      }

    token = tokens.nextToken();

    room = getString(tokens,':');

    if (tokens.ttype != ':')
      {
	System.err.println("system parse error 6 for system " + systemName);
	return;
      }
    
    token = tokens.nextToken();

    type = getString(tokens,':');

    if (tokens.ttype != ':')
      {
	System.err.println("system parse error 7 for system " + systemName);
	return;
      }
    
    token = tokens.nextToken();

    manu = getString(tokens,':');
    
    if (tokens.ttype != ':')
      {
	System.err.println("system parse error 8 for system " + systemName);
	return;
      }
    
    token = tokens.nextToken();

    model = getString(tokens,':');

    if (tokens.ttype != ':')
      {
	System.err.println("system parse error 9 for system " + systemName);
	return;
      }
    
    token = tokens.nextToken();

    os = getString(tokens,':');

    if (tokens.ttype != ':')
      {
	System.err.println("system parse error 10 for system " + systemName);
	return;
      }
    
    token = tokens.nextToken();

    user = getString(tokens);

    while (tokens.ttype != StreamTokenizer.TT_EOF &&
	   tokens.ttype != StreamTokenizer.TT_EOL)
      {
	token = tokens.nextToken();
      }
  }

  private String getString(StreamTokenizer tokens, int separator) throws IOException
  {
    int token;
    String result = "";
    boolean first = true;

    /* -- */


    while (tokens.ttype != separator)
      {
	if ((tokens.ttype == StreamTokenizer.TT_EOF) ||
	    (tokens.ttype == StreamTokenizer.TT_EOL))
	  {
	    return "";
	  }

	if (tokens.ttype == ',')
	  {
	    if (first)
	      {
		result = ", ";
		first = false;
	      }
	    else
	      {
		result += ", ";
	      }
	  }
	else
	  {
	    if (first)
	      {
		result = tokens.sval;
		first = false;
	      }
	    else
	      {
		result += " " + tokens.sval;
	      }
	  }

	token = tokens.nextToken();
      }

    if (tokens.ttype == separator)
      {
	return result;
      }

    return null;
  }

  private String getString(StreamTokenizer tokens) throws IOException
  {
    int token;
    String result;

    if ((tokens.ttype == StreamTokenizer.TT_EOF) ||
	(tokens.ttype == StreamTokenizer.TT_EOL))
      {
	return "";
      }

    if (tokens.ttype == StreamTokenizer.TT_WORD)
      {
	return tokens.sval;
      }

    return null;
  }

  public synchronized void display()
  {
    System.out.print(systemName + ":");

    for (int i = 0; i < aliases.size(); i++)
      {
	if (i > 0)
	  {
	    System.out.print(", ");
	  }

	System.out.print(aliases.elementAt(i));
      }
    
    System.out.print(":");

    for (int i = 0; i < admins.size(); i++)
      {
	if (i > 0)
	  {
	    System.out.print(", ");
	  }

	System.out.print(admins.elementAt(i));
      }

    System.out.println(":" + room + ":" + type + ":" + manu + ":" + model + ":" + os + ":" + user);

    interfaceObj iObj;

    for (int i = 0; i < interfaces.size(); i++)
      {
	System.out.print("\t");

	iObj = (interfaceObj) interfaces.elementAt(i);

	iObj.display();
      }
  }

}

/*------------------------------------------------------------------------------
                                                                           class
                                                                    interfaceObj

------------------------------------------------------------------------------*/

class interfaceObj {

  String interfaceName;
  String systemName;
  Vector aliases = new Vector();
  String IP;
  String Ether;

  /* -- */

  /**
   *
   * Constructor to load a line from the hosts_info file and
   * create an interface from it
   *
   */

  interfaceObj(StreamTokenizer tokens) throws IOException
  {
    int token;
    String tmp;

    /* -- */

    if (tokens.ttype != '>')
      {
	throw new IllegalArgumentException("bad line in interfaceObj constructor");
      }

    token = tokens.nextToken();

    if (tokens.ttype == ',')
      {
	interfaceName = "";
      }
    else
      {
	interfaceName = getString(tokens);
	interfaceName = SystemLoader.stripDomain(interfaceName);

	token = tokens.nextToken();
      }

    if (tokens.ttype != ',')
      {
	System.err.println("system parse error 1 for interface " + interfaceName);
	return;
      }

    token = tokens.nextToken();

    systemName = getString(tokens);
    systemName = SystemLoader.stripDomain(systemName);

    token = tokens.nextToken();

    if (tokens.ttype != ',')
      {
	System.err.println("system parse error 2 for interface " + interfaceName);
	return;
      }

    token = tokens.nextToken();

    while (tokens.ttype != ':' && tokens.ttype != StreamTokenizer.TT_EOF &&
	   tokens.ttype != StreamTokenizer.TT_EOL)
      {
	if (tokens.ttype != ',')
	  {
	    tmp = getString(tokens);

	    if (tmp == null)
	      {
		System.err.println("system parse error 3 for interface: " + interfaceName + ":" + systemName);
	      }

	    aliases.addElement(tmp);
	  }

	token = tokens.nextToken();
      }

    if (tokens.ttype != ':')
      {
	System.err.println("system parse error 4 for interface: " + interfaceName + ":" + systemName);
	return;
      }

    token = tokens.nextToken();

    IP = getString(tokens);

    token = tokens.nextToken();

    if (tokens.ttype != ':')
      {
	System.err.println("system parse error 5 for interface: " + interfaceName + ":" + systemName);
	return;
      }

    token = tokens.nextToken();

    Ether = getString(tokens);

    while (tokens.ttype != StreamTokenizer.TT_EOF &&
	   tokens.ttype != StreamTokenizer.TT_EOL)
      {
	token = tokens.nextToken();
      }
  }

  private String getString(StreamTokenizer tokens) throws IOException
  {
    int token;
    String result;
    
    if ((tokens.ttype == StreamTokenizer.TT_EOF) ||
	(tokens.ttype == StreamTokenizer.TT_EOL))
      {
	return "";
      }

    if (tokens.ttype == StreamTokenizer.TT_WORD)
      {
	return tokens.sval;
      }

    return null;
  }

  synchronized void display()
  {
    System.out.print(interfaceName + ":");

    for (int i = 0; i < aliases.size(); i++)
      {
	if (i > 0)
	  {
	    System.out.print(", ");
	  }

	System.out.print(aliases.elementAt(i));
      }
    
    System.out.println(":" + IP + ":" + Ether);
  }

}
