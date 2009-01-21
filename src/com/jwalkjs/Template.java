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
	public Template(String templateRoot) 
		throws FileNotFoundException {
		this( new File(templateRoot) );
	}
	
	/**
	 * Sets up the templating environment with a template root directory
	 * @param templateRoot The root directory that all the template are included under.
	 * 						This quarentines the scripts from including anything outside that directory.
	 */
	public Template(File templateRoot) 
		throws FileNotFoundException {
		
		// Template root exists?
		if( !templateRoot.exists() ){
			throw new FileNotFoundException("The template root '"+ tmplRoot.getPath() +"' doesn't exist.");
		}
		
		tmplRoot = templateRoot;
		cx = (new ContextFactory()).enterContext();
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
		parse( tmplFile, new File(out), globals );
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
		parse( template, new FileWriter(out), globals );
	}

	/**
	 * Parse a template file
	 * @param template The path to a template file to process
	 * 					This needs to exist under the 'templateRoot' path. 
	 * @param out The output stream that the processed template will be pushed to.
	 * @param globals A Map of global properties you want available to the template
	 */
	public void parse(File template, Writer out, Map<String, Object> globals)
		throws IOException {
		
		// Exists and is under template root?
		if( !template.exists() || tmplRoot.getCanonicalPath().indexOf( template.getParentFile().getCanonicalPath() ) == -1){
			throw new FileNotFoundException("The file '"+ template.getPath() +"' does not exist in '"+ tmplRoot.getPath() +"'");
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
		String[] jshelpers = { "print" };
		scope.defineFunctionProperties(jshelpers, Template.class, ScriptableObject.DONTENUM);
		
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
		scope.associateValue("output_writer", out);
		String tmplJS = convertTemplate(contents.toString());
		cx.evaluateString(scope, tmplJS, template.getPath(), 1, null);
		
		out.flush();
		out.close();
	}
	
	/**
	 * Convert the template file into JavaScript code that can be executed by Rhino
	 */
	private String convertTemplate(String template){
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
		code = code.replaceAll("\"", "\\\""); // Convert '"' to '\"'
		code = code.replaceAll("\n", "\"+\n\""); // Convert '\n' to '"+\n"'
		
		return code;
	}
	
		
	/**
	 * A JavaScript helper that prints to the output writer
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
				Writer out = (Writer) ( (ScriptableObject) thisObj ).getAssociatedValue("output_writer");
				out.write(content);
			}
		} catch (IOException ex){
			throw new Exception("Could not find or access the output writer.");
		}
		
	}
	
}
