package com.aresstack.huggingface.hub;

import com.aresstack.huggingface.hub.account.UserProfile;
import com.aresstack.huggingface.hub.model.HubFile;
import com.aresstack.huggingface.hub.model.ModelDetails;
import com.aresstack.huggingface.hub.model.ModelDetailsQuery;
import com.aresstack.huggingface.hub.model.ModelSearchResult;
import com.aresstack.huggingface.hub.query.HubQueryParameters;
import com.aresstack.huggingface.hub.repo.CreateRepositoryRequest;
import com.aresstack.huggingface.hub.repo.DeleteRepositoryRequest;
import com.aresstack.huggingface.hub.repo.RepositoryInfo;
import com.aresstack.huggingface.hub.repo.UpdateRepositorySettingsRequest;
import com.aresstack.huggingface.hub.upload.CommitRequest;
import com.aresstack.huggingface.hub.upload.CommitResult;

import java.util.List;

/**
 * The low-level transport-agnostic contract behind {@link HuggingFaceHub}.
 *
 * <p>Most users go through the fluent {@code HuggingFaceHub.models()/account()/repositories()} API
 * rather than calling this directly. Implement it (and supply it via
 * {@code HuggingFaceHub.standard().client(...)}) to fully customise transport or for testing. The
 * default implementation lives in the internal package.</p>
 */
public interface HuggingFaceHubClient {

    UserProfile whoAmI() throws HuggingFaceHubException;

    ModelSearchResult searchModels(HubQueryParameters parameters) throws HuggingFaceHubException;

    ModelDetails getModelDetails(ModelDetailsQuery query) throws HuggingFaceHubException;

    List<HubFile> listModelFiles(ModelDetailsQuery query) throws HuggingFaceHubException;

    com.aresstack.huggingface.hub.download.DownloadResult downloadFile(com.aresstack.huggingface.hub.download.DownloadRequest request) throws HuggingFaceHubException;

    RepositoryInfo createRepository(CreateRepositoryRequest request) throws HuggingFaceHubException;

    void deleteRepository(DeleteRepositoryRequest request) throws HuggingFaceHubException;

    void updateRepositorySettings(UpdateRepositorySettingsRequest request) throws HuggingFaceHubException;

    CommitResult commit(CommitRequest request) throws HuggingFaceHubException;
}