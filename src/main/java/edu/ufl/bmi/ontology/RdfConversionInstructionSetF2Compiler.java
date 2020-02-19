package edu.ufl.bmi.ontology;

import java.io.FileReader;
import java.io.LineNumberReader;
import java.io.IOException;

import java.text.ParseException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

import edu.ufl.bmi.misc.IriLookup;

/**
  * This class is a pseudocompiler that takes an instruction set file and stages
  *  appropriate instances of various instruction classes to carry out the 
  *  instruction in code.
  *  
  * Here, "F2" means format 2 for instruction set syntax.  This version of the 
  *  syntax is more data driven, meaning that if a field is either not present
  *  or has a blank value, the instructions associated with that field are 
  *  skipped.
 */
public class RdfConversionInstructionSetF2Compiler {
	String fileName;

	//ArrayList<RdfConversionInstruction> instructionList;
	IriLookup iriMap;
	OWLDataFactory odf;
	HashMap<String, HashMap<String, OWLNamedIndividual>> searchIndexes;
	IriRepository iriRepository;
	String iriRepositoryPrefix;
	String uniqueIdFieldName;
	String iriPrefix;

	static final String VARIABLE_PATTERN = "\\[(.*)\\]";

	public RdfConversionInstructionSetF2Compiler(String fName, IriLookup iriMap, OWLDataFactory odf, 
		HashMap<String, HashMap<String, OWLNamedIndividual>> searchIndexes, IriRepository iriRepository, 
		String iriRepositoryPrefix, String uniqueIdFieldName, String iriPrefix) {
		this.fileName = fName;
		this.iriMap = iriMap;
		this.odf = odf;
		this.searchIndexes = searchIndexes;
		this.iriRepository = iriRepository;
		this.iriRepositoryPrefix = iriRepositoryPrefix;
		this.uniqueIdFieldName = uniqueIdFieldName;
		this.iriPrefix = iriPrefix;
	}

	public RdfConversionInstructionSetExecutor compile() throws ParseException {
		RdfConversionInstructionSetExecutor rcise = new RdfConversionInstructionSetExecutor();
		try {
			FileReader fr = new FileReader(fileName);
			LineNumberReader lnr = new LineNumberReader(fr);
			String line;

			ArrayList<RdfConversionInstruction> instructionList = null; 
			Pattern p = Pattern.compile(VARIABLE_PATTERN);
			String elementName = "";
			while((line=lnr.readLine())!=null) {
				//System.err.println(line);
				line = line.trim();  //ignore any leading and trailing whitespace
				//skip all blank lines and comment lines
				if (line.length() == 0 || line.startsWith("#")) continue;

				Matcher m = p.matcher(line);
				//if the line constitutes a variable name, then we're starting a new section
				if (m.matches()) {
					//the first time through the current variable name is empty, so only do 
					// this section if we're changing variable names
					if (elementName.length() > 0) {
						//save away the instruction set in the hash by its associated variable name
						boolean added = rcise.addInstructionSetForElement(elementName, new RdfConversionInstructionSet(instructionList));
						if (!added) {
							System.err.println("instructions for element " + elementName + " were not added to " +
								"the instruction set execution engine.");
						}
					}
					//prepare a new instruction list for the next variable
					instructionList = new ArrayList<RdfConversionInstruction>();
					// the variable name should be in group 1 of the instruction set
					elementName = m.group(1).trim();
				} else {
					if (line.contains("\\[")) System.err.println("line has [ but pattern did not match.");
				

					//an instruction is two parts -- instruction type : instruction content
					String[] flds = line.split(Pattern.quote(":"), 2);
					//System.out.println(flds.length + ", " + flds[0] + ", " + line);
					String instructionType = flds[0].trim();
					String instruction = flds[1].trim();
				
					RdfConversionInstruction rci = compileInstruction(instructionType, instruction);
					instructionList.add(rci);
				}
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}

		return rcise;
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
			RdfConversionAnnotationInstruction rcai = new RdfConversionAnnotationInstruction(iriMap, odf, 
				variableName, annotationPropertyTxt, annotationValueInstruction);
			return rcai;
		} else if (instructionType.equals("new-individual")) {
			if (flds.length != 4 && flds.length != 5) throw new ParseException(
				"new individual instruction must have four, tab-delimited fields: " + instruction, 2);
			String variableName = flds[0].trim();
			String classIriTxt = flds[1].trim();
			String annotationPropertyTxt = flds[2].trim();
			String annotationValueInstruction = flds[3].trim();
			RdfConversionNewIndividualInstruction rcnii = null;
			if (flds.length == 4) {
				rcnii = new RdfConversionNewIndividualInstruction(
					iriMap, odf, variableName, classIriTxt, annotationPropertyTxt, annotationValueInstruction, 
					iriRepository, iriRepositoryPrefix, uniqueIdFieldName);
			} else if (flds.length == 5) {
				String creationConditionLogic = flds[4].trim();
				rcnii = new RdfConversionNewIndividualInstruction(
					iriMap, odf, variableName, classIriTxt, annotationPropertyTxt, annotationValueInstruction,
					creationConditionLogic, iriRepository, iriRepositoryPrefix, uniqueIdFieldName);
			}
			return rcnii;
		} else if (instructionType.equals("data-property-expression")) {
			if (flds.length != 4) throw new ParseException(
				"data property expression instruction must have four, tab-delimited fields: " + instruction, 3);
			String variableName = flds[0].trim();
			String dataPropertyIriTxt = flds[1].trim();
			String dataValueInstruction = flds[2].trim(); //.replace("[","").replace("]","");
			String dataType = flds[3].trim();
			RdfConversionDataInstruction rcdi = new RdfConversionDataInstruction(iriMap, odf, variableName, 
					dataPropertyIriTxt, dataValueInstruction, dataType);
			return rcdi;
		} else if (instructionType.equals("object-property-expression")) {
			if (flds.length != 3) throw new ParseException(
				"object property expression instructions require three, tab-delimited fields.", 4);
			String sourceVariableName = flds[0].trim();
			String objectPropertyIriTxt = flds[1].trim();
			String targetVariableName = flds[2].trim();
			RdfConversionObjectPropertylInstruction rcopi = new RdfConversionObjectPropertylInstruction(iriMap, 
					odf, sourceVariableName, objectPropertyIriTxt, targetVariableName);
			return rcopi;
		} else if (instructionType.equals("lookup-individual")) {
			if (flds.length != 2) throw new ParseException(
				"lookup individual instructions must have two, tab-delimited fields: " + instruction, 5);
			String variableName = flds[0].trim();
			String searchFieldName = flds[1].trim().replace("[","").replace("]","");
			RdfConversionLookupInstruction rclii = new RdfConversionLookupInstruction(iriMap, 
					odf, variableName, searchFieldName, searchIndexes);
			return rclii;
		} else if (instructionType.equals("class-assertion-expression")) {
			if (flds.length !=2) throw new ParseException(
				"class assertion expressions must have two, tab-delimited fields: " + instruction, 7);
			String variableName = flds[0].trim();
			String classIriHandle = flds[1].trim();
			RdfClassAssertionInstruction rcai = new RdfClassAssertionInstruction(iriMap, odf, 
				variableName, classIriHandle);
			return rcai;
		} else if (instructionType.equals("query-individual")) {
			if (flds.length != 4) throw new ParseException(
				"query individual expressions must have four, tab-delimited fields." + instruction, 8);
			String variableName = flds[0].trim(); 					// e.g., affiliation-org
			String rowTypeName = flds[1].trim();					// e.g., organization
			String externalFileFieldName = flds[2].trim();			// e.g., ID
			String lookupValueFieldName = flds[3].trim();			// e.g., [OrganizationAffiliationID]

			/*
			IriLookup iriMap, HashMap<String,Integer> fieldNameToIndex, OWLDataFactory odf, String variableName, 
			IriRepository iriRepository, String iriRepositoryPrefix, String externalFileFieldName, String externalFileRowTypeName, String iriPrefix,
			String lookupValueFieldName
			*/
			RdfConversionQueryIndividualInstruction rcqii = new RdfConversionQueryIndividualInstruction(
				iriMap, odf, variableName, iriRepository, iriRepositoryPrefix, externalFileFieldName, 
				rowTypeName, iriPrefix, lookupValueFieldName);
			return rcqii;
		} else {
			throw new ParseException("don't understand instruction type of " + instructionType, 6);
		}
	}

}