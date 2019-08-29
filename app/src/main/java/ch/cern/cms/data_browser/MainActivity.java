package ch.cern.cms.data_browser;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.XmlResourceParser;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ShareActionProvider;
import android.widget.Toast;

public class MainActivity extends Activity {

    private ExploreFragment  exFragment   = null;
    private SimulateFragment simFragment  = null;
    private OpenGLFragment   glFragment   = null;
    private expListFragment  expFragment  = null;
    private TrackFragment    trkFragment  = null;
    
    private final ConnectionMonitorReceiver monitor = new ConnectionMonitorReceiver();
    private boolean hasNetwork = false;
    @SuppressWarnings("unused")
	private boolean isWiFi     = false;
	private boolean netCheckInProgress = false;
	
    private Settings settings = null;
    private ShareActionProvider mShareActionProvider;
    
    private Logger Log;
    
    @Override
	protected void onCreate(Bundle savedInstanceState) {

    	// Start the logger
        Log = Logger.getInstance();
        
    	super.onCreate(savedInstanceState);
    	
    	try {
    		setContentView(R.layout.activity_main);
    	} catch (Exception e) {
    		Log.w("setContentView()",e.toString());
			String message = "Unable to set initial view. Bailing out now";
			Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    		System.exit(1);
    	}
		getActionBar().setDisplayHomeAsUpEnabled(true);   // Toggle the "Up" caret next to the app icon
		getActionBar().setDisplayShowHomeEnabled(true);	  // For a navigation drawer, toggles the three-bar icon
		
        // Get a new instance of the settings and push the current context into it
        // so that the settings class can read the savedPrefs bundle
        settings = Settings.getInstance();
        settings.setContext(this);
        
        // Register the network monitor to receive broadcasts about the connection state
        monitor.register(this);
        
        if ( savedInstanceState != null ) {
        	if ( savedInstanceState.containsKey("isData") )
        		settings.putBoolean("isData", savedInstanceState.getBoolean("isData"));
        	else
        		settings.putBoolean("isData", false);
        	
        	if ( savedInstanceState.containsKey("isSim") )
        		settings.putBoolean("isSim", savedInstanceState.getBoolean("isSim"));
        	else
        		settings.putBoolean("isSim", false);
        }
        
    	FragmentTransaction transaction = getFragmentManager().beginTransaction();

    	glFragment = new OpenGLFragment();
		transaction.replace(R.id.opengl_container, glFragment);
    	//glFragment  = (OpenGLFragment)getFragmentManager().findFragmentById(R.id.opengl_fragment);
		exFragment  = (ExploreFragment)getFragmentManager().findFragmentById(R.id.explore_fragment);
        simFragment = (SimulateFragment)getFragmentManager().findFragmentById(R.id.simulate_fragment);

        expFragment = (expListFragment)getFragmentManager().findFragmentById(R.id.menu_fragment);
        trkFragment = (TrackFragment)getFragmentManager().findFragmentById(R.id.track_fragment);

        transaction.hide(simFragment);
        transaction.hide(exFragment);
        transaction.hide(expFragment);
        transaction.hide(trkFragment);
        
        transaction.commit();
        
        return;
    }
    
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {

		if ( settings.getIntSetting("debugLevel") > 0 )
			Log.w("MainActivity()", "Saving instance");

		savedInstanceState.putBoolean("saved", true);
		if ( exFragment != null )
			savedInstanceState.putBoolean("dataUp", exFragment.isVisible());
		if ( simFragment != null )
			savedInstanceState.putBoolean("simUp", simFragment.isVisible());
		if ( expFragment != null )
			savedInstanceState.putBoolean("menuVisible", expFragment.isVisible());
		if ( trkFragment != null )
			savedInstanceState.putBoolean("editVisible", trkFragment.isVisible());

		savedInstanceState.putBoolean("isData", settings.getBooleanSetting("isData"));
		savedInstanceState.putBoolean("isSim", settings.getBooleanSetting("isSim"));

		// Flush user settings to storage
		settings.saveSettings();
		
		super.onSaveInstanceState(savedInstanceState);
		return;
	}
	
	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		
		super.onRestoreInstanceState(savedInstanceState);

		if ( savedInstanceState != null ) {
			boolean wasShowing = savedInstanceState.getBoolean("dataUp");
			if ( wasShowing && exFragment != null ) {
				try {
					FragmentTransaction transaction = getFragmentManager().beginTransaction();
					transaction.show(exFragment).commit();
				} catch (Exception e) {
					Log.e("onRestoreInstanceState()", "Caught exception "+e.toString());
				}
			} else {
				wasShowing = savedInstanceState.getBoolean("simUp");
				if ( wasShowing && simFragment != null ) {
					try {
						FragmentTransaction transaction = getFragmentManager().beginTransaction();
						transaction.show(simFragment).commit();
					} catch (Exception e) {
						Log.e("onRestoreInstanceState()", "Caught exception "+e.toString());
					}
				}
			}
			if ( savedInstanceState.containsKey("menuVisible") ) {
				wasShowing = savedInstanceState.getBoolean("menuVisible");
				if ( wasShowing )
					openMenu();
			}
			if ( savedInstanceState.containsKey("editVisible") ) {
				wasShowing = savedInstanceState.getBoolean("editVisible");
				if ( wasShowing ) {
					FragmentTransaction transaction = getFragmentManager().beginTransaction();
					transaction.show(trkFragment).commit();
				}
			}
		}
		
		return;
	}
	
	//@SuppressLint("NewApi")
	@Override
	protected void onPause() {
		
		if ( monitor != null )
			monitor.unregister(this);

		// Unregister since the activity is not visible
    	LocalBroadcastManager.getInstance(this).unregisterReceiver(listMessageReceiver);
    	LocalBroadcastManager.getInstance(this).unregisterReceiver(fragMessageReceiver);

		super.onPause();
	}

	@Override
	protected void onResume() {
		
		if ( monitor != null )
			monitor.register(this);
		super.onResume();
		// Register message receivers for the listManager signals and fragment signals
		LocalBroadcastManager.getInstance(this).registerReceiver(listMessageReceiver,	new IntentFilter("listManMsg"));
		LocalBroadcastManager.getInstance(this).registerReceiver(fragMessageReceiver,	new IntentFilter("fragmentMsg"));

	}

	@Override
	protected void onStart() {
		super.onStart();
	}
	
	@Override
	protected void onStop() {
			
		if ( monitor != null )
			monitor.unregister(this);
		
		// Unregister since the activity is not visible
    	LocalBroadcastManager.getInstance(this).unregisterReceiver(listMessageReceiver);
    	LocalBroadcastManager.getInstance(this).unregisterReceiver(fragMessageReceiver);

    	// Flush & close up the logfile
    	Log.cleanup();
    	
		super.onStop();

		// Save the current settings info
		if ( !settings.saveSettings() )
	    	Toast.makeText(this, "CMS Data Browser Save preferences failed!", Toast.LENGTH_LONG).show();
	}
	
    @Override
    public void onBackPressed() {
    	
    	if ( trkFragment.getVisibleState() ) {
    		// OK... we're canceling the addTrack() call
        	FragmentTransaction transaction = getFragmentManager().beginTransaction();

    		if ( exFragment.getLastVisibleState() )
    			transaction.show(exFragment);
    		
    		if ( simFragment.getLastVisibleState() )
    			transaction.show(simFragment);
    		
        	transaction.setCustomAnimations(R.animator.fade_in, R.animator.fade_out);
    		transaction.hide(trkFragment);
    		
    		transaction.commit();

    	} else if ( expFragment.getVisibleState() ) {
        		closeMenu();
        	
    	} else if ( (simFragment != null && simFragment.getVisibleState()) || (exFragment  != null && exFragment.getVisibleState()) ) {
        	FragmentTransaction transaction = getFragmentManager().beginTransaction();
        	transaction.hide(simFragment);
        	transaction.hide(exFragment);
        	
        	transaction.commit();

        	glFragment.clearEvent();
        	
    	} else {
    		super.onBackPressed();
    	}
    	
    	return;
    }
    
    public Bundle getTrackData(int trackID) {
    	return glFragment.getTrackData(trackID);
    }
        
    public List<String> getNumberOfTracks() {
    	if ( glFragment != null ) {
    		return glFragment.getNumberOfTracks();
    	}
    	return null;
    }
    
	public boolean menuIsVisible() {
		return this.expFragment.getVisibleState();
	}
	
	public void closeMenu() {
    	FragmentTransaction transaction = getFragmentManager().beginTransaction();
    	transaction.setCustomAnimations(R.animator.slide_in, R.animator.slide_out);

    	if ( expFragment.getVisibleState() ) {
    		transaction.hide(expFragment).commit();
    		expFragment.setVisibleState(false);
    	}
    	return;
	}
	
	public void openMenu() {
    	FragmentTransaction transaction = getFragmentManager().beginTransaction();
    	transaction.setCustomAnimations(R.animator.slide_in, R.animator.slide_out);

    	if ( !expFragment.getVisibleState() ) {
    		transaction.show(expFragment).commit();
    		expFragment.setVisibleState(true);
    	}
    	return;
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }
	
    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);

	    // Locate MenuItem with ShareActionProvider
	    MenuItem item = menu.findItem(R.id.action_menu_share);

	    // Fetch and store ShareActionProvider
	    mShareActionProvider = (ShareActionProvider)item.getActionProvider();
	    mShareActionProvider.setShareIntent(  new Intent(Intent.ACTION_SEND).setType("image/*") );

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		if ( item.getItemId() == android.R.id.home ) {

			if ( expFragment.getVisibleState() )
				closeMenu();
			else
				openMenu();
	    	return true;
	    	
		} else if ( item.getItemId() == R.id.action_menu_screenshot ) {
			if ( glFragment != null )
				glFragment.setScreenShot(false);
			return true;
			
		} else if ( item.getItemId() == R.id.action_menu_share ) {
			if ( glFragment != null )
				glFragment.setScreenShot(true);
		}
		
		return super.onOptionsItemSelected(item);
	}
	
	public void shareSnapshot(String screenShotURI) {
		Intent intent =
				new Intent(Intent.ACTION_SEND).setType("image/*").putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(screenShotURI)));
		mShareActionProvider.setShareIntent(intent);

		return;
	}
	
	@Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

	public void showToast(final String toast, final int length)	{
	    runOnUiThread(new Runnable() {
	        public void run() {
	        	if ( length == Toast.LENGTH_SHORT || length == Toast.LENGTH_LONG )
	        		Toast.makeText(MainActivity.this, toast, length).show();
	        	else
	        		Toast.makeText(MainActivity.this, toast, Toast.LENGTH_SHORT).show();
	        }
	    });
	    return;
	}
	
	public void showToast(final String toast)	{
	    runOnUiThread(new Runnable() {
	        public void run() {
	            Toast.makeText(MainActivity.this, toast, Toast.LENGTH_SHORT).show();
	        }
	    });
	    return;
	}
	
	public void checkNetwork() {
		if ( netCheckInProgress )
			return;
		
		if ( settings.getIntSetting("debugLevel") > 0 )
			Log.w("MainActivity()", "Checking network connection");
        checkInternetConnection task = new checkInternetConnection();
        task.execute();
		return;
	}
	
    /*******************************************************
     * Check and see if we've got a network connection, and 
     * if we can actually reach our data server
     *******************************************************/
	private class checkInternetConnection extends AsyncTask<Void, Void, Boolean> {

		private  List<String> serverBase;

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			this.serverBase = this.getXMLServerList(R.xml.servers);
			
		} // End onPreExecute
		
		@Override
		protected Boolean doInBackground(Void... none) {
			netCheckInProgress = true;
   		 
			if ( settings.getIntSetting("debugLevel") > 0 )
				Log.w("MainActivity()","Checking net connection ... ");

			boolean hasActiveNet = hasActiveInternetConnection();
			if ( !hasActiveNet )
				settings.putString("server", "none");

			return hasActiveNet;
		}
   	 
		@Override
		protected void onPostExecute(Boolean hasNet) {
			hasNetwork = hasNet;

			if ( simFragment != null )
				simFragment.setNetworkState(hasNetwork);
			if ( exFragment != null )
				exFragment.setNetworkState(hasNetwork);
   		 
			if ( hasNetwork ) {
				// Set up an instance of the error reporter if it doesn't already exist
		        if(!(Thread.getDefaultUncaughtExceptionHandler() instanceof CustomExceptionHandler)) {
		            Thread.setDefaultUncaughtExceptionHandler(new CustomExceptionHandler(
		            		settings.getContext().getCacheDir().getAbsolutePath(),
		            		settings.getStringSetting("server")  + "/crash_reports/upload.php"));
		        }
			}
			
			if ( glFragment != null )
				glFragment.checkUpdatesOnNetwork();
			
			netCheckInProgress = false;
			return;
		}

		private List<String> getXMLServerList(int xmlID) {
			
			List<String> serverList = new ArrayList<String>();
			XmlResourceParser parser = getResources().getXml(xmlID);
			try {
			
				int eventType = parser.getEventType();
				String name;

				while ( eventType != XmlPullParser.END_DOCUMENT ) {
	    		
					switch ( eventType ) {

					case XmlPullParser.START_DOCUMENT:
						break;
	        		
					case XmlPullParser.START_TAG:
	            	
						name = parser.getName();
	            	
						if ( name.equalsIgnoreCase("url"))
							serverList.add( parser.nextText() );
	            
						break;
	            	
					case XmlPullParser.END_TAG:
						break;

					} // End switch ( eventType ) 
	        	
					eventType = parser.next();

				} // End while ( eventType != XmlPullParser.END_DOCUMENT )
		
			} catch (Exception e) {
				Log.e("getXMLServerList()", "Exception reading servers.xml\n"+e.toString());
				serverList.add("http://ubergeek.isageek.net/cmsdb");
				serverList.add("http://freyr.phys.nd.edu/cmsdb");
			}
	    	
			return serverList;
		}

		@SuppressWarnings("unused")
		private List<String> getServerList(String serverDef) {
			
			List<String> serverList = new ArrayList<String>();
			
			try {
				XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
				factory.setNamespaceAware(true);
				XmlPullParser parser = factory.newPullParser();
				
				try {
					InputStream in = null;
					try {
						in = settings.getContext().getAssets().open(serverDef);

					} catch (Exception fe) {
						Log.e("getServerList()", "Can't find servers.xml\n"+fe.toString());
						serverList.add("http://ubergeek.isageek.net/cmsdb");
						serverList.add("http://freyr.phys.nd.edu/cmsdb");
						return serverList;
					}
					parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
			        parser.setInput(in, null);

			        if ( parser == null ) {
			        	Log.e("getServerList()", "xmlParser is null!");
						serverList.add("http://ubergeek.isageek.net/cmsdb");
						serverList.add("http://freyr.phys.nd.edu/cmsdb");
						return serverList;
			        }
			        
					// Parse the server list XML file
			    	int eventType = parser.getEventType();
			    	String name;

			    	while ( eventType != XmlPullParser.END_DOCUMENT ) {
			    		
			        	switch ( eventType ) {

			        	case XmlPullParser.START_DOCUMENT:
			        		break;
			        		
			            case XmlPullParser.START_TAG:
			            	
			            	name = parser.getName();
			            	
			            	if ( name.equalsIgnoreCase("url"))
			            		serverList.add( parser.nextText() );
			            
			            	break;
			            	
			            case XmlPullParser.END_TAG:
			            	break;

			        	} // End switch ( eventType ) 
			        	
			        	eventType = parser.next();

			    	} // End while ( eventType != XmlPullParser.END_DOCUMENT )
				
				
				} catch (IOException ie) {
					Log.e("getServerList()", "I/O Exception\n"+ie.toString());
					serverList.add("http://ubergeek.isageek.net/cmsdb");
					serverList.add("http://freyr.phys.nd.edu/cmsdb");
				}
			} catch (XmlPullParserException xe) {
				Log.e("getServerList()", "Pull Parser Exception\n"+xe.toString());
				serverList.add("http://ubergeek.isageek.net/cmsdb");
				serverList.add("http://freyr.phys.nd.edu/cmsdb");
			}
				
			return serverList;
		}

		private boolean isNetworkAvailable() {
			ConnectivityManager connectivityManager 
   	 			= (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
			NetworkInfo mWifi = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
   		 
			if ( mWifi.isConnectedOrConnecting() )
				isWiFi = true;
   		 
			return activeNetworkInfo != null;
		}

		private Boolean hasActiveInternetConnection() {
   		 
			if (isNetworkAvailable()) {
   				
				for ( int i=0; i<this.serverBase.size(); i++ ) {
					if ( settings.getIntSetting("debugLevel") > 0 )
						Log.w("hasActiveConnection()", "Checking "+this.serverBase.get(i));
   						
					try{
						
						HttpURLConnection urlc = 
								(HttpURLConnection) (new URL(this.serverBase.get(i)+"/network_check.html").openConnection());
						urlc.setRequestProperty("User-Agent", "Test");
						urlc.setRequestProperty("Connection", "close");
						urlc.setConnectTimeout(1500); 
						urlc.connect();
   						 
						if (urlc.getResponseCode() == 200) {
   		   					if ( settings.getIntSetting("debugLevel") > 0 )
   		   						Log.w("hasActiveConnection()", "Saving "+this.serverBase.get(i)+" as server");
   							 
   		   					settings.putString("server", this.serverBase.get(i));
   		   					return true;
						} else if ( settings.getIntSetting("debugLevel") > 0 )
							Log.e("hasActiveConnection()", "Got "+urlc.getResponseCode()+" from server");
   					 
					} catch(IOException e) {
						Log.e("hasActiveConnection()", "Error checking internet connection: "+e.toString());
					}
   						
				} // End for ( int i=0; i<constants.serverBase.length; i++ )

				// If we couldn't find any servers, return false even if there's a good network
				return false;

			} else if ( settings.getIntSetting("debugLevel") > 0 ) {
				Log.w("hasActiveConnection()", "No network available!");
			}
			return false;
		} // End boolean hasActiveInternetConnection
    
	} // End private class checkInternetConnection

	/* Message handler for signals from the OS on the state of the network connection */
    private class ConnectionMonitorReceiver extends BroadcastReceiver {
    	private boolean isRegistered = false;
    	
        @Override
        public void onReceive(Context ctx, Intent intent) {
      		 ConnectivityManager connectivityManager 
    	 		= (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
    		 NetworkInfo netInfo = connectivityManager.getActiveNetworkInfo();

            if (netInfo == null) {
            	if ( hasNetwork ) {
            		if ( settings.getIntSetting("debugLevel") > 0 )
            			Log.w("ConnectionMonitor", "Network state changed from "+hasNetwork+" to false");
            			
            		hasNetwork = false;
                	if ( simFragment != null )
                		simFragment.setNetworkState(false);
                	if ( exFragment != null )
                		exFragment.setNetworkState(false);
            	}
            	
            	return ;
            }
            // Are we seeing a state change?
            if ( netInfo.isConnected() !=  hasNetwork ) {
            	if ( settings.getIntSetting("debugLevel") > 0 )
            		Log.w("ConnectionMonitor", "Network state changed from "+hasNetwork+" to "+netInfo.isConnected());

            		hasNetwork = netInfo.isConnected();

            	// If there's a network connection....
         		 if ( hasNetwork ) {
         			 // make sure we can reach our server
         			 if ( !netCheckInProgress ) {
         				 
         				 checkInternetConnection task = new checkInternetConnection();
         				 task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[])null);
         			 }
         			 
         	        // Let the user know if the network state has changed
                 	if ( settings.getIntSetting("debugLevel") > 1 )
                 		Toast.makeText(getApplicationContext(), 
                 				getApplicationContext().getString(R.string.network_up), Toast.LENGTH_LONG).show();
            		
         		 } else if ( settings.getIntSetting("debugLevel") > 1 )
          			 Toast.makeText(getApplicationContext(), 
           					getApplicationContext().getString(R.string.network_down), Toast.LENGTH_LONG).show();
            	
            	// See if this is Wifi or 3/4 G
         		 netInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
          		 
          		 if ( netInfo.isConnectedOrConnecting() )
          			 isWiFi = true;
          		 
            } // End if ( netInfo.isConnected() !=  hasNetwork ) 

        } // End onReceive(Context ctx, Intent intent)

        public void register(Context c) {
        	if ( !isRegistered ) {
        		c.registerReceiver(this, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        		isRegistered = true;
        	}
        } // End register(Context c)
        
        public void unregister(Context c) {
        	if ( isRegistered ) {
        		c.unregisterReceiver(this);
        		isRegistered = false;
        	}
        } // End unregister(Context c)
        
    } // End class ConnectionMonitorReceiver


	/***********************************************************************************
    /* Message handler for signals from fragments. This is separated from the receiver
     * below (the listManMsg receiver) purely for convenience. List messages go there,
     * fragment messages come here, even though there is some cross-over between them.
     ***********************************************************************************/
    private BroadcastReceiver fragMessageReceiver;
	{
		fragMessageReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {

				String name = "";
				int progress = 0;

				if (intent.hasExtra("widgetName"))
					name = intent.getStringExtra("widgetName");

				if (intent.hasExtra("seekBarStatusChanged")) {

					if (intent.hasExtra("progress"))
						progress = intent.getIntExtra("progress", -1);
				}

				if (name.equalsIgnoreCase("ptcut")) {
					settings.putFloat("ptcut", (float) progress / 1000.0f);
					glFragment.applyPTCut();

					// Make sure the simulate & explore fragments have their sliders synchronized
					simFragment.setPtSlider();
					exFragment.setPtSlider();

				} else if (name.equalsIgnoreCase("getEventData")) {

					if (intent.hasExtra("result")) {

						// Get the particle data
						List<String> result = intent.getStringArrayListExtra("result");

						// If there's an event on display, tell the renderer to clear it out
						if (glFragment.isEventLoaded())
							glFragment.clearEvent();

						// Add this event to the event pool (the pool waits until
						// the renderer is done cleaning up events from above)
						glFragment.setEventData(result);

						// Store the data particle string
						exFragment.setEventString(result);

						if (!exFragment.isLocal()) {
							// Expose the "save event" button
							expFragment.unHideItem("eventSave");
							expFragment.hideItem("eventDelete");
						} else {
							// Or the "delete event" if it's already stored
							expFragment.unHideItem("eventDelete");
							expFragment.hideItem("eventSave");
						}

					} else
						Log.e("fragMessageReceiver()", "Got null result from dataList (data)!");

				} else if (name.equalsIgnoreCase("getSimData")) {

					if (intent.hasExtra("result")) {

						// Get the particle list
						List<String> result = intent.getStringArrayListExtra("result");

						// If there's an event on display, tell the renderer to clear it out
						if (glFragment.isEventLoaded())
							glFragment.clearEvent();

						// Add this event to the event pool (the pool waits until
						// the renderer is done cleaning up events from above)
						glFragment.setEventData(result);
						simFragment.setSimulateString(result);

						if (!simFragment.isLocal()) {
							// Expose the "save event" button
							expFragment.unHideItem("eventSave");
							expFragment.hideItem("eventDelete");
						} else {
							// Or the "delete event" if it's already stored
							expFragment.unHideItem("eventDelete");
							expFragment.hideItem("eventSave");
						}
					} else
						Log.e("fragmentMessageReceiver()", "Got null result from dataList (sim)!");

				} else if (name.equalsIgnoreCase("addTrack")) {

					if (intent.hasExtra("trackData")) {
						Bundle trackData = intent.getBundleExtra("trackData");
						glFragment.addTrack(trackData);
					}

				} else if (name.equalsIgnoreCase("editTrack")) {

					if (intent.hasExtra("trackData")) {
						Bundle trackData = intent.getBundleExtra("trackData");
						glFragment.editTrack(trackData);
					}

				} else if (name.equalsIgnoreCase("deleteTrack")) {

					if (intent.hasExtra("trackData")) {
						Bundle trackData = intent.getBundleExtra("trackData");

						if (trackData.containsKey("trackID")) {
							int trackID = trackData.getInt("trackID");

							if (trackID > -1)
								glFragment.deleteTrack(trackID);
						}
					}

				} else if (intent.hasExtra("widgetName"))
					Log.w("fragMessageReceiver()", "Got a signal from " + intent.getStringExtra("widgetName"));
				else
					Log.w("fragMessageReceiver()", "Got a spurious signal from an unnamed widget");

				return;
			}
		};
	}

	// Message handler for listManager signals from the navigation drawer
    private BroadcastReceiver listMessageReceiver;
	{
	listMessageReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {

				if (intent.hasExtra("textOnClick")) {

					if (intent.hasExtra("itemName")) {

						String name = intent.getStringExtra("itemName");
						FragmentTransaction transaction = getFragmentManager().beginTransaction();

						if (name.equalsIgnoreCase("Explore") && exFragment != null && (hasNetwork || exFragment.hasLocalData())) {

							if (simFragment != null && simFragment.getVisibleState())
								transaction.hide(simFragment);

							if (trkFragment != null && trkFragment.getVisibleState())
								transaction.hide(trkFragment);

							if (exFragment != null && !exFragment.getVisibleState()) {
								transaction.show(exFragment);

							} else
								Log.w("BroadcastReceiver()", "Unable to find ExploreFragment!");

							// And close the menu since we'll be working on other things for a while
							closeMenu();

						} else if (name.equalsIgnoreCase("Simulate") && simFragment != null && (hasNetwork || simFragment.hasLocalData())) {

							if (exFragment != null && exFragment.getVisibleState())
								transaction.hide(exFragment);

							if (trkFragment != null && trkFragment.getVisibleState())
								transaction.hide(trkFragment);

							if (simFragment != null && !simFragment.getVisibleState()) {
								transaction.show(simFragment);

							} else
								Log.e("BroadcastReceiver()", "Unable to find SimulateFragment!");

							// And close the menu since we'll be working on other things for a while
							closeMenu();

						} else if (name.equalsIgnoreCase("Add/Edit")) {

							if (exFragment.getVisibleState())
								transaction.hide(exFragment);

							if (simFragment.getVisibleState())
								transaction.hide(simFragment);

							transaction.setCustomAnimations(R.animator.fade_in, R.animator.fade_out);
							transaction.show(trkFragment);

						} else if (name.equalsIgnoreCase("Home")) {
							glFragment.setHomeView();
						} else if (name.equalsIgnoreCase("YZView")) {
							glFragment.setXView();
						} else if (name.equalsIgnoreCase("XYView")) {
							glFragment.setZView();

						} else if (name.equalsIgnoreCase("eventSave")) {

							if (glFragment.isEventLoaded()) {

								if (settings.getBooleanSetting("isData")) {
									if (exFragment.storeEvent())
										Toast.makeText(settings.getContext(), "Event saved", Toast.LENGTH_SHORT).show();
									else
										Toast.makeText(settings.getContext(), "Event save failed", Toast.LENGTH_SHORT).show();

								} else if (settings.getBooleanSetting("isSim")) {
									simFragment.storeSim();
									Toast.makeText(settings.getContext(), simFragment.getSelectedSimName() + " saved", Toast.LENGTH_SHORT).show();
								}
							}

						} else if (name.equalsIgnoreCase("eventDelete")) {

							if (settings.getBooleanSetting("isData")) {
								if (exFragment.removeEvent()) {
									Toast.makeText(settings.getContext(), "Event deleted", Toast.LENGTH_SHORT).show();
									settings.putBoolean("isData", false);
									glFragment.clearEvent();
								} else
									Toast.makeText(settings.getContext(), "Event delete failed", Toast.LENGTH_SHORT).show();
							} else {
								simFragment.removeSim();
								Toast.makeText(settings.getContext(), simFragment.getSelectedSimName() + " removed from DB", Toast.LENGTH_SHORT).show();
								settings.putBoolean("isSim", false);
								glFragment.clearEvent();
							}
						} else if (name.equalsIgnoreCase("dbClear")) {
							exFragment.clearDB();
							simFragment.clearDB();

						} else
							Log.w("onReceive()", name + " was just clicked");

						// Complete the transaction
						transaction.commit();

						// And close the menu since we'll be working on other things for a while
						closeMenu();

					} // End if ( intent.hasExtra("itemName") )

					// End if ( intent.hasExtra("textOnClick") )

				} else if (intent.hasExtra("seekBarStatusChanged")) {
					String type = "", name = "";
					int progress = 0;

					if (intent.hasExtra("changeType"))
						type = intent.getStringExtra("changeType");
					if (intent.hasExtra("widgetName"))
						name = intent.getStringExtra("widgetName");
					if (intent.hasExtra("progress"))
						progress = intent.getIntExtra("progress", -1);

					if (name.equalsIgnoreCase("All")) {

						if (glFragment.setTransparency(constants.ALL, progress))
							settings.putInt("transAll", progress);
						else if (settings.getIntSetting("debugLevel") > 0)
							Log.e("BroadcastReceiver()", "Failed to set ALL transparency to " + progress);

						if (type.equalsIgnoreCase("seekBarStopTracking")) {
							expFragment.setItem("transTrkr", progress);
							settings.putInt("transTrkr", progress);

							expFragment.setItem("transEcal", progress);
							settings.putInt("transEcal", progress);

							expFragment.setItem("transHcal", progress);
							settings.putInt("transHcal", progress);

							expFragment.setItem("transCryo", progress);
							settings.putInt("transCryo", progress);

							expFragment.setItem("transYoke", progress);
							settings.putInt("transYoke", progress);

							expFragment.setItem("transCaps", progress);
							settings.putInt("transCaps", progress);

							expFragment.setItem("transAxes", progress);
							settings.putInt("transAxes", progress);

						}

					} else if (name.equalsIgnoreCase("Trkr")) {

						if (glFragment.setTransparency(constants.TRKR, progress))
							settings.putInt("transTrkr", progress);
						else if (settings.getIntSetting("debugLevel") > 0)
							Log.e("BroadcastReceiver()", "Failed to set TRKR transparency to " + progress);

					} else if (name.equalsIgnoreCase("Ecal")) {

						if (glFragment.setTransparency(constants.ECAL, progress))
							settings.putInt("transEcal", progress);
						else if (settings.getIntSetting("debugLevel") > 0)
							Log.e("BroadcastReceiver()", "Failed to set ECAL transparency to " + progress);

					} else if (name.equalsIgnoreCase("Hcal")) {

						if (glFragment.setTransparency(constants.HCAL, progress))
							settings.putInt("transHcal", progress);
						else if (settings.getIntSetting("debugLevel") > 0)
							Log.e("BroadcastReceiver()", "Failed to set HCAL transparency to " + progress);

					} else if (name.equalsIgnoreCase("Cryo")) {

						if (glFragment.setTransparency(constants.CRYO, progress))
							settings.putInt("transCryo", progress);
						else if (settings.getIntSetting("debugLevel") > 0)
							Log.e("BroadcastReceiver()", "Failed to set CRYO transparency to " + progress);

					} else if (name.equalsIgnoreCase("Yoke")) {

						if (glFragment.setTransparency(constants.YOKE, progress))
							settings.putInt("transYoke", progress);
						else if (settings.getIntSetting("debugLevel") > 0)
							Log.e("BroadcastReceiver()", "Failed to set YOKE transparency to " + progress);

					} else if (name.equalsIgnoreCase("Caps")) {

						if (glFragment.setTransparency(constants.CAPS, progress))
							settings.putInt("transCaps", progress);
						else if (settings.getIntSetting("debugLevel") > 0)
							Log.e("BroadcastReceiver()", "Failed to set CAPS transparency to " + progress);

					} else if (name.equalsIgnoreCase("Axes")) {

						if (glFragment.setTransparency(constants.AXES, progress))
							settings.putInt("transAxes", progress);
						else if (settings.getIntSetting("debugLevel") > 0)
							Log.e("BroadcastReceiver()", "Failed to set AXES transparency to " + progress);

					} else if (name.equalsIgnoreCase("zoomSpeed")) {
						if (type.equalsIgnoreCase("seekBarStopTracking")) {
							settings.putInt("zoomSpeed", progress + 2);
							glFragment.setZoomSpeed(progress + 2);
						}

					} else if (name.equalsIgnoreCase("rotationSpeed")) {
						if (type.equalsIgnoreCase("seekBarStopTracking")) {
							float rotateSpeed = 500f - progress;
							rotateSpeed = -1f / rotateSpeed;
							settings.putFloat("rotateSpeed", rotateSpeed);
							glFragment.setRotationWeight(rotateSpeed);
						}

					} else if (name.equalsIgnoreCase("resolution")) {
						if (type.equalsIgnoreCase("seekBarStopTracking")) {
							float trackRes = progress + 1f;
							trackRes = 1f / trackRes;
							settings.putFloat("trackRes", trackRes);
						}

					} else if (name.equalsIgnoreCase("lineWidth")) {
						if (type.equalsIgnoreCase("seekBarStopTracking")) {
							float lineWidth = progress + 10f;
							lineWidth /= 10f;
							settings.putFloat("lineWidth", lineWidth);
						}

					} else if (name.equalsIgnoreCase("maxTime")) {
						if (type.equalsIgnoreCase("seekBarStopTracking"))
							settings.putInt("maxTime", progress + 25);

					} else if (name.equalsIgnoreCase("debugLevel")) {
						if (type.equalsIgnoreCase("seekBarStopTracking"))
							settings.putInt("debugLevel", progress);

					} else if (name.equalsIgnoreCase("maxEvents")) {
						if (progress < 1)
							progress = 1;
						if (type.equalsIgnoreCase("seekBarStopTracking")) {
							settings.putInt("maxEvents", progress * 10);
							Log.w("listMsgRec()", "Saved maxEvents = " + settings.getIntSetting("maxEvents"));
						}
					} else
						Log.w("onReceive()", "Slider " + name + " changed to " + progress);

					// End else if ( intent.hasExtra("seekBarStatusChanged") )

				} else if (intent.hasExtra("switchCheckedChanged")) {
					String name = "";
					boolean checked = false;
					boolean toggleDetector = false;
					boolean updateVisible = false;

					if (intent.hasExtra("widgetName"))
						name = intent.getStringExtra("widgetName");
					if (intent.hasExtra("checked"))
						checked = intent.getBooleanExtra("checked", false);

					if (name.equalsIgnoreCase("Detector") || name.equalsIgnoreCase("All")) {

						if (glFragment.setVisibility(constants.ALL, checked)) {
							// Make sure all of the individual elements reflect the new state
							expFragment.setItem("TransAll", checked);
							expFragment.setItem("TransTrkr", checked);
							expFragment.setItem("TransEcal", checked);
							expFragment.setItem("TransHcal", checked);
							expFragment.setItem("TransCryo", checked);
							expFragment.setItem("TransYoke", checked);
							expFragment.setItem("TransCaps", checked);
							expFragment.setItem("TransAxes", checked);
							expFragment.setItem("Detector", checked);

							// And store the new settings
							settings.putBoolean("allState", checked);
							settings.putBoolean("trkrState", checked);
							settings.putBoolean("ecalState", checked);
							settings.putBoolean("hcalState", checked);
							settings.putBoolean("cryoState", checked);
							settings.putBoolean("yokeState", checked);
							settings.putBoolean("capsState", checked);
							settings.putBoolean("axesState", checked);

						} else if (settings.getIntSetting("debugLevel") > 0)
							Log.e("BroadcastReceiver()", "Failed to set ALL visibility to " + checked);

					} else if (name.equalsIgnoreCase("Detector Response")) {
						settings.putBoolean("response", checked);
						updateVisible = true;

					} else if (name.equalsIgnoreCase("Particle Paths")) {
						settings.putBoolean("paths", checked);
						updateVisible = true;

					} else if (name.equalsIgnoreCase("Trkr")) {

						if (glFragment.setVisibility(constants.TRKR, checked))
							settings.putBoolean("trkrState", checked);
						else if (settings.getIntSetting("debugLevel") > 0)
							Log.e("BroadcastReceiver()", "Failed to set TRKR visibility to " + checked);

						toggleDetector = checked;

					} else if (name.equalsIgnoreCase("Ecal")) {

						if (glFragment.setVisibility(constants.ECAL, checked))
							settings.putBoolean("ecalState", checked);
						else if (settings.getIntSetting("debugLevel") > 0)
							Log.e("BroadcastReceiver()", "Failed to set ECAL visibility to " + checked);

						toggleDetector = checked;

					} else if (name.equalsIgnoreCase("Hcal")) {

						if (glFragment.setVisibility(constants.HCAL, checked))
							settings.putBoolean("hcalState", checked);
						else if (settings.getIntSetting("debugLevel") > 0)
							Log.e("BroadcastReceiver()", "Failed to set HCAL visibility to " + checked);

						toggleDetector = checked;

					} else if (name.equalsIgnoreCase("Cryo")) {

						if (glFragment.setVisibility(constants.CRYO, checked))
							settings.putBoolean("cryoState", checked);
						else if (settings.getIntSetting("debugLevel") > 0)
							Log.e("BroadcastReceiver()", "Failed to set CRYO visibility to " + checked);

						toggleDetector = checked;

					} else if (name.equalsIgnoreCase("Yoke")) {

						if (glFragment.setVisibility(constants.YOKE, checked))
							settings.putBoolean("yokeState", checked);
						else if (settings.getIntSetting("debugLevel") > 0)
							Log.e("BroadcastReceiver()", "Failed to set YOKE visibility to " + checked);

						toggleDetector = checked;

					} else if (name.equalsIgnoreCase("Caps")) {

						if (glFragment.setVisibility(constants.CAPS, checked))
							settings.putBoolean("capsState", checked);
						else if (settings.getIntSetting("debugLevel") > 0)
							Log.e("BroadcastReceiver()", "Failed to set CAPS visibility to " + checked);

						toggleDetector = checked;

					} else if (name.equalsIgnoreCase("Axes")) {

						if (glFragment.setVisibility(constants.AXES, checked))
							settings.putBoolean("axesState", checked);
						else if (settings.getIntSetting("debugLevel") > 0)
							Log.e("BroadcastReceiver()", "Failed to set AXES visibility to " + checked);

						toggleDetector = checked;

					} else if (name.equalsIgnoreCase("Electrons")) {
						settings.putBoolean("showElectrons", checked);
						updateVisible = true;

					} else if (name.equalsIgnoreCase("Photons")) {
						settings.putBoolean("showPhotons", checked);
						updateVisible = true;

					} else if (name.equalsIgnoreCase("Tracks")) {
						settings.putBoolean("showTracks", checked);
						updateVisible = true;

					} else if (name.equalsIgnoreCase("Jets/MET")) {
						settings.putBoolean("showJetMET", checked);
						updateVisible = true;

					} else if (name.equalsIgnoreCase("Muons")) {
						settings.putBoolean("showMuons", checked);
						updateVisible = true;

					} else if (name.equalsIgnoreCase("Axis")) {
						settings.putBoolean("showAxis", checked);
						updateVisible = true;

					} else if (name.equalsIgnoreCase("Cone")) {
						settings.putBoolean("showCone", checked);
						updateVisible = true;

					} else if (name.equalsIgnoreCase("Hadrons")) {
						settings.putBoolean("showHadrons", checked);
						updateVisible = true;

					} else if (name.equalsIgnoreCase("Tracker")) {
						settings.putBoolean("showTracker", checked);
						updateVisible = true;

					} else if (name.equalsIgnoreCase("StandAlone")) {
						settings.putBoolean("showStandAlone", checked);
						updateVisible = true;

					} else if (name.equalsIgnoreCase("Global")) {
						settings.putBoolean("showGlobal", checked);
						updateVisible = true;

					} else if (name.equalsIgnoreCase("Underlying Event")) {
						settings.putBoolean("showUnderlying", checked);
						updateVisible = true;

					} else if (name.equalsIgnoreCase("Undetectable")) {
						settings.putBoolean("showUndetectable", checked);
						updateVisible = true;

					} else
						Log.w("onReceive()", "Status of " + name + " has changed to " + checked);

					if (toggleDetector) {
						// If the "Detector" toggle is off and we turned something on, toggle "Detector" switch back on
						boolean state = expFragment.getWidgetState("Detector", "switch");
						if (!state)
							expFragment.setWidgetState("Detector", "switch", true);
					}

					if (updateVisible)
						glFragment.updateVisible();

				} // End else if ( intent.hasExtra("switchCheckedChanged") )
			} // End
		};
	}

} // End class MainActivity
