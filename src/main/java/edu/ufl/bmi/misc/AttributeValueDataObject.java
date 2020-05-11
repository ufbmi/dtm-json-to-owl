package edu.ufl.bmi.misc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import edu.ufl.bmi.misc.DataObject;

public class AttributeValueDataObject extends DataObject {

	HashMap<String, ArrayList<String>> attributesAndValues;

	public AttributeValueDataObject(String keyName) {
		super("", keyName);
		this.attributesAndValues = new HashMap<String, ArrayList<String>>();
	}

	public void addAttributeWithValues(String attribute, String[] values) {
		ArrayList<String> valueList = new ArrayList<String>();
		for (String value : values) valueList.add(value);

		if (!this.attributesAndValues.containsKey(attribute)) {
			this.attributesAndValues.put(attribute, valueList);
		} else {
			this.attributesAndValues.get(attribute).addAll(valueList);
		}
	}

	public void addAttributeValue(String attribute, String value) {
		if (this.attributesAndValues.containsKey(attribute)) {
			this.attributesAndValues.get(attribute).add(value);
		} else {
			ArrayList<String> values = new ArrayList<String>();
			values.add(value);
			this.attributesAndValues.put(attribute, values);
		}
	}

	public void addAttributeValueAtPosition(String attribute, String value, int position) {
		ArrayList<String> values = (this.attributesAndValues.containsKey(attribute)) ?
			this.attributesAndValues.get(attribute) : new ArrayList<String>();

			values.add(value);
			if (position != values.size()) {
				System.err.println("position="+position+", but value is at " + values.size());
				System.err.println("\tfor attribute="+attribute + " and value=" + value);
			}

		if (!this.attributesAndValues.containsKey(attribute)) {
			this.attributesAndValues.put(attribute, values);
		}
		
	}

	@Override
	public  String getDataElementValue(String elementName) {
		if (this.attributesAndValues.containsKey(elementName)) {
			return this.attributesAndValues.get(elementName).iterator().next();
		} else
			return null;
	}
	
	@Override
	public Set<String> getElementKeySet() {
		return this.attributesAndValues.keySet();
	}

	@Override
	public DataObjectType getDataObjectType() {
		return DataObjectType.ATTRIBUTE_VALUE;
	}

	@Override
	public String[] getValuesForElement(String elementName) {
		String[] values = null;
		if (this.attributesAndValues.containsKey(elementName)) {
			ArrayList<String> valuesList = this.attributesAndValues.get(elementName);
			values = (String[])valuesList.toArray(new String[valuesList.size()]);
		}
		return values;
	}

	@Override
	public DataObject[] getValuesAsDataObjectsForElement(String elementName) {
		String[] values = null;
		if (this.attributesAndValues.containsKey(elementName)) {
			ArrayList<String> valuesList = this.attributesAndValues.get(elementName);
			values = (String[])valuesList.toArray(new String[valuesList.size()]);
		}
		//System.err.println("Splitting values of " + elementName + " into " + values.length + " data objects.");
		AttributeValueDataObject[] dos = null;
		if (values != null) {
		 	dos = new AttributeValueDataObject[values.length];
		 	int i=0;
		 	for (String value : values) {
		 		dos[i] = new AttributeValueDataObject(this.keyName);
		 		dos[i].addAttributeValue(keyName, this.getDataElementValue(keyName));
		 		//System.err.println("\tAdding "  + elementName + "=" + value);
		 		dos[i].addAttributeValue(elementName, value);
		 		i++;
		 	}
		 }
		 return dos;
	}
}