package ch.cern.cms.data_browser;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import android.content.Context;
import android.os.AsyncTask;

import com.threed.jpct.Config;
import com.threed.jpct.Loader;
import com.threed.jpct.Object3D;
import com.threed.jpct.SimpleVector;
import com.threed.jpct.World;

public class CMSModel {

	private Context mContext = null;
	private Settings settings = null;
	private World world = null;
	private boolean setup = true;
	private List<model> modelList;
	private Logger Log;
	
	public CMSModel (Context iContext, World world) {

		settings = Settings.getInstance();
		this.mContext = settings.getContext();

		// Start up the logger
		Log = Logger.getInstance();
		
		if ( settings.getIntSetting("debugLevel") > 0 ) {
			SimpleDateFormat s = new SimpleDateFormat("hh:mm:ss", Locale.US);
			String format = s.format(new Date());

			Log.w("CMSModel()", "Starting model creation at "+format);
		}
		
		//this.mContext = iContext;
		this.world = world;
		
		Config.glTransparencyMul = 0.0078f;
		Config.glTransparencyOffset = 0.0078f;
		Config.collideOffset = 60f;

		loadModelParams [] params;
		params = new loadModelParams[constants.modelFiles.length];
		
		for ( int i=0; i<constants.modelFiles.length; i++ )
			params[i] = new loadModelParams(constants.modelFiles[i], constants.modelIDs[i]);
		
		loadModelsAsyncTask task = new loadModelsAsyncTask();
		task.execute(params);
		
		setup = false;
		return;
	}
	
	public CMSModel (Context iContext) {
		
		SimpleDateFormat s = new SimpleDateFormat("hh:mm:ss", Locale.US);
		String format = s.format(new Date());

		Log.e("CMSModel()", "Starting model creation at "+format);
		
		this.mContext = iContext;
		this.world = null;
		
		Config.glTransparencyMul = 0.0078f;
		Config.glTransparencyOffset = 0.0078f;
		Config.collideOffset = 60f;

		settings = Settings.getInstance();
		
		loadModelParams [] params;
		params = new loadModelParams[constants.modelFiles.length];
		
		for ( int i=0; i<constants.modelFiles.length; i++ )
			params[i] = new loadModelParams(constants.modelFiles[i], constants.modelIDs[i]);
		
		loadModelsAsyncTask task = new loadModelsAsyncTask();
		task.execute(params);
		
		setup = false;
		return;
	}
	
    /*********************************************************/
    /******** Private accessors into the setting class *******/
    /*********************************************************/
    private int getIntSetting( String key ) {
    	return settings.getIntSetting(key);
    }
    private boolean getBooleanSetting( String key ) {
    	return settings.getBooleanSetting(key);
    }
    /*********************************************************/

	public SimpleVector getTransformedCenter() {
		return modelList.get(0).getObject().getTransformedCenter();
	}
	
	public boolean setTransparency(int which, int value) {
		if ( setup )
			return false;
		
		value = (value >= constants.MAX_TRANSPARENCY) ? -1 : value;
		for ( int i=0; i<modelList.size(); i++ ) {

			if ( modelList.get(i).getObject() != null ) {

				if ( which == constants.ALL )
					modelList.get(i).getObject().setTransparency(value);
				else if (modelList.get(i).ID == which) {
					modelList.get(i).getObject().setTransparency(value);
					return true;
				}
			}
		}
		return true;
	}
	
	public boolean setVisibility(int which, boolean value) {
		if ( setup )
			return false;
		
		for ( int i=0; i<modelList.size(); i++ ) {

			if ( modelList.get(i).getObject() != null ) {

				if ( which == constants.ALL )
					modelList.get(i).getObject().setVisibility(value);
				else if (modelList.get(i).ID == which) {
					modelList.get(i).getObject().setVisibility(value);
					return true;
				}
			}
		}

		return true;
	}

	// Simple class to combine the strings & ints that are headed into the loadModelsAsyncTask class
	private class loadModelParams {
		
		String fileName;
		int    ID;
		
		loadModelParams(String f, int i) {
			this.fileName = f;
			this.ID = i;
			return;
		}
	}

	public void checkUpdatesOnNetwork() {
		checkUpdatesAsyncTask task = new checkUpdatesAsyncTask();
		task.execute();
		return;
	}
	
	// Asynchronus task to load the CMS model from storage in the background
	// so the renderer can finish its setup and release back to main
	// Takes an array of type loadModelParams (defined just above) and 
	// constructs a new model from each parameter set (file name and id number)
    private class loadModelsAsyncTask extends AsyncTask<loadModelParams, Void, Void> {

    	@Override
    	protected Void doInBackground(loadModelParams... params) {
    		modelList = new ArrayList<model>();
    		
    		for ( int i=0; i<params.length; i++ ) {
    			model model = new model(mContext, params[i].fileName, params[i].ID);
    			if ( model != null ) {
    				modelList.add( model );
    			}
    		}
    		return null;
    	}

    	@Override
    	protected void onPostExecute(Void dummy) {

    		if ( settings.getIntSetting("debugLevel") > 0 ) {
    			SimpleDateFormat s = new SimpleDateFormat("hh:mm:ss", Locale.US);
    			String format = s.format(new Date());

    			Log.w("CMSModel()", "done at "+format);
    		}
    		
    		setup = false;

    		return;
    	}
    	
    } // End class loadModelsAsyncTask
    	
    private class checkUpdatesAsyncTask extends AsyncTask<Void, Void, Void> {
    	
    	@Override
    	protected Void doInBackground(Void... dummy) {
			String server = settings.getStringSetting("server");
			boolean hasNetworkServer = !server.equalsIgnoreCase("none");
			
			// If there's no network, don't bother looking for an update
			if ( !hasNetworkServer )
				return null;
			
			if ( getIntSetting("debugLevel") > 0 )
				Log.w("checkUpdatesAsyncTask()", "We has network & server, checking for updated models");
				
			for ( int i=0; i<modelList.size(); i++ ) {
				if ( !modelList.get(i).loadCheckDone() )
					modelList.get(i).doFileCheck();
					
				if ( !modelList.get(i).hasCurrentFile() ) {
					modelList.get(i).downloadUpdate();
					
					if ( modelList.get(i).hasCurrentFile() ) {
						modelList.get(i).loadFile();

						if ( modelList.get(i).isLoaded() )
							modelList.get(i).setup();
							
					} // End if ( modelList.get(i).hasCurrentFile()
	
				} // End if ( !modelList.get(i).hasCurrentFile() )

				// Make sure the update worked....
				if ( !modelList.get(i).isLoaded() || !modelList.get(i).hasCurrentFile() )
					Log.e("checkUpdatesAsyncTask()", "File update failed!");
					
			} // End for ( int i=0; i<modelList.size(); i++ ) 
				
    		return null;
    	}
    	
    	@Override
    	protected void onPostExecute(Void dummy) {
    		return;
    	}
    	
    }
    
    
    /*********************************************************/
	/*  Small class to load a single serialized 3ds model    */
	/*  with appropriate checks and scans for model updates  */
	/*********************************************************/
	private class model {
		
		private boolean loadCheck = false;
		private boolean hasCurrent = false;
		private boolean fileIsThere = true;
		private boolean fileIsLoaded = false;
		private boolean addedToWorld = false;
		
		private int ID = 0;
		
		private String fileName = null;
		private String md5sum = "0x0";
		private Object3D object = null;
		private float scale = 100.0f;
		
		private Context mContext = null;
		
		public model( Context mContext, String fileName, int ID ) {

			this.fileName = fileName;
			this.mContext = mContext;
			this.ID = ID;
			
			// Load the model file and, 
			loadFile();
			
			// if it loaded properly, do the setup stuff
			if ( fileIsLoaded )
				setup();

			return;
			
		} // End public subsystem()
		
		/*****************************************************/
		/*       Basic class accessors                       */
		/*****************************************************/

		public boolean isLoaded() {
			return this.fileIsLoaded;
		}
		
		public boolean loadCheckDone() {
			return this.loadCheck;
		}
		
		public boolean hasCurrentFile() {
			return this.hasCurrent;
		}
		
		public Object3D getObject() {
			return this.object;
		}

		/*****************************************************/
		/* The meat of the matter, where all the work is done*/
		/*****************************************************/

		// Check that the file is there and that it's the latest version
		public void doFileCheck() {

			// If we've already done the loadCheck, don't bother
			if ( this.loadCheck )
				return;

			// If there's no file, we obviously need an update
			if ( !this.fileIsThere ) {
				this.hasCurrent = false;
				this.loadCheck = true;
				return;
			}
			
			InputStream is = null;
			String server = settings.getStringSetting("server");
			boolean hasNetworkServer = !server.equalsIgnoreCase("none");

			// If the file exists get the md5 sum for it to check for updates
			if ( this.fileIsThere ) {
				MessageDigest md = null;
				
				try {
					is = mContext.openFileInput(this.fileName);
					md = MessageDigest.getInstance("MD5");
					
					byte [] buffer = new byte[1024];
					int numRead = 0;
					while ( numRead != -1 ) {
						numRead = is.read(buffer);
						if (numRead > 0)
							md.update(buffer, 0, numRead);
					}
					
					byte [] digest = md.digest();
					this.md5sum = "";
					
					for ( int i=0; i< digest.length; i++)
						this.md5sum += Integer.toString(( digest[i] & 0xff ) + 0x100, 16).substring(1);

				} catch ( Exception e ) {
					Log.e("doFileCheck()", "MD5 failed: "+e.toString());
				} // End try/catch 
				
			} // End if ( fileIsThere )

			if ( !hasNetworkServer )
				this.loadCheck = false;
			
			// If we're checking for an update, do it now
			if ( this.fileIsThere && hasNetworkServer ) {
				// Get the checksum for the file on the server
				String url = server + "/" + "getMD5.pl";
				String result = null;
				HttpClient httpclient = new DefaultHttpClient();
				HttpPost httppost = new HttpPost(url);
				
				List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
				nameValuePairs.add(new BasicNameValuePair("file", "ser/"+this.fileName));

				try {
					 httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
				 } catch ( Exception e ) {
					 Log.w("Event", "Unable to set UrlEncoding");
				 }

				if ( httppost != null ) {
					try {
						HttpResponse response = httpclient.execute(httppost);
						result = EntityUtils.toString(response.getEntity()).trim();

						// If the checksums are the same, then the local file is up to date
						this.hasCurrent = result.equals(this.md5sum);
						
						
						//******************** Force a file check for debugging ******************************//
						//if ( this.fileName.equalsIgnoreCase("hcal.ser") )
							//this.hasCurrent = false;
						
					} catch ( Exception e ) {
						Log.e("Event", "httpclient.execute() "+e.toString());
					}
				} // End if ( httppost != null )

				// We're done with the initial checks
				this.loadCheck = true;

			} // End if ( fileIsThere && hasNetworkServer )

			if ( getIntSetting("debugLevel") > 0 )
				Log.e("doFileCheck()", "file "+this.fileName+" is current = "+hasCurrent);
			
			return;
		} // End doLoadCheck()
		
		// If there's an updated file on the server, go get it
		public void downloadUpdate() {
			
			// Make sure we're working with up-to-the-microsecond information
			if ( !this.loadCheck ) {
				this.doFileCheck();
				if ( !this.loadCheck )
					return;	
			}
			
			// If we've already done a load check and this is the current file,
			// what the !@$(!@_ are we doing here anyhow? Go away
			if ( this.hasCurrent )
				return;
			
			String server = settings.getStringSetting("server");
			boolean hasNetworkServer = !server.equalsIgnoreCase("none");
			
			// If we don't have the most current file, we should go get it
			if ( hasNetworkServer ) {
			
				HttpClient httpclient = new DefaultHttpClient();
				HttpPost httppost = null;
				String url = server + "/ser/" + fileName;

				httppost = new HttpPost(url);
				if ( httppost != null ) {
					((MainActivity)mContext).showToast(fileName.substring(0, fileName.length()-4)+" downloading update");
					
					try {
						HttpResponse response = httpclient.execute(httppost);

						InputStream in = response.getEntity().getContent();
   		 				int contentSize = (int) response.getEntity().getContentLength();
   		 				if ( contentSize > 0 ) {
   		
   		 					BufferedInputStream bis = new BufferedInputStream(in, 1024);
   		 					byte[]	data = new byte[contentSize];
   		 					int bytesRead = 0;
   		 					int offset = 0;
   		 				
   		 					while (bytesRead != -1 && offset < contentSize) {
   		 						bytesRead = bis.read(data, offset, contentSize - offset);
   		 						offset += bytesRead;
   		 					}

   		 					try {
   		 						if ( getIntSetting("debug") > 1)
   		 							Log.e("subsystem()", "Downloaded "+fileName+" from "+server+" ... Saving to internal storage");

   		 						OutputStream out = mContext.openFileOutput(fileName, Context.MODE_PRIVATE);
   		 						
   		 						out.write(data);
   		 						out.close();
   		 						if ( getIntSetting("debug") > 1) Log.e("subsystem()", fileName+" updated");
   		 						((MainActivity)mContext).showToast(fileName.substring(0, fileName.length()-4)+" model updated");
   		 						this.hasCurrent = true;
   		 						this.fileIsThere = true;

   		 					} catch (IOException ioe) {
   		 						Log.e("model()", "Unable to write "+fileName);
   		 						((MainActivity)mContext).showToast("Failed. Unable to write "+fileName.substring(0, fileName.length()-4)+" to internal storage");
   		 					}
   		 				} else {          // End if ( contentSize > 0 ) 
   		 					Log.e("model()", "Zero length response from server d/l "+fileName.substring(0, fileName.length()-4));
   		 					((MainActivity)mContext).showToast("Failedto update "+fileName.substring(0, fileName.length()-4)+" Zero length response from server");
   		 				}
   		 			} catch ( Exception e ) {
   		 				Log.e("model()", "httpclient.execute() "+e.toString());
   		 				((MainActivity)mContext).showToast("Failed to update "+fileName.substring(0, fileName.length()-4));
   		 			}
   		 		} // End if ( httppost != null )

			} // End if ( !this.hasCurrent && hasNetworkServer )

			return;
			
		}
		
		// Load the file from storage and create the 3D model it contains
		public void loadFile() {
			InputStream is = null;

			// If the file exists load it.
			try {
				is = mContext.openFileInput(this.fileName);
				this.fileIsThere = true;

			} catch (FileNotFoundException fnfe) {
				Log.e("loadFile()", "No such file "+fileName);
				this.fileIsThere = false;
				this.fileIsLoaded = false;
				this.hasCurrent = false;
				return;
			}

			Object3D[] model = Loader.loadSerializedObjectArray(is);
			this.object = new Object3D(0);

			for (int i = 0; i < model.length; i++) {
				model[i].setCenter(SimpleVector.ORIGIN);
				this.object = Object3D.mergeObjects(this.object, model[i]);
			}

			this.object.rotateY(0.5f*(float)Math.PI);
			this.object.rotateZ(0.5f*(float)Math.PI);

			this.fileIsLoaded = (this.object != null);
			if ( !this.fileIsLoaded )
				Log.w("loadFile()", "Something went wrong loading "+fileName);

			return;
		} // End loadFile()
		
		// Do the basic setup/compilation for the loaded models
		public void setup() {
			
			int transparency = constants.MAX_TRANSPARENCY;
			boolean visibility = true;
			
			if ( this.object == null )
				return;
			
			// Do the basic setup stuff common to most objects
			this.object.setCulling(false);
			this.object.forceGeometryIndices(true);
			this.object.setScale(this.scale);
			this.object.setCollisionMode(Object3D.COLLISION_CHECK_OTHERS);

			// Now handle the object specific setup stuff. 
			switch (this.ID) {
			case constants.TRKR:
				this.object.setSortOffset(-1000.0f);
				this.object.setName("trkr");
				transparency = getIntSetting("transTrkr");
				visibility   = getBooleanSetting("trkrState");
				break;
				
			case constants.ECAL:
				this.object.setSortOffset(-750.0f);
				this.object.setName("ecal");
				transparency = getIntSetting("transEcal");
				visibility   = getBooleanSetting("ecalState");
				break;
				
			case constants.HCAL:
				this.object.setSortOffset(-500.0f);
				this.object.translate(0f, 1.1f*constants.hcalRadius, 0f);
				this.object.setName("hcal");
				transparency = getIntSetting("transHcal");
				visibility   = getBooleanSetting("hcalState");
				break;
				
			case constants.CRYO:
				this.object.setSortOffset(-250.0f);
				this.object.setName("cryo");
				transparency = getIntSetting("transCryo");
				visibility   = getBooleanSetting("cryoState");
				break;
				
			case constants.YOKE:
				this.object.setSortOffset(-100.0f);
				this.object.translate(0f,25f, 0f);
				this.object.setName("yoke");
				transparency = getIntSetting("transYoke");
				visibility   = getBooleanSetting("yokeState");
				break;
				
			case constants.CAPS:
				this.object.setSortOffset(0f);
				this.object.setName("caps");
				transparency = getIntSetting("transCaps");
				visibility   = getBooleanSetting("capsState");
				break;
				
			case constants.AXES:
				this.object.setSortOffset(-1050f);
				this.object.setCollisionMode(Object3D.COLLISION_CHECK_NONE);
				this.object.setName("axes");
				transparency = getIntSetting("transAxes");
				visibility   = getBooleanSetting("axesState");
				break;
				
			case constants.HITS:
				this.object.setSortOffset(-1250.0f);
				this.object.setCollisionMode(Object3D.COLLISION_CHECK_NONE);
				this.object.setTransparencyMode(Object3D.TRANSPARENCY_MODE_ADD);
				this.object.rotateX((float)(0.5f*Math.PI));
				transparency = constants.MAX_TRANSPARENCY;
				visibility   = false;
				this.object.setName("hits");
			}
			
			this.object.setTransparency(transparency);
			this.object.setVisibility(visibility);			
			
			// And do the build/compilation
			this.object.build();
			
			if ( this.object != null ) {
				if ( this.addedToWorld )
					world.removeObject(this.object);
				world.addObject(this.object);
			}
			
			
		} // End switch (this.ID)

	} // End class subsystem
	
} // End class CMSModel
