package edu.ufl.bmi.ontology;

import java.io.FileReader;
import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.IRI;

import edu.ufl.bmi.misc.DataObject;
import edu.ufl.bmi.misc.IriLookup;

import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;

public class RdfConversionLookupMappingToIndividualInstruction extends RdfConversionInstruction {

	String variableName;
	String searchFieldName;
	//int searchFieldIndex;
	/*
	 *  TODO: we can update the instruction syntax to tell the execution specifically
	 *    which unique field to search.  Then we need only one search index here, which
	 *    we can look up prior to passing it here.
	 */
	HashMap<String, HashMap<String, OWLNamedIndividual>>  searchIndexes;
	String lookupFileLocation;
	String sparqlQueryTemplate;
	Model model;



	public RdfConversionLookupMappingToIndividualInstruction(IriLookup iriMap, OWLDataFactory odf, String variableName, String searchFieldName,
			HashMap<String, HashMap<String, OWLNamedIndividual>>  searchIndexes, String lookupFileLocation, String sparqlQueryTemplate) {
		super(iriMap, odf);
		this.variableName = variableName.replace("[","").replace("]","").trim();
		this.searchFieldName = searchFieldName;
		if (this.searchFieldName.startsWith("[")) this.searchFieldName = this.searchFieldName.substring(1);
		if (this.searchFieldName.endsWith("]")) this.searchFieldName = this.searchFieldName.substring(0, this.searchFieldName.length()-1);
		System.err.println("Search field name in RdfConversionLookupMappingToIndividualInstruction is " + this.searchFieldName);
		//this.searchFieldIndex = fieldNameToIndex.get(searchFieldName);
		this.searchIndexes = searchIndexes;
		this.lookupFileLocation = lookupFileLocation.replace("<","").replace(">","");
		this.sparqlQueryTemplate = sparqlQueryTemplate;
		setupModel();
	}

	protected void setupModel() {
		model = ModelFactory.createDefaultModel();
		try { FileReader fr = new FileReader(this.lookupFileLocation);
			model.read(fr, null);
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
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
	public void execute(OWLNamedIndividual rowIndividual, DataObject dataObject, DataObject parentObject, HashMap<String, OWLNamedIndividual> variables,
		 OWLOntology oo) {
		//Find the value of the field specified in this instruction, and search for named individuals created with that field value
		String fieldValue = dataObject.getDataElementValue(this.searchFieldName);
		//System.err.println("Searching for individual by " + this.searchFieldName + " = " + fieldValue);
		//System.out.println("lookup field value: " + fieldValue + " (is valid: " + validFieldValue(fieldValue) + "), lookup element name " + searchFieldName);
		if (validFieldValue(fieldValue)) {
			String queryTxt = this.sparqlQueryTemplate.replace("???", fieldValue);
			HashSet<IRI> result = new HashSet<IRI>();
			Query query = QueryFactory.create(queryTxt.toString()) ;

  			try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
   				ResultSet results = qexec.execSelect();
   				System.out.print("QUERY RESULT(S): ");
   				for ( ; results.hasNext() ; ) {
     				QuerySolution soln = results.nextSolution() ;
    				RDFNode x = soln.get("x") ;       // Get a result variable by name.
    				result.add(IRI.create(x.toString()));
    				System.out.print(x + "\t");
    			}
    			System.out.println("\n");
  			} catch (Exception e) {
  				e.printStackTrace();
  			}

  			if (result.size() > 1) {
  				System.err.println("LOOKUP MAPPING TO INDIVIDUAL INSTRUCTION: MORE THAN ONE QUERY RESULT: " + result.size() + " for query " + queryTxt);
  			} else if (result.size() < 1) {
  				System.err.println("LOOKUP MAPPING TO INDIVIDUAL INSTRUCTION: ZERO QUERY RESULTS: " + result.size() + " for query " + queryTxt);
  			} else {
				OWLNamedIndividual oni = odf.getOWLNamedIndividual(result.iterator().next());
				if (oni != null) {
					variables.put(variableName, oni);
					//System.out.println("added the following to hashmap " + variableName + "\t" + oni);
			
				}
			}
		} else {
			System.out.println("Bad field value in lookup mapping to individual instruction: '" + fieldValue + "'");
		}		
	}
}