/**
 * @typedef {{ x: number, y: number }} Point
 */

/**
 * @typedef {Object} FlowNode
 * @property {string} id
 * @property {string} type
 * @property {string} label
 * @property {string=} name
 * @property {number} x
 * @property {number} y
 * @property {string=} color
 * @property {string=} group
 * @property {number=} inputs
 * @property {number=} outputs
 * @property {string[]=} inputLabels
 * @property {string[]=} outputLabels
 * @property {number=} bottomOutputs
 * @property {string[]=} bottomOutputLabels
 * @property {number=} bottomInputs
 * @property {string[]=} bottomInputLabels
 * @property {Record<string, unknown>=} data
 */

/**
 * @typedef {Object} FlowLink
 * @property {string} id
 * @property {{ nodeId: string, portIndex: number }} from
 * @property {{ nodeId: string, portIndex: number }} to
 * @property {'bezier' | 'orthogonal' | 'loop-return'=} routing
 */

/**
 * @typedef {Object} Flow
 * @property {string} id
 * @property {string} name
 * @property {FlowNode[]} nodes
 * @property {FlowLink[]} links
 */

/**
 * @typedef {Object} PaletteItem
 * @property {string} type
 * @property {string} label
 * @property {string} color
 * @property {number} inputs
 * @property {number} outputs
 * @property {string=} classname
 * @property {string=} icon
 * @property {string=} group
 * @property {string[]=} outputLabels
 * @property {number=} bottomOutputs
 * @property {string[]=} bottomOutputLabels
 * @property {number=} bottomInputs
 * @property {string[]=} bottomInputLabels
 */

/**
 * @typedef {Object} PaletteGroup
 * @property {string} name
 * @property {PaletteItem[]} items
 */

/**
 * @typedef {Object} TreeviewItem
 * @property {string} id
 * @property {string=} label
 * @property {string=} name
 * @property {string=} icon
 * @property {string=} classname
 * @property {boolean | TreeviewItem[]=} children
 * @property {boolean=} isLoop
 * @property {boolean=} isXml
 * @property {boolean=} isSourceContainer
 */

/**
 * @typedef {Object} TreeviewResponse
 * @property {string=} id
 * @property {TreeviewItem[]=} children
 */

/**
 * @typedef {Object} SequenceTreeNode
 * @property {string} id
 * @property {string} label
 * @property {string=} name
 * @property {string=} icon
 * @property {string=} classname
 * @property {boolean} isLoop
 * @property {boolean} isXml
 * @property {boolean} isSourceContainer
 * @property {boolean} hasChildren
 * @property {SequenceTreeNode[]} children
 */

/**
 * @typedef {Object} FlowStepNodeData
 * @property {string} id
 * @property {string} label
 * @property {string} name
 * @property {string} type
 * @property {string=} color
 * @property {string=} icon
 * @property {string=} classname
 * @property {string=} group
 * @property {boolean=} isLoop
 * @property {boolean=} isReturn
 * @property {boolean=} isBreak
 * @property {boolean=} isVariable
 * @property {boolean=} isXml
 * @property {boolean=} isSourceContainer
 * @property {boolean=} hasChildren
 * @property {boolean=} hasThenBranch
 * @property {boolean=} hasElseBranch
 * @property {boolean=} isFlowTerminal
 * @property {'request' | 'response'=} terminalKind
 * @property {number} inputs
 * @property {number} outputs
 * @property {string[]} inputLabels
 * @property {string[]} outputLabels
 * @property {number} bottomInputs
 * @property {number} bottomOutputs
 * @property {number[]=} outputVisualOrder
 * @property {number=} loopReturnInputIndex
 * @property {boolean=} isSubstepCollapsed
 * @property {number=} substepDescendantCount
 * @property {(function(string, boolean): void)=} onToggleSubsteps
 * @property {string=} originalId
 * @property {boolean=} isSelected
 * @property {boolean=} isDropTarget
 * @property {boolean=} isDropDenied
 * @property {'inside' | 'before' | 'after'=} dropPosition
 * @property {string=} dropBranch
 * @property {string=} dropHostLabel
 * @property {boolean=} isRenaming
 * @property {string=} parentBranch
 * @property {(function(string, string): void)=} onRename
 * @property {(function(string): void)=} onRequestRename
 * @property {(function(string): void)=} onDelete
 */

export {};
