package edu.ufl.bmi.misc;

import java.util.ArrayList;
import java.io.IOException;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.util.regex.Pattern;

import edu.ufl.bmi.misc.DtmIndividConnectRule;

public class DtmIndivConnectGuide {

    String fname;
    ArrayList<DtmIndividConnectRule> rules;

    public DtmIndivConnectGuide(String fname) {
	this.fname = fname;
	rules = new ArrayList<DtmIndividConnectRule>();
    }

    public void init() throws IOException {
	FileReader fr = new FileReader(fname);
	LineNumberReader lnr = new LineNumberReader(fr);
	String line;
	while((line=lnr.readLine())!=null) {
	    String[] flds = line.split(Pattern.quote("\t"));
	    String sourceKey = flds[0];
	    String objPropKey = flds[1];
	    String targetKey = flds[2];
	    DtmIndividConnectRule rule = new DtmIndividConnectRule(sourceKey, objPropKey, targetKey);
	    rules.add(rule);
	}
    }

    public ArrayList<DtmIndividConnectRule> getRules() {
	return rules;
    }
}