/*

   Person.java

   This class keeps track of person objects in the Ganymede
   directLoader.
   
   Created: 25 March 1998
   Release: $Name:  $
   Version: $Revision: 1.2 $
   Last Mod Date: $Date: 1999/01/22 18:04:38 $
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
