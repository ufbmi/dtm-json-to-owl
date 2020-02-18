package edu.ufl.bmi.ontology;

import java.util.HashMap;

public class RdfConversionInstructionSetExecutor {
	
	HashMap<String, RdfConversionInstructionSet> instructionsByElementName;

	public RdfConversionInstructionSetExecutor() {
		instructionsByElementName = new HashMap<String, RdfConversionInstructionSet>();
	}

	public boolean addInstructionSetForElement(String elementName, RdfConversionInstructionSet rcis) {
		boolean added = false;
		if (elementName != null && !instructionsByElementName.containsKey(elementName)) {
			instructionsByElementName.put(elementName, rcis);
			added = true;
		}
		return added;
	}



}