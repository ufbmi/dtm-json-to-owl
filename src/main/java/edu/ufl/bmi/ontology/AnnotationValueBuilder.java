package edu.ufl.bmi.ontology;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;

import edu.ufl.bmi.misc.DataObject;


public class AnnotationValueBuilder {

	ArrayList<String> annotationValueComponent;
	ArrayList<Boolean> isLiteralValue;
	ArrayList<Boolean> isFromParent;

	public AnnotationValueBuilder(String annotationInstruction) {
		parseAnnotationValueInstruction(annotationInstruction);
	}

/*
	public AnnotationValueBuilder(String annotationInstruction, HashMap<String, Integer> fieldNameToIndex) {
		this.fieldNameToIndex = fieldNameToIndex;
		parseAnnotationValueInstruction(annotationInstruction);
	}
	*/

/*
	public String buildAnnotationValue(ArrayList<String> recordFields) {
		StringBuilder sb2 = new StringBuilder();
		int size = annotationValueComponent.size();
		for (int i=0; i<size; i++) {
			String s = annotationValueComponent.get(i);
			if (isLiteralValue.get(i)) {
				sb2.append(s);
			} else {
				String value = recordFields.get(fieldNameToIndex.get(s));
				if (value.trim().length()>0)
					sb2.append(value);
			}
		}

		String annotationValue2=sb2.toString();
		if (annotationValue2.trim().length() == 0) annotationValue2 = null;
		return annotationValue2 ;
	}
	*/

	public String buildAnnotationValue(DataObject dataObject, DataObject parentObject) {
		StringBuilder sb2 = new StringBuilder();
		int size = annotationValueComponent.size();
		for (int i=0; i<size; i++) {
			String s = annotationValueComponent.get(i);
			if (isLiteralValue.get(i)) {
				sb2.append(s);
			} else {
				//System.err.println("Getting value of " + s + " element to build annotation.");
				String value = (isFromParent.get(i)) ? parentObject.getDataElementValue(s) : dataObject.getDataElementValue(s);
				if (value.trim().length()>0)
					sb2.append(value);
			}
		}

		String annotationValue2=sb2.toString().replace("  "," ");
		if (annotationValue2.trim().length() == 0) annotationValue2 = null;
		return annotationValue2 ;
	}


	public void parseAnnotationValueInstruction(String annotationValueInstruction) {
		/*
			Make a list.  If list item is String, then that String goes into value
			as a literal.  If list item is Integer, then we lookup that field value.
		*/
		annotationValueComponent = new ArrayList<String>();
		isLiteralValue = new ArrayList<Boolean>();
		isFromParent = new ArrayList<Boolean>();

		String[] flds = annotationValueInstruction.split(Pattern.quote("+"));
		for (String fld : flds) {
			fld = fld.trim();
			
			if (fld.startsWith("[") && fld.endsWith("]")) {
				String varName = fld.substring(1, fld.length()-1);
				annotationValueComponent.add(varName);
				isLiteralValue.add(false);
				isFromParent.add(false);
			} else if (fld.startsWith("p.[") && fld.endsWith("]")) {
				String varName = fld.substring(3, fld.length()-1);
				annotationValueComponent.add(varName);
				isLiteralValue.add(false);
				isFromParent.add(true);
			} else {
				annotationValueComponent.add(fld.replace("\"",""));
				isLiteralValue.add(true);
				isFromParent.add(false);
			}
		}
	}
}