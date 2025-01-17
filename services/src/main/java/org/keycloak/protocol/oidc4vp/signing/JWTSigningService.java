package org.keycloak.protocol.oidc4vp.signing;

import com.danubetech.verifiablecredentials.VerifiableCredential;
import com.danubetech.verifiablecredentials.jwt.JwtVerifiableCredential;
import com.danubetech.verifiablecredentials.jwt.ToJwtConverter;
import com.nimbusds.jose.JOSEException;
import org.bitcoinj.core.ECKey;

import java.util.Optional;

public class JWTSigningService extends SigningService<String> {

	private final AlgorithmType algorithmType;

	public JWTSigningService(String keyPath, Optional<String> optionalKeyId){
		super(keyPath, optionalKeyId);
		algorithmType = getAlgorithmType();
	}

	private AlgorithmType getAlgorithmType() {
		 return AlgorithmType.getByValue(signingKey.getPrivate().getAlgorithm());
	}

	@Override
	public String signCredential(VerifiableCredential verifiableCredential) {
		JwtVerifiableCredential jwtVerifiableCredential = ToJwtConverter.toJwtVerifiableCredential(
				verifiableCredential);
		try {

			return switch (algorithmType) {
				case RSA -> {
					String concreteAlgorithm = signingKey.getPrivate().getAlgorithm();
					if (concreteAlgorithm.equalsIgnoreCase("ps256")) {
						yield jwtVerifiableCredential.sign_RSA_PS256(signingKey);
					} else {
						yield jwtVerifiableCredential.sign_RSA_RS256(signingKey);
					}
				}
				case ECDSA_SECP256K1 -> jwtVerifiableCredential.sign_secp256k1_ES256K(
						ECKey.fromPrivate(signingKey.getPrivate().getEncoded()));
				case ED_DSA_ED25519 -> jwtVerifiableCredential.sign_Ed25519_EdDSA(signingKey.getPrivate().getEncoded());

			};
		} catch (JOSEException e) {
			throw new SigningServiceException("Was not able to sign the credential.", e);
		}
	}
}