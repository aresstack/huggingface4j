package com.aresstack.huggingface.hub.http;

import com.aresstack.huggingface.hub.download.ProgressListener;

import java.io.IOException;
import java.nio.file.Path;

public interface HubHttpClient {

    HubHttpResponse execute(HubHttpRequest request) throws IOException;

    long download(HubHttpRequest request, Path targetFile, ProgressListener progressListener) throws IOException;
}
