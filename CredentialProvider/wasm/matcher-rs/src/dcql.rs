use crate::json_value::{DeterministicMap, JsonValue};
pub use crate::openid4vp_models::*;

use std::borrow::Cow;

pub fn add_all_claims<'a>(
    matched_claim_names: &mut Vec<&'a JsonValue>,
    candidate_paths: &'a DeterministicMap<String, JsonValue>,
) {
    candidate_paths
        .values()
        .filter_map(|v| match v {
            JsonValue::Object(obj) => Some((v, obj)),
            _ => None,
        })
        .for_each(|(v, obj)| {
            if let Some(display) = obj.get("display") {
                matched_claim_names.push(display);
            } else {
                add_all_claims_from_json(matched_claim_names, v);
            }
        });
}

fn add_all_claims_from_json<'a>(matched_claim_names: &mut Vec<&'a JsonValue>, json: &'a JsonValue) {
    match json {
        JsonValue::Object(obj) => {
            if let Some(display) = obj.get("display") {
                matched_claim_names.push(display);
            } else {
                obj.values()
                    .for_each(|v| add_all_claims_from_json(matched_claim_names, v));
            }
        }
        JsonValue::Array(arr) => {
            arr.iter()
                .for_each(|v| add_all_claims_from_json(matched_claim_names, v));
        }
        _ => {}
    }
}

fn get_format_candidates<'a>(
    format: &str,
    registry: &'a Registry,
) -> (
    Option<&'a DeterministicMap<String, Vec<RegistryCredential>>>,
    Option<&'a Vec<RegistryIssuanceEntry>>,
) {
    match format {
        "mso_mdoc" => (
            registry.credentials.mso_mdoc.as_ref(),
            registry.credentials.issuance.as_ref().map(|i| &i.mso_mdoc),
        ),
        "dc+sd-jwt" => (
            registry.credentials.sd_jwt.as_ref(),
            registry.credentials.issuance.as_ref().map(|i| &i.sd_jwt),
        ),
        _ => {
            log::warn!("Unsupported format: {}", format);
            (None, None)
        }
    }
}

fn filter_candidates_by_meta<'a>(
    format: &str,
    meta: &Option<DcqlMeta>,
    candidates: Option<&'a DeterministicMap<String, Vec<RegistryCredential>>>,
    inline_issuance_candidates: Option<&'a Vec<RegistryIssuanceEntry>>,
) -> (
    Vec<&'a RegistryCredential>,
    Option<&'a RegistryIssuanceEntry>,
) {
    let Some(meta) = meta else {
        log::trace!(
            "No meta provided, collecting all candidates for format {}",
            format
        );
        let v = candidates
            .map(|c| c.values().flatten().collect())
            .unwrap_or_default();
        return (v, None);
    };

    let mut inline_issuance = None;
    let filtered_candidates = match format {
        "mso_mdoc" => {
            if meta.doctype_value.is_empty() {
                log::trace!("mso_mdoc requested but no doctype_value in meta");
                return (Vec::new(), None);
            }

            log::trace!(
                "Filtering mso_mdoc candidates by doctype: {}",
                meta.doctype_value
            );
            inline_issuance = inline_issuance_candidates.and_then(|cands| {
                cands
                    .iter()
                    .find(|cand| cand.supported.contains(&meta.doctype_value))
                    .map(|cand| {
                        log::debug!(
                            "Found matching inline issuance for doctype {}: {}",
                            meta.doctype_value,
                            cand.id
                        );
                        cand
                    })
            });
            candidates
                .and_then(|c| c.get(&meta.doctype_value))
                .map(|v| v.iter().collect())
                .unwrap_or_default()
        }
        "dc+sd-jwt" => {
            if meta.vct_values.is_empty() {
                return (Vec::new(), None);
            }

            log::trace!(
                "Filtering dc+sd-jwt candidates by vcts: {:?}",
                meta.vct_values
            );
            inline_issuance = meta.vct_values.iter().find_map(|vct| {
                inline_issuance_candidates.and_then(|cands| {
                    cands
                        .iter()
                        .find(|cand| cand.supported.contains(vct))
                        .map(|cand| {
                            log::debug!(
                                "Found matching inline issuance for vct {}: {}",
                                vct,
                                cand.id
                            );
                            cand
                        })
                })
            });

            meta.vct_values
                .iter()
                .filter_map(|vct| candidates.and_then(|c| c.get(vct)))
                .flatten()
                .collect()
        }
        _ => Vec::new(),
    };

    (filtered_candidates, inline_issuance)
}

fn match_candidate_claims<'a>(
    candidate: &'a RegistryCredential,
    claims_req: &'a Vec<DcqlClaim>,
    claim_sets_req: &'a Vec<Vec<String>>,
) -> Option<MatchedCredential<'a>> {
    if claims_req.is_empty() {
        log::debug!(
            "Candidate {}: no specific claims requested, matching all available claims",
            candidate.id
        );
        let mut matched_claim_names = Vec::new();
        add_all_claims(&mut matched_claim_names, &candidate.paths);
        return Some(MatchedCredential {
            id: &candidate.id,
            display: &candidate.display,
            matched_claim_names,
            matched_claim_metadata: Vec::new(),
        });
    }

    if !claim_sets_req.is_empty() {
        log::trace!("Candidate {}: matching against claim_sets", candidate.id);
        let matched_claim_ids: DeterministicMap<&'a str, MatchedClaim<'a>> = claims_req
            .iter()
            .filter(|claim| !claim.id.is_empty())
            .filter_map(|claim| {
                match_claim(claim, &candidate.paths).map(|info| {
                    log::trace!("Candidate {}: claim {} matched", candidate.id, claim.id);
                    (claim.id.as_str(), info)
                })
            })
            .collect();

        return claim_sets_req
            .iter()
            .enumerate()
            .find_map(|(idx, claim_set)| {
                let mut current_set_names = Vec::new();
                let mut current_set_metadata = Vec::new();

                let all_matched = claim_set.iter().all(|claim_id| {
                    let Some(info) = matched_claim_ids.get(claim_id.as_str()) else {
                        log::trace!(
                            "Candidate {}: claim set index {} failed because claim {} did not match",
                            candidate.id,
                            idx,
                            claim_id
                        );
                        return false;
                    };
                    current_set_names.push(info.display);
                    current_set_metadata.push(info.path);
                    true
                });

                if !all_matched {
                    return None;
                }

                log::debug!(
                    "Candidate {}: matched claim set index {}",
                    candidate.id,
                    idx
                );
                Some(MatchedCredential {
                    id: &candidate.id,
                    display: &candidate.display,
                    matched_claim_names: current_set_names,
                    matched_claim_metadata: current_set_metadata,
                })
            })
            .or_else(|| {
                log::debug!("Candidate {}: no claim sets matched", candidate.id);
                None
            });
    }

    log::trace!(
        "Candidate {}: matching all {} requested claims",
        candidate.id,
        claims_req.len()
    );
    let mut matched_claim_names = Vec::new();
    let mut matched_claim_metadata = Vec::new();

    let all_matched = claims_req.iter().all(|claim| {
        let Some(info) = match_claim(claim, &candidate.paths) else {
            log::trace!(
                "Candidate {}: claim path {:?} failed to match",
                candidate.id,
                claim.path
            );
            return false;
        };
        matched_claim_names.push(info.display);
        matched_claim_metadata.push(info.path);
        true
    });

    if !all_matched {
        return None;
    }

    log::debug!("Candidate {}: all claims matched", candidate.id);
    Some(MatchedCredential {
        id: &candidate.id,
        display: &candidate.display,
        matched_claim_names,
        matched_claim_metadata,
    })
}

pub fn match_credential<'a>(
    credential: &'a DcqlCredential,
    registry: &'a Registry,
) -> MatchCredentialResult<'a> {
    log::debug!(
        "Matching credential req id: {}, format: {}",
        credential.id,
        credential.format
    );
    let (candidates, inline_issuance_candidates) =
        get_format_candidates(&credential.format, registry);
    let (filtered_candidates, inline_issuance) = filter_candidates_by_meta(
        &credential.format,
        &credential.meta,
        candidates,
        inline_issuance_candidates,
    );

    log::debug!(
        "Found {} potential candidates after meta filtering",
        filtered_candidates.len()
    );
    let matched_creds = filtered_candidates
        .into_iter()
        .filter_map(|candidate| {
            match_candidate_claims(candidate, &credential.claims, &credential.claim_sets)
        })
        .collect();

    MatchCredentialResult {
        matched_creds,
        inline_issuance,
    }
}

fn match_claim<'a>(
    claim: &'a DcqlClaim,
    candidate_paths: &'a DeterministicMap<String, JsonValue>,
) -> Option<MatchedClaim<'a>> {
    log::trace!("Matching claim path: {:?}", claim.path);

    let final_val =
        claim
            .path
            .iter()
            .enumerate()
            .try_fold(None, |curr_val: Option<&JsonValue>, (i, p)| {
                let next_val = if i == 0 {
                    candidate_paths.get(p)
                } else if let Some(JsonValue::Object(obj)) = curr_val {
                    obj.get(p)
                } else {
                    log::trace!(
                        "Claim path match failed at step {}: key {} not found or not an object",
                        i,
                        p
                    );
                    return Err(());
                };

                match next_val {
                    Some(v) => Ok(Some(v)),
                    None => {
                        log::trace!("Claim path match failed at step {}: key {} not found", i, p);
                        Err(())
                    }
                }
            });

    let curr_val = final_val.ok()??;

    let JsonValue::Object(obj) = curr_val else {
        log::trace!(
            "Claim matched path but final node is not an object at {:?}",
            claim.path
        );
        return None;
    };

    let Some(display) = obj.get("display") else {
        log::trace!(
            "Claim matched path but missing 'display' field at {:?}",
            claim.path
        );
        return None;
    };

    let actual_value = obj.get("value");
    if claim.values.is_empty() {
        log::trace!("Claim path matched successfully (no value constraint)");
        return Some(MatchedClaim {
            display,
            path: &claim.path,
        });
    }

    let Some(actual) = actual_value else {
        log::trace!(
            "Claim value missing at {:?}, but values constraint is present",
            claim.path
        );
        return None;
    };

    if !claim.values.iter().any(|v| v == actual) {
        log::trace!(
            "Claim value mismatch at {:?}. Expected one of {:?}, found {:?}",
            claim.path,
            claim.values,
            actual
        );
        return None;
    }

    log::trace!("Claim matched with value: {:?}", actual);
    Some(MatchedClaim {
        display,
        path: &claim.path,
    })
}

fn evaluate_explicit_credential_sets<'a>(
    credential_sets: &'a [DcqlCredentialSet],
    candidate_matched_credentials: &DeterministicMap<&'a str, DcqlMatchedCredentialEntry<'a>>,
) -> (bool, Vec<Vec<MatchedCredentialSetInfo<'a>>>) {
    let mut matched_credential_sets = Vec::new();

    let all_required_matched = credential_sets
        .iter()
        .enumerate()
        .filter(|(_, set)| set.required.unwrap_or(true))
        .all(|(set_idx, set)| {
            log::debug!(
                "Evaluating required credential_set index {} with {} options",
                set_idx,
                set.options.len()
            );

            let curr_matched_options: Vec<MatchedCredentialSetInfo<'a>> = set
                .options
                .iter()
                .enumerate()
                .filter_map(|(opt_idx, option)| {
                    let mut matched_cred_ids = Vec::new();
                    let option_matched = option.iter().all(|cred_id| {
                        if !candidate_matched_credentials.contains_key(cred_id.as_str()) {
                            log::trace!(
                                "Option {} in set {} failed because {} did not match",
                                opt_idx,
                                set_idx,
                                cred_id
                            );
                            return false;
                        }
                        matched_cred_ids.push(cred_id.as_str());
                        true
                    });

                    if !option_matched {
                        return None;
                    }

                    log::debug!("Option {} in set {} is satisfied", opt_idx, set_idx);
                    Some(MatchedCredentialSetInfo {
                        set_id: Cow::Owned(set_idx.to_string()),
                        option_id: Cow::Owned(opt_idx.to_string()),
                        matched_credential_ids: matched_cred_ids,
                    })
                })
                .collect();

            if curr_matched_options.is_empty() {
                log::info!(
                    "Required credential_set index {} failed to match any options",
                    set_idx
                );
                return false;
            }

            log::info!(
                "Required credential_set index {} matched {} options",
                set_idx,
                curr_matched_options.len()
            );
            matched_credential_sets.push(curr_matched_options);
            true
        });

    (all_required_matched, matched_credential_sets)
}

fn evaluate_implicit_credential_sets<'a>(
    credentials_req: &'a [DcqlCredential],
    candidate_matched_credentials: &DeterministicMap<&'a str, DcqlMatchedCredentialEntry<'a>>,
) -> Vec<Vec<MatchedCredentialSetInfo<'a>>> {
    if credentials_req.len() == candidate_matched_credentials.len() {
        log::info!(
            "All {} credential requirements satisfied",
            credentials_req.len()
        );
        let matched_cred_ids: Vec<&str> = credentials_req.iter().map(|c| c.id.as_str()).collect();
        let single_set_info = MatchedCredentialSetInfo {
            set_id: Cow::Borrowed(""),
            option_id: Cow::Borrowed(""),
            matched_credential_ids: matched_cred_ids,
        };
        return vec![vec![single_set_info]];
    }

    log::info!(
        "Implicit credential requirements failed: {} of {} satisfied",
        candidate_matched_credentials.len(),
        credentials_req.len()
    );
    Vec::new()
}

pub fn dcql_query<'a>(query: &'a DcqlQuery, registry: &'a Registry) -> DcqlMatchResult<'a> {
    log::info!(
        "Starting DCQL query with {} credential requirements",
        query.credentials.len()
    );
    let mut candidate_matched_credentials = DeterministicMap::new();
    let mut candidate_inline_issuance_credentials = DeterministicMap::new();

    for cred_req in &query.credentials {
        let res = match_credential(cred_req, registry);
        if !res.matched_creds.is_empty() {
            log::info!(
                "Credential requirement {} matched {} candidates",
                cred_req.id,
                res.matched_creds.len()
            );
            candidate_matched_credentials.insert(
                cred_req.id.as_str(),
                DcqlMatchedCredentialEntry {
                    id: &cred_req.id,
                    matched: res.matched_creds,
                },
            );
        } else {
            log::info!(
                "Credential requirement {} matched 0 candidates",
                cred_req.id
            );
        }
        if let Some(inline) = res.inline_issuance {
            log::info!(
                "Credential requirement {} has inline issuance available: {}",
                cred_req.id,
                inline.id
            );
            candidate_inline_issuance_credentials.insert(cred_req.id.as_str(), inline);
        }
    }

    let (matched_credential_sets, overall_matched, inline_issuance) = if !query
        .credential_sets
        .is_empty()
    {
        let (overall_matched, sets) = evaluate_explicit_credential_sets(
            &query.credential_sets,
            &candidate_matched_credentials,
        );
        (sets, overall_matched, None)
    } else {
        let sets =
            evaluate_implicit_credential_sets(&query.credentials, &candidate_matched_credentials);

        let mut inline_issuance = None;
        let all_satisfied = query.credentials.len() == candidate_inline_issuance_credentials.len()
            && !candidate_inline_issuance_credentials.is_empty();

        if all_satisfied {
            log::info!("All requirements could be satisfied by inline issuance");
            inline_issuance = candidate_inline_issuance_credentials
                .values()
                .next()
                .copied();
        }
        let overall_matched = !sets.is_empty() || inline_issuance.is_some();
        (sets, overall_matched, inline_issuance)
    };

    if !overall_matched {
        log::info!("Overall DCQL query failed");
        return DcqlMatchResult {
            matched_credential_sets: Vec::new(),
            matched_credentials: DeterministicMap::new(),
            inline_issuance: None,
        };
    }

    log::info!("Overall DCQL query matched");
    DcqlMatchResult {
        matched_credential_sets,
        matched_credentials: candidate_matched_credentials,
        inline_issuance,
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_dcql_query_simple() {
        let mut registry = Registry::default();
        let mut mso_mdoc = DeterministicMap::new();
        let mut creds = Vec::new();
        let mut paths = DeterministicMap::new();
        paths.insert(
            "Given Name".to_string(),
            JsonValue::Object({
                let mut map = DeterministicMap::new();
                map.insert(
                    "display".to_string(),
                    JsonValue::String("Given Name".to_string()),
                );
                map.insert("value".to_string(), JsonValue::String("John".to_string()));
                map
            }),
        );
        creds.push(RegistryCredential {
            id: "mdoc_cred_1".to_string(),
            display: RegistryDisplay::default(),
            paths,
        });
        mso_mdoc.insert("org.iso.18013.5.1.mDL".to_string(), creds);
        registry.credentials.mso_mdoc = Some(mso_mdoc);

        let query = DcqlQuery {
            credentials: vec![DcqlCredential {
                id: "mdl".to_string(),
                format: "mso_mdoc".to_string(),
                meta: Some(DcqlMeta {
                    doctype_value: "org.iso.18013.5.1.mDL".to_string(),
                    vct_values: vec![],
                }),
                claims: vec![],
                claim_sets: vec![],
            }],
            credential_sets: vec![],
        };

        let result = dcql_query(&query, &registry);
        assert!(!result.matched_credentials.is_empty());
        assert!(result.matched_credentials.contains_key("mdl"));
    }

    #[test]
    fn test_dcql_query_value_match() {
        let mut registry = Registry::default();
        let mut mso_mdoc = DeterministicMap::new();
        let mut creds = Vec::new();
        let mut paths = DeterministicMap::new();
        paths.insert(
            "Age".to_string(),
            JsonValue::Object({
                let mut map = DeterministicMap::new();
                map.insert("display".to_string(), JsonValue::String("Age".to_string()));
                map.insert("value".to_string(), JsonValue::Integer(25));
                map
            }),
        );
        creds.push(RegistryCredential {
            id: "mdoc_cred_1".to_string(),
            display: RegistryDisplay::default(),
            paths,
        });
        mso_mdoc.insert("org.iso.18013.5.1.mDL".to_string(), creds);
        registry.credentials.mso_mdoc = Some(mso_mdoc);

        let query = DcqlQuery {
            credentials: vec![DcqlCredential {
                id: "mdl".to_string(),
                format: "mso_mdoc".to_string(),
                meta: Some(DcqlMeta {
                    doctype_value: "org.iso.18013.5.1.mDL".to_string(),
                    vct_values: vec![],
                }),
                claims: vec![DcqlClaim {
                    id: "age".to_string(),
                    path: vec!["Age".to_string()],
                    values: vec![JsonValue::Integer(25)],
                }],
                claim_sets: vec![],
            }],
            credential_sets: vec![],
        };

        let result = dcql_query(&query, &registry);
        assert!(!result.matched_credentials.is_empty());
    }

    #[test]
    fn test_dcql_query_value_mismatch() {
        let mut registry = Registry::default();
        let mut mso_mdoc = DeterministicMap::new();
        let mut creds = Vec::new();
        let mut paths = DeterministicMap::new();
        paths.insert(
            "Age".to_string(),
            JsonValue::Object({
                let mut map = DeterministicMap::new();
                map.insert("display".to_string(), JsonValue::String("Age".to_string()));
                map.insert("value".to_string(), JsonValue::Integer(20));
                map
            }),
        );
        creds.push(RegistryCredential {
            id: "mdoc_cred_1".to_string(),
            display: RegistryDisplay::default(),
            paths,
        });
        mso_mdoc.insert("org.iso.18013.5.1.mDL".to_string(), creds);
        registry.credentials.mso_mdoc = Some(mso_mdoc);

        let query = DcqlQuery {
            credentials: vec![DcqlCredential {
                id: "mdl".to_string(),
                format: "mso_mdoc".to_string(),
                meta: Some(DcqlMeta {
                    doctype_value: "org.iso.18013.5.1.mDL".to_string(),
                    vct_values: vec![],
                }),
                claims: vec![DcqlClaim {
                    id: "age".to_string(),
                    path: vec!["Age".to_string()],
                    values: vec![JsonValue::Integer(25)],
                }],
                claim_sets: vec![],
            }],
            credential_sets: vec![],
        };

        let result = dcql_query(&query, &registry);
        assert!(result.matched_credentials.is_empty());
    }

    #[test]
    fn test_dcql_query_credential_sets() {
        let mut registry = Registry::default();
        let mut mso_mdoc = DeterministicMap::new();
        let mut creds = Vec::new();
        creds.push(RegistryCredential {
            id: "mdoc_cred_1".to_string(),
            display: RegistryDisplay::default(),
            paths: DeterministicMap::new(),
        });
        mso_mdoc.insert("org.iso.18013.5.1.mDL".to_string(), creds);
        registry.credentials.mso_mdoc = Some(mso_mdoc);

        let query = DcqlQuery {
            credentials: vec![DcqlCredential {
                id: "mdl".to_string(),
                format: "mso_mdoc".to_string(),
                meta: Some(DcqlMeta {
                    doctype_value: "org.iso.18013.5.1.mDL".to_string(),
                    vct_values: vec![],
                }),
                claims: vec![],
                claim_sets: vec![],
            }],
            credential_sets: vec![DcqlCredentialSet {
                options: vec![vec!["mdl".to_string()]],
                required: Some(true),
            }],
        };

        let result = dcql_query(&query, &registry);
        assert!(!result.matched_credential_sets.is_empty());
        assert_eq!(result.matched_credential_sets.len(), 1);
        assert_eq!(
            result.matched_credential_sets[0][0].matched_credential_ids,
            vec!["mdl"]
        );
    }

    #[test]
    fn test_add_all_claims() {
        let mut matched_claim_names = Vec::new();
        let mut candidate_paths = DeterministicMap::new();

        let mut claim1 = DeterministicMap::new();
        claim1.insert(
            "display".to_string(),
            JsonValue::String("Claim 1".to_string()),
        );
        candidate_paths.insert("path1".to_string(), JsonValue::Object(claim1));

        let mut claim2 = DeterministicMap::new();
        let mut nested = DeterministicMap::new();
        nested.insert(
            "display".to_string(),
            JsonValue::String("Claim 2".to_string()),
        );
        claim2.insert("nested".to_string(), JsonValue::Object(nested));
        candidate_paths.insert("path2".to_string(), JsonValue::Object(claim2));

        add_all_claims(&mut matched_claim_names, &candidate_paths);

        assert_eq!(matched_claim_names.len(), 2);
        assert_eq!(
            matched_claim_names[0],
            &JsonValue::String("Claim 1".to_string())
        );
        assert_eq!(
            matched_claim_names[1],
            &JsonValue::String("Claim 2".to_string())
        );
    }

    #[test]
    fn test_get_format_candidates() {
        let mut registry = Registry::default();
        let mut mso_mdoc = DeterministicMap::new();
        mso_mdoc.insert("org.iso.18013.5.1.mDL".to_string(), vec![]);
        registry.credentials.mso_mdoc = Some(mso_mdoc);

        let (candidates, _) = get_format_candidates("mso_mdoc", &registry);
        assert!(candidates.is_some());
        assert!(candidates.unwrap().contains_key("org.iso.18013.5.1.mDL"));

        let (candidates, _) = get_format_candidates("unsupported", &registry);
        assert!(candidates.is_none());
    }

    #[test]
    fn test_filter_candidates_by_meta() {
        let mut candidates = DeterministicMap::new();
        let cred1 = RegistryCredential {
            id: "c1".to_string(),
            ..Default::default()
        };
        candidates.insert("doctype1".to_string(), vec![cred1.clone()]);

        let meta = Some(DcqlMeta {
            doctype_value: "doctype1".to_string(),
            vct_values: vec![],
        });

        let (filtered, _) = filter_candidates_by_meta("mso_mdoc", &meta, Some(&candidates), None);
        assert_eq!(filtered.len(), 1);
        assert_eq!(filtered[0].id, "c1");

        let meta_empty = Some(DcqlMeta::default());
        let (filtered, _) =
            filter_candidates_by_meta("mso_mdoc", &meta_empty, Some(&candidates), None);
        assert_eq!(filtered.len(), 0);
    }

    #[test]
    fn test_match_claim() {
        let mut candidate_paths = DeterministicMap::new();
        let mut claim_data = DeterministicMap::new();
        claim_data.insert("display".to_string(), JsonValue::String("Name".to_string()));
        claim_data.insert("value".to_string(), JsonValue::String("John".to_string()));
        candidate_paths.insert("name".to_string(), JsonValue::Object(claim_data));

        let claim_req = DcqlClaim {
            id: "req1".to_string(),
            path: vec!["name".to_string()],
            values: vec![JsonValue::String("John".to_string())],
        };

        let matched = match_claim(&claim_req, &candidate_paths);
        assert!(matched.is_some());
        assert_eq!(
            matched.unwrap().display,
            &JsonValue::String("Name".to_string())
        );

        let claim_req_fail = DcqlClaim {
            id: "req1".to_string(),
            path: vec!["name".to_string()],
            values: vec![JsonValue::String("Jane".to_string())],
        };
        let matched = match_claim(&claim_req_fail, &candidate_paths);
        assert!(matched.is_none());
    }

    #[test]
    fn test_dcql_query_complex_overlapping_sets() {
        let mut registry = Registry::default();
        let mut mso_mdoc = DeterministicMap::new();

        // Helper to create a credential with a claim
        let create_cred = |id: &str, path: &str, value: &str| {
            let mut paths = DeterministicMap::new();
            let mut claim_data = DeterministicMap::new();
            claim_data.insert("display".to_string(), JsonValue::String(path.to_string()));
            claim_data.insert("value".to_string(), JsonValue::String(value.to_string()));

            let mut ns_map = DeterministicMap::new();
            ns_map.insert(path.to_string(), JsonValue::Object(claim_data));

            paths.insert("org.iso.18013.5.1".to_string(), JsonValue::Object(ns_map));

            RegistryCredential {
                id: id.to_string(),
                display: RegistryDisplay::default(),
                paths,
            }
        };

        let cred1 = create_cred("cred1", "given_name", "John");
        let cred2 = create_cred("cred2", "given_name", "Jane");
        let cred3 = create_cred("cred3", "family_name", "Doe");

        mso_mdoc.insert(
            "org.iso.18013.5.1.mDL".to_string(),
            vec![cred1, cred2, cred3],
        );
        registry.credentials.mso_mdoc = Some(mso_mdoc);

        let query = DcqlQuery {
            credential_sets: vec![DcqlCredentialSet {
                options: vec![
                    vec!["mdl1".to_string(), "mdl3".to_string()],
                    vec!["mdl2".to_string(), "mdl3".to_string()],
                    vec!["mdl1".to_string(), "mdl2".to_string()],
                ],
                required: None,
            }],
            credentials: vec![
                DcqlCredential {
                    id: "mdl1".to_string(),
                    format: "mso_mdoc".to_string(),
                    meta: Some(DcqlMeta {
                        doctype_value: "org.iso.18013.5.1.mDL".to_string(),
                        vct_values: vec![],
                    }),
                    claims: vec![DcqlClaim {
                        id: "".to_string(),
                        path: vec!["org.iso.18013.5.1".to_string(), "given_name".to_string()],
                        values: vec![JsonValue::String("John".to_string())],
                    }],
                    claim_sets: vec![],
                },
                DcqlCredential {
                    id: "mdl2".to_string(),
                    format: "mso_mdoc".to_string(),
                    meta: Some(DcqlMeta {
                        doctype_value: "org.iso.18013.5.1.mDL".to_string(),
                        vct_values: vec![],
                    }),
                    claims: vec![DcqlClaim {
                        id: "".to_string(),
                        path: vec!["org.iso.18013.5.1".to_string(), "given_name".to_string()],
                        values: vec![JsonValue::String("Jane".to_string())],
                    }],
                    claim_sets: vec![],
                },
                DcqlCredential {
                    id: "mdl3".to_string(),
                    format: "mso_mdoc".to_string(),
                    meta: Some(DcqlMeta {
                        doctype_value: "org.iso.18013.5.1.mDL".to_string(),
                        vct_values: vec![],
                    }),
                    claims: vec![DcqlClaim {
                        id: "".to_string(),
                        path: vec!["org.iso.18013.5.1".to_string(), "family_name".to_string()],
                        values: vec![JsonValue::String("Doe".to_string())],
                    }],
                    claim_sets: vec![],
                },
            ],
        };

        let result = dcql_query(&query, &registry);
        assert!(!result.matched_credential_sets.is_empty());
        assert_eq!(result.matched_credential_sets[0].len(), 3);
    }

    #[test]
    fn test_dcql_query_optional_set() {
        let mut registry = Registry::default();
        let mut mso_mdoc = DeterministicMap::new();

        let mut paths = DeterministicMap::new();
        let mut claim_data = DeterministicMap::new();
        claim_data.insert("display".to_string(), JsonValue::String("Name".to_string()));
        paths.insert("name".to_string(), JsonValue::Object(claim_data));

        let cred1 = RegistryCredential {
            id: "cred1".to_string(),
            display: RegistryDisplay::default(),
            paths,
        };
        mso_mdoc.insert("doctype1".to_string(), vec![cred1]);
        registry.credentials.mso_mdoc = Some(mso_mdoc);

        let query = DcqlQuery {
            credential_sets: vec![
                DcqlCredentialSet {
                    options: vec![vec!["req1".to_string()]],
                    required: Some(true),
                },
                DcqlCredentialSet {
                    options: vec![vec!["req2".to_string()]],
                    required: Some(false),
                },
            ],
            credentials: vec![
                DcqlCredential {
                    id: "req1".to_string(),
                    format: "mso_mdoc".to_string(),
                    meta: Some(DcqlMeta {
                        doctype_value: "doctype1".to_string(),
                        vct_values: vec![],
                    }),
                    claims: vec![],
                    claim_sets: vec![],
                },
                DcqlCredential {
                    id: "req2".to_string(),
                    format: "mso_mdoc".to_string(),
                    meta: Some(DcqlMeta {
                        doctype_value: "doctype2".to_string(),
                        vct_values: vec![],
                    }), // Will not match
                    claims: vec![],
                    claim_sets: vec![],
                },
            ],
        };

        let result = dcql_query(&query, &registry);
        assert!(!result.matched_credentials.is_empty());
        assert!(result.matched_credentials.contains_key("req1"));
        assert!(!result.matched_credentials.contains_key("req2"));

        assert_eq!(result.matched_credential_sets.len(), 1);
        assert_eq!(
            result.matched_credential_sets[0][0].matched_credential_ids,
            vec!["req1"]
        );
    }
}
