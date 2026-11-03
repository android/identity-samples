use crate::base64url::decode_base64url;
use crate::credman::CredmanApi;
use crate::json_value::{DeterministicMap, JsonValue};
use crate::openid4vp_models::*;
use nanoserde::DeJson;

/// Reports a credential as a payment transaction entry to Credential Manager.
fn report_payment_transaction_entry(
    credman: &mut impl CredmanApi,
    matched_credential: &MatchedCredential<'_>,
    document_index: i32,
    credential_set_id: &str,
    selection_metadata_json: &str,
    merchant_name: &str,
    transaction_amount: &str,
    additional_info: &str,
    credentials_blob: &[u8],
) -> Result<(), Box<dyn std::error::Error>> {
    log::info!("Reporting as payment entry: {}", matched_credential.id);

    // Extract icon bytes from the blob if available.
    let icon_bytes = matched_credential
        .display
        .verification
        .icon
        .as_ref()
        .map_or(&[][..], |icon| {
            &credentials_blob[icon.start..icon.start + icon.length]
        });

    credman.add_payment_entry_to_set_v2(
        matched_credential.id,
        merchant_name,
        &matched_credential.display.verification.title,
        &matched_credential.display.verification.subtitle,
        icon_bytes,
        transaction_amount,
        &[], // bank_icon (empty for now)
        &[], // provider_icon (empty for now)
        additional_info,
        selection_metadata_json,
        credential_set_id,
        document_index,
        matched_credential.delegation_type,
    );
    Ok(())
}

/// Reports a credential as a standard verification entry to Credential Manager.
fn report_standard_verification_entry(
    credman: &mut impl CredmanApi,
    wasm_version: u32,
    matched_credential: &MatchedCredential<'_>,
    document_index: i32,
    credential_set_id: &str,
    selection_metadata_json: &str,
    credentials_blob: &[u8],
) -> Result<(), Box<dyn std::error::Error>> {
    log::info!("Reporting as standard entry: {}", matched_credential.id);

    // Extract icon bytes from the blob if available.
    let icon_bytes = matched_credential
        .display
        .verification
        .icon
        .as_ref()
        .map_or(&[][..], |icon| {
            &credentials_blob[icon.start..icon.start + icon.length]
        });

    credman.add_entry_to_set(
        matched_credential.id,
        icon_bytes,
        &matched_credential.display.verification.title,
        &matched_credential.display.verification.subtitle,
        &matched_credential.display.verification.explainer,
        "", // warning (empty for now)
        selection_metadata_json,
        credential_set_id,
        document_index,
        matched_credential.delegation_type,
    );

    log::trace!(
        "Reporting {} claims for entry {}",
        matched_credential.matched_claim_names.len(),
        matched_credential.id
    );
    // Report individual fields/claims for this credential.
    for claim in &matched_credential.matched_claim_names {
        let JsonValue::Object(claim_obj) = claim else {
            continue;
        };

        let Some(JsonValue::Object(verification_obj)) = claim_obj.get("verification") else {
            continue;
        };

        let Some(JsonValue::String(display_name)) = verification_obj.get("display") else {
            continue;
        };

        let display_value = match verification_obj.get("display_value") {
            Some(JsonValue::String(s)) => s,
            _ => "",
        };

        credman.add_field_to_entry_set(
            matched_credential.id,
            display_name,
            display_value,
            credential_set_id,
            document_index,
        );
    }

    // Report metadata display text if supported by Wasm version.
    if wasm_version >= 5
        && !matched_credential
            .display
            .verification
            .metadata_display_text
            .is_empty()
    {
        log::trace!(
            "Adding metadata display text: {}",
            matched_credential
                .display
                .verification
                .metadata_display_text
        );
        credman.add_metadata_display_text_to_entry_set(
            matched_credential.id,
            &matched_credential
                .display
                .verification
                .metadata_display_text,
            credential_set_id,
            document_index,
        );
    }
    Ok(())
}

/// Reports a matched credential, deciding whether it's a payment or standard entry.
fn report_matched_credential(
    credman: &mut impl CredmanApi,
    wasm_version: u32,
    matched_credential_entry: &DcqlMatchedCredentialEntry<'_>,
    matched_credential_id: &str,
    document_index: i32,
    request_idx: usize,
    credential_set_id: &str,
    dcql_set_idx: Option<&str>,
    dcql_option_idx: Option<&str>,
    only_fully_delegated: bool,
    credentials_blob: &[u8],
    transaction_info: &Option<(Vec<String>, String, String, String)>,
) -> Result<(), Box<dyn std::error::Error>> {
    log::debug!(
        "Reporting matched credential: id={}, dcql_id={}, doc_idx={}",
        matched_credential_entry.id,
        matched_credential_id,
        document_index
    );
    for matched_credential in &matched_credential_entry.matched {
        if only_fully_delegated && !is_effectively_fully_delegated(matched_credential, wasm_version) {
            log::debug!(
                "Skipping {}: not fully delegated and the entry set is partial",
                matched_credential.id
            );
            continue;
        }
        let metadata = SelectionMetadata {
            claims: &matched_credential.matched_claim_metadata,
            dc_request_index: request_idx,
            dcql_cred_id: matched_credential_id,
            dcql_credential_set_index: dcql_set_idx.unwrap_or(""),
            dcql_option_index: dcql_option_idx.unwrap_or(""),
        };
        let metadata_str = nanoserde::SerJson::serialize_json(&metadata);

        let mut reported = false;
        if let Some((td_ids, merchant, amount, additional)) = transaction_info {
            if td_ids.iter().any(|id| id == matched_credential_id) {
                report_payment_transaction_entry(
                    credman,
                    matched_credential,
                    document_index,
                    credential_set_id,
                    &metadata_str,
                    merchant,
                    amount,
                    additional,
                    credentials_blob,
                )?;
                reported = true;
            }
        }

        if !reported {
            report_standard_verification_entry(
                credman,
                wasm_version,
                matched_credential,
                document_index,
                credential_set_id,
                &metadata_str,
                credentials_blob,
            )?;
        }
    }
    Ok(())
}

/// Extracts transaction info from OpenId4VpData if present.
fn extract_transaction_info(
    openid4vp_data: &OpenId4VpData,
) -> Result<Option<(Vec<String>, String, String, String)>, Box<dyn std::error::Error>> {
    if openid4vp_data.transaction_data.len() != 1 {
        return Ok(None);
    }

    log::debug!("Decoding transaction data");
    let decoded = decode_base64url(&openid4vp_data.transaction_data[0])?;
    let td: TransactionData = DeJson::deserialize_json(std::str::from_utf8(&decoded)?)?;

    if !td.transaction_type.is_empty() {
        log::trace!("Transaction type: {}", td.transaction_type);
        if td.transaction_type == "urn:eudi:sca:payment:1" {
            if let Some(payload) = &td.payload {
                let merchant_name = payload
                    .payee
                    .as_ref()
                    .map(|p| p.name.clone())
                    .unwrap_or_default();
                let transaction_amount = if !payload.amount_display.is_empty() {
                    payload.amount_display.clone()
                } else if !payload.currency.is_empty() {
                    format!("{} {:.2}", payload.currency, payload.amount.unwrap_or(0.0))
                } else {
                    format!("{:.2}", payload.amount.unwrap_or(0.0))
                };
                log::info!(
                    "Found transaction data (sca): merchant={}, amount={}",
                    merchant_name,
                    transaction_amount
                );
                return Ok(Some((
                    td.credential_ids,
                    merchant_name,
                    transaction_amount,
                    td.additional_info.clone(),
                )));
            }
        } else if td.transaction_type == "payment_details" {
            let merchant_name = td.payee_name.clone();
            let transaction_amount = format!("{} {}", td.payment_currency, td.payment_amount);
            log::info!(
                "Found transaction data (details): merchant={}, amount={}",
                merchant_name,
                transaction_amount
            );
            return Ok(Some((
                td.credential_ids,
                merchant_name,
                transaction_amount,
                td.additional_info.clone(),
            )));
        }
    }

    log::info!(
        "Found transaction data: merchant={}, amount={}",
        td.merchant_name,
        td.amount
    );
    Ok(Some((
        td.credential_ids,
        td.merchant_name,
        td.amount,
        td.additional_info,
    )))
}

/// Minimum Wasm API version at which Credential Manager understands delegation types
/// (`SetDelegationTypeForEntryInSet`).
const WASM_VERSION_DELEGATION: u32 = 7;

/// Returns true if `matched_credential` will be fulfilled by Credential Manager through this
/// provider's background service rather than its UI. On hosts older than
/// [`WASM_VERSION_DELEGATION`] the delegation type cannot be communicated, so every entry is
/// effectively non-delegated there.
fn is_effectively_fully_delegated(
    matched_credential: &MatchedCredential<'_>,
    wasm_version: u32,
) -> bool {
    wasm_version >= WASM_VERSION_DELEGATION
        && matched_credential.delegation_type == DelegationType::Full
}

/// Returns true if `option` contributes at least one entry to an entry set, given whether only
/// fully delegated entries may be reported.
fn option_has_reportable_entry(
    option: &MatchedCredentialSetInfo<'_>,
    candidate_matched_credentials: &DeterministicMap<&str, DcqlMatchedCredentialEntry<'_>>,
    only_fully_delegated: bool,
    wasm_version: u32,
) -> bool {
    option.matched_credential_ids.iter().any(|id| {
        candidate_matched_credentials.get(*id).is_some_and(|entry| {
            entry.matched.iter().any(|m| {
                !only_fully_delegated || is_effectively_fully_delegated(m, wasm_version)
            })
        })
    })
}

/// Main entry point for reporting match results to Credential Manager.
/// It generates the Cartesian product of all options across all matched sets.
///
/// Each combination of one option per required set is reported as a single entry set. If this
/// provider matched every credential of the combination, all entries are reported. If the
/// combination is only partially matched, the missing slots can only be filled by other
/// providers' entries, which Credential Manager does exclusively for fully delegated entries;
/// therefore only this provider's [`DelegationType::Full`] entries are reported in that case,
/// and the entry set is omitted entirely if that leaves nothing to report. On hosts that predate
/// delegation support (Wasm API < 7) no entry is delegated, so partial sets are never reported.
pub fn report_match_result(
    credman: &mut impl CredmanApi,
    match_result: &DcqlMatchResult<'_>,
    request_idx: usize,
    openid4vp_data: &OpenId4VpData,
    credentials_blob: &[u8],
) -> Result<(), Box<dyn std::error::Error>> {
    let wasm_version = credman.get_wasm_version();
    log::info!("Reporting match results. Wasm version: {}", wasm_version);

    if !match_result.matched_credential_sets.is_empty() {
        let transaction_info = extract_transaction_info(openid4vp_data)?;

        // Recursive helper to generate Cartesian product.
        fn report_combinations(
            credman: &mut impl CredmanApi,
            wasm_version: u32,
            request_idx: usize,
            credential_sets: &[Vec<MatchedCredentialSetInfo<'_>>],
            selected_option_indices: &mut Vec<usize>,
            candidate_matched_credentials: &DeterministicMap<&str, DcqlMatchedCredentialEntry<'_>>,
            credentials_blob: &[u8],
            transaction_info: &Option<(Vec<String>, String, String, String)>,
        ) -> Result<(), Box<dyn std::error::Error>> {
            if selected_option_indices.len() == credential_sets.len() {
                // We have selected one option from each set. Now report this combination.
                let selected_options: Vec<&MatchedCredentialSetInfo<'_>> = selected_option_indices
                    .iter()
                    .enumerate()
                    .map(|(set_idx, &option_idx)| &credential_sets[set_idx][option_idx])
                    .collect();

                // A partial entry set may only contain fully delegated entries (see the doc
                // comment on `report_match_result`).
                let only_fully_delegated = selected_options.iter().any(|o| o.is_partial());
                if !selected_options.iter().any(|option| {
                    option_has_reportable_entry(
                        option,
                        candidate_matched_credentials,
                        only_fully_delegated,
                        wasm_version,
                    )
                }) {
                    log::info!(
                        "Skipping partial combination {:?}: no fully delegated entries",
                        selected_option_indices
                    );
                    return Ok(());
                }

                let mut set_id_parts = vec![format!("req:{}", request_idx)];
                let mut total_length = 0;

                for option in &selected_options {
                    if !option.set_id.is_empty() {
                        set_id_parts
                            .push(format!("set:{};option:{}", option.set_id, option.option_id));
                    }
                    // Use the option length as requested by the verifier (not just the number
                    // of credentials this provider matched) so that, for a partial match,
                    // Credential Manager can fill the remaining slots from other providers.
                    total_length += option.option_length;
                }

                // Preserve the "req:N;null" format for implicit sets (no explicit sets matched).
                let set_id_str = if set_id_parts.len() == 1 {
                    format!("req:{};null", request_idx)
                } else {
                    set_id_parts.join(";")
                };

                if wasm_version > 1 {
                    credman.add_entry_set(&set_id_str, total_length as i32);
                }

                // Each credential is reported at its position within the concatenated options,
                // i.e. the index the verifier assigned to it. For a partial match this leaves
                // gaps that other providers are expected to fill.
                let mut option_offset = 0;
                for option in &selected_options {
                    for (matched_credential_id, &index_in_option) in option
                        .matched_credential_ids
                        .iter()
                        .zip(option.matched_indices.iter())
                    {
                        let Some(doc) = candidate_matched_credentials.get(*matched_credential_id)
                        else {
                            continue;
                        };
                        report_matched_credential(
                            credman,
                            wasm_version,
                            doc,
                            matched_credential_id,
                            (option_offset + index_in_option) as i32,
                            request_idx,
                            &set_id_str,
                            Some(&option.set_id),
                            Some(&option.option_id),
                            only_fully_delegated,
                            credentials_blob,
                            transaction_info,
                        )?;
                    }
                    option_offset += option.option_length;
                }
                return Ok(());
            }

            // Recurse to pick options for the next set.
            let next_set_idx = selected_option_indices.len();
            for option_idx in 0..credential_sets[next_set_idx].len() {
                selected_option_indices.push(option_idx);
                report_combinations(
                    credman,
                    wasm_version,
                    request_idx,
                    credential_sets,
                    selected_option_indices,
                    candidate_matched_credentials,
                    credentials_blob,
                    transaction_info,
                )?;
                selected_option_indices.pop();
            }

            Ok(())
        }

        let mut selected_option_indices = Vec::new();
        report_combinations(
            credman,
            wasm_version,
            request_idx,
            &match_result.matched_credential_sets,
            &mut selected_option_indices,
            &match_result.matched_credentials,
            credentials_blob,
            &transaction_info,
        )?;
    }

    if let Some(inline) = &match_result.inline_issuance {
        log::info!("Reporting inline issuance entry: {}", inline.id);
        let icon_bytes = inline.icon.as_ref().map_or(&[][..], |icon| {
            &credentials_blob[icon.start..icon.start + icon.length]
        });

        credman.add_inline_issuance_entry(&inline.id, icon_bytes, &inline.title, &inline.subtitle);
    }

    Ok(())
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::json_value::DeterministicSet;
    use std::borrow::Cow;

    struct MockCredman {
        wasm_version: u32,
        added_entries: Vec<String>,
        added_delegations: Vec<(String, DelegationType)>,
        added_fields: Vec<(String, String, String)>,
        payment_entries: Vec<String>,
        inline_entries: Vec<String>,
        added_entry_sets: Vec<(String, i32)>,
        added_entry_indices: Vec<(String, i32)>,
    }

    impl CredmanApi for MockCredman {
        fn get_request_buffer(&self) -> Vec<u8> {
            vec![]
        }
        fn get_registered_data(&self) -> Vec<u8> {
            vec![]
        }
        fn get_wasm_version(&self) -> u32 {
            self.wasm_version
        }
        fn add_string_id_entry(
            &mut self,
            _entry_id: &str,
            _icon: &[u8],
            _title: &str,
            _subtitle: &str,
            _disclaimer: &str,
            _warning: &str,
        ) {
        }
        fn add_issuance_entry(
            &mut self,
            _entry_id: &str,
            _icon: &[u8],
            _title: &str,
            _subtitle: &str,
            _explainer: &str,
            _metadata: &str,
        ) {
        }
        fn add_entry_set(&mut self, set_id: &str, length: i32) {
            self.added_entry_sets.push((set_id.to_string(), length));
        }
        fn add_entry_to_set(
            &mut self,
            cred_id: &str,
            _icon: &[u8],
            _title: &str,
            _subtitle: &str,
            _disclaimer: &str,
            _warning: &str,
            _metadata: &str,
            _set_id: &str,
            set_index: i32,
            delegation_type: DelegationType,
        ) {
            self.added_entries.push(cred_id.to_string());
            self.added_delegations.push((cred_id.to_string(), delegation_type));
            self.added_entry_indices.push((cred_id.to_string(), set_index));
        }
        fn add_field_to_entry_set(
            &mut self,
            cred_id: &str,
            field_display_name: &str,
            field_display_value: &str,
            _set_id: &str,
            _set_index: i32,
        ) {
            self.added_fields.push((
                cred_id.to_string(),
                field_display_name.to_string(),
                field_display_value.to_string(),
            ));
        }
        fn add_payment_entry_to_set_v2(
            &mut self,
            cred_id: &str,
            _merchant_name: &str,
            _payment_method_name: &str,
            _payment_method_subtitle: &str,
            _payment_method_icon: &[u8],
            _transaction_amount: &str,
            _bank_icon: &[u8],
            _payment_provider_icon: &[u8],
            _additional_info: &str,
            _metadata: &str,
            _set_id: &str,
            _set_index: i32,
            delegation_type: DelegationType,
        ) {
            self.payment_entries.push(cred_id.to_string());
            self.added_delegations.push((cred_id.to_string(), delegation_type));
        }
        fn add_inline_issuance_entry(
            &mut self,
            cred_id: &str,
            _icon: &[u8],
            _title: &str,
            _subtitle: &str,
        ) {
            self.inline_entries.push(cred_id.to_string());
        }
        fn add_metadata_display_text_to_entry_set(
            &mut self,
            _cred_id: &str,
            _metadata_display_text: &str,
            _set_id: &str,
            _set_index: i32,
        ) {
        }
        fn self_declare_package_info(
            &mut self,
            _package_display_name: &str,
            _package_icon: &[u8],
        ) {
        }
    }

    #[test]
    fn test_report_match_result_empty() {
        let mut mock = MockCredman {
            wasm_version: 1,
            added_entries: vec![],
            added_delegations: vec![],
            added_fields: vec![],
            payment_entries: vec![],
            inline_entries: vec![],
            added_entry_sets: vec![],
            added_entry_indices: vec![],
        };
        let match_result = DcqlMatchResult {
            matched_credential_sets: vec![],
            matched_credentials: DeterministicMap::new(),
            inline_issuance: None,
        };
        let openid4vp_data = OpenId4VpData::default();

        report_match_result(&mut mock, &match_result, 0, &openid4vp_data, &[]).unwrap();

        assert!(mock.added_entries.is_empty());
    }

    #[test]
    fn test_report_standard_entry() {
        let mut mock = MockCredman {
            wasm_version: 1,
            added_entries: vec![],
            added_delegations: vec![],
            added_fields: vec![],
            payment_entries: vec![],
            inline_entries: vec![],
            added_entry_sets: vec![],
            added_entry_indices: vec![],
        };

        let display = RegistryDisplay {
            verification: RegistryVerification {
                title: "Test Cred".to_string(),
                subtitle: "".to_string(),
                explainer: "".to_string(),
                warning: "".to_string(),
                metadata_display_text: "".to_string(),
                icon: None,
            },
        };

        let mut matched_credentials = DeterministicMap::new();
        matched_credentials.insert(
            "cred1",
            DcqlMatchedCredentialEntry {
                id: "cred1",
                matched: vec![MatchedCredential {
                    id: "cred1",
                    delegation_type: DelegationType::None,
                    display: &display,
                    matched_claim_names: vec![],
                    matched_claim_metadata: vec![],
                }],
            },
        );

        let match_result = DcqlMatchResult {
            matched_credential_sets: vec![vec![MatchedCredentialSetInfo {
                set_id: Cow::Borrowed("set1"),
                option_id: Cow::Borrowed("opt1"),
                matched_credential_ids: vec!["cred1"],
                matched_indices: vec![0],
                option_length: 1,
            }]],
            matched_credentials,
            inline_issuance: None,
        };
        let openid4vp_data = OpenId4VpData::default();

        report_match_result(&mut mock, &match_result, 0, &openid4vp_data, &[]).unwrap();

        assert_eq!(mock.added_entries, vec!["cred1"]);
        assert_eq!(mock.added_delegations, vec![("cred1".to_string(), DelegationType::None)]);
    }

    #[test]
    fn test_report_standard_entry_with_delegation_full() {
        let mut mock = MockCredman {
            wasm_version: 7,
            added_entries: vec![],
            added_delegations: vec![],
            added_fields: vec![],
            payment_entries: vec![],
            inline_entries: vec![],
            added_entry_sets: vec![],
            added_entry_indices: vec![],
        };

        let display = RegistryDisplay {
            verification: RegistryVerification {
                title: "Delegated Cred".to_string(),
                subtitle: "".to_string(),
                explainer: "".to_string(),
                warning: "".to_string(),
                metadata_display_text: "".to_string(),
                icon: None,
            },
        };

        let mut matched_credentials = DeterministicMap::new();
        matched_credentials.insert(
            "cred_full",
            DcqlMatchedCredentialEntry {
                id: "cred_full",
                matched: vec![MatchedCredential {
                    id: "cred_full",
                    delegation_type: DelegationType::Full,
                    display: &display,
                    matched_claim_names: vec![],
                    matched_claim_metadata: vec![],
                }],
            },
        );

        let match_result = DcqlMatchResult {
            matched_credential_sets: vec![vec![MatchedCredentialSetInfo {
                set_id: Cow::Borrowed("set1"),
                option_id: Cow::Borrowed("opt1"),
                matched_credential_ids: vec!["cred_full"],
                matched_indices: vec![0],
                option_length: 1,
            }]],
            matched_credentials,
            inline_issuance: None,
        };
        let openid4vp_data = OpenId4VpData::default();

        report_match_result(&mut mock, &match_result, 0, &openid4vp_data, &[]).unwrap();

        assert_eq!(mock.added_entries, vec!["cred_full"]);
        assert_eq!(
            mock.added_delegations,
            vec![("cred_full".to_string(), DelegationType::Full)]
        );
    }

    #[test]
    fn test_report_payment_entry_with_delegation_full() {
        let mut mock = MockCredman {
            wasm_version: 7,
            added_entries: vec![],
            added_delegations: vec![],
            added_fields: vec![],
            payment_entries: vec![],
            inline_entries: vec![],
            added_entry_sets: vec![],
            added_entry_indices: vec![],
        };

        let display = RegistryDisplay {
            verification: RegistryVerification {
                title: "Payment Card".to_string(),
                subtitle: "".to_string(),
                explainer: "".to_string(),
                warning: "".to_string(),
                metadata_display_text: "".to_string(),
                icon: None,
            },
        };

        let mut matched_credentials = DeterministicMap::new();
        matched_credentials.insert(
            "cred_pay",
            DcqlMatchedCredentialEntry {
                id: "cred_pay",
                matched: vec![MatchedCredential {
                    id: "cred_pay",
                    delegation_type: DelegationType::Full,
                    display: &display,
                    matched_claim_names: vec![],
                    matched_claim_metadata: vec![],
                }],
            },
        );

        let match_result = DcqlMatchResult {
            matched_credential_sets: vec![vec![MatchedCredentialSetInfo {
                set_id: Cow::Borrowed("set1"),
                option_id: Cow::Borrowed("opt1"),
                matched_credential_ids: vec!["cred_pay"],
                matched_indices: vec![0],
                option_length: 1,
            }]],
            matched_credentials,
            inline_issuance: None,
        };

        let td_b64 = "eyJ0eXBlIjoidXJuOmV1ZGk6c2NhOnBheW1lbnQ6MSIsInBheWxvYWQiOnsicGF5ZWUiOnsibmFtZSI6Ik1lcmNoYW50IFgifSwiYW1vdW50X2Rpc3BsYXkiOiJFVVIgNTAuMDAifSwiY3JlZGVudGlhbF9pZHMiOlsibWRsIiwiY3JlZF9wYXkiXX0";
        let openid4vp_data = OpenId4VpData {
            transaction_data: vec![td_b64.to_string()],
            ..Default::default()
        };

        report_match_result(&mut mock, &match_result, 0, &openid4vp_data, &[]).unwrap();

        assert_eq!(mock.payment_entries, vec!["cred_pay"]);
        assert_eq!(
            mock.added_delegations,
            vec![("cred_pay".to_string(), DelegationType::Full)]
        );
    }

    #[test]
    fn test_report_inline_issuance() {
        let mut mock = MockCredman {
            wasm_version: 1,
            added_entries: vec![],
            added_delegations: vec![],
            added_fields: vec![],
            payment_entries: vec![],
            inline_entries: vec![],
            added_entry_sets: vec![],
            added_entry_indices: vec![],
        };

        let inline = RegistryIssuanceEntry {
            id: "inline_cred".to_string(),
            title: "Get Inline".to_string(),
            subtitle: "".to_string(),
            icon: None,
            supported: DeterministicSet::default(),
        };

        let match_result = DcqlMatchResult {
            matched_credential_sets: vec![],
            matched_credentials: DeterministicMap::new(),
            inline_issuance: Some(&inline),
        };
        let openid4vp_data = OpenId4VpData::default();

        report_match_result(&mut mock, &match_result, 0, &openid4vp_data, &[]).unwrap();

        assert_eq!(mock.inline_entries, vec!["inline_cred"]);
    }

    #[test]
    fn test_report_cartesian_product() {
        let mut mock = MockCredman {
            wasm_version: 2,
            added_entries: vec![],
            added_delegations: vec![],
            added_fields: vec![],
            payment_entries: vec![],
            inline_entries: vec![],
            added_entry_sets: vec![],
            added_entry_indices: vec![],
        };

        let display = RegistryDisplay::default();
        let match_result = DcqlMatchResult {
            matched_credential_sets: vec![
                vec![
                    MatchedCredentialSetInfo {
                        set_id: Cow::Borrowed("set1"),
                        option_id: Cow::Borrowed("opt1"),
                        matched_credential_ids: vec!["cred1"],
                        matched_indices: vec![0],
                        option_length: 1,
                    },
                    MatchedCredentialSetInfo {
                        set_id: Cow::Borrowed("set1"),
                        option_id: Cow::Borrowed("opt2"),
                        matched_credential_ids: vec!["cred2"],
                        matched_indices: vec![0],
                        option_length: 1,
                    },
                ],
                vec![
                    MatchedCredentialSetInfo {
                        set_id: Cow::Borrowed("set2"),
                        option_id: Cow::Borrowed("opt3"),
                        matched_credential_ids: vec!["cred3"],
                        matched_indices: vec![0],
                        option_length: 1,
                    },
                    MatchedCredentialSetInfo {
                        set_id: Cow::Borrowed("set2"),
                        option_id: Cow::Borrowed("opt4"),
                        matched_credential_ids: vec!["cred4"],
                        matched_indices: vec![0],
                        option_length: 1,
                    },
                ],
                vec![
                    MatchedCredentialSetInfo {
                        set_id: Cow::Borrowed("set3"),
                        option_id: Cow::Borrowed("opt5"),
                        matched_credential_ids: vec!["cred5"],
                        matched_indices: vec![0],
                        option_length: 1,
                    },
                    MatchedCredentialSetInfo {
                        set_id: Cow::Borrowed("set3"),
                        option_id: Cow::Borrowed("opt6"),
                        matched_credential_ids: vec!["cred6"],
                        matched_indices: vec![0],
                        option_length: 1,
                    },
                ],
            ],
            matched_credentials: {
                let mut map = DeterministicMap::new();
                let create_entry = |id: &'static str| DcqlMatchedCredentialEntry {
                    id,
                    matched: vec![MatchedCredential {
                        id,
                        delegation_type: DelegationType::None,
                        display: &display,
                        matched_claim_names: vec![],
                        matched_claim_metadata: vec![],
                    }],
                };
                map.insert("cred1", create_entry("cred1"));
                map.insert("cred2", create_entry("cred2"));
                map.insert("cred3", create_entry("cred3"));
                map.insert("cred4", create_entry("cred4"));
                map.insert("cred5", create_entry("cred5"));
                map.insert("cred6", create_entry("cred6"));
                map
            },
            inline_issuance: None,
        };

        let openid4vp_data = OpenId4VpData::default();

        report_match_result(&mut mock, &match_result, 0, &openid4vp_data, &[]).unwrap();

        // We expect 2^3 = 8 calls to add_entry_set
        assert_eq!(mock.added_entry_sets.len(), 8);

        let set_ids: Vec<String> = mock
            .added_entry_sets
            .iter()
            .map(|(id, _)| id.clone())
            .collect();

        assert!(set_ids.contains(
            &"req:0;set:set1;option:opt1;set:set2;option:opt3;set:set3;option:opt5".to_string()
        ));
        assert!(set_ids.contains(
            &"req:0;set:set1;option:opt1;set:set2;option:opt3;set:set3;option:opt6".to_string()
        ));
        assert!(set_ids.contains(
            &"req:0;set:set1;option:opt1;set:set2;option:opt4;set:set3;option:opt5".to_string()
        ));
        assert!(set_ids.contains(
            &"req:0;set:set1;option:opt1;set:set2;option:opt4;set:set3;option:opt6".to_string()
        ));
        assert!(set_ids.contains(
            &"req:0;set:set1;option:opt2;set:set2;option:opt3;set:set3;option:opt5".to_string()
        ));
        assert!(set_ids.contains(
            &"req:0;set:set1;option:opt2;set:set2;option:opt3;set:set3;option:opt6".to_string()
        ));
        assert!(set_ids.contains(
            &"req:0;set:set1;option:opt2;set:set2;option:opt4;set:set3;option:opt5".to_string()
        ));
        assert!(set_ids.contains(
            &"req:0;set:set1;option:opt2;set:set2;option:opt4;set:set3;option:opt6".to_string()
        ));
    }

    fn full_delegation_entry<'a>(
        id: &'a str,
        display: &'a RegistryDisplay,
    ) -> DcqlMatchedCredentialEntry<'a> {
        DcqlMatchedCredentialEntry {
            id,
            matched: vec![MatchedCredential {
                id,
                delegation_type: DelegationType::Full,
                display,
                matched_claim_names: vec![],
                matched_claim_metadata: vec![],
            }],
        }
    }

    #[test]
    fn test_report_partial_option_uses_verifier_length_and_index() {
        let mut mock = MockCredman {
            wasm_version: 7,
            added_entries: vec![],
            added_delegations: vec![],
            added_fields: vec![],
            payment_entries: vec![],
            inline_entries: vec![],
            added_entry_sets: vec![],
            added_entry_indices: vec![],
        };
        let display = RegistryDisplay::default();
        let mut matched_credentials = DeterministicMap::new();
        matched_credentials.insert("email", full_delegation_entry("email", &display));

        // The verifier asked for ["phone", "email"]; this provider only holds "email".
        let match_result = DcqlMatchResult {
            matched_credential_sets: vec![vec![MatchedCredentialSetInfo {
                set_id: Cow::Borrowed("0"),
                option_id: Cow::Borrowed("0"),
                matched_credential_ids: vec!["email"],
                matched_indices: vec![1],
                option_length: 2,
            }]],
            matched_credentials,
            inline_issuance: None,
        };

        report_match_result(&mut mock, &match_result, 0, &OpenId4VpData::default(), &[]).unwrap();

        // The entry set is declared with the full length so Credential Manager can merge another
        // provider's entry for index 0, and our credential is placed at its own index (1).
        assert_eq!(mock.added_entry_sets, vec![("req:0;set:0;option:0".to_string(), 2)]);
        assert_eq!(mock.added_entry_indices, vec![("email".to_string(), 1)]);
        assert_eq!(mock.added_delegations, vec![("email".to_string(), DelegationType::Full)]);
    }

    fn no_delegation_entry<'a>(
        id: &'a str,
        display: &'a RegistryDisplay,
    ) -> DcqlMatchedCredentialEntry<'a> {
        DcqlMatchedCredentialEntry {
            id,
            matched: vec![MatchedCredential {
                id,
                delegation_type: DelegationType::None,
                display,
                matched_claim_names: vec![],
                matched_claim_metadata: vec![],
            }],
        }
    }

    fn new_mock(wasm_version: u32) -> MockCredman {
        MockCredman {
            wasm_version,
            added_entries: vec![],
            added_delegations: vec![],
            added_fields: vec![],
            payment_entries: vec![],
            inline_entries: vec![],
            added_entry_sets: vec![],
            added_entry_indices: vec![],
        }
    }

    #[test]
    fn test_report_partial_option_without_full_delegation_reports_nothing() {
        let mut mock = new_mock(7);
        let display = RegistryDisplay::default();
        let mut matched_credentials = DeterministicMap::new();
        matched_credentials.insert("email", no_delegation_entry("email", &display));

        // ["phone", "email"] requested, only "email" matched and it is not delegated: nothing
        // could ever complete this set, so it must not be declared at all.
        let match_result = DcqlMatchResult {
            matched_credential_sets: vec![vec![MatchedCredentialSetInfo {
                set_id: Cow::Borrowed("0"),
                option_id: Cow::Borrowed("0"),
                matched_credential_ids: vec!["email"],
                matched_indices: vec![1],
                option_length: 2,
            }]],
            matched_credentials,
            inline_issuance: None,
        };

        report_match_result(&mut mock, &match_result, 0, &OpenId4VpData::default(), &[]).unwrap();

        assert!(mock.added_entry_sets.is_empty());
        assert!(mock.added_entries.is_empty());
    }

    #[test]
    fn test_report_partial_set_is_not_reported_before_delegation_support() {
        // Same as test_report_partial_option_uses_verifier_length_and_index, but on a host
        // that predates delegation (Wasm API < 7): the FULL entry cannot be flagged as such, so
        // it is effectively non-delegated and the partial set must not be declared.
        let mut mock = new_mock(6);
        let display = RegistryDisplay::default();
        let mut matched_credentials = DeterministicMap::new();
        matched_credentials.insert("email", full_delegation_entry("email", &display));

        let match_result = DcqlMatchResult {
            matched_credential_sets: vec![vec![MatchedCredentialSetInfo {
                set_id: Cow::Borrowed("0"),
                option_id: Cow::Borrowed("0"),
                matched_credential_ids: vec!["email"],
                matched_indices: vec![1],
                option_length: 2,
            }]],
            matched_credentials,
            inline_issuance: None,
        };

        report_match_result(&mut mock, &match_result, 0, &OpenId4VpData::default(), &[]).unwrap();

        assert!(mock.added_entry_sets.is_empty());
        assert!(mock.added_entries.is_empty());
    }

    #[test]
    fn test_report_complete_set_before_delegation_support_reports_all_entries() {
        let mut mock = new_mock(6);
        let display = RegistryDisplay::default();
        let mut matched_credentials = DeterministicMap::new();
        matched_credentials.insert("a", full_delegation_entry("a", &display));
        matched_credentials.insert("c", no_delegation_entry("c", &display));

        let match_result = DcqlMatchResult {
            matched_credential_sets: vec![
                vec![MatchedCredentialSetInfo {
                    set_id: Cow::Borrowed("0"),
                    option_id: Cow::Borrowed("0"),
                    matched_credential_ids: vec!["a"],
                    matched_indices: vec![0],
                    option_length: 1,
                }],
                vec![MatchedCredentialSetInfo {
                    set_id: Cow::Borrowed("1"),
                    option_id: Cow::Borrowed("0"),
                    matched_credential_ids: vec!["c"],
                    matched_indices: vec![0],
                    option_length: 1,
                }],
            ],
            matched_credentials,
            inline_issuance: None,
        };

        report_match_result(&mut mock, &match_result, 0, &OpenId4VpData::default(), &[]).unwrap();

        assert_eq!(
            mock.added_entry_sets,
            vec![("req:0;set:0;option:0;set:1;option:0".to_string(), 2)]
        );
        assert_eq!(mock.added_entries, vec!["a", "c"]);
    }

    #[test]
    fn test_report_partial_set_drops_non_delegated_entries_only() {
        let mut mock = new_mock(7);
        let display = RegistryDisplay::default();
        let mut matched_credentials = DeterministicMap::new();
        matched_credentials.insert("a", full_delegation_entry("a", &display));
        matched_credentials.insert("c", no_delegation_entry("c", &display));

        // Set 0 option 0 = ["a", "b"]: partial, "a" FULL.
        // Set 1 option 0 = ["c"]: complete but not delegated.
        // The combined entry set is partial, so "a" is reported (another provider may fill
        // "b") while "c" is withheld: it could only be fulfilled through this provider's UI,
        // which Credential Manager cannot combine with other providers' entries.
        let match_result = DcqlMatchResult {
            matched_credential_sets: vec![
                vec![MatchedCredentialSetInfo {
                    set_id: Cow::Borrowed("0"),
                    option_id: Cow::Borrowed("0"),
                    matched_credential_ids: vec!["a"],
                    matched_indices: vec![0],
                    option_length: 2,
                }],
                vec![MatchedCredentialSetInfo {
                    set_id: Cow::Borrowed("1"),
                    option_id: Cow::Borrowed("0"),
                    matched_credential_ids: vec!["c"],
                    matched_indices: vec![0],
                    option_length: 1,
                }],
            ],
            matched_credentials,
            inline_issuance: None,
        };

        report_match_result(&mut mock, &match_result, 0, &OpenId4VpData::default(), &[]).unwrap();

        assert_eq!(
            mock.added_entry_sets,
            vec![("req:0;set:0;option:0;set:1;option:0".to_string(), 3)]
        );
        assert_eq!(mock.added_entries, vec!["a"]);
        assert_eq!(mock.added_entry_indices, vec![("a".to_string(), 0)]);
    }

    #[test]
    fn test_report_complete_set_reports_mixed_delegation_entries() {
        let mut mock = new_mock(7);
        let display = RegistryDisplay::default();
        let mut matched_credentials = DeterministicMap::new();
        matched_credentials.insert("a", full_delegation_entry("a", &display));
        matched_credentials.insert("c", no_delegation_entry("c", &display));

        // Every requested credential is matched by this provider, so the entry set is
        // self-contained and non-delegated entries are reported as before.
        let match_result = DcqlMatchResult {
            matched_credential_sets: vec![
                vec![MatchedCredentialSetInfo {
                    set_id: Cow::Borrowed("0"),
                    option_id: Cow::Borrowed("0"),
                    matched_credential_ids: vec!["a"],
                    matched_indices: vec![0],
                    option_length: 1,
                }],
                vec![MatchedCredentialSetInfo {
                    set_id: Cow::Borrowed("1"),
                    option_id: Cow::Borrowed("0"),
                    matched_credential_ids: vec!["c"],
                    matched_indices: vec![0],
                    option_length: 1,
                }],
            ],
            matched_credentials,
            inline_issuance: None,
        };

        report_match_result(&mut mock, &match_result, 0, &OpenId4VpData::default(), &[]).unwrap();

        assert_eq!(
            mock.added_entry_sets,
            vec![("req:0;set:0;option:0;set:1;option:0".to_string(), 2)]
        );
        assert_eq!(mock.added_entries, vec!["a", "c"]);
        assert_eq!(
            mock.added_delegations,
            vec![
                ("a".to_string(), DelegationType::Full),
                ("c".to_string(), DelegationType::None)
            ]
        );
    }

    #[test]
    fn test_report_implicit_partial_set_only_reports_delegated_entries() {
        let mut mock = new_mock(7);
        let display = RegistryDisplay::default();
        let mut matched_credentials = DeterministicMap::new();
        matched_credentials.insert("email", full_delegation_entry("email", &display));
        matched_credentials.insert("mdl", no_delegation_entry("mdl", &display));

        // No credential_sets: ["phone", "email", "mdl"] all required; "phone" is missing.
        let match_result = DcqlMatchResult {
            matched_credential_sets: vec![vec![MatchedCredentialSetInfo {
                set_id: Cow::Borrowed(""),
                option_id: Cow::Borrowed(""),
                matched_credential_ids: vec!["email", "mdl"],
                matched_indices: vec![1, 2],
                option_length: 3,
            }]],
            matched_credentials,
            inline_issuance: None,
        };

        report_match_result(&mut mock, &match_result, 0, &OpenId4VpData::default(), &[]).unwrap();

        assert_eq!(mock.added_entry_sets, vec![("req:0;null".to_string(), 3)]);
        assert_eq!(mock.added_entry_indices, vec![("email".to_string(), 1)]);
    }

    /// End-to-end (dcql_query + report_match_result) for a request with several sets and
    /// options where this provider can only contribute to some slots:
    ///   credential_sets: [ {a,b} ], [ {c} | {e,f} ], optional [ {d} ]
    ///   provider:        a1 FULL, a2 NONE, a3 FULL, b NONE, e FULL   (no c, d, f)
    #[test]
    fn test_report_offers_delegated_entries_for_every_option_combination() {
        use crate::dcql::dcql_query;

        let rc = |id: &str, d: DelegationType| RegistryCredential {
            id: id.to_string(),
            delegation_type: d,
            display: RegistryDisplay::default(),
            paths: DeterministicMap::new(),
        };
        let mut registry = Registry::default();
        let mut mso_mdoc = DeterministicMap::new();
        mso_mdoc.insert(
            "doc_a".to_string(),
            vec![
                rc("a1", DelegationType::Full),
                rc("a2", DelegationType::None),
                rc("a3", DelegationType::Full),
            ],
        );
        mso_mdoc.insert("doc_b".to_string(), vec![rc("b", DelegationType::None)]);
        mso_mdoc.insert("doc_e".to_string(), vec![rc("e", DelegationType::Full)]);
        registry.credentials.mso_mdoc = Some(mso_mdoc);

        let cred = |id: &str| DcqlCredential {
            id: id.to_string(),
            format: "mso_mdoc".to_string(),
            meta: Some(DcqlMeta {
                doctype_value: format!("doc_{}", id),
                vct_values: vec![],
            }),
            claims: vec![],
            claim_sets: vec![],
        };
        let set = |options: &[&[&str]], required: bool| DcqlCredentialSet {
            options: options
                .iter()
                .map(|o| o.iter().map(|s| s.to_string()).collect())
                .collect(),
            required: Some(required),
        };
        let query = DcqlQuery {
            credentials: vec![cred("a"), cred("b"), cred("c"), cred("d"), cred("e"), cred("f")],
            credential_sets: vec![
                set(&[&["a", "b"]], true),
                set(&[&["c"], &["e", "f"]], true),
                set(&[&["d"]], false),
            ],
        };

        let result = dcql_query(&query, &registry);
        let mut mock = new_mock(7);
        report_match_result(&mut mock, &result, 0, &OpenId4VpData::default(), &[]).unwrap();

        // Two entry sets, one per option of set 1. Both are partial (this provider holds no
        // c or f), so only its FULL entries are reported: a1 and a3 as alternatives for slot
        // 0, plus e in the second set. a2 and b are withheld; c, f (and b) are left for other
        // providers.
        assert_eq!(
            mock.added_entry_sets,
            vec![
                ("req:0;set:0;option:0;set:1;option:0".to_string(), 3),
                ("req:0;set:0;option:0;set:1;option:1".to_string(), 4),
            ]
        );
        assert_eq!(
            mock.added_entry_indices,
            vec![
                ("a1".to_string(), 0),
                ("a3".to_string(), 0),
                ("a1".to_string(), 0),
                ("a3".to_string(), 0),
                ("e".to_string(), 2),
            ]
        );
        assert!(mock
            .added_delegations
            .iter()
            .all(|(_, d)| *d == DelegationType::Full));
    }

    #[test]
    fn test_report_partial_option_offsets_subsequent_sets() {
        let mut mock = MockCredman {
            wasm_version: 7,
            added_entries: vec![],
            added_delegations: vec![],
            added_fields: vec![],
            payment_entries: vec![],
            inline_entries: vec![],
            added_entry_sets: vec![],
            added_entry_indices: vec![],
        };
        let display = RegistryDisplay::default();
        let mut matched_credentials = DeterministicMap::new();
        matched_credentials.insert("a", full_delegation_entry("a", &display));
        matched_credentials.insert("c", full_delegation_entry("c", &display));

        // Set 0 option 0 = ["a", "b"] (partial: only "a"), set 1 option 0 = ["c"] (complete).
        let match_result = DcqlMatchResult {
            matched_credential_sets: vec![
                vec![MatchedCredentialSetInfo {
                    set_id: Cow::Borrowed("0"),
                    option_id: Cow::Borrowed("0"),
                    matched_credential_ids: vec!["a"],
                    matched_indices: vec![0],
                    option_length: 2,
                }],
                vec![MatchedCredentialSetInfo {
                    set_id: Cow::Borrowed("1"),
                    option_id: Cow::Borrowed("0"),
                    matched_credential_ids: vec!["c"],
                    matched_indices: vec![0],
                    option_length: 1,
                }],
            ],
            matched_credentials,
            inline_issuance: None,
        };

        report_match_result(&mut mock, &match_result, 0, &OpenId4VpData::default(), &[]).unwrap();

        assert_eq!(
            mock.added_entry_sets,
            vec![("req:0;set:0;option:0;set:1;option:0".to_string(), 3)]
        );
        // "c" is at index 2 because the (partially matched) first option occupies indices 0..2.
        assert_eq!(
            mock.added_entry_indices,
            vec![("a".to_string(), 0), ("c".to_string(), 2)]
        );
    }
}
