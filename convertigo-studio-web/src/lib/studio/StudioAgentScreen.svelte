<script>
	/**
	 * The browser of the agents of the Assistant in the desktop Studio, as a view beside the preview of the
	 * application: its page live, which takes the clicks, the wheel and the keys of the user in the view, as the
	 * application editor of the Eclipse Studio shows the page an agent checks. The browser itself stays a process
	 * apart, which the agents alone drive through their port, its window shown on demand.
	 */
	import Ico from '#lib/utils/Ico.svelte';
	import StudioEmptyState from './StudioEmptyState.svelte';

	/** @type {{ active?: boolean, onAgentPage?: (url: string) => void }} */
	let { active = false, onAgentPage } = $props();

	/**
	 * @typedef {Object} AgentScreen the bridge of the desktop Studio to the browser of the agents
	 * @property {() => void} start
	 * @property {() => void} watch
	 * @property {() => void} stop
	 * @property {() => void} show
	 * @property {(input: Record<string, unknown>) => void} input
	 * @property {(callback: (frame: { data: string, width: number, height: number }) => void) => () => void} onFrame
	 * @property {(callback: (state: { state: string, url: string }) => void) => () => void} onState
	 */

	/** @type {AgentScreen | undefined} */
	const agentScreen =
		typeof window === 'undefined'
			? undefined
			: /** @type {any} */ (window).convertigoStudio?.agentScreen;

	let frame = $state('');
	/** the size of the page, in its pixels, which the input of the user goes in */
	let pageWidth = 0;
	let pageHeight = 0;
	let browserState = $state('waiting');
	let url = $state('');
	/** @type {HTMLImageElement | undefined} */
	let image = $state();
	/** @type {HTMLDivElement | undefined} */
	let view = $state();

	$effect(() => {
		if (!agentScreen) {
			return;
		}
		const shown = active;
		// the page the agent is on when the view starts to follow it opens nothing
		let first = true;
		const offFrame = agentScreen.onFrame((next) => {
			// a blank page paints too: the view tells instead that the agent opened none yet
			if (shown && browserState === 'on') {
				frame = `data:image/jpeg;base64,${next.data}`;
				pageWidth = next.width;
				pageHeight = next.height;
			}
		});
		const offState = agentScreen.onState((next) => {
			const previous = url;
			browserState = next.state;
			if (next.url || next.state !== 'on') {
				url = next.url;
			}
			if (next.state !== 'on') {
				frame = '';
			}
			if (!shown && !first && next.state === 'on' && next.url && next.url !== previous) {
				onAgentPage?.(next.url);
			}
			first = false;
		});
		// hidden, the view follows the address of the page only: the browser paints for the agent
		if (shown) {
			agentScreen.start();
		} else {
			agentScreen.watch();
		}
		return () => {
			agentScreen.stop();
			offFrame();
			offState();
		};
	});

	/** the address of the page, from the application on */
	let shownUrl = $derived(url.replace(/^https?:\/\/[^/]+/, ''));

	/** @param {MouseEvent | KeyboardEvent} event */
	function modifiersOf(event) {
		return (
			(event.altKey ? 1 : 0) |
			(event.ctrlKey ? 2 : 0) |
			(event.metaKey ? 4 : 0) |
			(event.shiftKey ? 8 : 0)
		);
	}

	/**
	 * @param {MouseEvent} event
	 * @returns {{ x: number, y: number } | null} the point of the page under the pointer
	 */
	function pagePoint(event) {
		const rect = image?.getBoundingClientRect();
		if (!rect?.width || !rect.height || !pageWidth || !pageHeight) {
			return null;
		}
		return {
			x: Math.round(((event.clientX - rect.left) * pageWidth) / rect.width),
			y: Math.round(((event.clientY - rect.top) * pageHeight) / rect.height)
		};
	}

	/**
	 * @param {'mousePressed' | 'mouseReleased' | 'mouseMoved'} action
	 * @param {MouseEvent} event
	 */
	function mouse(action, event) {
		const point = pagePoint(event);
		if (!point) {
			return;
		}
		const moved = action === 'mouseMoved';
		agentScreen?.input({
			type: 'mouse',
			action,
			...point,
			button: moved
				? event.buttons & 1
					? 'left'
					: 'none'
				: (['left', 'middle', 'right'][event.button] ?? 'left'),
			clickCount: moved ? 0 : Math.max(1, event.detail || 1),
			modifiers: modifiersOf(event)
		});
	}

	/** @type {PointerEvent | null} the last move, sent once a frame */
	let pendingMove = null;
	/** @param {PointerEvent} event */
	function move(event) {
		if (!pendingMove) {
			requestAnimationFrame(() => {
				if (pendingMove) {
					mouse('mouseMoved', pendingMove);
				}
				pendingMove = null;
			});
		}
		pendingMove = event;
	}

	/** @param {PointerEvent} event */
	function press(event) {
		event.preventDefault();
		view?.focus();
		mouse('mousePressed', event);
	}

	/** @param {WheelEvent} event */
	function wheel(event) {
		const point = pagePoint(event);
		if (!point) {
			return;
		}
		event.preventDefault();
		agentScreen?.input({
			type: 'wheel',
			...point,
			deltaX: event.deltaX,
			deltaY: event.deltaY,
			modifiers: modifiersOf(event)
		});
	}

	/**
	 * The keys typed in the view go to the page, the shortcuts with Command staying for the Studio.
	 * @param {'keyDown' | 'keyUp'} action
	 * @param {KeyboardEvent} event
	 */
	function key(action, event) {
		if (!frame || event.metaKey) {
			return;
		}
		event.preventDefault();
		event.stopPropagation();
		agentScreen?.input({
			type: 'key',
			action,
			key: event.key,
			code: event.code,
			modifiers: modifiersOf(event)
		});
	}
</script>

<div class="studio-agent-screen">
	<div class="studio-agent-screen__bar">
		<span class="studio-agent-screen__url" title={url}>
			<Ico icon="mdi:robot-outline" size={4} class="shrink-0" />
			<span>{browserState === 'off' ? 'Agent browser off' : shownUrl || 'No page yet'}</span>
		</span>
		<button
			type="button"
			class="studio-agent-screen__show"
			title="Open the browser of the agents in a window of its own"
			disabled={browserState === 'off'}
			onclick={() => agentScreen?.show()}
		>
			<Ico icon="mdi:open-in-new" size={4} />
			<span>Open in a window</span>
		</button>
	</div>
	<!-- the view takes the keys for the page it shows, as a remote screen does -->
	<!-- svelte-ignore a11y_no_noninteractive_element_interactions -->
	<div
		class="studio-agent-screen__view"
		bind:this={view}
		role="application"
		aria-label="The page of the browser of the agents"
		tabindex="-1"
		onkeydown={(event) => key('keyDown', event)}
		onkeyup={(event) => key('keyUp', event)}
	>
		{#if frame}
			<img
				bind:this={image}
				src={frame}
				alt="The page the agent checks"
				draggable="false"
				onpointerdown={press}
				onpointerup={(event) => mouse('mouseReleased', event)}
				onpointermove={move}
				onwheel={wheel}
				oncontextmenu={(event) => event.preventDefault()}
			/>
		{:else}
			<StudioEmptyState
				icon="mdi:robot-outline"
				message={browserState === 'off'
					? 'The browser of the agents does not run: File > Agent Browser turns it on.'
					: 'The agent has not opened a page in its browser yet.'}
			/>
		{/if}
	</div>
</div>

<style>
	.studio-agent-screen {
		display: grid;
		height: 100%;
		min-height: 0;
		grid-template: auto minmax(0, 1fr) / minmax(0, 1fr);
		background: var(--color-surface-100-900);
	}

	.studio-agent-screen__bar {
		display: flex;
		align-items: center;
		gap: 0.5rem;
		border-bottom: 1px solid var(--studio-line);
		padding: 0.35rem 0.5rem;
	}

	.studio-agent-screen__url {
		display: flex;
		min-width: 0;
		flex: 1;
		align-items: center;
		gap: 0.4rem;
		color: var(--studio-text-idle);
		font-family: ui-monospace, monospace;
		font-size: 0.75rem;
	}

	.studio-agent-screen__url > span {
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.studio-agent-screen__show {
		display: inline-flex;
		flex: none;
		align-items: center;
		gap: 0.35rem;
		border: 1px solid var(--studio-line);
		border-radius: 0.4rem;
		background: transparent;
		color: var(--studio-text);
		padding: 0.25rem 0.6rem;
		font-size: 0.75rem;
	}

	.studio-agent-screen__show:hover:not(:disabled) {
		background: var(--studio-hover-bg);
	}

	.studio-agent-screen__show:disabled {
		color: var(--studio-text-idle);
		opacity: 0.6;
	}

	.studio-agent-screen__view {
		display: grid;
		min-height: 0;
		/* a single cell the size of the view, which the page fits in whatever the size of its frames */
		grid-template: minmax(0, 1fr) / minmax(0, 1fr);
		place-items: center;
		overflow: hidden;
		padding: 0.5rem;
	}

	.studio-agent-screen__view:focus {
		outline: none;
	}

	.studio-agent-screen__view:focus img {
		border-color: var(--color-primary-500);
	}

	.studio-agent-screen__view img {
		cursor: default;
		user-select: none;
		max-width: 100%;
		max-height: 100%;
		border: 1px solid var(--studio-line);
		border-radius: 0.4rem;
		box-shadow: 0 4px 16px light-dark(rgb(0 0 0 / 0.12), rgb(0 0 0 / 0.45));
		object-fit: contain;
	}
</style>
