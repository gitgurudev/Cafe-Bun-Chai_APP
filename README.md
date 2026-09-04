# Cafe Bun Chai POS

Staff Android counter app. **Firestore is the source of truth.** Room is an offline cache on the phone.

## Firestore layout

Collections appear after the first staff login (Firestore creates a collection when the first document is written). You do **not** create empty collections in the console.

```
users/{firebaseUid}          role: "admin" | "staff"   (create these docs yourself)
cafes/bun-chai               cafe metadata
cafes/bun-chai/categories/{id}
cafes/bun-chai/menuItems/{id}
cafes/bun-chai/inventory/{id}
cafes/bun-chai/recipes/{id}
cafes/bun-chai/orders/{id}   lines[] on the same document
```

On first login, if `categories` / `menuItems` are empty, the app writes the seeded menu + stock to those paths.

## Rules

Publish `firestore.rules` in Firebase Console → Firestore → Rules (or `firebase deploy --only firestore:rules`).

Until rules are published, cloud writes fail and phones stay on local Room only.

## Users

Create `users/{uid}` with field `role` (`admin` or `staff`). Document ID must be the Auth UID, not the email.

## Run

1. Android Studio, SDK 35, open `cafe-bun-chai`.
2. Install the debug APK from `%USERPROFILE%\AppData\Local\cafe-bun-chai-build\app\outputs\apk\debug\app-debug.apk`.
3. Sign in. After a few seconds, Console → Firestore should show `cafes/bun-chai` and subcollections.
