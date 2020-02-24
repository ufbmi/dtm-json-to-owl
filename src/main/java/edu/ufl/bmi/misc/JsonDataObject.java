package edu.ufl.bmi.misc;

import java.util.Set;
import java.util.regex.Pattern;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonObject;


public class JsonDataObject extends DataObject {

	boolean cleanFieldValues;
	JsonObject jo;

	public JsonDataObject(String json, String keyName) {
		super(json, keyName);
        jo = JsonParser.parseString(json).getAsJsonObject();
		this.dot = DataObjectType.JSON;
		this.cleanFieldValues = true;
	}

	public JsonDataObject(JsonObject jo, String keyName) {
		super(jo.getAsString(), keyName);
		this.jo = jo;
		this.dot = DataObjectType.JSON;
		this.cleanFieldValues = true;
	}


	@Override
	public String getDataElementValue(String elementName) {
       return getValueForJsonPath(jo, elementName);
	}


    protected static String getValueForJsonPath(JsonObject jo, String path) {
    	if (jo == null || path == null) return "";
        String[] pathElems = path.split(Pattern.quote("."), 2);
        //System.out.println("Path is " + path);
        //System.out.print("Split path is: " + pathElems[0]);
        //if (pathElems.length == 2) System.out.print(", " + pathElems[1]);
        //System.out.println();

        //System.out.println("Getting value for path: " + path);
        //System.out.println("path length is " + pathElems.length);
        if (pathElems.length > 1) {
            //if pathElems[0] has bracket, then need to get array instead of object
            //  and return ith object in array where i is index inside bracket
            JsonObject jo2;
            if (pathElems[0].contains("[")) {
                String[] arrayInfo = pathElems[0].split(Pattern.quote("["), 2);
                String elemName = arrayInfo[0];
                int i = Integer.parseInt(arrayInfo[1].replace("]",""));
                //System.out.println("getting array entry " + i + " in JSON path.");
                //System.out.println("\t\t" + elemName + "\t" + i);
                if (jo.has(elemName))
                	jo2 = jo.get(elemName).getAsJsonArray().get(i).getAsJsonObject();
                else 
                	jo2 = null;
                //System.out.println
            } else {
            	if (jo.has(pathElems[0]))
                	jo2 = jo.get(pathElems[0]).getAsJsonObject();
                else jo2 = null;
            }
            return getValueForJsonPath(jo2, pathElems[1]);
        } else if (pathElems.length == 1) {
            //if pathElems[0] has bracket, then need to get array instead of string
            //  and return ith string in array where i is index inside bracket
            String value = "";
           if (pathElems[0].contains("[")) {
                String[] arrayInfo = pathElems[0].split(Pattern.quote("["), 2);
                String elemName = arrayInfo[0];
                int i = Integer.parseInt(arrayInfo[1].replace("]",""));
                value = jo.get(elemName).getAsJsonArray().get(i).getAsString();
            } else {
            	if (jo.has(pathElems[0])) {
            		JsonElement je = jo.get(pathElems[0]);
            		if (!je.isJsonNull()) {
            			value = (je.isJsonPrimitive()) ? je.getAsString() : je.toString();
            		}
            	}
            }
            return value;
        } else
            return "";
    }


	@Override
	public DataObjectType getDataObjectType() {
		return this.dot;
	}

	@Override 
	public Set<String> getElementKeySet() {
		return jo.keySet();
	}
}