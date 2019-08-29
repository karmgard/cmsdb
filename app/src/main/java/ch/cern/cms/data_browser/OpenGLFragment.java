package ch.cern.cms.data_browser;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Html;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.threed.jpct.Object3D;
import com.threed.jpct.Polyline;

public class OpenGLFragment extends Fragment {
	
	private static OpenGLFragment   master = null;

	private Context       mContext  = null;
    private TrackRenderer renderer  = null;

    private boolean isVisible = true;
    private String currentDescription = null;
    
    private Settings settings = null;
    private Logger Log;

    private boolean animation = false;

    private char animation_state = 0;
    private List<Integer> items_enabled = new ArrayList<Integer>(10);
    private List<Integer> items_visible = new ArrayList<Integer>(10);
    
    /******** Android activity functions ************/
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		View view = inflater.inflate(R.layout.opengl_fragment, container, false);
    	settings = Settings.getInstance();
    	
		if ( master != null ) {
    		copy(master);

    		// If we're restoring (like on a rotation) set the animation toggle here instead of in the renderer
    		settings.putBoolean("animation", animation);
    		
		} else
    		renderer = new TrackRenderer(container.getContext(), this);
		
		this.mContext = container.getContext();
		
		renderer.setContext(mContext);
		renderer.setParent(this);
		
		GLSurfaceView mGLView = new GLSurfaceView( mContext );
    	mGLView.setRenderer(renderer);
    	
    	GestureListener gestureListener = new GestureListener();
    	final GestureDetector mDetector = new GestureDetector(mContext, gestureListener);
    	
    	mGLView.setOnTouchListener( new OnTouchListener() {
    		@Override
    		public boolean onTouch(View v, MotionEvent me) {
    			mDetector.onTouchEvent(me);
    			return  true;
    		} // End onTouch(View,MotionEvent)		
    	});

    	LinearLayout layout = view.findViewById(R.id.surfaceLayout);
    	LayoutParams lParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
 
    	// Attach to the logger
    	Log = Logger.getInstance();
    	
    	layout.addView(mGLView, lParams);
    	
    	// We can has menu?
    	setHasOptionsMenu(true);
    	   	
    	master = this;
    	
    	return view;
    }

    @Override
    public void onResume() {
    	 super.onResume();
    	 renderer.onResume();
    }

    @Override  
    public void onStop() {
    	super.onStop();
    	renderer.onPause();
     }

    @Override
    public void onPause() {
    	super.onPause();
    	renderer.onPause();
    }
     
    @Override
    public void onSaveInstanceState(Bundle outState) {
    	super.onSaveInstanceState(outState);
    	animation = settings.getBooleanSetting("animation");
    	return;
    }
     
  	public void eventLoadSignal() {
  		if ( mContext != null ) {
  			((Activity)mContext).invalidateOptionsMenu();
  		} else
  			Log.e("eventLoadSignal()", "Unable to get context!");
  		
  		return;
  	}
     
 	@Override
 	public void onCreateOptionsMenu( Menu menu, MenuInflater inflater) {
     	inflater.inflate(R.menu.gl_menu, menu);
 	   return;
 	}
 	
    public void onPrepareOptionsMenu(Menu menu) {

    	// animation_state determines which buttons show up. 
    	// State 0 no animation playing
    	// State 1 animation is playing
    	// State 2 animation is active but paused
    	// State 4 animation is active and finished playing
    	
    	// Find the animate button, Don't show it unless there's an event displayed and the textures were loaded
	    for ( int i=0; i<menu.size(); i++ ) {
	    	MenuItem item = menu.getItem(i);
	    	int id = item.getItemId();
	    	
	    	if ( id == R.id.action_animate ) {
	    		boolean showMe = renderer.isEventLoaded() && settings.getBooleanSetting("animation") && animation_state == 0;
	    		
	    		item.setEnabled(showMe);
	    		item.setVisible(showMe);
	    		
	    	} else if ( id == R.id.action_pause ) {
	    		item.setEnabled( animation_state == 1 );
	    		item.setVisible( animation_state == 1 );
	    		
	    	} else if ( id == R.id.action_play ) {
	    		item.setEnabled( animation_state == 2 );
	    		item.setVisible( animation_state == 2 );
	    		
	    	} else if ( id == R.id.action_stop ) {
	    		item.setEnabled( animation_state > 0 );
	    		item.setVisible( animation_state > 0 );

	    	} else if ( id == R.id.action_menu_screenshot || id == R.id.action_menu_share ) {
	    		// leave these guys alone, they're always visible
	    		item.setEnabled(true);
	    		item.setVisible(true);
	    		
	    	} else {
	    		if ( animation_state > 0 ) {
	    			// If there's an animation, store the ids of any active items
	    			if ( item.isEnabled() )
	    				items_enabled.add(id);
	    			if ( item.isVisible() )
	    				items_visible.add(id);
	    			item.setEnabled(false);
	    			item.setVisible(false);
	    		} else {
	    			for ( int j=items_enabled.size()-1; j>=0; j-- ) {
	    				if ( id == items_enabled.get(j) ) {
	    					item.setEnabled(true);
	    					items_enabled.remove(j);
	    					break;
	    				} // End if ( id == items_enabled.get(i) )
	    			} // End for ( int j=items_enabled.size()-1; j>=0; j-- )
	    			
	    			for ( int j=items_visible.size()-1; j>=0; j-- ) {
	    				if ( id == items_visible.get(j) ) {
	    					item.setVisible(true);
	    					items_visible.remove(j);
	    					break;
	    				} // End if ( id == items_visible.get(j) )
	    			} // End for ( int j=items_visible.size()-1; j>=0; j-- )

	    		} // End else
	    		
	    	} // End else
	    	
	    } // End for (int i=0; i<menu.size(); i++ )

	    super.onPrepareOptionsMenu(menu);
	    return;
    }

 	@Override
 	public boolean onOptionsItemSelected(MenuItem item) {
 		switch (item.getItemId() ) {
 		
 		case R.id.action_animate:
 			if ( !settings.getBooleanSetting("animation") ) {
 				// We really shouldn't be here.... this button shouldn't have 
 				// shown up if the animation was supressed in the renderer.
 				Log.e("onOptionsItemSelected()", "Attempt to run a supressed animation!");
 				return true;
 			}

 			animate();
 			animation_state = 1;
 			((Activity)mContext).invalidateOptionsMenu();
			return true;
 			
 		case R.id.action_pause:
 			animation_state = 2;
 			((Activity)mContext).invalidateOptionsMenu();
 			return true;
 			
 		case R.id.action_play:
 			animation_state = 1;
 			((Activity)mContext).invalidateOptionsMenu();
 			return true;
 			
 		case R.id.action_stop:
 			renderer.clearAnimation();
 			animation_state = 0;
 			((Activity)mContext).invalidateOptionsMenu();
 			return true;
 			
 		case R.id.action_exp_clear:
 			clearEvent();
 			animation_state = 0;
 			return true;
 			
 		case R.id.action_sim_clear:
 			clearEvent();
 			animation_state = 0;
 			return true;
 			
 		default:
 			return super.onOptionsItemSelected(item);
 		}
 	}

 	private void copy(Object src) {
 		try {
 			Field[] fs = src.getClass().getDeclaredFields();
 			for (Field f : fs) {
 				f.setAccessible(true);
 				f.set(this, f.get(src));
 			}
 		} catch (Exception e) {
 			throw new RuntimeException(e);
 		}
 	} // End copy(Object src)
     
     public void onAnimationComplete() {
    	 animation_state = 4;
    	 ((Activity)mContext).invalidateOptionsMenu();
    	 return;
     }
     
     /******** Inter-class communications ************/
     public boolean addToWorld( Object3D object ) {
    	 return renderer.addToWorld(object);
     }
     
     public boolean addToWorld( Polyline line ) {
    	 return renderer.addToWorld(line);
     }
     
     public boolean removeFromWorld( Object3D object ) {
    	 return renderer.removeFromWorld(object);
     }
     
     public boolean removeFromWorld( Polyline line ) {
    	 return renderer.removeFromWorld(line);
     }
     
     public boolean setUpWorld() {
    	 return renderer.setUpWorld();
     }
     
     public boolean nextStep() {
    	 if ( animation_state == 2 )         // paused
    		 return true;
    	 else if ( animation_state == 4 )   // stopped (end of animation)
    		 return false;
    	 else
    		 return renderer.nextStep();
     }
     
     public boolean isEventLoaded() {
    	 return renderer.isEventLoaded();
     }
     
     /******** Functions for interacting with the event pool ************/
     public void setSim(String simProc, String simChan) {
    	 renderer.setSim(simProc, simChan);
    	 return;
     }
     
     public void setEventData(List<String>data) {
    	 if ( renderer != null )
    		 renderer.setEventData(data);
    	 return;
     }
     
     public void updateshowAxis( boolean showAxis ) {
    		 renderer.updateShowAxis(showAxis);
     }
     
     public void updateShowHadrons( boolean showHadrons ) {
    	 renderer.updateShowHadrons(showHadrons);
     }
     
     public void updateShowCones( boolean showCones ) {
    	 renderer.updateShowCones(showCones);
     }

     public void updateShowTracker( boolean showTracker ) {
    	 renderer.updateShowTracker(showTracker);
    	 return;
     }
     
     public void updateShowStandalone( boolean showStandAlone ) {
    	 renderer.updateShowStandalone(showStandAlone);
    	 return;
     }
     
     public void updateShowGlobal( boolean showGlobal ) {
    	 renderer.updateShowGlobal(showGlobal);
    	 return;
     }
     
     public void updateVisible() {
    	 renderer.updateVisible();
    	 return;
     }
     
     public void applyPTCut() {
    	 renderer.applyPTCut();
     }

     public void setEvent( String filter ) {
    	 renderer.setEvent(filter);
    	 return;
     }

     public void clearEvent() {
    	 ((expListFragment)getFragmentManager().findFragmentById(R.id.menu_fragment)).hideItem("eventSave");
    	 renderer.clearEvent();
    	 return;
     }
     
     public List<String> getNumberOfTracks() {
    	 return renderer.getNumberOfTracks();
     }
     
     public Bundle getTrackData(int id) {
    	 return renderer.getTrackData(id);
     }
     
     public void deleteTrack(int trackID) {
    	 renderer.deleteTrack(trackID);
    	 return;
     }
    
     public void editTrack(Bundle trackData) {
    	 renderer.editTrack(trackData);
    	 return;
     }
     
     public void addTrack(Bundle trackData) {
    	 renderer.addTrack(trackData);
    	 return;
     }

     public void setHideUnderlyingEvent() {
    	 renderer.setHideUnderlyingEvent();
    	 return;
     }
     
     public void setHideUnDetectable() {
    	 renderer.setHideUnDetectable();
    	 return;
     }

     public void animate() {
    	 renderer.onAnimationStart();
    	 return;
     }

     /******** Functions for interacting with the renderer ************/     
     public void setLineWidth( float newLineWidth ) {
    	 renderer.setLineWidth(newLineWidth);
    	 return;
     }
     
     public void setDetectorHits(boolean show) {
    	 renderer.setDetectorHits(show);
    	 return;
     }
     
     public void setEventTitle( String title ) {
    	 renderer.setEventTitle(title);
    	 return;
     }
     
     public void setScreenShot(boolean isShare) {
    	 renderer.setScreenShot(isShare);
    	 return;
     }
     
     public String getLastScreenShotURI() {
    	 return renderer.getLastScreenShotURI();
     }
     
     public void setZoomSpeed( int newZoomSpeed ) {
    	 renderer.setZoomSpeed(newZoomSpeed);
    	 return;
     }
     
     public void setRotationWeight( float newRotateSpeed ) {
    	 renderer.setRotationSpeed(newRotateSpeed);
    	 return;
     }

     public boolean setTransparency( int which, int value ) {
    	 return renderer.setTransparency(which, value);
     }
     
     public boolean setVisibility( int which, boolean value ) {
    	 return renderer.setVisibility(which, value);
     }
   
     public void setHomeView() {
     	 renderer.setHomeView();
     	 return;
      }

     public void setXView() {
     	 renderer.setXView();
     	 return;
      }

     public void setZView() {
     	 renderer.setZView();
     	 return;
      }
 
     public void checkUpdatesOnNetwork() {
     	if ( this.renderer != null )
     		renderer.checkUpdatesOnNetwork();
     }
     
     /******** Functions for handling this fragment ************/
     
     public boolean setVisibleState(boolean canuseeme) {
    	 this.isVisible = canuseeme;
    	 return this.isVisible;
     }

     public boolean getVisibleState() { 
    	 return this.isVisible;
     }

     private class GestureListener extends GestureDetector.SimpleOnGestureListener {

 		private double oldDist = 0;
 		private double newDist = 0;
 		CountDownTimer timer = null;

 		@Override
 		public boolean onDown(MotionEvent event) { 

 		    if ( event.getPointerCount() > 1 ) {
 			double x = event.getX(0) - event.getX(1);
 			double y = event.getY(0) - event.getY(1);

 			if ( x+y != 0 )
 			    oldDist = 1.0/Math.sqrt(x*x + y*y);
 			else
 			    oldDist = 0.0;
 		    }
 	            return true;
 	        }

 		@Override
 		public void onLongPress(MotionEvent event) {
 			
 			final String trkInfo = renderer.getSelectedTrackInfo( (int)event.getX(0), (int)event.getY(0) );
 			if ( trkInfo == null || trkInfo.equalsIgnoreCase("none") )
 				return;
 			
 			final Toast toast = new Toast(mContext); 			
 			LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
 			
            View layout = inflater.inflate(R.layout.toast_track_data,
            		(ViewGroup)getView().findViewById(R.id.toast_track_data));
            
            TextView text = layout.findViewById(R.id.trackDataView);
            text.setText(Html.fromHtml(trkInfo));

 			timer = new CountDownTimer(30000, 1900) {
 				public void onTick(long millisUntilFinished) {
 					toast.show();
 				}
 				public void onFinish() {
 					renderer.cancelSelection();
 					this.cancel();
 				}
 			};
 			
            toast.setView(layout);
 			timer.start();
 			return;
 		}

 		@Override
 		public boolean onScroll(MotionEvent e1, MotionEvent e2, float dX, float dY) {

 		    if ( e2.getPointerCount() > 1 ) {

 		    	double x = e1.getX(0) - e2.getX(1);
 		    	double y = e1.getY(0) - e2.getY(1);

 		    	newDist = Math.sqrt(x*x + y*y);
 		    	double scale = newDist * oldDist;

 		    	renderer.incZoom(scale);

 		    	oldDist = 1.0/newDist;

 		    } else {
 		    	renderer.setTouchTurn( dX );
 		    	renderer.setTouchTurnUp( dY );
 		    }
 		    return true;
 		}

 		public boolean onDoubleTap(MotionEvent event) {
 			
 			if ( timer != null ) {
 	 			timer.onFinish();
 	 			timer = null;
 			} else if ( currentDescription != null )
 				Toast.makeText(mContext, currentDescription, Toast.LENGTH_LONG).show();
 			
 			return true;
 		}
 		
 		public boolean onSingleTapConfirmed(MotionEvent event) {
 			
 			if ( ((MainActivity)mContext).menuIsVisible() )
 				((MainActivity)mContext).closeMenu();
 			
 			return true;
 		}

 	} // End class MyGestureListener

} // End class OpenGLFragment
