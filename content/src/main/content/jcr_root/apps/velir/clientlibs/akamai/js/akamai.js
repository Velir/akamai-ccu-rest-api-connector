
(function($, ns, document) {
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
	const readFile = evt => {
		let file = evt.detail.item.file;
		if(file.type.indexOf("/json") === -1){
			return;
		}
		let reader = new FileReader();
		reader.addEventListener("load", () => {
			const json = JSON.parse(reader.result);
			const objects = json.objects;
			const type = json.type;
			const objMulti = document.getElementById('objMulti');
			objMulti.querySelectorAll(".coral3-Multifield-remove").forEach(rm =>rm.click());
			objects.forEach(obj => {
				objMulti.querySelector('.js-coral-Multifield-add').click();
				const last = objMulti.querySelector('coral-multifield-item:last-of-type');
				Coral.commons.ready(last, el => {
					const picker = el.querySelector('foundation-autocomplete');
					picker.value = obj;
				});
			});
			const select = document.getElementById('typeSelect');
			select.items.getAll().forEach((item) => {
				if(item.value === type){
					item.selected = true;
				}
			});
		});
		reader.readAsText(file);
	};
	$(document).ready(() => {
		document.getElementById('objects-upload').addEventListener('coral-fileupload:fileadded', readFile)
	});
}(window.jQuery, window.velirTouchUI = window.velirTouchUI || {}, document));
