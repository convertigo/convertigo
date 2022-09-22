package com.convertigo.icons;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;

public class MakeIcons {
	Path rootPath;
	SortedSet<String> toIgnore = Collections.synchronizedSortedSet(new TreeSet<>());

	public static void main(String[] args) throws Exception {
		if (args.length == 0) {
			System.err.println("missing url arguments");
			System.exit(1);
		}
		for (String path: args) {
			new MakeIcons(Paths.get(path)).run();
		}
	}
	
	public MakeIcons(Path rootPath) {
		this.rootPath = rootPath;
	}

	void run() throws Exception {
		Files.walk(rootPath)
		.filter(Files::isRegularFile)
		.filter(p -> p.toString().endsWith(".svg"))
		.parallel()
		.forEach(this::convert);
		
		try (PrintWriter writer = new PrintWriter(rootPath.resolve(".gitignore").toFile(), StandardCharsets.UTF_8)) {
			toIgnore.forEach(writer::println);
		}
		System.out.println("Successfully convert " + toIgnore.size() + " svg icons from " + rootPath);
	}

	void convert(Path path) {
		TranscoderInput input = new TranscoderInput(path.toUri().toString());
		convert(path, input, 16);
		convert(path, input, 32);
	}

	void convert(Path path, TranscoderInput input, int size) {
		Path outpath = path.resolveSibling(path.getFileName().toString().replace(".svg", "_" + size + "x" + size + ".png"));
		try (OutputStream os = new FileOutputStream(outpath.toFile())) {
			TranscoderOutput output = new TranscoderOutput(os);
			PNGTranscoder transcoder = new PNGTranscoder();
			transcoder.addTranscodingHint(PNGTranscoder.KEY_HEIGHT, (float) size);
			transcoder.addTranscodingHint(PNGTranscoder.KEY_WIDTH, (float) size);
			transcoder.transcode(input, output);
			toIgnore.add(rootPath.relativize(outpath).toString().replace('\\', '/'));
		} catch (Exception e) {
			System.err.println("transcode failed: " + e.getMessage());
		}
	}
}
