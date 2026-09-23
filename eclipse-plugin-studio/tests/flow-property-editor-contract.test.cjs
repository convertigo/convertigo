// Run the actual Java host selectors, not a JavaScript rewrite of their behavior.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const { execFileSync } = require('node:child_process');
const source = fs.readFileSync(path.join(__dirname, '../src/com/twinsoft/convertigo/eclipse/views/projectexplorer/model/FlowVirtualObjectTreeObject.java'), 'utf8');
const methods = ['usesFlowEditor', 'usesNativeEnum'].map(name => {
  const start = source.indexOf('\tprivate static boolean ' + name + '(');
  const end = source.indexOf('\n\t}\n', start);
  assert.ok(start >= 0 && end > start);
  return source.slice(start, end + 4);
}).join('\n');
assert.doesNotMatch(methods, /optString\("(?:kind|type)"|"binding"|"template"|"expression"/,
  'Java must not infer editor semantics from a private vocabulary');
const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'flow-editor-contract-'));
const javaBin = process.env.JAVA_HOME ? path.join(process.env.JAVA_HOME, 'bin') : '';
try {
  fs.writeFileSync(path.join(dir, 'EditorContractTest.java'), `
public class EditorContractTest {
  record JSONObject(String mode) { String optString(String key, String fallback) { return mode == null ? fallback : mode; } }
  ${methods}
  static void check(String mode, boolean custom, boolean choice) {
    var definition = new JSONObject(mode);
    if (usesFlowEditor("anyFutureProperty", definition) != custom || usesNativeEnum(definition) != choice)
      throw new AssertionError("Wrong host editor for " + mode);
  }
  public static void main(String[] args) {
    check("custom", true, false); check("text", false, false); check("choice", false, true);
    check(null, true, false); check("future", true, false);
    if (!usesFlowEditor("id", new JSONObject("custom"))) throw new AssertionError("Business names cannot select an editor");
    System.out.println("Java property host contract: 6 scenarios OK");
  }
}`);
  execFileSync(path.join(javaBin, 'javac'), ['--release', '21', path.join(dir, 'EditorContractTest.java')], { stdio: 'inherit' });
  execFileSync(path.join(javaBin, 'java'), ['-cp', dir, 'EditorContractTest'], { stdio: 'inherit' });
} finally { fs.rmSync(dir, { recursive: true, force: true }); }
