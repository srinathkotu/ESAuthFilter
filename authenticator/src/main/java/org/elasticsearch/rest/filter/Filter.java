package org.elasticsearch.rest.filter;

import static org.elasticsearch.rest.RestRequest.Method.DELETE;
import static org.elasticsearch.rest.RestRequest.Method.GET;
import static org.elasticsearch.rest.RestRequest.Method.POST;
import static org.elasticsearch.rest.RestRequest.Method.PUT;

import java.io.FileInputStream;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.Properties;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.loader.JsonSettingsLoader;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentBuilderString;
import org.elasticsearch.rest.BytesRestResponse;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestFilter;
import org.elasticsearch.rest.RestFilterChain;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestStatus;

public class Filter extends RestFilter {
	static Properties authParams = new Properties();
	ESLogger logger = Loggers.getLogger(this.getClass());
	String errorMessage;
	static Settings settings = null;
	String username = null;
	public Filter(Settings handlerSettings) {
		settings = handlerSettings;
	}
	
	/*
	 * This method controls the actions and responses depending on type of method and authorization of user(non-Javadoc)
	 * @see org.elasticsearch.rest.RestFilter#process(org.elasticsearch.rest.RestRequest, org.elasticsearch.rest.RestChannel, org.elasticsearch.rest.RestFilterChain)
	 */
	@Override
	public void process(RestRequest request, RestChannel channel,RestFilterChain chain)  {
		logger.debug("start of Authentication");
		username="testuser1";
		errorMessage=null;
		try {
			authParams.load(new FileInputStream(settings.get("authParams.path")));	//loads properties from path specified in elasticsearch.yml file
		} catch (Exception e) {
			errorMessage = e.getLocalizedMessage();
			e.printStackTrace();
		}
		String[] pathArray = request.rawPath().split("/");
		String index=null;
		if(pathArray.length>=2 && !pathArray[1].contains(".") )
		{
			logger.debug("Validating Request");
			index= pathArray[1];
			boolean gettingSettings = false;
			if(request.param("gettingSettings")!=null)
				gettingSettings = Boolean.parseBoolean(request.param("gettingSettings"));
			javax.ws.rs.client.Client client = ClientBuilder.newClient();	//client is used to 
			if(errorMessage == null)
			{
				if(request.method().equals(PUT))	//validates ip for incoming PUT requests
				{
					InetAddress ip = ((InetSocketAddress) request.getRemoteAddress()).getAddress();
					if(!ip.isLoopbackAddress() && !ip.isSiteLocalAddress())
						errorMessage = authParams.getProperty("invalid.ip");
				}
				else if(request.method().equals(DELETE) || request.method().equals(POST))
					authorizeUserForPostDelete(username, client, channel);
				else if(request.method().equals(GET) && !gettingSettings)	//condition applied for skipping check while getting settings for index
					authorizeUserForGet(username, index, client, channel);
			}
		}
		if(errorMessage != null)
			sendResponse(channel, errorMessage, RestStatus.UNAUTHORIZED);
		else
		{
			logger.debug("Continuing Processing");
			chain.continueProcessing(request, channel);
		}
		logger.debug("end of Authentication");
	}

	/*
	 * The following method validates user for the authorization of GET request to index
	 */
	public boolean authorizeUserForGet(String username, String index, Client client, RestChannel channel)
	{
		logger.debug("authorizeUserForGet entered, username :"+username+", index :"+index);
		boolean result = false;
		try 
		{
			String resp = client.target("http://localhost:9200/"+index+"/_settings").queryParam("gettingSettings", "true").request("application/json").get(String.class);	//query elasticsearch for getting settings of requested index
			JsonSettingsLoader jsl = new JsonSettingsLoader();	
			String jsString =jsl.load(resp).get(index+".settings.index.ACI");	//convert response string to JSON and extracting javascript from response map

			if(jsString==null ||  jsString.isEmpty())
				errorMessage= authParams.getProperty("index.setting.notdefined");	
			else
			{
				List<String> groupnames =new Filter(settings).queryRest(username,client);	//query external rest api for getting accessible group names for the user
				if(groupnames==null || groupnames.size()==0)
					errorMessage= authParams.getProperty("groupnames.username.notdefined");
				else
				{
					ListIterator<String> iterator = groupnames.listIterator();
					while (iterator.hasNext())
						iterator.set(iterator.next().toLowerCase());
					logger.debug("accessible group names for "+username+": "+groupnames);

					//javascript construction and evaluation
					ScriptEngine engine = new ScriptEngineManager().getEngineByName("javascript");
					ScriptContext context = engine.getContext();
					context.setAttribute("groupnames", groupnames, ScriptContext.ENGINE_SCOPE);
					context.setAttribute("kibanaIndex", index.toLowerCase(), ScriptContext.ENGINE_SCOPE);
					StringWriter writer = new StringWriter();
					context.setWriter(writer);
					engine.eval(jsString);
					if(!Boolean.parseBoolean(writer.toString().trim()))
						errorMessage = authParams.getProperty("index.access.forbidden.get"); //error message if user doen'st have access right for GET
				}
			}
		}
		catch (NotFoundException nfe){
			errorMessage= authParams.getProperty("index.not.found");	
			logger.error("Index-"+index+" not found when trying to get settings");
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		logger.debug("authorizeUserForIndex exited, result:"+result+", errormessage:"+errorMessage);
		return result;
	}

	/*
	 * The following method validates user for the authorization of GET request to index
	 */
	public boolean authorizeUserForPostDelete(String username, Client client, RestChannel channel)
	{
		logger.debug("authorizeUserForIndex entered, username :"+username);
		boolean result = false;
		try 
		{
			List<String> groupNames = queryRest(username,client);	//query external rest api for getting accessible group names for the user
			if(groupNames==null || groupNames.size()==0)
				errorMessage= authParams.getProperty("groupnames.username.notdefined");
			else
			{
				ListIterator<String> iterator = groupNames.listIterator();
				while (iterator.hasNext())
					iterator.set(iterator.next().toLowerCase());
				logger.debug("accessible group names for "+username+": "+groupNames);
				List<String> adminList = new ArrayList<String>(Arrays.asList(authParams.get("admin.list").toString().split(",")));
				iterator = adminList.listIterator();
				while (iterator.hasNext())
					iterator.set(iterator.next().toLowerCase());
				groupNames.retainAll(adminList);
				if(groupNames.size()==0)
					errorMessage=authParams.getProperty("user.not.admin");	//error message if user doesn't have access rights for DELETE or POST, i.e., user is not ELK Admin
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		logger.debug("authorizeUserForIndex exited, result:"+result+", errormessage:"+errorMessage);
		return result;
	}

	/*
	 * This method queries the external api to retrieve the list of indices that can be accessed by the user
	 */
	public List<String> queryRest(String solution,Client client)
	{
		logger.debug("entered queryRest, solution:"+solution);
		List<String> gnames=null;
		try 
		{
			String resp = client.target("http://localhost:2403/groups?username="+solution).request("application/json").get(String.class);
			JsonSettingsLoader jsl = new JsonSettingsLoader();
			resp = resp.substring(1, resp.length()-1);
			gnames = new ArrayList<String>(Arrays.asList(jsl.load(resp).get("gname").split(",")));	//convert response string to JSON and tokenize group names to list
		} 
		catch (Exception e) {
			logger.debug("Exception while authenticating user");
		}
		logger.debug("entered queryRest, gnames:"+gnames);
		return gnames;
	}

	/*
	 * This method sends respective error response to the request 
	 */
	public void sendResponse(RestChannel channel,String errorMessage, RestStatus status){
		try{
			XContentBuilder builder = channel.newBuilder();
			builder
			.startObject()
			.field(new XContentBuilderString("Error"), errorMessage)
			.endObject();
			channel.sendResponse(new BytesRestResponse(status,builder));
		}
		catch (Exception e) {
			logger.error("Failed to send failure response", e); 
		}
	}
}