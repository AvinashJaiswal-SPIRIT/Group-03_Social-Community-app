# Smart Community – API (Auth + Dashboard + Communities)

Base URL (local): `http://localhost:8080`
All bodies are JSON. Timestamps are ISO-8601 strings (e.g. `2026-10-19T05:30:00Z`).

## How authentication works (summary)

1. `POST /api/auth/register` → user gets an OTP email
2. `POST /api/auth/verify-otp` → email verified
3. `POST /api/auth/login` → returns `accessToken` + `refreshToken`
4. Send the access token on **every other request**: `Authorization: Bearer <accessToken>`
5. Access token lasts 15 minutes. When any request returns **401**, call `POST /api/auth/refresh`
   with `{ "refreshToken": "..." }` to get a new pair, then retry. If refresh also fails → go to login.

## Error format (same for every endpoint)

```json
{ "status": 409, "message": "You are already a member of this community", "errors": null }
```

Validation errors (400) fill `errors` per field:

```json
{ "status": 400, "message": "Validation failed", "errors": { "name": "size must be between 3 and 50" } }
```

| Status | Meaning |
|-------|---------|
| 400 | Validation failed / bad request |
| 401 | Missing, invalid or expired token (or wrong password on login) |
| 403 | Not allowed (e.g. trying to join a private community by id) |
| 404 | Not found (also returned for a private community you are not in) |
| 409 | Duplicate (name already taken, already a member) |

---

## The community card (used everywhere)

```json
{
  "id": 7,
  "name": "Java Developers",
  "description": "Discuss Java, Spring Boot, and backend development.",
  "iconUrl": null,
  "visibility": "PUBLIC",
  "memberCount": 124,
  "myRole": "MEMBER",
  "inviteCode": null,
  "createdAt": "2026-10-19T05:30:00Z"
}
```

- `visibility`: `PUBLIC` | `PRIVATE`
- `myRole`: `OWNER` | `MODERATOR` | `MEMBER`, or **`null` if the current user has not joined**
  (use it to decide between showing a role badge or a **Join** button).
  The "Admin" badge in the design = `OWNER`.
- `inviteCode`: only present when `myRole` is `OWNER` or `MODERATOR`, otherwise `null`.
- `iconUrl`: may be `null` → show a default icon.

## Paginated lists

Lists return:

```json
{ "items": [ /* community cards */ ], "page": 0, "size": 10, "totalItems": 25, "totalPages": 3 }
```

`page` starts at **0**. `size` max is 50.

---

## 1. Dashboard

### `GET /api/dashboard`

Everything the Home page needs in one call.

```json
{
  "user": { "id": 1, "displayName": "Tarun", "email": "tarun@example.com" },
  "stats": {
    "joinedCommunities": 3,
    "availableCommunities": 5,
    "unreadMessages": 0,
    "newNotifications": 0
  },
  "myCommunities":       [ /* up to 3 community cards */ ],
  "discoverCommunities": [ /* up to 3 community cards */ ],
  "communityStats": { "totalCommunities": 8, "totalMembers": 482, "yourRole": "USER" }
}
```

| UI element | Field |
|-----------|-------|
| "Good morning, Tarun!" | `user.displayName` |
| Joined Communities card | `stats.joinedCommunities` |
| Available Communities card | `stats.availableCommunities` (public communities you haven't joined) |
| Unread Messages / New Notifications cards | `stats.unreadMessages` / `stats.newNotifications` – **always 0 for now** (messaging & notifications are not built yet) |
| My Communities row | `myCommunities` |
| Discover Communities row | `discoverCommunities` |
| Community Stats panel | `communityStats` (`yourRole` is the platform role: `USER` / `ADMIN`) |

"View all" links use the endpoints below.

---

## 2. Communities

### `POST /api/communities` — create (→ 201)

```json
{
  "name": "Java Developers",
  "description": "Discuss Java and Spring Boot",
  "iconUrl": null,
  "visibility": "PUBLIC"
}
```

- `name`: required, 3–50 chars, must be unique (case-insensitive) → otherwise **409**
- `description`: optional, max 255
- `iconUrl`: optional, max 500
- `visibility`: required, `PUBLIC` or `PRIVATE`

Returns the community card with `myRole: "OWNER"` and the `inviteCode` (show/copy it to the owner).

### `GET /api/communities/my?page=0&size=10` — My Communities ("View all")

Paginated cards of every community the user has joined (newest join first).

### `GET /api/communities/discover?search=java&page=0&size=10` — Discover Communities ("View all" + search box)

Paginated cards of **public** communities the user has **not** joined. `search` is optional
(matches part of the name, case-insensitive). `myRole` is always `null` here.

### `GET /api/communities/{id}` — details

Returns one card. A private community you are not in returns **404**.

### `POST /api/communities/{id}/join` — join a public community (the "Join" button)

No body. Returns the updated card (`myRole: "MEMBER"`, `memberCount` increased).
Errors: 404 not found · 403 community is private (use invite code) · 409 already a member.

### `POST /api/communities/join` — join with invite code (the "Join Community" quick action)

```json
{ "inviteCode": "K7P2QX9M" }
```

Works for public **and** private communities. Not case-sensitive. Returns the updated card.
Errors: 404 `Invalid invite code` · 409 already a member.

### `DELETE /api/communities/{id}/leave` — leave (→ 204, no body)

Errors: 404 not a member · 400 the owner cannot leave their own community.

---

## Not built yet (will be added later)

Messages, notifications (and the "Recent Notifications" panel), channels, members list,
roles management, moderation. Settings and Recent Activity were intentionally left out.
