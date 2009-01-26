/**
 * Manages the JavaScript helper methods which are made available to
 * JS files being executed.
 */
package com.jwalkjs;

import java.io.*;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.NativeJavaObject;

public class JSHelpers {

	/**
	 * Load the helper methods into the JavaScript scope.
	 * By default this loads the 'print' and 'include' functions.
	 * @param scope The JavaScript scope
	 */
	protected static void load(ScriptableObject scope){
		load(scope, new String[0]);
	}

	/**
	 * Load the helper methods into the JavaScript scope.
	 * By default this loads the 'print' and 'include' functions.
	 * @param scope The JavaScript scope
	 * @param include List of the non-default script functions to include
	 */
	protected static void load(ScriptableObject scope, String[] include){
		String[] defnames = { "print", "include" };

		scope.defineFunctionProperties(defnames, JSHelpers.class, ScriptableObject.DONTENUM);
		scope.defineFunctionProperties(include, JSHelpers.class, ScriptableObject.DONTENUM);
	}

	/**
	 * A simple wrapper function to get the associated value from the scope object
	 * @param scope The scope object to get the value from.
	 * 				This needs to be an instance of ScriptableObject otherwise the function will return NULL.
	 * @param key The key used to retrieve the value
	 */
	private static Object getAssociatedValue(Scriptable scope, Object key){
		if (scope instanceof ScriptableObject) {
			ScriptableObject scopeObj = (ScriptableObject)scope;
			return scopeObj.getAssociatedValue(key);
		}
		return null;
	}

	/**
	 * A JavaScript helper that prints to the output writer
	 * Requires the 'output_writer' property to be set via scope.associateValue.
	 * 		For example: scope.associateValue("output_writer", System.out);
	 */
	public static void print(Context cx, Scriptable scope, Object[] args, Function funObj)
		throws Exception{
		if( args.length == 0 ){
			return;
		}

		// Convert to String
		String content;
		content = (String)Context.jsToJava((Object)args[0], String.class);

		// Get writer and output
		try {
			OutputStream out = (OutputStream) getAssociatedValue(scope, "output_writer");
			if( out != null ){

				if( out instanceof PrintStream){
					((PrintStream) out).println(content);
				}
				else{
					out.write(content.getBytes());
				}
				out.flush();
			}
		} catch (IOException ex){
			throw new Exception("Could not find or access the output writer.");
		}

	}

	/**
	 * Includes and executes a JavaScript file
	 * This requires 2 properties to be set by scope.associateValue
	 * 		- doc_root: The directory root where all the includable script files exist
	 * 		- template: The Template instance or null
	 */
	public static void include(Context cx, Scriptable scope, Object[] args, Function funObj)
		throws Exception{
		if( args.length == 0 ){
			return;
		}

		String filePath = (String)args[0];
		String docRoot = (String)getAssociatedValue(scope, "doc_root");
		Template template = (Template)getAssociatedValue(scope, "template");
		Exception fileEx = new Exception("Could not read file '"+ filePath +"', are you sure it exists under '"+ docRoot +"'");

		File file = new File(docRoot, filePath);
		if( !file.exists() ){
			throw fileEx;
		}

		// Read file
		StringBuilder contents = new StringBuilder();
		BufferedReader input =  new BufferedReader( new FileReader(file) );
		try{
			String line;
			while (( line = input.readLine()) != null){
	          contents.append(line);
	          contents.append(System.getProperty("line.separator"));
	        }
		} catch(IOException ex){
			throw fileEx;
		} finally{
			input.close();
		}

		// Prepare source
		String src = contents.toString();
		if( template != null ){
			src = template.convertTemplate(src);
		}

		// Execute
		Script script = cx.compileString(src, file.getPath(), 0, null);
		script.exec(cx, scope);
	}
}
