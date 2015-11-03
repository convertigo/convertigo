package com.twinsoft.convertigo.engine.util;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimePart;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;


public class BigMimeMultipart {
	static final private Pattern pHeader = Pattern.compile("(.*?): (.*)");
	static final private Pattern pBoundary = Pattern.compile("boundary=\"(.*?)\"");
	static final private byte[] sLine = {'\r', '\n'};
	
	private PartInputStream pis;
	private String preamble;
	
	public BigMimeMultipart (InputStream is, String contentType) throws IOException {
		Matcher mBoundary = pBoundary.matcher(contentType);
		
		if (mBoundary.find()) {
			byte[] boundary = ("--" + mBoundary.group(1)).getBytes("ascii");
			pis = new PartInputStream(is, boundary);
			
			preamble = IOUtils.toString(pis, "ascii");
			pis.nextPart(ArrayUtils.addAll(sLine, boundary));
		} else {
			throw new IOException("(BigMimeMultipart) boundary not found in the contentType: " + contentType);
		}
	}
	
	public String getPreamble() {
		return preamble;
	}
	
	public MimePart nextPart(OutputStream os) throws IOException, MessagingException {			
		MimeBodyPart bodyPart = readHeaders();
		
		if (bodyPart != null) {
			IOUtils.copy(pis, os);
			
			pis.nextPart();	
		}
		
		return bodyPart;
	}
	
	public MimePart nextPart(File file) throws IOException, MessagingException {			
		MimeBodyPart bodyPart = readHeaders();
		
		if (bodyPart != null) {
			FileUtils.copyInputStreamToFile(pis, file);
			
			pis.nextPart();	
		}
		
		return bodyPart;
	}
	
	private MimeBodyPart readHeaders() throws IOException, MessagingException {
		if (pis.isStreamEnd()) {
			return null;
		}
		
		byte[] sep = new byte[2];
		int read = pis.read(sep);
		
		if (read != 2 || !Arrays.equals(sep, sLine))
		{
			return null;
		}
		
		MimeBodyPart bodyPart = new MimeBodyPart();		
		PartInputStream lis = new PartInputStream(pis, sLine);
		
		for (String line = IOUtils.toString(lis, "ascii"); !line.isEmpty(); line = IOUtils.toString(lis, "ascii")) {
			Matcher mHeader = pHeader.matcher(line);
			
			if (mHeader.matches()) {
				bodyPart.addHeader(mHeader.group(1), mHeader.group(2));
				
				if (mHeader.group(1).equalsIgnoreCase("Content-ID")) {
					bodyPart.setContentID(mHeader.group(2));
				}
			}
			
			lis.nextPart();
		}
		
		return bodyPart;
	}
}