/*
   GASH 2

   DBJournalTransaction.java

   The GANYMEDE object storage system.

   Created: 11 Februrary 2005
   Version: $Revision$
   Last Mod Date: $Date$
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996-2005
   The University of Texas at Austin

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
   Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
   02111-1307, USA

*/

package arlut.csd.ganymede.server;

/*------------------------------------------------------------------------------
                                                                           class
                                                            DBJournalTransaction

------------------------------------------------------------------------------*/

/**
 * <p>This class is a simple data carrier recording the time and
 * transaction number for a transaction in the process of being
 * persisted to Ganymede's replay journal.</p>
 */

class DBJournalTransaction {

  private long time;
  private int transactionNumber;

  /* -- */

  public DBJournalTransaction(long time, int transactionNumber)
  {
    this.time = time;
    this.transactionNumber = transactionNumber;
  }

  public long getTime()
  {
    return this.time;
  }

  public int getTransactionNumber()
  {
    return this.transactionNumber;
  }
}
