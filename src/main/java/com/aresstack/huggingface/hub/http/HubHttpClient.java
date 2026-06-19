package com.aresstack.huggingface.hub.http;

import java.io.IOException;

public interface HubHttpClient {

    HubHttpResponse execute(HubHttpRequest request) throws IOException;
}
