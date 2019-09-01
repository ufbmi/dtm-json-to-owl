package edu.ufl.bmi.ontology;

import java.util.ArrayList;
import java.util.HashMap;

import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

import edu.ufl.bmi.misc.IriLookup;

public class RdfConversionLookupInstruction extends RdfConversionInstruction {

	String variableName;
	int searchFieldIndex;
	/*
	 *  TODO: we can update the instruction syntax to tell the execution specifically
	 *    which unique field to search.  Then we need only one search index here, which
	 *    we can look up prior to passing it here.
	 */
	ArrayList<HashMap<String, OWLNamedIndividual>> searchIndexes;

	public RdfConversionLookupInstruction(IriLookup iriMap, HashMap<String,Integer> fieldNameToIndex, 
		OWLDataFactory odf, String variableName, String searchFieldName, ArrayList<HashMap<String, OWLNamedIndividual>> searchIndexes) {
		super(iriMap, fieldNameToIndex, odf);
		this.variableName = variableName.replace("[","").replace("]","").trim();
		this.searchFieldIndex = fieldNameToIndex.get(searchFieldName);
		this.searchIndexes = searchIndexes;
	}

	@Override
	public void execute(OWLNamedIndividual rowIndividual, ArrayList<String> recordFields, HashMap<String, OWLNamedIndividual> variables, OWLOntology oo) {
		//Find the value of the field specified in this instruction, and search for named individuals created with that field value
		String fieldValue = recordFields.get(searchFieldIndex);
		System.out.println("lookup field value: " + fieldValue + " lookup field index " + searchFieldIndex);
		if (validFieldValue(fieldValue)) {
			for (HashMap<String,OWLNamedIndividual> searchIndex : searchIndexes) {
				OWLNamedIndividual oni = searchIndex.get(fieldValue);
				if (oni != null) {
					variables.put(variableName, oni);
					System.out.println("added the following to hashmap " + variableName + "\t" + oni);
					break;
				}
			}
		} else {
			System.out.println("Bad field value: " + fieldValue);
		}
	}
}