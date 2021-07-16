pub(crate) struct Artist {
    pub(crate) name: String,
}

impl Artist {
    pub fn new(name: String) -> Self {
        Self { name }
    }
}
