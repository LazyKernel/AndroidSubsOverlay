# SubsOverlay

## :warning: This project is a proof of concept :warning:
```
This project is a proof of concept and thus will most likely break often. 
Since it also uses Android's Accessibility Service, some bugs might leave
overlays drawn on top of everything, requiring a restart or a manual removal
by developer tools / boot menu. Use at your own risk.
```

## About the project

### Overview
Android has a really cool feature that let's developers build accessibility services to help disabled users.
[Accessibility services](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService) 
are given a *very* large set of options for interacting with the OS itself and other applications running on 
the phone. These services can opt in to listen for events, like buttons being clicked or views being drawn.
**For every. Single. App. Running.** Naturally it should stand that we can build a service and tell Android
we want to listen for events created by the Netflix app, right? 

Yes but the question is, How much data can we extract out of Netflix? Surprisingly, quite a bit. Netflix 
hasn't even made it that hard on us. Arguably the most important events have clearly named objects, classes 
are (usually) sourced from relevant native widgets giving us information by default and we can even get the 
current time (with a bit of math) straight from the accessibility events.

Life isn't all sunshine and rainbows, though. Some things, like directly getting subtitles out of the app,
just aren't possible. At least as far as I have tested. Other things, like detecting when the user exits
the Netflix media player, are somewhat possible, although in a very *very* janky way. 

### Features and limitations
Currently supports a very limited set of features with multiple caveats.

- Only supports Netflix, Crunchyroll and Wakanim at the moment
- Reads dictionaries in Yomichan format version 3, but only term bank entries 
  (aka words, no frequency lists or tags etc.)
- Requires manually selecting subtitles for each episode, only reads srt format
- Supports skipping

### Planned future additions
- Card export to Anki, audio recording / screenshots will not be supported due to DRM
(*audio recording works on Crunchyroll, and some apps on the Play Store can record audio from Netflix*)
- Forvo integration

## Downloads
For the latest version, [click here](https://github.com/LazyKernel/AndroidSubsOverlay/releases/latest).

All application apks can be found in [releases](https://github.com/LazyKernel/AndroidSubsOverlay/releases).

## Getting started
If you wish to willingly torture yourself with terrible code and janky solutions, follow these steps.

### Prerequisites
An Android development environment capable of building Gradle projects for an API level of at least 22.

### Installation
1. Clone the repo
```
git clone https://github.com/LazyKernel/AndroidSubsOverlay.git
```

2. Build the project using Gradle

## License
Distributed under the GNU GPLv3 License. See `LICENSE` for more information.

### 3rd party licenses
#### FuriganaTextView
FuriganaTextView is licensed under [Creative Commons BY-SA 3.0](http://creativecommons.org/licenses/by-sa/3.0/)