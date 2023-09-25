<script>

	import {
		Accordion,
		AccordionItem
	} from '@skeletonlabs/skeleton';
 
    import { categories } from './samplePalette';

 	import { onMount } from 'svelte';
	import { linear } from 'svelte/easing';
	import { callService, getServiceUrl } from '$lib/convertigo';

    // @ts-ignore
    import IconCloud from '~icons/mdi/cloud-outline';

    let search = '';

    onMount(() => {
        
    });
    

</script>
<div class="palette">
    <div class="header">
        <div>
            <button type="button" class="btn variant-filled"><IconCloud/></button>
            <button type="button" class="btn variant-filled"><IconCloud/></button>
            <button type="button" class="btn variant-filled"><IconCloud/></button>
        </div>
        <div>
            <input class="input" type="text" placeholder="Search..." />
        </div>
    </div>
  
    <div class="content">
        <Accordion>
            {#each categories as category}
                <AccordionItem>
                    <!--<svelte:fragment slot="lead">(icon)</svelte:fragment>-->
                    <svelte:fragment slot="summary">{category.name}</svelte:fragment>
                    <svelte:fragment slot="content">
                        <div class="flex-container">
                            {#each category.items as item}
                                <div class="flex-child">
                                    {#if item.icon.includes('?')}
                                    <img
                                        src={getServiceUrl() +
                                        item.icon +
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
        /*border: 2px solid red;*/
    }

    .header {
        display: flex;
        flex-flow: row wrap;
    }

    .content {
        /*margin: 2px;*/
        /*border: 2px solid yellow;*/
    }  

    .flex-container {
        display: flex;
        flex-flow: row wrap;
     }

    .flex-container > .flex-child {
        /*background-color: #f1f1f1;*/
        border-radius: 5px;
        /*box-shadow: 1px 1px 1px 1px #f1f1f1;*/
        border: 1px solid #f1f1f1;
        width: 100px;
        margin: 5px;
        text-align: center;
        vertical-align: text-top;
        font-size: 10px;
    }
 
</style>