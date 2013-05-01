/*

   SyncRunner.java

   This class is used in the Ganymede server to handle sync channel
   synchronization.  It is responsible for acting as a Runnable in the
   Ganymede scheduler (when run, it executes the external service program for
   a given sync channel queue) and for tracking the data associated with the
   sync channel.

   Created: 2 February 2005

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

package arlut.csd.ganymede.server;

import arlut.csd.ganymede.common.FieldBook;
import arlut.csd.ganymede.common.Invid;
import arlut.csd.ganymede.common.ObjectStatus;
import arlut.csd.ganymede.common.scheduleHandle;
import arlut.csd.ganymede.common.SchemaConstants;
import arlut.csd.ganymede.common.NotLoggedInException;
import arlut.csd.ganymede.common.ReturnVal;
import arlut.csd.ganymede.common.SyncPrefEnum;
import arlut.csd.ganymede.rmi.FileTransmitter;

import arlut.csd.Util.booleanSemaphore;
import arlut.csd.Util.FileOps;
import arlut.csd.Util.TranslationService;

import java.io.File;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.jclark.xml.output.UTF8XMLWriter;

/*------------------------------------------------------------------------------
                                                                           class
                                                                      SyncRunner

------------------------------------------------------------------------------*/

/**
 * <p>This class is used in the Ganymede server to handle Sync
 * Channel-style incremental and full-state synchronization.  Unlike
 * the full-state {@link
 * arlut.csd.ganymede.server.GanymedeBuilderTask} system, SyncRunner
 * is not designed to be customized through subclassing.  Instead, all
 * SyncRunner objects generate a common XML file format for writing
 * out synchronization data.  SyncRunner objects are registered in the
 * server for execution automatically when a <code>Sync Channel</code>
 * object (using the {@link
 * arlut.csd.ganymede.server.syncChannelCustom} DBEditObject subclass
 * for management) is created in the Ganymede data store.  This
 * <code>Sync Channel</code> object provides all of the configuration
 * controls which determine where the XML synchronization files are
 * written, what external program should be used to process the files,
 * and what data needs to be written out for synchronization.</p>
 *
 * <p>Every time a transaction is committed, the Ganymede server
 * compares all objects involved in the transaction against every
 * registered <code>Sync Channel</code> object.  If any of the objects
 * or fields created, deleted, or edited during the transaction
 * matches against a Sync Channel's so-called <code>Field
 * Options</code> matrix of objects and fields to sync, that Sync
 * Channel will be tasked to write an XML file to the directory
 * configured in the Sync Channel's <code>Queue Directory</code>
 * field.</p>
 *
 * <p>For incremental Sync Channels, each XML file that is created in
 * a Sync Channel output directory is given a unique filename, based
 * on the number of transactions committed by the Ganymede server
 * since the server's database was first initialized.  The first
 * transaction committed is transaction 0, the second is 1, and so
 * forth.  These transaction numbers are global in the Ganymede
 * server.  Every time any transaction is committed, the transaction
 * number is incremented, whether or not any Sync Channels match
 * against the transaction.</p>
 *
 * <p>These incremental XML files are written out synchronously with
 * the transaction commit.  What this means is that the transaction is
 * not counted as successfully committed until all SyncRunners that
 * match against the transaction successfully write and flush their
 * XML files to the proper output directory.  If a problem prevents
 * any of the SyncRunners from successfully writing its XML files, the
 * transaction will be aborted as if it never happened.  All of this
 * is done with proper ACID transactional guarantees.  The SyncRunner
 * implementation is designed so that the Ganymede server can be
 * killed at any time without leaving a transaction partially
 * committed between the Sync Channels and the Ganymede journal file.
 * Either a transaction is successfully recorded to all relevant Sync
 * Channels and the journal, or it will not be recorded to any of
 * them.</p>
 *
 * <p>Whenever any transaction is successfully committed, each
 * incremental Sync Channel Runner is scheduled for execution by the
 * {@link arlut.csd.ganymede.server.GanymedeScheduler}.  When the Sync
 * Runner's {@link arlut.csd.ganymede.server.SyncRunner#run()} method
 * is called, it runs its external <code>Service Program</code>,
 * passing the most recently committed transaction number as a single
 * command line argument.  The <code>Service Program</code> is meant
 * to examine the <code>Queue Directory</code> specified in the
 * <code>Sync Channel</code> object, and to process any XML files with
 * numbers less than or equal to its command line argument.  As is the
 * case with GanymedeBuilderTask, the GanymedeScheduler will not
 * relaunch a SyncRunner until the previous execution of the
 * SyncRunner completes.  Unlike GanymedeBuilderTask, the Ganymede
 * server does not prevent new files from being written out while the
 * <code>Service Program</code> is being executed.  It is the
 * responsibility of the <code>Service Program</code> to ignore any
 * XML files that it finds with transaction numbers higher than that
 * passed to it on the command line.</p>
 *
 * <p>In this way, the incremental Sync Channel system allows
 * transactions to be committed at a rapid rate, while allowing the
 * <code>Service Program</code> to take as little or as much time as
 * is required to process transactions.  The principle of back-to-back
 * builds is very much part of this Ganymede synchronization mechanism
 * as well.</p>
 *
 * <p>Because the incremental Sync Channel transaction files are
 * generated while the transaction is being committed, the Sync
 * Channel writing code has complete access to the before and after
 * state of all objects in the transaction.  This before and after
 * information is incorporated into each XML file, so that the
 * external <code>Service Program</code> has access to the change
 * context in order to apply the appropriate changes against the
 * directory service target.</p>
 *
 * <p>Full-state automatic sync channels, by contrast, work more like
 * the GanymedeBuilderTask system.  When a transaction is committed
 * that matches a full-state sync channel's Sync Channel object
 * definition, the sync channel is scheduled for later execution in
 * the Ganymede scheduler.  When the full-state sync channel is
 * serviced by the scheduler, the full state of the system (as
 * filtered by the requirements of the Sync Channel object at the time
 * the scheduler runs) the XML file is generated and the service
 * program is run immediately therafter.</p>
 *
 * <p>Incremental sync channels can be more efficient, as only changed
 * data need be written to the Sync Channel, but not all types of data
 * services can be supported with incremental synchronization.</p>
 *
 * <p>Because incremental synchronization is based on applying changes
 * to a directory service target, it works best on directory services
 * that can be updated incrementally, like LDAP.  It is not designed
 * for systems that require full-state replacement in order to make
 * changes at all, such as NIS or classical DNS.  Even for systems
 * that can accept incremental changes, however, the use of discrete
 * deltas for Ganymede synchronization can be problematic.  If an XML
 * transaction file cannot successfully be applied to a directory
 * service target, the Ganymede server has no way of knowing this,
 * because the Service Program is not run until sometime after the
 * transaction has been successfully committed.</p>
 *
 * <p>In order to cope with this, the Sync Channel system has
 * provisions for doing manual 'full-state' XML dumps as well. You
 * can do this manually by using the Ganymede xmlclient.  The command
 * you want looks like</p>
 *
 * <pre>
 *   xmlclient -dumpdata sync=Users > full_users_sync.xml
 * </pre>
 *
 * <p>The effect of this command is to dump all data in the server
 * that matches the filters for the 'Users' Sync Channel to the
 * full_users_sync.xml file.  Note that in order for this to work, you
 * should make sure that your Sync Channel's name does not include any
 * whitespace.  Java's command line parsing logic is fundamentally
 * broken, and makes it impossible to portably process command line
 * parameters with internal whitespace.  Note as well that, as with
 * all xmlclient dump operations, the database is locked against
 * transaction commits while this is running, so this operation can
 * only be done by a supergash-level account.</p>
 *
 * <p>You would use such a full state sync file for those cases where
 * the incremental synchronization system experiences a loss of
 * synchronization between the Ganymede server and the target
 * directory service.  The idea is that your external Service Program
 * should in some way be able to recognize that it is being given a
 * full state dump, and undertake a more lengthy and computationally
 * expensive process of reconciliation to bring the directory service
 * target into compliance with the data in the Ganymede server.</p>
 *
 * <p>See the <a href="../../../../../synchronization/index.html" target="_top">Ganymede
 * synchronization guide</a> for more details on all of this.</p>
 */

public final class SyncRunner implements Runnable {

  /**
   * <p>TranslationService object for handling string localization in
   * the Ganymede server.</p>
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.server.SyncRunner");

  static final boolean debug = false;

  /**
   * XML version major id
   */

  static final byte major_xml_sync_version = 1;

  /**
   * XML version minor id
   */

  static final byte minor_xml_sync_version = 0;

  // ---

  /**
   * <p>Enum of possible modalities for a SyncRunner</p>
   */

  public enum SyncType
  {
    /**
     * <p>The SyncRunner is not triggered automatically, but may be
     * manually fired from the admin console, or used for filtering
     * the output of an xmlclient dump.</p>
     */

    MANUAL(0),

    /**
     * <p>The SyncRunner is triggered automatically, and writes out an
     * XML file with before-and-after context describing a transaction
     * that is committed in the Ganymede data store.</p>
     */

    INCREMENTAL(1),


    /**
     * <p>The SyncRunner is triggered automatically on Ganymede
     * commits, and writes out a filtered set of all data, regardless
     * of whether or not the data objects were modified recently.</p>
     */

    FULLSTATE(2);

    /* -- */

    public static SyncType get(int objectVal)
    {
      switch (objectVal)
        {
        case 0:
          return MANUAL;

        case 1:
          return INCREMENTAL;

        case 2:
          return FULLSTATE;

        default:
          throw new IllegalArgumentException("Unrecognized integral value");
        }
    }

    private final int numVal;

    SyncType(int val)
    {
      this.numVal = val;
    }

    public int valOf()
    {
      return numVal;
    }

    public Integer Val()
    {
      return Integer.valueOf(numVal);
    }
  }

  private String name;
  private String directory;
  private String fullStateFile;
  private String serviceProgram;
  private int transactionNumber;
  private boolean includePlaintextPasswords;
  private Map<String, SyncPrefEnum> matrix;

  /**
   * <p>Controls what type of Sync Channel we're handling.</p>
   */

  private SyncType mode;

  /**
   * <p>This variable is true if we've seen a transaction that
   * requires us to issue a build on the next run().</p>
   *
   * <p>We set this to true on startup so that we will initiate a
   * "catch-up" build on server startup, just in case.</p>
   */

  private booleanSemaphore needBuild = new booleanSemaphore(true);

  /**
   * <p>This semaphore controls whether or not this SyncRunner will
   * attempt to write out transactions.  This semaphore is disabled
   * when this Sync Channel is disabled in the admin console.</p>
   */

  private booleanSemaphore active = new booleanSemaphore(true);

  /**
   * <p>If the Sync Channel object in the Ganymede server had a class
   * name defined, and we can load it successfully, master will
   * contain a reference to the {@link
   * arlut.csd.ganymede.server.SyncMaster} used to provide
   * augmentation to the delta sync records produced by this
   * SyncRunner.</p>
   */

  private SyncMaster master;

  /**
   * <p>A reference to the scheduleHandle for this sync channel.  We
   * use this handle to set the status for propagation to the admin
   * console(s).</p>
   */

  private scheduleHandle handle;

  /**
   * <p>If we're an incremental sync channel, we'll track the size of
   * what we add to the queue during runs of the service program, so
   * we can determine whether the service program cleared the queue
   * (minus the new entries) during its run.</p>
   */

  private int queueGrowthSize = 0;

  /**
   * <p>The lock object we're using for the queueGrowthSize management.</p>
   */

  private Object queueGrowthMonitor = new Object();

  /**
   * <p>A filter we use to find transaction files in the queue directory.</p>
   */

  private QueueDirFilter dirFilter = new QueueDirFilter();

  /* -- */

  public SyncRunner(DBObject syncChannel)
  {
    // on startup, we'll take the last known good transaction number
    // for our baseline
    this.transactionNumber = Ganymede.db.getTransactionNumber();

    updateInfo(syncChannel);
  }

  /**
   * <p>Configure this SyncRunner from the corresponding Ganymede Sync
   * Channel DBObject.</p>
   */

  private synchronized void updateInfo(DBObject syncChannel)
  {
    if (syncChannel.getTypeID() != SchemaConstants.SyncChannelBase)
      {
        // "Error, passed the wrong kind of DBObject."
        throw new IllegalArgumentException(ts.l("updateInfo.typeError"));
      }

    name = (String) syncChannel.getFieldValueLocal(SchemaConstants.SyncChannelName);
    directory = (String) syncChannel.getFieldValueLocal(SchemaConstants.SyncChannelDirectory);
    fullStateFile = (String) syncChannel.getFieldValueLocal(SchemaConstants.SyncChannelFullStateFile);
    serviceProgram = (String) syncChannel.getFieldValueLocal(SchemaConstants.SyncChannelServicer);
    includePlaintextPasswords = syncChannel.isSet(SchemaConstants.SyncChannelPlaintextOK);

    String className = (String) syncChannel.getFieldValueLocal(SchemaConstants.SyncChannelClassName);

    if (className != null)
      {
        Class classdef;

        try
          {
            classdef = Class.forName(className);

            Constructor c;
            Class[] cParams = new Class[0];
            Object[] params = new Object[0];

            c = classdef.getDeclaredConstructor(cParams); // no param constructor
            master = (SyncMaster) c.newInstance(params);
          }
        catch (ClassNotFoundException ex)
          {
            // "Couldn''t load SyncMaster class {0} for Sync Channel {1}"
            Ganymede.debug(ts.l("updateInfo.nosuchclass", className, name));
          }
        catch (NoSuchMethodException ex)
          {
            // "Couldn''t find no-param constructor for SyncMaster class {0} for Sync Channel {1}"
            Ganymede.debug(ts.l("updateInfo.missingconstructor", className, name));
          }
        catch (Exception ex)
          {
            // "Exception calling no-param constructor for SyncMaster class {0} for Sync Channel {1}: {2}"
            Ganymede.debug(ts.l("updateInfo.constructorerror", className, name, ex.toString()));
          }
      }

    if (master == null)
      {
        master = new NoopSyncMaster();
      }

    try
      {
        this.mode = SyncType.get((Integer) syncChannel.getFieldValueLocal(SchemaConstants.SyncChannelTypeNum));
      }
    catch (NullPointerException ex)
      {
        this.mode = SyncType.INCREMENTAL; // the default old behavior
      }

    FieldOptionDBField f = (FieldOptionDBField) syncChannel.getField(SchemaConstants.SyncChannelFields);

    if (f == null)
      {
        this.matrix = new HashMap<String, SyncPrefEnum>();
      }
    else
      {
        this.matrix = new HashMap<String, SyncPrefEnum>(f.matrix);
      }
  }

  /**
   * <p>Used to set the number of the last transaction known to have been
   * safely persisted to disk (journal and sync channels) at the time this
   * method is called.</p>
   */

  public void setTransactionNumber(int trans)
  {
    if (trans >= this.transactionNumber)
      {
        this.transactionNumber = trans;
      }
    else
      {
        // "Error, can''t set the persisted transaction number to a lower number than previously seen."
        throw new IllegalArgumentException(ts.l("setTransactionNumber.badNumber"));
      }
  }

  /**
   * <p>Returns the number of the last transaction known to have been
   * safely persisted to disk (journal and sync channels) at the time this
   * method is called.</p>
   */

  public int getTransactionNumber()
  {
    return this.transactionNumber;
  }

  /**
   * <p>This method is used to enable or disable this sync channel's
   * writing of transactions to disk.</p>
   */

  public void setActive(boolean state)
  {
    this.active.set(state);
  }

  /**
   * <p>Returns the name of the Sync Channel that this SyncRunner was
   * configured from.</p>
   */

  public String getName()
  {
    return name;
  }

  /**
   * <p>Returns true if this SyncRunner is configured as a full state
   * sync channel.</p>
   */

  public boolean isFullState()
  {
    return this.mode == SyncType.FULLSTATE;
  }

  /**
   * <p>Returns true if this SyncRunner is configured as an
   * incremental sync channel.</p>
   */

  public boolean isIncremental()
  {
    return this.mode == SyncType.INCREMENTAL;
  }

  /**
   * <p>Returns the queue directory we'll write to if we're an
   * incremental build channel.</p>
   */

  public String getDirectory()
  {
    return directory;
  }

  /**
   * <p>Returns the file we'll write to for a full state build.</p>
   */

  public String getFullStateFile()
  {
    return this.fullStateFile;
  }

  /**
   * <p>Returns the name of the external service program for this Sync
   * Channel.</p>
   */

  public String getServiceProgram()
  {
    return serviceProgram;
  }

  /**
   * <p>Sets a reference to the scheduleHandle that this sync channel
   * will use to communicate its status to the admin consoles.</p>
   */

  public void setScheduleHandle(scheduleHandle handle)
  {
    this.handle = handle;

    if (this.mode == SyncType.INCREMENTAL)
      {
        updateAdminConsole(false);
      }
  }

  /**
   * <p>This method writes out the differential transaction record to
   * the delta Sync Channel defined by this SyncRunner object.  The
   * transaction record will only include those objects and fields
   * that are specified in the Sync Channel database object that this
   * SyncRunner was initialized with, or which are included by a
   * SyncMaster class referenced by name in the SyncChannel DBObject
   * used to create this SyncRunners.</p>
   *
   * @param transRecord A transaction description record describing
   * the transaction we are writing
   * @param objectList An array of DBEditObjects that the transaction
   * has checked out at commit time
   * @param transaction The DBEditSet that is being committed.
   */

  public void writeIncrementalSync(DBJournalTransaction transRecord,
                                   DBEditObject[] objectList,
                                   DBEditSet transaction) throws IOException
  {
    if (!this.active.isSet())
      {
        return;
      }

    XMLDumpContext xmlOut = null;

    /* -- */

    if (debug)
      {
        System.err.println("SyncRunner.writeIncrementalSync: entering queueGrowthMonitor section");
      }

    try
      {
        synchronized (queueGrowthMonitor)
          {
            if (debug)
              {
                System.err.println("SyncRunner.writeIncrementalSync: inside queueGrowthMonitor section");

                for (DBEditObject eObj: objectList)
                  {
                    Ganymede.debug("SyncRunner.writeIncrementalSync(): we'll be processing object " + eObj);
                  }
              }

            FieldBook book = new FieldBook();

            initializeFieldBook(objectList, book);

            int context_count = 0;

            // we want to group the objects we write out by invid type

            Set<Short> typeSet = new HashSet<Short>();

            for (Invid invid: book.objects())
              {
                typeSet.add(invid.getType());
              }

            // first write out the objects that we actually changed this
            // transaction

            for (Short type: typeSet)
              {
                if (debug)
                  {
                    Ganymede.debug("SyncRunner.writeIncrementalSync(): examining type " + type + " from the typeSet.");
                  }

                for (Invid invid: book.objects())
                  {
                    if (debug)
                      {
                        Ganymede.debug("SyncRunner.writeIncrementalSync(): examining invid " + invid + " from the book.objects() set.");
                      }

                    if (!type.equals(invid.getType()))
                      {
                        continue;
                      }

                    if (!transaction.isEditingObject(invid) || !mayInclude(invid.getType()))
                      {
                        if (debug)
                          {
                            if (!transaction.isEditingObject(invid))
                              {
                                Ganymede.debug("Hey, SyncRunner says the transaction is not editing object " + invid);
                              }
                            else if (!mayInclude(invid.getType()))
                              {
                                Ganymede.debug("Hey, SyncRunner says we may not include objects of type " + invid);
                              }
                          }

                        context_count++;

                        continue;
                      }

                    DBEditObject syncObject = transaction.findObject(invid);

                    if (xmlOut == null)
                      {
                        xmlOut = createXMLSync(transRecord);
                        xmlOut.setDeltaFieldBook(book);
                        xmlOut.setDBSession(transaction.getDBSession());
                      }

                    switch (syncObject.getStatus())
                      {
                      case ObjectStatus.CREATING:
                        xmlOut.startElementIndent("object_delta");

                        xmlOut.indentOut();

                        xmlOut.startElementIndent("before");
                        xmlOut.endElement("before");

                        xmlOut.startElementIndent("after");
                        xmlOut.indentOut();
                        syncObject.emitXML(xmlOut);
                        xmlOut.indentIn();
                        xmlOut.endElementIndent("after");

                        xmlOut.indentIn();
                        xmlOut.endElementIndent("object_delta");

                        break;

                      case ObjectStatus.DELETING:
                        xmlOut.startElementIndent("object_delta");

                        xmlOut.indentOut();

                        xmlOut.startElementIndent("before");
                        xmlOut.indentOut();
                        syncObject.getOriginal().emitXML(xmlOut);
                        xmlOut.indentIn();
                        xmlOut.endElementIndent("before");

                        xmlOut.startElementIndent("after");
                        xmlOut.endElement("after");

                        xmlOut.indentIn();
                        xmlOut.endElementIndent("object_delta");
                        break;

                      case ObjectStatus.EDITING:
                        syncObject.emitXMLDelta(xmlOut);
                        break;
                      }
                  }
              }

            // then write out any objects that the SyncMaster has thrown in
            // for context

            if (context_count > 0)
              {
                if (xmlOut == null)
                  {
                    xmlOut = createXMLSync(transRecord);
                    xmlOut.setDeltaFieldBook(book);
                    xmlOut.setDBSession(transaction.getDBSession());
                  }

                xmlOut.startElementIndent("context_objects");
                xmlOut.indentOut();

                for (Short type: typeSet)
                  {
                    for (Invid invid: book.objects())
                      {
                        if (!type.equals(invid.getType()))
                          {
                            continue;   // skip
                          }

                        if (transaction.isEditingObject(invid) && mayInclude(invid.getType()))
                          {
                            continue;   // skip, we handled this in the delta section
                          }

                        DBObject refObject = transaction.getDBSession().viewDBObject(invid);

                        refObject.emitXML(xmlOut);
                      }
                  }

                xmlOut.indentIn();
                xmlOut.endElementIndent("context_objects");
              }

            if (xmlOut != null)
              {
                xmlOut.indentIn();
                xmlOut.endElementIndent("transaction");
                xmlOut.skipLine();
                xmlOut.close();         // close() automatically flushes before closing
                xmlOut = null;

                needBuild.set(true);
              }

            setTransactionNumber(transRecord.getTransactionNumber());

            queueGrowthSize = queueGrowthSize + 1; // count the one that we just wrote out
          }
      }
    finally
      {
        if (xmlOut != null)
          {
            xmlOut.close();
          }
      }

    updateAdminConsole(false);
  }

  /**
   * <p>This method checks this Full State SyncRunner against the
   * objects involved in the provided transaction.  If this
   * SyncRunner's Sync Channel definition matches against the
   * transaction, a flag will be set causing a Full State build to be
   * executed upon the next run of this Sync Runner in the Ganymede
   * scheduler.</p>
   *
   * @param transRecord A transaction description record describing
   * the transaction we are checking
   * @param objectList An array of DBEditObjects that the transaction
   * has checked out at commit time
   * @param transaction The DBEditSet that is being committed.
   */

  public void checkBuildNeeded(DBJournalTransaction transRecord, DBEditObject[] objectList,
                               DBEditSet transaction) throws IOException
  {
    for (DBEditObject syncObject: objectList)
      {
        if (shouldInclude(syncObject))
          {
            this.needBuild.set(true);
            return;
          }
      }
  }

  /**
   * <p>If we go to commit a transaction and we find that we can't
   * write a sync to its sync channel for some reason, we'll need to
   * go back and erase the sync files that we did write out.  This method
   * is responsible for wielding the axe.</p>
   *
   * @param transRecord A transaction description record describing
   * the transaction we are clearing from this sync channel
   */

  public void unSync(DBJournalTransaction transRecord) throws IOException
  {
    File syncFile = new File(this.getDirectory() + File.separator + String.valueOf(transRecord.getTransactionNumber()));

    if (syncFile.exists())
      {
        // "SyncRunner {0} deleting left over transaction fragment {1}"
        Ganymede.debug(ts.l("unSync.deleting", this.getName(), syncFile.getPath()));

        if (!syncFile.delete())
          {
            throw new IOException("Could not delete " + syncFile.getPath());
          }
      }

    this.transactionNumber = transRecord.getTransactionNumber() - 1;
  }

  /**
   * <p>This private helper method creates the {@link
   * arlut.csd.ganymede.server.XMLDumpContext} that writeIncrementalSync() will
   * write to.</p>
   *
   * @param transRecord A transaction description record describing
   * the transaction we are writing
   */

  private XMLDumpContext createXMLSync(DBJournalTransaction transRecord) throws IOException
  {
    FileOutputStream outStream = null;
    BufferedOutputStream bufStream = null;

    outStream = new FileOutputStream(this.getDirectory() + File.separator + String.valueOf(transRecord.getTransactionNumber()));
    bufStream = new BufferedOutputStream(outStream);

    XMLDumpContext xmlOut = new XMLDumpContext(new UTF8XMLWriter(bufStream, UTF8XMLWriter.MINIMIZE_EMPTY_ELEMENTS),
                                               includePlaintextPasswords, // whether we include plaintext passwords when we have alternate hashes
                                               false, // don't include creator/modifier data
                                               this,
                                               true); // include oid's in the object to act as remote foreign keys

    xmlOut.setDumpPasswords(true);

    xmlOut.startElement("transaction");
    xmlOut.attribute("major_version", Byte.toString(major_xml_sync_version));
    xmlOut.attribute("minor_version", Byte.toString(major_xml_sync_version));
    xmlOut.attribute("channel", getName());

    if (transRecord.getUsername() != null)
      {
        xmlOut.attribute("user", transRecord.getUsername());
      }

    xmlOut.attribute("number", String.valueOf(transRecord.getTransactionNumber()));
    xmlOut.attribute("time", String.valueOf(transRecord.getTime()));
    xmlOut.indentOut();

    return xmlOut;
  }

  /**
   * <p>This method creates an initial internal FieldBook for this
   * SyncRunner, based on the parameters defined in the incremental
   * Sync Channel DBObject that this SyncRunner is configured
   * from.</p>
   */

  private void initializeFieldBook(DBEditObject[] objectList, FieldBook book)
  {
    for (DBEditObject syncObject: objectList)
      {
        if (syncObject.getStatus() == ObjectStatus.DROPPING)
          {
            // Created and destroyed in this transaction.. no-op.

            continue;
          }

        if (debug)
          {
            Ganymede.debug("SyncRunner.initializeFieldBook(): Contemplating " + syncObject);
          }

        if (mayInclude(syncObject))
          {
            if (debug)
              {
                Ganymede.debug("SyncRunner.initializeFieldBook(): Good news we can consider including " + syncObject);
              }

            DBObject origObj = syncObject.getOriginal();

            // We know that checked-out DBEditObjects have a copy of
            // all defined fields, so we don't need to consult the
            // (possibly null) origObj reference looking for fields
            // that were deleted from syncObject.

            for (DBField memberField: syncObject.getFieldVect())
              {
                DBField origField;

                if (origObj == null)
                  {
                    origField = null;
                  }
                else
                  {
                    origField = (DBField) origObj.getField(memberField.getID());
                  }

                // created

                if (origField == null && memberField.isDefined())
                  {
                    if (debug)
                      {
                        Ganymede.debug("SyncRunner.initializeFieldBook(): Created field " +
                                       memberField + " in " + syncObject);
                      }

                    SyncPrefEnum fieldOption = getOption(memberField);

                    if (fieldOption != null && fieldOption != SyncPrefEnum.NEVER)
                      {
                        book.add(syncObject.getInvid(), memberField.getID());
                        continue;
                      }
                  }

                // deleted

                if (!memberField.isDefined() && origField != null)
                  {
                    if (debug)
                      {
                        Ganymede.debug("SyncRunner.initializeFieldBook(): Deleted field " +
                                       memberField + " in " + syncObject);
                      }

                    SyncPrefEnum fieldOption = getOption(memberField);

                    if (fieldOption != null && fieldOption != SyncPrefEnum.NEVER)
                      {
                        book.add(syncObject.getInvid(), memberField.getID());
                        continue;
                      }
                  }

                // changed

                if (memberField.isDefined() && origField != null)
                  {
                    if (debug)
                      {
                        Ganymede.debug("SyncRunner.initializeFieldBook(): Edited (maybe) field " +
                                       memberField + " in " + syncObject);
                      }

                    // check to see if the field has changed and whether we
                    // should include it.  The 'hasChanged()' check is
                    // required because this shouldInclude() call will always
                    // return true if memberField is defined as an 'always
                    // include' field, whereas in this object-level
                    // shouldInclude() check loop, we are looking to see
                    // whether a field that we care about was changed.

                    // Basically the hasChanged() call checks to see if the
                    // field changed, and the shouldInclude() call checks to
                    // see if it's a field that we care about.

                    if (debug)
                      {
                        if (memberField.hasChanged(origField))
                          {
                            Ganymede.debug("SyncRunner.initializeFieldBook(): Hey field " +
                                           memberField + " really did change compared to the original " + origField);
                          }
                        else
                          {
                            Ganymede.debug("SyncRunner.initializeFieldBook(): Hey I don't think field " +
                                           memberField + " changed at all compared to the original " + origField);
                          }
                      }

                    if (memberField.hasChanged(origField) && shouldInclude(memberField, origField, null))
                      {
                        book.add(syncObject.getInvid(), memberField.getID());
                        continue;
                      }
                  }
              }
          }

        // give our plug-in management class the chance to extend the
        // FieldBook that we're assembling with reference objects, etc.

        master.augment(book, syncObject);
      }
  }

  /**
   * <p>Returns true if the Sync Channel attached to this SyncRunner
   * is configured to write plain text passwords.</p>
   */

  public boolean includePlaintextPasswords()
  {
    return includePlaintextPasswords;
  }

  /**
   * <p>Returns true if this sync channel is configured to ever
   * include objects of the given baseID.</p>
   *
   * <p>This method does not take into account any augmentation done
   * by a linked {@link arlut.csd.ganymede.server.SyncMaster SyncMaster}.</p>
   */

  public boolean mayInclude(short baseID)
  {
    SyncPrefEnum x = getOption(baseID);

    return x == SyncPrefEnum.WHENCHANGED;
  }

  /**
   * <p>Returns true if this sync channel is configured to ever
   * include objects of the given baseID.</p>
   *
   * <p>This method does not take into account any augmentation done
   * by a linked {@link arlut.csd.ganymede.server.SyncMaster SyncMaster}.</p>
   */

  public boolean mayInclude(DBObject object)
  {
    SyncPrefEnum x = getOption(object.getTypeID());

    return x == SyncPrefEnum.WHENCHANGED;
  }

  /**
   * <p>Returns true if the DBEditObject passed in needs to be synced
   * to this channel.</p>
   *
   * <p>Only intended to be used for Full State Dumps, as it doesn't
   * include the SyncMaster augmentation for incrementals.</p>
   */

  public boolean shouldInclude(DBEditObject object)
  {
    if (!mayInclude(object))
      {
        return false;
      }

    if (object.getStatus() == ObjectStatus.DROPPING)
      {
        return false;
      }

    DBObject origObj = object.getOriginal();

    // We know that checked-out DBEditObjects have a copy of
    // all defined fields, so we don't need to consult the
    // (possibly null) origObj reference looking for fields
    // that were deleted from syncObject.

    for (DBField memberField: object.getFieldVect())
      {
        DBField origField;

        if (origObj == null)
          {
            origField = null;
          }
        else
          {
            origField = (DBField) origObj.getField(memberField.getID());
          }

        // created

        if (origField == null && memberField.isDefined())
          {
            SyncPrefEnum fieldOption = getOption(memberField);

            if (fieldOption != null && fieldOption != SyncPrefEnum.NEVER)
              {
                return true;
              }
          }

        // deleted

        if (!memberField.isDefined() && origField != null)
          {
            SyncPrefEnum fieldOption = getOption(memberField);

            if (fieldOption != null && fieldOption != SyncPrefEnum.NEVER)
              {
                return true;
              }
          }

        // changed

        if (memberField.isDefined() && origField != null)
          {
            // check to see if the field has changed and whether we
            // should include it.  The 'hasChanged()' check is
            // required because this shouldInclude() call will always
            // return true if memberField is defined as an 'always
            // include' field, whereas in this object-level
            // shouldInclude() check loop, we are looking to see
            // whether a field that we care about was changed.

            // Basically the hasChanged() call checks to see if the
            // field changed, and the shouldInclude() call checks to
            // see if it's a field that we care about.

            if (memberField.hasChanged(origField) && shouldInclude(memberField, origField, null))
              {
                return true;
              }
          }
      }

    return false;
  }

  /**
   * <p>Returns true if the given field needs to be sent to this sync
   * channel.  This method is responsible for doing the determination
   * only if both field and origField are not null and isDefined().</p>
   *
   * <p>Only intended to be used for Full State Dumps, as it doesn't
   * include the SyncMaster augmentation for incrementals.</p>
   */

  public boolean shouldInclude(DBField newField, DBField origField, FieldBook book)
  {
    SyncPrefEnum fieldOption;

    /* -- */

    if (debug)
      {
        Ganymede.debug("SyncRunner.shouldInclude(" + newField + ", " + origField + ", book)");
      }

    if (!newField.isDefined())
      {
        // "newField is undefined"
        throw new IllegalArgumentException(ts.l("shouldInclude.badNewField"));
      }

    if (origField == null)
      {
        // "origField is null"
        throw new NullPointerException(ts.l("shouldInclude.badOrigField"));
      }

    if (book != null)
      {
        if (book.has(newField.getOwner().getInvid(), newField.getID()))
          {
            return true;
          }
      }

    fieldOption = getOption(newField);

    if (fieldOption == null || fieldOption == SyncPrefEnum.NEVER)
      {
        if (debug)
          {
            Ganymede.debug("SyncRunner.shouldInclude(): nope, we don't care about field " + newField);
          }

        return false;
      }
    else if (fieldOption == SyncPrefEnum.ALWAYS)
      {
        if (debug)
          {
            Ganymede.debug("SyncRunner.shouldInclude(): oh, hell yeah, we totally care about field " + newField);
          }

        return true;
      }
    else if (fieldOption == SyncPrefEnum.WHENCHANGED)
      {
        if (debug)
          {
            Ganymede.debug("SyncRunner.shouldInclude(): Well, we might care about field " + newField);
          }

        if (newField.hasChanged(origField))
          {
            return true;
          }

        if (debug)
          {
            Ganymede.debug("SyncRunner.shouldInclude(): But actually, we don't think that field " +
                           newField + " changed in this transaction, so no.");
          }
      }
    else
      {
        if (debug)
          {
            Ganymede.debug("SyncRunner.shouldInclude(): WTF?  fieldOption == " +
                           fieldOption + " for field " + newField);
          }
      }

    return false;
  }

  /**
   * <p>Returns true if the given type of field may be included in
   * this sync channel.  The hasChanged parameter should be set to
   * true if the field being tested was changed in the current
   * transaction, or false if it remains unchanged.</p>
   *
   * <p>This differs from shouldInclude on DBField in that this method
   * leaves it to the caller to decide whether the field has changed.</p>
   */

  public boolean mayInclude(DBField field, boolean hasChanged)
  {
    return this.mayInclude(field.getOwner().getTypeID(), field.getID(), hasChanged);
  }

  /**
   * <p>Returns true if the given type of field may be included in
   * this sync channel.  The hasChanged parameter should be set to
   * true if the field being tested was changed in the current
   * transaction, or false if it remains unchanged.</p>
   *
   * <p>This differs from shouldInclude on DBField in that this method
   * leaves it to the caller to decide whether the field has changed.</p>
   */

  public boolean mayInclude(short baseID, short fieldID, boolean hasChanged)
  {
    SyncPrefEnum x = getOption(baseID, fieldID);

    if (x == null)
      {
        return false;
      }

    switch (x)
      {
      case WHENCHANGED:
        // hasChanged will be set to true by XMLDumpContext when doing
        // a full state sync or dump, so WHENCHANGED is equivalent to
        // ALWAYS in that case.

        return hasChanged;

      case ALWAYS:
        return true;

      default:
        return false;
      }
  }

  /**
   * <p>Returns the SyncPrefEnum, if any, for the given
   * field.  This enum can be one of three values:</p>
   *
   * <p>NEVER, meaning the field is never included in this sync channel, nor
   * should it be examined to make a decision about whether a given
   * object is written to this sync channel.</p>
   *
   * <p>WHENCHANGED, meaning that the field is included in this sync channel if
   * it has changed, and that the object that includes the field
   * should be written to the sync channel if this field was changed
   * in the object.  If a field has an option string of WHENCHANGED but has
   * not changed in a given transaction, that field won't trigger the
   * object to be written to the sync channel.</p>
   *
   * <p>ALWAYS, meaning that the field is always included in this sync
   * channel if the object that it is contained in is sent to this
   * sync channel, even if it wasn't changed in the transaction.  If
   * this field was changed in a given transaction, that will suffice
   * to cause an object that is changed in any fashion during a
   * transaction to be sent to this sync channel.  In this sense, it
   * is like WHENCHANGED, but with the added feature that it will "ride along"
   * with its object to the sync channel, even if it wasn't changed
   * during the transaction.</p>
   *
   * <p>If WHENCHANGED or ALWAYS is true for a field in a given object
   * type, the corresponding SyncPrefEnum for the object's type should
   * be WHENCHANGED, signaling that at least some fields in the object
   * should be sent to this sync channel when the object is involved
   * in a transaction.</p>
   */

  private SyncPrefEnum getOption(DBField field)
  {
    return matrix.get(FieldOptionDBField.matrixEntry(field.getOwner().getTypeID(), field.getID()));
  }

  /**
   * <p>Returns the SyncPrefEnum, if any, for the given base and field ids.
   * This enum can be one of three values:</p>
   *
   * <p>NEVER, meaning the field is never included in this sync channel, nor
   * should it be examined to make a decision about whether a given
   * object is written to this sync channel.</p>
   *
   * <p>WHENCHANGED, meaning that the field is included in this sync channel if
   * it has changed, and that the object that includes the field
   * should be written to the sync channel if this field was changed
   * in the object.  If a field has an option string of WHENCHANGED but has
   * not changed in a given transaction, that field won't trigger the
   * object to be written to the sync channel.</p>
   *
   * <p>ALWAYS, meaning that the field is always included in this sync
   * channel if the object that it is contained in is sent to this
   * sync channel, even if it wasn't changed in the transaction.  If
   * this field was changed in a given transaction, that will suffice
   * to cause an object that is changed in any fashion during a
   * transaction to be sent to this sync channel.  In this sense, it
   * is like WHENCHANGED, but with the added feature that it will "ride along"
   * with its object to the sync channel, even if it wasn't changed
   * during the transaction.</p>
   *
   * <p>If WHENCHANGED or ALWAYS is true for a field in a given object
   * type, the corresponding SyncPrefEnum for the object's type should
   * be WHENCHANGED, signaling that at least some fields in the object
   * should be sent to this sync channel when the object is involved
   * in a transaction.</p>
   */

  private SyncPrefEnum getOption(short baseID, short fieldID)
  {
    return matrix.get(FieldOptionDBField.matrixEntry(baseID, fieldID));
  }

  /**
   * <p>Returns the SyncPrefEnum, if any, for the given base.  This option
   * string should be one of two values:</p>
   *
   * <p>NEVER, meaning that this object type will never be sent to this
   * sync channel.</p>
   *
   * <p>WHENCHANGED, meaning that this object type may be sent to this
   * sync channel if any field contained within it which has a
   * SyncPrefEnum of WHENCHANGED or ALWAYS was changed during the
   * transaction.
   */

  private SyncPrefEnum getOption(short baseID)
  {
    return matrix.get(FieldOptionDBField.matrixEntry(baseID));
  }

  /**
   * <p>This is the method that will run when the GanymedeScheduler
   * schedules us for execution after a transaction commit.  If our
   * incremental flag is set to true, we'll consider ourselves as
   * servicing an incremental build, in which case we just call our
   * servicer script with the last transaction committed as our
   * command line argument.  If we are full state, we'll do more like
   * a {@link arlut.csd.ganymede.server.GanymedeBuilderTask}| and
   * write out a complete, filtered dump of the server's contents to
   * an XML file and then call the servicer with that file name as a
   * command line argument.</p>
   */

  public void run()
  {
    if (this.mode == SyncType.FULLSTATE)
      {
        if (this.needBuild.isSet())
          {
            runFullState();
          }
        else
          {
            if (debug)
              {
                // "{0} has not seen any changes that need to be processed."
                Ganymede.debug(ts.l("run.skipping_full", this.getName()));
              }
          }
      }
    else if (this.mode == SyncType.INCREMENTAL)
      {
        if (this.needBuild.isSet() || getQueueSize() > 0)
          {
            runIncremental();
          }
        else
          {
            if (debug)
              {
                // "{0} has no incremental transactions to process."
                Ganymede.debug(ts.l("run.skipping_incremental", this.getName()));
              }
          }
      }
  }

  /**
   * <p>This method handles running a full state XML build.</p>
   */

  public void runFullState()
  {
    String label = null;
    Thread currentThread = java.lang.Thread.currentThread();
    boolean
      success1 = false;
    boolean alreadyDecdCount = false;
    GanymedeSession session = null;
    DBDumpLock lock = null;

    /* -- */

    String shutdownState = GanymedeServer.shutdownSemaphore.increment();

    if (shutdownState != null)
      {
        // "Aborting full state sync channel {0} for shutdown condition: {1}"
        Ganymede.debug(ts.l("runFullState.shutting_down", this.getClass().getName(), shutdownState));
        return;
      }

    try
      {
        GanymedeBuilderTask.incPhase1(true);

        try
          {
            // XXX note: this string must not be changed because the
            // GanymedeSession constructor behaves in a special way
            // for "builder:" and "sync channel:" session labels.

            session = new GanymedeSession("sync channel:");

            try
              {
                lock = session.getDBSession().openDumpLock();
              }
            catch (InterruptedException ex)
              {
                // "Could not run full state sync channel {0}, couldn''t get dump lock."
                Ganymede.debug(ts.l("runFullState.failed_lock_acquisition", this.getClass().getName()));
                return;
              }

            writeFullStateSync(session);
          }
        catch (Exception ex)
          {
            GanymedeBuilderTask.decPhase1(true); // we're aborting, notify the consoles
            alreadyDecdCount = true;

            Ganymede.debug(Ganymede.stackTrace(ex));
            return;
          }
        finally
          {
            if (!alreadyDecdCount)
              {
                GanymedeBuilderTask.decPhase1(false); // we'll roll directly into phase 2 on the admin consoles, etc.
              }

            // release the lock, and so on

            if (session != null)
              {
                session.logout();       // will clear the dump lock

                session = null;
                lock = null;
              }
          }

        if (currentThread.isInterrupted())
          {
            // "Full state sync channel {0} interrupted, not doing network build."
            Ganymede.debug(ts.l("runFullState.task_interrupted", this.getClass().getName()));
            Ganymede.updateBuildStatus();
            return;
          }

        try
          {
            GanymedeBuilderTask.incPhase2(true);

            runFullStateService();
          }
        finally
          {
            GanymedeBuilderTask.decPhase2(true);
          }
      }
    finally
      {
        GanymedeServer.shutdownSemaphore.decrement();
      }
  }

  /**
   * <p>This method writes out a full state XML dump to the fullStateFile
   * registered in this SyncRunner.  It is run within the context of a
   * {@link arlut.csd.ganymede.server.DBDumpLock} asserted on the
   * Ganymede {@link arlut.csd.ganymede.server.DBStore}.</p>
   */

  private void writeFullStateSync(GanymedeSession session) throws NotLoggedInException, RemoteException, IOException
  {
    // Clear the needBuild flag, since we're already committed to
    // trying to do a full state build, and the DBDumpLock that is
    // held for us by runFullState() will keep any new commits from
    // being integrated until we finish.
    //
    // We could wait and clear the flag after we write, but if we run
    // into an exception opening the output file, we don't want to
    // perseverate on trying to write this file out when whatever
    // caused the exception will presumably need someone's
    // intervention to cure.  Waiting until someone again modifies an
    // object that matches this SyncRunner's criteria shouldn't hurt
    // anything significantly.

    this.needBuild.set(false);

    // session.getDataXML will obtain and manage another dump lock on
    // another thread, but we have ensured that DBWriteLock will not
    // allow any establishment of a DBWriteLock while the upper
    // DBDumpLock in runFullState() remains in effect, thus preventing
    // deadlock.

    ReturnVal retVal = session.getDataXML(this.name, true, true);
    FileTransmitter transmitter = retVal.getFileTransmitter();
    BufferedOutputStream out = null;

    out = new BufferedOutputStream(new FileOutputStream(this.fullStateFile));

    try
      {
        byte[] chunk = transmitter.getNextChunk();

        while (chunk != null)
          {
            out.write(chunk, 0, chunk.length);
            chunk = transmitter.getNextChunk();
          }
      }
    finally
      {
        out.close();
      }
  }

  /**
   * <p>This method executes the external build, feeding the external
   * service script the name of the full state XML file that we dumped
   * out.</p>
   */

  private void runFullStateService()
  {
    if (debug)
      {
        // "Full State Sync Channel {0} external build running."
        Ganymede.debug(ts.l("runFullStateService.running", this.name));
      }

    int resultCode = -999;  // a resultCode of 0 is success

    File file = new File(this.serviceProgram);

    if (file.exists())
      {
        try
          {
            resultCode = FileOps.runProcess(this.serviceProgram);
          }
        catch (IOException ex)
          {
            // "Couldn''t exec full state sync channel build script {0} for Sync Channel {1}, due to IOException:\n{2}"
            Ganymede.debug(ts.l("runFullStateService.exception_running", this.serviceProgram,
                                this.name, Ganymede.stackTrace(ex)));
          }
        catch (InterruptedException ex)
          {
            // "Failure during exec of full state sync channel build script {0} for Sync Channel {1}:\n{2}"
            Ganymede.debug(ts.l("runFullStateService.interrupted", this.serviceProgram,
                                this.name, Ganymede.stackTrace(ex)));
          }

        if (debug)
          {
            // "Full State Sync Channel {0} external build completed."
            Ganymede.debug(ts.l("runFullStateService.ran", this.name));
          }
      }
    else
      {
        // "Full State Sync Channel {1} error: external build script {0} does not exist."
        Ganymede.debug(ts.l("runFullStateService.whaa", this.serviceProgram, this.name));
      }

    if (resultCode != 0)
      {
        String path = "";

        try
          {
            path = file.getCanonicalPath();
          }
        catch (IOException ex)
          {
            path = this.serviceProgram;
          }

        // Error encountered running sync script "{0}" for the "{1}" Sync Channel.
        //
        // I got a result code of {2} when I tried to run it.

        String message = ts.l("runFullStateService.externalerror", path, this.getName(), Integer.valueOf(resultCode));

        DBLogEvent event = new DBLogEvent("externalerror", message, null, null, null, null);

        Ganymede.log.logSystemEvent(event);
      }
  }

  /**
   * <p>This method handles running an incremental XML build.</p>
   */

  public void runIncremental()
  {
    int myTransactionNumber;
    String myName, myServiceProgram;
    String invocation[];
    String shutdownState = null;
    File file;
    List results = null;

    /* -- */

    synchronized (queueGrowthMonitor)
      {
        queueGrowthSize = 0;

        myName = getName();
        myTransactionNumber = getTransactionNumber();
        myServiceProgram = getServiceProgram();
        invocation = new String[2];
        invocation[0] = myServiceProgram;
        invocation[1] = String.valueOf(myTransactionNumber);
        needBuild.set(false);
      }

    // increment the shutdownSemaphore so that the system knows we
    // are in the middle of running a critical task, and that it
    // should not allow shutdown until the build completes

    shutdownState = GanymedeServer.shutdownSemaphore.increment();

    if (shutdownState != null)
      {
        // "Refusing to run Sync Channel {0}, due to shutdown condition: {1}"
        Ganymede.debug(ts.l("runIncremental.shutting_down", this.getName(), shutdownState));
        return;
      }

    try
      {
        GanymedeBuilderTask.incPhase2(true); // so that the client sees the phase 2 icon rolling

        if (debug)
          {
            // "SyncRunner {0} running"
            Ganymede.debug(ts.l("runIncremental.running", myName));
          }

        if (getServiceProgram() != null)
          {
            file = new File(getServiceProgram());

            if (file.exists())
              {
                try
                  {
                    results = FileOps.runCaptureOutputProcess(invocation);
                  }
                catch (IOException ex)
                  {
                    // "Couldn''t exec SyncRunner {0}''s service program "{1}" due to IOException: {2}"
                    Ganymede.debug(ts.l("runIncremental.ioException", myName, myServiceProgram, ex));
                  }
                catch (InterruptedException ex)
                  {
                    // "Failure during exec of SyncRunner {0}''s service program "{1}""
                    Ganymede.debug(ts.l("runIncremental.interrupted", myName, myServiceProgram));
                  }
              }
            else
              {
                // ""{0}" doesn''t exist, not running external service program for SyncRunner {1}"
                Ganymede.debug(ts.l("runIncremental.nonesuch", myServiceProgram, myName));
              }

            if (results == null || ((Integer) results.get(0)).intValue() != 0)
              {
                String scriptPath = "";

                try
                  {
                    scriptPath = file.getCanonicalPath();
                  }
                catch (IOException ex)
                  {
                    scriptPath = getServiceProgram();
                  }

                String message = "";

                if (results == null)
                  {
                    // Error encountered running sync script "{0}" for the "{1}" Sync Channel.
                    //
                    // I got a result code of {2} when I tried to run it.
                    //
                    // Queue size is {3}.
                    //
                    // {4}

                    message = ts.l("runIncremental.externalerror", scriptPath, this.getName(), -999, Integer.valueOf(getQueueSize()), "");
                  }
                else
                  {
                    // Error encountered running sync script "{0}" for the "{1}" Sync Channel.
                    //
                    // I got a result code of {2} when I tried to run it.
                    //
                    // Queue size is {3}.
                    //
                    // {4}

                    message = ts.l("runIncremental.externalerror", scriptPath, this.getName(), results.get(0), Integer.valueOf(getQueueSize()), (String) results.get(2));
                  }

                DBLogEvent event = new DBLogEvent("externalerror", message, null, null, null, null);

                Ganymede.log.logSystemEvent(event);
              }
          }
        else
          {
            // "No external service program defined for SyncRunner {0}, not servicing {0}!"
            Ganymede.debug(ts.l("runIncremental.undefined", myName));
          }
      }
    finally
      {
        // decrement first, so that we won't lose if the less critical
        // GanymedeBuilderTask.decPhase2() throws an exception

        GanymedeServer.shutdownSemaphore.decrement();
        GanymedeBuilderTask.decPhase2(true);
      }

    if (debug)
      {
        // "SyncRunner {0} finished"
        Ganymede.debug(ts.l("runIncremental.done", myName));
      }

    updateAdminConsole(true);
  }

  /**
   * <p>Performs a readdir loop on the queue directory for this sync
   * channel (if incremental) to see how many entries are currently in
   * the queue.</p>
   */

  public int getQueueSize()
  {
    String[] dirEntries = new File(this.getDirectory()).list(dirFilter);

    if (dirEntries == null)
      {
        return 0;
      }
    else
      {
        return dirEntries.length;
      }
  }

  /**
   * <p>Updates the queue status in the admin consoles</p>
   */

  private void updateAdminConsole(boolean justRanQueue)
  {
    if (handle == null)
      {
        return;
      }

    if (debug)
      {
        System.err.println("SyncRunner.updateAdminConsole(): entering queueGrowthMonitor section");
      }

    synchronized (queueGrowthMonitor)
      {
        if (debug)
          {
            System.err.println("SyncRunner.updateAdminConsole(): now in queueGrowthMonitor section");
          }

        int currentQueueSize = getQueueSize();

        if (currentQueueSize == 0)
          {
            handle.setTaskStatus(scheduleHandle.TaskStatus.EMPTYQUEUE, 0, "");
          }
        else
          {
            if (justRanQueue)
              {
                if (currentQueueSize <= queueGrowthSize)
                  {
                    handle.setTaskStatus(scheduleHandle.TaskStatus.NONEMPTYQUEUE, currentQueueSize, "");
                  }
                else
                  {
                    handle.setTaskStatus(scheduleHandle.TaskStatus.STUCKQUEUE, currentQueueSize, "");
                    needBuild.set(true);
                  }
              }
            else
              {
                // if we got set to STUCKQUEUE after running the
                // service program, we'll stay stuck until
                // updateAdminConsole(true) says otherwise.

                if (handle.getTaskStatus() == scheduleHandle.TaskStatus.STUCKQUEUE)
                  {
                    handle.setTaskStatus(scheduleHandle.TaskStatus.STUCKQUEUE, currentQueueSize, "");
                  }
                else
                  {
                    handle.setTaskStatus(scheduleHandle.TaskStatus.NONEMPTYQUEUE, currentQueueSize, "");
                  }
              }
          }
      }
  }

  public String toString()
  {
    return this.getName();
  }
}

/*------------------------------------------------------------------------------
                                                                           class
                                                                  QueueDirFilter

------------------------------------------------------------------------------*/

/**
 * <p>Filename pattern matcher for our incremental sync channel queue
 * files.</p>
 */

class QueueDirFilter implements java.io.FilenameFilter {

  static private Pattern p = Pattern.compile("^\\d+$");

  public QueueDirFilter()
  {
  }

  public boolean accept(File dir, String name)
  {
    return p.matcher(name).matches();
  }
}


/*------------------------------------------------------------------------------
                                                                           class
                                                                  NoopSyncMaster

------------------------------------------------------------------------------*/

/**
 * <p>No-op SyncMaster class used in cases where no SyncMaster is defined
 * for a delta Sync Channel object.</p>
 *
 * <p>By using a No-op Sync Master, we simplify the logic in the
 * SyncRunner's shouldInclude() method.</p>
 */

class NoopSyncMaster implements SyncMaster {

  public NoopSyncMaster()
  {
  }

  /**
   * <p>The augment() method optionally adds DBObject and DBField
   * identifiers to the FieldBook book parameter if the SyncMaster
   * decides that the additional DBObject/DBFields need to be written
   * to a delta sync channel in response to the changes made to
   * obj.</p>
   */

  public void augment(FieldBook book, DBEditObject obj)
  {
  }
}
