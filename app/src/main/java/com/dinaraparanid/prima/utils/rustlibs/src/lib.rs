#![feature(option_result_unwrap_unchecked)]
extern crate jni;

use jni::sys::{jbyteArray, jchar, jclass, jint, jintArray, jsize, jstring, JNIEnv};

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
        if len > 2 { 2 } else { len as jsize },
    )
}

/// Calculates time in hh:mm:ss format
///
/// #Arguments
/// *millis* - millisecond to convert
///
/// #Return
/// jintArray[hh, mm, ss]

#[no_mangle]
#[allow(non_snake_case)]
pub unsafe extern "system" fn Java_com_dinaraparanid_prima_utils_rustlibs_NativeLibrary_calcTrackTime(
    env: *mut JNIEnv,
    _class: jclass,
    mut millis: jint,
) -> jintArray {
    let time = (**env).NewIntArray.unwrap_unchecked()(env, 3);

    let h = millis / 3600000;
    millis -= h * 3600000;

    let m = millis / 60000;
    millis -= m * 60000;

    let s = millis / 1000;

    let arr = [h, m, s];

    (**env).SetIntArrayRegion.unwrap_unchecked()(env, time, 0, 3, arr.as_ptr());
    time
}

/// Gets playlist title.
/// If it equals to path,
/// it'll return 'Unknown album' in selected locale
///
/// #Arguments
/// *trackPlaylist* - album name
/// *trackPath* - path to track (DATA column from MediaStore)
/// *unknown* - 'Unknown album' string in selected locale
///
/// #Return
/// correct album title or 'Unknown album'

#[no_mangle]
#[allow(non_snake_case)]
pub unsafe extern "system" fn Java_com_dinaraparanid_prima_utils_rustlibs_NativeLibrary_playlistTitle(
    env: *mut JNIEnv,
    _class: jclass,
    trackPlaylist: jbyteArray,
    trackPath: jbyteArray,
    unknown: jbyteArray,
) -> jstring {
    let playlist = string_from_java(env, trackPlaylist);
    let unknown = string_from_java(env, unknown);

    let path = string_from_java(env, trackPath)
        .split("/")
        .collect::<Vec<_>>()
        .into_iter()
        .rev()
        .skip(1)
        .next()
        .unwrap_unchecked()
        .to_string();

    if path == playlist {
        let unknown = unknown.encode_utf16().collect::<Vec<_>>();
        (**env).NewString.unwrap_unchecked()(env, unknown.as_ptr(), unknown.len() as jsize)
    } else {
        let playlist = playlist.encode_utf16().collect::<Vec<_>>();
        (**env).NewString.unwrap_unchecked()(env, playlist.as_ptr(), playlist.len() as jsize)
    }
}

/// Creates string from jbyteArray

#[inline]
unsafe fn string_from_java(env: *mut JNIEnv, jstr: jbyteArray) -> String {
    let len = (**env).GetArrayLength.unwrap_unchecked()(env, jstr) as usize;
    String::from_raw_parts(
        (**env).GetByteArrayElements.unwrap_unchecked()(env, jstr, &mut 0) as *mut u8,
        len,
        len,
    )
}
