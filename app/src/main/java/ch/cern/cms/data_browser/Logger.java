package ch.cern.cms.data_browser;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import android.os.Environment;
import android.util.Log;

public class Logger {

	private static Logger instance;
	private static File logFile;
	private static FileWriter buffer;
	
	private boolean localDebugger = false;
	private static String TAG = "Logger";
	
	// Enumerations
	private static final int DEBUG   = 0;
	private static final int VERBOSE = 1;
	private static final int INFO    = 2;
	private static final int WARN    = 3;
	private static final int ERR     = 4;

	// Switch for when we put something into the log file
	private static int LOGLEVEL = WARN;
	
	public Logger() {
		
		if ( !localDebugger ) {
			String fileName =
					Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/cmsdb.log.txt";

			// Open a new log file
			logFile = new File(fileName);
		
			// Hook it to a buffered writer
			try {
				buffer = new FileWriter(logFile);  // Open in write mode, clears anything in the file
			} catch (Exception e) {
				Log.e(TAG, "Failed to open buffer "+e.toString());

				// If we can't open a new logfile, don't keep trying to write to it
				localDebugger = true;
			}
		} // end if ( !localDebugger )
		return;
	}
	
	public void cleanup() {
		if ( !localDebugger ) {
			try {
				buffer.flush();
				buffer.close();
				buffer = null;
			} catch (Exception e) {
				Log.e(TAG, e.toString());
			}
		} // end if ( !localDebugger )
		return;
	}
	
	public static synchronized Logger getInstance() {
		if ( instance == null )
			instance = new Logger();
		
		if ( buffer == null && !instance.localDebugger ) {
			try {
				buffer = new FileWriter(logFile, true); // Reopen the log in append mode
			} catch (IOException f ) {
				Log.e(TAG, f.toString());
			}
		}
		
		return instance;
	}

	// Debug messages
	public void d(String TAG, String message) {
		if ( localDebugger )
			Log.d(TAG, message);
		else if ( LOGLEVEL <= DEBUG ){
			try {
				if ( buffer != null ) {
					buffer.write(TAG+": "+message+"\n");
					buffer.flush();
				}
			} catch (Exception e) {
				Log.e("DEBUG: "+TAG, e.toString());
			}
		}
		return;
	}
	
	// Verbose messages
	public void v(String TAG, String message) {
		if ( localDebugger )
			Log.v(TAG, message);
		else if ( LOGLEVEL <= VERBOSE ){
			try {
				if ( buffer != null ) {
					buffer.write(TAG+": "+message+"\n");
					buffer.flush();
				}
			} catch (Exception e) {
				Log.e("VERBOSE: "+TAG, e.toString());
			}
		}
		return;
	}
	
	// Info messages
	public void i(String TAG, String message) {
		if ( localDebugger )
			Log.i(TAG, message);
		else if ( LOGLEVEL <= INFO ){
			try {
				if ( buffer != null ) {
					buffer.write(TAG+": "+message+"\n");
					buffer.flush();
				}
			} catch (Exception e) {
				Log.e("INFO: "+TAG, e.toString());
			}
		}
		return;
	}
	
	// Warning messages
	public void w(String TAG, String message) {
		if ( localDebugger )
			Log.w(TAG, message);
		else if ( LOGLEVEL <= WARN ){
			try {
				if ( buffer != null ) {
					buffer.write(TAG+": "+message+"\n");
					buffer.flush();
				}
			} catch (Exception e) {
				Log.e("WARNING: "+TAG, e.toString());
			}
		}
		return;
	}
	
	// Error messages
	public void e(String TAG, String message) {
		if ( localDebugger )
			Log.e(TAG, message);
		else if ( LOGLEVEL <= ERR ){
			try {
				if ( buffer != null ) {
					buffer.write("ERROR: "+TAG+": "+message+"\n");
					buffer.flush();
				}
			} catch (Exception e) {
				Log.e(TAG, e.toString());
			}
		}
		return;
	}

} // End class Logger
