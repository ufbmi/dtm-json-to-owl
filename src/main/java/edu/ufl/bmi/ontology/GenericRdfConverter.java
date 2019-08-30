package edu.ufl.bmi.ontology;

import java.io.FileReader;
import java.io.LineNumberReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.FileOutputStream;
import java.text.ParseException;
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
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.AddImport;

import edu.ufl.bmi.misc.IriLookup;

public class GenericRdfConverter {
	static boolean headerProcessed = false;
	static String iriCounterTxt;
    static long iriCounter;
    static String iriPrefix;
    static String iriLenTxt;
    static int iriLen;

    static IriLookup iriMap;
   	static String rowTypeTxt;
    static String inputFileName;

    static String instructionFileName;

    static OWLOntologyManager oom;
    static OWLDataFactory odf;
    static OWLOntology oos;  //one for each admin level
    static String outputFileName;
    static String outputFileIriId;

    static HashMap<String, Integer> fieldNameToIndex;
    static ArrayList<Integer> indexesOfNamesIds;
    static HashMap<Integer, OWLNamedIndividual> lineNumToInd;
    static ArrayList<HashMap<String, OWLNamedIndividual>> mapsNamesIdsToInd;

    public static void main(String[] args) {
		try {
		    readConfigurationFile(args[0]);
		    setupOuputOwlFile();
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
		    if (flds[0].trim().equals("row_type")) {
				rowTypeTxt = flds[1];
		    } else if (flds[0].trim().equals("data_file")) {
				inputFileName=flds[1];
		    } else if (flds[0].trim().equals("iri_prefix")) {
				iriPrefix = flds[1];
		    } else if (flds[0].trim().equals("iri_counter")) {
				iriCounterTxt = flds[1];
		    } else if (flds[0].trim().equals("iri_lookup")) {
				iriMap = new IriLookup(flds[1].trim());
				iriMap.init();
		    } else if (flds[0].trim().equals("iri_id_length")) {
				iriLenTxt = flds[1].trim();
		    } else if (flds[0].trim().equals("output_file")) {
		    	outputFileName = flds[1].trim();
		    } else if (flds[0].trim().equals("output_file_iri_id")) {
		    	outputFileIriId = flds[1].trim();
		    } else if (flds[0].trim().equals("instructions")) {
		    	instructionFileName = flds[1].trim();
		    } else {
				System.err.println("don't know what " + flds[0] + " is. Ignoring...");
		    }
		}

		System.out.println(iriLenTxt);
		iriLen = Integer.parseInt(iriLenTxt);
		iriCounter = Long.parseLong(iriCounterTxt);

		lnr.close();
		fr.close();
    }

    protected static void processHeaderRow(LineNumberReader lnr) throws IOException {
    	
    	if (fieldNameToIndex == null) fieldNameToIndex = new HashMap<String, Integer>();
    	if (indexesOfNamesIds == null) indexesOfNamesIds = new ArrayList<Integer>();

    	String line;
		do {
			line=lnr.readLine();
			line = line.trim();	
		} while (line != null || line.length() == 0);

		if (headerProcessed) return;

		String[] flds = line.split(Pattern.quote("\t"));
		if (mapsNamesIdsToInd == null) mapsNamesIdsToInd = new ArrayList<HashMap<String,OWLNamedIndividual>>(flds.length);

		for (int i=0; i<flds.length; i++) {
			String fieldName = flds[i].trim();
			fieldNameToIndex.put(fieldName, i);
			String fieldNameLower = fieldName.toLowerCase();
			if (fieldNameLower.contains("name") || fieldName.contains("ident") || 
				fieldNameLower.equals("id")) {
				indexesOfNamesIds.add(i);
			}
			HashMap<String, OWLNamedIndividual> mapi = new HashMap<String, OWLNamedIndividual>();
			mapsNamesIdsToInd.add(mapi);
		}
		headerProcessed = true;
    }

    public static void setupOuputOwlFile() {
		oom = OWLManager.createOWLOntologyManager();
		odf = OWLManager.getOWLDataFactory();
		String iriText = iriPrefix + "/" + outputFileIriId;
		IRI ontologyIRI = IRI.create(iriText);
		try {
			oos = oom.createOntology(ontologyIRI);
		} catch (OWLOntologyCreationException ooce) {
			ooce.printStackTrace();
		}
    }

    public static void processInputFile() {
    	try {
    		firstPassthrough();	
    		processInstructionsForEachRow();
    	} catch (IOException ioe) {
    		System.err.println(ioe);
    		ioe.printStackTrace();
    	}
	}

	public static void firstPassthrough() throws IOException {
		lineNumToInd = new HashMap<Integer, OWLNamedIndividual>();
		FileReader fr = new FileReader(inputFileName);
		LineNumberReader lnr = new LineNumberReader(fr);
		String line;
    	processHeaderRow(lnr);

		while ((line=lnr.readLine())!=null) {
			String[] flds = line.split(Pattern.quote("\t"));
			OWLNamedIndividual oni = createNamedIndividualWithType(iriMap.lookupClassIri(rowTypeTxt));
			int lineNumber = lnr.getLineNumber();
			lineNumToInd.put(lineNumber, oni);
			Iterator<Integer> indexes = indexesOfNamesIds.iterator();
			while(indexes.hasNext()) {
				Integer iInt = indexes.next();
				int i = iInt.intValue();
				String fieldValue = flds[i];
				HashMap<String, OWLNamedIndividual> mapi = mapsNamesIdsToInd.get(i);
				mapi.put(fieldValue, oni);
			}
		}

		lnr.close();
		fr.close();
    }

    protected static void processInstructionsForEachRow() throws IOException {
    	RdfConversionInstructionSetCompiler c = new RdfConversionInstructionSetCompiler(instructionFileName, iriMap, fieldNameToIndex, 
				odf, mapsNamesIdsToInd);
    	try {
    		c.compile();
    	} catch (ParseException pe) {
    		pe.printStackTrace();
    	}

    	FileReader fr = new FileReader(inputFileName);
		LineNumberReader lnr = new LineNumberReader(fr);
		String line;
    	processHeaderRow(lnr);



    	while ((line=lnr.readLine())!=null) {
			String[] flds = line.split(Pattern.quote("\t"));
		}

		lnr.close();
		fr.close();
    }

    public static void saveOntologies() {
		try {
			oom.saveOntology(oos, new FileOutputStream(outputFileName));
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

   public static OWLNamedIndividual createNamedIndividualWithType(IRI classTypeIri) {
		OWLNamedIndividual oni = odf.getOWLNamedIndividual(nextIri());
		OWLClassAssertionAxiom ocaa = odf.getOWLClassAssertionAxiom(odf.getOWLClass(classTypeIri), oni);
		oom.addAxiom(oos,ocaa);
				
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


    public static void addStringDataToNamedIndividual(OWLNamedIndividual oni, IRI dataPropIri, String value, OWLOntology oo) {
		//OWLLiteral li = odf.getOWLLiteral(value);
		OWLDataProperty dp = odf.getOWLDataProperty(dataPropIri);
		//getOWLDataPropertyAssertionAxiom(OWLDataPropertyExpression property, OWLIndividual subject, String value) 
		OWLDataPropertyAssertionAxiom odpaa = odf.getOWLDataPropertyAssertionAxiom(dp, oni, value);
		oom.addAxiom(oo, odpaa);
    }

    public static void addBooleanDataToNamedIndividual(OWLNamedIndividual oni, IRI dataPropIri, boolean value, OWLOntology oo) {
		//OWLLiteral li = odf.getOWLLiteral(value);
		OWLDataProperty dp = odf.getOWLDataProperty(dataPropIri);
		//getOWLDataPropertyAssertionAxiom(OWLDataPropertyExpression property, OWLIndividual subject, String value) 
		OWLDataPropertyAssertionAxiom odpaa = odf.getOWLDataPropertyAssertionAxiom(dp, oni, value);
		oom.addAxiom(oo, odpaa);
    }

    public static void addFloatDataToNamedIndividual(OWLNamedIndividual oni, IRI dataPropIri, float value, OWLOntology oo) {
		//OWLLiteral li = odf.getOWLLiteral(value);
		OWLDataProperty dp = odf.getOWLDataProperty(dataPropIri);
		//getOWLDataPropertyAssertionAxiom(OWLDataPropertyExpression property, OWLIndividual subject, String value) 
		OWLDataPropertyAssertionAxiom odpaa = odf.getOWLDataPropertyAssertionAxiom(dp, oni, value);
		oom.addAxiom(oo, odpaa);
    }

    public static void addIntDataToNamedIndividual(OWLNamedIndividual oni, IRI dataPropIri, int value, OWLOntology oo) {
		//OWLLiteral li = odf.getOWLLiteral(value);
		OWLDataProperty dp = odf.getOWLDataProperty(dataPropIri);
		//getOWLDataPropertyAssertionAxiom(OWLDataPropertyExpression property, OWLIndividual subject, String value) 
		OWLDataPropertyAssertionAxiom odpaa = odf.getOWLDataPropertyAssertionAxiom(dp, oni, value);
		oom.addAxiom(oo, odpaa);
    }
}