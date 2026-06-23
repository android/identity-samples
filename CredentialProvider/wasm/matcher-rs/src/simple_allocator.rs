#![cfg(target_arch = "wasm32")]

use core::alloc::{GlobalAlloc, Layout};

pub struct SimpleAllocator;

unsafe extern "C" {
    static __heap_base: u8;
}

static mut NEXT_ADDR: usize = 0;
static mut CURRENT_PAGES: usize = 0;

const PAGE_SIZE: usize = 65536;

unsafe impl GlobalAlloc for SimpleAllocator {
    unsafe fn alloc(&self, layout: Layout) -> *mut u8 {
        let mut next_addr = unsafe { NEXT_ADDR };
        if next_addr == 0 {
            next_addr = core::ptr::addr_of!(__heap_base) as usize;
            unsafe { CURRENT_PAGES = core::arch::wasm32::memory_size(0) };
        }

        let align = layout.align();
        let size = layout.size();

        // Align the current pointer
        let alloc_ptr = (next_addr + align - 1) & !(align - 1);
        let end_ptr = alloc_ptr + size;

        let current_limit = unsafe { CURRENT_PAGES * PAGE_SIZE };

        if end_ptr > current_limit {
            let needed_bytes = end_ptr - current_limit;
            let needed_pages = (needed_bytes + PAGE_SIZE - 1) / PAGE_SIZE;
            if core::arch::wasm32::memory_grow(0, needed_pages) == usize::MAX {
                return core::ptr::null_mut();
            }
            unsafe { CURRENT_PAGES += needed_pages };
        }

        unsafe { NEXT_ADDR = end_ptr };
        alloc_ptr as *mut u8
    }

    unsafe fn dealloc(&self, _ptr: *mut u8, _layout: Layout) {
        // No-op bump allocator
    }
}
