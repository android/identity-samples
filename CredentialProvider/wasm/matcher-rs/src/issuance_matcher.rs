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
        let matched = match &self {
            Self::Unit {} => {
                log::trace!("Filter Unit matched");
                true
            }
            Self::And { filters } => {
                let res = filters.iter().all(|f| f.matches(request));
                log::trace!("Filter And matched: {}", res);
                res
            }
            Self::Or { filters } => {
                let res = filters.iter().any(|f| f.matches(request));
                log::trace!("Filter Or matched: {}", res);
                res
            }
            Self::Not { filter } => {
                let res = !filter.matches(request);
                log::trace!("Filter Not matched: {}", res);
                res
            }
            Self::AllowsIssuers { issuers } => {
                let res = issuers.contains(request.credential_issuer);
                log::trace!(
                    "Filter AllowsIssuers (issuer={}) matched: {}",
                    request.credential_issuer,
                    res
                );
                res
            }
            Self::AllowsConfigurationIds { configuration_ids } => {
                let res = request
                    .credential_configuration_ids
                    .iter()
                    .any(|id| configuration_ids.contains(id));
                log::trace!("Filter AllowsConfigurationIds matched: {}", res);
                res
            }
            Self::SupportsAuthCodeFlow {} => {
                let res = request.grants.contains_key("authorization_code");
                log::trace!("Filter SupportsAuthCodeFlow matched: {}", res);
                res
            }
            Self::SupportsPreAuthFlow {} => {
                let res = request
                    .grants
                    .contains_key("urn:ietf:params:oauth:grant-type:pre-authorized_code");
                log::trace!("Filter SupportsPreAuthFlow matched: {}", res);
                res
            }
            Self::SupportsNonceEndpoint {} => {
                let res = request
                    .credential_issuer_metadata
                    .is_some_and(|m| !m.nonce_endpoint.is_empty());
                log::trace!("Filter SupportsNonceEndpoint matched: {}", res);
                res
            }
            Self::SupportsDeferredCredentialEndpoint {} => {
                let res = request
                    .credential_issuer_metadata
                    .is_some_and(|m| !m.deferred_credential_endpoint.is_empty());
                log::trace!("Filter SupportsDeferredCredentialEndpoint matched: {}", res);
                res
            }
            Self::SupportsNotificationEndpoint {} => {
                let res = request
                    .credential_issuer_metadata
                    .is_some_and(|m| !m.notification_endpoint.is_empty());
                log::trace!("Filter SupportsNotificationEndpoint matched: {}", res);
                res
            }
            Self::RequiresBatchIssuance { min_batch_size } => {
                let res = request.credential_issuer_metadata.is_some_and(|m| {
                    m.batch_credential_issuance
                        .as_ref()
                        .is_some_and(|b| b.batch_size >= *min_batch_size)
                });
                log::trace!(
                    "Filter RequiresBatchIssuance (min={}) matched: {}",
                    min_batch_size,
                    res
                );
                res
            }
            Self::SupportsMdocDoctype { doctypes } => {
                let res = request
                    .credential_configurations
                    .iter()
                    .any(|c| doctypes.contains(&c.doctype));
                log::trace!("Filter SupportsMdocDoctype matched: {}", res);
                res
            }
            Self::SupportsSdJwtVct { vcts } => {
                let res = request
                    .credential_configurations
                    .iter()
                    .any(|c| vcts.contains(&c.vct));
                log::trace!("Filter SupportsSdJwtVct matched: {}", res);
                res
            }
        };
        matched
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
