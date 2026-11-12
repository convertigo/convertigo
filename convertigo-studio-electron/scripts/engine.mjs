// Prepares resources/engine, the Convertigo engine of the Electron Studio, the same for every target:
// the war of the engine exploded, the embedded Tomcat of build.gradle's tomcatVersion, its launcher
// DevEngine compiled, and the Tomcat configuration of the Eclipse Studio. Also copies the icon.
//
// Usage: node scripts/engine.mjs [--war <convertigo war>]
// The war defaults to the one `./gradlew :engine:war` writes in engine/build/libs.
import fs from 'node:fs';
import path from 'node:path';
import { parseArgs } from 'node:util';

import { cacheDir, download, extract, gradleProperty, repoDir, resourcesDir, run, electronDir } from './common.mjs';

const { values } = parseArgs({ options: { war: { type: 'string' } } });

const engineDir = path.join(resourcesDir, 'engine');
const war = values.war ? path.resolve(values.war) : defaultWar();

function defaultWar() {
	const libs = path.join(repoDir, 'engine', 'build', 'libs');
	const wars = fs.existsSync(libs) ? fs.readdirSync(libs).filter((name) => /^convertigo-.*\.war$/.test(name)) : [];
	if (wars.length !== 1) {
		throw new Error(`Expected one convertigo-*.war in ${libs} (run ./gradlew :engine:war), found ${wars.length}`);
	}
	return path.join(libs, wars[0]);
}

const MAVEN = (process.env.MAVEN_REPOSITORY || 'https://repo1.maven.org/maven2').replace(/\/$/, '');
const tomcatVersion = gradleProperty('tomcatVersion');
// the embedded Tomcat that DevEngine starts, the version the engine is built against
const TOMCAT_JARS = [
	'org.apache.tomcat.embed:tomcat-embed-core',
	'org.apache.tomcat.embed:tomcat-embed-websocket',
	'org.apache.tomcat:tomcat-annotations-api',
	'org.apache.tomcat:tomcat-dbcp',
	'org.apache.tomcat:tomcat-juli'
];

fs.rmSync(engineDir, { recursive: true, force: true });

console.log(`Exploding ${war}`);
extract(war, path.join(engineDir, 'war'));

const libDir = path.join(engineDir, 'lib');
fs.mkdirSync(libDir, { recursive: true });
for (const coordinates of TOMCAT_JARS) {
	const [group, artifact] = coordinates.split(':');
	const name = `${artifact}-${tomcatVersion}.jar`;
	const url = `${MAVEN}/${group.replaceAll('.', '/')}/${artifact}/${tomcatVersion}/${name}`;
	const cached = path.join(cacheDir, 'maven', name);
	await download(url, cached, { algorithm: 'sha512', url: `${url}.sha512` });
	fs.copyFileSync(cached, path.join(libDir, name));
}

const javac = process.env.JAVA_HOME
	? path.join(process.env.JAVA_HOME, 'bin', process.platform === 'win32' ? 'javac.exe' : 'javac')
	: 'javac';
const classpath = fs
	.readdirSync(libDir)
	.map((name) => path.join(libDir, name))
	.join(path.delimiter);
run(javac, [
	'--release',
	'21',
	'-cp',
	classpath,
	'-d',
	path.join(engineDir, 'classes'),
	path.join(electronDir, 'engine', 'DevEngine.java')
]);

// the configuration of the Tomcat of the Eclipse Studio: its context.xml and default web.xml
const confDir = path.join(engineDir, 'base', 'conf');
fs.mkdirSync(confDir, { recursive: true });
for (const name of ['context.xml', 'web.xml']) {
	fs.copyFileSync(path.join(repoDir, 'eclipse-plugin-studio', 'tomcat', 'conf', name), path.join(confDir, name));
}

fs.copyFileSync(
	path.join(repoDir, 'eclipse-repository', 'images', 'convertigo_512x512_32.png'),
	path.join(resourcesDir, 'icon.png')
);

console.log(`${engineDir}: war ${path.basename(war)}, Tomcat ${tomcatVersion}`);
