from yt_dlp import YoutubeDL

def get_video_context(video_url):
    ydl_opts = {
        'format': 'bestaudio/best',
        'postprocessors': [{
            'key': 'FFmpegExtractAudio',
            'preferredcodec': 'mp3',
            'preferredquality': '192',
        }],
    }

    with YoutubeDL(ydl_opts) as ydl:
        info = ydl.extract_info(video_url, download=False)
        title = info.get('title')
        description = info.get('description')
        thumbnail = info.get('thumbnail')
        view_count = info.get('view_count')
        like_count = info.get('like_count')
        mp3_url = info.get('url')

    return title, description, thumbnail, view_count, like_count, mp3_url
