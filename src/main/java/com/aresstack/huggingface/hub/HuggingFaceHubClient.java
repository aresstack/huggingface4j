package com.aresstack.huggingface.hub;

import com.aresstack.huggingface.hub.account.UserProfile;
import com.aresstack.huggingface.hub.model.HubFile;
import com.aresstack.huggingface.hub.model.ModelDetails;
import com.aresstack.huggingface.hub.model.ModelDetailsQuery;
import com.aresstack.huggingface.hub.model.ModelSearchResult;
import com.aresstack.huggingface.hub.query.HubQueryParameters;

import java.util.List;

public interface HuggingFaceHubClient {

    UserProfile whoAmI() throws HuggingFaceHubException;

    ModelSearchResult searchModels(HubQueryParameters parameters) throws HuggingFaceHubException;

    ModelDetails getModelDetails(ModelDetailsQuery query) throws HuggingFaceHubException;

    List<HubFile> listModelFiles(ModelDetailsQuery query) throws HuggingFaceHubException;

    com.aresstack.huggingface.hub.download.DownloadResult downloadFile(com.aresstack.huggingface.hub.download.DownloadRequest request) throws HuggingFaceHubException;
}