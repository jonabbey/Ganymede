/*

   SystemLoader.java

   Class to load and store the data from system lines in the
   GASH hosts_info file
   
   Created: 18 October 1997
   Version: $Revision: 1.1 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

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
  private FileInputStream inStream;

  /* -- */

  public SystemLoader(String filename) throws IOException
  {
    system systemTmp;
    interfaceObj interfaceTmp;

    /* -- */

    try
      {
	inStream = new FileInputStream(filename);
      }
    catch (FileNotFoundException ex)
      {
	System.err.println("Couldn't find " + filename);
      }

    tokens = new StreamTokenizer(inStream);

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
	interfaceName = getString(tokens); // this will be the empty string
	token = tokens.nextToken();
      }

    if (tokens.ttype != ',')
      {
	System.err.println("system parse error 1 for interface " + interfaceName);
	return;
      }

    token = tokens.nextToken();

    systemName = getString(tokens);

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
