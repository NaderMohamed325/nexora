# Function Reference

This file documents the public functions exposed by controllers and service interfaces.

## Response Envelope

All controllers use `ApiResponse<T>` with:

- `success: boolean`
- `message: string`
- `data: T` (nullable)
- `timestamp: LocalDateTime`

---

## Controllers (`src/main/java/com/neo/nexora/controller`)

### `AuthController` (`/api/auth`)

| Function | Method + Path | Auth | Purpose |
|---|---|---|---|
| `register` | `POST /register` | No | Register user and return JWT (`AuthResponse`). |
| `login` | `POST /login` | No | Authenticate and return JWT (`AuthResponse`). |
| `logout` | `POST /logout` | Yes | Blacklist current token. |
| `requestPasswordReset` | `POST /password-reset/request` | No | Generate password-reset token for email. |
| `confirmPasswordReset` | `POST /password-reset/confirm` | No | Reset password using reset token. |
| `changePassword` | `POST /change-password` | Yes | Change password for authenticated user. |
| `createAdmin` | `POST /create-admin` | Yes (`ADMIN`) | Create admin account. |

### `UserController` (`/api/users`)

| Function | Method + Path | Auth | Purpose |
|---|---|---|---|
| `getUserById` | `GET /{id}` | Yes | Fetch user DTO by ID. |
| `getUserByEmail` | `GET /email/{email}` | Yes (`ADMIN`) | Fetch user DTO by email. |
| `getAllUsers` | `GET /` | Yes | Paginated users with sort (`page,size,sortBy,sortDir`). |
| `searchUsers` | `GET /search?username=` | Yes | Username pattern search. |
| `updateUser` | `PUT /` | Yes | Update current authenticated user profile. |
| `deleteUser` | `DELETE /{id}` | Yes (`ADMIN`) | Permanently delete user by ID. |
| `deactivateAccount` | `PUT /deactivate` | Yes | Mark account as deactivated/scheduled flow. |
| `reactivateAccount` | `PUT /reactivate` | Yes | Reactivate deactivated account. |
| `uploadAvatar` | `POST /{id}/avatar` | Yes (`ADMIN` or same user) | Upload avatar to Cloudinary. |

### `PostController` (`/api/posts`)

| Function | Method + Path | Auth | Purpose |
|---|---|---|---|
| `createPost` | `POST /` (`multipart/form-data`) | Yes | Create post with optional media files. |
| `getAllPosts` | `GET /` | Yes | Paginated feed. |
| `getPostById` | `GET /{id}` | Yes | Fetch single post. |
| `getPostsByUserId` | `GET /user/{userId}` | Yes | List posts created by user. |
| `searchPosts` | `GET /search?keyword=` | Yes | Keyword search in title/content. |
| `getPostsByDateRange` | `GET /date-range` | Yes | Filter posts by created date range. |
| `getPostsByUserIdAndDateRange` | `GET /user/{userId}/date-range` | Yes | Date-filtered posts for one user. |
| `updatePost` | `PUT /{id}` (`multipart/form-data`) | Yes | Update post text/media. |
| `deletePost` | `DELETE /{id}` | Yes | Delete post (owner/admin). |

### `CommentController` (`/api/comments`)

| Function | Method + Path | Auth | Purpose |
|---|---|---|---|
| `addComment` | `POST /post/{postId}` | Yes | Add comment to post. |
| `getCommentsByPost` | `GET /post/{postId}` | Yes | Paginated comments for post. |
| `getCommentsByUser` | `GET /user/{userId}` | Yes | List comments authored by user. |
| `updateComment` | `PUT /{commentId}` | Yes | Update comment text (owner/admin). |
| `deleteComment` | `DELETE /{commentId}` | Yes | Delete comment (owner/admin). |

### `LikeController` (`/api/likes`)

| Function | Method + Path | Auth | Purpose |
|---|---|---|---|
| `togglePostLike` | `POST /post/{postId}` | Yes | Toggle like on post. |
| `getPostLikeCount` | `GET /post/{postId}/count` | Yes | Count likes on post. |
| `hasUserLikedPost` | `GET /post/{postId}/status` | Yes | Check current user like status on post. |
| `toggleCommentLike` | `POST /comment/{commentId}` | Yes | Toggle like on comment. |
| `getCommentLikeCount` | `GET /comment/{commentId}/count` | Yes | Count likes on comment. |
| `hasUserLikedComment` | `GET /comment/{commentId}/status` | Yes | Check current user like status on comment. |

### `SlugController` (`/api/slugs`)

| Function | Method + Path | Auth | Purpose |
|---|---|---|---|
| `createSlug` | `POST /` | Yes (`ADMIN`) | Create unique slug for an entity. |
| `resolveSlug` | `GET /{slug}/resolve` | Yes | Resolve slug and increment click count. |
| `getSlug` | `GET /{slug}` | Yes | Read slug without incrementing click count. |
| `slugExists` | `GET /{slug}/exists` | Yes | Check availability of slug value. |
| `getAllSlugs` | `GET /` | Yes (`ADMIN`) | Paginated slugs by click popularity. |
| `getSlugsByEntityType` | `GET /type/{entityType}` | Yes (`ADMIN`) | Paginated slugs by entity type. |
| `getTopSlugs` | `GET /top?limit=` | Yes | Top clicked slugs. |
| `deleteSlug` | `DELETE /{slug}` | Yes (`ADMIN`) | Delete slug by value. |

### `FollowController` (`/api/follow`)

| Function | Method + Path | Auth | Purpose |
|---|---|---|---|
| `followUser` | `POST /{followeeId}` | Yes | Follow another user. |
| `unfollowUser` | `DELETE /{followeeId}` | Yes | Unfollow user. |

---

## Service Interfaces (`src/main/java/com/neo/nexora/service`)

### `AuthService`

- `register(RegisterRequest request)`: create account and return token payload.
- `login(LoginRequest request)`: authenticate and return token payload.
- `logout(String token)`: blacklist JWT.
- `requestPasswordReset(PasswordResetRequest request)`: issue reset token.
- `confirmPasswordReset(PasswordResetConfirm request)`: apply reset token and new password.
- `changePassword(String username, ChangePasswordRequest request)`: change current user password.
- `createAdmin(RegisterRequest request)`: create admin account.

### `UserService`

- `getUserById(Long id)`: read user DTO by ID.
- `getUserByEmail(String email)`: read user DTO by email.
- `getAllUsers(Pageable pageable)`: paged user listing.
- `getUsersLike(String likeUserName)`: fuzzy username search.
- `updateUser(UserDetails userDetails, UserUpdateDto userUpdateDto)`: update current user.
- `deleteUserById(Long id)`: hard delete user.
- `deactivateAccount(UserDetails userDetails)`: deactivate + schedule deletion flow.
- `reactivateAccount(UserDetails userDetails)`: reactivate account.
- `extractUserById(Long id)`: return raw `User` entity by ID.

### `PostService`

- `createPost(UserDetails userDetails, String idempotencyKey, PostRequestDto requestDto, List<MultipartFile> fileList)`: create post with optional idempotency and media.
- `getPostsByUserId(Long id)`: user posts.
- `getAllPosts(int page, int size)`: paged post feed.
- `searchPosts(String keyword, int page, int size)`: keyword search.
- `getPostById(Long id)`: read one post.
- `deletePostById(UserDetails userDetails, Long id)`: owner/admin delete.
- `updatePost(UserDetails userDetails, Long id, PostRequestDto requestDto, PostMediaUpdateDto mediaUpdateDto, List<MultipartFile> fileList)`: update text/media.
- `getPostsByDateRange(LocalDateTime startDate, LocalDateTime endDate, int page, int size)`: date-filtered posts.
- `getPostsByUserIdAndDateRange(Long userId, LocalDateTime startDate, LocalDateTime endDate, int page, int size)`: date-filtered user posts.

### `CommentService`

- `addComment(UserDetails userDetails, String idempotencyKey, Long postId, CommentRequestDto requestDto)`: add comment to post.
- `getCommentsByPostId(Long postId, int page, int size)`: paged comments.
- `getCommentsByUserId(Long userId)`: user comments.
- `updateComment(UserDetails userDetails, Long commentId, CommentRequestDto requestDto)`: update comment.
- `deleteComment(UserDetails userDetails, Long commentId)`: delete comment.

### `LikeService`

- `togglePostLike(UserDetails userDetails, Long postId)`: post like/unlike toggle.
- `toggleCommentLike(UserDetails userDetails, Long commentId)`: comment like/unlike toggle.
- `getPostLikeCount(Long postId)`: aggregate likes.
- `getCommentLikeCount(Long commentId)`: aggregate likes.
- `hasUserLikedPost(UserDetails userDetails, Long postId)`: current user post-like status.
- `hasUserLikedComment(UserDetails userDetails, Long commentId)`: current user comment-like status.

### `SlugService`

- `createSlug(SlugRequestDto requestDto)`: create slug record.
- `resolveSlug(String slug)`: read + increment click count.
- `getSlug(String slug)`: read only.
- `getAllSlugs(int page, int size)`: paged slug list.
- `getSlugsByEntityType(EntityType entityType, int page, int size)`: paged by entity type.
- `getTopSlugs(int limit)`: most clicked slugs.
- `deleteSlug(String slug)`: remove slug.
- `slugExists(String slug)`: check if in use.

### `FollowService`

- `followUser(UserDetails userDetails, Long followeeId)`: create follow relation.
- `unfollowUser(UserDetails userDetails, Long followeeId)`: remove follow relation.

### `CloudinaryUploadService`

- `upload(MultipartFile file, String folder)`: generic upload helper.
- `uploadAvatar(MultipartFile file, Long userId)`: upload and bind avatar URL.
- `delete(String publicId)`: remove Cloudinary asset.
- `extractPublicId(String cloudinaryUrl)`: derive public ID from URL.

### `IdempotencyService`

- `validate(String key, String userId)`: reject duplicate requests for same key/user pair.

### Queue Services

#### `UserDeletionProducer`

- `scheduleUserDeletion()`: scheduled publisher for deletion batches.

#### `UserDeletionConsumer`

- `processDeletionBatch(UserDeletionBatchDto batch)`: queue consumer deletion handler.

#### `NotificationPublisher`

- `publishNotification(NotificationMessage message)`: publish generic notification.
- `notifyLike(Long recipientId, Long actorId, String actorUsername, String entityType, Long entityId)`: like notification helper.
- `notifyFollow(Long recipientId, Long actorId, String actorUsername)`: follow notification helper.
- `notifyComment(Long recipientId, Long actorId, String actorUsername, String entityType, Long entityId)`: comment notification helper.

#### `NotificationConsumer`

- `consume(NotificationMessage message)`: consume notification event.

---

## Implementation Classes (`*Impl`) and Runtime Behavior

Controllers are documented in the controller section above. This section adds the concrete implementation classes and what each public function does in practice.

### `AuthServiceImpl`

- `register(...)`: checks username with Bloom-filter fast path + DB, checks email uniqueness, creates `USER`, returns JWT response.
- `login(...)`: authenticates through `AuthenticationManager` and returns JWT payload.
- `logout(...)`: blacklists current JWT via `TokenBlacklistService`.
- `requestPasswordReset(...)`: delegates reset-token creation to `PasswordResetService`.
- `confirmPasswordReset(...)`: verifies password confirmation, then resets by token.
- `changePassword(...)`: verifies confirmation, loads user by username, then delegates password change.
- `createAdmin(...)`: same uniqueness checks as register, creates `ADMIN` user.

### `UserServiceImpl`

- `getUserById(...)`, `getUserByEmail(...)`: resolve user and map to `UserResponseDto`.
- `getAllUsers(...)`: paginated user list directly from repository.
- `getUsersLike(...)`: username search using repository matcher.
- `updateUser(...)`: updates authenticated user username/email; pushes new username into Bloom filter.
- `deleteUserById(...)`: existence check then hard delete.
- `deactivateAccount(...)`: marks account as `PENDING_DELETION`, sets deactivation + scheduled deletion timestamps.
- `reactivateAccount(...)`: restores `ACTIVE` and clears deletion/deactivation timestamps.
- `extractUserById(...)`: returns raw `User` entity.

### `PostServiceImpl`

- `createPost(...)`: validates idempotency key, resolves author, uploads optional media to Cloudinary, saves post.
- `getPostsByUserId(...)`, `getAllPosts(...)`, `searchPosts(...)`, `getPostById(...)`: read/query flows with DTO mapping.
- `deletePostById(...)`: enforces ownership, deletes linked Cloudinary assets, then deletes post.
- `updatePost(...)`: enforces ownership, updates text, removes selected media from Cloudinary, appends new media uploads.
- `getPostsByDateRange(...)`: defaults missing dates and validates range.
- `getPostsByUserIdAndDateRange(...)`: validates user exists, then filters by date range.

### `CommentServiceImpl`

- `addComment(...)`: resolves author/post, validates idempotency key, saves comment.
- `getCommentsByPostId(...)`, `getCommentsByUserId(...)`: validates target exists, returns mapped DTO list/page.
- `updateComment(...)`, `deleteComment(...)`: owner-or-admin authorization checks before mutate/delete.

### `LikeServiceImpl`

- `togglePostLike(...)`, `toggleCommentLike(...)`: toggle behavior (insert if missing, delete if present) and return updated counts.
- `getPostLikeCount(...)`, `getCommentLikeCount(...)`: validate target exists, then count likes.
- `hasUserLikedPost(...)`, `hasUserLikedComment(...)`: per-user status checks.

### `FollowServiceImpl`

- `followUser(...)`: adds followee to follower's `following` set if not already present.
- `unfollowUser(...)`: removes followee from follower's `following` set if present.

### `SlugServiceImpl`

- `createSlug(...)`: uniqueness check then create with initial click count 0.
- `resolveSlug(...)`: reads slug, increments click count using DB-side update, re-reads authoritative count.
- `getSlug(...)`, `getAllSlugs(...)`, `getSlugsByEntityType(...)`, `getTopSlugs(...)`: read/query variants.
- `deleteSlug(...)`: existence check then delete.
- `slugExists(...)`: availability check.

### `CloudinaryUploadServiceImpl`

- `upload(...)`: validates size/type (image+video), uploads to folder, returns cloud metadata.
- `uploadAvatar(...)`: validates image-only upload, removes previous avatar asset, uploads new one, persists URL on user.
- `delete(...)`: deletes asset by public ID.
- `extractPublicId(...)`: parses public ID from Cloudinary URL.

### `IdempotencyServiceImpl`

- `validate(...)`: stores `idempotency:<user>:<key>` in Redis with TTL (5 min); throws on duplicate submission.

### `UserLookUpServiceImpl`

- `init()`: rebuilds Bloom filter from usernames on startup and weekly scheduled refresh.
- `addUser(...)`: inserts username into Bloom filter.
- `mightContainUser(...)`, `shouldCheckDatabase(...)`: probabilistic lookup for username existence fast-path.

### Queue Implementations

#### `UserDeletionProducerImpl`

- `scheduleUserDeletion()`: runs on cron (`0 0 2 * * *`), pages users due for deletion, publishes batches to RabbitMQ.

#### `UserDeletionConsumerImpl`

- `processDeletionBatch(...)`: listens to deletion queue, partitions IDs into sub-batches of 500, deletes per sub-batch, rethrows on failure for retry.

#### `NotificationPublisherImpl`

- `publishNotification(...)`: publishes to topic exchange with `notifications.<type>` routing key.
- `notifyLike(...)`, `notifyFollow(...)`, `notifyComment(...)`: typed notification builders that delegate to `publishNotification(...)`.

#### `NotificationConsumerImpl`

- `consume(...)`: persists notification entity and pushes real-time event over WebSocket destination `/topic/notifications/{recipientId}`.

---

## Request Formatting Notes for Multipart Functions

For `POST /api/posts` and `PUT /api/posts/{id}`:

- Use `multipart/form-data`.
- `post` part should be JSON (prefer `application/json` part type).
- `files` part should be file uploads (`image/jpeg`, `image/png`, etc.).

For `POST /api/users/{id}/avatar`:

- Use `multipart/form-data` with `file` part.

## Related References

- API docs runtime: `/swagger-ui.html`, `/v3/api-docs`
- Postman collection: `postman/Nexora-All-APIs.postman_collection.json`


