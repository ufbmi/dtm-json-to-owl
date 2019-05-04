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

public class DataFormatProcessor {
    static long iriCounter = 1200007200L;
    static String iriPrefix = "http://www.pitt.edu/obc/IDE_ARTICLE_";
    static int iriLen = 10;

    static OWLOntologyManager oom;

    static String baseName;
    static String[] versionNames;
    static String[] fullNames;

    static String fullName;

    static OWLNamedIndividual olympus;

    static HashSet<String> uniqueFormats;

    static HashMap<String, OWLNamedIndividual> formatInds;
    static HashMap<String, OWLNamedIndividual> devNis;
    static HashMap<String, OWLNamedIndividual> dateNis;
    static HashMap<String, OWLNamedIndividual> licenseNis;
    static HashMap<String, OWLNamedIndividual> websiteInds;

    static Properties p;

    public static void main(String[] args) {
    	FileReader fr = null;
    	LineNumberReader lnr = null;
    	FileOutputStream fos = null;
    	FileWriter fw = null;

		try ( FileReader pr = new FileReader(args[0]); ) {

			p = new Properties();
			p.load(pr);

			//fr = new FileReader("./src/main/resources/data_format_metadata-2017-05-25T1630.txt"
			fr = new FileReader(p.getProperty("format_info"));
			lnr = new LineNumberReader(fr);

		    IriLookup iriMap = new IriLookup(p.getProperty("iris"));
		    iriMap.init();

		    oom = OWLManager.createOWLOntologyManager();
		    OWLDataFactory odf = OWLManager.getOWLDataFactory();
		    OWLOntology oo = null;
		    IRI ontologyIRI = IRI.create("http://www.pitt.edu/mdc/dataformat");
		    try {
				oo = oom.createOntology(ontologyIRI);
		    } catch (OWLOntologyCreationException ooce) {
				ooce.printStackTrace();
		    }

	        //devNis = new HashMap<String, OWLNamedIndividual>();
	        //loadDevelopers("./src/main/resources/developer_iris-2017-05-10.txt", odf);
			dateNis = new HashMap<String, OWLNamedIndividual>();
			String websiteFname = p.getProperty("website_ind");
	        websiteFname = websiteFname.replace("<date>", args[1].trim()).trim();
		 	loadWebsites(websiteFname, odf);
			loadLicenses(p.getProperty("license_info"), odf);
		 	 
		 	String line;
		    while((line=lnr.readLine())!=null) {
				String[] flds = line.split(Pattern.quote("\t"), -1);
				System.out.println(flds + " fields: " + line);
				//System.out.println("line " + lnr.getLineNumber() + " has " + flds.length + " fields.");

				/* IRI	name	identifier	identifier_source	type	type_IRI	
						description	licenses	version	title	Referecned via
				*/

				String formatIriTxt = flds[0].trim();
				String name = flds[1].trim();
				String formatIdTxt = flds[2].trim();
				String formatIdSource = flds[3].trim();
				String formatType = flds[4].trim();
				String formatTypeIriTxt = flds[5].trim();
				String formatDescription = flds[6].trim();
				String formatLicense = flds[7].trim();
				String formatVersionIdTxt = flds[8].trim();
				String formatHumanReadableSpecValue = flds[9].trim();
				String formatHumanReadableSpecIri = flds[10].trim();
				String formatMachineReadableSpecValue = flds[11].trim();
				String formatMachineReadableSpecIri = flds[12].trim();
				String formatTitle = flds[13].trim();
				String formatIndexingTerms = flds[14].trim();
				String formatIsInMdcTxt = flds[16].trim();
				String formatDoi = flds[17].trim();
				
		    	//We'll create them as agent level ecosystem data sets, case series, etc.
			    System.out.println("SUBTYPE.  subtype=\"" + formatType + "\"");
			    
			    //Need to add dataset types to iris.txt
			    IRI classIri = iriMap.lookupClassIri("dataformat");
			    if (classIri != null) {

					System.out.println("\t"+ formatTitle + " is a data format");

					//Matt's parsing of DATS slipped in a string value of "null", so that caused weirdness
					if (!isValidFieldValue(formatVersionIdTxt)) formatVersionIdTxt = "";		
					//System.out.println("base name = " + baseName + ", version = " + version);
					String versionSuffix = (formatVersionIdTxt == null || formatVersionIdTxt.length() == 0) ? "" : " - " + 
				    	((Character.isDigit(formatVersionIdTxt.charAt(0))) ? "v" + formatVersionIdTxt : formatVersionIdTxt);
					
					baseName = name + versionSuffix;
					fullName = formatTitle + versionSuffix + ", data format";
					System.out.println("\t" + fullName);
					
					IRI edPrefIri = iriMap.lookupAnnPropIri("editor preferred");
					IRI labelIri = iriMap.lookupAnnPropIri("label");
					IRI titleIri = iriMap.lookupAnnPropIri("title");
					OWLNamedIndividual format = createNamedIndividualWithIriTypeAndLabel(IRI.create(formatIriTxt),
													odf, oo, classIri, edPrefIri, fullName);
					addAnnotationToNamedIndividual(format, labelIri, baseName, odf, oo);

					OWLNamedIndividual mdcInd = odf.getOWLNamedIndividual(iriMap.lookupIndividIri("mdc"));
					/*
						Add format to MDC if it's marked "true" as being a part of MDC.

						This step is necessary because the Pitt group sunsetted the EpiCase Format.  WE assigned it an IRI,
						  so we won't delete it, but we don't want to count it in the FAIR-o-meter, so we qualify all
						  the FAIR-o-meter queries based on whether things are in MDC.
					*/
					boolean isMdc = Boolean.parseBoolean(formatIsInMdcTxt);
					if (isMdc) {
						createOWLObjectPropertyAssertion(mdcInd, iriMap.lookupObjPropIri("has proper part"), format, odf, oo);
					}

					if (isValidFieldValue(formatTitle)) {
						addAnnotationToNamedIndividual(format, titleIri, formatTitle, odf, oo);
					}

					HashMap<String, OWLNamedIndividual> niMap = new HashMap<String, OWLNamedIndividual>();
					niMap.put("format", format);
					
			    	if (isValidFieldValue(formatIdTxt)) {
						handleIdentifier(formatIdTxt, niMap, oo, odf, iriMap);
			    	} 

			    	if (isValidFieldValue(formatDescription)) {
			    		addAnnotationToNamedIndividual(format, iriMap.lookupAnnPropIri("description"), 
			    				formatDescription, odf, oo);
			    	}

			    	if (isValidFieldValue(formatLicense)) {
			    		handleLicense(formatLicense, niMap, oo, odf, iriMap);
			    	}

			    	if (isValidFieldValue(formatVersionIdTxt)) {
			    		handleVersionId(formatVersionIdTxt, niMap, oo, odf, iriMap);
			    	}

			    	if (isValidFieldValue(formatHumanReadableSpecValue)) {
			    		handleHumanReadableSpec(formatHumanReadableSpecValue, niMap, oo, odf, iriMap);
			    	}

			    	if (isValidFieldValue(formatMachineReadableSpecIri)) {
			    		handleMachineReadableSpec(formatMachineReadableSpecIri, niMap, oo, odf, iriMap);
			    	}

			    	if (isValidFieldValue(formatDoi)) {
			    		handleDigitalObjectIdentifier(formatDoi, niMap, oo, odf, iriMap);
			    	}

			    	/* There's no authors attribute for data formats at present
			    	if (isValidFieldValue(authors)) {
			    		addAnnotationToNamedIndividual(dataset, iriMap.lookupAnnPropIri("authors"), authors, odf, oo);
						handleDeveloper(authors, niMap, oo, odf, iriMap);
			    	} 
			    	*/

			    /* There's no created, modified, or curated for data formats at the present time
			    	if (isValidFieldValue(created)) {
			    		/*for backwards compatibility with existing OBC.ide, make the dc:date of the data set, the creation 
			    		  date
						
						addAnnotationToNamedIndividual(dataset, iriMap.lookupAnnPropIri("data set date"), created, odf, oo);
			    		handleCreationDate(created, niMap, oo, odf, iriMap);
			    	}

			    	if (isValidFieldValue(modified)) {
			    		/* we're going to need a class "process of modifying a data set".  Then follow pretty much the same
			    			outline as above.  Except there's no direct annotation on the dataset for backwards compatibility.
			    			
			    		
			    	}

			    	if (isValidFieldValue(curated)) {
			    		/* key question.  what's the difference betweeen modifying and curating (and maybe even creating)?
			    			But, assuming we can define a real difference, then follows the same pattern as modifying.
			    		
			    		
			    	}
			    	

			    	if (isValidFieldValue(landingPage)) {
						handleLocation(landingPage, niMap, oo, odf, iriMap);
			    	}

			    	if (isValidFieldValue(accessPage)) {
			    		IRI urlIri = iriMap.lookupAnnPropIri("hasURL");
			    		addAnnotationToNamedIndividual(dataset, urlIri, accessPage, odf, oo);
			    	}

			    	if (isValidFieldValue(format)) {
						handleDataFormats(format, niMap, oo, odf, iriMap);
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
					
					processAnyIndexingTerms(popIriTxt, beIriTxt, ecIriTxt, epiIriTxt, dataset, iriMap, odf, oo);
				*/

				}
			}

			

			try {
				SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
				String dateTxt = df.format(new Date());
				String owlFileName = "./data-format-ontology-" + dateTxt + ".owl";
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

    public static void loadDevelopers(String fName, OWLDataFactory odf) throws IOException {
    	FileReader fr = new FileReader(fName);
    	LineNumberReader lnr = new LineNumberReader(fr);
    	String line;
    	while ((line=lnr.readLine())!=null) {
    		String[] flds = line.split(Pattern.quote("\t"));
    		OWLNamedIndividual devInd = odf.getOWLNamedIndividual(IRI.create(flds[1]));
    		devNis.put(flds[0], devInd);
    	}
    	lnr.close();
    	fr.close();
    }

    public static boolean isValidFieldValue(String value) {
    	return (value !=null && !value.equals("null") && value.length()>0 && !value.toLowerCase().equals("n/a")
    				&& !value.startsWith("?") && !value.toLowerCase().equals("under development") 
    				&& !value.toLowerCase().equals("identifier will be created at time of release"));
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
    */

    public static void handleIdentifier(String datasetIdentifier, HashMap<String, OWLNamedIndividual> niMap,
					   OWLOntology oo, OWLDataFactory odf, IriLookup iriMap) {

    	String url = null, idText = null;
    
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

		IRI identifierClassIri = (idText.startsWith("10.")) ? 
									iriMap.lookupClassIri("doi") : iriMap.lookupClassIri("identifier");
		OWLNamedIndividual idInd = createNamedIndividualWithTypeAndLabel(odf, oo, identifierClassIri, 
										iriMap.lookupAnnPropIri("label"), idText);
		niMap.put("format identifier", idInd);
		if (url != null && url.length()>0) {
			addAnnotationToNamedIndividual(idInd, iriMap.lookupAnnPropIri("hasURL"), url, odf, oo);
		}
		createOWLObjectPropertyAssertion(idInd, iriMap.lookupObjPropIri("denotes"), niMap.get("format"), odf, oo);
    }

    public static void handleDigitalObjectIdentifier(String idText, HashMap<String, OWLNamedIndividual> niMap,
					   OWLOntology oo, OWLDataFactory odf, IriLookup iriMap) {
    	String url = "http://dx.doi.org/" + idText;
    	IRI identifierClassIri = (idText.startsWith("10.")) ? 
									iriMap.lookupClassIri("doi") : iriMap.lookupClassIri("identifier");
		OWLNamedIndividual idInd = createNamedIndividualWithTypeAndLabel(odf, oo, identifierClassIri, 
										iriMap.lookupAnnPropIri("label"), idText);
		niMap.put("format doi", idInd);
		if (url != null && url.length()>0) {
			addAnnotationToNamedIndividual(idInd, iriMap.lookupAnnPropIri("hasURL"), url, odf, oo);
		}
		createOWLObjectPropertyAssertion(idInd, iriMap.lookupObjPropIri("denotes"), niMap.get("format"), odf, oo);
    }

    /*
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
    */

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
	    createOWLObjectPropertyAssertion(oni, iriMap.lookupObjPropIri("is about"), niMap.get("format"), odf, oo);
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

    public static void handleVersionId(String versionIdTxt, HashMap<String, OWLNamedIndividual> niMap,
					   OWLOntology oo, OWLDataFactory odf, IriLookup iriMap) {
		OWLNamedIndividual oni = createNamedIndividualWithTypeAndLabel(odf, oo, iriMap.lookupClassIri("version number"),
									iriMap.lookupAnnPropIri("label"), versionIdTxt);
		niMap.put("versionid",oni);
		createOWLObjectPropertyAssertion(oni, iriMap.lookupObjPropIri("denotes"), niMap.get("format"), odf, oo);
    }


    public static void handleLicense(String formatLicense, HashMap<String, OWLNamedIndividual> niMap, 
    					OWLOntology oo, OWLDataFactory odf, IriLookup iriMap) {

		if (licenseNis.containsKey(formatLicense)) {
			OWLNamedIndividual license = licenseNis.get(formatLicense);
			OWLNamedIndividual format = niMap.get("format");
			createOWLObjectPropertyAssertion(license, iriMap.lookupObjPropIri("is part of"), format, odf, oo);
		} else {
			System.err.println("SKIPPING LICENSE: " + formatLicense);
		}
    }

    public static void handleDeveloper(String developers, HashMap<String, OWLNamedIndividual> niMap,
				  OWLOntology oo, OWLDataFactory odf, IriLookup iriMap) {
		//OWLNamedIndividual devInd = niMap.get("developer");
		//OWLNamedIndividual wrtInd = niMap.get("codewriting");

    	OWLNamedIndividual createInd = null;
		if (!niMap.containsKey("data set creation")) {
			createInd = createNamedIndividualWithTypeAndLabel(odf, oo, iriMap.lookupClassIri("data set creation"),
							iriMap.lookupAnnPropIri("label"), "process of creating " + fullName);
			niMap.put("data set creation", createInd);
		} else {
			createInd = niMap.get("data set creation");
		}

	    String[] devs = developers.split(Pattern.quote(","));

	    for (int i=0; i<devs.length; i++) {
	    	String label = devs[i].trim();
	    	String prefTerm = label + ", developer of " + fullName;
	    	OWLNamedIndividual devi = null;
	    	if (devNis.containsKey(devs[i])) {
	    		devi = devNis.get(devs[i]);
	    	} else {
			 	devi = createNamedIndividualWithTypeAndLabel(odf, oo, iriMap.lookupClassIri("developer"), 
			 				iriMap.lookupAnnPropIri("label"), devs[i]);
			 	OWLNamedIndividual lpri = createNamedIndividualWithTypeAndLabel(odf, oo, iriMap.lookupClassIri("legalpersonrole"), 
			 								iriMap.lookupAnnPropIri("label"), "legal person role of " + devs[i]);
				createOWLObjectPropertyAssertion(devi, iriMap.lookupObjPropIri("bearer"), lpri, odf, oo);
				devNis.put(devs[i], devi);
	    	}
	    	addAnnotationToNamedIndividual(devi, iriMap.lookupAnnPropIri("editor preferred"), prefTerm, odf, oo);
			createOWLObjectPropertyAssertion(createInd, iriMap.lookupObjPropIri("has active participant"), devi, odf, oo);
		}
		//System.out.println(i);
	    
    }

    public static void handleCreationDate(String date, HashMap<String, OWLNamedIndividual> niMap, 
    				OWLOntology oo, OWLDataFactory odf, IriLookup iriMap) {
    	//create the dataset creation process if it doesn't exist
		OWLNamedIndividual createInd = null;
		if (!niMap.containsKey("data set creation")) {
			createInd = createNamedIndividualWithTypeAndLabel(odf, oo, iriMap.lookupClassIri("data set creation"),
							iriMap.lookupAnnPropIri("label"), "process of creating " + fullName);
			niMap.put("data set creation", createInd);
		} else {
			createInd = niMap.get("data set creation");
		}
		//connect the creation process to the dataset
		createOWLObjectPropertyAssertion(createInd, iriMap.lookupObjPropIri("has specified output"), niMap.get("dataset"), odf, oo);
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

    public static void handleDataFormats(String value, HashMap<String, OWLNamedIndividual> niMap,
				  OWLOntology oo, OWLDataFactory odf, IriLookup iriMap) {
	
		    uniqueFormats.add(value);

		    // if dataset creating process not available, create and store it
		    OWLNamedIndividual createInd = null;
		    if (!niMap.containsKey("data set creation")) {
				createInd = createNamedIndividualWithTypeAndLabel(odf, oo, iriMap.lookupClassIri("data set creation"),
							iriMap.lookupAnnPropIri("label"), "process of creating " + fullName);
				niMap.put("data set creation", createInd);
		    } else {
		    	createInd = niMap.get("data set creation");
		    }

		    OWLNamedIndividual formatInd = formatInds.get(value);
		    if (formatInd != null) {
				/* connect data creating process to the format.  Specifically, if data are conformant with a particular 
					specification, then it's because there was a planned process (the data creation process) achieved
					the objective of the specification, itself a plan for creating data in a certain structure/semantics
					*/
				createOWLObjectPropertyAssertion(createInd, iriMap.lookupObjPropIri("achieves objective"), formatInd, odf, oo);

			} else {
		    	System.err.println("Ignoring format: " + value);
		    }
    }

    public static void processAnyIndexingTerms(String popIriTxt, String beIriTxt, String ecIriTxt, String epiIriTxt, 
    		OWLNamedIndividual dataset, IriLookup iriMap, OWLDataFactory odf, OWLOntology oo) {
    	if (popIriTxt != null) {
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
    		if (iri.length() == 0) continue;
    		System.out.println("IRI IS " + iri);
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

	public static void handleHumanReadableSpec(String formatHumanReadableSpecValue, 
						HashMap<String, OWLNamedIndividual> niMap, OWLOntology oo, 
						OWLDataFactory odf, IriLookup iriMap) {
		//make it a documentation that is about the format: if it's URL, add it as hasURL annotation, else label
		IRI annPropIri = (formatHumanReadableSpecValue.startsWith("http")) ?
							iriMap.lookupAnnPropIri("hasURL") : iriMap.lookupAnnPropIri("label");
		OWLNamedIndividual oni = createNamedIndividualWithTypeAndLabel(odf, oo, iriMap.lookupClassIri("documentation"),
									annPropIri, formatHumanReadableSpecValue);
		addAnnotationToNamedIndividual(oni, iriMap.lookupAnnPropIri("editor preferred"), "human readable specification for " + 
										fullName, odf, oo);
		OWLNamedIndividual format = niMap.get("format");
		//if (oni == null) System.err.println("oni is null.");
		//if (iriMap.lookupAnnPropIri("is about") == null) System.err.println("is about is null.");
		//if (format == null) System.err.println("format is null.");
		createOWLObjectPropertyAssertion(oni, iriMap.lookupObjPropIri("is about"), niMap.get("format"), odf, oo);
	}
			    	
	public static void handleMachineReadableSpec(String formatMachineReadableSpecIri, 
						HashMap<String, OWLNamedIndividual> niMap, OWLOntology oo, 
						OWLDataFactory odf, IriLookup iriMap) {
		//add it as URL on format individual itself
		addAnnotationToNamedIndividual(niMap.get("format"), iriMap.lookupAnnPropIri("hasURL"), formatMachineReadableSpecIri, odf, oo);
	}

    public static OWLNamedIndividual createNamedIndividualWithTypeAndLabel(
			   OWLDataFactory odf, OWLOntology oo, IRI classTypeIri, IRI labelPropIri, String rdfsLabel) {
		OWLNamedIndividual oni = odf.getOWLNamedIndividual(nextIri());
		OWLClassAssertionAxiom ocaa = odf.getOWLClassAssertionAxiom(odf.getOWLClass(classTypeIri), oni);
		oom.addAxiom(oo,ocaa);
		addAnnotationToNamedIndividual(oni, labelPropIri, rdfsLabel, odf, oo);

		return oni;
    }

    public static OWLNamedIndividual createNamedIndividualWithIriTypeAndLabel(
									      IRI individualIri, OWLDataFactory odf, OWLOntology oo, IRI classTypeIri, IRI labelPropIri, String rdfsLabel) {
		OWLNamedIndividual oni = odf.getOWLNamedIndividual(individualIri);
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

/*
    public static void loadAndCreateDataFormatIndividuals(OWLDataFactory odf, OWLOntology oo, IriLookup iriMap) throws IOException {
		formatInds = new HashMap<String, OWLNamedIndividual>();
		FileReader fr = new FileReader("./src/main/resources/format-individuals-to-create.txt");
		LineNumberReader lnr = new LineNumberReader(fr);
		String line;
		while ((line=lnr.readLine())!=null) {
		    String[] flds = line.split(Pattern.quote("\t"));
		    String label = flds[0];
		    String prefTerm = flds[1];
		    String[] keys = flds[2].split(Pattern.quote(";"));

		    OWLNamedIndividual formatInd = createNamedIndividualWithTypeAndLabel(odf, oo, iriMap.lookupClassIri("dataformat"), iriMap.lookupAnnPropIri("editor preferred"), prefTerm);
		    addAnnotationToNamedIndividual(formatInd, iriMap.lookupAnnPropIri("label"), label, odf, oo);

		    for (String key : keys) {
			formatInds.put(key, formatInd);
		    }
		}
    }
*/
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
