# Pin data 
Pin data is the class of data that is stored within the app database, it has 9 parameters:

## pinId : Int
This is the id of the pin, this id must be unique.
The pinId is used to refer to the pin by predecessors and following pins.

## location : String
The location on the map where the pin is to be located stored in a String. 
Location is given in UTM format, the northing ends with N and the easting with E.
### Example: 
`"31N3149680N46777336E"`

## difficulty : Int
The difficulty of the pin, determines the color of the pin.
	1:	Easy
	2: 	Medium
	3:	Hard

## type : String
The type of pin, this is used for determining which symbol should be displayed in the pin.
	"TEXT"	:	A pin with text as its most important piece of content.
	"VIDEO"	:	A pin with a video as its main piece of content.
	"IMAGE"	:	A pin with an image as its main piece of content.
	
## title : String
The title of the pin, this is displayed at the top of the pin screen when opened as well as in the pin menu.

## content : String
The content of the pin, see `pin_content_documentation.md` for a more information.

## status : Int 
The starting status of the pin, this represents if the pin is locked unlocked or completed.
	0:	Pin is locked 		This pin will require `predecessorIds` to be set to be able to be unlocked.
	1: 	Pin is unlocked 	This is the default state for pins which should always be visible.
	2:	Pin is completed 	

## predecessorIds : String
The id's of all pins that should be completed for this pin to unlock, -1 if no predecessors exist.
The list of predecessor pins should be given sepparated by commas (,).
### Example:
`"0,1,2"`

## followIds : String
The id's of all pins that are waiting for this pin (and maybe others) to unlock, -1 if no predecessors exist.
The list of following pins should be given sepparated by commas (,).
### Example:
`"0,1,2"`
	