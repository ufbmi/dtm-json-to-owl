package edu.ufl.bmi.ontology;

import java.lang.StringBuilder;

import java.io.FileReader;
import java.io.IOException;
import java.io.FileOutputStream;
import java.util.Iterator;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Set;
import java.util.Map;
import java.util.ArrayList;
import java.util.Optional;
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


public class DtmJsonProcessor {
    static int iriCounter = 10;
    static String iriPrefix = "http://www.pitt.edu/dc/IDE_";
    static int iriLen = 10;

    static OWLOntologyManager oom;

    public static void main(String[] args) {
	try {
	    FileReader fr = new FileReader("./src/main/resources/software.json");
	    JsonParser jp = new JsonParser();
	    JsonElement je  = jp.parse(fr);

	    IndividualsToCreate itc = new IndividualsToCreate("./src/main/resources/individuals_required.txt");
	    itc.init();
	    HashMap<String, HashSet<String>> indsMap = itc.getIndividualsToCreate();
	    
	    IriLookup iriMap = new IriLookup("./src/main/resources/iris.txt");
	    iriMap.init();

	    oom = OWLManager.createOWLOntologyManager();
	    OWLDataFactory odf = OWLManager.getOWLDataFactory();
	    OWLOntology oo = null;
	    IRI ontologyIRI = IRI.create("http://www.pitt.edu/dc/dtm");
	    try {
		oo = oom.createOntology(ontologyIRI);
	    } catch (OWLOntologyCreationException ooce) {
		ooce.printStackTrace();
	    }
	    
	    HashSet<String> allDtmAtt = new HashSet<String>();
	    HashSet<String> dtmEntrySet = initializeDtmEntrySet(je);
	    System.out.println("entry set size = " + dtmEntrySet.size());

	    System.out.println("Main element is object: " + je.isJsonObject());

	    JsonObject jo = (JsonObject)je;
	    System.out.println(jo.size());
	    Set<Map.Entry<String,JsonElement>> jeSet = jo.entrySet();
	    System.out.println(jeSet.size());
	    Iterator<Map.Entry<String,JsonElement>> i = jeSet.iterator();
	    while(i.hasNext()) {
		Map.Entry<String,JsonElement> e = i.next();
		String key = e.getKey();
		//System.out.println(key);
		if (key.equals("settings")) 
		    continue;

		JsonElement je2 = e.getValue();
		JsonObject jo2 = (JsonObject)je2;
		JsonElement je3 = jo2.get("directory");
		if (je3.isJsonPrimitive()) {
		    JsonPrimitive jprim = (JsonPrimitive)je3;
		    String entryTxt = jprim.getAsString();
		    //System.out.println(" " + entryTxt);
		    if (dtmEntrySet.contains(entryTxt)) {
			System.out.println("\t"+ key  + " is a DTM.");

			//create OWL individual for DTM
			    //for it's dc:title, use the title entry in the JSON if available, else use the key variable
			    //for rdfs:label, title/key plus version
			    //sourceCodeRelease is URL to release of source code - annotate individual for DTM with hasURL for this
			    //for generalInfo key, create human readable synopsis annotation with the value

			//for doi key, create individual of type 'digital object identifier', annotate with rdfs:label = value.  Create denotes
			// relationship to dtm.

			//if key is version, create software version identifier individual, and connect it to DTM with denotes.  Annotate it with 
			//  value associated with key as rdfs:label.  If there is an executable individual connect version identifier to it with denotes.

			//whereas source is URL to source code repository generally - annotate indivdiual for source code repo with hasURL for this

			//executables: create individual, dc:title is label of DTM individual plus " executable", and the value here is hasURL.
			//  also need individual for process of compiling code.  need to link DTM individual to compiling individual, compiling
			//  individual to executable individual

			//documentation: individual for documentation, is about relationship to DTM.  dc:title is "Documentation for " + rdfs:label from dtm individual
			// ditto for userGuideAndManuals

			//license: create individual, annotate with rdfs:label = value, and create part of relationship to dtm individual.

			//webApplication: create individual for software execution, annotate with hasURL = value, and connect it to executable via
			//   realizes executable.  If executable doesn't exist, do same as executables, but no hasURL on the executable.

			//developer: split on commas, remove 'and', and for each entry: create individual of type IC, create individual of type
			// legal person role (value and value's legal person role, respectively), IC is bearer of role, IC actively participates
			// in process of writing dtm (another individual of type software development), that process has specified output dtm.

			/*
			  hostSpeciesIncluded
			  diseaseCoverage
			  isApolloEnabled
			  executables
			  webApplication
			  sourceCodeRelease
			  documentation
			  source
			  generalInfo
			  title
			  directory
			  version
			  userGuidesAndManuals
			  license
			  controlMeasures
			  publicationsThatUsedRelease
			  locationCoverage
			  location
			  developer
			  publicationsAboutRelease
			  isOlympus
			  doi

			  what's source vs. sourceCodeRelease
			*/

		        String baseName = key;
			String version = null;
			Set<Map.Entry<String,JsonElement>> dtmAttSet = jo2.entrySet();
			Iterator<Map.Entry<String,JsonElement>> j = dtmAttSet.iterator();
			HashSet<String> reqInds = new HashSet<String>();
			while (j.hasNext()) {
			    Map.Entry<String,JsonElement> ej = j.next();
			    String keyj = ej.getKey();
			    allDtmAtt.add(keyj);
			    System.out.println("\t\t" + keyj);
			    if (keyj.equals("title")) {
				JsonElement jej = ej.getValue();
				if (jej instanceof JsonPrimitive)
				    baseName = ((JsonPrimitive)jej).getAsString();
				else
				    System.err.println("title key does not have primitive value");
			    } else if (keyj.equals("version")) {
				JsonElement jej = ej.getValue();
				if (jej instanceof JsonPrimitive)
				    version = ((JsonPrimitive)jej).getAsString();
				else 
				    System.err.println("version key does not have primitive value");
			    }
			    HashSet<String> indsForKey = indsMap.get(keyj);
			    if (indsForKey != null) {
				reqInds.addAll(indsForKey);
				//System.out.println("adding inds for key " + keyj);
			    }
			}

			//System.out.println("base name = " + baseName + ", version = " + version);
			String baseLabel = (version == null) ? baseName : baseName + " - " + 
			    ((Character.isDigit(version.charAt(0))) ? " v" + version : version);
			System.out.println(baseLabel);
			Iterator<String> k = reqInds.iterator();
			//System.out.println("\t\t\treqInds.size() = " + reqInds.size());
			IRI labelIri = iriMap.lookupAnnPropIri("editor preferred");
			HashMap<String, OWLNamedIndividual> niMap = new HashMap<String, OWLNamedIndividual>();
			while(k.hasNext()) {
			    String ks = k.next();
			    IRI classIri = iriMap.lookupClassIri(ks);
			    System.out.println("\t\t\t" + ks + "\t" + classIri); 
			    OWLNamedIndividual oni = createNamedIndividualWithTypeAndLabel(odf, oo, classIri, labelIri, baseLabel + " " + ks); 
			    niMap.put(ks, oni);
			}

			//Once we've identified all the individuals we need, and we've created them, now we have to go back through
			// and stick stuff on the individuals
			j=dtmAttSet.iterator();
			while (j.hasNext()) {
			    Map.Entry<String,JsonElement> ej = j.next();
			    String keyj = ej.getKey();
			    if (keyj.equals("title")) {
				handleTitle(ej, niMap, oo, odf, iriMap);
			    } else if (keyj.equals("version")) {
				handleVersion(ej, niMap, oo, odf, iriMap);
			    } else if (keyj.equals("source")) {
				handleSource(ej, niMap, oo, odf, iriMap);
			    } else if (keyj.equals("license")) {
				handleLicense(ej, niMap, oo, odf, iriMap);
			    } else if (keyj.equals("doi")) {
				handleDoi(ej, niMap, oo, odf, iriMap);
			    } else if (keyj.equals("sourceCodeRelease")) {
				handleSourceCodeRelease(ej, niMap, oo, odf, iriMap);
			    } else if (keyj.equals("generalInfo")) {
				handleGeneralInfo(ej, niMap, oo, odf, iriMap);
			    } else if (keyj.equals("executables")) {
				handleExecutables(ej, niMap, oo, odf, iriMap);
			    } else if (keyj.equals("webApplication")) {
				handleWebApplication(ej, niMap, oo, odf, iriMap);
			    } else if (keyj.equals("location")) {
				handleLocation(ej, niMap, oo, odf, iriMap);
			    } else if (keyj.equals("documentation") || keyj.startsWith("userGuides")) {
				handleDocumentation(ej, niMap, oo, odf, iriMap);
			    } else if (keyj.equals("developer")) {
				handleDeveloper(ej, niMap, oo, odf, iriMap);
			    } else {
				System.out.println("WARNING: assuming that handling of " + keyj + " attribute will occur in manual, post-processing step.");
			    }
			}

			//Now, we need to connect up all the individuals
			connectDtmIndividuals(niMap, oo, odf, iriMap);
		    }

		}
	    
       
		try {
		    oom.saveOntology(oo, new FileOutputStream("./dtm-ontology.owl"));
		} catch (IOException ioe) {
		    ioe.printStackTrace();
		} catch (OWLOntologyStorageException oose) {
		    oose.printStackTrace();
		}
	    } //while(i.hasNext()).  i is iterating over settings plus the main payload.

		Iterator<String> si = allDtmAtt.iterator();
		while (si.hasNext()) {
		    System.out.println(si.next());
		}
		
		System.out.println(nextIri());
		System.out.println(nextIri());


	} catch (IOException ioe) {
	    ioe.printStackTrace();
	} catch (JsonIOException jioe) {
	    jioe.printStackTrace();
	} catch (JsonSyntaxException jse) {
	    jse.printStackTrace();
	}
    }

    public static void handleTitle(Map.Entry<String, JsonElement> e, HashMap<String, OWLNamedIndividual> niMap,
					   OWLOntology oo, OWLDataFactory odf, IriLookup iriMap) {
	OWLNamedIndividual oni = niMap.get("dtm");
        JsonElement je = e.getValue();
        if (je instanceof JsonPrimitive) {
	    String value = ((JsonPrimitive)je).getAsString();
	    addAnnotationToNamedIndividual(oni, iriMap.lookupAnnPropIri("title"), value, odf, oo);
	}
    }

    public static void handleVersion(Map.Entry<String, JsonElement> e, HashMap<String, OWLNamedIndividual> niMap,
					   OWLOntology oo, OWLDataFactory odf, IriLookup iriMap) {
	OWLNamedIndividual oni = niMap.get("versionid");
        JsonElement je = e.getValue();
        if (je instanceof JsonPrimitive) {
	    String value = ((JsonPrimitive)je).getAsString();
	    addAnnotationToNamedIndividual(oni, iriMap.lookupAnnPropIri("label"), value, odf, oo);
	}
    }

    public static void handleSource(Map.Entry<String, JsonElement> e, HashMap<String, OWLNamedIndividual> niMap,
					   OWLOntology oo, OWLDataFactory odf, IriLookup iriMap) {
	OWLNamedIndividual oni = niMap.get("sourcerepository");
        JsonElement je = e.getValue();
        if (je instanceof JsonPrimitive) {
	    String value = ((JsonPrimitive)je).getAsString();
	    addAnnotationToNamedIndividual(oni, iriMap.lookupAnnPropIri("hasURL"), value, odf, oo);
	}
    }

    public static void handleLicense(Map.Entry<String, JsonElement> e, HashMap<String, OWLNamedIndividual> niMap,
					   OWLOntology oo, OWLDataFactory odf, IriLookup iriMap) {
	OWLNamedIndividual oni = niMap.get("license");
        JsonElement je = e.getValue();
        if (je instanceof JsonPrimitive) {
	    String value = ((JsonPrimitive)je).getAsString();
	    if (value.contains("<a ")) {
		    Document d = Jsoup.parse(value);
		    Elements links = d.select("a");
		    String url = links.get(0).attr("href");
		    String txt = links.get(0).ownText();
		    addAnnotationToNamedIndividual(oni, iriMap.lookupAnnPropIri("label"), txt, odf, oo);
		    addAnnotationToNamedIndividual(oni, iriMap.lookupAnnPropIri("hasURL"), url, odf, oo);
	    } else {
		addAnnotationToNamedIndividual(oni, iriMap.lookupAnnPropIri("label"), value, odf, oo);
	    }
	}
    }

    public static void handleDoi(Map.Entry<String, JsonElement> e, HashMap<String, OWLNamedIndividual> niMap,
					   OWLOntology oo, OWLDataFactory odf, IriLookup iriMap) {
	OWLNamedIndividual oni = niMap.get("doi");
        JsonElement je = e.getValue();
        if (je instanceof JsonPrimitive) {
	    String value = ((JsonPrimitive)je).getAsString();
	    Document d = Jsoup.parse(value);
	    Elements links = d.select("a");
	    String url = links.get(0).attr("href");
	    String txt = links.get(0).ownText();
	    addAnnotationToNamedIndividual(oni, iriMap.lookupAnnPropIri("label"), txt, odf, oo);
	    addAnnotationToNamedIndividual(oni, iriMap.lookupAnnPropIri("hasURL"), url, odf, oo);
	}
    }

    public static void handleSourceCodeRelease(Map.Entry<String, JsonElement> e, HashMap<String, OWLNamedIndividual> niMap,
					   OWLOntology oo, OWLDataFactory odf, IriLookup iriMap) {
	OWLNamedIndividual oni = niMap.get("dtm");
        JsonElement je = e.getValue();
        if (je instanceof JsonPrimitive) {
	    String value = ((JsonPrimitive)je).getAsString();
	    String[] htmls = value.split(Pattern.quote(","));
	    for (String html : htmls) {
		Document d = Jsoup.parse(html);
		Elements links = d.select("a");
		String url = links.get(0).attr("href");
		String txt = links.get(0).ownText();
		addAnnotationToNamedIndividual(oni, iriMap.lookupAnnPropIri("hasURL"), url, odf, oo);
	    }
	}
    }

    public static void handleGeneralInfo(Map.Entry<String, JsonElement> e, HashMap<String, OWLNamedIndividual> niMap,
					   OWLOntology oo, OWLDataFactory odf, IriLookup iriMap) {
	OWLNamedIndividual oni = niMap.get("dtm");
        JsonElement je = e.getValue();
        if (je instanceof JsonPrimitive) {
	    String value = ((JsonPrimitive)je).getAsString();
	    addAnnotationToNamedIndividual(oni, iriMap.lookupAnnPropIri("synopsis"), value, odf, oo);
	}
    }

    public static void handleExecutables(Map.Entry<String, JsonElement> e, HashMap<String, OWLNamedIndividual> niMap,
					   OWLOntology oo, OWLDataFactory odf, IriLookup iriMap) {
	OWLNamedIndividual oni = niMap.get("executable");
        JsonElement je = e.getValue();
        if (je instanceof JsonPrimitive) {
            String value = ((JsonPrimitive)je).getAsString();
	    String[] execs = value.split(Pattern.quote(","));
	    ArrayList<OWLNamedIndividual> execNis = new ArrayList<OWLNamedIndividual>();
	    ArrayList<OWLNamedIndividual> compNis = new ArrayList<OWLNamedIndividual>();
	    System.out.println(value + " " + execs.length);
	    String labelPrefix = "";
	    if (execs[0].contains("<a ")) {
		Document d = Jsoup.parse(execs[0]);
		Elements links = d.select("a");
		String url = links.get(0).attr("href");
		String txt = links.get(0).ownText();
		addAnnotationToNamedIndividual(oni, iriMap.lookupAnnPropIri("hasURL"), url, odf, oo);
		addAnnotationToNamedIndividual(oni, iriMap.lookupAnnPropIri("label"), txt, odf, oo);
	    } else {
		//otherwise, if the executable value is not html, just put whatever is in there on as a URL.
		addAnnotationToNamedIndividual(oni, iriMap.lookupAnnPropIri("hasURL"), value, odf, oo);
	    }	   

	    //execNis.add(oni);
	    if (execs.length > 1) {
		for (int i=1; i<execs.length; i++) {
		    if (execs[i].trim().startsWith("<a ")) {
			Document d = Jsoup.parse(execs[i]);
			Elements links = d.select("a");
			String url = links.get(0).attr("href");
			String txt = links.get(0).ownText();
			
			OWLNamedIndividual execi = createNamedIndividualWithTypeAndLabel(odf, oo, iriMap.lookupClassIri("executable"), iriMap.lookupAnnPropIri("label"), txt);
			OWLNamedIndividual compi = createNamedIndividualWithTypeAndLabel(odf, oo, iriMap.lookupClassIri("compiling"), iriMap.lookupAnnPropIri("editor preferred"), 
											 "compiling to " + txt);
			addAnnotationToNamedIndividual(execi, iriMap.lookupAnnPropIri("hasURL"), url, odf, oo);
			execNis.add(execi);
			compNis.add(compi);

			Stream<OWLAnnotationAssertionAxiom> annStream = oo.annotationAssertionAxioms(oni.getIRI()).filter(w -> w.getProperty().getIRI().equals(iriMap.lookupAnnPropIri("editor preferred")));
			Optional<OWLAnnotationAssertionAxiom> ax = annStream.findFirst();
			if (ax.isPresent()) {
			    OWLAnnotation oa = (ax.get()).getAnnotation();
			    oom.addAxiom(oo, odf.getOWLAnnotationAssertionAxiom(execi.getIRI(), oa));
			}

		    } else {
			Stream<OWLAnnotationAssertionAxiom> annStream = oo.annotationAssertionAxioms(oni.getIRI()).filter(w -> w.getProperty().getIRI().equals(iriMap.lookupAnnPropIri("editor preferred")));
			Optional<OWLAnnotationAssertionAxiom> ax = annStream.findFirst();

			OWLNamedIndividual execi = createNamedIndividualWithTypeAndLabel(odf, oo, iriMap.lookupClassIri("executable"), iriMap.lookupAnnPropIri("hasURL"), execs[i].trim());
			if (ax.isPresent()) {
			    OWLAnnotation oa = (ax.get()).getAnnotation();
			    oom.addAxiom(oo, odf.getOWLAnnotationAssertionAxiom(execi.getIRI(), oa));
			}
			execNis.add(execi);
			
		    }
		}
	    }
	    //Some of the values for executables attribute is an html link to the documentation with link text.  In that case
	    // put the link on the executable individual with hasURL annotation, and put the link 
	    // text on that individual as an rdfs:label annotation.
	    for (int i=0; i<execNis.size(); i++) {
		OWLNamedIndividual execi = execNis.get(i);
		OWLNamedIndividual compi = compNis.get(i);
		String execKey = "executable" + i;
		String compKey = "compiling" + i;
		niMap.put(execKey, execi);
		niMap.put(compKey, compi);
	    }
	} else {
	    System.err.println("value of documentation attribute is not primitive.");
	}
    }

    public static void handleWebApplication(Map.Entry<String, JsonElement> e, HashMap<String, OWLNamedIndividual> niMap,
					   OWLOntology oo, OWLDataFactory odf, IriLookup iriMap) {
	OWLNamedIndividual oni = niMap.get("webexecutionof");
        JsonElement je = e.getValue();
        if (je instanceof JsonPrimitive) {
            String value = ((JsonPrimitive)je).getAsString();
	    //value is just a URL so slap it on the web execution of dtm individual
	    addAnnotationToNamedIndividual(oni, iriMap.lookupAnnPropIri("hasURL"), value, odf, oo);
	} else {
	    System.err.println("value of web application attribute is not primitive.");
	}	
    }

    public static void handleLocation(Map.Entry<String, JsonElement> e, HashMap<String, OWLNamedIndividual> niMap,
					   OWLOntology oo, OWLDataFactory odf, IriLookup iriMap) {
	OWLNamedIndividual oni = niMap.get("website");
        JsonElement je = e.getValue();
        if (je instanceof JsonPrimitive) {
            String value = ((JsonPrimitive)je).getAsString();
	    //value is just a URL so slap it on the website individual
	    addAnnotationToNamedIndividual(oni, iriMap.lookupAnnPropIri("hasURL"), value, odf, oo);
	} else {
	    System.err.println("value of location attribute is not primitive.");
	}	
    }

    public static void handleDocumentation(Map.Entry<String, JsonElement> e, HashMap<String, OWLNamedIndividual> niMap,
					   OWLOntology oo, OWLDataFactory odf, IriLookup iriMap) {
	OWLNamedIndividual oni = niMap.get("documentation");
        JsonElement je = e.getValue();
        if (je instanceof JsonPrimitive) {
            String value = ((JsonPrimitive)je).getAsString();
	    //Some of the documentation is an html link to the documentation with link text.  In that case
	    // put the link on the documentation individual with hasURL annotation, and put the link 
	    // text on that individual as an rdfs:label annotation.
	    if (value.startsWith("<a ")) {
		Document d = Jsoup.parse(value);
		Elements links = d.select("a");
		String url = links.get(0).attr("href");
		String txt = links.get(0).ownText();
		//System.out.println("URL IS " + url + " AND TEXT IS " + txt);
		addAnnotationToNamedIndividual(oni, iriMap.lookupAnnPropIri("hasURL"), url, odf, oo);
		addAnnotationToNamedIndividual(oni, iriMap.lookupAnnPropIri("label"), txt, odf, oo);
	    } else {
		//otherwise, if the documentation value is not html, just put whatever is in there on as a URL.
		addAnnotationToNamedIndividual(oni, iriMap.lookupAnnPropIri("hasURL"), value, odf, oo);
	    }
	} else {
	    System.err.println("value of documentation attribute is not primitive.");
	}
    }

    public static void handleDeveloper(Map.Entry<String, JsonElement> e, HashMap<String, OWLNamedIndividual> niMap,
				  OWLOntology oo, OWLDataFactory odf, IriLookup iriMap) {
	JsonElement je = e.getValue();
	if (je instanceof JsonPrimitive) {
	    String value = ((JsonPrimitive)je).getAsString();
	    String[] devs = value.split(Pattern.quote(","));
	    ArrayList<OWLNamedIndividual> devNis = new ArrayList<OWLNamedIndividual>();
	    OWLNamedIndividual oni = niMap.get("developer");
	    addAnnotationToNamedIndividual(oni, iriMap.lookupAnnPropIri("label"), devs[0], odf, oo);
	    devNis.add(oni);
	    if (devs.length > 1) {
		for (int i=1; i<devs.length; i++) {
		    devNis.add(createNamedIndividualWithTypeAndLabel(odf, oo, iriMap.lookupClassIri("developer"), iriMap.lookupAnnPropIri("label"), devs[i].trim()));
		}
	    }
	    for (int i=0; i<devNis.size(); i++) {
		OWLNamedIndividual lpri = createNamedIndividualWithTypeAndLabel(odf, oo, iriMap.lookupClassIri("legalpersonrole"), iriMap.lookupAnnPropIri("label"), "legal person role of " + devs[i].trim());
		OWLNamedIndividual devi = devNis.get(i);
		createOWLObjectPropertyAssertion(devi, iriMap.lookupObjPropIri("bearer"), lpri, odf, oo);
	    }
	} else {
	    System.err.println("Value for developer is not primitive!");
	}
    }

    public static HashSet<String> initializeDtmEntrySet(JsonElement je) {
	HashSet<String> dtmEntrySet = new HashSet<String>();
	dtmEntrySet.add("Disease transmission models");

	    if (je instanceof JsonObject) {
		System.out.println("object");
		JsonObject jo = (JsonObject)je;
	        JsonElement je2 = jo.get("settings");
		if (je2 instanceof JsonObject) {
		    System.out.println("object again");
		    System.out.println(je2);
		    JsonElement je3 = ((JsonObject)je2).get("directories");
		    if (je3 instanceof JsonArray) {
			System.out.println("array this time!");
			JsonArray ja = (JsonArray)je3;
			Iterator<JsonElement> i = ja.iterator();
			while (i.hasNext()) {
			    JsonElement jei = i.next();
			    System.out.println(jei + "\t" + jei.isJsonObject());
			    if (jei.isJsonObject()) {
				JsonObject jo2 = (JsonObject)jei;
				JsonElement jei1 = jo2.get("Disease transmission models");
				if (jei1 != null) {
				    System.out.println(jei1.isJsonArray());
				    JsonArray ja2 = (JsonArray)jei1;
				    Iterator<JsonElement> j = ja2.iterator();
				    while (j.hasNext()) {
					JsonElement jej = j.next();
					if (jej.isJsonPrimitive()) {
					    JsonPrimitive jp = (JsonPrimitive)jej;
					    System.out.println("\t\t" + jp.getAsString());
					    dtmEntrySet.add(jp.getAsString());
					    
					}
				    }
				}
			    }
			}
		    }
		} else if (je2 instanceof JsonArray) {
		    JsonArray ja = (JsonArray)je2;
		    System.out.println("array of size" + ja.size());
		} else {
		    System.out.println("something other than object or array");
		}
		//JsonArray ja = jo.getAsJsonArray("directories");
		//System.out.println(ja.size());
	    } else if (je instanceof JsonArray) {
		System.out.println("array");
	    }

	    return dtmEntrySet;
	
    }

    public static OWLNamedIndividual createNamedIndividualWithTypeAndLabel(
			   OWLDataFactory odf, OWLOntology oo, IRI classTypeIri, IRI labelPropIri, String rdfsLabel) {
	OWLNamedIndividual oni = odf.getOWLNamedIndividual(nextIri());
	OWLClassAssertionAxiom ocaa = odf.getOWLClassAssertionAxiom(odf.getOWLClass(classTypeIri), oni);
	oom.addAxiom(oo,ocaa);
	addAnnotationToNamedIndividual(oni, labelPropIri, rdfsLabel, odf, oo);
	/*OWLLiteral li = odf.getOWLLiteral(rdfsLabel);
	OWLAnnotationProperty la = odf.getOWLAnnotationProperty(labelPropIri);
	OWLAnnotation oa = odf.getOWLAnnotation(la, li);
	OWLAnnotationAssertionAxiom oaaa = odf.getOWLAnnotationAssertionAxiom(oni.getIRI(), oa);
	oom.addAxiom(oo, oaaa);*/
	return oni;
    }

    public static void addAnnotationToNamedIndividual(OWLNamedIndividual oni, IRI annPropIri, String value, OWLDataFactory odf, OWLOntology oo) {
	OWLLiteral li = odf.getOWLLiteral(value);
	OWLAnnotationProperty la = odf.getOWLAnnotationProperty(annPropIri);
	OWLAnnotation oa = odf.getOWLAnnotation(la, li);
	OWLAnnotationAssertionAxiom oaaa = odf.getOWLAnnotationAssertionAxiom(oni.getIRI(), oa);
	oom.addAxiom(oo, oaaa);	
    }

    public static void connectDtmIndividuals(HashMap<String, OWLNamedIndividual> niMap, OWLOntology oo, OWLDataFactory odf, IriLookup iriMap) {
	DtmIndivConnectGuide rules = new DtmIndivConnectGuide("./src/main/resources/obj_prop_assertions.txt");
	try {
	    rules.init();
	    ArrayList<DtmIndividConnectRule> rulesList = rules.getRules();
	    for (DtmIndividConnectRule rule : rulesList) {
		String sourceKey = rule.getSourceKey();
		String targetKey = rule.getTargetKey();
		if (niMap.containsKey(sourceKey) && niMap.containsKey(targetKey)) {
		    String objPropKey = rule.getObjPropKey();
		    OWLNamedIndividual source = niMap.get(sourceKey);
		    OWLNamedIndividual target = niMap.get(targetKey);
		    IRI objPropIri = iriMap.lookupObjPropIri(objPropKey);
		    createOWLObjectPropertyAssertion(source, objPropIri, target, odf, oo);
		}
	    }
	} catch (IOException ioe) {
	    ioe.printStackTrace();
	}
    }

    public static void createOWLObjectPropertyAssertion(OWLNamedIndividual source, IRI objPropIri, OWLNamedIndividual target, OWLDataFactory odf, OWLOntology oo) {
	OWLObjectProperty oop = odf.getOWLObjectProperty(objPropIri);
	OWLObjectPropertyAssertionAxiom oopaa = odf.getOWLObjectPropertyAssertionAxiom(oop, source, target);
	oom.addAxiom(oo, oopaa);
    }

    public static IRI nextIri() {
	String counterTxt = Integer.toString(iriCounter++);
	StringBuilder sb = new StringBuilder(iriPrefix);
	int numZero = 10-counterTxt.length();
	for (int i=0; i<numZero; i++) 
	    sb.append("0");
	sb.append(counterTxt);
	return IRI.create(new String(sb));
    }
}
