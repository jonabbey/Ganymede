/*
   GASH 2

   DBReadLock.java

   The GANYMEDE object storage system.

   Created: 2 July 1996

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
            
   Ganymede Directory Management System
 
   Copyright (C) 1996-2010
   The University of Texas at Austin

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
                                                                      DBReadLock

------------------------------------------------------------------------------*/

/**
 * <p>DBReadLock is a class used in the Ganymede server to represent a read lock on
 * one or more {@link arlut.csd.ganymede.server.DBObjectBase DBObjectBase} objects.  A
 * DBReadLock is used in the
 * {@link arlut.csd.ganymede.server.GanymedeSession GanymedeSession} class to guarantee
 * that all query operations go from start to finish without any changes being made
 * along the way.</p>
 *
 * <p>While a DBReadLock is established on a DBObjectBase, no changes may be made
 * to that base.  The {@link arlut.csd.ganymede.server.DBWriteLock DBWriteLock}'s
 * {@link arlut.csd.ganymede.server.DBWriteLock#establish(java.lang.Object) establish()}
 * method will suspend until all read locks on a base are cleared.  As soon as
 * a thread attempts to establish a DBWriteLock on a base, no more DBReadLocks
 * will be established on that base until the DBWriteLock is cleared, but any
 * DBReadLocks already established will persist until released, whereupon the
 * DBWriteLock will establish.</p>
 *
 * <p>See {@link arlut.csd.ganymede.server.DBLock DBLock},
 * {@link arlut.csd.ganymede.server.DBWriteLock DBWriteLock}, and
 * {@link arlut.csd.ganymede.server.DBDumpLock DBDumpLock} for details.</p>
 */

public class DBReadLock extends DBLock {

  static final boolean debug = false;

  /* -- */

  /**
   *
   * constructor to get a read lock on all the object bases
   *
   */

  public DBReadLock(DBStore store)
  {
    key = null;
    this.lockSync = store.lockSync;
    baseSet = store.getBases();
  }

  /**
   *
   * constructor to get a read lock on a subset of the
   * object bases.
   *
   */

  public DBReadLock(DBStore store, Vector baseSet)
  {
    key = null;
    this.lockSync = store.lockSync;
    this.baseSet = baseSet;
  }

  /**
   * <p>Establish a read lock on bases specified in this DBReadLock's
   * constructor.  Can throw InterruptedException if another thread
   * orders us to abort() while we're waiting for permission to
   * proceed with reads on the specified baseset.</p>
   */

  public void establish(Object key) throws InterruptedException
  {
    boolean done = false, okay = false, added = false;
    DBObjectBase base;

    /* -- */

    synchronized (lockSync)
      {
        try
          {
            // okay, we're wanting to establish a read lock.. first we
            // check to see if we already have a lock with the same
            // lock key.. if so, and it's a reader, we'll add
            // ourselves to the list of locks held by that key.  If
            // not, we'll record that we are trying to establish/hold
            // a read lock with this key

            if (lockSync.isLockHeld(key) && !lockSync.isReadLock(key))
              {
                // we've got a write lock or a dump lock already held
                // on this key.  we can't proceed or else we might
                // deadlock in a number of ways.. if this lock wants
                // bases that we don't already have locked under this
                // key, or if granting this lock to a subset or our
                // locked set would confuse the lock scheduling,
                // either way.  better not to risk it.

                if (debug)
                  {
                    System.err.println("DBReadLock (" + key + "):  dump or write lock blocking us");
                  }
                
                throw new RuntimeException("Error: read lock sought by owner of existing write or dump lockset.");
              }

            // we may already have a readlock registered with this
            // key, but that's okay, since we only do read locks
            // in the GanymedeSession query() and dump() methods.
            // These methods have no possibility of deadlock, as
            // they will proceed to completion without acquiring
            // more locks.

            // so, record who we are and that we are pending 

            inEstablish = true;     
            this.key = key;
            lockSync.addReadLock(key, this);
            added = true;

            // okay, we've made sure that we're not going to chance a
            // deadlock by grabbing incompatible locks after we've
            // already gotten some.  now we need to actually acquire
            // lock the bases down.

            done = false;

            while (!done)
              {
                if (debug)
                  {
                    System.err.println("DBReadLock (" + key + "):  looping to get establish permission");
                  }

                // if we've received an abort notification, bail.

                if (abort)
                  {
                    throw new InterruptedException("DBReadLock (" + key + "):  establish aborting before permission granted");
                  }

                // assume we can proceed to get our lock until we find out
                // otherwise

                okay = true;

                // if there are any writers queued on any of the bases
                // we want to get a readlock for, we have to wait for
                // them to finish before we can proceed

                for (int i = 0; okay && (i < baseSet.size()); i++)
                  {
                    base = (DBObjectBase) baseSet.elementAt(i);
                
                    // check for writers.  we don't care about dumpers, since
                    // we can read without problems while a dump lock is held

                    // note that we check to see if the writer *wait
                    // queue* is non-empty.  the writer will remain in
                    // the writer wait queue as long as the lock is
                    // held

                    // we're being very polite, for we are a lowly
                    // read lock
                    
                    if (!base.isWriterEmpty() || base.isWriteInProgress())
                      {
                        if (debug)
                          {
                            System.err.println("DBReadLock (" + key + "):  base " + 
                                               base.getName() + " has writers queued/locked");
                          }

                        okay = false;
                      }
                  }

                if (!okay)
                  {
                    // oops, things aren't clear for us to lock yet.
                    // we need to wait for a few seconds, or until
                    // something wakes us up by doing a notifyAll() on
                    // our lockSync.p
                    
                    if (debug)
                      {
                        System.err.println("DBReadLock (" + key + "):  waiting on lockSync");
                      }
                    
                    lockSync.wait(2500); // an InterruptedException here gets propagated up

                    if (debug)
                      {
                        System.err.println("DBReadLock (" + key + "):  done waiting on lockSync");
                      }
                  }
                else
                  {
                    // we were given the okay to lock, do it

                    int i = 0;
                    
                    try
                      {
                        for (i = 0; i < baseSet.size(); i++)
                          {
                            base = (DBObjectBase) baseSet.elementAt(i);
                            base.addReader(this);
                          }

                        done = true;
                      }
                    catch (RuntimeException ex)
                      {
                        Ganymede.logError(ex);

                        // clean up intelligently if we can

                        for (int j = i-1; j >= 0; j--)
                          {
                            base = (DBObjectBase) baseSet.elementAt(i);
                            base.removeReader(this);
                          }

                        throw ex; // the finally clause below will clean up
                      }
                  }
              } // while (!done)

            // okay!  if we got this far, we're locked

            locked = true;
            lockSync.addLock(); // notify consoles

            if (debug)
              {
                System.err.println("DBReadLock (" + key + "):  read lock established");
              }
          }
        finally
          {
            inEstablish = false; // in case we threw an exception while establishing

            if (added && !locked)
              {
                lockSync.delReadLock(key, this);
              }

            lockSync.notifyAll(); // let a thread trying to release this lock proceed
          }
      } // synchronized (lockSync)
  }

  /**
   * <p>Relinquish the lock on bases held by this lock object.</p>
   *
   * <p>Should be called by {@link arlut.csd.ganymede.server.DBSession DBSession}'s
   * {@link arlut.csd.ganymede.server.DBSession#releaseLock(arlut.csd.ganymede.server.DBLock) releaseLock()}
   * method.</p>
   *
   * <p>Note that this method is designed to be able to be called from
   * one thread while another is trying to use and/or establish the lock.  If
   * this.abort is not set to true before calling release(), release() will
   * block until the establish is granted.  That's why abort() sets this.abort
   * to true before calling release().</p>
   *
   * <p>The point of release() is to clear out this lock's connections to
   * the locked object bases and to allow DBLock establish() methods in other
   * threads to proceed.</p>
   */

  public void release()
  {
    DBObjectBase base;

    /* -- */

    if (debug)
      {
        System.err.println("DBReadLock (" + key + "):  attempting release");
      }

    synchronized (lockSync)
      {
        // if this lock is being established in another thread, we
        // need to wait until that thread exits its establish
        // section.  if we haven't set abort to true, this won't
        // happen until it gets the lock established, or it
        // catches an InterruptedException for some reason.

        while (inEstablish)
          {
            if (debug)
              {
                System.err.println("DBReadLock (" + key + "):  release() looping waiting on inEstablish");
              }

            try
              {
                lockSync.wait(2500); // or until notify'ed
              } 
            catch (InterruptedException ex)
              {
              }
          }

        if (!locked)
          {
            if (debug)
              {
                System.err.println("DBReadLock (" + key + "):  release() not locked, returning");
              }

            return;
          }

        for (int i = 0; i < baseSet.size(); i++)
          {
            base = (DBObjectBase) baseSet.elementAt(i);
            base.removeReader(this);
          }

        locked = false;

        // dissociate this lock from the lockSync's lockHash.
        // if this key has another readlock, that other lock will
        // need to take care of its own dissociation

        lockSync.delReadLock(key, this);

        key = null;             // for gc

        if (debug)
          {
            System.err.println("DBReadLock (" + key + "):  release() released");
          }

        lockSync.removeLock();  // notify consoles
        lockSync.notifyAll(); // let other threads waiting to establish proceed
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
   * Any subsequent calls to establish() will always throw
   * InterruptedException.</p>
   *
   * <p>Note that calling abort() on a lock that has already established
   * in another thread will remove the lock, but a thread that is using
   * the lock to iterate over a list will explicitly need to check to
   * see if its lock was pulled.  
   *{@link arlut.csd.ganymede.server.GanymedeSession#queryDispatch(arlut.csd.ganymede.common.Query,boolean,boolean,arlut.csd.ganymede.server.DBLock,arlut.csd.ganymede.server.DBEditObject) queryDispatch()} 
   * and {@link arlut.csd.ganymede.server.GanymedeSession#getObjects(short) getObjects()}
   * both do this properly, so it is generally safe to abort read locks in the
   * GanymedeServer as needed.</p>
   */

  public void abort()
  {
    synchronized (lockSync)
      {
        abort = true;
        release();              // blocks until freed
      }
  }
}
