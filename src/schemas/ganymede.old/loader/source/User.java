/*

   User.java

   Class to load and store the data from a line in the
   GASH user_info file
   
   Created: 22 August 1997
   Version: $Revision: 1.2 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.loader;

import java.io.*;

import arlut.csd.ganymede.Invid;

/*------------------------------------------------------------------------------
                                                                           class
                                                                            User

------------------------------------------------------------------------------*/

public class User {

  public static void initTokenizer(StreamTokenizer tokens)
  {
    tokens.resetSyntax();
    tokens.wordChars(0, Integer.MAX_VALUE);
    tokens.eolIsSignificant(true);
    tokens.ordinaryChar(':');
    tokens.ordinaryChar(',');
    tokens.ordinaryChar('\n');
  }

  // member fields

  String name;
  String password;
  int uid;
  int gid;
  String fullname;
  String room;
  String division;
  String officePhone;
  String homePhone;
  String directory;
  String shell;
  String socialsecurity;
  String type;

  Invid userInvid;		// for use in directLoader after we've been recorded

  // instance constructor

  public User()
  {
  }

  public boolean loadLine(StreamTokenizer tokens) throws IOException, EOFException
  {
    // read username

    tokens.nextToken();

    if (tokens.ttype == StreamTokenizer.TT_EOF)
      {
	return true;
      }
    else
      {
	tokens.pushBack();
      }

    name = getNextBit(tokens);

    //    System.out.println("name = '" + name + "'");

    password = getNextBit(tokens); 

    // System.out.println("password = '" + password + "'");

    String uidString = getNextBit(tokens);
    uid = new Integer(uidString).intValue();

    // System.out.println("uid = '" + uid + "'");

    String gidString = getNextBit(tokens);
    gid = new Integer(gidString).intValue();

    // System.out.println("gid = '" + gid + "'");

    fullname = getNextBit(tokens);

    // System.out.println("fullname = '" + fullname + "'");

    String tmp = getNextBit(tokens);
    int index = tmp.indexOf(' ');

    if (index == -1)
      {
	// no space in the string.. maybe we got
	// a comma where we shouldn't have?

	room = getNextBit(tokens);
      }
    else
      {
	room = tmp.substring(0, index); 
      }

    // System.out.println("room = '" + room + "'");
    division = tmp.substring(index+1); 

    // System.out.println("division = '" + division + "'");

    officePhone = getNextBit(tokens);

    // System.out.println("officePhone = '" + officePhone + "'");

    int token = tokens.nextToken();

    // should be a comma

    if (token != ',')
      {
	System.err.println("No comma after office phone..");
      }
    else
      {
	token = tokens.nextToken();
      }

    if (token == ':')
      {
	// no home phone

	homePhone = "";
      }
    else
      {
	// System.err.println("XXX");
	tokens.pushBack();
	homePhone = getNextBit(tokens);

	// System.out.println("homePhone = '" + homePhone + "'");
      }

    directory = getNextBit(tokens);

    // System.out.println("directory = '" + directory + "'");

    shell = getNextBit(tokens);

    // System.out.println("shell = '" + shell + "'");

    socialsecurity = getNextBit(tokens);

    // take out the -'s 

    socialsecurity = deleteChar(socialsecurity, '-');

    type = getNextBit(tokens);

    // get to the end of line

    // System.err.println("HEY! Token = " + token + ", ttype = " + tokens.ttype);

    while ((tokens.ttype != StreamTokenizer.TT_EOL) && (tokens.ttype != StreamTokenizer.TT_EOF))
      {
	// System.err.print(".");
	token = tokens.nextToken();
      }

    return (tokens.ttype == StreamTokenizer.TT_EOF);
  }

  public void display()
  {
    System.out.println("User: " + name + ", pass: " + password + ", uid: " + uid + ", gid: " + gid);
    System.out.println("\tfullname: " + fullname + ", room: " + room + ", division: " + division +
		       ", officePhone: " + officePhone + ", homePhone: " + homePhone);
    System.out.println("Directory: " + directory + ", shell: " + shell);
    System.out.println("SS#: " + socialsecurity);
  }
  
  private String deleteChar(String in, char c)
  {
    char ci[];
    StringBuffer buf = new StringBuffer();

    /* -- */

    ci = in.toCharArray();
    
    for (int i=0; i < ci.length; i++)
      {
	if (ci[i] != c)
	  {
	    buf.append(ci[i]);
	  }
      }

    return buf.toString();
  }

  private String getNextBit(StreamTokenizer tokens) throws IOException
  {
    int token;
    String result;

    token = tokens.nextToken();

    while (tokens.ttype == ':' || tokens.ttype == ',')
      {
	//	System.err.println("*");
	token = tokens.nextToken();
      }

    if (tokens.ttype == StreamTokenizer.TT_WORD)
      {
	//	System.err.println("returning native word");
	return tokens.sval;
      }

    if (tokens.ttype == StreamTokenizer.TT_NUMBER)
      {
	// System.err.println("returning converted word");
	result = Integer.toString(new Double(tokens.nval).intValue());

	while (tokens.ttype != ':' && tokens.ttype != ',' && tokens.ttype != StreamTokenizer.TT_EOF &&
	       tokens.ttype != StreamTokenizer.TT_EOL)
	  {
	    token = tokens.nextToken();
	    
	    if (tokens.ttype == StreamTokenizer.TT_WORD)
	      {
		result += tokens.sval;
	      }
	  }

	return result;
      }

    return null;
  }

}
