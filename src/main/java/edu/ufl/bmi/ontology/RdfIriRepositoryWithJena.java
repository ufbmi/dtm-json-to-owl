package edu.ufl.bmi.ontology;

import java.io.InputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.util.Set;
import java.util.HashMap;
import java.util.Iterator;

import org.semanticweb.owlapi.model.IRI;

import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Property;

public class RdfIriRepositoryWithJena implements IriRepository {

	String repositoryFileName;
	InputStream fileIn;
	Model m;

	public RdfIriRepositoryWithJena(String fileName) {
		this.repositoryFileName = fileName;
	}

	public Set<IRI> queryIris(IRI namedGraphIri, HashMap<IRI, String> propertyValuePairs) {
		return null;
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