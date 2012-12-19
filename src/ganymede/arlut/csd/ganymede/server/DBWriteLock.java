/*
   GASH 2

   DBWriteLock.java

   The GANYMEDE object storage system.

   Created: 2 July 1996

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------

   Ganymede Directory Management System

   Copyright (C) 1996-2012
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

import java.util.Vector;

/*------------------------------------------------------------------------------
                                                                           class
                                                                     DBWriteLock

------------------------------------------------------------------------------*/

/**
 * <p>A DBWriteLock is a {@link arlut.csd.ganymede.server.DBLock DBLock}
 * subclass used to lock one or more {@link
 * arlut.csd.ganymede.server.DBObjectBase DBObjectBases} for the
 * purposes of committing changes into those bases, preventing any
 * other threads from reading or writing to the database while the
 * update is being performed.  When a DBWriteLock is established on a
 * DBObjectBase, the establishing thread suspends until all readers
 * currently working in the specified DBObjectBases complete.  The
 * write lock is then established, and the thread possessing the
 * DBWriteLock is free to replace objects in the {@link
 * arlut.csd.ganymede.server.DBStore DBStore} with modified copies.</p>
 *
 * <p>DBWriteLocks are typically created and managed by the code in the
 * {@link arlut.csd.ganymede.server.DBEditSet DBEditSet} class.  It is
 * very important that any thread that obtains a DBWriteLock be
 * scrupulous about releasing the lock in a timely fashion once the
 * appropriate changes are made in the database.</p>
 *
 * @see arlut.csd.ganymede.server.DBEditSet
 * @see arlut.csd.ganymede.server.DBObjectBase
 */

public class DBWriteLock extends DBLock {

  static final boolean debug = false;

  /* -- */

  /**
   * <p>Constructor to get a write lock on all the object bases</p>
   */

  public DBWriteLock(DBStore store)
  {
    this.key = null;
    this.lockSync = store.lockSync;
    this.baseSet = store.getBases();
  }

  /**
   * <p>Constructor to get a write lock on a subset of the server's
   * object bases.</p>
   */

  public DBWriteLock(DBStore store, Vector<DBObjectBase> baseSet)
  {
    this.key = null;
    this.lockSync = store.lockSync;
    this.baseSet = baseSet;
  }

  /**
   * <p>A thread that calls establish() will be suspended (waiting on
   * the server's {@link arlut.csd.ganymede.server.DBStore DBStore}
   * until all DBObjectBases listed in this DBWriteLock's constructor
   * are available to be locked.  At that point, the thread blocking
   * on establish() will wake up possessing a write lock on the
   * requested DBObjectBases.</p>
   *
   * <p>It is possible for the establish() to fail completely.. the
   * admin console may reject a client whose thread is blocking on
   * establish(), for instance, or the server may be shut down.  In
   * those cases, another thread may call this DBWriteLock's {@link
   * arlut.csd.ganymede.server.DBLock#abort() abort()} method, in
   * which case establish() will throw an InterruptedException, and
   * the lock will not be established.</p>
   *
   * @param key An object used in the server to uniquely identify the
   * entity internal to Ganymede that is attempting to obtain the
   * lock, typically a {@link
   * arlut.csd.ganymede.server.GanymedeSession GanymedeSession} or a
   * {@link arlut.csd.ganymede.server.GanymedeBuilderTask
   * GanymedeBuilderTask}.
   */

  public void establish(Object key) throws InterruptedException
  {
    boolean waiting = false;
    boolean okay = false;

    /* -- */

    if (debug)
      {
        System.err.println(key + ": DBWriteLock.establish(): enter");
        System.err.println(key + ": DBWriteLock.establish(): baseSet vector size " + baseSet.size());
      }

    synchronized (lockSync)
      {
        if (!lockSync.claimLockKey(key, this))
          {
            throw new InterruptedException("DBWriteLock.establish(): error, lock already held for key: " + key);
          }

        try
          {
            if (debug)
              {
                System.err.println(key + ": DBWriteLock.establish(): added myself to the DBLockSync lockHash.");
              }

            // okay, now we've got ourselves recorded as the only lock
            // that can be in the process of establishing for this
            // key.  record that fact so that we can interact with
            // another thread trying to release us.

            this.key = key;
            this.inEstablish = true;

            // wait until there are no dumpers queued for establish,
            // then queue up for own own turn at our bases

            while (!okay)
              {
                if (this.abort)
                  {
                    throw new InterruptedException("aborted on command");
                  }

                // if the server is in shutdown or in schema edit, we
                // can't proceed.  in theory, this check would be
                // taken care of above here, but internal sessions
                // don't use the login semaphore so that a delayed
                // shutdown won't wait around forever for
                // Ganymede.internalSession

                String disabledMessage = GanymedeServer.lSemaphore.checkEnabled();

                if (disabledMessage != null && !disabledMessage.equals("shutdown"))
                  {
                    // if the server is in soon-to-be-shutdown mode,
                    // we'll go ahead and allow the transaction to
                    // complete.  if the server is in schema edit
                    // mode, or for some other reason is refusing
                    // logins, we won't allow a write lock. Wouldn't
                    // be prudent at this juncture.

                    throw new InterruptedException(disabledMessage);
                  }

                okay = true;

                for (DBObjectBase base: baseSet)
                  {
                    // we won't queue up on the bases while there are
                    // dumpers waiting or in effect.  once we get on
                    // the writer wait lists, the dumpers will wait
                    // for us, but we let dumpers proceed before us if
                    // they are waiting

                    if (!base.isWaitingDumperListEmpty() || !base.isDumpLockListEmpty())
                      {
                        if (debug)
                          {
                            System.err.println(this.key + ": DBWriteLock.establish(): waiting for dumpers on base " +
                                               base.getName());
                            System.err.println(this.key + ": DBWriteLock.establish(): dumperList size: " +
                                               base.getWaitingDumperListSize());
                          }

                        okay = false;
                        break;
                      }
                  }

                if (!okay)
                  {
                    lockSync.wait(2500);
                  }
              }

            if (debug)
              {
                System.err.println(this.key + ": DBWriteLock.establish(): no dumpers queued.");
              }

            // okay, there are no dump locks waiting to establish.
            // Add ourselves to the ObjectBase write queues.  This
            // will prevent any read locks from establishing, but any
            // readlocks that have already been granted will block us
            // later.  The idea is for the granted read locks to drain
            // away while we block out any new readers until we're
            // through

            for (DBObjectBase base: baseSet)
              {
                base.addWaitingWriter(this);
              }

            waiting = true;

            if (debug)
              {
                System.err.println(this.key + ": DBWriteLock.establish(): added ourself to the writerList.");
              }

            // spinwait until we can get into all of the ObjectBases
            // note that since we added ourselves to the writer
            // queues, we know that any new dump or read locks will
            // wait until we finish.. at this point, we're just
            // waiting for existing read locks to drain

            okay = false;

            while (!okay)
              {
                if (debug)
                  {
                    System.err.println(this.key + ": DBWriteLock.establish(): spinning.");
                  }

                if (this.abort)
                  {
                    // we've been told to abort by another thread's
                    // abort() call.  get out of dodge.

                    throw new InterruptedException("aborted on command");
                  }

                okay = true;

                for (DBObjectBase base: baseSet)
                  {
                    // writers are exclusive.. if any lock of any kind
                    // is asserted, we can't play

                    if (base.isLocked())
                      {
                        if (debug)
                          {
                            if (!base.isReaderEmpty())
                              {
                                System.err.println(this.key +
                                                   ": DBWriteLock.establish(): " +
                                                   "waiting for readers to release.");
                              }
                            else if (!base.isDumpLockListEmpty())
                              {
                                System.err.println(this.key +
                                                   ": DBWriteLock.establish(): " +
                                                   "waiting for dumpers to release.");
                              }
                            else if (base.isWriteInProgress())
                              {
                                System.err.println(this.key +
                                                   ": DBWriteLock.establish(): " +
                                                   "waiting for writer to release.");
                              }
                          }

                        okay = false;
                        break;
                      }
                  }

                if (!okay)
                  {
                    lockSync.wait(2500);
                    continue;
                  }

                // nothing can stop us now

                for (DBObjectBase base: baseSet)
                  {
                    base.setWriteLock(this);
                  }

                this.locked = true;
                lockSync.incLockCount();
              }
          }
        finally
          {
            this.inEstablish = false;

            if (waiting)
              {
                for (DBObjectBase base: baseSet)
                  {
                    base.removeWaitingWriter(this);
                  }
              }

            if (!this.locked)
              {
                lockSync.unclaimLockKey(key, this);
                this.key = null;
              }

            lockSync.notifyAll();
          }
      }

    if (debug)
      {
        System.err.println(key + ": DBWriteLock.establish(): got the lock.");
      }
  }

  /**
   * <p>Release this lock on all bases locked</p>
   */

  public void release()
  {
    synchronized (lockSync)
      {
        // if we are trying to force this lock to go away on behalf of
        // a thread distinct from the locking thread, we have to wait
        // for that thread's establish method to get clear

        while (this.inEstablish)
          {
            try
              {
                lockSync.wait(2500);
              }
            catch (InterruptedException ex)
              {
              }
          }

        // note that we have to check locked here or else we might
        // accidentally release somebody else's lock below, if we
        // called release on a non-locked thread.

        if (!locked)
          {
            return;
          }

        for (DBObjectBase base: baseSet)
          {
            base.clearWriteLock(this);
          }

        this.locked = false;
        lockSync.unclaimLockKey(key, this);

        key = null;             // gc

        lockSync.decLockCount();
        lockSync.notifyAll();   // many readers may want in
      }
  }

  /**
   * <p>Withdraw this lock.  This method can be called by a thread to
   * interrupt a lock establish that is blocked waiting to get
   * access to the appropriate set of
   * {@link arlut.csd.ganymede.server.DBObjectBase DBObjectBase} objects.  If
   * this method is called while another thread is blocked in
   * establish(), establish() will throw an InterruptedException.</p>
   *
   * <p>Once abort() is processed, this lock may never be established.
   * Any subsequent calls to estabish() will always throw
   * InterruptedException.</p>
   */

  public void abort()
  {
    synchronized (lockSync)
      {
        abort = true;
        release();              // blocks
      }
  }
}
