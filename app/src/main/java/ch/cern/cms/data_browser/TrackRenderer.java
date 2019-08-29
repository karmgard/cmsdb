package ch.cern.cms.data_browser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.opengl.GLSurfaceView.Renderer;
import android.os.Bundle;
import android.os.Debug;
import android.os.Environment;
import android.widget.Toast;
import ch.cern.cms.data_browser.GLFont.AGLFont;

import com.threed.jpct.Camera;
import com.threed.jpct.FrameBuffer;
import com.threed.jpct.Interact2D;
import com.threed.jpct.Light;
import com.threed.jpct.Matrix;
import com.threed.jpct.Object3D;
import com.threed.jpct.Polyline;
import com.threed.jpct.RGBColor;
import com.threed.jpct.SimpleVector;
import com.threed.jpct.Texture;
import com.threed.jpct.TextureManager;
import com.threed.jpct.World;
import com.threed.jpct.util.MemoryHelper;

public class TrackRenderer implements Renderer {
    private Context       mContext = null;
    private FrameBuffer   buffer   = null;
    private World         world    = null;
    private Light         sun      = null;
    private Camera        cam      = null;
    private RGBColor back = new RGBColor(5, 5, 5);
    private Settings settings = null;
    private Logger Log = null;
    private ActivityManager activityManager = null;

    private Event eventPool = null;
    private int pointer = 0;
    private boolean first = true;

    private float touchTurn   = 0.0f;
    private float touchTurnUp = 0.0f;
    private int   zoom        = 0;
    private int zoom_speed    = 10;
    private float weight      = -0.004f;
    private int camera_motion_direction;
    private boolean screenshot = false;
    private int width = 0, height = 0;

    private String eventTitle = null;
    private String trackInfo  = null;
    
    private String screenshotURI = null;
    private boolean sharing  = false;
    private boolean animate  = false;
    private boolean clearing = false;
    public boolean drawing   = false;
    
    private double selection_radius = 0.0;
    
	private long time = 0;
	private long lastTime = 0;
	private int fps = 0;
    
    private AGLFont trackDescription = null;
    private AGLFont eventDescription = null;

    private SimpleVector origin = new SimpleVector(0,0,0);
	private CMSModel cms = null;
	private OpenGLFragment parent = null;
	
	Bitmap [] trkrTextures = new Bitmap[19];
	ByteBuffer [] texBuf = new ByteBuffer[19];
	boolean [] updated = new boolean [19];
	boolean loadEvent = false;

	private int step = -100;
	private Object3D [] bunch = new Object3D[2];

	// Public constructor for the renderer and the pool manager
    public TrackRenderer(Context context, OpenGLFragment gl ) {

    	mContext = context;
    	
    	if ( gl != null )
    		parent = gl;
    	
		if ( eventPool != null ) {
			eventPool.setContext(mContext);
			eventPool.setParent(this);
		} else {
			eventPool = new Event(mContext, this);
		}
    	
    	// Grab an instance of the settings singleton
    	if ( settings == null )
			settings = Settings.getInstance();
			
    	// And the logging singleton
    	if ( Log == null )
    		Log = Logger.getInstance();
    	
    	// Load the texture for animating particles. We only need the one, and it requires a
    	// context which we don't have in the track class so load it here and make it available
    	// The try/catch wrapper comes from the following: If the texture isn't sized at a 
    	// power of 2, then jpct will throw a RuntimeException and the app will abort. Silly,
    	// but there it is. Sadly, no matter what size a drawable is Android might try to resize
    	// it. We're loading ^2 textures as raw streams, so it should be safe from resizing, but...
    	try {
    		InputStream is = mContext.getResources().openRawResource(R.raw.halo_32);
    	
    		Bitmap bitmap = BitmapFactory.decodeStream(is);
    		Texture texture = new Texture(bitmap);
    		TextureManager.getInstance().addTexture("halo", texture);
    	
    		// Ditto the textures for the beam animation
    		is = mContext.getResources().openRawResource(R.raw.flare_128);
    		bitmap = BitmapFactory.decodeStream(is);
    		texture = new Texture(bitmap);
    		TextureManager.getInstance().addTexture("flare", texture);

    		// Create the bitmaps we'll use to show hits in the tracker
    		Bitmap.Config config = Bitmap.Config.ARGB_8888; 

    		// Load blank textures for the 19 different layers in the model 256x256 for the barrel
    		for ( int i=0; i<15; i++ ) {
    			TextureManager.getInstance().addTexture(String.format(Locale.US, "tex%03d.png", i),
    					new Texture(Bitmap.createBitmap(256, 256, config))); 
    		}
    		// And 128x128 for the endcap textures
    		for ( int i=15; i<19; i++ ) {
    			TextureManager.getInstance().addTexture(String.format(Locale.US, "tex%03d.png", i),
    					new Texture(Bitmap.createBitmap(128, 128, config))); 
    		}
    		trkrTextures = new Bitmap[19];
    		
    		// Create the image files we'll use to update the hits textures, and load the result as a new texture
    		for ( int i=0; i<15; i++ ) {
    			trkrTextures[i] = Bitmap.createBitmap(256, 256, config); 
    		}

    		for ( int i=15; i<19; i++ ) {
    			trkrTextures[i] = Bitmap.createBitmap(128, 128, config); 
    		}
    		
    		for ( int i=0; i<19; i++ )
    			updated[i] = false;
    		   
    		for ( int i=0; i<19; i++ ) {
    			texBuf[i] = ByteBuffer.allocate(trkrTextures[i].getRowBytes() * trkrTextures[i].getHeight());
    		}

    		settings.putBoolean("animation", true);

    		// Create the beam bunch textures for animations
    		bunch[0] = makeSprite(128f, "flare");
    		bunch[0].translate(0f, 0f, -1200f);

    		bunch[1] = makeSprite(128f, "flare");
    		bunch[1].translate(0f, 0f, +1200f);
        	
    	} catch ( Exception e ) {
    		Log.e("TrackRenderer()", e.toString());
    		Log.e("TrackRenderer()", "Caught exception loading textures!");
    		settings.putBoolean("animation", false);
    	}
		
    	first = true;
    	float weight  = settings.getFloatSetting("rotateSpeed");
    	int    speed  = settings.getIntSetting("zoomSpeed");

    	if ( weight != 0 )
    		this.weight = weight;
    	if ( speed > - 100000 )
    		this.zoom_speed = speed;
    	
 		// Create a new instance of the CMS detector model
    	//cms = new CMSModel(mContext);

		Paint paint = new Paint();
		paint.setAntiAlias(true);
		paint.setTypeface(Typeface.create((String)null, Typeface.BOLD));
		
		paint.setTextSize(24);
		trackDescription = new AGLFont(paint);
		eventDescription = new AGLFont(paint);
		
    	activityManager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);

    	return;
    }
    
    public void setContext(Context context) {
    	this.mContext = context;
    	return;
    }
    
    public void setParent(OpenGLFragment gl) {
    	if ( gl != null )
    		this.parent = gl;
    	return;
    }
    
    /************************ Faux Android system calls ****************************/

    public void onResume() {
    	 if ( eventPool != null )
    		 eventPool.onResume();
    }

    public void onStop() {
    	 if ( eventPool != null )
    		 eventPool.onPause();
     }

    public void onPause() {
    	if ( eventPool != null )
    		eventPool.onPause();
    }
    
    public void eventLoadSignal() {
    	parent.eventLoadSignal();
    	this.loadEvent = eventPool.isEventLoaded();
    	return;
    }
    
    /***************************************************************************
     * Animation routines                                                      *
     ***************************************************************************/
    public void onAnimationStart() {
      	 if ( eventPool != null ) {
      		 if ( eventPool.isEventLoaded() ) {
      			 animate = true;
      			 
      			 // Reset the tracker pixel textures to black/transparent
      			 for ( int layer=0; layer<19; layer++ ) {
      				 for ( int pixel1=0; pixel1<((layer<15)?256:128); pixel1++ ) {
      					 for ( int pixel2=0; pixel2<((layer<15)?256:128); pixel2++ )
      						 trkrTextures[layer].setPixel(pixel1,  pixel2,  Color.TRANSPARENT);
      				 }
      			 }
      			 for ( int layer=0; layer<19; layer++ ) {
      				 trkrTextures[layer].copyPixelsToBuffer(texBuf[layer]);
      				 TextureManager.getInstance().getTexture(String.format("tex%03d.png", layer)).overrideTexelData(texBuf[layer]);
      			 }
      				 
      			 this.setDetectorHits(true);
      			 
      		 } else
      			 Log.w("glFragment()", "No event loaded");
      	 } else
      		 Log.w("glFragment()", "Event Pool not built yet!");
      	 
      	 return;
       }

    public void onAnimationComplete() {
    	parent.onAnimationComplete();
    	
      	 if ( eventPool != null ) {
      		 if ( eventPool.isEventLoaded() ) {
       			
      			 animate = false;

       			 bunch[0].setVisibility(false);
       			 bunch[1].setVisibility(false);

       			 SimpleVector curPos = bunch[0].getTransformedCenter();
       			 SimpleVector newPos = new SimpleVector(0f, 0f, -1200f);
       			 newPos.sub(curPos);
       			 bunch[0].translate(newPos);
       			
       			 curPos = bunch[1].getTransformedCenter();
       			 newPos = new SimpleVector(0f, 0f, +1200f);
       			 newPos.sub(curPos);
       			 bunch[1].translate(newPos);
       			
       			 step = -100;
       			 pointer = 0;
       			 
       		 } else
       			 Log.w("glFragment()", "No event loaded");
       	 } else
       		 Log.w("glFragment()", "Event Pool not built yet!");

      	 return;
    }
    
    public void clearAnimation() {
    	
     	 if ( eventPool != null ) {
     		 if ( eventPool.isEventLoaded() ) {
      			
     			 animate = false;
     			 
     			 if ( settings.getBooleanSetting("response") ) {
      				List<int[]> iHits = eventPool.getHitsList();
      				int pointer = 0;
      				
      				while ( pointer < iHits.size() ) {
      				   
      					int layer  = iHits.get(pointer)[1];
      					int pixel1 = iHits.get(pointer)[2];
      					int pixel2 = iHits.get(pointer)[3];
      					
      					if ( pixel1 >= 0 && pixel1 < 256 && pixel2 >= 0 && pixel2 < 256 )
      						trkrTextures[layer].setPixel(pixel1,  pixel2,  Color.GREEN);
      					else
      						Log.e("animate()", "Hit at "+iHits.get(pointer)[0]+" ns in layer "+layer+" at ("+pixel1+", "+pixel2+")" );
      					   
      					pointer++;
      				} // Wend

      			   // Copy the images to a byte buffer
      			   for ( int i=0; i<19; i++ ) {
      				   trkrTextures[i].copyPixelsToBuffer(texBuf[i]);
      				   TextureManager.getInstance().getTexture(String.format("tex%03d.png", i)).overrideTexelData(texBuf[i]);
      			   }
      			 }
     			 
      			 this.setDetectorHits(settings.getBooleanSetting("response"));

      			 eventPool.updateVisible();
      			 eventPool.setSpriteVisibility(false);
      			
      			 bunch[0].setVisibility(false);
      			 bunch[1].setVisibility(false);

      			 SimpleVector curPos = bunch[0].getTransformedCenter();
      			 SimpleVector newPos = new SimpleVector(0f, 0f, -1200f);
      			 newPos.sub(curPos);
      			 bunch[0].translate(newPos);
      			
      			 curPos = bunch[1].getTransformedCenter();
      			 newPos = new SimpleVector(0f, 0f, +1200f);
      			 newPos.sub(curPos);
      			 bunch[1].translate(newPos);
      			
      			 step = -100;
      			 pointer = 0;
      			 
      		 } else
      			 Log.w("glFragment()", "No event loaded");
      	 } else
      		 Log.w("glFragment()", "Event Pool not built yet!");

    	
    }
    
    public boolean nextStep() {

    	if ( eventPool == null || !eventPool.isEventLoaded() ) {
 		   Log.e("onReceiver()", "Not ready to animate yet");
 		   return false;
 	   }
 	   
 	   if ( step == -100 ) {
 		   step++;

 		   eventPool.setVisible(false);
 		   
 		   bunch[0].setVisibility(true);
 		   bunch[1].setVisibility(true);
 		   step++;
 	   } else if ( step < 1 ) {
 		   bunch[0].translate(0f, 0f, +12f);
 		   bunch[1].translate(0f, 0f, -12f);
 		   step++;
 		   
 	   } else if ( step == 1 ) {
 		   eventPool.setSpriteVisibility(true);
 		   step++;
 	   } else if ( step < 400 ) {

 		   if ( step == 100 ) {
 			   bunch[0].setVisibility(false);
 			   bunch[1].setVisibility(false);
 			   
 			   SimpleVector curPos = bunch[0].getTransformedCenter();
 			   SimpleVector newPos = new SimpleVector(0f, 0f, -1200f);
 			   newPos.sub(curPos);
 			   bunch[0].translate(newPos);
 			   
 			   curPos = bunch[1].getTransformedCenter();
 			   newPos = new SimpleVector(0f, 0f, +1200f);
 			   newPos.sub(curPos);
 			   bunch[1].translate(newPos);

 		   } else if ( step < 100 ) {
     		   bunch[0].translate(0f, 0f, +12f);
     		   bunch[1].translate(0f, 0f, -12f);
 		   }
 		   
 		   int stillGoing = eventPool.setPositionAt(step*0.25f);
 		   
 		   // Check the hits in the tracker
 		   List<int[]> iHits = eventPool.getHitsList();
 		   if ( pointer < iHits.size() && iHits.get(pointer)[0] < step*0.25f ) {
 		   
 			   while ( pointer < iHits.size() && iHits.get(pointer)[0] < step*0.25 ) {
 			   
 				   int layer  = iHits.get(pointer)[1];
 				   int pixel1 = iHits.get(pointer)[2];
 				   int pixel2 = iHits.get(pointer)[3];
 				   
 				   if ( pixel1 >= 0 && pixel1 < 256 && pixel2 >= 0 && pixel2 < 256 ) {
 					   trkrTextures[layer].setPixel(pixel1,  pixel2,  Color.GREEN);
 					   updated[layer] = true;
 				   } else
 					   Log.e("onReceive()", "Hit at "+iHits.get(pointer)[0]+" ns in layer "+layer+" at ("+pixel1+", "+pixel2+")" );
 				   
 				   pointer++;
 			   } // Wend
 		   } // End if ( pointer < iHits.size() && iHits.get(pointer)[0] < step*0.25f )
 		   
 		   // Copy the images to a byte buffer
 		   for ( int i=0; i<19; i++ ) {
 			   if ( updated[i] )
 				   trkrTextures[i].copyPixelsToBuffer(texBuf[i]);
				   TextureManager.getInstance().getTexture(String.format("tex%03d.png", i)).overrideTexelData(texBuf[i]);
				   updated[i] = false;
 		   }

 		   if ( stillGoing < 10 )
 			   step += 1000;
 		   else
 			   step++;
 	   } else {
 		   onAnimationComplete();
 		   step = -100;
 		   pointer = 0;
 		   return false;
 	   }
 	   return true;
    
    }

    /******** Functions for interacting with the event pool ************/
    public void setSim(String simProc, String simChan) {
    	//if ( eventPool != null )
   		 //eventPool.getSimData(simProc, simChan);
   	 return;
    }
    
    public void updateShowAxis( boolean showAxis ) {
   	 if ( eventPool != null )
   		 eventPool.updateShowAxis(showAxis);
    }
    
    public void updateShowHadrons( boolean showHadrons ) {
   	 if ( eventPool != null )
   		 eventPool.updateShowHadrons(showHadrons);
    }
    
    public void updateShowCones( boolean showCones ) {
   	 if ( eventPool != null )
   		 eventPool.updateShowCones(showCones);
    }

    public void updateShowTracker( boolean showTracker ) {
   	 if ( eventPool != null )
   		 eventPool.updateShowTracker(showTracker);

   	 return;
    }
    
    public void updateShowStandalone( boolean showStandAlone ) {
   	 if ( eventPool != null )
   		 eventPool.updateShowStandalone(showStandAlone);
   	 return;
    }
    
    public void updateShowGlobal( boolean showGlobal ) {
   	 if ( eventPool != null )
   		 eventPool.updateShowGlobal(showGlobal);
   	 return;
    }
    
    public void updateVisible() {
   	 if ( eventPool != null )
   		 eventPool.updateVisible();

   	 boolean showTrackerHits = settings.getBooleanSetting("showTracks");
   	 if (settings.getBooleanSetting("response"))
   		 setDetectorHits(showTrackerHits);
   	 else
   		 setDetectorHits(false);

   	 return;
    }
    
    public void applyPTCut() {
   	 if ( eventPool != null )
   		 eventPool.applyPTCut();
    }

    public void setEvent( String filter ) {
   	 //if ( eventPool != null )
   		 //eventPool.getEventData(filter);
   	 return;
    }
    
    public void setEventData(List<String>data) {
    	if ( eventPool != null )
    		eventPool.setEventData(data);
    	return;
    }

    public void clearEvent() {
    	this.clearing = true;
    	return;
    }
    
    public List<String> getNumberOfTracks() {
   	 if ( eventPool != null )
   		 return eventPool.getNumberOfTracks();
   	 else
   		 return null;
    }
    
    public Bundle getTrackData(int id) {
   	 if ( eventPool != null )
   		 return eventPool.getTrackData(id);
   	 else
   		 return null;
    }
    
    public void deleteTrack(int trackID) {
   	 if ( eventPool != null )    	 
   		 eventPool.deleteTrack(trackID);
   	 return;
    }
   
    public void editTrack(Bundle trackData) {
   	 if ( eventPool != null )
   		 eventPool.editTrack(trackData);
   	 return;
    }
    
    public void addTrack(Bundle trackData) {
   	 if ( eventPool != null )
   		 eventPool.addTrack(trackData);
   	 return;
    }

    public void setHideUnderlyingEvent() {
   	 if ( eventPool != null )
   		 eventPool.setHideUnderlyingEvent();
   	 return;
    }
    
    public void setHideUnDetectable() {
   	 if ( eventPool != null )
   		 eventPool.setHideUnDetectable();
   	 return;
    }

    public boolean isEventLoaded() {
    	if ( eventPool != null )
    		return eventPool.isEventLoaded();
    	else
    		return false;
    }

    public boolean setUpWorld() {
    	if ( eventPool != null )
    		return eventPool.setUpWorld();
    	else
    		return false;
    }

    private Object3D makeSprite(float width, String texture) {
        float offset = width / 2.0f;
        Object3D obj = new Object3D( 2 );
        
        obj.addTriangle( new SimpleVector( -offset, -offset, 0 ), 0, 0,
        new SimpleVector( -offset, offset, 0 ), 0, 1,
        new SimpleVector( offset, offset, 0 ), 1, 1);
        
        obj.addTriangle( new SimpleVector( offset, offset, 0 ), 1, 1,
        new SimpleVector( offset, -offset, 0 ), 1, 0,
        new SimpleVector( -offset, -offset, 0 ), 0, 0);
        
        // Make it billboard:
        obj.setBillboarding( Object3D.BILLBOARDING_ENABLED );
        
        // Set up the transparency:
        obj.setTransparency( 100 );
        obj.setTransparencyMode( Object3D.TRANSPARENCY_MODE_ADD );
        
        obj.setSortOffset(-1250);
        
        // Add the texture
		obj.setTexture(texture);
		obj.calcTextureWrapSpherical();

		obj.setVisibility(false);
		obj.build();

        return obj;
    }

    public String getSelectedTrackInfo( int x, int y ) {

    	String temp = null;
    	if ( eventPool != null )
    		temp = eventPool.getSelectedTrackInfo(
    				this.getCam(), 
    				this.getBuffer(), 
    				this.getSelectionRadius(), 
    				x, y);
    	return temp;
    }
    
    public void cancelSelection() {
    	if ( eventPool != null )
    		eventPool.cancelSelection();
    	return;
    }
    
    /***************************************************************************/


    /********************* Inter-class communications *************************/
    public boolean addToWorld( Object3D object ) {
    	
    	if ( world != null ) {
    		world.addObject( object );
    		return true;
    	}
    	return false;
    }
    
    public boolean addToWorld( Polyline line ) {
    	
    	if ( world != null ) {
    		world.addPolyline( line );
    		return true;
    	}
    	return false;
    }

    public boolean removeFromWorld( Object3D object ) {
    	if ( world != null ) {
    		world.removeObject(object);
    		return true;
    	}
    	return false;
    }
    
    public boolean removeFromWorld( Polyline line ) {
    	if ( world != null ) {
    		world.removePolyline(line);
    		return true;
    	}
    	return false;
    }

    public void checkUpdatesOnNetwork() {
    	if ( this.cms != null )
    		cms.checkUpdatesOnNetwork();
    }
    
    /*************************************************************************/
    public String getSelection(float x, float y) {
		String txt = "";
		
		SimpleVector dir = 
				new SimpleVector(Interact2D.reproject2D3DWS(cam, buffer, (int)x,(int)y)).normalize();
		Object[] result = 
				world.calcMinDistanceAndObject3D(cam.getPosition(), dir, 100000000f);

		if ( result[1] != null ) {
			Object3D hit = (Object3D)result[1];

			String name = hit.getName();
			if ( name == "Trkr" )
				txt = mContext.getString(R.string.trkr_info);
			else if ( name == "ECal" )
				txt = mContext.getString(R.string.ecal_info);
			else if ( name == "HCal" )
				txt = mContext.getString(R.string.hcal_info);
			else if ( name == "Cryo" )
				txt = mContext.getString(R.string.cryo_info);
			else if ( name == "Yoke" )
				txt = mContext.getString(R.string.yoke_info);
			else if ( name == "Caps" )
				txt = mContext.getString(R.string.caps_info);
			else
				txt = null;
		} else
			txt = null;

		return txt;
	}

	public Camera getCam() {
		return this.cam;
	}
	
	public FrameBuffer getBuffer() {
		return this.buffer;
	}
	
	public World getWorld() {
		return this.world;
	}

	public double getSelectionRadius() {
		return this.selection_radius;
	}
	
	public void setHomeView() {
    	SimpleVector camPos = new SimpleVector(0,0,2500);
    	
    	cam.setPosition(camPos);
		Matrix rotationMatrix = new Matrix();				
		SimpleVector backVect = cms.getTransformedCenter();
		backVect.scalarMul(-1.0f);

		rotationMatrix.translate(backVect);
		rotationMatrix.rotateY((float)(-0.25*Math.PI));
		rotationMatrix.translate(cms.getTransformedCenter());
		camPos.matMul(rotationMatrix);

		cam.setPosition(camPos);
		cam.lookAt(cms.getTransformedCenter());

    	return;
    }
    
    public void setXView() {
    	SimpleVector camPos = new SimpleVector(0,0,2*constants.yokeRadius);
    	
    	cam.setPosition(camPos);
		Matrix rotationMatrix = new Matrix();				
		SimpleVector backVect = cms.getTransformedCenter();
		backVect.scalarMul(-1.0f);

		rotationMatrix.translate(backVect);
		rotationMatrix.rotateY((float)(0.5*Math.PI));
		rotationMatrix.translate(cms.getTransformedCenter());
		camPos.matMul(rotationMatrix);
		
		cam.setPosition(camPos);
    	cam.lookAt(cms.getTransformedCenter());

    	return;
    }
    
    public void setZView() {
    	cam.setPosition(new SimpleVector(0,0,-2*constants.yokeLength));
    	cam.lookAt(cms.getTransformedCenter());
    	return;
    }

    public void setScreenShot(boolean isShared) {
    	sharing = isShared;
    	this.screenshot = true;
    	
    	return;
    }
    
    public void setLineWidth(float newLineWidth) {
    	return;
    }
    
    public boolean setTransparency( int which, int value ) {
    	return cms.setTransparency(which, value);
    }
    
    public boolean setVisibility( int which, boolean value ) {
    	return cms.setVisibility(which, value);
    }
    
    public void setDetectorHits( boolean toggle ) {
    	cms.setVisibility(constants.HITS, toggle);
    	return;
    }
    
    public String getLastScreenShotURI() {
    	if ( screenshotURI != null )
    		return screenshotURI;
    	return null;
    }
    
    public void setRotationSpeed( float newRotateSpeed ) {
    	if ( newRotateSpeed < 0 )
    		this.weight = newRotateSpeed;
    	else if ( newRotateSpeed > 0 )
    		this.weight = -newRotateSpeed;
    	
    	return;
    }
    
    public void setTouchTurnUp(float ttu) {
    	if ( cam == null )
    		return;
    	
    	if ( cam.getPosition().z <= 0 )
    		this.touchTurnUp = ttu*this.weight;
    	else
    		this.touchTurnUp = -ttu*this.weight;
    	return;
    }

    public void setTouchTurn(float tt) {
	this.touchTurn = tt*this.weight;
	return;
    }

    public void setZoomSpeed( int newZoomSpeed ) {
    	if ( newZoomSpeed > 0 )
    		this.zoom_speed = newZoomSpeed;
   	return;
    }
    
    public void incZoom(double scale) {
	if ( scale > 0 && scale < 10 ) {
	    this.zoom += this.zoom_speed;
	    if ( scale > 1 ) {
		camera_motion_direction = Camera.CAMERA_MOVEIN;
	    } else {
		camera_motion_direction = Camera.CAMERA_MOVEOUT;
	    }
	}
	return;
    }

    public void setEventTitle( String title ) {
    	if ( title != null )
    		this.eventTitle = title;
    	else
    		this.eventTitle = null;
    	return;
    }
    
    public void onDrawFrame(GL10 gl) {
    	
    	this.drawing = true;
    	
    	// Move the camera into position for the new view
    	Matrix rotationMatrix = new Matrix();
    	Matrix transformMatrix = new Matrix();

    	SimpleVector camPos = cam.getPosition();
    	
    	if (touchTurn != 0) {
    		rotationMatrix.rotateY(touchTurn);
    		camPos.matMul(rotationMatrix);
    		touchTurn = 0;
    	}

    	if (touchTurnUp != 0) {					
    		transformMatrix.rotateX(touchTurnUp);
    		camPos.matMul(transformMatrix);
    		touchTurnUp = 0;
    	}
	
    	cam.setPosition(camPos);
    	cam.lookAt(origin);

    	if ( zoom != 0 ) {
    		cam.moveCamera(camera_motion_direction, zoom);
    		zoom = 0;
    	}
    	transformMatrix.setIdentity();
    	rotationMatrix.setIdentity();

    	// Clear the buffer
    	buffer.clear(back);
	
    	try {
    		// And redraw the world
    		world.renderScene(buffer);
    		world.draw(buffer);
		
    		if ( trackDescription != null && trackInfo != null )
    			trackDescription.blitStringSpecial(buffer, trackInfo, 10, (int)(0.85*this.height), this.width, 33);
    		if ( eventDescription != null && eventTitle != null )
    			eventDescription.blitStringSpecial(buffer, eventTitle, 10, 25, this.width, 90);
		
    		buffer.display();

    	} catch ( Exception e ) { e.printStackTrace(); }
		
    	if ( screenshot ) {
    		takeSnapShot(gl);
    		screenshot = false;
    	}

    	// Animate at a target rate of 20 fps
    	if ( animate && System.currentTimeMillis() - lastTime >= 50) {
    		animate = parent.nextStep();
    		lastTime = System.currentTimeMillis();
    	}

    	// Monitor the system performance (FPS & memory consumption)
    	if (System.currentTimeMillis() - time >= 1000) {
		
    		if ( settings.getIntSetting("debugLevel") >= 0 ) {
    			int [] pid = {android.os.Process.myPid()};
    			if ( activityManager != null ) {
    				Debug.MemoryInfo [] procInfo = activityManager.getProcessMemoryInfo(pid);
    				long mem = procInfo[0].getTotalPss()/1024L;
    				trackInfo = "FPS = "+fps+"|Mem = "+mem;
    			}
    		} else
    			trackInfo = null;
		
    		fps = 0;
    		time = System.currentTimeMillis();
    	}
    	fps++;
	
    	if ( this.clearing )
    		clearEventObjects();
	
    	if ( this.loadEvent )
    		loadEventObjects();
	
    	this.drawing = false;

    	return;
    }

    private void loadEventObjects() {
    	
    	//Log.w("loadEventObjects()", "New event, loading event objects");
    	
		List<Polyline> lineList = new ArrayList<Polyline>();
		List<Object3D> objList  = new ArrayList<Object3D>();
		
		lineList = eventPool.getEventTracks();
		objList  = eventPool.getEventObjects();
		
		boolean doLoad = true;
		
		if (lineList == null ) {
			Log.w("onDraw()", "Null list for polylines!");
			doLoad = false;
		}
		if ( objList == null ) {
			Log.w("onDraw()", "Null list for objects!");
			doLoad = false;
		}

		if (lineList.size() == 0 && objList.size() == 0)
			doLoad = false;

		if ( doLoad ) {
			for ( int i=0; i<lineList.size(); i++ )
				world.addPolyline(lineList.get(i));
			for ( int i=0; i<objList.size(); i++ )
				world.addObject(objList.get(i));

			List<int[]> iHits = eventPool.getHitsList();
			int pointer = 0;
			
			//Log.w("loadEventObjects()", "Turning on "+iHits.size()+" hit pixels");
			while ( pointer < iHits.size() ) {
			   
				int layer  = iHits.get(pointer)[1];
				int pixel1 = iHits.get(pointer)[2];
				int pixel2 = iHits.get(pointer)[3];
				
				if ( pixel1 >= 0 && pixel1 < 256 && pixel2 >= 0 && pixel2 < 256 )
					trkrTextures[layer].setPixel(pixel1,  pixel2,  Color.GREEN);
				else
					Log.e("onReceive()", "Hit at "+iHits.get(pointer)[0]+" ns in layer "+layer+" at ("+pixel1+", "+pixel2+")" );
				   
				pointer++;
			} // Wend

		   // Copy the images to a byte buffer
		   for ( int i=0; i<19; i++ ) {
			   trkrTextures[i].copyPixelsToBuffer(texBuf[i]);
			   TextureManager.getInstance().getTexture(String.format("tex%03d.png", i)).overrideTexelData(texBuf[i]);
		   }
		}
		
    	this.loadEvent = false;
    	return;
    }
    
    private void clearEventObjects() {
    
    	//Log.w("onDraw()", "Clearing events in the renderer");
		
    	List<Polyline> lineList = new ArrayList<Polyline>();
		List<Object3D> objList  = new ArrayList<Object3D>();
		
		lineList = eventPool.getEventTracks();
		objList  = eventPool.getEventObjects();
		
		boolean doClear = true;
		
		if (lineList == null ) {
			Log.w("onDraw()", "Null list for polylines!");
			doClear = false;
		}
		if ( objList == null ) {
			Log.w("onDraw()", "Null list for objects!");
			doClear = false;
		}

		if (lineList.size() == 0 && objList.size() == 0)
			doClear = false;
		
		if ( doClear ) {
			//Log.w("clearEventObjects()", "Clearing "+lineList.size()+" lines & "+objList.size()+" objects");
			
			for ( int i=0; i<lineList.size(); i++ )
				world.removePolyline(lineList.get(i));
			for ( int i=0; i<objList.size(); i++ )
				world.removeObject(objList.get(i));
			
			// If the textures didn't load... bail now before we crash
	    	if ( settings.getBooleanSetting("animation") ) {

	    		//Log.w("onDrawFrame()", "Clearing tracker textures");
	    	
	    		// Reset the tracker pixel textures to black
	    		for ( int layer=0; layer<19; layer++ ) {
	    			for ( int pixel1=0; pixel1<((layer<15)?256:128); pixel1++ ) {
	    				for ( int pixel2=0; pixel2<((layer<15)?256:128); pixel2++ )
	    					trkrTextures[layer].setPixel(pixel1,  pixel2,  Color.TRANSPARENT);
	    			}
	    		}
	    		for ( int layer=0; layer<19; layer++ ) {
	    			trkrTextures[layer].copyPixelsToBuffer(texBuf[layer]);
	    			TextureManager.getInstance().getTexture(String.format("tex%03d.png", layer)).overrideTexelData(texBuf[layer]);
	    		}
	    		
	    	} // End if ( settings.getBooleanSetting("animation") ) 
	    	
	    	// Flush the event pool
			eventPool.clearEvent();
		} // End if ( doClear )
	
		//Log.w("clearEventObjects()", "Done");
		this.clearing = false;
	
		return;
    }
    
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
	return;
    }

    public void onSurfaceChanged(GL10 gl, int w, int h) {

	if ( buffer != null )
	    buffer.dispose();

	buffer = new FrameBuffer(gl, w, h);

	this.width = w;
	this.height = h;
	this.selection_radius = Math.sqrt((0.01f*w*h)/Math.PI);
	
	if ( first ) { 

	    first = false;
	
	    world = new World();
	    world.setAmbientLight(120, 120, 120);
	    world.setClippingPlanes(1f, 5000f);

	    // Create a new instance of the CMS detector model
	    if ( cms == null )
	    	cms = new CMSModel(mContext, world);
	    
	    sun = new Light(world);
	    sun.setIntensity(255,255,255);
	    
	    cam = world.getCamera();
	    cam.moveCamera(Camera.CAMERA_MOVEOUT, 2500);
	    
	    Matrix rotationMatrix = new Matrix();				
	    SimpleVector backVect = new SimpleVector(0,0,0);
	    SimpleVector camPos = cam.getPosition();

	    rotationMatrix.translate(backVect);
	    rotationMatrix.rotateY((float)(-0.25*Math.PI));
	    rotationMatrix.translate(backVect);
	    camPos.matMul(rotationMatrix);
	    
	    cam.setPosition(camPos);
	    cam.lookAt(backVect);

	    SimpleVector sv = new SimpleVector();
	    sv.set(backVect);
	    sv.y -= 100;
	    sv.z -= 100;
	    
	    sun.setPosition(sv);
	    MemoryHelper.compact();

	    time = lastTime = System.currentTimeMillis();
	    
	    world.addObject(bunch[0]);
	    world.addObject(bunch[1]);
	    
	    // Tell papa we're done setting up the GL surface
	    // so it can add & remove the tracks & sprites and such
		eventPool.setUpWorld();

	}

		return;
    }
    
    public boolean takeSnapShot( GL10 gl ) {
    	if ( width == 0 || height == 0 || gl == null  ) {
			return false;
		}
		
        int screenshotSize = width * height;
        ByteBuffer bb = ByteBuffer.allocateDirect(screenshotSize * 4);
        bb.order(ByteOrder.nativeOrder());
        gl.glReadPixels(0, 0, width, height, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, bb);
		int[] pixelsBuffer = new int[screenshotSize];
        bb.asIntBuffer().get(pixelsBuffer);
        bb = null;
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        bitmap.setPixels(pixelsBuffer, screenshotSize-width, -width, 0, 0, width, height);
        pixelsBuffer = null;

		short[] sBuffer = new short[screenshotSize];
        ShortBuffer sb = ShortBuffer.wrap(sBuffer);
        bitmap.copyPixelsToBuffer(sb);

        //Making created bitmap (from OpenGL points) compatible with Android bitmap
        for (int i = 0; i < screenshotSize; ++i) {                  
            short v = sBuffer[i];
            sBuffer[i] = (short) (((v&0x1f) << 11) | (v&0x7e0) | ((v&0xf800) >> 11));
        }
        sb.rewind();
        bitmap.copyPixelsFromBuffer(sb);
        
        try {
        	if ( isExternalStorageReadable() ) {
        		
        		//File destDir = getAlbumStorageDir("CMS");  API < 8
        		File destDir = new File(Environment.getExternalStoragePublicDirectory(
        	            Environment.DIRECTORY_PICTURES), "CMS");

        		if ( !destDir.isDirectory() && !destDir.mkdirs() ) {
    				Log.e("Renderer()", "Can't create directory "+destDir+"/CMS");
    				Toast.makeText(mContext,  "Unable to save snapshot", Toast.LENGTH_SHORT).show();
    				return false;
        		}

        		SimpleDateFormat s = new SimpleDateFormat( "ddMMyyyyhhmmss", Locale.US );
        		String format = s.format(new Date());
        		String fileName = destDir + "/screenshot-" + format + ".png";

        		FileOutputStream fos = new FileOutputStream(fileName);
        		bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
        		fos.flush();
        		fos.close();
        		
        		screenshotURI = fileName;

        		if ( sharing )
        			((MainActivity)mContext).shareSnapshot(screenshotURI);
        		
        		return true;
        	}
        } catch (Exception e) {
            Log.e("Renderer().screenshot()",e.toString());
        }
        
    	return false;
    }
    
    /* Checks if external storage is available to at least read */
    public boolean isExternalStorageReadable() {
    	String state = Environment.getExternalStorageState();
		return Environment.MEDIA_MOUNTED.equals(state) ||
				Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
	}
    
    public File getAlbumStorageDir(String albumName) {

    	// A nice method which requires API 8.
    	//File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), albumName);

    	// Get the directory for the user's public pictures directory.
        File file = new File(Environment.getExternalStorageDirectory().getPath() + "/Pictures/" + albumName);

        if ( file.exists() )
        	return file;
        else if (!file.mkdirs()) {
            Log.e("Renderer()", "Directory not created");
            return null;
        }
        return file;
    }

} // End TrackRenderer()
