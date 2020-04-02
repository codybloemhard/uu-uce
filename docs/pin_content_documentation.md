# Pin content 

The content of a pin consists of multiple content blocks,
this allows the user the ability to combine any amount of types of content in a single pin.

## Types of content blocks
There are 3 different kinds of pin content blocks:
1. Text content
    Use: 		Displaying a string of text in the content of a pin.
    
    Parameters: `tag`, `text`
2. Image content
    Use:		Displaying an image in the content of a pin.
    
    Parameters: `tag`, `file_path`
3. Video content
    Use: 		Displaying a video in the content of a pin.
    
    Parameters: `tag`, `file_path`, `thumbnail(?)`, `title(?)`
    (Parameters that are followed by `(?)` are optional and the `(?)` is not part of the parameter)

## Format
We use a JSON text format for storing the pin content, it works as follows:

The string starting off the content of a pin should always start with an opening square bracket `[`.
A content block is started by an opening bracket `{`.

A parameter for a content block is given in quotation marks `"param"`.
A parameter should always be assigned a value by adding a colon `:`.

To finish assigning a value to a parameter enter a value in quotation marks `"Value"`.
If you want to assign a value to another parameter an assignment should be followed by a comma `,`.
To close a content block place a closing bracket `}`.

To create another content block follow the previous content block up with a comma `,` just like with parameter assignments.

To close the pin content end with a closing square bracket `]`.

## Example
The following example will generate a block of text followed by an image and finally a video:
```json
[
    {
        "tag"			: "TEXT",
        "content"       : "Put your text here"
    },
    {
        "tag" 			: "IMAGE",
        "file_path"	    : "file:///data/data/com.uu_uce/files/designated_directory/images/your_image_name.png"
    },
    {
        "tag" 			: "VIDEO",
        "file_path"	    : "file:///data/data/com.uu_uce/files/designated_directory/videos/your_video_name.mp4",
        "thumbnail"     : "file:///data/data/com.uu_uce/files/designated_directory/videos/thumbnails/your_thumbnail_name.png",
        "title"			: "Put video title here"
    }
]
```

Notes: 
- The video content block has the optional parameters thumbnail and title, these can be left out although not recommended.
- The order in which parameters are assigned is arbitrary.


## File location (developer only)
The entire file path (formated as Uri) is saved in the JSON file. All content for the pins is stored to
```file:///data/data/com.uu_uce/files/pin_content/```
Additional file path is determined depending on the kind of file:
- Images are stored in the: `images/` folder
- Videos are stored in the: `videos/` folder
- Thumbnails are stored in the: `videos/thumbnails` folder