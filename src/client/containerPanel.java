/*

   containerPanel.java

   This is the container for all the information in a field.  Used in window Panels.

   Created:  11 August 1997
   Release: $Name:  $
   Version: $Revision: 1.123 $
   Last Mod Date: $Date: 2001/06/29 07:58:42 $
   Module By: Michael Mulvaney

   -----------------------------------------------------------------------
	    
   Ganymede Directory Management System
 
   Copyright (C) 1996, 1997, 1998, 1999, 2000, 2001
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
   Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
   02111-1307, USA

*/

package arlut.csd.ganymede.client;

import javax.swing.*;
import javax.swing.event.*;

import java.io.*;
import java.awt.*;
import java.beans.*;
import java.awt.event.*;
import java.rmi.*;
import java.util.*;

import arlut.csd.ganymede.*;

import arlut.csd.JDataComponent.*;
import arlut.csd.JDialog.*;
import arlut.csd.Util.VecSortInsert;

/*------------------------------------------------------------------------------
                                                                           class
                                                                  containerPanel

------------------------------------------------------------------------------*/

/**
 * <p>One of the basic building blocks of the ganymede client, a containerPanel is
 * a GUI panel which allows the user to view and/or edit all the custom fields
 * for an object in the Ganymede database.</p>
 *
 * <p>Each containerPanel displays a single
 * {@link arlut.csd.ganymede.db_object db_object}, and allows the
 * user to edit or view each {@link arlut.csd.ganymede.db_field db_field} in the
 * object.  On loading, containerPanel loops through the fields of the object, adding the
 * appropriate type of input for each field.  This includes text fields,
 * number fields, boolean fields, and string selector fields(fields that can have
 * multiple values).</p>
 *
 * <p>containerPanel handles the connection between GUI components and server
 * fields, translating GUI activity to attempted changes to server fields.  Any
 * attempted change that the server refuses will cause a dialog to be popped up via
 * gclient's
 * {@link arlut.csd.ganymede.client.gclient#handleReturnVal(arlut.csd.ganymede.ReturnVal) handleReturnVal()}
 * method, and the GUI component that caused the change will be reverted to its pre-change
 * status.</p>
 *
 * <p>The gclient's handleReturnVal() method also supports extracting a list of objects
 * and fields that need to be refreshed when one change on the server is reflected
 * across more than one object.  This is handled by containerPanel's
 * {@link arlut.csd.ganymede.client.containerPanel#update(java.util.Vector) update()}
 * method.</p>
 *
 * @version $Revision: 1.123 $ $Date: 2001/06/29 07:58:42 $ $Name:  $
 * @author Mike Mulvaney
 */

public class containerPanel extends JPanel implements ActionListener, JsetValueCallback, ItemListener {

  boolean debug = false;
  static final boolean debug_persona = false;
  static final int FIELDWIDTH = 25;

  // ---

  /**
   * Used to tell the load() method to stop populating a panel if the window
   * we are in is closed.
   */
  
  private boolean keepLoading = true;

  /**
   * Reference to the client's main class, used for some utility functions.
   */

  gclient gc;

  /**
   * Remote reference to the server-side object we are viewing or editing.
   */

  private db_object object;

  /**
   * Database id for the object we are viewing or editing
   */

  private Invid invid;

  /**
   * Reference to the desktop pane containing the client's internal windows.  Used to access
   * some GUI resources.
   */

  windowPanel winP;

  /**
   * The window we are contained in, may be null if we are embedded in a 
   * {@link arlut.csd.ganymede.client.vectorPanel vectorPanel}.
   */

  protected framePanel frame;

  /**
   * <p>Vector of Short field id's used to track fields for which
   * we receive update requests while we are still loading.  After
   * we finish loading this panel, we'll go back and refresh any fields
   * whose field id's are listed in this vector.</p>
   */

  Vector updatesWhileLoading = new Vector();

  /**
   * <p>Vector used to list vectorPanels embedded in this object window.  This
   * variable is used by
   * {@link arlut.csd.ganymede.client.vectorPanel#expandAllLevels() vectorPanel.expandAllLevels()}
   * to do recursive expansion of embedded objects.</p>
   */

  Vector vectorPanelList = new Vector();

  /**
   * <p>To help avoid recursive problems, we keep track of any arlut.csd.JDataComponent
   * GUI components that are currently having their change notification messages
   * handled, and refuse to try to refresh them reentrantly.</p>
   */

  JComponent currentlyChangingComponent = null;

  /**
   * <p>Hashtable mapping Short field id's to the AWT/Swing GUI component managing
   * that database field.</p>
   */

  Hashtable shortToComponentHash = new Hashtable();

  /**
   * <p>Hashtable mapping the active GUI components to their labels.  This
   * is used so that when the containerPanel update() method decides to hide
   * a field, the associated label can be hid too.</p>
   */

  Hashtable rowHash = new Hashtable();

  /**
   * <p>Hashtable mapping GUI components to their associated
   * {@link arlut.csd.ganymede.db_field db_field}'s.
   */

  Hashtable objectHash = new Hashtable();

  /**
   * <p>Hashtable mapping the combo boxes contained within
   * {@link arlut.csd.ganymede.client.JInvidChooser JInvidChooser}
   * GUI components to their associated
   * {@link arlut.csd.ganymede.db_field db_field}'s.</p>
   *
   * <p>This is required because while we want to hide or reveal the JInvidChooser
   * as a whole, we'll get itemStateChanged() calls from the combo
   * box within the JInvidChooser.</p>
   */

  Hashtable invidChooserHash = new Hashtable();

  /**
   * <p>Vector of {@link arlut.csd.ganymede.FieldInfo FieldInfo} objects
   * holding the values for fields in this object.  Used during loading
   * and update.</p>
   */

  Vector infoVector = null;

  /**
   * <p>Vector of {@link arlut.csd.ganymede.FieldTemplate FieldTemplate}
   * objects holding the constant field type information for fields in
   * this object.</p>
   */

  Vector templates = null;

  boolean
    isCreating,
    editable;

  boolean
    isEmbedded,
    loading = false,
    loaded = false;

  /**
   * If progressBar is not null, the load() method for containerPanel will
   * update this progressBar as the panel is loaded from the server.
   */

  JProgressBar
    progressBar;

  int
    vectorElementsAdded = 0;

  /**
   * Object type id for this object.. should be equal to invid.getType().
   */

  short type;

  /**
   * <p>If true, this containerPanel is being displayed in a persona pane in
   * a frame panel, and we'll hide the associated user field, which is implicit
   * when we embedded a persona panel in a framePanel showing a user object.</p>
   *
   * <p>This is a dirty hack to make the client a little extra smart about one
   * particular kind of mandatory Ganymede server object.</p>
   */

  boolean isPersonaPanel = false;

  GridBagLayout
    gbl = new GridBagLayout();
  
  GridBagConstraints
    gbc = new GridBagConstraints();

  /* -- */

  /**
   * <p>Constructor with default values for progressBar set to false, loadNow
   * set to true, and isCreating set to false.</p>
   *
   * @param object   The object to be displayed
   * @param editable If true, the fields presented will be enabled for editing
   * @param parent   Parent gclient of this container
   * @param window   windowPanel containing this containerPanel
   * @param frame    framePanel holding this containerPanel(although this cp is not necessarily in the "General" tab)
   * @param context An object that can be provided to identify the context in
   * which this containerPanel is being created.
   */

  public containerPanel(db_object    object,
			boolean      editable, 
			gclient      gc,
			windowPanel  window,
			framePanel   frame,
			Object context)
  {
    this(object, editable, gc, window, frame, null, true, context);
  }

  /** 
   * <p>Constructor with default values for loadNow set to true, and
   * isCreating set to false.</p>
   *
   * <p>The &lt;progressBar&gt; parameter is used so that
   * containerpanel can increment an external JProgressBar as
   * information on the fields for this object are loaded from
   * the server.  progressBar should be null if this containerPanel
   * is not serving as the main panel for a framePanel.</p>
   *
   * @param object   The object to be displayed
   * @param editable If true, the fields presented will be enabled for editing
   * @param parent   Parent gclient of this container
   * @param window   windowPanel containing this containerPanel
   * @param frame    framePanel holding this containerPanel
   * @param progressBar JProgressBar to be updated, can be null
   * @param context An object that can be provided to identify the context in
   * which this containerPanel is being created.  
   */

  public containerPanel(db_object object, 
			boolean editable, 
			gclient gc, 
			windowPanel window, 
			framePanel frame, 
			JProgressBar progressBar,
			Object context)
  {
    this(object, editable, gc, window, frame, progressBar, true, context);
  }

  /**
   * <p>Constructor with default value for isCreating set to false.</p>
   *
   * <p>The &lt;progressBar&gt; parameter is used so that
   * containerpanel can increment an external JProgressBar as
   * information on the fields for this object are loaded from
   * the server.</p>
   *
   * @param object   The object to be displayed
   * @param editable If true, the fields presented will be enabled for editing
   * @param parent   Parent gclient of this container
   * @param window   windowPanel containing this containerPanel
   * @param progressBar JProgressBar to be updated, can be null
   * @param loadNow  If true, container panel will be loaded immediately
   * @param context An object that can be provided to identify the context in
   * which this containerPanel is being created.
   */

  public containerPanel(db_object object,
			boolean editable,
			gclient gc,
			windowPanel window,
			framePanel frame,
			JProgressBar progressBar,
			boolean loadNow,
			Object context)
  {
    this(object, editable, gc, window, frame, progressBar, loadNow, false, context);
  }

  /**
   * <p>Primary constructor for containerPanel</p>
   *
   * <p>The &lt;progressBar&gt; parameter is used so that
   * containerpanel can increment an external JProgressBar as
   * information on the fields for this object are loaded from
   * the server.  progressBar should be null if this containerPanel
   * is not serving as the main panel for a framePanel.</p>
   *
   * @param object   The object to be displayed
   * @param editable If true, the fields presented will be enabled for editing
   * @param parent   Parent gclient of this container
   * @param window   windowPanel containing this containerPanel
   * @param progressBar JProgressBar to be updated, can be null
   * @param loadNow  If true, container panel will be loaded immediately
   * @param isCreating  
   * @param context An object that can be provided to identify the context in
   * which this containerPanel is being created.  
   */

  public containerPanel(db_object object,
			boolean editable,
			gclient gc,
			windowPanel window,
			framePanel frame,
			JProgressBar progressBar,
			boolean loadNow,
			boolean isCreating,
			Object context)
  {
    super(false);

    /* -- */

    this.gc = gc;

    if (!debug)
      {
	debug = gc.debug;
      }

    if (object == null)
      {
	throw new NullPointerException("null object passed to containerPanel constructor");
      }

    this.winP = window;
    this.object = object;
    this.editable = editable;
    this.frame = frame;
    this.progressBar = progressBar;
    this.isCreating = isCreating;

    if (context != null && (context instanceof personaContainer))
      {
	this.isPersonaPanel = true;
      }

    frame.addContainerPanel(this);

    // initialize layout

    setLayout(gbl);

    gbc.anchor = GridBagConstraints.NORTHWEST;
    gbc.insets = new Insets(4,4,4,4);

    if (loadNow)
      {
	load();
      }
  }

  /**
   * Downloads all necessary information from the server
   * about the object being viewed or edited.  Typically this is called
   * when the containerPanel is initialized by the containerPanel
   * constructor, but we defer loading when we are placed in a vector
   * panel hierarchy.
   */
  
  public void load() 
  {
    loading = true;

    int infoSize;

    FieldInfo 
      fieldInfo = null;

    FieldTemplate
      fieldTemplate = null;

    short ID;

    /* -- */

    if (loaded)
      {
	printErr("Container panel is already loaded!");
	return;
      }

    if (debug)
      {
	println("Loading container panel");
      }
    
    try
      {
	// if we are a top-level container panel in a general pane
	// or persona pane, we'll have a progress bar.. we'll want
	// to update it as we go along loading field information.

	if (progressBar != null)
	  {
	    progressBar.setMinimum(0);
	    progressBar.setMaximum(20);
	    progressBar.setValue(0);
	  }

	// Get the list of fields

	if (debug)
	  {
	    println("Getting list of fields");
	  }
    
	try
	  {
	    type = object.getTypeID();
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Could not get the object's type id: " + rx);
	  }

	setProgressBar(1);

	// pull static field type information from the client's caches

	templates = gc.getTemplateVector(type);

	if (templates == null || templates.size() == 0)
	  {
	    System.err.println("No fields defined for this object type.. ??");

	    if (templates == null)
	      {
		System.err.println("templates is *null*");
	      }
	    else
	      {
		System.err.println("templates is empty");
	      }

	    setStatus("No fields defined for this object type.. error.");
	    return;
	  }

	setProgressBar(2);

	// ok, got the list of field definitions.  Now we need to
	// get the current values and visibility information for
	// the fields in this object.

	try
	  {
	    infoVector = object.getFieldInfoVector();
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Could not get FieldInfoVector: " + rx);
	  }

	if (infoVector.size() == 0)
	  {
	    System.err.println("No field info in getFieldInfoVector()");
	  }

	// keep a copy of the infoVector size so we don't have to
	// continously call the synchronized infoVector.size()
	// method during our loops

	infoSize = infoVector.size();
			
	// now we know how many fields are actually present in this
	// object, we can set the max size of the progress bar (plus
	// how many elements in each vector panel.)

	if (progressBar != null)
	  {
	    int totalSize = infoSize + 2;

	    for (int i = 0; i < infoSize; i++)
	      {
		FieldInfo info = (FieldInfo)infoVector.elementAt(i);
		FieldTemplate template = findtemplate(info.getID());

		if (template.isArray())
		  {
		    if ((template.getType() == FieldType.INVID) && template.isEditInPlace())
		      {
			totalSize += ((Vector)info.getValue()).size();
		      }
		  }
	      }

	    progressBar.setMaximum(totalSize);
	    progressBar.setValue(3);	   
	  }

	if (debug)
	  {
	    println("Entering big loop");
	  }
      
	for (int i = 0; i < infoSize; i++)
	  {
	    // let the gclient interrupt us

	    if (!keepLoading())
	      {
		break;
	      }

	    setProgressBar(i + 3 + vectorElementsAdded);
		
	    try
	      {
		fieldInfo = (FieldInfo) infoVector.elementAt(i);
		ID = fieldInfo.getID();
		fieldTemplate = findtemplate(ID);
		
		if (fieldTemplate == null)
		  {
		    throw new RuntimeException("Could not find the template for this field: " + 
					       fieldInfo.getField());
		  }

		// Skip some fields.  custom panels hold the built ins, and a few others.

		// If we are a persona panel, hide the associated user field.
		    
		if (((type== SchemaConstants.OwnerBase) && 
		     (ID == SchemaConstants.OwnerObjectsOwned))
		    || (ID == SchemaConstants.BackLinksField)
		    || ((type == SchemaConstants.UserBase) && 
			(ID == SchemaConstants.UserAdminPersonae))
		    || ((ID == SchemaConstants.ContainerField) && 
			object.isEmbedded())
		    || (isPersonaPanel && 
			(type == SchemaConstants.PersonaBase) &&
			(ID == SchemaConstants.PersonaAssocUser)))
		  {
		    if (debug)
		      {
			println("Skipping a special field: " + fieldTemplate.getName());
		      }

		    continue;
		  }

		// and do the work.

		addFieldComponent(fieldInfo.getField(), fieldInfo, fieldTemplate);
	      }
	    catch (RemoteException ex)
	      {
		throw new RuntimeException("caught remote exception adding field " + ex);
	      }
	  }
    
	if (debug)
	  {
	    println("Done with loop");
	  }

	setStatus("Finished loading containerPanel");
      }
    finally
      {
	loaded = true;
	loading = false;

	if (!keepLoading)
	  {
	    return;
	  }
	
	// If update(Vector) was called during the load, then any
	// fields to be updated were added to the updatesWhileLoading
	// vector.  So call update with that vector now, if it has any
	// size.
	
	synchronized (updatesWhileLoading)
	  {
	    if (updatesWhileLoading.size() > 0)
	      {
		if (debug)
		  {
		    println("Calling update with the updatesWhileLoading vector.");
		  }
		
		update(updatesWhileLoading);
	      }
	  }
      }
  }

  /** 
   * This method is reponsible for cleaning up after this
   * containerPanel when it is shut down, particularly to close any
   * auxiliary windows attached to GUI components contained in this
   * panel.  
   */

  public synchronized void unregister()
  {
    Enumeration enum = shortToComponentHash.elements();

    while (enum.hasMoreElements())
      {
	Object x = enum.nextElement();

	if (x instanceof JdateField)
	  {
	    JdateField df = (JdateField) x;

	    df.unregister();
	  }
      }
  }

  /**
   * Helper method to keep the load() method clean.
   */

  private final void setProgressBar(int count)
  {
    if (progressBar != null)
      {
	progressBar.setValue(count);
      }
  }

  /**
   * Helper method to keep the load() method clean.
   */

  private final FieldTemplate findtemplate(short type)
  {
    FieldTemplate result;
    int tsize;

    /* -- */

    tsize = templates.size();

    for (int i = 0; i < tsize; i++)
      {
	result = (FieldTemplate) templates.elementAt(i);

	if (result.getID() == type)
	  {
	    return result;
	  }
      }

    return null;
  }

  /**
   * This is a convenience method for other client classes to access
   * our gclient reference.
   */

  public final gclient getgclient()
  {
    return gc;
  }

  /** 
   * Use this to print stuff out, so we know it is from the containerPanel
   */

  private final void println(String s)
  {
    System.out.println("containerPanel: " + s);
  }

  private final void printErr(String s)
  {
    System.err.println("containerPanel err: " + s);
  }

  /**
   * Get the object contained in this containerPanel.
   */

  public  db_object getObject()
  {
    return object;
  }

  /**
   * Get the invid for the object in this containerPanel.
   */

  public Invid getObjectInvid()
  {
    if (invid == null)
      {
	try
	  {
	    invid = object.getInvid();
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Could not get the object's Invid in containerPanel: " + rx);
	  }
      }

    return invid;
  }

  /**
   * This method returns true if this containerPanel has already
   * been loaded.
   */

  public boolean isLoaded()
  {
    return loaded;
  }

  /**
   * <p>This method allows the gclient that contains us to set a flag
   * that will interrupt the load() method.</p>
   *
   * <p>Note that this method must not be synchronized.</p>
   */

  public void stopLoading()
  {
    keepLoading = false;
  }

  /**
   * This method returns false when the containerPanel loading has
   * been interupted.  The vectorPanel checks this.  
   */

  public boolean keepLoading()
  {
    return keepLoading;
  }

  /** 
   * <p>Goes through all the components and checks to see if they should
   * be visible, and updates their contents.</p>
   *
   * <p>If this containerPanel is attached to an object that is not
   * being edited, this method will return without doing anything.</p>
   */

  public void updateAll()
  {
    // View windows can't be updated.

    if (!editable)
      {
	return;
      }

    Enumeration enum;

    /* -- */

    if (debug)
      {
	println("Updating container panel");
      }

    gc.setWaitCursor();

    try
      {
	enum = objectHash.keys();
	
	while (enum.hasMoreElements())
	  {
	    updateComponent((Component)enum.nextElement());
	  }
	
	invalidate();
	frame.validate();
	
      }
    finally
      {
	if (debug)
	  {
	    println("Done updating container panel");
	  }
	
	gc.setNormalCursor();
      }
  }

  /**
   * <p>Updates a subset of the fields in this containerPanel.</p>
   *
   * @param fields Vector of Shorts, field ID's
   */

  public void update(Vector fields)
  {
    if (!editable)
      {
	return;
      }

    if (fields == null)
      {
	return;
      }

    Component c;

    /* -- */

    if (debug)
      {
	println("Updating a few fields...");
      }

    // If the containerPanel is not loaded, then we need to keep track
    // of all the fields that need to be updated, and call update on
    // them after the load is finished.

    synchronized(updatesWhileLoading)
      {
	if (!loaded)
	  {
	    // If we are not loading yet, then we don't need to worry
	    // about keeping track of the fields.  They will current when
	    // they are first loaded.
	    
	    if (loading)
	      {
		for (int i = 0; i < fields.size(); i++)
		  {
		    updatesWhileLoading.addElement(fields.elementAt(i));
		  }
	      }
	    
	    return;
	  }
      }

    gc.setWaitCursor();

    try
      {
	for (int i = 0; i < fields.size(); i++)
	  {
	    Short fieldID = (Short) fields.elementAt(i);

	    // if we're not an embedded container panel, check to see if
	    // we should update either the expiration field or the removal
	    // field.

	    if (frame != null)
	      {
		if (fieldID.shortValue() == SchemaConstants.ExpirationField)
		  {
		    frame.refresh_expiration_date_panel();
		    continue;
		  }
		else if (fieldID.shortValue() == SchemaConstants.RemovalField)
		  {
		    frame.refresh_removal_date_panel();
		    continue;
		  }
	      }

	    c = (Component) shortToComponentHash.get(fieldID);

	    if (c == null)
	      {
		if (debug)
		  {
		    println("Could not find this component: ID = " + (Short)fields.elementAt(i));
		    println("There are " + infoVector.size() + " things in the info vector.");
		    println("There are " + rowHash.size() + " things in the row hash.");
		    println("Working on number " + i + " in the fields vector.");
		    println("Valid ids: ");
		
		    Enumeration k = shortToComponentHash.keys();
		
		    while (k.hasMoreElements())
		      {
			Object next = k.nextElement();
			println("   " + next);
		      }
		  }
	      }
	    else 
	      {
		// to avoid refreshing an arlut.csd.JDataComponent
		// field while that field is waiting for its
		// setValuePerformed() call to be answered, we won't
		// force an update of a component that is still trying
		// to react to a user's manipulation.

		if (!c.equals(currentlyChangingComponent))
		  {
		    updateComponent(c);
		  }
	      }
	  }

	invalidate();
	frame.validate();

	if (debug)
	  {
	    println("Done updating container panel");
	  }
      }
    finally
      {
	gc.setNormalCursor();
      }
  }

  /**
   * <p>Updates the contents and visibility status of
   * a component in this containerPanel.</p>
   *
   * @param comp An AWT/Swing component that we need to refresh
   */

  private void updateComponent(Component comp)
  {
    if (debug)
      {
	System.err.println("containerPanel.updateComponent(" + comp + ")");
      }

    try
      {
	db_field field = (db_field) objectHash.get(comp);

	// by getting a FieldInfo, we'll save a call to the
	// server

	FieldInfo currentInfo = field.getFieldInfo();

	if (debug)
	  {
	    println("Updating " + field.getName() + " " + comp); 
	  }
	
	if (field == null)
	  {
	    println("-----Field is null, skipping.");
	    return;
	  }

	// if the field is not visible, just hide it and 
	// return.. otherwise, set it visible and update
	// the value and choices for the field

	if (!currentInfo.isVisible())
	  {
	    setRowVisible(comp, false);
	    return;
	  }
	
	setRowVisible(comp, true);

	if (comp instanceof JstringField)
	  {
	    // we don't need to worry about turning off callbacks
	    // here because JstringField only sends callbacks on
	    // focus loss

	    ((JstringField)comp).setText((String)currentInfo.getValue());
	  }
	else if (comp instanceof JstringArea)
	  {
	    ((JstringArea)comp).setText((String)currentInfo.getValue());
	  }
	else if (comp instanceof JdateField)
	  {
	    // we don't need to worry about turning off callbacks
	    // here because JdateField only sends callbacks on
	    // focus loss

	    ((JdateField)comp).setDate((Date)currentInfo.getValue());
	  }
	else if (comp instanceof JnumberField)
	  {
	    Integer value = (Integer)currentInfo.getValue();

	    // we don't need to worry about turning off callbacks
	    // here because JnumberField only sends callbacks on
	    // focus loss

	    ((JnumberField)comp).setValue(value);
	  }
 	else if (comp instanceof JfloatField)
 	  {
 	    Double value = (Double)currentInfo.getValue();
 
 	    // we don't need to worry about turning off callbacks
 	    // here because JfloatField only sends callbacks on
 	    // focus loss
 
 	    ((JfloatField)comp).setValue(value);
 	  }
	else if (comp instanceof JCheckBox)
	  {
	    Boolean value = (Boolean)currentInfo.getValue();
	    JCheckBox cb = (JCheckBox) comp;

	    // make sure we don't trigger a callback here

	    cb.removeActionListener(this);
	    cb.setSelected((value == null) ? false : value.booleanValue());
	    cb.addActionListener(this);
	  }
	else if (comp instanceof JComboBox)
	  {
	    JComboBox cb = (JComboBox) comp;
	    string_field sf = (string_field) field;

	    /* -- */

	    // remove this as an item listener so we don't get tricked
	    // into thinking this update came from the user

	    cb.removeItemListener(this);

	    if (debug)
	      {
		println("Updating the combo box.");
	      }

	    // First we need to rebuild the list of choices

	    Vector labels = null;
	    Object key = sf.choicesKey();

	    // if our choices key is null, we're not going to use a cached copy..
	    // pull down a new list of choices for this field.

	    if (key == null)
	      {
		QueryResult qr = sf.choices();

		if (qr != null)
		  {
		    labels = qr.getLabels();
		  }
	      }
	    else
	      {
		if (debug)
		  {
		    println("key = " + key);
		  }
		  
		if (gc.cachedLists.containsList(key))
		  {
		    if (debug)
		      {
			println("key in there, using cached list");
		      }
		      
		    labels = gc.cachedLists.getLabels(key, false);
		  }
		else
		  {
		    if (debug)
		      {
			println("JComboBox contents not cached, downloading stringfield choices.");
		      }
		      
		    QueryResult choicesV = sf.choices();

		    // if we got a null result, assume we have no choices,
		    // otherwise we're going to cache this result
		      
		    if (choicesV == null)
		      {
			labels = new Vector();
		      }
		    else
		      {
			gc.cachedLists.putList(key, choicesV);
			labels = choicesV.getLabels();
		      }
		  }
	      }

	    // reset the combo box.

	    boolean mustChoose = sf.mustChoose();

	    String currentValue = (String) sf.getValue();

	    if (!mustChoose || currentValue == null)
	      {
		labels.addElement("<none>");
	      }

	    if (currentValue == null)
	      {
		currentValue = "<none>";
	      }

	    // create a new model to avoid O(n^2) order time hassles when
	    // we add items one-by-one to an extant JComboBox

	    cb.setModel(new DefaultComboBoxModel(labels));
	    cb.setSelectedItem(currentValue);

	    if (debug)
	      {
		System.err.println("setting currentvalue in JComboBox to " + currentValue);
	      }

	    // put us back on as an item listener so we are live for updates
	    // from the user again

	    cb.repaint();
	    cb.addItemListener(this);
	  }
	else if (comp instanceof JInvidChooser)
	  {
	    JInvidChooser chooser = (JInvidChooser) comp;
	    invid_field invf = (invid_field) field;
	    listHandle noneHandle = new listHandle("<none>", null);
	    boolean mustChoose;

	    /* -- */

	    // remove this as an item listener so we don't get tricked
	    // into thinking this update came from the user

	    chooser.removeItemListener(this);

	    if (debug)
	      {
		println("Updating the InvidChooser.");
	      }
	      
	    // First we need to rebuild the list of choices

	    Vector choiceHandles = null;
	    Object key = invf.choicesKey();

	    // if our choices key is null, we're not going to use a cached copy..
	    // pull down a new list of choices for this field.
	      
	    if (key == null)
	      {
		if (debug)
		  {
		    println("key is null, getting new copy, not caching.");
		  }
		
		QueryResult qr = invf.choices();

		if (qr != null)
		  {
		    choiceHandles = qr.getListHandles(); // pre-sorted
		  }
	      }
	    else
	      {
		if (debug)
		  {
		    println("key = " + key);
		  }
		  
		if (gc.cachedLists.containsList(key))
		  {
		    if (debug)
		      {
			println("key in there, using cached list");
		      }
		      
		    choiceHandles = gc.cachedLists.getListHandles(key, false); // pre-sorted
		  }
		else
		  {
		    if (debug)
		      {
			println("JInvidChooser contents not cached, downloading invid field choices.");
		      }
		      
		    QueryResult choicesV = invf.choices();

		    // if we got a null result, assume we have no choices
		    // otherwise, we're going to cache this result
		      
		    if (choicesV == null)
		      {
			choiceHandles = new Vector();
		      }
		    else
		      {
			gc.cachedLists.putList(key, choicesV);
			choiceHandles = choicesV.getListHandles(); // sorted
		      }
		  }
	      }

	    // reset the combo box.

	    Invid currentValue = (Invid) invf.getValue();
	    String currentLabel = invf.getValueString();
	    listHandle currentValueHandle = new listHandle(currentLabel, currentValue);
	    listHandle currentHandle = null;

	    if (debug)
	      {
		System.err.println("containerPanel.updateComponent(): updating invid chooser combo box");
		System.err.println("containerPanel.updateComponent(): searching for value " + currentValue);
	      }

	    for (int i = 0; i < choiceHandles.size(); i++)
	      {
		currentHandle = (listHandle) choiceHandles.elementAt(i);

		if (currentHandle.getObject().equals(currentValue))
		  {
		    break;
		  }
		else
		  {
		    currentHandle = null;
		  }
	      }

	    // in many cases, the list of choices may not include the
	    // current value held in an invid field.  In this case,
	    // we need to synthesize a handle for the current value

	    if (currentHandle == null && currentValue != null)
	      {
		VecSortInsert inserter = new VecSortInsert(new arlut.csd.Util.Compare() 
							   {
							     public int compare(Object o_a, Object o_b) 
							       {
								 listHandle a, b;
	  
								 a = (listHandle) o_a;
								 b = (listHandle) o_b;
								 int compResult = 0;
	  
								 compResult = a.toString().compareTo(b.toString());
	  
								 if (compResult < 0)
								   {
								     return -1;
								   }
								 else if (compResult > 0)
								   { 
								     return 1;
								   } 
								 else
								   { 
								     return 0;
								   }
							       }
							   });

		inserter.insert(choiceHandles, currentValueHandle);
		currentHandle = currentValueHandle;
	      }

	    // now we need to decide whether we should allow the user
	    // to set this field back to the <none> selection.

	    mustChoose = invf.mustChoose();

	    if (!mustChoose || (currentHandle == null))
	      {
		choiceHandles.insertElementAt(noneHandle, 0);
	      }

	    if (debug)
	      {
		System.err.println("containerPanel.updateComponent(): got handles, setting model");
	      }

	    // aaaand resort

	    //	    choiceHandles = gc.sortListHandleVector(choiceHandles);

	    if (currentHandle == null)
	      {
		chooser.setVectorContents(choiceHandles, noneHandle);
	      }
	    else
	      {
		chooser.setVectorContents(choiceHandles, currentHandle);
	      }

	    if (debug)
	      {
		System.err.println("setting currentvalue in JInvidChooser to " + currentHandle);
	      }

	    // put us back on as an item listener so we are live for updates
	    // from the user again

	    chooser.repaint();
	    chooser.addItemListener(this);
	  }
	else if (comp instanceof JLabel)
	  {
	    ((JLabel)comp).setText((String)currentInfo.getValue());
	  }
	else if (comp instanceof JButton)
	  {
	    // This is an invid field, non-editable.
	    Invid inv = (Invid)currentInfo.getValue();
	    ((JButton)comp).setText(gc.getSession().viewObjectLabel(inv));
	  }
	else if (comp instanceof JpassField)
	  {
	    if (debug)
	      {
		println("Passfield, ingnoring");
	      }
	  }
	else if (comp instanceof StringSelector)
	  {
	    if (field instanceof invid_field)
	      {
		updateInvidStringSelector((StringSelector)comp, (invid_field)field);
	      }
	    else // must be a string_field
	      {
		updateStringStringSelector((StringSelector)comp, (string_field)field,
					   currentInfo);
	      }
	  }
	else if (comp instanceof vectorPanel)
	  {
	    ((vectorPanel)comp).refresh();
	  }
	else if (comp instanceof JIPField)
	  {
	    if (debug)
	      {
		println("Updating JIPField.");
	      }

	    ((JIPField)comp).setValue((Byte[]) currentInfo.getValue());
	  }
	else 
	  {
	    printErr("field of unknown type: " + comp);
	  }
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not check visibility in updateComponent: " + rx);
      }
  }

  /**
   * <p>Updates the contents of a vector {@link arlut.csd.ganymede.string_field string_field}
   * value selector against the current contents of the field on the server.</p>
   *
   * @param ss The StringSelector GUI component being updated
   * @param field The server-side string_field attached to the StringSelector to be updated
   * @param currentInfo A download of the string_field's current value
   */

  public void updateStringStringSelector(StringSelector ss, string_field field,
					 FieldInfo currentInfo) throws RemoteException
  {
    Vector available = null;
    Vector chosen = null;
    Object key = null;

    /* -- */

    // If the field is not editable, there will be no available vector

    if (ss.isEditable())
      {
	key = field.choicesKey();
	
	if (key == null)
	  {
	    QueryResult qr = field.choices();

	    if (qr != null)
	      {
		available = qr.getListHandles();
	      }
	  }
	else
	  {
	    if (gc.cachedLists.containsList(key))
	      {
		if (debug)
		  {
		    println("key in there, using cached list");
		  }
	    
		available = gc.cachedLists.getListHandles(key, false);
	      }
	    else
	      {
		if (debug)
		  {
		    println("list for updateStringStringSelector() not loaded, downloading a new one.");
		  }
	    
		QueryResult choicesV = field.choices();
	    
		// if we got a null result, assume we have no choices
		// otherwise, we're going to cache this result
	    
		if (choicesV == null)
		  {
		    available = new Vector();
		  }
		else
		  {
		    gc.cachedLists.putList(key, choicesV);
		    available = choicesV.getListHandles();
		  }
	      }
	  }
      }

    // now find the chosen vector

    chosen = (Vector) currentInfo.getValue();

    ss.update(available, true, chosen, false);
  }

  /**
   * <p>Updates the contents of a vector {@link arlut.csd.ganymede.invid_field invid_field}
   * value selector against the current contents of the field on the server.</p>
   *
   * @param ss The StringSelector GUI component being updated
   * @param field The server-side invid_field attached to the StringSelector to be updated
   */

  public void updateInvidStringSelector(StringSelector ss, invid_field field) throws RemoteException
  {
    Vector available = null;
    Vector chosen = null;
    Object key = null;

    /* -- */

    // Only editable fields have available vectors

    if (ss.isEditable())
      {
	key = field.choicesKey();
    
	if (key == null)
	  {
	    QueryResult qr = field.choices();

	    if (qr != null)
	      {
		available = qr.getListHandles();
	      }
	  }
	else
	  {
	    if (gc.cachedLists.containsList(key))
	      {
		if (debug)
		  {
		    println("key in there, using cached list");
		  }
	    
		available = gc.cachedLists.getListHandles(key, false);
	      }
	    else
	      {
		if (debug)
		  {
		    println("list for updateInvidStringSelector() not loaded, downloading a new one.");
		  }
	    
		QueryResult choicesV = field.choices();
	    
		// if we got a null result, assume we have no choices
		// otherwise, we're going to cache this result
	    
		if (choicesV == null)
		  {
		    available = new Vector();
		  }
		else
		  {
		    gc.cachedLists.putList(key, choicesV);
		    available = choicesV.getListHandles();
		  }
	      }
	  }
      }
    
    QueryResult res = field.encodedValues();

    if (res != null)
      {
	chosen = res.getListHandles();
      }

    try
      {
	ss.update(available, true, chosen, false);
      }
    catch (Exception e)
      {
	println("Caught exception updating StringSelector: " + e);
      }
  }

  /**
   * <p>This method comprises the JsetValueCallback interface, and is how
   * the customized data-carrying components in this containerPanel
   * notify us when something changes.</p>
   *
   * <p>Note that we don't use this method for checkboxes, or comboboxes.</p>
   *
   * @see arlut.csd.JDataComponent.JsetValueCallback
   * @see arlut.csd.JDataComponent.JValueObject
   *
   * @return false if the JDataComponent that is calling us should
   * reject the value change operation and revert back to the prior
   * value.
   */

  public boolean setValuePerformed(JValueObject v)
  {
    ReturnVal returnValue = null;

    /* -- */

    if (v.getOperationType() == JValueObject.ERROR)
      {
	gc.showErrorMessage((String)v.getValue());
	return true;
      }

    currentlyChangingComponent = (JComponent)v.getSource();

    try
      {
	// ok, now we have to connect the field change report coming
	// from the JDataComponent to the appropriate field object
	// on the Ganymede server.  First we'll try the simplest,
	// generic case.

	if ((v.getSource() instanceof JstringField) ||
	    (v.getSource() instanceof JnumberField) ||
 	    (v.getSource() instanceof JfloatField) ||
	    (v.getSource() instanceof JIPField) ||
	    (v.getSource() instanceof JdateField) ||
	    (v.getSource() instanceof JstringArea))
	  {
	    db_field field = (db_field) objectHash.get(v.getSource());

	    /* -- */

	    try
	      {
		if (debug)
		  {
		    println(field.getTypeDesc() + " trying to set to " + v.getValue());
		  }

		returnValue = field.setValue(v.getValue());
	      }
	    catch (RemoteException rx)
	      {
		println("Could not set field value: " + rx);
		return false;
	      }
	  }
	else if (v.getSource() instanceof JpassField)
	  {
	    pass_field field = (pass_field) objectHash.get(v.getSource());

	    /* -- */

	    try
	      {
		if (debug)
		  {
		    println(field.getTypeDesc() + " trying to set to " + v.getValue());
		  }

		returnValue = field.setPlainTextPass((String)v.getValue());
	      }
	    catch (RemoteException rx)
	      {
		println("Could not set field value: " + rx);
		return false;
	      }
 	  }
	else if (v.getSource() instanceof vectorPanel)
	  {
	    // no vectorPanel should really ever call this
	    // method, so wtf?

	    if (debug)
	      {
		println("Something happened in the vector panel");
	      }
	  }
	else if (v.getSource() instanceof StringSelector)
	  {
	    StringSelector sourceComponent = (StringSelector) v.getSource();

	    /* -- */

	    if (debug)
	      {
		println("value performed from StringSelector");
	      }

	    // a StringSelector data component could be feeding us any of a
	    // number of conditions, that we need to check.

	    // First, are we being given a menu operation from StringSelector?
	
	    if (v.getOperationType() == JValueObject.PARAMETER)
	      {
		if (debug)
		  {
		    println("MenuItem selected in a StringSelector");
		  }

		String command = (String) v.getParameter();

		if (command.equals("Edit object"))
		  {
		    if (debug)
		      {
			println("Edit object: " + v.getValue());
		      }

		    Invid invid = (Invid) v.getValue();
		    
		    gc.editObject(invid);

		    return true;
		  }
		else if (command.equals("View object"))
		  {
		    if (debug)
		      {
			println("View object: " + v.getValue());
		      }

		    Invid invid = (Invid) v.getValue();
		    
		    gc.viewObject(invid);

		    return true;
		  }
		else
		  {
		    println("Unknown action command from popup: " + command);
		  }
	      }
	    else if (objectHash.get(sourceComponent) instanceof invid_field)
	      {
		invid_field field = (invid_field) objectHash.get(sourceComponent);

		/* -- */

		if (field == null)
		  {
		    throw new RuntimeException("Could not find field in objectHash");
		  }

		try
		  {
		    if (v.getOperationType() == JValueObject.ADD)
		      {
			if (debug)
			  {
			    println("Adding new value to string selector");
			  }

			returnValue = field.addElement(v.getValue());
		      }
		    else if (v.getOperationType() == JValueObject.ADDVECTOR)
		      {
			if (debug)
			  {
			    println("Adding new value vector to string selector");
			  }

			returnValue = field.addElements((Vector) v.getValue());
		      }
		    else if (v.getOperationType() == JValueObject.DELETE)
		      {
			if (debug)
			  {
			    println("Removing value from field(strig selector)");
			  }

			returnValue = field.deleteElement(v.getValue());
		      }
		    else if (v.getOperationType() == JValueObject.DELETEVECTOR)
		      {
			if (debug)
			  {
			    println("Removing value vector from field(string selector)");
			  }

			returnValue = field.deleteElements((Vector) v.getValue());
		      }
		  }
		catch (RemoteException rx)
		  {
		    throw new RuntimeException("Could not change add/delete invid from field: " + rx);
		  }
	      }
	    else if (objectHash.get(v.getSource()) instanceof string_field)
	      {
		string_field field = (string_field) objectHash.get(v.getSource());

		/* -- */

		if (field == null)
		  {
		    throw new RuntimeException("Could not find field in objectHash");
		  }

		try
		  {
		    if (v.getOperationType() == JValueObject.ADD)
		      {
			returnValue = field.addElement(v.getValue());
		      }
		    else if (v.getOperationType() == JValueObject.ADDVECTOR)
		      {
			returnValue = field.addElements((Vector) v.getValue());
		      }
		    else if (v.getOperationType() == JValueObject.DELETE)
		      {
			returnValue = field.deleteElement(v.getValue());
		      }
		    else if (v.getOperationType() == JValueObject.DELETEVECTOR)
		      {
			returnValue = field.deleteElements((Vector) v.getValue());
		      }
		  }
		catch (RemoteException rx)
		  {
		    throw new RuntimeException("Could not add/remove string from string_field: " + rx);
		  }
	      }
	    else
	      {
		println("Not an Invid in string selector.");
	      }
	  }
	else
	  {
	    println("Value performed from unknown source");
	  }

	// Handle any wizards or error dialogs

	returnValue = gc.handleReturnVal(returnValue);

	if (returnValue == null)  // Success, no need to do anything else
	  {
	    if (debug)
	      {
		println("retVal is null: returning true");
	      }

	    gc.somethingChanged();
	    return true;
	  }

	if (returnValue.didSucceed())
	  {
	    if (debug)
	      {
		println("didSucceed: Returning true.");
	      }
	    
	    gc.somethingChanged();
	    return true;
	  }
	else
	  {
	    if (debug)
	      {
		println("didSucceed: Returning false.");
	      }
	    
	    return false;
	  }
      }
    catch (NullPointerException ne)
      {
	println("NullPointerException in containerPanel.setValuePerformed:\n " + ne);
	return false;
      }
    catch (IllegalArgumentException e)
      {
	println("IllegalArgumentException in containerPanel.setValuePerformed:\n " + e);
	return false;
      }
    catch (RuntimeException e)
      {
	println("RuntimeException in containerPanel.setValuePerformed:\n " + e);
	return false;
      }
    finally
      {
	currentlyChangingComponent = null;
      }
  }

  /**
   * <p>Some of our components, most notably the checkboxes, don't
   * go through JDataComponent.setValuePerformed(), but instead
   * give us direct feedback.  Those we take care of here.</p>
   *
   * @see java.awt.event.ActionListener
   */

  public void actionPerformed(ActionEvent e)
  {
    ReturnVal returnValue = null;
    db_field field = null;
    boolean newValue;

    // we are only acting as an action listener for checkboxes..
    // we'll just throw a ClassCastException if this changes
    // and we haven't fixed this code to match.

    JCheckBox cb = (JCheckBox) e.getSource();

    /* -- */

    field = (db_field) objectHash.get(cb);
    if (field == null)
      {
	throw new RuntimeException("Whoa, null field for a JCheckBox: " + e);
      }

    try
      {
	newValue = cb.isSelected();
	
	try
	  {
	    returnValue = field.setValue(new Boolean(newValue));
	  }
	catch (RemoteException rx)
	  {
	    throw new IllegalArgumentException("Could not set field value: " + rx);
	  }
      
	
	// Handle any wizards or error dialogs resulting from the
	// field.setValue()

	returnValue = gc.handleReturnVal(returnValue);

	if (returnValue == null)
	  {
	    gc.somethingChanged();
	  }
	else if (returnValue.didSucceed())
	  {
	    gc.somethingChanged();
	  }
	else
	  {
	    // we need to undo things

	    // We need to turn off ourselves as an action listener
	    // while we flip this back, so we don't go through this
	    // method again.
	
	    cb.removeActionListener(this);
	
	    cb.setSelected(!newValue);
	
	    // and we re-enable event notification
	
	    cb.addActionListener(this);
	  }
      }
    catch (Exception ex)
      {
	// An exception was thrown, most likely from the server.  We need to revert the check box.

	println("Exception occured in containerPanel.actionPerformed: " + ex);

	try
	  {
	    Boolean b = (Boolean)field.getValue();
	    cb.setSelected((b == null) ? false : b.booleanValue());
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Could not talk to server: " + rx);
	  }
      }
  }

  /**
   * <p>Some of our components, most notably the JComboBoxes, don't
   * go through JDataComponent.setValuePerformed(), but instead
   * give us direct feedback.  Those we take care of here.</p>
   *
   * @see java.awt.event.ItemListener
   */

  public void itemStateChanged(ItemEvent e)
  {
    ReturnVal returnValue = null;

    // we are only acting as an action listener for comboboxes..
    // we'll just throw a ClassCastException if this changes
    // and we haven't fixed this code to match.

    JComboBox cb = (JComboBox) e.getSource();

    /* -- */

    // We don't care about deselect reports

    if (e.getStateChange() != ItemEvent.SELECTED)
      {
	return;
      }

    if (debug)
      {
	println("containerPanel.itemStateChanged(): Item selected: " + e.getItem());
      }

    // Find the field that is associated with this combo box.  Some
    // combo boxes are all by themselves, and they will be in the
    // objectHash.  Other comboBoxes are part of JInvidChoosers, and
    // they will be in the invidChooserHash

    db_field field = (db_field) objectHash.get(cb);

    if (field == null)
      {
	field = (db_field) invidChooserHash.get(cb);
	
	if (field == null)
	  {
	    throw new RuntimeException("Whoa, null field for a JComboBox: " + e);
	  }
      }

    try
      {
	Object newValue = e.getItem();
	Object oldValue = field.getValue();

	if (newValue.equals(oldValue))
	  {
	    return;		// what else is new?
	  }

	if (newValue instanceof String)
	  {
	    returnValue = field.setValue(newValue);
	  }
	else if (newValue instanceof listHandle)
	  {
	    listHandle lh = (listHandle) newValue;

	    if (debug)
	      {
		if (field == null)
		  {
		    println("Field is null.");
		  }
	      }

	    returnValue = field.setValue(lh.getObject());
	  }
	else 
	  {
	    throw new RuntimeException("Unknown type from JComboBox: " + newValue);
	  }

	// handle any wizards and/or error dialogs

	returnValue = gc.handleReturnVal(returnValue);

	if (returnValue == null)
	  {
	    gc.somethingChanged();

	    if (debug)
	      {
		println("field setValue returned true");
	      }
	  }
	else if (returnValue.didSucceed())
	  {
	    if (debug)
	      {
		println("field setValue returned true!!");
	      }

	    gc.somethingChanged();
	  }
	else
	  {
	    // Failure.. need to revert the combobox

	    // turn off callbacks

	    cb.removeItemListener(this);
	    
	    if (oldValue == null)
	      {
		cb.setSelectedItem(null);
	      }
	    else if (newValue instanceof String)
	      {
		cb.setSelectedItem(oldValue);
	      }
	    else if (newValue instanceof listHandle)
	      {
		listHandle lh = new listHandle(gc.getSession().viewObjectLabel((Invid) oldValue), oldValue);
		cb.setSelectedItem(lh);
	      }
	    
	    // turn callbacks back on
	    
	    cb.addItemListener(this);
	  }
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not set combo box value: " + rx);
      }
  }

  /** 
   * <p>All the other addXXX methods used to add GUI components of a
   * particular type call this to actually register the GUI component
   * in this containerPanel for display.</p>
   */

  private synchronized void addRow(Component comp, int row, String label, boolean visible)
  {
    JLabel l = new JLabel(label);
    rowHash.put(comp, l);

    gbc.fill = GridBagConstraints.NONE;
    gbc.gridwidth = 1;
    
    gbc.weightx = 0.0;
    gbc.gridx = 0;
    gbc.gridy = row;
    gbl.setConstraints(l, gbc);
    add(l);
    
    gbc.gridx = 1;
    gbc.weightx = 1.0;
    
    if (comp instanceof JstringArea)
      {
	JScrollPane sp = new JScrollPane(comp);
	gbl.setConstraints(sp, gbc);
	add(sp);

	setRowVisible(sp, visible);
      }
    else
      {
	gbl.setConstraints(comp, gbc);
	add(comp);
	
	setRowVisible(comp, visible);
      }
  }

  /**
   * <p>This private method toggles the visibility of a field component
   * and its label in this containerPanel.</p>
   */

  private void setRowVisible(Component comp, boolean b)
  {
    Component c = (Component) rowHash.get(comp);

    /* -- */

    if (c == null)
      {
	return;
      }

    comp.setVisible(b);
    c.setVisible(b);
  }

  /**
   * <p>Helper method to add a component during constructor operation.  This
   * is the top-level field component adding method.</p>
   */

  private void addFieldComponent(db_field field, 
				 FieldInfo fieldInfo, 
				 FieldTemplate fieldTemplate) throws RemoteException
  {
    short fieldType;
    String name = null;
    boolean isVector;
    boolean isEditInPlace;

    /* -- */

    if (field == null)
      {
	throw new IllegalArgumentException("null field");
      }

    fieldType = fieldTemplate.getType();
    isVector = fieldTemplate.isArray();

    if (debug)
      {
	println(" Name: " + fieldTemplate.getName() + " Field type desc: " + fieldType);
      }

    if (isVector)
      {
	if (fieldType == FieldType.STRING)
	  {
	    addStringVector((string_field) field, fieldInfo, fieldTemplate);
	  }
	else if (fieldType == FieldType.INVID && !fieldTemplate.isEditInPlace())
	  {
	    addInvidVector((invid_field) field, fieldInfo, fieldTemplate);
	  }
	else			// generic vector
	  {
	    addVectorPanel(field, fieldInfo, fieldTemplate);
	  }
      }
    else
      {
	// plain old component

	switch (fieldType)
	  {
	  case -1:
	    printErr("**** Could not get field information");
	    break;
		      
	  case FieldType.STRING:
	    addStringField((string_field) field, fieldInfo, fieldTemplate);
	    break;
		      
	  case FieldType.PASSWORD:
	    addPasswordField((pass_field) field, fieldInfo, fieldTemplate);
	    break;
		      
	  case FieldType.NUMERIC:
	    addNumericField(field, fieldInfo, fieldTemplate);
	    break;

 	  case FieldType.FLOAT:
 	    addFloatField(field, fieldInfo, fieldTemplate);
 	    break;

	  case FieldType.DATE:
	    addDateField(field, fieldInfo, fieldTemplate);
	    break;
		      
	  case FieldType.BOOLEAN:
	    addBooleanField(field, fieldInfo, fieldTemplate);
	    break;
		      
	  case FieldType.PERMISSIONMATRIX:
	    addPermissionField(field, fieldInfo, fieldTemplate);
	    break;
		      
	  case FieldType.INVID:
	    addInvidField((invid_field)field, fieldInfo, fieldTemplate);
	    break;

	  case FieldType.IP:
	    addIPField((ip_field) field, fieldInfo, fieldTemplate);
	    break;
		      
	  default:
	    JLabel label = new JLabel("(Unknown)Field type ID = " + fieldType);
	    addRow(label, templates.indexOf(fieldTemplate), fieldTemplate.getName(), true);
	  }
      }
  }

  /**
   *
   * private helper method to instantiate a string vector in this
   * container panel
   *
   */

  private void addStringVector(string_field field, 
			       FieldInfo fieldInfo,
			       FieldTemplate fieldTemplate) throws RemoteException
  {
    objectList list = null;

    /* -- */

    if (debug)
      {
	println("Adding StringSelector, its a vector of strings!");
      }

    if (field == null)
      {
	println("Hey, this is a null field! " + fieldTemplate.getName());
	return;
      }

    if (editable && fieldInfo.isEditable())
      {
	QueryResult qr = null;
	
	if (debug)
	  {
	    println("Getting choicesKey()");
	  }

	Object id = field.choicesKey();

	if (id == null)
	  {
	    if (debug)
	      {
		println("Key is null, Getting choices");
	      }

	    qr = field.choices();

	    if (qr != null)
	      {
		list = new objectList(qr);
	      }
	  }
	else
	  {
	    if (gc.cachedLists.containsList(id))
	      {
		list = gc.cachedLists.getList(id);
	      }
	    else
	      {	
		if (debug)
		  {
		    println("Getting QueryResult now");
		  }

		qr = field.choices();

		if (qr != null)
		  {
		    gc.cachedLists.putList(id, qr);
		    list = gc.cachedLists.getList(id);
		  }
	      }
	  }
    
	if (!keepLoading())
	  {
	    if (debug)
	      {
		println("Stopping containerPanel in the midst of loading a StringSelector");
	      }

	    return;
	  }

	if (list == null)
	  {
	    StringSelector ss = new StringSelector(null,
						   (Vector)fieldInfo.getValue(), 
						   this,
						   true, // editable
						   false,  // canChoose
						   false,  // mustChoose
						   300); // this is double wide because there is no available list

	    objectHash.put(ss, field);
	    shortToComponentHash.put(new Short(fieldInfo.getID()), ss);

	    ss.setCallback(this);

	    String comment = fieldTemplate.getComment();
	    
	    if (comment != null && !comment.equals(""))
	      {
		ss.setToolTipText(comment);
	      }

	    addRow(ss, templates.indexOf(fieldTemplate), fieldTemplate.getName(), fieldInfo.isVisible()); 
	  }
	else
	  {
	    Vector available = list.getLabels(false);
	    StringSelector ss = new StringSelector(available,
						   (Vector)fieldInfo.getValue(), 
						   this,
						   true, // editable
						   true,   // canChoose
						   false,  // mustChoose
						   (available != null ? 150 : 300));
	    objectHash.put(ss, field);
	    shortToComponentHash.put(new Short(fieldInfo.getID()), ss);

	    ss.setCallback(this);

	    String comment = fieldTemplate.getComment();
	    
	    if (comment != null && !comment.equals(""))
	      {
		ss.setToolTipText(comment);
	      }

	    addRow(ss, templates.indexOf(fieldTemplate), fieldTemplate.getName(), fieldInfo.isVisible()); 
	  }
      }
    else  //not editable, don't need whole list of things
      {
	StringSelector ss = new StringSelector(null,
					       (Vector)fieldInfo.getValue(), 
					       this,
					       false, // not editable
					       false,   // canChoose
					       false,  // mustChoose
					       300); // no availble list, so it is wider
	objectHash.put(ss, field);
	shortToComponentHash.put(new Short(fieldInfo.getID()), ss);

	String comment = fieldTemplate.getComment();
	    
	if (comment != null && !comment.equals(""))
	  {
	    ss.setToolTipText(comment);
	  }

	addRow(ss, templates.indexOf(fieldTemplate), fieldTemplate.getName(), fieldInfo.isVisible()); 
      }
  }

  /**
   * <p>private helper method to instantiate an invid vector in this
   * container panel</p>
   */

  private void addInvidVector(invid_field field, 
			      FieldInfo fieldInfo, 
			      FieldTemplate fieldTemplate) throws RemoteException
  {
    QueryResult
      valueResults = null,
      choiceResults = null;

    Vector
      valueHandles = null,
      choiceHandles = null;

    objectList
      list = null;

    /* -- */

    if (debug)
      {
	println("Adding StringSelector, its a vector of invids!");
      }

    QueryResult qres = field.encodedValues();

    if (qres != null)
      {
	valueHandles = qres.getListHandles();
      }

    if (!keepLoading())
      {
	if (debug)
	  {
	    println("Stopping containerPanel in the midst of loading a StringSelector");
	  }

	return;
      }

    if (editable && fieldInfo.isEditable())
      {
	Object key = field.choicesKey();

	if (key == null)
	  {
	    if (debug)
	      {
		println("key is null, downloading new copy");
	      }

	    QueryResult choices = field.choices();

	    if (choices != null)
	      {
		// the server may not automatically restrict inactive
		// objects from the list, so we want to exclude inactive
		// objects.  We don't want to exclude non-editable objects,
		// however, because the server may include nominally
		// non-editable objects that we are granted permission
		// to link to by the DBEditObject.anonymousLinkOK()
		// method.

		choiceHandles = choices.getListHandles(false, true);
	      }
	    else
	      { 
		if (debug)
		  {
		    println("choices is null");
		  }

		choiceHandles = null;
	      }
	  }
	else
	  {
	    if (debug)
	      {
		println("key= " + key);
	      }

	    if (gc.cachedLists.containsList(key))
	      {
		if (debug)
		  {
		    println("It's in there, using cached list");
		  }

		// when we are drawing a list of choices from the cache,
		// we know that the server didn't filter that list for
		// us so that the 'non-editables' are actually valid
		// choices in this context, so we don't want either
		// inactive nor non-editable choices.

		choiceHandles = gc.cachedLists.getListHandles(key, false, false);
	      }
	    else
	      {
		if (debug)
		  {
		    println("Choice list for addInvidVector not cached, downloading choices.");
		  }

		QueryResult qr = field.choices();

		if (qr == null)
		  {
		    choiceHandles = null;
		  }
		else
		  {
		    gc.cachedLists.putList(key, qr);
		    list = gc.cachedLists.getList(key);

		    // when the server gives us a choice key, we know
		    // that the server didn't filter that list for us
		    // so that the 'non-editables' are actually valid
		    // choices in this context, so we don't want
		    // either inactive nor non-editable choices.

		    choiceHandles = list.getListHandles(false, false);
		  }

		// debuging stuff

		if (debug_persona)
		  {
		    System.out.println();
		    
		    for (int i = 0; i < choiceHandles.size(); i++)
		      {
			println(" choices: " + (listHandle)choiceHandles.elementAt(i));
		      }
		    
		    System.out.println();
		  }
	      }
	  }
      }
    else
      { 
	if (debug)
	  {
	    println("Not editable, not downloading choices");
	  }
      }

    // ss is canChoose, mustChoose
    JPopupMenu invidTablePopup = new JPopupMenu();
    JMenuItem viewO = new JMenuItem("View object");
    JMenuItem editO = new JMenuItem("Edit object");
    invidTablePopup.add(viewO);
    invidTablePopup.add(editO);
    
    JPopupMenu invidTablePopup2 = new JPopupMenu();
    JMenuItem viewO2 = new JMenuItem("View object");
    JMenuItem editO2 = new JMenuItem("Edit object");
    invidTablePopup2.add(viewO2);
    invidTablePopup2.add(editO2);

    if (debug)
      {
	println("Creating StringSelector");
      }

    StringSelector ss = new StringSelector(choiceHandles, valueHandles, this, 
					   editable && fieldInfo.isEditable(), 
					   true, true, 
					   ((choiceHandles != null) && 
					    (editable && fieldInfo.isEditable())) ? 150 : 300,
					   "Members", "Available",
					   invidTablePopup, invidTablePopup2);
    if (choiceHandles == null)
      {
	ss.setButtonText("Create");
      }

    objectHash.put(ss, field);
    shortToComponentHash.put(new Short(fieldInfo.getID()), ss);
    
    ss.setCallback(this);

    String comment = fieldTemplate.getComment();
	    
    if (comment != null && !comment.equals(""))
      {
	ss.setToolTipText(comment);
      }

    addRow(ss, templates.indexOf(fieldTemplate), fieldTemplate.getName(), fieldInfo.isVisible()); 
  }

  /**
   * <p>private helper method to instantiate a vector panel in this
   * container panel</p>
   */

  private void addVectorPanel(db_field field, 
			      FieldInfo fieldInfo, 
			      FieldTemplate fieldTemplate) throws RemoteException
  {
    boolean isEditInPlace = fieldTemplate.isEditInPlace();

    /* -- */

    if (debug)
      {
	if (isEditInPlace)
	  {
	    println("Adding editInPlace vector panel");
	  }
	else
	  {
	    println("Adding normal vector panel");
	  }
      }

    vectorPanel vp = new vectorPanel(field, winP, editable && fieldInfo.isEditable(), 
				     isEditInPlace, this, isCreating);
    vectorPanelList.addElement(vp);
    objectHash.put(vp, field);
    shortToComponentHash.put(new Short(fieldInfo.getID()), vp);

    String comment = fieldTemplate.getComment();
	    
    if (comment != null && !comment.equals(""))
      {
	vp.setToolTipText(comment);
      }

    addVectorRow(vp, templates.indexOf(fieldTemplate), 
		 fieldTemplate.getName(), fieldInfo.isVisible());
  }

  /**
   * <p>This private helper method is used to insert a vectorPanel into the containerPanel</p>
   */

  private synchronized void addVectorRow(Component comp, int row, String label, boolean visible)
  {
    JLabel l = new JLabel(label);
    rowHash.put(comp, l);
    
    gbc.gridwidth = 2;
    gbc.gridx = 0;
    gbc.gridy = row;

    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbl.setConstraints(comp, gbc);
    add(comp);

    setRowVisible(comp, visible);
  }

  /**
   * <p>If we contain any {@link arlut.csd.ganymede.client.vectorPanel vectorPanel}s,
   * they will call this method during loading to let us update our progress bar if
   * we have it still up.  This is used to let us include the time it will take
   * to get vector panels loaded in the progress bar time estimate.</p>
   */

  public void vectorElementAdded()
  {
    if (progressBar != null)
      {
	progressBar.setValue(progressBar.getValue() + 1);
      }
    
    ++vectorElementsAdded;
  }

  /**
   * <p>private helper method to instantiate a scalar string field in this
   * container panel</p>
   *
   * @param field Remote reference to database field to be associated with a gui component
   * @param fieldInfo Downloaded value and status information for this field
   * @param fieldTemplate Downloaded static field type information for this field
   */

  private void addStringField(string_field field, FieldInfo fieldInfo, 
			      FieldTemplate fieldTemplate) throws RemoteException
  {
    objectList
      list;

    JstringField
      sf;

    boolean
      mustChoose;

    /* -- */

    if (field.canChoose())
      {
	if (debug)
	  {
	    println("You can choose");
	  }
	    
	Vector choiceHandles = null;
	Vector choices = null;

	Object key = field.choicesKey();

	if (key == null)
	  {
	    if (debug)
	      {
		println("key is null, getting new copy.");
	      }

	    choices = field.choices().getLabels();
	  }
	else
	  {
	    if (debug)
	      {
		println("key = " + key);
	      }
		
	    if (gc.cachedLists.containsList(key))
	      {
		if (debug)
		  {
		    println("key in there, using cached list");
		  }
		
		list = gc.cachedLists.getList(key);
	      }
	    else
	      {
		if (debug)
		  {
		    println("Choice list for addStringField not cached, downloading a new one.");
		  }
		
		gc.cachedLists.putList(key, field.choices());
		list = gc.cachedLists.getList(key);
	      }

	    choiceHandles = list.getListHandles(false);
	    choices = list.getLabels(false);
	  }    

	String currentChoice = (String) fieldInfo.getValue();
	boolean found = false;
	    
	JComboBox combo = new JComboBox(choices);
	combo.setKeySelectionManager(new TimedKeySelectionManager());

	combo.setMaximumRowCount(8);
	combo.setMaximumSize(new Dimension(Integer.MAX_VALUE,20));

	try
	  {
	    mustChoose = field.mustChoose();
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Could not check to see if field was mustChoose.");
	  }

	combo.setEditable(!mustChoose); 

	if (currentChoice == null)
	  {
	    if (debug)
	      {
		println("Setting current value to <none>, because the current choice is null. " + currentChoice);
	      }

	    combo.addItem("<none>");
	    combo.setSelectedItem("<none>");
	  }
	else
	  {
	    if (debug)
	      {
		println("Setting current value: " + currentChoice);
	      }	  

	    try
	      {
		combo.setSelectedItem(currentChoice);
	      }
	    catch (IllegalArgumentException e)
	      {
		println("IllegalArgumentException: current choice is not in the string selection combobox.  Adding it now.");
		combo.addItem(currentChoice);
		combo.setSelectedItem(currentChoice);
	      }
	  }

	if (editable && fieldInfo.isEditable())
	  {
	    combo.addItemListener(this); // register callback
	  }

	objectHash.put(combo, field);
	shortToComponentHash.put(new Short(fieldInfo.getID()), combo);

	String comment = fieldTemplate.getComment();

	if (comment != null && !comment.equals(""))
	  {
	    combo.setToolTipText(comment);
	  }
	    
	addRow(combo, templates.indexOf(fieldTemplate), 
	       fieldTemplate.getName(), fieldInfo.isVisible());
      }
    else if (fieldTemplate.isMultiLine())
      {
	JstringArea sa = new JstringArea(6, FIELDWIDTH);
	sa.setEditable(editable && fieldInfo.isEditable());		
	sa.setAllowedChars(fieldTemplate.getOKChars());
	sa.setDisallowedChars(fieldTemplate.getBadChars());
	sa.setCallback(this);
	
	objectHash.put(sa, field);
	shortToComponentHash.put(new Short(fieldInfo.getID()), sa);
			      
	sa.setText((String)fieldInfo.getValue());
	    			
	if (editable && fieldInfo.isEditable())
	  {
	    sa.setCallback(this);
	  }

	sa.setEditable(editable && fieldInfo.isEditable());

	String comment = fieldTemplate.getComment();

	if (comment != null && !comment.equals(""))
	  {
	    sa.setToolTipText(comment);
	  }

	addRow(sa, templates.indexOf(fieldTemplate), fieldTemplate.getName(), fieldInfo.isVisible());
      }
    else
      {
	// It's not a choice

	int maxLength = fieldTemplate.getMaxLength();

	sf = new JstringField(FIELDWIDTH > maxLength ? maxLength + 1 : FIELDWIDTH,
			      maxLength,
			      editable && fieldInfo.isEditable(),
			      false,
			      fieldTemplate.getOKChars(),
			      fieldTemplate.getBadChars(),
			      this);
			      
	objectHash.put(sf, field);
	shortToComponentHash.put(new Short(fieldInfo.getID()), sf);
			      
	sf.setText((String)fieldInfo.getValue());
	    			
	if (editable && fieldInfo.isEditable())
	  {
	    sf.setCallback(this);
	  }

	sf.setEditable(editable && fieldInfo.isEditable());

	String comment = fieldTemplate.getComment();

	if (comment != null && !comment.equals(""))
	  {
	    sf.setToolTipText(comment);
	  }

	addRow(sf, templates.indexOf(fieldTemplate), fieldTemplate.getName(), fieldInfo.isVisible());
      }
  }

  /**
   * <p>private helper method to instantiate a password field in this
   * container panel</p>
   *
   * @param field Remote reference to database field to be associated with a gui component
   * @param fieldInfo Downloaded value and status information for this field
   * @param fieldTemplate Downloaded static field type information for this field
   */

  private void addPasswordField(pass_field field, 
				FieldInfo fieldInfo, 
				FieldTemplate fieldTemplate) throws RemoteException
  {
    JstringField sf;

    /* -- */

    if (editable && fieldInfo.isEditable())
      {
	JpassField pf = new JpassField(gc, true, 10, 8, editable && fieldInfo.isEditable());
	objectHash.put(pf, field);
	shortToComponentHash.put(new Short(fieldInfo.getID()), pf);
			
	if (editable && fieldInfo.isEditable())
	  {
	    pf.setCallback(this);
	  }

	String comment = fieldTemplate.getComment();

	if (comment != null && !comment.equals(""))
	  {
	    pf.setToolTipText(comment);
	  }
	  
	addRow(pf, templates.indexOf(fieldTemplate), field.getName(), field.isVisible());
      }
    else
      {
	int maxLength = fieldTemplate.getMaxLength();
	sf = new JstringField(FIELDWIDTH > maxLength ? maxLength + 1 : FIELDWIDTH,
			      maxLength,
			      true,
			      false,
			      null,
			      null);

	objectHash.put(sf, field);
	shortToComponentHash.put(new Short(fieldInfo.getID()), sf);
			  
	// the server won't give us an unencrypted password, we're clear here
			  
	sf.setText((String)fieldInfo.getValue());
		      
	sf.setEditable(false);

	String comment = fieldTemplate.getComment();

	if (comment != null && !comment.equals(""))
	  {
	    sf.setToolTipText(comment);
	  }
	
	addRow(sf, templates.indexOf(fieldTemplate), fieldTemplate.getName(), fieldInfo.isVisible());
      }
  }

  /**
   * <p>private helper method to instantiate a numeric field in this
   * container panel</p>
   *
   * @param field Remote reference to database field to be associated with a gui component
   * @param fieldInfo Downloaded value and status information for this field
   * @param fieldTemplate Downloaded static field type information for this field
   */

  private void addNumericField(db_field field, 
			       FieldInfo fieldInfo, 
			       FieldTemplate fieldTemplate) throws RemoteException
  {
    if (debug)
      {
	println("Adding numeric field");
      }
      
    JnumberField nf = new JnumberField();

    objectHash.put(nf, field);
    shortToComponentHash.put(new Short(fieldInfo.getID()), nf);
		      
    Integer value = (Integer)fieldInfo.getValue();

    if (value != null)
      {
	nf.setValue(value.intValue());
      }

    if (editable && fieldInfo.isEditable())
      {
	nf.setCallback(this);
      }

    nf.setEditable(editable && fieldInfo.isEditable());
    nf.setColumns(FIELDWIDTH);

    String comment = fieldTemplate.getComment();

    if (comment != null && !comment.equals(""))
      {
	nf.setToolTipText(comment);
      }
    
    addRow(nf, templates.indexOf(fieldTemplate), fieldTemplate.getName(), fieldInfo.isVisible());
  }

   /**
    * <p>private helper method to instantiate a numeric field in this
    * container panel</p>
    *
    * @param field Remote reference to database field to be associated with a gui component
    * @param fieldInfo Downloaded value and status information for this field
    * @param fieldTemplate Downloaded static field type information for this field
    */
 
  private void addFloatField(db_field field, 
 			     FieldInfo fieldInfo, 
 			     FieldTemplate fieldTemplate) throws RemoteException
  {
    if (debug)
      {
 	println("Adding float field");
      }
    
    JfloatField nf = new JfloatField();
 
    objectHash.put(nf, field);
    shortToComponentHash.put(new Short(fieldInfo.getID()), nf);
 		      
    Double value = (Double)fieldInfo.getValue();
 
    if (value != null)
      {
 	nf.setValue(value.doubleValue());
      }
    
    if (editable && fieldInfo.isEditable())
      {
 	nf.setCallback(this);
      }
 
    nf.setEditable(editable && fieldInfo.isEditable());
    nf.setColumns(FIELDWIDTH);
 
    String comment = fieldTemplate.getComment();
 
    if (comment != null && !comment.equals(""))
      {
 	nf.setToolTipText(comment);
      }
     
    addRow(nf, templates.indexOf(fieldTemplate), fieldTemplate.getName(), fieldInfo.isVisible());
  }

  /**
   * <p>private helper method to instantiate a date field in this
   * container panel</p>
   *
   * @param field Remote reference to database field to be associated with a gui component
   * @param fieldInfo Downloaded value and status information for this field
   * @param fieldTemplate Downloaded static field type information for this field
   */

  private void addDateField(db_field field, 
			    FieldInfo fieldInfo, 
			    FieldTemplate fieldTemplate) throws RemoteException
  {
    JdateField df = new JdateField();

    objectHash.put(df, field);
    shortToComponentHash.put(new Short(fieldInfo.getID()), df);

    if (debug) {
      println("Editable: " + editable  + " isEditable: " +fieldInfo.isEditable());
    }

    df.setEditable(editable && fieldInfo.isEditable());
    df.setEnabled(editable && fieldInfo.isEditable());

    Date date = ((Date)fieldInfo.getValue());
    
    if (date != null)
      {
	df.setDate(date);
      }

    // note that we set the callback after we initially set the
    // date, to avoid having the callback triggered on a listing

    if (editable && fieldInfo.isEditable())
      {
	df.setCallback(this);
      }

    addRow(df, templates.indexOf(fieldTemplate), 
	   fieldTemplate.getName(), fieldInfo.isVisible());
  }

  /**
   * <p>private helper method to instantiate a boolean field in this
   * container panel</p>
   *
   * @param field Remote reference to database field to be associated with a gui component
   * @param fieldInfo Downloaded value and status information for this field
   * @param fieldTemplate Downloaded static field type information for this field
   */

  private void addBooleanField(db_field field, FieldInfo fieldInfo, 
			       FieldTemplate fieldTemplate) throws RemoteException
  {
    JCheckBox cb = new JCheckBox();

    /* -- */

    objectHash.put(cb, field);
    shortToComponentHash.put(new Short(fieldInfo.getID()), cb);
    cb.setEnabled(editable && fieldInfo.isEditable());

    if (editable && fieldInfo.isEditable())
      {
	cb.addActionListener(this);	// register callback
      }

    try
      {
	cb.setSelected(((Boolean)fieldInfo.getValue()).booleanValue());
      }
    catch (NullPointerException ex)
      {
	if (debug)
	  {
	    println("Null pointer setting selected choice: " + ex);
	  }
      }

    String comment = fieldTemplate.getComment();

    if (comment != null && !comment.equals(""))
      {
	cb.setToolTipText(comment);
      }

    addRow(cb, templates.indexOf(fieldTemplate), 
	   fieldTemplate.getName(), fieldInfo.isVisible());
  }

  /**
   * <p>private helper method to instantiate a permission matrix field in this
   * container panel</p>
   *
   * @param field Remote reference to database field to be associated with a gui component
   * @param fieldInfo Downloaded value and status information for this field
   * @param fieldTemplate Downloaded static field type information for this field
   */

  private void addPermissionField(db_field field, FieldInfo fieldInfo,
				  FieldTemplate fieldTemplate) throws RemoteException
  {
    if (debug)
      {
	println("Adding perm matrix");
      }

    // note that the permissions editor does its own callbacks to
    // the server, albeit using our transaction / session.

    Invid invid = object.getInvid(); // server call

    perm_button pb = new perm_button((perm_field) field,
				     editable && fieldInfo.isEditable(),
				     gc,
				     false,
				     fieldTemplate.getName());

    String comment = fieldTemplate.getComment();

    if (comment != null && !comment.equals(""))
      {
	pb.setToolTipText(comment);
      }
    
    addRow(pb, templates.indexOf(fieldTemplate),
	   fieldTemplate.getName(), fieldInfo.isVisible());
  }

  /**
   * <p>private helper method to instantiate a scalar invid field in this
   * container panel</p>
   *
   * @param field Remote reference to database field to be associated with a gui component
   * @param fieldInfo Downloaded value and status information for this field
   * @param fieldTemplate Downloaded static field type information for this field
   */

  private void addInvidField(invid_field field, 
			     FieldInfo fieldInfo, 
			     FieldTemplate fieldTemplate) throws RemoteException
  {
    objectList list;

    /* -- */

    if (debug)
      {
	println("addInvidField(" + fieldTemplate.getName() + ")");
      }
    
    if (fieldTemplate.isEditInPlace())
      {
	// this should never happen
	
	if (debug)
	  {
	    println("Hey, " + fieldTemplate.getName() +
		    " is edit in place but not a vector, what gives?");
	  }

	addRow(new JLabel("edit in place non-vector"), 
	       templates.indexOf(fieldTemplate), 
	       fieldTemplate.getName(), 
	       fieldInfo.isVisible());

	return;
      }

    if (!editable || !fieldInfo.isEditable())
      {
	if (fieldInfo.getValue() != null)
	  {
	    final Invid thisInvid = (Invid) fieldInfo.getValue();
	    
	    String label = (String) gc.getSession().viewObjectLabel(thisInvid);

	    if (label == null)
	      {
		if (debug)
		  {
		    println("-you don't have permission to view this object.");
		  }

		label = "Permission denied!";
	      }

	    JButton b = new JButton(label);

	    b.addActionListener(new ActionListener() {
	      public void actionPerformed(ActionEvent e)
		{
		  getgclient().viewObject(thisInvid);
		}
	    });

	    String comment = fieldTemplate.getComment();
	    
	    if (comment != null && !comment.equals(""))
	      {
		b.setToolTipText(comment);
	      }

	    addRow(b, 
		   templates.indexOf(fieldTemplate), 
		   fieldTemplate.getName(), 
		   fieldInfo.isVisible());
	  }
	else
	  {
	    addRow(new JTextField("null invid"), 
		   templates.indexOf(fieldTemplate), 
		   fieldTemplate.getName(), 
		   fieldInfo.isVisible());
	  }

	return;
      }

    // okay, we've got an editable field, we need to construct and install
    // a JInvidChooser.

    Object key = field.choicesKey();
	
    Vector choices = null;

    if (key == null)
      {
	if (debug)
	  {
	    println("key is null, not using cache.  Downloading choice list.");
	  }
	
	list = new objectList(field.choices());

	if (debug)
	  {
	    println("Choice list downloaded.");
	  }

	// we have to include non-editables, because the server will
	// include some that are non-editable, but for which
	// DBEditObject.anonymousLinkOK() nonetheless give us rights
	// to link.

	choices = list.getListHandles(false, true);
      }
    else
      {
	if (debug)
	  {
	    println("key = " + key);
	  }

	if (gc.cachedLists.containsList(key))
	  {
	    if (debug)
	      {
		println("Got it from the cachedLists");
	      }

	    list = gc.cachedLists.getList(key);
	  }
	else
	  {
	    if (debug)
	      {
		println("Choice list for addInvidField not cached, downloading a new one.");
	      }

	    gc.cachedLists.putList(key, field.choices());

	    if (debug)
	      {
		println("Choice list downloaded.");
	      }

	    list = gc.cachedLists.getList(key);
	  }

	// we have to include non-editables, because the server will
	// include some that are non-editable, but for which
	// DBEditObject.anonymousLinkOK() nonetheless give us rights
	// to link.

	choices = list.getListHandles(false, true);
      }

    Invid currentChoice = (Invid) fieldInfo.getValue();
    String currentChoiceLabel = gc.getSession().viewObjectLabel(currentChoice);

    if (debug)
      {
	println("Current choice is : " + currentChoice + ", " + currentChoiceLabel);
      }
	
    listHandle currentListHandle = null;
    listHandle noneHandle = new listHandle("<none>", null);
    boolean found = false;
    JInvidChooser combo;
    boolean mustChoose = false;
	
    try
      {
	mustChoose = field.mustChoose();
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not get mustChoose: " + rx);
      }

    // Find currentListHandle
    
    // Make sure the current choice is in the chooser, if there is
    // a current choice.
    
    if (currentChoice != null)
      {
	for (int j = 0; j < choices.size(); j++)
	  {
	    listHandle thisChoice = (listHandle) choices.elementAt(j);

	    if (thisChoice.getObject() == null)
	      {
		println("Current object " + thisChoice + " is null.");
	      }

	    if (currentChoice.equals(thisChoice.getObject()))
	      {
		if (debug)
		  {
		    println("Found the current object in the list!");
		  }

		currentListHandle = thisChoice;
		found = true;
		break;
	      }
	  }

	if (!found)
	  {
	    currentListHandle = new listHandle(gc.getSession().viewObjectLabel(currentChoice), currentChoice);
	    choices.addElement(currentListHandle);
	  }
      }

    if (!mustChoose || (currentChoice == null))
      {
	if (debug)
	  {
	    println("inserting null handle");
	  }

	choices.insertElementAt(noneHandle, 0);
      }

    if (debug)
      {
	println("creating JInvidChooser");
      }

    combo = new JInvidChooser(choices, this, fieldTemplate.getTargetBase());

    combo.setMaximumRowCount(12);
    combo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
    combo.setVisible(true);
    
    if (currentChoice != null)
      {
	if (debug)
	  {
	    println("setting current choice: " + currentChoiceLabel);
	  }

	try
	  {
	    combo.setSelectedItem(currentListHandle);
	  }
	catch (IllegalArgumentException e)
	  {
	    println("IllegalArgumentException: current handle not in the list, adding it now.");
	    combo.addItem(currentListHandle);
	    combo.setSelectedItem(currentListHandle);
	  }
      }
    else
      {
	if (debug)
	  {
	    println("currentChoice is null");
	  }

	combo.setSelectedItem(noneHandle);
      }	  

    if (editable && fieldInfo.isEditable())
      {
	combo.addItemListener(this); // register callback
      }
    
    combo.setAllowNone(!mustChoose);

    // We get the itemStateChanged straight from the JComboBox in the
    // JInvidChooser, so we need to save an association between the
    // combobox and the field
    
    invidChooserHash.put(combo.getCombo(), field); 
    
    // The update method still need to be able to find the field from
    // the JInvidChooser, so we save it in objectHash, too.
    
    objectHash.put(combo, field); 
    
    shortToComponentHash.put(new Short(fieldInfo.getID()), combo);
    
    if (debug)
      {
	println("Adding to panel");
      }

    String comment = fieldTemplate.getComment();

    if (comment != null && !comment.equals(""))
      {
	combo.setToolTipText(comment);
      }
    
    addRow(combo, templates.indexOf(fieldTemplate), fieldTemplate.getName(), fieldInfo.isVisible());
  }

  /**
   * <p>private helper method to instantiate an ip address field in this
   * container panel</p>
   *
   * @param field Remote reference to database field to be associated with a gui component
   * @param fieldInfo Downloaded value and status information for this field
   * @param fieldTemplate Downloaded static field type information for this field 
   */

  private void addIPField(ip_field field, 
			  FieldInfo fieldInfo, 
			  FieldTemplate fieldTemplate) throws RemoteException
  {
    JIPField
      ipf;

    Byte[] bytes;

    /* -- */

    if (debug)
      {
	println("Adding IP field");
      }

    try
      {
	ipf = new JIPField(editable && fieldInfo.isEditable(),
			   (editable && fieldInfo.isEditable()) ? field.v6Allowed() : field.isIPV6());
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not determine if v6 Allowed for ip field: " + rx);
      }
    
    objectHash.put(ipf, field);
    shortToComponentHash.put(new Short(fieldInfo.getID()), ipf);
    
    bytes = (Byte[]) fieldInfo.getValue();

	if (bytes != null)
	  {
	    ipf.setValue(bytes);
	  }
	
    ipf.setCallback(this);

    String comment = fieldTemplate.getComment();

    if (comment != null && !comment.equals(""))
      {
	ipf.setToolTipText(comment);
      }
		
    addRow(ipf,
	   templates.indexOf(fieldTemplate), 
	   fieldTemplate.getName(), 
	   fieldInfo.isVisible());
  }

  /**
   * <p>This method provides a handy way to null out data structures held in
   * relationship to this containerPanel, particularly network reference
   * resources.</p>
   *
   * <p>It is essential that this method be called from the client's
   * GUI thread.</p>
   */

  public final void cleanUp()
  {
    Enumeration enum;

    /* -- */

    if (debug)
      {
	System.err.println("containerPanel cleanUp()");
      }

    // first we pull everything out of our panel

    enum = rowHash.keys();

    while (enum.hasMoreElements())
      {
	JComponent j = (JComponent) enum.nextElement();

	if (j instanceof JCheckBox)
	  {
	    ((JCheckBox) j).removeActionListener(this);
	  }
	else if (j instanceof JComboBox)
	  {
	    ((JComboBox) j).removeItemListener(this);
	  }
	else if (j instanceof JInvidChooser)
	  {
	    ((JInvidChooser) j).removeItemListener(this);
	  }
      }

    this.removeAll();

    gc = null;
    invid = null;
    winP = null;
    frame = null;
    
    if (updatesWhileLoading != null)
      {
	updatesWhileLoading.setSize(0);
	updatesWhileLoading = null;
      }

    if (vectorPanelList != null)
      {
	vectorPanelList.setSize(0);
	vectorPanelList = null;
      }

    currentlyChangingComponent = null;

    if (shortToComponentHash != null)
      {
	shortToComponentHash.clear();
	shortToComponentHash = null;
      }

    if (rowHash != null)
      {
	rowHash.clear();
	rowHash = null;
      }

    /**
     * <p>The critical ones.. this will release our references to fields
     * on the server</p>
     */

    object = null;

    if (objectHash != null)
      {
	objectHash.clear();
	objectHash = null;
      }

    if (invidChooserHash != null)
      {
	invidChooserHash.clear();
	invidChooserHash = null;
      }

    if (infoVector != null)
      {
	infoVector.setSize(0);
	infoVector = null;
      }

    if (templates != null)
      {
	// it is critical that we don't setSize(0) on templates,
	// as it is belongs to Loader.  we can drop our reference
	// to it, though.

	templates = null;
      }

    progressBar = null;
    gbl = null;
    gbc = null;
  }

  /**
   * <p>Convenience method.</p>
   */

  private final void setStatus(String s)
  {
    gc.setStatus(s);
  }
}
