/**
 * Manages the JavaScript helper methods which are made available to 
 * JS files being executed.
 */
package com.jwalkjs;

import java.io.IOException;
import java.io.Writer;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

public class JSHelpers {
	
	/**
	 * Load the helper methods into the JavaScript scope.
	 * @param scope The JavaScript scope
	 */
	protected static void load(ScriptableObject scope){
		String[] names = { "print" };
		scope.defineFunctionProperties(names, JSHelpers.class, ScriptableObject.DONTENUM);
	}
	
	/**
	 * A JavaScript helper that prints to the output writer
	 * Requires the 'output_writer' property to be set via scope.associateValue.
	 * 		For example: scope.associateValue("output_writer", System.out);
	 */
	public static void print(Context cx, Scriptable thisObj, Object[] args, Function funObj) 
		throws Exception{
		
		// Content to print
		if( args.length == 0 ){
			return;
		}
		String content =  (String)args[0];
		
		// Get writer and output
		try {
			if (thisObj instanceof ScriptableObject) {
				ScriptableObject scope = (ScriptableObject) thisObj; 
				Writer out = (Writer) scope.getAssociatedValue("output_writer");
				out.write(content);
			}
		} catch (IOException ex){
			throw new Exception("Could not find or access the output writer.");
		}
		
	}
}
