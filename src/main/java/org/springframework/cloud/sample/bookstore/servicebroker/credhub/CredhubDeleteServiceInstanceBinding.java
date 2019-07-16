package org.springframework.cloud.sample.bookstore.servicebroker.credhub;

import org.springframework.cloud.servicebroker.model.binding.DeleteServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.binding.DeleteServiceInstanceBindingResponse;
import org.springframework.credhub.core.CredHubOperations;
import org.springframework.credhub.support.CredentialName;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

public class CredhubDeleteServiceInstanceBinding extends CredHubPersistingWorkflow {

	private static final Logger LOG = Loggers.getLogger(CredhubDeleteServiceInstanceBinding.class);

	private final CredHubOperations credHubOperations;

	public CredhubDeleteServiceInstanceBinding(CredHubOperations credHubOperations, String appName) {
		super(appName);
		this.credHubOperations = credHubOperations;
	}

	public Mono<DeleteServiceInstanceBindingResponse.DeleteServiceInstanceBindingResponseBuilder> buildResponse(DeleteServiceInstanceBindingRequest
																													request,
																												DeleteServiceInstanceBindingResponse.DeleteServiceInstanceBindingResponseBuilder responseBuilder) {
		return buildCredentialName(request.getServiceDefinitionId(), request.getBindingId())
			.filter(this::credentialExists)
			.flatMap(credentialName -> deleteBindingCredentials(credentialName)
				.doOnRequest(l -> LOG.debug("Deleting binding credentials with name '{}' in CredHub", credentialName.getName()))
				.doOnSuccess(r -> LOG.debug("Finished deleting binding credentials with name '{}' in CredHub", credentialName.getName()))
				.doOnError(exception -> LOG.error("Error deleting binding credentials with name '{}' in CredHub with error: {}",
					credentialName.getName(), exception.getMessage())))
			.thenReturn(responseBuilder);
	}

	private boolean credentialExists(CredentialName credentialName) {
		return !credHubOperations.credentials().findByName(credentialName).isEmpty();
	}

	private Mono<Void> deleteBindingCredentials(CredentialName credentialName) {
		return Mono.fromCallable(() -> {
			credHubOperations.credentials().deleteByName(credentialName);
			return null;
		});
	}

}
