package edu.ufl.bmi.ontology;

import java.lang.StringBuilder;

import java.io.FileReader;
import java.io.FileWriter;
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
    static long iriCounter = 1200006700L;
    static String iriPrefix = "http://www.pitt.edu/obc/IDE_ARTICLE_";
    static int iriLen = 10;

    static OWLOntologyManager oom;

    static String baseName;
    static String[] versionNames;
    static String[] fullNames;

    static String versionSuffix;
    static String fullName;

    static OWLNamedIndividual olympus;
    static OWLNamedIndividual uids;
    static OWLNamedIndividual uidsCompiling;
    static OWLNamedIndividual uidsExecutable;
    static OWLNamedIndividual uidsExecutableConcretization;

    public static void main(String[] args) {
	try {
	    FileReader fr = new FileReader("../digital-commons/src/main/webapp/resources/hardcoded-software.json");
	    //FileReader fr = new FileReader("./src/main/resources/software.json");
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

	    olympus = createOlympusIndividuals(odf, oo, iriMap);
	    uids = createUidsIndividuals(odf, oo, iriMap);

	    HashSet<String> allDtmAtt = new HashSet<String>();
	    HashSet<String> dtmEntrySet = initializeDtmEntrySet(je);
	    //System.out.println("entry set size = " + dtmEntrySet.size());

	    System.out.println("Main element is object: " + je.isJsonObject());

	    JsonObject jo = (JsonObject)je;
	    System.out.println(jo.size());
	    Set<Map.Entry<String,JsonElement>> jeSet = jo.entrySet();
	    System.out.println(jeSet.size());
	    Iterator<Map.Entry<String,JsonElement>> i = jeSet.iterator();

	    HashSet<String> uniqueLocationsCovered = new HashSet<String>();
	    HashSet<String> uniquePathogensCovered = new HashSet<String>();
	    HashSet<String> uniqueHostsCovered = new HashSet<String>();
	    HashMap<String, ArrayList<String>> popsNeededByDtm = new HashMap<String, ArrayList<String>>();
	    HashSet<String> populationsNeeded = new HashSet<String>();
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
			baseName = key;

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
			    if (keyj.equals("title")) {
				JsonElement jej = ej.getValue();
				if (jej instanceof JsonPrimitive)
				    baseName = ((JsonPrimitive)jej).getAsString();
				else {
				    System.err.println("title key does not have primitive value");
				    throw new IllegalArgumentException("title element may not be something other than primitive!");
				}
			    } else if (keyj.equals("version")) {
				JsonElement jej = ej.getValue();
				if (jej instanceof JsonPrimitive) {
				    /* versionNames = new String[1];
				    versionNames[0] = ((JsonPrimitive)jej).getAsString();
				    System.out.println("CAUTION: There are still version primitives!"); */
				    throw new IllegalArgumentException("Version element may not be primitive");
				}
				else {
				    //System.err.println("version key does not have primitive value");
				    //System.err.println("it's type is instead " + jej.getClass());
				    JsonArray versionArray = (JsonArray)jej;
				    Iterator<JsonElement> vIter = versionArray.iterator();
				    versionNames = new String[versionArray.size()];
				    int vIndex = 0;
				    while (vIter.hasNext()) {
					JsonElement vElement = vIter.next();
					//System.out.println("Version element is " + vElement.getClass());
					if (vElement instanceof JsonPrimitive) {
					    versionNames[vIndex++] = ((JsonPrimitive)vElement).getAsString();
					} else {
					    System.err.println("Version element is not primitive!!!");
					}
				    }

				    if ((baseName.contains("FluTE") || baseName.contains("NAADSM"))&& versionNames.length > 1) {
					versionSuffix = "";
					for (int iName=0; iName<versionNames.length; iName++) {
					    versionSuffix += versionNames[iName] + ((iName<versionNames.length-1) ? ", " : "");
					}
				    } else if (baseName.contains("GLEAM") && versionNames.length > 1) {
					versionSuffix = "";
					for (int iName=0; iName<versionNames.length; iName++) {
					    if (versionNames[iName].contains("Server")) {
						versionSuffix = versionNames[iName];
					    }
					}
				    } else {
					versionSuffix = versionNames[0];
				    }
				    System.out.println("Number of versions is : " + vIndex);
				}
			    }
			    HashSet<String> indsForKey = indsMap.get(keyj);
			    if (indsForKey != null) {
				reqInds.addAll(indsForKey);
				//System.out.println("adding inds for key " + keyj);
			    }
			}

			//System.out.println("base name = " + baseName + ", version = " + version);
			String baseLabel = (versionNames == null) ? baseName : baseName + " - " + 
			    ((Character.isDigit(versionSuffix.charAt(0))) ? " v" + versionSuffix : versionSuffix);
			fullName = baseLabel;
			System.out.println(fullName);
			Iterator<String> k = reqInds.iterator();
			//System.out.println("\t\t\treqInds.size() = " + reqInds.size());
			IRI labelIri = iriMap.lookupAnnPropIri("editor preferred");
			HashMap<String, OWLNamedIndividual> niMap = new HashMap<String, OWLNamedIndividual>();
			while(k.hasNext()) {
			    String ks = k.next();
			    IRI classIri = iriMap.lookupClassIri(ks);
			    //System.out.println("\t\t\t'" + ks + "'\t" + classIri); 
			    OWLNamedIndividual oni = createNamedIndividualWithTypeAndLabel(odf, oo, classIri, labelIri, fullName + " " + ks); 
			    niMap.put(ks, oni);
			}

			//Once we've identified all the individuals we need, and we've created them, now we have to go back through
			// and stick stuff on the individuals
			j=dtmAttSet.iterator();
			HashSet<String> locations = new HashSet<String>();
			HashSet<String> pathogens = new HashSet<String>();
			HashSet<String> hosts = new HashSet<String>();

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
			    } else if (keyj.equals("location") || keyj.equals("site")) {
				handleLocation(ej, niMap, oo, odf, iriMap);
			    } else if (keyj.equals("documentation") || keyj.startsWith("userGuides")) {
				handleDocumentation(ej, niMap, oo, odf, iriMap);
			    } else if (keyj.equals("developer")) {
				handleDeveloper(ej, niMap, oo, odf, iriMap);
			    } else if (keyj.equals("publicationsThatUsedRelease")) {
				handlePublicationsThatUsedRelease(ej, niMap, oo, odf, iriMap);
			    } else if (keyj.equals("availableAt")) {
				handleAvailableAt(ej, niMap, oo, odf, iriMap);
			    } else if (keyj.equals("isUdsi")) {
				OWLNamedIndividual executableInd = niMap.get("executable");
				OWLNamedIndividual execConcInd = niMap.get("executableConcretization");
				JsonElement elem = ej.getValue();
				String value = ((JsonPrimitive)elem).getAsString();
				handleUdsi(value, 1, executableInd, execConcInd, iriMap, odf, oo);				
			    } else {
				JsonElement jeRemainder = ej.getValue();
				if (jeRemainder instanceof JsonPrimitive) {
				    String value = ((JsonPrimitive)jeRemainder).getAsString();
				
				} else if (jeRemainder instanceof JsonArray) {
				    JsonArray remArray = (JsonArray)jeRemainder;
				    Iterator<JsonElement> remIter = remArray.iterator();
				    while (remIter.hasNext()) {
					JsonElement remNext = remIter.next();
					if (remNext instanceof JsonPrimitive) {
					    String value = ((JsonPrimitive)remNext).getAsString();
					    
					    if (keyj.equals("locationCoverage")) {
						//System.out.println("LOCATION: " + value);
						if (!value.equals("N/A")) {
						    uniqueLocationsCovered.add(value);
						    locations.add(value);
						}
					    } else if (keyj.equals("diseaseCoverage") || keyj.equals("pathogenCoverage")) {
						if (!value.equals("N/A")) {
						    uniquePathogensCovered.add(value);
						    pathogens.add(value);
						}
					    } else if (keyj.equals("hostSpeciesIncluded")) {
						if (!value.equals("N/A")) {
						    uniqueHostsCovered.add(value);
						    hosts.add(value);
						}
					    }
					    
					} else {
					    throw new IllegalArgumentException("ERROR: element " + keyj + "has array of values that are complex.");
					}
				    }
				    
				} else { 
				    System.err.println("jeRemainder instanceof " + jeRemainder.getClass());
				}
				System.out.println("WARNING: assuming that handling of " + keyj + " attribute will occur in manual, post-processing step. values " + ej.getValue());
			    }
			}

			//Now, we need to connect up all the individuals
			connectDtmIndividuals(niMap, oo, odf, iriMap);

			//System.out.println(locations.size());
			//System.out.println(pathogens.size());
			//System.out.println(hosts.size());

			ArrayList<String> popsForThisDtm = new ArrayList<String>();
			for (String loci : locations) {
			    for (String path : pathogens) {
				String pop = path + " in region of " + loci;
				populationsNeeded.add(pop);
				popsForThisDtm.add(pop);
				//System.out.println(pop);
			    }
			    for (String host : hosts) {
				String pop = host + " in region of " + loci;
				populationsNeeded.add(pop);
				popsForThisDtm.add(pop);
				//System.out.println(pop);
			    }
			}
			popsNeededByDtm.put(fullName, popsForThisDtm);
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

		System.out.println("Locations required:");
		for (String location : uniqueLocationsCovered) {
		    System.out.println("\t" + location);
		}
		System.out.println();

		System.out.println("Pathogens required:");
		for (String pathogen : uniquePathogensCovered) {
		    System.out.println("\t" + pathogen);
		}
		System.out.println();
	    
		System.out.println("Hosts required: ");
		for (String host : uniqueHostsCovered) {
		    System.out.println("\t" + host);
		}
		System.out.println();       

		System.out.println("Populations required: ");
		for (String pop : populationsNeeded) {
		    System.out.println("\t" + pop);
		}
		System.out.println();       

		FileWriter fw = new FileWriter("./pops_by_dtm.txt");
		int iPop = 1;
		Set<String> dtmsWithPops = popsNeededByDtm.keySet();
		for (String dtm : dtmsWithPops) {
		    ArrayList<String> popsNeeded = popsNeededByDtm.get(dtm);
		    for (String pop : popsNeeded) {
			System.out.println(iPop + "\t" + dtm + "\t" + pop);
			fw.write(iPop + "\t" + dtm + "\t" + pop + "\n");
			iPop++;
		    }
		}
		fw.close();
					     

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
	} else {
	    System.err.println("Title attribute has value that is not primitive.");
	}
    }

    public static void handleVersion(Map.Entry<String, JsonElement> e, HashMap<String, OWLNamedIndividual> niMap,
					   OWLOntology oo, OWLDataFactory odf, IriLookup iriMap) {
	OWLNamedIndividual oni = niMap.get("versionid");
	addAnnotationToNamedIndividual(oni, iriMap.lookupAnnPropIri("label"), versionSuffix, odf, oo);
    }

    public static void handleSource(Map.Entry<String, JsonElement> e, HashMap<String, OWLNamedIndividual> niMap,
					   OWLOntology oo, OWLDataFactory odf, IriLookup iriMap) {
	OWLNamedIndividual oni = niMap.get("sourcerepository");
        JsonElement je = e.getValue();
        if (je instanceof JsonPrimitive) {
	    String value = ((JsonPrimitive)je).getAsString();
	    addAnnotationToNamedIndividual(oni, iriMap.lookupAnnPropIri("hasURL"), value, odf, oo);
	} else {
	    System.err.println("Source attribute has value that is not primitive.");
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
	} else {
	    System.err.println("License attribute has value that is not primitive.");
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
	} else {
	    System.err.println("DOI attribute has value that is not primitive.");
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
	} else {
	    System.err.println("Source code release attribute has value that is not primitive.");
	}
    }

    public static void handleGeneralInfo(Map.Entry<String, JsonElement> e, HashMap<String, OWLNamedIndividual> niMap,
					   OWLOntology oo, OWLDataFactory odf, IriLookup iriMap) {
	OWLNamedIndividual oni = niMap.get("dtm");
        JsonElement je = e.getValue();
        if (je instanceof JsonPrimitive) {
	    String value = ((JsonPrimitive)je).getAsString();
	    addAnnotationToNamedIndividual(oni, iriMap.lookupAnnPropIri("synopsis"), value, odf, oo);
	} else {
	    System.err.println("General info attribute has value that is not primitive.");
	}
    }

    public static void handleExecutables(Map.Entry<String, JsonElement> e, HashMap<String, OWLNamedIndividual> niMap,
					   OWLOntology oo, OWLDataFactory odf, IriLookup iriMap) {
	OWLNamedIndividual execInd = niMap.get("executable");
	OWLNamedIndividual compInd = niMap.get("compiling");

	String execIndPrefTerm = getAnnotationValueFromNamedIndividual(execInd, iriMap.lookupAnnPropIri("editor preferred"), oo);
	String compIndPrefTerm = getAnnotationValueFromNamedIndividual(execInd, iriMap.lookupAnnPropIri("editor preferred"), oo);

        JsonElement je = e.getValue();
        if (je instanceof JsonArray) {
	    JsonArray elemArray = (JsonArray)je;
	    Iterator<JsonElement> elemIter = elemArray.iterator();
	    String[] execs = new String[elemArray.size()];
	    int iExec = 0;
	    while (elemIter.hasNext()) {
		JsonElement elemi = elemIter.next();
		String value = ((JsonPrimitive)elemi).getAsString();
		execs[iExec++] = value;
	    }

	    ArrayList<OWLNamedIndividual> execNis = new ArrayList<OWLNamedIndividual>();
	    ArrayList<OWLNamedIndividual> compNis = new ArrayList<OWLNamedIndividual>();

	    String labelPrefix = "";
	    if (execs[0].contains("<a ")) {
		Document d = Jsoup.parse(execs[0]);
		Elements links = d.select("a");
		String url = links.get(0).attr("href");
		String txt = links.get(0).ownText();
		addAnnotationToNamedIndividual(execInd, iriMap.lookupAnnPropIri("hasURL"), url, odf, oo);
		addAnnotationToNamedIndividual(execInd, iriMap.lookupAnnPropIri("label"), txt, odf, oo);
	    } else {
		//otherwise, if the executable value is not html, just put whatever is in there on as a URL.
		addAnnotationToNamedIndividual(execInd, iriMap.lookupAnnPropIri("hasURL"), execs[0], odf, oo);
	    }	   

	    //execNis.add(oni);
	    //System.err.println("there are " + execs.length + " executables");
	    if (execs.length > 1) {
		for (int i=1; i<execs.length; i++) {
		    if (execs[i].trim().startsWith("<a ")) {
			Document d = Jsoup.parse(execs[i]);
			Elements links = d.select("a");
			String url = links.get(0).attr("href");
			String txt = links.get(0).ownText();
			
			String newExecIndPrefTerm = execIndPrefTerm + " " + txt;
			String newCompIndPrefTerm = "compiling to " + newExecIndPrefTerm;
			OWLNamedIndividual execi = createNamedIndividualWithTypeAndLabel(odf, oo, iriMap.lookupClassIri("executable"), iriMap.lookupAnnPropIri("label"), txt);
			OWLNamedIndividual compi = createNamedIndividualWithTypeAndLabel(odf, oo, iriMap.lookupClassIri("compiling"), iriMap.lookupAnnPropIri("editor preferred"), newCompIndPrefTerm);
			addAnnotationToNamedIndividual(execi, iriMap.lookupAnnPropIri("hasURL"), url, odf, oo);
			addAnnotationToNamedIndividual(execi, iriMap.lookupAnnPropIri("editor preferred"), newExecIndPrefTerm, odf, oo);			

			execNis.add(execi);
			compNis.add(compi);

		    } else {
			String newExecIndPrefTerm = execIndPrefTerm + " " + execs[i];
			String newCompIndPrefTerm = "compiling to " + newExecIndPrefTerm;
			OWLNamedIndividual execi = createNamedIndividualWithTypeAndLabel(odf, oo, iriMap.lookupClassIri("executable"), iriMap.lookupAnnPropIri("hasURL"), execs[i].trim());
			OWLNamedIndividual compi = createNamedIndividualWithTypeAndLabel(odf, oo, iriMap.lookupClassIri("compiling"), iriMap.lookupAnnPropIri("editor preferred"), newCompIndPrefTerm);
			addAnnotationToNamedIndividual(execi, iriMap.lookupAnnPropIri("editor preferred"), newExecIndPrefTerm, odf, oo);			

			execNis.add(execi);
			compNis.add(compi);
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
	    System.err.println("Executables  attribute has value that is not array.");
	    throw new IllegalArgumentException("executables must be an array and isn't");
	}
    }


    public static String getAnnotationValueFromNamedIndividual(OWLNamedIndividual oni, IRI annPropIri, OWLOntology oo) {
	String annValue = null;
	
	Stream<OWLAnnotationAssertionAxiom> annStream = oo.annotationAssertionAxioms(oni.getIRI()).filter(w -> w.getProperty().getIRI().equals(annPropIri));
	Optional<OWLAnnotationAssertionAxiom> ax = annStream.findFirst();
	if (ax.isPresent()) {
	    OWLAnnotation oa = (ax.get()).getAnnotation();
	    OWLAnnotationValue oav = oa.getValue();
	    Optional<OWLLiteral> op = oav.asLiteral();
	    if (op.isPresent()) {
		OWLLiteral ol = op.get();
		annValue = ol.getLiteral();
	    }
	}

	return annValue;
    }

    public static void handleWebApplication(Map.Entry<String, JsonElement> e, HashMap<String, OWLNamedIndividual> niMap,
					   OWLOntology oo, OWLDataFactory odf, IriLookup iriMap) {
	OWLNamedIndividual oni = niMap.get("webexecutionof");
        JsonElement je = e.getValue();
        if (je instanceof JsonArray) {
	    JsonArray elemArray = (JsonArray)je;
	    Iterator<JsonElement> elemIter = elemArray.iterator();
	    System.out.println("Web application size: " + elemArray.size());
	    while (elemIter.hasNext()) {
		JsonElement elemi = elemIter.next();
		String value = ((JsonPrimitive)elemi).getAsString();
		//value is just a URL so slap it on the web execution of dtm individual
		addAnnotationToNamedIndividual(oni, iriMap.lookupAnnPropIri("hasURL"), value, odf, oo);
	    }
	} else {
	    System.err.println("Web application attribute has value that is not primitive.");
	    throw new IllegalArgumentException("Web application attribute must be array.");
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
	    System.err.println("Location attribute has value that is not primitive.");
	    throw new IllegalArgumentException("location attribute must be primitive.");
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
		//otherwise, if the documentation value is not an <a href=... construct then if it starts with http:/// add as URL otherwise, add as label
		if (value.startsWith("http:"))
		    addAnnotationToNamedIndividual(oni, iriMap.lookupAnnPropIri("hasURL"), value, odf, oo);
		else
		    addAnnotationToNamedIndividual(oni, iriMap.lookupAnnPropIri("label"), value, odf, oo);		    
	    }
	} else if (je instanceof JsonArray) {
	    JsonArray ja = (JsonArray)je;
	    //System.out.println("DOCUMENTATION SIZE = " + ja.size());
	    Iterator<JsonElement> elemIter = ja.iterator();
	    while (elemIter.hasNext()) {
		JsonElement elemi = elemIter.next();
		int iDoc = 1;
		if (elemi instanceof JsonPrimitive) {
		    String value = ((JsonPrimitive)elemi).getAsString();
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
		    throw new IllegalArgumentException("entry in list of documentation is not primitive, but should be");
		}
		iDoc++;
		if (elemIter.hasNext()) oni = createNamedIndividualWithTypeAndLabel(odf, oo, iriMap.lookupClassIri("documentation"), iriMap.lookupAnnPropIri("editor preferred"), baseName + " documentation " + iDoc); 
	    }
	} else {
	    System.err.println("Documentation attribute has value that is not array!.");
	}
    }

    public static void handleDeveloper(Map.Entry<String, JsonElement> e, HashMap<String, OWLNamedIndividual> niMap,
				  OWLOntology oo, OWLDataFactory odf, IriLookup iriMap) {
	OWLNamedIndividual devInd = niMap.get("developer");
	OWLNamedIndividual wrtInd = niMap.get("codewriting");

	JsonElement je = e.getValue();
	if (je instanceof JsonPrimitive) {
	    String value = ((JsonPrimitive)je).getAsString();
	    String[] devs = (value.contains(";")) ? value.split(Pattern.quote(";")) : value.split(Pattern.quote(","));
	    //System.out.println("There are " + devs.length + " developers:");
	    System.err.println("Developer attribute has value that is primitive.");
	} else {
	    JsonArray devArray = (JsonArray)je;
	    String[] devs = new String[devArray.size()];
	    Iterator<JsonElement> devIter = devArray.iterator();
	    int iDev = 0;
	    while (devIter.hasNext()) {
		JsonElement devElement = devIter.next();
		devs[iDev++] = ((JsonPrimitive)devElement).getAsString();
	    }

	    ArrayList<OWLNamedIndividual> devNis = new ArrayList<OWLNamedIndividual>();
	    devNis.add(devInd);

	    String[] devNames = new String[devs.length];
	    for (int i=0; i<devs.length; i++) {
		//System.out.println("devs[i] = " + devs[i]);
		if (devs[i].trim().startsWith("<a")) {
		    Document d = Jsoup.parse(devs[i]);
		    Elements links = d.select("a");
		    String url = links.get(0).attr("href");
		    String txt = links.get(0).ownText();
		    devNames[i] = txt.trim();

		    //System.out.println("developer name = " + devNames[i] + "\t" + txt);
		    //System.out.println("developer href = " + url + "\t" + url);

		    OWLNamedIndividual devi = null;
		    if (i>=devNis.size()) {
			devi = createNamedIndividualWithTypeAndLabel(odf, oo, iriMap.lookupClassIri("developer"), iriMap.lookupAnnPropIri("label"), devNames[i]);
			devNis.add(devi);
		    } else {
			devi = devNis.get(i);
			addAnnotationToNamedIndividual(devi, iriMap.lookupAnnPropIri("label"), devNames[i], odf, oo);
		    }
		    OWLNamedIndividual emailInd = createNamedIndividualWithTypeAndLabel(odf, oo, iriMap.lookupClassIri("email address"), iriMap.lookupAnnPropIri("label"), url);
		    createOWLObjectPropertyAssertion(emailInd, iriMap.lookupObjPropIri("is contact information about"), devi, odf, oo);
		} else {
		    devNames[i] = devs[i].trim();
		    if (i>=devNis.size()) {
			OWLNamedIndividual devi = createNamedIndividualWithTypeAndLabel(odf, oo, iriMap.lookupClassIri("developer"), iriMap.lookupAnnPropIri("label"), devNames[i]);
			devNis.add(devi);
			addAnnotationToNamedIndividual(devi, iriMap.lookupAnnPropIri("label"), devNames[i], odf, oo);
		    } else {
			addAnnotationToNamedIndividual(devNis.get(i), iriMap.lookupAnnPropIri("label"), devNames[i], odf, oo);
		    }
		}
		//System.out.println(i);
	    }

	    for (int i=0; i<devNis.size(); i++) {
		OWLNamedIndividual lpri = createNamedIndividualWithTypeAndLabel(odf, oo, iriMap.lookupClassIri("legalpersonrole"), iriMap.lookupAnnPropIri("label"), "legal person role of " + devNames[i]);
		OWLNamedIndividual devi = devNis.get(i);
		createOWLObjectPropertyAssertion(devi, iriMap.lookupObjPropIri("bearer"), lpri, odf, oo);
		createOWLObjectPropertyAssertion(wrtInd, iriMap.lookupObjPropIri("has active participant"), devi, odf, oo);
	    }
	}
    }

    public static void handlePublicationsThatUsedRelease(Map.Entry<String, JsonElement> e, HashMap<String, OWLNamedIndividual> niMap,
				  OWLOntology oo, OWLDataFactory odf, IriLookup iriMap) {
	OWLNamedIndividual executableInd = niMap.get("executable");
	OWLNamedIndividual execInd = niMap.get("executionof");
	OWLNamedIndividual studyInd = niMap.get("studyexecution");

	JsonElement je = e.getValue();
	if (je instanceof JsonArray) {
	    JsonArray elemArray = (JsonArray)je;
	    Iterator<JsonElement> elemIter = elemArray.iterator();

	    ArrayList<String> pubInfo = new ArrayList<String>();
	    ArrayList<OWLNamedIndividual> execInds = new ArrayList<OWLNamedIndividual>();
	    ArrayList<OWLNamedIndividual> studyInds = new ArrayList<OWLNamedIndividual>();
	    while (elemIter.hasNext()) {
		JsonElement elemi = elemIter.next();
		String value = ((JsonPrimitive)elemi).getAsString();

		if (value.contains("<li>")) {
		    Document d = Jsoup.parse(value);
		    Elements pubs = d.select("li");
		    System.out.println("THERE ARE " + pubs.size() + " pubs that used release.");
		    for (Element pub : pubs) {
			pubInfo.add(pub.ownText().trim());
		    }
		} else {
		    String[] pubs = value.split(Pattern.quote(";"));
		    //System.out.println("value = " + value);
		    //System.out.println("THERE ARE " + pubs.length + " pubs that used release.");
		    for (int i=0; i<pubs.length; i++) {
			pubInfo.add(pubs[i].trim());
		    }
		}
	    }

	    addAnnotationToNamedIndividual(execInd, iriMap.lookupAnnPropIri("label"), "execution of dtm for study described in " + pubInfo.get(0), odf, oo);
	    addAnnotationToNamedIndividual(studyInd, iriMap.lookupAnnPropIri("label"), "study process described in " + pubInfo.get(0), odf, oo);

	    for (int i=1; i<pubInfo.size(); i++) {
		OWLNamedIndividual execi = createNamedIndividualWithTypeAndLabel(odf, oo, iriMap.lookupClassIri("executionof"), iriMap.lookupAnnPropIri("label"), "execution of dtm for study described in " + pubInfo.get(i));
		OWLNamedIndividual studyi = createNamedIndividualWithTypeAndLabel(odf, oo, iriMap.lookupClassIri("studyexecution"), iriMap.lookupAnnPropIri("label"), "study process described in " + pubInfo.get(i));
		
		createOWLObjectPropertyAssertion(executableInd, iriMap.lookupObjPropIri("is realized by"), execi, odf, oo); 
		createOWLObjectPropertyAssertion(execi, iriMap.lookupObjPropIri("is part of"), studyi, odf, oo); 
	    }

	} else {
	    System.err.println("Publications that used release attribute has value that is not primitive.");
	    throw new IllegalArgumentException("value for pubsThatUsedRelease cannot be primitive (must be array).");
	}
    }

    public static void handleAvailableAt(Map.Entry<String, JsonElement> e, HashMap<String, OWLNamedIndividual> niMap,
				  OWLOntology oo, OWLDataFactory odf, IriLookup iriMap) {
	OWLNamedIndividual executableInd = niMap.get("executable");
	OWLNamedIndividual execConcInd = niMap.get("executableConcretization");

	JsonElement je = e.getValue();
	if (je instanceof JsonArray) {
	    JsonArray elemArray = (JsonArray)je;
	    Iterator<JsonElement> elemIter = elemArray.iterator();
	    int size = elemArray.size();
	    while (elemIter.hasNext()) {
		JsonElement elemi = elemIter.next();
		if (elemi instanceof JsonPrimitive) {
		    String value = ((JsonPrimitive)elemi).getAsString();
		    if (value.equals("olympus")) {
			createOWLObjectPropertyAssertion(olympus, iriMap.lookupObjPropIri("bearer"), execConcInd, odf, oo);
       		    } else if (value.equals("udsi")) {
			handleUdsi(value, size, executableInd, execConcInd, iriMap, odf, oo);
		    } else {
			throw new IllegalArgumentException("value of availableAt must be 'olympus' or 'udsi'");
		    }
		} else {
		    throw new IllegalArgumentException("value of availableAt must be primitive");
		}
	    }
	}	
    }

    public static void handleUdsi(String value, int size, OWLNamedIndividual execInd, OWLNamedIndividual execConcInd, IriLookup iriMap, OWLDataFactory odf, OWLOntology oo) {
	
	if (size > 1) {
	    /*  This means that we also have Olympus, but the concretization of the executable available through UDSI is likely a different copy
		than the one on Olympus, so we're going to clone the executable concretization.  Also it could be that the compiled executables 
		b/w UDSI and Olympus are from different compiling processes, but I'm not going that far.  They should be identical anyway
	    */
	    
	    String execConcIndPrefTerm = getAnnotationValueFromNamedIndividual(execConcInd, iriMap.lookupAnnPropIri("editor preferred"), oo);
	    execConcIndPrefTerm += " available through UDSI";
	    execConcInd = createNamedIndividualWithTypeAndLabel(odf, oo,  iriMap.lookupClassIri("executableConcretization"), iriMap.lookupAnnPropIri("editor preferred"), execConcIndPrefTerm);
	    //connect executable of DTM to concretization of executable of DTM.  If we didn't create new concretization, then it's already connected.
	    createOWLObjectPropertyAssertion(execInd, iriMap.lookupObjPropIri("is concretized as"), execConcInd, odf, oo);
	}

	//create disposition to invoke DTM 
	String invokeDispPrefTerm = "disposition of UIDS to invoke " + fullName;
	OWLNamedIndividual invokeDisp = createNamedIndividualWithTypeAndLabel(odf, oo, iriMap.lookupClassIri("disposition"), iriMap.lookupAnnPropIri("editor preferred"), invokeDispPrefTerm);

	//connect concretization of UDSI to dispostion to invoke
	createOWLObjectPropertyAssertion(uidsExecutableConcretization, iriMap.lookupObjPropIri("has basis in"), invokeDisp, odf, oo);

	//create disposition to execute DTM
	String executeDispPrefTerm = "disposition of remote service to execute " + fullName;
	OWLNamedIndividual executeDisp = createNamedIndividualWithTypeAndLabel(odf, oo, iriMap.lookupClassIri("disposition"), iriMap.lookupAnnPropIri("editor preferred"), executeDispPrefTerm);

	//connect disposition to execute to DTM concretization of executable of DTM
	createOWLObjectPropertyAssertion(execConcInd, iriMap.lookupObjPropIri("has basis in"), executeDisp, odf, oo);

	//connect dispositions
	createOWLObjectPropertyAssertion(invokeDisp, iriMap.lookupObjPropIri("s-depends"), executeDisp, odf, oo);
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
				    //System.out.println(jei1.isJsonArray());
				    JsonArray ja2 = (JsonArray)jei1;
				    Iterator<JsonElement> j = ja2.iterator();
				    while (j.hasNext()) {
					JsonElement jej = j.next();
					if (jej.isJsonPrimitive()) {
					    JsonPrimitive jp = (JsonPrimitive)jej;
					    System.out.println("init entry set::\t\t" + jp.getAsString());
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

    public static OWLNamedIndividual createOlympusIndividuals(OWLDataFactory odf, OWLOntology oo, IriLookup iriMap) {
	OWLNamedIndividual oni = odf.getOWLNamedIndividual(iriMap.lookupIndividIri("olympus"));
	//Deleting this.  Olympus class assertion axiom occurs in OBC.ide OWL file, so we don't want/need to re-iterate it here.
	//OWLClassAssertionAxiom ocaaTemp = odf.getOWLClassAssertionAxiom(odf.getOWLClass(iriMap.lookupClassIri("compute cluster")), oni);
	//oom.addAxiom(oo,ocaaTemp);
	addAnnotationToNamedIndividual(oni, iriMap.lookupAnnPropIri("label"), "Olympus", odf, oo);

	OWLNamedIndividual olympusWebsite = odf.getOWLNamedIndividual(nextIri());
	OWLClassAssertionAxiom ocaa = odf.getOWLClassAssertionAxiom(odf.getOWLClass(iriMap.lookupClassIri("website")), olympusWebsite);
	oom.addAxiom(oo,ocaa);
	addAnnotationToNamedIndividual(olympusWebsite, iriMap.lookupAnnPropIri("editor preferred"), "Olympus home page", odf, oo);
	addAnnotationToNamedIndividual(olympusWebsite, iriMap.lookupAnnPropIri("title"), "Olympus", odf, oo);
	addAnnotationToNamedIndividual(olympusWebsite, iriMap.lookupAnnPropIri("hasURL"), "https://www.psc.edu/resources/computing/olympus", odf, oo);

	createOWLObjectPropertyAssertion(olympusWebsite, iriMap.lookupObjPropIri("is about"), oni, odf, oo);
	return oni;
    }

    public static OWLNamedIndividual createUidsIndividuals(OWLDataFactory odf, OWLOntology oo, IriLookup iriMap) {
	OWLNamedIndividual oni = odf.getOWLNamedIndividual(nextIri());
	OWLClassAssertionAxiom ocaaTemp = odf.getOWLClassAssertionAxiom(odf.getOWLClass(iriMap.lookupClassIri("software")), oni);
	oom.addAxiom(oo,ocaaTemp);
	addAnnotationToNamedIndividual(oni, iriMap.lookupAnnPropIri("label"), "Universal Interface to Disease Simulators (UIDS) version 4.0.1", odf, oo);
	addAnnotationToNamedIndividual(oni, iriMap.lookupAnnPropIri("editor preferred"), "The Apollo Universal Interface to Disease Simulators (UIDS) version 4.0.1", odf, oo);
	addAnnotationToNamedIndividual(oni, iriMap.lookupAnnPropIri("hasURL"), "https://github.com/ApolloDev/simple-end-user-apollo-web-application/tree/38161eba742a9000bb610aa47419f7fb62f0c3ac", odf, oo);
	addAnnotationToNamedIndividual(oni, iriMap.lookupAnnPropIri("synopsis"), "The UIDS (Universal Interface to Disease Simulators) is a web application that assists a user in the construction and simulation of infectious disease scenarios (scenarios).  The UIDS xml-encodes scenarios according to the Apollo-XSD InfectiousDiseaseScenario type.  A user can construct a scenario de novo or download an existing scenario from the Apollo Library.   UIDS can transmit a scenario to a disease transmission simulator by calling the Apollo Broker service <insert URL>.  UIDS reports on the status of a requested run of a disease transmission simulator and displays maps and time series graphs of simulator output.  UIDS can also save a scenario to the Apollo Library <insert URL>.", odf, oo);

	OWLNamedIndividual uidsWebsite = odf.getOWLNamedIndividual(nextIri());
	OWLClassAssertionAxiom ocaa = odf.getOWLClassAssertionAxiom(odf.getOWLClass(iriMap.lookupClassIri("website")), uidsWebsite);
	oom.addAxiom(oo,ocaa);
	addAnnotationToNamedIndividual(uidsWebsite, iriMap.lookupAnnPropIri("editor preferred"), "UIDS home page", odf, oo);
	addAnnotationToNamedIndividual(uidsWebsite, iriMap.lookupAnnPropIri("title"), "UIDS home", odf, oo);
	addAnnotationToNamedIndividual(uidsWebsite, iriMap.lookupAnnPropIri("hasURL"), "https://research.rods.pitt.edu/apollo-web-client/index.php", odf, oo);

	createOWLObjectPropertyAssertion(uidsWebsite, iriMap.lookupObjPropIri("is about"), oni, odf, oo);

	uidsCompiling = createNamedIndividualWithTypeAndLabel(odf, oo, iriMap.lookupClassIri("compiling"), iriMap.lookupAnnPropIri("editor preferred"), "compiling of UIDS source to UIDS executable");
	addAnnotationToNamedIndividual(uidsCompiling, iriMap.lookupAnnPropIri("label"), "compiling UIDS", odf, oo);

	uidsExecutable = createNamedIndividualWithTypeAndLabel(odf, oo, iriMap.lookupClassIri("executable"), iriMap.lookupAnnPropIri("editor preferred"), "UIDS executable code");
	addAnnotationToNamedIndividual(uidsExecutable, iriMap.lookupAnnPropIri("label"), "UIDS executable", odf, oo);

	uidsExecutableConcretization = createNamedIndividualWithTypeAndLabel(odf, oo, iriMap.lookupClassIri("executableConcretization"), iriMap.lookupAnnPropIri("editor preferred"), "concretization of UIDS executable");
	addAnnotationToNamedIndividual(uidsExecutableConcretization, iriMap.lookupAnnPropIri("label"), "UIDS executable concretization", odf, oo);

	createOWLObjectPropertyAssertion(uidsCompiling, iriMap.lookupObjPropIri("has specified input"), oni, odf, oo);
	createOWLObjectPropertyAssertion(uidsCompiling, iriMap.lookupObjPropIri("has specified output"), uidsExecutable, odf, oo);
	createOWLObjectPropertyAssertion(uidsExecutable, iriMap.lookupObjPropIri("is concretized as"), uidsExecutableConcretization, odf, oo);

	
	return oni;
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

	//if (rdfsLabel.contains("dtm")) System.out.println(rdfsLabel + " created with IRI: " + oni.getIRI());

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
	String counterTxt = Long.toString(iriCounter++);
	StringBuilder sb = new StringBuilder(iriPrefix);
	int numZero = 10-counterTxt.length();
	for (int i=0; i<numZero; i++) 
	    sb.append("0");
	sb.append(counterTxt);
	return IRI.create(new String(sb));
    }
}
