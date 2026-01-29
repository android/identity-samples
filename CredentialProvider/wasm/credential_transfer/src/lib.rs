use std::ffi::{c_void, CString};
use std::os::raw::{c_char, c_int};
use std::slice;
use serde::Deserialize;

// -------------------------------------------------------------------------
// 1. WASM Imports (Mapping to credman module)
// -------------------------------------------------------------------------

#[link(wasm_import_module = "credman")]
extern "C" {
    fn AddEntry(cred_id: i64, icon: *const u8, icon_len: usize, title: *const c_char, subtitle: *const c_char, disclaimer: *const c_char, warning: *const c_char);
    fn AddField(cred_id: i64, field_display_name: *const c_char, field_display_value: *const c_char);
    fn AddStringIdEntry(cred_id: *const c_char, icon: *const u8, icon_len: usize, title: *const c_char, subtitle: *const c_char, disclaimer: *const c_char, warning: *const c_char);
    
    // Note: The C code used import_name("AddExportEntry"), mapping here.
    fn AddExportEntry(cred_id: *const c_char, icon: *const u8, icon_len: usize, username: *const c_char, provider_name: *const c_char, display_name: *const c_char);

    fn GetRequestBuffer(buffer: *mut c_void);
    fn GetRequestSize(size: *mut u32);
    fn ReadCredentialsBuffer(buffer: *mut c_void, offset: usize, len: usize) -> usize;
    fn GetCredentialsSize(size: *mut u32);
    fn GetCallingAppInfo(info: *mut CallingAppInfo);
}

// -------------------------------------------------------------------------
// 2. Struct Definitions (JSON & Binary mapping)
// -------------------------------------------------------------------------

#[repr(C)]
struct CallingAppInfo {
    package_name: [u8; 256],
    origin: [u8; 512],
}

// JSON: Request Schema
#[derive(Deserialize)]
struct Request {
    #[serde(rename = "credentialTypes")]
    credential_types: Option<Vec<String>>,
}

// JSON: Display Info inside an Entry
#[derive(Deserialize)]
struct DisplayInfo {
    user_name: String,
    icon_id: Option<usize>,
    account_name: Option<String>,
}

// JSON: Entry Schema
#[derive(Deserialize)]
struct Entry {
    id: String,
    supported_credential_types: Option<Vec<String>>,
    display_info: DisplayInfo,
}

// JSON: Credentials Root Schema
#[derive(Deserialize)]
struct Credentials {
    entries: Option<Vec<Entry>>,
}

// -------------------------------------------------------------------------
// 3. Helper Functions
// -------------------------------------------------------------------------

/// Allocates memory and fetches data from the host
fn get_buffer_from_host(
    size_fn: unsafe extern "C" fn(*mut u32),
    data_fn: unsafe extern "C" fn(*mut c_void)
) -> Vec<u8> {
    unsafe {
        let mut size: u32 = 0;
        size_fn(&mut size);
        if size == 0 { return Vec::new(); }
        
        let mut buffer = Vec::with_capacity(size as usize);
        data_fn(buffer.as_mut_ptr() as *mut c_void);
        buffer.set_len(size as usize);
        buffer
    }
}

/// Helper to parse an integer from a byte slice (Little Endian as per WASM standard)
fn read_i32(buffer: &[u8], offset: usize) -> i32 {
    let bytes = &buffer[offset..offset+4];
    i32::from_ne_bytes(bytes.try_into().unwrap())
}

// -------------------------------------------------------------------------
// 4. Main Logic
// -------------------------------------------------------------------------

// Credman expects this as the entry point, but it isn't there if the target is wasm32-unknown-unknown.
#[cfg(all(target_arch = "wasm32", target_os = "unknown"))]
#[unsafe(no_mangle)]
extern "C" fn _start() {
    main();
}

#[no_mangle]
pub extern "C" fn main() -> i32 {
    unsafe {
        // 1. Get Data from Host
        // Replaces: GetRequest() and GetCredentials()
        let request_buffer = get_buffer_from_host(GetRequestSize, GetRequestBuffer);
        
        let mut creds_total_size: u32 = 0;
        GetCredentialsSize(&mut creds_total_size);
        let mut credentials_buffer = Vec::with_capacity(creds_total_size as usize);
        ReadCredentialsBuffer(credentials_buffer.as_mut_ptr() as *mut c_void, 0, creds_total_size as usize);
        credentials_buffer.set_len(creds_total_size as usize);

        // 2. Get App Info (Preserving logic from C, though unused)
        let mut app_info = CallingAppInfo { package_name: [0; 256], origin: [0; 512] };
        GetCallingAppInfo(&mut app_info);

        // 3. Parse Binary Header from credentials_buffer
        // Layout: [header_size (4b)][creds_size (4b)][icon_count (4b)][icon_size_1][icon_size_2]...
        if credentials_buffer.len() < 12 { return 0; } // Safety check

        let header_size = read_i32(&credentials_buffer, 0) as usize;
        let creds_size = read_i32(&credentials_buffer, 4) as usize;
        let icon_count = read_i32(&credentials_buffer, 8) as usize;

        // Calculate Icon Offsets
        // The C code calculates absolute pointers. In Rust, we calculate offsets relative to the buffer.
        let mut icon_offsets = Vec::new();
        let mut current_icon_start = header_size + creds_size; // Start of icon data block
        
        // Loop through the icon size array which sits at offset 12
        for i in 0..icon_count {
            let size_offset = 12 + (i * 4);
            let this_icon_size = read_i32(&credentials_buffer, size_offset) as usize;
            
            icon_offsets.push((current_icon_start, this_icon_size));
            current_icon_start += this_icon_size;
        }

        // 4. Parse JSON
        // Request JSON
        let request_str = match std::str::from_utf8(&request_buffer) {
            Ok(s) => s,
            Err(_) => return 0,
        };
        let request_json: Request = match serde_json::from_str(request_str) {
            Ok(j) => j,
            Err(_) => return 0,
        };

        // Credentials JSON
        // Ensure we don't read out of bounds. The JSON starts at `header_size`.
        if header_size >= credentials_buffer.len() { return 0; }
        
        // We need to slice strictly the JSON part. The C code implies the JSON is 
        // located at `header_size`, but standard cJSON parsing usually stops at null or matching braces.
        // We will slice from header_size up to the start of icons.
        let json_end = header_size + creds_size;
        let creds_json_slice = &credentials_buffer[header_size..json_end];
        let creds_json_str = match std::str::from_utf8(creds_json_slice) {
            Ok(s) => s.trim_matches(char::from(0)), // Remove potential null terminators
            Err(_) => return 0,
        };

        let creds_json: Credentials = match serde_json::from_str(creds_json_str) {
            Ok(j) => j,
            Err(_) => return 0, // Failed to parse credentials JSON
        };

        // 5. Matching Logic
        if let Some(req_types) = request_json.credential_types {
            if let Some(entries) = creds_json.entries {
                for entry in entries {
                    let mut matched = false;

                    // Check if supported types match requested types
                    if let Some(ref supported) = entry.supported_credential_types {
                        for supp_type in supported {
                            if req_types.contains(supp_type) {
                                matched = true;
                                break;
                            }
                        }
                    }

                    if matched {
                        // Prepare data for export
                        let id = CString::new(entry.id).unwrap();
                        let username = CString::new(entry.display_info.user_name).unwrap();
                        let provider = CString::new("default_provider").unwrap(); // Hardcoded in C
                        
                        let account_name_str = entry.display_info.account_name.unwrap_or_default();
                        // C code passes "account_name" variable to "display_name" param
                        let display_name = CString::new(account_name_str).unwrap_or_default();

                        // Handle Icon
                        let mut icon_ptr: *const u8 = std::ptr::null();
                        let mut icon_len: usize = 0;

                        if let Some(icon_idx) = entry.display_info.icon_id {
                            if icon_idx < icon_offsets.len() {
                                let (offset, len) = icon_offsets[icon_idx];
                                // Get pointer to specific slice in buffer
                                icon_ptr = credentials_buffer.as_ptr().add(offset);
                                icon_len = len;
                            }
                        }

                        // Call Host Function
                        AddExportEntry(
                            id.as_ptr(),
                            icon_ptr,
                            icon_len,
                            username.as_ptr(),
                            provider.as_ptr(),
                            display_name.as_ptr()
                        );
                    }
                }
            }
        }
    }

    0
}