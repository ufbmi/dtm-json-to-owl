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

public class ForecasterEpidemicIndividualsCreator {

	static long iriCounter = 3916L;
    static String iriPrefix = "http://www.pitt.edu/obc/IDE_";
    static int iriLen = 10;

    static OWLOntologyManager oom;

    static IRI zikaEpidemicIri = IRI.create("http://www.pitt.edu/obc/IDE_0000000142");
    static IRI dzCourseIri = IRI.create("http://purl.obolibrary.org/obo/OGMS_0000063");
    static IRI dzIri = IRI.create("http://purl.obolibrary.org/obo/DOID_0060478");
    static IRI hostIri = IRI.create("http://purl.obolibrary.org/obo/NCBITaxon_9606");
    static IRI zikaFeverIri = IRI.create("http://purl.obolibrary.org/obo/DOID_0060478");

    static IRI propPartOccurrentIri = IRI.create("http://purl.obolibrary.org/obo/BFO_0000138");
    static IRI isAggregateOfIri = IRI.create("http://purl.obolibrary.org/obo/BFO_0000075");
    static IRI bearerOfIri = IRI.create("http://purl.obolibrary.org/obo/RO_0000053");
    static IRI hasParticipantIri = IRI.create("http://purl.obolibrary.org/obo/RO_0000057");
    static IRI realizesIri = IRI.create("http://purl.obolibrary.org/obo/BFO_0000055");

    static IRI edPrefTermIri = IRI.create("http://purl.obolibrary.org/obo/IAO_0000111");
    static IRI labelPropIri = IRI.create("http://www.w3.org/2000/01/rdf-schema#label");

    static OWLNamedIndividual epidemicInd,
    					dzCourseInd,
    					dzInd,
    					hostInd;

    static boolean epidemicExistsAlready;

	public static void main(String[] args) {
		FileOutputStream fos = null;

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

		try {
			FileReader fr = new FileReader("./src/main/resources/forecaster_pops_for_zika_forecast.txt");
			LineNumberReader lnr = new LineNumberReader(fr);
			String line;

			FileWriter epidemicIris = new FileWriter("./src/main/resources/iris_zika_epidemics.txt");

			while((line=lnr.readLine())!=null) {
				epidemicIris.write(line + "\t");
				/*
                    flds[0] = name of Zika population
                    flds[1] = IRI of Zika population
                    flds[2] = name of human population
                    flds[3] = IRI of human population
                    flds[4] = name of Aedes albopictus population
                    flds[5] = IRI of Aedes albopictus population
                    flds[6] = name of Aedes aegypti population
                    flds[7] = IRI of Aedes aegytpi population
                    flds[8] = start date of epidemic 
                    flds[9] = IRI of epidemic individual, which already exists if we get this IRI, else we have to create it.
                */
                String[] flds = line.split(Pattern.quote("\t"));
				System.out.println("number of fields = " + flds.length);

				epidemicExistsAlready = (flds.length == 10);
				if (epidemicExistsAlready)
					epidemicInd = odf.getOWLNamedIndividual(IRI.create(flds[9]));

				String regionShortName = flds[0].replace("Zika virus in ", "");
				String regionName = "region of " + regionShortName;
				String prefTerm = "Zika virus, humans, region of " + regionShortName + ", " + flds[8] + " to present";
				String label = "Zika virus, humans, " + regionShortName + ", " + flds[8] + " to present";
				createOwlNamedIndividuals(odf, oo, prefTerm, label, regionName);


				createOWLObjectPropertyAssertion(dzCourseInd, propPartOccurrentIri, epidemicInd, odf, oo);
				
				//dz realized by dz course
				createOWLObjectPropertyAssertion(dzCourseInd, realizesIri, dzInd, odf, oo);

				//host bearer of disease
				createOWLObjectPropertyAssertion(hostInd, bearerOfIri, dzInd, odf, oo);

				IRI hostPopIri = IRI.create(flds[3]);
				OWLNamedIndividual hostPopInd = odf.getOWLNamedIndividual(hostPopIri);
				
				//dz course aggregate has participant humans in region
				if (!epidemicExistsAlready)
					createOWLObjectPropertyAssertion(epidemicInd, hasParticipantIri, hostPopInd, odf, oo);

				//host is in aggregate humans in region
				createOWLObjectPropertyAssertion(hostPopInd, isAggregateOfIri, hostInd, odf, oo);

				if (!epidemicExistsAlready) {
					IRI zikaPopIri = IRI.create(flds[1]);
					OWLNamedIndividual zikaPopInd = odf.getOWLNamedIndividual(zikaPopIri);
					IRI aegyptiPopIri = IRI.create(flds[7]);
					OWLNamedIndividual aegyptiPopInd = odf.getOWLNamedIndividual(aegyptiPopIri);
					IRI alboPopIri = IRI.create(flds[5]);
					OWLNamedIndividual alboPopInd = odf.getOWLNamedIndividual(alboPopIri);
				
					//dz course aggregate has participant humans in region
					createOWLObjectPropertyAssertion(epidemicInd, hasParticipantIri, zikaPopInd, odf, oo);
					createOWLObjectPropertyAssertion(epidemicInd, hasParticipantIri, aegyptiPopInd, odf, oo);
					createOWLObjectPropertyAssertion(epidemicInd, hasParticipantIri, alboPopInd, odf, oo);

				}

				epidemicIris.write(epidemicInd.getIRI() + "\t" + prefTerm + "\n");

			}

			epidemicIris.close();

		} catch (IOException ioe) {
			ioe.printStackTrace();
		}

		//save OWL file
        try {
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
            String dateTxt = df.format(new Date());
            String owlFileName = "add-zika-to-obc-owl-file-ontology-" + dateTxt + ".owl";
            fos = new FileOutputStream(owlFileName);
            oom.saveOntology(oo, fos);
        } catch (OWLOntologyStorageException oose) {
            oose.printStackTrace();
        } catch (IOException ioe) {
        	ioe.printStackTrace();
        }
		
	}

	protected static void createOwlNamedIndividuals(OWLDataFactory odf, OWLOntology oo,
		String prefTerm, String label, String regionName) {
		
		if (!epidemicExistsAlready) {
			epidemicInd = createNamedIndividualWithIriTypeAndLabel(nextIri(), odf, oo, zikaEpidemicIri, labelPropIri, label );
			addAnnotationToNamedIndividual(epidemicInd, edPrefTermIri, prefTerm, odf, oo);
		}

		dzCourseInd = createNamedIndividualWithIriTypeAndLabel(nextIri(), odf, oo, 
			dzCourseIri, labelPropIri, buildDzCourseTerm(regionName, label) );
		addAnnotationToNamedIndividual(dzCourseInd, edPrefTermIri, 
			buildDzCourseTerm(regionName, prefTerm), odf, oo);

		dzInd = createNamedIndividualWithIriTypeAndLabel(nextIri(), odf, oo, 
			dzIri, labelPropIri, buildDzTerm(regionName, label) );
		addAnnotationToNamedIndividual(dzInd, edPrefTermIri, 
			buildDzTerm(regionName, prefTerm), odf, oo);

		hostInd = createNamedIndividualWithIriTypeAndLabel(nextIri(), odf, oo, 
			hostIri, labelPropIri, buildHostTerm(regionName, label) );
		addAnnotationToNamedIndividual(hostInd, edPrefTermIri, 
			buildHostTerm(regionName, prefTerm), odf, oo);

		/*
			We don't need this individual in the case of Zika

			ariInd = createNamedIndividualWithIriTypeAndLabel(nextIri(), odf, oo, 
				zikaFeverIri, labelPropIri, buildAriLabel(regionName) );
			addAnnotationToNamedIndividual(ariInd, edPrefTermIri, 
				buildAriEdPrefTerm(regionName), odf, oo);
		*/

	}

	protected static String buildDzCourseTerm(String regionName, String epidemicTerm) {
		StringBuilder sb = new StringBuilder();
		sb.append("disease course of one host in ");
		sb.append(regionName);
		sb.append(", and which is a proper occurrent part of the epidemic ");
		sb.append(epidemicTerm);
		return sb.toString();
	}

	protected static String buildDzTerm(String regionName, String epidemcTerm) {
		StringBuilder sb = new StringBuilder();
		sb.append("disease of one host in ");
		sb.append(regionName);
		sb.append(" during epidemic ");
		sb.append(epidemcTerm);
		return sb.toString();
	}

	protected static String buildHostTerm(String regionName, String epidemicTerm) {
		StringBuilder sb = new StringBuilder();
		sb.append("a host in ");
		sb.append(regionName);
		sb.append(" who is the bearer of a Zika fever disease realized in a disease course, ");
		sb.append("and that disease course is part of the epidemic ");
		sb.append(epidemicTerm);
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