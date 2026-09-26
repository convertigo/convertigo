<script>
	import { formatLifetime, LIFETIME_MODES, parseLifetime, WEEK_DAYS } from './responseLifetime';

	/**
	 * Builds the response lifetime of a requestable, as the generator of the cache editor of the Eclipse
	 * Studio: a number of seconds, or a time of each day, of a day of each week or of each month.
	 *
	 * @type {{
	 *  label?: string,
	 *  value?: string,
	 *  onApply?: (value: string) => void,
	 *  onClose?: () => void
	 * }}
	 */
	let { label = 'Response lifetime', value = '', onApply, onClose } = $props();

	/** @type {typeof LIFETIME_MODES[number]} */
	let mode = $state('absolute');
	let seconds = $state(3600);
	let time = $state('00:00:00');
	let day = $state(1);

	$effect.pre(() => {
		const lifetime = parseLifetime(value);
		if (lifetime) {
			mode = lifetime.mode;
			seconds = lifetime.seconds;
			time = lifetime.time;
			day = lifetime.day;
		}
	});

	let result = $derived(formatLifetime({ mode, seconds, time, day }));
</script>

<div class="studio-dialog" role="presentation" onclick={() => onClose?.()}>
	<div
		class="studio-dialog__box"
		role="dialog"
		aria-modal="true"
		aria-labelledby="studio-lifetime-title"
		tabindex="-1"
		onclick={(event) => event.stopPropagation()}
		onkeydown={(event) => {
			event.stopPropagation();
			if (event.key === 'Escape') onClose?.();
		}}
	>
		<header class="studio-dialog__header">
			<strong id="studio-lifetime-title">{label}</strong>
		</header>
		<form
			class="studio-dialog__form"
			onsubmit={(event) => {
				event.preventDefault();
				onApply?.(result);
			}}
		>
			<div class="studio-dialog__body">
				<label class="studio-dialog__field">
					<span>Expires</span>
					<select class="input-common" bind:value={mode}>
						<option value="absolute">after a number of seconds</option>
						<option value="daily">each day at a time</option>
						<option value="weekly">each week on a day at a time</option>
						<option value="monthly">each month on a day at a time</option>
					</select>
				</label>
				{#if mode === 'absolute'}
					<label class="studio-dialog__field">
						<span>Seconds</span>
						<input class="input-common" type="number" min="0" bind:value={seconds} />
					</label>
				{:else}
					<div class="studio-dialog__row">
						{#if mode === 'weekly'}
							<label class="studio-dialog__field">
								<span>Day</span>
								<select class="input-common" bind:value={day}>
									{#each WEEK_DAYS as name, index (name)}
										<option value={index + 1}>{name}</option>
									{/each}
								</select>
							</label>
						{:else if mode === 'monthly'}
							<label class="studio-dialog__field">
								<span>Day</span>
								<input class="input-common" type="number" min="1" max="31" bind:value={day} />
							</label>
						{/if}
						<label class="studio-dialog__field">
							<span>Time</span>
							<input class="input-common" type="time" step="1" bind:value={time} />
						</label>
					</div>
				{/if}
				<p class="studio-dialog__result" role="status">{result}</p>
			</div>
			<footer class="studio-dialog__footer">
				<button type="button" class="button-secondary" onclick={() => onApply?.('')}
					>No lifetime</button
				>
				<span class="studio-dialog__spacer"></span>
				<button type="button" class="button-secondary" onclick={() => onClose?.()}>Cancel</button>
				<button type="submit" class="button-primary">Apply</button>
			</footer>
		</form>
	</div>
</div>

<style>
	.studio-dialog {
		position: fixed;
		inset: 0;
		z-index: 90;
		display: grid;
		place-items: center;
		background: color-mix(in oklab, black 45%, transparent);
		padding: 1rem;
	}

	.studio-dialog__box {
		display: grid;
		width: min(28rem, 100%);
		overflow: hidden;
		border: 1px solid var(--studio-line);
		border-radius: 0.5rem;
		background: var(--studio-chrome-bg);
		box-shadow: 0 1.5rem 4rem color-mix(in oklab, black 35%, transparent);
		color: var(--studio-text);
	}

	.studio-dialog__form {
		display: contents;
	}

	.studio-dialog__header,
	.studio-dialog__footer {
		display: flex;
		align-items: center;
		justify-content: flex-end;
		gap: 0.6rem;
		padding: 0.75rem 1rem;
	}

	.studio-dialog__header {
		justify-content: flex-start;
		border-bottom: 1px solid var(--studio-line);
	}

	.studio-dialog__header strong {
		color: var(--studio-text-strong);
		font-size: 0.9rem;
		font-weight: 600;
	}

	.studio-dialog__footer {
		border-top: 1px solid var(--studio-line);
	}

	.studio-dialog__body {
		display: grid;
		gap: 0.7rem;
		padding: 1rem;
		font-size: 0.8rem;
	}

	.studio-dialog__row {
		display: grid;
		grid-template-columns: repeat(auto-fit, minmax(8rem, 1fr));
		gap: 0.6rem;
	}

	.studio-dialog__field {
		display: grid;
		gap: 0.3rem;
	}

	.studio-dialog__field > span {
		font-weight: 600;
	}

	.studio-dialog__field select,
	.studio-dialog__field input {
		height: 2rem;
		padding-block: 0;
		padding-inline: 0.6rem;
		font-size: 0.8rem;
	}

	.studio-dialog__result {
		margin: 0;
		color: var(--studio-text-idle);
		font-family: var(--font-mono, ui-monospace, monospace);
	}

	.studio-dialog__spacer {
		flex: 1;
	}
</style>
