package edu.ufl.bmi.misc;

import java.util.HashMap;
import java.util.regex.Pattern;
import java.io.File;
import java.io.IOException;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.util.regex.Matcher;

import org.semanticweb.owlapi.model.IRI;

public class IriLookup {
    HashMap<String, IRI> classIriMap;
    HashMap<String, IRI> objPropIriMap;
    HashMap<String, IRI> annPropIriMap;
    HashMap<String, IRI> individIriMap;
    HashMap<String, IRI> dataPropIriMap;

    String fname;

    public IriLookup(String fname) {
		this.fname = fname.replaceFirst("^~", Matcher.quoteReplacement(System.getProperty("user.home")));
		classIriMap = new HashMap<String, IRI>();
		objPropIriMap = new HashMap<String, IRI>();
		annPropIriMap = new HashMap<String, IRI>();
		individIriMap = new HashMap<String, IRI>();
		dataPropIriMap = new HashMap<String, IRI>();
    }

    public void init() throws IOException {
    	File f = new File(fname);
    	System.out.println("\t" + f.getAbsolutePath());
		FileReader fr = new FileReader(f.getAbsolutePath());
		LineNumberReader lnr = new LineNumberReader(fr);
		String line;
		while ((line=lnr.readLine())!=null) {
	    	String[] flds = line.split(Pattern.quote("\t"));
	    	if (flds.length == 0) continue;
	    	String handle = flds[0].trim();
	    	String iriTxt = flds[1].trim();
	    	String type = flds[2].trim();
	    	if (type.equals("class")) {
				classIriMap.put(handle, IRI.create(iriTxt));
	    	} else if (type.equals("object property")) {
				objPropIriMap.put(handle, IRI.create(iriTxt));
	    	} else if (type.equals("annotation")) {
				annPropIriMap.put(handle, IRI.create(iriTxt));
	    	} else if (type.equals("individual")) {
				individIriMap.put(handle, IRI.create(iriTxt));
	    	} else if (type.equals("data property")) {
	    		dataPropIriMap.put(handle, IRI.create(iriTxt));
	    	} else {
				System.err.println("iri type is: " + type + ", line is " + line);
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

    public IRI lookupIndividIri(String key) {
		return individIriMap.get(key);
    }

    public IRI lookupDataPropIri(String key) {
    	return dataPropIriMap.get(key);
    }
}