package edu.ufl.bmi.ontology;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLNamedIndividual;

import edu.ufl.bmi.misc.DataObject;
import edu.ufl.bmi.misc.IriLookup;

/*
 *  At the moment, this class is a shortcut.  Eventually, we're going to need for each element that takes
 *		as its value one of a finite set of values, a mapping from its values to an individual IRI.
 *
 *  Currently if we had two elements with the same text string that referred to different individuals,
 *    we would be stuck.  If two elements use different value strings to refer to the same 
 *    individual, then we could still hack that up, albeit unelegantly, in our current iris.txt
 *	  file.
*/
public class RdfConversionLookupByElementValueInstruction extends RdfConversionInstruction {

	String variableName;
	String lookupFieldName;
	//int searchFieldIndex;
	/*
	 *  TODO: we can update the instruction syntax to tell the execution specifically
	 *    which unique field to search.  Then we need only one search index here, which
	 *    we can look up prior to passing it here.
	 */
	HashMap<String, HashMap<String, OWLNamedIndividual>>  searchIndexes;

	public RdfConversionLookupByElementValueInstruction(IriLookup iriMap, OWLDataFactory odf, String variableName, String lookupFieldName,
			HashMap<String, HashMap<String, OWLNamedIndividual>>  searchIndexes) {
		super(iriMap, odf);
		this.variableName = variableName.replace("[","").replace("]","").trim();
		this.lookupFieldName = lookupFieldName;
		//this.searchFieldIndex = fieldNameToIndex.get(searchFieldName);
		this.searchIndexes = searchIndexes;
	}

	/*
	@Override
	public void execute(OWLNamedIndividual rowIndividual, ArrayList<String> recordFields, HashMap<String, OWLNamedIndividual> variables, OWLOntology oo) {
		//Find the value of the field specified in this instruction, and search for named individuals created with that field value
		String fieldValue = recordFields.get(fieldNameToIndex.get(this.searchFieldName));
		//System.out.println("lookup field value: " + fieldValue + " lookup field index " + searchFieldIndex);
		if (validFieldValue(fieldValue)) {
			for (HashMap<String,OWLNamedIndividual> searchIndex : searchIndexes) {
				OWLNamedIndividual oni = searchIndex.get(fieldValue);
				if (oni != null) {
					variables.put(variableName, oni);
					//System.out.println("added the following to hashmap " + variableName + "\t" + oni);
					break;
				}
			}
		} else {
			System.out.println("Bad field value: '" + fieldValue + "'");
		}
	}
	*/

	@Override
	public void execute(OWLNamedIndividual rowIndividual, DataObject dataObject, HashMap<String, OWLNamedIndividual> variables, OWLOntology oo) {
		//Find the value of the field specified in this instruction, and search for named individuals created with that field value
		String fieldValue = dataObject.getDataElementValue(this.lookupFieldName);
		//System.err.println("Searching for individual by " + this.lookupFieldName + " = " + fieldValue);
		//System.out.println("lookup field value: " + fieldValue + " (is valid: " + validFieldValue(fieldValue) + "), lookup element name " + searchFieldName);
		if (validFieldValue(fieldValue)) {
			OWLNamedIndividual oni = variables.get(fieldValue);
			if (oni != null) {
				variables.put(this.variableName, oni);
			}
		} else {
			System.out.println("Bad field value: '" + fieldValue + "'");
		}		
	}
}