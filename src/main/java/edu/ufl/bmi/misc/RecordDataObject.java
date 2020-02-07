package edu.ufl.bmi.misc;

import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;


public class RecordDataObject extends DataObject {
	HashMap<String, Integer> fieldNameToIndex;
	public static String DEFAULT_FIELD_DELIMITER = "\t";
	String[] fields;
	String delimiter;
	boolean cleanFieldValues;

	public RecordDataObject(List<String> orderedFieldNameList, String rawData) {
		super(rawData);
		this.dot = DataObjectType.TABLE_RECORD;

		//default settings
		this.delimiter = DEFAULT_FIELD_DELIMITER;
		this.cleanFieldValues = true;

		setupFieldIndex(orderedFieldNameList);
		splitRecordIntoFields();
	}

	public RecordDataObject(List<String> orderedFieldNameList, String rawData, String delimiter, boolean cleanupFields) {
		super(rawData);
		this.dot = DataObjectType.TABLE_RECORD;

		this.delimiter = delimiter;
		this.cleanFieldValues = cleanupFields;

		setupFieldIndex(orderedFieldNameList);
		splitRecordIntoFields();
	}

	protected void setupFieldIndex(List<String> orderedFieldNameList) {
		int size = orderedFieldNameList.size();
		fieldNameToIndex = new HashMap<String, Integer>();
		for (int i=0; i<size; i++) {
			String fieldName = orderedFieldNameList.get(i);
			fieldNameToIndex.put(fieldName, new Integer(i));
		}
	}

	protected void splitRecordIntoFields() {
		fields = rawData.split(Pattern.quote(delimiter), -1);
		if (fields.length != fieldNameToIndex.size())
			throw new IllegalArgumentException("Number of fields in data does not match number of fields in schema.");
		if (this.cleanFieldValues) {
			for (int i=0; i<fields.length; i++) {
				fields[i] = cleanupValue(fields[i]);
			}
		}
	}

	public String cleanupValue(String fieldValue) {
		return fieldValue.replace("\"","").replace("N/A","").replace("n/a","").trim();
	}

	@Override
	public String getDataElementValue(String elementName) {
		if (!fieldNameToIndex.containsKey(elementName))
			throw new IllegalArgumentException("No such field " + elementName + " in list of fields.");
		return fields[fieldNameToIndex.get(elementName)];
	}

	@Override
	public DataObjectType getDataObjectType() {
		return this.dot;
	}
}