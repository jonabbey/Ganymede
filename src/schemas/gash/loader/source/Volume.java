/*

   Volume.java

   Class to load and store the data from a line in the
   GASH auto.vol file
   
   Created: 4 December 1997
   Version: $Revision: 1.1 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.loader;

import java.io.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                          Volume

------------------------------------------------------------------------------*/

public class Volume {

  static final boolean debug = true;

  // --

  String volumeName;
  String mountOptions;
  String hostName;
  String path;

  /* -- */

  public Volume(String line) throws IOException
  {
    int startMHindex;
    int startPath;
    int startHost;
    int i;

    char[] chars;

    /* -- */

    chars = line.toCharArray();

    i = 0;

    while (i < chars.length && !isWhiteSpace(chars[i]))
      {
	i++;
      }

    if (i == chars.length)
      {
	throw new RuntimeException("parse error 1: premature end of line");
      }

    volumeName = line.substring(0, i);

    while (i < chars.length && isWhiteSpace(chars[i]))
      {
	i++;
      }

    if (i == chars.length)
      {
	throw new RuntimeException("parse error 2: premature end of line");
      }

    startMHindex = i;		// start of either the mount options or the host name

    i = chars.length - 1;	// start at end

    while (i > startMHindex && chars[i] != ':')
      {
	i--;
      }

    if (i == startMHindex)
      {
	throw new RuntimeException("parse error 3: null path component");
      }

    startPath = i+1;		// start of path component, goes to end

    path = line.substring(startPath, chars.length);

    while (i >= startMHindex && !isWhiteSpace(chars[i]))
      {
	i--;
      }

    startHost = i+1;		// start of host component

    if (startHost == startPath)
      {
	throw new RuntimeException("parse error 4: null host component");
      }

    hostName = line.substring(startHost, startPath);

    if (startHost > startMHindex)
      {
	mountOptions = line.substring(startMHindex, startHost -1);
      }
    else
      {
	mountOptions = "";
      }
  }

  private final boolean isWhiteSpace(char c)
  {
    return (c == ' ') || (c == '\t') || (c == '\n');
  }

  public String toString()
  {
    return volumeName + "\t" + mountOptions + " " + hostName + ":" + path;
  }
}
