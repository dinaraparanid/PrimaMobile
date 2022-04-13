extern crate genius_lyrics;
extern crate jni;
extern crate track_album_number_genius;

use jni::sys::{
    jbyteArray, jchar, jclass, jint, jintArray, jsize, jstring, JNIEnv, _jobject, jbyte,
};
use std::os::raw::c_char;

/// Converts artist name to the next pattern:
/// Name Family ... -> NF (upper case)
/// If artist don't have second word in his name, it will return only first letter
///
/// # Safety
/// Extern JNI function
///
/// # Arguments
/// name - full name of artist
///
/// # Return
/// Converted artist's name

#[no_mangle]
#[allow(non_snake_case)]
pub unsafe extern "system" fn Java_com_dinaraparanid_prima_utils_rustlibs_NativeLibrary_artistImageBind(
    env: *mut JNIEnv,
    _class: jclass,
    name: jbyteArray,
) -> jstring {
    let str = string_from_byte_array(env, name);
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
/// # Safety
/// Extern JNI junction
///
/// # Arguments
/// *millis* - millisecond to convert
///
/// # Return
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
/// # Safety
/// Extern JNI function
///
/// # Arguments
/// *trackPlaylist* - album name
/// *trackPath* - path to track (DATA column from MediaStore)
/// *unknown* - 'Unknown album' string in selected locale
///
/// # Return
/// Correct album title or 'Unknown album'
///
/// # Deprecated
/// Path checking isn't needed now

#[no_mangle]
#[allow(non_snake_case)]
pub unsafe extern "system" fn Java_com_dinaraparanid_prima_utils_rustlibs_NativeLibrary_playlistTitle(
    env: *mut JNIEnv,
    _class: jclass,
    trackPlaylist: jbyteArray,
    trackPath: jbyteArray,
    unknown: jbyteArray,
) -> jstring {
    let playlist = string_from_byte_array(env, trackPlaylist);
    let unknown = string_from_byte_array(env, unknown);

    let path = string_from_byte_array(env, trackPath)
        .split('/')
        .collect::<Vec<_>>()
        .into_iter()
        .rev()
        .nth(1)
        .unwrap_unchecked()
        .to_string();

    if path == playlist || playlist == "<unknown>" {
        let unknown = unknown.encode_utf16().collect::<Vec<_>>();
        (**env).NewString.unwrap_unchecked()(env, unknown.as_ptr(), unknown.len() as jsize)
    } else {
        let playlist = playlist.encode_utf16().collect::<Vec<_>>();
        (**env).NewString.unwrap_unchecked()(env, playlist.as_ptr(), playlist.len() as jsize)
    }
}

/// Creates string from jbyteArray
///
/// # Arguments
/// jstr - String from java that is casted to byte array
///
/// # Return
/// Rust's string from give Java's byte string

#[inline]
unsafe fn string_from_byte_array(env: *mut JNIEnv, jstr: jbyteArray) -> String {
    let len = (**env).GetArrayLength.unwrap_unchecked()(env, jstr) as usize;
    String::from_raw_parts(
        (**env).GetByteArrayElements.unwrap_unchecked()(env, jstr, &mut 0) as *mut u8,
        len,
        len,
    )
}

/// Creates string from jstring
///
/// # Arguments
/// jstr - String from java
///
/// # Return
/// Rust's string from give Java's string

#[inline]
unsafe fn string_from_jstring(env: *mut JNIEnv, jstr: jstring) -> String {
    let len = (**env).GetStringLength.unwrap_unchecked()(env, jstr) as usize;
    String::from_raw_parts(
        (**env).GetStringUTFChars.unwrap_unchecked()(env, jstr, &mut 0) as *mut c_char as *mut u8,
        len,
        len,
    )
}

/// Gets lyrics of some track by it's url from Genius
///
/// # Safety
/// Extern JNI function
///
/// # Arguments
/// url - url to track
///
/// # Return
/// Lyrics of this track or null if there are any errors

#[no_mangle]
#[allow(non_snake_case)]
pub unsafe extern "system" fn Java_com_dinaraparanid_prima_utils_rustlibs_NativeLibrary_getLyricsByUrl(
    env: *mut JNIEnv,
    _class: jclass,
    url: jstring,
) -> jstring {
    match genius_lyrics::get_lyrics_from_url_blocking(string_from_jstring(env, url).as_str()) {
        Ok(lyrics) => {
            let lyrics = lyrics.encode_utf16().collect::<Vec<_>>();

            (**env).NewString.unwrap_unchecked()(
                env,
                lyrics.as_ptr() as *const jchar,
                lyrics.len() as jsize,
            )
        }

        Err(_) => std::ptr::null_mut::<_jobject>(),
    }
}

/// Gets track's number (starting from zero) in album by album's URL
///
/// # Safety
/// Extern JNI function
///
/// # Arguments
/// url - url to track
/// track_title - track title as java's byte array
///
/// # Return
/// number of track (starting from zero)
/// or -1 if album doesn't have this track

#[no_mangle]
#[allow(non_snake_case)]
pub unsafe extern "system" fn Java_com_dinaraparanid_prima_utils_rustlibs_NativeLibrary_getTrackNumberInAlbum(
    env: *mut JNIEnv,
    _class: jclass,
    url: jstring,
    track_title: jbyteArray,
) -> jbyte {
    match track_album_number_genius::get_track_number_in_album_blocking(
        string_from_jstring(env, url).as_str(),
        string_from_byte_array(env, track_title).as_str(),
    ) {
        None => -1,
        Some(x) => x as jbyte,
    }
}
