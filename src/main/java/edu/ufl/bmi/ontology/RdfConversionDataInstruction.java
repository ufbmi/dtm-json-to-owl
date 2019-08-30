package edu.ufl.bmi.ontology;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

import edu.ufl.bmi.misc.IriLookup;


public class RdfConversionDataInstruction extends RdfConversionInstruction {

	IRI classIri;
	IRI dataPropertyIri;
	String variableName;
	DataValueBuilder dvb;

	public RdfConversionDataInstruction(IriLookup iriMap, HashMap<String,Integer> fieldNameToIndex, OWLDataFactory odf, String variableName, 
		String dataPropertyTxt, String dataValueInstruction, String dataType) {
		super(iriMap, fieldNameToIndex, odf);
		this.variableName = variableName;
		this.dataPropertyIri = iriMap.lookupAnnPropIri(dataPropertyTxt);
		this.dvb = new DataValueBuilder(dataValueInstruction, dataType, fieldNameToIndex);
	}

	@Override
	public void execute(OWLNamedIndividual rowIndividual, ArrayList<String> recordFields, HashMap<String, OWLNamedIndividual> variables, OWLOntology oo) {
		Object dataValue = dvb.buildDataValue(recordFields);
		OWLNamedIndividual oni = (variableName.equals("[row-individual]")) ? rowIndividual : variables.get(variableName);
		switch (dvb.getDataType()) {
			case "String":
				String s = (String)dataValue;
				GenericRdfConverter.addStringDataToNamedIndividual(oni, dataPropertyIri, s, oo); 
				break;
			case "int":
				Integer i = (Integer)dataValue;
				GenericRdfConverter.addIntDataToNamedIndividual(oni, dataPropertyIri, i.intValue(), oo); 
				break;
			case "float":
				Float f = (Float)dataValue;
				GenericRdfConverter.addFloatDataToNamedIndividual(oni, dataPropertyIri, f.floatValue(), oo); 
				break;
			case "boolean":
				Boolean b = (Boolean)dataValue;
				GenericRdfConverter.addBooleanDataToNamedIndividual(oni, dataPropertyIri, b.booleanValue(), oo); 
				break;
			default:
				System.err.println("dataValue is of type " + dataValue);
		}
	}

}