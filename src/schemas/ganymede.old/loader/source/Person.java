/*

   Person.java

   This class keeps track of person objects in the Ganymede
   directLoader.
   
   Created: 25 March 1998
   Version: $Revision: 1.1 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.loader;

import java.util.Vector;

/*------------------------------------------------------------------------------
                                                                           class
                                                                          Person

------------------------------------------------------------------------------*/

public class Person {

  String lastName = null;
  String firstName = null;
  String homePhone = null;
  String officePhone = null;
  String cellPhone = null;
  String faxNumber = null;
  String division = null;
  String room = null;
  String pagerNumber = null;
  String badgeNumber = null;
  String employeeType = null;

  Vector accounts = new Vector();

  /* -- */

  public Person(User user)
  {
    homePhone = user.homePhone;
    officePhone = user.officePhone;
    division = user.division;
    employeeType = user.type;
    room = user.room;

    // break user.fullname into firstName and lastName

    StringBuffer lastNameBuf = new StringBuffer();
    char[] cary = user.fullname.toCharArray();

    for (int i = cary.length-1; i >= 0 && cary[i] != ' '; i--)
      {
	lastNameBuf.insert(0, cary[i]);
      }

    lastName = lastNameBuf.toString();

    if (user.fullname.length() > lastName.length())
      {
	firstName = user.fullname.substring(0, user.fullname.length() - lastName.length() - 1);
      }
  }
}
