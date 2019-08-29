package ch.cern.cms.data_browser;

public class constants {
	
    // Type enumerations 
	static public final int TRKR = 0;
	static public final int ECAL = 1;
	static public final int HCAL = 2;
	static public final int CRYO = 3;
	static public final int YOKE = 4;
	static public final int CAPS = 5;
	static public final int AXES = 6;
	static public final int HITS = 7;
	static public final int ALL  = 8;
	static public final int MAX_TRANSPARENCY = 120;

	static public final int GS_POS = 0;
	static public final int TK_POS = 1;
	static public final int SM_POS = 2;
	static public final int EX_POS = 3;

    static public final int BK   = -1;
    static public final int EM   = 0;
    static public final int HD   = 1;
    static public final int MU   = 2;
    static public final int MET  = 3;
    static public final int TK   = 4;

	// CMS Detector sizes in centimeters
	static public final float trkrRadius = 160f;
	static public final float trkrLength = 990f;

	static public final float ecalLength = 1005f;
	static public final float ecalRadius = 180f;

	static public final float hcalRadius = 280f;
	static public final float hcalLength = 990f;

	static public final float cryoLength = 995f;
	static public final float cryoRadius = 370f;

	static public final float yokeLength = 990f;
	static public final float yokeRadius = 910f;

	static public final float pipeRadius = 10f;
	static public final float pipeLength = 3300f;

	// Fragment IDs
	static public final long TRACK    = 0x100;
	static public final long GRAPHICS = 0x200;
	static public final long OPENGL   = 0x300;
	static public final long PREFS    = 0x400;
	static public final long EXPLORE  = 0x500;
	static public final long SIM      = 0x600;

	static public final double RAD2DEG = 180/Math.PI;
	
	
	static public final float [] layers = new float [] {
		20.7f, 23.7f, 27.3f, 31.4f, 36.1f, 41.5f, 47.8f, 54.8f, 63.0f, 72.5f, 83.3f, 95.7f, 110.0f, 126.7f, 145.3f
	};
	
	// Serialized files that make up the CMS detector model
	static public final String [] modelFiles = 
		{ "trkr.ser", "ecal.ser", "hcal.ser", "cryo.ser", "yoke.ser", "caps.ser", "axes.ser", "hits.ser" };

	// Enumerations (from above) which correspond to the model files/subsystems
	static public final int [] modelIDs = 
		{ TRKR, ECAL, HCAL, CRYO, YOKE, CAPS, AXES, HITS };

	// Shared preferences file name
	static public final String SHARED_SETTINGS_NAME =
			"CMS_DATA_BROWSER";
	
	static public final String EXT_PATH_NAME = 
			"CMSDataBrowser";
}
