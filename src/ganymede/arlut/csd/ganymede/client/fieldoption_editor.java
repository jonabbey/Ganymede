package arlut.csd.ganymede.client;

import java.awt.*;
import java.awt.event.*;
import java.rmi.RemoteException;
import java.util.*;

import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.table.*;
import javax.swing.border.*;

import arlut.csd.Util.PackageResources;
import arlut.csd.ganymede.common.*;
import arlut.csd.ganymede.rmi.*;

/**
 * This class displays the client "widget" that allows a user to edit the
 * field options for a particular builder task. It's modeled loosely after
 * the permissions editor widget, and uses the same TreeTable component.
 **/
class fieldoption_editor extends JDialog 
{
  /* Temporary hack: hard code the string values for the option field */
  public static String labels[] = {"Never", "When changed", "Always"};

  boolean debug = true;

  /* Flag that indicated whether or not the widget it currently being
   * displayed or not. */
  boolean isActive = true;

  /* Reference to the actual field option data store this widget represents */
  field_option_field opField;

  /* Should the widget be displayed read-only or not? */
  boolean editable;

  /* The root of our tree, which is in column 0 of our tree table */
  DefaultMutableTreeNode rowRootNode;

  /* Reference to the main client class */
  gclient gc;

  /* Layout components */
  JButton OkButton = new JButton ("Ok");
  JButton CancelButton = new JButton("Cancel");
  JButton ExpandButton = new JButton ("Expand All");
  JButton CollapseButton = new JButton("Collapse All");
  JScrollPane edit_pane;
  JTreeTable treeTable;
  JTree tree;
  JPanel 
    Base_Panel,
    Bordered_Panel,
    Choice_Buttons,
    Expansion_Buttons,
    All_Buttons;



  /**
   * @param Should the widget be read only?
   * @param gc The gclient that connects us to the client-side schema caches
   * @param parent The frame we are attaching this dialog to
   * @param DialogTitle The title for this dialog box
   */
  public fieldoption_editor (field_option_field opField, boolean editable, gclient gc,
		             Frame parent, String DialogTitle)
  {
    super(parent, DialogTitle, false); // the boolean value is to make the dialog nonmodal

    this.opField = opField;
    this.editable = editable;
    this.gc = gc;

    if (!debug)
      {
	this.debug = gc.debug;
      }

    this.addWindowListener(new WindowAdapter()
			   {
			     public void windowClosing(WindowEvent e)
			       {
				 myshow(false);
			       }
			   });

    /* Change the images to match the client */
    UIManager.put("Tree.leafIcon", new ImageIcon(PackageResources.getImageResource(this, "i043.gif", getClass())));
    UIManager.put("Tree.openIcon", new ImageIcon(PackageResources.getImageResource(this, "openfolder.gif", getClass())));
    UIManager.put("Tree.closedIcon", new ImageIcon(PackageResources.getImageResource(this, "folder.gif", getClass())));
    UIManager.put("Tree.expandedIcon", new ImageIcon(PackageResources.getImageResource(this, "minus.gif", getClass())));
    UIManager.put("Tree.collapsedIcon", new ImageIcon(PackageResources.getImageResource(this, "plus.gif", getClass())));

    /* Group the OK and Cancel buttons together */
    Choice_Buttons = new JPanel(); 
    Choice_Buttons.setLayout(new GridLayout(1,2));
    Choice_Buttons.setBorder(new EmptyBorder(new Insets(5,5,5,5)));
    Choice_Buttons.add(OkButton);
    Choice_Buttons.add(CancelButton);

    /* Group the Expand and Collapse buttons together */
    Expansion_Buttons = new JPanel(); 
    Expansion_Buttons.setBorder(new EmptyBorder(new Insets(5,5,5,5)));
    Expansion_Buttons.setLayout(new GridLayout(1,2));
    Expansion_Buttons.add(ExpandButton);
    Expansion_Buttons.add(CollapseButton);

    /* Now take *those* groups and group them together */
    All_Buttons = new JPanel();
    All_Buttons.setLayout(new BorderLayout());
    All_Buttons.add("West", Expansion_Buttons);
    All_Buttons.add("East", Choice_Buttons);

    /* Setup the tree table */
    try
      {
        rowRootNode = initRowTree();
	if (rowRootNode == null)
	  {
	    this.dispose();
	    return;
	  }
      }
    catch (RemoteException ex)
      {
        throw new RuntimeException(ex);
      }
    
    TreeTableModel model = new FieldOptionModel(rowRootNode);
    treeTable = new JTreeTable(model);
    tree = treeTable.getTree();
    treeTable.setDefaultRenderer(Integer.class, new DelegateRenderer((TreeTableModelAdapter)treeTable.getModel(), editable));
    treeTable.setDefaultEditor(Integer.class, new DelegateEditor((TreeTableModelAdapter)treeTable.getModel(), editable));

    /* Expand all visible base nodes on startup */
    expandAllNodes();
    
    edit_pane = new JScrollPane(treeTable);
    edit_pane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

    Base_Panel = new JPanel(); 
    Base_Panel.setLayout(new BorderLayout());
    Base_Panel.add("Center", edit_pane);
    Base_Panel.add("South", All_Buttons);

    Bordered_Panel = new JPanel();
    Bordered_Panel.setLayout(new BorderLayout());
    Bordered_Panel.add("Center", Base_Panel);

    getContentPane().setLayout(new BorderLayout());
    getContentPane().add("Center", Bordered_Panel);

    /* Register event handlers */
    OkButton.addActionListener(new ActionListener()
                               {
                                 public void actionPerformed(ActionEvent e)
                                   {
                                     commitChanges();
                                   }
                               });

    CancelButton.addActionListener(new ActionListener()
                                   {
                                     public void actionPerformed(ActionEvent e)
                                       {
                                         myshow(false);
                                       }
                                   });

    ExpandButton.addActionListener(new ActionListener()
				   {
				     public void actionPerformed(ActionEvent e)
				       {
                                         expandAllNodes();
				       }
				   });
 
    CollapseButton.addActionListener(new ActionListener()
				     {
				       public void actionPerformed(ActionEvent e)
					 {
                                           collapseAllNodes();
					 }
				     });
 
    gc.setWaitCursor();
    myshow(true);
    gc.setNormalCursor();
  }
  

  /**
   * This method will create a tree of row info that will be used
   * to store the field option values for the base and basefields.
   * It returns a DefaultMutableTreeNode as the root of the tree structure
   */
  private DefaultMutableTreeNode initRowTree() throws RemoteException 
  {
    FieldOptionMatrix matrix;
    BaseDump base;
    FieldTemplate template;
    int basevalue, fieldvalue;
    String entry;
    Vector fields;
    Enumeration en;
    short id;
    String name;
    DefaultMutableTreeNode rootNode, baseNode, fieldNode;

    matrix = opField.getMatrix();

    rootNode = new DefaultMutableTreeNode(new FieldOptionRow(null, null, 0));

    /* Get a list of base types from the gclient 
     * (we really just care about their names and id's)
     */
    en = gc.getBaseList().elements();

    while (en.hasMoreElements())
      {
	base = (BaseDump) en.nextElement();
	id = base.getTypeID();
	name = base.getName();

	if (debug)
	  {
	    System.err.println("init_hash: processing " + name);
	  }

	entry = matrix.getOption(id);
	
	if (entry == null)
	  {
	    basevalue = 0;
	  }
	else
	  {
            basevalue = Integer.parseInt(entry);
	  }

	baseNode = new DefaultMutableTreeNode(new FieldOptionRow(base, null, basevalue));
	rootNode.add(baseNode);

	/* Now add all of the fields for this base as child nodes */
	fields = (Vector) gc.getTemplateVector(id);

	for (int j=0; fields != null && (j < fields.size()); j++) 
	  {
	    /* get the field options for this field */
	    template = (FieldTemplate) fields.elementAt(j);
	    entry = matrix.getOption(id, template.getID());

	    if (entry == null)
	      {
		/* if no option is explicitly recorded for this
                 * field, use the record for the base */
                fieldvalue = basevalue;
	      }
	    else 
	      {
                /* FIXME: This assumes that the field options are stored in
                 * the database as Strings containing integers. At the moment,
                 * this is a totally hard-coded assumption. Presumably, this
                 * will be remedied with an interface that declares some
                 * constants or the like. */
                fieldvalue = Integer.parseInt(entry);
	      }

            /* Create a new child node for this field and add it to the node
             * for the DBObjectBase. */
	    fieldNode = new DefaultMutableTreeNode(new FieldOptionRow(base, template, fieldvalue));
	    baseNode.add(fieldNode);
	  }
      }
    
    return rootNode;
  }



  /**
   *
   * Method to pop-up/pop-down the editor
   *
   */
  public void myshow(boolean truth_value)
  {
    if (truth_value)
      {
	setSize(550,550);
        setVisible(true);
      } 
    else 
      {
        isActive = false;
	cleanUp();
      }
  }



  /**
   * Am I currently being displayed or not?
   */
  public boolean isActiveEditor()
  {
    return isActive;
  }


  /**
   * Expands all of the nodes in the JTree
   */
  private void expandAllNodes()
  {
    for (Enumeration e = (rowRootNode.children()); e.hasMoreElements();) {
      DefaultMutableTreeNode node = (DefaultMutableTreeNode)e.nextElement();
      FieldOptionRow myRow = (FieldOptionRow)node.getUserObject();
      TreePath path = new TreePath(node.getPath());
      tree.expandPath(path);
    }
  }


  
  /**
   * Collapses all of the nodes in the JTree
   */
  private void collapseAllNodes()
  {
    for (Enumeration en = (rowRootNode.children()); en.hasMoreElements();) 
      {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode)en.nextElement();
        TreePath path = new TreePath(node.getPath());
        tree.collapsePath(path);
      } 
  }



  /**
   * Writes all changes the user has made back to the data store.
   */
  public void commitChanges()
  {
    FieldOptionRow ref;
    short baseid;
    BaseDump bd;
    String baseName, templateName;
    FieldTemplate template;
    int value;
   
    if (debug)
      {
        System.out.println("Ok was pushed");
      }
    
    Enumeration en = rowRootNode.preorderEnumeration();
    
    while (en.hasMoreElements()) 
      {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode)en.nextElement();
        ref = (FieldOptionRow)node.getUserObject();
      
        if (ref.isChanged()) 
          {
            gc.somethingChanged();
        
            if (ref.isBase()) 
              {
                bd = (BaseDump) ref.getReference();
                baseid = bd.getTypeID();
                baseName = bd.getName();
          
                value = ref.getOptionValue();
          
                if (debug)
                  {
                    System.err.println("setting base perms for " + baseName+ " ("+baseid+")");
                  }
          
                try
                  { 
                    gc.handleReturnVal(opField.setOption(baseid, labels[value]));
                  }
                catch (RemoteException ex)
                  {
                    throw new RuntimeException("Caught RemoteException" + ex);
                  }
              } 
            else 
              {
                template = (FieldTemplate) ref.getReference();
                templateName = template.getName();
          
                value = ref.getOptionValue();
          
                if (debug)
                  {
                    System.err.println("setting basefield option for field " + templateName);
                  }
          
                try
                  {
                    gc.handleReturnVal(opField.setOption(template.getBaseID(), template.getID(), labels[value]));
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



  /**
   * <p>This method pops down the widget and does some variable
   * clearing to assit in garbage collection.</p>
   */

  public void cleanUp()
  {
    setVisible(false);
    gc = null;
    OkButton = null;
    CancelButton = null;
    ExpandButton = null;
    CollapseButton = null;
  }
} 


/**
 * An instance of this class is attached to each node in the JTree. The hooks
 * in this class allow us to refer to the DBObjectBase / DBObjectBaseField
 * that the corresponding node in the tree represents. It also holds the value
 * of the options for each node in the tree.
 */
class FieldOptionRow {
  /* Either a BaseDump or a FieldTemplate object associated with this node
   * in the tree */
  private Object reference;

  /* The actual field option value */
  private int opValue;

  /* True if we've edited the contained field option value */
  private boolean changed;

  /* Title of the tree node */
  private String name;

  public FieldOptionRow(BaseDump base, FieldTemplate field, int opValue) 
  {
    this.opValue = opValue;

    if (base == null) 
      {
	reference = null;
	name = "Root";
      }
    else
      {
	if (field == null) 
	  {
	    reference = base;
	    name = base.getName();
	  } 
	else 
	  {
	    reference = field;
	    name = field.getName();
	  }
      }
  }
  
  public String toString() 
  {
    return name;
  } 

  public boolean isChanged() 
  {
    return changed;
  }
  
  public void setChanged(boolean value) 
  {
    changed = value;
  }
  
  public int getOptionValue()
  {
    return opValue;
  }

  public void setOptionValue(int newVal)
  {
    this.opValue = newVal;
  }

  /**
   * Returns the current base or field object
   */
  public Object getReference() 
  {
    return reference;
  }

  /**
   * Returns if the current row is dealing w/ base or field
   */
  public boolean isBase() 
  {
    if (reference instanceof BaseDump)
      {
	return true;
      }
    else
      {
	return false;
      }
  }

}



/**
 * <P>Custom TreeTableModel model for use with the Ganymede client's
 * {@link arlut.csd.ganymede.client.fieldoption_editor fieldoption_editor} 
 * field options editor dialog.</P>
 */

class FieldOptionModel extends AbstractTreeTableModel implements TreeTableModel {
  static protected String[]  cNames =  {"Name", "When to include in build"};
  static protected Class[]  cTypes = {TreeTableModel.class, Integer.class};
  
  public FieldOptionModel(DefaultMutableTreeNode root)
  {
    super(root); 
  }
  
  //
  // The TreeModel interface
  //

  public int getChildCount(Object node)
  {
    return ((DefaultMutableTreeNode)node).getChildCount();
  }
  
  public Object getChild(Object node, int i)
  { 
    return ((DefaultMutableTreeNode)node).getChildAt(i);
  }
  
  public boolean isLeaf(Object node)
  { 
    return ((DefaultMutableTreeNode)node).isLeaf();
  }
  
  //
  //  The TreeTableNode interface.
  //

  public int getColumnCount()
  {
    return cNames.length;
  }
  
  public String getColumnName(int column)
  {
    return cNames[column];
  }
  
  public Class getColumnClass(int column)
  {
    return cTypes[column];
  }
  
  public Object getValueAt(Object node, int column) 
  {
    FieldOptionRow myRow = (FieldOptionRow)((DefaultMutableTreeNode)node).getUserObject(); 
    
    switch(column) 
      {
      case 1:
	return new Integer(myRow.getOptionValue());
      }

    return null;
  }
  
  public boolean isCellEditable(Object node, int col) 
  {
    return true;
  }
  
  public void setValueAt(Object value, Object node, int col) 
  {
    FieldOptionRow myRow = (FieldOptionRow)((DefaultMutableTreeNode)node).getUserObject(); 
    int newVal = ((Integer)value).intValue();
    
    switch(col) 
      {
      /* case: 0 represents the column the tree is held in. You can't set that
       * value, so we'll ignore it */
      case 1:
        myRow.setOptionValue(newVal);
	myRow.setChanged(true);
      
	if (myRow.isBase()) 
	  {
	    setBaseChildren(node, 1, value);
	  }
	break;
      }
    
    /* Update table to reflect these changes */
    TreeNode[] path = ((DefaultMutableTreeNode)node).getPath();
    fireTreeNodesChanged(FieldOptionModel.this, path, null, null);    
  }
  
  public void setBaseChildren(Object node, int col, Object value) 
  {
    for (Enumeration e = ((DefaultMutableTreeNode)node).children(); e.hasMoreElements();) 
      {
        setValueAt(value, (DefaultMutableTreeNode)e.nextElement(), 1);
      }
  }
}



/**
 * A cell renderer that delegates rendering functions to one of 2 classes.
 * ObjectBases are rendered with a checkbox, and Fields are rendered with
 * a combo box.
 */
class DelegateRenderer implements TableCellRenderer
{
  /* Is the renderer read-only? */
  boolean editable;

  /* Hook for the backing store of the JTree. This gives us a way to access
   * our FieldOptionRow objects attached to various nodes in the tree. */
  TreeTableModelAdapter model;

  public DelegateRenderer(TreeTableModelAdapter model, boolean editable)
  {
    this.editable = editable;
    this.model = model;
  }

  /* This implements the TableCellRenderer interface */

  public Component getTableCellRendererComponent(JTable table,
                                                 Object value,
                                                 boolean isSelected,
                                                 boolean hasFocus,
                                                 int row,
                                                 int column)
  {
    DefaultMutableTreeNode node = (DefaultMutableTreeNode) (model.nodeForRow(row));
    FieldOptionRow qrow = (FieldOptionRow) (node.getUserObject());
    int opvalue = qrow.getOptionValue();
    Component renderer;

    /* ObjectBases are rendered as checkboxes */
    if (qrow.isBase())
    {
      CheckBoxRenderer cbr = new CheckBoxRenderer(editable);
      if (opvalue == 0)
      {
        cbr.setSelected(false);
      }
      else
      {
        cbr.setSelected(true);
      }
      renderer = cbr;
    }
    /* Fields are rendered as combo boxes */
    else
    {
      ComboRenderer lr = new ComboRenderer(editable);
      lr.setSelectedIndex(opvalue);
      renderer = lr;
    }
    return renderer;
  }
}



/**
 * A cell editor that delegates editing functions to one of 2 classes.
 * ObjectBases are handled with a checkbox, and Fields are handled with
 * a combo box.
 */
class DelegateEditor extends AbstractCellEditor implements TableCellEditor
{
  /* Is the renderer read-only? */
  boolean editable;

  /* Hook for the backing store of the JTree. This gives us a way to access
   * our FieldOptionRow objects attached to various nodes in the tree. */
  TreeTableModelAdapter model;

  public DelegateEditor(TreeTableModelAdapter model, boolean editable)
  {
    this.editable = editable;
    this.model = model;
  }

  public Component getTableCellEditorComponent(JTable table,
                                               Object value,
                                               boolean isSelected,
                                               int row,
                                               int column)
  {
    DefaultMutableTreeNode node = (DefaultMutableTreeNode) (model.nodeForRow(row));
    FieldOptionRow qrow = (FieldOptionRow) (node.getUserObject());
    int opvalue = qrow.getOptionValue();

    if (qrow.isBase())
    {
      CheckBoxEditor cbe = new CheckBoxEditor(editable);
      JCheckBox cb = (JCheckBox) cbe.getComponent();
      if (opvalue == 0)
      {
        cb.setSelected(false);
      }
      else
      {
        cb.setSelected(true);
      }
      return cb;
    }
    else
    {
      ComboEditor ce = new ComboEditor(editable);
      JComboBox cb = (JComboBox) ce.getComponent();
      cb.setSelectedIndex(opvalue);
      return cb;
    }
  }
}



/**
 * Renders the field options for an ObjectBase.
 */
class CheckBoxRenderer extends JCheckBox implements TableCellRenderer
{
  public CheckBoxRenderer(boolean editable)
  {
    setEnabled(editable);
  }

  /**
   * Takes an Integer of value 0 or 1 and returns the corresponding
   * boolean truth value.
   */
  public boolean convertValueToBoolean(Object value)
  {
    int i = ((Integer)value).intValue();
    if (i == 0)
      return false;
    else
      return true;
  }

  public Component getTableCellRendererComponent(JTable table, 
                                                 Object value, 
                                                 boolean isSelected,
                                                 boolean hasFocus,
                                                 int row,
                                                 int column)
  {
    setSelected(convertValueToBoolean(value));
    return this;
  }
}



/**
 * Presents an editor for the field options for a DBObjectBase.
 *
 * In reality, this just defers the task of editing to a CheckBoxRenderer
 * component, which is just an extension of JCheckBox.
 */
class CheckBoxEditor extends DefaultCellEditor implements TableCellEditor
{
  public CheckBoxEditor(boolean editable)
  {
    /* DefaultCellEditor requires you give it a Component to handle the
     * cell editing chores. We'll give it an uber-JCheckBox. */
    super(new CheckBoxRenderer(editable));
  }

  public Component getTableCellEditorComponent(JTable table,
                                               Object value,
                                               boolean isSelected,
                                               int row,
                                               int column)
  {
    /* getComponent() returns the CheckBoxRenderer we used in the
     * constructor */
    CheckBoxRenderer cbr = (CheckBoxRenderer) getComponent();
    cbr.setSelected(cbr.convertValueToBoolean(value));
    return cbr;
  }
}



/** 
 * Renders the field options for a DBObjectBaseField
 */
class ComboRenderer extends JComboBox implements TableCellRenderer
{
  public ComboRenderer(boolean editable)
  {
    /* Pass in the list of Strings to display in the combo box */
    super(fieldoption_editor.labels);
    setEnabled(editable);

    /* Disallow free-form text entry */
    setEditable(false);
  }
  
  public Component getTableCellRendererComponent(JTable table, 
                                                 Object value, 
                                                 boolean isSelected,
                                                 boolean hasFocus,
                                                 int row,
                                                 int column)
  {
    int labelIndex = ((Integer)value).intValue();
    setSelectedIndex(labelIndex);
    return this;
  }
}



/**
 * Presents an editor for the field options for a DBObjectBaseField.
 *
 * In reality, this just defers the task of editing to a ComboBoxEditor
 * component, which is just an extension of JComboBox.
 */
class ComboEditor extends DefaultCellEditor implements TableCellEditor
{
  public ComboEditor(boolean editable)
  {
    super(new ComboRenderer(editable));
  }

  public Component getTableCellEditorComponent(JTable table,
                                               Object value,
                                               boolean isSelected,
                                               int row,
                                               int column)
  {
    ComboRenderer cr = (ComboRenderer) getComponent();
    int labelIndex = ((Integer)value).intValue();
    cr.setSelectedIndex(labelIndex);
    return cr;
  }
}
