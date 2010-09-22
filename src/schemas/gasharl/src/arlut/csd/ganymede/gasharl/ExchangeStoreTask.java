/*

   ExchangeStoreTask.java

   This task is designed to query our Active Directory server to
   obtain a list of defined Exchange Mail Store volumes.  We use the
   results of this query to keep our Exchange Store object list up to
   date.

   Created: 22 September 2010

   Module By: Jonathan Abbey, jonabbey@arlut.utexas.edu

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996-2010
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
   along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/

package arlut.csd.ganymede.gasharl;

import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.IOException;

import java.rmi.RemoteException;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import javax.naming.Context;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchResult;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

import arlut.csd.ganymede.common.Invid;
import arlut.csd.ganymede.common.NotLoggedInException;
import arlut.csd.ganymede.common.ReturnVal;
import arlut.csd.ganymede.common.SchemaConstants;
import arlut.csd.ganymede.server.DBEditObject;
import arlut.csd.ganymede.server.DBObject;
import arlut.csd.ganymede.server.Ganymede;
import arlut.csd.ganymede.server.GanymedeServer;
import arlut.csd.ganymede.server.GanymedeSession;
import arlut.csd.ganymede.server.StringDBField;

/*------------------------------------------------------------------------------
                                                                           class
                                                               ExchangeStoreTask

------------------------------------------------------------------------------*/

/**
 * This task is designed to query our Active Directory server to
 * obtain a list of defined Exchange Mail Store volumes.  We use the
 * results of this query to keep our Exchange Store object list up to
 * date.
 *
 * @author Jonathan Abbey jonabbey@arlut.utexas.edu
 */

public class ExchangeStoreTask implements Runnable {

  static final boolean debug = true;

  static final boolean skipSSLValidation = false;
  static private Pattern homeMDBPat = Pattern.compile("CN=([^,]+)");

  /* -- */

  GanymedeSession mySession = null;

  /**
   *
   * Just Do It (tm)
   *
   * @see java.lang.Runnable
   *
   */

  public void run()
  {
    boolean transactionOpen = false;

    /* -- */

    Ganymede.debug("Exchange Store Task: Starting");

    String error = GanymedeServer.checkEnabled();
	
    if (error != null)
      {
	Ganymede.debug("Deferring Exchange Store Task - semaphore disabled: " + error);
	return;
      }

    try
      {
	try
	  {
	    mySession = new GanymedeSession("ExchangeStoreTask");
	  }
	catch (RemoteException ex)
	  {
	    Ganymede.debug("Exchange Store Task: Couldn't establish session");
	    return;
	  }

	// we don't want interactive handholding

	mySession.enableWizards(false);

	// and we want forced required fields oversight..

	mySession.enableOversight(true);
	
	ReturnVal retVal = mySession.openTransaction("Exchange Store Task");

	if (retVal != null && !retVal.didSucceed())
	  {
	    Ganymede.debug("Exchange Store Task: Couldn't open transaction");
	    return;
	  }

	transactionOpen = true;
	
	// do the stuff

	try
	  {
	    Map<String,String> map = getMailStores();
	  }
	catch (Throwable ex)
	  {
	    Ganymede.debug("Exchange Store Query / Update bailed");

	    mySession.abortTransaction();
	    return;
	  }

	retVal = mySession.commitTransaction();

	if (retVal != null && !retVal.didSucceed())
	  {
	    // if doNormalProcessing is true, the
	    // transaction was not cleared, but was
	    // left open for a re-try.  Abort it.

	    if (retVal.doNormalProcessing)
	      {
		Ganymede.debug("Exchange Store Task: couldn't fully commit, trying to abort.");

		mySession.abortTransaction();
	      }

	    Ganymede.debug("Exchange Store Task: Couldn't successfully commit transaction");
	  }
	else
	  {
	    Ganymede.debug("Exchange Store Task: Transaction committed");
	  }

	transactionOpen = false;
      }
    catch (NotLoggedInException ex)
      {
      }
    catch (Throwable ex)
      {
	Ganymede.debug("Caught " + ex.getMessage());
      }
    finally
      {
	if (transactionOpen)
	  {
	    Ganymede.debug("Exchange Store Task: Forced to terminate early, aborting transaction");
	  }

	mySession.logout();
      }
  }

  public Map<String,String> getMailStores()
  {
    Map<String,String> map = new HashMap<String,String>();
    Hashtable env = new Hashtable();
    String url = System.getProperty("gasharl.active_directory.url");
    String principal = System.getProperty("gasharl.active_directory.security_principal");
    String password = System.getProperty("gasharl.active_directory.security_password");

    if (url == null)
      {
	throw new IllegalArgumentException("Missing Active Directory url from properties file");
      }

    if (principal == null)
      {
	throw new IllegalArgumentException("Missing Active Directory security principal from properties file");
      }

    if (password == null)
      {
	throw new IllegalArgumentException("Missing Active Directory security password from properties file");
      }

    env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
    env.put(Context.SECURITY_AUTHENTICATION, "simple");
    env.put(Context.PROVIDER_URL, url);
    env.put(Context.SECURITY_PRINCIPAL, principal);
    env.put(Context.SECURITY_CREDENTIALS, password);

    if (skipSSLValidation)
      {
	env.put("java.naming.ldap.factory.socket", BlindSSLSocketFactory.class.getName());
      }

    DirContext ctx = null;

    try
      {
	ctx = new InitialDirContext(env);

	Attributes matchAttrs = new BasicAttributes();
	matchAttrs.put(new BasicAttribute("homeMDB"));
	matchAttrs.put(new BasicAttribute("msExchMailboxGuid"));

	NamingEnumeration answer = ctx.search("CN=Microsoft Exchange System Objects", matchAttrs);

	while (answer.hasMore())
	  {
	    SearchResult sr = (SearchResult) answer.next();

	    NamingEnumeration<Attribute> e = (NamingEnumeration<Attribute>)sr.getAttributes().getAll();

	    while (e.hasMore())
	      {
		Attribute a = e.next();

		if (a.getID().equals("homeMDB"))
		  {
		    String value = (String) a.get();

		    Matcher m = homeMDBPat.matcher(value);
		    
		    if (m.find())
		      {
			map.put(m.group(1), value);
		      }
		  }
	      }
	  }
      }
    catch (NamingException ex)
      {
	ex.printStackTrace();
      }
    finally
      {
	if (ctx != null)
	  {
	    try
	      {
		ctx.close();
	      }
	    catch (NamingException ex)
	      {
		ex.printStackTrace();
	      }
	  }
      }
   
    return map;
  }

  public static void main(String[] args)
  {
    if (args.length == 0)
      {
	throw new IllegalArgumentException("Missing command line argument for properties file");
      }

    FileInputStream fis = null;
    BufferedInputStream bis = null;

    try
      {
	fis = new FileInputStream(args[0]);
	bis = new BufferedInputStream(fis);
	System.getProperties().load(bis);
      }
    catch (IOException ex)
      {
	throw new IllegalArgumentException("Couldn't load properties file " + args[0]);
      }
    finally
      {
	try
	  {
	    if (bis != null)
	      {
		bis.close();
	      }
	  }
	catch (IOException ex)
	  {
	  }
      }

    ExchangeStoreTask task = new ExchangeStoreTask();

    Map<String, String> storeMap = task.getMailStores();

    for (Map.Entry<String,String> store: storeMap.entrySet())
      {
	System.out.println(store.getKey() + ":" + store.getValue());
      }
  }
}
