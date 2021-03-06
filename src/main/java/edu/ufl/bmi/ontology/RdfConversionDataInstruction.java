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


public class RdfConversionDataInstruction extends RdfConversionInstruction {

	IRI classIri;
	IRI dataPropertyIri;
	String variableName;
	DataValueBuilder dvb;

	public RdfConversionDataInstruction(IriLookup iriMap, OWLDataFactory odf, String variableName, 
		String dataPropertyTxt, String dataValueInstruction, String dataType) {
		super(iriMap, odf);
		this.variableName = variableName;
		this.dataPropertyIri = iriMap.lookupDataPropIri(dataPropertyTxt);
		this.dvb = new DataValueBuilder(dataValueInstruction, dataType);
	}

/*
	@Override
	public void execute(OWLNamedIndividual rowIndividual, ArrayList<String> recordFields, HashMap<String, OWLNamedIndividual> variables, OWLOntology oo) {
		Object dataValue = dvb.buildDataValue(recordFields);
		OWLNamedIndividual oni = (variableName.equals("[row-individual]")) ? rowIndividual : variables.get(variableName);
		if (oni != null) {
			switch (dvb.getDataType()) {
				case "String":
					String s = (String)dataValue;
					//System.out.println(dataPropertyIri + "\t" + s);
					if (s.trim().length() > 0)
						GenericOwl2Converter.addStringDataToNamedIndividual(oni, dataPropertyIri, s, oo); 
					break;
				case "int":
					Integer i = (Integer)dataValue;
					if (i != null)
						GenericOwl2Converter.addIntDataToNamedIndividual(oni, dataPropertyIri, i.intValue(), oo); 
					break;
				case "float":
					Float f = (Float)dataValue;
					if (f != null)
						GenericOwl2Converter.addFloatDataToNamedIndividual(oni, dataPropertyIri, f.floatValue(), oo); 
					break;
				case "boolean":
					Boolean b = (Boolean)dataValue;
					if (b != null)
						GenericOwl2Converter.addBooleanDataToNamedIndividual(oni, dataPropertyIri, b.booleanValue(), oo); 
					break;
				default:
					System.err.println("dataValue is of type " + dataValue);
			}
		}
	}
*/

	@Override
	public void execute(OWLNamedIndividual rowIndividual, DataObject dataObject, DataObject parentObject, HashMap<String, OWLNamedIndividual> variables, OWLOntology oo) {
		Object dataValue = dvb.buildDataValue(dataObject);
		OWLNamedIndividual oni = (variableName.equals("[row-individual]")) ? rowIndividual : variables.get(variableName);
		if (oni != null) {
			switch (dvb.getDataType()) {
				case "String":
					String s = (String)dataValue;
					//System.out.println(dataPropertyIri + "\t" + s);
					if (s.trim().length() > 0)
						GenericOwl2Converter.addStringDataToNamedIndividual(oni, dataPropertyIri, s, oo); 
					break;
				case "int":
					Integer i = (Integer)dataValue;
					if (i != null)
						GenericOwl2Converter.addIntDataToNamedIndividual(oni, dataPropertyIri, i.intValue(), oo); 
					break;
				case "float":
					Float f = (Float)dataValue;
					if (f != null)
						GenericOwl2Converter.addFloatDataToNamedIndividual(oni, dataPropertyIri, f.floatValue(), oo); 
					break;
				case "boolean":
					Boolean b = (Boolean)dataValue;
					if (b != null)
						GenericOwl2Converter.addBooleanDataToNamedIndividual(oni, dataPropertyIri, b.booleanValue(), oo); 
					break;
				default:
					System.err.println("dataValue is of type " + dataValue);
			}
		}		
	}

}