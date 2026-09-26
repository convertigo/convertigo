/**
 * Captures the pixels of an element of the page as the Studio shows it, with the screen capture of the
 * browser, which asks the user to share the tab.
 *
 * @param {Element} element
 * @returns {Promise<string>} the data URL of a JPEG picture
 */
export async function captureElement(element) {
	const stream = await navigator.mediaDevices.getDisplayMedia(
		/** @type {any} */ ({
			video: { displaySurface: 'browser' },
			audio: false,
			preferCurrentTab: true,
			selfBrowserSurface: 'include'
		})
	);
	try {
		const [track] = stream.getVideoTracks();
		// the Region Capture of Chrome crops the tab to the element
		const CropTarget = /** @type {any} */ (globalThis).CropTarget;
		let cropped = false;
		if (CropTarget && 'cropTo' in track) {
			try {
				await /** @type {any} */ (track).cropTo(await CropTarget.fromElement(element));
				cropped = true;
			} catch {
				// the whole tab, cropped below
			}
		}
		const video = document.createElement('video');
		video.srcObject = stream;
		video.muted = true;
		await video.play();
		// the frames after the sharing prompt closes
		await new Promise((resolve) => setTimeout(resolve, 400));
		const canvas = document.createElement('canvas');
		const context = canvas.getContext('2d');
		if (!context) {
			throw new Error('The browser cannot draw the capture.');
		}
		if (cropped) {
			canvas.width = video.videoWidth;
			canvas.height = video.videoHeight;
			context.drawImage(video, 0, 0);
		} else {
			const rect = element.getBoundingClientRect();
			const scale = video.videoWidth / window.innerWidth;
			canvas.width = Math.max(1, Math.round(rect.width * scale));
			canvas.height = Math.max(1, Math.round(rect.height * scale));
			context.drawImage(
				video,
				rect.x * scale,
				rect.y * scale,
				rect.width * scale,
				rect.height * scale,
				0,
				0,
				canvas.width,
				canvas.height
			);
		}
		return canvas.toDataURL('image/jpeg', 0.9);
	} finally {
		for (const track of stream.getTracks()) {
			track.stop();
		}
	}
}
