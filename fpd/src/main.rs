mod prop_patch;
mod umount;

const VERSION: &str = env!("CARGO_PKG_VERSION");
const HELP_URL: &str = "https://fp.mysqil.com/";

fn print_version() {
    println!("v{VERSION}");
}

fn print_help() {
    println!("Please read the FolkPatch documentation at {HELP_URL} for help.");
}

fn usage() -> ! {
    eprintln!("Usage: fpd [-version] [-hide] [-help] [-umount]");
    std::process::exit(1);
}

fn main() {
    let args: Vec<String> = std::env::args().collect();

    if args.len() == 1 {
        let exe = &args[0];
        let name = std::path::Path::new(exe)
            .file_name()
            .and_then(|n| n.to_str())
            .unwrap_or("");
        match name {
            "hide" => return prop_patch::run(),
            "umount" => std::process::exit(umount::run() as i32),
            _ => {}
        }
    }

    let mut flags = args.iter().skip(1);
    let flag = flags.next();
    if flags.next().is_some() {
        usage();
    }

    match flag.map(|s| s.as_str()) {
        None => print_help(),
        Some("-version") => print_version(),
        Some("-hide") => prop_patch::run(),
        Some("-help") => print_help(),
        Some("-umount") => std::process::exit(umount::run() as i32),
        Some(_) => usage(),
    }
}
