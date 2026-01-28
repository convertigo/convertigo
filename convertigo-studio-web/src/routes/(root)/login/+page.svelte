<script>
	import { Progress } from '@skeletonlabs/skeleton-svelte';
	import { goto } from '$app/navigation';
	import Card from '$lib/admin/components/Card.svelte';
	import Authentication from '$lib/common/Authentication.svelte';
	import InputGroup from '$lib/common/components/InputGroup.svelte';
	import Ico from '$lib/utils/Ico.svelte';

	/** @type {{data: import('./$types').PageData}} */
	let { data } = $props();

	/** @type {string|null} */
	let error = $state(null);

	let doAuthenticate = $state(false);
	let passwordVisible = $state(false);

	async function handleSubmit(e) {
		try {
			doAuthenticate = true;
			await Authentication.authenticate(e);
			if (Authentication.authenticated) {
				goto(`${data.redirect ?? '../admin'}`);
			} else {
				error = Authentication.error;
			}
		} catch (error) {
			error = '' + error;
		} finally {
			doAuthenticate = false;
		}
	}
</script>

{#if doAuthenticate}
	<div class="preset-glass-primary relative grid place-items-center rounded-full p-low">
		<Progress value={null} class="relative grid place-items-center">
			<Progress.Circle style="--size: 18rem; --thickness: 10px;">
				<Progress.CircleTrack class="stroke-surface-200-800/50" />
				<Progress.CircleRange class="stroke-primary-500" />
			</Progress.Circle>
			<span class="absolute animate-pulse text-sm font-medium">Authenticating …</span>
		</Progress>
	</div>
{:else}
	<Card class="preset-filled-surface-100-900 max-md:w-[95%]" bg="">
		<form onsubmit={handleSubmit} class="layout-y-m-center">
			<Ico icon="convertigo:logo" class="-m-5 text-primary-500" size="32" />
			<h1 class="text-center text-xl md:text-3xl">
				<p>Connect to <span class="font-normal text-primary-500">Convertigo</span></p>
				<p>Administration Console</p>
			</h1>
			<input type="hidden" name="authType" value="login" />
			<div class="layout-y-start-low w-full">
				<label
					class="label-common text-left text-sm font-medium text-surface-600-400"
					for="login-username">Username</label
				>
				<InputGroup
					id="login-username"
					name="authUserName"
					autocomplete="username"
					placeholder="Your username"
					icon="mdi:account-outline"
					iconClass="text-muted"
					class="rounded-xl bg-surface-200-800"
					labelClass="rounded-l-xl bg-transparent"
					inputClass="text-base"
				/>
			</div>

			<div class="layout-y-start-low w-full">
				<label
					class="label-common text-left text-sm font-medium text-surface-600-400"
					for="login-password">Password</label
				>
				<InputGroup
					id="login-password"
					name="authPassword"
					autocomplete="current-password"
					placeholder="Your password"
					type={passwordVisible ? 'text' : 'password'}
					icon="mdi:lock-outline"
					iconClass="text-muted"
					class="rounded-xl bg-surface-200-800"
					labelClass="rounded-l-xl bg-transparent"
					inputClass="text-base"
				>
					{#snippet actions()}
						<button
							type="button"
							class="btn h-full rounded-r-xl bg-transparent px-3 text-primary-500 hover:text-primary-400"
							onclick={() => (passwordVisible = !passwordVisible)}
							title={passwordVisible ? 'Hide password' : 'Show password'}
						>
							<Ico icon={passwordVisible ? 'mdi:eye-off-outline' : 'mdi:eye-outline'} size="nav" />
						</button>
					{/snippet}
				</InputGroup>
			</div>

			{#if error}
				<p class="rounded-sm preset-filled-error-500 p-low">{error}</p>
			{/if}
			<span></span>
			<button class="relative button-primary w-full py-5" type="submit">
				<span class="absolute left-1/2 inline-flex -translate-x-1/2 items-center gap-2">
					<span>Login</span>
					<span class="bounce"><Ico icon="mdi:arrow-right-thick" /></span>
				</span>
			</button>
		</form>
	</Card>
	<span class="text-xs">© {new Date().getFullYear()} Convertigo. All rights reserved.</span>
{/if}

<style lang="postcss">
	.bounce {
		animation: bounce-x 1.6s infinite alternate ease-in-out;
	}

	@keyframes bounce-x {
		from {
			transform: translateX(-3px);
		}
		to {
			transform: translateX(3px);
		}
	}
</style>
