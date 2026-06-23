use std::{ffi::CString, os::raw::c_void};

use crate::bindings::{
    AddEntrySet, AddEntryToSet, AddFieldToEntrySet, AddInlineIssuanceEntry,
    AddMetadataDisplayTextToEntrySet, AddPaymentEntryToSetV2, AddStringIdEntry, GetCredentialsSize,
    GetRequestBuffer, GetRequestSize, GetWasmVersion, ReadCredentialsBuffer,
};

pub trait CredmanApi {
    fn get_request_buffer(&self) -> Vec<u8>;
    fn get_registered_data(&self) -> Vec<u8>;
    fn get_wasm_version(&self) -> u32;
    fn add_string_id_entry(
        &mut self,
        entry_id: &str,
        icon: &[u8],
        title: &str,
        subtitle: &str,
        disclaimer: &str,
        warning: &str,
    );
    fn add_entry_set(&mut self, set_id: &str, set_length: i32);
    fn add_entry_to_set(
        &mut self,
        cred_id: &str,
        icon: &[u8],
        title: &str,
        subtitle: &str,
        disclaimer: &str,
        warning: &str,
        metadata: &str,
        set_id: &str,
        set_index: i32,
    );
    fn add_field_to_entry_set(
        &mut self,
        cred_id: &str,
        field_display_name: &str,
        field_display_value: &str,
        set_id: &str,
        set_index: i32,
    );
    fn add_payment_entry_to_set_v2(
        &mut self,
        cred_id: &str,
        merchant_name: &str,
        payment_method_name: &str,
        payment_method_subtitle: &str,
        payment_method_icon: &[u8],
        transaction_amount: &str,
        bank_icon: &[u8],
        payment_provider_icon: &[u8],
        additional_info: &str,
        metadata: &str,
        set_id: &str,
        set_index: i32,
    );
    fn add_inline_issuance_entry(
        &mut self,
        cred_id: &str,
        icon: &[u8],
        title: &str,
        subtitle: &str,
    );
    fn add_metadata_display_text_to_entry_set(
        &mut self,
        cred_id: &str,
        metadata_display_text: &str,
        set_id: &str,
        set_index: i32,
    );
}

pub struct CredmanApiImpl;

impl CredmanApi for CredmanApiImpl {
    fn get_request_buffer(&self) -> Vec<u8> {
        let mut size: u32 = 0;
        unsafe {
            GetRequestSize(&mut size);
        }
        log::debug!("Host requested request buffer of size: {}", size);
        let mut r = vec![0; size as usize];
        unsafe {
            GetRequestBuffer(r.as_mut_ptr() as *mut c_void);
        }
        r
    }
    fn get_registered_data(&self) -> Vec<u8> {
        let mut size: u32 = 0;
        unsafe {
            GetCredentialsSize(&mut size);
        }
        log::debug!("Host requested registered data buffer of size: {}", size);
        let mut r = vec![0; size.try_into().unwrap()];
        unsafe {
            ReadCredentialsBuffer(r.as_mut_ptr() as *mut c_void, 0, size as usize);
        }
        r
    }
    fn get_wasm_version(&self) -> u32 {
        let mut version: u32 = 0;
        unsafe {
            GetWasmVersion(&mut version);
        }
        version
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
        let entry_id_c = CString::new(entry_id).unwrap();
        let title_c = if title.is_empty() {
            None
        } else {
            Some(CString::new(title).unwrap())
        };
        let subtitle_c = if subtitle.is_empty() {
            None
        } else {
            Some(CString::new(subtitle).unwrap())
        };
        let disclaimer_c = if disclaimer.is_empty() {
            None
        } else {
            Some(CString::new(disclaimer).unwrap())
        };
        let warning_c = if warning.is_empty() {
            None
        } else {
            Some(CString::new(warning).unwrap())
        };

        let icon_bytes = if icon.is_empty() {
            std::ptr::null()
        } else {
            icon.as_ptr()
        } as *const std::os::raw::c_char;
        let icon_length = icon.len();

        unsafe {
            AddStringIdEntry(
                entry_id_c.as_ptr(),
                icon_bytes,
                icon_length,
                title_c.as_ref().map_or(std::ptr::null(), |c| c.as_ptr()),
                subtitle_c.as_ref().map_or(std::ptr::null(), |c| c.as_ptr()),
                disclaimer_c
                    .as_ref()
                    .map_or(std::ptr::null(), |c| c.as_ptr()),
                warning_c.as_ref().map_or(std::ptr::null(), |c| c.as_ptr()),
            );
        }
    }
    fn add_entry_set(&mut self, set_id: &str, set_length: i32) {
        let set_id_c = CString::new(set_id).unwrap();
        unsafe {
            AddEntrySet(set_id_c.as_ptr(), set_length);
        }
    }
    fn add_entry_to_set(
        &mut self,
        cred_id: &str,
        icon: &[u8],
        title: &str,
        subtitle: &str,
        disclaimer: &str,
        warning: &str,
        metadata: &str,
        set_id: &str,
        set_index: i32,
    ) {
        let cred_id_c = CString::new(cred_id).unwrap();
        let title_c = if title.is_empty() {
            None
        } else {
            Some(CString::new(title).unwrap())
        };
        let subtitle_c = if subtitle.is_empty() {
            None
        } else {
            Some(CString::new(subtitle).unwrap())
        };
        let disclaimer_c = if disclaimer.is_empty() {
            None
        } else {
            Some(CString::new(disclaimer).unwrap())
        };
        let warning_c = if warning.is_empty() {
            None
        } else {
            Some(CString::new(warning).unwrap())
        };
        let metadata_c = if metadata.is_empty() {
            None
        } else {
            Some(CString::new(metadata).unwrap())
        };
        let set_id_c = CString::new(set_id).unwrap();

        let icon_bytes = if icon.is_empty() {
            std::ptr::null()
        } else {
            icon.as_ptr()
        } as *const std::os::raw::c_char;
        let icon_length = icon.len();

        unsafe {
            AddEntryToSet(
                cred_id_c.as_ptr(),
                icon_bytes,
                icon_length,
                title_c.as_ref().map_or(std::ptr::null(), |c| c.as_ptr()),
                subtitle_c.as_ref().map_or(std::ptr::null(), |c| c.as_ptr()),
                disclaimer_c
                    .as_ref()
                    .map_or(std::ptr::null(), |c| c.as_ptr()),
                warning_c.as_ref().map_or(std::ptr::null(), |c| c.as_ptr()),
                metadata_c.as_ref().map_or(std::ptr::null(), |c| c.as_ptr()),
                set_id_c.as_ptr(),
                set_index,
            );
        }
    }
    fn add_field_to_entry_set(
        &mut self,
        cred_id: &str,
        field_display_name: &str,
        field_display_value: &str,
        set_id: &str,
        set_index: i32,
    ) {
        let cred_id_c = CString::new(cred_id).unwrap();
        let field_display_name_c = CString::new(field_display_name).unwrap();
        let field_display_value_c = if field_display_value.is_empty() {
            None
        } else {
            Some(CString::new(field_display_value).unwrap())
        };
        let set_id_c = CString::new(set_id).unwrap();

        unsafe {
            AddFieldToEntrySet(
                cred_id_c.as_ptr(),
                field_display_name_c.as_ptr(),
                field_display_value_c
                    .as_ref()
                    .map_or(std::ptr::null(), |c| c.as_ptr()),
                set_id_c.as_ptr(),
                set_index,
            );
        }
    }
    fn add_payment_entry_to_set_v2(
        &mut self,
        cred_id: &str,
        merchant_name: &str,
        payment_method_name: &str,
        payment_method_subtitle: &str,
        payment_method_icon: &[u8],
        transaction_amount: &str,
        bank_icon: &[u8],
        payment_provider_icon: &[u8],
        additional_info: &str,
        metadata: &str,
        set_id: &str,
        set_index: i32,
    ) {
        let cred_id_c = CString::new(cred_id).unwrap();
        let merchant_name_c = if merchant_name.is_empty() {
            None
        } else {
            Some(CString::new(merchant_name).unwrap())
        };
        let payment_method_name_c = if payment_method_name.is_empty() {
            None
        } else {
            Some(CString::new(payment_method_name).unwrap())
        };
        let payment_method_subtitle_c = if payment_method_subtitle.is_empty() {
            None
        } else {
            Some(CString::new(payment_method_subtitle).unwrap())
        };
        let transaction_amount_c = if transaction_amount.is_empty() {
            None
        } else {
            Some(CString::new(transaction_amount).unwrap())
        };
        let additional_info_c = if additional_info.is_empty() {
            None
        } else {
            Some(CString::new(additional_info).unwrap())
        };
        let metadata_c = if metadata.is_empty() {
            None
        } else {
            Some(CString::new(metadata).unwrap())
        };
        let set_id_c = CString::new(set_id).unwrap();

        let icon_bytes = if payment_method_icon.is_empty() {
            std::ptr::null()
        } else {
            payment_method_icon.as_ptr()
        } as *const std::os::raw::c_char;
        let icon_length = payment_method_icon.len();
        let bank_icon_bytes = if bank_icon.is_empty() {
            std::ptr::null()
        } else {
            bank_icon.as_ptr()
        } as *const std::os::raw::c_char;
        let bank_icon_length = bank_icon.len();
        let provider_icon_bytes = if payment_provider_icon.is_empty() {
            std::ptr::null()
        } else {
            payment_provider_icon.as_ptr()
        } as *const std::os::raw::c_char;
        let provider_icon_length = payment_provider_icon.len();

        unsafe {
            AddPaymentEntryToSetV2(
                cred_id_c.as_ptr(),
                merchant_name_c
                    .as_ref()
                    .map_or(std::ptr::null(), |c| c.as_ptr()),
                payment_method_name_c
                    .as_ref()
                    .map_or(std::ptr::null(), |c| c.as_ptr()),
                payment_method_subtitle_c
                    .as_ref()
                    .map_or(std::ptr::null(), |c| c.as_ptr()),
                icon_bytes,
                icon_length,
                transaction_amount_c
                    .as_ref()
                    .map_or(std::ptr::null(), |c| c.as_ptr()),
                bank_icon_bytes,
                bank_icon_length,
                provider_icon_bytes,
                provider_icon_length,
                additional_info_c
                    .as_ref()
                    .map_or(std::ptr::null(), |c| c.as_ptr()),
                metadata_c.as_ref().map_or(std::ptr::null(), |c| c.as_ptr()),
                set_id_c.as_ptr(),
                set_index,
            );
        }
    }
    fn add_inline_issuance_entry(
        &mut self,
        cred_id: &str,
        icon: &[u8],
        title: &str,
        subtitle: &str,
    ) {
        let cred_id_c = CString::new(cred_id).unwrap();
        let title_c = if title.is_empty() {
            None
        } else {
            Some(CString::new(title).unwrap())
        };
        let subtitle_c = if subtitle.is_empty() {
            None
        } else {
            Some(CString::new(subtitle).unwrap())
        };

        let icon_bytes = if icon.is_empty() {
            std::ptr::null()
        } else {
            icon.as_ptr()
        } as *const std::os::raw::c_char;
        let icon_length = icon.len();

        unsafe {
            AddInlineIssuanceEntry(
                cred_id_c.as_ptr(),
                icon_bytes,
                icon_length,
                title_c.as_ref().map_or(std::ptr::null(), |c| c.as_ptr()),
                subtitle_c.as_ref().map_or(std::ptr::null(), |c| c.as_ptr()),
            );
        }
    }
    fn add_metadata_display_text_to_entry_set(
        &mut self,
        cred_id: &str,
        metadata_display_text: &str,
        set_id: &str,
        set_index: i32,
    ) {
        let cred_id_c = CString::new(cred_id).unwrap();
        let metadata_display_text_c = CString::new(metadata_display_text).unwrap();
        let set_id_c = CString::new(set_id).unwrap();

        unsafe {
            AddMetadataDisplayTextToEntrySet(
                cred_id_c.as_ptr(),
                metadata_display_text_c.as_ptr(),
                set_id_c.as_ptr(),
                set_index,
            );
        }
    }
}
