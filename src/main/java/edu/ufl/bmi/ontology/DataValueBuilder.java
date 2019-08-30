package edu.ufl.bmi.ontology;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;


public class DataValueBuilder {
	ArrayList dataValueInstructions;
	HashMap<String, Integer> fieldNameToIndex;
	String dataType;

	public DataValueBuilder(String dataValueInstruction, String dataType, HashMap<String, Integer> fieldNameToIndex) {
		this.fieldNameToIndex = fieldNameToIndex;
		this.dataType = dataType;
		parseDataValueInstruction(dataValueInstruction);
	}

	public Object buildDataValue(ArrayList<String> recordFields) {
		StringBuilder sb = new StringBuilder();
		for (Object o : dataValueInstructions) {
			if (o instanceof String) {
				sb.append((String)o);
			} else if (o instanceof Integer) {
				sb.append(recordFields.get((Integer)o));
			}
		}
		String objectAsString = sb.toString();
		if (dataType.equals("String")) {
			return objectAsString;
		} else if (dataType.equals("float")) {
			return Float.parseFloat(objectAsString);
		} else if (dataType.equals("int")) {
			return Integer.parseInt(objectAsString);
		} else if (dataType.equals("boolean")) {
			return Boolean.parseBoolean(objectAsString);
		} else {
			return objectAsString;
		}
	}

	public String getDataType() {
		return dataType;
	}

	public void parseDataValueInstruction(String dataValueInstruction) {
		/*
			Make a list.  If list item is String, then that String goes into value
			as a literal.  If list item is Integer, then we lookup that field value.
		*/
		dataValueInstructions = new ArrayList();
		String[] flds = dataValueInstruction.split(Pattern.quote("+"));
		for (String fld : flds) {
			fld = fld.trim();
			if (fld.startsWith("[") && fld.endsWith("]")) {
				String varName = fld.substring(1, fld.length()-1);
				dataValueInstructions.add(fieldNameToIndex.get(varName));
			} else {
				dataValueInstructions.add(fld);
			}
		}
	}
}