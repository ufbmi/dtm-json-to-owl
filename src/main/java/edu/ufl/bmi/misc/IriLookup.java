package edu.ufl.bmi.misc;

import java.util.HashMap;
import java.util.regex.Pattern;
import java.io.IOException;
import java.io.FileReader;
import java.io.LineNumberReader;

import org.semanticweb.owlapi.model.IRI;

public class IriLookup {
    HashMap<String, IRI> classIriMap;
    HashMap<String, IRI> objPropIriMap;
    HashMap<String, IRI> annPropIriMap;

    String fname;

    public IriLookup(String fname) {
	this.fname = fname;
	classIriMap = new HashMap<String, IRI>();
	objPropIriMap = new HashMap<String, IRI>();
	annPropIriMap = new HashMap<String, IRI>();
    }

    public void init() throws IOException {
	FileReader fr = new FileReader(fname);
	LineNumberReader lnr = new LineNumberReader(fr);
	String line;
	while ((line=lnr.readLine())!=null) {
	    String[] flds = line.split(Pattern.quote("\t"));
	    if (flds[2].equals("class")) {
		classIriMap.put(flds[0], IRI.create(flds[1]));
	    } else if (flds[2].equals("object property")) {
		objPropIriMap.put(flds[0], IRI.create(flds[1]));
	    } else if (flds[2].equals("annotation")) {
		annPropIriMap.put(flds[0], IRI.create(flds[1]));
	    } else {
		System.err.println("iri type is: " + flds[2] + ", line is " + line);
	    }
	}
    }

    public IRI lookupClassIri(String key) {
	return classIriMap.get(key);
    }

    public IRI lookupObjPropIri(String key) {
	return objPropIriMap.get(key);
    }

    public IRI lookupAnnPropIri(String key) {
	return annPropIriMap.get(key);
    }
}