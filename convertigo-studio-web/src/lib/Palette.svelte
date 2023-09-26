<script>
    import {
        Accordion,
        AccordionItem
    } from '@skeletonlabs/skeleton';

 	import { onMount, onDestroy } from 'svelte';
	import { getServiceUrl } from '$lib/convertigo';
    import { categories } from '$lib/paletteStore';

    // @ts-ignore
    import IconLinkOn from '~icons/mdi/arrow-left-right-bold';
    // @ts-ignore
    import IconLinkOff from '~icons/mdi/arrow-left-right-bold-outline';
    // @ts-ignore
    import IconArrangeOn from '~icons/mdi/arrange-bring-forward';
    // @ts-ignore
    import IconArrangeOff from '~icons/mdi/arrange-send-backward';
    // @ts-ignore
    import IconDownOn from '~icons/mdi/chevron-down-box';
    // @ts-ignore
    import IconDownOff from '~icons/mdi/chevron-down-box-outline';

    let storeCategories = [];
    let localCategories = [];

    let search = '';
    
    let linkOn = true;
    let builtinOn = true;
    let additionalOn = true;

    onMount(() => {
        
    });
    
    const unsubscribe = categories.subscribe(value => {
        storeCategories = value;
        update();
	});

    onDestroy(unsubscribe);

    function update() {
        // link to selection in tree: get categories from store
        if (linkOn) {
            localCategories = storeCategories;
        }

        // filter based on button state
        let filtered = localCategories.map( ({type, name, items})  => {
            return ({type, name, "items": items.filter((item) => {
                let found = search !== "" ? item.name.toLowerCase().indexOf(search.toLowerCase()) != -1 : true;
                return found && ((builtinOn && item.builtin === builtinOn) || (additionalOn && item.additional === additionalOn))
            })})
        });
        localCategories = filtered
    }

    function handleLink(e) {
        linkOn = !linkOn;
        update();
    }

    function handleBuiltin(e) {
        builtinOn = !builtinOn
        update();
    }

    function handleAdditional(e) {
        additionalOn = !additionalOn
        update();
    }

    function doSearch() {
        update();
    }

</script>
<div class="palette">
    <div class="header">
        <div>
            <button type="button" class="btn" on:click={handleLink}>
                {#if linkOn }
                    <IconLinkOn/>
                {:else}
                    <IconLinkOff/>
                {/if}
            </button>
            <button type="button" class="btn" on:click={handleBuiltin}>
                {#if builtinOn }
                    <IconArrangeOn/>
                {:else}
                    <IconArrangeOff/>
                {/if}
            </button>
            <button type="button" class="btn" on:click={handleAdditional}>
                {#if additionalOn }
                    <IconDownOn/>
                {:else}
                    <IconDownOff/>
                {/if}
            </button>
        </div>
        <div>
            <input class="input" type="search" placeholder="Search..." bind:value={search} on:input={doSearch}/>
        </div>
    </div>
  
    <div class="content">
        <Accordion>
            {#each localCategories as category}
                {#if category.items.length > 0}
                    <AccordionItem open>
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
                {/if}
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
        margin-top: 5px;
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