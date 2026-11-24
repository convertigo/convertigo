package com.twinsoft.convertigo.engine.admin.services.projects;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import jakarta.servlet.http.HttpServletRequest;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.enums.ArchiveExportOption;
import com.twinsoft.convertigo.engine.servlets.GenericServletMultipartTest.Multipart;
import com.twinsoft.convertigo.engine.servlets.GenericServletMultipartTest.Response;
import com.twinsoft.convertigo.engine.util.ProjectBundleZip.Limits;

public class ProjectBundleServicesTest {
	@Rule public TemporaryFolder folder = new TemporaryFolder();

	@Test public void validatesEveryUploadBeforeDeployingAndCleansAllTemporaryFiles() throws Exception {
		var service = new Deployment();
		var request = new Multipart().file("file", "Good.car", car("Good"))
				.file("file", "Invalid.car", text("not a zip")).request("/admin/services/projects.DeployBundle");
		assertThrows(IOException.class, () -> service.getServiceResult(request, document()));
		assertTrue(service.calls.isEmpty());
		assertFalse(Files.exists(service.temporary));
	}

	@Test public void returnsPerProjectWarningsFailureAndNotAttemptedAfterStopping() throws Exception {
		var service = new Deployment();
		service.fail = "B";
		var request = new Multipart().field("bAssembleXsl", "true")
				.file("file", "C.car", car("C")).file("file", "B.zip", car("B"))
				.file("file", "A.car", car("A")).request("/admin/services/projects.DeployBundle");
		var document = document();
		service.getServiceResult(request, document);
		assertEquals(List.of("A", "B"), service.calls);
		assertTrue(service.assemble);
		var summary = (Element) document.getElementsByTagName("deployment").item(0);
		assertEquals("false", summary.getAttribute("success"));
		var projects = summary.getElementsByTagName("project");
		assertEquals(3, projects.getLength());
		assertEquals("deployed", ((Element) projects.item(0)).getAttribute("status"));
		assertEquals("Migration save warning", ((Element) projects.item(0)).getAttribute("warning"));
		assertEquals("failed", ((Element) projects.item(1)).getAttribute("status"));
		assertEquals("simulated write failure", ((Element) projects.item(1)).getAttribute("detail"));
		assertEquals("not_attempted", ((Element) projects.item(2)).getAttribute("status"));
		assertFalse(Files.exists(service.temporary));
	}

	@Test public void enforcesHttpSizeFileCountAndFormFieldBoundsBeforeDeployment() throws Exception {
		var oversized = new Deployment();
		oversized.configured = new Limits(2, 100, 100_000, 100_000, 200, 10, 2);
		var oversizedError = assertThrows(org.apache.commons.fileupload.FileUploadBase.SizeLimitExceededException.class,
				() -> oversized.getServiceResult(new Multipart().file("file", "A.car", car("A")).request("/"), document()));
		assertTrue(oversizedError.getMessage().contains("net.upload.admin.max_size"));
		assertTrue(oversized.calls.isEmpty());
		assertFalse(Files.exists(oversized.temporary));
		var count = new Deployment();
		count.configured = new Limits(2, 100, 100_000, 100_000, 200, 100_000, 1);
		assertThrows(IOException.class, () -> count.getServiceResult(new Multipart().file("file", "A.car", car("A"))
				.file("file", "B.car", car("B")).request("/"), document()));
		assertTrue(count.calls.isEmpty());
		assertFalse(Files.exists(count.temporary));
		var fields = new Deployment();
		assertThrows(IOException.class, () -> fields.getServiceResult(new Multipart().field("option", "x".repeat(4097))
				.file("file", "A.car", car("A")).request("/"), document()));
		assertTrue(fields.calls.isEmpty());
		assertFalse(Files.exists(fields.temporary));
	}

	@Test public void acceptsArchivesUpToTheUploadSizeWithTheirMultipartEnvelope() throws Exception {
		var archive = car("A");
		var service = new Deployment();
		// the multipart envelope of this request is larger than the 16 bytes left above the archive
		service.configured = new Limits(2, 100, 100_000, 100_000, 200, archive.length + 16, 2);
		service.getServiceResult(new Multipart().file("file", "A.car", archive).request("/"), document());
		assertEquals(List.of("A"), service.calls);
		assertFalse(Files.exists(service.temporary));
	}

	@Test public void refusesArchivesOverTheUploadSizeTogetherBeforeDeployment() throws Exception {
		var a = car("A");
		var b = car("B");
		var service = new Deployment();
		service.configured = new Limits(2, 100, 100_000, 100_000, 200, Math.max(a.length, b.length) + 16, 2);
		var error = assertThrows(org.apache.commons.fileupload.FileUploadBase.SizeLimitExceededException.class,
				() -> service.getServiceResult(new Multipart().file("file", "A.car", a).file("file", "B.car", b).request("/"), document()));
		assertTrue(error.getMessage().contains("net.upload.admin.max_size"));
		assertTrue(com.twinsoft.convertigo.engine.util.AdminUploadLimits.isTooLarge(error));
		assertTrue(service.calls.isEmpty());
		assertFalse(Files.exists(service.temporary));
	}

	@Test public void deploysWithoutUploadSizeLimitByDefaultAndKeepsTheBundleQuotas() throws Exception {
		var configured = Limits.configured();
		var defaults = Limits.defaults();
		assertEquals(Long.MAX_VALUE, configured.uploadBytes());
		assertEquals(List.of(defaults.projects(), defaults.entries(), defaults.totalBytes(), defaults.entryBytes(), defaults.ratio(), defaults.uploads()),
				List.of(configured.projects(), configured.entries(), configured.totalBytes(), configured.entryBytes(), configured.ratio(), configured.uploads()));
		var service = new Deployment();
		service.configured = configured;
		service.getServiceResult(new Multipart().file("file", "A.car", car("A")).request("/"), document());
		assertEquals(List.of("A"), service.calls);
		assertFalse(Files.exists(service.temporary));
	}

	@Test public void exportsTheSelectionAndOptionsThroughTheSharedAdapter() throws Exception {
		var service = new ExportBundle() {
			@Override protected void export(Path output, List<String> names, boolean dependencies, List<String> included, List<String> excluded, Set<ArchiveExportOption> options) throws Exception {
				assertEquals(List.of("A", "B"), names);
				assertTrue(dependencies);
				assertEquals(List.of("Extra"), included);
				assertEquals(List.of("Remote"), excluded);
				assertFalse(options.contains(ArchiveExportOption.includeTestCase));
				assertTrue(options.contains(ArchiveExportOption.includeStubs));
				Files.write(output, text("bundle bytes"));
			}
		};
		var response = new Response();
		service.writeResponseResult(parameters(Map.of("projects", "[\"A\",\"B\"]", "includeDependencies", "true",
				"includeProjects", "[\"Extra\"]", "excludeProjects", "[\"Remote\"]", "exportOptions", "{\"includeTestCase\":false}")), response.proxy());
		assertEquals("bundle bytes", response.text());
		assertEquals("attachment; filename=\"projects-bundle.zip\"", response.headers.get("Content-Disposition"));
		assertEquals("12", response.headers.get("Content-Length"));
	}

	@Test public void refusesMissingExternalReferencesBeforeReplacingAnyProject() throws Exception {
		var service = new Deployment();
		var request = new Multipart().file("file", "App.car", car("App", "Remote")).request("/");
		var error = assertThrows(IOException.class, () -> service.getServiceResult(request, document()));
		assertTrue(error.getMessage().contains("Required project Remote referenced by App"));
		assertTrue(service.calls.isEmpty());
		assertFalse(Files.exists(service.temporary));
		var existing = new Deployment() { @Override protected boolean projectAvailable(String name) { return name.equals("Remote"); } };
		existing.getServiceResult(new Multipart().file("file", "App.car", car("App", "Remote")).request("/"), document());
		assertEquals(List.of("App"), existing.calls);
	}

	@Test public void previewListsSelectedProjectsAndDependenciesWithExplicitExclusions() throws Exception {
		var service = new PreviewBundle() {
			@Override protected com.twinsoft.convertigo.engine.util.ProjectBundle.Selection preview(List<String> selected, boolean dependencies, List<String> included, List<String> excluded) throws Exception {
				assertEquals(List.of("App"), selected);assertTrue(dependencies);
				assertEquals(List.of("Extra"), included);assertEquals(List.of("Remote"), excluded);
				return new com.twinsoft.convertigo.engine.util.ProjectBundle.Selection(
					com.twinsoft.convertigo.engine.util.ProjectBundlePlan.create(List.of(
						new com.twinsoft.convertigo.engine.util.ProjectBundlePlan.ProjectInfo("App", "1", Set.of("Shared", "Remote")),
						new com.twinsoft.convertigo.engine.util.ProjectBundlePlan.ProjectInfo("Shared", "2", Set.of()))), Set.of("App"), Set.of("Remote"));
			}
		};
		var document = document();
		service.getServiceResult(parameters(Map.of("projects", "[\"App\"]", "includeDependencies", "true", "includeProjects", "[\"Extra\"]", "excludeProjects", "[\"Remote\"]")), document);
		var projects = document.getElementsByTagName("project");
		assertEquals(2, projects.getLength());
		assertEquals("Shared", ((Element)projects.item(0)).getAttribute("name"));
		assertEquals("false", ((Element)projects.item(0)).getAttribute("selected"));
		assertEquals("true", ((Element)projects.item(1)).getAttribute("selected"));
		assertEquals("Remote", ((Element)document.getElementsByTagName("excludedProject").item(0)).getAttribute("name"));
		assertThrows(IllegalArgumentException.class, () -> ExportBundle.projectNames(parameters(Map.of("excludeProjects", "[\"../Remote\"]")), "excludeProjects", false));
	}

	@Test public void retainsTheSingleProjectServiceRoles() {
		assertArrayEquals(Deploy.class.getAnnotation(ServiceDefinition.class).roles(), DeployBundle.class.getAnnotation(ServiceDefinition.class).roles());
		assertArrayEquals(Export.class.getAnnotation(ServiceDefinition.class).roles(), ExportBundle.class.getAnnotation(ServiceDefinition.class).roles());
		assertArrayEquals(ExportBundle.class.getAnnotation(ServiceDefinition.class).roles(), PreviewBundle.class.getAnnotation(ServiceDefinition.class).roles());
	}

	@Test public void acceptsTheExistingUploadQueryOption() throws Exception {
		var original = new Multipart().file("file", "A.car", car("A")).request("/");
		var request = (HttpServletRequest) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {HttpServletRequest.class},
				(proxy, method, args) -> method.getName().equals("getParameter") && args[0].equals("bAssembleXsl")
						? "true" : method.invoke(original, args));
		var service = new Deployment();
		service.getServiceResult(request, document());
		assertTrue(service.assemble);
		assertEquals(List.of("A"), service.calls);
		assertFalse(Files.exists(service.temporary));
	}

	@Test public void removesPartialUploadFilesWhenTheRequestIsInterrupted() throws Exception {
		var content = new byte[100_000];
		new java.util.Random(1296).nextBytes(content);
		var original = new Multipart().file("file", "A.car", content).request("/");
		var delegate = original.getInputStream();
		var stream = new jakarta.servlet.ServletInputStream() {
			int consumed;
			@Override public int read() throws IOException {
				if (consumed++ >= 32_768) throw new IOException("interrupted test request");
				return delegate.read();
			}
			@Override public int read(byte[] bytes, int offset, int length) throws IOException {
				if (consumed >= 32_768) throw new IOException("interrupted test request");
				int count = delegate.read(bytes, offset, Math.min(length, 32_768 - consumed));
				if (count > 0) consumed += count;
				return count;
			}
			@Override public boolean isFinished() { return false; }
			@Override public boolean isReady() { return true; }
			@Override public void setReadListener(jakarta.servlet.ReadListener listener) { }
		};
		var request = (HttpServletRequest) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {HttpServletRequest.class},
				(proxy, method, args) -> method.getName().equals("getInputStream") ? stream : method.invoke(original, args));
		var service = new Deployment();
		assertThrows(org.apache.commons.fileupload.FileUploadBase.IOFileUploadException.class, () -> service.getServiceResult(request, document()));
		assertTrue(service.calls.isEmpty());
		assertFalse(Files.exists(service.temporary));
	}

	@Test public void removesPreparedExportFilesWhenTheClientStopsTheDownload() throws Exception {
		Path[] output = new Path[1];
		var service = new ExportBundle() {
			@Override protected void export(Path destination, List<String> names, boolean dependencies, List<String> included, List<String> excluded, Set<ArchiveExportOption> options) throws Exception {
				output[0] = destination;
				Files.write(destination, new byte[100_000]);
			}
		};
		var response = new Response();
		response.fail = true;
		assertThrows(IOException.class, () -> service.writeResponseResult(parameters(Map.of("projects", "[\"A\"]")), response.proxy()));
		assertFalse(Files.exists(output[0].getParent()));
	}


	@Test public void installsEveryCyclicMemberBeforeLoadingAndReportsUnfinishedSources() throws Exception {
		for (boolean installationFailure : List.of(false, true)) {
			var events = new ArrayList<String>();
			var service = new Deployment() {
				@Override protected AutoCloseable openGroup(com.twinsoft.convertigo.engine.util.ProjectBundlePlan.Group group) {
					assertTrue(group.cyclic());
					events.add("open");
					return () -> events.add("close");
				}
				@Override protected com.twinsoft.convertigo.engine.util.ProjectBundleDeployment.Completion install(HttpServletRequest request, String name, Path archive, boolean assembleXsl) throws Exception {
					events.add("install " + name);
					if (installationFailure && name.equals("B")) throw new IOException("installation stopped");
					return () -> { events.add("load " + name); throw new IOException("loading stopped"); };
				}
			};
			var request = new Multipart().file("file", "A.car", car("A", "B"))
					.file("file", "B.car", car("B", "A")).file("file", "Z.car", car("Z")).request("/");
			var document = document();
			service.getServiceResult(request, document);
			assertEquals(installationFailure ? List.of("open", "install A", "install B", "close")
					: List.of("open", "install A", "install B", "load A", "close"), events);
			var projects = document.getElementsByTagName("project");
			assertEquals(3, projects.getLength());
			assertEquals(installationFailure ? "installed" : "failed", ((Element) projects.item(0)).getAttribute("status"));
			assertEquals(installationFailure ? "failed" : "installed", ((Element) projects.item(1)).getAttribute("status"));
			assertEquals("not_attempted", ((Element) projects.item(2)).getAttribute("status"));
			assertFalse(Files.exists(service.temporary));
		}
	}

	@Test public void putsTheWholeSelectionOffTheRuntimeWarmupUntilTheBundleEnds() throws Exception {
		for (boolean failure : List.of(false, true)) {
			var events = new ArrayList<String>();
			var service = new Deployment() {
				@Override protected void warmupDeploying(String name) { events.add("deploying " + name); }
				@Override protected void warmupDeployed(String name) { events.add("deployed " + name); }
				@Override protected com.twinsoft.convertigo.engine.util.ProjectBundleDeployment.Completion install(HttpServletRequest request, String name, Path archive, boolean assembleXsl) throws Exception {
					events.add("install " + name);
					return () -> {
						events.add("load " + name);
						if (failure && name.equals("Process")) throw new IOException("simulated failure");
						return null;
					};
				}
			};
			// K8s uses Process: Process is imported first, while K8s still has its former version
			var request = new Multipart().file("file", "K8s.car", car("K8s", "Process"))
					.file("file", "Process.car", car("Process")).request("/admin/services/projects.DeployBundle");
			service.getServiceResult(request, document());
			assertEquals(failure
					? List.of("deploying Process", "deploying K8s", "install Process", "load Process", "deployed Process", "deployed K8s")
					: List.of("deploying Process", "deploying K8s", "install Process", "load Process", "install K8s", "load K8s",
							"deployed Process", "deployed K8s"), events);
		}
	}

	private class Deployment extends DeployBundle {
		final List<String> calls = new ArrayList<>();
		Path temporary;
		String fail;
		boolean assemble;
		Limits configured = Limits.defaults();
		@Override protected boolean projectAvailable(String name) { return false; }
		@Override protected Limits limits() { return configured; }
		@Override protected Path createTemporaryDirectory() throws IOException { return temporary = folder.newFolder().toPath(); }
		@Override protected com.twinsoft.convertigo.engine.util.ProjectBundleDeployment.Completion install(HttpServletRequest request, String name, Path archive, boolean assembleXsl) throws Exception {
			assertTrue(Files.isRegularFile(archive));
			return () -> {
			calls.add(name);
			assemble = assembleXsl;
			if (name.equals(fail)) throw new IOException("simulated write failure");
			return name.equals("A") ? "Migration save warning" : null;
			};
		}
	}

	private static byte[] car(String name) throws IOException { return car(name, null); }

	private static byte[] car(String name, String dependency) throws IOException {
		var bytes = new ByteArrayOutputStream();
		try (var zip = new ZipOutputStream(bytes)) {
			zip.putNextEntry(new ZipEntry(name + "/c8oProject.yaml"));
			zip.write(text("↑convertigo: 8.0.0.m006\n↓" + name + " [core.Project]: \n  version: 1\n"
					+ (dependency == null ? "" : "  ↓Reference [references.ProjectSchemaReference]: \n    projectName: " + dependency + "\n")));
			zip.closeEntry();
		}
		return bytes.toByteArray();
	}

	private static byte[] text(String value) { return value.getBytes(StandardCharsets.UTF_8); }

	private static Document document() throws Exception {
		var document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		document.appendChild(document.createElement("admin"));
		return document;
	}

	private static HttpServletRequest parameters(Map<String, String> values) {
		return (HttpServletRequest) Proxy.newProxyInstance(ProjectBundleServicesTest.class.getClassLoader(),
				new Class<?>[] { HttpServletRequest.class }, (proxy, method, args) -> method.getName().equals("getParameter") ? values.get(args[0]) : null);
	}
}
