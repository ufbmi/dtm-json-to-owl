package edu.ufl.bmi.misc;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.io.File;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.io.IOException;

public class SoftwareIoInfo {
	
	protected HashMap<String, ArrayList<String>> softwareInputs;
	protected HashMap<String, ArrayList<String>> softwareOutputs;

	public SoftwareIoInfo() {
		softwareInputs = new HashMap<String, ArrayList<String>>();
		softwareOutputs = new HashMap<String, ArrayList<String>>();
	}

	public void init(String pathAndFileName) throws IOException {
		File f = new File(pathAndFileName);
		FileReader fr = new FileReader(f);
		LineNumberReader lnr = new LineNumberReader(fr);

		String line;
		while ((line=lnr.readLine())!=null) {
			String[] flds = line.split(Pattern.quote("\t"));

			String label = flds[0];
			String inputs = flds[1];
			String outputs = flds[2];

			processDelimitedIoList(softwareInputs, inputs, label);
			processDelimitedIoList(softwareOutputs, outputs, label);
		}
	}

	protected void processDelimitedIoList(HashMap<String, ArrayList<String>> map, String ioList, String key) {
		String[] flds = ioList.split(Pattern.quote(","));

		ArrayList<String> io = new ArrayList<String>();
		for (String fld : flds) {
			if (fld != null && fld.length() != 0) {
				io.add(fld);
			}
		}

		map.put(key, io);
	}

	protected ArrayList<String> getIoListForLabel(HashMap<String, ArrayList<String>> map, String key) {
		return map.get(key);
	}

	public ArrayList<String> getInputListForLabel(String label) {
		return getIoListForLabel(softwareInputs, label);
	}

	public ArrayList<String> getOutputListForLabel(String label) {
		return getIoListForLabel(softwareOutputs, label);
	}
}