/*

   AdminEntry.java

   A serializable object, holding the contents of a row in an
   admin console's table.
   
   Created: 3 February 1997
   Version: $Revision: 1.2 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede;

public class AdminEntry implements java.io.Serializable {

  static final long serialVersionUID = -2534608083606361951L;

  // ---

  String
    username,
    hostname,
    status,
    connecttime,
    event;

  public AdminEntry(String username, String hostname, String status,
		    String connecttime, String event)
  {
    this.username = username;
    this.hostname = hostname;
    this.status = status;
    this.connecttime = connecttime;
    this.event = event;
  }
}
