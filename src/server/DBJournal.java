/*

   DBJournal.java

   Class to handle the journal file for the DBStore.
   
   Created: 3 December 1996
   Version: $Revision: 1.1 $ %D%
   Module By: Jonathan Abbey
   Applied Research Laboratories, The University of Texas at Austin

*/

package csd.DBStore;

import java.io.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                                       DBJournal

------------------------------------------------------------------------------*/

public class DBJournal {

  String filename;

  FileInputStream inStream = null;
  FileOutputStream outStream = null;

  DataOutputStream out = null;
  DataInputStream in = null;

  /* -- */
  
  public DBJournal(String filename)
  {
    this.filename = filename;

    File file = new File(filename);

    if (!file.exists())
      {
	outStream = new FileOutputStream(filename);
	out = new DataOutputStream(outStream);
	out.writeUTF("GJOurnal");
      }
  }

  /**
   * The load() method reads in all transactions in the current DBStore Journal
   * and makes the appropriate changes to the DBStore Object Bases.  This
   * method should be called before the DBStore module is put into production
   * mode.
   *
   */

  public synchronized boolean load(DBStore store)
  {
  }

  /**
   *
   * The reset() method is used to copy the journal file to a safe location and
   * truncate it.  reset() should be called immediately after the DBStore is
   * dumped to disk and before the DumpLock is relinquished.
   *
   */

  public synchronized boolean reset()
  {
    if (outStream != null)
      {
	out.flush();
	outStream.close();
      }

    File file = new File(filename);

    file.renameTo(new File(filename + new Date()));

    outStream = new FileOutputStream(filename);
    out = new DataOutputStream(outStream);
    out.writeUTF("GJOurnal");
  }

  /**
   *
   * The writeTransaction() method actually performs the full work of
   * writing out a transaction to the DBStore Journal.
   * writeTransaction() should be called before the changes are
   * actually finalized in the DBStore Object Bases.  If
   * writeTransaction() is not able to successfully write the
   * transaction log to the Journal file, the transaction record on
   * disk will be marked as null and void (and possibly truncated out
   * of existence), so the Journal shouldn't ever become corrupted or
   * cause the DBStore to be made inconsistent regardless of whether
   * or not the server running the Ganymede / DBStore system crashes
   * during the transaction write.
   *
   */

  public synchronized boolean writeTransaction(DBEditSet editset)
  {
  }

}
