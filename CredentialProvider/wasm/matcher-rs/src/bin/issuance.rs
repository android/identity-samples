use matcher_rs::{credman::CredmanApiImpl, issuance::issuance_main};

fn main() {
    issuance_main(&mut CredmanApiImpl {}).unwrap();
}

// Credman expects this as the entry point, but it isn't there if the target is wasm32-unknown-unknown.
#[cfg(all(target_arch = "wasm32", target_os = "unknown"))]
#[unsafe(no_mangle)]
extern "C" fn _start() {
    main();
}
