/*

   SyncRunner.java

   This class is used in the Ganymede server to handle sync channel
   synchronization.  It is responsible for acting as a Runnable in the
   Ganymede scheduler (when run, it executes the external service program for
   a given sync channel queue) and for tracking the data associated with the
   sync channel.
   
   Created: 2 February 2005
   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

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

import arlut.csd.ganymede.common.Invid;
import arlut.csd.ganymede.common.ObjectStatus;
import arlut.csd.ganymede.common.SchemaConstants;

import arlut.csd.Util.FileOps;
import arlut.csd.Util.TranslationService;

import java.io.File;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;

/*------------------------------------------------------------------------------
                                                                           class
                                                                      SyncRunner

------------------------------------------------------------------------------*/

/**
 * <p>This class is used in the Ganymede server to handle sync channel
 * synchronization.  It is responsible for acting as a Runnable in the
 * Ganymede scheduler (when run, it executes the external service program for
 * a given sync channel queue) and for tracking the data associated with the
 * sync channel.</p>
 */

public class SyncRunner implements Runnable {

  /**
   * <p>TranslationService object for handling string localization in
   * the Ganymede server.</p>
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.server.SyncRunner");

  private static Runtime runtime = null;

  // ---

  private Invid syncChannelInvid;
  private String name;
  private String directory;
  private String serviceProgram;
  private int transactionNumber;
  private Hashtable matrix;

  /* -- */

  public SyncRunner(DBObject syncChannel)
  {
    updateInfo(syncChannel);
  }

  public synchronized void updateInfo(DBObject syncChannel)
  {
    if (syncChannel.getTypeID() != SchemaConstants.SyncChannelBase)
      {
	// "Error, passed the wrong kind of DBObject."
	throw new IllegalArgumentException(ts.l("updateInfo.typeError"));
      }

    this.syncChannelInvid = syncChannel.getInvid();
    this.name = (String) syncChannel.getFieldValueLocal(SchemaConstants.SyncChannelName);
    this.directory = (String) syncChannel.getFieldValueLocal(SchemaConstants.SyncChannelDirectory);
    this.serviceProgram = (String) syncChannel.getFieldValueLocal(SchemaConstants.SyncChannelServicer);
    
    FieldOptionDBField f = (FieldOptionDBField) syncChannel.getFieldValueLocal(SchemaConstants.SyncChannelFields);

    this.matrix = (Hashtable) f.matrix.clone();
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

  public String getName()
  {
    return name;
  }

  public String getDirectory()
  {
    return directory;
  }

  public String getServiceProgram()
  {
    return serviceProgram;
  }

  /**
   * <p>Returns true if this sync channel is configured to ever
   * include objects of the given baseID.</p>
   */

  public boolean mayInclude(short baseID)
  {
    String x = getOption(baseID);
    
    return (x != null && x.equals("1"));
  }

  /**
   * <p>Returns true if this sync channel is configured to ever
   * include objects of the given baseID.</p>
   */

  public boolean mayInclude(DBObject object)
  {
    String x = getOption(object.getTypeID());
    
    return (x != null && x.equals("1"));
  }

  /**
   * <p>Returns true if the DBEditObject passed in needs to be synced
   * to this channel.</p>
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

    String fieldOption;
    DBObject origObj = object.getOriginal();

    Vector fieldCopies = object.getFieldVect();

    for (int i = 0; i < fieldCopies.size(); i++)
      {
	DBField memberField = (DBField) fieldCopies.elementAt(i);

	switch (memberField.getID())
	  {
	  case SchemaConstants.CreationDateField:
	  case SchemaConstants.CreatorField:
	  case SchemaConstants.ModificationDateField:
	  case SchemaConstants.ModifierField:
	    continue;
	  }

	fieldOption = getOption(memberField);

	if (fieldOption == null || fieldOption.equals("0"))
	  {
	    continue;
	  }
	else if (fieldOption.equals("2"))
	  {
	    return true;
	  }
	else if (fieldOption.equals("1"))
	  {
	    if (origObj == null)
	      {
		return true;
	      }

	    if (memberField.hasChanged((DBField) origObj.getField(memberField.getID())))
	      {
		return true;
	      }
	  }
      }

    return false;
  }

  /**
   * <p>Returns true if the given object type (baseID) and field
   * number (fieldID) should be included in this sync channel.  The
   * hasChanged parameter should be set to true if the field being
   * tested was changed in the current transaction, or false if it
   * remains unchanged.</p>
   */

  public boolean shouldInclude(DBField field, boolean hasChanged)
  {
    return this.shouldInclude(field.getOwner().getTypeID(), field.getID(), hasChanged);
  }

  /**
   * <p>Returns true if the given object type (baseID) and field
   * number (fieldID) should be included in this sync channel.  The
   * hasChanged parameter should be set to true if the field being
   * tested was changed in the current transaction, or false if it
   * remains unchanged.</p>
   */

  public boolean shouldInclude(short baseID, short fieldID, boolean hasChanged)
  {
    String x = getOption(baseID, fieldID);

    if (hasChanged)
      {
	return (x != null && (x.equals("1") || x.equals("2")));
      }
    else
      {
	return (x != null && x.equals("2"));
      }
  }

  /**
   * <p>Returns the option string, if any, for the given base and field.
   * This option string should be one of three values:</p>
   *
   * <p>"0", meaning the field is never included in this sync channel, nor
   * should it be examined to make a decision about whether a given
   * object is written to this sync channel.</p>
   *
   * <p>"1", meaning that the field is included in this sync channel if
   * it has changed, and that the object that includes the field
   * should be written to the sync channel if this field was changed
   * in the object.  If a field has an option string of "1" but has
   * not changed in a given transaction, that field won't trigger the
   * object to be written to the sync channel.</p>
   *
   * <p>"2", meaning that the field is always included in this sync
   * channel if the object that it is contained in is sent to this
   * sync channel, even if it wasn't changed in the transaction.  If
   * this field was changed in a given transaction, that will suffice
   * to cause an object that is changed in any fashion during a
   * transaction to be sent to this sync channel.  In this sense, it
   * is like "1", but with the added feature that it will "ride along"
   * with its object to the sync channel, even if it wasn't changed
   * during the transaction.</p>
   *
   * <p>If "1" or "2" is true for a field in a given object type, the
   * corresponding option string for the object's type should be 1,
   * signaling that at least some fields in the object should be sent
   * to this sync channel when the object is involved in a
   * transaction.</p>
   */

  private String getOption(DBField field)
  {
    return (String) matrix.get(FieldOptionDBField.matrixEntry(field.getOwner().getTypeID(), field.getID()));
  }

  /**
   * <p>Returns the option string, if any, for the given base and field.
   * This option string should be one of three values:</p>
   *
   * <p>"0", meaning the field is never included in this sync channel, nor
   * should it be examined to make a decision about whether a given
   * object is written to this sync channel.</p>
   *
   * <p>"1", meaning that the field is included in this sync channel if
   * it has changed, and that the object that includes the field
   * should be written to the sync channel if this field was changed
   * in the object.  If a field has an option string of "1" but has
   * not changed in a given transaction, that field won't trigger the
   * object to be written to the sync channel.</p>
   *
   * <p>"2", meaning that the field is always included in this sync
   * channel if the object that it is contained in is sent to this
   * sync channel, even if it wasn't changed in the transaction.  If
   * this field was changed in a given transaction, that will suffice
   * to cause an object that is changed in any fashion during a
   * transaction to be sent to this sync channel.  In this sense, it
   * is like "1", but with the added feature that it will "ride along"
   * with its object to the sync channel, even if it wasn't changed
   * during the transaction.</p>
   *
   * <p>If "1" or "2" is true for a field in a given object type, the
   * corresponding option string for the object's type should be 1,
   * signaling that at least some fields in the object should be sent
   * to this sync channel when the object is involved in a
   * transaction.</p>
   */

  private String getOption(short baseID, short fieldID)
  {
    return (String) matrix.get(FieldOptionDBField.matrixEntry(baseID, fieldID));
  }

  /**
   * <p>Returns the option string, if any, for the given base.  This option
   * string should be one of two values:</p>
   *
   * <p>"0", meaning that this object type will never be sent to this
   * sync channel.</p>
   *
   * <p>"1", meaning that this object type may be sent to this sync
   * channel if any field contained within it which has an option
   * string of "1" or "2" was changed during the transaction.
   */

  private String getOption(short baseID)
  {
    return (String) matrix.get(FieldOptionDBField.matrixEntry(baseID));
  }

  public void run()
  {
    int myTransactionNumber;
    String myName, myServiceProgram, invocation;
    File file;

    /* -- */

    synchronized (this)
      {
	myName = getName();
	myTransactionNumber = getTransactionNumber();
	myServiceProgram = getServiceProgram();
	invocation = myServiceProgram + " " + String.valueOf(myTransactionNumber);
      }

    // "SyncRunner {0} running"
    Ganymede.debug(ts.l("run.running", myName));

    file = new File(myServiceProgram);

    if (file.exists())
      {
	if (runtime == null)
	  {
	    runtime = Runtime.getRuntime();
	  }

	try
	  {
	    FileOps.runProcess(invocation);
	  }
	catch (IOException ex)
	  {
	    // "Couldn''t exec SyncRunner {0}''s service program "{1}" due to IOException: {2}"
	    Ganymede.debug(ts.l("run.ioException", myName, myServiceProgram, ex));
	  }
	catch (InterruptedException ex)
	  {
	    // "Failure during exec of SyncRunner {0}''s service program "{1}""
	    Ganymede.debug(ts.l("run.interrupted", myName, myServiceProgram));
	  }
      }
    else
      {
	// ""{0}" doesn''t exist, not running external service program for SyncRunner {1}"
	Ganymede.debug(ts.l("run.nonesuch", myServiceProgram, myName));
      }

    // "SyncRunner {0} finished"
    Ganymede.debug(ts.l("run.done", myName));
  }
}
