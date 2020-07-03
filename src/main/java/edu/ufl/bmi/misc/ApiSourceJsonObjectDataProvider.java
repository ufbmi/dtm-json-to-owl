package edu.ufl.bmi.misc;

import java.io.IOException;
import java.net.SocketException;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ApiSourceJsonObjectDataProvider extends DataObjectProvider {
	


	String baseUrl;
	String allObjectOfTypeUrl;
	String getObjectByIdUrl;
	String allIdsUrl;

	String keyField;

	String[] allIds;
	JsonArray allObjects;
	int iCurrentObject;

	public ApiSourceJsonObjectDataProvider(String baseUrl, String allIdsUrl, String objectByIdUrl, String keyField) {
		this.baseUrl = baseUrl;
		this.allIdsUrl = allIdsUrl;
		this.getObjectByIdUrl = objectByIdUrl;
		this.keyField = keyField;
		int iCurrentObject = 0;
	}

	public ApiSourceJsonObjectDataProvider(String baseUrl, String allObjectOfTypeUrl, String keyField) {
		this.baseUrl = baseUrl;
		this.allObjectOfTypeUrl = allObjectOfTypeUrl;
		this.keyField = keyField;
	}


	public void initialize() {
		//httpclient = HttpClients.createSystem();
        CloseableHttpClient httpclient = HttpClients.createDefault();
        try {
            httpclient.close();
            httpclient.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        httpclient = HttpClients.createDefault();
        try {
            //HttpGet httpGetIds = new HttpGet(buildPeopleIdsUri(p));
            String getUrl = (allIdsUrl != null) ? this.allIdsUrl : this.allObjectOfTypeUrl;
            HttpGet httpGet = new HttpGet(getUrl);
            httpGet.addHeader("accept", "application/json");

            //System.out.println("Executing request " + httpGet.getRequestLine());
            // Create a custom response handler
            ResponseHandler<String> responseHandler = new ResponseHandler<String>() {

                @Override
                public String handleResponse(
                        final HttpResponse response) throws ClientProtocolException, IOException {
                    int status = response.getStatusLine().getStatusCode();
                    if (status >= 200 && status < 300) {
                        HttpEntity entity = response.getEntity();
                        return entity != null ? EntityUtils.toString(entity) : null;
                    } else {
                        throw new ClientProtocolException("Unexpected response status: " + status);
                    }
                }

            };
            int cTries = 0;
            String responseBody = null;
            Exception shell = null;
            do {
                try {
                    responseBody = httpclient.execute(httpGet, responseHandler);
                } catch (SocketException she) {
                    shell = she;
                    httpclient = HttpClients.createSystem();
                    cTries++;
                } catch (SSLException ssle) {
                    shell = ssle;
                    httpclient = HttpClients.createSystem();
                    cTries++;
                }
            } while (cTries < 10);
            if (responseBody == null && shell != null) throw new RuntimeException("After 10 tries, still didn't get repsonse");
            System.out.println("----------------------------------------");
            //System.out.println(responseBody);
            Gson g = new Gson();
            if (allIdsUrl != null) {
            	allIds = g.fromJson(responseBody, String[].class); 
            } else {
            	allObjects = JsonParser.parseString(responseBody).getAsJsonArray();;
            }
        } catch (IOException ioe) {
        	ioe.printStackTrace();
        } finally {
            try {
            	httpclient.close();
            } catch (IOException ioe) {
            	ioe.printStackTrace();
            }
        }
	}

	@Override
	public boolean isReusable() {
		return true;
	}

	protected DataObject getNextDataObject() {
		JsonDataObject jdo = null;

        if (allIds != null && iCurrentObject == allIds.length) {
        	iCurrentObject = 0;
        	return jdo;
        } else if (allObjects !=null && iCurrentObject == allObjects.size()) {
        	iCurrentObject = 0;
        	return jdo;
        }

		if (allIdsUrl != null) { 

			CloseableHttpClient httpclient = HttpClients.createDefault();
        	try {
            	//HttpGet httpGetIds = new HttpGet(buildPeopleIdsUri(p));
                
            	String getUrl = this.getObjectByIdUrl + "/" + allIds[iCurrentObject];
            	HttpGet httpGet = new HttpGet(getUrl);

            	System.out.println("Executing request " + httpGet.getRequestLine());

            	// Create a custom response handler
            	ResponseHandler<String> responseHandler = new ResponseHandler<String>() {

                	@Override
                	public String handleResponse(
                    	    final HttpResponse response) throws ClientProtocolException, IOException {
                    	int status = response.getStatusLine().getStatusCode();
                    	if (status >= 200 && status < 300) {
                        	HttpEntity entity = response.getEntity();
                        	return entity != null ? EntityUtils.toString(entity) : null;
                    	} else {
                        	throw new ClientProtocolException("Unexpected response status: " + status);
                    	}
                	}

            	};
            	
                int cTries = 0;
                SSLHandshakeException shell = null;
                do {
                    try {
            	       String responseBody = httpclient.execute(httpGet, responseHandler);
            	       jdo = new JsonDataObject(responseBody, this.keyField);
                       break;
                    } catch (SSLHandshakeException she) {
                        httpclient = HttpClients.createDefault();
                        shell = she;
                        cTries++;
                    }
                } while (cTries < 10);

                if (jdo == null && shell != null) throw new RuntimeException("After 10 attempts stilll got a SSLHandshakeException.");
			} catch (IOException ioe) {
        		ioe.printStackTrace();
        	} finally {
            	try {
            		httpclient.close();
            	} catch (IOException ioe) {
            		ioe.printStackTrace();
            	}
        	}
        } else {
        	jdo = new JsonDataObject(allObjects.get(iCurrentObject).getAsJsonObject(), this.keyField);
        }
        iCurrentObject++;

		return jdo;
	}

}