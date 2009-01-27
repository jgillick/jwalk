package com.jwalkjs;

import java.util.Comparator;

/**
 * Used to sort the ElementDoc and ScriptFile elements by name or path
 */
public class ListCompare implements Comparator {

	public int compare(Object o1, Object o2){
		String name1, name2;

		if( o1 instanceof ElementDoc ){
			name1 = ( (ElementDoc)o1 ).name.toLowerCase();
			name2 = ( (ElementDoc)o2 ).name.toLowerCase();
		} else if( o1 instanceof ScriptFile ){
			name1 = ( (ScriptFile)o1 ).path.toLowerCase();
			name2 = ( (ScriptFile)o2 ).path.toLowerCase();
		} else {
			return 0;
		}

		return name1.compareTo(name2);
	}

	public boolean equals(Object obj){
		return super.equals(obj);
	}
}
