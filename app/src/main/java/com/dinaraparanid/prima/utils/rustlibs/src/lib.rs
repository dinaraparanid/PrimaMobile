#![feature(option_result_unwrap_unchecked)]
extern crate jni;

use jni::sys::{jbyteArray, jchar, jclass, jsize, jstring, JNIEnv};

/// Converts artist name to the next pattern:
/// name family ... -> NF (upper case)
/// If artist don't have second word in his name, it will return only first letter

#[no_mangle]
#[allow(non_snake_case)]
pub unsafe extern "system" fn Java_com_dinaraparanid_prima_utils_rustlibs_NativeLibrary_artistImageBind(
    env: *mut JNIEnv,
    _class: jclass,
    name: jbyteArray,
) -> jstring {
    let len = (**env).GetArrayLength.unwrap_unchecked()(env, name) as usize;

    let str = String::from_raw_parts(
        (**env).GetByteArrayElements.unwrap_unchecked()(env, name, &mut 0) as *mut u8,
        len,
        len,
    );

    let arr = str.trim().split_whitespace().collect::<Vec<_>>();

    let len = arr.len() as jsize;

    let str = arr
        .into_iter()
        .filter(|&x| x != "&" && x != "feat." && x != "/")
        .take(2)
        .map(|s| {
            s.chars()
                .next()
                .unwrap_unchecked()
                .to_uppercase()
                .next()
                .unwrap_unchecked()
                .to_string()
                .encode_utf16()
                .next()
                .unwrap_unchecked()
        })
        .fold(Vec::with_capacity(2), |mut acc, x| {
            acc.push(x);
            acc
        });

    (**env).NewString.unwrap_unchecked()(
        env,
        str.as_ptr() as *const jchar,
        if len > 2 { 2 } else { len as i32 },
    )
}
