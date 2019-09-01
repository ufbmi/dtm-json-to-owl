package edu.ufl.bmi.ontology;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;


public class AnnotationValueBuilder {
	ArrayList annotationValueInstructions;
	HashMap<String, Integer> fieldNameToIndex;

	public AnnotationValueBuilder(String annotationInstruction, HashMap<String, Integer> fieldNameToIndex) {
		this.fieldNameToIndex = fieldNameToIndex;
		parseAnnotationValueInstruction(annotationInstruction);
	}

	public String buildAnnotationValue(ArrayList<String> recordFields) {
		StringBuilder sb = new StringBuilder();
		for (Object o : annotationValueInstructions) {
			if (o instanceof String) {
				sb.append((String)o);
			} else if (o instanceof Integer) {
				String fieldValue = recordFields.get((Integer)o);
				if (fieldValue.trim().length() > 0)
					sb.append(fieldValue);
				else 
					return null;
			}
		}
		return sb.toString();
	}


	public void parseAnnotationValueInstruction(String annotationValueInstruction) {
		/*
			Make a list.  If list item is String, then that String goes into value
			as a literal.  If list item is Integer, then we lookup that field value.
		*/
		annotationValueInstructions = new ArrayList();
		String[] flds = annotationValueInstruction.split(Pattern.quote("+"));
		for (String fld : flds) {
			fld = fld.trim();
			if (fld.startsWith("[") && fld.endsWith("]")) {
				String varName = fld.substring(1, fld.length()-1);
				annotationValueInstructions.add(fieldNameToIndex.get(varName));
			} else {
				annotationValueInstructions.add(fld.replace("\"",""));
			}
		}
	}
}