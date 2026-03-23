use std::ffi::CString;

use crate::{
    credman::CredmanApi,
    issuance_matcher::IssuanceMatcherData,
    openid4vci::{DigitalCredentialCreationRequest, RegularizedOpenId4VciRequestData},
};

use nanoserde::DeJson;

const ALLOWED_PROTOCOLS: [&str; 4] = [
    "openid4vci-1.0",
    "openid4vci1.0",
    "openid4vci-1.1",
    "openid4vci1.1",
];

pub fn issuance_main(credman: &mut impl CredmanApi) -> Result<(), Box<dyn std::error::Error>> {
    let matcher_data_buffer = credman.get_registered_data();
    let json_start = u32::from_le_bytes(matcher_data_buffer[..size_of::<u32>()].try_into()?);
    let matcher_data: IssuanceMatcherData = DeJson::deserialize_json(std::str::from_utf8(
        &matcher_data_buffer[json_start.try_into()?..],
    )?)?;
    let request: DigitalCredentialCreationRequest =
        DeJson::deserialize_json(std::str::from_utf8(&credman.get_request_buffer())?)?;
    if request.requests.iter().any(|r| {
        ALLOWED_PROTOCOLS.iter().any(|s| r.protocol == *s)
            && matcher_data
                .filter
                .matches(&RegularizedOpenId4VciRequestData::from(&r.data))
    }) {
        let icon = &matcher_data_buffer[matcher_data.icon.0..matcher_data.icon.1];
        let entry_id = CString::new(matcher_data.entry_id)?;
        let title = matcher_data.title.map(|s| CString::new(s)).transpose()?;
        let subtitle = matcher_data.subtitle.map(|s| CString::new(s)).transpose()?;
        credman.add_string_id_entry(
            &entry_id,
            if icon.is_empty() { None } else { Some(icon) },
            title.as_deref(),
            subtitle.as_deref(),
            None,
            None,
        );
    }

    Ok(())
}

#[cfg(test)]
mod test {
    use super::*;
    use std::ffi::{CStr, CString};

    struct AddedEntry {
        entry_id: CString,
        icon: Option<Vec<u8>>,
        title: Option<CString>,
        subtitle: Option<CString>,
        disclaimer: Option<CString>,
        warning: Option<CString>,
    }

    struct FakeCredman {
        request_json: &'static str,
        registered_json: &'static str,
        icon: Vec<u8>,
        added_entries: Vec<AddedEntry>,
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

        fn add_string_id_entry(
            &mut self,
            entry_id: &CStr,
            icon: Option<&[u8]>,
            title: Option<&CStr>,
            subtitle: Option<&CStr>,
            disclaimer: Option<&CStr>,
            warning: Option<&CStr>,
        ) {
            self.added_entries.push(AddedEntry {
                entry_id: entry_id.to_owned(),
                icon: icon.map(|i| i.to_vec()),
                title: title.map(|c| c.to_owned()),
                subtitle: subtitle.map(|c| c.to_owned()),
                disclaimer: disclaimer.map(|c| c.to_owned()),
                warning: warning.map(|c| c.to_owned()),
            });
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
        "title": "TTTT",
        "subtitle": "SSSSS",
        "icon": [0, 0],
        "filter": {
          "And": {
            "filters": [{
              "AllowsConfigurationIds": {
                "configuration_ids": ["US_SOCIAL_SECURITY_NUMBER", "EU_AGE"]
              }
            }, {
              "AllowsIssuers": {
                "issuers": ["ccb", "https://issuer.my"]
              }
            }]
          }
        }
      }"#,
            icon: Vec::new(),
            added_entries: Vec::new(),
        };

        issuance_main(&mut credman).unwrap();

        assert_eq!(credman.added_entries.len(), 1);
        let entry = &credman.added_entries[0];
        assert_eq!(entry.entry_id, c"C");
        assert_eq!(entry.title.as_ref().unwrap(), c"TTTT");
        assert_eq!(entry.subtitle.as_ref().unwrap(), c"SSSSS");
        assert!(entry.icon.is_none());
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
        "title": "TTTT",
        "subtitle": "SSSSS",
        "icon": [0, 0],
        "filter": {"Unit": {}}"#,
            icon: Vec::new(),
            added_entries: Vec::new(),
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
  "title": "TTTT",
  "subtitle": "SSSSS",
  "icon": [
    0,
    0
  ],
  "filter": {
    "And": {
      "filters": [
        {
          "AllowsConfigurationIds": {
            "configuration_ids": [
              "US_SOCIAL_SECURITY_NUMBER",
              "EU_AGE"
            ]
          }
        },
        {
          "AllowsIssuers": {
            "issuers": [
              "ccb",
              "https://issuer.my"
            ]
          }
        },
        {
          "Not": {
            "filter": {
              "SupportsNonceEndpoint": {
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
  "title": "TTTT",
  "subtitle": "SSSSS",
  "icon": [
    0,
    0
  ],
  "filter": {
    "Or": {
      "filters": [
        {
          "AllowsConfigurationIds": {
            "configuration_ids": [
              "US_SOCIAL_SECURITY_NUMBER",
              "EU_AGE"
            ]
          }
        },
        {
          "AllowsIssuers": {
            "issuers": [
              "ccb"
            ]
          }
        },
        {
          "SupportsMdocDoctype": {
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
        };

        issuance_main(&mut credman).unwrap();

        assert_eq!(credman.added_entries.len(), 1);
    }
}
