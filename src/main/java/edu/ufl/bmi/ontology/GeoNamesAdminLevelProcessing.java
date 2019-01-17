package edu.ufl.bmi.ontology;

import java.io.FileReader;
import java.io.LineNumberReader;
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
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.AddImport;

import edu.ufl.bmi.misc.IriLookup;

public class GeoNamesAdminLevelProcessing {
    static long iriCounter;
    static String iriPrefix = "http://www.pitt.edu/obc/GEO_";
    static int iriLen = 9;

    static String countryIso;
    static IriLookup iriMap;
    static String owlBase;
    static String inputFileName;

    static IRI countryIri;

    static OWLOntologyManager oom;
    static OWLDataFactory odf;
    static OWLOntology[] oos;  //one for each admin level
    static String[] owlFileNames;

    public static void main(String[] args) {
		try {
		    readConfigurationFile(args[0]);
		    setupOntologies();
		    processInputFile();
		    saveOntologies();
		} catch (IOException ioe) {
		    System.out.println(ioe);
		    ioe.printStackTrace();
		}
    }

    public static void readConfigurationFile(String fName) throws IOException {
		FileReader fr = new FileReader(fName);
		LineNumberReader lnr = new LineNumberReader(fr);
		String line;
		while((line=lnr.readLine())!=null) {
		    String[] flds = line.split(Pattern.quote("="));
		    if (flds[0].equals("countrycode")) {
				countryIso = flds[1];
		    } else if (flds[0].equals("input")) {
				inputFileName=flds[1];
		    } else if (flds[0].equals("owlpurlbase")) {
				owlBase = flds[1];
		    } else if (flds[0].equals("countryiri")) {
				countryIri = IRI.create(flds[1]);
		    } else if (flds[0].equals("irilookup")) {
				iriMap = new IriLookup(flds[1]);
				iriMap.init();
		    } else if (flds[0].equals("iriStart")) {
				iriCounter = Long.parseLong(flds[1]);
		    } else {
				System.err.println("don't know what " + flds[0] + " is. Ignoring...");
		    }
		}
    }

    public static void setupOntologies() {
		oom = OWLManager.createOWLOntologyManager();
		odf = OWLManager.getOWLDataFactory();
		oos = new OWLOntology[4];
		owlFileNames = new String[4];
		
		IRI lastIri = null;
		for (int i=0; i<4; i++) {
		
		    IRI ontologyIRI = IRI.create(owlBase + "-" + Integer.toString(i+1) + ".owl");
		    owlFileNames[i] = countryIso + "-admin-level-" + Integer.toString(i+1) + ".owl";
		    try {
				oos[i] = oom.createOntology(ontologyIRI);
		    } catch (OWLOntologyCreationException ooce) {
				ooce.printStackTrace();
		    }
		    if (lastIri != null) {
				OWLImportsDeclaration idec = odf.getOWLImportsDeclaration(lastIri);
				oom.applyChange(new AddImport(oos[i], idec));
		    } else {
				OWLImportsDeclaration idec = odf.getOWLImportsDeclaration(IRI.create("http://purl.obolibrary.org/obo/geo/dev/nation-geography.owl"));
				oom.applyChange(new AddImport(oos[i], idec));
		    }

		    lastIri = ontologyIRI;
		}
    }

    public static void processInputFile() {
		try {
		    FileReader fr = new FileReader(inputFileName);
		    LineNumberReader lnr = new LineNumberReader(fr);
		    String line;

		    ArrayList<HashMap<String, OWLNamedIndividual>> gnAdmCodeToNamedInd = new ArrayList<HashMap<String, OWLNamedIndividual>>();
		    for (int i=0; i<4; i++) {
		    	HashMap<String, OWLNamedIndividual> map = new HashMap<String, OWLNamedIndividual>();
		    	gnAdmCodeToNamedInd.add(map);
		    }
		    OWLNamedIndividual country = odf.getOWLNamedIndividual(countryIri);

		    while ((line=lnr.readLine())!=null) {
				String[] flds = line.split(Pattern.quote("\t"));
				/*
				  flds[0] - geonamesid
				  flds[1] - pref term
				  flds[2] - label
				  flds[3] - list of alternative terms
				  flds[7] - admin level
				  flds[8] - country code
				  flds[10] - admin 1 code
				  flds[11] - admin 2 code
				  flds[12] - admin 3 code
				  flds[13] - admin 4 code
				*/

				System.out.println(flds[7]);
				int level = Integer.parseInt(flds[7].substring(flds[7].length()-1));
				int iLevel = level-1;

				OWLNamedIndividual gr = createNamedIndividualWithTypeAndLabel(oos[iLevel], iriMap.lookupClassIri("geo region"), iriMap.lookupAnnPropIri("editor preferred"), "region of " + flds[1]);
				addAnnotationToNamedIndividual(gr, iriMap.lookupAnnPropIri("label"), "region of " + flds[2], oos[iLevel]);
				addAnnotationToNamedIndividual(gr, iriMap.lookupAnnPropIri("geonameid"), flds[0], oos[iLevel]);

				String[] altTerms = flds[3].split(Pattern.quote(","));
				for (int j=0; j<altTerms.length; j++) {
				    if (!altTerms[j].trim().equals(""))
					addAnnotationToNamedIndividual(gr, iriMap.lookupAnnPropIri("alternative term"), altTerms[j], oos[iLevel]);
				}

				int iCodeFld = -1, iParentFld = 0;
				IRI annPropIri = null;
				if (flds[7].equals("ADM1")) {
				    iCodeFld = 10;
				    iParentFld = -1;
				    annPropIri = iriMap.lookupAnnPropIri("admin1 code");
				} else if (flds[7].equals("ADM2")) {
				    iCodeFld = 11;
				    iParentFld = 10;
				    annPropIri = iriMap.lookupAnnPropIri("admin2 code");
		 		} else if (flds[7].equals("ADM3")) {
				    iCodeFld = 12;
				    iParentFld = 11;
				    annPropIri = iriMap.lookupAnnPropIri("admin3 code");
				} else if (flds[7].equals("ADM4")) {
				    iCodeFld = 13;
				    iParentFld = 12;		    
				    annPropIri = iriMap.lookupAnnPropIri("admin4 code");
				}
			 
			 	OWLNamedIndividual parentInd = country;
			 	int iParentLevel = iLevel-1;
				if (iParentFld > 0) {
					if (gnAdmCodeToNamedInd.get(iParentLevel).get(flds[iParentFld]) == null) {
						System.err.println("parent not found on line " + lnr.getLineNumber() + "\n\t" + line);
						boolean found = false;
						while (iParentFld > 10) {
							iParentFld--;
							iParentLevel--;
							if (flds[iParentFld] != null && flds[iParentFld].length() > 0 && gnAdmCodeToNamedInd.get(iParentLevel).get(flds[iParentFld]) != null) {
								found = true;
								break;
							}
						}
						if (found) {
							System.out.println("Connecting " + flds[1] + " to higher admin level than immediate parent level.");
							parentInd = gnAdmCodeToNamedInd.get(iParentLevel).get(flds[iParentFld]);
						}
					} else {
						parentInd = gnAdmCodeToNamedInd.get(iParentLevel).get(flds[iParentFld]);
					}
				}
				createOWLObjectPropertyAssertion(gr, iriMap.lookupObjPropIri("proper part"), parentInd, oos[iLevel]);
				   
				addAnnotationToNamedIndividual(gr, annPropIri, flds[iCodeFld], oos[iLevel]);
				gnAdmCodeToNamedInd.get(iLevel).put(flds[iCodeFld], gr);


		    }

		} catch (IOException ioe) {
		    System.err.println(ioe);
		    ioe.printStackTrace();
		}
    }

    public static void saveOntologies() {
		try {
		    for (int i=0; i<4; i++) {
				oom.saveOntology(oos[i], new FileOutputStream(owlFileNames[i]));
		    }
		} catch (IOException ioe) {
		    ioe.printStackTrace();
		} catch (OWLOntologyStorageException oose) {
		    oose.printStackTrace();
		}
    }

    public static IRI nextIri() {
		String counterTxt = Long.toString(iriCounter++);
		StringBuilder sb = new StringBuilder(iriPrefix);
		int numZero = iriLen-counterTxt.length();
		for (int i=0; i<numZero; i++) 
		    sb.append("0");
		sb.append(counterTxt);
		return IRI.create(new String(sb));
    }

    public static OWLNamedIndividual createNamedIndividualWithTypeAndLabel(OWLOntology oo, IRI classTypeIri, IRI labelPropIri, String rdfsLabel) {
		OWLNamedIndividual oni = odf.getOWLNamedIndividual(nextIri());
		OWLClassAssertionAxiom ocaa = odf.getOWLClassAssertionAxiom(odf.getOWLClass(classTypeIri), oni);
		oom.addAxiom(oo,ocaa);
		addAnnotationToNamedIndividual(oni, labelPropIri, rdfsLabel, oo);
		
		return oni;
    }

    public static void addAnnotationToNamedIndividual(OWLNamedIndividual oni, IRI annPropIri, String value, OWLOntology oo) {
		OWLLiteral li = odf.getOWLLiteral(value);
		OWLAnnotationProperty la = odf.getOWLAnnotationProperty(annPropIri);
		OWLAnnotation oa = odf.getOWLAnnotation(la, li);
		OWLAnnotationAssertionAxiom oaaa = odf.getOWLAnnotationAssertionAxiom(oni.getIRI(), oa);
		oom.addAxiom(oo, oaaa);
    }


    public static void createOWLObjectPropertyAssertion(OWLNamedIndividual source, IRI objPropIri, OWLNamedIndividual target, OWLOntology oo) {
		OWLObjectProperty oop = odf.getOWLObjectProperty(objPropIri);
		OWLObjectPropertyAssertionAxiom oopaa = odf.getOWLObjectPropertyAssertionAxiom(oop, source, target);
		oom.addAxiom(oo, oopaa);
    }
}