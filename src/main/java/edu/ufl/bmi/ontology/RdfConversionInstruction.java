package edu.ufl.bmi.ontology;

import java.util.ArrayList;
import java.util.HashMap;

import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

import edu.ufl.bmi.misc.IriLookup;

public abstract class RdfConversionInstruction {

	OWLDataFactory odf;
	HashMap<String,Integer> fieldNameToIndex;
	IriLookup iriMap;


	public RdfConversionInstruction(IriLookup iriMap, HashMap<String,Integer> fieldNameToIndex, OWLDataFactory odf) {
		this.odf = odf;
		this.fieldNameToIndex = fieldNameToIndex;
		this.iriMap = iriMap;
	}

	public abstract void execute(OWLNamedIndividual rowIndividual, ArrayList<String> recordFields, HashMap<String, OWLNamedIndividual> variables, OWLOntology oo);

}