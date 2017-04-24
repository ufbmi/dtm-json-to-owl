package edu.ufl.bmi.misc;

import java.io.IOException;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.util.regex.Pattern;
import java.util.HashMap;

import org.semanticweb.owlapi.model.IRI;

public class ControlMeasureIriMapping {

    String fName;

    HashMap<String, IRI> map;

    public ControlMeasureIriMapping(String fName) {
	this.fName = fName;
	map = new HashMap<String, IRI>();
    }

    public void initialize() throws IOException {
	FileReader fr = new FileReader(fName);
	LineNumberReader lnr = new LineNumberReader(fr);
	String line;
	while ((line=lnr.readLine()) != null) {
	    String[] flds = line.split(Pattern.quote("\t"));
	    String key = flds[0];
	    IRI value = IRI.create(flds[1]);
	    map.put(key, value);
	}
	fr.close();
	lnr.close();
    }

    public IRI get(String key) {
	return map.get(key);
    }

    public boolean containsKey(String key) {
	return map.containsKey(key);
    }
}