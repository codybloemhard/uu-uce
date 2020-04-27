# Pin content 

The content of a pin consists of multiple content blocks,
this allows the user the ability to combine any amount of types of content in a single pin.

## Types of content blocks
There are 4 different kinds of pin content blocks:
1. Text content
    Use: 		Displaying a string of text in the content of a pin.
    
    Parameters: `tag`, `text`
2. Image content
    Use:		Displaying an image in the content of a pin.
    
    Parameters: `tag`, `file_path`, `thumbnail(?)`
3. Video content
    Use: 		Displaying a video in the content of a pin.
    
    Parameters: `tag`, `file_path`, `thumbnail(?)`, `title(?)`
4. Mutliple choice quiz
	Use: 		Inserting buttons with different answers in the content of a pin.
	
	Parameters:	`tag`, `mc_correct_option`, `,mc_incorrect_option`, `reward`

_Parameters that are followed by `(?)` are optional and the `(?)` is not part of the parameter_

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
    },
	{
        "tag" 					: "MCQUIZ",
        "mc_correct_option"		: "The correct answer to the question",
        "mc_incorrect_option" 	: "An incorrect answer to the question",
        "reward"                : 50
    }
]
```

Notes: 
- The video content block has the optional parameters thumbnail and title, these can be left out although not recommended.
- The order in which parameters are assigned is arbitrary.
- A mutiple choice quiz is only valid if it has at least one correct and one incorrect answer.


## File location (developer only)
The entire file path (formated as Uri) is saved in the JSON file, for the thumbnails and file locations of videos and images

#### Pins
Pins are created by the teacher. All content for the pins is stored to
```file:///data/data/com.uu_uce/files/pin_content/```. It's not available in the gallery or any other way
Additional file path is determined depending on the kind of file:
- Images are stored in the: `images/` folder
- Videos are stored in the: `videos/` folder
- Thumbnails are stored in the: `videos/thumbnails` folder

#### Fieldbook
The fieldbook also makes use of PinContent. The pictures and images in the fieldbook are made by the users and should always be available to them (through the gallery). Content that's created outside of the UCE environment isn't moved and the file_path will reference its location in the camera directory, through a content uri.

Content created inside of the UCE environment is stored to ```content://com.uu-uce.fileprovider/fieldbook/```, which refers to ```/storage/emulated/0/UU-UCE/Fieldbook```. This content is made available to the gallery and they are independent of the app. Additional file path is determined depending on the kind of file:
- Images are stored in the `Pictures/` folder
- Videos are stored in the `Videos/` folder

To ensure that the user can still use its fieldbook, even after/if some media have been deleted/become unavailable, we store a thumbnail for all media added to the fieldbook, to ```file:///storage/emulated/0/Android/data/com.uu_uce/files/Fieldbook/Thumbnails/```. This directory is private to the app - and meant for runtime reading/writing - and all of its contents will be deleted on app deletion. The thumbnail name should - apart from exceptions - be equal to the original file name.