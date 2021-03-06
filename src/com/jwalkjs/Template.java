package com.jwalkjs;

import java.io.*;
import java.util.*;

import org.mozilla.javascript.*;

/**
 * A simple JavaScipt templating system
 */
public class Template {

	private File tmplRoot;
	private Context cx;
	private ScriptableObject scope;
	private boolean debug = false;

	/**
	 * The characters that start the executable code in the template file
	 */
	public String startTag = "<%";

	/**
	 * The characters that end the executable code in the template file
	 */
	public String endTag = "%>";

	/**
	 * Sets up the templating environment with a template root directory
	 * @param templateRoot The root directory that all the template are included under.
	 * 						This quarentines the scripts from including anything outside that directory.
	 */
	public Template(String templateRoot) throws IOException {
		this( new File(templateRoot) );
	}

	/**
	 * Sets up the templating environment with a template root directory
	 * @param templateRoot The root directory that all the template are included under.
	 * 						This quarentines the scripts from including anything outside that directory.
	 */
	public Template(File templateRoot) throws IOException {

		// Template root exists?
		if( !templateRoot.exists() ){
			throw new FileNotFoundException("The template root '"+ tmplRoot.getPath() +"' doesn't exist.");
		}

		tmplRoot = new File(templateRoot.getCanonicalPath()); // need canonical path to do template file location checking -- don't let templates include a file from outside the tmplRoot
		cx = (new ContextFactory()).enterContext();
	}

	/**
	 * Close the template and all the scripting resources opened
	 */
	public void close(){
		Context.exit();
	}

	/**
	 * Find and run the template dispatch script in the root template directory.
	 * The dispatch script is a JavaScript file (by default 'templates.js') that handles how to generate the templates.
	 * @param out The output directory to save the files to
	 * @param files The parsed and documented JavaScript files
	 */
	public void dispatch( File out, ScriptFile[] files ) throws Exception {
		dispatch( "Templates.js", out, files );
	}

	/**
	 * Find and run the template dispatch script in the root template directory.
	 * The dispatch script is a JavaScript file (by default 'Templates.js') that handles how to generate the templates.
	 * @param scriptName The dispatch script name
	 * @param out The output directory to save the files to
	 * @param files The parsed and documented JavaScript files
	 */
	public void dispatch( String scriptName, File out, ScriptFile[] files ) throws Exception{

		// Get dispatch file
		File dispatch = new File(tmplRoot, scriptName);
		if( !dispatch.exists() ){
			throw new FileNotFoundException( "The template dispatch script '"+ scriptName +"', was not found in '"+ tmplRoot.getPath() +"'." );
		}

		// Setup environment
		scope = cx.initStandardObjects();
		scope.associateValue("output_writer", System.out);
		scope.associateValue("template", this);
		scope.associateValue("doc_root", tmplRoot.getAbsolutePath());
		scope.associateValue("doc_out", out.getCanonicalFile()); // need full path for validation
		JSHelpers.load(scope, new String[]{ "template" });

		// Run dispatch script
		cx.evaluateReader(scope, new FileReader(dispatch), dispatch.getAbsolutePath(), 1, null);

		Scriptable tmplObj = (Scriptable) scope.get("Templates", scope);
		Function tmplFunc = (Function) ScriptableObject.getProperty(tmplObj, "createTemplates");
		tmplFunc.call(cx, scope, tmplObj, new Object[]{ files });
	}

	/**
	 * Parse a template file
	 * @param template The path to a template file to process
	 * 					This needs to exist under the 'templateRoot' path.
	 * @param out The file to save the output to.  This path can exist anywhere
	 * @param globals A Map of global properties you want available to the template
	 */
	public void parse(String template, String out, Map<String, Object> globals)
		throws IOException {

		File tmplFile = new File(tmplRoot, template);
		BufferedOutputStream fileOut = new BufferedOutputStream( new FileOutputStream( out ) );
		parse( tmplFile, fileOut, globals );
		fileOut.close();
	}

	/**
	 * Parse a template file
	 * @param template The path to a template file to process
	 * 					This needs to exist under the 'templateRoot' path.
	 * @param out The file to save the output to.  This path can exist anywhere
	 * @param globals A Map of global properties you want available to the template
	 */
	public void parse(File template, File out, Map<String, Object> globals)
		throws IOException {

		BufferedOutputStream fileOut = new BufferedOutputStream( new FileOutputStream( out ) );
		parse( template, fileOut, globals );
		fileOut.close();
	}

	/**
	 * Parse a template file
	 * @param template The path to a template file to process
	 * 					This needs to exist under the 'templateRoot' path.
	 * @param out The output stream that the processed template will be pushed to.
	 * @param globals A Map of global properties you want available to the template
	 */
	public void parse(String template, String out, ScriptableObject globals)
		throws IOException {

		// Convert JS object into globals map
		String key;
		Map<String, Object> globalMap = new HashMap<String, Object>();
		Object[] props = ScriptableObject.getPropertyIds(globals);
		for( int i = 0; i < props.length; i++ ){
			key = (String)props[i];
			globalMap.put(key, ScriptableObject.getProperty(globals, key));
		}

		// Setup template and output
		File tmplFile = new File(tmplRoot, template);
		BufferedOutputStream fileOut = new BufferedOutputStream( new FileOutputStream( out ) );

		// Parse
		parse( tmplFile, fileOut, globalMap );
		fileOut.close();
	}

	/**
	 * Parse a template file
	 * @param template The path to a template file to process
	 * 					This needs to exist under the 'templateRoot' path.
	 * @param out The output stream that the processed template will be pushed to.
	 * @param globals A Map of global properties you want available to the template
	 */
	public void parse(File template, OutputStream out, Map<String, Object> globals)
		throws IOException {

		// Exists and is under template root?
		template = template.getCanonicalFile();
		if( template.getPath().indexOf( tmplRoot.getPath() ) != 0){
			throw new IOException("The file '"+ template.getName() +"' is not within '"+ tmplRoot.getPath() +"'.\n" +
					"You cannot include files outside of the template set directory!");
		} else if ( !template.exists() ){
			throw new FileNotFoundException("The file '"+ template.getPath() +"' does not exist.");
		}

		// Read template file
		StringBuilder contents = new StringBuilder();
		BufferedReader input =  new BufferedReader( new FileReader(template) );
		try{
			String line;
			while (( line = input.readLine()) != null){
	          contents.append(line);
	          contents.append(System.getProperty("line.separator"));
	        }
		} finally{
			input.close();
		}

		// JS Environment
		scope = cx.initStandardObjects();
		scope.associateValue("template", this);
		scope.associateValue("doc_root", tmplRoot.getAbsolutePath());
		scope.associateValue("output_writer", out);
		JSHelpers.load(scope);

		// Add globals
		if( globals != null && globals.size() > 0){
			Object[] keys = globals.keySet().toArray();
			String key;
			Object value;
			for( int i = 0; i < keys.length; i++ ){
				key = (String)keys[i];
				value = Context.javaToJS( globals.get(key), scope );

				ScriptableObject.defineProperty(scope, key, value, 0);
			}
		}

		// Convert and execute
		String tmplJS = convertTemplate(contents.toString());
		if( debug ){
			System.out.println(tmplJS);
		}
		cx.evaluateString(scope, tmplJS, template.getPath(), 1, null);

		out.flush();
		out.close();
		scope = null;
	}

	/**
	 * Convert the template file into JavaScript code that can be executed by Rhino
	 */
	public String convertTemplate(String template){
		StringBuilder js = new StringBuilder();

		int tmplLen, endLen, cursor;
		boolean inPrint, inComment, escaped;
		char quote = 0, curr;

		// With each pass the process part of the template is removed until
		// the entire template is consumed
		while(template.length() > 0){
			tmplLen = template.length();
			inPrint = false;
			inComment = false;

			// Find the next start tag
			cursor = template.indexOf(startTag);
			if( cursor > -1 ){

				// Print the content stuff
				if( cursor > 0 ){
					js.append( "print(\"" );
					js.append( escape( template.substring(0, cursor) ) );
					js.append( "\");" );
				}

				cursor += startTag.length();

				// Special tag shorthands
				switch( template.charAt(cursor) ){
				case '=': // A printable statement: <%= 'foo' %>
					js.append( "print(" );
					inPrint = true;
					cursor++;
					break;

				case '#': // Comment statement: <%# Don't execute this -- ever %>
					inComment = true;
					cursor++;
					break;

				default:
					// No extensions present
					inPrint = false;
					inComment = false;
				}

				// Find the end of the executable code, but ignore tags inside quotes
				escaped = false;
				quote = 0;
				endLen = endTag.length();
				for(; cursor < tmplLen; cursor++ ){
					curr = template.charAt(cursor);

					// Quotes
					if( curr == '\'' || curr == '"'){

						// Start quote
						if( quote == 0 ){
							quote = curr;
						}
						// End quote
						else if( !escaped && quote == curr ){
							quote = 0;
						}
					}

					// Escape backslashes
					escaped = ( curr == '\\' && !escaped);


					// Peek and the next few characters for the end tag
					if ( quote == 0
							&& (cursor + endLen - 1) < tmplLen
							&& template.substring(cursor, cursor + endLen).equals( endTag ) ){

						// Finish the print
						if( inPrint ){
							js.append( ")" );
						}
						js.append( ";" );

						// Cut the template down
						cursor += endLen;
						template = template.substring(cursor);
						cursor = 0;
						break;
					}

					// Append character
					else if( !inComment || curr == '\n'){
						js.append(curr);
					}
				}

				// EOF
				if( cursor >= tmplLen){
					break;
				}

			}

			// EOF -- place the rest in a print
			else if( tmplLen > 0 ){
				js.append( "print(\"" );
				js.append( escape( template.substring(0) ) );
				js.append( "\");" );
				break;
			}

		}


		return js.toString();
	}

	/**
	 * Escape characters which will be quoted
	 */
	private String escape(String code){

		code = code.replaceAll("\\\\", "\\\\"); // Convert '\' to '\\'
		code = code.replaceAll("\"", "\\\\\""); // Convert '"' to '\"'
		code = code.replaceAll("\n", "\"+\n\"\\\\n"); // Convert '\n' to '"+\n"'

		return code;
	}

}
