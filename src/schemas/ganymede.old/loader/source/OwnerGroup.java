/*

   OwnerGroup.java

   Class to load and store the data from a line in the
   GASH user_info file
   
   Created: 30 September 1997
   Version: $Revision: 1.1 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.loader;

import arlut.csd.ganymede.Invid;
import java.util.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                      OwnerGroup

------------------------------------------------------------------------------*/

public class OwnerGroup {

  Invid objInvid;
  int lowuid;
  int highuid;
  int lowgid;
  int highgid;
  String prefix;
  Vector admins;

  // instance constructor

  public OwnerGroup(int lowuid, int highuid, int lowgid, int highgid, String prefix)
  {
    this.lowuid = lowuid;
    this.highuid = highuid;
    this.lowgid = lowgid;
    this.highgid = highgid;
    this.prefix = prefix;
    this.objInvid = null;

    if (this.prefix.indexOf('*') != -1)
      {
	if (this.prefix.length() > 1)
	  {
	    this.prefix = this.prefix.substring(0, this.prefix.length() - 1);
	  }
	else
	  {
	    this.prefix = "";
	  }
      }

    admins = new Vector();
  }

  public boolean equals(int lowuid, int highuid, int lowgid, int highgid, String prefix)
  {
    return (lowuid == this.lowuid &&
	    highuid == this.highuid &&
	    lowgid == this.lowgid &&
	    highgid == this.highgid &&
	    prefix.equals(this.prefix));
  }

  public synchronized void addAdmin(String adminName, String password)
  {
    admins.addElement(new adminRec(adminName, password));
  }

  public boolean matchUID(int uid)
  {
    return (uid >= lowuid && uid <= highuid);
  }

  public boolean matchGID(int gid)
  {
    return (gid >= lowgid && gid <= highgid);
  }

  public boolean matchMask(String str)
  {
    return str.startsWith(prefix);
  }

  public synchronized boolean containsAdmin(String name)
  {
    adminRec a;

    for (int i = 0; i < admins.size(); i++)
      {
	a = (adminRec) admins.elementAt(i);

	if (a.adminName.equals(name))
	  {
	    return true;
	  }
      }

    return false;
  }

  public void setInvid(Invid inv)
  {
    this.objInvid = inv;
  }

  public Invid getInvid()
  {
    return objInvid;
  }

}

class adminRec {

  String adminName;
  String password;

  adminRec(String name, String pass)
  {
    this.adminName = name;
    this.password = pass;
  }

}
