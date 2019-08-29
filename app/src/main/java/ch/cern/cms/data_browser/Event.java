package ch.cern.cms.data_browser;

import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.os.Bundle;
import android.os.SystemClock;
import android.widget.Toast;

import com.threed.jpct.Camera;
import com.threed.jpct.FrameBuffer;
import com.threed.jpct.Loader;
import com.threed.jpct.Matrix;
import com.threed.jpct.Object3D;
import com.threed.jpct.Polyline;
import com.threed.jpct.SimpleVector;

//Super class to manage the tracks/hits/etc in an event
public class Event {

	private int poolSize = 500;
	private int initSize = 500;
	private int add2Size = 100;

	private int selectedTrack = -1;
	
	private boolean eventIsLoaded = false;
	
	private List<particle> particles = new ArrayList<particle>();
	private List<int[]> iHits = new ArrayList<int[]>();
	
	private Context mContext = null;
	private String eventTitle = "";
	private ScheduledExecutorService executor = null;
    
	private Settings settings = null;
	private TrackRenderer parent = null;
	private Object3D htower = null;
	
	private Logger Log;
	
	public Event(Context context, TrackRenderer gl ) {
		
		mContext = context;
		settings = Settings.getInstance();
		
		// Get the instance of the logger
		Log = Logger.getInstance();
		
		if ( gl != null )
			this.parent = gl;
		
		// On startup, initialize a pool of tracks, sprites & hits to be used
		long start = System.currentTimeMillis();

		// Start up the pool monitor which will trim the pool size if tracks are unused
		setPoolCleanup();
		
		float elapsed = (System.currentTimeMillis() - start)/1000f;

		if (getIntSetting("debugLevel") > 0)
			Log.w("Event()", "Created "+particles.size()+" tracks in "+elapsed+" s");
		
		/**************************************************************/
    	InputStream is = mContext.getResources().openRawResource(R.raw.tower);
    	Object3D[] model = Loader.loadSerializedObjectArray(is);
        htower = new Object3D(0);

        Matrix identity = new Matrix();
		identity.setIdentity();

        for (int i = 0; i < model.length; i++) {
            model[i].setCenter(SimpleVector.ORIGIN);
            model[i].setScale(1.0f);
    		model[i].setRotationMatrix(identity);
            htower = Object3D.mergeObjects(htower, model[i]);
        }
		/**************************************************************/
		
		return;
	}

	// Faux Android activity management functions -- called from the OpenGLFragment
	protected void onStop() {
		cleanUpEvent();
		return;
	}
	protected void onPause() {
		cleanUpEvent();      
		return;
	}
	protected void onResume() {
		setPoolCleanup();
		return;
	}

    /*******************************************
    * Private accessors into the setting class *
    ********************************************/
    private int getIntSetting( String key ) {
    	return settings.getIntSetting(key);
    }
    
    /******************************************
     * 
     *  Public class methods
     * 
    *******************************************/

    public boolean setUpWorld() {
    	
    	for ( int i=0; i<initSize; i++ ) {
    		particles.add(new particle(i));
    		
        	// Check and see if the textures actually got loaded
        	if ( settings.getBooleanSetting("animation") )
        		parent.addToWorld(particles.get(i).sprite);
    	}
		return true;
    }
    
    public void setContext(Context context) {
    	this.mContext = context;
    	return;
    }

    public void setParent(TrackRenderer gl) {
    	if ( gl != null )
    		this.parent = gl;
    	return;
    }
    
    public void cleanUpEvent() {
		if ( executor != null ) {
			executor.shutdown();
			executor = null;
		}
		return;
	}
	
	public void setPoolCleanup() {
		
		if ( executor != null ) {
			//Log.e("Event()", "cleanUpTask() is already running!");
			return;
		}
		
		// Every 10s check the size of the pool and remove unnecessary tracks
		executor = Executors.newSingleThreadScheduledExecutor();
		Runnable task = new Runnable() {
			@Override
			public void run() {
				
				// Check the particle pool
				for ( int i=poolSize-1; i>=initSize; i-- ) {
                	if ( !particles.get(i).active ) {
                		particles.get(i).lifeTime--;
                		if ( particles.get(i).lifeTime <= 0 ) {
                			
                			particles.get(i).cleanup();
                			particles.remove(i);

                			poolSize--;
                		}
                	}
                }
				
				if ( poolSize > 0 )
					executor.schedule(this, 60, TimeUnit.SECONDS);
				else
					executor.shutdown();
			}
		};
		executor.schedule(task, 60, TimeUnit.SECONDS);
		
		return;
	}
	
	private void addToPool() {
		for ( int i=poolSize; i<poolSize+add2Size; i++ ) {
			particles.add(new particle(i));
	    	if ( settings.getBooleanSetting("animation") )
	    		parent.addToWorld(particles.get(i).sprite);
		}
		
		poolSize += add2Size;
		if ( getIntSetting("debugLevel") > 0 )
			Log.w("Event()", "Added "+add2Size+" tracks to pool for a new size of "+poolSize);
		
		return;
	}
	
	public void clearEvent() {
		
		//Log.w("clearEvent()", "Clearing events in the pool");
		
		for ( int i=0; i<poolSize; i++ ) {
			if ( !particles.get(i).active )
				break;
			particles.get(i).flush();
		}
		
		//Log.w("clearEvent()", "Clearing hits");
		iHits.clear();
		
		eventTitle = null;
		eventIsLoaded = false;
		parent.setEventTitle(eventTitle);
		parent.eventLoadSignal();
		
		//Log.w("clearEvent()", "Done");
		
		return;
	}
	
	public List<Polyline> getEventTracks() {
		List<Polyline> lineList = new ArrayList<Polyline>();
		for ( int i=0; i<poolSize; i++ ) {

			if ( !particles.get(i).active )
				break;

			lineList.add(particles.get(i).getTrack());
			
		}
		
		return lineList;
	}
	
	public List<Object3D> getEventObjects() {
		
		List<Object3D> objList = new ArrayList<Object3D>();
		for ( int i=0; i<poolSize; i++ ) {

			if ( !particles.get(i).active )
				break;
			if ( particles.get(i).hasCone() )
				objList.add(particles.get(i).getCone());
			
			if ( particles.get(i).addedToWorld ) {				
				objList.add(particles.get(i).tower);
				objList.add(particles.get(i).glow);
			}
			
			if ( particles.get(i).muonHits != null && particles.get(i).muonHits.size() > 0) {
	    		for ( int j=0; j<particles.get(i).muonHits.size(); j++ ) {
	    			
	    			if ( particles.get(i).muonHits.get(j) != null ) 
	    				objList.add(particles.get(i).muonHits.get(j).hit);
	    			
	    		}
	    	}
			
		}
		
		return objList;
		
	}
	
	public List<int[]> getHitsList() {
		return this.iHits;
	}
	
	public boolean isEventLoaded() {
		return this.eventIsLoaded;
	}

	public void setVisible(boolean visible) {

		for ( int i=0; i<particles.size(); i++ ) {
			if ( !particles.get(i).active )
				break;
			   
			particles.get(i).setVisible(false);
		   
		}
		return;
	}
	
	public void setSpriteVisibility( boolean visible) {
		for ( int i=0; i<particles.size(); i++ ) {
			if ( !particles.get(i).active )
				break;

			if ( particles.get(i).type == constants.TK )
				particles.get(i).sprite.setVisibility(visible);
			if ( !visible )
				particles.get(i).setPositionAt(0f);
		}

		for ( int i=0; i<particles.size(); i++ ) {
			if ( !particles.get(i).active )
				break;

			if ( particles.get(i).type != constants.TK )
				particles.get(i).sprite.setVisibility(visible);
			if ( !visible )
				particles.get(i).setPositionAt(0f);
		}
		
		return;
	}
	
	public int setPositionAt(float timeStamp) {
		int stillGoing = 0;
		for ( int i=0; i<particles.size(); i++ ) {
			if ( !particles.get(i).active )
				break;
			stillGoing += particles.get(i).setPositionAt(timeStamp);
			
			// Check for hits in the muon chambers
			if ( particles.get(i).type == constants.MU && particles.get(i).muonHits != null ) {
				for ( int j=0; j<particles.get(i).muonHits.size(); j++ ) {
				
					if ( timeStamp >= particles.get(i).muonHits.get(j).timeStamp )
						particles.get(i).muonHits.get(j).setVisibility(true);
				}
			}
			
		} // End for ( int i=0; i<particles.size(); i++ ) 
		return stillGoing;
	}
	
	public String getEventTitle() {
		return this.eventTitle;
	}

	public void updateShowCones( boolean showCones ) {
    	
		for ( int i=0; i<particles.size(); i++ ) {
			if ( particles.get(i).hasCone() ) 
				particles.get(i).getCone().setVisibility(showCones);
		}

    	return;
    }
    
    public void updateShowHadrons( boolean showHadrons ) {
    	for ( int i=0; i<particles.size(); i++ ) {
    		if ( particles.get(i).name.equalsIgnoreCase("hadron") ) 
    			particles.get(i).updateHadronVisible(showHadrons);
    	}
    	return;
    }
    
    public void updateShowAxis( boolean showAxis ) {
    	for ( int i=0; i<particles.size(); i++ ) {
    		if ( particles.get(i).name.equalsIgnoreCase("Jet") )
    			particles.get(i).updateAxisVisible(showAxis);
    	}
    	return;
    }

    public void updateShowTracker( boolean showTracker ) {
    	for ( int i=0; i<particles.size(); i++ ) {
    		if ( particles.get(i).type == constants.MU && particles.get(i).q != 0 ) {
    			particles.get(i).updateVisible();
    		}
    	}

    	return;
    }
    
    public void updateShowStandalone( boolean showStandAlone ) {
    	for ( int i=0; i<particles.size(); i++ ) {
    		if ( particles.get(i).type == constants.MU && particles.get(i).q != 0 ) {
    			particles.get(i).updateVisible();
    		}
    	}
    	return;
    }
    
    public void updateShowGlobal( boolean showGlobal ) {
    	for ( int i=0; i<particles.size(); i++ ) {
    		if ( particles.get(i).type == constants.MU && particles.get(i).q != 0 ) {
    			particles.get(i).updateVisible();
    		}
    	}
    	return;
    }

    public void applyPTCut() {
    	for ( int i=0; i<particles.size(); i++ ) {
    		if ( !particles.get(i).active ) {
    			break;
    		}
    		particles.get(i).updateVisible();
    	}
    	return;
    }

    public List<String> getNumberOfTracks() {
    	List<String> numberOfTracks = new ArrayList<String>();

    	if ( particles.size() < 1) {
        	numberOfTracks.add("New");
        	return numberOfTracks;
    	}
    	numberOfTracks.add("Track");
    	numberOfTracks.add("New");
    	for ( int i=0; i<particles.size(); i++ ) {
    		if ( !particles.get(i).active )
    			break;
    		numberOfTracks.add( String.valueOf(i+1));
    	}
    	return numberOfTracks;
    }

    public Bundle getTrackData(int id) {
    	Bundle trackData = new Bundle();
    	
    	if ( particles.get(id) != null && particles.get(id).active ) {
    		particle p = particles.get(id);
    		trackData.putDoubleArray("p", p.p.getDoubleArray());
    		trackData.putInt("vtx", (int)(p.vtx));
    		trackData.putInt("id", p.id);
    		trackData.putInt("type", p.type);
    		trackData.putInt("charge",  p.q);
    		trackData.putString("color", p.colorName);
    		return trackData;
    	}
    	return null;
    }
    
    public void deleteTrack(int id) {
    	if ( id < 0 || id > particles.size()-1 )
      		return;

      	particle p = particles.get(id);
      	if ( p == null )
      		return;

      	parent.removeFromWorld(p.getTrack());

      	particles.remove(id);
      	p.flush();
      	p = null;

      	return;
    }
    
    public void editTrack(Bundle newTrackData) {
    	int trk = newTrackData.getInt("id");
      	if ( trk < 0 || trk > particles.size()-1 )
      		return;

      	particle p = particles.get(trk);
      	if ( p == null )
      		return;

      	parent.removeFromWorld(p.getTrack());

    	if ( newTrackData.containsKey("p") ) {
    		double [] momentum = newTrackData.getDoubleArray("p");
    		p.setP(momentum);
    	}
    	if ( newTrackData.containsKey("vtx") ) {
    		int vtx = newTrackData.getInt("vtx");
    		p.setVtx((double)vtx);
    	}
    	if ( newTrackData.containsKey("color") ) {
    		String clr = newTrackData.getString("color");
    		p.setCLR(clr);
    	}
    	if ( newTrackData.containsKey("type") ) {
    		int type = newTrackData.getInt("type");
    		p.setType(type);
    	}
    	if ( newTrackData.containsKey("charge") ) {
    		int charge = newTrackData.getInt("charge");
    		p.setQ(charge);
    	}

    	p.makeTrack();
    	parent.addToWorld(p.getTrack());
    	
    	return;
    }
    
    public void addTrack(Bundle trackData) {

   	 double [] p = trackData.getDoubleArray("p");
   	 int vtx = trackData.getInt("vtx");
   	 String clr = trackData.getString("color");
   	 int type = trackData.getInt("type");
   	 int charge = trackData.getInt("charge");

   	 if (vtx > 25)
   		 vtx = 25;
   	 else if ( vtx < -25 )
   		 vtx = -25;
   	 
   	 for ( int i=0; i<particles.size(); i++ ) {
   		 
   		 if ( !particles.get(i).active ) {
   			 particles.get(i).set(p[0], p[1], p[2], charge, type, clr, vtx, null);
   			 parent.addToWorld(particles.get(i).getTrack());

   			break;
   			
   		 } // End if ( !particles.get(i).active )
   	 } // End for ( int i=0; i<particles.size(); i++ )
   	 return;
    }
    
    public void setHideUnderlyingEvent() {
    	for ( int i=0; i<particles.size(); i++ ) {
    		
        	if ( !particles.get(i).active )
        		break;
        	
    		particles.get(i).updateVisible();
    	}
    	
    	return;
    }

    public void setHideUnDetectable() {
    	for ( int i=0; i<particles.size(); i++ ) {
    		
        	if ( !particles.get(i).active )
        		break;
        	
    		particles.get(i).updateVisible();
    	}
    	
    	return;
    }

    public void updateVisible() {
    	for ( int i=0; i<particles.size(); i++ ) {
    		if ( !particles.get(i).active )
    			break;
    		particles.get(i).updateVisible();
    	}
    	
    	if ( settings.getBooleanSetting("response") && settings.getBooleanSetting("showTracks") ) {
    		parent.setDetectorHits(true);
    	} else
    		parent.setDetectorHits(false);
    	
    	return;
    }
    
	public void cancelSelection() {
		if ( selectedTrack > -1 ) {
        	parent.removeFromWorld(particles.get(selectedTrack).getTrack());
    		particles.get(selectedTrack).lineWidth /= 5;
    		particles.get(selectedTrack).makeTrack();
    		parent.addToWorld(particles.get(selectedTrack).getTrack());
    		selectedTrack = -1;
		}
		return;
	}
	
	public String getSelectedTrackInfo( Camera cam, FrameBuffer buffer, double selection_radius, int x, int y) {
		
		if ( particles == null || particles.size() == 0 )
			return null;
		
    	SimpleVector rayTrace = new SimpleVector (x, y, 0);

		long startTime = SystemClock.elapsedRealtime ();
		
    	float min_distance = 10000f;
    	int selection = -1;
    	for ( int i=0; i<particles.size(); i++ ) {

    		if ( !particles.get(i).active )
    			break;
    		
    		if ( !particles.get(i).visible )
    			continue;
    		
    		float distance = particles.get(i).findMinDistance(cam, buffer, rayTrace);
    		
    		if ( distance < 0 )
    			continue;

    		if ( distance < min_distance ) {
    			min_distance = distance;
    			selection = i;
    		} 
    	}
    	
    	if ( min_distance > selection_radius )
    		selection = -1;
    	
		long stopTime = SystemClock.elapsedRealtime ();
    	
    	if ( selection >= 0 ) {
        	parent.removeFromWorld(particles.get(selection).getTrack());
    		particles.get(selection).lineWidth *= 5;
    		particles.get(selection).makeTrack();
    		parent.addToWorld(particles.get(selection).getTrack());
    		
    	} else if ( getIntSetting("debugLevel") > 0 )
    		Log.w("Renderer()", "Nothing in selection area after "+ (stopTime - startTime) +" ms");

    	String formatted = "none";
    	
    	if ( selection >= 0 ) {
    		DecimalFormat df1 = new DecimalFormat("#.#");
    		DecimalFormat df2 = new DecimalFormat("#.##");
		
    		double pz = particles.get(selection).p.getZ();
    		double pt = particles.get(selection).Pt;
    		double dPhi = particles.get(selection).phi;
    		double eta = particles.get(selection).eta;
    		double e = particles.get(selection).E;
    		int vtx = (int)particles.get(selection).vtx;
    		String name = particles.get(selection).name;
    		int phi = (int)Math.round(180*dPhi/Math.PI);
		
    		formatted = 
    				mContext.getString(R.string.Pz,df1.format(pz))+"<br/>"+
    						mContext.getString(R.string.PT,df1.format(pt))+"<br/>"+
    						mContext.getString(R.string.phi, phi) + "<br/>"+
    						mContext.getString(R.string.eta, df2.format(eta))+ "<br/>"+
    						mContext.getString(R.string.e, df1.format(e))+ "<br/>" +
    						mContext.getString(R.string.vtx, vtx) + "<br/>" + "ID: " + name;
    	}
   	
    	selectedTrack = selection;
    	
    	return formatted;
    	
    }
	
	public void setEventData( List<String>event ) {
		
		long startTime = System.currentTimeMillis();
		
		// Wait until the display is cleared out in the 
		// renderer before we start adding stuff
		// (the renderer will call clearEvent() for us)
		while ( this.eventIsLoaded ) {
			
			// If we've been waiting more than 5s, remind the renderer to clear the event
			long elapsed = (System.currentTimeMillis() - startTime)/1000;
			if ( elapsed >= 5 ) {
				parent.clearEvent();
			} else if ( elapsed > 10 ) {
				Log.e("setEventData()", "Unable to clear current event!");
		    	Toast.makeText(mContext, "Unable to clear display! Event load failed", Toast.LENGTH_LONG).show();
				return;
			}
		}
	    
		// Line #1 is the event label
		eventTitle = event.get(0);
		
		// Event string format: px,py,pz,q,vtx,type,m,e,name
		for ( int i=0; i<event.size()-1; i++ ) {

		    double px,py,pz,vx,m,e;
	    	int q,t;

	    	// Data goes from line #2 through the end
		    String [] evt = event.get(i+1).split(",");
		    
		    try {
		    	px = Double.parseDouble(evt[0]);
		    	py = Double.parseDouble(evt[1]);
		    	pz = Double.parseDouble(evt[2]);
		    	q  = Integer.parseInt(evt[3]);
		    	vx = Double.parseDouble(evt[4]);
		    	t  = Integer.parseInt(evt[5]);
		    	m  = Double.parseDouble(evt[6]);
		    	e  = Double.parseDouble(evt[7]);
		    } catch ( Exception except ) { 
		    	Log.e("setEventData()", "Caught exception on "+event.get(i+1)+" on line "+i);
		    	continue;
		    }
		    
		    String  n = evt[8];
		    String color = getColor(t, q, n);
		    
		    if ( i >= poolSize )
		    	addToPool();
		    
		    // Add the particles to the pool, pool is already initialized with 
		    // enough space in memory, so activate them and set the physics properties
		    particles.get(i).set(px, py, pz, q, t, color, vx, n);
		    
		    particles.get(i).setEnergy(e);
		    particles.get(i).setMass(m);
		    
		    /*************************************************************************/
		    if ( t == constants.EM ) {
		    	particles.get(i).drawTower(htower);
		    	particles.get(i).addedToWorld = true;
		    	
		    } else if ( settings.getBooleanSetting("isData") && n.equalsIgnoreCase("hadron") ) {
		    	particles.get(i).drawTower(htower);
		    	particles.get(i).addedToWorld = true;
		    	
		    } else if ( settings.getBooleanSetting("isSim") && t  == constants.HD ) {
		    	particles.get(i).drawTower(htower);
		    	particles.get(i).addedToWorld = true;
		    	
		    } else if ( particles.get(i).type == constants.MU ) {
		    	particles.get(i).makeMuonHits();
		    }
		    /*************************************************************************/

		} // End for (int i=0; i<event.size(); i++ )

		// Debugging stuff
		if ( getIntSetting("debug") > 0 ) {		
			Log.w("setEventData()", eventTitle);

			long elapsed = System.currentTimeMillis() - startTime;
			Log.w("setEventData()", "Loaded "+event.size()+" tracks in "+elapsed/1000f+" s");
		}
		
		startTime = System.currentTimeMillis();
		for ( int i=0; i<particles.size(); i++ )
			iHits.addAll(particles.get(i).makeHitsI());
		long elapsed = System.currentTimeMillis() - startTime;

		if ( getIntSetting("debug") > 0 )
			Log.w("setEventData()", "Calculated "+iHits.size()+" integer hits in "+elapsed+" ms");
		
		startTime = System.currentTimeMillis();
		// Sort the hits by a) hit time and b) layer number
		Collections.sort(iHits, iTimeOrder);
		elapsed = (System.currentTimeMillis() - startTime);
		
		if ( getIntSetting("debug") > 0 )
			Log.w("setEventData()", elapsed + " ms to sort "+iHits.size()+" hits");
		
    	if ( settings.getBooleanSetting("response")  && settings.getBooleanSetting("showTracks") ) {	
    		parent.setDetectorHits(true);
    	} else
    		parent.setDetectorHits(false);

		parent.setEventTitle(eventTitle);
    	
		// We're loaded & ready, let the parent know it's tiem to set the menus
		this.eventIsLoaded = true;
		parent.eventLoadSignal();
		
		return;
	}

	private String getColor(int t, int q, String n) {
		String color = mContext.getString(mContext.getResources().getIdentifier("black" , "color", mContext.getPackageName()));

		if ( t == constants.EM ) {           // EM
			if ( q != 0 )             // Charged => electrons
				color = mContext.getString(mContext.getResources().getIdentifier("green" , "color", mContext.getPackageName()));
			else                      // Neutral => photons
				color = mContext.getString(mContext.getResources().getIdentifier("lightblue" , "color", mContext.getPackageName()));

		} else if ( t == constants.HD ) {    // Hadrons
			if ( n.equals("Jet") )
				color = mContext.getString(mContext.getResources().getIdentifier("brown" , "color", mContext.getPackageName()));
			else
				color = mContext.getString(mContext.getResources().getIdentifier("yellow" , "color", mContext.getPackageName()));

		} else if (t == constants.MU ) {
			if ( q != 0 )             // Muons
				color = mContext.getString(mContext.getResources().getIdentifier("red" , "color", mContext.getPackageName()));
			else                      // Neutrinos
				color = mContext.getString(mContext.getResources().getIdentifier("blue" , "color", mContext.getPackageName()));

		} else if ( t == constants.MET ) {    // MET
			color = mContext.getString(mContext.getResources().getIdentifier("magenta" , "color", mContext.getPackageName()));

		} else if ( t == constants.TK || t == constants.BK ) {  // underlying event or tracks
			color = mContext.getString(mContext.getResources().getIdentifier("white" , "color", mContext.getPackageName()));
		}

		if ( color.equals(mContext.getString(mContext.getResources().getIdentifier("black" , "color", mContext.getPackageName()))) ) {
			Log.e("TrackRenderer()", "Something's wrong with the particle ID!");
			// Set the color to grey so we can pick out the problem
			color = mContext.getString(mContext.getResources().getIdentifier("grey" , "color", mContext.getPackageName()));
		}
		
		return color;
	}
	
	private Comparator<int []> iTimeOrder = new Comparator<int[]>() {
		public int compare(int [] p1, int [] p2) {
		
			if ( p1[0] == p2[0] ) {
				
				if ( p1[1] < p2[1] )
					return -1;
				else if ( p1[1] == p2[1] )
					return 0;
				else
					return 1;
				
			} else if ( p1[0] < p2[0] )
				return -1;
			else
				return 1;
		}
	};

} // End class Event
