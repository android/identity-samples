use crate::base64url::decode_base64url;
use crate::credman::CredmanApi;
use crate::json_value::JsonValue;
pub use crate::openid4vp_models::*;
use crate::reporter::report_match_result;
use nanoserde::DeJson;
use std::borrow::Cow;

fn extract_multisigned_payload<'a>(
    pr: &'a ProtocolRequest,
) -> Result<String, Box<dyn std::error::Error>> {
    let json_str: &str = if let Some(data) = &pr.data {
        match data {
            ProtocolRequestData::String(s) => s.as_str(),
            ProtocolRequestData::Object(obj) => obj.request.as_str(),
        }
    } else if !pr.request.is_empty() {
        pr.request.as_str()
    } else {
        return Err("Missing multisigned request data".into());
    };

    let parsed: JsonValue = DeJson::deserialize_json(json_str)?;

    let payload = match &parsed {
        JsonValue::Object(map) => {
            if let Some(JsonValue::Object(req_map)) = map.get("request") {
                if let Some(JsonValue::String(p)) = req_map.get("payload") {
                    p.clone()
                } else {
                    return Err("Missing 'payload' in 'request' object".into());
                }
            } else if let Some(JsonValue::String(p)) = map.get("payload") {
                p.clone()
            } else {
                return Err("Missing 'payload' field in multisigned request".into());
            }
        }
        _ => return Err("Multisigned request must be a JSON object".into()),
    };

    if payload.is_empty() {
        return Err("Empty payload in multisigned request".into());
    }

    Ok(payload)
}

fn parse_protocol_request_data<'a>(
    pr: &'a ProtocolRequest,
) -> Result<Cow<'a, OpenId4VpData>, Box<dyn std::error::Error>> {
    if pr.protocol == "openid4vp-v1-signed" {
        log::debug!("Handling signed OpenID4VP request");
        let jws: &'a str = if let Some(data) = &pr.data {
            match data {
                ProtocolRequestData::String(s) => s,
                ProtocolRequestData::Object(obj) => {
                    if obj.request.is_empty() {
                        return Err("Missing 'request' field in signed data object".into());
                    }
                    &obj.request
                }
            }
        } else if !pr.request.is_empty() {
            &pr.request
        } else {
            return Err("Missing signed request data".into());
        };

        let parts: Vec<&str> = jws.split('.').collect();
        if parts.len() < 2 {
            log::error!("Invalid JWS parts");
            return Err("Invalid JWS".into());
        }

        let decoded = decode_base64url(parts[1])?;
        return Ok(Cow::Owned(DeJson::deserialize_json(std::str::from_utf8(
            &decoded,
        )?)?));
    } else if pr.protocol == "openid4vp-v1-multisigned" {
        log::debug!("Handling multisigned OpenID4VP request");
        let payload_str = extract_multisigned_payload(pr)?;
        let decoded = decode_base64url(&payload_str)?;
        return Ok(Cow::Owned(DeJson::deserialize_json(std::str::from_utf8(
            &decoded,
        )?)?));
    } 

    log::debug!("Handling unsigned OpenID4VP request");
    if let Some(data) = &pr.data {
        return match data {
            ProtocolRequestData::Object(obj) => Ok(Cow::Borrowed(obj)),
            ProtocolRequestData::String(s) => Ok(Cow::Owned(DeJson::deserialize_json(s)?)),
        };
    }

    if !pr.request.is_empty() {
        return Ok(Cow::Owned(DeJson::deserialize_json(&pr.request)?));
    }

    Err("Missing unsigned request data".into())
}

pub fn openid4vp_main(credman: &mut impl CredmanApi) -> Result<(), Box<dyn std::error::Error>> {
    log::info!("Starting OpenID4VP matching process");
    let matcher_data_buffer = credman.get_registered_data();
    if matcher_data_buffer.len() < 4 {
        log::error!(
            "Matcher data buffer is too small: {}",
            matcher_data_buffer.len()
        );
        return Err("Matcher data too small".into());
    }

    let json_start = u32::from_le_bytes(matcher_data_buffer[..4].try_into()?);
    log::debug!("Registry JSON starts at offset: {}", json_start);
    let matcher_data_str = std::str::from_utf8(&matcher_data_buffer[json_start as usize..])?;
    log::debug!("Registry JSON: {}", matcher_data_str);
    let registry: Registry = match DeJson::deserialize_json(matcher_data_str) {
        Ok(data) => data,
        Err(e) => {
            log::error!(
                "Failed to deserialize registry: {:?}. JSON snippet: {}",
                e,
                &matcher_data_str[..std::cmp::min(100, matcher_data_str.len())]
            );
            return Err(e.into());
        }
    };
    log::info!("Successfully parsed registry");

    let request_buffer = credman.get_request_buffer();
    let request_str = std::str::from_utf8(&request_buffer)?;
    log::debug!("Request JSON: {}", request_str);
    let request: OpenId4VpRequest = match DeJson::deserialize_json(request_str) {
        Ok(req) => req,
        Err(e) => {
            log::error!(
                "Failed to deserialize request: {:?}. JSON: {}",
                e,
                request_str
            );
            return Err(e.into());
        }
    };

    let protocol_requests = if !request.requests.is_empty() {
        request.requests
    } else {
        request.providers
    };
    log::info!("Found {} protocol requests", protocol_requests.len());

    for target_protocol in &registry.supported_protocols {
        log::debug!("Matching requests for protocol={}", target_protocol);
        for (i, pr) in protocol_requests.iter().enumerate() {
            if pr.protocol != *target_protocol {
                continue;
            }
            log::debug!("Processing request {}: protocol={}", i, pr.protocol);
            if pr.protocol != "openid4vp-v1-unsigned"
                && pr.protocol != "openid4vp-v1-signed"
                && pr.protocol != "openid4vp-v1-multisigned"
            {
                log::warn!("Unsupported protocol: {}", pr.protocol);
                continue;
            }

            let data_json = parse_protocol_request_data(pr)?;
            let query = data_json.dcql_query.as_ref().ok_or("Missing dcql_query")?;
            let match_result = crate::dcql::dcql_query(query, &registry);

            let has_matches = !match_result.matched_credential_sets.is_empty()
                || match_result.inline_issuance.is_some();
            if has_matches {
                // If there are multiple requests of the same preferred protocol,
                // we only process the first request that yields a match.
                report_match_result(credman, &match_result, i, &data_json, &matcher_data_buffer)?;
                return Ok(());
            }
        }
    }

    log::info!("OpenID4VP matching process completed");
    Ok(())
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_parse_protocol_request_data_unsigned() {
        let pr = ProtocolRequest {
            protocol: "openid4vp-v1-unsigned".to_string(),
            data: Some(ProtocolRequestData::String(
                r#"{"request":"test"}"#.to_string(),
            )),
            request: "".to_string(),
        };
        let data = parse_protocol_request_data(&pr).unwrap();
        assert_eq!(data.request, "test");
    }

    #[test]
    fn test_parse_protocol_request_data_invalid_jws() {
        let pr = ProtocolRequest {
            protocol: "openid4vp-v1-signed".to_string(),
            data: Some(ProtocolRequestData::String("invalid.jws!".to_string())),
            request: "".to_string(),
        };
        let err = parse_protocol_request_data(&pr).unwrap_err();
        assert!(err.to_string().contains("Invalid character"));
    }

    #[test]
    fn test_parse_protocol_request_data_missing_data() {
        let pr = ProtocolRequest {
            protocol: "openid4vp-v1-unsigned".to_string(),
            data: None,
            request: "".to_string(),
        };
        let err = parse_protocol_request_data(&pr).unwrap_err();
        assert!(err.to_string().contains("Missing unsigned request data"));
    }

    #[test]
    fn test_parse_protocol_request_data_multisigned_valid() {
        let json = r#"{
            "protocol": "openid4vp-v1-multisigned",
            "data": "{\"request\": {\"payload\": \"eyJkY3FsX3F1ZXJ5Ijp7ImNyZWRlbnRpYWxzIjpbXX19\"}}"
        }"#;
        let pr: ProtocolRequest = DeJson::deserialize_json(json).unwrap();
        let data = parse_protocol_request_data(&pr).unwrap();
        assert!(data.dcql_query.is_some());
    }

    #[test]
    fn test_parse_protocol_request_data_multisigned_invalid_payload() {
        let json = r#"{
            "protocol": "openid4vp-v1-multisigned",
            "data": "{\"request\": {}}"
        }"#;
        let pr: ProtocolRequest = DeJson::deserialize_json(json).unwrap();
        let err = parse_protocol_request_data(&pr).unwrap_err();
        assert!(err.to_string().contains("Missing 'payload'"));
    }

    use crate::test_utils::*;

    macro_rules! define_test {
        ($func_name:ident, $test_name:expr) => {
            #[test]
            fn $func_name() {
                run_openid4vp_test($test_name, None);
            }
        };
    }

    define_test!(tc07_mdoc_match, "TC07_MdocMatch");
    define_test!(tc08_mdoc_mismatch, "TC08_MdocMismatch");
    define_test!(tc09_sdjwt_match, "TC09_SdjwtMatch");
    define_test!(tc10_sdjwt_mismatch, "TC10_SdjwtMismatch");
    define_test!(tc11_inline_issuance_fallback, "TC11_InlineIssuanceFallback");
    define_test!(tc12_missing_format, "TC12_MissingFormat");
    define_test!(tc13_return_all_claims, "TC13_ReturnAllClaims");
    define_test!(tc14_match_specific_claims, "TC14_MatchSpecificClaims");
    define_test!(tc15_match_nested_claims, "TC15_MatchNestedClaims");
    define_test!(tc16_fail_missing_claims, "TC16_FailMissingClaims");
    define_test!(tc17_match_claim_values_bool, "TC17_MatchClaimValuesBool");
    define_test!(tc18_fail_claim_values_bool, "TC18_FailClaimValuesBool");
    define_test!(tc19_match_claim_values_int, "TC19_MatchClaimValuesInt");
    define_test!(tc20_fail_claim_values_int, "TC20_FailClaimValuesInt");
    define_test!(tc21_match_first_claim_set, "TC21_MatchFirstClaimSet");
    define_test!(tc22_match_second_claim_set, "TC22_MatchSecondClaimSet");
    define_test!(tc23_fail_all_claim_sets, "TC23_FailAllClaimSets");
    define_test!(tc24_dcql_query_single, "TC24_DcqlQuerySingle");
    define_test!(tc25_dcql_query_set_match, "TC25_DcqlQuerySetMatch");
    define_test!(
        tc26_dcql_query_set_fail_required,
        "TC26_DcqlQuerySetFailRequired"
    );
    define_test!(
        tc27_dcql_query_set_fail_optional,
        "TC27_DcqlQuerySetFailOptional"
    );
    define_test!(
        tc28_dcql_query_complex_overlapping_sets,
        "TC28_DcqlQueryComplexOverlappingSets"
    );
    define_test!(
        tc29_dcql_query_openid4vp_spec_example,
        "TC29_DcqlQueryOpenID4VPSpecExample"
    );
    define_test!(tc30_parse_v1_unsigned, "TC30_ParseV1Unsigned");
    define_test!(tc31_parse_v1_signed, "TC31_ParseV1Signed");
    define_test!(tc42_parse_v1_multisigned, "TC42_ParseV1Multisigned");
    define_test!(tc32_extract_payment_sca1, "TC32_ExtractPaymentSca1");
    define_test!(tc33_extract_payment_details, "TC33_ExtractPaymentDetails");
    define_test!(tc34_extract_payment_generic, "TC34_ExtractPaymentGeneric");
    define_test!(tc35_wasm_add_entry_to_set, "TC35_WasmAddEntryToSet");
    define_test!(tc36_wasm_payment_v2, "TC36_WasmPaymentV2");
    define_test!(tc38_dcql_cartesian_product, "TC38_DcqlCartesianProduct");
    define_test!(
        tc39_dcql_complex_cartesian_product,
        "TC39_DcqlComplexCartesianProduct"
    );

    #[test]
    fn tc37_wasm_metadata_text() {
        let manifest_dir = std::env::var("CARGO_MANIFEST_DIR").unwrap();
        let testdata_dir = std::path::PathBuf::from(manifest_dir).join("testdata");
        let mut registry_json =
            std::fs::read_to_string(testdata_dir.join("registry.json")).unwrap();
        let target = "\"title\": \"John's Driving License\"";
        if let Some(pos) = registry_json.find(target) {
            registry_json.insert_str(pos, "\"metadata_display_text\": \"Verified Member\", ");
        }
        run_openid4vp_test("TC37_WasmMetadataText", Some(&registry_json));
    }

    /// Replaces the `"supported_protocols": [...]` array inside a JSON registry string
    /// with the provided slice of protocol strings.
    fn replace_supported_protocols(registry_json: &str, protocols: &[&str]) -> String {
        let start_key = "\"supported_protocols\": [";
        if let (Some(start_idx), Some(end_rel)) = (
            registry_json.find(start_key),
            registry_json.find(start_key).and_then(|idx| registry_json[idx..].find("],")),
        ) {
            let end_idx = start_idx + end_rel + 2;
            let formatted_protocols = protocols
                .iter()
                .map(|p| format!("    \"{}\"", p))
                .collect::<Vec<_>>()
                .join(",\n");
            let new_section = format!("\"supported_protocols\": [\n{}\n  ],", formatted_protocols);
            let mut result = String::with_capacity(registry_json.len() + new_section.len());
            result.push_str(&registry_json[..start_idx]);
            result.push_str(&new_section);
            result.push_str(&registry_json[end_idx..]);
            result
        } else {
            registry_json.to_string()
        }
    }

    #[test]
    fn tc40_match_most_preferred_supported_protocol_available() {
        let manifest_dir = std::env::var("CARGO_MANIFEST_DIR").unwrap();
        let testdata_dir = std::path::PathBuf::from(manifest_dir).join("testdata");
        let registry_json = std::fs::read_to_string(testdata_dir.join("registry.json")).unwrap();

        // Inject custom supported_protocols preference order: signed then unsigned
        let modified_registry = replace_supported_protocols(
            &registry_json,
            &["openid4vp-v1-signed", "openid4vp-v1-unsigned"],
        );

        run_openid4vp_test(
            "TC40_MatchMostPreferredSupportedProtocolAvailable",
            Some(&modified_registry),
        );
    }

    #[test]
    fn tc41_no_match_if_no_requests_for_supported_protocols() {
        let manifest_dir = std::env::var("CARGO_MANIFEST_DIR").unwrap();
        let testdata_dir = std::path::PathBuf::from(manifest_dir).join("testdata");
        let registry_json = std::fs::read_to_string(testdata_dir.join("registry.json")).unwrap();

        // Inject custom supported_protocols containing only "openid4vp-v1-signed"
        let modified_registry = replace_supported_protocols(&registry_json, &["openid4vp-v1-signed"]);

        run_openid4vp_test(
            "TC41_NoMatchIfNoRequestsForSupportedProtocols",
            Some(&modified_registry),
        );
    }
}
