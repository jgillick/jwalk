
/**
 * @type {String}
 */
var globVar;
const GLOB_CONST = 0;

/**
 * This is a plain function that doesn't do a whole lot
 * @param {Variant} attr1 This parameter doesn't do much
 * @param {Variant }attr2 This parameter does even less than the first.
 */
function plainFunc(attr1, attr2){
	var localVar = "local";
	const CONST_VAR = "constant";
}

/**
 * A standard object constructor
 */
function objFunc(){
	var one, two, three = "chain", four = "4";

	// Redefine global var
	globVar = "global";	// hello

	/**
	 * Nothing to see here, just another [implicit] global variable.
	 *
	 * Implicit global variables are EVIL.
	 */
	globalVar5 = "move me to global scope";

	// local var
	var local = "foo";

	var page = document.getElementById("page");

	// Properties
	this.propOne = false;
	this.propTwo = "hello";
	this.propThree = "world";
	this.propFour;
	this.propFive.subOne = "test";

	otherObj.someProperty = "w00t";


	/**
	 *  Method referencing global function
	 *  (should appear as a method named hello)
	 */
	this.hello = helloWorld;

	/**
	 * This is an empty method...I sure hope it does something someday.
	 * @param {boolean} attrX A cool parameter, because it ends in 'X'
	 */
	this.methodFuncOne = function(attrX){

	}

	this.methodFuncTwo = function(){

	}

	/**
	 * Set the current state and class name to 'clicked'.
	 */
	this.clickHandler = function(){
		this.className = "clicked"; // 'this' will be the elment that was clicked
	}

	/**
	 * Private inner function
	 */
	function innerFunc(){

	}

}
objFunc.prototype = {
	mFoo : "Foo Bar",

	protoVar : "",
	protoFunc : function(){

	},

	/**
	 * This property has a personality disorder, and sometimes multiple personalities
	 */
	get fooProp(){
		return this.mFoo;
	},
	set fooProp(f){
		this.mFoo = f;
		return this.mFoo;
	}
}

var objClosure = {
	objVar : "",
	addClass : function(elem, clazz) {
		elem.className = clazz;
	}
}

var core = {};
core.team = {};
core.team.flow = {};

/**
 * This is the Core Team Flow feature object, of course.
 * What more do you want to know?
 */
core.team.flow.feature = function(){};
core.team.flow.feature.prototype = {
	state : null,

	/**
	 * Just like cowbells, we need more modals
	 */
	openLightbox : function(){
		return true;
	}
};
core.team.flow.feature.prototype.getValue = function(){

}

/**
 * A function that returns a clever string.
 * @param {String} paramStr
 * @param {boolean} paramBool
 * @return {String}
 */
function helloWorld(paramStr, paramBool){
	if(false){
		return 10;
	}

	// Loops
	var num = 25;
	for(var i = 0; i < num; i++){
		alert(i);
	}

	var loop = true;
	while(loop){
		loop = false;
	}

	do {
		loop = false;
	} while(loop);

	return "Hello World";
}

/**
 *  Echos something strange, cryptic and completely useless
 */
function echo(str, number, multiplier){

	var out = str + "foo bar ("+ (number * multiplier) +")";

	return out;
}

/**
 * An object following Crockfords module pattern.
 */
var staticObj = function() {

	var privateMember = "foo";

	return {
		fooMe : "hello world",
		fooYou : "bar baz",
		fooWorld : function(){ }
	}
}();

var functionVar = function(){

	function callMe(){
	}

}

function(){

	var cloakedVar = "you can't see me";

	function evilPlan(){
		return "Under the radar";
	}
}

window.onload = function() { };
