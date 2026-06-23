#![allow(unused)]

use crate::json_value::DeterministicMap;
use nanoserde::DeJson;

#[derive(DeJson, Debug, Default)]
#[nserde(default)]
pub struct DigitalCredentialCreationRequest {
    pub requests: Vec<OpenId4VciRequest>,
}

#[derive(DeJson, Debug, Default)]
#[nserde(default)]
pub struct OpenId4VciRequest {
    pub protocol: String,
    pub data: OpenId4VciRequestData,
}

#[derive(DeJson, Debug, Default)]
#[nserde(default)]
pub struct OpenId4VciRequestData {
    pub credential_issuer: String,
    pub credential_configuration_ids: Vec<String>,
    pub grants: DeterministicMap<String, credential_offer::Grant>,
    pub credential_issuer_metadata: Option<credential_issuer_metadata::CredentialIssuerMetadata>,
}

/**
 * This correlates the `credential_configuration_ids` in the offer and `credential_configurations_supported` in the issuer metadata.
 */
pub struct RegularizedOpenId4VciRequestData<'a> {
    pub credential_issuer: &'a str,
    pub credential_configuration_ids: &'a [String],
    pub grants: &'a DeterministicMap<String, credential_offer::Grant>,
    // Borrow the metadata
    pub credential_issuer_metadata:
        Option<&'a credential_issuer_metadata::CredentialIssuerMetadata>,
    pub credential_configurations: Vec<&'a credential_issuer_metadata::CredentialConfiguration>,
}

// Implement From on a Reference
impl<'a> From<&'a OpenId4VciRequestData> for RegularizedOpenId4VciRequestData<'a> {
    fn from(value: &'a OpenId4VciRequestData) -> Self {
        log::trace!(
            "Regularizing request for issuer: {}",
            value.credential_issuer
        );
        let mut configurations = Vec::with_capacity(value.credential_configuration_ids.len());

        if let Some(metadata) = &value.credential_issuer_metadata {
            for id in &value.credential_configuration_ids {
                if let Some(config) = metadata.credential_configurations_supported.get(id) {
                    configurations.push(config);
                } else {
                    log::trace!("Configuration ID '{}' not found in metadata", id);
                }
            }
        } else {
            log::trace!("No metadata available for regularization");
        }

        log::trace!(
            "Regularization complete: {} configurations matched",
            configurations.len()
        );
        Self {
            credential_issuer: &value.credential_issuer,
            credential_configuration_ids: &value.credential_configuration_ids,
            grants: &value.grants,
            credential_issuer_metadata: value.credential_issuer_metadata.as_ref(),
            credential_configurations: configurations,
        }
    }
}

pub mod credential_offer {
    use nanoserde::DeJson;

    #[derive(DeJson, Debug, Default)]
    #[nserde(default)]
    pub struct Grant {
        // Workaround for nanoserde bug:
        // https://github.com/not-fl3/nanoserde/issues/157
        #[allow(unused)]
        pub _dummy_not_in_use: i32,
    }
}

mod credential_issuer_metadata {
    use crate::json_value::{DeterministicMap, DeterministicSet};
    use nanoserde::DeJson;

    #[derive(DeJson, Debug, Default)]
    #[nserde(default)]
    pub struct CredentialIssuerMetadata {
        // pub credential_issuer: String,
        // pub authorization_servers: Option<Vec<String>>,
        // pub credential_endpoint: String,
        pub nonce_endpoint: String,
        pub deferred_credential_endpoint: String,
        pub notification_endpoint: String,
        // pub credential_request_encryption: Option<CredentialRequestEncryption>,
        // pub credential_response_encryption: Option<CredentialResponseEncryption>,
        pub batch_credential_issuance: Option<BatchCredentialIssuance>,
        // pub display: Option<Vec<Display>>,
        pub credential_configurations_supported: DeterministicMap<String, CredentialConfiguration>,
    }

    #[derive(DeJson, Debug, Default)]
    #[nserde(default)]
    pub struct CredentialRequestEncryption {
        // pub jwks: nanoserde::DeJson, // nanoserde doesn't have a Value type
        pub enc_values_supported: DeterministicSet<String>,
        // pub zip_values_supported: Option<Vec<String>>,
        pub encryption_required: bool,
    }

    #[derive(DeJson, Debug, Default)]
    #[nserde(default)]
    pub struct CredentialResponseEncryption {
        pub alg_values_supported: DeterministicSet<String>,
        pub enc_values_supported: DeterministicSet<String>,
        // pub zip_values_supported: Vec<String>,
        pub encryption_required: bool,
    }

    #[derive(DeJson, Debug, Default)]
    #[nserde(default)]
    pub struct BatchCredentialIssuance {
        pub batch_size: u32,
    }

    #[derive(DeJson, Debug, Default)]
    #[nserde(default)]
    pub struct Display {
        pub name: String,
        pub locale: String,
        pub logo: Option<Logo>,
    }

    #[derive(DeJson, Debug, Default)]
    #[nserde(default)]
    pub struct Logo {
        pub uri: String,
        pub alt_text: String,
    }

    #[derive(DeJson, Debug, Default)]
    #[nserde(default)]
    pub struct CredentialConfiguration {
        pub format: String,
        pub scope: String,
        pub doctype: String,
        pub vct: String,
        pub credential_signing_alg_values_supported: Vec<String>,
        pub cryptographic_binding_methods_supported: Vec<String>,
        pub proof_types_supported: DeterministicMap<String, ProofType>,
    }

    #[derive(DeJson, Debug, Default)]
    #[nserde(default)]
    pub struct ProofType {
        pub proof_signing_alg_values_supported: Vec<String>,
        pub key_attestations_required: Option<KeyAttestationsRequired>,
    }

    #[derive(DeJson, Debug, Default)]
    #[nserde(default)]
    pub struct KeyAttestationsRequired {
        pub key_storage: Vec<String>,
        pub user_authentication: Vec<String>,
    }
}
