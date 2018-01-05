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

public class SoftwareLicenseProcessor {
    static long iriCounter = 1200006900L;
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

		   fr = new FileReader(p.getProperty("license_info"));
		   lnr = new LineNumberReader(fr);

		    IriLookup iriMap = new IriLookup(p.getProperty("iris"));
		    iriMap.init();

		    oom = OWLManager.createOWLOntologyManager();
		    OWLDataFactory odf = OWLManager.getOWLDataFactory();
		    OWLOntology oo = null;
		    IRI ontologyIRI = IRI.create("http://www.pitt.edu/mdc/software_license");
		    try {
				oo = oom.createOntology(ontologyIRI);
		    } catch (OWLOntologyCreationException ooce) {
				ooce.printStackTrace();
		    }

		    dateNis = new HashMap<String, OWLNamedIndividual>();
		    websiteInds = new HashMap<String, OWLNamedIndividual>();

		    createMidasDigitalCommonsIndividuals(odf, oo, iriMap);

		   	String line;
		    while((line=lnr.readLine())!=null) {
				String[] flds = line.split(Pattern.quote("\t"), -1);
				
				String iriTxt = flds[0].trim();
				String title = flds[1].trim();
				String version = flds[2].trim();
				String created = flds[3].trim();
				String accessPage = flds[4].trim();
				String indexTerms = flds[5].trim();
				String classIriTxt = flds[6].trim();

		    			    
			    //Need to add dataset types to iris.txt
			    IRI classIri = IRI.create(classIriTxt);
			    if (classIri != null) {

					System.out.println("\t"+ title + " is a license");
					baseName = title;
					fullName = title;
					versionSuffix = (version != null && version.length()>0) ? version : "";
							
					//System.out.println("base name = " + baseName + ", version = " + version);
					String baseLabel = baseName + " " + versionSuffix;
					fullName = baseLabel + ", software license";
					System.out.println("\t\t" + title + "\n\t\t" + baseLabel + "\n\t\t" + fullName);
					
					IRI edPrefIri = iriMap.lookupAnnPropIri("editor preferred");
					IRI labelIri = iriMap.lookupAnnPropIri("label");
					IRI titleIri = iriMap.lookupAnnPropIri("title");
					OWLNamedIndividual license = createNamedIndividualWithIriTypeAndLabel(
						IRI.create(iriTxt), odf, oo, classIri, edPrefIri, fullName);
					addAnnotationToIndividual(license, titleIri, title, odf, oo);
					addAnnotationToIndividual(license, labelIri, baseLabel, odf, oo);
					addAnnotationToIndividual(license, edPrefIri, fullName, odf, oo);
				
					HashMap<String, OWLNamedIndividual> niMap = new HashMap<String, OWLNamedIndividual>();
					niMap.put("license", license);

					OWLNamedIndividual mdcInd = odf.getOWLNamedIndividual(iriMap.lookupIndividIri("mdc"));
					/*
						Add all licenses to MDC.
					*/
					createOWLObjectPropertyAssertion(mdcInd, iriMap.lookupObjPropIri("has proper part"), license, odf, oo);
					
			    	if (isValidFieldValue(created)) {
			    		/*for backwards compatibility with existing OBC.ide, make the dc:date of the data set, the creation 
			    		  date
						*/
						addAnnotationToIndividual(license, iriMap.lookupAnnPropIri("license date"), created, odf, oo);
			    		handleCreationDate(created, niMap, oo, odf, iriMap);
			    	}

			    	if (isValidFieldValue(version)) {
			    		handleVersionId(version, niMap, oo, odf, iriMap);
			    	}

			    	if (isValidFieldValue(accessPage)) {
			    		IRI urlIri = iriMap.lookupAnnPropIri("hasURL");
			    		addAnnotationToIndividual(license, urlIri, accessPage, odf, oo);
			    	}

			    	if (isValidFieldValue(indexTerms)) {
			    		handleIndexTerms(license, indexTerms, iriMap, odf, oo);
			    	}
				}
			} //while((line=lnr.readLine()))

			
			
			try {
				SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
				String dateTxt = df.format(new Date());
				String owlFileName = "./license-ontology-" + dateTxt + ".owl";
				fos = new FileOutputStream(owlFileName);
			    oom.saveOntology(oo, fos);
			} catch (IOException ioe) {
			    ioe.printStackTrace();
			} catch (OWLOntologyStorageException oose) {
			    oose.printStackTrace();
			} 

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
				if (fos != null) fos.close();
				if (lnr != null) lnr.close();
				if (fr != null) fr.close();
				if (fw != null) fw.close();
			} catch (IOException ioe) {
				//just eat it, eat it, don't you make me repeat it!
			}
		}
    }

/*
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
*/

    public static boolean isValidFieldValue(String value) {
    	return (value !=null && !value.equals("null") && value.length()>0 && !value.toLowerCase().equals("n/a")
    				&& !value.startsWith("?") && !value.toLowerCase().equals("under development") 
    				&& !value.toLowerCase().equals("identifier will be created at time of release"));
    }

    public static void handleTitle(String title, HashMap<String, OWLNamedIndividual> niMap,
					   OWLOntology oo, OWLDataFactory odf, IriLookup iriMap) {
		OWLNamedIndividual oni = niMap.get("dataset");
        addAnnotationToIndividual(oni, iriMap.lookupAnnPropIri("title"), title, odf, oo);
	}

	/*
    public static void handleVersion(Map.Entry<String, JsonElement> e, HashMap<String, OWLNamedIndividual> niMap,
					   OWLOntology oo, OWLDataFactory odf, IriLookup iriMap) {
	OWLNamedIndividual oni = niMap.get("versionid");
	addAnnotationToIndividual(oni, iriMap.lookupAnnPropIri("label"), versionSuffix, odf, oo);
    }


    public static void handleSource(Map.Entry<String, JsonElement> e, HashMap<String, OWLNamedIndividual> niMap,
					   OWLOntology oo, OWLDataFactory odf, IriLookup iriMap) {
	OWLNamedIndividual oni = niMap.get("sourcerepository");
        JsonElement je = e.getValue();
        if (je instanceof JsonPrimitive) {
	    String value = ((JsonPrimitive)je).getAsString();
	    addAnnotationToIndividual(oni, iriMap.lookupAnnPropIri("hasURL"), value, odf, oo);
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
		    addAnnotationToIndividual(oni, iriMap.lookupAnnPropIri("label"), txt, odf, oo);
		    addAnnotationToIndividual(oni, iriMap.lookupAnnPropIri("hasURL"), url, odf, oo);
	    } else {
		addAnnotationToIndividual(oni, iriMap.lookupAnnPropIri("label"), value, odf, oo);
	    }
	} else {
	    System.err.println("License attribute has value that is not primitive.");
	}
    }
    */

    public static void handleDatasetIdentifier(String datasetIdentifier, HashMap<String, OWLNamedIndividual> niMap,
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
		if (url != null && url.length()>0) {
			addAnnotationToIndividual(idInd, iriMap.lookupAnnPropIri("hasURL"), url, odf, oo);
		}
    }

 /*
    public static void handleGeneralInfo(Map.Entry<String, JsonElement> e, HashMap<String, OWLNamedIndividual> niMap,
					   OWLOntology oo, OWLDataFactory odf, IriLookup iriMap) {
	OWLNamedIndividual oni = niMap.get("dtm");
        JsonElement je = e.getValue();
        if (je instanceof JsonPrimitive) {
	    String value = ((JsonPrimitive)je).getAsString();
	    addAnnotationToIndividual(oni, iriMap.lookupAnnPropIri("synopsis"), value, odf, oo);
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
		OWLNamedIndividual oni = createNamedIndividualWithTypeAndLabel(odf, oo, iriMap.lookupClassIri("website"), iriMap.lookupAnnPropIri("editor preferred"), 
									"website for " + fullName);
		niMap.put("website", oni);
	    addAnnotationToIndividual(oni, iriMap.lookupAnnPropIri("hasURL"), url, odf, oo);
	    createOWLObjectPropertyAssertion(oni, iriMap.lookupObjPropIri("is about"), niMap.get("dataset"), odf, oo);
	    websiteInds.put(url, oni);
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
	    	addAnnotationToIndividual(devi, iriMap.lookupAnnPropIri("editor preferred"), prefTerm, odf, oo);
			createOWLObjectPropertyAssertion(createInd, iriMap.lookupObjPropIri("has active participant"), devi, odf, oo);
		}
		//System.out.println(i);
	    
    }
    */

     public static void handleVersionId(String versionIdTxt, HashMap<String, OWLNamedIndividual> niMap,
					   OWLOntology oo, OWLDataFactory odf, IriLookup iriMap) {
		OWLNamedIndividual oni = createNamedIndividualWithTypeAndLabel(odf, oo, iriMap.lookupClassIri("version number"),
									iriMap.lookupAnnPropIri("label"), versionIdTxt);
		niMap.put("versionid",oni);
		createOWLObjectPropertyAssertion(oni, iriMap.lookupObjPropIri("denotes"), niMap.get("license"), odf, oo);
    }

    public static void handleCreationDate(String date, HashMap<String, OWLNamedIndividual> niMap, 
    				OWLOntology oo, OWLDataFactory odf, IriLookup iriMap) {
    	//create the dataset creation process if it doesn't exist
		OWLNamedIndividual createInd = null;
		if (!niMap.containsKey("creating license")) {
			createInd = createNamedIndividualWithTypeAndLabel(odf, oo, iriMap.lookupClassIri("creating license"),
							iriMap.lookupAnnPropIri("label"), "process of creating " + fullName);
			niMap.put("creating license", createInd);
		} else {
			createInd = niMap.get("data set creation");
		}
		//connect the creation process to the dataset
		createOWLObjectPropertyAssertion(createInd, iriMap.lookupObjPropIri("has specified output"), niMap.get("license"), odf, oo);
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

		    addAnnotationToIndividual(execInd, iriMap.lookupAnnPropIri("label"), "execution of dtm for study described in " + pubInfo.get(0), odf, oo);
		    addAnnotationToIndividual(studyInd, iriMap.lookupAnnPropIri("label"), "study process described in " + pubInfo.get(0), odf, oo);

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

	public static void handleIndexTerms(OWLNamedIndividual license, String indexTerms, IriLookup iriMap, 
						OWLDataFactory odf, OWLOntology oo) {
		String[] terms = indexTerms.split(Pattern.quote(";"));
		for (String term : terms) {
			if (isValidFieldValue(term)) {
				addAnnotationToIndividual(license, iriMap.lookupAnnPropIri("alternative term"), term, odf, oo);
			}
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
		return createNamedIndividualWithIriTypeAndLabel(nextIri(), odf, oo, classTypeIri, labelPropIri, rdfsLabel);
    }

    public static OWLNamedIndividual createNamedIndividualWithIriTypeAndLabel(
									      IRI individualIri, OWLDataFactory odf, OWLOntology oo, IRI classTypeIri, IRI labelPropIri, String rdfsLabel) {
		OWLNamedIndividual oni = odf.getOWLNamedIndividual(individualIri);
		OWLClassAssertionAxiom ocaa = odf.getOWLClassAssertionAxiom(odf.getOWLClass(classTypeIri), oni);
		oom.addAxiom(oo,ocaa);
		addAnnotationToIndividual(oni, labelPropIri, rdfsLabel, odf, oo);
		return oni;
    }


    public static void addAnnotationToIndividual(OWLNamedIndividual oni, IRI annPropIri, String value, OWLDataFactory odf, OWLOntology oo) {
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


    public static OWLNamedIndividual createMidasDigitalCommonsIndividuals(OWLDataFactory odf, OWLOntology oo, IriLookup iriMap) {
        OWLNamedIndividual oni = odf.getOWLNamedIndividual(iriMap.lookupIndividIri("mdc"));
        OWLClassAssertionAxiom ocaaTemp = odf.getOWLClassAssertionAxiom(odf.getOWLClass(iriMap.lookupClassIri("dataset")), oni);
        oom.addAxiom(oo,ocaaTemp);
        addAnnotationToIndividual(oni, iriMap.lookupAnnPropIri("label"), "MIDAS Digital Commons", odf, oo);
        addAnnotationToIndividual(oni, iriMap.lookupAnnPropIri("editor preferred"), "Digital Commons of the MIDAS Research Network", odf, oo);

        OWLNamedIndividual mdcWebsite = odf.getOWLNamedIndividual(iriMap.lookupIndividIri("mdc website"));
        OWLClassAssertionAxiom ocaa = odf.getOWLClassAssertionAxiom(odf.getOWLClass(iriMap.lookupClassIri("website")), mdcWebsite);
        oom.addAxiom(oo, ocaa);
        addAnnotationToIndividual(mdcWebsite, iriMap.lookupAnnPropIri("editor preferred"), "MDC home page", odf, oo);
        addAnnotationToIndividual(mdcWebsite, iriMap.lookupAnnPropIri("title"), "MIDAS Digital Commons Home", odf, oo);
        addAnnotationToIndividual(mdcWebsite, iriMap.lookupAnnPropIri("hasURL"), "http://betaweb.rods.pitt.edu/digital-commons/main#_", odf, oo);
        addAnnotationToIndividual(mdcWebsite, iriMap.lookupAnnPropIri("authors"), "MIDAS Informatics Services Group", odf, oo);
        addAnnotationToIndividual(mdcWebsite, iriMap.lookupAnnPropIri("published date"), "2017-05-19", odf, oo);

        createOWLObjectPropertyAssertion(mdcWebsite, iriMap.lookupObjPropIri("is about"), oni, odf, oo);
        return oni;
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
		    addAnnotationToIndividual(formatInd, iriMap.lookupAnnPropIri("label"), label, odf, oo);

		    for (String key : keys) {
			formatInds.put(key, formatInd);
		    }
		}
    }
    */
    
}
