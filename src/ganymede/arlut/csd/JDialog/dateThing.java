/*

   dateThing.java

   Resource class for use with StringDialog.java

   Created: 13 October 1998

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2013
   The University of Texas at Austin

   Ganymede is a registered trademark of The University of Texas at Austin

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
   along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/

package arlut.csd.JDialog;

import java.util.Date;

/*------------------------------------------------------------------------------
                                                                           class
                                                                       dateThing

------------------------------------------------------------------------------*/

/**
 * <p>Serializable object to describe a date field for passing to the
 * client as part of a {@link arlut.csd.JDialog.JDialogBuff JDialogBuff}
 * or {@link arlut.csd.JDialog.StringDialog StringDialog}.</p>
 */

public class dateThing implements java.io.Serializable {

  static final long serialVersionUID = 8718087676857868186L;

  private final String stringLabel;
  private final Date currentDate;
  private final Date maxDate;

  /* -- */

  public dateThing(String label, Date currentDate, Date maxDate)
  {
    this.stringLabel = label;

    if (currentDate != null)
      {
        this.currentDate = new Date(currentDate.getTime());
      }
    else
      {
        this.currentDate = null;
      }

    if (maxDate != null)
      {
        this.maxDate = new Date(maxDate.getTime());
      }
    else
      {
        this.maxDate = null;
      }
  }

  public String getLabel()
  {
    return stringLabel;
  }

  public Date getDate()
  {
    if (currentDate == null)
      {
        return null;
      }

    return new Date(currentDate.getTime());
  }

  public Date getMaxDate()
  {
    if (maxDate == null)
      {
        return null;
      }

    return new Date(maxDate.getTime());
  }
}
