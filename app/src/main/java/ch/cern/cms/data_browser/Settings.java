package ch.cern.cms.data_browser;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

public class Settings {

	private static Settings instance;
	
	private Bundle settings = new Bundle();
	private Context context = null;
	private Logger Log;

    // Definitions, names, and default values of everything in the settings bundle
    String[] intKeys = {"zoomSpeed", "debugLevel", "maxTime", "transAll", "transTrkr", "transEcal",
            "transHcal", "transCryo", "transYoke", "transCaps", "transAxes", "maxEvents"}; //"movieTime" = 25, "frameRate" = 10
    int[] intDefs = {20, 0, 50, 127, 127, 127, 127, 127, 127, 127, 127, 25};

    String[] floatKeys = {"rotateSpeed", "lineWidth", "trackRes", "ptcut"};
    float[] floatDefs = {-0.004f, 2.5f, 1.0f, 0.0f};

    String[] boolKeys = {"showUndetectable", "showUnderlying", "showCone", "showAxis", "showHadrons", "showMuons",
            "showTracker", "showStandAlone", "showGlobal", "showElectrons", "showPhotons", "showJetMET", "showTracks",
            "allState", "trkrState", "ecalState", "hcalState", "cryoState", "yokeState", "capsState", "axesState",
            "response", "paths"};
    boolean[] boolDefs = {false, true, false, false, true, true, true, true, true, true,
            true, true, true, true, true, true, true, true, true, true, true, false, true};

    String[] longKeys = {};
    long[] longDefs = {};

    String[] stringKeys = {"server"};
    String[] stringDefs = {"none"};
	
	private Settings(Context context) {
		this.context = context;		
        SharedPreferences savedPrefs = this.context.getSharedPreferences(constants.SHARED_SETTINGS_NAME, 0);
        settings = copyBundle(savedPrefs);

		return;
	}
	
	private Settings() {
		Log = Logger.getInstance();
	}

	public void setContext(Context context) {
		this.context = context;
        SharedPreferences savedPrefs = this.context.getSharedPreferences(constants.SHARED_SETTINGS_NAME, 0);
        settings = copyBundle(savedPrefs);
        return;
	}
	
	public Context getContext() {return this.context;}
	
	public static synchronized Settings getInstance() {
		if ( instance == null )
			instance = new Settings();
		return instance;
	}
	
	public Bundle copyBundle( Bundle from ) {
		
		Bundle to = new Bundle();
		int debugLevel = settings.getInt("debugLevel");
		
		for ( int i=0; i<longKeys.length; i++ ) {
			if ( from.containsKey(longKeys[i]) ) {
				to.putLong(longKeys[i], from.getLong(longKeys[i]));
				if ( debugLevel > 1 )
					Log.w("copyBundle()", "Saved "+longKeys[i]+" = "+from.getLong(longKeys[i]));
			} else {
				to.putLong(longKeys[i], longDefs[i]);
				Log.w("copyBundle()", "Couldn't find "+longKeys[i]+" in bundle. Using default");
			}
		}

		for ( int i=0; i<intKeys.length; i++ ) {
			if ( from.containsKey(intKeys[i]) ) {
				to.putInt(intKeys[i], from.getInt(intKeys[i]));
				if ( debugLevel > 1 )
					Log.w("copyBundle()", "Saved "+intKeys[i]+" = "+from.getInt(intKeys[i]));
			} else {
				to.putInt(intKeys[i], intDefs[i]);
				Log.w("copyBundle()", "Couldn't find "+intKeys[i]+" in bundle. Using default");
			}
		}
		
		for ( int i=0; i<floatKeys.length; i++ ) {
			if ( from.containsKey(floatKeys[i]) ) {
				to.putFloat(floatKeys[i], from.getFloat(floatKeys[i]));
				if ( debugLevel > 1 )
					Log.w("copyBundle()", "Saved "+floatKeys[i]+" = "+from.getFloat(floatKeys[i]));
			} else {
				to.putFloat(floatKeys[i], floatDefs[i]);
				Log.w("copyBundle()", "Couldn't find "+floatKeys[i]+" in bundle. Using default");
			}
		}
		
		for ( int i=0; i<boolKeys.length; i++ ) {
			if ( from.containsKey(boolKeys[i]) ) {
				to.putBoolean(boolKeys[i], from.getBoolean(boolKeys[i]));
				if ( debugLevel > 1 )
					Log.w("copyBundle()", "Saved "+boolKeys[i]+" = "+from.getBoolean(boolKeys[i]));
			} else {
				to.putBoolean(boolKeys[i], boolDefs[i]);
				Log.w("copyBundle()", "Couldn't find "+boolKeys[i]+" in bundle. Using default");
			}
		}

		
		return to;
	}
	
	public Bundle copyBundle( SharedPreferences from ) {
		
		Bundle to = new Bundle();
		int debugLevel = from.getInt("debugLevel", 0);

		for ( int i=0; i<longKeys.length; i++ ) {
			if ( from.contains(longKeys[i]) ) {
				to.putLong(longKeys[i], from.getLong(longKeys[i], longDefs[i]));
				if ( debugLevel > 1 )
					Log.w("copyBundle()", "Copied long "+longKeys[i]+" = "+from.getLong(longKeys[i], longDefs[i]));
			} else {
				to.putLong(longKeys[i], longDefs[i]);
				Log.w("copyBundle()", "Couldn't find long "+longKeys[i]+" in bundle. Using default");
			}
		}

		for ( int i=0; i<intKeys.length; i++ ) {
			if ( from.contains(intKeys[i]) ) {
				to.putInt(intKeys[i], from.getInt(intKeys[i], intDefs[i]));
				if ( debugLevel > 1 )
					Log.w("copyBundle()", "Copied int "+intKeys[i]+" = "+from.getInt(intKeys[i], intDefs[i]));
			} else {
				to.putInt(intKeys[i], intDefs[i]);
				Log.w("copyBundle()", "Couldn't find int "+intKeys[i]+" in bundle. Using default");
			}
		}

		for ( int i=0; i<floatKeys.length; i++ ) {
			if ( from.contains(floatKeys[i]) ) {
				to.putFloat(floatKeys[i], from.getFloat(floatKeys[i], floatDefs[i]));
				if ( debugLevel > 1 )
					Log.w("copyBundle()", "Copied float "+floatKeys[i]+" = "+from.getFloat(floatKeys[i], floatDefs[i]));
			} else {
				to.putFloat(floatKeys[i], floatDefs[i]);
				Log.w("copyBundle()", "Couldn't find float "+floatKeys[i]+" in bundle. Using default");
			}
		}
		
		for ( int i=0; i<boolKeys.length; i++ ) {
			if ( from.contains(boolKeys[i]) ) {
				to.putBoolean(boolKeys[i], from.getBoolean(boolKeys[i], boolDefs[i]));
				if ( debugLevel > 1 )
					Log.w("copyBundle()", "Copied bool "+boolKeys[i]+" = "+from.getBoolean(boolKeys[i], boolDefs[i]));
			} else {
				to.putBoolean(boolKeys[i], boolDefs[i]);
				Log.w("copyBundle()", "Couldn't find bool "+boolKeys[i]+" in bundle. Using default");
			}
		}

		return to;
	}

	public boolean saveSettings() {
		SharedPreferences savedPrefs = this.context.getSharedPreferences(constants.SHARED_SETTINGS_NAME, 0);
		SharedPreferences.Editor editor = savedPrefs.edit();

		int debugLevel = settings.getInt("debugLevel");
		
		for ( int i=0; i<longKeys.length; i++ ) {
			if ( settings.containsKey(longKeys[i]) ) {
				editor.putLong(longKeys[i], settings.getLong(longKeys[i]));
				if ( debugLevel > 1 )
					Log.w("saveSettings()", "Saved "+longKeys[i]+" = "+settings.getLong(longKeys[i]));
			} else {
				editor.putLong(longKeys[i], longDefs[i]);
				Log.w("saveSettings()", "Couldn't find "+longKeys[i]+" in prefsUpdate bundle. Saving default");
			}
		}

		for ( int i=0; i<intKeys.length; i++ ) {
			if ( settings.containsKey(intKeys[i]) ) {
				editor.putInt(intKeys[i], settings.getInt(intKeys[i]));
				if ( debugLevel > 1 )
					Log.w("saveSettings()", "Saved "+intKeys[i]+" = "+settings.getInt(intKeys[i]));
			} else {
				editor.putInt(intKeys[i], intDefs[i]);
				Log.w("saveSettings()", "Couldn't find "+intKeys[i]+" in settings bundle. Saving default");
			}
		}
		
		for ( int i=0; i<floatKeys.length; i++ ) {
			if ( settings.containsKey(floatKeys[i]) ) {
				editor.putFloat(floatKeys[i], settings.getFloat(floatKeys[i]));
				if ( debugLevel > 1 )
					Log.w("saveSettings()", "Saved "+floatKeys[i]+" = "+settings.getFloat(floatKeys[i]));
			} else {
				editor.putFloat(floatKeys[i], floatDefs[i]);
				Log.w("saveSettings()", "Couldn't find "+floatKeys[i]+" in settings bundle. Saving default");
			}
		}
		
		for ( int i=0; i<boolKeys.length; i++ ) {
			if ( settings.containsKey(boolKeys[i]) ) {
				editor.putBoolean(boolKeys[i], settings.getBoolean(boolKeys[i]));
				if ( debugLevel > 1 )
					Log.w("saveSettings()", "Saved "+boolKeys[i]+" = "+settings.getBoolean(boolKeys[i]));
			} else {
				editor.putBoolean(boolKeys[i], boolDefs[i]);
				Log.w("saveSettings()", "Couldn't find "+boolKeys[i]+" in settings bundle");
			}
		}

		return editor.commit();
		
	}
	
	public Bundle getBundle() {
		return this.settings;
	}
	
	public void putBundle( Bundle newSettings ) {
		settings = copyBundle( newSettings );
		return;
	}

	// Getters
	public boolean getBooleanSetting( String key ) {
		if ( settings.containsKey(key) )
			return settings.getBoolean(key);
		
		boolean value = false;
		for ( int i=0; i<boolKeys.length; i++ ) {
			if ( boolKeys[i].equalsIgnoreCase(key) ) {
				value = boolDefs[i];
				break;
			}
		}
		return value;
	}
	public int getIntSetting( String key ) {
		if ( settings.containsKey(key) )
			return settings.getInt(key);
		
		int value = -1000000;
		for ( int i=0; i<intKeys.length; i++ ) {
			if ( intKeys[i].equalsIgnoreCase(key) ) {
				value = intDefs[i];
				break;
			}
		}
		
		return value;
	}
	public long getLongSetting( String key ) {
		if ( settings.containsKey(key) )
			return settings.getLong(key);
		
		long value = -1000000L;
		for ( int i=0; i<longKeys.length; i++ ) {
			if ( longKeys[i].equalsIgnoreCase(key) ) {
				value = longDefs[i];
				break;
			}
		}

		return value;
	}
	public float getFloatSetting( String key ) {
		if ( settings.containsKey(key) )
			return settings.getFloat(key);
		
		float value = -1000000f;
		for ( int i=0; i<floatKeys.length; i++ ) {
			if ( floatKeys[i].equalsIgnoreCase(key) ) {
				//Log.w("getFloat()", "Geting "+value+" from "+key);
				value = floatDefs[i];
				break;
			}
		}

		return value;
	}
	public String getStringSetting( String key ) {
		if ( settings.containsKey(key) )
			return settings.getString(key);
		
		String value = "none";
		for ( int i=0; i<stringKeys.length; i++ ) {
			if ( stringKeys[i].equalsIgnoreCase(key) ) {
				value = stringDefs[i];
				break;
			}
		}
		return value;
	}
	
	// And setters
	public boolean putBoolean( String key, boolean value ) {
		if ( settings.containsKey(key) ) {
			settings.putBoolean(key, value);
			return true;
		}
		settings.putBoolean(key, value);
		if ( settings.getInt("debugLevel") > 1)
			Log.w("settings.putBoolean()", "Created key "+key+" = "+value);
		return false;
	}
	public boolean putInt( String key, int value ) {
		if ( settings.containsKey(key) ) {
			settings.putInt(key, value);
			return true;
		}

		settings.putInt(key, value);
		if ( settings.getInt("debugLevel") > 1)
			Log.w("settings.putInt()", "Created new key "+key+" = "+value);
		return false;
	}	
	public boolean putLong( String key, long value ) {
		if ( settings.containsKey(key) ) {
			settings.putLong(key, value);
			return true;
		}
		settings.putLong(key, value);
		if ( settings.getInt("debugLevel") > 1)
			Log.w("settings.putLong()", "Created key "+key+" = "+value);
		return false;
	}
	public boolean putFloat( String key, float value ) {
		//Log.w("putFloat()", "Putting "+value+" into "+key);
		if ( settings.containsKey(key) ) {
			settings.putFloat(key, value);
			return true;
		}
		settings.putFloat(key, value);
		if ( settings.getInt("debugLevel") > 1)
			Log.w("settings.putFloat()", "Created key "+key+" = "+value);
		return false;
	}
	public boolean putString( String key, String value ) {
		if ( settings.containsKey(key) ) {
			settings.putString(key, value);
			return true;
		}
		
		settings.putString(key, value);
		if ( settings.getInt("debugLevel") > 1 )
			Log.w("settings.putString()", "Created new key "+key+" = "+value);
		return false;
	}
	
} // End class Settings
