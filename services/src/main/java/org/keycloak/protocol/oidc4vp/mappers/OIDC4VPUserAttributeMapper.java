package org.keycloak.protocol.oidc4vp.mappers;

import com.danubetech.verifiablecredentials.VerifiableCredential;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc4vp.OIDC4VPClientRegistrationProviderFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Factory implementation to provide the VCIssuer functionality as a realm resource.
 */
public class OIDC4VPUserAttributeMapper extends OIDC4VPMapper {

	public static final String MAPPER_ID = "oidc4vp-user-attribute-mapper";
	public static final String SUBJECT_PROPERTY_CONFIG_KEY = "subjectProperty";
	public static final String USER_ATTRIBUTE_KEY = "userAttribute";
	public static final String AGGREGATE_ATTRIBUTES_KEY = "aggregateAttributes";

	private static final List<ProviderConfigProperty> CONFIG_PROPERTIES = new ArrayList<>();

	public OIDC4VPUserAttributeMapper() {
		super();
		ProviderConfigProperty subjectPropertyNameConfig = new ProviderConfigProperty();
		subjectPropertyNameConfig.setName(SUBJECT_PROPERTY_CONFIG_KEY);
		subjectPropertyNameConfig.setLabel("Attribute Property Name");
		subjectPropertyNameConfig.setHelpText("Property to add the user attribute to in the credential subject.");
		subjectPropertyNameConfig.setType(ProviderConfigProperty.STRING_TYPE);
		CONFIG_PROPERTIES.add(subjectPropertyNameConfig);

		ProviderConfigProperty userAttributeConfig = new ProviderConfigProperty();
		userAttributeConfig.setName(USER_ATTRIBUTE_KEY);
		userAttributeConfig.setLabel("User attribute");
		userAttributeConfig.setHelpText("The user attribute to be added to the credential subject.");
		userAttributeConfig.setType(ProviderConfigProperty.LIST_TYPE);
		userAttributeConfig.setOptions(
				List.of(UserModel.USERNAME, UserModel.LOCALE, UserModel.FIRST_NAME, UserModel.LAST_NAME,
						UserModel.DISABLED_REASON, UserModel.EMAIL, UserModel.EMAIL_VERIFIED));
		CONFIG_PROPERTIES.add(userAttributeConfig);

		ProviderConfigProperty aggregateAttributesConfig = new ProviderConfigProperty();
		aggregateAttributesConfig.setName(AGGREGATE_ATTRIBUTES_KEY);
		aggregateAttributesConfig.setLabel("Aggregate attributes");
		aggregateAttributesConfig.setHelpText("Should the mapper aggregate user attributes.");
		aggregateAttributesConfig.setType(ProviderConfigProperty.BOOLEAN_TYPE);
		CONFIG_PROPERTIES.add(aggregateAttributesConfig);
	}

	@Override protected List<ProviderConfigProperty> getIndividualConfigProperties() {
		return CONFIG_PROPERTIES;
	}

	@Override public void setClaimsForCredential(VerifiableCredential.Builder credentialBuilder,
			UserSessionModel userSessionModel) {
		// nothing to do for the mapper.
	}

	@Override public void setClaimsForSubject(Map<String, Object> claims, UserSessionModel userSessionModel) {
		String propertyName = mapperModel.getConfig().get(SUBJECT_PROPERTY_CONFIG_KEY);
		String userAttribute = mapperModel.getConfig().get(USER_ATTRIBUTE_KEY);
		boolean aggregateAttributes = Optional.ofNullable(mapperModel.getConfig().get(AGGREGATE_ATTRIBUTES_KEY))
				.map(Boolean::parseBoolean).orElse(false);
		Collection<String> attributes =
				KeycloakModelUtils.resolveAttribute(userSessionModel.getUser(), userAttribute,
						aggregateAttributes);
		attributes.removeAll(Collections.singleton(null));
		if (!attributes.isEmpty()) {
			claims.put(propertyName, String.join(",", attributes));
		}
	}

	public static ProtocolMapperModel create(String mapperName, String userAttribute, String propertyName,
			boolean aggregateAttributes) {
		var mapperModel = new ProtocolMapperModel();
		mapperModel.setName(mapperName);
		Map<String, String> configMap = new HashMap<>();
		configMap.put(SUBJECT_PROPERTY_CONFIG_KEY, propertyName);
		configMap.put(USER_ATTRIBUTE_KEY, userAttribute);
		configMap.put(AGGREGATE_ATTRIBUTES_KEY, Boolean.toString(aggregateAttributes));
		mapperModel.setConfig(configMap);
		mapperModel.setProtocol(OIDC4VPClientRegistrationProviderFactory.PROTOCOL_ID);
		mapperModel.setProtocolMapper(MAPPER_ID);
		return mapperModel;
	}

	@Override public String getDisplayType() {
		return "User Attribute Mapper";
	}

	@Override public String getHelpText() {
		return "Maps user attributes to credential subject properties.";
	}

	@Override public String getId() {
		return MAPPER_ID;
	}
}
