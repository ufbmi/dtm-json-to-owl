package edu.ufl.bmi.misc;

import java.io.FileReader;
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
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class LocalFileSourceJsonObjectDataProvider extends DataObjectProvider {
	
	String filePathAndName;

	String keyField;

	String[] allIds;
	JsonArray allObjects;
	int iCurrentObject;

	public LocalFileSourceJsonObjectDataProvider(String filePathAndName, String keyField) {
        this.filePathAndName = filePathAndName;
		this.keyField = keyField;
		int iCurrentObject = 0;
	}

	public void initialize() {
		
        FileReader fr = null;
        try {
            System.out.println("file name is " + this.filePathAndName);
            fr = new FileReader(this.filePathAndName);
            JsonParser jp = new JsonParser();
            JsonElement jeTop = jp.parse(fr);
            if (jeTop.isJsonArray()) {
                allObjects = jeTop.getAsJsonArray();
            } else {
                System.err.println("Expected Json Array at top level of file.  Not initialized.");
            }
            fr.close();
        } catch (IOException ioe) {
        	ioe.printStackTrace();
        } /*finally {
            try {
                fr.close(); 	
            } catch (IOException ioe) {
            	ioe.printStackTrace();
            }
        }*/
	}

	@Override
	public boolean isReusable() {
		return true;
	}

	protected DataObject getNextDataObject() {
        if (iCurrentObject == allObjects.size()) { iCurrentObject = 0; return null; }
		JsonDataObject jdo = new JsonDataObject(allObjects.get(iCurrentObject).getAsJsonObject(), keyField);
        iCurrentObject++;
		return jdo;
	}

}