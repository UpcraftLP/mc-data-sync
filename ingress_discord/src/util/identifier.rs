use anyhow::bail;
use lazy_static::lazy_static;
use regex::Regex;
use std::fmt::Display;
use std::str::FromStr;

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Identifier {
    pub namespace: String,
    pub path: String,
}

lazy_static! {
    static ref NAMESPACE_REGEX: Regex = Regex::new(r#"^[a-z0-9_.-]+$"#).unwrap();
    static ref PATH_REGEX: Regex = Regex::new(r#"^[a-z0-9/._-]+$"#).unwrap();
}

impl FromStr for Identifier {
    type Err = anyhow::Error;

    fn from_str(s: &str) -> Result<Self, Self::Err> {
        let parts: Vec<&str> = s.split(':').collect();
        if parts.len() < 2 {
            bail!("missing namespace");
        }

        let namespace = parts[0].to_string();
        let path = &parts[1..].join("");

        if !NAMESPACE_REGEX.is_match(&namespace) {
            bail!(r#"namespace `{namespace}` does not match `^[a-z0-9/._-]+$`"#);
        }

        if !PATH_REGEX.is_match(path) {
            bail!(r#"path `{path}` does not match `^[a-z0-9/._-]+$`"#);
        }

        Ok(Identifier { namespace, path: path.clone() })
    }
}

impl Display for Identifier {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        write!(f, "{namespace}:{path}", namespace = self.namespace, path = self.path)
    }
}