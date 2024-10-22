<script>
	/** @type {{appUrl: any}} */
	let { appUrl } = $props();
</script>

<div class="device-iphone-8 device-gold">
	<div class="device-frame">
		<iframe src={appUrl} class="device-content"></iframe>
	</div>
</div>

<style lang="">
	.device-frame {
		position: relative;
		width: 100%;
		height: 100%;
		overflow: hidden;
		border-radius: 30px;
		box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
	}
	.device-content {
		width: 100%;
		height: 100%;
		border: none;
	}
</style>
