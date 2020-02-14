package edu.ufl.bmi.misc;

import java.io.IOException;

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
		CloseableHttpClient httpclient = HttpClients.createDefault();
        try {
            //HttpGet httpGetIds = new HttpGet(buildPeopleIdsUri(p));
            String getUrl = (allIdsUrl != null) ? this.allIdsUrl : this.allObjectOfTypeUrl;
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
            String responseBody = httpclient.execute(httpGet, responseHandler);
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
            	
            	String responseBody = httpclient.execute(httpGet, responseHandler);
            	jdo = new JsonDataObject(responseBody, this.keyField);
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