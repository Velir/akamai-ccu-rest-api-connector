
(function($, ns) {
	"use strict";
	const OK = [{text:"Ok"}];
	let registry = $(window).adaptTo("foundation-registry");
	registry.register("foundation.form.response.ui.success", {
		name: "velir.flush-akamai",
		handler: function(formEl, config, data, textStatus, xhr, resp) {
			let ui = $(window).adaptTo("foundation-ui");
			if(resp.httpStatus === 201){
				let msg = `<div><h3>Flush submitted</h3>
				<strong>Message:</strong> ${resp.detail}<br/>
				<strong>Estimated seconds until complete:</strong> ${resp.estimatedSeconds}<br/>
				<strong>Purge ID:</strong> ${resp.purgeId}<br/>
				<strong>Support ID:</strong> ${resp.supportId}
				</div>`;
				ui.prompt("Success", msg, 'success', OK);
			} else{
				ui.prompt("Error", `Error flushing paths: ${resp.detail}`, 'error', OK);
			}

		}
	});
}(window.jQuery, window.velirTouchUI = window.velirTouchUI || {}));