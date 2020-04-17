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
| 0.1.10        | 31-3-2020 | 100%          | yes, in shape-linter  | 100%          | 0
| 0.1.9         | 25-3-2020 | 100%          | yes, in shape-linter  | 100%          | 0
| 0.1.8         | 22-3-2020 | 100%          | yes, in shape-linter  | 100%          | 0
| 0.1.7         | 21-3-2020 | 100%          | yes, in shape-linter  | 100%          | 11 (Mild)
| 0.1.6         | 18-3-2020 | 100%          | Yes, in shape-linter  | 100%          | NaN
| 0.1.1/0.15    | 12-3-2020 | Some          | Yes, in shape-linter  | Some          | NaN
| 0.1.0         | 10-3-2020 | Some          | None                  | None          | NaN
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
| 0.4.0         | 6-4-2020  | None          | Yes, in uu-uce        | Output,Usage  | 6 (Mild, wip)
| 0.3.0         | 22-3-2020 | None          | Yes, in uu-uce        | Output        | 0
| 0.2.2         | 18-3-2020 | None          | Yes, in uu-uce        | Output        | 9 (Severe)
| 0.2.1         | 17-3-2020 | None          | Yes, in uu-uce        | Output        | NaN
| 0.2.0         | 16-3-2020 | None          | None                  | Output        | NaN
| 0.1.0         | 10-3-2020 | None          | None                  | None          | NaN
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
| 0.4.1     | 6-4-2020  | Content format    | New Issues pile up        |
| 0.4.0     | 31-3-2020 | Content format    | Many things are unused    |
| 0.3.1     | 21-3-2020 | Content format    | Kotlin ok, Android issues |
| 0.x.x     | ...       | None              | Only small issues         |
| 0.0.1     | 8-2-2020  | None              | None                      |
### Test Coverage Matrix
| Version   | Pixel 2 Emulator API 29 | Nokia 2 API 24  | One Plus 5 API 28 | Pixel 3 Emulator API 29 | Pixel C API 27 |
|-----------|-------------------------|-----------------|-------------------|-------------------------|----------------|
| 0.4.2     | 100% tested             | 100% tested     | 100% tested       | 100% tested             | 100% tested    |
| 0.4.1     | 100% tested             | 100% tested     | 100% tested       | 100% tested             | 100% tested    |
| 0.3.1     | 100% tested             | 100% tested     | 100% tested       | 100% tested             | 100% tested    |
| 0.x.x     | 100% tested             | 100% tested     | Partial: GPS, Map | 100% tested             | None           |
| 0.0.1     | 100% tested             | None            | None              | None                    | None           |
### Test Issues Matrix
| Version   | Pixel 2 Emulator API 29   | Nokia 2 API 24            | One Plus 5 API 28 | Moto g5s+         | Pixel 3 Emulator API 29 | Pixel C API 27 |
|-----------|---------------------------|---------------------------|-------------------|-------------------|-------------------------|----------------|
| 0.4.2     | IK6, IK8                  | IK7, IK8                  | IK8               |                   | IK8                     | IK8            |
| 0.4.1     | IK6                       | IK7                       | None              |                   | None                    | IK6            |
| 0.4.0     | None                      |                           | None              |                   | None                    | IK6            |
| 0.3.1     | IK2                       | None                      | None              | IK1, IK2          | None                    | IK6            |
| 0.x.x     | None                      | Popup UI scaling (IC3)    | Partialy Tested   | Partially tested  | Not tested              | Not tested     |
| 0.0.1     | None                      | Not Tested                | Not Tested        | Not tested        | Not tested              | Not tested     |
### Know Issues Matrix
| Version       | ID    | What              |
|---------------|-------|-------------------|
| 0.4.1         | IK8   | Crash on remove pin without image from fieldbook `java.lang.IllegalArgumentException: Uri lacks 'file' scheme`
| 0.4.1         | IK7   | Crash on load: `Only the original thread that created a view hierarchy can touch its views`
| 0.4.1         | IK6   | Image in fieldbook shows very small
| 0.3.3         | IK4   | Empty screen comes up when you return from pin screen
| 0.3.2         | IK3   | App crashes when opening pin from menu
| 0.3.1         | IK2   | Text in pins partially overlaps
| 0.3.1         | IK1   | Menu covers whole screen when phone is closed while running (on some devices)
| 0.3.1         | IK0   | Can't open popup from pin list menu
### Quality Checks Matrix
| Since Version | ID    | What to check                                         | Automated |
|---------------|-------|-------------------------------------------------------|-----------|
| 0.4.2         | IC20  | Cancel pin closing when progress is about to be lost  | Yes
| 0.4.2         | IC19  | Close pin when no progress is made in quiz            | Yes
| 0.4.2         | IC18  | Close pin when progress is made in quiz               | Yes
| 0.4.2         | IC17  | Complete multiple choice quiz                         | Yes
| 0.4.2         | IC16  | Fail multiple choice quiz                             | Yes
| 0.4.2         | IC15  | Open multiple choice quiz                             | Yes
| 0.4.1         | IC14  | Delete fieldbook pin without image                    | Yes
| 0.4.1         | IC13  | Delete fieldbook pin with image                       | No
| 0.4.1         | IC12  | Open fieldbook item, ui scaling                       | Yes
| 0.4.1         | IC11  | Add fieldbook item                                    | No
| 0.4.1         | IC10  | Open fieldbook                                        | Yes
| 0.3.2         | IC9   | Open video via video pin                              | Yes
| 0.3.2         | IC8   | Open pin via menu                                     | Yes
| 0.3.1         | IC7   | Pins drawn, click on pin to open popup                | No
| 0.x.x         | IC6   | Pin list menu, UI scaling                             | Yes
| 0.x.x         | IC5   | Enable, disable map layers                            | No
| 0.x.x         | IC4   | Swipe up menu                                         | Yes
| 0.x.x         | IC3   | Popup window UI scaling, UI scaling in general        | No
| 0.x.x         | IC2   | Map double tap to zoom out, go to position            | No
| 0.x.x         | IC1   | Map zooming, scrolling                                | No
| 0.x.x         | IC0   | Map LOD                                               | No
| 0.0.1         |       | None                                                  | -
### Detailed Linting Status
| Version   | Linting Details Kotlin: (Nr, Worst) Android: (Nr, Worst)  |
|-----------|-----------------------------------------------------------|
| 0.4.2     | Small decrease in issues, Kotlin: (7, mild) Android (61: Medium)
| 0.4.1     | Issues expand again, Kotlin: (12, mild) Android (80: Major)
| 0.4.0     | Mostly unused ... errors, Kotlin: (7, mild) Android: (58: Medium)
| 0.3.1     | Many Issues, Kotlin: (8, Mild) Android: (69, Major)
| 0.x.x     | Kotlin minimal issues, Android increasing issues
| 0.0.1     | Clean
