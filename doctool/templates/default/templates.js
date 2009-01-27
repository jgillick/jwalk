/**
* The default template set for JWalk.
* @author Jeremy Gillick
* @url http://jsjwalk.com
*/

include("assets/common.js");

var DefaultTemplates = {

	/**
	 * All the JS files that were parsed
	 * @type {ScriptFile[]}
	 */
	files : [],

	/**
	 * All the top-level objects for all files
	 * @type {ElementDoc[]}
	 */
	objects : [],

	/**
	 * Create all the templates from the files list
	 * @param {ScriptFile[]} files All the script files that were processed
	 */
	createTemplates : function( files ){
		this.files = files;

		// Extract all top-level objects from the files
		for( var i = 0, len = files.length; i < len; i++ ){
			var top = files[i].global.getChildren( SYMBOL_OBJECT );

			// Add to array
			var allLen = this.objects.length;
			for ( var n = 0, objLen = top.length; n < objLen; n++ ){
				this.objects[ allLen + n ] = top[n];
			}
		}

		// Build templates
		this.createIndex();
		this.allFiles();
		this.allObjects();
	},

	/**
	 * Create the index page.
	 */
	createIndex : function(){
		template("index.tmpl", "index.html", { files : this.files, objects : this.objects });
	},

	/**
	 * File pages
	 */
	allFiles : function(){

		// Main file list
		template("all_files.tmpl", "files.html", { files : files });

		// Template for each file
		var files = this.files;
		var file, elements, path, out;
		for( var i = 0, len = files.length; i < len; i++ ){
			file = files[i];
			out = pathForScript(file);

			template("each_file.tmpl", out, { file: file,
											doc: file.global,
											objects: file.global.getChildren( SYMBOL_OBJECT),
											variables: file.global.getChildren( SYMBOL_VARIABLE),
											functions: file.global.getChildren( SYMBOL_FUNCTION) });
		}
	},

	/**
	 * Build a template for all object files
	 */
	allObjects : function(){

	}
}
var Templates = DefaultTemplates;