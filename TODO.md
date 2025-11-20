# TODO - Kotlin API Development

## ✅ Completed Setup

- [x] Spring Boot 3.5.7 + Kotlin 1.9.25 + Java 21 project initialized
- [x] MySQL 8.0 database running in Docker
- [x] Repository entity, repository interface, controller (CRUD working)
- [x] OAuth2 + JWT authentication infrastructure
- [x] User entity and repository
- [x] Security configuration with JWT filter
- [x] Profile endpoint (`GET /api/v1/profile`)
- [x] 10 test repositories imported from Rails
- [x] Custom Gradle tasks (`version`, `info`, `dev`)
- [x] GitHub OAuth app registered
- [x] `.env` file configured with credentials
- [x] IntelliJ IDEA project opened

---

## 🎯 Next Session: Test Authentication Flow

### Step 1: Start the Application

**In IntelliJ:**
1. Open `RepoReconnoiterApplication.kt`
2. Click green ▶️ next to `fun main()` OR use Run Configuration
3. Wait for "Started RepoReconnoiterApplication" in console
4. Verify: `curl http://localhost:8080/api/v1/actuator/health`

**Or in Terminal:**
```bash
./gradlew bootRun
```

### Step 2: Test OAuth Login Flow

1. **Open browser:** http://localhost:8080/oauth2/authorization/github
2. **Authorize app** on GitHub
3. **Should redirect** to: `http://localhost:3000/auth/callback?token=<JWT>`
4. **Copy the JWT token** from URL

**Note:** Frontend doesn't exist yet, so redirect will 404. That's expected! Just grab the token from the URL.

### Step 3: Test Protected Endpoint

**Test `/profile` with JWT:**
```bash
# Replace YOUR_JWT_TOKEN with actual token from Step 2
curl -H "X-User-Token: YOUR_JWT_TOKEN" http://localhost:8080/api/v1/profile
```

**Expected response:**
```json
{
  "id": 1,
  "email": "your@email.com",
  "githubUsername": "yourusername",
  "githubName": "Your Name",
  "githubAvatarUrl": "https://...",
  "admin": false
}
```

### Step 4: Set Up Insomnia (API Testing Tool)

1. **Download Insomnia:** https://insomnia.rest/download
2. **Create workspace:** "RepoReconnoiter Kotlin API"
3. **Create environment:**
   - `base_url`: `http://localhost:8080/api/v1`
   - `jwt_token`: (paste token from Step 2)
4. **Test requests:**
   - `GET {{base_url}}/repositories`
   - `GET {{base_url}}/repositories/30`
   - `GET {{base_url}}/profile` (with header `X-User-Token: {{jwt_token}}`)

---

## 📋 Upcoming Features (After Auth Works)

### Phase 2: Analysis Endpoints
- [ ] `POST /api/v1/repositories/:id/analyze` - Deep analysis (async)
- [ ] `POST /api/v1/repositories/analyze_by_url` - Analyze by GitHub URL
- [ ] `GET /api/v1/repositories/status/:session_id` - Analysis status polling
- [ ] Create `Analysis` entity
- [ ] Create `AnalysisStatus` entity
- [ ] Background job simulation (or use Spring `@Async`)

### Phase 3: Comparison Endpoints
- [x] `GET /api/v1/comparisons` - List comparisons
- [ ] `POST /api/v1/comparisons` - Create comparison (async)
- [x] `GET /api/v1/comparisons/:id` - Show comparison
- [ ] `GET /api/v1/comparisons/status/:session_id` - Comparison status
- [ ] Create `Comparison` entity
- [ ] Create `ComparisonRepository` join table
- [ ] Background job for comparison creation

### Phase 4: WebSocket Support
- [ ] Add WebSocket dependencies
- [ ] Create STOMP configuration
- [ ] Broadcast analysis progress
- [ ] Broadcast comparison progress
- [ ] Test with Insomnia WebSocket tab

### Phase 5: Additional Features
- [ ] Category entity (if needed)
- [ ] Search/filtering for repositories
- [ ] Pagination improvements
- [ ] Rate limiting (matching Rails: 25/day per user)
- [ ] Error handling improvements
- [ ] API documentation (Springdoc OpenAPI)

---

## 🚀 Quick Reference

**Start app:**
```bash
./gradlew bootRun
# Or IntelliJ: Run → Run 'RepoReconnoiterApplication'
```

**Run tests:**
```bash
./gradlew test
```

**Check version:**
```bash
./gradlew version
```

**Database:**
```bash
# Start MySQL
docker-compose up -d

# View data
docker-compose exec mysql mysql -u reconnoiter -pdevpassword reconnoiter_dev
```

**Stop app:**
- IntelliJ: Click red ⏹️ stop button
- Terminal: `Ctrl+C`

---

## 📝 Notes

- JWT tokens expire after 24 hours (configured in `application.yml`)
- OAuth redirect URL is currently hardcoded to `http://localhost:3000/auth/callback` (frontend doesn't exist yet)
- For testing, just grab the token from the failed redirect URL
- All endpoints except `/profile` are currently public (no auth required)
- MySQL runs in Docker, app runs locally (not containerized yet)

---

## 🎯 Tomorrow's Goal

**Get OAuth + JWT working end-to-end:**
1. Login via GitHub → Get JWT token
2. Use JWT to access `/profile` endpoint
3. Set up Insomnia for future API testing

**Expected time:** 15-30 minutes if everything works! 🤞
