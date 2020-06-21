package edu.ufl.bmi.ontology;

import java.util.Set;
import java.util.HashMap;
import org.semanticweb.owlapi.model.IRI;

public interface IriRepository {
	public Set<IRI> queryIris(IRI namedGraphIri, HashMap<IRI, String> propertyValuePairs);
	public Set<String> queryAnnotationValueForIri(IRI individualIri, IRI annotationIri);
	public void addIris(IRI theIri, IRI namedGraphIri, HashMap<IRI, String> propertyValuePairs);
}