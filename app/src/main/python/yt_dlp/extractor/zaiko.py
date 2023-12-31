from .common import InfoExtractor
from ..utils import (
    ExtractorError,
    extract_attributes,
    int_or_none,
    str_or_none,
    traverse_obj,
    unescapeHTML,
    url_or_none,
)


class ZaikoIE(InfoExtractor):
    _VALID_URL = r'https?://(?:[\w-]+\.)?zaiko\.io/event/(?P<id>\d+)/stream(?:/\d+)+'
    _TESTS = [{
        'url': 'https://zaiko.io/event/324868/stream/20571/20571',
        'info_dict': {
            'id': '324868',
            'ext': 'mp4',
            'title': 'ZAIKO STREAMING TEST',
            'alt_title': '[VOD] ZAIKO STREAMING TEST_20210603(Do Not Delete)',
            'uploader_id': '454',
            'uploader': 'ZAIKO ZERO',
            'release_timestamp': 1583809200,
            'thumbnail': r're:https://[a-z0-9]+.cloudfront.net/[a-z0-9_]+/[a-z0-9_]+',
            'release_date': '20200310',
            'categories': ['Tech House'],
            'live_status': 'was_live',
        },
        'params': {'skip_download': 'm3u8'},
    }]

    def _parse_vue_element_attr(self, name, string, video_id):
        page_elem = self._search_regex(rf'(<{name}[^>]+>)', string, name)
        attrs = {}
        for key, value in extract_attributes(page_elem).items():
            if key.startswith(':'):
                attrs[key[1:]] = self._parse_json(
                    value, video_id, transform_source=unescapeHTML, fatal=False)
        return attrs

    def _real_extract(self, url):
        video_id = self._match_id(url)

        webpage, urlh = self._download_webpage_handle(url, video_id)
        final_url = urlh.geturl()
        if 'zaiko.io/login' in final_url:
            self.raise_login_required()
        elif '/_buy/' in final_url:
            raise ExtractorError('Your account does not have tickets to this event', expected=True)
        stream_meta = self._parse_vue_element_attr('stream-page', webpage, video_id)

        player_page = self._download_webpage(
            stream_meta['stream-access']['video_source'], video_id,
            'Downloading player page', headers={'referer': 'https://zaiko.io/'})
        player_meta = self._parse_vue_element_attr('player', player_page, video_id)
        status = traverse_obj(player_meta, ('initial_event_info', 'status', {str}))
        live_status, msg, expected = {
            'vod': ('was_live', 'No VOD stream URL was found', False),
            'archiving': ('post_live', 'Event VOD is still being processed', True),
            'deleting': ('post_live', 'This event has ended', True),
            'deleted': ('post_live', 'This event has ended', True),
            'error': ('post_live', 'This event has ended', True),
            'disconnected': ('post_live', 'Stream has been disconnected', True),
            'live_to_disconnected': ('post_live', 'Stream has been disconnected', True),
            'live': ('is_live', 'No livestream URL found was found', False),
            'waiting': ('is_upcoming', 'Live event has not yet started', True),
            'cancelled': ('not_live', 'Event has been cancelled', True),
        }.get(status) or ('not_live', f'Unknown event status "{status}"', False)

        stream_url = traverse_obj(player_meta, ('initial_event_info', 'endpoint', {url_or_none}))
        formats = self._extract_m3u8_formats(
            stream_url, video_id, live=True, fatal=False) if stream_url else []
        if not formats:
            self.raise_no_formats(msg, expected=expected)

        return {
            'id': video_id,
            'formats': formats,
            'live_status': live_status,
            **traverse_obj(stream_meta, {
                'title': ('event', 'name', {str}),
                'uploader': ('profile', 'name', {str}),
                'uploader_id': ('profile', 'id', {str_or_none}),
                'release_timestamp': ('stream', 'start', 'timestamp', {int_or_none}),
                'categories': ('event', 'genres', ..., {lambda x: x or None}),
            }),
            **traverse_obj(player_meta, ('initial_event_info', {
                'alt_title': ('title', {str}),
                'thumbnail': ('poster_url', {url_or_none}),
            })),
        }
