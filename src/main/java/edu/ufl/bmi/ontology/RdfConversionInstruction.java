package edu.ufl.bmi.ontology;

import java.util.ArrayList;
import java.util.HashMap;

import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

import edu.ufl.bmi.misc.DataObject;
import edu.ufl.bmi.misc.IriLookup;

public abstract class RdfConversionInstruction {

	OWLDataFactory odf;
	IriLookup iriMap;


	public RdfConversionInstruction(IriLookup iriMap, OWLDataFactory odf) {
		this.odf = odf;
		this.iriMap = iriMap;
	}

	//public abstract void execute(OWLNamedIndividual rowIndividual, ArrayList<String> recordFields, HashMap<String, OWLNamedIndividual> variables, OWLOntology oo);

	public String cleanupValue(String fieldValue) {
		return fieldValue.replace("\"","").replace("N/A","").trim();
	}

	public boolean validFieldValue(String fieldValue) {
		return (fieldValue != null && fieldValue.trim().length() > 0);
	}

	public abstract void execute(OWLNamedIndividual rowIndividual, DataObject dataObject, HashMap<String, OWLNamedIndividual> variables, OWLOntology oo);

}