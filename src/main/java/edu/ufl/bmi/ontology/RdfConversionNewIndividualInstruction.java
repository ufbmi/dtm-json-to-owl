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

public class RdfConversionNewIndividualInstruction extends RdfConversionInstruction {

	String variableName;
	IRI classIri;
	IRI annotationPropertyIri;
	AnnotationValueBuilder avb;
	boolean alwaysCreate;
	ArrayList<ArrayList<String>> conditions;
	IriRepository iriRepository;
	String iriRepositoryPrefix;
	String uniqueIdFieldName;
	int uniqueIdFieldIndex;
	IRI uniqueIdFieldIri;
	String iriSourceVariableName;
	
	public RdfConversionNewIndividualInstruction(IriLookup iriMap, OWLDataFactory odf, String variableName, 
		String classIriTxt, String annotationPropertyTxt, String annotationValueInstruction,
		IriRepository iriRepository, String iriRepositoryPrefix, String uniqueIdFieldName) {
		super(iriMap, odf);
		this.variableName = variableName.replace("[","").replace("]","");
		this.classIri = iriMap.lookupClassIri(classIriTxt);
		this.annotationPropertyIri = iriMap.lookupAnnPropIri(annotationPropertyTxt);
		this.avb = new AnnotationValueBuilder(annotationValueInstruction);
		this.alwaysCreate = true;
		this.iriRepository = iriRepository;
		this.iriRepositoryPrefix = iriRepositoryPrefix;
		this.uniqueIdFieldName = uniqueIdFieldName;
		this.uniqueIdFieldIri = IRI.create(iriRepositoryPrefix + "/" + uniqueIdFieldName);
		this.iriSourceVariableName = null;
	}

	public RdfConversionNewIndividualInstruction(IriLookup iriMap, OWLDataFactory odf, String variableName, 
		String classIriTxt, String annotationPropertyTxt, String annotationValueInstruction, String creationCondition, IriRepository iriRepository,
		String iriRepositoryPrefix, String uniqueIdFieldName) {
		super(iriMap, odf);
		this.variableName = variableName.replace("[","").replace("]","");
		this.classIri = iriMap.lookupClassIri(classIriTxt);
		this.annotationPropertyIri = iriMap.lookupAnnPropIri(annotationPropertyTxt);
		this.avb = new AnnotationValueBuilder(annotationValueInstruction);
		this.alwaysCreate = false;
		this.iriRepository = iriRepository;
		this.iriRepositoryPrefix = iriRepositoryPrefix;
		this.uniqueIdFieldName = uniqueIdFieldName;
		this.uniqueIdFieldIri = IRI.create(iriRepositoryPrefix + "/" + uniqueIdFieldName);
		this.setCreationConditionLogic(creationCondition);
		this.iriSourceVariableName = null;
	}

/*
	@Override
	public void execute(OWLNamedIndividual rowIndividual, ArrayList<String> recordFields, HashMap<String, OWLNamedIndividual> variables, OWLOntology oo) {
		if (alwaysCreate || evaluateCondition(recordFields)) {
			String annotationValue = avb.buildAnnotationValue(recordFields);
			if (validFieldValue(annotationValue)) {
				HashMap<IRI, String> repoAnnotations = new HashMap<IRI, String>();
				IRI varNameIri = IRI.create(iriRepositoryPrefix + "/variableName");
				repoAnnotations.put(varNameIri, variableName);
				IRI rdfLabelIri = iriMap.lookupAnnPropIri("label");
				repoAnnotations.put(rdfLabelIri, annotationValue);
				repoAnnotations.put(this.uniqueIdFieldIri, recordFields.get(uniqueIdFieldIndex));
				
				Set<IRI> resultSet = iriRepository.queryIris(null, repoAnnotations);
				int resultCount = resultSet.size();
				if (resultCount > 1) {
					throw new RuntimeException("resultSet should be size 1, but got " + resultCount);
				}

				OWLNamedIndividual oni = (resultCount == 1) ? 
					GenericRdfConverter.createNamedIndividualWithIriTypeAndLabel(resultSet.iterator().next(), oo, classIri, annotationPropertyIri, 
						annotationValue) :
					GenericRdfConverter.createNamedIndividualWithTypeAndLabel(oo, classIri, annotationPropertyIri, 
						annotationValue);
				//System.out.println("Adding the following to variables: " + variableName + "\t" + oni);
				variables.put(variableName, oni);
				iriRepository.addIris(oni.getIRI(), null, repoAnnotations);

			}
		}
	}

	public boolean evaluateCondition(ArrayList<String> recordFields) {
		boolean result = false;  // only set to true if one of the sublists has all true conditions
		for (ArrayList<String> condition : conditions) {
			boolean subConditionResult = true;  //any false within the list will change the "and" to false
			for (String subcondition : condition) {
				//check ==, !=, not-empty.  We can't handle any nesting at the moment, but we don't have the need, either.
				if (subcondition.contains("==")) {
					String[] flds = subcondition.split("==");
					String field = flds[0].trim().replace("[","").replace("]","");
					String value = flds[1];
					subConditionResult = subConditionResult && recordFields.get(fieldNameToIndex.get(field)).equals(value);
				} else if (subcondition.contains("!=")) {
					String[] flds = subcondition.split("!=");
					String field = flds[0].trim().replace("[","").replace("]","");
					String value = flds[1];
					subConditionResult = subConditionResult && !recordFields.get(fieldNameToIndex.get(field)).equals(value);
				} else if (subcondition.contains("not-empty")) {
					String field = subcondition.replace("not-empty","").trim().replace("[","").replace("]","");
					subConditionResult = subConditionResult && !recordFields.get(fieldNameToIndex.get(field)).isEmpty();
				}
			}
			result = result || subConditionResult;
		}
		return result;
	}
	*/

	public void execute(OWLNamedIndividual rowIndividual, DataObject dataObject, HashMap<String, OWLNamedIndividual> variables, OWLOntology oo) {
		if (alwaysCreate || evaluateCondition(dataObject)) {
			String annotationValue = avb.buildAnnotationValue(dataObject);
			if (validFieldValue(annotationValue)) {
				HashMap<IRI, String> repoAnnotations = new HashMap<IRI, String>();
				IRI varNameIri = IRI.create(iriRepositoryPrefix + "/variableName");
				repoAnnotations.put(varNameIri, variableName);
				IRI rdfLabelIri = iriMap.lookupAnnPropIri("label");
				repoAnnotations.put(rdfLabelIri, annotationValue);
				repoAnnotations.put(this.uniqueIdFieldIri, dataObject.getDataElementValue(uniqueIdFieldName));
				
				Set<IRI> resultSet = iriRepository.queryIris(null, repoAnnotations);
				int resultCount = resultSet.size();
				if (resultCount > 1) {
					throw new RuntimeException("resultSet should be size 1, but got " + resultCount);
				}

				IRI individualIri = null;
				if (resultCount == 1) {
					individualIri = resultSet.iterator().next();
				} else if (this.iriSourceVariableName != null && !this.iriSourceVariableName.isEmpty()) {
					String iriTxt = dataObject.getDataElementValue(this.iriSourceVariableName); 
					System.out.println("Got value of " + iriTxt + " for " + this.iriSourceVariableName + " data element.");
					individualIri = IRI.create(iriTxt);
					System.out.println("Created new IRI: " + individualIri.toString());
				}

				OWLNamedIndividual oni = (individualIri != null) ? 
					GenericRdfConverter.createNamedIndividualWithIriTypeAndLabel(individualIri, oo, classIri, annotationPropertyIri, 
						annotationValue) :
					GenericRdfConverter.createNamedIndividualWithTypeAndLabel(oo, classIri, annotationPropertyIri, 
						annotationValue);
				//System.out.println("Adding the following to variables: " + variableName + "\t" + oni);
				variables.put(variableName, oni);
				iriRepository.addIris(oni.getIRI(), null, repoAnnotations);

			}
		}		
	}

	public boolean evaluateCondition(DataObject dataObject) {
		boolean result = false;  // only set to true if one of the sublists has all true conditions
		for (ArrayList<String> condition : conditions) {
			boolean subConditionResult = true;  //any false within the list will change the "and" to false
			for (String subcondition : condition) {
				//check ==, !=, not-empty.  We can't handle any nesting at the moment, but we don't have the need, either.
				if (subcondition.contains("==")) {
					String[] flds = subcondition.split("==");
					String field = flds[0].trim().replace("[","").replace("]","");
					String value = flds[1];
					subConditionResult = subConditionResult && dataObject.getDataElementValue(field).equals(value);
				} else if (subcondition.contains("!=")) {
					String[] flds = subcondition.split("!=");
					String field = flds[0].trim().replace("[","").replace("]","");
					String value = flds[1];
					subConditionResult = subConditionResult && !dataObject.getDataElementValue(field).equals(value);
				} else if (subcondition.contains("not-empty")) {
					String field = subcondition.replace("not-empty","").trim().replace("[","").replace("]","");
					subConditionResult = subConditionResult && !dataObject.getDataElementValue(field).isEmpty();
				}
			}
			result = result || subConditionResult;
		}
		return result;
	}

	public void setIriSourceVariableName(String iriSourceVariableName) {
		this.iriSourceVariableName = iriSourceVariableName.replace("[","").replace("]","");
		System.out.println("Variable whose value is the IRI we'll give to individuals: "  + this.iriSourceVariableName);
	}

	public void setCreationConditionLogic(String creationConditionLogic) {
		/*
	 	 *  Need a data structure.  Could have list of lists.  If we take "and" as preference over "or", then we'd split on the "or" first.
	 	 *		that would give the outside list.  Splitting on "and" within the outside list creates a list entry inside each list.
	 	 *
	 	 *	Therefore, only one entry in the list of lists must evaluate to true for the entire expression to be true, then 
	 	 *		for any entry in the list of lists, all its entries must be true.  (inner lists are "and", outer list is "or")
		*/
		String[] flds = creationConditionLogic.split(" or ",-1);
		conditions = new ArrayList<ArrayList<String>>(flds.length);
		for (String fld : flds) {
			ArrayList<String> condition = new ArrayList<String>();
			if (!fld.isEmpty()) {
				String[] subflds = fld.split(" and ", -1);
				for (String subfld : subflds) {
					if (!subfld.isEmpty()) {
						condition.add(subfld);
					} else {
						condition.add("true");  // blanks don't make the "and" false
					}
				}
			} else {
				condition.add("false");  //blanks don't make the "or" true
			}
			conditions.add(condition);
		}
	}

}
