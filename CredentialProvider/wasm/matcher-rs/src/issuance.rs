use crate::{
    credman::CredmanApi,
    issuance_matcher::IssuanceMatcherData,
    openid4vci::{DigitalCredentialCreationRequest, RegularizedOpenId4VciRequestData},
};

use nanoserde::{DeJson, SerJson};

#[derive(SerJson)]
struct IssuanceMetadata {
    eidx: usize,
}

const ALLOWED_PROTOCOLS: [&str; 4] = [
    "openid4vci-1.0",
    "openid4vci1.0",
    "openid4vci-1.1",
    "openid4vci1.1",
];

fn is_protocol_allowed(protocol: &String, configured: &[String]) -> bool {
    if configured.is_empty() {
        ALLOWED_PROTOCOLS.contains(&protocol.as_str())
    } else {
        configured.contains(protocol)
    }
}

pub fn issuance_main(credman: &mut impl CredmanApi) -> Result<(), Box<dyn std::error::Error>> {
    log::info!("Starting issuance matching process");
    let matcher_data_buffer = credman.get_registered_data();
    log::debug!(
        "Retrieved matcher data buffer, size: {}",
        matcher_data_buffer.len()
    );

    let json_start = u32::from_le_bytes(matcher_data_buffer[..size_of::<u32>()].try_into()?);
    let matcher_data_str = std::str::from_utf8(&matcher_data_buffer[json_start.try_into()?..])?;
    let matcher_data: IssuanceMatcherData = match DeJson::deserialize_json(matcher_data_str) {
        Ok(data) => data,
        Err(e) => {
            log::error!(
                "Failed to deserialize matcher data: {:?}. JSON: {}",
                e,
                matcher_data_str
            );
            return Err(e.into());
        }
    };
    log::debug!("Parsed matcher data for entry: {}", matcher_data.entry_id);

    let request_buffer = credman.get_request_buffer();
    let request_str = std::str::from_utf8(&request_buffer)?;
    let request: DigitalCredentialCreationRequest = match DeJson::deserialize_json(request_str) {
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
    log::debug!(
        "Parsed request with {} sub-requests",
        request.requests.len()
    );

    for (i, r) in request.requests.iter().enumerate() {
        log::trace!("Checking request {}: protocol={}", i, r.protocol);
        if is_protocol_allowed(&r.protocol, &matcher_data.allowed_protocols) {
            let regularized = RegularizedOpenId4VciRequestData::from(&r.data);
            if matcher_data.filter.matches(&regularized) {
                log::info!("Match found for request {} with protocol {}", i, r.protocol);
                let version = credman.get_wasm_version();
                for (index, entry) in matcher_data.entries.iter().enumerate() {
                    let entry_id = format!("{}_{}", matcher_data.entry_id, index);
                    let icon = &matcher_data_buffer[entry.icon.0..entry.icon.1];
                    if version >= 9 {
                        log::debug!("Adding issuance entry (v>=9): {}", entry_id);
                        let metadata = SerJson::serialize_json(&IssuanceMetadata { eidx: index });
                        credman.add_issuance_entry(
                            &entry_id,
                            icon,
                            &entry.title,
                            &entry.subtitle,
                            "",
                            &metadata,
                        );
                    } else {
                        log::debug!("Adding string ID entry (v<9): {}", entry_id);
                        credman.add_string_id_entry(
                            &entry_id,
                            icon,
                            &entry.title,
                            &entry.subtitle,
                            "",
                            "",
                        );
                    }
                }
                // Assuming we only need to add one entry if any request matches
                break;
            }
        } else {
            log::warn!("Unsupported protocol: {}", r.protocol);
        }
    }

    log::info!("Issuance matching process completed");
    Ok(())
}

#[cfg(test)]
mod test {
    use super::*;
    use std::ffi::{CStr, CString};

    #[derive(Debug, PartialEq, Clone, Copy)]
    enum CallType {
        StringId,
        Issuance,
    }

    struct AddedEntry {
        entry_id: CString,
        icon: Option<Vec<u8>>,
        title: Option<CString>,
        subtitle: Option<CString>,
        disclaimer: Option<CString>,
        warning: Option<CString>,
        explainer: Option<CString>,
        metadata: Option<CString>,
        call_type: CallType,
    }

    struct FakeCredman {
        request_json: &'static str,
        registered_json: &'static str,
        icon: Vec<u8>,
        added_entries: Vec<AddedEntry>,
        wasm_version: u32,
    }

    impl CredmanApi for FakeCredman {
        fn get_request_buffer(&self) -> Vec<u8> {
            self.request_json.as_bytes().into()
        }

        fn get_registered_data(&self) -> Vec<u8> {
            let mut result = Vec::with_capacity(4 + self.icon.len() + self.registered_json.len());
            result.extend_from_slice(&u32::to_le_bytes(4 + self.icon.len() as u32));
            result.extend_from_slice(&self.icon);
            result.extend_from_slice(self.registered_json.as_bytes());
            result
        }

        fn get_wasm_version(&self) -> u32 {
            self.wasm_version
        }
        fn add_string_id_entry(
            &mut self,
            entry_id: &str,
            icon: &[u8],
            title: &str,
            subtitle: &str,
            disclaimer: &str,
            warning: &str,
        ) {
            self.added_entries.push(AddedEntry {
                entry_id: CString::new(entry_id).unwrap(),
                icon: if icon.is_empty() {
                    None
                } else {
                    Some(icon.to_vec())
                },
                title: if title.is_empty() {
                    None
                } else {
                    Some(CString::new(title).unwrap())
                },
                subtitle: if subtitle.is_empty() {
                    None
                } else {
                    Some(CString::new(subtitle).unwrap())
                },
                disclaimer: if disclaimer.is_empty() {
                    None
                } else {
                    Some(CString::new(disclaimer).unwrap())
                },
                warning: if warning.is_empty() {
                    None
                } else {
                    Some(CString::new(warning).unwrap())
                },
                explainer: None,
                metadata: None,
                call_type: CallType::StringId,
            });
        }
        fn add_issuance_entry(
            &mut self,
            entry_id: &str,
            icon: &[u8],
            title: &str,
            subtitle: &str,
            explainer: &str,
            metadata: &str,
        ) {
            self.added_entries.push(AddedEntry {
                entry_id: CString::new(entry_id).unwrap(),
                icon: if icon.is_empty() {
                    None
                } else {
                    Some(icon.to_vec())
                },
                title: if title.is_empty() {
                    None
                } else {
                    Some(CString::new(title).unwrap())
                },
                subtitle: if subtitle.is_empty() {
                    None
                } else {
                    Some(CString::new(subtitle).unwrap())
                },
                disclaimer: None,
                warning: None,
                explainer: if explainer.is_empty() {
                    None
                } else {
                    Some(CString::new(explainer).unwrap())
                },
                metadata: if metadata.is_empty() {
                    None
                } else {
                    Some(CString::new(metadata).unwrap())
                },
                call_type: CallType::Issuance,
            });
        }
        fn add_entry_set(&mut self, _set_id: &str, _set_length: i32) {}
        fn add_entry_to_set(
            &mut self,
            _cred_id: &str,
            _icon: &[u8],
            _title: &str,
            _subtitle: &str,
            _disclaimer: &str,
            _warning: &str,
            _metadata: &str,
            _set_id: &str,
            _set_index: i32,
        ) {
        }
        fn add_field_to_entry_set(
            &mut self,
            _cred_id: &str,
            _field_display_name: &str,
            _field_display_value: &str,
            _set_id: &str,
            _set_index: i32,
        ) {
        }
        fn add_payment_entry_to_set_v2(
            &mut self,
            _cred_id: &str,
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
        ) {
        }
        fn add_inline_issuance_entry(
            &mut self,
            _cred_id: &str,
            _icon: &[u8],
            _title: &str,
            _subtitle: &str,
        ) {
        }
        fn add_metadata_display_text_to_entry_set(
            &mut self,
            _cred_id: &str,
            _metadata_display_text: &str,
            _set_id: &str,
            _set_index: i32,
        ) {
        }
    }

    #[test]
    fn match_case1() {
        let mut credman = FakeCredman {
            request_json: r#"
{
  "requests": [
    {
      "protocol": "openid4vci-1.1",
      "data": {
        "credential_issuer": "https://issuer.my",
        "credential_configuration_ids": [
          "US_SOCIAL_SECURITY_NUMBER"
        ],
        "grants": {
          "authorization_code": {}
        },
        "credential_issuer_metadata": {
          "nonce_endpoint": "https://nonce.my"
        }
      }
    }
  ]
}"#,
            registered_json: r#"
      {
        "entry_id": "C",
        "entries": [
          {
            "title": "TTTT",
            "subtitle": "SSSSS",
            "icon": [0, 0]
          }
        ],
        "filter": {
          "And": {
            "filters": [{
              "AllowedConfigurationIds": {
                "configuration_ids": ["US_SOCIAL_SECURITY_NUMBER", "EU_AGE"]
              }
            }, {
              "AllowedIssuers": {
                "issuers": ["ccb", "https://issuer.my"]
              }
            }]
          }
        }
      }"#,
            icon: Vec::new(),
            added_entries: Vec::new(),
            wasm_version: 1,
        };

        issuance_main(&mut credman).unwrap();

        assert_eq!(credman.added_entries.len(), 1);
        let entry = &credman.added_entries[0];
        assert_eq!(entry.entry_id, c"C_0");
        assert_eq!(entry.title.as_ref().unwrap(), c"TTTT");
        assert_eq!(entry.subtitle.as_ref().unwrap(), c"SSSSS");
        assert!(entry.icon.is_none());
        assert_eq!(entry.call_type, CallType::StringId);
    }

    #[test]
    fn invalid_json() {
        let mut credman = FakeCredman {
            request_json: r#"
{
  "requests": [
    {
      "protocol": "openid4vci-1.1",
      "data": {
        "credential_issuer": "https://issuer.my",
        "credential_configuration_ids": [
          "US_SOCIAL_SECURITY_NUMBER"
        ],
        "grants": {
          "authorization_code": {}
        },
        "credential_issuer_metadata": {
          "nonce_endpoint": "https://nonce.my"
        }
      }
    }
  ]
"#,
            registered_json: r#"
      {
        "entry_id": "C",
        "entries": [
          {
            "title": "TTTT",
            "subtitle": "SSSSS",
            "icon": [0, 0]
          }
        ],
        "filter": {"Pass": {}}"#,
            icon: Vec::new(),
            added_entries: Vec::new(),
            wasm_version: 1,
        };

        let errmsg = format!("{:?}", issuance_main(&mut credman).unwrap_err());
        assert!(
            errmsg.contains("Unexpected token Eof") || errmsg.contains("Unexpected end of file")
        );
    }

    #[test]
    fn nomatch_case1() {
        let mut credman = FakeCredman {
            request_json: r#"
{
  "requests": [
    {
      "protocol": "openid4vci-1.1",
      "data": {
        "credential_issuer": "https://issuer.my",
        "credential_configuration_ids": [
          "US_SOCIAL_SECURITY_NUMBER"
        ],
        "grants": {
          "authorization_code": {}
        },
        "credential_issuer_metadata": {
          "nonce_endpoint": "https://nonce.my"
        }
      }
    }
  ]
}"#,
            registered_json: r#"
{
  "entry_id": "C",
  "entries": [
    {
      "title": "TTTT",
      "subtitle": "SSSSS",
      "icon": [
        0,
        0
      ]
    }
  ],
  "filter": {
    "And": {
      "filters": [
        {
          "AllowedConfigurationIds": {
            "configuration_ids": [
              "US_SOCIAL_SECURITY_NUMBER",
              "EU_AGE"
            ]
          }
        },
        {
          "AllowedIssuers": {
            "issuers": [
              "ccb",
              "https://issuer.my"
            ]
          }
        },
        {
          "Not": {
            "filter": {
              "Pass": {
              }
            }
          }
        }
      ]
    }
  }
}"#,
            icon: Vec::new(),
            added_entries: Vec::new(),
            wasm_version: 1,
        };

        issuance_main(&mut credman).unwrap();

        assert_eq!(credman.added_entries.len(), 0);
    }

    #[test]
    fn match_mdoc_doctype() {
        let mut credman = FakeCredman {
            request_json: r#"
{
  "requests": [
    {
      "protocol": "openid4vci-1.1",
      "data": {
        "credential_issuer": "https://issuer.my",
        "credential_configuration_ids": [
          "FICTITIOUS_STATE_MDL"
        ],
        "grants": {
          "authorization_code": {}
        },
        "credential_issuer_metadata": {
          "nonce_endpoint": "https://nonce.my",
          "credential_configurations_supported": {
            "FICTITIOUS_STATE_MDL": {
              "format": "mso_mdoc",
              "doctype": "org.iso.18013.5.1.mDL"
            }
          }
        }
      }
    }
  ]
}"#,
            registered_json: r#"
{
  "entry_id": "C",
  "entries": [
    {
      "title": "TTTT",
      "subtitle": "SSSSS",
      "icon": [
        0,
        0
      ]
    }
  ],
  "filter": {
    "Or": {
      "filters": [
        {
          "AllowedConfigurationIds": {
            "configuration_ids": [
              "US_SOCIAL_SECURITY_NUMBER",
              "EU_AGE"
            ]
          }
        },
        {
          "AllowedIssuers": {
            "issuers": [
              "ccb"
            ]
          }
        },
        {
          "AllowedMdocDoctypes": {
            "doctypes": [
              "org.iso.18013.5.1.mDL"
            ]
          }
        }
      ]
    }
  }
}"#,
            icon: Vec::new(),
            added_entries: Vec::new(),
            wasm_version: 1,
        };

        issuance_main(&mut credman).unwrap();

        assert_eq!(credman.added_entries.len(), 1);
    }

    #[test]
    fn match_custom_protocol() {
        let mut credman = FakeCredman {
            request_json: r#"
{
  "requests": [
    {
      "protocol": "my-custom-protocol",
      "data": {
        "credential_issuer": "https://issuer.my",
        "credential_configuration_ids": [
          "US_SOCIAL_SECURITY_NUMBER"
        ],
        "grants": {
          "authorization_code": {}
        },
        "credential_issuer_metadata": {
          "nonce_endpoint": "https://nonce.my"
        }
      }
    }
  ]
}"#,
            registered_json: r#"
      {
        "entry_id": "C",
        "entries": [
          {
            "title": "TTTT",
            "subtitle": "SSSSS",
            "icon": [0, 0]
          }
        ],
        "allowed_protocols": ["my-custom-protocol"],
        "filter": {
          "Pass": {}
        }
      }"#,
            icon: Vec::new(),
            added_entries: Vec::new(),
            wasm_version: 1,
        };

        issuance_main(&mut credman).unwrap();

        assert_eq!(credman.added_entries.len(), 1);
        assert_eq!(credman.added_entries[0].call_type, CallType::StringId);
        assert_eq!(credman.added_entries[0].entry_id, c"C_0");
    }

    #[test]
    fn nomatch_default_protocol_when_custom_configured() {
        let mut credman = FakeCredman {
            request_json: r#"
{
  "requests": [
    {
      "protocol": "openid4vci-1.1",
      "data": {
        "credential_issuer": "https://issuer.my",
        "credential_configuration_ids": [
          "US_SOCIAL_SECURITY_NUMBER"
        ],
        "grants": {
          "authorization_code": {}
        },
        "credential_issuer_metadata": {
          "nonce_endpoint": "https://nonce.my"
        }
      }
    }
  ]
}"#,
            registered_json: r#"
      {
        "entry_id": "C",
        "entries": [
          {
            "title": "TTTT",
            "subtitle": "SSSSS",
            "icon": [0, 0]
          }
        ],
        "allowed_protocols": ["my-custom-protocol"],
        "filter": {
          "Pass": {}
        }
      }"#,
            icon: Vec::new(),
            added_entries: Vec::new(),
            wasm_version: 1,
        };

        issuance_main(&mut credman).unwrap();

        assert_eq!(credman.added_entries.len(), 0);
    }

    #[test]
    fn match_case_v9() {
        let mut credman = FakeCredman {
            request_json: r#"
{
  "requests": [
    {
      "protocol": "openid4vci-1.1",
      "data": {
        "credential_issuer": "https://issuer.my",
        "credential_configuration_ids": [
          "US_SOCIAL_SECURITY_NUMBER"
        ],
        "grants": {
          "authorization_code": {}
        },
        "credential_issuer_metadata": {
          "nonce_endpoint": "https://nonce.my"
        }
      }
    }
  ]
}"#,
            registered_json: r#"
      {
        "entry_id": "C",
        "entries": [
          {
            "title": "TTTT",
            "subtitle": "SSSSS",
            "icon": [0, 0]
          }
        ],
        "filter": {
          "And": {
            "filters": [{
              "AllowedConfigurationIds": {
                "configuration_ids": ["US_SOCIAL_SECURITY_NUMBER", "EU_AGE"]
              }
            }, {
              "AllowedIssuers": {
                "issuers": ["ccb", "https://issuer.my"]
              }
            }]
          }
        }
      }"#,
            icon: Vec::new(),
            added_entries: Vec::new(),
            wasm_version: 9,
        };

        issuance_main(&mut credman).unwrap();

        assert_eq!(credman.added_entries.len(), 1);
        let entry = &credman.added_entries[0];
        assert_eq!(entry.entry_id, c"C_0");
        assert_eq!(entry.title.as_ref().unwrap(), c"TTTT");
        assert_eq!(entry.subtitle.as_ref().unwrap(), c"SSSSS");
        assert!(entry.icon.is_none());
        assert_eq!(entry.call_type, CallType::Issuance);
        assert!(entry.explainer.is_none());
        assert_eq!(entry.metadata.as_ref().unwrap(), c"{\"eidx\":0}");
    }

    #[test]
    fn match_multiple_entries() {
        let mut credman = FakeCredman {
            request_json: r#"
{
  "requests": [
    {
      "protocol": "openid4vci-1.1",
      "data": {
        "credential_issuer": "https://issuer.my",
        "credential_configuration_ids": [
          "US_SOCIAL_SECURITY_NUMBER"
        ],
        "grants": {
          "authorization_code": {}
        },
        "credential_issuer_metadata": {
          "nonce_endpoint": "https://nonce.my"
        }
      }
    }
  ]
}"#,
            registered_json: r#"
      {
        "entry_id": "C",
        "entries": [
          {
            "title": "TTTT1",
            "subtitle": "SSSSS1",
            "icon": [0, 0]
          },
          {
            "title": "TTTT2",
            "subtitle": "SSSSS2",
            "icon": [0, 0]
          }
        ],
        "filter": {
          "Pass": {}
        }
      }"#,
            icon: Vec::new(),
            added_entries: Vec::new(),
            wasm_version: 9,
        };

        issuance_main(&mut credman).unwrap();

        assert_eq!(credman.added_entries.len(), 2);
        assert_eq!(credman.added_entries[0].entry_id, c"C_0");
        assert_eq!(credman.added_entries[0].title.as_ref().unwrap(), c"TTTT1");
        assert_eq!(credman.added_entries[0].metadata.as_ref().unwrap(), c"{\"eidx\":0}");
        assert_eq!(credman.added_entries[1].entry_id, c"C_1");
        assert_eq!(credman.added_entries[1].title.as_ref().unwrap(), c"TTTT2");
        assert_eq!(credman.added_entries[1].metadata.as_ref().unwrap(), c"{\"eidx\":1}");
    }
}
