package edu.ufl.bmi.ontology;

import java.io.*;
import java.lang.StringBuilder;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.google.gson.JsonParser;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.JsonArray;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import edu.ufl.bmi.misc.IndividualsToCreate;
import edu.ufl.bmi.misc.IriLookup;
import edu.ufl.bmi.misc.DtmIndivConnectGuide;
import edu.ufl.bmi.misc.DtmIndividConnectRule;
import edu.ufl.bmi.misc.ControlMeasureIriMapping;
import edu.ufl.bmi.misc.PublicationLinks;

public class ForecasterIndividualsCreator {

	static long iriCounter = 2354L;
    static String iriPrefix = "http://www.pitt.edu/obc/IDE_";
    static int iriLen = 10;

    static OWLOntologyManager oom;

    static IRI dzCourseAggregateIri = IRI.create("http://purl.obolibrary.org/obo/APOLLO_SV_00000578");
    static IRI dzCourseIri = IRI.create("http://purl.obolibrary.org/obo/OGMS_0000063");
    static IRI dzIri = IRI.create("http://purl.obolibrary.org/obo/OGMS_0000031");
    static IRI hostIri = IRI.create("http://purl.obolibrary.org/obo/NCBITaxon_9606");
    static IRI ariIri = IRI.create("http://purl.obolibrary.org/obo/APOLLO_SV_00000574");

    static IRI propPartOccurrentIri = IRI.create("http://purl.obolibrary.org/obo/BFO_0000138");
    static IRI isAggregateOfIri = IRI.create("http://purl.obolibrary.org/obo/BFO_0000075");
    static IRI bearerOfIri = IRI.create("http://purl.obolibrary.org/obo/RO_0000053");
    static IRI hasParticipantIri = IRI.create("http://purl.obolibrary.org/obo/RO_0000057");
    static IRI realizesIri = IRI.create("http://purl.obolibrary.org/obo/BFO_0000055");

    static IRI edPrefTermIri = IRI.create("http://purl.obolibrary.org/obo/IAO_0000111");
    static IRI labelPropIri = IRI.create("http://www.w3.org/2000/01/rdf-schema#label");

    static OWLNamedIndividual dzCourseAggregateInd,
    					dzCourseInd,
    					dzInd,
    					hostInd,
    					ariInd;

	public static void main(String[] args) {
		FileOutputStream fos = null;

		//int[] startYears = { 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 };
		int[] startYears = { 2013, 2014, 2015, 2016 };


		HashMap<String, String> regionNameToHumanPopIri = new HashMap<>();

		try {
			FileReader fr = new FileReader("./src/main/resources/forecaster_regions");
			LineNumberReader lnr = new LineNumberReader(fr);
			String line;
			while((line=lnr.readLine())!=null) {
				String[] flds = line.split(Pattern.quote("\t"));
				System.out.println("number of fields = " + flds.length);
				regionNameToHumanPopIri.put(flds[0], flds[2]);
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}

		oom = OWLManager.createOWLOntologyManager();
        OWLDataFactory odf = OWLManager.getOWLDataFactory();
        OWLOntology oo = null;
        IRI ontologyIRI = IRI.create("http://www.pitt.edu/mdc/forecaster_individuals.owl");
        try {
        	//oo = oom.createOntology(ontologyIRI);
        	oo = oom.loadOntologyFromOntologyDocument(new File("./obc-ide-indexing-instances-and-classes.owl"));
        } catch (OWLOntologyCreationException ooce) {
        	ooce.printStackTrace();
        }


		Set<String> keySet = regionNameToHumanPopIri.keySet();

		for (int i=0; i<startYears.length; i++) {
			for (String region : keySet) {
				createOwlNamedIndividuals(odf, oo, startYears[i], region);

				//dz course proper part of occurrent dz course aggregate
				createOWLObjectPropertyAssertion(dzCourseInd, propPartOccurrentIri, 
					dzCourseAggregateInd, odf, oo);

				//dz realized by dz course
				createOWLObjectPropertyAssertion(dzCourseInd, realizesIri, 
					dzInd, odf, oo);

				//host bearer of disease
				createOWLObjectPropertyAssertion(hostInd, bearerOfIri, 
					dzInd, odf, oo);

				IRI hostPopIri = IRI.create(regionNameToHumanPopIri.get(region));
				OWLNamedIndividual hostPopInd = odf.getOWLNamedIndividual(hostPopIri);
				
				//dz course aggregate has participant humans in region
				createOWLObjectPropertyAssertion(dzCourseAggregateInd, hasParticipantIri, 
					hostPopInd, odf, oo);

				//host is in aggregate humans in region
				createOWLObjectPropertyAssertion(hostPopInd, isAggregateOfIri, 
					hostInd, odf, oo);

				//dz course has participant the ari
				createOWLObjectPropertyAssertion(dzCourseInd, hasParticipantIri, 
					ariInd, odf, oo);

				System.out.println(startYears[i] + "\t" + region + "\t" + dzCourseAggregateInd.getIRI()
					+ "\t" + hostPopIri);
			}
		}

		//save OWL file
        try {
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
            String dateTxt = df.format(new Date());
            String owlFileName = "add-to-obc-owl-file-ontology-" + dateTxt + ".owl";
            fos = new FileOutputStream(owlFileName);
            oom.saveOntology(oo, fos);
        } catch (OWLOntologyStorageException oose) {
            oose.printStackTrace();
        } catch (IOException ioe) {
        	ioe.printStackTrace();
        }
		
	}

	protected static void createOwlNamedIndividuals(OWLDataFactory odf, OWLOntology oo,
		int startYear, String regionName) {
		dzCourseAggregateInd = createNamedIndividualWithIriTypeAndLabel(nextIri(), odf, oo, 
			dzCourseAggregateIri, labelPropIri, buildDzCourseAggregateLabel(startYear, regionName) );
		addAnnotationToNamedIndividual(dzCourseAggregateInd, edPrefTermIri, 
			buildDzCourseAggregateEdPrefTerm(startYear, regionName), odf, oo);

		dzCourseInd = createNamedIndividualWithIriTypeAndLabel(nextIri(), odf, oo, 
			dzCourseIri, labelPropIri, buildDzCourseLabel(startYear, regionName) );
		addAnnotationToNamedIndividual(dzCourseInd, edPrefTermIri, 
			buildDzCourseEdPrefTerm(startYear, regionName), odf, oo);

		dzInd = createNamedIndividualWithIriTypeAndLabel(nextIri(), odf, oo, 
			dzIri, labelPropIri, buildDzLabel(startYear, regionName) );
		addAnnotationToNamedIndividual(dzInd, edPrefTermIri, 
			buildDzEdPrefTerm(startYear, regionName), odf, oo);

		hostInd = createNamedIndividualWithIriTypeAndLabel(nextIri(), odf, oo, 
			hostIri, labelPropIri, buildHostLabel(startYear, regionName) );
		addAnnotationToNamedIndividual(hostInd, edPrefTermIri, 
			buildHostEdPrefTerm(startYear, regionName), odf, oo);

		ariInd = createNamedIndividualWithIriTypeAndLabel(nextIri(), odf, oo, 
			ariIri, labelPropIri, buildAriLabel(startYear, regionName) );
		addAnnotationToNamedIndividual(ariInd, edPrefTermIri, 
			buildAriEdPrefTerm(startYear, regionName), odf, oo);
	}

	protected static String buildDzCourseAggregateEdPrefTerm(int startYear, String regionName) {
		StringBuilder sb = new StringBuilder();
		sb.append("aggregate of disease courses in which an acute respiratory illness is a participant in ");
		sb.append(regionName);
		sb.append(" from October ");
		sb.append(startYear);
		sb.append(" through May ");
		sb.append((startYear+1));
		return sb.toString();
	}

	protected static String buildDzCourseAggregateLabel(int startYear, String regionName) {
		StringBuilder sb = new StringBuilder();
		sb.append("all disease courses for an acute respiratory illness between October ");
		sb.append(startYear);
		sb.append(" and May ");
		sb.append((startYear+1));
		sb.append(" in ");
		sb.append(regionName);
		return sb.toString();
	}

	protected static String buildDzCourseEdPrefTerm(int startYear, String regionName) {
		StringBuilder sb = new StringBuilder();
		sb.append("disease course of one host in ");
		sb.append(regionName);
		sb.append(" that is a proper occurrent part of the aggregate of disease courses that ");
		sb.append("have an acute respiratory illness phenotype as participant from October ");
		sb.append(startYear);
		sb.append(" through May ");
		sb.append((startYear+1));
		return sb.toString();
	}

	protected static String buildDzCourseLabel(int startYear, String regionName) {
		StringBuilder sb = new StringBuilder();
		sb.append("disease course of one host in ");
		sb.append(regionName);
		sb.append(" that is one of all disease courses of acute respiratory illness from October ");
		sb.append(startYear);
		sb.append(" through May ");
		sb.append((startYear+1));
		return sb.toString();
	}

	protected static String buildDzEdPrefTerm(int startYear, String regionName) {
		StringBuilder sb = new StringBuilder();
		sb.append("disease of one host in ");
		sb.append(regionName);
		sb.append(" between October ");
		sb.append(startYear);
		sb.append(" and May ");
		sb.append((startYear+1));
		return sb.toString();
	}

	protected static String buildDzLabel(int startYear, String regionName) {
		return buildDzEdPrefTerm(startYear, regionName);
	}

	protected static String buildHostEdPrefTerm(int startYear, String regionName) {
		StringBuilder sb = new StringBuilder();
		sb.append("a host in ");
		sb.append(regionName);
		sb.append(" who is the bearer of a disease realized in a disease course, with an acute");
		sb.append(" respiratory illness phenotype as participant and that occurred between October ");
		sb.append(startYear);
		sb.append(" and May ");
		sb.append((startYear+1));
		return sb.toString();
	}

	protected static String buildHostLabel(int startYear, String regionName) {
		StringBuilder sb = new StringBuilder();
		sb.append("a host in ");
		sb.append(regionName);
		sb.append(" who had an acute respiratory illness between October ");
		sb.append(startYear);
		sb.append(" and May ");
		sb.append((startYear+1));
		return sb.toString();
	}

	protected static String buildAriEdPrefTerm(int startYear, String regionName) {
		StringBuilder sb = new StringBuilder();
		sb.append("the acute respiratory illness phenotype that participates in the disease course of a host in ");
		sb.append(regionName);
		sb.append(", and where that disease course occurred between October ");
		sb.append(startYear);
		sb.append(" and May ");
		sb.append((startYear+1));
		return sb.toString();
	}

	protected static String buildAriLabel(int startYear, String regionName) {
		StringBuilder sb = new StringBuilder();
		sb.append("the acute respiratory illness phenotype of a host in ");
		sb.append(regionName);
		sb.append(" that manifested in a disease course between October ");
		sb.append(startYear);
		sb.append(" and May ");
		sb.append((startYear+1));
		return sb.toString();
	}

	public static OWLNamedIndividual createNamedIndividualWithIriTypeAndLabel(
            IRI individualIri, OWLDataFactory odf, OWLOntology oo, IRI classTypeIri, IRI labelPropIri, String rdfsLabel) {
        OWLNamedIndividual oni = odf.getOWLNamedIndividual(individualIri);
        OWLClassAssertionAxiom ocaa = odf.getOWLClassAssertionAxiom(odf.getOWLClass(classTypeIri), oni);
        oom.addAxiom(oo, ocaa);
        addAnnotationToNamedIndividual(oni, labelPropIri, rdfsLabel, odf, oo);
        return oni;
    }

    public static void addAnnotationToNamedIndividual(OWLNamedIndividual oni, IRI annPropIri, String value, OWLDataFactory odf, OWLOntology oo) {
        OWLLiteral li = odf.getOWLLiteral(value);
        OWLAnnotationProperty la = odf.getOWLAnnotationProperty(annPropIri);
        OWLAnnotation oa = odf.getOWLAnnotation(la, li);
        OWLAnnotationAssertionAxiom oaaa = odf.getOWLAnnotationAssertionAxiom(oni.getIRI(), oa);
        oom.addAxiom(oo, oaaa);
    }

    public static void createOWLObjectPropertyAssertion(OWLNamedIndividual source, IRI objPropIri, OWLNamedIndividual target, OWLDataFactory odf, OWLOntology oo) {
        OWLObjectProperty oop = odf.getOWLObjectProperty(objPropIri);
        OWLObjectPropertyAssertionAxiom oopaa = odf.getOWLObjectPropertyAssertionAxiom(oop, source, target);
        oom.addAxiom(oo, oopaa);
    }



	public static IRI nextIri() {
        String counterTxt = Long.toString(iriCounter++);
        StringBuilder sb = new StringBuilder(iriPrefix);
        int numZero = 10 - counterTxt.length();
        for (int i = 0; i < numZero; i++)
            sb.append("0");
        sb.append(counterTxt);
        return IRI.create(new String(sb));
    }
}