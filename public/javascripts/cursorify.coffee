define ['jquery', 'less'], ($) -> $ ->

	socket = new WebSocket('ws://localhost:9000/update');
	socket.onopen = (e) -> console.log(e)
	socket.onclose = (e) -> console.log(e)
	socket.onmessage = (e) -> console.log(e)
	socket.onerror = (e) -> console.log(e)

	trackCursor = ->
		cursor = { x: 0, y: 0 }
		samples = 0
		trail = []
		
		$('html').append('<div id="overlay"></div>')
		
		$('#overlay').mousemove (e) ->
			cursor.x = e.pageX
			cursor.y = e.pageY
			
		setInterval (->
			trail.push { x: cursor.x,  y: cursor.y }
			if (samples += 1) % 8 == 0
				socket.send JSON.stringify({ op: 'update', body: trail })
				samples = 0
				trail = []
		), 250
		
	setTimeout (->
		socket.send JSON.stringify({ op: 'join' })
	), 500
	
	trackCursor()