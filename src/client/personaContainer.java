/*

   personaContainer.java

   a panel for handling User's personae.
   
   Created: 15 January 1999
   Version: $Revision: 1.1 $
   Last Mod Date: $Date: 1999/01/16 01:27:24 $
   Module By: Mike Mulvaney
   Applied Research Laboratories, The University of Texas at Austin

*/

/*------------------------------------------------------------------------------
                                                                           class
                                                                personaContainer

------------------------------------------------------------------------------*/

/**
 * Make sure you add this tab to a JTabbedPane BEFORE you call run()!
 */

class personaContainer extends JScrollPane implements Runnable{

  private final static boolean debug = false;
  
  boolean loaded = false;

  Invid
    invid;

  personaPanel
    pp;

  gclient
    gc;

  boolean
    createNew,
    editable;

  int
    index;

  JProgressBar
    progressBar;

  JPanel
    progressPane;

  db_object object;

  /* -- */

  public personaContainer(Invid invid, int index, boolean editable, personaPanel pp, db_object object)
  {
    if (object == null)
      {
	throw new IllegalArgumentException("Got a null object in personaContainer.");
      }

    this.invid = invid;
    this.object = object;
    this.index = index;
    this.pp = pp;
    gc = pp.gc;
    this.editable = editable;
    this.createNew = createNew;

    progressBar = new JProgressBar();
    progressPane = new JPanel();
    progressPane.add(new JLabel("Loading..."));
    progressPane.add(progressBar);
    getVerticalScrollBar().setUnitIncrement(15);
    setViewportView(progressPane);
  }

  public boolean isEditable()
  {
    return editable;
  }

  public synchronized void run()
  {
    if (debug)
      {
	System.out.println("Starting new thread");
      }

    try
      {
	String label = object.getLabel();

	if ((label != null) && (!label.equals("null")))
	  {
	    pp.middle.setTitleAt(index, label);
	    pp.middle.repaint();
	  }
	
	containerPanel cp = new containerPanel(object,
					       editable,
					       pp.fp.getgclient(), 
					       pp.fp.getWindowPanel(), 
					       pp.fp,
					       progressBar,
					       this);
	cp.setBorder(pp.empty);
	setViewportView(cp);
      }
    catch (RemoteException rx)
      {
	throw new RuntimeException("Could not load persona into container panel: " + rx);
      }

    loaded = true;

    if (debug)
      {
	System.out.println("Done with thread in personaPanel");
      }

    pp.invalidate();
    pp.fp.validate();

  }

  public synchronized void waitForLoad()
  {
    long startTime = System.currentTimeMillis();

    while (!loaded)
      {
	try
	  {
	    if (debug)
	      {
		System.out.println("presona panel waiting for load!");
	      }

	    this.wait(1000);

	    if (System.currentTimeMillis() - startTime > 200000)
	      {
		System.out.println("Something went wrong loading the persona panel. " +
				   " The wait for load thread was taking too long, so I gave up on it.");
		break;
	      }
	  }
	catch (InterruptedException e)
	  {
	    throw new RuntimeException("Interrupted while waiting for personaContainer to load: " + e);
	  }
      }
  }

  public Invid getInvid()
  {
    return invid;
  }
}
