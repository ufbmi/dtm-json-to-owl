package edu.ufl.bmi.misc;

import java.io.IOException;
import java.io.Reader;

public abstract class ReaderSourceRecordDataObjectProvider extends DataObjectProvider {
	
	Reader r;

	public ReaderSourceRecordDataObjectProvider(Reader r) {
		this.r = r;
	}

	public void closeReader() throws IOException {
		r.close();
	}

	/**
	 *  This type of object is not reusable because Reader objects
	 *		are "one and done."  Once you process through the 
	 *		character-based input, you cannot but with some
	 *		exceptions (and even many of those are ill behaved like
	 *		reset()) go back to the beginning and taunt you a 
	 *		second time...process the input a second time.
	 */
	@Override
	public boolean isReusable() {
		return false;
	}
}