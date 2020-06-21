package edu.ufl.bmi.ontology;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.regex.Pattern;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

import edu.ufl.bmi.misc.DataObject;
import edu.ufl.bmi.misc.IriLookup;

public class RdfConversionQueryIndividualByAttributeValueInstruction extends RdfConversionInstruction {

	/*
		Here, the variableName is the varible in the file currently being processed, whose
			value we will use to query for the IRI of an already existing individual 
			created while we processed a previous file.

		Loosely speaking (and drawing analogy to relational data), this field is the
			foreigh key.
	*/
	String variableName;
	IRI classIri;
	IRI annotationPropertyIri;
	AnnotationValueBuilder avb;
	boolean alwaysCreate;
	ArrayList<ArrayList<String>> conditions;
	IriRepository iriRepository;
	String iriRepositoryPrefix;

	String iriPrefix;
	String queryIriPrefix;
	String externalFileFieldName;
	String externalFileRowTypeName;
	String lookupValueFieldName;
	String lookupVariableName;
	String lookupUniqueIdFieldName;
	ArrayList<String> searchInstructions;
	//int lookupValueFieldIndex;
	
	public RdfConversionQueryIndividualByAttributeValueInstruction(IriLookup iriMap, OWLDataFactory odf, String variableName, 
			IriRepository iriRepository, String iriRepositoryPrefix, String externalFileFieldName, String externalFileRowTypeName, String iriPrefix,
			String lookupValueFieldName, String lookupUniqueIdFieldName, String searchInstructions) {
		super(iriMap, odf);
		this.variableName = variableName.replaceFirst("[\\[]","");
		if (variableName.endsWith("]")) {
			int len = this.variableName.length();
			this.variableName = this.variableName.substring(0, len-1);
		}
		
		//this.variableName = (variableName.startsWith("[")) ? variableName.replace("[","").replace("]","") : variableName ;
		this.iriRepository = iriRepository;
		this.iriRepositoryPrefix = iriRepositoryPrefix;
		this.externalFileFieldName = externalFileFieldName;
		this.externalFileRowTypeName = externalFileRowTypeName;
		this.iriPrefix = iriPrefix;
		this.queryIriPrefix = this.iriPrefix + this.externalFileRowTypeName;
		//this.lookupValueFieldName = lookupValueFieldName.replace("[","").replace("]","");
		this.lookupValueFieldName = lookupValueFieldName.replaceFirst("[\\[]","");
		if (lookupValueFieldName.endsWith("]")) {
			int len = this.lookupValueFieldName.length();
			this.lookupValueFieldName = this.lookupValueFieldName.substring(0, len-1);
		}
		this.lookupUniqueIdFieldName = lookupUniqueIdFieldName.replaceFirst("[\\[]","");
		if (lookupUniqueIdFieldName.endsWith("]")) {
			int len = this.lookupUniqueIdFieldName.length();
			this.lookupUniqueIdFieldName = this.lookupUniqueIdFieldName.substring(0, len-1);
		}

		if (searchInstructions != null) initializeSearchInstructions(searchInstructions);
		//System.out.println(lookupValueFieldName);
		//this.lookupValueFieldIndex = fieldNameToIndex.get(this.lookupValueFieldName); 
	}

	protected void initializeSearchInstructions(String instructions) {
		String[] instructionArray = instructions.split(Pattern.quote(","));
		this.searchInstructions = new ArrayList<String>();
		for (String instruction : instructionArray) {
			this.searchInstructions.add(instruction);
		}
	}

/*
	@Override
	public void execute(OWLNamedIndividual rowIndividual, ArrayList<String> recordFields, HashMap<String, OWLNamedIndividual> variables, OWLOntology oo) {		
		HashMap<IRI, String> repoAnnotations = new HashMap<IRI, String>();
		IRI externalFieldIri = IRI.create(queryIriPrefix + "/" + externalFileFieldName);
		repoAnnotations.put(externalFieldIri, recordFields.get(fieldNameToIndex.get(this.lookupValueFieldName)));
		IRI externalVarNameIri = IRI.create(queryIriPrefix + "/variableName");
		repoAnnotations.put(externalVarNameIri, "row individual");
		
		Set<IRI> resultSet = iriRepository.queryIris(null, repoAnnotations);
		int resultCount = resultSet.size();
		if (resultCount > 1) {
			throw new RuntimeException("resultSet should be size 1, but got " + resultCount);
		}
		OWLNamedIndividual oni = (resultCount == 1) ? 
			odf.getOWLNamedIndividual(resultSet.iterator().next()) :
			null;
			
		//System.out.println("Adding the following to variables: " + variableName + "\t" + oni);
		if (oni != null) variables.put(variableName, oni);
		//iriRepository.addIris(oni.getIRI(), null, repoAnnotations);
	}
*/

	public void setLookupVariableName(String lookupVariableName) {
		this.lookupVariableName = lookupVariableName;
	}

	public void execute(OWLNamedIndividual rowIndividual, DataObject dataObject, DataObject parentObject, HashMap<String, OWLNamedIndividual> variables, OWLOntology oo) {
		//if for some reason we already found it, or maybe in some circumstances I haven't foreseen yet, created it, then just return
		if (variables.containsKey(this.variableName) && variables.get(this.variableName) != null) {
			System.err.println("variable [" + this.variableName + "] already set to " + variables.get(this.variableName));
			return;
		}
		String lookupValue = dataObject.getDataElementValue(lookupValueFieldName);
		if (lookupValue == null || lookupValue.length() == 0) return;

		lookupValue = processSearchInstructions(lookupValue);

		HashMap<IRI, String> repoAnnotations = new HashMap<IRI, String>();
		System.out.println("query instruction is looking for " + this.externalFileFieldName + " == " + lookupValue);
		
		IRI externalVarNameIri = IRI.create(queryIriPrefix + "/variableName");
		repoAnnotations.put(externalVarNameIri, this.externalFileFieldName);
		repoAnnotations.put(iriMap.lookupAnnPropIri("label"), lookupValue);
		
		// If looking up by abbreviation for example this will return the IRI of the abbreviation individual
		Set<IRI> resultSet = iriRepository.queryIris(null, repoAnnotations);
		int resultCount = resultSet.size();
		if (resultCount > 1 || resultCount == 0) {
			//throw new RuntimeException("resultSet should be size 1, but got " + resultCount);
			System.err.println("resultSet size should be 1, but is " + resultCount);
			System.err.println("\tlookup variable is " + this.lookupValueFieldName);
			System.err.println("\tlookup value is " + lookupValue);

		}
		if (resultCount == 0) return;
		//OWLNamedIndividual oni = (resultCount == 1) ? 
		//	odf.getOWLNamedIndividual(resultSet.iterator().next()) :
		//	null;
		// Once we have the IRI for the abbreviation, we need to get the ID annotation off that individual
		//  So this class needs the unique ID field for the schema of the type of data object passed in
		IRI uniqueFieldIri = IRI.create(queryIriPrefix + "/" + this.lookupUniqueIdFieldName);
		IRI individualIri = resultSet.iterator().next();
		Set<String> resultSetString = iriRepository.queryAnnotationValueForIri(individualIri, uniqueFieldIri);

		String idValue = resultSetString.iterator().next();
		resultCount = resultSetString.size();
		if (resultCount > 1) {
			//throw new RuntimeException("resultSet should be size 1, but got " + resultCount);
			System.err.println("resultSet size should be 1, but is " + resultCount);
			System.err.println("\tlookup annotation IRI is " + uniqueFieldIri);
			System.err.println("\tlookup individual IRI is " + individualIri);

		}
		//  Then we'll ask for row individual with ID field set to value we got back from previous query
		HashMap<IRI, String> repoAnnotations2 = new HashMap<IRI, String>();
		repoAnnotations2.put(uniqueFieldIri, idValue);
		repoAnnotations2.put(externalVarNameIri, "row individual");
		resultSet = iriRepository.queryIris(null, repoAnnotations2);
		resultCount = resultSet.size();
		if (resultCount > 1) {
			//throw new RuntimeException("resultSet should be size 1, but got " + resultCount);
			System.err.println("resultSet size should be 1, but is " + resultCount);
			System.err.println("\tlookup variable is " + this.lookupUniqueIdFieldName);
			System.err.println("\tlookup value is " + idValue);

		}

		OWLNamedIndividual oni = (resultCount > 0) ? 
			odf.getOWLNamedIndividual(resultSet.iterator().next()) :
			null;
		
		System.err.println("for " + this.lookupValueFieldName + " == " + lookupValue + ", returning IRI == " + oni.getIRI());			
		//System.out.println("Adding the following to variables: " + variableName + "\t" + oni);
		if (oni != null) variables.put(variableName, oni);
		//iriRepository.addIris(oni.getIRI(), null, repoAnnotations);		
	}

	/*
	 *	So right now we just do transformations to upper case or lower case.
	 *   Future modifications could also remove punctuation like . , ( ) : ; ' " { } [ ] \ / | + - * & ^ % $ # @ ! ~ ` < > ? 
	 *   with various levels of granular control.
	 */
	protected String processSearchInstructions(String lookupValue) {
		String processedValue = lookupValue;
		if (this.searchInstructions != null) {
			for (String instruction : this.searchInstructions) {
				String[] components = instruction.split(Pattern.quote(";"));
				String theInstructionItself = components[0];
				switch (theInstructionItself) {
					case "uppercase":
						processedValue = processedValue.toUpperCase();
						break;
					case "lowercase":
						processedValue = processedValue.toLowerCase();
						break;
					case "removeword":
						String beginning = components[1]+" ";
						String end = " "+components[1];
						String beginningUpper = beginning.toUpperCase();
						String beginningLower = beginning.toLowerCase();
						String endUpper = end.toUpperCase();
						String endLower = end.toLowerCase();
						processedValue = processedValue.replace(beginning,"").replace(end,"").replace(beginngUpper,"")
							.replace(beginningLower,"").replace(endUpper,"").replace(endLower,"");
						break;
					default:
						System.err.println("don't understand search instruction " + instruction);
						break;
				}
			}

		}
		return processedValue;
	}
}