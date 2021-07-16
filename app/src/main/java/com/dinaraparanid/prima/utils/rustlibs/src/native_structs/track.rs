pub(crate) struct Track {
    pub(crate) android_id: u64,
    pub(crate) title: String,
    pub(crate) artist: String,
    pub(crate) playlist: String,
    pub(crate) path: String,
    pub(crate) duration: u64,
    pub(crate) relative_path: Option<String>,
    pub(crate) display_name: Option<String>,
}

impl Track {
    pub fn new(
        android_id: u64,
        title: String,
        artist: String,
        playlist: String,
        path: String,
        duration: u64,
        relative_path: Option<String>,
        display_name: Option<String>,
    ) -> Self {
        Self {
            android_id,
            title,
            artist,
            playlist,
            path,
            duration,
            relative_path,
            display_name,
        }
    }
}
