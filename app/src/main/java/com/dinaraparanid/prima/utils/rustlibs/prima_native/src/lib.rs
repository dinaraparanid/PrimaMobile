extern crate genius_lyrics;
extern crate jni;
extern crate track_album_number_genius;

use std::iter::FromIterator;

use jni::{
    objects::{JString, ReleaseMode},
    sys::{_jobject, jbyte, jbyteArray, jclass, jint, jintArray, jstring},
    JNIEnv,
};

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
    env: JNIEnv,
    _class: jclass,
    name: JString,
) -> jstring {
    env.new_string(String::from_iter(
        string_from_jstring(env, name)
            .trim()
            .split_whitespace()
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
            }),
    ))
        .unwrap_unchecked()
        .into_raw()
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
    env: JNIEnv,
    _class: jclass,
    mut millis: jint,
) -> jintArray {
    let time = env.new_int_array(3).unwrap_unchecked();

    let h = millis / 3600000;
    millis -= h * 3600000;

    let m = millis / 60000;
    millis -= m * 60000;

    let s = millis / 1000;

    let arr = [h, m, s];

    env.set_int_array_region(time, 0, arr.as_slice())
        .unwrap_unchecked();
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
    env: JNIEnv,
    _class: jclass,
    trackPlaylist: JString,
    trackPath: JString,
    unknown: JString,
) -> jstring {
    let playlist = string_from_jstring(env, trackPlaylist);
    let unknown = string_from_jstring(env, unknown);

    let path = string_from_jstring(env, trackPath)
        .split('/')
        .collect::<Vec<_>>()
        .into_iter()
        .rev()
        .nth(1)
        .unwrap_unchecked()
        .to_string();

    if path == playlist || playlist == "<unknown>" {
        env.new_string(unknown).unwrap_unchecked().into_raw()
    } else {
        env.new_string(playlist).unwrap_unchecked().into_raw()
    }
}

/// Creates string from jbyteArray
///
/// # Deprecated
///
/// # Arguments
/// jstr - String from java that is casted to byte array
///
/// # Return
/// Rust's string from give Java's byte string

#[inline]
#[deprecated]
unsafe fn string_from_byte_array(env: JNIEnv, jstr: jbyteArray) -> String {
    let len = env.get_array_length(jstr).unwrap_unchecked() as usize;

    String::from_raw_parts(
        env.get_array_elements(jstr, ReleaseMode::NoCopyBack)
            .unwrap_unchecked()
            .as_ptr(),
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
unsafe fn string_from_jstring(env: JNIEnv, jstr: JString) -> String {
    env.get_string(jstr)
        .unwrap_unchecked()
        .to_str()
        .unwrap_unchecked()
        .to_string()
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
    env: JNIEnv,
    _class: jclass,
    url: JString,
) -> jstring {
    match genius_lyrics::get_lyrics_from_url_blocking(string_from_jstring(env, url).as_str()) {
        Ok(lyrics) => env.new_string(lyrics).unwrap_unchecked().into_raw(),
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
    env: JNIEnv,
    _class: jclass,
    url: JString,
    track_title: JString,
) -> jbyte {
    match track_album_number_genius::get_track_number_in_album_blocking(
        string_from_jstring(env, url).as_str(),
        string_from_jstring(env, track_title).as_str(),
    ) {
        None => -1,
        Some(x) => x as jbyte,
    }
}
