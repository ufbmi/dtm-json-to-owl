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

public class RdfConversionQueryIndividualInstruction extends RdfConversionInstruction {

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
	//int lookupValueFieldIndex;
	
	public RdfConversionQueryIndividualInstruction(IriLookup iriMap, OWLDataFactory odf, String variableName, 
			IriRepository iriRepository, String iriRepositoryPrefix, String externalFileFieldName, String externalFileRowTypeName, String iriPrefix,
			String lookupValueFieldName) {
		super(iriMap, odf);
		this.variableName = variableName.replace("[","").replace("]","");
		this.iriRepository = iriRepository;
		this.iriRepositoryPrefix = iriRepositoryPrefix;
		this.externalFileFieldName = externalFileFieldName;
		this.externalFileRowTypeName = externalFileRowTypeName;
		this.iriPrefix = iriPrefix;
		this.queryIriPrefix = this.iriPrefix + this.externalFileRowTypeName;
		this.lookupValueFieldName = lookupValueFieldName.replace("[","").replace("]","");
		System.out.println(lookupValueFieldName);
		//this.lookupValueFieldIndex = fieldNameToIndex.get(this.lookupValueFieldName); 
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

	public void execute(OWLNamedIndividual rowIndividual, DataObject dataObject, HashMap<String, OWLNamedIndividual> variables, OWLOntology oo) {
		HashMap<IRI, String> repoAnnotations = new HashMap<IRI, String>();
		IRI externalFieldIri = IRI.create(queryIriPrefix + "/" + externalFileFieldName);
		repoAnnotations.put(externalFieldIri, dataObject.getDataElementValue(lookupValueFieldName));
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
}