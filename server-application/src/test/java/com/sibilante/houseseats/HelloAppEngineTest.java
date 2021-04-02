package com.sibilante.houseseats;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.sibilante.houseseats.resource.CheckNewShowsServlet;

class HelloAppEngineTest {

	@Test
	void test() throws IOException {
		MockHttpServletResponse response = new MockHttpServletResponse();
		new CheckNewShowsServlet().doGet(null, response);
		assertEquals("text/plain", response.getContentType());
		assertEquals("UTF-8", response.getCharacterEncoding());
		assertEquals("Hello App Engine!\r\n", response.getWriterContent().toString());
	}
}
