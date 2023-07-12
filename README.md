# MP3_Downloader  
Mp3 downloader for youtube videos, user can put link of any youtube video and the application will download the mp3 file or the audio file of that youtube video in the desired location selected by the user.

APK Link ->  
https://drive.google.com/file/d/1USY8CrkPUeglx_OPc7VBWytQFHi2Ht6W/view?usp=sharing  
(If app is getting blocked from installation, click on 'More Details' and then click 'Install Anyway')

Languages - Kotlin, Python  
Python libraries -> yt-dlp  
                Kotlin libraries ->  okhttp, ffmpeg, chaquopy

# yt-dlp
is used to get the information about the youtube video, and it uses python and the python script for it has been accomodated in the project 
and is being run through the use of chaquopy library, which helps to run python code in android.

# okhttp
library is being used to download the media file for the MP3 file, it is using the technique of downloading using multiple concurrent connections
to make the full use of the download speed, and download optimally thereby, increasing the whole download speed.

# ffmpeg
library is used to convert the downloaded media into MP3 file and its progress is being shown continuously to the user.
