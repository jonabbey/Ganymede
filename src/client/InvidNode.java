/*
   InvidNode.java

   Tree node subclass used by gclient.java and openObjectDialog.java

   Created: 15 January 1999
   Version: $Revision: 1.1 $
   Last Mod Date: $Date: 1999/01/16 01:27:23 $
   Module By: Mike Mulvaney, Jonathan Abbey, and Navin Manohar
   Applied Research Laboratories, The University of Texas at Austin

*/

/*---------------------------------------------------------------------
                                                                  class 
                                                              InvidNode

---------------------------------------------------------------------*/

class InvidNode extends arlut.csd.JTree.treeNode {

  final static boolean debug = true;

  private Invid invid;

  private String typeText;

  private ObjectHandle handle;

  public InvidNode(treeNode parent, String text, Invid invid, treeNode insertAfter,
		   boolean expandable, int openImage, int closedImage, treeMenu menu,
		   ObjectHandle handle)
  {
    super(parent, text, insertAfter, expandable, openImage, closedImage, menu);

    this.invid = invid;
    this.typeText = parent.getText();
    this.handle = handle;

    if (debug)
      {
	if (invid == null)
	  {
	    System.out.println(" null invid in InvidNode: " + text);
	  }
      }
  }

  public Invid getInvid()
  {
    return invid;
  }

  // Can't think of why you would ever want this

  public void setInvid(Invid invid)
  {
    this.invid = invid;
  }

  public String getTypeText()
  {
    return typeText;
  }  

  public ObjectHandle getHandle()
  {
    return handle;
  }

  public void setHandle(ObjectHandle handle)
  {
    this.handle = handle;
  }
}
