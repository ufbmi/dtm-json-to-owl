package edu.ufl.bmi.ontology;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.IRI;

import edu.ufl.bmi.misc.DataObject;
import edu.ufl.bmi.misc.IriLookup;

public class RdfConversionInstructionSetExecutor {
	List<String> orderedListOfElements;
	HashMap<String, RdfConversionInstructionSet> instructionsByElementName;
	HashMap<String, OWLNamedIndividual> variables;

	public RdfConversionInstructionSetExecutor() {
		this.orderedListOfElements = new ArrayList<String>();
		this.instructionsByElementName = new HashMap<String, RdfConversionInstructionSet>();
	}

	public boolean addInstructionSetForElement(String elementName, RdfConversionInstructionSet rcis) {
		boolean added = false;
		if (rcis == null) throw new IllegalArgumentException("cannot have a null instruction set.");
		if (elementName != null && !this.instructionsByElementName.containsKey(elementName)) {
			this.instructionsByElementName.put(elementName, rcis);
			this.orderedListOfElements.add(elementName);
			System.out.println("Added instruction set for " + elementName + " (" + instructionsByElementName.size() + ", " + orderedListOfElements.size() + ")");
			added = true;
		}
		return added;
	}


	public boolean executeAllInstructionSets(OWLNamedIndividual rowIndividual, DataObject dataObject, 
		OWLOntology oo) {
		/*
		 * We need to share variables across instruction sets because variables created by one
		 *   instruction set might be used by other instruction sets.  This situation does
		 *   mandate that we get the elements in some predetermined order, which is expected
		 *   and therefore dictated by the instructions file as compiled.
		 *
		 * Therefore, we drive the RDF conversion from the instruction set and then report out
		 *   any elements that are present for which we did not execute instructions...
		 *
		 */
		if (this.variables == null) this.variables = new HashMap<String, OWLNamedIndividual>();

		Set<String> elements = dataObject.getElementKeySet();
		System.out.println(this.orderedListOfElements.size() + " instruction sets to process.");
		for (String elementName : this.orderedListOfElements) {
			String value = dataObject.getDataElementValue(elementName);
			//if (elements.contains(elementName)) {
			if (value != null && value.length() > 0) {
				//System.err.print("Executing instructions for: " + elementName + "...");
				RdfConversionInstructionSet rcis = instructionsByElementName.get(elementName);
				rcis.executeInstructions(rowIndividual, dataObject, oo, variables);
				//System.err.println("...done");
			}
		}

		//Set<String> elements = dataObject.getElementKeySet();
		for (String element : elements) {
			if (!this.orderedListOfElements.contains(element)) {
				System.out.println("Mild warning: element " + element + " is present in " + 
					"data object but is not handled by any instruction set.");
			}
		}

		return true;  //not sure why I put this here, so TODO is reconsider.
	}

	public void initializeVariables(IriLookup iriMap, OWLDataFactory odf) {
		this.variables = new HashMap<String, OWLNamedIndividual>();
		setupIndividualsAsVariables(iriMap);
		setupSystemVariables(iriMap);
	}

	protected void setupIndividualsAsVariables(IriLookup iriMap) {
			Set<Map.Entry<String,IRI>> indsAndIris = iriMap.individualEntrySet();
		for (Map.Entry<String,IRI> indAndIri : indsAndIris) {
			String variableName = indAndIri.getKey();
			IRI iri = indAndIri.getValue();
			/*
			 *  This is admittedly a weird dependency on GenericRdfConverter...
			 */
			//createNamedIndividualWithIriTypeAndLabel(IRI iri, OWLOntology oo, IRI classTypeIri, IRI labelPropIri, String rdfsLabel)
			OWLNamedIndividual oni = GenericRdfConverter.createNamedIndividualWithIriTypeAndLabel(iri, iriMap.getTypeForIndividual(variableName),
				iriMap.getLabelForIndividual(variableName));
			this.variables.put(variableName, oni);
		}
	}

	protected void setupSystemVariables(IriLookup iriMap) {
		Calendar c = Calendar.getInstance();
		Calendar cZ = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		String year = Integer.toString(c.get(Calendar.YEAR));
		String month = Integer.toString(c.get(Calendar.MONTH)+1);  month = (month.length()<2) ? "0"+month : month;
		String day = Integer.toString(c.get(Calendar.DAY_OF_MONTH));  day = (day.length()<2) ? "0"+day : day;
		long zoneOffset = c.get(Calendar.ZONE_OFFSET);
		long zoneOffsetHour = zoneOffset/3600000L;
		long dstOffset = c.get(Calendar.DST_OFFSET);
		long dstOffsetHour = dstOffset/3600000L;
		long totalOffset = zoneOffsetHour + dstOffsetHour;
		StringBuilder tzBuilder = new StringBuilder();
		if (totalOffset == 0) tzBuilder.append("Z");
		else {
			if (totalOffset < 0) {
				tzBuilder.append("-");
				totalOffset = -totalOffset;
			}
			if (totalOffset<10)
				tzBuilder.append("0");
			tzBuilder.append(totalOffset);
			tzBuilder.append(":00");
		}
		String tzTxt = tzBuilder.toString();

		String date = year+"-"+month+"-"+day + tzTxt;
		System.out.println("zone offset: " + zoneOffset + ", zone offset in hours: " + zoneOffsetHour);
		System.out.println("DST offset: " + dstOffset + ", dst offset in hours: " + dstOffsetHour);
		//this.variables.put("sysYear", year);
		//this.variables.put("sysMonth", month);
		//this.variables.put("sysDay", day);
		//this.variables.put("sysDate", date);

		OWLNamedIndividual oniSysDateLocal = GenericRdfConverter.createNamedIndividualWithIriAndType(
			IRI.create("http://time.org/gregorian/"+date), iriMap.lookupClassIri("temporal interval"));
		this.variables.put("[sysDateUrlLocal]", oniSysDateLocal);  //http://time.org/

		String yearZ = Integer.toString(cZ.get(Calendar.YEAR));
		String monthZ = Integer.toString(cZ.get(Calendar.MONTH)+1); monthZ = (monthZ.length()<2) ? "0"+monthZ : monthZ;
		String dayZ = Integer.toString(cZ.get(Calendar.DAY_OF_MONTH));  dayZ = (dayZ.length()<2) ? "0"+dayZ : dayZ;

		String dateZ = yearZ+"-"+monthZ+"-"+dayZ+"Z";
		//this.variables.put("sysYearZ", yearZ);
		//this.variables.put("sysMonthZ", monthZ);
		//this.variables.put("sysDayZ", dayZ);
		//this.variables.put("sysDateZ", dateZ);
		OWLNamedIndividual oniSysDateZ = GenericRdfConverter.createNamedIndividualWithIriAndType(
			IRI.create("http://time.org/gregorian/"+dateZ), iriMap.lookupClassIri("temporal interval"));
		this.variables.put("[sysDateUrl]", oniSysDateZ);  //http://time.org/
		System.out.println(oniSysDateZ);

		//this.variables.put("sysTimeMillis", Long.toString(System.currentTimeMillis()));
	}
}