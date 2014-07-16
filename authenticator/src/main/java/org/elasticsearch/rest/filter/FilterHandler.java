package org.elasticsearch.rest.filter;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;

public class FilterHandler extends BaseRestHandler {

	/*
	 * Registers filters and handlers for Http requests
	 */
	@Inject public FilterHandler(Settings settings, Client client, RestController controller) {
		super(settings, client);
		controller.registerFilter(new Filter(settings));
	}

	/*
	 * Used to explicitly handle http requests(GET, PUT, DELETE etc.)(non-Javadoc)
	 * @see org.elasticsearch.rest.RestHandler#handleRequest(org.elasticsearch.rest.RestRequest, org.elasticsearch.rest.RestChannel)
	 */
	public void handleRequest(final RestRequest request, final RestChannel channel) {
	}
}
