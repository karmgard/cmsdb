package ch.cern.cms.data_browser;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.graphics.Color;

import com.threed.jpct.Camera;
import com.threed.jpct.FrameBuffer;
import com.threed.jpct.Interact2D;
import com.threed.jpct.Matrix;
import com.threed.jpct.Object3D;
import com.threed.jpct.Polyline;
import com.threed.jpct.Primitives;
import com.threed.jpct.RGBColor;
import com.threed.jpct.SimpleVector;

/**********************************************
 * Simple class to encapsulate a point in     *
 * 3-d for convenience and readability        *
 **********************************************/
class point {

    private double x = 0.0;
    private double y = 0.0;
    private double z = 0.0;
    private double r = 0.0;
    private int layer = -1;
    private int timeStamp = -1;

    // Principle constructor... make a point in space from 3 doubles
    public point(double x0, double y0, double z0) {

	try {
	    this.x = x0;
	    this.y = y0;
	    this.z = z0;
	    this.r = Math.sqrt(x0*x0 + y0*y0);
	} catch (Exception e) {
	    return;
	}
	return;
    } // End point(x0,y0,z0)

    // Alternate constructor from a SimpleVector instead of three doubles
    public point (SimpleVector p) {
    	try {
    	    this.x = p.x;
    	    this.y = p.y;
    	    this.z = p.z;
    	    this.r = Math.sqrt(x*x + y*y);
    	} catch (Exception e) {
    	    return;
    	}
    	return;
        } // End point(SimpleVector)

    // Empty constructor
    public point() {
	this.x = 0;
	this.y = 0;
	this.z = 0;
	this.r = 0;
	return;
    } // End point()

    private double setR() {
	try {
	    this.r = Math.sqrt(this.x*this.x + this.y*this.y);
	} catch(Exception e) {
	    return 0.0;
	}
	return this.r;
    }

    public double setX( double x0 ) {
	try{
	    this.x = x0;
	} catch ( Exception e) {
	    return 0.0;
	}
	this.setR();
	return this.x;
    }

    public double setY( double y0 ) {
	try{
	    this.y = y0;
	} catch ( Exception e) {
	    return 0.0;
	}
	this.setR();
	return this.y;
    }

    public double setZ( double z0 ) {
	try{
	    this.z = z0;
	} catch ( Exception e) {
	    return 0.0;
	}
	return this.z;
    }

    public double getX() {
	return this.x;
    }

    public double getY() {
	return this.y;
    }

    public double getZ() {
	return this.z;
    }

    public double getR() {
	return this.r;
    }

    public double addX( double xa ) {
	try{
	    this.x += xa;
	} catch ( Exception e) {
	    return 0.0;
	}
	this.setR();
	return this.x;
    }

    public double addY( double ya ) {
	try{
	    this.y += ya;
	} catch ( Exception e) {
	    return 0.0;
	}
	this.setR();
	return this.y;
    }

    public double addZ( double za ) {
	try{
	    this.z += za;
	} catch ( Exception e) {
	    return 0.0;
	}
	this.setR();
	return this.z;
    }

    public double multiplyX( double xm ) {
	try {
	    this.x *= xm;
	} catch ( Exception e) {
	    return 0.0;
	}
	this.setR();
	return this.x;
    }

    public double multiplyY( double ym ) {
	try {
	    this.y *= ym;
	} catch ( Exception e) {
	    return 0.0;
	}
	this.setR();
	return this.y;
    }

    public double multiplyZ( double zm ) {
	try {
	    this.z *= zm;
	} catch ( Exception e) {
	    return 0.0;
	}
	this.setR();
	return this.z;
    }

    /************************************************************************************/

    public double phi() {
    	return Math.atan2(this.y,this.x);
    }

    public double theta() {
    	double R =  Math.sqrt(this.x*this.x + this.y*this.y + this.z*this.z);
    	return Math.acos(this.z/R);
    }

    public double eta() {
    	double theta = 0.5*this.theta();
    	double tan = Math.tan(theta);
    	double eta = -Math.log(tan);
    	return Math.abs(eta);
    }

    public double diff( point pnt ) {
    	return Math.sqrt( (this.x-pnt.getX())*(this.x-pnt.getX()) + 
    				(this.y-pnt.getY())*(this.y-pnt.getY()) + 
    				(this.z-pnt.getZ())*(this.z-pnt.getZ()) );
    }

    /************************************************************************************/
    
    public void setLayer( int layer ) {
    	this.layer = layer;
    	return;
    }
    
    public int getLayer() {
    	return this.layer;
    }
    
    public void setTimeStamp( int timeStamp ) {
    	this.timeStamp = timeStamp;
    	return;
    }

    public int getTimestamp() {
    	return this.timeStamp;
    }
    
    public double magnitude() {
	return Math.sqrt(this.x*this.x + this.y*this.y + this.z*this.z);
    }

    public boolean copy( point p ) {
	if ( p == null )
	    return false;
	try {
	    this.x = p.x;
	    this.y = p.y;
	    this.z = p.z;
	    this.r = p.r;
	} catch ( Exception e ) {
	    return false;
	}
	return true;
    }

    public double [] getDoubleArray() {
   	return new double [] {this.x,this.y,this.z};
    }
    
    public SimpleVector getVector() {
	SimpleVector sv = new SimpleVector( this.x, this.y, this.z);
	return sv;
    }

    public String dump() {
	return "(x,y,z,r) = (" + x + ", " + y + ", "
	    + z + ", " + r + ")";
    }

} // End class point

class particle {
    
    // Class internals 
    point    p          = null;
    point    v          = null;
    double   gamma      = 0.0;
    double   vtx        = 0.0;
    double   r          = 0.0;
    double   w          = 0.0;
    double   phase      = 0.0;
    int      id         = 0;
    int      q          = 0;
    double   m          = -1.0;
    double   P          = 0.0;
    double   Pt         = 0.0;
    double   E          = 0.0;
    double   e          = 0.0;
    double   phi        = 0.0;
    double   theta      = 0.0;
    double   eta        = 0.0;
    int      punch      = 0;
    int      type       = 2;
    int      length     = 0;
    int      time       = 0; 
    double  timeStep   = 0f;
    point    oldPoint   = null;
    double  pathLength = 0.0;
    double  maxPath    = 0.0;
    int     lifeTime   = 5;
    boolean hasJetCone = false;
    
    String   colorName  = "black";
    RGBColor color      = new RGBColor(0xff, 0x00, 0x00);
    String   name       = "";
    double   mass       = 0.0;
    boolean  dirty      = false;
    boolean  active     = false;
    boolean  detectable = true;
    boolean  visible    = true;
    
    Polyline track      = null;
    Object3D jetCone    = null;
    Object3D tower      = null;
    Object3D glow       = null;
    
    List<muonHit> muonHits = null;
    
    boolean createJetCone = false;
    boolean addedToWorld  = false;
    
    ArrayList<SimpleVector> geometry = new ArrayList<SimpleVector>();
	
    Object3D sprite = null;

    private Settings settings = null;  // Pointer to the settings class singleton
    private Logger Log = null;         // Pointer to the logger class singleton
    
    public  float  lineWidth  = 2.5f;
    private float  trackRes = 1.0f;

    private boolean trackDump = false;
    
    // Empty constructor
    public particle() {
    	return;
    }

    // Almost Empty constructor
    public particle(int TID) {
    	if ( TID < 0 ) {
    		Log.w("Particle", "Attempt to assign invalid track ID: "+ TID);
    		return;
    	}
    	this.id = TID;
    	settings = Settings.getInstance();
    	Log = Logger.getInstance();
    	
    	// Make the sprite
    	if ( settings.getBooleanSetting("animation") )
    		this.sprite = makeSprite(32f);

    	return;
    }

    // Main constructor
    public particle(int TID, double px, double py, double pz, int q, int type, String c, double vtx) {

	if ( TID < 0 ) {
	    Log.w("Particle", "Attempt to assign invalid track ID: "+ TID);
	    return;
	}

	// Assign the track ID
	this.id = TID;

	settings = Settings.getInstance();
	Log = Logger.getInstance();

	this.p = new point(px,py,pz);

	this.q = q;
	this.type = type;
	if ( this.type == constants.HD )
		this.maxPath = constants.hcalRadius - constants.ecalRadius;
	else if (this.type == constants.EM )
		this.maxPath = constants.ecalRadius - constants.trkrRadius;

	if ( !getBooleanSetting("isData") ) {
	    if ( this.type == constants.BK ) {
		if ( Math.random() < 0.2 ) {
		    if ( Math.random() > 0.5 )
			this.punch = 1;
		    else
			this.punch = 2;
		} else
		    this.punch = 0;
	    }
	} else
	    this.punch = 0;

	this.lineWidth = getFloatSetting("lineWidth");
	this.trackRes  = getFloatSetting("trackRes");

	this.colorName = c;
	int iColor = Color.parseColor(this.colorName);

	// Get the RGB components of the integer color
	int blue  = (iColor&0x0000ff);
	int green = (iColor&0x00ff00) >> 8;
	int red   = (iColor&0xff0000) >> 16;

	this.color = new RGBColor(red, green, blue);
	this.vtx   = vtx;

	this.setMass();
	this.setRadius();
	this.setOmega();
	this.setPhase();

	this.visible = true;
	this.dirty = true;
	
	// Set the time increment based on the resolution we need
    // Base increment is trackRes (from settings) but alter it
	// some based on charge and type to try and strike a balance 
	// between calculation/rendering speed and smoothness in 
	// the graphics
    if ( this.q != 0 ) {
    	if ( this.type == constants.MU )
    		timeStep = this.trackRes/20.0;
    	else
    		timeStep = this.trackRes/10.0;
    } else
    	timeStep = this.trackRes/5.0;
	
	// Make the texture plane if this is not a neutrino or MET
	if ( this.type != constants.MET && !(this.type == constants.MU && this.q == 0) )
		sprite = makeSprite(75.0f);
	
	if (trackDump)
	    this.dump("particle()");

	return;
    } // End particle(int TID, double px, double py, double pz, int q, int type, int color, double vtx

    // Set up a previously constructed track
    public void set(double px, double py, double pz, int q, int type, String c, double vtx, String n) {

    	this.p = new point(px,py,pz);

    	this.q = q;
    	this.type = type;
    	if ( this.type == constants.HD )
    		this.maxPath = constants.hcalRadius - constants.ecalRadius;
    	else if (this.type == constants.EM )
    		this.maxPath = constants.ecalRadius - constants.trkrRadius;

    	if ( !getBooleanSetting("isData") ) {
    		if ( this.type == constants.BK ) {
    			if ( Math.random() < 0.2 ) {
    				if ( Math.random() > 0.5 )
    					this.punch = 1;
    				else
    					this.punch = 2;
    			} else
    				this.punch = 0;
    		}
    	} else
    		this.punch = 0;

    	this.lineWidth = getFloatSetting("lineWidth");
    	this.trackRes  = getFloatSetting("trackRes");

    	this.colorName = c;
    	int iColor = Color.parseColor(this.colorName);

    	// Get the RGB components of the integer color
    	int blue  = (iColor&0x0000ff);
    	int green = (iColor&0x00ff00) >> 8;
    	int red   = (iColor&0xff0000) >> 16;

    	this.color = new RGBColor(red, green, blue);
    	this.vtx   = vtx;

    	this.setMass();
    	this.setRadius();
    	this.setOmega();
    	this.setPhase();
    	
    	this.visible = false;
    	this.dirty = true;
	
    	this.setName(n);
    	
    	// Set the time increment based on the resolution we need
    	// Base increment is trackRes (from settings) but alter it
    	// some based on charge and type to try and strike a balance 
    	// between calculation/rendering speed and smoothness in 
    	// the graphics
    	if ( this.q != 0 ) {
    		if ( this.type == constants.MU )
    			timeStep = this.trackRes/20.0;
    		else
    			timeStep = this.trackRes/10.0;
    	} else
    		timeStep = this.trackRes/5.0;
	
    	// Create the track
    	this.makeTrack();

    	if ( this.name.toLowerCase(Locale.US).equals("jet") )
    		this.drawJetCone();
    	
    	this.updateVisible();
    	
    	if (trackDump)
    		this.dump("particle()");

    	// We're now active, reset the lifetime for pool cleaning
    	this.active = true;
    	this.lifeTime = 5;
    	
    	return;
    } // End set(double px, double py, double pz, int q, int type, int color, double vtx)
    
    /*********************************************************/
    /******** Private accessors into the setting class *******/
    /*********************************************************/
    private float getFloatSetting( String key ) {
    	return settings.getFloatSetting(key);
    }
    private int getIntSetting( String key ) {
    	return settings.getIntSetting(key);
    }
    private boolean getBooleanSetting( String key ) {
    	return settings.getBooleanSetting(key);
    }
    
    /*********************************************************/
    /******************** Utility functions ******************/
    /*********************************************************/
    // Set the ID
    public boolean setID(int TID) {

	if ( TID < 0 ) {
	    Log.w("Particle", "Attempt to assign invalid track ID: "+ TID);
	    return false;
	}
	return true;
    }

    // Set the particle name if known
    public void setName(String name) {

    if ( name == null ) {
    	if ( this.type == constants.EM ) {
    		if ( this.q == 0 )
    			this.name = "photon";
    		else if ( this.q == -1 )
    			this.name = "e-";
    		else
    			this.name = "e+";
    	} else if ( this.type == constants.HD ) {
    		if ( this.q == 0 )
    			this.name = "pi0";
    		else if ( this.q == -1 )
    			this.name = "pi-";
    		else
    			this.name = "pi+";
    	} else if ( this.type == constants.MU ) {
    		if ( this.q == 0 )
    			this.name = "neutrino";
    		else if ( this.q == -1 )
    			this.name = "mu-";
    		else
    			this.name = "mu+";
    	} else if ( this.type == constants.TK )
    		this.name = "track";
    	else
    		this.name = "unknown";
    	return;
    }
    	
    this.name = name;
    
	if ( trackDump )
	    this.dump("setName() = " + this.name);

	return;
    }

    // Set the track color
    public void setCLR(String color) {
    	
    	if ( color.equals(colorName) )
    		return;
    	
    	this.colorName = color;
    	int iColor = Color.parseColor(this.colorName);

    	int blue  = (iColor&0x0000ff);
    	int green = (iColor&0x00ff00) >> 8;
    	int red   = (iColor&0xff0000) >> 16;

    	this.color = new RGBColor(red, green, blue);
    	this.dirty = true;
    	
    	return;
    }

    public boolean setVtx(double vertex) {
	if ( this.vtx == Math.round(vertex) )
	    return false;

	this.vtx = vertex;

	if ( this.vtx < -25 )
	    this.vtx = -25;
	else if ( this.vtx > 25 )
	    this.vtx = 25;

	this.dirty = true;

	if ( trackDump )
	    this.dump("setVTX() = "+ this.vtx);

	return true;
    }

    // Set the magnitude of the momentum vector
    public boolean setP() {

	if ( this.p == null )
	    return false;

	this.P = p.magnitude();
	
	// We probably need to recalculate E & P_T as well
	this.setPt();
	this.setE();

	// We certainly need to recalculate eta/theta/phi
	this.setPhi();
	this.setTheta();
	this.setEta();      // Detectability checked here

	if ( trackDump )
	    this.dump("setP() = "+ this.P);

	return true;
    } // End setP()

    // set the magnitude of the transverse momentum
    public boolean setPt() {
	if ( this.p == null )
	    return false;

	this.Pt = p.getR();

	float ptmin = getFloatSetting( "ptcut" );
	this.visible = this.Pt > ptmin;

	this.setRadius();

	if ( getIntSetting("debugLevel")  > 0 )
	    this.dump("setPt() = "+ this.Pt);

	return true;
    } // End setPt()

    // set the particle energy from an external source
    public boolean setEnergy(double e) {
	if ( this.e == e )
	    return false;
	this.e = e;

	if ( this.e < 100 ) {
		
		if ( this.e < 25 )
			this.maxPath *= 0.25;
		else
			this.maxPath *= 0.01*this.e;
		
	}

	if ( getIntSetting("debugLevel")  > 0 )
	    this.dump("setEnergy() = "+ this.e);

	return true;
    } // End setEnergy(double e)

    // Calculate the energy of the particle 
    public boolean setE() {
	if ( this.P == 0 )
	    return false;

	this.E = Math.sqrt( this.P*this.P + this.m*this.m );

	if ( this.E < 100 ) {
		
		if ( this.E < 25 )
			this.maxPath *= 0.25;
		else
			this.maxPath *= 0.01*this.E;
		
	}
	
	if ( getIntSetting("debugLevel")  > 0 )
	    this.dump("setE() = "+ this.E);

	return true;
    } // End setE()

    // Calculate the gyroradius in the central field, end up with cm
    public boolean setRadius() {

	double r = 0.0;
	
	// 87.9 for B = 3.8T, 83.3 for B = 4T and R in cm
	r = (this.q != 0) ? 83.3*this.Pt/Math.abs(this.q) : Double.POSITIVE_INFINITY;

	if ( this.r == r )
	    return false;
	else
	    this.dirty = true;

	this.r = r;

	if ( getIntSetting("debugLevel")  > 0 )
	    this.dump("setRadius() = "+ this.r);
	return true;
    } // End setRadius()

    // Calculate the Cyclotron frequency... we'll end up with GHz
    public boolean setOmega() {

	double w = 0.0;

	if ( this.gamma == 0 || this.m == 0 )
		w = Double.POSITIVE_INFINITY;
	else
		w = 0.36*this.q/(this.gamma*this.m);
	
	if ( this.w == w )
	    return false;
	else
	    this.dirty = true;

	this.w = w;

	if ( getIntSetting("debugLevel")  > 0 )
	    this.dump("setOmega() = "+ this.w);

	return true;
    }

    // Phase angle for particle orbits
    public boolean setPhase() {

	double phase = this.phase;

	if ( this.p == null )
	    return false;

	this.phase = Math.atan2(this.p.getX(), this.p.getY());

	if ( this.phase == phase )
	    return false;
	else
	    this.dirty = true;

	if ( trackDump )
	    this.dump("setPhase() = "+ this.phase);

	return true;
    } // end setPhase()

    public boolean setDetectable() {
    	
    	if ( getBooleanSetting("isData") ) {
    		this.detectable = true;
    		return true;
    	}

    	if ( this.type == constants.MET ) {
    		this.detectable = true;
    		return true;
    	}

    	// Check by path... in the pipe? No can do
		this.detectable = !(Math.abs(this.eta) > 5.22);

    	// Ought to be the same thing as checking by eta, but....
    	this.detectable = (this.detectable) && (this.r > constants.layers[0]);

    	// Regardless of path, neutrinos are not detectable in CMS
    	if ( this.type == constants.MU )
    		this.detectable = (this.detectable) && (this.q != 0);

    	if ( !this.detectable ) {
    		RGBColor rgb = this.color;
    		int r = (int)(0.5*rgb.getRed());
    		int g = (int)(0.5*rgb.getGreen());
    		int b = (int)(0.5*rgb.getBlue());
		
    		this.color = new RGBColor(r,g,b);
    		//this.track.setColor(this.color);
    	}
    	
    	if ( getIntSetting("debugLevel")  > 0 )
    		this.dump("setDetectable() = Particle "+this.id+" detectability = "+this.detectable);

    	return true;
    } // End setDetectable()

    // Interesting quantities for physicists
    public boolean setPhi() {

	double phi = Math.atan2(this.p.getY(),this.p.getX());
	if ( this.phi == phi )
	    return false;
	this.phi = phi;
	this.dirty = true;

	if ( trackDump )
	    this.dump("setPhi() = "+ this.phi);

	return true;
    } // End setPhi()

    public boolean setTheta() {

	double theta = Math.acos(this.p.getZ()/this.P);
	if ( this.theta == theta )
	    return false;

	this.theta = theta;
	this.dirty = true;

	if ( trackDump )
	    this.dump("setTheta() = "+ this.theta);

	return true;
    } // End setTheta()

    public boolean setEta() {
	double eta = 0.5*Math.log( (this.P+this.p.getZ())/(this.P-this.p.getZ()) );

	if ( this.eta == eta )
	    return false;
	this.eta = eta;

	// Check the detectability
	this.setDetectable();

	this.dirty = true;

	if ( trackDump )
	    this.dump("setEta() = "+ this.eta);

	return true;
    } // End setEta()

    public boolean setEta(double iEta) {
	if ( this.eta == iEta )
	    return false;

	this.eta = iEta;

	// Check the detectability
	this.setDetectable();

	this.dirty = true;

	if ( trackDump )
	    this.dump("setEta("+ iEta +")");

	return true;
    } // End setEta(iEta)

    public boolean setP( double [] P ) {

    	this.p.setX(P[0]);
    	this.p.setY(P[1]);
    	this.p.setZ(P[2]);
    	this.P = this.p.magnitude();
    	
    	// Now... recalculate all the other stuff since we just changed the momentum
    	this.setRadius();
    	this.setVelocity();
    	this.setPhase();
    	
    	// And we're all dirty again... time for a bath
    	this.dirty = true;

    	if ( trackDump )
    	    this.dump("setP() = "+ this.p.magnitude());

    	return true;
    } // End setP(double [])
    public boolean setPX( double px ) {

	if ( this.p.getX() == px )
	    return false;

	this.p.setX(px);

	// Now... recalculate all the other stuff since we just changed the momentum
	this.setRadius();
	this.setVelocity();
	this.setP();

	// And we're all dirty again... time for a bath
	this.dirty = true;

	if ( trackDump )
	    this.dump("setPX() = "+ this.p.getX());

	return true;
    } // end setPX(px)

    public boolean setPY( double py ) {

	if ( this.p.getY() == py )
	    return false;

	this.p.setY(py);

	// Now... recalculate all the other stuff since we just changed the momentum
	this.setRadius();
	this.setVelocity();
	this.setP();

	// And we're all dirty again... time for a bath
	this.dirty = true;

	if ( trackDump )
	    this.dump("setPY() = "+ this.p.getY());

	return true;
    } // End setPY(py)

    public boolean setPZ( double pz ) {

	if ( this.p.getZ() == pz )
	    return false;

	this.p.setZ(pz);

	// Now... recalculate all the other stuff since we just changed the momentum
	this.setRadius();
	this.setVelocity();
	this.setP();

	// And we're all dirty again... time for a bath
	this.dirty = true;

	if ( trackDump )
	    this.dump("setPZ() = "+ this.p.getZ());

	return true;
    } // End setPZ(pz)

    public boolean setVelocity() {
	if ( this.p == null )
	    return false;

	if ( this.v == null )
	    this.v = new point();

	if ( this.P == 0 )
	    this.setP();

	// Speed of light.
	double c  = 3e8;
	
	double V = c*(this.P/Math.sqrt(this.m*this.m + this.P*this.P));
	
	// Changing v changes phase... reset it
	//this.setPhase();

	// Set the relativistic factor
	this.gamma = (V != c) ? 1/Math.sqrt(1 - ( (V*V)/(c*c) )) : Double.POSITIVE_INFINITY;

	this.setOmega();
	
	// Convert to cm/ns
	V *= 1e-7;
	
    // split up the speed by components of p
	this.v.setX( +V*this.p.getX()/this.P );
	this.v.setY( V*this.p.getY()/this.P );	
	this.v.setZ( V*this.p.getZ()/this.P );	
	
	if ( trackDump )
	    this.dump("setVelocity() = "+this.v.dump());

	return true;
    } // end setVelocity()

    public boolean setQ(int q) {

	if ( this.q == Math.round(q) )
	    return false;

	this.q = Math.round(q);

	// Since we're indexing particle type by q.. we have to reset
	this.setMass();
	this.setRadius();

	// Recheck the detectability of this particle
	this.setDetectable();

	this.dirty = true;

	if ( trackDump )
	    this.dump("setQ() = "+ this.q);

	return false;
    } // End setQ(q)

    public boolean setType(int type) {

	if ( this.type == Math.round(type) )
	    return false;

	if ( type < constants.EM )
	    type = constants.EM;
	else if ( type > constants.MU )
	    type = constants.MU;

	this.type = Math.round(type);

	// Different types use different
	// canonical masses.. set it
	this.setMass();

	// Recheck the detectability of this particle
	this.setDetectable();

	this.dirty = true;

	if ( trackDump )
	    this.dump("setType() = "+ this.type);

	return true;
    } // end setType(type)

    // For the public data events, push an actual mass into the class
    public boolean setMass(double m) {
	this.m = m;
	if ( trackDump )
	    this.dump("setMass("+ this.m +")");

	return true;
    }

    // For basic particles assume a particular mass
    public boolean setMass() {

	double mass = this.m;
	if ( this.q == 0 )
	    mass = 0;
	else if ( this.type == constants.EM )
	    mass = 0.000511;  // Electron mass in GeV
	else if ( this.type == constants.MU )
	    mass = 0.1057;    // Muon mass in GeV
	else 
	    mass = 0.139570; // Charged pion mass in GeV, Kaon mass = 0.770 GeV
	
	// If the mass was unchanged.. don't bother
	if ( this.m == mass )
	    return false;

	this.m = mass;

	// Changing the mass changes the velocity
	this.setVelocity();

	if ( trackDump )
	    this.dump("setMass() = "+ this.m);

	return true;
    } // End setMass()

    public boolean pathStop( point pnt ) {

	// Everything else is integer centimeters. So too here
	double r = Math.round(pnt.getR());
	double z = Math.abs(Math.round(pnt.getZ()));

	// If we've exited the detector... stop drawing
	if ( r > constants.yokeRadius || z > 0.5*constants.pipeLength ) {
	    return true;
	}
	
	/*************** Deal with each type seperately *****************/

	// Muons and neutrinos go and don't stop in the detector
	// and the pipeline stuff (type = -2) just goes down the line
	if ( Math.abs(this.type) == constants.MU )
	    return false;

	// For any type, if we're still in the pipe, keep going
	if ( r < constants.pipeRadius && !getBooleanSetting("isData") ) 
	    return false;

	// Underlying event -- splash the preshower and quit
	if ( this.type == constants.BK || this.type == constants.TK ) {

	    if ( this.punch == 0 ) {
		// Out of the detector
		if (z > 0.5*constants.trkrLength || r > constants.trkrRadius )
		    return true;
	    } else if ( this.punch == 1 ) {
		if (z > 0.5*constants.ecalLength || r > constants.ecalRadius )
		    return true;
	    } else {
		if (z > 0.5*constants.hcalLength || r > constants.hcalRadius )
		    return true;
	    }
	}

	// Hadrons stop in HCal. We hope
	if ( Math.abs(this.type) == constants.HD || Math.abs(this.type) == constants.MET ) {

	    if ( this.oldPoint == null ) {
	    	this.oldPoint = new point(pnt.getX(), pnt.getY(), pnt.getZ());
	    	this.pathLength = 0;
	    }

	    if ( pnt.eta() < 1.8 ) {

	    	if ( r > constants.trkrRadius && r < constants.ecalRadius ) {
	    		this.pathLength += 0.25*pnt.diff(this.oldPoint);   // EB

	    	} else if ( r > constants.ecalRadius ) {
	    		this.pathLength += pnt.diff(this.oldPoint);        // HB
	    	}

	    } else if ( pnt.eta() < 2.1 ) {   // z = 465
	    	if ( z > 465 ) {
	    		this.pathLength += pnt.diff(this.oldPoint);        // HE
	    	}

	    } else if ( pnt.eta() < 3.0 ) {    // z = 410
	    	if ( z > 465 ) {
	    		this.pathLength += pnt.diff(this.oldPoint);        // HE
	    	} else if ( z > 410 ) {
	    		this.pathLength += 0.25*pnt.diff(this.oldPoint);   // EE
	    	}

	    } else if ( pnt.eta() < 5.2 ) {   // 1200 < z < 1425       // HF

	    	if ( z > 1200 ) {
	    		this.pathLength += 0.5*pnt.diff(this.oldPoint);
	    	}

	    }
	    
	    if ( this.pathLength > this.maxPath || z > 0.5*constants.pipeLength ) {
	    	this.oldPoint = null;
	    	this.pathLength = 0;
	    	return true;
	    }

	    if ( this.oldPoint != null )
	    	this.oldPoint.copy(pnt);

	    return false;
	
	}

	// e/gamma stop in ECal.
	if ( Math.abs(this.type) == constants.EM ) {

	    if ( this.oldPoint == null ) {
	    	this.oldPoint = new point(pnt.getX(), pnt.getY(), pnt.getZ());
	    	this.pathLength = 0;
	    }

	    if ( pnt.eta() < 1.8 ) {           // EB
	    	if ( r > constants.trkrRadius ) {
	    		this.pathLength += pnt.diff(this.oldPoint);
	    	}
	    } else if ( pnt.eta() < 2.1 ) {      // HE

	    	if ( z > 465 ) {
	    		this.pathLength += pnt.diff(this.oldPoint);
	    	}

	    } else if ( pnt.eta() < 4 ) {      // EE 
	    	if ( z > 400 )  { 
	    		this.pathLength += pnt.diff(this.oldPoint);
	    	}
	    	
	    } else if ( pnt.eta() < 5 ) {      // HF
		
	    	if ( z > 1200 ) {
	    		this.pathLength += pnt.diff(this.oldPoint);
	    	}
	    }

	    if ( this.pathLength > this.maxPath || z > 0.5*constants.pipeLength ) {
	    	this.oldPoint = null;
	    	this.pathLength = 0;
	    	return true;
	    }

	    if ( this.oldPoint != null )
	    	this.oldPoint.copy(pnt);

	    return false;
		
	}

	// If we got this far, keep going and going and going
	return false;

    } // End pathStop()

    // Code up the (almost freshman) physics for particle motion
    public boolean makeTrack() {

	double t = 0.0;
	double angle = 0.0;
	
	this.geometry = new ArrayList<SimpleVector>();
	
	// The point we'll be calculating
	point pt = new point();

	// Get the cyclotron radius ...
	double r0 = this.r;

	// ... and frequency ( = v_T / r )
	double w0 = this.w;

	int sign = (this.q<0) ? -1 : 1;
	
	/* Get the specifics from the initial 
	 * conditions at t=0 x=y=0 and v = v0
	 */

	// phase for the trig functions ( = atan(px/py) )
	double phase = this.phase;

	// Center of the circular path
	double x0 = +r*Math.cos(phase);
	double y0 = -r*Math.sin(phase);

	// Ratio of the B field inside/outside the cryostat
	double B_Ratio = 2;

	// Amount of time spent in the central field
	double cTime = 0;

	boolean inYoke = false;

	double E = this.E;
	
	int maxTime = getIntSetting("maxTime");
	
	// While this particle is still going (break on pathStop in the loop),
	// but limited to maxTime ns so we don't get an infinite loop  
	while ( t < maxTime ) {

	    if ( this.q != 0 ) {

		// If we've moved into the yoke... the field flips over
		// So we have to reset some of the stuff to calculate
		// the new path correctly
		if (  pt.getR() > constants.cryoRadius && !inYoke ) {

		    // Reset the center of the circular path to the new circle through the yoke
		    x0 -= (1+B_Ratio) * r0 * Math.cos(angle);
		    y0 += (1+B_Ratio) * r0 * Math.sin(angle);

		    // And the radius
		    r0 *= B_Ratio;

		    // and the frequency
		    w0 *= 0.5;

		    // Reset the phase
		    phase = angle;

		    cTime = t;

		    inYoke = true;
		}

		/* Calculate the track of the particle 
		 * as it moves through the detector
		 */
		if ( !inYoke ) {   // If we're inside the cryostat....
		    angle = w0*t + phase;

			// Get the next point on the path
			pt.setX( sign*(-r0*Math.cos( angle ) + x0) );
			pt.setY( sign*(+r0*Math.sin( angle ) + y0) );
			pt.setZ( this.vtx + this.v.getZ()*t );

		} else {           // Otherwise, we've got a muon in the yoke
		    angle = w0 * (t-cTime) - phase;

		    // Get the next point on the path
		    pt.setX( sign*(+r0 * Math.cos( angle ) + x0) );
		    pt.setY( sign*(+r0 * Math.sin( angle ) + y0) );
		    pt.setZ( this.vtx + this.v.getZ()*t );

		}

	    // Nice straight path evolving from x(t) = x0 + vx*t
	    } else {
		// Figure out our new position dt ns later
		pt.setX(this.v.getX()*t); 
		pt.setY(this.v.getY()*t);
		pt.setZ( this.vtx + this.v.getZ()*t );
	    }

	    // The y & z axes are reversed for some reason
	    // flip the new vertices to account for it
	    pt.multiplyY(-1.0);
	    pt.multiplyZ(-1.0);
	    
	    // Add this point to the path
	    this.geometry.add(pt.getVector());

	    // If we've reached the end of the line, break off
	    if ( this.pathStop(pt) )
		break;

	    // Increment time (step size set in the constructor)
	    t += timeStep;
	    
	    // If we're in the tracker, deduct 5 MeV for every time we cross the silicon and bail if we run out of energy
	    if ( this.type != constants.MU ) {

	    	// What it means to be in the tracker
	    	boolean inTracker = Math.abs(pt.getZ()) < 390 && 
	    				pt.getR() > constants.layers[0] && 
	    				pt.getR() < constants.layers[14];
	    				
	    	if ( inTracker ) {
	    		int last_gap = 0, gap = 0;
	    		while ( pt.getR() > constants.layers[gap++] ) {
				}
	    		if ( gap != last_gap ) {
	    			E -= 0.005*Math.abs(gap - last_gap);
	    			if ( E <= 0 )
	    				break;
	    			last_gap = gap;
	    		} // End if ( gap != last_gap )
	    	} // End if ( inTracker )
	    } // End if ( this.type != constants.MU )
	} // End while ( t < maxTime )

	SimpleVector [] vertices = new SimpleVector[this.geometry.size()];
	this.geometry.toArray(vertices);

	this.length = vertices.length;
	this.time = (int)Math.round(t);
	
	this.track = new Polyline( vertices, this.color );
	this.track.setVisible(this.visible);
	this.track.setWidth(this.lineWidth);

	this.dirty = false;
	
	return true;
    } // End makeTrack()

    // When the animator sends an event time....
    // move the sprite into the proper position
    public int setPositionAt( float timeStamp ) {
    	int index = (int)(timeStamp / timeStep);
    	SimpleVector currentPos, newPos;
    
    	if ( index >= this.geometry.size() ) {
    		this.sprite.setVisibility(false);
    		this.sprite.translate(0f,0f,0f);
    		
    		//if ( this.type == constants.HD || this.type == constants.EM ) {
    			if ( this.tower != null ) {
    				this.tower.setVisibility(true);
    				this.glow.setVisibility(true);
    			}
    		//}
    		
    		return 0;
    	} else {	
    		currentPos = new SimpleVector(this.sprite.getTransformedCenter());
    		newPos = new SimpleVector(this.geometry.get(index));
    		newPos.sub(currentPos);
    		
    		this.sprite.translate( newPos );

    	}
    	
    	return 1;
    }
    
    // Make up a minimalist quad for showing the sprite texture
    private Object3D makeSprite(float width) {
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
        obj.setCulling(false);
        obj.setSortOffset(-1250.0f);

        // Add the texture
		obj.setTexture("halo");
		obj.calcTextureWrapSpherical();

		// Unseen for now
		obj.setVisibility(false);
		obj.setAdditionalColor(this.color);
		
		// Compile it ready for the graphics processor
		obj.build();
        
        return obj;
    }

    public List<int[]> makeHitsI() {
    	
    	SimpleVector [] vertices = new SimpleVector[this.geometry.size()];
    	this.geometry.toArray(vertices);

    	int prevHitLayer = -1;
    	double prevZ = -100;
    	double e = (this.e != 0) ? this.e : this.E;
    	
    	List<int[]> hitList = new ArrayList<int[]>();

    	// If this track has an orbital radius < the inner tracker radius, we'll never hit it
    	// Like wise if it's aimed so close to the pipe that it'll miss the tracker, or if it's
    	// a neutral particle (won't interact in the silicon)
    	if ( this.r < constants.layers[0] || this.q == 0 || !this.detectable ||  Math.abs(this.eta)>3.75 )
    		return hitList;
    	
    	for ( int i=1; i<vertices.length-1; i++ ) {
    		
    		double ri = Math.sqrt(vertices[i].x*vertices[i].x + vertices[i].y*vertices[i].y);
    		
    		// If we're not (yet) in the tracker keep searching
    		if ( ri < constants.pipeRadius )
    			continue;
    		
    		// If we've exited the tracker bail
    		if ( ri > constants.trkrRadius )
    			break;
    		
    		if ( Math.abs(vertices[i].z) > 0.4*constants.trkrLength ) {
    			//
    			// Format of the hit vector: 0 => time of hit,
				//                           1 => layer that was hit (indirectly = radius of the hit)
				//                           2 => x of the hit
				//                           3 => y of the hit
    			//
    			int timeStamp = (int)Math.round(this.timeStep * i);
				int pixel1 = (Math.round(63 + 63*vertices[i].x/160));
				int pixel2 = (Math.round(63 - 63*vertices[i].y/160));

				if ( pixel1 >= 0 && pixel1 < 256 && pixel2 >= 0 && pixel2 < 256 ) {

					if ( vertices[i].z > 0 ) {
						// Plant a hit in each of the end-planes along the track    				
						hitList.add( new int[] {timeStamp, 15, pixel1, pixel2} );
						hitList.add( new int[] {timeStamp, 16, pixel1, pixel2} );
    				
					} else {
						hitList.add( new int[] {timeStamp, 17, pixel1, pixel2} );
						hitList.add( new int[] {timeStamp, 18, pixel1, pixel2} );
    				
					}
				} else {
					
					if ( pixel1 < 0 || pixel1 > 255 ) {
						Log.e("makeHitsI()", "pixel1 = "+pixel1+" (x,y,t) = ("+vertices[i].x+", "+vertices[i].y+", "+theta+")");
					}
					if ( pixel2 < 0 || pixel2 > 255 ) {
						Log.e("makeHitsI()", "pixel2 = "+pixel2+" z = "+vertices[i].z);
					}

				}

    			break;
    		}
    		
    		// dE/dx ~ 5 MeV/hit in silicon. We're going no further
    		if ( e <= 0.005 )
    			break;
    		
    		// Get the points before & after this one
    		double rp = Math.sqrt(vertices[i-1].x*vertices[i-1].x + vertices[i-1].y*vertices[i-1].y);
    		double rn = Math.sqrt(vertices[i+1].x*vertices[i+1].x + vertices[i+1].y*vertices[i+1].y);

    		// Scan the path and see if we crossed a layer
    		for ( int j=0; j<constants.layers.length; j++ ) {
    		
    			// If the last hit was in this same layer skip to avoid double counting
    			if ( prevHitLayer == j && Math.abs(vertices[i].z - prevZ) < .05 )
    				continue;
    			
    			// Radial size of this layer
    			double rl = constants.layers[j];
    			
    			// A crossing means that rp and rn are on opposite sides of the layer rl
    			if ( (rn < rl && rp > rl) || (rp < rl && rn > rl) ) {
    				
    				// We crossed a layer. Plant a hit when & where it happened
    				//double xl = ( rl * vertices[i].x / ri ); // cos()
    				//double yl = ( rl * vertices[i].y / ri ); // sin()
    				
    				int timeStamp = (int)Math.round(this.timeStep * i);
    				
					double theta = constants.RAD2DEG*(Math.atan2(vertices[i].y, vertices[i].x));
					double z = vertices[i].z;
					int pixel1 = 0, pixel2 = 0;
					
					// Figure out which quadrant we're in: Since this is an arctan, 
					// Q1 : [0, -90], Q2 : [-90, -180], Q3 : [90, 180], Q4 : [0, 90]
					if ( theta <= 0 && theta > -90 ) {						// Quardrant 1. Pixels [192,255]
						theta = Math.abs(theta);
						pixel1 = (int)Math.round(255 - (theta/90)*63);
					} else if ( theta <= -90 && theta > -180 ) {			// Quadrant 2. Pixels [128, 191]
						theta = 180 + theta;
						pixel1 = (int)Math.round(128 + (theta/90.0)*63);
					} else if ( theta >= 90 && theta < 180 ) {				// Quadrant 3. Pixels [64, 127]
						theta = 180.0 - theta;
						pixel1 = (int)Math.round(127 - (theta/90.0)*63);
					} else if ( theta >= 0 && theta < 90 ) {				// Quardrant 4. Pixels [0, 63]
						pixel1 = (int)Math.round((theta/90.0)*63);
					}

					// Map Z position to texture pixel. Texture is 256 pixels, 127.5 is z = 0, 0 is z = -3.9, and 255 is z = +3.9
					pixel2 = (int)Math.round(127 - 127*z/400.0);
					
    				// Format of the hit vector: 0 => time of hit,
    				//                           1 => layer that was hit (indirectly = radius of the hit)
    				//                           2 => theta of the hit
    				//                           3 => z
    				
					if ( pixel1 >= 0 && pixel1 < 256 && pixel2 >= 0 && pixel2 < 256 ) {
						hitList.add( new int[] {timeStamp, j, pixel1, pixel2} );
    				
						e -= 0.005;
						prevHitLayer = j;
						prevZ = vertices[i].z;
					} else {
						
						if ( pixel1 < 0 || pixel1 > 255 ) {
							Log.e("makeHitsI()", "pixel1 = "+pixel1+" (x,y,t) = ("+vertices[i].x+", "+vertices[i].y+", "+theta+")");
						}
						if ( pixel2 < 0 || pixel2 > 255 ) {
							Log.e("makeHitsI()", "pixel2 = "+pixel2+" z = "+vertices[i].z);
						}

					}
    			
    			
    			} // End if ( (rn < rl && rp > rl) || (rp < rl && rn > rl) )
    			
    		} // End for ( int j=0; j<constants.layers.length; j++ )
    		
    	} // End for ( int i=1; i<vertices.length-1; i++ ) 
    	
    	if ( getIntSetting("debugLevel") > 1 && hitList.size() == 0 ) {
    		Log.w("makeHits()", "Zero hits for track "+id+"("+this.name+")");
    		Log.w("makeHits()", "energy = "+this.e);
    		Log.w("makeHits()", "eta    = "+this.eta);
    		Log.w("makeHits()", "radius = "+this.r);
    	}

    	return hitList;
    } // End makeHits()
    
    class muonHit {
    	
    	public Object3D hit;
    	public int timeStamp;
    	public RGBColor color = new RGBColor( 255,0,0 );
    	
    	muonHit( point p, int timeStamp, boolean visibility, RGBColor color ) {
    		
			// Plant a hit in a random place just off the path
			double min = -5., max = 5.;
			double rx = Math.floor(Math.random()*(max-min+1)+min);
			double ry = Math.floor(Math.random()*(max-min+1)+min);
			double rz = Math.floor(Math.random()*(max-min+1)+min);

			float x = (float)(p.getX() + rx);
			float y = (float)(p.getY() + ry);
			float z = (float)(p.getZ() + rz);

			this.timeStamp = timeStamp;
			this.color = color;
			
			// Make a new sprite to indicate the hit in the chamber
			hit = makeSprite(2);
			hit.translate(x, y, z);
			hit.scale(25);
			
			hit.setVisibility(visibility && settings.getBooleanSetting("response"));
    	}
    	
    	public void setVisibility( boolean visible ) {
    		this.hit.setVisibility(visible);
    	}
    }
    
    public void makeMuonHits() {
    	
    	if ( this.type != constants.MU || this.q == 0 )
    		return;
    	
    	if ( getBooleanSetting("isData") ) {
    		String filter = ".*(standalone).*";
    		if ( !this.name.matches(filter) )
    			return;
    	}
    	
    	muonHits = new ArrayList<muonHit>();

    	chambers chambers = new chambers();
    	int lastX = 0, lastY = 0;
    	
    	// Loop over the geometry vertices and see if any of them are in a chamber
    	for ( int i=0; i<this.geometry.size(); i++ ) {
    		
    		if ( chambers.inChamber(this.geometry.get(i)) ) {
    		
    			point p = new point (this.geometry.get(i).x, this.geometry.get(i).y, this.geometry.get(i).z);
    			if ( Math.abs( Math.round(lastX - p.getX()) ) > 1 && Math.abs( Math.round(lastY - p.getY()) ) > 1 ) {

    				// Record when the hit occurs (in event time)
    				int t = (int)Math.round(this.timeStep * i);

    				// Make sure we don't double count
    				lastX = (int)p.getX();
    				lastY = (int)p.getY();
    				
    				// And add a new hit to the list
    				muonHits.add(new muonHit(p, t, this.visible, this.color));
    				    			}
    		} // End if ( chambers.inChamber(this.geometry.get(i) )
    	} // End for ( int i=0; i<this.geometry.size(); i++ )
    	
    	return;
    }
    
    public Polyline getTrack() {
	if ( this.track != null )
	    return this.track;
	return null;
    }

    public void drawJetCone() {

    	int l         = this.geometry.size()-1;
    	double x      = this.geometry.get(l).x;
    	double y      = -this.geometry.get(l).y;
    	double z      = -this.geometry.get(l).z;
    	double z0     = -(float)this.vtx;
    	float scale  = 20.0f;

    	double r  = Math.sqrt(x*x + y*y + (z-z0)*(z-z0));
    	double height = 0.025*r;
    	double theta = 0;

		if ( z <= 0 )
			theta = Math.acos(y/r);
		else
			theta = -Math.acos(y/r);
		
		// Additional initial rotation flips the
		// negitive Y-axis around X or Z into the 
		// positive Y-axis
		theta += Math.PI;
		
		double phi = 0;
		if ( z < 0 )
			phi = Math.atan2(z,x) - Math.PI;
		else
			phi = Math.atan2(z,x);

		this.jetCone = Primitives.getCone(256, scale, (float)height);
		this.jetCone.setAdditionalColor(new RGBColor(255,255,0));
		this.jetCone.setTransparency(40);
		this.jetCone.setVisibility( this.visible ); 
		this.jetCone.setName("cone");
		this.jetCone.setCulling(false);
		this.jetCone.build();
		
    	// Move the cone down the Y-axis so the apex is at the origin
		this.jetCone.translate(new SimpleVector(0, scale*height, 0));

    	// Make the apex the pivot for rotations
		this.jetCone.setRotationPivot(new SimpleVector(0, -scale*height, 0));
    	
		//this.jetCone.rotateZ((float)Math.PI);
		this.jetCone.rotateZ((float)(theta));
		this.jetCone.rotateY((float)(phi));
    	
		this.hasJetCone = true;
		
    	return;
    	
    }

	public void drawTower(Object3D htower) {
		
		int l    = this.geometry.size() - 1;
    	double x =  this.geometry.get(l).x;
    	double y = this.geometry.get(l).y;
    	double z = this.geometry.get(l).z;
		
		SimpleVector jetAxis = new SimpleVector(x,y,z);

		/**************************************************************/
		this.tower = new Object3D(htower);
		this.tower.build();                                             // Build first
		this.tower.setRotationPivot(new SimpleVector(0f, 0f, 0f));	    // then set the pivot
		this.tower.setVisibility(this.visible && settings.getBooleanSetting("response"));
		/**************************************************************/

		if ( this.tower == null ) {
			Log.e("drawTower()", "Tower is NULL, bailing now!");
			return;
		}
		
		int transparency = (this.e < 25.) ? (int)(100.*this.E/25.) : 100;
		if ( transparency < 4 ) 
			transparency = 2;		
		
		// Get the initial center coordinates of the object
		SimpleVector c = tower.getTransformedCenter();
		tower.setTransparency(transparency);
		
		// Get the rotation matrix of the vector as though it started from (0,0,1) direction
		Matrix rotate = (new SimpleVector(x,y,z)).getRotationMatrix(new SimpleVector(0,-1,0));
		tower.setRotationMatrix(rotate);
		
		SimpleVector za = tower.getZAxis();

		// Rescale, translate to (x,y,z) will move the bottom of the object
		// to the point (x,y,z). We want the top at (x,y,z).
		double r = jetAxis.length() - 2.0*c.length();
		za.scalarMul((float)r);
		tower.translate(za);
		
		// Make it big. If this is done before build, build undoes it. If
		// it's done before getting the z-axis, getZAxis returns basically 0.
		if ( this.type == constants.HD )
			tower.scale(100);
		else if ( this.type == constants.EM )
			tower.scale(25);

		if ( this.tower == null ) {
			Log.w("drawTower()", "Tower is null for particle "+this.id+" of type "+this.type);
			return;
		}
		
		// Use the halo graphic to make the tower all glowy & purty.
		this.glow = makeSprite(2);
		if ( this.glow == null ) {
			Log.w("drawTower()", "makeSprite returned null for particle "+this.id+" of type "+this.type);
			return;
		}
		
		this.glow.translate( tower.getTransformedCenter() );
		this.glow.setTransparency(transparency);
		this.glow.setVisibility(this.visible && settings.getBooleanSetting("response"));
		if ( this.type == constants.HD )
			this.glow.scale(100f);
		else if ( this.type == constants.EM )
			this.glow.scale(25f);

		this.tower.setSortOffset(-1250f);
		this.glow.setSortOffset(-1260f);
		
		
		return;
	}
	
	public Object3D [] getTower() {
		if ( this.tower == null )
			Log.e("drawTower()", "Tower is NULL");
		if ( this.glow == null )
			Log.e("drawTower()", "Glow is NULL");
		return new Object3D [] {this.tower, this.glow};
	}
	
    public Object3D getCone() {
    	return this.jetCone;
    }

    public boolean hasCone() {
		return hasJetCone && (this.jetCone != null);
    }

	// Force visibility of this track
    public void setVisible(boolean canuseeme) {
    	this.visible = canuseeme;

    	this.track.setVisible(this.visible);
	
    	if ( this.jetCone != null )
    		this.jetCone.setVisibility(this.visible);

    	if ( this.tower != null )
    		this.tower.setVisibility(this.visible);

    	if ( this.glow != null )
    		this.glow.setVisibility(this.visible);
    	
    	if ( this.muonHits != null && this.muonHits.size() > 0 ) {
    		for ( int i=0; i<this.muonHits.size(); i++ )
    			this.muonHits.get(i).setVisibility(this.visible);
    	}
    	
    	return;
    }

    public void updateHadronVisible(boolean showHadrons) {
    	if ( this.name.equalsIgnoreCase("hadron") )
    		this.updateVisible();
    		
    	return;
    }
    
    public void updateAxisVisible( boolean showAxis ) {
    	if ( this.name.equalsIgnoreCase("Jet") )
    		this.updateVisible();
    	
    	return;
    }
    
    // Check and see if we ought to be visible
    public void updateVisible() {

    	// Apply the Pt Cut
    	float ptmin = getFloatSetting( "ptcut" );
    	this.visible = this.Pt > ptmin;
		
		// If we're already invisible, bail.
		//if ( !visible ) {
			//this.setVisible(false);
			//return;
		//}
		
		// Check for the type toggles from the "Display" group
		if ( this.type == constants.EM ) {
			if ( this.q == 0 )
				visible = visible && getBooleanSetting("showPhotons");
			else
				visible = visible && getBooleanSetting("showElectrons");
		}
		
		if ( this.type == constants.HD )
			visible = visible && getBooleanSetting("showJetMET");
		
		if ( this.type == constants.TK )
			visible = visible && getBooleanSetting("showTracks");
		
		if ( this.type == constants.MU && this.q != 0 )
			visible = visible && getBooleanSetting("showMuons");
		
		if ( this.type == constants.BK )
			visible = visible && getBooleanSetting("showUnderlying");
		
    	if ( this.type == constants.MET )
    		this.visible = this.visible && getBooleanSetting("showJetMET");

		// If we're already invisible, bail.
		//if ( !visible ){
			//this.setVisible(false);
			//return;
		//}
		
		// Jet filters
		if ( this.name.equalsIgnoreCase("hadron") )
    		this.visible = this.visible && getBooleanSetting("showHadrons");
		
    	// Check the toggles for jet cones & jet axis
    	if ( this.hasCone() )
    		this.jetCone.setVisibility(this.visible && getBooleanSetting("showCone"));
	
    	if ( this.name.equalsIgnoreCase("Jet") ) 
    		this.visible = this.visible && getBooleanSetting("showAxis");

    	// Muon filters
    	if ( getBooleanSetting("isData") && this.type == constants.MU && this.q != 0 ) {
    		
    		if ( getBooleanSetting("showTracker") || getBooleanSetting("showStandAlone") || getBooleanSetting("showGlobal") ) {
    		
    			String filter = "";
    			filter = ".*(";
		
    			filter += getBooleanSetting("showTracker")    ? "tracker|"    : "";
    			filter += getBooleanSetting("showStandAlone") ? "standalone|" : "";
    			filter += getBooleanSetting("showGlobal")     ? "global|"     : "";
    			filter  = filter.substring(0,filter.length()-1) + ").*";

    			if ( !filter.equalsIgnoreCase(".*().*") )
    				this.visible = this.visible && this.name.matches(filter);
    			
    		} else
    			visible = false;
    		
    	}
	
    	// Sim options
    	if ( !this.detectable ) {
    		if ( !getBooleanSetting("showUndetectable") )
    			this.visible = false ;
    	}

    	// If this is part of the underlying event in a simulation
    	if ( this.isUnderlying() )
    		this.visible = this.visible && getBooleanSetting("showUnderlying");
    	
    	// Should calorimeter towers be visible? What about muon chamber hits?
    	if ( this.type == constants.EM || this.type == constants.HD) {
    		if ( this.tower != null )
    			this.tower.setVisibility(this.visible && settings.getBooleanSetting("response"));
    		if ( this.glow != null )
    			this.glow.setVisibility(this.visible && settings.getBooleanSetting("response"));
    		
    	} else if ( this.type == constants.MU && this.muonHits != null && this.muonHits.size() > 0 ) {
    		for ( int i=0; i<this.muonHits.size(); i++ )
    			this.muonHits.get(i).setVisibility(this.visible && settings.getBooleanSetting("response"));
    	}
    	
    	// Tracker? Set in the parent Event class, more efficient that way
    	
    	// Set the visibility from the checks
    	this.track.setVisible(this.visible && settings.getBooleanSetting("paths"));
    		
    	return;
    }

    public boolean getVisible() {
    	return this.visible;
    }

    public boolean isUnderlying() {
    	return this.type == constants.BK && !this.getBooleanSetting("isData");
    }
    
    public float findMinDistance( Camera cam, FrameBuffer buf, SimpleVector x1 ) {
    	
    	if ( cam == null || buf == null || x1 == null ) {
    		Log.w("Track()", "Something's not right here");
    		return -1.0f;
    	}
    	
    	float min_distance = 10000.0f;
    	if ( this.geometry != null ) {

    		float distance = 10000f;
    		for ( int i=0; i<geometry.size(); i++ ) {
    			try {

    				SimpleVector x0 = new SimpleVector(); 
    				Interact2D.project3D2D(cam, buf,geometry.get(i), x0);
    				if ( x0 != null )
    					distance = x0.distance(x1);
    			} catch (Exception e) {
    				Log.e("Track()", e.toString());
    			}
    			
    			if ( distance < min_distance )
    				min_distance = distance;
    		}
    		return min_distance;
    	}
    	return -1.0f;
    }

    public boolean updateGeometry( SimpleVector[] newline ) {
	this.track.update(newline);
	return true;
    }

    public void cleanup() {
    	this.flush();
    	
        color    = null;
        name     = null;
        oldPoint = null;
    	p        = null;
        v        = null;
        track    = null;
        jetCone  = null;
        geometry = null;
        sprite   = null;
        
        return;
    }
    
    public void flush() {
    	p          = new point();
        v          = new point();
        gamma      = 0.0;
        vtx        = 0.0;
        r          = 0.0;
        w          = 0.0;
        phase      = 0.0;
        q          = 0;
        m          = -1.0;
        P          = 0.0;
        Pt         = 0.0;
        E          = 0.0;
        e          = 0.0;
        phi        = 0.0;
        theta      = 0.0;
        eta        = 0.0;
        punch      = 0;
        type       = 2;
        length     = 0;
        time       = 0; 
        timeStep   = 0f;
        oldPoint   = new point();
        pathLength = 0.0;
        colorName  = "black";
        color      = new RGBColor(0xff, 0x00, 0x00);
        name       = "";
        mass       = 0.0;
        dirty      = false;
        active     = false;
        addedToWorld = false;
        detectable = true;
        visible    = true;
        createJetCone = false;
        lifeTime = 5;
        
        SimpleVector [] vertices = {new SimpleVector(0f,0f,0f), new SimpleVector(0.01f, 0.01f, 0.01f)};
        track = new Polyline(vertices, this.color);
        geometry = new ArrayList<SimpleVector>();
        
        tower    = null;
        glow     = null;
        muonHits = null;

        if ( sprite != null ) {
        	sprite.translate(0f,0f,0f);
        	sprite.setVisibility(false);
        }
        return;
    }

    // Examiner function for debugging
    private void dump(String caller) {
    	Log.d("Track "+this.id, "called from "+caller);
    	return;
    }

    // Quick & Dirty class to test if a muon is inside of a muon chamber
    private class chambers {
    	private ArrayList<wheel> wheels, endcaps;
    	private List<chamber> quadrant1,quadrant2,quadrant3,quadrant4;
    	private List<List<chamber>> quadrants;

    	public chambers() {
    		wheels  = new ArrayList<wheel>();
    		endcaps = new ArrayList<wheel>();
    	
    		// Z range of the 5 wheels + 2 endcaps
    		wheels.add( new wheel(  4.1759,  6.7099 ) );      // Wheel +2
    		wheels.add( new wheel(  1.5094,  4.0434 ) );      // Wheel +1
    		wheels.add( new wheel( -1.1954,  1.3386 ) );      // Wheel  0
    		wheels.add( new wheel( -3.9007, -1.3667 ) );      // Wheel -1
    		wheels.add( new wheel( -6.6493, -4.1153 ) );      // Wheel -2

    		// Endcaps
    		endcaps.add( new wheel(  6.8000,  7.2629 ) );
    		endcaps.add( new wheel(  8.1057,  8.9686 ) );
    		endcaps.add( new wheel(  9.8314, 10.6743 ) );
    		endcaps.add( new wheel( 11.1171, 12.0000 ) );
    		
    	    // X-Y bounding boxes in each wheel. Bounding box for a chamber
    	    // is the z-range of the wheel plus the xy range of the chamber.
    	    // This divides the muon detector into quadrants of quintiles 
    	    // (1/20 of the barrel detector) plus 8 endcap regions for 
    	    // collision detection
    	    quadrant1 = new ArrayList<chamber>();
    	    quadrant2 = new ArrayList<chamber>();
    	    quadrant3 = new ArrayList<chamber>();
    	    quadrant4 = new ArrayList<chamber>();

    	    quadrants = new ArrayList<List<chamber>>(5);

    	    // The locations of every muon chamber in CMS, in meters
    	    quadrant1.add( new chamber (0.0778, 8.3426, 4.7771, 8.9426) );
    	    quadrant1.add( new chamber (3.3800, 5.4150, 7.6712, 8.2056) );
    	    quadrant1.add( new chamber (6.8497, 0.9788, 9.6399, 5.2701) );
    	    quadrant1.add( new chamber (8.4577, -3.7774, 9.0573, 0.9226) );
    	    quadrant1.add( new chamber (-9.0423, -3.7774, -8.4427, 0.9226) );
    	    quadrant1.add( new chamber (-1.3724, 6.7426, 2.1276, 7.3426) );
    	    quadrant1.add( new chamber (2.2773, 4.8043, 5.5286, 6.9950) );
    	    quadrant1.add( new chamber (5.2892, 1.5211, 7.4798, 4.7732) );
    	    quadrant1.add( new chamber (6.8571, -2.2274, 7.4577, 1.2726) );
    	    quadrant1.add( new chamber (-2.1126, -7.5574, 1.3874, -6.9574) );
    	    quadrant1.add( new chamber (-7.4427, -1.4874, -6.8421, 2.0126) );
    	    quadrant1.add( new chamber (-1.0218, 5.1426, 1.6775, 5.7426) );
    	    quadrant1.add( new chamber (1.7795, 3.6437, 4.3392, 5.4344) );
    	    quadrant1.add( new chamber (4.0783, 1.1108, 5.8694, 3.6701) );
    	    quadrant1.add( new chamber (5.2575, -1.7774, 5.8571, 0.9226) );
    	    quadrant1.add( new chamber (-1.6624, -5.9574, 1.0368, -5.3574) );
    	    quadrant1.add( new chamber (-5.8421, -1.1374, -5.2425, 1.5626) );
    	    quadrant1.add( new chamber (-1.0423, 3.7426, 1.0173, 4.3426) );
    	    quadrant1.add( new chamber (1.0626, 2.7612, 3.0677, 4.2319) );
    	    quadrant1.add( new chamber (2.8556, 0.9824, 4.3274, 2.9874) );
    	    quadrant1.add( new chamber (3.8571, -1.1174, 4.4577, 0.9426) );
    	    quadrant1.add( new chamber (-1.0022, -4.5574, 1.0573, -3.9574) );
    	    quadrant1.add( new chamber (-4.4427, -1.1574, -3.8421, 0.9026) );

    	    quadrant2.add( new chamber (-4.7620, 8.3426, -0.0628, 8.9426) );
    	    quadrant2.add( new chamber (8.4577, -3.7774, 9.0573, 0.9226) );
    	    quadrant2.add( new chamber (-9.0423, -3.7774, -8.4427, 0.9226) );
    	    quadrant2.add( new chamber (-9.6249, 0.9788, -6.8347, 5.2701) );
    	    quadrant2.add( new chamber (-7.6561, 5.4150, -3.3649, 8.2056) );
    	    quadrant2.add( new chamber (-1.3724, 6.7426, 2.1276, 7.3426) );
    	    quadrant2.add( new chamber (6.8571, -2.2274, 7.4577, 1.2726) );
    	    quadrant2.add( new chamber (-2.1126, -7.5574, 1.3874, -6.9574) );
    	    quadrant2.add( new chamber (-7.4427, -1.4874, -6.8421, 2.0126) );
    	    quadrant2.add( new chamber (-7.0956, 2.1619, -4.9040, 5.4141) );
    	    quadrant2.add( new chamber (-4.8730, 5.1743, -1.6208, 7.3650) );
    	    quadrant2.add( new chamber (-1.0218, 5.1426, 1.6775, 5.7426) );
    	    quadrant2.add( new chamber (5.2575, -1.7774, 5.8571, 0.9226) );
    	    quadrant2.add( new chamber (-1.6624, -5.9574, 1.0368, -5.3574) );
    	    quadrant2.add( new chamber (-5.8421, -1.1374, -5.2425, 1.5626) );
    	    quadrant2.add( new chamber (-5.5341, 1.6650, -3.7429, 4.2244) );
    	    quadrant2.add( new chamber (-3.7695, 3.9637, -1.2106, 5.7544) );
    	    quadrant2.add( new chamber (-1.0423, 3.7426, 1.0173, 4.3426) );
    	    quadrant2.add( new chamber (3.8571, -1.1174, 4.4577, 0.9426) );
    	    quadrant2.add( new chamber (-1.0022, -4.5574, 1.0573, -3.9574) );
    	    quadrant2.add( new chamber (-4.4427, -1.1574, -3.8421, 0.9026) );
    	    quadrant2.add( new chamber (-4.3319, 0.9477, -2.8610, 2.9528) );
    	    quadrant2.add( new chamber (-3.0877, 2.7412, -1.0826, 4.2119) );

    	    quadrant3.add( new chamber (8.4577, -3.7774, 9.0573, 0.9226) );
    	    quadrant3.add( new chamber (-3.1624, -9.1574, -0.0628, -8.5574) );
    	    quadrant3.add( new chamber (-5.4900, -8.4589, -3.3787, -6.8714) );
    	    quadrant3.add( new chamber (-8.2572, -7.0512, -5.9641, -3.4599) );
    	    quadrant3.add( new chamber (-9.0423, -3.7774, -8.4427, 0.9226) );
    	    quadrant3.add( new chamber (-1.3724, 6.7426, 2.1276, 7.3426) );
    	    quadrant3.add( new chamber (6.8571, -2.2274, 7.4577, 1.2726) );
    	    quadrant3.add( new chamber (-2.1126, -7.5574, 1.3874, -6.9574) );
    	    quadrant3.add( new chamber (-5.5136, -7.2099, -2.2623, -5.0192) );
    	    quadrant3.add( new chamber (-7.4647, -4.9881, -5.2741, -1.7359) );
    	    quadrant3.add( new chamber (-7.4427, -1.4874, -6.8421, 2.0126) );
    	    quadrant3.add( new chamber (-1.0218, 5.1426, 1.6775, 5.7426) );
    	    quadrant3.add( new chamber (5.2575, -1.7774, 5.8571, 0.9226) );
    	    quadrant3.add( new chamber (-1.6624, -5.9574, 1.0368, -5.3574) );
    	    quadrant3.add( new chamber (-4.3241, -5.6492, -1.7644, -3.8585) );
    	    quadrant3.add( new chamber (-5.8544, -3.8850, -4.0632, -1.3256) );
    	    quadrant3.add( new chamber (-5.8421, -1.1374, -5.2425, 1.5626) );
    	    quadrant3.add( new chamber (-1.0423, 3.7426, 1.0173, 4.3426) );
    	    quadrant3.add( new chamber (3.8571, -1.1174, 4.4577, 0.9426) );
    	    quadrant3.add( new chamber (-1.0022, -4.5574, 1.0573, -3.9574) );
    	    quadrant3.add( new chamber (-3.0526, -4.4468, -1.0475, -2.9761) );
    	    quadrant3.add( new chamber (-4.3123, -3.2023, -2.8406, -1.1972) );
    	    quadrant3.add( new chamber (-4.4427, -1.1574, -3.8421, 0.9026) );

    	    quadrant4.add( new chamber (8.4577, -3.7774, 9.0573, 0.9226) );
    	    quadrant4.add( new chamber (5.9889, -7.0312, 8.2820, -3.4399) );
    	    quadrant4.add( new chamber (3.3937, -8.4589, 5.5050, -6.8714) );
    	    quadrant4.add( new chamber (0.0778, -9.1574, 3.1775, -8.5574) );
    	    quadrant4.add( new chamber (-9.0423, -3.7774, -8.4427, 0.9226) );
    	    quadrant4.add( new chamber (-1.3724, 6.7426, 2.1276, 7.3426) );
    	    quadrant4.add( new chamber (6.8571, -2.2274, 7.4577, 1.2726) );
    	    quadrant4.add( new chamber (4.9191, -5.6290, 7.1106, -2.3767) );
    	    quadrant4.add( new chamber (1.6358, -7.5799, 4.8880, -5.3892) );
    	    quadrant4.add( new chamber (-2.1126, -7.5574, 1.3874, -6.9574) );
    	    quadrant4.add( new chamber (-7.4427, -1.4874, -6.8421, 2.0126) );
    	    quadrant4.add( new chamber (-1.0218, 5.1426, 1.6775, 5.7426) );
    	    quadrant4.add( new chamber (5.2575, -1.7774, 5.8571, 0.9226) );
    	    quadrant4.add( new chamber (3.7579, -4.4393, 5.5491, -1.8799) );
    	    quadrant4.add( new chamber (1.2257, -5.9692, 3.7846, -4.1785) );
    	    quadrant4.add( new chamber (-1.6624, -5.9574, 1.0368, -5.3574) );
    	    quadrant4.add( new chamber (-5.8421, -1.1374, -5.2425, 1.5626) );
    	    quadrant4.add( new chamber (-1.0423, 3.7426, 1.0173, 4.3426) );
    	    quadrant4.add( new chamber (3.8571, -1.1174, 4.4577, 0.9426) );
    	    quadrant4.add( new chamber (2.8761, -3.1676, 4.3470, -1.1626) );
    	    quadrant4.add( new chamber (1.0977, -4.4268, 3.1028, -2.9561) );
    	    quadrant4.add( new chamber (-1.0022, -4.5574, 1.0573, -3.9574) );
    	    quadrant4.add( new chamber (-4.4427, -1.1574, -3.8421, 0.9026) );

    	    quadrants.add(0, null );
    	    quadrants.add(1, quadrant1 );
    	    quadrants.add(2, quadrant2 );
    	    quadrants.add(3, quadrant3 );
    	    quadrants.add(4, quadrant4 );

    		return;
    }

    	public boolean inChamber(SimpleVector location) {
    		
    		boolean inWheel = false;
    		for ( int i=0; i<wheels.size(); i++ ) {
    			if ( location.z >= wheels.get(i).minZ && location.z <= wheels.get(i).maxZ ) {
    				inWheel = true;
    				break;
    			}
    		}
    		
    		if ( inWheel ) {
    			int quadrant = -1;
    			if  ( location.x >= 0 && location.y >= 0 )
    				quadrant = 1;
    		    else if ( location.x < 0 && location.y > 0 )
    		    	quadrant = 2;
    		    else if ( location.x < 0 && location.y < 0 )
    		    	quadrant = 3;
    		    else if ( location.x > 0 && location.y < 0 )
    		    	quadrant = 4;

    			// No idea how this could have happened. It's got to be somewhere right?
    			if ( quadrant < 1 || quadrant > 4 )
    				return false;
    			
    		    // If we got a valid quadrant from the location, see if it's inside a chamber
    			for ( int i=0; i<quadrants.get(quadrant).size(); i++ ) {
    				
    				if ( quadrants.get(quadrant).get(i).checkHit(location.x,  location.y) )
    					return true;
    				
    		    } // End for ( var i=0; i<quadrants[quadrant].length; i++ )
    		    
    		// Not in a wheel? What about the endcaps?
    		} else {
    			
    		    for ( int i=0; i<endcaps.size(); i++ ) {
    		    	if ( endcaps.get(i).checkHit(location.z) ) 
    				    return true;
    		    }
    		}
    		
    		return false;
    	}

    	class chamber {

    		double minX, minY, maxX, maxY;
    		int hits = 0;

    		public chamber( double minX, double minY, double maxX, double maxY ) {
    			this.minX = 100.*minX;
    			this.minY = 100.*minY;
    			this.maxX = 100.*maxX;
    			this.maxY = 100.*maxY;
    			return;
    		}
    		
    		public boolean checkHit( double x, double y ) {
    			if ( x >= minX && x <= maxX && y >= minY && y <= maxY ) {
    				hits++;
					return hits <= 3;
    			}
    			return false;
    		}
    	}

    	class wheel {
    		double minZ, maxZ;
    		int hits = 0;
    		public wheel( double minZ, double maxZ ) {
    			this.minZ = 100.*minZ;
    			this.maxZ = 100.*maxZ;
    			return;
    		}
    		
    		public boolean checkHit( double z ) {
    			if ( Math.abs(z) >= minZ && Math.abs(z) <= maxZ ) {
    				hits++;
					return hits <= 3;
    			}
    			return false;
    		}

    	}
    }
    
} // End class particle