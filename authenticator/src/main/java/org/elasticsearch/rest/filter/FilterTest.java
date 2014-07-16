package org.elasticsearch.rest.filter;

import java.util.Map;
import java.util.Map.Entry;

import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestFilterChain;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestRequest.Method;
import org.elasticsearch.rest.RestResponse;
import org.testng.Assert;
import org.testng.annotations.*;

public class FilterTest{

	Filter filter;
	RestRequest restRequest;
	RestChannel restChannel;
	RestFilterChain restFilterChain;
	String restResponseString = null;
	String output = null;
	String expectedOutput = null;
	Method method = null;
	String rawPath = null;
	String header="";
	String gettingSettings = null;

	@BeforeMethod
	public void setUp() throws Exception {
		restRequest = new RestRequest() {
			public String param(String key, String defaultValue) {
				return null;
			}
			public String uri() {
				return null;
			}
			public String rawPath() {
				return rawPath;
			}
			public Map<String, String> params() {
				return null;
			}
			public String param(String key) {
				return gettingSettings;
			}
			public Method method() {
				return method;
			}
			public Iterable<Entry<String, String>> headers() {
				return null;
			}
			public String header(String name) {
				return header;
			}
			public boolean hasParam(String key) {
				return false;
			}
			public boolean hasContent() {
				return false;
			}
			public boolean contentUnsafe() {
				return false;
			}
			public BytesReference content() {
				return null;
			}
		};

		Settings settings = ImmutableSettings.builder().put("authParams.path", "C:/Users/imisxk0/Downloads/elasticsearch-1.2.1/elasticsearch-1.2.1/config/authParams.props").build();

		restChannel = new RestChannel(restRequest) {
			public void sendResponse(RestResponse response) {
				restResponseString=response.content().toUtf8();
			}
		};
		restFilterChain = new RestFilterChain() {
			public void continueProcessing(RestRequest request, RestChannel channel) {
			}
		};
		filter = new Filter(settings);

	}


	@Test()
	public void testFilterProcessGettingSettings(){
		method=Method.GET;
		rawPath="/coke";
		filter.username="testuser1";
		gettingSettings="true";
		filter.process(restRequest, restChannel, restFilterChain);
		expectedOutput="{\r\n  \"error\" : \"success\"\r\n}\n";
		output=restResponseString;
		Assert.assertEquals(output,expectedOutput);
	}

	@Test()
	public void testFilterProcessGettingSettingsNotDefined(){
		method=Method.GET;
		rawPath="/coke";
		filter.username="testuser1";
		filter.process(restRequest, restChannel, restFilterChain);
		expectedOutput="{\"error\":\"success\"}";
		output=restResponseString;
		Assert.assertEquals(output,expectedOutput);
	}

	@Test()
	public void testFilterProcessGettingSettingsForNotGet(){
		method=Method.HEAD;
		rawPath="/coke";
		filter.username="testuser1";
		gettingSettings="true";
		filter.process(restRequest, restChannel, restFilterChain);
		expectedOutput="{\r\n  \"error\" : \"success\"\r\n}\n";
		output=restResponseString;
		Assert.assertEquals(output,expectedOutput);
	}

	@Test()
	public void testFilterProcessIndexNotFound(){
		method=Method.GET;
		rawPath="/co";
		filter.username="testuser1";
		filter.process(restRequest, restChannel, restFilterChain);
		output=restResponseString;
		expectedOutput="{\"error\":\"Index not found\"}";
		Assert.assertEquals(output,expectedOutput);
	}

	@Test()
	public void testFilterProcessIndexSettingNotDefined(){
		method=Method.GET;
		rawPath="/test";
		filter.username="testuser";
		filter.process(restRequest, restChannel, restFilterChain);
		output=restResponseString;
		expectedOutput="{\"error\":\"ACI index setting not defined\"}";
		Assert.assertEquals(output,expectedOutput);
	}

	@Test()
	public void testFilterProcessUserOrGroupnamesNotDefinedGet(){
		method=Method.GET;
		rawPath="/coke";
		filter.username="testuser";
		filter.process(restRequest, restChannel, restFilterChain);
		output=restResponseString;
		expectedOutput="{\"error\":\"Groupnames not defined for user\"}";
		Assert.assertEquals(output,expectedOutput);
	}

	/*@Test()
	public void testFilterProcessUserOrGroupnamesNotDefinedDelete(){
		method=Method.DELETE;
		rawPath="/coke";
		filter.username="testuser";
		filter.process(restRequest, restChannel, restFilterChain);
		output=restResponseString;
		expectedOutput="{\"error\":\"Groupnames not defined for user\"}";
		Assert.assertEquals(output,expectedOutput);
	}
	 */
	@Test()
	public void testFilterProcessUserOrGroupnamesNotDefinedPost(){
		method=Method.POST;
		rawPath="/coke";
		filter.username="testuser";
		filter.process(restRequest, restChannel, restFilterChain);
		output=restResponseString;
		expectedOutput="{\"error\":\"Groupnames not defined for user\"}";
		Assert.assertEquals(output,expectedOutput);
	}

	@Test()
	public void testFilterProcessUserOrGroupnamesEmptyPost(){
		method=Method.POST;
		rawPath="/coke";
		filter.username="testuser3";
		filter.process(restRequest, restChannel, restFilterChain);
		output=restResponseString;
		expectedOutput="{\"error\":\"Groupnames not defined for user\"}";
		Assert.assertEquals(output,expectedOutput);
	}

	@Test()
	public void testFilterProcessPostFail(){
		method=Method.POST;
		rawPath="/aa";
		filter.username="testuser1";
		filter.process(restRequest, restChannel, restFilterChain);
		output=restResponseString;
		expectedOutput="{\"error\":\"User is not an ELK Admin\"}";
		Assert.assertEquals(output,expectedOutput);
	}

	@Test()
	public void testFilterProcessPostSuccess(){
		method=Method.POST;
		rawPath="/aa";
		filter.username="testuser2";
		filter.process(restRequest, restChannel, restFilterChain);
		output=restResponseString;
		expectedOutput="{\"error\":\"success\"}";
		Assert.assertEquals(output,expectedOutput);
	}

	@Test()
	public void testFilterProcessDeleteFail(){
		method=Method.DELETE;
		rawPath="/aa";
		filter.username="testuser1";
		filter.process(restRequest, restChannel, restFilterChain);
		output=restResponseString;
		expectedOutput="{\"error\":\"User is not an ELK Admin\"}";
		Assert.assertEquals(output,expectedOutput);
	}

	@Test()
	public void testFilterProcessDeleteSuccess(){
		method=Method.DELETE;
		rawPath="/aa";
		filter.username="testuser2";
		filter.process(restRequest, restChannel, restFilterChain);
		output=restResponseString;
		expectedOutput="{\"error\":\"success\"}";
		Assert.assertEquals(output,expectedOutput);
	}

	@Test()
	public void testFilterProcessGetFail(){
		method=Method.GET;
		rawPath="/coke";
		filter.username="testuser2";
		filter.process(restRequest, restChannel, restFilterChain);
		output=restResponseString;
		expectedOutput="{\"error\":\"Cannot access index\"}";
		Assert.assertEquals(output,expectedOutput);
	}

	@Test()
	public void testFilterProcessGetSuccess(){
		method=Method.GET;
		rawPath="/coke";
		filter.username="testuser1";
		filter.process(restRequest, restChannel, restFilterChain);
		output=restResponseString;
		expectedOutput="{\"error\":\"success\"}";
		Assert.assertEquals(output,expectedOutput);
	}

	@Test()
	public void testFilterProcessRawPathReject1(){
		method=Method.GET;
		rawPath="/fav.ico";
		filter.process(restRequest, restChannel, restFilterChain);
		output=restResponseString;
		expectedOutput="{\"error\":\"success\"}";
		Assert.assertEquals(output,expectedOutput);
	}

	@Test()
	public void testFilterProcessRawPathReject2(){
		method=Method.GET;
		rawPath="fav.ico";
		filter.process(restRequest, restChannel, restFilterChain);
		output=restResponseString;
		expectedOutput="{\"error\":\"success\"}";
		Assert.assertEquals(output,expectedOutput);
	}

	@Test(expectedExceptions = NullPointerException.class)
	public void testFilterProcessPutSuccess(){
		method=Method.PUT;
		rawPath="/coke";
		filter.username="testuser1";
		filter.process(restRequest, restChannel, restFilterChain);
		output=restResponseString;
		expectedOutput="{\"error\":\"success\"}";
		Assert.assertEquals(output,expectedOutput);
	}

	@AfterMethod
	public void reintialize(){
		method = null;
		output = null;
		expectedOutput = null;
		rawPath = null;
		gettingSettings = null;
	}
}
