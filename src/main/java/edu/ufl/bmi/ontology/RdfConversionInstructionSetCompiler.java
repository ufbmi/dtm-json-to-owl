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
	ArrayList<HashMap<String, OWLNamedIndividual>> searchIndexes;
	IriRepository iriRepository;
	String iriRepositoryPrefix;
	String uniqueIdFieldName;
	String iriPrefix;

	public RdfConversionInstructionSetCompiler(String fName, IriLookup iriMap, HashMap<String,Integer> fieldNameToIndex, 
			OWLDataFactory odf, ArrayList<HashMap<String, OWLNamedIndividual>> searchIndexes,
			IriRepository iriRepository, String iriRepositoryPrefix, String uniqueIdFieldName, String iriPrefix) {
		this.fileName = fName;
		instructionList = new ArrayList<RdfConversionInstruction>();
		this.iriMap = iriMap;
		this.fieldNameToIndex = fieldNameToIndex;
		this.odf = odf;
		this.searchIndexes = searchIndexes;
		this.iriRepository = iriRepository;
		this.iriRepositoryPrefix = iriRepositoryPrefix;
		this.uniqueIdFieldName = uniqueIdFieldName;
		this.iriPrefix = iriPrefix;
	}

	public RdfConversionInstructionSet compile() throws ParseException {
		try {
			FileReader fr = new FileReader(fileName);
			LineNumberReader lnr = new LineNumberReader(fr);
			String line;

			while((line=lnr.readLine())!=null) {
				line = line.trim();
				if (line.length() == 0 || line.startsWith("#")) continue;

				String[] flds = line.split(Pattern.quote(":"), 2);
				//System.out.println(flds.length + ", " + flds[0]);
				String instructionType = flds[0].trim();
				String instruction = flds[1].trim();
				
				RdfConversionInstruction rci = compileInstruction(instructionType, instruction);
				instructionList.add(rci);
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}

		return new RdfConversionInstructionSet(instructionList);
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