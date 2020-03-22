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
| Version       | Date      | Unit tests    | Realworld tests       | Documentation | Lint Issues (Worst)   |
| ------------- | --------- | ------------- | --------------------- | ------------- | --------------------- |
| 0.1.8         | 22-3-2019 | 100%          | yes, in shape-linter  | 100%          | 0
| 0.1.7         | 21-3-2019 | 100%          | yes, in shape-linter  | 100%          | 11 (Mild)
| 0.1.6         | 18-3-2019 | 100%          | Yes, in shape-linter  | 100%          | NaN
| 0.1.1/0.15    | 12-3-2019 | Some          | Yes, in shape-linter  | Some          | NaN
| 0.1.0         | 10-3-2019 | Some          | None                  | None          | NaN
## shapefile-linter
### Tech Stack
<span style="color:#88f">Language: </span><span style="color:#f88">Rust</span></br>
<span style="color:#88f">Platform: </span><span style="color:#f88">Linux</span></br>
<span style="color:#88f">Type: </span><span style="color:#f88">Standalone</span></br>

### Links
- [shapefile-linter on github](https://github.com/ocdy1001/shapefile-linter)
### QA Matrix
| Version       | Date      | Unit tests    | Realworld tests       | Documentation | Lint Issues (Worst)   |
|---------------|-----------|---------------|-----------------------|---------------|-----------------------|
| 0.1.4         | 22-3-2019 | None          | Yes, in uu-uce        | Output        | 0
| 0.1.3         | 18-3-2019 | None          | Yes, in uu-uce        | Output        | 9 (Severe)
| 0.1.2         | 17-3-2019 | None          | Yes, in uu-uce        | Output        | NaN
| 0.1.1         | 16-3-2019 | None          | None                  | Output        | NaN
| 0.1.0         | 10-3-2019 | None          | None                  | None          | NaN
## uu-uce
<span style="color:#88f">Language: </span><span style="color:#f88">Kotlin</span></br>
<span style="color:#88f">Development Platform: </span><span style="color:#f88">Linux / Windows</span></br>
<span style="color:#88f">Build Platform: </span><span style="color:#f88">Android</span></br>
<span style="color:#88f">Type: </span><span style="color:#f88">App</span></br>

### Links
- [uu-uce on github](https://github.com/ocdy1001/uu-uce)
### QA Matrix
| Version   | Date      | Documentation     | Linting status            |
|-----------|-----------|-------------------|---------------------------|
| 0.2.1     | 21-3-2019 | Content format    | Kotlin ok, Android issues |
| 0.x.x     | ...       | None              | Only small issues         |
| 0.0.1     | 8-2-2019  | None              | None                      |
### Test Coverage Matrix
| Version   | Pixel 2 Emulator API 10 | Nokia 2 API 24  | One Plus  |
|-----------|-------------------------|-----------------|-----------|
| 0.2.1     | 100% tested | 100% tested | 100% tested
| 0.x.x     | 100% tested | 100% tested | Partial: GPS, Map
| 0.0.1     | 100% tested | None | None
### Test Issues Matrix
| Version   | Pixel 2 Emulator API 29 | Nokia 2 API 24  | One Plus  |
|-----------|-------------------------|-----------------|-----------|
| 0.2.1     | None | None | None
| 0.x.x     | None | Popup UI scaling | Partialy Tested
| 0.0.1     | None | Not Tested | Not Tested
### Detailed Linting Status
| Version   | Linting Details Kotlin: (Nr, Worst) Android: (Nr, Worst)  |
|-----------|-----------------------------------------------------------|
| 0.2.1     | Many Issues, Kotlin: (8, Mild) Android: (69, Major)
| 0.x.x     | Kotlin minimal issues, Android increasing issues
| 0.0.1     | Clean