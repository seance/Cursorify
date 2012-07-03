define ['jquery', 'less'], ($) -> $ ->

	trackCursor = ->
		cursor = { x: 0, y: 0 }
		trail = []
		
		$('body').append('<div id="overlay"></div>')
		
		$('#overlay').mousemove (e) ->
			cursor.x = e.pageX
			cursor.y = e.pageY
			
		setInterval (->
			trail.push { x: cursor.x,  y: cursor.y }
		), 250
		
	trackCursor()