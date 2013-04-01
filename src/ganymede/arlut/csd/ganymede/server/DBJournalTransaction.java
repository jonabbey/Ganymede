/*
   GASH 2

   DBJournalTransaction.java

   The GANYMEDE object storage system.

   Created: 11 Februrary 2005

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2013
   The University of Texas at Austin

   Ganymede is a registered trademark of The University of Texas at Austin

   Contact information

   Web site: http://www.arlut.utexas.edu/gash2
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

final class DBJournalTransaction {

  /**
   * <p>The time stamp of the transaction.</p>
   */

  private final long time;

  /**
   * <p>This offset records the write position in the journal that
   * we'll want to truncate the journal to if we decide to undo the
   * transaction from the journal, rather than finalizing it.</p>
   */

  private final long fileOffset;

  /**
   * <p>The sequential transaction number.</p>
   */

  private final int transactionNumber;

  /**
   * <p>The admin, user, or task name which initiated the
   * transaction.</p>
   */

  private final String username;

  /* -- */

  public DBJournalTransaction(long time, long fileOffset, int transactionNumber, String username)
  {
    this.time = time;
    this.fileOffset = fileOffset;
    this.transactionNumber = transactionNumber;
    this.username = username;
  }

  public long getTime()
  {
    return this.time;
  }

  public long getOffset()
  {
    return this.fileOffset;
  }

  public int getTransactionNumber()
  {
    return this.transactionNumber;
  }

  public String getUsername()
  {
    return this.username;
  }
}
