/*
   GASH 2

   GanymedeXMLSession.java

   The GANYMEDE object storage system.

   Created: 1 August 2000
   Release: $Name:  $
   Version: $Revision: 1.1 $
   Last Mod Date: $Date: 2000/08/09 02:22:19 $
   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999, 2000
   The University of Texas at Austin.

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
   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.

*/

package arlut.csd.ganymede;

import java.io.*;
import java.util.*;
import java.text.*;
import java.rmi.*;

import arlut.csd.Util.*;

/*------------------------------------------------------------------------------
                                                                           class
                                                              GanymedeXMLSession

------------------------------------------------------------------------------*/

/**
 * <p>This class handles operations and client interactions for the Ganymede
 * server in handling XML file loading.</p>
 */

public final class GanymedeXMLSession implements XMLSession {

  /**
   * <p>The working GanymedeSession underlying this XML session.</p>
   */

  GanymedeSession session;

  /* -- */

  public GanymedeXMLSession(GanymedeSession session)
  {
    this.session = session;
  }

  /**
   * <p>This method is called repeatedly by the XML client in order to
   * send the next packet of XML data to the server.  If the server
   * has detected any errors in the already-received XML stream,
   * xmlSubmit() may return a non-null ReturnVal with a description of
   * the failure.  Otherwise, the xmlSubmit() method will enqueue the
   * XML data for the server's continued processing and immediately
   * return a null value, indicating success.  The xmlSubmit() method
   * will only block if the server has filled up its internal buffers
   * and must wait to digest more of the already submitted XML.</p> 
   */

  public ReturnVal xmlSubmit(byte[] bytes)
  {
    return null;
  }

  /**
   * <p>This method is called by the XML client once the end of the XML
   * stream has been transmitted, whereupon the server will attempt
   * to finalize the XML transaction and return an overall success or
   * failure message in the ReturnVal.  The xmlEnd() method will block
   * until the server finishes processing all the XML data previously
   * submitted by xmlSubmit().</p>
   */

  public ReturnVal xmlEnd()
  {
    return null;
  }
}
