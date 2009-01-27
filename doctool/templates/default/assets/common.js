
/**
 * Get output path for a ScriptFile
 * @param {ScriptFile} scriptFile A JavaScript source ScriptFile
 */
function pathForScript(scriptFile){
	var fileInfo = fileParts(scriptFile.path);
	var out = "files/"+ fileInfo.path + fileInfo.name +".html";

	return out;
}

/**
 * Extract the file name and path from a full file path string
 * @param {String} path A full file path
 * @returns {Hash} A hash array with 'name', 'extension' and 'path' keys
 */
function fileParts( path ){
	var parts = { path: '', name: path, extension: '' }
	var match;

	// Separate path from name
	if( ( match = path.match(/^(.*?\/)([^\/]*$)/) ) != null ){
		parts.path = match[1];
		parts.name = match[2];
	}

	// Separate name and extension
	if( ( match = parts.name.match(/(.*?)\.([^\.]*$)/) ) != null ){
		parts.name = match[1];
		parts.extension = match[2];
	}

	return parts;
}