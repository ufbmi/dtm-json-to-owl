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
    static HashMap<Integer, OWLNamedIndividual> lineNumToInd;
	static ArrayList<HashMap<String, OWLNamedIndividual>> uniqueFieldsMapToInd;

    static ArrayList<String> uniqueKeyFieldNames;
    static ArrayList<Integer> uniqueKeyFieldIndexes;

    static String uniqueIdFieldName;
    static int uniqueIdFieldIndex;
    static IRI uniqueIdFieldIri;

    static String iriRepositoryPrefix;
    static RdfIriRepositoryWithJena iriRepository;


    static RdfConversionInstructionSet rcis;

    public static void main(String[] args) {
		try {
		    readConfigurationFile(args[0]);
		    setupOuputOwlFile();
		    processInputFile();
		    saveOntologies();
		} catch (IOException ioe) {
		    System.err.println(ioe);
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
		    } else if (flds[0].trim().equals("unique_key_fields")) {
		    	uniqueKeyFieldNames = new ArrayList<String>();
		    	String[] vals = flds[1].split(Pattern.quote(","));
		    	for (String v : vals) {
		    		uniqueKeyFieldNames.add(v);
		    	}
		    } else if (flds[0].trim().equals("unique_id_field")) {
		    	uniqueIdFieldName = flds[1].trim();
		    } else {
				System.err.println("don't know what " + flds[0] + " is. Ignoring...");
		    }
		}

		//System.out.println(iriLenTxt);
		iriLen = Integer.parseInt(iriLenTxt);
		iriCounter = Long.parseLong(iriCounterTxt);

		iriRepository = new RdfIriRepositoryWithJena(outputFileIriId + ".rdf");
		iriRepository.initialize();

		iriRepositoryPrefix = iriPrefix + rowTypeTxt;
		uniqueIdFieldIri = IRI.create(iriRepositoryPrefix + "/" + uniqueIdFieldName);

		lnr.close();
		fr.close();
    }

    protected static void processHeaderRow(LineNumberReader lnr) throws IOException {
    	
    	if (fieldNameToIndex == null) fieldNameToIndex = new HashMap<String, Integer>();
    	if (uniqueKeyFieldIndexes == null) uniqueKeyFieldIndexes = new ArrayList<Integer>();
    	if (uniqueFieldsMapToInd == null) uniqueFieldsMapToInd = new ArrayList<HashMap<String,OWLNamedIndividual>>();

    	String line;
		do {
			line=lnr.readLine();
			//System.out.println(line);
			if (line != null) line = line.trim();	
		} while (line == null || line.length() == 0);

		if (headerProcessed) return;

		String[] flds = line.split(Pattern.quote("\t"));

		for (int i=0; i<flds.length; i++) {
			String fieldName = flds[i].trim();
			fieldNameToIndex.put(fieldName, i);
			for (String uniqueField : uniqueKeyFieldNames) {
				if (fieldName.equals(uniqueField)) {
					uniqueKeyFieldIndexes.add(i);
					HashMap<String, OWLNamedIndividual> mapi = new HashMap<String, OWLNamedIndividual>();
					uniqueFieldsMapToInd.add(mapi);
				}
			}
		}
		uniqueIdFieldIndex = fieldNameToIndex.get(uniqueIdFieldName);

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
    		buildInstructionSet(); 
    		processInstructionsForEachRow();
    	} catch (IOException ioe) {
    		System.err.println(ioe);
    		ioe.printStackTrace();
    	}
	}

	public static void buildInstructionSet() {
		int uniqueFieldIndex = fieldNameToIndex.get(uniqueIdFieldName);
		RdfConversionInstructionSetCompiler c = new RdfConversionInstructionSetCompiler(instructionFileName, iriMap, fieldNameToIndex, 
				odf, uniqueFieldsMapToInd, iriRepository, iriRepositoryPrefix, uniqueIdFieldName);
    	try {
    		rcis = c.compile();
    	} catch (ParseException pe) {
    		pe.printStackTrace();
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
			
			HashMap<IRI, String> repoAnnotations = new HashMap<IRI, String>();
			IRI varNameIri = IRI.create(iriRepositoryPrefix + "/variableName");
			repoAnnotations.put(varNameIri, "row individual");
			repoAnnotations.put(uniqueIdFieldIri, flds[uniqueIdFieldIndex]);
			Set<IRI> resultSet = iriRepository.queryIris(null, repoAnnotations);
			int resultCount = resultSet.size();

			OWLNamedIndividual oni = null;
			if (resultCount == 1) {
				oni = createNamedIndividualWithIriAndType(resultSet.iterator().next(), iriMap.lookupClassIri(rowTypeTxt));
			} else if (resultCount == 0) {
				oni = createNamedIndividualWithType(iriMap.lookupClassIri(rowTypeTxt));
				iriRepository.addIris(oni.getIRI(), null, repoAnnotations);
			} else {
				throw new RuntimeException("Unexpected query result set number: " + 
					resultCount + ", expected 0 or 1.");
			}
			int lineNumber = lnr.getLineNumber();
			lineNumToInd.put(lineNumber, oni);
			Iterator<Integer> indexes = uniqueKeyFieldIndexes.iterator();
			int j = 0;
			while(indexes.hasNext()) {
				Integer iInt = indexes.next();
				int i = iInt.intValue();
				String fieldValue = flds[i];
				HashMap<String, OWLNamedIndividual> mapi = uniqueFieldsMapToInd.get(j);
				mapi.put(fieldValue, oni);
				j++;
			}
		}

		lnr.close();
		fr.close();
    }

    protected static void processInstructionsForEachRow() throws IOException {
    	

    	FileReader fr = new FileReader(inputFileName);
		LineNumberReader lnr = new LineNumberReader(fr);
		String line;
    	processHeaderRow(lnr);


    	/*
    	 *  For each row, we want to execute the instruction set against it.
    	 *
    	 */
    	while ((line=lnr.readLine())!=null) {
    		String[] flds = line.split(Pattern.quote("\t"), -1);
    		if (flds.length == 0) continue;

    		int lineNumber = lnr.getLineNumber();
    		//System.out.println("Line number: " + lineNumber + " has " + flds.length + " fields.");
    		OWLNamedIndividual rowInd = lineNumToInd.get(lineNumber);
    		ArrayList<String> fldList = new ArrayList<String>();
    		for (String s : flds) fldList.add(s);
			rcis.executeInstructions(rowInd, fldList, oos);
		}

		lnr.close();
		fr.close();
    }

    public static void saveOntologies() {
		try {
			oom.saveOntology(oos, new FileOutputStream(outputFileName));
			iriRepository.writeFile();
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
    	return createNamedIndividualWithIriTypeAndLabel(nextIri(), oo, classTypeIri, labelPropIri, rdfsLabel);
    }

    public static OWLNamedIndividual createNamedIndividualWithIriTypeAndLabel(IRI iri, OWLOntology oo, IRI classTypeIri, IRI labelPropIri, String rdfsLabel) {
		OWLNamedIndividual oni = odf.getOWLNamedIndividual(iri);
		OWLClassAssertionAxiom ocaa = odf.getOWLClassAssertionAxiom(odf.getOWLClass(classTypeIri), oni);
		oom.addAxiom(oo,ocaa);
		addAnnotationToNamedIndividual(oni, labelPropIri, rdfsLabel, oo);
		
		return oni;
    }

   public static OWLNamedIndividual createNamedIndividualWithType(IRI classTypeIri) {
   		return createNamedIndividualWithIriAndType(nextIri(), classTypeIri);
   }

   public static OWLNamedIndividual createNamedIndividualWithIriAndType(IRI individualIri, IRI classTypeIri) {
		OWLNamedIndividual oni = odf.getOWLNamedIndividual(individualIri);
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

    public static void addClassAssertionAxiom(OWLNamedIndividual oni, IRI classTypeIri, OWLOntology oo){
    	OWLClassAssertionAxiom ocaa = odf.getOWLClassAssertionAxiom(odf.getOWLClass(classTypeIri), oni);
		oom.addAxiom(oo, ocaa);
    }
}