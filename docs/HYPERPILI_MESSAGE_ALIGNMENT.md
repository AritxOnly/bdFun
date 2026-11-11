# Message center and home recommendation actions

Reference: Hyper-PiliPlus commit `0d08dd25bdeaa9a55e5a0e333154b681fd8048fe`.
The existing bdFun / Deadliner glass recipes, navigation chrome and message card appearance remain in use.

## Implemented behavior

- Home video cards open a long-press sheet with watch later, current-video feedback, creator feedback and the actual video category. Web feedback preserves the recommendation tracking ID. App feedback uses the server-provided `three_point_v2` dislike reason IDs and the existing signed access key.
- Only successful server feedback removes a video/creator from the visible feed. Category feedback uses the App reason when provided. When the recommendation source supplies no category reason, the sheet explicitly describes a **current-browsing-session local filter**, rather than claiming a server preference was updated. Subsequent pages resolve missing category metadata only while that filter is active, with bounded concurrency. Exclusions are not a durable blacklist.
- Message center keeps its shortcuts/cards and adds system notifications. Sessions and feeds use real lazy rows and server cursors. Filtering followed users no longer silently substitutes all users. Refresh failures retain content, retries retain the failed request mode, and an empty filtered page can still advance.
- Session long press supports pin/unpin, mute/unmute, read acknowledgement and removal. Mutations update local state after service success. A full read operation enumerates sessions, acknowledges each, opens notification feeds, then checks server unread counts before reporting success.
- Reply, mention and like feeds use `id` plus their corresponding time cursor; likes retain the latest/total buckets. Notification deletion uses the service's type enum, independent of the screen tab index. Like notifications can be muted per item.
- System notices use `message.bilibili.com/x/sys-msg/query_notify_list`, cursor acknowledgement and deletion. HTML content is rendered as text with HTTP(S) links, without a WebView. Dynamic notifications follow their supplied dynamic/opus link; video notifications retain comment/seek targeting.
- Private chats carry acknowledgement positions, page backwards by message sequence, refresh new messages only while resumed and preserve drafts on failure. Images are sent as image messages, not textual URLs. If an image sends but its caption fails, retry keeps only the unsent text. Withdrawn records render a withdrawal placeholder.
- Dynamic navigation uses MIUIX Album. Cache, history and watch-later actions use Download, Recent and SearchDevice; player watch-later/cache shortcuts use the same icons.

## Scope / service differences

Hyper-PiliPlus's newest session main/secondary screens use its Dart gRPC IM client. This implementation retains bdFun's authenticated HTTP session API and maps the message-center operations to the reference HTTP contracts; it does not claim wire-level gRPC parity or add bulk session deletion / the secondary inbox management screens. Existing text, image and video-sharing conversation rendering is retained.

## Source pointers

- Hyper-PiliPlus `lib/http/msg.dart`, `lib/pages/msg_feed_top/*/controller.dart`, `lib/pages/common/common_whisper_controller.dart`, `lib/http/video.dart`, `lib/models/home/rcmd/result.dart`.
- [Web recommendation feedback implementation](https://github.com/magicdawn/bilibili-app-recommend/blob/main/src/components/ModalDislike/api/index.ts) and [web reason mapping](https://github.com/magicdawn/bilibili-app-recommend/blob/main/src/components/ModalDislike/api/pc.md).
- [Session and private-message contract](https://github.com/bilibili-plugins/bilibili-api-collect/blob/master/docs/message/private_msg.md), particularly the inverted pin `op_type` (0 pin / 1 unpin) and sequence pagination.

## Validation

Unit checks cover the reply/mention/like cursor fields, server end/stuck cursors, the delete/pin enums, failed/partial/cancelled acknowledgements, duplicate acknowledgements and video/creator/category exclusion boundaries. Debug and optimized Release builds are checked separately.

Per the user's instructions, no emulator was used and no package was sent to Feishu. Authenticated service behavior and visual/touch behavior remain for physical-device verification: long-press dismissal and failure retries, following-only pagination, notification deletion and read counts, chat image/caption partial failures and history scrolling.
