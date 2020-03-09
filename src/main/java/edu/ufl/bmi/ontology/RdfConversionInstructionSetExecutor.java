package edu.ufl.bmi.ontology;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.IRI;

import edu.ufl.bmi.misc.DataObject;
import edu.ufl.bmi.misc.IriLookup;

public class RdfConversionInstructionSetExecutor {
	List<String> orderedListOfElements;
	HashMap<String, RdfConversionInstructionSet> instructionsByElementName;
	HashMap<String, OWLNamedIndividual> variables;

	public RdfConversionInstructionSetExecutor() {
		this.orderedListOfElements = new ArrayList<String>();
		this.instructionsByElementName = new HashMap<String, RdfConversionInstructionSet>();
	}

	public boolean addInstructionSetForElement(String elementName, RdfConversionInstructionSet rcis) {
		boolean added = false;
		if (rcis == null) throw new IllegalArgumentException("cannot have a null instruction set.");
		if (elementName != null && !this.instructionsByElementName.containsKey(elementName)) {
			this.instructionsByElementName.put(elementName, rcis);
			this.orderedListOfElements.add(elementName);
			System.out.println("Added instruction set for " + elementName + " (" + instructionsByElementName.size() + ", " + orderedListOfElements.size() + ")");
			added = true;
		}
		return added;
	}


	public boolean executeAllInstructionSets(OWLNamedIndividual rowIndividual, DataObject dataObject, 
		OWLOntology oo) {
		/*
		 * We need to share variables across instruction sets because variables created by one
		 *   instruction set might be used by other instruction sets.  This situation does
		 *   mandate that we get the elements in some predetermined order, which is expected
		 *   and therefore dictated by the instructions file as compiled.
		 *
		 * Therefore, we drive the RDF conversion from the instruction set and then report out
		 *   any elements that are present for which we did not execute instructions...
		 *
		 */
		if (this.variables == null) this.variables = new HashMap<String, OWLNamedIndividual>();

		Set<String> elements = dataObject.getElementKeySet();
		System.out.println(this.orderedListOfElements.size() + " instruction sets to process.");
		for (String elementName : this.orderedListOfElements) {
			String value = dataObject.getDataElementValue(elementName);
			//if (elements.contains(elementName)) {
			if (value != null && value.length() > 0) {
				//System.err.print("Executing instructions for: " + elementName + "...");
				RdfConversionInstructionSet rcis = instructionsByElementName.get(elementName);
				rcis.executeInstructions(rowIndividual, dataObject, oo, variables);
				//System.err.println("...done");
			}
		}

		//Set<String> elements = dataObject.getElementKeySet();
		for (String element : elements) {
			if (!this.orderedListOfElements.contains(element)) {
				System.out.println("Mild warning: element " + element + " is present in " + 
					"data object but is not handled by any instruction set.");
			}
		}

		return true;  //not sure why I put this here, so TODO is reconsider.
	}

	public void initializeVariables(IriLookup iriMap, OWLDataFactory odf) {
		this.variables = new HashMap<String, OWLNamedIndividual>();
		Set<Map.Entry<String,IRI>> indsAndIris = iriMap.individualEntrySet();
		for (Map.Entry<String,IRI> indAndIri : indsAndIris) {
			String variableName = indAndIri.getKey();
			IRI iri = indAndIri.getValue();
			/*
			 *  This is admittedly a weird dependency on GenericRdfConverter...
			 */
			OWLNamedIndividual oni = GenericRdfConverter.createNamedIndividualWithIriAndType(iri, iriMap.getTypeForIndividual(variableName));
			this.variables.put(variableName, oni);
		}
	}

}