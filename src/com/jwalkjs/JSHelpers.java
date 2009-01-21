/**
 * Manages the JavaScript helper methods which are made available to 
 * JS files being executed.
 */
package com.jwalkjs;

import org.mozilla.javascript.ScriptableObject;

public class JSHelpers extends ScriptableObject {

	public JSHelpers(){ }
	
	/**
	 * Prints a message to the console
	 */
	public void print(String msg){
		System.out.println(msg);
	}
	
	public String getClassName() { 
		return "JSHelpers";
	}
}
