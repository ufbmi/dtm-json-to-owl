package edu.ufl.bmi.ontology;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

import edu.ufl.bmi.misc.IriLookup;

public class RdfConversionNewIndividualInstruction extends RdfConversionInstruction {

	String variableName;
	IRI classIri;
	IRI annotationPropertyIri;
	AnnotationValueBuilder avb;
	
	public RdfConversionNewIndividualInstruction(IriLookup iriMap, HashMap<String,Integer> fieldNameToIndex, OWLDataFactory odf, String variableName, 
		String classIriTxt, String annotationPropertyTxt, String annotationValueInstruction) {
		super(iriMap, fieldNameToIndex, odf);
		this.variableName = variableName;
		this.classIri = iriMap.lookupClassIri(classIriTxt);
		this.annotationPropertyIri = iriMap.lookupAnnPropIri(annotationPropertyTxt);
		this.avb = new AnnotationValueBuilder(annotationValueInstruction, fieldNameToIndex);
	}

	@Override
	public void execute(OWLNamedIndividual rowIndividual, ArrayList<String> recordFields, HashMap<String, OWLNamedIndividual> variables, OWLOntology oo) {
		String annotationValue = avb.buildAnnotationValue(recordFields);
		OWLNamedIndividual oni = GenericRdfConverter.createNamedIndividualWithTypeAndLabel(oo, classIri, annotationPropertyIri, 
			annotationValue);
		variables.put(variableName, oni);
	}
}
