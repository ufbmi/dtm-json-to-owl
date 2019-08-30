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
	HashMap<String, OWLNamedIndividual> searchIndex;

	public RdfConversionLookupInstruction(IriLookup iriMap, HashMap<String,Integer> fieldNameToIndex, 
		OWLDataFactory odf, String variableName, String searchFieldName, ArrayList<HashMap<String, OWLNamedIndividual>> searchIndexes) {
		super(iriMap, fieldNameToIndex, odf);
		this.variableName = variableName;
		this.searchFieldIndex = fieldNameToIndex.get(searchFieldName);
		this.searchIndex = searchIndexes.get(searchFieldIndex);
	}

	@Override
	public void execute(OWLNamedIndividual rowIndividual, ArrayList<String> recordFields, HashMap<String, OWLNamedIndividual> variables, OWLOntology oo) {
		//Find the value of the field specified in this instruction, and search for named individuals created with that field value
		OWLNamedIndividual oni = searchIndex.get(recordFields.get(searchFieldIndex));
		variables.put(variableName, oni);
	}
}