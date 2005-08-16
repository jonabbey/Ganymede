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
 
   Copyright (C) 1996-2005
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

import arlut.csd.ganymede.common.Invid;
import arlut.csd.ganymede.common.Query;
import arlut.csd.ganymede.common.QueryResult;
import arlut.csd.ganymede.rmi.FileTransmitter;
import arlut.csd.Util.booleanSemaphore;
import arlut.csd.Util.BigPipedInputStream;
import arlut.csd.Util.TranslationService;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.PipedOutputStream;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Vector;

import com.jclark.xml.output.UTF8XMLWriter;

/*------------------------------------------------------------------------------
                                                                           class
                                                                  XMLTransmitter

------------------------------------------------------------------------------*/

/**
 * This class is used on the server to act as a FileTransmitter, a client
 * pulling data to do an xmlclient dump can make iterative calls on this object
 * over RMI in order to receive the file.
 */

public class XMLTransmitter extends UnicastRemoteObject implements FileTransmitter {

  private static final boolean debug = false;

  /**
   * TranslationService object for handling string localization in
   * the Ganymede server.
   */

  static final TranslationService ts = TranslationService.getTranslationService("arlut.csd.ganymede.server.XMLTransmitter");

  /**
   * How big should the buffer between the XML dumping thread and
   * the getNextChunk() method in this class?  This can be up to 64k,
   * and the larger it is up to that limit, the fewer RMI calls will
   * be required to pull a large XML dump from Ganymede.
   */

  static final int bufferSize = 65536;

  // ---

  private boolean eof;
  private PipedOutputStream outpipe;
  private BigPipedInputStream inpipe;
  private boolean doSendData;
  private boolean doSendSchema;
  private String syncChannel;

  /**
   * This constructor creates the XMLTransmitter used to send XML dump
   * data from the Ganymede server down to the xmlclient.  A
   * XMLTransmitter serves as an RMI exported object that the
   * xmlclient can continually poll to download chunks of XML.
   */

  public XMLTransmitter(boolean sendData, boolean sendSchema, String syncChannel, boolean includeHistory, boolean includeOid) throws IOException, RemoteException
  {
    super();			// UnicastRemoteObject initialization

    if (debug)
      {
	System.err.println("XMLTransmitter constructed!");
      }

    outpipe = new PipedOutputStream();
    inpipe = new BigPipedInputStream(outpipe, bufferSize);
    doSendData = sendData;
    doSendSchema = sendSchema;

    // we need to get a thread to dump the XML schema in the
    // background to our pipe

    final String myFinalSyncChannel = syncChannel;
    final boolean myIncludeHistory = includeHistory;
    final boolean myIncludeOid = includeOid;

    Thread dumpThread = new Thread(new Runnable() {
	public void run() {
	  try
	    {
	      Ganymede.db.dumpXML(outpipe, doSendData, doSendSchema, myFinalSyncChannel, myIncludeHistory, myIncludeOid);
	    }
	  catch (Throwable ex)
	    {
	      // dumpXML will close outpipe on any exception,
	      // nothing we can productively do here, go
	      // ahead and show it for debug purposes
	      
	      System.err.println(ts.l("init.eof"));
	    }
	}}, ts.l("init.threadname")); // "Ganymede XMLSession Schema/Data Dump Thread"
    
    // and set it running
    
    dumpThread.start();
  }

  /**
   * This constructor creates the XMLTransmitter used to send Ganymede
   * query result data from the Ganymede server down to the xmlclient.
   * A XMLTransmitter serves as an RMI exported object that the
   * xmlclient can continually poll to download chunks of XML.
   */

  public XMLTransmitter(GanymedeSession session, String queryString, Query query, QueryResult rows) throws IOException, RemoteException
  {
    super();			// UnicastRemoteObject initialization

    if (debug)
      {
	System.err.println("XMLTransmitter[query] constructed!");
      }

    outpipe = new PipedOutputStream();
    inpipe = new BigPipedInputStream(outpipe, bufferSize);

    // we need to get a thread to dump the XML objects in the
    // background to our pipe

    final GanymedeSession mySession = session;
    final String myQueryString = queryString;
    final Query myQuery = query;
    final QueryResult myRows = rows;

    Thread dumpThread = new Thread(new Runnable() {
	public void run() {
	  XMLDumpContext xmlOut = null;

	  try
	    {
	      xmlOut = new XMLDumpContext(new UTF8XMLWriter(outpipe, UTF8XMLWriter.MINIMIZE_EMPTY_ELEMENTS),
					  myQuery);

	      xmlOut.startElement("ganymede");
	      xmlOut.attribute("major", Byte.toString(DBStore.major_xml_version));
	      xmlOut.attribute("minor", Byte.toString(DBStore.minor_xml_version));

	      xmlOut.startElementIndent("ganydata");
	      xmlOut.indentOut();

	      DBSession dbSession = mySession.getSession();

	      Vector invids = myRows.getInvids();

	      for (int i = 0; i < invids.size(); i++)
		{
		  Invid invid = (Invid) invids.elementAt(i);
		  DBObject vobj = dbSession.viewDBObject(invid);
		  vobj.emitXML(xmlOut);
		}

	      xmlOut.indentIn();
	      xmlOut.endElementIndent("ganydata");
	    
	      xmlOut.indentIn();
	      xmlOut.endElementIndent("ganymede");

	      xmlOut.write("\n");
	      xmlOut.close();
	      xmlOut = null;
	    }
	  catch (Throwable ex)
	    {
	      if (xmlOut != null)
		{
		  try
		    {
		      xmlOut.close();
		    }
		  catch (IOException ioex)
		    {
		    }
		}

	      System.err.println(ts.l("init.eof"));
	    }
	  finally
	    {
	      try
		{
		  outpipe.close();
		}
	      catch (IOException ex)
		{
		}
	    }
	}}, ts.l("init.qthreadname")); // "Ganymede XML Query Thread"
    
    // and set it running
    
    dumpThread.start();
  }

  /**
   * This method pulls down the next sequence of bytes from the
   * FileTransmitter.  This method will block if necessary until the
   * data is ready to be transmitted.
   *
   * This method returns null on end of file, and will throw an
   * exception if it is called again after null is returned.
   */

  public synchronized byte[] getNextChunk() throws RemoteException
  {
    try
      {
	if (eof)
	  {
	    return null;
	  }

	// see how much input is ready to be read from the pipe..  if
	// avail is 0, that means there's nothing to read right now,
	// but there may be, after we call and block on inpipe.read().
	// In that case, we'll go ahead and set up for a 64k block,
	// but if we get less than 64k when the blocking call actually
	// returns, we'll create a proper-sized array for transmission

	int avail = 0;

	avail = inpipe.available();

	if (debug)
	  {
	    System.err.println("getNextChunk: avail was " + avail);
	  }

	// we don't want to try to send more than 64k at once, as it's
	// hard (impossible?) to serialize an array longer than that.

	// this used to be true, anyway...

	if (avail > bufferSize || avail == 0)
	  {
	    avail = bufferSize;
	  }

	byte[] data = new byte[avail];

	int count = inpipe.read(data); // we may block waiting for the schema dump thread here

	if (debug)
	  {
	    System.err.println("getNextChunk ahoy [" + avail + "," + count + "]!");
	  }

	if (count <= 0)
	  {
	    // -1 is eof, we shouldn't actually get 0, but..

	    eof = true;
	    return null;
	  }
	else if (count != data.length)
	  {
	    // we read a smaller chunk, shrink the data down

	    byte[] chunk = new byte[count];
	    System.arraycopy(data, 0, chunk, 0, count);
	    return chunk;
	  }
	else
	  {
	    // the data is good to go as is

	    return data;
	  }
      }
    catch (IOException ex)
      {
	ex.printStackTrace();
	throw new RemoteException(ex.getMessage());
      }
    finally
      {
	if (eof)
	  {
	    try
	      {
		inpipe.close();
	      }
	    catch (IOException ex)
	      {
		ex.printStackTrace();
	      }
	  }
      }
  }

  /**
   * This method is called to notify the FileTransmitter that no
   * more of the file will be pulled.
   */
  
  public synchronized void end() throws RemoteException
  {
    eof = true;

    try
      {
	inpipe.close();
      }
    catch (IOException ex)
      {
	ex.printStackTrace();
      }
  }
}
