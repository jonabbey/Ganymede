/*
   GASH 2

   XMLTransmitter.java

   The GANYMEDE object storage system.

   Created: 16 December 2004
   Last Mod Date: $Date$
   Last Revision Changed: $Rev$
   Last Changed By: $Author$
   SVN URL: $HeadURL$

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996-2004
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
   along with this program; if not, write to the Free Software
   Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
   02111-1307, USA

*/

package arlut.csd.ganymede.server;

import arlut.csd.ganymede.rmi.FileTransmitter;
import arlut.csd.Util.booleanSemaphore;
import arlut.csd.Util.BigPipedInputStream;
import arlut.csd.Util.TranslationService;

import java.io.PipedOutputStream;
import java.rmi.RemoteException;

/*------------------------------------------------------------------------------
                                                                           class
                                                                  XMLTransmitter

------------------------------------------------------------------------------*/

/**
 * <p>This class is used on the server to act as a FileTransmitter, a client
 * pulling data to do an xmlclient dump can make iterative calls on this object
 * over RMI in order to receive the file.</p>
 */

public class XMLTransmitter implements FileTransmitter {

  /**
   * <p>TranslationService object for handling string localization in
   * the Ganymede server.</p>
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.server.XMLTransmitter");

  // ---

  private byte[] data = null;
  private int oldavail = 0;
  private int avail;
  private boolean eof;
  private PipedOutputStream outpipe;
  private BigPipedInputStream inpipe;
  private boolean doSendData;
  private boolean doSendSchema;

  public XMLTransmitter(boolean sendData, boolean sendSchema)
  {
    outpipe = new PipedOutputStream();
    inpipe = new BigPipedInputStream(outpipe);
    doSendData = sendData;
    doSendSchema = sendSchema;

    // we need to get a thread to dump the XML schema in the
    // background to our pipe

    Thread dumpThread = new Thread(new Runnable() {
	public void run() {
	  try
	    {
	      Ganymede.db.dumpXML(outpipe, doSendData, doSendSchema);
	    }
	  catch (IOException ex)
	    {
	      // dumpXML will close outpipe on any exception,
	      // nothing we can productively do here, go
	      // ahead and show it for debug purposes
	      
	      System.err.println(ts.l("init.eof"));
	    }
	}}, ts.l("init.threadname"));
    
    // and set it running
    
    dumpThread.start();
  }

  /**
   * <p>This method pulls down the next sequence of bytes from the
   * FileTransmitter.  This method will block if necessary until the
   * data is ready to be transmitted.</p>
   *
   * <p>This method returns null on end of file, and will throw an excepti.</p>
   */

  public synchronized byte[] getNextChunk() throws RemoteException
  {
    try
      {
	if (eof)
	  {
	    return null;
	  }

	avail = inpipe.available();

	if (avail > 65536)
	  {
	    avail = 65536;
	  }

	if (avail == 0)
	  {
	    avail = 1024;
	  }

	if (oldavail != avail)
	  {
	    data = new byte[avail];
	  }

	int count  = inpipe.read(data);	// we may block waiting for the schema dump thread here

	if (count == -1)
	  {
	    eof = true;
	    return null;
	  }
	else
	  {
	    return data;
	  }
      }
    finally
      {
	if (eof)
	  {
	    inpipe.close();
	  }
      }
  }

  /**
   * <p>This method is called to notify the FileTransmitter that no
   * more of the file will be pulled.</p>
   */
  
  public synchronized void end() throws RemoteException
  {
    eof = true;
    inpipe.close();
  }
}
