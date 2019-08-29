package ch.cern.cms.data_browser.getData;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import ch.cern.cms.data_browser.Logger;
import ch.cern.cms.data_browser.R;
import ch.cern.cms.data_browser.Settings;

public class dataList {

	fillSpinner mCallback;
	private boolean eventIsLocal = false;
	
	Context mContext;
	
	public interface fillSpinner {
		void fill(List<String> entries, int which);
	}

	private String data_prompt, run_prompt, event_prompt, simulate_prompt, channel_prompt;
	private boolean hasInternet = false;
	private publicDataDB cmsData = null;
	private Settings settings = null;
	private Logger Log = null;

	public dataList (Context context, Fragment frag, Activity activity) {
		
		this.settings = Settings.getInstance();
		this.Log = Logger.getInstance();
		
		if ( settings.getIntSetting("debugLevel") > 1 ) Log.w("getList()", "Instantiating");

		mContext = context;

		// Use the getString call while we know we're attached
		data_prompt  = mContext.getString(R.string.data_prompt);
		run_prompt   = mContext.getString(R.string.run_prompt);
		event_prompt = mContext.getString(R.string.event_prompt);

		simulate_prompt = mContext.getString(R.string.simulate_prompt);
		channel_prompt = mContext.getString(R.string.channel_prompt);

        try {
            mCallback = (fillSpinner) frag;
        } catch (ClassCastException e) {
            throw new ClassCastException(frag.toString()
                    + " must implement fillSpinner");
        }
		if ( cmsData == null )
			cmsData = publicDataDB.getInstance(mContext);
        
		return;
	}

	/************* Utility functions into the local DB routines **************/
	public List<String> getLocalRunList() {
		return cmsData.getRunList();
	}
	
	public List<String> getLocalSimList() {
		return cmsData.getSimList();
	}
	
	public List<String> getLocalEventList(String runNumber) {
		return cmsData.getEventList(runNumber);
	}
	
	public boolean hasLocal(boolean isData) {
		return cmsData.hasLocal(isData);
	}
	
	public void setNetworkState( boolean netState ) {
		hasInternet = netState;
		if (settings.getIntSetting("debugLevel") > 1) Log.w("setNetworkState()", "hasInternet = "+hasInternet);
		return;
	}
	
	public void checkFile(String fileName) {
		if ( settings.getIntSetting("debugLevel") > 1 ) Log.w("getList().getData()", "Checking md5sum");
		
    	getDataList task = new getDataList();
    	task.execute("md5", "getMD5.pl", "file", "ser/"+fileName);

    	return;
	}
	
	public void setSimIndex( int index ) {
		cmsData.setSimIndex(index);
		return;
	}

	public void addSimToStore(String ... simValues) {
		cmsData.addSimToStore(simValues);
		return;
	}
	
	public void addEventToStore(String...values) {
		cmsData.addEventToStore(values);
		return;
	}
	
	public void deleteStoredEvent() {
		cmsData.deleteStoredEvent();
		return;
	}
	
	public void deleteStoredSim() {
		cmsData.deleteStoredSim();
		return;
	}

	public void clearDB(boolean isData) {
		cmsData.clearDB(isData);
	}
	
	public void getData(String... data) {
		if ( hasInternet || cmsData.hasLocal(true) ) {
			getDataList task = new getDataList();
			task.execute(data);
		}
		
    	return;
	}
	
	public List<String> getDataList( String filter ) {
		return cmsData.getDataList(filter);
	}
	
	public void getSimulation(String... data) {

		if ( hasInternet || cmsData.hasLocal(false) ) {
			getDataList task = new getDataList();
			task.execute(data);
		}
		return;
	}
	
	public void restore(boolean isLocal) {
		eventIsLocal = isLocal;
		return;
	}
	
	public boolean isLocal() {
		return eventIsLocal;
	}
	
	private class getDataList extends AsyncTask<String, Void, List<String>> {
	 
		private String tag = null;
		private boolean isData = true;
		
		@Override
		protected List<String> doInBackground(String... urls) {
			
			if ( settings.getIntSetting("debugLevel") > 1 ) {
				for ( int i=0; i<urls.length; i++ )
					Log.w("doInBackground()", "urls["+i+"] = "+urls[i]);
			}
			
			// eventIsLocal is false by default.
			eventIsLocal = false;
			
			if ( urls[0].equalsIgnoreCase("data") ) {
				eventIsLocal = (urls != null) && 
						(urls.length >=6)  &&
						(urls[5].length() > 12) && 
						(urls[5].substring(0, 12).equalsIgnoreCase("type='Local'"));
				isData = true;
			} else if ( urls[0].equalsIgnoreCase("simulation") ) {
				eventIsLocal = urls != null && urls.length >= 4 && urls[3].equalsIgnoreCase("Local");
				isData = false;
			}
			
			if ( eventIsLocal ) {
				tag = urls[0];
				String filter;
				if ( isData )
					filter = urls[5].substring(17);
				else
					filter = "isData=0 and type='"+urls[5]+"'";
				List<String> result = cmsData.getDataList(filter);
				return result;
			}
			
			String server = settings.getStringSetting("server");
			if ( server.equalsIgnoreCase("none") ) {
				if (settings.getIntSetting("debugLevel") > 1) Log.w("doInBackground()", "No server yet.");

				tag = urls[0];
				return null;
			}
		 
			if ( !hasInternet ) {
				Log.e("getList().doInBackground()", "No network available!");
				if ( urls[0] != null ) {
					tag = urls[0];
					if ( tag.equals(mContext.getString(R.string.simulate_prompt)) || tag.equals(mContext.getString(R.string.channel_prompt)) )
						isData = false;
				} else
					return null;

				return new ArrayList<String>();
			}
			
			HttpClient httpclient = new DefaultHttpClient();
			HttpPost httppost = null;
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
			List<String> result = null;
		 
			// Do we know what we're doing?
			if ( urls[0] != null ) {
				tag = urls[0];
				if ( tag.equals(mContext.getString(R.string.simulate_prompt)) || tag.equals(mContext.getString(R.string.channel_prompt)) )
					isData = false;
			} else
				return null;
		 
			// Do we know where we're going?
			if ( urls.length >= 2 ) {
				httppost = new HttpPost(server + "/" + urls[1]);
			 
				// Do we know what we're doing when we get there?
				if ( urls.length >= 4 ) { 
					nameValuePairs.add(new BasicNameValuePair(urls[2], urls[3]));
				}
			 
				// Anything else?
				if ( urls.length >= 6 ) {
					nameValuePairs.add(new BasicNameValuePair(urls[4], urls[5]));
				}

				try {
					httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
				} catch ( Exception e ) {
					Log.e("Event", "Unable to set UrlEncoding");
				}
			}
		 
			if ( httppost != null ) {
				if (settings.getIntSetting("debugLevel") > 1) Log.w("Event", "Executing Http POST");
				try {
					HttpResponse response = httpclient.execute(httppost);
					result = inputStreamToList(response.getEntity().getContent());
					
				} catch ( Exception e ) {
					Log.e("Event", "httpclient.execute() "+e.toString());
					result = null;
				}
			} else
				Log.e("doInBackground()", "Got null result from httppost!");

			return result;
		}

		@Override
		protected void onPostExecute(List<String> result) {
			
			if ( result != null ) {
				
				try {
					//if (settings.getIntSetting("debugLevel") > 1) Log.w("Event", "Server says... " + tag + " = " + result);
		 
					if ( mCallback == null )
						Log.e("onPostExecute()", "Oh-oh. mCallback is null");
					
					else if ( tag.equals(data_prompt) || tag.equals(simulate_prompt) ) {
						
						// If there are data or simulations stored in the DB on the device, add Local to the list
						if ( cmsData.hasLocal(isData) )
							result.add("Local");

						// Fill the spinner in the explore/simulate fragment
						mCallback.fill(result, 0);
						
					} else if ( tag.equals(run_prompt) || tag.equals(channel_prompt) )
						mCallback.fill(result, 1);

					else if ( tag.equals(event_prompt) )
						mCallback.fill(result, 2);

					else if ( tag.equals("data") || tag.equals("simulation") ) {

						if ( tag.equalsIgnoreCase("data") ) {
							settings.putBoolean("isData", true);
							settings.putBoolean("isSim", false);
							
						} else if ( tag.equalsIgnoreCase("simulation") ) {
							settings.putBoolean("isData", false);
							settings.putBoolean("isSim", true);
						}
						result.remove(0);
						
						boolean success = result.get(0).equalsIgnoreCase("success");
						result.remove(0);
						
						if ( success ) {

							// Let Main know that we've got the data
							Intent intent = new Intent("fragmentMsg");
					    		
							if ( tag.equalsIgnoreCase("data") || tag.equalsIgnoreCase("simulation") ) {
								
								if ( tag.equalsIgnoreCase("data") )
					    			intent.putExtra("widgetName", "getEventData");
					    		else if ( tag.equalsIgnoreCase("simulation") )
					    			intent.putExtra("widgetName", "getSimData");
					    		
					    		intent.putStringArrayListExtra("result", (ArrayList<String>)result);
					    		LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
							
							} else
								Log.e("onPostExecute()", "Don't know if it's real or memorex!");
					    		
						} else
							Log.e("onPostExecute()", "Server says event retreival failed!");
						
					} else {
						Log.e("onPostExecute()", "Not really sure what's going on");
					}
				} catch (Exception e) {
					Log.e("getList().onPostExecute(result)",e.toString());
					settings.putBoolean("isData", false);
					settings.putBoolean("isSim", false);
				}
			 
			} else {
				if ( cmsData.hasLocal(isData) && tag.equals(data_prompt) ) {
					result = new ArrayList<String>();
					result.add("Local");
					settings.putBoolean("isData", true);
					mCallback.fill(result, 0);
				} else if ( cmsData.hasLocal(false) && tag.equals(simulate_prompt) ) {
					result = new ArrayList<String>();
					result.add("Local");
					settings.putBoolean("isData", false);
					mCallback.fill(result, 0);
				} else {
					settings.putBoolean("isData", false);
					settings.putBoolean("isSim", false);
				}
			}
			return;
		}

		private List<String> inputStreamToList(InputStream is) {

			String line = "";
			List<String> list = new ArrayList<String>();

			// Wrap a BufferedReader around the InputStream
			BufferedReader rd = new BufferedReader(new InputStreamReader(is));
	    
			// Read response until the end
			try {
				while ((line = rd.readLine()) != null) {
					try {
						list.add(line);

					} catch (Exception e) {}
				}

				if ( list.size() > 1 )
					list.add(0,tag);
			 
			} catch (Exception e) {
				Log.e("inputStreamToList()", e.toString());
			}
	     
			// Return full list
			return list;
		} // End inputStreamToString
	 
	} // End private class getDataLists

}