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

public class RdfConversionInstructionSet {

	ArrayList<RdfConversionInstruction> instructions;

	public RdfConversionInstructionSet(ArrayList<RdfConversionInstruction> instructions) {
		this.instructions = new ArrayList<RdfConversionInstruction>();
		this.instructions.addAll(instructions);
	}

/*
	public void executeInstructions(OWLNamedIndividual rowIndividual, ArrayList<String> recordFields, OWLOntology oo) {
		HashMap<String, OWLNamedIndividual> variables = new HashMap<String, OWLNamedIndividual>();

		for (RdfConversionInstruction i : instructions) {
			i.execute(rowIndividual, recordFields, variables, oo);
		}
	}
*/

	public void executeInstructions(OWLNamedIndividual rowIndividual, DataObject dataObject, OWLOntology oo) {
		/*
		 * This list of variables will be added to by each instruction.  Each subsequent instruction
		 *   may then use the variables set by earlier instructions.
		 *
		 * So we create the variable list before starting execution of instructions, and then
		 *  pass it to each instruction for its execution.
		 *
		 */
		HashMap<String, OWLNamedIndividual> variables = new HashMap<String, OWLNamedIndividual>();

		for (RdfConversionInstruction i : instructions) {
			i.execute(rowIndividual, dataObject, variables, oo);
		}
	}

	/*
	 * In some cases, but especially for version 2 format (F2) instruction sets, we will want to 
	 *   pass the variables into this instruction set.
	 *
	 */
	public void executeInstructions(OWLNamedIndividual rowIndividual, DataObject dataObject, OWLOntology oo,
		HashMap<String, OWLNamedIndividual> variables) {

		for (RdfConversionInstruction i : instructions) {
			i.execute(rowIndividual, dataObject, variables, oo);
		}
	}



}