package edu.ufl.bmi.ontology;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;

import edu.ufl.bmi.misc.DataObject;

public class DataValueBuilder {
	ArrayList dataValueInstructions;

	ArrayList<String> dataValueComponent;
	ArrayList<Boolean> isLiteralValue;

	HashMap<String, Integer> fieldNameToIndex;
	String dataType;

	public DataValueBuilder(String dataValueInstruction, String dataType, HashMap<String, Integer> fieldNameToIndex) {
		this.fieldNameToIndex = fieldNameToIndex;
		this.dataType = dataType;
		parseDataValueInstruction(dataValueInstruction);
	}

	public Object buildDataValue(ArrayList<String> recordFields) {
		/*
		StringBuilder sb = new StringBuilder();
		for (Object o : dataValueInstructions) {
			if (o instanceof String) {
				String s = (String)o;
				if (s.trim().length() == 0) return null;
				sb.append(s);
			} else if (o instanceof Integer) {
				sb.append(recordFields.get((Integer)o));
			}
		}
		String objectAsString = sb.toString();
		*/

		StringBuilder sb2 = new StringBuilder();
		int size = dataValueComponent.size();
		for (int i=0; i<size; i++) {
			String s = dataValueComponent.get(i);
			if (isLiteralValue.get(i)) {
				sb2.append(s);
			} else {
				String value = recordFields.get(fieldNameToIndex.get(s));
				if (value.trim().length()>0)
					sb2.append(value);
			}
		}
		String objectAsString=sb2.toString();

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

	public Object buildDataValue(DataObject dataObject) {
		StringBuilder sb2 = new StringBuilder();
		int size = dataValueComponent.size();
		for (int i=0; i<size; i++) {
			String s = dataValueComponent.get(i);
			if (isLiteralValue.get(i)) {
				sb2.append(s);
			} else {
				String value = dataObject.getDataElementValue(s);
				if (value.trim().length()>0)
					sb2.append(value);
			}
		}
		String objectAsString=sb2.toString();

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
		dataValueComponent = new ArrayList<String>();
		isLiteralValue = new ArrayList<Boolean>();
		String[] flds = dataValueInstruction.split(Pattern.quote("+"));
		for (String fld : flds) {
			fld = fld.trim();
			if (fld.startsWith("[") && fld.endsWith("]")) {
				String varName = fld.substring(1, fld.length()-1);
				dataValueInstructions.add(fieldNameToIndex.get(varName));
				dataValueComponent.add(varName);
				isLiteralValue.add(false);
			} else {
				dataValueInstructions.add(fld);
				dataValueComponent.add(fld);
				isLiteralValue.add(true);
			}
		}
	}
}