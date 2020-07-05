package edu.ufl.bmi.misc;

import java.util.ArrayList;
import java.util.List;
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
		super(jo.toString(), keyName);
		this.jo = jo;
		this.dot = DataObjectType.JSON;
		this.cleanFieldValues = true;
	}


	@Override
	public String getDataElementValue(String elementName) {
       return getValueForJsonPath(jo, elementName);
	}


    protected static String getValueForJsonPath(JsonObject jo, String path) {
        //System.err.println("path="+path);
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
                System.out.println("For pathElems.length == 1, arrayInfo[0]="+arrayInfo[0] + ", and arrayInfo[1]=" + arrayInfo[1]);
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

    @Override
    public String[] getValuesForElement(String elementName) {
        ArrayList<String> values = new ArrayList<String>();
        getValuesForJsonPath(jo, elementName, values);
        return values.toArray(new String[values.size()]);
    }

    protected static void getValuesForJsonPath(JsonObject jo, String path, List<String> values) {
        if (jo == null || path == null) return;
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
                String indexTxt = arrayInfo[1].replace("]","");
                
                //System.out.println("getting array entry " + i + " in JSON path.");
                //System.out.println("\t\t" + elemName + "\t" + i);
                if (jo.has(elemName)) {
                    if (indexTxt != null && !indexTxt.isEmpty()) {
                        int i = Integer.parseInt(arrayInfo[1].replace("]",""));
                        jo2 = jo.get(elemName).getAsJsonArray().get(i).getAsJsonObject();
                        getValuesForJsonPath(jo2, pathElems[1], values);
                    } else {
                        JsonArray ja = jo.get(elemName).getAsJsonArray();
                        for (JsonElement je : ja) {
                            if (je.isJsonPrimitive()) {
                                values.add(je.getAsString());
                            } else if (je.isJsonObject()) {
                                jo2 = je.getAsJsonObject();
                                getValuesForJsonPath(jo2, pathElems[1], values);
                            }
                        }
                    }
                } else 
                    jo2 = null;
                //System.out.println
            } else {
                if (jo.has(pathElems[0])) {
                    jo2 = jo.get(pathElems[0]).getAsJsonObject();
                    getValuesForJsonPath(jo2, pathElems[1], values);
                }
                else jo2 = null;
            }
        } else if (pathElems.length == 1) {
            //if pathElems[0] has bracket, then need to get array instead of string
            //  and return ith string in array where i is index inside bracket
            String value = "";
            pathElems[0] = pathElems[0].trim();
            if (pathElems[0].contains("[")) {
                //System.out.println("pathElems[0]="+pathElems[9]);
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
            values.add(value);
        } else
            return;
    }

    @Override
    public DataObject[] getValuesAsDataObjectsForElement(String elementName) {
        //System.err.println("getting possibly multiple values for element="+elementName);
    	
        String[] pathSteps = elementName.split(Pattern.quote("."), -1);
        JsonObject joNew = jo;
        for (int i=0; i<pathSteps.length-1; i++) {
            joNew = joNew.get(pathSteps[i]).getAsJsonObject();
        }
        JsonElement je = joNew.get(pathSteps[pathSteps.length-1]);
        //System.out.println("Getting array of " + elementName);
        //System.out.println("\t"+ je);
    	JsonDataObject[] jdos = null;
    	if (je.isJsonArray()) {
            //System.out.print("The value of " + elementName + " is an array...");
    		JsonArray ja = je.getAsJsonArray();
            //System.out.println("of size " + ja.size());
    		jdos = new JsonDataObject[ja.size()];
    		int i=0;
    		for (JsonElement jei : ja) {
    			JsonObject joLocal = null;
    			if (jei.isJsonPrimitive()) {
    				joLocal = new JsonObject();
    				joLocal.addProperty(elementName, jei.getAsString());
    			} else if (jei.isJsonObject()) {
    				joLocal = jei.getAsJsonObject();
    			} 
                //System.out.println(joLocal.toString());
    			jdos[i++] = new JsonDataObject(joLocal, "id");
    		}
    		return jdos;
    	} else if (je.isJsonObject()) {
    		JsonObject joLocal = je.getAsJsonObject();
    		jdos = new JsonDataObject[1];
    		jdos[0] = new JsonDataObject(joLocal, "id");
    		return jdos;
    	} else if (je.isJsonPrimitive()) {
    		jdos = new JsonDataObject[1];
    		jdos[0] = this;
 			return jdos;
    	} else {
    		return jdos;
    	}
    }
}