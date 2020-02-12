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

public class RdfClassAssertionInstruction extends RdfConversionInstruction {

	/*
		The handle that we will use to lookup the individual
			for which we are making the class assertion
	*/
	String handleOfIndividual;

	/*
		The name of the variable/field in the source data that 
			specifies the class, we will look it up in the 
			IriLookup instance we get.
	*/
	String classTypeVariableName;

	IriLookup iriMap;
	OWLDataFactory odf;

	public RdfClassAssertionInstruction(IriLookup iriMap, OWLDataFactory odf, String handleOfIndividual, 
		String classTypeVariableName) {
		super(iriMap, odf);
		this.handleOfIndividual = handleOfIndividual.trim();
		this.classTypeVariableName = classTypeVariableName.replace("[","").replace("]","").trim();
		this.iriMap = iriMap;
	}

/*
		@Override
	public void execute(OWLNamedIndividual rowIndividual, ArrayList<String> recordFields, 
		HashMap<String, OWLNamedIndividual> variables, OWLOntology oo) {
		/*
		 *  Step 1: get IRI for class. If not null, continue.
		 *  Step 2: get individual for which we're making assertion
		 *  Step 3: make assertion
		 *
		String classHandle = recordFields.get(fieldIndex).trim();
		IRI classIri = iriMap.lookupClassIri(classHandle);
		if (classIri != null) {
			OWLNamedIndividual oni = (handleOfIndividual.equals("[row-individual]")) ? rowIndividual : variables.get(handleOfIndividual);
			//System.out.println("class is " + classHandle + "\t" + classIri);
			//System.out.println("ind is " + handleOfIndividual + "\t" + oni);
			GenericRdfConverter.addClassAssertionAxiom(oni, classIri, oo);
		} else {
			System.out.println("WARNING: could not find IRI for class: \"" + classHandle + "\"");
		}
	}
*/
	
	@Override
	public void execute(OWLNamedIndividual rowIndividual, DataObject dataObject, HashMap<String, OWLNamedIndividual> variables, OWLOntology oo) {
		/*
		 *  Step 1: get IRI for class. If not null, continue.
		 *  Step 2: get individual for which we're making assertion
		 *  Step 3: make assertion
		 */
		String classHandle = dataObject.getDataElementValue(classTypeVariableName).trim();
		IRI classIri = iriMap.lookupClassIri(classHandle);
		if (classIri != null) {
			OWLNamedIndividual oni = (handleOfIndividual.equals("[row-individual]")) ? rowIndividual : variables.get(handleOfIndividual);
			//System.out.println("class is " + classHandle + "\t" + classIri);
			//System.out.println("ind is " + handleOfIndividual + "\t" + oni);
			GenericRdfConverter.addClassAssertionAxiom(oni, classIri, oo);
		} else {
			System.out.println("WARNING: could not find IRI for class: \"" + classHandle + "\"");
		}
	}

}