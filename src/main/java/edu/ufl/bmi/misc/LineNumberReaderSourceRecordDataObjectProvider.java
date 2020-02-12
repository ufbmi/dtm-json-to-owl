package edu.ufl.bmi.misc;

import java.io.IOException;
import java.io.LineNumberReader;
import java.util.HashMap;
import java.util.regex.Pattern;

/**
 *	This class assumes a header row with field names.
*/
public class LineNumberReaderSourceRecordDataObjectProvider extends ReaderSourceRecordDataObjectProvider {

	public static String DEFAULT_FIELD_DELIM = "\t";
	public static String DEFAULT_SUBFIELD_DELIM = ";";

	boolean useSubfields = false;

	String fieldDelim;
	String subfieldDelim;

	String keyFieldName;

	HashMap<String, Integer> fieldNameToIndex;

	/**
	 * 	Create with default field delimiter (tab character) and no subfield 
	 *    delimiter (usesSubfields = false).
	*/
	public LineNumberReaderSourceRecordDataObjectProvider(LineNumberReader lnr) {
		super(lnr);
		setDelims(DEFAULT_FIELD_DELIM, null);
	}

	public LineNumberReaderSourceRecordDataObjectProvider(LineNumberReader lnr, 
		String fieldDelim) {
		super(lnr);
		setDelims(DEFAULT_FIELD_DELIM, null);
	}

	public LineNumberReaderSourceRecordDataObjectProvider(LineNumberReader lnr, 
		String fieldDelim, String subfieldDelim) {
		super(lnr);
		setDelims(fieldDelim, subfieldDelim);
	}

	public void setDelims(String field, String subfield) {
		this.fieldDelim = field;
		this.subfieldDelim = subfield;
		if (this.subfieldDelim != null) {
			this.useSubfields = true;
		}
	}

	public void processSchema() throws IOException {
		String line = ((LineNumberReader)r).readLine();
		String[] flds = line.split(Pattern.quote(this.fieldDelim), -1);
		fieldNameToIndex = new HashMap<String, Integer>();
		for (int i=0; i<flds.length; i++) {
			if (flds[i].contains("[key]"))
				this.keyFieldName = flds[i];
			fieldNameToIndex.put(flds[i], Integer.valueOf(i));
		}
		if (this.keyFieldName == null)
			this.keyFieldName = flds[0];  //strong assumption
	}

	@Override 
	public DataObject getNextDataObject() {
		String line = null;
		try {
			line = ((LineNumberReader)r).readLine();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		RecordDataObject rdo = null;
		if (line != null) {
			rdo = new RecordDataObject(this.fieldNameToIndex, line, this.keyFieldName,
				 this.fieldDelim, true);
		}
		return rdo;
	}
}