package edu.ufl.bmi.ontology;

import java.lang.StringBuilder;

import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Set;
import java.util.Map;

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
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;

import edu.ufl.bmi.misc.IndividualsToCreate;
import edu.ufl.bmi.misc.IriLookup;

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
	    try {
		oo = oom.createOntology(IRI.create("http://www.pitt.edu/dc/dtm"));
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

			Set<Map.Entry<String,JsonElement>> dtmAttSet = jo2.entrySet();
			Iterator<Map.Entry<String,JsonElement>> j = dtmAttSet.iterator();
			HashSet<String> reqInds = new HashSet<String>();
			while (j.hasNext()) {
			    Map.Entry<String,JsonElement> ej = j.next();
			    String keyj = ej.getKey();
			    allDtmAtt.add(keyj);
			    System.out.println("\t\t" + keyj);
			    HashSet<String> indsForKey = indsMap.get(keyj);
			    if (indsForKey != null) {
				reqInds.addAll(indsForKey);
				//System.out.println("adding inds for key " + keyj);
			    }
			}
			Iterator<String> k = reqInds.iterator();
			System.out.println("\t\t\treqInds.size() = " + reqInds.size());
			while(k.hasNext()) {
			    String ks = k.next();
			    System.out.println("\t\t\t" + ks + "\t" + iriMap.lookupClassIri(ks));
			}
		    }
		}
	    }

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
	OWLLiteral li = odf.getOWLLiteral(rdfsLabel);
	OWLAnnotationProperty la = odf.getOWLAnnotationProperty(labelPropIri);
	OWLAnnotation oa = odf.getOWLAnnotation(la, li);
	OWLAnnotationAssertionAxiom oaaa = odf.getOWLAnnotationAssertionAxiom(oni.getIRI(), oa);
	oom.addAxiom(oo, oaaa);
	return oni;
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
