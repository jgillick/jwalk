package com.jwalkjs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Represents a JavaScript source file and it's global element
 */
public class ScriptFile {

	/**
	 * File path to the script file.
	 */
	public String path = "";

	/**
	 * Script file name
	 */
	public String name = "";

	/**
	 * The source contents of the script file
	 */
	public String source = "";

	/**
	 * The 'virtual' global scope element for this script.
	 */
	public Element global = null;

	/**
	 * All the comments in the source code.
	 */
	public ArrayList<Comment> comments = new ArrayList<Comment>();

	/**
	 * Reads the contents of the source file
	 * @param path The path to the JavaScript file to read.
	 * @throws IOException
	 */
	protected ScriptFile(String path) throws IOException{
		this ( new File(path) );
	}
	
	/**
	 * Reads the contents of the source file
	 * @param path The JavaScript file to read.
	 * @throws IOException
	 */
	protected ScriptFile(File sourceFile) throws IOException{

		this.path = sourceFile.getPath();
		this.name = sourceFile.getName();

		// Read file contents
		String line;
		StringBuilder content = new StringBuilder();
		BufferedReader reader = new BufferedReader( new FileReader(sourceFile) );
		while((line = reader.readLine()) != null){
			content.append(line);
			content.append(System.getProperty("line.separator"));
		}

		source = content.toString();
	}


}
