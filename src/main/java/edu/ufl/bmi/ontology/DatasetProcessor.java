package edu.ufl.bmi.ontology;

import java.lang.StringBuilder;

import java.io.FileReader;
import java.io.LineNumberReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Properties;
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
import edu.ufl.bmi.misc.ControlMeasureIriMapping;
import edu.ufl.bmi.misc.PublicationLinks;

public class DatasetProcessor {
    static long iriCounter = 1200015000L;
    static String iriPrefix = "http://www.pitt.edu/obc/IDE_ARTICLE_";
    static int iriLen = 10;

    static OWLOntologyManager oom;

    static String baseName;
    static String[] versionNames;
    static String[] fullNames;

    static String versionSuffix;
    static String fullName;

    static OWLNamedIndividual olympus;

    static HashSet<String> uniqueFormats;

    static HashMap<String, OWLNamedIndividual> formatInds;
    static HashMap<String, OWLNamedIndividual> devNis;
    static HashMap<String, OWLNamedIndividual> dateNis;
    static HashMap<String, OWLNamedIndividual> licenseNis;
	static HashMap<String, OWLNamedIndividual> websiteInds;
	static HashMap<String, String> datasetIdToTypeHandle;
    static Properties p;

    public static void main(String[] args) {
    	FileReader fr = null;
    	LineNumberReader lnr = null;
    	FileOutputStream fos = null;
    	FileWriter fw = null;
    	FileWriter devOut = null;

		try ( FileReader pr = new FileReader(args[0]); ) {

			p = new Properties();
			p.load(pr);

			/*   Modifications needed to work more smoothly with Matt's DATS processor:

					1. We'll assume there are multiple files with dataset metadata instead of one big one
					2. All these files will be in one folder
					3. We'll try to use the file name to get the ontology class of which each dataset mentioned in the file is an instance
					4. For screwball files, like "location", we'll fail to find a class, in which case we'll fall back on a 
					    manually curated file.  This file will have a dataset identifier | ontology class lookup string mapping like such:
					    		 1. https://data.cdc.gov/api/views/cjae-szjv	atmospheric data set
								 2. https://data.cityofnewyork.us/api/views/w9ei-idxz	treatment facility census
								 3. https://data.cityofnewyork.us/api/views/kku6-nxdu	population demographic census
								 4. https://data.kingcounty.gov/api/views/7pbe-yd3f	treatment facility census
								 5. https://data.sfgov.org/api/views/yg87-cd6v	treatment facility census

				I will deprecate the "dataset_info" property and the new paradigm will look for dataset_directory and dataset_type_info
					properties, where obviously the first one is the directory full of data sets and the second one is obviously
					the manually curated, overriden, dataset type (class) information.
			*/

			fr = new FileReader(p.getProperty("dataset_info"));
			lnr = new LineNumberReader(fr);

		    IriLookup iriMap = new IriLookup(p.getProperty("iris"));
		    iriMap.init();

		    oom = OWLManager.createOWLOntologyManager();
		    OWLDataFactory odf = OWLManager.getOWLDataFactory();
		    OWLOntology oo = null;
		    IRI ontologyIRI = IRI.create("http://www.pitt.edu/mdc/dataset");
		    try {
				oo = oom.createOntology(ontologyIRI);
		    } catch (OWLOntologyCreationException ooce) {
				ooce.printStackTrace();
		    }

		    olympus = odf.getOWLNamedIndividual(iriMap.lookupIndividIri("olympus"));
		    HashSet<String> uniqueLocationsCovered = new HashSet<String>();
	        uniqueFormats = new HashSet<String>();
	        devNis = new HashMap<String, OWLNamedIndividual>();
	        String developerFname = p.getProperty("developer_ind");
	        developerFname = developerFname.replace("<date>", args[1].trim()).trim();
	        loadDevelopers(developerFname, odf);
			dateNis = new HashMap<String, OWLNamedIndividual>();

	        loadAndCreateDataFormatIndividuals(odf, oo, iriMap);
	        loadLicenses(p.getProperty("license_info"), odf);
	        String websiteFname = p.getProperty("website_ind");
	        websiteFname = websiteFname.replace("<date>", args[1].trim());
		 	loadWebsites(websiteFname, odf);

		 	loadCuratedDatasetTypeInfo();

		 	String line;
		    while((line=lnr.readLine())!=null) {
				String[] flds = line.split(Pattern.quote("\t"), -1);
				if (flds.length < 22) {
					System.err.println("Line #" + lnr.getLineNumber() + " is too short (len=" + flds.length + " fields. Need at least 23). Line: " + line);
					continue;
				}
				
				String dataSubtype = cleanField(flds[0]);   if (dataSubtype.startsWith("\uFEFF")) { dataSubtype = dataSubtype.substring(1);  }
				String title = cleanField(flds[1]);
				String description = cleanField(flds[2]);
				String datasetIdentifier = cleanField(flds[3]);
				/*
				String disease = flds[4].trim();
				String authors = flds[5].trim();
				String created = flds[6].trim();
				String modified = flds[7].trim();
				String curated = flds[8].trim();
				String landingPage = flds[9].trim();
				String accessPage = flds[10].trim();
				String format = flds[12].trim();
				String geography = flds[14].trim();
				String apolloLocationCode = flds[15].trim();
				String iso_3166 = flds[16].trim();
				String iso_3166_1 = flds[17].trim();
				String iso_3166_1_alpha_3 = flds[18].trim();
				String aoc = flds[19].trim();
				String ae = flds[20].trim();
				String license = flds[13].trim();
				String popIriTxt = (flds[22] != null) ? flds[22].trim() : null;
				String beIriTxt = (flds.length > 25 && flds[25] != null) ? flds[25].trim() : null;
				String ecIriTxt = (flds.length > 27 && flds[27] != null) ? flds[27].trim() : null;
				String epiIriTxt = (flds.length > 30 && flds[30] != null) ? flds[30].trim() : null;
				*/
				String authors = cleanField(flds[4]);
				String created = cleanField(flds[5]);
				String modified = cleanField(flds[6]);
				String curated = cleanField(flds[7]);
				String landingPage = cleanField(flds[8]);
				String accessPage = cleanField(flds[9]);
				String format = cleanField(flds[11]);
				String geography = cleanField(flds[13]);
				String apolloLocationCode = cleanField(flds[14]);
				String iso_3166 = cleanField(flds[15]);
				String iso_3166_1 = cleanField(flds[16]);
				String iso_3166_1_alpha_3 = cleanField(flds[17]);
				String aoc = cleanField(flds[18]);
				String ae = cleanField(flds[19]);
				String license = cleanField(flds[12]);
				String popIriTxt = (flds[21] != null) ? cleanField(flds[21]) : null;
				String beIriTxt = (flds.length > 23 && flds[23] != null) ? cleanField(flds[24]) : null;
				String ecIriTxt = (flds.length > 25 && flds[25] != null) ? cleanField(flds[25]) : null;
				String epiIriTxt = (flds.length > 29 && flds[29] != null) ? cleanField(flds[29]) : null;

		    	//We'll create them as agent level ecosystem data sets, case series, etc.
			    System.out.println("SUBTYPE.  subtype=\"" + dataSubtype + "\"");
			    
			    //Need to add dataset types to iris.txt
			    IRI classIri = iriMap.lookupClassIri(dataSubtype);
			    if (classIri == null) {
			    	dataSubtype = datasetIdToTypeHandle.get(datasetIdentifier);
			    	classIri = iriMap.lookupClassIri(dataSubtype);
			    }
			    if (classIri != null) {

					System.out.println("\t"+ title + " is a " + dataSubtype);
					baseName = title;
					fullName = title;
							
					//System.out.println("base name = " + baseName + ", version = " + version);
					String baseLabel = (versionNames == null) ? baseName : baseName + " - " + 
				    	((Character.isDigit(versionSuffix.charAt(0))) ? " v" + versionSuffix : versionSuffix);
					fullName = baseLabel + ", " + dataSubtype;
					System.out.println("\t" + fullName);
					
					IRI edPrefIri = iriMap.lookupAnnPropIri("editor preferred");
					IRI labelIri = iriMap.lookupAnnPropIri("label");
					IRI titleIri = iriMap.lookupAnnPropIri("title");
					OWLNamedIndividual dataset = createNamedIndividualWithTypeAndLabel(odf, oo, classIri, edPrefIri, fullName);
					addAnnotationToNamedIndividual(dataset, titleIri, title, odf, oo);
					addAnnotationToNamedIndividual(dataset, labelIri, baseLabel, odf, oo);

					HashMap<String, OWLNamedIndividual> niMap = new HashMap<String, OWLNamedIndividual>();
					niMap.put("dataset", dataset);

					OWLNamedIndividual mdcInd = odf.getOWLNamedIndividual(iriMap.lookupIndividIri("mdc"));
					/*
						Add all data sets to MDC.
					*/
					createOWLObjectPropertyAssertion(mdcInd, iriMap.lookupObjPropIri("has proper part"), dataset, odf, oo);
					
					if (isValidFieldValue(description)) {
						addAnnotationToNamedIndividual(dataset, iriMap.lookupAnnPropIri("description"),
							description, odf, oo);
					}

			    	if (isValidFieldValue(datasetIdentifier)) {
						handleDatasetIdentifier(datasetIdentifier, niMap, oo, odf, iriMap);
			    	} 

			    	if (isValidFieldValue(authors)) {
			    		addAnnotationToNamedIndividual(dataset, iriMap.lookupAnnPropIri("authors"), authors, odf, oo);
						handleDeveloper(authors, niMap, oo, odf, iriMap);
			    	} 

			    	if (isValidFieldValue(created)) {
			    		/*for backwards compatibility with existing OBC.ide, make the dc:date of the data set, the creation 
			    		  date
						*/
						addAnnotationToNamedIndividual(dataset, iriMap.lookupAnnPropIri("data set date"), created, odf, oo);
			    		handleCreationDate(created, niMap, oo, odf, iriMap);
			    	}

			    	if (isValidFieldValue(modified)) {
			    		/* we're going to need a class "process of modifying a data set".  Then follow pretty much the same
			    			outline as above.  Except there's no direct annotation on the dataset for backwards compatibility.
			    			*/
			    		
			    	}

			    	if (isValidFieldValue(curated)) {
			    		/* key question.  what's the difference betweeen modifying and curating (and maybe even creating)?
			    			But, assuming we can define a real difference, then follows the same pattern as modifying.
			    		*/
			    		
			    	}

			    	if (isValidFieldValue(landingPage)) {
						handleLocation(landingPage, niMap, oo, odf, iriMap);
			    	}

			    	if (isValidFieldValue(accessPage)) {
			    		IRI urlIri = iriMap.lookupAnnPropIri("hasURL");
			    		addAnnotationToNamedIndividual(dataset, urlIri, accessPage, odf, oo);
			    	}

			    	if (isValidFieldValue(format)) {
						handleDataFormats(format, dataSubtype, niMap, oo, odf, iriMap);
					}

			    	if (isValidFieldValue(aoc) && aoc.toLowerCase().equals("true")) {
						OWLNamedIndividual datasetConcInd = niMap.get("olympusConc");
						if (datasetConcInd == null) {
							datasetConcInd = createNamedIndividualWithTypeAndLabel(odf, oo, iriMap.lookupClassIri("olympusConc"), edPrefIri, fullName + " concretization on Olympus");
							niMap.put("olympusConc",datasetConcInd);
						}
						createOWLObjectPropertyAssertion(olympus, iriMap.lookupObjPropIri("bearer"), datasetConcInd, odf, oo);
						createOWLObjectPropertyAssertion(dataset, iriMap.lookupObjPropIri("is concretized as"), datasetConcInd, odf, oo);
			    	}		    	

			    	if (isValidFieldValue(ae) && ae.toLowerCase().equals("true")) {
						//just treat it like any other data format, I think

			    	} 

			    	if (isValidFieldValue(license)) {
			    		handleLicense(license, niMap, oo, odf, iriMap);
			    	}
					
					processAnyIndexingTerms(popIriTxt, beIriTxt, ecIriTxt, epiIriTxt, dataset, iriMap, odf, oo);
				
				} else { //end if classIri != null
					System.err.println("Cannot find class IRI for specified type of dataset: " + dataSubtype);
				}
			}

			

			try {
				SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
				String dateTxt = df.format(new Date());
				String owlFileName = "./dataset-ontology-" + dateTxt + ".owl";
				fos = new FileOutputStream(owlFileName);
			    oom.saveOntology(oo, fos);
			 
			} catch (OWLOntologyStorageException oose) {
			    oose.printStackTrace();
			}
		     //while((line=lnr.readLine())).
			
			fw = new FileWriter("websites.txt");
			Set<String> keys = websiteInds.keySet();
			for (String key : keys) {
				OWLNamedIndividual oni = websiteInds.get(key);
				fw.write(key + "\t" + oni.getIRI() + "\n");
			}

			
			System.out.println(nextIri());
			System.out.println(nextIri());

		} catch (IOException ioe) {
		    ioe.printStackTrace();
		} finally {
			try {
				if (fw != null) fw.close();
				if (fos != null) fos.close();
				if (lnr != null) lnr.close();
				if (fr != null) fr.close();
			} catch (IOException ioe) {
				//just eat it, eat it, don't you make me repeat it!
			}
		}
    }

    public static String cleanField(String field) {
    	field = field.trim();
    	if (field.startsWith("\"") && field.endsWith("\"")) {
    		int end = field.length()-1;
    		field = field.substring(1, end);
    	}
    	return field;
    }

    public static void loadDevelopers(String fName, OWLDataFactory odf) throws IOException {
    	FileReader fr = new FileReader(fName);
    	LineNumberReader lnr = new LineNumberReader(fr);
    	String line;
    	while ((line=lnr.readLine())!=null) {
    		String[] flds = line.split(Pattern.quote("\t"));
    		String developerName = flds[0].toLowerCase();
    		OWLNamedIndividual devInd = odf.getOWLNamedIndividual(IRI.create(flds[1]));
    		devNis.put(developerName, devInd);
    		System.out.println("Loading developer " + developerName);
    	}
    	lnr.close();
    	fr.close();
    }

    /*
    	MDC has some datasets listed as just "location data", which ontologically speaking, isn't great.

    	Since there's just 6 or so of them at the moment, we manually curated the ontology class for each of them.

    	We'll load that information, and then for any dataset for which we don't find an ontology class, we'll
    		look to this manually curated information to get them.
    */
    public static void loadCuratedDatasetTypeInfo() {
    	try {
    		String curatedInfoFname = p.getProperty("curated_dataset_type");
    		FileReader fr = new FileReader(curatedInfoFname);
    		LineNumberReader lnr = new LineNumberReader(fr);

    		String line;
			datasetIdToTypeHandle = new HashMap<String, String>();
    		while((line=lnr.readLine())!=null) {
    			String[] flds = line.split(Pattern.quote("\t"));
    			//flds[0] = dataset id
    			//flds[1] = handle to dataset type in iris.txt
    			datasetIdToTypeHandle.put(flds[0], flds[1]);
    		}
    	} catch (IOException ioe) {
    		ioe.printStackTrace();
    	}
    }

    /*
    	There's a lot of non-machine interpretable ugliness in these DATS files!
    */
    public static boolean isValidFieldValue(String value) {
    	String cleanValue = (value != null) ? value.trim().toLowerCase() : null;
    	return (value != null && cleanValue.length() > 0  
    				&& !cleanValue.equals("null") 
    				&& !cleanValue.equals("n/a")
    				&& !cleanValue.startsWith("?") 
    				&& !cleanValue.equals("under development") 
    				&& !cleanValue.startsWith("identifier will be created at time of release") 
    				&& !cleanValue.startsWith("anonymous")
    				&& !cleanValue.startsWith("unknown")
    				&& !cleanValue.equals("-")
    				);
    }

    public static void handleTitle(String title, HashMap<String, OWLNamedIndividual> niMap,
					   OWLOntology oo, OWLDataFactory odf, IriLookup iriMap) {
		OWLNamedIndividual oni = niMap.get("dataset");
        addAnnotationToNamedIndividual(oni, iriMap.lookupAnnPropIri("title"), title, odf, oo);
	}

	/*
    public static void handleVersion(Map.Entry<String, JsonElement> e, HashMap<String, OWLNamedIndividual> niMap,
					   OWLOntology oo, OWLDataFactory odf, IriLookup iriMap) {
	OWLNamedIndividual oni = niMap.get("versionid");
	addAnnotationToNamedIndividual(oni, iriMap.lookupAnnPropIri("label"), versionSuffix, odf, oo);
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
    */

    public static void handleDatasetIdentifier(String datasetIdentifier, HashMap<String, OWLNamedIndividual> niMap,
					   OWLOntology oo, OWLDataFactory odf, IriLookup iriMap) {

    	String url = null, idText = null, editorPreferred = "identifier for " + fullName;
    
    	if (datasetIdentifier.contains("<a href=")) {
		    Document d = Jsoup.parse(datasetIdentifier);
		    Elements links = d.select("a");
		    url = links.get(0).attr("href");
		    idText = links.get(0).ownText().trim();
	
		} else {
			idText = datasetIdentifier.trim();
			if (idText.startsWith("http://") || idText.startsWith("https://"))
				url = idText;
		}

		IRI identifierClassIri = null;
		int position = idText.indexOf("doi.org/10.");
		if (idText.startsWith("10.")) {
			identifierClassIri = iriMap.lookupClassIri("doi");
			url = "https://doi.org/" + idText;
		} else if (position > 0) {
			identifierClassIri = iriMap.lookupClassIri("doi");
			url = idText;
           	url = url.replace("http:", "https:");
            idText = idText.substring(position + 8);
			System.out.println("New id text is: " + idText + ", url = " + url);
		} else {
			identifierClassIri = iriMap.lookupClassIri("identifier");
		}
		OWLNamedIndividual idInd = createNamedIndividualWithTypeAndLabel(odf, oo, identifierClassIri, 
										iriMap.lookupAnnPropIri("label"), idText);
		addAnnotationToNamedIndividual(idInd, iriMap.lookupAnnPropIri("editor preferred"), editorPreferred, odf, oo);
		if (url != null && url.length()>0) {
			addAnnotationToNamedIndividual(idInd, iriMap.lookupAnnPropIri("hasURL"), url, odf, oo);
		}
		createOWLObjectPropertyAssertion(idInd, iriMap.lookupObjPropIri("denotes"), niMap.get("dataset"), odf, oo);
    }

 
/*
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

    public static void handleLocation(String url, HashMap<String, OWLNamedIndividual> niMap,
					   OWLOntology oo, OWLDataFactory odf, IriLookup iriMap) {
  		OWLNamedIndividual oni = null;
    	String term = "website for " + fullName;

    	if (websiteInds.containsKey(url)) {
    		oni = websiteInds.get(url);
    		addAnnotationToNamedIndividual(oni, iriMap.lookupAnnPropIri("alternative term"), term, odf, oo);
    	} else {
			oni = createNamedIndividualWithTypeAndLabel(odf, oo, iriMap.lookupClassIri("website"), iriMap.lookupAnnPropIri("editor preferred"), 
									term);
			addAnnotationToNamedIndividual(oni, iriMap.lookupAnnPropIri("hasURL"), url, odf, oo);
			websiteInds.put(url, oni);
		}

		niMap.put("website", oni);
	    createOWLObjectPropertyAssertion(oni, iriMap.lookupObjPropIri("is about"), niMap.get("dataset"), odf, oo);
    }

/*
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
*/
    public static void handleDeveloper(String developers, HashMap<String, OWLNamedIndividual> niMap,
				  OWLOntology oo, OWLDataFactory odf, IriLookup iriMap) {
		//OWLNamedIndividual devInd = niMap.get("developer");
		//OWLNamedIndividual wrtInd = niMap.get("codewriting");

    	OWLNamedIndividual createInd = null;
		if (!niMap.containsKey("data set creation")) {
			createInd = createIndividualForDatasetCreation(odf, oo, iriMap, niMap);
		} else {
			createInd = niMap.get("data set creation");
		}

	    String[] devs = developers.split(Pattern.quote(";"));

	    for (int i=0; i<devs.length; i++) {
	    	String label = devs[i].trim();
	    	//ok, the DATS gets crazy at times.  Anonymous?  Really?
	    	if (label.toLowerCase().equals("anonymous")) continue;

	    	String prefTerm = label + ", developer of " + fullName;
	    	OWLNamedIndividual devi = null;
	    	String devKey = label.toLowerCase();
	    	if (devNis.containsKey(devKey)) {
	    		devi = devNis.get(devKey);
	    	} else {
	    		System.err.println("CREATING DEVELOPER: "  + label + " (key = " + devKey + ")");
			 	devi = createNamedIndividualWithTypeAndLabel(odf, oo, iriMap.lookupClassIri("developer"), 
			 				iriMap.lookupAnnPropIri("label"), label);
			 	OWLNamedIndividual lpri = createNamedIndividualWithTypeAndLabel(odf, oo, iriMap.lookupClassIri("legalpersonrole"), 
			 								iriMap.lookupAnnPropIri("label"), "legal person role of " + label);
				createOWLObjectPropertyAssertion(devi, iriMap.lookupObjPropIri("bearer"), lpri, odf, oo);
				devNis.put(devKey, devi);
	    	}
	    	addAnnotationToNamedIndividual(devi, iriMap.lookupAnnPropIri("editor preferred"), prefTerm, odf, oo);
			createOWLObjectPropertyAssertion(createInd, iriMap.lookupObjPropIri("has active participant"), devi, odf, oo);
		}
		//System.out.println(i);
	    
    }

    public static OWLNamedIndividual createIndividualForDatasetCreation(OWLDataFactory odf, OWLOntology oo, IriLookup iriMap, 
    						HashMap<String, OWLNamedIndividual> niMap) {
    	OWLNamedIndividual createInd = createNamedIndividualWithTypeAndLabel(odf, oo, iriMap.lookupClassIri("data set creation"),
											iriMap.lookupAnnPropIri("label"), "process of creating " + fullName);
		niMap.put("data set creation", createInd);
		createOWLObjectPropertyAssertion(createInd, iriMap.lookupObjPropIri("has specified output"), niMap.get("dataset"), odf, oo);
		return createInd;
    }

    public static void handleCreationDate(String date, HashMap<String, OWLNamedIndividual> niMap, 
    				OWLOntology oo, OWLDataFactory odf, IriLookup iriMap) {
    	//create the dataset creation process if it doesn't exist
		OWLNamedIndividual createInd = null;
		if (!niMap.containsKey("data set creation")) {
			createInd = createIndividualForDatasetCreation(odf, oo, iriMap, niMap);
		} else {
			createInd = niMap.get("data set creation");
		}
		
		//create the time interval over which the dataset creation process occurred
		OWLNamedIndividual createIntervalInd = createNamedIndividualWithTypeAndLabel(odf, oo, iriMap.lookupClassIri("temporal interval"),
								iriMap.lookupAnnPropIri("editor preferred"), "interval over which " + fullName + " was created");
		createOWLObjectPropertyAssertion(createInd, iriMap.lookupObjPropIri("occupies temporal region"), createIntervalInd, odf, oo);
		//create the date in created variable, IF IT DOESN'T EXIST.  Question: as you just did for RTS, 
		//  do you want to generate IRIs based on ISO 8601
		OWLNamedIndividual dateInd = null;
		if (!dateNis.containsKey(date)) {
			dateInd = createNamedIndividualWithTypeAndLabel(odf, oo, iriMap.lookupClassIri("temporal interval"), 
							iriMap.lookupAnnPropIri("label"), date);
			dateNis.put(date,dateInd);
		} else {
			dateInd = dateNis.get(date);
		}
		//connect interval to creation date via ends during OP (i.e., the creation process ends at some 
		//   point during the day given by the creation date)
		createOWLObjectPropertyAssertion(createIntervalInd, iriMap.lookupObjPropIri("ends during"), dateInd, odf, oo);
    }
/*
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
				System.out.println("PUB USED: " + pub.ownText());
			    }
			} else {
			    String[] pubs = value.split(Pattern.quote(";"));
			    //System.out.println("value = " + value);
			    //System.out.println("THERE ARE " + pubs.length + " pubs that used release.");
			    for (int i=0; i<pubs.length; i++) {
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


		    for (int i=1; i<pubInfo.size(); i++) {
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
*/
/*
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
		} else {
		    throw new IllegalArgumentException("value of availableAt must be array");
		}
    }

    public static void	handleControlMeasures(Map.Entry<String, JsonElement> e, HashMap<String, OWLNamedIndividual> niMap,
				  OWLOntology oo, OWLDataFactory odf, IriLookup iriMap) {
		JsonElement je = e.getValue();
		if (je instanceof JsonArray) {
		    JsonArray elemArray = (JsonArray)je;
		    Iterator<JsonElement> elemIter = elemArray.iterator();
		    int size = elemArray.size();
		    while (elemIter.hasNext()) {
			JsonElement elemi = elemIter.next();
			if (elemi instanceof JsonPrimitive) {
			    String value = ((JsonPrimitive)elemi).getAsString();
			    uniqueCms.add(value);

			    if (cmMap.containsKey(value)) {
				IRI classIri = cmMap.get(value);
				String cmInstanceLabel = value + " control measure by " + fullName;
				String simxInstanceLabel = "simulating of epidemic with " + value + " control measure " + " by " + fullName;
				OWLNamedIndividual simxInd = createNamedIndividualWithTypeAndLabel(odf, oo, iriMap.lookupClassIri("simulatingx"), iriMap.lookupAnnPropIri("editor preferred"), simxInstanceLabel);
				OWLNamedIndividual cmInd = createNamedIndividualWithTypeAndLabel(odf, oo, classIri, iriMap.lookupAnnPropIri("editor preferred"), cmInstanceLabel);
				
				createOWLObjectPropertyAssertion(simxInd, iriMap.lookupObjPropIri("achieves objective"), niMap.get("executable"), odf, oo);
				createOWLObjectPropertyAssertion(simxInd, iriMap.lookupObjPropIri("has specified output"), cmInd, odf, oo);
			    } else {
				System.out.println("Skipping " + value + " control measure.");
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
		    JsonArray elemArray = (JsonArray)je;
		    Iterator<JsonElement> elemIter = elemArray.iterator();
		    int size = elemArray.size();
		    while (elemIter.hasNext()) {
			JsonElement elemi = elemIter.next();
			if (elemi instanceof JsonPrimitive) {
			    String value = ((JsonPrimitive)elemi).getAsString();
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
			    }
			}
		    }
		} else {
		    throw new IllegalArgumentException("value of controlMeasures attribute must be array");
		}
    }
*/
    public static void handleDataFormats(String value, String subtype, HashMap<String, OWLNamedIndividual> niMap,
				  OWLOntology oo, OWLDataFactory odf, IriLookup iriMap) {
	
    		String[] formatInfo = value.split(Pattern.quote(";"));
			OWLNamedIndividual formatInd = null;
			for (String format : formatInfo) {
				format = format.trim();
				if (formatInds.containsKey(format)) {
					formatInd = formatInds.get(format);
					System.out.println("found format by string: " + format);
					break;
				}
			}    		

		    uniqueFormats.add(value);

		    if (formatInd == null && (value.equals("Apollo XSD") || formatInfo[0].equals("Apollo XSD")
		    	  || (formatInfo.length > 1 && formatInfo[1].equals("Apollo XSD")))) {
		    	if (subtype.equals("case series data")) {
		    		formatInd = formatInds.get("APOLLO:CaseSeries-v4.0.1");
		    		System.out.println("format is listed as 'Apollo XSD' and dataset type is case series, so using " +
		    			"APOLLO:CaseSeries-v4.0.1");
		    	} else if (subtype.equals("epidemic data set")) {
		    		formatInd = formatInds.get("APOLLO:Epidemic-v4.0.1");
		    		System.out.println("format is listed as 'Apollo XSD' and dataset type is epidemic, so using " +
		    			"APOLLO:Epidemic-v4.0.1");
		    	} else if (subtype.equals("infectious disease scenario")) {
					formatInd = formatInds.get("APOLLO:InfectiousDiseaseScenario-v4.0.1");
					System.out.println("format is listed as 'Apollo XSD' and dataset type is infectious disease scenario, so using " +
		    			"APOLLO:InfectiousDiseaseScenario-v4.0.1");
		    	} else {
		    		System.err.println("Format is " + value + ", and subtype is " + subtype);
		    	}
		    }
		    //although the entry for format might not be complete garbage, it still might not be something
		    // that we processed in DataFormatProcessor.java
		    if (formatInd != null) {
		    	// if dataset creating process not yet available, create and store it
		    	OWLNamedIndividual createInd = null;
		    	if (!niMap.containsKey("data set creation")) {
					createInd = createIndividualForDatasetCreation(odf, oo, iriMap, niMap);
		    	} else {
		    		createInd = niMap.get("data set creation");
		    	}
				/* connect data creating process to the format.  Specifically, if data are conformant with a particular 
					specification, then it's because there was a planned process (the data creation process) achieved
					the objective of the specification, itself a plan for creating data in a certain structure/semantics
					*/
				createOWLObjectPropertyAssertion(createInd, iriMap.lookupObjPropIri("achieves objective"), formatInd, odf, oo);
			} else {
		    	System.err.println("WARNING!  Ignoring format: " + value);
		    }
    }


    public static void handleLicense(String licenseName, HashMap<String, OWLNamedIndividual> niMap, 
    					OWLOntology oo, OWLDataFactory odf, IriLookup iriMap) {
		if (licenseNis.containsKey(licenseName)) {
			OWLNamedIndividual license = licenseNis.get(licenseName);
			OWLNamedIndividual format = niMap.get("dataset");
			createOWLObjectPropertyAssertion(license, iriMap.lookupObjPropIri("is part of"), format, odf, oo);
		} else {
			System.err.println("SKIPPING LICENSE: " + licenseName);
		}
    }

    public static void processAnyIndexingTerms(String popIriTxt, String beIriTxt, String ecIriTxt, String epiIriTxt, 
    		OWLNamedIndividual dataset, IriLookup iriMap, OWLDataFactory odf, OWLOntology oo) {
    	//only process the populations if there's no epidemic IRIs.  Else the epidemic IRIs take care of the populations for us.
    	if (popIriTxt != null && (epiIriTxt == null || epiIriTxt.length() == 0)) {
    		processIndexing(popIriTxt, dataset, iriMap, odf, oo);
    	} 
    	if (beIriTxt != null) {
    		processIndexing(beIriTxt, dataset, iriMap, odf, oo);
    	}
    	if (ecIriTxt != null) {
    		processIndexing(ecIriTxt, dataset, iriMap, odf, oo);
    	}
    	if (epiIriTxt != null) {
    		processIndexing(epiIriTxt, dataset, iriMap, odf, oo);
    	}
    }

    public static void processIndexing(String iriList, OWLNamedIndividual dataset, IriLookup iriMap,
    					 OWLDataFactory odf, OWLOntology oo) {
    	String[] iris = iriList.split(Pattern.quote(";"));
    	for (String iri : iris) {
    		iri = iri.trim();
    		if (iri.length() == 0) continue;
    		//System.out.println("IRI IS " + iri);
    		OWLNamedIndividual aboutInd = odf.getOWLNamedIndividual(IRI.create(iri));
    		createOWLObjectPropertyAssertion(dataset, iriMap.lookupObjPropIri("is about"), aboutInd, odf, oo);
    	}
    }

    /*
    public static void handlePublicationsAbout(Map.Entry<String, JsonElement> e, HashMap<String, OWLNamedIndividual> niMap,
				  OWLOntology oo, OWLDataFactory odf, IriLookup iriMap) {
		JsonElement je = e.getValue();
		if (je instanceof JsonArray) {
		    JsonArray elemArray = (JsonArray)je;
		    Iterator<JsonElement> elemIter = elemArray.iterator();
		    int size = elemArray.size();
		    while (elemIter.hasNext()) {
			JsonElement elemi = elemIter.next();
			if (elemi instanceof JsonPrimitive) {
			    String value = ((JsonPrimitive)elemi).getAsString();
			    System.out.println("PUB ABOUT " + value);
			    IRI pubIri = pubLinks.get(value);
			    System.out.println("PUB ABOUT IRI = " + pubIri);
			    if (pubIri!=null) {
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
    */


    public static OWLNamedIndividual createNamedIndividualWithTypeAndLabel(
			   OWLDataFactory odf, OWLOntology oo, IRI classTypeIri, IRI labelPropIri, String rdfsLabel) {
		OWLNamedIndividual oni = odf.getOWLNamedIndividual(nextIri());
		OWLClassAssertionAxiom ocaa = odf.getOWLClassAssertionAxiom(odf.getOWLClass(classTypeIri), oni);
		oom.addAxiom(oo,ocaa);
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
		int numZero = 10-counterTxt.length();
		for (int i=0; i<numZero; i++) 
		    sb.append("0");
		sb.append(counterTxt);
		return IRI.create(new String(sb));
    }

    public static void loadAndCreateDataFormatIndividuals(OWLDataFactory odf, OWLOntology oo, IriLookup iriMap) throws IOException {
		formatInds = new HashMap<String, OWLNamedIndividual>();
		FileReader fr = new FileReader(p.getProperty("format_info"));
		LineNumberReader lnr = new LineNumberReader(fr);
		String line;
		while ((line=lnr.readLine())!=null) {
		    String[] flds = line.split(Pattern.quote("\t"), -1);
		    String iriTxt = flds[0];
		    String label = flds[1];
		    String[] keys = flds[14].split(Pattern.quote(";"));

		    OWLNamedIndividual formatInd = odf.getOWLNamedIndividual(IRI.create(iriTxt));
		   
		    for (String key : keys) {
				formatInds.put(key, formatInd);
				 System.out.println("Data format key = '" + key + "'");
		    }
		}
		lnr.close();
		fr.close();
    }

    public static void loadLicenses(String fName, OWLDataFactory odf) throws IOException {
    	licenseNis = new HashMap<String, OWLNamedIndividual>();
    	FileReader fr =  new FileReader(fName);
    	LineNumberReader lnr = new LineNumberReader(fr);
    	String line;
    	while((line=lnr.readLine())!=null) {
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

    public static void loadWebsites(String fName, OWLDataFactory odf) throws IOException {
    	websiteInds = new HashMap<String, OWLNamedIndividual>();
    	FileReader fr =  new FileReader(fName);
    	LineNumberReader lnr = new LineNumberReader(fr);
    	String line;
    	while((line=lnr.readLine())!=null) {
    		String[] flds = line.split(Pattern.quote("\t"));
    		OWLNamedIndividual website = odf.getOWLNamedIndividual(IRI.create(flds[1]));
    		websiteInds.put(flds[0], website);
    	}
    	lnr.close();
    	fr.close();
    }

}
