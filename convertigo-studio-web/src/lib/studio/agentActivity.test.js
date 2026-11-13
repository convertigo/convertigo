import { describe, expect, it } from 'vitest';
import {
	agentChange,
	classifyChange,
	createAgentFeed,
	createFollowPolicy,
	createTreeChangeLog,
	describeEntry,
	elapsedLabel,
	followView,
	objectOf
} from './agentActivity';

/** @param {string} id @param {string} [kind] @param {Record<string, any>} [more] */
function target(id, kind = 'sequence.child', more = {}) {
	return {
		projectName: 'Shop',
		id,
		kind,
		sequenceId: kind.startsWith('sequence') ? 'Shop.sq:Checkout' : '',
		pageId: '',
		applicationId: '',
		...more
	};
}

/** @param {string} id @param {string} [name] @param {any} [children] */
function node(id, name = id.slice(id.lastIndexOf(':') + 1), children = undefined) {
	return { id, name, label: name, icon: `icon/${name}`, ...(children ? { children } : {}) };
}

function clock(start = 1_000) {
	let time = start;
	return {
		now: () => time,
		/** @param {number} ms */
		advance(ms) {
			time += ms;
		}
	};
}

describe('agent changes', () => {
	it('names the changes the MCP tells, its words or the Studio ones', () => {
		expect(agentChange('created')).toBe('created');
		expect(agentChange('Update')).toBe('changed');
		expect(agentChange('remove')).toBe('deleted');
		expect(agentChange('rename')).toBe('renamed');
		expect(agentChange('explode')).toBe('');
		expect(agentChange(undefined)).toBe('');
	});

	it('names an object and its type from its id', () => {
		expect(objectOf('Shop.sq:Checkout.st:copy', 'sequence.child')).toEqual({
			name: 'copy',
			type: 'step'
		});
		expect(objectOf('Shop.sq:Stats.st:Rows.RowsIterator.Item.count', 'sequence.child')).toEqual({
			name: 'count',
			type: 'step'
		});
		expect(objectOf('Shop.sq:Checkout', 'sequence')).toEqual({
			name: 'Checkout',
			type: 'sequence'
		});
		expect(objectOf('Shop.Application.NgxApp.pg:Cart.Title', 'ngx.component')).toEqual({
			name: 'Title',
			type: 'component'
		});
		expect(objectOf('Shop.Application.NgxApp', 'ngx.application')).toEqual({
			name: 'NgxApp',
			type: 'application'
		});
		expect(objectOf('Shop', 'project')).toEqual({ name: 'Shop', type: 'project' });
	});

	it('shows each kind of object in its view, as a perspective', () => {
		expect(followView(target('Shop.Application.NgxApp.pg:Cart', 'ngx.page'))).toBe('frontend');
		expect(followView(target('Shop.sq:Checkout.st:copy'))).toBe('flow');
		expect(followView(target('Shop.sq:Checkout', 'sequence'))).toBe('flow');
		expect(followView(target('Shop.cn:db.tr:select', 'transaction'))).toBe('execution');
		expect(followView(target('Shop.cn:db', 'connector'))).toBe('');
		expect(followView(target('Shop', 'project'))).toBe('');
	});
});

describe('the tree change log', () => {
	it('tells what appears in or vanishes from a branch read again, not a branch read first', () => {
		const time = clock();
		const log = createTreeChangeLog({ now: time.now });
		expect(log.children('Shop.sq:Checkout', true, [node('Shop.sq:Checkout.st:a')])).toEqual([]);
		const before = [node('Shop.sq:Checkout.st:a'), node('Shop.sq:Checkout.st:b')];
		time.advance(100);
		const changes = log.children('Shop.sq:Checkout', before, [
			node('Shop.sq:Checkout.st:a'),
			node('Shop.sq:Checkout.st:c')
		]);
		expect(changes.map(({ type, id }) => `${type} ${id}`)).toEqual([
			'appeared Shop.sq:Checkout.st:c',
			'vanished Shop.sq:Checkout.st:b'
		]);
		expect(changes[0]).toMatchObject({ parentId: 'Shop.sq:Checkout', name: 'c', icon: 'icon/c' });
		expect(log.node('Shop.sq:Checkout.st:c')?.icon).toBe('icon/c');
	});

	it('compares the branches a read gives with their children, as the folders of a project', () => {
		const log = createTreeChangeLog({ now: () => 5 });
		const changes = log.children(
			'Shop',
			[node('Shop:sq', 'Sequences', [node('Shop.sq:Checkout')])],
			[node('Shop:sq', 'Sequences', [node('Shop.sq:Checkout'), node('Shop.sq:Pay')])]
		);
		expect(changes).toHaveLength(1);
		expect(changes[0]).toMatchObject({ type: 'appeared', id: 'Shop.sq:Pay', parentId: 'Shop:sq' });
	});

	it('compares a branch the tree reads first with the children an earlier read gave', () => {
		const log = createTreeChangeLog({ now: () => 5 });
		// the project read once, its connector with its transactions
		log.children('Shop', undefined, [
			node('Shop:cn', 'Connectors', [node('Shop.cn:db', 'db', [node('Shop.cn:db.tr:a')])])
		]);
		expect(log.events).toEqual([]);
		// the tree opens the connector for the first time, as the agent reveals a new transaction
		const changes = log.children('Shop.cn:db', true, [
			node('Shop.cn:db.tr:a'),
			node('Shop.cn:db.tr:b')
		]);
		expect(changes.map(({ type, id }) => `${type} ${id}`)).toEqual(['appeared Shop.cn:db.tr:b']);
	});

	it('takes the children of an object that has just appeared for new too', () => {
		const time = clock();
		const log = createTreeChangeLog({ now: time.now });
		log.children('Shop:sq', undefined, [node('Shop.sq:a')]);
		log.children('Shop:sq', [node('Shop.sq:a')], [node('Shop.sq:a'), node('Shop.sq:b')]);
		time.advance(500);
		expect(
			log
				.children('Shop.sq:b', true, [node('Shop.sq:b.st:x')])
				.map(({ type, id }) => `${type} ${id}`)
		).toEqual(['appeared Shop.sq:b.st:x']);
		// a sequence the tree reads first, which did not appear: its children are not told
		expect(log.children('Shop.sq:a', true, [node('Shop.sq:a.st:y')])).toEqual([]);
		// the steps of a sequence created a while ago, read first now, under their folder
		time.advance(60_000);
		log.children(
			'Shop:sq',
			[node('Shop.sq:a'), node('Shop.sq:b')],
			[node('Shop.sq:a'), node('Shop.sq:b'), node('Shop.sq:c')]
		);
		time.advance(45_000);
		expect(
			log
				.children('Shop.sq:c', true, [node('Shop.sq:c:st', 'Steps', [node('Shop.sq:c.st:Total')])])
				.map(({ type, id }) => `${type} ${id}`)
		).toEqual(['appeared Shop.sq:c:st', 'appeared Shop.sq:c.st:Total']);
		// a sequence renamed: its steps existed
		log.children(
			'Shop:sq',
			[node('Shop.sq:a'), node('Shop.sq:b'), node('Shop.sq:c')],
			[node('Shop.sq:a'), node('Shop.sq:b'), node('Shop.sq:d')]
		);
		expect(log.children('Shop.sq:d', true, [node('Shop.sq:d.st:Total')])).toEqual([]);
	});

	it('tells an object new to the branches read before, or in a parent new to them', () => {
		const log = createTreeChangeLog({ now: () => 5 });
		log.children('Shop', undefined, [
			node('Shop:sq', 'Sequences', [node('Shop.sq:Checkout')]),
			node('Shop:cn', 'Connectors', [node('Shop.cn:db')])
		]);
		log.children('Shop.cn:db', undefined, [node('Shop.cn:db.tr:a')]);
		expect(log.isNew('Shop.cn:db.tr:b')).toBe(true);
		expect(log.isNew('Shop.cn:db.tr:a')).toBe(false);
		expect(log.isNew('Shop.sq:Pay.st:Total')).toBe(true);
		// a step of a sequence whose steps were never read: not known
		expect(log.isNew('Shop.sq:Checkout.st:copy')).toBe(false);
		expect(
			classifyChange({ id: 'Shop.cn:db.tr:b', kind: 'transaction' }, [], {
				at: 5,
				isNew: log.isNew
			}).change
		).toBe('created');
	});

	it('forgets the old events and ignores the placeholders of a drop', () => {
		const time = clock();
		const log = createTreeChangeLog({ now: time.now, keep: 1_000 });
		log.children('P', [{ id: 'P.st:x', pending: true }], [node('P.st:y')]);
		expect(log.events.map((event) => event.id)).toEqual(['P.st:y']);
		time.advance(2_000);
		expect(log.events).toEqual([]);
	});
});

describe('the change of an object the agent reveals', () => {
	const at = 10_000;
	/** @param {'appeared' | 'vanished'} type @param {string} id @param {string} parentId @param {number} [when] */
	const seen = (type, id, parentId, when = at - 200) => ({
		type,
		id,
		parentId,
		name: id.slice(id.lastIndexOf(':') + 1),
		label: '',
		icon: '',
		at: when
	});

	it('is the one the MCP tells', () => {
		expect(
			classifyChange(target('Shop.sq:Checkout', 'sequence', { change: 'created' }), [], { at })
		).toEqual({ change: 'created', explicit: true });
	});

	it('is an opening for the application whose builder the agent opens', () => {
		const application = target('Shop.Application.NgxApp', 'ngx.application');
		expect(classifyChange(application, [], { at }).change).toBe('opened');
		expect(classifyChange({ ...application, change: 'changed' }, [], { at }).change).toBe(
			'changed'
		);
	});

	it('is a creation for an object the tree saw appear, before or after the reveal', () => {
		expect(
			classifyChange(
				target('Shop.sq:Checkout.st:c'),
				[seen('appeared', 'Shop.sq:Checkout.st:c', 'Shop.sq:Checkout', at + 900)],
				{ at }
			).change
		).toBe('created');
		expect(classifyChange(target('Shop.sq:Checkout.st:c'), [], { at }).change).toBe('changed');
		expect(
			classifyChange(
				target('Shop.sq:Checkout.st:c'),
				[seen('appeared', 'Shop.sq:Checkout.st:c', 'Shop.sq:Checkout', at - 60_000)],
				{ at }
			).change
		).toBe('changed');
	});

	it('is a rename for an object appearing as a sibling of its type vanishes, a move elsewhere', () => {
		const renamed = classifyChange(
			target('Shop.sq:Checkout.st:total'),
			[
				seen('vanished', 'Shop.sq:Checkout.st:sum', 'Shop.sq:Checkout'),
				seen('appeared', 'Shop.sq:Checkout.st:total', 'Shop.sq:Checkout')
			],
			{ at }
		);
		expect(renamed).toMatchObject({ change: 'renamed', previousName: 'sum' });
		const moved = classifyChange(
			target('Shop.sq:Pay.st:total'),
			[
				seen('vanished', 'Shop.sq:Checkout.st:total', 'Shop.sq:Checkout'),
				seen('appeared', 'Shop.sq:Pay.st:total', 'Shop.sq:Pay')
			],
			{ at }
		);
		expect(moved).toMatchObject({ change: 'moved', previousId: 'Shop.sq:Checkout.st:total' });
	});

	it('is a deletion for a parent revealed as one of its children vanishes', () => {
		const deleted = classifyChange(
			target('Shop.sq:Checkout', 'sequence'),
			[seen('vanished', 'Shop.sq:Checkout.st:old', 'Shop.sq:Checkout')],
			{ at }
		);
		expect(deleted).toMatchObject({
			change: 'deleted',
			removed: { id: 'Shop.sq:Checkout.st:old', name: 'old', parentId: 'Shop.sq:Checkout' }
		});
		// the MCP tells the deletion: the ghost of the child comes from the tree when it saw it
		const told = classifyChange(
			target('Shop.sq:Checkout', 'sequence', {
				change: 'deleted',
				previous: 'Shop.Checkout.unknown'
			}),
			[],
			{ at }
		);
		expect(told).toMatchObject({ change: 'deleted', explicit: true, removed: { name: 'unknown' } });
	});
});

describe('the activity feed of the agent', () => {
	it('lists the last action first and merges an object changed again in a row', () => {
		const time = clock();
		const feed = createAgentFeed({ now: time.now });
		const created = feed.add(target('Shop.sq:Checkout', 'sequence'), {
			change: 'created',
			explicit: false
		});
		time.advance(1_000);
		const again = feed.add(target('Shop.sq:Checkout', 'sequence'), {
			change: 'changed',
			explicit: false
		});
		expect(again).toBe(created);
		expect(created.count).toBe(2);
		expect(describeEntry(created)).toEqual({
			verb: 'Created',
			type: 'sequence',
			name: 'Checkout',
			detail: '2 changes'
		});
		time.advance(1_000);
		feed.add(target('Shop.sq:Checkout.st:copy'), { change: 'changed', explicit: false });
		expect(feed.entries.map((entry) => entry.name)).toEqual(['copy', 'Checkout']);
		// the agent comes back to its sequence: its entry is first again
		time.advance(20_000);
		feed.add(target('Shop.sq:Checkout', 'sequence'), { change: 'changed', explicit: false });
		expect(feed.entries.map((entry) => `${entry.name} ${entry.count}`)).toEqual([
			'Checkout 3',
			'copy 1'
		]);
		time.advance(100_000);
		feed.add(target('Shop.sq:Checkout.st:copy'), { change: 'changed', explicit: false });
		expect(feed.entries).toHaveLength(3);
	});

	it('updates the change the tree tells better, not the one the MCP told', () => {
		const feed = createAgentFeed({ now: () => 1 });
		const inferred = feed.add(target('Shop.sq:Checkout.st:copy'), {
			change: 'changed',
			explicit: false
		});
		expect(feed.update(inferred.key, { change: 'created', explicit: false })).toBe(true);
		expect(feed.entries[0].change).toBe('created');
		const told = feed.add(target('Shop.sq:Pay', 'sequence'), { change: 'changed', explicit: true });
		expect(feed.update(told.key, { change: 'created', explicit: false })).toBe(false);
	});

	it('tells a deletion, a rename, and keeps a bounded list', () => {
		const feed = createAgentFeed({ now: () => 1, max: 2 });
		const deleted = feed.add(target('Shop.sq:Checkout', 'sequence'), {
			change: 'deleted',
			explicit: false,
			removed: { id: 'Shop.sq:Checkout.st:old', name: 'old', parentId: '', icon: '' }
		});
		expect(describeEntry(deleted)).toEqual({
			verb: 'Deleted',
			type: 'step',
			name: 'old',
			detail: 'from Checkout'
		});
		const renamed = feed.add(target('Shop.sq:Pay', 'sequence'), {
			change: 'renamed',
			explicit: true,
			previousName: 'Payment'
		});
		expect(describeEntry(renamed).detail).toBe('was Payment');
		feed.add(target('Shop.sq:Other', 'sequence'), { change: 'created', explicit: false });
		expect(feed.entries.map((entry) => entry.name)).toEqual(['Other', 'Pay']);
	});

	it('tells how long ago', () => {
		expect(elapsedLabel(0, 2_000)).toBe('now');
		expect(elapsedLabel(0, 42_000)).toBe('42 s');
		expect(elapsedLabel(0, 5 * 60_000)).toBe('5 min');
	});
});

describe('following the agent', () => {
	it('pauses for the user working while the agent works, and resumes once the agent stopped', () => {
		const time = clock();
		const policy = createFollowPolicy({ now: time.now, activeWindow: 10_000 });
		// the agent does not work: the user only works
		expect(policy.userActed()).toBe(false);
		expect(policy.paused).toBe(false);
		policy.agent();
		expect(policy.working).toBe(true);
		expect(policy.userActed()).toBe(true);
		expect(policy.reason).toBe('user');
		expect(policy.userActed()).toBe(false);
		time.advance(5_000);
		expect(policy.expire()).toBe(false);
		time.advance(6_000);
		expect(policy.working).toBe(false);
		expect(policy.expire()).toBe(true);
		expect(policy.paused).toBe(false);
	});

	it('takes the agent for working from the start of a turn the Assistant tells to its end', () => {
		const time = clock();
		const policy = createFollowPolicy({ now: time.now, activeWindow: 10_000 });
		policy.run(true);
		// before any change, as the agent reads the project
		expect(policy.working).toBe(true);
		expect(policy.idleAt).toBe(Infinity);
		expect(policy.userActed()).toBe(true);
		policy.agent();
		time.advance(60_000);
		// a long turn, quiet for a minute: the agent still works
		expect(policy.working).toBe(true);
		expect(policy.expire()).toBe(false);
		policy.run(false);
		// the turn ended: no more waiting for the estimate
		expect(policy.working).toBe(false);
		expect(policy.expire()).toBe(true);
		expect(policy.paused).toBe(false);
		// a change after the turn, as from another agent: the estimate again
		time.advance(1_000);
		policy.agent();
		expect(policy.working).toBe(true);
		time.advance(11_000);
		expect(policy.working).toBe(false);
	});

	it('keeps a pause the user asked until the user resumes', () => {
		const time = clock();
		const policy = createFollowPolicy({ now: time.now, activeWindow: 10_000 });
		policy.pause('manual');
		policy.pause('user');
		expect(policy.reason).toBe('manual');
		time.advance(60_000);
		expect(policy.expire()).toBe(false);
		expect(policy.paused).toBe(true);
		policy.resume();
		expect(policy.paused).toBe(false);
	});
});
