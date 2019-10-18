package edu.ufl.bmi.ontology;

import java.io.InputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.semanticweb.owlapi.model.IRI;

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

public class RdfIriRepositoryWithJena implements IriRepository {

	String repositoryFileName;
	InputStream fileIn;
	Model m;

	public RdfIriRepositoryWithJena(String fileName) {
		this.repositoryFileName = fileName;
	}

	public Set<IRI> queryIris(IRI namedGraphIri, HashMap<IRI, String> propertyValuePairs) {
		StringBuilder queryTxt = new StringBuilder("select ?x\nwhere {\n");
		Iterator<IRI> i = propertyValuePairs.keySet().iterator();
		boolean first = true;
		while (i.hasNext()) {
			IRI propIri = i.next();
			String value = propertyValuePairs.get(propIri);
			if (!first)
				queryTxt.append(".\n");
			queryTxt.append("\t?x <");
			queryTxt.append(propIri.toString());
			queryTxt.append("> '");
			queryTxt.append(value.replace("'","\\'"));
			queryTxt.append("' ");
			first = false;


		}
		queryTxt.append("\n}");
		System.out.println("query is\n" + queryTxt + "\n\n");
		HashSet<IRI> result = new HashSet<IRI>();
		Query query = QueryFactory.create(queryTxt.toString()) ;
  		try (QueryExecution qexec = QueryExecutionFactory.create(query, m)) {
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
		return result;
	}

	public void initialize() {
		try {
			m = ModelFactory.createDefaultModel();
			File f = new File(repositoryFileName);
			if (f.exists() && !f.isDirectory()) {
				fileIn = new FileInputStream(f);
				m.read(fileIn, null);
				fileIn.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void addIris(IRI theIri, IRI namedGraphIri, HashMap<IRI, String> propertyValuePairs) {
		Resource theResource = m.createResource(theIri.toString());
		Iterator<IRI> keys = propertyValuePairs.keySet().iterator();
		while (keys.hasNext()) {
			IRI propertyIri = keys.next();
			String value = propertyValuePairs.get(propertyIri);
			Property property = m.getProperty(propertyIri.toString());
			theResource.addProperty(property, value);
		}
	}

	public void writeFile() {
		try {
			OutputStream os = new FileOutputStream(new File(repositoryFileName));
			m.write(os);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}