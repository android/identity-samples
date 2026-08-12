use crate::json_value::{DeterministicSet, DeterministicMap};

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
pub struct IssuanceExplainer {
    pub per_issuer: DeterministicMap<String, String>,
    pub default: String,
}

#[derive(DeJson, Debug, Default)]
#[nserde(default)]
pub struct IssuanceDisplayData {
    pub subtitle: String,
    pub explainer: IssuanceExplainer,
}

/// Holds display information for the package (app name and icon offsets in the packed blob).
/// This can be either self-declared by a privileged app (representing a package it wants to spoof/represent)
/// or auto-resolved from the calling package's actual package manager info.
#[derive(DeJson, Debug, Default)]
#[nserde(default)]
pub struct PackageInfo {
    pub name: String,
    /// Tuple of (start_offset, end_offset) of the icon bytes packed at the beginning of the registry blob.
    pub icon: (usize, usize),
}

#[derive(DeJson, Debug, Default)]
#[nserde(default)]
pub struct IssuanceMatcherData {
    pub entry_id: String,
    pub entries: Vec<IssuanceDisplayData>,
    pub filter: OpenId4VciFilter,
    pub preferred_protocols: Vec<String>,
    pub self_declared_package_info: Option<PackageInfo>,
    pub package_info: Option<PackageInfo>,
}
