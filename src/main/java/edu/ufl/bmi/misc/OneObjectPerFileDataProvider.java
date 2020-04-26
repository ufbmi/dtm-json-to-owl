package edu.ufl.bmi.misc;

import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Pattern;

public class OneObjectPerFileDataProvider extends DataObjectProvider {

	String listOfFilesFileName;
	String attributeValueDelimiter;
	String attributeSequenceDelimiter;
	String attributeArrangementInFile;
	String multipleValueType;
	String keyField;

	ArrayList<String> listOfFiles;
	ArrayList<AttributeValueDataObject> dataObjectList;
	Iterator<AttributeValueDataObject> iterator;

	public OneObjectPerFileDataProvider(String keyField, String objectFileListFileName, String attributeArrangementInFile, 
		String multipleValueType, String multipleValueElementDelimiter, String elementValueDelimiter){
		this.keyField = keyField;
		this.listOfFilesFileName = objectFileListFileName;
		this.attributeArrangementInFile = attributeArrangementInFile;
		//this.attributeSequenceDelimiter = Pattern.quote(multipleValueElementDelimiter);
		this.attributeSequenceDelimiter = multipleValueElementDelimiter;
		this.multipleValueType = multipleValueType;
		this.attributeValueDelimiter = elementValueDelimiter;
		this.dataObjectList = new ArrayList<AttributeValueDataObject>();
	}

	public void initialize() {
		try {
			if (this.listOfFiles == null && this.listOfFilesFileName != null) {
				loadFileNames();
			}

			processListOfFiles();
			iterator = dataObjectList.iterator();

		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	protected void processListOfFiles() throws IOException {
		for (String fName : this.listOfFiles) {
			FileReader fr = new FileReader(fName);
			LineNumberReader lnr = new LineNumberReader(fr);

			String line;
			AttributeValueDataObject dObject = new AttributeValueDataObject(keyField);
			while((line=lnr.readLine())!=null) {
				if (line.startsWith("\uFEFF")) line = line.substring(1);
				String[] flds = line.split(this.attributeValueDelimiter, 2);
				if (flds.length == 1) {
					System.err.println(flds.length);
					System.err.println(flds[0]);
					continue;
				}
				if (flds[0].contains(this.attributeSequenceDelimiter)) {
					String[] subFlds = flds[0].split(Pattern.quote(this.attributeSequenceDelimiter), -1);
					int position = Integer.parseInt(subFlds[1]);
					dObject.addAttributeValueAtPosition(subFlds[0], flds[1], position);
				} else {
					dObject.addAttributeValue(flds[0], flds[1]);
				}
			}
			this.dataObjectList.add(dObject);

			lnr.close();
			fr.close();
		}
	}

	protected void loadFileNames() throws IOException {
		FileReader fr = new FileReader(this.listOfFilesFileName);
		LineNumberReader lnr = new LineNumberReader(fr);
		this.listOfFiles = new ArrayList<String>();

		String line;
		while ((line=lnr.readLine())!=null) {
			if (line.startsWith("\uFEFF")) line = line.substring(1);
			this.listOfFiles.add(line);
		}
	}

	public boolean isReusable() {
		return true;
	}

	protected DataObject getNextDataObject() {
		AttributeValueDataObject dObject;
		if (iterator.hasNext()) {
			dObject = iterator.next();
		} else {
			dObject = null;
			iterator = dataObjectList.iterator();
		}
		return dObject;
	}
}