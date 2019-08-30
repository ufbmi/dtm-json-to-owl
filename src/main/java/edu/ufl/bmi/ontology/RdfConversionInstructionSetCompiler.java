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

import edu.ufl.bmi.misc.IriLookup;

public class RdfConversionInstructionSetCompiler {
	String fileName;
	ArrayList<RdfConversionInstruction> instructionList;
	IriLookup iriMap;
	HashMap<String,Integer> fieldNameToIndex;
	OWLDataFactory odf;

	public RdfConversionInstructionSetCompiler(String fName, IriLookup iriMap, HashMap<String,Integer> fieldNameToIndex, OWLDataFactory odf) {
		this.fileName = fName;
		instructionList = new ArrayList<RdfConversionInstruction>();
		this.iriMap = iriMap;
		this.fieldNameToIndex = fieldNameToIndex;
		this.odf = odf;
	}

	public void compile() throws ParseException {
		try {
			FileReader fr = new FileReader(fileName);
			LineNumberReader lnr = new LineNumberReader(fr);
			String line;

			while((line=lnr.readLine())!=null) {
				line = line.trim();
				if (line.length() == 0) continue;

				String[] flds = line.split(Pattern.quote(":"));
				String instructionType = flds[0].trim();
				String instruction = flds[1].trim();
				
				RdfConversionInstruction rci = compileInstruction(instructionType, instruction);
				instructionList.add(rci);
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	//public RdfConversionInstructionSet getInstructionSet() {
//
//	}

	public RdfConversionInstruction compileInstruction(String instructionType, String instruction) throws ParseException {
		String[] flds = instruction.split(Pattern.quote("\t"));
		if (instructionType.equals("annotation")) {		
			if (flds.length != 3) throw new ParseException(
				"annotation instruction must have three, tab delimited fields: " +  instruction, 1);
			String variableName = flds[0].trim();
			String annotationPropertyTxt = flds[1].trim();
			String annotationValueInstruction = flds[2].trim();
			RdfConversionAnnotationInstruction rcai = new RdfConversionAnnotationInstruction(iriMap, fieldNameToIndex, odf, 
				variableName, annotationPropertyTxt, annotationValueInstruction);
			return rcai;
		} else if (instructionType.equals("new-individual")) {
			if (flds.length != 4) throw new ParseException(
				"new individual instruction must have four, tab-delimited fields: " + instruction, 2);
			String variableName = flds[0].trim();
			String classIriTxt = flds[1].trim();
			String annotationPropertyTxt = flds[2].trim();
			String annotationValueInstruction = flds[3].trim();
			RdfConversionNewIndividualInstruction rcnii = new RdfConversionNewIndividualInstruction(
				iriMap, fieldNameToIndex, odf, variableName, classIriTxt, annotationPropertyTxt, annotationValueInstruction);
			return rcnii;
		} else if (instructionType.equals("data-property-expression")) {
			return null;
		} else if (instructionType.equals("object-property-expression")) {
			return null;
		} else if (instructionType.equals("lookup-individual")) {
			return null;
		} else {
			throw new ParseException("don't understand instruction type of " + instructionType, 6);
		}
	}

}