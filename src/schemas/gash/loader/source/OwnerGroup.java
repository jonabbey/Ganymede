/*

   OwnerGroup.java

   Class to load and store the data from a line in the
   GASH user_info file
   
   Created: 30 September 1997
   Version: $Revision: 1.2 $ %D%
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

  public OwnerGroup(Admin entry)
  {
    this(entry.lowuid, entry.highuid, entry.lowgid, entry.highgid, entry.mask);
  }

  public boolean equals(int lowuid, int highuid, int lowgid, int highgid, String prefix)
  {
    String temp;

    /* -- */

    if (prefix == null)
      {
	temp = "";
      }
    else
      {
	if (prefix.indexOf('*') != -1)
	  {
	    if (prefix.length() > 1)
	      {
		temp = prefix.substring(0, prefix.length() - 1);
	      }
	    else
	      {
		temp = "";
	      }
	  }
	else
	  {
	    temp = prefix;
	  }
      }

    return (lowuid == this.lowuid &&
	    highuid == this.highuid &&
	    lowgid == this.lowgid &&
	    highgid == this.highgid &&
	    temp.equals(this.prefix));
  }

  public boolean compatible(Admin entry)
  {
    return this.equals(entry.lowuid, entry.highuid, entry.lowgid, entry.highgid, entry.mask);
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

  public synchronized String password(String name)
  {
    adminRec a;

    for (int i = 0; i < admins.size(); i++)
      {
	a = (adminRec) admins.elementAt(i);

	if (a.adminName.equals(name))
	  {
	    return a.password;
	  }
      }

    throw new IllegalArgumentException(name + " is not an admin in this group!");
  }

  public void setInvid(Invid inv)
  {
    this.objInvid = inv;
  }

  public Invid getInvid()
  {
    return objInvid;
  }

  public String toString()
  {
    String result = objInvid == null ? "null " : (objInvid.toString() + " ");
    result += "[" + lowuid + "," + highuid + "], [" + lowgid + "," + highgid + "], [" + prefix + "], (";

    for (int i = 0; i < admins.size(); i++)
      {
	if (i > 0)
	  {
	    result += ", ";
	  }

	result += admins.elementAt(i);
      }

    result += ")";

    return result;
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

  public String toString()
  {
    return adminName;
  }

}
