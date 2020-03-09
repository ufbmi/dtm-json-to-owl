package edu.ufl.bmi.misc;

import java.util.Set;

public abstract class DataObject {
	String rawData;
	DataObjectType dot;
	String keyName;

	public DataObject(String rawData, String keyName) {
		this.rawData = rawData;
		this.keyName = keyName;
	}

	public String getKeyName() {
		return keyName;
	}

	public Object getValueForKey() {
		return this.getDataElementValue(this.keyName);
	}

	public abstract String getDataElementValue(String elementName);
	public abstract Set<String> getElementKeySet();
	public abstract DataObjectType getDataObjectType();

	public abstract String[] getValuesForElement(String elementName);
	public abstract DataObject[] getValuesAsDataObjectsForElement(String elementName);	
}