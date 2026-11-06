from pathlib import Path
import unittest
import xml.etree.ElementTree as ET


STUDIO = Path(__file__).resolve().parents[1]
WEB_VIEW = "com.twinsoft.convertigo.eclipse.views.rhino.JavascriptDebugView"
LEGACY_VIEW = "com.twinsoft.convertigo.eclipse.views.rhino.RhinoDebugView"


class JavascriptDebugViewTests(unittest.TestCase):
    def setUp(self):
        self.plugin = ET.parse(STUDIO / "plugin.xml").getroot()

    def test_only_the_web_backend_debugger_is_registered(self):
        views = self.plugin.findall("./extension[@point='org.eclipse.ui.views']/view")
        web = [view for view in views if view.get("id") == WEB_VIEW]
        self.assertEqual(len(web), 1)
        self.assertEqual(web[0].get("class"), WEB_VIEW)
        self.assertEqual(web[0].get("restorable"), "true")
        self.assertFalse(any(view.get("id") == LEGACY_VIEW for view in views))

    def test_perspective_places_the_registered_web_view(self):
        views = self.plugin.findall("./extension[@point='org.eclipse.ui.perspectiveExtensions']/perspectiveExtension/view")
        self.assertTrue(any(view.get("id") == WEB_VIEW for view in views))
        self.assertFalse(any(view.get("id") == LEGACY_VIEW for view in views))

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
