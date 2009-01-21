/**
* The 'manageTemplates' function will be passed an array of source file element trees
* and will decide which templates are processed.
*
* @name Default Templates
* @version 0.1
* @author Jeremy Gillick
* @homepage http://blog.mozmonkey.com/
* @jwalkversion 0.1
*/


/**
* JWalk passes it an array of file element trees and the function decides
* which templates will be processed and where the output should go.
*
*/
function manageTemplates(files){

	JWalk.parseTemplate("index.tmpl", "index.html", { files: files });
	JWalk.parseTemplate("all_files.tmpl", "files.html", { files: files });

	var file, filename;
	for(var i = 0; i < files.length; i++){
		file = files[i];
		filepath = file.path +"/"+ file.name.replace(".js", "");
		JWalk.parseTemplate("file.tmpl", filepath + ".html", { file: file[i] });
		readElements(filepath, file.getChildren());
	}
}

/**
* Loops through all the child elements and creates output files for all objects, constructors and global functions.
*/
function readElements(filepath, elements){

	var elem, output;
	for(var i = 0; i < elements.length; i++){
		elem = elements[i];

		output = filepath
		output += elem.getObjectHierarchy().replace(".", "/");
		output += elem.name +".html";

		switch(elem.type){
			case com.jwalk.Element.OBJ:
				JWalk.parseTemplate("object.tmpl", output, { object: elem });
				readElements(filepath, elem.getChildren());
				break;
			case com.jwalk.Element.FUNC:
				if(elem.isConstructor()){
					JWalk.parseTemplate("constructor.tmpl", output, { func: elem });
				}
				else{
					JWalk.parseTemplate("function.tmpl", output, { constructor: elem });
				}
				break;
		}
	}

}

