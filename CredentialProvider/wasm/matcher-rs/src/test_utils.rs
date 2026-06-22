#![cfg(test)]
use crate::credman::CredmanApi;
use crate::json_value::DeterministicMap;
use crate::openid4vp::openid4vp_main;
use nanoserde::{DeJson, SerJson};

#[derive(SerJson, DeJson, PartialEq, Debug, Clone, Default)]
pub enum EntryType {
    #[default]
    Verification,
    InlineIssuance,
    Payment,
    UserInfo,
    Export,
}

#[derive(SerJson, DeJson, PartialEq, Debug, Clone, Default)]
pub struct FakeEntry {
    #[nserde(rename = "credId")]
    pub cred_id: String,
    #[nserde(rename = "type")]
    pub entry_type: EntryType,
    #[nserde(default)]
    pub title: String,
    #[nserde(default)]
    pub subtitle: String,
    #[nserde(default)]
    pub disclaimer: String,
    #[nserde(default)]
    pub warning: String,
    #[nserde(default)]
    pub metadata_display_text: String,
    #[nserde(default)]
    pub fields: Vec<(String, String)>,
    #[nserde(default)]
    pub merchant_name: String,
    #[nserde(default)]
    pub transaction_amount: String,
    #[nserde(default)]
    pub additional_info: String,
}

#[derive(Debug, Clone, Default)]
pub struct AddedEntry {
    pub entry_id: String,
    pub icon: Vec<u8>,
    pub title: String,
    pub subtitle: String,
    pub disclaimer: String,
    pub warning: String,
}

#[derive(SerJson, DeJson, PartialEq, Debug, Clone)]
pub struct FakeEntrySet {
    #[nserde(rename = "setId")]
    pub set_id: String,
    #[nserde(rename = "setLength")]
    pub set_length: i32,
    #[nserde(default)]
    pub entries: DeterministicMap<String, DeterministicMap<String, FakeEntry>>,
}

#[derive(SerJson, DeJson, PartialEq, Debug)]
pub struct FakeCredmanResult {
    #[nserde(rename = "entrySets")]
    pub entry_sets: DeterministicMap<String, FakeEntrySet>,
    #[nserde(rename = "standaloneEntries")]
    pub standalone_entries: Vec<FakeEntry>,
}

pub struct FakeCredman {
    pub entry_sets: DeterministicMap<String, FakeEntrySet>,
    pub standalone_entries: Vec<FakeEntry>,
    pub wasm_version: u32,
    pub request_json: String,
    pub credentials_blob: Vec<u8>,
    pub added_entries: Vec<AddedEntry>,
}

impl FakeCredman {
    pub fn new() -> Self {
        Self {
            entry_sets: DeterministicMap::new(),
            standalone_entries: Vec::new(),
            wasm_version: 9999,
            request_json: String::new(),
            credentials_blob: Vec::new(),
            added_entries: Vec::new(),
        }
    }
}

impl CredmanApi for FakeCredman {
    fn get_request_buffer(&self) -> Vec<u8> {
        self.request_json.as_bytes().to_vec()
    }
    fn get_registered_data(&self) -> Vec<u8> {
        self.credentials_blob.clone()
    }
    fn get_wasm_version(&self) -> u32 {
        self.wasm_version
    }
    fn add_string_id_entry(
        &mut self,
        id: &str,
        icon: &[u8],
        title: &str,
        subtitle: &str,
        disclaimer: &str,
        warning: &str,
    ) {
        self.added_entries.push(AddedEntry {
            entry_id: id.to_string(),
            icon: icon.to_vec(),
            title: title.to_string(),
            subtitle: subtitle.to_string(),
            disclaimer: disclaimer.to_string(),
            warning: warning.to_string(),
        });
    }
    fn add_entry_set(&mut self, set_id: &str, set_length: i32) {
        let s_id = set_id.to_string();
        self.entry_sets.insert(
            s_id.clone(),
            FakeEntrySet {
                set_id: s_id,
                set_length,
                entries: DeterministicMap::new(),
            },
        );
    }
    fn add_entry_to_set(
        &mut self,
        cred_id: &str,
        _icon: &[u8],
        title: &str,
        subtitle: &str,
        disclaimer: &str,
        warning: &str,
        _metadata: &str,
        set_id: &str,
        set_index: i32,
    ) {
        let s_id = set_id.to_string();
        let c_id = cred_id.to_string();
        let entry = FakeEntry {
            cred_id: c_id.clone(),
            entry_type: EntryType::Verification,
            title: title.to_string(),
            subtitle: subtitle.to_string(),
            disclaimer: disclaimer.to_string(),
            warning: warning.to_string(),
            metadata_display_text: String::new(),
            fields: Vec::new(),
            merchant_name: String::new(),
            transaction_amount: String::new(),
            additional_info: String::new(),
        };
        self.entry_sets
            .get_mut(&s_id)
            .unwrap()
            .entries
            .entry(set_index.to_string())
            .or_insert_with(DeterministicMap::new)
            .insert(c_id, entry);
    }
    fn add_field_to_entry_set(
        &mut self,
        cred_id: &str,
        field_display_name: &str,
        field_display_value: &str,
        set_id: &str,
        set_index: i32,
    ) {
        let s_id = set_id.to_string();
        let c_id = cred_id.to_string();
        let f_name = field_display_name.to_string();
        let f_val = field_display_value.to_string();
        self.entry_sets
            .get_mut(&s_id)
            .unwrap()
            .entries
            .get_mut(&set_index.to_string())
            .unwrap()
            .get_mut(&c_id)
            .unwrap()
            .fields
            .push((f_name, f_val));
    }
    fn add_payment_entry_to_set_v2(
        &mut self,
        cred_id: &str,
        merchant_name: &str,
        _method_name: &str,
        _method_subtitle: &str,
        _icon: &[u8],
        transaction_amount: &str,
        _bank_icon: &[u8],
        _provider_icon: &[u8],
        additional_info: &str,
        _metadata: &str,
        set_id: &str,
        set_index: i32,
    ) {
        let s_id = set_id.to_string();
        let c_id = cred_id.to_string();
        let entry = FakeEntry {
            cred_id: c_id.clone(),
            entry_type: EntryType::Payment,
            title: String::new(),
            subtitle: String::new(),
            disclaimer: String::new(),
            warning: String::new(),
            metadata_display_text: String::new(),
            fields: Vec::new(),
            merchant_name: merchant_name.to_string(),
            transaction_amount: transaction_amount.to_string(),
            additional_info: additional_info.to_string(),
        };
        self.entry_sets
            .get_mut(&s_id)
            .unwrap()
            .entries
            .entry(set_index.to_string())
            .or_insert_with(DeterministicMap::new)
            .insert(c_id, entry);
    }
    fn add_inline_issuance_entry(
        &mut self,
        cred_id: &str,
        _icon: &[u8],
        title: &str,
        subtitle: &str,
    ) {
        let entry = FakeEntry {
            cred_id: cred_id.to_string(),
            entry_type: EntryType::InlineIssuance,
            title: title.to_string(),
            subtitle: subtitle.to_string(),
            disclaimer: String::new(),
            warning: String::new(),
            metadata_display_text: String::new(),
            fields: Vec::new(),
            merchant_name: String::new(),
            transaction_amount: String::new(),
            additional_info: String::new(),
        };
        self.standalone_entries.push(entry);
    }
    fn add_metadata_display_text_to_entry_set(
        &mut self,
        cred_id: &str,
        metadata_display_text: &str,
        set_id: &str,
        set_index: i32,
    ) {
        let s_id = set_id.to_string();
        let c_id = cred_id.to_string();
        self.entry_sets
            .get_mut(&s_id)
            .unwrap()
            .entries
            .get_mut(&set_index.to_string())
            .unwrap()
            .get_mut(&c_id)
            .unwrap()
            .metadata_display_text = metadata_display_text.to_string();
    }
}

pub fn create_registry_blob(json_str: &str) -> Vec<u8> {
    let mut blob = Vec::new();
    let offset = 4 + 10;
    blob.extend_from_slice(&(offset as u32).to_le_bytes());
    for i in 0..10 {
        blob.push(i as u8);
    }
    blob.extend_from_slice(json_str.as_bytes());
    blob
}

pub fn run_openid4vp_test(test_name: &str, custom_registry: Option<&str>) {
    let manifest_dir = std::env::var("CARGO_MANIFEST_DIR").unwrap();
    let testdata_dir = std::path::PathBuf::from(manifest_dir).join("testdata");

    let registry_json = match custom_registry {
        Some(s) => s.to_string(),
        None => std::fs::read_to_string(testdata_dir.join("registry.json")).unwrap(),
    };

    let request_path = testdata_dir.join(format!("{}_request.json", test_name));
    let expected_path = testdata_dir.join(format!("{}_expected.json", test_name));

    let request_json = std::fs::read_to_string(&request_path)
        .unwrap_or_else(|_| panic!("Failed to read {:?}", request_path));
    let expected_json = std::fs::read_to_string(&expected_path)
        .unwrap_or_else(|_| panic!("Failed to read {:?}", expected_path));

    let mut credman = FakeCredman::new();
    credman.credentials_blob = create_registry_blob(&registry_json);
    credman.request_json = request_json;

    openid4vp_main(&mut credman).unwrap();

    let result = FakeCredmanResult {
        entry_sets: credman.entry_sets,
        standalone_entries: credman.standalone_entries,
    };

    let expected_result: FakeCredmanResult = DeJson::deserialize_json(&expected_json).unwrap();

    assert_eq!(result, expected_result, "Test {} failed", test_name);
}
