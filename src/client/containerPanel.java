/*

    containerPanel.java

    This is the container for all the information in a field.  Used in window Panels.

    Created:  11 August 1997
    Version: $Revision: 1.55 $ %D%
    Module By: Michael Mulvaney
    Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.client;

import com.sun.java.swing.*;
import com.sun.java.swing.event.*;


import java.awt.*;
import java.beans.*;
import java.awt.event.*;
import java.rmi.*;
import java.util.*;

import arlut.csd.ganymede.*;

import arlut.csd.JDataComponent.*;
import arlut.csd.JDialog.*;
import arlut.csd.Util.VecQuickSort;

/*------------------------------------------------------------------------------
                                                                           class
                                                                  containerPanel

------------------------------------------------------------------------------*/

/**
 * ContainerPanel is the basic building block of the ganymede client.
 * Each containerPanel displays a single db_object, and allows the
 * user to edit or view each db_field in the object.  containerPanel
 * loops through the fields of the object, adding the appropriate type
 * of input for each field.  This includes text fields, number fields,
 * boolean fields, and string selector fields(fields that can have
 * multiple values.  
 */

public class containerPanel extends JPanel implements ActionListener, JsetValueCallback, ItemListener{  

  boolean debug = false;
  static final boolean debug_persona = false;

  // ---
  
  private boolean 
    keepLoading = true;

  gclient
    gc;				// our interface to the server

  db_object
    object;			// the object we're editing
  
  windowPanel
    winP;			// for interacting with our containing context

  protected framePanel
    frame;

  Vector
    vectorPanelList = new Vector();

  Hashtable
    shortToComponentHash = new Hashtable(),	// maps field id's to AWT/Swing component
    rowHash = new Hashtable(), 
    objectHash = new Hashtable();
  
  GridBagLayout
    gbl = new GridBagLayout();
  
  GridBagConstraints
    gbc = new GridBagConstraints();
  
  Vector 
    infoVector = null,
    templates = null;

  //int row = 0;			// we'll use this to keep track of rows added as we go along

  boolean
    editable;

  JProgressBar
    progressBar;

  boolean
    isEmbedded,
    loaded = false;

  short 
    type;

  /* -- */

  /**
   *
   * Constructor for containerPanel
   *
   * @param object   The object to be displayed
   * @param editable If true, the fields presented will be enabled for editing
   * @param parent   Parent gclient of this container
   * @param window   windowPanel containing this containerPanel
   * @param frame    framePanel holding this containerPanel(although this cp is not necessarily in the "General" tab)
   *
   */

  public containerPanel(db_object    object,
			boolean      editable, 
			gclient      gc,
			windowPanel  window,
			framePanel   frame)
  {
    this(object, editable, gc, window, frame, null, true);
  }

  /**
   *
   * Constructor for containerPanel
   *
   * @param object   The object to be displayed
   * @param editable If true, the fields presented will be enabled for editing
   * @param parent   Parent gclient of this container
   * @param window   windowPanel containing this containerPanel
   * @param frame    framePanel holding this containerPanel
   * @param progressBar JProgressBar to be updated, can be null
   */

  public containerPanel(db_object object, 
			boolean editable, 
			gclient gc, 
			windowPanel window, 
			framePanel frame, 
			JProgressBar progressBar)
  {
    this(object, editable, gc, window, frame, progressBar, true);
  }

  /**
   *
   * Main constructor for containerPanel
   *
   * @param object   The object to be displayed
   * @param editable If true, the fields presented will be enabled for editing
   * @param parent   Parent gclient of this container
   * @param window   windowPanel containing this containerPanel
   * @param progressBar JProgressBar to be updated, can be null
   * @param loadNow  If true, container panel will be loaded immediately
   *
   */

  public containerPanel(db_object object, 
			boolean editable, 
			gclient gc,			
			windowPanel window, 
			framePanel frame, 
			JProgressBar progressBar, 
			boolean loadNow)
  {
    super(false);

    /* -- */

    this.gc = gc;
    debug = gc.debug;

    if (object == null)
      {
	System.err.println("null object passed to containerPanel");
	setStatus("Could not get object.  Someone else might be editing it.  Try again at a later time.");
	return;
      }

    this.winP = window;
    this.object = object;
    this.editable = editable;
    this.frame = frame;
    this.progressBar = progressBar;

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
   *
   * This method downloads all necessary information from the server
   * about the object being viewed or edited.  Typically this is called
   * when the containerPanel is initialized by the containerPanel
   * constructor, but we defer loading when we are placed in a vector
   * panel hierarchy.
   *
   */
  
  public void load() 
  {
    int infoSize;

    FieldInfo 
      fieldInfo = null;

    FieldTemplate
      fieldTemplate = null;

    short ID;

    /* -- */

    if (loaded)
      {
	System.err.println("Container panel is already loaded!");
	return;
      }

    if (debug)
      {
	System.out.println("Loading container panel");
      }
    
    try
      {
	// Let the gclient object know about us, so that it can
	// tell us to stop loading if the user hits cancel

	gc.registerNewContainerPanel(this);

	// if we are a top-level container panel in a general pane
	// or persona pane, we'll have a progress bar.. we'll want
	// to update it as we go along loading field information.

	if (progressBar != null)
	  {
	    progressBar.setMinimum(0);
	    progressBar.setMaximum(10);
	    progressBar.setValue(0);
	  }

	// Get the list of fields

	if (debug)
	  {
	    System.out.println("Getting list of fields");
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

	templates = gc.getTemplateVector(type);

	if (templates == null)
	  {
	    setStatus("No fields defined for this object type.. error.");
	    return;
	  }

	setProgressBar(2);

	// ok, got the list of field definitions.  Now we need to
	// get the current values and visibility information for
	// the fields in this object.

	try
	  {
	    infoVector = object.getFieldInfoVector(true);  // Just gets the custom ones
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Could not get FieldInfoVector: " + rx);
	  }

	// now we know how many fields are actually present in this
	// object, we can set the max size of the progress bar

	if (progressBar != null)
	  {
	    progressBar.setMaximum(infoVector.size());
	    progressBar.setValue(3);
	  }

	if (debug)
	  {
	    System.out.println("Entering big loop");
	  }
      
	infoSize = infoVector.size();
			
	for (int i = 0; i < infoSize; i++)
	  {
	    // let the gclient interrupt us

	    if (!keepLoading)
	      {
		gc.containerPanelFinished(this);
		break;
	      }

	    setProgressBar(i + 4);
		
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
		    
		if (((type== SchemaConstants.OwnerBase) && (ID == SchemaConstants.OwnerObjectsOwned)) 
		    ||  (ID == SchemaConstants.BackLinksField)
		    || ((type == SchemaConstants.UserBase) && (ID == SchemaConstants.UserAdminPersonae))
		    || ((ID == SchemaConstants.ContainerField) && object.isEmbedded()))
		  {
		    if (debug)
		      {
			System.out.println("Skipping a special field: " + fieldTemplate.getName());
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
	    System.out.println("Done with loop");
	  }

	setStatus("Finished loading containerPanel");
      }
    finally
      {
	loaded = true;
	gc.containerPanelFinished(this);// Is this twice?
      }
  }

  /**
   *
   * Helper method to keep the load() method clean.
   *
   */

  private final void setProgressBar(int count)
  {
    if (progressBar != null)
      {
	progressBar.setValue(count);
      }
  }

  /**
   *
   * Helper method to keep the load() method clean.
   *
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
   *
   * This is a convenience method for other client classes to access
   * our gclient reference.
   *
   */

  public final gclient getgclient()
  {
    return gc;
  }

  /**
   *
   * This method returns true if this containerPanel has already
   * been loaded.
   *
   */

  public boolean isLoaded()
  {
    return loaded;
  }

  /**
   *
   * This method allows the gclient that contains us to set a flag
   * that will interrupt the load() method.<br><br>
   *
   * Note that this method must not be synchronized.
   *
   */

  public void stopLoading()
  {
    keepLoading = false;
  }

  /**
   *
   * Goes through all the components and checks to see if they should be visible,
   * and updates their contents.
   *
   */

  public void updateAll()
  {
    Enumeration enum;

    /* -- */

    if (debug)
      {
	System.out.println("Updating container panel");
      }

    gc.setWaitCursor();

    enum = objectHash.keys();

    while (enum.hasMoreElements())
      {
	updateComponent((Component)enum.nextElement());
      }

    invalidate();
    frame.validate();

    gc.setNormalCursor();
  }

  /**
   *
   * This method is used to update a subset of the fields in this
   * containerPanel.
   *
   * @param fields Vector of Shorts, field ID's
   */

  public void update(Vector fields)
  {
    Component c;

    /* -- */

    if (debug)
      {
	System.out.println("Updating a few fields...");
      }

    gc.setWaitCursor();

    for (int i = 0; i < fields.size(); i++)
      {
	c = (Component) shortToComponentHash.get(fields.elementAt(i));

	if (c == null)
	  {
	    System.out.println("Could not find this component: ID = " + (Short)fields.elementAt(i));
	  }
	else
	  {
	    updateComponent(c);
	  }
      }

    invalidate();
    frame.validate();

    gc.setNormalCursor();
  }

  /**
   *
   * This method updates the contents and visibility status of
   * a component in this containerPanel.
   *
   * @param comp An AWT/Swing component that we need to refresh
   *
   */

  private void updateComponent(Component comp)
  {
    if (debug)
      {
	System.out.println("Updating: " + comp);
      }

    try
      {
	db_field field = (db_field) objectHash.get(comp);

	if (field == null)
	  {
	    System.out.println("-----Field is null, skipping.");
	    return;
	  }

	// if the field is not visible, just hide it and 
	// return.. otherwise, set it visible and update
	// the value and choices for the field

	if (!field.isVisible())
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

	    ((JstringField)comp).setText((String)field.getValue());
	  }
	else if (comp instanceof JdateField)
	  {
	    // we don't need to worry about turning off callbacks
	    // here because JdateField only sends callbacks on
	    // focus loss

	    ((JdateField)comp).setDate((Date)field.getValue());
	  }
	else if (comp instanceof JnumberField)
	  {
	    Integer value = (Integer)field.getValue();

	    // we don't need to worry about turning off callbacks
	    // here because JnumberField only sends callbacks on
	    // focus loss

	    ((JnumberField)comp).setText((value == null) ? "" : value.toString());
	  }
	else if (comp instanceof JCheckBox)
	  {
	    Boolean value = (Boolean)field.getValue();
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
		System.out.println("Updating the combo box.");
	      }

	    // First we need to rebuild the list of choices

	    Vector choiceHandles = null;
	    Object key = sf.choicesKey();

	    // if our choices key is null, we're not going to use a cached copy..
	    // pull down a new list of choices for this field.

	    if (key == null)
	      {
		choiceHandles = sf.choices().getListHandles();
	      }
	    else
	      {
		if (debug)
		  {
		    System.out.println("key = " + key);
		  }
		  
		if (gc.cachedLists.containsList(key))
		  {
		    if (debug)
		      {
			System.out.println("key in there, using cached list");
		      }
		      
		    choiceHandles = gc.cachedLists.getListHandles(key, false);
		  }
		else
		  {
		    if (debug)
		      {
			System.out.println("It's not in there, downloading a new one.");
		      }
		      
		    QueryResult choicesV = sf.choices();

		    // if we got a null result, assume we have no choices,
		    // otherwise we're going to cache this result
		      
		    if (choicesV == null)
		      {
			choiceHandles = new Vector();
		      }
		    else
		      {
			gc.cachedLists.putList(key, choicesV);
			choiceHandles = choicesV.getListHandles();
		      }
		  }
	      }

	    String o = (String) sf.getValue();

	    // remove all the current values, add the choices that we
	    // just got

	    cb.removeAllItems();

	    // add choices to combo box.. remember that the choices are
	    // sorted coming out of the object Cache
	    
	    for (int i = 0; i < choiceHandles.size(); i++)
	      {
		cb.addItem(((listHandle)choiceHandles.elementAt(i)).getLabel());
	      }

	    // we're assuming here that none is a valid choice.. we shouldn't
	    // really assume this, should we?
	    
	    cb.addItem("<none>");

	    // and select the current value
	    
	    cb.setSelectedItem(o);

	    // put us back on as an item listener so we are live for updates
	    // from the user again
	    
	    cb.addItemListener(this);
	  }
	else if (comp instanceof JInvidChooser)
	  {
	    JInvidChooser chooser = (JInvidChooser) comp;
	    invid_field invf = (invid_field) field;

	    /* -- */

	    // remove this as an item listener so we don't get tricked
	    // into thinking this update came from the user

	    chooser.removeItemListener(this);

	    if (debug)
	      {
		System.out.println("Updating the combo box.");
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
		    System.out.println("key is null, getting new copy, not caching.");
		  }

		choiceHandles = invf.choices().getListHandles();
	      }
	    else
	      {
		if (debug)
		  {
		    System.out.println("key = " + key);
		  }
		  
		if (gc.cachedLists.containsList(key))
		  {
		    if (debug)
		      {
			System.out.println("key in there, using cached list");
		      }
		      
		    choiceHandles = gc.cachedLists.getListHandles(key, false);
		  }
		else
		  {
		    if (debug)
		      {
			System.out.println("It's not in there, downloading a new one.");
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
			choiceHandles = choicesV.getListHandles();
		      }
		  }
	      }

	    Invid o = (Invid) invf.getValue();
	    
	    chooser.removeAllItems();

	    for (int i = 0; i < choiceHandles.size(); i++)
	      {
		chooser.addItem((listHandle)choiceHandles.elementAt(i));
	      }

	    // we're assuming here that none is a valid choice.. we shouldn't
	    // really assume this, should we?

	    listHandle none = new listHandle("<none>", null);
		  
	    chooser.addItem(none);

	    // We assume that the choices list *does not include*
	    // the currently selected value, unless o is null.
	    // Even if the choices list does include the currently
	    // selected value, the JInvidChooser, which is derived
	    // from the JComboBox, shouldn't freak out too much.
	    // If it ever does, we might need to take care here to
	    // make sure that we don't create a new one if the
	    // value is already in the list of choices.
	    
	    if (o == null)
	      {
		chooser.setSelectedItem(none);
	      }
	    else
	      {
		listHandle lh = new listHandle(gc.getSession().viewObjectLabel((Invid)o), o);
		chooser.setSelectedItem(lh);
	      }

	    // put us back on as an item listener so we are live for updates
	    // from the user again

	    chooser.addItemListener(this);
	  }
	else if (comp instanceof JLabel)
	  {
	    ((JLabel)comp).setText((String)field.getValue());
	  }
	else if (comp instanceof JpassField)
	  {
	    System.out.println("Passfield, ingnoring");
	  }
	else if (comp instanceof StringSelector)
	  {
	    System.out.println("Skipping over StringSelector.");
	  }
	else if (comp instanceof vectorPanel)
	  {
	    System.out.println("VectorPanel: anything to do?");
	  }
	else 
	  {
	    System.err.println("field of unknown type: " + comp);
	  }
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not check visibility in updateComponent: " + rx);
      }
    
    if (debug)
      {
	System.out.println("Done updating container panel");
      }
  }

  /**
   *
   * This method comprises the JsetValueCallback interface, and is how
   * the customized data-carrying components in this containerPanel
   * notify us when something changes. <br><br>
   *
   * Note that we don't use this method for checkboxes, or comboboxes.
   *
   * @see arlut.csd.JDataComponent.JsetValueCallback
   * @see arlut.csd.JDataComponent.JValueObject
   *
   * @return false if the JDataComponent that is calling us should
   * reject the value change operation and revert back to the prior
   * value.
   * 
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

    try
      {
	// ok, now we have to connect the field change report coming
	// from the JDataComponent to the appropriate field object
	// on the Ganymede server.  First we'll try the simplest,
	// generic case.

	if ((v.getSource() instanceof JstringField) ||
	    (v.getSource() instanceof JnumberField) ||
	    (v.getSource() instanceof JIPField) ||
	    (v.getSource() instanceof JdateField))
	  {
	    db_field field = (db_field) objectHash.get(v.getSource());

	    /* -- */

	    try
	      {
		if (debug)
		  {
		    System.out.println(field.getTypeDesc() + " trying to set to " + v.getValue());
		  }

		returnValue = field.setValue(v.getValue());
	      }
	    catch (RemoteException rx)
	      {
		System.out.println("Could not set field value: " + rx);
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
		    System.out.println(field.getTypeDesc() + " trying to set to " + v.getValue());
		  }

		returnValue = field.setPlainTextPass((String)v.getValue());
	      }
	    catch (RemoteException rx)
	      {
		System.out.println("Could not set field value: " + rx);
		return false;
	      }
 
	  }
	else if (v.getSource() instanceof vectorPanel)
	  {
	    System.out.println("Something happened in the vector panel");
	  }
	else if (v.getSource() instanceof StringSelector)
	  {
	    StringSelector sourceComponent = (StringSelector) v.getSource();

	    /* -- */

	    if (debug)
	      {
		System.out.println("value performed from StringSelector");
	      }

	    // a StringSelector data component could be feeding us any of a
	    // number of conditions, that we need to check.

	    // First, are we being given a menu operation from StringSelector?
	
	    if (v.getOperationType() == JValueObject.PARAMETER)
	      {
		if (debug)
		  {
		    System.out.println("MenuItem selected in a StringSelector");
		  }

		String command = (String) v.getParameter();

		if (command.equals("Edit object"))
		  {
		    if (debug)
		      {
			System.out.println("Edit object: " + v.getValue());
		      }

		    Invid invid = (Invid) v.getValue();
		    
		    gc.editObject(invid);

		    return true;
		  }
		else if (command.equals("View object"))
		  {
		    if (debug)
		      {
			System.out.println("View object: " + v.getValue());
		      }

		    Invid invid = (Invid) v.getValue();
		    
		    gc.viewObject(invid);

		    return true;
		  }
		else if (command.equals("Create new Object"))
		  {
		    String label = null;
		    invid_field field = (invid_field) objectHash.get(sourceComponent);
		    db_object o;
		    db_field f;
		    short type;
		    Hashtable result;
		    Invid invid;

		    /* -- */

		    // We are being told to create a new object from an invid field.
		    
		    try
		      {
			// We first check to see if the target of the invid field is known..

			type = field.getTargetBase();

			// if we don't know what kind of target to create, we can't do it

			if (type < 0)
			  {
			    return false;
			  }

			// otherwise, try to go ahead and create the object
			
			o = gc.createObject(type, false);

			// Some objects have label fields pre-chosen.. if this is one
			// of those, we'll want to prompt for the label from the
			// user to make our tree handling clean

			f = o.getLabelField();

			if (f != null && (f instanceof string_field))
			  {
			    DialogRsrc r;

			    /* -- */

			    if (debug)
			      {
				System.out.println("Going to get label for this object.");
			      }

			    // ask the user what label they want for this object

			    r = new DialogRsrc(gc, 
					       "Choose Label for Object", 
					       "What would you like to name this object?", 
					       "Ok", 
					       "Cancel");
			    r.addString("Label:");
			    result = (new StringDialog(r)).DialogShow();

			    if (result == null)
			      {
				return false; // They pushed cancel.
			      }
			    
			    // the setValue operation may trigger a wizard, so we wrap
			    // the f.setValue() call in a gc.handleReturnVal().

			    returnValue = gc.handleReturnVal(f.setValue(result.get("Label:")));

			    if (returnValue == null || returnValue.didSucceed())
			      {
				label = (String) result.get("Label:");
				
				if (debug)
				  {
				    System.out.println("The set label worked!");
				  }
			      }
			    else
			      {
				if (debug)
				  {
				    System.out.println("set label failed!!!!");
				  }
			      }
			  }
			else
			  {
			    label = "New Item";
			  }

			// we'll want to save the invid of the newly created object
			// for linking into this field, as well as for inserting
			// into our tree

			invid = o.getInvid();

			// update the tree
			
			gc.showNewlyCreatedObject(o, invid, new Short(type));

			// and do the link in.  handleReturnVal() will
			// once again handle displaying any wizards
			// for us

			returnValue = gc.handleReturnVal(field.addElement(invid));

			if (returnValue != null && !returnValue.didSucceed())
			  {
			    if (debug)
			      {
				System.out.println("Newly created object could not be linked!!!!");
			      }

			    // well, the object did get created, but
			    // the operation as a whole didn't
			    // succeed, so we'll return false

			    return false;
			  }
			else
			  {
			    // display the newly linked object in the string selector

			    sourceComponent.addNewItem(new listHandle(label, invid), true);
			  }
		      }
		    catch (RemoteException rx)
		      {
			throw new RuntimeException("Exception creating new object from SS menu: " + rx);
		      }
		  }
		else
		  {
		    System.out.println("Unknown action command from popup: " + command);
		  }
	      }
	    else if (v.getValue() instanceof Invid)
	      {
		// we assume this will work.. if we get a ClassCastException here,
		// there's something wrong in the client logic

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
			    System.out.println("Adding new value to string selector");
			  }

			returnValue = field.addElement(v.getValue());
		      }
		    else if (v.getOperationType() == JValueObject.DELETE)
		      {
			if (debug)
			  {
			    System.out.println("Removing value from field(strig selector)");
			  }

			returnValue = field.deleteElement(v.getValue());
		      }
		  }
		catch (RemoteException rx)
		  {
		    throw new RuntimeException("Could not change add/delete invid from field: " + rx);
		  }
	      }
	    else if (v.getValue() instanceof String)
	      {
		// we assume this will work.. if we get a ClassCastException here,
		// there's something wrong in the client logic

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
		    else if (v.getOperationType() == JValueObject.DELETE)
		      {
			returnValue = field.deleteElement(v.getValue());
		      }
		  }
		catch (RemoteException rx)
		  {
		    throw new RuntimeException("Could not add/remove string from string_field: " + rx);
		  }
	      }
	    else
	      {
		System.out.println("Not an Invid in string selector.");
	      }
	  }
	else
	  {
	    System.out.println("Value performed from unknown source");
	  }

	// Handle any wizards or error dialogs

	returnValue = gc.handleReturnVal(returnValue);

	if (returnValue == null)  // Success, no need to do anything else
	  {
	    if (debug)
	      {
		System.out.println("retVal is null: returning true");
	      }

	    gc.somethingChanged();
	    return true;
	  }

	if (returnValue.didSucceed())
	  {
	    if (debug)
	      {
		System.out.println("didSucceed: Returning true.");
	      }
	    
	    // whatever happened, it may have caused other fields in this object
	    // to need to be updated.  We take care of that here.
	    
	    checkReturnValForRescan(returnValue);

	    gc.somethingChanged();
	    return true;
	  }
	else
	  {
	    if (debug)
	      {
		System.out.println("didSucceed: Returning false.");
	      }

	    return false;
	  }
      }
    catch (IllegalArgumentException e)
      {
	System.out.println("IllegalArgumentException in containerPanel.setValuePerformed:\n " + e);
	return false;
      }
    catch (RuntimeException e)
      {
	System.out.println("RuntimeException in containerPanel.setValuePerformed:\n " + e);
	return false;
      }
  }

  /**
   *
   * Some of our components, most notably the checkboxes, don't
   * go through JDataComponent.setValuePerformed(), but instead
   * give us direct feedback.  Those we take care of here.
   *
   * @see java.awt.event.ActionListener
   *
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
    else
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

	// That checkbox may have triggered value changes elsewhere in
	// this object.. rescan them as needed.

	checkReturnValForRescan(returnValue);
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

  /**
   *
   * Some of our components, most notably the JComboBoxes, don't
   * go through JDataComponent.setValuePerformed(), but instead
   * give us direct feedback.  Those we take care of here.
   *
   * @see java.awt.event.ItemListener
   *
   */

  public void itemStateChanged(ItemEvent e)
  {
    ReturnVal returnValue = null;

    // we are only acting as an action listener for comboboxes..
    // we'll just throw a ClassCastException if this changes
    // and we haven't fixed this code to match.

    JComboBox cb = (JComboBox) e.getSource();

    /* -- */

    if (debug)
      {
	System.out.println("Item changed: " + e.getItem());
      }

    // We don't care about deselect reports

    if (e.getStateChange() != ItemEvent.SELECTED)
      {
	return;
      }

    db_field field = (db_field) objectHash.get(cb);

    if (field == null)
      {
	throw new RuntimeException("Whoa, null field for a JComboBox: " + e);
      }

    try
      {
	Object newValue = e.getItem();
	Object oldValue = field.getValue();

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
		    System.out.println("Field is null.");
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
		System.out.println("field setValue returned true");
	      }
	  }
	else if (returnValue.didSucceed())
	  {
	    if (debug)
	      {
		System.out.println("field setValue returned true!!");
	      }

	    checkReturnValForRescan(returnValue);
	  }
	else
	  {
	    // Failure.. need to revert the combobox

	    // turn off callbacks

	    cb.removeItemListener(this);

	    if (newValue instanceof String)
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
   *
   * This private method is used to handle updating any fields that
   * may have changed as a result of a succesful value change
   * operation on the server.  ReturnVal can encode a list of field
   * id's that we need to update.
   *  
   */

  private void checkReturnValForRescan(ReturnVal rv)
  {
    if (debug)
      {
	System.out.println("=checking return val for rescan");
      }

    if (rv == null)
      {
	return;
      }

    if (rv.doRescan())
      {
	if (debug)
	  {
	    System.out.println("=doRescan is true");
	  }

	if (rv.rescanAll())
	  {
	    if (debug)
	      {
		System.out.println("=rescanAll is true");
	      }

	    updateAll();
	  }
	else 
	  {
	    update(rv.getRescanList());
	  }
      }
    else if (debug)
      {
	System.out.println("=It's not a doRescan");
      }
  }

  /**
   *
   * This private method is used to insert a normal field component.
   *
   */

  private void addRow(Component comp, int row, String label, boolean visible)
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
    gbl.setConstraints(comp, gbc);
    add(comp);

    setRowVisible(comp, visible);
  }

  /**
   *
   * This private method toggles the visibility of a field component
   * and its label in this containerPanel.
   *
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
   *
   * Helper method to add a component during constructor operation.  This
   * is the top-level field component adding method.
   *
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
	System.out.println(" Name: " + fieldTemplate.getName() + " Field type desc: " + fieldType);
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
	    System.err.println("**** Could not get field information");
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
	    addRow( label, templates.indexOf(fieldTemplate), fieldTemplate.getName(), true);
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
	System.out.println("Adding StringSelector, its a vector of strings!");
      }

    if (field == null)
      {
	System.out.println("Hey, this is a null field! " + fieldTemplate.getName());
	return;
      }

    if (editable && fieldInfo.isEditable())
      {
	QueryResult qr = null;
	
	if (debug)
	  {
	    System.out.println("Getting choicesKey()");
	  }

	Object id = field.choicesKey();

	if (id == null)
	  {
	    if (debug)
	      {
		System.out.println("Key is null, Getting choices");
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
		    System.out.println("Getting QueryResult now");
		  }

		qr =field.choices();

		if (qr != null)
		  {
		    gc.cachedLists.putList(id, qr);
		    list = gc.cachedLists.getList(id);
		  }
	      }
	  }
    
	if (!keepLoading)
	  {
	    System.out.println("Stopping containerPanel in the midst of loading a StringSelector");
	    gc.containerPanelFinished(this);
	    return;
	  }

	if (list == null)
	  {
	    StringSelector ss = new StringSelector(null,
						   (Vector)fieldInfo.getValue(), 
						   this,
						   editable && fieldInfo.isEditable(),
						   false,  // canChoose
						   false,  // mustChoose
						   160);

	    objectHash.put(ss, field);
	    shortToComponentHash.put(new Short(fieldInfo.getID()), ss);

	    ss.setCallback(this);

	    addRow(ss, templates.indexOf(fieldTemplate), fieldTemplate.getName(), fieldInfo.isVisible()); 
	  }
	else
	  {
	    StringSelector ss = new StringSelector(list.getLabels(false),
						   (Vector)fieldInfo.getValue(), 
						   this,
						   editable && fieldInfo.isEditable(),
						   true,   // canChoose
						   false,  // mustChoose
						   160);
	    objectHash.put(ss, field);
	    shortToComponentHash.put(new Short(fieldInfo.getID()), ss);

	    ss.setCallback(this);

	    addRow(ss, templates.indexOf(fieldTemplate), fieldTemplate.getName(), fieldInfo.isVisible()); 
	  }
      }
    else  //not editable, don't need whole list of things
      {
	StringSelector ss = new StringSelector(null,
					       (Vector)fieldInfo.getValue(), 
					       this,
					       editable && fieldInfo.isEditable(),
					       false,   // canChoose
					       false,  // mustChoose
					       160);
	objectHash.put(ss, field);
	shortToComponentHash.put(new Short(fieldInfo.getID()), ss);
	addRow(ss, templates.indexOf(fieldTemplate), fieldTemplate.getName(), fieldInfo.isVisible()); 
      }
  }

  /**
   *
   * private helper method to instantiate an invid vector in this
   * container panel
   *
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
	System.out.println("Adding StringSelector, its a vector of invids!");
      }

    valueHandles = field.encodedValues().getListHandles();

    if (! keepLoading)
      {
	System.out.println("Stopping containerPanel in the midst of loading a StringSelector");
	gc.containerPanelFinished(this);
	return;
      }

    if (editable && fieldInfo.isEditable())
      {
	Object key = field.choicesKey();

	if (key == null)
	  {
	    if (debug)
	      {
		System.out.println("key is null, downloading new copy");
	      }

	    QueryResult choices = field.choices();

	    if (choices != null)
	      {
		choiceHandles = choices.getListHandles();
	      }
	    else
	      { 
		if (debug)
		  {
		    System.out.println("choicse is null");
		  }

		choiceHandles = null;
	      }
	  }
	else
	  {
	    if (debug)
	      {
		System.out.println("key= " + key);
	      }

	    if (gc.cachedLists.containsList(key))
	      {
		if (debug)
		  {
		    System.out.println("It's in there, using cached list");
		  }

		choiceHandles = gc.cachedLists.getListHandles(key, false);
	      }
	    else
	      {
		if (debug)
		  {
		    System.out.println("It's not in there, downloading anew.");
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
		    choiceHandles = list.getListHandles(false);
		  }

		// debuging stuff

		if (debug_persona)
		  {
		    System.out.println();
		    
		    for (int i = 0; i < choiceHandles.size(); i++)
		      {
			System.out.println(" choices: " + (listHandle)choiceHandles.elementAt(i));
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
	    System.out.println("Not editable, not downloading choices");
	  }
      }

    // ss is canChoose, mustChoose
    JPopupMenu invidTablePopup = new JPopupMenu();
    JMenuItem editO = new JMenuItem("Edit object");
    JMenuItem viewO = new JMenuItem("View object");
    JMenuItem createO = new JMenuItem("Create new Object");
    invidTablePopup.add(editO);
    invidTablePopup.add(viewO);
    invidTablePopup.add(createO);
    
    JPopupMenu invidTablePopup2 = new JPopupMenu();
    JMenuItem editO2 = new JMenuItem("Edit object");
    JMenuItem viewO2 = new JMenuItem("View object");
    JMenuItem createO2 = new JMenuItem("Create new Object");
    invidTablePopup2.add(editO2);
    invidTablePopup2.add(viewO2);
    invidTablePopup2.add(createO2);

    if (debug)
      {
	System.out.println("Creating StringSelector");
      }

    StringSelector ss = new StringSelector(choiceHandles, valueHandles, this, editable && fieldInfo.isEditable(), 
					   true, true, 160, "Selected", "Available",
					   invidTablePopup, invidTablePopup2);
    if (choiceHandles == null)
      {
	ss.setButtonText("Create");
      }

    objectHash.put(ss, field);
    shortToComponentHash.put(new Short(fieldInfo.getID()), ss);
    
    ss.setCallback(this);

    addRow( ss, templates.indexOf(fieldTemplate), fieldTemplate.getName(), fieldInfo.isVisible()); 
  }


  private final void setStatus(String s)
  {
    gc.setStatus(s);
  }

  /**
   *
   * private helper method to instantiate a vector panel in this
   * container panel
   *
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
	    System.out.println("Adding editInPlace vector panel");
	  }
	else
	  {
	    System.out.println("Adding normal vector panel");
	  }
      }

    vectorPanel vp = new vectorPanel(field, winP, editable && fieldInfo.isEditable(), isEditInPlace, this);
    vectorPanelList.addElement(vp);
    objectHash.put(vp, field);
    shortToComponentHash.put(new Short(fieldInfo.getID()), vp);

    addVectorRow( vp, templates.indexOf(fieldTemplate), fieldTemplate.getName(), fieldInfo.isVisible());
    
  }

  /**
   *
   * This private helper method is used to insert an entry in a vector panel.
   *
   */

  private void addVectorRow(Component comp, int row, String label, boolean visible)
  {
    JLabel l = new JLabel("");
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
   *
   * private helper method to instantiate a string field in this
   * container panel
   *
   */

  private void addStringField(string_field field, FieldInfo fieldInfo, FieldTemplate fieldTemplate) throws RemoteException
  {
    objectList
      list;

    JstringField
      sf;

    /* -- */

    if (field.canChoose())
      {
	if (debug)
	  {
	    System.out.println("You can choose");
	  }
	    
	JComboBox combo = new JComboBox();

	Vector choiceHandles = null;
	Vector choices = null;

	Object key = field.choicesKey();

	if (key == null)
	  {
	    if (debug)
	      {
		System.out.println("key is null, getting new copy.");
	      }

	    choices = field.choices().getLabels();
	  }
	else
	  {
	    if (debug)
	      {
		System.out.println("key = " + key);
	      }
		
	    if (gc.cachedLists.containsList(key))
	      {
		if (debug)
		  {
		    System.out.println("key in there, using cached list");
		  }
		
		list = gc.cachedLists.getList(key);
	      }
	    else
	      {
		if (debug)
		  {
		    System.out.println("It's not in there, downloading a new one.");
		  }
		
		gc.cachedLists.putList(key, field.choices());
		list = gc.cachedLists.getList(key);
	      }

	    choiceHandles = list.getListHandles(false);
	    choices = list.getLabels(false);
	  }    

	String currentChoice = (String) fieldInfo.getValue();
	boolean found = false;
	    
	for (int j = 0; j < choices.size(); j++)
	  {
	    String thisChoice = (String)choices.elementAt(j);
	    combo.addItem(thisChoice);
		
	    if (!found && (currentChoice != null))
	      {
		if (thisChoice.equals(currentChoice))
		  {
		    found = true;
		  }
	      }
		
	    /*if (debug)
	      {
	      System.out.println("Adding " + (String)choices.elementAt(j));
	      }*/
	  }
	    
	// if the current value wasn't in the choice, add it in now
	    
	if (!found && (currentChoice != null))
	  {
	    combo.addItem(currentChoice);
	  }
	    
	combo.setMaximumRowCount(8);
	combo.setMaximumSize(new Dimension(Integer.MAX_VALUE,20));

	try
	  {
	    boolean mustChoose = field.mustChoose();
	    combo.setEditable(mustChoose); // this should be setEditable(mustChoose());

	    if (debug)
	      {
		System.out.println("Setting editable to + " + mustChoose);
	      }
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Could not check to see if field was mustChoose.");
	  }

	//combo.setVisible(true);  // This line is not necessary, right?
	    
	if (currentChoice != null)
	  {
	    combo.setSelectedItem(currentChoice);
	  }

	if (debug)
	  {
	    System.out.println("Setting current value: " + currentChoice);
	  }	  

	if (editable && fieldInfo.isEditable())
	  {
	    combo.addItemListener(this); // register callback
	  }

	objectHash.put(combo, field);
	shortToComponentHash.put(new Short(fieldInfo.getID()), combo);
	if (debug)
	  {
	    System.out.println("Adding to panel");
	  }
	    
	addRow( combo, templates.indexOf(fieldTemplate), fieldTemplate.getName(), fieldInfo.isVisible());
	    	    
      }
    else
      {
	// It's not a choice
      
	sf = new JstringField(20,
			      field.maxSize(),
			      //new JcomponentAttr(null,
			      //			 new Font("Helvetica",Font.PLAIN,12),
			      //			 Color.black,Color.white),
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

	sf.setToolTipText(fieldTemplate.getComment());

	addRow( sf, templates.indexOf(fieldTemplate), fieldTemplate.getName(), fieldInfo.isVisible());
      }
  }

  /**
   *
   * private helper method to instantiate a password field in this
   * container panel
   *
   */

  private void addPasswordField(pass_field field, FieldInfo fieldInfo, FieldTemplate fieldTemplate) throws RemoteException
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
	  
	addRow( pf, templates.indexOf(fieldTemplate), field.getName(), field.isVisible());
	
      }
    else
      {
	sf = new JstringField(20,
			      field.maxSize(),
			      //      new JcomponentAttr(null,
				//		 new Font("Helvetica",Font.PLAIN,12),
			      //	 Color.black,Color.white),
			      true,
			      false,
			      null,
			      null);

	objectHash.put(sf, field);
	shortToComponentHash.put(new Short(fieldInfo.getID()), sf);
			  
	// the server won't give us an unencrypted password, we're clear here
			  
	sf.setText((String)fieldInfo.getValue());
	
		      
	sf.setEditable(false);

	sf.setToolTipText(fieldTemplate.getComment());
	
	addRow( sf, templates.indexOf(fieldTemplate), fieldTemplate.getName(), fieldInfo.isVisible());
	
      }
  }

  /**
   *
   * private helper method to instantiate a numeric field in this
   * container panel
   *
   */

  private void addNumericField(db_field field, FieldInfo fieldInfo, FieldTemplate fieldTemplate) throws RemoteException
  {
    if (debug)
      {
	System.out.println("Adding numeric field");
      }
      
    JnumberField nf = new JnumberField();

			      
    objectHash.put(nf, field);
    shortToComponentHash.put(new Short(fieldInfo.getID()), nf);
	
		      
    Integer value = (Integer)fieldInfo.getValue();
    if (value != null)
      {
	nf.setValue(value.intValue());
      }

    if (debug)
      {
	System.out.println("Editable: " + editable  + " isEditable: " +fieldInfo.isEditable());
      }
    
    if (editable && fieldInfo.isEditable())
      {
	nf.setCallback(this);
      }

    nf.setEditable(editable && fieldInfo.isEditable());
    nf.setColumns(20);
    
    nf.setToolTipText(fieldTemplate.getComment());
    
    addRow( nf, templates.indexOf(fieldTemplate), fieldTemplate.getName(), fieldInfo.isVisible());
  
    
  }

  /**
   *
   * private helper method to instantiate a date field in this
   * container panel
   *
   */

  private void addDateField(db_field field, FieldInfo fieldInfo, FieldTemplate fieldTemplate) throws RemoteException
  {
    JdateField df = new JdateField();
		      
    objectHash.put(df, field);
    shortToComponentHash.put(new Short(fieldInfo.getID()), df);
    df.setEditable(editable && fieldInfo.isEditable());

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

    addRow( df, templates.indexOf(fieldTemplate), fieldTemplate.getName(), fieldInfo.isVisible());
  }

  /**
   *
   * private helper method to instantiate a boolean field in this
   * container panel
   *
   */

  private void addBooleanField(db_field field, FieldInfo fieldInfo, FieldTemplate fieldTemplate) throws RemoteException
  {
    //JcheckboxField cb = new JcheckboxField();

    JCheckBox cb = new JCheckBox();
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
	    System.out.println("Null pointer setting selected choice: " + ex);
	  }
      }

    addRow( cb, templates.indexOf(fieldTemplate), fieldTemplate.getName(), fieldInfo.isVisible());
    
  }

  /**
   *
   * private helper method to instantiate a permission matrix field in this
   * container panel
   *
   */

  private void addPermissionField(db_field field, FieldInfo fieldInfo, FieldTemplate fieldTemplate) throws RemoteException
  {
    if (debug)
      {
	System.out.println("Adding perm matrix");
      }

    // note that the permissions editor does its own callbacks to
    // the server, albeit using our transaction / session.

    Invid invid = object.getInvid(); // server call

    perm_button pb = new perm_button((perm_field) field,
				     editable && fieldInfo.isEditable(),
				     gc,
				     false);
    
    addRow( pb, templates.indexOf(fieldTemplate), fieldTemplate.getName(), fieldInfo.isVisible());
    
  }

  /**
   *
   * private helper method to instantiate an invid field in this
   * container panel
   *
   */

  private void addInvidField(invid_field field, 
			     FieldInfo fieldInfo, 
			     FieldTemplate fieldTemplate) throws RemoteException
  {
    objectList list;

    /* -- */

    if (fieldTemplate.isEditInPlace())
      {
	// this should never happen
	
	if (debug)
	  {
	    System.out.println("Hey, " + fieldTemplate.getName() +
			       " is edit in place but not a vector, what gives?");
	  }

	addRow(new JLabel("edit in place non-vector"), 
	       templates.indexOf(fieldTemplate), 
	       fieldTemplate.getName(), 
	       fieldInfo.isVisible());

	return;
      }

    if (editable && fieldInfo.isEditable())
      {
	Object key = field.choicesKey();
	
	Vector choices = null;

	if (key == null)
	  {
	    if (debug)
	      {
		System.out.println("key is null, not using cache");
	      }

	    list = new objectList(field.choices());
	    choices = list.getListHandles(false);
	  }
	else
	  {
	    if (debug)
	      {
		System.out.println("key = " + key);
	      }

	    if (gc.cachedLists.containsList(key))
	      {
		if (debug)
		  {
		    System.out.println("Got it from the cachedLists");
		  }

		list = gc.cachedLists.getList(key);
	      }
	    else
	      {
		if (debug)
		  {
		    System.out.println("It's not in there, downloading a new one.");
		  }

		gc.cachedLists.putList(key, field.choices());
		list = gc.cachedLists.getList(key);
	      }

	    choices = list.getListHandles(false);
	  }

        Invid currentChoice = (Invid) fieldInfo.getValue();
	listHandle currentListHandle = null;
	listHandle noneHandle = new listHandle("<none>", null);
	boolean found = false;
	JInvidChooser combo = new JInvidChooser(this, fieldTemplate.getTargetBase());
	
	/* -- */

	combo.addItem(noneHandle);
	
	choices = gc.sortListHandleVector(choices);

	for (int j = 0; j < choices.size(); j++)
	  {
	    listHandle thisChoice = (listHandle) choices.elementAt(j);
	    combo.addItem(thisChoice);
	    
	    if (!found && (currentChoice != null))
	      {
		if (thisChoice.getObject().equals(currentChoice))
		  {
		    if (debug)
		      {
			System.out.println("Found the current object in the list!");
		      }
		    currentListHandle = thisChoice;
		    found = true;
		  }
	      }
	    
	    /*	    if (debug)
	      {
		System.out.println("Adding " + (listHandle)choices.elementAt(j));
	      }*/
	  }
	
	// if the current value wasn't in the choice, add it in now
	
	if (!found)
	  {
	    if (currentChoice != null)
	      {
		currentListHandle = new listHandle(gc.getSession().viewObjectLabel(currentChoice), currentChoice);
		combo.addItem(currentListHandle);
	      }
	  }
	
	combo.setMaximumRowCount(12);
	combo.setMaximumSize(new Dimension(Integer.MAX_VALUE,20));
	combo.setEditable(false); // This should be true
	combo.setVisible(true);

	if (currentChoice != null)
	  {
	    if (debug)
	      {
		System.out.println("setting current choice: " + currentChoice);
	      }
	    combo.setSelectedItem(currentListHandle);
	  }
	else
	  {
	    if (debug)
	      {
		System.out.println("currentChoice is null");
	      }
	    combo.setSelectedItem(noneHandle);
	  }	  

	if (editable && fieldInfo.isEditable())
	  {
	    combo.addItemListener(this); // register callback
	  }

	objectHash.put(combo.getCombo(), field); // We do the itemStateChanged straight from the JComboBox in the JInvidChooser,
	objectHash.put(combo, field); // The update method still need to be able to find this JInvidChooser.
	
	shortToComponentHash.put(new Short(fieldInfo.getID()), combo);


	if (debug)
	  {
	    System.out.println("Adding to panel");
	  }
	
	addRow( combo, templates.indexOf(fieldTemplate), fieldTemplate.getName(), fieldInfo.isVisible());
	
      }
    else
      {
	if (fieldInfo.getValue() != null)
	  {
	    final Invid thisInvid = (Invid)fieldInfo.getValue();

	    String label = (String)gc.getSession().viewObjectLabel(thisInvid);

	    //JstringField sf = new JstringField(20, false);
	    //sf.setText(label);

	    if (label == null)
	      {
		System.out.println("-you don't have permission to view this object.");
		label = "Permission denied!";
	      }

	    //JPanel p = new JPanel(new BorderLayout());

	    JButton b = new JButton(label);

	    b.addActionListener(new ActionListener() {
	      public void actionPerformed(ActionEvent e)
		{
		  getgclient().viewObject(thisInvid);
		}});

	    //p.add("Center", sf);
	    //p.add("West", b);

	    addRow(b, 
		   templates.indexOf(fieldTemplate), 
		   fieldTemplate.getName(), 
		   fieldInfo.isVisible());
	  }
	else
	  {
	    addRow( new JTextField("null invid"), 
		    templates.indexOf(fieldTemplate), 
		    fieldTemplate.getName(), 
		    fieldInfo.isVisible());
	  }
      }
  }

  /**
   *
   * private helper method to instantiate an ip field in this
   * container panel
   *
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
	System.out.println("Adding IP field");
      }

    try
      {
	ipf = new JIPField(new JcomponentAttr(null,
					      new Font("Helvetica",Font.PLAIN,12),
					      Color.black,Color.white),
			   editable && fieldInfo.isEditable(),
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

    ipf.setToolTipText(fieldTemplate.getComment());
		
    addRow(ipf,
	   templates.indexOf(fieldTemplate), 
	   fieldTemplate.getName(), 
	   fieldInfo.isVisible());
  }
}
