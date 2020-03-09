package edu.ufl.bmi.ontology;

import java.io.FileReader;
import java.io.LineNumberReader;
import java.io.IOException;

import java.text.ParseException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;

import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

import edu.ufl.bmi.misc.DataObject;
import edu.ufl.bmi.misc.IriLookup;
import edu.ufl.bmi.ontology.RdfConversionInstruction;
import edu.ufl.bmi.ontology.RdfConversionInstructionSet;


public class RdfConversionMultipleValueConversionInstructionSet extends RdfConversionInstructionSet {
	
	String elementName;


	public RdfConversionMultipleValueConversionInstructionSet(String elementName, ArrayList<RdfConversionInstruction> instructions) {
		super(instructions);
		this.elementName = elementName;
	}

	@Override
	public void executeInstructions(OWLNamedIndividual rowIndividual, DataObject dataObject, OWLOntology oo,
		HashMap<String, OWLNamedIndividual> variables) {

		DataObject[] values = dataObject.getValuesAsDataObjectsForElement(this.elementName);

		for (DataObject nextDataObject : values) {
			/*
			 *  Each iteration will end up creating the same set of variables.  We don't
			 *    want to accidentally reuse a variable value from the previous 
			 *    iteration in the current iteration.  Therefore, we'll pass in a clean
			 *    copy of the variable set each time.
			 *
			 *  The consequence is that we cannot use any of these variables in other
			 *		instruction sets.  But for multiples, it would be hard to know
			 *		or specify which one(s) to reuse in the first place.
			 */
			HashMap<String, OWLNamedIndividual> varCopy = (HashMap<String, OWLNamedIndividual>)variables.clone();

			for (RdfConversionInstruction i : instructions) {
				i.execute(rowIndividual, nextDataObject, varCopy, oo);
			}
		}
	}

}