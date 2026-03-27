# Mahalle Ustası — Firestore Koleksiyon Şeması

## Koleksiyon: `users/{userId}`

```json
{
  "id":           "string (Firebase Auth UID)",
  "name":         "string",
  "email":        "string",
  "photoUrl":     "string | null",
  "rating":       "number (ortalama puan, 0–5)",
  "reviewCount":  "number",
  "completedJobsCount": "number",
  "createdAt":    "timestamp"
}
```

---

## Koleksiyon: `jobs/{jobId}`

```json
{
  "id":          "string (auto-id)",
  "title":       "string",
  "description": "string",
  "category":    "string (REPAIR | CLEANING | MOVING | TUTORING | GARDENING | TECH_SUPPORT | OTHER)",
  "status":      "string (OPEN | IN_PROGRESS | COMPLETED | CANCELLED)",

  "location": {
    "address": "string",
    "lat":     "number",
    "lng":     "number"
  },

  "budget":      "string | null  (örn: '500 TL' veya 'Teklife Açık')",
  "photoUrls":   ["string"],

  "postedByUserId":   "string (users koleksiyonuna referans)",
  "postedByUserName": "string (denormalize, hız için)",

  "acceptedOfferId":  "string | null",
  "offerCount":       "number",

  "createdAt":   "timestamp",
  "updatedAt":   "timestamp"
}
```

**Index:** `status ASC, createdAt DESC` (ana akış sorgusu için)

---

## Koleksiyon: `offers/{offerId}`

```json
{
  "id":          "string (auto-id)",
  "jobId":       "string (jobs koleksiyonuna referans)",
  "status":      "string (PENDING | ACCEPTED | REJECTED)",

  "price":       "number",
  "description": "string",

  "offeredByUserId":   "string",
  "offeredByUserName": "string",
  "offeredByPhotoUrl": "string | null",

  "createdAt":   "timestamp"
}
```

**Index (birleşik):** `jobId ASC` | `offeredByUserId ASC, createdAt DESC`

---

## Koleksiyon: `chats/{chatId}`

`chatId` formatı: `{jobId}_{acceptedOfferId}` (deterministik, tekrar oluşturulmaz)

```json
{
  "id":         "string",
  "jobId":      "string",
  "offerId":    "string",
  "jobTitle":   "string (denormalize)",

  "participants": ["userId1", "userId2"],

  "lastMessage":     "string",
  "lastMessageTime": "timestamp",
  "lastSenderId":    "string",

  "isJobCompleted":  "boolean",
  "createdAt":       "timestamp"
}
```

### Alt-koleksiyon: `chats/{chatId}/messages/{messageId}`

```json
{
  "id":        "string",
  "senderId":  "string",
  "text":      "string",
  "createdAt": "timestamp"
}
```

**Güvenlik:** Firestore Rules → `participants` array içeriyorsa okuma/yazma izni.

---

## Koleksiyon: `reviews/{reviewId}`

```json
{
  "id":          "string (auto-id)",
  "jobId":       "string",
  "chatId":      "string",

  "reviewerUserId":   "string",
  "reviewedUserId":   "string",

  "role":        "string (AS_WORKER | AS_CLIENT)",
  "rating":      "number (1–5)",
  "comment":     "string",

  "createdAt":   "timestamp"
}
```

**Açıklama:**
- `AS_WORKER`: İlan sahibi, hizmet vereni (ustayı) değerlendiriyor.
- `AS_CLIENT`: Hizmet veren, ilan sahibini (müşteriyi) değerlendiriyor.

---

## Firestore Security Rules (Taslak)

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    // Kullanıcılar kendi verisini okuyup yazabilir
    match /users/{userId} {
      allow read: if request.auth != null;
      allow write: if request.auth.uid == userId;
    }

    // İlanlar: herkes okuyabilir, sadece sahibi yazabilir
    match /jobs/{jobId} {
      allow read:   if request.auth != null;
      allow create: if request.auth != null;
      allow update: if request.auth.uid == resource.data.postedByUserId;
    }

    // Teklifler: giriş yapan okuyabilir, sahibi oluşturabilir
    match /offers/{offerId} {
      allow read:   if request.auth != null;
      allow create: if request.auth != null;
      allow update: if request.auth.uid == resource.data.offeredByUserId;
    }

    // Sohbet: sadece katılımcılar
    match /chats/{chatId} {
      allow read, write: if request.auth.uid in resource.data.participants;

      match /messages/{messageId} {
        allow read, write: if request.auth.uid in
          get(/databases/$(database)/documents/chats/$(chatId)).data.participants;
      }
    }

    // Yorumlar: giriş yapan okuyabilir, reviewer oluşturabilir
    match /reviews/{reviewId} {
      allow read:   if request.auth != null;
      allow create: if request.auth.uid == request.resource.data.reviewerUserId;
    }
  }
}
```
