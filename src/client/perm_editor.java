/*

   brian_editor.java

   Description.
   
   Created: 18 November 1998
   Version: $Revision: 1.10 $ %D%
   Module By: Brian O'Mara omara@arlut.utexas.edu
   Applied Research Laboratories, The University of Texas at Austin

*/

package arlut.csd.ganymede.client;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.rmi.RemoteException;

import jdj.PackageResources; 

import arlut.csd.ganymede.*;
import arlut.csd.JDataComponent.JSeparator;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;

/*------------------------------------------------------------------------------
                                                                           class 
                                                                     perm_editor

------------------------------------------------------------------------------*/

class brian_editor extends JDialog implements ActionListener, Runnable {

  boolean debug = false;

  boolean enabled;
  Session session;
  perm_field permField;
  PermMatrix matrix, templateMatrix;
  Vector rowVector;

  gclient gc;

  JButton OkButton = new JButton ("Ok");
  JButton CancelButton = new JButton("Cancel");
 
  boolean keepLoading = true;

  JProgressBar progressBar;
  JDialog progressDialog;
  JButton cancelLoadingButton;
  JScrollPane edit_pane;

  // Layout Stuff 
  JPanel 
    Choice_Buttons,
    waitPanel;
  GridBagLayout gbl = new GridBagLayout();
  GridBagConstraints gbc = new GridBagConstraints();

  /* -- */

  /**
   *
   * Constructor
   *
   * @param permField Who do we talk to to get the permissions?
   * @param enabled If false, it will not be possible to edit permissions, will just display
   * @param gc The gclient that connects us to the client-side schema caches
   * @param parent The frame we are attaching this dialog to
   * @param DialogTitle The title for this dialog box
   * @param justShowUser If true, this permissions editor will only show the user object.
   * This is used when editing the 'self permissions' object in the database.
   *
   */

  public brian_editor (perm_field permField, 
		       boolean enabled, gclient gc,
		       Frame parent, String DialogTitle,
		       boolean justShowUser) // need to get rid of justShowUser
  {
    super(parent, DialogTitle, true); // the boolean value is to make the dialog modal
    

    // Main constructor for the perm_editor window      

    this.session = session;
    this.permField = permField;
    this.enabled = enabled;
    this.gc = gc;
    
    if (!debug)
      {
	this.debug = gc.debug;
      }


    // Set up progress bar stuff

    progressBar = new JProgressBar();
    progressBar.setBorder(gc.emptyBorder10);
    progressBar.setMinimum(0);
    progressBar.setMaximum(29);
    progressBar.setValue(0);

    JPanel progressBarPanel = new JPanel();
    progressBarPanel.add(progressBar);

    waitPanel = new JPanel(new BorderLayout(5, 5));
    waitPanel.add("Center", progressBarPanel);
    waitPanel.add("South", new JSeparator());

    progressDialog = new JDialog(gc, "Loading permission editor", false);    
    progressDialog.getContentPane().setLayout(new BorderLayout(5, 5));
    progressDialog.getContentPane().add("Center", waitPanel);
    
    cancelLoadingButton = new JButton("Cancel");
    cancelLoadingButton.addActionListener(new ActionListener()
					  {
					    public void actionPerformed(ActionEvent e)
					      {
						keepLoading = false;
					      }
					  });

    JPanel cancelButtonPanel = new JPanel();
    cancelButtonPanel.add(cancelLoadingButton);
    progressDialog.getContentPane().add("South", cancelButtonPanel);

    JLabel loadingLabel = new JLabel("Loading permissions editor", SwingConstants.CENTER);
    loadingLabel.setBorder(gc.emptyBorder10);
    progressDialog.getContentPane().add("North", loadingLabel);

    Rectangle b = gc.getBounds();
    progressDialog.setLocation(b.width/2 + b.x - 75, b.height/2 + b.y - 50);
    progressDialog.pack();

    progressDialog.setVisible(true);

    Thread t = new Thread(this);
    t.start();
  }

  public void run() 
  {
    System.out.println("Starting thread");


    // Get a quick dump of the permission field's state

    try
      {
	this.matrix = permField.getMatrix();
	this.templateMatrix = permField.getTemplateMatrix();
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("couldn't get permission matrix\n" + ex.getMessage());
      }
    
    if (debug)
      {
	System.out.println("Starting Permissions Editor Initialization");
      }
    
    OkButton.addActionListener(this);
    CancelButton.addActionListener(this);
    OkButton.setBackground(Color.lightGray);
    CancelButton.setBackground(Color.lightGray);

    Choice_Buttons = new JPanel(); 
    Choice_Buttons.setLayout(new FlowLayout ());
    Choice_Buttons.add(OkButton);
    Choice_Buttons.add(CancelButton);

    progressBar.setValue(2);
    
    try 
      {

	rowVector = initRowVector();

	if (rowVector == null)
	  {
	    this.dispose();
	    return;
	  }
	
	System.out.println("rowVector initialized");

	if (debug)
	  {
	    System.err.println("got it, it " + (rowVector == null ? "is " : "isn't ") + "equal to null");
	  }
      }
    catch (RemoteException ex)
      {
	throw new RuntimeException("Caught RemoteException" + ex);
      }
    
    getContentPane().remove(waitPanel);
    getContentPane().setLayout(new BorderLayout());

    /* Initialize table w/ custom table model 
       and custom cell renderers, 
    */
    
    BPermTableModel permModel = new BPermTableModel(rowVector);
    JTable table = new JTable(permModel);

    // Set default column widths
 
    TableColumn column = null;
    for (int i = 0; i < 5; i++) {
      column = table.getColumnModel().getColumn(i);
      if (i == 0) {
	column.setPreferredWidth(180); 
      } else {
	column.setPreferredWidth(15);
      }
    }

    // Uses custom renderers for Strings and Booleans

    table.setDefaultRenderer(String.class,
			     new StringRenderer(rowVector));
    table.setDefaultRenderer(Boolean.class,
			     new BoolRenderer(rowVector));

    table.setShowHorizontalLines(false);
    //    table.getTableHeader().setReorderingAllowed(false);

    edit_pane = new JScrollPane(table);
    
    getContentPane().add("Center", edit_pane);
    getContentPane().add("South", Choice_Buttons);
    gc.setWaitCursor();
    
    progressDialog.setVisible(false);    
    progressDialog.dispose();
    
    this.myshow(true);
    gc.setNormalCursor();
  }
  
  /**
   * This method will create a vector of row info that will be used
   * to store the permissions values for the base and basefields
   */
  
  private Vector initRowVector() throws RemoteException 
  {
    PermEntry entry, templateEntry;
    BaseDump base;
    FieldTemplate template;
    Vector rows = new Vector();
    boolean create, view, edit, delete;
    boolean createOK, viewOK, editOK, deleteOK;
    boolean baseCreate, baseView, baseEdit, baseDelete;
    Vector fields;
    boolean visibleField;
    Enumeration enum;
    short id;
    String name;

    /* -- */

    // get a list of base types from the gclient.. we really just care
    // about their names and id's.

    enum = gc.getBaseList().elements();

    progressBar.setMaximum(gc.getBaseList().size() + 3);
    progressBar.setValue(3);

    int i = 3;

    while (enum.hasMoreElements())
      {
	if (! keepLoading)
	  {
	    // Cancel was pushed.
	    stopLoading();
	    return (Vector)null;
	  }

	progressBar.setValue(i++);

	base = (BaseDump) enum.nextElement();

	id = base.getTypeID();
	name = base.getName();

	if (debug)
	  {
	    System.err.println("init_hash: processing " + name);
	  }


	// retrieve the current permissions for this object type 
	
	entry = matrix.getPerm(id);
	
	if (entry == null)
	  {
	    baseCreate = baseView = baseEdit = baseDelete = false;
	  }
	else
	  {
	    baseCreate = entry.isCreatable();
	    baseView = entry.isVisible();
	    baseEdit = entry.isEditable();
	    baseDelete = entry.isDeletable();
	  }

	if (templateMatrix != null)
	  {
	    templateEntry = templateMatrix.getPerm(id);

	    // if permissions already exist on an entry, we'll allow
	    // it to be reduced.. it's conceivable that an admin might
	    // have authority to edit a role that he or she wouldn't
	    // normally have the authority to delegate.. of course,
	    // this wouldn't make a lot of sense since in theory the
	    // admin could just click on the delegatable checkbox and
	    // give him/herself the ability to delegate these perms,
	    // but we might conceivably give an admin permission to
	    // edit this perm field but not the delegatable checkbox
	    // in this object.

	    if (templateEntry == null)
	      {
		createOK = baseCreate;
		viewOK = baseView;
		editOK = baseEdit;
		deleteOK = baseDelete;
	      }
	    else
	      {
		createOK = templateEntry.isCreatable() || baseCreate;
		viewOK = templateEntry.isVisible() || baseView;
		editOK = templateEntry.isEditable() || baseEdit;
		deleteOK = templateEntry.isDeletable() || baseDelete;
	      }
	  }
	else
	  {
	    createOK = viewOK = editOK = deleteOK = true;
	  }


	// Initialize a PermRow object for this base and
	// add it to the rows vector 
	boolean[] basePermBitsAry = {baseView, baseCreate, baseEdit, baseDelete};
 	boolean[] basePermBitsOKAry = {viewOK, createOK, editOK, deleteOK};

	PermRow	basePermRow = new PermRow(base, null, basePermBitsAry, basePermBitsOKAry, enabled);

	rows.addElement(basePermRow);


	// Now go through the fields

	visibleField = baseView;

	fields = (Vector) gc.getTemplateVector(id);

	for (int j=0; fields != null && (j < fields.size()); j++) 
	  {
	    template = (FieldTemplate) fields.elementAt(j);

	    // we don't want to show built-in fields

	    if (template.isBuiltIn())
	      {
		continue;
	      }

	    // get the permission set for this field

	    entry = matrix.getPerm(id, template.getID());

	    if (entry == null)
	      {
		// if no permission is explicitly recorded for this
		// field, use the record for the base

		create = baseCreate;
		view = baseView;
		edit = baseEdit;
		delete = baseDelete;
	      }
	    else 
	      {
		create = entry.isCreatable();
		view = entry.isVisible();
		edit = entry.isEditable();
		delete = entry.isDeletable();
	      }

	    if (templateMatrix != null)
	      {
		templateEntry = templateMatrix.getPerm(id, template.getID());

		// if permissions already exist on an entry, we'll allow
		// it to be reduced.. it's conceivable that an admin might
		// have authority to edit a role that he or she wouldn't
		// normally have the authority to delegate.. of course,
		// this wouldn't make a lot of sense since in theory the
		// admin could just click on the delegatable checkbox and
		// give him/herself the ability to delegate these perms,
		// but we might conceivably give an admin permission to
		// edit this perm field but not the delegatable checkbox
		// in this object.
		
		if (templateEntry == null)
		  {
		    createOK = create;
		    viewOK = view;
		    editOK = edit;
		    deleteOK = delete;
		  }
		else
		  {
		    createOK = templateEntry.isCreatable() || create;
		    viewOK = templateEntry.isVisible() || view;
		    editOK = templateEntry.isEditable() || edit;
		    deleteOK = templateEntry.isDeletable() || delete;
		  }
	      }
	    else
	      {
		createOK = viewOK = editOK = deleteOK = true;
	      }


	    // Initialize a PermRow object for this field and
	    // add it to the rows vector 

	    boolean[] fieldPermBitsAry = {view, create, edit, delete};
	    boolean[] fieldPermBitsOKAry = {viewOK, createOK, editOK, deleteOK};

	    PermRow templatePermRow = new PermRow(base, template, fieldPermBitsAry, fieldPermBitsOKAry, visibleField);
	    
	    rows.addElement(templatePermRow);
	  }
      }
    
    return rows;
  }

  private void stopLoading()
  {
    if (progressDialog != null)
      {
	progressDialog.setVisible(false);
	progressDialog.dispose();
      }

    this.dispose();
  }

 
 
 
  /**
   *
   * Method to pop-up/pop-down the perm_editor
   *
   */
  
  public void myshow(boolean truth_value)
  {
    if (truth_value)
      {
	setSize(550,550);
      }

    setVisible(truth_value);
  }

  
  public void actionPerformed(ActionEvent e)
  {
    PermRow ref;
    short baseid;
    BaseDump bd;
    String baseName, templateName;
    FieldTemplate template;
    boolean view, edit, create, delete;
   
    /* -- */

    /* This method will either simply exit the perm_editor [if "ok" is 
     * the source], or will retore the original permission values [if the
     * "cancel" button is pushed]
     */
    
    if (e.getSource() == OkButton) 
      {
	if (debug)
	  {
	    System.out.println("Ok was pushed");
	  }

	Enumeration enum = rowVector.elements();
	
	while (enum.hasMoreElements()) {
	  ref = (PermRow)enum.nextElement();

	  if (!ref.isChanged()) {
	    continue;

	  } else {

	    gc.somethingChanged();	    

	    if (ref.isBase()) {
	      bd = (BaseDump) ref.getReference();
	      baseid = bd.getTypeID();
	      baseName = bd.getName();
	      
	      view = ref.isVisible();
	      create = ref.isCreatable();
	      edit = ref.isEditable();
	      delete = ref.isDeletable();

	      if (debug)
		{
		  System.err.println("setting base perms for " + baseName+ " ("+baseid+")");
		}

 
	      try
		{ 
		  // note that PermEntry bit order (V,E,C,D) is different from that used 
		  // everywhere else (V,C,E,D). Should see if PermEntry can be changed for consistency

		  gc.handleReturnVal(permField.setPerm(baseid, new PermEntry (view, edit, create, delete)));
		}
	      catch (RemoteException ex)
		{
		  throw new RuntimeException("Caught RemoteException" + ex);
		}
	    
	    } 
	    else  {
	      template = (FieldTemplate) ref.getReference();
	      templateName = template.getName();
	      
	      view = ref.isVisible();
	      create = ref.isCreatable();
	      edit = ref.isEditable();
	      delete = ref.isDeletable();

	      if (debug)
		{
		  System.err.println("setting basefield perms for field " + templateName);
		}
	      
	      
	      try
		{
		  // note that PermEntry bit order (V,E,C,D) is different from that used 
		  // everywhere else (V,C,E,D). Should see if PermEntry can be changed for consistency

		  gc.handleReturnVal(permField.setPerm(template.getBaseID(), template.getID(), 
						       new PermEntry (view, edit, create, delete)));
		}
	      catch (RemoteException ex)
		{
		  throw new RuntimeException("Caught RemoteException" + ex);
		}

	    }
	  }
	}	      
           
	myshow(false);
	return;

      }
    else 
      {
	if (debug)
	  {
	    System.out.println("Cancel was pushed");
	  }

	myshow(false);
	return;
      }    
  }
   
}

/*
  BPermTableModel

  This class describes and implements the table model for the permissions editor.
*/

class BPermTableModel extends AbstractTableModel {

  Vector rows; 
  Vector changedRowVector = new Vector();
  String[] columnNames = {"Name", 
			  "Visible",
			  "Creatable",
			  "Editable",
			  "Deletable"};

  static final int NAME = 0;
  static final int VISIBLE = 1;
  static final int CREATABLE = 2;
  static final int EDITABLE = 3;
  static final int DELETABLE = 4;

  public BPermTableModel(Vector rowVector) {
    this.rows = rowVector;
  } 
  
  public int getColumnCount() {
    return columnNames.length;
  }
  
  public int getRowCount() {
    return rows.size();
  }
  
  public String getColumnName(int col) {
    return columnNames[col];
  }
  
  public Object getValueAt(int row, int col) {

    PermRow myRow = (PermRow)rows.elementAt(row);

    switch(col) {
      
    case NAME: // This one is just for indent effect   
      if (myRow.isBase()) {
	BaseDump bd = (BaseDump)myRow.getReference();
	return " " + bd.getName();
      } 
      else {
	FieldTemplate ft = (FieldTemplate)myRow.getReference();
	return "   " + ft.getName();
      }
    
    case VISIBLE:
      return new Boolean(myRow.isVisible());
      
    case CREATABLE:
      return new Boolean(myRow.isCreatable());
      
    case EDITABLE:
      return new Boolean(myRow.isEditable());
      
    case DELETABLE:
      return new Boolean(myRow.isDeletable());
      
    default:
      return new Integer(col); // should be exception stuff here, I think.
    }
  }

  public Class getColumnClass(int col) {
    return getValueAt(0, col).getClass();
  }
  
  public boolean isCellEditable(int row, int col) {

      PermRow myRow = (PermRow)rows.elementAt(row);

      // name string or disabled checkbox is not editable  
      if ((getColumnClass(col) == String.class) || (!myRow.isEnabled()))  
	return false;

      // otherwise, if it's not an "X", it is editable  
      else { 
	
	switch(col) {
	  
	case VISIBLE:
	  if (myRow.canSeeView()) 
	    return true;
	  break;

	case CREATABLE:
	  if (myRow.canSeeCreate()) 
	    return true;
	  break;	  
	  
	case EDITABLE:
	  if (myRow.canSeeEdit()) 
	    return true;
	  break;
	  
	case DELETABLE:
	  if (myRow.canSeeDelete()) 
	    return true;
	  break;
	}
	return false;
      }
  }
  
  public void setValueAt(Object value, int row, int col) {
      PermRow myRow = (PermRow)rows.elementAt(row);

      switch(col) {
	
      case VISIBLE:
	myRow.setVisible((Boolean)value);
	myRow.setChanged(true);


	// If making a base selection
	// update children too 

	if (myRow.isBase()) {
	  setBaseChildren(row, VISIBLE, value);
	  

	  // Take care of visibility of creatable
	  // and editable children too

	  if (myRow.isCreatable()) {
	    setBaseChildren(row, CREATABLE, value);
	  }
	  
	  if (myRow.isEditable()) {
	    setBaseChildren(row, EDITABLE, value);
	  }
	  
	}
	break;
	
      case CREATABLE:
	myRow.setCreatable((Boolean)value);
	myRow.setChanged(true);
	// If base, update children too

	if ((myRow.isBase()) && (myRow.isVisible())) {
	  setBaseChildren(row, CREATABLE, value);
	}
	break;
	
      case EDITABLE:
	myRow.setEditable((Boolean)value);
	myRow.setChanged(true);
	// If base, update children too

	if ((myRow.isBase()) && (myRow.isVisible())) {
	  setBaseChildren(row, EDITABLE, value);
	}
	break;
	
      case DELETABLE:
	myRow.setDeletable((Boolean)value);
	myRow.setChanged(true);

	// No update of children for deletable
	break;
      }

      // Update table to reflect these changes
      fireTableDataChanged();
  }


  /* Programmatically updates status of children when a 
     base is toggled 
  */

  public void setBaseChildren(int row, int col, Object value) {
    
    for (int i = row+1; i<rows.size(); i++) {
      PermRow myRow = (PermRow) rows.elementAt(i);

      if (myRow.isBase()) { // stop updating at next base 
	break;
      } else {
	
	switch(col) {

	case VISIBLE:

	  // If checkbox not an "X", update it
	  if (myRow.canSeeView()) {
	    myRow.setVisible((Boolean)value);	    
	    myRow.setEnabled((Boolean)value);

	    // If View base was toggled to false
	    // then all children in each col are set to false
	    if (!myRow.isEnabled()) {

	      Boolean toFalse = new Boolean(false);

	      myRow.setVisible(toFalse);
	      myRow.setCreatable(toFalse);
	      myRow.setEditable(toFalse);
	      myRow.setDeletable(toFalse);
	    }

	    // Lets us know we need to write this row out
	    // when we're finished
	    myRow.setChanged(true);
	  }
	  break;

	case CREATABLE:
	  if (myRow.canSeeCreate()) {
	    myRow.setCreatable((Boolean)value);
	    myRow.setChanged(true);
	  }
	  break;

	case EDITABLE:
	  if (myRow.canSeeEdit()) {
	    myRow.setEditable((Boolean)value);
	    myRow.setChanged(true);
	  }
	  break;

	case DELETABLE:
	  if (myRow.canSeeDelete()){
	    myRow.setDeletable((Boolean)value);
	    myRow.setChanged(true);
	  }
	  break;
	}
      }
    }
  }
}


/* StringRenderer 

   Provides custom renderer for String class.
   Really only used to provide different colored backgrounds
   for bases and fields. 
*/

class StringRenderer extends JLabel
  implements TableCellRenderer {
  
  Vector rows;
  
  public StringRenderer(Vector rows) {
    super();
    this.rows = rows;
    setOpaque(true); 
  }
  
  public Component getTableCellRendererComponent(
						 JTable table, Object value, 
						 boolean isSelected, boolean hasFocus,
						 int row, int column) {
    
    PermRow myRow = (PermRow) rows.elementAt(row);

    // Bases are white, fields are gray
    if (myRow.isBase()) {
      setBackground(Color.white);
    }
    else {
      setBackground(Color.lightGray);
    }
    
    setText((String)table.getValueAt(row,column));
    
    return this;
  }
}


/* BoolRenderer

 Provides custom renderer for Boolean class. 
*/

class BoolRenderer extends JCheckBox
    implements TableCellRenderer {
  
  Vector rows;

  // This is a lighter gray than Color.lightGray
  Color lightGray2 = new Color(224,224,224); 

  // This is the "X" which replaces the checkbox
  ImageIcon noAccess = 
    new ImageIcon(PackageResources.getImageResource(this, "noaccess.gif", getClass()));

  public BoolRenderer(Vector rows) {
    super();
    this.rows = rows;
    setOpaque(true); //do this for background to show up.
  }
  
  public Component getTableCellRendererComponent(
						 JTable table, Object value, 
						 boolean isSelected, boolean hasFocus,
						 int row, int column) {
    
    PermRow myRow = (PermRow) rows.elementAt(row);
    Boolean selected = (Boolean)table.getValueAt(row,column);
    boolean enabled = myRow.isEnabled();
    
    String columnName = table.getColumnName(column);
    
    // Take care of bg colors- bases white, fields gray
    if (myRow.isBase()) {
      setBackground(Color.white);
    }
    else {
      setBackground(lightGray2);
    }

    // Check if viewOK, etc. If not, put
    // noAccess icon ("X") instead of checkbox


    // Visible Column    
    if (columnName.equals("Visible")) {

      if (myRow.canSeeView()) {
	setIcon(null);
	setEnabled(enabled);
	setSelected(selected.booleanValue());	
      } else {
	setIcon(noAccess);
	setEnabled(true);
      }

    }
    
    // Creatable Column
    else if (columnName.equals("Creatable")) {

      if (myRow.canSeeCreate()) {
	setIcon(null);
	setEnabled(enabled);
	setSelected(selected.booleanValue());	
      } else {
	setIcon(noAccess);
	setEnabled(true);
      }

    }
  
    // Editable Column
    else if (columnName.equals("Editable")) {

      if (myRow.canSeeEdit()) {
	setIcon(null);
	setEnabled(enabled);
	setSelected(selected.booleanValue());	
      } else {
	setIcon(noAccess);
	setEnabled(true);
      }

    }

    // Deletable Column
    else if (columnName.equals("Deletable")){

      if (myRow.canSeeDelete()) {
	setIcon(null);
	setEnabled(enabled);
	setSelected(selected.booleanValue());	
      } else {
	setIcon(noAccess);
	setEnabled(true);
      }

    }
  
    // Center checkbox
    setHorizontalAlignment(CENTER);   

    return this;
  }
}


class PermRow {

  private static final int VISIBLE = 0;
  private static final int CREATABLE = 1;
  private static final int EDITABLE = 2;
  private static final int DELETABLE = 3;

  private Object reference;
  private boolean[] permBitsAry;
  private boolean[] permBitsOKAry;
  private boolean enabled;
  private boolean changed;

  //
  // Constructor  
  //
  public PermRow(BaseDump base, FieldTemplate field, boolean[] permBitsAry, boolean[] permBitsOKAry, boolean enabled) {

    this.permBitsAry = permBitsAry;
    this.permBitsOKAry = permBitsOKAry;
    this.enabled = enabled;

    if (field == null) {   
      reference = base;
    } else {
      reference = field;
    }
    
  }

  
  //
  // Methods to check and set if row enabled
  //
  public boolean isEnabled() {
    return enabled;
  }
  
  public void setEnabled(Boolean value) {
    enabled = value.booleanValue();
  }


  //
  // Methods to check and set if row has changed
  //
  public boolean isChanged() {
    return changed;
  }
  
  public void setChanged(boolean value) {
    changed = value;
  }
  
  //
  // Methods to check and set values of perm bits
  //
  public boolean isVisible() {
    return permBitsAry[VISIBLE];
  }

  public void setVisible(Boolean value) {
    permBitsAry[VISIBLE] = value.booleanValue();
  }

  public boolean isCreatable() {
    return permBitsAry[CREATABLE];
  }
  
  public void setCreatable(Boolean value) {
    permBitsAry[CREATABLE] = value.booleanValue();
  }
  
  public boolean isEditable() {
    return permBitsAry[EDITABLE];
  }

  public void setEditable(Boolean value) {
    permBitsAry[EDITABLE] = value.booleanValue();
  }

  public boolean isDeletable() {
    return permBitsAry[DELETABLE];
  }
  
  public void setDeletable(Boolean value) {
    permBitsAry[DELETABLE] = value.booleanValue();
  }

  //
  // Methods to determine if you see checkbox or "X"
  //
  public boolean canSeeView() {
    return permBitsOKAry[VISIBLE];
  }

  public boolean canSeeCreate() {
    return permBitsOKAry[CREATABLE];
  }

  public boolean canSeeEdit() {
    return permBitsOKAry[EDITABLE];
  }

  public boolean canSeeDelete() {
    return permBitsOKAry[DELETABLE];
  }

  // Returns the current base or field object
  public Object getReference() {
    return reference;
  }

  // Returns if the current row is dealing w/ base or field
  public boolean isBase() {
    if (reference instanceof BaseDump)    
      return true;
    else
      return false;
  }

}
