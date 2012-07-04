define ['jquery', 'less'], ($) -> $ ->

	cursorifyUrl = "ws://localhost:9000/cursorify"
	samplingInterval = 20
	sampleCount = 10
	
	clientId = undefined
	quitted = []

	asyncLoop = (times, delay, f) ->
		counter = 0
		(doLoop = ->
			f counter
			counter = counter + 1
			if counter < times or times == 0
				setTimeout doLoop, delay)()

	trackCursor = ->
		cursor = { x: 0, y: 0 }
		samples = 0
		trail = []
		
		$('html').append("<div id='overlay'></div>")
		
		$("#overlay").mousemove (e) ->
			cursor.x = e.pageX
			cursor.y = e.pageY
			
		asyncLoop 0, samplingInterval, (ignored) ->
			trail.push { x: cursor.x,  y: cursor.y }
			samples = (samples + 1) % sampleCount
			if samples == 0
				socket.send JSON.stringify({ op: "update", trail: trail })
				samples = 0
				trail = []

	onMessage = (e) ->
		message = JSON.parse e.data
		switch(message.op)
			when "joined"
				console.log "Joined, my CID is #{message.cid}"
				clientId = message.cid
			when "updates"
				animateUpdates message.updates
			when "quit"
				console.log "Client CID #{message.cid} quit"
				quitted[message.cid] = true
				$("##{message.cid}").remove()

	animateUpdates = (updates) ->
		for update in updates
			animateUpdate update
			
	animateUpdate = (update) ->
		frames = update.trail.length
		interval = samplingInterval * (sampleCount / frames)
		asyncLoop frames, interval, (frame) ->
			drawFrame update, update.trail[frame], interval, frame

	drawFrame = (update, point, interval, frame) ->
		cursor = findOrCreateCursor update.cid, update.handle
		cursor.animate { top: point.y, left: point.x }, interval

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
		
	socket = new WebSocket(cursorifyUrl);
	socket.onmessage = onMessage	
	socket.onopen = onOpen
	
