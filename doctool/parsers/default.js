/**
* The default doc parsing script.
* This follows the JavaDoc doc comment style.
*
* @name Default Parser
* @version 0.1
* @author Jeremy Gillick
* @homepage http://blog.mozmonkey.com/
* @jwalkversion 0.1
*/

/**
* Main parsing object.  Name yours something different and unique.
*/
var DefaultParser = {	
	
	/**
	 * Parses a single JavaScript element and uses the
	 * comments around it to generate the metadata.
	 * @param {com.jwalk.ElementMeta} doc The object used to collect the documentable information.
	 * @param {com.jwalk.Element} element A JavaScript element (i.e. function, variable, etc).
	 */
	parseElement : function(doc, element){	
		if( element.previousComment ){
			this.parseComment(doc, element.previousComment);
		}
	},

	/**
	 * Parses the comment block for tags, links and formatting.
	 * This is called by {#parseElement}
	 * @param {com.jwalk.ElementMeta} object The element used to collect the documentable information.
	 * @param {String} comment The comment block to parse through.
	 */
	parseComment : function(doc, comment){
		var body = new String(comment.body); 
		var lines = body.split("\n");
		var end = comment.lineno + lines.length - 1;
		
		// Should this comment be associated with this element?
		if( end != doc.lineno - 1 || body.match(/^\s*\/\*\*/) == null){
			return;
		}
		
		// Extract comment text
		body = body.replace(/^\s*\/\*\*\s*/, ""); // start '/**'
		body = body.replace(/\s*\*\/\s*$/, ""); // end '*/'
		
		// Get all tags
		lines = body.split("\n");
		var curr = { name : null, body : "", attr : {} };
		var tags = []
		var line, tag;
		for ( var i = 0, len = lines.length; i < len; i++ ){
			line = lines[i];
			
			// Remove leading '*'
			line = line.replace(/^\s*\*\s*/, "");
			
			// Start of a tag? ( "@xxxxx Lorem ipsum" )
			if ( ( tag = line.match(/^@([^\s]+)(.*?)\s*$/i) ) != null ){
				
				// End current tag and add new one
				tags[tags.length] = curr;
				curr = { name : null, body : "", attr : {} };
				curr.name = tag[1].toLowerCase();
				curr.body = tag[2];
			}
			
			// Add to existing tag
			else{
				curr.body += " "+ this.strip(line);
			}
		}
		if( curr.name != null || curr.body.length > 0 ){
			tags[tags.length] = curr;
		}
		
		// Extract data from each tag
		var tag, body;
		for( var i = 0, len = tags.length; i < len; i++ ){
			tag = tags[i];
			body = tag.body;
			
			switch( tag.name ){
			case null:
				doc.description = body;
				break;
			case "param":
			case "return":
			case "returns":
			case "type":			
				
				// Parse type '@tag {TYPE} ...'
				var type = body.match(/^\s*{(.*?)}\s*(.*)/);
				if( type != null ){
					body = type[2];
					type = type[1];
				}
				
				// Add to documentation
				if( tag.name == "param" ){
						
					// Get name and description
					var param = { datatype: type }
					body = body.match(/^([^\s]+)\s*(.*)$/); // from: @param {TYPE} NAME DESCRIPTION....
					if( body == null ){
						continue;
					}
					param.name = body[1];
					param.desc = body[2];
					
					doc.setParam(param)
				}
				else {
					doc.returns = { datatype: type, 
									desc: body };
				}
				
				break;
			}
		}
		
	},
	
	/**
	 * Strips the leading/trailing space from a string
	 * @param {String} str The string to strip
	 * @returns {String} the new string
	 */
	strip : function(str){
		str = str.replace(/^\s*|\s$/, "");
		return str;
	},
};

var DocParser = DefaultParser;