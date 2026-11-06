from pathlib import Path
import unittest
import xml.etree.ElementTree as ET


STUDIO = Path(__file__).resolve().parents[1]
WEB_CLASS = "com.twinsoft.convertigo.eclipse.views.rhino.JavascriptDebugView"
VIEW_ID = "com.twinsoft.convertigo.eclipse.views.rhino.RhinoDebugView"
PREVIOUS_WEB_ID = WEB_CLASS


class JavascriptDebugViewTests(unittest.TestCase):
    def setUp(self):
        self.plugin = ET.parse(STUDIO / "plugin.xml").getroot()

    def test_historical_view_id_resolves_to_the_web_debugger(self):
        views = self.plugin.findall("./extension[@point='org.eclipse.ui.views']/view")
        web = [view for view in views if view.get("id") == VIEW_ID]
        self.assertEqual(len(web), 1)
        self.assertEqual(web[0].get("class"), WEB_CLASS)
        self.assertEqual(web[0].get("restorable"), "true")
        self.assertEqual(web[0].get("allowMultiple"), "false")

    def test_only_one_backend_debugger_is_registered(self):
        views = self.plugin.findall("./extension[@point='org.eclipse.ui.views']/view")
        self.assertEqual(sum(view.get("class") == WEB_CLASS for view in views), 1)
        self.assertFalse(any(view.get("id") == PREVIOUS_WEB_ID for view in views))
        self.assertFalse(any(view.get("class") == VIEW_ID for view in views))

    def test_perspective_places_the_registered_web_view(self):
        views = self.plugin.findall("./extension[@point='org.eclipse.ui.perspectiveExtensions']/perspectiveExtension/view")
        self.assertTrue(any(view.get("id") == VIEW_ID for view in views))
        self.assertFalse(any(view.get("id") == PREVIOUS_WEB_ID for view in views))

    def test_registered_class_exists_and_swing_classes_are_removed(self):
        package = STUDIO / "src/com/twinsoft/convertigo/eclipse/views/rhino"
        self.assertTrue((package / "JavascriptDebugView.java").is_file())
        self.assertFalse((package / "RhinoDebugView.java").exists())
        self.assertFalse((package / "RhinoDebug.java").exists())

    def test_flatlaf_is_not_required_by_remaining_studio_sources(self):
        self.assertNotIn("com.formdev:flatlaf:", (STUDIO / "build.gradle").read_text())
        for source in (STUDIO / "src").rglob("*.java"):
            with self.subTest(source=source.relative_to(STUDIO)):
                self.assertNotIn("com.formdev.flatlaf", source.read_text())


if __name__ == "__main__":
    unittest.main()
