importScripts("https://cdnjs.cloudflare.com/ajax/libs/qwest/4.5.0/qwest.min.js");
importScripts("/js/config.js");

onmessage = function (event) {
	for (i = 0; i < 1000; i++) {
		makeAjaxRequest(event.data);
	}
};

function makeAjaxRequest(ctoken) {
	qwest.get(config.urlUuid, {}, {
		headers: {
			"C-Token": ctoken
		}
	})
		.then(function (xhr, response) {
			postMessage({
				"uuid": JSON.parse(xhr.responseText).uuids[0],
				"appServerColor": xhr.getResponseHeader("AppServer"),
				"dbServerColor": xhr.getResponseHeader("DbServer")
			});
		})
		.catch(function (error, xhr, response) {
			postMessage({ "error": xhr.status });
		});
}
