package edu.ufl.bmi.misc;

import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Pattern;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.io.IOException;

public class IndividualsToCreate {
    String fname;
    HashMap<String, HashSet<String>> indsMap;
    boolean init = false;
    
    public IndividualsToCreate(String fname) {
	this.fname = fname;
	indsMap = new HashMap<String, HashSet<String>>();
    }

    public void init() throws IOException {
	FileReader fr = new FileReader(fname);
	LineNumberReader lnr = new LineNumberReader(fr);
	String line;
	while ((line=lnr.readLine())!=null) {
	    String[] flds = line.split(Pattern.quote("|"));
	    HashSet<String> inds = new HashSet<String>(); 
	    if (flds.length > 1) {
		String[] flds2 = flds[1].split(Pattern.quote(","));
		for (int i=0; i<flds2.length; i++) {
		    inds.add(flds2[i]);
		}
		indsMap.put(flds[0], inds);
	    }
	}
	init = true;
    }

    public HashMap<String, HashSet<String>> getIndividualsToCreate() {
	if (!init) throw new IllegalStateException("must call init() first");
	return indsMap;
    }

}