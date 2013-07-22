projectPath = function () {
	var path = window.location.href;
	var id = path.indexOf("/projects/");
	return path.substring(0, id + 10);
}();

C8O_hub = {
	_subscribers: {},
	
	subscribe: function (name, handler) {
		C8O_hub._subscribers[name] = handler;
	},
	
	publish: function (event) {
		
		var map = null;
		if ((map = eventHub[event.origin]) && (map = map[event.name])) {
			for(var i=0; i < map.length; i++){
				var data = map[i].data;
				if (typeof(data) === "undefined") {
					data = event.data;
				} else if(typeof(data) === "function") {
					var key, clone = {};
					for(key in event.data) {
						clone[key] = event.data[key];
					}
					data = data.call(this, clone);
					if(typeof(data) === "undefined") {
						data = clone;
					}
				}
				var payload = {
					data: data,
					type: (typeof(map[i].type) === "undefined") ? "call" : map[i].type
				}
				
				if (typeof(C8O_hub._subscribers[map[i].target]) === "function") {
					C8O_hub._subscribers[map[i].target] (payload);
				}
			}
		}
	}
}