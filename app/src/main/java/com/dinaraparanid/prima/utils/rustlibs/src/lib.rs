#![feature(option_result_unwrap_unchecked)]
extern crate jni;

mod native_structs;

use crate::native_structs::track::Track;

use jni::sys::{
    jbyteArray, jchar, jclass, jint, jintArray, jlong, jobject, jsize, jstring, JNIEnv,
};
use std::ptr::null;

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

unsafe fn string_from_java(env: *mut JNIEnv, jstr: jbyteArray) -> String {
    let len = (**env).GetArrayLength.unwrap_unchecked()(env, jstr) as usize;
    String::from_raw_parts(
        (**env).GetByteArrayElements.unwrap_unchecked()(env, jstr, &mut 0) as *mut u8,
        len,
        len,
    )
}

#[no_mangle]
#[allow(non_snake_case)]
pub unsafe extern "system" fn Java_com_dinaraparanid_prima_utils_rustlibs_NativeLibrary_newTrack(
    env: *mut JNIEnv,
    _class: jclass,
    androidId: jlong,
    title: jbyteArray,
    artist: jbyteArray,
    playlist: jbyteArray,
    path: jbyteArray,
    duration: jlong,
    relativePath: jbyteArray,
    displayName: jbyteArray,
) -> jlong {
    let mut track = Box::new(Track::new(
        androidId as u64,
        string_from_java(env, title),
        string_from_java(env, artist),
        string_from_java(env, playlist),
        string_from_java(env, path),
        duration as u64,
        if relativePath.is_null() {
            None
        } else {
            Some(string_from_java(env, relativePath))
        },
        if displayName.is_null() {
            None
        } else {
            Some(string_from_java(env, displayName))
        },
    ));

    let ptr = Box::leak(track) as *mut Track as jlong;
    ptr
}

#[no_mangle]
#[allow(non_snake_case)]
pub unsafe extern "system" fn Java_com_dinaraparanid_prima_utils_rustlibs_NativeLibrary_getTrackAndroidId(
    _env: *mut JNIEnv,
    _class: jclass,
    pointer: jlong,
) -> jlong {
    let track = (&pointer as *const jlong as *const Track)
        .as_ref()
        .unwrap_unchecked();
    track.android_id as jlong
}

#[no_mangle]
#[allow(non_snake_case)]
pub unsafe extern "system" fn Java_com_dinaraparanid_prima_utils_rustlibs_NativeLibrary_getTrackTitle(
    env: *mut JNIEnv,
    _class: jclass,
    mut pointer: jlong,
) -> jstring {
    let track = (&mut pointer as *mut jlong as *mut Track)
        .as_mut()
        .unwrap_unchecked();
    let str = track.title.encode_utf16().collect::<Vec<_>>();
    (**env).NewString.unwrap_unchecked()(env, str.as_ptr(), str.len() as jsize)
}

#[no_mangle]
#[allow(non_snake_case)]
pub unsafe extern "system" fn Java_com_dinaraparanid_prima_utils_rustlibs_NativeLibrary_getTrackArtist(
    env: *mut JNIEnv,
    _class: jclass,
    mut pointer: jlong,
) -> jstring {
    let track = (&mut pointer as *mut jlong as *mut Track)
        .as_mut()
        .unwrap_unchecked();
    let str = track.artist.encode_utf16().collect::<Vec<_>>();
    (**env).NewString.unwrap_unchecked()(env, str.as_ptr(), str.len() as jsize)
}

#[no_mangle]
#[allow(non_snake_case)]
pub unsafe extern "system" fn Java_com_dinaraparanid_prima_utils_rustlibs_NativeLibrary_getTrackPlaylist(
    env: *mut JNIEnv,
    _class: jclass,
    mut pointer: jlong,
) -> jstring {
    let track = (&mut pointer as *mut jlong as *mut Track)
        .as_mut()
        .unwrap_unchecked();
    let str = track.playlist.encode_utf16().collect::<Vec<_>>();
    (**env).NewString.unwrap_unchecked()(env, str.as_ptr(), str.len() as jsize)
}

#[no_mangle]
#[allow(non_snake_case)]
pub unsafe extern "system" fn Java_com_dinaraparanid_prima_utils_rustlibs_NativeLibrary_getTrackPath(
    env: *mut JNIEnv,
    _class: jclass,
    mut pointer: jlong,
) -> jstring {
    let track = (&mut pointer as *mut jlong as *mut Track)
        .as_mut()
        .unwrap_unchecked();

    let str = track.path.encode_utf16().collect::<Vec<_>>();
    (**env).NewString.unwrap_unchecked()(env, str.as_ptr(), str.len() as jsize)
}

#[no_mangle]
#[allow(non_snake_case)]
pub unsafe extern "system" fn Java_com_dinaraparanid_prima_utils_rustlibs_NativeLibrary_getTrackDuration(
    _env: *mut JNIEnv,
    _class: jclass,
    pointer: jlong,
) -> jlong {
    let track = (&pointer as *const jlong as *const Track)
        .as_ref()
        .unwrap_unchecked();
    track.duration as jlong
}

#[no_mangle]
#[allow(non_snake_case)]
pub unsafe extern "system" fn Java_com_dinaraparanid_prima_utils_rustlibs_NativeLibrary_getTrackRelativePath(
    env: *mut JNIEnv,
    _class: jclass,
    mut pointer: jlong,
) -> jstring {
    let track = (&mut pointer as *mut jlong as *mut Track)
        .as_mut()
        .unwrap_unchecked();

    match &mut track.relative_path {
        Some(x) => {
            let str = x.encode_utf16().collect::<Vec<_>>();
            (**env).NewString.unwrap_unchecked()(env, str.as_ptr(), str.len() as jsize)
        }

        None => (**env).NewString.unwrap_unchecked()(env, null::<u16>(), 0 as jsize),
    }
}

#[no_mangle]
#[allow(non_snake_case)]
pub unsafe extern "system" fn Java_com_dinaraparanid_prima_utils_rustlibs_NativeLibrary_getTrackDisplayName(
    env: *mut JNIEnv,
    _class: jclass,
    mut pointer: jlong,
) -> jstring {
    let track = (&mut pointer as *mut jlong as *mut Track)
        .as_mut()
        .unwrap_unchecked();

    match &mut track.display_name {
        Some(x) => {
            let str = x.encode_utf16().collect::<Vec<_>>();
            (**env).NewString.unwrap_unchecked()(env, str.as_ptr(), str.len() as jsize)
        }

        None => (**env).NewString.unwrap_unchecked()(env, null::<u16>(), 0 as jsize),
    }
}
