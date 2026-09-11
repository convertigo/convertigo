const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const {execFileSync} = require('node:child_process');
const source = fs.readFileSync(path.join(__dirname, '../src/com/twinsoft/convertigo/eclipse/views/assistant/AssistantView.java'), 'utf8');
const method = source.match(/private static void persistAssistantPreferenceUrl\(String url\)[\s\S]*?\n\t\}/)[0]
  .replace('org.eclipse.jface.preference.IPersistentPreferenceStore', 'PersistentStore');
assert.match(source, /persistAssistantPreferenceUrl\(preferenceUrl\);\s*startupUrl = resolveAssistantStartupUrl\(\);/);
const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'assistant-preferences-test-'));
try {
  fs.writeFileSync(path.join(dir, 'PreferenceTest.java'), `
public class PreferenceTest {
  interface Store { String getString(String key); void setValue(String key, String value); }
  interface PersistentStore extends Store { void save() throws java.io.IOException; }
  static class FakeStore implements PersistentStore {
    String value = "https://assistant.convertigo.com/#early-access-agent";
    String disk = value;
    boolean fail;
    public String getString(String key) { return value; }
    public void setValue(String key, String value) { this.value = value; }
    public void save() throws java.io.IOException {
      if (fail) throw new java.io.IOException("disk failure");
      disk = value;
    }
  }
  static class ConvertigoPlugin {
    static final String PREFERENCE_ASSISTANT_URL = "assistant.url";
    static final FakeStore store = new FakeStore();
    static ConvertigoPlugin getDefault() { return new ConvertigoPlugin(); }
    Store getPreferenceStore() { return store; }
  }
  ${method}
  public static void main(String[] args) throws Exception {
    String local = "/projects/lib_ConvertigoAssistant/DisplayObjects/mobile/";
    persistAssistantPreferenceUrl(local);
    if (!ConvertigoPlugin.store.disk.equals(local)) throw new AssertionError("URL not persisted");
    ConvertigoPlugin.store.fail = true;
    try { persistAssistantPreferenceUrl("other"); throw new AssertionError("Save failure swallowed"); }
    catch (java.io.IOException expected) { }
    if (!ConvertigoPlugin.store.value.equals(local)) throw new AssertionError("No rollback");
    if (!ConvertigoPlugin.store.disk.equals(local)) throw new AssertionError("Persisted value changed");
    System.out.println("Assistant preference persistence and failure tests passed");
  }
}`);
  execFileSync('javac', ['--release', '21', path.join(dir, 'PreferenceTest.java')], {stdio:'inherit'});
  execFileSync('java', ['-cp', dir, 'PreferenceTest'], {stdio:'inherit'});
} finally { fs.rmSync(dir, {recursive:true, force:true}); }
