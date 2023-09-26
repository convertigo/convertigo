<script>
    import {
        Accordion,
        AccordionItem
    } from '@skeletonlabs/skeleton';

 	import { onMount } from 'svelte';
	import { getServiceUrl } from '$lib/convertigo';
    import { categories } from '$lib/paletteStore';

    // @ts-ignore
    import IconLinkOn from '~icons/mdi/arrow-left-right-bold-outline';
    // @ts-ignore
    import IconLinkOff from '~icons/mdi/arrow-left-right-bold';
    // @ts-ignore
    import IconArrangeOn from '~icons/mdi/arrange-bring-forward';
    // @ts-ignore
    import IconArrangeOff from '~icons/mdi/arrange-send-backward';
    // @ts-ignore
    import IconDownOn from '~icons/mdi/chevron-down-box';
    // @ts-ignore
    import IconDownOff from '~icons/mdi/chevron-down-box-outline';

    let search = '';
    
    let linkOn = false;
    let builtinOn = true;
    let visibilityOn = true;

    onMount(() => {
        
    });
    
</script>
<div class="palette">
    <div class="header">
        <div>
            <button type="button" class="btn" on:click={(e) => linkOn = !linkOn}>
                {#if linkOn }
                    <IconLinkOn/>
                {:else}
                    <IconLinkOff/>
                {/if}
            </button>
            <button type="button" class="btn" on:click={(e) => builtinOn = !builtinOn}>
                {#if builtinOn }
                    <IconArrangeOn/>
                {:else}
                    <IconArrangeOff/>
                {/if}
            </button>
            <button type="button" class="btn" on:click={(e) => visibilityOn = !visibilityOn}>
                {#if visibilityOn }
                    <IconDownOn/>
                {:else}
                    <IconDownOff/>
                {/if}
            </button>
        </div>
        <div>
            <input class="input" type="text" placeholder="Search..."/>
        </div>
    </div>
  
    <div class="content">
        <Accordion>
            {#each $categories as category}
                <AccordionItem>
                    <svelte:fragment slot="summary">{category.name}</svelte:fragment>
                    <svelte:fragment slot="content">
                        <div class="flex-container">
                            {#each category.items as item}
                                <div class="flex-child">
                                    {#if item.icon.includes('/')}
                                        <img
                                            src={getServiceUrl() +
                                            'database_objects.GetIconFromPath?iconPath='+ item.icon +
                                            '&__xsrfToken=' +
                                            encodeURIComponent(localStorage.getItem('x-xsrf-token') ?? '')}
                                            alt="ico"
                                        />
                                    {/if}
                                    <span>
                                        {item.name}
                                    </span>
                                </div>
                            {/each}
                        </div>
                    </svelte:fragment>            
                </AccordionItem>
            {/each}
        </Accordion>
    </div>
</div>

<style>
    .palette {
        display: flex;
        flex-direction: column;
    }

    .header {
        display: flex;
        flex-flow: row wrap;
        margin: 10px;
    }

    .content {
        /*margin: 2px;*/
    }  

    .flex-container {
        display: flex;
        flex-flow: row wrap;
     }

    .flex-container > .flex-child {
        border-radius: 5px;
        border: 1px solid #f1f1f1;
        width: 100px;
        margin: 5px;
        text-align: center;
        vertical-align: text-top;
        font-size: 10px;
    }

</style>