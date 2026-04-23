//! WASM Logging implementation using `fd_write`.
//!
//! Logging is stripped by default at compile time to minimize the WASM binary size,
//! as the `log` crate and string formatting (fmt) significantly increase the footprint.
//!
//! To enable logging during development:
//! Use `--no-default-features --features logging` when building.
//!
//! Example:
//! cargo build --release --no-default-features --features logging

#[cfg(feature = "logging")]
use crate::bindings::fd_write;
#[cfg(feature = "logging")]
use log::{Metadata, Record};
#[cfg(feature = "logging")]
use std::os::raw::c_void;

#[cfg(feature = "logging")]
struct WasmLogger;

#[cfg(feature = "logging")]
#[repr(C)]
struct Ciovec {
    buf: *const u8,
    buf_len: u32,
}

#[cfg(feature = "logging")]
impl log::Log for WasmLogger {
    fn enabled(&self, _metadata: &Metadata) -> bool {
        true
    }

    fn log(&self, record: &Record) {
        if self.enabled(record.metadata()) {
            let msg = format!("{}: {}\n", record.level(), record.args());
            let ciovec = Ciovec {
                buf: msg.as_ptr(),
                buf_len: msg.len() as u32,
            };
            let mut nwritten = 0;
            unsafe {
                fd_write(
                    1, // stdout
                    &ciovec as *const _ as *const c_void,
                    1,
                    &mut nwritten,
                );
            }
        }
    }

    fn flush(&self) {}
}

#[cfg(feature = "logging")]
static LOGGER: WasmLogger = WasmLogger;

#[cfg(feature = "logging")]
pub fn init() {
    log::set_logger(&LOGGER)
        .map(|()| log::set_max_level(log::LevelFilter::Trace))
        .ok();

    std::panic::set_hook(Box::new(|panic_info| {
        let msg = panic_info.payload_as_str().unwrap_or("unknown");
        let formatted = match panic_info.location() {
            Some(loc) => format!("Panic at {}: {}\n", loc, msg),
            None => format!("Panic: {}\n", msg),
        };
        let ciovec = Ciovec {
            buf: formatted.as_ptr(),
            buf_len: formatted.len() as u32,
        };
        let mut nwritten = 0;
        unsafe {
            fd_write(
                1, // stdout
                &ciovec as *const _ as *const c_void,
                1,
                &mut nwritten,
            );
        }
    }));
}

#[cfg(not(feature = "logging"))]
pub fn init() {
    log::set_max_level(log::LevelFilter::Off);
}
