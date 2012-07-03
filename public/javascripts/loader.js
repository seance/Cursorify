(function() {
	if (!window.Cursorify) {
		window.Cursorify = {};
		
		var link = document.createElement('link');
		link.rel = 'stylesheet/less';
		link.href = 'http://localhost:9000/assets/stylesheets/cursorify.less';
		
		var script = document.createElement('script');
		script.type = 'text/javascript';
		script.src = 'http://localhost:9000/assets/javascripts/lib/require-2.0.2.js';
		script.onload = function() {
			require({
				baseUrl: 'http://localhost:9000/assets/javascripts',
				paths: {
					cs:		'lib/cs-0.4.2',
					less:	'lib/less-1.3.0.min',
					jquery: 'lib/jquery-1.7.2'
				}
			}, ['cs!cursorify']);
		};
		
		document.head.appendChild(link);
		document.head.appendChild(script);
	}
})();