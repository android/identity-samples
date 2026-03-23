use std::collections::HashSet;

use nanoserde::DeJson;

use crate::openid4vci::RegularizedOpenId4VciRequestData;

#[derive(DeJson, Debug)]
pub enum OpenId4VciFilter {
    Unit {}, // A placeholder that always matches
    And { filters: Vec<OpenId4VciFilter> },
    Or { filters: Vec<OpenId4VciFilter> },
    Not { filter: Box<OpenId4VciFilter> },
    AllowsIssuers { issuers: HashSet<String> },
    AllowsConfigurationIds { configuration_ids: HashSet<String> },
    SupportsAuthCodeFlow {},
    SupportsPreAuthFlow {},
    SupportsNonceEndpoint {},
    SupportsDeferredCredentialEndpoint {},
    SupportsNotificationEndpoint {},
    RequiresBatchIssuance { min_batch_size: u32 },
    SupportsMdocDoctype { doctypes: HashSet<String> },
    SupportsSdJwtVct { vcts: HashSet<String> },
}

impl Default for OpenId4VciFilter {
    fn default() -> Self {
        Self::Unit {}
    }
}

impl OpenId4VciFilter {
    pub fn matches(&self, request: &RegularizedOpenId4VciRequestData) -> bool {
        match &self {
            Self::Unit {} => true,
            Self::And { filters } => filters.iter().all(|f| f.matches(request)),
            Self::Or { filters } => filters.iter().any(|f| f.matches(request)),
            Self::Not { filter } => !filter.matches(request),
            Self::AllowsIssuers { issuers } => issuers.contains(request.credential_issuer),
            Self::AllowsConfigurationIds { configuration_ids } => request
                .credential_configuration_ids
                .iter()
                .any(|id| configuration_ids.contains(id)),
            Self::SupportsAuthCodeFlow {} => request.grants.contains_key("authorization_code"),
            Self::SupportsPreAuthFlow {} => request
                .grants
                .contains_key("urn:ietf:params:oauth:grant-type:pre-authorized_code"),
            Self::SupportsNonceEndpoint {} => request
                .credential_issuer_metadata
                .is_some_and(|m| !m.nonce_endpoint.is_empty()),
            Self::SupportsDeferredCredentialEndpoint {} => request
                .credential_issuer_metadata
                .is_some_and(|m| !m.deferred_credential_endpoint.is_empty()),
            Self::SupportsNotificationEndpoint {} => request
                .credential_issuer_metadata
                .is_some_and(|m| !m.notification_endpoint.is_empty()),
            Self::RequiresBatchIssuance { min_batch_size } => {
                request.credential_issuer_metadata.is_some_and(|m| {
                    m.batch_credential_issuance
                        .as_ref()
                        .is_some_and(|b| b.batch_size >= *min_batch_size)
                })
            }
            Self::SupportsMdocDoctype { doctypes } => request
                .credential_configurations
                .iter()
                .any(|c| doctypes.contains(&c.doctype)),
            Self::SupportsSdJwtVct { vcts } => request
                .credential_configurations
                .iter()
                .any(|c| vcts.contains(&c.vct)),
        }
    }
}

#[derive(DeJson, Debug, Default)]
#[nserde(default)]
pub struct IssuanceMatcherData {
    pub entry_id: String,
    pub icon: (usize, usize),
    pub title: Option<String>,
    pub subtitle: Option<String>,
    pub filter: OpenId4VciFilter,
}
