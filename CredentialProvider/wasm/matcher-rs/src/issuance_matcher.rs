use crate::json_value::DeterministicSet;

use nanoserde::DeJson;

use crate::openid4vci::RegularizedOpenId4VciRequestData;

#[derive(DeJson, Debug)]
pub enum OpenId4VciFilter {
    Pass {}, // A placeholder that always matches
    And {
        filters: Vec<OpenId4VciFilter>,
    },
    Or {
        filters: Vec<OpenId4VciFilter>,
    },
    Not {
        filter: Box<OpenId4VciFilter>,
    },
    AllowedIssuers {
        issuers: DeterministicSet<String>,
    },
    AllowedConfigurationIds {
        configuration_ids: DeterministicSet<String>,
    },

    AllowedMdocDoctypes {
        doctypes: DeterministicSet<String>,
    },
    AllowedSdJwtVcts {
        vcts: DeterministicSet<String>,
    },
}

impl Default for OpenId4VciFilter {
    fn default() -> Self {
        Self::Pass {}
    }
}

impl OpenId4VciFilter {
    pub fn matches(&self, request: &RegularizedOpenId4VciRequestData) -> bool {
        let matched = match &self {
            Self::Pass {} => {
                log::trace!("Filter Pass matched");
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
            Self::AllowedIssuers { issuers } => {
                let res = issuers.contains(request.credential_issuer);
                log::trace!(
                    "Filter AllowedIssuers (issuer={}) matched: {}",
                    request.credential_issuer,
                    res
                );
                res
            }
            Self::AllowedConfigurationIds { configuration_ids } => {
                let res = request
                    .credential_configuration_ids
                    .iter()
                    .any(|id| configuration_ids.contains(id));
                log::trace!("Filter AllowedConfigurationIds matched: {}", res);
                res
            }

            Self::AllowedMdocDoctypes { doctypes } => {
                let res = request
                    .credential_configurations
                    .iter()
                    .any(|c| doctypes.contains(&c.doctype));
                log::trace!("Filter AllowedMdocDoctypes matched: {}", res);
                res
            }
            Self::AllowedSdJwtVcts { vcts } => {
                let res = request
                    .credential_configurations
                    .iter()
                    .any(|c| vcts.contains(&c.vct));
                log::trace!("Filter AllowedSdJwtVcts matched: {}", res);
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
    pub title: String,
    pub subtitle: String,
    pub filter: OpenId4VciFilter,
    pub allowed_protocols: Vec<String>,
}
