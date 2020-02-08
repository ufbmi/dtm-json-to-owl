package edu.ufl.bmi.ontology;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

import edu.ufl.bmi.misc.DataObject;
import edu.ufl.bmi.misc.IriLookup;


public class RdfConversionAnnotationInstruction extends RdfConversionInstruction {

	IRI classIri;
	IRI annotationPropertyIri;
	String variableName;
	AnnotationValueBuilder avb;

	public RdfConversionAnnotationInstruction(IriLookup iriMap, HashMap<String,Integer> fieldNameToIndex, OWLDataFactory odf, String variableName, 
		String annotationPropertyTxt, String annotationValueInstruction) {
		super(iriMap, fieldNameToIndex, odf);
		this.variableName = variableName;
		this.annotationPropertyIri = iriMap.lookupAnnPropIri(annotationPropertyTxt);
		this.avb = new AnnotationValueBuilder(annotationValueInstruction, fieldNameToIndex);
	}

		@Override
	public void execute(OWLNamedIndividual rowIndividual, ArrayList<String> recordFields, HashMap<String, OWLNamedIndividual> variables, OWLOntology oo) {
		String annotationValue = avb.buildAnnotationValue(recordFields);
		if (validFieldValue(annotationValue)) {
			OWLNamedIndividual oni = (variableName.equals("[row-individual]")) ? rowIndividual : variables.get(variableName);
			if (oni != null)
				GenericRdfConverter.addAnnotationToNamedIndividual(oni, annotationPropertyIri, annotationValue, oo); 
		}
	}

	public void execute(OWLNamedIndividual rowIndividual, DataObject dataObject, HashMap<String, OWLNamedIndividual> variables, OWLOntology oo) {
		String annotationValue = avb.buildAnnotationValue(dataObject);
		if (validFieldValue(annotationValue)) {
			OWLNamedIndividual oni = (variableName.equals("[row-individual]")) ? rowIndividual : variables.get(variableName);
			if (oni != null)
				GenericRdfConverter.addAnnotationToNamedIndividual(oni, annotationPropertyIri, annotationValue, oo); 
		}		
	}

}