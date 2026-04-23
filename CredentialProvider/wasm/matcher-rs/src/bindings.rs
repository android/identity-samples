use std::os::raw::{c_char, c_void};

#[repr(C)]
pub struct CallingAppInfo {
    pub package_name: [u8; 256],
    pub origin: [u8; 512],
}

#[link(wasm_import_module = "credman")]
unsafe extern "C" {
    pub fn AddEntry(
        cred_id: i64,
        icon: *const c_char,
        icon_len: usize,
        title: *const c_char,
        subtitle: *const c_char,
        disclaimer: *const c_char,
        warning: *const c_char,
    );
    pub fn AddField(
        cred_id: i64,
        field_display_name: *const c_char,
        field_display_value: *const c_char,
    );
    pub fn AddEntrySet(set_id: *const c_char, set_length: i32);
    pub fn AddEntryToSet(
        cred_id: *const c_char,
        icon: *const c_char,
        icon_len: usize,
        title: *const c_char,
        subtitle: *const c_char,
        disclaimer: *const c_char,
        warning: *const c_char,
        metadata: *const c_char,
        set_id: *const c_char,
        set_index: i32,
    );
    pub fn AddFieldToEntrySet(
        cred_id: *const c_char,
        field_display_name: *const c_char,
        field_display_value: *const c_char,
        set_id: *const c_char,
        set_index: i32,
    );
    pub fn AddPaymentEntryToSet(
        cred_id: *const c_char,
        merchant_name: *const c_char,
        payment_method_name: *const c_char,
        payment_method_subtitle: *const c_char,
        payment_method_icon: *const c_char,
        payment_method_icon_len: usize,
        transaction_amount: *const c_char,
        bank_icon: *const c_char,
        bank_icon_len: usize,
        payment_provider_icon: *const c_char,
        payment_provider_icon_len: usize,
        metadata: *const c_char,
        set_id: *const c_char,
        set_index: i32,
    );
    pub fn AddPaymentEntryToSetV2(
        cred_id: *const c_char,
        merchant_name: *const c_char,
        payment_method_name: *const c_char,
        payment_method_subtitle: *const c_char,
        payment_method_icon: *const c_char,
        payment_method_icon_len: usize,
        transaction_amount: *const c_char,
        bank_icon: *const c_char,
        bank_icon_len: usize,
        payment_provider_icon: *const c_char,
        payment_provider_icon_len: usize,
        additional_info: *const c_char,
        metadata: *const c_char,
        set_id: *const c_char,
        set_index: i32,
    );
    pub fn AddStringIdEntry(
        cred_id: *const c_char,
        icon: *const c_char,
        icon_len: usize,
        title: *const c_char,
        subtitle: *const c_char,
        disclaimer: *const c_char,
        warning: *const c_char,
    );
    pub fn AddFieldForStringIdEntry(
        cred_id: *const c_char,
        field_display_name: *const c_char,
        field_display_value: *const c_char,
    );
    pub fn GetRequestBuffer(buffer: *mut c_void);
    pub fn GetRequestSize(size: *mut u32);
    pub fn ReadCredentialsBuffer(buffer: *mut c_void, offset: usize, len: usize) -> usize;
    pub fn GetCredentialsSize(size: *mut u32);
    pub fn GetWasmVersion(version: *mut u32);
    pub fn AddPaymentEntry(
        cred_id: *const c_char,
        merchant_name: *const c_char,
        payment_method_name: *const c_char,
        payment_method_subtitle: *const c_char,
        payment_method_icon: *const c_char,
        payment_method_icon_len: usize,
        transaction_amount: *const c_char,
        bank_icon: *const c_char,
        bank_icon_len: usize,
        payment_provider_icon: *const c_char,
        payment_provider_icon_len: usize,
    );
    pub fn AddInlineIssuanceEntry(
        cred_id: *const c_char,
        icon: *const c_char,
        icon_len: usize,
        title: *const c_char,
        subtitle: *const c_char,
    );
    pub fn SetAdditionalDisclaimerAndUrlForVerificationEntry(
        cred_id: *const c_char,
        secondary_disclaimer: *const c_char,
        url_display_text: *const c_char,
        url_value: *const c_char,
    );
    pub fn SetAdditionalDisclaimerAndUrlForVerificationEntryInCredentialSet(
        cred_id: *const c_char,
        secondary_disclaimer: *const c_char,
        url_display_text: *const c_char,
        url_value: *const c_char,
        set_id: *const c_char,
        set_index: i32,
    );
    pub fn GetCallingAppInfo(info: *mut CallingAppInfo);
    pub fn SelfDeclarePackageInfo(
        package_display_name: *const c_char,
        package_icon: *const c_char,
        package_icon_len: usize,
    );
    pub fn AddMetadataDisplayTextToEntrySet(
        cred_id: *const c_char,
        metadata_display_text: *const c_char,
        set_id: *const c_char,
        set_index: i32,
    );
    pub fn fd_write(fd: i32, iovs_ptr: *const c_void, iovs_len: i32, nwritten_ptr: *mut i32)
    -> i32;
}
