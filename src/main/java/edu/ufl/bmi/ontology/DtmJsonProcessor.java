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

public class DtmJsonProcessor {
    static long iriCounter = 1200007500L;
    static String iriPrefix = "http://www.pitt.edu/obc/IDE_ARTICLE_";
    static int iriLen = 10;

    static long reservedSimPopIriCounter = 1200007300L;

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

    static FileWriter simPops;

    static HashSet<String> uniqueCms;
    static HashSet<String> uniqueInputFormats;
    static HashSet<String> uniqueOutputFormats;;

    static ControlMeasureIriMapping cmMap;

    static HashMap<String, OWLNamedIndividual> formatInds;
    static HashMap<String, OWLNamedIndividual> fullNameToExecutable;
    static HashMap<String, OWLNamedIndividual> devNis;
    static HashMap<String, OWLNamedIndividual> licenseNis;

    static PublicationLinks pubLinks;

    static HashMap<String, String> dtmToSimInds;
    static HashMap<String, String> simPopIris;
    static HashMap<String, OWLNamedIndividual> websiteInds;
	static HashSet<String> allDtmAtt;
	static HashSet<String> uniqueLocationsCovered;
    static HashSet<String> uniquePathogensCovered;
    static HashSet<String> uniqueHostsCovered;
	static HashSet<String> uniqueFormats;
    static HashMap<String, ArrayList<String>> popsNeededByDtm;
    static HashSet<String> populationsNeeded;

	static IriLookup iriMap;
	static IndividualsToCreate itc;
	static HashMap<String, HashSet<String>> indsMap;

	static OWLDataFactory odf;
	static OWLOntology oo;

	static IRI ontologyIRI = IRI.create("http://www.pitt.edu/mdc/software");

    static Properties p;

     public static void main(String[] args) {
        FileReader fr = null;
        LineNumberReader lnr = null;
        FileOutputStream fos = null;
        FileWriter fw = null;
        FileWriter devOut = null;

        initialize(args);

        try {
            JsonArray jo = null;
            JsonParser jp = new JsonParser();
            String softwareMetadataLocation = p.getProperty("software_info");
          
            fr = new FileReader(softwareMetadataLocation);
            lnr = new LineNumberReader(fr);
            JsonElement je = jp.parse(fr);

            /*
            	The file is an array of JSON objects, one per digital object
            */
            jo = (JsonArray) je;

            //just want to make sure the "software-ontology-YYY-MM-DD" file write Elements in
            //the same order, so let's sort the JsonArray
            jo = sortForEasyValidation(jo);

            /*
            //Code used by Levander to validate that the input data (software metadata)
            //was identical between new version and old version...

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Writer fOut = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream("orig-software"), "UTF-8"));
            fOut.write(gson.toJson((JsonElement) jo));
            fOut.close();
            */

            Iterator<JsonElement> i;
            i = jo.iterator();
            System.out.println(jo.size());

            // this outer loop for iterator "i" processes one software/data service at a time
            while (i.hasNext()) {
            	/* 
            		Get the next element in the array, which is the JSON object that represents
            			a digital object.
            	*/
                JsonElement ei = i.next();
                JsonObject jo2 = (JsonObject) ei;

				/*
					Get the type attribute, and check its value.  If it's neither software, 
						nor data service, skip it.
				*/
				JsonElement typeElem = jo2.get("type");
				if (typeElem.isJsonPrimitive()) {
					JsonPrimitive typeValue = (JsonPrimitive)typeElem;
					String type = typeValue.getAsString();
					if (type.equals("edu.pitt.isg.mdc.dats2_2.Dataset") ||
						type.equals("edu.pitt.isg.mdc.dats2_2.DataStandard"))
						continue;
				} else {
					// else it's an error!
					System.err.println("Bad JSON - type element should be primitive.");
				}

				/*
					Now, the guts of the thing are in the "content" attribute, as a nested
						JSON object.
				*/
				JsonElement contentElem = jo2.get("content");
				JsonObject contentObject = (JsonObject)contentElem;


                JsonElement je3 = contentObject.get("subtype");
                if (je3.isJsonPrimitive()) {
                    JsonPrimitive jprim = (JsonPrimitive) je3;
                    String subtype = jprim.getAsString();
                    System.out.println("SUBTYPE.  subtype=\"" + subtype + "\"");

                    //System.out.println("\t"+ key  + " is a " + subtype);
                    baseName = "";
                    versionSuffix = "";
                    fullName = "";
                    versionNames = null;
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

                    Set<Map.Entry<String, JsonElement>> dtmAttSet = contentObject.entrySet();
                    Iterator<Map.Entry<String, JsonElement>> j = dtmAttSet.iterator();
                    HashSet<String> reqInds = new HashSet<String>();
                    while (j.hasNext()) {
                        Map.Entry<String, JsonElement> ej = j.next();
                        String keyj = ej.getKey();
                        allDtmAtt.add(keyj);
                        //System.out.println("\t\t" + keyj);
                        if (keyj.equals("title")) {
                            JsonElement jej = ej.getValue();
                            if (jej instanceof JsonPrimitive) {
                                baseName = ((JsonPrimitive) jej).getAsString();
                                System.out.println("\t" + baseName + " is a " + subtype);
                            } else {
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
                            } else {
                                //System.err.println("version key does not have primitive value");
                                //System.err.println("it's type is instead " + jej.getClass());
                                JsonArray versionArray = (JsonArray) jej;
                                Iterator<JsonElement> vIter = versionArray.iterator();
                                versionNames = new String[versionArray.size()];
                                //System.out.println("VERSION COUNT: " + versionNames.length);
                                int vIndex = 0;
                                while (vIter.hasNext()) {
                                    JsonElement vElement = vIter.next();
                                    //System.out.println("Version element is " + vElement.getClass());
                                    if (vElement instanceof JsonPrimitive) {
                                        versionNames[vIndex++] = ((JsonPrimitive) vElement).getAsString();
                                        //System.out.print(versionNames[vIndex-1] + ", ");
                                    } else {
                                        System.err.println("Version element is not primitive!!!");
                                    }
                                }

                            }
                        }
                        HashSet<String> indsForKey = indsMap.get(keyj);
                        if (indsForKey != null) {
                            reqInds.addAll(indsForKey);
                            //System.out.println("adding inds for key " + keyj);
                        }
                    } // end while (j.hasNext())
                    //System.out.println();


                    if ((baseName.contains("FluTE") || baseName.contains("NAADSM")) && versionNames.length > 1) {
                        versionSuffix = "";
                        for (int iName = 0; iName < versionNames.length; iName++) {
                            versionSuffix += versionNames[iName] + ((iName < versionNames.length - 1) ? ", " : "");
                        }
                    } else if (baseName.contains("GLEAM") && versionNames.length > 1) {
                        versionSuffix = "";
                        for (int iName = 0; iName < versionNames.length; iName++) {
                            if (versionNames[iName].contains("Server")) {
                                versionSuffix = versionNames[iName];
                            }
                        }
                    } else {
                        versionSuffix = (versionNames != null) ? versionNames[0] : "";
                    }

                    int cVersion = (versionNames != null) ? versionNames.length : 0;
                    //System.out.println("Number of versions is : " + cVersion);

                    //System.out.println("base name = " + baseName + ", version = " + version);
                    String baseLabel = (versionNames == null) ? baseName : baseName + " - " +
                            ((Character.isDigit(versionSuffix.charAt(0))) ? " v" + versionSuffix : versionSuffix);
                    fullName = baseLabel;
                    //System.out.println("FULLNAME: " + fullName);
                    Iterator<String> k = reqInds.iterator();
                    //System.out.println("\t\t\treqInds.size() = " + reqInds.size());
                    IRI labelIri = iriMap.lookupAnnPropIri("editor preferred");
                    HashMap<String, OWLNamedIndividual> niMap = new HashMap<String, OWLNamedIndividual>();
                    while (k.hasNext()) {
                        String ks = k.next();
                        IRI classIri = (ks.equals("dtm")) ? iriMap.lookupClassIri(subtype) : iriMap.lookupClassIri(ks);
                        //System.out.println("\t\t\t'" + ks + "'\t" + classIri);
                        String indLabel = fullName + " " + subtype.substring(0, subtype.length() - 1);
                        indLabel = indLabel + ((ks.equals("dtm")) ? " software" : " " + ks);
                        OWLNamedIndividual oni = createNamedIndividualWithTypeAndLabel(odf, oo, classIri, labelIri, indLabel);
                        //if (ks.equals("dtm")) System.out.println("DTMINDLABEL: " + indLabel);
					    if (ks.equals("dtm")) addAnnotationToNamedIndividual(oni, iriMap.lookupAnnPropIri("label"), fullName, odf, oo);
                        if (ks.startsWith("simulat"))
                            simPops.write(oni.getIRI() + "\t" + fullName + " " + ks + "\t" + fullName + "\n");
                        niMap.put(ks, oni);
                    } // end while k.hasNext()

                    //Once we've identified all the individuals we need, and we've created them, now we have to go back through
                    // and stick stuff on the individuals
                    j = dtmAttSet.iterator();
                    HashSet<String> locations = new HashSet<String>();
                    HashSet<String> pathogens = new HashSet<String>();
                    HashSet<String> hosts = new HashSet<String>();

                    while (j.hasNext()) {
                        Map.Entry<String, JsonElement> ej = j.next();
                        String keyj = ej.getKey();
                        if (keyj.equals("title")) {
                            handleTitle(ej, niMap, oo, odf, iriMap);
                        } else if (keyj.equals("version")) {
                            handleVersion(ej, niMap, oo, odf, iriMap);
                        } else if (keyj.equals("source")) {
                            handleSource(ej, niMap, oo, odf, iriMap);
                        } else if (keyj.equals("license")) {
                            handleLicense(ej, niMap, oo, odf, iriMap);
                        } else if (keyj.equals("doi")) {  //this attribute appears to be obsolete. Identifier is used.
                            handleDoi(ej, niMap, oo, odf, iriMap);
                        } else if (keyj.equals("sourceCodeRelease")) {
                            handleSourceCodeRelease(ej, niMap, oo, odf, iriMap);
                        } else if (keyj.equals("generalInfo") || keyj.equals("humanReadableSynopsis")) {
                            handleGeneralInfo(ej, niMap, oo, odf, iriMap);
                        } else if (keyj.equals("executables")) {
                            handleExecutables(ej, niMap, oo, odf, iriMap);
                            fullNameToExecutable.put(fullName, niMap.get("executable"));
                        } else if (keyj.equals("webApplication")) {
                            handleWebApplication(ej, niMap, oo, odf, iriMap);
                        } else if (keyj.equals("location") || keyj.equals("site") || keyj.equals("website")) {
                            handleLocation(ej, niMap, oo, odf, iriMap);
                        } else if (keyj.equals("documentation") || keyj.startsWith("userGuides")) {
                            handleDocumentation(ej, niMap, oo, odf, iriMap);
                        } else if (keyj.equals("developer") || keyj.equals("developers")) {
                            handleDeveloper(ej, niMap, oo, odf, iriMap);
                        } else if (keyj.equals("publicationsThatUsedRelease")) {
                            handlePublicationsThatUsedRelease(ej, niMap, oo, odf, iriMap);
                        } else if (keyj.equals("availableAt")) {
                            handleAvailableAt(ej, niMap, oo, odf, iriMap);
                        } else if (keyj.equals("isUdsi") || keyj.equals("availableOnUdsi") || keyj.equals("availableViaUdsi") || keyj.equals("availableAtUids") ||
                                keyj.equals("availableOnUids") || keyj.equals("availableOnUIDS")) {
                            OWLNamedIndividual executableInd = niMap.get("executable");
                            OWLNamedIndividual uidsConcInd = niMap.get("uidsConc");
                            JsonElement elem = ej.getValue();
                            String value = ((JsonPrimitive) elem).getAsString();
                            handleUids(value, 1, executableInd, uidsConcInd, iriMap, odf, oo);
                        } else if (keyj.equals("availableOnOlympus") || keyj.equals("isOlympus") || keyj.equals("availableAtOlympus")) {
                            OWLNamedIndividual executableInd = niMap.get("executable");
                            OWLNamedIndividual execConcInd = niMap.get("olympusConc");
                            JsonElement elem = ej.getValue();
                            String value = ((JsonPrimitive) elem).getAsString();
                            createOWLObjectPropertyAssertion(olympus, iriMap.lookupObjPropIri("bearer"), execConcInd, odf, oo);
                        } else if (keyj.equals("controlMeasures")) {
                            handleControlMeasures(ej, niMap, oo, odf, iriMap);
                        } else if (keyj.equals("dataInputFormats") || keyj.equals("dataInputFormat")) {
                            handleDataInputFormats(ej, niMap, oo, odf, iriMap);
                        } else if (keyj.equals("dataOutputFormats")) {
                            handleDataOutputFormats(ej, niMap, oo, odf, iriMap);
                        } else if (keyj.equals("publicationsAbout") || keyj.equals("publicationsAboutRelease")) {
                            handlePublicationsAbout(ej, niMap, oo, odf, iriMap);
                        } else if (keyj.equals("identifier")) {
                            handleIdentifier(ej.getValue(), niMap, oo, odf, iriMap);
                        } 
                        /* 
                        	Handle attributes specific to disease forecasters.  It might be better just
                        		to connect all of them in batch at the end. 
                        */
                        else if (keyj.equals("diseases")) {
                            handleDiseases(ej, niMap, oo, odf, iriMap);
                        } else if (keyj.equals("region")) {
                            handleRegion(ej, niMap, oo, odf, iriMap);
                        }
                        /* 
                        	End attributes specific to disease forecasters
                       */


                        else {
                        	/* Supposedly if we get here, then it's an attribute we don't know 
                        		how to handle yet.  However, inexplicably, we handle some still
                        		below and I'm still trying to figure out why I did that.  Right
                        		now, it appears to be that we don't handle the attribute directly
                        		here in the code, but in a manual, post-processing step.
                        	*/
                            boolean handled = false;
                            /*
                            	Get the value of the attribute & report it out too.
                            */
                            JsonElement jeRemainder = ej.getValue();
                            if (jeRemainder instanceof JsonPrimitive) {
                            	//If the value is primitive, it looks like we just eat it, which is odd.
                                String value = ((JsonPrimitive) jeRemainder).getAsString();
                            } else if (jeRemainder instanceof JsonArray) {
                            	/*
                            		Otherwise if the value is an array, we get the array. We can handle array
                            			values above so not sure why I moved this code down here.
                            	*/
                                JsonArray remArray = (JsonArray) jeRemainder;
                                Iterator<JsonElement> remIter = remArray.iterator();
                                /*
                                	Iterate through the array, which is the value of the attribute we don't
                                	 	know about yet.
                               	*/
                                while (remIter.hasNext()) {
                                	/*
                                		For the next element in the array...
                                	*/
                                    JsonElement remNext = remIter.next();
                                    if (remNext instanceof JsonObject) {
                                    	// If it is a JSON object, and the key is location coverage...
                                        JsonElement idElem = remNext.getAsJsonObject().get("identifier");
                                        if (idElem == null) {
                                            System.out.println("WARNING: ignoring " + keyj + " attribute.");
                                            continue;
                                        }
                                        String value = idElem.getAsJsonObject().get("identifierDescription").getAsString();

                                        if (keyj.equals("locationCoverage")) {
                                            //System.out.println("LOCATION: " + value);
                                            handled = true;
                                            if (!value.equals("N/A")) {
                                            	/*  
                                            		Here, we just record the values of locations covered,
                                            			we don't actually "handle" them in the sense of 
                                            			connecting the software or data service to them
                                            	*/
                                                uniqueLocationsCovered.add(value);
                                                locations.add(value);
                                            }
                                        } else if (keyj.equals("diseaseCoverage") || keyj.equals("pathogenCoverage")) {
                                            handled = true;
                                            if (!value.equals("N/A")) {
                                            	/*
                                            		Same thing for disease/pathogen coverage.  Just note the value,
                                            			but we don't connect the software to the particular disease
                                            			or pathogen in this code.
                                            	*/
                                                uniquePathogensCovered.add(value);
                                                pathogens.add(value);
                                            }
                                        } else if (keyj.equals("hostSpeciesIncluded")) {
                                            handled = true;
                                            if (!value.equals("N/A")) {
                                            	/*
                                            		Same thing for host coverage.  Just note the value,
                                            			but we don't connect the software to the particular host
                                            			in this code.
                                            	*/
                                                uniqueHostsCovered.add(value);
                                                hosts.add(value);
                                            }
                                        }

                                    } else {
                                    	/*
                                    		 If we get here, we have an array of things that are mere strings.

                                    		 The only attribute like this is the dataServiceDescriptor or something
                                    		 	like that.
                                    	*/
                                        System.err.println("ERROR: element " + keyj + " has array of values that are string.  Ignoring.");
                                    }
                                }

                            } else {
                            	/*
                            		If we get here, we have neither a String value, nor an Array value,
                            		 but an object value, for the key for which we don't know how to process
                            	*/
                                //System.err.println("jeRemainder instanceof " + jeRemainder.getClass());
                                //System.err.println(jeRemainder);
                                JsonObject remObject = (JsonObject) jeRemainder;
                                Set<Map.Entry<String, JsonElement>> remEntrySet = remObject.entrySet();
                                for (Map.Entry<String, JsonElement> remEntryi : remEntrySet) {
                                    String key = remEntryi.getKey();
                                    JsonElement remElem = remEntryi.getValue();
                                    //System.err.println("\t" + key + " == " + remElem.isJsonPrimitive());
                                    /* 
                                    	Identifier lives here, because we don't handle it above.  But I 
                                    		think we just need to move it above and everything should still
                                    		work fine
                                    
                                    if (key.equals("identifier")) {
                                        handleIdentifier(remElem, niMap, oo, odf, iriMap);
                                    } else {*/
                                        System.out.println("WARNING: assuming that handling of " + key + " attribute in remainder will occur in manual, post-processing step. values " + remElem);
                                    //}
                                }
                            }
                            if (!handled && !keyj.equals("subtype")) { //} && !keyj.equals("identifier")) {
                                System.out.println("WARNING: assuming that handling of " + keyj + " attribute will occur in manual, post-processing step. values " + ej.getValue());
                                if (keyj.equals("publicationsAboutRelease")) {
                                    //System.out.println("PUB ABOUT: " + ej.getValue());

                                }
                            }
                        }
                    } // end while (j.hasNext())

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

                    handleSimInds(niMap, oo, odf, iriMap);
                }


                try {
                    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
                    String dateTxt = df.format(new Date());
                    String owlFileName = "software-ontology-" + dateTxt + ".owl";
                    fos = new FileOutputStream(owlFileName);
                    oom.saveOntology(oo, fos);
                } catch (OWLOntologyStorageException oose) {
                    oose.printStackTrace();
                }
            } /* while(i.hasNext()).  i is iterating over settings plus the main payload, which
            		is all the software apps and data services.  In new JSON retrieved by API, 
            		there are no settings any longer */

           	/*
           		This code merely iterates over all the attributes specified by all the software
           			and data services collectively, and prints them out.  It's handy to have a 
           			complete list of everything used.
           	*/
            Iterator<String> si = allDtmAtt.iterator();
            while (si.hasNext()) {
                System.out.println(si.next());
            }

            /*
            	This code helpfully prints out where the program left off with IRI 
            		generation, so that any other programs needing to pick up where
            		this one left off can do so.
            */
            System.out.println(nextIri());
            System.out.println(nextIri());

            /*
            	This code displays all the geographical regions encountered across 
            	 all software and data services.
            */
            System.out.println("Locations required:");
            for (String location : uniqueLocationsCovered) {
                System.out.println("\t" + location);
            }
            System.out.println();

            /*
            	This code displays all the pathogens encountered across all sofware
            		and data services.
            */
            System.out.println("Pathogens required:");
            for (String pathogen : uniquePathogensCovered) {
                System.out.println("\t" + pathogen);
            }
            System.out.println();

            /*
            	This code displays all the hosts encountered across all software and
            		data services.
            */
            System.out.println("Hosts required: ");
            for (String host : uniqueHostsCovered) {
                System.out.println("\t" + host);
            }
            System.out.println();

            /*
            	This code displays all the pathogen+geographical region and 
            		host+geographical region combinations required.
            */
            System.out.println("Populations required: ");
            for (String pop : populationsNeeded) {
                System.out.println("\t" + pop);
            }
            System.out.println();

            /*
            	This code writes to a file all the populations (host and pathogen)
            		that are required for each DTM.
            */
            fw = new FileWriter("./pops_by_dtm.txt");
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

            /*
            	This code outputs all the IRIs that were assigned to individuals created
            		to represent developers of software and data services.
            */
            devOut = new FileWriter("./developer_iris.txt");
            Set<String> devs = devNis.keySet();
            for (String dev : devs) {
                OWLNamedIndividual devInd = devNis.get(dev);
                devOut.write(dev + "\t" + devInd.getIRI() + "\n");
                if (dev.equals("Shawn T. Brown")) devOut.write("Shawn Brown\t" + devInd.getIRI() + "\n");
                if (dev.contains("Bill")) {
                        String devAlt = dev.replace("Bill","William");
                        devOut.write(devAlt + "\t" + devInd.getIRI() + "\n");
                }
            }
            devOut.close();

            /*
            	This code displays all the unique control measures encountered across
            		all DTMs.
            */
            System.out.println("Control measures:");
            for (String cm : uniqueCms) {
                System.out.println(cm);
            }

            /*
            	This code combines all the input/output formats into a single, unique
            		list.
            */
            uniqueFormats.addAll(uniqueInputFormats);
            uniqueFormats.addAll(uniqueOutputFormats);

            /*
            	Display all the unique input formats encountered
           	*/
            System.out.println("\nInput formats:");
            for (String input : uniqueInputFormats) {
                System.out.println("\t" + input);
            }

            /*
            	Display all the unique output formats encountered
            */
            System.out.println("\nOutput formats:");
            for (String output : uniqueOutputFormats) {
                System.out.println("\t" + output);
            }

            /*
            	Display the entire list of unique formats across all
            		input & output
            */
            System.out.println("\nAll formats:");
            for (String format : uniqueFormats) {
                System.out.println("\t" + format);
            }

        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (JsonIOException jioe) {
            jioe.printStackTrace();
        } catch (JsonSyntaxException jse) {
            jse.printStackTrace();
        } finally {
            try {
            	/*
            		Close everything down
            	*/
                if (fw != null) fw.close();
                if (fos != null) fos.close();
                if (lnr != null) lnr.close();
                if (fr != null) fr.close();
                if (simPops != null) simPops.close();
                if (devOut != null) devOut.close();
            } catch (IOException ioe) {
                //just eat it, eat it, don't you make me repeat it!
                //Strangely, this is the correct thing to do in this situation: yay, java!
            }
        }
    }

    protected static void initialize(String[] args) {
    	FileReader fr = null;

        try (FileReader pr = new FileReader(args[0]);) {

        	/* 
        		The properties tell this program where to find various resources, including
        			where to find a mapping from short text "handles" to needed IRIs.
        	*/
            p = new Properties();
            p.load(pr);

            simPops = new FileWriter("./simulated-populations.txt");



            //cmMap = new ControlMeasureIriMapping("./src/main/resources/control-measure-mapping.txt");
            cmMap = new ControlMeasureIriMapping(p.getProperty("cm_mapping"));
            cmMap.initialize();

            //pubLinks = new PublicationLinks("./src/main/resources/pubs_about_using.txt");
            pubLinks = new PublicationLinks(p.getProperty("pubs_info"));
            pubLinks.init();

            fullNameToExecutable = new HashMap<String, OWLNamedIndividual>();
            devNis = new HashMap<String, OWLNamedIndividual>();
            licenseNis = new HashMap<String, OWLNamedIndividual>();

            oom = OWLManager.createOWLOntologyManager();

            allDtmAtt = new HashSet<String>();

            iriMap = new IriLookup(p.getProperty("iris"));
            iriMap.init();

           
            //IndividualsToCreate itc = new IndividualsToCreate("./src/main/resources/individuals_required.txt");
            itc = new IndividualsToCreate(p.getProperty("inds_required"));
            itc.init();
            indsMap = itc.getIndividualsToCreate();


           odf = OWLManager.getOWLDataFactory();
                       
            try {
                oo = oom.createOntology(ontologyIRI);
            } catch (OWLOntologyCreationException ooce) {
                ooce.printStackTrace();
            }

            olympus = createOlympusIndividuals(odf, oo, iriMap);
            uids = createUidsIndividuals(odf, oo, iriMap);
            loadAndCreateDataFormatIndividuals(odf, oo, iriMap);
            loadLicenses(p.getProperty("license_info"), odf);
            String websiteFname = p.getProperty("website_ind");
            websiteFname = websiteFname.replace("<date>", args[1].trim()).trim();
            loadWebsites(websiteFname, odf);
           	
             initializeDtmSimInds(p.getProperty("dtm_sim_ind"));
             initializeSimPopIris(p.getProperty("sim_pop_iri"));


            uniqueLocationsCovered = new HashSet<String>();
            uniquePathogensCovered = new HashSet<String>();
            uniqueHostsCovered = new HashSet<String>();
            uniqueInputFormats = new HashSet<String>();
            uniqueOutputFormats = new HashSet<String>();
            uniqueFormats = new HashSet<String>();
            popsNeededByDtm = new HashMap<String, ArrayList<String>>();
            populationsNeeded = new HashSet<String>();

        } catch (IOException ioe) {
        	ioe.printStackTrace();
        } finally {
            try {
                if (fr != null) fr.close();
            } catch (IOException ioe) {
                //just eat it, eat it, don't you make me repeat it!
                //Strangely, this is the correct thing to do in this situation: yay, java!
            }
        }


        uniqueCms = new HashSet<String>();
    }

    /*
    	This code handles the title attribute in the JSON for the software/data service
    */
    public static void handleTitle(Map.Entry<String, JsonElement> e, HashMap<String, OWLNamedIndividual> niMap,
                                   OWLOntology oo, OWLDataFactory odf, IriLookup iriMap) {
        //Get the individual for the current software application.  Despite the name "dtm", works for 
        //  all kinds of software.
        OWLNamedIndividual oni = niMap.get("dtm");

        //Get the value of the title attribute...
        JsonElement je = e.getValue();
        if (je instanceof JsonPrimitive) {
            String value = ((JsonPrimitive) je).getAsString();
            // ...and add it to the individual as a dc:title annotation
            addAnnotationToNamedIndividual(oni, iriMap.lookupAnnPropIri("title"), value, odf, oo);
        } else {
        	//If we get here, then there's a problem in the JSON
            System.err.println("Title attribute has value that is not primitive.");
        }
    }

   /*
    	This code handles the version attribute in the JSON for the software/data service
    */
    public static void handleVersion(Map.Entry<String, JsonElement> e, HashMap<String, OWLNamedIndividual> niMap,
                                     OWLOntology oo, OWLDataFactory odf, IriLookup iriMap) {
        //Get the individual we created to represent the software version identifier
        OWLNamedIndividual oni = niMap.get("versionid");
        // ...and add a label annotation to it with the value of the version attribute in the JSON
        addAnnotationToNamedIndividual(oni, iriMap.lookupAnnPropIri("label"), versionSuffix, odf, oo);
    }

   /*
    	This code handles the source attribute in the JSON for the software/data service.

    	"source" here means "source code", and even more precisely, we typically get a URL to 
    		the entire source code repository.
    */
    public static void handleSource(Map.Entry<String, JsonElement> e, HashMap<String, OWLNamedIndividual> niMap,
                                    OWLOntology oo, OWLDataFactory odf, IriLookup iriMap) {
        OWLNamedIndividual oni = niMap.get("sourcerepository");
        JsonElement je = e.getValue();
        if (je instanceof JsonPrimitive) {
            String value = ((JsonPrimitive) je).getAsString();
            addAnnotationToNamedIndividual(oni, iriMap.lookupAnnPropIri("hasURL"), value, odf, oo);
        } else {
            System.err.println("Source attribute has value that is not primitive.");
        }
    }

   /*
    	This code handles the license attribute in the JSON for the software/data service
    */
    public static void handleLicense(Map.Entry<String, JsonElement> e, HashMap<String, OWLNamedIndividual> niMap,
                                     OWLOntology oo, OWLDataFactory odf, IriLookup iriMap) {

    	//get the value of the license attribute from the JSON...
        JsonElement je = e.getValue();
        if (je instanceof JsonPrimitive) {
            String value = ((JsonPrimitive) je).getAsString();
            //if we got some HTML with a link instead of just plain text...
            if (value.contains("<a ")) {
            	//...then parse out the text and URL separately
                Document d = Jsoup.parse(value);
                Elements links = d.select("a");
                String url = links.get(0).attr("href");
                String txt = links.get(0).ownText();
                OWLNamedIndividual oni = null;
                if (licenseNis.containsKey(txt)) {
                	/* If the license individual exists already, then get it.  It will already have
                		  annotations including any URL.  We can safely eat the value of the license
                	 	attribute in this case.
                	 */
                    oni = licenseNis.get(txt);
                } else {
                	//else, create the license individual and add text/URL annotations to it.
                    System.out.println("CREATING LICENSE: " + txt);
                    oni = createNamedIndividualWithTypeAndLabel(odf, oo, iriMap.lookupClassIri("license"),
                            iriMap.lookupAnnPropIri("editor preferred"),
                            fullName + " - " + txt + " software license");
                    addAnnotationToNamedIndividual(oni, iriMap.lookupAnnPropIri("label"), txt, odf, oo);
                    addAnnotationToNamedIndividual(oni, iriMap.lookupAnnPropIri("hasURL"), url, odf, oo);
                }
			    /* leaving the addannotation lines in for now for debugging and QA/QC purposes, but will need to 
			    	remove them eventually
			    	*/

			    //finally, state that the license is part of the overall software application (or data service)
                createOWLObjectPropertyAssertion(oni, iriMap.lookupObjPropIri("is part of"), niMap.get("dtm"), odf, oo);
            } else {
            	/* Otherwise we have just a plain text value for the license attribute in the JSON, and it's
            		the same routine as before
            	*/
                OWLNamedIndividual oni = null;
                if (licenseNis.containsKey(value)) {
                    oni = licenseNis.get(value);
                } else {
                    System.out.println("CREATING LICENSE: " + value);
                    oni = createNamedIndividualWithTypeAndLabel(odf, oo, iriMap.lookupClassIri("license"),
                            iriMap.lookupAnnPropIri("editor preferred"),
                            fullName + " - " + value + " software license");
                    addAnnotationToNamedIndividual(oni, iriMap.lookupAnnPropIri("label"), value, odf, oo);
                }
			    /* leaving the addannotation lines in for now for debugging and QA/QC purposes, but will need to 
			    	remove them eventually
			    	*/

			    //finally, state that the license is part of the overall software application (or data service)
                createOWLObjectPropertyAssertion(oni, iriMap.lookupObjPropIri("is part of"), niMap.get("dtm"), odf, oo);
            }
        } else {
        	/* 
        		If we get here, then we got malformed JSON, so report the error
        	*/
            System.err.println("License attribute has value that is not primitive.");
        }
    }

    /*
    	This code handles the DOI attribute.  It is now obsolete, as no software or data services have
    		a DOI attribute any longer.
    */
    public static void handleDoi(Map.Entry<String, JsonElement> e, HashMap<String, OWLNamedIndividual> niMap,
                                 OWLOntology oo, OWLDataFactory odf, IriLookup iriMap) {
        OWLNamedIndividual oni = niMap.get("doi");
        JsonElement je = e.getValue();
        if (je instanceof JsonPrimitive) {
            String value = ((JsonPrimitive) je).getAsString();
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

   /*
    	This code handles the identifier attribute.  
    */
    public static void handleIdentifier(JsonElement elem, HashMap<String, OWLNamedIndividual> niMap,
                                        OWLOntology oo, OWLDataFactory odf, IriLookup iriMap) {
        /*
        	Unlike most if not all other attributes, the value of the identifier attribute is 
        		an object.  So we have to unpack it.
       	*/
       	String value = "";
        JsonObject valueAsObject = (JsonObject) elem;
        Set<Map.Entry<String, JsonElement>> objectEntrySet = valueAsObject.entrySet();
        for (Map.Entry<String, JsonElement> entryi : objectEntrySet) {
            String key = entryi.getKey();
            JsonElement entryElem = entryi.getValue();
            value = entryElem.getAsString().trim();
            break;
        }

        /*
        	Filter out bogus identifier values like the string "null", empty values (length is zero), 
        	 	n/a (it's always "a"!), "?", "under development", and "idnetifer will be created at 
        	 	time of release".
       	*/
        if (value != null && !value.equals("null") && value.length() > 0 && !value.toLowerCase().equals("n/a")
                && !value.startsWith("?") && !value.toLowerCase().equals("under development")
                && !value.toLowerCase().equals("identifier will be created at time of release")) {

            String idText = value, url = "";
        	/*
        		If we get a value with <a href, then it's embedded in HTML, so parse out the URL and text
        	*/
            if (value.startsWith("<a href")) {
                Document d = Jsoup.parse(value);
                Elements links = d.select("a");
                url = links.get(0).attr("href");
                idText = links.get(0).ownText();
            }
            IRI identifierClassIri = null;
            /*
            	Is it a Digital Obect Identifier?  If so, then we want to rdf:type it as a DOI instead
            		of a regular software identifier
            */
            int position = idText.indexOf("doi.org/10.");
            if (idText.startsWith("10.")) {
                identifierClassIri = iriMap.lookupClassIri("doi");
            } else if (position > 0) {
            	identifierClassIri = iriMap.lookupClassIri("doi");
				if (!url.equals(idText)) { System.err.println("bad doi: " + idText + ", url = " + url); }
                url = idText;
                idText = idText.substring(position + 8);
                //System.out.println("New id text is: " + idText + ", url = " + url);
            } else {
            	/*
            		If it is not a DOI, then just use the regular identifier class
            	*/
                identifierClassIri = iriMap.lookupClassIri("identifier");
            }

            /*
            	Create the identifier individual, with the idText as the label
            */
            OWLNamedIndividual oni = createNamedIndividualWithTypeAndLabel(odf, oo, identifierClassIri,
                    iriMap.lookupAnnPropIri("label"), idText);
            /*
            	If there's a URL associated with it, which is always true for DOIs (we either get one or
            		create one), then add the URL to the individual as well.
            */
            if (url.length() > 0) {
                addAnnotationToNamedIndividual(oni, iriMap.lookupAnnPropIri("hasURL"), url, odf, oo);
            }

            /*
            	Assert that the identifier/DOI denotes the software/data service
            */
            createOWLObjectPropertyAssertion(oni, iriMap.lookupObjPropIri("denotes"), niMap.get("dtm"), odf, oo);
        }
    }

  /*
    	This code handles the source code release attribute in the JSON for the software/data service.

    	"source" here means "source code", and even more precisely, we typically get a URL to 
    		the specific release version within the source code repository (i.e., it is a 
    		different URL than to the entire source code repository).
    */
    public static void handleSourceCodeRelease(Map.Entry<String, JsonElement> e, HashMap<String, OWLNamedIndividual> niMap,
                                               OWLOntology oo, OWLDataFactory odf, IriLookup iriMap) {
        OWLNamedIndividual oni = niMap.get("dtm");
        JsonElement je = e.getValue();
        if (je instanceof JsonPrimitive) {
            String value = ((JsonPrimitive) je).getAsString();
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

  	/*
    	This code handles the "human readable synopsis" attribute, which used to be called "general info", 
    		which explains the historical name of the method.
    */
    public static void handleGeneralInfo(Map.Entry<String, JsonElement> e, HashMap<String, OWLNamedIndividual> niMap,
                                         OWLOntology oo, OWLDataFactory odf, IriLookup iriMap) {
        OWLNamedIndividual oni = niMap.get("dtm");
        JsonElement je = e.getValue();
        if (je instanceof JsonPrimitive) {
            String value = ((JsonPrimitive) je).getAsString();
            addAnnotationToNamedIndividual(oni, iriMap.lookupAnnPropIri("synopsis"), value, odf, oo);
        } else {
            System.err.println("General info attribute has value that is not primitive.");
        }
    }

  	/*
    	This code handles the "executables" attribute.
    */
    public static void handleExecutables(Map.Entry<String, JsonElement> e, HashMap<String, OWLNamedIndividual> niMap,
                                         OWLOntology oo, OWLDataFactory odf, IriLookup iriMap) {
        //get the executable individual we already created
        OWLNamedIndividual execInd = niMap.get("executable");
        //get the compiling process individual we already created
        OWLNamedIndividual compInd = niMap.get("compiling");

        //get the preferred term from the executable individual
        String execIndPrefTerm = getAnnotationValueFromNamedIndividual(execInd, iriMap.lookupAnnPropIri("editor preferred"), oo);
        //get the preferred term from the compiling individual
        String compIndPrefTerm = getAnnotationValueFromNamedIndividual(execInd, iriMap.lookupAnnPropIri("editor preferred"), oo);

        /*
        	There actually might be multiple executables, which we don't really know until we get here
        */
        JsonElement je = e.getValue();
        if (je instanceof JsonArray) {
            JsonArray elemArray = (JsonArray) je;
            Iterator<JsonElement> elemIter = elemArray.iterator();
            String[] execs = new String[elemArray.size()];
            int iExec = 0;
            /*
            	Accumulate all the values for the executable attribute.  The values are always in a JSON
            		array, even if there's only one value.
            */
            while (elemIter.hasNext()) {
                JsonElement elemi = elemIter.next();
                String value = ((JsonPrimitive) elemi).getAsString();
                execs[iExec++] = value;
            }

            /*
            	So we have to create new individuals for the additional executables if there are more than two, 
            		and new compiling process individuals.  We assume that each executable is the result of 
            		a different compiling process, although it is conceivable that one big "build" process
            		could create all of them.  We just don't know.
            */
            ArrayList<OWLNamedIndividual> execNis = new ArrayList<OWLNamedIndividual>();
            ArrayList<OWLNamedIndividual> compNis = new ArrayList<OWLNamedIndividual>();

            String labelPrefix = "";
            if (execs[0].contains("<a ")) {
            	/*
            		We often get URLs embedded in <a href= with a text value, so parse all that out
            			of the HTML
            	*/
                Document d = Jsoup.parse(execs[0]);
                Elements links = d.select("a");
                String url = links.get(0).attr("href");
                String txt = links.get(0).ownText();
                addAnnotationToNamedIndividual(execInd, iriMap.lookupAnnPropIri("hasURL"), url, odf, oo);
                addAnnotationToNamedIndividual(execInd, iriMap.lookupAnnPropIri("label"), txt, odf, oo);
            } else {
                //otherwise, if the executable value is not html, just put whatever is in there on as a URL.
                if (execs[0].startsWith("http")) {
                    addAnnotationToNamedIndividual(execInd, iriMap.lookupAnnPropIri("hasURL"), execs[0], odf, oo);
                } else {
                    addAnnotationToNamedIndividual(execInd, iriMap.lookupAnnPropIri("label"), execs[0], odf, oo);
                }
            }

            //execNis.add(oni);
            //System.err.println("there are " + execs.length + " executables");
            if (execs.length > 1) {
                for (int i = 1; i < execs.length; i++) {
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
                        OWLNamedIndividual execi = createNamedIndividualWithTypeAndLabel(odf, oo, iriMap.lookupClassIri("executable"), iriMap.lookupAnnPropIri("editor preferred"), newExecIndPrefTerm);
                        OWLNamedIndividual compi = createNamedIndividualWithTypeAndLabel(odf, oo, iriMap.lookupClassIri("compiling"), iriMap.lookupAnnPropIri("editor preferred"), newCompIndPrefTerm);

                        if (execs[i].startsWith("http")) {
                            addAnnotationToNamedIndividual(execi, iriMap.lookupAnnPropIri("hasURL"), execs[i].trim(), odf, oo);
                        } else {
                            addAnnotationToNamedIndividual(execi, iriMap.lookupAnnPropIri("label"), execs[i].trim(), odf, oo);
                        }

                        execNis.add(execi);
                        compNis.add(compi);
                    }
                }
            }
            //Some of the values for executables attribute is an html link to the documentation with link text.  In that case
            // put the link on the executable individual with hasURL annotation, and put the link
            // text on that individual as an rdfs:label annotation.
            for (int i = 0; i < execNis.size(); i++) {
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

    /*
    	This method takes a named individual, the IRI of an annotation property, and returns the literal value
    		of any annotations the individual has using the annotation property.

    		It's an easy way to get the label, preferred term, URL, etc. from an individual
    */
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

    /*
    	This method handles the "web application" attribute.  Typically it's a URL to a web page that 
    		interactively runs the software in real time for a user.
    */
    public static void handleWebApplication(Map.Entry<String, JsonElement> e, HashMap<String, OWLNamedIndividual> niMap,
                                            OWLOntology oo, OWLDataFactory odf, IriLookup iriMap) {
        OWLNamedIndividual oni = niMap.get("webexecutionof");
        JsonElement je = e.getValue();
        if (je instanceof JsonArray) {
            JsonArray elemArray = (JsonArray) je;
            Iterator<JsonElement> elemIter = elemArray.iterator();
            //System.out.println("Web application size: " + elemArray.size());
            while (elemIter.hasNext()) {
                JsonElement elemi = elemIter.next();
                String value = ((JsonPrimitive) elemi).getAsString();
                //value is just a URL so slap it on the web execution of dtm individual
                addAnnotationToNamedIndividual(oni, iriMap.lookupAnnPropIri("hasURL"), value, odf, oo);
            }
        } else {
            System.err.println("Web application attribute has value that is not primitive.");
            throw new IllegalArgumentException("Web application attribute must be array.");
        }
    }

    /*
    	This method handles the "website" attribute, which used to be called "location", hence the 
    		historical name.  The website typically differs from the source code repository in that
    		it's more documentation for the software with links to source code, executables, other
    		documents (especially PDF user guides), etc.  It's more descriptive of the software
    		than the actual location of the software itself.
    */
    public static void handleLocation(Map.Entry<String, JsonElement> e, HashMap<String, OWLNamedIndividual> niMap,
                                      OWLOntology oo, OWLDataFactory odf, IriLookup iriMap) {
        OWLNamedIndividual oni = niMap.get("website");
        JsonElement je = e.getValue();
        if (je instanceof JsonPrimitive) {
            String value = ((JsonPrimitive) je).getAsString();
            //value is just a URL so slap it on the website individual
            //System.out.println("WEBSITE URL = " + value + ", " + iriMap.lookupAnnPropIri("hasURL") + ", " + odf + "," + oo + ", " + oni);
            addAnnotationToNamedIndividual(oni, iriMap.lookupAnnPropIri("hasURL"), value, odf, oo);

            if (websiteInds.containsKey(value)) {
                System.err.println("Possible duplicate website. Attribute value is: " + value + ", and " +
                        "there was a website with the same URL created previously with IRI " + websiteInds.get(value).getIRI() +
                        ". The current IRI is " + oni.getIRI());
            }
        } else {
            System.err.println("Location attribute has value that is not primitive.");
            throw new IllegalArgumentException("location attribute must be primitive.");
        }
    }

    /*
    	This method handles the "documentation" attribute. Typically we get URLs to PDFs and websites
    		for user guides, developer guides, etc.
    */
    public static void handleDocumentation(Map.Entry<String, JsonElement> e, HashMap<String, OWLNamedIndividual> niMap,
                                           OWLOntology oo, OWLDataFactory odf, IriLookup iriMap) {
        OWLNamedIndividual oni = niMap.get("documentation");
        JsonElement je = e.getValue();
        if (je instanceof JsonPrimitive) {
            String value = ((JsonPrimitive) je).getAsString();
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
            JsonArray ja = (JsonArray) je;
            //System.out.println("DOCUMENTATION SIZE = " + ja.size());
            Iterator<JsonElement> elemIter = ja.iterator();
            while (elemIter.hasNext()) {
                JsonElement elemi = elemIter.next();
                int iDoc = 1;
                if (elemi instanceof JsonPrimitive) {
                    String value = ((JsonPrimitive) elemi).getAsString();
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

    /*
    	This method handles the "developers" attribute, whose value(s) represents the people and/or
    		organizations involved in the creation of the software source code. Regardless of how 
    		many developers there are (even if there's just one), the values are in a JSON array.
    */
    public static void handleDeveloper(Map.Entry<String, JsonElement> e, HashMap<String, OWLNamedIndividual> niMap,
                                       OWLOntology oo, OWLDataFactory odf, IriLookup iriMap) {
        /*
        	We connect developers to the software through the process of writing it, so we have both the 
        		developer individual and the codewriting individual.
        */
        OWLNamedIndividual devInd = niMap.get("developer");
        OWLNamedIndividual wrtInd = niMap.get("codewriting");

        JsonElement je = e.getValue();
        if (je instanceof JsonPrimitive) {
        	/*
        		This should no longer happen.  In early version of the JSON, occasionally the 
        			developers were a primitive value and multiple developers names were
        			separated by commas.
        	*/
            String value = ((JsonPrimitive) je).getAsString();
            String[] devs = (value.contains(";")) ? value.split(Pattern.quote(";")) : value.split(Pattern.quote(","));
            //System.out.println("There are " + devs.length + " developers:");

            //Because this situation shouldn't happen any more, report it out as an error:
            System.err.println("Developer attribute has value that is primitive.");
        } else {
        	/*
        		This is what we expect: developer values are in JsonArray even if just one.

        		If there's no developers specified by JSON, then we shouldn't even have the
        		attribute.
        	*/
            JsonArray devArray = (JsonArray) je;
            String[] devs = new String[devArray.size()];
            Iterator<JsonElement> devIter = devArray.iterator();
            int iDev = 0;
            while (devIter.hasNext()) {
                JsonElement devElement = devIter.next();
                devs[iDev++] = ((JsonPrimitive) devElement).getAsString();
            }

            //ArrayList<OWLNamedIndividual> devNis = new ArrayList<OWLNamedIndividual>();
            //devNis.add(devInd);

            String[] devNames = new String[devs.length];
            for (int i = 0; i < devs.length; i++) {
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
                    if (!devNis.containsKey(devNames[i])) {
                        devi = createNamedIndividualWithTypeAndLabel(odf, oo, iriMap.lookupClassIri("developer"), iriMap.lookupAnnPropIri("label"), devNames[i]);
                        OWLNamedIndividual lpri = createNamedIndividualWithTypeAndLabel(odf, oo, iriMap.lookupClassIri("legalpersonrole"),
                                iriMap.lookupAnnPropIri("label"), "legal person role of " + devNames[i]);
                        createOWLObjectPropertyAssertion(devi, iriMap.lookupObjPropIri("bearer"), lpri, odf, oo);
                        devNis.put(devNames[i], devi);
                    } else {
                        devi = devNis.get(devNames[i]);
                    }
                    addAnnotationToNamedIndividual(devi, iriMap.lookupAnnPropIri("editor preferred"), devNames[i] + ", developer of " + fullName, odf, oo);
                    OWLNamedIndividual emailInd = createNamedIndividualWithTypeAndLabel(odf, oo, iriMap.lookupClassIri("email address"), iriMap.lookupAnnPropIri("label"), url);
                    createOWLObjectPropertyAssertion(emailInd, iriMap.lookupObjPropIri("is contact information about"), devi, odf, oo);
                    createOWLObjectPropertyAssertion(wrtInd, iriMap.lookupObjPropIri("has active participant"), devi, odf, oo);
                } else {
                    devNames[i] = devs[i].trim();
                    OWLNamedIndividual devi = null;
                    if (!devNis.containsKey(devNames[i])) {
                        devi = createNamedIndividualWithTypeAndLabel(odf, oo, iriMap.lookupClassIri("developer"), iriMap.lookupAnnPropIri("label"), devNames[i]);
                        OWLNamedIndividual lpri = createNamedIndividualWithTypeAndLabel(odf, oo, iriMap.lookupClassIri("legalpersonrole"),
                                iriMap.lookupAnnPropIri("label"), "legal person role of " + devNames[i]);
                        createOWLObjectPropertyAssertion(devi, iriMap.lookupObjPropIri("bearer"), lpri, odf, oo);
                        devNis.put(devNames[i], devi);
                    } else {
                        devi = devNis.get(devNames[i]);
                    }
                    createOWLObjectPropertyAssertion(wrtInd, iriMap.lookupObjPropIri("has active participant"), devi, odf, oo);
                    addAnnotationToNamedIndividual(devi, iriMap.lookupAnnPropIri("editor preferred"), devNames[i] + ", developer of " + fullName, odf, oo);
                }
                //System.out.println(i);
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
            JsonArray elemArray = (JsonArray) je;
            Iterator<JsonElement> elemIter = elemArray.iterator();

            ArrayList<String> pubInfo = new ArrayList<String>();
            ArrayList<OWLNamedIndividual> execInds = new ArrayList<OWLNamedIndividual>();
            ArrayList<OWLNamedIndividual> studyInds = new ArrayList<OWLNamedIndividual>();
            while (elemIter.hasNext()) {
                JsonElement elemi = elemIter.next();
                String value = ((JsonPrimitive) elemi).getAsString();

                if (value.contains("<li>")) {
                    Document d = Jsoup.parse(value);
                    Elements pubs = d.select("li");
                    System.out.println("THERE ARE " + pubs.size() + " pubs that used release.");
                    for (Element pub : pubs) {
                        pubInfo.add(pub.ownText().trim());
                        System.out.println("PUB USED: " + pub.ownText());
                    }
                } else {
                    String[] pubs = value.split(Pattern.quote(";"));
                    //System.out.println("value = " + value);
                    //System.out.println("THERE ARE " + pubs.length + " pubs that used release.");
                    for (int i = 0; i < pubs.length; i++) {
                        pubInfo.add(pubs[i].trim());
                        System.out.println("PUB USED: " + pubs[i]);
                    }
                }
            }

            addAnnotationToNamedIndividual(execInd, iriMap.lookupAnnPropIri("label"), "execution of dtm for study described in " + pubInfo.get(0), odf, oo);
            addAnnotationToNamedIndividual(studyInd, iriMap.lookupAnnPropIri("label"), "study process described in " + pubInfo.get(0), odf, oo);

            IRI pubIri = pubLinks.get(pubInfo.get(0));
            System.out.println("PUB USING IRI " + pubIri);
            if (pubIri != null) {
                OWLNamedIndividual pub = odf.getOWLNamedIndividual(pubIri);
                createOWLObjectPropertyAssertion(pub, iriMap.lookupObjPropIri("is about"), studyInd, odf, oo);
            }


            for (int i = 1; i < pubInfo.size(); i++) {
                OWLNamedIndividual execi = createNamedIndividualWithTypeAndLabel(odf, oo, iriMap.lookupClassIri("executionof"), iriMap.lookupAnnPropIri("label"), "execution of dtm for study described in " + pubInfo.get(i));
                OWLNamedIndividual studyi = createNamedIndividualWithTypeAndLabel(odf, oo, iriMap.lookupClassIri("studyexecution"), iriMap.lookupAnnPropIri("label"), "study process described in " + pubInfo.get(i));

                createOWLObjectPropertyAssertion(execi, iriMap.lookupObjPropIri("achieves objective"), executableInd, odf, oo);
                createOWLObjectPropertyAssertion(execi, iriMap.lookupObjPropIri("is part of"), studyi, odf, oo);

                pubIri = pubLinks.get(pubInfo.get(i));
                System.out.println("PUB USING IRI " + pubIri);
                if (pubIri != null) {
                    OWLNamedIndividual pub = odf.getOWLNamedIndividual(pubIri);
                    createOWLObjectPropertyAssertion(pub, iriMap.lookupObjPropIri("is about"), studyi, odf, oo);
                }
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
            JsonArray elemArray = (JsonArray) je;
            Iterator<JsonElement> elemIter = elemArray.iterator();
            int size = elemArray.size();
            while (elemIter.hasNext()) {
                JsonElement elemi = elemIter.next();
                if (elemi instanceof JsonPrimitive) {
                    String value = ((JsonPrimitive) elemi).getAsString();
                    if (value.equals("olympus")) {
                        createOWLObjectPropertyAssertion(olympus, iriMap.lookupObjPropIri("bearer"), execConcInd, odf, oo);
                    } else if (value.equals("udsi") || value.equals("uids")) {
                        handleUids(value, size, executableInd, execConcInd, iriMap, odf, oo);
                    } else {
                        throw new IllegalArgumentException("value of availableAt must be 'olympus' or 'uids'");
                    }
                } else {
                    throw new IllegalArgumentException("value of availableAt must be primitive");
                }
            }
        } else {
            throw new IllegalArgumentException("value of availableAt must be array");
        }
    }

    public static void handleDiseases(Map.Entry<String, JsonElement> e, HashMap<String, OWLNamedIndividual> niMap,
                                         OWLOntology oo, OWLDataFactory odf, IriLookup iriMap) {

    }
                      
    public static void handleRegion(Map.Entry<String, JsonElement> e, HashMap<String, OWLNamedIndividual> niMap,
                                         OWLOntology oo, OWLDataFactory odf, IriLookup iriMap) {

    }

    public static void handleUids(String value, int size, OWLNamedIndividual execInd, OWLNamedIndividual uidsConcInd, IriLookup iriMap, OWLDataFactory odf, OWLOntology oo) {
        if (uidsConcInd == null) throw new IllegalArgumentException("uidsConcInd cannot be null");
		/*if (size > 1) {
		    /*  This means that we also have Olympus, but the concretization of the executable available through UIDS is likely a different copy
			than the one on Olympus, so we're going to clone the executable concretization.  Also it could be that the compiled executables 
			b/w UIDS and Olympus are from different compiling processes, but I'm not going that far.  They should be identical anyway
		  
		    
		    String execConcIndPrefTerm = getAnnotationValueFromNamedIndividual(execConcInd, iriMap.lookupAnnPropIri("editor preferred"), oo);
		    execConcIndPrefTerm += " available through UIDS";
		    execConcInd = createNamedIndividualWithTypeAndLabel(odf, oo,  iriMap.lookupClassIri("executableConcretization"), iriMap.lookupAnnPropIri("editor preferred"), execConcIndPrefTerm);
		    //connect executable of DTM to concretization of executable of DTM.  If we didn't create new concretization, then it's already connected.
		    createOWLObjectPropertyAssertion(execInd, iriMap.lookupObjPropIri("is concretized as"), execConcInd, odf, oo);
		}*/

        String uidsConcIndPrefTerm = "Concretization of " + fullName + " available through, but not as part of, UIDS";
        addAnnotationToNamedIndividual(uidsConcInd, iriMap.lookupAnnPropIri("editor preferred"), uidsConcIndPrefTerm, odf, oo);
        //create disposition to invoke DTM
        String invokeDispPrefTerm = "disposition of UIDS to invoke " + fullName;
        OWLNamedIndividual invokeDisp = createNamedIndividualWithTypeAndLabel(odf, oo, iriMap.lookupClassIri("disposition"), iriMap.lookupAnnPropIri("editor preferred"), invokeDispPrefTerm);

        //connect concretization of UIDS to dispostion to invoke - disp has basis in concretization
        createOWLObjectPropertyAssertion(invokeDisp, iriMap.lookupObjPropIri("has basis in"), uidsExecutableConcretization, odf, oo);

        //create disposition to execute DTM
        String executeDispPrefTerm = "disposition of remote service to execute " + fullName;
        OWLNamedIndividual executeDisp = createNamedIndividualWithTypeAndLabel(odf, oo, iriMap.lookupClassIri("disposition"), iriMap.lookupAnnPropIri("editor preferred"), executeDispPrefTerm);

        //connect disposition to execute to DTM concretization of executable of DTM - execute disp has basis in the exec conc for uids
        createOWLObjectPropertyAssertion(executeDisp, iriMap.lookupObjPropIri("has basis in"), uidsConcInd, odf, oo);

        //connect dispositions
        createOWLObjectPropertyAssertion(invokeDisp, iriMap.lookupObjPropIri("s-depends"), executeDisp, odf, oo);
    }

    public static void handleControlMeasures(Map.Entry<String, JsonElement> e, HashMap<String, OWLNamedIndividual> niMap,
                                             OWLOntology oo, OWLDataFactory odf, IriLookup iriMap) {
        JsonElement je = e.getValue();
        if (je instanceof JsonArray) {
            JsonArray elemArray = (JsonArray) je;
            Iterator<JsonElement> elemIter = elemArray.iterator();
            int size = elemArray.size();
            while (elemIter.hasNext()) {
                JsonElement elemi = elemIter.next();
                if (elemi instanceof JsonPrimitive) {
                    String value = ((JsonPrimitive) elemi).getAsString();
                    uniqueCms.add(value);

                    if (cmMap.containsKey(value)) {
                        IRI classIri = cmMap.get(value);
                        String cmInstanceLabel = value + " control measure by " + fullName;
                        String simxInstanceLabel = "simulating of epidemic with " + value + " control measure by " + fullName;
                        OWLNamedIndividual simxInd = createNamedIndividualWithTypeAndLabel(odf, oo, iriMap.lookupClassIri("simulatingx"), iriMap.lookupAnnPropIri("editor preferred"), simxInstanceLabel);
                        OWLNamedIndividual cmInd = createNamedIndividualWithTypeAndLabel(odf, oo, classIri, iriMap.lookupAnnPropIri("editor preferred"), cmInstanceLabel);
                        addAnnotationToNamedIndividual(cmInd, iriMap.lookupAnnPropIri("label"), value, odf, oo);

                        createOWLObjectPropertyAssertion(simxInd, iriMap.lookupObjPropIri("achieves objective"), niMap.get("executable"), odf, oo);
                        createOWLObjectPropertyAssertion(simxInd, iriMap.lookupObjPropIri("has specified output"), cmInd, odf, oo);
                    } else {
                        System.err.println("Skipping " + value + " control measure.");
                    }
                }
            }
        } else {
            throw new IllegalArgumentException("value of controlMeasures attribute must be array");
        }
    }

    public static void handleDataInputFormats(Map.Entry<String, JsonElement> e, HashMap<String, OWLNamedIndividual> niMap,
                                              OWLOntology oo, OWLDataFactory odf, IriLookup iriMap) {
        JsonElement je = e.getValue();
        if (je instanceof JsonArray) {
            JsonArray elemArray = (JsonArray) je;
            Iterator<JsonElement> elemIter = elemArray.iterator();
            int size = elemArray.size();
            while (elemIter.hasNext()) {
                JsonElement elemi = elemIter.next();
                if (elemi instanceof JsonPrimitive) {
                    String value = ((JsonPrimitive) elemi).getAsString().trim();
                    if (value.trim().startsWith("<a href")) {
                        Document d = Jsoup.parse(value);
                        Elements links = d.select("a");
                        String url = links.get(0).attr("href");
                        value = links.get(0).ownText();
                    }
                    uniqueInputFormats.add(value);

                    OWLNamedIndividual formatInd = formatInds.get(value);
                    if (formatInd != null) {
                        String planSpecLabel = "data parsing plan specification for format " + value + " as part of " + fullName;
                        String dataParsingLabel = "data parsing of file in " + value + " format by " + fullName;
                        OWLNamedIndividual planSpecInd = createNamedIndividualWithTypeAndLabel(odf, oo, iriMap.lookupClassIri("executableParsingPlan"),
                                iriMap.lookupAnnPropIri("editor preferred"), planSpecLabel);
                        OWLNamedIndividual dataParsingInd = createNamedIndividualWithTypeAndLabel(odf, oo, iriMap.lookupClassIri("dataparsing"),
                                iriMap.lookupAnnPropIri("editor preferred"), dataParsingLabel);

                        //connect parsing to format
                        createOWLObjectPropertyAssertion(dataParsingInd, iriMap.lookupObjPropIri("achieves objective"), formatInd, odf, oo);

                        //connect parsing to plannedSpec
                        createOWLObjectPropertyAssertion(dataParsingInd, iriMap.lookupObjPropIri("achieves objective"), planSpecInd, odf, oo);

                        //connect plannedSpec to executable
                        createOWLObjectPropertyAssertion(planSpecInd, iriMap.lookupObjPropIri("is part of"), niMap.get("executable"), odf, oo);
                    } else {
                        System.err.println("UNRECOGNIZED DATA INPUT FORMAT: " + value);
                    }
                }
            }
        } else {
            System.err.println("value of dataInputFormats attribute must be array. Ignoring " + je.getAsString());
        }
    }

    public static void handleDataOutputFormats(Map.Entry<String, JsonElement> e, HashMap<String, OWLNamedIndividual> niMap,
                                               OWLOntology oo, OWLDataFactory odf, IriLookup iriMap) {
        JsonElement je = e.getValue();
        if (je instanceof JsonArray) {
            JsonArray elemArray = (JsonArray) je;
            Iterator<JsonElement> elemIter = elemArray.iterator();
            int size = elemArray.size();
            while (elemIter.hasNext()) {
                JsonElement elemi = elemIter.next();
                if (elemi instanceof JsonPrimitive) {
                    String value = ((JsonPrimitive) elemi).getAsString();
                    uniqueOutputFormats.add(value);

                    OWLNamedIndividual formatInd = formatInds.get(value);
                    if (formatInd != null) {
                        String planSpecLabel = "data encoding plan specification for format " + value + " as part of " + fullName;
                        String dataWritingLabel = "data encoding of file in " + value + " format by " + fullName;
                        OWLNamedIndividual planSpecInd = createNamedIndividualWithTypeAndLabel(odf, oo, iriMap.lookupClassIri("executableEncodingPlan"),
                                iriMap.lookupAnnPropIri("editor preferred"), planSpecLabel);
                        OWLNamedIndividual dataWritingInd = createNamedIndividualWithTypeAndLabel(odf, oo, iriMap.lookupClassIri("dataencoding"),
                                iriMap.lookupAnnPropIri("editor preferred"), dataWritingLabel);

                        //connect parsing to format
                        createOWLObjectPropertyAssertion(dataWritingInd, iriMap.lookupObjPropIri("achieves objective"), formatInd, odf, oo);

                        //connect parsing to plannedSpec
                        createOWLObjectPropertyAssertion(dataWritingInd, iriMap.lookupObjPropIri("achieves objective"), planSpecInd, odf, oo);

                        //connect plannedSpec to executable
                        createOWLObjectPropertyAssertion(planSpecInd, iriMap.lookupObjPropIri("is part of"), niMap.get("executable"), odf, oo);
                    } else {
                        System.err.println("UNRECOGNIZED DATA OUTPUT FORMAT: " + value);
                    }
                }
            }
        } else {
            System.err.println("value of dataOutputFormats attribute must be array.  Ignoring " + je.getAsString());
        }
    }

    public static void handlePublicationsAbout(Map.Entry<String, JsonElement> e, HashMap<String, OWLNamedIndividual> niMap,
                                               OWLOntology oo, OWLDataFactory odf, IriLookup iriMap) {
        JsonElement je = e.getValue();
        if (je instanceof JsonArray) {
            JsonArray elemArray = (JsonArray) je;
            Iterator<JsonElement> elemIter = elemArray.iterator();
            int size = elemArray.size();
            while (elemIter.hasNext()) {
                JsonElement elemi = elemIter.next();
                if (elemi instanceof JsonPrimitive) {
                    String value = ((JsonPrimitive) elemi).getAsString();
                    System.out.println("PUB ABOUT " + value);
                    IRI pubIri = pubLinks.get(value);
                    System.out.println("PUB ABOUT IRI = " + pubIri);
                    if (pubIri != null) {
                        OWLNamedIndividual pub = odf.getOWLNamedIndividual(pubIri);
                        OWLNamedIndividual software = niMap.get("dtm");
                        createOWLObjectPropertyAssertion(pub, iriMap.lookupObjPropIri("is about"), software, odf, oo);
                    }
                }
            }
        } else {
            throw new IllegalArgumentException("value of publicationsAboutRelease must be array");
        }
    }

    public static OWLNamedIndividual createOlympusIndividuals(OWLDataFactory odf, OWLOntology oo, IriLookup iriMap) {
        OWLNamedIndividual oni = odf.getOWLNamedIndividual(iriMap.lookupIndividIri("olympus"));
        //Deleting this.  Olympus class assertion axiom occurs in OBC.ide OWL file, so we don't want/need to re-iterate it here.
        //OWLClassAssertionAxiom ocaaTemp = odf.getOWLClassAssertionAxiom(odf.getOWLClass(iriMap.lookupClassIri("compute cluster")), oni);
        //oom.addAxiom(oo,ocaaTemp);
        //The label is now asserted in the OBC.ide OWL File so we don't need to assert it here
        //addAnnotationToNamedIndividual(oni, iriMap.lookupAnnPropIri("label"), "Olympus", odf, oo);

        OWLNamedIndividual olympusWebsite = odf.getOWLNamedIndividual(nextIri());
        OWLClassAssertionAxiom ocaa = odf.getOWLClassAssertionAxiom(odf.getOWLClass(iriMap.lookupClassIri("website")), olympusWebsite);
        oom.addAxiom(oo, ocaa);
        addAnnotationToNamedIndividual(olympusWebsite, iriMap.lookupAnnPropIri("editor preferred"), "Olympus home page", odf, oo);
        addAnnotationToNamedIndividual(olympusWebsite, iriMap.lookupAnnPropIri("title"), "Olympus", odf, oo);
        addAnnotationToNamedIndividual(olympusWebsite, iriMap.lookupAnnPropIri("hasURL"), "https://www.psc.edu/resources/computing/olympus", odf, oo);
        addAnnotationToNamedIndividual(olympusWebsite, iriMap.lookupAnnPropIri("authors"), "Pittsburgh Supercomputing Center", odf, oo);
        addAnnotationToNamedIndividual(olympusWebsite, iriMap.lookupAnnPropIri("published date"), "2016-03-02", odf, oo);

        createOWLObjectPropertyAssertion(olympusWebsite, iriMap.lookupObjPropIri("is about"), oni, odf, oo);
        return oni;
    }

    public static OWLNamedIndividual createUidsIndividuals(OWLDataFactory odf, OWLOntology oo, IriLookup iriMap) {
        OWLNamedIndividual oni = odf.getOWLNamedIndividual(nextIri());
        OWLClassAssertionAxiom ocaaTemp = odf.getOWLClassAssertionAxiom(odf.getOWLClass(iriMap.lookupClassIri("software")), oni);
        oom.addAxiom(oo, ocaaTemp);
        addAnnotationToNamedIndividual(oni, iriMap.lookupAnnPropIri("label"), "Universal Interface to Disease Simulators (UIDS) version 4.0.1", odf, oo);
        addAnnotationToNamedIndividual(oni, iriMap.lookupAnnPropIri("editor preferred"), "The Apollo Universal Interface to Disease Simulators (UIDS) version 4.0.1", odf, oo);
        addAnnotationToNamedIndividual(oni, iriMap.lookupAnnPropIri("hasURL"), "https://github.com/ApolloDev/simple-end-user-apollo-web-application/tree/38161eba742a9000bb610aa47419f7fb62f0c3ac", odf, oo);
        addAnnotationToNamedIndividual(oni, iriMap.lookupAnnPropIri("synopsis"), "The UIDS (Universal Interface to Disease Simulators) is a web application that assists a user in the construction and simulation of infectious disease scenarios (scenarios).  The UIDS xml-encodes scenarios according to the Apollo-XSD InfectiousDiseaseScenario type.  A user can construct a scenario de novo or download an existing scenario from the Apollo Library.   UIDS can transmit a scenario to a disease transmission simulator by calling the Apollo Broker service <insert URL>.  UIDS reports on the status of a requested run of a disease transmission simulator and displays maps and time series graphs of simulator output.  UIDS can also save a scenario to the Apollo Library <insert URL>.", odf, oo);

        OWLNamedIndividual uidsWebsite = odf.getOWLNamedIndividual(nextIri());
        OWLClassAssertionAxiom ocaa = odf.getOWLClassAssertionAxiom(odf.getOWLClass(iriMap.lookupClassIri("website")), uidsWebsite);
        oom.addAxiom(oo, ocaa);
        addAnnotationToNamedIndividual(uidsWebsite, iriMap.lookupAnnPropIri("editor preferred"), "UIDS home page", odf, oo);
        addAnnotationToNamedIndividual(uidsWebsite, iriMap.lookupAnnPropIri("title"), "UIDS home", odf, oo);
        addAnnotationToNamedIndividual(uidsWebsite, iriMap.lookupAnnPropIri("hasURL"), "https://research.rods.pitt.edu/apollo-web-client/index.php", odf, oo);
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

   /*
    	This method is a facade in front of the OWLAPI to simplify creating a new individual with its
    		first text-based (i.e. literal) annotation.  The IRI assigned to the individual is 
    		created by calling the nextIri() method.
    */
    public static OWLNamedIndividual createNamedIndividualWithTypeAndLabel(
            OWLDataFactory odf, OWLOntology oo, IRI classTypeIri, IRI labelPropIri, String rdfsLabel) {
        return createNamedIndividualWithIriTypeAndLabel(nextIri(), odf, oo, classTypeIri, labelPropIri, rdfsLabel);
    }

   /*
    	This method is a facade in front of the OWLAPI to simplify creating a new individual with its
    		first text-based (i.e. literal) annotation and the given IRI.
    */
    public static OWLNamedIndividual createNamedIndividualWithIriTypeAndLabel(
            IRI individualIri, OWLDataFactory odf, OWLOntology oo, IRI classTypeIri, IRI labelPropIri, String rdfsLabel) {
        OWLNamedIndividual oni = odf.getOWLNamedIndividual(individualIri);
        OWLClassAssertionAxiom ocaa = odf.getOWLClassAssertionAxiom(odf.getOWLClass(classTypeIri), oni);
        oom.addAxiom(oo, ocaa);
        addAnnotationToNamedIndividual(oni, labelPropIri, rdfsLabel, odf, oo);
        return oni;
    }

    /*
    	This method is a facade in front of the OWLAPI to simplify adding an annotation to an individual.
    */
    public static void addAnnotationToNamedIndividual(OWLNamedIndividual oni, IRI annPropIri, String value, OWLDataFactory odf, OWLOntology oo) {
        OWLLiteral li = odf.getOWLLiteral(value);
        OWLAnnotationProperty la = odf.getOWLAnnotationProperty(annPropIri);
        OWLAnnotation oa = odf.getOWLAnnotation(la, li);
        OWLAnnotationAssertionAxiom oaaa = odf.getOWLAnnotationAssertionAxiom(oni.getIRI(), oa);
        oom.addAxiom(oo, oaaa);
    }

    public static void connectDtmIndividuals(HashMap<String, OWLNamedIndividual> niMap, OWLOntology oo, OWLDataFactory odf, IriLookup iriMap) {
        //DtmIndivConnectGuide rules = new DtmIndivConnectGuide("./src/main/resources/obj_prop_assertions.txt");
        DtmIndivConnectGuide rules = new DtmIndivConnectGuide(p.getProperty("op_assertions"));
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

    public static void handleSimInds(HashMap<String, OWLNamedIndividual> niMap, OWLOntology oo, OWLDataFactory odf, IriLookup iriMap) throws IOException {
        String simIndSetsTxt = dtmToSimInds.get(fullName);
        if (simIndSetsTxt == null) return;
        String[] simIndSets = simIndSetsTxt.split(Pattern.quote("|"));
        for (String indSet : simIndSets) {
            String[] simInds = indSet.split(Pattern.quote(","));
            OWLNamedIndividual simulating = null;
            String simText = null;
            for (String s : simInds) {
                if (s.startsWith("simulating")) {
                    simText = fullName + " " + s;
                    simulating = createNamedIndividualWithIriTypeAndLabel(nextSimPopIri(), odf, oo, iriMap.lookupClassIri("simulatingx"), iriMap.lookupAnnPropIri("editor preferred"), simText);
                    createOWLObjectPropertyAssertion(simulating, iriMap.lookupObjPropIri("achieves objective"), niMap.get("executable"), odf, oo);
                } else if (s.startsWith("host")) {
                    String popName = fullName + " simulated " + s;
                    //		    System.out.println("POP NAME: " + popName);
                    IRI simPopIri = IRI.create(simPopIris.get(popName));
                    OWLNamedIndividual pop = createNamedIndividualWithIriTypeAndLabel(simPopIri, odf, oo, iriMap.lookupClassIri("simulatedhostpopulation"), iriMap.lookupAnnPropIri("label"), popName);
                    createOWLObjectPropertyAssertion(simulating, iriMap.lookupObjPropIri("has specified output"), pop, odf, oo);
                    simPops.write(fullName + "\t" + simText + "\t" + simPopIri + "\t" + popName + "\n");
                } else if (s.startsWith("pathogen")) {
                    String popName = fullName + " simulated " + s;
                    IRI simPopIri = IRI.create(simPopIris.get(popName));
                    OWLNamedIndividual pop = createNamedIndividualWithIriTypeAndLabel(simPopIri, odf, oo, iriMap.lookupClassIri("simulatedpathogenpopulation"), iriMap.lookupAnnPropIri("label"), popName);
                    createOWLObjectPropertyAssertion(simulating, iriMap.lookupObjPropIri("has specified output"), pop, odf, oo);
                    simPops.write(fullName + "\t" + simText + "\t" + simPopIri + "\t" + popName + "\n");
                } else {
                    throw new IllegalArgumentException("don't understand " + s);
                }
            }
        }
    }

    /*
    	This code is a facade in front of the OWLAPI that simplifies creating an object property assertion
    		to connect two OWLIndividuals using an OWLObjectProperty (a.k.a relation).
    */
    public static void createOWLObjectPropertyAssertion(OWLNamedIndividual source, IRI objPropIri, OWLNamedIndividual target, OWLDataFactory odf, OWLOntology oo) {
        OWLObjectProperty oop = odf.getOWLObjectProperty(objPropIri);
        OWLObjectPropertyAssertionAxiom oopaa = odf.getOWLObjectPropertyAssertionAxiom(oop, source, target);
        oom.addAxiom(oo, oopaa);
    }

    /*
    	The method that generates sequentially numbered IRIs using the proper 
    		base for the OBC
    */
    public static IRI nextIri() {
        String counterTxt = Long.toString(iriCounter++);
        StringBuilder sb = new StringBuilder(iriPrefix);
        int numZero = 10 - counterTxt.length();
        for (int i = 0; i < numZero; i++)
            sb.append("0");
        sb.append(counterTxt);
        return IRI.create(new String(sb));
    }

    public static IRI nextSimPopIri() {
        String counterTxt = Long.toString(reservedSimPopIriCounter++);
        StringBuilder sb = new StringBuilder(iriPrefix);
        int numZero = 10 - counterTxt.length();
        for (int i = 0; i < numZero; i++)
            sb.append("0");
        sb.append(counterTxt);
        return IRI.create(new String(sb));
    }

    /*
    	Using separate Python code, we parse out and manually curate information from the DATS 2.2
    		representations of data formats in the MDC.  In separate Java code, we load in that 
    		curated data and create an OWLIndividual for each data format.  That code also outputs
    		a mapping from data format ID to IRI.  Here, we load in that mapping so we can connect
    		software/data services to thier input and output formats.
    */
    public static void loadAndCreateDataFormatIndividuals(OWLDataFactory odf, OWLOntology oo, IriLookup iriMap) throws IOException {
        formatInds = new HashMap<String, OWLNamedIndividual>();
        //FileReader fr = new FileReader("./src/main/resources/data_format_metadata-2017-05-25T1630.txt");
        FileReader fr = new FileReader(p.getProperty("format_info"));
        LineNumberReader lnr = new LineNumberReader(fr);
        String line;
        while ((line = lnr.readLine()) != null) {
            String[] flds = line.split(Pattern.quote("\t"), -1);
            String iriTxt = flds[0];
            String label = flds[1];
            String[] keys = flds[14].split(Pattern.quote(";"));

            OWLNamedIndividual formatInd = odf.getOWLNamedIndividual(IRI.create(iriTxt));

            for (String key : keys) {
                formatInds.put(key, formatInd);
            }
        }
        lnr.close();
        fr.close();
    }

    public static void loadSimPopsPerDtm(String fName) throws IOException {
        FileReader fr = new FileReader(fName);
        LineNumberReader lnr = new LineNumberReader(fr);
        String line;
        while ((line = lnr.readLine()) != null) {
            String[] flds = line.split(Pattern.quote("\t"));
        }
        lnr.close();
        fr.close();
    }

    public static void initializeDtmSimInds(String fName) throws IOException {
        dtmToSimInds = new HashMap<String, String>();
        FileReader fr = new FileReader(fName);
        LineNumberReader lnr = new LineNumberReader(fr);
        String line;
        while ((line = lnr.readLine()) != null) {
            String[] flds = line.split(Pattern.quote("\t"));
            dtmToSimInds.put(flds[0], flds[1]);
        }
        lnr.close();
        fr.close();
    }

    public static void initializeSimPopIris(String fName) throws IOException {
        simPopIris = new HashMap<String, String>();
        FileReader fr = new FileReader(fName);
        LineNumberReader lnr = new LineNumberReader(fr);
        String line;
        while ((line = lnr.readLine()) != null) {
            String[] flds = line.split(Pattern.quote("\t"));
            simPopIris.put(flds[3], flds[2]);
        }
        lnr.close();
        fr.close();
    }

    /*
    	Although the MDC does not treat content licenses (a category that includes software
    	licenses including open-source licenses) as full-fledged digital objects, we do.

    	We map all the non-computable variety of strings for open-source licenses to a single
    		OWLIndividual, and connect the software/data format to the individual that 
    		represents the license under which it is released.

    	Therefore, we have better semantics and can query, for example, reliably by license.
    		For example, we can query for all artifacts licensed under the Apache 2.0 license.
    */
    public static void loadLicenses(String fName, OWLDataFactory odf) throws IOException {
        licenseNis = new HashMap<String, OWLNamedIndividual>();
        FileReader fr = new FileReader(fName);
        LineNumberReader lnr = new LineNumberReader(fr);
        String line;
        while ((line = lnr.readLine()) != null) {
            String[] flds = line.split(Pattern.quote("\t"));
            OWLNamedIndividual license = odf.getOWLNamedIndividual(IRI.create(flds[0]));
            String[] indexTerms = flds[5].split(Pattern.quote(";"));
            for (String term : indexTerms) {
                licenseNis.put(term, license);
            }
        }
        lnr.close();
        fr.close();
    }

    /*
    	To ensure that the website for each digital object gets the same IRI every time,
    		we have pre-curated a mapping from the URL of the website to the IRI of an 
    		OWLIndividual that represents it.  Here we load the IRIs for the OWLIndividuals
    		for the websites, create the OWLIndividual using the IRI,
    		and hash them by their URLs.  
    */
    public static void loadWebsites(String fName, OWLDataFactory odf) throws IOException {
        websiteInds = new HashMap<String, OWLNamedIndividual>();
        FileReader fr = new FileReader(fName);
        LineNumberReader lnr = new LineNumberReader(fr);
        String line;
        while ((line = lnr.readLine()) != null) {
            String[] flds = line.split(Pattern.quote("\t"));
            OWLNamedIndividual website = odf.getOWLNamedIndividual(IRI.create(flds[1]));
            websiteInds.put(flds[0], website);
        }
        lnr.close();
        fr.close();
    }

    /*
    	This method sorts the digital objects we get from the MDC API to ensure that we 
    		process them in exactly the same order every time (modulo new objects and 
    		the degenerate case where the title or name of one or more objects changes).
    */
    private static JsonArray sortForEasyValidation(JsonArray jsonArray) {
        JsonArray sortedJsonArray = new JsonArray();
        List<JsonElement> jsonList = new ArrayList<JsonElement>();
        for (int i = 0; i < jsonArray.size(); i++) {
            jsonList.add(jsonArray.get(i));
        }

        Collections.sort(jsonList, new Comparator<JsonElement>() {
        	/*
        		We'll sort digital objects by title.  However, the data formats do not have
        			a title attribute, so if the title does not exist (i.e., either aTitle
        			or bTitle JsonElement is null), then we'll get the name attribute and 
        			use it instead.
        	*/
            public int compare(JsonElement a, JsonElement b) {
                if (a == null || b == null) {
                    System.err.println(a + "\n\n" + b);
                }
                
                JsonElement aTitle = a.getAsJsonObject().get("content").getAsJsonObject().get("title");
                if (aTitle == null) aTitle = a.getAsJsonObject().get("content").getAsJsonObject().get("name");
                
                JsonElement bTitle = b.getAsJsonObject().get("content").getAsJsonObject().get("title");
                if (bTitle == null) bTitle = b.getAsJsonObject().get("content").getAsJsonObject().get("name");
                
                if (aTitle == null || bTitle == null) {
                    System.err.println(aTitle + "\t" + bTitle);
                    System.err.println(a + "\n\n" + b);
                }
                return aTitle.toString().compareTo(bTitle.toString());
            }
        });

        for (int i = 0; i < jsonArray.size(); i++) {
            sortedJsonArray.add(jsonList.get(i));
        }
        return sortedJsonArray;
    }

}
