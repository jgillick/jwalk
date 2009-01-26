/**
* The default template set for JWalk.
* @author Jeremy Gillick
* @url http://jsjwalk.com
*/

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
			var top = files[i].global.doc.getChildren( ElementDoc.OBJECT );
			
			// Add to array
			var allLen = this.objects.length;
			for ( var n = 0, objLen = top.length; n < objLen; n++ ){
				this.objects[ allLen + n ] = top[n];
			}
		}
		
		// Build templates
		createIndex();
		allFiles();
		allObjects();
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
		template("all-files.tmpl", "all-files.html", { files : file, doc : file.global.doc });
		
		// Template for each file
		var files = this.files;
		var file, elements, path, out;
		for( var i = 0, len = files.length; i < len; i++ ){
			file = files[i];
			fileInfo = fileParts(file);
			out = "files/"+ fileInfo.path + "/"+ fileInfo.name +"/.html";
			
			template("each-file.tmpl", out, { files : file, doc : file.global.doc });
		}
	},
	
	/**
	 * Build a template for all object files
	 */
	allObjects : function(){

	},
		
	/**
	 * Extract the file name and path from a full file path string
	 * @param {String} path A full file path
	 * @returns {Hash} A hash array with 'name', 'extension' and 'path' keys  
	 */
	fileParts : function( path ){
	}
}
var Templates = DefaultTemplates; 