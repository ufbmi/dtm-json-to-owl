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
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLAnonymousIndividual;
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
//import edu.ufl.bmi.misc.SoftwareIoInfo;

public class DtmJsonProcessor {
    static long iriCounter = 1200008100L;
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

    //static SoftwareIoInfo ioInfo;

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

    static HashMap<String, String> forecasterIdToName;
    static HashMap<String, ArrayList<String>> forecasterIdToRegionCategories;
    static HashMap<String, ArrayList<String>> forecasterIdToYears;
    static HashMap<String, ArrayList<String>> regionTypeToRegionIris;
    static HashMap<String, String> forecasterIdToForecasts;
    static HashMap<String, String> forecasterIdToType;
    static HashMap<String, IRI> regionNameToHumanPopIri;
    static HashMap<String, IRI> yearRegionToDzCourseAggregateIri;

    static HashMap<String, OWLNamedIndividual> identifierToOwlIndividual;
    static HashSet<String> forecasterIds;

    static HashSet<String> attributesHandledInPostprocessing;

    static HashMap<String, OWLNamedIndividual> grantTxtToIndividual;
    
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
				String typeFragment = null;
				if (typeElem.isJsonPrimitive()) {
					JsonPrimitive typeValue = (JsonPrimitive)typeElem;
					String type = typeValue.getAsString();
					if (type.equals("edu.pitt.isg.mdc.dats2_2.Dataset") ||
						type.equals("edu.pitt.isg.mdc.dats2_2.DataStandard") ||
						type.equals("edu.pitt.isg.mdc.dats2_2.DatasetWithOrganization"))
						continue;
					String[] typeFragments = type.split(Pattern.quote("."));
					typeFragment = typeFragments[typeFragments.length-1];
					System.out.println("\n\nTYPE ATTRIBUTE HAS VALUE: " + typeFragment);
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

				String subtype = null;
                JsonElement je3 = contentObject.get("subtype");
                if (je3 == null) { 
                	subtype = typeFragment;
                } else if (je3.isJsonPrimitive()) {
                    JsonPrimitive jprim = (JsonPrimitive) je3;
                    subtype = jprim.getAsString();
                }
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
                        } else if (keyj.equals("softwareVersion")) {
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
                            ((Character.isDigit(versionSuffix.charAt(0))) ? "v" + versionSuffix : versionSuffix);
                    fullName = baseLabel;
                    //System.out.println("FULLNAME: " + fullName);
                    Iterator<String> k = reqInds.iterator();
                    //System.out.println("\t\t\treqInds.size() = " + reqInds.size());
                    IRI labelIri = iriMap.lookupAnnPropIri("editor preferred");
                    HashMap<String, OWLNamedIndividual> niMap = new HashMap<String, OWLNamedIndividual>();
                    while (k.hasNext()) {
                        String ks = k.next();
                        System.out.println("ks = " + ks); 
                        IRI classIri = (ks.equals("dtm")) ? iriMap.lookupClassIri(subtype) : iriMap.lookupClassIri(ks);
                        System.out.println("classIRI = " + classIri.toString());
                        //System.out.println("\t\t\t'" + ks + "'\t" + classIri);
                        String indLabel = fullName + " " + (
                        	(subtype.equals("MetagenomicAnalysis")) ? "metagenomic analysis" : subtype.substring(0, subtype.length() - 1));
                        indLabel = indLabel + ((ks.equals("dtm")) ? " software" : " " + ks);
                        OWLNamedIndividual oni = createNamedIndividualWithTypeAndLabel(odf, oo, classIri, labelIri, indLabel);
                        //if (ks.equals("dtm")) System.out.println("DTMINDLABEL: " + indLabel);
					    if (ks.equals("dtm")) {
					    	addAnnotationToIndividual(oni, iriMap.lookupAnnPropIri("label"), fullName, odf, oo);
					    	OWLNamedIndividual mdcInd = odf.getOWLNamedIndividual(iriMap.lookupIndividIri("mdc"));
							/*
								Add all software objects to MDC.
							*/
							createOWLObjectPropertyAssertion(mdcInd, iriMap.lookupObjPropIri("has proper part"), oni, odf, oo);
					    }
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

                    boolean hasIdentifier = false;
                    while (j.hasNext()) {
                        Map.Entry<String, JsonElement> ej = j.next();
                        String keyj = ej.getKey();
                        if (keyj.equals("title")) {
                            handleTitle(ej, niMap, oo, odf, iriMap);
                        } else if (keyj.equals("softwareVersion")) {
                            handleVersion(ej, niMap, oo, odf, iriMap);
                        } else if (keyj.equals("codeRepository") || keyj.equals("source")) {
                            handleSource(ej, niMap, oo, odf, iriMap);
                        } else if (keyj.equals("license")) {
                            handleLicense(ej, niMap, oo, odf, iriMap);
                        } else if (keyj.equals("doi")) {  //this attribute appears to be obsolete. Identifier is used.
                            handleDoi(ej, niMap, oo, odf, iriMap);
                        } else if (keyj.equals("sourceCodeRelease")) {
                            handleSourceCodeRelease(ej, niMap, oo, odf, iriMap);
                        } else if (keyj.equals("generalInfo") || keyj.equals("humanReadableSynopsis")) {
                            handleGeneralInfo(ej, niMap, oo, odf, iriMap);
                        } else if (keyj.equals("executables") || keyj.equals("binaryUrl")) {
                            handleExecutables(ej, niMap, oo, odf, iriMap);
                            fullNameToExecutable.put(fullName, niMap.get("executable"));
                        } else if (keyj.equals("webApplication")) {
                            handleWebApplication(ej, niMap, oo, odf, iriMap);
                        } else if (keyj.equals("location") || keyj.equals("site") || keyj.equals("website")) {
                            handleLocation(ej, niMap, oo, odf, iriMap);
                        } else if (keyj.equals("documentation") || keyj.startsWith("userGuides")) {
                            handleDocumentation(ej, niMap, oo, odf, iriMap);
                        } else if (keyj.equals("developer") || keyj.equals("developers") || keyj.equals("author") || keyj.equals("authors")) {
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
                            /*
                                We get data input formats from the inputs element now, and we now ignore
                                    any vestigaes of the old dataInputFormats / dataInputFormat elements.
                            */
                            //handleDataInputFormats(ej, niMap, oo, odf, iriMap);
                        } else if (keyj.equals("dataOutputFormats")) {
                            /*
                                We get data output formats from the outputs element now, and we now ignore
                                    any vestigaes of the old dataInputFormats / dataInputFormat elements.
                            */
                            //handleDataOutputFormats(ej, niMap, oo, odf, iriMap);
                        } else if (keyj.equals("inputs")) {
                            System.out.println("SOFTWARE IO: INPUTS");
                            handleSoftwareInputs(ej, niMap, oo, odf, iriMap);
                        } else if (keyj.equals("outputs")) {
                            System.out.println("SOFTWARE IO: OUTPUTS");
                            handleSoftwareOutputs(ej, niMap, oo, odf, iriMap);
                        } else if (keyj.equals("publicationsAbout") || keyj.equals("publicationsAboutRelease")) {
                            handlePublicationsAbout(ej, niMap, oo, odf, iriMap);
                        } else if (keyj.equals("identifier")) {
                            handleIdentifier(ej.getValue(), niMap, oo, odf, iriMap, subtype);
                            hasIdentifier = true;
                        } else if (keyj.equals("grants")) {
                        	handleGrants(ej.getValue(), niMap, oo, odf, iriMap, subtype);
                        } else if (keyj.equals("visualizationType")) {
                        	handleVisualizationType(ej.getValue(), niMap, oo, odf, iriMap, subtype);
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
                                        JsonElement valueElem = idElem.getAsJsonObject().get("identifierDescription");
                                        if (valueElem == null) continue;

                                        String value = valueElem.getAsString();

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
                                        System.err.println("NOTE: element " + keyj + " has array of values that are string.  Ignoring.");
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
                            if (!handled && !keyj.equals("subtype") && !attributesHandledInPostprocessing.contains(keyj)) { //} && !keyj.equals("identifier")) {
                                System.out.println("WARNING: assuming that handling of " + keyj + " attribute will occur in manual, post-processing step. values " + ej.getValue());
                                if (keyj.equals("publicationsAboutRelease")) {
                                    //System.out.println("PUB ABOUT: " + ej.getValue());

                                }
                            }
                        //}
                    } // end while (j.hasNext())

 

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

                    handleSimInds(niMap, oo, odf, iriMap);
                
                    if (!hasIdentifier) {
                    	identifierToOwlIndividual.put(baseName, niMap.get("dtm"));
                        System.out.println("BASE NAME IS: " + baseName);
                    	 if (subtype.contains("forecaster"))
                			forecasterIds.add(baseName);
                    	//System.out.println("hashing individual with baseName=" + baseName + ", other info is " +
                    	//	"baseLabel=" + baseLabel + ", fullName=" + fullName + ", versionSuffix=" + versionSuffix);
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

            /*
                Process manually curated disease forecaster attribute info, including
                    diseases
                    locationCoverage (used to be called 'region')
                    forecasts
                    forecastFrequency
            */
            processForecasterInfo();
            processPathogenEvolutionModelInfo();
            processPopulationDynamicsModelInfo();
            processDiseaseTransmissionTreeEstimatorInfo();
            
            try {
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
                String dateTxt = df.format(new Date());
                String owlFileName = "software-ontology-" + dateTxt + ".owl";
                fos = new FileOutputStream(owlFileName);
                oom.saveOntology(oo, fos);
            } catch (OWLOntologyStorageException oose) {
                oose.printStackTrace();
            }

                        /*
                This code helpfully prints out where the program left off with IRI 
                    generation, so that any other programs needing to pick up where
                    this one left off can do so.
            */
           	System.out.println(nextSimPopIri());
            System.out.println(nextSimPopIri());
            System.out.println(nextIri());
            System.out.println(nextIri());

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

            loadAndCreateGrantMapping();

            initializeForecasterInfo();

            //ioInfo = new SoftwareIoInfo();
            //ioInfo.init(p.getProperty("software_io_info"));

            if (attributesHandledInPostprocessing == null) {
                attributesHandledInPostprocessing = new HashSet<String>();
            }
             attributesHandledInPostprocessing.add("populationSpeciesIncluded");

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

    protected static void loadAndCreateGrantMapping() {
    	grantTxtToIndividual = new HashMap<String, OWLNamedIndividual>();
    	try {
    		FileReader fr = new FileReader(p.getProperty("grant_mapping"));
    		LineNumberReader lnr = new LineNumberReader(fr);
    		String line;
    		while ((line=lnr.readLine())!= null) {
    			String[] flds = line.split(Pattern.quote("\t"));
    			OWLNamedIndividual grantInd = odf.getOWLNamedIndividual(IRI.create(flds[1]));
    			OWLNamedIndividual grantResearchInd = createNamedIndividualWithTypeAndLabel(odf, oo,
    				iriMap.lookupClassIri("planned process"), iriMap.lookupAnnPropIri("editor preferred"),
    				"process of executing plan in grant " + flds[0]);
    			createOWLObjectPropertyAssertion(grantResearchInd, iriMap.lookupObjPropIri("achieves objective"), grantInd, odf, oo);
    			grantTxtToIndividual.put(flds[0], grantResearchInd);
    		}
    	} catch (IOException ioe) {
    		ioe.printStackTrace();
    	}
    }

    public static void initializeForecasterInfo() {
    	forecasterIdToName = new HashMap<String, String>();
    	forecasterIdToRegionCategories = new HashMap<String, ArrayList<String>>();
    	forecasterIdToYears = new HashMap<String, ArrayList<String>>();
        regionTypeToRegionIris = new HashMap<String, ArrayList<String>>();
        regionNameToHumanPopIri = new HashMap<String, IRI>();
        yearRegionToDzCourseAggregateIri = new HashMap<String, IRI>();

        identifierToOwlIndividual = new HashMap<String, OWLNamedIndividual>();
        forecasterIdToType = new HashMap<String, String>();
        forecasterIds = new HashSet<String>();
        forecasterIdToForecasts = new HashMap<String, String>();

    	try (FileReader fr1 = new FileReader(p.getProperty("forecaster_info"));
             FileReader fr2 = new FileReader(p.getProperty("forecaster_region"));
             FileReader fr3 = new FileReader(p.getProperty("forecaster_ecosystem"));
    		 ) {
    		
    		LineNumberReader lnr1 = new LineNumberReader(fr1);
    		String line;
    		while ((line=lnr1.readLine())!=null) {
    			String[] flds = line.split(Pattern.quote("\t"));
                /*
                    flds[0] = forecaster name
                    flds[1] = forecaseter id
                    flds[2] = years - pipe-delimited list
                    flds[3] = regions - pipe-delimited list
                    flds[4] = type
                    flds[5] = forecasters
                */
                forecasterIdToName.put(flds[1], flds[0]);
                System.out.println("FORECASTER " + flds[0] + ", " + flds[1]);
                String[] subflds1 = flds[2].split(Pattern.quote("|"));
                String[] subflds2 = flds[3].split(Pattern.quote("|"));

                System.out.println("NOTE: subflds1.length=" + subflds1.length);
                System.out.println("NOTE: subflds2.length=" + subflds2.length);

                ArrayList<String> regions = new ArrayList<String>();
                for (int i=0; i<subflds1.length; i++)
                    regions.add(subflds1[i]);

                forecasterIdToRegionCategories.put(flds[1], regions);
                
                ArrayList<String> years = new ArrayList<String>();
                for (int i=0; i<subflds2.length; i++)
                    years.add(subflds2[i]);

                forecasterIdToYears.put(flds[1], years);
                forecasterIdToType.put(flds[1], flds[4]);
                forecasterIdToForecasts.put(flds[1], flds[5]);
    		}

            LineNumberReader lnr2 = new LineNumberReader(fr2);
            while((line=lnr2.readLine())!=null) {
                String[] flds = line.split(Pattern.quote("\t"));
                /* 
                    flds[0] = region name
                    flds[1] = region type
                    flds[2] = IRI of human population in region
                */
                ArrayList<String> regions = null;
                if (!regionTypeToRegionIris.containsKey(flds[1])) {
                    regions = new ArrayList<String>();
                    regionTypeToRegionIris.put(flds[1], regions);
                } else {
                    regions = regionTypeToRegionIris.get(flds[1]);
                }
                regions.add(flds[0]);

                regionNameToHumanPopIri.put(flds[0], IRI.create(flds[2]));
                //System.out.println("hashed IRI of human population for region = " + flds[0]);
            }

            LineNumberReader lnr3 = new LineNumberReader(fr3);
            while((line=lnr3.readLine())!=null) {
                 String[] flds = line.split(Pattern.quote("\t"));
                /* 
                    flds[0] = year
                    flds[1] = region name
                    flds[2] = disease course aggregate IRI
                    flds[3] = human pop IRI (redundant from above so we'll ignore it)
                */
                String yearRegion = flds[0] + flds[1];
                yearRegionToDzCourseAggregateIri.put(yearRegion, IRI.create(flds[2]));
            }

            if (attributesHandledInPostprocessing == null) {
                attributesHandledInPostprocessing = new HashSet<String>();
            }
            attributesHandledInPostprocessing.add("forecasts");
            attributesHandledInPostprocessing.add("nowcasts");
            attributesHandledInPostprocessing.add("forecastFrequency");
            attributesHandledInPostprocessing.add("type");
            attributesHandledInPostprocessing.add("pathogens");

            System.out.println("Done initiatlizing forecaster info");

    	} catch (IOException ioe) {
    		ioe.printStackTrace();
    	} 
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
            addAnnotationToIndividual(oni, iriMap.lookupAnnPropIri("title"), value, odf, oo);
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
        System.out.println("Version individual: " + oni);
        // ...and add a label annotation to it with the value of the version attribute in the JSON
        addAnnotationToIndividual(oni, iriMap.lookupAnnPropIri("label"), versionSuffix, odf, oo);
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
            addAnnotationToIndividual(oni, iriMap.lookupAnnPropIri("hasURL"), value, odf, oo);
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
                    addAnnotationToIndividual(oni, iriMap.lookupAnnPropIri("label"), txt, odf, oo);
                    addAnnotationToIndividual(oni, iriMap.lookupAnnPropIri("hasURL"), url, odf, oo);
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
                    addAnnotationToIndividual(oni, iriMap.lookupAnnPropIri("label"), value, odf, oo);
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
            addAnnotationToIndividual(oni, iriMap.lookupAnnPropIri("label"), txt, odf, oo);
            addAnnotationToIndividual(oni, iriMap.lookupAnnPropIri("hasURL"), url, odf, oo);
        } else {
            System.err.println("DOI attribute has value that is not primitive.");
        }
    }

   /*
    	This code handles the identifier attribute.  
    */
    public static void handleIdentifier(JsonElement elem, HashMap<String, OWLNamedIndividual> niMap,
                                        OWLOntology oo, OWLDataFactory odf, IriLookup iriMap, String subtype) {
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

            String idText = value.replace("dx.",""), url = "";

        	if (idText.equals("https://zenodo.org/record/319937#.WKxb1xLytPV")) { idText = "10.5281/zenodo.319937"; }  //fixing BAD doi that Pitt assigned to Pitt SEIR model
        	if (idText.equals("https://zenodo.org/badge/latestdoi/80568018")) { idText = "10.5281/zenodo.268504"; } //bad DOI for FRED Windows
        	if (idText.equals("https://zenodo.org/record/439078#.WNvahxLytzq")) { idText = "10.5281/zenodo.439078"; }  //bad DOI for Pitt anthrax DTM
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
                url = "https://doi.org/" + idText;
            } else if (position > 0) {
            	identifierClassIri = iriMap.lookupClassIri("doi");
				if (!url.equals(idText)) { System.err.println("bad doi: " + idText + ", url = " + url); }
                url = idText;
                url = url.replace("http:", "https:");
                idText = idText.substring(position + 8);
                if (idText.startsWith("/")) idText = idText.replace("/", "").trim();
                System.out.println("New id text is: " + idText + ", url = " + url);
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
                addAnnotationToIndividual(oni, iriMap.lookupAnnPropIri("hasURL"), url, odf, oo);
            }

            /*
            	Assert that the identifier/DOI denotes the software/data service
            */
            OWLNamedIndividual doInd = niMap.get("dtm");
            createOWLObjectPropertyAssertion(oni, iriMap.lookupObjPropIri("denotes"), doInd, odf, oo);

            /* 
                Keep a map from identifier of digital object to the OWLNamedIndividual.

                We'll use this map later when we do post-processing, especially of forecasters.
            */
            identifierToOwlIndividual.put(idText, doInd);
            if (subtype.contains("forecaster"))
                forecasterIds.add(idText);
        }
    }

  /* 
  		This code handles the grants attribute in the JSON for the software/data service.
  */
  	public static void handleGrants(JsonElement elem, HashMap<String, OWLNamedIndividual> niMap,
                                        OWLOntology oo, OWLDataFactory odf, IriLookup iriMap, String subtype) {
  		/*
  			The value of the grants element is a JsonArray.  Each item in the array is a simple value.
  		*/
  		if (elem instanceof JsonArray) {
  			JsonArray ja = (JsonArray)elem;
  			Iterator<JsonElement> i = ja.iterator();

  			while (i.hasNext()) {
  				JsonElement elemi = i.next();
                String granti = ((JsonPrimitive) elemi).getAsString();

                /*
                	Get the named individual for string granti from the HashMap that holds the mapping.

                	This individual is the research process that realizes the plan in the grant proposal
                		text.  This process has as part the codewriting of the software, is our 
                		assumption.
                */
                OWLNamedIndividual grantResearchInd = grantTxtToIndividual.get(granti);
                OWLNamedIndividual codeWritingInd = niMap.get("codewriting");
                createOWLObjectPropertyAssertion(codeWritingInd, iriMap.lookupObjPropIri("is part of"), grantResearchInd, odf, oo);
  			}
  		}

  	}

  /* 
  		This code handles the visuzalizationType attribute in the JSON for software/data service.

  		Basically, we just need to get an individual representing the process of the executable
  			running, and state that it has input some data set of the kind visualized, and has
  			output some diagram
  */
  	public static void handleVisualizationType(JsonElement elem, HashMap<String, OWLNamedIndividual> niMap,
                                        OWLOntology oo, OWLDataFactory odf, IriLookup iriMap, String subtype) {
  		// The value of the visualizationType attribute is a JsonArray.
  		if (elem instanceof JsonArray) {
			JsonArray ja = (JsonArray)elem;
  			Iterator<JsonElement> i = ja.iterator();

  			while (i.hasNext()) {
  				JsonElement elemi = i.next();
                String visualizationType = ((JsonPrimitive) elemi).getAsString();

                /*
                	Get the executing of the software individual
                */
                OWLNamedIndividual executionInd = niMap.get("executionof");
                /*
                	Create the time series data set individual.  The executing has specified input the dataset.
                */
                OWLNamedIndividual tsDataSet = createNamedIndividualWithTypeAndLabel(iriMap.lookupClassIri("time series data set"),
                	iriMap.lookupAnnPropIri("editor preferred"), "time series data set input into " + fullName);
                addAnnotationToIndividual(tsDataSet, iriMap.lookupAnnPropIri("label"), visualizationType, odf, oo);
				createOWLObjectPropertyAssertion(executionInd, iriMap.lookupObjPropIri("has specified input"), tsDataSet, odf, oo);

                /*
                	Create the diagram individual. The executing has specified output the diagram.
                */
                OWLNamedIndividual diagram = createNamedIndividualWithTypeAndLabel(iriMap.lookupClassIri("diagram"),
                	iriMap.lookupAnnPropIri("editor preferred"), "diagram output by " + fullName);
                createOWLObjectPropertyAssertion(executionInd, iriMap.lookupObjPropIri("has specified output"), diagram, odf, oo);
  			}  			

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
            	String url = null;
            	if (html.contains("a href")){
                	Document d = Jsoup.parse(html);
                	Elements links = d.select("a");
                	url = links.get(0).attr("href");
                	//String txt = links.get(0).ownText();  // not doing anything with this text, so...
                } else {
                	url = html;
                }
                addAnnotationToIndividual(oni, iriMap.lookupAnnPropIri("hasURL"), url, odf, oo);
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
            addAnnotationToIndividual(oni, iriMap.lookupAnnPropIri("synopsis"), value, odf, oo);
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
                addAnnotationToIndividual(execInd, iriMap.lookupAnnPropIri("hasURL"), url, odf, oo);
                addAnnotationToIndividual(execInd, iriMap.lookupAnnPropIri("label"), txt, odf, oo);
            } else {
                //otherwise, if the executable value is not html, just put whatever is in there on as a URL.
                if (execs[0].startsWith("http")) {
                    addAnnotationToIndividual(execInd, iriMap.lookupAnnPropIri("hasURL"), execs[0], odf, oo);
                } else {
                    addAnnotationToIndividual(execInd, iriMap.lookupAnnPropIri("label"), execs[0], odf, oo);
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
                        addAnnotationToIndividual(execi, iriMap.lookupAnnPropIri("hasURL"), url, odf, oo);
                        addAnnotationToIndividual(execi, iriMap.lookupAnnPropIri("editor preferred"), newExecIndPrefTerm, odf, oo);

                        execNis.add(execi);
                        compNis.add(compi);
                    } else {
                        String newExecIndPrefTerm = execIndPrefTerm + " " + execs[i];
                        String newCompIndPrefTerm = "compiling to " + newExecIndPrefTerm;
                        OWLNamedIndividual execi = createNamedIndividualWithTypeAndLabel(odf, oo, iriMap.lookupClassIri("executable"), iriMap.lookupAnnPropIri("editor preferred"), newExecIndPrefTerm);
                        OWLNamedIndividual compi = createNamedIndividualWithTypeAndLabel(odf, oo, iriMap.lookupClassIri("compiling"), iriMap.lookupAnnPropIri("editor preferred"), newCompIndPrefTerm);

                        if (execs[i].startsWith("http")) {
                            addAnnotationToIndividual(execi, iriMap.lookupAnnPropIri("hasURL"), execs[i].trim(), odf, oo);
                        } else {
                            addAnnotationToIndividual(execi, iriMap.lookupAnnPropIri("label"), execs[i].trim(), odf, oo);
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
                addAnnotationToIndividual(oni, iriMap.lookupAnnPropIri("hasURL"), value, odf, oo);
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
            addAnnotationToIndividual(oni, iriMap.lookupAnnPropIri("hasURL"), value, odf, oo);

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
                addAnnotationToIndividual(oni, iriMap.lookupAnnPropIri("hasURL"), url, odf, oo);
                addAnnotationToIndividual(oni, iriMap.lookupAnnPropIri("label"), txt, odf, oo);
            } else {
                //otherwise, if the documentation value is not an <a href=... construct then if it starts with http:/// add as URL otherwise, add as label
                if (value.startsWith("http:"))
                    addAnnotationToIndividual(oni, iriMap.lookupAnnPropIri("hasURL"), value, odf, oo);
                else
                    addAnnotationToIndividual(oni, iriMap.lookupAnnPropIri("label"), value, odf, oo);
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
                        addAnnotationToIndividual(oni, iriMap.lookupAnnPropIri("hasURL"), url, odf, oo);
                        addAnnotationToIndividual(oni, iriMap.lookupAnnPropIri("label"), txt, odf, oo);
                    } else {
                        //otherwise, if the documentation value is not html, just put whatever is in there on as a URL.
                        addAnnotationToIndividual(oni, iriMap.lookupAnnPropIri("hasURL"), value, odf, oo);
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
                    addAnnotationToIndividual(devi, iriMap.lookupAnnPropIri("editor preferred"), devNames[i] + ", developer of " + fullName, odf, oo);
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
                    addAnnotationToIndividual(devi, iriMap.lookupAnnPropIri("editor preferred"), devNames[i] + ", developer of " + fullName, odf, oo);
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

            addAnnotationToIndividual(execInd, iriMap.lookupAnnPropIri("label"), "execution of dtm for study described in " + pubInfo.get(0), odf, oo);
            addAnnotationToIndividual(studyInd, iriMap.lookupAnnPropIri("label"), "study process described in " + pubInfo.get(0), odf, oo);

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
		if (value.equals("true")) {
        	String uidsConcIndPrefTerm = "Concretization of " + fullName + " available through, but not as part of, UIDS";
        	addAnnotationToIndividual(uidsConcInd, iriMap.lookupAnnPropIri("editor preferred"), uidsConcIndPrefTerm, odf, oo);
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
        } else {
        	System.out.println("UIDS non true value: " + value);
        }
    }

    public static void handleControlMeasures(Map.Entry<String, JsonElement> e, HashMap<String, OWLNamedIndividual> niMap,
                                             OWLOntology oo, OWLDataFactory odf, IriLookup iriMap) {
    	/*
    		Control measures are now an array of objects, not strings.
    	*/
        JsonElement je = e.getValue();
        if (je instanceof JsonArray) {
            JsonArray elemArray = (JsonArray) je;
            Iterator<JsonElement> elemIter = elemArray.iterator();
            int size = elemArray.size();
            while (elemIter.hasNext()) {
                JsonElement elemi = elemIter.next();
                JsonObject jo = (JsonObject)elemi;
                String value = jo.getAsJsonObject("identifier").getAsJsonPrimitive("identifierDescription").getAsString();
                
                uniqueCms.add(value);

                if (cmMap.containsKey(value)) {
                    IRI classIri = cmMap.get(value);
                    String cmInstanceLabel = value + " control measure by " + fullName;
                    String simxInstanceLabel  = "simulating of epidemic with " + value + " control measure by " + fullName;
                    OWLNamedIndividual simxInd = createNamedIndividualWithTypeAndLabel(odf, oo, iriMap.lookupClassIri("simulatingx"), iriMap.lookupAnnPropIri("editor preferred"), simxInstanceLabel);
                    OWLNamedIndividual cmInd = createNamedIndividualWithTypeAndLabel(odf, oo, classIri, iriMap.lookupAnnPropIri("editor preferred"), cmInstanceLabel);
                    addAnnotationToIndividual(cmInd, iriMap.lookupAnnPropIri("label"), value, odf, oo);

                    createOWLObjectPropertyAssertion(simxInd, iriMap.lookupObjPropIri("achieves objective"), niMap.get("executable"), odf, oo);
                    createOWLObjectPropertyAssertion(simxInd, iriMap.lookupObjPropIri("has specified output"), cmInd, odf, oo);
                } else {
                    System.err.println("Skipping " + value + " control measure.");
                }
            }
        } else {
            throw new IllegalArgumentException("value of controlMeasures attribute must be array");
        }
    }

    /*
    public static void handleDataInputFormats(Map.Entry<String, JsonElement> e, HashMap<String, OWLNamedIndividual> niMap,
                                              OWLOntology oo, OWLDataFactory odf, IriLookup iriMap) {
    	
    	ArrayList<String> inputs = null;
    	if (ioInfo.getInputListForLabel(fullName) != null) {
    		inputs = ioInfo.getInputListForLabel(fullName);
    		System.out.println("Bypassing JSON data for curated software input data for: " + fullName + ", with " + inputs.size() + " inputs.");
    	} else {
    		System.out.println("No curated software input data for: " + fullName);
    	
    		inputs = new ArrayList<String>();
    		String values = "";
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
                        	//String url = links.get(0).attr("href");
                        	value = links.get(0).ownText();
                    	}
                    	values += value;
                    	if (elemIter.hasNext()) values += ";";   //we're treating everything in the list as an alternative format for one input
                    	uniqueInputFormats.add(value);
                    } else {
                    	System.err.println("Value of data input format attribute is not primitive.");
                    }
                }
                inputs.add(values);
            }
        }

        //for each input we create a plan specification and relate it to the various data format specs that are options
        int iInput = 1;
        int cInput = inputs.size();
        Iterator<String> inputIter = inputs.iterator();
        while (inputIter.hasNext()) {
        	//create the overall "data input plan specification"
        	String planSpecPrefTerm = "data input #" + iInput + " of " + cInput + " for executable of " + fullName;
        	OWLNamedIndividual planSpecInd = createNamedIndividualWithTypeAndLabel(odf, oo, iriMap.lookupClassIri("dataInputPlanSpecification"),
        			iriMap.lookupAnnPropIri("editor preferred"), planSpecPrefTerm);
        	//connect plan specification to executable.  The plan specification is part of the executable.
            createOWLObjectPropertyAssertion(planSpecInd, iriMap.lookupObjPropIri("is part of"), niMap.get("executable"), odf, oo);

        	String inputFormatsConcat = inputIter.next();
        	String[] inputFormats = inputFormatsConcat.split(Pattern.quote(";"));
        	
        	//for each format in the list for the current input, associate the input format with the input plan specification
        	//  via a data parsing according to the input format specification.
        	for (String inputFormat : inputFormats) {
            	OWLNamedIndividual formatInd = formatInds.get(inputFormat);
            	if (formatInd == null) {
            		System.err.println("UNRECOGNIZED DATA INPUT FORMAT: " + inputFormat);

            		/*
            			Go ahead and create a non-MDC-registered data format.  We just have to be careful when
            				writing SPARQL queries that we do not count this format to the FAIR-o-meter metrics,
            				both in terms of data formats in MDC, and in terms of the number of inputs with a data
            				format registered in MDC. 
         			//
            		String formatSpecLabel = inputFormat + ", a data format not cataloged in MDC, but that is used as " +
            				"an input format to " + fullName + ", which is a software that is cataloged in MDC.";
            		formatInd = createNamedIndividualWithTypeAndLabel(odf, oo, iriMap.lookupClassIri("dataformat"),
            				iriMap.lookupAnnPropIri("editor preferred"), formatSpecLabel);   

            		formatInds.put(inputFormat, formatInd);  //save just in case we encounter it again.             	
            	}

            	addAnnotationToIndividual(formatInd, iriMap.lookupAnnPropIri("comment"), 
            			"data input format for " + fullName, odf, oo);

            	String dataParsingLabel = "data parsing of file in " + inputFormat + " format by " + fullName;
				OWLNamedIndividual dataParsingInd = createNamedIndividualWithTypeAndLabel(odf, oo, iriMap.lookupClassIri("dataparsing"),
                    	    iriMap.lookupAnnPropIri("editor preferred"), dataParsingLabel);

            	//connect parsing to format.  Parsing realizes data input format specification
                createOWLObjectPropertyAssertion(dataParsingInd, iriMap.lookupObjPropIri("achieves objective"), formatInd, odf, oo);

            	//connect parsing to plan specification.  Parsing realizes data input specification.
                createOWLObjectPropertyAssertion(dataParsingInd, iriMap.lookupObjPropIri("achieves objective"), planSpecInd, odf, oo);
            }
            iInput++;
        }
    }
    */

    /*
    public static void handleDataOutputFormats(Map.Entry<String, JsonElement> e, HashMap<String, OWLNamedIndividual> niMap,
                                               OWLOntology oo, OWLDataFactory odf, IriLookup iriMap) {
    	ArrayList<String> outputs = null;
    	if (ioInfo.getOutputListForLabel(fullName) != null) {
    		outputs = ioInfo.getOutputListForLabel(fullName);
    		System.out.println("Bypassing JSON data for curated software output data for: " + fullName + ", with " + outputs.size() + " outputs.");
    	} else {
    		outputs = new ArrayList<String>();
	        JsonElement je = e.getValue();
	        if (je instanceof JsonArray) {
	            JsonArray elemArray = (JsonArray) je;
	            Iterator<JsonElement> elemIter = elemArray.iterator();
	            int size = elemArray.size();
	            String values = "";
	            while (elemIter.hasNext()) {
	                JsonElement elemi = elemIter.next();
	                if (elemi instanceof JsonPrimitive) {
	                    String value = ((JsonPrimitive) elemi).getAsString();
	                    uniqueOutputFormats.add(value);
	                    if (!values.trim().toLowerCase().equals("n/a")) {
	                    	values += value;
	                    	if (elemIter.hasNext()) values += ";";
	                    } else {
	                    	System.err.println("IGNORING BAD DATA OUTPUT FORMAT VALUE: " + value);
	                    }
	        		} else { //end if instanceof JsonPrimitive
	        			System.err.println("Value of dataOutputFormats attribute is not primitive: " + elemi);
	        		}
	   			} //end while
	   			outputs.add(values);
	    	} //end if instanceof JsonArraty
	    } //end else

	    int iOutput = 1;
	    int cOutput = outputs.size();
	    Iterator<String> outputIter = outputs.iterator();
        while (outputIter.hasNext()) {
			//create the overall "data output plan specification"
        	String planSpecPrefTerm = "data output #" + iOutput + " of " + cOutput + " for executable of " + fullName;
        	OWLNamedIndividual planSpecInd = createNamedIndividualWithTypeAndLabel(odf, oo, iriMap.lookupClassIri("dataOutputPlanSpecification"),
        			iriMap.lookupAnnPropIri("editor preferred"), planSpecPrefTerm);
        	//connect plan specification to executable.  The plan specification is part of the executable.
            createOWLObjectPropertyAssertion(planSpecInd, iriMap.lookupObjPropIri("is part of"), niMap.get("executable"), odf, oo);

        	String outputFormatsConcat = outputIter.next();
        	String[] outputFormats = outputFormatsConcat.split(Pattern.quote(";"));
        	
        	//for each format in the list for the current input, associate the input format with the input plan specification
        	//  via a data encoding according to the input format specification.
        	for (String outputFormat : outputFormats) {
            	OWLNamedIndividual formatInd = formatInds.get(outputFormat);
            	System.out.println("\nPROCESSING OUTPUT FORMAT: " + outputFormat);
            	if (formatInd == null) {
            		System.err.println("UNRECOGNIZED DATA OUTPUT FORMAT: " + outputFormat);

            		/*
            			Go ahead and create a non-MDC-registered data format.  We just have to be careful when
            				writing SPARQL queries that we do not count this format to the FAIR-o-meter metrics,
            				both in terms of data formats in MDC, and in terms of the number of outputs with a data
            				format registered in MDC. 
         			
            		String formatSpecLabel = outputFormat + ", a data format not cataloged in MDC, but that is used as " +
            				"an output format by " + fullName + ", which is a software that is cataloged in MDC.";
            		formatInd = createNamedIndividualWithTypeAndLabel(odf, oo, iriMap.lookupClassIri("dataformat"),
            				iriMap.lookupAnnPropIri("editor preferred"), formatSpecLabel);   

            		formatInds.put(outputFormat, formatInd);  //save just in case we encounter it again.             	
            	}

            	addAnnotationToIndividual(formatInd, iriMap.lookupAnnPropIri("comment"), 
            			"data output format for " + fullName, odf, oo);

            	String dataEncodingLabel = "data encoding of file in " + outputFormat + " format by " + fullName;
				OWLNamedIndividual dataEncodingInd = createNamedIndividualWithTypeAndLabel(odf, oo, iriMap.lookupClassIri("dataencoding"),
                    	    iriMap.lookupAnnPropIri("editor preferred"), dataEncodingLabel);

            	//connect encoding to format.  Encoding realizes data output format specification
                createOWLObjectPropertyAssertion(dataEncodingInd, iriMap.lookupObjPropIri("achieves objective"), formatInd, odf, oo);

            	//connect encoding to plan specification.  Encoding realizes data output specification.
                createOWLObjectPropertyAssertion(dataEncodingInd, iriMap.lookupObjPropIri("achieves objective"), planSpecInd, odf, oo);
            }
            iOutput++;
        }
    }
    */

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
        //addAnnotationToIndividual(oni, iriMap.lookupAnnPropIri("label"), "Olympus", odf, oo);

        OWLNamedIndividual olympusWebsite = odf.getOWLNamedIndividual(nextIri());
        OWLClassAssertionAxiom ocaa = odf.getOWLClassAssertionAxiom(odf.getOWLClass(iriMap.lookupClassIri("website")), olympusWebsite);
        oom.addAxiom(oo, ocaa);
        addAnnotationToIndividual(olympusWebsite, iriMap.lookupAnnPropIri("editor preferred"), "Olympus home page", odf, oo);
        addAnnotationToIndividual(olympusWebsite, iriMap.lookupAnnPropIri("title"), "Olympus", odf, oo);
        addAnnotationToIndividual(olympusWebsite, iriMap.lookupAnnPropIri("hasURL"), "https://www.psc.edu/resources/computing/olympus", odf, oo);
        addAnnotationToIndividual(olympusWebsite, iriMap.lookupAnnPropIri("authors"), "Pittsburgh Supercomputing Center", odf, oo);
        addAnnotationToIndividual(olympusWebsite, iriMap.lookupAnnPropIri("published date"), "2016-03-02", odf, oo);

        createOWLObjectPropertyAssertion(olympusWebsite, iriMap.lookupObjPropIri("is about"), oni, odf, oo);
        return oni;
    }

    public static OWLNamedIndividual createUidsIndividuals(OWLDataFactory odf, OWLOntology oo, IriLookup iriMap) {
        OWLNamedIndividual oni = odf.getOWLNamedIndividual(nextIri());
        OWLClassAssertionAxiom ocaaTemp = odf.getOWLClassAssertionAxiom(odf.getOWLClass(iriMap.lookupClassIri("software")), oni);
        oom.addAxiom(oo, ocaaTemp);
        addAnnotationToIndividual(oni, iriMap.lookupAnnPropIri("label"), "Universal Interface to Disease Simulators (UIDS) version 4.0.1", odf, oo);
        addAnnotationToIndividual(oni, iriMap.lookupAnnPropIri("editor preferred"), "The Apollo Universal Interface to Disease Simulators (UIDS) version 4.0.1", odf, oo);
        addAnnotationToIndividual(oni, iriMap.lookupAnnPropIri("hasURL"), "https://github.com/ApolloDev/simple-end-user-apollo-web-application/tree/38161eba742a9000bb610aa47419f7fb62f0c3ac", odf, oo);
        addAnnotationToIndividual(oni, iriMap.lookupAnnPropIri("synopsis"), "The UIDS (Universal Interface to Disease Simulators) is a web application that assists a user in the construction and simulation of infectious disease scenarios (scenarios).  The UIDS xml-encodes scenarios according to the Apollo-XSD InfectiousDiseaseScenario type.  A user can construct a scenario de novo or download an existing scenario from the Apollo Library.   UIDS can transmit a scenario to a disease transmission simulator by calling the Apollo Broker service <insert URL>.  UIDS reports on the status of a requested run of a disease transmission simulator and displays maps and time series graphs of simulator output.  UIDS can also save a scenario to the Apollo Library <insert URL>.", odf, oo);

        OWLNamedIndividual uidsWebsite = odf.getOWLNamedIndividual(nextIri());
        OWLClassAssertionAxiom ocaa = odf.getOWLClassAssertionAxiom(odf.getOWLClass(iriMap.lookupClassIri("website")), uidsWebsite);
        oom.addAxiom(oo, ocaa);
        addAnnotationToIndividual(uidsWebsite, iriMap.lookupAnnPropIri("editor preferred"), "UIDS home page", odf, oo);
        addAnnotationToIndividual(uidsWebsite, iriMap.lookupAnnPropIri("title"), "UIDS home", odf, oo);
        addAnnotationToIndividual(uidsWebsite, iriMap.lookupAnnPropIri("hasURL"), "https://research.rods.pitt.edu/apollo-web-client/index.php", odf, oo);
        addAnnotationToIndividual(uidsWebsite, iriMap.lookupAnnPropIri("hasURL"), "https://research.rods.pitt.edu/apollo-web-client/index.php", odf, oo);
        createOWLObjectPropertyAssertion(uidsWebsite, iriMap.lookupObjPropIri("is about"), oni, odf, oo);

        uidsCompiling = createNamedIndividualWithTypeAndLabel(odf, oo, iriMap.lookupClassIri("compiling"), iriMap.lookupAnnPropIri("editor preferred"), "compiling of UIDS source to UIDS executable");
        addAnnotationToIndividual(uidsCompiling, iriMap.lookupAnnPropIri("label"), "compiling UIDS", odf, oo);

        uidsExecutable = createNamedIndividualWithTypeAndLabel(odf, oo, iriMap.lookupClassIri("executable"), iriMap.lookupAnnPropIri("editor preferred"), "UIDS executable code");
        addAnnotationToIndividual(uidsExecutable, iriMap.lookupAnnPropIri("label"), "UIDS executable", odf, oo);

        uidsExecutableConcretization = createNamedIndividualWithTypeAndLabel(odf, oo, iriMap.lookupClassIri("executableConcretization"), iriMap.lookupAnnPropIri("editor preferred"), "concretization of UIDS executable");
        addAnnotationToIndividual(uidsExecutableConcretization, iriMap.lookupAnnPropIri("label"), "UIDS executable concretization", odf, oo);

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
        addAnnotationToIndividual(oni, labelPropIri, rdfsLabel, odf, oo);
        return oni;
    }

       /*
    	This method is a facade in front of the OWLAPI to simplify creating a new individual with its
    		first text-based (i.e. literal) annotation.  The IRI assigned to the individual is 
    		created by calling the nextIri() method.
    */
    public static OWLNamedIndividual createNamedIndividualWithTypeAndLabel(IRI classTypeIri, IRI labelPropIri, String rdfsLabel) {
        return createNamedIndividualWithIriTypeAndLabel(nextIri(), odf, oo, classTypeIri, labelPropIri, rdfsLabel);
    }


    /*
    	This method is a facade in front of the OWLAPI to simplify adding an annotation to an individual.
    */
    public static void addAnnotationToIndividual(OWLIndividual oi, IRI annPropIri, String value, OWLDataFactory odf, OWLOntology oo) {
        OWLLiteral li = odf.getOWLLiteral(value);
        OWLAnnotationProperty la = odf.getOWLAnnotationProperty(annPropIri);
        OWLAnnotation oa = odf.getOWLAnnotation(la, li);
        OWLAnnotationAssertionAxiom oaaa = null;
        //System.out.println(annPropIri + "     =    " + value + ",      to be placed on " + oi);
        if (oi instanceof OWLNamedIndividual) {
        	OWLNamedIndividual oni = (OWLNamedIndividual)oi;
        	oaaa = odf.getOWLAnnotationAssertionAxiom(oni.getIRI(), oa);
        } else if (oi instanceof OWLAnonymousIndividual)
        	oaaa = odf.getOWLAnnotationAssertionAxiom((OWLAnonymousIndividual)oi, oa);
        oom.addAxiom(oo, oaaa);
    }

       /*
    	This method is a facade in front of the OWLAPI to simplify creating a new anonymous individual with its
    		first text-based (i.e. literal) annotation.
    */
    public static OWLAnonymousIndividual createAnonymousIndividualWithTypeAndLabel(IRI classTypeIri, 
    		IRI labelPropIri, String rdfsLabel) {
        OWLAnonymousIndividual oai = odf.getOWLAnonymousIndividual();
        OWLClassAssertionAxiom ocaa = odf.getOWLClassAssertionAxiom(odf.getOWLClass(classTypeIri), oai);
        oom.addAxiom(oo, ocaa);
        addAnnotationToIndividual(oai, labelPropIri, rdfsLabel, odf, oo);
        return oai;
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
        if (simIndSetsTxt == null) {
            System.err.println("No indexing information for disease transmission model with fullName=" + fullName);
            return;
        }
        String[] simIndSets = simIndSetsTxt.split(Pattern.quote("|"));
        System.err.println("\tsimIndSets size is " + simIndSets.length+ " for software " + fullName);
        for (String indSet : simIndSets) {
            String[] simInds = indSet.split(Pattern.quote(","));
            System.err.println("\t\tsimInds size is " + simInds.length);
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
    public static void createOWLObjectPropertyAssertion(OWLIndividual source, IRI objPropIri, OWLIndividual target, OWLDataFactory odf, OWLOntology oo) {
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

    protected static void processForecasterInfo() {
        Iterator<String> i = forecasterIds.iterator();
        System.out.println("BEGIN PROCESSING FORECASTERS...");
        while (i.hasNext()) {
            String forecasterId = i.next();
            System.out.println("\tProcessing forecaster: " + forecasterId);

            /*
                Get the forecaster with forecasterID
            */
            OWLNamedIndividual forecasterInd = identifierToOwlIndividual.get(forecasterId);

            String type = forecasterIdToType.get(forecasterId);
            if (type != null) {
                OWLNamedIndividual objective = createNamedIndividualWithTypeAndLabel(iriMap.lookupClassIri(type), 
                    iriMap.lookupAnnPropIri("editor preferred"), type + " objective of forecaster with ID = " + forecasterId);
                createOWLObjectPropertyAssertion(objective, iriMap.lookupObjPropIri("is part of"), forecasterInd, odf, oo);
                System.out.println("\t\tCreated " + type + " objective for forecaster " + forecasterId);
            }

            if (forecasterIdToRegionCategories.containsKey(forecasterId)) {
                /*
                	Get the executable individual if there is one...but at this point in the 
                    	processing, we don't have access to it anymore.  However, because none of the
                    	forecasters have an executable attribute in the JSON that represents them,
                    	no worries, just create it.  If someone ever adds an executable, we'd have to
                    	deal with it then.

                    	We need it to connect it to an "execution" (or running) of it.  The execution has
                    		as output a dataset (per below) that is about the aggregate of disease courses.
                    		But we'll have a different execution for each dataset and a different one for 
                    		the simulated population as well.
				*/
                OWLNamedIndividual executable = createNamedIndividualWithTypeAndLabel(iriMap.lookupClassIri("executable"),
                        iriMap.lookupAnnPropIri("editor preferred"), "executable for disease forecaster with ID = " + forecasterId);
                OWLNamedIndividual compiling = createNamedIndividualWithTypeAndLabel(iriMap.lookupClassIri("compiling"),
                        iriMap.lookupAnnPropIri("editor preferred"), "compiling of executable for disease forecaster with ID = " 
                        + forecasterId);

                /*
                	The compiling has the forecaster as specified input. has specified input
                */
                createOWLObjectPropertyAssertion(compiling, iriMap.lookupObjPropIri("has specified input"), forecasterInd, odf, oo);

                /* 
                	The compiling has the executable as specified output. has specified output
                */
                createOWLObjectPropertyAssertion(compiling, iriMap.lookupObjPropIri("has specified output"), executable, odf, oo);

                /*
                	Create simulating individual individual and connect it to executable: simulating achieves 
                		planned objective of executable.  We're saying one simulating process generates all the 
                		region-specific simulated populations below.
                */
				OWLNamedIndividual simulating = createNamedIndividualWithTypeAndLabel(iriMap.lookupClassIri("simulatingx"),
                        iriMap.lookupAnnPropIri("editor preferred"), "simulating of disease course aggregate by disease forecaster with ID = " + forecasterId);
  				createOWLObjectPropertyAssertion(simulating, iriMap.lookupObjPropIri("achieves objective"), executable, odf, oo);
  				
  				handleForecasts(executable, forecasterId);
  				handleForecastFrequency(forecasterId);

                /*
                	Get the set of categories geographical regions for which this (ILI) forecaster can forecast.  The 
                		categories are "state" (it can do all 50 states), "country" (it can do a list of countries), 
                		and "hrsa" (it can do all of the 10 US HRSA regions individually).
                */
                Iterator<String> regionCats = forecasterIdToRegionCategories.get(forecasterId).iterator();

                //For each such region
                while(regionCats.hasNext()) {
                	String regionCat = regionCats.next();
                	System.out.println("\t\tProcessing region category: " + regionCat);
                    // For each category of region, we have a list of individual regions in that category.
                    Iterator<String> regions = regionTypeToRegionIris.get(regionCat).iterator();
                    // For each individual geographical region for which the forecaster can forecast...
                    while (regions.hasNext()) {
                        String region = regions.next();
                        System.out.println("\t\t\tProcessing region: " + region);
                        /*
                			Create simulation of population individual and connect to simulating: simulating has specified
                				output simulation of population
                		*/
                		OWLNamedIndividual simpop = createNamedIndividualWithTypeAndLabel(iriMap.lookupClassIri("simulatedhostpopulation"),
                			iriMap.lookupAnnPropIri("editor preferred"), "simulation of population of humans in " +
                				region + " by the disease forecaster with ID=" + forecasterId);
                		createOWLObjectPropertyAssertion(simulating, iriMap.lookupObjPropIri("has specified output"), simpop, odf, oo);

                        /*
                			Get IRI of human population for this region
                		*/
                		IRI popIri = regionNameToHumanPopIri.get(region);
                		OWLNamedIndividual popInd = odf.getOWLNamedIndividual(popIri);

                		/*
                			And say simulation "simulates" population.  This connects the forecaster to the geographical
                				region that it can simulate, via the relationships between the population and the region. 
                		*/
                		createOWLObjectPropertyAssertion(simpop, iriMap.lookupObjPropIri("simulates"), popInd, odf, oo);

                        // Get the list of years for which the forecaster can forecast.
                        Iterator<String> years = forecasterIdToYears.get(forecasterId).iterator();
                        while (years.hasNext()) {
                        	// For each year (for each region)
                            String year = years.next();
                            System.out.println("\t\t\t\tProcessing year: " + year);
                            // Info on IRIs of disease course aggregates is indexed by combo key
                            String key = year + region;
                            /* 
                                For the given year and region combination, associate the forecaster with the aggregate of 
                                    disease courses that occurred that year.  Note that here, we are dealing with only
                                    influenza forecasters.
                            */
                            IRI dzCourseAggregateIri = yearRegionToDzCourseAggregateIri.get(key);
                            OWLNamedIndividual dzCourseAggregateInd = odf.getOWLNamedIndividual(dzCourseAggregateIri);

                            /*
                                We need to create a dataset for the region/year combo, but only once.  So check the hash
                                    for it.  If it doesn't exist, then create it and hash it.

                                    Also, think carefully about making rdf:type a dataset.  That could cause issues.

                                    Perhaps creating it as an anonymous individual will help.  We might have to alter our
                                     SPARQL queries to only retrieve datasets represented by named individuals, which
                                     would be relatively straightforward - should only require adding a single clause.
                            */
                            OWLNamedIndividual dataset = createNamedIndividualWithTypeAndLabel(iriMap.lookupClassIri("dataabouthost"), 
                                    iriMap.lookupAnnPropIri("editor preferred"), 
                                    "data set that is about an aggregate of disease courses, where the disease is an acute respiratory " +
                                        "illness, and the aggregate occurs in " + region + " in outbreak season beginning in " + year);

                			/*
                   				 Ditto the execution process of the executable. Also, the execution achieves the planned objective
                   				  of the executable, and has specified output the dataset.
               				*/
               				OWLNamedIndividual execution = createNamedIndividualWithTypeAndLabel(iriMap.lookupClassIri("executionof"),
                       			iriMap.lookupAnnPropIri("editor preferred"), "execution process for disease forecaster with ID = " + forecasterId +
                       				" that created dataset for " + year + " in " + region);
               				createOWLObjectPropertyAssertion(execution, iriMap.lookupObjPropIri("achieves objective"),
               						executable, odf, oo);
               				createOWLObjectPropertyAssertion(execution, iriMap.lookupObjPropIri("has specified output"),
               						dataset, odf, oo);

               				/*
               					Lastly, the connection: the dataset is about the disease course aggregate.  This last step
               						means we have connected the forecaster (via compiling->executable->execution->dataset
               						->dz course aggregate) to the disease (the dz course aggregate subsequently has a 
               						participant who is the bearer of a disease that is rdf:type acute respiratory illness).
               				*/
							createOWLObjectPropertyAssertion(dataset, iriMap.lookupObjPropIri("is about"), dzCourseAggregateInd, odf, oo);
               				
                        } /*
                                end while(years.hasNext())
                           */
                    } /*
                            end while(regions.hasNext())
                      */
                } /* 
                        end while(regionCats.hasNext())

                        Here, we're iterating over region categories (country, hrsa, state)
                  */
            } else if (forecasterId.equals("ZIKA Modeling")) { //end if
                /*
                    We need an executable, a simulating, and a connection, which also requires a compiling, etc.
                */
                OWLNamedIndividual executable = createNamedIndividualWithTypeAndLabel(iriMap.lookupClassIri("executable"),
                        iriMap.lookupAnnPropIri("editor preferred"), "executable for disease forecaster with ID = " + forecasterId);
                OWLNamedIndividual compiling = createNamedIndividualWithTypeAndLabel(iriMap.lookupClassIri("compiling"),
                        iriMap.lookupAnnPropIri("editor preferred"), "compiling of executable for disease forecaster with ID = " 
                        + forecasterId);
                OWLNamedIndividual execution = createNamedIndividualWithTypeAndLabel(iriMap.lookupClassIri("executionof"),
                        iriMap.lookupAnnPropIri("editor preferred"), "execution process for disease forecaster with ID = " + forecasterId);
                OWLNamedIndividual forecast = createNamedIndividualWithTypeAndLabel(iriMap.lookupClassIri("case count"),
                		iriMap.lookupAnnPropIri("editor preferred"), "Number of Zika-attributable microcephaly cases");

                /*
                    The compiling has the forecaster as specified input. has specified input
                */
                createOWLObjectPropertyAssertion(compiling, iriMap.lookupObjPropIri("has specified input"), forecasterInd, odf, oo);

                /* 
                    The compiling has the executable as specified output. has specified output
                */
                createOWLObjectPropertyAssertion(compiling, iriMap.lookupObjPropIri("has specified output"), executable, odf, oo);

                /*
                	The execution achieves the objective of the executable
               	*/
				createOWLObjectPropertyAssertion(execution, iriMap.lookupObjPropIri("achieves objective"),
                               executable, odf, oo);
                
				/*
					And the execution has specified output the forecast
				*/
				createOWLObjectPropertyAssertion(execution, iriMap.lookupObjPropIri("has specified output"),
								forecast, odf, oo);

                /*
                    Now, for each Zika region, we create four simulated populations, each one is the output of the simulating.

                    Then we connect the simulated populations to the populations via the IRIs we pre-loaded.
                */
                try (FileReader zikaPops = new FileReader((String)p.get("forecaster_zika_pops")); ) {
                    
                    String line;
                    LineNumberReader zikaLnr = new LineNumberReader(zikaPops);
                    while ((line=zikaLnr.readLine())!=null) {
                       
                        /*
                            flds[0] = name of Zika population
                            flds[1] = IRI of Zika population
                            flds[2] = name of human population
                            flds[3] = IRI of human population
                            flds[4] = name of Aedes albopictus population
                            flds[5] = IRI of Aedes albopictus population
                            flds[6] = name of Aedes aegypti population
                            flds[7] = IRI of Aedes aegytpi population
                            flds[8] = start date of epidemic - we ignore it here
                            flds[9] = IRI of epidemic for some
                            flds[10] = IRI of epidemic
                            flds[11] = pref term of epidemic
                        */
                        String[] flds = line.split(Pattern.quote("\t"));

                         /*
                            Create simulating individual individual and connect it to executable: simulating achieves 
                                planned objective of executable.  We're saying one simulating process generates all the 
                                region-specific simulated populations below.
                        */
                        OWLNamedIndividual simulating = createNamedIndividualWithTypeAndLabel(iriMap.lookupClassIri("simulatingx"),
                                iriMap.lookupAnnPropIri("editor preferred"), "simulating of epidemic by disease forecaster with ID = " + forecasterId +
                                " with pathogen = " + flds[0]);
                        createOWLObjectPropertyAssertion(simulating, iriMap.lookupObjPropIri("achieves objective"), executable, odf, oo);


                        OWLNamedIndividual simulatedZikaPopulation = createNamedIndividualWithTypeAndLabel(iriMap.lookupClassIri("simulatedpathogenpopulation"),
                                iriMap.lookupAnnPropIri("editor preferred"), "simulation of " + flds[0] + " created by " + forecasterId);
                        OWLNamedIndividual simulatedHumanPopulation = createNamedIndividualWithTypeAndLabel(iriMap.lookupClassIri("simulatedhostpopulation"),
                                iriMap.lookupAnnPropIri("editor preferred"), "simulation of " + flds[2] + " created by " + forecasterId);
                        OWLNamedIndividual simulatedAlbopictusPopulation = createNamedIndividualWithTypeAndLabel(iriMap.lookupClassIri("simulatedhostpopulation"),
                                iriMap.lookupAnnPropIri("editor preferred"), "simulation of " + flds[4] + " created by " + forecasterId);
                        OWLNamedIndividual simulatedAegyptiPopulation = createNamedIndividualWithTypeAndLabel(iriMap.lookupClassIri("simulatedhostpopulation"),
                                iriMap.lookupAnnPropIri("editor preferred"), "simulation of " + flds[6] + " created by " + forecasterId);

                        createOWLObjectPropertyAssertion(simulating, iriMap.lookupObjPropIri("has specified output"), simulatedZikaPopulation, odf, oo);
                        createOWLObjectPropertyAssertion(simulating, iriMap.lookupObjPropIri("has specified output"), simulatedHumanPopulation, odf, oo);
                        createOWLObjectPropertyAssertion(simulating, iriMap.lookupObjPropIri("has specified output"), simulatedAlbopictusPopulation, odf, oo);
                        createOWLObjectPropertyAssertion(simulating, iriMap.lookupObjPropIri("has specified output"), simulatedAegyptiPopulation, odf, oo);

                        OWLNamedIndividual zikaPop = odf.getOWLNamedIndividual(IRI.create(flds[1]));
                        OWLNamedIndividual humanPop = odf.getOWLNamedIndividual(IRI.create(flds[3]));
                        OWLNamedIndividual albopictusPop = odf.getOWLNamedIndividual(IRI.create(flds[5]));
                        OWLNamedIndividual aegyptiPop = odf.getOWLNamedIndividual(IRI.create(flds[7]));

                        createOWLObjectPropertyAssertion(simulatedZikaPopulation, iriMap.lookupObjPropIri("simulates"), zikaPop, odf, oo);
                        createOWLObjectPropertyAssertion(simulatedHumanPopulation, iriMap.lookupObjPropIri("simulates"), humanPop, odf, oo);
                        createOWLObjectPropertyAssertion(simulatedAlbopictusPopulation, iriMap.lookupObjPropIri("simulates"), albopictusPop, odf, oo);
                        createOWLObjectPropertyAssertion(simulatedAegyptiPopulation, iriMap.lookupObjPropIri("simulates"), aegyptiPop, odf, oo);

                        OWLNamedIndividual dataset = createNamedIndividualWithTypeAndLabel(iriMap.lookupClassIri("dataabouthost"), 
                                iriMap.lookupAnnPropIri("editor preferred"), "dataset about epidemic " + flds[11] + ", which is output by " +
                                forecasterId);
                        OWLNamedIndividual epidemic = odf.getOWLNamedIndividual(IRI.create(flds[10]));
                        createOWLObjectPropertyAssertion(execution, iriMap.lookupObjPropIri("has specified output"),
                                    dataset, odf, oo);
                        createOWLObjectPropertyAssertion(dataset, iriMap.lookupObjPropIri("is about"), epidemic, odf, oo);
                        
                    } //end while readLine()

                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            } //end else if

        } /*  
                End while(i.hasNext()) which is iterating over the identifiers of all the forecasters
          */
          System.out.println("FINISHED PROCESSING FORECASTERS.");
    }

    static protected void handleForecasts(OWLNamedIndividual executable, String forecasterId) {
    	/*
    		Forecasts stored in HashMap
    	*/
    	String forecastList = forecasterIdToForecasts.get(forecasterId);
    	String[] forecasts = forecastList.split(Pattern.quote("|"));

    	/*
    		Create an execution and state that it achieves objective of the executable
    		*/
    	OWLNamedIndividual execution = createNamedIndividualWithTypeAndLabel(
    		iriMap.lookupClassIri("executionof"), iriMap.lookupAnnPropIri("editor preferred"),
    		"execution of forecaster with ID = " + forecasterId + " with output of forecasted data items.");
    	createOWLObjectPropertyAssertion(execution, iriMap.lookupObjPropIri("achieves objective"),
    		executable, odf, oo);

    	for (int i=0; i<forecasts.length; i++) {
    		String label = forecasts[i];
    		IRI classIri = null;
    		if (label.contains("ILI") && label.contains("percent")) {
    			classIri = iriMap.lookupClassIri("weighted mean");
    		} else if (label.contains("count")) {
    			classIri = iriMap.lookupClassIri("epi case count");
    		} else if (label.contains("peak week") || label.toLowerCase().contains("peak week")) {
    			classIri = iriMap.lookupClassIri("peak week");
    		} else if (label.contains("peak intensity") || label.toLowerCase().contains("peak intensity")) {
    			classIri = iriMap.lookupClassIri("peak intensity");
    		} else if (label.contains("start week") || label.toLowerCase().contains("start week")) {
    			classIri = iriMap.lookupClassIri("start week");
    		} else {
    			System.err.println("UNRECOGNIZED FORECAST: " + label);
    			continue;
    		}
    		OWLNamedIndividual forecastInd = createNamedIndividualWithTypeAndLabel(classIri,
    			iriMap.lookupAnnPropIri("label"), label);

    		createOWLObjectPropertyAssertion(execution, iriMap.lookupObjPropIri("has specified output"),
    			forecastInd, odf, oo);
    	}
    }

    static protected void handleForecastFrequency(String forecasterId) {
    	OWLNamedIndividual forecasterInd = identifierToOwlIndividual.get(forecasterId);

    	OWLNamedIndividual simTimeSpec = createNamedIndividualWithTypeAndLabel(
    		iriMap.lookupClassIri("sim time step spec"), iriMap.lookupAnnPropIri("editor preferred"),
    			"simulator time step specification for forecaster with ID = " + forecasterId);
    	OWLNamedIndividual valueSpec = createNamedIndividualWithTypeAndLabel(
    		iriMap.lookupClassIri("scalar value specification"), iriMap.lookupAnnPropIri("editor preferred"),
    			"value specification for simulator time step specification for forecaster with ID = " 
    			+ forecasterId);
    	OWLNamedIndividual timeUnit = odf.getOWLNamedIndividual(iriMap.lookupIndividIri("week"));

    	createOWLObjectPropertyAssertion(simTimeSpec, iriMap.lookupObjPropIri("is part of"),
    		forecasterInd, odf, oo);
    	createOWLObjectPropertyAssertion(simTimeSpec, iriMap.lookupObjPropIri("has value specification"),
    		valueSpec, odf, oo);
    	createOWLObjectPropertyAssertion(valueSpec, iriMap.lookupObjPropIri("has unit"), timeUnit, odf, oo);
    }

    static protected void processPathogenEvolutionModelInfo() {
		try {
    		FileReader fr = new FileReader(p.getProperty("pathogen_evolution_pops"));
    		LineNumberReader lnr = new LineNumberReader(fr);
    		String line;
    		HashMap<String, OWLIndividual> idToExecutable = new HashMap<String, OWLIndividual>();
    		while ((line=lnr.readLine())!=null) {
    			String[] flds = line.split(Pattern.quote("\t"));
    			OWLIndividual pathEvoModelInd = identifierToOwlIndividual.get(flds[0]);
                if (pathEvoModelInd == null) {
                    System.err.println("No pathogen evolution model individual found for identifier: " + flds[0]);
                }
    			IRI indexingPopIri = IRI.create(flds[1]);
    			OWLIndividual popInd = odf.getOWLNamedIndividual(indexingPopIri);

    			OWLIndividual executable = null;
    			if (idToExecutable.containsKey(flds[0])) {
    				executable = idToExecutable.get(flds[0]);
    			} else {
    				executable = createNamedIndividualWithTypeAndLabel(iriMap.lookupClassIri("executable"),
                        iriMap.lookupAnnPropIri("editor preferred"), "executable for pathogen evolution model with ID = " + flds[0]);
                	OWLNamedIndividual compiling = createNamedIndividualWithTypeAndLabel(iriMap.lookupClassIri("compiling"),
                        iriMap.lookupAnnPropIri("editor preferred"), "compiling of pathogen evolution model with ID = " 
                        + flds[0] + " into executable form");
                	/*
                   		 The compiling has the model as specified input. has specified input
                	*/
                	createOWLObjectPropertyAssertion(compiling, iriMap.lookupObjPropIri("has specified input"), pathEvoModelInd, odf, oo);

                	/* 
                    	The compiling has the executable as specified output. has specified output
                	*/
               		createOWLObjectPropertyAssertion(compiling, iriMap.lookupObjPropIri("has specified output"), executable, odf, oo);

               		idToExecutable.put(flds[0], executable);
    			}

                OWLNamedIndividual execution = createNamedIndividualWithTypeAndLabel(iriMap.lookupClassIri("executionof"),
                        iriMap.lookupAnnPropIri("editor preferred"), "execution process of pathogen evolution model with ID = " + flds[0]);
    			 OWLNamedIndividual dataset = createNamedIndividualWithTypeAndLabel(iriMap.lookupClassIri("dna sequence data set"), 
                                iriMap.lookupAnnPropIri("editor preferred"), "DNA sequence data set about pathogen population " + flds[2] + ", which is input into " +
                                flds[0] + " pathogen evolution model");

                /*
                	The execution achieves the objective of the executable
               	*/
				createOWLObjectPropertyAssertion(execution, iriMap.lookupObjPropIri("achieves objective"),
                               executable, odf, oo);
                
				/*
					And the execution has specified input the dataset
				*/
				createOWLObjectPropertyAssertion(execution, iriMap.lookupObjPropIri("has specified input"),
								dataset, odf, oo);
                      
                /*
					And the dataset is about the pathogen population
				*/
                createOWLObjectPropertyAssertion(dataset, iriMap.lookupObjPropIri("is about"), popInd, odf, oo);
    		}
    	} catch (IOException ioe) {
    		ioe.printStackTrace();
    	}    	
    }

    static protected void processPopulationDynamicsModelInfo() {
        try {
            FileReader fr = new FileReader(p.getProperty("population_dynamics_pops"));
            LineNumberReader lnr = new LineNumberReader(fr);
            String line;
            HashMap<String, OWLIndividual> idToExecutable = new HashMap<String, OWLIndividual>();
            while ((line=lnr.readLine())!=null) {
                String[] flds = line.split(Pattern.quote("\t"));
                OWLIndividual popDynamicsModelInd = identifierToOwlIndividual.get(flds[0]);
                IRI indexingPopIri = IRI.create(flds[1]);
                OWLIndividual popInd = odf.getOWLNamedIndividual(indexingPopIri);


                OWLIndividual executable = null;
                if (idToExecutable.containsKey(flds[0])) {
                	executable = idToExecutable.get(flds[0]);
                } else {
                	executable = createNamedIndividualWithTypeAndLabel(iriMap.lookupClassIri("executable"),
                        iriMap.lookupAnnPropIri("editor preferred"), "executable for population dynamics model with ID = " + flds[0]);
               		OWLNamedIndividual compiling = createNamedIndividualWithTypeAndLabel(iriMap.lookupClassIri("compiling"),
                        iriMap.lookupAnnPropIri("editor preferred"), "compiling of population dynamics model with ID = " 
                        + flds[0] + " into executable form");

               		/*
                    	The compiling has the model as specified input. has specified input
               		 */
                	createOWLObjectPropertyAssertion(compiling, iriMap.lookupObjPropIri("has specified input"), popDynamicsModelInd, odf, oo);

                	/* 
                 	   The compiling has the executable as specified output. has specified output
                	*/
                	createOWLObjectPropertyAssertion(compiling, iriMap.lookupObjPropIri("has specified output"), executable, odf, oo);

                	idToExecutable.put(flds[0], executable);
                }
                
                OWLNamedIndividual execution = createNamedIndividualWithTypeAndLabel(iriMap.lookupClassIri("executionof"),
                        iriMap.lookupAnnPropIri("editor preferred"), "execution process of population dynamics model with ID = " + flds[0]);
                 OWLNamedIndividual dataset = createNamedIndividualWithTypeAndLabel(iriMap.lookupClassIri("dataabouthost"), 
                                iriMap.lookupAnnPropIri("editor preferred"), "dataset about population " + flds[2] + ", which is input into the " +
                                flds[0] + " population dynamics model");



                /*
                    The execution achieves the objective of the executable
                */
                createOWLObjectPropertyAssertion(execution, iriMap.lookupObjPropIri("achieves objective"),
                               executable, odf, oo);
                
                /*
                    And the execution has specified input the dataset
                */
                createOWLObjectPropertyAssertion(execution, iriMap.lookupObjPropIri("has specified input"),
                                dataset, odf, oo);
                      
                /*
                    And the dataset is about the pathogen population
                */
                createOWLObjectPropertyAssertion(dataset, iriMap.lookupObjPropIri("is about"), popInd, odf, oo);
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }       
    }

    static protected void processDiseaseTransmissionTreeEstimatorInfo() {
        try {
            FileReader fr = new FileReader(p.getProperty("transmission_tree_pops"));
            LineNumberReader lnr = new LineNumberReader(fr);
            String line;
            HashMap<String, OWLIndividual> idToExecutable = new HashMap<String, OWLIndividual>();
            while ((line=lnr.readLine())!=null) {
                String[] flds = line.split(Pattern.quote("\t"));
                OWLIndividual treeEstimatorInd = identifierToOwlIndividual.get(flds[0]);
                //System.out.println(treeEstimatorInd);
                String simHostLabel = flds[0] + "-generated simulation of " + flds[4];
                String simPathogenLabel = flds[0] + "-generated simulation of " + flds[2];

                //executable
                OWLIndividual executable = null;
                if (idToExecutable.containsKey(flds[0])) {
                	executable = idToExecutable.get(flds[0]);
                } else {
                	executable = createNamedIndividualWithTypeAndLabel(iriMap.lookupClassIri("executable"),
                        iriMap.lookupAnnPropIri("editor preferred"), "executable for disease transmission tree estimator with ID = " + flds[0]);
                	OWLNamedIndividual compiling = createNamedIndividualWithTypeAndLabel(iriMap.lookupClassIri("compiling"),
                        iriMap.lookupAnnPropIri("editor preferred"), "compiling of disease transmission tree estimator with ID = " 
                        + flds[0] + " into executable form");
                	idToExecutable.put(flds[0], executable);

                	/*
                    	The compiling has the model as specified input. has specified input
                	*/
                	createOWLObjectPropertyAssertion(compiling, iriMap.lookupObjPropIri("has specified input"), treeEstimatorInd, odf, oo);

                	/* 
                    	The compiling has the executable as specified output. has specified output
                	*/
                	createOWLObjectPropertyAssertion(compiling, iriMap.lookupObjPropIri("has specified output"), executable, odf, oo);
                }

                //simulating and output
                OWLNamedIndividual simulating = createNamedIndividualWithTypeAndLabel(iriMap.lookupClassIri("simulatingx"),
                        iriMap.lookupAnnPropIri("editor preferred"), "simulating process of transmission tree estimator with ID = " + flds[0]);
                OWLNamedIndividual simHostPopInd = createNamedIndividualWithTypeAndLabel(iriMap.lookupClassIri("simulatedhostpopulation"), 
                        iriMap.lookupAnnPropIri("label"), simHostLabel);
                OWLNamedIndividual simPathogenPopInd = createNamedIndividualWithTypeAndLabel(iriMap.lookupClassIri("simulatedpathogenpopulation"), 
                        iriMap.lookupAnnPropIri("label"), simPathogenLabel);

                //actual host and pathogen populations
                OWLNamedIndividual hostPopInd = odf.getOWLNamedIndividual(IRI.create(flds[3]));
                OWLNamedIndividual pathogenPopInd = odf.getOWLNamedIndividual(IRI.create(flds[1]));


                /*
                    The simulating achieves the objective of the executable
                */
                createOWLObjectPropertyAssertion(simulating, iriMap.lookupObjPropIri("achieves objective"),
                               executable, odf, oo);

                //simulating, simulating output
                createOWLObjectPropertyAssertion(simulating, iriMap.lookupObjPropIri("has specified output"), simHostPopInd, odf, oo);
                createOWLObjectPropertyAssertion(simulating, iriMap.lookupObjPropIri("has specified output"), simPathogenPopInd, odf, oo);

                //simulated host simulates host, simulated pathogen simulates pathogen
                createOWLObjectPropertyAssertion(simHostPopInd, iriMap.lookupObjPropIri("simulates"), hostPopInd, odf, oo);
                createOWLObjectPropertyAssertion(simPathogenPopInd, iriMap.lookupObjPropIri("simulates"), pathogenPopInd, odf, oo);
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }     
    }

    static protected void handleSoftwareInputs(Map.Entry<String, JsonElement> e, HashMap<String, OWLNamedIndividual> niMap,
                                               OWLOntology oo, OWLDataFactory odf, IriLookup iriMap) {

        JsonElement je = e.getValue();
        if (je instanceof JsonArray) {
            JsonArray elemArray = (JsonArray) je;
            int cInput = elemArray.size();
            Iterator<JsonElement> elemIter = elemArray.iterator();
            //System.out.println("Web application size: " + elemArray.size());
            while (elemIter.hasNext()) {
                JsonElement elemi = elemIter.next();
                if (elemi instanceof JsonObject) {
                    JsonObject jo = (JsonObject)elemi;
                    JsonElement numElement = jo.get("inputNumber");
                    String numElemValue = numElement.getAsString();
                    System.out.println("\t\tSOFTWARE INPUT: INPUT NUMBER " + numElemValue);
                    JsonElement descriptionElement = jo.get("description");
                    String descriptionElementValue = descriptionElement.getAsString();
                    JsonElement dataFormatsElement = jo.get("dataFormats");
                    JsonArray formatsArray = (JsonArray)dataFormatsElement;
                    ArrayList<String> inputFormats = new ArrayList<String>();
                    if (dataFormatsElement == null) {
                        System.err.println("Software input #" + numElemValue + " for software " + fullName + " has no dataFormats element at all.");
                    }  else {
                        Iterator<JsonElement> formatIterator = formatsArray.iterator();
                   
                        while (formatIterator.hasNext()) {
                            JsonElement formatElemI = formatIterator.next();
                            String formatI = formatElemI.getAsString();
                            inputFormats.add(formatI);
                        }
                    }

                    JsonElement isOptionalElement = jo.get("isOptional");
                    //System.out.println("\t\tSOFTWARE INPUT OPTIONAL : " + isOptionalElement);
                    JsonElement completeElement = jo.get("isListOfDataFormatsComplete");
                    //System.out.println("\t\tSOFTWARE INPUT IS COMPLETE: " + completeElement);

                    String isOptional = (isOptionalElement == null) ? null : isOptionalElement.getAsString();
                    String complete = (completeElement == null) ? null : completeElement.getAsString();

                    handleSoftwareInput(numElemValue, cInput, descriptionElementValue, isOptional, complete, inputFormats, niMap, oo, odf, iriMap);
                } else {
                    System.err.println("The software input is not an object as expected: " + elemi);
                }

            }
        } else {
            System.err.println("Inputs attribute has value that is not array.");
            throw new IllegalArgumentException("Inputs attribute must be array.");
        }
    }

    static protected void handleSoftwareInput(String iInput, int cInput, String description, String isOptional, 
                                String isListOfDataFormatsComplete, List<String> inputFormats, HashMap<String, OWLNamedIndividual> niMap,
                                               OWLOntology oo, OWLDataFactory odf, IriLookup iriMap) {

            String planSpecPrefTerm = "data input #" + iInput + " of " + cInput + " for executable of " + fullName;
            //create a named individual that represents the software input
            OWLNamedIndividual planSpecInd = createNamedIndividualWithTypeAndLabel(odf, oo, iriMap.lookupClassIri("dataInputPlanSpecification"),
                    iriMap.lookupAnnPropIri("editor preferred"), planSpecPrefTerm);
            //add the description element value as the rdfs:label of the software input
            addAnnotationToIndividual(planSpecInd, iriMap.lookupAnnPropIri("label"), description, odf, oo);
            //connect input plan specification to executable.  The software input plan specification is part of the executable.
            createOWLObjectPropertyAssertion(planSpecInd, iriMap.lookupObjPropIri("is part of"), niMap.get("executable"), odf, oo);
            
            //for each format in the list for the current software input, associate the input format with the input plan specification
            //  via a data parsing according to the input format specification.
            for (String inputFormat : inputFormats) {
                //lookup from known list of data formats, which should have been prepared by DataFormatProcessor.java first and saved 
                //  in a file for use here.
                OWLNamedIndividual formatInd = formatInds.get(inputFormat);
                if (formatInd == null) {
                    //If the data format is not a known, named format from the MDC, alert!
                    System.err.println("UNRECOGNIZED DATA INPUT FORMAT: " + inputFormat);

                    /*
                        Go ahead and create a non-MDC-registered data format.  We just have to be careful when
                            writing SPARQL queries that we do not count this format to the FAIR-o-meter metrics,
                            both in terms of data formats in MDC, and in terms of the number of inputs with a data
                            format registered in MDC. 
                    */
                    String formatSpecLabel = inputFormat + ", a data format not cataloged in MDC, but that is used as " +
                            "a data format to input number " + iInput + " of the software " + fullName + ", which is a software that is cataloged in MDC.";
                    formatInd = createNamedIndividualWithTypeAndLabel(odf, oo, iriMap.lookupClassIri("dataformat"),
                            iriMap.lookupAnnPropIri("editor preferred"), formatSpecLabel);   

                    if (!inputFormat.equals("Undocumented") && !inputFormat.equals("Proprietary"))
                        formatInds.put(inputFormat, formatInd);  //save just in case we encounter it again.                 
                }

                addAnnotationToIndividual(formatInd, iriMap.lookupAnnPropIri("comment"), 
                        "data input format for " + fullName, odf, oo);

                String dataParsingLabel = "data parsing of file in " + inputFormat + " format by " + fullName;
                OWLNamedIndividual dataParsingInd = createNamedIndividualWithTypeAndLabel(odf, oo, iriMap.lookupClassIri("dataparsing"),
                            iriMap.lookupAnnPropIri("editor preferred"), dataParsingLabel);

                //connect parsing to format.  Parsing realizes data input format specification
                createOWLObjectPropertyAssertion(dataParsingInd, iriMap.lookupObjPropIri("achieves objective"), formatInd, odf, oo);

                //connect parsing to plan specification.  Parsing realizes data input specification.
                createOWLObjectPropertyAssertion(dataParsingInd, iriMap.lookupObjPropIri("achieves objective"), planSpecInd, odf, oo);
            }

            //TODO we need to add info about optionality

            //TODO we need to add info about whether list of formats is complete. 
    }

    static protected void handleSoftwareOutputs(Map.Entry<String, JsonElement> e, HashMap<String, OWLNamedIndividual> niMap,
                                               OWLOntology oo, OWLDataFactory odf, IriLookup iriMap) {
        JsonElement je = e.getValue();
        if (je instanceof JsonArray) {
            JsonArray elemArray = (JsonArray) je;
            int cOutput = elemArray.size();
            Iterator<JsonElement> elemIter = elemArray.iterator();
            //System.out.println("Web application size: " + elemArray.size());
            while (elemIter.hasNext()) {
                JsonElement elemi = elemIter.next();
                if (elemi instanceof JsonObject) {
                    JsonObject jo = (JsonObject)elemi;
                    JsonElement numElement = jo.get("outputNumber");
                    String numElemValue = numElement.getAsString();
                    System.out.println("\t\tSOFTWARE OUTPUT: OUTPUT NUMBER " + numElemValue);
                    JsonElement descriptionElement = jo.get("description");
                    String descriptionElementValue = descriptionElement.getAsString();
                    JsonElement dataFormatsElement = jo.get("dataFormats");
                    JsonArray formatsArray = (JsonArray)dataFormatsElement;
                    ArrayList<String> outputFormats = new ArrayList<String>();
                    if (dataFormatsElement == null) {
                        System.err.println("Software output #" + numElemValue + " for software " + fullName + " has no dataFormats element at all.");
                    }  else {
                        Iterator<JsonElement> formatIterator = formatsArray.iterator();
                   
                        while (formatIterator.hasNext()) {
                            JsonElement formatElemI = formatIterator.next();
                            String formatI = formatElemI.getAsString();
                            outputFormats.add(formatI);
                        }
                    }

                    JsonElement isOptionalElement = jo.get("isOptional");
                    //System.out.println("\t\tSOFTWARE OUTPUT OPTIONAL : " + isOptionalElement);
                    JsonElement completeElement = jo.get("isListOfDataFormatsComplete");
                    //System.out.println("\t\tSOFTWARE OUTPUT IS COMPLETE: " + completeElement);

                    String isOptional = (isOptionalElement == null) ? null : isOptionalElement.getAsString();
                    String complete = (completeElement == null) ? null : completeElement.getAsString();

                    handleSoftwareOutput(numElemValue, cOutput, descriptionElementValue, isOptional, complete, outputFormats, niMap, oo, odf, iriMap);
                } else {
                    System.err.println("The software output is not an object as expected: " + elemi);
                }

            }
        } else {
            System.err.println("Outputs attribute has value that is not array.");
            throw new IllegalArgumentException("Outputs attribute must be array.");
        }
    }

    static protected void handleSoftwareOutput(String iOutput, int cOutput, String description, String isOptional, 
                                String isListOfDataFormatsComplete, List<String> outputFormats, HashMap<String, OWLNamedIndividual> niMap,
                                               OWLOntology oo, OWLDataFactory odf, IriLookup iriMap) {
            String planSpecPrefTerm = "data output #" + iOutput + " of " + cOutput + " for executable of " + fullName;
            //create a named individual that represents the software output
            OWLNamedIndividual planSpecInd = createNamedIndividualWithTypeAndLabel(odf, oo, iriMap.lookupClassIri("dataOutputPlanSpecification"),
                    iriMap.lookupAnnPropIri("editor preferred"), planSpecPrefTerm);
            //add the description element value as the rdfs:label of the software output
            addAnnotationToIndividual(planSpecInd, iriMap.lookupAnnPropIri("label"), description, odf, oo);
            //connect output plan specification to executable.  The software output plan specification is part of the executable.
            createOWLObjectPropertyAssertion(planSpecInd, iriMap.lookupObjPropIri("is part of"), niMap.get("executable"), odf, oo);
            
            //for each format in the list for the current software output, associate the output format with the output plan specification
            //  via a data parsing according to the output format specification.
            for (String outputFormat : outputFormats) {
                //lookup from known list of data formats, which should have been prepared by DataFormatProcessor.java first and saved 
                //  in a file for use here.
                OWLNamedIndividual formatInd = formatInds.get(outputFormat);
                if (formatInd == null) {
                    //If the data format is not a known, named format from the MDC, alert!
                    System.err.println("UNRECOGNIZED DATA OUTPUT FORMAT: " + outputFormat);

                    /*
                        Go ahead and create a non-MDC-registered data format.  We just have to be careful when
                            writing SPARQL queries that we do not count this format to the FAIR-o-meter metrics,
                            both in terms of data formats in MDC, and in terms of the number of outputs with a data
                            format registered in MDC. 
                    */
                    String formatSpecLabel = outputFormat + ", a data format not cataloged in MDC, but that is used as " +
                            "a data format to output number " + iOutput + " of the software " + fullName + ", which is a software that is cataloged in MDC.";
                    formatInd = createNamedIndividualWithTypeAndLabel(odf, oo, iriMap.lookupClassIri("dataformat"),
                            iriMap.lookupAnnPropIri("editor preferred"), formatSpecLabel);   

                    if (!outputFormat.equals("Undocumented") && !outputFormat.equals("Proprietary"))
                        formatInds.put(outputFormat, formatInd);  //save just in case we encounter it again.                 
                }

                addAnnotationToIndividual(formatInd, iriMap.lookupAnnPropIri("comment"), 
                        "data output format for " + fullName, odf, oo);

                String dataEncodingLabel = "data encoding of file in " + outputFormat + " format by " + fullName;
                OWLNamedIndividual dataEncodingInd = createNamedIndividualWithTypeAndLabel(odf, oo, iriMap.lookupClassIri("dataencoding"),
                            iriMap.lookupAnnPropIri("editor preferred"), dataEncodingLabel);

                //connect parsing to format.  Parsing realizes data output format specification
                createOWLObjectPropertyAssertion(dataEncodingInd, iriMap.lookupObjPropIri("achieves objective"), formatInd, odf, oo);

                //connect parsing to plan specification.  Parsing realizes data output specification.
                createOWLObjectPropertyAssertion(dataEncodingInd, iriMap.lookupObjPropIri("achieves objective"), planSpecInd, odf, oo);
            }

            //TODO we need to add info about optionality

            //TODO we need to add info about whether list of formats is complete.       
    }
}
