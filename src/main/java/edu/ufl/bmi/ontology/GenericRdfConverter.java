package edu.ufl.bmi.ontology;

import java.io.File;
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
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.TurtleDocumentFormat;
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

import edu.ufl.bmi.misc.ApiSourceJsonObjectDataProvider;
import edu.ufl.bmi.misc.DataObject;
import edu.ufl.bmi.misc.DataObjectProvider;
import edu.ufl.bmi.misc.LineNumberReaderSourceRecordDataObjectProvider;
import edu.ufl.bmi.misc.RecordDataObject;
import edu.ufl.bmi.misc.IriLookup;

public class GenericRdfConverter {
	static boolean headerProcessed = false;
	static String iriCounterTxt;
    static long iriCounter;
    static String iriPrefix;
    static String iriLenTxt;
    static int iriLen;

    static IriLookup iriMap;
   	static String objectTypeTxt;
    static String inputFileName;
    static String inputFileDelim;

    static String instructionFileName;

    static OWLOntologyManager oom;
    static OWLDataFactory odf;
    static OWLOntology oos;  //one for each admin level
    static String outputFileName;
    static String outputFileIriId;

    static HashMap<String, Integer> fieldNameToIndex;
    static ArrayList<String> fieldsInOrder;
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
    static RdfConversionInstructionSetExecutor rcise;

    static DataObjectProvider dop, dop1;
    static String dataProviderTypeTxt;
    static HashMap<String, OWLNamedIndividual> keyToInd;
    static HashMap<String, HashMap<String, OWLNamedIndividual>> uniqueFieldsMapValuesToInd; 

    static String baseUrl;
    static String allObjectIdsUrl;
    static String objectLocatorTxt;

    static String instructionSetVersion;

    public static void main(String[] args) {
		try {
		    readConfigurationProperties(args[0]);
		    setupOuputOwlFile();
		    /*
		     *  Process input file is the old way that I'm working on sunsetting
		     */ 
		    //processInputFile();
		    /*
		     *  Process data objects is the new way that I'm working on as the
		     *	 replacement.  The rest -- config file, output OWL file, and 
		     *   saveOntologies -- can stay the same.
		     */
		    processDataObjects();
		    saveOntologies();
		} catch (IOException ioe) {
		    System.err.println(ioe);
		    ioe.printStackTrace();
		}
    }

    public static void readConfigurationProperties(String fName) throws IOException {
    	Properties p = new Properties();
    	p.load(new FileReader(fName));

    	objectTypeTxt = p.getProperty("object_type");
    	dataProviderTypeTxt = p.getProperty("data_provider_type");
    	inputFileName = p.getProperty("data_file");
    	String delimTxt = p.getProperty("data_file_delimiter");
    	if (delimTxt != null && delimTxt.equals("tab")) {
			inputFileDelim = "\t";
		} else {
			inputFileDelim = delimTxt;
		}
		iriPrefix = p.getProperty("iri_prefix");
		iriCounterTxt = p.getProperty("iri_counter");
		String iriMapFileName = p.getProperty("iri_lookup");
		iriMap = new IriLookup(iriMapFileName);
		iriMap.init();
		iriLenTxt = p.getProperty("iri_id_length");
		outputFileName = p.getProperty("output_file");
		outputFileIriId = p.getProperty("output_file_iri_id");
		instructionFileName = p.getProperty("instructions");
		instructionSetVersion = p.getProperty("instructions_version");

		baseUrl = p.getProperty("api_base_url");
		allObjectIdsUrl = p.getProperty("get_all_object_ids_url");
		objectLocatorTxt = p.getProperty(objectTypeTxt);

		String allUniqueKeyFieldNames = p.getProperty("unique_key_fields");
		uniqueKeyFieldNames = new ArrayList<String>();
		String[] vals = allUniqueKeyFieldNames.split(Pattern.quote(","));
		for (String v : vals) {
			uniqueKeyFieldNames.add(v);
		}

		uniqueIdFieldName = p.getProperty("unique_id_field");

    	//System.out.println(iriLenTxt);
		iriLen = Integer.parseInt(iriLenTxt);
		iriCounter = Long.parseLong(iriCounterTxt);

		iriRepository = new RdfIriRepositoryWithJena(outputFileIriId + ".rdf");
		iriRepository.initialize();

		iriCounter = Math.max(iriCounter, iriRepository.getIriCounter());
		System.out.println("Setting counter to: " + iriCounter);

		iriRepositoryPrefix = iriPrefix + objectTypeTxt;
		uniqueIdFieldIri = IRI.create(iriRepositoryPrefix + "/" + uniqueIdFieldName);

		rcise = new RdfConversionInstructionSetExecutor();
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

		String[] flds = line.split(Pattern.quote("\t"), -1);

		fieldsInOrder = new ArrayList<String>();

		for (int i=0; i<flds.length; i++) {
			String fieldName = flds[i].trim();
			fieldNameToIndex.put(fieldName, i);
			System.out.println("fieldName = '" + fieldName + "'");
			boolean added = fieldsInOrder.add(fieldName);
			if (!added) System.err.println("Failed to add " + i + "th element: '" + fieldName + "'");
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
		String iriText = iriPrefix + "/" + objectTypeTxt;
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

	public static void processDataObjects() {
		try {
			buildDataObjectProviders();
			/*
			 * The first passthrough not only creates IRIs for the particular
			 *   in reality represented by the data object, but also sifts
			 *   through various fields in the data object to create search
			 *   indexes to retrieve those objections in the processing of 
			 *   instructions.  
			 *
			 * This situation creates a dependency, where the first passthrough
			 *   must occur before building the instruction sets for tranforming
			 *   the data objects to RDF.
			 *
			 */
			firstPassthroughDataObjects();
			buildInstructionSet();
			executeInstructionsAgainstDataObjects();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected static void buildDataObjectProviders() throws IOException {
		/*
			Based on config file input, which will tell us
			 both what type of data object provider to build
			 as well as the information needed to build it.

			 So for LineNumberReaderSourceDataObjectProvider:
			 	file name
			 	field delimiter
			 	subfield delimiter if any
			 	name of field that is the key
			 	name(s) of field(s) that have unique values for each record

			 And for JsonApiSourceDataObjectProvider:
			 	base url for the API
			 	extension for the types of objects
			 	whether it's an "all" or "by id" situation
			 	if "by id" how to get a list of all IDs to 
			 		subsequently use to get the objects
		*/
		dop = buildDataObjectProvider();
		if (!dop.isReusable())
			dop1 = buildDataObjectProvider();
		else 
			dop1 = dop;
	}

	protected static DataObjectProvider buildDataObjectProvider() throws IOException {
		DataObjectProvider dopLocal = null;
		if (dataProviderTypeTxt.equals("file line reader")) {
			FileReader fr = new FileReader(inputFileName);
			dopLocal = new LineNumberReaderSourceRecordDataObjectProvider(
					new LineNumberReader(fr), inputFileDelim);
			((LineNumberReaderSourceRecordDataObjectProvider)dopLocal).processSchema();
		} else if (dataProviderTypeTxt.equals("json api")) {
			dopLocal = new ApiSourceJsonObjectDataProvider(baseUrl, allObjectIdsUrl, baseUrl+objectLocatorTxt, uniqueIdFieldName);
			((ApiSourceJsonObjectDataProvider)dopLocal).initialize();
		}
		return dopLocal;
	}

	protected static void firstPassthroughDataObjects() {
		/*
		 *  Goals here:  create OWLNamedIndividual with IRI for each object
		 *		Setup the following lookups:
		 *			1. unique key / ID field to OWLNamedIndividual
		 *			2. other unique value fields to OWLNamedIndividual
		*/
		keyToInd = new HashMap<String, OWLNamedIndividual>();
		uniqueFieldsMapValuesToInd = new HashMap<String, HashMap<String, OWLNamedIndividual>>();
		//System.out.println("uniqueKeyFieldNames = " + uniqueKeyFieldNames);
		for (String keyField : uniqueKeyFieldNames) {
			HashMap<String, OWLNamedIndividual> soni = new HashMap<String, OWLNamedIndividual>();
			uniqueFieldsMapValuesToInd.put(keyField, soni);
		}

		for (DataObject dataObject : dop) {
			HashMap<IRI, String> repoAnnotations = new HashMap<IRI, String>();
			IRI varNameIri = IRI.create(iriRepositoryPrefix + "/variableName");
			repoAnnotations.put(varNameIri, "row individual");
			//System.out.println("uniqueIdFieldName=" + uniqueIdFieldName);
			String keyValue = dataObject.getDataElementValue(uniqueIdFieldName);

			repoAnnotations.put(uniqueIdFieldIri, keyValue);
			Set<IRI> resultSet = iriRepository.queryIris(null, repoAnnotations);
			int resultCount = resultSet.size();

			OWLNamedIndividual oni = null;
			if (resultCount == 1) {
				oni = createNamedIndividualWithIriAndType(resultSet.iterator().next(), iriMap.lookupClassIri(objectTypeTxt));
			} else if (resultCount == 0) {
				oni = createNamedIndividualWithType(iriMap.lookupClassIri(objectTypeTxt));
				iriRepository.addIris(oni.getIRI(), null, repoAnnotations);
			} else {
				throw new RuntimeException("Unexpected query result set number: " + 
					resultCount + ", expected 0 or 1.");
			}

			keyToInd.put(keyValue, oni);

			for (String keyField : uniqueKeyFieldNames) {
				String keyFieldValue = dataObject.getDataElementValue(keyField);
				if (keyFieldValue != null && keyFieldValue.length() > 0) {
					HashMap<String, OWLNamedIndividual> soni = uniqueFieldsMapValuesToInd.get(keyField);
					soni.put(keyFieldValue, oni);
				} else {
					System.out.println("Bad value for key field '" + 
						keyField + "' = '" + keyFieldValue + "'");
				}
			}
		}
	}

	public static void buildInstructionSet() {

		if (instructionSetVersion.equals("v1")) {
			RdfConversionInstructionSetCompiler c = new RdfConversionInstructionSetCompiler(instructionFileName, iriMap,
				odf, uniqueFieldsMapValuesToInd, iriRepository, iriRepositoryPrefix, uniqueIdFieldName, iriPrefix);
			try {
    			rcis = c.compile();
    			rcise = null;
    		} catch (ParseException pe) {
    			pe.printStackTrace();
    		}
		} else if (instructionSetVersion.equals("v2")) {
			RdfConversionInstructionSetF2Compiler c1 = new RdfConversionInstructionSetF2Compiler("./src/main/resources/organization-json-instruction-set.txt",
				iriMap, odf, uniqueFieldsMapValuesToInd, iriRepository, iriRepositoryPrefix, uniqueIdFieldName, iriPrefix);
			try {
    			rcise = c1.compile();
    			rcis = null;
    		} catch (ParseException pe) {
    			pe.printStackTrace();
    		}
		} else {
			System.err.println("Instruction set version " + instructionSetVersion + " either " +
				" doesn't exist or I don't support it.");
		}
	}

	protected static void executeInstructionsAgainstDataObjects() throws IOException {
    	/*
    	 *  For each data object, we want to execute the instruction set against it.
    	 *
    	 */
    	for (DataObject dataObject : dop1) {
    		OWLNamedIndividual rowInd = keyToInd.get(dataObject.getDataElementValue(uniqueIdFieldName));
       		//rcis.executeInstructions(rowInd, dataObject, oos);
       		if (rcis != null) {
       			rcis.executeInstructions(rowInd, dataObject, oos);
       		} else if (rcise != null) {
       			rcise.executeAllInstructionSets(rowInd, dataObject, oos);
       		}
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
				oni = createNamedIndividualWithIriAndType(resultSet.iterator().next(), iriMap.lookupClassIri(objectTypeTxt));
			} else if (resultCount == 0) {
				oni = createNamedIndividualWithType(iriMap.lookupClassIri(objectTypeTxt));
				iriRepository.addIris(oni.getIRI(), null, repoAnnotations);
			} else {
				throw new RuntimeException("Unexpected query result set number: " + 
					resultCount + ", expected 0 or 1.");
			}
			//IRI oniIri = oni.getIRI();
			//System.out.println(oniIri + " is row individual IRI.");
			//System.out.println("\tIRI namespace is: " + oniIri.getNamespace());
			//System.out.println("\tIRI fragment is:  " + oniIri.getFragment());
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
			//rcis.executeInstructions(rowInd, fldList, oos);
    		
    		RecordDataObject rdo = new RecordDataObject(fieldsInOrder, line, uniqueIdFieldName);
    		rcis.executeInstructions(rowInd, rdo, oos);
		}

		lnr.close();
		fr.close();
    }

    public static void saveOntologies() {
		try {
			oom.saveOntology(oos, new TurtleDocumentFormat(), new FileOutputStream(outputFileName));
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