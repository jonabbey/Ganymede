/*

   Contract.java

   This class is used to load information for ARL contracts from an
   activecontracts.txt file.
   
   Created: 15 March 1999
   Release: $Name:  $
   Version: $Revision: 1.1 $
   Last Mod Date: $Date: 1999/03/15 22:23:23 $
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

package arlut.csd.ganymede.custom;

import arlut.csd.ganymede.*;

import java.util.*;
import java.text.*;
import java.io.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                        Contract

------------------------------------------------------------------------------*/

/**
 *
 * This class is used to load information for ARL contracts from an
 * activecontracts.txt file, where lines look like
 *
 * 0051-1-43-1;5104301;1998/12/18;1999/12/17;TX EP DATABASE ENHANCE;
 *
 *
 * @author Jonathan Abbey jonabbey@arlut.utexas.edu
 * 
 */

public class Contract {

  static final boolean debug = true;
  static Calendar myCal = Calendar.getInstance();

  public String name;
  public String number;
  public Date startDate;
  public Date stopDate;
  public String description;
  public Invid invid = null;

  /* -- */

  public Contract(String line)
  {
    int index = 0, index2;

    /* -- */

    // everything up to the first semi-colon is the name

    index2 = line.indexOf(';', index);
    name = line.substring(index, index2);
    index = index2 + 1;

    // everything between first and second semi-colons is the contract number
    
    index2 = line.indexOf(';', index);
    number = line.substring(index, index2);
    index = index2 + 1;
    
    // everything between second and third semi-colons is the start date

    index2 = line.indexOf(';', index);
    startDate = dateParse(line.substring(index, index2));
    index = index2 + 1;
    
    // everything between third and fourth semi-colons is the stop date

    index2 = line.indexOf(';', index);
    stopDate = dateParse(line.substring(index, index2));
    index = index2 + 1;

    // everything between fourth and fifth semi-colons is the long description
    
    index2 = line.indexOf(';', index);
    description = line.substring(index, index2);
    
    if (debug)
      {
	System.err.println("Name: " + name + "\n" +
			   "Number: " + number + "\n" +
			   "Start: " + startDate + "\n" +
			   "Stop: " + stopDate + "\n" +
			   "Description: " + description + "\n");
      }
  }

  /**
   *
   * The ARL activecontracts.dat file uses dates like
   *
   * 1999/05/31
   *
   */

  private Date dateParse(String input) throws NumberFormatException
  {
    int year = Integer.parseInt(input.substring(0,4));
    int month = Integer.parseInt(input.substring(5,7));
    int day = Integer.parseInt(input.substring(8,9));

    if (year == 0)
      {
	return null;
      }

    myCal.clear();
    myCal.set(year, month-1, day);

    return myCal.getTime();
  }
}
