use std::{env, fs::File, io::Write, path::Path, process::Command};

// app/src/main/cpp/version is the single source of the KernelPatch version;
// both this crate and the root build.gradle.kts derive their copy from it.
fn get_kp_version() -> (u32, u32, u32) {
    let header = std::fs::read_to_string("../app/src/main/cpp/version")
        .expect("Failed to read ../app/src/main/cpp/version");
    let parse = |name: &str| -> u32 {
        header
            .lines()
            .find_map(|line| {
                line.strip_prefix(format!("#define {name} ").as_str())
                    .and_then(|v| v.trim().parse().ok())
            })
            .unwrap_or_else(|| panic!("{name} not found in app/src/main/cpp/version"))
    };
    (parse("MAJOR"), parse("MINOR"), parse("PATCH"))
}

fn get_git_version() -> Result<(u32, String), std::io::Error> {
    // Try to get version code from environment variable first
    let version_code: u32 = if let Ok(env_version_code) = env::var("APATCH_VERSION_CODE") {
        env_version_code.parse().map_err(|_| {
            std::io::Error::new(std::io::ErrorKind::Other, "Failed to parse {version_code}")
        })?
    } else {
        // Fallback to git-based calculation
        let output = Command::new("git")
            .args(["rev-list", "--count", "HEAD"])
            .output()?;

        let output = output.stdout;
        let git_count = String::from_utf8(output).expect("Failed to read git count stdout");
        let git_count: u32 = git_count.trim().parse().map_err(|_| {
            std::io::Error::new(std::io::ErrorKind::Other, "Failed to parse git count")
        })?;
        std::cmp::max(11000 + 200 + git_count, 10762) // For historical reasons and ensure minimum version
    };

    let version_name = if let Ok(env_version_name) = env::var("APATCH_VERSION_NAME") {
        env_version_name
    } else {
        "113005-Matsuzaka-yuki".to_string()
    };

    Ok((version_code, version_name))
}

fn main() {
    // update VersionCode when git repository change
    println!("cargo:rerun-if-changed=../.git/HEAD");
    println!("cargo:rerun-if-changed=../.git/refs/");
    println!("cargo:rerun-if-changed=../app/src/main/cpp/version");

    let (code, name) = match get_git_version() {
        Ok((code, name)) => (code, name),
        Err(_) => {
            // show warning if git is not installed
            println!("cargo:warning=Failed to get git version, using 0.0.0");
            (0, "0.0.0".to_string())
        }
    };
    let out_dir = env::var("OUT_DIR").expect("Failed to get $OUT_DIR");
    println!("out_dir: ${out_dir}");
    println!("code: ${code}");
    let out_dir = Path::new(&out_dir);
    File::create(Path::new(out_dir).join("VERSION_CODE"))
        .expect("Failed to create VERSION_CODE")
        .write_all(code.to_string().as_bytes())
        .expect("Failed to write VERSION_CODE");

    File::create(Path::new(out_dir).join("VERSION_NAME"))
        .expect("Failed to create VERSION_NAME")
        .write_all(name.trim().as_bytes())
        .expect("Failed to write VERSION_NAME");

    let (major, minor, patch) = get_kp_version();
    File::create(Path::new(out_dir).join("kp_version.rs"))
        .expect("Failed to create kp_version.rs")
        .write_all(
            format!(
                "pub const KP_MAJOR: i64 = {major};\n\
                 pub const KP_MINOR: i64 = {minor};\n\
                 pub const KP_PATCH: i64 = {patch};\n"
            )
            .as_bytes(),
        )
        .expect("Failed to write kp_version.rs");
}
