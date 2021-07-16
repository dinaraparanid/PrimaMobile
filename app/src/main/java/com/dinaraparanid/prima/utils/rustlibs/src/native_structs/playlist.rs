use crate::native_structs::track::Track;

use std::{
    ops::{Index, IndexMut},
    slice::{Iter, IterMut},
    vec::IntoIter,
};

pub(crate) struct Playlist {
    pub(crate) title: String,
    pub(crate) cur_index: usize,
    pub(crate) tracks: Vec<Track>,
}

impl Playlist {
    #[inline]
    pub fn new(title: String, tracks: Vec<Track>) -> Self {
        Self {
            title,
            cur_index: 0,
            tracks,
        }
    }

    #[inline]
    pub fn iter(&self) -> Iter<'_, Track> {
        self.tracks.iter()
    }

    #[inline]
    pub fn iter_mut(&mut self) -> IterMut<'_, Track> {
        self.tracks.iter_mut()
    }
}

impl From<String> for Playlist {
    #[inline]
    fn from(title: String) -> Self {
        Self {
            title,
            cur_index: 0,
            tracks: vec![],
        }
    }
}

impl Extend<Track> for Playlist {
    #[inline]
    fn extend<T: IntoIterator<Item = Track>>(&mut self, iter: T) {
        self.tracks.extend(iter)
    }
}

impl Index<usize> for Playlist {
    type Output = Track;

    #[inline]
    fn index(&self, index: usize) -> &Self::Output {
        self.tracks.index(index)
    }
}

impl IndexMut<usize> for Playlist {
    #[inline]
    fn index_mut(&mut self, index: usize) -> &mut Self::Output {
        self.tracks.index_mut(index)
    }
}

impl IntoIterator for Playlist {
    type Item = Track;
    type IntoIter = IntoIter<Track>;

    #[inline]
    fn into_iter(self) -> Self::IntoIter {
        self.tracks.into_iter()
    }
}
