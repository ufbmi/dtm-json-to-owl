package edu.ufl.bmi.misc;

public abstract class DataObject {
	String rawData;
	DataObjectType dot;

	public DataObject(String rawData) {
		this.rawData = rawData;
	}

	public abstract String getDataElementValue(String elementName);

	public abstract DataObjectType getDataObjectType();

}