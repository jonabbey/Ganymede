/*

   dateThing.java

   Resource class for use with StringDialog.java
   
   Created: 13 October 1998
   Release: $Name:  $
   Version: $Revision: 1.3 $
   Last Mod Date: $Date: 1999/01/22 18:04:02 $
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

package arlut.csd.JDialog;

import java.util.Date;

/*------------------------------------------------------------------------------
                                                                           class
                                                                       dateThing

------------------------------------------------------------------------------*/

public class dateThing implements java.io.Serializable {

  String stringLabel;
  Date currentDate;
  Date maxDate;

  /* -- */

  public dateThing(String label, Date currentDate, Date maxDate)
  {
    this.stringLabel = label;
    this.currentDate = currentDate;
    this.maxDate = maxDate;
  }

  public String getLabel()
  {
    return stringLabel;
  }

  public Date getDate()
  {
    return currentDate;
  }

  public Date getMaxDate()
  {
    return maxDate;
  }

}
