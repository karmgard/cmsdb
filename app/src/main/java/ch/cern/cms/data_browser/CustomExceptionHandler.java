package ch.cern.cms.data_browser;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.Thread.UncaughtExceptionHandler;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

import android.os.AsyncTask;

public class CustomExceptionHandler implements UncaughtExceptionHandler {

    private UncaughtExceptionHandler defaultUEH;
    private String localPath;
    private String fileName;
    private String url;
    private Logger Log;

    // if any of the parameters is null, the respective functionality will not be used
    public CustomExceptionHandler(String localPath, String url) {
        this.localPath = localPath;
        this.url = url;
        this.Log = new Logger();
        this.defaultUEH = Thread.getDefaultUncaughtExceptionHandler();
    }

    public void uncaughtException(Thread t, Throwable e) {
    	SimpleDateFormat s = new SimpleDateFormat("ddMMyyyyhhmmss", Locale.US);
    	String timestamp = s.format(new Date());
    			    			
        final Writer result = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(result);
        e.printStackTrace(printWriter);
        String stacktrace = result.toString();
        printWriter.close();
        fileName = timestamp + ".stacktrace";

        if (localPath != null) {
            writeToFile(stacktrace, fileName);
        }
        if (url != null) {
        	postLogToServer task = new postLogToServer();
        	task.execute(stacktrace, fileName);
        }

        defaultUEH.uncaughtException(t, e);
    }

    private void writeToFile(String stacktrace, String filename) {
        try {
            BufferedWriter bos = new BufferedWriter(new FileWriter(
                    localPath + "/" + filename));
            bos.write(stacktrace);
            bos.flush();
            bos.close();
        } catch (Exception e) {
            Log.e("CustomExceptionHandler", e.toString());
        }
    }
    
	private class postLogToServer extends AsyncTask<String, Void, Void> {

		@Override
		protected Void doInBackground(String... strings) {
			
			String stacktrace = strings[0];
			String filename = strings[1];
			
	        DefaultHttpClient httpClient = new DefaultHttpClient();
	        HttpPost httpPost = new HttpPost(url);
	        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
	        nvps.add(new BasicNameValuePair("filename", filename));
	        nvps.add(new BasicNameValuePair("stacktrace", stacktrace));
	        try {
	            httpPost.setEntity(
	                    new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
	            httpClient.execute(httpPost);
	        } catch (IOException e) {
	            Log.e("postLogToServer()",e.toString());
	        }
			return null;
		}
			
		@Override
		protected void onPostExecute(Void nothing) {
			File f = new File (localPath, fileName);
			if ( !f.delete() )
				Log.e("CustomExceptionHandler", "Unable to delete "+localPath+"/"+fileName);
			
			return;
		}
	}
}
