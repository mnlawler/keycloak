package org.keycloak.protocol.oidc4vp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.OIDCWellKnownProvider;
import org.keycloak.protocol.oidc4vp.model.FormatObject;
import org.keycloak.protocol.oidc4vp.model.SupportedCredential;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.keycloak.protocol.oidc4vp.OIDC4VPClientRegistrationProvider.VC_TYPES_PREFIX;
import static org.keycloak.protocol.oidc4vp.OIDC4VPIssuerEndpoint.GRANT_TYPE_PRE_AUTHORIZED_CODE;
import static org.keycloak.protocol.oidc4vp.model.Format.LDP_VC;

public class OIDC4VPWellKnownProvider extends OIDC4VPAbstractWellKnownProvider {

	public OIDC4VPWellKnownProvider(KeycloakSession keycloakSession, ObjectMapper objectMapper) {
		super(keycloakSession, objectMapper);
	}

	@Override public void close() {
		// no-op
	}

	@Override public Object getConfig() {
		// some wallets use the openid-config well-known to also gather the issuer metadata. In
		// the future(when everyone uses .well-known/openid-credential-issuer), that can be removed.
		Map<String, Object> configAsMap = objectMapper.convertValue(
				new OIDCWellKnownProvider(keycloakSession, null, false).getConfig(),
				Map.class);

		List<String> supportedGrantTypes = Optional.ofNullable(configAsMap.get("grant_types_supported"))
				.map(grantTypesObject -> objectMapper.convertValue(
						grantTypesObject, new TypeReference<List<String>>() {
						})).orElse(new ArrayList<>());
		// newly invented by OIDC4VCI and supported by this implementation
		supportedGrantTypes.add(GRANT_TYPE_PRE_AUTHORIZED_CODE);
		configAsMap.put("grant_types_supported", supportedGrantTypes);
		configAsMap.put("credential_endpoint", getCredentialsEndpoint(keycloakSession.getContext()));

		FormatObject ldpVC = new FormatObject(new ArrayList<>());
		FormatObject jwtVC = new FormatObject(new ArrayList<>());

		getCredentialsFromModels(
				keycloakSession.getContext().getRealm().getClientsStream().toList())
				.forEach(supportedCredential -> {
					if (supportedCredential.getFormat() == LDP_VC) {
						ldpVC.getTypes().addAll(supportedCredential.getTypes());
					} else {
						jwtVC.getTypes().addAll(supportedCredential.getTypes());
					}
				});

		return configAsMap;
	}

	// filter the client models for supported verifable credentials
	private List<SupportedCredential> getCredentialsFromModels(List<ClientModel> clientModels) {
		return List.copyOf(clientModels.stream()
				.map(ClientModel::getAttributes)
				.filter(Objects::nonNull)
				.flatMap(attrs -> attrs.entrySet().stream())
				.filter(attr -> attr.getKey().startsWith(VC_TYPES_PREFIX))
				.flatMap(entry -> mapAttributeEntryToSc(entry).stream())
				.collect(Collectors.toSet()));
	}

}
