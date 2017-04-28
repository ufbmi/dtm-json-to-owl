package edu.ufl.bmi.misc;

import java.io.FileReader;
import java.io.LineNumberReader;
import java.io.IOException;

import java.util.HashMap;
import java.util.regex.Pattern;

import org.semanticweb.owlapi.model.IRI;

public class PublicationLinks {
    HashMap<String, IRI> pubEntryToIri;
    String fName;

    public PublicationLinks(String fName) {
	this.fName = fName;
	pubEntryToIri = new HashMap<String, IRI>();
    }
    
    public void init() throws IOException {
	FileReader fr = new FileReader(fName);
	LineNumberReader lnr = new LineNumberReader(fr);
	String line;
	while((line=lnr.readLine())!=null) {
	    String[] flds = line.split(Pattern.quote("\t"));
	    pubEntryToIri.put(flds[3], IRI.create(flds[0]));
	}
    }

    public IRI get(String key) {
	return pubEntryToIri.get(key);
    }
}