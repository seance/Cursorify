cursorifyUrl = (path) ->
	"http://localhost:9000/assets/" + path

injectLoader = (tag, options, onload) ->
	elem = document.createElement tag
	for opt of options
		elem[opt] = options[opt]
	if onload
		elem.onload = onload
	document.head.appendChild elem
	
if window.Cursorify
	window.Cursorify.init()
	
else	
	window.Cursorify = {}

	injectLoader "link", {
		rel: "stylesheet",
		href: cursorifyUrl("stylesheets/jquery-ui-1.8.21.css")
	}, ->
	
		injectLoader "link", {
			rel: "stylesheet",
			href: cursorifyUrl("stylesheets/cursorify.css")
		}
	
	injectLoader "script", {
		type: "text/javascript",
		src: cursorifyUrl("javascripts/jquery-1.7.2.js")
	}, ->
	
		injectLoader "script", {
				type: "text/javascript",
				src: cursorifyUrl("javascripts/jquery-ui-1.8.21.min.js")
			}, ->
			
				injectLoader "script", {
					type: "text/javascript",
					src: cursorifyUrl("javascripts/cursorify.js")
				}, ->
					window.Cursorify.init()
