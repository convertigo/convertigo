/**
 * @typedef {import('./types').Flow} Flow
 * @typedef {import('./types').FlowLink} FlowLink
 * @typedef {import('./types').FlowNode} FlowNode
 * @typedef {import('./types').PaletteItem} PaletteItem
 * @typedef {import('./types').SequenceTreeNode} SequenceTreeNode
 * @typedef {'true' | 'false'} BranchKind
 * @typedef {'loop' | 'done'} LoopBranchLane
 * @typedef {{ kind: LoopBranchLane, baseY: number }} LoopLaneInfo
 * @typedef {{ id: string, branch?: BranchKind }} SubstepReference
 * @typedef {{ node: FlowNode, branch?: BranchKind }} ChildPositionInfo
 * @typedef {Object} PendingTailLink
 * @property {FlowNode} node
 * @property {BranchKind} branch
 * @property {number=} fromPortIndex
 * @property {boolean=} preferOrthogonalRouting
 */

class SequenceFlowBuilder {
	/** @type {string[]} */
	palette = ['#3b82f6', '#22c55e', '#a855f7', '#f59e0b', '#ef4444', '#06b6d4', '#eab308'];
	/** @type {Map<string, string>} */
	colorCache = new Map();
	/** @type {Set<string>} */
	loopStepIds = new Set();
	/**
	 * Normalizes a raw tree item into the shape used by the flow builder.
	 * @param {import('./types').TreeviewItem} item Source tree item coming from the admin API.
	 * @returns {Omit<SequenceTreeNode, 'children'>} Node metadata without children, ready for hydration.
	 */
	normalizeTreeItem(item) {
		const label = (item.label || item.name || item.id || '').trim();
		const hasChildren =
			item.children === true || (Array.isArray(item.children) && item.children.length > 0);
		return {
			id: item.id,
			label: label || item.id,
			name: item.name,
			icon: item.icon,
			classname: item.classname,
			isLoop: !!item.isLoop,
			isXml: !!item.isXml,
			isSourceContainer: !!item.isSourceContainer,
			hasChildren
		};
	}
	/**
	 * Converts a sequence tree into flow nodes and links for rendering.
	 * @param {string} projectName Name of the parent project the flow belongs to.
	 * @param {string} sequenceName Name of the sequence being reconstructed.
	 * @param {SequenceTreeNode[]} children Tree nodes representing the sequence steps.
	 * @param {Map<string, PaletteItem>} palette Palette lookup used to infer node styling and ports.
	 * @returns {Flow} Flow model containing nodes, links, and metadata.
	 */
	buildFlowFromTree(projectName, sequenceName, children, palette) {
		this.loopStepIds.clear();
		const sequenceId = `${projectName}.sq:${sequenceName}`;
		const nodes = [];
		const nodeMap = new Map();
		const links = [];
		const linkKeys = new Set();
		const substepsByParent = new Map();
		const childOrderByParent = new Map();
		const conditionalNodesWithBranches = new Set();
		const branchlessConditionalNodes = new Set();
		const alignmentTargetsByNode = new Map();
		const branchlessTrueTailsByNode = new Map();
		const loopParentsWithExplicitContinuation = new Set();
		let orderIndex = 0;
		const ensureAlignmentEntry = (target) => {
			let entry = alignmentTargetsByNode.get(target.id);
			if (!entry) {
				entry = { tailIds: new Set(), branchByTailId: new Map() };
				alignmentTargetsByNode.set(target.id, entry);
			}
			return entry;
		};
		const addAlignmentTail = (target, tail, branch) => {
			if (!target || !tail) {
				return;
			}
			const entry = ensureAlignmentEntry(target);
			entry.tailIds.add(tail.id);
			if (!entry.branchByTailId.has(tail.id)) {
				entry.branchByTailId.set(tail.id, branch);
			}
		};
		const hasRenderableDescendant = (candidates) => {
			if (!Array.isArray(candidates)) {
				return false;
			}
			for (const candidate of candidates) {
				if (!candidate || this.shouldSkipSequenceNode(candidate)) {
					continue;
				}
				if (candidate.icon === 'folder' || candidate.classname === 'RequestableVariable') {
					if (hasRenderableDescendant(candidate.children)) {
						return true;
					}
					continue;
				}
				const branchKind = this.branchContainerKind(candidate.classname);
				if (branchKind) {
					if (hasRenderableDescendant(candidate.children)) {
						return true;
					}
					continue;
				}
				return true;
			}
			return false;
		};
		const hasRenderableSiblingAfter = (siblings, currentIndex) => {
			for (let lookAhead = currentIndex + 1; lookAhead < siblings.length; lookAhead += 1) {
				const sibling = siblings[lookAhead];
				if (!sibling || this.shouldSkipSequenceNode(sibling)) {
					continue;
				}
				if (sibling.icon === 'folder' || sibling.classname === 'RequestableVariable') {
					if (hasRenderableDescendant(sibling.children)) {
						return true;
					}
					continue;
				}
				const branchKind = this.branchContainerKind(sibling.classname);
				if (branchKind) {
					if (hasRenderableDescendant(sibling.children)) {
						return true;
					}
					continue;
				}
				return true;
			}
			return false;
		};
		const connectPendingFalseTailsToTarget = (queue, target) => {
			if (!queue.length || !target) {
				return false;
			}
			const attempts = queue.length;
			let connected = false;
			for (let i = 0; i < attempts; i++) {
				const tail = queue.shift();
				if (!tail) {
					continue;
				}
				const tailNode = tail.node;
				if (!tailNode) {
					continue;
				}
				this.pushLink(links, linkKeys, tailNode, target, {
					fromPortIndex: tail.fromPortIndex,
					toPortIndex: 0,
					loopContext: this.isLoopNode(tailNode) ? 'done' : void 0,
					routing: tail.preferOrthogonalRouting ? 'orthogonal' : void 0
				});
				addAlignmentTail(target, tailNode, tail.branch);
				const trueTailIds = branchlessTrueTailsByNode.get(tailNode.id);
				if (trueTailIds && trueTailIds.size) {
					trueTailIds.forEach((trueTailId) => {
						const trueTailNode = nodeMap.get(trueTailId);
						if (trueTailNode) {
							addAlignmentTail(target, trueTailNode, 'true');
						}
					});
				}
				connected = true;
			}
			return connected;
		};
		const traverse = (items, parentId, branch, logicalParentId = sequenceId) => {
			let previousId;
			const pendingTrueTails = [];
			const pendingFalseTails = [];
			const createdNodeIds = [];
			const rootNodeIds = [];
			const rootNodes = [];
			let trueTailId;
			let falseTailId;
			let skipSequentialNext = false;
			for (let index = 0; index < items.length; index += 1) {
				const item = items[index];
				if (!item) {
					continue;
				}
				if (this.shouldSkipSequenceNode(item)) {
					continue;
				}
				const isLastChild = index === items.length - 1;
				let skipSequentialFlag = skipSequentialNext;
				skipSequentialNext = false;
				const branchKind = this.branchContainerKind(item.classname);
				if (branchKind) {
					const branchResult = traverse(
						item.children,
						parentId,
						branchKind === 'true' ? 'true' : 'false',
						logicalParentId
					);
					if (branchResult.pendingTrueTails.length) {
						pendingTrueTails.push(...branchResult.pendingTrueTails.map((tail) => ({ ...tail })));
					}
					if (branchResult.pendingFalseTails.length) {
						pendingFalseTails.push(...branchResult.pendingFalseTails.map((tail) => ({ ...tail })));
					}
					if (branchResult.lastProcessedId) {
						previousId = branchResult.lastProcessedId;
					}
					createdNodeIds.push(...branchResult.createdNodeIds);
					rootNodeIds.push(...branchResult.rootNodeIds);
					rootNodes.push(...branchResult.rootNodes);
					if (branchResult.trueTailId) {
						trueTailId = branchResult.trueTailId;
					}
					if (branchResult.falseTailId) {
						falseTailId = branchResult.falseTailId;
					}
					continue;
				}
				if (item.icon === 'folder' || item.classname === 'RequestableVariable') {
					const previousBeforeFolder = previousId;
					const folderResult = traverse(item.children, parentId, branch, logicalParentId);
					if (folderResult.rootNodeIds.length) {
						const firstNodeId = folderResult.rootNodeIds[0];
						const firstNode = nodeMap.get(firstNodeId);
						if (firstNode) {
							while (pendingTrueTails.length) {
								const tail = pendingTrueTails.shift();
								if (tail) {
									const tailNode = tail.node;
									this.pushLink(links, linkKeys, tailNode, firstNode, {
										fromPortIndex: tail.fromPortIndex,
										toPortIndex: 0,
										loopContext: this.isLoopNode(tailNode) ? 'done' : void 0
									});
									addAlignmentTail(firstNode, tailNode, tail.branch);
								}
							}
							connectPendingFalseTailsToTarget(pendingFalseTails, firstNode);
						}
					}
					if (folderResult.pendingTrueTails.length) {
						pendingTrueTails.push(...folderResult.pendingTrueTails.map((tail) => ({ ...tail })));
					}
					if (folderResult.pendingFalseTails.length) {
						pendingFalseTails.push(...folderResult.pendingFalseTails.map((tail) => ({ ...tail })));
					}
					if (folderResult.lastProcessedId) {
						previousId = folderResult.lastProcessedId;
					}
					createdNodeIds.push(...folderResult.createdNodeIds);
					rootNodeIds.push(...folderResult.rootNodeIds);
					rootNodes.push(...folderResult.rootNodes);
					if (folderResult.rootNodeIds.length) {
						const firstNode = nodeMap.get(folderResult.rootNodeIds[0]);
						if (firstNode) {
							if (previousBeforeFolder) {
								const prevNode = nodeMap.get(previousBeforeFolder);
								if (prevNode) {
									const prevBranchRefs = substepsByParent.get(prevNode.id) ?? [];
									const prevHasBranchChildren = prevBranchRefs.some(
										(ref) => ref.branch === 'true' || ref.branch === 'false'
									);
									const shouldSkipSequential =
										skipSequentialFlag ||
										(!branch &&
											this.isConditionalNode(prevNode) &&
											(conditionalNodesWithBranches.has(prevNode.id) || prevHasBranchChildren));
									if (!shouldSkipSequential) {
										this.pushLink(links, linkKeys, prevNode, firstNode, {
											loopContext: this.isLoopNode(prevNode) ? 'done' : void 0
										});
									}
								}
							} else if (parentId) {
								const parentNode2 = nodeMap.get(parentId);
								if (parentNode2) {
									const parentTruePort2 = this.conditionalTruePortIndex(parentNode2);
									const isConditionalParent2 = parentTruePort2 !== void 0;
									this.pushLink(links, linkKeys, parentNode2, firstNode, {
										fromPortIndex: parentTruePort2,
										preferBottomPort: !isConditionalParent2,
										loopContext: this.isLoopNode(parentNode2) ? 'body' : void 0
									});
								}
							}
						}
					}
					continue;
				}
				const flowNode = this.toFlowNode(item, orderIndex, palette, logicalParentId, branch);
				orderIndex += 1;
				nodes.push(flowNode);
				nodeMap.set(flowNode.id, flowNode);
				if (this.isLoopNode(flowNode)) {
					this.loopStepIds.add(flowNode.id);
				}
				createdNodeIds.push(flowNode.id);
				rootNodeIds.push(flowNode.id);
				rootNodes.push({ id: flowNode.id, branch });
				let childPendingTrue = [];
				let childPendingFalse = [];
				let nodeTrueTailId;
				let nodeFalseTailId;
				let hasTrueBranchChildren = false;
				let hasFalseBranchChildren = false;
				let hasImplicitTrueBranch = false;
				const isConditionalNode = this.isConditionalNode(flowNode);
				const hasBranchContainers = Array.isArray(item.children)
					? item.children.some((child) => {
							const kind = this.branchContainerKind(child.classname);
							return kind === 'true' || kind === 'false';
						})
					: false;
				const isBranchlessConditionalCandidate = isConditionalNode && !hasBranchContainers;
				const isReturnNode = this.isReturnNode(flowNode);
				const isBreakNode = this.isBreakNode(flowNode);
				if (isBranchlessConditionalCandidate) {
					branchlessConditionalNodes.add(flowNode.id);
				} else {
					branchlessConditionalNodes.delete(flowNode.id);
				}
				const parentNode = parentId ? nodeMap.get(parentId) : void 0;
				const parentTruePort = parentNode ? this.conditionalTruePortIndex(parentNode) : void 0;
				const parentFalsePort = parentNode ? this.conditionalFalsePortIndex(parentNode) : void 0;
				const isConditionalParent = parentTruePort !== void 0 || parentFalsePort !== void 0;
				const parentIsLoop = !!(parentNode && this.isLoopNode(parentNode));
				if (isReturnNode || (isBreakNode && parentIsLoop)) {
					skipSequentialNext = true;
				}
				const targetNode = flowNode;
				while (pendingTrueTails.length) {
					const tail = pendingTrueTails.shift();
					if (tail) {
						const tailNode = tail.node;
						this.pushLink(links, linkKeys, tailNode, targetNode, {
							fromPortIndex: tail.fromPortIndex,
							toPortIndex: 0,
							loopContext: this.isLoopNode(tailNode) ? 'done' : void 0,
							routing: tail.preferOrthogonalRouting ? 'orthogonal' : void 0
						});
						addAlignmentTail(targetNode, tailNode, tail.branch);
					}
				}
				const connectedFalseTails = connectPendingFalseTailsToTarget(pendingFalseTails, targetNode);
				let connected = connectedFalseTails;
				if (!connected) {
					if (previousId) {
						const prevNode = nodeMap.get(previousId);
						const prevBranchRefs = prevNode ? (substepsByParent.get(prevNode.id) ?? []) : [];
						const prevHasBranchChildren = prevBranchRefs.some(
							(ref) => ref.branch === 'true' || ref.branch === 'false'
						);
						const shouldSkipSequential =
							skipSequentialFlag ||
							(!branch &&
								!!prevNode &&
								this.isConditionalNode(prevNode) &&
								(conditionalNodesWithBranches.has(prevNode.id) || prevHasBranchChildren));
						if (prevNode && !shouldSkipSequential) {
							this.pushLink(links, linkKeys, prevNode, flowNode, {
								loopContext: this.isLoopNode(prevNode) ? 'done' : void 0
							});
							addAlignmentTail(flowNode, prevNode, branch);
							connected = true;
						}
					} else if (parentNode) {
						const branchFromParent = branch;
						let fromPortIndex;
						if (branchFromParent === 'false') {
							fromPortIndex = parentFalsePort ?? parentTruePort;
						} else if (branchFromParent === 'true') {
							fromPortIndex = parentTruePort;
						} else {
							fromPortIndex = parentTruePort;
						}
						const preferBottom = branchFromParent === 'false' ? true : !isConditionalParent;
						const loopContext =
							this.isLoopNode(parentNode) &&
							branchFromParent !== 'true' &&
							branchFromParent !== 'false'
								? 'body'
								: void 0;
						this.pushLink(links, linkKeys, parentNode, flowNode, {
							fromPortIndex,
							preferBottomPort: preferBottom,
							loopContext
						});
						addAlignmentTail(flowNode, parentNode, branchFromParent);
						connected = true;
					}
				}
				let branchTailId;
				if (item.children.length) {
					childOrderByParent.set(
						flowNode.id,
						item.children.map((child) => child.id)
					);
					const implicitChildBranch =
						isBranchlessConditionalCandidate && item.children.length ? 'true' : void 0;
					const childResult = traverse(
						item.children,
						flowNode.id,
						implicitChildBranch,
						flowNode.id
					);
					childPendingTrue = childResult.pendingTrueTails.map((tail) => ({ ...tail }));
					childPendingFalse = childResult.pendingFalseTails.map((tail) => ({ ...tail }));
					nodeTrueTailId = childResult.trueTailId;
					nodeFalseTailId = childResult.falseTailId;
					createdNodeIds.push(...childResult.createdNodeIds);
					hasTrueBranchChildren =
						isConditionalNode && childResult.rootNodes.some((ref) => ref.branch === 'true');
					hasFalseBranchChildren =
						isConditionalNode && childResult.rootNodes.some((ref) => ref.branch === 'false');
					const hasBranchChildren = hasTrueBranchChildren || hasFalseBranchChildren;
					hasImplicitTrueBranch =
						isConditionalNode && !hasBranchChildren && childResult.rootNodeIds.length > 0;
					if (hasBranchChildren || hasImplicitTrueBranch) {
						conditionalNodesWithBranches.add(flowNode.id);
					}
					if (
						hasImplicitTrueBranch &&
						childResult.lastProcessedId &&
						childResult.lastProcessedId !== flowNode.id
					) {
						nodeTrueTailId = childResult.lastProcessedId;
					}
					const canTargetParentBottom = (flowNode.bottomInputs ?? 0) > 0;
					if (canTargetParentBottom) {
						let retargetedTrue = false;
						let retargetedFalse = false;
						if (childPendingTrue.length) {
							childPendingTrue.forEach((tail) => {
								const tailNode = tail.node;
								if (!tailNode) {
									return;
								}
								this.pushLink(links, linkKeys, tailNode, flowNode, {
									fromPortIndex: tail.fromPortIndex,
									targetBottom: true,
									loopContext: this.isLoopNode(tailNode) ? 'done' : void 0
								});
							});
							retargetedTrue = true;
						} else if (childResult.trueTailId) {
							const trueTailNode = nodeMap.get(childResult.trueTailId);
							if (trueTailNode) {
								let fromPortIndex = this.rightmostSideOutputPortIndex(trueTailNode);
								if (this.isConditionalNode(trueTailNode)) {
									const truePort = this.conditionalTruePortIndex(trueTailNode);
									if (truePort !== void 0) {
										fromPortIndex = truePort;
									}
								}
								this.pushLink(links, linkKeys, trueTailNode, flowNode, {
									fromPortIndex,
									targetBottom: true,
									loopContext: this.isLoopNode(trueTailNode) ? 'done' : void 0
								});
								retargetedTrue = true;
							}
						}
						if (childPendingFalse.length) {
							childPendingFalse.forEach((tail) => {
								const tailNode = tail.node;
								if (!tailNode) {
									return;
								}
								this.pushLink(links, linkKeys, tailNode, flowNode, {
									fromPortIndex: tail.fromPortIndex,
									targetBottom: true,
									loopContext: this.isLoopNode(tailNode) ? 'done' : void 0
								});
							});
							retargetedFalse = true;
						} else if (childResult.falseTailId) {
							const falseTailNode = nodeMap.get(childResult.falseTailId);
							if (falseTailNode) {
								if (this.isConditionalNode(falseTailNode)) {
									const falsePort = this.conditionalFalsePortIndex(falseTailNode);
									if (falsePort === void 0) {
										const candidateFrom = this.rightmostSideOutputPortIndex(falseTailNode);
										this.pushLink(links, linkKeys, falseTailNode, flowNode, {
											fromPortIndex: candidateFrom,
											targetBottom: true,
											loopContext: this.isLoopNode(falseTailNode) ? 'done' : void 0
										});
										retargetedFalse = true;
									}
								} else {
									const candidateFrom = this.rightmostSideOutputPortIndex(falseTailNode);
									this.pushLink(links, linkKeys, falseTailNode, flowNode, {
										fromPortIndex: candidateFrom,
										targetBottom: true,
										loopContext: this.isLoopNode(falseTailNode) ? 'done' : void 0
									});
									retargetedFalse = true;
								}
							}
						}
						if (retargetedTrue) {
							nodeTrueTailId = void 0;
							childPendingTrue = [];
						}
						if (retargetedFalse) {
							nodeFalseTailId = void 0;
							childPendingFalse = [];
						}
					}
					if (childResult.rootNodes.length) {
						const normalizedRootRefs = hasImplicitTrueBranch
							? childResult.rootNodes.map((ref) => ({ id: ref.id, branch: 'true' }))
							: childResult.rootNodes;
						substepsByParent.set(flowNode.id, [...normalizedRootRefs]);
					}
					if (childResult.lastProcessedId && childResult.lastProcessedId !== flowNode.id) {
						const tailNodeId = childResult.lastProcessedId;
						const tailNode = nodeMap.get(tailNodeId);
						let forwardedTailId = tailNodeId;
						if (!isConditionalNode && tailNode) {
							const tailIsBreak = this.isBreakNode(tailNode);
							const tailIsReturn = this.isReturnNode(tailNode);
							if (!(tailIsBreak || tailIsReturn)) {
								const loopContext = this.isLoopNode(tailNode) ? 'done' : void 0;
								const fromPortIndexOverride = this.isLoopNode(tailNode)
									? this.loopDonePortIndex(tailNode)
									: this.rightmostSideOutputPortIndex(tailNode);
								const falsePort = this.conditionalFalsePortIndex(tailNode);
								const isDirectFalseFromConditional =
									this.isConditionalNode(tailNode) &&
									falsePort !== void 0 &&
									fromPortIndexOverride === falsePort;
								if (!isDirectFalseFromConditional) {
									this.pushLink(links, linkKeys, tailNode, flowNode, {
										fromPortIndex: fromPortIndexOverride,
										targetBottom: true,
										loopContext
									});
									const hasBottomPorts =
										(flowNode.bottomOutputs ?? 0) > 0 || (flowNode.bottomInputs ?? 0) > 0;
									if (hasBottomPorts) {
										forwardedTailId = flowNode.id;
									}
								} else {
									forwardedTailId = flowNode.id;
								}
							} else {
								forwardedTailId = flowNode.id;
							}
						}
						branchTailId = forwardedTailId;
					} else {
						branchTailId = void 0;
					}
					if (childResult.trueTailId) {
						trueTailId = childResult.trueTailId;
					}
					if (childResult.falseTailId) {
						falseTailId = childResult.falseTailId;
					}
				}
				let shouldLinkTruePortToNextSibling = false;
				let branchlessFalse = false;
				let handledLoopFalse = false;
				let handledLoopTrue = false;
				if (isConditionalNode) {
					const hasSequentialSibling = hasRenderableSiblingAfter(items, index);
					const hasExplicitBranchChildren = hasTrueBranchChildren || hasFalseBranchChildren;
					shouldLinkTruePortToNextSibling = !hasExplicitBranchChildren && !hasImplicitTrueBranch;
					branchlessFalse = !hasFalseBranchChildren;
					const treatAsBranching =
						hasExplicitBranchChildren || hasImplicitTrueBranch || shouldLinkTruePortToNextSibling;
					if (treatAsBranching) {
						skipSequentialNext = true;
						conditionalNodesWithBranches.add(flowNode.id);
					}
					let trueTailLinks = [];
					if (childPendingTrue.length) {
						trueTailLinks = [...childPendingTrue];
					} else if (nodeTrueTailId) {
						const trueTailNode = nodeMap.get(nodeTrueTailId);
						if (trueTailNode) {
							trueTailLinks = [{ node: trueTailNode, branch: 'true' }];
						}
					} else if (shouldLinkTruePortToNextSibling) {
						const truePortForNode = this.conditionalTruePortIndex(flowNode);
						trueTailLinks = [
							{
								node: flowNode,
								branch: 'true',
								fromPortIndex: truePortForNode
							}
						];
					}
					if (branchlessFalse && trueTailLinks.length) {
						const trueTailIds = trueTailLinks
							.map((tail) => tail.node?.id)
							.filter((id) => !!id && id !== flowNode.id);
						if (trueTailIds.length) {
							branchlessTrueTailsByNode.set(flowNode.id, new Set(trueTailIds));
						} else {
							branchlessTrueTailsByNode.delete(flowNode.id);
						}
					} else {
						branchlessTrueTailsByNode.delete(flowNode.id);
					}
					const shouldLoopFalseToParent = branchlessFalse && parentIsLoop && !hasSequentialSibling;
					if (shouldLoopFalseToParent) {
						const falsePortForNode = this.conditionalFalsePortIndex(flowNode);
						if (falsePortForNode !== void 0 && parentNode) {
							this.pushLink(links, linkKeys, flowNode, parentNode, {
								fromPortIndex: falsePortForNode,
								loopContext: 'body'
							});
							addAlignmentTail(parentNode, flowNode, 'false');
							childPendingFalse = [];
							nodeFalseTailId = void 0;
							handledLoopFalse = true;
							loopParentsWithExplicitContinuation.add(parentNode.id);
						}
					}
					if (childPendingFalse.length) {
						pendingFalseTails.push(...childPendingFalse);
					} else if (nodeFalseTailId && !handledLoopFalse) {
						const falseTailNode = nodeMap.get(nodeFalseTailId);
						if (falseTailNode) {
							pendingFalseTails.push({ node: falseTailNode, branch: 'false' });
						}
					}
					if (parentIsLoop && !isReturnNode && !isBreakNode && parentNode) {
						if (trueTailLinks.length) {
							trueTailLinks.forEach((tail) => {
								const tailNode = tail.node;
								if (!tailNode) {
									return;
								}
								this.pushLink(links, linkKeys, tailNode, parentNode, {
									fromPortIndex: tail.fromPortIndex,
									loopContext: 'body'
								});
							});
							loopParentsWithExplicitContinuation.add(parentNode.id);
							childPendingTrue = [];
							nodeTrueTailId = void 0;
							handledLoopTrue = true;
						}
					}
					if (!handledLoopTrue) {
						if (trueTailLinks.length) {
							pendingTrueTails.push(...trueTailLinks);
						}
					} else {
						trueTailLinks = [];
					}
				} else {
					if (childPendingTrue.length) {
						pendingTrueTails.push(...childPendingTrue);
					}
					if (childPendingFalse.length) {
						pendingFalseTails.push(...childPendingFalse);
					}
				}
				if (isConditionalNode && !(handledLoopFalse && branchlessFalse)) {
					const falsePortForNode = this.conditionalFalsePortIndex(flowNode);
					if (falsePortForNode !== void 0 && isBranchlessConditionalCandidate) {
						pendingFalseTails.push({
							node: flowNode,
							branch: 'false',
							fromPortIndex: falsePortForNode,
							preferOrthogonalRouting: branchlessFalse
						});
					}
				}
				if (isBreakNode && parentIsLoop) {
					const breakPortIndex =
						flowNode.outputs && flowNode.outputs > 0
							? this.rightmostSideOutputPortIndex(flowNode)
							: 0;
					pendingTrueTails.push({
						node: flowNode,
						branch: 'true',
						fromPortIndex: breakPortIndex
					});
				}
				if (
					parentIsLoop &&
					isLastChild &&
					!loopParentsWithExplicitContinuation.has(parentNode.id) &&
					!isBreakNode &&
					!isReturnNode &&
					!isConditionalNode &&
					!branch &&
					parentNode
				) {
					const loopFromPort =
						flowNode.outputs && flowNode.outputs > 0
							? this.rightmostSideOutputPortIndex(flowNode)
							: void 0;
					this.pushLink(links, linkKeys, flowNode, parentNode, {
						fromPortIndex: loopFromPort,
						routing: 'orthogonal'
					});
					addAlignmentTail(parentNode, flowNode, branch);
				}
				const shouldPropagateTail = !(
					isReturnNode ||
					(isBreakNode && parentIsLoop) ||
					handledLoopTrue
				);
				const propagatesThroughBranchTails =
					isConditionalNode && conditionalNodesWithBranches.has(flowNode.id);
				if (this.isLoopNode(flowNode)) {
					const donePortIndex = this.loopDonePortIndex(flowNode);
					const hasQueuedDone =
						donePortIndex === void 0
							? true
							: pendingTrueTails.some(
									(tail) =>
										tail.node.id === flowNode.id && (tail.fromPortIndex ?? -1) === donePortIndex
								);
					if (donePortIndex !== void 0 && !hasQueuedDone) {
						pendingTrueTails.push({
							node: flowNode,
							branch: 'true',
							fromPortIndex: donePortIndex
						});
					}
				}
				previousId = shouldPropagateTail && !propagatesThroughBranchTails ? flowNode.id : void 0;
				if (shouldPropagateTail && !propagatesThroughBranchTails) {
					const candidateTailId = branchTailId ?? flowNode.id;
					if (branch === 'true') {
						trueTailId = candidateTailId;
					} else if (branch === 'false') {
						falseTailId = candidateTailId;
					}
				} else if (branch === 'true') {
					trueTailId = branchTailId;
				} else if (branch === 'false') {
					falseTailId = branchTailId;
				}
			}
			return {
				lastProcessedId: previousId,
				createdNodeIds,
				rootNodeIds,
				trueTailId,
				falseTailId,
				rootNodes,
				pendingTrueTails: pendingTrueTails.map((tail) => ({ ...tail })),
				pendingFalseTails: pendingFalseTails.map((tail) => ({ ...tail }))
			};
		};
		traverse(children, void 0, void 0, sequenceId);
		this.ensureBottomContainerReturnLinks(links, linkKeys, nodeMap);
		this.annotateBranchPresence(nodeMap, substepsByParent);
		const bottomOutputChildren = new Map();
		for (const link of links) {
			const parentNode = nodeMap.get(link.from.nodeId);
			if (!parentNode) {
				continue;
			}
			const totalOutputs = parentNode.outputs ?? 0;
			const bottomOutputs = parentNode.bottomOutputs ?? 0;
			if (!bottomOutputs || !totalOutputs) {
				continue;
			}
			const sideOutputs = totalOutputs - bottomOutputs;
			if (link.from.portIndex >= sideOutputs) {
				let set = bottomOutputChildren.get(parentNode.id);
				if (!set) {
					set = new Set();
					bottomOutputChildren.set(parentNode.id, set);
				}
				set.add(link.to.nodeId);
			}
		}
		this.layoutNodesHorizontally(
			nodes,
			nodeMap,
			substepsByParent,
			childOrderByParent,
			alignmentTargetsByNode,
			bottomOutputChildren,
			links
		);
		if (!nodes.length) {
			return this.createPlaceholderFlow(projectName, sequenceName);
		}
		return {
			id: `${projectName}.${sequenceName}`,
			name: sequenceName,
			nodes,
			links
		};
	}
	/**
	 * Marks conditional nodes with the branch lanes actually present in the
	 * rendered tree so labels can distinguish a simple false continuation from
	 * an explicit else branch.
	 * @param {Map<string, FlowNode>} nodeMap Lookup of flow nodes.
	 * @param {Map<string, SubstepReference[]>} substepsByParent Rendered child refs by parent id.
	 */
	annotateBranchPresence(nodeMap, substepsByParent) {
		substepsByParent.forEach((childRefs, parentId) => {
			const parentNode = nodeMap.get(parentId);
			if (!parentNode || !this.isConditionalNode(parentNode)) {
				return;
			}
			parentNode.data = {
				...(parentNode.data ?? {}),
				hasThenBranch: childRefs.some((ref) => ref.branch === 'true'),
				hasElseBranch: childRefs.some((ref) => ref.branch === 'false')
			};
		});
	}
	/**
	 * Transforms a sequence tree node into a flow node with visual metadata.
	 * @param item Tree node describing the step.
	 * @param orderIndex Running index used to generate fallback ids.
	 * @param palette Palette information providing port counts and colors.
	 * @param parentId Logical parent id used by editor operations.
	 * @param branch Optional branch lane for children of conditionals.
	 * @returns Flow node ready to place on the canvas.
	 */
	toFlowNode(item, orderIndex, palette, parentId = '', branch = void 0) {
		const classname = item.classname || 'Step';
		const simpleType = this.simpleTypeName(classname);
		const paletteItem = palette.get(simpleType) ?? palette.get(classname);
		const type = paletteItem ? simpleType : classname;
		const label = item.label || type;
		const isReturnStep = this.isReturnTreeNode(item);
		const isBreakStep = this.isBreakTreeNode(item);
		const baseInputs = paletteItem?.inputs ?? (paletteItem ? 0 : 1);
		let baseOutputs = paletteItem?.outputs ?? 1;
		let bottomOutputs = paletteItem?.bottomOutputs ?? 0;
		const bottomInputs = paletteItem?.bottomInputs ?? 0;
		if (isReturnStep) {
			baseOutputs = 0;
			bottomOutputs = 0;
		} else if (isBreakStep) {
			baseOutputs = Math.max(baseOutputs, 1);
		}
		const totalOutputs = (baseOutputs ?? 0) + bottomOutputs;
		const totalInputs = (baseInputs ?? 0) + bottomInputs;
		const isLoopStep = this.isLoopTreeNode(item);
		const inferredOutputLabels = this.resolveOutputLabels(
			type,
			paletteItem,
			baseOutputs,
			bottomOutputs,
			isLoopStep
		);
		const inferredInputLabels = this.resolveInputLabels(paletteItem, baseInputs, bottomInputs);
		return {
			id: item.id || `node_${orderIndex}`,
			type,
			label,
			name: item.name || item.label,
			x: 0,
			y: 0,
			color: paletteItem?.color ?? this.colorForType(type),
			inputs: totalInputs,
			outputs: totalOutputs,
			inputLabels: inferredInputLabels ?? void 0,
			outputLabels: inferredOutputLabels ?? void 0,
			bottomOutputs,
			bottomOutputLabels: paletteItem?.bottomOutputLabels
				? [...paletteItem.bottomOutputLabels]
				: void 0,
			bottomInputs,
			bottomInputLabels: paletteItem?.bottomInputLabels
				? [...paletteItem.bottomInputLabels]
				: void 0,
			data: {
				icon: item.icon,
				originalId: item.id,
				parentId,
				parentBranch: branch,
				orderIndex,
				classname,
				isLoop: isLoopStep,
				isReturn: isReturnStep,
				isBreak: isBreakStep,
				isXml: item.isXml,
				isSourceContainer: item.isSourceContainer,
				hasChildren: item.hasChildren
			}
		};
	}
	/**
	 * Creates a link between two flow nodes while deduplicating connections.
	 * @param links Mutable array the new link should be pushed into.
	 * @param linkKeys Set tracking unique link signatures.
	 * @param fromNode Source node of the connection.
	 * @param toNode Target node of the connection.
	 * @param opts Optional overrides for port selection and routing.
	 */
	pushLink(links, linkKeys, fromNode, toNode, opts = {}) {
		if (!fromNode || !toNode || fromNode.id === toNode.id) {
			return;
		}
		let fromPortIndex = opts.fromPortIndex;
		if (fromPortIndex === void 0) {
			if (opts.loopContext === 'body') {
				fromPortIndex = this.loopPortIndex(fromNode);
			} else if (opts.loopContext === 'done') {
				fromPortIndex = this.loopDonePortIndex(fromNode);
			}
		}
		if (fromPortIndex === void 0) {
			fromPortIndex = this.resolveOutputPortIndex(fromNode, opts.preferBottomPort);
		}
		const toPortIndex = opts.toPortIndex ?? this.resolveInputPortIndex(toNode, opts.targetBottom);
		const key = `${fromNode.id}:${fromPortIndex}->${toNode.id}:${toPortIndex}`;
		if (linkKeys.has(key)) {
			return;
		}
		linkKeys.add(key);
		const link = {
			id: `link_${links.length + 1}`,
			from: { nodeId: fromNode.id, portIndex: fromPortIndex },
			to: { nodeId: toNode.id, portIndex: toPortIndex }
		};
		if (opts.routing) {
			link.routing = opts.routing;
		}
		links.push(link);
	}
	/**
	 * Determines which output port index should be used for a connection.
	 * @param node Flow node supplying the outgoing link.
	 * @param preferBottom Whether a bottom port should be prioritized.
	 * @returns Zero-based port index to use.
	 */
	resolveOutputPortIndex(node, preferBottom) {
		const outputs = node.outputs ?? 0;
		if (!outputs) {
			return 0;
		}
		if (preferBottom && (node.bottomOutputs ?? 0) > 0) {
			const bottomOutputs = node.bottomOutputs ?? 0;
			const sideOutputs = outputs - bottomOutputs;
			return Math.max(sideOutputs, 0);
		}
		return 0;
	}
	/**
	 * Finds the rightmost side port for a node, falling back to the last port.
	 * @param node Node whose outputs are being inspected.
	 * @returns Port index positioned furthest to the right on the side.
	 */
	rightmostSideOutputPortIndex(node) {
		const outputs = node.outputs ?? 0;
		if (outputs <= 0) {
			return 0;
		}
		const bottomOutputs = node.bottomOutputs ?? 0;
		const sideOutputs = outputs - bottomOutputs;
		if (sideOutputs > 0) {
			return sideOutputs - 1;
		}
		return Math.max(outputs - 1, 0);
	}
	/**
	 * Selects an input port index, optionally preferring bottom inputs.
	 * @param node Node receiving the link.
	 * @param preferBottom Flag instructing to choose a bottom input when possible.
	 * @returns Zero-based input port index.
	 */
	resolveInputPortIndex(node, preferBottom) {
		const inputs = node.inputs ?? 0;
		if (!inputs) {
			return 0;
		}
		if (preferBottom && (node.bottomInputs ?? 0) > 0) {
			const bottomInputs = node.bottomInputs ?? 0;
			const sideInputs = inputs - bottomInputs;
			return Math.max(sideInputs, 0);
		}
		return 0;
	}
	/**
	 * Resolves a consistent color for a given node type, caching results.
	 * @param type Node type identifier.
	 * @returns Hex color string associated with the type.
	 */
	colorForType(type) {
		if (!type) {
			return '#3b82f6';
		}
		const cached = this.colorCache.get(type);
		if (cached) {
			return cached;
		}
		let hash = 0;
		for (let i = 0; i < type.length; i++) {
			hash = (hash << 5) - hash + type.charCodeAt(i);
			hash |= 0;
		}
		const color = this.palette[Math.abs(hash) % this.palette.length];
		this.colorCache.set(type, color);
		return color;
	}
	/**
	 * Builds a minimal placeholder flow when a sequence has no nodes.
	 * @param projectName Parent project name used for ids.
	 * @param sequenceName Sequence name used for ids and labels.
	 * @returns Flow containing a single placeholder node.
	 */
	createPlaceholderFlow(projectName, sequenceName) {
		const id = `${projectName}.${sequenceName}`;
		const nodeId = `${id}.root`;
		const label = sequenceName || 'Sequence';
		return {
			id,
			name: sequenceName,
			nodes: [
				{
					id: nodeId,
					type: 'InputVariablesStep',
					label,
					x: 180,
					y: 180,
					color: '#3b82f6',
					inputs: 0,
					outputs: 1,
					data: { placeholder: true }
				}
			],
			links: []
		};
	}
	/**
	 * Ensures bottom-output containers keep a visible return link from their last child lane node.
	 * @param {FlowLink[]} links Existing mutable links.
	 * @param {Set<string>} linkKeys Deduplication keys used by pushLink.
	 * @param {Map<string, FlowNode>} nodeMap Flow nodes by id.
	 */
	ensureBottomContainerReturnLinks(links, linkKeys, nodeMap) {
		const outgoing = this.groupLinksBySource(links);
		for (const parentNode of nodeMap.values()) {
			if ((parentNode.bottomInputs ?? 0) <= 0 || (parentNode.bottomOutputs ?? 0) <= 0) {
				continue;
			}
			const descendants = this.collectBottomOutputDescendants(parentNode, outgoing, nodeMap);
			if (!descendants.size) {
				continue;
			}
			for (const childId of descendants) {
				const childNode = nodeMap.get(childId);
				if (
					!childNode ||
					!this.isBottomContainerReturnCandidate(childNode, descendants, outgoing)
				) {
					continue;
				}
				this.pushLink(links, linkKeys, childNode, parentNode, {
					fromPortIndex: this.rightmostSideOutputPortIndex(childNode),
					targetBottom: true,
					routing: 'orthogonal'
				});
			}
		}
	}
	/**
	 * @param {FlowLink[]} links Links to group.
	 * @returns {Map<string, FlowLink[]>}
	 */
	groupLinksBySource(links) {
		const outgoing = new Map();
		for (const link of links) {
			const list = outgoing.get(link.from.nodeId) ?? [];
			list.push(link);
			outgoing.set(link.from.nodeId, list);
		}
		return outgoing;
	}
	/**
	 * @param {FlowNode} parentNode Container node with bottom output.
	 * @param {Map<string, FlowLink[]>} outgoing Links grouped by source id.
	 * @param {Map<string, FlowNode>} nodeMap Flow nodes by id.
	 * @returns {Set<string>}
	 */
	collectBottomOutputDescendants(parentNode, outgoing, nodeMap) {
		const descendants = new Set();
		const visited = new Set();
		const outputs = parentNode.outputs ?? 0;
		const bottomOutputs = parentNode.bottomOutputs ?? 0;
		const bottomPortStart = Math.max(0, outputs - bottomOutputs);
		const queue = (outgoing.get(parentNode.id) ?? [])
			.filter((link) => link.from.portIndex >= bottomPortStart)
			.map((link) => link.to.nodeId);
		while (queue.length) {
			const nodeId = queue.shift();
			if (!nodeId || nodeId === parentNode.id || visited.has(nodeId)) {
				continue;
			}
			const node = nodeMap.get(nodeId);
			if (!node || !this.isNodeContainedBy(parentNode, node)) {
				continue;
			}
			visited.add(nodeId);
			descendants.add(nodeId);
			for (const link of outgoing.get(nodeId) ?? []) {
				const targetNode = nodeMap.get(link.to.nodeId);
				if (
					link.to.nodeId !== parentNode.id &&
					!visited.has(link.to.nodeId) &&
					targetNode &&
					this.isNodeContainedBy(parentNode, targetNode)
				) {
					queue.push(link.to.nodeId);
				}
			}
		}
		return descendants;
	}
	/**
	 * Checks whether a rendered node still belongs to a container subtree.
	 * @param {FlowNode} parentNode Container node.
	 * @param {FlowNode} node Candidate node.
	 * @returns {boolean}
	 */
	isNodeContainedBy(parentNode, node) {
		const parentId = parentNode.id;
		const nodeId = node.id;
		const nodeParentId = node.data?.parentId;
		if (!parentId || !nodeId || nodeId === parentId) {
			return false;
		}
		return (
			nodeId.startsWith(`${parentId}.`) ||
			nodeParentId === parentId ||
			(typeof nodeParentId === 'string' && nodeParentId.startsWith(`${parentId}.`))
		);
	}
	/**
	 * @param {FlowNode} node Candidate child lane tail.
	 * @param {Set<string>} descendants Descendants reachable from the bottom output.
	 * @param {Map<string, FlowLink[]>} outgoing Links grouped by source id.
	 * @returns {boolean}
	 */
	isBottomContainerReturnCandidate(node, descendants, outgoing) {
		if ((node.outputs ?? 0) <= 0) {
			return false;
		}
		if (this.isConditionalNode(node) || this.isReturnNode(node) || this.isBreakNode(node)) {
			return false;
		}
		return !(outgoing.get(node.id) ?? []).some((link) => descendants.has(link.to.nodeId));
	}
	/**
	 * Determines output port labels using palette defaults and heuristics.
	 * @param type Resolved node type.
	 * @param paletteItem Palette entry containing label metadata.
	 * @param sideOutputs Number of side outputs provided by the palette.
	 * @param bottomOutputs Count of bottom outputs.
	 * @param isLoop Whether the node represents a loop.
	 * @returns Array of output labels or undefined when none apply.
	 */
	resolveOutputLabels(type, paletteItem, sideOutputs, bottomOutputs, isLoopStep) {
		const labels = paletteItem?.outputLabels ? [...paletteItem.outputLabels] : [];
		const bottomLabels = paletteItem?.bottomOutputLabels ? [...paletteItem.bottomOutputLabels] : [];
		const totalOutputs = (sideOutputs ?? 0) + bottomOutputs;
		const lower = type.toLowerCase();
		if (lower.startsWith('if')) {
			labels[0] ||= 'true';
			labels[1] ||= 'false';
		} else if (isLoopStep || this.isLoopish(lower)) {
			labels[0] ||= 'loop';
			labels[1] ||= 'done';
		}
		if (!labels.length && !bottomLabels.length) {
			return null;
		}
		if (totalOutputs <= 0) {
			return null;
		}
		const combined = Array(totalOutputs).fill('');
		for (let i = 0; i < Math.min(labels.length, sideOutputs); i++) {
			combined[i] = labels[i];
		}
		for (let i = 0; i < bottomOutputs; i++) {
			combined[sideOutputs + i] = bottomLabels[i] || '';
		}
		return combined;
	}
	/**
	 * Resolves input port labels based on palette configuration.
	 * @param paletteItem Palette entry describing the step.
	 * @param sideInputs Number of side inputs.
	 * @param bottomInputs Number of bottom inputs.
	 * @returns Array of input labels or undefined when not applicable.
	 */
	resolveInputLabels(paletteItem, sideInputs, bottomInputs) {
		const bottomLabels = paletteItem?.bottomInputLabels ? [...paletteItem.bottomInputLabels] : [];
		const totalInputs = (sideInputs ?? 0) + bottomInputs;
		if (!bottomInputs && !bottomLabels.length) {
			return null;
		}
		if (totalInputs <= 0) {
			return null;
		}
		const combined = Array(totalInputs).fill('');
		for (let i = 0; i < bottomInputs; i++) {
			combined[sideInputs + i] = bottomLabels[i] || '';
		}
		return combined;
	}
	/**
	 * Extracts a simplified type name from a fully qualified classname.
	 * @param classname Full classname string to simplify.
	 * @returns Simplified type token used for palette lookups.
	 */
	simpleTypeName(classname) {
		if (!classname) {
			return 'Step';
		}
		const parts = classname.split('.');
		return parts[parts.length - 1] || classname;
	}
	/**
	 * Computes node positions on the canvas respecting branch alignment rules.
	 * @param nodes All nodes participating in the flow.
	 * @param nodeMap Map for quick node lookup by id.
	 * @param substepsByParent Mapping of parent ids to their substep references.
	 * @param childOrderByParent Tracks original child ordering per parent.
	 * @param alignmentTargetsByNode Alignment metadata generated during traversal.
	 * @param bottomOutputChildren Mapping from parent id to bottom-output child ids.
	 * @param links Flow links influencing alignment.
	 */
	layoutNodesHorizontally(
		nodes,
		nodeMap,
		substepsByParent,
		childOrderByParent,
		alignmentTargetsByNode,
		bottomOutputChildren,
		links
	) {
		if (!nodes.length) {
			return;
		}
		const bottomChildSet = new Set();
		bottomOutputChildren.forEach((children) => {
			children.forEach((id) => bottomChildSet.add(id));
		});
		const baseX = 180;
		const baseY = 160;
		const spacingX = 260;
		const spacingY = 200;
		const nodeWidth = 170;
		const canvasWidth = 4096;
		const rightMargin = 240;
		const maxX = canvasWidth - rightMargin;
		const substepIds = new Set();
		substepsByParent.forEach((childRefs) => {
			for (const ref of childRefs) {
				substepIds.add(ref.id);
			}
		});
		const positioned = new Set();
		let column = 0;
		let row = 0;
		for (const node of nodes) {
			if (substepIds.has(node.id)) {
				continue;
			}
			const proposedX = baseX + column * spacingX;
			if (proposedX > maxX && column > 0) {
				row += 1;
				column = 0;
			}
			node.x = baseX + column * spacingX;
			node.y = baseY + row * spacingY;
			column += 1;
			positioned.add(node.id);
		}
		const childRowOffset = spacingY;
		const pending = new Map(substepsByParent);
		const maxIterations = pending.size ? pending.size * 4 : 0;
		let iteration = 0;
		while (pending.size && iteration < maxIterations) {
			let progressed = false;
			for (const [parentId, childRefs] of Array.from(pending.entries())) {
				if (!positioned.has(parentId)) {
					continue;
				}
				const parentNode = nodeMap.get(parentId);
				if (!parentNode) {
					pending.delete(parentId);
					progressed = true;
					continue;
				}
				const childInfos = childRefs
					.map((ref) => {
						const node = nodeMap.get(ref.id);
						return node && node.id !== parentId ? { node, branch: ref.branch } : void 0;
					})
					.filter((info) => !!info);
				if (!childInfos.length) {
					pending.delete(parentId);
					progressed = true;
					continue;
				}
				this.positionChildNodes(
					parentNode,
					childInfos,
					nodeMap,
					positioned,
					spacingX,
					childRowOffset,
					nodeWidth,
					childOrderByParent,
					alignmentTargetsByNode,
					bottomOutputChildren,
					links
				);
				pending.delete(parentId);
				progressed = true;
			}
			if (!progressed) {
				break;
			}
			iteration += 1;
		}
		if (pending.size) {
			pending.forEach((childRefs, parentId) => {
				const parentNode = nodeMap.get(parentId);
				if (!parentNode) {
					return;
				}
				const childInfos = childRefs
					.map((ref) => {
						const node = nodeMap.get(ref.id);
						return node && node.id !== parentId ? { node, branch: ref.branch } : void 0;
					})
					.filter((info) => !!info);
				this.positionChildNodes(
					parentNode,
					childInfos,
					nodeMap,
					positioned,
					spacingX,
					childRowOffset,
					nodeWidth,
					childOrderByParent,
					alignmentTargetsByNode,
					bottomOutputChildren,
					links
				);
			});
		}
		if (positioned.size !== nodes.length) {
			for (const node of nodes) {
				if (positioned.has(node.id)) {
					continue;
				}
				const proposedX = baseX + column * spacingX;
				if (proposedX > maxX && column > 0) {
					row += 1;
					column = 0;
				}
				node.x = baseX + column * spacingX;
				node.y = baseY + row * spacingY;
				column += 1;
				positioned.add(node.id);
			}
		}
		if (alignmentTargetsByNode.size) {
			alignmentTargetsByNode.forEach((entry, nodeId) => {
				if (bottomChildSet.has(nodeId)) {
					return;
				}
				const targetNode = nodeMap.get(nodeId);
				if (!targetNode) {
					return;
				}
				const usableTailIds = Array.from(entry.tailIds).filter((id) => {
					const tailNode = nodeMap.get(id);
					return !(
						this.isLoopNode(targetNode) &&
						tailNode &&
						this.isNodeContainedBy(targetNode, tailNode)
					);
				});
				const preferredTailIds = usableTailIds.filter(
					(id) => entry.branchByTailId.get(id) === 'true'
				);
				const tailIdsToUse = preferredTailIds.length ? preferredTailIds : usableTailIds;
				const alignNodes = tailIdsToUse
					.map((id) => nodeMap.get(id))
					.filter((n) => !!n && typeof n.x === 'number');
				if (!alignNodes.length) {
					return;
				}
				const alignX = Math.max(...alignNodes.map((n) => n.x));
				const desiredX = alignX + spacingX;
				const currentX = typeof targetNode.x === 'number' ? targetNode.x : 0;
				const nextX = Math.max(currentX, desiredX);
				if (nextX > currentX + 1e-3) {
					const shift = nextX - currentX;
					targetNode.x = nextX;
					this.shiftContainedDescendants(targetNode.id, shift, nodeMap, new Set());
				} else {
					targetNode.x = nextX;
				}
			});
		}
		if (bottomOutputChildren.size) {
			const visited = new Set();
			const rootParents = [];
			bottomOutputChildren.forEach((_, parentId) => {
				if (!bottomChildSet.has(parentId)) {
					rootParents.push(parentId);
				}
			});
			rootParents.forEach((parentId) => {
				this.recenterBottomChildren(
					parentId,
					nodeMap,
					bottomOutputChildren,
					childOrderByParent,
					substepsByParent,
					spacingX,
					childRowOffset,
					visited,
					positioned,
					nodeWidth
				);
			});
			bottomOutputChildren.forEach((_, parentId) => {
				if (!visited.has(parentId)) {
					this.recenterBottomChildren(
						parentId,
						nodeMap,
						bottomOutputChildren,
						childOrderByParent,
						substepsByParent,
						spacingX,
						childRowOffset,
						visited,
						positioned,
						nodeWidth
					);
				}
			});
		}
		this.resolveRowCollisions(nodeMap, spacingY);
		this.alignLoopLanes(nodeMap, links, spacingY);
		this.spreadLoopBodyLanes(nodeMap, spacingY, nodeWidth);
		this.spreadRowOverlaps(nodeMap, bottomOutputChildren, nodeWidth);
		this.markLoopReturnLinks(nodeMap, links);
	}
	/**
	 * Places child nodes beneath a parent while respecting branch metadata.
	 * @param parent Parent node whose children are being positioned.
	 * @param childInfos Child nodes along with their branch information.
	 * @param nodeMap Lookup of all nodes by id.
	 * @param positioned Set tracking nodes already placed on the canvas.
	 * @param spacingX Horizontal spacing between columns.
	 * @param childRowOffset Vertical offset applied to child rows.
	 * @param nodeWidth Width of a node card, used for spacing calculations.
	 * @param childOrderByParent Map preserving the child's original order.
	 * @param alignmentTargetsByNode Alignment targets to update.
	 * @param bottomOutputChildren Bottom-output mappings used for layout adjustments.
	 * @param links Flow links which may influence alignment.
	 */
	positionChildNodes(
		parentNode,
		childInfos,
		nodeMap,
		positioned,
		spacingX,
		childRowOffset,
		nodeWidth,
		childOrderByParent,
		alignmentTargetsByNode,
		bottomOutputChildren,
		links
	) {
		if (!childInfos.length) {
			return;
		}
		const preferredOrder = childOrderByParent.get(parentNode.id) ?? [];
		let orderedChildInfos = childInfos;
		if (preferredOrder.length) {
			const orderIndexById = new Map();
			preferredOrder.forEach((id, index) => orderIndexById.set(id, index));
			const fallbackBase = preferredOrder.length;
			const fallbackIndexById = new Map();
			childInfos.forEach((info, index) => fallbackIndexById.set(info.node.id, index));
			const orderFor = (info) => {
				const exact = orderIndexById.get(info.node.id);
				if (exact !== void 0) {
					return exact;
				}
				return fallbackBase + (fallbackIndexById.get(info.node.id) ?? 0);
			};
			orderedChildInfos = [...childInfos].sort((a, b) => orderFor(a) - orderFor(b));
		}
		if (typeof parentNode.x !== 'number' || typeof parentNode.y !== 'number') {
			return;
		}
		if (this.isLoopNode(parentNode)) {
			const loopPort = this.loopPortIndex(parentNode);
			const donePort = this.loopDonePortIndex(parentNode);
			const portsByChild = new Map();
			for (const link of links) {
				if (link.from.nodeId === parentNode.id) {
					portsByChild.set(link.to.nodeId, link.from.portIndex);
				}
			}
			const loopChildren = [];
			const doneChildren = [];
			const neutralChildren = [];
			const classified = new Set();
			for (const info of orderedChildInfos) {
				const child = info.node;
				const portIdx = portsByChild.get(child.id);
				if (loopPort !== void 0 && portIdx === loopPort) {
					loopChildren.push(child);
					classified.add(child.id);
				} else if (donePort !== void 0 && portIdx === donePort) {
					doneChildren.push(child);
					classified.add(child.id);
				} else {
					neutralChildren.push(child);
				}
			}
			if (!loopChildren.length && orderedChildInfos.length) {
				const fallback = orderedChildInfos[0].node;
				if (!classified.has(fallback.id)) {
					loopChildren.push(fallback);
					const idxNeutral = neutralChildren.findIndex((n) => n.id === fallback.id);
					if (idxNeutral !== -1) {
						neutralChildren.splice(idxNeutral, 1);
					}
				}
			}
			const maxAttempts = 64;
			const loopBaseY = parentNode.y + childRowOffset;
			const bodyChildIds = new Set([...loopChildren, ...neutralChildren].map((child) => child.id));
			const bodyChildren = orderedChildInfos
				.map((info) => info.node)
				.filter((child) => bodyChildIds.has(child.id));
			if (bodyChildren.length) {
				const baseX = parentNode.x + spacingX;
				bodyChildren.forEach((child, index) => {
					child.y = loopBaseY;
					child.x = baseX + index * spacingX;
					positioned.add(child.id);
				});
			}
			const doneBaseY = parentNode.y;
			if (doneChildren.length) {
				const ignoreIds = new Set(doneChildren.map((n) => n.id));
				const rowY2 = doneBaseY;
				let baseX = parentNode.x;
				let attempts = 0;
				while (attempts < maxAttempts) {
					const collision = doneChildren.some((child, index) => {
						const candidateX = baseX + index * spacingX;
						return this.rowHasCollision(
							nodeMap,
							positioned,
							rowY2,
							candidateX,
							nodeWidth,
							ignoreIds
						);
					});
					if (!collision) {
						break;
					}
					baseX += spacingX;
					attempts += 1;
				}
				doneChildren.forEach((child, index) => {
					child.y = rowY2;
					child.x = baseX + index * spacingX;
					positioned.add(child.id);
				});
			}
			return;
		}
		const bottomChildIds = bottomOutputChildren.get(parentNode.id);
		const childNodes = orderedChildInfos.map((info) => info.node);
		const isTrueContainer = this.isTrueBranchContainer(parentNode);
		if (isTrueContainer) {
			const branchSpacing = spacingX;
			const baseX = parentNode.x + branchSpacing;
			childNodes.forEach((child, index) => {
				child.y = parentNode.y;
				child.x = baseX + index * branchSpacing;
				positioned.add(child.id);
			});
			return;
		}
		const isConditionalParent = this.isConditionalNode(parentNode);
		if (isConditionalParent) {
			const branchSpacing = spacingX;
			const branchOriginX = parentNode.x + branchSpacing;
			const trueBranch = orderedChildInfos.filter((info) => info.branch === 'true');
			const falseBranch = orderedChildInfos.filter((info) => info.branch === 'false');
			const neutralInfos = orderedChildInfos.filter(
				(info) => info.branch !== 'true' && info.branch !== 'false'
			);
			const bottomNeutral = bottomChildIds
				? neutralInfos.filter((info) => bottomChildIds.has(info.node.id))
				: [];
			const regularNeutral = bottomChildIds
				? neutralInfos.filter((info) => !bottomChildIds.has(info.node.id))
				: neutralInfos;
			const trueBranchY = trueBranch.length ? parentNode.y + childRowOffset : parentNode.y;
			const falseBranchY =
				trueBranch.length && falseBranch.length ? parentNode.y : parentNode.y + childRowOffset;
			trueBranch.forEach((info, index) => {
				info.node.y = trueBranchY;
				info.node.x = branchOriginX + index * branchSpacing;
				positioned.add(info.node.id);
			});
			falseBranch.forEach((info, index) => {
				info.node.y = falseBranchY;
				info.node.x = branchOriginX + index * branchSpacing;
				positioned.add(info.node.id);
			});
			if (bottomNeutral.length) {
				const bottomSpacing = spacingX * 0.5;
				const rowY2 = parentNode.y + childRowOffset;
				bottomNeutral.forEach((info, index) => {
					info.node.y = rowY2;
					if (bottomNeutral.length === 1) {
						info.node.x = parentNode.x;
					} else {
						const relative = index - (bottomNeutral.length - 1) / 2;
						info.node.x = parentNode.x + relative * bottomSpacing;
					}
					positioned.add(info.node.id);
				});
			}
			if (regularNeutral.length) {
				const branchColumns = Math.max(trueBranch.length, falseBranch.length);
				let neutralBaseColumn = parentNode.x + branchSpacing * Math.max(branchColumns + 1, 1);
				regularNeutral.forEach((info, index) => {
					info.node.y = parentNode.y;
					let columnForNode = neutralBaseColumn;
					const alignEntry = alignmentTargetsByNode.get(info.node.id);
					if (alignEntry && alignEntry.tailIds.size) {
						const preferredTailIds = Array.from(alignEntry.tailIds).filter(
							(id) => alignEntry.branchByTailId.get(id) === 'true'
						);
						const tailIdsToUse = preferredTailIds.length
							? preferredTailIds
							: Array.from(alignEntry.tailIds);
						const alignNodes = tailIdsToUse
							.map((id) => nodeMap.get(id))
							.filter((n) => !!n && typeof n.x === 'number');
						if (alignNodes.length) {
							const alignX = Math.max(...alignNodes.map((n) => n.x));
							neutralBaseColumn = Math.max(neutralBaseColumn, alignX + branchSpacing);
							columnForNode = neutralBaseColumn;
						}
					}
					info.node.x = columnForNode + index * branchSpacing;
					positioned.add(info.node.id);
				});
			}
			return;
		}
		const rowEntries = orderedChildInfos.map((info) => info.node);
		if (!rowEntries.length) {
			return;
		}
		const rowY = parentNode.y + childRowOffset;
		const hasBottomChildren = !!(bottomChildIds && bottomChildIds.size);
		if (hasBottomChildren) {
			const ignoreIds = new Set(rowEntries.map((node) => node.id));
			const maxAttempts = 64;
			let attempt = 0;
			while (attempt < maxAttempts) {
				const baseX2 = parentNode.x;
				const collision = rowEntries.some((node, index) => {
					const candidateX = baseX2 + index * spacingX;
					return this.rowHasCollision(nodeMap, positioned, rowY, candidateX, nodeWidth, ignoreIds);
				});
				if (!collision) {
					break;
				}
				this.shiftRowRightFrom(parentNode, spacingX, nodeMap, bottomOutputChildren);
				attempt += 1;
			}
			const baseX = parentNode.x;
			rowEntries.forEach((node, index) => {
				node.y = rowY;
				node.x = baseX + index * spacingX;
				positioned.add(node.id);
			});
			return;
		}
		const lateralOffset = spacingX * 0.8;
		if (rowEntries.length === 1) {
			const node = rowEntries[0];
			node.y = rowY;
			node.x = parentNode.x;
			positioned.add(node.id);
			return;
		}
		const midpoint = (rowEntries.length - 1) / 2;
		rowEntries.forEach((node, index) => {
			const relative = index - midpoint;
			node.y = rowY;
			node.x = parentNode.x + relative * lateralOffset;
			positioned.add(node.id);
		});
	}
	/**
	 * Recenters bottom-output child lanes under their parent node.
	 * @param parentId Id of the parent node being adjusted.
	 * @param nodeMap Map for retrieving node instances.
	 * @param bottomOutputChildren Mapping from parents to their bottom children.
	 * @param childOrderByParent Order of children per parent.
	 * @param substepsByParent Substep metadata affecting layout.
	 * @param spacingX Horizontal spacing between columns.
	 * @param childRowOffset Vertical offset for child rows.
	 * @param visited Set of parent ids already processed to avoid loops.
	 * @param positioned Set of node ids already positioned.
	 * @param nodeWidth Standard node width used for alignment.
	 */
	recenterBottomChildren(
		parentId,
		nodeMap,
		bottomOutputChildren,
		childOrderByParent,
		substepsByParent,
		spacingX,
		childRowOffset,
		visited,
		positioned,
		nodeWidth
	) {
		if (visited.has(parentId)) {
			return;
		}
		const parentNode = nodeMap.get(parentId);
		if (!parentNode) {
			return;
		}
		const childIds = bottomOutputChildren.get(parentId);
		if (!childIds || !childIds.size) {
			return;
		}
		if (typeof parentNode.x !== 'number' || typeof parentNode.y !== 'number') {
			return;
		}
		visited.add(parentId);
		const parentY = parentNode.y;
		const preferredOrder = childOrderByParent.get(parentId) ?? [];
		const treeOrderedIds = preferredOrder.filter((id) => childIds.has(id));
		const substepOrderedIds = (substepsByParent.get(parentId) ?? [])
			.map((ref) => ref.id)
			.filter((id) => childIds.has(id));
		const orderedIds = treeOrderedIds.length ? treeOrderedIds : substepOrderedIds;
		const remainingIds = Array.from(childIds).filter((id) => !orderedIds.includes(id));
		const finalIds = orderedIds.concat(remainingIds);
		const childNodes = finalIds.map((childId) => nodeMap.get(childId)).filter((child) => !!child);
		if (!childNodes.length) {
			return;
		}
		const targetY = parentY + childRowOffset;
		childNodes.forEach((child) => {
			child.y = targetY;
		});
		const ignoreIds = new Set(childNodes.map((child) => child.id));
		const maxAttempts = 64;
		let attempt = 0;
		while (attempt < maxAttempts) {
			const baseX2 = parentNode.x;
			const collision = childNodes.some((child, index) => {
				const candidateX = baseX2 + index * spacingX;
				return this.rowHasCollision(nodeMap, positioned, targetY, candidateX, nodeWidth, ignoreIds);
			});
			if (!collision) {
				break;
			}
			this.shiftRowRightFrom(parentNode, spacingX, nodeMap, bottomOutputChildren);
			attempt += 1;
		}
		const baseX = parentNode.x;
		childNodes.forEach((child, index) => {
			child.x = baseX + index * spacingX;
		});
		childIds.forEach((childId) => {
			this.recenterBottomChildren(
				childId,
				nodeMap,
				bottomOutputChildren,
				childOrderByParent,
				substepsByParent,
				spacingX,
				childRowOffset,
				visited,
				positioned,
				nodeWidth
			);
		});
	}
	/**
	 * Aligns loop body and completion lanes to keep routing predictable.
	 * @param nodes Nodes under consideration for alignment.
	 * @param nodeMap Map used to resolve node references.
	 * @param links All links connecting the nodes.
	 */
	alignLoopLanes(nodeMap, links, laneSpacing) {
		if (!nodeMap.size || !links.length) {
			return;
		}
		const linksBySource = new Map();
		const linksByTarget = new Map();
		for (const link of links) {
			let list = linksBySource.get(link.from.nodeId);
			if (!list) {
				list = [];
				linksBySource.set(link.from.nodeId, list);
			}
			list.push(link);
			let targetList = linksByTarget.get(link.to.nodeId);
			if (!targetList) {
				targetList = [];
				linksByTarget.set(link.to.nodeId, targetList);
			}
			targetList.push(link);
		}
		const queue = [];
		const assigned = new Map();
		const loopPortByNode = new Map();
		const donePortByNode = new Map();
		const loopBodyBaseYByNode = new Map();
		const laneKey = (nodeId, kind) => `${nodeId}:${kind}`;
		const enqueue = (nodeId, kind, baseY) => {
			const key = laneKey(nodeId, kind);
			const existing = assigned.get(key);
			if (existing) {
				if (Math.abs(existing.baseY - baseY) > 1) {
					existing.baseY = baseY;
				}
			} else {
				assigned.set(key, { kind, baseY });
			}
			queue.push({ nodeId, kind, baseY: assigned.get(key).baseY });
		};
		const loopSeeds = [];
		for (const node of nodeMap.values()) {
			if (!this.isLoopNode(node)) {
				continue;
			}
			const existing = assigned.get(laneKey(node.id, 'done'));
			const incomingBaseNode = (linksByTarget.get(node.id) || [])
				.map((link) => nodeMap.get(link.from.nodeId))
				.filter(
					(source) =>
						!!source &&
						source.id !== node.id &&
						!this.isNodeContainedBy(node, source) &&
						typeof source.x === 'number' &&
						typeof node.x === 'number' &&
						source.x < node.x
				)
				.sort((a, b) => (b.x ?? 0) - (a.x ?? 0))[0];
			const baseY = existing?.baseY ?? incomingBaseNode?.y ?? node.y ?? 0;
			const loopPort = this.loopPortIndex(node);
			const donePortRaw = this.loopDonePortIndex(node);
			loopPortByNode.set(node.id, loopPort);
			donePortByNode.set(node.id, donePortRaw);
			loopSeeds.push({ node, baseY, loopPort, donePortRaw });
		}
		const laneCountByDoneRow = new Map();
		const rowKeyForY = (value) => String(Math.round(value / 0.5) * 0.5);
		loopSeeds
			.sort((a, b) => {
				if (Math.abs(a.baseY - b.baseY) > 1e-3) {
					return a.baseY - b.baseY;
				}
				return (a.node.x ?? 0) - (b.node.x ?? 0);
			})
			.forEach((seed) => {
				const key = rowKeyForY(seed.baseY);
				const index = laneCountByDoneRow.get(key) ?? 0;
				laneCountByDoneRow.set(key, index + 1);
				loopBodyBaseYByNode.set(seed.node.id, seed.baseY + laneSpacing * (index + 1));
			});
		for (const seed of loopSeeds) {
			const node = seed.node;
			const baseY = seed.baseY;
			enqueue(node.id, 'done', baseY);
			const totalOutputs = node.outputs ?? 0;
			const bottomOutputs = node.bottomOutputs ?? 0;
			const sideOutputs = Math.max(0, totalOutputs - bottomOutputs);
			const resolvedLoopPort =
				seed.loopPort !== void 0 ? seed.loopPort : sideOutputs > 0 ? 0 : void 0;
			const resolvedDonePort =
				seed.donePortRaw !== void 0
					? seed.donePortRaw
					: sideOutputs >= 2
						? sideOutputs - 1
						: totalOutputs >= 2
							? totalOutputs - 1
							: void 0;
			const outgoing = linksBySource.get(node.id) || [];
			for (const link of outgoing) {
				if (resolvedLoopPort !== void 0 && link.from.portIndex === resolvedLoopPort) {
					enqueue(link.to.nodeId, 'loop', loopBodyBaseYByNode.get(node.id) ?? baseY + laneSpacing);
				} else if (resolvedDonePort !== void 0 && link.from.portIndex === resolvedDonePort) {
					enqueue(link.to.nodeId, 'done', baseY);
				}
			}
		}
		const visited = new Set();
		while (queue.length) {
			const task = queue.shift();
			const key = `${task.nodeId}:${task.kind}`;
			const latest = assigned.get(key);
			if (latest && Math.abs(latest.baseY - task.baseY) > 1) {
				continue;
			}
			if (visited.has(key)) {
				continue;
			}
			visited.add(key);
			const baseY = latest?.baseY ?? task.baseY;
			const node = nodeMap.get(task.nodeId);
			if (node) {
				node.y = baseY;
			}
			const outgoing = linksBySource.get(task.nodeId);
			if (!outgoing || !outgoing.length) {
				continue;
			}
			const sourceNode = nodeMap.get(task.nodeId);
			const sideOutputs = sourceNode
				? Math.max(0, (sourceNode.outputs ?? 0) - (sourceNode.bottomOutputs ?? 0))
				: 0;
			for (const link of outgoing) {
				if (sourceNode && link.from.portIndex >= sideOutputs) {
					continue;
				}
				if (sourceNode && this.isLoopNode(sourceNode)) {
					const loopPort = loopPortByNode.get(sourceNode.id);
					const donePort = donePortByNode.get(sourceNode.id);
					if (loopPort !== void 0 && link.from.portIndex === loopPort) {
						enqueue(
							link.to.nodeId,
							'loop',
							loopBodyBaseYByNode.get(sourceNode.id) ?? baseY + laneSpacing
						);
						continue;
					}
					if (donePort !== void 0 && link.from.portIndex === donePort) {
						enqueue(link.to.nodeId, 'done', baseY);
						continue;
					}
				}
				const targetNode = nodeMap.get(link.to.nodeId);
				if (
					sourceNode &&
					targetNode &&
					this.isConditionalNode(sourceNode) &&
					targetNode.data?.parentId === sourceNode.id
				) {
					const truePort = this.conditionalTruePortIndex(sourceNode);
					const falsePort = this.conditionalFalsePortIndex(sourceNode);
					if (truePort !== void 0 && link.from.portIndex === truePort) {
						enqueue(link.to.nodeId, task.kind, baseY + laneSpacing);
						continue;
					}
					if (falsePort !== void 0 && link.from.portIndex === falsePort) {
						enqueue(link.to.nodeId, task.kind, baseY);
						continue;
					}
				}
				if (targetNode && this.isLoopNode(targetNode)) {
					const sourceX = typeof sourceNode?.x === 'number' ? sourceNode.x : 0;
					const targetX = typeof targetNode.x === 'number' ? targetNode.x : 0;
					if (task.kind === 'loop' && targetX <= sourceX) {
						continue;
					}
					enqueue(link.to.nodeId, 'done', baseY);
					continue;
				}
				enqueue(link.to.nodeId, task.kind, baseY);
			}
		}
	}
	/**
	 * Keeps loop body lanes from visually running through the previous loop body
	 * when several loops sit on the same completion row.
	 * @param {Map<string, FlowNode>} nodeMap Lookup map of all nodes.
	 * @param {number} laneSpacing Horizontal gap to reserve between loop bodies.
	 * @param {number} nodeWidth Standard node width used as occupied bounds.
	 */
	spreadLoopBodyLanes(nodeMap, laneSpacing, nodeWidth) {
		const loopsByDoneRow = new Map();
		for (const node of nodeMap.values()) {
			if (!this.isLoopNode(node) || typeof node.x !== 'number' || typeof node.y !== 'number') {
				continue;
			}
			const rowKey = String(Math.round(node.y / 0.5) * 0.5);
			const list = loopsByDoneRow.get(rowKey) ?? [];
			list.push(node);
			loopsByDoneRow.set(rowKey, list);
		}
		for (const loops of loopsByDoneRow.values()) {
			if (loops.length < 2) {
				continue;
			}
			loops.sort((left, right) => (left.x ?? 0) - (right.x ?? 0));
			let reservedEnd = -Infinity;
			for (const loopNode of loops) {
				const bodyNodes = Array.from(nodeMap.values()).filter(
					(node) =>
						node.id !== loopNode.id &&
						this.isNodeContainedBy(loopNode, node) &&
						typeof node.x === 'number' &&
						typeof node.y === 'number' &&
						node.y > (loopNode.y ?? 0) + 1
				);
				if (!bodyNodes.length) {
					continue;
				}
				const minX = Math.min(...bodyNodes.map((node) => node.x ?? 0));
				const maxX = Math.max(...bodyNodes.map((node) => (node.x ?? 0) + nodeWidth));
				const desiredMinX = Math.max(minX, reservedEnd + laneSpacing);
				if (desiredMinX > minX + 1e-3) {
					const shift = desiredMinX - minX;
					bodyNodes.forEach((node) => {
						node.x = (node.x ?? 0) + shift;
					});
					reservedEnd = Math.max(reservedEnd, maxX + shift);
				} else {
					reservedEnd = Math.max(reservedEnd, maxX);
				}
			}
		}
	}
	/**
	 * Routes loop-back links below the loop body instead of through the body lane.
	 * @param {Map<string, FlowNode>} nodeMap Lookup map of all nodes.
	 * @param {FlowLink[]} links Links to update.
	 */
	markLoopReturnLinks(nodeMap, links) {
		for (const link of links) {
			const sourceNode = nodeMap.get(link.from.nodeId);
			const targetNode = nodeMap.get(link.to.nodeId);
			if (!sourceNode || !targetNode) {
				continue;
			}
			if (!this.isLoopNode(targetNode) || !this.isNodeContainedBy(targetNode, sourceNode)) {
				continue;
			}
			link.routing = 'loop-return';
		}
	}
	/**
	 * Ensures nodes sharing a lane do not overlap horizontally.
	 * @param nodeMap Lookup map of all nodes.
	 * @param bottomOutputChildren Bottom-output descendants for propagating shifts.
	 * @param nodeWidth Standard node width, used as the minimum spacing.
	 */
	spreadRowOverlaps(nodeMap, bottomOutputChildren, nodeWidth) {
		if (!nodeMap.size) {
			return;
		}
		const toleranceY = 0.5;
		const rows = new Map();
		nodeMap.forEach((node) => {
			if (typeof node.x !== 'number' || typeof node.y !== 'number') {
				return;
			}
			const rowKey = Math.round(node.y / toleranceY) * toleranceY;
			let list = rows.get(rowKey);
			if (!list) {
				list = [];
				rows.set(rowKey, list);
			}
			list.push(node);
		});
		rows.forEach((rowNodes) => {
			if (!rowNodes || rowNodes.length < 2) {
				return;
			}
			rowNodes.sort((a, b) => {
				const ax = a.x ?? 0;
				const bx = b.x ?? 0;
				if (Math.abs(ax - bx) <= 1e-3) {
					return (a.id || '').localeCompare(b.id || '');
				}
				return ax - bx;
			});
			let lastEnd;
			rowNodes.forEach((node) => {
				if (typeof node.x !== 'number') {
					return;
				}
				const nodeStart = node.x;
				if (lastEnd === void 0) {
					lastEnd = nodeStart + nodeWidth;
					return;
				}
				if (nodeStart < lastEnd - 1e-3) {
					const shift = lastEnd - nodeStart;
					node.x = nodeStart + shift;
					this.shiftBottomDescendants(node.id, shift, nodeMap, bottomOutputChildren, new Set());
				}
				lastEnd = (node.x ?? nodeStart) + nodeWidth;
			});
		});
	}
	/**
	 * Checks whether a row intersects with already positioned nodes.
	 * @param rowNodes Nodes currently in the row being validated.
	 * @param positioned Set of nodes already positioned.
	 * @param nodeMap Lookup map for nodes by id.
	 * @returns True when a collision is detected, otherwise false.
	 */
	rowHasCollision(nodeMap, positioned, rowY, candidateX, nodeWidth, ignoreIds) {
		const start = candidateX;
		const end = candidateX + nodeWidth;
		const toleranceY = 0.5;
		for (const node of nodeMap.values()) {
			if (!positioned.has(node.id) || ignoreIds.has(node.id)) {
				continue;
			}
			if (typeof node.x !== 'number' || typeof node.y !== 'number') {
				continue;
			}
			if (Math.abs(node.y - rowY) > toleranceY) {
				continue;
			}
			const existingStart = node.x;
			const existingEnd = node.x + nodeWidth;
			if (start < existingEnd && end > existingStart) {
				return true;
			}
		}
		return false;
	}
	/**
	 * Shifts a row and all subsequent siblings to the right by one column.
	 * @param startIndex Index within the nodes array to start shifting from.
	 * @param nodes Nodes in the current row.
	 * @param spacingX Horizontal spacing increment.
	 */
	shiftRowRightFrom(anchorNode, deltaX, nodeMap, bottomOutputChildren) {
		if (typeof anchorNode.x !== 'number' || typeof anchorNode.y !== 'number') {
			return;
		}
		const rowY = anchorNode.y;
		const toleranceY = 0.5;
		const anchorX = anchorNode.x;
		const nodesToShift = Array.from(nodeMap.values())
			.filter((node) => {
				if (!node || typeof node.x !== 'number' || typeof node.y !== 'number') {
					return false;
				}
				if (Math.abs(node.y - rowY) > toleranceY) {
					return false;
				}
				if (node.id === anchorNode.id) {
					return true;
				}
				return node.x > anchorX + 1e-3;
			})
			.sort((a, b) => (a.x ?? 0) - (b.x ?? 0));
		const visited = new Set();
		for (const node of nodesToShift) {
			node.x = (node.x ?? 0) + deltaX;
			this.shiftBottomDescendants(node.id, deltaX, nodeMap, bottomOutputChildren, visited);
		}
	}
	/**
	 * Propagates a horizontal shift to all bottom-descendant nodes.
	 * @param parentId Parent whose descendants should move.
	 * @param nodeMap Lookup map of nodes.
	 * @param bottomOutputChildren Bottom-output adjacency map.
	 * @param deltaX Amount of horizontal shift to apply.
	 */
	shiftBottomDescendants(nodeId, deltaX, nodeMap, bottomOutputChildren, visited) {
		if (visited.has(nodeId)) {
			return;
		}
		visited.add(nodeId);
		const childIds = bottomOutputChildren.get(nodeId);
		if (!childIds) {
			return;
		}
		childIds.forEach((childId) => {
			const childNode = nodeMap.get(childId);
			if (!childNode) {
				return;
			}
			if (typeof childNode.x === 'number') {
				childNode.x += deltaX;
			}
			this.shiftBottomDescendants(childId, deltaX, nodeMap, bottomOutputChildren, visited);
		});
	}
	/**
	 * Keeps all already positioned visual descendants aligned when a container is shifted.
	 * @param {string} nodeId Parent node id.
	 * @param {number} deltaX Horizontal shift to apply.
	 * @param {Map<string, FlowNode>} nodeMap Lookup map of nodes.
	 * @param {Set<string>} visited Descendants already shifted.
	 */
	shiftContainedDescendants(nodeId, deltaX, nodeMap, visited) {
		if (!deltaX || visited.has(nodeId)) {
			return;
		}
		const parentNode = nodeMap.get(nodeId);
		if (!parentNode) {
			return;
		}
		visited.add(nodeId);
		for (const node of nodeMap.values()) {
			if (!node || node.id === nodeId || visited.has(node.id)) {
				continue;
			}
			if (!this.isNodeContainedBy(parentNode, node)) {
				continue;
			}
			if (typeof node.x === 'number') {
				node.x += deltaX;
			}
			visited.add(node.id);
		}
	}
	/**
	 * Resolves overlapping nodes that share the same coordinates.
	 * @param nodes Nodes included in the layout.
	 * @param nodeMap Map for fast node lookup.
	 * @param positioned Set of nodes already positioned.
	 */
	resolveRowCollisions(nodeMap, verticalOffset) {
		if (!nodeMap.size || !verticalOffset) {
			return;
		}
		const tolerance = 0.5;
		const originalY = new Map();
		nodeMap.forEach((node) => {
			if (typeof node.y === 'number') {
				originalY.set(node.id, node.y);
			}
		});
		const bucketsByX = new Map();
		nodeMap.forEach((node) => {
			if (typeof node.x !== 'number' || typeof node.y !== 'number') {
				return;
			}
			const keyX = Math.round(node.x / tolerance) * tolerance;
			let list = bucketsByX.get(keyX);
			if (!list) {
				list = [];
				bucketsByX.set(keyX, list);
			}
			list.push(node);
		});
		bucketsByX.forEach((list) => {
			if (!list || list.length < 2) {
				return;
			}
			list.sort((a, b) => {
				const ay = originalY.get(a.id) ?? a.y ?? 0;
				const by = originalY.get(b.id) ?? b.y ?? 0;
				if (Math.abs(ay - by) <= tolerance) {
					return (a.x ?? 0) - (b.x ?? 0);
				}
				return ay - by;
			});
			let lastPlacedY;
			list.forEach((node) => {
				const baseY = originalY.get(node.id) ?? node.y ?? 0;
				if (lastPlacedY === void 0) {
					node.y = baseY;
					lastPlacedY = node.y;
					return;
				}
				const currentY = typeof node.y === 'number' ? node.y : baseY;
				if (currentY <= lastPlacedY + tolerance) {
					node.y = lastPlacedY + verticalOffset;
				} else {
					node.y = currentY;
				}
				lastPlacedY = node.y;
			});
		});
	}
	/**
	 * Exposes the set of loop step ids captured during traversal.
	 * @returns Array of loop node identifiers.
	 */
	getLoopStepIds() {
		return Array.from(this.loopStepIds);
	}
	/**
	 * Determines if a tree node represents a loop construct.
	 * @param node Tree node describing a step.
	 * @returns True when the node is loop-like, otherwise false.
	 */
	isLoopTreeNode(node) {
		if (!node) {
			return false;
		}
		if (node.isLoop) {
			return true;
		}
		const classnameLower = (node.classname ?? '').toLowerCase();
		const simpleLower = this.simpleTypeName(node.classname ?? '').toLowerCase();
		const labelLower = (node.label ?? '').toLowerCase();
		return (
			this.isLoopish(classnameLower) || this.isLoopish(simpleLower) || this.isLoopish(labelLower)
		);
	}
	/**
	 * Checks whether a flow node should be flagged as a loop.
	 * @param node Flow node under inspection.
	 * @returns True if the node is a loop, else false.
	 */
	isLoopNode(node) {
		if (!node) {
			return false;
		}
		if (node.data && typeof node.data['isLoop'] === 'boolean') {
			return !!node.data['isLoop'];
		}
		const typeLower = (node.type ?? '').toLowerCase();
		const classLower = (node.data?.['classname'] ?? '').toLowerCase();
		return this.isLoopish(typeLower) || this.isLoopish(classLower);
	}
	/**
	 * Helper that matches loop-like keywords within a string.
	 * @param value Value to examine.
	 * @returns True when the value hints at loop semantics.
	 */
	isLoopish(value) {
		if (!value) {
			return false;
		}
		const lower = value.toLowerCase();
		if (!lower) {
			return false;
		}
		return (
			lower.includes('loop') ||
			lower.includes('iterator') ||
			lower.includes('iterate') ||
			lower.includes('foreach') ||
			lower.includes('repeat') ||
			lower.includes('while')
		);
	}
	/**
	 * Finds the port index representing the loop body for a loop node.
	 * @param node Loop node whose body port is required.
	 * @returns Port index pointing to the loop body, or undefined.
	 */
	loopPortIndex(node) {
		const labels = this.normalizedOutputLabels(node);
		const idx = this.findAliasIndex(labels, ['loop', 'body', 'continue', 'repeat']);
		if (idx !== -1) {
			return idx;
		}
		const outputs = node.outputs ?? 0;
		if (!outputs) {
			return void 0;
		}
		if (this.isLoopNode(node) && outputs >= 2) {
			return 0;
		}
		return void 0;
	}
	/**
	 * Resolves the port index that signals loop completion.
	 * @param node Loop node whose done port is needed.
	 * @returns Port index for the done path, or undefined.
	 */
	loopDonePortIndex(node) {
		const labels = this.normalizedOutputLabels(node);
		const idx = this.findAliasIndex(labels, ['done', 'end', 'exit', 'after', 'next']);
		if (idx !== -1) {
			return idx;
		}
		const outputs = node.outputs ?? 0;
		if (!outputs) {
			return void 0;
		}
		if (this.isLoopNode(node)) {
			return outputs - 1;
		}
		return void 0;
	}
	/**
	 * Determines if a tree view item should be ignored during hydration.
	 * @param item Tree item to evaluate.
	 * @returns True when the item should be skipped.
	 */
	shouldSkipTreeviewItem(item) {
		const classname = item.classname?.toLowerCase() || '';
		if (classname.includes('stepvariable') || classname.includes('testcase')) {
			return true;
		}
		const id = item.id || '';
		if (id.includes(':tc')) {
			return true;
		}
		return false;
	}
	/**
	 * Indicates whether a sequence node should be excluded from the flow.
	 * @param node Sequence node to examine.
	 * @returns True when the node should not be rendered.
	 */
	shouldSkipSequenceNode(node) {
		const classname = node.classname?.toLowerCase() || '';
		if (classname.includes('stepvariable') || classname.includes('testcase')) {
			return true;
		}
		const id = node.id || '';
		if (id.includes(':tc')) {
			return true;
		}
		return false;
	}
	/**
	 * Detects whether the provided flow node behaves like a conditional.
	 * @param node Flow node to test.
	 * @returns True if the node is conditional.
	 */
	isConditionalNode(node) {
		if (!node) {
			return false;
		}
		const typeLower = (node.type || '').toLowerCase();
		const rawClass = node.data?.['classname'];
		const classLower = typeof rawClass === 'string' ? rawClass.toLowerCase() : '';
		return this.isConditionalType(typeLower, classLower);
	}
	/**
	 * Determines whether a node represents a true-branch container.
	 * @param node Flow node to inspect.
	 * @returns True when the node wraps a true branch.
	 */
	isTrueBranchContainer(node) {
		if (!node) {
			return false;
		}
		const kindFromType = this.branchContainerKind(node.type);
		if (kindFromType === 'true') {
			return true;
		}
		const rawClass = node.data?.['classname'];
		const kindFromClass = typeof rawClass === 'string' ? this.branchContainerKind(rawClass) : null;
		return kindFromClass === 'true';
	}
	/**
	 * Resolves the output port index representing the true branch.
	 * @param node Conditional node whose true port is needed.
	 * @returns Port index for the true branch, or undefined.
	 */
	conditionalTruePortIndex(node) {
		const normalizedLabels = this.normalizedOutputLabels(node);
		const idx = this.findAliasIndex(normalizedLabels, ['true', 'then', 'yes']);
		if (idx !== -1) {
			return idx;
		}
		const totalOutputs = node.outputs ?? 0;
		if (!totalOutputs) {
			return void 0;
		}
		if (this.isConditionalNode(node)) {
			return 0;
		}
		return void 0;
	}
	/**
	 * Resolves the output port index representing the false branch.
	 * @param node Conditional node whose false port is needed.
	 * @returns Port index for the false branch, or undefined.
	 */
	conditionalFalsePortIndex(node) {
		const totalOutputs = node.outputs ?? 0;
		if (totalOutputs <= 1) {
			return void 0;
		}
		const normalizedLabels = this.normalizedOutputLabels(node);
		const idx = this.findAliasIndex(normalizedLabels, ['false', 'else', 'no']);
		if (idx !== -1) {
			return idx;
		}
		if (!this.isConditionalNode(node)) {
			return void 0;
		}
		const bottomOutputs = node.bottomOutputs ?? 0;
		const sideOutputs = totalOutputs - bottomOutputs;
		if (sideOutputs >= 2) {
			return 1;
		}
		return totalOutputs - 1;
	}
	/**
	 * Produces a lowercase, trimmed copy of a node's output labels.
	 * @param node Flow node whose labels are being normalized.
	 * @returns Array of normalized labels in output order.
	 */
	normalizedOutputLabels(node) {
		const total = node.outputs ?? 0;
		if (!total) {
			return [];
		}
		const labels = node.outputLabels ?? [];
		return Array.from({ length: total }, (_, index) => (labels[index] ?? '').trim().toLowerCase());
	}
	/**
	 * Finds the index of the first label matching any alias provided.
	 * @param labels Candidate labels to search.
	 * @param aliases Aliases to match against.
	 * @returns Index of the first match, or -1 when none match.
	 */
	findAliasIndex(labels, aliases) {
		for (const alias of aliases) {
			const idx = labels.findIndex((label) => label === alias);
			if (idx !== -1) {
				return idx;
			}
		}
		return -1;
	}
	isReturnTreeNode(node) {
		return this.isReturnClass(node.classname);
	}
	isBreakTreeNode(node) {
		return this.isBreakClass(node.classname);
	}
	isReturnNode(node) {
		if (!node) {
			return false;
		}
		if (node.data && typeof node.data['isReturn'] === 'boolean') {
			return !!node.data['isReturn'];
		}
		const className =
			typeof node.data?.['classname'] === 'string' ? node.data?.['classname'] : void 0;
		if (this.isReturnClass(className)) {
			return true;
		}
		const simpleType = this.simpleTypeName(node.type ?? '').toLowerCase();
		return simpleType === 'returnstep';
	}
	isBreakNode(node) {
		if (!node) {
			return false;
		}
		if (node.data && typeof node.data['isBreak'] === 'boolean') {
			return !!node.data['isBreak'];
		}
		const className =
			typeof node.data?.['classname'] === 'string' ? node.data?.['classname'] : void 0;
		if (this.isBreakClass(className)) {
			return true;
		}
		const simpleType = this.simpleTypeName(node.type ?? '').toLowerCase();
		return simpleType === 'breakstep';
	}
	isReturnClass(classname) {
		if (!classname) {
			return false;
		}
		const simple = this.simpleTypeName(classname).toLowerCase();
		return simple === 'returnstep';
	}
	isBreakClass(classname) {
		if (!classname) {
			return false;
		}
		const simple = this.simpleTypeName(classname).toLowerCase();
		return simple === 'breakstep';
	}
	/**
	 * Heuristically determines if type/class names describe a conditional.
	 * @param typeLower Lower-cased node type string.
	 * @param classLower Lower-cased classname string.
	 * @returns True when the identifiers map to a conditional.
	 */
	isConditionalType(typeLower, classLower) {
		if (!typeLower && !classLower) {
			return false;
		}
		if (typeLower.startsWith('if')) {
			return true;
		}
		if (classLower.includes('.beans.steps.if')) {
			return true;
		}
		if (typeLower.startsWith('is') && typeLower.includes('thenelse')) {
			return true;
		}
		if (classLower.includes('.beans.steps.isinthenelse')) {
			return true;
		}
		return false;
	}
	/**
	 * Maps container classnames to their branch kind when applicable.
	 * @param classname Classname to inspect.
	 * @returns Branch kind (`true`/`false`) or null when not a branch container.
	 */
	branchContainerKind(classname) {
		if (!classname) {
			return null;
		}
		const simple = this.simpleTypeName(classname);
		const lower = simple.toLowerCase();
		if (lower === 'thenstep') {
			return 'true';
		}
		if (lower === 'elsestep') {
			return 'false';
		}
		return null;
	}
}
export { SequenceFlowBuilder };
