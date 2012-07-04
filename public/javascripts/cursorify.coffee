define ['jquery', 'less'], ($) -> $ ->

	cursorifyUrl = "ws://localhost:9000/cursorify"
	samplingInterval = 50
	sampleCount = 5
	
	clientId = undefined
	quitted = []
	updates = []
	frame = 0

	trackCursor = ->
		cursor = { x: 0, y: 0 }
		samples = 0
		trail = []
		
		$('html').append("<div id='overlay'></div>")
		
		$("#overlay").mousemove (e) ->
			cursor.x = e.pageX
			cursor.y = e.pageY
			
		setInterval (->
			trail.push { x: cursor.x,  y: cursor.y }
			samples = (samples + 1) % sampleCount
			if samples == 0
				socket.send JSON.stringify({ op: "update", trail: trail })
				samples = 0
				trail = []
		), samplingInterval
		
	onMessage = (e) ->
		message = JSON.parse e.data
		switch(message.op)
			when "joined"
				console.log "Joined, my CID is #{message.cid}"
				clientId = message.cid
			when "updates"
				updates = message.updates
				frame = 0
			when "quit"
				console.log "Client CID #{message.cid} quit"
				quitted[message.cid] = true
				$("##{message.cid}").remove()
		
	drawUpdates = ->
		setInterval (->
			for update in updates
				if update.trail.length > frame
					point = update.trail[frame]
					drawUpdate update.cid, update.handle, point.x, point.y
			frame++
		), samplingInterval
		
	drawUpdate = (cid, handle, x, y) ->
		if clientId != cid
			cursor = findOrCreateCursor cid, handle
			cursor.animate { top: y, left: x }, samplingInterval
		
	findOrCreateCursor = (cid, handle) ->
		cursor = $("##{cid}")
		if cursor.length == 0 and !quitted[cid]
			cursor = $("<div id='#{cid}' class='cursor'></div>")
			cursor.append $("<div class='handle'>#{handle}</div>")
			$('#overlay').append cursor
		cursor

	onOpen = ->
		socket.send JSON.stringify({ op: 'join', handle: 'Keke' })
		trackCursor()
		drawUpdates()
		
	socket = new WebSocket(cursorifyUrl);
	socket.onmessage = onMessage	
	socket.onopen = onOpen
	
