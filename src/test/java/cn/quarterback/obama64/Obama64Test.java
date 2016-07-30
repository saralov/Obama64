package cn.quarterback.obama64;

import static org.junit.Assert.*;

import java.io.UnsupportedEncodingException;

import org.junit.Before;
import org.junit.Test;

import cn.quarterback.obama64.Obama64;

public class Obama64Test {
	
	Obama64 obama64 = new Obama64();
	String content;
	String contentAscii;

	@Before
	public void setUp() throws Exception {
		content = "@Crusader_815，使用Obama64进行编码。";
		contentAscii = "@Crusader_815, encode with Obama64.";
	}

	@Test
	public void testEncode() throws UnsupportedEncodingException {
		String encodeContent = new String(obama64.encode(content.getBytes("GBK")), "GBK");
		String decodeContent = new String(obama64.decode(encodeContent.getBytes("GBK")), "GBK");
		assertEquals(content, decodeContent);
	}

	@Test
	public void testEncodeAscii() throws UnsupportedEncodingException {
		String encodeAscii = new String(obama64.encode(contentAscii.getBytes("GBK")), "GBK");
		String decodeAscii = new String(obama64.decode(encodeAscii.getBytes("GBK")), "GBK");
		assertEquals(contentAscii, decodeAscii);
	}

}
