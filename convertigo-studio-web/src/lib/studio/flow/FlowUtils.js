import ELK from "elkjs/lib/elk.bundled.js";
import { Position } from "@xyflow/svelte";

function handleOutsides() {
    return true;
}

export function isOutsideContainer(classname) {
    return classname && classname.endsWith("ThenElseStep") || classname === "ParallelStep";
}

export function createNodesAndEdges(parentDbo = {}, parentNode = undefined) {
    const handledOutsides = handleOutsides();
    const nodes = [];
    const edges = [];

    if (parentDbo.children.length > 0) {
        let containerId;
        if (parentNode == undefined) {
            containerId = undefined
        } else {
            if (isOutsideContainer(parentNode.data.classname)) {
                containerId = parentNode.parentId;
            } else {
                containerId = parentNode.id;
            }
        }

        let lastChildDbo = undefined;
        parentDbo.children.forEach((childDbo) => {
            let currentNode = parentNode;
            if (childDbo.icon !== "folder" && childDbo.classname !== "RequestableVariable") {
                const hasChildren = childDbo.children === true || childDbo?.children?.length > 0;
                const node = {
                    id: `${childDbo.id}`,
                    data: {
                        label: `${childDbo.label}`,
                        classname: `${childDbo.classname}`,
                        icon: `${childDbo.icon}`,
                        parentDboId: `${parentDbo.id}`,
                        isXml: childDbo.isXml,
                        isLoop: childDbo.isLoop,
                        isSourceContainer: childDbo.isSourceContainer,
                        hasChildren: hasChildren
                    },
                    type: "step-node",
                    position: { x: 0, y: 0 },
                    style: "width: 150px; height: 40px",
                };

                node.parentId = containerId;
                nodes.push(node);
                currentNode = node;

                if (handledOutsides) {
                    if (isOutsideContainer(parentDbo.classname)) {
                        edges.push(createEdge(parentDbo.id, childDbo.id));
                    } else {
                        if (lastChildDbo !== undefined) {
                            if (isOutsideContainer(lastChildDbo.classname)) {
                                edges.push(createEdge(lastChildDbo.id, childDbo.id, lastChildDbo?.children?.length > 0));
                                if (lastChildDbo?.children?.length > 0) {
                                    nodes
                                        .filter((n) => n.data.parentDboId == lastChildDbo.id)
                                        .forEach((n) => {
                                            edges.push(createEdge(n.id, childDbo.id));
                                        });
                                }
                            } else {
                                edges.push(createEdge(lastChildDbo.id, childDbo.id));
                            }
                        }
                    }
                } else {
                    if (lastChildDbo !== undefined) {
                        if (!isOutsideContainer(parentDbo.classname)) {
                            edges.push(createEdge(lastChildDbo.id, childDbo.id));
                        }
                    }
                }
            }

            if (childDbo.children.length > 0) {
                const ne = createNodesAndEdges(childDbo, currentNode);
                nodes.push(...ne.nodes);
                edges.push(...ne.edges);
            }
            lastChildDbo = childDbo;
        });
    }

    return { nodes, edges };
}

export function createEdge(sourceId, targetId, hidden = false) {
    const edge = {
        id: `${sourceId}-${targetId}`,
        type: "smoothstep",
        source: `${sourceId}`,
        sourceHandle: 'out',
        target: `${targetId}`,
        targetHandle: 'in',
        hidden: hidden,
        animated: false,
        zIndex: 100,
    };
    return edge;
}

const elk = new ELK();

function getElkDimension(node) {
    let w = 150,
        h = 50;
    const arr = node.style ? node.style.split(";") : [];
    try {
        w = parseInt(
            arr
                .filter((s) => s.trim().startsWith("width"))[0]
                .split(":")[1]
                .replace("px", ""),
        );
    } catch (e) { }
    try {
        h = parseInt(
            arr
                .filter((s) => s.trim().startsWith("height"))[0]
                .split(":")[1]
                .replace("px", ""),
        );
    } catch (e) { }
    return { width: w, height: h };
}

// ELK
// - https://www.eclipse.org/elk/reference/algorithms.html
// - https://www.eclipse.org/elk/reference/options.html
const elkOptions = {
    "elk.algorithm": "layered",
    "elk.layered.spacing.nodeNodeBetweenLayers": "30",
    "elk.layered.spacing.edgeNodeBetweenLayers": "30",
    "elk.layered.spacing.edgeEdgeBetweenLayers": "30",
    "elk.layered.considerModelOrder.strategy": 'PREFER_NODES',
    "elk.hierarchyHandling": "INCLUDE_CHILDREN",
};

export function getLayoutedElements(nodes, edges, options = {}) {
    const isHorizontal = options?.["elk.direction"] === "RIGHT";
    const elkLayoutOptions = { ...elkOptions, ...options };

    const hiddenNodes = nodes.filter((node) => node?.hidden == true)
    const visibleNodes = nodes.filter((node) => node?.hidden != true)
    //const hiddenEdges = edges.filter((edge) => edge?.hidden == true)
    const visibleEdges = edges.filter((edge) => edge?.hidden != true)

    const elkNodeMap = visibleNodes.reduce((acc, node) => {
        const nodeDim = getElkDimension(node);
        acc[node.id] = {
            id: node.id,
            data: node.data,
            type: node.type ?? "",
            width: nodeDim.width,
            height: nodeDim.height,
            hidden: node.hidden ?? false,
            parentId: node.parentId,
            children: [],
            layoutOptions: {
                ...elkOptions,
                "elk.direction": "DOWN",
                "elk.padding": "[top=60.0,left=20.0,bottom=20.0,right=20.0]",
            },
        };

        return acc;
    }, {});

    const buildElkNode = (id) => {
        const node = elkNodeMap[id];
        if (!node) return undefined;
        node.children = visibleNodes
            .filter((node) => node.parentId === id)
            .map((node) => buildElkNode(node.id));
        return node;
    };

    const elkNodes = visibleNodes
        .filter((node) => node.parentId === undefined)
        .map((node) => buildElkNode(node.id));

    //console.log("elkNodes", elkNodes);

    const elkEdges = visibleEdges.map((edge) => ({
        id: edge.id,
        hidden: edge.hidden,
        sources: [edge.source],
        targets: [edge.target],
        zIndex: edge.zIndex || 100,
    }));

    //console.log("elkEdges", elkEdges);

    const graph = {
        id: "root",
        layoutOptions: elkLayoutOptions,
        children: elkNodes,
        edges: elkEdges,
    };

    const flatten = (items) => {
        let children = [];
        const flattenedItems = items.map((m) => {
            if (m.children && m.children.length > 0) {
                let mChildren = m.children?.map((child) => ({
                    ...child,
                    parentId: m.id,
                }));
                children = [...children, ...mChildren];
            }
            return m;
        });
        const flattened = flattenedItems.concat(
            children.length ? flatten(children) : children,
        );
        return flattened;
    };

    return elk
        .layout(graph)
        .then((layoutedGraph) => {
            const layoutedElkNodes = layoutedGraph.children;
            const flattenedElkNodes = flatten(layoutedElkNodes);
            //console.log("flattenedElkNodes", flattenedElkNodes);

            const svelteNodes = flattenedElkNodes.map((n) => ({
                id: n.id,
                parentId: n.parentId,
                data: n.data,
                type: n.type ?? "",
                hidden: n.hidden,
                position: { x: n.x, y: n.y },
                style: "width:" + n.width + "px; height:" + n.height + "px",
                targetPosition: isHorizontal ? Position.Left : Position.Top,
                sourcePosition: isHorizontal ? Position.Right : Position.Bottom,
            }));
            //console.log("svelteNodes", svelteNodes);
            //console.log("hiddenNodes", JSON.parse(JSON.stringify(hiddenNodes)));

            const result = {
                nodes: [...svelteNodes, ...hiddenNodes],
                edges: edges.map((edge) => ({
                    ...edge,
                    zIndex: edge.zIndex || 100,
                })),
            };
            //console.log("result", result);
            return result;
        })
        .catch(console.error);
}

