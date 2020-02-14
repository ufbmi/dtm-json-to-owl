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
        String[] pathElems = path.split(Pattern.quote("."), 2);
        if (pathElems.length > 1) {
            //if pathElems[0] has bracket, then need to get array instead of object
            //  and return ith object in array where i is index inside bracket
            JsonObject jo2;
            if (pathElems[0].contains("[")) {
                String[] arrayInfo = pathElems[0].split(Pattern.quote("["), 2);
                String elemName = arrayInfo[0];
                int i = Integer.parseInt(arrayInfo[1].replace("]",""));
                //System.out.println("\t\t" + elemName + "\t" + i);
                jo2 = jo.get(elemName).getAsJsonArray().get(i).getAsJsonObject();
                //System.out.println
            } else {
                jo2 = jo.get(pathElems[0]).getAsJsonObject();
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
                value = jo.get(pathElems[0]).getAsString();
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