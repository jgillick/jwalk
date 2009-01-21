/**
 * Manages the helper functions provided to the JavaScript comment parser
 */
package com.jwalkjs;

import org.mozilla.javascript.ScriptableObject;

public class ParserHelpers extends ScriptableObject {

	public ParserHelpers(){ }
	public void jsConstructor( ) { }
	
	/**
	 * Get the the JS comments closest to the line number provided.
	 */
	public Element[] jsFunction_getCommentsForLine(){
		Element[] elems = new Element[2];
		return elems;
	}
	
	/**
	 * Prints a message to the console
	 */
	public void jsFunction_log(String msg){
		System.out.println(msg);
	}
	
	@Override
	public String getClassName() { 
		return "ParserHelpers";
	}
}