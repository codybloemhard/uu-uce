# QA
In this document we document how well everythings has been tested and documented.
QA stands for quality assurance.
## bin-buffer
A rust crate to put primitves into a buffer.
Used by shapefile-linter to save it's custom files for geological data.
### Tech Stack
<span style="color:#88f">Language: </span><span style="color:#f88">Rust</span></br>
<span style="color:#88f">Platform: </span><span style="color:#f88">Cross platform</span></br>
<span style="color:#88f">Type: </span><span style="color:#f88">Library</span></br>
### Links
- [bin-buffer on crates.io](https://crates.io/crates/bin_buffer)
- [bin-buffer on rust docs](https://docs.rs/bin_buffer/)
- [bin-buffer on github](https://github.com/ocdy1001/bin-buffer)
### QA Matrix
| Version       | Date      | Unit tests    | Realworld tests       | Documentation |
| ------------- | --------- | ------------- | --------------------- | ------------- |
| 0.1.7         | 21-3-2019 | 100%          | yes, in shape-linter  | 100%          |
| 0.1.6         | 18-3-2019 | 100%          | Yes, in shape-linter  | 100%          |
| 0.1.1/0.15    | 12-3-2019 | Some          | Yes, in shape-linter  | Some          |
| 0.1.0         | 10-3-2019 | Some          | None                  | None          |
## shapefile-linter
### Tech Stack
<span style="color:#88f">Language: </span><span style="color:#f88">Rust</span></br>
<span style="color:#88f">Platform: </span><span style="color:#f88">Linux</span></br>
<span style="color:#88f">Type: </span><span style="color:#f88">Standalone</span></br>
### Links
- [shapefile-linter on github](https://github.com/ocdy1001/shapefile-linter)
### QA Matrix
| Version       | Date      | Unit tests    | Realworld tests       | Documentation |
|---------------|-----------|---------------|-----------------------|---------------|
| 0.1.3         | 18-3-2019 | None          | Yes, in uu-uce        | Output        |
| 0.1.2         | 17-3-2019 | None          | Yes, in uu-uce        | Output        |
| 0.1.1         | 16-3-2019 | None          | None                  | Output        |
| 0.1.0         | 10-3-2019 | None          | None                  | None          |
## uu-uce
<span style="color:#88f">Language: </span><span style="color:#f88">Kotlin</span></br>
<span style="color:#88f">Development Platform: </span><span style="color:#f88">Linux/Windows</span></br>
<span style="color:#88f">Build Platform: </span><span style="color:#f88">Android</span></br>
<span style="color:#88f">Type: </span><span style="color:#f88">App</span></br>
### Links
- [uu-uce on github] (https://github.com/ocdy1001/uu-uce)
### QA Matrix
| Version   | Date  | Documentation     | Unit Tests    |