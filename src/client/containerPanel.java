/*

    containerPanel.java

    This is the container for all the information in a field.  Used in window Panels.

    Created:  11 August 1997
    Version: $Revision: 1.52 $ %D%
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
 * ContainerPanel is the basic building block of the ganymede client.  Each containerPanel
 * displays a single db_object, and allows the user to edit or view each db_field in the
 * object.  containerPanel loops through the fields of the object, adding the appropriate
 * type of input for each field.  This includes text fields, number fields, boolean fields,
 * and string selector fields(fields that can have multiple values.
 */

public class containerPanel extends JPanel implements ActionListener, JsetValueCallback, ItemListener{  

  boolean debug = false;
  static final boolean debug_persona = false;

  // -- 
  
  private boolean 
    keepLoading = true;

  gclient
    gc;			// our interface to the server

  db_object
    object;			// the object we're editing
  
  windowPanel
    winP;			// for interacting with our containing context

  protected framePanel
    frame;

  Vector
    vectorPanelList = new Vector();

  Hashtable
    shortToComponentHash,
    rowHash, 
    objectHash;
  
  //  TableLayout 
  // layout;

  GridBagLayout
    gbl;
  
  GridBagConstraints
    gbc;
  
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
	setStatus("Could not get object.  Someone else might be editting it.  Try again at a later time.");
	return;
      }

    this.winP = window;
    this.object = object;
    this.editable = editable;
    this.frame = frame;
    this.progressBar = progressBar;

    if (loadNow)
      {
	load();
      }
  }
  
  public void load() 
  {

    if (loaded)
      {
	System.out.println("Container panel is already loaded!");
	return;
      }

    try
      {
	gc.registerNewContainerPanel(this);


	if (debug)
	  {
	    System.out.println("Loading container panel");
	  }

	shortToComponentHash = new Hashtable();
	objectHash = new Hashtable();
	rowHash = new Hashtable();

	gbl = new GridBagLayout();
	gbc = new GridBagConstraints();
    
	setLayout(gbl);
	gbc.anchor = GridBagConstraints.NORTHWEST;
	gbc.insets = new Insets(4,4,4,4);
    
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

	    if (progressBar != null)
	      {
		progressBar.setValue(1);
	      }
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Could not get the fields: " + rx);
	  }

	Short Type = new Short(type);

	templates = gc.getTemplateVector(Type);

	if (progressBar != null)
	  {
	    progressBar.setValue(2);
	  }

	try
	  {
	    infoVector = object.getFieldInfoVector(true);  // Just gets the custom ones
	  }
	catch (RemoteException rx)
	  {
	    throw new RuntimeException("Could not get FieldInfoVector: " + rx);
	  }

	if (progressBar != null)
	  {
	    progressBar.setMaximum(infoVector.size());
	    progressBar.setValue(3);
	  }

	if (debug)
	  {
	    System.out.println("Entering big loop");
	  }
      
	if (templates != null)
	  {
	    int infoSize = infoVector.size();
	    FieldInfo fieldInfo = null;
	    FieldTemplate fieldTemplate = null;
	    int tSize = templates.size();
			
	    for (int i = 0; i < infoSize ; i++)
	      {
		if (keepLoading)
		  {

		    if (progressBar != null)
		      {
			progressBar.setValue(i + 4);
		      }
		
		    try
		      {
			// Skip some fields.  custom panels hold the built ins, and a few others.
			fieldInfo = (FieldInfo)infoVector.elementAt(i);
			// Find the template
			boolean found = false;
		    	short ID = fieldInfo.getID();


			for (int k = 0; k < tSize; k++)
			  {
			    fieldTemplate = (FieldTemplate)templates.elementAt(k);
			    if (fieldTemplate.getID() == ID)
			      {
				found = true;
				break;
			      }
			  }
		
			if (! found)
			  {
			    throw new RuntimeException("Could not find the template for this field: " + 
						       fieldInfo.getField());
			  }
		    
			if (((type== SchemaConstants.OwnerBase) && (ID == SchemaConstants.OwnerObjectsOwned)) 
			    ||  (ID == SchemaConstants.BackLinksField)
			    || ((type == SchemaConstants.UserBase) && (ID == SchemaConstants.UserAdminPersonae))
			    || ((ID == SchemaConstants.ContainerField) && object.isEmbedded()))
			  {
			    if (debug)
			      {
				System.out.println("Skipping a special field: " + fieldTemplate.getName());
			      }
			  }
			else
			  {
			    addFieldComponent(fieldInfo.getField(), fieldInfo, fieldTemplate);
			  }
		      }
		    catch (RemoteException ex)
		      {
			throw new RuntimeException("caught remote exception adding field " + ex);
		      }
		  }
		else
		  {
		    // Yikes, we have to stop loading...
		    gc.containerPanelFinished(this);
		    break;
		  }
	    
	      }
	  }
    
	if (debug)
	  {
	    System.out.println("Done with loop");
	  }

	//setViewportView(panel);

	setStatus("Finished loading containerPanel");
      }
    finally
      {
	loaded = true;
	gc.containerPanelFinished(this);// Is this twice?
      }
  }

  public boolean isLoaded()
  {
    return loaded;
  }

  public void stopLoading()
  {
    System.out.println("Stopping the load");
    if (isLoaded())
      {
	return;
      }

    keepLoading = false;

  }

  /**
   * Goes through all the components and checks to see if they should be visible,
   * and updates their contents.
   *
   */

  public void updateAll()
  {
    if (debug)
      {
	System.out.println("Updating container panel");
      }

    gc.setWaitCursor();

    Enumeration enum = objectHash.keys();

    while (enum.hasMoreElements())
      {
	updateComponent((Component)enum.nextElement());
      }
  }

  /**
   * update some of the fields.
   *
   * @fields Vector of Shorts, field ID's
   */
  public void update(Vector fields)
  {
    if (debug)
      {
	System.out.println("Updating a few fields...");
      }

    for (int i = 0; i < fields.size(); i++)
      {
	Component c = (Component)shortToComponentHash.get(fields.elementAt(i));
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
  }

  public void updateComponent(Component comp)
    {
      if (debug)
	{
	  System.out.println("Updating: " + comp);
	}

      try
	{
	  db_field field = (db_field)objectHash.get(comp);
	  if (field == null)
	    {
	      System.out.println("Fiel dis null, skipping.");
	      return;
	    }

	  setRowVisible(comp, field.isVisible());

	  if (comp instanceof JstringField)
	    {
	      ((JstringField)comp).setText((String)field.getValue());
	    }
	  else if (comp instanceof JdateField)
	    {
	      ((JdateField)comp).setDate((Date)field.getValue());
	    }
	  else if (comp instanceof JnumberField)
	    {
	      Integer value = (Integer)field.getValue();
	      ((JnumberField)comp).setText((value == null) ? "" : value.toString());
	    }
	  else if (comp instanceof JcheckboxField)
	    {
	      Boolean value = (Boolean)field.getValue();
	      ((JcheckboxField)comp).setSelected((value == null) ? false : value.booleanValue());
	    }
	  else if (comp instanceof JCheckBox)
	    {
	      Boolean value = (Boolean)field.getValue();

	      ((JCheckBox)comp).setSelected((value == null) ? false : value.booleanValue());
	    }
	  else if ((comp instanceof JComboBox) || (comp instanceof JInvidChooser))
	    {
	      if (comp instanceof JComboBox)
		{
		  ((JComboBox)comp).removeItemListener(this);
		}
	      else
		{
		  ((JInvidChooser)comp).removeItemListener(this);
		}

	      if (debug)
		{
		  System.out.println("Updating the combo box.");
		}
	      
	      //First we need to rebuild to list

	      Vector choiceHandles = null;
	      
	      Object key = null;
	      if (field instanceof string_field)
		{
		  key = ((string_field)field).choicesKey();
		}
	      else if (field instanceof invid_field)
		{
		  key = ((invid_field)field).choicesKey();
		}
	      
	      if (key == null)
		{
		  if (debug)
		    {
		      System.out.println("key is null, getting new copy, not caching.");
		    }
		  if (field instanceof string_field)
		    {
		      choiceHandles = ((string_field)field).choices().getListHandles();
		    }
		  else if (field instanceof invid_field)
		    {
		      choiceHandles = ((invid_field)field).choices().getListHandles();
		    }
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
		      
		      QueryResult choicesV = null;
		      
		      if (field instanceof invid_field)
			{
			  choicesV = ((invid_field)field).choices();
			}
		      else if (field instanceof string_field)
			{
			  choicesV = ((string_field)field).choices();
			}
		      
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

	      Object o = field.getValue();

	      if (o instanceof String)
		{
		  if (debug)
		    {
		      System.out.println("o is a String");
		    }

		  JComboBox cb = (JComboBox)comp;
		  cb.removeAllItems();

		  // add choices to combo box.. remember that the choices are
		  // sorted coming out of the object Cache

		  for (int i = 0; i < choiceHandles.size(); i++)
		    {
		      cb.addItem(((listHandle)choiceHandles.elementAt(i)).getLabel());
		    }
		    
		  cb.addItem("<none>");

		  // Do I need to check to make sure that this one is possible?

		  cb.setSelectedItem((String)o);
		}
	      else if (o instanceof Invid)
		{
		  if (debug)
		    {
		      System.out.println("o is an Invid");
		    }


		  JInvidChooser cb = (JInvidChooser)comp;
		  cb.removeAllItems();

		  for (int i = 0; i < choiceHandles.size(); i++)
		    {
		      cb.addItem((listHandle)choiceHandles.elementAt(i));
		    }
		  
		  cb.addItem(new listHandle("<none>", null));


		  // Still need to rebuild list here.
		  listHandle lh = new listHandle(gc.getSession().viewObjectLabel((Invid)o), o);
		  cb.setSelectedItem(lh);
		}
	      else
		{
		  // This might be null.  Which means we should choose <none>.  But do
		  // we choose (string)<none> or (listHandle)<none>?

		  if (field instanceof string_field)
		    {
		      ((JComboBox)comp).setSelectedItem("<none>");
		    }
		  else if (field instanceof invid_field)
		    {
		      ((JInvidChooser)comp).setSelectedItem(new listHandle("<none>", null));
		    }
		  else
		    {
		      System.out.println("I am not expecting this type in JComboBox: " + field);
		    }
		}
	      if (comp instanceof JComboBox)
		{
		  ((JComboBox)comp).addItemListener(this);
		}
	      else
		{
		  ((JInvidChooser)comp).addItemListener(this);
		}
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
	  else 
	    {
	      System.err.println("field of unknown type: " + comp);
	    }
	}
      catch (RemoteException rx)
	{
	  throw new RuntimeException("Could not check visibility");
	}
      
      if (debug)
	{
	  System.out.println("Done updating container panel");
	}
      
      gc.setNormalCursor();
    }

  /*
   *
   * This method does causes the hierarchy of containers above
   * us to be recalculated from the bottom (us) on up.  Normally
   * the validate process works from the top-most container down,
   * which isn't what we want at all in this context.
   *
   * Don't use this.
   *

  public void invalidateRight()
  {
    Component c;

    c = this;

    while ((c != null) && !(c instanceof JViewport))
      {
	System.out.println("contianer panel doLayout on " + c);

	c.doLayout();
	c = c.getParent();
      }
  }
  */

  public boolean setValuePerformed(JValueObject v)
  {
    try
      {
	if (v.getOperationType() == JValueObject.ERROR)
	  {
	    gc.showErrorMessage((String)v.getValue());
	    return true;
	  }

	ReturnVal returnValue = null;

	/* -- */

	if (v.getSource() instanceof JstringField)
	  {
	    if (debug)
	      {
		System.out.println((String)v.getValue());
	      }
	    db_field field = (db_field)objectHash.get(v.getSource());

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
	else if (v.getSource() instanceof JnumberField)
	  {

	    if (debug)
	      {
		System.out.println("Jnumberfirled changed.");
	      }
	
	
	    db_field field = (db_field)objectHash.get(v.getSource());

	    try
	      {
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
	    if (debug)
	      {
		System.out.println((String)v.getValue());
	      }

	    pass_field field = (pass_field)objectHash.get(v.getSource());

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
	else if (v.getSource() instanceof JdateField)
	  {
	    if (debug)
	      {
		System.out.println("date field changed");
	      }

	    db_field field = (db_field)objectHash.get(v.getSource());

	    try
	      {
		returnValue =  field.setValue(v.getValue());
	      }
	    catch (RemoteException rx)
	      {
		throw new IllegalArgumentException("Could not set field value: " + rx);
	      }
	  }
	else if (v.getSource() instanceof vectorPanel)
	  {
	    System.out.println("Something happened in the vector panel");
	  }
	else if (v.getSource() instanceof StringSelector)
	  {
	    if (debug)
	      {
		System.out.println("value performed from StringSelector");
	      }
	
	    if (v.getOperationType() == JValueObject.PARAMETER)
	      {
		if (debug)
		  {
		    System.out.println("MenuItem selected in a StringSelector");
		  }
		String command = (String)v.getParameter();

		if (command.equals("Edit object"))
		  {
		    if (debug)
		      {
			System.out.println("Edit object: " + v.getValue());
		      }

		    if (v.getValue() instanceof listHandle)
		      {
			Invid invid = (Invid)((listHandle)v.getValue()).getObject();
		    
			gc.editObject(invid);
		      }
		    else if (v.getValue() instanceof Invid)
		      {
			if (debug)
			  {
			    System.out.println("It's an invid!");
			  }
		    
			Invid invid = (Invid)v.getValue();
		    
			gc.editObject(invid);
		      }

		    returnValue = null;
		  }
		else if (command.equals("View object"))
		  {
		    if (debug)
		      {
			System.out.println("View object: " + v.getValue());
		      }

		    if (v.getValue() instanceof Invid)
		      {
			Invid invid = (Invid)v.getValue();
		    
			gc.viewObject(invid);
		      }

		    returnValue = null;
		  }
		else if (command.equals("Create new Object"))
		  {
		    if (objectHash.get(v.getSource()) instanceof invid_field)
		      {
			String label = null;
			invid_field field = (invid_field)objectHash.get(v.getSource());
			try
			  {
			    short type = field.getTargetBase();
			    db_object o = gc.createObject(type, false);

			    // Find the label field.
			    db_field f = o.getLabelField();
			    if ((f != null) && (f instanceof string_field))
			      {
				if (debug)
				  {
				    System.out.println("Going to get label for this object.");
				  }
				// Set up a label for this object.
				DialogRsrc r = new DialogRsrc(gc, "Choose Label for Object", "What would you like to name this object?", "Ok", "Cancel");
				r.addString("Label:");
			    
				StringDialog d = new StringDialog(r);
				Hashtable result = d.DialogShow();
				if (result == null)
				  {
				// They pushed cancel.
				    return false;
				  }
			    
				ReturnVal setLabel = f.setValue((String)result.get("Label:"));

				// wizard?

				setLabel = gc.handleReturnVal(setLabel);

				if ((setLabel == null) || setLabel.didSucceed())
				  {
				    label = (String)result.get("Label:");

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

			    Invid invid = o.getInvid();
			    returnValue = field.addElement(invid);

			    gc.showNewlyCreatedObject(o, invid, new Short(type));
			
			    if ((returnValue == null) || returnValue.didSucceed())
			      {
				if (debug)
				  {
				    System.out.println("--Adding it to the StringSelector");
				  }
				if (label == null)
				  {
				    ((StringSelector)v.getSource()).addNewItem(new listHandle("New item", invid), true);
				  }
				else
				  {
				    ((StringSelector)v.getSource()).addNewItem(new listHandle(label, invid), true);
				  }
			      }
			    else
			      {
				System.out.println("--Something went wrong, so I am NOT adding it to the StringSelector");
			      }
			  }
			catch (RemoteException rx)
			  {
			    throw new RuntimeException("Exception creating new object from SS menu: " + rx);
			  }
		      }
		    else
		      {
			System.out.println("I don't know what this is supposed to be: " + v.getSource());
		      }
		  }
		else
		  {
		    System.out.println("Unknown action command from popup: " + command);
		  }
	      }

	    else if (v.getValue() instanceof Invid)
	      {
		db_field field = (db_field)objectHash.get(v.getSource());

		if (field == null)
		  {
		    throw new RuntimeException("Could not find field in objectHash");
		  }

		Invid invid = (Invid)v.getValue();
		int index = v.getIndex();

		try
		  {
		    if (v.getOperationType() == JValueObject.ADD)
		      {
			if (debug)
			  {
			    System.out.println("Adding new value to string selector");
			  }
			returnValue = (field.addElement(invid));
		      }
		    else if (v.getOperationType() == JValueObject.DELETE)
		      {
			if (debug)
			  {
			    System.out.println("Removing value from field(strig selector)");
			  }
			returnValue = (field.deleteElement(invid));
		      }
		  }
		catch (RemoteException rx)
		  {
		    throw new RuntimeException("Could not change owner field: " + rx);
		  }
	      }
	    else if (v.getValue() instanceof String)
	      {
		if (objectHash.get(v.getSource()) instanceof string_field)
		  {
		    string_field field = (string_field)objectHash.get(v.getSource());
		
		    if (v.getOperationType() == JValueObject.ADD)
		      {
			try
			  {
			    returnValue = field.addElement((String)v.getValue());
			  }
			catch (RemoteException rx)
			  {
			    throw new RuntimeException("Could not add string to string_field: " + rx);
			  }
		      }
		    else if (v.getOperationType() == JValueObject.DELETE)
		      {
			try
			  {
			    returnValue = field.deleteElement((String)v.getValue());
			  }
			catch (RemoteException rx)
			  {
			    throw new RuntimeException("Could not remove string from string_field: " + rx);
			  }
		      }
		  }
		else if (objectHash.get(v.getSource()) instanceof invid_field)
		  {
		    invid_field field = (invid_field)objectHash.get(v.getSource());

		
		    if (v.getOperationType() == JValueObject.ADD)
		      {
			System.out.println("I see what's going on here!  You want to add a new Invid, and you want me to make it for you.");
			return false;
			/*
			  try
			  {
			  returnValue = field.addElement((String)v.getValue());
			  }
			  catch (RemoteException rx)
			  {
			  throw new RuntimeException("Could not add string to string_field: " + rx);
			  }*/
		      }
		    else if (v.getOperationType() == JValueObject.DELETE)
		      {
			System.out.println("This doesn't make sense, I don't think the SS should be giving me Strings to delete out of an Invid field.");
			return false;
		      }
		
		  }
		else
		  {
		    throw new IllegalArgumentException("What kind of field is this? : " + objectHash.get(v.getSource()));
		  }
	      }
	    else
	      {
		System.out.println("Not an Invid in string selector.");
	      }
	  }
	else if (v.getSource() instanceof JIPField)
	  {
	    if (debug)
	      {
		System.out.println("ip field changed");
	      }

	    db_field field = (db_field)objectHash.get(v.getSource());

	    try
	      {
		returnValue =  field.setValue(v.getValue());
	      }
	    catch (RemoteException rx)
	      {
		throw new IllegalArgumentException("Could not set ip field value: " + rx);
	      }
	  }
	else
	  {
	    System.out.println("Value performed from unknown source");
	  }

	// Check to see if anything needs updating.

	returnValue = gc.handleReturnVal(returnValue);

	if (returnValue == null)  // Success, no need to do anything else
	  {
	    gc.somethingChanged();
	    return true;
	  }

	checkReturnValForRescan(returnValue);
    
	System.out.println("Finished checking, going to return now.");

	if (returnValue.didSucceed())
	  {
	    System.out.println("Returning true.");
	    return true;
	  }
	else
	  {
	    System.out.println("Returning false.");
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


  public void actionPerformed(ActionEvent e)
  {
    ReturnVal returnValue = null;
    db_field field = null;

    if (e.getSource() instanceof JCheckBox)
      {
	field = (db_field)objectHash.get(e.getSource());
	  
	if (field == null)
	  {
	    System.out.println("Whoa, null field for a JCheckBox: " + e);
	    return;
	  }
	else
	  {
	    try
	      {
		returnValue = field.setValue(new Boolean(((JCheckBox)e.getSource()).isSelected()));
	      }
	    catch (RemoteException rx)
	      {
		throw new IllegalArgumentException("Could not set field value: " + rx);
	      }
	  }
      }
    else
      {
	System.err.println("Unknown ActionEvent in containerPanel");
      }
    
    // Check to see if anything needs updating.
    returnValue = gc.handleReturnVal(returnValue);

    if (returnValue == null)
      {
	gc.somethingChanged();
      }
    else if (returnValue.didSucceed())
      {
	gc.somethingChanged();
	checkReturnValForRescan(returnValue);
      }
    else
      {
	if (e.getSource() instanceof JCheckBox)
	  {
	    try
	      {
		JCheckBox cb = (JCheckBox)e.getSource();
		
		// This really should revert the thing.  But that will trigger another actionPerformed...
		cb.removeActionListener(this);
		// If field is not null, then set the state to whatever the field says it should be.
		// If field is null, flip the state.  The field should never be null, but what the hey.
		cb.setSelected((field == null) ? !cb.isSelected() : ((Boolean)field.getValue()).booleanValue());
		cb.addActionListener(this);
	      }
	    catch (RemoteException rx)
	      {
		throw new RuntimeException("couldn't get the value of a measly boolean field: " + rx);
	      }
	  }

	checkReturnValForRescan(returnValue);
      }
  }

  public void itemStateChanged(ItemEvent e)
  {
    ReturnVal returnValue = null;

    if (debug)
      {
	System.out.println("Item changed: " + e.getItem());
      }

    if (e.getSource() instanceof JComboBox)
      {
	if (e.getStateChange() == ItemEvent.SELECTED)
	  {
	    db_field field = (db_field)objectHash.get(e.getSource());

	    try
	      {
		boolean ok = false;
		Object item = e.getItem();
		if (item instanceof String)
		  {
		    returnValue = field.setValue((String)e.getItem());
		  }
		else if (item instanceof listHandle)
		  {
		    listHandle lh = (listHandle)item;
		    if (debug)
		      {
			if (field == null)
			  {
			    System.out.println("Field is null.");
			  }
		      }

		    returnValue = field.setValue(((Invid)lh.getObject() ));

		  }
		else 
		  {
		    System.out.println("Unknown type from JComboBox: " + item);
		  }

		returnValue = gc.handleReturnVal(returnValue);

		if (returnValue == null)
		  {
		    gc.somethingChanged();;
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
		    // Some kind of reversion?
		    checkReturnValForRescan(returnValue);
		  }
	    
	      }
	    catch (RemoteException rx)
	      {
		throw new RuntimeException("Could not set combo box value: " + rx);
	      }
	  }
      }
    else
      {
	System.out.println("Not from a JCombobox");
      }
  }

  void checkReturnValForRescan(ReturnVal rv)
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


  void addVectorRow(Component comp, int row, String label, boolean visible)
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
  
  void addRow(Component comp, int row, String label, boolean visible)
  {
    JLabel l = new JLabel(label);
    rowHash.put(comp, l);

    //comp.setBackground(ClientColor.ComponentBG);

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

  void setRowVisible(Component comp, boolean b)
  {
    Component c = (Component) rowHash.get(comp);

    if (c == null)
      {
	return;
      }

    comp.setVisible(b);
    c.setVisible(b);

    //invalidateRight();
  }

  /**
   *
   * Helper method to add a component during constructor operation
   *
   */

  private void addFieldComponent(db_field field, FieldInfo fieldInfo, FieldTemplate fieldTemplate) throws RemoteException
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
	System.out.println("Name: " + fieldTemplate.getName() + " Field type desc: " + fieldType);
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
	    System.err.println("Could not get field information");
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

  private void addStringVector(string_field field, FieldInfo fieldInfo,
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

	    //	    if (editable && fieldInfo.isEditable())
	    //{
		ss.setCallback(this);
		//}

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

	    //if (editable && fieldInfo.isEditable())
	    //{
		ss.setCallback(this);
		//}

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

  private void addInvidVector(invid_field field, FieldInfo fieldInfo, FieldTemplate fieldTemplate) throws RemoteException
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
    //if (editable && fieldInfo.isEditable())
    //  {
	ss.setCallback(this);
	//}
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

  private void addVectorPanel(db_field field, FieldInfo fieldInfo, FieldTemplate fieldTemplate) throws RemoteException
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

    addVectorRow( vp, templates.indexOf(fieldTemplate), fieldTemplate.getName(), fieldInfo.isVisible());
    
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

  private void addInvidField(invid_field field, FieldInfo fieldInfo, FieldTemplate fieldTemplate) throws RemoteException
  {
    objectList list;

    /* -- */

    if (fieldTemplate.isEditInPlace())
      {
	if (debug)
	  {
	    System.out.println("Hey, " + fieldTemplate.getName() + " is edit in place but not a vector, what gives?");
	  }

	addRow(new JLabel("edit in place non-vector"), templates.indexOf(fieldTemplate), fieldTemplate.getName(), fieldInfo.isVisible());
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

	    String label = (String)gc.getSession().view_db_object(thisInvid).getLabel();
	    //JstringField sf = new JstringField(20, false);
	    //sf.setText(label);

	    //JPanel p = new JPanel(new BorderLayout());
	    JButton b = new JButton(label);
	    b.addActionListener(new ActionListener() {
	      public void actionPerformed(ActionEvent e)
		{
		  getgclient().viewObject(thisInvid);
		}});

	    //p.add("Center", sf);
	    //p.add("West", b);
	    addRow(b, templates.indexOf(fieldTemplate), fieldTemplate.getName(), fieldInfo.isVisible());
	  }
	else
	  {
	    addRow( new JTextField("null invid"), templates.indexOf(fieldTemplate), fieldTemplate.getName(), fieldInfo.isVisible());
	  }
      }
  }

  /**
   *
   * private helper method to instantiate an ip field in this
   * container panel
   *
   */

  private void addIPField(ip_field field, FieldInfo fieldInfo, FieldTemplate fieldTemplate) throws RemoteException
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
		
    addRow( ipf, templates.indexOf(fieldTemplate), fieldTemplate.getName(), fieldInfo.isVisible());
    
  }

  public final gclient getgclient()
  {
    return gc;
  }
}
