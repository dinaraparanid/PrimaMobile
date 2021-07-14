extern crate jni;

use jni::sys::{jbyteArray, jclass, jint, jstring, JNIEnv};
use std::os::raw::c_char;

#[no_mangle]
#[allow(non_snake_case)]
pub unsafe extern "system" fn Java_com_dinaraparanid_prima_utils_rustlibs_NativeLibrary_artistImageBind(
    env: *mut JNIEnv,
    _class: jclass,
    name: jbyteArray,
    bytes: jint,
) -> jstring {
    (**env).NewStringUTF.unwrap()(
        env,
        String::from_raw_parts(
            (**env).GetByteArrayElements.unwrap()(env, name, &mut 0) as *mut u8,
            bytes as usize,
            bytes as usize,
        )
        .split_whitespace()
        .take(2)
        .map(|s| match s.chars().next() {
            Some(x) => x.to_uppercase(),
            None => '?'.to_uppercase(),
        })
        .fold("".to_string(), |acc, x| format!("{}{}", acc, x))
        .as_bytes()
        .as_ptr() as *const c_char,
    )
}
