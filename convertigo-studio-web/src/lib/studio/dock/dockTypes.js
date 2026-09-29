/**
 * The views of the dock of the Studio, and the layout of a profile before it is changed.
 *
 * @typedef {Object} DockView
 * @property {string} id
 * @property {string} title
 * @property {string=} icon
 * @property {import('svelte').Snippet} content
 * @property {import('svelte').Snippet=} actions the buttons of the view, in the header of its group
 * @property {import('svelte').Snippet=} toolbar the buttons of the view too many for the header, over it
 * @property {boolean=} lazy shown once before it renders, as the views that load their content
 * @property {boolean=} main a view of the work area, on the background of the editors
 * @property {boolean=} scroll a view taller than its panel, which scrolls in it
 * @property {string=} alert what goes wrong in the view, as a build that fails, marked on its tab
 * @property {string=} detail what the view shows, as the project of an application, after its name
 *
 * @typedef {'left' | 'center' | 'right' | 'bottom'} DockArea
 * @typedef {Object} DockLayout the layout of a profile before it is changed
 * @property {Partial<Record<DockArea, string[]>>} areas the views of each area, the first one shown
 * @property {string[]=} active the views shown in their area instead of the first one
 * @property {string[]=} closed the views of an area not open at first, which come in it once shown
 * @property {Partial<Record<DockArea, number>>=} sizes the width of a side, the height of the bottom
 * @property {DockArea[]=} hidden the areas hidden at first, as the logs
 */

export {};
