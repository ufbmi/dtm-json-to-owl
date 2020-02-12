package edu.ufl.bmi.misc;

import java.lang.Iterable;
import java.util.Iterator;

public abstract class DataObjectProvider implements Iterable<DataObject> {
	
	class IteratorImpl implements Iterator<DataObject> {
		DataObjectProvider p;
		DataObject currentObject;

		public IteratorImpl(DataObjectProvider provider) {
			this.p = provider;
			currentObject = provider.getNextDataObject();
		}

		public boolean hasNext() {
			return (currentObject != null);
		}

		public DataObject next() {
			DataObject returnObject = currentObject;
			currentObject = p.getNextDataObject();
			return returnObject;
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}

	}

	public Iterator<DataObject> iterator() {
		return new IteratorImpl(this);
	}

	public abstract boolean isReusable();

	protected abstract DataObject getNextDataObject();
}