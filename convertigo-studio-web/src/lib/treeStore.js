import { writable } from 'svelte/store';

export const treeData = writable({
    id: null,
    label: '',
    children: true,
    icon: 'folder',
    expanded: true
});
